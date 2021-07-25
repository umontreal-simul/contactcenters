package umontreal.iro.lecuyer.contactcenters.router;


/**
 * Represent possible roles of waiting queues
 * for routing policies. 
 */
public enum WaitingQueueType {
   /**
    * When this type is used, there must be one
    * waiting queue for each contact type.
    * More specifically, waiting queue $k$,
    * for $k=0,\ldots,K-1$, contains only
    * contacts of type $k$, where $K$
    * is the total number of contact types.
    */
   CONTACTTYPE,
   
   /**
    * When this type is used, there must be one
    * waiting queue for each agent group.
    * More specifically, waiting queue $i$,
    * for $i=0,\ldots,I-1$, contains only
    * contacts that are to be served by agents
    * in group $i$, where $I$ is the
    * total number of agent groups.
    */
   AGENTGROUP,
   
   /**
    * Used when the waiting queues do not correspond
    * to the schemes described by
    * {@link #CONTACTTYPE} or
    * {@link #AGENTGROUP}.
    */
   GENERAL
}
