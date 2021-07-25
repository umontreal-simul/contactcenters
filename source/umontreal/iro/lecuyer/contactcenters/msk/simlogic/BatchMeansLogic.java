package umontreal.iro.lecuyer.contactcenters.msk.simlogic;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.params.BatchSimParams;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManagerWithStaffing;
import umontreal.iro.lecuyer.contactcenters.msk.model.ArrivalProcessManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.Call;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallFactory;
import umontreal.iro.lecuyer.contactcenters.msk.model.DialerManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.MeasureType;
import umontreal.iro.lecuyer.contactcenters.msk.stat.SimCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatPeriod;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simexp.BatchMeansSim;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfTallies;
import umontreal.ssj.stat.mperiods.MeasureMatrix;

/**
 * Implements the logic for a simulation with batch means. This logic simulates
 * a single long replication which is divided into time intervals called
 * batches. The logic uses matrices of counters with a single column for storing
 * values for the current real batch. In the notation of the super class
 * {@link BatchMeansSim}, these counters are used to generate the $\boldV_j$
 * vectors containing statistics for real batches. When batch aggregation is
 * disabled, these counters are used directly to make the matrices of
 * observations which correspond to the $\boldX_r$ vectors. When batch
 * aggregation is enabled, the vectors of counts for each real batch are added
 * to intermediate lists of statistical probes, and matrices of observations are
 * constructed by aggregating some of these vectors. The operator used for
 * aggregation is the sum, but it can also be the maximum for some statistics
 * such as the maximal number of busy agents, maximal queue size, and maximal
 * waiting time. For more information about batch aggregation, see the
 * documentation of the super class {@link BatchMeansSim}.
 */
public class BatchMeansLogic extends BatchMeansSim implements SimLogic {
   private final Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.msk.simlogic"); //$NON-NLS-1$
   private CallCenter cc;
   private SimLogicBase base;
   private BatchSimParams simParams;
   private SimCallCenterStat stat;
   private CallCenterMeasureManager ccm;

   // Contains a probability for each call type, to
   // initialize the system in a non-empty state
   // in the initSimulation method.
   private double[] ctProbs;
   private double totalArrivalRate;

   // Variables necessary for the heuristic stability check
   private boolean seemsUnstable = false;
   private int maxQueueSize;
   private int maxQueueSizeThresh;
   private StabilityChecker sChecker;

   // Variables necessary for batch aggregation
   private int s, h;
   private double l;

   // List of statistical probes containing the V_j's when
   // batch aggregation is turned on.
   private final Map<MeasureType, ListOfTallies<TallyStore>> rpMap = new EnumMap<MeasureType, ListOfTallies<TallyStore>> (
         MeasureType.class);

   /**
    * Constructs a new simulation logic for batch means, using the model
    * \texttt{cc}, the simulation parameters \texttt{simParams}, and estimating
    * performance measures of all types listed in \texttt{pms}.
    * 
    * @param cc
    *           the simulated model.
    * @param simParams
    *           the simulation parameters.
    * @param pms
    *           the estimated performance measures.
    */
   public BatchMeansLogic (CallCenter cc, BatchSimParams simParams,
         PerformanceMeasureType... pms) {
      super (cc.simulator (), simParams.getMinBatches (), simParams
            .isSetMaxBatches () ? simParams.getMaxBatches ()
            : Integer.MAX_VALUE, cc.getTime (simParams.getBatchSize ()),
            simParams.getWarmupBatches ()
                  * cc.getTime (simParams.getBatchSize ()));
      base = new SimLogicBase ();
      setBatchAggregation (simParams.isAggregation ());
      this.simParams = simParams;
      this.cc = cc;
      ccm = new BatchMeansCMM (cc, this, simParams.isEstimateContactTypeAgentGroup (), pms);
      ccm.registerListeners ();
      stat = new SimCallCenterStat (cc, ccm, simParams.isKeepObs (), simParams.isNormalizeToDefaultUnit (), pms);
      createRealBatchProbes ();
      cc.setAwtPeriod (this);
   }

   public CallCenter getCallCenter () {
      return cc;
   }

   public BatchSimParams getSimParams () {
      return simParams;
   }

   public CallCenterStatProbes getCallCenterStatProbes () {
      return stat;
   }

