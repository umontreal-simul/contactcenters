package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.simevents.Event;

/**
 * Represents an event that restarts the service of
 * a contact.
 * Service can be restarted in its communication
 * phase, or in the after-contact work.
 * This is used for state restoration of an
 * agent group.
 */
public class StartServiceEvent extends Event {
   private AgentGroup targetGroup;
   private Agent targetAgent;
   private Contact contact;
   private double contactTime;
   private int ecType;
   private boolean contactDone;
   private double afterContactTime;
   private int esType;
   private EndServiceEvent newEndServiceEvent;
   
   /**
    * Constructs a new start-service event
    * that will put the contact
    * in service represented by \texttt{oldEndServiceEvent}
    * in the target agent group given by
    * {@link EndServiceEvent#getAgentGroup()}.
    * @param oldEndServiceEvent the old end-service event.
    */
   public StartServiceEvent (EndServiceEvent oldEndServiceEvent) {
      this (oldEndServiceEvent.getAgentGroup(), oldEndServiceEvent);
   }
   
   /**
    * Constructs a new start-service event
    * that will put the contact
    * in service represented by \texttt{oldEndServiceEvent}
    * in the target agent group \texttt{targetGroup}.
    * @param targetGroup the target agent group.
    * @param oldEndServiceEvent the old end-service event.
    */
   public StartServiceEvent (AgentGroup targetGroup, EndServiceEvent oldEndServiceEvent) {
      super (oldEndServiceEvent.simulator());
   //   if (oldEndServiceEvent == null)
   //      throw new NullPointerException();
      if (oldEndServiceEvent.afterContactDone ())
         throw new IllegalArgumentException
         ("Cannot make a start-service event with an end-service event representing a completed service");
      if (targetGroup == null)
         throw new NullPointerException();
      this.targetGroup = targetGroup;
      if (oldEndServiceEvent instanceof EndServiceEventDetailed)
         targetAgent = ((EndServiceEventDetailed)oldEndServiceEvent).getAgent ();
      contact = oldEndServiceEvent.getContact ();
      if (oldEndServiceEvent.contactDone ()) {
         contactDone = true;
         contactTime = oldEndServiceEvent.getEffectiveContactTime ();
         ecType = oldEndServiceEvent.getEffectiveEndContactType ();
         afterContactTime = oldEndServiceEvent.getScheduledAfterContactTime ();
         esType = oldEndServiceEvent.getScheduledEndServiceType ();
      }
      else {
         contactTime = oldEndServiceEvent.getScheduledContactTime ();
         ecType = oldEndServiceEvent.getScheduledEndContactType ();
      }
   }
   
   /**
    * Constructs an event that will
    * call
    * \texttt{targetGroup.}{@link AgentGroup#serve(Contact,double,int)
    * serve}
    * \texttt{(contact, contactTime, ecType)} when
    * it happens.
    * @param targetGroup the target agent group.
    * @param contact the contact to serve.
    * @param contactTime the contact time.
    * @param ecType the end-contact type.
    * @exception NullPointerException if \texttt{targetGroup} or
    * \texttt{contact} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{contactTime}
    * is negative.
    */
   public StartServiceEvent (AgentGroup targetGroup, Contact contact, double contactTime, int ecType) {
      super (contact.simulator());
      if (targetGroup == null || contact == null)
         throw new NullPointerException();
      if (contactTime < 0)
         throw new IllegalArgumentException ("contactTime < 0");
      this.targetGroup = targetGroup;
      this.contact = contact;
      this.contactTime = contactTime;
      this.ecType = ecType;
   }

   /**
    * Constructs an event that will
    * call
    * \texttt{targetGroup.}{@link AgentGroup#serve(Contact,double,int,double,int)
    * serve}
    * \texttt{(contact, contactTime, ecType, afterContactTime, esType)} when
    * it happens.
    * @param targetGroup the target agent group.
    * @param contact the contact to serve.
    * @param contactTime the contact time.
    * @param ecType the end-contact type.
    * @param afterContactTime the after-contact time.
    * @param esType the end-service type.
    * @exception NullPointerException if \texttt{targetGroup} or
    * \texttt{contact} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{contactTime}
    * or \texttt{afterContactTime} are negative.
    */
   public StartServiceEvent (AgentGroup targetGroup, Contact contact, double contactTime, int ecType, double afterContactTime, int esType) {
      this (targetGroup, contact, contactTime, ecType);
      if (afterContactTime < 0)
         throw new IllegalArgumentException
         ("afterContactTime < 0");
      contactDone = true;
      this.afterContactTime = afterContactTime;
      this.esType = esType;
   }
   
