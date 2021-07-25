package umontreal.iro.lecuyer.contactcenters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.util.Misc;

/**
 * Defines a simulation event that occurs upon
 * period changes
 * and supporting fixed-sized or variable-sized
 * periods.
 * Because this event must cover the
 * complete
 * simulation horizon (day, week, etc.), not only the times at which the contact
 * center is
 * opened, three types of periods need to be defined.
 *
 * The $P$ periods during which the contact center is
 * opened are denoted \emph{main periods}, or simply periods.  Main
 * period $p$, where
 * $p=1,\ldots,P$, corresponds to simulation time interval $[t_{p-1}, t_p[$,
 * where $t_0 < \cdots < t_P$.
 * During the \emph{preliminary
 * period} $[0, t_0)$, the contact center is
 * closed.
 * Sometimes, arrivals start at time $t_0-\tau$ for a queue to build up
 * before agents enter into service.
 * During the \emph{wrap-up period} $[t_P, T]$, no more
 * arrival occurs, but ongoing services are terminated.
 * Note that preliminary and wrap-up periods are more useful
 * when a simulation replication corresponds to a day.
 *
 * Before starting the simulation, the period-change event
 * should be initialized by calling {@link #init()}, which
 * resets the current period index.
 * The event needs to be started by using {@link #start()};
 * this schedules it at the beginning of the first main period.
 * This also schedules auxiliary event managing
 * period changes at the other periods.
 * It is recommended to start the period-change
 * event before scheduling any other event to
 * ensure that period changes have priority
 * over other events.
 * When the period change occurs, it is
 * notified to any registered {@link PeriodChangeListener} implementation.
 * When returning from \texttt{Sim.start} or just before
 * calling \texttt{Sim.stop}, it is recommended to call
 * the {@link #stop()} method of this object, since the end
 * of the wrap-up period is not scheduled as an event.
 * This notifies all registered listeners about the
 * end of the simulation, and disables
 * any remaining auxiliary event.  This can be useful for
 * some statistical collectors.
 *
 * Note: the {@link PeriodChangeListener} implementations
 * are notified in the order of the list returned by
 * {@link #getPeriodChangeListeners()}, and a period-change listener
 * modifying the list of listeners by using
 * {@link #addPeriodChangeListener(PeriodChangeListener)} or
 * {@link #removePeriodChangeListener(PeriodChangeListener)}
 * could result in unpredictable behavior.
 */
public class PeriodChangeEvent extends Event implements Initializable, Named, ToggleElement {
   /**
    * Default priority of period-change events.
    * This priority index is set to 0.1, because the period-change event
    * should execute before any other event at the same time.
    */
   public static final double PRIORITY = 0.1;
   private String name = "";
   private double[] endingTimes = null;
   private int currentPeriod;
   private final List<PeriodChangeListener> pclList = new ArrayList<PeriodChangeListener>();
   private final List<PeriodChangeListener> umPclList = Collections.unmodifiableList (pclList);
   private double periodDuration;
   private double stopTime = -1;
   private boolean lockedPeriod = false;
   private boolean started = false;
   private int modCount = 0;

   /**
    * Constructs a new period-change event
    * with fixed-sized main periods of duration
    * \texttt{periodDuration}, a total of $P+2=$~\texttt{numPeriods} periods,
    * with the first main period
    * beginning at time $t_0=$~\texttt{startingTime}, and
    * using the default simulator.  For the event to be used,
    * there must be at least two periods (preliminary and wrap-up).
    * With a total of \texttt{numPeriods} periods, the event defines
    * \texttt{numPeriods - 2} main periods, a preliminary, and a wrap-up periods,
    * even if the given starting time is 0.
    *
    * Note that this constructor calls the
    * {@link #setFixedPeriods(double,double)}
    * method.
    @param periodDuration the length of each period, in simulation time units.
    @param numPeriods the total number of periods $P+2$.
    @param startingTime the beginning of the first period.
    @exception IllegalArgumentException if the number of periods
    is smaller than 2 or the period duration or starting time is
    smaller than 0.
    */
   public PeriodChangeEvent (double periodDuration, int numPeriods,
         double startingTime) {
      this (Simulator.getDefaultSimulator (), periodDuration, numPeriods, startingTime);
   }

