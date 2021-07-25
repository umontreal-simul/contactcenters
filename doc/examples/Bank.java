import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.MatrixUtil;
import umontreal.iro.lecuyer.contactcenters.NonStationaryMeasureMatrix;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.RepSimCC;
import umontreal.iro.lecuyer.contactcenters.StatUtil;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.ContactSumMatrix;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.TrunkGroup;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.AgentGroupSelectors;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.ContactTimeGenerator;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStatMeasureMatrix;
import umontreal.iro.lecuyer.probdist.UniformDist;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.iro.lecuyer.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.stat.mperiods.IntegralMeasureMatrix;
import umontreal.iro.lecuyer.stat.mperiods.MeasureMatrix;
import umontreal.iro.lecuyer.stat.mperiods.MeasureSet;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

public class Bank {
   // Contact type and agent group identifiers and names
   static final int SAVINGS         = 0;
   static final int CHECKING        = 1;
   static final int BALANCE         = 2;
   static final String[] TYPENAMES  = {
      "savings", "checking", "account balance", "all types"
   };
   static final String[] GROUPNAMES = {
      "savings specialists", "checking specialists", "all groups"
   };
   static final int K       = 3;
   static final int I       = 2;
   static final int P       = 24*7;
   static final String[] GROUPTYPENAMES = new String[K*(I + 1)];
   // PERIODNAMES[p] gives the name for period p
   static final String[] PERIODNAMES    = new String[P + 1];

   static {
      for (int i = 0; i <= I; i++)
         for (int k = 0; k < K; k++) {
            String tn = TYPENAMES[k];
            String gn = GROUPNAMES[i];
            if (tn.equals ("account balance"))
               tn = "balance";
            gn = gn.replaceFirst ("specialists", "spec.");
            GROUPTYPENAMES[i*K + k] = gn + "/" + tn;
         }
      for (int p = 0; p < P; p++)
         PERIODNAMES[p] = "period " + (p + 1);
      PERIODNAMES[P] = "all periods";
   }

   // Input data (all times are in minutes)
   // Cost of agents per minute.
   // The array contains the cost of Savings and Checking specialists,
   // in that order.
   static final double[] COST          = { 12.0/60, 7.5/60 };
   static final double PERIODDURATION = 60.0;
   static final int STARTHOUR          = 8;
   static final int ENDHOUR            = 17;
   static final int NUMTRUNKS          = 15;
   static final double[] FIRSTHOURARRIVALRATES =
   { 4, 40, 100 };
   static final double[] ARRIVALRATES = { 2, 20, 50 };
   static final int[] NUMAGENTS = { 3, 4 };
   // Service time multipliers
   static final double[][] STIMEMULT = {
      // Savings specialists
      { 0.75, 1.0, 1.0 },
      // Checking specialists
      { 1.0, 0.75, 1.0 }
   };

   // Mean service times
   static final double[] STIME         = { 5, 5, 1 };
   static final double[] AWT           = { 1.0, 90.0/60.0, 30.0/60.0 };
   static final int[][] TYPETOGROUPMAP = {
      // Savings contact type
      { SAVINGS, CHECKING },
      // Checking contact type
      { CHECKING, SAVINGS },
      // Account balance contact type
      { SAVINGS, CHECKING }
   };
   static final int[][] GROUPTOTYPEMAP = {
      // Savings specialists
      { SAVINGS, CHECKING, BALANCE },
      // Checking specialists
      { CHECKING, SAVINGS, BALANCE }
   };
   static final double LEVEL           = 0.95;  // Level of conf. int.
   static final int NUMDAYS            = 1000;

   // Contact center components
   PeriodChangeEvent pce;  // Event marking beginning of periods
   TrunkGroup trunks;      // Manages phone lines
   PiecewiseConstantPoissonArrivalProcess[] arrivProc =
      new PiecewiseConstantPoissonArrivalProcess[K];
   AgentGroup[] agentGroups = new AgentGroup[I];
   WaitingQueue[] queues = new WaitingQueue[K];
   Router router;

