package umontreal.iro.lecuyer.contactcenters.ctmc;

public class PriorityGroupSelector implements AgentGroupSelector {
   private double[] ranks;
   
   public PriorityGroupSelector (double[] ranks) {
      this.ranks = ranks;
   }
   
   public double[] getRanks() {
      return ranks.clone ();
   }

   public int selectAgentGroup (CallCenterCTMC ctmc, int tr) {
      double bestRank = Double.POSITIVE_INFINITY;
      int maxNumFreeAgents = 0;
      int bestI = -1;
      for (int ip = 0; ip < ranks.length; ip++) {
         if (Double.isInfinite (ranks[ip]))
            continue;
         final int nf = ctmc.getNumAgents (ip) - ctmc.getNumContactsInServiceI (ip);
         if (nf == 0)
            continue;
         if (ranks[ip] < bestRank) {
            bestRank = ranks[ip];
            maxNumFreeAgents = nf;
            bestI = ip;
         }
         else if (ranks[ip] == bestRank && nf > maxNumFreeAgents) {
            maxNumFreeAgents = nf;
            bestI = ip;
         }
      }
      return bestI;
   }

}
