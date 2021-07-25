package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents an object that can compute
 * information on the waiting time distribution conditional
 * on the total number of transitions during a time horizon,
 * the transition rate,
 * and the number of transitions spent in queue
 * by a particular contact.
 */
public interface ProbInAWT {

   /**
    * Returns the probability that the
    * waiting time of a contact having spent
    * \texttt{delta} transitions in the queue
    * is smaller than the current acceptable waiting time.
    * @param delta the number of transitions spent in queue by the contact.
    * @return the probability of the waiting time being smaller than the threshold.
    */
   public double getProbInAWT (int delta);
   
   /**
    * Returns the expected waiting time for a contact
    * having spent \texttt{delta} transitions in queue.
    * @param delta the number of transitions spent in queue.
    * @return the expected waiting time.
    */
   public double getExpectedWaitingTime (int delta);
   
   /**
    * Returns the expected waiting time conditional that
    * the waiting time is greater than the acceptable waiting time,
    * for a contact having spent \texttt{delta}
    * transitions into the queue.
    * @param delta the number of transitions spent into
    * the queue.
    * @return the expected waiting time.
    */
   public double getExpectedWaitingTimeGTAWT (int delta);

   /**
    * Returns the currently used transition rate.
    * @return the transition rate.
    */
   public double getJumpRate ();
   
   /**
    * Returns the currently used acceptable waiting time.
    * @return the acceptable waiting time.
    */
   public double getAWT ();

   /**
    * Returns the currently used time horizon.
    * @return the time horizon.
    */
   public double getTimeHorizon ();
   
   /**
    * Returns the currently used number of transitions.
    * @return the current number of transitions.
    */
   public int getNumTransitions();
   
   /**
    * Initializes this object with a new
    * acceptable waiting time,
    * transition rate, time horizon, and
    * number of transitions.
    * @param awt the new acceptable waiting time.
    * @param jumpRate the new transition rate.
    * @param timeHorizon the new time horizon.
    * @param numTransitions the new number of transitions.
    */
   public void init (double awt, double jumpRate, double timeHorizon, int numTransitions);
}
