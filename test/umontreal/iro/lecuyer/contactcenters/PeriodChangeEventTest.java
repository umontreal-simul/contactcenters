package umontreal.iro.lecuyer.contactcenters;

import junit.framework.TestCase;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class PeriodChangeEventTest extends TestCase {
   PeriodChangeEvent pceFix;
   PeriodChangeEvent pceVar;

   public PeriodChangeEventTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      pceFix = new PeriodChangeEvent (50.0, 5, 10.0);
      pceVar = new PeriodChangeEvent (new double[] { 1.2, 5.4, 9.8, 17.3 });
   }

   @Override
   public void tearDown () {
      pceFix = null;
      pceVar = null;
   }

   public void testPeriodInfo () {
      assertEquals ("Number of periods", 5, pceFix.getNumPeriods ());
      assertEquals ("Start of period 0", 0, pceFix.getPeriodStartingTime (0), 1e-5);
      assertEquals ("End of period 0", 10.0, pceFix.getPeriodEndingTime (0), 1e-5);
      assertEquals ("Start of period 1", 10.0, pceFix.getPeriodStartingTime (1),
            1e-5);
      assertEquals ("End of period 1", 60.0, pceFix.getPeriodEndingTime (1), 1e-5);
      assertEquals ("Start of period 2", 60.0, pceFix.getPeriodStartingTime (2),
            1e-5);
      assertEquals ("End of period 2", 110.0, pceFix.getPeriodEndingTime (2), 1e-5);
      assertEquals ("Start of period 3", 110.0, pceFix.getPeriodStartingTime (3),
            1e-5);
      assertEquals ("End of period 3", 160.0, pceFix.getPeriodEndingTime (3), 1e-5);
      assertEquals ("Start of period 4", 160.0, pceFix.getPeriodStartingTime (4),
            1e-5);
      assertTrue (Double.isNaN (pceFix.getPeriodEndingTime (4)));
      assertEquals ("Duration of period 0", 10.0, pceFix.getPeriodDuration (0),
            1e-5);
      assertEquals ("Duration of period 2", 50.0, pceFix.getPeriodDuration (2),
            1e-5);
      assertTrue (Double.isNaN (pceFix.getPeriodDuration (4)));
      assertEquals ("Number of periods", 5, pceVar.getNumPeriods ());
      assertEquals ("110.0 is a start-period time", true, pceFix
            .isPeriodStartingTime (110.0));
      assertEquals ("60.01 is not a start-period time", false, pceFix
            .isPeriodStartingTime (60.01));
   }

   public void testGetPeriodFix () {
      assertEquals ("Period for time 0", 0, pceFix.getPeriod (0));
      assertEquals ("Period for time 5", 0, pceFix.getPeriod (5));
      assertEquals ("Period for time 10", 1, pceFix.getPeriod (10.0));
      assertEquals ("Period for time 25", 1, pceFix.getPeriod (25.0));
      assertEquals ("Period for time 175.0", 4, pceFix.getPeriod (175.0));
   }

   public void testGetPeriodVar () {
      assertEquals ("Period for time 0", 0, pceVar.getPeriod (0));
      assertEquals ("Period for time 1.3", 1, pceVar.getPeriod (1.3));
      assertEquals ("Period for time 5.4", 2, pceVar.getPeriod (5.4));
      assertEquals ("Period for time 10.2", 3, pceVar.getPeriod (10.2));
      assertEquals ("Period for time 175.0", 4, pceVar.getPeriod (175.0));
   }

   public void testSetEndTimes () {
      pceVar.setEndingTimes (new double[] { 32, 37, 45, 52 });
      // The two following calls are supposed to fail
      boolean ex = false;
      try {
         pceFix.setEndingTimes (new double[] { 32, 37, 35, 52 });
      }
      catch (final IllegalArgumentException iae) {
         ex = true;
      }
      if (!ex)
         fail ("setEndTimes accepts a decreasing function");
      ex = false;
      try {
         pceFix.setEndingTimes (new double[] { 32, 33 });
      }
      catch (final IllegalArgumentException iae) {
         ex = true;
      }
      if (!ex)
         fail ("setEndTimes will change the number of periods");
   }

   public void testPeriodChangeListener () {
      final TestPeriodChangeListener tns = new TestPeriodChangeListener ();
      pceFix.addPeriodChangeListener (tns);
      Sim.init ();
      pceFix.init ();
      pceFix.start ();

      Sim.start ();
      assertTrue (tns.beginFirstMainPeriodCalled);
      assertTrue (tns.nextMainPeriodCalled);
      assertTrue (tns.endLastMainPeriodCalled);
      assertFalse (tns.endCalled);
      pceFix.stop ();
      assertTrue (tns.endCalled);
   }

   public void testLastPeriod () {
      Sim.init ();
      pceFix.init ();
      pceFix.start ();
      new EndSimEvent ().schedule (200.0);
      Sim.start ();
      assertEquals ("Last period end time at time 200", 200.0, pceFix
            .getPeriodEndingTime (4), 1e-3);
      assertEquals ("Last period duration at time 200", 40.0, pceFix
            .getPeriodDuration (4), 1e-3);
   }

   class EndSimEvent extends Event {
      @Override
      public void actions () {
         Sim.stop ();
      }
   }

   class TestPeriodChangeListener implements PeriodChangeListener {
      boolean beginFirstMainPeriodCalled = false;
      boolean nextMainPeriodCalled = false;
      boolean endLastMainPeriodCalled = false;
      boolean endCalled = false;
      int currentPeriod = 0;

      public void changePeriod (PeriodChangeEvent pce) {
         assertTrue (pce == PeriodChangeEventTest.this.pceFix);
         final int period = pce.getCurrentPeriod ();
         if (period == 1) {
            beginFirstMainPeriodCalled = true;
            currentPeriod = 1;
            assertEquals ("First main period index", currentPeriod, pceFix
                  .getCurrentPeriod ());
         }
         else if (period == pce.getNumPeriods () - 2) {
            endLastMainPeriodCalled = true;
            currentPeriod++;
            assertEquals ("Index of last period", currentPeriod, pceFix
                  .getCurrentPeriod ());
            Sim.stop ();
         }
         else {
            currentPeriod++;
            nextMainPeriodCalled = true;
            assertEquals ("Current main period", currentPeriod, pceFix
                  .getCurrentPeriod ());
         }
      }

      public void stop (PeriodChangeEvent pce) {
         assertTrue (pce == PeriodChangeEventTest.this.pceFix);
         endCalled = true;
      }
   }
}
