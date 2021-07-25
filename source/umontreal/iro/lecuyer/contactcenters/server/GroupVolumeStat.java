package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Simulator;

/**
 * Computes statistics for a specific agent group.
 * Using accumulates, this class can compute integrals of
 * $N_i(t)$, $\Ni[i](t)$, $\Nb[i](t)$, $\Ng[i](t)$, and $\Nf[i](t)$, for
 * agent group~$i$ from the last call to {@link #init} to the current
 * simulation time.
 * Optionally, it can also compute the integral for $\Nb[i,k](t)$,
 * the number of busy agents in group~$i$ serving contacts of type~$k$,
 * for $k=0,\ldots,K-1$.
 */
public class GroupVolumeStat implements Cloneable {
   private Accumulate[] statBusyAgents;
   private Accumulate statNumBusyAgents;
   private Accumulate statNumIdleAgents;
   private Accumulate statNumAgents;
   private Accumulate statNumFreeAgents;
   private Accumulate statNumGhostAgents;
   private AgentGroup agentGroup = null;
   private VolumeListener vl = new VolumeListener();

   /**
    * Constructs a new agent-group volume statistical probe
    * observing the agent group \texttt{group} and only
    * computing aggregate statistics.
    * This is equivalent to
    * {@link #GroupVolumeStat(AgentGroup,int) Group\-Volume\-Stat}
    * \texttt{(group, 0)}.
    @param group the observed agent group.
    */
   public GroupVolumeStat (AgentGroup group) {
      this (Simulator.getDefaultSimulator(), group, 0);
   }

   public GroupVolumeStat (Simulator sim, AgentGroup group) {
      this (sim, group, 0);
   }

   /**
    * Constructs a new agent-group volume statistical probe
    * observing the agent group \texttt{group}, and
    * supporting \texttt{numTypes} contact types.
    @param group the observed agent group.
    @param numTypes the number of contact types.
    @exception IllegalArgumentException if \texttt{numTypes} is negative.
    */
   public GroupVolumeStat (AgentGroup group, int numTypes) {
      this (Simulator.getDefaultSimulator(), group, numTypes);
   }

   public GroupVolumeStat (Simulator sim, AgentGroup group, int numTypes) {
      if (numTypes < 0)
         throw new IllegalArgumentException ("numTypes must be positive or 0");
      statBusyAgents = new Accumulate[numTypes];
      for (int k = 0; k < statBusyAgents.length; k++)
         statBusyAgents[k] = new Accumulate
         (sim, "Number of busy agents serving contacts of type " + k);
      statNumBusyAgents = new Accumulate (sim, "Number of busy agents");
      statNumIdleAgents = new Accumulate (sim, "Number of idle agents");
      statNumAgents = new Accumulate (sim, "Number of agents");
      statNumFreeAgents = new Accumulate (sim, "Number of free agents");
      statNumGhostAgents = new Accumulate (sim, "Number of ghost agents");
      setAgentGroup (group);
   }

   /**
    * Sets the simulator attached to internal accumulates
    * to \texttt{sim}.
    * @param sim the new simulator.
    * @exception NullPointerException if \texttt{sim} is \texttt{null}.
    */
   public void setSimulator (Simulator sim) {
      for (final Accumulate element : statBusyAgents)
         element.setSimulator (sim);
      statNumAgents.setSimulator (sim);
      statNumBusyAgents.setSimulator (sim);
      statNumFreeAgents.setSimulator (sim);
      statNumIdleAgents.setSimulator (sim);
      statNumGhostAgents.setSimulator (sim);
   }

   /**
    * Returns the agent group currently associated
    * with this object.
    @return the currently associated agent group.
    */
   public final AgentGroup getAgentGroup() {
      return agentGroup;
   }

   /**
    * Sets the associated agent group
    * to \texttt{agentGroup}.
    * If the given group is \texttt{null}, the statistical collector is
    * disabled until a non-\texttt{null} agent group is given.
    * This can be used during a replication if
    * the integrals must be computed during some periods only.
    @param agentGroup the new associated agent group.
    */
   public final void setAgentGroup (AgentGroup agentGroup) {
      if (this.agentGroup != null)
         this.agentGroup.removeAgentGroupListener (vl);
      this.agentGroup = agentGroup;
      if (agentGroup != null)
         agentGroup.addAgentGroupListener (vl);
      updateValues();
   }

