package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.functions.MathFunctionUtil;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;

/**
 * Defines a Poisson arrival process with arrival rate
 * $B\lambda(t)\le B\bar\lambda$ for time $t$, and generated using the
 * thinning method.
 * This arrival process generates pseudo-arrivals as a
 * homogeneous Poisson process with rate $B\bar\lambda$.
 * A pseudo-arrival at time $t$ is accepted, i.e., becomes an arrival,
 * with probability $\lambda(t)/\bar\lambda$, and
 * rejected with probability
 * $1 - \lambda(t)/\bar\lambda$.
 */
public class PoissonArrivalProcessWithThinning extends PoissonArrivalProcess {
   private MathFunction lambda;
   private RandomStream uStream;
   private double maxTime;

   /**
    * Constructs a new thinned Poisson arrival process
    * using \texttt{factory} to generate contacts,
    * \texttt{stream} to generate pseudo-arrivals,
    * \texttt{uStream} to test for acceptance or
    * rejection, \texttt{lambda} for
    * $\lambda(t)$, and
    * \texttt{lambdaMax} for $\bar\lambda$.
    * @param factory the contact factory used to construct contacts.
    * @param stream the random stream for pseudo-arrivals.
    * @param uStream the random stream for tests of acceptance.
    * @param lambda the function $\lambda(t)$.
    * @param lambdaMax the value of $\bar\lambda$.
    * @param maxTime the smallest time $T$ for which
    * $\lambda(t)=0$ for any $t\ge T$.
    * @exception NullPointerException if any argument is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{lambdaMax} is
    * negative, infinite, or NaN, or if \texttt{maxTime} is
    * negative.
    */
   public PoissonArrivalProcessWithThinning (ContactFactory factory, RandomStream stream, RandomStream uStream, MathFunction lambda, double lambdaMax, double maxTime) {
      this (Simulator.getDefaultSimulator (), factory, stream, uStream, lambda, lambdaMax, maxTime);
   }

   /**
    * Equivalent to {@link #PoissonArrivalProcessWithThinning(ContactFactory,RandomStream,RandomStream,MathFunction,double,double)},
    * using the given simulator \texttt{sim}.
    */
   public PoissonArrivalProcessWithThinning (Simulator sim, ContactFactory factory, RandomStream stream, RandomStream uStream, MathFunction lambda, double lambdaMax, double maxTime) {
      super (sim, factory, lambdaMax, stream);
      if (lambda == null)
         throw new NullPointerException();
      if (uStream == null)
         throw new NullPointerException();
      if (lambdaMax < 0 || Double.isInfinite (lambdaMax) || Double.isNaN (lambdaMax))
         throw new IllegalArgumentException
         ("Invalid value of lambdaMax: " + lambdaMax);
      if (maxTime < 0)
         throw new IllegalArgumentException
         ("maxTime must not be negative");
      this.lambda = lambda;
      this.uStream = uStream;
      this.maxTime = maxTime;
   }

   /**
    * Returns the $\lambda(t)$ function.
    * @return the $\lambda(t)$ function.
    */
   public MathFunction getLambdaFunction() {
      return lambda;
   }

   /**
    * Sets the $\lambda(t)$ function to \texttt{lambda}.
    * @param lambda the new $\lambda(t)$ function.
    * @exception NullPointerException if \texttt{lambda} is
    * \texttt{null}.
    */
   public void setLambdaFunction (MathFunction lambda) {
      if (lambda == null)
         throw new NullPointerException();
      this.lambda = lambda;
   }

   /**
    * Returns the value of $\bar\lambda$.
    */
   @Override
   public double getLambda() {
      return super.getLambda();
   }

   /**
    * Sets the value of $\bar\lambda$ to \texttt{lambda}.
    * @exception IllegalArgumentException if \texttt{lambdaMax} is
    * negative, infinite, or NaN.
    */
   @Override
   public void setLambda (double lambda) {
      if (lambda < 0 || Double.isInfinite (lambda) || Double.isNaN (lambda))
         throw new IllegalArgumentException
         ("Invalid value of lambdaMax: " + lambda);
      super.setLambda (lambda);
   }

   /**
    * Returns the random stream for tests of acceptance.
    * @return the random stream for tests of acceptance.
    */
   public RandomStream getRejectionStream() {
      return uStream;
   }

   /**
    * Sets the random stream for tests of acceptance to
    * \texttt{uStream}.
    * @param uStream the new random stream for tests of acceptance.
    * @exception NullPointerException if \texttt{uStream} is \texttt{null}.
    */
   public void setRejectionStream (RandomStream uStream) {
      if (uStream == null)
         throw new NullPointerException();
      this.uStream = uStream;
   }

   /**
    * Returns the smallest time $T$ for which
    * $\lambda(t)=0$ for all $t\ge T$.
    * This corresponds to the maximal time an arrival
    * can occur.
    * @return the maximal time.
    */
   public double getMaximalTime() {
      return maxTime;
   }

   /**
    * Sets the maximal time $T$ to \texttt{maxTime}.
    * @param maxTime the new maximal time $T$.
    * @exception IllegalArgumentException if \texttt{maxTime} is
    * negative.
    */
   public void setMaximalTime (double maxTime) {
      if (maxTime < 0)
         throw new IllegalArgumentException
         ("maxTime must not be negative");
      this.maxTime = maxTime;
   }

   @Override
   public void startStationary() {
      throw new UnsupportedOperationException();
   }

   @Override
   public double nextTime() {
      double time = simulator().time();
      while (time < maxTime) {
         // Generate a pseudo arrival time with constante arrival rate
         // B*lambda
         time += super.nextTime();
         if (time >= maxTime)
            continue;
         // Determine if the arrival is accepted
         final double probAccept = lambda.evaluate (time) / getLambda();
         assert probAccept >= 0 && probAccept <= 1 :
            "Invalid probability of acceptance " + probAccept +
            "; time=" + time + ", lambda(t)=" + lambda.evaluate (time) +
            ", lambdaMax=" + getLambda();
         if (uStream.nextDouble() <= probAccept)
            // Return the inter-arrival time
            return time - simulator().time();
      }
      return Double.POSITIVE_INFINITY;
   }

   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return MathFunctionUtil.integral (lambda, st, et) / (et - st);
   }

   @Override
   public double getArrivalRate (double st, double et) {
      return getExpectedArrivalRate (st, et) * getBusynessFactor ();
   }
}
