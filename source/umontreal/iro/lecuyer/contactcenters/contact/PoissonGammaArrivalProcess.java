package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.probdist.NegativeBinomialDist;
import umontreal.ssj.randvar.GammaGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.GammaShapeEstimatorType;


/**
 * Represents a doubly-stochastic Poisson process with piecewise-constant
 * randomized arrival rates \cite{tJON00a}.
 * The base arrival rates $\lambda_p$ are constant during each period,
 * but they are not deterministic:
 * for period~$p$, the base rate of the Poisson
 * process is defined as $\lambda_p$ times a gamma random variable with
 * shape and scale parameters $\alpha_{\mathrm{G},p}$, and mean 1.
 * However, if $\alpha_{\mathrm{G}, p}$ or $\lambda_{p}$
 * are 0, the resulting arrival rate during period $p$ is always set to 0.
 * As with the Poisson process with deterministic arrival rates,
 * the generated base arrival rates are multiplied by a global busyness
 * factor $B$ for the day, and also by a busyness factor $B_p$ specific to
 * each period of the day in order to get the arrival rates. 
 * Because the values of $\lambda(t)$ are generated once for a replication,
 * in the {@link #init} method, not calling this method
 * before the simulation starts could lead to unpredictable
 * arrival rates.
 */
public class PoissonGammaArrivalProcess extends PiecewiseConstantPoissonArrivalProcess {
   private RandomStream streamBusyness;  // busyness generator for B_j
   private double[] galphas;
   private double[] glambdas;

   /**
    * Constructs a new Poisson-gamma arrival process
    * using \texttt{factory} to instantiate contacts.
    * For each period \texttt{p}, the parameters of the gamma
    * rate are given in \texttt{galphas[p]} and \texttt{glambdas[p]}.
    * The random stream \texttt{stream} is used
    * to generate the uniforms for the exponential times whereas
    * the stream \texttt{streamBusyness} is used
    * to generate the busyness factors for each period of the day.
    @param pce the period-change event associated with this object.
    @param factory the factory creating contacts for this generator.
    @param galphas the $\alpha_{\mathrm{G}, p}$ parameters for the gamma variates for busyness.
    @param glambdas the $\lambda_{p}$ arrival rates.
    @param stream random number stream for the exponential variates.
    @param streamBusyness random number stream for the gamma rate values.
    @exception IllegalArgumentException if there is not one rate
    for each period.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public PoissonGammaArrivalProcess (PeriodChangeEvent pce,
                                      ContactFactory factory,
                                      double[] galphas,
                                      double[] glambdas,
                                      RandomStream stream,
                                      RandomStream streamBusyness) {
      super (pce, factory, new double[glambdas.length], stream);
      if (streamBusyness == null)
         throw new NullPointerException ("The given random stream for rates must not be null");
      this.streamBusyness = streamBusyness;
      if (galphas.length != glambdas.length || galphas.length <
          pce.getNumPeriods())
         throw new IllegalArgumentException ("Invalid number of " +
            "parameters, needs one parameter for each period");
      this.galphas = galphas;
      this.glambdas = glambdas;
   }

   public PoissonGammaArrivalProcess (PeriodChangeEvent pce,
         ContactFactory factory,
         double[] galphas,
         double[] glambdas,
         RandomStream stream,
         RandomStream streamBusyness, RandomVariateGen bgen) {
   	this (pce, factory, galphas, glambdas, stream, streamBusyness);
   	busyGen = bgen;
      setBusynessFactor(busyGen.nextDouble());
    //  computeRates();
   }
   /**
    * Returns the parameters $\alpha_{\mathrm{G}, p}$ of the
    * gamma distribution for busyness.
    @return the $\alpha_{\mathrm{G}, p}$ parameters.
    */
   public double[] getGammaAlphas() {
      return galphas;
   }

   /**
    * Returns the $\lambda_{p}$ parameters for the rates.
    @return the $\lambda_{p}$ parameters.
    */
   public double[] getGammaLambdas() {
      return glambdas.clone();
   }

