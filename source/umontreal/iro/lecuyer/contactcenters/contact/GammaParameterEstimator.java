package umontreal.iro.lecuyer.contactcenters.contact;

// import umontreal.ssj.randvar.GammaGen;
import umontreal.ssj.randvar.GammaAcceptanceRejectionGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.util.Num;
import umontreal.ssj.probdist.NegativeBinomialDist;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.randvarmulti.MultinormalCholeskyGen;
// import umontreal.ssj.probdistmulti.norta.*;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.PoissonGen;
// package umontreal.iro.lecuyer.contactcenters.contact.CorrelationMatrixCorrector;

/**
 * This class implements the parameter estimation for the doubly Gamma-Poisson process.
 * The rate $T_{i,j}$ of this process consists of three multiplicative contributions: (i)
 * the deterministic piece-wise constant rate $\lambda_i$, (ii) the busyness factor for
 * the day, $\beta_j$, (iii) the business factor for the sub-period of the day, $B_{i,j}$.
 * The input data are counts observed for $I$ sub-periods of the day during $J$ days.
 * We assume that the rate follows $T_{i,j} = \lambda_i \beta_j B_{i,j}, \forall i=1\ldots I, j = 1\ldots J$
 * and input data $Y_{i,j} \sim \textrm{Poisson}(T_{i,j})$ follow the Poisson distribution
 * conditional on the rates. The busyness factor for the day follows Gamma distribution with
 * parameters $Q$ and $Q$. The busyness factor follows the Gamma distribution with parameters
 * $R$ and $R$. The busyness factors are assumed to be independent across
 * days and sub-intervals of the day. The class implements two estimators of the process parameters:
 * the Moment Matching Estimator (MME) in the method {@link #getMMEdoublyGamma} and the Maximum
 * Likelihood Estimator (MLE) in the method {@link #getMLEdoublyGamma}. The MME is a
 * suboptimal, but simple and fast estimator based on matching the theoretical means, variances
 * and covariances of the process with their empirical counterparts. The MLE is a statistically
 * optimal estimator with greater accuracy than the MME, but it is less computationally efficient.
 * The MLE is implemented using the stochastic trust-region Gauss-Newton algorithm. The MLE estimator first calls the MME estimator
 * and uses it as a starting point for the optimization. The {@link #startGradUncTrustRegion} method uses
 * common random numbers to track changes in the cost function and call of this method with option
 * "CostOnly" must, in general, be always preceded by the call of this method without this option
 * in order to provide meaningful results.
 *
 * @author Boris N. Oreshkin
 *
 */
public class GammaParameterEstimator {

   private int[][] arrivals; // Input data
   private int numObs; // Number of observations
   private int numPeriods; // Number of sub-periods in the day
   // These are for use with the doubly Gamma MLE
   private RandomStream GammaGenStream; // Random stream for the Gamma generator
   private RandomStream GammaGenStreamAux; // Auxiliary random stream for the Gamma generator
   private int numSamples; // Number of samples used in MC integration
   private double[][] xi; // MC samples
   private double[][] xi_weights; // MC weights used for common random number cost function evaluation
   // These are for the use with  doubly Gamma spline MLE
   private DoubleMatrix2D QRQ; // The spline matrices
   private double smoothingLambda; // the smoothing parameter for the smoothing spline
   // These are for the use with doubly Gamma MME
   private int movWindSize; // The size of the moving window to average the MME estimator of Gamma shape parameter
   // These are for use with GradUncTrustRegion optimizer
   private double Rinit; // Initial trust region size
   private double tol; // tolerance
   private int maxit; // maximum number of iterations
   private double g1; // trust region increase multiplier
   private double g0; // trust region decrease multiplier
   private double c1; // trust region quality upper boundary
   private double c0; // trust region quality lower boundary
   private double eta; // Noise attenuator
   private double pwr; // The power in the step size annealing sequence
   public double x[][]; // Optimization trace
   // These are for the use with the NORTA stochastic root finding
   private RandomStream GaussGenStream; // Random stream for the Gaussian generator
   private RandomStream PoissonGenStream;
   private double[][] yGaussCorr;
   private double[] Qout;
   private double[] LamOut;

   /**
    * Constructs a new estimator object with a given set of input data
    * @param data the matrix of input data $Y_{i,j}$ with $N$ rows corresponding to $N$
    * observations and $P$ columns corresponding to $P$ sub-periods in the day.
    * @param N the number of observations
    * @param P the number of sub-periods in the day
    */
   public GammaParameterEstimator(int[][] data, int N, int P) {
      numPeriods = P;
      numObs = N;
      arrivals = new int[numObs][numPeriods];
      for (int i = 0; i < numPeriods; i++) {
         for (int j = 0; j < numObs; j++) {
            arrivals[j][i] = data[j][i];
         }
      }

   }
   /**
    * Constructs a new estimator object with a given set of input data and default
    * seed for the Gamma random variable generators.
    * @param data the matrix of input data $Y_{i,j}$ with $N$ rows corresponding to $N$
    * observations and $P$ columns corresponding to $P$ sub-periods in the day.
    * @param N the number of observations
    * @param P the number of sub-periods in the day
    * @param M the number of Monte-Carlo samples used in the evaluation of
    * stochastic derivatives and the cost function
    */
   public GammaParameterEstimator(int[][] data, int N, int P, int M) {
     	this (data, N, P);
      numSamples = M;
      
      xi = new double[M][N];
      xi_weights = new double[M][N];
      GammaGenStream = new MRG32k3a();
      GammaGenStreamAux = new MRG32k3a();

      c0 = 0.01;
      c1 = 0.5;
      g0 = 1 / (1.1 * 1.1);
      g1 = 1.1;
      maxit = 200;
      tol = 1e-9;
      eta = 0.5;
      pwr = 5.0 / 6.0;
      Rinit = 0.5;
      
      smoothingLambda = 0.95;
      movWindSize = 5; 
  }

   /**
    * Constructs a new estimator object with a given set of input data and a user defined
    * seed for the Gamma random variable generators.
    * @param data the matrix of input data $Y_{i,j}$ with $N$ rows corresponding to $N$
    * observations and $P$ columns corresponding to $P$ sub-periods in the day.
    * @param N the number of observations
    * @param P the number of sub-periods in the day
    * @param M the number of Monte-Carlo samples used in the evaluation of
    * stochastic derivatives and the cost function
    * @param Seed is the vector of 6 integers. The first 3 values of the seed
    * must all be less than $m_1 = 4294967087$, and not all 0; and the last 3
    * values must all be less than $m_2 = 4294944443$, and not all 0.
    */
   public GammaParameterEstimator(int[][] data, int N, int P, int M, long[] Seed) {
   	this (data, N, P, M);
      MRG32k3a.setPackageSeed(Seed);
      GammaGenStream = new MRG32k3a();
      GammaGenStreamAux = new MRG32k3a();
   }

   public void initNortaStochRootFinding(long[] Seed) {
      MRG32k3a.setPackageSeed(Seed);
      initNortaStochRootFinding( );
   }

   public void initNortaStochRootFinding( ) {
      GaussGenStream = new MRG32k3a();
      PoissonGenStream = new MRG32k3a();

      eta = 0.5;
      pwr = 9.0 / 16.0;
      maxit = 1000;
      tol = 1e-9;

      yGaussCorr = new double[numPeriods][numPeriods];
      Qout = new double[numPeriods];
      LamOut = new double[numPeriods];
   }




