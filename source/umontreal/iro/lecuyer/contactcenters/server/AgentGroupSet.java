package umontreal.iro.lecuyer.contactcenters.server;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a set of agent groups for which it
 * is possible to get the total number of members.
 */
public class AgentGroupSet extends AbstractSet<AgentGroup> implements Initializable, Named, Cloneable {
   private String name = "";
   private Set<AgentGroup> groups = new LinkedHashSet<AgentGroup>();

   private NumberChecker nc = new NumberChecker();

   private boolean collect = false;
   private Accumulate statNumAgents;
   private Accumulate statNumFreeAgents;
   private Accumulate statNumIdleAgents;
   private Accumulate statNumBusyAgents;
   private Accumulate statNumGhostAgents;

   public String getName() {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
      setProbeNames();
   }

   private void setProbeNames() {
      if (statNumAgents == null)
         return;
      String gn;
      if (getName().length() > 0)
         gn = " (" + getName() + ")";
      else
         gn = "";
      statNumAgents.setName ("Number of agents" + gn);
      statNumFreeAgents.setName ("Number of free agents" + gn);
      statNumGhostAgents.setName ("Number of ghost agents" + gn);
      statNumBusyAgents.setName ("Number of busy agents" + gn);
      statNumIdleAgents.setName ("Number of idle agents" + gn);
   }

   /**
    * Returns the total number of agents currently
    * in the registered agent groups.
    @return the total number of agents.
    */
   public int getNumAgents() {
      int numAgents = 0;
      for (final AgentGroup group : groups)
         numAgents += group.getNumAgents();
      return numAgents;
   }

   /**
    * Returns the total number of free agents
    * currently in the set of agent groups.
    @return the total number of free agents.
    */
   public int getNumFreeAgents() {
      int numFreeAgents = 0;
      for (final AgentGroup group : groups)
         numFreeAgents += group.getNumFreeAgents();
      return numFreeAgents;
   }

   /**
    * Returns the total number of busy agents
    * currently in the set of agent groups.
    @return the total number of busy agents.
    */
   public int getNumBusyAgents() {
      int numBusyAgents = 0;
      for (final AgentGroup group : groups)
         numBusyAgents += group.getNumBusyAgents();
      return numBusyAgents;
   }

   /**
    * Returns the total number of idle agents
    * currently in the set of agent groups.
    @return the total number of idle agents.
    */
   public int getNumIdleAgents() {
      int numIdleAgents = 0;
      for (final AgentGroup group : groups)
         numIdleAgents += group.getNumIdleAgents();
      return numIdleAgents;
   }

   /**
    * Returns the total number of ghost agents
    * currently in the set of agent groups.
    @return the total number of ghost agents.
    */
   public int getNumGhostAgents() {
      int numGhostAgents = 0;
      for (final AgentGroup group : groups)
         numGhostAgents += group.getNumGhostAgents();
      return numGhostAgents;
   }

   private void update() {
      if (collect) {
         statNumAgents.update (getNumAgents());
         statNumFreeAgents.update (getNumFreeAgents());
         statNumBusyAgents.update (getNumBusyAgents());
         statNumIdleAgents.update (getNumIdleAgents());
         statNumGhostAgents.update (getNumGhostAgents());
      }
   }

   /**
    * Adds the agent group \texttt{group} to
    * this set of agent groups.
    @param group the agent group being added.
    @exception NullPointerException if \texttt{group} is \texttt{null}.
    */
   @Override
   public boolean add (AgentGroup group) {
      if (group == null)
         throw new NullPointerException();
      final boolean added = groups.add (group);
      if (added) {
         group.addAgentGroupListener (nc);
         update();
         return true;
      }
      return false;
   }

   @Override
   public boolean contains (Object o) {
      return groups.contains (o);
   }

   @Override
   public int size() {
      return groups.size();
   }

   @Override
   public boolean isEmpty() {
      return groups.isEmpty();
   }

   @Override
   public Iterator<AgentGroup> iterator() {
      return new MyIterator (groups.iterator());
   }

