package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.EnumMap;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.ColumnType;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterEval;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;

/**
 * Provides a merged view of several single-period
 * {@link CallCenterStat} instances.
 * More specifically, for each supported type of
 * performance measure,
 * a matrix of tallies resulting from the concatenation
 * of all single-column matrices concerning
 * the same type of performance measure can be obtained through this object.
 * An object of this class is constructed using an array of
 * {@link CallCenterStat} instances.
 * Any update to these sets of statistical probes are
 * reflected on the merged view.
 * Matrices of statistical probes regrouping statistics for
 * each separate period, for a given type of performance measure,
 * can be obtained by using the method {@link #getMatrixOfStatProbes(PerformanceMeasureType)}. 
 */
public class CallCenterStatMP {
   private Map<PerformanceMeasureType, MatrixOfStatProbes<?>> pmTallies = new EnumMap<PerformanceMeasureType, MatrixOfStatProbes<?>> (PerformanceMeasureType.class);
   private PerformanceMeasureType[] pms;
   private CallCenterCTMC[] ctmc;
   private CallCenterStat[] ccStat;
   
   public CallCenterStatMP() {}

   /**
    * Constructs a new set of statistical probes from
    * the given $(P+1)$-dimensional arrays of CTMCs and
    * period-specific statistical counters.
    * The last element of \texttt{ctmc} corresponds to the CTMC
    * for the wrap-up period while the last element
    * of \texttt{ccStat} contains the collectors
    * concerning the complete horizon.
    * All other elements concern a specific main period.
    * @param ctmc the array of CTMCs.
    * @param ccStat the array of statistical probes.
    */
   public CallCenterStatMP (CallCenterCTMC[] ctmc, CallCenterStat[] ccStat) {
      this.ctmc = ctmc;
      this.ccStat = ccStat;
      if (ccStat.length == 0)
         return;
      pms = ccStat[ccStat.length - 1].getPerformanceMeasures();
      for (PerformanceMeasureType pm : pms) {
         MatrixOfStatProbes<?> msp = ccStat[ccStat.length - 1].getMatrixOfStatProbes (pm);
         if (ccStat.length == 1 || pm.getColumnType () != ColumnType.MAINPERIOD)
            pmTallies.put (pm, msp);
         else if (msp instanceof MatrixOfTallies) {
            MatrixOfTallies<Tally> mta = new MatrixOfTallies<Tally> (msp.rows(), ccStat.length);
            for (int mp = 0; mp < ccStat.length; mp++) {
               MatrixOfStatProbes<?> mspp = ccStat[mp].getMatrixOfStatProbes (pm);
               MatrixOfTallies<?> mtap = (MatrixOfTallies<?>)mspp;
               for (int r = 0; r < mtap.rows(); r++)
                  mta.set (r, mp, mtap.get (r, 0));
            }
            pmTallies.put (pm, mta);
         }
         else if (msp instanceof MatrixOfFunctionOfMultipleMeansTallies) {
            MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta =
               new MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> (msp.rows(), ccStat.length);
            for (int mp = 0; mp < ccStat.length; mp++) {
               MatrixOfStatProbes<?> mspp = ccStat[mp].getMatrixOfStatProbes (pm);
               MatrixOfFunctionOfMultipleMeansTallies<?> mtap = (MatrixOfFunctionOfMultipleMeansTallies<?>)mspp;
               for (int r = 0; r < mtap.rows(); r++)
                  mta.set (r, mp, mtap.get (r, 0));
            }
            pmTallies.put (pm, mta);
         }
      }
   }
   
   public PerformanceMeasureType[] getPerformanceMeasures () {
      if (pms == null)
         pms = pmTallies.keySet ().toArray (
               new PerformanceMeasureType[pmTallies.size ()]);
      return pms;
   }
   
   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes() {
      return pmTallies;
   }
   
   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      return (MatrixOfStatProbes<?>) pmTallies.get (m);
   }
   
   public void init() {
      for (int mp = 0; mp < ccStat.length; mp++)
         ccStat[mp].init (ctmc[mp]);
   }
   
   /**
    * Adds statistical information about the number of transitions
    * during each period
    * to a map of evaluation information.
    * Usually, the map is obtained using {@link ContactCenterEval#getEvalInfo()}, and
    * generated information is displayed in reports produced by a simulator.
    * @param evalInfo the evaluation information.
    * @param numExpectedTransitions the expected number of transitions, for
    * each period.
    */
   public void formatReport (Map<String, Object> evalInfo, double[] numExpectedTransitions) {
      if (ctmc.length > 2) {
         double[] jumpRates = new double[ctmc.length];
         double[] ntr = new double[ctmc.length - 1], nftr = new double[ctmc.length - 1],
         ptr = new double[ctmc.length - 1];
         for (int mp = 0; mp < jumpRates.length; mp++) {
            jumpRates[mp] = ctmc[mp].getJumpRate ();
            if (mp < jumpRates.length - 1) {
               ntr[mp] = ccStat[mp].statNumTransitions.average ();
               nftr[mp] = ccStat[mp].statNumFalseTransitions.average ();
               ptr[mp] = nftr[mp] / numExpectedTransitions[mp];
            }
         }
         evalInfo.put ("Per-period maximal transition rates", jumpRates);
         evalInfo.put ("Per-period average numbers of transitions",
               ntr);
         evalInfo.put ("Per-period average numbers of false transitions",
               nftr);
         evalInfo.put ("Per-period proportions of false transitions",
               ptr);
      }
      else
         evalInfo.put ("Maximal transition rate", ctmc[0].getJumpRate ());
      final double gntr = ccStat[ccStat.length - 1].statNumTransitions.average ();
      final double gnftr = ccStat[ccStat.length - 1].statNumFalseTransitions.average ();
      double e = 0;
      for (int mp = 0; mp < numExpectedTransitions.length; mp++)
         e += numExpectedTransitions[mp];
      evalInfo.put ("Average number of transitions",
            gntr);
      evalInfo.put ("Average number of false transitions",
            gnftr);
      evalInfo.put ("Proportion of false transitions",
            gnftr / e);
   }
}
