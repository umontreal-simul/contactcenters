package umontreal.iro.lecuyer.contactcenters.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.MinValueGenerator;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;

/**
 * Represents an individual agent in a detailed agent group.
 *
 * Note: the {@link AgentListener} implementations are notified in the order of
 * the list returned by {@link #getAgentListeners()}, and an agent listener
 * modifying the list of listeners by using
 * {@link #addAgentListener(AgentListener)} or
 * {@link #removeAgentListener(AgentListener)} could result in unpredictable
 * behavior.
 */
public class Agent implements Initializable, Named
{
   private String name = "";
   DetailedAgentGroup group;
   int agentId = -1;
   boolean avail = true;
   boolean ghost = false;
   EndServiceEventDetailed es = null;
   double idleSimTime = Double.NaN;
   double firstLoginTime = Double.NaN;
   double lastLoginTime = Double.NaN;
   private final List<AgentListener> listeners = new ArrayList<AgentListener>();
   private final List<AgentListener> umListeners = Collections
         .unmodifiableList(listeners);
   private boolean broadcastInProgress;
   MinValueGenerator cgens = new MinValueGenerator(0);
   MinValueGenerator acgens = new MinValueGenerator(0);
   private Map<Object, Object> attributes = null;
   private Map<Integer, MultiPeriodGen> mapServiceTime;
   public String getName()
   {
      return name;
   }

   public void setName(String n)
   {
      if (n == null)
         throw new NullPointerException("The given name must not be null");
      name = n;
   }

   /**
    * Constructs and returns a token object containing the state of this agent.
    *
    * @return the state of this agent.
    */
   public AgentState save()
   {
      return new AgentState(this);
   }

   /**
    * Restores the state of this agent by using the given state object
    * \texttt{state}.
    *
    * @param state
    *            the state of the agent.
    */
   public void restore(AgentState state)
   {
      state.restore(this);
   }

