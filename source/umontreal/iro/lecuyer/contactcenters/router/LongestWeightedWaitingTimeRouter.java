package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * This extends the queue priority router to select
 * contacts with the longest weighted waiting time.
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
 * contact with the longest weighted waiting time is removed.
 * More specifically, let $w_q$ be a user-defined
 * weight associated with waiting queue $q$, and let
 * $W_q$ be the waiting time of the first contact
 * waiting in queue $q$ (if queue $q$ is empty,
 * let $W_q=-\infty$).
 * The router then selects the first contact in the
 * queue with the maximal $w_qW_q$ value.
 * If more than one queue authorized by the
 * freed agent has a first contact
 * with the same weighted waiting time, which rarely happens in practice,
 * the contact is removed from the first queue
 * in the ordered list obtained from the group-to-type map.
 */
public class LongestWeightedWaitingTimeRouter extends QueuePriorityRouter {
   private double[] queueWeights;
   
   /**
    * Constructs a new longest weighted waiting time router with
    * a type-to-group map \texttt{typeToGroupMap}, a
    * group-to-type map \texttt{groupToTypeMap}, and
    * an array of weights \texttt{queueWeights}.
    * Each element of the last array corresponds
    * to a weight assigned to a waiting queue.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    @param queueWeights the array of weights $w_q$ for waiting queues.
    */
   public LongestWeightedWaitingTimeRouter (int[][] typeToGroupMap,
                                 int[][] groupToTypeMap,
                                 double[] queueWeights) {
      super (typeToGroupMap, groupToTypeMap);
      if (queueWeights.length != getNumWaitingQueues())
         throw new IllegalArgumentException
         ("The length of the array of weights (" 
               + queueWeights.length + ") must be the same as the number of waiting queues ("
               + getNumWaitingQueues() + ")");
      this.queueWeights = queueWeights;
   }
   
   /**
    * Returns the weights associated with each waiting queue.
    * Element $q$ of the returned array
    * contains the weight $w_q$ for the waiting queue $q$.
    * @return the array of weights.
    */
   public double[] getQueueWeights() {
      return queueWeights;
   }
   
   /**
    * Sets the weights of waiting queues to \texttt{queueWeights}.
    * @param queueWeights the new array of weights.
    */
   public void setQueueWeights (double[] queueWeights) {
      if (queueWeights.length != getNumWaitingQueues())
         throw new IllegalArgumentException
         ("The length of the array of weights (" 
               + queueWeights.length + ") must be the same as the number of waiting queues ("
               + getNumWaitingQueues() + ")");
      this.queueWeights = queueWeights;
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final int[] rankList = groupToTypeMap[group.getId()];
      final WaitingQueue queue = WaitingQueueSelectors.selectLongestWeightedWaitingTime
         (this, rankList, queueWeights);
      if (queue == null)
         return null;
      if (queue.isEmpty ())
         return null;
      else
         return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }
   
   @Override
   public String getDescription() {
      return "Longest weighted waiting time router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ("\nWeight of waiting queues: ");
      for (int q = 0; q < queueWeights.length; q++) {
         if (q > 0)
            sb.append (", ");
         sb.append (queueWeights[q]);
      }
      return sb.toString();
   }
}
