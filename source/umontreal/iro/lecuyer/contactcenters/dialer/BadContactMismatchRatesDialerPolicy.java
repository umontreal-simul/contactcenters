package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupSet;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.stat.mperiods.SumMatrixSW;

/**
 * Represents a threshold-based dialer's policy taking bad contact and mismatch
 * rates into account for dialing, as used in Deslaurier's blend call center
 * model \cite{ccDES03a}. This dialer's policy needs to be informed about the
 * contact center's activity through two methods:
 * {@link #notifyInboundContact(Contact,boolean)}, and
 * {@link #notifyOutboundContact(Contact,boolean)}. When an inbound contact is
 * processed (served, abandoned, or blocked) by the contact center, the
 * {@link #notifyInboundContact(Contact,boolean)} method needs to be called.
 * When an outbound contact is processed, the
 * {@link #notifyOutboundContact(Contact,boolean)} method must be called. For
 * both contact types, the user has to indicate this dialer's policy if the
 * processed contact must be considered as good or bad. Usually, a \emph{good}
 * inbound contact is a contact meeting some service level requirements, e.g.,
 * having waited less than a certain time in queue. An outbound contact can be
 * considered as good if it is not a mismatch.
 *
 * When the dialer's policy is asked a number of calls to make, it gets the
 * total number of free agents $\Ntf(t)$ in a \emph{test set}. If this number is
 * smaller than a given minimum $s_{\mathrm{t}}$, no call is made. Otherwise,
 * the dialer looks at the rate of bad inbound contacts in the last $p$ periods
 * of duration~$d$. If this rate is smaller than or equal to a threshold
 * $s_{\mathrm{i}}$, the dialer's policy evaluates $\Ndf(t)$, the number of free
 * agents in a \emph{target set}. If this number is smaller than
 * $s_{\mathrm{d}}$, no call is made. Otherwise, the base number of calls to
 * dial $n=\max\{${@link Math#round Math.round}\texttt{
 * (}$\kappa\Ndf(t)$\texttt{)}$+c-a, 0\}$ is computed. Then, if the rate of bad
 * outbound contacts in the $p$ last periods of duration~$d$ is smaller than or
 * equal to a threshold $s_{\mathrm{o}}$, $2n$ calls are made. Otherwise, $n$
 * calls are made.
 */
public class BadContactMismatchRatesDialerPolicy extends ThresholdDialerPolicy {
   private boolean started = false;
   private double startTime;
   private SumMatrixSW inBadContacts;
   private SumMatrixSW inTotal;
   private SumMatrixSW mismatch;
   private SumMatrixSW outTotal;

   private double maxBadContactRate;
   private double mismatchRateThresh;
   private int numCheckedPeriods;
   private double checkedPeriodDuration;

   /**
    * This is the same as the constructor
    * {@link #BadContactMismatchRatesDialerPolicy(DialerList,AgentGroupSet,AgentGroupSet,int,int,double,int,double,double,int,double)}
    * , with $\kappa=1$ and $c=0$.
    *
    * @param list
    *           the dialer list from which to get contacts.
    * @param testSet
    *           the test agent group set.
    * @param targetSet
    *           the target agent group set.
    * @param minFreeTest
    *           the minimal number of free agents in the test set.
    * @param minFreeTarget
    *           the minimal number of free agents in the target set.
    * @param maxBadContactRate
    *           the maximal rate of bad contacts.
    * @param mismatchRateThresh
    *           the mismatch rate threshold.
    * @param numCheckedPeriods
    *           the number of checked periods $p$.
    * @param checkedPeriodDuration
    *           the duration $d$ of checked periods.
    * @exception NullPointerException
    *               if \texttt{list}, \texttt{testSet} or \texttt{targetSet} are
    *               \texttt{null}.
    * @exception IllegalArgumentException
    *               if a threshold on the number of agents, the number of
    *               checked periods or the duration of checked periods are
    *               negative, or a rate is negative or greater than 1.
    */
   public BadContactMismatchRatesDialerPolicy (DialerList list,
         AgentGroupSet testSet, AgentGroupSet targetSet, int minFreeTest,
         int minFreeTarget, double maxBadContactRate,
         double mismatchRateThresh, int numCheckedPeriods,
         double checkedPeriodDuration) {
      this (list, testSet, targetSet, minFreeTest, minFreeTarget, 1.0, 0,
            maxBadContactRate, mismatchRateThresh, numCheckedPeriods,
            checkedPeriodDuration);
   }

