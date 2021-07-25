package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeListener;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdistmulti.NegativeMultinomialDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.randvar.RandomVariateGen;

/**
 * Represents a non-homogeneous Poisson arrival process with
 * piecewise-constant arrival rates.
 * Each inter-arrival time is an exponential variate with rate $\lambda(t)$, where
 * $\lambda(t)=B\lambda_{p(t)}$ is a piecewise-constant function
 * over simulation time.
 * The function $p(t)$ gives the period corresponding to simulation time~$t$
 * whereas
 * $\lambda_p$ is the \emph{base arrival rate} for the Poisson process,
 * during period~$p$.
 * This class uses the {@link PoissonArrivalProcess} base class
 * to generate inter-arrival times and to adjust the arrival time
 * when the rate changes.
 * If a single period $p$ is simulated as if it was infinite in the model,
 * the arrival rate is fixed to $\lambda_p$.
 */
public class PiecewiseConstantPoissonArrivalProcess
   extends PoissonArrivalProcess implements PeriodChangeListener {
   private double[] lambdas;
   private PeriodChangeEvent m_pce;
   private boolean normalize = false;
   protected RandomVariateGen busyGen = null; // busyness generator for B

   /*
    * Lower limit for the variance of the gamma distribution for busyness,
    * where alpha = \texttt{s\_bgammaParam}. When the variance is smaller
    * than this limit, we replace the busyness factor by the constant 1.
    */
   protected static double varianceEpsilon;

   /**
    * Contains the parameter for the
    * gamma-distributed busyness factor given by
    * methods for parameter estimation. This is the alpha
    * parameter of the gamma distribution for busyness.
    * ATTENTION: variable de travail; utiliser tout de suite
    * apres getMLENegMulti
    */
   public static double s_bgammaParam;    // = alpha
   // number of samples in MonteCarlo experiment in subclasses
   protected static int s_numMC;


   /**
    * Sets the number of Monte Carlo samples to $n$. This is the number of
    * MC samples used in the \texttt{getMLE} method in subclasses.
    * @param n
    */
   public static void setNumMC (int n) {
   	s_numMC = n;
   }

   /**
    * Sets the number of Monte Carlo samples to $n$. This is the number of
    * MC samples used in the \texttt{getMLE} method in subclasses.
    */
   public static int getNumMC () {
   	return s_numMC;
   }

   /**
    * Constructs a new Poisson arrival process
    * with piecewise-constant arrival rates
    * instantiating new contacts using \texttt{factory}.
    * The parameter $\lambda$ is initialized with $B$\texttt{lambdas[0]},
    * and is updated at the beginning of each period with
    * a value from \texttt{lambdas}.
    * The random number stream \texttt{stream} is used to
    * generate the needed uniforms.
    * The newly-constructed arrival process is
    * added to the period-change event \texttt{pce}
    * for the arrival rate to be automatically updated.
    @param pce the period-change event associated with this object.
    @param factory the factory instantiating contacts.
    @param lambdas the base arrival rates.
    @param stream the random number generator for inter-arrival times.
    @exception IllegalArgumentException if there is not one rate
    per period.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public PiecewiseConstantPoissonArrivalProcess (PeriodChangeEvent pce,
         ContactFactory factory,
         double[] lambdas,
         RandomStream stream) {
      super (pce.simulator (), factory, lambdas[0], stream);
      if (pce.getNumPeriods() < 1)
         throw new IllegalArgumentException
            ("At least one one period must be defined");
      if (lambdas.length < pce.getNumPeriods())
         throw new IllegalArgumentException
            ("Invalid number of parameters, needs one rate per period");
      this.lambdas = lambdas.clone ();
      pce.addPeriodChangeListener (this);
      this.m_pce = pce;
      busyGen = null;
   }

   /**
    * Similar to  {@link #PiecewiseConstantPoissonArrivalProcess (PeriodChangeEvent,
    * ContactFactory, double[], RandomStream)}, but with busyness generator
    * \texttt{bgen}. It generates a busyness factor multiplying the base rate.
    @param pce the period-change event associated with this object.
    @param factory the factory instantiating contacts.
    @param lambdas the base arrival rates.
    @param stream the random number generator for inter-arrival times.
    @param bgen random number generator for busyness
    @exception IllegalArgumentException if there is not one rate
    per period.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public PiecewiseConstantPoissonArrivalProcess (PeriodChangeEvent pce,
         ContactFactory factory, double[] lambdas,
         RandomStream stream, RandomVariateGen bgen) {
      this (pce, factory, lambdas, stream);
      if (null == bgen)
         throw new IllegalArgumentException (" busyness generator is null");
      busyGen = bgen;
      setBusynessFactor(busyGen.nextDouble());
      computeRates();
   }

   /**
    * Determines if the base
    * arrival rates are normalized with period duration.
    * When normalization is enabled, for period \texttt{p},
    * the effective base arrival rate is {@link #getLambda getLambda}
    * \texttt{(p)/}{@link #getPeriodChangeEvent}\texttt{.getPeriodDuration (p)}.
    * No normalization is applied for the wrap-up period, because
    * its duration is unknown when it starts.
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
    * Returns the period-change event associated with this
    * object.
    @return the associated period-change event.
    */
   public PeriodChangeEvent getPeriodChangeEvent() {
      return m_pce;
   }

   /**
    * This method checks that the associated
    * period-change event is locked to a fixed period, and
    * calls {@link #start()} if this is the case.
    * Otherwise, it throws an unsupported-operation
    * exception since the arrival rate can
    * change with the current period.
    */
   @Override
   public void startStationary() {
      if (!m_pce.isLockedPeriod ())
         throw new UnsupportedOperationException
         ("Period-change event not locked to a fixed period");
      start();
   }

   private double getBaseArrivalRate (int p) {
      double l = lambdas[p];
      if (normalize && !m_pce.isWrapupPeriod (p)) {
         final double d = m_pce.getPeriodDuration (p);
         if (d > 0)
            l /= d;
      }
      return l;
   }

   private final void computeRates() {
   	if (null == busyGen) return;
      final double b = getBusynessFactor();
      final double[] lam = getLambdas();
      for (int i = 0; i < lam.length; i++) {
         lam[i] *= b;
      }
      setLambdas (lam);
   }

   private double getBaseArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      int p = m_pce.getPeriod (st);
      double totalRate = 0;
      while (p < m_pce.getNumPeriods () - 1 && et >= m_pce.getPeriodEndingTime (p)) {
         final double rate = getBaseArrivalRate (p);
         double s = Math.max (st, m_pce.getPeriodStartingTime (p));
         double e = Math.min (et, m_pce.getPeriodEndingTime (p));
         totalRate += (e - s) * rate;
         ++p;
      }
      if (p == m_pce.getNumPeriods () - 1) {
         final double rate = getBaseArrivalRate (p);
         totalRate += rate * (et - m_pce.getPeriodStartingTime (p));
      }
      return totalRate / (et - st);
   }

   @Override
   public double getArrivalRate (int p) {
   	return getBaseArrivalRate(p) * getBusynessFactor();
   }

   @Override
   public double getArrivalRate (double st, double et) {
    	return getBaseArrivalRate (st, et) *getBusynessFactor();
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      return getBaseArrivalRate (p);
   }

   @Override
   public double getExpectedArrivalRate (double st, double et) {
      return getBaseArrivalRate (st, et);
   }

   /**
    * Returns the current value of \texttt{lambdas}.
    @return the current base rates for the process.
    */
   public double[] getLambdas() {
      return lambdas.clone ();
   }

   /**
    * Sets the base arrival rates to \texttt{lambdas}.
    @param lambdas the new base arrival rates.
    @exception NullPointerException if the given array is \texttt{null}.
    @exception IllegalArgumentException if the length of the array
    is smaller than the number of periods.
    */
   public void setLambdas (double[] lambdas) {
      if (lambdas.length < m_pce.getNumPeriods())
         throw new IllegalArgumentException
            ("Invalid number of parameters, needs one rate for each period");
      System.arraycopy (lambdas, 0, this.lambdas, 0, lambdas.length);
   }

   @Override
   public void init() {
      super.init();
      final double lam = getBaseArrivalRate (m_pce.getCurrentPeriod());
      setLambda (lam);
   }

   @Override
   public void changePeriod (PeriodChangeEvent pce) {
      if (pce != this.m_pce)
         return;
      final int currentPeriod = pce.getCurrentPeriod();
      final double lam = getBaseArrivalRate (currentPeriod);
      setLambda (lam);
   }

   @Override
   public void stop (PeriodChangeEvent pce) {}

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      if (m_pce.getName().length() > 0)
         sb.append (", period change event: ").append (m_pce.getName());
      if (normalize)
         sb.append (", unnormalized arrival rates: {");
      else
         sb.append (", arrival rates: {");
      for (int i = 0; i < lambdas.length; i++)
         sb.append (i > 0 ? ", " : "").append (lambdas[i]);
      sb.append ("}]");
      return sb.toString();
   }

   /**
    * Estimates the parameters of a Poisson arrival process
    * with piecewise-constant arrival rate
    * from the number of arrivals in the array
    * \texttt{arrivals}, and returns an array giving
    * the estimated arrival rate for each main period.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during main period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This method estimates
    * the expected number of arrivals
    * during main period $p$, noted $\lambda_p$, independently
    * for each main period, assuming that the number of
    * arrivals in that period follows the Poisson distribution.
    * The returned array contains the estimated arrival rate
    * for each of the $P$ periods, noted
    * $\lambda_1,\ldots,\lambda_P$.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated arrival rates.
    */
   public static double[] getMLE (int[][] arrivals, int numObs, int numPeriods) {
      final double[] lambdas = new double[numPeriods];
      final int[] arrivalsp = new int[numObs];
      for (int p = 0; p < numPeriods; p++) {
         for (int i = 0; i < numObs; i++)
            arrivalsp[i] = arrivals[i][p];
         lambdas[p] = PoissonDist.getMLE (arrivalsp, numObs)[0];
      }
      return lambdas;
   }

   /**
    * Sets the lower limit for the variance of the busyness distribution.
    * When the variance would be smaller than eps, the parameter of
    * the busyness distribution is reset so that variance = eps.
    */
   public static void setVarianceEpsilon (double eps) {
   	// variance = 1/alpha <= 1/eps, where alpha is the
      // parameter of the gamma distribution for busyness.
   	varianceEpsilon = eps;
   }

   /**
    * Returns the value of \texttt{varianceEpsilon}.
    * @return the lower bound of the variance for busyness
    */
   public static double getVarianceEpsilon () {
   	return varianceEpsilon;
   }

   /**
    * Estimates the parameters of a Poisson arrival process
    * with piecewise-constant arrival rate multiplied
    * by a day-specific busyness factor following the
    * gamma$(\alpha_0,\alpha_0)$ distribution
    * from the number of arrivals in the array
    * \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals of this type on day \texttt{i}
    * during main period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This returns an array with the estimated arrival rates and stores the
    * gamma busyness parameter in {@link #s_bgammaParam}.
    * This method assumes that the number of arrivals
    * during main periods, represented by the vector
    * $A_1,\ldots,A_P$,
    * follows the negative multinomial distribution with
    *  parameters $(\alpha_0, \rho_1, \ldots, \rho_P)$
    *  where $\rho_p=\lambda_p/(\alpha_0+\sum_{k=1}^P\lambda_k)$
    *  for $p=1,\ldots,P$.
    *  After $\alpha_0,\rho_1,\ldots,\rho_P$ are estimated
    *  using maximum likelihood,
    *  arrival rate for any main period $p=1,\ldots,P$
    *  can be obtained using
    *  $\lambda_p=\alpha_0 \rho_p/\rho_0$, where
    *  $\rho_0=1-\sum_{k=1}^P \rho_k$.
    *  This method thus returns the array with $\lambda_1,\ldots,\lambda_P$.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated arrival rates.
    */
   public static double[] getMLENegMulti (int[][] arrivals, int numObs, int numPeriods) {
   	/*
      final double[] params = NegativeMultinomialDist.getMLE (arrivals, numObs, numPeriods);
      s_bgammaParam = params[0];
      double p0 = 0;
      for (int p = 1; p < params.length; p++)
         p0 += params[p];
      p0 = 1 - p0;
      final double[] lambdas = new double[numPeriods];
      for (int i = 0; i < lambdas.length; i++)
         lambdas[i] = s_bgammaParam * params[i+1] / p0;
      return lambdas;
      */

   	/*
   	 * The above commented out instructions are equivalent and give the
   	 * same results as the instructions below, but they are overcomplex.
   	 */
   	/*
   	double tem = NegativeMultinomialDist.getMLEninv (arrivals, numObs, numPeriods);
   	s_bgammaParam = 1.0 / tem;
   	*/
   	double[] tem1;
      try {
   	   tem1 = NegativeMultinomialDist.getMLE (arrivals, numObs, numPeriods);
   	   s_bgammaParam = tem1[0];
      } catch (IllegalArgumentException excep) {
         double BIG = 1.0 / getVarianceEpsilon();
         s_bgammaParam = BIG + 1;
      }
   	int i, j;
   	int[] obs = new int[numPeriods];
      for (i = 0; i < numObs; i++)
         for (j = 0; j < numPeriods; j++)
            obs[j] += arrivals[i][j];
      double[] lambdas = new double[numPeriods];
      for (i = 0; i < numPeriods; i++)
         lambdas[i] = (double) obs[i] / numObs;
      return lambdas;
   }

   /**
    * Constructs a new arrival process with arrival rates
    * estimated by the maximum likelihood method based on
    * the \texttt{numObs} observations in array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * If \texttt{withGammaBusyness}
    * is \texttt{true}, the number of arrivals
    * is considered to follow the negative multinomial
    * distribution, and the $\alpha_0$ parameter
    * for the gamma-distributed busyness factor is stored in
    * {@link #s_bgammaParam}.
    * Otherwise, the periods are considered independent,
    * and the number of arrivals during a period is
    * considered to follow the Poisson distribution.
    * The expected number of arrivals used during
    * the preliminary period
    * is equal to the expectation estimated for the first main
    * period while the arrival rate during the
    * wrap-up period is always 0.
    * @param pce the period-change event marking the end of periods.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream to generate arrival times.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @param withGammaBusyness determines if the $\alpha_0$
    * parameter is estimated in addition to the arrival rates.
    * @return the constructed arrival process.
    */
   public static PiecewiseConstantPoissonArrivalProcess getInstanceFromMLE
     (PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
           int[][] arrivals, int numObs, int numPeriods, boolean withGammaBusyness) {
      double[] lambdasmain;
      if (withGammaBusyness) {
         lambdasmain = getMLENegMulti (arrivals, numObs, numPeriods);
      } else {
         lambdasmain = getMLE (arrivals, numObs, numPeriods);
      }
      assert lambdasmain.length == numPeriods : "The number of estimated arrival rates, equal to "
         + lambdasmain.length + " should be equal to the number of periods " + numPeriods;
      final double[] lambdas = new double[lambdasmain.length + 2];
      lambdas[0] = lambdasmain[0];
      System.arraycopy (lambdasmain, 0, lambdas, 1, lambdasmain.length);
      final PiecewiseConstantPoissonArrivalProcess pap = new PiecewiseConstantPoissonArrivalProcess
         (pce, factory, lambdas, stream);
      pap.setNormalizing (true);
      return pap;
   }
}
