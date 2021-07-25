package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents the state of a waiting queue. For now, this state is represented
 * by an array of dequeue events representing queued contacts.
 */
public class WaitingQueueState {
   private DequeueEvent[] queuedContacts;

   /**
    * Constructs a new state object by saving the state of the waiting queue
    * \texttt{queue}.
    * 
    * @param queue
    *           the queue to be saved.
    */
   protected WaitingQueueState (WaitingQueue queue) {
      save (queue);
   }

   /**
    * Saves the state of the waiting queue \texttt{queue}.
    * 
    * @param queue
    *           the waiting queue to be saved.
    */
   private void save (WaitingQueue queue) {
      queuedContacts = new DequeueEvent[queue.size ()];
      int idx = 0;
      for (final DequeueEvent ev : queue)
         queuedContacts[idx++] = ev.clone ();
   }

   /**
    * Restores the state of the waiting queue. This method first calls
    * {@link WaitingQueue#init} to reset the waiting queue. Then, for each saved
    * dequeue event, it schedules an {@link EnqueueEvent} that will use
    * {@link WaitingQueue#add(Contact,double,int)} to add the contact in the
    * waiting queue.
    * 
    * @param queue
    *           the queue to be restored.
    */
   void restore (WaitingQueue queue) {
      queue.init ();
      queue.queueSize = queuedContacts.length;
      Arrays.fill (queue.queueSizeK, 0);
      for (final DequeueEvent oldEv : queuedContacts) {
         // new EnqueueEvent (queue, queuedContacts[j]).schedule();
         final double enqueueTime = oldEv.getEnqueueTime ();
         final double qTime = oldEv.getScheduledQueueTime ();
         final DequeueEvent newEv = new DequeueEvent (queue, oldEv
               .getContact ().clone (), enqueueTime);
         newEv.qTime = qTime;
         newEv.dqType = oldEv.getScheduledDequeueType ();
         if (!Double.isInfinite (qTime) && !Double.isNaN (qTime))
            newEv.schedule (qTime + enqueueTime - newEv.simulator().time ());
         queue.elementsAdd (newEv);
         newEv.getContact ().enqueued (newEv);
         final int k = newEv.getContact ().getTypeId ();
         if (k >= 0) {
            if (k >= queue.queueSizeK.length)
               queue.queueSizeK = ArrayUtil.resizeArray (queue.queueSizeK, k + 1);
            ++queue.queueSizeK[k];
         }
      }
   }

   /**
    * Returns the array containing the queued contacts in the queue at the time
    * the state was saved.
    * 
    * @return the state of the queued contacts.
    */
   public DequeueEvent[] getQueuedContacts () {
      return queuedContacts;
   }
}
