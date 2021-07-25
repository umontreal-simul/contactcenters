package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;

/**
 * Waiting time predictor using the waiting time of the
 * last contact exiting queue $q$ for service as
 * a prediction for the waiting time of a new contact
 * entering queue $q$.
 * This predictor collects the waiting times of
 * contacts, and stores the last waiting
 * time separately for each queue.
 * If a prediction is requested for a specific queue,
 * the last waiting time for that queue is given.
 * If a prediction is requested for any queue,
 * the last waiting time over all queues is given.  
 */
public class LastWaitingTimePerQueuePredictor extends LastWaitingTimePredictor
{
   private double[] lastWaitingTimeQ;

   @Override
   public double getWaitingTime (Contact contact, WaitingQueue queue)
   {
      final int q = queue.getId ();
      return lastWaitingTimeQ[q];
   }

   @Override
   public void init ()
   {
      super.init();
      Arrays.fill (lastWaitingTimeQ, 0);
   }

   @Override
   public void setRouter (Router router)
   {
      super.setRouter (router);
      if (router == null)
         lastWaitingTimeQ = null;
      else
         lastWaitingTimeQ = new double[router.getNumWaitingQueues()];
   }

   @Override
   protected void dequeued (DequeueEvent ev)
   {
      super.dequeued (ev);
      final int q = ev.getWaitingQueue ().getId ();
      if (ev.getEffectiveDequeueType () == Router.DEQUEUETYPE_BEGINSERVICE) {
         if (isCollectingService())
            lastWaitingTimeQ[q] = ev.getEffectiveQueueTime ();
      } else if (isCollectingAbandonment())
         lastWaitingTimeQ[q] = ev.getEffectiveQueueTime ();
   }
}
