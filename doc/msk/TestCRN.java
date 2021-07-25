import java.io.File;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimWithObservations;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.stat.Tally;

public class TestCRN {
   // Adds the differences of observations to the given tally
   public static void addObsDiff (double[] obs0, double[] obs1, Tally tally) {
      assert obs0.length == obs1.length;
      tally.init ();
      for (int j = 0; j < obs0.length; j++) {
         double diff = obs1[j] - obs0[j];
         tally.add (diff);
      }
   }

   public static void main (String[] args) throws CallCenterCreationException {
      if (args.length != 4) {
         System.out.println
            ("Usage: java TestCRN ccParams simParams i deltaStaffing");
         System.exit (1);
      }
      String ccParamsFn = args[0];
      String simParamsFn = args[1];
      int i = Integer.parseInt (args[2]);
      int deltaStaffing = Integer.parseInt (args[3]);

      CallCenterParamsConverter cnvCC = new CallCenterParamsConverter ();
      CallCenterParams ccParams = cnvCC.unmarshalOrExit (new File (ccParamsFn));
      SimParamsConverter cnvSim = new SimParamsConverter ();
      SimParams simParams = cnvSim.unmarshalOrExit (new File (simParamsFn));
      simParams.setKeepObs (true);

      ContactCenterSimWithObservations sim = new CallCenterSim (ccParams,
            simParams);
      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (),
            ccParamsFn, simParamsFn);
      sim.setAutoResetStartStream (false);

      // The type of the performance measure of interest
      PerformanceMeasureType pm = PerformanceMeasureType.RATEOFINTARGETSL;
      // The dimensions of a typical matrix of performance measures of
      // the type pm.
      int rows = pm.rows (sim);
      int columns = pm.columns (sim);
      System.out.println ("Simulating with initial staffing");
      sim.eval ();
      // Gets the observations with the initial staffing
      double[] obs0 = sim.getObs (pm, rows - 1, columns - 1);
      // Resets random streams for the simulation with CRNs
      sim.resetStartStream ();
      // Adjusts staffing
      int[] staffing = (int[]) sim.getEvalOption (EvalOptionType.STAFFINGVECTOR);
      staffing[i] += deltaStaffing;
      sim.setEvalOption (EvalOptionType.STAFFINGVECTOR, staffing);
      System.out.println ("Simulating with updated staffing");
      sim.eval ();
      // Gets the observations with the updated staffing, correlated with
      // obs0
      double[] obs1 = sim.getObs (pm, rows - 1, columns - 1);
      System.out.println ("Simulating with updated staffing, IRN");
      sim.eval ();
      // Gets another independent array of observations
      double[] obs1i = sim.getObs (pm, rows - 1, columns - 1);

      // Now sum up the differences
      Tally diffIRN = new Tally ("Difference with IRNs");
      addObsDiff (obs0, obs1i, diffIRN);
      System.out.println (diffIRN.reportAndCIStudent (0.95, 3));
      Tally diffCRN = new Tally ("Difference with CRNs");
      addObsDiff (obs0, obs1, diffCRN);
      System.out.println (diffCRN.reportAndCIStudent (0.95, 3));
   }
}
