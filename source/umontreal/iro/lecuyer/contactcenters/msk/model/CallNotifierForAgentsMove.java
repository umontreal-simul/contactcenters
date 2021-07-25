package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.dialer.AgentsMoveDialerPolicy;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.stat.mperiods.SumMatrixSW;

/**
 * Exited-contact listener used to update the state of the
 * \texttt{AGENTSMOVE} dialer's policy during the simulation.
 * This listener collects statistics about exiting calls to
 * get estimates of the service level in a time window, which
 * is used to determine if the dialer performs
 * inbound-to-outbound, or outbound-to-inbound moves.
 * After this listener is constructed, it should be registered
 * with the router.
 */
public class CallNotifierForAgentsMove implements ExitedContactListener {
   private final DialerManager dialerManager;
   private SumMatrixSW inServedG;
   private SumMatrixSW inServed;
   private SumMatrixSW inAbandonedB;
   private SumMatrixSW inBlocked;

   /**
    * Constructs a new call notifier for
    * the dialer manager \texttt{dialerManager}.
    * @param dialerManager the associated dialer manager.
    */
   public CallNotifierForAgentsMove (DialerManager dialerManager) {
      this.dialerManager = dialerManager;
      final int numCheckedPeriods = this.dialerManager.getNumCheckedPeriods ();
      inAbandonedB = new SumMatrixSW (1, numCheckedPeriods);
      inBlocked = new SumMatrixSW (1, numCheckedPeriods);
      inServed = new SumMatrixSW (1, numCheckedPeriods);
      inServedG = new SumMatrixSW (1, numCheckedPeriods);
   }
   
   public void blocked (Router router, Contact contact, int bType) {
      final CallCenter cc = this.dialerManager.getCallCenter ();
      if (contact.getTypeId () < cc.getNumInContactTypes ()) {
         final int tp = getPeriod ();
         inBlocked.add (0, tp, 1);
         inAbandonedB.add (0, tp, 0);
         inServed.add (0, tp, 0);
         inServedG.add (0, tp, 0);
      }
      checkSL ();
   }

   public void dequeued (Router router, DequeueEvent ev) {
      final Contact contact = ev.getContact ();
      final CallCenter cc = this.dialerManager.getCallCenter ();
      if (contact.getTypeId () < cc.getNumInContactTypes ()) {
         final ServiceLevelParamReadHelper slp = cc
               .getServiceLevelParams (this.dialerManager.getServiceLevelIndex ());
         final double awt = slp.getAwtDefault (cc.getNumInContactTypes (), cc
               .getNumMainPeriods ());
         final int tp = getPeriod ();
         inAbandonedB
               .add (0, tp, contact.getTotalQueueTime () > awt ? 1 : 0);
         inBlocked.add (0, tp, 0);
         inServed.add (0, tp, 0);
         inServedG.add (0, tp, 0);
      }
      checkSL ();
   }

   public void served (Router router, EndServiceEvent ev) {
      final Contact contact = ev.getContact ();
      final CallCenter cc = this.dialerManager.getCallCenter ();
      if (contact.getTypeId () < cc.getNumInContactTypes ()) {
         final ServiceLevelParamReadHelper slp = cc
               .getServiceLevelParams (this.dialerManager.getServiceLevelIndex ());
         final double awt = slp.getAwtDefault (cc.getNumInContactTypes (), cc
               .getNumMainPeriods ());
         final int tp = getPeriod ();
         inServedG.add (0, tp, contact.getTotalQueueTime () <= awt ? 1 : 0);
         inServed.add (0, tp, 1);
         inBlocked.add (0, tp, 0);
         inAbandonedB.add (0, tp, 0);
      }
      checkSL ();
   }

   public void init() {
      inAbandonedB.init ();
      inBlocked.init ();
      inServed.init ();
      inServedG.init ();
   }

   private final int getPeriod () {
      final double simTime = dialerManager.getCallCenter ().getPeriodChangeEvent ().simulator ().time ();
      return (int) (simTime / this.dialerManager.getCheckedPeriodDuration());
   }

   private void checkSL () {
      final Dialer dialer = this.dialerManager.getDialer ();
      if (!(dialer.getDialerPolicy () instanceof AgentsMoveDialerPolicy))
         return;
      if (!dialer.isStarted ())
         return;
      // Tally slTally = new Tally();
      double served = 0, servedG = 0, abandonedB = 0, blocked = 0;
      for (int tp = 0; tp < inServed.getNumPeriods (); tp++) {
         served += inServed.getMeasure (0, tp);
         servedG += inServedG.getMeasure (0, tp);
         abandonedB += inAbandonedB.getMeasure (0, tp);
         blocked += inBlocked.getMeasure (0, tp);
         // double slp = inServedG.getMeasure (0, p) /
         // (inServed.getMeasure (0, p) + inAbandonedB.getMeasure (0, p) +
         // inBlocked.getMeasure (0, p));
         // if (Double.isNaN (slp))
         // slp = 1;
         // slTally.add (slp);
      }
      double sl = servedG / (served + abandonedB + blocked);
      if (Double.isNaN (sl))
         sl = 1.0;
      assert sl >= 0 && sl <= 1 : "Invalid service level " + sl;
      final AgentsMoveDialerPolicy pol = (AgentsMoveDialerPolicy) dialer.getDialerPolicy ();
      if (sl >= this.dialerManager.getSlInboundThresh ())
         pol.stopOutboundToInbound ();
      if (sl <= this.dialerManager.getSlOutboundThresh ())
         pol.stopInboundToOutbound ();

      if (sl < this.dialerManager.getSlInboundThresh ())
         pol.startOutboundToInbound ();
      if (sl > this.dialerManager.getSlOutboundThresh ())
         pol.startInboundToOutbound ();

      // AgentsMoveDialerPolicy.AgentGroupInfo[] groupInfo =
      // pol.getAgentGroupInfo ();
      // int[] numAgents = new int[groupInfo.length];
      // int[] vstaffing = new int[groupInfo.length];
      // int p = cc.getPeriodChangeEvent ().getCurrentMainPeriod ();
      // for (int j = 0; j < groupInfo.length; j++) {
      // for (AgentGroup grp : groupInfo[j].getInboundGroups ()) {
      // numAgents[j] += grp.getNumAgents ();
      // int i = grp.getId ();
      // int staff = cc.getAgentGroupManager (i).getStaffing (p);
      // vstaffing[j] += staff;
      // }
      // for (AgentGroup grp : groupInfo[j].getOutboundGroups ()) {
      // numAgents[j] += grp.getNumAgents ();
      // int i = grp.getId ();
      // int staff = cc.getAgentGroupManager (i).getStaffing (p);
      // vstaffing[j] += staff;
      // }
      // }
      //      
      // for (int j = 0; j < numAgents.length; j++) {
      // if (numAgents[j] != vstaffing[j]) {
      // System.out.printf ("Simulation time: %f%n", Sim.time ());
      // System.out.println ("Current number of agents in virtual groups: " +
      // Arrays.toString (numAgents));
      // System.out.println ("Supposed number of agents in virtual group: " +
      // Arrays.toString (vstaffing));
      // //throw new AssertionError();
      // }
      // }
   }
}
