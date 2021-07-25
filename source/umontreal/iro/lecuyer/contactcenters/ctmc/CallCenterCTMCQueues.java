package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

/**
 * Provides helper method used to maintain information
 * on queued calls, for a CTMC model of a call center.
 * This class encapsulates an array of circular arrays of
 * integers representing the waiting queues, and
 * provides the {@link #init()} that should be called
 * after {@link CallCenterCTMC#initEmpty()} or
 * {@link CallCenterCTMC#init(CallCenterCTMC)}.
 * It also implements the 
 * {@link #update(CallCenterCTMC,TransitionType)}
 * method which should be 
 * called after each transition to
 * update the waiting queues.
 * The methods {@link #getLastWaitingTime(int)}, and
 * {@link #getQueue(int)} can then be used by
 * CTMC models to implement the interface
 * {@link CallCenterCTMCWithQueues}.
 */
public class CallCenterCTMCQueues implements Cloneable {
   private CircularIntArray[] queues;
   private int[] lastWaitingTimes;

   /**
    * Constructs a new object holding queueing information
    * from the call center CTMC model \texttt{ctmc}.
    * @param ctmc the call center CTMC model.
    */
   public CallCenterCTMCQueues (CallCenterCTMC ctmc) {
      queues = new CircularIntArray[ctmc.getNumContactTypes ()];
      for (int k = 0; k < queues.length; k++)
         queues[k] = new CircularIntArray (ctmc.getQueueCapacity ());
      lastWaitingTimes = new int[queues.length];
   }

   /**
    * Returns the circular array of integers
    * containing the transition number at which
    * each call of type \texttt{k} entered the queue,
    * provided that the {@link #update(CallCenterCTMC,TransitionType)}
    * method has been called at each transition.
    * @param k the tested call type.
    * @return the circular array representing the queue.
    */
   public CircularIntArray getQueue (int k) {
      return queues[k];
   }

   /**
    * Returns the number of transitions spent by the last
    * call of type \texttt{k} having left the queue,
    * provided that the {@link #update(CallCenterCTMC,TransitionType)}
    * method has been called after each transition.
    * @param k the tested call type.
    * @return the number of transitions spent in queue.
    */
   public int getLastWaitingTime (int k) {
      return lastWaitingTimes[k];
   }

   /**
    * Empties all the circular arrays representing
    * waiting queues, and resets the last waiting times to 0.
    */
   public void init () {
      for (int k = 0; k < queues.length; k++)
         queues[k].clear ();
      Arrays.fill (lastWaitingTimes, 0);
   }
   
   /**
    * Initializes this object with the contents
    * of the other object \texttt{q}.
    * @param q another object holding queue information.
    */
   public void init (CallCenterCTMCQueues q) {
      if (queues.length != q.queues.length)
         throw new IllegalArgumentException();
      System.arraycopy (q.lastWaitingTimes, 0, lastWaitingTimes, 0, lastWaitingTimes.length);
      for (int k = 0; k < queues.length; k++)
         queues[k].init (q.queues[k]);
   }

   /**
    * Updates the status of the waiting queues after
    * a transition of type \texttt{type} of the CTMC model
    * \texttt{ctmc}.
    * @param ctmc the CTMC model in which the transition occurred.
    * @param type the type of transition.
    */
   public void update (CallCenterCTMC ctmc, TransitionType type) {
      final int tr = ctmc.getNumTransitionsDone () - 1
            - ctmc.getNumFollowingFalseTransitions ();
      int k;
      switch (type) {
      case ARRIVALSERVED:
      case ARRIVALBALKED:
         k = ctmc.getLastSelectedContactType ();
         lastWaitingTimes[k] = 0;
         break;
      case ARRIVALQUEUED:
         k = ctmc.getLastSelectedContactType ();
         queues[k].add (tr);
         assert queues[k].size () == ctmc.getNumContactsInQueue (k);
         break;
      case ENDSERVICEANDDEQUEUE:
         k = ctmc.getLastSelectedQueuedContactType ();
         lastWaitingTimes[k] = tr - queues[k].removeFirst ();
         assert queues[k].size () == ctmc.getNumContactsInQueue (k);
         assert lastWaitingTimes[k] >= 0;
         break;
      case ABANDONMENT:
         k = ctmc.getLastSelectedContactType ();
         int index;
         if (queues[k].size () > 1)
            index = ctmc.getLastSelectedContact ();
         else
            index = 0;
         final int qtr = queues[k].remove (index);
         lastWaitingTimes[k] = tr - qtr;
         assert queues[k].size () == ctmc.getNumContactsInQueue (k);
         assert lastWaitingTimes[k] >= 0;
         break;
      }
   }

   /**
    * Constructs and returns a deep copy of this object,
    * including copies of the waiting queues.
    */
   public CallCenterCTMCQueues clone () {
      CallCenterCTMCQueues cpy;
      try {
         cpy = (CallCenterCTMCQueues) super.clone ();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      cpy.queues = queues.clone ();
      cpy.lastWaitingTimes = lastWaitingTimes.clone ();
      for (int k = 0; k < queues.length; k++)
         cpy.queues[k] = queues[k].clone ();
      return cpy;
   }
}
