package umontreal.iro.lecuyer.contactcenters.dialer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEventDetailed;
import umontreal.ssj.rng.RandomPermutation;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;
import cern.colt.list.IntArrayList;

/**
 * Represents a dialer policy that dynamically moves agents from inbound to
 * outbound groups to balance performance. This policy is inspired from a real
 * dialer called SER's SmartAgent Manager. This dialer manages a subset of the
 * $I$ agent groups of the contact center by separating them into two
 * categories: inbound agent groups and outbound agent groups. The inbound
 * groups are assumed to serve inbound contacts only while the outbound groups
 * process outbound contacts only. An inbound agent is an agent belonging to an
 * inbound group while any outbound agent belongs to an outbound group.
 * Consequently, an inbound agent is made outbound by removing it from its
 * original inbound group, and adding it into an outbound group. A similar
 * process is used to turn an outbound agent into an inbound one. This dialer
 * policy performs such transfers in order to balance performance.
 *
 * Note that this dialer policy does not impose outbound contacts to be routed
 * to outbound agents, and inbound contacts to be sent to inbound agents. The
 * routing policy must be configured separately to be consistent with the
 * inbound and outbound agent groups managed by the dialer.
 *
 * This policy required
 * two different aspects to be specified: how contacts are dialed, and how
 * agents are moved across groups. We will now describe these two aspects in
 * more details.
 *
 * \paragraph*{The dialing process.} Two algorithms are available for dialing:
 * one simple method using no routing information, and one more elaborate method
 * using the information. With the first and fastest method, the policy does not
 * control the distribution of the dialed calls, which can result in many
 * mismatches if agents can only serve a restricted subset of the calls. With
 * the second method, the number of dialed calls of each type depends on the
 * agents available to serve them. Both methods use a dialer list $L$ to obtain
 * calls to dial.
 *
 * The first method works as follows. When the dialer is triggered, i.e., when
 * it is requested to dial numbers, this policy computes the number of outbound
 * agents managed by the dialer given by \[ N = \sum_{i=0}^{I-1}
 * \Nf[i](t)\I\{i\mbox{ is an outbound agent group managed by the dialer}\}. \] The
 * number of calls to dial is then obtained using $n=\mathrm{round}(\kappa N) +
 * c - a$ where $\kappa\in\RR$, $c\in\NN$, and $\mathrm{round}(\cdot)$ rounds
 * its argument to the nearest integer.
 * The dialer schedules an \emph{action event} for each call waiting
 * a dial delay.
 * If the number of action events is taken
 * into account (the default), the constant $a$ is the number of action events
 * currently scheduled by the dialer. Otherwise, $a=0$. An action event occurs
 * when a call made by the dialer reaches a person or fails. The $n$ calls to be
 * dialed are extracted from the dialer list $L$.
 *
 * The dialing method using routing information works as follows. For each
 * managed agent group, the dialer determines a number of calls to dial using
 * the number of free agents. It then sums up the number of calls $m_k$ for each
 * type, and constructs a dialer list $L_2$ containing at most $m_k$ calls of
 * type $k$. The calls are extracted from the dialer list $L$. The contents of
 * the dialer list might be affected by limits
 * imposed on the number of calls of each type.
 *
 * The values of $m_k$ are computed as follows. First, $m_k=0$ for each value of
 * $k$. Then, for each managed outbound agent group $i$, the dialer obtains
 * $n_i=\mathrm{round}(\kappa \Nf[i](t)) + c$. Let $K_i$ be the number of
 * different types of calls agents in group $i$ can serve, and $b_{k, i}=1$ if
 * and only if agents in group $i$ can serve calls of type $k$. If $K_i>0$, for
 * each value of $k$, the value $\mathrm{round}(b_{k, i}n_i/K_i)$ is added to
 * $m_k$. Usually, $K_i=1$ with this model, i.e., agents in each group $i$ can
 * serve a single outbound call type.
 *
 * After each agent group is processed, if the dialer takes the number of action
 * events into account, the number of action events concerning calls of type $k$
 * is subtracted to each value of $m_k$, for each call type $k$.
 *
 * \paragraph{Management of agents.} This dialer regroups agent groups into
 * virtual groups containing inbound and outbound agent groups. Let $J$ be the
 * total number of virtual agent groups, and $V_j(t)=I_j(t)+O_j(t)$ the number
 * of agents in virtual group $j$ at simulation time $t$, where $I_j(t)$ is the
 * total number of inbound agents in virtual group~$j$, and $O_j(t)$ is the
 * total number of outbound agents in virtual group~$j$ at time $t$. This dialer
 * policy never changes the virtual group of an agent; it only transfers agents
 * to groups within the same virtual group.
 *
 * Note that an agent group can only be in a single virtual group, for a single
 * dialer.
 *
 * Any external change to an agent group managed by this dialer policy is
 * handled the same way as if the dialer never transferred agents from groups to
 * groups. This requires the dialer to keep track of the number of agents
 * transferred into or out of each managed group. The changes are performed as
 * follows. If the number of agents is increased, agents are added to the
 * concerned group. However, if the number of agents is reduced, the dialer
 * first removes outbound agents, then inbound agents if the affected virtual
 * group does not contain any more outbound agents. The order in which the agent
 * groups are selected to remove agents from is random to avoid an agent group
 * having priority over other groups. The only constraint on the order is the
 * priority of outbound agents over inbound agents. When the dialer is stopped,
 * every outbound agent becomes inbound, but busy outbound agents terminate
 * their on-going services before they become inbound.
 *
 * Two flags are available for this dialer policy: inbound-to-outbound flag, and
 * outbound-to-inbound flag. These flags trigger procedures that can be
 * considered as background processes, although they are implemented with
 * simulation events. When the inbound-to-outbound flag is turned ON, the policy
 * starts the following procedure for each virtual agent group $j$, each time
 * the dialer is required to take a decision. \begin{enumerate} \item If the
 * procedure is already running for virtual group $j$, stop. \item Let $\tau$ be
 * the delay between the last time an inbound agent in virtual group $j$ became
 * outbound, and the current simulation time. If $\tau<\dOO[j]$, wait for
 * $\dOO[j] - \tau$. \item Generate a random permutation of the inbound agent
 * groups in the virtual group $j$. \item For each inbound agent group $i$ in
 * the virtual group $j$, do the following. Agent groups are processed in the
 * order given by the random permutation of the previous step. While
 * $\Ni[i](t)>0$, \begin{enumerate} \item Select an agent $A$ in group $i$ with
 * the following characteristics: \begin{itemize} \item The agent is in group
 * $i$ (idle or busy) for a minimal time $\dIO[j]$, \item The idle time of the
 * agent is greater than or equal to $t_j$, \item The number of idle inbound
 * agents in virtual group $j$ is greater than or equal to $m_j$, \item The
 * number of outbound idle agents in virtual group $j$ is smaller than $M_j$.
 * \end{itemize} \item If no agent was selected at previous step, skip to next agent group.
 *  \item
 * Remove agent $A$ from group $i$, select outbound agent group $o$ with
 * probability $p_{j, o}$, and add the agent $A$ to group $o$. \item Wait for a
 * delay $\dOO[j]$. \end{enumerate} \end{enumerate} When the flag is turned OFF,
 * every process moving inbound agents to outbound groups is stopped.
 *
 * On the other hand, when the outbound-to-inbound flag is turned ON, the policy
 * starts the following procedure for each virtual agent group $j$, each time
 * the dialer is required to take a decision. \begin{enumerate} \item If the
 * procedure is already running for group $j$, stop. \item Let $\tau$ be delay
 * between the last time an outbound agent in virtual group $j$ became inbound,
 * and the current simulation time. If $\tau<\dII[j]$, wait for $\dII[j] -
 * \tau$. \item Generate a random permutation of outbound agent groups. \item
 * For each outbound agent group $o$ in virtual group $j$, do the following.
 * Agent groups are processed in the order given by the random permutation
 * generated at the previous step. While $\Ni[o](t)>0$, \begin{enumerate} \item
 * Select an agent $A$ in group $o$ with the following characteristic:
 * \begin{itemize} \item The agent is in group $o$ (idle or busy) for a minimal
 * time $\dOI[j]$. \end{itemize} \item If no agent was selected at previous
 * step, skip to next agent group. \item Remove agent $A$ from group $o$, select inbound agent group
 * with probability $p_{j, i}$, and add agent $A$ to group $i$. \item Wait for a
 * delay $\dII[j]$. \end{enumerate} \end{enumerate} When the flag is turned OFF,
 * every process moving outbound agents to inbound groups is stopped.
 */
