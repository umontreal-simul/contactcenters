import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.AgentGroupSelectors;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

import umontreal.iro.lecuyer.simevents.Sim;

public class SimpleMSKWithRerouting extends SimpleMSK {
   static final int[][] TYPETOGROUPMAP1  = { { 0 }, { 0 }, { 1 } };
   static final int[][] TYPETOGROUPMAP2  = { { 0 }, { 0, 1 }, { 1 } };
   static final double DELAY = 0.2;

   @Override
   Router createRouter () {
      return new MyRouter (TYPETOGROUPMAP1, GROUPTOTYPEMAP);
   }

   class MyRouter extends SingleFIFOQueueRouter {
      public MyRouter (int[][] typeToGroupMap, int[][] groupToTypeMap) {
         super (typeToGroupMap, groupToTypeMap);
      }

      @Override
      protected double getReroutingDelay (DequeueEvent dqEv, int numReroutingsDone) {
         return numReroutingsDone == -1 ? DELAY : -1;
      }

      @Override
      protected EndServiceEvent selectAgent (DequeueEvent dqEv, int numReroutingsDone) {
         final Contact contact = dqEv.getContact ();
         final AgentGroup g = AgentGroupSelectors.selectFirst
         (this, TYPETOGROUPMAP2[contact.getTypeId()]);
         if (g == null)
            return null;
         final EndServiceEvent es = g.serve (contact);
         assert es != null : "AgentGroup.serve should not return null";
         return es;
      }

      @Override
      protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
         WaitingQueue bestQueue = null;
         final double enqueueTime = Double.POSITIVE_INFINITY;
         for (final int k : groupToTypeMap[group.getId()]) {
            final WaitingQueue queue = getWaitingQueue (k);
            if (queue.isEmpty ())
               continue;
            final DequeueEvent firstContact = queue.getFirst ();
            final double time = firstContact.getEnqueueTime ();
            if (group.getId () == 1 && k == 1 && (Sim.time() - time) < DELAY)
               continue;
            if (time < enqueueTime)
               bestQueue = queue;
         }
         if (bestQueue == null)
            return null;
         else
            return bestQueue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
      }
   }

   public static void main (String[] args) {
      final SimpleMSKWithRerouting s = new SimpleMSKWithRerouting();
      s.simulate (NUMDAYS);
      s.printStatistics();
   }
}
