package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Performs agent and contact selection based on user-defined priorities. By
 * default, this router selects the agent with the longest idle time when
 * several agents share the same priority, and the longest waiting time to
 * perform a selection among contacts sharing the same priority. The agents'
 * preference-based router is a generalization of the router taken from
 * \cite{ccWHI04a}, using matrices of ranks to take its decisions. The router
 * applies static routing when the ranks are different and uses a dynamic policy
 * when they are equal. This permits the user to partially define the priorities
 * instead of assigning all of them as with the queue priority routing. For
 * example, the user can set the router for the first waiting queue to have
 * precedence over the others while the other queues share the same priority.
 *
 * \paragraph*{Data structures.}
 * Two matrices of ranks can be defined, one specifying how contacts prefer
 * agents, and a second one defining how agents prefer contacts. The former
 * matrix, used for agent selection, defines a function $\rTG(k, i)$ giving the
 * rank for contacts of type~$k$ served by agents in group~$i$. The latter
 * matrix, used for contact selection, defines a function $\rGT(i, k)$ giving
 * the rank of contacts of type~$k$ when agents in group~$i$ perform contact
 * selection. In many cases, one can specify $\rGT(i, k)$ only, and have
 * $\rTG(k, i)=\rGT(i, k)$.
 *
 * Additionally, the router uses matrices of weights to adjust the priority for
 * candidates with the same rank. These matrices define functions $\wTG(k, i)$
 * and $\wGT(i, k)$ which are similar to the ranks functions, except they can
 * take any real number. These matrices are optional and default to matrices of
 * 1's if they are not specified.
 *
 * \paragraph{Basic routing schemes.}
 * The priorities defined by matrices of ranks are used to assign agents to
 * incoming contacts, and contacts to free agents by performing several linear
 * searches over the space of agent groups or waiting queues. Each search
 * constructs or narrows a list of candidates until zero or one candidate is
 * retained. The general algorithm can be summarized as follows.
 * \begin{enumerate} \item Find a list of candidates sharing the lowest possible
 * rank, or equivalently the highest possible priority; \item Assign a score to
 * each selected candidate; \item Select the candidate with the best score.
 * \end{enumerate}
 *
 * \paragraph*{Agent selection.}
 * More specifically, when a new contact of type~$k$ arrives, the router
 * constructs an initial list of agent groups for which $\Nf[i](t)>0$, and
 * $\rTG(k,i)<\infty$. If this list of candidates contains several agent
 * groups, the router compares their ranks $\rTG(k, i)$, and retains the agent
 * groups with the minimal rank. If more than one candidates share the same
 * minimal rank, a score is assigned to each of them and the candidate with the
 * best score is taken. The default score of an agent group~$i$ is the longest
 * idle time of the agents in that group multiplied by the weight $\wTG(k, i)$
 * (which is 1 by default), also called the longest weighted idle time. For this
 * reason, agent groups linked to this router must be able to take individual
 * agents into account. In the rare event where two candidates share the best
 * score, i.e., two agent groups have the same weighted longest idle time, the
 * candidate with the smallest index~$i$ is retained. If, during the algorithm,
 * the list of candidates happens to be empty, the routed contact is put into a
 * waiting queue corresponding to its type, or blocked if the queue capacity is
 * exceeded. If the list of candidates contains a single agent group, this agent
 * group is selected and service starts.
 *
 * Note that for a fixed contact type~$k$, if $\rTG(k, i)$ is different for all
 * $i$ such that $\rTG(k, i)<\infty$, the scheme for agent selection is
 * equivalent to a pure overflow router: each agent group is tested in a fixed
 * order for a free agent. In that setting, the weights $\wTG(k, \cdot)$ have no
 * effect. On the other hand, if all finite values of $\rTG(k, i)$ for a fixed
 * $k$ are equal, the routing is completely based on the
 * longest-weighted-idle-time selection policy. Any intermediate combination of
 * these two extremes can be achieved by adjusting the ranks appropriately.
 *
 * \paragraph*{Contact selection.}
 * Since one waiting queue contains contacts of a single type, we define waiting
 * queue~$k$ as the queue containing only contacts of type~$k$. The router
 * assumes that every waiting queue uses a FIFO discipline. When an agent in
 * group~$i$ becomes free, an initial list of waiting queues containing at least
 * one contact, and for which $\rGT(i,k)<\infty$. If the list of candidates
 * contains several waiting queues, the waiting queues~$k$ with the minimal rank
 * are retained. If several waiting queues share this minimal rank, a score is
 * assigned to each candidate, and the waiting queue with the best score is
 * chosen. The default score of a waiting queue~$k$ is the weighted waiting time
 * of the first queued contact, i.e., the waiting time multiplied by $\wGT(i,
 * k)$. In the rare event where several waiting queues have the same minimal
 * rank, and the same best score, i.e., several queued contacts have the exact
 * same weighted waiting time, the waiting queue with the smallest index~$k$ is
 * chosen. If, at any time during the algorithm, the list of candidates becomes
 * empty, the tested agent remains free. When the list of candidates contains a
 * single waiting queue, the first contact in that waiting queue is assigned to
 * the free agent.
 *
 * Note that for a fixed agent group~$i$, if $\rGT(i, k)$ is different for all
 * $k$ such that $\rGT(i, k)<\infty$, this policy is equivalent to the queue
 * priority router's contact selection: the waiting queues are queried in a
 * fixed order for contacts. In that particular setting, the weights $\wGT(i,
 * \cdot)$ have no effect. On the other hand, if, for a fixed $i$, all finite
 * $\rGT(i, k)$ are equal for all $k$, the router uses the longest weighted
 * waiting time policy for agent group~$i$. As with agent selection, any
 * combination of these two extremes can be achieved by adjusting the ranks.
 *
 * \paragraph*{Randomized selection.}
 * By default, if several agent groups or waiting queues share the same
 * minimal rank, a score is assigned to each of them, and
 * the agent group or queue with the minimal score is chosen.
 * However, this selection can be randomized as follows.
 * Let $C_i$ be the score given to agent group $i$ during agent selection,
 * any negative score excluding
 * the concerned group being replaced with 0. When randomized agent selection
 * is used, the agent group~$i$ is selected with probability
 * $p_i=C_i/\sum_{i=0}^{I-1} C_i$. In other words, the highest score an agent
 * group obtains, the greatest is its probability of selection.
 * A similar logic applies for contact selection, with $C_i$
 * replaced by $C_k$, the score assigned to contact type $k$.
 */
