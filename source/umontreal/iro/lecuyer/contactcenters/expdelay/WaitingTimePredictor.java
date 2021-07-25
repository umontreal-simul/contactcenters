package umontreal.iro.lecuyer.contactcenters.expdelay;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;

/**
 * Represents a heuristic that can predict the waiting
 * time of a contact depending on the system's state.
 * Such predictions can be used, e.g., for routing,
 * altering patience time, etc.
 * A predictor can have an associated router which is
 * used to obtain system state necessary for predictions.
 * It can also register listeners in order to
 * receive additional information.
 * The method {@link #getWaitingTime(Contact)}
 * is used to get a prediction of the waiting time
 * for a given contact waiting in any queue.
 * The method {@link #getWaitingTime(Contact,WaitingQueue)}, on the
 * other hand, gives a prediction of the waiting time for a contact
 * waiting in a specific queue.
 */
public interface WaitingTimePredictor
{
   /**
    * Returns a reference to the router associated with
    * this predictor.
    * By default, this returns \texttt{null} since no
    * router is bound to a newly-constructed predictor.
    * A router is associated with a predictor
    * using the {@link #setRouter(Router)} method.
    * @return a reference to the currently associated router.
    */
   public Router getRouter();

   /**
    * Sets the router associated with this predictor to
    * \texttt{router}.
    * When \texttt{router} is non-\texttt{null}, this
    * method can also register any listener required
    * to make the predictions.
    * If the router associated with a predictor is changed,
    * the predictor should unregister any listener associated
    * with the previous router.
    * @param router the new router.
    */
   public void setRouter (Router router);

   /**
    * Resets any internal variable of this predictor.
    */
   public void init ();

   /**
    * Returns a prediction of the waiting time
    * of contact \texttt{contact} waiting in any
    * queue.
    * This method returns {@link Double#NaN}
    * if it cannot make a prediction for the given contact.
    * @param contact the contact for which we need a prediction.
    * @return the global waiting time.
    */
   public double getWaitingTime (Contact contact);

   /**
    * Returns a prediction of the waiting time
    * for the given contact
    * \texttt{contact} conditional on the contact
    * joining the waiting
    * queue \texttt{queue}.
    * This method returns {@link Double#NaN}
    * if it cannot make a prediction for the given contact,
    * or the given waiting queue.
    * @param contact the contact for which a delay is predicted.
    * @param queue the target waiting queue.
    * @return the predicted delay.
    */
   public double getWaitingTime (Contact contact, WaitingQueue queue);
}
