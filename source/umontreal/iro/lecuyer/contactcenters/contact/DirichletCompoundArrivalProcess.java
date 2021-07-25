package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.Arrays;

import optimization.Uncmin_f77;
import optimization.Uncmin_methods;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.util.Num;
import umontreal.ssj.util.RootFinder;

/**
 * Represents a generalization of the non-homogeneous
 * Poisson process where the arrival rates are
 * generated from a Dirichlet compound negative
 * multinomial distribution \cite{tMOS63a}.
 * As proven in \cite{ccAVR04a}, if the arrival rate of a
 * Poisson process is a piecewise-constant function
 * of the simulation time given by
 * $\lambda(t)=B\lambda_{p(t)}$,
 * $B$ being a gamma-distributed busyness factor with
 * shape parameter $\gamma$, the
 * distribution of the vector $(A_1, \ldots, A_P)$ giving
 * the number of arrivals in each main period is
 * the negative multinomial with parameters
 * $(\gamma, \rho_1, \ldots, \rho_{P+1})$
 * \cite[page 292]{tJOH69a}, where
 * \begin{equation}
 * \rho_p=\frac{\lambda_p}{1 + \sum_{j=1}^P\lambda_j},\label{eq:rhow}
 * \end{equation}
 * for $p=1,\ldots,P$, and $\rho_{P+1}=1-\sum_{j=1}^P \rho_j$.
 *
 * This arrival process generalizes the previous process by
 * modeling $\mathbf{A}=(A_1,\ldots, A_P)$ with
 * a Dirichlet compound negative multinomial distribution \cite{tMOS63a}
 * instead of a negative multinomial.
 * In this model, the user specifies $\gamma$ as well as
 * $\alpha_1,\ldots,\alpha_{P+1}$.
 * At the beginning of each replication, when base arrival rates are needed,
 * the vector $(\rho_1,\ldots, \rho_{P+1})$
 * is generated from the Dirichlet distribution with parameters
 * $(\alpha_1, \ldots, \alpha_{P+1})$, and
 * the base arrival rates $\lambda_1,\ldots,\lambda_P$ are
 * determined by solving~(\ref{eq:rhow}).
 * This results in $\lambda_p=\rho_p/\rho_{P+1}$.
 * During preliminary and wrap-up periods, the base arrival rate
 * is set to 0.
 *
 * The inter-arrival times are generated using the rates
 * $B\lambda_p$, where $B$ is a busyness given by the user.
 * Note that this variability
 * factor should be gamma-distributed with shape parameter $\gamma$
 * and scale parameter 1
 * to remain consistent with the Dirichlet compound model.
 */
