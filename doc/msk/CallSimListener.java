import java.io.File;
import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimListener;
import umontreal.iro.lecuyer.contactcenters.app.ObservableContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

public class CallSimListener {
   public static void main (String[] args) throws CallCenterCreationException {
      if (args.length != 2) {
         System.err.println ("Usage: java CallSimListener <call center params>"
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
      final ObservableContactCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.addContactCenterSimListener (new SimListener ());

      sim.eval ();
      final DoubleMatrix2D sl = sim
            .getPerformanceMeasure (PerformanceMeasureType.SERVICELEVEL);
      System.out.printf ("Service level = %.3f%n", sl.get (sl.rows () - 1, sl
            .columns () - 1));
   }

   private static class SimListener implements ContactCenterSimListener {
      public void simulationExtended (ObservableContactCenterSim sim,
            int newNumTargetSteps) {
         System.out.println ("Simulation extended, " + newNumTargetSteps
               + " target steps");
      }

      public void simulationStarted (ObservableContactCenterSim sim,
            int numTargetSteps) {
         System.out.println ("Simulation started, " + numTargetSteps
               + " target steps");
      }

      public void simulationStopped (ObservableContactCenterSim sim,
            boolean aborted) {
         System.out.println ("Simulation stopped after "
               + sim.getCompletedSteps () + " steps");
      }

      public void stepDone (ObservableContactCenterSim sim) {
         System.out.println ("Simulation step " + sim.getCompletedSteps ()
               + " done");
      }
   }
}
