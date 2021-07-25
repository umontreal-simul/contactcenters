package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.ContactInstantiationException;

/**
 * Implements the {@link DialerList} interface for an infinite
 * dialer list whose elements are produced using a contact
 * factory.  This list can be used when there is
 * no defined model for the calls made by the dialer.
 */
public class InfiniteDialerList implements DialerList {
   private ContactFactory factory;

   /**
    * Constructs a new infinite dialer list whose contacts
    * are instantiated using the contact factory \texttt{factory}.
    @param factory the contact factory used to instantiate contacts.
    @exception NullPointerException if \texttt{factory} is \texttt{null}.
    */
   public InfiniteDialerList (ContactFactory factory) {
      if (factory == null)
         throw new NullPointerException ("The contact factory must not be null");
      this.factory = factory;
   }
   
   /**
    * Returns the contact factory associated with this
    * dialer list.
    @return the associated contact factory.
    */
   public ContactFactory getContactFactory() {
      return factory;
   }

   /**
    * Sets the contact factory used to instantiate
    * contacts to \texttt{factory}.
    @param factory the new contact factory.
    @exception NullPointerException if \texttt{factory} is \texttt{null}.
    */
   public void setContactFactory (ContactFactory factory) {
      if (factory == null)
         throw new NullPointerException ("The contact factory must not be null");
      this.factory = factory;
   }

   public int size(int[] contactTypes) {
      if (contactTypes != null)
         throw new UnsupportedOperationException();
      return Integer.MAX_VALUE;
   }

   public Contact removeFirst(int[] contactTypes) {
      if (contactTypes != null)
         throw new UnsupportedOperationException();
      try {
         return factory.newInstance();
      }
      catch (final ContactInstantiationException e) {
         throw new NoSuchElementException ("The dialer list is empty");
      }
   }

   public Contact newInstance() {
      return factory.newInstance();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("contact factory: ").append (factory.toString());
      sb.append (']');
      return sb.toString();
   }

   public void clear() {}
}
