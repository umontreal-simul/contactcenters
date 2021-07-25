package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.ssj.rng.RandomStream;

/**
 * Provides some convenience methods to select an agent
 * from a list of agent groups. All the methods provided
 * by this class are static and return a reference
 * to the selected agent group. If no agent group is
 * available, they return \texttt{null}.
 * They must be given an array of indices \texttt{ind} used
 * to reference
 * agent groups in the given router. One can also
 * specify an optional array of booleans \texttt{subset} indicating which
 * element in the list will be taken into account.
 *
 * For each index \texttt{j}, let \texttt{i = ind[j]}.
 * If \texttt{r >= 0} and \texttt{subset[j]} is \texttt{true} if
 * the subset is specified, the agent group
 * {@link Router#getAgentGroup Router.get\-Agent\-Group}
 * \texttt{(i)} will be considered. Otherwise, \texttt{i} will
 * be ignored.
 */
public final class AgentGroupSelectors {
   private AgentGroupSelectors() {}

   /**
    * Selects, from the given ordered list, the first agent group
    * containing at least one free agent.
    @param router the router used to map indices in the ordered list
    to {@link AgentGroup} references.
    @param ind the ordered list of agent group indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected agent group.
    */
   public static AgentGroup selectFirst (Router router, int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         if (group != null && group.getNumFreeAgents() > 0)
            return group;
      }
      return null;
   }

   /**
    * Equivalent to {@link #selectFirst selectFirst} \texttt{(router, ind, null)}.
    */
   public static AgentGroup selectFirst (Router router, int[] ind) {
      for (final int element : ind) {
         if (element < 0)
            continue;
         final AgentGroup group = router.getAgentGroup (element);
         if (group != null && group.getNumFreeAgents() > 0)
            return group;
      }
      return null;
   }

   /**
    * Selects, from the given ordered list, the last agent group
    * containing at least one free agent.
    @param router the router used to map indices in the ordered list
    to {@link AgentGroup} references.
    @param ind the ordered list of agent group indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected agent group.
    */
   public static AgentGroup selectLast (Router router, int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      for (int j = ind.length - 1; j >= 0; j--) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         if (group != null && group.getNumFreeAgents() > 0)
            return group;
      }
      return null;
   }

   /**
    * Equivalent to {@link #selectLast selectLast} \texttt{(router, ind, null)}.
    */
   public static AgentGroup selectLast (Router router, int[] ind) {
      for (int j = ind.length - 1; j >= 0; j--) {
         if (ind[j] < 0)
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         if (group != null && group.getNumFreeAgents() > 0)
            return group;
      }
      return null;
   }

   /**
    * Returns a reference to the agent group, among the groups referred to by the
    * given list of indices,
    * containing the greatest number of free agents.
    @param router the router used to map indices in the list
    to {@link AgentGroup} references.
    @param ind the list of agent group indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected agent group.
    */
   public static AgentGroup selectGreatestFree (Router router, int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      AgentGroup best = null;
      int bestFree = 0;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         final int nFree = group == null ? 0 : group.getNumFreeAgents();
         if (nFree > bestFree) {
            best = group;
            bestFree = nFree;
         }
      }
      return best;
   }

   /**
    * Equivalent to {@link #selectGreatestFree selectGreatestFree} \texttt{(router, ind, null)}.
    */
   public static AgentGroup selectGreatestFree (Router router, int[] ind) {
      AgentGroup best = null;
      int bestFree = 0;
      for (final int element : ind) {
         if (element < 0)
            continue;
         final AgentGroup group = router.getAgentGroup (element);
         final int nFree = group == null ? 0 : group.getNumFreeAgents();
         if (nFree > bestFree) {
            best = group;
            bestFree = nFree;
         }
      }
      return best;
   }

   /**
    * Returns a reference to a randomly selected agent group,
    * among the groups referred to by the given list of indices.
    * The probability of group $i$ to be selected is
    * given by $\Nf[i](t)/\Nf(t)$,
    * where $\Nf[i](t)$ is the number of free agents
    * in group~$i$ at current simulation time,
    * and $\Nf(t)$ is the total number of free agents
    * in the groups referred to by the indices.
    @param router the router used to map indices in the given list
    to {@link AgentGroup} references.
    @param ind the list of agent group indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @param stream the random number stream to generate one uniform.
    @return the selected agent group.
    */
   public static AgentGroup selectUniform (Router router, int[] ind, boolean[] subset,
                                           RandomStream stream) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      int nAvail = 0;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         if (group != null && group.getNumFreeAgents() > 0)
            nAvail += group.getNumFreeAgents();
      }
      // A uniform is generated, independently of the state of
      // the system. This helps for random number synchronization.
      double u = stream.nextDouble();
      if (nAvail == 0)
         return null;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         final int nFree = group == null ? 0 : group.getNumFreeAgents();
         if (nFree > 0) {
            final double prob = (double)nFree/nAvail;
            assert prob >= 0 && prob <= 1 :
               "Invalid probability of agent group " + ind[j] + " to be selected (" +
               prob + ")";
            if (u <= prob)
               return group;
            u -= prob;
         }
      }
      throw new AssertionError
         ("The method could not randomly select an available agent " +
          "although there were available agents");
   }

   /**
    * Equivalent to {@link #selectUniform selectUniform}
    * \texttt{(router, ind, null, stream)}.
    */
   public static AgentGroup selectUniform (Router router, int[] ind,
                                           RandomStream stream) {
      return selectUniform (router, ind, null, stream);
   }

   /**
    * Returns the reference to the agent having the longest idle
    * time among the agent groups indexed by the list \texttt{ind}
    * and possibly restricted by \texttt{subset} if it is non-\texttt{null}.
    * This selection rule will be applied only to {@link DetailedAgentGroup}
    * linked to the router. Indices mapping to an {@link AgentGroup} instance
    * will be ignored.
    @param router the router used to map indices in the given list
    to {@link AgentGroup} references.
    @param ind the list of agent group indices.
    @param subset the subset of indices to take into account when traversing
    the given list.
    @return the selected agent.
    */
   public static Agent selectLongestIdle (Router router, int[] ind, boolean[] subset) {
      if (subset != null && subset.length != ind.length)
         throw new IllegalArgumentException
            ("Invalid length of subset: " + subset.length);
      Agent bestAgent = null;
      double bestTime = Double.POSITIVE_INFINITY;
      for (int j = 0; j < ind.length; j++) {
         if (ind[j] < 0)
            continue;
         if (subset != null && !subset[j])
            continue;
         final AgentGroup group = router.getAgentGroup (ind[j]);
         if (!(group instanceof DetailedAgentGroup))
            // Continue if group is null or not an instance of
            // DetailedAgentGroup.
            continue;
         final DetailedAgentGroup dgroup = (DetailedAgentGroup)group;
         final Agent agent = dgroup.getLongestIdleAgent();
         if (agent == null)
            continue;
         final double st = agent.getIdleSimTime();
         if (st < bestTime) {
            bestTime = st;
            bestAgent = agent;
         }
      }
      return bestAgent;
   }

   /**
    * Equivalent to {@link #selectLongestIdle selectLongestIdle}
    * \texttt{(router, ind, null)}.
    */
   public static Agent selectLongestIdle (Router router, int[] ind) {
      Agent bestAgent = null;
      double bestTime = Double.POSITIVE_INFINITY;
      for (final int element : ind) {
         if (element < 0)
            continue;
         final AgentGroup group = router.getAgentGroup (element);
         if (!(group instanceof DetailedAgentGroup))
            // Continue if group is null or not an instance of
            // DetailedAgentGroup.
            continue;
         final DetailedAgentGroup dgroup = (DetailedAgentGroup)group;
         final Agent agent = dgroup.getLongestIdleAgent();
         if (agent == null)
            continue;
         final double st = agent.getIdleSimTime();
         if (st < bestTime) {
            bestTime = st;
            bestAgent = agent;
         }
      }
      return bestAgent;
   }
}
