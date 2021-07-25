package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;

/**
 * This event instructs a toggle element, i.e., any object implementing
 * {@link ToggleElement}, to be started or stopped during the simulation. It
 * can be useful to toggle some elements of a contact center, e.g.,
 * arrival processes, at determined moments during simulation. After the event
 * is constructed and scheduled, when the simulation clock reaches the scheduled
 * time, the event starts or stops the associated toggle element. Note that
 * for the event to have a meaningful name when printing the event list, the
 * target toggle element should override its \texttt{toString} method.
 */
public class ToggleEvent extends Event {
   private ToggleElement element;
   private boolean start;

   /**
    * Constructs a new toggle event that will, at the time of its execution,
    * start the toggle element \texttt{element} if \texttt{start} is
    * \texttt{true}, or stop it if \texttt{enabled} is \texttt{false}.
    * 
    * @param element
    *           the toggle element affected by the event.
    * @param start
    *           the status of the toggle element after the event occurs.
    * @exception NullPointerException
    *               if \texttt{element} is \texttt{null}.
    */
   public ToggleEvent (ToggleElement element, boolean start) {
      this (Simulator.getDefaultSimulator (), element, start);
   }
   
   /**
    * Equivalent to {@link #ToggleEvent(ToggleElement,boolean)},
    * with a user-defined simulator \texttt{sim}.
    * @param sim the simulator associated with the toggle event.
    * @param element
    *           the toggle element affected by the event.
    * @param start
    *           the status of the toggle element after the event occurs.
    * @exception NullPointerException
    *               if \texttt{sim} or \texttt{element} are \texttt{null}.
    */
   public ToggleEvent (Simulator sim, ToggleElement element, boolean start) {
      super (sim);
      if (element == null)
         throw new NullPointerException ("The toggle element must not be null");
      this.element = element;
      this.start = start;
   }

   /**
    * Returns the toggle element affected by this event.
    * 
    * @return the toggle element affected by this event.
    */
   public ToggleElement getToggleElement () {
      return element;
   }

   /**
    * Changes the associated toggle element to \texttt{element}.
    * 
    * @param element
    *           the new toggle element.
    * @exception NullPointerException
    *               if \texttt{element} is \texttt{null}.
    */
   public void setToggleElement (ToggleElement element) {
      if (element == null)
         throw new NullPointerException ("The toggle element must not be null");
      this.element = element;
   }

   /**
    * Returns the status of the toggle element associated with this object after
    * this event has occurred.
    * 
    * @return the status of the associated toggle element after this event has
    *         occurred.
    */
   public boolean getStart () {
      return start;
   }

   /**
    * When the event occurs, the activity status of the toggle element will be
    * set to \texttt{start}.
    * 
    * @param start
    *           \texttt{true} if the toggle element will be started,
    *           \texttt{false} if it will be stopped.
    */
   public void setStart (boolean start) {
      this.start = start;
   }

   @Override
   public void actions () {
      // Avoids an exception to be thrown during the simulation.
      if (element.isStarted () != start)
         if (start)
            element.start ();
         else
            element.stop ();
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append (start ? "enable" : "disable");
      sb.append (" the toggle element ");
      sb.append (element.toString ());
      sb.append (']');
      return sb.toString ();
   }
}