   /**
    * Removes the agent group \texttt{group} from
    * this set of agent groups.
    @param group the agent group being removed.
    @exception NullPointerException if \texttt{group} is \texttt{null}.
    */
   @Override
   public boolean remove (Object group) {
      final boolean removed = groups.remove (group);
      if (removed) {
         ((AgentGroup)group).removeAgentGroupListener (nc);
         update();
         return true;
      }
      return false;
   }

   /**
    * Removes all the agent groups contained
    * in this set of agent groups.
    */
   @Override
   public void clear() {
      for (final AgentGroup group : groups)
         group.removeAgentGroupListener (nc);
      groups.clear();
      update();
   }

   /**
    * Initializes all the agent groups in this set of agent groups.
    */
   public void init() {
      for (final AgentGroup group : groups)
         group.init();
      if (collect)
         initStat();
   }

   /**
    * Initializes the statistical collectors
    * for this set of agent groups.  If statistical collecting
    * is turned OFF, this
    * throws an {@link IllegalStateException}.
    @exception IllegalStateException if statistical
    collecting is turned OFF.
    */
   public void initStat() {
      if (!collect)
         throw new IllegalStateException
            ("Statistical collecting is disabled");
      update();
   }

   /**
    * Determines if this set of agent groups is collecting
    * statistics about the number of agents.
    * If this returns \texttt{true}, statistical collecting is turned ON.
    * Otherwise (the default), it is turned OFF.
    @return the state of statistical collecting.
    */
   public boolean isStatCollecting() {
      return collect;
   }

   /**
    * Sets the state of statistical collecting to \texttt{b}.
    * If \texttt{b} is \texttt{true}, statistical collecting is turned ON, and
    * the statistical collectors are created or reinitialized.
    * If \texttt{b} is \texttt{false}, statistical collecting is turned OFF.
    @param b the new state of statistical collecting.
    */
   public void setStatCollecting (boolean b) {
      if (b)
         setStatCollecting (Simulator.getDefaultSimulator ());
      else
         collect = false;
   }

   /**
    * Enables statistical collecting, and uses
    * the given simulator \texttt{sim}.
    * The simulator is used by the internal accumulates
    * when the simulation time is required to update
    * probes with new values.
    * @param sim the simulator attached to accumulates.
    */
   public void setStatCollecting (Simulator sim) {
      if (sim == null)
         throw new NullPointerException();
      collect = true;
      if (statNumAgents == null) {
         statNumAgents = new Accumulate (sim);
         statNumFreeAgents = new Accumulate (sim);
         statNumBusyAgents = new Accumulate (sim);
         statNumIdleAgents = new Accumulate (sim);
         statNumGhostAgents = new Accumulate (sim);
         setProbeNames();
      }
      else {
         statNumAgents.setSimulator (sim);
         statNumFreeAgents.setSimulator (sim);
         statNumBusyAgents.setSimulator (sim);
         statNumIdleAgents.setSimulator (sim);
         statNumGhostAgents.setSimulator (sim);
      }
      initStat();
   }

   /**
    * Returns the statistical collector for the number of
    * agents in the agent groups.  This returns a non-\texttt{null} value
    * only if statistical collecting was turned ON since
    * this object was constructed.
    @return the statistical collector for the number of agents.
    */
   public Accumulate getStatNumAgents() {
      return statNumAgents;
   }

   /**
    * Returns the statistical collector for the number of free
    * agents in the agent groups.  This returns a non-\texttt{null} value
    * only if statistical collecting was turned ON since
    * this object was constructed.
    @return the statistical collector for the number of free agents.
    */
   public Accumulate getStatNumFreeAgents() {
      return statNumFreeAgents;
   }

   /**
    * Returns the statistical collector for the number of busy
    * agents in the agent groups.  This returns a non-\texttt{null} value
    * only if statistical collecting was turned ON since
    * this object was constructed.
    @return the statistical collector for the number of busy agents.
    */
   public Accumulate getStatNumBusyAgents() {
      return statNumBusyAgents;
   }

