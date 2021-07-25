package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;

/**
 * Represents a waiting queue using a Java {@link Queue} implementation as a
 * data structure. For example, {@link PriorityQueue} can be used as a queue to
 * have a heap. This can be more efficient than using
 * {@link PriorityWaitingQueue}, which is backed by a {@link SortedSet}, but
 * {@link #getLast} and {@link #removeLast} are not supported, because
 * {@link Queue} does not provide any method for getting or removing the last
 * element.
 * Moreover, the iterator does not enumerate
 * queued contacts in a particular order.
 */
public class QueueWaitingQueue extends WaitingQueue {
   private Queue<DequeueEvent> queue;

   /**
    * Constructs a waiting queue using a priority heap with {@link DequeueEventComparator} for
    * dequeue event.
    */
   public QueueWaitingQueue () {
      queue = new PriorityQueue<DequeueEvent> (5, new DequeueEventComparator());
   }

   /**
    * Constructs a new waiting queue using a priority heap with the comparator
    * \texttt{comparator}.
    * 
    * @param comparator
    *           the comparator used to compare events.
    */
   public QueueWaitingQueue (Comparator<? super DequeueEvent> comparator) {
      queue = new PriorityQueue<DequeueEvent> (5, comparator);
   }

   /**
    * Constructs a new waiting queue using the queue \texttt{queue} as a data
    * structure. The given waiting queue can only contain dequeue events, and is
    * cleared before usage.
    * 
    * @param queue
    *           the queue being used.
    * @exception NullPointerException
    *               if \texttt{queue} is \texttt{null}.
    */
   public QueueWaitingQueue (Queue<DequeueEvent> queue) {
      super ();
      if (queue == null)
         throw new NullPointerException ("The given queue must not be null");
      queue.clear ();
      this.queue = queue;
   }

   @Override
   protected void elementsAdd (DequeueEvent dqEvent) {
      queue.add (dqEvent);
   }

   @Override
   protected void elementsClear () {
      queue.clear ();
   }

   @Override
   protected DequeueEvent elementsGetFirst () {
      return queue.element ();
   }

   @Override
   protected DequeueEvent elementsGetLast () {
      throw new UnsupportedOperationException ();
   }

   @Override
   protected boolean elementsIsEmpty () {
      return queue.isEmpty ();
   }

   @Override
   protected Iterator<DequeueEvent> elementsIterator () {
      return queue.iterator ();
   }

   @Override
   protected DequeueEvent elementsRemoveFirst () {
      return queue.remove ();
   }

   @Override
   protected DequeueEvent elementsRemoveLast () {
      throw new UnsupportedOperationException ();
   }
}
