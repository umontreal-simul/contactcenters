package umontreal.iro.lecuyer.contactcenters.app;

import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouter;
import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouterWithDelays;
import umontreal.iro.lecuyer.contactcenters.router.ExpDelayRouter;
import umontreal.iro.lecuyer.contactcenters.router.LocalSpecRouter;
import umontreal.iro.lecuyer.contactcenters.router.LongestQueueFirstRouter;
import umontreal.iro.lecuyer.contactcenters.router.LongestWeightedWaitingTimeRouter;
import umontreal.iro.lecuyer.contactcenters.router.OverflowAndPriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueueAtLastGroupRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueueRatioOverflowRouter;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;

/**
 * Represents the type of router's policies supported
 * by blend/multi-skill call center simulations.
 * This policy determines how the router assigns an agent to
 * incoming calls and how free agents look for queued calls.
 @xmlconfig.title Available router's policies
 */
public enum RouterPolicyType {
   /**
    * \javadoc{Queue priority routing policy.
    * See {@link QueuePriorityRouter} for more
    * information.}\begin{xmldocenv}{@linkplain QueuePriorityRouter}
    * \end{xmldocenv}
    * This routing policy requires a type-to-group and a group-to-type maps.
    @xmlconfig.title
    */
   QUEUEPRIORITY (Messages.getString("RouterPolicyType.QueuePriority")), //$NON-NLS-1$

   /**
    * \javadoc{Queue at last group routing policy.
    * See {@link QueueAtLastGroupRouter} for more
    * information.}\begin{xmldocenv}{@linkplain QueueAtLastGroupRouter}
    * \end{xmldocenv}
    * This routing policy requires a type-to-group map only.
    @xmlconfig.title
    */
   QUEUEATLASTGROUP (Messages.getString("RouterPolicyType.QueueAtLastGroup")), //$NON-NLS-1$

   /**
    * \javadoc{Longest queue first routing policy.
    * See {@link LongestQueueFirstRouter} for more
    * information.}\begin{xmldocenv}{@linkplain LongestQueueFirstRouter}
    * \end{xmldocenv}
    * This routing policy requires a type-to-group and a group-to-type maps.
    * Since the group-to-type map is used as a tie breaker only,
    * it is not as important as with the queue priority routing policy, but
    * it must be specified as well.
    @xmlconfig.title
    */
   LONGESTQUEUEFIRST (Messages.getString("RouterPolicyType.LongestQueueFirst")), //$NON-NLS-1$

   /**
    * \javadoc{Single FIFO queue router policy.
    * See {@link SingleFIFOQueueRouter} for more
    * information.}\begin{xmldocenv}{@linkplain SingleFIFOQueueRouter}
    * \end{xmldocenv}
    * This routing policy requires a type-to-group and a group-to-type maps.
    * Since the group-to-type map is used as a tie breaker only,
    * it is not as important as with the queue priority routing policy, but it
    * must be specified as well.
    @xmlconfig.title
    */
   SINGLEFIFOQUEUE (Messages.getString("RouterPolicyType.SingleFIFOQueue")), //$NON-NLS-1$

   /**
    * \javadoc{Longest weighted waiting time router policy.
    * See {@link LongestWeightedWaitingTimeRouter} for more
    * information.}\begin{xmldocenv}{@linkplain LongestWeightedWaitingTimeRouter}
    * \end{xmldocenv}
    * This routing policy requires a type-to-group, a group-to-type maps,
    * and an array of weights.
    * Since the group-to-type map is used as a tie breaker only,
    * it is not as important as with the queue priority routing policy, but it
    * must be specified as well.
    @xmlconfig.title
    */
   LONGESTWEIGHTEDWAITINGTIME (Messages.getString("RouterPolicyType.LongestWeightedWaitingTime")), //$NON-NLS-1$

   /**
    * \javadoc{Agents' preference-based routing policy.
    * See {@link AgentsPrefRouter} for more
    * information.}\begin{xmldocenv}{@linkplain AgentsPrefRouter}
    * \end{xmldocenv}
    @xmlconfig.title
    */
   AGENTSPREF (Messages.getString("RouterPolicyType.AgentsPref")), //$NON-NLS-1$

