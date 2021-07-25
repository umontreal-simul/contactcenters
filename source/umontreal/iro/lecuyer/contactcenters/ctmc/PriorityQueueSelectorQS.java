package umontreal.iro.lecuyer.contactcenters.ctmc;

public class PriorityQueueSelectorQS implements WaitingQueueSelector {
   private double[] ranks;
   
   public PriorityQueueSelectorQS (double[] ranks) {
      this.ranks = ranks;
   }

   public int selectWaitingQueue (CallCenterCTMC ctmc, int k, int tr) {
      double bestRank = Double.POSITIVE_INFINITY;
      int longestQueueSize = 0;
      int bestK = -1;
      for (int kp = 0; kp < ranks.length; kp++) {
         if (Double.isInfinite (ranks[kp]))
            continue;
         final int qs = ctmc.getNumContactsInQueue (kp);
         if (qs == 0)
            continue;
         if (ranks[kp] < bestRank) {
            bestRank = ranks[kp];
            longestQueueSize = qs;
            bestK = kp;
         }
         else if (ranks[kp] == bestRank && qs > longestQueueSize) {
            longestQueueSize = qs;
            bestK = kp;
         }
      }
      return bestK;
   }

   public double[] getRanks() {
      return ranks.clone ();
   }
}
