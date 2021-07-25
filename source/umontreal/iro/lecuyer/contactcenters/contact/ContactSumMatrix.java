package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.stat.mperiods.SumMatrix;

/**
 * This sum matrix can be used to compute contact-related
 * observations. It defines one measure type for each contact type
 * as well as one aggregate measure.  When the supported
 * number of contact types is 1, it computes the aggregate sum only.
 * When the supported number of contact types is greater than 1,
 * it computes a sum specific for each type as well as the aggregate sum.
 * If one does not require the sum row, one can use a {@link SumMatrix}
 * instead.
 */
public class ContactSumMatrix extends SumMatrix {
   private PeriodChangeEvent pce = null;

   /**
    * Constructs a new contact sum matrix for
    * \texttt{numTypes} contact types and one period.
    @param numTypes the number of contact types.
    @exception IllegalArgumentException if the number of contact types
    is negative or 0.
    */
   public ContactSumMatrix (int numTypes) {
      super (numTypes <= 1 ? numTypes : numTypes + 1);
   }

   /**
    * Constructs a new contact sum matrix with period change
    * event \texttt{pce} and for
    * \texttt{numTypes} contact types.
    * The number of periods is determined by using
    * {@link PeriodChangeEvent#getNumPeriods}.
    @param pce the period change event.
    @param numTypes the number of contact types.
    @exception IllegalArgumentException if the number of contact types
    is negative or 0.
    @exception NullPointerException if \texttt{pce} is \texttt{null}.
    */
   public ContactSumMatrix (PeriodChangeEvent pce, int numTypes) {
      super (numTypes <= 1 ? numTypes : numTypes + 1, pce.getNumPeriods());
      this.pce = pce;
   }

   /**
    * Constructs a new contact sum matrix for
    * \texttt{numTypes} contact types and \texttt{numPeriods} periods.
    @param numTypes the number of contact types.
    @param numPeriods the number of periods.
    @exception IllegalArgumentException if the number of contact types
    or periods is negative or 0.
    */
   public ContactSumMatrix (int numTypes, int numPeriods) {
      super (numTypes <= 1 ? numTypes : numTypes + 1, numPeriods);
   }

   @Override
   public void setNumMeasures (int nm) {
      final int oldNm = getNumMeasures();
      if (nm == oldNm)
         return;
      if (nm == 2)
         throw new IllegalArgumentException
            ("Cannot have two measures (one contact type and an aggregate sum)");
      // Make a backup copy of the last row.
      final double[] copy = new double[getNumStoredPeriods()];
      for (int p = 0; p < copy.length; p++)
         copy[p] = getMeasure (oldNm - 1, p);
      super.setNumMeasures (nm);
      if (nm > oldNm)
         // If we increase the number of measures, the old last row
         // becomes a contact-type-specific row.
         // We replace the sums by 0.
         for (int p = 0; p < copy.length; p++)
            //set (oldNm - 1, getFirstRealPeriod() + p, 0);
            set (oldNm - 1, p, 0);
      // Copy the backed up sums in the new last row
      for (int p = 0; p < copy.length; p++)
         // for SumMatrixSW
         // set (nm - 1, getFirstRealPeriod() + p, copy[p]);
         set (nm - 1, p, copy[p]);
   }

   /**
    * Equivalent to {@link #add(int,int,double) add}
    * \texttt{(contact.getTypeId(), period, x)} where
    * \texttt{period} is the period at which the contact
    * arrived. If no period change event was
    * associated with this object, the period is always 0.
    @param contact the contact to which the observation is related.
    @param x the value being added.
    @exception NullPointerException if \texttt{contact} is \texttt{null}.
    */
   public void add (Contact contact, double x) {
      final int period = pce == null ? 0 : pce.getPeriod (contact.getArrivalTime());
      add (contact.getTypeId(), period, x);
   }

   /**
    * Equivalent to {@link #add(int,int,double) add}
    * \texttt{(contact.getTypeId(), period, x)}.
    @param contact the contact to which the observation is related.
    @param period the period the observation is added to.
    @param x the value being added.
    @exception NullPointerException if \texttt{contact} is \texttt{null}.
    */
   public void add (Contact contact, int period, double x) {
      final int type = contact.getTypeId();
      add (type, period, x);
   }

   /**
    * Adds a new observation \texttt{x} for contact type \texttt{type}
    * in the period \texttt{period}.
    * If the object supports only one contact type, this will
    * add one observation in the measure 0 of the matrix,
    * independently of the contact type identifier.
    * Otherwise, an observation is added in the measure corresponding
    * the contact type as well as in the last measure for the aggregate
    * sum.
    * Even if the contact type identifier cannot be mapped to a valid
    * measure index, the observation is added to the last row.
    @param type the contact type of the new observation.
    @param period the period of the new observation.
    @param x the value being added.
    @exception ArrayIndexOutOfBoundsException if \texttt{type} or
    \texttt{period} are negative or greater than or equal to the
    number of supported contact types or periods.
    */
   @Override
   public void add (int type, int period, double x) {
      int numTypes1 = getNumMeasures();
      if (numTypes1 > 1)
         numTypes1--;
      if (numTypes1 > 1 && type >= 0 && type < numTypes1)
         super.add (type, period, x);
      super.add (getNumMeasures() - 1, period, x);
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1).append (", ");
      int nm = getNumMeasures();
      if (nm > 1)
         nm--;
      sb.append ("number of supported contact types: ").append (nm);
      sb.append (']');
      return sb.toString();
   }
}
