package umontreal.iro.lecuyer.contactcenters.router;


/**
 * Represents state information for contact or agent
 * rerouting.
 */
public class ReroutingState implements Cloneable {
   private int numReroutingsDone;
   private double nextReroutingTime;
   
   /**
    * Constructs a new state information object
    * for rerouting for a contact or an agent that
    * has been previously rerouted \texttt{numReroutingsDone}
    * times, and whose next rerouting will happen at
    * time \texttt{nextReroutingTime}.
    * @param numReroutingsDone the number of times the contact or agent has been rerouted before.
    * @param nextReroutingTime the simulation of the next rerouting.
    */
   public ReroutingState (int numReroutingsDone, double nextReroutingTime) {
      this.numReroutingsDone = numReroutingsDone;
      this.nextReroutingTime = nextReroutingTime;
   }
   
   /**
    * Returns the simulation time at which the router
    * will try to reroute the contact or the agent.
    * @return the next rerouting time.
    */
   public double getNextReroutingTime () {
      return nextReroutingTime;
   }

   /**
    * Returns the number of reroutings that has
    * happened so far for the contact or agent.
    * @return the number of preceding reroutings.
    */
   public int getNumReroutingsDone () {
      return numReroutingsDone;
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("Number of reroutings done before: ").append (numReroutingsDone);
      sb.append (", simulation time of the next rerouting: ").append (nextReroutingTime);
      sb.append (']');
      return sb.toString();
   }

   @Override
   public ReroutingState clone() {
      try {
         return (ReroutingState)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
   }
}
