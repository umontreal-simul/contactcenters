package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.functions.MathFunctionUtil;
import umontreal.ssj.probdist.GammaDist;

/**
 * Computes information on the conditional
 * distribution of the waiting time in the case of
 * a random horizon. 
 */
public class ProbInAWTGamma implements ProbInAWT {
   private double awt;
   private double jumpRate;
   private double[] probInAWT;
   private double[] wtcond;
   private double timeHorizon;
   private int ntr;
   
   public ProbInAWTGamma () {
      probInAWT = new double[1];
      probInAWT[0] = 1;
      for (int i = 1; i < probInAWT.length; i++)
         probInAWT[i] = Double.NaN;
      wtcond = new double[1];
      Arrays.fill (wtcond, Double.NaN);
   }

   /* (non-Javadoc)
    * @see umontreal.iro.lecuyer.contactcenters.ctmc.ProbGoodSL#getProbGoodSL(int)
    */
   public double getProbInAWT (int delta) {
      if (delta < probInAWT.length) {
         if (Double.isNaN (probInAWT[delta]))
            return probInAWT[delta] = GammaDist.cdf (delta, jumpRate, 15, awt);
         return probInAWT[delta];
      }
      double[] probInAWT2 = new double[delta + 1];
      System.arraycopy (probInAWT, 0, probInAWT2, 0, probInAWT.length);
      for (int i = probInAWT.length; i < probInAWT2.length; i++)
         probInAWT2[i] = Double.NaN;
      probInAWT = probInAWT2;
      return probInAWT[delta] = GammaDist.cdf (delta, jumpRate, 15, awt);
   }
   
   public double getExpectedWaitingTime (int delta) {
      return delta / jumpRate;
   }
   
   public double getExpectedWaitingTimeGTAWT (int delta) {
      if (delta >= wtcond.length) {
         double[] wtcond2 = new double[delta + 1];
         System.arraycopy (wtcond, 0, wtcond2, 0, wtcond.length);
         for (int i = wtcond.length; i < wtcond2.length; i++)
            wtcond2[i] = Double.NaN;
         wtcond = wtcond2;
      }
      if (!Double.isNaN (wtcond[delta]))
         return wtcond[delta];
      final double poutawt = 1 - getProbInAWT (delta);
      final double wt = getExpectedWaitingTime (delta);
      if (wt > awt)
         return wtcond[delta] = wt / poutawt;
      final GammaDist gdist = new GammaDist (delta, jumpRate);
      final MathFunction fn = new MathFunction() {
         public double evaluate (double x) {
            return x*gdist.density (x);
         }
      };
      final double iawt = MathFunctionUtil.integral (fn, 0, awt);
      return wtcond[delta] = (wt - iawt) / poutawt;
   }

   /* (non-Javadoc)
    * @see umontreal.iro.lecuyer.contactcenters.ctmc.ProbGoodSL#getAWT()
    */
   public double getAWT() {
      return awt;
   }
   
   public double getJumpRate() {
      return jumpRate;
   }
   
   public int getNumTransitions () {
      return ntr;
   }

   public double getTimeHorizon () {
      return timeHorizon;
   }

   public void init (double awt1, double jumpRate1, double timeHorizon1,
         int numTransitions) {
      this.timeHorizon = timeHorizon1;
      ntr = numTransitions;
      if (awt1 != this.awt || jumpRate1 != this.jumpRate) {
         this.awt = awt1;
         this.jumpRate = jumpRate1;
         Arrays.fill (probInAWT, Double.NaN);
         Arrays.fill (wtcond, Double.NaN);
         probInAWT[0] = 1;
      }
   }
}
