package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This router uses a queue-at-last-group policy.
 * When a new contact of type~$k$ arrives, the serving agent
 * is selected the same way as with queue priority routing
 * policy: each agent group $i_{k, 0}, i_{k, 1}, \ldots$ of the type-to-group
 * map is tested to find a free agent.
 * However, if no agent can serve the contact, the contact is put
 * into a waiting queue associated with the last agent group
 * in the ordered list rather than the contact type.
 * As usual, if the router's queue capacity is exceeded, the contact is blocked.
 * When an agent requests a new contact to be served,
 * it looks into its associated waiting queue only.  If no contact
 * is available in that queue, the agent remains free.
 * The loss-delay approximation, presented in \cite{ccAVR09a},
 * assumes that the contact center uses this policy.
 */
public class QueueAtLastGroupRouter extends Router {
   /**
    * Agent group which is neither loss  nor delay.
    */
   public  static final int NONE = 0;
   /**
    * Agent group is a loss station; see {@link #isLoss}.
    */
   public static final int LOSS = 1;
   /**
    * Agent group is a delay station; see {@link #isDelay}.
    */
   public static final int DELAY = 2;
   /**
    * Agent group is a loss and delay station.
    */
   public static final int LOSSDELAY = 3;

   /**
    * Contains the type-to-group map routing table.
    */
   protected int[][] typeToGroupMap;

   /**
    * Constructs a new queue at last group router with
    * a type-to-group map \texttt{typeToGroupMap}.
    @param numGroups the number of agent groups.
    @param typeToGroupMap the type-to-group map.
    */
   public QueueAtLastGroupRouter (int numGroups, int[][] typeToGroupMap) {
      super (typeToGroupMap.length, numGroups, numGroups);
      this.typeToGroupMap = typeToGroupMap;
   }

   /**
    * Returns the type-to-group map associated
    * with this router.
    @return the associated type-to-group map.
    */
   public int[][] getTypeToGroupMap() {
      return ArrayUtil.deepClone (typeToGroupMap, true);
   }

   /**
    * Returns the ordered list concerning
    * contact type \texttt{k} in the
    * type-to-group map.
    * @param k the index of the contact type.
    * @return the ordered list.
    */
   public int[] getTypeToGroupMap (int k) {
      return typeToGroupMap[k] == null ? null : typeToGroupMap[k].clone();
   }

   /**
    * Sets the type-to-group map associated with this router
    * to \texttt{tg}.
    @param tg the new type-to-group map.
    */
   public void setTypeToGroupMap (int[][] tg) {
      if (tg.length != getNumContactTypes())
         throw new IllegalArgumentException
            ("Cannot change the number of contact types");
      typeToGroupMap = tg;
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.AGENTGROUP;
   }

   /**
    * Determines the type of agent group \texttt{i} for contacts of
    * type \texttt{k}.  This returns {@link #LOSS} if the
    * group is a loss station, i.e., {@link #isLoss} returns \texttt{true},
    * {@link #DELAY} if it is a delay station ({@link #isDelay} returns
    * \texttt{true}), and {@link #NONE} otherwise.
    @param k the contact type.
    @param i the agent group.
    @return the status of agent group for the contact type.
    @exception IndexOutOfBoundsException if \texttt{i} or
    \texttt{k} are negative, \texttt{i} is greater than or equal to
    {@link #getNumAgentGroups} or \texttt{k}
    is greater than or equal to {@link #getNumContactTypes}.
    */
   public int getAgentGroupType (int k, int i) {
      if (i < 0 || i >= getNumAgentGroups())
         throw new IndexOutOfBoundsException
            ("Invalid agent group index: " +i);

      final int[] orderedList = typeToGroupMap[k];
      boolean found = false;
      int last = -1;
      for (final int element : orderedList) {
         if (element < 0)
            continue;
         if (element == i)
            found = true;
         last = element;
      }
      if (!found)
         return NONE;
      else if (last == i)
         return DELAY;
      else
         return LOSS;
   }

   /**
    * Determines if the agent group
    * \texttt{i} is a \emph{loss station} regarding the contact type \texttt{k}, i.e., it
    * forwards contacts of type $k$ it cannot serve immediately
    * to other
    * agent groups in the system, without
    * queueing them.
    * If the group is not in the ordered list for the contact
    * type or
    * if the group appears at the end of the ordered list,
    * this returns \texttt{false}.
    * Otherwise, this returns \texttt{true}.
    @param k the contact type identifier being tested.
    @param i the agent group identifier being tested.
    @return \texttt{true} if the agent group is a loss station.
    @exception IndexOutOfBoundsException if \texttt{i} or
    \texttt{k} are negative, \texttt{i} is greater than or equal to
    {@link #getNumAgentGroups} or \texttt{k}
    is greater than or equal to {@link #getNumContactTypes}.
    */
   public boolean isLoss (int k, int i) {
      return getAgentGroupType (k, i) == LOSS;
   }

