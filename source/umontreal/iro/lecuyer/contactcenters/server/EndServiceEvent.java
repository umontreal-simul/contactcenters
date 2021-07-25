package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactStepInfo;
import umontreal.ssj.simevents.Event;

/**
 * Represents the simulation event for a contact's
 * end of service.  It is constructed and returned by the
 * {@link AgentGroup#serve(Contact)} method and can be used to abort the service
 * of a contact, dynamically modify its service time, or
 * get information about the service.
 * The event contains scheduled as well as effective information.
 * A scheduled information is determined at the time the event
 * is scheduled.  For example, the schedule contact time is the
 * contact time which was generated at the beginning of the service.
 * An effective information is determined at the time the
 * event occurs, or the service is aborted.  It is different from
 * the scheduled information only when the service is aborted.
 */
public class EndServiceEvent extends Event implements ContactStepInfo, Cloneable {
   private AgentGroup group;
   private Contact contact;
   double beginServiceTime;
   int ecType;
   int esType;
   int eecType = -1;
   int eesType = -1;
   double contactTime;
   boolean afterContactTimeSet = false;
   double afterContactTime;
   double econtactTime;
   double eafterContactTime;
   boolean contactDone = false;
   boolean afterContactDone = false;
   boolean ghostAgent = false;
   private int expectedInitCount;

   /**
    * Constructs a new end-service event with
    * contact \texttt{contact} served by an agent in
    * group \texttt{group}, with service beginning
    * at simulation time \texttt{beginServiceTime}.
    *
    * This constructor is rarely used directly;
    * the recommended way to create end-service
    * events is to use
    * {@link AgentGroup#serve(Contact)}.
    @param group the associated agent group.
    @param contact the contact being served.
    @param beginServiceTime the time at which the service begins.
    */
   protected EndServiceEvent (AgentGroup group, Contact contact, double beginServiceTime) {
      super (contact.simulator());
      if (group == null)
         throw new NullPointerException();
      this.group = group;
      expectedInitCount = group.initCount;
      this.contact = contact;
      this.beginServiceTime = beginServiceTime;
   }

   /**
    * Returns the contact being served.
    @return the contact being served.
   */
   @Override
   public Contact getContact() {
      return contact;
   }

   /**
    * Returns the simulation time at which
    * the service started.
    @return the time of beginning of service.
   */
   public double getBeginServiceTime() {
      return beginServiceTime;
   }

   @Override
   public double getStartingTime() {
      return beginServiceTime;
   }

   @Override
   public double getEndingTime() {
      return beginServiceTime + getEffectiveContactTime();
   }

   /**
    * Returns the scheduled duration of the communication
    * with the contact.
    @return the scheduled contact time.
    */
   public double getScheduledContactTime() {
      return contactTime;
   }

   /**
    * Returns the effective contact time.
    * If the communication is not terminated, this throws
    * an {@link IllegalStateException}.
    @return the effective contact time.
    @exception IllegalStateException if the communication is not
    terminated.
    */
   public double getEffectiveContactTime() {
      if (!contactDone)
         throw new IllegalStateException ("Effective contact time not available yet");
      return econtactTime;
   }

   /**
    * Returns the scheduled after-contact time.
    * If the communication is not terminated, an
    * {@link IllegalStateException} is thrown.
    @return the scheduled after-contact time.
    @exception IllegalStateException if the communication is
    not terminated.
    */
   public double getScheduledAfterContactTime() {
      if (!contactDone)
         throw new IllegalStateException ("Scheduled after contact time not available yet");
      return afterContactTime;
   }

   /**
    * Returns the effective after-contact time.
    * If the service is not terminated, this throws an
    * {@link IllegalStateException}.
    @return the effective after-contact time.
    @exception IllegalStateException if the service is not terminated.
    */
   public double getEffectiveAfterContactTime() {
      if (!afterContactDone)
         throw new IllegalStateException ("Effective after contact time not available yet");
      return eafterContactTime;
   }

   /**
    * Returns the type of contact termination that
    * will occur when this event happens for the first time.
    * This scheduled end-contact type
    * can be overridden by using the {@link AgentGroup#endContact(EndServiceEvent,int)}
    * method.
    @return the scheduled end-contact type.
    */
   public int getScheduledEndContactType() {
      return ecType;
   }

   /**
    * Returns the type of the service termination that
    * will occur when this event happens for the second time.
    * This scheduled end-service
    * type can be overridden by using the
    * {@link AgentGroup#endService(EndServiceEvent,int)} method.
    @return the scheduled end-service type.
   */
   public int getScheduledEndServiceType() {
      if (!contactDone)
         throw new IllegalStateException ("Scheduled end service type not available yet");
      return esType;
   }

   /**
    * Returns the effective type of contact termination.
    * If the communication is not terminated, this throws an
    * {@link IllegalStateException}.
    @return the effective end-contact type.
    @exception IllegalStateException if the communication is not terminated.
    */
   public int getEffectiveEndContactType() {
      if (!contactDone)
         throw new IllegalStateException ("Effective end contact type not available yet");
      return eecType;
   }

   /**
    * Returns the effective type of the service termination.
    * If the service is not terminated, this throws an
    * {@link IllegalStateException}.
    @return the effective end-service type.
    @exception IllegalStateException if the service is not terminated.
   */
   public int getEffectiveEndServiceType() {
      if (!afterContactDone)
         throw new IllegalStateException ("Effective end service type not available yet");
      return eesType;
   }

   /**
    * Changes the type of contact termination that will
    * occur when this event happens to \texttt{ecType}.
    * If the communication is terminated, this throws an
    * {@link IllegalStateException}.
    @param ecType the new end-contact type.
    @exception IllegalStateException if the communication is terminated.
    */
   public void setScheduledEndContactType (int ecType) {
      if (contactDone)
         throw new IllegalStateException ("End of contact already happened");
      this.ecType = ecType;
   }