   /**
    * \javadoc{Agents' preference-based routing policy with delays.
    * See {@link AgentsPrefRouterWithDelays} for more
    * information.}\begin{xmldocenv}{@linkplain AgentsPrefRouterWithDelays}
    * \end{xmldocenv}
    @xmlconfig.title
    */
   AGENTSPREFWITHDELAYS (Messages.getString("RouterPolicyType.AgentsPrefWithDelays")), //$NON-NLS-1$

   /**
    * \javadoc{Local-specialist routing policy.
    * See {@link LocalSpecRouter}
    * for more information.}\begin{xmldocenv}{@linkplain LocalSpecRouter}
    * \end{xmldocenv}
    * To use this router, one must specify contact type and agent group names
    * containing a region name.
    * If available, this router uses the matrices of ranks to select
    * agents and waiting queues.
    @xmlconfig.title
    */
   LOCALSPEC (Messages.getString("RouterPolicyType.LocalSpec")), //$NON-NLS-1$

   /**
    * \javadoc{Overflow routing policy using queue ratio.
    * See {@link QueueRatioOverflowRouter} for more
    * information.}\begin{xmldocenv}{@linkplain QueueRatioOverflowRouter}
    * \end{xmldocenv}
    * This routing policy requires a contacts-to-agents matrix of ranks.
    @xmlconfig.title
    */
   QUEUERATIOOVERFLOW ("Queue ratio overflow"),

   /**
    * \javadoc{Routing policy using expected delay to select
    * agent groups.
    * See {@link ExpDelayRouter} for more
    * information.}\begin{xmldocenv}{@linkplain ExpDelayRouter}
    * \end{xmldocenv}
    * This routing policy requires a contacts-to-agents weights matrix.
    @xmlconfig.title
    */
   EXPDELAY ("Queue ratio overflow"),

   /**
    * \javadoc{Routing policy based on overflow and priority.
    * See {@link OverflowAndPriorityRouter} for more
    * information.}\begin{xmldocenv}{@linkplain OverflowAndPriorityRouter}
    * \end{xmldocenv}
    *
    * The $f_{k,j}$ and $g_{k,j}$ functions are defined using
    * sequences of triplets $(C_{k,j,l}, A_{k,j,l}, Q_{k,j,l})$, where
    * $C_{k,j,l}$ represents a condition, and $A_{k,j,l}$ and $Q_{k,j,l}$
    * are vectors.
    * First, the condition $C_{k,j,0}$ is checked.
    * If it is true, $A_{k,j,0}$ is used as the vector of ranks
    * for agent groups, and $Q_{k,j,0}$ is used to set up
    * priorities for queues.
    * Otherwise, the condition $C_{k,j,1}$ is checked, and the corresponding
    * vectors $A_{k,j,1}$, and $Q_{k,j,1}$ are used if the condition is true.
    * This check continues for other conditions $C_{k,j,2}, C_{k,j,3}, \ldots$,
    * until a true condition is found, or the list of cases is exhausted.
    * If a condition $C_{k,j,l}$ applies, and no vectors of ranks are
    * associated with this condition, the last vectors of ranks are preserved,
    * i.e., the $j$th routing stage has no effect.
    * If the list of cases is exhausted without finding an applicable
    * condition, a default set of vectors of ranks
    * is used.  If no such default vectors are given, the routing stage $j$
    * has no effect.
    *
    * A vector of ranks can also be set relatively to the preceding
    * vector.  When a relative vector is used,
    * the ranks are summed with the previous ranks, which
    * allows to update ranks rather than overriding them.
    * This can be useful to accumulate the effect of several
    * conditions at different stages of routing, e.g.,
    * increase priority at queue 1 depending on its size,
    * decrease priority at queue 2 depending on the number of free
    * agents, etc.
    *
    * This policy requires a script for each call type
    * in the parameter file for the call center.
    * Such a script is set up by a \texttt{call\-Type\-Routing}
    * element.
    * Such an element contains one ore more \texttt{stage} children describing the
    * stages of routing.
    * A \texttt{stage} element has an attribute
    * \texttt{waiting\-Time} giving the waiting time $w_{k,j}$ as well
    * as a sequence of \texttt{case} elements for the cases, and
    * a \texttt{default} element for the default vectors of ranks.
    * See the complex type \texttt{Call\-Type\-Routing\-Params}, in the
    * HTML documentation of the XML Schema for the complete syntax
    * of routing parameters, including how to encode conditions.
    @xmlconfig.title
    */
   OVERFLOWANDPRIORITY ("Overflow and priority");

   private String name;

   private RouterPolicyType (String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return name;
   }
}
