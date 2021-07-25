package umontreal.iro.lecuyer.contactcenters.ctmc;

public class SimpleGroupSelector implements AgentGroupSelector {
   public int selectAgentGroup (CallCenterCTMC ctmc, int tr) {
      assert ctmc.getNumAgentGroups() == 1;
      if (ctmc.getNumContactsInService() < ctmc.getNumAgents())
         return 0;
      return -1;
   }
   
   public double[] getRanks() {
      return new double[] { 1 };
   }
}
