package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Parameter indicating how the default
 * contact selection score computed by
 * {@link AgentsPrefRouter#getScoreForContactSelection(AgentGroup,DequeueEvent)}
 * is computed.
 */
public enum ContactSelectionScore {
   /**
    * The score for an agent group $i$ for contact type $k$ corresponds
    * to the weight $\wGT(i, k)$.
    */
   WEIGHTONLY,
   
   /**
    * The score corresponds to the
    * number of contacts in queue
    * multiplied by $\wGT(i, k)$. 
    */
   QUEUESIZE,
   
   /**
    * The score corresponds to the
    * waiting time of the queued contact
    * of type $k$ multiplied by the
    * weight $\wGT(i, k)$.
    */
   LONGESTWAITINGTIME
}
