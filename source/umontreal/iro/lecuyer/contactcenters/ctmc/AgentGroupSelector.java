package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents a policy selecting an agent group
 * for an incoming contact, in a CTMC call center model.
 * A CTMC model supporting multiple call types
 * can have a different agent group selector for
 * each contact type.
 * The available implementations are
 * {@link ListGroupSelector}, and
 * {@link PriorityGroupSelector}.
 */
public interface AgentGroupSelector {
   /**
    * Selects an agent group for the newly arrived contact,
    * and returns the index of the selected agent group.
    * If no agent group can be selected, this
    * method returns a negative value.
    * @param ctmc the call center CTMC model.
    * @param tr the current transition number.
    * @return the selected agent group.
    */
   public int selectAgentGroup (CallCenterCTMC ctmc, int tr);
   
   /**
    * Returns an array giving the rank associated with
    * each agent group by this agent group selector.
    * @return the array of ranks.
    */
   public double[] getRanks();
}
