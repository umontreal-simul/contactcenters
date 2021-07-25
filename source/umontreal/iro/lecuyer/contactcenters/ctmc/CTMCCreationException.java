package umontreal.iro.lecuyer.contactcenters.ctmc;

public class CTMCCreationException extends Exception {
   private static final long serialVersionUID = 3967659296625462038L;

   public CTMCCreationException () {
      super ();
   }

   public CTMCCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public CTMCCreationException (String message) {
      super (message);
   }

   public CTMCCreationException (Throwable cause) {
      super (cause);
   }
}
