package umontreal.ssj.randvar;

import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.rng.RandomStream;

/**
 * Random variate generator applying a shift to the generated values. This
 * generator uses another random variate generator to generate variates. For
 * each variate $v$, the {@link #nextInt} method of this generator returns
 * $v-\ell$, where $\ell\in\NN$ is a constant, user-defined shift.
 */
public class RandomVariateGenIntWithShift extends RandomVariateGenInt {
   private RandomVariateGenInt gen;
   private int shift;

   /**
    * Constructs a new random variate generator with underlying generator
    * \texttt{gen}, and shift \texttt{shift}.
    * 
    * @param gen
    *           the generator being used.
    * @param shift
    *           the shift $\ell$.
    */
   public RandomVariateGenIntWithShift (RandomVariateGenInt gen, int shift) {
      if (gen == null)
         throw new NullPointerException ("gen cannot be null");
      this.gen = gen;
      this.shift = shift;
   }

   /**
    * Returns the current value of the shift $\ell$.
    * 
    * @return the current value of the shift.
    */
   public int getShift () {
      return shift;
   }

   /**
    * Sets the current value of the shift to \texttt{shift}.
    * 
    * @param shift
    *           the new value of the shift.
    */
   public void setShift (int shift) {
      this.shift = shift;
   }

   /**
    * Returns the random variate generator being used by this object.
    * 
    * @return the associated random variate generator.
    */
   public RandomVariateGenInt getRandomVariateGenerator () {
      return gen;
   }

   @Override
   public DiscreteDistributionInt getDistribution () {
      return gen.getDistribution ();
   }

   @Override
   public RandomStream getStream () {
      return gen.getStream ();
   }

   @Override
   public int nextInt () {
      return gen.nextInt () - shift;
   }

   @Override
   public void setStream (RandomStream stream) {
      gen.setStream (stream);
   }
}
