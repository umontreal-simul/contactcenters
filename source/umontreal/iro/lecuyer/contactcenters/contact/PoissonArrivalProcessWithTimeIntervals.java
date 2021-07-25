package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a Poisson arrival process with piecewise-constant
 * arrival rates that can change at arbitrary moments during the simulation.
 * This process is similar to {@link PiecewiseConstantPoissonArrivalProcess},
 * except the times arrival rates change do not need to correspond to
 * main periods.
 * More specifically, let $t_0<\cdots<t_L$ be an increasing
 * sequence of simulation times, and let
 * $B\lambda_j$, for $j=0,\ldots,L-1$, be the arrival rate
 * during time interval $[t_j,t_{j+1})$.
 * The arrival rate is 0 for $t<t_0$ and $t\ge t_L$. 
 */
public class PoissonArrivalProcessWithTimeIntervals extends
      PoissonArrivalProcess {
   private double[] times;
   private double[] lambdas;
   private boolean normalize = false;
   private int currentInterval = 0;
   private Event nextIntervalEvent;
   
   /**
    * Calls {@link #PoissonArrivalProcessWithTimeIntervals(Simulator,ContactFactory,double[],double[],RandomStream)
    * Poisson\-Arrival\-Process\-With\-Time\-Intervals}
    * \texttt{(Simulator.getDefaultSimulator(), factory, times, lambdas, stream)}.
    */
   public PoissonArrivalProcessWithTimeIntervals
   (ContactFactory factory, double[] times, double[] lambdas, RandomStream stream) {
      this (Simulator.getDefaultSimulator (), factory, times, lambdas, stream);
   }
   
   /**
    * Constructs a new arrival process using the
    * simulator \texttt{sim}, the contact factory
    * \texttt{factory} for creating contacts,
    * times $t_0,\ldots,t_L$ in array \texttt{times},
    * and arrival rates in array \texttt{lambdas}.
    * Inter-arrival times are generated using the random
    * stream \texttt{stream}.
    * @param sim the simulator used to schedule events. 
    @param factory the factory creating contacts for this arrival process.
    * @param times the sequence of times at which arrival rate changes.
    * @param lambdas the arrival rates.
    * @param stream the random stream for inter-arrival times.
    * @exception NullPointerException if any argument is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{lambdas.length}
    * is smaller than 1, or if \texttt{times.length} does not correspond
    * to \texttt{lambdas.length} plus 1, or
    * if \texttt{times} is not an increasing sequence of numbers.
    */
   public PoissonArrivalProcessWithTimeIntervals
   (Simulator sim, ContactFactory factory, double[] times, double[] lambdas, RandomStream stream) {
      super (sim, factory, lambdas[0], stream);
      if (times.length != lambdas.length + 1)
         throw new IllegalArgumentException
         ("times.length must be lambdas.length + 1");
      if (lambdas.length == 0)
         throw new IllegalArgumentException
         ("lambdas must contain at least one arrival rate");
      if (times[0] < 0)
         throw new IllegalArgumentException
         ("Times must not be negative");
      for (int i = 1; i < times.length; i++)
         if (times[i-1] >= times[i])
            throw new IllegalArgumentException
            ("Times must be increasing");
      this.times = times.clone ();
      this.lambdas = lambdas.clone ();
      nextIntervalEvent = new NextIntervalEvent (sim);
   }
   
   /**
    * Determines if the base
    * arrival rates are normalized with length of intervals.
    * When normalization is enabled, for interval $t_{j+1}-t_j$,
    * the effective base arrival rate is $\lambda_j/ (t_{j+1}-t_j)$.
    * If normalization is disabled (the default), the base arrival
    * rates are used as specified.
    @return if the arrival process normalizes base arrival rates.
    */
   public boolean isNormalizing() {
      return normalize;
   }

   /**
    * Sets the arrival rates normalization indicator to \texttt{b}.
    @param b the new arrival rate normalization indicator.
    @see #isNormalizing
    */
   public void setNormalizing (boolean b) {
      normalize = b;
   }
   
   /**
    * Returns the array of times containing
    * $t_0,\ldots,t_L$.
    * @return the array of times.
    */
   public double[] getTimes() {
      return times.clone ();
   }
   
   /**
    * Similar to {@link #getArrivalRates(double[])},
    * for the arrival rates per interval.
    */
   public double[] getArrivalRatesInt() {
      double[] rates = getExpectedArrivalRatesInt();
      for (int j = 0; j < rates.length; j++)
         rates[j] *= getBusynessFactor ();
      return rates;
   }
   
   /**
    * Similar to {@link #getExpectedArrivalRates(double[])},
    * for the arrival rates per interval.
    */
   public double[] getExpectedArrivalRatesInt() {
      double[] rates = lambdas.clone ();
      if (normalize) {
         for (int j = 0; j < rates.length; j++)
            rates[j] /= times[j+1] - times[j];
      }
      return rates;
   }
   
   /**
    * Similar to {@link #getExpectedArrivalRatesB(double[])},
    * for the arrival rates per interval.
    */
   public double[] getExpectedArrivalRatesBInt() {
      double[] rates = getExpectedArrivalRatesInt();
      for (int j = 0; j < rates.length; j++)
         rates[j] *= getExpectedBusynessFactor ();
      return rates;
   }
   
   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      double totalRate = 0;
      for (int j = 0; j < times.length - 1; j++) {
         final double i1 = times[j];
         final double i2 = times[j+1];
         if (et < i1 || st >= i2)
            continue;
         final double s = Math.max (i1, st);
         final double e = Math.min (i2, et);
         final double rate = normalize ? lambdas[j] / (i2 - i1) : lambdas[j];
         totalRate += (e - s) * rate;
      }
      return totalRate / (et - st);
   }
   
   @Override
   public double getArrivalRate (double st, double et) {
      return getExpectedArrivalRate (st, et) * getBusynessFactor ();
   }
   
   @Override
   public void init() {
      super.init ();
      currentInterval = 0;
   }

   @Override
   public void startStationary() {
      throw new UnsupportedOperationException();
   }
   
   @Override
   public void start() {
      scheduleNextInterval();
      super.start ();
   }
   
   @Override
   public void stop() {
      nextIntervalEvent.cancel ();
      super.stop ();
   }
   
   private void scheduleNextInterval() {
      final double simTime = simulator ().time ();
      if (simTime < times[0]) {
         setLambda (0);
         nextIntervalEvent.schedule (times[0] - simTime);
      }
      else {
         while (currentInterval < times.length - 1 && simTime >= times[currentInterval + 1])
            ++currentInterval;
         if (currentInterval < lambdas.length) {
            double l = lambdas[currentInterval];
            if (normalize)
               l /= times[currentInterval + 1] - times[currentInterval];
            setLambda (l);
            nextIntervalEvent.schedule (times[currentInterval + 1] - simTime);
         }
         else
            setLambda (0);
      }
   }
   
   private class NextIntervalEvent extends Event {
      public NextIntervalEvent (Simulator sim) {
         super (sim);
      }

      @Override
      public void actions () {
         scheduleNextInterval ();
      }
   }
}
