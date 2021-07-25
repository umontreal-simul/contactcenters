package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Comparator;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Default comparator used to sort dequeue events in a priority queue.
 * The default order for {@link DequeueEvent} is given by the
 * {@link Comparable#compareTo(Object)} method, which
 * sorts events according to time of occurrence, i.e., dequeue time.
 * This is adapted for inserting dequeue events in the event list of the
 * simulator, not in a waiting queue.
 * This comparator can be used when the waiting queue needs a comparator
 * to establish the order of the elements.
 * This comparator is not needed for waiting queues using
 * a list, i.e., {@link StandardWaitingQueue}.
 */
public class DequeueEventComparator implements Comparator<DequeueEvent> {

   /**
    * Compares dequeue event \texttt{e1} with the other event \texttt{e2}.
    * The method
    * extracts the {@link Contact} objects from the events.
    * The {@link Contact#compareTo(Contact)} method is then used to
    * compare objects. A contact that cannot be extracted is assigned the
    * \texttt{null} value and precedes any non-\texttt{null} contacts.
    *
    * @param e1
    * the first event.
    * @param e2
    * the second event.
    * @return the result of the comparison.
    */
   public int compare (DequeueEvent e1, DequeueEvent e2) {
      final Contact c1 = e1 == null ? null : e1.getContact ();
      final Contact c2 = e2 == null ? null : e2.getContact ();
      if (c1 == null && c2 == null)
         return 0;
      else if (c1 == null)
         return -1;
      else if (c2 == null)
         return 1;
      return c1.compareTo (c2);
   }
}
