/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.ctmc;


/**
 * Represents information about a change in the arrival rate.
 */
public class RateChangeInfo implements Comparable<RateChangeInfo> {
   private int tr;
   private int k;
   private double rate;
   
   /**
    * Constructs a new object representing a change
    * to arrival rate \texttt{rate}
    * of call type \texttt{k} at transition number \texttt{tr}.
    * @param tr the transition number at which the rate is supposed to change.
    * @param k the affected call type.
    * @param rate the new arrival rate.
    */
   public RateChangeInfo (int tr, int k, double rate) {
      super ();
      this.tr = tr;
      this.k = k;
      this.rate = rate;
   }

   /**
    * Returns the transition number.
    */
   public int getTransition () {
      return tr;
   }

   /**
    * Returns the index of the call type.
    */
   public int getK () {
      return k;
   }

   /**
    * Returns the new arrival rate.
    */
   public double getRate () {
      return rate;
   }

   /**
    * Compares this object with another object \texttt{o}.
    * This comparison method orders objects using
    * the transition number, and the call type for objects
    * with the same transition number.
    */
   public int compareTo (RateChangeInfo o) {
      if (tr < o.tr)
         return -1;
      if (tr > o.tr)
         return 1;
      if (k < o.k)
         return -1;
      if (k > o.k)
         return 1;
      return 0;
   }
   
   @Override
   public String toString() {
      return String.format ("tr=%d, k=%d, rate=%f", tr, k, rate);
   }
}