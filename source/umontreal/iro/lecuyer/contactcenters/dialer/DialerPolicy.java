package umontreal.iro.lecuyer.contactcenters.dialer;

/**
 * Represents a dialer's policy to determine the
 * outbound calls to try on each occasion.
 * A dialer's policy works as follows:
 * each time the dialer is triggered, using
 * the {@link Dialer#dial()} method,
 * it uses the dialer policy to get the number
 * of calls to try.
 * It then uses the policy to obtain a dialer list
 * from which to extract calls.
 *
 * The simplest dialer policies compute and return
 * a single number of calls to dial, e.g., by looking
 * at the number of free outbound agents.
 * A fixed dialer list is then returned to allow the dialer
 * to get the contacts.
 * However, most complex policies might generate a list of
 * contacts each time the dialer is triggered.
 */
public interface DialerPolicy {
   /**
    * Returns the number of calls the dialer
    * should try to make simultaneously at the
    * current simulation time.
    *
    * If {@link Dialer#isUsingNumActionsEvents()}
    * returns \texttt{true}, this method
    * must take into account the current number of
    * action events while determining the additional
    * number of calls to dial.
    * In the simplest and most common cases, the method
    * subtracts the result of {@link Dialer#getNumActionEvents()}
    * to the number of calls to dial.
    * However, in some cases, it might be necessary
    * to use {@link Dialer#getNumActionEvents(int)}
    * to get the number of action events for
    * each contact type individually.
    * @param dialer the triggered dialer.
    @return the number of calls the dialer should try to make.
   */
   public int getNumDials (Dialer dialer);

   /**
    * Returns the dialer list from which contacts have to
    * be removed from, at the current simulation time.
    * This list should not be stored
    * into another object since it could be constructed
    * dynamically when {@link #getNumDials(Dialer)} is called.
    * @param dialer the dialer for which the dialer list
    * is required.
    @return the associated dialer list.
    */
   public DialerList getDialerList (Dialer dialer);

   /**
    * Initializes this dialer's policy for a new simulation replication.
    * This method can be used, for example, to clear
    * data structures containing information about a preceding
    * simulation.  This method should also
    * clear the associated dialer list when
    * appropriate.
    * @param dialer the dialer which initialized
    * this policy.
    */
   public void init (Dialer dialer);

   /**
    * This method is called when the dialer using this
    * policy is started.
    * @param dialer the started dialer.
    */
   public void dialerStarted (Dialer dialer);

   /**
    * This method is called when the dialer using this
    * policy is stopped.
    * @param dialer the stopped dialer.
    */
   public void dialerStopped (Dialer dialer);
}
