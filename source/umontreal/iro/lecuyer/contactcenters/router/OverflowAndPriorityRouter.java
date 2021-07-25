package umontreal.iro.lecuyer.contactcenters.router;

import java.util.Arrays;
import java.util.Comparator;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import static umontreal.iro.lecuyer.contactcenters.router.Router.DEQUEUETYPE_NOAGENT;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents a routing policy allowing contacts to overflow from
 * one set of
 * agents to another, and agents to pick out queued
 * contacts based on priorities that can change at predefined moments
 * during the waiting time.
 * This routing policy also supports some forms of conditional routing.
 * However, the router using this policy might be slow, because
 * of the more complex management of queues.
 * Therefore, if conditional routing is not needed, or if priorities do not change with time,
 * it might be faster to use a simpler policy such as
 *  {@link AgentsPrefRouter} or {@link AgentsPrefRouterWithDelays}.
 * The latter policy also supports some forms of priorities changing with time.
 *
 * We now describe the policy in details.
 * The agent selection
 * of any new contact $C$ of type $k$ using this policy is based
 * on a sequence of stages.  Each stage is defined by
 * a triplet $(w_{k,j}, f_{k,j}(X,C), g_{k,j}(X,C))$ where
 * $w_{k,j}$ is a minimal waiting
 * time, $f_{k,j}(X,C)$ is a function returning a
 * vector of ranks for agent selection, and
 * $g_{k,j}(X,C)$ is another function returning a vector of
 * ranks for queueing.
 * For any call type $k=0,\ldots,K-1$, we have
 * $0\le w_{k,0}<w_{k,1}<\cdots$.
 * Often, we have $f_{k,j}=g_{k,j}$.
 * The vectors returned by these functions can depend on the contact but
 * also on the state $X$ of the system, which allows the implementation of
 * some forms of conditional routing.
 *
 * More specifically,
 * when a contact of type $k$ arrives, the router checks the first
 * triplet $(w_{k,0}, f_{k,0}, g_{k,0})$. If $w_{k,0}>0$, the contact
 * waits for $w_{k,0}$ time units in an extra waiting queue no
 * agent has access to; this
 * can be used to model a positive routing delay.
 * Then, the function $f_{k,0}(X,C)$ is evaluated on the new contact $C$ to
 * get a vector of ranks $(r_0, \ldots, r_{I-1})$.
 * These ranks determine which agent groups can be selected for the new
 * contact, and the priority for each group.
 * The smaller is $r_i$, the higher is the priority for the agent group
 * $i$.
 * If $r_i=\infty$, the contact cannot be sent to agent group $i$ at this
 * stage of routing.
 *
 * The router selects the agent group with the smallest value $r_i$ among
 * the groups containing at least one free agent.
 * If a single group with this minimal rank exists, the contact is sent
 * to a free agent in it, and routing is done.
 * Otherwise, a score $S_i$ is associated with each group with minimal
 * rank, and the group with the highest score is selected.
 * Usually, the score corresponds to the longest idle time of agents in
 * the group.
 *
 * If no agent group can be assigned to the new contact,
 * the contact is put into one or more waiting queues.
 * There is one priority queue per agent group, and an extra
 * queue storing contacts not queued to any agent group.
 * To select the waiting queues, the router applies
 * the function $g_{k,0}(X,C)$ on the new contact to get
 * a vector $(q_0,\ldots,q_{I-1})$ of ranks.
 * The rank $q_i$ determines the priority of the contact in queue $i$.
 * The smaller is the rank, the higher is the priority.
 * An infinite rank $q_i$ prevents the contact to be put in queue $i$.
 * Often, the priority is the same for every waiting queue
 * allowed for the contact, but priorities may differ in general.
 * If all ranks $q_i$ are infinite, the contact goes into the extra queue.
 *
 * When an agent becomes free, it looks for a contact in the queue
 * associated with its group only.
 * The contacts in this queue are sorted in increasing order of rank.
 * Contacts sharing the same rank are sorted in decreasing order of score.
 * The default function for the score is the time spent in queue.
 * When a contact is removed from a queue, it is also removed
 * from every other queue managed by the router.
 *
 * If the contact waits for $w_1$ time units in queue without abandoning
 * or being served, a new agent selection happens.
 * The selection is similar to the first one, except that
 * a new function, $f_{k,1}(X,C)$, is used to generate the vector of ranks.
 * The ranks can thus evolve with time.
 * If no agent group is available for the contact at this second stage
 * of routing,
 * a waiting queue update occurs.
 * For this, a vector of ranks is generated using $g_{k,1}(X,C)$, and used
 * to determine the new priority of the contact, for each queue.
 * If the priority $q_i$ goes from an infinite to a finite value, the
 * contact joins queue $i$.
 * If the priority goes from a finite to an infinite value,
 * the contact leaves queue $i$.
 * If the priority changes from a finite value to another finite value,
 * the position of the contact in queue is updated.
 * The priority of a contact can thus evolve with time.
 * This process is repeated at waiting time
 * $w_2$, $w_3$, and so on, for all stages of routing.
 *
 * A contact leaving all waiting queues linked to agent groups at a given stage is
 * put into the extra waiting queue.
 * It can still abandon, but it cannot be served until
 * a subsequent stage of routing puts it back into a waiting queue
 * linked to an agent group.
 * On the other hand, if a contact enters a queue
 * linked to an agent group at a given stage of routing,
 * it leaves the extra queue.
 * Moreover, even if the contact changes queue, it keeps
 * the same residual patience time; changing waiting queue
 * does not reset the maximal queue time.
 *
 * For example, suppose that a contact
 * of type $k$ can be served by two
 * agent groups, $0$ and $1$.
 * A newly arrived contact has access to group $0$ only, and
 * is queued with priority 1 if it cannot be served immediately.
 * However, after $s$ seconds of wait, the contact gains access
 * to group $1$. It is queued to this new group with priority 1, but
 * the priority with original group $0$ changes to 2 (a lower priority).
 * The parameters for such a routing would be
 * $(0, (1, \infty), (1, \infty))$,
 * $(s, (1, 1), (2, 1))$.
 * For an example with conditional routing, suppose
 * that at waiting time $s$, the priorities depend
 * on the service level observed in the last $m$ minutes.
 */
