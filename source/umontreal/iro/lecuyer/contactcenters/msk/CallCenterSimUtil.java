package umontreal.iro.lecuyer.contactcenters.msk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.params.PrintedStatParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SequentialSamplingParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.ssj.simexp.SimExp;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.util.AbstractChrono;

public class CallCenterSimUtil {
   private CallCenterSimUtil () {}

   private static final PerformanceMeasureType[] pmsAll = CallCenterMeasureManager.getSupportedPerformanceMeasures ();
   static private class PMComparator implements Comparator<PerformanceMeasureType> {
      public int compare (PerformanceMeasureType o1, PerformanceMeasureType o2) {
         return o1.getDescription ().compareTo (o2.getDescription ());
      }
   }
   static {
      Arrays.sort (pmsAll, new PMComparator());
   }

   public static PerformanceMeasureType[] initPerformanceMeasures (
         SimParams simParams) {
      if (!simParams.isRestrictToPrintedStat ()
            || simParams.getReport ().getPrintedStats().size() == 0)
         return simParams.isEstimateContactTypeAgentGroup () ? 
               pmsAll.clone () :
                  filterContactTypeAgentGroup (pmsAll);
      final PerformanceMeasureType[] pms = new PerformanceMeasureType[simParams
            .getReport ().getPrintedStats().size()];
      int i = 0;
      for (final PrintedStatParams par : simParams.getReport().getPrintedStats())
         pms[i++] = PerformanceMeasureType.valueOf (par.getMeasure ());
      return simParams.isEstimateContactTypeAgentGroup () ? 
      pms : filterContactTypeAgentGroup (pms);
   }
   
   private static PerformanceMeasureType[] filterContactTypeAgentGroup (PerformanceMeasureType... pms) {
      final List<PerformanceMeasureType> pmsList = new ArrayList<PerformanceMeasureType> (pms.length);
      for (final PerformanceMeasureType pm : pms)
         if (!pm.getRowType ().isContactTypeAgentGroup ())
            pmsList.add (pm);
      return pmsList.toArray (new PerformanceMeasureType[pmsList.size ()]);
   }
   
   public static PerformanceMeasureType[] removeVQ (PerformanceMeasureType... pms) {
      final List<PerformanceMeasureType> pmsList = new ArrayList<PerformanceMeasureType> (pms.length);
      for (final PerformanceMeasureType pm : pms)
         if (!pm.name ().contains ("VQ"))
            pmsList.add (pm);
      return pmsList.toArray (new PerformanceMeasureType[pmsList.size ()]);
   }

   /**
    * Computes the number of additional replications or batches required for
    * reaching a certain precision.
    * 
    * @param ccStat the statistical probes of the call center.
    * @param seqSamp the parameters for sequential sampling.
    * @param verbose determines if the method logs information about
    * the number of required additional observations, for
    * each tested performance measure.
    * @return the number of additional observations required.
    */
   public static int getRequiredNewSteps (Map<PerformanceMeasureType, MatrixOfStatProbes<?>> ccStat,
         List<SequentialSamplingParams> seqSamp,
         boolean verbose) {
      StringBuilder sb = null;
      if (verbose) {
         sb = new StringBuilder ();
      // sb.append ("Simulated " + sim.getCompletedSteps ()
      // + " replications, computing error\n");
      }
      int nnewreps = 0;
      for (final SequentialSamplingParams seq : seqSamp) {
         final double targetError = seq.getTargetError();
         final double level = seq.getConfidenceLevel();
         final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (seq.getMeasure());
         final MatrixOfStatProbes<?> probes = ccStat.get (pm);
         int r;
         if (seq.isGlobalOnly())
            r = SimExp.getRequiredNewObservations(probes.get (probes.rows() - 1, probes.columns() - 1), targetError, level);
         else
            r = SimExp.getRequiredNewObservations(probes, targetError, level);
         if (r > nnewreps)
            nnewreps = r;
         if (verbose)
            sb.append ("For ").append (pm.name()).append
            (", need ").append (r).append (" more replications");
      }
      if (verbose) {
         final Logger logger = Logger
               .getLogger ("umontreal.iro.lecuyer.contactcenters.msk.sim"); //$NON-NLS-1$
         logger.info (sb.toString ());
      }
      return nnewreps;
   }

