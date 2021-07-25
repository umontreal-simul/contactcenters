package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.rng.RandomStream;
import cern.colt.list.DoubleArrayList;

/**
 * This arrival process can be used when the number of
 * arrivals per period $A_p$ is known (when $B = 1$). By default,
 * for each period~$p$, $A_p^*=\mathrm{round}(BA_p)$
 * uniforms are generated and sorted
 * in increasing order to get the inter-arrival times,
 * supposing we have a Poisson process with stationary increments.
 * Because this algorithm requires the duration of each period,
 * arrivals are not allowed during the wrap-up period,
 * which has a random duration not known at the
 * time arrivals are generated.
 * The algorithm for generating arrival times can be
 * customized by overriding {@link #computeArrivalTimes}.
 * The number of arrivals, constant by default, can also be
 * changed between replications.
 */
public class PoissonUniformArrivalProcess extends ContactArrivalProcess {
   private PeriodChangeEvent pce;
   private int idx = 0;
   private int[] arrivals;
   private RandomStream stream;
   /**
    * Array list containing the arrival times of contacts.
    */
   protected DoubleArrayList times = new DoubleArrayList();

   /**
    * Constructs a new arrival process with known number
    * of arrivals in each period.  The constructed process
    * uses period-change event
    * \texttt{pce}, contact factory \texttt{factory}, mean number of
    * arrivals \texttt{arrivals[p]} in period \texttt{p}, and
    * random number stream \texttt{stream} to generate
    * random values.
    @param pce the period-change event defining the periods.
    @param factory the contact factory used to create contacts.
    @param arrivals the mean number of arrivals in each period.
    @param stream the random number stream for the uniforms.
    @exception IllegalArgumentException if there is not a mean number
    of arrivals for each period.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public PoissonUniformArrivalProcess (PeriodChangeEvent pce,
                                        ContactFactory factory,
                                        int[] arrivals, RandomStream stream) {
      super (pce.simulator (), factory);
      if (pce.getNumPeriods() > arrivals.length)
         throw new IllegalArgumentException
            ("Invalid arrivals size, needs one mean number of arrivals for each period");
      if (stream == null)
         throw new NullPointerException ("The given random stream must not be null");
      this.arrivals = arrivals;
      this.pce = pce;
      this.stream = stream;
   }

   /**
    * Returns the random number stream used to generate uniforms.
    @return the associated random number stream.
    */
   public RandomStream getStream() {
      return stream;
   }

   /**
    * Sets the random number stream to
    * \texttt{stream} for generating uniforms.
    @param stream the new random number stream.
    @exception NullPointerException if \texttt{stream} is \texttt{null}.
    */
   public void setStream (RandomStream stream) {
      if (stream == null)
         throw new NullPointerException ("The given random stream must not be null");
      this.stream = stream;
   }

   /**
    * Returns the period-change event associated with this
    * object.
    @return the associated period-change event.
    */
   public PeriodChangeEvent getPeriodChangeEvent() {
      return pce;
   }

   @Override
   public double getArrivalRate (int p) {
      return arrivals[p]/pce.getPeriodDuration (p);
   }