public class OverflowAndPriorityRouter extends Router {
   private AgentSelectionScore agentSelectionScore = AgentSelectionScore.LONGESTIDLETIME;
   private ContactSelectionScore contactSelectionScore = ContactSelectionScore.LONGESTWAITINGTIME;
   private RoutingStageInfo[][] stages;
   private double[][] weightsTG;
   private double[][] weightsGT;

   /**
    * Constructs a new overflow and priority router
    * with \texttt{numGroups} agent groups, and
    * \texttt{stages} for information about routing stages.
    * The 2D array \texttt{stages} must contain $K$ rows, each
    * row giving a routing script for a specific contact type.
    * @param numGroups the number of agent groups.
    * @param stages the information about routing stages.
    * @exception NullPointerException if \texttt{stages} is
    * \texttt{null}.
    * @exception IllegalArgumentException if \texttt{numGroups}
    * is negative or a list of stages are not ordered
    * with respect to waiting time, for at least one
    * contact type.
    */
   public OverflowAndPriorityRouter (int numGroups, RoutingStageInfo[][] stages) {
      super (stages.length, numGroups + 1, numGroups);
      for (int k = 0; k < stages.length; k++) {
         if (stages[k] == null)
            throw new NullPointerException();
         for (int j = 1; j < stages[k].length; j++) {
            if (stages[k][j-1].getWaitingTime () > stages[k][j].getWaitingTime ())
               throw new IllegalArgumentException ("Minimal waiting times must be non-decreasing");
         }
      }
      this.stages = stages;
      weightsTG = new double[stages.length][numGroups];
      weightsGT = new double[numGroups][stages.length];
      for (int k = 0; k < stages.length; k++)
         for (int i = 0; i < numGroups; i++)
            weightsTG[k][i] = weightsGT[i][k] = 1;
   }

