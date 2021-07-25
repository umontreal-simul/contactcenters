import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.old.app.BatchSimParams;
import umontreal.iro.lecuyer.contactcenters.old.app.RepSimParams;
import umontreal.iro.lecuyer.contactcenters.old.app.SimParams;
import umontreal.iro.lecuyer.contactcenters.old.msk.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.old.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.old.msk.RouterParams;
import umontreal.iro.lecuyer.contactcenters.old.msk.model.Model;
import umontreal.iro.lecuyer.contactcenters.old.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

import umontreal.iro.lecuyer.xmlconfig.ParamReader;

public class CallCenterSimWithCustomRouter extends CallCenterSim {
   public CallCenterSimWithCustomRouter (CallCenterParams ccParams,
         SimParams simParams) {
      super (ccParams, simParams);
   }

   public CallCenterSimWithCustomRouter (CallCenterParams ccParams,
         SimParams simParams, RandomStreams streams) {
      super (ccParams, simParams, streams);
   }

   @Override
   protected Model createModel (CallCenterParams ccPs, RandomStreams streams) {
      final Model model = new CustomModel (ccPs, streams);
      model.create (false);
      return model;
   }

   private static class CustomModel extends Model {
      public CustomModel (CallCenterParams ccParams, RandomStreams streams) {
         super (ccParams, streams);
      }

      @Override
      public Router createRouter () {
         final double[] mus = new double[getNumContactTypes()];
         final double[] ns = new double[getNumContactTypes()];
         for (int k = 0; k < mus.length; k++)
            mus[k] = 1.0/getCallCenterParams().getCallType (k).getServiceTime().getMean();
         // Initialize ns too
         return new CustomRouter (mus, ns,
               getNumAgentGroups ());
      }
   }

   public static void main (String[] args) throws IOException,
         MalformedURLException,
         ParserConfigurationException,
         SAXException,
         JAXBException {
      if (args.length != 2 && args.length != 3) {
         System.err
               .println ("Usage: java CallCenterSimWithCustomRouter "
                     + "<call center data file name> <experiment parameter file> [<output file name>]");
         System.exit (1);
      }
      final String ccParamsFn = args[0];
      final String simParamsFn = args[1];
      if (!new File (ccParamsFn).exists ()) {
         System.err.println ("Cannot find the file " + ccParamsFn);
         System.exit (1);
      }
      if (!new File (simParamsFn).exists ()) {
         System.err.println ("Cannot find the file " + simParamsFn);
         System.exit (1);
      }
      File outputFile = null;
      if (args.length == 3)
         outputFile = new File (args[2]);

      final ParamReader reader = new ParamReader ();
      CallCenterParams.initParamReader (reader);
      RepSimParams.initParamReader (reader);
      BatchSimParams.initParamReader (reader);

      final CallCenterParams ccParams = (CallCenterParams) reader
            .readURL (ccParamsFn);
      ccParams.check ();
      final SimParams simParams = (SimParams) reader.readURL (simParamsFn);
      simParams.check ();

      final RouterParams routerParams = ccParams.getRouter();
      System.out.printf ("Test period duration: %.5f\n", routerParams.getTestPeriodDuration());
      System.out.printf ("Threshold on queue size: %d\n", routerParams.getQueueSizeThresh());
      final double[] fa = routerParams.getTargetFracAgents();
      int numAgents = 0;
      for (int i = 0; i < ccParams.getNumAgentGroups(); i++)
         numAgents += ccParams.getAgentGroup (i).getStaffing (0);
      System.out.printf ("Number of agents: %d\n", numAgents);
      for (int k = 0; k < fa.length; k++)
         fa[k] *= numAgents;
      System.out.println ("Fraction of agents: " + Arrays.toString (fa));

      final CallCenterSim sim = new CallCenterSimWithCustomRouter (ccParams, simParams);

      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (), ccParamsFn, simParamsFn);
      sim.eval ();
      PerformanceMeasureFormat.formatResults (sim, outputFile);
   }
}

class CustomRouter extends Router {
   public CustomRouter (double[] mus, double[] ns, int numGroups) {
      super (mus.length, mus.length, numGroups);
   }

   @Override
   public boolean canServe (int i, int k) {
      return true;
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
   // TODO Auto-generated method stub
   }

   @Override
   protected EndServiceEvent selectAgent (Contact contact) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact contact) {
      final WaitingQueue queue = getWaitingQueue (contact.getTypeId ());
      if (queue == null)
         return null;
      final DequeueEvent ev = queue.add (contact);
      assert ev != null : "WaitingQueue.add should not return null";
      return ev;
   }
}
