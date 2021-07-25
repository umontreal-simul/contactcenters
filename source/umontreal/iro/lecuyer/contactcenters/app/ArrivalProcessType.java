package umontreal.iro.lecuyer.contactcenters.app;

import umontreal.iro.lecuyer.contactcenters.contact.DirichletArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.DirichletCompoundArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.NORTADrivenArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcessWithTimeIntervals;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaNortaRatesArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonUniformArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.FixedCountsArrivalProcess;

/**
 * Represents the type of arrival process for a blend/multi-skill call center.
 * This process defines at which times a new call
 * occurs during the simulation.
 * The used arrival process determines how
 * the parameters, usually given using an array of double-precision values,
 * are used.
 * All the arrival processes defined in \cite{ccAVR04a} are supported.
 *
 * Note that when simulating on an infinite horizon,
 * only arrival processes capable of generating arrivals
 * following parameters not evolving with time are allowed.
 * The recommended arrival processes for such simulations
 * are {@link #POISSON} and {@link #PIECEWISECONSTANTPOISSON}.
 *
 * If the busyness generator for $B$ is undefined, then $B$ is replaced by
 * a deterministic constant = 1.
 @xmlconfig.title Available arrival processes
 */
public enum ArrivalProcessType {
   /**
    * Poisson arrival process.
    * \javadoc{See {@link PoissonArrivalProcess} for more
    * information. }Inter-arrival times are
    * generated independently from the exponential distribution with fixed rate $B\lambda$,
    * where $\lambda$ is the base arrival rate given by the first value
    * of the \texttt{arrivals} element,
    * and $B$ is a global busyness factor given by
    * \texttt{busynessGen} element in call center parameters.
    * The $\lambda$ parameter can be estimated from data.
    @xmlconfig.title
    */
   POISSON (Messages.getString("ArrivalProcessType.Poisson")), //$NON-NLS-1$

   /**
    * Non-homogeneous Poisson arrival process with piecewise-constant
    * arrival rates.
    * \javadoc{See {@link PiecewiseConstantPoissonArrivalProcess} for more
    * information. }Inter-arrival times are independent exponential variates with rate
    * $\lambda(t)=B\lambda_{p(t)}$,
    * where $p(t)$ is the period corresponding to simulation time~$t$,
    * and $\lambda_p$ is the base arrival rate for period~$p$.
    * The values in \texttt{arrivals} element
    * give the base arrival rate for each
    * main period, while the rates for preliminary and wrap-up
    * periods are always 0.
    * The $\lambda_p$ parameters can be estimated from data
    * by assuming that the periods are independent, and
    * the per-period numbers of arrivals follow the
    * Poisson distribution.
    * The arrival rates can also be estimated together with
    * a busyness factor following the gamma$(\alpha_0,\alpha_0)$
    * distribution.  In this case, the number of arrivals
    * is assumed to follow the negative multinomial distribution
    * with parameters $\alpha_0, \rho_1,\ldots,\rho_P$, where
    * \[
    * \rho_p=\frac{\lambda_p}{\alpha_0+\sum_{k=1}^P\lambda_k}
    * \]
    * for $p=1,\ldots,P$.
    * The above method for the $\lambda_p$ is equivalent to
    * \[
    * \lambda_p=\frac1{n}\sum_{k=1}^P X_{k,p},
    * \]
    * where $n$ is the number of days.
    @xmlconfig.title
    */
   PIECEWISECONSTANTPOISSON (Messages.getString("ArrivalProcessType.PiecewiseConstantPoisson")), //$NON-NLS-1$