public class AgentsMoveDialerPolicy implements DialerPolicy {
   private DialerList list;
   private final ContactListenerDialerList list2 = new ContactListenerDialerList ();
   private Router router;
   private boolean makeInboundMode;
   private boolean makeOutboundMode;
   private AgentGroupInfo[] groupInfo;
   private boolean started = false;
   private double kappa = 1;
   private int c = 0;

   /**
    * Constructs a new dialer policy using the dialer list \texttt{list}, and
    * agent group information \texttt{groupInfo}. Each {@link AgentGroupInfo}
    * object results in a virtual group of agents for the dialer.
    *
    * @param list
    *           the dialer list.
    * @param groupInfo
    *           the agent group information.
    */
   public AgentsMoveDialerPolicy (DialerList list, AgentGroupInfo[] groupInfo,
         double kappa, int c) {
      if (list == null || groupInfo == null)
         throw new NullPointerException ();
      this.list = list;
      this.groupInfo = groupInfo.clone ();
      this.kappa = kappa;
      this.c = c;
   }

   /**
    * Returns an array containing the references to the virtual agent groups
    * managed by this dialer policy.
    *
    * @return the virtual agent groups for this policy.
    */
   public AgentGroupInfo[] getAgentGroupInfo () {
      return groupInfo.clone ();
   }

   public void dialerStarted (Dialer dialer) {
      started = true;
   }

   /**
    * Calls {@link #init(Dialer)}.
    */
   public void dialerStopped (Dialer dialer) {
      stopInboundToOutbound ();
      stopOutboundToInbound ();
      for (final AgentGroupInfo info : groupInfo)
         info.makeAllInbound ();
      started = false;
   }

   /**
    * Returns the dialer list associated with this policy.
    */
   public DialerList getDialerList (Dialer dialer) {
      return router == null ? list : list2;
   }

   /**
    * Sets the dialer list of this policy to \texttt{list}.
    *
    * @param list
    *           the new dialer list.
    */
   public void setDialerList (DialerList list) {
      if (list == null)
         throw new NullPointerException ();
      this.list = list;
   }