   /**
    * Constructs a new bad contact/mismatch rates dialer's policy with the
    * dialer list \texttt{list}, test set \texttt{testSet}, target set
    * \texttt{targetSet}. The minimal number of free agents is set to
    * \texttt{minFreeTest} for the test set, and \texttt{minFreeTarget} for the
    * target set. The maximal rate of bad contacts is set to
    * \texttt{maxBadContactRate}, while the threshold for mismatch rate is
    * \texttt{mismatchRateThresh}. To take its decisions, the policy uses rates
    * for the last \texttt{numCheckedPeriods} periods of duration
    * \texttt{checkedPeriodDuration}.
    *
    * @param list
    *           the dialer list from which to get contacts.
    * @param testSet
    *           the test agent group set.
    * @param targetSet
    *           the target agent group set.
    * @param minFreeTest
    *           the minimal number of free agents in the test set.
    * @param minFreeTarget
    *           the minimal number of free agents in the target set.
    * @param kappa
    *           the $\kappa$ multiplicative constant.
    * @param c
    *           the $c$ additive constant.
    * @param maxBadContactRate
    *           the maximal rate of bad contacts.
    * @param mismatchRateThresh
    *           the mismatch rate threshold.
    * @param numCheckedPeriods
    *           the number of checked periods $p$.
    * @param checkedPeriodDuration
    *           the duration $d$ of checked periods.
    * @exception NullPointerException
    *               if \texttt{list}, \texttt{testSet} or \texttt{targetSet} are
    *               \texttt{null}.
    * @exception IllegalArgumentException
    *               if a threshold on the number of agents, the number of
    *               checked periods or the duration of checked periods are
    *               negative, or a rate is negative or greater than 1.
    */
   public BadContactMismatchRatesDialerPolicy (DialerList list,
         AgentGroupSet testSet, AgentGroupSet targetSet, int minFreeTest,
         int minFreeTarget, double kappa, int c, double maxBadContactRate,
         double mismatchRateThresh, int numCheckedPeriods,
         double checkedPeriodDuration) {
      super (list, testSet, targetSet, minFreeTest, minFreeTarget, kappa, c);
      if (maxBadContactRate < 0 || maxBadContactRate > 1)
         throw new IllegalArgumentException (
               "The maximal rate of bad contacts must be in [0, 1]");
      if (mismatchRateThresh < 0 || mismatchRateThresh > 1)
         throw new IllegalArgumentException (
               "The mismatch rate threshold must be in [0, 1]");
      if (numCheckedPeriods <= 0)
         throw new IllegalArgumentException (
               "The number of checked periods must be greater than 0");
      if (checkedPeriodDuration <= 0)
         throw new IllegalArgumentException (
               "The duration of the checked periods must be positive");
      this.maxBadContactRate = maxBadContactRate;
      this.mismatchRateThresh = mismatchRateThresh;
      this.numCheckedPeriods = numCheckedPeriods;
      this.checkedPeriodDuration = checkedPeriodDuration;

      inBadContacts = new SumMatrixSW (1, numCheckedPeriods + 1);
      inTotal = new SumMatrixSW (1, numCheckedPeriods + 1);
      mismatch = new SumMatrixSW (1, numCheckedPeriods + 1);
      outTotal = new SumMatrixSW (1, numCheckedPeriods + 1);
   }

   /**
    * Returns the maximal rate of bad contacts $s_{\mathrm{i}}$ for this
    * dialer's policy.
    *
    * @return the maximal rate of bad contacts.
    */
   public double getMaxBadContactRate () {
      return maxBadContactRate;
   }

