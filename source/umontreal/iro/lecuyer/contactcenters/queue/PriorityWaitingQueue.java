package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Extends the {@link WaitingQueue} class for a priority waiting queue. The
 * queue uses a {@link SortedSet} to store the dequeue events, and the user can
 * supply a comparator indicating how to order pairs of elements. By default,
 * the sorted set is implemented using a red black tree \cite{iCOR01a} which is
 * a binary tree with automatic balancing for more stable search speed. This
 * class should be used only when there are many priorities in the system,
 * and queued contacts needs to be enumerated
 * in a consistent order. If
 * there are only a few degrees of priorities, it is more efficient to use one
 * standard waiting queue per priority.
 * If contacts do not have to be enumerated,
 * using a heap is more efficient.
 */
public final class PriorityWaitingQueue extends WaitingQueue {
   private SortedSet<DequeueEvent> elements;

   /**
    * Constructs a new waiting queue using a {@link TreeSet} to store
    * the elements. Dequeue events are compared based on their associated
    * contacts, using {@link DequeueEventComparator}.
    */
   public PriorityWaitingQueue () {
      super ();
      elements = new TreeSet<DequeueEvent> (new DequeueEventComparator());
   }

   /**
    * Constructs a new waiting queue using a {@link TreeSet} to store
    * the elements, and the given \texttt{comparator} to determine how to order
    * pairs of elements. The supplied comparator must be able to compare
    * {@link DequeueEvent} objects.
    * 
    * @param comparator
    *           the comparator used to sort the elements.
    */
   public PriorityWaitingQueue (Comparator<? super DequeueEvent> comparator) {
      super ();
      elements = new TreeSet<DequeueEvent> (comparator);
   }

   /**
    * Constructs a new waiting queue using the given {@link java.util.SortedSet}
    * implementation to manage the elements. At any given time, this sorted set
    * contains only {@link DequeueEvent} objects. If no comparator is given,
    * dequeue events are compared based on their associated contacts, using
    * {@link Contact#compareTo}. The given \texttt{set} will be cleared before
    * it is used.
    * 
    * @param set
    *           a sorted set object that will contain the dequeue events.
    */
   public PriorityWaitingQueue (SortedSet<DequeueEvent> set) {
      super ();
      elements = set;
      elements.clear ();
   }

   /**
    * Returns the comparator used to compare the dequeue events, or
    * \texttt{null} if no comparator was given. This method calls
    * {@link SortedSet#comparator} and returns the result.
    * 
    * @return the associated comparator or \texttt{null}.
    */
   public Comparator<? super DequeueEvent> comparator () {
      return elements.comparator ();
   }

   @Override
   protected Iterator<DequeueEvent> elementsIterator () {
      return elements.iterator ();
   }

   @Override
   protected void elementsClear () {
      elements.clear ();
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
      return elements.first ();
   }

   @Override
   protected DequeueEvent elementsGetLast () {
      return elements.last ();
   }

   @Override
   protected DequeueEvent elementsRemoveFirst () {
      final DequeueEvent dqEvent = elements.first ();
      elements.remove (dqEvent);
      return dqEvent;
   }

   @Override
   protected DequeueEvent elementsRemoveLast () {
      final DequeueEvent dqEvent = elements.last ();
      elements.remove (dqEvent);
      return dqEvent;
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (super.toString ());
      sb.deleteCharAt (sb.length () - 1);
      sb.append (", sorted set class: ").append (
            elements.getClass ().getName ());
      if (elements.comparator () != null)
         sb.append (", comparator: ").append (
               elements.comparator ().toString ());
      sb.append (']');
      return sb.toString ();
   }
}