   // Random streams and random variate generators
   RandomStream agentStream = new MRG32k3a();
   UniformGen[] pgen = {
      // Savings
      new UniformGen (new MRG32k3a(), new UniformDist (3.0, 5.0)),
      // Checking
      new UniformGen (new MRG32k3a(), new UniformDist (3.0, 5.0)),
      // Account balance
      new UniformGen (new MRG32k3a(), new UniformDist (2.5, 3.5))
   };
   ExponentialGen sgen[] = {
      // Savings
      new ExponentialGen (new MRG32k3a(), 1.0/STIME[SAVINGS]),
      // Checking
      new ExponentialGen (new MRG32k3a(), 1.0/STIME[CHECKING]),
      new ExponentialGen (new MRG32k3a(), 1.0/STIME[BALANCE])
   };

   // Measure matrices (counters)
   List<MeasureMatrix> mats = new ArrayList<MeasureMatrix>();
   ContactSumMatrix numArriv;
   ContactSumMatrix numBlocked;
   ContactSumMatrix numGoodSL;
   ContactSumMatrix numServed;
   ContactSumMatrix numAbandoned;
   ContactSumMatrix numDisconnected;
   ContactSumMatrix sumServiceTimes;
   ContactSumMatrix sumWaitingTimes;
   IntegralMeasureMatrix<?>[] vstat = new IntegralMeasureMatrix[I];
   MeasureSet svm;
   MeasureSet ivm;
   MeasureSet tvm;
   MeasureSet dsvm;
   MeasureSet dtvm;

   // Statistical collectors
   List<MatrixOfTallies<Tally>> mTallies = new ArrayList<MatrixOfTallies<Tally>>();
   MatrixOfTallies<Tally> arrivals;
   MatrixOfTallies<Tally> blocked;
   MatrixOfTallies<Tally> served;
   MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> serviceLevel;
   MatrixOfTallies<Tally> occupancy;
   MatrixOfTallies<Tally> occupancyPerType;
   MatrixOfTallies<Tally> serviceTimes;
   MatrixOfTallies<Tally> idleCost;
   MatrixOfTallies<Tally> busyCost;
   MatrixOfTallies<Tally> waitingTimes;
   MatrixOfTallies<Tally> disconnected;
   MatrixOfTallies<Tally> abandoned;
   Tally trunkAvail;
   Tally trunkBusy;