   /**
    * Returns the statistical probe computing the
    * integral of the total number of agents
    * over the simulation time.
    @return the statistical probe for the total number of agents.
    */
   public Accumulate getStatNumAgents() {
      return statNumAgents;
   }

   /**
    * Returns the statistical probe computing the
    * integral of the number of ghost agents
    * over the simulation time.
    @return the statistical probe for the number of ghost agents.
    */
   public Accumulate getStatNumGhostAgents() {
      return statNumGhostAgents;
   }

   /**
    * Returns the statistical probe computing the
    * integral of the number of idle (available and unavailable)
    * agents over the simulation time.
    @return the statistical probe for the number of idle agents.
    */
   public Accumulate getStatNumIdleAgents() {
      return statNumIdleAgents;
   }

   /**
    * Returns the statistical probe computing the
    * integral of the number of free
    * agents over the simulation time.
    @return the statistical probe for the number of free agents.
    */
   public Accumulate getStatNumFreeAgents() {
      return statNumFreeAgents;
   }

   /**
    * Returns the statistical probe computing the
    * integral of the number of busy
    * agents over the simulation time.
    @return the statistical probe for the number of busy agents.
    */
   public Accumulate getStatNumBusyAgents() {
      return statNumBusyAgents;
   }

   /**
    * Returns the statistical probe computing the
    * integral of the number of busy agents
    * serving contacts of type~\texttt{k}, over the
    * simulation time.
    @param k the queried contact type.
    @return the service volume statistical probe.
    @exception ArrayIndexOutOfBoundsException if \texttt{k}
    is negative or greater than or equal to the number of supported contact types.
    */
   public Accumulate getStatNumBusyAgents (int k) {
      return statBusyAgents[k];
   }

   /**
    * Returns the number of contact types
    * supported by this object.
    * @return the number of supported contact types.
    */
   public int getNumContactTypes() {
      return statBusyAgents.length;
   }

   private void updateValues() {
      if (agentGroup == null) {
         for (final Accumulate stat : statBusyAgents)
            stat.update (0);
         statNumBusyAgents.update (0);
         statNumIdleAgents.update (0);
         statNumAgents.update (0);
         statNumFreeAgents.update (0);
         statNumGhostAgents.update (0);
      }
      else {
         statNumBusyAgents.update (agentGroup.getNumBusyAgents());
         statNumIdleAgents.update (agentGroup.getNumIdleAgents());
         statNumAgents.update (agentGroup.getNumAgents());
         statNumFreeAgents.update (agentGroup.getNumFreeAgents());
         statNumGhostAgents.update (agentGroup.getNumGhostAgents());
         if (statBusyAgents.length > 0)
            if (agentGroup.getNumBusyAgents() > 0)
               // This happens only when init is called during a simulation replication
               // or when setAgentGroup is called with an agent group having
               // busy agents. Since such uses of this class are not
               // recommend, one will rarely need to keep end service events.
//               final int[] nb = new int[statBusyAgents.length];
//               for (final Iterator<EndServiceEvent> it = agentGroup.endServiceEventsIterator(); it.hasNext(); ) {
//                  final EndServiceEvent esv = it.next();
//                  final int tid = esv.getContact().getTypeId();
//                  if (tid < nb.length)
//                     ++nb[tid];
//               }
//               for (int k = 0; k < nb.length; k++)
//                  statBusyAgents[k].update (nb[k]);
               for (int k = 0; k < statBusyAgents.length; k++)
                  statBusyAgents[k].update (agentGroup.getNumBusyAgents (k));
            else
               for (final Accumulate stat : statBusyAgents)
                  stat.update (0);
      }
   }

   public void init() {
      for (final Accumulate stat : statBusyAgents)
         stat.init();
      statNumBusyAgents.init();
      statNumIdleAgents.init();
      statNumAgents.init();
      statNumFreeAgents.init();
      statNumGhostAgents.init();
      updateValues();
   }

