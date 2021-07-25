package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.server.AgentGroupSet;

/**
 * Represents a threshold-based dialing policy
 * selecting the number of calls to try based on the
 * number of free agents in certain groups.
 * Before trying to make calls, the policy
 * determines the total number of free
 * agents $\Ntf(t)$ in a \emph{test set} of agent groups.
 * If the number of free agents is greater than
 * or equal to $s_{\mathrm{t}}$, the policy
 * counts the total number $\Ndf(t)$ of free agents
 * in a \emph{target set} of agent groups which may
 * differ from the test set.  If $\Ndf(t)\ge s_{\mathrm{d}}$,
 * the dialer tries to make
 * $\max\{${@link Math#round Math.round}\texttt{ (}$\kappa\Ndf(t)$\texttt{)}$+c-a, 0\}$
 * calls, where $\kappa\in\RR$ and $c\in\NN$ are predefined
 * numbers. The constant $a$ is the result of {@link Dialer#getNumActionEvents()}
 * if {@link Dialer#isUsingNumActionsEvents()} returns
 * \texttt{true}, or 0 otherwise.
 * Any parameter used by this policy can be changed at
 * any time during the simulation.
 */
public class ThresholdDialerPolicy implements DialerPolicy {
   private DialerList list;
   private AgentGroupSet testGroups;
   private AgentGroupSet targetGroups;
   private int minFreeTest;
   private int minFreeTarget;
   private double kappa;
   private int c;

   /**
    * Constructs a new dialer's policy with dialer list
    * \texttt{list}, test set \texttt{testGroups}, and
    * target set \texttt{targetGroups}.
    * The free agents threshold for the test set is set to
    * \texttt{minFreeTest}, the threshold is set to \texttt{minFreeTarget}
    * for the target set,
    * the multiplicative constant
    * is set to \texttt{kappa}, and the additive
    * constant to \texttt{c}.
    @param list the dialer list being used.
    @param testGroups the test set of agent groups.
    @param targetGroups the target set of agent groups.
    @param minFreeTest the (inclusive) minimum number of free agents in the test set.
    @param minFreeTarget the (inclusive) minimum number of free agents in the target set.
    @param kappa the multiplicative constant.
    @param c the additive constant.
    @exception NullPointerException if \texttt{list},
    \texttt{testGroups}, or \texttt{targetGroups}
    are \texttt{null}.
    @exception IllegalArgumentException if the free
    agents threshold is negative.
    */
   public ThresholdDialerPolicy
      (DialerList list, AgentGroupSet testGroups,
       AgentGroupSet targetGroups,
       int minFreeTest, int minFreeTarget, double kappa, int c) {
      if (list == null)
         throw new NullPointerException ("The dialer list must not be null");
      if (testGroups == null || targetGroups == null)
         throw new NullPointerException ("The test and target sets must not be null");
      if (minFreeTest < 0)
         throw new IllegalArgumentException
            ("minFreeTest < 0");
      if (minFreeTarget < 0)
         throw new IllegalArgumentException
            ("minFreeTarget < 0");
      this.list = list;
      this.testGroups = testGroups;
      this.targetGroups = targetGroups;
      this.minFreeTest = minFreeTest;
      this.minFreeTarget = minFreeTarget;
      this.kappa = kappa;
      this.c = c;
   }

   /**
    * Equivalent to {@link #ThresholdDialerPolicy(DialerList,AgentGroupSet,AgentGroupSet,
    * int,int,double,int) Threshold\-Dialer\-Policy} \texttt{(list, testGroups, targetGroups,
    * minFreeTest, 1, kappa, c)}.
    */
   public ThresholdDialerPolicy
      (DialerList list, AgentGroupSet testGroups,
       AgentGroupSet targetGroups,
       int minFreeTest, double kappa, int c) {
      this (list, testGroups, targetGroups, minFreeTest, 1, kappa, c);
   }

   public DialerList getDialerList(Dialer dialer) {
      return list;
   }

   /**
    * Sets the currently used dialer list to \texttt{list}.
    @param list the new dialer list.
    @exception NullPointerException if \texttt{list} is
    \texttt{null}.
    */
   public void setDialerList (DialerList list) {
      if (list == null)
         throw new NullPointerException ("The dialer list must not be null");
      this.list = list;
   }

