package umontreal.iro.lecuyer.contactcenters.contact;

import junit.framework.TestCase;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ToggleEvent;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.GammaGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Sim;

public class ContactArrivalProcessMLETest extends TestCase {
   private ContactFactory factory;
   private PeriodChangeEvent pce;
   private ArrivalCounter arrivCount;

   private static final int P = 6;
   private static final int N = 500;
   private static final double[] lambdas = { 0, 25, 75, 50, 10, 25, 100, 0 };
   private static final double[] glambdas = { 0, 2.5, 1.3, 0.5, 1.0, 2.5, 2.2,
         0 };
   private static final double alpha0 = 10;
   private static final double[] dalphas = { 22, 15, 31, 39.15, 32.3, 11.2 };
   private static final double[] dcalphas = { 15.3, 22, 15, 31, 39.15, 32.3,
         11.2 };
   private static final double dgalpha = 300;
   private static final double dglambda = 1.5;

   public ContactArrivalProcessMLETest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      factory = new SimpleContactFactory ();
      pce = new PeriodChangeEvent (500.0, P + 2, 0);
      arrivCount = new ArrivalCounter ();
   }

   @Override
   public void tearDown () {
      pce = null;
      factory = null;
      arrivCount = null;
   }

   public void testMLENHPP () {
      PiecewiseConstantPoissonArrivalProcess pap = new PiecewiseConstantPoissonArrivalProcess (
            pce, factory, lambdas, new MRG32k3a ());
      pap.setNormalizing (true);
      pap.addNewContactListener (arrivCount);
      final int[][] arrivals = generateArrivals (pap, N);
      pap = PiecewiseConstantPoissonArrivalProcess.getInstanceFromMLE (pce,
            factory, new MRG32k3a (), arrivals, N, P, false);
      final double[] tlambdas = pap.getLambdas ();
      for (int p = 1; p <= P; p++)
         assertEquals ("lambda[" + p + "]", lambdas[p], tlambdas[p], Math
               .sqrt (lambdas[p]));
   }

   public void testMLEPoissonGamma () {
      final double[] galphas = new double[glambdas.length];
      for (int p = 0; p < galphas.length; p++)
         galphas[p] = lambdas[p] * glambdas[p];
      PoissonGammaArrivalProcess pgp = new PoissonGammaArrivalProcess (pce,
            factory, galphas, glambdas, new MRG32k3a (), new MRG32k3a ());
      pgp.setNormalizing (true);
      pgp.addNewContactListener (arrivCount);
      final int[][] arrivals = generateArrivals (pgp, N);
      pgp = PoissonGammaArrivalProcess.getInstanceFromMLE (pce, factory,
            new MRG32k3a (), new MRG32k3a (), arrivals, N, P);
      final double[] tgalphas = pgp.getGammaAlphas ();
      final double[] tglambdas = pgp.getGammaLambdas ();
      for (int p = 1; p <= P; p++) {
         final double gstdDev = Math.sqrt (galphas[p]
               / (glambdas[p] * glambdas[p]));
         assertEquals ("galphas[" + p + "]", galphas[p], tgalphas[p],
               20 * gstdDev);
         assertEquals ("glambdas[" + p + "]", glambdas[p], tglambdas[p],
               gstdDev);
      }
   }

   public void testMLENegMulti () {
      PiecewiseConstantPoissonArrivalProcess pap = new PiecewiseConstantPoissonArrivalProcess (
            pce, factory, lambdas, new MRG32k3a ());
      pap.setNormalizing (true);
      pap.addNewContactListener (arrivCount);
      final RandomVariateGen bgen = new GammaGen (new MRG32k3a (),
            new GammaDist (alpha0, alpha0));
      final double astdDev = bgen.getDistribution ().getStandardDeviation ();
      final int[][] arrivals = generateArrivals (pap, bgen, N);
      pap = PiecewiseConstantPoissonArrivalProcess.getInstanceFromMLE (pce,
            factory, new MRG32k3a (), arrivals, N, P, true);
      assertEquals ("alpha0", alpha0,
            PiecewiseConstantPoissonArrivalProcess.s_bgammaParam, 100 * astdDev);
      final double[] tlambdas = pap.getLambdas ();
      for (int p = 1; p <= P; p++)
         assertEquals ("lambda[" + p + "]", lambdas[p], tlambdas[p], Math
               .sqrt (lambdas[p]));
   }

   public void testMLEDirichletCompound () {
      DirichletCompoundArrivalProcess dap = new DirichletCompoundArrivalProcess (
            pce, factory, dcalphas, new MRG32k3a (), new MRG32k3a ());
      dap.setNormalizing (true);
      dap.addNewContactListener (arrivCount);
      final RandomVariateGen bgen = new GammaGen (new MRG32k3a (),
            new GammaDist (alpha0, 1.0));
      final double astdDev = bgen.getDistribution ().getStandardDeviation ();
      final int[][] arrivals = generateArrivals (dap, bgen, N);
      dap = DirichletCompoundArrivalProcess.getInstanceFromMLE (pce, factory,
            new MRG32k3a (), new MRG32k3a (), arrivals, N, P);
      assertEquals ("gamma", alpha0,
            PiecewiseConstantPoissonArrivalProcess.s_bgammaParam, 100 * astdDev);
      for (int p = 0; p <= P; p++)
         assertEquals ("dalpha[" + p + "]", dcalphas[p], dap.getAlpha (p), Math
               .sqrt (dcalphas[p]));
   }

   public void testMLEDirichlet () {
      final RandomVariateGen agen = new GammaGen (new MRG32k3a (),
            new GammaDist (dgalpha, dglambda));
      final double gstdDev = agen.getDistribution ().getStandardDeviation ();
      DirichletArrivalProcess dap = new DirichletArrivalProcess (pce, factory,
            dalphas, new MRG32k3a (), agen);
      dap.addNewContactListener (arrivCount);
      final int[][] arrivals = generateArrivals (dap, N);
      dap = DirichletArrivalProcess.getInstanceFromMLE (pce, factory,
            new MRG32k3a (), new MRG32k3a (), GammaDist.class, arrivals, N, P);
      final Distribution dist = dap.getNumArrivalsGenerator ()
            .getDistribution ();
      final GammaDist gdist = (GammaDist) dist;
      assertEquals ("dgalpha", dgalpha, gdist.getAlpha (), 2.5 * gstdDev);
      assertEquals ("dglambda", dglambda, gdist.getLambda (), gstdDev);
      for (int p = 0; p < P; p++)
         assertEquals ("dalpha[" + p + "]", dalphas[p], dap.getAlpha (p), Math
               .sqrt (dalphas[p]));
   }

   private int[][] generateArrivals (ContactArrivalProcess arrivProc,
         RandomVariateGen bgen, int n) {
      final int[][] arrivals = new int[n][];
      for (int i = 0; i < n; i++)
         arrivals[i] = generateArrivalsDay (arrivProc, bgen.nextDouble ());
      return arrivals;
   }

   private int[][] generateArrivals (ContactArrivalProcess arrivProc, int n) {
      final int[][] arrivals = new int[n][];
      for (int i = 0; i < n; i++)
         arrivals[i] = generateArrivalsDay (arrivProc, 1.0);
      return arrivals;
   }

   private int[] generateArrivalsDay (ContactArrivalProcess arrivProc, double b) {
      Sim.init ();
      pce.init ();
      arrivProc.init (b);
      pce.start ();
      new ToggleEvent (arrivProc, true).schedule (pce.getPeriodEndingTime (0));
      new ToggleEvent (arrivProc, false).schedule (pce.getPeriodEndingTime (P));
      arrivCount.init ();
      Sim.start ();
      return arrivCount.getNumArrivals ().clone ();
   }

   private class ArrivalCounter implements NewContactListener {
      private final int[] numArrivals = new int[pce.getNumMainPeriods ()];

      public int[] getNumArrivals () {
         return numArrivals;
      }

      public void init () {
         for (int i = 0; i < numArrivals.length; i++)
            numArrivals[i] = 0;
      }

      public void newContact (Contact contact) {
         final int p = pce.getCurrentMainPeriod ();
         ++numArrivals[p];
      }
   }
}
