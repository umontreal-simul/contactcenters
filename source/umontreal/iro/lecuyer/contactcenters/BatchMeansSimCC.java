package umontreal.iro.lecuyer.contactcenters;

import java.util.ArrayList;
import java.util.List;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simexp.BatchMeansSim;
import umontreal.ssj.stat.mperiods.IntegralMeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Extends {@link BatchMeansSim} to use matrices of measures for storing the
 * intermediate values $\boldV_j$'s of real batches. This class defines a list
 * of measure matrices which is automatically initialized after the warmup
 * period. When batch aggregation is turned OFF, these matrices contain a single
 * period and are reinitialized at the beginning of each batch. If batch
 * aggregation is turned ON, each period in measure matrices correspond to a
 * real batch.
 */
public abstract class BatchMeansSimCC extends BatchMeansSim {
   private final List<MeasureMatrix> matrices = new ArrayList<MeasureMatrix> ();

   /**
    * Calls {@link BatchMeansSim#BatchMeansSim(int,double,double) super}
    * \texttt{(minBatches, batchSize, warmupTime)}.
    */
   public BatchMeansSimCC (int minBatches, double batchSize, double warmupTime) {
      super (minBatches, batchSize, warmupTime);
   }

   /**
    * Calls {@link BatchMeansSim#BatchMeansSim(Simulator,int,double,double)}.
    */
   public BatchMeansSimCC (Simulator sim, int minBatches, double batchSize, double warmupTime) {
      super (sim, minBatches, batchSize, warmupTime);
   }

   /**
    * Calls {@link BatchMeansSim#BatchMeansSim(int,int,double,double) super}
    * \texttt{(minBatches, maxBatches, batchSize, warmupTime)}.
    */
   public BatchMeansSimCC (int minBatches, int maxBatches, double batchSize,
         double warmupTime) {
      super (minBatches, maxBatches, batchSize, warmupTime);
   }

   /**
    * Calls {@link BatchMeansSim#BatchMeansSim(Simulator,int,int,double,double)}.
    */
   public BatchMeansSimCC (Simulator sim, int minBatches, int maxBatches, double batchSize,
         double warmupTime) {
      super (sim, minBatches, maxBatches, batchSize, warmupTime);
   }

   /**
    * Returns the matrices of measures registered to this object. These matrices
    * must be capable of supporting multiple periods. The returned list should
    * contain non-\texttt{null} instances of {@link MeasureMatrix} only.
    *
    * @return the list of measure matrices.
    */
   public List<MeasureMatrix> getMeasureMatrices () {
      return matrices;
   }

   /**
    * Initializes registered matrices of measures if batch aggregation is turned
    * OFF.
    */
   @Override
   public void initBatchStat () {
      if (!getBatchAggregation ())
         ContactCenter.initElements (getMeasureMatrices ());
   }

   @Override
   public void allocateCapacity (int newCapacity) {
      for (final MeasureMatrix mat : getMeasureMatrices ())
         if (getBatchAggregation ())
            mat.setNumPeriods (newCapacity);
         else if (mat.getNumPeriods () != 1)
            mat.setNumPeriods (1);
   }

   @Override
   public void regroupRealBatches (int x) {
      if (getBatchAggregation ())
         for (final MeasureMatrix mat : getMeasureMatrices ())
            mat.regroupPeriods (x);
   }

   /**
    * Initializes the matrices of measures.
    */
   @Override
   public void initRealBatchProbes () {
      ContactCenter.initElements (getMeasureMatrices ());
   }

   /**
    * For each {@link IntegralMeasureMatrix} instance in the list returned by
    * {@link #getMeasureMatrices}, calls
    * {@link IntegralMeasureMatrix#newRecord}.
    */
   @Override
   public void addRealBatchObs () {
      for (final MeasureMatrix mat : getMeasureMatrices ())
         if (mat instanceof IntegralMeasureMatrix)
            ((IntegralMeasureMatrix<?>) mat).newRecord ();
   }

