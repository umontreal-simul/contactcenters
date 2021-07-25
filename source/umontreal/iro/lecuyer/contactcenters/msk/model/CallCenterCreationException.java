package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when a problem
 * occurs during the creation of a call center model.
 */
public class CallCenterCreationException extends Exception {
   private static final long serialVersionUID = -3443702567626681292L;

   public CallCenterCreationException () {
      super ();
   }

   public CallCenterCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public CallCenterCreationException (String message) {
      super (message);
   }

   public CallCenterCreationException (Throwable cause) {
      super (cause);
   }
}