   Bank() {
      pce = new PeriodChangeEvent (PERIODDURATION, P + 2, 0.0);
      trunks = new TrunkGroup (NUMTRUNKS);
      trunks.setStatCollecting (true);

      final double[][] arrivalRates = new double[K][P + 2];
      for (int day = 0; day < 5; day++) {
         for (int k = 0; k < K; k++)
            arrivalRates[k][24*day + STARTHOUR + 1] =
               FIRSTHOURARRIVALRATES[k];
         for (int hour = STARTHOUR + 1; hour < ENDHOUR; hour++)
            for (int k = 0; k < K; k++)
               arrivalRates[k][24*day + hour + 1] = ARRIVALRATES[k];
      }
      for (int k = 0; k < K; k++) {
         arrivProc[k] = new PiecewiseConstantPoissonArrivalProcess
            (pce, new MyContactFactory (k), arrivalRates[k], new MRG32k3a());
         arrivProc[k].setNormalizing (true);
      }

      final int[][] numAgents = new int[I][P + 2];
      for (int day = 0; day < 5; day++)
         for (int hour = STARTHOUR; hour < ENDHOUR; hour++)
            for (int i = 0; i < I; i++)
               numAgents[i][24*day + hour + 1] = NUMAGENTS[i];
      for (int i = 0; i < I; i++) {
         agentGroups[i] = new AgentGroup (pce, numAgents[i]);
         agentGroups[i].setContactTimeGenerator
            (0, new ContactTimeGenerator (agentGroups[i], STIMEMULT[i]));
      }

      for (int q = 0; q < K; q++)
         queues[q] = new StandardWaitingQueue();

      router = new MyRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
      for (int k = 0; k < K; k++) arrivProc[k].addNewContactListener (router);
      for (int q = 0; q < K; q++) router.setWaitingQueue (q, queues[q]);
      for (int i = 0; i < I; i++) router.setAgentGroup (i, agentGroups[i]);
      router.setClearWaitingQueues (true);
      router.addExitedContactListener (new MyContactMeasures());

      mats.add (numArriv        = new ContactSumMatrix (K, P + 2));
      mats.add (numBlocked      = new ContactSumMatrix (K, P + 2));
      mats.add (numServed       = new ContactSumMatrix (K, P + 2));
      mats.add (numGoodSL       = new ContactSumMatrix (K, P + 2));
      mats.add (numAbandoned    = new ContactSumMatrix (K, P + 2));
      mats.add (numDisconnected = new ContactSumMatrix (K, P + 2));
      mats.add (sumWaitingTimes = new ContactSumMatrix (K, P + 2));
      mats.add (sumServiceTimes = new ContactSumMatrix  (K, P + 2));
      for (int i = 0; i < I; i++)
         mats.add (vstat[i] = new NonStationaryMeasureMatrix<GroupVolumeStatMeasureMatrix>
                    (pce, new GroupVolumeStatMeasureMatrix (agentGroups[i], K)));
      svm = GroupVolumeStatMeasureMatrix.getServiceVolumeMeasureSet (vstat);
      ivm = GroupVolumeStatMeasureMatrix.getIdleVolumeMeasureSet (vstat);
      tvm = GroupVolumeStatMeasureMatrix.getTotalVolumeMeasureSet (vstat);
      dsvm = GroupVolumeStatMeasureMatrix.getServiceVolumeMeasureSet (vstat, K);
      dtvm = GroupVolumeStatMeasureMatrix.getTotalVolumeMeasureSet (vstat, K);

      mTallies.add (arrivals = create
                     ("Number of arrived contacts",
                      TYPENAMES, PERIODNAMES));
      mTallies.add (blocked = create
                     ("Number of blocked contacts",
                      TYPENAMES, PERIODNAMES));
      mTallies.add (served = create
                     ("Number of served contacts",
                      TYPENAMES, PERIODNAMES));
      serviceLevel = createRatio
                     ("Service level",
                      TYPENAMES, PERIODNAMES);
      mTallies.add (occupancy = create
                     ("Agents' occupancy ratio",
                      GROUPNAMES, PERIODNAMES));
      mTallies.add (occupancyPerType = create
                     ("Occupancy ratio per contact type",
                      GROUPTYPENAMES, PERIODNAMES));
      mTallies.add (serviceTimes = create
                     ("Service time",
                      TYPENAMES, PERIODNAMES));
      mTallies.add (busyCost = create
                     ("Busy cost of agents",
                      GROUPNAMES, PERIODNAMES));
      mTallies.add (idleCost = create
                     ("Idle cost of agents",
                      GROUPNAMES, PERIODNAMES));
      trunkAvail = new Tally
                     ("Number of available trunks");
      trunkBusy = new Tally
                     ("Number of busy trunks");
      mTallies.add (waitingTimes = create
                     ("Average waiting time",
                      TYPENAMES, PERIODNAMES));
      mTallies.add (abandoned = create
                     ("Number of abandoned contacts",
                      TYPENAMES, PERIODNAMES));
      mTallies.add (disconnected = create
                     ("Number of disconnected contacts",
                      TYPENAMES, PERIODNAMES));
   }

