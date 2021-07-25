package umontreal.iro.lecuyer.contactcenters.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.contact.SingleTypeContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.TrunkGroup;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.PriorityWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEventDetailed;

/**
 * Represents a contact router which can perform agent and contact selections. A
 * router links the contact sources, agent groups and waiting queues together
 * and acts as a central element of the contact center. It supports a certain
 * number of contact types and contains slots for waiting queues and agent
 * groups.
 *
 * Dequeued contacts and freed agents are notified to the router through nested
 * classes implementing the appropriate listener interfaces. These classes
 * listen to the connected agent groups and waiting queues only. Agent groups
 * and waiting queues are connected to the router using the
 * {@link #setAgentGroup(int,AgentGroup)} and {@link #setWaitingQueue(int,WaitingQueue)} methods, respectively.
 * During connection, they are assigned a numerical identifier to be referred to
 * efficiently during routing.
 *
 * This abstract class does not implement any routing policy. To implement such
 * a policy, many informations must be provided in a subclass: data structures,
 * algorithms for agent, waiting queue and contact selections, and an algorithm
 * to automatically clear waiting queues. A router can also specify what happens
 * when a contact is served or abandons. We now examine these elements in more
 * details. Data structures are encoded into fields, and usually consist of a
 * type-to-group and a group-to-type maps, or matrices of ranks. Algorithms are
 * provided by overriding methods.
 *
 * When a new contact is notified through its {@link #newContact(Contact)} method
 * specified by the {@link NewContactListener} interface, the router performs
 * \emph{agent selection}, i.e., it tries to assign an agent to the contact. The
 * {@link #selectAgent(Contact)} method is used to select the agent, and
 * {@link #selectWaitingQueue(Contact)} is called if no free agent is available.
 *
 * The router supports contact rerouting which works as follows. When a contact
 * is queued, the router gets a delay using
 * {@link #getReroutingDelay(DequeueEvent,int)}. If that delay is finite and
 * greater than or equal to 0, a \emph{rerouting event} is scheduled. When such
 * an event happens, the router tries to use
 * {@link #selectAgent(DequeueEvent,int)} to assign an agent to a queued
 * contact. If this rerouting fails, the router uses
 * {@link #selectWaitingQueue(DequeueEvent,int)} to decide if the contact
 * should be dropped, transferred into another queue, or kept in the same queue.
 * After this queue reselection has happened, the router uses
 * {@link #getReroutingDelay(DequeueEvent,int)} again to decide if a subsequent
 * rerouting will happen. By default, this functionality is disabled. One has
 * to override {@link #getReroutingDelay(DequeueEvent,int)} and
 * {@link #selectAgent(DequeueEvent,int)} to use rerouting.
 *
 * When an agent becomes free, the router must perform \emph{contact selection},
 * i.e., it must try to assign a queued contact to the free agent through the
 * {@link #checkFreeAgents(AgentGroup,Agent)} method. The {@link #checkFreeAgents(AgentGroup,Agent)} method is
 * called, and usually calls {@link #selectContact(AgentGroup,Agent)} to get queued contacts. If
 * no queued contact is available for the free agent, the agent remains free.
 *
 * The router also supports agent rerouting which works as follows. If an agent
 * has finished the service of a contact and cannot find a new contact to serve,
 * before letting the agent idle, the router gets a delay using
 * {@link #getReroutingDelay(Agent,int)}. If this delay is finite and greater
 * than or equal to 0, the router schedules an event that will try to assign a
 * new contact to the agent. The contact is selected using the
 * {@link #selectContact(Agent,int)} method. As with contact rerouting, agent
 * rerouting can happen multiple times and it is disabled by default. One needs
 * to use detailed agent groups considering individual agents and override
 * {@link #getReroutingDelay(Agent,int)} as well as
 * {@link #selectContact(Agent,int)} to take advantage of agent rerouting.
 *
 * At some moments during the day, queued contacts may never be served, because
 * no skilled agent is present. For example, when the center closes, all agents
 * leave and queued contacts are forced to wait forever or abandon. To avoid
 * this, an additional algorithm may be implemented in
 * {@link #checkWaitingQueues(AgentGroup)} to automatically clear the queues when no agent
 * can serve contacts. This clearing is disabled by default but can be enabled
 * by using {@link #setClearWaitingQueue(int,boolean)} or {@link #setClearWaitingQueues(boolean)}.
 *
 * Finally, the moment a contact exits can be controlled. By default, dequeued
 * and served contacts exit the system, but it is possible to override methods
 * in this class to change this behavior, e.g., transfer a dequeued contact to
 * another queue, transfer a served contact to another agent, etc.
 *
 * Note that the blocking, dequeue, end-contact and end-service indicators
 * \texttt{Integer.MAX\_VALUE - 1000} through \texttt{Integer.MAX\_VALUE} are
 * reserved for present and future use by routers.
 * Dequeue type 0 is also reserved, and
 * represents the beginning of the service
 * for a queued contact.
 * The constant {@link #DEQUEUETYPE_BEGINSERVICE}
 * can be used to represent this.
 *
 * Note: the {@link ExitedContactListener} implementations are notified in the
 * order of the list returned by {@link #getExitedContactListeners()}, and an
 * exited-contact listener modifying the list of listeners by using
 * {@link #addExitedContactListener(ExitedContactListener)} or {@link #removeExitedContactListener(ExitedContactListener)}
 * could result in unpredictable behavior.
 */
public abstract class Router implements NewContactListener {
   private SingleTypeContactFactory[] factories;
   private WaitingQueue[] queues;
   private AgentGroup[] groups;
   private List<Dialer>[] dialers;
   private final List<ExitedContactListener> listeners = new ArrayList<ExitedContactListener> ();
   private final List<ExitedContactListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;
   private int queueCapacity = Integer.MAX_VALUE;
   private int totalQueueSize = 0;
   private final RouterListener rl = new RouterListener ();
   private boolean[] clearQueue;

   Map<DequeueEvent, ContactReroutingEvent> contactReroutingEvents;
   Map<Agent, AgentReroutingEvent> agentReroutingEvents;
   int numContactReroutingEvents = 0;
   int numAgentReroutingEvents = 0;
   private Map<DequeueEvent, ContactReroutingEvent> umContactReroutingEvents;
   private Map<Agent, AgentReroutingEvent> umAgentReroutingEvents;

