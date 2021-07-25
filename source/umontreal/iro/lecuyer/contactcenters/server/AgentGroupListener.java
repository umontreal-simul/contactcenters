package umontreal.iro.lecuyer.contactcenters.server;

/**
 * Represents an agent-group listener
 * which is notified when the number of agents
 * in a group is modified or when a service starts or ends.
 */
public interface AgentGroupListener {
   /**
    * This method is called when the number of available or free
    * agents in the agent group \texttt{group} is changed.
    * This happens when the {@link AgentGroup#setNumAgents}
    * method is called, or when the
    * efficiency is changed.
    * This is also called when {@link DetailedAgentGroup#addAgent}
    * or {@link DetailedAgentGroup#removeAgent} are used.
    @param group the agent group being modified.
    */
   public void agentGroupChange (AgentGroup group);

   /**
    * This method is called after the service of a
    * contact by an agent
    * was started.  The end-service event \texttt{ev}
    * holds all the available information about the service.
    @param ev the end-service event associated with the contact being served.
   */
   public void beginService (EndServiceEvent ev);

   /**
    * This method is called after the communication of a
    * contact with an agent
    * was terminated, with \texttt{ev} containing
    * all the information.
    @param ev the end-service event associated with the served contact.
   */
   public void endContact (EndServiceEvent ev);

   /**
    * This method is called after the service of a
    * contact by an agent
    * was terminated.  The service includes
    * the communication as well as the
    * after-contact work.
    @param ev the end-service event associated with the served contact.
   */
   public void endService (EndServiceEvent ev);

   /**
    * This method is called after the {@link AgentGroup#init} method
    * is called for the agent group \texttt{group}.
    @param group the agent group being initialized.
    */
   public void init (AgentGroup group);
}
