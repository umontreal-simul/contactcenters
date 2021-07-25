package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactInstantiationException;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;

/**
 * Implements the {@link DialerList} interface for a finite
 * dialer list whose elements are obtained from an external
 * source.  Since this class implements the
 * {@link NewContactListener} interface, it can be bound
 * to an arrival process or any source of contacts.
 * When a new contact is notified to this dialer list,
 * it is added at the end of an internal ordered list for later use.
 * When calling {@link #removeFirst}, the first
 * element from this internal list is returned.
 */
public class ContactListenerDialerList implements DialerList,
                                                  NewContactListener {
   private List<Contact> dialerList;

   /**
    * Constructs a new empty dialer list implemented by
    * a doubly-linked list.  The used implementation is provided
    * by the standard {@link LinkedList} class.
    */
   public ContactListenerDialerList() {
      dialerList = new LinkedList<Contact>();
   }

   /**
    * Constructs a new dialer list using the given \texttt{dialerList}
    * to store the contacts.  The given list should be empty
    * or contain only {@link Contact} instances.
    @param dialerList the list used to store the contacts.
    @exception NullPointerException if \texttt{dialerList} is \texttt{null}.
    */
   public ContactListenerDialerList (List<Contact> dialerList) {
      if (dialerList == null)
         throw new NullPointerException ("The internal list must not be null");
      this.dialerList = dialerList;
   }

   /**
    * Returns the internal list containing the contacts to dial.
    * This list should contain only non-\texttt{null}
    * {@link Contact} instances.
    @return the internal dialer list.
    */
   public List<Contact> getList() {
      return dialerList;
   }

   /**
    * Sets the internal list of contacts to dial to \texttt{dialerList}.
    @param dialerList the list used to store the contacts.
    @exception NullPointerException if \texttt{dialerList} is \texttt{null}.
    */
   public void setList (List<Contact> dialerList) {
      if (dialerList == null)
         throw new NullPointerException ("The internal list must not be null");
      this.dialerList = dialerList;
   }

   public int size(int[] contactTypes) {
      if (contactTypes != null) {
         int size = 0;
         for (final Contact ct : dialerList) {
            if (ct == null)
               continue;
            final int type = ct.getTypeId ();
            for (final int typeTest : contactTypes)
               if (type == typeTest) {
                  ++size;
                  continue;
               }
         }
         return size;
      }
      return dialerList.size();
   }

   public Contact removeFirst(int[] contactTypes) {
      if (contactTypes != null)
         for (final Iterator<Contact> it = dialerList.iterator (); it.hasNext (); ) {
            final Contact ct = it.next ();
            if (ct == null) {
               it.remove ();
               continue;
            }
            final int type = ct.getTypeId ();
            for (final int typeTest : contactTypes)
               if (type == typeTest) {
                  it.remove ();
                  return ct;
               }
         }
      else
         while (!dialerList.isEmpty()) {
            final Contact c = dialerList.remove (0);
            if (c != null)
               return c;
         }
      throw new NoSuchElementException ("The dialer list is empty");
   }

   /**
    * Adds the new contact \texttt{contact} to the dialer list.
    @param contact the contact being added.
    @exception NullPointerException if \texttt{contact} is \texttt{null}.
    */
   public void newContact (Contact contact) {
      if (contact == null)
         throw new NullPointerException ("The notified contact must not be null");
      dialerList.add (contact);
   }

   public Contact newInstance() {
      while (!dialerList.isEmpty())
         return dialerList.remove (0);
      throw new ContactInstantiationException
         (this, "The dialer list is empty");
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("dialer list class: ").append (dialerList.getClass().getName());
      sb.append (']');
      return sb.toString();
   }

   public void clear() {
      dialerList.clear();
   }
}
