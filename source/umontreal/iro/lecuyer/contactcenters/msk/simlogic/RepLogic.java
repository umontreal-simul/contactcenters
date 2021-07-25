package umontreal.iro.lecuyer.contactcenters.msk.simlogic;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeListener;
import umontreal.iro.lecuyer.contactcenters.RepSimCC;
import umontreal.iro.lecuyer.contactcenters.SwitchEvent;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.params.RepSimParams;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManagerWithSchedule;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManagerWithStaffing;
import umontreal.iro.lecuyer.contactcenters.msk.model.ArrivalProcessManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.Call;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.DialerManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.SegmentInfo;
import umontreal.iro.lecuyer.contactcenters.msk.model.StartingState;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.MeasureType;
import umontreal.iro.lecuyer.contactcenters.msk.stat.SimCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatPeriod;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simexp.RepSim;
import umontreal.ssj.stat.mperiods.MeasureMatrix;

/**
 * Implements the logic for a simulation with independent replications.
 * For each replication, this logic initializes the model to an empty state,
 * and simulates the entire horizon, i.e., a single day, week, month, etc., depending
 * on the model's parameters.
 * Statistics are collected in every period.
 */
public class RepLogic extends RepSim implements SimLogic {
   private CallCenter cc;
   private SimLogicBase base;
   private RepSimParams simParams;
   private SimCallCenterStat stat;
   private CallCenterMeasureManager ccm;
   private IntegralMeasureUpdater miu;

   /**
    * Constructs a new simulation logic for
    * independent replications, using
    * the model \texttt{cc},
    * the simulation parameters \texttt{simParams},
    * and estimating performance measures
    * of all types listed in
    * \texttt{pms}.
    * @param cc the simulated model.
    * @param simParams the simulation parameters.
    * @param pms the estimated performance measures.
    */
   public RepLogic (CallCenter cc, RepSimParams simParams,
         PerformanceMeasureType... pms) {
      super (cc.simulator (), simParams.getMinReplications (),
            simParams.isSetMaxReplications () ? simParams.getMaxReplications () : Integer.MAX_VALUE);
      if (cc == null || simParams == null)
         throw new NullPointerException();
      base = new SimLogicBase ();
      this.cc = cc;
      this.simParams = simParams;
      ccm = new RepCMM (cc, this, simParams.isEstimateContactTypeAgentGroup (), pms);
      ccm.registerListeners ();
      stat = new SimCallCenterStat (cc, ccm, simParams.isKeepObs (), simParams.isNormalizeToDefaultUnit (), pms);
      miu = new IntegralMeasureUpdater (ccm);
      cc.getPeriodChangeEvent ().addPeriodChangeListener (miu);
      cc.setAwtPeriod (this);
   }

   public void registerListeners () {
      ccm.registerListeners ();
      cc.getPeriodChangeEvent ().addPeriodChangeListener (miu);
   }

   public void unregisterListeners () {
      ccm.unregisterListeners ();
      cc.getPeriodChangeEvent ().removePeriodChangeListener (miu);
   }

   public CallCenterStatProbes getCallCenterStatProbes () {
      return stat;
   }

   @Override
   public void performReplication (int r) {
      // Same as the implementation in RepSim,
      // except we call pce.stop after Sim.start.
      // We also notify listeners, and
      // abort the simulation if
      // isAborted returns true.
      final Simulator sim1 = cc.simulator();
      sim1.init ();
      // ContactCenter.initMeasureMatrices (getMeasureMatrices());
      initReplication (r);
      sim1.start ();
      for (WaitingQueue queue : cc.getWaitingQueues ())
         queue.clear (Router.DEQUEUETYPE_NOAGENT);
      cc.getPeriodChangeEvent ().stop ();
      replicationDone ();
      for (final SimLogicListener l : getSimLogicListeners ())
         l.stepDone (this);
      addReplicationObs (r);
      if (isAborted ())
         setTargetReplications (getCompletedReplications ());
   }