   /**
    * This method returns the number of free agents in all outbound groups
    * connected to this dialer.
    */
   public int getNumDials (Dialer dialer) {
      if (!started)
         return 0;
      for (final AgentGroupInfo info : groupInfo)
         if (info.changeLock > 0)
            return 0;
      if (makeInboundMode)
         for (final AgentGroupInfo info : groupInfo)
            info.startOutboundToInbound ();
      if (makeOutboundMode)
         for (final AgentGroupInfo info : groupInfo)
            info.startInboundToOutbound ();
      for (final AgentGroupInfo info : groupInfo)
         info.makeInboundIfNoOut ();
      if (router == null) {
         int n = 0;
         for (final AgentGroupInfo info : groupInfo)
            for (final AgentGroup grp : info.getOutboundGroups ())
               n += grp.getNumFreeAgents ();
         if (n == 0)
            return 0;
         int nd = (int) Math.round (kappa * n) + c;
         if (dialer.isUsingNumActionsEvents ())
            nd -= dialer.getNumActionEvents ();
         return nd < 0 ? 0 : nd;
      }
      else {
         final int[] numCalls = new int[router.getNumContactTypes ()];
         final int[] numCallsLimit = new int[numCalls.length];
         for (int k = 0; k < numCallsLimit.length; k++)
            numCallsLimit[k] = list.size (new int[] { k });
         for (final AgentGroupInfo info : groupInfo) {
            final AgentGroup[] outboundGroups = info.getOutboundGroups ();
            for (int idxOut = 0; idxOut < outboundGroups.length; idxOut++) {
               final AgentGroup grp = outboundGroups[idxOut];
               final int[] types = getTypes (grp.getId ());
               int numTypes = 0;
               int numTypes2 = 0;
               for (final int element : types) {
                  if (numCallsLimit[element] > 0)
                     ++numTypes;
                  if (dialer.getNumActionEvents (element) > 0)
                     ++numTypes2;
               }
               if (numTypes + numTypes2 == 0) {
                  info.outboundGroupProbs[idxOut] = 0;
                  continue;
               }
               else
                  info.outboundGroupProbs[idxOut] = info.initialOutboundGroupProbs[idxOut];

               final int nf = grp.getNumFreeAgents ();
               if (nf == 0)
                  continue;
               final int ng = (int) Math.round (kappa * nf) + c;

               for (final int element : types)
                  if (numCallsLimit[element] > 0)
                     numCalls[element] += ng / numTypes;
            }
         }

         if (dialer.isUsingNumActionsEvents ())
            for (int k = 0; k < numCalls.length; k++) {
               numCalls[k] -= dialer.getNumActionEvents (k);
               if (numCalls[k] < 0)
                  numCalls[k] = 0;
            }

         list2.clear ();
         boolean done = false;
         while (!done) {
            done = true;
            for (int k = 0; k < numCalls.length; k++) {
               if (numCalls[k] == 0)
                  continue;
               try {
                  final Contact ct = list.removeFirst (new int[] { k });
                  --numCalls[k];
                  list2.newContact (ct);
                  done = false;
               }
               catch (final NoSuchElementException nse) {
                  numCalls[k] = 0;
               }
            }
         }
         return list2.size (null);
      }
   }

   private int[] getTypes (int i) {
      if (router == null)
         return null;
      final IntArrayList typeList = new IntArrayList ();
      for (int k = 0; k < router.getNumContactTypes (); k++)
         if (router.canServe (i, k))
            typeList.add (k);
      typeList.trimToSize ();
      return typeList.elements ();
   }

   public void setRouter (Router router) {
      this.router = router;
      if (router != null) {
         final AgentGroupListener l = router.getAgentGroupListener ();
         for (final AgentGroupInfo info : groupInfo) {
            for (final AgentGroup grp : info.getInboundGroups ()) {
               if (!grp.getAgentGroupListeners ().contains (l))
                  continue;
               // Put the router listener at the end of the list
               grp.removeAgentGroupListener (l);
               grp.addAgentGroupListener (l);
            }
            for (final AgentGroup grp : info.getOutboundGroups ()) {
               if (!grp.getAgentGroupListeners ().contains (l))
                  continue;
               // Put the router listener at the end of the list
               grp.removeAgentGroupListener (l);
               grp.addAgentGroupListener (l);
            }
         }
      }
   }

   /**
    * Makes every agent inbound when the dialer stops.
    */
   public void init (Dialer dialer) {
      list.clear ();
      list2.clear ();
      stopInboundToOutbound ();
      stopOutboundToInbound ();
      for (final AgentGroupInfo info : groupInfo)
         info.init ();
   }

   /**
    * Determines if the inbound-to-outbound flag is turned ON.
    *
    * @return the status of the inbound-to-outbound flag.
    */
   public boolean isInboundToOutboundStarted () {
      return makeOutboundMode;
   }

   /**
    * Turns the inbound-to-outbound flag on.
    */
   public void startInboundToOutbound () {
      if (!started || makeOutboundMode)
         return;
      makeOutboundMode = true;
      for (final AgentGroupInfo info : groupInfo)
         info.startInboundToOutbound ();
   }

   /**
    * Turns the inbound-to-outbound flag off.
    */
   public void stopInboundToOutbound () {
      if (!started || !makeOutboundMode)
         return;
      makeOutboundMode = false;
      for (final AgentGroupInfo info : groupInfo)
         info.stopInboundToOutbound ();
   }

   /**
    * Determines if the outbound-to-inbound flag is turned ON.
    *
    * @return the status of the outbound-to-inbound flag.
    */
   public boolean isOutboundToInboundStarted () {
      return makeInboundMode;
   }

   /**
    * Turns the outbound-to-inbound flag on.
    */
   public void startOutboundToInbound () {
      if (!started || makeInboundMode)
         return;
      makeInboundMode = true;
      for (final AgentGroupInfo info : groupInfo)
         info.startOutboundToInbound ();
   }

   /**
    * Turns the outbound-to-inbound flag off.
    */
   public void stopOutboundToInbound () {
      if (!started || !makeInboundMode)
         return;
      makeInboundMode = false;
      for (final AgentGroupInfo info : groupInfo)
         info.stopOutboundToInbound ();
   }

   /**
    * Event used to transfer outbound agents into inbound groups.
    */
   private static class MakeInboundEvent extends Event {
      private AgentGroupInfo info;

      public MakeInboundEvent (AgentGroupInfo info) {
         super (info.simulator ());
         this.info = info;
      }

      @Override
      public void actions () {
         if (info.requiredEmptyGroupsOutIn != null)
            for (final AgentGroup grp : info.requiredEmptyGroupsOutIn)
               if (grp.getNumAgents () > 0)
                  return;
         int numIdleOutboundAgents = 0;
         for (final AgentGroup grp : info.getOutboundGroups ())
            numIdleOutboundAgents += grp.getNumIdleAgents ();
         if (numIdleOutboundAgents == 0)
            return;

         if (info.permOutbound.length > 1)
            RandomPermutation.shuffle (info.permOutbound, info.stream);
         final AgentGroup[] outboundGroups = info.getOutboundGroups ();
         for (final int idxOut : info.permOutbound) {
            final double simTime = simulator ().time ();
            for (final Agent agent : ((DetailedAgentGroup) outboundGroups[idxOut])
                  .getIdleAgents ()) {
               final double loginTime = simTime - agent.getLastLoginTime ();
               if (loginTime < info.getDelayOutIn ())
                  continue;
               if (info.transferToInbound (idxOut, agent)) {
                  schedule (info.getDelayInIn ());
                  return;
               }
            }
         }
      }
   }

