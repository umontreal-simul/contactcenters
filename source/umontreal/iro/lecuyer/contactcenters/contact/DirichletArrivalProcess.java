package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.DistributionFactory;
import umontreal.ssj.probdistmulti.DirichletDist;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.RandomStream;

/**
 * Represents an arrival process where
 * the number of arrivals are spread
 * in periods using a
 * Dirichlet distribution \cite{ccAVR04a}.
 * Let's define the vector of ratios
 * \[\mathbf{\mathcal{Q}}\latex{\equiv}\html{=}(\mathcal{Q}_1,\ldots,\mathcal{Q}_P)
 * =(A_1/A,\ldots,A_P/A),\]
 * where $A_p$ denotes the number of arrivals during main period~$p$
 * and
 * \[A=\sum_{p=1}^P A_p\]
 * is the total number of arrivals.
 * The number of arrivals during the
 * preliminary and the wrap-up periods,
 * $A_0$ and $A_{P+1}$ respectively, are always 0
 * for this process.
 *
 * At the beginning of each replication,
 * $A$ is generated from a probability
 * distribution such as gamma.  A vector $\mathbf{\mathcal{Q}}$ is then generated
 * from a Dirichlet distribution \cite{tJOH69a} with parameters
 * $(\alpha_1, \ldots, \alpha_P)$.  Each component of $\mathbf{\mathcal{Q}}$
 * is multiplied with $A$ to get $\tilde{\mathbf{A}}$ before
 * the vector $\mathbf{A}$ is obtained by rounding each
 * component of $\tilde{\mathbf{A}}$ to the nearest integer.
 *
 * Since per-period numbers of arrivals are generated directly
 * rather than through arrival rates, this
 * process does not arise as a Poisson arrival process.
 * However, inter-arrival times are generated as if the
 * $A_p^*=\mathrm{round}(BA_p)$
 * were Poisson variates.
 * As a result, for each main period, the arrival process generates $A_p^*$
 * uniforms ranging from the beginning to the end of the period, and
 * the uniforms are sorted to get inter-arrival times.
 */
public class DirichletArrivalProcess extends PoissonUniformArrivalProcess {
   private double[] q;
   private RandomVariateGen agen;
   private DirichletGen dgen;

   /**
    * Constructs a new Dirichlet arrival process
    * with period-change event \texttt{pce}, contact factory \texttt{factory},
    * Dirichlet parameters \texttt{alphas}, random number stream
    * \texttt{stream}, and generator \texttt{agen} for the number of arrivals.
    @param pce the period change event.
    @param factory the contact factory instantiating contacts.
    @param alphas the parameters of the Dirichlet distribution.
    @param stream the random number stream for Dirichlet vectors
    and uniform arrival times.
    @param agen the random variate generator for the number of arrivals.
    @exception IllegalArgumentException if there is not an $\alpha$ value
    for each main period, or if one $\alpha$ value is negative or 0.
    @exception NullPointerException if one argument is \texttt{null}.
    */
   public DirichletArrivalProcess (PeriodChangeEvent pce,
                                   ContactFactory factory,
                                   double[] alphas,
                                   RandomStream stream,
                                   RandomVariateGen agen) {
      super (pce, factory, new int[alphas.length + 2], stream);
      final int nv = getPeriodChangeEvent().getNumPeriods() - 2;
      if (alphas.length == nv)
         dgen = new DirichletGen (getStream(), alphas);
      else {
         final double[] alph = new double[nv];
         System.arraycopy (alphas, 0, alph, 0, nv);
         dgen = new DirichletGen (getStream(), alph);
      }
      q = new double[alphas.length];
      if (agen == null)
         throw new NullPointerException ("The given random variate generator for A must not be null");
      this.agen = agen;
   }

   /**
    * Returns the random variate generator used for the
    * total number of arrivals $A$.
    @return the random variate generator for the total number of arrivals.
    */
   public RandomVariateGen getNumArrivalsGenerator() {
      return agen;
   }

