package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactStepInfo;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.ssj.simevents.Event;

/**
 * Represents an event happening when a contact leaves a waiting queue without
 * being explicitly removed. This event also holds the necessary information
 * about a contact in queue and is added to the waiting queue's data structure.
 * When it becomes obsolete, it can be used to keep track of
 * the queueing step of the concerned contact. For this reason, the event
 * implements the {@link ContactStepInfo} interface.
 *
 * Note that the natural ordering of dequeue events
 * corresponds to ascending order of automatic removal
 * from queue, not the order of insertion.
 * This is adapted for insertion of dequeue events in
 * event lists, not for priority queues.
 * The class {@link DequeueEventComparator} must
 * be used to impose the order of insertion, for
 * priority queues.
 * This comparator is used when calling the
 * default constructor of {@link PriorityWaitingQueue}
 * and {@link QueueWaitingQueue}.
 */
public final class DequeueEvent extends Event implements
      ContactStepInfo, Cloneable {
   private WaitingQueue queue;
   private Contact contact;
   double enqueueTime;
   double qTime;
   int dqType;
   double eqTime;
   int edqType;
   boolean dequeued;
   private int expectedInitCount;
   private List<Integer> TabListeGroupAgent[] = null ;  /* Ajouter pour initialer le tableau de la liste des
                                                      types que chaque groupe peut traiter*/

   /**
    * Constructs a new dequeue event with contact \texttt{contact}
    * entering waiting queue \texttt{queue} at simulation
    * time \texttt{enqueueTime}.
    *
    * This constructor is rarely used directly;
    * the recommended way to create dequeue
    * events is to use
    * {@link WaitingQueue#add(Contact)}.
    *
    * @param queue
    *           the associated waiting queue.
    * @param contact
    *           the contact being queued.
    * @param enqueueTime
    * the time at which the contact
    * enters the queue.
    */
   protected DequeueEvent (WaitingQueue queue, Contact contact,
         double enqueueTime) {
      super (contact.simulator());
      if (queue == null)
         throw new NullPointerException();
      this.queue = queue;
      expectedInitCount = queue.initCount;
      //  this.contact = contact;
      if (queue.size() > 0)                                                   //    Ajouter
         contact.setPositionInWaitingQueue(queue.size());               //    Ajouter
      //Router router=contact.getRouter();
      if (contact.getRouter() != null) {
         HashSet<Integer> rec = listeDesTypeMemeAgents(contact);               //   Ajouter
         HashMap<Integer, Double> listeHashMap = new HashMap<Integer, Double>();
         double tailleFile;
         if (rec != null && rec.size() >= 1) {
for (int k: rec) {
               tailleFile = contact.getRouter().getWaitingQueue(k).size();
               listeHashMap.put(k, tailleFile);
            }
         }
         if (listeHashMap != null)
            contact.setListeDesTraitesParLesMemeAgents(listeHashMap) ;
      }      // jusqu'a ici ajouter
      this.enqueueTime = enqueueTime;
      dequeued = false;
      // if(TabListeGroupAgent==null){initTabListDesGroupAgent(contact.getRouter()); }                      //Ajouter
      this.contact = contact;
   }

   /**
    * Returns a reference to the queued contact.
    *
    * @return a reference to the queued contact.
    */
   public Contact getContact () {
      return contact;
   }

   /**
    * Returns a reference to the waiting queue.
    *
    * @return a reference to the waiting queue.
    */
   public WaitingQueue getWaitingQueue () {
      return queue;
   }

   /**
    * Returns the simulation time at which the contact was enqueued.
    *
    * @return the contact's enqueue time.
    */
   public double getEnqueueTime () {
      return enqueueTime;
   }

   public double getStartingTime () {
      return enqueueTime;
   }

   public double getEndingTime () {
      return enqueueTime + getEffectiveQueueTime ();
   }

   /**
    * Returns the scheduled queue time for this contact. This corresponds to the
    * maximal time the contact can spend in queue before being automatically
    * removed, if this event occurs.
    *
    * @return the contact's scheduled queue time.
    */
   public double getScheduledQueueTime () {
      return qTime;
   }

   /**
    * Returns the scheduled dequeue type of the contact if this event occurs.
    * This scheduled dequeue type can be overridden when a contact is manually
    * removed.
    *
    * @return the contact's scheduled dequeue type.
    */
   public int getScheduledDequeueType () {
      return dqType;
   }

   /**
    * Changes the dequeue type of the contact to \texttt{dqType} when the event
    * occurs. If this is called after the contact was dequeued, an
    * {@link IllegalStateException} is thrown.
    *
    * @param dqType
    *           the new type of removal.
    * @exception IllegalStateException
    *               if the contact was dequeued.
    */
   public void setScheduledDequeueType (int dqType) {
      if (dequeued)
         throw new IllegalStateException ("Contact already dequeued");
      this.dqType = dqType;
   }

   /**
    * Returns the simulation time the contact has effectively spent in queue.
    *
    * @return the effective queue time.
    * @exception IllegalStateException
    *               if the contact is still in queue.
    */
   public double getEffectiveQueueTime () {
      if (!dequeued)
         return simulator ().time () - enqueueTime;
      return eqTime;
   }

   /**
    * Returns the effective dequeue type of the contact having waited in this
    * queue. Throws an {@link IllegalStateException} if the contact is still in
    * queue.
    *
    * @return the effective dequeue type.
    * @exception IllegalStateException
    *               if the contact is still in queue.
    */
   public int getEffectiveDequeueType () {
      if (!dequeued)
         throw new IllegalStateException (
            "Effective dequeue type not available yet");
      return edqType;
   }

   /**
    * Removes this dequeue event from its
    * associated waiting queue, with
    * dequeue type \texttt{dqType}.
    * Returns \texttt{true} if and only if
    * the removal was successful.
    * This method calls
    * {@link #getWaitingQueue()}
    * {@link WaitingQueue#remove(DequeueEvent,int) .remove (this, dqType)},
    * and returns the result.
    * @param dqType1 the dequeue type.
    * @return the success indicator of the operation.
    */
   public boolean remove (int dqType1) {
      return queue.remove (this, dqType1);
   }

   /**
    * Indicates that the contact has left the queue and that this event is
    * obsolete. If an obsolete event is scheduled, an
    * {@link IllegalStateException} is thrown at the time it happens. The event
    * can be used as a data structure to keep a trace of the queueing process of
    * the contact.
    *
    * @return the dequeue indicator.
    */
   public boolean dequeued () {
      // This is also used to mark contacts for removal
      // instead of performing linear search to retrieve
      // them in data structures.
      return dequeued;
   }

   /**
    * Determines if this event is obsolete. When calling
    * {@link WaitingQueue#init()}, some dequeue events might still be in the
    * simulator's event list. One must use this method in {@link #actions()} to
    * test if this event is obsolete. If that returns \texttt{true}, one should
    * return immediately.
    *
    * @return \texttt{true} for an obsolete event, \texttt{false} otherwise.
    */
   public boolean isObsolete () {
      return expectedInitCount != queue.initCount;
   }

   @Override
   public void actions () {
      if (isObsolete ())
         return;
      if (dequeued)
         throw new IllegalStateException ("Contact already dequeued");
      queue.dequeueUpdateStatus (this);
      queue.dequeued (this, dqType);
   }

   /**
    * Compares this dequeue event with the other event \texttt{ev}. The method
    * extracts the {@link Contact} object from this event and from the
    * \texttt{ev} argument.
    * The {@link Contact#compareTo(Contact)} method is then used to
    * compare objects. A contact that cannot be extracted is assigned the
    * \texttt{null} value and precedes any non-\texttt{null} contacts.
    *
    * @param ev
    *           the other event being compared.
    * @return the result of the comparison.
    */
   public int compareTo (DequeueEvent ev) {
      final Contact c1 = contact;
      final Contact c2 = ev == null ? null : ev.getContact ();
      if (c1 == null && c2 == null)
         return 0;
      else if (c1 == null)
         return -1;
      else if (c2 == null)
         return 1;
      return c1.compareTo (c2);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append ("waiting queue: ")
      .append (ContactCenter.toShortString (queue));
      sb.append (", queued contact: ").append (
         ContactCenter.toShortString (contact));
      sb.append (", enqueue time: ").append (enqueueTime);
      if (!dequeued || qTime != eqTime)
         sb.append (", scheduled queue time: ").append (qTime);
      if (!dequeued || dqType != edqType)
         sb.append (", scheduled dequeue type: ").append (dqType);
      if (dequeued) {
         sb.append (", effective queue time: ").append (eqTime);
         sb.append (", effective dequeue type: ").append (edqType);
      } else
         sb.append (", still in queue");
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Returns a copy of this event.
    * This method clones every field of the event, except the
    * waiting queue which is not cloneable.
    */
   @Override
   public DequeueEvent clone () {
      return clone (contact.clone ());
   }

   /**
    * Similar to {@link #clone()}, but initializes the contact
    * of the cloned event with \texttt{clonedContact} instead of
    * a clone of the contact returned by {@link #getContact()}.
    * This method can be useful when cloning a contact \texttt{c}
    * for which \texttt{c.getSteps()} returns a non-empty
    * list containing dequeue events.
    * In that case, the contact associated with the events included in \texttt{c.getSteps()} must
    * correspond to \texttt{c} rather than clones of \texttt{c}.
    */
   public DequeueEvent clone (Contact clonedContact) {
      DequeueEvent ev;
      try {
         ev = (DequeueEvent) super.clone ();
      } catch (final CloneNotSupportedException cne) {
         throw new InternalError (
            "CloneNotSupportedException for a class implementing Cloneable");
      }
      ev.contact = clonedContact;
      //ev.expectedInitCount = queue.initCount - 1;
      return ev;
   }

   //Ajouter pour initialer le tableau de la liste des types que chaque groupe peut traiter

   public void initTabListDesGroupAgent(Router router)
   {
      if (router != null) {
         TabListeGroupAgent = new ArrayList[router.getNumAgentGroups()];
         for (int k = 0;k < router.getNumAgentGroups();k++)
            TabListeGroupAgent[k] = new ArrayList<Integer>();
         for (int i = 0;i < router.getNumAgentGroups();i++) {
            for (int j = 0;j < router.getNumContactTypes();j++) {
               if (router.canServe(i, j) )
                  TabListeGroupAgent[i].add(j);
            }
         }
      }

   }
   // Ajouter pour retourner la liste des type traiter par les memes agents
   public HashSet<Integer> listeDesTypeMemeAgents(Contact contact )
   {
      if (TabListeGroupAgent == null) {
         initTabListDesGroupAgent(contact.getRouter());
      }
      //if(contact.getRouter()==null){System.out.println("Router du contact est null");}
      HashSet<Integer> listeDesTypes = new HashSet<Integer>(); // Pour eviter les doublons
      final int idType = contact.getTypeId();
      int idGroupAgent;
      for (int i = TabListeGroupAgent.length - 1;i >= 0;i--) {
         for (int k = TabListeGroupAgent[i].size() - 1;k >= 0;k--) {
            idGroupAgent = TabListeGroupAgent[i].get(k);
            if (idGroupAgent == idType) { //Integer objet=new Integer(idType);
               TabListeGroupAgent[i].remove(k);
               listeDesTypes.addAll(TabListeGroupAgent[i]);
            }
         }
      }
      return listeDesTypes;
   }
}
