package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents the end-service event for
 * a detailed agent group.
 */
public class EndServiceEventDetailed extends EndServiceEvent {
   private Agent agent;

   /**
    * Constructs a new end-service event with
    * contact \texttt{contact} served by agent \texttt{agent},
    * with service beginning at
    * \texttt{beginServiceTime}.
    * 
    * This constructor is rarely used directly;
    * the recommended way to create end-service
    * events is to use
    * {@link DetailedAgentGroup#serve(Contact)}.
    * @param contact the contact being served.
    * @param agent the agent serving the contact.
    * @param beginServiceTime the simulation at which the service begins.
    */
   protected EndServiceEventDetailed (Contact contact, Agent agent, double beginServiceTime) {
      super (agent.getAgentGroup (), contact, beginServiceTime);
      this.agent = agent;
   }
   
   @Override
   public DetailedAgentGroup getAgentGroup() {
      return (DetailedAgentGroup)super.getAgentGroup ();
   }

   /**
    * Returns the agent serving or having served
    * the contact.
    @return the serving agent.
    */
   public Agent getAgent() {
      return agent;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      sb.append (", agent: ").append
         (ContactCenter.toShortString (agent));
      sb.append (']');
      return sb.toString();
   }
}
