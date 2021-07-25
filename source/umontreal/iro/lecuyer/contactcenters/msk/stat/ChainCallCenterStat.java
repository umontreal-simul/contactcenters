package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

import umontreal.iro.lecuyer.collections.MergedMap;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Combines the matrices of statistical probes from two call center statistical
 * objects. Two implementations of {@link CallCenterStatProbes} are associated
 * with each instance of this class. Each time a matrix of statistical probes is
 * queried, this class queries the first inner call center statistic object. If
 * the matrix is available, it returns it, otherwise, it queries the second
 * inner object. This results in combining the statistics available in both
 * objects.
 */
public class ChainCallCenterStat implements CallCenterStatProbes {
   private CallCenterStatProbes stat1;
   private CallCenterStatProbes stat2;

   /**
    * Constructs a new chained call center statistical object from inner objects
    * \texttt{stat1} and \texttt{stat2}.
    * 
    * @param stat1
    *           the first statistical object.
    * @param stat2
    *           the second statistical object.
    */
   public ChainCallCenterStat (CallCenterStatProbes stat1,
         CallCenterStatProbes stat2) {
      if (stat1 == null || stat2 == null)
         throw new NullPointerException ("stat1 and stat2 must not be null");
      this.stat1 = stat1;
      this.stat2 = stat2;
   }

   public void init () {
      stat1.init ();
      stat2.init ();
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      final Set<PerformanceMeasureType> pmSet = new LinkedHashSet<PerformanceMeasureType> ();
      for (final PerformanceMeasureType pm : stat1.getPerformanceMeasures ())
         pmSet.add (pm);
      for (final PerformanceMeasureType pm : stat2.getPerformanceMeasures ())
         pmSet.add (pm);
      return pmSet.toArray (new PerformanceMeasureType[pmSet.size ()]);
   }

   public boolean hasPerformanceMeasure (PerformanceMeasureType pm) {
      return stat1.hasPerformanceMeasure (pm)
            || stat2.hasPerformanceMeasure (pm);
   }
   
   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes () {
      return new MergedMap<PerformanceMeasureType, MatrixOfStatProbes<?>> (stat1.getMatricesOfStatProbes(), stat2.getMatricesOfStatProbes());
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMatrixOfStatProbes (pm);
      else
         return stat2.getMatrixOfStatProbes (pm);
   }

   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMatrixOfTallies (pm);
      else
         return stat2.getMatrixOfTallies (pm);
   }

   public MatrixOfTallies<TallyStore> getMatrixOfTallyStores (
         PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMatrixOfTallyStores (pm);
      else
         return stat2.getMatrixOfTallyStores (pm);
   }

   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMatrixOfFunctionOfMultipleMeansTallies (pm);
      else
         return stat2.getMatrixOfFunctionOfMultipleMeansTallies (pm);
   }

   public DoubleMatrix2D getAverage (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getAverage (pm);
      else
         return stat2.getAverage (pm);
   }

   public DoubleMatrix2D getMax (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMax (pm);
      else
         return stat2.getMax (pm);
   }

   public DoubleMatrix2D getMin (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getMin (pm);
      else
         return stat2.getMin (pm);
   }

   public DoubleMatrix2D getVariance (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getVariance (pm);
      else
         return stat2.getVariance (pm);
   }

   public DoubleMatrix2D getVarianceOfAverage (PerformanceMeasureType pm) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getVarianceOfAverage (pm);
      else
         return stat2.getVarianceOfAverage (pm);
   }

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType pm, double level) {
      if (stat1.hasPerformanceMeasure (pm))
         return stat1.getConfidenceInterval (pm, level);
      else
         return stat2.getConfidenceInterval (pm, level);
   }
}