   public void registerListeners () {
      ccm.registerListeners ();
      if (sChecker != null)
         for (final WaitingQueue queue : cc.getWaitingQueues ())
            queue.addWaitingQueueListener (sChecker);
   }

   public void unregisterListeners () {
      ccm.unregisterListeners ();
      if (sChecker != null)
         for (final WaitingQueue queue : cc.getWaitingQueues ())
            queue.removeWaitingQueueListener (sChecker);
   }

   private void createRealBatchProbes () {
      rpMap.clear ();
      for (final MeasureType mt : ccm.getMeasures ()) {
         final MeasureMatrix mm = ccm.getMeasureMatrix (mt);
         final ListOfTallies<TallyStore> a = ListOfTallies
               .createWithTallyStore (mm.getNumMeasures ());
         rpMap.put (mt, a);
      }
   }

   @Override
   public void initBatchStat () {
      ccm.initMeasureMatrices ();
   }

   @Override
   public void initRealBatchProbes () {
      if (!getBatchAggregation ())
         return;
      for (final ListOfTallies<TallyStore> a : rpMap.values ())
         a.init ();
   }

   @Override
   public void addRealBatchObs () {
      if (!getBatchAggregation ())
         return;
      // This is for time-average waiting time and number of agents.
      // The counters for these integral measure matrices are reset in
      // initBatchStat at the beginning of each batch, which creates a first
      // record at the beginning of the batch.
      // The newPeriod method, called at the end of the batch, creates
      // the final records, which allows sums to be obtained.
      // See the documentation of IntegralMeasureMatrix for more details
      // about records.
      ccm.finishCurrentPeriod ();
      for (final MeasureType mt : ccm.getMeasures ()) {
         final ListOfTallies<TallyStore> a = rpMap.get (mt);
         double[] tmp = getValuesForRealBatch (mt);
         a.add (tmp);
      }
   }

   @Override
   public void initEffectiveBatchProbes () {
      stat.init ();
   }

   @Override
   public void addEffectiveBatchObs (int s, int h1, double ell) {
      if (getBatchAggregation ()) {
         // Stores s, h, and l in fields to be read by the getValues method.
         this.s = s;
         this.h = h1;
      }
      else {
         this.s = 0;
         this.h = 1;
         ccm.finishCurrentPeriod ();
      }
      this.l = ell;
      // The addObs method calls back getValues in this class to
      // obtain the matrices of observations.
      stat.addObs ();
   }

   public void reset (PerformanceMeasureType... pms) {
      setTargetBatches (simParams.getMinBatches ());
      setBatchSize (cc.getTime (simParams.getBatchSize ()));
      ccm.unregisterListeners ();
      ccm = new BatchMeansCMM (cc, this, simParams.isEstimateContactTypeAgentGroup (), pms);
      ccm.registerListeners ();
      stat = new SimCallCenterStat (cc, ccm, simParams.isKeepObs (), simParams.isNormalizeToDefaultUnit (), pms);
      createRealBatchProbes ();
   }

   /**
    * Returns 1.
    */
   public int getNumPeriodsForCounters () {
      return 1;
   }

   /**
    * Returns 1.
    */
   public int getNumPeriodsForCountersAwt () {
      return 1;
   }
   
   public boolean needsSlidingWindows () {
      return false;
   }
   
   public boolean needsStatForPeriodSegmentsAwt () {
      return false;
   }

   public int getCompletedSteps () {
      return getCompletedRealBatches ();
   }

   public CallCenterMeasureManager getCallCenterMeasureManager () {
      return ccm;
   }

   private double[] getValuesForRealBatch (MeasureType mt) {
      double[] m;
      final MeasureMatrix mm = ccm.getMeasureMatrix (mt);
      m = new double[mm.getNumMeasures ()];
      for (int j = 0; j < m.length; j++)
         m[j] = mm.getMeasure (j, 0);
      return m;
   }
   
   public boolean isContactTypeAgentGroup() {
      return ccm.isContactTypeAgentGroup ();
   }   
   
   public void initMeasureMatrices() {
      ccm.initMeasureMatrices ();
   }
   
   public MeasureType[] getMeasures() {
      return ccm.getMeasures ();
   }
   
   public boolean hasMeasureMatrix (MeasureType mt) {
      return ccm.hasMeasureMatrix (mt);
   }
   
