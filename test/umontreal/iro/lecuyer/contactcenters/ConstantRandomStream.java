package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.rng.RandomStream;

public class ConstantRandomStream implements RandomStream {
   private double value;

   public ConstantRandomStream (double value) {
      if (value <= 0 || value >= 1)
         throw new IllegalArgumentException ();
      this.value = value;
   }

   public String formatState () {
      return "";
   }

   public void nextArrayOfDouble (double[] u, int start, int n) {
      for (int i = 0; i < n; i++)
         u[start + i] = value;
   }

   public void nextArrayOfInt (int i, int j, int[] u, int start, int n) {
      for (int k = 0; k < n; k++)
         u[start + k] = (int) ((j - i + 1) * value) + i;
   }

   public double nextDouble () {
      return value;
   }

   public int nextInt (int i, int j) {
      return i + (int) ((j - i + 1) * value);
   }

   public void resetNextSubstream () {}

   public void resetStartSubstream () {}

   public void resetStartStream () {}

   public void setAntithetic (boolean anti) {}
}