   /**
    * Contact blocking type occurring when there is no communication channel
    * available in the trunk group associated with an incoming contact.
    */
   public static final int BLOCKTYPE_NOLINE = Integer.MAX_VALUE;

   /**
    * Contact blocking type occurring when the total queue capacity is exceeded
    * upon the arrival of a contact.
    */
   public static final int BLOCKTYPE_QUEUEFULL = Integer.MAX_VALUE - 1;

   /**
    * Contact blocking type occurring when a contact cannot be queued, i.e.,
    * {@link #selectWaitingQueue(Contact)} returns \texttt{null}.
    */
   public static final int BLOCKTYPE_CANTQUEUE = Integer.MAX_VALUE - 2;

   /**
    * Contact dequeueing type representing the beginning of
    * the service.
    */
   public static final int DEQUEUETYPE_BEGINSERVICE = 0;

   /**
    * Contact dequeuing type occurring when a waiting queue is cleared because
    * there is no agent in the system capable of serving the contact.
    */
   public static final int DEQUEUETYPE_NOAGENT = Integer.MAX_VALUE;

   /**
    * Contact dequeue type used to remove multiple copies of a contact
    * from waiting queues.
    * When a contact has to wait in more than one waiting queues, it
    * can exit any of these queues at any time.
    * When the contact is dequeued, e.g., because it is transferred
    * to an agent.  In this case, the contact also
    * needs to be removed from other queues.
    * This dequeue type can be used to avoid such
    * contacts being counted several times by
    * statistical facilities.
    */
   public static int DEQUEUETYPE_FANTOM = Integer.MAX_VALUE - 1;

   /**
    * Contact dequeue type used when transferring a contact
    * from a waiting to another waiting queue.
    */
   public static int DEQUEUETYPE_TRANSFER = Integer.MAX_VALUE - 2;

   /**
    * Constructs a new router with \texttt{numTypes} contact types,
    * \texttt{numQueues} waiting queues, and \texttt{numGroups} agent groups.
    *
    * @param numTypes
    *           number of contact types.
    * @param numQueues
    *           number of waiting queues.
    * @param numGroups
    *           number of agent groups.
    * @exception IllegalArgumentException
    *               if any argument is negative.
    */
   @SuppressWarnings ("unchecked")
   public Router (int numTypes, int numQueues, int numGroups) {
      if (numTypes < 0 || numGroups < 0 || numQueues < 0)
         throw new IllegalArgumentException (
               "Invalid numTypes, numGroups or numQueues");
      factories = new SingleTypeContactFactory[numTypes];
      queues = new WaitingQueue[numQueues];
      groups = new AgentGroup[numGroups];
      dialers = new List[numGroups];
      for (int i = 0; i < numGroups; i++)
         dialers[i] = new ArrayList<Dialer> ();
      clearQueue = new boolean[numQueues];
   }

   /**
    * Determines if this router keeps track of all rerouting events scheduled.
    * By default, these events are discarded, i.e., they are stored in the event
    * list only.
    *
    * @return \texttt{true} if the router keeps track of the rerouting events,
    *         \texttt{false} otherwise.
    */
   public boolean isKeepingReroutingEvents () {
      return contactReroutingEvents != null;
   }

   /**
    * Sets the keep-rerouting-events indicator to \texttt{keep}.
    *
    * @param keep
    *           the value of the indicator.
    */
   public void setKeepingReroutingEvents (boolean keep) {
      if (keep && contactReroutingEvents == null) {
         if (numContactReroutingEvents > 0) {
            final Iterator<ContactReroutingEvent> itcr = contactReroutingEventsIterator ();
            contactReroutingEvents = new HashMap<DequeueEvent, ContactReroutingEvent> ();
            while (itcr.hasNext ()) {
               final ContactReroutingEvent rev = itcr.next ();
               contactReroutingEvents.put (rev.getDequeueEvent (), rev);
            }
         }
         else
            contactReroutingEvents = new HashMap<DequeueEvent, ContactReroutingEvent> ();
         if (numAgentReroutingEvents > 0) {
            final Iterator<AgentReroutingEvent> itar = agentReroutingEventsIterator ();
            agentReroutingEvents = new HashMap<Agent, AgentReroutingEvent> ();
            while (itar.hasNext ()) {
               final AgentReroutingEvent rev = itar.next ();
               agentReroutingEvents.put (rev.getAgent (), rev);
            }
         }
         else
            agentReroutingEvents = new HashMap<Agent, AgentReroutingEvent> ();
         umContactReroutingEvents = Collections
               .unmodifiableMap (contactReroutingEvents);
         umAgentReroutingEvents = Collections
               .unmodifiableMap (agentReroutingEvents);
      }
      else if (!keep && contactReroutingEvents != null) {
         contactReroutingEvents.clear ();
         agentReroutingEvents.clear ();
         contactReroutingEvents = null;
         agentReroutingEvents = null;
         umContactReroutingEvents = null;
         umAgentReroutingEvents = null;
      }
   }

   /**
    * Constructs and returns an iterator for the contact rerouting events. If
    * {@link #isKeepingReroutingEvents()} returns \texttt{true}, the iterator is
    * constructed from the set returned by {@link #getContactReroutingEvents()}.
    * Otherwise, an iterator traversing the event list and filtering the
    * appropriate events is constructed and returned.
    *
    * @return the iterator for contact rerouting events.
    */
   public Iterator<ContactReroutingEvent> contactReroutingEventsIterator () {
      if (!isKeepingReroutingEvents())
//         return new FilteredIterator<ContactReroutingEvent> (Sim
//               .getEventList ().iterator (), numContactReroutingEvents) {
//            @Override
//            public boolean filter (Object ev) {
//               if (ev instanceof ContactReroutingEvent) {
//                  final ContactReroutingEvent rev = (ContactReroutingEvent) ev;
//                  if (rev.getRouter () == Router.this)
//                     return true;
//               }
//               return false;
//            }
//         };
         throw new IllegalStateException
         ("Cannot obtain contact rerouting events; call setKeepingReroutingEvents(true) while no such event is scheduled");
      else
         return getContactReroutingEvents ().values ().iterator ();
   }

   /**
    * Returns an unmodifiable map containing the currently scheduled contact
    * rerouting events. Each key of this map corresponds to a dequeue event
    * while each value corresponds to an instance of
    * {@link ContactReroutingEvent}. If rerouting events are not kept, this
    * throws an {@link IllegalStateException}.
    *
    * @return the map of contact rerouting events.
    * @exception IllegalStateException
    *               if rerouting events are not kept.
    */
   public Map<DequeueEvent, ContactReroutingEvent> getContactReroutingEvents () {
      if (contactReroutingEvents == null)
         throw new IllegalStateException ("Rerouting events are not kept");
      return umContactReroutingEvents;
   }

