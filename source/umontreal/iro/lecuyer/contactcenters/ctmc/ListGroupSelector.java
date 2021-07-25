package umontreal.iro.lecuyer.contactcenters.ctmc;

import umontreal.iro.lecuyer.contactcenters.router.RoutingTableUtils;

/**
 * Represents an agent group selector using
 * static lists for agent selection.
 * When a call enters the center, the group of the serving agent
 * is given by the first group, among the user-specified
 * groups $i_0, i_1, \ldots$, containing at least one free agent.
 */
public class ListGroupSelector implements AgentGroupSelector {
   private int numGroups;
   private int[] groupList;
   
   /**
    * Constructs a new list-based agent group selector using
    * the given static list \texttt{groupList}.
    * @param groupList the list of agent groups being queried
    * by {@link #selectAgentGroup(CallCenterCTMC,int)}.
    */
   public ListGroupSelector (int numGroups, int[] groupList) {
      this.numGroups = numGroups;
      this.groupList = groupList;
   }

   public int selectAgentGroup (CallCenterCTMC ctmc, int tr) {
      for (int idx = 0; idx < groupList.length; idx++) {
         final int ip = groupList[idx];
         if (ip < 0)
            continue;
         if (ctmc.getNumContactsInServiceI (ip) < ctmc.getNumAgents (ip))
            return ip;
      }
      return -1;
   }
   
   public double[] getRanks() {
      int[][] typeToGroupMap = new int[][] {
            groupList
      };
      double[][] ranks = RoutingTableUtils.getRanksFromTG (numGroups, typeToGroupMap);
      return ranks[0];
   }
}