   public boolean hasMeasureMatricesFor (PerformanceMeasureType pm) {
      return ccm.hasMeasureMatricesFor (pm);
   }

   /**
    * Returns the result of {@link #getStatPeriod()}.
    */
   public int getStatPeriod (Contact contact) {
      return getStatPeriod();
   }
   
   /**
    * Returns 0 if the warmup period is over, or -1 otherwise.
    */
   public int getStatPeriod () {
      if (!isWarmupDone ())
         return -1;
      return 0;
   }

   /**
    * Returns the same value as {@link #getStatPeriod(Contact)}.
    */
   public int getStatPeriodAwt (Contact contact) {
      return getStatPeriod (contact);
   }

   /**
    * This returns $P'-1$.
    */
   public int getAwtPeriod (Contact contact) {
      return cc.getNumMainPeriodsWithSegments () - 1;
   }

   /**
    * This returns $P'-1$.
    */
   public int getGlobalAwtPeriod () {
      return cc.getNumMainPeriodsWithSegments () - 1;
   }

   public boolean isSteadyState () {
      return true;
   }

   public void formatReport (Map<String, Object> evalInfo) {
      if (seemsUnstable) {
         evalInfo
               .put (Messages.getString ("BatchMeansLogic.SeemsUnstable"), ""); //$NON-NLS-1$
         evalInfo.put (Messages.getString ("BatchMeansLogic.MaxQueueSize"),
               maxQueueSize);
         evalInfo.put (Messages
               .getString ("BatchMeansLogic.MaxQueueSizeThresh"),
               maxQueueSizeThresh);
      }
      evalInfo
            .put (
                  Messages.getString ("BatchMeansLogic.TotalSimulationTime"), cc.simulator ().time ()); //$NON-NLS-1$
      evalInfo
            .put (
                  Messages.getString ("BatchMeansLogic.NumBatches"), getCompletedRealBatches ()); //$NON-NLS-1$
   }

   public int[] getStaffing () {
      final int[] numAgents = new int[cc.getNumAgentGroups ()];
      final int mp = getCurrentMainPeriod ();
      for (int i = 0; i < numAgents.length; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         numAgents[i] = group.getEffectiveStaffing (mp);
      }
      return numAgents;
   }

