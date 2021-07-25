package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.simevents.Event;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This event occurs when the dialer
 * reached or failed to reach a called person.
 * Such events are scheduled by the {@link Dialer#dial}
 * method if generated dial delays are greater than zero.
 */
public class DialerActionEvent extends Event implements Cloneable {
   private Dialer dialer;
   private Contact contact;
   private boolean success;
   private int expectedInitCount;

   /**
    * Constructs a new dialer action event for contact \texttt{contact}
    * with success indicator \texttt{success}.
    * When the event occurs, if \texttt{success} is \texttt{true},
    * the contact is notified to
    * new-contact listeners for right-party connect.
    * When \texttt{success} is \texttt{false}, a failed contact
    * is notified to the appropriate new-contact listeners.
    * @param dialer the associated dialer.
    @param contact the contact object representing the call being tried.
    @param success the success indicator.
    */
   public DialerActionEvent (Dialer dialer, Contact contact, boolean success) {
      super (dialer.simulator());
      this.dialer = dialer;
      expectedInitCount = dialer.initCount;
      this.contact = contact;
      this.success = success;
   }
   
   @Override
   public boolean cancel() {
      final boolean res = super.cancel();
      if (dialer.actionEvents != null)
         dialer.actionEvents.remove (this);
      --dialer.numActionEvents;
      --dialer.numActionEventsTypes[contact.getTypeId ()];
      return res;
   }
   
   private void addActionEvent() {
      ++dialer.numActionEvents;
      final int k = contact.getTypeId ();
      if (k >= dialer.numActionEventsTypes.length)
         dialer.numActionEventsTypes = ArrayUtil.resizeArray (dialer.numActionEventsTypes, k + 1);
      ++dialer.numActionEventsTypes[k];
   }
   
   @Override
   public void schedule (double delay) {
      super.schedule (delay);
      if (dialer.actionEvents != null)
         dialer.actionEvents.add (this);
      addActionEvent ();
   }
   
   @Override
   public void scheduleBefore (Event event) {
      super.scheduleBefore (event);
      if (dialer.actionEvents != null)
         dialer.actionEvents.add (this);
      addActionEvent ();
   }
   
   @Override
   public void scheduleAfter (Event event) {
      super.scheduleAfter (event);
      if (dialer.actionEvents != null)
         dialer.actionEvents.add (this);
      addActionEvent ();
   }
   
   @Override
   public void scheduleNext() {
      super.scheduleNext();
      if (dialer.actionEvents != null)
         dialer.actionEvents.add (this);
      addActionEvent ();
   }
   
   /**
    * Returns the contact object representing
    * the called person.
    @return the concerned contact.
    */
   public Contact getContact() {
      return contact;
   }

   /**
    * Returns the dialer this event
    * is attached to.
    @return the attached dialer.
    */
   public Dialer getDialer() {
      return dialer;
   }

   /**
    * Returns \texttt{true} if a right party connect will occur
    * at the time of this event.
    * Otherwise, returns \texttt{false}.
    @return the success indicator.
    */
   public boolean isSuccessful() {
      return success;
   }
   
   /**
    * Determines if this event is obsolete.
    * When calling {@link Dialer#init}, some action
    * events might still be in the simulator's event list.
    * One must
    * use this method in {@link #actions} to test
    * if this event is obsolete.  If that returns \texttt{true},
    * one should return immediately.
    * @return \texttt{true} for an obsolete event,
    * \texttt{false} otherwise.
    */
   public boolean isObsolete() {
      return expectedInitCount != dialer.initCount;
   }

   @Override
   public void actions() {
      if (isObsolete())
         return;
      if (dialer.actionEvents != null)
         dialer.actionEvents.remove (this);
      --dialer.numActionEvents;
      --dialer.numActionEventsTypes[contact.getTypeId ()];
      dialer.notifyListeners (contact, success);
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("contact: ").append
         (ContactCenter.toShortString (contact));
      if (success)
         sb.append (", successful");
      else
         sb.append (", failed");
      sb.append ("dialer: ").append
         (ContactCenter.toShortString (dialer));
      sb.append (']');
      return sb.toString();
   }
   
   @Override
   public DialerActionEvent clone() {
      DialerActionEvent cpy;
      try {
         cpy = (DialerActionEvent)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
      cpy.expectedInitCount = dialer.initCount - 1;
      return cpy;
   }
}
