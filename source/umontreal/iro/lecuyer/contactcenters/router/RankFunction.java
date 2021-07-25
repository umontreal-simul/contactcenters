package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents a function computing a vector of ranks
 * for a given contact, for the
 * {@link OverflowAndPriorityRouter} router.
 */
public interface RankFunction {
   /**
    * Fills the array \texttt{ranks} with the ranks for the contact
    * \texttt{contact}.
    * The given array should have length $I$, and is filled
    * by this method with ranks.
    * The function might use the given contact as well
    * as any relevant model's state to determine
    * the ranks.
    * Note that additional routing information
    * can be obtained through the
    * {@link OverflowAndPriorityRouter.RoutingInfo}.
    * 
    * The vector of ranks given to this method is constructed by the
    * router, and associated to a specific call.
    * When this method is called for a new call,
    * the vector contains {@link Double#POSITIVE_INFINITY} values.
    * For any subsequent calls, the vector contains the current ranks
    * for the call.
    * This method should replace these values with
    * the new ranks concerning the call.
    * The method returns \texttt{true} if and only if at least
    * one of the ranks in the given vector needs to be updated.
    * Otherwise, it returns \texttt{false}.
    * @param contact the contact being routed.
    * @param ranks the vector filled with ranks.
    * @return \texttt{true} if the vector of ranks
    * was modified, \texttt{false} otherwise.
    */
   public boolean updateRanks (Contact contact, double[] ranks);
   
   /**
    * Determines if {@link #updateRanks(Contact,double[])}
    * can return a finite rank at position \texttt{i} for
    * this particular function.
    */
   public boolean canReturnFiniteRank (int i);
}