   /**
    * Changes the type of service termination that will
    * occur when this event happens to \texttt{esType}.
    * If the service is terminated, this throws an
    * {@link IllegalStateException}.
    @param esType the new end-service type.
    @exception IllegalStateException if the service is terminated.
    */
   public void setScheduledEndServiceType (int esType) {
      if (afterContactDone)
         throw new IllegalStateException ("End of service already happened");
      this.esType = esType;
   }

   /**
    * Returns the agent group containing the
    * agent serving the contact.
    @return the agent group serving the contact.
   */
   public AgentGroup getAgentGroup() {
      return group;
   }

   /**
    * Terminates the communication part of the service
    * represented by this event, with end-contact type
    * \texttt{ecType}, and
    * returns \texttt{true} if and only
    * if the communication part was terminated
    * successfully.
    * This method calls
    * {@link #getAgentGroup()}
    * {@link AgentGroup#endContact(EndServiceEvent,int)},
    * and returns the result.
    * @param ecType1 the end-contact type.
    * @return the success indicator of the operation.
    */
   public boolean endContact (int ecType1) {
      return group.endContact (this, ecType1);
   }

   /**
    * Terminates the after-contact part of the service
    * represented by this event, with end-service type
    * \texttt{esType}, and
    * returns \texttt{true} if and only
    * if the after-contact part was terminated
    * successfully.
    * This method calls
    * {@link #getAgentGroup()}
    * {@link AgentGroup#endService(EndServiceEvent,int)},
    * and returns the result.
    * @param esType1 the end-service type.
    * @return the success indicator of the operation.
    */
   public boolean endService (int esType1) {
      return group.endService (this, esType1);
   }

   /**
    * Determines if the communication is finished between
    * the contact and the agent.
    @return \texttt{true} if the contact was served, \texttt{false} otherwise.
    */
   public boolean contactDone() {
      return contactDone;
   }

   /**
    * Determines if the after-contact work or service is terminated by
    * the agent.
    @return \texttt{true} if the after-contact work is done, \texttt{false} otherwise.
    */
   public boolean afterContactDone() {
      return afterContactDone;
   }

   /**
    * Determines if the agent ending the service
    * of the contact disappears after
    * the service is completed.
    @return the ghost agent status.
    */
   public boolean wasGhostAgent() {
      return ghostAgent;
   }

   /**
    * Determines if this event is obsolete.
    * When calling {@link AgentGroup#init()}, some end-service
    * events might still be in the simulator's event list.
    * Since this agent group does not store every
    * scheduled end-service event by default, one must
    * use this method in {@link #actions()} to test
    * if this event is obsolete.  If that returns \texttt{true},
    * one should return immediately.
    * @return \texttt{true} for an obsolete event,
    * \texttt{false} otherwise.
    */
   public boolean isObsolete() {
      return expectedInitCount != group.initCount;
   }

   @Override
   public void schedule (double delay) {
      if (contactDone && afterContactDone)
         throw new IllegalStateException
         ("Obsolete end service events must not be scheduled");
      super.schedule (delay);
   }

   @Override
   public void actions() {
      if (isObsolete())
         return;
      if (!contactDone)
         group.completeContact (ecType, this, false);
      else if (!afterContactDone)
         group.completeService (esType, this, false);
      else
         throw new IllegalStateException
            ("Obsolete end service events must not be scheduled " + toString());
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Agent group: ").append
         (ContactCenter.toShortString (group));
      sb.append (", served contact: ").append
         (ContactCenter.toShortString (contact));
      sb.append (", begin service time: ").append
         (beginServiceTime);

      if (!contactDone || contactTime != econtactTime)
         sb.append (", scheduled contact time: ").append
            (contactTime);
      if (!contactDone || ecType != eecType)
         sb.append (", scheduled end-contact type: ").append
            (ecType);
      if (contactDone) {
         sb.append (", effective contact time: ").append
            (econtactTime);
         sb.append (", effective end-contact type: ").append
            (eecType);

         if (!afterContactDone || afterContactTime != eafterContactTime)
            sb.append (", scheduled after-contact time: ").append
               (afterContactTime);
         if (!afterContactDone || esType != eesType)
            sb.append (", scheduled end-service type: ").append
               (esType);
         if (afterContactDone) {
            sb.append (", effective after-contact time: ").append
               (eafterContactTime);
            sb.append (", effective end-service type: ").append
               (eesType);
         }
         else
            sb.append (", still in after-contact work");
      }
      else
         sb.append (", still in communication");
      sb.append (']');
      return sb.toString();
   }

   /**
    * Returns a copy of this event.
    * This method clones every field of the event, except the
    * agent group which is not cloneable.
    */
   @Override
   public EndServiceEvent clone() {
      return clone (contact.clone());
   }

   /**
    * Similar to {@link #clone()}, but initializes the contact
    * of the cloned event with \texttt{clonedContact} instead of
    * a clone of the contact returned by {@link #getContact()}.
    * This method can be useful when cloning a contact \texttt{c}
    * for which \texttt{c.getSteps()} returns a non-empty
    * list containing end-service events. In that case, the contact associated
    *  with the events included in \texttt{c.getSteps()} must
    * correspond to \texttt{c} rather than clones of \texttt{c}.
    */
   @Override
   public EndServiceEvent clone (Contact clonedContact) {
      EndServiceEvent cpy;
      try {
         cpy = (EndServiceEvent)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
      cpy.contact = clonedContact;
      //cpy.expectedInitCount = group.initCount - 1;
      return cpy;
   }
}
