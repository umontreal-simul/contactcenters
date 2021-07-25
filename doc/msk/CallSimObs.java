import java.io.File;
import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimWithObservations;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

public class CallSimObs {
   public static void main (String[] args) throws CallCenterCreationException {
      if (args.length != 2) {
         System.err.println ("Usage: java CallSimObs <call center params>"
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

      if (!simPs.isKeepObs ()) {
         System.err.println ("The simulator does not store observations");
         System.err.println ("Enabling observation keeping");
         simPs.setKeepObs (true);
      }

      // Construct the simulator
      SimRandomStreamFactory.initSeed (simPs.getRandomStreams());
      final ContactCenterSimWithObservations sim = new CallCenterSim (ccPs, simPs);

      sim.eval ();

      final DoubleMatrix2D sl =
         sim.getPerformanceMeasure (PerformanceMeasureType.RATEOFSERVICESBEFOREAWT);
      final double[] obs = sim.getObs (PerformanceMeasureType.RATEOFSERVICESBEFOREAWT,
                                 sl.rows () - 1, sl.columns () - 1);
      System.out.println ("\nObservations for the number "
            + "of served contacts waiting less than s seconds");
      for (final double element : obs)
         System.out.println (element);
   }
}
