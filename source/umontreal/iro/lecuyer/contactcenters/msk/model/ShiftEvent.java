
package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.ArrayList;
import java.util.Map;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;



/**
 * Represents a simulation event adding agents to a group
 * at the beginning of working parts of a shift, and removing
 * them at the end of working parts.
 * The agents to be added or removed are stored into
 * an internal array of {@link Agent} objects
 * so the agents are reused from parts to parts of
 * a given shift.
 */
public class ShiftEvent extends Event {
   private DetailedAgentGroup group;
   private ScheduleShift shift;
   private Agent[] agents;
   private int partIndex;
   boolean stoppingTime = false;
   
   /**
    * Constructs a new shift event managing agent group \texttt{group},
    * and using information in shift \texttt{shift}.
    * @param group the agent group to which agents are added and removed.
    * @param shift the shift used to determine the number of
    * agents and working parts.
    */
   public ShiftEvent (DetailedAgentGroup group, ScheduleShift shift) {
      this (group, null, shift);
   }
   
   /**
    * Similar to {@link #ShiftEvent(DetailedAgentGroup,ScheduleShift)},
    * except that the agents in array \texttt{agents} are
    * added and removed to the group rather than an array of
    * new {@link Agent} objects.
    */
   public ShiftEvent (DetailedAgentGroup group, Agent[] agents, ScheduleShift shift) {
      super (group.simulator ());
      this.group = group;
      this.shift = shift;
   }
   
   /**
    * Initializes this event with a new multiplier \texttt{mult},
    * and resets the internal part index. 
    * This method gets the number of agents on the associated shift,
    * multiplies this number with \texttt{mult}, and rounds the
    * result to the nearest integer; this gives the effective
    * number of agents on the shift.
    * The method then creates or updates an internal
    * array of {@link Agent} objects which are added and
    * removed from the associated group each time
    * the event occurs.
    * The array of agents is created or updated only if it does
    * not exist yet, or if its
    * length does not correspond to the
    * effective number of agents on the shift.
    * 
    * 
    * @param stream a random stream used to generate the number
    * of agents when it is random.
    * @param mult the multiplier for the number of agents.
    * @param serviceTimeList the list of maps of service time distributions for each agent in this group.
    * See {@link AgentGroupManagerWithAgents#getListMapServiceTimeAgentGroup()}.
    * @param agentIndex the index number of the agent in the group.
    * 
    * @exception IllegalArgumentException if \texttt{mult} is negative.
    * 
    */
   public void init (RandomStream stream, double mult, ArrayList<Map <Integer,MultiPeriodGen>> serviceTimeList, int agentIndex) {
	      if (mult < 0)
	         throw new IllegalArgumentException
	         ("mult must not be negative");
	      final int nmult = (int)Math.round (mult*shift.getNumAgents ());
	      final double p = shift.getAgentProbability ();
	      final int na;
	      if (p != 1)
	         na = BinomialDist.inverseF (nmult, p, stream.nextDouble ());
	      else
	         na = nmult;
	      if (agents == null || agents.length != na) {
	         agents = new Agent[na];
	         for (int i = 0; i < agents.length; i++)
	         { 
                   agents[i] = new Agent();
	           Map <Integer,MultiPeriodGen> map=serviceTimeList.get(agentIndex);
	           agents[i].setMapServiceTime(map);
	           String name=  group.getAgentGroupParam().getAgents().get(agentIndex).getName();
	           agents[i].setName(name);
	         }
	      }
	      partIndex = 0;
	   }
   
   
   
   public void init (RandomStream stream, double mult) {
   if (mult < 0)
      throw new IllegalArgumentException
      ("mult must not be negative");
   final int nmult = (int)Math.round (mult*shift.getNumAgents ());
   final double p = shift.getAgentProbability ();
   final int na;
   if (p != 1)
      na = BinomialDist.inverseF (nmult, p, stream.nextDouble ());
   else
      na = nmult;
   if (agents == null || agents.length != na) {
      agents = new Agent[na];
      for (int i = 0; i < agents.length; i++)
        agents[i] = new Agent();
      
   }
   partIndex = 0;
}
   
   
   /**
    * Schedules this event to occur at the next time
    * it is needed to add or remove the associated
    * agents from the attached group.
    * If the simulation time is greater than the ending time
    * of the last part of the shift, the event is not
    * scheduled anymore.
    * The method {@link #init(RandomStream,double)}
    * can be used to reset the event. 
    */
   public void schedule() {
      stoppingTime = false;
      final double simTime = simulator().time ();
      while (partIndex < shift.getNumParts()) {
         final ShiftPart part = shift.getPart (partIndex);
         if (!part.isWorking()) {
            ++partIndex;
            continue;
         }
         if (part.getStartingTime() >= simTime) {
            schedule (part.getStartingTime() - simTime);
            return;
         }
         ++partIndex;
      }
   }
   
   @Override
   public void actions() {
      if (stoppingTime) {
         for (final Agent agent : agents)
            agent.getAgentGroup ().removeAgent (agent);
         schedule();
      }
      else {
         for (final Agent agent : agents)
            group.addAgent (agent);
         schedule (shift.getPart (partIndex++).getEndingTime() - simulator().time());
         stoppingTime = true;
      }
   }

}