   /**
    * Returns the statistical collector for the number of idle
    * agents in the agent groups.  This returns a non-\texttt{null} value
    * only if statistical collecting was turned ON since
    * this object was constructed.
    @return the statistical collector for the number of idle agents.
    */
   public Accumulate getStatNumIdleAgents() {
      return statNumIdleAgents;
   }

   /**
    * Returns the statistical collector for the number of ghost
    * agents in the agent groups.  This returns a non-\texttt{null} value
    * only if statistical collecting was turned ON since
    * this object was constructed.
    @return the statistical collector for the number of ghost agents.
    */
   public Accumulate getStatNumGhostAgents() {
      return statNumGhostAgents;
   }

   private final class NumberChecker implements AgentGroupListener {
      public void agentGroupChange (AgentGroup group) {
         assert groups.contains (group) :
            "The agent group " + group.toString() + " is not a member of the set " +
            AgentGroupSet.this.toString();
         update();
      }

      public void endContact (EndServiceEvent ev) {
         assert groups.contains (ev.getAgentGroup()) :
            "The agent group " + ev.getAgentGroup().toString() + " is not a member of the set " +
            AgentGroupSet.this.toString();
      }

      public void endService (EndServiceEvent ev) {
         assert groups.contains (ev.getAgentGroup()) :
            "The agent group " + ev.getAgentGroup().toString() + " is not a member of the set " +
            AgentGroupSet.this.toString();
         update();
      }

      public void beginService (EndServiceEvent ev) {
         assert groups.contains (ev.getAgentGroup()) :
            "The agent group " + ev.getAgentGroup().toString() + " is not a member of the set " +
            AgentGroupSet.this.toString();
         update();
      }

      public void init (AgentGroup group) {
         assert groups.contains (group) :
            "The agent group " + group.toString() + " is not a member of the set " +
            AgentGroupSet.this.toString();
         update();
      }

      @Override
      public String toString() {
         final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
         sb.append ('[');
         sb.append ("associated set of agent groups: ").append
            (ContactCenter.toShortString (AgentGroupSet.this));
         sb.append (']');
         return sb.toString();
      }
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      if (getName().length() > 0)
         sb.append ("name: ").append (getName()).append (", ");
      sb.append ("agent groups in the set: ").append
         (groups.size());
      if (groups.size() > 0) {
         sb.append (", total number of agents: ").append
            (getNumAgents());
         sb.append (", total number of free agents: ").append
            (getNumFreeAgents());
         sb.append (", total number of busy agents: ").append
            (getNumBusyAgents());
         sb.append (", total number of idle agents: ").append
            (getNumIdleAgents());
         sb.append (", total number of ghost agents: ").append
            (getNumGhostAgents());
      }
      sb.append (", statistical collecting ");
      if (collect)
         sb.append ("ON");
      else
         sb.append ("OFF");
      sb.append (']');
      return sb.toString();
   }

   @Override
   public AgentGroupSet clone() {
      AgentGroupSet cpy;
      try {
         cpy = (AgentGroupSet)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
      cpy.groups = new LinkedHashSet<AgentGroup> (groups);
      cpy.statNumAgents = statNumAgents.clone();
      cpy.statNumFreeAgents = statNumFreeAgents.clone();
      cpy.statNumIdleAgents = statNumIdleAgents.clone();
      cpy.statNumBusyAgents = statNumBusyAgents.clone();
      cpy.statNumGhostAgents = statNumGhostAgents.clone();
      cpy.nc = cpy.new NumberChecker();
      for (final AgentGroup group : cpy.groups)
         group.addAgentGroupListener (cpy.nc);
      return cpy;
   }

   private class MyIterator implements Iterator<AgentGroup> {
      private Iterator<AgentGroup> itr;
      private AgentGroup lastRet;

      public MyIterator (Iterator<AgentGroup> itr) {
         this.itr = itr;
      }

      public boolean hasNext () {
         return itr.hasNext();
      }

      public AgentGroup next () {
         lastRet = itr.next();
         return lastRet;
      }

      public void remove () {
         itr.remove();
         lastRet.removeAgentGroupListener (nc);
         update();
         lastRet = null;
      }
   }
}
