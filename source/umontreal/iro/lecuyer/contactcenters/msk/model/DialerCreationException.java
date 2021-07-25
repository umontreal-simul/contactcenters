package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when a problem occurs during
 * the creation of a dialer.
 */
public class DialerCreationException extends Exception {
   private static final long serialVersionUID = -2552205667270527135L;

   public DialerCreationException () {
      super ();
   }

   public DialerCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public DialerCreationException (String message) {
      super (message);
   }

   public DialerCreationException (Throwable cause) {
      super (cause);
   }
}