   public static int getRequiredNewSteps (CallCenterStatProbes ccStat,
         double targetError, double level, boolean onlyServiceLevel, int mp,
         boolean verbose) {
      StringBuilder sb = null;
      if (verbose) {
         sb = new StringBuilder ();
      // sb.append ("Simulated " + sim.getCompletedSteps ()
      // + " replications, computing error\n");
         }
      int nnewreps = 0;
      final MatrixOfFunctionOfMultipleMeansTallies<?> serviceLevel = ccStat
            .getMatrixOfFunctionOfMultipleMeansTallies (PerformanceMeasureType.SERVICELEVEL);
      int r = SimExp.getRequiredNewObservations (serviceLevel.viewColumn (mp), targetError, level);
      if (r > nnewreps)
         nnewreps = r;
      if (verbose)
         sb.append ("For service level in period " + mp + ", need " + r
               + " more replications");
      if (!onlyServiceLevel) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> abandonmentRatio = ccStat
               .getMatrixOfFunctionOfMultipleMeansTallies (PerformanceMeasureType.ABANDONMENTRATIO);
         r = SimExp.getRequiredNewObservations (abandonmentRatio.viewColumn (mp),
                                                  targetError, level);
         if (r > nnewreps)
            nnewreps = r;
         if (verbose)
            sb.append ("\nFor abandonment ratio, need " + r
                  + " more replications\n");
         final MatrixOfFunctionOfMultipleMeansTallies<?> waitingTime = ccStat
               .getMatrixOfFunctionOfMultipleMeansTallies (PerformanceMeasureType.WAITINGTIME);
         r = SimExp.getRequiredNewObservations (waitingTime.viewColumn (mp), targetError, level);
         if (r > nnewreps)
            nnewreps = r;
         if (verbose)
            sb.append ("For waiting time, need " + r + " more replications\n");
         final MatrixOfFunctionOfMultipleMeansTallies<?> occupancy = ccStat
               .getMatrixOfFunctionOfMultipleMeansTallies (PerformanceMeasureType.OCCUPANCY);
         r = SimExp.getRequiredNewObservations (occupancy.viewColumn (mp), targetError, level);
         if (r > nnewreps)
            nnewreps = r;
         if (verbose)
            sb.append ("For agents' occupancy ratio, need " + r
                  + " more replications");
      }
      if (verbose) {
         final Logger logger = Logger
               .getLogger ("umontreal.iro.lecuyer.contactcenters.msk.sim"); //$NON-NLS-1$
         logger.info (sb.toString ());
      }
      return nnewreps;
   }

   /**
    * Corrects the number of observations required to approximately enforce the
    * CPU time limit. This method estimates the CPU time for computing one
    * observation by dividing the CPU time elapsed by \texttt{nb}, and estimates
    * the maximal number of observations allowed without exceeding the CPU time
    * limit. The method then returns this number, or \texttt{nb} if the limit is
    * greater than \texttt{nb}.
    * 
    * @param nb
    *           the computed number of additional observations.
    * @return the corrected number of additional observations.
    */
   public static int checkCpuTimeLimit (double cpuTime, double limit, int steps, int nb, boolean verbose) {
      final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.msk"); //$NON-NLS-1$
      if (cpuTime > 0 && !Double.isInfinite (limit) && nb > 0) {
         if (cpuTime >= limit) {
            if (verbose)
               logger.info ("Elapsed CPU time " + AbstractChrono.format (cpuTime)
                     + " is greater than the limit, stopping "
                     + "the simulation");
            return 0;
         }
         final double timePerRep = cpuTime / steps;
         final double remainingTime = limit - cpuTime;
         final int maxNb = (int) (remainingTime / timePerRep);
         if (nb > maxNb) {
            if (verbose)
               logger.info ("The number of required observations " + nb
                     + " will be limited to " + maxNb
                     + " to avoid exceeding the CPU time limit");
            return maxNb;
         }
      }
      return nb;
   }
   
   public static double[] getObs (TallyStore tally) {
      int numberObs = tally.numberObs ();
      double[] inArray = tally.getArray ();
      if (inArray.length == numberObs)
         return inArray.clone ();
      double[] outArray = new double[numberObs];
      System.arraycopy (inArray, 0, outArray, 0, numberObs);
      return outArray;
   }
}
