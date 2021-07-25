package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * Types of random streams for arrival processes.
 */
public enum ArrivalProcessStreamType {
   /**
    * Stream for inter-arrival times.
    */
   INTERARRIVAL,
   /**
    * Stream for random arrival rates, in the case of
    * doubly-stochastic arrival processes.
    */
   RATES
}
