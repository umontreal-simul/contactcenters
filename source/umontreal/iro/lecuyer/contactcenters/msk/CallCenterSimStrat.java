package umontreal.iro.lecuyer.contactcenters.msk;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.StratSimParams;
import umontreal.iro.lecuyer.contactcenters.msk.cv.CVCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.cv.ControlVariable;
import umontreal.iro.lecuyer.contactcenters.msk.cv.NumArrivalsCV;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.RepLogic;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.ChainCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatType;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.TruncatedDist;
import umontreal.ssj.randvar.ConstantGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.rng.TruncatedRandomStream;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Defines a call center simulator using stratified sampling. This simulator
 * stratifies on $B$, the busyness factor for inbound calls, and uses
 * proportional allocation.
 */
public class CallCenterSimStrat extends AbstractCallCenterSim {
   private StratSimParams simParams;
   private StatCallCenterStat statAvg;
   private StatCallCenterStat statVar;
   private StatCallCenterStat statVarProp;
   private StatCallCenterStat statSigma;
   private final NumArrivalsCV arvCV = new NumArrivalsCV ();
   private CVCallCenterStat cvStat;
   private StatCallCenterStat statAvgCV;
   private StatCallCenterStat statVarCV;
   private StatCallCenterStat statVarPropCV;
   private StatCallCenterStat statSigmaCV;

   boolean pilotRunsDone = false;
   private int[] numRepPerStrata;
   private int totalNumRep;
   private StratData[] pilotRuns;
   private PerformanceMeasureType pmOpt;
   private int rowOpt;
   private int colOpt;

   /**
    * Constructs a new stratified call center simulator using the call center
    * parameters \texttt{ccParams}, the simulation parameters
    * \texttt{simParams}, and simulating \texttt{numStrata} strata.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    */
   public CallCenterSimStrat (CallCenterParams ccParams,
         StratSimParams simParams) throws CallCenterCreationException {
      super (ccParams, simParams);
      this.simParams = simParams;
      simParams.unsetSequentialSampling();
      statAvg = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.AVERAGE, true);
      statVarProp = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCE, true);
//      statCovProp = new CovFMMCallCenterStat (getSimLogic ()
//            .getCallCenterStatProbes (), false);
      statVar = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCEOFAVERAGE, true);
      statSigma = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.STANDARDDEVIATION, true);
