package umontreal.iro.lecuyer.contactcenters.app;

/**
 * Represents an observer of the progress of a simulation.
 */
public interface ContactCenterSimListener {
   /**
    * Indicates that a new simulation was started by the simulator \texttt{sim},
    * and that it will consist of \texttt{numTargetSteps} steps.
    * 
    * @param sim
    *           the contact center simulator.
    * @param numTargetSteps
    *           the predicted number of steps.
    */
   public void simulationStarted (ObservableContactCenterSim sim,
         int numTargetSteps);

   /**
    * Indicates that an in-progress simulation performed by the simulator
    * \texttt{sim} is extended to consist of \texttt{newNumTargetSteps} steps.
    * This occurs when sequential sampling is used to reach a certain percision.
    * 
    * @param sim
    *           the contact center simulator.
    * @param newNumTargetSteps
    *           the new target number of steps.
    */
   public void simulationExtended (ObservableContactCenterSim sim,
         int newNumTargetSteps);

   /**
    * Indicates that a simulation performed by \texttt{sim} is terminated. If
    * \texttt{aborted} is \texttt{true}, the simulation was stopped using
    * {@link ObservableContactCenterSim#abort}. Otherwise, it has terminated
    * after the target number of steps is reached.
    * 
    * @param sim
    *           the contact center simulator.
    * @param aborted
    *           \texttt{true} if and only if the simulation was aborted.
    */
   public void simulationStopped (ObservableContactCenterSim sim,
         boolean aborted);

   /**
    * Indicates that a step was done by the simulator \texttt{sim}. One can use
    * {@link ContactCenterSim#getCompletedSteps} to obtain the number of
    * completed steps.
    * 
    * @param sim
    *           the contact center simulator.
    */
   public void stepDone (ObservableContactCenterSim sim);
}
