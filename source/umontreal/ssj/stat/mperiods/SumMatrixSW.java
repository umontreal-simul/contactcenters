package umontreal.ssj.stat.mperiods;

import cern.colt.function.DoubleDoubleFunction;

/**
 * Extends {@link SumMatrix} to add a sliding window. By using a circular buffer
 * to store the values, it can compute observations for all the periods or for a
 * subset of the periods.
 * 
 * When values are added to this matrix of sums using the {@link #add} method,
 * the index of a \emph{real period} needs to be specified. When obtaining a
 * value from this object, the index of a \emph{stored period} (or simply a
 * period) is needed. If the number of considered periods is smaller than or
 * equal to the number of stored periods, these two index spaces match, no
 * information is lost, and this class behaves exactly as {@link SumMatrix}.
 * This is the most common case.
 * 
 * However, if the number of real periods is greater than the number of stored
 * periods, some values are lost. Only the values from the last periods are
 * accessible at any time. For example, if a matrix of sums is defined to store
 * 10 periods, values for the periods~0 to~9 are available until the
 * {@link #add} method is required to add a value in the period~10 or greater.
 * After a value is added into the real period~10, values for the period~0 are
 * lost. The stored periods are shifted and stored periods~0 to~9 then
 * correspond to real periods~1 to~10. This facility allows, for example, to
 * compute a statistic for the last ten minutes, at any times during a
 * simulation.
 */
public class SumMatrixSW extends SumMatrix {
   private int firstRealPeriod = 0;

   /**
    * This is the period corresponding to stored period 0. Instead of moving
    * values in the {@link #count} array, the class simply modifies this value.
    */
   private int periodShift = 0;

   /**
    * Constructs a new matrix of sums with sliding window for \texttt{numTypes}
    * event types and a single period.
    * 
    * @param numTypes
    *           the number of event types.
    * @exception IllegalArgumentException
    *               if the number of types is negative.
    */
   public SumMatrixSW (int numTypes) {
      super (numTypes);
   }

   /**
    * Constructs a new matrix of sums with sliding window for \texttt{numTypes}
    * event types and \texttt{numPeriods} periods.
    * 
    * @param numTypes
    *           the number of event types.
    * @param numPeriods
    *           the number of stored periods.
    * @exception IllegalArgumentException
    *               if the number of types or periods is negative.
    */
   public SumMatrixSW (int numTypes, int numPeriods) {
      super (numTypes, numPeriods);
   }

   /**
    * Returns the real period corresponding to stored period having index~0 when
    * using the {@link #getMeasure} method. If no period is currently lost, this
    * returns 0.
    * 
    * @return the first real period.
    */
   public int getFirstRealPeriod () {
      return firstRealPeriod;
   }

   /**
    * Sets the index of the first real period to \texttt{firstRealPeriod}.
    * 
    * @param firstRealPeriod
    *           the new index of the first real period.
    * @exception IllegalArgumentException
    *               if \texttt{firstRealPeriod} is negative.
    */
   public void setFirstRealPeriod (int firstRealPeriod) {
      if (firstRealPeriod < 0)
         throw new IllegalArgumentException (
               "firstRealPeriod must not be negative");
      this.firstRealPeriod = firstRealPeriod;
   }

   /**
    * Returns the number of real periods used by this matrix of sums. This
    * corresponds to one plus the maximal value of \texttt{realPeriod} given to
    * {@link #add} or {@link #set} since the last call to {@link #init}. In
    * contrast with the similar method {@link #getNumStoredPeriods}, the
    * returned value can be greater than {@link #getNumPeriods}.
    * 
    * @return the number of used real periods.
    */
   public int getNumRealPeriods () {
      return firstRealPeriod + getNumStoredPeriods ();
   }

