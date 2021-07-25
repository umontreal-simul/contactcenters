package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.CTMCRepSimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ExceptionUtil;


public class IntMCallCenterCTMCSim extends AbstractCallCenterCTMCSim {
   static double EPSILON = 1e-8;
   protected double numExpectedTransitions;
   protected CallCenterCounters counters2;
   
   private PoissonDist pdist;
   private int trmin;

   public IntMCallCenterCTMCSim (CallCenter cc, CTMCRepSimParams simParams, int mp) throws CTMCCreationException {
      super (cc, simParams, mp);
      if (rateChange.hasChanges ())
         throw new CTMCCreationException("Arrival rate cannot change with time when using this simulator");
      init();
   }

   public IntMCallCenterCTMCSim (CallCenterParams ccParams, CTMCRepSimParams simParams, int mp) throws CTMCCreationException, CallCenterCreationException {
      super (ccParams, simParams, mp);
      init();
   }

   private void init() {
      counters2 = new CallCenterCounters (ctmc, awt, null,
            false,
            new double[] { ctmc.getJumpRate () },
            new int[] { 0 }, 0);
   }
   
   @Override
   public void reset() {
      super.reset ();
      init();
   }
   
   @Override
   public void newSeeds() {
      super.newSeeds ();
      init();
   }
   
   @Override
   public double getNumExpectedTransitions () {
      return numExpectedTransitions;
   }
   
   private void addWeighted (double prob) {
      counters.numTransitions += prob*counters2.numTransitions;
      counters.numFalseTransitions += prob*counters2.numFalseTransitions;
      final int numTypes = ctmc.getNumContactTypes();
      for (int k = 0; k < numTypes; k++) {
         counters.numBlocked[k] += prob*counters2.numBlocked[k];
         counters.numArrivals[k] += prob*counters2.numArrivals[k];
         counters.numAbandoned[k] += prob*counters2.numAbandoned[k];
         counters.queueSize[k] += prob*counters2.queueSize[k]; //  / (tr2 + 1.0)
         
         counters.sumWaitingTimesServed[k] += prob*counters2.sumWaitingTimesServed[k];
         counters.sumWaitingTimesAbandoned[k] += prob*counters2.sumWaitingTimesAbandoned[k];
      }
      for (int k = 0; k < counters.numServedBeforeAWT.length; k++) {
         counters.numServedBeforeAWT[k] += prob*counters2.numServedBeforeAWT[k];
         counters.numAbandonedBeforeAWT[k] += prob*counters2.numAbandonedBeforeAWT[k];
      }
      final int numGroups = ctmc.getNumAgentGroups();
      for (int i = 0; i < numGroups; i++)
         counters.busyAgents[i] += prob*counters2.busyAgents[i]; //  / (tr2 + 1.0)
      for (int i = 0; i < numGroups; i++)
         counters.totalAgents[i] += prob*counters2.totalAgents[i]; //  / (tr2 + 1.0)
      for (int ki = 0; ki < counters.servedRates.length; ki++)
         counters.servedRates[ki] += prob*counters2.servedRates[ki];
   }

   protected void collectStat (TransitionType type) {
      final int np = ctmc.getNumPrecedingFalseTransitions ();
      final int nf = ctmc.getNumFollowingFalseTransitions ();
      final int tr = ctmc.getNumTransitionsDone() - np - nf - 1;
      if (tr + np > trmin) {
         if (np > 0) {
            final int min = Math.max (trmin, tr);
            final int max = tr + np;
            final double prob = pdist.cdf (max) - pdist.cdf (min);
            addWeighted (prob);
         }
      }
      counters2.collectStat (ctmc, type);
      if (tr + np + nf + 1 >= trmin) {
         counters2.updateStatOnTime (ctmc);
         final int min = Math.max (trmin, tr + np + 1);
         final int max = tr + np + nf + 1;
         final double prob = min == max ? pdist.prob (min) : pdist.cdf (max) - pdist.cdf (min - 1);
         addWeighted (prob);
      }
   }
   
   @Override
   protected void initReplication (RandomStream stream, double timeHorizon, int ntr) {
      super.initReplication (stream, timeHorizon, ntr);
      counters2.init (ctmc, getTimeHorizon (), ntr);
   }
   
