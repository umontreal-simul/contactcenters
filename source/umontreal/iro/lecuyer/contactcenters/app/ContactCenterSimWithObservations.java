package umontreal.iro.lecuyer.contactcenters.app;

import java.util.NoSuchElementException;

/**
 * Represents a contact simulator capable of returning individual observations
 * for performance measures.
 */
public interface ContactCenterSimWithObservations extends ContactCenterSim {
   /**
    * Returns the number of observations available for a performance measure of
    * type \texttt{pm}, identified by row \texttt{row} and column
    * \texttt{column}. If the number of observations is not available for the
    * given performance measure, this method throws a
    * {@link NoSuchElementException}.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row of the performance measure.
    * @param column
    *           the column of the performance measure.
    * @return the number of observations.
    * @exception NoSuchElementException
    *               if the observations are not available for the given
    *               performance measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{row} or \texttt{column} are out of bounds.
    */
   public int numberObs (PerformanceMeasureType pm, int row, int column);

   /**
    * Returns an array containing the observations for a performance measure of
    * type \texttt{pm}, identified by row \texttt{row} and column
    * \texttt{column}. If the observations are not available for the given
    * performance measure, this method throws a {@link NoSuchElementException}.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row of the performance measure.
    * @param column
    *           the column of the performance measure.
    * @return the array of observations.
    * @exception NoSuchElementException
    *               if the observations are not available for the given
    *               performance measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{row} or \texttt{column} are out of bounds.
    */
   public double[] getObs (PerformanceMeasureType pm, int row, int column);
}
