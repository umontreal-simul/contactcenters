import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Simulator;
import umontreal.iro.lecuyer.simevents.UnusableSimulator;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;


public class MMCSim {
   // Input data (times are in minutes)
   static final double LAMBDA          = 4.2;      // Arrival rate
   static final double MU              = 0.5;      // Service rate
   static final double AWT             = 20/60.0;  // Acceptable waiting time
   static final int NUMAGENTS          = 12;
   static final int[][] TYPETOGROUPMAP = { { 0 } };
   static final int[][] GROUPTOTYPEMAP = { { 0 } };
   static final double LEVEL           = 0.95;     // Level of conf. int.
   static final double DAYLENGTH       = 8.0*60.0; // Eight hours
   static final int NUMDAYS            = 1000;

   // Contact center components
   Simulator sim;
   ContactArrivalProcess arrivProc;
   AgentGroup agents;
   WaitingQueue queue;
   Router router;

   // Service time generator
   ExponentialGen sgen = new ExponentialGen (new MRG32k3a(), MU);

   // Counters and probe used during replications
   int numGoodSL, numServed;
   double sumWaitingTimes;
   GroupVolumeStat vstat;
   double Nb, N;

   // Collectors with one obs./rep.
   Tally goodSL = new Tally ("Number of contacts in target");
   FunctionOfMultipleMeansTally serviceLevel = new FunctionOfMultipleMeansTally
         (new RatioFunction(), "Service level", 2),
      occupancy = new FunctionOfMultipleMeansTally
         (new RatioFunction(), "Occupancy ratio", 2),
      speedOfAnswer = new FunctionOfMultipleMeansTally
         (new RatioFunction(), "Speed of answer", 2);

   MMCSim() {
      sim = new Simulator();
      arrivProc = new PoissonArrivalProcess
         (sim, new MyContactFactory(), LAMBDA, new MRG32k3a());
      agents = new AgentGroup (NUMAGENTS);
      queue = new StandardWaitingQueue();
      router = new QueuePriorityRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
      arrivProc.addNewContactListener (router);
      router.setAgentGroup (0, agents);
      router.setWaitingQueue (0, queue);
      router.addExitedContactListener (new MyContactMeasures());
      vstat = new GroupVolumeStat (sim, agents);
   }

   // Creates the new contacts
   class MyContactFactory implements ContactFactory {
      public Contact newInstance() {
         final Contact contact = new Contact (sim);
         contact.setDefaultServiceTime (sgen.nextDouble());
         return contact;
      }
   }

   // Updates counters when a contact exits
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, DequeueEvent ev) {}
      public void served (Router router, EndServiceEvent ev) {
         ++numServed;
         final double qt = ev.getContact().getTotalQueueTime();
         if (qt < AWT) ++numGoodSL;
         sumWaitingTimes += qt;
      }
   }

   class EndSimEvent extends Event {
      public EndSimEvent (Simulator sim) { super (sim); }
      @Override
      public void actions() { endSim(); }
   }

   void endSim() {
      arrivProc.stop();
      Nb = vstat.getStatNumBusyAgents().sum();   // Int. for N_b0(t)
      N = vstat.getStatNumAgents().sum();        // Int. for N_0(t)
   }

   void simulateOneDay() {
      sim.init();      // Initialize clock and clear event list
      new EndSimEvent (sim).schedule (DAYLENGTH);
      arrivProc.init();    agents.init();    queue.init();
      numServed = numGoodSL = 0;
      sumWaitingTimes = 0;
      vstat.init();
      arrivProc.start();
      sim.start();    // Simulation runs here
      addObs();
   }

   void addObs() {
      goodSL.add (numGoodSL);
      serviceLevel.add (numGoodSL, numServed);
      speedOfAnswer.add (sumWaitingTimes, numServed);
      occupancy.add (Nb, N);
   }

   void simulate (int days) {
      goodSL.init();
      serviceLevel.init();
      occupancy.init();
      for (int r = 0; r < days; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (goodSL.reportAndCIStudent (LEVEL, 3));
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
      System.out.println (speedOfAnswer.reportAndCIDelta (LEVEL, 3));
      System.out.println (occupancy.reportAndCIDelta (LEVEL, 3));
   }

   public static void main (String[] args) {
      Simulator.defaultSimulator = new UnusableSimulator();
      final MMCSim s = new MMCSim();
      final Chrono timer = new Chrono();
      s.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      s.printStatistics();
   }
}
