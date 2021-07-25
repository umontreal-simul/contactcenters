package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents a condition that can be checked on a given contact.
 * Often, the test performed by such a condition is simple,
 * e.g., the condition applies if the number of queued contacts
 * of the type of the tested contact is greater than
 * a threshold.
 * 
 * However, some conditions require complex state information, such
 * as statistics observed during
 * some time periods.
 * In such cases, mechanisms need to be initialized at the beginning
 * of simulation steps, and started during time intervals the condition is used.
 * For this, the condition object might implement
 * the {@link Initializable} and
 * {@link ToggleElement} interfaces in addition
 * to this interface.
 * The simulator calls \texttt{init()} on each initializable condition, then
 * \texttt{start()} for each condition implementing
 * {@link ToggleElement}.
 */
public interface Condition {
   /**
    * Checks the represented condition for the given contact
    * \texttt{contact}, and returns \texttt{true} if and
    * only if the condition applies.
    * Some conditions depend on the state of the system rather
    * than a particular contact.
    * In such cases, the contact object can be ignored.
    * @param contact the contact on which to check the condition.
    * @return the success indicator of the test.
    */
   public boolean applies (Contact contact);
}
