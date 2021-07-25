package umontreal.iro.lecuyer.contactcenters.server;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.MinValueGenerator;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeListener;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
/**
 * Represents a group $i$ of agents capable of serving some types of contacts.
 * An instance of this class keeps counters for the number of agents in a group,
 * and provides logic to manage the service of contacts. It also defines a list
 * of observers being notified when the agent group changes.
 *
 * An agent group contains $N_i(t)\in\NN$ members at simulation time $t$. Among
 * these agents, $\Ni[i](t)$ are idle and $\Nb[i](t)$ are busy. Since agents
 * terminate their service before they leave, we can have $N_i(t)<\Nb[i](t)$,
 * in which case $\Ng[i](t)=\Nb[i](t)-N_i(t)$ \emph{ghost agents} need to
 * disappear after they finish their work. As a result, the true number of
 * agents in a group~$i$ at time~$t$ is given by $N_i(t)+\Ng[i](t)$. New
 * contacts are not accepted by the group when $N_i(t)\le\Nb[i](t)$. Since
 * $\Nb[i](t)$ includes the ghost agents, we have \begin{equation}
 * N_i(t)+\Ng[i](t)=\Nb[i](t)+\Ni[i](t). \label{eq:Nit} \end{equation}
 *
 * Some idle agents may be unavailable to serve contacts at some times during
 * their shift. They can be taking unplanned breaks, going to the bathroom, etc.
 * These details can be modeled in the simulation if the appropriate information
 * is available. But in practice they are often approximated by various models
 * such as an efficiency factor $\epsilon_i\in[0,1]$, which corresponds to the
 * fraction of agents being effectively busy or available to serve contacts. If
 * $\Nb[i](t)=0$, the number of free agents $\Nf[i](t)$ available to serve
 * contacts is given by $\Nf[i](t)=\mathrm{round}(\epsilon_i N_i(t))$ where
 * $\mathrm{round}(\cdot)$ rounds its argument to the nearest integer. If
 * $\Nb[i](t)>0$, the number of busy members of the group,
 * $\Nb[i](t)-\Ng[i](t)$, needs to be subtracted to get $\Nf[i](t)$. This
 * yields: \begin{equation} \mathrm{round}(\epsilon_i N_i(t)) + \Ng[i](t) =
 * \Nb[i](t)+\Nf[i](t). \label{eq:Nit2} \end{equation} If $\epsilon_i=1$,
 * $\Nf[i](t)=\Ni[i](t)$ and we are back to (\ref{eq:Nit}). This elementary
 * efficiency model is provided because it can be used without simulating
 * individual agents. When agents are differentiated, other more complex and
 * more realistic models can easily be implemented by manipulating the state of
 * agents during simulation.
 *
 * The service of a contact, started by the {@link #serve(Contact)} method, is divided in
 * two steps. After communicating with a customer (first step), an agent can
 * perform after-contact work (second step), e.g., update an account, take some
 * notes, etc.
 * After the first step, the contact may exit the system, or be
 * transferred to another agent. However, the agent becomes free only after the
 * second step (if any) is over. The end of these steps is scheduled using a
 * simulation event {@link EndServiceEvent} that contains additional information
 * about the service. Service can be terminated automatically through the event
 * or manually through the {@link #endContact(EndServiceEvent,int)} and
 {@link #endService(EndServiceEvent,int)} methods of
 * this class. Special indicators called the \emph{end-contact type} and
 * \emph{end-service type} tell us which type of termination has occurred for
 * each step.
 * By default, the two steps of the service are terminated automatically
 * after durations obtained using
 * the {@link Contact#getDefaultContactTime()} and
 * {@link Contact#getDefaultAfterContactTime()} methods of the
 * concerned contact, respectively.
 * These default times can be set to infinity if services
 * need to be terminated manually, conditional on some event.
 * The way times are obtained can also be changed
 * by setting value generators
 * using {@link #setContactTimeGenerator(int,ValueGenerator)},
 * and {@link #setAfterContactTimeGenerator(int,ValueGenerator)},
 * or by overriding {@link #getContactTime(EndServiceEvent)}
 * and {@link #getAfterContactTime(EndServiceEvent)}.
 *
 * Registered \emph{agent-group listeners} can be notified when $N_i(t)$
 * changes, when a service starts, and when it ends.
 *
 * Note: the {@link AgentGroupListener} implementations are notified in the
 * order of the list returned by {@link #getAgentGroupListeners()}, and an
 * agent-group listener modifying the list of listeners by using
 * {@link #addAgentGroupListener(AgentGroupListener)} or {@link #removeAgentGroupListener(AgentGroupListener)} could
 * result in unpredictable behavior.
 *
 * An agent group can also be viewed as a collection
 * of end-service events.
 * For this reason, this class implements
 * the {@link Collection} interface.
 * The collection contains end-service events
 * corresponding to in-progress services.
 * Its size thus always corresponds to the
 * number of busy agents.
 */
