package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Map;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Encapsulates collectors containing statistics about a simulated call center.
 * This interface specifies a method mapping types of performance measures
 * to matrices of statistical probes. These matrices are constructed and updated
 * internally by the implementation. The updating method, which is
 * implementation-specific, often uses another set of call center probes, or
 * measures from a simulation logic.
 *
 * The main implementation of this interface is {@link SimCallCenterStat},
 * which uses an instance of {@link CallCenterMeasureManager} to obtain observations
 * for statistical collectors.
 */
public interface CallCenterStatProbes {
   /**
    * Initializes the statistical collectors contained in this object.
    */
   public void init ();

   /**
    * Returns the types of performance measures contained into the implemented
    * set of call center probes. If the implementing group of probes does not
    * contain any matrix of statistical probes, this method must return an array
    * with length 0 rather than \texttt{null}.
    *
    * @return the supported types of performance measures.
    */
   public PerformanceMeasureType[] getPerformanceMeasures ();

   /**
    * Determines if the implementing set of call center probes contains a matrix
    * of probes for the performance measure \texttt{pm}. This method returns
    * \texttt{true} if and only if {@link #getPerformanceMeasures()} returns an
    * array containing \texttt{pm}.
    *
    * @param pm
    *           the type of performance measure.
    * @return \texttt{true} if the measures are computed by the simulator,
    *         \texttt{false} otherwise.
    */
   public boolean hasPerformanceMeasure (PerformanceMeasureType pm);

   /**
    * Returns a map containing the matrix of statistical probes
    * for each type of performance measure.
    * @return the map of statistical probes.
    */
   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes();

   /**
    * Returns a matrix of statistical probes corresponding to the given type
    * \texttt{pm} of performance measure. If the type \texttt{pm} is not
    * supported, this method throws a {@link NoSuchElementException}.
    *
    * @param pm
    *           the type of performance measure.
    * @return the matrix of statistical probes.
    * @exception NoSuchElementException
    *               if the type of performance measure is not supported.
    */
   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType pm);

   /**
    * Returns a matrix of tallies corresponding to the given type \texttt{pm} of
    * performance measure. This method usually calls
    * {@link #getMatrixOfStatProbes(PerformanceMeasureType)} and casts the
    * results into a matrix of tallies.
    *
    * @param pm
    *           the type of performance measure.
    * @return the matrix of tallies.
    * @exception NoSuchElementException
    *               if the type of performance measure is not supported.
    */
   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType pm);

   /**
    * Returns a matrix of tallies corresponding to the given type \texttt{pm} of
    * performance measure. This method usually calls
    * {@link #getMatrixOfStatProbes(PerformanceMeasureType)} and casts the
    * results into a matrix of tallies that can store their observations.
    *
    * @param pm
    *           the type of performance measure.
    * @return the matrix of tallies.
    * @exception NoSuchElementException
    *               if the type of performance measure is not supported.
    */
   public MatrixOfTallies<TallyStore> getMatrixOfTallyStores (
         PerformanceMeasureType pm);

   /**
    * Returns a matrix of function of multiple means tallies corresponding to
    * the given type \texttt{pm} of performance measure. This method usually
    * calls {@link #getMatrixOfStatProbes(PerformanceMeasureType)} and casts the
    * results into a matrix of tallies.
    *
    * @param pm
    *           the type of performance measure.
    * @return the matrix of tallies.
    * @exception NoSuchElementException
    *               if the type of performance measure is not supported.
    */
   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType pm);

   public DoubleMatrix2D getAverage (PerformanceMeasureType pm);

   public DoubleMatrix2D getVariance (PerformanceMeasureType pm);

   public DoubleMatrix2D getVarianceOfAverage (PerformanceMeasureType pm);

   public DoubleMatrix2D getMin (PerformanceMeasureType pm);

   public DoubleMatrix2D getMax (PerformanceMeasureType pm);

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType pm, double level);
}
