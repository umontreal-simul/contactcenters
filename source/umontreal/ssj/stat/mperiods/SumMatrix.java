package umontreal.ssj.stat.mperiods;

import cern.colt.function.DoubleDoubleFunction;

/**
 * This matrix of measures can be used to compute sums of values, or the number
 * of occurrences of events. It supports several types of observations on
 * several simulation periods. This class supports every optional operation
 * specified by the {@link MeasureMatrix} interface.
 */
public class SumMatrix implements MeasureMatrix, Cloneable {
   /**
    * Number of types of events.
    */
   protected int numTypes;

   /**
    * Number of periods.
    */
   protected int numPeriods;

   /**
    * Number of stored periods.
    */
   protected int numStoredPeriods = 0;

   /**
    * Array containing the sums, in row major order. If \texttt{i} is an event
    * type and \texttt{p} is a stored period, the value at \texttt{(i, p)} is
    * given by \texttt{count[i + numTypes*p]}.
    */
   protected double[] count;

   /**
    * Constructs a new matrix of sums for \texttt{numTypes} event types and a
    * single period.
    * 
    * @param numTypes
    *           the number of event types.
    * @exception IllegalArgumentException
    *               if the number of types is negative.
    */
   public SumMatrix (int numTypes) {
      if (numTypes < 0)
         throw new IllegalArgumentException (
               "The number of types must be positive");
      this.numTypes = numTypes;
      numPeriods = 1;
      count = new double[numTypes];
   }

   /**
    * Constructs a new matrix of sums for \texttt{numTypes} event types and
    * \texttt{numPeriods} periods.
    * 
    * @param numTypes
    *           the number of event types.
    * @param numPeriods
    *           the number of stored periods.
    * @exception IllegalArgumentException
    *               if the number of types or periods is negative.
    */
   public SumMatrix (int numTypes, int numPeriods) {
      if (numTypes < 0)
         throw new IllegalArgumentException (
               "The number of types must be positive");
      if (numPeriods < 0)
         throw new IllegalArgumentException (
               "The number of periods must be positive");
      this.numTypes = numTypes;
      this.numPeriods = numPeriods;
      count = new double[numTypes * numPeriods];
   }

   /**
    * Returns the total number of periods stored in this matrix of sums. This
    * corresponds to $p+1$ if $p$ is the maximal period index given to
    * {@link #add} or {@link #set} since the last call to {@link #init}. If
    * {@link #add} or {@link #set} were not called since the last
    * initialization, this returns 0. The returned value cannot be larger than
    * the number of stored periods ({@link #getNumPeriods}).
    * 
    * @return the number of used periods.
    */
   public int getNumStoredPeriods () {
      return numStoredPeriods;
   }

   /**
    * Adds a new observation \texttt{x} of type \texttt{type} in the period
    * \texttt{period}.
    * 
    * @param type
    *           the type of the new value.
    * @param period
    *           the period of the new value.
    * @param x
    *           the value being added.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{type} or \texttt{period} are negative, if
    *               \texttt{type} is greater than or equal to the number of
    *               supported types, or if \texttt{period} is greater than or
    *               equal to the number of supported periods.
    */
   public void add (int type, int period, double x) {
      if (type < 0 || type >= numTypes)
         throw new ArrayIndexOutOfBoundsException ("Invalid type index: "
               + type);
      if (period < 0 || period >= numPeriods)
         throw new ArrayIndexOutOfBoundsException ("Invalid period index: "
               + period);
      if (period >= numStoredPeriods)
         numStoredPeriods = period + 1;
      count[numTypes * period + type] += x;
   }

   /**
    * Similar to {@link #add(int, int, double)}, but applies
    * a function \texttt{fn} instead of just adding.
    * More specifically, if $c$ is the original value in the matrix, and
    * $x$ is the new value, this method adds the value $f(c, x)$ at the given
    * position in the matrix.
    */
   public void add (int type, int period, double x, DoubleDoubleFunction fn) {
      if (type < 0 || type >= numTypes)
         throw new ArrayIndexOutOfBoundsException ("Invalid type index: "
               + type);
      if (period < 0 || period >= numPeriods)
         throw new ArrayIndexOutOfBoundsException ("Invalid period index: "
               + period);
      if (period >= numStoredPeriods)
         numStoredPeriods = period + 1;
      final int idx = numTypes * period + type; 
      count[idx] = fn.apply (count[idx], x);
   }
   
   /**
    * Sets the sum for event \texttt{type} in period \texttt{period} for this
    * matrix to \texttt{x}. This is the same as the {@link #add} method except
    * the measure is replaced by \texttt{x} instead of being incremented.
    * 
    * @param type
    *           the type of the event.
    * @param period
    *           the period of the event.
    * @param x
    *           the new value.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{type} or \texttt{period} are negative, if
    *               \texttt{type} is greater than or equal to the number of
    *               supported types, or if \texttt{period} is greater than or
    *               equal to the number of supported periods.
    */
   public void set (int type, int period, double x) {
      if (period < 0 || type < 0 || period >= numPeriods || type >= numTypes)
         throw new ArrayIndexOutOfBoundsException (
               "Invalid type or period index");
      if (period >= numStoredPeriods)
         numStoredPeriods = period + 1;
      count[numTypes * period + type] = x;
   }

