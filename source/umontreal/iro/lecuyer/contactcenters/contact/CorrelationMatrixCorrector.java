package umontreal.iro.lecuyer.contactcenters.contact;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

/**
 * Implements the algorithm POSDEF that transforms the approximate correlation matrix that
 * may contain negative eigenvalues to the valid positive definite correlation matrix.
 * The algorithm is described in Davenport, J. M. and Iman, R. L. (1982). An iterative
 * algorithm to produce a positive definite correlation matrix from an approximate
 * correlation matrix. Technical report, Sandia National Laboratories, Albuquerque, New Mexico.
 * If the POSDEF algorithm fails to converge this implementation uses diagonal loading algorithm.
 * In this case it returns the original matrix plus scaled identity matrix. 
 * The returned matrix is scaled so that the diagonal entries are equal to 1.
 *
 * @author Boris N. Oreshkin
 */
public class CorrelationMatrixCorrector
{
   int maxit;
   double epsilon; // Parameter of the POSDEF algorithm
   double epsilon_loading; // Parameter of the loading algorithm
   public DoubleMatrix2D Rin;
   public DoubleMatrix2D Rout;
   public DoubleMatrix2D D;
   public DoubleMatrix2D V;

   /**
    * Constructs CorrelationMatrixCorrector object from the correlations matrix Rin. Sets maximum number of 
    * iterations equal to maxit and the number, which is used to substitute the negative eigenvalues,
    * equal to epsilon. The correlation matrix Rin must be symmetric.
    * 
    * @param maxit Maximum number of iterations
    * @param epsilon Number used to replace the negative eigenvalues
    * @param Rin Input correlation matrix
    */
   public CorrelationMatrixCorrector(int maxit, double epsilon, double[][] Rin)
   {
      this.maxit = maxit;
      this.epsilon = epsilon;
      this.Rin = new DenseDoubleMatrix2D(Rin);
      epsilon_loading = 1e-5;
   }

   /**
    * Constructs CorrelationMatrixCorrector object from the correlations matrix Rin. 
    * Sets  {@link #maxit} and  {@link #epsilon} to default values of 100 and 1e-3 
    * respectively.
    * @param Rin the input correlation matrix
    */
   public CorrelationMatrixCorrector(double[][] Rin) {
   	this (100, 1.0e-3, Rin);
   }

   /**
    * Returns the corrected positive definite correlation matrix.
    * @return the positive definite correlation matrix
    */
   public double[][] getRout()
   {
      return Rout.toArray();
   }

   /**
    * Returns the number, which is used to substitute for the negative eigenvalues.
    * @return value replacing negative eigenvalues
    */
   public double getEpsilon() {
      return epsilon;
   }

   /**
    * Returns the maximum number of iterations in the algorithm.
    * @return maximum number of iterations
    */
   public int getMaxit() {
      return maxit;
   }

   /**
    * Sets the maximum number of iterations in the algorithm.
    * @param maxit The maximum number of iterations in the algorithm.
    */
   public void setMaxit(int maxit) {
      this.maxit = maxit;
   }

   /**
    * Sets the number, which is used to substitute the negative eigenvalues in the POSDEF algorithm
    * @param epsilon  The positive number, which is used to substitute the negative 
    * eigenvalues in the POSDEF algorithm
    */
   public void setEpsilon(double epsilon)
   {
      this.epsilon = epsilon;
   }

   /**
    * Sets the number, which is used to scale the identity matrix in the diagonal loading algorithm
    * 
    * @param epsilonLoading The positive number, which is used to scale the identity matrix 
    * in the diagonal loading algorithm 
    * 
    */
   public void setEpsilonLoading(double epsilonLoading)
   {
      this.epsilon_loading = epsilonLoading;
   }



   /**
    * Calculate the corrected correlation matrix according to the posdef algorithm proposed
    * by Davenport and Iman.
    * 
    * @return Corrected correlation matrix in 2D double array.
    */
   public double[][] calcCorrectedR()
   {
      int numNegative = 0;
      int iteration = 0;
      DoubleMatrix2D temp;
      DoubleMatrix2D temp1;
      EigenvalueDecomposition eig;

      eig = new EigenvalueDecomposition(Rin);
      D = eig.getD();
      V = eig.getV();

      for (int i = 0; i < Rin.columns(); i++) {
         if (D.get(i, i) <= 0) {
            numNegative += 1;
         }
      }
      Rout = Rin.copy();
      temp = D.copy();
      temp1 = D.copy();
      while ((numNegative > 0) && (iteration < maxit) ) {
         int numEpsiloned;

         // System.out.printf ("%g \n ", (double)numNegative);

         numEpsiloned = 0;
         for (int i = 0; i < Rout.columns(); i++) {
            if (D.get(i, i) <= 0) {
               D.set(i, i, epsilon);
            } else if (D.get(i, i) < epsilon) {
               D.set(i, i, epsilon);
               numEpsiloned += 1;
            }
            if (numEpsiloned == numNegative) {
               break;
            }
         }

         temp = D.copy();
         // This is to preserve symmetry of the correlation matrix
         for (int i = 0; i < Rout.columns(); i++) {
            temp.set(i, i, Math.sqrt(D.get(i, i)));

         }
         temp.zMult(V, temp1, 1.0, 0.0, false, true);
         temp = temp1.copy();
         temp.zMult(temp1, Rout, 1.0, 0.0, true, false);
         for (int i = 0; i < Rout.columns(); i++) {
            Rout.set(i, i, 1.0);
         }

         eig = new EigenvalueDecomposition(Rout);
         D = eig.getD();
         V = eig.getV();

         numNegative = 0;
         for (int i = 0; i < Rout.columns(); i++) {
            if (D.get(i, i) <= 0) {
               numNegative += 1;
            }
         }

         iteration += 1;
      }
      // If the algorithm did not converge apply diagonal loading
      if (numNegative > 0) {
         double minD;

         Rout = Rin.copy();

         eig = new EigenvalueDecomposition(Rin);
         D = eig.getD();

         minD = 0;
         for (int i = 0; i < D.columns(); i++) {
            if (D.get(i, i) < minD) {
               minD = D.get(i, i);
            }
         }
         for (int i = 0; i < Rout.columns(); i++) {
            Rout.set(i, i, Rout.get(i, i) - minD + epsilon_loading);
         }
         for (int i = 0; i < Rout.columns(); i++) {
            for (int j = 0; j < Rout.columns(); j++) {
               Rout.set(i, j, Rout.get(i, j) / (1 - minD + epsilon_loading));
            }
         }
      }


      return Rout.toArray();
   }

   private int[] sortEigs(double[] smallEigs)
   {
      int[] SortedIndxs;
      int N;

      N = smallEigs.length;
      SortedIndxs = new int[N];
      for (int i = 0; i < N; i++) {
         SortedIndxs[i] = i;
      }
      for (int j = 0; j < N; j++) {
         for (int i = 0; i < N - 1; i++) {
            if (smallEigs[i] > smallEigs[i + 1]) {
               double temp;

               temp = smallEigs[i + 1];
               smallEigs[i + 1] = smallEigs[i];
               smallEigs[i] = temp;
            }
         }
      }

      return SortedIndxs;
   }

}