   /**
    * Copies the values corresponding to the current effective batch for the
    * measure matrix \texttt{mat} into the matrix \texttt{m}. The given matrix
    * of measures should be registered to this object for this method to be
    * used. The Colt matrix \texttt{m} must have one row for each measure, and a
    * single column. The method returns \texttt{m} after it is filled.
    *
    * @param mat
    *           the measure matrix for which the Colt matrix is required.
    * @param m
    *           the matrix receiving the results.
    * @param s
    *           the starting real batch.
    * @param h
    *           the number of real batches per effective batch.
    * @exception IllegalArgumentException
    *               if the dimensions of the matrix \texttt{m} are incompatible.
    */
   public static DoubleMatrix2D getBatchValues (MeasureMatrix mat,
         DoubleMatrix2D m, int s, int h) {
      if (m.rows () != mat.getNumMeasures () || m.columns () != 1)
         throw new IllegalArgumentException ("Invalid matrix dimension"
               + ", found " + m.rows () + "x" + m.columns () + ", but needs "
               + mat.getNumMeasures () + "x1");
      for (int i = 0; i < mat.getNumMeasures (); i++) {
         double v = 0;
         for (int j = 0; j < h; j++)
            v += mat.getMeasure (i, s + j);
         m.setQuick (i, 0, v);
      }
      return m;
   }

   /**
    * Constructs a matrix with one row for each measure in \texttt{mat} and a
    * single column, then calls
    * {@link #getBatchValues(MeasureMatrix,DoubleMatrix2D,int,int)}.
    */
   public static DoubleMatrix2D getBatchValues2D (MeasureMatrix mat, int s,
         int h) {
      return getBatchValues (mat, new DenseDoubleMatrix2D (mat
            .getNumMeasures (), 1), s, h);
   }

   /**
    * Equivalent to
    * {@link #getBatchValues(MeasureMatrix,DoubleMatrix2D,int,int)} for a
    * {@link DoubleMatrix1D} instance.
    */
   public static DoubleMatrix1D getBatchValues (MeasureMatrix mat,
         DoubleMatrix1D m, int s, int h) {
      if (m.size () != mat.getNumMeasures ())
         throw new IllegalArgumentException ("Invalid matrix dimension"
               + ", found " + m.size () + ", but needs "
               + mat.getNumMeasures ());
      for (int i = 0; i < mat.getNumMeasures (); i++) {
         double v = 0;
         for (int j = 0; j < h; j++)
            v += mat.getMeasure (i, s + j);
         m.setQuick (i, v);
      }
      return m;
   }

   /**
    * Constructs a matrix with one row for each measure in \texttt{mat}, and
    * calls {@link #getBatchValues(MeasureMatrix,DoubleMatrix1D,int,int)}.
    */
   public static DoubleMatrix1D getBatchValues1D (MeasureMatrix mat, int s,
         int h) {
      return getBatchValues (mat, new DenseDoubleMatrix1D (mat
            .getNumMeasures ()), s, h);
   }

   /**
    * Equivalent to
    * {@link #getBatchValues(MeasureMatrix,DoubleMatrix2D,int,int)} for an
    * array.
    */
   public static double[] getBatchValues (MeasureMatrix mat, double[] m, int s,
         int h) {
      if (m.length != mat.getNumMeasures ())
         throw new IllegalArgumentException ("Invalid array length"
               + ", found " + m.length + ", but needs " + mat.getNumMeasures ());
      for (int i = 0; i < mat.getNumMeasures (); i++) {
         double v = 0;
         for (int j = 0; j < h; j++)
            v += mat.getMeasure (i, s + j);
         m[i] = v;
      }
      return m;
   }

   /**
    * Constructs an array with one element for each measure in \texttt{mat}, and
    * calls {@link #getBatchValues(MeasureMatrix,double[],int,int)}.
    */
   public static double[] getBatchValues (MeasureMatrix mat, int s, int h) {
      return getBatchValues (mat, new double[mat.getNumMeasures ()], s, h);
   }

   /**
    * Normalizes the rows of the one-column matrix \texttt{m} using the batch
    * length \texttt{l}. Each row of the matrix is divided by \texttt{l} and the
    * modified matrix is returned.
    *
    * @param m
    *           the matrix being normalized.
    * @param l
    *           the batch length.
    * @return the modified matrix.
    */
   public static DoubleMatrix2D timeNormalize (DoubleMatrix2D m, double l) {
      if (m.columns () > 1)
         throw new IllegalArgumentException (
               "The matrix must have only one column.");
      m.assign (Functions.div (l));
      return m;
   }

   /**
    * Equivalent to {@link #timeNormalize(DoubleMatrix2D,double)} with an
    * instance of {@link DoubleMatrix1D}.
    */
   public static DoubleMatrix1D timeNormalize (DoubleMatrix1D m, double l) {
      m.assign (Functions.div (l));
      return m;
   }

   /**
    * Equivalent to {@link #timeNormalize(DoubleMatrix2D,double)} for an array.
    */
   public static double[] timeNormalize (double[] m, double l) {
      for (int r = 0; r < m.length; r++)
         m[r] /= l;
      return m;
   }
}
