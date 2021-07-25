package umontreal.iro.lecuyer.contactcenters;

import junit.framework.TestCase;

import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class MultiPeriodGenTest extends TestCase {
   PeriodChangeEvent pce;
   RandomStream stream;

   public MultiPeriodGenTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      pce = new PeriodChangeEvent (1.0, 12, 5.0);
      stream = new MRG32k3a ();
   }

   @Override
   public void tearDown () {
      pce = null;
      stream = null;
   }

   public void testConstant () {
      final double[] cons = { 1, 1, 5, 3, 2, 9, 3, 2, 5, 3, 2, 2 };
      checkGen (MultiPeriodGen.createConstant (pce, cons));
   }

   public void testExponential () {
      final double[] lams = { 1, 1, 5, 3, 2, 9, 3, 2, 5, 3, 2, 2 };
      checkGen (MultiPeriodGen.createExponential (pce, stream, lams));
   }

   public void testGamma () {
      final double[] alph = { 2, 2, 5, 2, 2, 6, 6, 3, 6, 3, 2, 2 };
      final double[] lams = { 1, 1, 5, 3, 2, 9, 3, 2, 5, 3, 2, 2 };
      checkGen (MultiPeriodGen.createGamma (pce, stream, alph, lams));
   }

   void checkGen (MultiPeriodGen gen) {
      stream.resetStartStream ();
      Sim.init ();
      pce.init ();
      pce.start ();
      new CheckPeriod (gen, 0).schedule (0.1);
      for (int i = 1; i < pce.getNumPeriods () - 1; i++) {
         final double st = pce.getPeriodStartingTime (i);
         final double et = pce.getPeriodEndingTime (i);
         final double t = (st + et) / 2.0;
         new CheckPeriod (gen, i).schedule (t);
      }
      new CheckPeriod (gen, pce.getNumPeriods () - 1).schedule (100.0);
      Sim.start ();
   }

   class CheckPeriod extends Event {
      MultiPeriodGen gen;
      int p;

      CheckPeriod (MultiPeriodGen gen, int p) {
         this.gen = gen;
         this.p = p;
      }

      @Override
      public void actions () {
         final RandomVariateGen g = gen.getGenerator (p);
         final double[] vals = new double[250];
         stream.resetStartSubstream ();
         for (int i = 0; i < vals.length; i++)
            vals[i] = gen.nextDouble ();
         stream.resetStartSubstream ();
         for (int i = 0; i < vals.length; i++) {
            final double v = g.nextDouble ();
            assertEquals ("Generated value " + i, vals[i], v, 1e-6);
         }
      }
   }
}