   @Override
   public void initReplication (int r) {
      cc.getPeriodChangeEvent ().init ();
      cc.getPeriodChangeEvent ().start ();
      cc.initSim ();
      ccm.initMeasureMatrices ();
      for (final AgentGroup group : cc.getAgentGroups())
         group.setNumAgents (0);
      for (final ArrivalProcessManager arvProcInfo : cc.getArrivalProcesManagers())
         if (arvProcInfo != null && arvProcInfo.isSourceEnabled ())
            scheduleSwitch (cc, arvProcInfo.getArrivalProcess(),
                  arvProcInfo.getSourceToggleTimes());
      for (final DialerManager dialerInfo : cc.getDialerManagers())
         if (dialerInfo != null && dialerInfo.isSourceEnabled ())
            scheduleSwitch (cc, dialerInfo.getDialer(),
                  dialerInfo.getSourceToggleTimes());
      
      // initialize with non-empty queues and pre-assigned
      // calls to agents if required.
      initStartingState();
   }
   
   
   /**
    * Initializes the waiting queues and pre-assigns calls to agents before simulating
    * each replication.
    * This method depends on the parameter {@link CallCenter#getStartingState()}, and
    * it does nothing if this parameter is {@code null} or if
    * {@link StartingState#isEnabled()} is set to {@code false}.
    * 
    * This method does 2 operations:
    * \begin{enumerate}
    * \item It creates the desired number of agents and assigns new calls to these agents.
    * \item It fills the queues with the defined number of waiting clients (these calls
    *       will not initially be assigned to the agents).
    * \end{enumerate}
    * 
    * This method will not check if the number of pre-assigned agents is equal to the staffed
    * or scheduled number of agents.
    * If the staffed number of agents in a group is higher than pre-assigned number of agents,
    * then the simulator will create the missing agents once the simulation
    * starts. Otherwise, if the staffed number is lower, then the difference between the
    * pre-assigned number and the staffed number of agents will be declared as ghost agents, and they
    * will exit the call center as soon as they finish serving their call.
    * 
    * All the calls defined in the starting state parameter {@link CallCenter#getStartingState()}
    * will be counted in the performance measures (rate of arrivals, abandonment, ...).
    * We note in particular that the calls pre-assigned to the starting agents will have 
    * zero waiting time of 0, thus these calls will increase the service levels.
    * Because of the complexity of the simulation process, we do not override the arrival
    * time of a client (to negative simulation time value) even when the waited time
    * has been given in the starting state parameter.
    * The waited times and served times specified before simulation are used only to compute the
    * remaining random patience and service times.
    * These waited times and served times are not counted in the performance measures.
    */
   public void initStartingState() {
      StartingState ss = cc.getStartingState();
      
      if (ss == null || ss.isEnabled() == false)
         return;

      Router router = cc.getRouter();
      
      if (ss.isCallServiceByGroupAvailable()) {
         int[][] cg = ss.getCallServiceByGroup(); // [group][call type]
         // set the initial number of agents in each group
         for (int g = 0; g < cc.getNumAgentGroups(); g++) {
            // get sum of needed agents
            int totalAgents = 0;
            for (int k = 0; k < cg[g].length; k++) {
               if (cg[g][k] > 0 && router.canServe(g, k) == false)
                  throw new IllegalArgumentException("Starting state error: Group " + g + " cannot serve call type " + k);
               totalAgents += cg[g][k];
            }
            cc.getAgentGroup(g).setNumAgents(totalAgents);
            // set to serve
            for (int k = 0; k < cg[g].length; k++) {
               for (int i = 0; i < cg[g][k]; i++)
                  cc.getAgentGroup(g).serve(cc.getCallFactory(k).newInstance());
            }
         }
      }
      else if (ss.isDetailedGroupsAvailable()) {
         for (int g = 0; g < ss.getDetailedGroups().size(); g++) {
            StartingState.StartingServingGroup group = ss.getDetailedGroups().get(g);
            
            // get sum of needed agents
            cc.getAgentGroup(g).setNumAgents(group.getServingList().size());
            
            for (StartingState.ServingCall c : group.getServingList()) {
               if (router.canServe(g, c.getTypeId()) == false)
                  throw new IllegalArgumentException("Starting state error: Group " + g + " cannot serve call type " + c.getTypeId());
               
               Contact call = cc.getCallFactory(c.getTypeId()).newInstance();
               double newServ = computeRemainingService(c.getServedTime(), call.getDefaultContactTime(g), g, c.getTypeId());
               call.setDefaultContactTime(g, newServ);
               call.setDefaultContactTime(newServ);
               
               cc.getAgentGroup(g).serve(call);
            }
         }
      }

      if (ss.isQueueSizesAvailable()) {
         int[] s = cc.getStartingState().getQueueSizes();
         for (int k = 0; k < s.length; k++) {
            for (int j = 0; j < s[k]; j++) {
               router.newContact(cc.getCallFactory(k).newInstance()); // assume exponential dist
            }
         }
      }
      else if (ss.isDetailedQueuesAvailable()) {
         for (int k = 0; k < ss.getDetailedQueues().size(); k++) {
            StartingState.StartingWaitingQueue q = ss.getDetailedQueues().get(k);
            for (StartingState.WaitingCall c : q.getQueue()) {
               Contact call = cc.getCallFactory(k).newInstance();
               // compute new remaining patience time
               double newPat = computeRemainingPatience(c.getWaitedTime(), call.getDefaultPatienceTime(), k);
               call.setDefaultPatienceTime(newPat);
               
               router.newContact(call); // add to router
            }
         }
      }
   }
   