   /**
    * Constructs and returns an iterator for the agent rerouting events. If
    * {@link #isKeepingReroutingEvents()} returns \texttt{true}, the iterator is
    * constructed from the set returned by {@link #getAgentReroutingEvents()}.
    * Otherwise, an iterator traversing the event list and filtering the
    * appropriate events is constructed and returned.
    *
    * @return the iterator for agent rerouting events.
    */
   public Iterator<AgentReroutingEvent> agentReroutingEventsIterator () {
      if (!isKeepingReroutingEvents())
//         return new FilteredIterator<AgentReroutingEvent> (Sim.getEventList ()
//               .iterator (), numAgentReroutingEvents) {
//            @Override
//            public boolean filter (Object ev) {
//               if (ev instanceof AgentReroutingEvent) {
//                  final AgentReroutingEvent rev = (AgentReroutingEvent) ev;
//                  if (rev.getRouter () == Router.this)
//                     return true;
//               }
//               return false;
//            }
//         };
         throw new IllegalStateException
         ("Cannot obtain agent rerouting events; call setKeepingReroutingEvents(true) while no such event is scheduled");
      else
         return getAgentReroutingEvents ().values ().iterator ();
   }

   /**
    * Returns an unmodifiable map containing the currently scheduled agent
    * rerouting events. Each key of this map corresponds to an {@link Agent}
    * object while each value corresponds to an instance of
    * {@link AgentReroutingEvent}. If rerouting events are not kept, this
    * throws an {@link IllegalStateException}.
    *
    * @return the map of agent rerouting events.
    * @exception IllegalStateException
    *               if rerouting events are not kept.
    */
   public Map<Agent, AgentReroutingEvent> getAgentReroutingEvents () {
      if (agentReroutingEvents == null)
         throw new IllegalStateException ("Rerouting events are not kept");
      return umAgentReroutingEvents;
   }

   /**
    * Saves the state of this router, and returns the resulting state object.
    *
    * @return the current state of this router.
    */
   public RouterState save () {
      return new RouterState (this);
   }

   /**
    * Restores the state \texttt{state} of this router.
    *
    * @param state
    *           the saved state of the router.
    */
   public void restore (RouterState state) {
      state.restore (this);
   }

   /**
    * Returns the total capacity of the waiting queues for this router. This
    * capacity determines the maximal number of contacts that can be queued
    * simultaneously by this router. By default, this is
    * \texttt{Integer.MAX\_VALUE}, i.e., infinite.
    *
    * @return the total queue capacity of the router.
    */
   public int getTotalQueueCapacity () {
      return queueCapacity;
   }

   /**
    * Sets the total queue capacity to \texttt{capacity} for this router. If the
    * given capacity is negative, an {@link IllegalArgumentException} is thrown.
    * If the capacity is less than the total number of queued contacts, this
    * throws an {@link IllegalStateException}.
    *
    * @param capacity
    *           the new total queue capacity.
    * @exception IllegalArgumentException
    *               if the given capacity is negative.
    * @exception IllegalStateException
    *               if the given capacity is less than the actual number of
    *               queued contacts.
    */
   public void setTotalQueueCapacity (int capacity) {
      if (capacity < 0)
         throw new IllegalArgumentException (
               "The capacity must not be negative");
      if (capacity < totalQueueSize)
         throw new IllegalStateException ("New capacity " + capacity
               + " is smaller than the total queue size " + totalQueueSize);
      queueCapacity = capacity;
   }

   /**
    * Returns the total number of contacts in the connected waiting queues.
    *
    * @return the total number of contacts in queues.
    */
   public int getCurrentQueueSize () {
      return totalQueueSize;
   }

   /**
    * Returns the number of contact types supported by this router.
    *
    * @return the supported number of contact types.
    */
   public int getNumContactTypes () {
      return factories.length;
   }

   /**
    * Returns the number of agent groups supported by this router.
    *
    * @return the number of agent groups.
    */
   public int getNumAgentGroups () {
      return groups.length;
   }

   /**
    * Returns the number of waiting queues supported by this router.
    *
    * @return the number of waiting queues.
    */
   public int getNumWaitingQueues () {
      return queues.length;
   }

   /**
    * Returns the waiting queue with index \texttt{q} for this router. If
    * \texttt{q} is less than 0 or greater than or equal to the number of
    * supported queues, an exception is thrown. Calling the
    * {@link WaitingQueue#getId()} method on the returned waiting queue should
    * return \texttt{q}, unless this method returns \texttt{null}.
    *
    * @param q
    *           the index of the queue.
    * @return the associated waiting queue, or \texttt{null} if no queue is
    *         defined for this index.
    * @exception IndexOutOfBoundsException
    *               if \texttt{q} is negative or greater than or equal to
    *               {@link #getNumWaitingQueues()}.
    */
   public WaitingQueue getWaitingQueue (int q) {
      return queues[q];
   }

   /**
    * Returns an array containing the waiting queues attached to this router.
    *
    * @return the waiting queues attached to this router.
    */
   public WaitingQueue[] getWaitingQueues () {
      return queues.clone ();
   }

   /**
    * Associates the waiting queue \texttt{queue} with the index \texttt{q} in
    * the router. The method tries to set the queue id to \texttt{q} and
    * registers a waiting-queue listener for \texttt{queue} to be notified about
    * automatic dequeues if needed. If a waiting queue was previously associated
    * with the index, the router's waiting-queue listener is removed from that
    * previous waiting queue.
    *
    * Note that some routers assume that waiting queues use FIFO discipline. In
    * this case, one should use {@link StandardWaitingQueue} instances only.
    * Using {@link PriorityWaitingQueue} may lead to routing not corresponding
    * to the defined policy.
    *
    * @param q
    *           the index of the queue.
    * @param queue
    *           the queue to be associated.
    * @exception IllegalStateException
    *               if the queue id was already set to another value than
    *               \texttt{q}.
    * @exception IndexOutOfBoundsException
    *               if \texttt{q} is negative or greater than or equal to
    *               {@link #getNumWaitingQueues()}.
    */
   public void setWaitingQueue (int q, WaitingQueue queue) {
      if (queues[q] != null) {
         queues[q].removeWaitingQueueListener (rl);
         totalQueueSize -= queues[q].size ();
      }
      queues[q] = queue;
      if (queue != null) {
         // Calling setId prevents applications from associating
         // the same waiting queue to several indices.
         queue.setId (q);
         queue.addWaitingQueueListener (rl);
         totalQueueSize += queue.size ();
      }
   }