   /**
    * Sets the maximal rate of bad contacts for this dialer's policy to
    * \texttt{maxBadContactRate}.
    *
    * @param maxBadContactRate
    *           the new rate of bad contacts.
    * @exception IllegalArgumentException
    *               if \texttt{maxBadContactRate} is smaller than 0 or greater
    *               than 1.
    */
   public void setMaxBadContactRate (double maxBadContactRate) {
      if (maxBadContactRate < 0 || maxBadContactRate > 1)
         throw new IllegalArgumentException (
               "The maximal rate of bad contacts must be in [0, 1]");
      this.maxBadContactRate = maxBadContactRate;
   }

   /**
    * Returns the threshold on the mismatch rate $s_{\mathrm{o}}$ for this
    * dialer's policy.
    *
    * @return the mismatch rate threshold.
    */
   public double getMismatchRateThresh () {
      return mismatchRateThresh;
   }

   /**
    * Sets the threshold on the mismatch rate to \texttt{mismatchRateThresh}.
    *
    * @param mismatchRateThresh
    *           the threshold on the mismatch rate.
    * @exception IllegalArgumentException
    *               if \texttt{mismatchRateThresh} is smaller than 0 or greater
    *               than 1.
    */
   public void setMismatchRateThresh (double mismatchRateThresh) {
      if (mismatchRateThresh < 0 || mismatchRateThresh > 1)
         throw new IllegalArgumentException (
               "The mismatch rate threshold must be in [0, 1]");
      this.mismatchRateThresh = mismatchRateThresh;
   }

   /**
    * Returns the number of checked periods $p$ for this dialer's policy.
    *
    * @return the number of checked periods.
    */
   public int getNumCheckedPeriods () {
      return numCheckedPeriods;
   }

   /**
    * Sets the number of checked periods to \texttt{numCheckedPeriods}.
    *
    * @param numCheckedPeriods
    *           the number of checked periods.
    * @exception IllegalArgumentException
    *               if \texttt{numCheckedPeriods} is negative or 0.
    */
   public void setNumCheckedPeriods (int numCheckedPeriods) {
      if (numCheckedPeriods <= 0)
         throw new IllegalArgumentException (
               "The number of checked periods must be greater than 1");
      if (numCheckedPeriods == this.numCheckedPeriods)
         return;
      this.numCheckedPeriods = numCheckedPeriods;
      inTotal.setNumPeriods (numCheckedPeriods + 1);
      outTotal.setNumPeriods (numCheckedPeriods + 1);
      inBadContacts.setNumPeriods (numCheckedPeriods + 1);
      mismatch.setNumPeriods (numCheckedPeriods + 1);
   }

   /**
    * Returns the duration~$d$ of the checked periods.
    *
    * @return the checked period duration.
    */
   public double getCheckedPeriodDuration () {
      return checkedPeriodDuration;
   }

   /**
    * Sets the duration of the checked periods to
    * \texttt{checkedPeriodDuration}.
    *
    * @param checkedPeriodDuration
    *           the duration of the checked periods.
    * @exception IllegalArgumentException
    *               if \texttt{checkedPeriodDuration} is negative or 0.
    */
   public void setCheckedPeriodDuration (double checkedPeriodDuration) {
      if (checkedPeriodDuration <= 0)
         throw new IllegalArgumentException (
               "The checked period duration must be greater than 0");
      if (checkedPeriodDuration == this.checkedPeriodDuration)
         return;
      this.checkedPeriodDuration = checkedPeriodDuration;
      inTotal.init ();
      outTotal.init ();
      inBadContacts.init ();
      mismatch.init ();
      // if (started)
      // startTime = Sim.time();
   }

   @Override
   public void init (Dialer dialer) {
      inBadContacts.init ();
      inTotal.init ();
      mismatch.init ();
      outTotal.init ();
      super.init (dialer);
   }

   @Override
   public void dialerStarted (Dialer dialer) {
      started = true;
      startTime = dialer.simulator ().time ();
   }

   @Override
   public void dialerStopped (Dialer dialer) {
      started = false;
   }

   private final int getPeriod (Simulator sim) {
      return (int) ((sim.time () - startTime) / checkedPeriodDuration);
   }

   /**
    * Gets the current bad contact rate as used by {@link #getNumDials}.
    *
    * @return the current bad contact rate.
    */
   public double getCurrentBadContactRate () {
      double bq = 0;
      double ar = 0;
      for (int i = 0; i < inBadContacts.getNumPeriods () - 1; i++) {
         bq += inBadContacts.getMeasure (0, i);
         ar += inTotal.getMeasure (0, i);
      }
      return ar == 0 ? 0 : bq / ar;
   }

