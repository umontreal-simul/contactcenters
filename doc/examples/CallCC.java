import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

import umontreal.iro.lecuyer.probdist.ExponentialDist;
import umontreal.iro.lecuyer.probdist.GammaDist;
import umontreal.iro.lecuyer.randvar.GammaAcceptanceRejectionGen;
import umontreal.iro.lecuyer.randvar.GammaGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.Tally;

public class CallCC {
   static final double HOUR = 3600.0;  // Time is in seconds.

   // Data
   // Arrival rates are per hour, service and patience times are in seconds.
   int numDays;           // Number of days to simulate.
   double openingTime;    // Opening time of the center (in hours).
   int numPeriods;        // Number of working periods (hours) in the day.
   int[] numAgents;       // Number of agents for each period.
   double[] lambda;       // Base arrival rate lambda_j for each j.
   double alpha0;         // Parameter of gamma distribution for W.
   double p;              // Probability that patience time is 0.
   double nu;             // Parameter of exponential for patience time.
   double alpha, beta;    // Parameters of gamma service time distribution.
   double s;              // Want stats on waiting times smaller than s.

   // Variables
   int nArrivals;         // Number of arrivals today;
   int nAbandon;          // Number of abandonments during the day.
   int nGoodSL;           // Number of waiting times less than s today.
   double nCallsExpected; // Expected number of calls per day.

   PeriodChangeEvent pce;   // Event marking new periods
   PiecewiseConstantPoissonArrivalProcess arrivProc;
   WaitingQueue waitingQueue = new StandardWaitingQueue();
   AgentGroup agentGroup;
   Router router;

   RandomStream streamB        = new MRG32k3a(); // For B.
   RandomStream streamArr      = new MRG32k3a(); // For arrivals.
   RandomStream streamPatience = new MRG32k3a(); // For patience times.
   RandomStream streamS        = new MRG32k3a(); // For service times.
   GammaGen genServ;   // For service times; created in readData().
   GammaGen bgen;      // For busyness; created in readData().

   Tally statArrivals = new Tally ("Number of arrivals per day");
   Tally statWaits    = new Tally ("Average waiting time per customer");
   Tally statWaitsDay = new Tally ("Waiting times within a day");
   Tally statGoodSL   = new Tally ("Proportion of waiting times < s");
   Tally statAbandon  = new Tally ("Proportion of calls lost");

   public CallCC (String fileName) throws IOException {
      readData (fileName);
      pce = new PeriodChangeEvent (HOUR, numPeriods + 2, openingTime*HOUR);
      arrivProc = new PiecewiseConstantPoissonArrivalProcess
         (pce, new MyContactFactory(), lambda, streamArr);
      arrivProc.setNormalizing (true);
      agentGroup = new AgentGroup (pce, numAgents);
      router = new QueuePriorityRouter (new int[][] { { 0 } }, new int[][] { { 0 }});
      arrivProc.addNewContactListener (router);
      router.setWaitingQueue (0, waitingQueue);
      router.setAgentGroup (0, agentGroup);
      router.addExitedContactListener (new MyContactMeasures());
   }

   // Creates new contacts
   class MyContactFactory implements ContactFactory {
      public Contact newInstance() {
         final Contact contact = new Contact();
         contact.setDefaultServiceTime (genServ.nextDouble());
         contact.setDefaultPatienceTime (generPatience());
         return contact;
      }
   }

   // Updates counters
   class MyContactMeasures implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, DequeueEvent ev) {
         nArrivals++; nAbandon++;
         final double qt = ev.getContact().getTotalQueueTime();
         if (qt < s) nGoodSL++;
         statWaitsDay.add (qt);
      }
      public void served (Router router, EndServiceEvent ev) {
         nArrivals++;
         final double qt = ev.getContact().getTotalQueueTime();
         if (qt < s) nGoodSL++;
         statWaitsDay.add (qt);
      }
   }

   public double generPatience() {
      if (agentGroup.getNumFreeAgents() > 0) return 0;
      // Generates the patience time for a call.
      final double u = streamPatience.nextDouble();
      if (u <= p)  return 0.0;
      else         return ExponentialDist.inverseF (nu, (1.0-u) / (1.0-p));
   }

   void readData (String fileName) throws IOException {
      // Reads data and construct arrays.
      final BufferedReader input = new BufferedReader (new FileReader (fileName));
      StringTokenizer line = new StringTokenizer (input.readLine());
      openingTime = Double.parseDouble (line.nextToken());
      line = new StringTokenizer (input.readLine());
      numPeriods  = Integer.parseInt (line.nextToken());

      numAgents = new int[numPeriods+2];
      lambda = new double[numPeriods+2];
      nCallsExpected = 0.0;
      for (int j=0; j < numPeriods; j++) {
         line = new StringTokenizer (input.readLine());
         numAgents[j+1] = Integer.parseInt (line.nextToken());
         lambda[j+1]    = Double.parseDouble (line.nextToken());
         nCallsExpected += lambda[j+1];
      }
      numAgents[numAgents.length - 1] = numAgents[numAgents.length - 2];
      line = new StringTokenizer (input.readLine());
      alpha0 = Double.parseDouble (line.nextToken());
      line = new StringTokenizer (input.readLine());
      p = Double.parseDouble (line.nextToken());
      line = new StringTokenizer (input.readLine());
      nu = Double.parseDouble (line.nextToken());
      line = new StringTokenizer (input.readLine());
      alpha = Double.parseDouble (line.nextToken());
      line = new StringTokenizer (input.readLine());
      beta = Double.parseDouble (line.nextToken());
      // genServ can be created only after its parameters are known.
      genServ = new GammaAcceptanceRejectionGen (streamS, alpha, beta);
                                 // Faster than inversion.
      bgen = new GammaGen (streamB, new GammaDist (alpha0, alpha0, 8));
      line = new StringTokenizer (input.readLine());
      s = Double.parseDouble (line.nextToken());
      input.close();
   }

   void simulateOneDay() {
      Sim.init();
      statWaitsDay.init();
      nArrivals = nAbandon = nGoodSL = 0;
      pce.init();
      arrivProc.init (bgen.nextDouble());
      agentGroup.init();
      waitingQueue.init();
      arrivProc.start();
      pce.start();
      Sim.start();    // Here the simulation is running...
      pce.stop();
      statArrivals.add (nArrivals);
      statAbandon.add (nAbandon / nCallsExpected);
      statGoodSL.add (nGoodSL / nCallsExpected);
      statWaits.add (statWaitsDay.sum() / nCallsExpected);
   }

   void simulate (int numDays) {
      statArrivals.init();     statAbandon.init();
      statGoodSL.init();       statWaits.init();
      for (int r=1; r <= numDays; r++)  simulateOneDay();
   }

   void printStatistics() {
      System.out.println ("\n Num. calls expected = " + nCallsExpected +"\n");
      System.out.println (statArrivals.reportAndCIStudent (0.9, 3));
      System.out.println (statWaits.reportAndCIStudent (0.9, 3));
      System.out.println (statGoodSL.reportAndCIStudent (0.9, 3));
      System.out.println (statAbandon.reportAndCIStudent (0.9, 4));
   }

   static public void main (String[] args) throws IOException {
      final CallCC cc = new CallCC ("CallCenter.dat");
      cc.simulate (1000);
      cc.printStatistics();
   }
}
