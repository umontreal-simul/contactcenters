package umontreal.iro.lecuyer.contactcenters.msk.spi;

import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.RouterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RouterManager;
import umontreal.iro.lecuyer.contactcenters.msk.params.RouterParams;
import umontreal.iro.lecuyer.contactcenters.router.Router;

/**
 * Provdes a method to create a router from the
 * user-specified parameters.
 */
public interface RouterFactory {
   /**
    * Constructs and returns a router
    * for the call center model \texttt{cc} and
    * the router parameters \texttt{par}.
    * This method uses the {@link RouterParams#getRouterPolicy()}
    * method to get the name of the router's policy given by the user,
    * and creates a router object if it supports that particular
    * policy name.
    * Otherwise, it returns \texttt{null}.
    * A router-creation exception is thrown only if
    * the given routing policy is supported by the
    * implementation, but some error occurs during
    * the construction of the router, e.g., invalid
    * parameters.
    * @param cc the call center model.
    * @param par the router's parameters.
    * @return the new router, or \texttt{null}.
    */
   public Router createRouter (CallCenter cc, RouterManager rm, RouterParams par) throws RouterCreationException;
}
