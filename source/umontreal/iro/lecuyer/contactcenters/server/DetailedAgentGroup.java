package umontreal.iro.lecuyer.contactcenters.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.ssj.simevents.Simulator;

/**
 * Extends the {@link AgentGroup} class for a detailed agent group, where
 * individual agents can be differentiated. When serving a contact, a specific
 * agent, represented by an instance of the class {@link Agent}, must be chosen
 * automatically using the longest idle policy, or manually. At any time during
 * the simulation, agents can be added to or removed from the group. Agents can
 * also be made available or unavailable to process new contacts.
 */
public class DetailedAgentGroup extends AgentGroup
{
   private final Logger logger = Logger
                                 .getLogger ("umontreal.iro.lecuyer.contactcenters.server");
   private Simulator sim;
   // Contains idle agents, available or not for serving contacts
   List<Agent> idleAgents = new ArrayList<Agent> ();
   // Contains busy AND ghost agents
   List<Agent> busyAgents = new ArrayList<Agent> ();
   // Contains ghost agents only, i.e., this is a subset of the busy agents
   List<Agent> ghostAgents = new ArrayList<Agent> ();
   private int changeLock = 0;
   boolean idleEqualFree = true;
   private boolean addingAgent = false;
   private boolean removingAgent = false;
   AgentGroupParams par = null;
   /**
    * Constructs a new agent group with \texttt{n} available agents.
    *
    * @param n
    *           the number of agents in the group.
    */
   public DetailedAgentGroup (int n)
   {
      this (Simulator.getDefaultSimulator (), n);

   }

   public DetailedAgentGroup (Simulator sim, int n)
   {
      super (n);
      if (sim == null)
         throw new NullPointerException (
            "The attached simulator must not be null");
      this.sim = sim;
      ++changeLock;
      try {
         setNumAgents (n);
      } finally {
         --changeLock;
      }
   }

   public void setAgentGroupParam(AgentGroupParams par)
   {
      this.par = par;
   }

