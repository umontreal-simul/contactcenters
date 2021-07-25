import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.TrunkGroup;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.AgentGroupSelectors;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;

import umontreal.iro.lecuyer.probdist.UniformDist;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.stat.list.ListOfTallies;
import umontreal.iro.lecuyer.util.Chrono;

public class Bilingual {
   // Contact type and agent group identifiers and names
   static final int ENGLISH          = 0;
   static final int SPANISH          = 1;
   static final int BILINGUAL        = 2;
   // TYPENAMES[k] gives the name of contact type k, for k = 0 and 1
   // TYPENAMES[K] gives the name for all contact types
   static final String[] TYPENAMES   = {
      "English", "Spanish","all types"
   };
   // GROUPNAMES[i] gives the name of agent group i, for i = 0, 1, and 2
   // GROUPNAMES[I] gives the name for all agent groups
   static final String[] GROUPNAMES  = {
      "English-only", "Spanish-only", "bilingual", "all groups"
   };

   // Input data (times are in minutes)
   static final int K                  = 2;
   static final int I                  = 3;
   static final int P                  = 24*7;
   static final double PERIODDURATION  = 60;
   static final int STARTHOUR          = 8;
   static final int ENDHOUR            = 17;
   static final double FIRSTHOURARRIVALRATE = 100;
   static final double ARRIVALRATE = 50;
   // Capacity of each agent group
   // 7 English-only, 7 Spanish-only, 4 bilingual
   static final int[] NUMAGENTS = { 7, 7, 4 };
   static final int NUMTRUNKS          = 40;
   static final double AWT             = 1.0;
   static final double PTIME           = 2.0;
   static final double STIMEENGLISHMIN = 3.0;
   static final double STIMEENGLISHMAX = 7.0;
   static final double STIMESPANISHMIN = 4.0;
   static final double STIMESPANISHMAX = 6.0;
   static final double PROBCBACK       = 0.75;
   static final double CBACKTIME       = 20.0;
   static final int[][] TYPETOGROUPMAP = {
      // English contact type
      { ENGLISH, BILINGUAL },
      // Spanish contact type
      { SPANISH, BILINGUAL },
   };
   static final int[][] GROUPTOTYPEMAP = {
      // English-only agents
      { ENGLISH },
      // Spanish-only agents
      { SPANISH },
      // Bilingual agents
      { ENGLISH, SPANISH }
   };

   static final double LEVEL         = 0.95;   // Level of conf. int.
   static final int NUMDAYS          = 1000;

   // Known expectations
   static final double EXPARRIVALS = 5*(FIRSTHOURARRIVALRATE +
                                        (ENDHOUR - STARTHOUR - 1)*ARRIVALRATE);
   static double[] EXPNUMAGENTS = new double[I + 1];
   static {
      for (int i = 0; i < I; i++) {
         EXPNUMAGENTS[i] = 5*PERIODDURATION*(ENDHOUR - STARTHOUR)*NUMAGENTS[i];
         EXPNUMAGENTS[I] += EXPNUMAGENTS[i];
      }
   }

   // Contact center components
   PeriodChangeEvent pce;   // Event marking new periods
   TrunkGroup trunks;       // Manages phone lines
   PiecewiseConstantPoissonArrivalProcess[] arrivProc = new
      PiecewiseConstantPoissonArrivalProcess[K];
   AgentGroup[] agentGroups = new AgentGroup[I];
   WaitingQueue[] queues = new WaitingQueue[K];
   Router router;

   // Random streams and random variate generators
   RandomStream agentStream = new MRG32k3a();
   RandomStream streamContactBack = new MRG32k3a();
   // Patience time generator
   ExponentialGen pgen = new ExponentialGen (new MRG32k3a(), 1.0/PTIME);
   // Service time generator, for each contact type
   UniformGen[] sgen   = {
      // English
      new UniformGen (new MRG32k3a(), new UniformDist
                      (STIMEENGLISHMIN, STIMEENGLISHMAX)),
      // Spanish
      new UniformGen (new MRG32k3a(), new UniformDist
                      (STIMESPANISHMIN, STIMESPANISHMAX))
   };

