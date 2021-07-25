package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.simevents.Event;

/**
 * Represents an event happening when the router tries to reroute a queued
 * contact to an agent, or another queue.
 */
public final class ContactReroutingEvent extends Event {
   private final Router router;
   private DequeueEvent dqEv;
   private int numReroutingsDone;

   /**
    * Constructs an event that will reroute the queued contact \texttt{dqEv}
    * to an agent or another queue.
    * 
    * @param router the router this event is linked to.
    * @param dqEv
    *           the dequeue event.
    * @param numReroutingsDone
    *           the number of reroutings done.
    */
   public ContactReroutingEvent (Router router, DequeueEvent dqEv,
         int numReroutingsDone) {
      super (dqEv.simulator());
      this.router = router;
      this.dqEv = dqEv;
      this.numReroutingsDone = numReroutingsDone;
   }
   
   /**
    * Returns the router associated with this
    * event.
    * @return the associated router.
    */
   public Router getRouter() {
      return router;
   }

   /**
    * Returns the dequeue event associated with this rerouting event.
    * 
    * @return the associated dequeue event.
    */
   public DequeueEvent getDequeueEvent () {
      return dqEv;
   }

   /**
    * Returns the number of reroutings done, i.e., the number of calls
    * to {@link #actions} having resulted in the contact not being
    * transferred to an agent.
    * 
    * @return the number of reroutings that has happened.
    */
   public int getNumReroutingsDone () {
      return numReroutingsDone;
   }
   
   @Override
   public boolean cancel () {
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.remove (dqEv);
      --router.numContactReroutingEvents;
      return super.cancel ();
   }

   @Override
   public void schedule (double delay) {
      super.schedule (delay);
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.put (dqEv, this);
      ++router.numContactReroutingEvents;
   }

   @Override
   public void scheduleAfter (Event other) {
      super.scheduleAfter (other);
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.put (dqEv, this);
      ++router.numContactReroutingEvents;
   }

   @Override
   public void scheduleBefore (Event other) {
      super.scheduleBefore (other);
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.put (dqEv, this);
      ++router.numContactReroutingEvents;
   }

   @Override
   public void scheduleNext () {
      super.scheduleNext ();
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.put (dqEv, this);
      ++router.numContactReroutingEvents;
   }

   public boolean isObsolete() {
      return dqEv.isObsolete() || dqEv.dequeued ();
   }

   @Override
   public void actions () {
      if (isObsolete()) {
         if (router.contactReroutingEvents != null)
            router.contactReroutingEvents.remove (dqEv);
         --router.numContactReroutingEvents;
         return;
      }
      final EndServiceEvent es = router.selectAgent (dqEv, numReroutingsDone);
      if (es == null) {
         router.dqTypeRet = 1;
         final DequeueEvent ev = router.selectWaitingQueue (dqEv,
               numReroutingsDone);
//         assert ev == null || !ev.dequeued ();  // may fail with routing policy OVERFLOWANDPRIORITY when a rank goes to infinity.
         if (ev != dqEv) {
            if (!dqEv.dequeued ())
               dqEv.getWaitingQueue ().remove (dqEv, router.dqTypeRet);
            if (router.contactReroutingEvents != null) {
               router.contactReroutingEvents.remove (dqEv);
               if (ev != null)
                  router.contactReroutingEvents.put (ev, this);
               else
                  --router.numContactReroutingEvents;
            }
            dqEv = ev;
            if (ev == null)
               return;
         }
         if (ev != null) {
            final double delay = router.getReroutingDelay (dqEv, numReroutingsDone);
            if (delay >= 0 && !Double.isInfinite (delay)
                  && !Double.isNaN (delay)) {
               ++numReroutingsDone;
               super.schedule (delay);
               return;
            }
         }
      }
      else
         dqEv.getWaitingQueue ().remove (dqEv, Router.DEQUEUETYPE_BEGINSERVICE);
      if (router.contactReroutingEvents != null)
         router.contactReroutingEvents.remove (dqEv);
      --router.numContactReroutingEvents;
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append ("Dequeue event: ").append (dqEv.toString ());
      sb.append (", number of reroutings done: ").append (numReroutingsDone);
      sb.append (']');
      return sb.toString ();
   }
}
