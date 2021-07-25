package umontreal.iro.lecuyer.contactcenters.server;

import java.util.Arrays;
import java.util.Set;

import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents the state of an agent group, i.e.,
 * the contacts being served at a specific simulation time.
 */
public class AgentGroupState {
   private EndServiceEvent[] contactsInService;
   private int numAgents;
   private int numFreeAgents;
   private int numGhostAgents;
   private double efficiency;
   
   /**
    * Constructs a new state object holding
    * the state of the agent group \texttt{group}.
    * @param group the agent group to be saved.
    */
   protected AgentGroupState (AgentGroup group) {
      save (group);
   }
   
   /**
    * Saves the state of the agent group \texttt{group}
    * in this object.
    * This requires {@link AgentGroup#isKeepingEndServiceEvents()}
    * to return \texttt{true} for \texttt{group}.
    * @param group the agent group for which the state must be saved.
    */
   private void save (AgentGroup group) {
      numAgents = group.getNumAgents();
      numFreeAgents = group.getNumFreeAgents();
      numGhostAgents = group.getNumGhostAgents();
      efficiency = group.getEfficiency();
      final Set<EndServiceEvent> esev = group.getEndServiceEvents();
      contactsInService = new EndServiceEvent[esev.size()];
      int idx = 0;
      for (final EndServiceEvent es : esev) {
         assert !es.isObsolete() : "An agent group is storing an obsolete end-service event";
         contactsInService[idx++] = es.clone();
      }
   }
   
   /**
    * Restores the state of the agent group \texttt{group}
    * with the information stored into this object.
    * @param group the agent group to restore.
    */
   void restore (AgentGroup group) {
//      group.setNumAgents (contactsInService.length);
//      group.setEfficiency (1.0);
      group.init();
//      double maxTime = 0;
//      for (int i = 0; i < contactsInService.length; i++) {
//         StartServiceEvent ev = new StartServiceEvent (group, contactsInService[i]);
//         ev.schedule();
//         double time = ev.time();
//         if (time > maxTime)
//            maxTime = time;
//      }
//      new SetNumAgentsEvent (group, numAgents, efficiency).schedule (maxTime - Sim.time());
      restoreServices (group);
   }
   
   void restoreServices (AgentGroup group) {
      group.numAgents = numAgents;
      group.efficiency = efficiency;
      group.numBusyAgents = contactsInService.length;
      Arrays.fill (group.numBusyAgentsK, 0);
      group.numFreeAgents = numFreeAgents;
      group.numGhostAgents = numGhostAgents;
      for (final EndServiceEvent oldEv : contactsInService) {
         final EndServiceEvent newEv = createEndServiceEvent (oldEv, group);
         newEv.contactTime = oldEv.getScheduledContactTime();
         newEv.ecType = oldEv.getScheduledEndContactType();
         newEv.ghostAgent = oldEv.wasGhostAgent();
         if (oldEv.contactDone()) {
            newEv.contactDone = true;
            newEv.econtactTime = oldEv.getEffectiveContactTime();
            newEv.eecType = oldEv.getEffectiveEndContactType();
            newEv.afterContactTime = oldEv.getScheduledAfterContactTime();
            newEv.esType = oldEv.getScheduledEndServiceType();
            final double afterContactTime = oldEv.getScheduledAfterContactTime();
            if (!Double.isInfinite (afterContactTime) && !Double.isNaN (afterContactTime))
               newEv.schedule
               (oldEv.getBeginServiceTime() + oldEv.getEffectiveContactTime()
                     + afterContactTime - newEv.simulator().time());
         }
         else {
            final double contactTime = oldEv.getScheduledContactTime();
            if (!Double.isInfinite (contactTime) && !Double.isNaN (contactTime))
               newEv.schedule (oldEv.getBeginServiceTime() + contactTime - newEv.simulator().time());
            newEv.getContact().beginService (newEv);
         }
         if (group.esevSet != null)
            group.esevSet.add (newEv);
         final int k = newEv.getContact ().getTypeId ();
         if (k >= 0) {
            if (k >= group.numBusyAgentsK.length)
               group.numBusyAgentsK = ArrayUtil.resizeArray (group.numBusyAgentsK, k + 1);
            ++group.numBusyAgentsK[k];
         }
      }
   }
   
   EndServiceEvent createEndServiceEvent (EndServiceEvent oldEv, AgentGroup group) {
      return new EndServiceEvent (group, oldEv.getContact().clone(), oldEv.getBeginServiceTime());
   }

   /**
    * Returns the end-service events representing
    * the contacts being served at the time the
    * state was saved.
    * @return the array of end-service events representing contacts in service.
    */
   public EndServiceEvent[] getContactsInService () {
      return contactsInService;
   }

   /**
    * Returns the efficiency of the agent group
    * at the time of state saving.
    * @return the efficiency at the time the state was saved.
    */
   public double getEfficiency () {
      return efficiency;
   }

   /**
    * Returns the number of agents in the
    * agent group at the time of state saving.
    * @return the number of agents at the time the state was saved.
    */
   public int getNumAgents () {
      return numAgents;
   }
   
   /**
    * Returns the number of free agents in the
    * agent group at the time of state saving.
    * @return the number of free agents at the time the state was saved.
    */
   public int getNumFreeAgents() {
      return numFreeAgents;
   }
   
   /**
    * Returns the number of ghost agents in the
    * agent group at the time of state saving.
    * @return the number of ghost agents at the time the state was saved.
    */
   public int getNumGhostAgents() {
      return numGhostAgents;
   }
}
