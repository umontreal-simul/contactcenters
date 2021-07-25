package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This router implements the local-specialist policy which
 * tries to assign contacts to agents in the same region and
 * prefers specialists to preserve generalists.
 * This router associates a region identifier with each
 * contact type and agent group.
 * The \emph{originating region} of a contact is determined by
 * the region identifier associated with its type.
 * The \emph{location} of agents in an agent group is determined
 * by the region identifier associated with the agent group.
 * This policy is similar to agents' preference-based routing, but
 * it adds a region tie breaker and the rank $\rGT(i, k)$ can be considered as
 * a measurement of the specialty of agents in group~$i$ in serving
 * contacts of type~$k$.  Often, $\rTG(k, i)=\rGT(i, k)$.
 *
 * When a new contact arrives, the router applies the same
 * agent selection scheme as the agents'  preference-based router,
 * except that only agent groups within the originating region of the contact
 * are accepted as candidates.  If the contact cannot be served locally,
 * it is added to a waiting queue corresponding to its type.
 * After the contact spent a certain time in queue, called the
 * \emph{overflow delay},
 * the router tries to perform a new agent selection, this time
 * allowing local and remote agents to serve the contact.  If the contact
 * can be served remotely, it is removed from the waiting queue before
 * service starts.  Otherwise, it stays in queue.
 *
 * When an agent becomes free, the same contact selection scheme
 * as with the agents' preference-based router is applied, except that
 * a contact can be pulled from a waiting queue only if its originating
 * region is the same as the location of the free agent.
 * In other words, the local waiting queues are queried first.
 * If, after this first pass, the agent is still free, the router
 * performs a second pass which proceeds the
 * same way as agents' preference-based, except that
 * a contact can be pulled from a waiting queue only if it is
 * in the same region as the free agent, or its
 * waiting time is greater than the overflow delay.
 *
 * Often, $\rGT(i, k)=s(i)$ for each $k$ corresponding to a contact type
 * the agents in group~$i$ can serve, and $\rTG(k, i)=\rGT(i, k)$.
 * The function $s(i)$ is the \emph{skill count}
 * for agent group~$i$, i.e., the number of contact types
 * agents in group~$i$ can serve.
 * An agent in group~$i_1$ is more specialist
 * than an agent in group~$i_2$ if $s(i_1)< s(i_2)$.
 * With this format of matrix,
 * if an agent becomes free, local waiting queues are queried first
 * and the contact with the longest weighted waiting time is pulled.
 * Moreover, if weights $\wGT(i,k)$ are all set to 1 (the default),
 * only the location of the free agent induces priority for
 * contact selection.
 */
public class LocalSpecRouter extends AgentsPrefRouter {
   private double overflowDelay;
   private int[] typeRegion;
   private int[] groupRegion;
   private boolean localOnly = true;

   /**
    * Constructs a local-specialist router with contact type region identifiers
    * \texttt{typeRegion}, agent group region \texttt{groupRegion},
    * overflow delay \texttt{overflowDelay}, skill counts
    * \texttt{skill\-Counts}, and incidence matrix \texttt{m}.
    * The rank function $\rGT(i, k)$ is set to \texttt{skillCounts[i]}
    * if \texttt{m[i][k]} is \texttt{true} and $\infty$
    * otherwise while $\rTG(k, i)=\rGT(i, k)$.
    * The incidence matrix has one row per agent group and one column
    * per contact type; the
    * boolean \texttt{m[i][k]} determines if an agent group \texttt{i}
    * can serve a contact type \texttt{k}.
    * If \texttt{skillCounts} is \texttt{null}, the skill count
    * for agent group~$i$ is determined by counting the
    * number of $k$ values for which \texttt{m[i][k]} is \texttt{true}.
    * The weights matrices are initialized with 1's.
    @param typeRegion the contact type region identifiers.
    @param groupRegion the agent group region identifiers.
    @param overflowDelay the delay before overflow is allowed.
    @param skillCounts the number of skills for each agent group.
    @param m the incidence matrix.
    @exception NullPointerException if \texttt{typeRegion}, \texttt{groupRegion},
    or \texttt{m} are \texttt{null}.
    @exception IllegalArgumentException if the overflow delay is negative
    or the incidence matrix is non rectangular.
    */
   public LocalSpecRouter (int[] typeRegion, int[] groupRegion,
                           double overflowDelay, int[] skillCounts, boolean[][] m) {
      super (makeRanksGT (m, skillCounts));
      initParams (typeRegion, groupRegion, overflowDelay);
   }