   /**
    * Determines if the router must clear the waiting queue \texttt{q} when all
    * queued contacts cannot be served since no agent capable of serving them is
    * online anymore. By default, this is set to \texttt{false}.
    *
    * @param q
    *           the index of the checked waiting queue.
    * @return the clear-waiting-queue indicator.
    * @exception IndexOutOfBoundsException
    *               if \texttt{q} is negative or greater than or equal to
    *               {@link #getNumWaitingQueues()}.
    */
   public boolean mustClearWaitingQueue (int q) {
      return clearQueue[q];
   }

   /**
    * Sets the clear-waiting-queue indicator for the waiting queue \texttt{q} to
    * \texttt{b}. See {@link #mustClearWaitingQueue(int)} for more information.
    *
    * @param q
    *           the index of the affected waiting queue.
    * @param b
    *           the new value of the indicator.
    * @exception IndexOutOfBoundsException
    *               if \texttt{q} is negative or greater than or equal to
    *               {@link #getNumWaitingQueues()}.
    * @see #mustClearWaitingQueue(int)
    */
   public void setClearWaitingQueue (int q, boolean b) {
      clearQueue[q] = b;
   }

   /**
    * Sets the clear-waiting-queue indicator to \texttt{b} for all waiting
    * queues. See {@link #mustClearWaitingQueue(int)} for more information.
    *
    * @param b
    *           the new value of the indicator.
    * @see #mustClearWaitingQueue(int)
    */
   public void setClearWaitingQueues (boolean b) {
      for (int i = 0; i < clearQueue.length; i++)
         clearQueue[i] = b;
   }

   /**
    * Returns the contact factory used by the simulator
    * to create contacts of type \texttt{k}.
    * This factory may be used by some routing
    * policies to obtain information such as
    * the distribution of service times.
    * When a routing policy uses this information,
    * the simulator should create contacts of
    * type \texttt{k} with this
    * single-type contact factory only.
    * @param k the contact type identifier.
    * @return the contact factory.
    */
   public SingleTypeContactFactory getContactFactory (int k) {
      return factories[k];
   }

   /**
    * Sets the contact factory used to create
    * contacts of type \texttt{k}
    * to \texttt{factory}.
    * @param k the contact type identifier.
    * @param factory the contact factory.
    */
   public void setContactFactory (int k, SingleTypeContactFactory factory) {
      factories[k] = factory;
   }

   /**
    * Returns the agent group with index \texttt{i} for this router. If
    * \texttt{i} is less than 0 or greater than or equal to the number of
    * groups, an exception is thrown. Calling {@link AgentGroup#getId()} on the
    * returned group should return \texttt{i}, unless this method returns
    * \texttt{null}.
    *
    * @param i
    *           the index of the agent group.
    * @return the associated agent group, or \texttt{null} if no agent group is
    *         defined for this index.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} is negative or greater than or equal to
    *               {@link #getNumAgentGroups()}.
    */
   public AgentGroup getAgentGroup (int i) {
      return groups[i];
   }

   /**
    * Returns an array containing the agent groups attached to this router.
    *
    * @return the attached agent groups.
    */
   public AgentGroup[] getAgentGroups () {
      return groups.clone ();
   }

   /**
    * Associates the agent group \texttt{group} with the index \texttt{i} in the
    * router. The method tries to set the identifier of the group to \texttt{i}
    * and registers an agent-group listener to be notified about agents becoming
    * free in order to perform contact selection. If an agent group was
    * previously associated with the index, the router's agent-group listener is
    * removed from that previous agent group.
    *
    * @param i
    *           the index of the agent group.
    * @param group
    *           the agent group to be associated.
    * @exception IllegalStateException
    *               if the group id was already set to another value than
    *               \texttt{i}.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} is negative or greater than or equal to
    *               {@link #getNumAgentGroups()}.
    */
   public void setAgentGroup (int i, AgentGroup group) {
      if (groups[i] != null)
         groups[i].removeAgentGroupListener (rl);
      groups[i] = group;
      if (group != null) {
         group.setId (i);
         group.addAgentGroupListener (rl);
      }
   }

   /**
    * Returns a list containing the dialers which will be triggered when the
    * service of a contact by an agent in group \texttt{i} ends. This list,
    * which may contain only non-\texttt{null} instances of the {@link Dialer}
    * class, should be used instead of an agent-group listener to activate the
    * dialer. As opposed to an agent-group listener requesting dialers to try
    * calls, dialers in the returned list are activated only after contact
    * selection for agents in group \texttt{i} is done, and they are guaranteed
    * to be activated in the order given by the list.
    *
    * @param i
    *           the index of the agent group.
    * @return the list of dialers.
    * @exception ArrayIndexOutOfBoundsException
    *               if the agent group index is out of bounds.
    */
   public List<Dialer> getDialers (int i) {
      return dialers[i];
   }

   /**
    * Returns \texttt{true} if and only if
    * some agents in group \texttt{i}
    * are authorized to serve contacts
    * of type \texttt{k} by this router.
    * @param i the agent group index.
    * @param k the contact type index.
    * @return determines if contacts can be served.
    */
   public abstract boolean canServe (int i, int k);

   /**
    * Determines if the agent group \texttt{i}
    * should consider individual agents.
    * This does not determine directly how the
    * agent group returned by {@link #getAgentGroup(int)}
    * is implemented. This method only gives clues to a
    * simulator on how to construct the concerned
    * agent group.
    * @param i the index of the agent group.
    * @return the detailed status of the agent group.
    */
   public boolean needsDetailedAgentGroup (int i) {
      return false;
   }

   /**
    * Returns an indicator describing how the
    * implemented routing policies organizes waiting queues.
    * The supported modes of organization
    * cover the most common cases only: waiting queues
    * corresponding to contact types or agent groups.
    * For any other modes, the
    * {@link WaitingQueueType#GENERAL}
    * must be used.
    *
    * By default, this method returns {@link WaitingQueueType#GENERAL}.
    *
    * @return the organization mode of waiting queues.
    */
   public WaitingQueueType getWaitingQueueType() {
      return WaitingQueueType.GENERAL;
   }

