package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;

/**
 * Provides some convenience methods for selecting a waiting queue
 * from a list of indices. Each static method of this class
 * returns a reference to the selected waiting queue or
 * \texttt{null} if no waiting queue can be chosen.
 * They must be given a list of indices used to reference
 * waiting queues in the given router. One can also
 * specify an optional array of booleans indicating which
 * indices will be taken into account.
 * The list of indices is traversed the same way as with {@link AgentGroupSelectors}
 * except that {@link Router#getWaitingQueue} is used instead of
 * {@link Router#getAgentGroup}.
 */
public final class WaitingQueueSelectors {
   private WaitingQueueSelectors() {}

   /**
    * Selects, from the given list of indices, the first waiting queue
    * containing at least one contact.
    @param router the router used to map indices in the given list
    to {@link WaitingQueue} references.
    @param ind the list of waiting queue indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected waiting queue.
    */
   public static WaitingQueue selectFirstNonEmpty (Router router, int[] ind,
                                                boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final WaitingQueue queue = router.getWaitingQueue (ind[j]);
         if (queue != null && queue.size() > 0)
            return queue;
      }
      return null;
   }

   /**
    * Equivalent to {@link #selectFirstNonEmpty selectFirstNonEmpty}
    * \texttt{(router, ind, null)}.
    */
   public static WaitingQueue selectFirstNonEmpty (Router router, int[] ind) {
      for (final int element : ind) {
         if (element < 0)
            continue;
         final WaitingQueue queue = router.getWaitingQueue (element);
         if (queue != null && queue.size() > 0)
            return queue;
      }
      return null;
   }

   /**
    * Selects, from the given list of indices, the last waiting queue
    * containing at least one contact.
    @param router the router used to map indices in the given list
    to {@link WaitingQueue} references.
    @param ind the list of waiting queue indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected waiting queue.
    */
   public static WaitingQueue selectLastNonEmpty (Router router, int[] ind,
                                               boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      for (int j = ind.length - 1; j >= 0; j--) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final WaitingQueue queue = router.getWaitingQueue (ind[j]);
         if (queue != null && queue.size() > 0)
            return queue;
      }
      return null;
   }

   /**
    * Equivalent to {@link #selectLastNonEmpty selectLastNonEmpty}
    * \texttt{(router, ind, null)}.
    */
   public static WaitingQueue selectLastNonEmpty (Router router, int[] ind) {
      for (int j = ind.length - 1; j >= 0; j--) {
         if (ind[j] < 0)
            continue;
         final WaitingQueue queue = router.getWaitingQueue (ind[j]);
         if (queue != null && queue.size() > 0)
            return queue;
      }
      return null;
   }

   /**
    * Returns a reference to the longest waiting queue in the given list.
    @param router the router used to map indices in the given list
    to {@link WaitingQueue} references.
    @param ind the list of waiting queue indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected waiting queue.
    */
   public static WaitingQueue selectLongest (Router router, int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      WaitingQueue best = null;
      int bestSize = 0;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
          if (subset != null && !subset[j])
            continue;
         final WaitingQueue queue = router.getWaitingQueue (ind[j]);
         final int size = queue == null ? 0 : queue.size();
         if (size > bestSize) {
            best = queue;
            bestSize = size;
         }
      }
      return best;
   }

   /**
    * Equivalent to {@link #selectLongest selectLongest}
    * \texttt{(router, ind, null)}.
    */
   public static WaitingQueue selectLongest (Router router, int[] ind) {
      WaitingQueue best = null;
      int bestSize = 0;
      for (final int element : ind) {
         if (element < 0)
            continue;
         final WaitingQueue queue = router.getWaitingQueue (element);
         final int size = queue == null ? 0 : queue.size();
         if (size > bestSize) {
            best = queue;
            bestSize = size;
         }
      }
      return best;
   }

   /**
    * Selects the waiting queue containing the contact
    * with the smallest enqueue time, assuming
    * that waiting queues attached to the router
    * use FIFO discipline. This allows
    * the router to consider a set of waiting queues
    * as a single queue when dequeueing contacts.
    @param router the router used to map indices in the given list
    with {@link WaitingQueue} references.
    @param ind the list of waiting queue indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected waiting queue.
    */
   public static WaitingQueue selectSmallestFirstEnqueueTime (Router router,
                                                           int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      WaitingQueue best = null;
      double minTime = Double.POSITIVE_INFINITY;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
          if (subset != null && !subset[j])
            continue;
         final WaitingQueue queue = router.getWaitingQueue (ind[j]);
         if (queue != null && queue.size() > 0) {
            final DequeueEvent ev = queue.getFirst();
            final double qt = ev.getEnqueueTime();
            if (qt < minTime) {
               minTime = qt;
               best = queue;
            }
         }
      }
      return best;
   }

   /**
    * Equivalent to {@link #selectSmallestFirstEnqueueTime selectSmallestFirstEnqueueTime}
    * \texttt{(router, ind, null)}.
    */
   public static WaitingQueue selectSmallestFirstEnqueueTime (Router router, int[] ind) {
      WaitingQueue best = null;
      double minTime = Double.POSITIVE_INFINITY;
      for (final int element : ind) {
         if (element < 0)
            continue;
         final WaitingQueue queue = router.getWaitingQueue (element);
         if (queue != null && queue.size() > 0) {
            final DequeueEvent ev = queue.getFirst();
            final double qt = ev.getEnqueueTime();
            if (qt < minTime) {
               minTime = qt;
               best = queue;
            }
         }
      }
      return best;
   }

   /**
    * Selects the waiting queue containing the contact
    * with the longest weighted waiting time, assuming
    * that waiting queues attached to the router
    * use FIFO discipline.  The given array \texttt{weights}
    * assigned a weight $w_q$ to each of the $Q$ waiting queues
    * attached to the router.
    * This method considers each queues referred to by
    * the array of indices, and selects the non-empty
    * queue for which $w_qW_q$ is maximal, where
    * $W_q$ is the waiting time of the first contact
    * in queue $q$.
    @param router the router used to map indices in the given list
    with {@link WaitingQueue} references.
    @param ind the list of waiting queue indices.
    @param weights the array of weights assigned to waiting queues.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected waiting queue.
    */
   public static WaitingQueue selectLongestWeightedWaitingTime (Router router,
                                                           int[] ind,
                                                           double[] weights,
                                                           boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      if (weights.length != router.getNumWaitingQueues())
         throw new IllegalArgumentException
         ("The length of the array of weights (" 
               + weights.length + ") must be the same as the number of waiting queues ("
               + router.getNumWaitingQueues() + ")");
      WaitingQueue best = null;
      double maxWeightedTime = Double.NEGATIVE_INFINITY;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final int qid = ind[j];
         final WaitingQueue queue = router.getWaitingQueue (qid);
         if (queue != null && queue.size() > 0) {
            final DequeueEvent ev = queue.getFirst();
            final double qt = ev.simulator().time() - ev.getEnqueueTime();
            assert qt >= 0 : "Negative waiting time: " + qt;
            final double wqt = weights[qid]*qt;
            if (wqt > maxWeightedTime) {
               maxWeightedTime = wqt;
               best = queue;
            }
         }
      }
      return best;
   }

   /**
    * Equivalent to {@link #selectLongestWeightedWaitingTime selectLongestWeightedWaitingTime}
    * \texttt{(router, ind, weights, null)}.
    */
   public static WaitingQueue selectLongestWeightedWaitingTime (Router router, int[] ind, double[] weights) {
      if (weights.length != router.getNumWaitingQueues())
         throw new IllegalArgumentException
         ("The length of the array of weights (" 
               + weights.length + ") must be the same as the number of waiting queues ("
               + router.getNumWaitingQueues() + ")");
      WaitingQueue best = null;
      double maxWeightedTime = Double.NEGATIVE_INFINITY;
      for (final int element : ind) {
         if (element < 0)
            continue;
         final int qid = element;
         final WaitingQueue queue = router.getWaitingQueue (qid);
         if (queue != null && queue.size() > 0) {
            final DequeueEvent ev = queue.getFirst();
            final double qt = ev.simulator().time() - ev.getEnqueueTime();
            assert qt >= 0 : "Negative waiting time: " + qt;
            final double wqt = weights[qid]*qt;
            if (wqt > maxWeightedTime) {
               maxWeightedTime = wqt;
               best = queue;
            }
         }
      }
      return best;
   }
}
