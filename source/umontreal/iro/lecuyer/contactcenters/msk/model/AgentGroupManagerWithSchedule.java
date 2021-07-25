package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupScheduleParams;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;

/**
 * Manages an agent group whose member follow a given
 * schedule. A schedule is composed of shifts that can
 * start and end at arbitrary times during the simulation horizon.
 * This agent group manager encapsulates a simulation
 * event for each shift.
 * This event is used to add or remove agents to the managed group
 * during simulation.
 */
public class AgentGroupManagerWithSchedule extends AgentGroupManager {
   private ShiftEvent[] shiftEvents;
   
   /**
    * Constructs the new schedule-based agent group manager.
    * @param cc the call center model.
    * @param par the agent group parameters.
    * @param i the index of the agent group.
    * @throws AgentGroupCreationException if an exception occurs
    * when creating the agent group.
    */
   public AgentGroupManagerWithSchedule (CallCenter cc, AgentGroupParams par, int i) throws AgentGroupCreationException {
      super (cc, par, i);
      final AgentGroupScheduleParams scheduleParams = par.getSchedule();
      AgentGroupSchedule schedule = new AgentGroupSchedule (cc, scheduleParams);
      setSchedule (schedule);
      ScheduleShift[] shifts = schedule.getShifts ();
      shiftEvents = new ShiftEvent[shifts.length];
      for (int j = 0; j < shiftEvents.length; j++)
         shiftEvents[j] = new ShiftEvent (getAgentGroup(), null, shifts[j]);
   }
   
   /**
    * Constructs and returns a detailed agent group, which
    * is needed to add and remove agents.
    */
   @Override
   protected AgentGroup createAgentGroup (AgentGroupParams par, int i) throws AgentGroupCreationException {
      if (par.isSetDetailed () && !par.isDetailed ())
         throw new AgentGroupCreationException
         ("An agent group with schedule must be detailed; set the detailed attribute to true");
      return new DetailedAgentGroup (getCallCenter ().simulator (), 0);
   }

   /**
    * Returns the shifts composing the schedule of the
    * agents.
    * @return the shifts composing the schedule.
    */
   public ScheduleShift[] getShifts() {
      return getSchedule().getShifts ();
   }
   
   /**
    * Returns the number of shifts in the schedule.
    * @return the number of shifts in the schedule.
    */
   public int getNumShifts() {
      return getSchedule().getNumShifts();
   }
   
   /**
    * Returns the shift with index \texttt{i}.
    * @param i the index of the shift.
    * @return the corresponding shift.
    */
   public ScheduleShift getShift (int i) {
      return getSchedule().getShift (i);
   }
   
   /**
    * Returns a vector giving the raw number of agents
    * for each shift.
    * This method is for internal use;
    * the method {@link #getEffectiveNumAgents()}
    * is recommended to take
    * account of agents multipliers into account.
    * @return the raw number of agents per shift.
    */
   public int[] getNumAgents() {
      return getSchedule().getNumAgents ();
   }
   
   /**
    * Returns the raw number of agents in shift \texttt{shift}.
    * The method {@link #getEffectiveNumAgents(int)}
    * can be used to take agents multipliers into account.
    * @param shift the index of the shift.
    * @return the raw number of agents on the shift.
    */
   public int getNumAgents (int shift) {
      return getSchedule().getNumAgents (shift);
   }
   
   /**
    * Returns the effective number of agents during
    * each shift.
    * This method calls {@link #getNumAgents()}, and
    * multiplies each element of the returned array
    * by $m*m_i$, where $m$ is determined by
    * {@link CallCenter#getAgentsMult()} and
    * $m_i$ is given by {@link #getAgentsMult()}.
    * The resulting numbers are rounded to the nearest integers, and
    * stored in the array being returned.
    * @return the effective number of agents during each shift.
    */
   public int[] getEffectiveNumAgents() {
      int[] numAgents = getNumAgents();
      double mult = getAgentsMult ()*getCallCenter().getAgentsMult ();
      if (mult != 1.0) {
         for (int j = 0; j < numAgents.length; j++)
            numAgents[j] = (int)Math.round (mult*numAgents[j]);
      }
      return numAgents;
   }
   