   /**
    * Changes the random variate generator for the
    * number of arrivals to \texttt{agen}.
    @param agen the new random variate generator for the number of arrivals.
    @exception NullPointerException if the parameter is \texttt{null}.
    */
   public void setNumArrivalsGenerator (RandomVariateGen agen) {
      if (agen == null)
         throw new NullPointerException ("The given random variate generator for A must not be null");
      this.agen = agen;
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

   @Override
   public void setStream (RandomStream stream) {
      super.setStream (stream);
      dgen.setStream (stream);
   }

   /**
    * Sets the Dirichlet parameters $\alpha_p$
    * for this object.
    @param alphas a new vector of parameters.
    @exception IllegalArgumentException if the length of \texttt{alphas}
    does not correspond to the number of main periods or if
    one of the $\alpha$ parameter is negative or 0.
    @exception NullPointerException if \texttt{alphas} is \texttt{null}.
    */
   public void setAlphas (double[] alphas) {
      final int nv = getPeriodChangeEvent().getNumPeriods() - 2;
      if (alphas.length < nv)
         throw new IllegalArgumentException
            ("Invalid number of Dirichlet parameters, needs at least " + nv + " values");
      if (alphas.length == nv)
         dgen = new DirichletGen (getStream(), alphas);
      else {
         final double[] alph = new double[nv];
         System.arraycopy (alphas, 0, alph, 0, nv);
         dgen = new DirichletGen (getStream(), alph);
      }
   }

   /**
    * Initializes the number of arrivals
    * with a fixed $A$ \texttt{a}.
    @param a the total number of arrivals.
    @exception IllegalArgumentException if \texttt{a} is negative or 0.
    */
   public void initWithFixedA (double a) {
      if (a <= 0)
         throw new IllegalArgumentException ("y <= 0");
      computeArrivals (a);
      super.init();
   }

   @Override
   public void init() {
      computeArrivals (agen.nextDouble());
      super.init();
   }

   private final void computeArrivals (double a) {
      dgen.nextPoint (q);
      final int[] arv = getArrivals();
      arv[0] = arv[arv.length - 1] = 0;
      for (int p = 0; p < q.length; p++)
         arv[p + 1] = (int)Math.round (q[p]*a);
      setArrivals (arv);
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
      final int d = dgen.getDimension();
      if (p < 0 || p > d + 1)
         throw new IllegalArgumentException ("Invalid period index " + p);
      if (p == 0 || p == d + 1)
         return 0;
      double alpha0 = 0;
      for (int i = 0; i < d; i++)
         alpha0 += dgen.getAlpha (i);
      final double pMean = dgen.getAlpha (p - 1) / alpha0;
      final double duration = getPeriodChangeEvent().getPeriodDuration (p);
      final double numArrivals = agen.getDistribution().getMean() * pMean;
      return numArrivals / duration;
   }

   /**
    * Estimates the Dirichlet parameters of an arrival process
    * from the number of arrivals in the array
    * \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, and $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * This method computes $\rho_{i, p}=X_{i, p}/Y_i$
    * where $X_{i, p}$ is the number of arrivals on day
    * $i$ during period $p$, and $Y_i$ is the total
    * number of arrivals during day $i$.
    * The returned array contains the Dirichlet
    * parameters $\hat{\alpha}_0, \ldots, \hat{\alpha}_{P-1}$
    * estimated by assuming that
    * the ratios $\rho_{i, p}$
    * follow the Dirichlet distribution.
    * @param arrivals the number of arrivals during each day and period.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * @return the estimated Dirichlet parameters.
    */
   public static double[] getMLE (int[][] arrivals, int numObs, int numPeriods) {
      final double[][] par = new double[numObs][numPeriods];
      for (int i = 0; i < numObs; i++) {
         int totalArrivals = 0;
         for (int p = 0; p < numPeriods; p++)
            totalArrivals += arrivals[i][p];
         for (int p = 0; p < numPeriods; p++)
            par[i][p] = arrivals[i][p] / (double)totalArrivals;
      }
      final double[] alphas = DirichletDist.getMLE (par, numObs, numPeriods);
      return alphas;
   }

   /**
    * Constructs a new arrival process with Dirichlet parameters
    * estimated by the maximum likelihood method based on
    * the \texttt{numObs} observations in array \texttt{arrivals}.
    * Element \texttt{arrivals[i][p]} corresponds
    * to the number of arrivals on day \texttt{i}
    * during period \texttt{p},
    * where $i=0,\ldots,n-1$, $p=0,\ldots,P-1$,
    * $n=$~\texttt{numObs}, and $P=$~\texttt{numPeriods}.
    * The created arrival process uses the random variate generator
    * \texttt{agen} to generate the total number of arrivals for
    * each day while
    * the Dirichlet parameters are estimated
    * using {@link #getMLE(int[][],int,int)}.
    * @param pce the period-change event marking the end of periods.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream to generate arrival times.
    * @param agen the random variate generator for $A$.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * parameter is estimated in addition to the arrival rates.
    * @return the constructed arrival process.
    */
   public static DirichletArrivalProcess getInstanceFromMLE
     (PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
           RandomVariateGen agen,
           int[][] arrivals, int numObs, int numPeriods) {
      final double[] alphas = getMLE (arrivals, numObs, numPeriods);
      final DirichletArrivalProcess dap = new DirichletArrivalProcess (pce, factory, alphas, stream, agen);
      return dap;
   }

   /**
    * Similar to {@link #getInstanceFromMLE(PeriodChangeEvent,ContactFactory,RandomStream,RandomVariateGen,int[][],int,int)},
    * but also estimates the parameters for $A$.
    * This method accepts a class object \texttt{aDistClass}
    * which is the guessed probability distribution of $A$.
    * It uses {@link DistributionFactory} to get an
    * instance of the distribution (with estimated parameters),
    * and constructs the arrival process by
    * using this distribution, and the Dirichlet parameters
    * estimated by {@link #getMLE(int[][],int,int)}.
    * @param pce the period-change event marking the end of periods.
    * @param factory the contact factory used to create contacts.
    * @param stream the random stream to generate arrival times.
    * @param streamArr the random stream for $A$.
    * @param aDistClass the class of the probability distribution of $A$.
    * @param arrivals the number of arrivals.
    * @param numObs the number of days.
    * @param numPeriods the number of periods.
    * parameter is estimated in addition to the arrival rates.
    * @return the constructed arrival process.
    */
   @SuppressWarnings("unchecked")
   public static DirichletArrivalProcess getInstanceFromMLE
   (PeriodChangeEvent pce, ContactFactory factory, RandomStream stream,
         RandomStream streamArr,
         Class<? extends Distribution> aDistClass,
         int[][] arrivals, int numObs, int numPeriods) {
      Distribution dist;
      if (DiscreteDistributionInt.class.isAssignableFrom (aDistClass)) {
         final int[] totArv = new int[numObs];
         for (int i = 0; i < numObs; i++)
            for (int p = 0; p < numPeriods; p++)
               totArv[i] += arrivals[i][p];
         dist = DistributionFactory.getDistributionMLE ((Class<? extends DiscreteDistributionInt>)aDistClass, totArv, numObs);
      }
      else {
         final double[] totArv = new double[numObs];
         for (int i = 0; i < numObs; i++)
            for (int p = 0; p < numPeriods; p++)
               totArv[i] += arrivals[i][p];
         dist = DistributionFactory.getDistributionMLE ((Class<? extends ContinuousDistribution>)aDistClass, totArv, numObs);
      }
      final RandomVariateGen agen = new RandomVariateGen (streamArr, dist);
      return getInstanceFromMLE (pce, factory, stream, agen, arrivals, numObs, numPeriods);
   }
}