   public void setStaffing (int[] numAgents) {
      if (numAgents.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException (
               "Incompatible staffing vector dimensions");
      final int mp = getCurrentMainPeriod ();
      for (int i = 0; i < numAgents.length; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         if (group instanceof AgentGroupManagerWithStaffing) {
            AgentGroupManagerWithStaffing group2 = (AgentGroupManagerWithStaffing) group;
            group2.setEffectiveStaffing (mp, numAgents[i]);
         }
         else
            throw new ClassCastException ("Agent group " + i
                  + " uses a schedule; cannot set a staffing vector directly");
      }
   }

   public int[][] getStaffingMatrix () {
      final int[][] numAgents = new int[cc.getNumAgentGroups ()][1];
      final int mp = getCurrentMainPeriod ();
      for (int i = 0; i < numAgents.length; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         numAgents[i][0] = group.getEffectiveStaffing (mp);
      }
      return numAgents;
   }

   public void setStaffingMatrix (int[][] numAgents) {
      if (numAgents.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException (
               "Incompatible staffing matrix dimensions");
      final int mp = getCurrentMainPeriod ();
      for (int i = 0; i < numAgents.length; i++) {
         if (numAgents[i] == null)
            continue;
         if (numAgents[i].length != 1)
            throw new IllegalArgumentException (
                  "Incompatible dimensions for staffing matrix");
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         if (group instanceof AgentGroupManagerWithStaffing) {
            AgentGroupManagerWithStaffing group2 = (AgentGroupManagerWithStaffing) group;
            group2.setEffectiveStaffing (mp, numAgents[i][0]);
         }
         else
            throw new ClassCastException ("Agent group " + i
                  + " uses a schedule; cannot set a staffing vector directly");
      }
   }

   @Override
   public int[][] getScheduledAgents () {
      throw new UnsupportedOperationException ();
   }

   @Override
   public void setScheduledAgents (int[][] ag) {
      throw new UnsupportedOperationException ();
   }

   @Override
   public int getCurrentMainPeriod () {
      return simParams.getCurrentPeriod ();
   }

   /**
    * Sets the current period for this simulator to \texttt{mp}.
    * 
    * @param mp the new current period.
    * @throws IllegalArgumentException if the new current period
    * is greater than the number of main periods {@link CallCenter#getNumMainPeriods()}
    */
   @Override
   public void setCurrentMainPeriod (int mp) {
      if (mp < 0 || mp >= cc.getNumMainPeriods())
         throw new IllegalArgumentException("Invalid main period: " + mp);
      simParams.setCurrentPeriod (mp);
   }

   @Override
   public void initSimulation () {
      cc.getPeriodChangeEvent ().setCurrentPeriod (
            simParams.getCurrentPeriod () + 1);
      cc.initSim ();
      ccm.initMeasureMatrices ();
      // ccm.initMaxSizes ();

      // ccm.initMaxBusyAgents ();
      int cp = simParams.getCurrentPeriod ();
      if (cp < 0 || cp >= cc.getNumMainPeriods())
         throw new IllegalArgumentException("Invalid main period: " + cp);
      
      // final CallCenterParams ccParams = model.getCallCenterParams ();
      // final AgentGroup[] groups = model.getAgentGroups ();
      // for (int i = 0; i < I; i++) {
      // final AgentGroupParams par = ccParams.getAgentGroups().get (i);
      // groups[i].setNumAgents (par.getStaffing (cp));
      // }
      if (ctProbs == null || ctProbs.length != cc.getNumInContactTypes ())
         ctProbs = new double[cc.getNumInContactTypes ()];
      totalArrivalRate = 0;
      cp = cc.getPeriodChangeEvent ().getCurrentPeriod ();
      for (int k = 0; k < ctProbs.length; k++) {
         final double rate;
         ContactArrivalProcess arrivProc = cc.getArrivalProcess (k);
         if (arrivProc == null)
            rate = 0;
         else
            rate = arrivProc.getArrivalRate (cp);
         totalArrivalRate += rate;
         ctProbs[k] = rate;
      }
      for (int k = 0; k < ctProbs.length; k++)
         ctProbs[k] /= totalArrivalRate;
      final int KI = cc.getNumInContactTypes ();
      if (simParams.isInitNonEmpty ()
            && simParams.getTargetInitOccupancy () > 0) {
         // Disable queueing
         cc.getRouter ().setTotalQueueCapacity (0);
         int totalAgents = 0;
         for (final AgentGroup group : cc.getAgentGroups ())
            totalAgents += group.getNumAgents ();
         totalAgents = (int) Math.round (totalAgents
               * simParams.getTargetInitOccupancy ());
         int previousBusyAgents = 0;
         boolean done = false;
         int nBlocked = 0;
         final RandomStreams streams = cc.getRandomStreams ();
         final RandomStream rsmCt = streams.getStreamCT ();
         do {
            // Put a new contact in the system
            double u = rsmCt.nextDouble ();
            int ct = 0;
            while (ct < KI && u > ctProbs[ct]) {
               u -= ctProbs[ct];
               ++ct;
            }
            // The router will direct the contact to an agent group or a
            // queue
            final Contact c = cc.getArrivalProcess (ct).getContactFactory ()
                  .newInstance ();
            ((Call) c).setUTransfer (1);
            c.setSource (cc.getArrivalProcess (ct));
            cc.getRouter ().newContact (c);

            // Stop when all agents are busy
            int totalBusyAgents = 0;
            for (final AgentGroup group : cc.getAgentGroups ())
               totalBusyAgents += group.getNumBusyAgents ();
            done = totalBusyAgents >= totalAgents;
            if (totalBusyAgents == previousBusyAgents) {
               nBlocked++;
               if (nBlocked >= simParams.getMaxInitBlocked ())
                  done = true;
            }
            else {
               previousBusyAgents = totalBusyAgents;
               nBlocked = 0;
            }
         }
         while (!done);

         // Enables queueing.
         cc.getRouter ().setTotalQueueCapacity (cc.getQueueCapacity ());
      }
      for (final ArrivalProcessManager apInfo : cc.getArrivalProcesManagers ()) {
         if (apInfo == null)
            continue;
         if (apInfo.isSourceEnabled ())
            apInfo.getArrivalProcess ().startStationary ();
      }
      for (final DialerManager dialerInfo : cc.getDialerManagers ()) {
         if (dialerInfo == null)
            continue;
         if (dialerInfo.isSourceEnabled ())
            dialerInfo.getDialer ().start ();
      }
   }

   @Override
   public void init () {
      setBatchSize (cc.getTime (simParams.getBatchSize ()));
      setMaxBatches (simParams.isSetMaxBatches () ? simParams.getMaxBatches ()
            : Integer.MAX_VALUE);
      seemsUnstable = false;
      maxQueueSize = 0;
      if (stabilityCheck ()) {
         maxQueueSizeThresh = computeMaxQueueSizeThresh ();
         if (sChecker == null)
            sChecker = new StabilityChecker ();
         for (final WaitingQueue queue : cc.getWaitingQueues ())
            queue.addWaitingQueueListener (sChecker);
      }
      else if (sChecker != null) {
         for (final WaitingQueue queue : cc.getWaitingQueues ())
            queue.removeWaitingQueueListener (sChecker);
         sChecker = null;
      }
      setTargetBatches (getMinBatches ());
      setAborted (false);
      super.init ();
   }

   @Override
   public void adjustTargetBatches (int numNewBatches) {
      super.adjustTargetBatches (numNewBatches);
      if (getBatchAggregation ()) {
         int nb = getTargetBatches () - getCompletedRealBatches ();
         final int numStored = getCompletedRealBatches ()
               - getDroppedRealBatches ();
         if (numStored > 0 && nb >= 100 * numStored) {
            if (isVerbose ()) {
               logger.info ("The number of required new real batches, " + nb
                     + ", largely exceeds the number of stored real batches "
                     + numStored + ".");
               logger.info ("To avoid running out of memory, the batch size "
                     + "will be increased, and the number of "
                     + "required batches will be decreased");
            }
            final int mult = 20 * getMinBatches ();
            setBatchSize (mult * getBatchSize ());
            nb /= mult;
            dropFirstRealBatches (numStored);
            initRealBatchProbes ();
            for (final ListOfTallies<TallyStore> a : rpMap.values ())
               for (final TallyStore t : a)
                  t.getDoubleArrayList ().ensureCapacity (numNewBatches);
         }
         else
            for (final ListOfTallies<TallyStore> a : rpMap.values ())
               for (final TallyStore t : a)
                  t.getDoubleArrayList ().ensureCapacity (
                        numStored + numNewBatches);
      }
   }

   public void simulate (int numBatches) {
      if (numBatches < 0)
         throw new IllegalArgumentException ("numBatches < 0");
      if (numBatches == 0)
         return;
      adjustTargetBatches (numBatches);
      if (!isWarmupDone ())
         warmup ();
      simulateBatches ();
      stat.initRawStatistics ();
      stat.addObsRawStatistics ();
   }

   public boolean seemsUnstable () {
      return seemsUnstable;
   }

   /**
    * Computes and returns the maximal queue size threshold before a simulated
    * system is declared unstable. By default, this returns $20000 +
    * 1000\sqrt{N}$ where $N$ is the total number of agents.
    * 
    * @return the maximal queue size threshold.
    */
   public int computeMaxQueueSizeThresh () {
      // Compute the maximal queue size
      int totalNumAgents = 0;
      for (final AgentGroup group : cc.getAgentGroups ())
         totalNumAgents += group.getNumAgents ();
      return (int) Math.ceil (20000 + 1000 * Math.sqrt (totalNumAgents));
   }

   public boolean stabilityCheck () {
      final int K = cc.getNumInContactTypes ();
      final int cp = getCurrentMainPeriod ();
      for (int k = 0; k < K; k++) {
         final CallFactory factory = cc.getCallFactory (k);
         final MultiPeriodGen mpg = factory.getPatienceTimeGen ();
         if (mpg == null)
            return true;
         final RandomVariateGen rp = mpg.getGenerator (cp + 1);
         if (Double.isInfinite (rp.getDistribution ().getMean ()))
            return true;
         // if (rp.getDistribution() instanceof ExponentialDist) {
         // final double rate = 1.0 / rp.getDistribution().getMean();
         // if (rate == 0)
         // return true;
         // // else if (rate < MINPATIENCERATE) {
         // // logger.warning ("Patience rate too small for call type " + k
         // // +
         // // ", resetting to " + MINPATIENCERATE);
         // // rp.setExpLambda (MINPATIENCERATE);
         // // p.setGenParams (cp, rp);
         // // }
         // }
      }
      return false;
   }

   @Override
   public void simulateBatch () {
      if (seemsUnstable || isAborted ())
         setTargetBatches (getCompletedRealBatches ());
      else {
         final double f = getBatchSizeMultiplier ();
         if (f > 1) {
            setBatchSize (getBatchSize () * f);
            if (isVerbose ())
               logger
                     .info ("To avoid excessive memory usage, increasing batch size to "
                           + getBatchSize ());
         }
         simulateBatch (getBatchSize () * getBatchFraction ());
         for (final SimLogicListener ell : getSimLogicListeners ())
            ell.stepDone (this);
      }
   }

   private final class StabilityChecker implements WaitingQueueListener {

      public void dequeued (DequeueEvent ev) {
         checkSize ();
      }

      public void enqueued (DequeueEvent ev) {
         checkSize ();
      }

      public void init (WaitingQueue queue) {
         checkSize ();
      }

      private void checkSize () {
         if (!isWarmupDone ())
            return;
         final int queueSize = cc.getRouter ().getCurrentQueueSize ();
         if (queueSize > maxQueueSize)
            maxQueueSize = queueSize;
         if (getCompletedRealBatches () < getMinBatches ())
            return;
         if (queueSize > maxQueueSizeThresh)
            seemsUnstable = true;
         if (seemsUnstable && queueSize < maxQueueSizeThresh)
            seemsUnstable = false;
      }
   }

   public boolean isVerbose () {
      return base.isVerbose ();
   }

   public void setVerbose (boolean verbose) {
      base.setVerbose (verbose);
   }

   public boolean isAborted () {
      return base.isAborted ();
   }

   public void setAborted (boolean aborted) {
      base.setAborted (aborted);
   }

   public void addSimLogicListener (SimLogicListener ell) {
      base.addSimLogicListener (ell);
   }

   public void clearSimLogicListeners () {
      base.clearSimLogicListeners ();
   }

   public List<SimLogicListener> getSimLogicListeners () {
      return base.getSimLogicListeners ();
   }

   public void removeSimLogicListener (SimLogicListener ell) {
      base.removeSimLogicListener (ell);
   }
   
   private class BatchMeansCMM extends CallCenterMeasureManager {
      public BatchMeansCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, Collection<MeasureType> measures) {
         super (cc, statP, contactTypeAgentGroup, measures);
      }

      public BatchMeansCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, PerformanceMeasureType[] pms) {
         super (cc, statP, contactTypeAgentGroup, pms);
      }

      public BatchMeansCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup) {
         super (cc, statP, contactTypeAgentGroup);
      }

