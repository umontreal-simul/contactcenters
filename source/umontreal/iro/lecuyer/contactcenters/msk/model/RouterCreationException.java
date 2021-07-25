package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when a problem
 * occurs during the creation of the router.
 */
public class RouterCreationException extends Exception {
   private static final long serialVersionUID = 495188546221624488L;

   public RouterCreationException () {
      super ();
   }

   public RouterCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public RouterCreationException (String message) {
      super (message);
   }

   public RouterCreationException (Throwable cause) {
      super (cause);
   }
}