   /**
   * Estimates the parameters of a doubly Gamma Poisson-Gamma arrival process
   * that has both busyness factor for the day and the busyness factor for the
   * sub-period of the day, both following the Gamma distribution, from
   * the number of arrivals in the array \texttt{arrivals} using method of moments. The day-specific
   * busyness factor follows the Gamma$(Q,Q)$ distribution, the sub-period-specific
   * busyness factor follows the gamma$(R,R)$ distribution.
   * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
   * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,n-1$, and $p=0,\ldots,P-1$,
   * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
   * If we follow the notation introduced for PoissonGammaArrivalProcess, then this method
   * estimates $\alpha_{\mathrm{G}, p}$, $\lambda_{\mathrm{G}, p}$  and the daily gamma
   * busyness parameter. It is assumed that $\alpha_{\mathrm{G}, p}$ is a vector of
   * distinct values while all the entries
   * of $\lambda_{\mathrm{G}, p}$ are the same and equal to $R$. The estimation is based on
   * matching the empirical first and second order moments of the distribution
   * of counts (mean, variance and covariance) with the analytical moments of the
   * doubly Gamma Poisson-Gamma arrival process distribution.
   * The returned array of $2P+1$ elements contains
   * $(\alpha_{\mathrm{G}, 0},
   * \lambda_{\mathrm{G}, 0}, \ldots,
   * \alpha_{\mathrm{G}, P-1},
   * \lambda_{\mathrm{G}, P-1}), \beta_0$.
   * @return the estimated gamma parameters.
   */
   public double[] getMMEdoublyGamma ( ) {
      final double[] gammaParams = new double[2 * numPeriods + 1];

      final double[] empMean = new double[numPeriods];
      final double[] empVar = new double[numPeriods];
      final double[][] empCov = new double[numPeriods][numPeriods];
      double MuSum = 0;
      double RSum = 0;
      double Qhat;
      double Rhat;
      double Mu2Sum = 0;
      double VarSum = 0;
      double MeanSum = 0;

      // Calculate the empirical moments
      for (int i = 0; i < numPeriods; i++) {
         empMean[i] = 0;
         empVar[i] = 0;
         for (int j = 0; j < numPeriods; j++) {
            empCov[i][j] = 0;
         }
      }
      for (int i = 0; i < numPeriods; i++) {
         for (int j = 0; j < numObs; j++) {
            empMean[i] += arrivals[j][i];
         }
         empMean[i] /= numObs;
      }
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            for (int j = 0; j < numObs; j++) {
               empCov[i][k] += (arrivals[j][i] - empMean[i]) * (arrivals[j][k] - empMean[k]);
            }
            empCov[i][k] /= numObs;
         }
         empVar[k] = empCov[k][k];
      }
      // Calculate sums of the empirical moments
      for (int i = 0; i < numPeriods - 1; i++) {
         for (int j = i + 1; j < numPeriods; j++) {
            RSum += empCov[j][i];
            MuSum += empMean[j] * empMean[i];
         }
      }
      for (int i = 0; i < numPeriods; i++) {
         Mu2Sum += empMean[i] * empMean[i];
         VarSum += empVar[i];
         MeanSum += empMean[i];
      }
      // Calculate the estimators of Gamma parameters
      Qhat = MuSum / Math.abs(RSum);
      if ( VarSum*Qhat - Mu2Sum - MeanSum*Qhat <= 0 ) {
         Rhat = 10 * Qhat;
      } else {
         Rhat = (1 + Qhat) * Mu2Sum / (VarSum * Qhat - Mu2Sum - MeanSum * Qhat);
      }

      // Set the outputs
      for (int i = 0; i < numPeriods; i++) {
         gammaParams[2*i] = empMean[i];
         gammaParams[2*i + 1] = Rhat;
      }
      gammaParams[2*numPeriods] = Qhat;
      // bgammaParam = Qhat;

      return gammaParams;
   }

   /**
   * Estimates the parameters of a doubly Gamma Poisson-Gamma arrival process
   * that has both busyness factor for the day and the busyness factor for the
   * sub-period of the day, both following the Gamma distribution, from
   * the number of arrivals in the array \texttt{arrivals} using method of moments. The day-specific
   * busyness factor follows the Gamma$(Q,Q)$ distribution, the sub-period-specific
   * busyness factor follows the gamma$(R,R)$ distribution.
   * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
   * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,n-1$, and $p=0,\ldots,P-1$,
   * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
   * If we follow the notation introduced for PoissonGammaArrivalProcess, then this method
   * estimates $\alpha_{\mathrm{G}, p}$, $\lambda_{\mathrm{G}, p}$  and the daily gamma
   * busyness parameter. It is assumed that $\alpha_{\mathrm{G}, p}$ and $\lambda_{\mathrm{G}, p}$
   * are vectors of distinct values. The estimation is based on
   * matching the empirical first and second order moments of the distribution
   * of counts (mean, variance and covariance) with the analytical moments of the
   * doubly Gamma Poisson-Gamma arrival process distribution.
   * The returned array of $2P+1$ elements contains
   * $(\alpha_{\mathrm{G}, 0},
   * \lambda_{\mathrm{G}, 0}, \ldots,
   * \alpha_{\mathrm{G}, P-1},
   * \lambda_{\mathrm{G}, P-1}), \beta_0$.
   * @return the estimated gamma parameters.
   */
   public double[] getMMEdoublyGammaGeneral ( ) {
      final double[] gammaParams = new double[2 * numPeriods + 1];

      final double[] empMean = new double[numPeriods];
      final double[] empVar = new double[numPeriods];
      final double[][] empCov = new double[numPeriods][numPeriods];
      double MuSum = 0;
      double RSum = 0;
      double Qhat;
      double Mu2Sum = 0;
      double VarSum = 0;
      double MeanSum = 0;
      double[] Rhat = new double[numPeriods];

      // Calculate the empirical moments
      for (int i = 0; i < numPeriods; i++) {
         empMean[i] = 0;
         empVar[i] = 0;
         for (int j = 0; j < numPeriods; j++) {
            empCov[i][j] = 0;
         }
      }
      for (int i = 0; i < numPeriods; i++) {
         for (int j = 0; j < numObs; j++) {
            empMean[i] += arrivals[j][i];
         }
         empMean[i] /= numObs;
      }
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            for (int j = 0; j < numObs; j++) {
               empCov[i][k] += (arrivals[j][i] - empMean[i]) * (arrivals[j][k] - empMean[k]);
            }
            empCov[i][k] /= numObs;
         }
         empVar[k] = empCov[k][k];
      }
      // Calculate sums of the empirical moments
      for (int i = 0; i < numPeriods - 1; i++) {
         for (int j = i + 1; j < numPeriods; j++) {
            RSum += empCov[j][i];
            MuSum += empMean[j] * empMean[i];
         }
      }
      // Calculate the estimators of Gamma parameters
      Qhat = MuSum / Math.abs(RSum);
      for (int i = 0; i < numPeriods; i++) {
        int sumIndBegin;
        int sumIndEnd;
        
        Mu2Sum=0;
        VarSum=0;
        MeanSum=0;
        if (i<Math.round((double)(movWindSize-1)/2)){
            sumIndBegin = 0;
            sumIndEnd = movWindSize;
        } else if (i>numPeriods-Math.round((double)(movWindSize-1)/2)-1) {
            sumIndBegin = numPeriods-movWindSize;
            sumIndEnd = numPeriods;
        } else {
            sumIndBegin = i - (int)Math.round((double)(movWindSize-1)/2);
            sumIndEnd = sumIndBegin+movWindSize;
        }
        for (int j = sumIndBegin; j < sumIndEnd; j++){
          Mu2Sum += empMean[j] * empMean[j];
          VarSum += empVar[j];
          MeanSum += empMean[j];
        }
                    
        if ( VarSum*Qhat - Mu2Sum - MeanSum*Qhat <= 0 ){
            Rhat[i] = 10;
        } else {
            Rhat[i] = (1+Qhat)*Mu2Sum / (VarSum*Qhat - Mu2Sum - MeanSum*Qhat);
        }
        
        if (Rhat[i] > 100){
            Rhat[i] = 100;
        }
      }

      // Set the outputs
      for (int i = 0; i < numPeriods; i++) {
         gammaParams[2*i] = empMean[i];
         gammaParams[2*i + 1] = Rhat[i];
      }
      gammaParams[2*numPeriods] = Qhat;
      // bgammaParam = Qhat;

      return gammaParams;
   }

   
   /**
   * Estimates the parameters of a doubly Gamma Poisson-Gamma arrival process
   * that has both busyness factor for the day and the busyness factor for the
   * sub-period of the day, both following the Gamma distribution, from
   * the number of arrivals in the array \texttt{arrivals} using maximum likelihood approach.
   * It uses the trust region Gauss-Newton optimizer implemented in {@link #startGradUncTrustRegion}
   * and the stochastic approximation of the likelihood function and its derivatives implemented
   * in {@link #getLikelihoodDerivativesDoublyGamma} in order to obtain the MLE. Before
   * launching the Gauss-Newton optimizer, this algorithm calls the {@link #getMMEdoublyGamma()} in order
   * to get good initialization for the values of parameters. The output is formatted the
   * same way as it is done for the {@link #getMMEdoublyGamma()}.
   * If we follow the notation introduced for PoissonGammaArrivalProcess, then this method
   * estimates $\alpha_{\mathrm{G}, p}$, $\lambda_{\mathrm{G}, p}$  and the daily gamma
   * busyness parameter. It is assumed that $\alpha_{\mathrm{G}, p}$ is a vector of
   * distinct values while all the entries of $\lambda_{\mathrm{G}, p}$ are the same and equal to $R$.
   * Thus the returned array of $2P+1$ elements contains
   * $(\alpha_{\mathrm{G}, 0} , \ldots, \alpha_{\mathrm{G}, P-1}, 
   *   \lambda_{\mathrm{G}, 0} , \ldots, \lambda_{\mathrm{G}, P-1}, \beta_0$.
   * @return the estimated gamma parameters.
   */
   public double[] getMLEdoublyGamma () {
      double[] gammaParams = new double[2 * numPeriods + 1];
      double[] Output = new double[2*numPeriods + 1];
      double[] Output_temp = new double[numPeriods + 2];

      // Get initial estimate via method of moments
      gammaParams = getMMEdoublyGamma ( );

      // Initialize optimization trace
      x = new double[maxit][numPeriods + 2 + 5];

      // Configure inputs
      // Parameters according to the model B_j ~ \lambda_j Gamma(\alpha_j, \alpha_j)
      for (int i = 0; i < numPeriods; i++) {
         Output_temp[i] = gammaParams[2 * i];
      }
      Output_temp[numPeriods] = gammaParams[1];
      Output_temp[numPeriods + 1] = gammaParams[2 * numPeriods];
      
      // Get MLE via trust region optimization
      // Parameters according to the model B_j ~ \lambda_j Gamma(\alpha_j, \alpha_j)
      Output_temp = startGradUncTrustRegion(Output_temp);
      for (int i = 0; i < numPeriods; i++) {
         Output[i] = Output_temp[numPeriods];
         Output[numPeriods + i] = Output_temp[i];
      }
      Output[2*numPeriods] = Output_temp[numPeriods+1];
      
      return Output;
   }

   
   /**
   * Estimates the parameters of a doubly Gamma Poisson-Gamma arrival process
   * that has both busyness factor for the day and the busyness factor for the
   * sub-period of the day, both following the Gamma distribution, from
   * the number of arrivals in the array \texttt{arrivals} using maximum likelihood approach.
   * It uses the trust region Gauss-Newton optimizer implemented in {@link #startGradUncTrustRegion}
   * and the stochastic approximation of the likelihood function and its derivatives implemented
   * in {@link #getLikelihoodDerivativesDoublyGamma} in order to obtain the MLE. Before
   * launching the Gauss-Newton optimizer, this algorithm calls the {@link #getMMEdoublyGamma()} in order
   * to get good initialization for the values of parameters. The output is formatted the
   * same way as it is done for the {@link #getMMEdoublyGamma()}.
   * If we follow the notation introduced for PoissonGammaArrivalProcess, then this method
   * estimates $\alpha_{\mathrm{G}, p}$, $\lambda_{\mathrm{G}, p}$  and the daily gamma
   * busyness parameter. It is assumed that $\alpha_{\mathrm{G}, p}$ is a vector of
   * distinct values while all the entries of $\lambda_{\mathrm{G}, p}$ are the same and equal to $R$.
   * Thus the returned array of $2P+1$ elements contains
   * $(\alpha_{\mathrm{G}, 0} , \ldots, \alpha_{\mathrm{G}, P-1}, 
   *   \lambda_{\mathrm{G}, 0} , \ldots, \lambda_{\mathrm{G}, P-1}, \beta_0$.
   * @return the estimated gamma parameters.
   */
   public double[] getMLEdoublyGammaSpline () {
      double[] gammaParams = new double[2 * numPeriods + 1];
      double[] Output = new double[2*numPeriods + 1];
      double[] Output_temp = new double[2*numPeriods + 1];

      // Get initial estimate via method of moments
      gammaParams = getMMEdoublyGammaGeneral ( );

      // Configure inputs
      // Parameters according to the model B_j ~ \lambda_j Gamma(\alpha_j, \alpha_j)
      for (int i = 0; i < numPeriods; i++) {
         Output_temp[i] = gammaParams[2 * i];
         Output_temp[numPeriods+i] = gammaParams[2*i+1];
      }
      Output_temp[2*numPeriods] = gammaParams[2 * numPeriods];
      
      // Get MLE via trust region optimization
      // Parameters according to the model B_j ~ \lambda_j Gamma(\alpha_j, \alpha_j)
      Output_temp = startGradUncTrustRegionSpline(Output_temp);
      for (int i = 0; i < numPeriods; i++) {
         Output[i] = Output_temp[numPeriods+i];
         Output[numPeriods + i] = Output_temp[i];
      }
      Output[2*numPeriods] = Output_temp[2*numPeriods];
      
      return Output;
   }
   

   /**
    * Calculates the values of the log-likelihood function and its derivatives for
    * the doubly Gamma-Poisson arrival process model.
    * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
    * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,I-1$, and $p=0,\ldots,P-1$,
    * $I=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    *
    * For the doubly Gamma-Poisson arrival process model the log-likelihood function
    * does not admit any closed form as it contains
    * an integral over $\beta_j$, the daily busyness factor. The integral cannot be treated
    * analytically. This integral is thus treated numerically via the Monte-Carlo
    * approach. The Monte-Calro approach uses {@link #numSamples} samples for the
    * evaluation of the integral. The Gamma distribution with both parameters equal to
    * $Q$ is taken as the proposal distribution. The cost function and all the derivatives
    * are thus approximated stochastically. There is an option to return only the
    * value of the cost function (the log-likelihood function) by assigning to \texttt{OutType}
    * the string "CostOnly". In this case the algorithm uses the random numbers common with
    * the ones that were used to evaluate the derivatives last time the function was called.
    * The importance sampling is used to compensate for possible change of integration measure
    * (that may arise due to change in the value of $Q$ between calls). This feature is used
    * by the trust region algorithm for determining the quality of the trust region and for
    * regulating the step size of the optimization. The common random numbers approach reduces
    * the effects of noise on the step size regulation.
    *
    * The returned array of $2*(P+2) + 1$  elements contains
    * \begin{verbatim}
    * 1) The value of the cost function
    * 2) P values of first order derivatives for deterministic rates
    * 3) Derivative with respect to $R$, sub-period Gamma rate
    * 4) Derivative with respect to $Q$, daily Gamma rate
    * 5) P values of second order derivatives for deterministic rates
    * 6) Second order derivative with respect to $R$, sub-period Gamma rate
    * 7) Second order derivative with respect to $Q$, daily Gamma rate
    * \end{verbatim}
    * @param Lam initial values of base rates.
    * @param R initial value of the Gamma distribution parameter for the sub-period of the day business factor.
    * @param Q initial value of the Gamma distribution parameter for the daily business factor.
    * @param OutType the string controlling output options
    * @return the values of the log-likelihood function and its first and second order
    * derivatives.
    */
   public double[] getLikelihoodDerivativesDoublyGamma (double[] Lam, double R, double Q, String OutType) {
      double[] Output = new double[2 * (numPeriods + 2) + 1];
      double[][] W = new double[numObs][numSamples];
      double[] maxW = new double[numObs];
      double C = 0;
      double logPhiRY;
      double DlogPhiRY;
      double D2logPhiRY;
      double dCdR;
      double d2CdR2;
      double dCdQ;
      double d2CdQ2;
      double[] dCdLam = new double[numPeriods];
      double[] d2CdLam2 = new double[numPeriods];

      if (OutType.equals("CostOnly")) {
         double[][] xi_weights_new = new double[numSamples][numObs];

         // Calculate the correction for the importance weights based on the new distribution
         double logGammaQ = Num.lnGamma(Q);
         double QlogQ;
         QlogQ = Q * Math.log(Q);
         for (int m = 0; m < numSamples; m++) {
            for (int j = 0; j < numObs; j++) {
               //xi_weights_new[m][j] = GammaDist.density(Q, Q, xi[m][j]);
               xi_weights_new[m][j] = QlogQ + (Q - 1) * Math.log(xi[m][j]) - Q * xi[m][j] - logGammaQ;
            }
         }

         // Calculate log importance weights
         for (int j = 0; j < numObs; j++) {
            maxW[j] = Double.NEGATIVE_INFINITY;
            for (int m = 0; m < numSamples; m++) {
               // W[j][m] = Math.log(xi_weights_new[m][j]) - Math.log(xi_weights[m][j]);
               W[j][m] = xi_weights_new[m][j] - xi_weights[m][j];
               for (int i = 0; i < numPeriods; i++) {
                  W[j][m] += arrivals[j][i] * Math.log(xi[m][j] * Lam[i]);
                  W[j][m] -= (arrivals[j][i] + R) * Math.log(xi[m][j] * Lam[i] + R);
               }
               if (maxW[j] < W[j][m]) {
                  maxW[j] = W[j][m];
               }
            }
         }
         // Shift the log importance weights for numerical stability
         // Exponentiate the log importance weights to obtain importance weights
         for (int j = 0; j < numObs; j++) {
            for (int m = 0; m < numSamples; m++) {
               W[j][m] = W[j][m] - maxW[j];
               W[j][m] = Math.exp(W[j][m]);
            }
         }
         // Calculate the value of the likelihood function
         logPhiRY = numObs * numPeriods * R * Math.log(R) - numObs * numPeriods * Num.lnGamma(R);
         for (int j = 0; j < numObs; j++) {
            double temp = 0;
            for (int m = 0; m < numSamples; m++) {
               temp += W[j][m];
            }
            for (int i = 0; i < numPeriods; i++) {
               logPhiRY += Num.lnGamma(R + arrivals[j][i]);
            }
            C += Math.log(temp) + maxW[j];
         }
         C += logPhiRY;


         Output[0] = C;
         return Output;
      }

      // Generate the MC samples from Gamma(Q,Q)
      // Evaluate Gamma pdf for CRN importance weight calculation
      // RandomVariateGen gamRND = new GammaGen(GammaGenStream, Q, Q);
      RandomVariateGen gamRND = new GammaAcceptanceRejectionGen(GammaGenStream, GammaGenStreamAux, Q, Q);
      double logGammaQ = Num.lnGamma(Q);
      double QlogQ;
      QlogQ = Q * Math.log(Q);
      for (int m = 0; m < numSamples; m++) {
         for (int j = 0; j < numObs; j++) {
            double temp;

            temp = gamRND.nextDouble();
            xi[m][j] = temp;
            // xi_weights[m][j] = GammaDist.density(Q, Q, temp);
            xi_weights[m][j] = QlogQ + (Q - 1) * Math.log(temp) - Q * temp - logGammaQ;
         }
      }
      // Calculate log importance weights
      for (int j = 0; j < numObs; j++) {
         maxW[j] = Double.NEGATIVE_INFINITY;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = 0;
            for (int i = 0; i < numPeriods; i++) {
               W[j][m] += arrivals[j][i] * Math.log(xi[m][j] * Lam[i]);
               W[j][m] -= (arrivals[j][i] + R) * Math.log(xi[m][j] * Lam[i] + R);
            }
            if (maxW[j] < W[j][m]) {
               maxW[j] = W[j][m];
            }
         }
      }
      // Shift the log importance weights for numerical stability
      // Exponentiate the log importance weights to obtain importance weights
      for (int j = 0; j < numObs; j++) {
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = W[j][m] - maxW[j];
            W[j][m] = Math.exp(W[j][m]);
         }
      }
      // Calculate the value of the likelihood function
      logPhiRY = numObs * numPeriods * R * Math.log(R) - numObs * numPeriods * Num.lnGamma(R);
      DlogPhiRY = numObs * numPeriods * (Math.log(R) + 1) - numObs * numPeriods * Num.digamma(R);
      D2logPhiRY = numObs * numPeriods / R - numObs * numPeriods * Num.trigamma(R);
      for (int j = 0; j < numObs; j++) {
         double temp = 0;
         for (int m = 0; m < numSamples; m++) {
            temp += W[j][m];
         }
         for (int i = 0; i < numPeriods; i++) {
            logPhiRY += Num.lnGamma(R + arrivals[j][i]);
            DlogPhiRY += Num.digamma(R + arrivals[j][i]);
            D2logPhiRY += Num.trigamma(R + arrivals[j][i]);
         }
         C += Math.log(temp) + maxW[j];
      }
      C += logPhiRY;

      // Normalize importance weights so they sum to 1 over m
      for (int j = 0; j < numObs; j++) {
         double Wsum = 0;
         for (int m = 0; m < numSamples; m++) {
            Wsum += W[j][m];
         }
         Wsum = 1 / Wsum;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] *= Wsum;
         }
      }
      // Calculate derivatives
      dCdR = DlogPhiRY;
      d2CdR2 = D2logPhiRY;
      dCdQ = numObs * (Math.log(Q) + 1 - Num.digamma(Q));
      d2CdQ2 = numObs * (1 / Q - Num.trigamma(Q));
      for (int j = 0; j < numObs; j++) {
         double temp1_dCdR = 0;
         double temp1_d2CdR2 = 0;
         double temp_dCdQ = 0;
         double[] temp_dCdLam = new double[numPeriods];

         for (int m = 0; m < numSamples; m++) {
            double temp_dCdR = 0;
            double temp_d2CdR2 = 0;

            for (int i = 0; i < numPeriods; i++) {
               double alpha_lam;
               double OneOverRplusAlphaLam;
               double RplusYoverRplusAlphaLam;
               double AlphaLamMinusYoverRplusAlphaLam2;

               alpha_lam = xi[m][j] * Lam[i];
               OneOverRplusAlphaLam = 1 / (R + alpha_lam);
               RplusYoverRplusAlphaLam = (R + arrivals[j][i]) * OneOverRplusAlphaLam;
               AlphaLamMinusYoverRplusAlphaLam2 = OneOverRplusAlphaLam * OneOverRplusAlphaLam;
               AlphaLamMinusYoverRplusAlphaLam2 *= (alpha_lam - arrivals[j][i]);

               temp_dCdR += Math.log(R + alpha_lam) + RplusYoverRplusAlphaLam;
               temp_d2CdR2 += OneOverRplusAlphaLam + AlphaLamMinusYoverRplusAlphaLam2;

               temp_dCdLam[i] -= RplusYoverRplusAlphaLam * xi[m][j] * W[j][m];

            }
            temp1_dCdR += temp_dCdR * W[j][m];
            temp1_d2CdR2 += (temp_dCdR * temp_dCdR - temp_d2CdR2) * W[j][m];

            temp_dCdQ += (Math.log(xi[m][j]) - xi[m][j]) * W[j][m];

         }
         dCdR -= temp1_dCdR;
         d2CdR2 += -temp1_dCdR * temp1_dCdR + temp1_d2CdR2;
         dCdQ += temp_dCdQ;
         d2CdQ2 -= temp_dCdQ * temp_dCdQ;

         for (int i = 0; i < numPeriods; i++) {
            double temp_d2CdLam2;

            temp_d2CdLam2 = temp_dCdLam[i] + arrivals[j][i] / Lam[i];
            dCdLam[i] += temp_d2CdLam2;
            d2CdLam2[i] -= temp_d2CdLam2 * temp_d2CdLam2;
         }

      }


      Output[0] = C;
      for (int i = 1; i < numPeriods + 1; i++) {
         Output[i] = dCdLam[i - 1];
         Output[numPeriods + 2 + i] = d2CdLam2[i - 1];
      }
      Output[numPeriods + 1] = dCdR;
      Output[numPeriods + 2] = dCdQ;
      Output[2*numPeriods + 3] = d2CdR2;
      Output[2*numPeriods + 4] = d2CdQ2;

      return Output;
   }
   
   
   /**
    * Calculates the values of the log-likelihood function and its derivatives for
    * the extended doubly Gamma-Poisson arrival process model.
    * The rate is modelled as product of the deterministic base rate, the Gamma 
    * distributed sub-period component independent over sub-periods and the busyness 
    * factor for the day exponentiated to the power $P_p$, where  $P_p$ changes 
    * from sub period to sub-period. The marginal daily busyness factor distribution 
    * for each period thus comes from the generalized Gamma distribution.
    * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
    * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,I-1$, and $p=0,\ldots,P-1$,
    * $I=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    *
    * For the doubly Gamma-Poisson arrival process model the log-likelihood function
    * does not admit any closed form as it contains
    * an integral over $\beta_j$, the daily busyness factor. The integral cannot be treated
    * analytically. This integral is thus treated numerically via the Monte-Carlo
    * approach. The Monte-Calro approach uses {@link #numSamples} samples for the
    * evaluation of the integral. The Gamma distribution with both parameters equal to
    * $Q$ is taken as the proposal distribution. The cost function and all the derivatives
    * are thus approximated stochastically. There is an option to return only the
    * value of the cost function (the log-likelihood function) by assigning to \texttt{OutType}
    * the string "CostOnly". In this case the algorithm uses the random numbers common with
    * the ones that were used to evaluate the derivatives last time the function was called.
    * The importance sampling is used to compensate for possible change of integration measure
    * (that may arise due to change in the value of $Q$ between calls). This feature is used
    * by the trust region algorithm for determining the quality of the trust region and for
    * regulating the step size of the optimization. The common random numbers approach reduces
    * the effects of noise on the step size regulation.
    *
    * The returned 2D array Output of $P \times 7$  elements contains
    * \begin{verbatim}
    * 1) C The value of the cost function
    * \end{verbatim}
    * @param Lam initial values of base rates.
    * @param R initial value of the Gamma distribution parameter for the sub-period of the day business factor.
    * @param Q initial value of the Gamma distribution parameter for the daily business factor.
    * @param OutType the string controlling output options
    * @return the 2D array containing values of the log-likelihood function and its first and second order
    * derivatives. 
    */
   public double getLikelihoodExtendedDoublyGammaSpline (double[] Lam, double[] R, double Q, double[] p) {
      double[][] W = new double[numObs][numSamples];
      double[] maxW = new double[numObs];
      double C = 0;
      double logPhiRY;
      
      DoubleMatrix2D d;
      
      double[] gammaPi = new double[numPeriods];
      
      d = new DenseDoubleMatrix2D(numPeriods, 1);
      DoubleMatrix2D smothnessPenalty = new DenseDoubleMatrix2D(numPeriods, 1);
      for (int i=0; i<numPeriods; i++)
          d.set(i, 0, R[i]);
//      QRQ.zMult(d, smothnessPenalty, 1.0, 0.0, false, false);
      
      for (int i = 0; i < numPeriods; i++) {
          gammaPi[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i] + Q) - Num.lnGamma(Q));
      }

      // Generate the MC samples from Gamma(Q,Q)
      // Evaluate Gamma pdf for CRN importance weight calculation
      // RandomVariateGen gamRND = new GammaGen(GammaGenStream, Q, Q);
      RandomVariateGen gamRND = new GammaAcceptanceRejectionGen(GammaGenStream, GammaGenStreamAux, Q, Q);
      for (int m = 0; m < numSamples; m++) {
         for (int j = 0; j < numObs; j++) {
            double temp;

            temp = gamRND.nextDouble();
            xi[m][j] = temp;
         }
      }
      // Calculate log importance weights
      for (int j = 0; j < numObs; j++) {
         maxW[j] = Double.NEGATIVE_INFINITY;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = 0;
            for (int i = 0; i < numPeriods; i++) {
               W[j][m] += arrivals[j][i] * Math.log( Math.pow(xi[m][j], p[i]) / gammaPi[i] * Lam[i]);
               W[j][m] -= (arrivals[j][i] + R[i]) * Math.log( Math.pow(xi[m][j], p[i]) / gammaPi[i] * Lam[i] + R[i]);
            }
            if (maxW[j] < W[j][m]) {
               maxW[j] = W[j][m];
            }
         }
      }
      // Shift the log importance weights for numerical stability
      // Exponentiate the log importance weights to obtain importance weights
      for (int j = 0; j < numObs; j++) {
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = W[j][m] - maxW[j];
            W[j][m] = Math.exp(W[j][m]);
         }
      }
      // Calculate the value of the likelihood function
      logPhiRY = 0;
      for (int j = 0; j < numObs; j++) {
         double temp = 0;
         for (int m = 0; m < numSamples; m++) {
            temp += W[j][m];
         }
         for (int i = 0; i < numPeriods; i++) {
            logPhiRY += Num.lnGamma(R[i] + arrivals[j][i]);
            logPhiRY += R[i] * Math.log(R[i]) - Num.lnGamma(R[i]);
            
         }
         C += Math.log(temp) + maxW[j];
      }
      C += logPhiRY;
      
      C *= smoothingLambda;
      for (int i=0; i<numPeriods; i++){
          C -= 2.0/3.0 *(1 - smoothingLambda) * smothnessPenalty.get(i, 0) * d.get(i, 0);
      }

      return C;
   }

   
   /**
    * Calculates the values of the log-likelihood function and its derivatives for
    * the extended doubly Gamma-Poisson arrival process model.
    * The rate is modelled as product of the deterministic base rate, the Gamma 
    * distributed sub-period component independent over sub-periods and the busyness 
    * factor for the day exponentiated to the power $P_p$, where  $P_p$ changes 
    * from sub period to sub-period. The marginal daily busyness factor distribution 
    * for each period thus comes from the generalized Gamma distribution.
    * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
    * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,I-1$, and $p=0,\ldots,P-1$,
    * $I=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    *
    * For the doubly Gamma-Poisson arrival process model the log-likelihood function
    * does not admit any closed form as it contains
    * an integral over $\beta_j$, the daily busyness factor. The integral cannot be treated
    * analytically. This integral is thus treated numerically via the Monte-Carlo
    * approach. The Monte-Calro approach uses {@link #numSamples} samples for the
    * evaluation of the integral. The Gamma distribution with both parameters equal to
    * $Q$ is taken as the proposal distribution. The cost function and all the derivatives
    * are thus approximated stochastically. There is an option to return only the
    * value of the cost function (the log-likelihood function) by assigning to \texttt{OutType}
    * the string "CostOnly". In this case the algorithm uses the random numbers common with
    * the ones that were used to evaluate the derivatives last time the function was called.
    * The importance sampling is used to compensate for possible change of integration measure
    * (that may arise due to change in the value of $Q$ between calls). This feature is used
    * by the trust region algorithm for determining the quality of the trust region and for
    * regulating the step size of the optimization. The common random numbers approach reduces
    * the effects of noise on the step size regulation.
    *
    * The returned 2D array Output of $P \times 7$  elements contains
    * \begin{verbatim}
    * 1) Output[0][0] The value of the cost function
    * 2) Output[:][1] (second column) P values of first order derivatives  with respect to deterministic rates
    * 3) Output[:][2] P values of second order derivatives  with respect to deterministic rates
    * 4) Output[:][3] P values of first order derivatives with respect to $R_p$, sub-period Gamma rate
    * 5) Output[:][4] P values of second order derivatives with respect to  $R_p$, sub-period Gamma rate
    * 6) Output[0][5] Derivative with respect to $Q$, daily Gamma rate
    * 7) Output[0][6] Second order derivative with respect to $Q$, daily Gamma rate
    * \end{verbatim}
    * @param Lam initial values of base rates.
    * @param R initial value of the Gamma distribution parameter for the sub-period of the day business factor.
    * @param Q initial value of the Gamma distribution parameter for the daily business factor.
    * @param OutType the string controlling output options
    * @return the 2D array containing values of the log-likelihood function and its first and second order
    * derivatives. 
    */
   public double[][] getLikelihoodDerivativesExtendedDoublyGammaSpline (double[] Lam, double[] R, double Q, double[] p, String OutType) {
      double[][] Output = new double[8][numPeriods];
      double[][] W = new double[numObs][numSamples];
      double[] maxW = new double[numObs];
      double C = 0;
      double logPhiRY;
      
      // double[] DlogPhiRY = new double[numPeriods];
      // double[] D2logPhiRY = new double[numPeriods];
      double[] dCdR = new double[numPeriods];
      
      double dCdQ;
      double[] dCdLam = new double[numPeriods];
      DoubleMatrix2D d;
      
      double[] dCdPi = new double[numPeriods];
      double[] gammaPi = new double[numPeriods];
      double[] dGammaPidPi = new double[numPeriods];
      double[] dGammaPidQ = new double[numPeriods];
      
      d = new DenseDoubleMatrix2D(numPeriods, 1);
      DoubleMatrix2D smothnessPenalty = new DenseDoubleMatrix2D(numPeriods, 1);
      for (int i=0; i<numPeriods; i++)
          d.set(i, 0, R[i]);
      QRQ.zMult(d, smothnessPenalty, 1.0, 0.0, false, false);
      
      for (int i = 0; i < numPeriods; i++) {
          gammaPi[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i] + Q) - Num.lnGamma(Q));
      }

      if (OutType.equals("CostOnly")) {
         double[][] xi_weights_new = new double[numSamples][numObs];

         // Calculate the correction for the importance weights based on the new distribution
         double logGammaQ = Num.lnGamma(Q);
         double QlogQ;
         QlogQ = Q * Math.log(Q);
         for (int m = 0; m < numSamples; m++) {
            for (int j = 0; j < numObs; j++) {
               //xi_weights_new[m][j] = GammaDist.density(Q, Q, xi[m][j]);
               xi_weights_new[m][j] = QlogQ + (Q - 1) * Math.log(xi[m][j]) - Q * xi[m][j] - logGammaQ;
            }
         }

         // Calculate log importance weights
         for (int j = 0; j < numObs; j++) {
            maxW[j] = Double.NEGATIVE_INFINITY;
            for (int m = 0; m < numSamples; m++) {
               // W[j][m] = Math.log(xi_weights_new[m][j]) - Math.log(xi_weights[m][j]);
               W[j][m] = xi_weights_new[m][j] - xi_weights[m][j];
               for (int i = 0; i < numPeriods; i++) {
                   double temp;
                   
                   temp = Math.pow(xi[m][j], p[i]) / gammaPi[i] * Lam[i];
                   
                   W[j][m] += arrivals[j][i] * Math.log( temp );
                   W[j][m] -= (arrivals[j][i] + R[i]) * Math.log( temp + R[i]);
               }
               if (maxW[j] < W[j][m]) {
                  maxW[j] = W[j][m];
               }
            }
         }
         // Shift the log importance weights for numerical stability
         // Exponentiate the log importance weights to obtain importance weights
         for (int j = 0; j < numObs; j++) {
            for (int m = 0; m < numSamples; m++) {
               W[j][m] = W[j][m] - maxW[j];
               W[j][m] = Math.exp(W[j][m]);
            }
         }
         // Calculate the value of the likelihood function
         logPhiRY = 0.0;
         for (int j = 0; j < numObs; j++) {
            double temp = 0;
            for (int m = 0; m < numSamples; m++) {
               temp += W[j][m];
            }
            for (int i = 0; i < numPeriods; i++) {
               logPhiRY += Num.lnGamma(R[i] + arrivals[j][i]);
               logPhiRY += R[i] * Math.log(R[i]) - Num.lnGamma(R[i]);
            }
            C += Math.log(temp) + maxW[j];
         }
         C += logPhiRY;
         
         C *= smoothingLambda;
         for (int i=0; i<numPeriods; i++){
             C -= 2.0/3.0 * (1 - smoothingLambda) * smothnessPenalty.get(i, 0) * d.get(i, 0);
         }


         Output[0][0] = C;
         return Output;
      }
      
      for (int i = 0; i < numPeriods; i++) {
          /*
          dGammaPidPi[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i]+Q) ) *
                  (-Math.log(Q) + Num.digamma(p[i] + Q))  
                  / Math.exp( Num.lnGamma(Q) );
          
          dGammaPidQ[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i]+Q) ) *
                  (-p[i]/Q + Num.digamma(p[i] + Q) - Num.digamma(Q))
                  / Math.exp( Num.lnGamma(Q) );
          */
          
          dGammaPidPi[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i]+Q) - Num.lnGamma(Q) ) *
                  (-Math.log(Q) + digamma(p[i] + Q));
          
          dGammaPidQ[i] = Math.pow(Q, -p[i]) * Math.exp( Num.lnGamma(p[i]+Q) - Num.lnGamma(Q) ) *
                  (-p[i]/Q + digamma(p[i] + Q) - digamma(Q));
      }

      // Generate the MC samples from Gamma(Q,Q)
      // Evaluate Gamma pdf for CRN importance weight calculation
      // RandomVariateGen gamRND = new GammaGen(GammaGenStream, Q, Q);
      RandomVariateGen gamRND = new GammaAcceptanceRejectionGen(GammaGenStream, GammaGenStreamAux, Q, Q);
      double logGammaQ = Num.lnGamma(Q);
      double QlogQ;
      QlogQ = Q * Math.log(Q);
      for (int m = 0; m < numSamples; m++) {
         for (int j = 0; j < numObs; j++) {
            double temp;

            temp = gamRND.nextDouble();
            xi[m][j] = temp;
            // xi_weights[m][j] = GammaDist.density(Q, Q, temp);
            xi_weights[m][j] = QlogQ + (Q - 1) * Math.log(temp) - Q * temp - logGammaQ;
         }
      }
      // Calculate log importance weights
      for (int j = 0; j < numObs; j++) {
         maxW[j] = Double.NEGATIVE_INFINITY;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = 0;
            for (int i = 0; i < numPeriods; i++) {
                double temp;
                
                temp = Math.pow(xi[m][j], p[i]) / gammaPi[i] * Lam[i];
                
                W[j][m] += arrivals[j][i] * Math.log( temp );
                W[j][m] -= (arrivals[j][i] + R[i]) * Math.log( temp + R[i]);
            }
            if (maxW[j] < W[j][m]) {
               maxW[j] = W[j][m];
            }
         }
      }
      // Shift the log importance weights for numerical stability
      // Exponentiate the log importance weights to obtain importance weights
      for (int j = 0; j < numObs; j++) {
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = W[j][m] - maxW[j];
            W[j][m] = Math.exp(W[j][m]);
         }
      }
      // Calculate the value of the likelihood function
      logPhiRY = 0;
      for (int j = 0; j < numObs; j++) {
         double temp = 0;
         for (int m = 0; m < numSamples; m++) {
            temp += W[j][m];
         }
         for (int i = 0; i < numPeriods; i++) {
            logPhiRY += Num.lnGamma(R[i] + arrivals[j][i]);
            logPhiRY += R[i] * Math.log(R[i]) - Num.lnGamma(R[i]);
            
            /*
            dCdR[i] += Num.digamma(R[i] + arrivals[j][i]);
            dCdR[i] += (Math.log(R[i]) + 1) - Num.digamma(R[i]);
            */
            
            dCdR[i] += digamma(R[i] + arrivals[j][i]);
            dCdR[i] += (Math.log(R[i]) + 1) - digamma(R[i]);
            
         }
         C += Math.log(temp) + maxW[j];
      }
      C += logPhiRY;
      
      C *= smoothingLambda;
      for (int i=0; i<numPeriods; i++){
          C -= 2.0/3.0 *(1 - smoothingLambda) * smothnessPenalty.get(i, 0) * d.get(i, 0);
      }

      // Normalize importance weights so they sum to 1 over m
      for (int j = 0; j < numObs; j++) {
         double Wsum = 0;
         for (int m = 0; m < numSamples; m++) {
            Wsum += W[j][m];
         }
         Wsum = 1 / Wsum;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] *= Wsum;
         }
      }
      // Calculate derivatives
      /* dCdQ = numObs * (Math.log(Q) + 1 - Num.digamma(Q)); */
      dCdQ = numObs * (Math.log(Q) + 1 - digamma(Q));
      for (int j = 0; j < numObs; j++) {
         double[] temp1_dCdR = new double[numPeriods];
         double temp_dCdQ = 0;
         double[] temp_dCdLam = new double[numPeriods];
         double[] temp_dCdPi = new double[numPeriods];

         for (int m = 0; m < numSamples; m++) {
            double[] temp_dCdR = new double[numPeriods];

            for (int i = 0; i < numPeriods; i++) {
               double alpha_lam;
               double OneOverRplusAlphaLam;
               double RplusYoverRplusAlphaLam;
               double temp1_dCdPi;
               double temp1_dCdQ;
               double temp;
               
               temp = Math.pow(xi[m][j], p[i]) / gammaPi[i];

               alpha_lam = temp * Lam[i];
               OneOverRplusAlphaLam = 1 / (R[i] + alpha_lam);
               RplusYoverRplusAlphaLam = (R[i] + arrivals[j][i]) * OneOverRplusAlphaLam;

               temp_dCdR[i] += Math.log(R[i] + alpha_lam) + RplusYoverRplusAlphaLam;

               temp_dCdLam[i] -= RplusYoverRplusAlphaLam * temp * W[j][m];
               
               temp1_dCdR[i] += temp_dCdR[i] * W[j][m];
               
               temp1_dCdPi = alpha_lam / gammaPi[i] * (Math.log(xi[m][j])*gammaPi[i] - dGammaPidPi[i]) *
                       (arrivals[j][i] /alpha_lam - (arrivals[j][i] +R[i]) / (R[i] + alpha_lam) );
               temp_dCdPi[i] += temp1_dCdPi * W[j][m];
               
               temp1_dCdQ = alpha_lam / gammaPi[i] * dGammaPidQ[i] *
                       (arrivals[j][i] /alpha_lam - (arrivals[j][i] +R[i]) / (R[i] + alpha_lam) );
               temp_dCdQ -= temp1_dCdQ * W[j][m];

            }
            temp_dCdQ += (Math.log(xi[m][j]) - xi[m][j]) * W[j][m];
         }
         dCdQ += temp_dCdQ;

         for (int i = 0; i < numPeriods; i++) {
            
            dCdR[i] -= temp1_dCdR[i];

            dCdLam[i] += temp_dCdLam[i] + arrivals[j][i] / Lam[i];
            
            dCdPi[i] += temp_dCdPi[i];
         }

      }

      Output[0][0] = C;
      for (int i = 0; i < numPeriods; i++) {
         Output[1][i] = dCdLam[i];
         Output[3][i] = smoothingLambda * dCdR[i] - (1-smoothingLambda) * 4.0 / 3.0 * smothnessPenalty.get(i, 0);
         Output[7][i] = dCdPi[i];
         
         //Output[2][i] = dGammaPidPi[i];
         //Output[4][i] = gammaPi[i];
         //Output[6][i] = dGammaPidQ[i];
      }
      Output[5][0] = dCdQ;

      return Output;
   }

   
   
   
   /**
    * Calculates the values of the log-likelihood function and its derivatives for
    * the doubly Gamma-Poisson arrival process model.
    * Element \texttt{arrivals[i][p]} corresponds to the number of arrivals on
    * day \texttt{i} during period \texttt{p}, where $i=0,\ldots,I-1$, and $p=0,\ldots,P-1$,
    * $I=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    *
    * For the doubly Gamma-Poisson arrival process model the log-likelihood function
    * does not admit any closed form as it contains
    * an integral over $\beta_j$, the daily busyness factor. The integral cannot be treated
    * analytically. This integral is thus treated numerically via the Monte-Carlo
    * approach. The Monte-Calro approach uses {@link #numSamples} samples for the
    * evaluation of the integral. The Gamma distribution with both parameters equal to
    * $Q$ is taken as the proposal distribution. The cost function and all the derivatives
    * are thus approximated stochastically. There is an option to return only the
    * value of the cost function (the log-likelihood function) by assigning to \texttt{OutType}
    * the string "CostOnly". In this case the algorithm uses the random numbers common with
    * the ones that were used to evaluate the derivatives last time the function was called.
    * The importance sampling is used to compensate for possible change of integration measure
    * (that may arise due to change in the value of $Q$ between calls). This feature is used
    * by the trust region algorithm for determining the quality of the trust region and for
    * regulating the step size of the optimization. The common random numbers approach reduces
    * the effects of noise on the step size regulation.
    *
    * The returned 2D array Output of $P \times 7$  elements contains
    * \begin{verbatim}
    * 1) Output[0][0] The value of the cost function
    * 2) Output[:][1] (second column) P values of first order derivatives  with respect to deterministic rates
    * 3) Output[:][2] P values of second order derivatives  with respect to deterministic rates
    * 4) Output[:][3] P values of first order derivatives with respect to $R_p$, sub-period Gamma rate
    * 5) Output[:][4] P values of second order derivatives with respect to  $R_p$, sub-period Gamma rate
    * 6) Output[0][5] Derivative with respect to $Q$, daily Gamma rate
    * 7) Output[0][6] Second order derivative with respect to $Q$, daily Gamma rate
    * \end{verbatim}
    * @param Lam initial values of base rates.
    * @param R initial value of the Gamma distribution parameter for the sub-period of the day business factor.
    * @param Q initial value of the Gamma distribution parameter for the daily business factor.
    * @param OutType the string controlling output options
    * @return the 2D array containing values of the log-likelihood function and its first and second order
    * derivatives. 
    */
   public double[][] getLikelihoodDerivativesDoublyGammaSpline (double[] Lam, double[] R, double Q, String OutType) {
      double[][] Output = new double[7][numPeriods];
      double[][] W = new double[numObs][numSamples];
      double[] maxW = new double[numObs];
      double C = 0;
      double logPhiRY;
      // double[] DlogPhiRY = new double[numPeriods];
      // double[] D2logPhiRY = new double[numPeriods];
      double[] dCdR = new double[numPeriods];
      double[] d2CdR2 = new double[numPeriods];
      double dCdQ;
      double d2CdQ2;
      double[] dCdLam = new double[numPeriods];
      double[] d2CdLam2 = new double[numPeriods];
      DoubleMatrix2D d;
      
      d = new DenseDoubleMatrix2D(numPeriods, 1);
      DoubleMatrix2D smothnessPenalty = new DenseDoubleMatrix2D(numPeriods, 1);
      for (int i=0; i<numPeriods; i++)
          d.set(i, 0, R[i]);
      QRQ.zMult(d, smothnessPenalty, 1.0, 0.0, false, false);

      if (OutType.equals("CostOnly")) {
         double[][] xi_weights_new = new double[numSamples][numObs];

         // Calculate the correction for the importance weights based on the new distribution
         double logGammaQ = Num.lnGamma(Q);
         double QlogQ;
         QlogQ = Q * Math.log(Q);
         for (int m = 0; m < numSamples; m++) {
            for (int j = 0; j < numObs; j++) {
               //xi_weights_new[m][j] = GammaDist.density(Q, Q, xi[m][j]);
               xi_weights_new[m][j] = QlogQ + (Q - 1) * Math.log(xi[m][j]) - Q * xi[m][j] - logGammaQ;
            }
         }

         // Calculate log importance weights
         for (int j = 0; j < numObs; j++) {
            maxW[j] = Double.NEGATIVE_INFINITY;
            for (int m = 0; m < numSamples; m++) {
               // W[j][m] = Math.log(xi_weights_new[m][j]) - Math.log(xi_weights[m][j]);
               W[j][m] = xi_weights_new[m][j] - xi_weights[m][j];
               for (int i = 0; i < numPeriods; i++) {
                  W[j][m] += arrivals[j][i] * Math.log(xi[m][j] * Lam[i]);
                  W[j][m] -= (arrivals[j][i] + R[i]) * Math.log(xi[m][j] * Lam[i] + R[i]);
               }
               if (maxW[j] < W[j][m]) {
                  maxW[j] = W[j][m];
               }
            }
         }
         // Shift the log importance weights for numerical stability
         // Exponentiate the log importance weights to obtain importance weights
         for (int j = 0; j < numObs; j++) {
            for (int m = 0; m < numSamples; m++) {
               W[j][m] = W[j][m] - maxW[j];
               W[j][m] = Math.exp(W[j][m]);
            }
         }
         // Calculate the value of the likelihood function
         logPhiRY = 0;
         for (int j = 0; j < numObs; j++) {
            double temp = 0;
            for (int m = 0; m < numSamples; m++) {
               temp += W[j][m];
            }
            for (int i = 0; i < numPeriods; i++) {
               logPhiRY += Num.lnGamma(R[i] + arrivals[j][i]);
               logPhiRY += R[i] * Math.log(R[i]) - Num.lnGamma(R[i]);
            }
            C += Math.log(temp) + maxW[j];
         }
         C += logPhiRY;
         
         C *= smoothingLambda;
         for (int i=0; i<numPeriods; i++){
             C -= 2.0/3.0 * (1 - smoothingLambda) * smothnessPenalty.get(i, 0) * d.get(i, 0);
         }


         Output[0][0] = C;
         return Output;
      }

      // Generate the MC samples from Gamma(Q,Q)
      // Evaluate Gamma pdf for CRN importance weight calculation
      // RandomVariateGen gamRND = new GammaGen(GammaGenStream, Q, Q);
      RandomVariateGen gamRND = new GammaAcceptanceRejectionGen(GammaGenStream, GammaGenStreamAux, Q, Q);
      double logGammaQ = Num.lnGamma(Q);
      double QlogQ;
      QlogQ = Q * Math.log(Q);
      for (int m = 0; m < numSamples; m++) {
         for (int j = 0; j < numObs; j++) {
            double temp;

            temp = gamRND.nextDouble();
            xi[m][j] = temp;
            // xi_weights[m][j] = GammaDist.density(Q, Q, temp);
            xi_weights[m][j] = QlogQ + (Q - 1) * Math.log(temp) - Q * temp - logGammaQ;
         }
      }
      // Calculate log importance weights
      for (int j = 0; j < numObs; j++) {
         maxW[j] = Double.NEGATIVE_INFINITY;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = 0;
            for (int i = 0; i < numPeriods; i++) {
               W[j][m] += arrivals[j][i] * Math.log(xi[m][j] * Lam[i]);
               W[j][m] -= (arrivals[j][i] + R[i]) * Math.log(xi[m][j] * Lam[i] + R[i]);
            }
            if (maxW[j] < W[j][m]) {
               maxW[j] = W[j][m];
            }
         }
      }
      // Shift the log importance weights for numerical stability
      // Exponentiate the log importance weights to obtain importance weights
      for (int j = 0; j < numObs; j++) {
         for (int m = 0; m < numSamples; m++) {
            W[j][m] = W[j][m] - maxW[j];
            W[j][m] = Math.exp(W[j][m]);
         }
      }
      // Calculate the value of the likelihood function
      logPhiRY = 0;
      for (int j = 0; j < numObs; j++) {
         double temp = 0;
         for (int m = 0; m < numSamples; m++) {
            temp += W[j][m];
         }
         for (int i = 0; i < numPeriods; i++) {
            logPhiRY += Num.lnGamma(R[i] + arrivals[j][i]);
            logPhiRY += R[i] * Math.log(R[i]) - Num.lnGamma(R[i]);
            
            dCdR[i] += Num.digamma(R[i] + arrivals[j][i]);
            dCdR[i] += (Math.log(R[i]) + 1) - Num.digamma(R[i]);
            
            d2CdR2[i] += Num.trigamma(R[i] + arrivals[j][i]);
            d2CdR2[i] += 1 / R[i] - Num.trigamma(R[i]);
         }
         C += Math.log(temp) + maxW[j];
      }
      C += logPhiRY;
      
      C *= smoothingLambda;
      for (int i=0; i<numPeriods; i++){
          C -= 2.0/3.0 *(1 - smoothingLambda) * smothnessPenalty.get(i, 0) * d.get(i, 0);
      }

      // Normalize importance weights so they sum to 1 over m
      for (int j = 0; j < numObs; j++) {
         double Wsum = 0;
         for (int m = 0; m < numSamples; m++) {
            Wsum += W[j][m];
         }
         Wsum = 1 / Wsum;
         for (int m = 0; m < numSamples; m++) {
            W[j][m] *= Wsum;
         }
      }
      // Calculate derivatives
      // dCdR = DlogPhiRY;
      // d2CdR2 = D2logPhiRY;
      dCdQ = numObs * (Math.log(Q) + 1 - Num.digamma(Q));
      d2CdQ2 = numObs * (1 / Q - Num.trigamma(Q));
      for (int j = 0; j < numObs; j++) {
         double[] temp1_dCdR = new double[numPeriods];
         double[] temp1_d2CdR2 = new double[numPeriods];
         double temp_dCdQ = 0;
         double[] temp_dCdLam = new double[numPeriods];

         for (int m = 0; m < numSamples; m++) {
            double[] temp_dCdR = new double[numPeriods];
            double[] temp_d2CdR2 = new double[numPeriods];

            for (int i = 0; i < numPeriods; i++) {
               double alpha_lam;
               double OneOverRplusAlphaLam;
               double RplusYoverRplusAlphaLam;
               double AlphaLamMinusYoverRplusAlphaLam2;

               alpha_lam = xi[m][j] * Lam[i];
               OneOverRplusAlphaLam = 1 / (R[i] + alpha_lam);
               RplusYoverRplusAlphaLam = (R[i] + arrivals[j][i]) * OneOverRplusAlphaLam;
               AlphaLamMinusYoverRplusAlphaLam2 = OneOverRplusAlphaLam * OneOverRplusAlphaLam;
               AlphaLamMinusYoverRplusAlphaLam2 *= (alpha_lam - arrivals[j][i]);

               temp_dCdR[i] += Math.log(R[i] + alpha_lam) + RplusYoverRplusAlphaLam;
               temp_d2CdR2[i] += OneOverRplusAlphaLam + AlphaLamMinusYoverRplusAlphaLam2;

               temp_dCdLam[i] -= RplusYoverRplusAlphaLam * xi[m][j] * W[j][m];
               
               temp1_dCdR[i] += temp_dCdR[i] * W[j][m];
               temp1_d2CdR2[i] += (temp_dCdR[i] * temp_dCdR[i] - temp_d2CdR2[i]) * W[j][m];

            }
            

            temp_dCdQ += (Math.log(xi[m][j]) - xi[m][j]) * W[j][m];

         }
         dCdQ += temp_dCdQ;
         d2CdQ2 -= temp_dCdQ * temp_dCdQ;

         for (int i = 0; i < numPeriods; i++) {
            double temp_d2CdLam2;
            
            dCdR[i] -= temp1_dCdR[i];
            d2CdR2[i] += -temp1_dCdR[i] * temp1_dCdR[i] + temp1_d2CdR2[i];

            temp_d2CdLam2 = temp_dCdLam[i] + arrivals[j][i] / Lam[i];
            dCdLam[i] += temp_d2CdLam2;
            d2CdLam2[i] -= temp_d2CdLam2 * temp_d2CdLam2;
         }

      }

      
      
                  
      Output[0][0] = C;
      for (int i = 0; i < numPeriods; i++) {
         Output[1][i] = dCdLam[i];
         Output[2][i] = d2CdLam2[i];
         Output[3][i] = smoothingLambda * dCdR[i] - (1-smoothingLambda) * 4.0 / 3.0 * smothnessPenalty.get(i, 0);
         Output[4][i] = -d2CdR2[i];
      }
      Output[5][0] = dCdQ;
      Output[6][0] = d2CdQ2;

      return Output;
   }

   /**
    * Implements the trust region Gauss-Newton maximizer of the likelihood of
    * the doubly Gamma-Poisson process. In this algorithm the expressions for
    * the second order derivatives (or their approximations having reduced
    * number of terms) are used as pre-scalers for the gradients. Moreover,
    * there is also a set of step size regulators that reflect the size of the
    * trust region, the region in which we believe our model of the cost function
    * (the locally linear model based on the first order Taylor expansion of
    * the cost function) is correct. Step size regulators determine the magnitude
    * of the step at each optimization iteration. We have three step size
    * regulators: for the rates $\lambda_i$, for $R$ and for $Q$. By choosing
    * three different step size regulators for each group of parameters we make
    * sure that we can choose optimal gradient scaling for each group of parameters.
    * This compensates for the fact that the second order derivatives (or their
    * approximations) provide suboptimal scaling of the gradient by either over-
    * or underestimating the necessary scale of gradient step. The factor by which
    * the scaling is under- or overestimated is typically different for different
    * groups of parameters. Finally, because our optimization uses stochastic
    * approximation of derivatives, there is some noise in the gradients.
    * To account for this fact we two methods could be used. First option is the
    * third scaling factor, the noise attenuator {@link #eta}, which is a positive
    * constant smaller than one.
    * In the case  {@link #eta} is smaller than 1, only a portion of the gradient is
    * used during each iteration. As the number of Monte-Carlo samples used to approximate the
    * derivatives reduces, the (absolute) value of the noise attenuator must also be reduced.
    * The second option is to use the stochastic approximation approach with the step size
    * reduction sequence of the form $t^{-\alpha}$, where $t$ is
    * the iteration number and $\alpha$ is the number between $1/2$ and $1$. In our implementation
    * we have parameter {@link #pwr} with default value $5/6$ that determines the speed of
    * decay of the step sequence and is equivalent to $\alpha$.
    * @param SolInit the value of the solution at first iteration
    * @return xiter the MLE estimator of the parameters of the doubly Gamma-Poisson process
    */
   private double[] startGradUncTrustRegion (double[] SolInit) {
      double[] xiter = new double[numPeriods + 2];
      double[] xiter_new = new double[numPeriods + 2];
      int iteration = 0;
      double[] Rspec = new double[3];
      double[] fNewSpec = new double[3];
      double fNew = 1;
      double f = 0;

      // Initialize the iteration vector and check if the initialization provides admissible values
      System.arraycopy(SolInit, 0, xiter, 0, numPeriods + 2);
      for (int i = 0; i < numPeriods + 2; i++) {
         if (xiter[i] <= 0) {
            xiter[i] = 1e-1;
         }
      }
      // Configure parameters
      Rspec[0] = Rinit;
      Rspec[1] = Rinit;
      Rspec[2] = Rinit;
      while ( (iteration < maxit) && (Math.abs(fNew - f) > tol) ) {
         double[] O = new double[2 * (numPeriods + 2) + 1];
         double[] H = new double[numPeriods + 2];
         double[] G = new double[numPeriods + 2];
         double[] s = new double[numPeriods + 2];
         double rho;
         double[] rhoSpec = new double[3];
         double[] E = new double[3];
         double temp;

         // Calculate current value of the likelihood function and its derivatives
         O = getLikelihoodDerivativesDoublyGamma (xiter, xiter[numPeriods], xiter[numPeriods + 1], "All");
         f = O[0];
         System.arraycopy(O, 1, G, 0, numPeriods + 2);
         System.arraycopy(O, numPeriods + 3, H, 0, numPeriods + 2);
         // Calculate preliminary update and scale it using step size regulators
         for (int i = 0; i < numPeriods + 2; i++) {
            if ( Math.abs(H[i]) > 0 ) {
               s[i] = G[i] / H[i];
            } else {
               s[i] = G[i];
            }
         }
         for (int i = 0; i < numPeriods; i++) {
            s[i] *= Rspec[0];
         }
         s[numPeriods] *= Rspec[1];
         s[numPeriods + 1] *= Rspec[2];

         for (int i = 0; i < numPeriods + 2; i++) {
            x[iteration][i] = xiter[i];
         }
         x[iteration][numPeriods + 2] = f;
         x[iteration][numPeriods + 3] = Rspec[0];
         x[iteration][numPeriods + 4] = Rspec[1];
         x[iteration][numPeriods + 5] = Rspec[2];


         // Calculate preliminary updated parameters
         for (int i = 0; i < numPeriods + 2; i++) {
            xiter_new[i] = xiter[i] - s[i];
         }
         // Check if update resulted in admissible values, if not return to previous value
         temp = 0;
         for (int i = 0; i < numPeriods + 2; i++) {
            if (xiter_new[i] <= 0) {
               xiter_new[i] = xiter[i];
            }
            temp += s[i] * G[i];
         }
         E[1] = s[numPeriods] * G[numPeriods];
         E[2] = s[numPeriods + 1] * G[numPeriods + 1];
         E[0] = Math.abs(temp - E[2] - E[1]);
         E[2] = Math.abs(E[2]);
         E[1] = Math.abs(E[1]);
         // Calculate updated value of cost function and trust region quality metric
         O = getLikelihoodDerivativesDoublyGamma (xiter_new, xiter_new[numPeriods], xiter_new[numPeriods + 1], "CostOnly");
         fNew = O[0];
         rho = (fNew - f) / Math.abs(temp);
         // Calculate partially updated likelihood funtion and partial trust region quality metrics
         O = getLikelihoodDerivativesDoublyGamma (xiter_new, xiter[numPeriods], xiter[numPeriods + 1], "CostOnly");
         fNewSpec[0] = O[0];
         O = getLikelihoodDerivativesDoublyGamma (xiter, xiter_new[numPeriods], xiter[numPeriods + 1], "CostOnly");
         fNewSpec[1] = O[0];
         O = getLikelihoodDerivativesDoublyGamma (xiter, xiter[numPeriods], xiter_new[numPeriods + 1], "CostOnly");
         fNewSpec[2] = O[0];
         // Adjut step regulators based on partial trust region quality metrics
         for (int i = 0; i < 3; i++) {
            rhoSpec[i] = (fNewSpec[i] - f) / E[i];
            if (rhoSpec[i] < c0) {
               Rspec[i] *= g0;
            } else if (rhoSpec[i] > c1) {
               Rspec[i] *= g1;
            }
         }
         // Make final update conditional on the trust region quality metric
         if (rho > c0) {
            for (int i = 0; i < numPeriods + 2; i++) {
               xiter_new[i] = xiter[i] - Math.pow(iteration, -pwr) * eta * s[i];
               if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
                  xiter[i] = xiter_new[i];
               }
            }
         }

         iteration += 1;
      }


      return xiter;
   }
   
   /**
    * Implements the trust region Gauss-Newton maximizer of the likelihood of 
    * the doubly Gamma-Poisson process. In this algorithm the expressions for 
    * the second order derivatives (or their approximations having reduced 
    * number of terms) are used as pre-scalers for the gradients. Moreover, 
    * there is also a set of step size regulators that reflect the size of the 
    * trust region, the region in which we believe our model of the cost function
    * (the locally linear model based on the first order Taylor expansion of 
    * the cost function) is correct. Step size regulators determine the magnitude 
    * of the step at each optimization iteration. We have three step size 
    * regulators: for the rates $\lambda_i$, for $R$ and for $Q$. By choosing 
    * three different step size regulators for each group of parameters we make 
    * sure that we can choose optimal gradient scaling for each group of parameters. 
    * This compensates for the fact that the second order derivatives (or their 
    * approximations) provide suboptimal scaling of the gradient by either over- 
    * or underestimating the necessary scale of gradient step. The factor by which 
    * the scaling is under- or overestimated is typically different for different 
    * groups of parameters. Finally, because our optimization uses stochastic 
    * approximation of derivatives, there is some noise in the gradients. 
    * To account for this fact we two methods could be used. First option is the 
    * third scaling factor, the noise attenuator {@link #eta}, which is a positive 
    * constant smaller than one. 
    * In the case  {@link #eta} is smaller than 1, only a portion of the gradient is
    * used during each iteration. As the number of Monte-Carlo samples used to approximate the 
    * derivatives reduces, the (absolute) value of the noise attenuator must also be reduced. 
    * The second option is to use the stochastic approximation approach with the step size 
    * reduction sequence of the form $t^{-\alpha}$, where $t$ is
    * the iteration number and $\alpha$ is the number between $1/2$ and $1$. In our implementation
    * we have parameter {@link #pwr} with default value $5/6$ that determines the speed of 
    * decay of the step sequence and is equivalent to $\alpha$.
    * @param SolInit the value of the solution at first iteration
    * @return xiter the MLE estimator of the parameters of the doubly Gamma-Poisson process
    */
   public double[] startGradUncTrustRegionSpline (double[] SolInit) {
   double[] xiter = new double[2*numPeriods+1];
   double[] xiter_new = new double[2*numPeriods+1];
   int iteration = 0;
   double[] Rspec = new double[2+numPeriods];
   double[] fNewSpec = new double[2+numPeriods];
   double f=0;
   DoubleMatrix2D Rspline;
   DoubleMatrix2D invR;
   DoubleMatrix2D Q;
   
   x = new double[maxit][3*numPeriods+1+5];
   
   // Initialize the smoothing spline related variables
   Rspline = new DenseDoubleMatrix2D(numPeriods-2, numPeriods-2);
   Q = new DenseDoubleMatrix2D(numPeriods-2, numPeriods);
   // QRQ = new DenseDoubleMatrix2D(numPeriods, numPeriods);
   for (int i=0; i<numPeriods-2; i++){
       Rspline.set(i,i, 4.0);
       if (i<numPeriods-3){
           Rspline.set(i+1,i, 1.0);
           Rspline.set(i,i+1, 1.0);
       }
       Q.set(i,i, 3.0);
       Q.set(i,i+1, -6.0);
       Q.set(i,i+2, 3.0);
   }
   Algebra algebraObj = new Algebra();
   invR = algebraObj.inverse(Rspline); 
   Rspline = invR.zMult(Q, null, 1.0, 0.0, false, false);
   QRQ = Q.zMult(Rspline, null, 1.0, 0.0, true, false);
   
   // Initialize the iteration vector and check if the initialization provides admissible values
   System.arraycopy(SolInit, 0, xiter, 0, 2*numPeriods+1);
   for (int i = 0; i < 2*numPeriods+1; i++){
       if (xiter[i] <= 0){
           xiter[i]= 1e-1;
       }
   }
   // Configure parameters
   Rspec[0] = Rinit;
   for (int i=1; i<numPeriods+1; i++){
       Rspec[i] = Rinit;
   }
   Rspec[numPeriods+1] = Rinit;
   while ( (iteration<maxit) ){
       double[][] O = new double[7][numPeriods];
       double[] H = new double[2*numPeriods+1];
       double[] G = new double[2*numPeriods+1];
       double[] s = new double[2*numPeriods+1];
       double[] rhoSpec = new double[2+numPeriods];
       double[] E = new double[2+numPeriods];
       double   temp;
       double[] R = new double[numPeriods];
       
       // Calculate current value of the likelihood function and its derivatives
       System.arraycopy(xiter, numPeriods, R, 0, numPeriods);
       O = getLikelihoodDerivativesDoublyGammaSpline (xiter, R, xiter[2*numPeriods], "All");
       f = O[0][0];
       System.arraycopy(O[1], 0, G, 0, numPeriods);
       System.arraycopy(O[3], 0, G, numPeriods, numPeriods);
       G[2*numPeriods] = O[5][0];
       System.arraycopy(O[2], 0, H, 0, numPeriods);
       System.arraycopy(O[4], 0, H, numPeriods, numPeriods);
       H[2*numPeriods] = O[6][0];
       // Calculate preliminary update and scale it using step size regulators
       for (int i=0; i<numPeriods; i++){
           s[i] = Rspec[0] * G[i];
           s[numPeriods+i] = Rspec[i+1] * G[numPeriods+i] ;
       }
       s[2*numPeriods] = Rspec[numPeriods+1] * G[2*numPeriods];
       
       for (int i=0; i<2*numPeriods+1; i++){
           x[iteration][i] = xiter[i];  
       }
       x[iteration][2*numPeriods+1] = f;
       x[iteration][2*numPeriods+2] = Rspec[0];
       for (int i=0; i<numPeriods; i++){
           x[iteration][2*numPeriods+3+i] = Rspec[i+1];
       }
       x[iteration][3*numPeriods+3] = Rspec[numPeriods+1];
       
       
       // Calculate preliminary updated parameters
       for (int i=0; i<2*numPeriods+1; i++){
           xiter_new[i] = xiter[i] + s[i];
       }
       // Check if update resulted in admissible values, if not return to previous value
       for (int i = 0; i < 2*numPeriods+1; i++){
           if (xiter_new[i] <= 0){
               xiter_new[i]= xiter[i];
           }
       }
       for (int i = 0; i < numPeriods; i++){
           E[0] += s[i] * G[i];
           E[i+1] += Math.abs(s[numPeriods+i] * G[numPeriods+i]);
       }
       E[numPeriods+1] = Math.abs(s[2*numPeriods] * G[2*numPeriods]);
       E[0] = Math.abs(E[0]);
       // Calculate partially updated likelihood funtion and partial trust region quality metrics
       System.arraycopy(xiter, numPeriods, R, 0, numPeriods);
       O = getLikelihoodDerivativesDoublyGammaSpline (xiter_new, R, xiter[2*numPeriods], "CostOnly");
       fNewSpec[0] = O[0][0];
       O = getLikelihoodDerivativesDoublyGammaSpline (xiter, R, xiter_new[2*numPeriods], "CostOnly");
       fNewSpec[1+numPeriods] = O[0][0];
       for (int i=0; i<numPeriods; i++){
           R[i] = xiter_new[numPeriods+i];
           O = getLikelihoodDerivativesDoublyGammaSpline (xiter, R, xiter[2*numPeriods], "CostOnly");
           fNewSpec[i+1] = O[0][0];
           R[i] = xiter[numPeriods+i];
       }
       
       // Adjust step regulators based on partial trust region quality metrics
       for (int i = 0; i < numPeriods+2; i++){
           rhoSpec[i] = (fNewSpec[i] - f) / E[i];
           if (rhoSpec[i] < c0){
               Rspec[i] *= g0;
           } else if (rhoSpec[i] > c1) {
               Rspec[i] *= g1;
           }
       }
       double Mnoj;
       if (iteration > Math.round(maxit/2)){
           Mnoj = Math.pow(iteration - Math.round(maxit/2), -pwr);
       } else{
           Mnoj = 1.0;
       }
       if (rhoSpec[0] > c0){
           for (int i=0; i<numPeriods; i++){
               xiter_new[i] = xiter[i] + Mnoj * eta * s[i];
               if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
                   xiter[i] = xiter_new[i];
               } 
           }
       }
       for (int i=0; i<numPeriods; i++){
           if (rhoSpec[i+1] > c0){
               xiter_new[numPeriods+i] = xiter[numPeriods+i] + Mnoj * eta * s[numPeriods+i];
               if ((xiter_new[numPeriods+i] > 0) && !Double.isNaN(xiter_new[numPeriods+i]) && !Double.isInfinite(xiter_new[numPeriods+i]) ) {
                   xiter[numPeriods+i] = xiter_new[numPeriods+i];
               } 
           }
       }
       if (rhoSpec[numPeriods+1] > c0){
           int i=2*numPeriods;
           xiter_new[i] = xiter[i] + Mnoj * eta * s[i];
           if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
               xiter[i] = xiter_new[i];
           } 
       }
       
       iteration += 1;
   }

       
   return xiter;
   }
   
   
   /**
    * Implements the trust region Gauss-Newton maximizer of the likelihood of 
    * the doubly Gamma-Poisson process. In this algorithm the expressions for 
    * the second order derivatives (or their approximations having reduced 
    * number of terms) are used as pre-scalers for the gradients. Moreover, 
    * there is also a set of step size regulators that reflect the size of the 
    * trust region, the region in which we believe our model of the cost function
    * (the locally linear model based on the first order Taylor expansion of 
    * the cost function) is correct. Step size regulators determine the magnitude 
    * of the step at each optimization iteration. We have three step size 
    * regulators: for the rates $\lambda_i$, for $R$ and for $Q$. By choosing 
    * three different step size regulators for each group of parameters we make 
    * sure that we can choose optimal gradient scaling for each group of parameters. 
    * This compensates for the fact that the second order derivatives (or their 
    * approximations) provide suboptimal scaling of the gradient by either over- 
    * or underestimating the necessary scale of gradient step. The factor by which 
    * the scaling is under- or overestimated is typically different for different 
    * groups of parameters. Finally, because our optimization uses stochastic 
    * approximation of derivatives, there is some noise in the gradients. 
    * To account for this fact we two methods could be used. First option is the 
    * third scaling factor, the noise attenuator {@link #eta}, which is a positive 
    * constant smaller than one. 
    * In the case  {@link #eta} is smaller than 1, only a portion of the gradient is
    * used during each iteration. As the number of Monte-Carlo samples used to approximate the 
    * derivatives reduces, the (absolute) value of the noise attenuator must also be reduced. 
    * The second option is to use the stochastic approximation approach with the step size 
    * reduction sequence of the form $t^{-\alpha}$, where $t$ is
    * the iteration number and $\alpha$ is the number between $1/2$ and $1$. In our implementation
    * we have parameter {@link #pwr} with default value $5/6$ that determines the speed of 
    * decay of the step sequence and is equivalent to $\alpha$.
    * @param SolInit the value of the solution at first iteration
    * @return xiter the MLE estimator of the parameters of the doubly Gamma-Poisson process
    */
   public double[] startGradUncTrustRegionExtendedSpline (double[] SolInit) {
   double[] xiter = new double[3*numPeriods+1];
   double[] xiter_new = new double[3*numPeriods+1];
   int iteration = 0;
   double[] Rspec = new double[2+2*numPeriods];
   double[] fNewSpec = new double[2+2*numPeriods];
   double f=0;
   DoubleMatrix2D Rspline;
   DoubleMatrix2D invR;
   DoubleMatrix2D Q;
   
   x = new double[maxit][5*numPeriods+1+5 + 4*numPeriods];
   
   // Initialize the smoothing spline related variables
   Rspline = new DenseDoubleMatrix2D(numPeriods-2, numPeriods-2);
   Q = new DenseDoubleMatrix2D(numPeriods-2, numPeriods);
   QRQ = new DenseDoubleMatrix2D(numPeriods, numPeriods);
   for (int i=0; i<numPeriods-2; i++){
       Rspline.set(i,i, 4.0);
       if (i<numPeriods-3){
           Rspline.set(i+1,i, 1.0);
           Rspline.set(i,i+1, 1.0);
       }
       Q.set(i,i, 3.0);
       Q.set(i,i+1, -6.0);
       Q.set(i,i+2, 3.0);
   }
   Algebra algebraObj = new Algebra();
   invR = algebraObj.inverse(Rspline); 
   Rspline = invR.zMult(Q, null, 1.0, 0.0, false, false);
   QRQ = Q.zMult(Rspline, null, 1.0, 0.0, true, false);
   
   // Initialize the iteration vector and check if the initialization provides admissible values
   System.arraycopy(SolInit, 0, xiter, 0, 3*numPeriods+1);
   for (int i = 0; i < 2*numPeriods+1; i++){
       if (xiter[i] <= 0){
           xiter[i]= 1e-1;
       }
   }
   for (int i = 2*numPeriods+1; i < 3*numPeriods+1; i++){
       if (xiter[i] <= 0){
           xiter[i]= 1.0;
       }
   }
   // Configure parameters
   Rspec[0] = Rinit;
   for (int i=1; i<numPeriods+1; i++){
       Rspec[i] = Rinit;
       Rspec[numPeriods+1+i] = Rinit;
   }
   Rspec[numPeriods+1] = Rinit;
   
   while ( (iteration<maxit) ){
       double[][] O = new double[8][numPeriods];
       double[] G = new double[3*numPeriods+1];
       double[] s = new double[3*numPeriods+1];
       double[] rhoSpec = new double[2+2*numPeriods];
       double[] E = new double[2+2*numPeriods];
       double[] R = new double[numPeriods];
       double[] P = new double[numPeriods];
       
       // Calculate current value of the likelihood function and its derivatives
       System.arraycopy(xiter, numPeriods, R, 0, numPeriods);
       System.arraycopy(xiter, 2*numPeriods+1, P, 0, numPeriods);
       O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter, R, xiter[2*numPeriods], P, "All");
       f = O[0][0];
       System.arraycopy(O[1], 0, G, 0, numPeriods);
       System.arraycopy(O[3], 0, G, numPeriods, numPeriods);
       G[2*numPeriods] = O[5][0];
       System.arraycopy(O[7], 0, G, 2*numPeriods+1, numPeriods);
       // Calculate preliminary update and scale it using step size regulators
       for (int i=0; i<numPeriods; i++){
           s[i] = Rspec[0] * G[i];
           s[numPeriods+i] = Rspec[i+1] * G[numPeriods+i] ;
           s[2*numPeriods+1+i] = Rspec[numPeriods+2] * G[2*numPeriods+1+i] ; // Rspec[numPeriods+2+i]
       }
       s[2*numPeriods] = Rspec[numPeriods+1] * G[2*numPeriods];
       
       
       for (int i=0; i<3*numPeriods+1; i++){
           x[iteration][i] = xiter[i];  
       }
       x[iteration][3*numPeriods+1] = f;
       x[iteration][3*numPeriods+2] = Rspec[0];
       for (int i=0; i<numPeriods; i++){
           x[iteration][3*numPeriods+3+i] = Rspec[i+1];
           x[iteration][4*numPeriods+4] = Rspec[numPeriods+2]; // x[iteration][4*numPeriods+4+i] = Rspec[numPeriods+2+i];
           
       }
       x[iteration][4*numPeriods+3] = Rspec[numPeriods+1];
       
       for (int i=0; i<3*numPeriods+1; i++){
           x[iteration][4*numPeriods+6+i] = G[i];
       }
       
       // Calculate preliminary updated parameters
       for (int i=0; i<3*numPeriods+1; i++){
           xiter_new[i] = xiter[i] + s[i];
       }
       // Check if update resulted in admissible values, if not return to previous value
       for (int i = 0; i < 3*numPeriods+1; i++){
           if (xiter_new[i] <= 0){
               xiter_new[i]= xiter[i];
           }
       }
       // Calculate the increase of cost function predicted by the linear model
       for (int i = 0; i < numPeriods; i++){
           E[0] += s[i] * G[i];
           E[i+1] += Math.abs(s[numPeriods+i] * G[numPeriods+i]);
           E[numPeriods+2] = s[2*numPeriods+1+i] * G[2*numPeriods+1+i]; // E[numPeriods+2+i] = Math.abs(s[2*numPeriods+1+i] * G[2*numPeriods+1+i]);
       }
       E[numPeriods+1] = Math.abs(s[2*numPeriods] * G[2*numPeriods]);
       E[0] = Math.abs(E[0]);
       E[numPeriods+2] = Math.abs(E[numPeriods+2]);
       // Calculate partially updated likelihood funtion and partial trust region quality metrics
       System.arraycopy(xiter, numPeriods, R, 0, numPeriods);
       O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter_new, R, xiter[2*numPeriods], P, "CostOnly");
       fNewSpec[0] = O[0][0];
       
       O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter, R, xiter_new[2*numPeriods], P, "CostOnly");
       fNewSpec[1+numPeriods] = O[0][0];
       
       for (int i=0; i<numPeriods; i++){
           R[i] = xiter_new[numPeriods+i];
           O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter, R, xiter[2*numPeriods], P, "CostOnly");
           fNewSpec[i+1] = O[0][0];
           R[i] = xiter[numPeriods+i];
       }
       
       System.arraycopy(xiter, numPeriods, R, 0, numPeriods);
       System.arraycopy(xiter_new, 2*numPeriods+1, P, 0, numPeriods);
       O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter, R, xiter[2*numPeriods], P, "CostOnly");
       fNewSpec[2+numPeriods] = O[0][0];
       /*  
       for (int i=0; i<numPeriods; i++){
           P[i] = xiter_new[2*numPeriods+1+i];
           O = getLikelihoodDerivativesExtendedDoublyGammaSpline (xiter, R, xiter[2*numPeriods], P, "CostOnly");
           fNewSpec[2+numPeriods+i] = O[0][0];
           P[i] = xiter[2*numPeriods+1+i];
       }
       */
       
       // Adjust step regulators based on partial trust region quality metrics
       for (int i = 0; i < numPeriods+3; i++){ // for (int i = 0; i < 2*numPeriods+2; i++){
           rhoSpec[i] = (fNewSpec[i] - f) / E[i];
           if (rhoSpec[i] < c0){
               Rspec[i] *= g0;
           } else if (rhoSpec[i] > c1) {
               Rspec[i] *= g1;
           }
       }
       double Mnoj;
       if (iteration > Math.round(maxit/2)){
           Mnoj = Math.pow(iteration - Math.round(maxit/2), -pwr);
       } else{
           Mnoj = 1.0;
       }
       if (rhoSpec[0] > c0){
           for (int i=0; i<numPeriods; i++){
               xiter_new[i] = xiter[i] + Mnoj * eta * s[i];
               if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
                   xiter[i] = xiter_new[i];
               } 
           }
       }
       for (int i=0; i<numPeriods; i++){
           if (rhoSpec[i+1] > c0){
               xiter_new[numPeriods+i] = xiter[numPeriods+i] + Mnoj * eta * s[numPeriods+i];
               if ((xiter_new[numPeriods+i] > 0) && !Double.isNaN(xiter_new[numPeriods+i]) && !Double.isInfinite(xiter_new[numPeriods+i]) ) {
                   xiter[numPeriods+i] = xiter_new[numPeriods+i];
               } 
           }
       }
       
       if ( rhoSpec[numPeriods+1] > c0 ){
           int i=2*numPeriods;
           xiter_new[i] = xiter[i] + Mnoj * eta * s[i];
           if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
               xiter[i] = xiter_new[i];
           } 
       }
       
       /*
       for (int i=0; i<numPeriods; i++){
           if ( (rhoSpec[numPeriods+2+i] > c0) && (OutType.equals("P") || (OutType.equals("QP"))) ) {
               xiter_new[2*numPeriods+1+i] = xiter[2*numPeriods+1+i] + Mnoj * eta * s[2*numPeriods+1+i];
               if ((xiter_new[2*numPeriods+1+i] > 0) && !Double.isNaN(xiter_new[2*numPeriods+1+i]) && !Double.isInfinite(xiter_new[2*numPeriods+1+i]) ) {
                   xiter[2*numPeriods+1+i] = xiter_new[2*numPeriods+1+i];
               } 
           }
       }
       */
       
       if (rhoSpec[numPeriods+2] > c0){
           for (int i=2*numPeriods+1; i<3*numPeriods+1; i++){
               xiter_new[i] = xiter[i] + Mnoj * eta * s[i];
               if ((xiter_new[i] > 0) && !Double.isNaN(xiter_new[i]) && !Double.isInfinite(xiter_new[i]) ) {
                   xiter[i] = xiter_new[i];
               } 
           }
       }
       
       
       
       iteration += 1;
   }

       
   return xiter;
   }
   


   /**
    * Sets the maximum number of iterations in the trust region optimization algorithm
    * @param Value maximum number of iterations in the trust region optimization algorithm
    */
   public void setTrustRegionMaxIterations (int Value) {
      maxit = Value;
   }
   /**
    * Sets the lower and higher bounds on the quality metrics of the trust region.
    * If the quality metric has value lower than {@link #c0} then the size of the trust region
    * (the step size regulator) will be reduced. If the quality metric has value higher than
    * {@link #c1} then the size of the trust region (the step size regulator) will be increased.
    * @param LowQualBound is the lower bound
    * @param HighQualBound is the higher bound
    */
   public void setTrustRegionQualBounds (double LowQualBound, double HighQualBound) {
      c0 = LowQualBound;
      c1 = HighQualBound;
   }
   /**
    * Sets the multipliers that control the rate at which the step size regulator is increased ({@link #g1})
    * or decreased ({@link #g0}). If the quality metric has value lower than {@link #c0} then the size of
    * the trust region (the step size regulator) is multiplied by {@link #g0}. If the quality
    * metric has value higher than {@link #c1} then the size of the trust region
    * (the step size regulator) will be multiplied by {@link #g1}. IncreaseMultiplier must be greater than 1,
    * DecreaseMultiplier must be between 0 and 1.
    * @param IncreaseMultiplier
    * @param DecreaseMultiplier
    */
   public void setTrustRegionMultipliers (double IncreaseMultiplier, double DecreaseMultiplier) {
      if ((DecreaseMultiplier <= 1) && (DecreaseMultiplier > 0)) {
         g0 = DecreaseMultiplier;
      }
      if (IncreaseMultiplier > 1) {
         g1 = IncreaseMultiplier;
      }
   }
   /**
    * Sets the threshold for the tolerance of the trust region optimization algorithm.
    * If the difference between the updated and the previous values of the cost
    * function is greater than this value, algorithm stops and returns current values
    * of parameters as a solution.
    * @param Value the value of the stopping criterion
    */
   public void setTrustRegionTol (double Value) {
      tol = Value;
   }
   /**
    * Sets the value of the noise attenuator for the trust region approach. The optimization
    * algorithm is based on stochastic derivatives. Because of this derivatives contain
    * noise and to reduce its adverse effects we use only portion of the gradient to
    * during each update. As the number of Monte-Carlo samples used to approximate the
    * derivatives reduces, the (absolute) value of the noise attenuator must also be reduced.
    * @param Value the value of the noise attenuator
    */
   public void setTrustRegionNoiseAttenuator (double Value) {
      eta = Value;
   }
   /**
    * Sets the initial size of the trust region (initial value of the step size regulator).
    * This value should be reasonably small to prevent the algorithm from making unreasonably
    * large initial steps at the beginning, when the optimal scaling for the gradients is not known.
    * As the optimization progresses, the step size regulator grows (if necessary) fast at the
    * geometric rate and the optimal scaling for the derivatives is quickly learnt.
    * @param Value the initial value of the step size regulator.
    */
   public void setTrustRegionInitBoundary (double Value) {
      Rinit = Value;
   }
   /**
    * Sets the power law in the step size stochastic approximation annealing sequence.
    * @param Value
    */
   public void setTrustRegionAnnealingPwr (double Value) {
      pwr = Value;
   }
   /**
     * Sets the smoothing parameter for the use with the smoothing spline doubly gamma penalized MLE.
     * @param lambda smoothing parameter in the interval [0, 1]. When this parameter is equal to 1, the
     * smoothing spline is not used and shape parameters are assumed to be independent 
     * across sub-periods. Default value 0.99.
     */
    public void setSmoothingLambda(double lambda) {
        this.smoothingLambda = lambda;
    }
    /**
     * Sets the number of sub-periods over which to average the MME estimate in getMMEdoublyGammaGeneral.
     * Without averaging the MME estimates are too noisy. If we want no averaging we can set movWindSize=1. 
     * Default value 5. 
     * @param movWindSize number of sub-periods over which to average the MME estimate.
     */
    public void setMovingWindowSize(int movWindSize) {
        this.movWindSize = movWindSize;
    }
    
    public void setMaxIter(int maxit) {
   	 this.maxit = maxit;
   }
    
   /**
    * Returns the the matrix of the optimization trace containing the evolution
    * of parameters during optimization iterations.
    */
   public double[][] getOptimizationTrace () {
      return x;
   }
   /**
    * Calculates the estimates of the parameters based on the stochastic optimization framework
    * described by B T Polyak and A B Juditsky in "Acceleration of Stochastic Approximation by Averaging",
    * SIAM J Control Optim. 30(4) 838???855. For the proper use of this option the
    * trust region optimization algorithm must be configured correspondingly. In particular,
    * recommended settings for parameters are {@link #Rinit} 0.1, {@link #eta} 0.3  and {@link #pwr 5/6}.
    *
    * @return The average of the optimization trace over iterations.
    */
   public double[] getPolyakAverage () {
      double[] Output = new double[numPeriods + 2];

      for (int j = 0; j < numPeriods + 2; j++) {
         for (int i = 0; i < maxit; i++) {
            Output[j] += x[i][j];
         }
         Output[j] /= maxit;
      }
      return Output;
   }

   /*
    * First version of root finding. Boris says we can remove it
    */
   /*
   public void estimateNortaRateParamsRootFinding ( )
   {
      final double[] gammaParams = new double[2 * numPeriods];
      final int[] arrivalsp = new int[numObs];
      double[][] yTrans = new double[numObs][numPeriods];
      double[] yTransVar = new double[numPeriods];
      double[] yTransMean = new double[numPeriods];
      double[][] yTransCorr = new double[numPeriods][numPeriods];
      double[][] CountsToRates = new double[numPeriods][numPeriods];

      NegativeBinomialDist NB = new NegativeBinomialDist(1.0, 1.0);

      for (int p = 0; p < numPeriods; p++) {

         for (int i = 0; i < numObs; i++)
            arrivalsp[i] = arrivals[i][p];
         final double[] gpar = getNegBinMLE(arrivalsp, 1e-6);

         gammaParams[2*p] = gpar[0];
         gammaParams[2*p + 1] = gpar[1];

         Qout[p] = gpar[0];
         LamOut[p] = (1 - gpar[1]) / gpar[1] * gpar[0];

         NB.setParams(gpar[0], gpar[1]);
         for (int i = 0; i < numObs; i++) {
            yTrans[i][p] = NB.cdf(arrivals[i][p]);
            yTransMean[p] += yTrans[i][p];
         }
         yTransMean[p] /= numObs;
      }

      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            for (int j = 0; j < numObs; j++) {
               yTransCorr[i][k] += (yTrans[j][i] - yTransMean[i]) * (yTrans[j][k] - yTransMean[k]);
            }
            yTransCorr[i][k] /= numObs;
         }
         yTransVar[k] = yTransCorr[k][k];
      }

      // Compute Spearman correlation
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            yTransCorr[i][k] /= Math.sqrt(yTransVar[k] * yTransVar[i]);
         }
      }
      // Compute correction factors to take into account for the rate to count transition
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            if (i == k) {
               CountsToRates[i][k] = 1;
            } else {
               CountsToRates[i][k] = Math.sqrt( (1 + Qout[i] / LamOut[i]) * (1 + Qout[k] / LamOut[k]) );
            }
         }
      }
      // Compute the Gaussian correlation matrix by applying the correction term and
      // using the root finding approach from Avramidis et al. that relates correlation
      // of the uniform with that of the Gaussian
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i <= k; i++) {
            double temp;
            temp = CountsToRates[i][k] * yTransCorr[i][k];
            if ( (k != i) && (temp >= 1) ) {
               temp = 0.999;
            }
            if (temp <= -1) {
               temp = -0.999;
            }
            if (i == k) {
               temp = 1;
            } else {
               double tr;
               double temp1;
               double temp2 = 0;
               int RhoEval = 1;

               DiscreteDistributionInt dist1 = new NegativeBinomialDist(Qout[k], Qout[k] / (Qout[k] + LamOut[k]));
               DiscreteDistributionInt dist2 = new NegativeBinomialDist(Qout[i], Qout[i] / (Qout[i] + LamOut[i]));
               tr = 1.0 - 1.0e-3;
               NI2b ni2bObj = new NI2b(temp, dist1, dist2, tr, 200, 1.0e-4);
               temp1 = ni2bObj.computeCorr();
               //while ((Math.abs(temp2-temp1) > 1e-12) && (RhoEval < 20)){
               //    NI2b ni2bObj_1 = new NI2b(temp, dist1, dist2, tr, 5 + 20 * RhoEval, 1.0e-4);
               //    temp2 = ni2bObj_1.computeCorr();
               //    RhoEval += 1;
               //    temp1=temp2;
               //}
               temp = temp1;

            }
            yGaussCorr[i][k] = temp;
            yGaussCorr[k][i] = temp;
         }
      }
   }*/

   /**
    * Estimates the parameters of the Gamma-Poisson NORTA model for rates using
    * stochastic root finding approach. It stores the estimated Gaussian copula
    * correlation matrix in {@link #yGaussCorr}, the estimated base rates in {@link #Qout}
    * and the parameter of the Gamma distribution in {@link #LamOut}.
    * These quantities can be accessed using methods {@link #getNortaRateGaussCorr},
    * {@link #getNortaRateGammaShape} and {@link #getNortaRateGammaScale}.
    * The parameters of the Gamma distribution are estimated using method {@link #getNegBinMLE}.
    * The entries of the copula correlation matrix are estimated using
    * method {@link #getNortaRhoStochasticRootFinding}.
    *
    */
   public void estimateNortaRateParamsStochasticRootFinding () {
      final double[] gammaParams = new double[2 * numPeriods];
      final int[] arrivalsp = new int[numObs];
      double[][] yTrans = new double[numObs][numPeriods];
      double[] yTransVar = new double[numPeriods];
      double[] yTransMean = new double[numPeriods];
      double[][] yTransCorr = new double[numPeriods][numPeriods];

      NegativeBinomialDist NB = new NegativeBinomialDist(1.0, 1.0);

      for (int p = 0; p < numPeriods; p++) {

         for (int i = 0; i < numObs; i++) {
            arrivalsp[i] = arrivals[i][p];
         }
         final double[] gpar = getNegBinMLE(arrivalsp, 1e-6);

         gammaParams[2*p] = gpar[0];
         gammaParams[2*p + 1] = gpar[1];

         LamOut[p] = gpar[0];
         Qout[p] = gpar[1] / (1 - gpar[1]);
     /*    if (gpar[0] <= 0.) {
            for (int i = 0; i < numObs; i++) {
               yTrans[i][p] = 0.;
            }
            yTransMean[p] = 0.;
         } else {*/
            NB.setParams(gpar[0], gpar[1]);
            for (int i = 0; i < numObs; i++) {
               yTrans[i][p] = NB.cdf(arrivals[i][p]);
               yTransMean[p] += yTrans[i][p];
            }
     //    }
         yTransMean[p] /= numObs;
      }

      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            for (int j = 0; j < numObs; j++) {
               yTransCorr[i][k] += (yTrans[j][i] - yTransMean[i]) * (yTrans[j][k] - yTransMean[k]);
            }
            yTransCorr[i][k] /= numObs;
         }
         yTransVar[k] = yTransCorr[k][k];
      }

      // Compute Spearman correlation
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i < numPeriods; i++) {
            yTransCorr[i][k] /= Math.sqrt(yTransVar[k] * yTransVar[i]);
         }
      }
      // Compute the Gaussian correlation matrix by using the stochastic root finding approach
      // implemeted in getNortaRhoStochasticRootFinding
      for (int k = 0; k < numPeriods; k++) {
         for (int i = 0; i <= k; i++) {
            double rhoTarget;
            double rhoGauss;
            double[][] NegBinParams = new double[2][2];

            rhoTarget = yTransCorr[i][k];

            if (i == k) {
               rhoGauss = 1;
            } else {
               NegBinParams[0][0] = LamOut[k];
               NegBinParams[1][0] = LamOut[i];
               NegBinParams[0][1] = Qout[k] / (Qout[k] + 1);
               NegBinParams[1][1] = Qout[i] / (Qout[i] + 1);
               rhoGauss = getNortaRhoStochasticRootFinding (rhoTarget, NegBinParams, rhoTarget);
            }
            yGaussCorr[i][k] = rhoGauss;
            yGaussCorr[k][i] = rhoGauss;
         }
      }
   }

   /**
    * Calculates the MLEs of parameters of the negative binomial distribution.
    *
    * @param X vector of observed counts
    * @param tole parameter search tolerance for the binary search
    *
    * @return Array of size 2 containing MLEs of the parameters of the negative binomial
    * distribution. First element of the array is the MLE of the number of failures.
    * Second element of the returned array is the MLE of the success probability.
    */
   public double[] getNegBinMLE (int[] X, double tole) {
      final double[] NegBinParams = new double[2];
      double fLeft;
      double fRight;
      double lam = 0;
      double rMin = 0;
      double rMax;
      double Trials = 0;

      for (int i = 0; i < numObs; i++) {
         lam += X[i];
      }
      lam /= numObs;

      rMax = lam;
      fLeft = getLogNegBinDer (X, lam, rMin);
      fRight = getLogNegBinDer (X, lam, rMax);
      while ((fRight > 0) && (Trials < 10) ) {
         rMax *= 5;
         fRight = getLogNegBinDer (X, lam, rMax);
         Trials += 1;
      }

      if (fRight >= 0) {
         NegBinParams[0] = rMax;
         NegBinParams[1] = 1 - lam / (lam + rMax);
         return NegBinParams;
      }

      double iter = 0;
      while ((rMax - rMin > tole) && (iter < 1000)) {
         double rC;
         double fC;

         rC = (rMin + rMax) / 2;
         fC = getLogNegBinDer (X, lam, rC);
         if (fC < 0) {
            rMax = rC;
            // fRight = fC;
         } else {
            rMin = rC;
            // fLeft = fC;
         }
         iter += 1;
      }
      NegBinParams[0] = (rMin + rMax) / 2;
      NegBinParams[1] = 1 - lam / (lam + NegBinParams[0]);

      return NegBinParams;
   }

   /**
    * Calculates the derivative of the log-likelihood function for the Negative binomial
    * distribution.
    *
    * @param X data values
    * @param Xmean mean of the data values
    * @param r parameter of the negative binomial distribution (number of failures)
    *
    * @return derivative of the log-likelihood function for the Negative binomial
    * distribution with respect to parameter r.
    */
   public double getLogNegBinDer (int[] X, double Xmean, double r) {
      double Output;

      if (r == 0) {
         return 1.0 / 0.0;
      }

      Output = -numObs * Num.digamma(r) + numObs * Math.log(r / (r + Xmean));
      for (int i = 0; i < numObs; i++) {
         Output += Num.digamma(X[i] + r);
      }
      return Output;
   }

   /**
    * Estimates the Spearman correlation coefficient of counts in the Gamma-Poisson NORTA model
    * for rates using Monte-Carlo simulation.
    *
    * @param NegBinParams Parameters of the marginal distribution. In the
    * Gamma-Poisson NORTA model marginal distributions are negative binomial
    * @param rhoGauss Correlation coefficient of the Gaussian copula
    * @return Estimated Spearman correlation coefficient of counts.
    */
   private double getRhoGammaPoisson(double[][] NegBinParams, double rhoGauss) {
      double rhoGP;
      double[] Lam = new double[2];
      double[] Q = new double[2];
      MultinormalCholeskyGen ngen;
      DoubleMatrix2D R;
      double[][] NormalArray = new double[numSamples][2];
      int[][] GammaPoissonArray = new int[numSamples][2];
      GammaDist[] gamDist = new GammaDist[2];
      NegativeBinomialDist[] NB = new NegativeBinomialDist[2];
      double[] Mean = new double[2];
      double Cov = 0;
      double[] Var = new double[2];

      Lam[0] = NegBinParams[0][0];
      Lam[1] = NegBinParams[1][0];
      Q[0] = NegBinParams[0][1] / (1 - NegBinParams[0][1]);
      Q[1] = NegBinParams[1][1] / (1 - NegBinParams[1][1]);

      // Initialize correlated Gaussian generator
      R = new DenseDoubleMatrix2D(2, 2);
      R.set(0, 0, 1.0);
      R.set(1, 1, 1.0);
      R.set(0, 1, rhoGauss);
      R.set(1, 0, rhoGauss);
      final NormalGen ngen1 = new NormalGen(GaussGenStream, new NormalDist());
      ngen = new MultinormalCholeskyGen (ngen1, new double[2], R);
      // Generate correlated gaussians
      ngen.nextArrayOfPoints(NormalArray, 0, numSamples);
      // Initialize the Gamma distributions
  //    if (Lam[0] <= 0.)
  //    	gamDist[0] = null;
  //    else
         gamDist[0] = new GammaDist(Lam[0], Q[0]);
  //    if (Lam[1] <= 0.)
  //    	gamDist[1] = null;
  //    else
         gamDist[1] = new GammaDist(Lam[1], Q[1]);
      // Transform gaussians to the uniforms
      // Transform uniforms to Gammas
      for (int i = 0; i < numSamples; i++) {
         for (int j = 0; j < 2; j++) {
            NormalArray[i][j] = NormalDist.cdf01 (NormalArray[i][j]);
            if (null != gamDist[j])
               NormalArray[i][j] = gamDist[j].inverseF(NormalArray[i][j]);
            else
            	NormalArray[i][j] = 0.;
         }
      }
      for (int i = 0; i < numSamples; i++) {
         for (int j = 0; j < 2; j++) {
           // PoissonGen poisRND;
            if (NormalArray[i][j] <= 0.) {
            	GammaPoissonArray[i][j] = 0;
            } else {
            //   poisRND = new PoissonGen(PoissonGenStream, NormalArray[i][j]);
            //   GammaPoissonArray[i][j] = poisRND.nextInt();
            	GammaPoissonArray[i][j] = PoissonGen.nextInt(PoissonGenStream, NormalArray[i][j]);
            }
         }
      }
      // Calculate the Spearman correlation coefficient
      // Initialize the negative binomial distribution
      NB[0] = new NegativeBinomialDist(NegBinParams[0][0], NegBinParams[0][1]);
      NB[1] = new NegativeBinomialDist(NegBinParams[1][0], NegBinParams[1][1]);
      for (int i = 0; i < numSamples; i++) {
         for (int j = 0; j < 2; j++) {
            NormalArray[i][j] = NB[j].cdf(GammaPoissonArray[i][j]);
            Mean[j] += NormalArray[i][j];
         }
      }
      Mean[0] /= numSamples;
      Mean[1] /= numSamples;
      for (int i = 0; i < numSamples; i++) {
         for (int j = 0; j < 2; j++) {
            Var[j] += (NormalArray[i][j] - Mean[j]) * (NormalArray[i][j] - Mean[j]);
         }
         Cov += (NormalArray[i][1] - Mean[1]) * (NormalArray[i][0] - Mean[0]);
      }
      rhoGP = Cov / (Math.sqrt(Var[0]) * Math.sqrt(Var[1]));

      return rhoGP;
      // return rhoGP;
   }

   /**
    *
    * Solves the problem of fitting the NORTA correlation coefficient to the empirical
    * Spearman correlation coefficient of counts in the Gamma-Poisson copula model
    * using stochastic root finding approach.
    *
    * @param rhoTarget the empirically observed Spearman correlation coefficient
    * of counts in the Gamma-Poisson copula model
    * @param NegBinParams estimated parameters of the marginal distribution,
    * which is Negative Binomial in the case of Gamma-Poisson copula model
    * @param rhoInit initial value of the NORTA correlation coefficient. Typically,
    * rhoInit = rhoTarget
    * @return Fitted NORTA correlation coefficient
    */
   public double getNortaRhoStochasticRootFinding (double rhoTarget, double[][] NegBinParams, double rhoInit) {
      double xiter;
      double xiter_new;
      int iteration = 1;
      double fNew = 1;
      double f = 0;

      // Initialize optimization track
      x = new double[maxit][4];


      if (rhoInit >= 1) {
         xiter = 0.999;
      } else if (rhoInit <= -1) {
         xiter = -0.999;
      } else {
         xiter = rhoInit;
      }
      while ( (iteration < maxit) && (Math.abs(fNew - f) > tol) ) {
         double s;

         f = fNew;
         fNew = getRhoGammaPoisson(NegBinParams, xiter);
         s = rhoTarget - fNew;

         x[iteration - 1][0] = xiter;
         x[iteration - 1][1] = fNew;
         x[iteration - 1][2] = s;
         x[iteration - 1][3] = Math.pow(iteration, -pwr) * eta;

         xiter_new = xiter + Math.pow(iteration, -pwr) * eta * s;

         if ((xiter_new <= 1.0) && (xiter_new >= -1.0) ) {
            xiter = xiter_new;
         }
         iteration += 1;
      }

      return xiter;
   }
   
   public double digamma(double x) {
       double st, st1;
       
       st = 1e-5 * x;
       st1 = 0.5e-5 * x;
       
       return (Num.lnGamma(x+st1) - Num.lnGamma(x-st1)) / st;
   }

   /**
    * Returns the estimated vector of $\alpha_G$ parameters of the Gamma distribution in the
    * compound Gamma-Poisson NORTA model for rates. The definition of vector $\alpha_G$
    * follows that introduced in {@link PiecewiseConstantPoissonArrivalProcess} so that the base rate
    * in subperiod $p$ is equal to $\alpha_{G,p} / \lambda_{G,p}$.
    * @return the estimated vector of $\alpha_G$
    */
   public double[] getNortaRateGammaShape() {
      return LamOut;
   }

   /**
   * Returns the estimated vector of $\lambda_G$ parameters of the Gamma distribution in the
   * compound Gamma-Poisson NORTA model for rates. The definition of vector $\lambda_G$
   * follows that introduced in {@link PiecewiseConstantPoissonArrivalProcess} so that the base rate
   * in subperiod $p$ is equal to $\alpha_{G,p} / \lambda_{G,p}$.
   * @return the estimated vector of $\lambda_G$ 
   */
   public double[] getNortaRateGammaScale() {
      return Qout;
   }

   /**
    * Returns the estimated copula correlation matrix for the Gamma-Poisson NORTA model for rates.
    * @return the estimated copula correlation matrix
    */
   public double[][] getNortaRateGaussCorr() {
      return yGaussCorr;
   }

   /**
    * Returns the estimated and corrected copula correlation matrix for the Gamma-Poisson
    * NORTA model for rates. The estimated correlation matrix, which is not necessarily
    * positive definite is transformed into a
    * positive definite matrix using the POSDEF algorithm described in \cite{tDAV82a}.
    *
    * @return the estimated and corrected copula correlation matrix
    */
   public double[][] getNortaRateGaussCorrCorrected() {
      CorrelationMatrixCorrector obj = new CorrelationMatrixCorrector(yGaussCorr);
      return obj.calcCorrectedR();
   }

   /**
    * Fits the single $\rho$ Markov linear model $r_j = b^j$ to the estimated
    * copula correlation matrix for the Gamma-Poisson  NORTA model for rates.
    *
    * @return $b$
    */
   public double getNortaRateGaussCorrFitMarkovSingleRho() {
      double[] ACF = new double[numPeriods - 1];

      for (int i = 0; i < numPeriods - 1; i++) {
         for (int j = i + 1; j < numPeriods; j++) {
            ACF[i] += yGaussCorr[j][j - i - 1];
         }
         ACF[i] /= (numPeriods - i - 1);
      }
      CorrelationMtxFitting obj = new CorrelationMtxFitting(ACF);
      return obj.fitMarkovSingleRho();
   }

   /**
    * Fits the general linear model $r_j = a b^j + c$ to the estimated
    * copula correlation matrix for the Gamma-Poisson  NORTA model for rates.
    * Returns a vector of length 3 with parameters $a$, $b$ and $c$.
    *
    * @return vector [$a$, $b$ and $c$]
    */
   public double[] getNortaRateGaussCorrFitGeneralLinear() {
      double[] ACF = new double[numPeriods - 1];

      for (int i = 0; i < numPeriods - 1; i++) {
         for (int j = i + 1; j < numPeriods; j++) {
            ACF[i] += yGaussCorr[j][j - i - 1];
         }
         ACF[i] /= (numPeriods - i - 1);
      }
      CorrelationMtxFitting obj = new CorrelationMtxFitting(ACF);
      return obj.fitMarkovGeneralLinear();
   }

}