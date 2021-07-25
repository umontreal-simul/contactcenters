package umontreal.ssj.randvar;

import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.rng.RandomStream;

/**
 * Exponential kernel density random variate generator. This random variate
 * generator uses the empirical distribution of log-service times to generate
 * service times. It uses a gaussian kernel with positive reflection and applies
 * the exponential function on every generated variate.
 */
public class ExpKernelDensityGen extends KernelDensityGen {
   /**
    * Constructs a new exponential kernel density generator from the empirical
    * distribution \texttt{dist} and the random stream \texttt{stream}. This
    * constructor calls the {@link #setPositiveReflection} public method.
    * 
    * @param stream
    *           the random number stream to generate the uniforms.
    * @param dist
    *           the empirical distribution for the log-service times.
    */
   public ExpKernelDensityGen (RandomStream stream, EmpiricalDist dist) {
      super (stream, dist, new NormalGen (stream, new NormalDist ()));
      setPositiveReflection (true);
   }

   @Override
   public double nextDouble () {
      return Math.exp (super.nextDouble ());
   }
}
