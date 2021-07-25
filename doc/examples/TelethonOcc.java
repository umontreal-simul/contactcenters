import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;

import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;

public class TelethonOcc extends Telethon {
   Tally occupancyCorr = new Tally ("Corrected agents' occupancy ratio");
   GroupVolumeStat vstatCorr;

   TelethonOcc() {
      super();
      volunteers.addAgentGroupListener (new MyAgentGroupListener());
      vstatCorr = new GroupVolumeStat (volunteers);
   }

   class MyAgentGroupListener implements AgentGroupListener {
      public void agentGroupChange (AgentGroup group) {
         if (group.getNumAgents() == 0)
            new SetAgentGroupEvent (null).scheduleNext();
         else
            new SetAgentGroupEvent (group).scheduleNext();
      }

      public void beginService (EndServiceEvent es) {}
      public void endContact (EndServiceEvent es) {}
      public void endService (EndServiceEvent es) {}
      public void init (AgentGroup group) { vstatCorr.init(); }
   }

   class SetAgentGroupEvent extends Event {
      AgentGroup group;

      public SetAgentGroupEvent (AgentGroup group) {
         this.group = group;
      }

      @Override
      public void actions() {
         vstatCorr.setAgentGroup (group);
      }
   }

   @Override
   public void addObs() {
      super.addObs();
      final double Nb = vstatCorr.getStatNumBusyAgents().sum();
      occupancyCorr.add (100.0*Nb/EXPNUMAGENTS);
   }

   @Override
   public void simulate (int n) {
      occupancyCorr.init();
      super.simulate (n);
   }

   @Override
   public void printStatistics() {
      super.printStatistics();
      System.out.println (occupancyCorr.reportAndCIStudent (LEVEL, 3));
   }

   public static void main (String[] args) {
      final Telethon t = new TelethonOcc();
      final Chrono timer = new Chrono();
      t.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      t.printStatistics();
   }
}
