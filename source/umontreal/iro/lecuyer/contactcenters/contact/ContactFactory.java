package umontreal.iro.lecuyer.contactcenters.contact;

/**
 * Allows contact sources to create
 * contact objects of user-defined classes.  When
 * the {@link Contact} class is extended to add user-defined
 * attributes, a contact factory must also be created
 * to allow contact sources to instantiate objects
 * derived from the {@link Contact} subclass.  To construct
 * a new contact factory, the user simply implements this interface
 * to provide a {@link #newInstance} method the contact sources
 * call to get contacts.
 */
public interface ContactFactory {
   /**
    * Constructs and returns a new {@link Contact} object.
    * If a contact cannot be instantiated, a
    * {@link ContactInstantiationException} is thrown.
    @return the new contact object.
    @exception ContactInstantiationException if a contact cannot be instantiated.
    */
   public Contact newInstance();
}
