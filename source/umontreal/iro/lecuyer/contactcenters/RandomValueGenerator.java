package umontreal.iro.lecuyer.contactcenters;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.randvar.RandomVariateGen;

/**
 * Implements the {@link ValueGenerator} interface when the values come from a
 * continuous and possibly non-stationary distribution. For each period and
 * contact type, a different random variate generator can be used to get a
 * value. This class can be instantiated the same way a
 * {@link ConstantValueGenerator} is constructed, replacing constants with
 * random variate generators.
 */
public class RandomValueGenerator implements ValueGenerator {
   private RandomVariateGen[][] gens;
   private PeriodChangeEvent pce = null;

   /**
    * Constructs a new random stationary value generator with generator
    * \texttt{gen} for each contact type, and supporting \texttt{numTypes}
    * contact types.
    * 
    * @param numTypes
    *           the number of supported contact types.
    * @param gen
    *           the random variate generator used for all contact types.
    */
   public RandomValueGenerator (int numTypes, RandomVariateGen gen) {
      gens = new RandomVariateGen[1][numTypes];
      for (int k = 0; k < numTypes; k++)
         gens[0][k] = gen;
   }

   /**
    * Constructs a new random stationary value generator with generator
    * \texttt{gens[k]} for contact type \texttt{k}.
    * 
    * @param gens
    *           the random variate generators used by this object.
    */
   public RandomValueGenerator (RandomVariateGen[] gens) {
      this.gens = new RandomVariateGen[1][gens.length];
      System.arraycopy (gens, 0, this.gens[0], 0, gens.length);
   }

   /**
    * Constructs a new random value generator with period-change event
    * \texttt{pce}, generator \texttt{gens[p]} for period \texttt{p}, and
    * supporting \texttt{numTypes} contact types.
    * 
    * @param pce
    *           the associated period-change event.
    * @param numTypes
    *           the number of supported contact types.
    * @param gens
    *           the array containing a generator for each period.
    * @exception IllegalArgumentException
    *               if a generator is not specified for each period.
    */
   public RandomValueGenerator (PeriodChangeEvent pce, int numTypes,
         RandomVariateGen[] gens) {
      if (pce.getNumPeriods () != gens.length)
         throw new IllegalArgumentException (
               "Invalid length of gens array, needs one RandomVariateGen for each period");
      this.gens = new RandomVariateGen[gens.length][numTypes];
      for (int p = 0; p < gens.length; p++)
         for (int k = 0; k < numTypes; k++)
            this.gens[p][k] = gens[p];
      this.pce = pce;
   }

   /**
    * Constructs a new random value generator with period-change event
    * \texttt{pce} and random variate generators \texttt{gens}. For the period
    * \texttt{p} and contact type \texttt{k}, the random variate generator
    * \texttt{gens[p][k]} is used.
    * 
    * @param pce
    *           the associated period-change event.
    * @param gens
    *           the array of generators for each period and contact type.
    * @exception IllegalArgumentException
    *               if an array of generators is not specified for each period.
    */
   public RandomValueGenerator (PeriodChangeEvent pce, RandomVariateGen[][] gens) {
      if (pce.getNumPeriods () != gens.length)
         throw new IllegalArgumentException (
               "Invalid length of gens array, needs one array of RandomVariateGen "
                     + "for each period");
      this.gens = gens;
      this.pce = pce;
   }

   /**
    * Returns the array of random variate generators associated with this
    * object. The format of this array is the same as the array passed to
    * \latex{the last constructor}\html{{@link #RandomValueGenerator(PeriodChangeEvent,RandomVariateGen[][])}}.
    * 
    * @return the random variate generators for this object.
    */
   public RandomVariateGen[][] getRandomVariateGens () {
      return gens;
   }

   /**
    * Sets the random variate generators for this object to \texttt{gens}. This
    * method can be used to change the number of supported contact types, but it
    * cannot be used to change the number of periods.
    * 
    * @param gens
    *           the new random variate generators for this object.
    * @exception IllegalArgumentException
    *               if the length of the given array is incorrect.
    */
   public void setRandomVariateGens (RandomVariateGen[][] gens) {
      if (pce == null && gens.length != 1
            || pce != null && pce.getNumPeriods () != gens.length)
         throw new IllegalArgumentException (
               "Invalid length of array, needs one array of values for each period");
      this.gens = gens;
   }

   public double nextDouble (Contact contact) {
      final int ct = contact.getTypeId ();
      final int period = ConstantValueGenerator.getPeriod (pce);
      final RandomVariateGen[] v = gens[period];
      return v[ct].nextDouble ();
   }

   public void init () {}

   @Override
   public String toString () {
      return ConstantValueGenerator.format ("Random", pce);
   }
}