public class DirichletCompoundArrivalProcess extends
                                                PiecewiseConstantPoissonArrivalProcess {
    private double[] m_p;
    private DirichletGen dgen;

   /**
    * Constructs a new Dirichlet compound Poisson arrival process.
    * The constructed process uses the period-change event \texttt{pce},
    * creates contacts using the factory \texttt{factory}, and
    * uses the Dirichlet parameters \texttt{alphas}.
    * The random stream \texttt{stream} is used for the uniforms for
    * inter-arrival times, and \texttt{streamRates} is used for Dirichlet.
    @param pce the period-change event associated with this object.
    @param factory the factory creating contacts for this generator.
    @param alphas the values of the $\alpha_p$ parameters.
    @param stream the random number stream for the exponential variates.
    @param streamRates the random number stream for the Dirichlet compound
    arrival rates.
    @exception IllegalArgumentException if the number of main
    periods is not \texttt{alphas.length - 1}, or if one $\alpha_p$ value
    is negative or 0.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public DirichletCompoundArrivalProcess (PeriodChangeEvent pce,
                                           ContactFactory factory,
                                           double[] alphas,
                                           RandomStream stream,
                                           RandomStream streamRates) {
      super (pce, factory, new double[alphas.length+1], stream);
      if (alphas.length == getPeriodChangeEvent().getNumPeriods() - 1)
         dgen = new DirichletGen (streamRates, alphas);
      else {
         assert alphas.length < getPeriodChangeEvent().getNumPeriods() - 1;
         final double[] alph = new double[getPeriodChangeEvent().getNumPeriods() - 1];
         System.arraycopy (alphas, 0, alph, 0, alph.length);
         dgen = new DirichletGen (streamRates, alph);
      }
      m_p = new double[dgen.getDimension()];
   }

   /**
    * Returns the value of the $\alpha_p$ parameter
    * for the Dirichlet distribution.
    @param p the index of the parameter.
    @return the value of the parameter.
    */
   public double getAlpha (int p) {
      return dgen.getAlpha (p);
   }

   /**
    * Sets the Dirichlet parameters $\alpha_p$
    * for this object.
    @param alphas a new vector of parameters.
    @exception IllegalArgumentException if the length of \texttt{alphas}
    is smaller than $P+1$, where $P$ is the
    number of main periods, or if one or more $\alpha_p$ values
    are negative or 0.
    */
   public void setAlphas (double[] alphas) {
      final int nv = getPeriodChangeEvent().getNumPeriods() - 1;
      if (alphas.length < nv)
         throw new IllegalArgumentException
            ("Invalid number of Dirichlet parameters, needs " +
             nv + " values");
      if (alphas.length == nv)
         dgen = new DirichletGen (getRateStream(), alphas);
      else {
         assert alphas.length < nv;
         final double[] alph = new double[nv];
         System.arraycopy (alphas, 0, alph, 0, alph.length);
         dgen = new DirichletGen (getRateStream(), alph);
      }
   }

   /**
    * Returns the random stream used to generate
    * the rates for the Poisson arrival process.
    @return the random stream for the values of $\lambda_p$.
    */
   public RandomStream getRateStream() {
      return dgen.getStream();
   }

   /**
    * Changes the random stream used to generate
    * the rates for the Poisson arrival process to \texttt{streamRates}.
    @param streamRates the random number generator for the $\lambda_p$ values.
    @exception NullPointerException if the parameter is \texttt{null}.
    */
   public void setRateStream (RandomStream streamRates) {
      if (streamRates == null)
         throw new NullPointerException ("The given random stream for rates must not be null");
      dgen.setStream (streamRates);
   }

   private final void computeWs() {
      dgen.nextPoint (m_p);
      final double n = m_p[m_p.length - 1];
      final double[] lam = getLambdas();
      lam[0] = 0;
      lam[lam.length - 1] = 0;
      for (int i = 1; i < m_p.length; i++)
         lam[i] = m_p[i-1]/n;
      setLambdas (lam);
   }

   @Override
   public void init() {
      computeWs();
      super.init();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      sb.append (", Dirichlet parameters: {");
      final int d = dgen.getDimension();
      for (int i = 0; i < d; i++)
         sb.append (i > 0 ? ", " : "").append (dgen.getAlpha (i));
      sb.append ("}]");
      return sb.toString();
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      if (p < 0 || p > dgen.getDimension())
         throw new IllegalArgumentException ("Invalid period index");
      if (p == 0 || p == dgen.getDimension())
         // Preliminary or wrap-up periods
         return 0;
      // pp is the expected value of rho_p, which is alpha_p/alpha_0.
      // pn is the expected value of rho_{P+1}, which is alpha_{P+1}/alpha_0.
      // lambda is pp/pn, so alpha_0 is cancelled and we do not
      // need to compute it.
      return dgen.getAlpha (p - 1)/dgen.getAlpha (dgen.getDimension() - 1);
   }

   /**
    * Estimates the parameters of a Dirichlet compound
    * negative multinomial arrival process
    * with a
    * busyness factor following the
    * gamma$(\gamma,1)$ distribution
    * from the number of arrivals in the array
    * \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, and $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This returns the $\alpha_p$ Dirichlet
    * parameters, for $P=0,\ldots,P$, and stores the
    * gamma busyness parameter in {@link #s_bgammaParam}.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated Dirichlet parameters.
    */
   public static double[] getMLE (int[][] arrivals, int numObs, int numPeriods) {
      if (numObs <= 0)
         throw new IllegalArgumentException ("n <= 0");
      if (numPeriods <= 0)
         throw new IllegalArgumentException ("d <= 0");

      // Step 1. Guess initial values for gamma and
      // the alpha_p's.
      // For gamma, we assume that the number of arrivals
      // follows the negative multinomial distribution.
      // For the initial values of alpha_p,
      // the heuristic matches the theoritical
      // mean and variance for period p with
      // the empirical mean and variance.
      final double[] totalArrivals = new double[numObs];
      final double[] mean = new double[numPeriods];
      final double[] var = new double[numPeriods];

      int p, i, l;
      double M;

      // Ups_i = Sum_p x_ip
      for (i = 0; i < numObs; i++)
      {
         totalArrivals[i] = 0;
         for (p = 0; p < numPeriods; p++)
            totalArrivals[i] += arrivals[i][p];
      }

      // Calcul des moyennes et des variances
      final Tally tally = new Tally();
      for (p = 0; p < numPeriods; p++)
      {
         tally.init();
         for (i = 0; i < numObs; i++)
            tally.add (arrivals[i][p]);
         mean[p] = tally.average();
         var[p] = tally.variance();
      }

      // M = Max(Ups_j)
      M = totalArrivals[0];
      for (i = 1; i < numObs; i++)
         if (totalArrivals[i] > M)
            M = totalArrivals[i];

      Arrays.sort (totalArrivals);
      final EmpiricalDist dist = new EmpiricalDist (totalArrivals);
      final double Fl[] = new double[(int)M];
      for (l = 0; l < M; l++)
         Fl[l] = dist.barF (l + 1.0);

/*.
      double typsiz[] = new double[numPeriods + 3];
      double fscale[] = new double[2];
      int method[] = new int[2];
      int iexp[] = new int[2];
      int msg[] = new int[2];
      int itnlim[] = new int[2];
      int ndigit[] = {0, 5};
      int iagflg[] = {0, 1};
      int iahflg[] = new int[2];
      double dlt[] = new double[2];
      double gradtl[] = new double[2];
      double stepmx[] = new double[2];
      double steptl[] = new double[2];
*/
      final double[] xpls = new double[numPeriods + 3];
      final double[] theta = new double[numPeriods + 3];
      final double[] fpls = new double[numPeriods + 3];
      final double[] gpls = new double[numPeriods + 3];
      final int[] itrcmd = new int[2];
      final double[][] a = new double[numPeriods + 3][numPeriods + 3];
      final double[] udiag = new double[numPeriods + 3];

      final NegMultiGammaFunction f = new NegMultiGammaFunction (numObs, (int)M, totalArrivals, Fl);
      final DCNegMultiFunction system = new DCNegMultiFunction (arrivals, numObs, numPeriods);

      // Estimation of initial gamma
      theta[numPeriods+2] = RootFinder.brentDekker (1e-15, 1e9, f, 1e-5);

      double sumMeans = 0.0;
      double sumVars = 0.0;
      for (p = 0; p < numPeriods; p++)
      {
         sumMeans += mean[p] * (mean[p] + theta[numPeriods+2]) / theta[numPeriods+2];
         sumVars += var[p];
      }

      // Estimation of initial alpha_p's
      double c0 = sumVars / sumMeans;
      if (c0 < 1.1)
         c0 = 1.1;
      for (p = 1; p < numPeriods + 1; p++)
         theta[p] = mean[p-1] * (c0 + theta[numPeriods+2]) / (theta[numPeriods+2] * (c0 - 1.0));
      theta[numPeriods+1] = (2.0 * c0 + theta[numPeriods+2] - 1.0) / (c0 - 1.0);

      // Step 2. Maximization of the log-likelihood of the
      // Dirichlet compound negative multinomial density function
      // by minimizing the -log-likelihood with a penalty
      // for negative values of gamma or alpha_p.
      Uncmin_f77.optif0_f77 (numPeriods + 2, theta, system, xpls, fpls, gpls, itrcmd, a, udiag);
      /*Uncmin_f77.optif9_f77(d + 2, theta, system, typsiz, fscale, method, iexp, msg,
                            ndigit, itnlim, iagflg, iahflg, dlt, gradtl, stepmx, steptl,
                            xpls, fpls, gpls, itrcmd, a, udiag);*/

      final double[] parameters = new double[numPeriods + 2];
      s_bgammaParam = xpls[numPeriods+2];
      for (p = 0; p < numPeriods + 1; p++)
         parameters[p] = xpls[p+1];
      return parameters;
   }

   /**
    * Constructs a new arrival process with parameters
    * estimated by the maximum likelihood method based on
    * the \texttt{numObs} observations in array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * The number of arrivals
    * is considered to follow the
    * Dirichlet compound negative multinomial
    * distribution, and the $\gamma$ parameter
    * for the gamma busyness factor is stored in
    * {@link #s_bgammaParam}.
    * @param pce the period-change event marking the end of periods.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream to generate arrival times.
    * @param streamRates the random stream to generate Dirichlet vectors from.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the constructed arrival process.
    */
   public static DirichletCompoundArrivalProcess getInstanceFromMLE
     (PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
           RandomStream streamRates,
           int[][] arrivals, int numObs, int numPeriods) {
      final double[] alphas = getMLE (arrivals, numObs, numPeriods);
      final DirichletCompoundArrivalProcess dap = new DirichletCompoundArrivalProcess
         (pce, factory, alphas, stream, streamRates);
      dap.setNormalizing (true);
      return dap;
   }

   /**
    * Computes the likelihhod for the initial
    * gamma constant, estimated by the negative
    * multinomial model.
    */
   private static final class NegMultiGammaFunction implements MathFunction {
      protected double Fl[];
      protected double totalArrivals[];
      protected int numObs;
      protected int M;
      protected int sumUps;

      public NegMultiGammaFunction (int numObs, int m, double totalArrivals[], double Fl[])
      {
         this.numObs = numObs;
         M = m;

         this.Fl = new double[Fl.length];
         System.arraycopy (Fl, 0, this.Fl, 0, Fl.length );
         this.totalArrivals = new double[totalArrivals.length];
         System.arraycopy (totalArrivals, 0, this.totalArrivals, 0, totalArrivals.length);

         sumUps = 0;
         for (final double arv : totalArrivals)
            sumUps += arv;
      }

      public double evaluate (double gamma)
      {
         double sum = 0.0;
         for (int l = 0; l < M; l++)
            sum += Fl[l] / (gamma + l);
         return sum - Math.log1p (sumUps / (numObs * gamma));
      }
   }

   /**
    * Implements the function for the log-likelihhod of
    * the Dirichlet compound negative multinomial density function.
    */
   private static final class DCNegMultiFunction implements Uncmin_methods {
      private int numObs;
      private int numPeriods;
      private int[] totalArrivals;
      private int[][] arrivals;

      public DCNegMultiFunction (int[][] arrivals, int numObs, int numPeriods)
      {
         this.numObs = numObs;
         this.numPeriods = numPeriods;
         this.arrivals = new int[numObs][numPeriods];
         totalArrivals = new int[numObs];

         for (int i = 0; i < numObs; i++)
         {
            totalArrivals[i] = 0;
            for (int p = 0; p < numPeriods; p++)
            {
               this.arrivals[i][p] = arrivals[i][p];
               totalArrivals[i] += arrivals[i][p];
            }
         }
      }

      public double f_to_minimize (double[] theta) {
      	final double PENALTY = 1.0e100;
          final double r = theta[numPeriods + 2];
         if (r <= 0.0)
            return PENALTY;
         double a = 0.0;
         double sumGYr = 0.0;
         double sumGXBeta = 0.0;
         double sumGYra = 0.0;
         double sumGBeta = 0.0;
         int p;

         for (p = 1; p < numPeriods + 2; p++) {
            if (theta[p] <= 0.0)
               return PENALTY;
            a += theta[p];
            sumGBeta += Num.lnGamma (theta[p]);
         }

         for (int i = 0; i < numObs; i++) {
            sumGYr += Num.lnGamma (totalArrivals[i] + r);
            sumGYra += Num.lnGamma (totalArrivals[i] + r + a);
            for (p = 0; p < numPeriods; p++)
               sumGXBeta += Num.lnGamma (arrivals[i][p] + theta[p+1]);
         }

         double y = -sumGYr + numObs * Num.lnGamma (r) - numObs * Num.lnGamma (a) - sumGXBeta -
                    numObs * Num.lnGamma (r + theta[numPeriods+1]) + numObs * sumGBeta + sumGYra;

         return y;
      }

      public void gradient (double[] theta, double[] g) {
/*
         double r = theta[numPeriods+2];
         double a = 0.0;
         double sumPsiYr = 0.0;
         double sumPsiYar = 0.0;
         double sumPsiBeta = 0.0;
         double sumPsiXBeta = 0.0;

         for (int p = 1; p < numPeriods + 2; p++)
         {
            a += theta[p];
            sumPsiBeta +=  Num.digamma(theta[p]);
         }

         for (int i = 0; i < numObs; i++)
         {
            sumPsiYr += Num.digamma (totalArrivals[i] + r);
            sumPsiYar += Num.digamma (r + totalArrivals[i] + a);

            for (int p = 0; p < numPeriods; p++)
            {
               sumPsiXBeta += Num.digamma(arrivals[i][p] + theta[p+1]);
            }
         }

         for (int p = 1; p <= numPeriods; p++)
            g[p] = - numObs * Num.digamma(a) - sumPsiXBeta + numObs * sumPsiBeta + sumPsiYar;

         g[numPeriods+1] = - numObs * Num.digamma(a) - numObs * Num.digamma(r + theta[numPeriods+1]) +
                  numObs * sumPsiBeta + sumPsiYar;
         g[numPeriods+2] = - sumPsiYr + numObs * Num.digamma (r) - numObs * Num.digamma (r + theta[numPeriods+1])
                  + sumPsiYar;
*/
      }

      public void hessian (double[] theta, double[][] h) {}
   }
}
