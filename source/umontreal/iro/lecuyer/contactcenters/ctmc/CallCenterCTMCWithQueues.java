package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Extends the {@link CallCenterCTMC} interface
 * for keeping
 * track of the transition number for any queued contact.
 * This additional bookkeeping allows one to
 * obtain the waiting time of the last contact
 * having entered service or abandoned
 * using the {@link #getLastWaitingTime(int)} method, or
 * the longest waiting time in queue using
 * the {@link #getLongestWaitingTime(int)} method.
 * 
 * Note that waiting times returned by these two methods are
 * expressed in numbers of transitions.
 * Because the simulated Markov chain is uniformized, the
 * expected waiting time can be retrived by dividing
 * this integer by {@link CallCenterCTMC#getJumpRate()}.
 * 
 * One can use {@link CallCenterCTMCQueues} to implement the
 * two methods specified by this interface.
 */
public interface CallCenterCTMCWithQueues extends CallCenterCTMC {
   /**
    * Ŗeturns waiting time of the last contact of type \texttt{k}
    * having entered service, or abandoned.
    * If no such contact exists, this method returns 0.
    * @param k the queried tppe of contact.
    * @return the last waiting time.
    */
   public int getLastWaitingTime (int k);
   
   /**
    * Returns the longest waiting time among all contacts
    * of type \texttt{k}.
    * This returns 0 if no contact of type \texttt{k}
    * is waiting in queue.
    * @param k the queried contact type.
    * @return the longest waiting time.
    */
   public int getLongestWaitingTime (int k);
}
