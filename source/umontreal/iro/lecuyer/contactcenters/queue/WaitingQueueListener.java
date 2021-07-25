package umontreal.iro.lecuyer.contactcenters.queue;

/**
 * Represents a waiting-queue listener which can be notified about events
 * concerning waiting queues. When an implementation is registered to a waiting
 * queue, it is notified when contacts are enqueued and dequeued, or when the
 * queue is initialized.
 */
public interface WaitingQueueListener {
   /**
    * This method is called after a contact was added to a queue. The event
    * \texttt{ev} can be used to access the available information about the
    * queued contact. When this is called, it should be possible to use the
    * waiting-queue iterator to find the contact in the queue. However, if the
    * contact is immediately dequeued, it can be absent from the queue.
    * 
    * @param ev
    *           the dequeue event associated with the queued contact.
    */
   public void enqueued (DequeueEvent ev);

   /*
    * Calling enqueued for every queued contact can simplify statistical
    * counters, like QueueSizeStat, which keeps track of the number of contacts
    * in the queue of each type. If we do not call this upon an immediate
    * dequeue, the listeners will have to figure out if this is an immediate
    * exit of queue or not by comparing the queue time to 0.
    */

   /**
    * This method is called when a contact is removed from a waiting queue,
    * \texttt{ev} representing the corresponding dequeue event.
    * 
    * @param ev
    *           the obsolete dequeue event.
    */
   public void dequeued (DequeueEvent ev);

   /**
    * This method is called after the {@link WaitingQueue#init()} method is called
    * for the waiting queue \texttt{queue}.
    * 
    * @param queue
    *           the queue being initialized.
    */
   public void init (WaitingQueue queue);
}