   /**
    * Event used to transfer inbound agents into outbound groups.
    */
   private static class MakeOutboundEvent extends Event {
      private AgentGroupInfo info;

      public MakeOutboundEvent (AgentGroupInfo info) {
         super (info.simulator ());
         this.info = info;
      }

      @Override
      public void actions () {
         if (info.requiredEmptyGroupsInOut != null)
            for (final AgentGroup grp : info.requiredEmptyGroupsInOut)
               if (grp.getNumAgents () > 0)
                  return;
         int numIdleInboundAgents = 0;
         for (final AgentGroup grp : info.getInboundGroups ())
            numIdleInboundAgents += grp.getNumIdleAgents ();
         if (numIdleInboundAgents == 0)
            return;
         if (numIdleInboundAgents < info.getMinimumIdleInboundAgents ())
            return;
         int numIdleOutboundAgents = 0;
         for (final AgentGroup grp : info.getOutboundGroups ())
            numIdleOutboundAgents += grp.getNumIdleAgents ();
         if (numIdleOutboundAgents > info.getMaximumIdleOutboundAgents ())
            return;

         if (info.permInbound.length > 1)
            RandomPermutation.shuffle (info.permInbound, info.stream);
         final AgentGroup[] inboundGroups = info.getInboundGroups ();
         for (final int idxIn : info.permInbound) {
            final double simTime = simulator ().time ();
            for (final Agent agent : ((DetailedAgentGroup) inboundGroups[idxIn])
                  .getIdleAgents ()) {
               final double loginTime = simTime - agent.getLastLoginTime ();
               if (loginTime < info.getDelayInOut ())
                  continue;
               final double idleTime = simTime - agent.getIdleSimTime ();
               assert idleTime >= 0;
               if (idleTime < info.getMinimumIdleTime ())
                  continue;
               if (info.transferToOutbound (idxIn, agent)) {
                  schedule (info.getDelayOutOut ());
                  return;
               }
            }
         }
      }
   }

   /**
    * Represents a virtual agent group $j$ for the
    * {@link AgentsMoveDialerPolicy}. This class encapsulates information about
    * inbound and outbound groups in the virtual group as well as thresholds,
    * probabilities, and delays. It also implements methods to transfer agents
    * from groups to groups.
    */
   public static class AgentGroupInfo {
      private AgentGroup[] inboundGroups;
      private double[] inboundGroupProbs;
      private AgentGroup[] outboundGroups;
      private double[] initialOutboundGroupProbs;
      private double[] outboundGroupProbs;
      private RandomStream stream;
      private AgentGroup[] requiredEmptyGroupsInOut;
      private AgentGroup[] requiredEmptyGroupsOutIn;

      // Number of agents transferred by this dialer policy
      private int[] numTransferredInbound;
      private int[] numTransferredOutbound;
      // Number of agents tracked by this dialer policy,
      // needed by agentChange to know the number of agents
      // before the change.
      private int[] numInbound;
      private int[] numOutbound;
      // Temporary arrays to store permutations
      private int[] permInbound;
      private int[] permOutbound;

      private double delayInOut, delayOutIn;
      private double delayInIn, delayOutOut;
      private double minimumIdleTime;
      private int minimumIdleInboundAgents;
      private int maximumIdleOutboundAgents;

      private final AgentGroupChecker gChecker = new AgentGroupChecker ();
      private Event inboundEvent;
      private Event outboundEvent;
      private double lastNewInboundTime;
      private double lastNewOutboundTime;
      private final Set<Agent> markedAgents = new HashSet<Agent> ();
      // Allows the agent-group listener to distinguish internal
      // and external changes of the number of agents in the
      // managed groups.
      private int changeLock = 0;

      private int numInOutMoves = 0;
      private int numOutInMoves = 0;

      /**
       * Constructs a new virtual agent group containing all inbound agent
       * groups int \texttt{inboundGroups}, and all outbound agent groups in
       * \texttt{outboundGroups}. The arrays \texttt{inboundGroupProbs} and
       * \texttt{outboundGroupProbs} contain probabilities $p_{j, i}$ of
       * selection of agent groups as targets for transfers. The random stream
       * \texttt{stream} is used to generated random numbers for permutations,
       * and for selecting target agent groups during transfers.
       *
       * @param inboundGroups
       *           the inbound agent group.
       * @param inboundGroupProbs
       *           the probabilities of selection for each inbound agent group
       *           when performing transfers.
       * @param outboundGroups
       *           the outbound agent group.
       * @param outboundGroupProbs
       *           the probabilities of selection for each outbound agent group
       *           when performing transfers.
       */
      public AgentGroupInfo (AgentGroup[] inboundGroups,
            double[] inboundGroupProbs, AgentGroup[] outboundGroups,
            double[] outboundGroupProbs, RandomStream stream) {
         if (inboundGroups.length != inboundGroupProbs.length)
            throw new IllegalArgumentException (
                  "The arrays inboundGroups and inboundGroupProbs must have the same length");
         if (outboundGroups.length != outboundGroupProbs.length)
            throw new IllegalArgumentException (
                  "The arrays outboundGroups and outboundGroupProbs must have the same length");
         this.inboundGroups = inboundGroups.clone ();
         this.inboundGroupProbs = inboundGroupProbs.clone ();
         this.outboundGroups = outboundGroups.clone ();
         this.outboundGroupProbs = outboundGroupProbs.clone ();
         initialOutboundGroupProbs = outboundGroupProbs.clone ();
         this.stream = stream;

         for (final AgentGroup grp : inboundGroups)
            grp.addAgentGroupListener (gChecker);
         for (final AgentGroup grp : outboundGroups)
            grp.addAgentGroupListener (gChecker);
         numTransferredInbound = new int[inboundGroups.length];
         numTransferredOutbound = new int[outboundGroups.length];
         numInbound = new int[inboundGroups.length];
         numOutbound = new int[outboundGroups.length];
         for (int j = 0; j < numInbound.length; j++)
            numInbound[j] = inboundGroups[j].getNumAgents ();
         for (int j = 0; j < numOutbound.length; j++)
            numOutbound[j] = outboundGroups[j].getNumAgents ();

         permInbound = new int[inboundGroups.length];
         for (int j = 0; j < permInbound.length; j++)
            permInbound[j] = j;
         permOutbound = new int[outboundGroups.length];
         for (int j = 0; j < permOutbound.length; j++)
            permOutbound[j] = j;

         inboundEvent = new MakeInboundEvent (this);
         outboundEvent = new MakeOutboundEvent (this);
      }