   public void init () {
      numStoredPeriods = 0;
      for (int i = 0; i < count.length; i++)
         count[i] = 0;
   }

   public int getNumMeasures () {
      return numTypes;
   }

   /**
    * Sets the number of measures to \texttt{nm}. If \texttt{nm} is greater than
    * {@link #getNumMeasures}, new measures are added and initialized to 0. If
    * \texttt{nm} is smaller than {@link #getNumMeasures}, the last
    * {@link #getNumMeasures}\texttt{ - nm} measures are removed. Otherwise,
    * nothing happens.
    * 
    * @param nm
    *           the new number of measures.
    * @exception IllegalArgumentException
    *               if the given number is negative.
    */
   public void setNumMeasures (int nm) {
      if (nm < 0)
         throw new IllegalArgumentException (
               "The number of measures must not be negative");
      if (nm == numTypes)
         return;
      final double[] ncount = new double[nm * numPeriods];
      final int ncp = Math.min (numTypes, nm);
      for (int p = 0; p < numPeriods; p++)
         System.arraycopy (count, p * numTypes, ncount, p * nm, ncp);
      count = ncount;
      numTypes = nm;
   }

   public int getNumPeriods () {
      return numPeriods;
   }

   /**
    * Sets the number of periods to \texttt{np}. As with {@link #setNumMeasures},
    * added periods are initialized to 0 and the last periods are removed if
    * necessary.
    * 
    * @param np
    *           the new number of periods.
    * @exception IllegalArgumentException
    *               if the given number is negative.
    */
   public void setNumPeriods (int np) {
      if (np < 0)
         throw new IllegalArgumentException (
               "The number of periods must not be negative");
      if (np == numPeriods)
         return;
      final double[] ncount = new double[numTypes * np];
      final int ncp = Math.min (np, numStoredPeriods);
      for (int p = 0; p < ncp; p++) {
         final int rp = getPeriod (p);
         System
               .arraycopy (count, rp * numTypes, ncount, p * numTypes, numTypes);
      }
      count = ncount;
      numPeriods = np;
      numStoredPeriods = ncp;
   }

   public double getMeasure (int i, int p) {
      if (i < 0 || i >= numTypes || p < 0 || p >= numPeriods)
         throw new ArrayIndexOutOfBoundsException ("i = " + i + ", p = " + p);
      return count[numTypes * getPeriod (p) + i];
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append (numTypes).append (" measure types, ");
      sb.append (numPeriods).append (" periods, ");
      sb.append (numStoredPeriods).append (" stored periods, ");
      sb.append (']');
      return sb.toString ();
   }

   public void regroupPeriods (int x) {
      regroupPeriods (x, false);
   }

   /**
    * Similar to {@link #regroupPeriods(int)}, but if \texttt{onlyFirst} is
    * \texttt{false}, do not sum the values in each group.
    * \texttt{regroupPeriods} with \texttt{onlyFirst = false} is internally used
    * by {@link IntegralMeasureMatrix}.
    */
   protected void regroupPeriods (int x, boolean onlyFirst) {
      // If onlyFirst is true, only the first period in each
      // group are used, no sum is performed.
      // This method is called with onlyFirst = true by
      // IntegralMeasureMatrix.
      if (x <= 0)
         throw new IllegalArgumentException ("x <= 0");
      int newNumStoredPeriods = numStoredPeriods / x;
      if (numStoredPeriods % x != 0)
         // A period not regrouping x periods is added.
         newNumStoredPeriods++;
      for (int p = 0; p < newNumStoredPeriods; p++) {
         // p: group index
         // si: period index in the regrouped space
         // si2: period index in the original space
         final int si = getPeriod (p);
         final int si2 = getPeriod (p * x);
         // Copy the values only if the period indices are different.
         // They are the same for p=0.
         if (si != si2)
            System.arraycopy (count, si * numTypes, count, si2 * numTypes,
                  numTypes);
         // If !onlyFirst, sum the last x-1 periods.
         for (int i = 1; i < x && !onlyFirst; i++) {
            // sx: period number of the (i+1)th element of group p.
            // six: period index of the element in the regrouped space
            final int sx = x * p + i;
            if (sx > numStoredPeriods)
               // Do not copy zeros.
               break;
            final int six = getPeriod (sx);
            // Sum for each event type
            for (int t = 0; t < numTypes; t++)
               count[si * numTypes + t] += count[six * numTypes + t];
         }
      }
      // Fill the rest of the buffer with zeros
      for (int p = newNumStoredPeriods; p < numStoredPeriods; p++) {
         final int si = getPeriod (p);
         for (int t = 0; t < numTypes; t++)
            count[si * numTypes + t] = 0;
      }
      numStoredPeriods = newNumStoredPeriods;
   }

   @Override
   public SumMatrix clone () {
      try {
         final SumMatrix sc = (SumMatrix) super.clone ();
         sc.count = count.clone ();
         return sc;
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }

   /**
    * Returns the period index corresponding to period \texttt{p}. This returns
    * \texttt{p} by default, but it is overridden by {@link SumMatrixSW} to take
    * the sliding window into account.
    * 
    * @param p
    *           the source period index.
    * @return the target period index.
    */
   protected int getPeriod (int p) {
      return p;
   }
}
