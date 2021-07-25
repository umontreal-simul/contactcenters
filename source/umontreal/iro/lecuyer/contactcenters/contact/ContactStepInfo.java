package umontreal.iro.lecuyer.contactcenters.contact;



/**
 * Represents an information object about a single step
 * (end of service, exit of waiting queue, etc.)
 * in the life cycle of a contact in the contact center.
 * Implementations of this interface are used when
 * the steps of contacts are traced.
 */
public interface ContactStepInfo {
   /**
    * Returns the contact concerned by this step.
    @return the concerned contact.
    */
   public Contact getContact();

   /**
    * Returns the simulation time at which this step
    * started.
    @return the start time of the step.
    */
   public double getStartingTime();

   /**
    * Returns the simulation time at which this step
    * ended.
    @return the end time of the step.
    */
   public double getEndingTime();

   /**
    * Makes a copy of this data object that will be
    * associated with the cloned contact \texttt{clonedContact}.
    * This method is intended to be used in
    * {@link Contact#clone}.
    * @param clonedContact the contact being cloned.
    * @return the clone of this data object.
    */
   public ContactStepInfo clone (Contact clonedContact);
}
