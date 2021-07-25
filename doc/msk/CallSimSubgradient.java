import java.io.File;
import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.util.Chrono;

public class CallSimSubgradient {
   public static void main (String[] args) throws CallCenterCreationException {
      if (args.length != 2) {
         System.err.println ("Usage: java CallSimSubgradient <call center params>"
                             + " <simulation params>");
         System.exit (1);
      }
      final String ccPsFn = args[0];
      final String simPsFn = args[1];
      // Reading model parameters
      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter ();
      final CallCenterParams ccPs = cnvCC.unmarshalOrExit (new File (ccPsFn));

      // Reading simulation parameters
      final SimParamsConverter cnvSim = new SimParamsConverter ();
      final SimParams simPs = cnvSim.unmarshalOrExit (new File (simPsFn));

      // Construct the simulator
      SimRandomStreamFactory.initSeed (simPs.getRandomStreams ());
      final ContactCenterSim sim = new CallCenterSim (ccPs, simPs);

      final Chrono timer = new Chrono ();
      sim.eval ();
      DoubleMatrix2D slm = sim
            .getPerformanceMeasure (PerformanceMeasureType.SERVICELEVEL);
      final double sl = slm.get (slm.rows () - 1, slm.columns () - 1);
      System.out.println ("CPU time: " + timer.format ());
      System.out.printf ("Service level = %.3f%n", sl);
      final int[][] staffing = (int[][]) sim
            .getEvalOption (EvalOptionType.STAFFINGMATRIX);
      final double[][] subg = new double[staffing.length][staffing[0].length];
      for (int i = 0; i < staffing.length; i++)
         for (int p = 0; p < staffing[i].length; p++) {
            ++staffing[i][p];
            sim.setEvalOption (EvalOptionType.STAFFINGMATRIX, staffing);
            --staffing[i][p];
            sim.eval ();
            slm = sim
                  .getPerformanceMeasure (PerformanceMeasureType.SERVICELEVEL);
            final double slg = slm.get (slm.rows () - 1, slm.columns () - 1);
            subg[i][p] = slg - sl;
         }
      sim.setEvalOption (EvalOptionType.STAFFINGMATRIX, staffing);
      System.out.println ("Total CPU time: " + timer.format ());
      System.out.print ("Subgradient = [");
      for (int i = 0; i < subg.length; i++) {
         if (i > 0)
            System.out.print (", ");
         System.out.print ("[");
         for (int p = 0; p < subg[i].length; p++) {
            if (p > 0)
               System.out.print (", ");
            System.out.printf ("%.5f", subg[i][p]);
         }
         System.out.print ("]");
      }
      System.out.println ("]");
   }
}
