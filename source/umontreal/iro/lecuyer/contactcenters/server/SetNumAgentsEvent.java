package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a simulation that sets the number of agents
 * and agents' efficiency in an agent group.
 */
public class SetNumAgentsEvent extends Event {
   private AgentGroup group;
   private int numAgents;
   private double efficiency;

   /**
    * Constructs a new set-num-agents event
    * that sets the number of agents in
    * the group \texttt{group} to
    * \texttt{numAgents}, and the
    * efficiency factor to \texttt{efficiency}.
    * @param group the target agent group.
    * @param numAgents the number of agents in the group after the event occurs.
    * @param efficiency the efficiency after the event occurs.
    */
   public SetNumAgentsEvent (AgentGroup group, int numAgents, double efficiency) {
      this (Simulator.getDefaultSimulator(), group, numAgents, efficiency);
   }

   /**
    * Equivalent to {@link #SetNumAgentsEvent(AgentGroup,int,double)},
    * using the given simulator \texttt{sim}.
    */
   public SetNumAgentsEvent (Simulator sim, AgentGroup group, int numAgents, double efficiency) {
      super (sim);
      if (group == null)
         throw new NullPointerException();
      if (numAgents < 0)
         throw new IllegalArgumentException
         ("numAgents must not be negative");
      if (efficiency < 0 || efficiency > 1)
         throw new IllegalArgumentException
         ("The efficiency must be in [0,1]");
      this.group = group;
      this.numAgents = numAgents;
      this.efficiency = efficiency;
   }

   /**
    * Returns the agent group affected by this event.
    * @return the target agent group.
    */
   public AgentGroup getTargetAgentGroup() {
      return group;
   }

   /**
    * Returns the number of agents in the
    * target group after the event occurs.
    * @return the desired number of agents in the target group.
    */
   public int getNumAgents() {
      return numAgents;
   }

   /**
    * Returns the agents' efficiency in the
    * target group after this event occurs.
    * @return the desired efficiency in the target agent group.
    */
   public double getEfficiency() {
      return efficiency;
   }

   @Override
   public void actions () {
      group.setNumAgents (numAgents);
      group.setEfficiency (efficiency);
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Target agent group: ").append (group.toString());
      sb.append (", number of agents: ").append (numAgents);
      sb.append (", agents' efficiency: ").append (efficiency);
      sb.append (']');
      return sb.toString();
   }
}
