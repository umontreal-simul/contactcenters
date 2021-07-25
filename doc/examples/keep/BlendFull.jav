import java.util.*;
import umontreal.iro.lecuyer.util.*;
import umontreal.iro.lecuyer.probdist.*;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.randvar.*;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.simevents.SimUtils;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.stat.*;
import umontreal.iro.lecuyer.stat.array.*;
import umontreal.iro.lecuyer.contactcenters.*;
import umontreal.iro.lecuyer.contactcenters.queue.*;
import umontreal.iro.lecuyer.contactcenters.contact.*;
import umontreal.iro.lecuyer.contactcenters.server.*;
import umontreal.iro.lecuyer.contactcenters.dialer.*;
import umontreal.iro.lecuyer.contactcenters.router.*;

public class BlendFull extends ContactCenter {
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

   static final int K = 2;
   static final int I = 2;
   static final int P = 25;
   static final double PERIODDURATION = 30;
   static final double STARTDAY       = 8*60;
   static final double STARTDIALER    = 14*60;
   static final double[] ARRIVALRATES = {
      63.0, 63.0, 91.8, 119.0, 135.6, 146.2, 145.2, 142.2, 140.2, 134.6, 135.4,
      141.4, 135.8, 132.6, 133.2, 131.4, 142.4, 140.6, 130.0, 98.6,
      95.8, 77.6, 67.8, 62.0, 55.2, 39.2, 0
   };
   static final double ALPHA0 = 29.7;
   static final double[] STALPHAS = {
      0.729, 0.729, 0.729, 0.729, 0.729,
      0.729, 0.729, 0.729, 0.729, 0.62,
      0.62, 0.62, 0.62, 0.62, 0.755,
      0.755, 0.755, 0.755, 0.553,
      0.553, 0.553, 0.553, 0.518,
      0.518, 0.518, 0.518,  0.518
   };
   static final double[] STLAMBDAS = {
      4.406, 4.406, 4.406, 4.406, 4.406,
      4.406, 4.406, 4.406, 4.406, 3.88,
      3.88, 3.88, 3.88, 3.88, 4.776,
      4.776, 4.776, 4.776, 3.611,
      3.611, 3.611, 3.611, 3.667,
      3.667, 3.667, 3.667,  3.667
   };
   static final double EXPOUTSERVICERATE = 8.178/60.0;
   static final double PROBBALK          = 0.0050;
   static final double[] NU              = {
      9.0, 9.0, 9.0, 9.0, 5.14, 5.14, 6.0, 6.0, 6.0, 6.0, 6.0, 7.2, 7.2, 7.2, 
      7.2, 7.2, 7.2, 7.2, 7.2, 7.2, 7.2, 7.2, 7.2, 7.2, 36.0, 72.0, 72.0
   };
   static final double OUTBOUNDPATIENCETIME = 5.0/60.0;
   static final double[] PROBREACH = {
      0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
      0.27, 0.27, 0.28, 0.29, 0.29, 0.3, 0.33, 0.37, 0.4, 0.38,
      0.41, 0.41, 0.41, 0.41
   };
   static final int[] INBOUNDAGENTS = {
      0, 12, 18, 22, 25, 27, 26, 26, 24, 22, 23, 28, 25, 25, 
      23, 23, 23, 21, 17, 15, 10, 4, 3, 3, 3, 3, 3
   };
   static final int[] BLENDAGENTS = {
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 11, 16, 18, 16, 14, 11,
      16, 16, 16, 16, 17, 16, 16
   };
   static final double INBOUNDEFFICIENCY = 0.9;
   static final double BLENDEFFICIENCY   = 0.85;
   static final int QUEUECAPACITY        = 80;
   static final double AWT               = 20.0/60.0;
   static final int MINFREEAGENTS = 4;
   static final double KAPPA = 2;
   static final int C = 0;
   static {
      for (int i = 0; i < ARRIVALRATES.length; i++)
         ARRIVALRATES[i] /= 60.0;
      for (int i = 0; i < STLAMBDAS.length; i++)
         STLAMBDAS[i] /= 60.0;
      for (int i = 0; i < NU.length; i++)
         NU[i] /= 60.0;
   }
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
   static final int NUMDAYS           = 1000;
   static final double LEVEL          = 0.95;

   // Contact center components
   PeriodChangeEvent pce;  // Event marking beginning of periods
   Dialer dialer;
   QueuePriorityRouter router;
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
   RandomVariateGen sigen;
   // Service times for outbound calls
   RandomVariateGen sogen = new ExponentialGen
         (new MRG32k3a(), new ExponentialDist (EXPOUTSERVICERATE));
   // Patience times for inbound calls
   RandomVariateGen pigen;

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
   ArrayOfTallies arrivals = new ArrayOfTallies
      ("Number of arrived contacts", TYPENAMES);
   ArrayOfTallies served = new ArrayOfTallies
      ("Number of served contacts", TYPENAMES);
   ArrayOfTallies abandoned = new ArrayOfTallies
      ("Number of abandoned contacts", TYPENAMES);
   Tally badSL = new Tally ("Number of contacts not in target");
   RatioTally serviceLevel = new RatioTally ("Service level");
   ArrayOfRatioTallies occupancy = new ArrayOfRatioTallies
      ("Agents' occupancy ratio", GROUPNAMES);
   ArrayOfTallies queueSize = new ArrayOfTallies
      ("Time-average queue size", TYPENAMES);
   ArrayOfRatioTallies totalQueueWait = new ArrayOfRatioTallies
      ("Average waiting time", TYPENAMES);
   ArrayOfTallies posWait = new ArrayOfTallies
      ("Number of contacts having to wait", TYPENAMES);

