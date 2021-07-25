package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.app.AbstractContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimWithObservations;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.CTMCRepSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.contactcenters.app.trace.ContactTrace;
import umontreal.iro.lecuyer.contactcenters.app.trace.FileContactTrace;
import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcessWithTimeIntervals;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSimUtil;
import umontreal.iro.lecuyer.contactcenters.msk.Messages;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallFactory;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.ssj.util.Chrono;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

/**
 * Base class for simulators of call centers using a
 * continuous-time Markov chain.
 * Any instance of this class encapsulates a CTMC,
 * statistical counters concerning replications,
 * and statistical probes for collecting observations
 * for the replications.
 * The simulator is constructed from
 * an instance of {@link CallCenterParams} which is usually obtained
 * from a XML parameter file.
 */
public abstract class AbstractCallCenterCTMCSim extends
      AbstractContactCenterSim implements ContactCenterSimWithObservations {
   private Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.ctmc");
   protected CallCenter cc;
   protected int mp; // the simulated main period
   protected CallCenterCTMC ctmc;
   protected CallCenterCounters counters;
   protected CallCenterStat ccStat;
   protected ContactTrace trace;

   protected double[] awt;
   protected RateChangeTransitions rateChange;

   private RandomStream stream;
   private double m_timeHorizon;
   protected CTMCRepSimParams simParams;
   private Chrono timer;
   private double cpuTime;
   private int numSteps;
   private final List<TransitionListener> listeners = new ArrayList<TransitionListener>();
   private final List<TransitionListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;

   /**
    * Constructs a new simulator using call center parameters
    * \texttt{ccParams}, experiment parameters
    * \texttt{simParams}, and concentrating on main
    * period \texttt{mp} of the model.
    * @param ccParams the parameters of the call center.
    * @param simParams the parameters of the experiment.
    * @param mp the index of the simulated main period.
    * @throws CallCenterCreationException if an error
    * occurs during the creation of the call center.
    * @throws CTMCCreationException if an exception occurs
    * during the creation of the CTMC.
    */
   public AbstractCallCenterCTMCSim (CallCenterParams ccParams,
         CTMCRepSimParams simParams, int mp)
         throws CallCenterCreationException, CTMCCreationException {
      RandomStreams streams = new RandomStreams (new SimRandomStreamFactory (simParams.getRandomStreams ()), ccParams);
      this.cc = new CallCenter (ccParams, streams);
      cc.create ();
      cc.initSim ();
      this.simParams = simParams;
      this.mp = mp;
      init ();
   }

   public AbstractCallCenterCTMCSim (CallCenterParams ccParams,
         CTMCRepSimParams simParams, RandomStreams streams,
         int mp)
         throws CallCenterCreationException, CTMCCreationException {
      this.cc = new CallCenter (ccParams, streams);
      cc.create ();
      cc.initSim ();
      this.simParams = simParams;
      this.mp = mp;
      init ();
   }

   public AbstractCallCenterCTMCSim (CallCenter cc, CTMCRepSimParams simParams,
         int mp)
         throws CTMCCreationException {
      this.cc = cc;
      cc.initSim ();
      this.simParams = simParams;
      this.mp = mp;
      init ();
   }

   private void init () throws CTMCCreationException {
      if (simParams.isSetTimeHorizon ())
         m_timeHorizon = cc.getTime (simParams.getTimeHorizon ());
      else
         m_timeHorizon = cc.getPeriodDuration ();
      stream = cc.getRandomStreams ().getStreamCT ();
      final boolean keepQueues = simParams.isKeepQueues ();
      int[][] thresholds;
      if (simParams.getThresholds ().isEmpty ())
         thresholds = null;
      else if (simParams.getThresholds ().size () == 1)
         thresholds = ArrayConverter.unmarshalArray (simParams.getThresholds ().get (0));
      else
         thresholds = ArrayConverter.unmarshalArray (simParams.getThresholds ().get (mp));
      rateChange = new RateChangeTransitions (cc);
      ctmc = getCTMC (cc, mp, keepQueues, simParams.getMaxNumAgents (),
            simParams.getCallTrace () != null, thresholds,
            simParams.isAlwaysUseIndexedSearch ());
      if (keepQueues)
         awt = getAWT (cc, mp);
      counters = new CallCenterCounters (ctmc, awt,
            false);
      final int ns = cc.getNumMatricesOfAWT ();
      ccStat = new CallCenterStat (ctmc, awt == null ? 0 : ns, simParams.isKeepObs());
      trace = FileContactTrace.create (simParams.getCallTrace ());
   }

   public int getCurrentPeriod () {
      return mp;
   }

   public void setCurrentPeriod (int mp) {
      this.mp = mp;
      reset();
   }

   public int[] getStaffing() {
      return ctmc.getNumAgentsArray();
   }

   public int[][] getStaffingMatrix() {
      int[] staffing = ctmc.getNumAgentsArray ();
      int[][] staffingMatrix = new int[staffing.length][1];
      for (int i = 0; i < staffing.length; i++)
         staffingMatrix[i][0] = staffing[i];
      return staffingMatrix;
   }

   public void setStaffing (int[] staffing) {
      if (staffing.length != ctmc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("Invalid length of staffing vector");
      int[] maxNumAgents = ctmc.getMaxNumAgentsArray();
      boolean maxChanged = false;
      for (int i = 0; i < staffing.length; i++) {
         if (staffing[i] > maxNumAgents[i]) {
            maxNumAgents[i] = staffing[i];
            maxChanged = true;
         }
      }
      if (maxChanged)
         ctmc.setMaxNumAgents (maxNumAgents);
      ctmc.setNumAgents (staffing);
   }

   public void setStaffingMatrix (int[][] staffingMatrix) {
      int[] staffing = new int[staffingMatrix.length];
      for (int i = 0; i < staffing.length; i++)
         if (staffingMatrix[i] != null)
            staffing[i] = staffingMatrix[i][0];
      setStaffing (staffing);
   }

   public CallCenterCTMC getCTMC() {
      return ctmc;
   }

   public int[] getMaxNumAgents() {
      return ctmc.getMaxNumAgentsArray();
   }

   public void setMaxNumAgents (int[] maxNumAgents) {
      ctmc.setMaxNumAgents (maxNumAgents);
   }

   public int getQueueCapacity() {
      return ctmc.getQueueCapacity();
   }

   public void setQueueCapacity (int q) {
      ctmc.setQueueCapacity (q);
   }

   public double getTimeHorizon () {
      return m_timeHorizon;
   }

   public void setTimeHorizon (double timeHorizon) {
      this.m_timeHorizon = timeHorizon;
   }

   protected void initStat () {
      ccStat.init (ctmc);
   }

   protected void initReplication (RandomStream stream, double timeHorizon, int ntr) {
      counters.init (ctmc, timeHorizon, ntr);
   }

   protected void addObs () {
      ccStat.addObs (counters, m_timeHorizon);
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
      if (ctmc.getNumStateThresh () > 1) {
         StateThresh thresh = ctmc.getStateThresh ();
         getEvalInfo().put ("Thresholds on queue size", thresh.getThreshQueueSize ());
         getEvalInfo().put ("Thresholds on the number of agents", thresh.getThreshNumAgents ());
      }
      getEvalInfo().put ("Expected number of transitions",
            getNumExpectedTransitions ());
      ccStat.formatReport (getEvalInfo(), getNumExpectedTransitions());
   }

   public abstract void simulate (RandomStream stream1, double timeHorizon, int n);

   public abstract double getNumExpectedTransitions ();

   public double getNumFalseTransitions() {
      return ccStat.getStatNumFalseTransitions ().average ();
   }

   public Tally getStatNumFalseTransitions() {
      return ccStat.getStatNumFalseTransitions ();
   }

   public static CallCenterCTMC getCTMC (CallCenter cc, int mp,
         boolean keepQueues, int[] maxNumAgents, boolean needsCallMix, int[][] thresholds,
         boolean alwaysUseSearchIndex) throws CTMCCreationException {
      if (cc.getQueueCapacity () == Integer.MAX_VALUE)
         throw new CTMCCreationException (
               "The queue capacity must not be infinite");
      int queueCapacity = cc.getQueueCapacity ();
      int numTypes = cc.getNumContactTypes ();
      int numGroups = cc.getNumAgentGroups ();

      final double sp = cc.getPeriodChangeEvent ().getPeriodStartingTime (mp + 1);
      final double ep = cc.getPeriodChangeEvent ().getPeriodEndingTime (mp + 1);
      double[] lambda = new double[numTypes];
      double[] rho = new double[numTypes];
      double[] nu = new double[numTypes];
      double[][] mu = new double[numTypes][numGroups];
      for (int k = 0; k < lambda.length; k++) {
         ContactArrivalProcess cap = cc.getArrivalProcess (k);
         if (cap instanceof PoissonArrivalProcessWithTimeIntervals) {
            PoissonArrivalProcessWithTimeIntervals capIn = (PoissonArrivalProcessWithTimeIntervals)cap;
            double[] times = capIn.getTimes ();
            double[] rates = capIn.getExpectedArrivalRatesBInt();
            lambda[k] = 0;
            for (int j = 0; j < times.length - 1; j++) {
               if (times[j+1] < sp)
                  continue;
               if (times[j] >= ep)
                  continue;
               if (lambda[k] < rates[j])
                  lambda[k] = rates[j];
            }
         }
         else
            lambda[k] = cc.getArrivalProcess (k).getExpectedArrivalRateB (sp, ep);
         CallFactory factory = cc.getCallFactory (k);
         rho[k] = factory.getProbAbandon (mp);
         if (factory.getPatienceTimeGen () == null)
            nu[k] = 0;
         else
            nu[k] = 1.0 / factory.getPatienceTimeGen ().getMean (mp + 1);
         for (int i = 0; i < numGroups; i++) {
            MultiPeriodGen stg = factory.getServiceTimesManager().getServiceTimeGen (i);
            if (stg == null || !cc.getRouter().canServe (i, k))
               mu[k][i] = 0;
            else
               mu[k][i] = 1.0 / stg.getMean (mp + 1);
         }
      }

      int[] numAgents = new int[numGroups];
      for (int i = 0; i < numGroups; i++)
         numAgents[i] = cc.getAgentGroupManager (i).getEffectiveStaffing (mp);

      AgentGroupSelector[] ags = new AgentGroupSelector[numTypes];
      WaitingQueueSelector[] wqs = new WaitingQueueSelector[numGroups];
      if (cc.getRouter () instanceof AgentsPrefRouter) {
         AgentsPrefRouter router = (AgentsPrefRouter) cc.getRouter ();
         double[][] ranksTG = router.getRanksTG ();
         double[][] ranksGT = router.getRanksGT ();
         for (int k = 0; k < ags.length; k++)
            ags[k] = new PriorityGroupSelector (ranksTG[k]);
         if (keepQueues)
            for (int i = 0; i < wqs.length; i++)
               wqs[i] = new PriorityQueueSelectorWT (ranksGT[i]);
         else
            for (int i = 0; i < wqs.length; i++)
               wqs[i] = new PriorityQueueSelectorQS (ranksGT[i]);
      }
      else if (cc.getRouter () instanceof QueuePriorityRouter) {
         QueuePriorityRouter router = (QueuePriorityRouter) cc.getRouter ();
         int[][] typeToGroupMap = router.getTypeToGroupMap ();
         int[][] groupToTypeMap = router.getGroupToTypeMap ();
         for (int k = 0; k < ags.length; k++)
            ags[k] = new ListGroupSelector (cc.getNumAgentGroups (), typeToGroupMap[k]);
         for (int i = 0; i < wqs.length; i++)
            wqs[i] = new ListQueueSelector (cc.getNumContactTypes (), groupToTypeMap[i]);
      }

      int[] max = new int[numAgents.length];
      if (maxNumAgents != null) {
         if (maxNumAgents.length == 0)
            Arrays.fill (max, 0);
         else if (maxNumAgents.length == 1)
            Arrays.fill (max, maxNumAgents[0]);
         else {
            if (maxNumAgents.length != max.length)
               throw new IllegalArgumentException
               ("Invalid length of array maxNumAgents in simulation parameters");
            System.arraycopy (maxNumAgents, 0, max, 0, maxNumAgents.length);
         }
      }

      //final double[] muI = getEqualMu (mu);
      if (lambda.length == 1 && numAgents.length == 1 && !alwaysUseSearchIndex) {
         if (keepQueues)
            return new CallCenterCTMC11WithQueues (lambda[0], lambda[0], mu[0][0], mu[0][0], numAgents[0], max[0],
                  rho[0], nu[0], nu[0], queueCapacity, queueCapacity, thresholds);
         else
            return new CallCenterCTMC11 (lambda[0], lambda[0], mu[0][0], mu[0][0],
                  numAgents[0], max[0],
                  rho[0], nu[0], nu[0], queueCapacity, queueCapacity, thresholds);
      }
//      else if (numAgents.length == 1) {
//         if (muI != null) {
//            if (keepQueues)
//               return new CallCenterCTMCKSameMuWithQueues (lambda, muI[0], numAgents[0], max[0],
//                     rho, nu, queueCapacity, wqs[0]);
//            else
//               return new CallCenterCTMCKSameMu (lambda, muI[0], numAgents[0], max[0],
//                     rho, nu, queueCapacity, wqs[0]);
//         }
//         else {
//            if (keepQueues)
//               return new CallCenterCTMCKWithQueues (lambda, getMuI (mu, 0), numAgents[0], max[0],
//                     rho, nu, queueCapacity, wqs[0]);
//            else
//               return new CallCenterCTMCK (lambda, getMuI (mu, 0), numAgents[0], max[0],
//                     rho, nu, queueCapacity, wqs[0]);
//         }
//      }
//      else if (lambda.length == 1) {
//         if (keepQueues)
//            return new CallCenterCTMCIWithQueues (lambda[0], mu[0], numAgents, max,
//                  rho[0], nu[0], queueCapacity, ags[0]);
//         else
//            return new CallCenterCTMCI (lambda[0], mu[0], numAgents, max,
//                  rho[0], nu[0], queueCapacity, ags[0]);
//      }
//      else if (muI != null) {
//         if (keepQueues)
//            return new CallCenterCTMCKISameMuWithQueues (lambda, muI, numAgents, max,
//                  rho, nu, queueCapacity, ags, wqs);
//         else
//            return new CallCenterCTMCKISameMu (lambda, muI, numAgents, max, rho, nu,
//                  queueCapacity, ags, wqs);
//      }
      else {
         if (keepQueues)
            return new CallCenterCTMCKIWithQueues (lambda, lambda, mu, mu, numAgents, max,
                  rho, nu, nu, queueCapacity, queueCapacity, ags, wqs, needsCallMix, thresholds);
         else
            return new CallCenterCTMCKI (lambda, lambda, mu, mu, numAgents, max, rho, nu, nu,
                  queueCapacity, queueCapacity, ags, wqs, needsCallMix, thresholds);
      }
   }

//   private static double[] getMuI (double[][] mu, int i) {
//      if (mu.length == 0)
//         return new double[0];
//      if (mu[0].length == 0)
//         return new double[0];
//      double[] muI = new double[mu.length];
//      for (int k = 0; k < muI.length; k++)
//         muI[k] = mu[k][i];
//      return muI;
//   }

//   private static double[] getEqualMu (double[][] mu) {
//      if (mu.length == 0)
//         return null;
//      if (mu[0].length == 0)
//         return null;
//      double[] muI = new double[mu[0].length];
//      for (int i = 0; i < muI.length; i++) {
//         muI[i] = mu[0][i];
//         for (int k = 1; k < mu.length; k++)
//            if (Math.abs (mu[k][i] - muI[i]) > 1e-10)
//               return null;
//      }
//      return muI;
//   }

   public static double[] getAWT (CallCenter cc, int mp) {
      final int numTypes = cc.getNumContactTypes ();
      final int nt = numTypes > 1 ? numTypes + 1 : numTypes;
      final int ns = cc.getNumMatricesOfAWT ();
      double[] awt = new double[ns*nt];
      for (int s = 0; s < ns; s++) {
         final ServiceLevelParamReadHelper slp = cc.getServiceLevelParams (s);
         for (int k = 0; k < nt; k++)
            awt[s*nt + k] = slp.getAwtDefault (k, mp);
      }
      return awt;
   }

   public static void traceStep (CallCenterCTMC ctmc, TransitionType type, ContactTrace trace, int step, int period) {
      if (trace == null)
         return;
      if (type == TransitionType.FALSETRANSITION)
         return;

      final int lastK = ctmc.getLastSelectedContactType ();
      final int lastI = ctmc.getLastSelectedAgentGroup ();
      final int lastKp = ctmc.getLastSelectedQueuedContactType ();
      double delta;
      final double arv = ctmc.getNumTransitionsDone ();
      // The ArvTime column of the trace contains the number of the transition
      // in the CTMC.
      // Two extra outcomes: Queued, and BeginService which
      // represent intermediate steps for calls.
      switch (type) {
      case ARRIVALBALKED:
         trace.writeLine (step, lastK,
               period, arv,
               0, ContactTrace.OUTCOME_ABANDONED,
               -1, Double.NaN);
         break;
      case ARRIVALBLOCKED:
         trace.writeLine (step, lastK,
               period, ctmc.getNumTransitionsDone (),
               0, ContactTrace.OUTCOME_BLOCKED,
               -1, Double.NaN);
         break;
      case ARRIVALQUEUED:
         trace.writeLine (step, lastK,
               period, ctmc.getNumTransitionsDone (),
               Double.NaN, "Queued",
               -1, Double.NaN);
         break;
      case ARRIVALSERVED:
         trace.writeLine (step, lastK,
               period, ctmc.getNumTransitionsDone (),
               0, "BeginService",
               lastI, Double.NaN);
         break;
      case ABANDONMENT:
         if (ctmc instanceof CallCenterCTMCWithQueues)
            delta = ((CallCenterCTMCWithQueues)ctmc).getLastWaitingTime (lastK);
         else
            delta = Double.NaN;
         trace.writeLine (step, lastK,
               period, arv,
               delta, ContactTrace.OUTCOME_ABANDONED,
               -1, Double.NaN);
         break;
      case ENDSERVICEANDDEQUEUE:
         if (ctmc instanceof CallCenterCTMCWithQueues && lastKp >= 0)
            delta = ((CallCenterCTMCWithQueues)ctmc).getLastWaitingTime (lastKp);
         else
            delta = Double.NaN;
         trace.writeLine (step, lastKp,
               period, arv,
               delta, "BeginService",
               lastI, Double.NaN);
      case ENDSERVICENODEQUEUE:
         trace.writeLine (step, lastK,
               period, arv,
               Double.NaN, ContactTrace.OUTCOME_SERVED,
               lastI, Double.NaN);
         break;
      }
   }

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
         nb = CallCenterSimUtil.getRequiredNewSteps (ccStat.getMatricesOfStatProbes(), simParams
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
            simulate (stream, m_timeHorizon, numSteps);
         else {
            numSteps = 0;
            for (int nb = minSteps; nb > 0;) {
               simulate (stream, m_timeHorizon, nb);
               numSteps += nb;
               nb = getRequiredNewSteps ();
            }
         }
         setOneSimDone (true);
      }
      finally {
         if (trace != null)
            trace.close ();
      }
      if (autoResetStartStream)
         stream.resetStartStream ();
      cpuTime = timer == null ? 0 : timer.getSeconds ();
      formatReport ();
      ctmc.initEmpty ();
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
      return ccStat.getMatrixOfStatProbes (m);
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
      case CURRENTPERIOD:
         return getCurrentPeriod();
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
            EvalOptionType.CURRENTPERIOD,
            EvalOptionType.STAFFINGVECTOR,
            EvalOptionType.STAFFINGMATRIX,
            EvalOptionType.QUEUECAPACITY
      };
   }

   public int getNumAgentGroups () {
      return ctmc.getNumAgentGroups ();
   }

   public int getNumContactTypes () {
      return ctmc.getNumContactTypes ();
   }

   public int getNumInContactTypes () {
      return ctmc.getNumContactTypes ();
   }

   public CallCenter getModel() {
      return cc;
   }

   public CallCenterStat getStat() {
      return ccStat;
   }

   public void setStat (CallCenterStat ccStat) {
      this.ccStat = ccStat;
   }

   public int getNumMainPeriods () {
      return 1;
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
      return ccStat.getPerformanceMeasures();
   }

   public boolean hasEvalOption (EvalOptionType option) {
      return option == EvalOptionType.STAFFINGVECTOR ||
      option == EvalOptionType.STAFFINGMATRIX ||
      option == EvalOptionType.CURRENTPERIOD ||
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
      case CURRENTPERIOD:
         setCurrentPeriod ((Integer)value);
         break;
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

   public void notifyInit (int r, int mp, CallCenterCTMC ctmc) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).init (ctmc, r, mp);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   public void notifyTransition (int r, int mp1, CallCenterCTMC ctmc1, TransitionType type) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).newTransition (ctmc1, r, mp1, type);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   public static String formatInfo (CallCenterCTMC ctmc) {
      Formatter fmt = new Formatter();
      fmt.format ("%d contact types, %d agent groups%n", ctmc.getNumContactTypes(), ctmc.getNumAgentGroups());
      fmt.format ("Total arrival rate: %f%n", ctmc.getArrivalRate());
      fmt.format ("Number of agents: %d%n", ctmc.getNumAgents());
      fmt.format ("Queue capacity: %d", ctmc.getQueueCapacity());
      fmt.format ("Arrival rate: [");
      for (int k = 0; k < ctmc.getNumContactTypes(); k++) {
         final double lambdak = ctmc.getArrivalRate (k);
         fmt.format ("%s%f", k > 0 ? ", " : "", lambdak);
      }
      fmt.format ("]%n");
      fmt.format ("Staffing: [");
      for (int i = 0; i < ctmc.getNumAgentGroups(); i++) {
         final int ni = ctmc.getNumAgents (i);
         fmt.format ("%s%d", i > 0 ? ", " : "", ni);
      }
      fmt.format ("]%n");
      fmt.format ("Service rate: [");
      for (int k = 0; k < ctmc.getNumContactTypes(); k++) {
         fmt.format ("Type %d: [", k);
         for (int i = 0; i < ctmc.getNumAgentGroups(); i++) {
            final double muki = ctmc.getServiceRate (k, i);
            fmt.format ("%s%f", k > 0 ? ", " : "", muki);
         }
         fmt.format ("]%n");
      }
      fmt.format ("]%n");
      return fmt.toString();
   }
}
