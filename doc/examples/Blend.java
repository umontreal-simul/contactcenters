import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeListener;
import umontreal.iro.lecuyer.contactcenters.ToggleEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.dialer.DialerPolicy;
import umontreal.iro.lecuyer.contactcenters.dialer.InfiniteDialerList;
import umontreal.iro.lecuyer.contactcenters.dialer.ThresholdDialerPolicy;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.QueueSizeStat;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupSet;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;
import umontreal.iro.lecuyer.probdist.GammaDist;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.randvar.GammaAcceptanceRejectionGen;
import umontreal.iro.lecuyer.randvar.GammaGen;
import umontreal.iro.lecuyer.randvar.RandomVariateGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.stat.list.ListOfFunctionOfMultipleMeansTallies;
import umontreal.iro.lecuyer.stat.list.ListOfTallies;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class Blend {
   // Contact type and agent group identifiers and names
   static final int INBOUND = 0;
   static final int OUTBOUND = 1;
   static final int BLEND = 1;
   // Input data (times are in minutes)
   static final String[] TYPENAMES = {
      "Inbound", "Outbound", "All types"
   };
   static final String[] GROUPNAMES = {
      "Inbound only", "Blend", "All groups"
   };
   // Input data (all times are in minutes)
   static final int K = 2;
   static final int I = 2;
   static final int P = 3;
   static final double PERIODDURATION = 120.0;
   static final double STARTDAY       = 8*60;
   static final double STARTDIALER    = 11*60;
   static final double[] ARRIVALRATES = {
      0, 68.45/30, 72.93/30, 71.92/30, 0
   };
   static final double ALPHA0               = 29.7;
   static final double PROBBALK             = 0.005;
   static final double INPATIENCETIME       = 500/60.0;
   static final double ALPHA                = 0.755;
   static final double BETA                 = 753.8/60.0;
   static final double AWT                  = 20.0/60.0;
   static final double OUTSERVICETIME       = 440.2/60.0;
   static final double OUTPATIENCETIME      = 5.0/60.0;
   static final double[] PROBREACH = { 0.28, 0.28, 0.29, 0.29, 0.29 };
   static final int MINFREEAGENTS = 4;
   static final double KAPPA = 2.0;
   static final int C        = 0;
   static final int[] INBOUNDAGENTS = { 0, 23, 23, 21, 21 };
   static final int[] BLENDAGENTS   = { 0, 16, 18, 16, 16 };
   static final int QUEUECAPACITY   = 80;
   static final int[][] TYPETOGROUPMAP = {
      // Inbound calls
      { 0, 1 },
      // Outbound calls
      { 1 }
   };
   static final int[][] GROUPTOTYPEMAP = {
      // Inbound-only agents
      { 0 },
      // Blend agents
      { 1, 0 }
   };
   static final double LEVEL          = 0.95;  // Level of conf. int.
   static final int NUMDAYS           = 1000;

   // Contact center components
   PeriodChangeEvent pce;  // Event marking beginning of periods
   Dialer dialer;
   Router router;
   PiecewiseConstantPoissonArrivalProcess arrivProc;
   AgentGroup inboundAgents;
   AgentGroup blendAgents;
   WaitingQueue inQueue;
   WaitingQueue outQueue;

   // Random number generators
   // Busyness generator
   RandomVariateGen bgen = new GammaGen
      (new MRG32k3a(), new GammaDist (ALPHA0, ALPHA0));
   RandomStream streamBalk = new MRG32k3a();
   // Service times for inbound calls
   RandomVariateGen sigen = new GammaAcceptanceRejectionGen
         (new MRG32k3a(), new GammaDist (ALPHA, 1.0/BETA));
   // Service times for outbound calls
   RandomVariateGen sogen = new ExponentialGen (new MRG32k3a(), 1.0/OUTSERVICETIME);
   // Patience times for inbound calls
   RandomVariateGen pigen = new ExponentialGen (new MRG32k3a(), 1.0/INPATIENCETIME);

   // Counters
   int numTriedDialed;
   double[] numArriv = new double[K + 1];
   double[] numServed = new double[K + 1];
   double[] numAbandoned = new double[K + 1];
   double numBadSL;
   double[] numPosWait = new double[K + 1];
   double[] sumWaitingTimes = new double[K + 1];
   QueueSizeStat inQueueSize;
   QueueSizeStat outQueueSize;
   GroupVolumeStat inboundVolume;
   GroupVolumeStat blendVolume;
   double[] Nb = new double[I + 1];
   double[] N = new double[I + 1];
   double[] qs = new double[K + 1];

   // Statistical collectors
   Tally triedDial = new Tally ("Number of tried outbound calls");
   ListOfTallies<Tally> arrivals = create
      ("Number of arrived contacts", TYPENAMES);
   ListOfTallies<Tally> served = create
      ("Number of served contacts", TYPENAMES);
   ListOfTallies<Tally> abandoned = create
      ("Number of abandoned contacts", TYPENAMES);
   Tally badSL = new Tally ("Number of contacts not in target");
   FunctionOfMultipleMeansTally serviceLevel =
      new FunctionOfMultipleMeansTally (new RatioFunction(), "Service level", 2);
   ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> occupancy =
      createRatio ("Agents' occupancy ratio", GROUPNAMES);
   ListOfTallies<Tally> queueSize =
      create ("Time-average queue size", TYPENAMES);
   ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> totalQueueWait =
      createRatio ("Waiting time", TYPENAMES);
   ListOfTallies<Tally> posWait =
      create ("Number of contacts having to wait", TYPENAMES);

   private ListOfTallies<Tally> create
   (String name, String[] elementNames) {
      final ListOfTallies<Tally> lt =
         ListOfTallies.createWithTally (elementNames.length);
      lt.setName (name);
      for (int i = 0; i < elementNames.length; i++) {
         lt.get (i).setName (elementNames[i]);
         lt.get (i).setConfidenceIntervalStudent();
         lt.get (i).setConfidenceLevel (LEVEL);
      }
      return lt;
   }

   private ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally>
   createRatio (String name, String[] elementNames) {
      final RatioFunction ratio = new RatioFunction();
      final ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta =
         ListOfFunctionOfMultipleMeansTallies.create (ratio, 2, elementNames.length);
      mta.setName (name);
      for (int r = 0; r < elementNames.length; r++) {
         mta.get (r).setName (elementNames[r]);
         mta.get (r).setConfidenceIntervalDelta();
         mta.get (r).setConfidenceLevel (LEVEL);
      }
      return mta;
   }

   Blend() {
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, STARTDAY);
      pce.addPeriodChangeListener (new MyPeriodChangeListener());
      inboundAgents = new AgentGroup (pce, INBOUNDAGENTS);
      inboundVolume = new GroupVolumeStat (inboundAgents);
      blendAgents = new AgentGroup (pce, BLENDAGENTS);
      blendVolume = new GroupVolumeStat (blendAgents);

      inQueue = new StandardWaitingQueue();
      inQueueSize = new QueueSizeStat (inQueue);
      outQueue = new StandardWaitingQueue();
      outQueueSize = new QueueSizeStat (outQueue);

      final AgentGroupSet testSet = new AgentGroupSet();
      testSet.add (inboundAgents);
      testSet.add (blendAgents);
      final AgentGroupSet targetSet = new AgentGroupSet();
      targetSet.add (blendAgents);
      final DialerPolicy pol = new ThresholdDialerPolicy
         (new InfiniteDialerList (new MyContactFactory (OUTBOUND)),
          testSet, targetSet, MINFREEAGENTS, 1, KAPPA, C);

      final ConstantValueGenerator pReachGen = new ConstantValueGenerator
         (pce, K, PROBREACH);
      dialer = new Dialer (pol, new MRG32k3a(), pReachGen);
      final ContactCounter ntd = new ContactCounter();
      dialer.addReachListener (ntd);
      dialer.addFailListener (ntd);

      arrivProc = new PiecewiseConstantPoissonArrivalProcess
         (pce, new MyContactFactory (INBOUND), ARRIVALRATES,
          new MRG32k3a());

      router = new QueuePriorityRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
      arrivProc.addNewContactListener (router);
      dialer.addReachListener (router);
      router.addExitedContactListener (new MyContactMeasures());
      router.setAgentGroup (INBOUND, inboundAgents);
      router.setAgentGroup (BLEND, blendAgents);
      router.getDialers (INBOUND).add (dialer);
      router.getDialers (BLEND).add (dialer);
      router.setWaitingQueue (INBOUND, inQueue);
      router.setWaitingQueue (OUTBOUND, outQueue);
      router.setTotalQueueCapacity (QUEUECAPACITY);
   }

   // Creates contacts
   class MyContactFactory implements ContactFactory {
      int type;
      public MyContactFactory (int type) { this.type = type; }

      public Contact newInstance() {
         final Contact contact = new Contact (type);
         switch (type) {
         case INBOUND:
            // Inbound contact
            contact.setDefaultContactTime (sigen.nextDouble());
            final double u = streamBalk.nextDouble();
            if (u < PROBBALK) contact.setDefaultPatienceTime (0);
            else contact.setDefaultPatienceTime (pigen.nextDouble());
            break;
         case OUTBOUND:
            // Outbound contact
            contact.setDefaultContactTime (sogen.nextDouble());
            contact.setDefaultPatienceTime (OUTPATIENCETIME);
            break;
         }
         return contact;
      }
   }

   // Updates counters
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         if (contact.getArrivalTime() < STARTDAY)
            return;
         final int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numAbandoned[type];
         ++numAbandoned[K];
         ++numPosWait[type];
         ++numPosWait[K];
      }

      public void dequeued (Router router, DequeueEvent ev) {
         final Contact contact = ev.getContact();
         if (contact.getArrivalTime() < STARTDAY)
            return;
         final int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numAbandoned[type];
         ++numAbandoned[K];
         final double qt = contact.getTotalQueueTime();
         sumWaitingTimes[type] += qt;
         sumWaitingTimes[K] += qt;
         ++numPosWait[type];
         ++numPosWait[K];
         if (qt >= AWT && type == INBOUND) ++numBadSL;
      }

      public void served (Router router, EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         if (contact.getArrivalTime() < STARTDAY)
            return;
         final int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numServed[type];
         ++numServed[K];
         final double qt = contact.getTotalQueueTime();
         sumWaitingTimes[type] += qt;
         sumWaitingTimes[K] += qt;
         if (qt > 0) {
            ++numPosWait[type];
            ++numPosWait[K];
         }
         if (qt >= AWT && type == INBOUND) ++numBadSL;
      }
   }

   // Counts tried outbound calls
   class ContactCounter implements NewContactListener {
      public void newContact (Contact contact) {
         if (contact.getArrivalTime() < STARTDAY)
            return;
         ++numTriedDialed;
      }
   }

   // Ensures that integrals are computed over main periods only
   class MyPeriodChangeListener implements PeriodChangeListener {
      public void changePeriod (PeriodChangeEvent pce) {
         if (pce.getCurrentPeriod() == 1) {
            // End of preliminary period
            inQueueSize.init();
            outQueueSize.init();
            inboundVolume.init();
            blendVolume.init();
         }
         else if (pce.getCurrentPeriod() == P + 1) {
            // Beginning of wrap-up period
            Nb[INBOUND] = inboundVolume.getStatNumBusyAgents().sum();
            N[INBOUND] = inboundVolume.getStatNumAgents().sum() +
               inboundVolume.getStatNumGhostAgents().sum();
            Nb[BLEND] = blendVolume.getStatNumBusyAgents().sum();
            N[BLEND] = blendVolume.getStatNumAgents().sum() +
               blendVolume.getStatNumGhostAgents().sum();
            Nb[2] = Nb[INBOUND] + Nb[BLEND];
            N[2] = N[INBOUND] + N[BLEND];

            qs[INBOUND] = inQueueSize.getStatQueueSize().sum()/(PERIODDURATION*P);
            qs[OUTBOUND] = outQueueSize.getStatQueueSize().sum()/(PERIODDURATION*P);
            qs[2] = qs[INBOUND] + qs[OUTBOUND];
         }
      }

      public void stop (PeriodChangeEvent pce) {}
   }

   void simulate (int n) {
      triedDial.init();
      arrivals.init();
      served.init();
      abandoned.init();
      badSL.init();
      serviceLevel.init();
      occupancy.init();
      queueSize.init();
      totalQueueWait.init();
      posWait.init();
      for (int r = 0; r < n; r++) simulateOneDay();
   }

   void simulateOneDay() {
      Sim.init();
      pce.init();
      arrivProc.init (bgen.nextDouble());
      dialer.init();
      inboundAgents.init();
      blendAgents.init();
      inQueue.init();
      outQueue.init();
      numTriedDialed = 0;
      numBadSL = 0;
      for (int k = 0; k <= K; k++) {
         numArriv[k] = 0;
         numServed[k] = 0;
         numAbandoned[k] = 0;
         numPosWait[k] = 0;
         sumWaitingTimes[k] = 0;
      }
      new ToggleEvent (arrivProc, true).schedule (STARTDAY - 5);
      new ToggleEvent (dialer, true).schedule (STARTDIALER);
      final double endDay = STARTDAY + P*PERIODDURATION;
      new ToggleEvent (dialer, false).schedule (endDay - 5);
      pce.start();
      Sim.start();
      pce.stop();
      addObs();
   }

   void addObs() {
      triedDial.add (numTriedDialed);
      arrivals.add (numArriv);
      served.add (numServed);
      abandoned.add (numAbandoned);
      badSL.add (numBadSL);
      serviceLevel.add (100*(numArriv[INBOUND] - numBadSL), numArriv[INBOUND]);
      for (int i = 0; i < Nb.length; i++)
         Nb[i] *= 100;
      occupancy.addSameDimension (Nb, N);
      queueSize.add (qs);
      totalQueueWait.addSameDimension (sumWaitingTimes, numArriv);
      posWait.add (numPosWait);
   }

   public void printStatistics() {
      System.out.println (triedDial.report ());
      System.out.println (arrivals.report ());
      System.out.println (served.report ());
      System.out.println (abandoned.report ());
      System.out.println (badSL.report ());
      System.out.println (serviceLevel.report ());
      System.out.println (occupancy.report ());
      System.out.println (queueSize.report ());
      System.out.println (totalQueueWait.report ());
      System.out.println (posWait.report ());
   }

   public static void main (String[] args) {
      final Blend blend = new Blend();
      final Chrono timer = new Chrono();
      blend.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      blend.printStatistics();
   }
}