public class AgentsPrefRouter extends Router {
   protected double[][] ranksTG;
   protected double[][] ranksGT;
   protected double[][] weightsTG;
   protected double[][] weightsGT;
   private AgentSelectionScore agentSelectionScore = AgentSelectionScore.LONGESTIDLETIME;
   private ContactSelectionScore contactSelectionScore = ContactSelectionScore.LONGESTWAITINGTIME;
   private RandomStream streamAgentSelection;
   private RandomStream streamContactSelection;

   private boolean[] candidates;
   private boolean[] qCandidates;
   private double[] scores;
   private double[] qScores;

   /**
    * Constructs a new agents' preference-based router with a group-to-type map
    * \texttt{groupToTypeMap} and \texttt{numTypes} contact types. This router
    * always uses queue priority for contact selection. The rank $\rGT(i, k)$ is
    * the value $j$ for which $k_{i, j} = k$ in the group-to-type map, and
    * $\rTG(k, i)=\rGT(i, k)$. The matrices of weights are initialized with 1's.
    *
    * @param numTypes
    *           the number of contact types.
    * @param groupToTypeMap
    *           the group-to-type map.
    */
   public AgentsPrefRouter (int numTypes, int[][] groupToTypeMap) {
      super (numTypes, numTypes, groupToTypeMap.length);
      ranksGT = RoutingTableUtils.getRanksFromGT (numTypes, groupToTypeMap);
      ranksTG = ArrayUtil.getTranspose (ranksGT);
      initDefaultWeights (numTypes, groupToTypeMap.length);
      initFields();
   }

   private void initFields() {
      candidates = new boolean[ranksGT.length];
      qCandidates = new boolean[ranksTG.length];
      scores = new double[candidates.length];
      qScores = new double[qCandidates.length];
   }

