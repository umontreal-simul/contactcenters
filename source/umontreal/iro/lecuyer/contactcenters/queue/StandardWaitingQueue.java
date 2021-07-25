package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Extends the {@link WaitingQueue} class for a standard waiting queue, without
 * priority. The queue uses a {@link List} to store the dequeue events ordered
 * by insertion times. By default, a doubly-linked list is used, which
 * implements insertion and removal of the first and last elements in constant
 * time.
 */
public final class StandardWaitingQueue extends WaitingQueue {
   private List<DequeueEvent> elements;

   /**
    * Constructs a new waiting queue using a {@link java.util.LinkedList} to
    * store the elements.
    */
   public StandardWaitingQueue () {
      super ();
      elements = new LinkedList<DequeueEvent> ();
   }

   /**
    * Constructs a new waiting queue using the given {@link java.util.List}
    * implementation to manage the elements. This list must contain only
    * {@link DequeueEvent} objects and it will be cleared before being used.
    *
    * @param list
    *           the list containing the queued contacts.
    */
   public StandardWaitingQueue (List<DequeueEvent> list) {
      super ();
      elements = list;
      elements.clear ();
   }

   @Override
   protected void elementsClear () {
      elements.clear ();
   }

   @Override
   protected Iterator<DequeueEvent> elementsIterator () {
      return elements.iterator ();
   }

   @Override
   protected void elementsAdd (DequeueEvent dqEvent) {
      elements.add (dqEvent);
   }

   @Override
   protected boolean elementsIsEmpty () {
      return elements.isEmpty ();
   }

   @Override
   protected DequeueEvent elementsGetFirst () {
      if (elements.isEmpty ())
         throw new NoSuchElementException ("The waiting queue is empty");
      // The get method throws an IndexOutOfBoundsException
      // if the list is empty, but the interface with
      // WaitingQueue specifies a NoSuchElementException.
      return elements.get (0);
   }

   @Override
   protected DequeueEvent elementsGetLast () {
      if (elements.isEmpty ())
         throw new NoSuchElementException ("The waiting queue is empty");
      return elements.get (elements.size () - 1);
   }

   @Override
   protected DequeueEvent elementsRemoveFirst () {
      if (elements.isEmpty ())
         throw new NoSuchElementException ("The waiting queue is empty");
      return elements.remove (0);
   }

   @Override
   protected DequeueEvent elementsRemoveLast () {
      if (elements.isEmpty ())
         throw new NoSuchElementException ("The waiting queue is empty");
      return elements.remove (elements.size () - 1);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (super.toString ());
      sb.deleteCharAt (sb.length () - 1);
      sb.append (", list class: ").append (elements.getClass ().getName ());
      sb.append (']');
      return sb.toString ();
   }
}