   /**
    * Returns the needed data structure for waiting queue
    * with index \texttt{q}.
    * This method is used by the simulator to get clues
    * on how to construct the waiting queue; it does not
    * affect directly the implementation of the waiting queue
    * returned by {@link #getWaitingQueue(int)}.
    * By default, this returns {@link WaitingQueueStructure#LIST}.
    * @param q the index of the waiting queue.
    * @return the structure indicator.
    */
   public WaitingQueueStructure getNeededWaitingQueueStructure (int q) {
      return WaitingQueueStructure.LIST;
   }

   /**
    * Determines how contacts in queue should be compared with
    * each other for waiting queue \texttt{q}. This comparator
    * is used by a simulator to construct a waiting queue if
    * {@link #getNeededWaitingQueueStructure(int)} returns
    * {@link WaitingQueueStructure#SORTEDSET} or
    * {@link WaitingQueueStructure#PRIORITY}.
    * By default, this returns \texttt{null}.
    * @param q the index of the waiting queue.
    * @return the waiting queue comparator.
    */
   public Comparator<? super DequeueEvent> getNeededWaitingQueueComparator (int q) {
      return null;
   }

   /**
    * This method is called when the new contact \texttt{contact} enters in the
    * system and should not be overridden. The {@link Contact#setRouter(Router)} method
    * is first used to set the router of the new contact to this object. Then,
    * if {@link Contact#getTrunkGroup()} returns a non-\texttt{null} value, a
    * communication channel is allocated. If no communication channel is
    * available for the contact, the contact is blocked with blocking type
    * {@link #BLOCKTYPE_NOLINE}. If the contact has no associated trunk group
    * or if a communication channel could be successfully allocated, the
    * {@link #selectAgent(Contact)} method is called to try to assign it an agent. In
    * case of failure, i.e., {@link #selectAgent(Contact)} returns \texttt{null}, the
    * router tries to queue the contact. If the total queue size is equal to the
    * total queue capacity, or if {@link #selectWaitingQueue(Contact)} returns
    * \texttt{null}, the contact is blocked with blocking type
    * {@link #BLOCKTYPE_QUEUEFULL} or {@link #BLOCKTYPE_CANTQUEUE},
    * respectively.
    *
    * @param contact
    *           the arrived contact.
    * @exception IllegalStateException
    *               if {@link Contact#getRouter()} returns a non-\texttt{null}
    *               value before the router is set.
    * @exception IllegalArgumentException
    *               if the contact type identifier of the contact is negative or
    *               greater than or equal to {@link #getNumContactTypes()}.
    */
   public void newContact (Contact contact) {
      if (contact.hasExited ())
         throw new IllegalStateException ("Exited contact");
      if (contact.getRouter () != null)
         throw new IllegalStateException (
               "A contact arrival process must have only one router as a listener");
      final int type = contact.getTypeId ();
      if (type < 0 || type >= factories.length)
         throw new IllegalArgumentException ("Contact type not supported: "
               + type);
      contact.setRouter (this);
      final TrunkGroup tg = contact.getTrunkGroup ();
      if (tg != null &&
            contact.getNumWaitingQueues() == 0 &&
            contact.getNumAgentGroups() == 0 &&
            !tg.take (contact))
         exitBlocked (contact, BLOCKTYPE_NOLINE);
      else {
         final EndServiceEvent es = selectAgent (contact);
         if (es == null)
            if (totalQueueSize >= queueCapacity)
               exitBlocked (contact, BLOCKTYPE_QUEUEFULL);
            else {
               final DequeueEvent dqEv = selectWaitingQueue (contact);
               if (dqEv == null)
                  exitBlocked (contact, BLOCKTYPE_CANTQUEUE);
               else {
                  final double delay = getReroutingDelay (dqEv, -1);
                  if (delay >= 0 && !Double.isInfinite (delay)
                        && !Double.isNaN (delay)) {
                     final ContactReroutingEvent ev = new ContactReroutingEvent (
                           this, dqEv, 0);
                     ev.schedule (delay);
                  }
               }
            }
         else
            assert es == null || es.getContact () == contact;
      }
   }

   /**
    * Adds the exited-contact listener \texttt{listener} to this router. If the
    * listener is already registered, nothing happens.
    *
    * @param listener
    *           the listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addExitedContactListener (ExitedContactListener listener) {
      if (listener == null)
         throw new NullPointerException (
               "Null exited contact listener not allowed");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains (listener))
         listeners.add (listener);
   }

   /**
    * Removes the exited-contact listener \texttt{listener} from this router.
    *
    * @param listener
    *           the exited contact listener being removed.
    */
   public void removeExitedContactListener (ExitedContactListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.remove (listener);
   }

   /**
    * Removes all the exited-contact listeners registered to this router.
    */
   public void clearExitedContactListeners () {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.clear ();
   }

   /**
    * Returns an unmodifiable list containing all the exited-contact listeners
    * registered with this router.
    *
    * @return the list of all registered exited-contact listeners.
    */
   public List<ExitedContactListener> getExitedContactListeners () {
      return umListeners;
   }

