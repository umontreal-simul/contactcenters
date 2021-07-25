package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.Set;

/**
 * Represents the state of a dialer.
 */
public class DialerState {
   private DialerActionState[] dialerActionEvents;
   
   /**
    * Constructs a new dialer state containing
    * the state of the dialer \texttt{dialer}.
    * @param dialer the dialer for which the state must be saved.
    */
   protected DialerState (Dialer dialer) {
      save (dialer);
   }
   
   /**
    * Returns an array containing a state object
    * for each dialer action event saved.
    * @return the array of dialer action events.
    */
   public DialerActionState[] getDialerActionEvents() {
      return dialerActionEvents;
   }
   
   /**
    * Saves the state of the dialer \texttt{dialer}.
    * This requires {@link Dialer#isKeepingActionEvents()}
    * to return \texttt{true}.
    * @param dialer the dialer to save state.
    */
   private void save (Dialer dialer) {
      final Set<DialerActionEvent> dev = dialer.getActionEvents();
      dialerActionEvents = new DialerActionState[dev.size()];
      int idx = 0;
      for (final DialerActionEvent ev : dev)
         dialerActionEvents[idx++] = new DialerActionState (ev);
   }
   
   /**
    * Restores the state of the dialer \texttt{dialer}
    * with the information stored in this object.
    * @param dialer the dialer to restore.
    */
   void restore (Dialer dialer) {
      dialer.init();
      for (final DialerActionState actionState : dialerActionEvents)
         new DialerActionEvent (dialer, actionState.getContact().clone(),
               actionState.isSuccessful())
               .schedule (actionState.getDialEndTime() - dialer.simulator().time());
   }
}
