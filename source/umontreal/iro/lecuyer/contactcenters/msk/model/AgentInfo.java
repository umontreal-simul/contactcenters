package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.msk.params.AgentParams;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.xmlbind.NamedInfo;

/**
 * Encapsulates the information concerning
 * a specific agent in a call center model.
 */
public class AgentInfo extends NamedInfo {
   private final Agent agent = new Agent();
   private ScheduleShift shift;
   
   /**
    * Constructs a new agent information object using
    * the call center model \texttt{cc}, and
    * the agent parameters \texttt{par}.
    * @param cc the call center model.
    * @param par the agent parameters.
    */
   public AgentInfo (CallCenter cc, AgentParams par) {
      super (par);
      shift = new ScheduleShift (cc, par.getShift());
   }
   
   /**
    * Returns the agent associated with this object.
    * @return the associated agent.
    */
   public Agent getAgent() {
      return agent;
   }
   
   /**
    * Returns an object representing the shift
    * of the agent associated with this
    * object.
    * @return the shift of this agent.
    */
   public ScheduleShift getShift() {
      return shift;
   }
}
