package umontreal.ssj.stat.mperiods;

/**
 * Computes per-period values for a matrix of measures with a single period.
 * Some matrices of measures only support a single period. For example, when
 * using an {@link umontreal.ssj.simevents.Accumulate} object to compute
 * an integral over simulation time, per-period measures cannot be computed
 * directly. This class can be used to transform a matrix of measures with a
 * single period computing integrals into a multiple-periods matrix.
 *
 * Let $f_i(t)$ be an integral (or a sum) for measure~$i$, computed by the
 * underlying single-period matrix over the simulation time, from~0 to~$t$.
 * Often, $f_i(t)$ is a discrete function such as a sum or the integral of a
 * piecewise-constant function, but the function can also be continuous. If
 * {@link #newRecord} is called at simulation time~$t_p$, the value of
 * $\boldf(t_p)=(f_0(t_p),f_1(t_p),\ldots)$ is computed and recorded. At
 * time~$t_0$ where {@link #init} is called, a record is automatically added
 * with the values in the matrices of measures. At the end of the simulation, if
 * {@link #newRecord} was called $P$ times, we have $P+1$ recorded values of
 * $\boldf(t_p)$, for $p=0,\ldots,P$. This permits the computation of $P$
 * vectors of integrals, each corresponding to a period. The integrals for
 * period \texttt{p}, i.e., during the interval $[t_p, t_{p+1})$, are computed
 * by $\boldf(t_{p+1}) - \boldf(t_p)$.
 */
