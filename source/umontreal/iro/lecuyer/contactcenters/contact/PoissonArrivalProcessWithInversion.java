package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.util.RootFinder;

/**
 * Defines a Poisson arrival process with arrival rate $B\lambda(t)$
 * at time $t$
 * and generated by inversion.
 * If
 * \[B\Lambda(t)=\int_0^t B\lambda(s)ds\]
 * is the cumulative arrival rate of the Poisson process,
 * and
 * $M(t)=N(\Lambda^{-1}(t)/B)$,
 * $\{M(t), t\ge 0\}$ is a standard Poisson process, i.e.,
 * homogeneous with arrival rate 1.
 * If $\Lambda^{-1}(t)$ can be computed easily,
 * this class generates arrival times
 * by inversion as follows:
 * generate the arrival times $X_0, X_1, \ldots$
 * for a standard Poisson process and let
 * $T_j=\Lambda^{-1}(X_j)/B$ be the arrival times of the
 * non-homogeneous Poisson process.
 */
public class PoissonArrivalProcessWithInversion extends PoissonArrivalProcess {
   private MathFunction cLambda;
   private MathFunction invLambda;

   /**
    * Constructs a new transformed Poisson arrival process
    * using contact factory \texttt{factory} for
    * creating contacts, random stream
    * \texttt{stream} for generating uniforms,
    * \texttt{cLambda} for the $\Lambda(t)$ function,
    * and \texttt{invLambda} for the $\Lambda^{-1}(t)$
    * function.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream used to generate uniforms.
    * @param cLambda the function defining $\Lambda(t)$.
    * @param invLambda the function defining $\Lambda^{-1}(t)$.
    * @exception NullPointerException if any argument is
    * \texttt{null}.
    */
   public PoissonArrivalProcessWithInversion (ContactFactory factory, RandomStream stream, MathFunction cLambda, MathFunction invLambda) {
      this (Simulator.getDefaultSimulator (), factory, stream, cLambda, invLambda);
   }

   /**
    * Equivalent to {@link #PoissonArrivalProcessWithInversion(ContactFactory,RandomStream,MathFunction,MathFunction)},
    * using the given simulator \texttt{sim}.
    */
   public PoissonArrivalProcessWithInversion (Simulator sim, ContactFactory factory, RandomStream stream, MathFunction cLambda, MathFunction invLambda) {
      super (sim, factory, 1.0, stream);
      if (cLambda == null || invLambda == null)
         throw new NullPointerException();
      this.cLambda = cLambda;
      this.invLambda = invLambda;
   }

   /**
    * Similar to {@link #PoissonArrivalProcessWithInversion(ContactFactory, RandomStream,MathFunction,MathFunction)
    * Poisson\-Arrival\-Process\-With\-Inversion
    * (factory, stream, cLambda, f)}, where \texttt{f}
    * is a function performing the inversion of \texttt{cLambda}
    * using the Brent-Decker root finding algorithm.
    * This can be used when the $\Lambda^{-1}(t)$ function is unavailable,
    * and $\Lambda(t)$ can be computed efficiently.
    * However, the generated inversion function can be slow to compute.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream used to generate uniforms.
    * @param cLambda the function defining $\Lambda(t)$.
    * @exception NullPointerException if any argument is
    * \texttt{null}.
    */
   public PoissonArrivalProcessWithInversion (ContactFactory factory, RandomStream stream, MathFunction cLambda) {
      this (Simulator.getDefaultSimulator (), factory, stream, cLambda);
   }

   /**
    * Equivalent to {@link #PoissonArrivalProcessWithInversion(ContactFactory,RandomStream,MathFunction)},
    * using the given simulator \texttt{sim}.
    */
   public PoissonArrivalProcessWithInversion (Simulator sim, ContactFactory factory, RandomStream stream, MathFunction cLambda) {
      super (sim, factory, 1.0, stream);
      if (cLambda == null)
         throw new NullPointerException();
      this.cLambda = cLambda;
      invLambda = new InverseFunction (cLambda);
   }

   @Override
   public void startStationary() {
      throw new UnsupportedOperationException();
   }

   private class InverseFunction implements MathFunction {
      private MathFunction fn;

      public InverseFunction (MathFunction fn) {
         this.fn = fn;
      }

      public double evaluate (double x) {
         final MathFunction rfn = new ShiftedFunction (fn, x);
         final double simTime = simulator().time();
         return RootFinder.brentDekker (simTime, simTime + 1.0, rfn, 1e-8);
      }
   }

   private static class ShiftedFunction implements MathFunction {
      private MathFunction fn;
      private double delta;

      public ShiftedFunction (MathFunction fn, double delta) {
         this.fn = fn;
         this.delta = delta;
      }

      public double evaluate (double x) {
         return fn.evaluate (x) - delta;
      }
   }

   /**
    * Returns the function $\Lambda(t)$ in use.
    * @return the $\Lambda(t)$ function.
    */
   public MathFunction getCumulativeLambdaFunction () {
      return cLambda;
   }

   /**
    * Sets the $\Lambda(t)$ function to
    * \texttt{cLambda}.
    * @param cLambda the new $\Lambda(t)$ function.
    * @exception NullPointerException if \texttt{cLambda} is
    * \texttt{null}.
    */
   public void setCumulativeLambdaFunction (MathFunction cLambda) {
      if (cLambda == null)
         throw new NullPointerException();
      this.cLambda = cLambda;
      if (invLambda instanceof InverseFunction)
         invLambda = new InverseFunction (cLambda);
   }

   /**
    * Returns the function $\Lambda^{-1}(t)$ in use.
    * @return the $\Lambda^{-1}(t)$ function.
    */
   public MathFunction getInvertedLambdaFunction() {
      return invLambda;
   }

   /**
    * Sets the $\Lambda^{-1}(t)$ function to
    * \texttt{invLambda}.
    * If \texttt{invLambda} is \texttt{null}, the method sets the
    * current $\Lambda^{-1}(t)$ to the default inversion function, which
    * uses the Brent-Decker root finder.
    * @param invLambda the new $\Lambda^{-1}(t)$ function.
    */
   public void setInvertedLambdaFunction (MathFunction invLambda) {
      if (invLambda == null)
         this.invLambda = new InverseFunction (cLambda);
      else
         this.invLambda = invLambda;
   }

   @Override
   public void setLambda (double lambda) {
      throw new UnsupportedOperationException
      ("lambda cannot be changed");
   }

   @Override
   public double nextTime() {
      // Generate an exponential variate with rate lambda=1, not Blambda
      final double time = super.nextTime() * getBusynessFactor();
      // Get the standardized arrival time
      final double t = cLambda.evaluate (simulator().time()) + time;
      // Get the arrival time, with Blambda(t) arrival rate
      final double t2 = invLambda.evaluate (t) / getBusynessFactor();
      // Get the inter-arrival time
      return t2 - simulator().time();
   }

   @Override
   public double getArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return getBusynessFactor ()*(cLambda.evaluate (et) - cLambda.evaluate (st)) / (et - st);
   }

   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return cLambda.evaluate (et) - cLambda.evaluate (st) / (et - st);
   }
}
