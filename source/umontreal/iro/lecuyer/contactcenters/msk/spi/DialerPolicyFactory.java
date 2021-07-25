package umontreal.iro.lecuyer.contactcenters.msk.spi;

import umontreal.iro.lecuyer.contactcenters.dialer.DialerPolicy;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.DialerCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.DialerManager;
import umontreal.iro.lecuyer.contactcenters.msk.params.DialerParams;

/**
 * Provdes a method to create a dialer from the
 * user-specified parameters.
 */
public interface DialerPolicyFactory {
   /**
    * Constructs and returns a dialer policy
    * for the call center model \texttt{cc} and
    * the dialer parameters \texttt{par}.
    * This method uses the result of {@link DialerParams#getDialerPolicy()}
    * as a policy identifier given by the user.  It returns a dialer policy
    * if that particular dialer policy identifier is supported.
    * Otherwise, it returns \texttt{null}.
    * A dialer-creation exception is thrown only if
    * the implementation supports the creation of the policy,
    * but fails due to some error such as bad parameters.
    * @param cc the call center model.
    * @param par the dialer's parameters.
    * @return the new dialer's policy, or \texttt{null}.
    */
   public DialerPolicy createDialerPolicy (CallCenter cc, DialerManager dm, DialerParams par) throws DialerCreationException;
}
