package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.functions.MathFunctionUtil;
import umontreal.ssj.probdist.BetaDist;
import umontreal.ssj.probdist.BinomialDist;

/**
 * Computes information on the conditional
 * distribution of the waiting time, for
 * a deterministic horizon.
 */
public class ProbInAWTBinomial implements ProbInAWT {
   private double awt;
   private double timeHorizon;
   private int ntr;
   private double jumpRate;
   private BinomialDist mbdist;
   private double[] probInAWT;
   private double[] wtcond;
   
   public ProbInAWTBinomial () {
      probInAWT = new double[1];
      probInAWT[0] = 1;
      for (int i = 1; i < probInAWT.length; i++)
         probInAWT[i] = Double.NaN;
      wtcond = new double[1];
      Arrays.fill (wtcond, Double.NaN);
   }

   public double getProbInAWT (int delta) {
      if (delta < probInAWT.length) {
         if (Double.isNaN (probInAWT[delta]))
            return probInAWT[delta] = mbdist.barF (delta);
         return probInAWT[delta];
      }
      double[] probInAWT2 = new double[delta + 1];
      System.arraycopy (probInAWT, 0, probInAWT2, 0, probInAWT.length);
      for (int i = probInAWT.length; i < probInAWT2.length; i++)
         probInAWT2[i] = Double.NaN;
      probInAWT = probInAWT2;
      return probInAWT[delta] = mbdist.barF (delta);
   }
   
   public double getExpectedWaitingTime (int delta) {
      if (delta > ntr)
         return timeHorizon;
      return timeHorizon*delta / (ntr + 1.0);
   }

   public double getExpectedWaitingTimeGTAWT (int delta) {
      if (delta > ntr)
         return timeHorizon;
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
      final BetaDist bdist = new BetaDist (delta, ntr + 1 - delta);
      final MathFunction fn = new MathFunction() {
         public double evaluate (double x) {
            return x*bdist.density (x);
         }
      };
      final double iawt = MathFunctionUtil.integral (fn, 0, awt / timeHorizon);
      return wtcond[delta] = (wt - timeHorizon*iawt) / poutawt;
   }
   
   public double getAWT() {
      return awt;
   }
   
   public double getTimeHorizon() {
      return timeHorizon;
   }
   
   public int getNumTransitions() {
      return ntr;
   }
   
   public double getJumpRate () {
      return jumpRate;
   }
   
   public void init (double awt1, double jumpRate1, double timeHorizon1,
         int numTransitions) {
      this.jumpRate = jumpRate1;
      if (awt1 != this.awt || timeHorizon1 != this.timeHorizon || numTransitions != ntr) {
         this.awt = awt1;
         this.timeHorizon = timeHorizon1;
         this.ntr = numTransitions;
         mbdist = new BinomialDist (ntr, awt1 / timeHorizon1);
         Arrays.fill (probInAWT, Double.NaN);
         Arrays.fill (wtcond, Double.NaN);
         probInAWT[0] = 1;
      }
   }
}