   private void initDefaultWeights (int K, int I) {
      weightsTG = new double[K][I];
      weightsGT = new double[I][K];
      for (int i = 0; i < I; i++)
         for (int k = 0; k < K; k++) {
            weightsTG[k][i] = 1;
            weightsGT[i][k] = 1;
         }
   }

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksGT} defining how agents prefer contacts. The given matrix
    * must be rectangular with one row per agent group, and one column per
    * contact type. It defines the function $\rGT(i, k)$ while $\rTG(k,
    * i)=\rGT(i, k)$. The matrices of weights are initialized with 1's.
    *
    * @param ranksGT
    *           the contact selection matrix of ranks being used.
    * @exception NullPointerException
    *               if \texttt{ranksGT} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the ranks 2D array is not rectangular.
    */
   public AgentsPrefRouter (double[][] ranksGT) {
      super (ranksGT[0].length, ranksGT[0].length, ranksGT.length);
      ArrayUtil.checkRectangularMatrix (ranksGT);
      this.ranksGT = ArrayUtil.deepClone (ranksGT, true);
      ranksTG = ArrayUtil.getTranspose (ranksGT);
      initDefaultWeights (ranksGT[0].length, ranksGT.length);
      initFields ();
   }

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksTG} defining how contacts prefer agents, and \texttt{ranksGT}
    * defining how agents prefer contacts. The given matrices must be
    * rectangular. The matrices of weights are initialized with 1's.
    *
    * @param ranksTG
    *           the matrix of ranks defining how contacts prefer agents.
    * @param ranksGT
    *           the matrix of ranks defining how agents prefer contacts.
    * @exception NullPointerException
    *               if \texttt{ranksGT} or \texttt{ranksTG} are \texttt{null}.
    * @exception IllegalArgumentException
    *               if the ranks 2D arrays are not rectangular.
    */
   public AgentsPrefRouter (double[][] ranksTG, double[][] ranksGT) {
      super (ranksGT[0].length, ranksGT[0].length, ranksGT.length);
      ArrayUtil.checkRectangularMatrix (ranksTG);
      ArrayUtil.checkRectangularMatrix (ranksGT);
      if (ranksTG.length != ranksGT[0].length
            || ranksGT.length != ranksTG[0].length)
         throw new IllegalArgumentException ("Incompatible matrix dimensions");
      this.ranksTG = ArrayUtil.deepClone (ranksTG, true);
      this.ranksGT = ArrayUtil.deepClone (ranksGT, true);
      initDefaultWeights (ranksGT[0].length, ranksGT.length);
      initFields ();
   }

   /**
    * Constructs a new agents' preference-based router with matrix of ranks
    * \texttt{ranksTG} defining how contacts prefer agents, and \texttt{ranksGT}
    * defining how agents prefer contacts. The matrices of weights are set to
    * \texttt{weightsTG}, and \texttt{weightsGT}. The given matrices must be
    * rectangular.
    *
    * @param ranksTG
    *           the matrix of ranks defining how contacts prefer agents.
    * @param ranksGT
    *           the matrix of ranks defining how agents prefer contacts.
    * @param weightsTG
    *           the matrix of weights defining $\wTG(k, i)$.
    * @param weightsGT
    *           the matrix of weights defining $\wGT(i, k)$.
    * @exception NullPointerException
    *               if \texttt{ranksGT}, \texttt{ranksTG}, \texttt{weightsTG},
    *               or \texttt{weightsGT} are \texttt{null}.
    * @exception IllegalArgumentException
    *               if the 2D arrays are not rectangular.
    */
   public AgentsPrefRouter (double[][] ranksTG, double[][] ranksGT,
         double[][] weightsTG, double[][] weightsGT) {
      super (ranksGT[0].length, ranksGT[0].length, ranksGT.length);
      ArrayUtil.checkRectangularMatrix (ranksTG);
      ArrayUtil.checkRectangularMatrix (ranksGT);
      ArrayUtil.checkRectangularMatrix (weightsTG);
      ArrayUtil.checkRectangularMatrix (weightsGT);
      if (ranksTG.length != ranksGT[0].length
            || ranksGT.length != ranksTG[0].length)
         throw new IllegalArgumentException ("Incompatible matrix dimensions");
      if (weightsTG.length != ranksTG.length
            || weightsTG[0].length != ranksGT.length)
         throw new IllegalArgumentException ("Invalid dimensions of weightsTG");
      if (weightsGT.length != ranksGT.length
            || weightsGT[0].length != ranksTG.length)
         throw new IllegalArgumentException ("Invalid dimensions of weightsGT");
      this.ranksTG = ArrayUtil.deepClone (ranksTG, true);
      this.ranksGT = ArrayUtil.deepClone (ranksGT, true);
      this.weightsTG = ArrayUtil.deepClone (weightsTG, true);
      this.weightsGT = ArrayUtil.deepClone (weightsGT, true);
      initFields ();
   }

   /**
    * Returns the matrix of ranks defining how contacts prefer agents, used for
    * agent selection.
    *
    * @return the matrix of ranks defining how contacts prefer agents.
    */
   public double[][] getRanksTG () {
      return ArrayUtil.deepClone (ranksTG, true);
   }

   /**
    * Sets the matrix of ranks defining how contacts prefer agents to
    * \texttt{ranksTG}.
    *
    * @param ranksTG
    *           the new agent selection matrix of ranks.
    * @exception NullPointerException
    *               if \texttt{ranksTG} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{ranksTG} is not rectangular or has wrong
    *               dimensions.
    */
   public void setRanksTG (double[][] ranksTG) {
      ArrayUtil.checkRectangularMatrix (ranksTG);
      if (ranksTG.length != getNumContactTypes ())
         throw new IllegalArgumentException (
               "Invalid number of rows in the matrix of ranks");
      if (ranksTG[0].length != getNumAgentGroups ())
         throw new IllegalArgumentException (
               "Invalid number of columns in the matrix of ranks");
      this.ranksTG = ArrayUtil.deepClone (ranksTG, true);
   }

   /**
    * Returns the matrix of ranks defining how agents prefer contacts, used for
    * contact selection.
    *
    * @return the matrix of ranks defining how agents prefer contacts.
    */
   public double[][] getRanksGT () {
      return ArrayUtil.deepClone (ranksGT, true);
   }

   /**
    * Sets the matrix of ranks defining how agents prefer contacts to
    * \texttt{ranksGT}.
    *
    * @param ranksGT
    *           the new contact selection matrix of ranks.
    * @exception NullPointerException
    *               if \texttt{ranksGT} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{ranksGT} is not rectangular or has wrong
    *               dimensions.
    */
   public void setRanksGT (double[][] ranksGT) {
      ArrayUtil.checkRectangularMatrix (ranksGT);
      if (ranksGT.length != getNumAgentGroups ())
         throw new IllegalArgumentException (
               "Invalid number of rows in the matrix of ranks");
      if (ranksGT[0].length != getNumContactTypes ())
         throw new IllegalArgumentException (
               "Invalid number of columns in the matrix of ranks");
      this.ranksGT = ArrayUtil.deepClone (ranksGT, true);
   }

   /**
    * Returns the matrix of weights defining $\wTG(k, i)$.
    *
    * @return the matrix of weights defining $\wTG(k, i)$.
    */
   public double[][] getWeightsTG () {
      return ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Sets the matrix of weights defining $\wTG(k, i)$ to \texttt{weightsTG}.
    *
    * @param weightsTG
    *           the new matrix of weights defining $\wTG(k, i)$.
    * @exception NullPointerException
    *               if \texttt{weightsTG} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{weightsTG} is not rectangular or has wrong
    *               dimensions.
    */
   public void setWeightsTG (double[][] weightsTG) {
      ArrayUtil.checkRectangularMatrix (weightsTG);
      if (weightsTG.length != ranksTG.length
            || weightsTG[0].length != ranksGT.length)
         throw new IllegalArgumentException ("Invalid dimensions of weightsTG");
      this.weightsTG = ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Returns the matrix of weights defining $\wGT(i, k)$.
    *
    * @return the matrix of weights defining $\wGT(i, k)$.
    */
   public double[][] getWeightsGT () {
      return ArrayUtil.deepClone (weightsGT, true);
   }

   /**
    * Sets the matrix of weights defining $\wGT(i, k)$ to \texttt{weightsGT}.
    *
    * @param weightsGT
    *           the new matrix of weights defining $\wGT(i, k)$.
    * @exception NullPointerException
    *               if \texttt{weightsGT} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{weightsGT} is not rectangular or has wrong
    *               dimensions.
    */
   public void setWeightsGT (double[][] weightsGT) {
      ArrayUtil.checkRectangularMatrix (weightsGT);
      if (weightsGT.length != ranksGT.length
            || weightsGT[0].length != ranksTG.length)
         throw new IllegalArgumentException ("Invalid dimensions of weightsGT");
      this.weightsGT = ArrayUtil.deepClone (weightsGT, true);
   }

   /**
    * Returns the rank of contact type~$k=$~\texttt{k} for agent
    * group~$i=$~\texttt{i}, used for agent selection.
    *
    * @param k
    *           the contact type identifier.
    * @param i
    *           the agent group identifier.
    * @return the rank of the contact type for the agent group.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{i} or \texttt{k} are negative, \texttt{i} is
    *               greater than or equal to {@link #getNumAgentGroups()}, or
    *               \texttt{k} is greater than or equal to
    *               {@link #getNumContactTypes()}.
    */
   public double getRankTG (int k, int i) {
      return ranksTG[k][i];
   }

   /**
    * Returns the rank of contact type~$k=$~\texttt{k} for agent
    * group~$i=$~\texttt{i}, used for contact selection.
    *
    * @param i
    *           the agent group identifier.
    * @param k
    *           the contact type identifier.
    * @return the rank of the contact type for the agent group.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{i} or \texttt{k} are negative, \texttt{i} is
    *               greater than or equal to {@link #getNumAgentGroups()}, or
    *               \texttt{k} is greater than or equal to
    *               {@link #getNumContactTypes()}.
    */
   public double getRankGT (int i, int k) {
      return ranksGT[i][k];
   }

   /**
    * Returns the weight of contact type~$k=$~\texttt{k} for agent
    * group~$i=$~\texttt{i}, used for agent selection.
    *
    * @param k
    *           the contact type identifier.
    * @param i
    *           the agent group identifier.
    * @return the weight of the contact type for the agent group.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{i} or \texttt{k} are negative, \texttt{i} is
    *               greater than or equal to {@link #getNumAgentGroups()}, or
    *               \texttt{k} is greater than or equal to
    *               {@link #getNumContactTypes()}.
    */
   public double getWeightTG (int k, int i) {
      return weightsTG[k][i];
   }

   /**
    * Returns the weight of contact type~$k=$~\texttt{k} for agent
    * group~$i=$~\texttt{i}, used for contact selection.
    *
    * @param i
    *           the agent group identifier.
    * @param k
    *           the contact type identifier.
    * @return the weight of the contact type for the agent group.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{i} or \texttt{k} are negative, \texttt{i} is
    *               greater than or equal to {@link #getNumAgentGroups()}, or
    *               \texttt{k} is greater than or equal to
    *               {@link #getNumContactTypes()}.
    */
   public double getWeightGT (int i, int k) {
      return weightsGT[i][k];
   }

   /**
    * Computes a type-to-group map from the agent selection matrix of ranks by
    * calling {@link RoutingTableUtils#getTypeToGroupMap(double[][])} on the
    * transpose of the matrix of ranks, and returns the result.
    *
    * @return the computed type-to-group map.
    */
   public int[][] getTypeToGroupMap () {
      return RoutingTableUtils.getTypeToGroupMap (ranksTG);
   }

   /**
    * Computes a group-to-type map from the contact selection matrix of ranks by
    * calling {@link RoutingTableUtils#getGroupToTypeMap(double[][])}, and
    * returns the result.
    *
    * @return the computed group-to-type map.
    */
   public int[][] getGroupToTypeMap () {
      return RoutingTableUtils.getGroupToTypeMap (ranksGT);
   }

   /**
    * Returns the random stream used for agent selection. If the agent selection
    * is not randomized (the default), this returns \texttt{null}.
    *
    * @return the random stream for agent selection.
    */
   public RandomStream getStreamAgentSelection () {
      return streamAgentSelection;
   }

   /**
    * Sets the random stream for agent selection to
    * \texttt{streamAgentSelection}. Setting the stream to \texttt{null}
    * disables randomized agent selection.
    *
    * @param streamAgentSelection
    *           the new random stream for agent selection.
    */
   public void setStreamAgentSelection (RandomStream streamAgentSelection) {
      this.streamAgentSelection = streamAgentSelection;
   }

   /**
    * Returns the random stream used for contact selection. If the contact
    * selection is not randomized (the default), this returns \texttt{null}.
    *
    * @return the random stream for contact selection.
    */
   public RandomStream getStreamContactSelection () {
      return streamContactSelection;
   }

   /**
    * Sets the random stream for contact selection to
    * \texttt{streamContactSelection}. Setting the stream to \texttt{null}
    * disables randomized contact selection.
    *
    * @param streamContactSelection
    *           the new random stream for contact selection.
    */
   public void setStreamContactSelection (RandomStream streamContactSelection) {
      this.streamContactSelection = streamContactSelection;
   }

   /**
    * Returns the current mode of computation for the agent selection score. The
    * default value is {@link AgentSelectionScore#LONGESTIDLETIME}.
    *
    * @return the way the score is computed for agent selection.
    */
   public AgentSelectionScore getAgentSelectionScore () {
      return agentSelectionScore;
   }

   /**
    * Sets the way scores for agent selection are computed to
    * \texttt{agentSelectionScore}.
    *
    * @param agentSelectionScore
    *           the way scores for agent selection are computed.
    * @exception NullPointerException
    *               if \texttt{agentSelectionScore} is \texttt{null}.
    */
   public void setAgentSelectionScore (AgentSelectionScore agentSelectionScore) {
      if (agentSelectionScore == null)
         throw new NullPointerException ();
      this.agentSelectionScore = agentSelectionScore;
   }

   /**
    * Returns the current mode of computation for the contact selection score.
    * The default value is {@link ContactSelectionScore#LONGESTWAITINGTIME}.
    *
    * @return the way the score is computed for contact selection.
    */
   public ContactSelectionScore getContactSelectionScore () {
      return contactSelectionScore;
   }

   /**
    * Sets the way scores for contact selection are computed to
    * \texttt{contactSelectionScore}.
    *
    * @param contactSelectionScore
    *           the way scores for contact selection are computed.
    * @exception NullPointerException
    *               if \texttt{contactSelectionScore} is \texttt{null}.
    */
   public void setContactSelectionScore (
         ContactSelectionScore contactSelectionScore) {
      if (contactSelectionScore == null)
         throw new NullPointerException ();
      this.contactSelectionScore = contactSelectionScore;
   }

   @Override
   public boolean canServe (int i, int k) {
      return !Double.isInfinite (ranksTG[k][i])
            || !Double.isInfinite (ranksGT[i][k]);
   }

   @Override
   public boolean needsDetailedAgentGroup (int i) {
      return agentSelectionScore == AgentSelectionScore.LONGESTIDLETIME
      && getNumAgentGroups () > 1;
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.CONTACTTYPE;
   }

   /**
    * Determines the rank to be used for agent selection for contact type
    * \texttt{k}, and agent in group \texttt{i}. By default, this returns
    * $\rTG(k, i)$, but subclasses may override this method for the rank to
    * depend on some state of the system.
    *
    * @param k
    *           the contact type index.
    * @param i
    *           the agent group index.
    * @return the rank associated with $(k, i)$.
    */
   protected double getRankForAgentSelection (int k, int i) {
      return ranksTG[k][i];
   }

   @Override
   protected EndServiceEvent selectAgent (Contact ct) {
      final int k = ct.getTypeId ();
      double bestRank = Double.POSITIVE_INFINITY;
      int numCandidates = 0;
      final int I = getNumAgentGroups ();
      for (int i = 0; i < I; i++) {
         candidates[i] = false;
         final double rank = getRankForAgentSelection (k, i);
         if (Double.isInfinite (rank) || rank > bestRank)
            continue;
         final AgentGroup group = getAgentGroup (i);
         if (group == null || group.getNumFreeAgents () == 0)
            // No free agent in the group
            continue;
         if (rank < bestRank) {
            if (!Double.isInfinite (bestRank)) {
               // We have found a candidate whose rank is lower
               // than the ranks of all preceding candidates, so
               // reset the list of candidates.
               for (int j = 0; j < i; j++)
                  candidates[j] = false;
            }
            bestRank = rank;
            candidates[i] = true;
            numCandidates = 1;
         }
         else if (rank == bestRank) {
            candidates[i] = true;
            ++numCandidates;
         }
      }
      if (numCandidates == 0)
         return null;
      assert !Double.isInfinite (bestRank);
      selectAgent (ct, bestRank, candidates, numCandidates);
      if (bestGroup == null)
         return null;
      final EndServiceEvent es;
      if (bestAgent != null) {
         assert bestAgent.getAgentGroup () == bestGroup;
         es = bestAgent.serve (ct);
      }
      else
         es = bestGroup.serve (ct);
      assert es != null : "AgentGroup.serve should not return null";
      bestGroup = null;
      bestAgent = null;
      return es;
   }

   /**
    * Best agent group selected by
    * {@link #selectAgent(Contact,double,boolean[],int)}.
    */
   protected AgentGroup bestGroup;

   /**
    * Best agent selected by {@link #selectAgent(Contact,double,boolean[],int)}, or
    * \texttt{null} if the best agent group does not take account of individual
    * agents.
    */
   protected Agent bestAgent;

   /**
    * This method is called by {@link #selectAgent(Contact)} to perform the
    * selection of an agent among
    * the \texttt{numCandidates} agent groups sharing the same minimal finite
    * rank and containing at least one free agent. The method must select an
    * agent group \texttt{i} among the agent groups for which
    * \texttt{candidates[i]} is \texttt{true} and store the reference to this
    * group in the protected field {@link #bestGroup}. The field
    * {@link #bestAgent} can be used to hold the selected agent if the agent
    * group takes account of individual agents. If no candidate is satisfactory,
    * the method must set {@link #bestGroup} and {@link #bestAgent} to
    * \texttt{null} before returning; the incoming contact will be queued or
    * blocked as appropriate.
    *
    * The default implementation computes a score for each candidate using
    * {@link #getScoreForAgentSelection(Contact,AgentGroup,Agent)} and takes the
    * candidate with the best score. This method can be overridden to implement
    * a different selection scheme, e.g., randomly selecting a free agent.
    *
    * @param ct
    *           the contact being processed.
    * @param bestRank
    *           the best rank found when looking for a candidate agent.
    * @param candidates1
    *           the agent group that could serve the contact.
    * @param numCandidates
    * the number of \texttt{true} values in \texttt{candidates}.
    */
   protected void selectAgent (Contact ct, double bestRank, boolean[] candidates1, int numCandidates) {
      bestGroup = null;
      bestAgent = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      double sumScores = 0;
      final int I = getNumAgentGroups ();
      for (int i = 0; i < I; i++) {
         if (!candidates1[i]) {
            scores[i] = 0;
            continue;
         }
         final AgentGroup group = getAgentGroup (i);
         final Agent testAgent;
         if (group instanceof DetailedAgentGroup)
            testAgent = ((DetailedAgentGroup) group).getLongestIdleAgent ();
         else
            testAgent = null;
         if (numCandidates == 1) {
            bestGroup = group;
            bestAgent = testAgent;
            return;
         }
         final double score = getScoreForAgentSelection (ct, group, testAgent);
         scores[i] = score;
         if (score < 0 && Double.isInfinite (score))
            continue;
         sumScores += score;
         if (score > bestScore) {
            bestGroup = group;
            bestAgent = testAgent;
            bestScore = score;
         }
      }
      if (bestGroup != null && streamAgentSelection != null
            && sumScores > bestScore) {
         for (int i = 0; i < I; i++)
            scores[i] /= sumScores;
         double u = streamAgentSelection.nextDouble ();
         bestGroup = null;
         bestAgent = null;
         for (int i = 0; i < I && bestGroup == null; i++)
            if (u < scores[i]) {
               bestGroup = getAgentGroup (i);
               if (bestGroup instanceof DetailedAgentGroup)
                  bestAgent = ((DetailedAgentGroup) bestGroup)
                        .getLongestIdleAgent ();
               else
                  bestAgent = null;
            }
            else
               u -= scores[i];
      }
   }

   /**
    * Returns the score for contact \texttt{ct} associated with agent group
    * \texttt{testGroup} and agent \texttt{testAgent}. When selecting an agent
    * for contact \texttt{ct}, if there are several agent groups with the same
    * minimal rank, the agent group with the greatest score is selected.
    * Returning a negative infinite score prevents an agent group from being
    * selected. The default {@link #selectAgent(Contact,double,boolean[],int)}
    * method calls this method with \texttt{testAgent = null} if
    * \texttt{testGroup} is not an instance of {@link DetailedAgentGroup},
    * otherwise the method is called with \texttt{testAgent = testGroup.}{@link DetailedAgentGroup#getLongestIdleAgent get\-Longest\-Idle\-Agent()}.
    *
    * By default, this returns a score depending on the return value of
    * {@link #getAgentSelectionScore()}. This can return the longest weighted
    * idle time (the default), the weighted number of free agents, or the weight
    * only. See {@link AgentSelectionScore} for more information.
    *
    * @param ct
    *           the contact being assigned an agent.
    * @param testGroup
    *           the tested agent group.
    * @param testAgent
    *           the tested agent, can be \texttt{null}.
    * @return the score given to the association between the contact and the
    *         agent.
    */
   protected double getScoreForAgentSelection (Contact ct,
         AgentGroup testGroup, Agent testAgent) {
      final int k = ct.getTypeId ();
      final int i = testGroup.getId ();
      final double w = weightsTG[k][i];
      switch (agentSelectionScore) {
      case WEIGHTONLY:
         return w;
      case NUMFREEAGENTS:
         return w * testGroup.getNumFreeAgents ();
      case LONGESTIDLETIME:
         final double s;
         if (testAgent == null)
            //s = testGroup.getNumFreeAgents ();
            throw new IllegalStateException
               ("Unavailable longest idle time of agents in group " + testGroup.getId() +
                "; use detailed agent groups");
         else
            s = testAgent.getIdleTime ();
         return s * w;
      }
      throw new AssertionError ();
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact ct) {
      final WaitingQueue queue = getWaitingQueue (ct.getTypeId ());
      if (queue == null)
         return null;
      final DequeueEvent ev = queue.add (ct);
      assert ev != null : "WaitingQueue.add should not return null";
      return ev;
   }

   /**
    * Determines the rank to be used for contact selection for contact type
    * \texttt{k}, when an agent in group \texttt{i} becomes free. By default,
    * this returns $\rGT(i, k)$, but subclasses may override this method for the
    * rank to depend on some state of the system.
    *
    * @param i
    *           the agent group index.
    * @param k
    *           the contact type index.
    * @return the rank associated with $(i, k)$.
    */
   protected double getRankForContactSelection (int i, int k) {
      return ranksGT[i][k];
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      double bestRank = Double.POSITIVE_INFINITY;
      int numCandidates = 0;
      final int K = getNumContactTypes ();
      for (int k = 0; k < K; k++) {
         qCandidates[k] = false;
         final double rank = getRankForContactSelection (group.getId (), k);
         if (Double.isInfinite (rank) || rank > bestRank)
            continue;
         final WaitingQueue queue = getWaitingQueue (k);
         if (queue == null || queue.isEmpty ())
            continue;
         if (rank < bestRank) {
            if (!Double.isInfinite (bestRank))
               for (int j = 0; j < k; j++)
                  qCandidates[j] = false;
            bestRank = rank;
            qCandidates[k] = true;
            numCandidates = 1;
         }
         else if (rank == bestRank) {
            qCandidates[k] = true;
            ++numCandidates;
         }
      }
      if (numCandidates == 0)
         return null;
      assert !Double.isInfinite (bestRank);

      selectWaitingQueue (group, agent, bestRank, qCandidates, numCandidates);
      if (bestQueue == null)
         return null;
      final DequeueEvent ev;
      if (bestQueuedContact == null)
         ev = bestQueue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
      else {
         final boolean rm = bestQueue.remove (bestQueuedContact,
               DEQUEUETYPE_BEGINSERVICE);
         assert rm;
         ev = bestQueuedContact;
      }
      bestQueue = null;
      bestQueuedContact = null;
      return ev;
   }

   /**
    * Contains the best waiting queue selected by
    * {@link #selectWaitingQueue(AgentGroup,Agent,double,boolean[],int)}.
    */
   protected WaitingQueue bestQueue;

   /**
    * Contains the best queued contact selected by
    * {@link #selectWaitingQueue(AgentGroup,Agent,double,boolean[],int)}, or
    * \texttt{null} if the first contact in the best queue is taken.
    */
   protected DequeueEvent bestQueuedContact;

   /**
    * Selects the queued contact for an agent \texttt{agent} in group
    * \texttt{group}, with waiting queue candidates \texttt{qCandidates}. The
    * waiting queues \texttt{k} for which \texttt{qCandidates[k]} is
    * \texttt{true} share the same minimal rank and contain at least one
    * contact. The method must store the selected waiting queue in the field
    * {@link #bestQueue} and possibly the contact to be removed in the
    * {@link #bestQueuedContact}. If {@link #bestQueuedContact} is
    * \texttt{null}, the first contact in the best queue will be used. If no
    * waiting queue can be selected, {@link #bestQueue} must be set to
    * \texttt{null}.
    *
    * The default implementation selects the waiting queue with the greatest
    * score as computed by
    * {@link #getScoreForContactSelection(AgentGroup,DequeueEvent)}, and the
    * best queued contact is always \texttt{null}. This method can be overridden
    * to use an alternate selection scheme, e.g., randomly selecting a queued
    * contact.
    *
    * @param group
    *           the agent group containing the free agent.
    * @param agent
    *           the free agent.
    * @param bestRank
    *           the best rank found when searching for candidates.
    * @param qCandidates1
    *           the waiting queue candidates contacts can be pulled from.
    */
   protected void selectWaitingQueue (AgentGroup group, Agent agent,
         double bestRank, boolean[] qCandidates1, int numCandidates) {
      bestQueue = null;
      bestQueuedContact = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      final int K = getNumContactTypes ();
      double sumScores = 0;
      for (int k = 0; k < K; k++) {
         if (!qCandidates1[k]) {
            qScores[k] = 0;
            continue;
         }
         final WaitingQueue queue = getWaitingQueue (k);
         if (numCandidates == 1) {
            bestQueue = queue;
            return;
         }
         final double score = getScoreForContactSelection (group, queue
               .getFirst ());
         if (score < 0 && Double.isInfinite (score)) {
            qScores[k] = 0;
            continue;
         }
         qScores[k] = score;
         sumScores += score;
         if (score > bestScore) {
            bestQueue = queue;
            bestScore = score;
         }
      }
      if (bestQueue != null && streamContactSelection != null
            && sumScores > bestScore) {
         for (int k = 0; k < K; k++)
            qScores[k] /= sumScores;
         double u = streamContactSelection.nextDouble ();
         bestQueue = null;
         for (int k = 0; k < K && bestQueue == null; k++)
            if (u < qScores[k])
               bestQueue = getWaitingQueue (k);
            else
               u -= qScores[k];
      }
   }

   /**
    * Returns the score for the association between the agent group
    * \texttt{group} and the queued contact represented by \texttt{ev}. If
    * contacts can be pulled from several waiting queues with the same minimal
    * rank, the router takes the contact with the greatest score. A negative
    * infinite score prevents a contact from being dequeued.
    *
    * By default, this returns a score depending on the return value of
    * {@link #getContactSelectionScore()}. This can return the weighted waiting
    * time (the default), the weighted number of queued agents, or the weight
    * only. See {@link ContactSelectionScore} for more information.
    *
    * @param group
    *           the agent group to which pulled contacts would be assigned.
    * @param ev
    *           the dequeue event.
    * @return the assigned score.
    */
   protected double getScoreForContactSelection (AgentGroup group,
         DequeueEvent ev) {
      final int i = group.getId ();
      final int k = ev.getContact ().getTypeId ();
      final double w = weightsGT[i][k];
      switch (contactSelectionScore) {
      case WEIGHTONLY:
         return w;
      case LONGESTWAITINGTIME:
         final double W = ev.simulator ().time () - ev.getEnqueueTime ();
         return w * W;
      case QUEUESIZE:
         final WaitingQueue queue = ev.getWaitingQueue ();
         int s = queue.size ();
         if (ev == queue.getFirst ())
            return w * s;
         else
            for (final DequeueEvent testEv : queue)
               if (ev == testEv)
                  return w * s;
               else
                  --s;
      }
      throw new AssertionError ();
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      final int gid = group.getId ();
      for (int k = 0; k < getNumContactTypes (); k++) {
         if (!canServe (gid, k))
            continue;
         final WaitingQueue queue = getWaitingQueue (k);
         if (queue == null || !mustClearWaitingQueue (k) || queue.size () == 0)
            continue;
         boolean clear = true;
         for (int i = 0; clear && i < getNumAgentGroups (); i++) {
            if (!canServe (i, k))
               continue;
            final AgentGroup tgroup = getAgentGroup (i);
            if (tgroup.getNumAgents () > 0)
               clear = false;
         }
         if (clear)
            queue.clear (DEQUEUETYPE_NOAGENT);
      }
   }

   @Override
   public String getDescription () {
      return "Agents' preference-based router";
   }

   @Override
   public String toLongString () {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Matrix of ranks specifying how contacts prefer agents\n");
      sb.append (RoutingTableUtils.formatRanksTG (ranksTG)).append ("\n");
      sb.append ("Matrix of ranks specifying how agents prefer contacts\n");
      sb.append (RoutingTableUtils.formatRanksGT (ranksGT)).append ("\n");
      sb.append ("Matrix of weights for agent selection\n");
      sb.append (RoutingTableUtils.formatWeightsTG (weightsTG)).append ("\n");
      sb.append ("Matrix of weights for contact selection\n");
      sb.append (RoutingTableUtils.formatWeightsGT (weightsGT));
      return sb.toString ();
   }
}
