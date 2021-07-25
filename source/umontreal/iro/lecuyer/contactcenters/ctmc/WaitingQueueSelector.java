package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents a policy selecting a queued contact
 * for an agent becoming free, in a CTMC call center model.
 * A CTMC model supporting multiple agent groups
 * can have a different waiting queue selector for
 * each agent group.
 * The available implementations are
 * {@link ListQueueSelector},
 * {@link PriorityQueueSelectorQS}, and
 * {@link PriorityQueueSelectorWT}.
 */
public interface WaitingQueueSelector {
   /**
    * Selects a waiting queue for the free agent,
    * and returns the index of the selected queue.
    * If no waiting queue can be selected, this
    * method returns a negative value.
    * @param ctmc the call center CTMC model.
    * @param tr the current transition number.
    * @return the selected waiting queue.
    */
   public int selectWaitingQueue (CallCenterCTMC ctmc, int k, int tr);
   
   /**
    * Returns an array giving the rank associated with
    * each waiting queue by this waiting queue selector.
    * @return the array of ranks.
    */
   public double[] getRanks();
}
