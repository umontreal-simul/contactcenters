package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents the information needed to scheduled
 * a dialer action event.
 */
public class DialerActionState {
   private Contact contact;
   private boolean success;
   private double dialEndTime;
   
   /**
    * Constructs a new dialer action event state object
    * for a contact \texttt{contact}.
    * The \texttt{success} flag gives the success
    * indicator when the dial-up is finished
    * at simulation time \texttt{dialEndTime}.
    * @param contact the contact being dialed.
    * @param success the success indicator.
    * @param dialEndTime the simulation time of success or failure.
    */
   public DialerActionState (Contact contact, boolean success, double dialEndTime) {
      this.contact = contact.clone();
      this.success = success;
      this.dialEndTime = dialEndTime;
   }
   
   /**
    * Constructs a new dialer action state object
    * from the dialer action event \texttt{ev}.
    * @param ev the dialer action event to extract information from.
    */
   public DialerActionState (DialerActionEvent ev) {
      this (ev.getContact(), ev.isSuccessful(), ev.time());
   }
   
   /**
    * Returns the contact being dialed.
    * @return the contact being dialed.
    */
   public Contact getContact() {
      return contact;
   }
   
   /**
    * Returns the success indicator of the dial.
    * @return the success indicator of the dial.
    */
   public boolean isSuccessful() {
      return success;
   }
   
   /**
    * Returns the time at which success or failure
    * will occur.
    * @return the success or failure time.
    */
   public double getDialEndTime() {
      return dialEndTime;
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Dialed contact: ").append (contact);
      if (success)
         sb.append (", right party connect");
      else
         sb.append (", failed contact");
      sb.append (" occuring at time ").append (dialEndTime);
      sb.append (']');
      return sb.toString();
   }
}
