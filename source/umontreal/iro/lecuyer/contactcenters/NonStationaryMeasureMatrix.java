package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.stat.mperiods.IntegralMeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureMatrix;

/**
 * Computes per-period values for a one-period measure matrix. This class
 * extends the {@link IntegralMeasureMatrix} and maps a period with a contact
 * center period. It automatically calls {@link #newRecord} upon period changes.
 */
public class NonStationaryMeasureMatrix<M extends MeasureMatrix> extends
      IntegralMeasureMatrix<M> implements PeriodChangeListener {
   private PeriodChangeEvent pce;

   /**
    * Constructs a new non-stationary measure matrix from the one-period measure
    * matrix \texttt{mat} and using the period change event \texttt{pce} to
    * define the periods.
    *
    * @param pce
    *           the period change event.
    * @param mat
    *           the one-period only measure matrix.
    * @exception IllegalArgumentException
    *               if a multiple-periods measure matrix is given.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public NonStationaryMeasureMatrix (PeriodChangeEvent pce, M mat) {
      super (mat, pce.getNumPeriods ());
      this.pce = pce;
      pce.addPeriodChangeListener (this);
   }

   @Override
   public void setNumPeriods (int np) {
      if (np > pce.getNumPeriods ())
         throw new IllegalArgumentException ("Too many periods: " + np);
      super.setNumPeriods (np);
   }

   public void changePeriod (PeriodChangeEvent pce1) {
      if (pce1 != this.pce)
         return;
      if (pce1.isLockedPeriod ())
         return;
      newRecord ();
   }

   public void stop (PeriodChangeEvent pce1) {
      if (pce1 != this.pce)
         return;
      if (pce1.isLockedPeriod ())
         return;
      newRecord ();
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (super.toString ());
      sb.deleteCharAt (sb.length () - 1).append (", ");
      sb.append ("period change event: " + pce.toString ());
      sb.append (']');
      return sb.toString ();
   }
}
