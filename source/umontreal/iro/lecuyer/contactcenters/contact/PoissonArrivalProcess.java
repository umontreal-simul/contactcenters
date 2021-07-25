package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.randvar.RandomVariateGenWithCache;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a Poisson-based contact arrival process.
 * This base class implements a Poisson arrival
 * process with (piecewise-)constant arrival rates:
 * when an inter-arrival time is required,
 * it is generated from the exponential distribution with
 * rate $B\lambda$.  By default, the arrival rate is constant,
 * but it may be changed at any time during the simulation.
 * When the arrival rate changes,
 * the currently scheduled arrival is adjusted
 * automatically to reflect the change.
 * This class can be used as a basis each time the rate
 * function $\lambda(t)$ of a Poisson process is piecewise-constant over
 * the simulation time $t$.
 */
public class PoissonArrivalProcess extends ContactArrivalProcess {
   private double lambda;
   private double blambda;
   private RandomVariateGen expGen;
   private RandomVariateGenWithCache expGenCached;

   /**
    * Constructs a new Poisson arrival process
    * instantiating new contacts using \texttt{factory}.
    * The parameter $\lambda$ is initialized with \texttt{lambda} and
    * the random number stream \texttt{stream} is used to
    * generate the needed uniforms.
    @param factory the factory instantiating contacts.
    @param lambda the initial value of $\lambda(t)$.
    @param stream random number stream.
    @exception IllegalArgumentException if \texttt{lambda} $< 0$.
    @exception NullPointerException if \texttt{factory} or
    \texttt{stream} are \texttt{null}.
    */
   public PoissonArrivalProcess (ContactFactory factory,
         double lambda,
         RandomStream stream) {
      this (Simulator.getDefaultSimulator (),
            factory, lambda, stream);
   }