   private final class VolumeListener implements AgentGroupListener {
      public void agentGroupChange (AgentGroup group) {
         // If the object was computing the total volume for
         // several agent groups, it would be necessary to
         // iterate on all the groups to get the total value here.
         // Another problem: we do not know for which groups
         // we are registered to if GroupVolumeStat is
         // an AgentGroupListener.
         statNumAgents.update (group.getNumAgents());
         statNumGhostAgents.update (group.getNumGhostAgents());
         statNumIdleAgents.update (group.getNumIdleAgents());
         statNumFreeAgents.update (group.getNumFreeAgents());
      }

      public void beginService (EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         final AgentGroup group = ev.getAgentGroup();
         // Same problems as in agentGroupChange if
         // the object computes the measure
         // for several agent groups at once.
         final int type = contact.getTypeId();
         if (type >= 0 && type < statBusyAgents.length)
            statBusyAgents[type].update (statBusyAgents[type].getLastValue() + 1);
         statNumBusyAgents.update (group.getNumBusyAgents());
         statNumIdleAgents.update (group.getNumIdleAgents());
         statNumFreeAgents.update (group.getNumFreeAgents());
      }

      public void endContact (EndServiceEvent ev) {}

      public void endService (EndServiceEvent ev) {
         final Contact contact = ev.getContact();
         final AgentGroup group = ev.getAgentGroup();
         final int type = contact.getTypeId();
         if (type >= 0 && type < statBusyAgents.length) {
            statBusyAgents[type].update (statBusyAgents[type].getLastValue() - 1);
            assert statBusyAgents[type].getLastValue () >= 0 :
               "Negative number of busy agents in accumulate for contact type " + type + ": " +
               statBusyAgents[type].getLastValue ();
         }
         statNumBusyAgents.update (group.getNumBusyAgents());
         statNumIdleAgents.update (group.getNumIdleAgents());
         statNumFreeAgents.update (group.getNumFreeAgents());
         // The number of available agents will not change, but
         // there may be one less ghost agent.
         statNumGhostAgents.update (group.getNumGhostAgents());
         assert statNumAgents.sum() >= 0;
         assert statNumFreeAgents.sum() >= 0;
         assert statNumBusyAgents.sum() >= 0;
         assert statNumIdleAgents.sum() >= 0;
         assert statNumGhostAgents.sum() >= 0;
      }

      public void init (AgentGroup group) {
         // getNumBusyAgents is 0
         for (final Accumulate stat : statBusyAgents)
            stat.update (0);
         statNumBusyAgents.update (0);
         statNumAgents.update (group.getNumAgents());
         statNumGhostAgents.update (group.getNumGhostAgents());
         statNumFreeAgents.update (group.getNumFreeAgents());
         statNumIdleAgents.update (group.getNumIdleAgents());
      }

      @Override
      public String toString() {
         final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
         sb.append ('[');
         sb.append ("associated agent group: ").append
            (ContactCenter.toShortString (agentGroup));
         sb.append (']');
         return sb.toString();
      }
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("associated agent group: ").append
         (ContactCenter.toShortString (agentGroup));
      sb.append (']');
      return sb.toString();
   }

   /**
    * Constructs and returns a clone of this
    * agent-group statistical collector.
    * This method clones the internal
    * statistical collectors, but
    * the clone has no associated agent
    * group.
    * This can be used to save
    * the state of the statistical
    * collector for
    * future restoration.
    * @return a clone of this object.
    */
   @Override
   public GroupVolumeStat clone() {
      GroupVolumeStat cpy;
      try {
         cpy = (GroupVolumeStat)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("CloneNotSupportedException for a class implementing Cloneable");
      }
      cpy.statBusyAgents = statBusyAgents.clone ();
      for (int i = 0; i < statBusyAgents.length; i++)
         cpy.statBusyAgents[i] = statBusyAgents[i].clone ();
      cpy.statNumBusyAgents = statNumBusyAgents.clone();
      cpy.statNumIdleAgents = statNumIdleAgents.clone();
      cpy.statNumAgents = statNumAgents.clone();
      cpy.statNumFreeAgents = statNumFreeAgents.clone();
      cpy.statNumGhostAgents = statNumGhostAgents.clone();
      cpy.vl = cpy.new VolumeListener();
      cpy.agentGroup = null;
      cpy.updateValues ();
      return cpy;
   }
}