   /**
    * Equivalent to {@link #PeriodChangeEvent(double,int,double)},
    * with a user-defined simulator \texttt{sim}.
    @param sim the simulator attached to the new event.
    @param periodDuration the length of each period, in simulation time units.
    @param numPeriods the total number of periods $P+2$.
    @param startingTime the beginning of the first period.
    @exception IllegalArgumentException if
    \texttt{sim} is \texttt{null}, if the number of periods
    is smaller than 2, or the period duration or starting time is
    smaller than 0.
    */
   public PeriodChangeEvent (Simulator sim,
         double periodDuration, int numPeriods,
         double startingTime) {
      super (sim);
      priority = PRIORITY;
      if (numPeriods < 2)
         throw new IllegalArgumentException
            ("There must be at least two periods");
      endingTimes = new double[numPeriods-1];
      setFixedPeriods (periodDuration, startingTime);
      currentPeriod = 0;
   }

   /**
    * Constructs a new period-change event
    * with variable-sized periods,
    * using the default simulator.  The object
    * will support \texttt{endingTimes.length + 1} periods
    * where $t_p=$~\texttt{endTimes[p]}.
    * The ending times in the array must be non-decreasing,
    * otherwise an {@link IllegalArgumentException}
    * is thrown.
    *
    * This constructor accepts a variable number of arguments, i.e., one
    * can use \texttt{new PeriodChangeEvent (t1, t2, t3, t4, t5)},
    * where \texttt{tN} are ending times.
    * One can also pass a regular array.
    *
    * Note that this constructor calls the
    * {@link #setEndingTimes(double[])} method.
    @param endingTimes the ending times of periods.
    @exception IllegalArgumentException if one ending time is negative
    or the ending times are not non-decreasing.
    */
   public PeriodChangeEvent (double... endingTimes) {
      this (Simulator.getDefaultSimulator (), endingTimes);
   }

