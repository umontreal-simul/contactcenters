package umontreal.iro.lecuyer.contactcenters.router;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import umontreal.iro.lecuyer.collections.DescendingOrderComparator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Extends the agents' preference-based router to support delays for routing,
 * and allow priority to change with waiting time. Often, a contact has to
 * wait for some time before it can overflow to groups of backup agents. Delays
 * are used to favor the usage of primary agents as opposed to backup agents
 * which are kept for customers which have waited long enough. The priority of a
 * waiting contact may also change if it is waiting long enough. This router
 * allows the user to input such delays, and to set up several different
 * matrices of ranks for priority to be a piecewise-constant function of the waiting time.
 *
 * \paragraph*{Data structures.}
 * This router uses the same structures as the agents' preference-based router
 * without delays, with an additional $I\times K$ matrix of delays, and optional
 * extra group-to-type matrices of ranks associated with minimal waiting times.
 * Each delay $d(i, k)$ is a finite positive number indicating the minimal time
 * a contact of type~$k$ must wait to be accepted for service by an agent in
 * group~$i$.
 *
 * Each extra matrix of ranks defines a function $\rGT[j](i, k)$ which associates a
 * matrix of ranks with the minimal waiting time $w_j$. Let $w_0=0$ and $\rGT[0](i,
 * k)=\rGT(i, k)$. So if no extra matrix of ranks is given, we have only $\rGT[0](i,
 * k)$, the default matrix of ranks used by the agents' preference-based routing
 * policy without delays.
 *
 * Note that fixing $d(i, k)=0$ for all $i$ and $k$, and omitting extra
 * matrices of ranks reverts to the original agents' preference-based
 * routing without delays.
 *
 * \paragraph*{Basic routing scheme.}
 * We now describe more specifically how the routing with delays works. Let
 * $d_{k, 1}, d_{k, 2}, \ldots$ be the delays $d(\cdot, k)$ sorted in increasing
 * order, with duplicates eliminated, and $d_{k, 0}=0$. When a contact of
 * type~$k$ arrives, it can be served only by agents whose group~$i$ satisfies
 * $d(i, k)=0$ in addition to the conditions imposed by the agents'
 * preference-based routing policy. If a contact is queued as no free agent is
 * available to serve it, an event is scheduled to try routing the contact again
 * after a delay $d_{k, 1}$. During this so-called rerouting, the delay
 * condition becomes $W\ge d(i, k)$, where $W$ is the time the contact has
 * waited in queue so far. If this second agent selection fails, a third trial
 * happens after a delay $d_{k, 2} - d_{k, 1}$. More generally, reroutings
 * happen for each delay $d_{k, j}$, for $j=1,2,\ldots$, unless the contact is
 * accepted by an agent, or abandons. Consequently, as its waiting time
 * increases, the contact can be accepted by a wider range of agents.
 *
 * Contact selection is done in a similar way as with the agents'
 * preference-based routing policy, except that delays $d(i, k)$, and extra
 * matrices of ranks $\rGT[j](i, k)$ are taken into account while determining the
 * rank for a pair $(i,k)$. More specifically, let $W_k$ be the longest waiting
 * time among all queued contacts of type~$k$.
 * First, the rank of a queued contact of type~$k$ is infinite (so the call cannot
 * leave the queue) if its waiting time $W_k$ is smaller
 * than the delay $d(i,k)$.
 * On the other hand,
 * if $W_k\ge d(i,k)$, the rank is given by
 * $\rGT[j'](i, k)$ where $j'=\max\{j: W_k\ge w_j\}$ is the index of the matrix of
 * ranks applying to the queued contact.
 * If $j'>0$, we check the other queued contacts of type~$k$ to
 * determine if another queued contact has a smaller rank, i.e., an higher priority.
 * For each scanned queued contact, we check the delay condition
 * and stop scanning as soon as $W_k < d(i,k)$ or $j'=0$.
 *
 * The default behavior of this policy can be altered by two switches: overflow
 * transfer, and longest waiting time modes. When overflow transfer is turned
 * ON, a contact gaining access to some agent groups after waiting some delay
 * also loses access to the original agent groups. When longest waiting time is
 * turned OFF, the contact selection gives priority to pairs $(i, k)$ with small
 * delays $d(i, k)$.
 *
 * \paragraph*{Overflow transfer mode.}
 * In this mode, turned off by default, the delay condition for the $j$th
 * rerouting ($j+1$th agent selection) becomes $d_{k, j}\le q < d_{k, j+1}$,
 * $j$ starting with 0, while the original condition is $d_{k, j}\le q$. With
 * this variant, when a contact has waited sufficient long to overflow to a
 * new set of agent groups, it cannot be served by the original agent groups.
 * Overflow can then be considered as a transfer in a new section of the
 * contact center.
 *
 * \paragraph*{Longest waiting time mode.}
 * In this mode, turned on by default, contact selection is performed
 * in a single
 * pass, in a way similar to the contact selection of the
 * policy without delays. However, the
 * delay condition
 * is enforced to restrict
 * contact-to-agent assignment.
 *
 * If this option is disabled, contact selection is performed using the
 * following multiple-passes process. When an agent in group~$i$ becomes free,
 * it first searches for a contact whose type~$k$ satisfies $d(i, k)=0$.
 * Then, it searches for contacts for which $d(i, k)\le d_{k, 1}$, for
 * contacts for which $d(i, k)\le d_{k, 2}$, etc., in that order. This gives
 * higher priority to contacts with small minimal delay, because they can be
 * served by a more restricted set of agents.
 *
 * The latter behavior of this router is especially appropriate if delays are
 * functions of the distance between the contact and the agent. For local
 * contacts, $d(i, k)$ is small, while it is large for remote contacts. The
 * router then always gives priority to local assignments.
 * However, it is often simpler and more intuitive to use the single-pass
 * contact selection.
 */
public class AgentsPrefRouterWithDelays extends AgentsPrefRouter {
   private double[][] delaysGT;
   // For each contact type, gives the list of different
   // delaysGT[.][k], sorted in increasing order.
   private double[][] sortedDelaysTG;
   private int curNumReroutingsDone;
   private int maxNumReroutings;
   private boolean overflowTransfer = false;
   private boolean longestWaitingTime = true;
   private final SortedMap<Double, double[][]> ranksGTDelayMap = new TreeMap<Double, double[][]> (
         new DescendingOrderComparator<Double> ());
   private DequeueEvent[] bestEvents;

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksGT} and delays matrix \texttt{delaysGT}. The given matrices
    * must be rectangular with one row per agent group, and one column per
    * contact type. They define the functions $\rGT(i, k)$
    * (with $\rTG(k, i)=\rGT(i, k)$), and $d(i, k)$, respectively.
    * The weights matrices are initialized with 1's.
    *
    * @param ranksGT
    *           the contact selection matrix of ranks being used.
    * @param delaysGT
    *           the delays matrix.
    * @exception NullPointerException
    *               if \texttt{ranksGT} or \texttt{delaysGT} are \texttt{null}.
    * @exception IllegalArgumentException
    *               if the ranks or delays 2D array are not rectangular.
    */
   public AgentsPrefRouterWithDelays (double[][] ranksGT, double[][] delaysGT) {
      super (ranksGT);
      bestEvents = new DequeueEvent[ranksGT[0].length];
      initDelays (delaysGT);
   }

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksTG} defining how contacts prefer agents, \texttt{ranksGT}
    * defining how agents prefer contacts, and \texttt{delaysGT} for routing
    * delays. The given matrices must be rectangular. The weights matrices are
    * initialized with 1's.
    *
    * @param ranksTG
    *           the matrix of ranks defining how contacts prefer agents.
    * @param ranksGT
    *           the matrix of ranks defining how agents prefer contacts.
    * @param delaysGT
    *           the delays matrix.
    * @exception NullPointerException
    *               if \texttt{ranksGT}, \texttt{ranksTG}, or \texttt{delaysGT}
    *               are \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given 2D arrays are not rectangular.
    */
   public AgentsPrefRouterWithDelays (double[][] ranksTG, double[][] ranksGT,
         double[][] delaysGT) {
      super (ranksTG, ranksGT);
      bestEvents = new DequeueEvent[ranksTG.length];
      initDelays (delaysGT);
   }

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksTG} defining how contacts prefer agents, and \texttt{ranksGT}
    * defining how agents prefer contacts. The weights matrices are set to
    * \texttt{weightsTG}, and \texttt{weightsGT}. The delays matrix is set to
    * \texttt{delaysGT}. The given matrices must be rectangular.
    *
    * @param ranksTG
    *           the matrix of ranks defining how contacts prefer agents.
    * @param ranksGT
    *           the matrix of ranks defining how agents prefer contacts.
    * @param weightsTG
    *           the weights matrix defining $\wTG(k, i)$.
    * @param weightsGT
    *           the weights matrix defining $\wGT(i, k)$.
    * @param delaysGT
    *           the delays matrix.
    * @exception NullPointerException
    *               if \texttt{ranksGT}, \texttt{ranksTG}, \texttt{weightsTG},
    *               \texttt{weightsGT}, or \texttt{delaysGT} are \texttt{null}.
    * @exception IllegalArgumentException
    *               if the 2D arrays are not rectangular.
    */
   public AgentsPrefRouterWithDelays (double[][] ranksTG, double[][] ranksGT,
         double[][] weightsTG, double[][] weightsGT, double[][] delaysGT) {
      super (ranksTG, ranksGT, weightsTG, weightsGT);
      bestEvents = new DequeueEvent[ranksTG.length];
      initDelays (delaysGT);
   }

   private void initDelays (double[][] delaysGT1) {
      ArrayUtil.checkRectangularMatrix (delaysGT1);
      final int K = getNumContactTypes ();
      final int I = getNumAgentGroups ();
      if (delaysGT1.length != I)
         throw new IllegalArgumentException (
               "Invalid number of rows in the delays matrix");
      if (delaysGT1[0].length != K)
         throw new IllegalArgumentException (
               "Invalid number of columns in the delays matrix");
      this.delaysGT = ArrayUtil.deepClone (delaysGT1, true);
      sortedDelaysTG = new double[K][];
      maxNumReroutings = 0;
      final SortedSet<Double> set = new TreeSet<Double> ();
      for (int k = 0; k < K; k++) {
         for (int i = 0; i < I; i++) {
            if (Double.isNaN (delaysGT1[i][k]) || delaysGT1[i][k] < 0)
               throw new IllegalArgumentException ("delaysGT[" + i + "][" + k
                     + "] = " + delaysGT1[i][k] + " is invalid");
            if (Double.isInfinite (delaysGT1[i][k]))
               continue;
            // if (delaysGT[i][k] > 0)
            set.add (delaysGT1[i][k]);
         }
         set.add (0.0);
         final int size = set.size ();
         sortedDelaysTG[k] = new double[size];
         int idx = 0;
         for (final Double v : set)
            sortedDelaysTG[k][idx++] = v;
         set.clear ();
         if (size > maxNumReroutings)
            maxNumReroutings = size;
      }
   }

   /**
    * Returns the delays matrix used by this router.
    *
    * @return the delays matrix.
    */
   public double[][] getDelaysGT () {
      return ArrayUtil.deepClone (delaysGT, true);
   }

   /**
    * Returns $d(i, k)$ for the given agent group index \texttt{i}, and contact
    * type identifier \texttt{k}.
    *
    * @param i
    *           the queried agent group index.
    * @param k
    *           the queried contact type identifier.
    * @return the delay $d(i, k)$.
    */
   public double getDelayGT (int i, int k) {
      return delaysGT[i][k];
   }

   /**
    * Sets the delays matrix of this router to \texttt{delaysGT}.
    *
    * @param delaysGT
    *           the new delays matrix.
    * @exception NullPointerException
    *               if \texttt{delaysGT} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{delaysGT} is not rectangular.
    */
   public void setDelaysGT (double[][] delaysGT) {
      initDelays (delaysGT);
   }

   /**
    * Returns \texttt{true} if the overflow transfer mode is enabled. By
    * default, this returns \texttt{false}.
    *
    * @return the status of the overflow transfer mode.
    */
   public boolean getOverflowTransferStatus () {
      return overflowTransfer;
   }

   /**
    * Sets the overflow transfer mode to \texttt{overFlowTransfer}.
    *
    * @param overflowTransfer
    *           the new status of the mode.
    * @see #getOverflowTransferStatus()
    */
   public void setOverflowTransferStatus (boolean overflowTransfer) {
      this.overflowTransfer = overflowTransfer;
   }

   /**
    * Returns \texttt{true} if the router uses a single-phase agent selection
    * based on the longest waiting time.
    *
    * @return the status of the longest waiting time mode.
    */
   public boolean getLongestWaitingTimeStatus () {
      return longestWaitingTime;
   }

   /**
    * Sets the longest waiting time mode to \texttt{longestWaitingTime}.
    *
    * @param longestWaitingTime
    *           the new status of the mode.
    * @see #getLongestWaitingTimeStatus()
    */
   public void setLongestWaitingTimeStatus (boolean longestWaitingTime) {
      this.longestWaitingTime = longestWaitingTime;
   }

   /**
    * Sets the matrix of ranks used for selecting contacts which have waited at
    * least \texttt{minWaitingTime}.
    *
    * @param minWaitingTime
    *           the minimum waiting time.
    * @param ranksGT
    *           the matrix of ranks for this minimal waiting time.
    */
   public void setRanksGT (double minWaitingTime, double[][] ranksGT) {
      if (minWaitingTime <= 0)
         throw new IllegalArgumentException ("minWaitingTime must be positive");
      ArrayUtil.checkRectangularMatrix (ranksGT);
      if (ranksGT.length != getNumAgentGroups ())
         throw new IllegalArgumentException (
               "Invalid number of rows in the matrix of ranks");
      if (ranksGT[0].length != getNumContactTypes ())
         throw new IllegalArgumentException (
               "Invalid number of columns in the matrix of ranks");
      ranksGTDelayMap.put (minWaitingTime, ArrayUtil.deepClone (ranksGT));
   }

   private double getTestDelay (int k, int step) {
      if (step >= sortedDelaysTG[k].length)
         return Double.POSITIVE_INFINITY;
      return sortedDelaysTG[k][step];
   }

   private boolean isInfiniteScoreFromDelay (int k, int i) {
      final double delay = delaysGT[i][k];
      // We do not compare the delay with the waiting time directly,
      // because the waiting time is computed from a difference.
      // This results in numerical instability which can affect
      // the results of the test.
      // Get the waiting time of the contact at the current rerouting step
      final double testWaitingTime = getTestDelay (k, curNumReroutingsDone + 1);
      if (overflowTransfer)
         return testWaitingTime != delay;
      else
         // Infinite score if the delay is greater than the contact's
         // waiting time.
         return testWaitingTime < delay;
   }

   private double getMaxDelay (int i, int k) {
      if (!overflowTransfer)
         return Double.POSITIVE_INFINITY;
      final double minDelay = delaysGT[i][k];
      int idx = 0;
      while (sortedDelaysTG[k][idx] != minDelay)
         ++idx;
      if (idx + 1 < sortedDelaysTG[k].length)
         return sortedDelaysTG[k][idx + 1];
      return Double.POSITIVE_INFINITY;
   }

   private double getSmallestWaitingTime (int k) {
      final WaitingQueue queue = getWaitingQueue (k);
      if (queue == null || queue.isEmpty ())
         return 0;
      final DequeueEvent lastEvent = queue.getLast ();
      return lastEvent.simulator ().time ()
            - lastEvent.getEnqueueTime ();
   }

   private double getLongestWaitingTime (int k) {
      final WaitingQueue queue = getWaitingQueue (k);
      if (queue == null || queue.isEmpty ())
         return 0;
      final DequeueEvent firstEvent = queue.getFirst ();
      return firstEvent.simulator ().time ()
            - firstEvent.getEnqueueTime ();
   }

   @Override
   protected double getRankForContactSelection (int i, int k) {
      bestEvents[k] = null;
      final double minDelay = delaysGT[i][k];
      if (!longestWaitingTime) {
         final int pass = curNumReroutingsDone + 1;
         final double minDelayPass = getTestDelay (k, pass);
         final double maxDelayPass = getTestDelay (k, pass + 1);
         if (minDelay < minDelayPass || minDelay >= maxDelayPass)
            return Double.POSITIVE_INFINITY;
      }
      final double longestWaitingTime1 = getLongestWaitingTime (k);
      if (longestWaitingTime1 < minDelay)
         // Any subsequent contact has smaller waiting time
         return Double.POSITIVE_INFINITY;
      final double maxDelay = getMaxDelay (i, k);
      boolean scanQueue = false;
      final double smallestWaitingTime = getSmallestWaitingTime (k);
      if (longestWaitingTime1 > maxDelay) {
         // The time spent by the first contact in queue is too long
         if (smallestWaitingTime > maxDelay)
            // All queued contacts are waiting for a too long time
            return Double.POSITIVE_INFINITY;
         else
            // By scanning the queue, we may find a contact with
            // a waiting time smaller than maxDelay but larger or equal to
            // minDelay
            scanQueue = true;
      }

      if (longestWaitingTime1 <= 0)
         // Empty queue, so revert to super class returning a fixed rank
         return super.getRankForContactSelection (i, k);
      final SortedMap<Double, double[][]> curRanks;
      final double rank0;
      if (!scanQueue) {
         if (ranksGTDelayMap.isEmpty ())
            // No priority changing with waiting time, so return
            // a fixed rank
            return super.getRankForContactSelection (i, k);

         // From now on, the rank can depend on the waiting time.
         // First, find the rank for small waiting times.
         // This is the basic rank
         rank0 = getRankGT (i, k);
         // Eliminate matrices of ranks concerning delays greater than the current
         // longest waiting time.
         // Since ranksGTDelayMap is sorted by descending order of delay,
         // obtaining the tail map eliminates larger delays.
         curRanks = ranksGTDelayMap.tailMap (longestWaitingTime1);
         if (curRanks.isEmpty ())
            // There is not entry in the map for delays smaller than
            // longestWaitingTime, so there is no change of priority
            // even for the first contact in queue which has waited
            // the longest.
            return rank0;
         // Find the rank for the first contact in queue k.
         // This gives the matrix of ranks associated with
         // the maximal threshold smaller than longestWaitingTime.
         double[][] testRanksGT = curRanks.get (curRanks.firstKey ());
         double rank = testRanksGT[i][k];
         if (longestWaitingTime1 == smallestWaitingTime)
            // Queue with a single contact, so we have
            // the correct rank
            return rank;
         if (curRanks.size () == 1) {
            // There is a single priority change here.
            if (rank <= rank0)
               // The rank for contacts with large waiting times
               // is smaller than the rank for contacts with
               // shorter waiting times, so priority
               // increases with waiting time.
               // In other words, even if we scan the
               // queue, we will not find any contact
               // with a greater rank than
               // the first one.
               // So we have found the rank.
               return rank;
         }
         // Here, there may be contacts in the queue
         // with smaller rank that the first contact,
         // so we need to scan the queue to find this.
      }
      else {
         curRanks = ranksGTDelayMap.tailMap (longestWaitingTime1);
         rank0 = getRankGT (i, k);
      }

      // We resort to scanning the queue for finding the
      // contact with the maximal rank.
      final Iterator<Map.Entry<Double, double[][]>> itRanks = curRanks.entrySet ()
      .iterator ();
      double curWt;
      double curRank;
      if (itRanks.hasNext ()) {
         final Map.Entry<Double, double[][]> curEntry = itRanks.next ();
         curWt = curEntry.getKey ();
         curRank = curEntry.getValue ()[i][k];
      }
      else {
         curWt = 0;
         curRank = rank0;
      }
      final Iterator<DequeueEvent> itQ = getWaitingQueue (k).iterator ();
      double bestRank = Double.POSITIVE_INFINITY;
      while (itQ.hasNext ()) {
         final DequeueEvent ev = itQ.next ();
         final double wt = ev.simulator ().time ()
         - ev.getEnqueueTime ();
         if (wt < minDelay)
            // Waiting time is too short, and any further
            // waiting times will be even shorter, so stop
            // iterating over the queue, and return
            // the rank found so far.
            return bestRank;
         if (wt >= maxDelay)
            // The delay is too large, so skip the contact.
            continue;
         while (wt < curWt) {
            // Decrease the curWt threshold (and adjust the rank)
            // accordingly, unless the threshold is not greater
            // than the waiting time.
            // Further waiting times will be smaller than wt,
            // so we do not need to reset itRanks.
            if (itRanks.hasNext ()) {
               final Map.Entry<Double, double[][]> curEntry = itRanks.next ();
               curWt = curEntry.getKey ();
               curRank = curEntry.getValue ()[i][k];
            }
            else {
               curWt = 0;
               curRank = rank0;
            }
         }
         if (curRank < bestRank) {
            bestRank = curRank;
            bestEvents[k] = ev;
            if (!itRanks.hasNext () && curRank <= rank0)
               // Any further contact in the queue will
               // have the same rank, or larger rank.
               return bestRank;
         }
      }
      return bestRank;
//
//      final SortedMap<Double, double[][]> curRanks2 = ranksGTDelayMap.tailMap (smallestWaitingTime);
//      final double minWaitingTimeInMap;
//      if (curRanks2.isEmpty ())
//         minWaitingTimeInMap = 0;
//      else
//         minWaitingTimeInMap = curRanks2.firstKey ();
//
//
//      double[][] testRanksGT = curRanks.get (curRanks.firstKey ());
//      final double rank = testRanksGT[i][k];
//      if (rank > rank0)
//         // Supporting priority decreasing would require scanning the waiting
//         // queues,
//         // which is costly.
//         throw new UnsupportedOperationException (
//               "The priority of a contact type cannot decrease with its waiting time with this routing policy");
//      final Iterator<Map.Entry<Double, double[][]>> it = curRanks.entrySet ()
//      .iterator ();
//      it.next ();
//      while (it.hasNext ()) {
//         final Map.Entry<Double, double[][]> e = it.next ();
//         final double[][] ranks = e.getValue ();
//         if (ranks[i][k] < rank)
//            throw new UnsupportedOperationException (
//                  "The priority of a contact type cannot decrease with its waiting time with this routing policy");
//      }
//      return rank;
   }

   @Override
   protected void selectWaitingQueue (AgentGroup group, Agent agent,
         double bestRank, boolean[] qCandidates, int numCandidates) {
      // Similar to the implementation in the superclass,
      // except that the dequeued contact may differ
      // from the first contact in queue.
      //final int i = group.getId ();
      bestQueue = null;
      bestQueuedContact = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      final int K = getNumContactTypes ();
      for (int k = 0; k < K; k++) {
         if (!qCandidates[k])
            continue;
         //final double minDelay = delaysGT[i][k];
//          if (!longestWaitingTime) {
//             final int pass = curNumReroutingsDone + 1;
//             final double minDelayPass = getTestDelay (k, pass);
//             final double maxDelayPass = getTestDelay (k, pass + 1);
//             if (minDelay < minDelayPass || minDelay >= maxDelayPass)
//                continue;
//          }
         final WaitingQueue queue = getWaitingQueue (k);
         //final double maxDelay = getMaxDelay (i, k);
         DequeueEvent dqEvent;
         if (bestEvents[k] != null)
            // We have already scanned the queue in getRankForContactSelection,
            // so reuse the result of this here.
            dqEvent = bestEvents[k];
         else
            dqEvent = queue.getFirst ();
         //double q = dqEvent.getContact ().getTotalQueueTime ();
//         double q = getLongestWaitingTime (k);
//         if (q < minDelay)
//            // Any subsequent contact has smaller waiting time
//            continue;
//         if (q > maxDelay)
//            if (queue.size () > 1)
//               // The queue might contain a contact
//               // with a smaller waiting time,
//               // so we must scan it.
//               for (final DequeueEvent ev : queue) {
//                  if (ev == dqEvent) {
//                     // Skip the first contact we have
//                     // already inspected.
//                     dqEvent = null;
//                     continue;
//                  }
//                  q = ev.getContact ().getTotalQueueTime ();
//                  if (q < minDelay)
//                     // Any subsequent contact will have
//                     // smaller waiting times
//                     break;
//                  if (q < maxDelay) {
//                     // Found a candidate
//                     dqEvent = ev;
//                     break;
//                  }
//               }
//            else
//               dqEvent = null;
         if (dqEvent == null)
            continue;
         if (numCandidates == 1) {
            bestQueue = queue;
            bestQueuedContact = bestEvents[k];
            return;
         }
         final double score = getScoreForContactSelection (group, dqEvent);
         if (score < 0 && Double.isInfinite (score))
            continue;
         if (score > bestScore) {
            bestQueue = queue;
            bestQueuedContact = bestEvents[k];
            bestScore = score;
         }
      }
   }

   @Override
   protected double getRankForAgentSelection (int k, int i) {
      if (isInfiniteScoreFromDelay (k, i))
         return Double.POSITIVE_INFINITY;
      return super.getRankForAgentSelection (k, i);
   }

   @Override
   protected double getScoreForAgentSelection (Contact ct,
         AgentGroup testGroup, Agent testAgent) {
      if (isInfiniteScoreFromDelay (ct.getTypeId (), testGroup.getId ()))
         return Double.NEGATIVE_INFINITY;
      return super.getScoreForAgentSelection (ct, testGroup, testAgent);
   }

   @Override
   protected double getReroutingDelay (DequeueEvent dqEv, int numReroutingsDone) {
      final Contact contact = dqEv.getContact ();
      final int k = contact.getTypeId ();
      final int idx = numReroutingsDone + 1;
      if (idx >= sortedDelaysTG[k].length - 1)
         return Double.POSITIVE_INFINITY;
      // We do not use the waiting time for numerical stability
      return sortedDelaysTG[k][idx + 1] - sortedDelaysTG[k][idx];
      // final double qt = contact.getTotalQueueTime ();
      // return sortedDelaysTG[k][idx] - qt;
      // final int I = getNumAgentGroups();
      // double reroutingDelay = Double.POSITIVE_INFINITY;
      // for (int i = 0; i < I; i++) {
      // double remainingDelay = delaysGT[i][k] - qt;
      // if (remainingDelay <= 0)
      // continue;
      // if (remainingDelay < reroutingDelay)
      // reroutingDelay = remainingDelay;
      // }
      // return reroutingDelay;
   }

   @Override
   protected EndServiceEvent selectAgent (Contact ct) {
      curNumReroutingsDone = -1;
      return super.selectAgent (ct);
   }

   @Override
   protected boolean checkFreeAgents (AgentGroup group, Agent agent) {
      curNumReroutingsDone = -1;
      boolean oneBusy = super.checkFreeAgents (group, agent);
      if (longestWaitingTime) {
         // Multiple passes
         ++curNumReroutingsDone;
         Agent testAgent = agent;
         while (group.getNumFreeAgents () > 0
               && curNumReroutingsDone < maxNumReroutings) {
            if (oneBusy)
               testAgent = null;
            if (super.checkFreeAgents (group, testAgent))
               oneBusy = true;
            ++curNumReroutingsDone;
         }
      }
      return oneBusy;
   }

   @Override
   protected EndServiceEvent selectAgent (DequeueEvent dqEv,
         int numReroutingsDone) {
      // The value of curNumReroutingsDone affects
      // the ranks returned by getRankForAgentSelection,
      // so the effect of the selectAgent method in the superclass
      // we call here depends on curNumReroutingsDone.
      curNumReroutingsDone = numReroutingsDone;
      return super.selectAgent (dqEv.getContact ());
   }

   @Override
   public String getDescription () {
      return "Agents' preference-based router with delays";
   }

   @Override
   public String toLongString () {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Delays matrix\n");
      sb.append (RoutingTableUtils.formatWeightsGT (delaysGT));
      return sb.toString ();
   }
}
