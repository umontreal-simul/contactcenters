package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.List;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a contact source which produces
 * contacts during a simulation.
 * Before any simulation replication,
 * any contact source needs to be initialized.
 * Since initialization disables the source, the source
 * must be enabled to produce contacts.
 * When a contact is produced,
 * the contact source should instantiate a {@link Contact} object
 * using a user-specified {@link ContactFactory} implementation,
 * or pick an instance from an internal list.
 * It should then notify the new contact to any registered
 * {@link NewContactListener} implementation.
 */
public interface ContactSource extends ToggleElement, Initializable, Named {
   /*
    * The main reason for this interface is to avoid
    * the necessity of getArrivalProcess and getDialer
    * methods in Contact. getSource returns the contact
    * source, wheter it is an arrival process (inbound contact)
    * or dialer (outbound contact).
    */

   /**
    * Initializes the contact source for a new replication of a simulation.
    * This method should disable the contact source if it
    * is enabled, and cancel
    * any scheduled event.  One can assume
    * this method will be called before any simulation replication
    * starts.
    */
   public void init();

   /**
    * Adds the listener \texttt{listener} to be
    * notified when a new contact is produced.
    * If the listener was already registered, nothing happens, because
    * the listener cannot be notified more than once.
    @param listener the new-contact listener being added.
    @exception NullPointerException if \texttt{listener} is \texttt{null}.
    */
   public void addNewContactListener (NewContactListener listener);

   /**
    * Removes the new-contact listener \texttt{listener} from
    * the list associated with this
    * contact source.  If the listener was not previously
    * registered with this contact source, nothing happens.
    @param listener the new-contact listener being removed.
    */
   public void removeNewContactListener (NewContactListener listener);

   /**
    * Clears the list of new-contact listeners associated
    * with this contact source.
    */
   public void clearNewContactListeners();

   /**
    * Returns an unmodifiable list containing
    * all the new-contact listeners registered
    * with this contact source.
    * @return the list of all registered new-contact listeners.
    */
   public List<NewContactListener> getNewContactListeners();

   /**
    * Returns a reference to the simulator
    * associated with this contact source.
    * The simulator is used to schedule any
    * event required by the contact source
    * to produce contacts.
    *
    * Any implementation of this interface
    * should provide a constructor
    * accepting the simulator as an argument.
    * Constructors not receiving a simulator
    * should use the default simulator
    * returned by {@link Simulator#getDefaultSimulator()}.
    * @return the associated simulator.
    */
   public Simulator simulator();

   /**
    * Sets the simulator attached to this
    * contact source to \texttt{sim}.
    * This method should not be called
    * while the contact source is started.
    * @param sim the new simulator.
    * @exception NullPointerException if
    * \texttt{sim} is \texttt{null}.
    */
   public void setSimulator (Simulator sim);
}