      /**
       * Returns 1.
       */
      public int getNumPeriodsForStatProbes () {
         return 1;
      }
      
      /**
       * Returns the matrix of observations corresponding to measure type
       * \texttt{mt}. If batch aggregation is enabled, this sums up the values in
       * real batches corresponding to the current effective batch.
       */
      public DoubleMatrix2D getValues (MeasureType mt, boolean norm) {
         DoubleMatrix2D m;
         DoubleDoubleFunction func = mt.getAggregationFunction ();
         if (getBatchAggregation ()) {
            final ListOfTallies<TallyStore> a = rpMap.get (mt);
            m = new DenseDoubleMatrix2D (a.size (), 1);
            for (int j = 0; j < a.size (); j++) {
               final double[] ell = a.get (j).getArray ();
               double v = 0;
               for (int k = s; k < s + h; k++)
                  v = func.apply (v, ell[k - getDroppedRealBatches ()]);
               m.setQuick (j, 0, v);
            }
         }
         else {
            double[] tmp = getValuesForRealBatch (mt);
            m = new DenseDoubleMatrix2D (tmp.length, 1);
            m.viewColumn (0).assign (tmp);
         }
         if (norm)
            timeNormalize (mt, m);
         return m;
      }

      public void timeNormalize (MeasureType mt, DoubleMatrix2D m) {
         switch (mt.getTimeNormalizeType ()) {
         case NEVER:
            return;
         case CONDITIONAL:
            if (!simParams.isNormalizeToDefaultUnit ())
               m.assign (Functions.mult (cc.getPeriodDuration ()));
         case ALWAYS:
            m.assign (Functions.div (l));
         }
      }
   }
}
