package umontreal.iro.lecuyer.contactcenters.contact;

/**
 * Defines a new-contact listener that receives incoming contacts
 * for further processing.
 * This interface is mainly used to link the contact arrival processes
 * to routers or dialer lists.
 * It can also be used for counting the number of arrivals,
 * for statistical collecting.
 */
public interface NewContactListener {
   /**
    * Notifies the listener about a new contact \texttt{contact}.
    * The given contact object can be assumed non-\texttt{null},
    * and may be stored or processed in any needed ways.
    @param contact the new contact.
    */
   public void newContact (Contact contact);
}
