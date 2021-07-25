package umontreal.iro.lecuyer.contactcenters.app;

/**
 * Represents a simulation stopping condition which is checked before the
 * simulation ends. By default, a simulator performs a minimal number of
 * replications or a single replication with a minimal length to get some
 * statistics and performs some tests to determine the additional simulation
 * time. If an additional stopping condition is added through
 * {@link ContactCenterEval#setEvalOption}, this condition is checked and the
 * returned result is used instead of the default result.
 */
public interface SimStoppingCondition {
   /**
    * Checks the implemented stopping condition and returns the required number
    * of additional batches or replications to simulate. This method must be
    * given the contact center simulator and the number of additional
    * replications or batches to simulate according to the simulator's default
    * stopping condition. This number can be used or ignored and the returned
    * value will be used instead by the simulator.
    * 
    * @param sim
    *           the contact center simulator.
    * @param newReps
    *           the number of required additional batches or replications,
    *           according to the default stopping condition.
    * @return the number of new replications or batches, according to the
    *         implemented stopping condition.
    */
   public int check (ContactCenterSim sim, int newReps);
}
