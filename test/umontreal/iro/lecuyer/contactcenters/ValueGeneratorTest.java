package umontreal.iro.lecuyer.contactcenters;

import junit.framework.TestCase;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class ValueGeneratorTest extends TestCase {
   RandomStream[] streams = new RandomStream[] {
         new ConstantRandomStream (0.2), new ConstantRandomStream (0.2),
         new ConstantRandomStream (0.2) };
   RandomStream[] rstreams = new RandomStream[] { new MRG32k3a (),
         new MRG32k3a (), new MRG32k3a () };
   FilterRandomStream aux = new FilterRandomStream (new ConstantRandomStream (
         0.2));
   FilterRandomStream raux = new FilterRandomStream (new MRG32k3a ());

   public ValueGeneratorTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      rstreams[0].resetStartStream ();
      rstreams[1].resetStartStream ();
      rstreams[2].resetStartStream ();
      raux.resetStartStream ();
   }

   public void testConstantValueGenerator () {
      ConstantValueGenerator cvg = new ConstantValueGenerator (3, 29.97);
      assertEquals ("Value for contact type 0", 29.97, cvg
            .nextDouble (new Contact (0)), 1e-3);
      assertEquals ("Value for contact type 1", 29.97, cvg
            .nextDouble (new Contact (1)), 1e-3);
      assertEquals ("Value for contact type 2", 29.97, cvg
            .nextDouble (new Contact (2)), 1e-3);

      cvg = new ConstantValueGenerator (new double[] { 20, 30, 40 });
      assertEquals ("Value for contact type 0", 20, cvg
            .nextDouble (new Contact (0)), 1e-3);
      assertEquals ("Value for contact type 1", 30, cvg
            .nextDouble (new Contact (1)), 1e-3);
      assertEquals ("Value for contact type 2", 40, cvg
            .nextDouble (new Contact (2)), 1e-3);
      cvg.getValues ()[0][1] = 34.0;
      assertEquals ("Value for contact type 1", 34.0, cvg
            .nextDouble (new Contact (1)), 1e-3);
   }

   public void testConstantValueGeneratorP () {
      final PeriodChangeEvent pce = new PeriodChangeEvent (20.0, 4, 15.0);
      ConstantValueGenerator cvg = new ConstantValueGenerator (pce, 3,
            new double[] { 12, 12, 24, 24 });
      Sim.init ();
      pce.init ();
      pce.start ();
      new CheckValuesEvent (pce, cvg, new double[] { 12, 12, 12 }).schedule (0);
      new CheckValuesEvent (pce, cvg, new double[] { 12, 12, 12 })
            .schedule (15.5);
      new CheckValuesEvent (pce, cvg, new double[] { 24, 24, 24 })
            .schedule (35.1);
      new CheckValuesEvent (pce, cvg, new double[] { 24, 24, 24 })
            .schedule (60.0);
      Sim.start ();

      final double[][] values = { { 2, 4, 6 }, { 2, 4, 6 }, { 8, 10, 12 },
            { 8, 10, 12 } };
      cvg = new ConstantValueGenerator (pce, values);
      Sim.init ();
      pce.init ();
      pce.start ();
      new CheckValuesEvent (pce, cvg, values[0]).schedule (0);
      new CheckValuesEvent (pce, cvg, values[1]).schedule (15.5);
      new CheckValuesEvent (pce, cvg, values[2]).schedule (35.1);
      new CheckValuesEvent (pce, cvg, values[3]).schedule (60.0);
      Sim.start ();
   }

   public void testRandomValueGenerator () {
      RandomValueGenerator rvg = new RandomValueGenerator (3,
            new ConstantVariateGen (29.97));
      assertEquals ("Value for contact type 0", 29.97, rvg
            .nextDouble (new Contact (0)), 1e-3);
      assertEquals ("Value for contact type 1", 29.97, rvg
            .nextDouble (new Contact (1)), 1e-3);
      assertEquals ("Value for contact type 2", 29.97, rvg
            .nextDouble (new Contact (2)), 1e-3);

      rvg = new RandomValueGenerator (new RandomVariateGen[] {
            new ConstantVariateGen (20), new ConstantVariateGen (30),
            new ConstantVariateGen (40) });
      assertEquals ("Value for contact type 0", 20, rvg
            .nextDouble (new Contact (0)), 1e-3);
      assertEquals ("Value for contact type 1", 30, rvg
            .nextDouble (new Contact (1)), 1e-3);
      assertEquals ("Value for contact type 2", 40, rvg
            .nextDouble (new Contact (2)), 1e-3);
      rvg.getRandomVariateGens ()[0][1] = new ConstantVariateGen (34.0);
      assertEquals ("Value for contact type 1", 34.0, rvg
            .nextDouble (new Contact (1)), 1e-3);
   }

   public void testRandomValueGeneratorP () {
      final PeriodChangeEvent pce = new PeriodChangeEvent (20.0, 4, 15.0);
      RandomValueGenerator rvg = new RandomValueGenerator (pce, 3,
            new RandomVariateGen[] { new ConstantVariateGen (12),
                  new ConstantVariateGen (12), new ConstantVariateGen (24),
                  new ConstantVariateGen (24) });
      Sim.init ();
      pce.init ();
      pce.start ();
      new CheckValuesEvent (pce, rvg, new double[] { 12, 12, 12 }).schedule (0);
      new CheckValuesEvent (pce, rvg, new double[] { 12, 12, 12 })
            .schedule (15.5);
      new CheckValuesEvent (pce, rvg, new double[] { 24, 24, 24 })
            .schedule (35.1);
      new CheckValuesEvent (pce, rvg, new double[] { 24, 24, 24 })
            .schedule (60.0);
      Sim.start ();

      final double[][] values = { { 2, 4, 6 }, { 2, 4, 6 }, { 8, 10, 12 },
            { 8, 10, 12 } };
      final RandomVariateGen[][] rvalues = new RandomVariateGen[values.length][values[0].length];
      for (int i = 0; i < values.length; i++)
         for (int j = 0; j < values[i].length; j++)
            rvalues[i][j] = new ConstantVariateGen (values[i][j]);
      rvg = new RandomValueGenerator (pce, rvalues);
      Sim.init ();
      pce.init ();
      pce.start ();
      new CheckValuesEvent (pce, rvg, values[0]).schedule (0);
      new CheckValuesEvent (pce, rvg, values[1]).schedule (15.5);
      new CheckValuesEvent (pce, rvg, values[2]).schedule (35.1);
      new CheckValuesEvent (pce, rvg, values[3]).schedule (60.0);
      Sim.start ();
   }

   double averageValue (ValueGenerator vgen, Contact contact, double expected) {
      final int N = 1000000;
      double v = 0;
      for (int i = 0; i < N; i++) {
         v += vgen.nextDouble (contact);
         if (Math.abs (v / (i + 1) - expected) < 1e-3)
            return v / (i + 1);
      }
      return v / N;
   }

   class CheckValuesEvent extends Event {
      PeriodChangeEvent pce;
      ValueGenerator vgen;
      double[] values;
      boolean avg = false;

      public CheckValuesEvent (PeriodChangeEvent pce, ValueGenerator vgen,
            double[] values) {
         this.pce = pce;
         this.vgen = vgen;
         this.values = values;
      }

      public CheckValuesEvent (PeriodChangeEvent pce, ValueGenerator vgen,
            double[] values, boolean avg) {
         this.pce = pce;
         this.vgen = vgen;
         this.values = values;
         this.avg = avg;
      }

      @Override
      public void actions () {
         final int period = pce.getCurrentPeriod ();
         for (int t = 0; t < values.length; t++)
            if (avg)
               assertEquals ("Average value for period " + period
                     + ", contact type " + t, values[t],
               // vgen instanceof ExponentialValueGenerator ?
                     // averageValue ((ExponentialValueGenerator)vgen, new
                     // Contact (t)) :
                     averageValue (vgen, new Contact (t), values[t]), 1e-2);
            else
               assertEquals ("Value for period " + period + ", contact type "
                     + t, values[t], vgen.nextDouble (new Contact (t)), 1e-3);
      }
   }

   class ConstantVariateGen extends RandomVariateGen {
      private double value;

      public ConstantVariateGen (double value) {
         this.value = value;
      }

      @Override
      public double nextDouble () {
         return value;
      }
   }

   class FilterRandomStream implements RandomStream {
      boolean used = false;
      RandomStream stream;

      public FilterRandomStream (RandomStream stream) {
         this.stream = stream;
      }

      public void nextArrayOfDouble (double[] u, int start, int n) {
         used = true;
         stream.nextArrayOfDouble (u, start, n);
      }

      public void nextArrayOfInt (int i, int j, int[] u, int start, int n) {
         used = true;
         stream.nextArrayOfInt (i, j, u, start, n);
      }

      public double nextDouble () {
         used = true;
         return stream.nextDouble ();
      }

      public int nextInt (int i, int j) {
         used = true;
         return stream.nextInt (i, j);
      }

      public void resetNextSubstream () {
         used = true;
         stream.resetNextSubstream ();
      }

      public void resetStartSubstream () {
         used = true;
         stream.resetStartSubstream ();
      }

      public void resetStartStream () {
         used = true;
         stream.resetStartStream ();
      }
   }
}
