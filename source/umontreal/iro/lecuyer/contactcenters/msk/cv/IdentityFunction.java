package umontreal.iro.lecuyer.contactcenters.msk.cv;

import umontreal.ssj.util.MultivariateFunction;

public class IdentityFunction implements MultivariateFunction {
   public int getDimension () {
      return 1;
   }

   public double evaluate (double... x) {
      return x[0];
   }

   public double evaluateGradient (int i, double... x) {
      return 1;
   }
}