   /**
    * Computes the arrival rate using
    * the period-change event
    * returned by {@link #getPeriodChangeEvent()}
    * to determine the boundaries of periods, and
    * the arrival rates returned by
    * {@link #getArrivalRate(int)}.
    */
   @Override
   public double getArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      int p = pce.getPeriod (st);
      double totalRate = 0;
      while (p < pce.getNumPeriods () - 1 && et >= pce.getPeriodEndingTime (p)) {
         final double rate = getArrivalRate (p);
         double s = Math.max (st, pce.getPeriodStartingTime (p));
         double e = Math.min (et, pce.getPeriodEndingTime (p));
         totalRate += (e - s) * rate;
         ++p;
      }
      if (p == pce.getNumPeriods () - 1) {
         final double rate = getArrivalRate (p);
         totalRate += rate * (et - pce.getPeriodStartingTime (p));
      }
      return totalRate / (et - st);
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      return getArrivalRate (p);
   }

   /**
    * Computes the expected arrival rate using
    * the period-change event
    * returned by {@link #getPeriodChangeEvent()}
    * to determine the boundaries of periods, and
    * the expected arrival rates returned by
    * {@link #getExpectedArrivalRate(int)}.
    */
   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      int p = pce.getPeriod (st);
      double totalRate = 0;
      while (p < pce.getNumPeriods () - 1 && et >= pce.getPeriodEndingTime (p)) {
         final double rate = getExpectedArrivalRate (p);
         double s = Math.max (st, pce.getPeriodStartingTime (p));
         double e = Math.min (et, pce.getPeriodEndingTime (p));
         totalRate += (s - e) * rate;
         ++p;
      }
      if (p == pce.getNumPeriods () - 1) {
         final double rate = getExpectedArrivalRate (p);
         totalRate += rate * (et - pce.getPeriodStartingTime (p));
      }
      return totalRate / (et - st);
   }

   /**
    * Returns the number of arrivals
    * for each period.
    @return the array of number of arrivals.
    */
   public int[] getArrivals() {
      return arrivals;
   }

   /**
    * Sets the number of arrivals in each period
    * to \texttt{arrivals}.
    @param arrivals the number of arrivals.
    @exception NullPointerException if the array is \texttt{null}.
    @exception IllegalArgumentException if the length of the given
    array does not correspond to the number of periods
    as defined by {@link #getPeriodChangeEvent}.
    */
   public void setArrivals (int[] arrivals) {
      if (arrivals.length < pce.getNumPeriods())
         throw new IllegalArgumentException
            ("Length of arrivals does not correspond to the number of periods");
      this.arrivals = arrivals;
   }

   @Override
   public void init() {
      super.init();
      times.clear();
      computeArrivalTimes();
      idx = 0;
   }

   /**
    * This is called by {@link #init} to compute the arrival times
    * based on the number of arrivals in each period.
    * The arrival times must be stored in the array list {@link #times}, which
    * is empty at the time the method is called.  The arrival times in
    * the list are assumed to be sorted in increasing order after
    * the method returns.
    * The method should use the random stream returned
    * by {@link #getStream} to generate
    * the random numbers.
    *
    * By default, for each period $p=0,\ldots,P$ (preliminary and main
    * periods), this generates $A_p^*$ uniforms in $[t_{p-1}, t_p)$, where
    * $t_{-1}=0$, $A_p$ is the expected number of arrivals in period~$p$, and
    * $t_p$ is the ending time of period~$p$.
    * The generated arrival times are then sorted.
    * If $B=1$, $A_p^*=A_p$, otherwise
    * $A_p^*=\mathrm{round}(A_p B)$.
    */
   protected void computeArrivalTimes() {
      if (arrivals[pce.getNumPeriods() - 1] > 0)
         throw new IllegalStateException
            ("No arrival is allowed during the wrap-up period");
      final double b = getBusynessFactor();
      final int np = pce.getNumPeriods();
      for (int p = 0; p < np - 1; p++) {
         final double start = pce.getPeriodStartingTime (p);
         final double length = pce.getPeriodDuration (p);
         int numArrivals = arrivals[p];
         if (b != 1.0)
            numArrivals = (int)Math.round (b*numArrivals);
         for (int j = 0; j < numArrivals; j++) {
            final double v = stream.nextDouble()*length + start;
            times.add (v);
         }
      }
      Arrays.sort (times.elements(), 0, times.size());
   }

   @Override
   public double nextTime() {
      double t = -1;
      while (t < 0 && idx < times.size())
         t = times.get (idx++) - simulator().time();
      if (t < 0 && idx == times.size())
         return Double.POSITIVE_INFINITY;
      else
         return t;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      if (pce.getName().length() > 0)
         sb.append (", period change event: ").append (pce.getName());
      sb.append (", per-period arrivals: {");
      for (int i = 0; i < arrivals.length; i++)
         sb.append (i > 0 ? ", " : "").append (arrivals[i]);
      sb.append ("}]");
      return sb.toString();
   }
}