   /**
    * Returns the matrix of weights defining $\wTG(k, i)$
    * for each contact type and agent group.
    * These weights are used by
    * {@link #getScoreForAgentSelection(Contact,AgentGroup,Agent)}
    * to compute scores for agent groups, and default to 1 if
    * they are not set by {@link #setWeightsTG(double[][])}.
    *
    * @return the matrix of weights defining $\wTG(k, i)$.
    */
   public double[][] getWeightsTG () {
      return ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Sets the matrix of weights defining $\wTG(k, i)$
    * for each $k$ and $i$ to \texttt{weightsTG}.
    *
    * @param weightsTG
    *           the new matrix of weights defining $\wTG(k, i)$.
    * @exception NullPointerException
    *               if \texttt{weightsTG} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{weightsTG} is not rectangular or has wrong
    *               dimensions.
    */
   public void setWeightsTG (double[][] weightsTG) {
      ArrayUtil.checkRectangularMatrix (weightsTG);
      if (weightsTG.length != getNumContactTypes ()
            || weightsTG[0].length != getNumAgentGroups ())
         throw new IllegalArgumentException ("Invalid dimensions of weightsTG");
      this.weightsTG = ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Returns the matrix of weights defining $\wGT(i, k)$ for
    * each contact type and agent group.
    * These weights are used by {@link #getScoreForContactSelection(DequeueEvent)}
    * to give scores to waiting queues, and default to
    * 1 if they are not set by {@link #setWeightsGT(double[][])}.
    *
    * @return the matrix of weights defining $\wGT(i, k)$.
    */
   public double[][] getWeightsGT () {
      return ArrayUtil.deepClone (weightsGT, true);
   }

   /**
    * Sets the matrix of weights defining $\wGT(i, k)$
    * for each $k$ and $i$ to \texttt{weightsGT}.
    *
    * @param weightsGT
    *           the new matrix of weights defining $\wGT(i, k)$.
    * @exception NullPointerException
    *               if \texttt{weightsGT} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{weightsGT} is not rectangular or has wrong
    *               dimensions.
    */
   public void setWeightsGT (double[][] weightsGT) {
      ArrayUtil.checkRectangularMatrix (weightsGT);
      if (weightsGT.length != getNumAgentGroups ()
            || weightsGT[0].length != getNumContactTypes ())
         throw new IllegalArgumentException ("Invalid dimensions of weightsGT");
      this.weightsGT = ArrayUtil.deepClone (weightsGT, true);
   }

   /**
    * Returns the current mode of computation for the agent selection score. The
    * default value is {@link AgentSelectionScore#LONGESTIDLETIME}.
    *
    * @return the way the score is computed for agent selection.
    */
   public AgentSelectionScore getAgentSelectionScore () {
      return agentSelectionScore;
   }

   /**
    * Sets the way scores for agent selection are computed to
    * \texttt{agentSelectionScore}.
    *
    * @param agentSelectionScore
    *           the way scores for agent selection are computed.
    * @exception NullPointerException
    *               if \texttt{agentSelectionScore} is \texttt{null}.
    */
   public void setAgentSelectionScore (AgentSelectionScore agentSelectionScore) {
      if (agentSelectionScore == null)
         throw new NullPointerException ();
      this.agentSelectionScore = agentSelectionScore;
   }

   /**
    * Returns the current mode of computation for the contact selection score.
    * The default value is {@link ContactSelectionScore#LONGESTWAITINGTIME}.
    *
    * @return the way the score is computed for contact selection.
    */
   public ContactSelectionScore getContactSelectionScore () {
      return contactSelectionScore;
   }

   /**
    * Sets the way scores for contact selection are computed to
    * \texttt{contactSelectionScore}.
    *
    * @param contactSelectionScore
    *           the way scores for contact selection are computed.
    * @exception NullPointerException
    *               if \texttt{contactSelectionScore} is \texttt{null}.
    */
   public void setContactSelectionScore (
         ContactSelectionScore contactSelectionScore) {
      if (contactSelectionScore == null)
         throw new NullPointerException ();
      this.contactSelectionScore = contactSelectionScore;
   }

   @Override
   public boolean canServe (int i, int k) {
      for (int j = 0; j < stages[k].length; j++)
         if (stages[k][j].getRankFunctionForAgentSelection ().canReturnFiniteRank (i))
            return true;
      return false;
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      if (group.getNumAgents() == 0) {
         final int gid = group.getId ();
         final WaitingQueue queue = getWaitingQueue (gid);
         queue.clear (DEQUEUETYPE_NOAGENT);
      }
   }

   @Override
   protected EndServiceEvent selectAgent (Contact contact) {
      RoutingInfo info = (RoutingInfo)contact.getAttributes ().get (this);
      if (info == null) {
         info = new RoutingInfo (getNumAgentGroups ());
         contact.getAttributes ().put (this, info);
      }
      final int k = contact.getTypeId();
      if (stages[k][0].getWaitingTime () > 0)
         return null;
      if (!stages[k][0].getRankFunctionForAgentSelection ().updateRanks (contact, info.getRanksForAgentSelectionArray ()))
         return null;
      final EndServiceEvent es = selectAgent (contact, info);
      if (es == null)
         return null;
      info.oneStageDone ();
      return es;
   }

   protected EndServiceEvent selectAgent (Contact contact, RoutingInfo info) {
      final double[] ranks = info.getRanksForAgentSelectionArray ();
      double bestRank = Double.POSITIVE_INFINITY, bestScore = Double.NEGATIVE_INFINITY;
      AgentGroup bestGroup = null;
      Agent bestAgent = null;
      for (int i = 0; i < ranks.length; i++) {
         if (Double.isInfinite (ranks[i]))
            continue;
         final AgentGroup grp = getAgentGroup (i);
         if (grp.getNumFreeAgents () == 0)
            continue;
         final Agent testAgent;
         if (grp instanceof DetailedAgentGroup)
            testAgent = ((DetailedAgentGroup)grp).getLongestIdleAgent ();
         else
            testAgent = null;
         if (ranks[i] <= bestRank) {
            final double score = getScoreForAgentSelection (contact, grp, testAgent);
            if (ranks[i] < bestRank || score > bestScore) {
               bestRank = ranks[i];
               bestScore = score;
               bestGroup = grp;
               bestAgent = testAgent;
            }
         }
      }
      if (bestAgent != null)
         return bestAgent.serve (contact);
      else if (bestGroup != null)
         return bestGroup.serve (contact);
      return null;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact contact) {
      final int k = contact.getTypeId ();
      if (stages[k][0].getWaitingTime () > 0)
         // Put the contact in the dummy queue
         return getWaitingQueue (getNumWaitingQueues () - 1).add (contact);
      final RoutingInfo info = (RoutingInfo)contact.getAttributes ().get (this);
      if (!stages[k][0].getRankFunctionForContactSelection ().updateRanks (contact, info.getRanksForContactSelectionArray ())) {
         Arrays.fill (info.getRanksForContactSelectionArray (), Double.POSITIVE_INFINITY);
         // Put the contact in the dummy queue
         info.oneStageDone ();
         return getWaitingQueue (getNumWaitingQueues () - 1).add (contact);
      }
      info.oneStageDone ();
      return selectWaitingQueue (contact, info);
   }

   protected DequeueEvent selectWaitingQueue (Contact contact, RoutingInfo info) {
      DequeueEvent ev = null;
      final double[] ranks = info.getRanksForContactSelectionArray ();
      for (int i = 0; i < ranks.length; i++) {
         if (Double.isInfinite (ranks[i]))
            continue;
         ev = getWaitingQueue (i).add (contact);
         if (ev.dequeued ())
            // Balking occurred
            return ev;
         info.setDequeueEvent (i, ev);
      }
      if (ev == null)
         // Add to dummy waiting queue
         ev = getWaitingQueue (getNumWaitingQueues () - 1).add (contact);
      return ev;
   }

   @Override
   public Comparator<? super DequeueEvent> getNeededWaitingQueueComparator (
         int q) {
      if (q == getNumAgentGroups ())
         return null;
      return new ComparatorForQueue (this, q);
   }

   @Override
   public WaitingQueueStructure getNeededWaitingQueueStructure (int q) {
      if (q == getNumAgentGroups ())
         // The last (dummy) waiting queue is a list
         return WaitingQueueStructure.LIST;
      return WaitingQueueStructure.PRIORITY;
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.AGENTGROUP;
   }

   @Override
   public boolean needsDetailedAgentGroup (int i) {
      return agentSelectionScore == AgentSelectionScore.LONGESTIDLETIME;
   }

   @Override
   protected double getReroutingDelay (DequeueEvent dqEv, int numReroutingsDone) {
      RoutingInfo info = (RoutingInfo)dqEv.getContact ().getAttributes ().get (this);
      final int k = dqEv.getContact ().getTypeId ();
      final int stage = info.getStagesDone ();
      if (stages[k].length <= stage)
         return Double.POSITIVE_INFINITY;
      final double w = stages[k][stage].getWaitingTime ();
      double wt = dqEv.simulator ().time () - dqEv.getEnqueueTime ();
      return Math.max (0, w - wt);
   }

   @Override
   protected EndServiceEvent selectAgent (DequeueEvent dqEv,
         int numReroutingsDone) {
      final RoutingInfo info = (RoutingInfo)dqEv.getContact ().getAttributes ().get (this);
      final int k = dqEv.getContact ().getTypeId ();
      final int stage = info.getStagesDone ();
      if (stages[k].length <= stage)
         return null;
      if (!stages[k][stage].getRankFunctionForAgentSelection ().updateRanks (dqEv.getContact (), info.getRanksForAgentSelectionArray ()))
         return null;
      final EndServiceEvent es = selectAgent (dqEv.getContact (), info);
      if (es == null)
         return null;
      info.oneStageDone ();
      return es;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (DequeueEvent dqEv,
         int numReroutingsDone) {
      // Gather necessary information to put a contact back in queue,
      // with a different priority. Without this precaution,
      // the patience time would be reset each time the contact
      // changes queue or priority.
      assert !dqEv.dequeued ();
      final double enqueueTime = dqEv.getEnqueueTime ();
      final double timeOfAbandonment = dqEv.time ();
      final double remainingPatience;
      if (timeOfAbandonment < 0)
         // Unscheduled dequeue event, infinite patience
         remainingPatience = Double.POSITIVE_INFINITY;
      else {
         remainingPatience = timeOfAbandonment - dqEv.simulator ().time ();
         if (remainingPatience <= 0) {
            // Non-positive patience time results in abandonment.
            dqEv.remove (dqEv.getScheduledDequeueType ());
            return null;
         }
      }
      final int dqType = dqEv.getScheduledDequeueType ();

      final RoutingInfo info = (RoutingInfo)dqEv.getContact ().getAttributes ().get (this);
      final int k = dqEv.getContact ().getTypeId ();
      final int stage = info.getStagesDone ();
      if (stages[k].length <= stage)
         return dqEv;
      if (!stages[k][stage].getRankFunctionForContactSelection ().updateRanks (dqEv.getContact (), info.getNewRanksForContactSelectionArray ())) {
         info.oneStageDone ();
         return dqEv;
      }
      info.oneStageDone ();
      final double[] qRanks = info.getRanksForContactSelectionArray ();
      final double[] newRanks = info.getNewRanksForContactSelectionArray ();
      DequeueEvent newEv = dqEv;
      if (info.getNumQueues () == 0) {
         // Contact in dummy queue
         for (int i = 0; i < newRanks.length; i++) {
            if (Double.isInfinite (newRanks[i]))
               continue;
            if (!dqEv.dequeued ()) {
               // Contact leaves the dummy only if some queue
               // with a finite rank exists.
               getWaitingQueue (getNumWaitingQueues () - 1).remove (dqEv, DEQUEUETYPE_FANTOM);
            }
            qRanks[i] = newRanks[i];
            info.setDequeueEvent (i, newEv = getWaitingQueue (i).add (dqEv.getContact (), enqueueTime, remainingPatience, dqType));
         }
      }
      else {
         for (int i = 0; i < newRanks.length; i++) {
            if (Double.isInfinite (newRanks[i]) && Double.isInfinite (qRanks[i]))
               // Contact is not in queue i before and after this stage
               continue;
            if (Double.isInfinite (newRanks[i])) {
               // Contact was in queue i before, but now, it is not
               if (info.getDequeueEvent(i) != null)
                  info.getDequeueEvent (i).remove (DEQUEUETYPE_FANTOM);
               info.setDequeueEvent (i, null);
               qRanks[i] = newRanks[i];
            }
            else if (Double.isInfinite (qRanks[i])) {
               // Contact was not in queue i before, but now it is
               qRanks[i] = newRanks[i];
               info.setDequeueEvent (i, newEv = getWaitingQueue (i).add (dqEv.getContact (), enqueueTime, remainingPatience, dqType));
            }
            else if (newRanks[i] != qRanks[i]) {  
               // if DequeueEvent is null, then this call is no longer in queue.
               // the number of agents fell to 0 at some point, 
               if (info.getDequeueEvent(i) == null) { 
                  assert info.getStagesDone() > 1: "Error: this dequeueEvent should not be NULL on the 1 stage!";
                  // Do not add back this call into queue, if there are no agents in this group.
                  if (getAgentGroup(i).getNumAgents() <= 0)
                    continue;
               }
               else {
                  // A change of priority occurs
                  info.getDequeueEvent (i).remove (DEQUEUETYPE_FANTOM);
               }
               // The contact must be removed before changing its priority, otherwise
               // the behavior of the data structure used for the queue will
               // be undefined.
               qRanks[i] = newRanks[i];
               info.setDequeueEvent (i, newEv = getWaitingQueue (i).add (dqEv.getContact (), enqueueTime, remainingPatience, dqType));
            }
         }
         if (info.getNumQueues () == 0)
            // Contact is not in any queue after this stage, so
            // add it to dummy queue to keep at trace of it.
            newEv = getWaitingQueue (getNumWaitingQueues () - 1).add (dqEv.getContact (), enqueueTime, remainingPatience, dqType);
      }
      if (dqEv.dequeued ())
         return newEv;
      // If dqEv.dequeued and we return an event different from dqEv,
      // ContactReroutingEvent will call
      // dqEv.remove, which results in bad routing.
      return dqEv;
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final int gid = group.getId ();
      final WaitingQueue queue = getWaitingQueue (gid);
      if (queue.isEmpty ())
         return null;
//      DequeueEvent firstEv = null;
//      for (DequeueEvent ev : queue) {
//         if (ev.dequeued ())
//            continue;
//         if (firstEv == null)
//            firstEv = ev;
//         else if (ev.compareTo (firstEv) < 0)
//            firstEv = ev;
//      }
//      final DequeueEvent firstEv2 = queue.getFirst ();
//      if (firstEv2 != firstEv) {
//         System.out.println (new ComparatorForQueue (this, queue.getId ()).compare (firstEv, firstEv2));
//         throw new AssertionError();
//      }
      final DequeueEvent ev = queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
      // Contact is removed from other queues automatically,
      // because of the call to the following dequeued method.
//      // Remove additional copies of the contacts in queues
//      RoutingInfo info = (RoutingInfo)ev.getContact ().getAttributes ().get (this);
//      if (info != null)
//         for (DequeueEvent ev2 : info.getDequeueEventsArray ()) {
//            if (ev2 != null && !ev2.dequeued ())
//               ev2.remove (DEQUEUETYPE_FANTOM);
//         }
      return ev;
   }

   @Override
   protected void dequeued (DequeueEvent ev) {
      if (ev.getEffectiveDequeueType () == DEQUEUETYPE_FANTOM)
         return;
      final RoutingInfo info = (RoutingInfo)ev.getContact ().getAttributes ().get (this);
      if (info.getNumQueues () > 0) {
         final int qid = ev.getWaitingQueue ().getId ();
         if (qid < getNumAgentGroups () && info.getDequeueEvent (qid) == ev)
            info.setDequeueEvent (qid, null);
         for (int q = 0; q < getNumWaitingQueues () - 1 && info.getNumQueues () > 0; q++) {
            DequeueEvent ev2 = info.getDequeueEvent (q);
            if (ev2 == null)
               continue;
            if (ev2 != ev && ev.getEffectiveDequeueType () == DEQUEUETYPE_NOAGENT)
               // This copy exits the queue because of an insufficient number of
               // agents, but other copies might still stay in queue.
               return;
            if (!ev2.dequeued ())
               ev2.remove (DEQUEUETYPE_FANTOM);
            info.setDequeueEvent (q, null);
         }
      }
      super.dequeued (ev);
   }

   /**
    * Returns the score for contact \texttt{ct} associated with agent group
    * \texttt{testGroup} and agent \texttt{testAgent}. When selecting an agent
    * for contact \texttt{ct}, if there are several agent groups with the same
    * minimal rank, the agent group with the greatest score is selected.
    * Returning a negative infinite score prevents an agent group from being
    * selected.
    *
    * By default, this returns a score depending on the return value of
    * {@link #getAgentSelectionScore()}. This can return the longest weighted
    * idle time (the default), the weighted number of free agents, or the weight
    * only. See {@link AgentSelectionScore} for more information.
    *
    * @param ct
    *           the contact being assigned an agent.
    * @param testGroup
    *           the tested agent group.
    * @param testAgent
    *           the tested agent, can be \texttt{null}.
    * @return the score given to the association between the contact and the
    *         agent.
    */
   protected double getScoreForAgentSelection (Contact ct,
         AgentGroup testGroup, Agent testAgent) {
      final int k = ct.getTypeId ();
      final int i = testGroup.getId ();
      final double w = weightsTG[k][i];
      switch (agentSelectionScore) {
      case WEIGHTONLY:
         return w;
      case NUMFREEAGENTS:
         return w * testGroup.getNumFreeAgents ();
      case LONGESTIDLETIME:
         final double s;
         if (testAgent == null)
            //s = testGroup.getNumFreeAgents ();
            throw new IllegalStateException
               ("Unavailable longest idle time of agents in group " + testGroup.getId() +
                "; use detailed agent groups");
         else
            s = testAgent.getIdleTime ();
         return s * w;
      }
      throw new AssertionError ();
   }

   /**
    * Returns the score for the
    * queued contact represented by \texttt{ev}.
    *
    * By default, this returns a score depending on the return value of
    * {@link #getContactSelectionScore()}. This can return the weighted waiting
    * time (the default), the weighted number of queued agents, or the weight
    * only. See {@link ContactSelectionScore} for more information.
    *
    * @param ev
    *           the dequeue event.
    * @return the assigned score.
    */
   protected double getScoreForContactSelection (DequeueEvent ev) {
      final int i = ev.getWaitingQueue ().getId ();
      final int k = ev.getContact ().getTypeId ();
      final double w = weightsGT[i][k];
      switch (contactSelectionScore) {
      case WEIGHTONLY:
         return w;
      case LONGESTWAITINGTIME:
         // For contacts marked to be removed from the queue,
         // this gives a value larger than the true waiting time
         // of the contact. But if we use the true waiting times,
         // the order relationship induced by the score function changes with
         // simulation time, which breaks the queues
         // implemented as heaps.
         final double W = ev.simulator ().time () - ev.getEnqueueTime ();
         return w * W;
      case QUEUESIZE:
         final WaitingQueue queue = ev.getWaitingQueue ();
         int s = queue.size ();
         //if (ev == queue.getFirst ())
            return w * s;
//         else
//            for (final DequeueEvent testEv : queue)
//               if (ev == testEv)
//                  return w * s;
//               else
//                  --s;
      }
      throw new AssertionError ();
   }


   /**
    * Represents information about the routing for a particular contact.
    * When this router processes a contact, it creates an instance of
    * this class and associates it to the contact.
    * This instance can be retrieved by using
    * \texttt{contact.getAttributes().get (router)}, where
    * \texttt{router} is the corresponding router.
    */
   public static final class RoutingInfo {
      private int numStagesDone = 0;
      private int numGroups;
      private double[] aRanks;
      private double[] qRanks;
      private double[] qRanks2;
      private DequeueEvent[] dqEv;
      private int numQueues;

      /**
       * Constructs a new routing information object
       * for a system with \texttt{numGroups}
       * agent groups.
       * @param numGroups the number of agent groups.
       */
      public RoutingInfo (int numGroups) {
         if (numGroups < 0)
            throw new IllegalArgumentException ("The number of agent groups cannot be negative");
         this.numGroups = numGroups;
      }

      /**
       * Returns an array containing the ranks associated with this
       * contact for the last agent selection.
       * The first time this method is called, an array
       * is created and returned.
       * This array can then be filled with ranks.
       * @return the ranks for agent selection.
       */
      public double[] getRanksForAgentSelectionArray() {
         if (aRanks == null) {
            aRanks = new double[numGroups];
            Arrays.fill (aRanks, Double.POSITIVE_INFINITY);
            return aRanks;
         }
         return aRanks;
      }

      /**
       * Similar to {@link #getRanksForAgentSelectionArray()}, for
       * the ranks used by waiting queue selection.
       * @return the ranks for waiting queues.
       */
      public double[] getRanksForContactSelectionArray() {
         if (qRanks == null) {
            qRanks = new double[numGroups];
            Arrays.fill (qRanks, Double.POSITIVE_INFINITY);
            return qRanks;
         }
         return qRanks;
      }

      /**
       * Similar to {@link #getRanksForAgentSelectionArray()}, for
       * the ranks used by waiting queue selection.
       * This array is used when a new vector of ranks is generated, to
       * be compared with the original vector of ranks giving
       * the priorities of the contacts currently in queues.
       * @return the ranks for waiting queues.
       */
      public double[] getNewRanksForContactSelectionArray() {
         if (qRanks2 == null) {
            qRanks2 = new double[numGroups];
            if (qRanks == null)
               Arrays.fill (qRanks2, Double.POSITIVE_INFINITY);
            else
               System.arraycopy (qRanks, 0, qRanks2, 0, numGroups);
            return qRanks2;
         }
         return qRanks2;
      }

      /**
       * Similar to {@link #getRanksForAgentSelectionArray()}, but
       * returns an array of dequeue events.
       * These events represent the contacts in each
       * waiting queue bound to the router.
       * @return the array of dequeue events.
       */
      private DequeueEvent[] getDequeueEventsArray() {
         if (dqEv == null)
            return dqEv = new DequeueEvent[numGroups];
         return dqEv;
      }

      /**
       * Returns the event representing
       * the associated contact in queue \texttt{q}.
       * If the contact is not in this queue, this
       * returns \texttt{null}.
       * @param q the index of the tested queue.
       * @return the dequeue event.
       */
      public DequeueEvent getDequeueEvent (int q) {
         return getDequeueEventsArray ()[q];
      }

      /**
       * Sets the dequeue event for queue
       * \texttt{q} to \texttt{ev}.
       * @param q the index of the waiting queue.
       * @param ev the dequeue event.
       */
      public void setDequeueEvent (int q, DequeueEvent ev) {
         final DequeueEvent[] evs = getDequeueEventsArray ();
         if (evs[q] == null && ev != null)
            ++numQueues;
         if (evs[q] != null && ev == null)
            --numQueues;
         evs[q] = ev;
      }

      /**
       * Returns the number of waiting queues the
       * contact is in.
       */
      public int getNumQueues() {
         return numQueues;
      }

      /**
       * Returns the number of agent selections performed so far
       * for this contact.
       */
      public int getStagesDone() {
         return numStagesDone;
      }

      /**
       * Indicates that a new agent selection was just done for
       * the contact.
       */
      public void oneStageDone() {
         ++numStagesDone;
      }
   }

   private static final class ComparatorForQueue implements Comparator<DequeueEvent> {
      private OverflowAndPriorityRouter router;
      private int i;

      public ComparatorForQueue (OverflowAndPriorityRouter router, int i) {
         this.router = router;
         this.i = i;
      }

      public int compare (DequeueEvent o1, DequeueEvent o2) {
         final Contact contact1 = o1.getContact ();
         final RoutingInfo info1 = (RoutingInfo)contact1.getAttributes ().get (router);
         final double p1 = info1 == null ? Double.POSITIVE_INFINITY : info1.getRanksForContactSelectionArray ()[i];
         final Contact contact2 = o2.getContact ();
         final RoutingInfo info2 = (RoutingInfo)contact2.getAttributes ().get (router);
         final double p2 = info2 == null ? Double.POSITIVE_INFINITY : info2.getRanksForContactSelectionArray ()[i];
         if (p1 < p2)
            return -1;
         if (p2 < p1)
            return 1;
         final double s1 = router.getScoreForContactSelection (o1);
         final double s2 = router.getScoreForContactSelection (o2);
         if (s1 > s2)
            return -1;
         if (s1 < s2)
            return 1;
         if (contact1.getArrivalTime () < contact2.getArrivalTime ())
            return -1;
         if (contact2.getArrivalTime () < contact1.getArrivalTime ())
            return 1;
         return 0;
      }
   }
}
