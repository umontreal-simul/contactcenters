package umontreal.iro.lecuyer.contactcenters.app;

import java.util.List;

/**
 * Represents a contact center simulation whose simulation can be observed or
 * stopped. An observer can be registered by using the
 * {@link #addContactCenterSimListener} method to be notified each time a step
 * (replication or batch) of the simulation is done. Moreover, the
 * {@link #abort} method can be used to stop the simulation before its end. This
 * can be used to implement a user interface allowing the progress of the
 * simulation to be displayed.
 */
public interface ObservableContactCenterSim extends ContactCenterSim {
   /**
    * Registers the listener \texttt{l} to be notified about the progress of the
    * simulator.
    * 
    * @param l
    *           the listener to be notified.
    * @exception NullPointerException
    *               if \texttt{l} is \texttt{null}.
    */
   public void addContactCenterSimListener (ContactCenterSimListener l);

   /**
    * Removes the listener \texttt{l} from the list of listeners registered with
    * this simulator.
    * 
    * @param l
    *           the listener being removed.
    */
   public void removeContactCenterSimListener (ContactCenterSimListener l);

   /**
    * Removes all the listeners registered with this simulator.
    */
   public void clearContactCenterSimListeners ();

   /**
    * Returns the listeners registered with this simulator.
    * 
    * @return the list of registered listeners.
    */
   public List<ContactCenterSimListener> getContactCenterSimListeners ();

   /**
    * Determines if the simulation has been aborted by using the {@link #abort}
    * method.
    * 
    * @return \texttt{true} if the simulation was aborted, \texttt{false}
    *         otherwise.
    */
   public boolean isAborted ();

   /**
    * Aborts the current simulation.
    */
   public void abort ();
}
