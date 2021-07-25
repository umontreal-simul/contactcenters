package umontreal.iro.lecuyer.contactcenters.server;

import java.util.List;


/**
 * Represents the state of a detailed agent group.
 */
public class DetailedAgentGroupState extends AgentGroupState {
   private AgentState[] busyAgents;
   private AgentState[] ghostAgents;
   private AgentState[] idleAgents;
   
   /**
    * Constructs a new agent group state object holding
    * state information about the agent group \texttt{group}.
    * @param group the agent group to save state.
    */
   protected DetailedAgentGroupState (DetailedAgentGroup group) {
      super (group);
      save (group);
   }
   
   private void save (DetailedAgentGroup dgroup) {
      busyAgents = saveState (dgroup.getBusyAgents());
      ghostAgents = saveState (dgroup.getGhostAgents());
      idleAgents = saveState (dgroup.getIdleAgents());
   }
   
   private AgentState[] saveState (List<? extends Agent> agents) {
      final AgentState[] states = new AgentState[agents.size()];
      int j = 0;
      for (final Agent agent : agents)
         states[j++] = agent.save();
      return states;
   }
   
   @Override
   void restore (AgentGroup group) {
      if (group instanceof DetailedAgentGroup)
         restore ((DetailedAgentGroup)group);
      else
         super.restore (group);
   }
   
   void restore (DetailedAgentGroup dgroup) {
//      Agent[] agents = dgroup.getBusyAgents();
//      for (int j = 0; j < agents.length; j++)
//         if (!agents[j].isGhost())
//            dgroup.removeAgent (agents[j]);
//      agents = dgroup.getIdleAgents();
//      for (int j = 0; j < agents.length; j++)
//         dgroup.removeAgent (agents[j]);
      dgroup.idleAgents.clear();
      dgroup.busyAgents.clear();
      dgroup.ghostAgents.clear();
      dgroup.init();

      for (final AgentState state : idleAgents) {
         final Agent agent = state.getAgent();
         agent.init();
         //dgroup.addAgent (agent);
         agent.group = dgroup;
         agent.es = null;
         dgroup.idleAgents.add (agent);
         agent.restore (state);
      }
      for (final AgentState state : busyAgents) {
         final Agent agent = state.getAgent();
         agent.init();
         //dgroup.addAgent (agent);
         //agent.setAvailable (true);
         agent.group = dgroup;
         //agent.ghost = false;
         dgroup.busyAgents.add (agent);
         agent.restore (state);
      }
      for (final AgentState state : ghostAgents) {
         final Agent agent = state.getAgent();
         dgroup.ghostAgents.add (agent);
         agent.group = null;
      }
      restoreServices (dgroup);
       
//      EndServiceEvent[] contactsInService = getContactsInService();
//      double maxTime = 0;
//      for (int i = 0; i < contactsInService.length; i++) {
//         Agent agent = ((EndServiceEventDetailed)contactsInService[i]).getAgent();
//         boolean agentInList = false;
//         for (int j = 0; j < busyAgents.length && !agentInList; j++)
//            if (busyAgents[j].getAgent() == agent)
//               agentInList = true;
//         for (int j = 0; j < ghostAgents.length && !agentInList; j++)
//            if (ghostAgents[j].getAgent() == agent)
//               agentInList = true;
//         if (!agentInList)
//            throw new AssertionError (agent.toString());
//         StartServiceEvent ev = new StartServiceEvent (dgroup, contactsInService[i]);
//         ev.schedule();
//         double time = ev.time();
//         if (time > maxTime)
//            maxTime = time;
//      }
//      new RestoreAgentsEvent (dgroup, busyAgents, ghostAgents).schedule (maxTime - Sim.time());
   }
   
   @Override
   EndServiceEvent createEndServiceEvent (EndServiceEvent oldEv, AgentGroup group) {
      if (oldEv instanceof EndServiceEventDetailed && group instanceof DetailedAgentGroup) {
         final Agent agent = ((EndServiceEventDetailed)oldEv).getAgent ();
         final DetailedAgentGroup oldGroup = agent.group;
         agent.group = (DetailedAgentGroup)group;
         final EndServiceEventDetailed es = new EndServiceEventDetailed
         (oldEv.getContact().clone(), agent,
            oldEv.getBeginServiceTime());
         agent.es = es;
         agent.group = oldGroup;
         return es;
      }
      else
         return super.createEndServiceEvent (oldEv, group);
   }

   /**
    * Returns the state information for each
    * busy agent in the group at the time of
    * state saving.
    * @return the state information about busy agents.
    */
   public AgentState[] getBusyAgents () {
      return busyAgents;
   }

   /**
    * Returns the state information for the
    * ghost agents in the group, at time of state
    * saving.
    * @return the state information about ghost agents.
    */
   public AgentState[] getGhostAgents () {
      return ghostAgents;
   }

   /**
    * Returns the state information about
    * idle agents in the group, at the time of
    * state saving.
    * @return the state information about idle agents.
    */
   public AgentState[] getIdleAgents () {
      return idleAgents;
   }
}