   /**
    * Non-homogeneous Poisson arrival process with piecewise-constant
    * arrival rates that can change at arbitrary times.
    * \javadoc{See {@link PoissonArrivalProcessWithTimeIntervals} for more
    * information. }This process is similar to {@link #PIECEWISECONSTANTPOISSON},
    * except arrival rates can change at any time, not only at period
    * boundaries.
    * More specifically, let $t_0<\cdots<t_L$ be an increasing
    * sequence of simulation times, and let
    * $B\lambda_j$, for $j=0,\ldots,L-1$, be the arrival rate
    * during time interval $[t_j,t_{j+1})$.
    * The arrival rate is 0 for $t<t_0$ and $t\ge t_L$.
    * The sequence of times is given using
    * the \texttt{times} element while the sequence of
    * rates is given using \texttt{lambdas}.
    * Of course, the length of the sequence of rates  must be
    * one less than the length of the the sequence of times.
    * @xmlconfig.title
    */
   PIECEWISECONSTANTPOISSONINT (Messages.getString ("ArrivalProcessType.PiecewiseConstantPoissonInt")), //$NON-NLS-1$

   /**
    * Uniform arrival process with piecewise-constant
    * arrival rates.
    * \javadoc{See {@link PoissonUniformArrivalProcess} for more
    * information. }For each main period $p=1,\ldots, P$,
    * $\mathrm{round}(B\lambda_p)$ arrivals are generated
    * uniformly in the period, and arrival times are
    * sorted in increasing order.
    * The values in the \texttt{arrivals} element
    * give the base arrival rate $\lambda_p$ for each
    * main period, while the rates for preliminary and wrap-up
    * periods are always 0.
    * The $\lambda_p$ parameters can be estimated from data
    * by assuming that the periods are independent, and
    * the per-period numbers of arrivals follow the
    * Poisson distribution.
    @xmlconfig.title
    */
   UNIFORM (Messages.getString("ArrivalProcessType.Uniform")), //$NON-NLS-1$

   /**
    * Uniform arrival process with pre-determined
    * arrival counts $C_p$ in each period.
    * \javadoc{Represents a counts-driven arrival process.
    * See {@link FixedCountsArrivalProcess} for more information.
    * }\begin{xmldocenv}{@linkplain FixedCountsArrivalProcess}
    * \end{xmldocenv}
    * For each main period $p=1,\ldots, P$, the $C_p$ arrivals are generated
    * uniformly in the period, and arrival times are sorted in increasing
    * order. The values in the \texttt{counts} element give the number of
    * arrivals for each main period, while the counts for preliminary and
    * wrap-up periods are always 0.
    @xmlconfig.title
    */
   FIXEDCOUNTS (Messages.getString("ArrivalProcessType.FixedCounts")), //$NON-NLS-1$

   /**
    * Poisson
    * process with piecewise-constant randomized arrival rates \cite{tJON00a}.
    * \javadoc{See {@link PoissonGammaArrivalProcess} for more
    * information. }As with {@link #PIECEWISECONSTANTPOISSON},
    * the arrival rate is given by $\lambda(t)=B\lambda_{p(t)}$.
    * The base arrival rates $\lambda_p$ are constant during each main period,
    * but they are not deterministic:
    * for main period~$p$, the base rate of the Poisson
    * process is defined as a gamma random variable with
    * shape parameter $\alpha_{\mathrm{G},p}$, and
    * scale parameter
    * $\lambda_{\mathrm{G}, p}$ (mean $\alpha_{\mathrm{G},p}/\lambda_{\mathrm{G}, p}$).
    * Shape parameters are stored in
    * element
    * \texttt{poisson\-Gamma\-Shape}
    * while scale parameters are stored in
    * \texttt{poisson\-Gamma\-Scale}.
    * As with the Poisson process with deterministic arrival rates,
    * the generated base arrival rates are multiplied
    * by a busyness factor $B$ to get
    * the arrival rates, and the arrival rate is 0
    * during preliminary and wrap-up periods.
    * The $\alpha_{\mathrm{G},p}$ and $\lambda_{\mathrm{G}, p}$
    * parameters can be estimated from data by considering
    * that the number of arrivals during period $p$ follow
    * the negative binomial distribution with parameters
    *  $(\alpha_{\mathrm{G},p},
    *  \lambda_{\mathrm{G}, p}/(\alpha_{\mathrm{G},p} + \lambda_{\mathrm{G}, p}))$,
    * independently of the other periods.
    * However, the parameters of the distribution of the
    * busyness factor cannot be estimated at the
    * same time as the gamma parameters.
    @xmlconfig.title
    */
   POISSONGAMMA (Messages.getString("ArrivalProcessType.PoissonGamma")), //$NON-NLS-1$

