package umontreal.ssj.stat.mperiods;

import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 * Matrix of measures whose values are obtained using an list of statistical
 * probes. This matrix contains one measure for each element of the list, and a
 * single period. The measures are obtained by using the {@link StatProbe#sum}
 * method. Since the sum can be considered as an integral, the
 * {@link IntegralMeasureMatrix} can be used to turn this single-period matrix
 * into a multiple-periods one if needed.
 */
public class ListOfStatProbesMeasureMatrix implements MeasureMatrix, Cloneable {
   private ListOfStatProbes<? extends StatProbe> list;

   /**
    * Constructs a new matrix of measures using the list of probes
    * \texttt{list}.
    *
    * @param list
    *           the list of statistical probes being used.
    */
   public ListOfStatProbesMeasureMatrix (
         ListOfStatProbes<? extends StatProbe> list) {
      this.list = list;
   }

   /**
    * Returns the list of statistical probes associated with this matrix.
    *
    * @return the associated list of statistical probes.
    */
   public ListOfStatProbes<? extends StatProbe> getListOfStatProbes () {
      return list;
   }

   /**
    * Sets the associated list of statistical probes to \texttt{list}. If the
    * given list is \texttt{null}, the number of measures and periods is set to
    * 0. Otherwise, the number of measures corresponds to the length of the
    * list, and the number of periods is 1.
    *
    * @param list
    *           the new list of statistical probes.
    */
   public void setListOfStatProbes (ListOfStatProbes<? extends StatProbe> list) {
      this.list = list;
   }

   public void init () {}

   public int getNumMeasures () {
      return list == null ? 0 : list.size ();
   }

   public int getNumPeriods () {
      return list == null ? 0 : 1;
   }

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
      if (list == null || p != 0)
         throw new IndexOutOfBoundsException (
               "Invalid measure or period indices");
      return list.get (i).sum ();
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      if (list == null)
         sb.append ("no associated list of probes");
      else
         sb.append ("associated list of probes: " + list.toString ());
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Makes a copy of this matrix of measures. The list of statistical probes is
    * not cloned.
    *
    * @return a clone of this instance.
    */
   @Override
   public ListOfStatProbesMeasureMatrix clone () {
      try {
         return (ListOfStatProbesMeasureMatrix) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }
}
