package umontreal.iro.lecuyer.contactcenters.expdelay;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;

/**
 * Waiting time predictor using the waiting time
 * of the last contact beginning service as a
 * prediction for the waiting time.
 * This predictor monitors every waiting queue
 * attached to the associated router, and
 * stores the last observed waiting time.
 * This waiting time is returned each time a prediction
 * is requested.
 * One can decide if the collected waiting times
 * include times before abandonment, and
 * time before service, using
 * methods {@link #setCollectingAbandonment(boolean)},
 * and {@link #setCollectingService(boolean)},
 * respectively.
 * By default, only the waiting times before service
 * are collected. 
 */
public class LastWaitingTimePredictor implements WaitingTimePredictor
{
   private final QueueListener ql = new QueueListener();
   private Router router;
   private double lastWaitingTime;
   private boolean collectingAbandonment = false;
   private boolean collectingService = true;

   public Router getRouter ()
   {
      return router;
   }

   public double getWaitingTime (Contact contact)
   {
      return lastWaitingTime;
   }

   public double getWaitingTime (Contact contact, WaitingQueue queue)
   {
      return lastWaitingTime;
   }

   public void init ()
   {
      lastWaitingTime = 0;
   }

   public void setRouter (Router newRouter)
   {
      if (router != null && newRouter != router) {
         final int nq = router.getNumWaitingQueues ();
         for (int q = 0; q < nq; q++) {
            final WaitingQueue queue = router.getWaitingQueue (q);
            if (queue != null)
               queue.removeWaitingQueueListener (ql);
         }
         lastWaitingTime = 0;
      }
      if (newRouter != null && router != newRouter) {
         final int nq = newRouter.getNumWaitingQueues ();
         for (int q = 0; q < nq; q++) {
            final WaitingQueue queue = newRouter.getWaitingQueue (q);
            if (queue != null)
               queue.addWaitingQueueListener (ql);
         }
         lastWaitingTime = 0;
      }
      router = newRouter;
   }

   protected void dequeued (DequeueEvent ev)
   {
      if (ev.getEffectiveDequeueType () == Router.DEQUEUETYPE_BEGINSERVICE) {
         if (collectingService)
            lastWaitingTime = ev.getEffectiveQueueTime ();
      } else if (ev.getEffectiveDequeueType () != Router.DEQUEUETYPE_TRANSFER)
         if (collectingAbandonment)
            lastWaitingTime = ev.getEffectiveQueueTime ();
   }

   private class QueueListener implements WaitingQueueListener
   {
      public void dequeued (DequeueEvent ev)
      {
         LastWaitingTimePredictor.this.dequeued (ev);
      }

      public void enqueued (DequeueEvent ev)
      {}

      public void init (WaitingQueue queue)
      {}
   }

   /**
    * Determines if the collected waiting times
    * for predictions include times before
    * abandonment.
    * By default, this is set to \texttt{false}.
    * @return \texttt{true} if and only if times of abandonment
    * are used for predicting waiting times.
    */
   public boolean isCollectingAbandonment ()
   {
      return collectingAbandonment;
   }

   /**
    * Sets the flag for collecting abandonment to
    * \texttt{collectingAbandonment}.
    * @param collectingAbandonment the new value of the flag.
    * @see #isCollectingAbandonment()
    */
   public void setCollectingAbandonment (boolean collectingAbandonment)
   {
      this.collectingAbandonment = collectingAbandonment;
   }

   /**
    * Determines if the collected waiting times
    * for predictions include times before
    * service.
    * By default, this is set to \texttt{true}.
    * @return \texttt{true} if and only if times of beginning
    * of service
    * are used for predicting waiting times.
    */
   public boolean isCollectingService ()
   {
      return collectingService;
   }

   /**
    * Sets the flag for collecting service to
    * \texttt{collectingService}.
    * @param collectingService the new value of the flag.
    * @see #isCollectingService()
    */
   public void setCollectingService (boolean collectingService)
   {
      this.collectingService = collectingService;
   }
}