   /**
    * Generates the remaining patience time when initializing a non-empty waiting queue.
    * 
    * @param waitedTime the amount of time the call has already waited
    * @param currPat the current patience time generated. This value is used only to
    * retrieve the uniform variate, which will be used to generated the new patience time.
    * @param type the call type of the call
    * 
    * @return the remaining patience time
    */
   private double computeRemainingPatience(double waitedTime, double currPat, int type) {
      Distribution dist = cc.getCallFactory(type).getPatienceTimeGen().getGenerator(0).getDistribution();
      double u = dist.cdf(currPat); // get the uniform in order to re-use it
      double ut = dist.cdf(waitedTime); // get the conditional prob
      // compute the new uniform in the interval [ut , 1]
      double u2 = (1 - ut) * u + ut; // re-use uniform u
      // compute the new patience time conditional to be over waitedTime
      double newPat = dist.inverseF(u2);
      
      return Math.max(newPat - waitedTime, 0); // must subtract the time already waited
   }
   
   /**
    * Generates the remaining service time of a call already in service,
    * before the start of the simulation.
    * 
    * @param servedTime the amount of time the call has already been in service
    * @param currServ the current service time generated. This value is used only to
    * retrieve the uniform variate, which will be used to generate the new service time.
    * @param group the agent group that is serving this call
    * @param type the call type of the call
    * 
    * @return the remaining service time for this call by this agent group
    */
   private double computeRemainingService(double servedTime, double currServ, int group, int type) {
      Distribution dist = cc.getCallFactory(type).getServiceTimesManager().getServiceTimeGen(group).getGenerator(0).getDistribution();
      double u = dist.cdf(currServ); // get the uniform in order to re-use it
      double ut = dist.cdf(servedTime); // get the conditional prob
      // compute the new uniform in the interval [ut , 1]
      double u2 = (1 - ut) * u + ut; // re-use uniform u
      // compute the new service time conditional to be over servedTime
      double newServ = dist.inverseF(u2);
      
      return Math.max(newServ - servedTime, 0); // must subtract the time already waited
   }
   

   @Override
   public void addReplicationObs (int r) {
      stat.addObs ();
      stat.addObsRawStatistics ();
      cc.resetNextSubstream ();
   }

   public CallCenter getCallCenter () {
      return cc;
   }

   public RepSimParams getSimParams () {
      return simParams;
   }

   public void reset (PerformanceMeasureType... pms) {
      setTargetReplications (simParams.getMinReplications ());
      ccm.unregisterListeners ();
      cc.getPeriodChangeEvent ().removePeriodChangeListener (miu);
      ccm = new RepCMM (cc, this, simParams.isEstimateContactTypeAgentGroup (), pms);
      ccm.registerListeners ();
      stat = new SimCallCenterStat (cc, ccm, simParams.isKeepObs (), simParams.isNormalizeToDefaultUnit (), pms);
      miu = new IntegralMeasureUpdater (ccm);
      cc.getPeriodChangeEvent ().addPeriodChangeListener (miu);
      cc.setAwtPeriod (this);
   }