   /**
    * Constructs a local-specialist router with contact type region identifiers
    * \texttt{typeRegion}, agent group region identifiers \texttt{groupRegion},
    * overflow delay \texttt{overflowDelay}, and contact selection
    * ranks matrix \texttt{ranksGT}.
    * The agent selection ranks matrix is generated by
    * transposing the contact selection matrix.
    * The weights matrices are initialized with 1's.
    @param typeRegion the contact type region identifiers.
    @param groupRegion the agent group region identifiers.
    @param overflowDelay the delay before overflow is allowed.
    @param ranksGT the matrix giving the $\rGT(i, k)$ function.
    @exception NullPointerException if \texttt{typeRegion}, \texttt{groupRegion},
    or \texttt{ranks} are \texttt{null}.
    @exception IllegalArgumentException if the overflow delay is negative.
    */
   public LocalSpecRouter (int[] typeRegion, int[] groupRegion,
                           double overflowDelay, double[][] ranksGT) {
      super (ranksGT);
      initParams (typeRegion, groupRegion, overflowDelay);
   }

   /**
    * Constructs a local-specialist router with contact type region identifiers
    * \texttt{typeRegion}, agent group region identifiers \texttt{groupRegion},
    * overflow delay \texttt{overflowDelay}, agent selection
    * ranks matrix \texttt{ranksTG}, and contact selection
    * ranks matrix \texttt{ranksGT}.
    * The weights matrices are initialized with 1's.
    @param typeRegion the contact type region identifiers.
    @param groupRegion the agent group region identifiers.
    @param overflowDelay the delay before overflow is allowed.
    @param ranksTG the matrix giving the $\rTG(k, i)$ function.
    @param ranksGT the matrix giving the $\rGT(i, k)$ function.
    @exception NullPointerException if \texttt{typeRegion}, \texttt{groupRegion},
    or \texttt{ranks} are \texttt{null}.
    @exception IllegalArgumentException if the overflow delay is negative.
    */
   public LocalSpecRouter (int[] typeRegion, int[] groupRegion,
                           double overflowDelay, double[][] ranksTG, double[][] ranksGT) {
      super (ranksTG, ranksGT);
      initParams (typeRegion, groupRegion, overflowDelay);
   }

   /**
    * Constructs a local-specialist router with contact type region identifiers
    * \texttt{typeRegion}, agent group region identifiers \texttt{groupRegion},
    * overflow delay \texttt{overflowDelay}, agent selection
    * ranks matrix \texttt{ranksTG}, and contact selection
    * ranks matrix \texttt{ranksGT}.
    * The weights matrices are set to
    * \texttt{weightsTG}, and \texttt{weightsGT}.
    @param typeRegion the contact type region identifiers.
    @param groupRegion the agent group region identifiers.
    @param overflowDelay the delay before overflow is allowed.
    @param ranksTG the matrix giving the $\rTG(k, i)$ function.
    @param ranksGT the matrix giving the $\rGT(i, k)$ function.
    @param weightsTG the weights matrix defining $\wTG(k, i)$.
    @param weightsGT the weights matrix defining $\wGT(i, k)$.
    @exception NullPointerException if \texttt{typeRegion}, \texttt{groupRegion},
    ranks or weights matrices are \texttt{null}.
    @exception IllegalArgumentException if the overflow delay is negative.
    */
   public LocalSpecRouter (int[] typeRegion, int[] groupRegion,
                           double overflowDelay, double[][] ranksTG, double[][] ranksGT,
                           double[][] weightsTG, double[][] weightsGT) {
      super (ranksTG, ranksGT, weightsTG, weightsGT);
      initParams (typeRegion, groupRegion, overflowDelay);
   }

   private static double[][] makeRanksGT (boolean[][] m, int[] skillCounts) {
      ArrayUtil.checkRectangularMatrix (m);
      return RoutingTableUtils.getRanks (m, skillCounts);
   }

   private void initParams (int[] typeRegion1, int[] groupRegion1, double overflowDelay1) {
      if (overflowDelay1 < 0)
         throw new IllegalArgumentException
            ("The overflow delay must not be negative");
      if (typeRegion1.length != getNumContactTypes())
         throw new IllegalArgumentException
            ("A region code is needed for each contact type");
      if (groupRegion1.length != getNumAgentGroups())
         throw new IllegalArgumentException
            ("A region code is needed for each agent group");
      this.overflowDelay = overflowDelay1;
      this.typeRegion = typeRegion1.clone ();
      this.groupRegion = groupRegion1.clone ();
   }

   /**
    * Returns the current overflow delay for this router.
    @return the current overflow delay.
    */
   public double getOverflowDelay() {
      return overflowDelay;
   }

   /**
    * Sets the overflow delay to \texttt{overflowDelay}.
    @param overflowDelay the new overflow delay.
    @exception IllegalArgumentException if the overflow delay is negative.
    */
   public void setOverflowDelay (double overflowDelay) {
      if (overflowDelay < 0)
         throw new IllegalArgumentException
            ("The overflow delay must not be negative");
      this.overflowDelay = overflowDelay;
   }

