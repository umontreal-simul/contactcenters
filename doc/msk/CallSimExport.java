import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

public class CallSimExport {
   public static void main (String[] args) throws CallCenterCreationException,
      IOException, JAXBException {
      if (args.length != 2 && args.length != 3) {
         System.err.println ("Usage: java CallSim <call center params>"
               + " <simulation params> [output file]");
         System.exit (1);
      }
      final String ccPsFn = args[0];
      final String simPsFn = args[1];
      final String outputFn = args.length > 2 ? args[2] : null;

      // Reading model parameters
      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      final CallCenterParams ccPs = cnvCC.unmarshalOrExit (new File (ccPsFn));

      // Reading simulation parameters
      final SimParamsConverter cnvSim = new SimParamsConverter();
      final SimParams simPs = cnvSim.unmarshalOrExit (new File (simPsFn));

      // Construct the simulator
      SimRandomStreamFactory.initSeed (simPs.getRandomStreams());
      final ContactCenterSim sim = new CallCenterSim (ccPs, simPs);
      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (),
            args[0], args[1]);

      // The remainder of the program is independent of the specific simulator
      sim.eval ();
      PerformanceMeasureFormat.formatResults (sim, outputFn);
   }
}
