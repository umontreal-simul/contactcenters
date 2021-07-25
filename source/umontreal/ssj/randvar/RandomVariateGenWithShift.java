package umontreal.ssj.randvar;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.rng.RandomStream;

/**
 * Random variate generator applying a shift to the generated values. This
 * generator uses another random variate generator to generate variates. For
 * each variate $v$, the {@link #nextDouble} method of this generator returns
 * $v-\ell$, where $\ell\in\RR$ is a constant, user-defined shift.
 */
public class RandomVariateGenWithShift extends RandomVariateGen {
   private RandomVariateGen gen;
   private double shift;

   /**
    * Constructs a new random variate generator with underlying generator
    * \texttt{gen}, and shift \texttt{shift}.
    * 
    * @param gen
    *           the generator being used.
    * @param shift
    *           the shift $\ell$.
    */
   public RandomVariateGenWithShift (RandomVariateGen gen, double shift) {
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
   public double getShift () {
      return shift;
   }

   /**
    * Sets the current value of the shift to \texttt{shift}.
    * 
    * @param shift
    *           the new value of the shift.
    */
   public void setShift (double shift) {
      this.shift = shift;
   }

   /**
    * Returns the random variate generator being used by this object.
    * 
    * @return the associated random variate generator.
    */
   public RandomVariateGen getRandomVariateGenerator () {
      return gen;
   }

   @Override
   public Distribution getDistribution () {
      return gen.getDistribution ();
   }

   @Override
   public RandomStream getStream () {
      return gen.getStream ();
   }

   @Override
   public double nextDouble () {
      return gen.nextDouble () - shift;
   }

   @Override
   public void setStream (RandomStream stream) {
      gen.setStream (stream);
   }
}
