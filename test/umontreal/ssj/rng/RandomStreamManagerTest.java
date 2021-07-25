package umontreal.ssj.rng;

import junit.framework.TestCase;

public class RandomStreamManagerTest extends TestCase {
   RandomStreamManager rsm;

   public RandomStreamManagerTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      rsm = new RandomStreamManager ();
   }

   @Override
   public void tearDown () {
      rsm = null;
   }

   public void performCheck (CheckRandomStream cr, int calls) {
      cr.resetStartSubstreamCalled = 0;
      cr.resetNextSubstreamCalled = 0;
      cr.resetStartStreamCalled = 0;
      rsm.resetNextSubstream ();
      assertEquals ("Number of calls to resetNextSubstream", calls,
            cr.resetNextSubstreamCalled);
      rsm.resetStartSubstream ();
      assertEquals ("Number of calls to resetStartSubstream", calls,
            cr.resetStartSubstreamCalled);
      rsm.resetStartStream ();
      assertEquals ("Number of calls to resetStartStream", calls,
            cr.resetStartStreamCalled);
   }

   public void testAdd () {
      final CheckRandomStream cr = new CheckRandomStream ();
      rsm.add (cr);
      performCheck (cr, 1);
   }

   public void testRemove () {
      final CheckRandomStream cr = new CheckRandomStream ();
      rsm.add (cr);
      rsm.remove (cr);
      performCheck (cr, 0);
   }

   public void testClear () {
      final CheckRandomStream cr = new CheckRandomStream ();
      rsm.add (cr);
      rsm.clear ();
      performCheck (cr, 0);
   }

   public void testAddTwo () {
      final CheckRandomStream cr = new CheckRandomStream ();
      // Should not forward the calls twice
      rsm.add (cr);
      rsm.add (cr);
      performCheck (cr, 1);
   }

   class CheckRandomStream implements RandomStream {
      int resetStartSubstreamCalled = 0;
      int resetNextSubstreamCalled = 0;
      int resetStartStreamCalled = 0;

      public String formatState () {
         return "";
      }

      public void nextArrayOfDouble (double[] u, int start, int n) {}

      public void nextArrayOfInt (int i, int j, int[] u, int start, int n) {}

      public double nextDouble () {
         return 0;
      }

      public int nextInt (int i, int j) {
         return i;
      }

      public void resetNextSubstream () {
         resetNextSubstreamCalled++;
      }

      public void resetStartSubstream () {
         resetStartSubstreamCalled++;
      }

      public void resetStartStream () {
         resetStartStreamCalled++;
      }

      public void setAntithetic (boolean anti) {}
   }
}
