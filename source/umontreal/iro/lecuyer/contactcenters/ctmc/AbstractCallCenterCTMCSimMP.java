package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import umontreal.iro.lecuyer.contactcenters.app.AbstractContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimWithObservations;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.CTMCRepSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.contactcenters.app.trace.ContactTrace;
import umontreal.iro.lecuyer.contactcenters.app.trace.FileContactTrace;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSimUtil;
import umontreal.iro.lecuyer.contactcenters.msk.Messages;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.ssj.util.Chrono;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

public abstract class AbstractCallCenterCTMCSimMP extends
      AbstractContactCenterSim implements ContactCenterSimWithObservations {
   private Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.ctmc");
   protected CallCenter cc;
   protected CallCenterCTMC[] ctmc;
   protected CallCenterCounters[] counters;
   protected CallCenterCounters countersG;
   protected CallCenterStat[] ccStat;
   protected CallCenterStatMP ccStatMP;
   protected ContactTrace trace;

   protected double[][] awt;
   protected double[] jumpRate;
   protected int[] startingTransition;
   protected RateChangeTransitions rateChange;

   private RandomStream stream;
   protected CTMCRepSimParams simParams;
   private Chrono timer;
   private double cpuTime;
   private int numSteps;
   private final List<TransitionListener> listeners = new ArrayList<TransitionListener>();
   private final List<TransitionListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;

   public AbstractCallCenterCTMCSimMP (CallCenterParams ccParams,
         CTMCRepSimParams simParams)
         throws CallCenterCreationException, CTMCCreationException {
      RandomStreams streams = new RandomStreams (new SimRandomStreamFactory (simParams.getRandomStreams ()), ccParams);
      this.cc = new CallCenter (ccParams, streams);
      cc.create ();
      cc.initSim ();
      this.simParams = simParams;
      init ();
   }

   public AbstractCallCenterCTMCSimMP (CallCenterParams ccParams,
         CTMCRepSimParams simParams,
         RandomStreams streams)
         throws CallCenterCreationException, CTMCCreationException {
      this.cc = new CallCenter (ccParams, streams);
      cc.create ();
      cc.initSim ();
      this.simParams = simParams;
      init ();
   }

   public AbstractCallCenterCTMCSimMP (CallCenter cc, CTMCRepSimParams simParams)
         throws CTMCCreationException {
      this.cc = cc;
      cc.initSim ();
      this.simParams = simParams;
      init ();
   }

   private void init () throws CTMCCreationException {
      stream = cc.getRandomStreams ().getStreamCT ();
      final boolean keepQueues = simParams.isKeepQueues ();
      final int numPeriods = cc.getNumMainPeriods();
      ctmc = new CallCenterCTMC[numPeriods + 1];
      jumpRate = new double[ctmc.length];
      startingTransition = new int[ctmc.length];
      final int Pp = numPeriods > 1 ? numPeriods + 1 : numPeriods;
      counters = new CallCenterCounters[ctmc.length];
      ccStat = new CallCenterStat[Pp];
      if (keepQueues) {
         awt = new double[Pp][];
         for (int mp = 0; mp < Pp; mp++)
            awt[mp] = AbstractCallCenterCTMCSim.getAWT (cc, mp);
      }
      final int ns = cc.getNumMatricesOfAWT ();
      final double[] gawt = numPeriods > 1 ? awt[awt.length - 1] : null;
      int[] maxNumAgents = simParams.getMaxNumAgents ();
      if (numPeriods > 1) {
         if (maxNumAgents.length == 0)
            maxNumAgents = new int[cc.getNumAgentGroups ()];
         else if (maxNumAgents.length == 1) {
            final int m = maxNumAgents[0];
            maxNumAgents = new int[cc.getNumAgentGroups ()];
            Arrays.fill (maxNumAgents, m);
         }
         else if (maxNumAgents.length != cc.getNumAgentGroups ())
            throw new IllegalArgumentException();
         for (int i = 0; i < maxNumAgents.length; i++) {
            int[] staffing = cc.getAgentGroupManager (i).getEffectiveStaffing ();
            final int m = ArrayUtil.max (staffing);
            if (maxNumAgents[i] < m)
               maxNumAgents[i] = m;
         }
      }
      rateChange = new RateChangeTransitions (cc);
      for (int mp = 0; mp < numPeriods; mp++) {
         int[][] thresholds;
         if (simParams.getThresholds ().isEmpty ())
            thresholds = null;
         else if (simParams.getThresholds ().size () == 1)
            thresholds = ArrayConverter.unmarshalArray (simParams.getThresholds ().get (0));
         else
            thresholds = ArrayConverter.unmarshalArray (simParams.getThresholds ().get (mp));
         ctmc[mp] = AbstractCallCenterCTMCSim.getCTMC (cc, mp, keepQueues, maxNumAgents,
               simParams.getCallTrace () != null, thresholds,
               simParams.isAlwaysUseIndexedSearch ());
         jumpRate[mp] = ctmc[mp].getJumpRate ();
         counters[mp] = new CallCenterCounters (ctmc[mp], awt[mp], gawt, false, jumpRate, startingTransition, mp);
         ccStat[mp] = new CallCenterStat (ctmc[mp], awt[mp] == null ? 0 : ns, simParams.isKeepObs());
      }
      ctmc[ctmc.length - 1] = ctmc[ctmc.length - 2].clone ();
      double[] zrates = new double[ctmc[ctmc.length - 1].getNumContactTypes ()];
      ctmc[ctmc.length - 1].setArrivalRates (zrates);
      ctmc[ctmc.length - 1].setMaxArrivalRates (zrates);
      jumpRate[jumpRate.length - 1] = ctmc[ctmc.length - 1].getJumpRate ();
      counters[counters.length - 1] = new CallCenterCounters (ctmc[ctmc.length - 1],
            awt[numPeriods - 1], gawt, true, jumpRate, startingTransition, numPeriods);

      countersG = new CallCenterCounters (ctmc[0], awt[0], gawt,
            false,
            jumpRate, startingTransition, 0);
      if (numPeriods > 1)
         ccStat[numPeriods] = new CallCenterStat (ctmc[0], awt[0] == null ? 0 : ns, simParams.isKeepObs ());
      ccStatMP = new CallCenterStatMP (ctmc, ccStat);
      trace = FileContactTrace.create (simParams.getCallTrace ());
   }

   public int[] getStaffing() {
      final int numPeriods = ctmc.length - 1;
      int[] staffing = new int[numPeriods*ctmc[0].getNumAgentGroups()];
      for (int mp = 0; mp < numPeriods; mp++)
         for (int i = 0; i < ctmc[mp].getNumAgentGroups (); i++)
            staffing[numPeriods*i + mp] = ctmc[mp].getNumAgents (i);
      return staffing;
   }

   public void setStaffing (int[] staffing) {
      final int numPeriods = ctmc.length - 1;
      if (staffing.length != numPeriods*ctmc[0].getNumAgentGroups())
         throw new IllegalArgumentException
         ("Invalid length of staffing vector");
      final int[] maxNumAgents = ctmc[ctmc.length - 1].getMaxNumAgentsArray();
      boolean maxChanged = false;
      for (int mp = 0; mp < numPeriods; mp++) {
         for (int i = 0; i < ctmc[mp].getNumAgentGroups (); i++) {
            final int s = staffing[numPeriods*i + mp];
            if (maxNumAgents[i] < s) {
               maxChanged = true;
               maxNumAgents[i] = s;
            }
         }
      }
      for (int mp = 0; mp < numPeriods; mp++) {
         if (maxChanged)
            ctmc[mp].setMaxNumAgents (maxNumAgents);
         for (int i = 0; i < ctmc[mp].getNumAgentGroups (); i++)
            ctmc[mp].setNumAgents (i, staffing[numPeriods*i + mp]);
      }
   }

   public int[][] getStaffingMatrix() {
      final int numPeriods = ctmc.length - 1;
      int[][] staffing = new int[ctmc[0].getNumAgentGroups()][numPeriods];
      for (int i = 0; i < staffing.length; i++)
         for (int mp = 0; mp < numPeriods; mp++)
            staffing[i][mp] = ctmc[mp].getNumAgents (i);
      return staffing;
   }

   public void setStaffingMatrix (int[][] staffing) {
      final int numPeriods = ctmc.length - 1;
      if (staffing.length != ctmc[0].getNumAgentGroups())
         throw new IllegalArgumentException
         ("Invalid length of staffing vector");
      final int[] maxNumAgents = ctmc[ctmc.length - 1].getMaxNumAgentsArray();
      boolean maxChanged = false;
      for (int mp = 0; mp < numPeriods; mp++) {
         for (int i = 0; i < ctmc[mp].getNumAgentGroups (); i++) {
            if (staffing[i] == null)
               continue;
            final int s = staffing[i][mp];
            if (maxNumAgents[i] < s) {
               maxChanged = true;
               maxNumAgents[i] = s;
            }
         }
      }
      for (int mp = 0; mp < numPeriods; mp++) {
         if (maxChanged)
            ctmc[mp].setMaxNumAgents (maxNumAgents);
         for (int i = 0; i < ctmc[mp].getNumAgentGroups (); i++)
            if (staffing[i] != null)
               ctmc[mp].setNumAgents (i, staffing[i][mp]);
      }
   }

   public int getQueueCapacity() {
      return ctmc[0].getQueueCapacity ();
   }

   public void setQueueCapacity (int q) {
      for (int mp = 0; mp < ctmc.length; mp++)
         ctmc[mp].setQueueCapacity (q);
   }

   public CallCenterCTMC getCTMC (int p) {
      return ctmc[p];
   }

   protected void initStat () {
      ccStatMP.init ();
      if (ctmc.length > 2)
         ccStat[ccStat.length - 1].initLambda (ctmc);
   }

   public CallCenterStatMP getStat() {
      return ccStatMP;
   }

   public void setStat (CallCenterStatMP ccStatMP) {
      this.ccStatMP = ccStatMP;
   }

   protected void initReplication (RandomStream stream1, int[] ntr) {
      for (int mp = 0; mp < ntr.length; mp++)
         counters[mp].init (ctmc[mp], cc.getPeriodDuration (), ntr[mp]);
      counters[counters.length - 1].init (ctmc[ctmc.length - 1], cc.getPeriodDuration (), 0);
      countersG.init (ctmc[0], cc.getPeriodDuration (), ntr[0]);
   }

   protected void addObs () {
      final double periodDuration = cc.getPeriodDuration ();
      for (int mp = 0; mp < ctmc.length - 2; mp++)
         ccStat[mp].addObs (counters[mp], periodDuration);
      countersG.collectSum (false, false, counters[counters.length - 2], counters[counters.length - 1]);
      ccStat[ctmc.length - 2].addObs (countersG, periodDuration);
      if (ctmc.length > 2) {
         countersG.collectSum (false, true, counters);
         ccStat[ccStat.length - 1].addObs (countersG, periodDuration*cc.getNumMainPeriods ());
      }
   }

   public void formatReport() {
      if (cpuTime > 0) {
         DatatypeFactory df;
         try {
            df = DatatypeFactory.newInstance();
         }
         catch (DatatypeConfigurationException dce) {
            df = null;
         }
         if (df != null)
            getEvalInfo ().put (Messages.getString ("CallCenterSimUtil.TotalCPUTime"),
                  df.newDuration ((long)(cpuTime * 1000)));
      }
      getEvalInfo().put ("Per-period expected numbers of transitions",
            getNumExpectedTransitions ());
      //ccStat.formatReport (getEvalInfo(), getNumExpectedTransitions());
      ccStatMP.formatReport (getEvalInfo(), getNumExpectedTransitions());
   }

   public abstract void simulate (RandomStream stream1, int n);

   public abstract double[] getNumExpectedTransitions ();

   public void initTimer () {
      if (simParams.isEnableChrono () && timer == null)
         try {
            timer = Chrono.createForSingleThread ();
         }
         catch (final UnsatisfiedLinkError err) {
            logger
                  .log (
                        Level.WARNING,
                        "Could not create an instance of Chrono, missing ssjutil native library",
                        err);
         }
         catch (final SecurityException se) {
            logger
                  .log (
                        Level.WARNING,
                        "Could not create an instance of ChronoSingleThread, because of a security error",
                        se);
         }
      else if (!simParams.isEnableChrono () && timer != null)
         timer = null;
      if (timer == null && !simParams.isSetCpuTimeLimit ())
         logger
               .warning ("The CPU time limit cannot be enforced, because the Chrono was disabled");
      if (timer != null)
         timer.init ();
   }

   public int getRequiredNewSteps () {
      int nb;
      if (getOneSimDone () || !simParams.isSetSequentialSampling ())
         nb = 0;
      else
         nb = CallCenterSimUtil.getRequiredNewSteps (ccStatMP.getMatricesOfStatProbes(), simParams
               .getSequentialSampling (), isVerbose ());
      // final SimStoppingCondition scond = getSimStoppingCondition ();
      // if (scond != null)
      // nb = scond.check (this, nb);
      final int tnb = getCompletedSteps () + nb;
      int maxSteps;
      if (simParams.isSetMaxReplications ())
         maxSteps = simParams.getMaxReplications ();
      else
         maxSteps = Integer.MAX_VALUE;
      if (tnb >= maxSteps) {
         if (isVerbose ())
            logger.info ("Maximum number of steps reached");
         nb = maxSteps - getCompletedSteps ();
         if (nb < 0)
            nb = 0;
      }
      if (simParams.isSetCpuTimeLimit ())
         nb = CallCenterSimUtil.checkCpuTimeLimit (timer == null ? 0 : timer
               .getSeconds (), simParams.getCpuTimeLimit ().getTimeInMillis (
               new Date ()) / 1000, getCompletedSteps (), nb, isVerbose ());
      return nb;
   }

   public void eval () {
      initStat ();
      initTimer ();
      if (trace != null)
         trace.init ();
      try {
         int minSteps = simParams.getMinReplications ();
         if (getOneSimDone () && numSteps >= minSteps && !getSeqSampEachEval ())
            simulate (stream, numSteps);
         else {
            numSteps = 0;
            for (int nb = minSteps; nb > 0;) {
               simulate (stream, nb);
               numSteps += nb;
               nb = getRequiredNewSteps ();
            }
         }
      }
      finally {
         if (trace != null)
            trace.close ();
      }
      setOneSimDone (true);
      if (autoResetStartStream)
         stream.resetStartStream ();
      cpuTime = timer == null ? 0 : timer.getSeconds ();
      formatReport ();
      for (CallCenterCTMC c : ctmc)
         c.initEmpty ();
   }

   public int getCompletedSteps () {
      return numSteps;
   }

   public ReportParams getReportParams () {
      return simParams.getReport ();
   }

   public double getConfidenceLevel () {
      return getReportParams ().getConfidenceLevel ();
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      return ccStatMP.getMatrixOfStatProbes (m);
   }

   public void newSeeds () {
      try {
         cc.create (true);
         init ();
         setOneSimDone (false);
      }
      catch (CallCenterCreationException cce) {
         throw new IllegalStateException ("Cannot perform reset");
      }
      catch (CTMCCreationException cce) {
         throw new IllegalStateException ("Cannot perform reset");
      }
   }

   public void resetNextSubstream () {
      stream.resetNextSubstream ();
   }

   public void resetStartStream () {
      stream.resetStartStream ();
   }

   public void resetStartSubstream () {
      stream.resetStartSubstream ();
   }

   public void setConfidenceLevel (double level) {
      getReportParams ().setConfidenceLevel (level);
   }

   public TimeUnit getDefaultUnit () {
      return cc.getDefaultUnit ();
   }

   public Object getEvalOption (EvalOptionType option) {
      switch (option) {
      case STAFFINGVECTOR:
         return getStaffing();
      case STAFFINGMATRIX:
         return getStaffingMatrix();
      case QUEUECAPACITY:
         return getQueueCapacity();
      default:
         throw new NoSuchElementException ("No such evaluation option");
      }
   }

   public EvalOptionType[] getEvalOptions () {
      return new EvalOptionType[] {
            EvalOptionType.STAFFINGVECTOR,
            EvalOptionType.STAFFINGMATRIX,
            EvalOptionType.QUEUECAPACITY
      };
   }

   public int getNumAgentGroups () {
      return ctmc[0].getNumAgentGroups ();
   }

   public int getNumContactTypes () {
      return ctmc[0].getNumContactTypes ();
   }

   public int getNumInContactTypes () {
      return ctmc[0].getNumContactTypes ();
   }

   public int getNumMainPeriods () {
      return ctmc.length - 1;
   }

   @Override
   public String getMainPeriodName (int mp) {
      return cc.getMainPeriodName (mp);
   }

   public int getNumMatricesOfAWT () {
      return awt == null ? 0 : cc.getNumMatricesOfAWT ();
   }

   @Override
   public String getMatrixOfAWTName (int m) {
      if (awt == null)
         return null;
      return cc.getMatrixOfAWTName (m);
   }

   public int getNumOutContactTypes () {
      return 0;
   }

   public int getNumWaitingQueues () {
      return cc.getNumContactTypes ();
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      return ccStatMP.getPerformanceMeasures();
   }

   public boolean hasEvalOption (EvalOptionType option) {
      return option == EvalOptionType.STAFFINGVECTOR ||
      option == EvalOptionType.STAFFINGMATRIX ||
      option == EvalOptionType.QUEUECAPACITY;
   }

   public void reset () {
      try {
         cc.create (false);
         init ();
         setOneSimDone (false);
      }
      catch (CallCenterCreationException cce) {
         throw new IllegalStateException ("Cannot perform reset");
      }
      catch (CTMCCreationException cce) {
         throw new IllegalStateException ("Cannot perform reset");
      }
   }

   public boolean seemsUnstable () {
      return false;
   }

   public void setEvalOption (EvalOptionType option, Object value) {
      switch (option) {
      case STAFFINGVECTOR:
         setStaffing ((int[])value);
         break;
      case STAFFINGMATRIX:
         setStaffingMatrix ((int[][])value);
         break;
      case QUEUECAPACITY:
         setQueueCapacity ((Integer)value);
         break;
      default:
         throw new NoSuchElementException
         ("Evaluation option not available");
      }
   }

   @Override
   public String getAgentGroupName (int i) {
      return cc.getAgentGroupName (i);
   }

   @Override
   public Map<String, String> getAgentGroupProperties (int i) {
      return cc.getAgentGroupProperties (i);
   }

   @Override
   public String getContactTypeName (int k) {
      return cc.getContactTypeName (k);
   }

   @Override
   public Map<String, String> getContactTypeProperties (int k) {
      return cc.getContactTypeProperties (k);
   }

   @Override
   public String getWaitingQueueName (int q) {
      return getContactTypeName (q);
   }

   @Override
   public Map<String, String> getWaitingQueueProperties (int q) {
      return getContactTypeProperties (q);
   }

   public double[] getObs (PerformanceMeasureType pm, int row, int column) {
      MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      Tally ta = mta.get (row, column);
      if (!(ta instanceof TallyStore))
         throw new NoSuchElementException
         ("The simulator does not keep track of observations");
      return CallCenterSimUtil.getObs ((TallyStore)ta);
   }

   public int numberObs (PerformanceMeasureType pm, int row, int column) {
      MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      return mta.numberObs ();
   }

   public void addTransitionListener (TransitionListener listener) {
      if (listener == null)
         throw new NullPointerException ("The given listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains (listener))
         listeners.add (listener);
   }

   public void removeTransitionListener (TransitionListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.remove (listener);
   }

   public void clearTransitionListeners() {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.clear();
   }

   public List<TransitionListener> getTransitionListeners() {
      return umListeners;
   }

   public void notifyInit (int r, int mp, CallCenterCTMC ctmc1) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).init (ctmc1, r, mp);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   public void notifyTransition (int r, int mp, CallCenterCTMC ctmc1, TransitionType type) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).newTransition (ctmc1, r, mp, type);
      }
      finally {
         broadcastInProgress = old;
      }
   }
}