   /**
    * This method returns $P+2$, the number of periods.
    */
   public int getNumPeriodsForCounters () {
      return cc.getPeriodChangeEvent ().getNumPeriods ();
   }

   /**
    * This method returns $P'$, the number of segments
    * regrouping main periods.
    */
   public int getNumPeriodsForCountersAwt () {
      return cc.getNumMainPeriodsWithSegments();
   }

   public boolean needsSlidingWindows () {
      return false;
   }

   public boolean needsStatForPeriodSegmentsAwt () {
      return true;
   }

   public int getCompletedSteps () {
      return getCompletedReplications ();
   }

   public CallCenterMeasureManager getCallCenterMeasureManager () {
      return ccm;
   }

   private static DoubleMatrix2D toMainPeriods (DoubleMatrix2D m, DoubleDoubleFunction aggrFunc) {
      if (m.columns () == 1)
         return m;
      // Gets a matrix m2 with all the contents of m, except the first
      // and last column
      DoubleMatrix2D m2 = m.viewPart (0, 1, m.rows(), m.columns() - 2);
      if (aggrFunc != null) {
         // Adds the first column of m to the first column of m2
         m2.viewColumn (0).assign (m.viewColumn (0), aggrFunc);
         // Adds the last column of m to the last column of m2
         m2.viewColumn (m2.columns () - 1).assign (m.viewColumn (m.columns () - 1), aggrFunc);
      }
      return m2;
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

   private int getPeriod (Contact contact) {
      final Call call = (Call)contact;
      switch (simParams.getPerPeriodCollectingMode ()) {
      case PERIODOFENTRY:
         return call.getArrivalPeriod ();
      case PERIODOFEXIT:
         return call.getExitPeriod ();
      case PERIODOFBEGINSERVICEORENTRY:
         final int p = call.getBeginServicePeriod ();
         if (p < 0)
            return call.getArrivalPeriod ();
         return p;
      case PERIODOFBEGINSERVICEOREXIT:
         final int p2 = call.getBeginServicePeriod ();
         if (p2 < 0)
            return call.getExitPeriod ();
         return p2;
      }
      throw new AssertionError();
   }

   /**
    * By default, this returns the period of arrival of
    * the given contact.
    * However, this can be changed using
    * the \texttt{Per\-Period\-Collecting\-Mode} attribute
    * in the \texttt{rep\-Sim\-Params} parameter file.
    */
   public int getStatPeriod (Contact contact) {
      //return cc.getPeriodChangeEvent ().getMainPeriod (getPeriod (contact)) + 1;
      return getPeriod (contact);
   }

   /**
    * Returns the result of {@link #getAwtPeriod(Contact)}.
    */
   public int getStatPeriodAwt (Contact contact) {
      return getAwtPeriod (contact);
   }

   /**
    * Returns the index of the current period.
    */
   public int getStatPeriod () {
      return cc.getPeriodChangeEvent ().getCurrentPeriod ();
   }

   /**
    * Computes the statistical period $p$ of the contact by
    * calling {@link #getStatPeriod(Contact)}, and
    * converts $p$ to a main period using
    * {@link PeriodChangeEvent#getMainPeriod(int)}.
    */
   public int getAwtPeriod (Contact contact) {
      return cc.getPeriodChangeEvent ().getMainPeriod (getPeriod (contact));
   }

   /**
    * This returns $P'-1$.
    */
   public int getGlobalAwtPeriod () {
      return cc.getNumMainPeriodsWithSegments() - 1;
   }

   public boolean isSteadyState () {
      return false;
   }

   @Override
   public void initReplicationProbes () {
      getCallCenterStatProbes ().init ();
   }

   @Override
   public void init() {
      setMaxReplications (simParams.isSetMaxReplications () ? simParams.getMaxReplications () : Integer.MAX_VALUE);
      setMinReplications (1);
      setTargetReplications (1);
      setAborted (false);
      super.init();
   }

   public void simulate (int numSteps) {
      if (numSteps < 0)
         throw new IllegalArgumentException
         ("numSteps < 0");
      adjustTargetReplications (numSteps);
      if (getTargetReplications() == 0)
         return;
      while (getCompletedReplications() < getTargetReplications())
         performReplication (getCompletedReplications());
   }

   public void formatReport (Map<String, Object> evalInfo) {
      evalInfo.put
            (Messages.getString("RepLogic.NumReplications"), getCompletedSteps ());
   }

   public int[] getStaffing () {
      final int I = cc.getNumAgentGroups ();
      final int P = cc.getPeriodChangeEvent ().getNumMainPeriods ();
      final int[] numAgents = new int[I * P];
      for (int i = 0; i < I; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         final int[] staffing = group.getEffectiveStaffing ();
         for (int mp = 0; mp < P; mp++)
            numAgents[i * P + mp] = staffing == null ? 0 : staffing[mp];
      }
      return numAgents;
   }

   public void setStaffing (int[] numAgents) {
      final int I = cc.getNumAgentGroups ();
      final int P = cc.getPeriodChangeEvent ().getNumMainPeriods ();
      if (numAgents.length != I * P)
         throw new IllegalArgumentException (
               "Incompatible staffing vector dimensions");
      for (int i = 0; i < I; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         final int[] staffing = new int[P];
         for (int mp = 0; mp < P; mp++) {
            final int num = numAgents[i * P + mp];
            staffing[mp] = num;
         }
         if (group instanceof AgentGroupManagerWithStaffing) {
            AgentGroupManagerWithStaffing group2 = (AgentGroupManagerWithStaffing) group;
            group2.setEffectiveStaffing (staffing);
         }
         else
            throw new ClassCastException
            ("Agent group " + i + " uses a schedule; cannot set a staffing vector directly");
      }
   }

   public int[][] getStaffingMatrix () {
      final int I = cc.getNumAgentGroups ();
      final int P = cc.getPeriodChangeEvent ().getNumMainPeriods ();
      final int[][] numAgents = new int[I][P];
      for (int i = 0; i < I; i++) {
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         final int[] staffing = group.getEffectiveStaffing ();
         for (int mp = 0; mp < P; mp++)
            numAgents[i][mp] = staffing == null ? 0 : staffing[mp];
      }
      return numAgents;
   }

   public void setStaffingMatrix (int[][] numAgents) {
      final int I = cc.getNumAgentGroups ();
      final int P = cc.getPeriodChangeEvent ().getNumMainPeriods ();
      if (numAgents.length != I)
         throw new IllegalArgumentException (
               "Incompatible staffing matrix dimensions");
      for (int i = 0; i < I; i++) {
         if (numAgents[i] == null)
            continue;
         if (numAgents[i].length != P)
            throw new IllegalArgumentException
            ("Incompatible number of columns in staffing matrix");
         final AgentGroupManager group = cc.getAgentGroupManager (i);
         if (group instanceof AgentGroupManagerWithStaffing) {
            AgentGroupManagerWithStaffing group2 = (AgentGroupManagerWithStaffing) group;
            group2.setEffectiveStaffing (numAgents[i]);
         }
         else
            throw new ClassCastException
            ("Agent group " + i + " uses a schedule; cannot set a staffing vector directly");
      }
   }

   public int[][] getScheduledAgents () {
      int[][] na = new int[cc.getNumAgentGroups ()][];
      for (int i = 0; i < na.length; i++) {
         if (cc.getAgentGroupManager (i) instanceof AgentGroupManagerWithSchedule)
            na[i] = ((AgentGroupManagerWithSchedule)cc.getAgentGroupManager (i)).getEffectiveNumAgents ();
      }
      return na;
   }

   public void setScheduledAgents (int[][] ag) {
      if (ag.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("The given array must have length I=" + cc.getNumAgentGroups ());
      for (int i = 0; i < ag.length; i++) {
         if (ag[i] != null)
            if (cc.getAgentGroupManager (i) instanceof AgentGroupManagerWithSchedule)
               ((AgentGroupManagerWithSchedule)cc.getAgentGroupManager (i)).setEffectiveNumAgents (ag[i]);
            else
               throw new ClassCastException
                  ("Agent group " + i + " requires a staffing vector, not scheduling information");
      }
   }

   public int getCurrentMainPeriod () {
      throw new UnsupportedOperationException ();
   }

   public void setCurrentMainPeriod (int mp) {
      throw new UnsupportedOperationException ();
   }

   public boolean seemsUnstable () {
      return false;
   }

   private static void scheduleSwitch (CallCenter cc, ToggleElement el,
         double[] times) {
      double[] usedTimes;
      if (times == null) {
         final double start = cc.getPeriodChangeEvent ().getPeriodStartingTime (1);
         final double end = cc.getPeriodChangeEvent ().getPeriodEndingTime (cc.getNumMainPeriods ());
         usedTimes = new double[] { start, end };
      }
      else
         usedTimes = times;
      final SwitchEvent ev = new SwitchEvent (cc.simulator (), el, usedTimes);
      ev.setPriority (PeriodChangeEvent.PRIORITY * 2);
      ev.schedule ();
   }

   public boolean isVerbose () {
      return base.isVerbose ();
   }

   public void setVerbose (boolean verbose) {
      base.setVerbose (verbose);
   }

   public boolean isAborted() {
      return base.isAborted ();
   }

   public void setAborted (boolean aborted) {
      base.setAborted (aborted);
   }

   public void addSimLogicListener (SimLogicListener l) {
      base.addSimLogicListener (l);
   }

   public void clearSimLogicListeners () {
      base.clearSimLogicListeners ();
   }

   public List<SimLogicListener> getSimLogicListeners () {
      return base.getSimLogicListeners ();
   }

   public void removeSimLogicListener (SimLogicListener l) {
      base.removeSimLogicListener (l);
   }

   private static final class IntegralMeasureUpdater implements
         PeriodChangeListener {
      private CallCenterMeasureManager ccm;

      public IntegralMeasureUpdater (CallCenterMeasureManager ccm) {
         this.ccm = ccm;
      }

      public void changePeriod (PeriodChangeEvent pce) {
         ccm.updateCurrentPeriod ();
      }

      public void stop (PeriodChangeEvent pce) {
         ccm.finishCurrentPeriod ();
      }
   }

   private class RepCMM extends CallCenterMeasureManager {
      public RepCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, Collection<MeasureType> measures) {
         super (cc, statP, contactTypeAgentGroup, measures);
      }

      public RepCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, PerformanceMeasureType[] pms) {
         super (cc, statP, contactTypeAgentGroup, pms);
      }

      public RepCMM (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup) {
         super (cc, statP, contactTypeAgentGroup);
      }

      /**
       * This method returns $P'$, the number of segments
       * regrouping main periods.
       */
      public int getNumPeriodsForStatProbes () {
         return cc.getNumMainPeriodsWithSegments ();
      }

      /**
       * Constructs a matrix of observations from the current values of
       * counters for measure type \texttt{mt}.
       * This method first gets a $R\times P+2$ matrix $M_1$ of double-precision
       * values from the {@link MeasureMatrix} object
       * in {@link #getCallCenterMeasureManager()}, using
       * {@link RepSimCC#getReplicationValues(MeasureMatrix)}.
       * For most measure types, the first and last rows of this
       * matrix are then removed to get a $R\times P$ matrix $M_2$.
       * The values in the first and last columns of $M_1$
       * are then summed up with the values in the first and last colums of
       * $M_2$.
       * The resulting matrix has one column per main period.
       * The method {@link SegmentInfo#addColumnSegments(DoubleMatrix2D,DoubleDoubleFunction,SegmentInfo[])}
       * is used on this matrix to add extra columns for segments
       * regrouping several main periods.
       * This gives a $R\times P'$ matrix which is returned.
       *
       * For measure types depending on an acceptable waiting time,
       * matrices of counters already have $P'$ columns so the columns
       * are not transformed in this case.
       * This exception is due to the fact that columns in such matrix
       * can be computed with different acceptable waiting times, and thus
       * cannot be aggregated in general.
       */
      public DoubleMatrix2D getValues (MeasureType mt, boolean norm) {
         final MeasureMatrix mat = ccm.getMeasureMatrix (mt);
         // Most matrices of measures contains one column per period.
         // But we need to have P' columns, one column per segment of main periods
         DoubleMatrix2D m = RepSimCC.getReplicationValues (mat);
         switch (mt) {
         case NUMBUSYAGENTS:
         case NUMWORKINGAGENTS:
         case NUMSCHEDULEDAGENTS:
         case QUEUESIZE:
            if (m.columns() > 1)
               // Retain only main periods in m, and add columns for the implicit segment
               // regrouping all main periods, as well as user-defined segments.
               m = SegmentInfo.addColumnSegments
               (toMainPeriods (m, null),
                     mt.getAggregationFunction (),
                     cc.getMainPeriodSegments ());
            break;
         case SUMSERVED:
         case NUMSERVEDBEFOREAWT:
         case NUMSERVEDAFTERAWT:
         case NUMABANDONEDBEFOREAWT:
         case NUMABANDONEDAFTERAWT:
         case SUMEXCESSTIMESABANDONED:
         case SUMEXCESSTIMESSERVED:
            // The matrix contains P' columns, so do not transform it
            break;
         default: // Preliminary and wrap-up periods are ignored
            // for the overall statistics.
            // This way, the optional normalization factor
            // is the same for each replication; this simplifies
            // the application of control variates.
            if (m.columns() > 1)
               // Retain only main periods in m, and add columns for the implicit segment
               // regrouping all main periods, as well as user-defined segments.
               m = SegmentInfo.addColumnSegments
               (toMainPeriods (m, mt.getAggregationFunction ()),
                     mt.getAggregationFunction (),
                     cc.getMainPeriodSegments ());
         }
         if (m.columns () == 2)
            m = m.viewPart (0, 0, m.rows (), 1);

         if (norm)
            timeNormalize (mt, m);
         return m;
      }

      public void timeNormalize (MeasureType mt, DoubleMatrix2D m) {
         final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
         boolean norm = false;
         switch (mt.getTimeNormalizeType ()) {
         case NEVER:
            return;
         case ALWAYS:
            norm = true;
            break;
         case CONDITIONAL:
            norm = simParams.isNormalizeToDefaultUnit ();
         }
         if (norm) {
            if (mt == MeasureType.SUMSERVED)
               // Divide everything by t_P - t_0,
               // the total duration of main periods.
               m.assign (Functions.div (pce
                     .getPeriodEndingTime (
                           pce.getNumPeriods () - 2)
                           - pce.getPeriodEndingTime (0)));
            else
               timeNormalize (m);
         }
      }

      private void timeNormalize (DoubleMatrix2D m) {
         final int P = cc.getNumMainPeriods ();
         final int Ps = cc.getNumMainPeriodSegments ();
         final int Pp = cc.getNumMainPeriodsWithSegments ();
         final double periodDuration = cc.getPeriodDuration ();
         if (m.columns () != Pp)
            throw new IllegalArgumentException
            ("The matrix has " + m.columns () + " columns, but it needs " + Pp + " columns");
         // The first P columns correspond to main periods, so
         // we just divide by the period duration.
         m.viewPart (0, 0, m.rows (), P).assign (Functions.div (periodDuration));
         if (P > 1) {
            // The last P'-P columns correspond to segments
            // regrouping one or more main periods, so
            // we need to multiply the period duration with
            // the number of periods in the segment
            // to get the normalization factors.

            // This is the implicit segment regrouping all main periods,
            // i.e., the last column of the matrix
            m.viewColumn (m.columns () - 1).assign (Functions.div (periodDuration * cc.getNumMainPeriods ()));
            for (int ps = 0; ps < Ps; ps++) {
               // This is a user-defined segment
               final double d = periodDuration * cc.getMainPeriodSegment (ps).getNumValues ();
               m.viewColumn (P + ps).assign (Functions.div (d));
            }
         }
      }
   }
}