   /**
    * Equivalent to {@link #PoissonArrivalProcess(ContactFactory,double,RandomStream)},
    * with the given simulator \texttt{sim}.
    */
   public PoissonArrivalProcess (Simulator sim, ContactFactory factory,
                                 double lambda,
                                 RandomStream stream) {
      super (sim, factory);
      if (lambda < 0)
         throw new IllegalArgumentException
            ("lambda cannot be negative");
      this.lambda = lambda;
      if (stream == null)
         throw new NullPointerException
            ("The random stream cannot be null");
      expGen = new ExponentialGen (stream, new ExponentialDist (1.0));
      expGenCached = new RandomVariateGenWithCache (expGen);
      expGenCached.setCaching (false);
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      return lambda;
   }

   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return lambda;
   }

   /**
    * Returns the current value of the arrival rate $\lambda$.
    @return the current value of $\lambda$.
    */
   public double getLambda() {
      return lambda;
   }

   @Override
   public double getArrivalRate (int p) {
      return lambda*getBusynessFactor();
   }

   @Override
   public double getArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return lambda*getBusynessFactor();
   }

   /**
    * Changes the value of $\lambda$ to
    * \texttt{newLambda}.  This adjusts the time of the
    * next arrival if necessary.
    * If \texttt{newLambda} is set to 0, the currently scheduled
    * arrival, if any, is cancelled and the Poisson process
    * is stopped.  The Poisson process can be restarted
    * by setting a new non-zero $\lambda$ value.
    @param newLambda the new value of $\lambda$.
    @exception IllegalArgumentException if \texttt{newLambda} $< 0$.
    */
   public void setLambda (double newLambda) {
      if (newLambda < 0.0)
         throw new IllegalArgumentException ("newLambda < 0");
      boolean mustSchedule = false;
      if (contactEvent.time() > 0) {
         if (newLambda == 0)
            contactEvent.cancel();
         else {
            // Adjust the scheduled event to take account
            // of parameter change.
            double diffTime = contactEvent.time() - simulator().time();
            diffTime *= lambda/newLambda;
            assert !Double.isNaN (diffTime) && !Double.isInfinite (diffTime)
               : "lambda is " + newLambda + ", maybe init was not called";
            contactEvent.reschedule (diffTime);
         }
      }
      else if (lambda == 0 && newLambda > 0)
         mustSchedule = true;
      lambda = newLambda;
      blambda = newLambda*getBusynessFactor();
      if (mustSchedule && isStarted())
         contactEvent.schedule (nextTime());
   }

   /**
    * Returns the random number stream used
    * to generate the uniforms for inter-arrival times.
    @return the random number stream for the uniforms.
    */
   public RandomStream getStream() {
      return expGen.getStream();
   }

   /**
    * Sets the random number stream used to
    * generate the uniforms for the inter-arrival times
    * to \texttt{stream}.
    @param stream the new random number stream.
    @exception NullPointerException if \texttt{stream} is \texttt{null}.
    */
   public void setStream (RandomStream stream) {
      if (stream == null)
         throw new NullPointerException ("The given random stream must not be null");
      expGen = new ExponentialGen
         (stream, new ExponentialDist (1.0));
      expGenCached.setCachedGen (expGen);
   }

   /**
    * Determines if the generated inter-arrival times
    * are cached for more efficiency.
    * When caching is enabled, the arrival process
    * records every standardized inter-arrival time
    * generated.  These random times follow the
    * exponential distribution with $\lambda=1$,
    * and are divided by the arrival rate in use.
    * Therefore, the cache can be used even if
    * the arrival rate changes.
    * The {@link #initCache} method must be called
    * to start reusing cached values.  This avoids some
    * computations and increases the performance, at the
    * expense of memory.  This is useful when comparing
    * several contact centers with common random numbers.
    * By default, this caching is disabled
    * for more efficient memory usage.
    @return the caching indicator for this arrival process.
    */
   public boolean isCaching() {
      return expGenCached.isCaching();
   }

   /**
    * Sets the caching indicator to \texttt{caching} for
    * this Poisson process.
    @param caching the new value of the caching indicator.
    @see #isCaching
    */
   public void setCaching (boolean caching) {
      expGenCached.setCaching (caching);
      if (caching)
         expGen = expGenCached;
      else
         expGen = expGenCached.getCachedGen();
   }

   /**
    * Resets the random variate generator cache
    * to get the generated inter-arrival times.
    * This method has no effect if caching is disabled.
    @see #isCaching
    */
   public void initCache() {
      expGenCached.initCache();
   }

   /**
    * Clears the cached inter-arrival times for this
    * Poisson arrival process.  This has some effect
    * only if caching is enabled.
    @see #isCaching
    */
   public void clearCache() {
      expGenCached.clearCache();
   }

   /**
    * Returns the random variate generator for the exponential
    * arrival times used when caching is enabled.
    * If caching is disabled, the method throws an
    * {@link IllegalStateException}.
    * @return the random variate generator.
    * @exception IllegalStateException if caching is disabled.
    */
   public RandomVariateGenWithCache getGenWithCache() {
      if (!expGenCached.isCaching())
         throw new IllegalStateException ("Caching is disabled");
      return expGenCached;
   }

   @Override
   public void init() {
      super.init();
      blambda = lambda*getBusynessFactor();
   }

   @Override
   public double nextTime() {
      if (lambda == 0)
         return Double.POSITIVE_INFINITY;
      else
         return expGen.nextDouble()/blambda;
   }

   /**
    * This method calls {@link #start()} assuming
    * that the $\lambda$ arrival rate will not
    * change during simulation.
    * Subclasses violating this assumption
    * should override this method.
    */
   @Override
   public void startStationary() {
      start();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      sb.append (", current lambda: ").append (lambda);
      sb.append (']');
      return sb.toString();
   }

   /**
    * Estimates the parameters of a Poisson arrival process
    * with arrival rate $\lambda$
    * from the number of arrivals in the array
    * \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This method sums the number of arrivals on every
    * period for each day and uses the resulting
    * array of \texttt{numObs} observations
    * to estimate a Poisson arrival rate.
    * This returns an array containing the
    * estimated $\hat{\lambda}$, which
    * estimates the expected number of
    * arrivals during one day.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated arrival rates.
    */
   public static double[] getMLE (int[][] arrivals, int numObs, int numPeriods) {
      final int[] totalArrivals = new int[numObs];
      for (int i = 0; i < numObs; i++)
         for (int p = 0; p < numPeriods; p++)
            totalArrivals[i] += arrivals[i][p];
      return PoissonDist.getMLE (totalArrivals, numObs);
   }

   /**
    * Constructs a new arrival process with arrival rate
    * estimated by the maximum likelihood method based on
    * the \texttt{numObs} observations in array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * The estimated arrival rate, which approximates
    * the expected number of arrivals during a day,
    * is divided by \texttt{dayLength} to
    * be relative to one time unit.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream to generate arrival times.
    * @param dayLength the duration of the day, in simulation time units.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the constructed arrival process.
    */
   public static PoissonArrivalProcess getInstanceFromMLE
     (ContactFactory factory, RandomStream stream,
           double dayLength,
           int[][] arrivals, int numObs, int numPeriods) {
      final double[] lambda = getMLE (arrivals, numObs, numPeriods);
      final PoissonArrivalProcess pap = new PoissonArrivalProcess (factory, lambda[0]/dayLength, stream);
      return pap;
   }
}
