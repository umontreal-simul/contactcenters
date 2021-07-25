/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.ssj.simevents.Event;

/**
 * Represents an event occuring when a disconnected agent becomes available
 * again.
 */
public final class MakeAgentAvailableEvent extends Event {
   private final CallCenter model;
   private Agent agent;

   /**
    * Constructs an event making the agent \texttt{agent} in the model
    * \texttt{model} available when it occurs.
    * 
    * @param model
    *           the model the agent belongs to.
    * @param agent
    *           the agent that will be made available.
    */
   public MakeAgentAvailableEvent (CallCenter model, Agent agent) {
      super (model.simulator ());
      if (model == null || agent == null)
         throw new NullPointerException ();
      this.model = model;
      this.agent = agent;
   }

   /**
    * Returns the model associated with this event.
    * 
    * @return the associated model.
    */
   public CallCenter getCallCenter () {
      return model;
   }

   /**
    * Returns the agent associated with this event.
    * 
    * @return the associated agent.
    */
   public Agent getAgent () {
      return agent;
   }

   @Override
   public void actions () {
      agent.setAvailable (true);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append ("agent: ").append (agent.toString ());
      sb.append (']');
      return sb.toString ();
   }
}