//      statCov = new CovFMMCallCenterStat (getSimLogic ()
//            .getCallCenterStatProbes (), true);
   }

   @Override
   public CallCenterStatProbes getCallCenterStatProbes() {
      return statAvg;
   }

   @Override
   public boolean isUsingControlVariables() {
      return cvStat != null;
   }

   @Override
   public void disableControlVariables() {
      cvStat = null;
   }

   @Override
   public void enableControlVariables (ControlVariable... cvs) {
      correctControlVariables (cvs);
      if (cvStat != null)
         cvStat = null;
      if (!simParams.isKeepObs ())
         throw new IllegalStateException (
         "Cannot use control variables while discarding observations");
      final CallCenterStatProbes ccStat = getSimLogic ().getCallCenterStatProbes ();
      cvStat = new CVCallCenterStat (getSimLogic(), ccStat,
            true, cvs);
      cvStat.initCV ();
      final CallCenterStatProbes stat = new ChainCallCenterStat (cvStat,
            ccStat);
      statAvgCV = new StatCallCenterStat (stat, StatType.AVERAGE, true);
      statVarPropCV = new StatCallCenterStat (stat, StatType.VARIANCE, true);
//      statCovProp = new CovFMMCallCenterStat (stat, false);
      statVarCV = new StatCallCenterStat (stat, StatType.VARIANCEOFAVERAGE,
            true);
//      statCov = new CovFMMCallCenterStat (stat, true);
      statSigmaCV = new StatCallCenterStat (stat, StatType.STANDARDDEVIATION, true);
   }

   protected void correctControlVariables (ControlVariable[] cvs) {
      for (int i = 0; i < cvs.length; i++)
         if (cvs[i] instanceof NumArrivalsCV)
            cvs[i] = arvCV;
   }

   protected void simulateForFixedB (double u, int numReplications) {
      final CallCenter cc = getCallCenter ();
      // RepLogic sim = (RepLogic) getSimLogic ();
      // RepSimParams simParams = (RepSimParams) sim.getSimParams ();
      final RandomVariateGen bgen = cc.getBusynessGen ();
      final double busyness = bgen.getDistribution ().inverseF (u);
      final boolean ar = getAutoResetStartStream ();
      try {
         cc.setBusynessGen (new ConstantGen (busyness));
         setAutoResetStartStream (false);
         getSimLogic ().init();
         getSimLogic ().simulate (numReplications);
      }
      finally {
         cc.setBusynessGen (bgen);
         setAutoResetStartStream (ar);
      }
   }

   public void makePilotRuns () {
      final int numStrata = simParams.getNumStrata ();
      final int numRepPerStrat = simParams.getNumPilotRunsPerStratum ();
      if (numRepPerStrat < 2)
         throw new IllegalArgumentException
         ("At least 2 pilot runs per stratum is required");
      pilotRuns = new StratData[numStrata];
      for (int strat = 0; strat < numStrata; strat++) {
         simulateOneStrat (strat, numRepPerStrat);
         pilotRuns[strat] = new StratData (this);
      }
      pilotRunsDone = true;
   }

   public double getBusynessFactor (int strat) {
      final int numStrata = simParams.getNumStrata();
//      Distribution dist = getCallCenter().getBusynessGen().getDistribution();
//      return numStrata*(dist.cdf ((strat + 1.0) / numStrata)
//            - dist.cdf ((double)strat / numStrata));
      final ContinuousDistribution dist = (ContinuousDistribution)getCallCenter().getBusynessGen().getDistribution ();
      double a;
      try {
         a = dist.inverseF ((double)strat / numStrata);
      }
      catch (final ArithmeticException e) {
         a = dist.inverseF ((strat + numStrata / 1000.0) / numStrata);
      }
      double b;
      try {
         b = dist.inverseF ((strat + 1.0) / numStrata);
      }
      catch (final ArithmeticException e) {
         b = dist.inverseF ((strat + 1 - numStrata / 1000.0) / numStrata);
      }
      final TruncatedDist tdist = new TruncatedDist (dist, a, b);
      return tdist.getMean ();
   }

   private void simulateOneStrat (int strat, int numReplications) {
      final CallCenter cc = getCallCenter ();
      final RandomVariateGen bgen = cc.getBusynessGen ();
      final RandomStream bStream = bgen.getStream ();
      final RandomStream stratStream = new TruncatedRandomStream (bgen
            .getStream (), (double) strat / simParams.getNumStrata (),
            (strat + 1.0) / simParams.getNumStrata ());
      arvCV.setBusynessFactor (getBusynessFactor (strat));
      final boolean ar = getAutoResetStartStream ();
      try {
         bgen.setStream (stratStream);
         setAutoResetStartStream (false);
         getSimLogic ().init ();
         getSimLogic ().simulate (numReplications);
      }
      finally {
         bgen.setStream (bStream);
         setAutoResetStartStream (ar);
      }
   }

   /**
    * Initializes the number of replications in each stratum for proportional
    * allocation. This sets the number of replications $n_s$ to $n/m$, where $n$
    * is the total number of replications and $m$ is the number of strata.
    */
   public void setProportionalAllocation () {
      numRepPerStrata = new int[simParams.getNumStrata ()];
      Arrays.fill (numRepPerStrata, simParams.getMinReplications ()
            / simParams.getNumStrata ());
      totalNumRep = simParams.getMinReplications () / simParams.getNumStrata ();
      totalNumRep *= simParams.getNumStrata ();
      pmOpt = null;
      rowOpt = -1;
      colOpt = -1;
   }

   /**
    * Sets the number of replications in each stratum for optimal allocation
    * minimizing the variance of performance measure of type \texttt{m}, at row
    * \texttt{r} and column \texttt{c}. The boolean \texttt{cv} determines if
    * control variables are used. One must call {@link #makePilotRuns} before
    * calling this method.
    *
    * @param m
    *           the type of performance measure.
    * @param r
    *           the row index.
    * @param c
    *           the column index.
    * @param cv
    *           determines if control variables will be used.
    */
   public void setOptimalAllocation (PerformanceMeasureType m, int r, int c,
         boolean cv) {
      if (r < 0) {
         setOptimalAllocation (m, r + m.rows (this), c, cv);
         return;
      }
      if (c < 0) {
         setOptimalAllocation (m, r, c + m.columns (this), cv);
         return;
      }
      if (!pilotRunsDone)
         makePilotRuns ();
      double sigmaBar = 0;
      final double[] sigmas = new double[simParams.getNumStrata ()];
      for (int strat = 0; strat < sigmas.length; strat++) {
         if (cv)
            sigmas[strat] = pilotRuns[strat].getSigmasCV (m).get (r, c);
         else
            sigmas[strat] = pilotRuns[strat].getSigmas (m).get (r, c);
         sigmaBar += sigmas[strat];
      }
      sigmaBar /= sigmas.length;

      final int numStrata = simParams.getNumStrata ();
      numRepPerStrata = new int[numStrata];
      totalNumRep = 0;
      for (int s = 0; s < numStrata; s++) {
         int ns = (int) Math.round (simParams.getMinReplications () * sigmas[s]
               / (numStrata * sigmaBar));
         if (ns < 2)
            ns = 2;
         numRepPerStrata[s] = ns;
         totalNumRep += ns;
      }
      pmOpt = m;
      rowOpt = r;
      colOpt = c;
   }

   @Override
   public void eval () {
      prepareEvaluation ();
      if (!pilotRunsDone &&
            (simParams.isOptimalAllocation() ||
                  isUsingControlVariables()))
         makePilotRuns ();
      if (simParams.isOptimalAllocation ()) {
         if (simParams.getSelectedPerformanceMeasure () == null)
            throw new IllegalArgumentException
            ("No selected performance measure");
         else {
            final PerformanceMeasureType pm =
               PerformanceMeasureType.valueOf (simParams
                  .getSelectedPerformanceMeasure ());
            final int r = simParams.getSelectedRow ();
            final int c = simParams.getSelectedColumn ();
            setOptimalAllocation (pm, r, c, cvStat != null);
         }
      }
      else
         setProportionalAllocation ();
      if (isVerbose ())
         logger.info ("Starting production runs");
      final int numStrata = simParams.getNumStrata ();
      statAvg.init ();
      statVarProp.init ();
      statVar.init ();
      statSigma.init ();
      if (isUsingControlVariables()) {
         statAvgCV.init ();
         statVarPropCV.init ();
         statVarCV.init ();
         statSigmaCV.init ();
      }
      for (int s = 0; s < numStrata; s++) {
         simulateOneStrat (s, numRepPerStrata[s]);
         statAvg.addStat ();
         statVar.addStat ();
         statVarProp.addStat ();
         statSigma.addStat ();
         if (cvStat != null) {
            cvStat.applyControlVariables (pilotRuns[s].getBetas ());
            statAvgCV.addStat ();
            statVarCV.addStat ();
            statVarPropCV.addStat ();
            statSigmaCV.addStat ();
         }
      }
      finishEvaluation ();
   }

   @Override
   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      return statAvg.getMatrixOfStatProbes (m);
   }

   @Override
   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType m) {
      return statAvg.getMatrixOfTallies (m);
   }

   @Override
   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType m) {
      return statAvg.getMatrixOfFunctionOfMultipleMeansTallies (m);
   }

   // getPerformanceMeasure returns the statAvg.average,
   // which is the stratified average

   public DoubleMatrix2D getVariance (PerformanceMeasureType pm, boolean prop, boolean cv) {
      MatrixOfTallies<?> mta;
      if (prop) {
         // For proportional allocation, we need the average
         // of the variance across strata.
         if (cv && cvStat != null)
            mta = statVarPropCV.getMatrixOfTallies (pm);
         else
            mta = statVarProp.getMatrixOfTallies (pm);
      }
      else if (cv && cvStat != null)
         mta = statVarCV.getMatrixOfTallies (pm);
      else
         mta = statVar.getMatrixOfTallies (pm);

      final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta.columns ());
      mta.average (var);
      if (!prop)
         var.assign (Functions.mult (totalNumRep / simParams.getNumStrata ()));
      return var;
   }

   public DoubleMatrix2D getVarianceProp (PerformanceMeasureType pm) {
      return getVariance (pm, true, cvStat != null);
   }

   @Override
   public DoubleMatrix2D getVariance (PerformanceMeasureType pm) {
      return getVariance (pm, false, cvStat != null);
   }

   public DoubleMatrix2D getVarMeanAcrossStrata (PerformanceMeasureType pm, boolean cv) {
      DoubleMatrix2D m;
      if (cv && cvStat != null)
         m = statAvgCV.getVariance (pm);
      else
         m = statAvg.getVariance (pm);
      final int numStrata = simParams.getNumStrata ();
      final double factor = (numStrata - 1.0) / numStrata;
      m.assign (Functions.mult (factor));
      return m;
   }

   public DoubleMatrix2D getVarStandardDeviationAcrossStrata (PerformanceMeasureType pm, boolean cv) {
      DoubleMatrix2D m;
      if (cv && cvStat != null)
         m = statSigmaCV.getVariance (pm);
      else
         m = statSigma.getVariance (pm);
      final int numStrata = simParams.getNumStrata ();
      final double factor = (numStrata - 1.0) / numStrata;
      m.assign (Functions.mult (factor));
      return m;
   }

   @Override
   public DoubleMatrix2D getMin (PerformanceMeasureType pm) {
      throw new NoSuchElementException();
   }

   @Override
   public DoubleMatrix2D getMax (PerformanceMeasureType pm) {
      throw new NoSuchElementException();
   }

   @Override
   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType m,
         double level) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      if (!(sm instanceof MatrixOfTallies)
            && !(sm instanceof MatrixOfFunctionOfMultipleMeansTallies))
         throw new NoSuchElementException (
               "No available confidence interval for " + m.getDescription ());
      final DoubleMatrix2D[] res = new DoubleMatrix2D[2];
      res[0] = new DenseDoubleMatrix2D (sm.rows (), sm.columns ());
      res[1] = new DenseDoubleMatrix2D (sm.rows (), sm.columns ());
      final double[] cr = new double[2];
      final DoubleMatrix2D average = getPerformanceMeasure (m);
      final DoubleMatrix2D variance = getVariance (m);
      for (int i = 0; i < sm.rows (); i++)
         for (int j = 0; j < sm.columns (); j++) {
            // StatProbe probe = sm.getProbe (i, j);
            final double avg = average.getQuick (i, j);
            final double var = variance.getQuick (i, j);
            try {
               final int n = totalNumRep;
               double t;
               // if (probe instanceof Tally)
               // t = StudentDist.inverseF (n - 1, 0.5 * (level + 1));
               // else if (probe instanceof FunctionOfMultipleMeansTally)
               // t = NormalDist.inverseF01 (0.5 * (level + 1));
               // else
               // throw new RuntimeException ();
               t = NormalDist.inverseF01 (0.5 * (level + 1));
               cr[0] = avg;
               cr[1] = t * Math.sqrt (var / n);
            }
            catch (final RuntimeException re) {
               throw new NoSuchElementException (
                     "No available confidence interval for "
                           + m.getDescription ());
            }
            res[0].set (i, j, cr[0] - cr[1]);
            res[1].set (i, j, cr[0] + cr[1]);
         }
      return res;
   }

   @Override
   public void reset () {
      super.reset ();
      statAvg = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.AVERAGE, true);
      statVarProp = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCE, true);
      statVar = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCEOFAVERAGE, true);
      statSigma = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.STANDARDDEVIATION, true);
      //cvStat = null;
      pilotRunsDone = false;
   }

   @Override
   public void reset (CallCenterParams ccParams, SimParams simParams1) throws CallCenterCreationException {
      super.reset (ccParams, simParams1);
      statAvg = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.AVERAGE, true);
      statVarProp = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCE, true);
      statVar = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.VARIANCEOFAVERAGE, true);
      statSigma = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.STANDARDDEVIATION, true);
      //cvStat = null;
      pilotRunsDone = false;
   }

   @Override
   protected SimLogic createSimLogic (CallCenter cc, SimParams simParams1) {
      return new StratRepLogic (cc, (StratSimParams) simParams1, this);
   }

   private static class StratRepLogic extends RepLogic {
      private CallCenterSimStrat ccSim;

      public StratRepLogic (CallCenter cc, StratSimParams simParams,
            CallCenterSimStrat ccSim) {
         super (cc, simParams, ccSim.getPerformanceMeasures ());
         this.ccSim = ccSim;
      }

      @Override
      public void formatReport (Map<String, Object> evalInfo) {
         final StratSimParams simParams = (StratSimParams) getSimParams ();
         evalInfo.put ("Number of strata", simParams.getNumStrata ());
         evalInfo.put ("Total number of replications",
               ccSim.totalNumRep);
         if (ccSim.pmOpt == null)
            evalInfo.put ("Allocation type", "Proportional");
         else {
            evalInfo.put ("Allocation type", "Optimal");
            evalInfo.put ("Optimized performance measure type", ccSim.pmOpt.getDescription ());
            evalInfo.put ("Optimized performance measure row", ccSim.rowOpt);
            evalInfo.put ("Optimized performance measure column", ccSim.colOpt);
         }
      }
   }

   private static class StratData {
      private Map<PerformanceMeasureType, double[][][]> betas;
      private Map<PerformanceMeasureType, DoubleMatrix2D> sigmas;
      private Map<PerformanceMeasureType, DoubleMatrix2D> sigmasCV;

      public StratData (CallCenterSimStrat sim) {
         betas = new EnumMap<PerformanceMeasureType, double[][][]> (PerformanceMeasureType.class);
         sigmas = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (PerformanceMeasureType.class);
         sigmasCV = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (PerformanceMeasureType.class);

         final CallCenterStatProbes stat = sim.getSimLogic().getCallCenterStatProbes ();
         for (final PerformanceMeasureType pm : stat.getPerformanceMeasures ()) {
            final DoubleMatrix2D stdDev = stat.getVariance (pm);
            stdDev.assign (Functions.sqrt);
            sigmas.put (pm, stdDev);
         }
         if (sim.cvStat != null) {
            sim.cvStat.applyControlVariables ();
            for (final PerformanceMeasureType pm : sim.cvStat.getPerformanceMeasures ()) {
               betas.put (pm, sim.cvStat.getBetas (pm));
               final DoubleMatrix2D stdDev = sim.cvStat.getVariance (pm);
               stdDev.assign (Functions.sqrt);
               sigmasCV.put (pm, stdDev);
            }
         }
      }

      public Map<PerformanceMeasureType, double[][][]> getBetas() {
         return betas;
      }

      public double[][][] getBetas (PerformanceMeasureType pm) {
         return betas.get (pm);
      }

      public Map<PerformanceMeasureType, DoubleMatrix2D> getSigmas() {
         return sigmas;
      }

      public DoubleMatrix2D getSigmas (PerformanceMeasureType pm) {
         return sigmas.get (pm);
      }

      public Map<PerformanceMeasureType, DoubleMatrix2D> getSigmasCV() {
         return sigmasCV;
      }

      public DoubleMatrix2D getSigmasCV (PerformanceMeasureType pm) {
         return sigmasCV.get (pm);
      }
   }

   /**
    * Main method allowing to run this class from the command-line. The needed
    * command-line arguments are the name of an XML file containing the
    * non-stationary simulation parameters (root element \texttt{mskccparams}),
    * and the name of a second XML file containing the simulation parameters
    * (root elements \texttt{batchsimparams} or \texttt{repsimparams}).
    *
    * @param args
    *           the command-line arguments.
    */
   public static void main (String[] args) throws IOException,
         JAXBException, CallCenterCreationException {
      if (args.length != 2 && args.length != 3) {
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.msk.CallCenterSimStrat "
                     + "[call center data file name] [experiment parameter file]");
         System.exit (1);
      }
      if (!new File (args[0]).exists ()) {
         System.err.println ("Cannot find the file " + args[0]);
         System.exit (1);
      }
      if (!new File (args[1]).exists ()) {
         System.err.println ("Cannot find the file " + args[1]);
         System.exit (1);
      }
      File outputFile = null;
      if (args.length == 3)
         outputFile = new File (args[2]);

      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      final SimParamsConverter cnvSim = new SimParamsConverter();

      final CallCenterParams ccParams;
      final StratSimParams simParams;
      try {
         ccParams = cnvCC.unmarshal (new File (args[0]));
         simParams = (StratSimParams)cnvSim.unmarshal (new File (args[1]));
      }
      catch (final JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
         return;
      }
      final CallCenterSimStrat sim = new CallCenterSimStrat (ccParams,
            simParams);

      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (), args[0], args[1]);
      sim.eval ();
      PerformanceMeasureFormat.formatResults (sim, outputFile);

      DoubleMatrix2D m = sim.getVarMeanAcrossStrata (PerformanceMeasureType.RATEOFINTARGETSL, false);
      System.out.printf ("Mean across strata: %.3f\n", m.get (m.rows () - 1, m.columns () - 1));
      m = sim.getVarStandardDeviationAcrossStrata (PerformanceMeasureType.RATEOFINTARGETSL, false);
      System.out.printf ("Standard deviation across strata: %.3f\n", m.get (m.rows () - 1, m.columns () - 1));
      m = sim.getVarMeanAcrossStrata (PerformanceMeasureType.RATEOFINTARGETSL, true);
      System.out.printf ("Mean across strata with CV: %.3f\n", m.get (m.rows () - 1, m.columns () - 1));
      m = sim.getVarStandardDeviationAcrossStrata (PerformanceMeasureType.RATEOFINTARGETSL, true);
      System.out.printf ("Standard deviation across strata with CV: %.3f\n", m.get (m.rows () - 1, m.columns () - 1));
   }
}
