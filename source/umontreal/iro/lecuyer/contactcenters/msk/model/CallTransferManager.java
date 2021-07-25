package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.ContactTimeGenerator;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.simevents.Event;

/**
 * Implements the necessary logic for call transfer from
 * primary to secondary agents.
 */
public class CallTransferManager {
   private CallCenter cc;
   private final AgentTransferListener agentTransferListener = new AgentTransferListener ();
   private final QueueTransferListener queueTransferListener = new QueueTransferListener ();

   public CallTransferManager (CallCenter cc) {
      this.cc = cc;
      for (final AgentGroup grp : cc.getAgentGroups ()) {
         grp.addAgentGroupListener (agentTransferListener);
         grp.setContactTimeGenerator (0, new STimeGen (grp));
      }
      for (final WaitingQueue queue : cc.getWaitingQueues ())
         queue.addWaitingQueueListener (queueTransferListener);
   }

   /**
    * This contact time generator gives
    * an infinite service time when call transfer
    * is supported for a given call.
    * When the service time is infinite,
    * the program is free to end the service at
    * any needed time.
    */
   private class STimeGen extends ContactTimeGenerator {
      public STimeGen (AgentGroup group, double[] mult) {
         super (group, mult);
      }

      public STimeGen (AgentGroup group) {
         super (group);
      }

      @Override
      public double nextDouble (Contact contact) {
         final int k = contact.getTypeId ();
         if (cc.getCallFactory (k).isCallTransferSupported ())
            // Manual service termination
            return Double.POSITIVE_INFINITY;
         else
            return super.nextDouble (contact);
      }
   }

   private class AgentTransferListener implements AgentGroupListener {
      public void agentGroupChange (AgentGroup group) {}

      public void beginService (EndServiceEvent ev) {
         if (!(ev.getContact () instanceof Call))
            return;
         final Call call = (Call) ev.getContact ();
         final int i = ev.getAgentGroup ().getId ();
         final int k = call.getTypeId ();
         if (ev.time () < 0 && cc.getCallFactory (k).isCallTransferSupported ()) {
            // When call transfer is possible, an infinite service time is generated,
            // and the end-service event created by AgentGroup.serve is not scheduled
            // automatically, so it has a negative time.
            // This listener then captures such events,
            // schedules them, or wraps them into
            // auxiliary events if transfer is done.
            final int mp = cc.getPeriodChangeEvent ().getMainPeriod (call.getArrivalPeriod ());
            final double u = call.getUTransfer ();
            double t = call.getDefaultContactTime (i);
            if (t < 0)
               t = 0;
            assert !ev.contactDone ();
            if (!Double.isInfinite (t))
               if (u < cc.getCallFactory (k).getProbTransfer (i, mp)) {
                  // Transfer will be done, so schedule the wrapper event
                  final double m = cc.getCallFactory (k).getServiceTimesMultTransfer (i, mp);
                  new CallTransferEvent (ev).schedule (t*m);
               }
               else
                  // No transfer, so schedule the ordinary event.
                  ev.schedule (t);
         }

         // Allows the primary agent to be free after the conference with
         // the secondary agent
         final EndServiceEvent evTransfer = call.getPrimaryEndServiceEvent ();
         if (evTransfer != null) {
            // evTransfer is the end-service event associated with the primary agent
            assert !evTransfer.contactDone () : "Primary agent already freed at the time the secondary agent is found, end-service event " + evTransfer + ", time=" + evTransfer.simulator ().time ();
            final double t = call.getConferenceTimes().getServiceTime (i);
            if (t > 0)
               // Schedule service termination by the primary agent
               // after the conference time.
               evTransfer.schedule (t);
            else
               // End the service by the primary agent.
               evTransfer.endContact (0);
         }
      }

      public void endContact (EndServiceEvent ev) {}

      public void endService (EndServiceEvent ev) {}

      public void init (AgentGroup group) {}
   }

   private class QueueTransferListener implements WaitingQueueListener {
      public void dequeued (DequeueEvent ev) {
         // End the service by the primary agent if the call was
         // transferred and abandoned while waiting for a secondary agent.
         if (ev.getEffectiveDequeueType () == Router.DEQUEUETYPE_BEGINSERVICE)
            return;
         if (!(ev.getContact () instanceof Call))
            return;
         final Call call = (Call) ev.getContact ();
         final EndServiceEvent evTransfer = call.getPrimaryEndServiceEvent ();
         if (evTransfer != null)
            evTransfer.endContact (0);
      }

      public void enqueued (DequeueEvent ev) {}

      public void init (WaitingQueue queue) {}
   }

   private class CallTransferEvent extends Event {
      // ev is the end-service event with the primary agent.
      private EndServiceEvent ev;

      public CallTransferEvent (EndServiceEvent ev) {
         super (ev.simulator ());
         this.ev = ev;
      }

      @Override
      public void actions () {
         // The call is transferred to a new agent.
         assert !ev.contactDone () : "Agent already freed at the time the transfer should occur, for end-service event " + ev;
         assert ev.time () < 0 : "Unexpectedly scheduled event " + ev;
         final Call call = (Call) ev.getContact ();
         final int k = call.getTypeId ();
         final int i = ev.getAgentGroup ().getId ();
         // Create the object representing the transferred call
         final Call call2;
         try {
            call2 = (Call) cc.getCallFactory (k).getTransferTargetFactory ().newInstance ();
         }
         catch (final IllegalStateException ise) {
            ev.endContact (0);
            return;
         }
         // Set arrival time and arrival period to the original values.
         //call2.setArrivalTime (call.getArrivalTime ());
         //call2.setArrivalPeriod (call.getArrivalPeriod ());
         
         // Wait for a transfer delay
         final double tr = call.getTransferTimes ().getServiceTime (i);
         if (tr <= 0)
            finishTransfer (call, call2);
         else if (!Double.isInfinite (tr))
            // Wait for a delay before transfer occurs.
            new FinishTransferEvent (call, call2).schedule (tr);
      }
      
      private void finishTransfer (Call call, Call call2) {
         // Decide (randomly) if the primary agent must wait
         final int k = call.getTypeId();
         final int k2 = call2.getTypeId();
         final int i = ev.getAgentGroup ().getId ();
         final int mp = cc.getPeriodChangeEvent ().getMainPeriod (call.getArrivalPeriod ());
         final double u = call.getUTransferWait ();
         if (u < cc.getCallFactory (k).getProbTransferWait (i, mp)) {
            // Keep a trace of the primary agent in call2, and
            // generate the conference times.
            call2.setPrimaryEndServiceEvent (ev);
            cc.getCallFactory (k2).setConferenceTimes (call2);
         }
         else {
            ev.endContact (0);
            cc.getCallFactory (k2).setPreServiceTimesNoConf (call2);
         }
         cc.getRouter ().newContact (call2);
      }
      
      private class FinishTransferEvent extends Event {
         private Call call;
         private Call call2;
         
         public FinishTransferEvent (Call call, Call call2) {
            super (call.simulator ());
            this.call = call;
            this.call2 = call2;
         }

         @Override
         public void actions () {
            finishTransfer (call, call2);
         }
      }
   }
}