   /**
    * Equivalent to {@link #PeriodChangeEvent(double[])},
    * with a user-defined simulator \texttt{sim}.
    @param sim the simulator attached to this event.
    @param endingTimes the ending times of periods.
    @exception IllegalArgumentException if \texttt{sim}
    is \texttt{null}, or if one ending time is negative
    or the ending times are not non-decreasing.
    */
   public PeriodChangeEvent (Simulator sim, double... endingTimes) {
      super (sim);
      priority = PRIORITY;
      this.endingTimes = new double[endingTimes.length];
      // The check for non-decreasing ending times is made in setEndingTimes
      setEndingTimes (endingTimes);
      currentPeriod = 0;
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
    * Sets this period-change event to fixed-length periods of
    * duration \texttt{periodDuration}, and with main period
    * starting at $t_0=$~\texttt{startingTime}.
    * It should not be used during a simulation replication.
    * It is not allowed to modify the number of periods
    * as many other objects depend on this parameter.
    @param periodDuration the length of each main period.
    @param startingTime the starting time of the first main period.
    @exception IllegalArgumentException if one argument
    is negative.
    */
   public final void setFixedPeriods (double periodDuration, double startingTime) {
      if (periodDuration <= 0)
         throw new IllegalArgumentException
            ("The period duration must be greater than 0");
      if (startingTime < 0)
         throw new IllegalArgumentException
            ("The starting time must be greater than or equal to 0");
      endingTimes[0] = startingTime;
      cancel();
      for (int i = 1; i < endingTimes.length; i++)
         endingTimes[i] = endingTimes[0] + i*periodDuration;
      this.periodDuration = periodDuration;
   }

   /**
    * Changes the ending times of periods
    * to \texttt{endingTimes}.  This should not be used during
    * a simulation replication, otherwise the period-change
    * event will be cancelled and no more
    * period change will be notified to listeners.
    * It is also not allowed to change the number of periods,
    * because many objects can depend on this number.
    @param endingTimes the new period ending times.
    @exception IllegalArgumentException if one ending time is negative,
    the ending times are not non-decreasing, or one tries to change
    the number of periods.
    */
   public final void setEndingTimes (double... endingTimes) {
      // Since this is called from constructor, we cannot
      // allow inheritance for this method.
      if (this.endingTimes.length != endingTimes.length)
         throw new IllegalArgumentException
            ("Cannot change the number of periods");
      if (endingTimes[0] < 0)
         throw new IllegalArgumentException
            ("The ending times must not be negative");
      double lastTime = endingTimes[0];
      for (int i = 1; i < endingTimes.length; i++) {
         if (endingTimes[i] < lastTime)
            throw new IllegalArgumentException
               ("endTimes[" + i + "] is smaller than endTimes[" + (i-1) + "]");
         lastTime = endingTimes[i];
      }
      cancel();
      System.arraycopy (endingTimes, 0, this.endingTimes, 0, endingTimes.length);
      periodDuration = -1;
   }

   /**
    * Registers the period-change listener \texttt{l} to be notified
    * when a period change occurs.
    @param l the listener to be registered.
    @exception NullPointerException if \texttt{l} is \texttt{null}.
    */
   public void addPeriodChangeListener (PeriodChangeListener l) {
      if (l == null)
         throw new NullPointerException ("The added listener must not be null");
      if (!pclList.contains (l))
         pclList.add (l);
   }

   /**
    * Removes the period-change listener \texttt{l} from this
    * period-change event.
    @param l the period change listener being removed.
    */
   public void removePeriodChangeListener (PeriodChangeListener l) {
      pclList.remove (l);
   }

   /**
    * Removes all the period change listeners from this event.
    */
   public void clearPeriodChangeListeners() {
      pclList.clear();
   }

   /**
    * Returns an unmodifiable list containing the
    * period-change listeners currently registered
    * with this event.
    * @return the list of period-change listeners.
    */
   public List<PeriodChangeListener> getPeriodChangeListeners() {
      return umPclList;
   }

   /**
    * Equivalent to {@link #init(double) init (simulator().time())}.
    */
   public void init() {
      init (simulator().time());
   }

   /**
    * Inits this period-change event at initial
    * time \texttt{initTime}.
    * This initializes the event for a new simulation
    * replication, which resets the current period index.
    * When {@link #start()} is called,
    * no period changes will be scheduled
    * before \texttt{initTime}.
    *
    * @param initTime the initialization time.
    */
   public void init (double initTime) {
      lockedPeriod = false;
      currentPeriod = getPeriod (initTime);
      cancel();
      stopTime = -1;
      started = false;
   }

   /**
    * Equivalent to {@link #initAndNotify(double) initAndNotify (simulator().time())}.
    */
   public void initAndNotify() {
      initAndNotify (simulator().time());
   }

   /**
    * Calls {@link #init()} and notifies the
    * period-change listeners if the period changed
    * due to the initialization.
    * This can be useful to force period-change listeners
    * to restore parameters.
    */
   public void initAndNotify (double initTime) {
      final int cp = currentPeriod;
      init (initTime);
      if (cp != currentPeriod) {
         final int npcl = pclList.size();
         for (int i = 0; i < npcl; i++)
            pclList.get (i).changePeriod (this);
      }
   }

   /**
    * Starts the period-change event by scheduling it.
    */
   public void start() {
      if (lockedPeriod)
         throw new IllegalStateException
            ("The current period has been fixed, init() must be called to reset the event");
      if (stopTime >= 0)
         throw new IllegalStateException
            ("The event was already stopped, init() must be called to reset");
      if (currentPeriod < endingTimes.length)
         schedule (endingTimes[currentPeriod] - simulator().time());
      for (int p = currentPeriod + 1; p < endingTimes.length; p++)
         new PCEWrapper (this).schedule (endingTimes[p] - simulator().time ());
      started = true;
   }

   /**
    * This method should be called when the simulation
    * ends.  It calls the {@link PeriodChangeListener#stop(PeriodChangeEvent)} method of
    * all registered {@link PeriodChangeListener} implementations.
    */
   public void stop() {
      /*
       * This method should be called automatically
       * when the simulation stop.
       * We would need hook events implemented in simevents.Sim
       * for this, i.e., an event
       * occurring just before Sim.start returns. Without this
       * mechanism, we are forced to constrain the user
       * to manually call this method, which is error-prone.
       */

      if (currentPeriod < endingTimes.length)
         cancel();
      stopTime = simulator().time();
      started = false;
      final int npcl = pclList.size();
      for (int i = 0; i < npcl; i++)
         pclList.get (i).stop (this);
   }

   /**
    * Determines if this period-change event was
    * stopped since the last call to {@link #init()}.
    * @return \texttt{true} if and only if the period-change event was stopped.
    */
   public boolean wasStopped() {
      return stopTime >= 0;
   }

   public boolean isStarted() {
      return started;
   }

   @Override
   public boolean cancel() {
      final boolean b = super.cancel ();
      // This makes PCEWrapper's obsolete
      ++modCount;
      return b;
   }

   /**
    * Returns the index of the current simulation
    * period.
    @return the index of the current period.
    */
   public int getCurrentPeriod() {
      return currentPeriod;
   }

   /**
    * Sets the current period to \texttt{p} and
    * disables all period changes initiated by this event.
    * When the period is arbitrarily set by this method,
    * the period-change event is cancelled
    * and cannot be used to change period until
    * the next call to {@link #init()}, or {@link #initAndNotify()}.
    * However,
    * this method can be used multiple
    * times without calling {@link #init()}.
    * Each time this method changes the current period,
    * registered period-change listeners are notified.
    @param p the new period index.
    @exception IllegalArgumentException if the period index is
    negative or greater than or equal to {@link #getNumPeriods()}.
    */
   public void setCurrentPeriod (int p) {
      if (p < 0 || p >= getNumPeriods())
         throw new IllegalArgumentException
            ("Invalid period index");
      final boolean notify = currentPeriod != p;
      currentPeriod = p;
      cancel();
      lockedPeriod = true;
      started = true;
      if (notify) {
         final int npcl = pclList.size();
         for (int i = 0; i < npcl; i++)
            pclList.get (i).changePeriod (this);
      }
   }

   /**
    * Returns \texttt{true} if the current period
    * was changed using {@link #setCurrentPeriod(int)}
    * from the last call to {@link #init()}.
    * When the period is locked, only calls to {@link #setCurrentPeriod(int)}
    * can change the period index.
    * If the period is not locked, this returns \texttt{false}.
    @return the period locking indicator.
    */
   public boolean isLockedPeriod() {
      return lockedPeriod;
   }

   /**
    * Returns the current main period for this period-change event.
    * This is equivalent to {@link #getMainPeriod(int) get\-Main\-Period}
    * \texttt{(}{@link #getCurrentPeriod}\texttt{)}.
    @return the index of the current main period.
    */
   public int getCurrentMainPeriod() {
      return getMainPeriod (getCurrentPeriod());
   }

   /**
    * Determines if the period index \texttt{period} corresponds
    * to the preliminary period.  This method returns \texttt{true}
    * if and only if \texttt{period} is equal to 0.
    @param period the tested period index.
    @return \texttt{true} if and only if the period index corresponds to
    the preliminary period.
    */
   public boolean isPreliminaryPeriod (int period) {
      return period == 0;
   }

   /**
    * Determines if the period index \texttt{period} corresponds to
    * a main period.
    * The method returns \texttt{true} if and only if
    * \texttt{period} is greater than 0 and smaller than
    * or equal to {@link #getNumPeriods()}\texttt{ - 2}.
    @param period the tested period index.
    @return \texttt{true} if \texttt{period} corresponds to a main period.
    */
   public boolean isMainPeriod (int period) {
      return period > 0 && period <= getNumPeriods() - 2;
   }

   /**
    * Determines if the period index \texttt{period} corresponds
    * to the wrap-up period.  This method returns \texttt{true}
    * if and only if \texttt{period} is equal to {@link #getNumPeriods()}\texttt{ - 1}.
    @param period the tested period index.
    @return \texttt{true} if and only if the period index corresponds to
    the wrap-up period.
    */
   public boolean isWrapupPeriod (int period) {
      return period == getNumPeriods() - 1;
   }

   /**
    * Returns the main period index corresponding to
    * period \texttt{period}.
    * This returns the result of \texttt{period - 1} for main periods.
    * If the period is the preliminary period, this
    * returns 0, the index of the first main period.
    * If the period is the wrap-up period, this
    * returns the index of the last main period.
    @param period the period index to be processed.
    @return the main period index.
    */
   public int getMainPeriod (int period) {
      if (period < 1)
         return 0;
      final int np = getNumPeriods();
      if (period >= np - 1)
         return np - 3;
      return period - 1;
   }

   /**
    * Computes the period index corresponding
    * to the simulation time \texttt{simTime}.
    @param simTime the simulation time.
    @return the corresponding period index.
    */
   public int getPeriod (double simTime) {
      if (simTime <= 0 || simTime < endingTimes[0])
         // The time 0 must always be in the preliminary period
         return 0;
      else if (simTime >= endingTimes[endingTimes.length - 1])
         // The time is in the wrap-up period.
         return endingTimes.length;
      else if (periodDuration > 0) {
         // Fixed-length periods
         int p = (int)((simTime - endingTimes[0])/periodDuration) + 1;
         if (simTime == endingTimes[p])
            ++p;
         assert simTime >= endingTimes[p-1] && simTime < endingTimes[p];
         return p;
      }
      else
         // Perform binary search
         return Misc.getTimeInterval (endingTimes, 0, endingTimes.length - 1, simTime) + 1;
   }

   /**
    * Determines if the time \texttt{time} corresponds to
    * the beginning of a period.
    * This class cannot force period-change events
    * to have priority over simulation events happening
    * at the time of a period change, but
    * the period change should usually be processed before
    * any other event happening at the same time.
    * Otherwise, parameters may have inconsistent values.
    * This method can be used to help reschedule
    * offending events manually if they cannot be
    * scheduled after {@link #start()} is called.
    * One can use {@link #getPeriod(double)} to obtain
    * the period corresponding to the given
    * simulation time if needed.
    * @param time the simulation time to test.
    * @return \texttt{true} if the given time corresponds to
    * the time of a (future) period change, \texttt{false}
    * otherwise.
    */
   public boolean isPeriodStartingTime (double time) {
      // We cannot use periodDuration here, because
      // this would imply a multiplication which
      // is not accurate.
      int from = 0;
      int to = endingTimes.length;
      while (from != to) {
         final int mid = (from + to) / 2;
         if (time == endingTimes[mid])
            return true;
         else if (time < endingTimes[mid])
            to = mid;
         else
            from = mid + 1;
      }
      return false;
   }

   /**
    * Returns the simulation time at which the
    * period \texttt{period} starts.
    @param period the index of the queried period.
    @return the simulation time at which the period begins.
    @exception IllegalArgumentException if the period index is invalid.
    */
   public double getPeriodStartingTime (int period) {
      if (period == 0)
         return 0;
      else if (period >= 1 && period <= endingTimes.length)
         return endingTimes[period - 1];
      else
         throw new IllegalArgumentException
            ("Invalid period index: " + period);
   }

   /**
    * Returns the simulation time at which the
    * period \texttt{period} ends.
    * If the index of the last period is given,
    * this returns the time at which {@link #stop()}
    * was called. If it was not called yet,
    * this returns the current simulation time
    * if the current period is the last one or
    * \texttt{Double.NaN} otherwise.
    @param period the queried period.
    @return the simulation time of the beginning of the period.
    @exception IllegalArgumentException if the period index is invalid.
    */
   public double getPeriodEndingTime (int period) {
      if (period >= 0 && period < endingTimes.length)
         return endingTimes[period];
      else if (period == endingTimes.length) {
         if (stopTime > 0)
            return stopTime;
         else if (simulator().time() < endingTimes[endingTimes.length - 1])
            // NaN because we don't know when the last period ends.
            return Double.NaN;
         else
            return simulator().time();
      }
      else
         throw new IllegalArgumentException
            ("Invalid period index: " + period);
   }

   /**
    * Returns the duration of the period \texttt{period}.
    * This corresponds to {@link #getPeriodEndingTime(int)} minus
    * {@link #getPeriodStartingTime(int)}.
    @param period the period of interest.
    @return the duration of the period.
    @exception IllegalArgumentException if the period index is invalid.
    */
   public double getPeriodDuration (int period) {
      if (period == 0)
         return endingTimes[0];
      else if (period >= 1 && period < endingTimes.length)
         return endingTimes[period] - endingTimes[period-1];
      else if (period == endingTimes.length) {
         if (stopTime > 0)
            return stopTime - endingTimes[endingTimes.length-1];
         else if (simulator().time() < endingTimes[endingTimes.length - 1])
            return Double.NaN;
         else
            return simulator().time() - endingTimes[endingTimes.length-1];
      }
      else
         throw new IllegalArgumentException
            ("Invalid period index: " + period);
   }

   /**
    * Returns $P+2$, the number of periods
    * supported by this period change event.
    @return the number of periods.
    */
   public int getNumPeriods() {
      return endingTimes.length + 1;
   }

   /**
    * Returns $P$, the number of main periods used by
    * this period change event, i.e., {@link #getNumPeriods()}\texttt{ - 2}.
    @return the number of main periods.
    */
   public int getNumMainPeriods() {
      return getNumPeriods() - 2;
   }

   @Override
   public void actions() {
      if (lockedPeriod)
         return;
      ++currentPeriod;
      final int npcl = pclList.size();
      for (int i = 0; i < npcl; i++)
         pclList.get (i).changePeriod (this);
      // if (currentPeriod < endTimes.length)
      //    schedule (endTimes[currentPeriod] - simulator().time());
      // To avoid inconsistencies during the simulation
      // the period-change event should have the highest priority.
      // It should happen before any other events at the same
      // simulation time.
      // This code should be updated when SSJ supports
      // event priorities. In the mean time, if the problem arises,
      // one should schedule the beginning of the first main period
      // to a time slightly smaller than the real time.
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName() + "[");
      if (getName().length() > 0)
         sb.append ("name: ").append (getName()).append (", ");
      sb.append ("number of periods: ").append (endingTimes.length + 1);
      if (periodDuration > 0)
         sb.append (", period duration: ").append (periodDuration)
            .append (", first main period starting at: ").append (endingTimes[0]);
      else {
         sb.append (", ending times = ");
         sb.append (Arrays.toString (endingTimes));
      }
      sb.append (']');
      return sb.toString();
   }

   /**
    * This is used to schedule all period-change events
    * at the time {@link #start()} is called.
    * This made sure that period-change events occurred before
    * any other events while event priorities were not implemented in
    * SSJ.
    */
   private static final class PCEWrapper extends Event {
      private PeriodChangeEvent pce;
      private int expectedModCount;

      public PCEWrapper (PeriodChangeEvent pce) {
         super (pce.simulator ());
         priority = pce.priority();
         this.pce = pce;
         expectedModCount = pce.modCount;
      }

      @Override
      public void actions() {
         if (expectedModCount == pce.modCount)
            pce.actions ();
      }

      @Override
      public String toString() {
         return pce.toString ();
      }
   }
}
