package umontreal.iro.lecuyer.contactcenters.ctmc;


/**
 * Represents the type of a transition performed by
 * the {@link CallCenterCTMC#nextState(double)}
 * method.
 */
public enum TransitionType {
   /**
    * Arrival with immediate service.
    */
   ARRIVALSERVED,
   /**
    * Arrival with balking (immediate abandonment).
    */
   ARRIVALBALKED,
   /**
    * Arrival and waiting in queue.
    */
   ARRIVALQUEUED,
   /**
    * Arrival and blocking due to exceeded queue
    * capacity.
    */
   ARRIVALBLOCKED,
   /**
    * An agent terminates a service, and receives
    * a new queued contact to serve.
    */
   ENDSERVICEANDDEQUEUE,
   /**
    * An agent terminates a service, and remains free
    * because of no available queued contacts.
    */
   ENDSERVICENODEQUEUE,
   /**
    * A queued contacts abandons, i.e., leaves
    * the queue without receiving service.
    */
   ABANDONMENT,
   /**
    * Fictious transition not affecting the
    * state of the CTMC.
    */
   FALSETRANSITION
}