   /**
    * Adds the agent listener \texttt{listener} to this object.
    *
    * @param listener
    *            the agent listener being added.
    * @exception NullPointerException
    *                if \texttt{listener} is \texttt{null}.
    */
   public void addAgentListener(AgentListener listener)
   {
      if (listener == null)
         throw new NullPointerException(
            "The added listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException(
            "Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains(listener))
         listeners.add(listener);
   }

   /**
    * Removes the agent listener \texttt{listener} from this object.
    *
    * @param listener
    *            the agent listener being removed.
    */
   public void removeAgentListener(AgentListener listener)
   {
      if (broadcastInProgress)
         throw new IllegalStateException(
            "Cannot modify the list of listeners while broadcasting");
      listeners.remove(listener);
   }

   /**
    * Removes all the agent listeners registered with this agent.
    */
   public void clearAgentListeners()
   {
      if (broadcastInProgress)
         throw new IllegalStateException(
            "Cannot modify the list of listeners while broadcasting");
      listeners.clear();
   }

   /**
    * Returns an unmodifiable list containing all the agent listeners
    * registered with this agent.
    *
    * @return the list of all registered agent listeners.
    */
   public List<AgentListener> getAgentListeners()
   {
      return umListeners;
   }

   protected void notifyInit()
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).init(this);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyAvailable(boolean avail1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).agentAvailable(this, avail1);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyAdded(DetailedAgentGroup group1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).agentAdded(this, group1);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyRemoved(DetailedAgentGroup group1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).agentRemoved(this, group1);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyBeginService(EndServiceEventDetailed es1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).beginService(es1);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyEndContact(EndServiceEventDetailed es1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).endContact(es1);
      } finally {
         broadcastInProgress = old;
      }
   }

   protected void notifyEndService(EndServiceEventDetailed es1)
   {
      final int nl = listeners.size();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get(i).endService(es1);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Instructs this agent to begin the service of the contact
    * \texttt{contact}, and returns the constructed end-service event
    * representing the service. This method calls {@link #getAgentGroup()}
    * {@link DetailedAgentGroup#serve(Contact,Agent) .serve (contact, this)}.
    *
    * @param contact
    *            the contact to be served.
    * @return the end-service event representing the service.
    */
   public EndServiceEventDetailed serve(Contact contact)
   {
      if (group == null)
         throw new NullPointerException(
            "An agent must be in a group to serve contacts");
      return group.serve(contact, this);
   }

   /**
    * Returns the value generator used to generate contact times for
    * end-contact type \texttt{ecType}. This returns \texttt{null} if there is
    * no value generator associated with this type of contact termination.
    *
    * @param ecType1
    *            the queried end-contact type.
    * @return the value generator associated with this end-contact type.
    */
   public ValueGenerator getContactTimeGenerator(int ecType1)
   {
      return cgens.getValueGenerator(ecType1);
   }

   /**
    * Returns the value generator used to generate after-contact times for
    * end-service type \texttt{esType}. This returns \texttt{null} if there is
    * no value generator associated with this type of service termination.
    *
    * @param esType1
    *            the queried end-service type.
    * @return the value generator associated with this end-service type.
    */
   public ValueGenerator getAfterContactTimeGenerator(int esType1)
   {
      return acgens.getValueGenerator(esType1);
   }

   /**
    * Sets the contact time generator for end-contact type \texttt{ecType} to
    * \texttt{cgen}.
    *
    * @param ecType
    *            the affected end-contact type.
    * @param cgen
    *            the new contact time generator associated with this
    *            end-contact type.
    * @exception IllegalArgumentException
    *                if the end-contact type is negative.
    */
   public void setContactTimeGenerator(int ecType, ValueGenerator cgen)
   {
      cgens.setValueGenerator(ecType, cgen);
   }

   /**
    * Sets the after-contact time generator for end-service type
    * \texttt{esType} to \texttt{acgen}.
    *
    * @param esType
    *            the modified end-service type.
    * @param acgen
    *            the new after-contact time generator associated with this
    *            end-service type.
    * @exception IllegalArgumentException
    *                if the end-service type is negative.
    */
   public void setAfterContactTimeGenerator(int esType, ValueGenerator acgen)
   {
      acgens.setValueGenerator(esType, acgen);
   }

   /**
    * Initializes this agent for a new simulation replication.
    */
   public void init()
   {
      cgens.init();
      acgens.init();
      firstLoginTime = group == null ? Double.NaN : group.simulator().time();
      lastLoginTime = firstLoginTime;
      idleSimTime = firstLoginTime;
      if (es != null) {
         es.cancel();
         es = null;
      }
      ghost = false;
      avail = true;
      notifyInit();
   }

   /**
    * Determines if the agent is available, or is serving contacts.
    *
    * @return the availability status of this agent.
    */
   public boolean isAvailable()
   {
      return avail;
   }

   /**
    * Sets the availability status of this agent to \texttt{avail}. If this
    * method is called with \texttt{true}, the agent will be capable of
    * processing new contacts (the default). Otherwise, it will not receive new
    * contacts. This does not affect the contact being served by this agent if
    * it is busy.
    *
    * @param avail
    *            the new availability status of this agent.
    */
   public void setAvailable(boolean avail)
   {
      if (this.avail != avail) {
         this.avail = avail;
         if (!avail && group != null)
            group.idleEqualFree = false;
         notifyAvailable(avail);
         if (es == null && group != null) {
            if (avail)
               ++group.numFreeAgents;
            else
               --group.numFreeAgents;
            group.notifyChange();
         }
      }
   }

   /**
    * Determines if this agent is a ghost, i.e., if it was removed from an
    * agent group before it has ended the service of a contact.
    *
    * @return \texttt{true} if the agent is a ghost agent, \texttt{false}
    *         otherwise.
    */
   public boolean isGhost()
   {
      return ghost;
   }

   /**
    * Determines if this agent is busy.
    *
    * @return the agent's busyness indicator.
    */
   public boolean isBusy()
   {
      return es != null;
   }

   /**
    * Returns the current end-service event for this agent, or \texttt{null} if
    * the agent is not busy.
    *
    * @return the current end-service event, or \texttt{null}.
    */
   public EndServiceEventDetailed getEndServiceEvent()
   {
      return es;
   }

   /**
    * Returns the last simulation time at which this agent became idle.
    *
    * @return the simulation idle time of this agent.
    * @exception IllegalStateException
    *                if this agent is not idle.
    */
   public double getIdleSimTime()
   {
      if (es != null)
         throw new IllegalStateException("Agent is not idle");
      return idleSimTime;
   }

   /**
    * Returns the time elapsed since the last moment this agent became idle.
    * This corresponds to the current simulation time minus the result of
    * {@link #getIdleSimTime()}.
    *
    * @return the agent's idle time.
    */
   public double getIdleTime()
   {
      return group.simulator().time() - getIdleSimTime();
   }

   public void setIdleSimTime(double idleSimTime)
   {
      this.idleSimTime = idleSimTime;
   }

   /**
    * Returns the first simulation time at which this agent was added to an
    * agent group.
    *
    * @return the agent's first login time.
    */
   public double getFirstLoginTime()
   {
      return firstLoginTime;
   }

   public void setFirstLoginTime(double firstLoginTime)
   {
      this.firstLoginTime = firstLoginTime;
   }

   /**
    * Returns the last simulation time at which this agent was added to an
    * agent group.
    *
    * @return the agent's last login time.
    */
   public double getLastLoginTime()
   {
      return lastLoginTime;
   }

   public void setLastLoginTime(double lastLoginTime)
   {
      this.lastLoginTime = lastLoginTime;
   }

   /**
    * Returns the detailed agent group this agent is part of, or \texttt{null}
    * if the agent is not in a group.
    *
    * @return the parent agent group.
    */
   public DetailedAgentGroup getAgentGroup()
   {
      if (group != null)
         return group;
      if (es != null)
         return es.getAgentGroup();
      return null;
   }

   /**
    * The end-contact type associated with the contact time returned by
    * {@link #getContactTime(Contact)}.
    */
   protected int ecType = 0;

   /**
    * Generates and returns the contact time associated with the contact
    * \texttt{contact}. The method returns the generated value and can store an
    * end-contact type indicator in the protected field {@link #ecType} if the
    * default value of 0 is not appropriate. If this returns {@link Double#NaN}
    * , the contact time will be generated by the parent agent group.
    *
    * By default, a {@link MinValueGenerator} is used. For each end-contact
    * type $c$ with an associated value generator, a contact time $C_c$ is
    * generated. The scheduled contact time is $C_{c^*}=\min_c\{C_c\}$, and the
    * end-contact type is $c^*$.
    *
    * @param contact
    *            the contact being served.
    * @return the generated contact time.
    */
   protected double getContactTime(Contact contact)
   {
      final double stime = cgens.nextDouble(contact);
      if (Double.isNaN(stime)) {
         ecType = 0;
         return Double.NaN;
      } else
         ecType = cgens.getLastVType();
      return stime;
   }

   /**
    * The end-service type associated with the after-contact time returned by
    * {@link #getAfterContactTime(Contact)}.
    */
   protected int esType = 0;

   /**
    * Generates and returns the after-contact time associated with the contact
    * \texttt{contact}. The method returns the generated value and can store an
    * end-service type indicator in the protected field {@link #esType} if the
    * default value of 0 is not appropriate. If this returns {@link Double#NaN}
    * , the after-contact time will be generated by the parent agent group.
    *
    * By default, a {@link MinValueGenerator} is used. For each end-service
    * type $c$ with an associated value generator, an after-contact time $C_c$
    * is generated. The scheduled after-contact time is
    * $C_{c^*}=\min_c\{C_c\}$, and the end-service type is $c^*$.
    *
    * @param contact
    *            the contact being served.
    * @return the generated after-contact time.
    */
   protected double getAfterContactTime(Contact contact)
   {
      final double stime = acgens.nextDouble(contact);
      if (Double.isNaN(stime)) {
         esType = 0;
         return Double.NaN;
      } else
         esType = acgens.getLastVType();
      return stime;
   }

   /**
    * Returns the identifier associated with this agent. This identifier, which
    * defaults to \texttt{-1}, can be used as an index in routers.
    *
    * @return the identifier associated with this agent.
    */
   public int getId()
   {
      return agentId;
   }

   /**
    * Sets the identifier of this agent to \texttt{id}. Once this identifier is
    * set to a positive or 0 value, it cannot be changed anymore.
    *
    * @param id
    *            the new identifier associated with the agent.
    * @exception IllegalStateException
    *                if the identifier was already set.
    */
   public void setId(int id)
   {
      if (agentId >= 0 && id != agentId)
         throw new IllegalStateException("Identifier already set");
      agentId = id;
   }

   /**
    * Returns the map containing the service time distributions
    * defined specifically for the call types and this agent.
    * Note that this map does not contain the service time
    * distributions defined for a group or all groups.
    *
    * @return the map where the keys are call type ids, obtained
    * by {@link umontreal.iro.lecuyer.contactcenters.contact.Contact#getTypeId}, and
    * the values are the service time distributions.
    */
   public Map<Integer, MultiPeriodGen> getMapServiceTime()
   {
      return mapServiceTime;
   }


   /**
    * Sets the map containing the service time distributions
    * defined specifically for the call types and this agent.
    * Note that this map should not contain the service time
    * distributions defined for a group or all groups.
    *
    * @param map the map where the keys are call type ids, obtained
    * by {@link umontreal.iro.lecuyer.contactcenters.contact.Contact#getTypeId}, and
    * the values are the service time distributions.
    */
   public void setMapServiceTime(Map<Integer, MultiPeriodGen> map)
   {
      if (map != null)
         mapServiceTime = map;
   }

   /**
    * Returns the map containing the attributes for this agent. Attributes can
    * be used to add user-defined information to agent objects at runtime,
    * without creating a subclass. However, for maximal efficiency, it is
    * recommended to create a subclass of \texttt{Agent} instead of using
    * attributes.
    *
    * @return the map containing the attributes for this object.
    */

   public Map<Object, Object> getAttributes()
   {
      if (attributes == null)
         attributes = new HashMap<Object, Object>();
      return attributes;
   }

   public void setAttributes(Map<Object, Object> map)
   {
      if (attributes == null)
         attributes = map;
   }

   @Override
   public String toString()
   {
      final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
      sb.append('[');
      if (getName().length() > 0)
         sb.append("name: ").append(getName()).append(", ");
      if (agentId != -1)
         sb.append("id: ").append(agentId).append(", ");
      if (group == null)
         sb.append("no parent group");
      else
         sb.append("parent group: ").append(
            ContactCenter.toShortString(group));
      if (avail)
         sb.append(", agent can serve contacts");
      else
         sb.append(", agent cannot serve contacts");
      if (avail)
         if (es == null)
            sb.append(", no contact being served");
         else {
            sb.append(", serving contact: ").append(
               ContactCenter.toShortString(es.getContact()));
            if (ghost)
               sb.append(", agent disappears after its service");
         }
      if (group != null) {
         sb.append(", first login simulation time: ").append(firstLoginTime);
         sb.append(", last login simulation time: ").append(lastLoginTime);
         sb.append(", idle simulation time: ").append(idleSimTime);
      }
      sb.append(']');
      return sb.toString();
   }
}
