package umontreal.iro.lecuyer.contactcenters;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Implements the {@link ValueGenerator} interface
 * for a constant and possibly non-stationary value.
 * During each period of the simulation, the generated value
 * is constant for each contact type.  When a new period begins,
 * the constant value can be changed.
 *
 * This implementation only takes contact type
 * identifiers ({@link Contact#getTypeId}) and
 * current period ({@link PeriodChangeEvent#getCurrentPeriod})
 * into account for generating values.
 */
public class ConstantValueGenerator implements ValueGenerator {
   private double[][] vals;
   private PeriodChangeEvent pce = null;

   /**
    * Constructs a new constant stationary value generator
    * supporting \texttt{numTypes} contact types, and
    * with value \texttt{val} for each contact type.
    @param numTypes the number of supported contact types.
    @param val the value that will be returned by {@link #nextDouble}.
    */
   public ConstantValueGenerator (int numTypes, double val) {
      vals = new double[1][numTypes];
      for (int k = 0; k < numTypes; k++)
         vals[0][k] = val;
   }

   /**
    * Constructs a new constant stationary value generator with
    * value \texttt{vals[k]} for contact type \texttt{k}.
    @param vals the values for each contact type.
    */
   public ConstantValueGenerator (double[] vals) {
      this.vals = new double[1][vals.length];
      System.arraycopy (vals, 0, this.vals[0], 0, vals.length);
   }

   /**
    * Constructs a new constant value generator with
    * period-change event \texttt{pce},
    * value \texttt{vals[p]} for period \texttt{p},
    * and supporting
    * \texttt{numTypes} contact types.
    @param pce the associated period-change event.
    @param numTypes the number of supported contact types.
    @param vals the generated value for each period.
    @exception IllegalArgumentException if a value is not specified
    for each period.
    */
   public ConstantValueGenerator (PeriodChangeEvent pce, int numTypes,
                                  double[] vals) {
      if (pce.getNumPeriods() != vals.length)
         throw new IllegalArgumentException
            ("Invalid length of array, needs one value for each period");
      this.vals = new double[vals.length][numTypes];
      for (int p = 0; p < vals.length; p++)
         for (int k = 0; k < numTypes; k++)
            this.vals[p][k] = vals[p];
      this.pce = pce;
   }

   /**
    * Constructs a new constant value generator with
    * values \texttt{vals} and period-change event \texttt{pce}.
    * The array element \texttt{vals[p][k]} gives the value
    * for period \texttt{p}, contact type \texttt{k}.
    @param pce the associated period-change event.
    @param vals the array of values.
    @exception IllegalArgumentException if an array
    of values is not specified for each period.
    */
   public ConstantValueGenerator (PeriodChangeEvent pce, double[][] vals) {
      if (pce.getNumPeriods() != vals.length)
         throw new IllegalArgumentException
            ("Invalid length of array, needs one array of values for each period");
      this.vals = vals;
      this.pce = pce;
   }

   /**
    * Returns the values used by this generator.
    * The format of the array is the same as in the
    * \latex{last constructor}\html{constructor {@link #ConstantValueGenerator(PeriodChangeEvent,double[][])}}.
    @return the associated values.
    */
   public double[][] getValues() {
      return vals;
   }

   /**
    * Sets the values for this generator to \texttt{vals}.
    * This method can be used to change the number of
    * supported contact types, but it cannot be
    * used to change the number of periods.
    @param vals the new values for this generator.
    @exception IllegalArgumentException if the length
    of the given array is incorrect.
    */
   public void setValues (double[][] vals) {
      /* 
       * Changing the number of periods would require
       * to change the PeriodChangeEvent object, which
       * would affect other objects in the system.
       */
      if (pce == null && vals.length != 1 ||
          pce.getNumPeriods() != vals.length)
         throw new IllegalArgumentException
            ("Invalid length of array, needs one array of values for each period");
      this.vals = vals;
   }

   /**
    * This is used internally by this
    * and other value generators implementations
    * to map the current period from the appropriate
    * period index.
    *
    * If \texttt{pce} is \texttt{null}, this returns 0.
    * Otherwise, this returns the value of {@link PeriodChangeEvent#getCurrentPeriod}.
    @param pce the associated period-change event.
    @return the period index to be used.
    */
   static final int getPeriod (PeriodChangeEvent pce) {
      if (pce != null)
         return pce.getCurrentPeriod();
      return 0;
   }

   /**
    * Used internally by
    * \texttt{toString} methods to foarmat
    * value generator strings.
    */
   static final String format (String name, PeriodChangeEvent pce) {
      return name + " value generator" +
       (pce == null ? "" : " with periods defined by " + pce.toString());
   }

   /**
    * Returns the value of the constant corresponding to the
    * type of \texttt{contact}, and the current period.
    * If the contact type identifier is greater than
    * or equal to the number of supported contact types,
    * or a type smaller than zero, an exception is thrown.
    */
   public double nextDouble (Contact contact) {
      final int ct = contact.getTypeId();
      final int period = getPeriod (pce);
      final double[] v = vals[period];
      return v[ct];
   }

   public void init() {}

   @Override
   public String toString() {
      return format ("Constant", pce);
   }
}
