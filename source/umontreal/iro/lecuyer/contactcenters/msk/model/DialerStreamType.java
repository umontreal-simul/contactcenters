package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * Types of random streams for dialers.
 */
public enum DialerStreamType {
   /**
    * Random stream for dialing delays.
    */
   DIALDELAY,
   /**
    * Random stream for testing if a call is reached or has failed.
    */
   REACHTEST
}
