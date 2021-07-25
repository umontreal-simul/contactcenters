package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEventDetailed;
import umontreal.ssj.simevents.Event;

/**
 * Represents an event happening when the router tries once more to affect a
 * contact to an agent.
 */
public final class AgentReroutingEvent extends Event {
   private final Router router;
   private Agent agent;
   private double idleSimTime;
   private int numReroutingsDone;

   /**
    * Constructs a new agent rerouting event instructing
    * the router \texttt{router} to try to find a queued contact
    * for the idle agent \texttt{agent} after
    * there was \texttt{numReroutingsDone}
    * preceding reroutings.
    * @param router the router to be used.
    * @param agent the agent to be rerouted.
    * @param numReroutingsDone the number of preceding trials.
    */
   public AgentReroutingEvent (Router router, Agent agent, int numReroutingsDone) {
      super (agent.getAgentGroup().simulator());
      if (router == null)
         throw new NullPointerException();
      this.router = router;
      this.agent = agent;
      this.numReroutingsDone = numReroutingsDone;
      idleSimTime = agent.getIdleSimTime ();
   }

   /**
    * Returns the router associated with this
    * event.
    * @return the associated router.
    */
   public Router getRouter() {
      return router;
   }

   /**
    * Returns the agent to be assigned a queued contact.
    * @return the agent to be assigned a queued contact.
    */
   public Agent getAgent() {
      return agent;
   }

   /**
    * Returns the number of preceding reroutings.
    * @return the number of reroutings already tried.
    */
   public int getNumReroutingsDone() {
      return numReroutingsDone;
   }

   @Override
   public boolean cancel () {
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.remove (agent);
      --router.numAgentReroutingEvents;
      return super.cancel ();
   }

   @Override
   public void schedule (double delay) {
      super.schedule (delay);
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.put (agent, this);
      ++router.numAgentReroutingEvents;
   }

   @Override
   public void scheduleAfter (Event other) {
      super.scheduleAfter (other);
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.put (agent, this);
      ++router.numAgentReroutingEvents;
   }

   @Override
   public void scheduleBefore (Event other) {
      super.scheduleBefore (other);
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.put (agent, this);
      ++router.numAgentReroutingEvents;
   }

   @Override
   public void scheduleNext () {
      super.scheduleNext ();
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.put (agent, this);
      ++router.numAgentReroutingEvents;
   }

   public boolean isObsolete() {
      return agent.isBusy () || agent.getIdleSimTime () != idleSimTime;
   }

   @Override
   public void actions () {
      if (isObsolete()) {
         if (router.agentReroutingEvents != null)
            router.agentReroutingEvents.remove (agent);
         --router.numAgentReroutingEvents;
         return;
      }
      final DequeueEvent ev = router.selectContact (agent, numReroutingsDone);
      if (ev != null) {
         assert ev.dequeued ();
         final EndServiceEventDetailed es = agent.serve (ev.getContact ());
         assert es.getAgent () == agent;
         assert es.getContact () == ev.getContact ();
      }
      else {
         final double delay = router.getReroutingDelay (agent, numReroutingsDone);
         if (delay >= 0 && !Double.isInfinite (delay)
               && !Double.isNaN (delay)) {
            ++numReroutingsDone;
            super.schedule (delay);
            return;
         }
      }
      if (router.agentReroutingEvents != null)
         router.agentReroutingEvents.remove (agent);
      --router.numAgentReroutingEvents;
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append ("Agent: ").append (agent);
      sb.append (", number of preceding reroutings: ")
            .append (numReroutingsDone);
      sb.append (']');
      return sb.toString ();
   }
}