   /**
    * Similar to {@link #getEffectiveNumAgents()},
    * for a specific shift \texttt{shift}.
    * @param shift  the index of the tested shift.
    * @return the effective number of agents on the shift.
    */
   public int getEffectiveNumAgents (int shift) {
      int na = getNumAgents (shift);
      double mult = getAgentsMult ()*getCallCenter().getAgentsMult ();
      if (mult == 1.0)
         return na;
      return (int)Math.round (na*mult);
   }
   
   /**
    * Sets the vector of raw numbers of agents to
    * \texttt{numAgents}.
    * @param numAgents the new vector of agents.
    */
   public void setNumAgents (int[] numAgents) {
      getSchedule().setNumAgents (numAgents);
   }

   /**
    * Sets the raw number of agents in shift \texttt{shift}
    * to \texttt{n}. 
    * @param shift the index of the affected shift.
    * @param n the new number of agents.
    */
   public void setNumAgents (int shift, int n) {
      getSchedule().setNumAgents (shift, n);
   }

   /**
    * Sets the effective number of agents for each shift
    * of the managed agent group
    * to \texttt{numAgents}.
    * This method sets the
    * number of agents to \texttt{numAgents}
    * using {@link #setNumAgents(int[])}, but it also
    * resets the value of the multiplier $m*m_k$ to 1.
    * This makes sure that {@link #getEffectiveNumAgents()}
    * will return the same value as the vector
    * passed to this method.
    * @param numAgents the new vector of agents.
    */
   public void setEffectiveNumAgents (int[] numAgents) {
      getCallCenter().resetAgentsMult ();
      setNumAgents (numAgents);
      setAgentsMult (1.0);
   }
   
   /**
    * Similar to {@link #setEffectiveNumAgents(int[])},
    * but only sets the number of agents in shift \texttt{shift}
    * to \texttt{n} instead of the number of agents in all shifts.
    * @param shift the index of the affected shift.
    * @param n the new number of agents.
    */
   public void setEffectiveNumAgents (int shift, int n) {
      getCallCenter ().resetAgentsMult ();
      if (getAgentsMult() == 1.0)
         setNumAgents (shift, n);
      else {
         int[] numAgents = getEffectiveNumAgents ();
         setAgentsMult (1.0);
         numAgents[shift] = n;
         setNumAgents (numAgents);
      }
   }
   
   /**
    * Computes and returns the matrix of shifts.
    * Element $(j, p)$ of this $J\times P$ matrix, where
    * $J$ corresponds to the number of shifts and
    * $P$, to the number of main periods,
    * is \texttt{true} if and only if agents are scheduled to work
    * on shift \texttt{j} during main period \texttt{p}.
    */
   public boolean[][] getShiftMatrix() {
      return getSchedule().getShiftMatrix (getCallCenter().getPeriodChangeEvent ());
   }
   
   /**
    * Similar to {@link #getShiftMatrix()}, but
    * returns a matrix of integers, with 0 meaning
    * \texttt{false}, and 1 meaning \texttt{true}.
    */
   public int[][] getShiftMatrixInt() {
      return getSchedule().getShiftMatrixInt (getCallCenter().getPeriodChangeEvent ());
   }

   /**
    * Computes and returns the staffing vector.
    * This corresponds to the column vector returned by
    * {@link #getNumAgents()} multiplied by
    * the matrix returned by {@link #getShiftMatrix()}.
    */
   @Override
   public int[] getStaffing () {
      return getSchedule().getStaffing (getCallCenter().getPeriodChangeEvent ());
   }
   
   @Override
   public int getStaffing (int mp) {
      final int[] staffing = getStaffing();
      return staffing[mp];
   }
   
   @Override
   public DetailedAgentGroup getAgentGroup() {
      return (DetailedAgentGroup)super.getAgentGroup();
   }
   
   @Override
   public void init() {
      super.init();
      getAgentGroup().setNumAgents (0);
      if (getCallCenter().getPeriodChangeEvent().isLockedPeriod()) {
         final int[] staffing = getEffectiveStaffing();
         getAgentGroup().setNumAgents (staffing[getCallCenter().getPeriodChangeEvent().getCurrentMainPeriod()]);
      }
      else
         for (final ShiftEvent event : shiftEvents) {
            event.init (getProbDisconnectStream (), getAgentsMult()*getCallCenter().getAgentsMult ());
            event.schedule();
         }
   }
}
