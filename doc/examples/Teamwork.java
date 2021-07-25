import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.TrunkGroup;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;
import umontreal.iro.lecuyer.probdist.TriangularDist;
import umontreal.iro.lecuyer.probdist.UniformDist;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.randvar.RandomVariateGen;
import umontreal.iro.lecuyer.randvar.TriangularGen;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.stat.list.ListOfTallies;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class Teamwork {
   // Agent group identifiers
   static final int RECEPTION       = 0;
   static final int ACCOUNTING      = 1;
   static final int TECHSUP         = 2;
   static final int DEV             = 3;
   static final int MANAGER         = 4;
   static final String[] GROUPNAMES = {
      "Reception", "Accounting", "Technical support", "Development",
      "Manager", "All groups"
   };

   // Input data (times are in minutes)
   static final int P                 = 24*7;
   static final double PERIODDURATION = 60;
   static final int NUMQUEUES         = 2;
   static final int I                 = 5;
   static final int STARTHOUR = 8;
   static final int ENDHOUR = 17;
   static final int NUMTRUNKS         = 25;
   static final double FHARRIVALRATE   = 200;
   static final double ARRIVALRATE     = 100;
   static final double PATIENCETIME    = 2.0;
   static final double MINSERVICETIME  = 0.5;
   static final double MODESERVICETIME = 1.0;
   static final double MAXSERVICETIME  = 5.0;
   // Mean time for after contact work
   static final double ACWTIME         = 1.0;
   static final double PROBCONFACCOUNTING = 0.2;
   static final double PROBTRANSMANAGER   = 0.05;
   static final double PROBCONFDEV        = 0.2;
   // Acceptable waiting time
   static final double AWT                = 120/60.0;
   // 2 receptionists, 5 technical support, and 1 agent in each other groups
   static final int[] NUMAGENTS           = { 2, 1, 5, 1, 1 };
   // Service time is multiplied by 10 for technical support,
   // and by 3 for the manager.
   // Conference time is multiplied by 2 for accounting,
   // and by 5 for developer.
   static final double[] STIMEMULT        = { 1, 2, 10, 3, 5 };
   static final double LEVEL              = 0.95; // Level of conf. int.
   static final int NUMDAYS               = 1000;

   // Known expectations
   static double EXPARRIVALS = (FHARRIVALRATE +
                                (ENDHOUR - STARTHOUR - 1)*ARRIVALRATE)*7;
   static double[] EXPNUMAGENTS = new double[I + 1];
   static {
      for (int i = 0; i < I; i++) {
         EXPNUMAGENTS[i] = 7*PERIODDURATION*(ENDHOUR - STARTHOUR)*NUMAGENTS[i];
         EXPNUMAGENTS[I] += EXPNUMAGENTS[i];
      }
   }

   // Contact center components
   PeriodChangeEvent pce;   // Event marking new periods
   TrunkGroup trunks;       // Manages phone lines
   PiecewiseConstantPoissonArrivalProcess customers;
   AgentGroup[] groups = new AgentGroup[I];
   WaitingQueue[] queues = new WaitingQueue[NUMQUEUES];
   Router router;

   // Random number generators
   RandomVariateGen pgen = new ExponentialGen (new MRG32k3a(), 1.0/PATIENCETIME);
   RandomVariateGen sgen = new TriangularGen
      (new MRG32k3a(), new TriangularDist
       (MINSERVICETIME, MAXSERVICETIME, MODESERVICETIME));
   RandomVariateGen acwgen = new ExponentialGen (new MRG32k3a(), 1.0/ACWTIME);
   RandomVariateGen confGen = new UniformGen (new MRG32k3a(), 0, 1);
   RandomStream probConfAccountingStream = new MRG32k3a();
   RandomStream probTransManagerStream = new MRG32k3a();
   RandomStream probConfDevStream = new MRG32k3a();

   // Counters used during replications
   int numArrived, numOffered, numBlocked, numAbandoned,
      numGoodSL, numServed, numDisconnected;
   double[] Nb = new double[I + 1];
   double sumWaitingTimes, sumServiceTimes, sumTotalTimes;
   GroupVolumeStat[] vstat = new GroupVolumeStat[I];

   // Statistical probes
   Tally arrived = new Tally ("Number of arrived contacts");
   Tally offered = new Tally ("Offered load");
   Tally served = new Tally ("Number of served contacts");
   Tally disconnected = new Tally ("Number of disconnected contacts");
   Tally goodSL = new Tally ("Number of contacts in target");
   Tally serviceLevel = new Tally ("Service level");
   Tally blocked = new Tally ("Number of blocked contacts");
   Tally abandoned = new Tally ("Number of abandoned contacts");
   Tally totalTime = new Tally ("Sojourn time of contacts");
   FunctionOfMultipleMeansTally speedOfAnswer =
      new FunctionOfMultipleMeansTally (new RatioFunction(), "Speed of answer", 2);
   Tally serviceTime = new Tally ("Service time");
   ListOfTallies<Tally> occupancy = ListOfTallies.createWithTally (I + 1);
   Tally trunkBusy = new Tally ("Number of busy trunks");
   Tally trunkAvail = new Tally ("Number of available trunks");

   Teamwork() {
      occupancy.setName ("Agents' occupancy ratio");
      for (int i = 0; i <= I; i++) {
         occupancy.get (i).setName (GROUPNAMES[i]);
         occupancy.get (i).setConfidenceIntervalStudent();
         occupancy.get (i).setConfidenceLevel (LEVEL);
      }
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, 0.0);
      trunks = new TrunkGroup (NUMTRUNKS);
      trunks.setStatCollecting (true);

      final double[] arrivalRates = new double[P + 2];
      for (int day = 0; day < 7; day++) {
         arrivalRates[24*day + STARTHOUR + 1] = FHARRIVALRATE;
         for (int hour = STARTHOUR + 1; hour < ENDHOUR; hour++)
            arrivalRates[24*day + hour + 1] = ARRIVALRATE;
      }
      customers = new PiecewiseConstantPoissonArrivalProcess
         (pce, new MyContactFactory(), arrivalRates, new MRG32k3a());
      customers.setNormalizing (true);

      final int[][] numAgents = new int[I][P + 2];
      for (int day = 0; day < 7; day++)
         for (int hour = STARTHOUR; hour < ENDHOUR; hour++) {
            final int ind = 24*day + hour + 1;
            for (int i = 0; i < I; i++)
               numAgents[i][ind] = NUMAGENTS[i];
         }
      for (int i = 0; i < I; i++) {
         groups[i] = new AgentGroup (pce, numAgents[i]);
         groups[i].setAfterContactTimeGenerator
            (0, new AfterContactGen (i));
         vstat[i] = new GroupVolumeStat (groups[i]);
      }
      for (int q = 0; q < queues.length; q++)
         queues[q] = new StandardWaitingQueue();
      queues[1].setMaximalQueueTimeGenerator
         (1, new ValueGenerator() {
               public void init() {}

               public double nextDouble (Contact contact) {
                  return ((MyContact)contact).pManager;
               }
            });

      router = new MyRouter();
      router.setClearWaitingQueues (true);
      customers.addNewContactListener (router);
      for (int q = 0; q < queues.length; q++)
         router.setWaitingQueue (q, queues[q]);
      for (int i = 0; i < I; i++) router.setAgentGroup (i, groups[i]);
      router.addExitedContactListener (new MyContactMeasures());
   }

   // Create new contacts
   class MyContactFactory implements ContactFactory {
      public Contact newInstance() {
         final Contact contact = new MyContact();
         contact.setTrunkGroup (trunks);
         return contact;
      }
   }

   // Contact subclass with custom attributes
   class MyContact extends Contact {
      double pManager;
      double[] stime = new double[I];
      double[] acwTime = new double[I];
      double uConfAccounting;
      double uTransManager;
      double uConfDev;

      MyContact() {
         setDefaultPatienceTime (pgen.nextDouble());
         pManager = pgen.nextDouble();
         for (int i = 0; i < I; i++) {
            double baseServiceTime;
            if (i == ACCOUNTING || i == DEV) {
               baseServiceTime = confGen.nextDouble();
               acwTime[i] = 0;
            }
            else {
               baseServiceTime = sgen.nextDouble();
               acwTime[i] = acwgen.nextDouble();
            }
            stime[i] = STIMEMULT[i]*baseServiceTime;
         }

         uConfAccounting = probConfAccountingStream.nextDouble();
         uTransManager = probTransManagerStream.nextDouble();
         uConfDev = probConfDevStream.nextDouble();
      }
   }

   // Custom router
   class MyRouter extends Router {
      MyRouter() {
         super (1, NUMQUEUES, I);
      }

      @Override
      public boolean canServe (int i, int k) {
         return true;
      }

      // Agent selection for incoming contacts
      @Override
      protected EndServiceEvent selectAgent (Contact contact) {
         if (groups[RECEPTION].getNumFreeAgents() == 0)
            return null;
         final EndServiceEvent es = groups[RECEPTION].serve (contact);
         return es;
      }

      // Queue selection for incoming contacts, if agent selection fails
      @Override
      protected DequeueEvent selectWaitingQueue (Contact contact) {
         return queues[0].add (contact);
      }

      private WaitingQueue getQueue (int gid) {
         WaitingQueue queue = null;
         switch (gid) {
         case RECEPTION:
            queue = getWaitingQueue (0);
            break;
         case MANAGER:
            queue = getWaitingQueue (1);
            break;
         }
         return queue;
      }

      // Contact selection for receptionists and manager
      @Override
      protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
         final int gid = group.getId();
         final WaitingQueue queue = getQueue (gid);
         if (queue == null)
            return null;
         if (queue.size() == 0)
            return null;
         return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
      }

      // Clear queues when agents are off-duty
      @Override
      protected void checkWaitingQueues (AgentGroup group) {
         final int gid = group.getId();
         final WaitingQueue queue = getQueue (gid);
         if (queue == null)
            return;
         if (!mustClearWaitingQueue (queue.getId()))
            return;
         if (group.getNumAgents() == 0)
            queue.clear (DEQUEUETYPE_NOAGENT);
      }

      // Determines the service times, and manages conferencing
      @Override
      protected void beginService (EndServiceEvent es) {
         final int gid = es.getAgentGroup().getId();
         final MyContact contact = (MyContact)es.getContact();
         switch (gid) {
         case RECEPTION:
            if (contact.uConfAccounting < PROBCONFACCOUNTING)
               new ConferenceEvent (es, groups[ACCOUNTING]).schedule
                  (contact.stime[RECEPTION]);
            else
               es.schedule (contact.stime[RECEPTION]);
            break;
         case TECHSUP:
            if (contact.uConfDev < PROBCONFDEV)
               new ConferenceEvent (es, groups[DEV]).schedule
                  (contact.stime[TECHSUP]);
            else
               es.schedule (contact.stime[TECHSUP]);
            break;
         case ACCOUNTING:
         case DEV:
            break;
         default:
            es.schedule (contact.stime[gid]);
         }
      }

      // At the end of the communication with an agent,
      // manages transfer or exit
      @Override
      protected void endContact (EndServiceEvent es) {
         final AgentGroup group = es.getAgentGroup();
         final MyContact contact = (MyContact)es.getContact();
         if (group == groups[ACCOUNTING] ||
             group == groups[DEV])
            return;
         else if (group == groups[RECEPTION]) {
            if (contact.uTransManager < PROBTRANSMANAGER) {
               // Transfer to manager
               if (groups[MANAGER].getNumAgents() == 0)
                  // Disconnected contact, but counted in offered load
                  exitBlocked (contact, 5);
               else if (groups[MANAGER].getNumFreeAgents() == 0)
                  queues[1].add (contact);
               else
                  groups[MANAGER].serve (contact);
            }
            else // Transfer to technical support
            if (groups[TECHSUP].getNumFreeAgents() == 0)
               // Disconnected contact counted in offered load
               exitBlocked (contact, 5);
            else
               groups[TECHSUP].serve (contact);
         }
         else
            // The contact exits at the manager or technical support
            exitServed (es);
      }

      // Event happening at the end of a service, to manage conferencing
      class ConferenceEvent extends Event {
         // Secondary agent group to conference with (accounting or developer)
         AgentGroup targetGroup;
         // End-service event for primary agent
         EndServiceEvent es;
         // End-service event for secondary agent
         EndServiceEvent esConf;

         ConferenceEvent (EndServiceEvent es,
                          AgentGroup targetGroup) {
            this.es = es;
            this.targetGroup = targetGroup;
         }

         @Override
         public void actions() {
            final MyContact contact = (MyContact)es.getContact();
            if (esConf != null ||
                targetGroup.getNumFreeAgents() == 0) {
               // End conferencing or abort it because
               // there is no agent in the target group.
               if (esConf != null)
                  esConf.getAgentGroup().endContact (esConf, 0);
               es.getAgentGroup().endContact (es, 0);
            }
            else {
               // Start the conference: the contact is
               // served simultaneously by two agents.
               esConf = targetGroup.serve (contact);
               schedule (contact.stime[targetGroup.getId()]);
            }
         }
      }
   }

   // Extracts after-contact work from contacts
   class AfterContactGen implements ValueGenerator {
      private int groupId;

      AfterContactGen (int groupId) {
         this.groupId = groupId;
      }

      public void init() {}

      public double nextDouble (Contact contact) {
         return ((MyContact)contact).acwTime[groupId];
      }
   }

   // Updates counters
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         ++numArrived;
         if (bType == 5) {
            ++numOffered;
            ++numDisconnected;
         }
         else
            ++numBlocked;
      }

      public void dequeued (Router router, DequeueEvent ev) {
         ++numArrived;
         ++numOffered;
         if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_NOAGENT)
            ++numDisconnected;
         else
            ++numAbandoned;
      }

      public void served (Router router, EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         ++numArrived;
         ++numOffered;
         final double qt = contact.getTotalQueueTime();
         if (qt < AWT) ++numGoodSL;
         sumWaitingTimes += qt;
         final double st = ev.getEffectiveContactTime();
         sumServiceTimes += st;
         ++numServed;
         sumTotalTimes += contact.getTotalQueueTime()
            + contact.getTotalServiceTime();
      }
   }

   void simulateOneDay() {
      Sim.init();
      // Initializes period-change event, trunk group, arrival process,
      // waiting queues, and agent groups.
      pce.init();
      trunks.init();
      customers.init();
      ContactCenter.initElements (groups);
      ContactCenter.initElements (queues);
      numArrived = numOffered = numBlocked = numAbandoned = 0;
      sumWaitingTimes = sumServiceTimes = sumTotalTimes = 0;
      numGoodSL = numServed = numDisconnected = 0;
      for (int i = 0; i < I; i++) vstat[i].init();
      customers.start();
      pce.start();
      Sim.start();
      pce.stop();
      addObs();
   }

   void addObs() {
      arrived.add (numArrived);
      offered.add (numOffered);
      served.add (numServed);
      disconnected.add (numDisconnected);
      goodSL.add (numGoodSL);
      blocked.add (numBlocked);
      abandoned.add (numAbandoned);
      serviceTime.add (sumServiceTimes/numServed);
      totalTime.add (sumTotalTimes/numServed);
      serviceLevel.add (100.0*numGoodSL/EXPARRIVALS);
      speedOfAnswer.add (sumWaitingTimes, numServed);

      trunkBusy.add (trunks.getStatLines().average());
      trunkAvail.add (trunks.getCapacity() - trunks.getStatLines().average());

      Nb[I] = 0;
      for (int i = 0; i < I; i++) {
         Nb[i] = vstat[i].getStatNumBusyAgents().sum();
         Nb[I] += Nb[i];
         Nb[i] /= EXPNUMAGENTS[i]/100;
      }
      Nb[I] /= EXPNUMAGENTS[I]/100;
      occupancy.add (Nb);
   }

   void simulate (int n) {
      arrived.init();
      offered.init();
      served.init();
      disconnected.init();
      goodSL.init();
      serviceLevel.init();
      blocked.init();
      abandoned.init();
      totalTime.init();
      speedOfAnswer.init();
      serviceTime.init();
      occupancy.init();
      trunkBusy.init();
      trunkAvail.init();
      for (int r = 0; r < n; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (trunkAvail.reportAndCIStudent (LEVEL, 3));
      System.out.println (trunkBusy.reportAndCIStudent (LEVEL, 3));
      System.out.println (arrived.reportAndCIStudent (LEVEL, 3));
      System.out.println (offered.reportAndCIStudent (LEVEL, 3));
      System.out.println (served.reportAndCIStudent (LEVEL, 3));
      System.out.println (disconnected.reportAndCIStudent (LEVEL, 3));
      System.out.println (blocked.reportAndCIStudent (LEVEL, 3));
      System.out.println (goodSL.reportAndCIStudent (LEVEL, 3));
      System.out.println (abandoned.reportAndCIStudent (LEVEL, 3));
      System.out.println (totalTime.reportAndCIStudent (LEVEL, 3));
      System.out.println (speedOfAnswer.reportAndCIDelta (LEVEL, 3));
      System.out.println (serviceTime.reportAndCIStudent (LEVEL, 3));
      System.out.println (serviceLevel.reportAndCIStudent (LEVEL, 3));
      System.out.println (occupancy.report ());
   }

   public static void main (String[] args) {
      final Teamwork t = new Teamwork();
      final Chrono timer = new Chrono();
      t.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      t.printStatistics();
   }
}
