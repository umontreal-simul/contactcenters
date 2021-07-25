import umontreal.iro.lecuyer.util.*;

public class arraytest
{

   public static void main (String[]args)
   {
      // procc ();
      proca ();
     // procb ();
   }


   private static void proca ()
   {
      final double x = Double.NaN; //99;
      double[][] A = { {2, 3, 4}, {6, 7} };
//      double[][] A = new double [0][0];
      printD (A);
      double[][] B = ArrayUtil.resizeArray (A, 4, 3, x);
      printD (B);
      double[][] C = ArrayUtil.resizeArray (A, 2, 3, x);
      printD (C);
      double[][] D = ArrayUtil.resizeArray (A, 2, 2, x);
      printD (D);
      double[][] F = ArrayUtil.resizeArray (A, 2, 1, x);
      printD (F);
   }

   private static void procb ()
   {
      int[] A = { 2, 3, 4 };
      print (A);
      int[] B = ArrayUtil.resizeArray (A, 4);
      print (B);
      int[] C = ArrayUtil.resizeArray (A, 2);
      print (C);
   }

   private static void procc ()
   {
      int[][] A = { {2, 3, 4}, {6, 7} };
      print (A);
      int[][] D = ArrayUtil.resizeRow (A, 3, 2);
      print (D);
      int[][] B = ArrayUtil.resizeRow (A, 1, 3);
      print (B);
      int[][] C = ArrayUtil.resizeRow (A, 1, 1);
      print (C);
   }

   private static void printD (double[][]A)
   {
      for (int i = 0; i < A.length; i++) {
         for (int j = 0; j < A[i].length; j++) {
            System.out.printf ("%5.0f ", A[i][j]);
         }
         System.out.println ();
      }
      System.out.println ("-------------------------------------\n");
   }


   private static void print (int[][]A)
   {
      for (int i = 0; i < A.length; i++) {
         for (int j = 0; j < A[i].length; j++) {
            System.out.printf ("%3d ", A[i][j]);
         }
         System.out.println ();
      }
      System.out.println ("-------------------------------------\n");
   }

   private static void print (int[]A)
   {
      for (int i = 0; i < A.length; i++) {
         System.out.printf ("%3d ", A[i]);
      }
      System.out.println ("\n-------------------------------------\n");
   }

}
