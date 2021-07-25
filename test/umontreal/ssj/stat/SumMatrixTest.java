package umontreal.ssj.stat;

import junit.framework.TestCase;

import umontreal.ssj.stat.mperiods.SumMatrix;

public class SumMatrixTest extends TestCase {
   SumMatrix sc1t1p;
   SumMatrix scxt1p;
   SumMatrix sc1txp;
   SumMatrix scxtxp;

   public SumMatrixTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      sc1t1p = new SumMatrix (1);
      scxt1p = new SumMatrix (2);
      sc1txp = new SumMatrix (1, 2);
      scxtxp = new SumMatrix (2, 2);
   }

   @Override
   public void tearDown () {
      sc1t1p = scxt1p = sc1txp = scxtxp = null;
   }

   public void testOneTypeOnePeriod () {
      final SumMatrix sc = sc1t1p;
      assertEquals ("Number of measures in the sum matrix", 1, sc
            .getNumMeasures ());
      assertEquals ("Number of time intervals in the sum matrix", 1, sc
            .getNumPeriods ());
      sc.add (0, 0, 1.5);
      assertEquals ("Measure 0 of the sum matrix", 1.5, sc.getMeasure (0, 0),
            1e-6);
      sc.add (0, 0, 1.2);
      assertEquals ("Measure 0 of the sum matrix", 2.7, sc.getMeasure (0, 0),
            1e-6);
   }

   public void testManyTypesOnePeriod () {
      final SumMatrix sc = scxt1p;
      assertEquals ("Number of measures in the sum matrix", 2, sc
            .getNumMeasures ());
      assertEquals ("Number of time intervals in the sum matrix", 1, sc
            .getNumPeriods ());
      sc.add (0, 0, 1.5);
      assertEquals ("Measure 0 of the sum matrix", 1.5, sc.getMeasure (0, 0),
            1e-6);
      assertEquals ("Measure 1 of the sum matrix", 0, sc.getMeasure (1, 0),
            1e-6);
      sc.add (1, 0, 1.2);
      assertEquals ("Measure 0 of the sum matrix", 1.5, sc.getMeasure (0, 0),
            1e-6);
      assertEquals ("Measure 1 of the sum matrix", 1.2, sc.getMeasure (1, 0),
            1e-6);
   }

   public void testOneTypeManyPeriods () {
      final SumMatrix sc = sc1txp;
      assertEquals ("Number of measures in the sum matrix", 1, sc
            .getNumMeasures ());
      assertEquals ("Number of time intervals in the sum matrix", 2, sc
            .getNumPeriods ());
      sc.add (0, 0, 1.5);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 1.5, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 0, sc
            .getMeasure (0, 1), 1e-6);
      sc.add (0, 1, 1.2);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 1.5, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 1, time interval 1 of the sum matrix", 1.2, sc
            .getMeasure (0, 1), 1e-6);

      sc.regroupPeriods (2);
      assertEquals ("Measure 0, time interval 0 of the sum matrix", 2.7, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 0, time interval 1 of the sum matrix", 0, sc
            .getMeasure (0, 1), 1e-6);
   }

   public void testManyTypesManyPeriods () {
      final SumMatrix sc = scxtxp;
      assertEquals ("Number of measures in the sum matrix", 2, sc
            .getNumMeasures ());
      assertEquals ("Number of time intervals in the sum matrix", 2, sc
            .getNumPeriods ());
      sc.add (0, 0, 1.5);
      sc.add (0, 1, 3.0);
      assertEquals ("Measure 0 time interval 0 of the sum matrix", 1.5, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 1 time interval 0 of the sum matrix", 0, sc
            .getMeasure (1, 0), 1e-6);
      assertEquals ("Measure 0 time interval 1 of the sum matrix", 3.0, sc
            .getMeasure (0, 1), 1e-6);
      assertEquals ("Measure 1 time interval 1 of the sum matrix", 0, sc
            .getMeasure (1, 1), 1e-6);
      sc.add (1, 0, 1.2);
      sc.add (1, 1, 2.4);
      assertEquals ("Measure 0 time interval 0 of the sum matrix", 1.5, sc
            .getMeasure (0, 0), 1e-6);
      assertEquals ("Measure 1 time interval 0 of the sum matrix", 1.2, sc
            .getMeasure (1, 0), 1e-6);
      assertEquals ("Measure 0 time interval 1 of the sum matrix", 3.0, sc
            .getMeasure (0, 1), 1e-6);
      assertEquals ("Measure 1 time interval 1 of the sum matrix", 2.4, sc
            .getMeasure (1, 1), 1e-6);
   }
}
