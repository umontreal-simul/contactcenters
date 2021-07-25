package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Abstract event type with some helper
 * methods to generate 
 * $u_1\le u<u_2$ randomly and uniformly, and
 * to test that $u<t$ for any value of $t$.
 */
public abstract class EventWithTest implements CCEvent {
   private double minU;
   private double maxU;
   private int numBits;
   private int mask;
   private double norm;

   /**
    * Constructs a new event with
    * the associated interval
    * $[u_1,u_2)$, and using
    * \texttt{numBits} additional random
    * bits to generate $u$ when
    * needed.
    * The value of $u_1$ and $u_2$
    * are given using the fields
    * \texttt{minU} and
    * \texttt{maxU}.
    * @param minU the value of $u_1$.
    * @param maxU the value of $u_2$.
    * @param numBits the number of bits
    * used to generate $u$.
    */
   public EventWithTest (double minU, double maxU, int numBits) {
      if (minU > maxU)
         throw new IllegalArgumentException
         ("minU > maxU");
      if (numBits < 0)
         throw new IllegalArgumentException
         ("numBits < 0");
      this.minU = minU;
      this.maxU = maxU;
      this.numBits = numBits;
      final double deltaU = maxU - minU;
      int power = 1;
      for (int j = 0; j < numBits; j++)
         power *= 2;
      mask = power - 1;
      norm = deltaU / power;
   }
   
   /**
    * Returns the value of $u_1$
    * associated with this event.
    * @return the associated value of $u_1$.
    */
   public double getMinU() {
      return minU;
   }
   
   /**
    * Returns the value of $u_2$
    * associated with this event.
    * @return the associated value of $u_2$.
    */
   public double getMaxU() {
      return maxU;
   }
   
   /**
    * Generates the value of $u$
    * using bits in \texttt{rv}
    * but ignoring the first
    * least significant \texttt{usedBits}
    * bits.
    * @param rv the random bits to generate
    * $u$ from.
    * @param usedBits the number of used
    * random bits.
    * @return the value of $u$.
    */
   public double getU (int rv, int usedBits) {
      assert usedBits + numBits <= 31;
      if (mask == 0)
         return minU;
      final int v = (rv >>> usedBits) & mask;
      final double u = minU + v * norm;
      assert u >= minU && u < maxU;
      return u;
   }
   
   /**
    * Returns \texttt{true}
    * if and only if
    * $u< t$, using
    * {@link #getU(int,int) getU}
    * \texttt{(rv, usedBits)}
    * to generate $u$ randomly.
    * This method returns \texttt{true}
    * without generating $u$ if
    * $u_2<t$ since in that case, $u<u_2<t$.
    * Similarly, it returns \texttt{false}
    * without generating $u$ if
    * $u_1\ge t$ since in this case,
    * $u\ge u_1\ge t$.
    * Otherwise, {@link #getU(int,int)}
    * is called to get the value of $u$.
    * 
    * @param rv the random bits to generate
    * $u$ from.
    * @param usedBits the number of used
    * random bits.
    * @param t the tested threshold.
    * @return the success indicator of the test.
    */
   public boolean isUSmallerThan (int rv, int usedBits, double t) {
      if (maxU < t)
         return true;
      if (minU >= t)
         return false;
      return getU (rv, usedBits) < t;
   }
   
   /**
    * Returns the value $s$ for which
    * $0\le u-sW<w$, where $s=0,\ldots,S-1$,  
    * or $S$ if
    * such $s$ does not exist.
    * Here, $w\le W$.
    * For example, if $0\le u<\tilde q$, 
    * $w=\nu$, and $W=\tilde\nu$, $s$ can be interpreted
    * as the position in queue of a contact having
    * abandoned.
    * @param rv the random bits to generate
    * $u$ from.
    * @param usedBits the number of used
    * random bits.
    * @param weight the value of $w$.
    * @param maxWeight the value of $W$.
    * @param maxS the maximal value $S$ of $s$.
    * @return the value of $s$.
    */
   public int getPosition (int rv, int usedBits, double weight, double maxWeight, int maxS) {
      assert weight <= maxWeight;
      final double u1 = minU;
      assert u1 >= 0;
      final double maxValue = maxS*maxWeight;
      if (u1 >= maxValue)
         return maxS;
      final double u = getU (rv, usedBits);
      if (u >= maxValue)
         return maxS;
      final int s = (int) (u / maxWeight);
      if (weight < maxWeight) {
         final double v = u % maxWeight;
         if (v >= weight)
            return maxS;
      }
      return s;
   }
}
