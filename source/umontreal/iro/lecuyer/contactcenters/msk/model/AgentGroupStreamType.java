package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * Types of random streams for agent groups.
 */
public enum AgentGroupStreamType {
   /**
    * Random stream for probability that an agent
    * disconnects after some event occurs.
    */
   DISCONNECTTEST,
   /**
    * Random stream for the time an agent
    * remains offline after it disconnects.
    */
   DISCONNECTTIME
}
