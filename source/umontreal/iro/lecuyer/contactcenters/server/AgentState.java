package umontreal.iro.lecuyer.contactcenters.server;

/**
 * Represents the state of an agent in a group.
 */
public class AgentState {
   private Agent agent;
   private double idleSimTime;
   private double firstLoginTime;
   private boolean avail;
   private boolean ghost;
   
   /**
    * Constructs an agent state object holding
    * information about agent \texttt{agent}.
    * @param agent the agent for which to save state.
    */
   AgentState (Agent agent) {
      save (agent);
   }
   
   /**
    * Saves the state of the agent \texttt{agent}
    * in this object.
    * @param agent1 the agent to save state.
    */
   private void save (Agent agent1) {
      this.agent = agent1;
      if (agent1.isBusy())
         idleSimTime = Double.NaN;
      else
         idleSimTime = agent1.getIdleSimTime();
      firstLoginTime = agent1.getFirstLoginTime();
      avail = agent1.isAvailable();
      ghost = agent1.isGhost ();
   }
   
   /**
    * Restores the state of the agent \texttt{agent}
    * by using information in this object.
    * @param agent1 the agent to restore.
    */
   void restore (Agent agent1) {
      agent1.setIdleSimTime (idleSimTime);
      agent1.setFirstLoginTime (firstLoginTime);
      agent1.setAvailable (avail);
      agent1.ghost = ghost;
   }
   
   /**
    * Restores the state of the agent attached to
    * this state object.
    */
   public void restore() {
      restore (agent);
   }
   
   /**
    * Returns the agent for which the state was saved.
    * @return the agent for which the state was saved.
    */
   public Agent getAgent () {
      return agent;
   }

   /**
    * Determines the availability status of the
    * agent at the time of state saving.
    * @return \texttt{true} if the agent was
    * available for serving contacts at the time of
    * state saving, \texttt{false} otherwise.
    */
   public boolean wasAvailable () {
      return avail;
   }

   /**
    * Returns the first login time of the agent
    * at the time the state was saved.
    * @return the first login time of the agent.
    */
   public double getFirstLoginTime () {
      return firstLoginTime;
   }

   /**
    * Returns the last simulation time the
    * agent became idle, at the time of state saving.
    * @return the last idle time of the agent.
    */
   public double getIdleSimTime () {
      return idleSimTime;
   }
}
