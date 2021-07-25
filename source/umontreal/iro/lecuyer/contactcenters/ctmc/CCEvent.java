package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents an event occurring during a transition of
 * a CTMC representing a contact center with
 * multiple contact types and agent groups.
 */
public interface CCEvent {
   /**
    * Performs the necessary actions for the transition, and
    * returns the appropriate transition type.
    * This method is called by
    * {@link CallCenterCTMCKI#nextStateInt(int)}
    * in order to generate a transition.
    * Random bits can be obtained as needed by
    * using the given integer \texttt{rv},
    * but the \texttt{numUsedBits} least significant
    * bits of \texttt{rv} are already used before
    * the method is called, i.e., to select the index
    * of an event in a lookup table.
    * 
    * @param ctmc the CTMC representing the call center.
    * @param tr the number of transitions already done.
    * @param rv the random integer used to simulate the transition.
    * @param usedBits the number of bits already used in \texttt{rv}.
    * @param changeState determines if the event can change the
    * state of the CTMC.
    * @return the type of the simulated transition.
    */
   public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv, int usedBits, boolean changeState);
}
