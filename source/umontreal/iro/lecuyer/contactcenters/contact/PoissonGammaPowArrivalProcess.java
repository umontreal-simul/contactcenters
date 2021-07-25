package umontreal.iro.lecuyer.contactcenters.contact;

import cern.jet.stat.Gamma;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.GammaGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;

/**
 *
 * 
 */
public class PoissonGammaPowArrivalProcess extends PoissonGammaArrivalProcess {

    private double[] pow; // the exponetial powers for each period
    private double beta; 
    private double[] gammapInv; // the inverse of gammap, because multiply is faster than divide

    public PoissonGammaPowArrivalProcess (PeriodChangeEvent pce,
         ContactFactory factory,
         double[] galphas,
         double[] glambdas,
         double[] pow,
         RandomStream stream,
         RandomStream streamBusyness, RandomVariateGen bgen) {
        super(pce, factory, galphas, glambdas, stream, streamBusyness, bgen);
        
        if (pow.length != glambdas.length || pow.length < pce.getNumPeriods())
         throw new IllegalArgumentException ("Invalid number of " +
            "parameters, needs one parameter for each period");
        this.pow = pow;
        
        if (bgen.getDistribution() instanceof GammaDist == false)
            throw new IllegalStateException("PoissonGammaPow arrival process requires the daily busyness distribution"
                    + " to be a Gamma distribution.");
        beta = 1.0 / bgen.getDistribution().getVariance();
        
        gammapInv = new double[pow.length];
        for (int j = 0; j < pow.length; j++) {
//            gammapInv[j] = 1.0 / (Math.pow(beta, -1.0 * pow[j]) * Gamma.gamma(pow[j] + beta) / Gamma.gamma(beta));
            double logGammapInv = pow[j] * Math.log(beta)-Gamma.logGamma(pow[j] + beta)+Gamma.logGamma(beta);
            gammapInv[j] = Math.exp(logGammapInv);
        }
    }
    
    /**
     * Sets the exponential power $p_j$ of each period.
     * 
     * @param p the exponential power
     */
    public void setExpPower(double[] p) {
        this.pow = p;
    }
    
    /**
     * Returns the exponential power of each period.
     * 
     * @return the exponential power of each period.
     */
    public double[] getExpPower() {
        return pow.clone();
    }
    
    @Override
    protected void computeRates() {
      final double[] lam = getLambdas();
      
      final double[] galphas = getGammaAlphas();
      final double[] glambdas = getGammaLambdas();
      RandomStream streamBusyness = getBusynessStream();
      
      double b;
      if (null == busyGen)
         b = 1;
      else
         b = getBusynessFactor();
      
      for (int j = 0; j < galphas.length; j++) {
         lam[j] = galphas[j] == 0 || glambdas[j] == 0 ? 0 :
         	glambdas[j] * Math.pow(b, pow[j]) * GammaGen.nextDouble (streamBusyness, galphas[j], galphas[j]) * gammapInv[j];
      }
      setLambdas (lam);
   }
 
}
