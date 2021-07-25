package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * This extends the queue priority router to implement
 * a single FIFO queue.
 * The router assumes that every attached waiting queue
 * uses a FIFO discipline.
 * When a contact arrives into the router, the same
 * scheme for
 * agent group selection as with the queue priority router
 * is used.
 * However, when an agent becomes free, instead of using
 * the group-to-type map to determine the order in which
 * the queues are queried, all queues authorized by
 * the group-to-type map
 * are considered, and the
 * contact with the longest waiting time is removed.
 * If more than one queue has a first contact
 * with the same queue time, which rarely happens in practice,
 * the contact is removed from the first one
 * in the ordered list obtained from the group-to-type map.
 * This policy is equivalent to but more efficient than
 * merging all waiting
 * queues, sorting the contacts in ascending
 * arrival times, and having the agents take the
 * first contact they can serve.
 */
public class SingleFIFOQueueRouter extends QueuePriorityRouter {
   /**
    * Constructs a new single FIFO queue router with
    * a type-to-group map \texttt{typeToGroupMap} and a
    * group-to-type map \texttt{groupToTypeMap}.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    */
   public SingleFIFOQueueRouter (int[][] typeToGroupMap,
                                 int[][] groupToTypeMap) {
      super (typeToGroupMap, groupToTypeMap);
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final int[] rankList = groupToTypeMap[group.getId()];
      final WaitingQueue queue = WaitingQueueSelectors.selectSmallestFirstEnqueueTime
         (this, rankList);
      if (queue == null)
         return null;
      if (queue.isEmpty())
         return null;
      else
         return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   public String getDescription() {
      return "Smallest enqueue time router";
   }
}
