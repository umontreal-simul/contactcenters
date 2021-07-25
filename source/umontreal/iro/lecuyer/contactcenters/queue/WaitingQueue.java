package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.MinValueGenerator;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents a waiting queue where contacts are added if they cannot be served
 * immediately.
 * The queue contains {@link DequeueEvent} objects
 * being scheduled to happen at the time of automatic removal, e.g.,
 * abandonment, disconnection, etc.
 * These dequeue events, which encapsulate contacts,
 * are used
 * to support abandonment as well as other types of exits of queue.
 * When a contact is added at the end of the
 * queue using the {@link #add(Contact)} method, its dequeue event is constructed, and
 * scheduled if a maximal queue time is available.
 * If the dequeue event occurs, the associated
 * queued contact is removed from the queue.
 * Queued contacts can also be
 * removed manually using the {@link #removeFirst(int)} or {@link #removeLast(int)}
 * methods (this cancels the appropriate dequeue event),
 * or visited by an iterator returned by {@link #iterator(int)}. An
 * iterator is useful to enumerate queued contacts, and to remove arbitrary
 * ones.
 *
 * All registered \emph{waiting-queue listeners} are notified about added and
 * removed contacts. The reason of the removal is available for listeners
 * through an integer called the \emph{dequeue type}, encapsulated in the
 * dequeue event. For example, this permits statistical collectors to
 * distinguish abandonment from disconnection.
 *
 * This abstract class does not implement a data structure for storing queued
 * contacts. The subclasses {@link StandardWaitingQueue},
 * {@link QueueWaitingQueue}, and
 * {@link PriorityWaitingQueue} implement such data structures.
 *
 * Note: the {@link WaitingQueueListener} implementations are notified in the
 * order of the list returned by {@link #getWaitingQueueListeners()}, and a
 * waiting-queue listener modifying the list of listeners by using
 * {@link #addWaitingQueueListener(WaitingQueueListener)} or {@link #removeWaitingQueueListener(WaitingQueueListener)} could
 * result in unpredictable behavior.
 */
public abstract class WaitingQueue extends AbstractQueue<DequeueEvent>
      implements Initializable, Named {
   private final MinValueGenerator dqgens = new MinValueGenerator (2);
   int queueSize = 0;
   int[] queueSizeK = new int[1];
   private int id = -1;
   private final List<WaitingQueueListener> listeners = new ArrayList<WaitingQueueListener> ();
   private final List<WaitingQueueListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;
   private String name = "";
   private int dqTypeDefault;
   int initCount;
   private Map<Object,Object> attributes = null;
   private boolean isClearing = false;

   /**
    * Constructs a new waiting queue.
    */
   public WaitingQueue () {
      dqgens.setValueGenerator (1, new ContactPatienceTimeGenerator ());
   }

   public String getName () {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
   }

   /**
    * Initializes this waiting queue for a new simulation replication. This
    * removes all the contacts from the queue without notification of individual
    * contacts to the listeners.
    */
   public void init () {
      // In case there were a bug with queueSize,
      // we call elementsClear all the times to avoid
      // memory problem
      queueSize = 0;
      Arrays.fill (queueSizeK, 0);
      elementsClear ();
      dqgens.init ();
      ++initCount;
      notifyInit();
   }

   @Override
   public void clear () {
      clear (getDefaultDequeueType ());
   }

   @Override
   public boolean isEmpty () {
      return queueSize == 0;
   }

   public boolean offer (DequeueEvent ev) {
      if (ev.getWaitingQueue () != this)
         throw new IllegalArgumentException ("Event linked to the wrong queue");
      if (contains (ev))
         throw new IllegalArgumentException ("Event already in queue");
      internalAdd (ev);
      return true;
   }

   public DequeueEvent peek () {
      return isEmpty () ? null : getFirst ();
   }

   public DequeueEvent poll () {
      return isEmpty () ? null : removeFirst (getDefaultDequeueType ());
   }

   /**
    * Removes all the contacts contained into this waiting queue with dequeue
    * type \texttt{dqType}. In contrast with {@link #init()}, any removed contact
    * is notified to the registered listeners.
    *
    * @param dqType
    *           the dequeue type of the removed contacts.
    */
   public void clear (int dqType) {
      isClearing = true;
      final Iterator<DequeueEvent> itr = elementsIterator ();
      try {
         while (itr.hasNext ()) {
            final DequeueEvent ev = itr.next ();
            if (!ev.dequeued) {
               dequeueUpdateStatus (ev);
               ev.cancel ();
               dequeued (ev, dqType);
            }
         }
         elementsClear ();
      } finally {
         isClearing = false;
      }
      queueSize = 0;
      Arrays.fill (queueSizeK, 0);
   }

   /**
    * Returns the number of contacts in this waiting queue.
    *
    * @return the number of contacts in the waiting queue.
    */
   @Override
   public int size () {
      // The size of the data structure can be greater
      // than the real queue size, because
      // lost contacts are not immediately
      // removed from the queue.
      assert queueSize >= 0;
      return queueSize;
   }

   /**
    * Returns the number of contacts of type
    * \texttt{k} in this waiting queue.
    * @param k the tested contact type.
    * @return the number of contacts of type \texttt{k} in the queue.
    */
   public int size (int k) {
      if (k < 0 || k >= queueSizeK.length)
         return 0;
      assert queueSizeK[k] >= 0;
      return queueSizeK[k];
   }

   /**
    * Adds the contact \texttt{contact} to the waiting queue and returns a
    * reference to the constructed dequeue event. The maximal queue time is
    * obtained using {@link #getMaximalQueueTime(DequeueEvent)}, and an event is scheduled
    * with its corresponding dequeue type. In case of a zero or negative maximal
    * queue time, the contact is enqueued then immediately dequeued. Otherwise,
    * the contact is enqueued and a dequeue event is scheduled if the maximal
    * queue time is not {@link Double#POSITIVE_INFINITY} or {@link Double#NaN}.
    * The returned event can be used to get information about the queued contact
    * and to manually remove it from the queue. If it is directly cancelled, the
    * contact will not leave the queue automatically.
    *
    * @param contact
    *           the contact to be added.
    * @return a reference to the dequeue event.
    */
   public DequeueEvent add (Contact contact) {
      final DequeueEvent ev = new DequeueEvent (this, contact, contact.simulator().time());
      dqTypeRet = 1;
      double qTime = getMaximalQueueTime (ev);
      if (qTime < 0)
         qTime = 0;
      ev.dqType = dqTypeRet;
      ev.qTime = qTime;
      return internalAdd (ev);
   }

   /**
    * This is the same as {@link #add(Contact)}, except that the
    * enqueue time, maximal queue
    * time and dequeue type if the queue time is reached, are specified
    * explicitly.
    *
    * @param contact
    *           the contact being queued.
    * @param enqueueTime
    * the time at which the contact joined the queue.
    * @param maxQueueTime
    *           the maximal queue time.
    * @param dqType
    *           the dequeue type if the maximal queue time is reached.
    * @return the dequeue event representing the queued contact.
    */
   public DequeueEvent add (Contact contact, double enqueueTime, double maxQueueTime, int dqType) {
      final DequeueEvent ev = new DequeueEvent (this, contact, enqueueTime);
      ev.qTime = maxQueueTime;
      ev.dqType = dqType;
      return internalAdd (ev);
   }

   /**
    * Adds a contact into the queue by using the information stored in an old
    * dequeue event \texttt{oldDequeueEvent}. This method extracts the queued
    * contact, the scheduled maximal queue time, and the scheduled dequeue type
    * from the given event, and uses that information to call
    * {@link #add(Contact,double,double,int)}.
    *
    * @param oldDequeueEvent
    *           the old dequeue event.
    * @return the new dequeue event representing the queued contact.
    */
   public DequeueEvent addFromOldEvent (DequeueEvent oldDequeueEvent) {
      return add (oldDequeueEvent.getContact ().clone (),
            oldDequeueEvent.getEnqueueTime (),
            oldDequeueEvent.getScheduledQueueTime (),
            oldDequeueEvent.getScheduledDequeueType ());
   }

   private DequeueEvent internalAdd (DequeueEvent ev) {
      final double qTime = ev.qTime;
      final Contact contact = ev.getContact ();
      if (qTime == 0) {
         // Immediate dequeue
         contact.enqueued (ev);
         notifyEnqueued (ev);
         if (!ev.dequeued)
            dequeued (ev, ev.dqType);
      } else {
         if (!Double.isInfinite (qTime) && !Double.isNaN (qTime))
            ev.schedule (qTime);
         enqueueUpdateStatus (ev);
         contact.enqueued (ev);
         notifyEnqueued (ev);
      }
      return ev;
   }

   private void enqueueUpdateStatus (DequeueEvent ev) {
      elementsAdd (ev);
      ++queueSize;
      final int k = ev.getContact ().getTypeId ();
      if (k >= 0) {
         if (k >= queueSizeK.length)
            queueSizeK = ArrayUtil.resizeArray (queueSizeK, k + 1);
         ++queueSizeK[k];
      }
   }

   /**
    * Contains the dequeue type generated by {@link #getMaximalQueueTime(DequeueEvent)}.
    */
   protected int dqTypeRet = 1;

   /**
    * Generates and returns the maximal queue time for the queued contact
    * represented by \texttt{ev}. The method can store a dequeue type in the
    * protected field {@link #dqTypeRet} if the default value of 1 is not
    * appropriate.
    *
    * By default, a {@link MinValueGenerator} is used. For each dequeue type $q$
    * with an associated value generator, a maximal queue time $V_q$ is
    * generated. The scheduled queue time is $V_{q^*}=\min_q\{V_q\}$, and the
    * dequeue type is $q^*$.
    *
    * @param ev
    *           the dequeue event representing the queued contact.
    * @return the maximal queue time.
    */
   protected double getMaximalQueueTime (DequeueEvent ev) {
      final Contact contact = ev.getContact ();
      final double minV = dqgens.nextDouble (contact);
      if (Double.isNaN (minV)) {
         dqTypeRet = 1;
         return Double.POSITIVE_INFINITY;
      } else
         dqTypeRet = dqgens.getLastVType ();
      return minV;
   }

   /**
    * Removes the contact identified by the dequeue event \texttt{dqEvent},
    * setting its effective dequeue type to \texttt{dqType}. Returns
    * \texttt{true} if the removal was successful, \texttt{false} otherwise.
    *
    * @param dqEvent
    *           the dequeue event.
    * @param dqType
    *           the effective dequeue type.
    * @return the success indicator of the operation.
    */
   public boolean remove (DequeueEvent dqEvent, int dqType) {
      if (dqEvent.getWaitingQueue () != this)
         // An event belonging to another waiting queue
         return false;
      if (dqEvent.dequeued)
         // An obsolete event
         return false;
      dequeueUpdateStatus (dqEvent);
      dqEvent.cancel ();
      dequeued (dqEvent, dqType);
      return true;
   }

   void dequeueUpdateStatus (DequeueEvent dqEvent) {
      --queueSize;
      final int k = dqEvent.getContact ().getTypeId ();
      if (k >= 0 && k < queueSizeK.length)
         --queueSizeK[k];
      if (queueSize == 0 && !elementsIsEmpty() && !isClearing)
         // The data structure for the queue contains
         // only dummy elements, so clear it.
         elementsClear ();
   }

   @Override
   public boolean remove (Object o) {
      if (!(o instanceof DequeueEvent))
         return false;
      return remove ((DequeueEvent) o, getDefaultDequeueType ());
   }

   /**
    * Removes the contact \texttt{contact} from the waiting queue. with dequeue
    * type \texttt{dqType}. Returns \texttt{true} if the contact was removed,
    * \texttt{false} otherwise. If a dequeue event was scheduled when the
    * contact was added, this event is cancelled. This method has to linearly
    * search for the contact being removed using {@link #getDequeueEvent(Contact)},
    * which is less efficient than when a dequeue event is given.
    *
    * @param contact
    *           the contact being removed from the queue.
    * @param dqType
    *           the effective dequeue type of the contact.
    * @return \texttt{true} if the contact was removed, \texttt{false}
    *         otherwise.
    */
   public boolean remove (Contact contact, int dqType) {
      final DequeueEvent ev = getDequeueEvent (contact);
      if (ev == null)
         return false;
      return remove (ev, dqType);
   }

   /**
    * Returns the dequeue event for the contact \texttt{contact}. If the contact
    * is not in queue, this returns \texttt{null}. Since this method has to
    * perform a linear search, it is more efficient to keep the dequeue events
    * returned by {@link #add(Contact)} when they are needed.
    *
    * @param contact
    *           the queried contact.
    * @return the dequeue event for the contact, or \texttt{null} if the contact
    *         was not found.
    */
   public DequeueEvent getDequeueEvent (Contact contact) {
      // Not called very often, so use the iterator
      // To get it more efficient, we would need to
      // create a hash table mapping contact objects to
      // the dequeue events. This would add
      // overhead affecting all WaitingQueue's methods.
      for (final Iterator<DequeueEvent> itr = elementsIterator (); itr
            .hasNext ();) {
         final DequeueEvent ev = itr.next ();
         if (ev.dequeued) {
            itr.remove ();
            if (ev.getContact () == contact)
               // We already know that the dequeue event does not
               // exist anymore in the waiting queue.
               return null;
         }
         else if (ev.getContact () == contact)
            return ev;
      }
      return null;
   }

   /**
    * Returns the dequeue event representing the first contact in the queue, or
    * throws a {@link NoSuchElementException} if the queue is empty.
    *
    * @return the dequeue event for the first contact in the queue.
    * @exception NoSuchElementException
    *               if the queue is empty.
    */
   public DequeueEvent getFirst () {
      DequeueEvent ev = null;
      // Since this is called often, avoid to create the iterator.
      while (ev == null) {
         final DequeueEvent testEv = elementsGetFirst ();
         if (testEv.dequeued)
            elementsRemoveFirst ();
         else
            ev = testEv;
      }
      return ev;
   }

   /**
    * Returns the dequeue event representing the last contact in the queue, or
    * throws a {@link NoSuchElementException} if the queue is empty.
    *
    * @return the dequeue event for the last contact in the queue.
    * @exception NoSuchElementException
    *               if the queue is empty.
    */
   public DequeueEvent getLast () {
      DequeueEvent ev = null;
      // Since this is called often, avoid to create the iterator.
      while (ev == null) {
         final DequeueEvent testEv = elementsGetLast ();
         if (testEv.dequeued)
            elementsRemoveLast ();
         else
            ev = testEv;
      }
      return ev;
   }

   /**
    * Removes the first contact in the waiting queue and returns the
    * corresponding dequeue event. The event is assigned the effective dequeue
    * type \texttt{dqType}. If the queue is empty, a
    * {@link NoSuchElementException} is thrown. The {@link #getFirst()} method is
    * used to get the dequeue event.
    *
    * @param dqType
    *           the effective dequeue type.
    * @return the dequeue event corresponding to the removed contact.
    * @exception NoSuchElementException
    *               if the queue is empty.
    */
   public DequeueEvent removeFirst (int dqType) {
      getFirst ();
      final DequeueEvent ev = elementsRemoveFirst ();
      dequeueUpdateStatus (ev);
      ev.cancel ();
      dequeued (ev, dqType);
      return ev;
   }

   /**
    * Removes the last contact in the waiting queue and returns the
    * corresponding dequeue event. The event is assigned the effective dequeue
    * type \texttt{dqType}. If the queue is empty, a
    * {@link NoSuchElementException} is thrown. The {@link #getLast()} method is
    * used to get the dequeue event.
    *
    * @param dqType
    *           the effective dequeue type.
    * @return the dequeue event corresponding to the removed contact.
    * @exception NoSuchElementException
    *               if the queue is empty.
    */
   public DequeueEvent removeLast (int dqType) {
      getLast ();
      final DequeueEvent ev = elementsRemoveLast ();
      dequeueUpdateStatus (ev);
      ev.cancel ();
      dequeued (ev, dqType);
      return ev;
   }

   /**
    * Returns an iterator allowing the dequeue events representing contacts in
    * queue to be enumerated. The order of the elements depends on the type of
    * waiting queue and the order of insertion. The objects returned by the
    * iterator's {@link java.util.Iterator#next() next()} method are instances of
    * the {@link DequeueEvent} class. The optional
    * {@link java.util.Iterator#remove() remove()} method is implemented and
    * removes contacts with dequeue type \texttt{dqType}. If
    * {@link java.util.Iterator#remove() remove()} is never called on the returned
    * iterator, \texttt{dqType} is not used.
    *
    * @param dqType
    *           the dequeue type of any removed contact.
    * @return an iterator enumerating the contacts in queue.
    */
   public Iterator<DequeueEvent> iterator (int dqType) {
      return new QueueIterator (elementsIterator (), dqType);
   }

   /**
    * This is similar to {@link #iterator(int)}, except it uses the default
    * dequeue type returned by {@link #getDefaultDequeueType()}.
    *
    * @return the constructed iterator.
    */
   @Override
   public Iterator<DequeueEvent> iterator () {
      return new QueueIterator (elementsIterator (), dqTypeDefault);
   }

   /**
    * Returns the default dequeue type used by this object when the user does
    * not specify a dequeue type explicitly. The initial default dequeue type is
    * 0.
    *
    * @return the default dequeue type.
    */
   public int getDefaultDequeueType () {
      return dqTypeDefault;
   }

   /**
    * Sets the default dequeue type to \texttt{dqTypeDefault}.
    *
    * @param dqTypeDefault
    *           the new default dequeue type.
    */
   public void setDefaultDequeueType (int dqTypeDefault) {
      this.dqTypeDefault = dqTypeDefault;
   }

   /**
    * Adds the new waiting-queue listener \texttt{listener} to this object. If
    * the listener is already added, nothing happens; it is not added a second
    * time.
    *
    * @param listener
    *           the listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addWaitingQueueListener (WaitingQueueListener listener) {
      if (listener == null)
         throw new NullPointerException ("The added listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains (listener))
         listeners.add (listener);
   }

   /**
    * Removes the waiting-queue listener \texttt{listener} from this object. If
    * the listener is not registered, nothing happens.
    *
    * @param listener
    *           the waiting-queue listener being removed.
    */
   public void removeWaitingQueueListener (WaitingQueueListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.remove (listener);
   }

   /**
    * Removes all waiting-queue listeners registered with this waiting queue.
    */
   public void clearWaitingQueueListeners () {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.clear ();
   }

   /**
    * Returns an unmodifiable list containing all the waiting-queue listeners
    * registered with this waiting queue.
    *
    * @return the list of all registered waiting-queue listeners.
    */
   public List<WaitingQueueListener> getWaitingQueueListeners () {
      return umListeners;
   }

   /**
    * Notifies every registered listener that
    * this waiting queue was initialized.
    */
   protected void notifyInit () {
      final int nl = listeners.size ();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).init (this);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that
    * a contact was enqueued, this event being
    * represented by \texttt{ev}.
    * @param ev the dequeue event representing the
    * queued contact.
    */
   protected void notifyEnqueued (DequeueEvent ev) {
      final int nl = listeners.size ();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).enqueued (ev);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Notifies every registered listener that a contact
    * left this queue, this event being represented
    * by \texttt{ev}.
    * @param ev the event representing the contact
    * having left the queue.
    */
   protected void notifyDequeued (DequeueEvent ev) {
      final int nl = listeners.size ();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).dequeued (ev);
      } finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Constructs a new {@link WaitingQueueState} object holding the current
    * state of this waiting queue, i.e., every queued contact.
    *
    * @return the state of this waiting queue.
    */
   public WaitingQueueState save () {
      return new WaitingQueueState (this);
   }

   /**
    * Restores the state of the waiting queue by using the
    * {@link WaitingQueueState#restore(WaitingQueue) restore} method of
    * \texttt{state}.
    *
    * @param state
    *           the saved state of the waiting queue.
    */
   public void restore (WaitingQueueState state) {
      state.restore (this);
   }

   /**
    * Returns the identifier associated with this queue. This identifier, which
    * defaults to \texttt{-1}, can be used as an index in routers.
    *
    * @return the identifier associated with this queue.
    */
   public int getId () {
      return id;
   }

   /**
    * Sets the identifier of this queue to \texttt{id}. Once this identifier is
    * set to a positive or 0 value, it cannot be changed anymore. This method is
    * automatically called by the router when a waiting queue is connected. If
    * one tries to attach the same queue to different routers, the queue must
    * have the same index for each of them. For this reason, if one tries to
    * change the identifier, an {@link IllegalStateException} is thrown.
    *
    * @param id
    *           the new identifier associated with the queue.
    * @exception IllegalStateException
    *               if the identifier was already set.
    */
   public void setId (int id) {
      if (this.id >= 0 && this.id != id)
         throw new IllegalStateException ("Identifier already set");
      this.id = id;
   }

   /**
    * Returns the maximal queue time generator associated with dequeue type
    * \texttt{dqType} for this waiting queue. Returns \texttt{null} if no value
    * generator is associated with the given \texttt{dqType}.
    *
    * @param dqType
    *           the queried dequeue type.
    * @return the maximal queue time generator of this object.
    */
   public ValueGenerator getMaximalQueueTimeGenerator (int dqType) {
      return dqgens.getValueGenerator (dqType);
   }

   /**
    * Changes the maximal queue time generator associated with dequeue type
    * \texttt{dqType} for this waiting queue to \texttt{dqgen}.
    *
    * @param dqType
    *           the affected dequeue type.
    * @param dqgen
    *           the new maximal queue time generator.
    * @exception IllegalArgumentException
    *               if the given dequeue type is negative.
    * @exception NullPointerException
    *               if \texttt{dqgen} is \texttt{null}.
    */
   public void setMaximalQueueTimeGenerator (int dqType, ValueGenerator dqgen) {
      if (dqType <= 0)
         throw new IllegalArgumentException (
               "Cannot associate value generators to dequeue type " + dqType + "; this dequeue type is negative or reserved to signal service startup");
      dqgens.setValueGenerator (dqType, dqgen);
   }

   /**
    * Returns an iterator capable of traversing, in the correct order, the
    * elements in the waiting queue's internal data structure. This is different
    * from the {@link #iterator(int)} method because this iterator returns the
    * contacts marked for dequeue as well as the contacts still enqueued. If the
    * returned iterator does not implement {@link Iterator#remove() remove()},
    * {@link #remove(Contact,int)} and {@link #getDequeueEvent(Contact)} will not work
    * properly.
    *
    * @return an iterator for the waiting queue elements.
    */
   protected abstract Iterator<DequeueEvent> elementsIterator ();

   /**
    * Clears all elements in the data structure representing the queued
    * contacts.
    */
   protected abstract void elementsClear ();

   /**
    * Adds the new dequeued event \texttt{dqEvent} to the internal data
    * structure representing the waiting queue.
    *
    * @param dqEvent
    *           the dequeue event being added.
    */
   protected abstract void elementsAdd (DequeueEvent dqEvent);

   /**
    * Determines if the internal waiting queue data structure is empty.
    *
    * @return \texttt{true} if the data structure is empty, \texttt{false}
    *         otherwise.
    */
   protected abstract boolean elementsIsEmpty ();

   /**
    * Returns the first element of the waiting queue's internal data structure,
    * or throws a {@link NoSuchElementException} if no such element exists.
    *
    * @return the first element of the data structure.
    * @exception NoSuchElementException
    *               if the queue's data structure is empty.
    */
   protected abstract DequeueEvent elementsGetFirst ();

   /**
    * Returns the last element of the waiting queue's internal data structure,
    * or throws a {@link NoSuchElementException} if no such element exists.
    *
    * @return the last element of the data structure.
    * @exception NoSuchElementException
    *               if the queue's data structure is empty.
    */
   protected abstract DequeueEvent elementsGetLast ();

   /**
    * Removes and returns the first element in the waiting queue's internal data
    * structure. Throws a {@link NoSuchElementException} if no such element
    * exists.
    *
    * @return the removed element.
    * @exception NoSuchElementException
    *               if the queue's data structure is empty.
    */
   protected abstract DequeueEvent elementsRemoveFirst ();

   /**
    * Removes and returns the last element in the waiting queue's internal data
    * structure. Throws a {@link NoSuchElementException} if no such element
    * exists.
    *
    * @return the removed element.
    * @exception NoSuchElementException
    *               if the queue's data structure is empty.
    */
   protected abstract DequeueEvent elementsRemoveLast ();

   // This is called by DequeueEvent
   final void dequeued (DequeueEvent dqEvent, int dqType) {
      final double simTime = dqEvent.simulator().time();
      final double waitingTime = dqEvent.enqueueTime < simTime ? simTime - dqEvent.enqueueTime : 0;
      assert !dqEvent.dequeued : "The queued contact represented by the given event is already dequeued";
      dqEvent.dequeued = true;
      dqEvent.edqType = dqType;
      dqEvent.eqTime = waitingTime;
      dqEvent.getContact ().dequeued (dqEvent);
      notifyDequeued (dqEvent);
   }

   private static final class QueueIterator implements Iterator<DequeueEvent> {
      private final Iterator<DequeueEvent> itr;
      private DequeueEvent nextInfo = null;
      private DequeueEvent removeInfo = null;
      private int dqType;

      QueueIterator (Iterator<DequeueEvent> itr, int dqType) {
         this.itr = itr;
         this.dqType = dqType;
      }

      public boolean hasNext () {
         if (nextInfo != null)
            return true;
         removeInfo = null;
         while (itr.hasNext ()) {
            final DequeueEvent testInfo = itr.next ();
            if (testInfo.dequeued)
               itr.remove ();
            else {
               nextInfo = testInfo;
               return true;
            }
         }
         return false;
      }

      public DequeueEvent next () {
         if (!hasNext ())
            throw new NoSuchElementException ("No more contact in queue");
         removeInfo = nextInfo;
         nextInfo = null;
         return removeInfo;
      }

      public void remove () {
         if (removeInfo == null)
            throw new NoSuchElementException ("No more contact in queue");
         itr.remove ();
         final DequeueEvent sinfo = removeInfo;
         // When calling served, the state of the iterator
         // must be consistent because it could
         // be used inside a waiting-queue listener.
         removeInfo = null;
         if (sinfo.dequeued)
            return;
         final WaitingQueue queue = sinfo.getWaitingQueue ();
         queue.dequeueUpdateStatus (sinfo);
         sinfo.cancel ();
         queue.dequeued (sinfo, dqType);
      }

      @Override
      public String toString () {
         final StringBuilder sb = new StringBuilder (getClass ()
               .getSimpleName ());
         return sb.toString ();
      }
   }

   /**
    * Returns the map containing the attributes for this
    * waiting queue.  Attributes can be used to add user-defined information
    * to waiting queue objects at runtime, without creating
    * a subclass.  However, for maximal efficiency,
    * it is recommended to create a subclass of \texttt{Waiting\-Queue}
    * instead of using attributes.
    @return the map containing the attributes for this object.
    */
   public Map<Object,Object> getAttributes() {
      if (attributes == null)
         attributes = new HashMap<Object,Object>();
      return attributes;
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ("[");
      if (getName ().length () > 0)
         sb.append ("name: ").append (getName ()).append (", ");
      if (id != -1)
         sb.append ("id: ").append (id).append (", ");
      sb.append ("size: " + size ());
      sb.append ("]");
      return sb.toString ();
   }
}
