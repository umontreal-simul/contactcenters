package umontreal.iro.lecuyer.contactcenters.ctmc;

import umontreal.iro.lecuyer.contactcenters.router.RoutingTableUtils;

/**
 * Represents a waiting queue selector using static lists.
 * When an agent becomes free, the router selects the first
 * waiting queue, among the user-specified queues
 * $k_0, k_1, \dots$, containing at least one call.
 * If such a queue exists, the first queued call is removed, and assigned
 * to the free agent. Otherwise, the agent stays free until a new call
 * arrives.
 */
public class ListQueueSelector implements WaitingQueueSelector {
   private int numQueues;
   private int[] queueList;
   
   /**
    * Constructs a new list-based waiting queue selector
    * using the given list \texttt{queueList}.
    * @param queueList the list of waiting queried by
    * {@link #selectWaitingQueue(CallCenterCTMC,int,int)}.
    */
   public ListQueueSelector (int numQueues, int[] queueList) {
      this.numQueues = numQueues;
      this.queueList = queueList;
   }



   public int selectWaitingQueue (CallCenterCTMC ctmc, int k, int tr) {
      for (int idx = 0; idx < queueList.length; idx++) {
         final int kp = queueList[idx];
         if (kp < 0)
            continue;
         if (ctmc.getNumContactsInQueue (kp) > 0)
            return kp;
      }
      return -1;
   }
   
   public double[] getRanks() {
      int[][] groupToTypeMap = new int[][] {
            queueList
      };
      double[][] ranks = RoutingTableUtils.getRanksFromGT (numQueues, groupToTypeMap);
      return ranks[0];
   }
}