      public void setRequiredEmptyGroupsInOut (AgentGroup[] groups) {
         requiredEmptyGroupsInOut = groups == null ? null : groups.clone ();
      }

      public void setRequiredEmptyGroupsOutIn (AgentGroup[] groups) {
         requiredEmptyGroupsOutIn = groups == null ? null : groups.clone ();
      }

      public Simulator simulator () {
         for (final AgentGroup inboundGroup : inboundGroups)
            if (inboundGroup instanceof DetailedAgentGroup)
               return ((DetailedAgentGroup) inboundGroup).simulator ();
         for (final AgentGroup outboundGroup : outboundGroups)
            if (outboundGroup instanceof DetailedAgentGroup)
               return ((DetailedAgentGroup) outboundGroup).simulator ();
         return Simulator.getDefaultSimulator ();
      }

      public int getNumInOutMoves () {
         return numInOutMoves;
      }

      public int getNumOutInMoves () {
         return numOutInMoves;
      }

      /**
       * Returns the value of $\dII[j]$, which defaults to 0.
       */
      public double getDelayInIn () {
         return delayInIn;
      }

      /**
       * Sets the value of $\dII[j]$ to \texttt{delayInIn}.
       */
      public void setDelayInIn (double delayInIn) {
         this.delayInIn = delayInIn;
      }

      /**
       * Returns the value of $\dIO[j]$, which defaults to 0.
       */
      public double getDelayInOut () {
         return delayInOut;
      }

      /**
       * Sets the value of $\dIO[j]$ to \texttt{delayInOut}.
       */
      public void setDelayInOut (double delayInOut) {
         this.delayInOut = delayInOut;
      }

      /**
       * Returns the value of $\dOI[j]$, which defaults to 0.
       */
      public double getDelayOutIn () {
         return delayOutIn;
      }

      /**
       * Sets the value of $\dOI[j]$ to \texttt{delayOutIn}.
       */
      public void setDelayOutIn (double delayOutIn) {
         this.delayOutIn = delayOutIn;
      }

      /**
       * Returns the value of $\dOO[j]$, which defaults to 0.
       */
      public double getDelayOutOut () {
         return delayOutOut;
      }

      /**
       * Sets the value of $\dOO[j]$ to \texttt{delayOutOut}.
       */
      public void setDelayOutOut (double delayOutOut) {
         this.delayOutOut = delayOutOut;
      }

      /**
       * Returns the inbound agent group associated with this information
       * object.
       */
      public AgentGroup[] getInboundGroups () {
         return inboundGroups;
      }

      /**
       * Returns the outbound agent group associated with this information
       * object.
       */
      public AgentGroup[] getOutboundGroups () {
         return outboundGroups;
      }

      /**
       * Returns the value of $M_j$, which defaults to 0.
       */
      public int getMaximumIdleOutboundAgents () {
         return maximumIdleOutboundAgents;
      }

      /**
       * Sets the value of $M_j$ to \texttt{maximumIdleAgents}.
       */
      public void setMaximumIdleOutboundAgents (int maximumIdleAgents) {
         maximumIdleOutboundAgents = maximumIdleAgents;
      }

      /**
       * Returns the value of $m_j$, which defaults to 0.
       */
      public int getMinimumIdleInboundAgents () {
         return minimumIdleInboundAgents;
      }

      /**
       * Sets the value of $m_j$ to \texttt{minimumIdleAgents}.
       */
      public void setMinimumIdleInboundAgents (int minimumIdleAgents) {
         minimumIdleInboundAgents = minimumIdleAgents;
      }

      /**
       * Returns the value of $t_j$, which defaults to 0.
       */
      public double getMinimumIdleTime () {
         return minimumIdleTime;
      }

      /**
       * Sets the value of $t_j$ to \texttt{minimumIdleTime}.
       */
      public void setMinimumIdleTime (double minimumIdleTime) {
         this.minimumIdleTime = minimumIdleTime;
      }

      /**
       * Returns the probabilities $p_{j, i}$ of selection for each inbound
       * agent group. Element \texttt{k} of the returned array corresponds to
       * the probability associated with agent group \texttt{k} in the array
       * returned by {@link #getInboundGroups()}.
       *
       * @return the probability of selection for inbound agent groups.
       */
      public double[] getInboundGroupProbs () {
         return inboundGroupProbs;
      }

      /**
       * Returns the probabilities $p_{j, i}$ of selection for each outbound
       * agent group. Element \texttt{k} of the returned array corresponds to
       * the probability associated with agent group \texttt{k} in the array
       * returned by {@link #getOutboundGroups()}.
       *
       * @return the probability of selection for outbound agent groups.
       */
      public double[] getOutboundGroupProbs () {
         return outboundGroupProbs;
      }

      private int getRandomInboundGroup () {
         if (inboundGroups.length == 1 && inboundGroupProbs[0] > 0)
            return 0;
         double sum = 0;
         for (int idx = 0; idx < inboundGroups.length; idx++)
            sum += inboundGroupProbs[idx];
         if (sum == 0)
            return -1;
         double u = stream.nextDouble ();
         int idx = 0;
         while (idx < inboundGroups.length && u > inboundGroupProbs[idx] / sum) {
            u -= inboundGroupProbs[idx] / sum;
            ++idx;
         }
         if (idx >= inboundGroups.length)
            return -1;
         return idx;
      }

