package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.EnqueueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;

/**
 * Represents an event that queues a contact, and schedules an additional event
 * for supporting rerouting. This event is the same as the event represented by
 * the superclass {@link EnqueueEvent}, except that the dequeue event obtained
 * by adding the contact into the waiting queue is used to construct a
 * {@link ContactReroutingEvent} based on stored information.
 */
public class EnqueueEventWithRerouting extends EnqueueEvent {
   private Router targetRouter;
   private int numReroutingsDone;
   private double nextReroutingTime;

   /**
    * Constructs a new dequeue event with rerouting
    * from the old dequeue event \texttt{oldDequeueEvent},
    * the target router \texttt{targetRouter},
    * and the rerouting state \texttt{reroutingState}.
    * This calls {@link EnqueueEvent#EnqueueEvent(DequeueEvent) super}
    * \texttt{(oldDequeueEvent)} and sets
    * the target router and rerouting information.
    * @param oldDequeueEvent the old dequeue event.
    * @param targetRouter the target router.
    * @param reroutingState the rerouting state.
    */
   public EnqueueEventWithRerouting (DequeueEvent oldDequeueEvent,
         Router targetRouter, ReroutingState reroutingState) {
      super (oldDequeueEvent);
      if (targetRouter == null || reroutingState == null)
         throw new NullPointerException();
      this.targetRouter = targetRouter;
      numReroutingsDone = reroutingState.getNumReroutingsDone ();
      nextReroutingTime = reroutingState.getNextReroutingTime ();
   }

   /**
    * Constructs a new enqueue event with rerouting from
    * the target waiting queue \texttt{targetQueue},
    * the old dequeue event \texttt{oldDequeueEvent},
    * the target router \texttt{targetRouter},
    * and the rerouting state information \texttt{reroutingState}.
    * This calls {@link EnqueueEvent#EnqueueEvent(WaitingQueue,DequeueEvent) super}
    * \texttt{(targetQueue, oldDequeueEvent)} and sets
    * the target router and rerouting information.
    * @param targetQueue the target waiting queue.
    * @param oldDequeueEvent the old dequeue event.
    * @param targetRouter the target router.
    * @param reroutingState the rerouting information.
    */
   public EnqueueEventWithRerouting (WaitingQueue targetQueue,
         DequeueEvent oldDequeueEvent, Router targetRouter, ReroutingState reroutingState) {
      super (targetQueue, oldDequeueEvent);
      this.targetRouter = targetRouter;
      numReroutingsDone = reroutingState.getNumReroutingsDone ();
      nextReroutingTime = reroutingState.getNextReroutingTime ();
   }

   /**
    * Constructs a new enqueue event with rerouting from
    * the target waiting queue \texttt{targetQueue},
    * queueing information, and rerouting information.
    * This calls 
    * {@link EnqueueEvent#EnqueueEvent(WaitingQueue,Contact,double,int) super}
    * \texttt{(targetQueue, contact, queueTime, dqType)} and sets
    * the target router and rerouting information.
    * @param targetQueue the target waiting queue.
    * @param contact the contact being queued.
    * @param queueTime the maximal queue time.
    * @param dqType the dequeue type.
    * @param targetRouter the target router.
    * @param numReroutingsDone the number of times the contact or agent has been rerouted before.
    * @param nextReroutingTime the simulation of the next rerouting.
    */
   public EnqueueEventWithRerouting (WaitingQueue targetQueue,
         Contact contact, double queueTime, int dqType,
         Router targetRouter, int numReroutingsDone, double nextReroutingTime) {
      super (targetQueue, contact, queueTime, dqType);
      this.targetRouter = targetRouter;
      this.numReroutingsDone = numReroutingsDone;
      this.nextReroutingTime = nextReroutingTime;
   }

   /**
    * Returns the target router for this event,
    * i.e., the router for which the rerouting
    * event will be scheduled.
    * @return the target router.
    */
   public Router getTargetRouter () {
      return targetRouter;
   }

   /**
    * Returns the simulation time at which the router
    * will try to reroute the contact or the agent.
    * @return the next rerouting time.
    */
   public double getNextReroutingTime () {
      return nextReroutingTime;
   }

   /**
    * Returns the number of reroutings that has
    * happened so far for the contact or agent.
    * @return the number of preceding reroutings.
    */
   public int getNumReroutingsDone () {
      return numReroutingsDone;
   }

   @Override
   public void actions () {
      super.actions ();
      final DequeueEvent dqev = getNewDequeueEvent ();
      new ContactReroutingEvent (targetRouter, dqev,
            numReroutingsDone).schedule (
            nextReroutingTime
            - simulator().time ());
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (super.toString ());
      sb.deleteCharAt (sb.length () - 1);
      sb.append (", target router: ").append (targetRouter.toString ());
      sb.append (", number of reroutings done before: ").append (numReroutingsDone);
      sb.append (", simulation time of the next rerouting: ").append (nextReroutingTime);
      sb.append (']');
      return sb.toString ();
   }
}