   /**
    * Returns the current mismatch rate as used by {@link #getNumDials}.
    *
    * @return the current mismatch rate.
    */
   public double getCurrentMismatchRate () {
      double nm = 0;
      double ar = 0;
      for (int i = 0; i < mismatch.getNumPeriods () - 1; i++) {
         nm += mismatch.getMeasure (0, i);
         ar += outTotal.getMeasure (0, i);
      }
      return ar == 0 ? 0 : nm / ar;
   }

   /**
    * Notify a processed inbound contact to this dialer's policy. The
    * \texttt{bad} indicator determines if a bad contact is notified. The
    * simulator must determine which contacts to notify as well as a definition
    * of bad contacts. Usually, all inbound contacts are notified to the dialer,
    * and bad contacts have a waiting time greater than some acceptable waiting
    * time.
    *
    * @param contact
    *           the notified contact.
    * @param bad
    *           \texttt{true} if a bad contact is notified, \texttt{false} if a
    *           good contact is notified.
    */
   public void notifyInboundContact (Contact contact, boolean bad) {
      if (!started)
         return;
      final int p = getPeriod (contact.simulator ());
      inTotal.add (0, p, 1);
      inBadContacts.add (0, p, bad ? 1 : 0);
   }

   /**
    * Notifies an outbound contact to this dialer policy. If \texttt{m} is
    * \texttt{true}, the contact is a mismatch, i.e., it has balked (most often)
    * or needed to wait before abandoning or being served. The application needs
    * to decide which outbound contacts are notified and determine if contacts
    * are mismatches or not. Usually, only outbound contacts produced by the
    * dialer using this policy are notified, including right and wrong party
    * connects.
    *
    * @param contact
    *           the notified contact.
    * @param m
    *           \texttt{true} if the notified contact is a mismatch,
    *           \texttt{false} otherwise.
    */
   public void notifyOutboundContact (Contact contact, boolean m) {
      if (!started)
         return;
      final int p = getPeriod (contact.simulator ());
      outTotal.add (0, p, 1);
      mismatch.add (0, p, m ? 1 : 0);
   }

   @Override
   public int getNumDials (Dialer dialer) {
      final int n = super.getNumDials (dialer);
      if (n == 0)
         return 0;
      if (getCurrentBadContactRate () > maxBadContactRate)
         return 0;
      if (getCurrentMismatchRate () <= mismatchRateThresh)
         return 2 * n;
      else
         return n;
      // int nFree = getTestSet().getNumFreeAgents();
      // if (nFree >= getMinFreeAgentsTest()) {
      // int nOut = getTargetSet().getNumFreeAgents();
      // if (nOut >= getMinFreeAgentsTarget() && getCurrentBadContactRate() <=
      // maxBadContactRate) {
      // if (getCurrentMismatchRate() <= mismatchRateThresh)
      // return 2*nOut;
      // else
      // return nOut;
      // }
      // }
      // return 0;
   }

   /**
    * Returns the matrix of sums counting the number of bad inbound contacts
    * notified to this dialing policy.
    *
    * @return the matrix of sums for bad contacts.
    */
   public SumMatrixSW getInBadContactsSumMatrix () {
      return inBadContacts;
   }

   /**
    * Returns the matrix of sums counting the total number of inbound contacts
    * notified to this dialing policy.
    *
    * @return the matrix of sums for the total number of contacts.
    */
   public SumMatrixSW getInTotalSumMatrix () {
      return inTotal;
   }

   /**
    * Returns the matrix of sums counting the number of mismatches notified to
    * this dialing policy.
    *
    * @return the matrix of sums for the number of mismatches.
    */
   public SumMatrixSW getMismatchSumMatrix () {
      return mismatch;
   }

   /**
    * Returns the matrix of sums counting the total number of outbound contacts
    * for this dialing policy.
    *
    * @return the matrix of sums for the number of outbound contacts.
    */
   public SumMatrixSW getOutTotalSumMatrix () {
      return outTotal;
   }
}