   /**
    * Adds a new observation \texttt{x} of type \texttt{type} in the real period
    * \texttt{realPeriod}. If the given real period is inside the interval
    * \texttt{[}{@link #getFirstRealPeriod}, \ldots,
    * {@link #getFirstRealPeriod}\texttt{ + }{@link #getNumPeriods}\texttt{ -
    * 1]}, which we will call the current interval, a new value is added; the
    * corresponding measure is increased by \texttt{x}. If the period is at the
    * left of the current interval, an exception is thrown. Otherwise, the
    * window of observations slides and {@link #getFirstRealPeriod} is
    * increased; some computed values are then lost.
    * 
    * @param type
    *           the type of the new value.
    * @param realPeriod
    *           the real period of the new value.
    * @param x
    *           the value being added.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{type} or \texttt{realPeriod} are negative, if
    *               \texttt{type} is greater than or equal to the number of
    *               supported types, or if \texttt{realPeriod} is smaller than
    *               {@link #getFirstRealPeriod}.
    */
   @Override
   public void add (int type, int realPeriod, double x) {
      count[getIndex (type, realPeriod)] += x;
   }

   @Override
   public void add (int type, int realPeriod, double x, DoubleDoubleFunction fn) {
      final int idx = getIndex (type, realPeriod); 
      count[idx] = fn.apply (count[idx], x);
   }
   
   /**
    * Sets the sum for event \texttt{type} in real period \texttt{realPeriod}
    * for this matrix to \texttt{x}. This is the same as the {@link #add} method
    * except the measure is replaced by \texttt{x} instead of being incremented.
    * 
    * @param type
    *           the type of the event.
    * @param realPeriod
    *           the real period of the event.
    * @param x
    *           the new value.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{type} or \texttt{realPeriod} are negative, if
    *               \texttt{type} is greater than or equal to the number of
    *               supported types, or if \texttt{realPeriod} is smaller than
    *               {@link #getFirstRealPeriod}.
    */
   @Override
   public void set (int type, int realPeriod, double x) {
      count[getIndex (type, realPeriod)] = x;
   }

   @Override
   public void init () {
      super.init ();
      firstRealPeriod = 0;
      periodShift = 0;
   }

   @Override
   public void setNumPeriods (int np) {
      if (np == numPeriods)
         return;
      super.setNumPeriods (np);
      // super.setNumPeriods uses getPeriod when resizing count.
      // This way, elements of count are moved, so the period shift can
      // be reset to 0.
      periodShift = 0;
   }

   private final int getIndex (int type, int realPeriod) {
      if (type < 0 || type >= numTypes || realPeriod < firstRealPeriod)
         throw new ArrayIndexOutOfBoundsException ("type = " + type
               + ", period = " + realPeriod);
      if (realPeriod >= numStoredPeriods)
         numStoredPeriods = Math.min (realPeriod + 1, numPeriods);
      final int endRealPeriod = firstRealPeriod + numPeriods - 1;
      if (realPeriod > endRealPeriod)
         shiftWindowRight (realPeriod - endRealPeriod);
      final int per = (realPeriod - firstRealPeriod + periodShift) % numPeriods; 
      return numTypes
            * per
            + type;
   }

   private final void shiftWindowRight (int np) {
      if (np >= numPeriods) {
         // If we right-shift numPeriods positions or more,
         // all measures are lost; simply reset everything.
         // The start period must be saved.
         final int sp = firstRealPeriod + np;
         init ();
         firstRealPeriod = sp;
         return;
      }

      // Instead of performing linear-time copying,
      // we simply modify the period shift.
      // The circular list of measures is hidden by the
      // MeasureMatrix interface.
      // Some measures may have to be zeroed.
      for (int p = 0; p < np; p++) {
         final int rp = (p + periodShift) % numPeriods;
         for (int i = 0; i < numTypes; i++)
            count[rp * numTypes + i] = 0;
      }

      firstRealPeriod += np;
      periodShift = (periodShift + np) % numPeriods;
   }

   @Override
   protected void regroupPeriods (int x, boolean onlyFirst) {
      super.regroupPeriods (x, onlyFirst);
      firstRealPeriod /= x;
   }

   @Override
   protected int getPeriod (int p) {
      return (p + periodShift) % numPeriods;
   }
}