public class IntegralMeasureMatrix<M extends MeasureMatrix> implements
      MeasureMatrix, Cloneable {
   /**
    * Internal matrix of sums used to hold records.
    */
   private SumMatrix mpc;
   private M mat;

   /**
    * This methods creates and returns the internal sum matrix, and is
    * overridden in {@link IntegralMeasureMatrixSW} to create an instance of
    * {@link SumMatrixSW} instead.
    *
    * @param nm
    *           the number of measures.
    * @param np
    *           the number of periods.
    */
   protected SumMatrix createSumMatrix (int nm, int np) {
      return new SumMatrix (nm, np);
   }

   /**
    * Constructs a new matrix of measures for computing integrals on multiple
    * periods. The wrapped matrix of measures is given by \texttt{mat}, and the
    * integral is computed for \texttt{numPeriods}. The object will be able to
    * record \texttt{numPeriods+1} values of $\boldf(t)$. The number of measures
    * or periods of \texttt{mat} should not be changed after it is associated
    * with this object.
    *
    * @param mat
    *           the single-period matrix of measures.
    * @param numPeriods
    *           the required number of periods.
    * @exception NullPointerException
    *               if \texttt{mat} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if a multiple-periods matrix of measures is given, or if
    *               \texttt{numPeriods} is smaller than 1.
    */
   public IntegralMeasureMatrix (M mat, int numPeriods) {
      if (mat.getNumPeriods () > 1)
         throw new IllegalArgumentException (
               "The given matrix of measures must not have multiple periods");
      if (numPeriods < 1)
         throw new IllegalArgumentException ("At least one period is needed");
      this.mat = mat;
      mpc = createSumMatrix (mat.getNumMeasures (), numPeriods + 1);
      for (int i = 0; i < mpc.getNumMeasures (); i++)
         mpc.add (i, 0, mat.getMeasure (i, 0));
   }

   /**
    * Returns the associated single-period matrix of measures.
    *
    * @return the associated single-period matrix of measures.
    */
   public M getMeasureMatrix () {
      return mat;
   }

   /**
    * Sets the associated matrix of measures to \texttt{mat}. This should only
    * be called before or after {@link #init}.
    *
    * @param mat
    *           the new matrix of measures.
    * @exception NullPointerException
    *               if \texttt{mat} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given matrix has multiple periods.
    */
   public void setMeasureMatrix (M mat) {
      if (mat.getNumPeriods () > 1)
         throw new IllegalArgumentException (
               "The given matrix of measures must not have multiple periods");
      if (mat.getNumMeasures () != mpc.getNumMeasures ())
         mpc.setNumMeasures (mat.getNumMeasures ());
      this.mat = mat;
   }

   /**
    * Returns the internal sum matrix for which each period $p$ contains the
    * value of $\boldf(t_p)$. The number of measures of this matrix is
    * {@link #getNumMeasures} while the number of periods is one more than
    * {@link #getNumPeriods}.
    *
    * @return the internal matrix of sums.
    */
   public SumMatrix getSumMatrix () {
      return mpc;
   }

   /**
    * Returns the current number of records of $\boldf(t)$ available for this
    * matrix of measures.
    *
    * @return the current number of records.
    */
   public int getNumStoredRecords () {
      return mpc.getNumStoredPeriods ();
   }

   /**
    * Records the current values of $\boldf(t)$. This increases the number of
    * stored records, and an {@link IllegalStateException} is thrown if no
    * additional record can be stored.
    */
   public void newRecord () {
      check ();
      final int nm = mat.getNumMeasures ();
      final int period = getPeriod ();
      for (int i = 0; i < nm; i++)
         mpc.add (i, period, mat.getMeasure (i, 0));
   }

   /**
    * Returns the period, in {@link #mpc}, the new record needs to be added in.
    * This returns \texttt{mpc.}{@link SumMatrix#getNumStoredPeriods
    * get\-Num\-Stored\-Periods()}.
    *
    * @return the period used by {@link #newRecord}.
    */
   protected int getPeriod () {
      final int p = mpc.getNumStoredPeriods ();
      if (p == mpc.getNumPeriods ())
         throw new IllegalStateException ("Cannot add new records");
      return p;
   }

   public void init () {
      mat.init ();
      mpc.init ();
      if (mat.getNumMeasures () != mpc.getNumMeasures ())
         mpc.setNumMeasures (mat.getNumMeasures ());
      for (int i = 0; i < mpc.getNumMeasures (); i++)
         mpc.add (i, 0, mat.getMeasure (i, 0));
   }

   public int getNumMeasures () {
      check ();
      return mat.getNumMeasures ();
   }

   public void setNumMeasures (int nm) {
      mat.setNumMeasures (nm);
      check ();
   }

   public int getNumPeriods () {
      return mpc.getNumPeriods () - 1;
   }

   public void setNumPeriods (int numPeriods) {
      if (numPeriods <= 0)
         throw new IllegalArgumentException ("At least one period is needed");
      mpc.setNumPeriods (numPeriods + 1);
   }

   /**
    * Returns $f_i(t_r)$, the measure \texttt{i} of the associated measure
    * matrix at the simulation time $t_r$.
    *
    * @param i
    *           the measure index.
    * @param r
    *           the record index.
    * @return the value of the measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} or \texttt{r} are out of bounds.
    */
   public double getSum (int i, int r) {
      check ();
      return mpc.getMeasure (i, r);
   }

   /**
    * Returns the measure \texttt{i} for period \texttt{p}. This corresponds to
    * $f_i(t_{p+1}) - f_i(t_p)$ where $t_p$ is the simulation time of the stored
    * record $p$.
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
   public double getMeasure (int i, int p) {
      check ();
      return mpc.getMeasure (i, p + 1) - mpc.getMeasure (i, p);
   }

   public void regroupPeriods (int x) {
      check ();
      mpc.regroupPeriods (x, true);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append ("converted matrix of measures: ");
      sb.append (mat.toString ()).append (", ");
      sb.append (getNumStoredRecords ()).append (" stored records, ");
      sb.append (']');
      return sb.toString ();
   }

   private final void check () {
      if (mat.getNumMeasures () != mpc.getNumMeasures ())
         mpc.setNumMeasures (mat.getNumMeasures ());
      if (mat.getNumPeriods () != 1)
         throw new IllegalStateException (
               "The number of periods of the wrapped matrix of measures has been changed");
   }

   @Override
   @SuppressWarnings ("unchecked")
   public IntegralMeasureMatrix<M> clone () {
      try {
         final IntegralMeasureMatrix<M> m = (IntegralMeasureMatrix<M>) super
               .clone ();
         m.mpc = mpc.clone ();
         return m;
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }
}
