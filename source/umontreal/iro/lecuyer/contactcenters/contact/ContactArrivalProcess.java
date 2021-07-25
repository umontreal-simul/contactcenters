package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a contact arrival process modeling
 * the arrival of inbound contacts.
 * Such a process schedules an event for each new contact, and
 * broadcasts the arrival to any registered new-contact listeners.
 * More specifically, a single simulation event manages arrivals as follows:
 * upon an arrival, a new contact is instantiated using
 * the associated contact factory, the contact is broadcast to
 * any registered listener,
 * and the next arrival is scheduled.
 * The interarrival times are computed using the {@link #nextTime()} method
 * which needs to be implemented in a concrete subclass.
 * This abstract class also takes care of new-contact
 * listeners registration and notification.
 * Subclasses only needs to define {@link #nextTime()} and
 * optionally {@link #init()} which initializes the
 * arrival process at the beginning of the simulation.
 * It is also possible to access
 * the scheduled new-contact event to reschedule or cancel it as needed.
 * Implementing {@link #getArrivalRate(int)}, and {@link #getExpectedArrivalRate(int)}
 * is recommended to allow programs to get the arrival rate and expected
 * arrival rate.
 *
 * Each arrival process has an associated simulator which
 * is an instance of the {@link Simulator} class.
 * This simulator is used
 * to schedule the event managing the arrival process.
 * It is also assumed that the user-defined contact factory attaches this
 * simulator to each new contact.
 * Failing to meet this condition might lead to unexpected behavior, and
 * will trigger a failed assertion if assertion checking is turned on during
 * execution.
 *
 * The arrival process can be inflated or deflated by
 * a \emph{busyness factor} denoted $B$,
 * a random variable with mean 1, and usually
 * generated once for a day.
 * Any arrival process can be defined as
 * $\{N(t), t\ge 0\}$, where $N(t)$ is the
 * number of arrivals during the time interval $[0,t)$.
 * The process affected by the busyness, $\{\tilde N(t), t\ge 0\}$,
 * is given by taking $\tilde N(t) = \mathrm{round}(BN(t))$,
 * where $\mathrm{round}(\cdot)$ rounds its argument to the nearest integer.
 * The exact way to take account of the busyness factor
 * depends on the specific arrival process.
 * For example, for Poisson processes, the busyness
 * is used to inflate or deflate the $\lambda$ arrival rate.
 *
 * The busyness factor must be set externally, because the value
 * of $B$ for this arrival process is often correlated with $B$ for
 * other arrival processes.
 * The recommended way to set $B$ is using {@link #init(double)}.
 * The current value of $B$ might be obtained using {@link #getBusynessFactor()}.
 * By default, it is assumed that $\E[B]=1$.  If this is not true for
 * a particular model, one should call {@link #setExpectedBusynessFactor(double)}
 * to set the expectation of the factor.
 *
 * Note: the {@link NewContactListener} implementations
 * are notified in the order of the list
 * returned by {@link #getNewContactListeners()},
 * and a new-contact listener
 * modifying the list of listeners by using {@link #addNewContactListener(NewContactListener)}
 * or {@link #removeNewContactListener(NewContactListener)} could result in
 * unpredictable behavior.
 */
public abstract class ContactArrivalProcess
   implements ContactSource {
   private Simulator sim;
   private String name = "";
   private ContactFactory factory;
   private final List<NewContactListener> listeners = new ArrayList<NewContactListener>();
   private final List<NewContactListener> umListeners = Collections.unmodifiableList (listeners);
   private boolean broadcastInProgress;
   private boolean enabled = false;
   private double m_b = 1.0;
   private double bMean;

   /**
    * Event representing the arrival of a new contact.
    * Subclasses can cancel or reschedule this event
    * to adjust it when a parameter change occurs.
    */
   protected final Event contactEvent;

   /**
    * Constructs a new contact arrival process creating contacts
    * using the given \texttt{factory}.
    @param factory the factory creating contacts for this arrival process.
    @exception NullPointerException if \texttt{factory} is \texttt{null}.
    */
   public ContactArrivalProcess (ContactFactory factory) {
      this (Simulator.getDefaultSimulator (), factory);
   }

   /**
    * Equivalent to {@link #ContactArrivalProcess(ContactFactory)},
    * with a user-defined simulator \texttt{sim}.
    * @param sim the simulator attached to this arrival process.
    @param factory the factory creating contacts for this arrival process.
    @exception NullPointerException if \texttt{sim} or \texttt{factory} are \texttt{null}.
    */
   public ContactArrivalProcess (Simulator sim, ContactFactory factory) {
      if (sim == null)
         throw new NullPointerException ("The simulator must not be null");
      if (factory == null)
         throw new NullPointerException ("The given contact factory must not be null");
      this.factory = factory;
      this.sim = sim;
      contactEvent = new ContactEvent (sim);
   }

   public Simulator simulator() {
      return sim;
   }

   public void setSimulator (Simulator sim) {
      if (sim == null)
         throw new NullPointerException
         ("The simulator cannot be null");
      contactEvent.setSimulator (sim);
      this.sim = sim;
   }

   public String getName() {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
   }

   /**
    * Computes and returns the time before the next contact
    * arrival is simulated by this object.
    * If this method returns \texttt{Double.POSITIVE\_INFINITY},
    * no more arrival events will be scheduled until
    * the arrival process is reinitialized.
    @return the time before the next arrival.
    */
   public abstract double nextTime();

   /**
    * Returns the currently used busyness factor $B$,
    * which
    * must be greater than or equal to 0, and
    * defaults to 1.
    @return the current busyness factor.
    */
   public double getBusynessFactor() {
      return m_b;
   }

   /**
    * Sets the busyness factor to \texttt{b}.
    * This method should be called before
    * {@link #init()} is called, or one should
    * use {@link #init(double)}.
    * @param b the new busyness factor.
    * @exception IllegalArgumentException if \texttt{b}
    * is negative.
    */
   public void setBusynessFactor (double b) {
      if (b < 0)
         throw new IllegalArgumentException
         ("b must not be negative");
      this.m_b = b;
   }

   /**
    * Returns the expected value of the busyness factor
    * for this arrival process.
    * @return the expected value of the busyness factor.
    */
   public double getExpectedBusynessFactor () {
      return bMean;
   }

   /**
    * Sets the expected busyness factor for this
    * arrival process to \texttt{bMean}.
    * @param bMean the new value of the expectation.
    * @exception IllegalArgumentException if \texttt{bMean} is negative.
    */
   public void setExpectedBusynessFactor (double bMean) {
      if (bMean < 0)
         throw new IllegalArgumentException
         ("bMean < 0");
      this.bMean = bMean;
   }

   /**
    * Initializes this process
    * with a specific busyness factor $B=$~\texttt{b}.
    * By default, this method simply calls
    * {@link #setBusynessFactor(double)} followed by
    * {@link #init()}.
    @param b the value of the busyness factor.
    @exception IllegalArgumentException if \texttt{b} $\le 0$.
    */
   public void init (double b) {
      setBusynessFactor (b);
      init();
   }

   /**
    * Initializes the new arrival process.
    * If this method is overridden by a subclass, it
    * is important to call \texttt{super.init()} in order to
    * ensure that everything is initialized correctly.
    */
   public void init() {
      // If we want to call stop, we must
      // document it somewhere, because
      // this could affect subclasses overriding stop.
      // Or we can make stop final.
      contactEvent.cancel();
      enabled = false;
   }

   /**
    * Returns a reference to the associated
    * contact factory.  This factory is used
    * to instantiate contact objects.
    @return the associated contact factory.
    */
   public ContactFactory getContactFactory() {
      return factory;
   }

   /**
    * Sets the contact factory to \texttt{factory}.
    * This new contact factory will be used to instantiate future contact
    * objects.
    @param factory the new contact factory.
    @exception NullPointerException if the given contact factory is \texttt{null}.
    */
   public void setContactFactory (ContactFactory factory) {
      if (factory == null)
         throw new NullPointerException ("The given contact factory must not be null");
      this.factory = factory;
   }

   public void addNewContactListener (NewContactListener listener) {
      if (listener == null)
         throw new NullPointerException ("The given listener must not be null");
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      if (!listeners.contains (listener))
         listeners.add (listener);
   }

   public void removeNewContactListener (NewContactListener listener) {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.remove (listener);
   }

   public void clearNewContactListeners() {
      if (broadcastInProgress)
         throw new IllegalStateException
         ("Cannot modify the list of listeners while broadcasting");
      listeners.clear();
   }

   public List<NewContactListener> getNewContactListeners() {
      return umListeners;
   }

   public void start() {
      if (enabled)
         throw new IllegalStateException
            ("Arrival process already started");
      final double nt = nextTime();
      if (!Double.isInfinite (nt))
         contactEvent.schedule (nt);
      enabled = true;
   }

   /**
    * Setup the arrival process to be stationary, and
    * starts it using the {@link #start()}
    * method.
    * When an arrival process is started using
    * this method, its parameters do not evolve
    * with time. This can be useful, e.g., to
    * simulate a single period as if it was infinite
    * in the model.
    * If the arrival process does not
    * support stationary mode, this
    * method throws an
    * unsupported-operation exception.
    * The default behavior of this method
    * is to throw this exception.
    */
   public void startStationary() {
      throw new UnsupportedOperationException
      ("Arrival process does not support stationary mode");
   }

   /**
    * Starts this arrival process and schedules the first arrival
    * to happen after \texttt{delay} simulation time units,
    * independently of how {@link #nextTime()}
    * is implemented.  If \texttt{delay} is set to
    * {@link Double#POSITIVE_INFINITY}, no arrival
    * is scheduled.
    * Any subsequent inter-arrival
    * times will be generated with {@link #nextTime()}
    * as usual.
    * @param delay the first inter-arrival time.
    */
   public void start (double delay) {
      if (enabled)
         throw new IllegalStateException
            ("Arrival process already started");
      if (!Double.isInfinite (delay))
         contactEvent.schedule (delay);
      enabled = true;
   }

   public void stop() {
      if (!enabled)
         throw new IllegalStateException
            ("Arrival process already stopped");
      contactEvent.cancel();
      enabled = false;
   }

   public boolean isStarted() {
      return enabled;
   }

   /**
    * Returns the simulation time of the next
    * arrival currently scheduled by this arrival
    * process.  If the arrival process is stopped or
    * no arrival is scheduled, this returns a negative
    * number.
    * @return the arrival time of the next contact.
    */
   public double getNextArrivalTime() {
      return contactEvent.time();
   }

   /**
    * Determines the arrival rate in period \texttt{p} for this
    * arrival process.
    * The arrival rate corresponds to the expected number of arrivals
    * per simulation time unit during the specified period;
    * one must multiply the rate by the period duration
    * to get the expected number of arrivals during the period.
    *
    * If arrival rate is random, this returns the arrival rate
    * for the current replication.
    * One should use {@link #getExpectedArrivalRate(int)}
    * or {@link #getExpectedArrivalRateB(int)} to get the
    * expected arrival rate.
    *
    * If the arrival rate is not available,
    * throws an {@link UnsupportedOperationException}.
    @param p the queried period index.
    @return the arrival rate in that period.
    */
   public double getArrivalRate (int p) {
      throw new UnsupportedOperationException
         ("Unknown arrival rate");
   }

   /**
    * Fills the given array \texttt{rates} with the
    * arrival rate for each period.
    * After this method returns,
    * element \texttt{rates[p]}
    * corresponds to the value
    * returned by {@link #getArrivalRate(int) getArrivalRate (p)}.
    * @param rates the array filled with rates.
    */
   public void getArrivalRates (double[] rates) {
      for (int p = 0; p < rates.length; p++)
         rates[p] = getArrivalRate (p);
   }

   /**
    * Determines the expected arrival rate in period \texttt{p} for this
    * arrival process assuming that the expected value of the busyness factor is 1.
    * The arrival rate corresponds to the expected number of arrivals
    * per simulation time unit during the specified period;
    * one must multiply the rate by the period duration
    * to get the expected number of arrivals during the period.
    * If arrival rates are deterministic, this returns the same value
    * as {@link #getArrivalRate(int)}.
    *
    * If $\E[B]\ne 1$, one should use {@link #getExpectedArrivalRateB(int)}
    * which takes the expectation of the busyness factor into account.
    *
    * If the expected arrival rate is not available,
    * throws an {@link UnsupportedOperationException}.
    * This is the default behavior of this
    * method if not overridden by a subclass.
    @param p the queried period index.
    @return the expected arrival rate in that period.
    */
   public double getExpectedArrivalRate (int p) {
      throw new UnsupportedOperationException
         ("Unknown arrival rate");
   }

   /**
    * Fills the given array \texttt{rates} with the
    * expected arrival rate for each period.
    * After this method returns,
    * element \texttt{rates[p]}
    * corresponds to the value
    * returned by {@link #getExpectedArrivalRate(int) getExpectedArrivalRate (p)}.
    * @param rates the array filled with rates.
    */
   public void getExpectedArrivalRates (double[] rates) {
      for (int p = 0; p < rates.length; p++)
         rates[p] = getExpectedArrivalRate (p);
   }

   /**
    * Returns the expected arrival rate considering
    * the current expected busyness factor.
    * This corresponds to the product of the value returned
    * by {@link #getExpectedArrivalRate(int)}, and
    * the value returned by {@link #getExpectedBusynessFactor()}.
    * @param p the tested period.
    * @return the tested arrival rate.
    */
   public double getExpectedArrivalRateB (int p) {
      double exp = getExpectedArrivalRate (p);
      return exp * bMean;
   }

   /**
    * Fills the given array \texttt{rates} with the
    * expected arrival rate for each period.
    * After this method returns,
    * element \texttt{rates[p]}
    * corresponds to the value
    * returned by {@link #getExpectedArrivalRateB(int) getExpectedArrivalRateB (p)}.
    * @param rates the array filled with rates.
    */
   public void getExpectedArrivalRatesB (double[] rates) {
      for (int p = 0; p < rates.length; p++)
         rates[p] = getExpectedArrivalRateB (p);
   }

   /**
    * Determines the mean arrival rate
    * in time interval $[s,e]$.
    * The arrival rate corresponds to the expected number of arrivals
    * per simulation time unit during the specified interval;
    * one must multiply the rate by the interval length
    * to get the expected number of arrivals during the interval.
    * If $\lambda(t)$ is the arrival rate at time $t$, this
    * method returns the result of
    * \[
    * \int_s^e\lambda(t)dt / (e - s).
    * \]
    *
    * If arrival rate is random, this returns the arrival rate
    * for the current replication.
    * One should use {@link #getExpectedArrivalRate(double,double)}
    * or {@link #getExpectedArrivalRateB(double,double)} to get the
    * expected arrival rate.
    *
    * This method returns 0 if $e\le s$.
    *
    * If the arrival rate is not available,
    * throws an {@link UnsupportedOperationException}.
    * This is the default behavior of this
    * method if not overridden by a subclass.
    * @param st the starting time $s$.
    * @param et the ending time $e$.
    @return the arrival rate in the given time interval.
    */
   public double getArrivalRate (double st, double et) {
      throw new UnsupportedOperationException
         ("Unknown arrival rate");
   }

   /**
    * Determines the expected mean arrival rate in
    * time interval $[s,e]$ for this
    * arrival process assuming that the expected value of the busyness factor is 1.
    * The arrival rate corresponds to the expected number of arrivals
    * per simulation time unit during the specified interval;
    * one must multiply the rate by the interval length
    * to get the expected number of arrivals during the interval.
    * If arrival rates are deterministic, this returns the same value
    * as {@link #getArrivalRate(double,double)}.
    * If $\lambda(t)$ is the arrival rate at time $t$, this
    * method returns
    * \[
    * \int_s^e\E[\lambda(t)]dt / (e - s).
    * \]
    *
    * If $\E[B]\ne 1$, one should use {@link #getExpectedArrivalRateB(double,double)}
    * which takes the expectation of the busyness factor into account.
    *
    * This method returns 0 if $e\le s$.
    *
    * If the expected arrival rate is not available,
    * throws an {@link UnsupportedOperationException}.
    * This is the default behavior of this
    * method if not overridden by a subclass.
    * @param st the starting time $s$.
    * @param et the ending time $e$.
    @return the expected arrival rate in the given time interval.
    */
   public double getExpectedArrivalRate (double st, double et) {
      throw new UnsupportedOperationException
         ("Unknown arrival rate");
   }

   /**
    * Returns the expected mean arrival rate considering
    * the current expected busyness factor.
    * This corresponds to the product of the value returned
    * by {@link #getExpectedArrivalRate(double,double)}, and
    * the value returned by {@link #getExpectedBusynessFactor()}.
    * @param st the starting time $s$.
    * @param et the ending time $e$.
    @return the expected arrival rate in the given time interval.
    */
   public double getExpectedArrivalRateB (double st, double et) {
      double exp = getExpectedArrivalRate (st, et);
      return exp * bMean;
   }


   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      if (getName().length() > 0)
         sb.append ("name: ").append (getName()).append (", ");
      sb.append ("enabled: ");
      sb.append (enabled ? "yes" : "no");
      sb.append (", contact factory: ").append (factory.toString());
      sb.append (", busyness factor: ").append (m_b);
      sb.append (']');
      return sb.toString();
   }

   /**
    * Notifies the contact \texttt{contact} to
    * every registered listener.
    * @param contact the contact to be notified.
    */
   public void notifyNewContact (Contact contact) {
      final int nl = listeners.size();
      if (nl == 0)
         return;
      final boolean old = broadcastInProgress;
      broadcastInProgress = true;
      try {
         for (int i = 0; i < nl; i++)
            listeners.get (i).newContact (contact);
      }
      finally {
         broadcastInProgress = old;
      }
   }

   private final class ContactEvent extends Event {
      public ContactEvent (Simulator sim) {
         super (sim);
      }

      @Override
      public void actions() {
         final Contact contact = factory.newInstance();
         assert contact.simulator () == simulator();
         contact.setSource (ContactArrivalProcess.this);
         notifyNewContact (contact);
         final double nt = nextTime();
         if (!Double.isInfinite (nt))
            schedule (nt);
      }

      @Override
      public String toString() {
         return getClass().getSimpleName() +
            "[arrival process: " + ContactCenter.toShortString
            (ContactArrivalProcess.this) + "]";
      }
   }
}
