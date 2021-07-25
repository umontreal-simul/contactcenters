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


public class BasicCallCenterCTMCSimMP extends AbstractCallCenterCTMCSimMP {
   protected double[] numExpectedTransitions;

   // PxKxL_k 3D array of distributions
   private double[][][] timeDist;
   
   public BasicCallCenterCTMCSimMP (CallCenter cc, CTMCRepSimParams simParams) throws CTMCCreationException {
      super (cc, simParams);
      init();
   }
   
   public BasicCallCenterCTMCSimMP (CallCenterParams ccParams, CTMCRepSimParams simParams) throws CallCenterCreationException, CTMCCreationException {
      super (ccParams, simParams);
      init();
   }
   
   private void init() {
      timeDist = new double[cc.getNumMainPeriods ()][][];
      for (int mp = 0; mp < timeDist.length; mp++) {
         final double st = cc.getPeriodChangeEvent ().getPeriodStartingTime (mp + 1);
         final double et = cc.getPeriodChangeEvent ().getPeriodEndingTime (mp + 1);
         timeDist[mp] = rateChange.getTimeDist (st, et);
      }
   }

   @Override
   public double[] getNumExpectedTransitions () {
      return numExpectedTransitions;
   }
   
   @Override
   public void simulate (RandomStream stream, int n) {
      numExpectedTransitions = new double[ctmc.length - 1];
      PoissonDist[] pdist = new PoissonDist[ctmc.length - 1];
      int[] ntr = new int[ctmc.length - 1];
      RateChangeInfo[][] changes = new RateChangeInfo[cc.getNumMainPeriods ()][];
      for (int mp = 0; mp < ctmc.length - 1; mp++) {
         numExpectedTransitions[mp] = ctmc[mp].getJumpRate ()*cc.getPeriodDuration ();
         pdist[mp] = new PoissonDist (numExpectedTransitions[mp]);
      }
      for (int r = 0; r < n; r++) {
         for (int mp = 0; mp < ntr.length; mp++)
            ntr[mp] = pdist[mp].inverseFInt (stream.nextDouble ());
         for (int mp = 0; mp < ntr.length; mp++) 
            changes[mp] = rateChange.generateRateChanges (stream, timeDist[mp], ntr[mp]);
         initReplication (stream, ntr);
         int targetTr = 0, lastTargetTr = 0;
         for (int mp = 0; mp < ntr.length; mp++) {
            if (mp == 0) {
               ctmc[mp].initEmpty ();
               counters[mp].init (ctmc[mp], cc.getPeriodDuration (), ntr[mp]);
               startingTransition[mp] = ctmc[mp].getNumTransitionsDone ();
            }
            else {
               ctmc[mp].init (ctmc[mp - 1]);
               counters[mp].init (ctmc[mp], cc.getPeriodDuration (), ntr[mp]);
               startingTransition[mp] = ctmc[mp].getNumTransitionsDone ();
               for (int ii = 0; ii < ctmc[mp].getNumAgentGroups (); ii++) {
                  while (ctmc[mp].selectContact (ii))
                     counters[mp].collectStat (ctmc[mp], TransitionType.ENDSERVICEANDDEQUEUE);
               }
            }
            if (rateChange.hasChanges ()) {
               for (int k = 0; k < timeDist[mp].length; k++)
                  if (timeDist[mp][k] != null)
                     ctmc[mp].setArrivalRate (k, 0);
            }
            targetTr += ntr[mp];
            ctmc[mp].setTargetNumTransitions (targetTr);
            notifyInit (r, mp, ctmc[mp]);
            int idx = 0;
            for (int tr = targetTr - ntr[mp]; tr < targetTr; tr++) {
               assert tr == ctmc[mp].getNumTransitionsDone ();
               while (idx < changes[mp].length && changes[mp][idx].getTransition () + lastTargetTr <= tr) {
                  ctmc[mp].setArrivalRate (changes[mp][idx].getK (), changes[mp][idx].getRate ());
                  ++idx;
               }
               final TransitionType type = ctmc[mp].nextStateInt (stream.nextInt (0, Integer.MAX_VALUE));
               tr += ctmc[mp].getNumPrecedingFalseTransitions ();
               notifyTransition (r, mp, ctmc[mp], type);
               AbstractCallCenterCTMCSim.traceStep (ctmc[mp], type, trace, r, mp + 1);
               counters[mp].collectStat (ctmc[mp], type);
               tr += ctmc[mp].getNumFollowingFalseTransitions();
            }
            counters[mp].updateStatOnTime (ctmc[mp]);
            lastTargetTr = targetTr;
         }
         CallCenterCTMC ctmcL = ctmc[ctmc.length - 1]; 
         ctmcL.init (ctmc[ctmc.length - 2]);
         ctmcL.setTargetNumTransitions (Integer.MAX_VALUE);
         startingTransition[ctmc.length - 1] = ctmcL.getNumTransitionsDone ();
         counters[counters.length - 1].init (ctmc[ctmc.length - 1], cc.getPeriodDuration (), 1);
         while (ctmcL.getNumContactsInQueue () > 0 || ctmcL.getNumContactsInService () > 0) {
            final TransitionType type = ctmcL.nextStateInt (stream.nextInt (0, Integer.MAX_VALUE));
            AbstractCallCenterCTMCSim.traceStep (ctmcL, type, trace, r, ctmc.length);
            counters[counters.length - 1].collectStat (ctmcL, type);
         }
         
         addObs();
         stream.resetNextSubstream();
      }
   }
   
   public static void main (String[] args) {
      if (args.length != 2 && args.length != 3) {
         System.err.println ("Usage: java BasicCallCenterCTMCSimMP ccParams simParams [output file]");
         System.exit (1);
      }
      
      String ccPsFn = args[0];
      String simPsFn = args[1];
      File outputFile;
      if (args.length > 2)
         outputFile = new File (args[2]);
      else
         outputFile = null;
      
      CallCenterParamsConverter cnv = new CallCenterParamsConverter();
      CallCenterParams ccParams = cnv.unmarshalOrExit (new File (ccPsFn));
      SimParamsConverter cnvSim = new SimParamsConverter();
      CTMCRepSimParams simParams = (CTMCRepSimParams)cnvSim.unmarshalOrExit (new File (simPsFn));
      
      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());
      BasicCallCenterCTMCSimMP sim;
      try {
         sim = new BasicCallCenterCTMCSimMP (ccParams, simParams);
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