   private MatrixOfTallies<Tally> create
   (String name, String[] rowNames, String[] columnNames) {
      final MatrixOfTallies<Tally> mta = MatrixOfTallies.createWithTally
      (rowNames.length, columnNames.length);
      mta.setName (name);
      for (int r = 0; r < rowNames.length; r++)
         for (int c = 0; c < columnNames.length; c++)
            mta.get (r, c).setName (rowNames[r] + ", " + columnNames[c]);
      for (final Tally ta : mta) {
         ta.setConfidenceIntervalStudent();
         ta.setConfidenceLevel (LEVEL);
      }
      return mta;
   }

   private MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally>
   createRatio (String name, String[] rowNames, String[] columnNames) {
      final RatioFunction ratio = new RatioFunction();
      final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta =
         MatrixOfFunctionOfMultipleMeansTallies.create
         (ratio, 2, rowNames.length, columnNames.length);
      mta.setName (name);
      for (int r = 0; r < rowNames.length; r++)
         for (int c = 0; c < columnNames.length; c++)
            mta.get (r, c).setName (rowNames[r] + ", " + columnNames[c]);
      for (final FunctionOfMultipleMeansTally ta : mta) {
         ta.setConfidenceIntervalDelta();
         ta.setConfidenceLevel (LEVEL);
      }
      return mta;
   }

   // Creates new contacts
   class MyContactFactory implements ContactFactory {
      int type;
      MyContactFactory (int type) { this.type = type; }

      public Contact newInstance() {
         final Contact contact = new Contact (type);
         contact.setTrunkGroup (trunks);
         contact.setDefaultServiceTime (sgen[type].nextDouble());
         contact.setDefaultPatienceTime (pgen[type].nextDouble());
         return contact;
      }
   }

   class MyRouter extends SingleFIFOQueueRouter {
      MyRouter (int[][] typeToGroupMap, int[][] groupToTypeMap) {
         super (typeToGroupMap, groupToTypeMap);
      }

      @Override
      protected EndServiceEvent selectAgent (Contact contact) {
         final int tid = contact.getTypeId();
         if (tid == BALANCE) {
            final AgentGroup group = AgentGroupSelectors.selectUniform
               (this, typeToGroupMap[tid], agentStream);
            if (group == null)    return null;
            return group.serve (contact);
         }
         else
            return super.selectAgent (contact);
      }

