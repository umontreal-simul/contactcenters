package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;

/**
 * Represents a list that contains and manages contacts to be made later
 * by a dialer.
 * This interface specifies methods to clear the current
 * list, to remove the first contact of the list, and
 * to compute the current size of the list.
 * The contents of the dialer list depends on the implementation.
 * For example, {@link InfiniteDialerList} always has an
 * infinite size, and uses a user-supplied contact
 * factory to obtain contacts.
 * The {@link ContactListenerDialerList}, on the other hand,
 * is backed by a fixed list which is populated by a contact
 * listener.
 * The contents of the dialer list could also change with time.
 * For example, during some time intervals of the day,
 * a limit on the maximal number of dialed contacts
 * might be imposed by restricting the size of the
 * dialer list.
 *
 *  Some dialer lists might also allow one to restrict the types of
 *  contacts that can be extracted from the list.
 *  This can be useful for some dialing policies managing
 *  contacts of several types, and composing
 *  specific numbers of contacts of each type.
 *
 * A dialer list also implements
 * the {@link ContactFactory} interface;
 * the implementation of the {@link #newInstance()}
 * method usually forwards the call to
 * {@link #removeFirst(int[])}.
 * However,
 * while the contact factory always creates new objects,
 * a dialer list may extract them from a list with
 * possibly finite size.
 */
public interface DialerList extends ContactFactory {
   /**
    * Returns the number of contacts of desired types stored into this
    * dialer list.
    * This method counts and returns the number of
    * stored contacts
    * whose type identifiers correspond to one of the
    * elements in the given \texttt{contactTypes} array.
    * If the array is \texttt{null}, the check is applied for
    * all contact types.
    * If the size of the list is infinite,
    * this must return {@link Integer#MAX_VALUE}.
    * If the dialer list does not allow restriction to
    * specific contact types, this method throws
    * an {@link UnsupportedOperationException}.
    *
    * @param contactTypes the array of desired contact types.
    @return the number of contacts in the dialer list.
    @exception UnsupportedOperationException if \texttt{contactTypes}
    is non-\texttt{null} while the dialer list does not support
    restrictions to specific contact types.
    */
   public int size(int[] contactTypes);

   /**
    * Clears the contents of this dialer list.
    * This method does not always reset the size of the list to 0.
    * For example, this method has no effect in the case
    * of infinite dialer lists.
    * For dialer lists with limits on the number of dialed
    * contacts, this resets the size to the maximum
    * number of contacts allowed.
    */
   public void clear();

   /**
    * Removes and returns the first contact
    * with one of the desired types from the dialer list.
    * If the list is empty or does not contain any
    * contact of the desired types, this method must throw a
    * {@link NoSuchElementException}.
    * If \texttt{contactTypes} is \texttt{null},
    * any contact type is allowed.
    * If \texttt{contactTypes} is non-\texttt{null} while
    * the dialer list does not support restrictions to
    * specific contact types, this throws an
    * {@link UnsupportedOperationException}.
    * @param contactTypes the array of desired contact types.
    @return the removed contact.
    @exception NoSuchElementException if the dialer list is empty.
    @exception UnsupportedOperationException if \texttt{contactTypes}
    is non-\texttt{null} while the dialer list does not support
    restrictions to specific contact types.
   */
   public Contact removeFirst(int[] contactTypes);
}
