package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This router sends new contacts to agent groups using a
 * fixed list, but for each agent group, routing occurs
 * conditional on the expected waiting time.
 * More specifically, the router uses a $K\times I$ ranks matrix
 * giving a rank $\rTG(k, i)$ for each contact
 * type~$k$ and agent group~$i$.  The lower is this rank,
 * the higher is the priority of assigning contacts of type~$k$
 * to agents in group~$i$.
 * If a rank is $\infty$, the corresponding assignment
 * is not allowed.
 * The ranks matrix giving $\rTG(k,i)$ for all $k$ and $i$ is used
 * to generate \emph{overflow lists} defined as follows.
 * For each contact type $k$, the router creates a list of agent \emph{groupsets}
 * sharing the same priority.  The $j$th groupset for contact type $k$ is denoted
 * $i(k, j)=\{i=0,\ldots,I-1 \mid \rTG(k,i)=r_{k,j}\}$. Here,
 * $r_{k,j_1}< r_{k,j_2}<\infty$ for any $j_1< j_2$.
 * The overflow list for contact of type~$k$ is then
 * $i(k, 0), i(k, 1), \ldots$
 * For example, suppose we have the following ranks matrix:
 * \[
 * \left(\begin{array}{ccc}
 * 1 & 1 & 2 \\
 * 1 & \infty & 2 \\
 * \infty & 1 & \infty
 * \end{array}\right)
 * \]
 * The overflow list for contact type~0 is
 * $((0, 1), (2))$, while the overflow list
 * for contact type~1 is
 * $((0), (2))$.
 *
 * When a new contact of type~$k$ arrives,
 * the router performs two phases to assign an agent group or
 * waiting queues to the contact.
 * Here, each waiting queue corresponds to a single
 * agent group.
 * The first phase tries to associate an agent groupset with the contact
 * while the second phase, which occurs when the first phase fails,
 * associates a waiting queue to the contact.
 * The first phase checks every agent groupset $i(k, j)$ sequentially, and
 * stops as soon as a groupset containing a free agent is found.
 * For each considered groupset, the router
 * tests every agent group to
 * determine if at least one agent is free.
 * If a single agent is free, the contact is routed
 * to that agent.
 * If several agents of that groupset are free,
 * the contact is routed to the agent with
 * the longest idle time.
 *
 * If no agent is available in the tested groupset,
 * one or more waiting queues must be selected to add the contact to.
 * A waiting queue~$i$ with size $Q_i(t)$ is associated with
 * a single agent group, which has $N_i(t)$ agents, where $t$ is the current
 * simulation time.
 * The router considers waiting queues in the current
 * groupset only, and selects a queue only if
 * the queue ratio $(S_i(t) + 1)/N_i(t)$ is greater than
 * the agent-group specific target.
 * This queue ratio gives an estimate of the expected waiting time of the contact.
 * Note that this estimate assumes that service times are exponential, and
 * no abandonment is allowed.
 * If no candidate waiting queue is available, e.g., all waiting queues in
 * the groupset have a queue ratio greater than the
 * target queue ratio, the router checks the next
 * agent groupset.
 *
 * If there are no more groupset, the router performs the second phase
 * as follows.
 * Since no groupset contains a free agent or waiting queue with a small
 * enough queue ratio, the router checks every authorized waiting queue,
 * i.e., each queue~$i$ for
 * which $\rTG(k, i)<\infty$, and selects the waiting queue with the
 * smallest queue ratio.
 * In this phase, the queue ratio is allowed to be greater than
 * the target.
 *
 * The queues the contact is sent to depend on two flags associated with
 * this router: the copy and overflow modes.
 * The copy mode determines if contacts can be queued to multiple
 * agent groups.
 * The overflow mode, which is used only when contacts can be added
 * into multiple queues, can be set to transfer or promotion.
 * In \emph{transfer} mode, the contact moves from groupsets to
 * groupsets. In \emph{promotion} mode, a copy of the contact
 * is left in every considered groupset.
 *
 * More specifically,
 * if queueing to multiple targets is disabled,
 * the router always sends the contact to the queue with the smallest queue
 * ratio among the considered candidates.
 * In the first phase, these candidates are the queues in the current
 * groupset with a queue ratio smaller than the target.
 * In the second phase, this corresponds to all queues the
 * contact is authorized in.
 *
 * If contacts can be added to multiple queues, the
 * overflow mode has the following effect.
 * If candidates were found during the first phase, when
 * checking agent groupset $i(k, j)$, the contact is queued
 * to all queues in that groupset when the overflow mode is transfer.
 * However,
 * if the overflow mode is promotion, the contact is also
 * added to all queues in the preceding groupsets, i.e.,
 * groupsets $i(k, j')$ for $j'=0,\ldots,j$.
 * If the router reaches the second phase, the contact is always sent
 * to the queue with the smallest queue ratio.
 * In promotion mode, it is also queued to all other authorized waiting queues.
 *
 * This router needs agent groups taking individual
 * agents into account to select agents based
 * on their longest idle times.
 */
public class QueueRatioOverflowRouter extends Router {
   private double[][] ranksTG;
   // overflowList[k][j] contains the jth list of agent groups
   // to consider when trying to route a contact of type k.
   private int[][][] overflowList;
   private double[] targetQueueRatio;
   private boolean allowCopies = false;
   private boolean overflowTransfer;

   /**
    * Constructs a new queue-ratio overflow router with ranks
    * matrix \texttt{ranksTG}, \texttt{numGroups} agent groups,
    * and target queue ratio for contact type~$k$ set to
    * \texttt{targetQueueRatio[k]}.
    * The \texttt{allowCopies} flag determines if contacts
    * can be added to multiple waiting queues, while
    * the \texttt{overflowTransfer} flag determines if
    * the transfer overflow mode is active.
    * If this latter flag is \texttt{false}, overflow mode is set
    * to promotion.
    * The second flag has no effect if the first flag
    * is \texttt{false}.
    * @param numGroups the number of agent groups.
    * @param ranksTG the ranks matrix.
    * @param targetQueueRatio the target queue ratio for each contact type.
    * @param allowCopies the allow-copies flag.
    * @param overflowTransfer the overflow-transfer flag.
    * @exception NullPointerException if any argument is \texttt{null}.
    * @exception IllegalArgumentException if the length of
    * \texttt{targetQueueRatio} does not correspond to the
    * number of rows in the ranks matrix.
    */
   public QueueRatioOverflowRouter (int numGroups, double[][] ranksTG, double[] targetQueueRatio, boolean allowCopies, boolean overflowTransfer) {
      super (ranksTG.length, numGroups, numGroups);
      if (targetQueueRatio.length != numGroups)
         throw new IllegalArgumentException
         ("The length of targetQueueRatio must correspond to the number of agent groups");
      ArrayUtil.checkRectangularMatrix (ranksTG);
      this.ranksTG = ArrayUtil.deepClone (ranksTG);
      this.targetQueueRatio = targetQueueRatio.clone ();
      this.allowCopies = allowCopies;
      this.overflowTransfer = overflowTransfer;
      overflowList = RoutingTableUtils.getOverflowLists (ranksTG);
   }

   /**
    * Returns the ranks matrix defining how contacts prefer agents, used for
    * agent selection.
    *
    * @return the ranks matrix defining how contacts prefer agents.
    */
   public double[][] getRanksTG() {
      return ArrayUtil.deepClone (ranksTG);
   }

   /**
    * Sets the ranks matrix defining how contacts prefer agents to
    * \texttt{ranksTG}.
    *
    * @param ranksTG
    *           the new agent selection ranks matrix.
    * @exception NullPointerException
    *               if \texttt{ranksTG} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{ranksTG} is not rectangular or has wrong
    *               dimensions.
    */
   public void setRanksTG (double[][] ranksTG) {
      ArrayUtil.checkRectangularMatrix (ranksTG);
      if (ranksTG.length != getNumContactTypes())
         throw new IllegalArgumentException
            ("Invalid number of rows in the ranks matrix");
      if (ranksTG[0].length != getNumAgentGroups())
         throw new IllegalArgumentException
            ("Invalid number of columns in the ranks matrix");
      this.ranksTG = ArrayUtil.deepClone (ranksTG, true);
      overflowList = RoutingTableUtils.getOverflowLists (ranksTG);
   }

   /**
    * Determines if contacts can be added to multiple queues.
    * @return \texttt{true} if and only if contacts can be added to multiple queues.
    */
   public boolean isAllowCopies () {
      return allowCopies;
   }

   /**
    * Sets the allow-copies flag to \texttt{allowCopies}.
    * @param allowCopies the new value of the flag.
    * @see #isAllowCopies()
    */
   public void setAllowCopies (boolean allowCopies) {
      this.allowCopies = allowCopies;
   }

   /**
    * Determines if the overflow mode is transfer.
    * @return \texttt{true} if the overflow mode is
    * transfer.
    */
   public boolean isOverflowTransfer () {
      return overflowTransfer;
   }

   /**
    * Sets the overflow mode to \texttt{overflowTransfer}.
    * @param overflowTransfer the new overflow mode.
    * @see #isOverflowTransfer()
    */
   public void setOverflowTransfer (boolean overflowTransfer) {
      this.overflowTransfer = overflowTransfer;
   }

   /**
    * Gets the target queue ratio for each contact type.
    * @return the target queue ratios.
    */
   public double[] getTargetQueueRatio () {
      return targetQueueRatio.clone ();
   }

   /**
    * Sets the target queue ratio for each contact type to
    * \texttt{targetQueueRAtio}.
    * @param targetQueueRatio the new target queue ratios.
    * @exception IllegalArgumentException if the length of
    * \texttt{targetQueueRatio} does not correspond to the
    * number of rows in the ranks matrix.
    */
   public void setTargetQueueRatio (double[] targetQueueRatio) {
      if (targetQueueRatio.length != getNumAgentGroups ())
         throw new IllegalArgumentException
         ("The length of targetQueueRatio must correspond to the number of contact types");
      this.targetQueueRatio = targetQueueRatio.clone ();
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.AGENTGROUP;
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      if (group == null)
         return;
      if (group.getNumAgents () == 0) {
         final WaitingQueue queue = getWaitingQueue (group.getId ());
         if (queue == null)
            return;
         queue.clear (DEQUEUETYPE_NOAGENT);
      }
   }

   @Override
   public boolean canServe (int i, int k) {
      return !Double.isInfinite (ranksTG[k][i]);
   }

   @Override
   protected EndServiceEvent selectAgent (Contact contact) {
      final int k = contact.getTypeId ();
      // First phase: test each set of agent group for contact type k
      for (final int[] groupSet : overflowList[k]) {
         // Determine if an agent is available among the tested groups
         double bestScore = Double.NEGATIVE_INFINITY;
         AgentGroup bestGroup = null;
         Agent bestAgent = null;
         for (final int i : groupSet) {
            final AgentGroup group = getAgentGroup (i);
            if (group == null || group.getNumFreeAgents () == 0)
               // The group has no free agent.
               continue;
            Agent testAgent;
            double score;
            if (group instanceof DetailedAgentGroup) {
               testAgent = ((DetailedAgentGroup)group).getLongestIdleAgent ();
               score = testAgent.getIdleTime ();
            }
            else {
               testAgent = null;
               score = group.getNumFreeAgents ();
            }
            if (score > bestScore) {
               bestScore = score;
               bestGroup = group;
               bestAgent = testAgent;
            }
         }
         if (bestAgent != null)
            // First phase successful: we have found an agent for the contact
            return bestAgent.serve (contact);
         else if (bestGroup != null)
            // First phase successful: we have found an agent for the contact
            return bestGroup.serve (contact);

         // No agent, so try to select a waiting queue
         double smallestQueueRatio = Double.POSITIVE_INFINITY;
         int bestQueue = -1;
         for (final int i : groupSet) {
            final double queueRatio = getQueueRatio (i);
            if (queueRatio > targetQueueRatio[i])
               // The queue ratio is too high, skip to next queue
               continue;
            if (queueRatio < smallestQueueRatio) {
               smallestQueueRatio = queueRatio;
               bestQueue = i;
            }
         }
         if (bestQueue >= 0) {
            final boolean[] queues = getRoutingAttributes (contact, true).queues;
            if (allowCopies) {
               // The contact is queued in all queues for the groupset
               for (final int i : groupSet)
                  queues[i] = true;
               if (!overflowTransfer)
                  for (final int[] groupSet2 : overflowList[k]) {
                     if (groupSet2 == groupSet)
                        break;
                     for (final int i : groupSet2)
                        queues[i] = true;
                  }
            }
            else
               // Contact cannot be sent to multiple queues.
               queues[bestQueue] = true;
            // First phase successful: we have found a queue for the contact
            return null;
         }
      }

      // Phase 1 failed, so entering phase 2.
      // No agent group or queue could be found for this contact.
      // Search for the queue with the best queue ratio
      double smallestQueueRatio = Double.POSITIVE_INFINITY;
      int bestQueue = -1;
      for (int i = 0; i < ranksTG[k].length; i++) {
         if (Double.isInfinite (ranksTG[k][i]))
            continue;
         if (allowCopies && !overflowTransfer)
            // Promotion mode: queue in all waiting queues
            getRoutingAttributes (contact, true).queues[i] = true;
         else {
            final double queueRatio = getQueueRatio (i);
            if (queueRatio < smallestQueueRatio) {
               smallestQueueRatio = queueRatio;
               bestQueue = i;
            }
         }
      }
      if (bestQueue >= 0) {
         // In promotion mode, this will be -1.
         final boolean[] queues = getRoutingAttributes (contact, true).queues;
         queues[bestQueue] = true;
      }

      return null;
   }

   private double getQueueRatio (int i) {
      final AgentGroup group = getAgentGroup (i);
      final int numAgents;
      if (group != null)
         numAgents = group.getNumAgents ();
      else
         numAgents = 0;
      final WaitingQueue queue = getWaitingQueue (i);
      final int queueSize;
      if (queue != null)
         queueSize = queue.size ();
      else
         queueSize = 0;
      final double queueRatio = (queueSize + 1.0) / numAgents;
      return queueRatio;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact contact) {
      final RoutingAttributes a = getRoutingAttributes (contact, false);
      if (a == null)
         // No queueing information for this contact.
         // The contact will be blocked.
         return null;
      DequeueEvent firstEvent = null;
      for (int q = 0; q < a.queues.length; q++) {
         if (!a.queues[q])
            continue;
         final WaitingQueue queue = getWaitingQueue (q);
         if (queue == null)
            continue;
         ++a.numQueues;
         if (firstEvent == null) {
            // This also generates a patience time
            a.events[q] = firstEvent = queue.add (contact);
            if (firstEvent.dequeued ())
               // Balking, we do not queue the contact elsewhere
               return firstEvent;
         }
         else
            a.events[q] = queue.add (contact, contact.simulator ().time (), firstEvent.getScheduledQueueTime (), firstEvent.getScheduledDequeueType ());
      }
      return firstEvent;
   }

   @Override
   protected void dequeued (DequeueEvent ev) {
      if (ev.getEffectiveDequeueType () == DEQUEUETYPE_FANTOM)
         return;
      final RoutingAttributes a = getRoutingAttributes (ev.getContact (), false);
      if (a != null) {
         --a.numQueues;
         if (a.numQueues > 0) {
            if (ev.getEffectiveDequeueType () == DEQUEUETYPE_NOAGENT)
               // This copy exits the queue because of an insufficient number of
               // agents, but other copies might still stay in queue.
               return;
            // Exit the contact from the other queues, if
            // there are multiple copies.
            for (int q = 0; q < a.events.length; q++) {
               if (q == ev.getWaitingQueue ().getId ())
                  // This is the queue the contact exited from
                  // when dequeued was called.
                  continue;
               if (a.events[q] != null) {
                  // Calling this method also generates a dequeue-event which
                  // is listened by the router, and results in a recursive call
                  // to this method.
                  // Consequently, we use a special dequeue type to
                  // indicate the recursively called method to
                  // return immediately, avoiding infinite
                  // loops.
                  a.events[q].remove (DEQUEUETYPE_FANTOM);
                  --a.numQueues;
               }
            }
            assert a.numQueues == 0;
         }
      }
      super.dequeued (ev);
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final int q = group.getId ();
      final WaitingQueue queue = getWaitingQueue (q);
      if (queue == null || queue.isEmpty ())
         return null;
      return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   public String getDescription() {
      return "Queue ratio overflow router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Ranks matrix defining how contacts prefer agents\n");
      sb.append (RoutingTableUtils.formatRanksTG (ranksTG)).append ("\n");
      return sb.toString();
   }

   private RoutingAttributes getRoutingAttributes (Contact contact, boolean create) {
      RoutingAttributes a = (RoutingAttributes)contact.getAttributes ().get (this);
      if (a == null && create) {
         a = new RoutingAttributes (getNumWaitingQueues ());
         contact.getAttributes ().put (this, a);
      }
      return a;
   }

   private static class RoutingAttributes {
      int numQueues;
      boolean[] queues;
      DequeueEvent[] events;

      public RoutingAttributes (int numQueues) {
         queues = new boolean[numQueues];
         events = new DequeueEvent[numQueues];
      }
   }
}