      @Override
      protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
         final int gid = group.getId();
         final int bestType = groupToTypeMap[gid][0];
         if (queues[bestType].isEmpty ())
            return super.selectContact (group, agent);
         else
            return queues[bestType].removeFirst (DEQUEUETYPE_BEGINSERVICE);
      }
   }

   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         final int type = contact.getTypeId();
         final int period = pce.getPeriod (contact.getArrivalTime());
         numArriv.add (type, period, 1);
         numBlocked.add (type, period, 1);
      }

      public void dequeued (Router router, DequeueEvent ev) {
         final Contact contact = ev.getContact();
         final int type = contact.getTypeId();
         final int period = pce.getPeriod (contact.getArrivalTime());
         numArriv.add (type, period, 1);
         if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_NOAGENT)
            numDisconnected.add (type, period, 1);
         else
            numAbandoned.add (type, period, 1);
      }

      public void served (Router router, EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         final int type = contact.getTypeId();
         final int period = pce.getPeriod (contact.getArrivalTime());
         numArriv.add (type, period, 1);
         numServed.add (type, period, 1);
         final double qt = contact.getTotalQueueTime();
         if (qt < AWT[type]) numGoodSL.add (type, period, 1);
         final double st = contact.getTotalServiceTime();
         sumServiceTimes.add (type, period, st);
         sumWaitingTimes.add (type, period, contact.getTotalQueueTime());
      }
   }

   public void simulateOneDay() {
      Sim.init();
      pce.init();
      trunks.init();
      ContactCenter.initElements (arrivProc);
      ContactCenter.initElements (agentGroups);
      ContactCenter.initElements (queues);
      ContactCenter.initElements (mats);
      ContactCenter.toggleElements (arrivProc, true);
      pce.start();
      Sim.start();
      pce.stop();
      addObs();
   }

   void addObs() {
      final DoubleMatrix2D arrivalsM = RepSimCC.getReplicationValues (numArriv, false, false);
      arrivals.add (arrivalsM);
      final DoubleMatrix2D bl = RepSimCC.getReplicationValues (numBlocked, false, false);
      blocked.add (bl);
      final DoubleMatrix2D servedM = RepSimCC.getReplicationValues
         (numServed, false, false);
      served.add (servedM);
      final DoubleMatrix2D gslM = RepSimCC.getReplicationValues
         (numGoodSL, false, false);
      gslM.assign (Functions.mult (100));
      //addRatio (serviceLevel, gslM, arrivalsM);
      serviceLevel.addSameDimension (gslM, arrivalsM);
      final DoubleMatrix2D svM = RepSimCC.getReplicationValues
         (svm, false, false);
      final DoubleMatrix2D ivM = RepSimCC.getReplicationValues
         (ivm, false, false);
      final DoubleMatrix2D tvM = RepSimCC.getReplicationValues
         (tvm, false, false);
      StatUtil.addRatio (occupancy, svM, tvM, 100.0);
      final DoubleMatrix2D dsvM = RepSimCC.getReplicationValues
         (dsvm, false, false);
      final DoubleMatrix2D dtvM = RepSimCC.getReplicationValues
         (dtvm, false, false);
      StatUtil.addRatio (occupancyPerType, dsvM, dtvM, 100.0);

      MatrixUtil.getCost (svM, COST);
      MatrixUtil.getCost (ivM, COST);
      busyCost.add (svM);
      idleCost.add (ivM);
      final DoubleMatrix2D st = RepSimCC.getReplicationValues (sumServiceTimes, false, false);
      StatUtil.addRatio (serviceTimes, st, servedM);
      final DoubleMatrix2D wt = RepSimCC.getReplicationValues (sumWaitingTimes, false, false);
      StatUtil.addRatio (waitingTimes, wt, arrivalsM);
      final DoubleMatrix2D ab = RepSimCC.getReplicationValues (numAbandoned, false, false);
      abandoned.add (ab);
      final DoubleMatrix2D dis = RepSimCC.getReplicationValues (numDisconnected, false, false);
      disconnected.add (dis);
      trunkBusy.add (trunks.getStatLines().average());
      trunkAvail.add (trunks.getCapacity() - trunks.getStatLines().average());
   }

   public void simulate (int n) {
      ContactCenter.initElements (mTallies);
      serviceLevel.init();
      trunkAvail.init();
      trunkBusy.init();
      for (int r = 0; r < n; r++) simulateOneDay();
   }

   public void printStatistics() {
      System.out.println (trunkAvail.reportAndCIStudent (LEVEL, 3));
      System.out.println (trunkBusy.reportAndCIStudent (LEVEL, 3));

      System.out.println
         (arrivals.columnReport
          (arrivals.columns() - 1));
      System.out.println
         (blocked.columnReport
          (blocked.columns() - 1));
      System.out.println
         (serviceLevel.columnReport
          (serviceLevel.columns() - 1));
      System.out.println
         (serviceTimes.columnReport
          (serviceTimes.columns() - 1));
      System.out.println
         (waitingTimes.columnReport
          (waitingTimes.columns() - 1));
      System.out.println
         (abandoned.columnReport
          (abandoned.columns() - 1));
      System.out.println
         (disconnected.columnReport
          (disconnected.columns() - 1));
      System.out.println
         (occupancy.columnReport
          (occupancy.columns() - 1));
      System.out.println
         (idleCost.columnReport
          (idleCost.columns() - 1));
      System.out.println
         (busyCost.columnReport
          (busyCost.columns() - 1));
      System.out.println
         (occupancyPerType.columnReport
          (occupancyPerType.columns() - 1));
   }

   public static void main (String[] args) {
      final Bank b = new Bank();
      final Chrono timer = new Chrono();
      b.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      b.printStatistics();
   }
}
