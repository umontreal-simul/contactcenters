package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.rng.RandomStream;

/**
 * Represents an arrival process in which the numbers of arrivals 
 * per-period $C_p$ (the counts) are given (in a file or directly).
 * $A_0$ and $A_{P+1}$, the number of arrivals during the
 * preliminary and the wrap-up periods, respectively, are always 0
 * for this process.
 * The busyness factor is always 1.
 */
public class FixedCountsArrivalProcess extends PoissonUniformArrivalProcess {
   /**
    * Constructs a new Poisson arrival process conditional on the number
    * of arrivals being given in each period. With period-change event
    * \texttt{pce}, contact factory \texttt{factory}, number of arrivals
    * in each period \texttt{arrivals}, and random number stream
    * \texttt{stream}.
    @param pce the period-change event defining the periods.
    @param factory the contact factory instantiating contacts.
    @param counts the number of arrivals in each period.
    @param stream the random number stream for uniform arrival times.
    @exception NullPointerException if one argument is \texttt{null}.
    @exception IllegalArgumentException if the length of \texttt{arrivals}
    do not correspond to number of main periods~$P$.
    */
   public FixedCountsArrivalProcess (PeriodChangeEvent pce,
   		ContactFactory factory, int[] counts, RandomStream stream) {
      super (pce, factory, counts, stream);
   }

   @Override
   public void init() {
      setBusynessFactor(1);
      super.init();
   }

   @Override
   public double getArrivalRate (int p) {
      final double d = getPeriodChangeEvent().getPeriodDuration (p);
      if (p < 0 || p > d + 1)
         throw new IllegalArgumentException ("Invalid period index " + p);
      if (p == 0 || p == d + 1)
         return 0;
      final int[] numArr = getArrivals();
      return numArr[p] / d;
   }
}
