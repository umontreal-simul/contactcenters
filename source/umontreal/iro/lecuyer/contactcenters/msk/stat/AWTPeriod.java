package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents an object capable of computing a period index to get the
 * acceptable waiting time of a contact.
 * In general, the acceptable waiting time may depend on the call type and
 * a period index.  The period index often corresponds to the
 * period of arrival, but it can be set to a fixed value in
 * some cases. 
 * An
 * implementation of this interface maps a contact object to a period index
 * corresponding to the correct acceptable waiting time.
 */
public interface AWTPeriod {
   /**
    * Returns the index of the main period for the acceptable waiting time of
    * contact \texttt{contact}. The returned index must not be smaller than 0 or
    * greater than $P$, where $P$ is the number of main periods. If this method
    * returns $P$, the acceptable waiting time for all periods is used.
    * 
    * @param contact
    *           the contact being queried.
    * @return the main period index for the acceptable waiting time.
    */
   public int getAwtPeriod (Contact contact);

   /**
    * Returns the index for the acceptable waiting time for all periods.
    * 
    * @return the main period index for the acceptable waiting time.
    */
   public int getGlobalAwtPeriod ();
}
