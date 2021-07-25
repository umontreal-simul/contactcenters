import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.util.ExceptionUtil;

public class SimulateScenariosThreads {
   private List<SimulateScenarios> sims;
   private ExecutorService exe;
   private ThreadLocal<SimulateScenarios> sceTh = new ThreadLocal<SimulateScenarios> ();
   private String resFileBase;

   public SimulateScenariosThreads (CallCenterParams ccParams,
         SimParams simParams, String resFileBase, int numThreads)
         throws CallCenterCreationException {
      this.resFileBase = resFileBase;
      sims = new LinkedList<SimulateScenarios> ();
      RandomStreams streams = null;
      for (int i = 0; i < numThreads; i++) {
         CallCenterSim sim;
         if (streams == null) {
            sim = new CallCenterSim (ccParams, simParams);
            streams = sim.getCallCenter ().getRandomStreams ();
         }
         else
            sim = new CallCenterSim (ccParams, simParams, streams.clone ());
         SimulateScenarios sce = new SimulateScenarios (sim);
         sims.add (sce);
      }
      exe = Executors.newFixedThreadPool (numThreads);
   }

   public void scheduleScenario (double arrivalsMult, double patienceMult,
         double serviceMult, double agentsMult) {
      exe.submit (new Scenario (arrivalsMult, patienceMult, serviceMult,
            agentsMult));
   }

   public void simulate () {
      exe.shutdown ();
   }

   private class Scenario implements Runnable {
      private double arrivalsMult;
      private double patienceMult;
      private double serviceMult;
      private double agentsMult;

      public Scenario (double arrivalsMult, double patienceMult,
            double serviceMult, double agentsMult) {
         this.arrivalsMult = arrivalsMult;
         this.patienceMult = patienceMult;
         this.serviceMult = serviceMult;
         this.agentsMult = agentsMult;
      }

      public void run () {
         SimulateScenarios sce = sceTh.get ();
         if (sce == null) {
            synchronized (sims) {
               sce = sims.remove (0);
               sceTh.set (sce);
            }
         }

         sce.setArrivalsMult (arrivalsMult);
         sce.setPatienceTimesMult (patienceMult);
         sce.setServiceTimesMult (serviceMult);
         sce.setAgentsMult (agentsMult);
         try {
            sce.simulateScenario (resFileBase);
         }
         catch (IOException e) {
            e.printStackTrace ();
         }
         catch (JAXBException e) {
            e.printStackTrace ();
         }
      }
   }

   public static void main (String[] args) {
      if (args.length != 3 && args.length != 4) {
         System.err.println ("Usage: java SimulateScenarios <ccParams> "
               + "<simParams> <resFileBase> [numThreads]");
         System.exit (1);
      }

      String ccParamsFn = args[0];
      String simParamsFn = args[1];
      String resFileBase = args[2];
      int numThreads;
      if (args.length > 3)
         numThreads = Integer.parseInt (args[3]);
      else
         numThreads = Runtime.getRuntime ().availableProcessors (); 

      CallCenterParamsConverter cnvCC = new CallCenterParamsConverter ();
      CallCenterParams ccParams = cnvCC.unmarshalOrExit (new File (ccParamsFn));
      SimParamsConverter simCC = new SimParamsConverter ();
      SimParams simParams = simCC.unmarshalOrExit (new File (simParamsFn));
      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());

      SimulateScenariosThreads sth;
      try {
         sth = new SimulateScenariosThreads (ccParams, simParams, resFileBase,
               numThreads);
      }
      catch (CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }

      for (double arrivalsMult : new double[] { 0.8, 1, 1.2 })
         for (double patienceMult : new double[] { 0.8, 1, 1.2 })
            for (double serviceMult : new double[] { 0.8, 1, 1.2 })
               for (double agentsMult : new double[] { 0.8, 1, 1.2 })
                  sth.scheduleScenario (arrivalsMult, patienceMult,
                        serviceMult, agentsMult);
      sth.simulate ();
   }
}
