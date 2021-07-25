package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when a problem occurs during
 * the creation of a call factory.
 */
public class CallFactoryCreationException extends Exception {
   private static final long serialVersionUID = -4950491509666293622L;

   public CallFactoryCreationException () {
      super ();
   }

   public CallFactoryCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public CallFactoryCreationException (String message) {
      super (message);
   }

   public CallFactoryCreationException (Throwable cause) {
      super (cause);
   }
}
