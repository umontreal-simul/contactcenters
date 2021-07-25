package umontreal.ssj.stat.mperiods;

/**
 * This extends {@link IntegralMeasureMatrix} to add a sliding window for the
 * records. With the base class, the total number of records is limited to the
 * number of periods in the measure matrix. With this class, the number of added
 * records can be greater than the number of periods. If a record is added while
 * all allocated periods are used, the first record is lost and the new one is
 * added. Therefore, the integral can be obtained for the last periods only.
 */
public class IntegralMeasureMatrixSW<M extends MeasureMatrix> extends
      IntegralMeasureMatrix<M> {
   @Override
   protected SumMatrixSW createSumMatrix (int nm, int np) {
      return new SumMatrixSW (nm, np);
   }

   /**
    * Calls {@link IntegralMeasureMatrix#IntegralMeasureMatrix super}
    * \texttt{(mat, numPeriods)}.
    */
   public IntegralMeasureMatrixSW (M mat, int numPeriods) {
      super (mat, numPeriods);
   }

   @Override
   public SumMatrixSW getSumMatrix () {
      return (SumMatrixSW) super.getSumMatrix ();
   }

   /**
    * Returns the first value $p$ for which a recorded value $\boldf(t_p)$ is
    * available. If no recorded value is lost, this returns 0.
    * 
    * @return the first value of $p$ for which $\boldf(t_p)$ is available.
    */
   public int getFirstRealRecord () {
      return getSumMatrix ().getFirstRealPeriod ();
   }

   /**
    * Sets the index of the first real record to \texttt{firstRealRecord}.
    * 
    * @param firstRealRecord
    *           the index of the first real record.
    * @exception IllegalArgumentException
    *               if \texttt{firstRealRecord} is negative.
    */
   public void setFirstRealRecord (int firstRealRecord) {
      getSumMatrix ().setFirstRealPeriod (firstRealRecord);
   }

   /**
    * Returns the total number of times the {@link #newRecord} method was called
    * since the last call to {@link #init} plus one. If the returned value
    * exceeds the number of stored records ({@link #getNumStoredRecords}),
    * only the values of $\boldf(t)$ for the last {@link #getNumStoredRecords}
    * are accessible; the first values are then lost.
    * 
    * @return the total number of records.
    */
   public int getNumRealRecords () {
      return getSumMatrix ().getNumRealPeriods ();
   }

   /**
    * This is the same as in the superclass, but if the number of stored records
    * exceeds the number of real records, the first stored record is discarded.
    */
   @Override
   public void newRecord () {
      super.newRecord ();
   }

   /**
    * Returns \texttt{mpc.}{@link SumMatrixSW#getNumRealPeriods get\-Num\-Real\-Periods()}.
    */
   @Override
   protected int getPeriod () {
      return getSumMatrix ().getNumRealPeriods ();
   }

   /**
    * Returns $f_i(t_j)$, the measure \texttt{i} of the associated measure
    * matrix at the simulation time $t_j$, $j$ being \texttt{r + }{@link #getFirstRealRecord}.
    * 
    * @param i
    *           the measure index.
    * @param r
    *           the record index.
    * @return the value of the measure.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} or \texttt{r} are out of bounds.
    */
   @Override
   public double getSum (int i, int r) {
      return super.getSum (i, r);
   }

   /**
    * Returns the measure \texttt{i} for period \texttt{p}. This corresponds to
    * $f_i(t_{s+p+1}) - f_i(t_{s+p})$ where $t_{s+p}$ is the simulation time of
    * the stored record $p$, and $s$ is the result of
    * {@link #getFirstRealRecord}.
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
   @Override
   public double getMeasure (int i, int p) {
      return super.getMeasure (i, p);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (super.toString ());
      sb.deleteCharAt (sb.length () - 1);
      sb.append (getNumRealRecords ()).append (" real records");
      sb.append (']');
      return sb.toString ();
   }
}
