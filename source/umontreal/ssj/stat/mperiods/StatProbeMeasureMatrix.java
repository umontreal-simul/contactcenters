package umontreal.ssj.stat.mperiods;

import umontreal.ssj.stat.StatProbe;

/**
 * Matrix of measures whose value is obtained using a statistical probe. This
 * matrix only contains one measure and one period, and its value is obtained by
 * using {@link StatProbe#sum}. Since the sum can be considered as an integral,
 * the {@link IntegralMeasureMatrix} class can be used to turn this
 * single-period matrix into a multiple-periods one.
 */
public class StatProbeMeasureMatrix implements MeasureMatrix, Cloneable {
   private StatProbe probe;

   /**
    * Constructs a new matrix of measures using the statistical probe
    * \texttt{probe}.
    * 
    * @param probe
    *           the statistical probe being used.
    */
   public StatProbeMeasureMatrix (StatProbe probe) {
      this.probe = probe;
   }

   /**
    * Returns the statistical probe associated with this matrix.
    * 
    * @return the associated statistical probe.
    */
   public StatProbe getStatProbe () {
      return probe;
   }

   /**
    * Sets the associated statistical probe to \texttt{probe}. If \texttt{null}
    * is given, this changes the number of measures and periods to 0. If a
    * non-\texttt{null} probe is given, the number of measures in this object is
    * 1.
    * 
    * @param probe
    *           the new statistical probe.
    */
   public void setStatProbe (StatProbe probe) {
      this.probe = probe;
   }

   public void init () {}

   public int getNumMeasures () {
      return probe == null ? 0 : 1;
   }

   public int getNumPeriods () {
      return probe == null ? 0 : 1;
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    * 
    * @exception UnsupportedOperation
    *               if this method is called.
    */
   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ();
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    * 
    * @exception UnsupportedOperation
    *               if this method is called.
    */
   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ();
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    * 
    * @exception UnsupportedOperation
    *               if this method is called.
    */
   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   public double getMeasure (int i, int p) {
      if (probe == null || i != 0 || p != 0)
         throw new IndexOutOfBoundsException (
               "Invalid measure or period indices");
      return probe.sum ();
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      if (probe == null)
         sb.append ("no associated probe");
      else
         sb.append ("associated probe: " + probe.toString ());
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Makes a copy of this matrix of measures. The statistical probe is not
    * cloned.
    * 
    * @return a clone of this instance.
    */
   @Override
   public StatProbeMeasureMatrix clone () {
      try {
         return (StatProbeMeasureMatrix) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }
}
