import java.util.List;
import umontreal.iro.lecuyer.util.*;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.probdist.*;
import umontreal.iro.lecuyer.randvar.*;
import umontreal.iro.lecuyer.stat.*;
import umontreal.iro.lecuyer.stat.mperiods.*;
import umontreal.iro.lecuyer.stat.matrix.*;
import umontreal.iro.lecuyer.contactcenters.*;
import umontreal.iro.lecuyer.contactcenters.contact.*;
import umontreal.iro.lecuyer.contactcenters.server.*;
import umontreal.iro.lecuyer.contactcenters.queue.*;
import umontreal.iro.lecuyer.contactcenters.router.*;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.simevents.BatchMeansSim;
import cern.colt.matrix.DoubleMatrix2D;

public class Simple {
   static final double ARRIVALRATE = 0.9;
   static final double SERVICERATE = 0.2;
   static final double AWT         = 20/60.0;
   static final int NUMAGENTS      = 12;
   static final int NBATCHES       = 30;
   static final double WARMUPTIME  = 10000.0;
   static final double BATCHSIZE   = 80000.0;
   static final double LEVEL       = 0.95;

   BatchMeansSim sim       = new Simulation();
   RandomStreamManager rsm = new RandomStreamManager();
   ContactArrivalProcess ap;
   AgentGroup agents;
   WaitingQueue queue;
   Router router;

   ExponentialGen sgen = new ExponentialGen
      (rsm.add (new MRG32k3a()), new ExponentialDist (SERVICERATE));

   ContactSumMatrix numArriv;
   ContactSumMatrix numGoodSL;
   ContactSumMatrix numServed;
   IntegralMeasureMatrix vcalc;
   MeasureSet svm;
   MeasureSet tvm;

   MatrixOfTallies arrivals;
   MatrixOfTallies served;
   MatrixOfRatioTallies serviceLevel;
   MatrixOfRatioTallies occupancy;

   public static void main (String[] args) {
      Simple s = new Simple();
      Chrono timer = new Chrono();
      s.simulate();
      System.out.println ("CPU time: " + timer.format());
      s.printStatistics();
   }

   Simple() {
      ap = new PoissonArrivalProcess
         (new MyContactFactory(), ARRIVALRATE,
          rsm.add (new MRG32k3a()));

      agents = new AgentGroup (NUMAGENTS);
      queue = new StandardWaitingQueue();

      int[][] typeToGroupMap = {
         { 0 }
      };
      int[][] groupToTypeMap = {
         { 0 }
      };
      router = new QueuePriorityRouter
         (typeToGroupMap, groupToTypeMap);
      router.setAgentGroup (0, agents);
      router.setWaitingQueue (0, queue);
      router.addExitedContactListener (new MyContactMeasures());
      ap.addNewContactListener (router);

      List mats = sim.getMeasureMatrices();
      mats.add (numArriv   = new ContactSumMatrix (1, 1));
      mats.add (numServed  = new ContactSumMatrix (1, 1));
      mats.add (numGoodSL  = new ContactSumMatrix (1, 1));
      mats.add (vcalc      = new IntegralMeasureMatrix
                 (new AgentGroupVolumeMatrix (agents), 1));

      svm = AgentGroupVolumeMatrix.getServiceVolumeMeasureSet
         (new IntegralMeasureMatrix[] { vcalc });
      tvm = AgentGroupVolumeMatrix.getTotalVolumeMeasureSet
         (new IntegralMeasureMatrix[] { vcalc });

      List batchProbes = sim.getSimProbes();
      batchProbes.add (arrivals  = new MatrixOfTallies
                       ("Number of arrivals",
                        numArriv.getNumMeasures(), 1));
      batchProbes.add (served    = new MatrixOfTallies
                       ("Number of served calls",
                        numServed.getNumMeasures(), 1));
      batchProbes.add (serviceLevel       = new MatrixOfRatioTallies
                       ("Service level",
                        numGoodSL.getNumMeasures(), 1));
      batchProbes.add (occupancy = new MatrixOfRatioTallies
                       ("Occupancy rate",
                        svm.getNumMeasures(), 1));
   }

   public void printStatistics() {
      System.out.println
         (arrivals.getTally (arrivals.rows() - 1, arrivals.columns() - 1).
          reportAndConfidenceIntervalStudent (LEVEL));
      System.out.println
         (served.getTally (served.rows() - 1, served.columns() - 1).
          reportAndConfidenceIntervalStudent (LEVEL));
      System.out.println
         (serviceLevel.getRatioTally (serviceLevel.rows() - 1, serviceLevel.columns() - 1).
          reportAndConfidenceIntervalDelta (LEVEL));
      System.out.println
         (occupancy.getRatioTally (occupancy.rows() - 1, occupancy.columns() - 1).
          reportAndConfidenceIntervalDelta (LEVEL));
   }

   class MyContactFactory implements ContactFactory {
      public Contact newInstance() {
         Contact contact = new Contact();
         contact.setDefaultServiceTime
            (sgen.nextDouble());
         return contact;
      }
   }

   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, WaitingQueue.DequeueEvent ev) {}

      public void served (Router router, AgentGroup.EndServiceEvent ev) {
         if (!sim.isWarmupDone())
            return;
         Contact contact = ev.getContact();
         int batch = sim.getCompletedBatches();
         numArriv.add (0, batch, 1);
         numServed.add (0, batch, 1);
         double qt = contact.getTotalQueueTime();
         if (qt < AWT)
            numGoodSL.add (0, batch, 1);
      }
   }

   void simulate() {
      sim.simulate();
   }

   class Simulation extends BatchMeansSim {
      Simulation() {
         super (NBATCHES, BATCHSIZE, WARMUPTIME);
      }

      protected void initSimulation() {
         ap.init(); agents.init(); queue.init();
         ap.setEnabled (true);
      }

      protected void initBatchObs() {}
      protected void addBatchObs() {
         DoubleMatrix2D arrivalsM = getBatchValues2D (numArriv);
         arrivals.add (arrivalsM);
         DoubleMatrix2D servedM = getBatchValues2D (numServed);
         served.add (servedM);
         DoubleMatrix2D gslM = getBatchValues2D (numGoodSL);
         serviceLevel.add (gslM, arrivalsM, 100.0);
         DoubleMatrix2D svM = getBatchValues2D (svm);
         DoubleMatrix2D tvM = getBatchValues2D (tvm);
         occupancy.add (svM, tvM, 100.0);
      }
   }
}
