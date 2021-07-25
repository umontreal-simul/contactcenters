import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;
import umontreal.iro.lecuyer.probdist.GammaDist;
import umontreal.iro.lecuyer.randvar.GammaGen;
import umontreal.iro.lecuyer.randvar.RandomVariateGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.RatioFunction;

public class SimpleMSK {
   // All times are in minutes
   static final int K                   = 3;        // Number of contact types
   static final int I                   = 2;        // Number of agent groups
   static final int P                   = 3;        // Number of periods
   static final double PERIODDURATION   = 120.0;    // Two hours
   // LAMBDA[k][p] gives the arrival rate for type k in period p
   static final double[][] LAMBDA       =
      { { 0, 4.2, 5.3, 3.2, 0 }, { 0, 5.1, 4.3, 4.8, 0 }, { 0, 6.3, 5.2, 4.8, 0 } };
   // Gamma parameter for busyness
   static final double ALPHA0           = 28.7;
   // Service rate for each period
   static final double[] MU   = { 0.5, 0.5, 0.6, 0.4, 0.4 };
   // Abandonment rate for each period
   static final double[] NU   = { 0.3, 0.3, 0.4, 0.2, 0.2 };
   // Acceptable waiting time (20 sec.)
   static final double AWT              = 20/60.0;
   // NUMAGENTS[i][p] gives the number of agents for group i in period p
   static final int[][] NUMAGENTS       = { { 0, 12, 18, 9, 9 }, { 0, 15, 20, 11, 11 } };
   // Routing table, TYPETOGROUPMAP[k] and GROUPTOTYPEMAP[i] contain ordered lists
   static final int[][] TYPETOGROUPMAP  = { { 0 }, { 0, 1 }, { 1 } };
   static final int[][] GROUPTOTYPEMAP = { { 1, 0 }, { 2, 1 } };
   static final double LEVEL            = 0.95;     // Level for confidence intervals
   static final int NUMDAYS             = 1000;     // Number of replications

   PeriodChangeEvent pce;                // Event marking the beginning of each period
   PiecewiseConstantPoissonArrivalProcess[] arrivProc
      = new PiecewiseConstantPoissonArrivalProcess[K];
   AgentGroup[] groups = new AgentGroup[I];
   WaitingQueue[] queues = new WaitingQueue[K];
   Router router;
   RandomVariateGen sgen;                           // Service times generator
   RandomVariateGen pgen;                           // Patience times generator
   RandomVariateGen bgen;                           // Busyness  generator

   // Counters
   int numGoodSL, numServed, numAbandoned, numAbandonedAfterAWT;
   double[][] numGoodSLKP = new double[K][P];
   GroupVolumeStat vstat;              // Integral of the occupancy ratio

   // statistical collectors
   Tally served = new Tally ("Number of served contacts");
   Tally abandoned = new Tally ("Number of contacts having abandoned");
   MatrixOfTallies<Tally> goodSLKP = MatrixOfTallies.createWithTally (K, P);
   FunctionOfMultipleMeansTally serviceLevel = new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Service level", 2);
   FunctionOfMultipleMeansTally occupancy = new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Occupancy ratio", 2);

