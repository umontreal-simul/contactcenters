package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.Arrays;

import junit.framework.TestCase;
import umontreal.iro.lecuyer.contactcenters.ConstantRandomStream;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ToggleEvent;
import umontreal.ssj.functionfit.PolInterp;
import umontreal.ssj.functions.Polynomial;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.randvar.GammaAcceptanceRejectionGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.list.ListOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class ContactArrivalProcessTest extends TestCase {
   private int numContacts;
   private ExponentialGen egen;
   private ContactFactory factory;
   private NewContactListener listener;
   static double[] lambdas = { 0, 25, 75, 50, 10, 25, 100, 0 };
   static int[] arrivals = { 0, 25, 75, 50, 10, 25, 100, 0 };
   private PeriodChangeEvent pce;
   
   private static Polynomial lambdat = new PolInterp
   (new double[] { 0, 1500, 3000 },
         new double[] { 0, 100, 0 });
   private static double periodDuration = 500.0;
   private static double lambdaMax = 100;
   private static double tMax = 3000;
   private static double[] lambdaInt = new double[lambdas.length - 2];
   
   static {
      for (int p = 0; p < lambdaInt.length; p++)
         lambdaInt[p] = lambdat.integral (p*periodDuration, (p+1)*periodDuration);
   }

   public ContactArrivalProcessTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      numContacts = 0;
      egen = new ExponentialGen (new ConstantRandomStream (0.1),
            new ExponentialDist (1.5));
      factory = new SimpleContactFactory ();
      listener = new TestContactListener ();
      Sim.init ();
      pce = new PeriodChangeEvent (periodDuration, lambdas.length, 0);
      pce.init ();
   }

   @Override
   public void tearDown () {
      pce = null;
      egen = null;
      factory = null;
      listener = null;
   }

   /**
    * Performs a general test.
    */
   public void performBasicTest (ContactArrivalProcess arrivProc) {
      assertFalse (arrivProc.isStarted ());
      new EndSimEvent ().schedule (3500.0);
      new ToggleEvent (arrivProc, true).schedule (100.0);
      new ToggleEvent (arrivProc, false).schedule (1000.0);
      new ToggleEvent (arrivProc, true).schedule (2500.0);
      pce.start ();
      Sim.start ();
      pce.stop ();
   }
   
   class EndSimEvent extends Event {
      @Override
      public void actions () {
         Sim.stop ();
      }
   }
   
   public void performStatTest (ContactArrivalProcess arrivProc, double... lambdas) {
      final int P = pce.getNumMainPeriods ();
      final ListOfTallies<Tally> statArrivals = ListOfTallies.createWithTally (P);
      final ArvCounter arvCounter = new ArvCounter();
      arrivProc.addNewContactListener (arvCounter);
      for (int i = 0; i < 50; i++) {
         Sim.init ();
         pce.init ();
         arvCounter.init ();
         new EndSimEvent ().schedule (3000.0);
         arrivProc.init ();
         arrivProc.start ();
         pce.start ();
         Sim.start ();
         arrivProc.stop ();
         pce.stop ();
         statArrivals.add (arvCounter.getArrivals ());
      }
      arrivProc.removeNewContactListener (arvCounter);
      
      final double[] cr = new double[2];
      final double level = 1 - 0.05 / P;
      for (int p = 0; p < P; p++) {
         statArrivals.get (p).confidenceIntervalStudent (level, cr);
         final double l = cr[0] - cr[1];
         final double u = cr[0] + cr[1];
         assertTrue ("The arrival rate " + lambdas[0] +
               " in main period " + p +
               " is in the confidence interval [" +
               l + ", " + u + "]",
               lambdas[p] >= l && lambdas[p] <= u);
      }
   }
   
   class ArvCounter implements NewContactListener {
      double[] nArrivals = new double[pce.getNumMainPeriods ()];
      
      public void newContact (Contact contact) {
         final int p = pce.getCurrentMainPeriod ();
         ++nArrivals[p];
      }
      
      public void init() {
         Arrays.fill (nArrivals, 0);
      }
      
      public double[] getArrivals() {
         return nArrivals;
      }
   }


   class TestContactListener implements NewContactListener {
      public void newContact (Contact contact) {
         numContacts++;
      }
   }

   public void testStationaryGenerator () {
      final StationaryContactArrivalProcess sarrivProc = new StationaryContactArrivalProcess (
            factory, egen);
      sarrivProc.init ();
      assertEquals ("Random variate generator for interarrival times", egen,
            sarrivProc.getTimesGen ());
      assertEquals ("Next interarrival time", egen.nextDouble (), sarrivProc
            .nextTime (), 1e-6);
      sarrivProc.addNewContactListener (listener);
      sarrivProc.init ();
      performBasicTest (sarrivProc);
      assertTrue (numContacts > 0);
      numContacts = 0;
      Sim.init ();
      pce.init ();
      sarrivProc.removeNewContactListener (listener);
      sarrivProc.init ();
      performBasicTest (sarrivProc);
      assertTrue (numContacts == 0);
   }

   public void testPoissonProcess () {
      final PoissonArrivalProcess arrivProc = new PoissonArrivalProcess (
            factory, lambdas[1], new ConstantRandomStream (0.1));
      arrivProc.init ();
      arrivProc.addNewContactListener (listener);
      new ToggleEvent (arrivProc, true).schedule (5.0);
      final double arv = ExponentialDist.inverseF (lambdas[1], 0.1);
      new CheckNumContactsEvent (1).schedule (5.0 + arv + 0.00001);
      new SetLambdaEvent (arrivProc, lambdas[1] / 3.0).schedule (5.0 + arv
            + arv / 4);
      new CheckNumContactsEvent (1).schedule (5.0 + 2 * arv + 0.0001);
      new CheckNumContactsEvent (2).schedule (5.0 + arv + arv / 4 + 3
            * (arv - arv / 4) + 0.0001);
      new ToggleEvent (arrivProc, false).schedule (10.0);
      pce.start ();
      Sim.start ();
      pce.stop ();
   }

   public void testPoissonUniformArrivalProcess () {
      final PoissonUniformArrivalProcess arrivProc = new PoissonUniformArrivalProcess (
            pce, factory, arrivals, new MRG32k3a ());
      arrivProc.init ();
      arrivProc.start ();
      arrivProc.addNewContactListener (listener);
      new CheckNumContactsEvent (0).schedule (0);
      new CheckNumContactsEvent (25).schedule (500.0);
      new CheckNumContactsEvent (100).schedule (1000.0);
      new CheckNumContactsEvent (150).schedule (1500.0);
      new CheckNumContactsEvent (160).schedule (2000.0);
      new CheckNumContactsEvent (185).schedule (2500.0);
      new CheckNumContactsEvent (285).schedule (3000.0);
      new CheckNumContactsEvent (285).schedule (3500.0);
      pce.start ();
      Sim.start ();
      pce.stop ();
   }

   public void testPiecewiseConstantPoissonProcess () {
      final ContactArrivalProcess arrivProc = new PiecewiseConstantPoissonArrivalProcess (
            pce, factory, lambdas, new MRG32k3a ());
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      final double[] lam = new double[lambdas.length - 2];
      System.arraycopy (lambdas, 1, lam, 0, lam.length);
      for (int p = 0; p < lam.length; p++)
         lam[p] *= periodDuration;
      performStatTest (arrivProc, lam);
   }

   public void testPoissonGammaProcess () {
      final double[] alphas = { 0, 50, 100, 100, 30, 100, 100, 0 };
      final double[] lams = { 0, 2, 1.3333, 2, 3, 4, 1, 0 };
      final ContactArrivalProcess arrivProc = new PoissonGammaArrivalProcess (
            pce, factory, alphas, lams, new MRG32k3a (), new MRG32k3a ());
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      final double[] lam = new double[lambdas.length - 2];
      System.arraycopy (lambdas, 1, lam, 0, lam.length);
      for (int p = 0; p < lam.length; p++)
         lam[p] *= periodDuration;
      performStatTest (arrivProc, lam);
   }

   public void testDirichletCompoundProcess () {
      final double[] alphas = { 33.2, 44.3, 35.4, 51.4, 53.3, 34.3, 62.1 };
      final DirichletCompoundArrivalProcess arrivProc = new DirichletCompoundArrivalProcess (
            pce, factory, alphas, new MRG32k3a (), new MRG32k3a ());
      final RandomVariateGen bgen = new GammaAcceptanceRejectionGen (
            new MRG32k3a (), new GammaDist (0.5, 0.5));
      arrivProc.addNewContactListener (listener);
      arrivProc.init (bgen.nextDouble ());
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      final double[] lam = new double[lambdas.length - 2];
      for (int p = 0; p < lam.length; p++)
         lam[p] = periodDuration * arrivProc.getExpectedArrivalRate (p + 1);
      arrivProc.setBusynessFactor (1.0);
      performStatTest (arrivProc, lam);
   }

   public void testDirichletProcess () {
      final double[] alphas = { 1.2, 3.4, 5.6, 7.8, 8.9, 10.0 };
      final ContactArrivalProcess arrivProc = new DirichletArrivalProcess (pce,
            factory, alphas, new MRG32k3a (), new GammaAcceptanceRejectionGen (
                  new MRG32k3a (), new GammaDist (19.5, 2.5)));
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      final double[] lam = new double[lambdas.length - 2];
      for (int p = 0; p < lam.length; p++)
         lam[p] = periodDuration * arrivProc.getExpectedArrivalRate (p + 1);
      performStatTest (arrivProc, lam);
   }

   public void testNORTADrivenProcess () {
      final DoubleMatrix2D sigma = new DenseDoubleMatrix2D (6, 6);
      for (int p = 0; p < 6; p++)
         sigma.setQuick (p, p, 1.0);
      final double[] gammas = { 29, 42, 15, 30, 60, 25 };
      final double[] probs = { 0.2, 0.1, 0.4, 0.9, 0.5, 0.3 };
      final ContactArrivalProcess arrivProc = new NORTADrivenArrivalProcess (
            pce, factory, sigma, gammas, probs, new MRG32k3a ());
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      final double[] lam = new double[lambdas.length - 2];
      for (int p = 0; p < lam.length; p++)
         lam[p] = periodDuration * arrivProc.getExpectedArrivalRate (p + 1);
      performStatTest (arrivProc, lam);
   }
   
   public void testPoissonArrivalProcessWithInversion () {
      final ContactArrivalProcess arrivProc = new PoissonArrivalProcessWithInversion (
            factory, new MRG32k3a (), lambdat.integralPolynomial (0));
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      performStatTest (arrivProc, lambdaInt);
   }

   public void testPoissonArrivalProcessWithThinning () {
      final ContactArrivalProcess arrivProc = new PoissonArrivalProcessWithThinning (
            factory, new MRG32k3a (), new MRG32k3a(), lambdat, lambdaMax, tMax);
      arrivProc.addNewContactListener (listener);
      arrivProc.init ();
      performBasicTest (arrivProc);
      assertTrue (numContacts > 0);
      performStatTest (arrivProc, lambdaInt);
   }
   
   class CheckNumContactsEvent extends Event {
      int target;

      public CheckNumContactsEvent (int target) {
         this.target = target;
      }

      @Override
      public void actions () {
         assertEquals ("Number of observed contacts", target, numContacts);
      }
   }

   class SetLambdaEvent extends Event {
      PoissonArrivalProcess pp;
      double newLambda;

      public SetLambdaEvent (PoissonArrivalProcess pp, double newLambda) {
         this.pp = pp;
         this.newLambda = newLambda;
      }

      @Override
      public void actions () {
         pp.setLambda (newLambda);
      }
   }
}
