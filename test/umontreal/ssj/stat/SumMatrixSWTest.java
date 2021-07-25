package umontreal.ssj.stat;

import umontreal.ssj.stat.mperiods.SumMatrixSW;

public class SumMatrixSWTest extends SumMatrixTest {
   public SumMatrixSWTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      sc1t1p = new SumMatrixSW (1);
      scxt1p = new SumMatrixSW (2);
      sc1txp = new SumMatrixSW (1, 2);
      scxtxp = new SumMatrixSW (2, 2);
   }

   @Override
   public void testOneTypeOnePeriod () {
      super.testOneTypeOnePeriod ();
      final SumMatrixSW sc = (SumMatrixSW) sc1t1p;
      // Test the sliding window, the measure for time interval 0 should
      // disappear, replaced by time interval 1.
      sc.add (0, 1, 1.8);
      assertEquals ("Measure 0 of the sum matrix", 1.8, sc.getMeasure (0, 0),
            1e-6);
      sc.init ();
      assertEquals ("Measure 0 after init", 0.0, sc.getMeasure (0, 0), 1e-6);
   }

   @Override
   public void testManyTypesOnePeriod () {
      super.testManyTypesOnePeriod ();
      final SumMatrixSW sc = (SumMatrixSW) scxt1p;
      sc.add (0, 1, 1.9);
      assertEquals ("Measure 0 of the sum matrix", 1.9, sc.getMeasure (0, 0),
            1e-6);
      assertEquals ("Measure 1 of the sum matrix", 0, sc.getMeasure (1, 0),
            1e-6);
   }

   @Override
   public void testOneTypeManyPeriods () {
      super.testOneTypeManyPeriods ();
      final SumMatrixSW sc = (SumMatrixSW) sc1txp;
      sc.init ();
      sc.add (0, 0, 1.5);
      sc.add (0, 1, 1.2);

      sc.add (0, 2, 7.2);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 1.2, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 7.2, sc
            .getMeasure (0, 1), 1e-6);
      sc.add (0, 3, 8.9);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 7.2, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 8.9, sc
            .getMeasure (0, 1), 1e-6);
      sc.add (0, 5, 1.2);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 0, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 1.2, sc
            .getMeasure (0, 1), 1e-6);
      sc.add (0, 6, 7.2);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 1.2, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 7.2, sc
            .getMeasure (0, 1), 1e-6);
   }
}
