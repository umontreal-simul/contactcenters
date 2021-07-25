/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents a call center event using random bits to select
 * the index of a subinterval corresponding to an event.
 * The executed selected event might perform some action or
 * a new indexed search.
 * A lookup event can be constructed from any array of call center events.
 * Alternatively, a method 
 * {@link #createIndex(double[],CCEventFactory[],int,int)}
 * is provided to construct a search index. 
 */
public class LookupEvent implements CCEvent {
   private CCEvent[] events;
   private int numBits;
   private int mask;

   /**
    * Creates a new lookup event selecting events
    * from the given array \texttt{events}. 
    * @param events the array of events.
    */
   public LookupEvent (CCEvent[] events) {
      this.events = events;
      int size = events.length;
      numBits = 0;
      while (size > 1) {
         if (size % 2 == 1)
            throw new IllegalArgumentException (
                  "The length of the array must be a power of two");
         size /= 2;
         ++numBits;
         mask = (mask << 1) | 1;
      }
   }

   public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv, int usedBits, boolean changeState) {
      assert usedBits + numBits <= 31;
      final int idx = (rv >>> usedBits) & mask;
      return events[idx].actions (ctmc, tr, rv, usedBits + numBits, changeState);
   }

   private static int getLog2 (int v) {
      int numBits = 0;
      int vv = v;
      while (vv > 1) {
         if (vv % 2 == 1)
            throw new IllegalArgumentException (
                  "The given value must be a power of two");
         vv /= 2;
         ++numBits;
      }
      return numBits;
   }
   
   /**
    * Creates a search index by partitioning the $[0,1]$ in
    * \texttt{numIntervals} subintervals, and using a maximum
    * of \texttt{maxBits} randm bits for the indexed search.
    * A subinterval is assigned an event $l$ created with
    * the factory \texttt{factories[l]} with probability
    * \texttt{prob[l]}.  
    * @param prob the probabilities of occurrence of the events.
    * @param factories the event factories.
    * @param numIntervals the number of subintervals.
    * @param maxBits the maximal number of bits.
    * @return the event performing the indexed search. 
    */
   public static LookupEvent createIndex (double[] prob, CCEventFactory[] factories, int numIntervals, int maxBits) {
      return createIndex (prob, factories, 0, 1, numIntervals, maxBits);
   }
   
   private static LookupEvent createIndex (double[] prob, CCEventFactory[] factories, double minU, double maxU, int numIntervals, int maxBits) {
      if (prob.length != factories.length)
         throw new IllegalArgumentException
         ("prob and factories must share the same length");
      if (prob.length == 0)
         throw new IllegalArgumentException
         ("prob and factories must have positive lengths");
      final double sizeU = maxU - minU;
      final int numBits = getLog2 (numIntervals);
      if (numBits > maxBits)
         throw new IllegalArgumentException ();
      final int bits = Math.min (numBits, maxBits - numBits);
      int newSize = 1;
      for (int j = 0; j < bits; j++)
         newSize *= 2;
      final boolean allowSplit = bits > 0 && sizeU > 1e-10;
      final double deltaU = sizeU / numIntervals;
      CCEvent[] events = new CCEvent[numIntervals];
      jloop:
      for (int j = 0; j < numIntervals; j++) {
         final double u1 = j * deltaU + minU;
         final double u2 = u1 + deltaU;
         double sumProb = 0;
         for (int l = 0; l < prob.length; l++) {
            sumProb += prob[l];
            if (u1 < sumProb) {
               if (allowSplit) {
                  if (u2 >= sumProb)
                     events[j] = createIndex (prob, factories, u1, u2, newSize, maxBits - numBits);
                  else
                     events[j] = factories[l].newInstance (u1, u2, maxBits - numBits);
               }
               else
                  events[j] = factories[l].newInstance (u1, u2, maxBits - numBits);
               continue jloop;
            }
         }
         events[j] = factories[factories.length - 1].newInstance (u1, u2, maxBits - numBits);
      }
      return new LookupEvent (events);
   }
}
