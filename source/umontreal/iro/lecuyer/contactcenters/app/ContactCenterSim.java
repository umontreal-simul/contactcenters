package umontreal.iro.lecuyer.contactcenters.app;

import java.util.NoSuchElementException;

import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Represents a simulation-based evaluation system adapted
 * for contact centers.  Simulation uses some {@link RandomStream}
 * instances to generate random values, schedules events, and
 * generates samples of
 * observations which are used to estimate performance measures.
 * When simulation is used,
 * the {@link #getPerformanceMeasure} method, defined in {@link ContactCenterEval},
 * returns matrices of averages.
 * This interface extends the {@link ContactCenterEval} interface
 * to provide methods for obtaining matrices of sample variances,
 * minima, maxima, and confidence intervals.  If, for a group of measures
 * \texttt{m},
 * {@link #getPerformanceMeasure} returns an $a\times b$ matrix,
 * each of the methods of this interface must return a matrix with the
 * same dimensions if called with \texttt{m}.
 */
public interface ContactCenterSim extends ContactCenterEval {
   /**
    * Performs a simulation to evaluate the performance
    * measures.
    * Unless {@link #getAutoResetStartStream()}
    * returns \texttt{false}, if {@link #eval()} is called
    * multiple times without changing system
    * parameters, {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)}
    * should return the same matrices of estimates
    * after each call.
    * This requires that random streams used for simulation
    * be reset after each evaluation.
    * Thus,
    * before returning,
    * this method should use {@link RandomStream#resetStartSubstream}
    * on all random streams in order
    * to reset the seeds.
    * It is also recommended to always use
    * {@link RandomStream#resetNextSubstream()}
    * for all random streams
    * after any replication to improve synchronization
    * of random streams.
    */
   public void eval();

   /**
    * Changes the seeds of the random number generators used during
    * the simulation.
    * When calling {@link #eval} multiple times to perform
    * a simulation, the results should be identical for the same
    * values of parameters.  If one requires the simulation to
    * be performed with new random seeds, the random streams
    * need to be reset.
    * This can be done by calling {@link RandomStream#resetNextSubstream}
    * method on each {@link RandomStream} object associated with the simulator,
    * or by creating new random streams.
    */
   public void newSeeds();

   /**
    * Returns a matrix of sample variances for the group of
    * performance measures
    * \texttt{m}.  If the group of performance measures is not
    * supported, or the sample variance cannot be computed,
    * this method throws a {@link NoSuchElementException}.
    @param m the queried group of performance measures.
    @return the matrix of sample variances.
    @exception NoSuchElementException if the given group of performance
    measures is not supported, or the sample variance cannot be computed.
    @exception IllegalStateException if the values are not
    available.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public DoubleMatrix2D getVariance (PerformanceMeasureType m);

   /**
    * Returns a matrix of minimum values for the group of
    * performance measures
    * \texttt{m}.  If the group of measures defines no minimum
    * (e.g., a ratio of expectations), or it
    * is not supported, this method throws {@link NoSuchElementException}.
    @param m the queried group of performance measures.
    @return the matrix of minima.
    @exception NoSuchElementException if the given group of performance
    measures is not supported, or the minima cannot be computed.
    @exception IllegalStateException if the values are not
    available.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public DoubleMatrix2D getMin (PerformanceMeasureType m);

   /**
    * Returns a matrix of maximum values for the performance measure
    * \texttt{m}.  If the group of measures defines no maximum
    * (e.g., a ratio of expectations), or if it
    * is not supported, this method throws {@link NoSuchElementException}.
    @param m the queried group of performance measures.
    @return the matrix of maxima.
    @exception NoSuchElementException if the given group of performance
    measures is not supported, or the maxima cannot be computed.
    @exception IllegalStateException if the values are not
    available.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public DoubleMatrix2D getMax (PerformanceMeasureType m);

   /**
    * Returns confidence intervals on the means or
    * ratios of means, for the group of
    * performance measures \texttt{m}, with confidence
    * level \texttt{level}.  This must return an array
    * of two matrices, the first containing the lower bound
    * values and the second, the upper bound values.
    * For an unbounded confidence interval,
    * one of the two matrices can be \texttt{null}.
    * For each element of the performance measure matrix,
    * a confidence interval whose desired coverage probability
    * is \texttt{level} must be computed, independently of the other
    * elements in the matrix.  As a result, the coverage probability
    * of all computed intervals will be smaller than \texttt{level}.
    * The way each interval is computed is implementation-specific.
    @param m the queried group of performance measures.
    @param level desired probability that, for a given performance measure,
    the (random) confidence interval covers the true mean (a constant).
    @return an array of two matrices containing lower and upper bounds
    of the confidence intervals.
    @exception NoSuchElementException if the given group of performance
    measures is not supported or the confidence interval cannot be computed.
    @exception IllegalStateException if the values are not
    available.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public DoubleMatrix2D[] getConfidenceInterval
      (PerformanceMeasureType m, double level);
   /*
    * Confidence interval types could be influenced, in the future,
    * by evaluation options.
    */
   
   /**
    * Returns the confidence level of the intervals
    * output by {@link #formatStatistics}.
    * The initial confidence level is
    * implementation-specific, and
    * usually set by a
    * constructor.
    * @return the level of confidence for the intervals.
    */
   public double getConfidenceLevel();
   
   /**
    * Sets the level of confidence for the
    * intervals output by {@link #formatStatistics}
    * to \texttt{level}.
    * @param level the level of confidence of the intervals.
    * @exception IllegalArgumentException if \texttt{level}
    * is smaller than or equal to 0, or
    * greater than or equal to 1.
    */
   public void setConfidenceLevel (double level);

   /**
    * Returns the matrix of statistical probes
    * used to manage observations
    * for estimating the performance measures in group
    * \texttt{m}.  The particular subclass of the statistical
    * probe matrix depends on the performance measure type
    * only.  For averages, this method must return a
    * {@link MatrixOfTallies} object.
    * For functions of multiple averages, e.g., ratios of averages,
    * this must return a {@link MatrixOfFunctionOfMultipleMeansTallies}.
    @param m the group of performance measures of interest.
    @return the matrix of statistical probes.
    */
   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m);
   
   /**
    * Returns the matrix of tallies
    * used to manage observations
    * for estimating the performance measures in group
    * \texttt{m}.  This matrix is available only
    * for performance measures corresponding to
    * expectations, not functions of expectations.
    * @param m the group of performance measures of interest.
    * @return the matrix of tallies.
    */
   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType m);
   
   /**
    * Returns the matrix of function of multiple means tallies
    * used to manage observations
    * for estimating the performance measures in group
    * \texttt{m}.  This matrix is available only
    * for performance measures corresponding functions
    * of expectations.
    * @param m the group of performance measures of interest.
    * @return the matrix of function of multiple means tallies.
    */
   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (PerformanceMeasureType m);
  
   /**
    * Returns the number of completed steps
    * for the simulation.  When using
    * independent replications, a step corresponds
    * to a replication.  
    * When using batch means for
    * stationary simulation, this corresponds to the
    * number of terminated batches.
    @return the number of completed steps.
    */
   public int getCompletedSteps();

   /**
    * Determines if the random streams are automatically
    * reset at the end of each evaluation.
    * By default, a simulator calls {@link RandomStream#resetStartStream}
    * on each {@link RandomStream} object he has created for
    * {@link #eval} to use the same seeds if called multiple times.
    * If this option is set to \texttt{false},
    * the streams are not reset automatically, and
    * {@link #eval} always returns different results when called
    * multiple times.
    * However, {@link RandomStream#resetNextSubstream()}
    * should still be called for all random streams after
    * each replication.
    @return \texttt{true} if streams are reset automatically,
    \texttt{false} otherwise.
    */
   public boolean getAutoResetStartStream();

   /**
    * Sets the automatic reset start stream indicator
    * to \texttt{r}.
    @param r the new value of the indicator.
    @see #getAutoResetStartStream
    */
   public void setAutoResetStartStream (boolean r);
   
   /**
    * Determines if sequential sampling is done upon
    * each call on {@link #eval()}.
    * If the implemented simulator uses sequential sampling,
    * the number of steps (replications or batches) simulated
    * is random. By default, the first call to
    * {@link #eval()} determines the number of simulated
    * steps while each subsequent call to
    * {@link #eval()} simulates the exact same
    * number of steps, without reapplying
    * sequential sampling.
    * Turning this flag on changes this behavior,
    * forcing the simulator to perform
    * sequential sampling upon every call to
    * {@link #eval()}.
    * @return the value of the indicator.
    */
   public boolean getSeqSampEachEval();
   
   /**
    * Sets the indicator for sequential sampling on each eval
    * to \texttt{seqSamp}.
    * @param seqSamp the new value of the indicator.
    */
   public void setSeqSampEachEval (boolean seqSamp);

   /**
    * Calls {@link RandomStream#resetStartStream}
    * for all random streams used by the simulator.
    */
   public void resetStartStream();

   /**
    * Calls {@link RandomStream#resetStartSubstream}
    * for all random streams used by the simulator.
    */
   public void resetStartSubstream();

   /**
    * Calls {@link RandomStream#resetNextSubstream}
    * for all random streams used by the simulator.
    */
   public void resetNextSubstream();
}