   /**
    * Sets the $\alpha_{\mathrm{G}, p}$ and $\lambda_{p}$
    * parameters for the busyness and the arrival
    * rates to \texttt{galphas} and \texttt{glambdas}, respectively.
    @param galphas the new $\alpha_{\mathrm{G}, p}$ parameters.
    @param glambdas the new $\lambda_{p}$ rates.
    @exception NullPointerException if the given arrays are \texttt{null}.
    @exception IllegalArgumentException if the length of the given
    arrays does not correspond to at least the number of periods.
    */
   public void setGammaParams (double[] galphas, double[] glambdas) {
      if (galphas.length != glambdas.length ||
          galphas.length < getPeriodChangeEvent().getNumPeriods())
         throw new IllegalArgumentException
            ("Invalid length of galphas or glambdas");
      this.galphas = galphas;
      this.glambdas = glambdas;
   }

   /**
    * Returns the random stream used to generate
    * the busyness factors for this arrival process.
    @return the random stream for the values of the busyness factors.
    */
   public RandomStream getBusynessStream() {
      return streamBusyness;
   }

   /**
    * Changes the random stream used to generate
    * the busyness factors for this arrival process.
    @param streamBusyness random number stream for the busyness factors.
    @exception NullPointerException if the parameter is \texttt{null}.
    */
   public void setBusynessStream (RandomStream streamBusyness) {
      if (streamBusyness == null)
         throw new NullPointerException ("The given random stream for rates must not be null");
      this.streamBusyness = streamBusyness;
   }

