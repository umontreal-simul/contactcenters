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
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ExceptionUtil;


public class BasicCallCenterCTMCSim extends AbstractCallCenterCTMCSim {
   protected double numExpectedTransitions;
   private double[][] timeDist;
   
   public BasicCallCenterCTMCSim (CallCenter cc, CTMCRepSimParams simParams, int mp) throws CTMCCreationException {
      super (cc, simParams, mp);
   }
   
   public BasicCallCenterCTMCSim (CallCenterParams ccParams, CTMCRepSimParams simParams, int mp) throws CallCenterCreationException, CTMCCreationException {
      super (ccParams, simParams, mp);
   }

   @Override
   public double getNumExpectedTransitions () {
      return numExpectedTransitions;
   }
   
   //private static final int NUMRESETS = 20; 
   public void simulateTransitions (RandomStream stream, int i, double timeHorizon, int ntr) {
      RateChangeInfo[] changes = rateChange.generateRateChanges (stream, timeDist, ntr);
      ctmc.initEmpty();
      if (rateChange.hasChanges ()) {
         for (int k = 0; k < timeDist.length; k++)
            if (timeDist[k] != null)
               ctmc.setArrivalRate (k, 0);
      }
      ctmc.setTargetNumTransitions (ntr);
      initReplication (stream, timeHorizon, ntr);
      notifyInit (i, mp, ctmc);
      //int ntrReset = ntr / NUMRESETS;
      int idx = 0;
      for (int tr = 0; tr < ntr; tr++) {
         assert tr == ctmc.getNumTransitionsDone ();
         while (idx < changes.length && changes[idx].getTransition () <= tr) {
            ctmc.setArrivalRate (changes[idx].getK (), changes[idx].getRate ());
            ++idx;
         }
//         if (tr > 0 && tr % ntrReset == 0)
//            stream.resetNextSubstream ();
         final TransitionType type = ctmc.nextStateInt (stream.nextInt (0, Integer.MAX_VALUE));
         tr += ctmc.getNumPrecedingFalseTransitions ();
         notifyTransition (i, mp, ctmc, type);
         traceStep (ctmc, type, trace, i, mp);
         counters.collectStat (ctmc, type);
         tr += ctmc.getNumFollowingFalseTransitions();
//         final int nf = ctmc.getNumFollowingFalseTransitions();
//         for (int j = 0; j < nf && tr < ntr; j++, tr++)
//            counters.collectStat (ctmc, TransitionType.FALSETRANSITION, tr + 1);
      }
      counters.updateStatOnTime (ctmc);
      addObs();
   }

   @Override
   public void simulate (RandomStream stream, double timeHorizon, int n) {
      final double st = cc.getStartingTime ();
      final double et = st + timeHorizon;
      timeDist = rateChange.getTimeDist (st, et);
      numExpectedTransitions = ctmc.getJumpRate()*timeHorizon;
      PoissonDist pdist = new PoissonDist (numExpectedTransitions);
      for (int i = 0; i < n; i++) {
         int ntr = pdist.inverseFInt (stream.nextDouble());
         simulateTransitions (stream, i, timeHorizon, ntr);
         stream.resetNextSubstream();
      }
   }
   
   public static void main (String[] args) {
      if (args.length != 3 && args.length != 4) {
         System.err.println ("Usage: java BasicCallCenterCTMCSim ccParams simParams mp [output file]");
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
      BasicCallCenterCTMCSim sim;
      try {
         sim = new BasicCallCenterCTMCSim (ccParams, simParams, mp);
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
