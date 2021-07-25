package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * This extends the queue priority router to select contacts
 * in the longest waiting queue.
 * When a contact arrives into the router, the same
 * scheme for agent group selection as with the queue priority router
 * is used.
 * However, when an agent becomes free, instead of using
 * the group-to-type map to determine the order in which
 * the queues are queried, all queues authorized by the
 * group-to-type map are
 * considered, and a contact is removed from the longest one.
 * If more than one queue has the same maximal size,
 * the contact is removed from the first queue
 * in the ordered list given by the group-to-type map.
 */
public class LongestQueueFirstRouter extends QueuePriorityRouter {
   /**
    * Constructs a new longest-queue-first router with
    * a type-to-group map \texttt{typeToGroupMap} and a
    * group-to-type map \texttt{groupToTypeMap}.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    */
   public LongestQueueFirstRouter (int[][] typeToGroupMap,
                               int[][] groupToTypeMap) {
      super (typeToGroupMap, groupToTypeMap);
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final int[] rankList = groupToTypeMap[group.getId()];
      final WaitingQueue queue = WaitingQueueSelectors.selectLongest (this, rankList);
      if (queue == null)
         return null;
      if (queue.isEmpty ())
         return null;
      else
         return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   public String getDescription() {
      return "Longest queue router";
   }
}