   public void simulateTransitions (RandomStream stream, int i, double timeHorizon, int ntr) {
      ctmc.initEmpty();
      ctmc.setTargetNumTransitions (ntr);
      initReplication (stream, timeHorizon, ntr);
      for (int tr = 0; tr < ntr; tr++) {
         TransitionType type = ctmc.nextStateInt (stream.nextInt (0, Integer.MAX_VALUE));
         tr += ctmc.getNumPrecedingFalseTransitions () + ctmc.getNumFollowingFalseTransitions();
         notifyTransition (i, mp, ctmc, type);
         traceStep (ctmc, type, trace, i, mp);
         collectStat (type);
      }
      addObs();
   }
   
   public static int getLowerBound (double lambda, double eps) {
      if (lambda < 0)
         throw new IllegalArgumentException();
      if (lambda < 25)
         return 0;
      final double eps2 = eps / 2;
      final double mode = (int)lambda;
      final double blam = (1 + 1/lambda)*Math.exp (1.0 / (8*lambda));
      int k = (int)Math.ceil (1 / Math.sqrt (2*lambda));
      while (blam*NormalDist.density (0, 1, k) / k > eps2)
         ++k;
      return (int)Math.ceil (mode - k*Math.sqrt (lambda) - 3.0 / 2); 
   }
   
   public static int getUpperBound (double lambda, double eps) {
      if (lambda < 0)
         throw new IllegalArgumentException();
      if (lambda == 0)
         return 0;
      if (lambda < 400)
         return getUpperBound (400, eps);
      final double eps2 = eps / 2;
      final double mode = (int)lambda;
      final double alam = (1 + 1/lambda)*Math.exp (1.0 / 16)*Math.sqrt (2);
      int k = (int)Math.ceil (1 / (2*Math.sqrt (2*lambda)));
      final int kmax = (int)Math.floor (Math.sqrt (lambda) / (2*Math.sqrt (2)));
      while (k < kmax && alam * dklam (k, lambda)*NormalDist.density (0, 1, k) / k > eps2)
         k++;
      return (int)Math.ceil (mode + k*Math.sqrt (2*lambda) + 3.0 / 2); 
   }
   
   private static double dklam (int k, double lambda) {
      final double v = k*Math.sqrt (2*lambda) + 3.0 / 2;
      final double exp = Math.exp (-2*v/9);
      return 1.0 / (1 - exp);
   }

   @Override
   public void simulate (RandomStream stream, double timeHorizon, int n) {
      numExpectedTransitions = ctmc.getJumpRate()*timeHorizon;
      pdist = new PoissonDist (numExpectedTransitions);
      trmin = getLowerBound (numExpectedTransitions, EPSILON);
      int ntrPerRun = getUpperBound (numExpectedTransitions, EPSILON);
      //trmin = Math.max ((int)(numExpectedTransitions - 4*Math.sqrt (numExpectedTransitions)), 0);
      //int ntrPerRun = (int)(numExpectedTransitions + 4*Math.sqrt (numExpectedTransitions));
      for (int i = 0; i < n; i++) {
         simulateTransitions (stream, i, timeHorizon, ntrPerRun);
         stream.resetNextSubstream();
      }
   }
   
   public static void main (String[] args) {
      if (args.length != 3 && args.length != 4) {
         System.err.println ("Usage: java IntMCallCenterCTMCSim ccParams simParams mp [output file]");
         System.exit (1);
      }
      
      String ccPsFn = args[0];
      String simPsFn = args[1];
      int mp = Integer.parseInt (args[2]);
      File outputFile;
      if (args.length > 3)
         outputFile = new File (args[3]);
      else
         outputFile = null;
      
      CallCenterParamsConverter cnv = new CallCenterParamsConverter();
      CallCenterParams ccParams = cnv.unmarshalOrExit (new File (ccPsFn));
      SimParamsConverter cnvSim = new SimParamsConverter();
      CTMCRepSimParams simParams = (CTMCRepSimParams)cnvSim.unmarshalOrExit (new File (simPsFn));
      
      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());
      IntMCallCenterCTMCSim sim;
      try {
         sim = new IntMCallCenterCTMCSim (ccParams, simParams, mp);
      }
      catch (CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }
      catch (CTMCCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }
      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo(), ccPsFn, simPsFn);
      sim.eval();

      try {
         PerformanceMeasureFormat.formatResults (sim, outputFile);
      }
      catch (IOException ioe) {
         System.err.println (ExceptionUtil.throwableToString (ioe));
         System.exit (1);
      }
      catch (JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
      }
   }
}
