package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.app.AbstractContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

import umontreal.iro.lecuyer.collections.MergedMap;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * This base class defines two maps that contain the statistical probes being
 * managed. The first map, {@link #tallyMap}, associates types of performance
 * measures with matrices of tallies. The second map, {@link #fmmTallyMap},
 * binds types of performance measures with matrices of function of multiple
 * means tallies. The methods in this class assume that every type of
 * performance measure do not appear in both maps.
 */
public class AbstractCallCenterStatProbes implements CallCenterStatProbes {
   /**
    * Map associating types of performance measures with matrices of tallies.
    */
   protected Map<PerformanceMeasureType, MatrixOfTallies<?>> tallyMap = new EnumMap<PerformanceMeasureType, MatrixOfTallies<?>> (
         PerformanceMeasureType.class);
   /**
    * Map associating types of performance measures with matrices of function of
    * multiple means tallies.
    */
   protected Map<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<?>> fmmTallyMap = new EnumMap<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<?>> (
         PerformanceMeasureType.class);
   private PerformanceMeasureType[] pms;

   protected void initPerformanceMeasures() {
      final List<PerformanceMeasureType> types = new ArrayList<PerformanceMeasureType> ();
      types.addAll (tallyMap.keySet ());
      types.addAll (fmmTallyMap.keySet ());
      pms = types.toArray (new PerformanceMeasureType[types.size ()]);
   }

   public void init () {
      for (final MatrixOfStatProbes<?> ms : tallyMap.values ())
         ms.init ();
      for (final MatrixOfStatProbes<?> ms : fmmTallyMap.values ())
         ms.init ();
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      if (pms == null)
         initPerformanceMeasures();
      return pms.clone ();
   }

   public boolean hasPerformanceMeasure (PerformanceMeasureType pm) {
      return tallyMap.containsKey (pm) || fmmTallyMap.containsKey (pm);
   }


   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes () {
      return new MergedMap<PerformanceMeasureType, MatrixOfStatProbes<?>> (tallyMap, fmmTallyMap);
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType pm) {
      MatrixOfStatProbes<?> ret = tallyMap.get (pm);
      if (ret == null)
         ret = fmmTallyMap.get (pm);
      if (ret == null)
         throw new NoSuchElementException (pm.getDescription ());
      return ret;
   }

   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType pm) {
      final MatrixOfTallies<?> ret = tallyMap.get (pm);
      if (ret == null)
         throw new NoSuchElementException (pm.getDescription ());
      return ret;
   }

   @SuppressWarnings ("unchecked")
   public MatrixOfTallies<TallyStore> getMatrixOfTallyStores (
         PerformanceMeasureType pm) {
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      if (mta.rows () == 0 || mta.columns () == 0
            || mta.get (0, 0) instanceof TallyStore)
         return (MatrixOfTallies<TallyStore>) mta;
      else
         throw new ClassCastException ();
   }

   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType pm) {
      final MatrixOfFunctionOfMultipleMeansTallies<?> ret = fmmTallyMap
            .get (pm);
      if (ret == null)
         throw new NoSuchElementException (pm.getDescription ());
      return ret;
   }

   public DoubleMatrix2D getAverage (PerformanceMeasureType pm) {
      final MatrixOfStatProbes<?> probes = getMatrixOfStatProbes (pm);
      final DoubleMatrix2D avg = new DenseDoubleMatrix2D (probes.rows (),
            probes.columns ());
      probes.average (avg);
      return avg;
   }

   public DoubleMatrix2D getMax (PerformanceMeasureType pm) {
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      final DoubleMatrix2D max = new DenseDoubleMatrix2D (mta.rows (), mta
            .columns ());
      for (int r = 0; r < max.rows (); r++)
         for (int c = 0; c < max.columns (); c++)
            max.setQuick (r, c, mta.get (r, c).max ());
      return max;
   }

   public DoubleMatrix2D getMin (PerformanceMeasureType pm) {
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      final DoubleMatrix2D min = new DenseDoubleMatrix2D (mta.rows (), mta
            .columns ());
      for (int r = 0; r < min.rows (); r++)
         for (int c = 0; c < min.columns (); c++)
            min.setQuick (r, c, mta.get (r, c).min ());
      return min;
   }

   public DoubleMatrix2D getVariance (PerformanceMeasureType pm) {
      final MatrixOfStatProbes<?> probes = getMatrixOfStatProbes (pm);
      if (probes instanceof MatrixOfFunctionOfMultipleMeansTallies) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> mta = getMatrixOfFunctionOfMultipleMeansTallies (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         mta.variance (var);
         return var;
      }
      else {
         final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         mta.variance (var);
         return var;
      }
   }

   public DoubleMatrix2D getVarianceOfAverage (PerformanceMeasureType pm) {
      final MatrixOfStatProbes<?> probes = getMatrixOfStatProbes (pm);
      final int nr = probes.rows ();
      final int nc = probes.columns ();
      if (probes instanceof MatrixOfFunctionOfMultipleMeansTallies) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> mta = getMatrixOfFunctionOfMultipleMeansTallies (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         mta.variance (var);
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
               var.setQuick (r, c, var.getQuick (r, c) / mta.get (r, c).numberObs ());
         return var;
      }
      else {
         final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         mta.variance (var);
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
               var.setQuick (r, c, var.getQuick (r, c) / mta.get (r, c).numberObs ());
         return var;
      }
   }

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType pm, double level) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (pm);
      return AbstractContactCenterSim.getConfidenceInterval (sm, level);
   }
}
