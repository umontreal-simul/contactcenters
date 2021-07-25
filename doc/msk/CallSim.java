import java.io.File;
import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

public class CallSim {
   public static void main (String[] args) throws CallCenterCreationException {
      if (args.length != 2) {
         System.err.println ("Usage: java CallSim <call center params>"
               + " <simulation params>");
         System.exit (1);
      }
      final String ccPsFn = args[0];
      final String simPsFn = args[1];

      // Reading model parameters
      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      final CallCenterParams ccPs = cnvCC.unmarshalOrExit (new File (ccPsFn));

      // Reading simulation parameters
      final SimParamsConverter cnvSim = new SimParamsConverter();
      final SimParams simPs = cnvSim.unmarshalOrExit (new File (simPsFn));

      // Construct the simulator
      SimRandomStreamFactory.initSeed (simPs.getRandomStreams());
      final ContactCenterSim sim = new CallCenterSim (ccPs, simPs);

      // The remainder of the program is independent of the specific simulator
      sim.eval ();
      final DoubleMatrix2D sl =
            sim.getPerformanceMeasure (PerformanceMeasureType.SERVICELEVEL);
      System.out.printf ("Service level = %.3f%n",
                        sl.get (sl.rows () - 1, sl.columns () - 1));
   }
}