   SimpleMSK() {
      goodSLKP.setName ("Number of contacts meeting target service level");
      for (int k = 0; k < K; k++)
         for (int p = 0; p < P; p++) {
            goodSLKP.get (k, p).setName ("Type " + k + ", period " + p);
            goodSLKP.get (k, p).setConfidenceIntervalStudent();
            goodSLKP.get (k, p).setConfidenceLevel (LEVEL);
         }
      // One dummy preliminary period, P main periods, and one wrap-up period,
      // main periods start at time 0.
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, 0);
      for (int k = 0; k < K; k++)                     // For each contact type
         arrivProc[k] = new PiecewiseConstantPoissonArrivalProcess
            (pce, new MyContactFactory (k), LAMBDA[k], new MRG32k3a());
      bgen = new GammaGen (new MRG32k3a(), new GammaDist (ALPHA0, ALPHA0));
      for (int i = 0; i < I; i++) groups[i] = new AgentGroup (pce, NUMAGENTS[i]);
      for (int q = 0; q < K; q++) queues[q] = new StandardWaitingQueue();
      sgen = MultiPeriodGen.createExponential (pce, new MRG32k3a(), MU);
      pgen = MultiPeriodGen.createExponential (pce, new MRG32k3a(), NU);
      router = createRouter();
      for (int k = 0; k < K; k++) arrivProc[k].addNewContactListener (router);
      for (int i = 0; i < I; i++) router.setAgentGroup (i, groups[i]);
      for (int q = 0; q < K; q++) router.setWaitingQueue (q, queues[q]);
      router.addExitedContactListener (new MyContactMeasures());
      vstat = new GroupVolumeStat (groups[0]);
   }

   Router createRouter() {
      return new SingleFIFOQueueRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
   }

   // Creates the new contacts
   class MyContactFactory implements ContactFactory {
      int type;
      MyContactFactory (int type) { this.type = type; }
      public Contact newInstance() {
         final Contact contact = new Contact (type);
         contact.setDefaultServiceTime (sgen.nextDouble());
         contact.setDefaultPatienceTime (pgen.nextDouble());
         return contact;
      }
   }

   // Updates counters when a contact exits
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, DequeueEvent ev) {
         ++numAbandoned;
         if (ev.getContact().getTotalQueueTime() >= AWT) ++numAbandonedAfterAWT;
      }
      public void served (Router router, EndServiceEvent ev) {
         ++numServed;
         final Contact contact = ev.getContact();
         if (contact.getTotalQueueTime() < AWT) {
            ++numGoodSL;
            final int period = pce.getPeriod (contact.getArrivalTime()) - 1;
            if (period >= 0 || period < P) ++numGoodSLKP[contact.getTypeId()][period];
         }
      }
   }

   void simulateOneDay() {
      Sim.init();    pce.init();
      final double b = bgen.nextDouble();
      for (int k = 0; k < K; k++) arrivProc[k].init (b);
      for (int i = 0; i < I; i++) groups[i].init();
      for (int q = 0; q < K; q++) queues[q].init();
      numGoodSL = numServed = numAbandoned = numAbandonedAfterAWT = 0;   vstat.init();
      for (int k = 0; k < K; k++) for (int p = 0; p < P; p++) numGoodSLKP[k][p] = 0;
      for (int k = 0; k < K; k++) arrivProc[k].start();
      pce.start();    Sim.start();    // Simulation runs here
      pce.stop();
      served.add (numServed);  abandoned.add (numAbandoned);  goodSLKP.add (numGoodSLKP);
      serviceLevel.add (numGoodSL, numServed + numAbandonedAfterAWT);
      final double Nb = vstat.getStatNumBusyAgents().sum();   // Integral of N_b0(t)
      final double N = vstat.getStatNumAgents().sum();        // Integral of N_0(t)
      final double Ng = vstat.getStatNumGhostAgents().sum();  // Integral of N_g0(t)
      occupancy.add (Nb, N + Ng);
   }

   void simulate (int n) {
      served.init();          abandoned.init();    goodSLKP.init();
      serviceLevel.init();    occupancy.init();
      for (int r = 0; r < n; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (served.reportAndCIStudent (LEVEL, 3));
      System.out.println (abandoned.reportAndCIStudent (LEVEL, 3));
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
      System.out.println (occupancy.reportAndCIDelta (LEVEL, 3));
      for (int k = 0; k < K; k++)
         System.out.println (goodSLKP.rowReport (k));
   }

   public static void main (String[] args) {
      final SimpleMSK s = new SimpleMSK();  s.simulate (NUMDAYS);  s.printStatistics();
   }
}
