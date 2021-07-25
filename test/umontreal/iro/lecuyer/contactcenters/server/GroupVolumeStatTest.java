package umontreal.iro.lecuyer.contactcenters.server;

import junit.framework.TestCase;

import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;

import umontreal.ssj.simevents.Sim;

public class GroupVolumeStatTest extends TestCase {
   PeriodChangeEvent pce;
   AgentGroup sgroup;
   AgentGroup nsgroup;
   GroupVolumeStat svstat;
   GroupVolumeStat nsvstat;

   public GroupVolumeStatTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      final int[] numAgents = { 0, 7, 11, 24, 45, 20, 5, 5 };
      Sim.init ();
      final ValueGenerator sgen = new ConstantValueGenerator (1, 10);
      pce = new PeriodChangeEvent (10.0, 8, 0);
      sgroup = new AgentGroup (numAgents[1]);
      nsgroup = new AgentGroup (pce, numAgents);
      sgroup.setContactTimeGenerator (0, sgen);
      nsgroup.setContactTimeGenerator (0, sgen);
      sgroup.setName ("Test");
      nsgroup.setName ("Test");
      pce.init ();
      pce.start ();
      pce.setName ("Test");
      sgroup.init ();
      nsgroup.init ();
      svstat = new GroupVolumeStat (sgroup);
      nsvstat = new GroupVolumeStat (nsgroup);
   }

   @Override
   public void tearDown () {
      pce = null;
      sgroup = null;
      nsgroup = null;
      svstat = null;
      nsvstat = null;
   }

   public void testStationary () {
      new AgentGroupTest.EndSimEvent ().schedule (70.0);
      new AgentGroupTest.BeginServiceEvent (sgroup, true).schedule (5.0);
      new AgentGroupTest.BeginServiceEvent (sgroup, true).schedule (6.5);
      Sim.start ();
      assertEquals ("Service volume", 20.0, svstat.getStatNumBusyAgents().sum(), 1e-6);
      assertEquals ("Idle volume", 7 * 70.0 - 20.0, svstat.getStatNumIdleAgents().sum(),
            1e-6);
      assertEquals ("Total volume", 7 * 70.0, svstat.getStatNumAgents().sum() + svstat.getStatNumGhostAgents().sum(), 1e-6);
   }

   public void testNonStationary () {
      new AgentGroupTest.EndSimEvent ().schedule (70.0);
      new AgentGroupTest.BeginServiceEvent (nsgroup, true).schedule (5.0);
      new AgentGroupTest.BeginServiceEvent (nsgroup, true).schedule (6.5);
      Sim.start ();
      final double total = (7 + 11 + 24 + 45 + 20) * 10 + 5 * 20;
      assertEquals ("Service volume", 20.0, nsvstat.getStatNumBusyAgents().sum(), 1e-6);
      assertEquals ("Total volume", total, nsvstat.getStatNumAgents().sum() + nsvstat.getStatNumGhostAgents().sum(), 1e-6);
      assertEquals ("Idle volume", total - 20.0, nsvstat.getStatNumIdleAgents().sum(),
            1e-6);
   }

   public void testNonStationaryGhosts () {
      new AgentGroupTest.EndSimEvent ().schedule (70.0);
      // When the agents changes to 5 at time 50, there will be 6 busy
      // agents, so 0 free agent and 1 ghost agent. This will affect
      // the total volume.
      for (int i = 0; i < 6; i++)
         new AgentGroupTest.BeginServiceEvent (nsgroup, true).schedule (45.0);
      Sim.start ();
      final double total = (7 + 11 + 24 + 45 + 20) * 10 + 5 * 20 + 5;
      assertEquals ("Service volume", 60.0, nsvstat.getStatNumBusyAgents().sum(), 1e-6);
      assertEquals ("Idle volume", total - 60.0, nsvstat.getStatNumIdleAgents().sum(),
            1e-6);
      assertEquals ("Total volume", total, nsvstat.getStatNumAgents().sum() + nsvstat.getStatNumGhostAgents().sum(), 1e-6);
   }
}
