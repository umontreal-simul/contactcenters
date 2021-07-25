package umontreal.ssj.stat.mperiods;

/**
 * Represents a matrix of measures for a set of related values during successive
 * simulation periods. For example, it can compute the number of served
 * customers of different types, for each simulation period. A period can be any
 * time interval such as half an hour, a complete day, a batch, etc. At the
 * beginning of a simulation, the matrix is initialized using the {@link #init}
 * method. During the simulation, it is updated with new events or values by
 * implementation-specific methods. An implementation of this interface computes
 * raw observations of a simulated system by counting the number of occurrences
 * of events, by summing values, or by computing integrals. At determined times,
 * e.g., at the end of a replication or a batch, these raw observations are
 * processed to be added into some statistical collectors. This interface
 * provides an abstraction layer to separate the computation of observations
 * from the required processing before they are collected.
 * 
 * Some methods specified by this interface are mandatory whereas others are
 * optional. When an unsupported optional method is called, its implementation
 * simply throws an {@link UnsupportedOperationException}.
 */
public interface MeasureMatrix {
   /**
    * Initializes this matrix of measures for a new simulation replication. This
    * resets the measured values to 0, or initializes the probes used to compute
    * them.
    */
   public void init ();

   /**
    * Returns the number of measures calculated by the implementation of this
    * interface.
    * 
    * @return the number of computed values.
    */
   public int getNumMeasures ();

   /**
    * Sets the number of measures to \texttt{nm}. If this method is supported,
    * it can limit the maximal or minimal accepted number of measures.
    * 
    * @param nm
    *           the new number of measures.
    * @exception IllegalArgumentException
    *               if the given number is negative or not accepted.
    * @exception UnsupportedOperationException
    *               if the number of measures cannot be changed.
    */
   public void setNumMeasures (int nm);

   /**
    * Returns the number of periods stored into this matrix of measures.
    * 
    * @return the number of stored periods.
    */
   public int getNumPeriods ();

   /**
    * Sets the number of periods of this matrix to \texttt{np}. If this method
    * is supported, it can limit the maximal or minimal accepted number of
    * periods.
    * 
    * @param np
    *           the new number of periods.
    * @exception IllegalArgumentException
    *               if the given number is negative or not accepted.
    * @exception UnsupportedOperationException
    *               if the number of periods cannot be changed.
    */
   public void setNumPeriods (int np);

   /**
    * Returns the measure corresponding to the index~\texttt{i} and
    * period~\texttt{p}.
    * 
    * @param i
    *           the index of the measure.
    * @param p
    *           the period of the measure.
    * @return the corresponding value.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} or \texttt{p} are negative or greater than or
    *               equal to the number of measures or the number of periods,
    *               respectively.
    */
   public double getMeasure (int i, int p);

   /**
    * Increases the length of stored periods by regrouping them. If this method
    * is supported, for \texttt{p = 0}, ..., {@link #getNumPeriods}\texttt{/x -
    * 1}, it sums the values for periods \texttt{xp}, \ldots,\texttt{xp+x-1},
    * and stores the results in period \texttt{p} whose length will be
    * \texttt{x} times the length of original periods. If the number of periods
    * is not a multiple of \texttt{x}, an additional period is used to contain
    * the remaining sums of values. The unused periods are zeroed for future
    * use. This method can be useful for memory management when using batch
    * means to estimate steady-state performance measures.
    * 
    * @param x
    *           the number of periods per group.
    * @exception IllegalArgumentException
    *               if the number of periods per group is negative or 0.
    * @exception UnsupportedOperationException
    *               if the matrix does not support regrouping.
    */
   public void regroupPeriods (int x);
}
