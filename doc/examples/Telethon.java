import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.TrunkGroup;
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
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class Telethon {
   // Input data (time is in minutes)
   static final double SERVICETIME     = 10;
   static final double PATIENCETIME    = 2;
   static final double AWT             = 1.0;
   static final int P                  = 24*7;
   static final double PERIODDURATION  = 60.0;
   static final int STARTHOUR          = 6;
   static final int ENDHOUR            = 10;
   static final int NUMTRUNKS          = 24;  // Number of phone lines
   static final int NUMAGENTS          = 12;
   static final double ARRIVALRATE     = 50;
   static final int[][] TYPETOGROUPMAP = { { 0 } };
   static final int[][] GROUPTOTYPEMAP = { { 0 } };
   static final double LEVEL           = 0.95; // Level of confidence intervals
   static final int NUMDAYS            = 1000;

   // Known expectations
   // (ENDHOUR - STARTHOUR) hours per day,
   // Five working days per week
   // ARRIVALRATE contacts per hour
   static final double EXPARRIVALS = ARRIVALRATE*(ENDHOUR - STARTHOUR)*5;
   // Expected value of the integral for the number of agents N(t)
   // NUMAGENTS agents per hour
   static final double EXPNUMAGENTS = NUMAGENTS*(ENDHOUR -
                                                 STARTHOUR)*PERIODDURATION*5;

   // Contact center components
   PeriodChangeEvent pce;   // Event marking new periods
   TrunkGroup trunks;       // Manages phone lines
   PiecewiseConstantPoissonArrivalProcess donors;
   AgentGroup volunteers;
   WaitingQueue queue;
   Router router;

   // Service time generator
   ExponentialGen sgen = new ExponentialGen (new MRG32k3a(), 1.0/SERVICETIME);
   // Patience time generator
   ExponentialGen pgen = new ExponentialGen (new MRG32k3a(), 1.0/PATIENCETIME);

   // Counters and probe used during replications
   int numArrivals, numBlocked, numAbandoned,
      numMessages, numDisconnected, numGoodSL,
      numServed;
   double sumWaitingTimes;
   GroupVolumeStat vstat;

   // Statistical collectors
   Tally arrivals = new Tally ("Number of arrived contacts"),
      served = new Tally ("Number of served contacts"),
      blocked = new Tally ("Number of blocked contacts"),
      abandoned = new Tally ("Number of abandoned contacts"),
      messages = new Tally ("Number of messages"),
      disconnected = new Tally ("Number of disconnected contacts"),
      goodSL = new Tally ("Number of contacts in target"),
      serviceLevel = new Tally ("Service level"),
      occupancy = new Tally ("Agents' occupancy ratio");
   FunctionOfMultipleMeansTally speedOfAnswer =
      new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Speed of answer", 2);

   Telethon() {
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, 0.0);
      final double[] arrivalRates = new double[P+2];
      for (int day = 0; day < 5; day++)
         for (int hour = STARTHOUR; hour < ENDHOUR; hour++)
            arrivalRates[24*day + hour + 1] = ARRIVALRATE;
      donors = new PiecewiseConstantPoissonArrivalProcess
         (pce, new MyContactFactory(), arrivalRates, new MRG32k3a());
      donors.setNormalizing (true);
      trunks = new TrunkGroup (NUMTRUNKS);

      final int[] numAgents = new int[P+2];
      for (int day = 0; day < 5; day++)
         for (int hour = STARTHOUR; hour < ENDHOUR; hour++)
            numAgents[24*day + hour + 1] = NUMAGENTS;
      volunteers = new AgentGroup (pce, numAgents);
      queue = new StandardWaitingQueue();
      queue.setMaximalQueueTimeGenerator
         (5, new ConstantValueGenerator (1, PATIENCETIME));

      router = createRouter();
      router.setClearWaitingQueues (true);
      router.setWaitingQueue (0, queue);
      router.setAgentGroup (0, volunteers);
      donors.addNewContactListener (router);
      router.addExitedContactListener (new MyContactMeasures());
      vstat = new GroupVolumeStat (volunteers);
   }
   Router createRouter() {
      return new QueuePriorityRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
   }

   // Creates new contacts
   class MyContactFactory implements ContactFactory {
      public Contact newInstance() {
         final Contact contact = new Contact();
         contact.setTrunkGroup (trunks);
         contact.setDefaultPatienceTime (pgen.nextDouble());
         contact.setDefaultServiceTime (sgen.nextDouble());
         return contact;
      }
   }

   // Updates counters when a contact exits
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         ++numArrivals;    ++numBlocked;
      }

      public void dequeued (Router router, DequeueEvent ev) {
         ++numArrivals;
         if (ev.getEffectiveDequeueType() == 5)
            ++numMessages;
         else if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_NOAGENT)
            ++numDisconnected;
         else
            ++numAbandoned;
      }

      public void served (Router router, EndServiceEvent ev) {
         ++numArrivals;    ++numServed;
         final double qt = ev.getContact().getTotalQueueTime();
         if (qt < AWT) ++numGoodSL;
         sumWaitingTimes += qt;
      }
   }

   public void simulateOneDay() {
      Sim.init();       pce.init();
      trunks.init();    donors.init();
      queue.init();     volunteers.init();
      numArrivals = numBlocked = numAbandoned
         = numMessages = numDisconnected = numGoodSL
         = numServed = 0;
      sumWaitingTimes = 0;
      vstat.init();
      donors.start();
      pce.start();
      Sim.start();
      pce.stop();
      addObs();
   }

   void addObs() {
      arrivals.add (numArrivals);
      served.add (numServed);
      goodSL.add (numGoodSL);
      serviceLevel.add (100*numGoodSL/EXPARRIVALS);
      final double Nb = vstat.getStatNumBusyAgents().sum();
      occupancy.add (100*Nb/EXPNUMAGENTS);
      blocked.add (numBlocked);
      abandoned.add (numAbandoned);
      messages.add (numMessages);
      disconnected.add (numDisconnected);
      speedOfAnswer.add (sumWaitingTimes, numServed);
   }

   void simulate (int days) {
      arrivals.init();      served.init();        blocked.init();
      abandoned.init();     messages.init();      disconnected.init();
      serviceLevel.init(); speedOfAnswer.init(); occupancy.init();
      goodSL.init();
      for (int r = 0; r < days; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (arrivals.reportAndCIStudent (LEVEL, 3));
      System.out.println (served.reportAndCIStudent (LEVEL, 3));
      System.out.println (blocked.reportAndCIStudent (LEVEL, 3));
      System.out.println (abandoned.reportAndCIStudent (LEVEL, 3));
      System.out.println (messages.reportAndCIStudent (LEVEL, 3));
      System.out.println (disconnected.reportAndCIStudent (LEVEL, 3));
      System.out.println (goodSL.reportAndCIStudent (LEVEL, 3));
      System.out.println (speedOfAnswer.reportAndCIDelta (LEVEL, 3));
      System.out.println (serviceLevel.reportAndCIStudent (LEVEL, 3));
      System.out.println (occupancy.reportAndCIStudent (LEVEL, 3));
   }

   public static void main (String[] args) {
      final Telethon t = new Telethon();
      final Chrono timer = new Chrono();
      t.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      t.printStatistics();
   }
}