   /**
    * Notifies every registered listener that
    * the contact \texttt{contact} was blocked
    * with blocking type \texttt{bType}.
    * @param contact the blocked contact.
    * @param bType the blocking type.
    */
   public void notifyBlocked (Contact contact, int bType) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).blocked (this, contact, bType);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that
    * a contact left the waiting queue, this
    * event being represented by \texttt{ev}.
    * @param ev the event representing the contact having left the queue.
    */
   public void notifyDequeued (DequeueEvent ev) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).dequeued (this, ev);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that
    * a contact was served, the service being
    * represented by the end-service event
    * \texttt{ev}.
    * @param ev the end-service event representing
    * the end of the service.
    */
   public void notifyServed (EndServiceEvent ev) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).served (this, ev);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   /**
    * This method must be called to notify a contact exiting the system after an
    * end of service with end-service event \texttt{ev}. It notifies any
    * registered exited-contact listener, and releases the communication channel
    * taken by the contact. This must be called after the communication between
    * the contact and an agent, before after-contact work.
    *
    * @param ev
    *           the end-service event.
    */
   public void exitServed (EndServiceEvent ev) {
      // The serviceEnded method in Contact is called by AgentGroup
      final Contact contact = ev.getContact ();
      if (contact.getNumWaitingQueues() > 0 || contact.getNumAgentGroups() > 0)
         return;
      final TrunkGroup tg = contact.getTrunkGroup ();
      if (tg != null)
         tg.release (contact);
      contact.setExited (true);
      contact.setRouter (null);
      notifyServed (ev);
   }

   /**
    * This method must be called to notify that a contact exited the system
    * after being dequeued, \texttt{ev} representing the dequeue event. It
    * notifies any registered exited-contact listener, and releases the
    * communication channel taken by the contact.
    *
    * @param ev
    *           the dequeue event.
    */
   public void exitDequeued (DequeueEvent ev) {
      // The dequeued method in Contact is called by WaitingQueue
      final Contact contact = ev.getContact ();
      if (contact.getNumWaitingQueues() > 0 || contact.getNumAgentGroups() > 0)
         return;
      final TrunkGroup tg = contact.getTrunkGroup ();
      if (tg != null)
         tg.release (contact);
      contact.setRouter (null);
      contact.setExited (true);
      notifyDequeued (ev);
   }

   /**
    * This method can be called when the contact \texttt{contact} was blocked by
    * the router with blocking type \texttt{bType}. It notifies any registered
    * exited-contact listener, and releases the communication channel taken by
    * the contact. The \texttt{bType = }{@link #BLOCKTYPE_NOLINE} value is
    * reserved for the special case where there is no available communication
    * channel for the contact.
    *
    * @param contact
    *           the contact being blocked.
    * @param bType
    *           the blocking type.
    */
   public void exitBlocked (Contact contact, int bType) {
      contact.blocked (bType);
      if (contact.getNumWaitingQueues() > 0 || contact.getNumAgentGroups() > 0)
         return;
      if (bType != BLOCKTYPE_NOLINE) {
         final TrunkGroup tg = contact.getTrunkGroup ();
         if (tg != null)
            tg.release (contact);
      }
      contact.setRouter (null);
      contact.setExited (true);
      notifyBlocked (contact, bType);
   }

   /**
    * Starts the dialers after the service of a contact by an agent in group
    * \texttt{group}. This method is called after {@link #checkFreeAgents(AgentGroup,Agent)} and
    * should call the {@link Dialer#dial()} method on one or more dialers. The
    * default implementation starts all the dialers in the list
    * {@link #getDialers(int) getDialers}\texttt{ (group.getId())}.
    *
    * @param group
    *           the agent group being notified.
    */
   protected void startDialers (AgentGroup group) {
      final int gid = group.getId ();
      final List<Dialer> gDialers = getDialers (gid);
      final int nd = gDialers.size ();
      for (int i = 0; i < nd; i++) {
         final Dialer dialer = gDialers.get (i);
         dialer.dial ();
      }
   }

   /**
    * This method is called at the beginning of the
    * simulation to reset the state of
    * this router.
    */
   public void init() {}

   /**
    * Begins the service of the contact \texttt{contact} by trying to assign it
    * a free agent. The method must select an agent group with a free agent (or
    * a specific free agent), start the service, and return the end-service
    * event if the service was started, or \texttt{null} otherwise.
    *
    * @param contact
    *           the contact being routed to an agent.
    * @return the end-service event representing the started service, or
    *         \texttt{null} if the contact could not be served immediately.
    */
   protected abstract EndServiceEvent selectAgent (Contact contact);

   /**
    * Selects an agent for serving a queued contact in the context of rerouting.
    * The event \texttt{dqEv} is used to represent the dequeued contact, while
    * \texttt{numReroutingsDone} indicates the number of reroutings that has
    * happened so far. The method should return the end-service event
    * corresponding to the contact's new service by an agent, or \texttt{null}
    * for the contact to stay in queue.
    *
    * @param dqEv
    *           the dequeue event representing the queued contact.
    * @param numReroutingsDone
    *           the number of preceding reroutings.
    * @return the end-service event, or \texttt{null}.
    */
   protected EndServiceEvent selectAgent (DequeueEvent dqEv,
         int numReroutingsDone) {
      return null;
   }

   /**
    * Selects a waiting queue and puts the contact \texttt{contact} into it.
    * Returns the dequeue event if the contact could be queued, or \texttt{null}
    * otherwise.
    *
    * @param contact
    *           the contact being queued.
    * @return the dequeue event representing the queued contact, or
    *         \texttt{null} if the contact could not be queued.
    */
   protected abstract DequeueEvent selectWaitingQueue (Contact contact);

   /**
    * Contains the dequeue type used when a contact leaves a queue to enter a
    * new one. By default, this is set to 1.
    */
   protected int dqTypeRet = 1;

   /**
    * Selects a waiting queue for a queued contact in the context of rerouting.
    * The event \texttt{dqEv} is used to represent the queued contact, while
    * \texttt{numReroutingsDone} indicates the number of reroutings that has
    * happened so far. The method should return the dequeue event corresponding
    * to the contact's new queue, or \texttt{null} if the contact is required to
    * leave the system. If no transfer of queue is required, this method should
    * return \texttt{dqEv}. If a transfer occurs, one can use the
    * {@link #dqTypeRet} field to store the dequeue type of the contact leaving
    * the queue.
    *
    * @param dqEv
    *           the dequeue event representing the queued contact.
    * @param numReroutingsDone
    *           the number of preceding reroutings.
    * @return the dequeue event, or \texttt{null}.
    */
   protected DequeueEvent selectWaitingQueue (DequeueEvent dqEv,
         int numReroutingsDone) {
      return dqEv;
   }

   /**
    * This method is called when the agent \texttt{agent} in agent group
    * \texttt{group} becomes free. If the given agent is \texttt{null}, the
    * method assumes that one or more arbitrary agents in the group became free.
    * The method must select a contact to be transferred to the
    * free agent.
    * The selected contacts come from
    * waiting queues, and must be removed
    * from the queues with dequeue type {@link #DEQUEUETYPE_BEGINSERVICE} before
    * they are transferred to agents.
    * The method returns \texttt{true} if and only if at least one
    * free agent could be made busy.
    *
    * The default implementation calls {@link #selectContact(AgentGroup,Agent)} to get a new
    * dequeue event representing the
    * removed contact, extracts the contact,
    * and routes it to an agent, until \texttt{group} has no
    * more free agent.
    *
    * @param group
    *           the affected agent group.
    * @param agent
    *           the agent having ended its service.
    * @return \texttt{true} if some free agents became busy, \texttt{false}
    *         otherwise.
    */
   protected boolean checkFreeAgents (AgentGroup group, Agent agent) {
      boolean oneBusy = false;
      final boolean agentUnavailable = agent == null ? true : !agent.isAvailable ();
      while (group.getNumFreeAgents () > 0) {
         final DequeueEvent ev = selectContact (group, oneBusy || agentUnavailable ? null : agent);
         if (ev == null)
            return oneBusy;
         assert ev.dequeued ();
         final Contact ct = ev.getContact ();
         final EndServiceEvent es;
         if (oneBusy || agentUnavailable)
            es = group.serve (ct);
         else {
            assert group == agent.getAgentGroup () : "The given agent is in the wrong group";
            es = agent.serve (ct);
         }
         assert es != null : "The service of the selected contact could not start";
         assert es.getContact () == ct : "The started service corresponds to the wrong contact";
         oneBusy = true;
      }
      return oneBusy;
   }

   /**
    * Returns a dequeue event representing a queued contact
    * to be served by the agent \texttt{agent} in agent group
    * \texttt{group}. If \texttt{agent} is \texttt{null}, the method must return
    * a contact that can be served by any agent in the group. If no contact is
    * available, this method returns \texttt{null}.
    * The selected contacts come from waiting queues
    * attached to the router.
    * Before the selected contact is
    * returned, it must be removed from
    * its queue with dequeue type {@link #DEQUEUETYPE_BEGINSERVICE}, e.g., by using
    * {@link WaitingQueue#removeFirst(int) queue.removeFirst (DEQUEUETYPE\_BEGINSERVICE)}, or
    * {@link WaitingQueue#remove(DequeueEvent,int) queue.remove (ev, DEQUEUETYPE\_BEGINSERVICE)},
    * etc.
    *
    * Generally, it is sufficient
    * to override this method instead of {@link #checkFreeAgents(AgentGroup,Agent)}. One can
    * override {@link #checkFreeAgents(AgentGroup,Agent)} to improve efficiency when looking for
    * contacts in the same waiting queue. This method is not abstract and
    * returns \texttt{null} by default in order to allow
    * {@link #checkFreeAgents(AgentGroup,Agent)} to be overridden without implementing this
    * method.
    *
    * @param group
    *           the affected agent group.
    * @param agent
    *           the agent having ended its service.
    * @return the dequeue event representing the contact being selected.
    */
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      return null;
   }

   /**
    * Selects a new contact for the agent \texttt{agent}, in the context of
    * rerouting.
    *
    * @param agent
    *           the affected agent.
    * @param numReroutingsDone
    *           the number of preceding reroutings.
    * @return the selected contact, or \texttt{null}.
    */
   protected DequeueEvent selectContact (Agent agent, int numReroutingsDone) {
      return null;
   }

   /**
    * This method is called when the agent group \texttt{group} contains no more
    * online agents, i.e., {@link AgentGroup#getNumAgents()} returns 0. It must
    * check each waiting queue accessible for agents in this group to determine
    * if they need to be cleared. A queue is cleared if no agent, whether free
    * or busy, is available to serve any contact in it.
    *
    * @param group
    *           the agent group with no more agents.
    */
   protected abstract void checkWaitingQueues (AgentGroup group);

   /**
    * Returns the delay, in simulation time units, after which a queued contact
    * should be rerouted. The value of \texttt{numReroutingsDone} gives the
    * number of preceding reroutings, and \texttt{dqEv} is the dequeue event. If
    * this delay is negative, infinite, or NaN, no rerouting happens for the
    * contact. \texttt{numReroutings} will be -1 when this method is called at
    * the time the contact is queued.
    * By default, this method returns {@link Double#POSITIVE_INFINITY}.
    *
    * @param dqEv
    *           the dequeue event representing the queued contact.
    * @param numReroutingsDone
    *           the number of reroutings so far.
    * @return the rerouting delay.
    */
   protected double getReroutingDelay (DequeueEvent dqEv, int numReroutingsDone) {
      return Double.POSITIVE_INFINITY;
   }

   /**
    * Returns the delay, in simulation time units, after which an agent
    * \texttt{agent} should try a new time to get a contact to serve. If no
    * rerouting should happen, the returned delay must be negative or NaN.
    * \texttt{numReroutings} will be -1 when this method is called at the end of
    * a service.
    * By default, this method returns {@link Double#POSITIVE_INFINITY}.
    *
    * @param agent
    *           the idle agent, or \texttt{null}.
    * @param numReroutingsDone
    *           the number of previous reroutings for the agent.
    * @return the rerouting delay.
    */
   protected double getReroutingDelay (Agent agent, int numReroutingsDone) {
      return Double.POSITIVE_INFINITY;
   }

   /**
    * This method is called when the service of a contact, represented by the
    * event \texttt{ev}, begins. By default, this method does nothing.
    *
    * @param ev
    *           the end-service event.
    */
   protected void beginService (EndServiceEvent ev) {}

   /**
    * This method is called when the communication between a contact and an agent
    * is finished. By default, it calls {@link #exitServed(EndServiceEvent)}.
    *
    * @param ev
    *           the end-service event.
    */
   protected void endContact (EndServiceEvent ev) {
      exitServed (ev);
   }

   /**
    * This method is called when the service (communication and after-contact
    * work) of a contact in an agent group has ended. By default, this does
    * nothing.
    *
    * @param ev
    *           the end-service event.
    */
   protected void endService (EndServiceEvent ev) {}

   /**
    * This method is called when a contact is enqueued, \texttt{ev} representing
    * the dequeue event. By default, this method does nothing.
    *
    * @param ev
    *           the dequeue event.
    */
   protected void enqueued (DequeueEvent ev) {}

   /**
    * This method is called when a contact leaves a waiting queue, \texttt{ev}
    * representing the corresponding dequeue event. By default, for any
    * effective dequeue type other than 0, this calls {@link #exitDequeued(DequeueEvent)}.
    * This method should not notify an exiting contact for a 0 dequeue type
    * since it is reserved for queued and served contacts.
    *
    * @param ev
    *           the dequeue event.
    */
   protected void dequeued (DequeueEvent ev) {
      if (ev.getEffectiveDequeueType () == DEQUEUETYPE_BEGINSERVICE ||
            ev.getEffectiveDequeueType () == DEQUEUETYPE_TRANSFER)
         return;
      exitDequeued (ev);
   }

   /**
    * Formats the connected waiting queues as a string. For each queue slot, the
    * returned string contains a line with the text \texttt{Waiting queue q: }
    * followed by the queue's \texttt{toString} result. If no queue is connected
    * to the slot, \texttt{undefined} is used as the waiting queue descriptor.
    *
    * @return the waiting queues of the router.
    */
   public String formatWaitingQueues () {
      final StringBuilder sb = new StringBuilder ();
      for (int q = 0; q < queues.length; q++) {
         sb.append ("Waiting queue " + q + ": "
               + (queues[q] == null ? "undefined" : queues[q].toString ()));
         if (q < queues.length - 1)
            sb.append ("\n");
      }
      return sb.toString ();
   }

   /**
    * Formats the connected agent groups as a string. For each group slot, the
    * returned string contains a line with the text \texttt{Agent group i: }
    * followed by the group's \texttt{toString} result. If no group is connected
    * to the slot, \texttt{undefined} is used as the agent group descriptor.
    *
    * @return the agent groups of the router.
    */
   public String formatAgentGroups () {
      final StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < groups.length; i++) {
         sb.append ("Agent group ").append (i).append (": ");
         sb.append (groups[i] == null ? "undefined" : groups[i].toString ());
         if (i < groups.length - 1)
            sb.append ("\n");
      }
      return sb.toString ();
   }

   /**
    * Returns the waiting-queue listener registered with each waiting queue
    * connected to this router. Obtaining this listener can be useful to replace
    * it, in the list of listeners of a waiting queue, by a wrapper executing
    * code before or after some events.
    *
    * @return the waiting-queue listener registered with each waiting queue.
    */
   public WaitingQueueListener getWaitingQueueListener () {
      return rl;
   }

   /**
    * Returns the agent-group listener registered with each agent group
    * connected to this router. Obtaining this listener can be useful to replace
    * it, in the list of listeners of an agent group, by a wrapper executing
    * code before or after some events.
    *
    * @return the agent-group listener registered with each agent group.
    */
   public AgentGroupListener getAgentGroupListener () {
      return rl;
   }

   // Potential risk:
   // This listener causes problems, because it modifies the
   // state of the contact center, and calls
   // overridable methods.
   // The code of the listeners, or code in subclasses
   // of Router, could initiate other broadcasts by
   // agent groups and waiting queues, which will
   // recursively call methods in this class.
   // This design can result at best to inefficient execution,
   // at worst to infinite loops terminating in
   // StackOverflowError's.
   // Possible solution: in each method, schedule an event
   // at Sim.time() that executes the actual body.
   // But this adds overhead.
   private final class RouterListener implements WaitingQueueListener,
         AgentGroupListener {
      public void dequeued (DequeueEvent ev) {
         final Contact contact = ev.getContact ();
         final WaitingQueue queue = ev.getWaitingQueue ();
         if (contact.getRouter () != Router.this)
            return;
         --totalQueueSize;
         final int qid = queue.getId ();
         if (queues[qid] == queue)
            Router.this.dequeued (ev);
      }

      public void enqueued (DequeueEvent ev) {
         ++totalQueueSize;
         Router.this.enqueued (ev);
      }

      public void agentGroupChange (AgentGroup agentGroup) {
         final int gid = agentGroup.getId ();
         if (agentGroup == groups[gid]) {
            checkFreeAgents (agentGroup, null);
            if (agentGroup.getNumAgents () == 0)
               checkWaitingQueues (agentGroup);
         }
      }

      public void beginService (EndServiceEvent ev) {
         Router.this.beginService (ev);
      }

      public void endContact (EndServiceEvent ev) {
         final AgentGroup agentGroup = ev.getAgentGroup ();
         final int gid = agentGroup.getId ();
         if (agentGroup == groups[gid])
            Router.this.endContact (ev);
      }

      public void endService (EndServiceEvent ev) {
         final AgentGroup agentGroup = ev.getAgentGroup ();
         final Agent agent;
         if (!ev.wasGhostAgent () && ev instanceof EndServiceEventDetailed) {
            agent = ((EndServiceEventDetailed) ev).getAgent ();
            assert !agent.isGhost ();
         }
         else
            agent = null;
         final int gid = agentGroup.getId ();
         if (agentGroup == groups[gid]) {
            Router.this.endService (ev);
            if (!checkFreeAgents (agentGroup, agent) && agent != null) {
               final double delay = getReroutingDelay (agent, -1);
               if (delay >= 0 && !Double.isInfinite (delay)
                     && !Double.isNaN (delay)) {
                  final AgentReroutingEvent aev = new AgentReroutingEvent (
                        Router.this, agent, 0);
                  aev.schedule (delay);
               }
            }
            startDialers (agentGroup);
            if (agentGroup.getNumAgents () == 0)
               checkWaitingQueues (agentGroup);
         }
      }

      public void init (AgentGroup group) {}

      public void init (WaitingQueue queue) {
         computeSizes ();
      }

      private final void computeSizes () {
         totalQueueSize = 0;
         for (final WaitingQueue element : queues)
            if (element != null)
               totalQueueSize += element.size ();
      }

      @Override
      public String toString () {
         final StringBuilder sb = new StringBuilder (getClass ().getName ());
         sb.append ('[');
         sb.append ("associated router: ").append (Router.this.toString ());
         sb.append (']');
         return sb.toString ();
      }
   }

   public String getDescription () {
      return "";
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append ("number of contact types: ").append (getNumContactTypes ());
      sb.append (", number of agent groups: ").append (getNumAgentGroups ());
      sb.append (", number of waiting queues: ")
            .append (getNumWaitingQueues ());
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Returns a string representation of detailed information about the router.
    * This returns a string representation of each associated waiting queue and
    * agent group and routing policies. For a short, one-line description,
    * {@link #toString()} should be used.
    *
    * @return a string representation of detailed information about the router.
    */
   public String toLongString () {
      final StringBuilder sb = new StringBuilder ();
      final String desc = getDescription ();
      if (desc == null || desc.length () == 0)
         sb.append (desc).append ('\n');
      else
         sb.append (getClass ().getSimpleName ()).append ('\n');
      sb.append (getNumContactTypes ()).append (" supported contact types\n");
      sb.append (getNumWaitingQueues ()).append (" waiting queues\n");
      sb.append (formatWaitingQueues ()).append ("\n");
      sb.append (getNumAgentGroups ()).append (" agent groups\n");
      sb.append (formatAgentGroups ()).append ("\n");
      sb.append ("Queue capacity ").append (
            queueCapacity == Integer.MAX_VALUE ? "infinite" : String
                  .valueOf (queueCapacity));
      return sb.toString ();
   }
}
