package umontreal.iro.lecuyer.contactcenters.contact;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.msk.params.CorrelationFit;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.randvarmulti.MultinormalCholeskyGen;
import umontreal.ssj.rng.RandomStream;
//import umontreal.ssj.rng.MRG32k3a;
//import umontreal.ssj.probdist.NegativeBinomialDist;

/**
 * Represents a doubly-stochastic Gamma-Poisson process with piecewise-constant
 * randomized correlated arrival rates. The base arrival rates $\lambda_p$ are
 * constant during each period, but they are not deterministic: for period~$p$,
 * the base rate of the Poisson process is defined as a correlated gamma random
 * variable. The marginal distribution of the rate is gamma with shape parameter
 * $\alpha_{\mathrm{G},p}$, and scale parameter $\lambda_{\mathrm{G}, p}$ (mean
 * $\alpha_{\mathrm{G},p}/\lambda_{\mathrm{G}, p}$). The correlation structure
 * is modelled using Normal copula model with positive definite correlation
 * matrix $\boldSigma$ having elements in $[-1, 1]$. If $\alpha_{\mathrm{G}, p}$
 * or $\lambda_{\mathrm{G}, p}$ are 0, the resulting arrival rate during period
 * $p$ is always 0. As with the Poisson process with deterministic arrival
 * rates, the generated base arrival rates are multiplied by a busyness factor
 * $B$ to get the arrival rates. Because the values of $\lambda(t)$ are
 * generated once for a replication, in the {@link #init} method, not calling
 * this method before the simulation starts could lead to unpredictable arrival
 * rates.
 */