      private int getRandomOutboundGroup () {
         if (outboundGroups.length == 1 && outboundGroupProbs[0] > 0)
            return 0;
         double sum = 0;
         for (int idx = 0; idx < outboundGroups.length; idx++)
            sum += outboundGroupProbs[idx];
         if (sum == 0)
            return -1;

         double u = stream.nextDouble ();
         int idx = 0;
         while (idx < outboundGroups.length
               && u > outboundGroupProbs[idx] / sum) {
            u -= outboundGroupProbs[idx] / sum;
            ++idx;
         }
         if (idx >= outboundGroups.length)
            return -1;
         return idx;
      }

      /**
       * Transfers \texttt{n} agents from the outbound groups of this object to
       * its inbound group. For each transfer, the order of outbound agent
       * groups is chosen randomly to avoid an outbound group having priority
       * over the others.
       *
       * @param n
       *           the number of agents to transfer.
       */
      public void transferToInbound (int n) {
         int numOutboundAgents = 0;
         for (final AgentGroup grp : outboundGroups)
            numOutboundAgents += grp.getNumIdleAgents ();
         if (numOutboundAgents < n)
            throw new IllegalArgumentException ("Too large n=" + n);
         for (int k = 0; k < n; k++) {
            if (permOutbound.length > 1)
               RandomPermutation.shuffle (permOutbound, stream);
            boolean done = false;
            for (int idx = 0; idx < permOutbound.length && !done; idx++) {
               final int idxOut = permOutbound[idx];
               if (outboundGroups[idxOut].getNumAgents () == 0)
                  continue;
               final DetailedAgentGroup dgroup = (DetailedAgentGroup) outboundGroups[idxOut];
               if (dgroup.getNumIdleAgents () > 0)
                  if (transferToInbound (idxOut, dgroup.getIdleAgents ()
                        .get (0)))
                     done = true;
            }
            if (!done)
               return;
         }
      }

      /**
       * Transfers \texttt{n} agents from the inbound groups of this object to
       * its outbound group. For each transfer, the order of inbound agent
       * groups is chosen randomly to avoid an inbound group having priority
       * over the others.
       *
       * @param n
       *           the number of agents to transfer.
       */
      public void transferToOutbound (int n) {
         int numInboundAgents = 0;
         for (final AgentGroup grp : inboundGroups)
            numInboundAgents += grp.getNumIdleAgents ();
         if (numInboundAgents < n)
            throw new IllegalArgumentException ("Too large n=" + n);
         for (int k = 0; k < n; k++) {
            if (permInbound.length > 1)
               RandomPermutation.shuffle (permInbound, stream);
            boolean done = false;
            for (int idx = 0; idx < permInbound.length && !done; idx++) {
               final int idxIn = permInbound[idx];
               if (inboundGroups[idxIn].getNumIdleAgents () == 0)
                  continue;
               final DetailedAgentGroup dgroup = (DetailedAgentGroup) inboundGroups[idxIn];
               if (dgroup.getNumIdleAgents () > 0)
                  if (transferToOutbound (idxIn, dgroup.getIdleAgents ()
                        .get (0)))
                     done = true;
            }
            if (!done)
               return;
         }
      }

      /**
       * Transfers the agent \texttt{agent} to a randomly-chosen inbound agent
       * group.
       *
       * @param agent
       *           the agent to transfer.
       */
      public boolean transferToInbound (int idxOut, Agent agent) {
         ++changeLock;
         try {
            final DetailedAgentGroup grp = agent.getAgentGroup ();
            assert outboundGroups[idxOut] == grp;
            final int idxIn = getRandomInboundGroup ();
            if (idxIn == -1)
               return false;
            if (grp != null) {
               grp.removeAgent (agent);
               --numTransferredOutbound[idxOut];
               --numOutbound[idxOut];
               assert numOutbound[idxOut] >= 0 : "numOutbound = "
                     + numOutbound[idxOut];
            }
            final AgentGroup inboundGroup = inboundGroups[idxIn];
            lastNewInboundTime = inboundEvent.simulator ().time ();
            ++numOutInMoves;
            ++numTransferredInbound[idxIn];
            ++numInbound[idxIn];
            if (inboundGroup instanceof DetailedAgentGroup)
               ((DetailedAgentGroup) inboundGroup).addAgent (agent);
            else
               inboundGroup.setNumAgents (inboundGroup.getNumAgents () + 1);
         }
         finally {
            --changeLock;
         }
         return true;
      }

      /**
       * Transfers the agent \texttt{agent} to a randomly-chosen outbound agent
       * group.
       *
       * @param agent
       *           the agent to transfer.
       */
      public boolean transferToOutbound (int idxIn, Agent agent) {
         ++changeLock;
         try {
            final DetailedAgentGroup grp = agent.getAgentGroup ();
            assert inboundGroups[idxIn] == grp;
            final int idxOut = getRandomOutboundGroup ();
            if (idxOut == -1)
               return false;
            if (grp != null) {
               grp.removeAgent (agent);
               --numTransferredInbound[idxIn];
               --numInbound[idxIn];
               assert numInbound[idxIn] >= 0 : "numInbound = "
                     + numInbound[idxIn];
            }
            final AgentGroup outboundGroup = outboundGroups[idxOut];
            lastNewOutboundTime = outboundEvent.simulator ().time ();
            ++numInOutMoves;
            ++numTransferredOutbound[idxOut];
            ++numOutbound[idxOut];
            if (outboundGroup instanceof DetailedAgentGroup)
               ((DetailedAgentGroup) outboundGroup).addAgent (agent);
            else
               outboundGroup.setNumAgents (outboundGroup.getNumAgents () + 1);
         }
         finally {
            --changeLock;
         }
         return true;
      }

      /**
       * Starts the process moving inbound agents to outbound for the agent
       * groups associated with this object. This method does nothing if the
       * moving process is already started.
       */
      public void startInboundToOutbound () {
         if (outboundEvent.time () > 0)
            return;
         final double time = outboundEvent.simulator ().time ()
               - lastNewOutboundTime;
         if (time < delayOutOut)
            outboundEvent.schedule (delayOutOut - time);
         else
            // outboundEvent.scheduleNext ();
            outboundEvent.actions ();
      }

      /**
       * Stops the process moving inbound agents to the outbound group.
       */
      public void stopInboundToOutbound () {
         outboundEvent.cancel ();
      }

