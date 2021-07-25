package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * This exception is thrown when an error occurs during
 * the creation of an agent group. 
 */
public class AgentGroupCreationException extends Exception {
   private static final long serialVersionUID = -1773176330129387549L;

   public AgentGroupCreationException () {
      super ();
   }

   public AgentGroupCreationException (String message, Throwable cause) {
      super (message, cause);
   }

   public AgentGroupCreationException (String message) {
      super (message);
   }

   public AgentGroupCreationException (Throwable cause) {
      super (cause);
   }
}
