package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when a problem occurs during
 * the creation of an arrival process.
 */
public class ArrivalProcessCreationException extends Exception {
   private static final long serialVersionUID = -288997187288866537L;

   public ArrivalProcessCreationException () {
      super ();
   }

   public ArrivalProcessCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public ArrivalProcessCreationException (String message) {
      super (message);
   }

   public ArrivalProcessCreationException (Throwable cause) {
      super (cause);
   }
}