   /**
    * 
    */
   @Override
   public double getExpectedArrivalRate (int p) {
      double l = galphas[p] == 0 || glambdas[p] == 0 ? 0 : glambdas[p];
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

   /**
    * Computes the arrival rate with the random busyness level of each period.
    */
   protected void computeRates() {
      final double[] lam = getLambdas();
      double b;
   	if (null == busyGen)
   		b = 1;
   	else
   		b = getBusynessFactor();
      for (int i = 0; i < galphas.length; i++) {
         lam[i] = galphas[i] == 0 || glambdas[i] == 0 ? 0 :
         	b * glambdas[i] * GammaGen.nextDouble (streamBusyness, galphas[i], galphas[i]);
      }
      setLambdas (lam);
   }

   @Override
   public void init() {
      computeRates();
      super.init();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      sb.append (", busyness and rates parameters: {");
      for (int i = 0; i < galphas.length; i++)
         sb.append (i > 0 ? ", " : "").append
            ('{').append (galphas[i]).append (", ").append
            (glambdas[i]).append ('}');
      sb.append ("}]");
      return sb.toString();
   }

   /**
    * Estimates the parameters of a Poisson-gamma arrival process
    * from the number of arrivals in the array
    * \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, and $p=0,\ldots,P-1$, with
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This method estimates $\alpha_{\mathrm{G}, p}$ and
    * $\lambda_{p}$ independently
    * for each period, assuming that the number of
    * arrivals in that period follows the negative binomial distribution
    * with first parameter $\alpha_{\mathrm{G}, p}$.
    * The returned array contains
    * $(\alpha_{\mathrm{G}, 0},
    * \lambda_{0}, \ldots,
    * \alpha_{\mathrm{G}, P-1},
    * \lambda_{P-1})$.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated $\alpha_j$ and $\lambda_j$ parameters.
    */
   public static double[] getMLE (int[][] arrivals, int numObs, int numPeriods) {
      final double[] gammaParams = new double[2*numPeriods];
      final int[] arrivalsp = new int[numObs];
      double sum, mean;
      double[] tema;
      double BIG = 1.0 / PiecewiseConstantPoissonArrivalProcess.getVarianceEpsilon();
      
      for (int p = 0; p < numPeriods; p++) {
         sum = 0;
         for (int i = 0; i < numObs; i++) {
            arrivalsp[i] = arrivals[i][p];
            sum += arrivalsp[i];
         }
         mean = sum / numObs;

         try {
         	// tema contains {r, p}
            tema = NegativeBinomialDist.getMLE (arrivalsp, numObs);
            // r = alpha for Gamma(alpha, alpha) busyness
            gammaParams[2*p] = tema[0];
            gammaParams[2*p + 1] = mean;
            if (gammaParams[2*p] >= BIG) {
            	// set limit on alpha --> lower limit on variance
              	gammaParams[2*p] = BIG + 1.;
            	gammaParams[2*p+1] = mean;            	
            }
         } catch (UnsupportedOperationException exc) {
            // when mean >= variance, the MLE has no finite maximum;
         	gammaParams[2*p] = BIG + 1.;
         	gammaParams[2*p+1] = mean;
         }
      }
      return gammaParams;
   }

   /**
    * Constructs a new arrival process with gamma arrival rates
    * estimated by the maximum likelihood method based on
    * the \texttt{numObs} observations in array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$, with
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * The parameters of the gamma-distributed arrival rates
    * during the main periods
    * are estimated using {@link #getMLE(int[][],int,int)}.
    * For the preliminary period, the parameters of the first main
    * period are used.  For the wrap-up periods,
    * both parameters are set to 0; as a result,
    * the arrival rate is always 0 during the wrap-up period.
    * @param pce the period-change event marking the end of periods.
    * @param factory the contact factory used to create contacts.
    * @param stream random stream to generate arrival times.
    * @param streamBusyness random stream to generate busyness factors.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the constructed arrival process.
    */
   public static PoissonGammaArrivalProcess getInstanceFromMLE
     (PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
           RandomStream streamBusyness,
           int[][] arrivals, int numObs, int numPeriods) {
      final double[] gammaParams = getMLE (arrivals, numObs, numPeriods);
      final double[] galphas = new double[gammaParams.length/2 + 2];
      final double[] glambdas = new double[galphas.length];
      for (int p = 1; p < galphas.length - 1; p++) {
         galphas[p] = gammaParams[(p-1)*2];
         glambdas[p] = gammaParams[(p-1)*2 + 1];
         // Pierre dit qu'il faut prendre
         //  glambdas[p] = galphas[p] * gammaParams[(p-1)*2 + 1];
         // mais le mult galphas[p] est deja inclus dans 
      }
      galphas[0] = galphas[1];
      glambdas[0] = glambdas[1];
      final PoissonGammaArrivalProcess pap = 
      	new PoissonGammaArrivalProcess (pce, factory, galphas, glambdas, stream, streamBusyness);
      pap.setNormalizing (true);
      return pap;
   }
 
   /**
    * Estimates the parameters of a Poisson-gamma arrival process
    * for the case of a global busyness factor for the day, and
    * specific busyness factors for each period of the day,
    * from the number of arrivals in the array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals
    * on day \texttt{i} during period \texttt{p}, where $i=0,\ldots,n-1$,
    * and $p=0,\ldots,P-1$, with $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This method estimates and returns the parameters of the gamma
    * distribution $\alpha_{\mathrm{G}, p}$ and the average rate $\lambda_{p}$
    * for each period.  The returned array contains
    * $(\alpha_{\mathrm{G}, 0}, \lambda_{0}, \ldots,
    * \alpha_{\mathrm{G}, P-1}, \lambda_{P-1})$.
    * The global busyness factor is also estimated. 
    * This is the case where the arrivals are determined from $B*B_j*\lambda_j$.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @param numMC the number of MonteCarlo samples used in the estimation.
    * @param arrPar other parameters of the arrival process.
    * @return the estimated gamma and lambda parameters of this process.
    */
   public static double[] getMLEBB (int[][] arrivals, int numObs, int numPeriods,
                                    int numMC, ArrivalProcessParams arrPar) {
   	int maxit = -1;
      int movWindSize = -1;
      double smoothLambda = -1;
   	if (null != arrPar) {
   	   maxit = arrPar.getMaxIter();
         movWindSize = arrPar.getMovingWindowSize(); 
         smoothLambda = arrPar.getGammaShapeSmoothingFactor();
   	}
   	GammaParameterEstimator gpe = new GammaParameterEstimator(
   			arrivals, numObs, numPeriods, numMC);
   	if (null != arrPar) {
   	   gpe.setMaxIter(maxit);
   	   gpe.setMovingWindowSize(movWindSize);
   	   gpe.setSmoothingLambda(smoothLambda);
   	}
   	
      double[] bbParams = null;
      if (GammaShapeEstimatorType.SINGLESHAPE == arrPar.getGammaShapeEstimatorType()) {
         bbParams = gpe.getMLEdoublyGamma ();
      } else {
         bbParams = gpe.getMLEdoublyGammaSpline ();
      }
     
      // global busyness for the day = alpha0
      double alpha0 = bbParams[bbParams.length-1];
    	s_bgammaParam = alpha0;
      double[] gammaParams = new double[2*numPeriods];
      for (int p = 0; p < numPeriods; p++) {
         gammaParams[2*p] = bbParams[p];   // alpha
         gammaParams[2*p + 1] = bbParams[numPeriods + p]; // lambda
      }
      return gammaParams;      
   }

}
