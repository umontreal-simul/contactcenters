package umontreal.iro.lecuyer.contactcenters.server;

import junit.framework.TestCase;

import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class AgentGroupTest extends TestCase {
   AgentGroup group;
   TestAgentGroupListener listener;
   PeriodChangeEvent pce;

   public AgentGroupTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      final int[] numAgents = { 0, 7, 11, 24, 45, 20, 5, 5 };
      Sim.init ();
      final ValueGenerator sgen = new ConstantValueGenerator (1, 100);
      pce = new PeriodChangeEvent (10.0, 8, 0);
      group = new AgentGroup (pce, numAgents);
      group.setContactTimeGenerator (0, sgen);
      group
            .addAgentGroupListener (listener = new TestAgentGroupListener (
                  group));
      group.setName ("Test");
      pce.init ();
      pce.start ();
      pce.setName ("Test");
      group.setKeepingEndServiceEvents (true);
      group.init ();
   }

   @Override
   public void tearDown () {
      listener = null;
      group = null;
      pce = null;
   }

   public void testNumAgents () {
      new EndSimEvent ().schedule (75);
      // From time 0 through 10, we have 7 agents
      new AssertNumAgentsEvent (group, 7).schedule (1);
      new AssertNumAgentsEvent (group, 7).schedule (3);
      // All the agents are free
      new AssertNumFreeAgentsEvent (group, 7).schedule (3.5);
      new AssertNumBusyAgentsEvent (group, 0).schedule (4.0);
      // Test the effect of setNumAgents
      new SetNumAgentsEvent (group, 8).schedule (7.5);
      new AssertNumAgentsEvent (group, 8).schedule (8.0);
      // Test the number of agents in other main periods
      new AssertNumAgentsEvent (group, 11).schedule (11);
      new AssertNumAgentsEvent (group, 5).schedule (74.0);
      Sim.start ();
   }

   public void testService () {
      new EndSimEvent ().schedule (135.0);
      // Start a service and check that the number of free
      // agents is decreased and busy agents is increased.
      // The number of agents must stay the same.
      new AssertNumFreeAgentsEvent (group, 7).schedule (2.0);
      new BeginServiceEvent (group, true).schedule (4.5);
      new AssertNumFreeAgentsEvent (group, 6).schedule (5.5);
      new AssertNumBusyAgentsEvent (group, 1).schedule (5.6);
      new AssertNumAgentsEvent (group, 7).schedule (5.55);
      // Start a second service and check that the number of
      // free and busy agents are correctly updated.
      new BeginServiceEvent (group, true).schedule (6.0);
      new AssertNumFreeAgentsEvent (group, 5).schedule (9.0);
      new AssertNumBusyAgentsEvent (group, 2).schedule (9.01);
      // Make all the remaining 5 agents busy and check that
      // all agents are busy and no agents are free.
      for (int i = 0; i < 5; i++)
         new BeginServiceEvent (group, true).schedule (9.05 + 0.05 * i);
      new AssertNumFreeAgentsEvent (group, 0).schedule (9.9);
      new AssertNumBusyAgentsEvent (group, 7).schedule (9.905);
      // Checks that serve fails if all agents are busy.
      new BeginServiceEvent (group, false).schedule (9.91);
      // Since the total number of agents increased, there should
      // be free agents now. Check that these new free agents
      // are considered by serve.
      new AssertNumFreeAgentsEvent (group, 4).schedule (10.5);
      new BeginServiceEvent (group, true).schedule (11);
      new AssertNumFreeAgentsEvent (group, 3).schedule (12);
      new AssertNumBusyAgentsEvent (group, 8).schedule (12.1);
      // Verify that there is no ghost agents
      new AssertNumGhostAgentsEvent (group, 0).schedule (12.2);
      new AssertNumGhostAgentsEvent (group, 0).schedule (13);
      // In the last period, the number of agents decreases, so
      // ghost agents can appear
      new AssertNumGhostAgentsEvent (group, 3).schedule (81.0);
      new AssertNumFreeAgentsEvent (group, 4).schedule (110.6);
      // Verify that all services terminate and every busy agents
      // become free.
      new AssertNumGhostAgentsEvent (group, 0).schedule (120.0);
      new AssertNumFreeAgentsEvent (group, 5).schedule (121);
      Sim.start ();
      // Since the EndServiceEvent and endContact/endService
      // use the same logic to terminate the service, there is
      // no need to check manual service termination.
   }

   public void testManualService () {
      group.setContactTimeGenerator (0, new ContactTimeGenerator (group));
      final ValueGenerator acgen = new ConstantValueGenerator (1,
            Double.POSITIVE_INFINITY);
      group.setAfterContactTimeGenerator (0, acgen);
      new EndSimEvent ().schedule (120);
      // One manual service with end service event scheduling,
      // one using endContact/endService.
      new BeginManualServiceEvent (group, listener, true).schedule (5);
      new BeginManualServiceEvent (group, listener, false).schedule (35);
      Sim.start ();
   }

   public void testAfterContactWork () {
      final ValueGenerator acgen = new ConstantValueGenerator (1, 50);
      group.setAfterContactTimeGenerator (0, acgen);
      new EndSimEvent ().schedule (160.0);
      new BeginServiceEvent (group, true).schedule (5.0);
      new AssertNumBusyAgentsEvent (group, 1).schedule (5.5);
      // At this time, the communication with the contact is done,
      // but after contact work reamains to be done, so the
      // agent must stay busy.
      new AssertNumBusyAgentsEvent (group, 1).schedule (106);
      new AssertNumFreeAgentsEvent (group, 4).schedule (106);
      // At this time, the busy agents becomes free.
      new AssertNumBusyAgentsEvent (group, 0).schedule (156);
      new AssertNumFreeAgentsEvent (group, 5).schedule (156);
      Sim.start ();
   }

   public void testAgentGroupListener () {
      final ValueGenerator acgen = new ConstantValueGenerator (1, 50);
      group.setAfterContactTimeGenerator (0, acgen);
      new EndSimEvent ().schedule (170.0);
      // N(t) goes from 0 to 7
      new AssertNEvent (listener, 1, 1, 0, 0, 0).schedule (0.1);
      // A first change event occurs at time 10, when the period changes
      new AssertNEvent (listener, 1, 2, 0, 0, 0).schedule (15.0);
      new BeginServiceEvent (group, true).schedule (15.5);
      // The listener should be notified when the service starts
      new AssertNEvent (listener, 1, 2, 1, 0, 0).schedule (16.0);
      new AssertNEvent (listener, 1, 3, 1, 0, 0).schedule (21.0);
      // It is also notified when the service ends
      new AssertNEvent (listener, 1, 6, 1, 1, 0).schedule (116.0);
      new AssertNEvent (listener, 1, 6, 1, 1, 1).schedule (166.0);
      Sim.start ();
   }

   static class TestAgentGroupListener implements AgentGroupListener {
      AgentGroup group;
      int nInit = 0;
      int nChanged = 0;
      int nBeginServed = 0;
      int nEndContact = 0;
      int nEndServed = 0;

      int nFree = 0;
      int nBusy = 0;

      TestAgentGroupListener (AgentGroup group) {
         this.group = group;
      }

      public void init (AgentGroup agentGroup) {
         assertEquals (group, agentGroup);
         nInit++;
         nFree = agentGroup.getNumFreeAgents ();
         nBusy = agentGroup.getNumBusyAgents ();
         assertEquals ("Number of busy agents", agentGroup
               .getEndServiceEvents ().size (), nBusy);
      }

      public void agentGroupChange (AgentGroup agentGroup) {
         assertEquals (group, agentGroup);
         nChanged++;
         nFree = agentGroup.getNumFreeAgents ();
         nBusy = agentGroup.getNumBusyAgents ();
         assertEquals ("Number of busy agents", agentGroup
               .getEndServiceEvents ().size (), nBusy);
      }

      public void beginService (EndServiceEvent ev) {
         assertEquals (group, ev.getAgentGroup ());
         nBeginServed++;
         --nFree;
         ++nBusy;
         assertEquals ("Number of free agents", ev.getAgentGroup ()
               .getNumFreeAgents (), nFree);
         assertEquals ("Number of busy agents", ev.getAgentGroup ()
               .getNumBusyAgents (), nBusy);
         assertEquals ("Number of busy agents", ev.getAgentGroup ()
               .getEndServiceEvents ().size (), nBusy);
      }

      public void endContact (EndServiceEvent ev) {
         assertTrue (ev.contactDone ());
         nEndContact++;
      }

      public void endService (EndServiceEvent ev) {
         assertEquals (0, ev.getEffectiveEndServiceType ());
         assertEquals (group, ev.getAgentGroup ());
         assertTrue (ev.afterContactDone ());
         nEndServed++;
         --nBusy;
         if (!ev.wasGhostAgent ())
            ++nFree;
         assertEquals ("Number of free agents", ev.getAgentGroup ()
               .getNumFreeAgents (), nFree);
         assertEquals ("Number of busy agents", ev.getAgentGroup ()
               .getNumBusyAgents (), nBusy);
         assertEquals ("Number of busy agents", ev.getAgentGroup ()
               .getEndServiceEvents ().size (), nBusy);
      }
   }

   static class SetNumAgentsEvent extends Event {
      private AgentGroup group;
      private int num;

      public SetNumAgentsEvent (AgentGroup group, int num) {
         this.group = group;
         this.num = num;
      }

      @Override
      public void actions () {
         group.setNumAgents (num);
      }
   }

   static class AssertNumAgentsEvent extends Event {
      private AgentGroup group;
      private int num;

      public AssertNumAgentsEvent (AgentGroup group, int num) {
         this.group = group;
         this.num = num;
      }

      @Override
      public void actions () {
         assertEquals (num, group.getNumAgents ());
      }
   }

   static class AssertNumBusyAgentsEvent extends Event {
      private AgentGroup group;
      private int num;

      public AssertNumBusyAgentsEvent (AgentGroup group, int num) {
         this.group = group;
         this.num = num;
      }

      @Override
      public void actions () {
         assertEquals (num, group.getNumBusyAgents ());
      }
   }

   static class AssertNumFreeAgentsEvent extends Event {
      private AgentGroup group;
      private int num;

      public AssertNumFreeAgentsEvent (AgentGroup group, int num) {
         this.group = group;
         this.num = num;
      }

      @Override
      public void actions () {
         assertEquals (num, group.getNumFreeAgents ());
      }
   }

   static class AssertNumGhostAgentsEvent extends Event {
      private AgentGroup group;
      private int num;

      public AssertNumGhostAgentsEvent (AgentGroup group, int num) {
         this.group = group;
         this.num = num;
      }

      @Override
      public void actions () {
         assertEquals (num, group.getNumGhostAgents ());
      }
   }

   static class AssertNEvent extends Event {
      private TestAgentGroupListener listener;
      private int numInit;
      private int numChanged;
      private int numBeginServed;
      private int numEndContact;
      private int numEndServed;

      public AssertNEvent (TestAgentGroupListener listener, int numInit,
            int numChanged, int numBeginServed, int numEndContact,
            int numEndServed) {
         this.listener = listener;
         this.numInit = numInit;
         this.numChanged = numChanged;
         this.numBeginServed = numBeginServed;
         this.numEndContact = numEndContact;
         this.numEndServed = numEndServed;
      }

      @Override
      public void actions () {
         assertEquals ("Number of free agents", listener.group
               .getNumFreeAgents (), listener.nFree);
         assertEquals ("Number of busy agents", listener.group
               .getNumBusyAgents (), listener.nBusy);
         assertEquals ("Number of calls to AgentGroupListener.init", numInit,
               listener.nInit);
         assertEquals ("Number of calls to AgentGroupListener.agentChange",
               numChanged, listener.nChanged);
         assertEquals ("Number of calls to AgentGroupListener.beginService",
               numBeginServed, listener.nBeginServed);
         assertEquals ("Number of calls to AgentGroupListener.endContact",
               numEndContact, listener.nEndContact);
         assertEquals ("Number of calls to AgentGroupListener.endService",
               numEndServed, listener.nEndServed);
      }
   }

   static class EndSimEvent extends Event {
      @Override
      public void actions () {
         Sim.stop ();
      }
   }

   static class BeginServiceEvent extends Event {
      private AgentGroup group;
      private boolean ret;

      BeginServiceEvent (AgentGroup group, boolean ret) {
         this.ret = ret;
         this.group = group;
      }

      @Override
      public void actions () {
         final Contact contact = new Contact (0);
         if (ret)
            assertNotNull (group.serve (contact));
         else {
            try {
               group.serve (contact);
            }
            catch (final IllegalStateException e) {
               return;
            }
            assertFalse (true);
         }
      }
   }

   static class BeginManualServiceEvent extends Event {
      private AgentGroup group;
      private TestAgentGroupListener listener;
      private boolean scheduleEsv;
      private EndServiceEvent esv;
      private int nf;
      private int nb;
      private int nEndContact;
      private int nEndService;

      BeginManualServiceEvent (AgentGroup group,
            TestAgentGroupListener listener, boolean scheduleEsv) {
         this.scheduleEsv = scheduleEsv;
         this.group = group;
         this.listener = listener;
      }

      @Override
      public void actions () {
         // Test if starting a service with infinite
         // time will make one free agent busy.
         final Contact contact = new Contact (0);
         nf = group.getNumFreeAgents ();
         nb = group.getNumBusyAgents ();
         esv = group.serve (contact);
         assertEquals ("Number of free agents after begin service", nf - 1,
               group.getNumFreeAgents ());
         assertEquals ("Number of busy agents after begin service", nb + 1,
               group.getNumBusyAgents ());
         --nf;
         ++nb;
         // nf and nb are updated just before the end-service event happens
         new UpdateNumEvent ().schedule (10.0);
         if (scheduleEsv)
            // Communication between the agent and the contact
            // is interrupted by a scheduled event.
            esv.schedule (10.0);
         // if scheduleEsv is false, the communication will be aborted
         // in CheckEndContactEvent.
         new CheckEndContactEvent ().schedule (10.0);
      }

      class CheckEndContactEvent extends Event {
         @Override
         public void actions () {
            new UpdateNumEvent ().schedule (15.0);
            if (scheduleEsv)
               // Schedule the end of the after-contact work.
               esv.schedule (15.0);
            else
               assertTrue (group.endContact (esv, 0));
            assertEquals ("Number of free agents after end contact", nf, group
                  .getNumFreeAgents ());
            assertEquals ("Number of busy agents after end contact", nb, group
                  .getNumBusyAgents ());
            assertEquals ("Effective contact time", 10.0, esv
                  .getEffectiveContactTime (), 1e-6);
            assertEquals ("Number of calls to AgentGroupListener.endContact",
                  nEndContact + 1, listener.nEndContact);
            assertEquals ("Number of calls to AgentGroup.endService",
                  nEndService, listener.nEndServed);
            new CheckEndServiceEvent ().schedule (15.0);
         }
      }

      class CheckEndServiceEvent extends Event {
         @Override
         public void actions () {
            if (!scheduleEsv)
               assertTrue (group.endService (esv, 0));
            assertEquals ("Number of free agents after end service", nf + 1,
                  group.getNumFreeAgents ());
            assertEquals ("Number of busy agents after end service", nb - 1,
                  group.getNumBusyAgents ());
            assertEquals ("Effective after contact time", 15.0, esv
                  .getEffectiveAfterContactTime (), 1e-6);
            assertEquals ("Number of calls to AgentGroupListener.endContact",
                  nEndContact, listener.nEndContact);
            assertEquals ("Number of calls to AgentGroup.endService",
                  nEndService + 1, listener.nEndServed);
         }
      }

      class UpdateNumEvent extends Event {
         @Override
         public void actions () {
            nf = group.getNumFreeAgents ();
            nb = group.getNumBusyAgents ();
            nEndContact = listener.nEndContact;
            nEndService = listener.nEndServed;
         }
      }
   }
}
