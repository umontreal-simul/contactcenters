package umontreal.iro.lecuyer.contactcenters.ctmc;

public class PriorityQueueSelectorWT implements WaitingQueueSelector {
   private double[] ranks;
   
   public PriorityQueueSelectorWT (double[] ranks) {
      this.ranks = ranks;
   }
   
   public int selectWaitingQueue (CallCenterCTMC ctmc, int k, int tr) {
      final CallCenterCTMCWithQueues ctmcQ = (CallCenterCTMCWithQueues)ctmc;
      double bestRank = Double.POSITIVE_INFINITY;
      int longestQueueTime = 0;
      int bestK = -1;
      for (int kp = 0; kp < ranks.length; kp++) {
         if (Double.isInfinite (ranks[kp]))
            continue;
         if (ctmc.getNumContactsInQueue (kp) == 0)
            continue;
         final int wt = ctmcQ.getLongestWaitingTime (kp);
         if (ranks[kp] < bestRank) {
            bestRank = ranks[kp];
            longestQueueTime = wt;
            bestK = kp;
         }
         else if (ranks[kp] == bestRank && wt > longestQueueTime) {
            longestQueueTime = wt;
            bestK = kp;
         }
      }
      return bestK;
   }
   
   public double[] getRanks() {
      return ranks.clone ();
   }
}