   /**
    * Determines if the agent group
    * \texttt{i} is a \emph{delay station} regarding the contact type \texttt{k}, i.e., it
    * queues contacts of type $k$ if it cannot
    * serve them immediately.
    * If the group is at the last position in the ordered list for the contact
    * type $k$,
    * this returns \texttt{true}.
    * Otherwise, this returns \texttt{false}.
    @param k the contact type identifier being tested.
    @param i the agent group identifier being tested.
    @return \texttt{true} if the agent group is a delay station.
    @exception IndexOutOfBoundsException if \texttt{i} or
    \texttt{k} are negative, \texttt{i} is greater than or equal to
    {@link #getNumAgentGroups} or \texttt{k}
    is greater than or equal to {@link #getNumContactTypes}.
    */
   public boolean isDelay (int k, int i) {
      return getAgentGroupType (k, i) == DELAY;
   }

   /**
    * Returns the type of the agent group \texttt{i} regarding all contact types.
    * This returns {@link #LOSS} if the group is a pure loss station
    * ({@link #isPureLoss} returns \texttt{true}),
    * {@link #DELAY} if it is a pure delay station
    * ({@link #isPureDelay} returns \texttt{true}),
    * {@link #LOSSDELAY} for a loss/delay station
    * ({@link #isLossDelay} returns \texttt{true}),
    * and {@link #NONE} otherwise.
    @param i the agent group being tested.
    @return the type of the tested agent group.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to {@link #getNumAgentGroups}.
    */
   public int getAgentGroupType (int i) {
      boolean isLoss = false;
      boolean isDelay = false;
      for (int k = 0; k < getNumContactTypes(); k++) {
         final int r = getAgentGroupType (k, i);
         switch (r) {
         case NONE: break;
         case LOSS: isLoss = true; break;
         case DELAY: isDelay = true; break;
         default: throw new AssertionError
               ("getAgentGroupType (" + k + ", " + i + ") returned an invalid value " + r);
         }
      }
      if (isLoss && isDelay)
         return LOSSDELAY;
      else if (isLoss)
         return LOSS;
      else if (isDelay)
         return DELAY;
      else
         return NONE;
   }

  /**
    * Determines if the agent group \texttt{i} is a \emph{pure loss station}, i.e.,
    * it forwards all contacts
    * to another agent group.
    @param i the agent group identifier being tested.
    @return \texttt{true} if the agent group is a pure loss station,
    \texttt{false} otherwise.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to {@link #getNumAgentGroups}.
    */
    public boolean isPureLoss (int i) {
      return getAgentGroupType (i) == LOSS;
   }

  /**
   * Determines if the agent group \texttt{i} is a \emph{pure delay station}, i.e.,
    * it queues all contacts it cannot serve immediately.
    @param i the agent group identifier being tested.
    @return \texttt{true} if the agent group is a pure delay station,
    \texttt{false} otherwise.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to {@link #getNumAgentGroups}.
    */
   public boolean isPureDelay (int i) {
      return getAgentGroupType (i) == DELAY;
   }

   /**
    * Determines if the agent group \texttt{i} is a \emph{loss/delay station}, i.e.,
    * it queues some contacts it cannot serve while forwarding some
    * other contacts to other agent groups.
    @param i the agent group identifier being tested.
    @return \texttt{true} if the agent group is a loss/delay station,
    \texttt{false} otherwise.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to {@link #getNumAgentGroups}.
    */
   public boolean isLossDelay (int i) {
      return getAgentGroupType (i) == LOSSDELAY;
   }

   @Override
   public boolean canServe (int i, int k) {
      for (final int groupIndex : typeToGroupMap[k])
         if (groupIndex == i)
            return true;
      return false;
   }

   @Override
   protected EndServiceEvent selectAgent (Contact ct) {
      final AgentGroup g = AgentGroupSelectors.selectFirst (this,
                                                      typeToGroupMap[ct.getTypeId()]);
      if (g == null)
         return null;
      final EndServiceEvent es = g.serve (ct);
      assert es != null : "AgentGroup.serve should not return null";
      return es;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact ct) {
      final int[] orderedList = typeToGroupMap[ct.getTypeId()];
      int idx;
      for (idx = orderedList.length - 1; idx >= 0 && orderedList[idx] == -1; idx--);
      if (idx < 0)
         return null;
      final WaitingQueue queue = getWaitingQueue (orderedList[idx]);
      if (queue == null)
         return null;
      final DequeueEvent ev = queue.add (ct);
      assert ev != null : "WaitingQueue.add should not return null";
      return ev;
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final WaitingQueue queue = getWaitingQueue (group.getId());
      if (queue == null)
         return null;
      if (queue.isEmpty ())
         return null;
      else
         return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      final WaitingQueue queue = getWaitingQueue (group.getId());
      if (queue != null && mustClearWaitingQueue (group.getId()))
         queue.clear (DEQUEUETYPE_NOAGENT);
   }

   /**
    * Calls {@link RoutingTableUtils#formatTypeToGroupMap}
    * with the type-to-group map associated with this
    * router.
    @return the type-to-group map, formatted as a string.
    */
   public String formatTypeToGroupMap() {
      return typeToGroupMap == null ? "" :
         RoutingTableUtils.formatTypeToGroupMap
         (typeToGroupMap);
   }

   @Override
   public String getDescription() {
      return "Queue at last group router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Push routing rank list\n");
      sb.append (formatTypeToGroupMap());
      return sb.toString();
   }
}
