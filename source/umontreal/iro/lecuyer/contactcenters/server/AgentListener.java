package umontreal.iro.lecuyer.contactcenters.server;

/**
 * Represents an agent listener being notified
 * when the state of an individual agent changes.
 */
public interface AgentListener {
   /**
    * This method is called when the availability status of the agent
    * \texttt{agent} changes to \texttt{avail}.
    @param agent the agent being affected.
    @param avail the new availability status.
    */
   public void agentAvailable (Agent agent, boolean avail);

   /**
    * This method is called when the agent \texttt{agent} is added
    * to the agent group \texttt{group}.
    @param agent the agent being added.
    @param group the agent group the agent is added to.
    */
   public void agentAdded (Agent agent, DetailedAgentGroup group);

   /**
    * This method is called when the agent \texttt{agent} is removed
    * from the agent group \texttt{group}.
    @param agent the agent being removed.
    @param group the agent group the agent is removed from.
    */
   public void agentRemoved (Agent agent, DetailedAgentGroup group);

   /**
    * This method is called when the {@link Agent#init} method is called.
    @param agent the initialized agent.
    */
   public void init (Agent agent);

   /**
    * This method is called after the service of a
    * contact by an agent
    * is started.  The end-service event \texttt{ev}
    * holds all the available information about the service.
    @param ev the end-service event associated with the contact being
    served by an agent.
   */
   public void beginService (EndServiceEventDetailed ev);

   /**
    * This method is called when the communication with
    * a contact is terminated.  The end-service event \texttt{ev}
    * holds all the available information about the service.
    @param ev the end-service event associated with the served contact.
    */
   public void endContact (EndServiceEventDetailed ev);

   /**
    * This method is called after the service of a
    * contact by an agent was terminated.
    * The service includes the communication and the
    * after-contact work.  The end-service event \texttt{ev}
    * holds all the available information about the service.
    @param ev the end-service event associated with the served contact.
   */
   public void endService (EndServiceEventDetailed ev);
}
