package umontreal.iro.lecuyer.contactcenters.router;

/**
 * Possible data structures for waiting queues.
 */
public enum WaitingQueueStructure {
   /**
    * Queued contacts are placed in an ordinary list, in the order
    * they enter the queue.
    * When an agent becomes free, the first contact is
    * usually removed from the queue.
    * This structure therefore implements a FIFO queue.
    * This is the most common, the fastest and is the default structure.
    */
   LIST,

   /**
    * Queued contacts are put into a priority queue, usually implemented
    * using a heap.
    * A comparator is used to sort contacts by priority.
    * A free agent removes the contacts with the highest priority first.
    * However, if a priority queue is scanned, the order of the contacts
    * might not be the order imposed by the comparator.
    * The structure used only guarantees that the first contact is
    * the ``smallest'' with respect to the comparator given.
    */
   PRIORITY,

   /**
    * Queued contacts are put into a sorted set, usually implemented
    * by a binary tree.
    * This is similar to {@link #PRIORITY}, but the contacts in queue
    * can be enumerated in the correct order at any time.
    * However, sorted sets are slower than priority queues.
    */
   SORTEDSET
}
