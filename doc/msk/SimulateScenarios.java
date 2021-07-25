import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.util.ExceptionUtil;

public class SimulateScenarios {
   private CallCenterSim sim;
   private double arrivalsMultOrig, serviceTimesMultOrig,
         patienceTimesMultOrig, agentsMultOrig;
   private double arrivalsMultExp = 1, serviceTimesMultExp = 1,
         agentsMultExp = 1, patienceTimesMultExp = 1;

   public SimulateScenarios (CallCenterSim sim) {
      this.sim = sim;
      arrivalsMultOrig = sim.getCallCenter ().getArrivalsMult ();
      serviceTimesMultOrig = sim.getCallCenter ().getServiceTimesMult ();
      patienceTimesMultOrig = sim.getCallCenter ().getPatienceTimesMult ();
      agentsMultOrig = sim.getCallCenter ().getAgentsMult ();
   }

   public void setArrivalsMult (double mult) {
      sim.getCallCenter ().setArrivalsMult (mult * arrivalsMultOrig);
      arrivalsMultExp = mult;
   }

   public void setPatienceTimesMult (double mult) {
      sim.getCallCenter ().setPatienceTimesMult (mult * patienceTimesMultOrig);
      patienceTimesMultExp = mult;
   }

   public void setServiceTimesMult (double mult) {
      sim.getCallCenter ().setServiceTimesMult (mult * serviceTimesMultOrig);
      serviceTimesMultExp = mult;
   }

   public void setAgentsMult (double mult) {
      sim.getCallCenter ().setAgentsMult (mult * agentsMultOrig);
      agentsMultExp = mult;
   }

   // Setting multipliers specific to call type k to multk
   // Arrival rate
   // sim.getCallCenter ().getArrivalProcessManager (k).setArrivalsMult (multk);
   // Mean patience time
   // sim.getCallCenter ().getCallFactory (k).setPatienceTimesMult (multk);
   // Mean service time
   // sim.getCallCenter ().getCallFactory (k).getServiceTimesManager
   // ().setServiceTimesMult (multk);
   // sim.getCallCenter ().getCallFactory (k).getServiceTimesManager
   // ().setServiceTimesMult (i, multki);
   // Staffing, for agent group i to multi
   // sim.getCallCenter ().getAgentGroupManager (i).setAgentsMult (multi);

   public static String VSTR = "Call volume multiplier";
   public static String PSTR = "Patience times multiplier";
   public static String SSTR = "Service times multiplier";
   public static String ASTR = "Staffing multiplier";

   public void simulateScenario (String resFileBase) throws IOException,
         JAXBException {
      StringBuilder sbName = new StringBuilder ();
      sbName.append ("arv");
      sbName.append (arrivalsMultExp);
      sbName.append ("pt");
      sbName.append (patienceTimesMultExp);
      sbName.append ("aht");
      sbName.append (serviceTimesMultExp);
      sbName.append ("ag");
      sbName.append (agentsMultExp);

      File outputFile = new File (resFileBase + sbName.toString () + ".xml.gz");
      if (outputFile.exists ())
         return;
      sim.getEvalInfo ().put (VSTR, arrivalsMultExp);
      sim.getEvalInfo ().put (PSTR, patienceTimesMultExp);
      sim.getEvalInfo ().put (SSTR, serviceTimesMultExp);
      sim.getEvalInfo ().put (ASTR, agentsMultExp);
      System.out.println ("Simulating scenario " + sbName.toString ());
      sim.eval ();
      System.out.println ("End of simulation for scenario " + sbName.toString ());
      PerformanceMeasureFormat.formatResults (sim, outputFile);
   }

   public static void main (String[] args) throws IOException, JAXBException {
      if (args.length != 3) {
         System.err.println ("Usage: java SimulateScenarios <ccParams> "
               + "<simParams> <resFileBase>");
         System.exit (1);
      }

      String ccParamsFn = args[0];
      String simParamsFn = args[1];
      String resFileBase = args[2];

      CallCenterParamsConverter cnvCC = new CallCenterParamsConverter ();
      CallCenterParams ccParams = cnvCC.unmarshalOrExit (new File (ccParamsFn));
      SimParamsConverter simCC = new SimParamsConverter ();
      SimParams simParams = simCC.unmarshalOrExit (new File (simParamsFn));

      CallCenterSim sim;
      try {
         sim = new CallCenterSim (ccParams, simParams);
      }
      catch (CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }

      SimulateScenarios exp = new SimulateScenarios (sim);
      for (double v : new double[] { 0.8, 1, 1.2 }) {
         exp.setArrivalsMult (v);
         for (double p : new double[] { 0.8, 1, 1.2 }) {
            exp.setPatienceTimesMult (p);
            for (double s : new double[] { 0.8, 1, 1.2 }) {
               exp.setServiceTimesMult (s);
               for (double a : new double[] { 0.8, 1, 1.2 }) {
                  exp.setAgentsMult (a);
                  exp.simulateScenario (resFileBase);
               }
            }
         }
      }
   }
}
