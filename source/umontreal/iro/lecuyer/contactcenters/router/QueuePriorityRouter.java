package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This skill-based router with queue priority ranking is
 * based on the routing heuristic
 * in \cite{ccKOO03a}, extended to support queueing.
 * When a contact arrives to the router, an ordered list
 * (the type-to-group map)
 * is used to determine which agent groups are
 * able to serve it, and the order in which they are checked.
 * If agent group $i_{k, 0}$
 * contains at least one free agent, this agent serves the contact.
 * Otherwise, the router tries to test agent groups $i_{k, 1}$,
 * $i_{k, 2}$, etc.\
 * until a free agent is found, or the list of agent groups is exhausted.
 * In other words, the contact \emph{overflows} from one agent group
 * to another.
 * If no agent group in the ordered
 * list associated with the contact's type is able to serve
 * the contact, the contact is inserted into a waiting queue corresponding to
 * its type unless the queue is full.
 * If the total queueing capacity
 * of the router is exceeded, the contact is blocked.
 *
 * When an agent becomes free, it uses another ordered
 * list (the group-to-type map) to determine which types of contacts it can serve.
 * If the queue containing contacts of type~$k_{i, 0}$ is non-empty,
 * the first contact, i.e., the contact of type~$k_{i, 0}$ with the longest
 * waiting time, is removed and handled to the free agent.
 * Otherwise, the queues containing contacts of types~$k_{i, 1}$,
 * $k_{i, 2}$, etc.\ are queried similarly for contacts
 * to be served.  If no contact is available in any accessible
 * waiting queue, the agent stays free.  The router behaves as if
 * a priority queue was associated with each agent group,
 * implementing priorities by using several FIFO waiting
 * queues.
 *
 * This router should be used only when the type-to-group and
 * group-to-type maps are specified as input data.
 * If one table has to be generated from the other one,
 * the induced arbitrary order of the lists can
 * affect the performance of the contact center.
 */
public class QueuePriorityRouter extends Router {
   /**
    * Contains the type-to-group map routing table.
    */
   protected int[][] typeToGroupMap;

   /**
    * Contains the group-to-type map routing table.
    */
   protected int[][] groupToTypeMap;

   /**
    * Constructs a new queue priority router with
    * a type-to-group map \texttt{typeToGroupMap}, and a
    * group-to-type map \texttt{groupToTypeMap}.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    */
   public QueuePriorityRouter (int[][] typeToGroupMap,
                               int[][] groupToTypeMap) {
      super (typeToGroupMap.length, typeToGroupMap.length, groupToTypeMap.length);
      this.typeToGroupMap = ArrayUtil.deepClone (typeToGroupMap, true);
      this.groupToTypeMap = ArrayUtil.deepClone (groupToTypeMap, true);
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
    * Returns the group-to-type map associated
    * with this router.
    @return the associated group-to-type map.
    */
   public int[][] getGroupToTypeMap() {
      return ArrayUtil.deepClone (groupToTypeMap, true);
   }

   /**
    * Returns the ordered list concerning
    * agent group \texttt{i} in the
    * group-to-type map.
    * @param i the index of the agent group.
    * @return the ordered list.
    */
   public int[] getGroupToTypeMap (int i) {
      return groupToTypeMap[i] == null ? null : groupToTypeMap[i].clone();
   }

   /**
    * Changes the routing table for this
    * router.  The routing table must be
    * specified using \texttt{typeToGroupMap}
    * and \texttt{groupToTypeMap}.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    @exception IllegalArgumentException if the type-to-group map
    does not contain $K$ rows, or the group-to-type map does not
    contain $I$ rows.
    */
   public void setRoutingTable (int[][] typeToGroupMap,
                                int[][] groupToTypeMap) {
      if (typeToGroupMap.length != getNumContactTypes())
         throw new IllegalArgumentException
            ("Cannot change the number of contact types");
      if (groupToTypeMap.length != getNumAgentGroups())
         throw new IllegalArgumentException
            ("Cannot change the number of agent groups");
      this.typeToGroupMap = ArrayUtil.deepClone (typeToGroupMap, true);
      this.groupToTypeMap = ArrayUtil.deepClone (groupToTypeMap, true);
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.CONTACTTYPE;
   }

   @Override
   public boolean canServe (int i, int k) {
      for (final int typeIndex : groupToTypeMap[i])
         if (typeIndex == k)
            return true;
      return false;
   }

   @Override
   protected EndServiceEvent selectAgent (Contact ct) {
      final AgentGroup g = AgentGroupSelectors.selectFirst
         (this, typeToGroupMap[ct.getTypeId()]);
      if (g == null)
         return null;
      final EndServiceEvent es = g.serve (ct);
      assert es != null : "AgentGroup.serve should not return null";
      return es;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact ct) {
      final WaitingQueue queue = getWaitingQueue (ct.getTypeId());
      if (queue == null)
         return null;
      final DequeueEvent ev = queue.add (ct);
      assert ev != null : "WaitingQueue.add should not return null";
      return ev;
   }

   private WaitingQueue selectedQueue = null;

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      if (selectedQueue == null || selectedQueue.isEmpty()) {
         final int[] orderedList = groupToTypeMap[group.getId()];
         selectedQueue = WaitingQueueSelectors.selectFirstNonEmpty (this, orderedList);
      }
      if (selectedQueue == null)
         return null;
      if (selectedQueue.isEmpty ())
         return null;
      else
         return selectedQueue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   protected boolean checkFreeAgents (AgentGroup group, Agent agent) {
      selectedQueue = null;
      return super.checkFreeAgents (group, agent);
   }

   /**
    * This default implementation is suitable
    * only for routers specifying a type-to-group and a group-to-type map
    * and using one waiting queue for each contact type.
    * If the tables are not specified or the number of supported waiting queues
    * is different from the number of supported contact types,
    * this implementation does nothing.
    @param group the agent group with no more agents.
    */
   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      final int gid = group.getId();
      if (groupToTypeMap == null || typeToGroupMap == null)
         return;
      if (getNumWaitingQueues() != getNumContactTypes())
         return;
      final int[] orderedList = groupToTypeMap[gid];
      for (final int k : orderedList) {
         if (k < 0)
            continue;
         final WaitingQueue queue = getWaitingQueue (k);
         if (queue == null ||
             !mustClearWaitingQueue (k) ||
             queue.size() == 0)
            continue;
         boolean mustClear = true;
         for (int idx2 = 0; idx2 < typeToGroupMap[k].length && mustClear; idx2++) {
            final int i2 = typeToGroupMap[k][idx2];
            if (i2 < 0)
               continue;
            final AgentGroup grp = getAgentGroup (i2);
            if (grp != null && grp.getNumAgents() > 0)
               mustClear = false;
         }
         if (mustClear)
            queue.clear (DEQUEUETYPE_NOAGENT);
      }
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

   /**
    * Calls {@link RoutingTableUtils#formatGroupToTypeMap}
    * with the group-to-type map associated with this
    * router.
    @return the group-to-type map, formatted as a string.
    */
   public String formatGroupToTypeMap() {
      return groupToTypeMap == null ? "" :
         RoutingTableUtils.formatGroupToTypeMap
         (groupToTypeMap);
   }

   @Override
   public String getDescription() {
      return "Queue priority router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Push routing rank list\n");
      sb.append (formatTypeToGroupMap()).append ("\n");
      sb.append ("Pull routing rank list\n");
      sb.append (formatGroupToTypeMap());
      return sb.toString();
   }
}