   BlendFull() {
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, STARTDAY);
      pce.addPeriodChangeListener (new MyPeriodChangeListener());
      getPeriodChangeEvents().add (pce);
      inboundAgents = new AgentGroup (pce, INBOUNDAGENTS);
      inboundAgents.setEfficiency (INBOUNDEFFICIENCY);
      inboundVolume = new GroupVolumeStat (inboundAgents);
      getAgentGroups().add (inboundAgents);
      blendAgents = new AgentGroup (pce, BLENDAGENTS);
      blendAgents.setEfficiency (BLENDEFFICIENCY);
      blendVolume = new GroupVolumeStat (blendAgents);
      getAgentGroups().add (blendAgents);

      inQueue = new StandardWaitingQueue();
      inQueueSize = new QueueSizeStat (inQueue);
      getWaitingQueues().add (inQueue);
      outQueue = new StandardWaitingQueue();
      outQueueSize = new QueueSizeStat (outQueue);
      getWaitingQueues().add (outQueue);

      AgentGroupSet testSet = new AgentGroupSet();
      testSet.addAgentGroup (inboundAgents);
      testSet.addAgentGroup (blendAgents);
      AgentGroupSet targetSet = new AgentGroupSet();
      targetSet.addAgentGroup (blendAgents);
      DialerPolicy pol = new ThresholdDialerPolicy
         (new InfiniteDialerList (new MyContactFactory (OUTBOUND)),
          testSet, targetSet, MINFREEAGENTS, KAPPA, C);

      ConstantValueGenerator pReachGen = new ConstantValueGenerator
         (pce, K, PROBREACH);
      dialer = new Dialer (pol, new MRG32k3a(), pReachGen);
      ContactCounter ntd = new ContactCounter();
      dialer.addReachListener (ntd);
      dialer.addFailListener (ntd);
      getDialers().add (dialer);

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

      sigen = MultiPeriodGen.createGamma (pce, new MRG32k3a(), STALPHAS, STLAMBDAS);
      pigen = MultiPeriodGen.createExponential (pce, new MRG32k3a(), NU);
      inboundAgents.setContactTimeGenerator (0, (MultiPeriodGen)sigen);
      blendAgents.setContactTimeGenerator
         (0, new ValueGenerator() {
               public double nextDouble (Contact contact) {
                  if (contact.getTypeId() == INBOUND)
                     return sigen.nextDouble();
                  else
                     return contact.getDefaultContactTime();
               }
               public void init() {}
            });
   }

   // Creates contacts
   class MyContactFactory implements ContactFactory {
      int type;
      public MyContactFactory (int type) { this.type = type; }

      public Contact newInstance() {
         Contact contact = new Contact (type);
         switch (type) {
         case INBOUND:
            // Inbound contact
            //contact.setDefaultContactTime (sigen.nextDouble());
            double u = streamBalk.nextDouble();
            if (u < PROBBALK) contact.setDefaultPatienceTime (0);
            else contact.setDefaultPatienceTime (pigen.nextDouble());
            break;
         case OUTBOUND:
            // Outbound contact
            contact.setDefaultContactTime (sogen.nextDouble());
            contact.setDefaultPatienceTime (OUTBOUNDPATIENCETIME);
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
         int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numAbandoned[type];
         ++numAbandoned[K];
         ++numPosWait[type];
         ++numPosWait[K];
      }

      public void dequeued (Router router, WaitingQueue.DequeueEvent ev) {
         Contact contact = ev.getContact();
         if (contact.getArrivalTime() < STARTDAY)
            return;
         int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numAbandoned[type];
         ++numAbandoned[K];
         double qt = contact.getTotalQueueTime();
         sumWaitingTimes[type] += qt;
         sumWaitingTimes[K] += qt;
         ++numPosWait[type];
         ++numPosWait[K];
         if (qt >= AWT && type == INBOUND) ++numBadSL;
      }

      public void served (Router router, AgentGroup.EndServiceEvent ev) {
         Contact contact = ev.getContact();
         if (contact.getArrivalTime() < STARTDAY)
            return;
         int type = contact.getTypeId();
         ++numArriv[type];
         ++numArriv[K];
         ++numServed[type];
         ++numServed[K];
         double qt = contact.getTotalQueueTime();
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
      init();
      arrivProc.init (bgen.nextDouble());
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
      inboundVolume.init();
      blendVolume.init();
      new ToggleEvent (dialer, true).schedule (STARTDIALER);
      double endDay = STARTDAY + P*PERIODDURATION;
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
      occupancy.add (Nb, N, 100);
      queueSize.add (qs);
      totalQueueWait.add (sumWaitingTimes, numArriv);
      posWait.add (numPosWait);
   }

   public void printStatistics() {
      System.out.println (triedDial.reportAndCIStudent (LEVEL, 3));
      System.out.println (arrivals.reportAndCIStudent (LEVEL, 3));
      System.out.println (served.reportAndCIStudent (LEVEL, 3));
      System.out.println (abandoned.reportAndCIStudent (LEVEL, 3));
      System.out.println (badSL.reportAndCIStudent (LEVEL, 3));
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
      System.out.println (occupancy.reportAndCIDelta (LEVEL, 3));
      System.out.println (queueSize.reportAndCIStudent (LEVEL, 3));
      System.out.println (totalQueueWait.reportAndCIDelta (LEVEL, 3));
      System.out.println (posWait.reportAndCIStudent (LEVEL, 3));
   }

   public static void main (String[] args) {
      BlendFull blend = new BlendFull();
      Chrono timer = new Chrono();
      blend.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      blend.printStatistics();
   }
}