   /**
    * Constructs an event that will
    * call
    * \texttt{targetAgent.getGroup().}{@link DetailedAgentGroup#serve(Contact,Agent,double,int)
    * serve}
    * \texttt{(contact, targetAgent, contactTime, ecType)} when
    * it happens.
    * @param targetAgent the target agent.
    * @param contact the contact to serve.
    * @param contactTime the contact time.
    * @param ecType the end-contact type.
    * @exception NullPointerException if \texttt{targetAgent} or
    * \texttt{contact} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{contactTime}
    * is negative.
    */
   public StartServiceEvent (Agent targetAgent, Contact contact, double contactTime, int ecType) {
      this (targetAgent.getAgentGroup (), contact, contactTime, ecType);
      this.targetAgent = targetAgent;
   }   

   /**
    * Constructs an event that will
    * call
    * \texttt{targetAgent.getGroup().}{@link DetailedAgentGroup#serve(Contact,Agent,double,int,double,int)
    * serve}
    * \texttt{(contact, targetAgent, contactTime, ecType, afterContactTime, esType)} when
    * it happens.
    * @param targetAgent the target agent.
    * @param contact the contact to serve.
    * @param contactTime the contact time.
    * @param ecType the end-contact type.
    * @param afterContactTime the after-contact time.
    * @param esType the end-service type.
    * @exception NullPointerException if \texttt{targetAgent} or
    * \texttt{contact} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{contactTime}
    * or \texttt{afterContactTime} are negative.
    */
   public StartServiceEvent (Agent targetAgent, Contact contact, double contactTime, int ecType, double afterContactTime, int esType) {
      this (targetAgent.getAgentGroup (), contact, contactTime, ecType, afterContactTime, esType);
      this.targetAgent = targetAgent;
   }
   
   /**
    * Returns the agent group that will receive
    * the contact stored into the attached
    * end-service event.
    * @return the target agent group.
    */
   public AgentGroup getTargetAgentGroup() {
      return targetGroup;
   }
   
   /**
    * Returns the target agent of this event,
    * or \texttt{null} if no target agent
    * was specified.
    * @return the target agent.
    */
   public Agent getTargetAgent() {
      return targetAgent;
   }
   
   /**
    * Returns the contact being served.
    @return the contact being served.
   */
   public Contact getContact() {
      return contact;
   }

   /**
    * Returns the scheduled duration of the communication
    * between the contact and an agent.
    @return the scheduled contact time.
    */
   public double getScheduledContactTime() {
      return contactTime;
   }

   /**
    * Returns the scheduled after-contact time.
    * If the after-contact time was not set, an
    * {@link IllegalStateException} is thrown.
    @return the scheduled after-contact time.
    @exception IllegalStateException if the after-contact time
    was not set.
    */
   public double getScheduledAfterContactTime() {
      if (!contactDone)
         throw new IllegalStateException ("Scheduled after contact time not available yet");
      return afterContactTime;
   }

   /**
    * Returns the type of contact termination that
    * will occur when the end-service event happens for the first time.
    @return the scheduled end-contact type.
    */
   public int getScheduledEndContactType() {
      return ecType;
   }

   /**
    * Returns the type of the service termination that
    * will occur when the end-service event happens for the second time.
    @return the scheduled end-service type.
   */
   public int getScheduledEndServiceType() {
      if (!contactDone)
         throw new IllegalStateException ("Scheduled end service type not available yet");
      return esType;
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
    * Returns the end-service event representing
    * the contact's restarted service.
    * This returns a non-\texttt{null}
    * value only after the execution of the
    * {@link #actions} method.
    * @return the new dequeue event.
    */
   public EndServiceEvent getNewEndServiceEvent() {
      return newEndServiceEvent;
   }
   
   @Override
   public void actions () {
      if (targetGroup instanceof DetailedAgentGroup && targetAgent != null) {
         if (contactDone)
            newEndServiceEvent = ((DetailedAgentGroup)targetGroup).serve
            (contact, targetAgent, contactTime, ecType,
                  afterContactTime, esType);
         else
            newEndServiceEvent = ((DetailedAgentGroup)targetGroup).serve
            (contact, targetAgent, contactTime, ecType);
      }
      else if (contactDone)
         newEndServiceEvent = targetGroup.serve
         (contact, contactTime, ecType,
               afterContactTime, esType);
      else
         newEndServiceEvent = targetGroup.serve
         (contact, contactTime, ecType);
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Target agent group: ").append
      (ContactCenter.toShortString (targetGroup));
      if (targetAgent != null)
         sb.append (", target agent: ").append
         (ContactCenter.toShortString (targetAgent));
      sb.append (", served contact: ").append
      (ContactCenter.toShortString (contact));

      sb.append (", scheduled contact time: ").append
      (contactTime);
      sb.append (", scheduled end-contact type: ").append
      (ecType);
      if (contactDone) {
         sb.append (", scheduled after contact time: ").append
         (afterContactTime);
         sb.append (", scheduled end service type: ").append
         (esType);
      }
      sb.append (']');
      return sb.toString();
   }
}
