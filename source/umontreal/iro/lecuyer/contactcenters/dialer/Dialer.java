package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import umontreal.iro.lecuyer.collections.FilteredIterator;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactSource;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a predictive dialer making outbound contacts. A \emph{predictive
 * dialer} is normally used to generate outbound calls. The dialer's policy
 * determines the number of calls to try on each occasion (as a function of the
 * system's state), and supplies a list to extract them from. This list could be
 * produced by a contact factory and is often assumed to be infinite for
 * simplicity. Such lists could also be constructed from customer contacts who
 * left a message, who were disconnected, etc.
 *
 * For each call extracted from the dialer list, a success test is performed.
 * This test succeeds with a probability being fixed or depending on the tested
 * call, and the state of the system. Successful calls represent right party
 * connects whereas failed calls represent wrong party connects and connection
 * failures. The dialer generates a random delay representing the time between
 * the beginning of dialing and the success or failure. This delay may depend on
 * the success indicator, the call itself, the current time, etc. An event for
 * broadcasting the call to registered listeners is then scheduled to occur at
 * the time of success or failure.
 *
 * The dialer defines separate lists of new-contact listeners for right party
 * connects, and failed calls. Usually, only right party connects reach the
 * router, but statistical collectors may need to listen to failed calls as
 * well.
 *
 * Note: the order in which {@link NewContactListener} implementations are
 * notified is unspecified, and a new-contact listener modifying a list of
 * listeners could result in unpredictable behavior.
 */
public class Dialer implements ContactSource {
   private Simulator sim;
   private String name = "";
   private boolean enabled = false;
   private boolean useNumActionEvents = true;
   private DialerPolicy policy;
   private final List<NewContactListener> reachListeners = new ArrayList<NewContactListener> ();
   private final List<NewContactListener> failListeners = new ArrayList<NewContactListener> ();
   private final List<NewContactListener> umReachListeners = Collections.unmodifiableList (reachListeners);
   private final List<NewContactListener> umFailListeners = Collections.unmodifiableList (failListeners);
   private boolean broadcastInProgress;
   int numActionEvents;
   int[] numActionEventsTypes = new int[1];
   int initCount = 0;

   private RandomStream streamReach;
   private ValueGenerator probReach;
   private ValueGenerator reachTimeGen;
   private ValueGenerator failTimeGen;
   Set<DialerActionEvent> actionEvents = null;
   private Set<DialerActionEvent> umActionEvents = null;

   /**
    * Constructs a new dialer using the dialer policy \texttt{policy}, the
    * random stream \texttt{streamReach} to determine if a dialed call reaches
    * the right party, and with 0 reach and fail times.
    *
    * @param policy
    *           the dialer's policy being used.
    * @param streamReach
    *           the random number stream used to determine the success of a
    *           dial.
    * @param probReach
    *           the probability of reaching the right party.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public Dialer (DialerPolicy policy, RandomStream streamReach,
         ValueGenerator probReach) {
      this (Simulator.getDefaultSimulator (), policy, streamReach, probReach, new ZeroGenerator (),
            new ZeroGenerator ());
   }

   /**
    * Equivalent to {@link #Dialer(DialerPolicy,RandomStream,ValueGenerator)},
    * with the given user-defined simulator
    * \texttt{sim}.
    * @param sim the simulator attached to the dialer.
    * @param policy
    *           the dialer's policy being used.
    * @param streamReach
    *           the random number stream used to determine the success of a
    *           dial.
    * @param probReach
    *           the probability of reaching the right party.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public Dialer (Simulator sim, DialerPolicy policy, RandomStream streamReach,
         ValueGenerator probReach) {
      this (sim, policy, streamReach, probReach, new ZeroGenerator (),
            new ZeroGenerator ());
   }

   private static final class ZeroGenerator implements ValueGenerator {
      public void init () {}

      public double nextDouble (Contact contact) {
         return 0;
      }
   }

   /**
    * Constructs a new dialer using the dialer policy \texttt{policy}. The
    * random stream \texttt{streamReach} is used to determine if a call is
    * reached, \texttt{reachTimeGen} and \texttt{failTimeGen} compute the reach
    * and fail times, respectively, i.e., the simulation time between the call
    * to {@link #dial()} and the notification to the appropriate new-contact
    * listeners.
    *
    * @param policy
    *           the dialer policy being used.
    * @param streamReach
    *           the random number stream used to compute the status of a dial.
    * @param probReach
    *           the probability of successful contact.
    * @param reachTimeGen
    *           the value generator for the time between dialing and reaching.
    * @param failTimeGen
    *           the value generator for the time between dialing and failing.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public Dialer (DialerPolicy policy, RandomStream streamReach,
         ValueGenerator probReach, ValueGenerator reachTimeGen,
         ValueGenerator failTimeGen) {
      this (Simulator.getDefaultSimulator (),
            policy, streamReach, probReach, reachTimeGen, failTimeGen);
   }

   /**
    * Equivalent to {@link #Dialer(DialerPolicy,RandomStream,ValueGenerator,ValueGenerator,ValueGenerator)},
    * using the given simulator \texttt{sim}.
    * @param sim the simulator attached to the dialer.
    * @param policy
    *           the dialer policy being used.
    * @param streamReach
    *           the random number stream used to compute the status of a dial.
    * @param probReach
    *           the probability of successful contact.
    * @param reachTimeGen
    *           the value generator for the time between dialing and reaching.
    * @param failTimeGen
    *           the value generator for the time between dialing and failing.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public Dialer (Simulator sim, DialerPolicy policy, RandomStream streamReach,
         ValueGenerator probReach, ValueGenerator reachTimeGen,
         ValueGenerator failTimeGen) {
      if (sim == null)
         throw new NullPointerException
         ("The simulator must not be null");
      if (policy == null)
         throw new NullPointerException ("Dialer policy is null");
      if (streamReach == null)
         throw new NullPointerException (
               "Random stream for right party connect is null");
      if (probReach == null)
         throw new NullPointerException (
               "Value generator for probability of reaching is null");
      if (reachTimeGen == null)
         throw new NullPointerException ("Reach times generator is null");
      if (failTimeGen == null)
         throw new NullPointerException ("Fail times generator is null");

      this.sim = sim;
      this.policy = policy;
      this.streamReach = streamReach;
      this.probReach = probReach;
      this.reachTimeGen = reachTimeGen;
      this.failTimeGen = failTimeGen;
   }

   public Simulator simulator() {
      return sim;
   }

   public void setSimulator (Simulator sim) {
      if (sim == null)
         throw new NullPointerException
         ("The simulator must not be null");
      this.sim = sim;
   }

   public String getName () {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The name must not be null");
      this.name = name;
   }

   /**
    * Calls {@link #addReachListener(NewContactListener)}.
    *
    * @param listener
    *           the new-contact listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addNewContactListener (NewContactListener listener) {
      addReachListener (listener);
   }

   /**
    * Calls {@link #removeReachListener(NewContactListener)}.
    *
    * @param listener
    *           the new-contact listener being removed.
    */
   public void removeNewContactListener (NewContactListener listener) {
      removeReachListener (listener);
   }

   /**
    * Calls {@link #clearReachListeners()}.
    */
   public void clearNewContactListeners () {
      clearReachListeners ();
   }

   /**
    * Returns the result of {@link #getReachListeners()}.
    */
   public List<NewContactListener> getNewContactListeners () {
      return getReachListeners ();
   }

   /**
    * Adds the new-contact listener \texttt{listener} which will be notified
    * upon right party connects.
    *
    * @param listener
    *           the new-contact listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addReachListener (NewContactListener listener) {
      if (listener == null)
         throw new NullPointerException (
               "The new-contact listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!reachListeners.contains (listener))
         reachListeners.add (listener);
   }

   /**
    * Removes the new-contact listener \texttt{listener} from the list of
    * listeners being notified upon right party connects.
    *
    * @param listener
    *           the new-contact listener being removed.
    */
   public void removeReachListener (NewContactListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      reachListeners.remove (listener);
   }

   /**
    * Removes all new-contact listeners being notified when this dialer makes a
    * right party connect.
    */
   public void clearReachListeners () {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      reachListeners.clear ();
   }

   /**
    * Returns an unmodifiable list containing all the new-contact listeners
    * notified when a right-party connect occurs.
    *
    * @return the list of all registered new-contact listeners.
    */
   public List<NewContactListener> getReachListeners () {
      return umReachListeners;
   }

   /**
    * Adds the new-contact listener \texttt{listener} which will be notified
    * upon wrong party connects or connection failures.
    *
    * @param listener
    *           the new-contact listener being added.
    * @exception NullPointerException
    *               if \texttt{listener} is \texttt{null}.
    */
   public void addFailListener (NewContactListener listener) {
      if (listener == null)
         throw new NullPointerException (
               "The new-contact listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!failListeners.contains (listener))
         failListeners.add (listener);
   }

   /**
    * Removes the new-contact listener \texttt{listener} from the list of
    * listeners being notified upon wrong party connects or connection failures.
    *
    * @param listener
    *           the new-contact listener being removed.
    */
   public void removeFailListener (NewContactListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      failListeners.remove (listener);
   }

   /**
    * Removes all new-contact listeners being notified when this dialer fails to
    * make a contact.
    */
   public void clearFailListeners () {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      failListeners.clear ();
   }

   /**
    * Returns an unmodifiable list containing all the new-contact listeners
    * notified when the dialer fails making a contact.
    *
    * @return the list of all registered new-contact listeners.
    */
   public List<NewContactListener> getFailListeners () {
      return umFailListeners;
   }

   /**
    * Returns the dialing policy used by this dialer.
    *
    * @return the used dialer's policy.
    */
   public DialerPolicy getDialerPolicy () {
      return policy;
   }

   /**
    * Sets the dialing policy to \texttt{policy}.
    *
    * @param policy
    *           the new dialer's policy.
    * @exception NullPointerException
    *               if \texttt{policy} is \texttt{null}.
    */
   public void setDialerPolicy (DialerPolicy policy) {
      if (policy == null)
         throw new NullPointerException ("The dialer's policy must not be null");
      this.policy = policy;
   }

   /**
    * Returns the random stream used to determine if a called person is reached
    * or not.
    *
    * @return the stream used to determine if a called person is reached.
    */
   public RandomStream getStreamReach () {
      return streamReach;
   }

   /**
    * Sets the stream used to determine if a called person is reached to
    * \texttt{streamReach}.
    *
    * @param streamReach
    *           the new stream for success tests.
    * @exception NullPointerException
    *               if \texttt{streamReach} is \texttt{null}.
    */
   public void setStreamReach (RandomStream streamReach) {
      if (streamReach == null)
         throw new NullPointerException (
               "The random stream for reached contact must not be null");
      this.streamReach = streamReach;
   }

   /**
    * Returns the value generator for the probability of a call to be
    * successful, i.e., the probability of right party connect.
    *
    * @return the value generator for the reach probability.
    */
   public ValueGenerator getProbReachGenerator () {
      return probReach;
   }

   /**
    * Sets the value generator for right party connect probabilities to
    * \texttt{probReach}.
    *
    * @param probReach
    *           the value generator for right party connect probabilities.
    * @exception NullPointerException
    *               if \texttt{probReach} is \texttt{null}.
    */
   public void setProbReachGenerator (ValueGenerator probReach) {
      if (probReach == null)
         throw new NullPointerException (
               "The value generator for probability of right party connect must not be null");
      this.probReach = probReach;
   }

   /**
    * Returns the value generator for the reach times. A reach time corresponds
    * to the simulation time from the call to {@link #dial()} to the notification
    * of the successful call to the listeners.
    *
    * @return the value generator for reach times.
    */
   public ValueGenerator getReachTimeGenerator () {
      return reachTimeGen;
   }

   /**
    * Sets the value generator for reach times to \texttt{reachTimeGen}. If
    * \texttt{reachTimeGen} is \texttt{null}, the dial delay for successful
    * calls is reset to 0.
    *
    * @param reachTimeGen
    *           the value generator for reach times.
    */
   public void setReachTimeGenerator (ValueGenerator reachTimeGen) {
      if (reachTimeGen == null)
         this.reachTimeGen = new ZeroGenerator ();
      else
         this.reachTimeGen = reachTimeGen;
   }

   /**
    * Returns the value generator for the fail times. A fail time corresponds to
    * the simulation time from the call to {@link #dial()} to the notification of
    * the failed call to the listeners.
    *
    * @return the value generator for fail times.
    */
   public ValueGenerator getFailTimeGenerator () {
      return failTimeGen;
   }

   /**
    * Sets the value generator for fail times to \texttt{failTimeGen}. If
    * \texttt{reachTimeGen} is \texttt{null}, the dial delay for successful
    * calls is reset to 0.
    *
    * @param failTimeGen
    *           the value generator for fail times.
    */
   public void setFailTimeGenerator (ValueGenerator failTimeGen) {
      if (failTimeGen == null)
         this.failTimeGen = new ZeroGenerator ();
      else
         this.failTimeGen = failTimeGen;
   }

   /**
    * Determines if the call represented by \texttt{contact} is a right party
    * connect. Returns \texttt{true} if the call is successful, or
    * \texttt{false} otherwise.
    *
    * The default implementation uses the random stream returned by
    * {@link #getStreamReach()} to return \texttt{true} with some probability. The
    * probability of right party connect is generated using the value generator
    * returned by {@link #getProbReachGenerator()}.
    *
    * @param contact
    *           the contact being tested.
    * @return the success indicator.
    */
   public boolean isSuccessful (Contact contact) {
      final double u = streamReach.nextDouble ();
      return u <= probReach.nextDouble (contact);
   }

   /**
    * Determines if the {@link #dial()} method subtracts the
    * number of action events returned by {@link #getNumActionEvents()}
    * from the return value of {@link DialerPolicy#getNumDials(Dialer)}
    * in order to determine the number of calls to dial.
    * When dial delays are large enough for the dialer to start
    * often while phone numbers are being composed,
    * the agents of the contact center might receive too many
    * calls to serve, which results in a large number of mismatches.
    * If this flag is enabled (the default), the dialer will take
    * into account
    * the number of calls  for which dialing is in progress while
    * determining the number of additional calls to dial.
    *
    * @return \texttt{true} if the number of action events must
    * be taken into account while dialing.
    */
   public boolean isUsingNumActionsEvents() {
      return useNumActionEvents;
   }

   /**
    * Sets the flag for taking the number of action events
    * into account while dialing to \texttt{useNumActionEvents}.
    * @param useNumActionEvents the new value of the flag.
    * @see #isUsingNumActionsEvents()
    */
   public void setUsingNumActionEvents (boolean useNumActionEvents) {
      this.useNumActionEvents = useNumActionEvents;
   }

   /**
    * Instructs the dialer to try performing outbound calls. This should be
    * called at the end of a service, or at any time the number of agents
    * capable of serving outbound calls increases. This method does nothing if
    * the dialer is disabled.
    *
    * The method uses the dialer's policy to get the appropriate number of calls
    * to dial as well as the dialer list. The contact objects representing the
    * calls being made are removed from the dialer list, and each call is tested
    * using {@link #isSuccessful(Contact)}. After the success indicator is determined, a
    * corresponding dial delay is generated, and an event is scheduled to happen
    * if the delay is non-zero. After the delay is elapsed, the appropriate
    * new-contact listeners are notified about the new call.
    */
   public void dial () {
      if (!enabled)
         return;
      int numDials = policy.getNumDials (this);
      if (numDials <= 0)
         return;
      final DialerList list = policy.getDialerList (this);
      numDials = Math.min (list.size (null), numDials);

      for (int i = 0; i < numDials; i++) {
         final Contact contact;
         //try {
            contact = list.removeFirst (null);
//         }
//         catch (NoSuchElementException nse) {
//            return;
//         }
         if (contact.getSource () == null)
            // In case of callbacks, the arrival process
            // of the contact is already set.
            // In this case, an information is lost
            // (the dialer which called back).
            contact.setSource (this);
         assert contact.simulator() == sim;
         final boolean success = isSuccessful (contact);
         final List<NewContactListener> listeners = success ? reachListeners
               : failListeners;
         if (!listeners.isEmpty ()) {
            final double actionTime = success ? reachTimeGen
                  .nextDouble (contact) : failTimeGen.nextDouble (contact);
            if (actionTime <= 0)
               notifyListeners (contact, success);
            else {
               final DialerActionEvent ev = new DialerActionEvent (this,
                     contact, success);
               ev.schedule (actionTime);
            }
         }
      }
   }

   /**
    * Notifies registered new-contact listeners
    * about the success or failure of the
    * contact \texttt{contact}.
    * @param contact the contact to broadcast.
    * @param success the success indicator.
    */
   protected void notifyListeners (Contact contact, boolean success) {
      final List<NewContactListener> listeners = success ? reachListeners
            : failListeners;
      final int nl = listeners.size ();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int l = 0; l < nl; l++)
            listeners.get (l).newContact (contact);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   /**
    * Stops any ongoing dialing of calls. This can be called when the simulation
    * program knows that if the called persons are reached, a mismatch will
    * occur. Cancelled calls are notified as failed calls to the appropriate
    * listeners if dialer action events are kept. However, if the dialer does
    * not keep track of the action events, cancelled calls are lost without any
    * notification.
    */
   public void stopDial () {
      ++initCount;
      if (numActionEvents == 0)
         return;
      // We must use an iterator instead of a for-each loop,
      // because a listener might examine
      // the set of action events. Consequently,
      // the set must be updated each time
      // a new event is removed.
      for (final Iterator<DialerActionEvent> it = dialerActionEventsIterator ();
      it.hasNext ();) {
         final DialerActionEvent ev = it.next ();
         it.remove ();
         ev.cancel ();
         final int nl = failListeners.size ();
         for (int i = 0; i < nl; i++)
            failListeners.get (i).newContact (ev.getContact ());
      }
   }

   /**
    * Saves the state of this dialer and returns a state object containing the
    * information.
    *
    * @return the state of the dialer.
    */
   public DialerState save () {
      return new DialerState (this);
   }

   /**
    * Restores the state of this dialer with state information included in
    * \texttt{state}.
    *
    * @param state
    *           the saved state of the dialer.
    */
   public void restore (DialerState state) {
      state.restore (this);
   }

   public void init () {
      if (enabled)
         stop ();
      // If two dialers share the same associated objects, we call
      // init for each, which is less efficient.
      // Problems can happen if two dialers are not initialized at the same
      // time.
      // However, not calling init here will clutter
      // the application programs code.
      policy.init (this);
      probReach.init ();
      reachTimeGen.init ();
      failTimeGen.init ();
      ++initCount;
      if (actionEvents != null)
         actionEvents.clear ();
      numActionEvents = 0;
      Arrays.fill (numActionEventsTypes, 0);
   }

   /**
    * Determines if this dialer is keeping the action events. If this returns
    * \texttt{true}, the {@link #getActionEvents()} method can be used to return a
    * set containing the events. Otherwise, the action events are stored in the
    * event list only, and cannot be enumerated by the dialer. By default, the
    * events are not stored by the dialer.
    *
    * @return the keep action events indicator.
    */
   public boolean isKeepingActionEvents () {
      return actionEvents != null;
   }

   /**
    * Sets the keep-dial-events indicator to \texttt{keepActionEvents}.
    *
    * @param keepActionEvents
    *           the new value of the indicator.
    * @see #isKeepingActionEvents()
    */
   public void setKeepingActionEvents (boolean keepActionEvents) {
      if (keepActionEvents && actionEvents == null) {
         if (numActionEvents > 0) {
            final Iterator<DialerActionEvent> it = dialerActionEventsIterator ();
            actionEvents = new LinkedHashSet<DialerActionEvent> ();
            while (actionEvents.size () < numActionEvents && it.hasNext ())
               actionEvents.add (it.next ());
         }
         else
            actionEvents = new LinkedHashSet<DialerActionEvent> ();
         umActionEvents = Collections.unmodifiableSet (actionEvents);
      }
      else if (!keepActionEvents)
         if (actionEvents != null) {
            // Another object might still have
            // a reference to the set of
            // action events we just discard.
            // As a result, this is important
            // to clear it for the external
            // object to get an updated (empty) set.
            actionEvents.clear ();
            actionEvents = null;
            umActionEvents = null;
         }
   }

   /**
    * Constructs and returns an iterator for the dialer-action events. If
    * {@link #isKeepingActionEvents()} returns \texttt{true}, the iterator is
    * constructed from the set returned by {@link #getActionEvents()}.
    * Otherwise, an iterator traversing the event list and filtering the
    * appropriate events is constructed and returned.
    *
    * @return the iterator for dialer-action events.
    */
   public Iterator<DialerActionEvent> dialerActionEventsIterator () {
      if (actionEvents == null)
         return new FilteredIterator<DialerActionEvent> (simulator()
               .getEventList ().iterator (), numActionEvents) {
            @Override
            public boolean filter (Object ev) {
               if (ev instanceof DialerActionEvent) {
                  final DialerActionEvent dev = (DialerActionEvent) ev;
                  if (dev.getDialer () == Dialer.this)
                     return true;
               }
               return false;
            }
         };
      else
         return getActionEvents().iterator ();
   }

   /**
    * Returns a set containing all the currently scheduled
    * {@link DialerActionEvent} objects. If the dialer does not keep track of
    * these events, an {@link IllegalStateException} is thrown.
    *
    * @return the set of dialer action events.
    */
   public Set<DialerActionEvent> getActionEvents () {
      if (actionEvents == null)
         throw new IllegalStateException (
               "Action events are not kept by this dialer");
      return umActionEvents;
   }

   /**
    * Returns the number of action events currently scheduled by
    * this dialer.
    * This corresponds to the number of calls
    * the dialer is currently attempting.
    * @return the current number of action events.
    */
   public int getNumActionEvents() {
      return numActionEvents;
   }

   public int getNumActionEvents (int k) {
      if (k < 0 || k >= numActionEventsTypes.length)
         return 0;
      return numActionEventsTypes[k];
   }

   public boolean isStarted () {
      return enabled;
   }

   public void start () {
      startNoDial ();
      dial ();
   }

   /**
    * This is the same as {@link #start()}, except that no call to {@link #dial()}
    * is made after the dialer is started. {@link #dial()} will then be called
    * only when an agent becomes free.
    */
   public void startNoDial () {
      if (enabled)
         throw new IllegalStateException ("Dialer already started");
      enabled = true;
      policy.dialerStarted (this);
   }

   public void stop () {
      if (!enabled)
         throw new IllegalStateException ("Dialer already stopped");
      enabled = false;
      policy.dialerStopped (this);
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      if (getName ().length () > 0)
         sb.append ("name: ").append (name).append (", ");
      sb.append ("dialer policy: ").append (policy.toString ());
      sb.append (']');
      return sb.toString ();
   }
}
