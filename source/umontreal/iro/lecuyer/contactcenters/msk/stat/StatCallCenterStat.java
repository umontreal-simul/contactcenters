package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.EstimationType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Represents a set of statistical probes containing other statistics as
 * observations. An object of this class is constructed from another
 * implementation of {@link CallCenterStatProbes}, and a type of statistic to
 * collect. For each matrix of probes defined in the inner implementation,
 * a clone is made and stored into this object. When
 * {@link #addStat()} is called, each matrix of probes in the inner call center
 * statistical object is retrieved, an intermediate matrix of observations is
 * constructed, and the resulting matrix is added into the corresponding clone
 * stored into this object.
 *
 * This class is used for stratified sampling and randomized Quasi-Monte Carlo
 * simulation as follows. Each time a macroreplication or stratum is simulated,
 * statistics from a stratum or a randomization of a point set are available in
 * an inner {@link CallCenterStatProbes} implementation. This class can obtain
 * the averages, the variances, or the standard deviations from the probes, and
 * add them to other matrices of statistical probes. This results in averages of
 * averages, averages of variances, etc.
 */
public class StatCallCenterStat extends AbstractCallCenterStatProbes {
   private CallCenterStatProbes stat;
   private StatType statType;

   /**
    * Constructs a new group of call center statistical probes taking the
    * observations from the inner set of probes \texttt{stat}, and collecting
    * the statistic \texttt{statType}. This constructor creates a matrix of
    * statistical probes for each performance measure defined in \texttt{stat},
    * by using the \texttt{clone} method. However, if \texttt{fmm} is
    * \texttt{false}, no matrix of statistical probes is added for functions of
    * multiple means tallies.
    *
    * @param stat
    *           the input call center statistical probes.
    * @param statType
    *           the type of statistic collected.
    * @param fmm
    *           determines if functions of multiple means are processed.
    */
   public StatCallCenterStat (CallCenterStatProbes stat, StatType statType,
         boolean fmm) {
      this (stat, statType, fmm, false);
   }

   public StatCallCenterStat (CallCenterStatProbes stat, StatType statType,
            boolean fmm, boolean keepObs) {
      if (stat == null || statType == null)
         throw new NullPointerException();
      this.stat = stat;
      this.statType = statType;
      for (final PerformanceMeasureType pm : stat.getPerformanceMeasures ())
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
            if (fmm)
               tallyMap.put (pm,
                     createMatrixOfTallies (stat
                     .getMatrixOfFunctionOfMultipleMeansTallies (pm), keepObs));
         }
         else
            tallyMap.put (pm, createMatrixOfTallies (stat
                  .getMatrixOfTallies (pm), keepObs));
   }

   private MatrixOfTallies<?> createMatrixOfTallies (MatrixOfStatProbes<?> probes, boolean keepObs) {
      return keepObs ? MatrixOfTallies.createWithTally (probes.rows(), probes.columns()) :
         MatrixOfTallies.createWithTallyStore (probes.rows(), probes.columns());
   }

   /**
    * Adds new statistics to the probes defined by this object. Each matrix of
    * probes in the inner call center statistical object is retrieved, an
    * intermediate matrix of observations is constructed, and the resulting
    * matrix is added into the corresponding clone stored into this object.
    *
    * The way the matrix of observations is constructed depends on the type of
    * input matrix of probes. For matrices of tallies, element $(i, j)$ of the
    * matrix of observations is given by computing a statistic (average,
    * variance, etc.) on the observations of the tally $(i, j)$ of the inner
    * matrix of tallies. For matrices of functions of multiple means tallies, an
    * array of tallies corresponds to each element $(i, j)$, and a statistic is
    * extracted from each element of this array of tallies. This results in a 3D
    * array compatible with
    * {@link MatrixOfFunctionOfMultipleMeansTallies#add(double[][][])}.
    */
   public void addStat () {
      for (final Map.Entry<PerformanceMeasureType, MatrixOfTallies<?>> e : tallyMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final MatrixOfTallies<?> outStat = e.getValue ();
         final DoubleMatrix2D m;
         switch (statType) {
         case AVERAGE:
            m = stat.getAverage (pm);
            break;
         case VARIANCE:
            m = stat.getVariance (pm);
            break;
         case STANDARDDEVIATION:
            m = stat.getVariance (pm);
            m.assign (Functions.sqrt);
            break;
         case VARIANCEOFAVERAGE:
            m = stat.getVarianceOfAverage (pm);
            break;
         case STANDARDDEVIATIONOFAVERAGE:
            m = stat.getVarianceOfAverage (pm);
            m.assign (Functions.sqrt);
            break;
         default:
            throw new IllegalStateException
            ("Unsupported type of statistic");
         }
         outStat.add (m);
      }
   }
}