public class PoissonGammaNortaRatesArrivalProcess extends
		PiecewiseConstantPoissonArrivalProcess {
	private RandomStream busynessStream;

	private double[] galphas;

	private double[] glambdas;

	private MultinormalCholeskyGen ngen;

	/**
	 * Constructs a new Poisson-gamma arrival process using \texttt{factory} to
	 * instantiate contacts. For each period \texttt{p}, the parameters of the
	 * gamma rate are given in \texttt{galphas[p]} and \texttt{glambdas[p]}. The
	 * random stream \texttt{stream} is used to generate the uniforms for the
	 * exponential times whereas the stream \texttt{busynessStream} is used to
	 * generate the gamma rates.
	 * 
	 * @param pce
	 *           the period-change event associated with this object.
	 * @param factory
	 *           the factory creating contacts for this generator.
	 * @param galphas
	 *           the $\alpha_{\mathrm{G}, p}$ parameters of the gamma variates.
	 * @param glambdas
	 *           the $\lambda_{\mathrm{G}, p}$ parameters of the gamma variates.
	 * @param CorrMtx
	 *           the correlation matrix of the Normal copula model for rates.
	 * @param stream
	 *           random number stream for the exponential variates.
	 * @param busynessStream
	 *           random number stream for the busyness factor.
	 * @exception IllegalArgumentException
	 *               if there is not one rate for each period.
	 * @exception NullPointerException
	 *               if any argument is \texttt{null}.
	 */
	public PoissonGammaNortaRatesArrivalProcess(PeriodChangeEvent pce,
			ContactFactory factory, double[] galphas, double[] glambdas,
			double[][] CorrMtx, RandomStream stream, RandomStream busynessStream) {
		super(pce, factory, new double[glambdas.length], stream);
		if (busynessStream == null)
			throw new NullPointerException(
					"The given random stream for busyness must not be null");
		this.busynessStream = busynessStream;
		if (galphas.length != glambdas.length
				|| galphas.length < pce.getNumPeriods())
			throw new IllegalArgumentException("Invalid number of "
					+ "parameters, needs one parameter for each period");
		DoubleMatrix2D sigma = new DenseDoubleMatrix2D(CorrMtx);
		if (sigma.rows() != galphas.length || sigma.columns() != galphas.length)
			throw new IllegalArgumentException(
					"The dimensions of the correlation matrix for the NORTA-driven arrival process must be PxP, where P is the number of main periods");
		this.galphas = galphas;
		this.glambdas = glambdas;
		final NormalGen ngen1 = new NormalGen(stream, new NormalDist());
		ngen = new MultinormalCholeskyGen(ngen1, new double[galphas.length],
				sigma);
	}

	/**
	 * Returns the parameters $\alpha_{\mathrm{G}, p}$ for the gamma rates.
	 * 
	 * @return the $\alpha_{\mathrm{G}, p}$ parameters for this object.
	 */
	public double[] getGammaAlphas() {
		return galphas;
	}

	/**
	 * Returns the $\lambda_{p}$ parameters for the arrivals rates.
	 * 
	 * @return the $\lambda_{p}$ parameters.
	 */
	public double[] getGammaLambdas() {
		return glambdas.clone();
	}

	/**
	 * Sets the $\alpha_{\mathrm{G}, p}$ and $\lambda_{p}$ parameters for the
	 * gamma arrival rates to \texttt{galphas} and \texttt{glambdas},
	 * respectively.
	 * 
	 * @param galphas
	 *           the new $\alpha_{\mathrm{G}, p}$ parameters.
	 * @param glambdas
	 *           the new $\lambda_{p}$ parameters.
	 * @exception NullPointerException
	 *               if the given arrays are \texttt{null}.
	 * @exception IllegalArgumentException
	 *               if the length of the given arrays does not correspond to at
	 *               least the number of periods.
	 */
	public void setGammaParams(double[] galphas, double[] glambdas) {
		if (galphas.length != glambdas.length
				|| galphas.length < getPeriodChangeEvent().getNumPeriods())
			throw new IllegalArgumentException(
					"Invalid length of galphas or glambdas");
		this.galphas = galphas;
		this.glambdas = glambdas;
	}

	/**
	 * Returns the correlation matrix associated with this arrival process.
	 * 
	 * @return the associated correlation matrix.
	 */
	public double[][] getSigma() {
		return ngen.getSigma().toArray();
	}

	/**
	 * Sets the associated correlation matrix to \texttt{CorrMtx}.
	 * 
	 * @param CorrMtx
	 *           the new sigma correlation matrix.
	 * @exception NullPointerException
	 *               if \texttt{CorrMtx} is \texttt{null}.
	 * @exception IllegalArgumentException
	 *               if \texttt{CorrMtx} is not a $P\times P$ symmetric and
	 *               positive-definite matrix.
	 */
	public void setSigma(double[][] CorrMtx) {
		DoubleMatrix2D sigma = new DenseDoubleMatrix2D(CorrMtx);
		ngen.setSigma(sigma);
	}

	/**
	 * Returns the random stream used to generate the busyness factors for the
	 * Poisson arrival process.
	 * 
	 * @return the random stream for the values of $\lambda_p$.
	 */
	public RandomStream getBusynessStream() {
		return busynessStream;
	}

	/**
	 * Changes the random stream used to generate the busyness factors for the
	 * Poisson arrival process.
	 * 
	 * @param busynessStream
	 *           random number generator for the $\lambda_p$ values.
	 * @exception NullPointerException
	 *               if the parameter is \texttt{null}.
	 */
	public void setBusynessStream(RandomStream busynessStream) {
		if (busynessStream == null)
			throw new NullPointerException(
					"The given random stream for rates must not be null");
		this.busynessStream = busynessStream;
	}

	@Override
	public double getExpectedArrivalRate(int p) {
		double l = galphas[p] == 0 || glambdas[p] == 0 ? 0 : glambdas[p];
		if (isNormalizing() && !getPeriodChangeEvent().isWrapupPeriod(p)) {
			final double d = getPeriodChangeEvent().getPeriodDuration(p);
			if (d > 0)
				l /= d;
		}
		return l;
	}

	@Override
	public double getExpectedArrivalRate(double st, double et) {
		if (et <= st)
			return 0;
		final PeriodChangeEvent pce = getPeriodChangeEvent();
		int p = pce.getPeriod(st);
		double totalRate = 0;
		while (p < pce.getNumPeriods() - 1 && et >= pce.getPeriodEndingTime(p)) {
			final double rate = getExpectedArrivalRate(p);
			double s = Math.max(st, pce.getPeriodStartingTime(p));
			double e = Math.min(et, pce.getPeriodEndingTime(p));
			totalRate += (e - s) * rate;
			++p;
		}
		if (p == pce.getNumPeriods() - 1) {
			final double rate = getExpectedArrivalRate(p);
			totalRate += rate * (et - pce.getPeriodStartingTime(p));
		}
		return totalRate / (et - st);
	}

	private final void computeRates() {
		final double[] lam = getLambdas();
		GammaDist gamDist;
		double u;

		ngen.nextPoint(lam);
		for (int p = 0; p < lam.length; p++) {
			u = NormalDist.cdf01(lam[p]);
			if ((galphas[p] == 0) || (glambdas[p] == 0)) {
				lam[p] = 0;
			} else {
				gamDist = new GammaDist(galphas[p], galphas[p]);
				lam[p] = glambdas[p] * gamDist.inverseF(u);
			}

		}
		setLambdas(lam);
	}

	/*
	 * public static double[] computeRates(double[] galphas, double[] glambdas,
	 * double[][] CorrMtx) { final double[] lam = new double[galphas.length];
	 * GammaDist gamDist; DoubleMatrix2D sigma; MultinormalCholeskyGen ngenLoc;
	 * NegativeBinomialDist NbDist;
	 * 
	 * RandomStream stream = new MRG32k3a();
	 * 
	 * sigma = new DenseDoubleMatrix2D(CorrMtx); final NormalGen ngen1 = new
	 * NormalGen(stream, new NormalDist()); ngenLoc = new MultinormalCholeskyGen
	 * (ngen1, new double[galphas.length], sigma); ngenLoc.nextPoint (lam);
	 * 
	 * for (int p = 0; p < lam.length; p++){ lam[p] = NormalDist.cdf01 (lam[p]);
	 * gamDist = new GammaDist(galphas[p], glambdas[p]); NbDist = new
	 * NegativeBinomialDist(galphas[p], glambdas[p]/(1+glambdas[p])); lam[p] =
	 * galphas[p] == 0 || glambdas[p] == 0 ? 0 : gamDist.inverseF(lam[p]); }
	 * 
	 * return lam; }
	 */

	@Override
	public void init() {
		computeRates();
		super.init();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		sb.deleteCharAt(sb.length() - 1);
		sb.append(", gamma parameters: {");
		for (int i = 0; i < galphas.length; i++)
			sb.append(i > 0 ? ", " : "").append('{').append(galphas[i])
					.append(", ").append(glambdas[i]).append('}');
		sb.append("}]");
		return sb.toString();
	}

	/*
	 * Computes correlation matrix and returns it in \texttt{corr}.
	 */
	private static void getCorrMatrix(GammaParameterEstimator gpe,
			int numPeriods, CorrelationFit fit, double[][] corr) {
		final int n = numPeriods;

		switch (fit) {
		case FULLCORRELATION:
			double[][] corest = gpe.getNortaRateGaussCorrCorrected();
			for (int i = 0; i < n; ++i) {
				for (int j = 0; j < n; ++j) {
					corr[i][j] = corest[i][j];
				}
			}
			break;

		case MARKOVSINGLERHO:
			double br = gpe.getNortaRateGaussCorrFitMarkovSingleRho();
			for (int i = 0; i < n; ++i) {
				for (int j = 0; j < n; ++j) {
					corr[i][j] = Math.pow(br, Math.abs(i - j));
				}
				corr[i][i] = 1.0;
			}
			break;

		case MARKOVLINEARFIT:
			double[] T = gpe.getNortaRateGaussCorrFitGeneralLinear();
			final double a = T[0];
			final double b = T[1];
			final double c = T[2];
			double r;
			for (int i = 0; i < n; ++i) {
				for (int j = 0; j < n; ++j) {
					r = Math.pow(b, Math.abs(i - j));
					corr[i][j] = a * r + c;
				}
				corr[i][i] = 1.0;
			}
			break;

		default:
			throw new UnsupportedOperationException(
					"no such correlation fit algorithm");
		}
	}

	/**
	 * Estimates the parameters of a Poisson-gamma-norta-rates arrival process
	 * from the number of arrivals in the array \texttt{arrivals}. Element
	 * \texttt{arrivals[i][p]} corresponds to the number of arrivals on day
	 * \texttt{i} during period \texttt{p}, where $i=0,\ldots,n-1$, and
	 * $p=0,\ldots,P-1$, with $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
	 * This method estimates and returns the parameters of the gamma distribution
	 * $\alpha_{\mathrm{G}, p}$ and the average rate $\lambda_{p}$ for each
	 * period. The returned array contains $(\alpha_{\mathrm{G}, 0}, \lambda_{0},
	 * \ldots, \alpha_{\mathrm{G}, P-1}, \lambda_{P-1})$. It also estimates the
	 * correlation matrix using algorithm \texttt{fit}, and returns it in
	 * \texttt{corr}. The memory for the \texttt{numPeriods} x
	 * \texttt{numPeriods} elements of matrix \texttt{corr} must be reserved
	 * outside this method before calling it.
	 * 
	 * @param arrivals
	 *           the number of arrivals during each day and period.
	 * @param numObs
	 *           the number of days.
	 * @param numPeriods
	 *           the number of periods.
	 * @param numMC
	 *           the number of MonteCarlo samples used in the estimation.
	 * @param fit
	 *           type of fit used to compute the correlation matrix.
	 * @param corr
	 *           the estimated correlation matrix is returned in \texttt{corr}.
	 * @return the estimated gamma and lambda parameters of this process.
	 */
	public static double[] getMLE(int[][] arrivals, int numObs, int numPeriods,
			int numMC, CorrelationFit fit, double[][] corr) {
		GammaParameterEstimator gpe = new GammaParameterEstimator(arrivals,
				numObs, numPeriods, numMC);
		gpe.initNortaStochRootFinding();
		gpe.estimateNortaRateParamsStochasticRootFinding();
		getCorrMatrix(gpe, numPeriods, fit, corr);

		double[] alp = gpe.getNortaRateGammaShape();
		double[] lam = gpe.getNortaRateGammaScale();
		double[] gammaParams = new double[2 * numPeriods];
		for (int p = 0; p < numPeriods; p++) {
			gammaParams[2 * p] = alp[p];
			// In the estimator gpe, we use the other model where the mean rate
			// is alpha/lambda1; here the rate is lambda2 = alpha/lambda1
			gammaParams[2 * p + 1] = alp[p] / lam[p];
		}

		return gammaParams;
	}

	/**
	 * Constructs a new arrival process with gamma arrival rates estimated by the
	 * maximum likelihood method based on the \texttt{numObs} observations in
	 * array \texttt{arrivals}. Element \texttt{arrivals[i][p]} corresponds to
	 * the number of arrivals on day \texttt{i} during period \texttt{p}, where
	 * $i=0,\ldots,n-1$, $p=0,\ldots,P-1$, $n=$~\texttt{numObs}, and
	 * $P=$~\texttt{numPeriods}. The parameters of the gamma-distributed arrival
	 * rates during the main periods are estimated using
	 * {@link #getMLE(int[][],int,int,int,CorrelationFit,double[][])}. For
	 * the preliminary period, the parameters of the first main period are used.
	 * For the wrap-up periods, both parameters are set to 0; as a result, the
	 * arrival rate is always 0 during the wrap-up period.
	 * 
	 * @param pce
	 *           the period-change event marking the end of periods.
	 * @param factory
	 *           the contact factory used to create contacts.
	 * @param stream
	 *           the random stream to generate arrival times.
	 * @param busynessStreams
	 *           the random stream to generate busyness factors.
	 * @param arrivals
	 *           the number of arrivals.
	 * @param numObs
	 *           the number of days.
	 * @param numPeriods
	 *           the number of periods.
	 * @param numMC
	 *           the number of MonteCarlo samples used in the estimation.
	 * @param fit
	 *           type of fit used to compute the correlation matrix.
	 * @return the constructed arrival process.
	 */

	public static PoissonGammaNortaRatesArrivalProcess getInstanceFromMLE(
			PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
			RandomStream busynessStreams, int[][] arrivals, int numObs,
			int numPeriods, int numMC, CorrelationFit fit) {
		double[][] corr = new double[numPeriods][numPeriods];
		final double[] gammaParams = getMLE(arrivals, numObs, numPeriods, numMC,
				fit, corr);
		final double[] galphas = new double[gammaParams.length / 2 + 2];
		final double[] glambdas = new double[galphas.length];
		for (int p = 1; p < galphas.length - 1; p++) {
			galphas[p] = gammaParams[(p - 1) * 2];
			glambdas[p] = gammaParams[(p - 1) * 2 + 1];
		}
		galphas[0] = galphas[1];
		glambdas[0] = glambdas[1];
		final PoissonGammaNortaRatesArrivalProcess pap = new PoissonGammaNortaRatesArrivalProcess(
				pce, factory, galphas, glambdas, corr, stream, busynessStreams);
		pap.setNormalizing(true);
		return pap;
	}

}
