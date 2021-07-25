package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;

/**
 * Represents a poisson arrival process with
 * piecewise-constant randomized arrival rates following
 * a user-defined distribution.
 * When constructing an arrival process of this type,
 * one gives a specific random variate generator
 * for each period.
 * The arrival rates are generated independently for
 * each period, each time the process is initialized.
 */
public class PoissonArrivalProcessWithRandomRates extends PiecewiseConstantPoissonArrivalProcess {
   private RandomVariateGen[] genLambdas;
   
   /**
    * Constructs a new Poisson-gamma arrival process
    * using \texttt{factory} to instantiate contacts.
    * For each period \texttt{p}, the parameters of the random
    * rate are given in \texttt{genLambdas[p]}.
    * The random stream \texttt{stream} is used
    * to generate the uniforms for the exponential times.
    @param pce the period-change event associated with this object.
    @param factory the factory creating contacts for this generator.
    @param genLambdas the random variate generators, for each period.
    @param stream random number stream for the exponential variates.
    @exception IllegalArgumentException if there is not one generator
    for each period.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public PoissonArrivalProcessWithRandomRates (PeriodChangeEvent pce,
         ContactFactory factory,
         RandomVariateGen[] genLambdas,
         RandomStream stream) {
      super (pce, factory, new double[genLambdas.length], stream);
      if (genLambdas.length <
          pce.getNumPeriods())
         throw new IllegalArgumentException ("Invalid number of " +
            "parameters, needs one random variate generator for each period");
      this.genLambdas = genLambdas;
   }

   /**
    * Returns the random variate generators for
    * the arrival rates..
    @return the random variate generators for the arrival rates.
    */
   public RandomVariateGen[] getRateGenerators() {
      return genLambdas.clone ();
   }

   /**
    * Sets the random variate generators for the arrival
    * rates to \texttt{genLambdas}.
    @param genLambdas the random variate generators, for each period.
    @exception NullPointerException if \texttt{genLambdas} is \texttt{null}.
    @exception IllegalArgumentException if the length of the given
    array does not correspond to at least the number of periods.
    */
   public void setRateGenerators (RandomVariateGen[] genLambdas) {
      if (genLambdas.length < getPeriodChangeEvent().getNumPeriods())
         throw new IllegalArgumentException
            ("Invalid length of genLambdas");
      this.genLambdas = genLambdas;
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      double l = genLambdas[p].getDistribution ().getMean (); 
      if (isNormalizing() && !getPeriodChangeEvent().isWrapupPeriod (p)) {
         final double d = getPeriodChangeEvent().getPeriodDuration (p);
         if (d > 0)
            l /= d;
      }
      return l;
   }
   
   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      final PeriodChangeEvent pce = getPeriodChangeEvent ();
      int p = pce.getPeriod (st);
      double totalRate = 0;
      while (p < pce.getNumPeriods () - 1 && et >= pce.getPeriodEndingTime (p)) {
         final double rate = getExpectedArrivalRate (p);
         double s = Math.max (st, pce.getPeriodStartingTime (p));
         double e = Math.min (et, pce.getPeriodEndingTime (p));
         totalRate += (e - s) * rate;
         ++p;
      }
      if (p == pce.getNumPeriods () - 1) {
         final double rate = getExpectedArrivalRate (p);
         totalRate += rate * (et - pce.getPeriodStartingTime (p));
      }
      return totalRate / (et - st);
   }

   private final void computeRates() {
      final double[] lam = getLambdas();
      for (int i = 0; i < genLambdas.length; i++)
         lam[i] = genLambdas[i].nextDouble ();
      setLambdas (lam);
   }

   @Override
   public void init() {
      computeRates();
      super.init();
   }
}
