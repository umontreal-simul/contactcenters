package umontreal.ssj.simevents;

import umontreal.ssj.stat.mperiods.MeasureMatrix;

/**
 * This matrix of measures contains a single measure corresponding to the
 * current simulation time.
 */
public class SimTimeMeasureMatrix implements MeasureMatrix {
   public void init () {}

   public int getNumMeasures () {
      return 1;
   }

   public int getNumPeriods () {
      return 1;
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ();
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ();
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   public double getMeasure (int i, int p) {
      if (i != 0 || p != 0)
         throw new IndexOutOfBoundsException (
               "Invalid measure or period indices");
      return Sim.time ();
   }
}
