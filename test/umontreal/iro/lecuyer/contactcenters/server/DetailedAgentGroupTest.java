package umontreal.iro.lecuyer.contactcenters.server;

import java.util.List;

import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;

import umontreal.ssj.simevents.Sim;

public class DetailedAgentGroupTest extends AgentGroupTest {
   DetailedAgentGroup dgroup;

   public DetailedAgentGroupTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      final int[] numAgents = { 0, 7, 11, 24, 45, 20, 5, 5 };
      Sim.init ();
      final ValueGenerator sgen = new ConstantValueGenerator (1, 100);
      pce = new PeriodChangeEvent (10.0, 8, 0);
      group = new DetailedAgentGroup (pce, numAgents);
      group.setContactTimeGenerator (0, sgen);
      group
            .addAgentGroupListener (listener = new TestAgentGroupListener (
                  group));
      pce.init ();
      pce.start ();
      group.setKeepingEndServiceEvents (true);
      group.init ();
      dgroup = new DetailedAgentGroup (0);
      dgroup.setKeepingEndServiceEvents (true);
      dgroup.init ();
   }

   @Override
   public void tearDown () {
      super.tearDown ();
      dgroup = null;
   }

   @Override
   public void testNumAgents () {
      assertEquals ("Number of agents", 0, dgroup.getNumAgents ());
      assertEquals ("Number of free agents", 0, dgroup.getNumFreeAgents ());
      assertEquals ("Number of busy agents", 0, dgroup.getNumBusyAgents ());
      assertEquals ("Number of ghost agents", 0, dgroup.getNumGhostAgents ());

      dgroup.setNumAgents (10);

      assertEquals ("Number of agents", 10, dgroup.getNumAgents ());
      assertEquals ("Number of free agents", 10, dgroup.getNumFreeAgents ());
      assertEquals ("Number of busy agents", 0, dgroup.getNumBusyAgents ());
      assertEquals ("Number of ghost agents", 0, dgroup.getNumGhostAgents ());
   }

   public void testAvailableAgents () {
      dgroup.setNumAgents (10);
      final List<Agent> agents = dgroup.getIdleAgents ();
      assertEquals ("Number of free agents", 10, agents.size ());
      // Free agents are made unavailable, which should decrease the value
      // returned
      // by getNumFreeAgents.
      for (int i = 0; i < agents.size () / 2; i++) {
         agents.get (i).setAvailable (false);
         assertEquals ("Number of free agents", agents.size () - i - 1, dgroup
               .getNumFreeAgents ());
      }
      // Free agents are made available.
      for (int i = 0; i < agents.size () / 2; i++) {
         agents.get (i).setAvailable (true);
         assertEquals ("Number of free agents", agents.size () / 2 + i + 1,
               dgroup.getNumFreeAgents ());
      }
   }
}