   // Counters and probes used during replications
   double[] numArriv  = new double[K + 1];
   double[] numContactBack = new double[K + 1];
   double[] numGoodSL = new double[K + 1];
   double[] numServed = new double[K + 1];
   double[] numDisconnected = new double[K + 1];
   double[] numAbandoned = new double[K + 1];
   GroupVolumeStat[] vstat = new GroupVolumeStat[I];
   // Used as a temporary variable in addObs
   double[] Nb = new double[I + 1];

   // Statistical collectors
   ListOfTallies<Tally> arrivals = create
      ("Number of arrived contacts", TYPENAMES);
   ListOfTallies<Tally> contactBack = create
      ("Number of retrials after abandonment", TYPENAMES);
   ListOfTallies<Tally> served = create
      ("Number of served contacts", TYPENAMES);
   ListOfTallies<Tally> disconnected = create
      ("Number of disconnected contacts", TYPENAMES);
   ListOfTallies<Tally> abandoned = create
      ("Number of abandoned contacts", TYPENAMES);
   ListOfTallies<Tally> goodSL = create
      ("Number of contacts in target", TYPENAMES);
   ListOfTallies<Tally> serviceLevel = create
      ("Service level", TYPENAMES);
   ListOfTallies<Tally> occupancy = create
      ("Agents' occupancy ratio", GROUPNAMES);

   private ListOfTallies<Tally> create (String name, String[] elementNames) {
      final ListOfTallies<Tally> lt = ListOfTallies.createWithTally (elementNames.length);
      lt.setName (name);
      for (int i = 0; i < elementNames.length; i++) {
         lt.get (i).setName (elementNames[i]);
         lt.get (i).setConfidenceIntervalStudent();
         lt.get (i).setConfidenceLevel (LEVEL);
      }
      return lt;
   }

   Bilingual() {
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, 0.0);
      trunks = new TrunkGroup (NUMTRUNKS);

      final double[] arrivalRates = new double[P + 2];
      for (int day = 0; day < 5; day++) {
         arrivalRates[24*day + STARTHOUR + 1] = FIRSTHOURARRIVALRATE;
         for (int hour = STARTHOUR + 1; hour < ENDHOUR; hour++)
            arrivalRates[24*day + hour + 1] = ARRIVALRATE;
      }
      for (int k = 0; k < K; k++) {
         arrivProc[k] = new PiecewiseConstantPoissonArrivalProcess
            (pce, new MyContactFactory (k), arrivalRates, new MRG32k3a());
         arrivProc[k].setNormalizing (true);
      }

      final int[][] numAgents = new int[I][P + 2];
      for (int day = 0; day < 5; day++)
         for (int hour = STARTHOUR; hour < ENDHOUR; hour++) {
            final int ind = 24*day + hour + 1;
            for (int i = 0; i < I; i++)
               numAgents[i][ind] = NUMAGENTS[i];
         }
      for (int i = 0; i < I; i++) {
         agentGroups[i] = new AgentGroup (pce, numAgents[i]);
         vstat[i] = new GroupVolumeStat (agentGroups[i]);
      }
      for (int q = 0; q < K; q++)
         queues[q] = new StandardWaitingQueue();

