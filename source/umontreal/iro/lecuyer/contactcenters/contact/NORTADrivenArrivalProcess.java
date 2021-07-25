package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.probdist.NegativeBinomialDist;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.randvarmulti.MultinormalCholeskyGen;
import umontreal.ssj.rng.RandomStream;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Represents an arrival process in which the numbers of arrivals
 * per-period are correlated negative binomial
 * random variables, generated using the NORTA method.
 * To generate the number of arrivals, the process first obtains a vector
 * $\boldX=(X_1,\ldots,X_P)$
 * from the multivariate normal distribution with
 * mean vector $\mathbf{0}$ and covariance matrix
 * $\boldSigma$.  Assuming that $\boldSigma$
 * is a correlation matrix, i.e., each element
 * is in $[-1, 1]$ and 1's are on its diagonal,
 * the vector of uniforms
 * $\mathbf{U}=(\Phi(X_1), \ldots, \Phi(X_P))$ is obtained, where
 * $\Phi(x)$ is the distribution function of a standard normal variable.
 * For main period~$p$, the marginal probability distribution
 * for $A_p$
 * is assumed to be negative binomial with parameters
 * $\gamma_p$ and $\rho_p$,
 * $\gamma_p$ being a positive number and $0<\rho_p< 1$.
 * $A_0$ and $A_{P+1}$, the number of arrivals during the
 * preliminary and the wrap-up periods, respectively, are always 0
 * for this process.
 *
 * Since the numbers of arrivals per-period are generated directly, this
 * process does not arise as a Poisson arrival process.
 * However, inter-arrival times are generated as if $A_p^*=\mathrm{round}(BA_p)$
 * was a Poisson variate.
 * As a result, for each main period, the arrival process generates $A_p^*$
 * uniforms ranging from the beginning to the end of the period, and
 * the uniforms are sorted to get inter-arrival times.
 */
public class NORTADrivenArrivalProcess extends PoissonUniformArrivalProcess {
   private MultinormalCholeskyGen ngen;
   private NegativeBinomialDist[] nbdist;
   private double[] temp;

   /**
    * Constructs a new NORTA-driven arrival process
    * with period-change event \texttt{pce}, contact factory \texttt{factory},
    * correlation matrix \texttt{sigma},
    * negative binomial parameters \texttt{(gammas[p], probs[p])}, and
    * random number stream
    * \texttt{stream}.
    @param pce the period-change event defining the periods.
    @param factory the contact factory instantiating contacts.
    @param sigma the correlation matrix.
    @param gammas the $\gamma$ parameters for negative binomials.
    @param probs the $\rho$ parameters for negative binomials.
    @param stream the random number stream for correlated
    negative binomial vectors and uniform arrival times.
    @exception NullPointerException if one argument is \texttt{null}.
    @exception IllegalArgumentException if the dimensions of the
    correlation matrix does not correspond to $P\times P$,
    or the length of \texttt{ns} or \texttt{probs} do not
    correspond to number of main periods~$P$.
    */
   public NORTADrivenArrivalProcess
      (PeriodChangeEvent pce, ContactFactory factory,
       DoubleMatrix2D sigma, double[] gammas, double[] probs,
       RandomStream stream) {
      super (pce, factory, new int[probs.length + 2], stream);
      final int P = pce.getNumMainPeriods ();
      if (probs.length != P)
         throw new IllegalArgumentException
            ("Needs one probability for each main period");
      if (gammas.length != probs.length)
         throw new IllegalArgumentException
            ("Needs a number of arrivals for each main period");
      if (sigma.rows () != P || sigma.columns () != P)
         throw new IllegalArgumentException
         ("The dimensions of the correlation matrix for the NORTA-driven arrival process must be PxP, where P is the number of main periods");
      final NormalGen ngen1 = new NormalGen(stream, new NormalDist());
      ngen = new MultinormalCholeskyGen (ngen1, new double[probs.length], sigma);
      nbdist = new NegativeBinomialDist[probs.length];
      for (int p = 0; p < probs.length; p++)
         nbdist[p] = new NegativeBinomialDist (gammas[p], probs[p]);
      temp = new double[probs.length];
   }

   /**
    * Returns the correlation matrix associated with this
    * arrival process.
    @return the associated correlation matrix.
    */
   public DoubleMatrix2D getSigma() {
      return ngen.getSigma();
   }

   /**
    * Sets the associated correlation matrix to \texttt{sigma}.
    @param sigma the new correlation matrix.
    @exception NullPointerException if \texttt{sigma} is \texttt{null}.
    @exception IllegalArgumentException if \texttt{sigma} is not a $P\times P$
    symmetric and positive-definite matrix.
    */
   public void setSigma (DoubleMatrix2D sigma) {
      ngen.setSigma (sigma);
   }

   /**
    * Returns the value of $\gamma_p$, the negative binomial double-precision
    * parameter associated with main period~$p$.
    @param p the main period index.
    @return the value of $\gamma_p$.
    @exception ArrayIndexOutOfBoundsException if \texttt{p}
    is negative or greater than or equal to~$P$.
    */
   public double getNegBinGamma (int p) {
      return nbdist[p].getN();
   }

   /**
    * Returns the value of $\rho_p$, the negative binomial double-precision
    * parameter associated with main period~$p$.
    @param p the main period index.
    @return the value of $\rho_p$.
    @exception ArrayIndexOutOfBoundsException if \texttt{p}
    is negative or greater than or equal to~$P$.
    */
   public double getNegBinP (int p) {
      return nbdist[p].getP();
   }

   /**
    * Sets the parameters for the negative binomial of period
    * $p$ to $\gamma_p$ and $\rho_p$.
    @param p the index of the main period.
    @param gammap the new value of $\gamma_p$.
    @param rhop the new value of $\rho_p$.
    @exception ArrayIndexOutOfBoundsException if \texttt{p}
    is negative or greater than or equal to~$P$.
    @exception IllegalArgumentException if the negative binomial parameters
    are invalid.
    */
   public void setNegBinParams (int p, double gammap, double rhop) {
      nbdist[p].setParams (gammap, rhop);
   }

   @Override
   public void init() {
      computeArrivals();
      super.init();
   }

   private void computeArrivals() {
      ngen.nextPoint (temp);
      for (int p = 0; p < temp.length; p++)
         temp[p] = NormalDist.cdf01 (temp[p]);
      final int[] arv = getArrivals();
      arv[0] = arv[arv.length - 1] = 0;
      for (int p = 0; p < temp.length; p++)
         arv[p + 1] = nbdist[p].inverseFInt (temp[p]);
      setArrivals (arv);
   }

   @Override
   public double getExpectedArrivalRate (int p) {
      if (p == 0)
         return 0;
      final double d = getPeriodChangeEvent().getPeriodDuration (p);
      final NegativeBinomialDist nbd = nbdist[p - 1];
      //return nbd.getN()*(1.0 - nbd.getP())/(nbd.getP()*d);
      return nbd.getMean() / d;
   }
}
