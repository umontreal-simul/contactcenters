package umontreal.iro.lecuyer.contactcenters.expdelay;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;

/**
 * Head of queue waiting time predictor.
 * This predictor obtains a waiting time by taking
 * the longest waiting time among the first queued
 * contacts of the associated router.
 * When waiting queues are first in first out (FIFO),
 * this corresponds to the longest waiting time among
 * all queued contacts.
 * The waiting time of a queued contact is
 * the time from which the contact entered the queue
 * to the current time.   
 */
public class HeadOfQueuePredictor implements WaitingTimePredictor
{
   private Router router;

   public Router getRouter ()
   {
      return router;
   }

   public double getWaitingTime (Contact contact)
   {
      final int nq = router.getNumWaitingQueues ();
      double d = 0;
      for (int q = 0; q < nq; q++) {
         final WaitingQueue queue2 = router.getWaitingQueue (q);
         if (queue2 == null || queue2.isEmpty ())
            continue;
         final DequeueEvent firstEv = queue2.getFirst ();
         final double t = firstEv.simulator ().time () - firstEv.getEnqueueTime ();
         if (t > d)
            d = t;
      }
      return d;
   }

   public double getWaitingTime (Contact contact, WaitingQueue queue)
   {
      if (queue.isEmpty ())
         return 0;
      final DequeueEvent firstEv = queue.getFirst ();
      return firstEv.simulator ().time () - firstEv.getEnqueueTime ();
   }

   public void init ()
{}

   public void setRouter (Router router)
   {
      this.router = router;
   }
}