   /**
    * Doubly-stochastic Gamma-Poisson process with piecewise-constant
    * randomized correlated Gamma arrival rates.
    * \javadoc{See {@link PoissonGammaNortaRatesArrivalProcess} for more
    * information. }
    * 
    * The base arrival rates $\lambda_p$ are constant during each period,
    * but they are not deterministic:
    * for period~$p$, the base rate of the Poisson
    * process is defined as a correlated gamma random variable. The marginal
    * distribution of the rate is gamma with
    * shape parameter $\alpha_{\mathrm{G},p}$, and
    * scale parameter $\lambda_{\mathrm{G}, p}$
    * (and mean $\alpha_{\mathrm{G},p}/\lambda_{\mathrm{G}, p}$). The correlation structure
    * is modelled using the normal copula model with positive definite correlation matrix
    * $\boldSigma$ having elements in $[-1, 1]$.
    * If $\alpha_{\mathrm{G}, p}$ or $\lambda_{\mathrm{G}, p}$
    * are 0, the resulting arrival rate during period $p$ is always 0.
    * The Gamma shape parameters are stored in
    * element \texttt{poisson\-Gamma\-Shape}
    * while the Gamma scale parameters are stored in
    * \texttt{poisson\-Gamma\-Scale}. A correlation matrix must be given
    * by \texttt{copula\-Sigma}.
    * As with the Poisson process with deterministic arrival rates,
    * the generated base arrival rates are multiplied
    * by a busyness factor $B$ to get
    * the arrival rates, and the arrival rate is 0
    * during preliminary and wrap-up periods.
    * The parameters of the distribution of the
    * busyness factor cannot be estimated at the
    * same time as the gamma parameters.
    @xmlconfig.title
    */
   POISSONGAMMANORTARATES (Messages.getString("ArrivalProcessType.PoissonGammaNortaRates")), //$NON-NLS-1$

   /**
    * Dirichlet compound arrival process.
    * \javadoc{See {@link DirichletCompoundArrivalProcess} for more
    * information. }\begin{xmldocenv}{@linkplain DirichletCompoundArrivalProcess}
    * \end{xmldocenv}
    * The values in \texttt{arrivals} element are
    * used to store the $\alpha_p$
    * parameters.
    * These parameters, along with the $\gamma$
    * parameter of the busyness factor, can be estimated
    * from the data.
    @xmlconfig.title
    */
   DIRICHLETCOMPOUND (Messages.getString("ArrivalProcessType.DirichletCompound")), //$NON-NLS-1$

   /**
    * \javadoc{Dirichlet arrival process.
    * See {@link DirichletArrivalProcess} for more information.
    * }\begin{xmldocenv}{@linkplain DirichletArrivalProcess}
    * \end{xmldocenv}
    *
    * The values in \texttt{arrivals} element are
    * used for the $\alpha_p$
    * parameters.
    * If a busyness factor $B$ is generated for the day,
    * the generated $A_p$'s are multiplied by $B$ and
    * rounded to the nearest integer to get the modified
    * number of arrivals.
    * The parameters $\alpha_p$ can be estimated
    * from data, but one needs to specify a distribution
    * for $A$ to estimate parameters from.
    * This arrival process cannot estimate parameters
    * of the busyness factor.
    @xmlconfig.title
    */
   DIRICHLET (Messages.getString("ArrivalProcessType.Dirichlet")), //$NON-NLS-1$

