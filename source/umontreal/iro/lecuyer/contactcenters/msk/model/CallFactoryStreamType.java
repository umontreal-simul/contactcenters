package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * Types of random streams for call factories.
 */
public enum CallFactoryStreamType {
   /**
    * Random stream for immediate abandonment.
    */
   BALKTEST,
   /**
    * Random stream for patience time, for contacts not abandoning immediately.
    */
   PATIENCE,
   /**
    * Random stream for service time.
    */
   SERVICE
}
