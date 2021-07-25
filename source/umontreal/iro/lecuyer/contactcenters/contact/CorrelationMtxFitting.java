package umontreal.iro.lecuyer.contactcenters.contact;

/**
 *
 * Fits the parametric model for the correlation matrix using method of least squares. Two models are
 * implemented: (i) general linear model $r_j = a b^j + c$, (ii) single rho Markov model $r_j = b^j$.
 * Method {@link #fitMarkovGeneralLinear} implements fitting of model (i),
 * method {@link #fitMarkovSingleRho} implements fitting of model (ii). The optimization of parameter
 * $b$ is performed using exhaustive grid search with step {@link #step} in the range
 * [-1+{@link #delta}, 1-{@link #delta}]. For model (i) parameters $a$
 * and $c$ have closed form expressions in terms of the entries of correlation matrix and parameter $b$.
 *
 * @author Boris N. Oreshkin
 *
 */
public class CorrelationMtxFitting
{
   double[] CorrVector;
   int N;
   // int M; // Number of grid points for the brute force optimization of the correlation coefficient
   double tol;
   double delta;
   double step;

   public CorrelationMtxFitting(double[] CorrVector) {
      this.N = CorrVector.length;
      this.CorrVector = new double[CorrVector.length];
      System.arraycopy(CorrVector, 0, this.CorrVector, 0, CorrVector.length);
      tol = 1.e-9;
      delta = 1.e-3;
      step = 1.e-3;
   }

   public CorrelationMtxFitting(double[] CorrVector, double tol) {
      this.CorrVector = new double[CorrVector.length];
      System.arraycopy(CorrVector, 0, this.CorrVector, 0, CorrVector.length);
      this.N = CorrVector.length;
      this.tol = tol;
      delta = 1.e-3;
      step = 1.e-3;
   }

   /**
    * Sets the limits in which exhaustive grid search for the optimization of parameter $b$
    * is performed. The search is performed in the
    * interval [-1+{@link #delta}, 1-{@link #delta}].
    * @param delta Exhaustive grid search boundary offset
    */
   public void setDelta(double delta) {
      this.delta = delta;
   }

   /**
    * Sets the exhaustive grid search grid size for the optimization of parameter $b$.
    * @param step Exhaustive grid search grid size
    */
   public void setStep(double step) {
      this.step = step;
   }



   /**
    * Fits general linear model $r_j = a b^j + c$. Returns vector of length 3
    * with parameters $a$, $b$ and $c$.
    *
    * @return vector of [$a$, $b$ and $c$].
    */
   public double[] fitMarkovGeneralLinear() {
      double[] abc = new double[3];
      double sumBj, sumRj, sumRjBj, sumB2j;
      double bCurrent;
      double Cmin;
      double bMin;

      bCurrent = -1 + delta;
      Cmin = getSquaredErrorMarkovGeneralLinear(bCurrent);
      bMin = bCurrent;
      while (bCurrent <= 1 - delta) {
         double Ctemp;

         Ctemp = getSquaredErrorMarkovGeneralLinear(bCurrent);
         if (Ctemp < Cmin) {
            bMin = bCurrent;
            Cmin = Ctemp;
         }
         bCurrent += step;
      }
      abc[1] = bMin;

      sumBj = 0;
      sumRj = 0;
      sumRjBj = 0;
      sumB2j = 0;
      for (int j = 0; j < N; j++) {
         sumBj += Math.pow(abc[1], j + 1);
         sumRj += CorrVector[j];
         sumRjBj += Math.pow(abc[1], j + 1) * CorrVector[j];
         sumB2j += Math.pow(abc[1], 2 * (j + 1));
      }
      abc[0] = (sumRjBj - sumBj * sumRj / N) /
               (sumB2j - sumBj * sumBj / N);
      for (int j = 0; j < N; j++) {
         abc[2] += CorrVector[j] - abc[0] * Math.pow(abc[1], j + 1);
      }
      abc[2] /= N;
      return abc;
   }

   /**
    * Fits the Markov model with single correlation coefficient of the form $\rho_j = b^j$.
    *
    * @return parameter $b$.
    */
   public double fitMarkovSingleRho() {
      double bCurrent;
      double Cmin;
      double bMin;

      bCurrent = -1 + delta;
      Cmin = getSquaredErrorSingleRho(bCurrent);
      bMin = bCurrent;
      while (bCurrent <= 1 - delta) {
         double Ctemp;

         Ctemp = getSquaredErrorSingleRho(bCurrent);
         if (Ctemp < Cmin) {
            bMin = bCurrent;
            Cmin = Ctemp;
         }
         bCurrent += step;
      }
      return bMin;
   }

   private double getSquaredErrorSingleRho(double rho) {
      double Sum = 0;
      for (int i = 0; i < N; i++) {
         double temp;
         // double temp1;

         temp = Math.pow(rho, i + 1);
         // temp1 = (double)(i+1) * Math.pow(rho, i);
         Sum += (CorrVector[i] - temp) * (CorrVector[i] - temp);
         // Sum += temp1 * (CorrVector[i]-temp);
      }
      return Sum;
   }

   private double getSquaredErrorMarkovGeneralLinear(double b) {
      double C;
      double a, c;
      double sumBj, sumRj, sumRjBj, sumB2j;

      sumBj = 0;
      sumRj = 0;
      sumRjBj = 0;
      sumB2j = 0;
      for (int j = 0; j < N; j++) {
         sumBj += Math.pow(b, j + 1);
         sumRj += CorrVector[j];
         sumRjBj += Math.pow(b, j + 1) * CorrVector[j];
         sumB2j += Math.pow(b, 2 * (j + 1));
      }
      // Handle the special case using the limit
      if (b == 0) {
         double ab;

         ab = (CorrVector[0] - sumRj / N) / (1 - 1 / ((double)N));
         c = (sumRj - ab) / N;
         C = (CorrVector[0] - (ab + c)) * (CorrVector[0] - (ab + c));
         for (int j = 1; j < N; j++) {
            C += (CorrVector[j] - c) * (CorrVector[j] - c);
         }
         return C;
      }

      a = (sumRjBj - sumBj * sumRj / N) / (sumB2j - sumBj * sumBj / N);
      c = 0;
      for (int j = 0; j < N; j++) {
         c += CorrVector[j] - a * Math.pow(b, j + 1);
      }
      c /= N;
      C = 0;
      for (int j = 0; j < N; j++) {
         C += (CorrVector[j] - a * Math.pow(b, j + 1) - c) *
              (CorrVector[j] - a * Math.pow(b, j + 1) - c);
      }
      return C;
   }

}
