package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * This wrapper dialer list is used by dialers dropping mismatches. It uses a
 * regular dialer list, and resets the patience time of all created contacts
 * to 0. As a result, contacts that cannot be served immediately (mismatches)
 * leave the system without waiting in queue.
 */
public class DialerListNoQueueing implements DialerList {
   private DialerList list;

   /**
    * Constructs a new dialer list with no queueing
    * by using the inner list \texttt{list}.
    * @param list the inner dialer list.
    */
   public DialerListNoQueueing (DialerList list) {
      this.list = list;
   }
   
   /**
    * Returns a reference to the internal dialer list.
    * @return a reference to the internal dialer list.
    */
   public DialerList getDialerList() {
      return list;
   }

   public void clear () {
      list.clear ();
   }

   public Contact removeFirst (int[] contactTypes) {
      final Contact contact = list.removeFirst (null);
      contact.setDefaultPatienceTime (0);
      return contact;
   }

   public int size (int[] contactTypes) {
      return list.size (null);
   }

   public Contact newInstance () {
      final Contact contact = list.newInstance ();
      contact.setDefaultPatienceTime (0);
      return contact;
   }
}