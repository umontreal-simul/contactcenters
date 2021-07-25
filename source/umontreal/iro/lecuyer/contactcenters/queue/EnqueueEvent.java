package umontreal.iro.lecuyer.contactcenters.queue;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.simevents.Event;

/**
 * Represents a simulation event that will put a queued contact back in its
 * original waiting queue. This is used for state restoration of a waiting
 * queue.
 */
public class EnqueueEvent extends Event {
   private WaitingQueue targetQueue;
   private Contact contact;
   private double queueTime;
   private int dqType;
   private DequeueEvent newDequeueEvent;

   /**
    * Constructs a new enqueue event from an old dequeue event using the target
    * queue returned by {@link DequeueEvent#getWaitingQueue()}.
    *
    * @param oldDequeueEvent
    *           the old dequeue event to be used.
    */
   public EnqueueEvent (DequeueEvent oldDequeueEvent) {
      this (oldDequeueEvent.getWaitingQueue (), oldDequeueEvent);
   }

   /**
    * Constructs a new enqueue event from an old dequeue event that will put a
    * queued contact into the target waiting queue \texttt{targetQueue}.
    *
    * @param targetQueue
    *           the target waiting queue.
    * @param oldDequeueEvent
    *           the old dequeue event to be used.
    */
   public EnqueueEvent (WaitingQueue targetQueue, DequeueEvent oldDequeueEvent) {
      this (targetQueue, oldDequeueEvent.getContact (), oldDequeueEvent
            .getScheduledQueueTime (), oldDequeueEvent
            .getScheduledDequeueType ());
      if (oldDequeueEvent.dequeued ())
         throw new IllegalArgumentException (
               "Cannot use an old dequeue event representing a dequeued contact");
   }

   /**
    * Constructs a new enqueue event that will put a contact
    * \texttt{contact} into the target waiting queue \texttt{targetQueue}. The
    * maximal queue time of the contact will be \texttt{queueTime} while its
    * dequeue type is \texttt{dqType}.
    *
    * @param targetQueue
    *           the target waiting queue.
    * @param contact
    *           the contact being queued.
    * @param queueTime
    *           the maximal queue time.
    * @param dqType
    *           the dequeue type.
    * @exception NullPointerException
    *               if \texttt{contact} or \texttt{targetQueue} are
    *               \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{queueTime} is negative.
    */
   public EnqueueEvent (WaitingQueue targetQueue, Contact contact,
         double queueTime, int dqType) {
      super (contact.simulator());
      if (targetQueue == null || contact == null)
         throw new NullPointerException ();
      if (queueTime < 0)
         throw new IllegalArgumentException ("queueTime < 0");
      this.targetQueue = targetQueue;
      this.contact = contact;
      this.queueTime = queueTime;
      this.dqType = dqType;
   }

   /**
    * Returns the waiting queue in which the previously queued contact will be
    * added by this event.
    *
    * @return the target waiting queue.
    */
   public WaitingQueue getTargetWaitingQueue () {
      return targetQueue;
   }

   /**
    * Returns the contact to be queued when the event occurs.
    *
    * @return the contact being queued.
    */
   public Contact getContact () {
      return contact;
   }

   /**
    * Returns the scheduled maximal queue time assigned to the contact when it
    * is queued.
    *
    * @return the scheduled maximal queue time.
    */
   public double getScheduledQueueTime () {
      return queueTime;
   }

   /**
    * Returns the scheduled dequeue type assigned to the contact when it is
    * queued.
    *
    * @return the scheduled dequeue type.
    */
   public int getScheduledDequeueType () {
      return dqType;
   }

   /**
    * Returns the dequeue event representing the contact put back in the waiting
    * queue. This returns a non-\texttt{null} value only after the execution of
    * the {@link #actions} method.
    *
    * @return the new dequeue event.
    */
   public DequeueEvent getNewDequeueEvent () {
      return newDequeueEvent;
   }

   @Override
   public void actions () {
      newDequeueEvent = targetQueue.add (contact, contact.simulator ().time (), queueTime, dqType);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append ("Target waiting queue: ").append (
            ContactCenter.toShortString (targetQueue));
      sb.append (", queued contact: ").append (
            ContactCenter.toShortString (contact));
      sb.append (", scheduled maximal queue time: ").append (queueTime);
      sb.append (", scheduled dequeue type: ").append (dqType);
      sb.append (']');
      return sb.toString ();
   }
}
