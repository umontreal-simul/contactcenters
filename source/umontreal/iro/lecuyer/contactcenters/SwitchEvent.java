package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents an event that toggles an element on predefined simulation times.
 * This differs from {@link ToggleEvent} that occurs only at a specific
 * simulation time, and enables or disables the toggle element once. This event
 * is constructed with a toggle element, and an array of simulation times. The
 * constructors determines the first time, in the array, that is greater than
 * the current simulation time, and schedules the event at that time. When the
 * event happens, the element is toggled, and the event is scheduled again until
 * all the times in the array have been used.
 */
public class SwitchEvent extends Event {
   private int idx = 0;
   private ToggleElement el;
   private double[] times;

   /**
    * Constructs a new switch event from the toggle element \texttt{el}, and the
    * simulation times \texttt{times}. The times in \texttt{times} must be
    * sorted in ascending order.
    * 
    * @param el
    *           the toggle element.
    * @param times
    *           the simulation times the event will occur.
    * @exception NullPointerException
    *               if \texttt{el} or \texttt{times} are \texttt{null}.
    */
   public SwitchEvent (ToggleElement el, double[] times) {
      this (Simulator.getDefaultSimulator (), el, times);
   }
   
   /**
    * Equivalent to {@link #SwitchEvent(ToggleElement,double[])},
    * with a user-defined simulator \texttt{sim}.
    * @param sim the simulator attached to the new event.
    * @param el
    *           the toggle element.
    * @param times
    *           the simulation times the event will occur.
    * @exception NullPointerException
    *               if \texttt{sim}, \texttt{el}, or \texttt{times} are \texttt{null}.
    */
   public SwitchEvent (Simulator sim, ToggleElement el, double[] times) {
      super (sim);
      if (el == null)
         throw new NullPointerException ("The toggle element must not be null");
      if (times == null)
         throw new NullPointerException ("times must not be null");
      this.el = el;
      this.times = times;
      init ();
   }

   /**
    * Returns the toggle element affected with this event.
    * 
    * @return the affected toggle element.
    */
   public ToggleElement getToggleElement () {
      return el;
   }

   /**
    * Returns an array containing the toggle times used by this event.
    * 
    * @return the array of toggle times.
    */
   public double[] getToggleTimes () {
      return times.clone ();
   }

   public void init () {
      final double simTime = simulator().time ();
      while (idx < times.length && times[idx] < simTime)
         ++idx;
      --idx;
   }

   public int getNextTimeIndex () {
      return Math.min (times.length, idx + 1);
   }

   public double getNextTime () {
      if (idx + 1 < times.length)
         return times[idx + 1];
      return Double.POSITIVE_INFINITY;
   }

   public void schedule () {
      ++idx;
      if (idx < times.length)
         schedule (times[idx] - simulator().time ());
   }

   /**
    * Cancels this event if it is scheduled, and skips to the next toggle time.
    */
   public void skipTime () {
      cancel ();
      ++idx;
   }

   @Override
   public void actions () {
      boolean startEv = idx % 2 == 0;
      if (startEv && !el.isStarted ())
         el.start ();
      else if (!startEv && el.isStarted ())
         el.stop ();
      schedule ();
   }

   @Override
   public String toString () {
      return getClass ().getSimpleName () + "[toggle element: "
            + el.toString () + "]";
   }
}
