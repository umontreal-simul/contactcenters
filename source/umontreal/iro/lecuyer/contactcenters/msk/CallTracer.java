package umontreal.iro.lecuyer.contactcenters.msk;

import umontreal.iro.lecuyer.contactcenters.app.trace.ContactTrace;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

/**
 * Observer sending any notified call to a contact trace facility.
 * An object of this class is constructed using a {@link ContactTrace}
 * instance as well as a {@link SimLogic} object.
 * Each time a call exits the simulated system, a line
 * is written to the associated trace, using information
 * obtained from the contact object, and the simulation logic. 
 */
public class CallTracer implements ExitedContactListener, NewContactListener {
   private SimLogic simLogic;
   private ContactTrace trace;
   
   /**
    * Creates a new call tracer from the given simulation logic
    * and trace.
    * @param simLogic the simulation logic used to get
    * step and period for the trace.
    * @param trace the object representing the call-by-call trace
    * facility.
    */
   public CallTracer (SimLogic simLogic, ContactTrace trace) {
      this.simLogic = simLogic;
      this.trace = trace;
   }
   
   /**
    * Returns the simulation logic associated with this
    * call tracer.
    */
   public SimLogic getSimLogic() {
      return simLogic;
   }
   
   /**
    * Returns the associated facility for contact-by-contact
    * trace.
    */
   public ContactTrace getContactTrace() {
      return trace;
   }
   
   public void blocked (Router router, Contact contact, int type) {
      final int itr = simLogic.getCompletedSteps ();
      final int k = contact.getTypeId ();
      final int p = simLogic.getStatPeriod (contact);
      final double a = contact.getArrivalTime ();
      trace.writeLine (itr, k, p, a, Double.NaN, ContactTrace.OUTCOME_BLOCKED,
            -1, Double.NaN);
   }

   public void dequeued (Router router, DequeueEvent ev) {
      final Contact contact = ev.getContact ();
      final int itr = simLogic.getCompletedSteps ();
      final int k = contact.getTypeId ();
      final int p = simLogic.getStatPeriod (contact);
      final double a = contact.getArrivalTime ();
      final double q = contact.getTotalQueueTime ();
      trace.writeLine (itr, k, p, a, q, ContactTrace.OUTCOME_ABANDONED, -1,
            Double.NaN);
   }

   public void served (Router router, EndServiceEvent ev) {
      final Contact contact = ev.getContact ();
      final int itr = simLogic.getCompletedSteps ();
      final int k = contact.getTypeId ();
      final int p = simLogic.getStatPeriod (contact);
      final double a = contact.getArrivalTime ();
      final double q = contact.getTotalQueueTime ();
      final int i = contact.getLastAgentGroup ().getId ();
      final double s = contact.getTotalServiceTime ();
      trace.writeLine (itr, k, p, a, q, ContactTrace.OUTCOME_SERVED, i, s);
   }

   public void newContact (Contact contact) {
      final int itr = simLogic.getCompletedSteps ();
      final int k = contact.getTypeId ();
      final int p = simLogic.getStatPeriod (contact);
      final double a = contact.getArrivalTime ();
      trace.writeLine (itr, k, p, a, Double.NaN, ContactTrace.OUTCOME_FAILED,
            -1, Double.NaN);
   }

   /**
    * Registers this call tracer with the model
    * associated with the simulation logic returned
    * by {@link #getSimLogic()}.
    * After this method is called, this listener is notified
    * about every contact leaving the simulated system as
    * well as any failed outbound call.
    */
   public void register () {
      CallCenter model = simLogic.getCallCenter ();
      model.getRouter ().addExitedContactListener (this);
      for (Dialer dialer : model.getDialers ())
         if (dialer != null)
            dialer.addFailListener (this);
   }

   /**
    * Unregisters this call tracer with the model
    * associated with the simulation logic returned
    * by {@link #getSimLogic()}.
    */
   public void unregister () {
      CallCenter model = simLogic.getCallCenter ();
      model.getRouter ().removeExitedContactListener (this);
      for (Dialer dialer : model.getDialers ())
         if (dialer != null)
            dialer.removeFailListener (this);
   }
}