   /**
    * Returns the region identifier for contact type \texttt{k}.
    @param k the contact type identifier.
    @return the associated region identifier.
    @exception ArrayIndexOutOfBoundsException if \texttt{k} is negative
    or greater than or equal to the number of contact types.
    */
   public int getTypeRegion (int k) {
      return typeRegion[k];
   }

   /**
    * Sets the region identifier for contact type \texttt{k} to \texttt{r}.
    @param k the contact type identifier.
    @param r the new region identifier.
    @exception ArrayIndexOutOfBoundsException if \texttt{k} is negative
    or greater than or equal to the number of contact types.
    */
   public void setTypeRegion (int k, int r) {
      typeRegion[k] = r;
   }

   /**
    * Returns the region identifier for agent group \texttt{i}.
    @param i the agent group identifier.
    @return the associated region identifier.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to the number of agent groups.
    */
   public int getGroupRegion (int i) {
      return groupRegion[i];
   }

   /**
    * Sets the region identifier for agent group \texttt{i} to \texttt{r}.
    @param i the agent group identifier.
    @param r the new region identifier.
    @exception ArrayIndexOutOfBoundsException if \texttt{i} is negative
    or greater than or equal to the number of agent groups.
    */
   public void setGroupRegion (int i, int r) {
      groupRegion[i] = r;
   }

   /**
    * Computes a type-to-group map from the ranks matrix
    * by calling {@link RoutingTableUtils#getTypeToGroupMap(double[][],int[],int[])}, and
    * returns the result.
    @return the computed type-to-group map.
    */
   @Override
   public int[][] getTypeToGroupMap() {
      return RoutingTableUtils.getTypeToGroupMap
         (getRanksTG(), typeRegion, groupRegion);
   }

   /**
    * Computes a group-to-type map from the ranks matrix
    * by calling {@link RoutingTableUtils#getGroupToTypeMap(double[][],int[],int[])}, and
    * returns the result.
    @return the computed group-to-type map.
    */
   @Override
   public int[][] getGroupToTypeMap() {
      return RoutingTableUtils.getGroupToTypeMap
         (getRanksGT(), typeRegion, groupRegion);
   }

   @Override
   protected EndServiceEvent selectAgent (Contact ct) {
      localOnly = true;
      return super.selectAgent (ct);
   }

   @Override
   protected EndServiceEvent selectAgent (DequeueEvent dqEv, int numReroutings) {
      if (numReroutings == 0) {
         localOnly = false;
         return super.selectAgent (dqEv.getContact());
      }
      return null;
   }

   @Override
   protected double getReroutingDelay (DequeueEvent dqEv, int numReroutings) {
      if (numReroutings == -1 && !Double.isInfinite (overflowDelay))
         return overflowDelay;
      return -1;
   }

   @Override
   protected boolean checkFreeAgents (AgentGroup group, Agent agent) {
      // First, query local queues only
      localOnly = true;
      boolean oneBusy = false;
      if (super.checkFreeAgents (group, agent))
         oneBusy = true;
      // If there is still free agents in the group, query remote queues
      if (group.getNumFreeAgents() > 0) {
         localOnly = false;
         if (super.checkFreeAgents (group, oneBusy ? null : agent))
            oneBusy = true;
      }
      return oneBusy;
   }

   @Override
   protected double getScoreForAgentSelection (Contact ct, AgentGroup testGroup, Agent testAgent) {
      if (localOnly && typeRegion[ct.getTypeId()] != groupRegion[testGroup.getId()])
         return Double.NEGATIVE_INFINITY;
      return super.getScoreForAgentSelection (ct, testGroup, testAgent);
   }

   @Override
   protected double getScoreForContactSelection (AgentGroup group, DequeueEvent ev) {
      assert !ev.dequeued();
      if (localOnly)
         if (typeRegion[ev.getContact().getTypeId()] == groupRegion[group.getId()])
            // Only local queues are queried or the contact is local to the agent group
            return super.getScoreForContactSelection (group, ev);
         else
            return Double.NEGATIVE_INFINITY;
      // The contact to be dequeued is served remotely.
      final double qt = ev.simulator().time() - ev.getEnqueueTime();
      final int i = group.getId();
      final int k = ev.getContact().getTypeId();
      final double w = getWeightGT (i, k);
      if (typeRegion[k] != groupRegion[i] &&
          qt < overflowDelay)
         // The contact has not waited long enough to be served
         // remotely. Return an infinite score to disallow it
         // from being dequeued.
         return Double.NEGATIVE_INFINITY;
      else
         // Return the weighted waiting time
         return w*qt;
   }

   @Override
   public String getDescription() {
      return "Local-specialists router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Overfloe delay: ").append (overflowDelay).append ('\n');
      return sb.toString();
   }
}
