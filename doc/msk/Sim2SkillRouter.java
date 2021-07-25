import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RouterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RouterManager;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.RouterParams;
import umontreal.iro.lecuyer.contactcenters.msk.spi.RouterFactory;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.LongestWeightedWaitingTimeRouter;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;


public class Sim2SkillRouter {
   public static class RouterWithReservedAgents
                 extends LongestWeightedWaitingTimeRouter {
      int minGeneralists;

      public RouterWithReservedAgents (int[][] typeToGroupMap,
            int[][] groupToTypeMap, double[] queueWeights, int minGeneralists) {
         super (typeToGroupMap, groupToTypeMap, queueWeights);
         if (typeToGroupMap.length != 2)
            throw new IllegalArgumentException
            ("This router supports only two call types");
         if (groupToTypeMap.length != 3)
            throw new IllegalArgumentException
            ("This router only supports three agent groups");
         this.minGeneralists = minGeneralists;
      }

      @Override
      protected EndServiceEvent selectAgent (Contact ct) {
         if (ct.getTypeId () == 1) {
            // Try to route to specialists
            AgentGroup group1 = getAgentGroup (1);
            if (group1.getNumFreeAgents () > 0)
               return group1.serve (ct);
            // Route to generalists if there are enough agents
            AgentGroup group2 = getAgentGroup (2);
            if (group2.getNumFreeAgents () > minGeneralists)
               return group2.serve (ct);
            return null;
         }
         return super.selectAgent (ct);
      }

      @Override
      protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
         if (group.getId () == 2) {
            if (group.getNumFreeAgents () > minGeneralists)
               return super.selectContact (group, agent);
            WaitingQueue queue = getWaitingQueue (0);
            if (queue.size () > 0)
               return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
            else
               return null;
         }
         return super.selectContact (group, agent);
      }
   }

   public static class MyFactory implements RouterFactory {
      public Router createRouter (CallCenter cc, RouterManager rm,
            RouterParams par) throws RouterCreationException {
         if (!par.getRouterPolicy ().equals ("SIM2SKILL"))
            return null;
         rm.initTypeToGroupMap (par);
         rm.initGroupToTypeMap (par);
         rm.initQueueWeights (par);
         Integer omin = (Integer)rm.getProperties ().get ("minGeneralists");
         int min;
         if (omin == null)
            min = 0;
         else
            min = omin;
         System.out.printf ("minGeneralists=%d%n", min);
         return new RouterWithReservedAgents
         (rm.getTypeToGroupMap (),
               rm.getGroupToTypeMap (),
               rm.getQueueWeights (), min);
      }

   }

   public static void main (String[] args) throws CallCenterCreationException,
                                                  IOException, JAXBException {
      RouterManager.addRouterFactory (new MyFactory());
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
