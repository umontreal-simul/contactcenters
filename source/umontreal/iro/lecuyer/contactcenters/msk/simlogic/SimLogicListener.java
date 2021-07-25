package umontreal.iro.lecuyer.contactcenters.msk.simlogic;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;

/**
 * Represents an observer of the progress of a simulation.
 */
public interface SimLogicListener {
   /**
    * Indicates that a step was done by the simulator \texttt{sim}. One can use
    * {@link ContactCenterSim#getCompletedSteps} to obtain the number of
    * completed steps.
    * 
    * @param sim
    *           the contact center simulation logic.
    */
   public void stepDone (SimLogic sim);
}