public class AgentGroup extends AbstractCollection<EndServiceEvent>
         implements PeriodChangeListener, Initializable, Named
{
   private String name = "";
   PeriodChangeEvent pce = null;
   int[] allNumAgents;
   int groupId = -1;
   private final List<AgentGroupListener> listeners = new ArrayList<AgentGroupListener> ();
   private final List<AgentGroupListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;
   Set<EndServiceEvent> esevSet;
   private Set<EndServiceEvent> umEsevSet;
   private Map<Object, Object> attributes = null;

   int initCount = 0;
   int numAgents;
   int numFreeAgents;
   int numBusyAgents;
   int[] numBusyAgentsK = new int[1];
   int numGhostAgents;
   double efficiency = 1.0;
   MinValueGenerator cgens = new MinValueGenerator (1);
   MinValueGenerator acgens = new MinValueGenerator (1);
   private Agent agent = null;

   /**
    * Constructs a new agent group with \texttt{n} available agents.
    *
    * @param n
    *           the number of agents in the group.
    */
   public AgentGroup (int n)
   {
      allNumAgents = new int[0];
      initNumAgents (n);
   }

   /**
    * Constructs a new agent group with the period-change event \texttt{pce},
    * and \texttt{ns[p]} agents in the period \texttt{p}. The agent group is
    * automatically added to the period-change event for the number of agents to
    * be set automatically during the simulation.
    *
    * @param pce
    *           the period-change event defining the simulation periods.
    * @param ns
    *           the number of agents in the group for each period.
    * @exception IllegalArgumentException
    *               if there is not a number of agent for each period.
    */
   public AgentGroup (PeriodChangeEvent pce, int[] ns)
   {
      if (pce.getNumPeriods () != ns.length)
         throw new IllegalArgumentException (
            "Needs one number of agents for each period");
      allNumAgents = ns.clone ();
      initNumAgents (0);
      pce.addPeriodChangeListener (this);
      this.pce = pce;
   }

   @Override
   public String getName ()
   {
      return name;
   }

   @Override
   public void setName (String name)
   {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
   }

   /**
    * Returns the period-change event associated with this agent group.
    *
    * @return the associated period-change event.
    */
   public PeriodChangeEvent getPeriodChangeEvent ()
   {
      return pce;
   }

   /**
    * Returns $\epsilon_i$, the fraction of free and busy agents available to
    * serve contacts over the total number of agents. The default efficiency is
    * set to 1.
    *
    * @return the agents' efficiency.
    */
   public double getEfficiency ()
   {
      return efficiency;
   }

   /**
    * Changes the agents' efficiency to \texttt{eff}. This calls
    * {@link #setNumAgents(int)} to update the number of free agents according to the
    * new efficiency factor. If there is no busy agent, the number of free
    * agents is given by {@link #getNumAgents()}\texttt{*eff}, rounded to the
    * nearest integer. The efficiency factor must be in $[0,1]$, otherwise an
    * exception is thrown.
    *
    * @param eff
    *           the new efficiency.
    * @exception IllegalArgumentException
    *               if the efficiency factor is smaller than 0 or greater than
    *               1.
    */
   public void setEfficiency (double eff)
   {
      /*
       * If the efficiency is allowed to be greater than 1, the number of busy
       * agents plus the number of free agents will exceed the number of
       * available agents, which will get occupancy rates greater than 1 when
       * using the GroupVolumeStat class.
       */
      if (eff < 0 || eff > 1)
         throw new IllegalArgumentException ("The efficiency must be in [0,1]");
      efficiency = eff;
      setNumAgents (numAgents);
   }

   /**
    * Returns the value generator used to generate contact times for end-contact
    * type \texttt{ecType}. This returns \texttt{null} if there is no value
    * generator associated with this type of contact termination. By default, a
    * non-\texttt{null} value is returned for \texttt{ecType = 0} only.
    *
    * @param ecType
    *           the queried end-contact type.
    * @return the value generator associated with this end-contact type.
    */
   public ValueGenerator getContactTimeGenerator (int ecType)
   {
      return cgens.getValueGenerator (ecType);
   }

   /**
    * Returns the value generator used to generate after-contact times for
    * end-service type \texttt{esType}. This returns \texttt{null} if there is
    * no value generator associated with this type of service termination.
    *
    * @param esType
    *           the queried end-service type.
    * @return the value generator associated with this end-service type.
    */
   public ValueGenerator getAfterContactTimeGenerator (int esType)
   {
      return acgens.getValueGenerator (esType);
   }

   /**
    * Sets the contact time generator for end-contact type \texttt{ecType} to
    * \texttt{cgen}.
    *
    * @param ecType
    *           the affected end-contact type.
    * @param cgen
    *           the new contact time generator associated with this end-contact
    *           type.
    * @exception IllegalArgumentException
    *               if the end-contact type is negative.
    */
   public void setContactTimeGenerator (int ecType, ValueGenerator cgen)
   {
      cgens.setValueGenerator (ecType, cgen);
   }

   /**
    * Sets the after-contact time generator for end-service type \texttt{esType}
    * to \texttt{acgen}.
    *
    * @param esType
    *           the affected end-service type.
    * @param acgen
    *           the new after-contact time generator associated with this
    *           end-service type.
    * @exception IllegalArgumentException
    *               if the end-service type is negative.
    */
   public void setAfterContactTimeGenerator (int esType, ValueGenerator acgen)
   {
      acgens.setValueGenerator (esType, acgen);
   }

   /**
    * Initializes the agent group for a new simulation replication. It must be
    * called after the simulator is initialized
    * and before it is started.
    */
   @Override
   public void init ()
   {
      // If init is called during a simulation replication (not recommended
      // but not technically impossible), some end of service events
      // can be pending and will set this object into an inconsistent state
      // (negative number of busy agents). The initialization
      // counter will make these old end-of-service events
      // disappear silently when they occur, without
      // incurring the cost of keeping track of them.
      // Other possibility: getting the event list and iterating
      // through all the events, but it is in linear time
      // and o instanceof EndServiceEvent returns true even
      // if this is an end-of-service event for another agent group.
      ++initCount;
      // This sets numAgents and numFreeAgents to 0.
      // No busy or ghost agents
      numBusyAgents = 0;
      numGhostAgents = 0;
      Arrays.fill (numBusyAgentsK, 0);

      // In non-stationary cases, we need to set the number
      // of agents to the value in the preliminary period.
      if (allNumAgents.length > 0)
         numAgents = allNumAgents[0];
      numFreeAgents = (int) Math.round (numAgents * efficiency);
      cgens.init ();
      acgens.init ();
      if (esevSet != null)
         esevSet.clear ();
      notifyInit();
   }

   /**
    * Determines if this object keeps track of the end-service events for
    * contacts in service by an agent. If this returns \texttt{true}, the events
    * are stored. Otherwise (the default), they are stored in the SSJ event list
    * only.
    *
    * @return the value of the keep end-service events flag.
    */
   public boolean isKeepingEndServiceEvents ()
   {
      return esevSet != null;
   }

   /**
    * Sets the keep end-service-event indicator to \texttt{keepEsev}.
    *
    * @param keepEsev
    *           the new value of the indicator.
    * @see #isKeepingEndServiceEvents()
    */
   public void setKeepingEndServiceEvents (boolean keepEsev)
   {
      if (keepEsev && esevSet == null) {
         if (getNumBusyAgents() > 0) {
            final Iterator<EndServiceEvent> it = endServiceEventsIterator ();
            esevSet = new HashSet<EndServiceEvent> ();
            while (it.hasNext ())
               esevSet.add (it.next ());
         } else
            esevSet = new HashSet<EndServiceEvent> ();
         umEsevSet = Collections.unmodifiableSet (esevSet);
      } else if (!keepEsev) {
         if (esevSet != null)
            // If one called getEndServiceEvents, he might still
            // have a reference to esevSet. We clear
            // the set to free the memory taken by the events
            // and for consistency.
            esevSet.clear ();
         esevSet = null;
         umEsevSet = null;
      }
   }

   /**
    * Constructs and returns an iterator for the end-service events. If
    * {@link #isKeepingEndServiceEvents()} returns \texttt{true}, the iterator
    * is constructed from the set returned by {@link #getEndServiceEvents()}.
    * Otherwise, an illegal state exception is thrown.
    * % iterator traversing the event list and filtering the
    * % appropriate events is constructed and returned.
    *
    * @return the iterator for end-service events.
    */
   public Iterator<EndServiceEvent> endServiceEventsIterator()
   {
      if (!isKeepingEndServiceEvents())
         //         return new FilteredIterator<EndServiceEvent> (Sim.getEventList ()
         //               .iterator (), getNumBusyAgents()) {
         //            @Override
         //            public boolean filter (Object ev) {
         //               if (ev instanceof EndServiceEvent) {
         //                  final EndServiceEvent es = (EndServiceEvent) ev;
         //                  if (es.getAgentGroup () == AgentGroup.this)
         //                     return true;
         //               }
         //               return false;
         //            }
         //         };
         throw new IllegalStateException
         ("Cannot iterate over end-service events; call setKeepingEndServiceEvent(true) while this agent group has no busy agents");
      else
         return getEndServiceEvents ().iterator ();
   }

   @Override
   public Iterator<EndServiceEvent> iterator()
   {
      return endServiceEventsIterator ();
   }

   @Override
   public int size ()
   {
      return getNumBusyAgents();
   }

   @Override
   public boolean contains (Object o)
   {
      if (!(o instanceof EndServiceEvent))
         return false;
      final EndServiceEvent es = (EndServiceEvent)o;
      return es.getAgentGroup () == this &&
             (!es.contactDone() || !es.afterContactDone());
   }

   /**
    * Returns a reference to a set containing all the end-service events for
    * this agent group. This set contains the end-service events for each
    * contact currently served by an agent. As soon as a contact ends its
    * service (including after-contact work), it is removed from the set. If the
    * agent group does not keep track of these events (the default), this throws
    * an {@link IllegalStateException}.
    *
    * @return the set of end-service events.
    * @exception IllegalStateException
    *               if the agent group does not keep end-service events.
    */
   public Set<EndServiceEvent> getEndServiceEvents ()
   {
      if (esevSet == null)
         throw new IllegalStateException (
            "The end-service events are not kept, use init(true) to enable end-service events keeping");
      return umEsevSet;
   }

   /**
    * Begins the service of the contact \texttt{contact} and returns the
    * constructed end-service event. If no agent is available to serve the
    * contact, an {@link IllegalStateException} is thrown. Otherwise, a contact
    * time is obtained using {@link #getContactTime(EndServiceEvent)}.
    * The end-service event is then constructed and scheduled
    * if the contact time is not infinite. If an
    * infinite contact time is generated, one must manually abort the
    * communication using {@link #endContact(EndServiceEvent,int)}
    * or schedule the end-service event.
    * When the communication is over, the same rules are applied for generating
    * the after-contact time using {@link #getAfterContactTime(EndServiceEvent)}. When the
    * after-contact time is finite, the end-service event is scheduled a second
    * time for the service termination.
    *
    * @param contact
    *           the contact to be served.
    * @return a reference to the end-service event.
    * @exception IllegalStateException
    *               if no free agent is available.
    */
   public EndServiceEvent serve (Contact contact)
   {
      if (numFreeAgents == 0)
         throw new IllegalStateException ("No free agents");

      final EndServiceEvent es = new EndServiceEvent (this, contact,
                                 contact.simulator ().time ());

      ecTypeRet = 0;
      double stime = getContactTime (es);
      if (stime < 0)
         stime = 0;
      es.contactTime = stime;
      es.ecType = ecTypeRet;
      es.afterContactTimeSet = false;
      return internalServe (es);
   }

   /**
    * This is similar to {@link #serve(Contact)}, except that the specified
    * contact time and end-contact type are used instead of generated ones. The
    * after-contact time is generated as in {@link #serve(Contact)}. The main
    * purpose of this method is for recreating an end-service event based on
    * saved state information.
    *
    * @param contact
    *           the contact being served.
    * @param contactTime
    *           the communication time of the contact with the agent.
    * @param ecType
    *           the end-contact type.
    * @return the end-service event representing the service.
    * @exception IllegalStateException
    *               if no free agent is available.
    */
   public EndServiceEvent serve (Contact contact, double contactTime, int ecType)
   {
      if (numFreeAgents == 0)
         throw new IllegalStateException ("No free agents");

      final EndServiceEvent es = new EndServiceEvent (this, contact,
                                 contact.simulator ().time ());

      es.contactTime = contactTime;
      es.ecType = ecType;
      es.afterContactTimeSet = false;
      return internalServe (es);
   }

   /**
    * This is similar to {@link #serve(Contact)} except that the contact and
    * after-contact times are specified explicitly. The main purpose of this
    * method is for recreating an end-service event based on saved state
    * information.
    *
    * @param contact
    *           the contact being served.
    * @param contactTime
    *           the contact time.
    * @param ecType
    *           the end-contact type.
    * @param afterContactTime
    *           the after-contact time.
    * @param esType
    *           the end-service type.
    * @return the end-service event representing the service.
    * @exception IllegalStateException
    *               if no free agent is available.
    */
   public EndServiceEvent serve (Contact contact, double contactTime,
                                 int ecType, double afterContactTime, int esType)
   {
      if (numFreeAgents == 0)
         throw new IllegalStateException ("No free agents");

      final EndServiceEvent es = new EndServiceEvent (this, contact,
                                 contact.simulator ().time ());

      es.contactTime = contactTime;
      es.ecType = ecType;
      es.afterContactTime = afterContactTime;
      es.esType = esType;
      es.afterContactTimeSet = true;
      return internalServe (es);
   }

   /**
    * Starts the service of a contact based on information stored in the old
    * end-service event \texttt{oldEndServiceEvent}. If the event contains
    * information about the effective end-contact time, i.e., if
    * {@link EndServiceEvent#contactDone()} returns \texttt{true}, the method
    * uses the effective end-contact time and end-contact type, and the
    * scheduled end-service time and end-service type to start the service.
    * Otherwise, it uses the scheduled end-contact time and end-contact type
    * only.
    *
    * @param oldEndServiceEvent
    *           the old end-service event.
    * @return the new end-service event.
    */
   public EndServiceEvent serve (EndServiceEvent oldEndServiceEvent)
   {
      if (oldEndServiceEvent.contactDone ())
         return serve (oldEndServiceEvent.getContact (), oldEndServiceEvent
                       .getEffectiveContactTime (), oldEndServiceEvent
                       .getEffectiveEndContactType (), oldEndServiceEvent
                       .getScheduledAfterContactTime (), oldEndServiceEvent
                       .getScheduledEndServiceType ());
      else
         return serve (oldEndServiceEvent.getContact (), oldEndServiceEvent
                       .getScheduledContactTime (), oldEndServiceEvent
                       .getScheduledEndContactType ());
   }

   private EndServiceEvent internalServe (EndServiceEvent es)
   {
      beginServiceUpdateStatus (es);
      final double stime = es.contactTime;
      if (!Double.isInfinite (stime) && stime > 0)
         es.schedule (stime);
      es.getContact ().beginService (es);
      notifyBeginService (es);
      if (stime == 0)
         completeContact (es.ecType, es, false);
      assert getNumAgents() + getNumGhostAgents() == getNumBusyAgents() + getNumIdleAgents();
      return es;
   }

   void beginServiceUpdateStatus (EndServiceEvent es)
   {
      if (esevSet != null)
         esevSet.add (es);
      ++numBusyAgents;
      --numFreeAgents;
      final int k = es.getContact ().getTypeId ();
      if (k >= 0) {
         if (k >= numBusyAgentsK.length)
            numBusyAgentsK = ArrayUtil.resizeArray (numBusyAgentsK, k + 1);
         ++numBusyAgentsK[k];
      }
   }

   /**
    * Constructs a new {@link AgentGroupState} instance holding the state of
    * this agent group. The method {@link #isKeepingEndServiceEvents()} must
    * return \texttt{true} for this method to be called, because the state
    * includes every contact served by agents in this group.
    *
    * @return the state of this agent group.
    */
   public AgentGroupState save ()
   {
      return new AgentGroupState (this);
   }

   /**
    * Restores the state of this agent group by using the
    * {@link AgentGroupState#restore(AgentGroup) restore} method of
    * \texttt{state}.
    *
    * @param state
    *           the saved state of this agent group.
    */
   public void restore (AgentGroupState state)
   {
      state.restore (this);
   }

   /**
    * The end-contact type associated with the contact time returned by
    * {@link #getContactTime(EndServiceEvent)}.
    */
   protected int ecTypeRet = 0;

   /**
    * Generates and returns the contact time for the service represented by
    * \texttt{es}. The method returns the generated value and can store an
    * end-contact type indicator in the protected field {@link #ecTypeRet} if
    * the default value of 0 is not appropriate.
    *
    * By default, a {@link MinValueGenerator} is used. For each end-contact type
    * $c$ with an associated value generator, a contact time $C_c$ is generated.
    * The scheduled contact time is $C_{c^*}=\min_c\{C_c\}$, and the end-contact
    * type is $c^*$.
    *
    * @param es
    *           the end-service event.
    * @return the generated contact time.
    */
   protected double getContactTime (EndServiceEvent es)
   {
      final Contact contact = es.getContact ();
      final double stime = cgens.nextDouble (contact);
      if (Double.isNaN (stime)) {
         ecTypeRet = 0;
         return Double.POSITIVE_INFINITY;
      } else
         ecTypeRet = cgens.getLastVType ();
      return stime;
   }

   /**
    * The end-service type associated with the after-contact time returned by
    * {@link #getAfterContactTime(EndServiceEvent)}.
    */
   protected int esTypeRet = 0;

   /**
    * Generates and returns the after-contact time for the service represented
    * by \texttt{es}. The method returns the generated value and can store an
    * end-service type indicator in the protected field {@link #esTypeRet} if
    * the default value of 0 is not appropriate.
    *
    * By default, a {@link MinValueGenerator} is used. For each end-service type
    * $c$ with an associated value generator, an after-contact time $C_c$ is
    * generated. The scheduled after-contact time is $C_{c^*}=\min_c\{C_c\}$,
    * and the end-service type is $c^*$.
    *
    * @param es
    *           the end-service event.
    * @return the generated after-contact time.
    */
   protected double getAfterContactTime (EndServiceEvent es)
   {
      final Contact contact = es.getContact ();
      final double stime = acgens.nextDouble (contact);
      if (Double.isNaN (stime)) {
         esTypeRet = 0;
         return Double.POSITIVE_INFINITY;
      } else
         esTypeRet = acgens.getLastVType ();
      return stime;
   }

   /**
    * Aborts the communication with a contact identified by the end-service
    * event \texttt{es}, overriding the event's end-contact type with
    * \texttt{ecType}. Returns \texttt{true} if the operation was successful, or
    * \texttt{false} otherwise. Note that the after-contact time is generated
    * and after-contact work is performed. One must call {@link #endService(EndServiceEvent,int)}
    * after this method to completely abort the service.
    *
    * @param es
    *           the end-service event representing the service to be aborted.
    * @param ecType
    *           the type of communication termination.
    * @return the success indicator of the operation.
    */
   public boolean endContact (EndServiceEvent es, int ecType)
   {
      if (es.getAgentGroup () != this)
         return false;
      if (es.contactDone)
         return false;
      es.cancel ();
      completeContact (ecType, es, true);
      return true;
   }

   /**
    * Aborts the service of a contact identified by the end-service event
    * \texttt{es}, overriding the event's end-service type with \texttt{esType}.
    * Returns \texttt{true} if the operation was successful, or \texttt{false}
    * otherwise. For this method to return \texttt{true}, the communication
    * between the agent and the contactor must have ended. One can use
    * {@link #endContact(EndServiceEvent,int)} to abort the communication.
    *
    * @param es
    *           the end-service event representing the after-contact work to be
    *           aborted.
    * @param esType
    *           the type of service termination.
    * @return the success indicator of the operation.
    */
   public boolean endService (EndServiceEvent es, int esType)
   {
      if (es.getAgentGroup () != this)
         return false;
      if (!es.contactDone)
         return false;
      if (es.afterContactDone)
         return false;
      es.cancel ();
      completeService (esType, es, true);
      return true;
   }

   /**
    * Returns the total number of agents in the agent group. It is possible that
    * only a fraction of these agents can serve contacts.
    *
    * @return the total number of agents in the group.
    */
   public int getNumAgents ()
   {
assert numAgents >= 0 : "Negative number of agents"
      ;
      return numAgents;
   }

   /**
    * Changes the number of agents of this group to \texttt{n}. The number of
    * free agents is computed by multiplying \texttt{n} by the efficiency
    * factor, rounding the result to the nearest integer, and subtracting the
    * number of busy members of the group.
    *
    * @param n
    *           the total number of agents.
    * @exception IllegalArgumentException
    *               if the given number of agents is negative.
    */
   public void setNumAgents (int n)
   {
      if (n < 0)
         throw new IllegalArgumentException (
            "Number of agents cannot be negative");
      final int oldN = numAgents;
      final int oldNFree = numFreeAgents;
      numAgents = n;
      numFreeAgents = (int) Math.round (n * efficiency) - numBusyAgents;
      if (numAgents == oldN && numFreeAgents == oldNFree)
         return ;
      if (numFreeAgents < 0) {
         numGhostAgents = -numFreeAgents;
         numFreeAgents = 0;
      } else
         numGhostAgents = 0;
      notifyChange();
   }

   /**
    * Returns the array containing the number of agents for each period. This
    * method cannot be used unless the agent group is constructed with a
    * period-change event.
    *
    * @return the number of agents for each period.
    * @exception IllegalStateException
    *               if the per-period numbers of agents are not available.
    */
   public int[] getAllNumAgents ()
   {
      if (allNumAgents == null)
         throw new IllegalStateException (
            "Per-period numbers of agents not available");
      return allNumAgents.clone ();
   }

   /**
    * Returns the number of agents in period \texttt{p}. This method cannot be
    * used unless the agent group is constructed with a period-change event.
    *
    * @param p
    *           the period index.
    * @return the number of agents in the period.
    * @exception IllegalStateException
    *               if the per-period numbers of agents are not available.
    * @exception ArrayIndexOutOfBoundsException
    *               if the period index is negative or greater than or equal to
    *               the number of periods.
    */
   public int getNumAgents (int p)
   {
      if (allNumAgents == null)
         throw new IllegalStateException (
            "Per-period numbers of agents not available");
      return allNumAgents[p];
   }

   /**
    * Sets the number of agents in period \texttt{p} to \texttt{n}. This method
    * cannot be used unless the agent group is constructed with a period-change
    * event.
    *
    * @param p
    *           the period index.
    * @param n
    *           the new number of agents.
    * @exception IllegalStateException
    *               if the per-period numbers of agents are not available.
    * @exception ArrayIndexOutOfBoundsException
    *               if the period index is negative or greater than or equal to
    *               the number of periods.
    */
   public void setNumAgents (int p, int n)
   {
      if (allNumAgents == null)
         throw new IllegalStateException (
            "Per-period numbers of agents not available");
      allNumAgents[p] = n;
   }

   /**
    * Sets the vector giving the number of agent
    * for each period to \texttt{allNumAgents}.
    * @param allNumAgents the new vector of agents.
    */
   public void setNumAgents (int[] allNumAgents)
   {
      if (this.allNumAgents == null)
         throw new IllegalStateException (
            "Per-period numbers of agents not available");
      if (allNumAgents == null)
         throw new NullPointerException
         ("allNumAgents must not be null");
      if (allNumAgents.length != this.allNumAgents.length)
         throw new IllegalArgumentException
         ("The given array must have length " + this.allNumAgents.length + ", but it has length " + allNumAgents.length);
      System.arraycopy (allNumAgents, 0, this.allNumAgents, 0, allNumAgents.length);
   }

   /**
    * Returns $\Ng[i](t)$, the number of agents that should disappear
    * immediately after they have finished serving a contact. Such ghost agents
    * appear when the total number of agents is set to be smaller than the
    * number of busy agents.
    *
    * @return the number of ghost agents.
    */
   public int getNumGhostAgents ()
   {
      return numGhostAgents;
   }

   /**
    * Returns $\Ni[i](t)$, the number of idle agents in this agent group. Since
    * only a fraction of these idle agents can serve contacts, the returned
    * value is greater than or equal to {@link #getNumFreeAgents()}. If
    * {@link #getEfficiency()} returns 1, this returns the same value as
    * {@link #getNumFreeAgents()}.
    *
    * @return the number of idle agents.
    */
   public int getNumIdleAgents ()
   {
      final int ni = getNumAgents () - getNumBusyAgents ()
                     + getNumGhostAgents ();
      if (ni < 0)
         return 0;
      else
         return ni;
   }

   /**
    * Returns $\Nf[i](t)$, the total number of agents in the agent group which
    * are available to process contacts. This number must always be smaller than
    * or equal to the total number of agents.
    *
    * @return the number of free agents in the group.
    */
   public int getNumFreeAgents ()
   {
assert numFreeAgents >= 0 : "Negative number of free agents"
      ;
      return numFreeAgents;
   }

   /**
    * Returns $\Nb[i](t)$, the number of busy agents in the group. At any time
    * during the simulation, the value returned by this method should be smaller
    * than or equal to the sum of {@link #getNumAgents()} and
    * {@link #getNumGhostAgents()}.
    *
    * @return the number of busy agents.
    */
   public int getNumBusyAgents ()
   {
assert numBusyAgents >= 0 : "Negative number of busy agents"
      ;
      return numBusyAgents;
   }

   /**
    * Returns the number of busy agents serving contacts
    * of type $k$.
    * @param k the contact type index.
    * @return the number of busy agents serving contacts of type \texttt{k}.
    */
   public int getNumBusyAgents (int k)
   {
      if (k < 0 || k >= numBusyAgentsK.length)
         return 0;
      assert numBusyAgentsK[k] >= 0;
      return numBusyAgentsK[k];
   }

   /**
    * Returns the identifier associated with this agent group. This identifier,
    * which defaults to \texttt{-1}, can be used as an index in routers.
    *
    * @return the identifier associated with this agent group.
    */
   public int getId ()
   {
      return groupId;
   }

   /**
    * Sets the identifier of this agent group to \texttt{id}. Once this
    * identifier is set to a positive or 0 value, it cannot be changed anymore.
    * This method is automatically called by the router when an agent group is
    * connected. If one tries to attach the same group to different routers, the
    * group must have the same index for each of them. For this reason, if one
    * tries to change the identifier, an {@link IllegalStateException} is
    * thrown.
    *
    * @param id
    *           the new identifier associated with the agent group.
    * @exception IllegalStateException
    *               if the identifier was already set.
    */
   public void setId (int id)
   {
      if (groupId >= 0 && id != groupId)
         throw new IllegalStateException ("Identifier already set");
      groupId = id;
   }

   /**
    * Adds the agent-group listener \texttt{listener} to this object.
    *
    * @param listener
    *           the agent-group listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addAgentGroupListener (AgentGroupListener listener)
   {
      if (listener == null)
         throw new NullPointerException ("The added listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains (listener))
         listeners.add (listener);
   }

   /**
    * Removes the agent-group listener \texttt{listener} from this object.
    *
    * @param listener
    *           the agent-group listener being removed.
    */
   public void removeAgentGroupListener (AgentGroupListener listener)
   {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.remove (listener);
   }

   /**
    * Removes all the agent-group listeners registered with this agent group.
    */
   public void clearAgentGroupListeners ()
   {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.clear ();
   }

   /**
    * Returns an unmodifiable list containing all the agent-group listeners
    * registered with this agent group.
    *
    * @return the list of all registered agent-group listeners.
    */
   public List<AgentGroupListener> getAgentGroupListeners ()
   {
      return umListeners;
   }

   /**
    * Notifies every registered listener that
    * this agent group has been
    * initialized.
    */
   protected void notifyInit()
   {
      final int nl = listeners.size ();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).init (this);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that the
    * number of agents of this group has changed.
    */
   protected void notifyChange()
   {
      final int nl = listeners.size ();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).agentGroupChange (this);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that
    * a service, represented by \texttt{es},
    * was started by this agent group.
    * @param es the end-service event representing the service.
    */
   protected void notifyBeginService (EndServiceEvent es)
   {
      assert getNumBusyAgents () + getNumIdleAgents () == getNumAgents ()
      + getNumGhostAgents ();
      assert getNumFreeAgents () <= getNumAgents ();

      final int nl = listeners.size ();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).beginService (es);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that
    * the communication part of the service
    * represented by \texttt{es} has ended.
    * @param es the end-service event.
    * @param aborted determines if the service was aborted or terminated
    * normally.
    */
   protected void notifyEndContact (EndServiceEvent es, boolean aborted)
   {
      es.getContact ().endContact (es);
      final int nl = listeners.size ();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).endContact (es);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that the service
    * represented by \texttt{es} is finished.
    * @param es the end-service vent representing the ended service.
    * @param aborted determines if the after-contact work was
    * aborted or terminated normally.
    */
   protected void notifyEndService (EndServiceEvent es, boolean aborted)
   {
      assert getNumBusyAgents () + getNumIdleAgents () == getNumAgents ()
      + getNumGhostAgents ();
      assert getNumFreeAgents () <= getNumAgents ();
      es.getContact ().endService (es);
      final int nl = listeners.size ();
      if (nl == 0)
         return ;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).endService (es);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Returns the map containing the attributes for this
    * agent group.  Attributes can be used to add user-defined information
    * to agent group objects at runtime, without creating
    * a subclass.  However, for maximal efficiency,
    * it is recommended to create a subclass of \texttt{Agent\-Group}
    * instead of using attributes.
    @return the map containing the attributes for this object.
    */
   public Map<Object, Object> getAttributes()
   {
      if (attributes == null)
         attributes = new HashMap<Object, Object>();
      return attributes;
   }

   private final void initNumAgents (int n)
   {
      if (n < 0)
         throw new IllegalArgumentException (
            "Number of agents must not be negative");
      numAgents = n;
      numFreeAgents = (int) Math.round (n * efficiency);
      numBusyAgents = 0;
      Arrays.fill (numBusyAgentsK, 0);
      numGhostAgents = 0;
      cgens.setValueGenerator (0, new ContactTimeGenerator (this));
      acgens.setValueGenerator (0, new AfterContactTimeGenerator (this));
   }

   final void completeContact (int ecType, EndServiceEvent es, boolean aborted)
   {
      if (es.contactDone)
         throw new IllegalStateException (
            "The communication part of the service is already completed");
      es.contactDone = true;
      es.eecType = ecType;
      final double simTime = es.simulator().time ();
      es.econtactTime = es.beginServiceTime < simTime ? simTime - es.beginServiceTime : 0;

      double stime;
      if (es.afterContactTimeSet)
         stime = es.afterContactTime;
      else {
         esTypeRet = 0;
         stime = getAfterContactTime (es);
         if (stime < 0)
            stime = 0;
         es.afterContactTime = stime;
         es.esType = esTypeRet;
      }
      if (!Double.isInfinite (stime) && stime > 0)
         es.schedule (stime);
      notifyEndContact (es, aborted);
      if (stime == 0)
         completeService (es.esType, es, false);
   }

   final void completeService (int esType, EndServiceEvent es, boolean aborted)
   {
      if (es.afterContactDone)
         throw new IllegalStateException ("The service is already completed");
      es.afterContactDone = true;
      es.eesType = esType;
      final double simTime = es.simulator().time ();
      final double beginAfterContact = es.beginServiceTime + es.econtactTime;
      es.eafterContactTime = beginAfterContact < simTime ? simTime - beginAfterContact : 0;
      endServiceUpdateStatus (es);
      assert getNumAgents() + getNumGhostAgents() == getNumBusyAgents() + getNumIdleAgents();
      notifyEndService (es, aborted);
   }

   void endServiceUpdateStatus (EndServiceEvent es)
   {
      if (esevSet != null)
         esevSet.remove (es);
      --numBusyAgents;
      final int k = es.getContact ().getTypeId ();
      if (k >= 0 && k < numBusyAgentsK.length)
         --numBusyAgentsK[k];
      if (numGhostAgents > 0) {
         // If the agent was a ghost, it disappears without
         // receiving a new contact to be served.
         --numGhostAgents;
         es.ghostAgent = true;
      } else {
         // Otherwise, it needs a new contact
         ++numFreeAgents;
         es.ghostAgent = false;
      }
   }

   @Override
   public void changePeriod (PeriodChangeEvent pce1)
   {
      if (pce1 != this.pce)
         return ;
      final int currentPeriod = pce1.getCurrentPeriod ();
      setNumAgents (allNumAgents[currentPeriod]);
   }

   @Override
   public void stop (PeriodChangeEvent pce1)
{}

   @Override
   public String toString ()
   {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      if (getName ().length () > 0)
         sb.append ("name: ").append (getName ()).append (", ");
      if (groupId != -1)
         sb.append ("id: ").append (groupId).append (", ");
      sb.append ("number of agents: ").append (getNumAgents ());
      final int nf = getNumFreeAgents ();
      sb.append (", number of free agents: ").append (nf);
      sb.append (", number of busy agents: ").append (getNumBusyAgents ());
      final int ni = getNumIdleAgents ();
      if (nf != ni)
         sb.append (", number of idle agents: ").append (ni);
      final int ng = getNumGhostAgents ();
      if (ng > 0)
         sb.append (", number of ghost agents: ").append (ng);
      sb.append (']');
      return sb.toString ();
   }
}
