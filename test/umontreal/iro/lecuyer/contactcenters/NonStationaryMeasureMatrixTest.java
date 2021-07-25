package umontreal.iro.lecuyer.contactcenters;

import junit.framework.TestCase;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.mperiods.MeasureMatrix;

public class NonStationaryMeasureMatrixTest extends TestCase {
   private TestMeasureMatrix tm;
   private PeriodChangeEvent pce;
   private NonStationaryMeasureMatrix<TestMeasureMatrix> nsmc;

   public NonStationaryMeasureMatrixTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      tm = new TestMeasureMatrix ();
      tm.init ();
      pce = new PeriodChangeEvent (new double[] { 12.01, 44.01 });
      nsmc = new NonStationaryMeasureMatrix<TestMeasureMatrix> (pce, tm);
      Sim.init ();
      pce.init ();
      pce.start ();
   }

   @Override
   public void tearDown () {
      tm = null;
      pce = null;
      nsmc = null;
   }

   /**
    * We create a simple one-period measure matrix containing two values
    * initialized at 10 and 5. During a test simulation, we increment the value
    * at each simulation time unit. We then know the values of the counters for
    * any simulation time.
    */
   public void testMatrix () {
      new EndSimEvent ().schedule (50.01);
      new AddEvent ().schedule (1.0);
      Sim.start ();
      pce.stop ();

      assertEquals ("Number of measures", 2, nsmc.getNumMeasures ());
      assertEquals ("Number of periods", pce.getNumPeriods (), nsmc
            .getNumPeriods ());
      for (int p = 0; p < pce.getNumPeriods (); p++) {
         final double exp0 = ((int) pce.getPeriodDuration (p)) * 1.2;
         final double exp1 = ((int) pce.getPeriodDuration (p)) * 1.5;
         final double s0 = ((int) pce.getPeriodStartingTime (p)) * 1.2 + 10;
         final double s1 = ((int) pce.getPeriodStartingTime (p)) * 1.5 + 5;
         assertEquals ("Period " + p + " measure 0", exp0, nsmc.getMeasure (0,
               p), 1e-6);
         assertEquals ("Period " + p + " measure 1", exp1, nsmc.getMeasure (1,
               p), 1e-6);
         assertEquals ("Sum at period " + p + " measure 0", s0, nsmc.getSum (0,
               p), 1e-6);
         assertEquals ("Sum at period " + p + " measure 1", s1, nsmc.getSum (1,
               p), 1e-6);
      }
   }

   class AddEvent extends Event {
      @Override
      public void actions () {
         tm.count[0] += 1.2;
         tm.count[1] += 1.5;
         schedule (1.0);
      }
   }

   class EndSimEvent extends Event {
      @Override
      public void actions () {
         Sim.stop ();
      }
   }

   class TestMeasureMatrix implements MeasureMatrix {
      double[] count = new double[2];

      public int getNumMeasures () {
         return 2;
      }

      public void setNumMeasures (int nm) {
         throw new UnsupportedOperationException ();
      }

      public int getNumPeriods () {
         return 1;
      }

      public void regroupPeriods (int x) {}

      public void setNumPeriods (int np) {
         throw new UnsupportedOperationException ();
      }

      public double getMeasure (int i, int p) {
         return count[i];
      }

      public void init () {
         count[0] = 10;
         count[1] = 5;
      }
   }
}
