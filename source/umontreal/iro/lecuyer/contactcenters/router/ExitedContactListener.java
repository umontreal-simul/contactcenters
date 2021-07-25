package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

/**
 * Represents an exited-contact listener which
 * gets notified when a contact exits the system.
 * A contact can leave the center when it is served, dequeued or blocked.
 */
public interface ExitedContactListener {
   /**
    * This method is called when the contact \texttt{contact}
    * is blocked in the router \texttt{router}.  The integer
    * \texttt{bType} is used to indicate the reason of the blocking, e.g.,
    * the contact could
    * not be served or put into any waiting queue.
    @param router the router causing the blocking.
    @param contact the blocked contact.
    @param bType an indicator giving the reason why the contact is blocked.
   */
   public void blocked (Router router, Contact contact, int bType);

   /**
    * This method is called when a contact
    * leaves a waiting queue linked to the
    * router \texttt{router}, without being served.
    @param router the router causing the dequeueing.
    @param ev the dequeue event.
   */
   public void dequeued (Router router,
                         DequeueEvent ev);

   /**
    * This method is called when a contact
    * was served by an agent.
    * This method is called by the router before
    * the after-contact work begins
    * so \texttt{ev} does not contain the
    * information about after-contact time.
    @param router the router managing the contact.
    @param ev the end service event.
   */
   public void served (Router router,
                       EndServiceEvent ev);
}
