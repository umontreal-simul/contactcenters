package umontreal.iro.lecuyer.contactcenters.msk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.AbstractContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimListener;
import umontreal.iro.lecuyer.contactcenters.app.ControlVariableType;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.ObservableContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.SimStoppingCondition;
import umontreal.iro.lecuyer.contactcenters.app.params.BatchSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.CallTraceParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ControlVariableParams;
import umontreal.iro.lecuyer.contactcenters.app.params.RepSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.app.trace.ContactTrace;
import umontreal.iro.lecuyer.contactcenters.app.trace.FileContactTrace;
import umontreal.iro.lecuyer.contactcenters.msk.cv.CVCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.cv.ControlVariable;
import umontreal.iro.lecuyer.contactcenters.msk.cv.NumArrivalsCV;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.BatchMeansLogic;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.RepLogic;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogicListener;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.ChainCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simexp.BatchMeansSim;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.ssj.util.Chrono;
import umontreal.ssj.util.TimeUnit;

public abstract class AbstractCallCenterSim extends AbstractContactCenterSim
      implements ObservableContactCenterSim {
   protected Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.msk.sim");
   private PerformanceMeasureType[] pms;
   private CallCenter cc;
   private SimLogic simLogic;
   private Chrono timer;
   private SimStoppingCondition scond;
   private double cpuTime;
   private final List<ContactCenterSimListener> listeners = new ArrayList<ContactCenterSimListener>();
   private final List<ContactCenterSimListener> umListeners = Collections.unmodifiableList (listeners);
   private CVCallCenterStat cvStat;
   private CallCenterStatProbes ccStat;
   private CallTracer tracer;
   private boolean initialized = false;
   protected CallCenterMeasureManager ccm;                     //Ajouter

   /**
    * Constructs a new call center simulator using call center parameters
    * \texttt{ccParams}, and simulation parameters \texttt{simParams}.
    *
    * This calls {@link #createModel} to create the model,
    * {@link #createSimLogic} to create the simulation logic.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    */
   public AbstractCallCenterSim (CallCenterParams ccParams, SimParams simParams) throws CallCenterCreationException {
      this (new Simulator(), ccParams, simParams);
   }

   /**
    * Constructs a new call center simulator using call center parameters
    * \texttt{ccParams}, simulation parameters \texttt{simParams}, and random
    * streams \texttt{streams}.
    *
    * This calls {@link #createModel} to create the model,
    * {@link #createSimLogic} to create the simulation logic.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    * @param streams
    *           the random streams used by the simulator.
    */
   public AbstractCallCenterSim (CallCenterParams ccParams,
         SimParams simParams, RandomStreams streams) throws CallCenterCreationException {
      this (new Simulator(), ccParams, simParams, streams);
   }

   /**
    * Similar to {@link #AbstractCallCenterSim(CallCenterParams,SimParams)},
    * with the given simulator \texttt{sim}.
    */
   public AbstractCallCenterSim (Simulator sim, CallCenterParams ccParams, SimParams simParams) throws CallCenterCreationException {
      this (sim, ccParams, simParams, new RandomStreams (
            new SimRandomStreamFactory (simParams.getRandomStreams()),
            ccParams));
   }

   /**
    * Similar to {@link #AbstractCallCenterSim(CallCenterParams,SimParams,RandomStreams)},
    * with the given simulator \texttt{sim}.
    */
   public AbstractCallCenterSim (Simulator sim, CallCenterParams ccParams,
         SimParams simParams, RandomStreams streams) throws CallCenterCreationException {
      if (ccParams == null || simParams == null)
         throw new NullPointerException ();
      cc = createModel (sim, ccParams, streams);
      pms = CallCenterSimUtil.initPerformanceMeasures (simParams);
      if (!cc.isVirtualHoldSupported ())
         pms = CallCenterSimUtil.removeVQ (pms);
      this.simLogic = createSimLogic (cc, simParams);
   }

   @Override
   public ReportParams getReportParams() {
      return getSimLogic().getSimParams ().getReport();
   }

   @Override
   public void setReportParams (ReportParams reportParams) {
      getSimLogic().getSimParams ().setReport (reportParams);
   }

   public TimeUnit getDefaultUnit() {
      final TimeUnit unit = cc.getDefaultUnit();
      if (unit == null)
         return TimeUnit.HOUR;
      else
         return unit;
   }

   protected void init() {
      // This must not be called from the constructor.
      ccStat = simLogic.getCallCenterStatProbes();
      initControlVariableSupport();
   }

   /**
    * Use {@link #getCallCenter()} instead.
    */
   @Deprecated
   public CallCenter getModel () {
      return cc;
   }

   /**
    * Returns a reference to the model used by this simulator.
    *
    * @return a reference to the model.
    */
   public CallCenter getCallCenter () {
      return cc;
   }

   /**
    * Returns a reference to the simulation logic used by this simulator.
    *
    * @return a reference to the simulation logic.
    */
   public SimLogic getSimLogic () {
      return simLogic;
   }

   /**
    * Constructs and returns the model of the call center used by this
    * simulator. By default, this method constructs an instance of the
    * {@link CallCenter} class, calls the {@link CallCenter#create} method, and returns
    * the resulting model object.
    *
    * @param ccPs
    *           the parameters of the call center.
    * @param streams
    *           the random streams.
    * @return the constructed model.
    */
   protected CallCenter createModel (Simulator sim, CallCenterParams ccPs, RandomStreams streams) throws CallCenterCreationException {
      final CallCenter model = new CallCenter (sim, ccPs, streams);
      model.create (false);
      return model;
   }

   /**
    * Constructs and returns a {@link SimLogic} implementation for the
    * simulation logic, using the given \texttt{model} and simulation parameters
    * \texttt{simParams}.
    *
    * By default, this method creates a {@link RepLogic} instance if
    * \texttt{simParams} is an instance of {@link RepSimParams}, a
    * {@link BatchMeansLogic} if \texttt{simParams} is an instance of
    * {@link BatchSimParams}, and throws an exception otherwise.
    *
    * @param model
    *           the simulation model.
    * @param simParams
    *           the simulation parameters.
    * @return the simulation logic.
    */
   protected SimLogic createSimLogic (CallCenter model, SimParams simParams) {
      final SimLogic sim;
      if (simParams instanceof RepSimParams)
      { sim = new RepLogic (model, (RepSimParams) simParams, pms);                                    //initialisation du BatchMeanslogic
        this.ccm= sim.getCallCenterMeasureManager();                                          // Ajouter
      }
      else if (simParams instanceof BatchSimParams)
      {  sim = new BatchMeansLogic (model, (BatchSimParams) simParams, pms);                  //initialisation du BatchMeanslogic
         this.ccm=  sim.getCallCenterMeasureManager();                                  //Ajouter
      }
      else
         throw new IllegalArgumentException (
               "Invalid class of simulation parameters");
      sim.addSimLogicListener (new SLListener());
      return sim;
   }

   public CallCenterMeasureManager getCallCenterMeasureManager(){         //Ajouter
	   return ccm;
   }
   public void initTimer () {
      final SimParams simParams = simLogic.getSimParams ();
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
      if (timer == null && !simParams.isSetCpuTimeLimit())
         logger
               .warning ("The CPU time limit cannot be enforced, because the Chrono was disabled");
      if (timer != null)
         timer.init ();
   }

   public void initTrace() {
      final SimParams simParams = simLogic.getSimParams ();
      CallTraceParams traceParams = simParams.getCallTrace ();
      if (traceParams == null && tracer != null) {
         tracer.unregister ();
         tracer = null;
      }
      else if (traceParams != null && tracer == null) {
         ContactTrace trace = FileContactTrace.create (traceParams);
         tracer = new CallTracer (simLogic, trace);
         tracer.register ();
      }
      if (tracer != null) {
         final ContactTrace trace = tracer.getContactTrace ();
         trace.init ();
      }
   }

   @Override
   public boolean isVerbose () {
      return simLogic.isVerbose ();
   }

   @Override
   public void setVerbose (boolean verbose) {
      simLogic.setVerbose (verbose);
   }

   public double getConfidenceLevel () {
      return getReportParams().getConfidenceLevel ();
   }

   public void setConfidenceLevel (double level) {
      getReportParams().setConfidenceLevel (level);
   }

   @Override
   public String getAgentGroupName (int i) {
      return cc.getAgentGroupName (i);
   }

   @Override
   public String getContactTypeName (int k) {
      return cc.getContactTypeName (k);
   }

   public int getNumMatricesOfAWT () {
      return cc.getNumMatricesOfAWT ();
   }

   @Override
   public String getMatrixOfAWTName (int m) {
      return cc.getMatrixOfAWTName (m);
   }

   public EvalOptionType[] getEvalOptions () {
      return new EvalOptionType[] { EvalOptionType.STAFFINGVECTOR,
            EvalOptionType.STAFFINGMATRIX,
            EvalOptionType.SCHEDULEDAGENTS,
            EvalOptionType.QUEUECAPACITY,
            EvalOptionType.CURRENTPERIOD, EvalOptionType.SIMSTOPPINGCONDITION };
   }

   public boolean hasEvalOption (EvalOptionType option) {
      return option == EvalOptionType.STAFFINGVECTOR
      || option == EvalOptionType.STAFFINGMATRIX
      || option == EvalOptionType.SCHEDULEDAGENTS
      || option == EvalOptionType.QUEUECAPACITY
      || option == EvalOptionType.SIMSTOPPINGCONDITION
      || option == EvalOptionType.CURRENTPERIOD;
   }

   public Object getEvalOption (EvalOptionType option) {
      switch (option) {
      case STAFFINGVECTOR:
         return simLogic.getStaffing ();
      case STAFFINGMATRIX:
         return simLogic.getStaffingMatrix ();
      case SCHEDULEDAGENTS:
         return simLogic.getScheduledAgents();
      case CURRENTPERIOD:
         return new Integer (simLogic.getCurrentMainPeriod ());
      case QUEUECAPACITY:
         return simLogic.getCallCenter().getQueueCapacity();
      case SIMSTOPPINGCONDITION:
         return scond;
      default:
         throw new NoSuchElementException ("Evaluation option not available: "
               + option.toString ());
      }
   }

   public void setEvalOption (EvalOptionType option, Object value) {
      switch (option) {
      case STAFFINGVECTOR:
         simLogic.setStaffing ((int[]) value);
         break;
      case STAFFINGMATRIX:
         simLogic.setStaffingMatrix ((int[][]) value);
         break;
      case SCHEDULEDAGENTS:
         simLogic.setScheduledAgents ((int[][])value);
         break;
      case QUEUECAPACITY:
         simLogic.getCallCenter().setQueueCapacity ((Integer)value);
         break;
      case CURRENTPERIOD:
         simLogic.setCurrentMainPeriod (((Integer) value).intValue ());
         break;
      case SIMSTOPPINGCONDITION:
         setSimStoppingCondition ((SimStoppingCondition) value);
         break;
      default:
         throw new NoSuchElementException ("Evaluation option not available: "
               + option.toString ());
      }
   }

   public SimStoppingCondition getSimStoppingCondition() {
      return scond;
   }

   public void setSimStoppingCondition (SimStoppingCondition scond) {
      this.scond = scond;
   }

   public int getNumAgentGroups () {
      return cc.getNumAgentGroups ();
   }

   public int getNumContactTypes () {
      return cc.getNumContactTypes ();
   }

   public int getNumInContactTypes () {
      return cc.getNumInContactTypes ();
   }

   public int getNumMainPeriods () {
      return simLogic.isSteadyState () ? 1 : cc
            .getNumMainPeriods ();
   }

   public int getNumOutContactTypes () {
      return cc.getNumOutContactTypes ();
   }

   public int getNumWaitingQueues () {
      return cc.getNumWaitingQueues ();
   }

   @Override
   public Map<String, String> getContactTypeProperties (int k) {
      return cc.getContactTypeProperties (k);
   }

   @Override
   public String getContactTypeSegmentName (int k) {
      return cc.getContactTypeSegmentName (k);
   }

   @Override
   public Map<String, String> getContactTypeSegmentProperties (int k) {
      return cc.getContactTypeSegmentProperties (k);
   }

   @Override
   public Map<String, String> getAgentGroupProperties (int i) {
      return cc.getAgentGroupProperties (i);
   }

   @Override
   public String getAgentGroupSegmentName (int i) {
      return cc.getAgentGroupSegmentName (i);
   }

   @Override
   public Map<String, String> getAgentGroupSegmentProperties (int i) {
      return cc.getAgentGroupSegmentProperties (i);
   }

   @Override
   public String getInContactTypeSegmentName (int k) {
      return cc.getInContactTypeSegmentName (k);
   }

   @Override
   public Map<String, String> getInContactTypeSegmentProperties (int k) {
      return cc.getInContactTypeSegmentProperties (k);
   }

   @Override
   public String getMainPeriodSegmentName (int mp) {
      return cc.getMainPeriodSegmentName (mp);
   }

   @Override
   public int getNumAgentGroupSegments () {
      return cc.getNumAgentGroupSegments();
   }

   @Override
   public int getNumContactTypeSegments () {
      return cc.getNumContactTypeSegments();
   }

   @Override
   public int getNumInContactTypeSegments () {
      return cc.getNumInContactTypeSegments();
   }

   @Override
   public int getNumMainPeriodSegments () {
      return cc.getNumMainPeriodSegments();
   }

   @Override
   public int getNumOutContactTypeSegments () {
      return cc.getNumOutContactTypeSegments();
   }

   @Override
   public int getNumWaitingQueueSegments () {
      return cc.getNumWaitingQueueSegments ();
   }

   @Override
   public String getOutContactTypeSegmentName (int k) {
      return cc.getOutContactTypeSegmentName (k);
   }

   @Override
   public Map<String, String> getOutContactTypeSegmentProperties (int k) {
      return cc.getOutContactTypeSegmentProperties (k);
   }

   @Override
   public Map<String, String> getWaitingQueueProperties (int q) {
      return cc.getWaitingQueueProperties (q);
   }

   @Override
   public String getWaitingQueueSegmentName (int k) {
      return cc.getWaitingQueueSegmentName (k);
   }

   @Override
   public Map<String, String> getWaitingQueueSegmentProperties (int q) {
      return cc.getWaitingQueueSegmentProperties (q);
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      return pms.clone ();
   }

   @Override
   public String getWaitingQueueName (int q) {
      return cc.getWaitingQueueName (q);
   }

   @Override
   public String getMainPeriodName (int mp) {
      return cc.getMainPeriodName (mp);
   }

   public void reset () {
      try {
         cc.reset (cc.getCallCenterParams (), cc.getRandomStreams ());
      }
      catch (final CallCenterCreationException mce) {
         final IllegalStateException ise = new IllegalStateException
         ("Could not reset model;");
         ise.initCause (mce);
         throw ise;
      }
      pms = CallCenterSimUtil.initPerformanceMeasures (simLogic.getSimParams ());
      simLogic.reset (pms);
      setOneSimDone (false);
      ccStat = simLogic.getCallCenterStatProbes();
      if (isUsingControlVariables()) {
         disableControlVariables();
         initControlVariableSupport();
      }
      tracer = null;
   }

   public void reset (CallCenterParams ccParams, SimParams simParams) throws CallCenterCreationException {
      final RandomStreams streams = cc.getRandomStreams ();
      cc = createModel (cc.simulator (), ccParams, streams);
      pms = CallCenterSimUtil.initPerformanceMeasures (simParams);
      simLogic = createSimLogic (cc, simParams);
      cc.setAwtPeriod (simLogic);
      setOneSimDone (false);
      ccStat = simLogic.getCallCenterStatProbes();
      if (isUsingControlVariables()) {
         disableControlVariables();
         initControlVariableSupport();
      }
      tracer = null;
   }

   public boolean seemsUnstable () {
      return simLogic.seemsUnstable ();
   }


   protected void prepareEvaluation () {
      initTimer();
      initTrace();
      synchronized (this) {
         simLogic.setAborted (false);
      }
      if (!initialized) {
         init();
         initialized = true;
      }
   }

   protected void finishEvaluation () {
      // Clears memory used by the simulator
      cc.simulator ().getEventList ().clear ();
      for (final WaitingQueue queue : cc.getWaitingQueues())
         queue.init();
      for (final AgentGroupManager group : cc.getAgentGroupManagers())
         group.getAgentGroup().init();

      setOneSimDone (true);
      if (autoResetStartStream)
         resetStartStream ();
      cpuTime = timer == null ? 0 : timer.getSeconds ();
      formatReport();
   }

   public int getRequiredNewSteps() {
      if (isAborted ())
         return 0;
      final SimParams simParams = simLogic.getSimParams ();
      int nb;
      if (getOneSimDone () || !simParams.isSetSequentialSampling())
         nb = 0;
      else
         nb = CallCenterSimUtil.getRequiredNewSteps (
               getCallCenterStatProbes().getMatricesOfStatProbes(), simParams.getSequentialSampling(),
               isVerbose ());
      if (simLogic instanceof BatchMeansSim)
         nb *= ((BatchMeansSim)simLogic).getNumAggregates ();
      final SimStoppingCondition scond1 = getSimStoppingCondition ();
      if (scond1 != null)
         nb = scond1.check (this, nb);
      final int tnb = simLogic.getCompletedSteps () + nb;
      int maxSteps;
      if (simLogic instanceof BatchSimParams) {
         final BatchSimParams batchSim = (BatchSimParams)simLogic;
         if (batchSim.isSetMaxBatches())
            maxSteps = batchSim.getMaxBatches();
         else
            maxSteps = Integer.MAX_VALUE;
      }
      else if (simLogic instanceof RepSimParams) {
         final RepSimParams repSim = (RepSimParams)simLogic;
         if (repSim.isSetMaxReplications())
            maxSteps = repSim.getMaxReplications();
         else
            maxSteps = Integer.MAX_VALUE;
      }
      else
         maxSteps = Integer.MAX_VALUE;
      if (tnb >= maxSteps) {
         if (isVerbose ())
            logger.info ("Maximum number of steps reached");
         nb = maxSteps - getCompletedSteps ();
         if (nb < 0)
            nb = 0;
      }
      if (simLogic.getSimParams ().isSetCpuTimeLimit ())
         nb = CallCenterSimUtil.checkCpuTimeLimit
         (timer == null ? 0 : timer.getSeconds (),
               simLogic.getSimParams ().getCpuTimeLimit ().getTimeInMillis (new Date()) / 1000,
               simLogic.getCompletedSteps (),
               nb, isVerbose ());
      return nb;
   }


   public void eval () {
      prepareEvaluation ();
      try {
         final int numSteps = simLogic.getCompletedSteps ();
         final int minSteps;
         if (simLogic.getSimParams() instanceof RepSimParams)
            minSteps = ((RepSimParams)simLogic.getSimParams()).getMinReplications();
         else if (simLogic.getSimParams() instanceof BatchSimParams)
            minSteps = ((BatchSimParams)simLogic.getSimParams ()).getMinBatches();
         else
            minSteps = 1;
         simLogic.init ();
         if (getOneSimDone() && numSteps >= minSteps && !getSeqSampEachEval ())
            simLogic.simulate (numSteps);
         else
            for (int nb = minSteps; nb > 0; ) {
               if (simLogic.getCompletedSteps () == 0)
                  for (final ContactCenterSimListener l : getContactCenterSimListeners())
                     l.simulationStarted (this, nb);
               else
                  for (final ContactCenterSimListener l : getContactCenterSimListeners())
                     l.simulationExtended (this, nb);
               final int stepsBefore = simLogic.getCompletedSteps ();
               simLogic.simulate (nb);
               if (stepsBefore == simLogic.getCompletedSteps ())
                  break;
               applyControlVariables();
               nb = getRequiredNewSteps();
            }
      }
      finally {
         if (tracer != null)
            tracer.getContactTrace ().close ();
         for (final ContactCenterSimListener l : getContactCenterSimListeners())
            l.simulationStopped (this, isAborted());
      }
      finishEvaluation ();
   }

   public synchronized int getCompletedSteps () {
      return simLogic.getCompletedSteps ();
   }

   @Override
   public DoubleMatrix2D getPerformanceMeasure (PerformanceMeasureType m) {
      if (!getOneSimDone ())
         throw new IllegalStateException (
         "The evaluation was not been performed");
//      if (m == PerformanceMeasureType.MAXQUEUESIZE) {
//         final int[] maxSizes = sim.getCallCenterMeasures ().getMaxSizes ();
//         final DoubleMatrix2D qs = new DenseDoubleMatrix2D (maxSizes.length, 1);
//         for (int q = 0; q < maxSizes.length; q++)
//            qs.set (q, 0, maxSizes[q]);
//         return qs;
//      }
      return getCallCenterStatProbes().getAverage (m);
   }

   @Override
   public DoubleMatrix2D getVariance (PerformanceMeasureType m) {
      if (!getOneSimDone ())
         throw new IllegalStateException (
         "The evaluation was not been performed");
      return getCallCenterStatProbes ().getVariance (m);
   }

   @Override
   public DoubleMatrix2D getMin (PerformanceMeasureType m) {
      if (!getOneSimDone ())
         throw new IllegalStateException (
         "The evaluation was not been performed");
      return getCallCenterStatProbes ().getMin (m);
   }

   @Override
   public DoubleMatrix2D getMax (PerformanceMeasureType m) {
      if (!getOneSimDone ())
         throw new IllegalStateException (
         "The evaluation was not been performed");
      return getCallCenterStatProbes ().getMax (m);
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      return getCallCenterStatProbes ().getMatrixOfStatProbes (m);
   }

   @Override
   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType m) {
      return getCallCenterStatProbes ().getMatrixOfTallies (m);
   }

   @Override
   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType m) {
      return getCallCenterStatProbes ()
               .getMatrixOfFunctionOfMultipleMeansTallies (m);
   }

   @Override
   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType m, double level) {
      if (!getOneSimDone ())
         throw new IllegalStateException (
         "The evaluation was not been performed");
      return getCallCenterStatProbes ().getConfidenceInterval (m, level);
   }

   public void newSeeds () {
      cc.setRandomStreams (new RandomStreams (cc.getRandomStreams ()
            .getRandomStreamFactory (), cc.getCallCenterParams()));
      reset ();
   }

   public void resetNextSubstream () {
      cc.resetNextSubstream ();
   }

   public void resetStartStream () {
      cc.resetStartStream ();
   }

   public void resetStartSubstream () {
      cc.resetStartSubstream ();
   }

   public double getCpuTime () {
      return cpuTime;
   }

   public void formatReport() {
      simLogic.formatReport (getEvalInfo ());
      DatatypeFactory df;
      if (cpuTime > 0) {
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
   }

   public static ControlVariable[] createControlVariables (
         ControlVariableType... cvTypes) {
      final ControlVariable[] cvs = new ControlVariable[cvTypes.length];
      for (int i = 0; i < cvs.length; i++)
         switch (cvTypes[i]) {
         case NUMARRIVALS:
            cvs[i] = new NumArrivalsCV ();
            break;
         default:
            throw new IllegalArgumentException ("Control variable "
                  + cvTypes[i] + " not supported");
         }
      return cvs;
   }

   public CallCenterStatProbes getCallCenterStatProbes() {
      return ccStat;
   }

   public boolean isUsingControlVariables() {
      return cvStat != null;
   }

   public void enableControlVariables (ControlVariable... cvs) {
      if (cvStat != null)
         cvStat = null;
      if (!simLogic.getSimParams ().isKeepObs ())
         throw new IllegalStateException (
         "Cannot use control variables while discarding observations");
      cvStat = new CVCallCenterStat (simLogic, getCallCenterStatProbes (),
            true, cvs);
      ccStat = new ChainCallCenterStat (cvStat, simLogic.getCallCenterStatProbes());
      cvStat.initCV ();
      if (getOneSimDone ())
         cvStat.applyControlVariables ();
   }

   public void applyControlVariables() {
      if (cvStat != null)
         cvStat.applyControlVariables();
   }

   public void disableControlVariables() {
      cvStat = null;
      ccStat = simLogic.getCallCenterStatProbes();
   }

   public void initControlVariableSupport () {
      final SimParams simParams = simLogic.getSimParams ();
      final ControlVariableType[] cvTypes = new ControlVariableType[simParams.getControlVariables ().size ()];
      int idx = 0;
      for (final ControlVariableParams cvp : simParams.getControlVariables ())
         cvTypes[idx++] = ControlVariableType.valueOf (cvp.getName ());
      if (cvTypes == null || cvTypes.length == 0)
         disableControlVariables ();
      else if (!simParams.isKeepObs ()) {
         logger.warning ("Observations must be kept to use control variables");
         disableControlVariables ();
      }
      else
         enableControlVariables (createControlVariables (cvTypes));
   }

   public synchronized void abort () {
      getSimLogic ().setAborted (true);
   }

   public void addContactCenterSimListener (ContactCenterSimListener l) {
      if (l == null)
         throw new NullPointerException();
      if (!listeners.contains (l))
         listeners.add (l);
   }

   public void clearContactCenterSimListeners () {
      listeners.clear ();
   }

   public List<ContactCenterSimListener> getContactCenterSimListeners () {
      return umListeners;
   }

   public synchronized boolean isAborted () {
      return getSimLogic ().isAborted ();
   }

   public void removeContactCenterSimListener (ContactCenterSimListener l) {
      listeners.remove (l);
   }

   private class SLListener implements SimLogicListener {
      public void stepDone (SimLogic sim) {
         for (final ContactCenterSimListener l : getContactCenterSimListeners())
            l.stepDone (AbstractCallCenterSim.this);
      }
   }
}
