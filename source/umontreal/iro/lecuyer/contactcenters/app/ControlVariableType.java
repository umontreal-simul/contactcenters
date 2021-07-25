package umontreal.iro.lecuyer.contactcenters.app;

/**
 * Represents types of control variables that can be used by call center
 * simulators.
 */
public enum ControlVariableType {
   /**
    * The number of arrivals.
    */
   NUMARRIVALS (Messages.getString("ControlVariableType.NumArrivals")); //$NON-NLS-1$

   private String name;

   private ControlVariableType (String name) {
      this.name = name;
   }

   @Override
   public String toString () {
      return name;
   }
}
