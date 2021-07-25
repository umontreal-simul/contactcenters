package umontreal.iro.lecuyer.contactcenters.app;

import java.util.NoSuchElementException;

/**
 * Represents a contact center simulator producing sets of observations for
 * performance measures. The definition of a set of observations depends on the
 * specific simulator; it can correspond to a macro-replication, a stratum, etc.
 */
public interface ContactCenterSimWithObservationSets extends ContactCenterSim {
   /**
    * Returns the number of sets of observations available for a performance
    * measure of type \texttt{pm}, identified by row \texttt{row} and column
    * \texttt{column}. If the number of sets of observations is not available
    * for the given performance measure, this method throws a
    * {@link NoSuchElementException}.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row of the performance measure.
    * @param column
    *           the column of the performance measure.
    * @return the number of sets of observations.
    * @exception NoSuchElementException
    *               if the observations are not available for the given
    *               performance measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{row} or \texttt{column} are out of bounds.
    */
   public int getNumObservationSets (PerformanceMeasureType pm, int row,
         int column);

   /**
    * Returns the number of observations available in the set \texttt{set} for a
    * performance measure of type \texttt{pm}, identified by row \texttt{row}
    * and column \texttt{column}. If the number of observations is not available
    * for the given performance measure, this method throws a
    * {@link NoSuchElementException}.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row of the performance measure.
    * @param column
    *           the column of the performance measure.
    * @param set
    *           the index of the set of observations.
    * @return the number of observations.
    * @exception NoSuchElementException
    *               if the observations are not available for the given
    *               performance measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{row}, \texttt{column}, or \texttt{set} are out of
    *               bounds.
    */
   public int numberObs (PerformanceMeasureType pm, int row, int column, int set);

   /**
    * Returns the number of observations available in the set \texttt{set} for a
    * performance measure of type \texttt{pm}, identified by row \texttt{row}
    * and column \texttt{column}. If the number of observations is not available
    * for the given performance measure, this method throws a
    * {@link NoSuchElementException}.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row of the performance measure.
    * @param column
    *           the column of the performance measure.
    * @param set
    *           the index of the set of observations.
    * @return the number of observations.
    * @exception NoSuchElementException
    *               if the observations are not available for the given
    *               performance measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{row}, \texttt{column}, or \texttt{set} are out of
    *               bounds.
    */
   public double[] getObs (PerformanceMeasureType pm, int row, int column,
         int set);
}