   /**
    * \javadoc{Represents a NORTA-driven arrival process
    * with negative binomial marginals.
    * See {@link NORTADrivenArrivalProcess} for more information.
    * }\begin{xmldocenv}{@linkplain NORTADrivenArrivalProcess}
    * \end{xmldocenv}
    *
    * To use this process, a correlation matrix must be given
    * by \texttt{norta\-Sigma}, and
    * parameters for the negative binomials must be supplied by
    * \texttt{nortaGamma} and
    * \texttt{nortaP}.
    * If a busyness factor $B$ is generated for the day,
    * the generated $A_p$'s are multiplied by $B$ and
    * rounded to the nearest integer to get the modified
    * number of arrivals.
    * This arrival process does not support parameter
    * estimation from data.
    @xmlconfig.title
    */
   NORTADRIVEN (Messages.getString("ArrivalProcessType.NortaDriven")), //$NON-NLS-1$

   /**
    * Non-homogenous Poisson arrival process using a cubic spline
    * to model the time-varying arrival rate.
    * The $\lambda(t)$ function which represents the
    * arrival rate at any time $t$ is a cubic spline
    * created from a sequence of $n$ points $(t_i, \lambda(t_i))$
    * also called \emph{nodes}.
    * A \emph{cubic spline} is a set of cubic polynomials linked by some continuity
    * constraints.
    * The $i$th polynomial of such a spline, for $i=0,\ldots,n-2$, is defined as
    * \[s_i(t) = a_i(t - t_i)^3 + b_i(t - t_i)^2 + c_i(t - t_i) + d_i,\]
    * while the complete spline is defined as
    * \[s(t) = s_i(t)\mbox{ for }t\in[t_i, t_{i+1}].\]
    * For $t<t_0$ and $t>t_{n-1}$, the spline is undefined, but
    * the implementation performs linear extrapolation.
    *
    * \emph{Interpolating} splines are forced to pass through
    * every point, i.e., $s_i(t_i)=\lambda(t_i)$ for $i=0,\ldots,n-2$, and
    * $s_{n-2}(t_{n-1})=\lambda(t_{n-1})$.
    * On the other hand, \emph{smoothing} splines
    * tolerate some error, i.e., the spline minimizes
    * \[L = \rho\sum_{i=0}^{n-1}\left( \lambda(t_i) - s(t_i)\right)^2 +
    * (1-\rho)\int_{t_0}^{t_{n-1}}\left(s''(x)\right)^2dx.\]
    * The value $\rho$ in previous equation is the \emph{smoothing
    * factor} of the spline.
    *
    * Both kinds of splines enforce the three following continuity
    * constraints:
    * \begin{eqnarray*}
    * s_i(t_{i+1})&=&s_{i+1}(t_{i+1}),\\
    * \frac{d}{dt}s_i(t_{i+1})&=&\frac{d}{dt}s_{i+1}(t_{i+1}),\\
    * \mbox{and \quad} \frac{d^2}{dt^2}s_i(t_{i+1})&=&\frac{d^2}{dt^2}s_{i+1}(t_{i+1}),\mbox{ \quad for }i=0,\ldots,n-2.\\
    * \end{eqnarray*}
    * The cubic splines used are named \emph{natural splines},
    * because
    * $\frac{d^2}{dt^2}s(t_0)=\frac{d^2}{dt^2}s(t_{n-1})=0$.
    *
    * To use this arrival process, one must
    * specify the $t_i$'s with the
    * \texttt{times} element,
    * the $\lambda(t_i)'s$ with
    * \texttt{lambdas} element, and
    * the smoothing factor with
    * \texttt{smoothingFactor} attribute.
    * For this arrival process,
    * \texttt{times} and \texttt{lambdas} must share the same
    * length.
    * @xmlconfig.title
    */
   CUBICSPLINE (Messages.getString("ArrivalProcessType.CubicSpline"));

   private String name;

   private ArrivalProcessType (String name) { this.name = name; }

   @Override
   public String toString() {
      return name;
   }
 
}
