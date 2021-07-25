package umontreal.iro.lecuyer.contactcenters.router;

import java.util.HashMap;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueState;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupState;
import umontreal.iro.lecuyer.contactcenters.server.AgentState;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroupState;

/**
 * Represents state information for a router. This information includes the
 * contents of waiting queues, and the contacts served by agents.
 */
public class RouterState {
   private WaitingQueueState[] waitingQueues;
   private AgentGroupState[] agentGroups;
   private final Map<DequeueEvent, ReroutingState> contactReroutingInfo = new HashMap<DequeueEvent, ReroutingState> ();
   private final Map<AgentState, ReroutingState> agentReroutingInfo = new HashMap<AgentState, ReroutingState> ();

   /**
    * Constructs a new state information for a router \texttt{router}.
    * 
    * @param router
    *           the router being processed.
    */
   protected RouterState (Router router) {
      save (router);
   }

   /**
    * Saves the state of the router \texttt{router} into this object. This
    * requires {@link Router#isKeepingReroutingEvents()} to return
    * \texttt{true}, and every agent group must keep track of end-service
    * events.
    * 
    * @param router
    *           the router being processed.
    */
   private void save (Router router) {
      waitingQueues = new WaitingQueueState[router.getNumWaitingQueues ()];
      for (int q = 0; q < waitingQueues.length; q++)
         waitingQueues[q] = router.getWaitingQueue (q).save ();
      agentGroups = new AgentGroupState[router.getNumAgentGroups ()];
      for (int i = 0; i < agentGroups.length; i++)
         agentGroups[i] = router.getAgentGroup (i).save ();

      contactReroutingInfo.clear ();
      agentReroutingInfo.clear ();
      final Map<DequeueEvent, ContactReroutingEvent> rev = router
            .getContactReroutingEvents ();
      if (!rev.isEmpty ())
         for (int q = 0; q < waitingQueues.length; q++) {
            final DequeueEvent[] dqevs = waitingQueues[q].getQueuedContacts ();
            int j = 0;
            for (final DequeueEvent dqev : router.getWaitingQueue (q)) {
               final ContactReroutingEvent ev = rev.get (dqev);
               if (ev != null) {
                  final double time = ev.time ();
                  final int nr = ev.getNumReroutingsDone ();
                  if (time > 0)
                     contactReroutingInfo.put (dqevs[j], new ReroutingState (
                           nr, time));
               }
               ++j;
            }
         }

      final Map<Agent, AgentReroutingEvent> rev2 = router
            .getAgentReroutingEvents ();
      if (!rev2.isEmpty ())
         for (final AgentGroupState element : agentGroups) {
            if (!(element instanceof DetailedAgentGroupState))
               continue;
            final AgentState[] idleAgents = ((DetailedAgentGroupState) element)
                  .getIdleAgents ();
            for (final AgentState agent : idleAgents) {
               final AgentReroutingEvent ev = rev2.get (agent);
               if (ev != null) {
                  final double time = ev.time ();
                  final int nr = ev.getNumReroutingsDone ();
                  if (time > 0)
                     agentReroutingInfo.put (agent, new ReroutingState (nr,
                           time));
               }
            }
         }
   }

   /**
    * Schedules a set of simulation events intended to restore the state of the
    * router at the time it was saved in this object.
    * 
    * @param router
    *           the router being restored.
    */
   void restore (Router router) {
      for (int q = 0; q < waitingQueues.length; q++) {
         final WaitingQueue queue = router.getWaitingQueue (q);
         if (queue == null)
            continue;
         queue.restore (waitingQueues[q]);
         final DequeueEvent[] oldEvs = waitingQueues[q].getQueuedContacts ();
         int j = 0;
         for (final DequeueEvent newEv : queue) {
            final DequeueEvent oldEv = oldEvs[j];
            final ReroutingState rs = contactReroutingInfo.get (oldEv);
            if (rs != null)
               new ContactReroutingEvent (router, newEv, rs
                     .getNumReroutingsDone ()).schedule (rs
                     .getNextReroutingTime ()
                     - newEv.simulator().time ());
            ++j;
         }
      }
      for (int i = 0; i < agentGroups.length; i++) {
         final AgentGroup group = router.getAgentGroup (i);
         if (group == null)
            continue;
         group.restore (agentGroups[i]);
      }
      for (final Map.Entry<AgentState, ReroutingState> e : agentReroutingInfo
            .entrySet ()) {
         final Agent agent = e.getKey ().getAgent ();
         final ReroutingState rs = e.getValue ();
         new AgentReroutingEvent (router, agent, rs.getNumReroutingsDone ())
               .schedule (rs.getNextReroutingTime () - agent.getAgentGroup().simulator().time ());
      }
   }

   /**
    * Returns the state of each agent group saved at the time the state of the
    * router was saved.
    * 
    * @return the state of agent groups.
    */
   public AgentGroupState[] getAgentGroups () {
      return agentGroups;
   }

   /**
    * Returns the agent rerouting information saved at the time the state of the
    * router was saved. Each key of the returned map is of type {@link Agent}
    * while each value is of type {@link ReroutingState}.
    * 
    * @return the agent rerouting information.
    */
   public Map<AgentState, ReroutingState> getAgentReroutingInfo () {
      return agentReroutingInfo;
   }

   /**
    * Returns the contact rerouting information saved at the time the state of
    * the router was saved. Each key of the returned map is of class
    * {@link DequeueEvent} while each value is of class {@link ReroutingState}.
    * 
    * @return the contact rerouting information.
    */
   public Map<DequeueEvent, ReroutingState> getContactReroutingInfo () {
      return contactReroutingInfo;
   }

   /**
    * Returns the state of the waiting queues attached to the router at the time
    * the state of the router was saved.
    * 
    * @return the state of the waiting queues.
    */
   public WaitingQueueState[] getWaitingQueues () {
      return waitingQueues;
   }
}
