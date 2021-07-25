package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.ssj.simevents.Event;

/**
 * Represents an event that restores the state of
 * busy and ghost agents after the service
 * of contacts are started, during state
 * restoration.
 */
public class RestoreAgentsEvent extends Event {
   private DetailedAgentGroup dgroup;
   private AgentState[] busyAgents;
   private AgentState[] ghostAgents;
   
   /**
    * Constructs a new agent restoration event
    * concerning agents in the group \texttt{dgroup}.
    * When the event occurs,
    * the state (available, last idle time, etc.)
    * will be restored for all agents referred by
    * \texttt{busyAgents} and \texttt{ghostAgents}
    * while agents referred by \texttt{ghostAgents}
    * will be removed from the agent group.
    * @param dgroup the agent group affected by the restoration.
    * @param busyAgents the busy agents to be restored.
    * @param ghostAgents the ghost agents to be removed from the group. 
    */
   public RestoreAgentsEvent (DetailedAgentGroup dgroup, AgentState[] busyAgents, AgentState[] ghostAgents) {
      super (dgroup.simulator());
      if (dgroup == null || busyAgents == null || ghostAgents == null)
         throw new NullPointerException();
      this.dgroup = dgroup;
      this.busyAgents = busyAgents;
      this.ghostAgents = ghostAgents;
   }
   
   /**
    * Returns the agent group affected by this event.
    * @return the target agent group.
    */
   public DetailedAgentGroup getTargetAgentGroup() {
      return dgroup;
   }
   
   /**
    * Returns the state of the busy agents
    * that will be restored when the event
    * occurs.
    * @return the state of the busy agents.
    */
   public AgentState[] getBusyAgents() {
      return busyAgents;
   }
   
   /**
    * Returns the state of the ghost agents
    * that will be restored when this event
    * occurs.
    * @return the state of ghost agents.
    */
   public AgentState[] getGhostAgents() {
      return ghostAgents;
   }

   @Override
   public void actions () {
      for (final AgentState state : busyAgents)
         state.restore();
      for (final AgentState state : ghostAgents) {
         dgroup.removeAgent (state.getAgent());
         state.restore();
      }
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Target agent group: ").append (dgroup.toString());
      sb.append (']');
      return sb.toString();
   }
}
