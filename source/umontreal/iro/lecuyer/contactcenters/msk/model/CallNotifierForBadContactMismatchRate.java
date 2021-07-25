package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.dialer.BadContactMismatchRatesDialerPolicy;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

/**
 * Exited-contact and new-contact listeners used
 * to update the state of the \texttt{BADCONTACTMISMATCHRATE}
 * dialer's policy.
 * This listener calls
 * {@link BadContactMismatchRatesDialerPolicy#notifyInboundContact(Contact,boolean)}, and
 * {@link BadContactMismatchRatesDialerPolicy#notifyOutboundContact(Contact,boolean)}
 * methods when failed contacts are notified, or when other contacts
 * exit.
 * This listener should be registered with the router and with
 * the dialer to receive failed calls.
 */
public class CallNotifierForBadContactMismatchRate implements ExitedContactListener,
      NewContactListener {
   private final DialerManager dialerManager;

   /**
    * Constructs a new call notifier for the dialer manager
    * \texttt{dialerManager}.
    * @param dialerManager the associated dialer manager.
    */
   public CallNotifierForBadContactMismatchRate (DialerManager dialerManager) {
      this.dialerManager = dialerManager;
   }

   public void newContact (Contact contact) {
      notifyOutCall (contact);
   }

   public void blocked (Router router, Contact contact, int bType) {
      notifyOutCall (contact);
      notifyInCall (contact);
   }

   public void dequeued (Router router, DequeueEvent ev) {
      final Contact contact = ev.getContact ();
      notifyOutCall (contact);
      notifyInCall (contact);
   }

   public void served (Router router, EndServiceEvent ev) {
      final Contact contact = ev.getContact ();
      notifyOutCall (contact);
      notifyInCall (contact);
   }

   private void notifyInCall (Contact contact) {
      final Dialer dialer = dialerManager.getDialer ();
      if (!(dialer.getDialerPolicy () instanceof BadContactMismatchRatesDialerPolicy))
         return;
      final CallCenter cc = dialerManager.getCallCenter ();
      final int type = contact.getTypeId ();
      if (type >= cc.getNumInContactTypes ())
         return;
      final double qt = contact.getTotalQueueTime ();
      final int mp = cc.getAwtPeriod (contact);

      final BadContactMismatchRatesDialerPolicy pol = (BadContactMismatchRatesDialerPolicy) dialer.getDialerPolicy ();
      final ServiceLevelParamReadHelper slp = cc.getServiceLevelParams (dialerManager.getServiceLevelIndex ());
      final double skp = slp.getAwt (type, mp, cc.getDefaultUnit ());
      pol.notifyInboundContact (contact, qt >= skp);
   }

   private void notifyOutCall (Contact contact) {
      final Dialer dialer = dialerManager.getDialer ();
      if (!(dialer.getDialerPolicy () instanceof BadContactMismatchRatesDialerPolicy))
         return;
      final int k = contact.getTypeId () - dialerManager.getCallCenter().getNumInContactTypes ();
      if (k < 0)
         return;
      final boolean m = contact.getTotalQueueTime () > 0;
      final BadContactMismatchRatesDialerPolicy pol = (BadContactMismatchRatesDialerPolicy) dialer.getDialerPolicy ();
      pol.notifyOutboundContact (contact, m);
   }
}
