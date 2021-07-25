package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents an event that can select the
 * integers $k=0,\ldots,K-1$ and
 * $p=0,\ldots,n_k-1$ such that
 * $0\le u - \sum_{j=0}^{k-1}W_jn_j - pW_k<w_k$.
 * Here $0\le u_1<u_2\le WN$ where
 * $w_k\le W_k\le W$ for $k=0,\ldots,K-1$,
 * and $\sum_{k=0}^{K-1}n_k\le N$.
 * This can be interpreted as selecting an event $p=0,\ldots,n_k-1$ of
 * type $k$, with each event of type $k$ having weight $w_k$.

 * This class can be used, e.g., to determine
 * if a uniform $u$ generates a false transition, or
 * an abandonment of type $k$.
 * In this case, $n_k$ gives the number of queued
 * contacts of type $k$, $w_k=\nu_k$ is the
 * abandonment rate for contacts of type $k$,
 * $W_k=\tilde\nu_k$ is the maximal possible abandonment rate
 * for contacts of type $k$, 
 * and
 * $N$ is the total queue capacity.
 * The integer $p$ then corresponds to the position
 * of the contact in queue having abandoned.
 * 
 * To use this class, one must extend it to
 * provide implementations for {@link #getNumValues(CallCenterCTMCKI)},
 * and {@link #getNumValues(CallCenterCTMCKI,int)}.
 * In the {@link CCEvent#actions(CallCenterCTMCKI,int,int,int,boolean)} method,
 * one then calls {@link #selectType(CallCenterCTMCKI,int,int,int)}
 * to select the event type.
 */
public abstract class EventWithSelection extends EventWithTest {
   private double maxWeight;
   private int numTypes;
   private int kpos;

   /**
    * Constructs a new event with selection.
    * @param minU the value of $u_1$.
    * @param maxU the value of $u_2$.
    * @param numBits the number $b$ of bits used to generate $u$ when the test
    * cannot be completed only with $u_1$, and $u_2$.
    * @param numTypes the number $K$ of event types.
    * @param maxWeight the maximal weight $W$.
    */
   public EventWithSelection (double minU, double maxU, int numBits,
         int numTypes, double maxWeight) {
      super (minU, maxU, numBits);
      this.numTypes = numTypes;
      this.maxWeight = maxWeight;
   }
   
   /**
    * Returns the number $K$ of event types.
    * @return the value of $K$.
    */
   public int getNumTypes() {
      return numTypes;
   }
   
   /**
    * Returns the index $p$ of the last selected event
    * among events of type $k$.
    * @return the last selected position.
    */
   public int getLastSelectedEvent() {
      return kpos;
   }

   /**
    * Returns the weight $w_k$ corresponding to events of type $k$.
    * @param ctmc the tested CTMC.
    * @param k the tested type.
    * @return the weight $w_k$.
    */
   public abstract double getWeight (CallCenterCTMCKI ctmc, int k);
   
   /**
    * Returns the weight $W_k$ corresponding to events of type $k$.
    * @param ctmc the tested CTMC.
    * @param k the tested type.
    * @return the weight $W_k$.
    */
   public abstract double getMaxWeight (CallCenterCTMCKI ctmc, int k);

   /**
    * Returns the current value of $n_k$.
    * @param ctmc the tested CTMC.
    * @param k the tested type.
    * @return the value of $n_k$.
    */
   public abstract int getNumValues (CallCenterCTMCKI ctmc, int k);
   
   /**
    * Returns the sum
    * $\sum_{k=0}^{K-1}n_k$.
    * @param ctmc the tested CTMC.
    * @return the sum of $n_k$'s.
    */
   public abstract int getNumValues (CallCenterCTMCKI ctmc);

   /**
    * Selects and returns an event type $k$.
    * If the event corresponds to a false transition,
    * this returns $K$.
    * Otherwise, the method {@link #getLastSelectedEvent()}
    * can be used to obtain $p$.
    * @param ctmc the CTMC representing the call center.
    * @param tr the number of transitions already done.
    * @param rv the random integer used to simulate the transition.
    * @param usedBits the number of bits already used in \texttt{rv}.
    * @return the selected event type.
    */
   public int selectType (CallCenterCTMCKI ctmc, int tr, int rv, int usedBits) {
      final double minU = getMinU();
      if (minU >= maxWeight * getNumValues (ctmc))
         return numTypes;
      int kmin;
      double u1 = minU;
      double tmp = 0;
      double wmax = 0;
      for (kmin = 0; kmin < numTypes; kmin++) {
         wmax = getMaxWeight (ctmc, kmin);
         assert wmax <= maxWeight;
         tmp = wmax * getNumValues (ctmc, kmin);
         if (u1 < tmp)
            break;
         else
            u1 -= tmp;
      }
      if (kmin == numTypes)
         return numTypes;
      
      double u = getU (rv, usedBits) - minU + u1;
      
      for (int k = kmin; k < numTypes; k++) {
         wmax = getMaxWeight (ctmc, k);
         assert wmax <= maxWeight;
         tmp = wmax * getNumValues (ctmc, k);
         if (u < tmp) {
            kpos = (int)(u / wmax);
            double w = getWeight (ctmc, k);
            if (w < wmax) {
               final double v = u % wmax;
               if (v >= w)
                  return numTypes;
            }
            return k;
         }
         else
            u -= tmp;
      }
      return numTypes;
   }
}