      public void makeInboundIfNoOut () {
         for (int idxOut = 0; idxOut < outboundGroups.length; idxOut++)
            if (outboundGroupProbs[idxOut] == 0)
               while (outboundGroups[idxOut].getNumIdleAgents () > 0)
                  transferToInbound (idxOut,
                        ((DetailedAgentGroup) outboundGroups[idxOut])
                              .getIdleAgents ().get (0));
      }

      /**
       * Similar to {@link #startInboundToOutbound()}, for the
       * outbound-to-inbound process.
       */
      public void startOutboundToInbound () {
         if (inboundEvent.time () > 0)
            return;
         final double time = inboundEvent.simulator ().time ()
               - lastNewInboundTime;
         if (time < delayInIn)
            inboundEvent.schedule (delayInIn - time);
         else
            // inboundEvent.scheduleNext ();
            inboundEvent.actions ();
      }

      /**
       * Similar to {@link #stopInboundToOutbound()}, for the
       * outbound-to-inbound process.
       */
      public void stopOutboundToInbound () {
         inboundEvent.cancel ();
      }

      /**
       * Moves all outbound agents to the inbound group. Any busy outbound agent
       * is marked to be moved after its on-going service if finished.
       */
      public void makeAllInbound () {
         int idxOut = 0;
         for (final AgentGroup outboundGroup : outboundGroups) {
            if (outboundGroup.getNumAgents () == 0) {
               ++idxOut;
               continue;
            }
            final DetailedAgentGroup dgroup = (DetailedAgentGroup) outboundGroup;
            while (dgroup.getNumIdleAgents () > 0)
               transferToInbound (idxOut, dgroup.getIdleAgents ().get (0));
            // The busy agents are transferred as soon as they
            // have finished their services.
            final Agent[] busyAgents = dgroup.getBusyAgents ().toArray (new Agent[0]);
            for (final Agent agent : busyAgents)
               if (!agent.isGhost ()) {
                  assert !markedAgents.contains (agent) : "Agent already marked";
                  ++changeLock;
                  try {
                     // Make the agent unavailable to prevent the
                     // router assigning it a new call
                     agent.setAvailable (false);
                  }
                  finally {
                     --changeLock;
                  }
                  markedAgents.add (agent);
               }
            ++idxOut;
         }
      }

      /**
       * Initializes both agent groups, and resets the fields storing the last
       * time moves happened.
       */
      public void init () {
         System.arraycopy (initialOutboundGroupProbs, 0, outboundGroupProbs, 0,
               outboundGroupProbs.length);
         ++changeLock;
         try {
            for (final AgentGroup grp : inboundGroups)
               grp.init ();
            for (final AgentGroup grp : outboundGroups)
               grp.init ();
         }
         finally {
            --changeLock;
         }
         markedAgents.clear ();
         makeAllInbound ();
         lastNewInboundTime = 0;
         lastNewOutboundTime = 0;
         // Resets the number of agents in all groups as
         // if no transfer occurred
         ++changeLock;
         try {
            for (int idxIn = 0; idxIn < inboundGroups.length; idxIn++) {
               inboundGroups[idxIn].setNumAgents (inboundGroups[idxIn]
                     .getNumAgents ()
                     - numTransferredInbound[idxIn]);
               numInbound[idxIn] = inboundGroups[idxIn].getNumAgents ();
            }
            for (int idxOut = 0; idxOut < outboundGroups.length; idxOut++) {
               outboundGroups[idxOut].setNumAgents (outboundGroups[idxOut]
                     .getNumAgents ()
                     - numTransferredOutbound[idxOut]);
               numOutbound[idxOut] = outboundGroups[idxOut].getNumAgents ();
            }
            Arrays.fill (numTransferredInbound, 0);
            Arrays.fill (numTransferredOutbound, 0);
         }
         finally {
            --changeLock;
         }
         numInOutMoves = 0;
         numOutInMoves = 0;
      }

      /**
       * Tries to remove the given number of inbound agents, and returns the
       * number of removed agents.
       */
      private int removeInboundAgents (boolean targetOutbound, int idxTarget,
            int diff) {
         int rem = 0;
         if (permInbound.length > 1)
            RandomPermutation.shuffle (permInbound, stream);
         for (int i = 0; i < inboundGroups.length && rem < diff; i++) {
            final int idxIn = permInbound[i];
            if (inboundGroups[idxIn].getNumAgents () == 0)
               continue;
            final int diff2 = Math.min (diff - rem, inboundGroups[idxIn]
                  .getNumAgents ());
            ++changeLock;
            try {
               inboundGroups[idxIn].setNumAgents (inboundGroups[idxIn]
                     .getNumAgents ()
                     - diff2);
            }
            finally {
               changeLock = 0;
            }
            rem += diff2;
            numInbound[idxIn] -= diff2;
            assert numInbound[idxIn] >= 0 : "numInbound = " + numInbound[idxIn];
            // This method must behave as if the agent is transferred
            // back into its original group before it is removed.
            numTransferredInbound[idxIn] -= diff2;
            if (targetOutbound)
               numTransferredOutbound[idxTarget] += diff2;
            else
               numTransferredInbound[idxTarget] += diff2;
         }
         return rem;
      }

      // private void printAgentsInfo () {
      // System.out.println ("AGENTS INFO");
      // for (int idxIn = 0; idxIn < inboundGroups.length; idxIn++) {
      // AgentGroup grp = inboundGroups[idxIn];
      // System.out
      // .printf (
      // "i=%d, numAgents=%d, numBusyAgents=%d, numGhostAgents=%d,
      // numInbound=%d, diffInbound=%d%n",
      // grp.getId (), grp.getNumAgents (), grp
      // .getNumBusyAgents (), grp.getNumGhostAgents (),
      // numInbound[idxIn], numTransferredInbound[idxIn]);
      // }
      // for (int idxOut = 0; idxOut < outboundGroups.length; idxOut++) {
      // AgentGroup grp = outboundGroups[idxOut];
      // System.out
      // .printf (
      // "i=%d, numAgents=%d, numBusyAgents=%d, numGhostAgents=%d,
      // numOutbound=%d, diffOutbound=%d%n",
      // grp.getId (), grp.getNumAgents (), grp
      // .getNumBusyAgents (), grp.getNumGhostAgents (),
      // numOutbound[idxOut], numTransferredOutbound[idxOut]);
      // }
      // System.out.println ("END OF AGENTS INFO");
      // }