      router = new MyRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
      for (int k = 0; k < K; k++) arrivProc[k].addNewContactListener (router);
      for (int q = 0; q < K; q++) router.setWaitingQueue (q, queues[q]);
      for (int i = 0; i < I; i++) router.setAgentGroup (i, agentGroups[i]);
      router.setClearWaitingQueues (true);
      router.addExitedContactListener (new MyContactMeasures());
   }

   // Add a flag for determining if the contacts have retried
   // after abandoning.
   class MyContact extends Contact {
      boolean contactBack = false;
      MyContact (int type) {
         super (type);
      }
   }

   // Creates new contacts
   class MyContactFactory implements ContactFactory {
      int type;
      MyContactFactory (int type) { this.type = type; }

      public Contact newInstance() {
         final MyContact contact = new MyContact (type);
         contact.setTrunkGroup (trunks);
         contact.setDefaultPatienceTime (pgen.nextDouble());
         contact.setDefaultServiceTime (sgen[type].nextDouble());
         return contact;
      }
   }

   // Implements random agent selection and retrials.
   class MyRouter extends SingleFIFOQueueRouter {
      MyRouter (int[][] typeToGroupMap, int[][] groupToTypeMap) {
         super (typeToGroupMap, groupToTypeMap);
      }

      // Random agent selection
      @Override
      protected EndServiceEvent selectAgent (Contact contact) {
         final int tid = contact.getTypeId();
         final AgentGroup group = AgentGroupSelectors.selectUniform
            (this, typeToGroupMap[tid], agentStream);
         if (group == null) return null;
         return group.serve (contact);
      }

      // Retrial with some probability for abandoned contacts
      @Override
      protected void dequeued (DequeueEvent ev) {
         if (ev.getEffectiveDequeueType() == 0) return;
         if (ev.getEffectiveDequeueType() == 1) {
            final double u = streamContactBack.nextDouble();
            if (u <= PROBCBACK)
               new ContactBackEvent (ev.getContact()).schedule (CBACKTIME);
         }
         exitDequeued (ev);
      }
   }

   // Event happening when a customer contacts back
   class ContactBackEvent extends Event {
      Contact contact;
      ContactBackEvent (Contact contact) { this.contact = contact; }

      @Override
      public void actions() {
         contact.setExited (false);
         ((MyContact)contact).contactBack = true;
         router.newContact (contact);
      }
   }

   // Updates counters
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         countArrival (contact);
      }

      private void countArrival (Contact contact) {
         final int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         if (((MyContact)contact).contactBack) {
            ++numContactBack[type];
            ++numContactBack[K];
         }
      }

      public void dequeued (Router router, DequeueEvent ev) {
         final Contact contact = ev.getContact();
         countArrival (contact);
         final int type = contact.getTypeId();
         if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_NOAGENT) {
            ++numDisconnected[type];
            ++numDisconnected[K];
         }
         else {
            ++numAbandoned[type];
            ++numAbandoned[K];
         }
      }

      public void served (Router router, EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         countArrival (contact);
         final int type = contact.getTypeId();
         ++numServed[type];
         ++numServed[K];
         if (contact.getTotalQueueTime() < AWT) {
            ++numGoodSL[type];
            ++numGoodSL[K];
         }
      }
   }

   void simulateOneDay() {
      Sim.init();
      // Initializes period-change event, trunk group, arrival processes,
      // waiting queues, and agent groups.
      pce.init();
      trunks.init();
      ContactCenter.initElements (arrivProc);
      ContactCenter.initElements (agentGroups);
      ContactCenter.initElements (queues);
      for (int k = 0; k <= K; k++) {
         numArriv[k] = 0;
         numServed[k] = 0;
         numGoodSL[k] = 0;
         numDisconnected[k] = 0;
         numAbandoned[k] = 0;
         numContactBack[k] = 0;
      }
      for (final GroupVolumeStat element : vstat)
         element.init();
      ContactCenter.toggleElements (arrivProc, true);
      pce.start();
      Sim.start();
      pce.stop();
      addObs();
   }

   protected void addObs() {
      arrivals.add (numArriv);
      contactBack.add (numContactBack);
      served.add (numServed);
      disconnected.add (numDisconnected);
      abandoned.add (numAbandoned);
      goodSL.add (numGoodSL);
      for (int k = 0; k < K; k++)
         numGoodSL[k] /= EXPARRIVALS/100.0;
      numGoodSL[K] /= K*EXPARRIVALS/100.0;
      serviceLevel.add (numGoodSL);
      Nb[I] = 0;
      for (int i = 0; i < I; i++) {
         Nb[i] = vstat[i].getStatNumBusyAgents().sum();
         Nb[I] += Nb[i];
         Nb[i] /= EXPNUMAGENTS[i]/100.0;
      }
      Nb[I] /= EXPNUMAGENTS[I]/100.0;
      occupancy.add (Nb);
   }

   void simulate (int days) {
      arrivals.init();     served.init();       disconnected.init();
      abandoned.init();    serviceLevel.init(); occupancy.init();
      goodSL.init();       contactBack.init();
      for (int r = 0; r < days; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (arrivals.report ());
      System.out.println (contactBack.report ());
      System.out.println (served.report ());
      System.out.println (disconnected.report ());
      System.out.println (abandoned.report ());
      System.out.println (goodSL.report ());
      System.out.println (serviceLevel.report ());
      System.out.println (occupancy.report ());
   }

   public static void main (String[] args) {
      final Bilingual b = new Bilingual();
      final Chrono timer = new Chrono();
      b.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      b.printStatistics();
   }
}
