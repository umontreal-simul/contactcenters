package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Parameter indicating how the default
 * agent selection score computed by
 * {@link AgentsPrefRouter#getScoreForAgentSelection(Contact,AgentGroup,Agent)}
 * is computed.
 */
public enum AgentSelectionScore {
   /**
    * The score for an agent group $i$ for contact type $k$ corresponds
    * to the weight $\wTG(k, i)$.
    */
   WEIGHTONLY,
   
   /**
    * The score corresponds to the number of free agents
    * in group $i$ multiplied by the weight
    * $\wTG(k, i)$.
    */
   NUMFREEAGENTS,
   
   /**
    * The score corresponds to the longest idle time
    * of agents in group $i$, multiplied by
    * the weight $\wTG(k, i)$.
    * Using this score if agent groups are not detailed
    * throws an exception.
    */
   LONGESTIDLETIME
}