      /**
       * Tries to remove the given number of outbound agents, and returns the
       * number of removed agents.
       */
      private int removeOutboundAgents (boolean targetOutbound, int idxTarget,
            int diff) {
         int rem = 0;
         if (permOutbound.length > 1)
            RandomPermutation.shuffle (permOutbound, stream);
         for (int i = 0; i < outboundGroups.length && rem < diff; i++) {
            final int idxOut = permOutbound[i];
            if (outboundGroups[idxOut].getNumAgents () == 0)
               continue;
            final int diff2 = Math.min (diff - rem, outboundGroups[idxOut]
                  .getNumAgents ());
            ++changeLock;
            try {
               outboundGroups[idxOut].setNumAgents (outboundGroups[idxOut]
                     .getNumAgents ()
                     - diff2);
            }
            finally {
               changeLock = 0;
            }
            rem += diff2;
            numOutbound[idxOut] -= diff2;
            assert numOutbound[idxOut] >= 0 : "numOutbound = "
                  + numOutbound[idxOut];
            // This method must behave as if the agent is transferred
            // back into its original group before it is removed.
            numTransferredOutbound[idxOut] -= diff2;
            if (targetOutbound)
               numTransferredOutbound[idxTarget] += diff2;
            else
               numTransferredInbound[idxTarget] += diff2;
         }
         return rem;
      }

      private class AgentGroupChecker implements AgentGroupListener {
         public void agentGroupChange (AgentGroup group) {
            if (changeLock > 0)
               return;
            if (group instanceof DetailedAgentGroup) {
               // We react only to calls to setNumAgents,
               // not to addAgent or removeAgent.
               final DetailedAgentGroup dgroup = (DetailedAgentGroup) group;
               if (dgroup.isAddingAgent ())
                  return;
               if (dgroup.isRemovingAgent ())
                  return;
            }

            // Search for the idxIn or idxOut associated with
            // the changed group.
            for (int idxIn = 0; idxIn < inboundGroups.length; idxIn++) {
               if (group != inboundGroups[idxIn])
                  continue;
               // Adds or subtracts the number of transferred
               // agents to/from the new number of agents.
               final int numAgentsInGroup = group.getNumAgents ()
                     + numTransferredInbound[idxIn];
               if (numAgentsInGroup >= numInbound[idxIn]) {
                  // We add new agents, or agent count unchanged.
                  // In both cases, we need to update the number of agents
                  // in the group.
                  numInbound[idxIn] = numAgentsInGroup;
                  ++changeLock;
                  try {
                     group.setNumAgents (numInbound[idxIn]);
                  }
                  finally {
                     --changeLock;
                  }
               }
               else {
                  // We need to remove agents
                  ++changeLock;
                  try {
                     // This is necessary for some removal operations
                     // to succeed.
                     group.setNumAgents (numInbound[idxIn]);
                  }
                  finally {
                     --changeLock;
                  }
                  final int diff = numInbound[idxIn] - numAgentsInGroup;
                  final int rem = removeOutboundAgents (false, idxIn, diff);
                  if (rem < diff) {
                     final int rem2 = removeInboundAgents (false, idxIn, diff - rem);
                     assert rem2 == diff - rem : "Could not remove all required agents";
                  }
               }
               return;
            }
            for (int idxOut = 0; idxOut < outboundGroups.length; idxOut++) {
               if (group != outboundGroups[idxOut])
                  continue;
               final int numAgentsInGroup = group.getNumAgents ()
                     + numTransferredOutbound[idxOut];
               if (numAgentsInGroup >= numOutbound[idxOut]) {
                  // We add new agents
                  numOutbound[idxOut] = numAgentsInGroup;
                  ++changeLock;
                  try {
                     group.setNumAgents (numOutbound[idxOut]);
                  }
                  finally {
                     --changeLock;
                  }
               }
               else {
                  // We remove agents
                  ++changeLock;
                  try {
                     group.setNumAgents (numOutbound[idxOut]);
                  }
                  finally {
                     --changeLock;
                  }
                  final int diff = numOutbound[idxOut] - numAgentsInGroup;
                  final int rem = removeOutboundAgents (true, idxOut, diff);
                  if (rem < diff) {
                     final int rem2 = removeInboundAgents (true, idxOut, diff - rem);
                     assert rem2 == diff - rem : "Could not remove all required agents";
                  }
               }
               if (outboundGroupProbs[idxOut] == 0)
                  while (outboundGroups[idxOut].getNumIdleAgents () > 0)
                     transferToInbound (idxOut,
                           ((DetailedAgentGroup) outboundGroups[idxOut])
                                 .getIdleAgents ().get (0));
               return;
            }
         }

         public void beginService (EndServiceEvent ev) {}

         public void endContact (EndServiceEvent ev) {}

         public void endService (EndServiceEvent ev) {
            if (ev instanceof EndServiceEventDetailed) {
               final Agent agent = ((EndServiceEventDetailed) ev).getAgent ();
               if (markedAgents.contains (agent)) {
                  markedAgents.remove (agent);
                  final AgentGroup grp = agent.getAgentGroup ();
                  if (!agent.isGhost () && grp != null) {
                     // We need to search for the index
                     int idxOut = -1;
                     for (int i = 0; i < outboundGroups.length && idxOut == -1; i++)
                        if (grp == outboundGroups[i])
                           idxOut = i;
                     if (idxOut == -1)
                        throw new IllegalStateException (
                              "Could not find index of agent group with id "
                                    + grp.getId ());
                     transferToInbound (idxOut, agent);
                     ++changeLock;
                  }
                  try {
                     agent.setAvailable (true);
                  }
                  finally {
                     --changeLock;
                  }
               }
            }
         }

         public void init (AgentGroup group) {}
      }
   }
}