   /**
    * Returns the minimal number of free agents $s_{\mathrm{t}}$
    * in the test set to try outbound calls.
    @return the minimal number of free agents in the test set to dial.
    */
   public int getMinFreeAgentsTest() {
      return minFreeTest;
   }

   /**
    * Sets the minimal number of free agents in the test set to \texttt{minFreeTest}.
    @param minFreeTest the new minimal number of free agents in the test set.
    @exception IllegalArgumentException if \texttt{minFreeTest}
    is negative.
    */
   public void setMinFreeAgentsTest (int minFreeTest) {
      if (minFreeTest < 0)
         throw new IllegalArgumentException
            ("minFreeTest < 0");
      this.minFreeTest = minFreeTest;
   }

   /**
    * Returns the minimal number of free agents $s_{\mathrm{d}}$
    * in the target set to try outbound calls.
    @return the minimal number of free agents in the target set to dial.
    */
   public int getMinFreeAgentsTarget() {
      return minFreeTarget;
   }

   /**
    * Sets the minimal number of free agents in the target set to \texttt{minFreeTarget}.
    @param minFreeTarget the new minimal number of free agents in the target set.
    @exception IllegalArgumentException if \texttt{minFreeTarget}
    is negative.
    */
   public void setMinFreeAgentsTarget (int minFreeTarget) {
      if (minFreeTarget < 0)
         throw new IllegalArgumentException
            ("minFreeTarget < 0");
      this.minFreeTarget = minFreeTarget;
   }

   /**
    * Returns the current value of the multiplicative
    * constant $\kappa$ for this policy.
    @return the multiplicative constant.
    */
   public double getKappa() {
      return kappa;
   }

   /**
    * Sets the multiplicative constant $\kappa$ to \texttt{kappa}
    * for this dialer policy.
    @param kappa the new multiplicative constant.
    */
   public void setKappa (double kappa) {
      this.kappa = kappa;
   }

   /**
    * Returns the current value of the additive
    * constant $c$ for this policy.
    @return the additive constant.
    */
   public int getC() {
      return c;
   }

   /**
    * Sets the additive constant $c$ to \texttt{c}
    * for this dialer's policy.
    @param c the new additive constant.
    */
   public void setC (int c) {
      this.c = c;
   }

   /**
    * Returns the current test set of agent groups.
    @return the test set of agent groups.
    */
   public AgentGroupSet getTestSet() {
      return testGroups;
   }

   /**
    * Sets the test set of agent groups to \texttt{testGroups}.
    @param testGroups the new test set of agent groups.
    @exception NullPointerException if \texttt{testGroups} is \texttt{null}.
    */
   public void setTestSet (AgentGroupSet testGroups) {
      if (testGroups == null)
         throw new NullPointerException ("The test set must not be null");
      this.testGroups = testGroups;
   }

   /**
    * Returns the current target set of agent groups.
    @return the target set of agent groups.
    */
   public AgentGroupSet getTargetSet() {
      return targetGroups;
   }

   /**
    * Sets the target set of agent groups to \texttt{targetGroups}.
    @param targetGroups the new target set of agent groups.
    @exception NullPointerException if \texttt{targetGroups} is \texttt{null}.
    */
   public void setTargetSet (AgentGroupSet targetGroups) {
      if (targetGroups == null)
         throw new NullPointerException ("The target set must not be null");
      this.targetGroups = targetGroups;
   }

   public void init(Dialer dialer) {
      list.clear();
   }

   public void dialerStarted(Dialer dialer) {}

   public void dialerStopped(Dialer dialer) {}

   public int getNumDials(Dialer dialer) {
      final int free = testGroups.getNumFreeAgents();
      if (free < minFreeTest)
         return 0;

      final int n = targetGroups.getNumFreeAgents();
      if (n < minFreeTarget)
         return 0;
      final int a = dialer.isUsingNumActionsEvents () ? dialer.getNumActionEvents () : 0;
      final int d = (int)Math.round (kappa*n) + c - a;
      if (d < 0)
         return 0;
      else
         return d;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("dialer list: ").append (list);
      if (minFreeTest > 0)
         sb.append (", minimum number of free agents in test set: ").append (minFreeTest);
      if (minFreeTarget > 0)
         sb.append (", minimum number of free agents in target set: ").append (minFreeTarget);
      sb.append (", kappa: ").append (kappa);
      sb.append (", c: ").append (c);
      sb.append (']');
      return sb.toString();
   }
}