   public AgentGroupParams getAgentGroupParam()
   {
      return par;
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
    *               if there is not a number of agents for each period.
    */
   public DetailedAgentGroup (PeriodChangeEvent pce, int[] ns)
   {
      super (pce, ns);
      sim = pce.simulator ();
      ++changeLock;
      try {
         setNumAgents (ns[0]);
      } finally {
         --changeLock;
      }
   }

   /**
    * Returns a reference to the simulator used to obtain simulation times at
    * which agents are added or become free, for computing login and idle times
    * of agents.
    *
    * @return the attached simulator.
    */
   public Simulator simulator ()
   {
      return sim;
   }

   /**
    * Sets the attached simulator of this agent group to \texttt{sim}.
    *
    * @param sim
    *           the new attached simulator.
    */
   public void setSimulator (Simulator sim)
   {
      if (sim == null)
         throw new NullPointerException ();
      this.sim = sim;
   }

   @Override
   public int getNumAgents ()
   {
      return idleAgents.size () + busyAgents.size () - ghostAgents.size ();
   }

   /**
    * Sets the number of agents in the agent group to \texttt{n}. When the
    * number of agents is increased, the {@link #createAgent()} method is used
    * to create the required new agents. When removing agents, the method uses
    * the busyness status (busy agents are removed only if there is no more idle
    * agent), and the login time (the agent with the longest login time is
    * chosen) to decide which agent to remove. The methods
    * {@link #addAgent(Agent)} and {@link #removeAgent(Agent)} are used to add
    * and remove the agents.
    *
    * @param n
    *           the new number of agents.
    * @exception IllegalArgumentException
    *               if the number of agents is negative.
    */
   @Override
   public void setNumAgents (int n)
   {
      if (n < 0)
         throw new IllegalArgumentException (
            "Negative number of agents not allowed");
      final int oldN = getNumAgents ();
      ++changeLock;
      try {
         if (n > oldN)
            // New agents are added
            for (int i = 0; i < n - oldN; i++) {
               Agent agent;
               if (ghostAgents.isEmpty ())
                  agent = createAgent ();
               else
                  agent = ghostAgents.get (0);
               addAgent (agent);
            }
         else if (n < oldN)
            // Agents are removed, but we do not know which agents to remove.
            for (int i = 0; i < oldN - n; i++) {
               Agent bestAgent = null;
               double bestTime = Double.POSITIVE_INFINITY;
               for (final Agent agent : idleAgents)
                  if (agent.firstLoginTime < bestTime) {
                     bestTime = agent.firstLoginTime;
                     bestAgent = agent;
                  }
               if (bestAgent == null)
               for (final Agent agent : busyAgents) {
                     if (agent.isGhost ())
                        continue;
                     if (agent.firstLoginTime < bestTime) {
                        bestTime = agent.firstLoginTime;
                        bestAgent = agent;
                     }
                  }
               removeAgent (bestAgent);
            }
      } finally {
         --changeLock;
      }
      if (changeLock == 0 && n != oldN)
         notifyChange ();
   }

   /**
    * Adds the agent \texttt{agent} to the agent group. When an agent is a
    * ghost, it can be added back to its previous group, but it cannot be added
    * to another group until he has terminated his in-progress service. When an
    * agent is not in any group and not serving any contact, he can be added to
    * any group.
    *
    * @param agent
    *           the agent being added.
    * @exception NullPointerException
    *               if \texttt{agent} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the agent is already in a group.
    */
   public void addAgent (Agent agent)
   {

      if (agent.group != null)
         throw new IllegalArgumentException ("Agent already in a group");
      addingAgent = true;
      try {
         if (agent.es != null) {
            // A ghost agent is added back.
            if (agent.es.getAgentGroup () != this)
               throw new IllegalArgumentException (
                  "Ghost agent in the wrong group");
            ghostAgents.remove (agent);
            agent.ghost = false;
            --numGhostAgents;
         } else {
            agent.idleSimTime = sim.time ();
            idleAgents.add (agent);
            if (agent.isAvailable ())
               ++numFreeAgents;
            else
               idleEqualFree = false;
         }
         agent.group = this;
         final double simTime = sim.time ();
         if (Double.isNaN (agent.firstLoginTime)
               || agent.firstLoginTime > simTime)
            agent.firstLoginTime = simTime;
         agent.lastLoginTime = simTime;
         numAgents = getNumAgents ();
         if (changeLock == 0)
            notifyChange ();
         agent.notifyAdded (this);
      } finally {
         addingAgent = false;
      }
   }

   /**
    * Removes the agent \texttt{agent} from this agent group. If the agent is
    * serving a contact, it becomes a ghost until the contact is served.
    *
    * @param agent
    *           the agent being removed.
    * @exception NullPointerException
    *               if \texttt{agent} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the removed agent is not in this group.
    */
   public void removeAgent (Agent agent)
   {
      if (agent.group == null)
         throw new IllegalArgumentException (
            "The agent to remove is not in any agent group");
      removingAgent = true;
      try {
         if (agent.group != this)
            throw new IllegalArgumentException (
               "The agent to remove is in the wrong group");
         agent.group = null;
         if (agent.es == null) {
            // Removes the free agent
            assert idleAgents.contains (agent);
            assert !busyAgents.contains (agent);
            idleAgents.remove (agent);
            if (agent.isAvailable ())
               --numFreeAgents;
         } else {
            // Add the agent to the list of ghost agents.
            assert !idleAgents.contains (agent);
            assert busyAgents.contains (agent);
            ghostAgents.add (agent);
            agent.ghost = true;
            ++numGhostAgents;
         }
         if (changeLock == 0)
            notifyChange ();
         agent.notifyRemoved (this);
      } finally {
         removingAgent = false;
      }
   }

   /**
    * Determines if this agent group is currently
    * adding an agent using the {@link #addAgent(Agent)}
    * method.
    * This method can be used by {@link AgentGroupListener#agentGroupChange(AgentGroup)}
    * to determine the origin of a change in an observed agent group.
    * If an agent is added explicitly using {@link #addAgent(Agent)},
    * this method returns \texttt{true}.
    * Otherwise, the change originates from a call
    * to {@link #setNumAgents(int)}.
    * @return the result of the test.
    */
   public boolean isAddingAgent()
   {
      return addingAgent;
   }

   /**
    * Determines if an agent is currently being removed
    * using {@link #removeAgent(Agent)}.
    * This method can be used by an agent-group listener
    * as described in {@link #isAddingAgent()}.
    * @return the result of the test.
    */
   public boolean isRemovingAgent()
   {
      return removingAgent;
   }

   @Override
   public void init ()
   {
      ++initCount;
      numBusyAgents = 0;
      Arrays.fill (numBusyAgentsK, 0);
      numGhostAgents = 0;
      cgens.init ();
      acgens.init ();
      for (final Agent agent : idleAgents)
         agent.init ();
      // Busy agents become free and ghost agents disappear
      for (final Agent agent : busyAgents) {
         if (agent.isGhost ())
            continue;
         idleAgents.add (agent);
         agent.init ();
         if (!agent.isAvailable ())
            idleEqualFree = false;
      }
      busyAgents.clear ();
      for (final Agent agent : ghostAgents)
         agent.init ();
      ghostAgents.clear ();
      numFreeAgents = getNumFreeAgents ();

      // When the non-stationary convenience is used,
      // resets the number of agents
      if (allNumAgents.length > 0) {
         ++changeLock;
         try {
            setNumAgents (allNumAgents[0]);
         } finally {
            --changeLock;
         }
      }

      if (esevSet != null)
         esevSet.clear ();
      notifyInit ();
   }

   /**
    * Returns a list containing all the idle agent objects. These idle agents
    * are not necessarily available to process contacts.
    *
    * @return the idle agents.
    */
   public List<Agent> getIdleAgents ()
   {
      return Collections.unmodifiableList (idleAgents);
   }

   /**
    * Returns a list containing all the busy agent objects which are members of
    * this group. This excludes ghost agents since they have been removed from
    * the group.
    *
    * @return the busy agents.
    */
   public List<Agent> getBusyAgents ()
   {
      return Collections.unmodifiableList (busyAgents);
   }

   /**
    * Returns a list containing all the ghost agent objects having been members
    * of this agent group and finishing an in-progress service.
    *
    * @return the ghost agents.
    */
   public List<Agent> getGhostAgents ()
   {
      return Collections.unmodifiableList (ghostAgents);
   }

   /**
    * Returns the number of free agents available to process contacts. These are
    * the free agents for which {@link Agent#isAvailable()} returns
    * \texttt{true}.
    *
    * @return the number of free agents.
    */
   @Override
   public int getNumFreeAgents ()
   {
      if (idleAgents.isEmpty ())
         return 0;
      if (idleEqualFree)
         return idleAgents.size ();
      int n = 0;
      for (final Agent agent : idleAgents)
         if (agent.isAvailable ())
            ++n;
      if (n == idleAgents.size ())
         idleEqualFree = true;
      return n;
   }

   @Override
   public int getNumGhostAgents ()
   {
      return ghostAgents.size ();
   }

   @Override
   public int getNumBusyAgents ()
   {
      return busyAgents.size ();
   }

   /**
    * Returns the current efficiency of this agent group, which is given by the
    * fraction of available agents (free or busy) over the total number of
    * agents. This efficiency can change when the agents are added to or removed
    * from the group, or when the availability status of agents changes.
    *
    * @return the efficiency of the agent group.
    */
   @Override
   public double getEfficiency ()
   {
      int curAvail = getNumFreeAgents ();
      for (final Agent ag : busyAgents)
         if (ag.isAvailable () && !ag.isGhost ())
            ++curAvail;
      return (double) curAvail / getNumAgents ();
   }

   /**
    * Sets the efficiency of the agents in the group. The method computes a
    * target number of available agents by multiplying $N_i(t)$ by \texttt{eff}
    * and rounding the result to the nearest integer. Some agents are then made
    * available or unavailable to meet this target, starting with free agents,
    * then with busy agents. This method is provided for compatibility with the
    * {@link AgentGroup} base class. The recommended way to change the
    * efficiency is to change the availability of each individual agent by using
    * {@link Agent#setAvailable(boolean)}. This permits the implementation of
    * more complex and realistic models of agents' availability.
    *
    * @param eff
    *           the new agent group's efficiency.
    * @exception IllegalArgumentException
    *               if \texttt{eff} is smaller than 0 or greater than 1.
    */
   @Override
   public void setEfficiency (double eff)
   {
      if (eff < 0 || eff > 1)
         throw new IllegalArgumentException ("Efficiency must be in [0,1]");
      efficiency = eff;
      // Compute the target number of available agents
      final int nAvail = (int) Math.round (getNumAgents () * eff);

      // Computes the current number of available agents.
      int curAvail = getNumFreeAgents ();
      for (final Agent ag : busyAgents)
         if (ag.isAvailable () && !ag.isGhost ())
            curAvail++;
      int diff = nAvail - curAvail;

      // Modifies availability status of agents to
      // meet the target number of available agents.
      for (final Agent agent : idleAgents)
         if (diff > 0 && agent.isAvailable ()) {
            // Some agents have to be made unavailable
            agent.setAvailable (false);
            diff--;
         } else if (diff < 0 && !agent.isAvailable ()) {
            agent.setAvailable (true);
            diff++;
         }
      for (final Agent agent : busyAgents) {
         if (agent.isGhost ())
            continue;
         if (diff > 0 && agent.isAvailable ()) {
            // Some agents have to be made unavailable
            agent.setAvailable (false);
            diff--;
         } else if (diff < 0 && !agent.isAvailable ()) {
            agent.setAvailable (true);
            diff++;
         }
      }
      numFreeAgents = getNumFreeAgents ();
   }

   /**
    * Returns the idle agent with the longest idle time in this agent group. If
    * all agents are busy or unavailable to process new contacts, this returns
    * \texttt{null}.
    *
    * @return the idle agent with the longest idle time.
    */
   public Agent getLongestIdleAgent ()
   {
      // Agent bestAgent = null;
      // double bestTime = Double.POSITIVE_INFINITY;
      // for (Agent agent : idleAgents) {
      // if (agent.idleSimTime < bestTime && agent.isAvailable()) {
      // bestAgent = agent;
      // bestTime = agent.idleSimTime;
      // }
      // }
      // return bestAgent;
      // When an agent becomes idle, it is added at the end of
      // idleAgents. The first agent in this list has then
      // the longest idle time.
      if (idleAgents.isEmpty ())
         return null;
      assert idleNoEndServiceEvents ();
      // If possible, avoid creating an iterator.
      final Agent agent = idleAgents.get (0);
      if (agent.isAvailable ())
         return agent;
      // The first idle agent is unavailable, so visit all
      // idle agents and return the first available, if any.
      // We must use an iterator, because we may change
      // the data structure of idleAgents in the future.
      for (final Agent agent2 : idleAgents)
         if (agent2.isAvailable ())
            return agent2;
      // There is no available agent.
      return null;
   }

   private boolean idleNoEndServiceEvents ()
   {
      boolean correct = true;
      for (final Agent agentTest : idleAgents)
         if (agentTest.getEndServiceEvent () != null) {
            logger.severe ("An idle agent has an end-service event!");
            correct = false;
         }
      return correct;
   }

   /**
    * Begins the service of the contact \texttt{contact} by the agent with the
    * longest idle time in this group. After the agent is selected, the
    * {@link #serve(Contact,Agent)} method is called to begin the service.
    *
    * @param contact
    *           the contact being served.
    * @return the end of service event being created.
    */
   @Override
   public EndServiceEventDetailed serve (Contact contact)
   {
      final Agent bestAgent = getLongestIdleAgent ();
      if (bestAgent == null)
         throw new IllegalStateException ("No free agent");
      if (!bestAgent.isAvailable ())
         throw new IllegalStateException ("Selected idle agent not available");
      return serve (contact, bestAgent);
   }

   @Override
   public EndServiceEventDetailed serve (Contact contact, double contactTime,
                                         int ecType)
   {
      final Agent bestAgent = getLongestIdleAgent ();
      if (bestAgent == null)
         throw new IllegalStateException ("No free agent");
      return serve (contact, bestAgent, contactTime, ecType);
   }

   @Override
   public EndServiceEventDetailed serve (Contact contact, double contactTime,
                                         int ecType, double afterContactTime, int esType)
   {
      final Agent bestAgent = getLongestIdleAgent ();
      if (bestAgent == null)
         throw new IllegalStateException ("No free agent");
      return serve (contact, bestAgent, contactTime, ecType, afterContactTime,
                    esType);
   }

   /**
    * Begins the service of the contact \texttt{contact} by the agent
    * \texttt{agent}. Returns the constructed end-service event. Communication
    * times are generated using
    * {@link #getAgentContactTime(Agent, Contact)} if a specific service time
    * distribution has been defined for this agent and contact type, otherwise
    * {@link #getContactTime(EndServiceEvent)},  and
    * after-contact times are obtained using
    * {@link #getAfterContactTime(EndServiceEvent)}.
    *
    * @param contact
    *           the contact being served.
    * @param agent
    *           the agent serving the contact.
    * @return the constructed end-service event.
    * @exception NullPointerException
    *               if an argument is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given agent is in the wrong agent group.
    * @exception IllegalStateException
    *               if the agent is not available or already serving a contact.
    */
   public EndServiceEventDetailed serve (Contact contact, Agent agent)
   {
      if (agent.group == null)
         throw new IllegalArgumentException (
            "The given agent is not in any agent group");
      if (agent.group != this)
         throw new IllegalArgumentException (
            "The given agent is not part of this agent group");
      if (!agent.isAvailable () || agent.es != null)
         throw new IllegalStateException ("Agent is not free");

      final EndServiceEventDetailed es = new EndServiceEventDetailed (contact,
                                         agent, contact.simulator ().time ());
      agent.es = es;
      ecTypeRet = 0;

      double stime = getAgentContactTime(agent, contact);
      if (stime < 0) {
         stime = getContactTime (es);
         if (stime < 0)
            stime = 0;
      }
      es.contactTime = stime;

      es.ecType = ecTypeRet;
      es.afterContactTimeSet = false;
      return internalServe (es);
   }

   /**
    * This is similar to {@link #serve(Contact,Agent)}, except that the
    * specified contact time and end-contact type are used instead of generated
    * ones.
    *
    * Note that the service time returned by {@link #getAgentContactTime(Agent, Contact)}
    * will be used instead of the parameter \texttt{contactTime} if a specific
    * service time distribution has been defined for this agent and contact type.
    *
    * The after-contact time is generated as in {@link #serve(Contact)}.
    * The main purpose of this method is for recreating an end-service event
    * based on saved state information.
    *
    * @param contact
    *           the contact being served.
    * @param agent
    *           the agent serving the contact.
    * @param contactTime
    *           the communication time of the contact with the agent.
    * @param ecType
    *           the end-contact type.
    * @return the end-service event representing the service.
    * @exception IllegalStateException
    *               if no free agent is available.
    */
   public EndServiceEventDetailed serve (Contact contact, Agent agent,
                                         double contactTime, int ecType)
   {
      if (agent.group != this)
         throw new IllegalArgumentException (
            "The given agent is not part of this agent group");
      if (!agent.isAvailable () || agent.es != null)
         throw new IllegalStateException ("Agent is not free");

      final EndServiceEventDetailed es = new EndServiceEventDetailed (contact,
                                         agent, contact.simulator ().time ());
      agent.es = es;

      double s = getAgentContactTime(agent, contact);
      if (s >= 0)
         es.contactTime = s;
      else
         es.contactTime = contactTime;

      es.ecType = ecType;
      es.afterContactTimeSet = false;
      return internalServe (es);
   }

   /**
    * This is similar to {@link #serve(Contact,Agent)} except that the contact
    * and after-contact times are specified explicitly.
    *
    * Note that the service time returned by {@link #getAgentContactTime(Agent, Contact)}
    * will be used instead of the parameter \texttt{contactTime} if a specific
    * service time distribution has been defined for this agent and contact type.
    *
    * The main purpose of this
    * method is for recreating an end-service event based on saved state
    * information.
    *
    * @param contact
    *           the contact being served.
    * @param agent
    *           the agent serving the contact.
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
   public EndServiceEventDetailed serve (Contact contact, Agent agent,
                                         double contactTime, int ecType, double afterContactTime, int esType)
   {
      if (agent.group != this)
         throw new IllegalArgumentException (
            "The given agent is not part of this agent group");
      if (!agent.isAvailable () || agent.es != null)
         throw new IllegalStateException ("Agent is not free");

      final EndServiceEventDetailed es = new EndServiceEventDetailed (contact,
                                         agent, contact.simulator ().time ());
      agent.es = es;
      es.contactTime = contactTime;

      double s = getAgentContactTime(agent, contact);
      if (s >= 0)
         es.contactTime = s;
      else
         es.contactTime = contactTime;

      es.ecType = ecType;
      es.afterContactTime = afterContactTime;
      es.esType = esType;
      es.afterContactTimeSet = true;
      return internalServe (es);
   }

   @Override
   public EndServiceEvent serve (EndServiceEvent oldEndServiceEvent)
   {
      if (!(oldEndServiceEvent instanceof EndServiceEventDetailed))
         return super.serve (oldEndServiceEvent);
      final EndServiceEventDetailed es = (EndServiceEventDetailed) oldEndServiceEvent;
      if (oldEndServiceEvent.contactDone ())
         return serve (es.getContact (), es.getAgent (), es
                       .getEffectiveContactTime (), es.getEffectiveEndContactType (),
                       es.getScheduledAfterContactTime (), es
                       .getScheduledEndServiceType ());
      else
         return serve (es.getContact (), es.getAgent (), es
                       .getScheduledContactTime (), es.getScheduledEndContactType ());
   }

   private EndServiceEventDetailed internalServe (EndServiceEventDetailed es)
   {
      beginServiceUpdateStatus (es);
      final double stime = es.contactTime;
      if (!Double.isInfinite (stime) && stime > 0)
         es.schedule (stime);
      es.getContact ().beginService (es);
      notifyBeginService (es);
      if (stime == 0)
         completeContact (es.ecType, es, false);
      assert getNumAgents () + getNumGhostAgents () == getNumBusyAgents ()
      + getNumIdleAgents ();
      return es;
   }

   @Override
   void beginServiceUpdateStatus (EndServiceEvent es)
   {
      super.beginServiceUpdateStatus (es);
      final Agent agent = ((EndServiceEventDetailed) es).getAgent ();
      idleAgents.remove (agent);
      busyAgents.add (agent);
   }

   @Override
   public DetailedAgentGroupState save ()
   {
      return new DetailedAgentGroupState (this);
   }

   @Override
   public void restore (AgentGroupState state)
   {
      ((DetailedAgentGroupState) state).restore (this);
   }

   /**
    * By default, this method calls {@link Agent#getContactTime(Contact)}. If
    * this returns {@link Double#NaN}, the method of the superclass is called.
    *
    * @param es
    *           the end-service event.
    * @return the generated contact time.
    */
   @Override
   protected double getContactTime (EndServiceEvent es)
   {
      final Contact contact = es.getContact ();
      final Agent agent = ((EndServiceEventDetailed) es).getAgent ();
      final double stime = agent.getContactTime (contact);
      if (Double.isNaN (stime))
         return super.getContactTime (es);
      else
         ecTypeRet = agent.ecType;
      return stime;
   }

   /**
    * Generates a service time of this contact by this agent,
    * if a specific service time distribution has been
    * defined for this agent and contact type. Otherwise, this returns -1.
    *
    * @param contact
    *           the contact being served.
    * @param agent
    *           the agent serving the contact.
    *
    * @return the service time or -1 if no specific distribution has been defined
    * for this agent and contact type.
    */

   protected double getAgentContactTime(Agent agent, Contact contact)
   {
      double stime = -1;
      if (agent.getMapServiceTime() != null && agent.getMapServiceTime().containsKey(contact.getTypeId()))
         stime = agent.getMapServiceTime().get(contact.getTypeId()).nextDouble();
      return stime;
   }

   /**
    * By default, this method calls {@link Agent#getAfterContactTime(Contact)}.
    * If the called method returns {@link Double#NaN}, the method calls the
    * equivalent method of the superclass.
    *
    * @param es
    *           the end-service event.
    * @return the generated after-contact time.
    */
   @Override
   protected double getAfterContactTime (EndServiceEvent es)
   {
      final Contact contact = es.getContact ();
      final Agent agent = ((EndServiceEventDetailed) es).getAgent ();
      final double stime = agent.getAfterContactTime (contact);
      if (Double.isNaN (stime))
         return super.getAfterContactTime (es);
      else
         esTypeRet = agent.esType;
      return stime;
   }

   @Override
   public boolean endContact (EndServiceEvent es, int ecType)
   {
      if (!(es instanceof EndServiceEventDetailed))
         return false;
      return super.endContact (es, ecType);
   }

   @Override
   public boolean endService (EndServiceEvent es, int esType)
   {
      if (!(es instanceof EndServiceEventDetailed))
         return false;
      return super.endService (es, esType);
   }

   @Override
   void endServiceUpdateStatus (EndServiceEvent es)
   {
      super.endServiceUpdateStatus (es);
      final Agent agent = ((EndServiceEventDetailed) es).getAgent ();
      if (agent != null) {
         agent.es = null;
         agent.idleSimTime = es.simulator().time ();
         busyAgents.remove (agent);
         if (agent.ghost) {
            ghostAgents.remove (agent);
            agent.ghost = false;
            es.ghostAgent = true;
         } else {
            idleAgents.add (agent);
            es.ghostAgent = false;
            if (!agent.isAvailable ())
               idleEqualFree = false;
         }
      }
   }

   @Override
   protected void notifyBeginService (EndServiceEvent es)
   {
      super.notifyBeginService (es);
      final EndServiceEventDetailed es2 = (EndServiceEventDetailed) es;
      final Agent agent = es2.getAgent ();
      agent.notifyBeginService (es2);
   }

   @Override
   protected void notifyEndContact (EndServiceEvent es, boolean aborted)
   {
      super.notifyEndContact (es, aborted);
      final EndServiceEventDetailed es2 = (EndServiceEventDetailed) es;
      final Agent agent = es2.getAgent ();
      agent.notifyEndContact (es2);
   }

   @Override
   protected void notifyEndService (EndServiceEvent es, boolean aborted)
   {
      super.notifyEndService (es, aborted);
      final EndServiceEventDetailed es2 = (EndServiceEventDetailed) es;
      final Agent agent = es2.getAgent ();
      agent.notifyEndService (es2);
   }

   /**
    * Constructs a new agent object. This method can be overridden to create
    * subclasses of {@link Agent} containing additional information.
    *
    * @return the constructed agent object.
    */
   protected Agent createAgent ()
   {
      return new Agent ();
   }
}
