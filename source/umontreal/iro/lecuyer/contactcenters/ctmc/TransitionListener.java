package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents a listener that can be notified when a transition
 * occurs during a DTMC simulation of a call center.
 * After an object of a class implementing this interface is constructed,
 * it can be registered with a DTMC call center simulator. 
 */
public interface TransitionListener {
   /**
    * This method is called when the registered call center CTMC
    * is initialized, during replication \texttt{r}, at
    * the beginning of main period \texttt{mp}.
    * @param ctmc the initialized CTMC.
    * @param r the replication number.
    * @param mp the main period index.
    */
   public void init (CallCenterCTMC ctmc, int r, int mp);
   
   /**
    * This method is called when a new transition occurs in
    * the CTMC \texttt{ctmc}, during replication \texttt{r} of the
    * simulation of main period \texttt{mp}.
    * The type of the simulated transition is given
    * by \texttt{type}.
    * @param ctmc the continuous-time Markov chain.
    * @param r the replication index.
    * @param mp the main period index.
    * @param type the transition type.
    */
   public void newTransition (CallCenterCTMC ctmc, int r, int mp, TransitionType type);
}
