package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.functions.MathFunctionUtil;
import umontreal.ssj.probdist.BetaDist;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.stat.AccumulateWithTimes;

/**
 * Represents statistical counters computing sums
 * for individual replications of a simulation
 * of a call center using
 * a discrete-time Markov chain.
 * After a simulator constructs an instance of this
 * class, it calls {@link #init(CallCenterCTMC,double,int)}
 * at the beginning of each replication, and
 * then uses {@link #collectStat(CallCenterCTMC,TransitionType)}
 * for each simulated transition.
 * At the end of the simulation, the method
 * {@link #updateStatOnTime(CallCenterCTMC)}
 * should also be called.
 */
public class CallCenterCounters implements Cloneable {
   private int ns;
   private int numTypes, numGroups;
   private double[] m_awt, gawt;
   private ProbInAWT[] m_probInAWT;
   private ProbInAWT[] probInAWTG;

   private boolean randomHorizon;
   
   private double[] jumpRate;
   private int[] startingTransition;
   private int counterPeriod;
   private double m_timeHorizon;

   public double[] numArrivals;
   public double[] numBlocked;
   public double[] numAbandoned;
   public double[] queueSize;
   public double[] busyAgents;
   public double[] totalAgents;
   public double[] servedRates;
   public double[] maxQueueSize;
   public double[] maxBusyAgents;
   public double numTransitions;
   public double numFalseTransitions;
   private AccumulateWithTimes[] statQueueSize;
   private AccumulateWithTimes[] statBusyAgents;
   private AccumulateWithTimes[] statTotalAgents;

   public double[] numServedBeforeAWT;
   public double[] numAbandonedBeforeAWT;
   public double[] numServedBeforeAWTG;
   public double[] numAbandonedBeforeAWTG;
   public double[] sumWaitingTimesServed;
   public double[] sumWaitingTimesAbandoned;

   /**
    * Constructs a new set of call center counters using
    * the given CTMC to obtain the number of call types, agent groups, etc.
    * The argument \texttt{awt}
    * contains a vector giving the acceptable waiting times
    * for estimating the expected number of calls waiting less
    * than a given time limit.
    * Element $sK'+k$ of this array gives the
    * $s$th AWT for calls of type~$k$ if $k=0,\ldots,K-1$,
    * or for calls of all types
    * if $k=K$. Here, $K'=K+1$ if $K>1$, or $K$ otherwise.
    * This can be \texttt{null} if performance measures based on
    * acceptable waiting times are not estimated.
    * 
    * The boolean \texttt{randomHorizon} is
    * set to \texttt{true}
    * if we are simulating on a random
    * horizon, or \texttt{false} for
    * deterministic horizon.
    * The horizon type has an impact on how some
    * performance measures are estimated.
    * @param ctmc the call center model.
    * @param awt the vector of acceptable waiting times.
    * @param randomHorizon \texttt{true} for a random time horizon,
    * \texttt{false} for a deterministic horizon.
    */
   public CallCenterCounters (CallCenterCTMC ctmc, double[] awt,
         boolean randomHorizon) {
      this (ctmc, awt, null, randomHorizon, new double[] { ctmc.getJumpRate () },
            new int[] {0}, 0);
   }
   
   /**
    * Similar to constructor {@link #CallCenterCounters(CallCenterCTMC,double[],boolean)},
    * for a case with multiple periods.
    * The additional argument \texttt{gawt} plays a role similar
    * to \texttt{awt}, but
    * contains thresholds used for all periods;
    * this is used to estimate performance measures
    * based of acceptable waiting times over the entire
    * horizon while \texttt{awt} is used for the estimates
    * over a single period.
    * 
    * The last three arguments are used to estimate
    * performance measures for calls arriving
    * into and leaving the system during different periods.
    * The argument \texttt{jumpRate} contains
    * a $(P+1)$-dimensional vector of transition rates for
    * each of the $P+1$ period-specific CTMCs; there is $P$ CTMCs for
    * the main periods, plus one CTMC for the wrap-up period.
    * The argument \texttt{startingTransition}, on the other hand,
    * is a $(P+1)$-dimensional
    * vector giving the starting transition for each period;
    * this is updated as the simulation is made.
    * The argument \texttt{counterPeriod} gives the index
    * of the period for which this set of counters computes sums. 
    * 
    * @param ctmc the call center model.
    * @param awt the vector of period-specific acceptable waiting times.
    * @param gawt the vector of global acceptable waiting times.
    * @param randomHorizon \texttt{true} for a random time horizon,
    * \texttt{false} for a deterministic horizon.
    * @param jumpRate the per-period transition rates.
    * @param startingTransition the starting transitions for each period.
    * @param counterPeriod the period concerned by this counter.
    */
   public CallCenterCounters (CallCenterCTMC ctmc, double[] awt, double[] gawt,
         boolean randomHorizon,
         double[] jumpRate, int[] startingTransition, int counterPeriod) {
      this.startingTransition = startingTransition;
      this.counterPeriod = counterPeriod;
      this.jumpRate = jumpRate;
      this.randomHorizon = randomHorizon;
      numTypes = ctmc.getNumContactTypes ();
      numGroups = ctmc.getNumAgentGroups ();
      numArrivals = new double[numTypes];
      numBlocked = new double[numTypes];
      numAbandoned = new double[numTypes];
      queueSize = new double[numTypes];
      servedRates = new double[numTypes*numGroups];
      statQueueSize = new AccumulateWithTimes[numTypes];
      for (int k = 0; k < statQueueSize.length; k++)
         statQueueSize[k] = new AccumulateWithTimes ();

      busyAgents = new double[numGroups];
      statBusyAgents = new AccumulateWithTimes[numGroups];
      for (int i = 0; i < statBusyAgents.length; i++)
         statBusyAgents[i] = new AccumulateWithTimes ();

      totalAgents = new double[numGroups];
      statTotalAgents = new AccumulateWithTimes[numGroups];
      for (int i = 0; i < statTotalAgents.length; i++)
         statTotalAgents[i] = new AccumulateWithTimes ();
      
      final int nt = numTypes > 1 ? numTypes + 1 : numTypes;
      ns = awt == null ? 0 : awt.length / nt;
      numServedBeforeAWT = new double[nt * ns];
      numAbandonedBeforeAWT = new double[nt * ns];
      numServedBeforeAWTG = new double[nt * ns];
      numAbandonedBeforeAWTG = new double[nt * ns];
      sumWaitingTimesServed = new double[numTypes];
      sumWaitingTimesAbandoned = new double[numTypes];
      maxQueueSize = new double[nt];

      this.m_awt = awt;
      this.gawt = gawt;
      if (awt != null) {
         m_probInAWT = new ProbInAWT[awt.length];
         for (int s = 0; s < awt.length; s++) {
            if (randomHorizon)
               m_probInAWT[s] = new ProbInAWTGamma ();
            else
               m_probInAWT[s] = new ProbInAWTBinomial ();
         }
      }
      else
         m_probInAWT = new ProbInAWT[0];
      if (gawt != null) {
         probInAWTG = new ProbInAWT[gawt.length];
         for (int s = 0; s < gawt.length; s++) {
            if (randomHorizon)
               probInAWTG[s] = new ProbInAWTGamma ();
            else
               probInAWTG[s] = new ProbInAWTBinomial ();
         }
      }
      else
         probInAWTG = new ProbInAWT[0];

      final int ng = numGroups > 1 ? numGroups + 1 : numGroups;
      maxBusyAgents = new double[ng];
   }

   /**
    * Initializes this set of counters using
    * the given call center CTMC \texttt{ctmc}, for
    * a simulation over a time horizon \texttt{timeHorizon} with
    * \texttt{ntr} transitions.
    * For random horizon, the number of transitions
    * is ignored.
    * @param ctmc the call center CTMC.
    * @param timeHorizon the time horizon.
    * @param ntr the number of transitions to simulate.
    */
   public void init (CallCenterCTMC ctmc, double timeHorizon, int ntr) {
      for (int s = 0; s < m_probInAWT.length; s++)
         m_probInAWT[s].init (m_awt[s], ctmc.getJumpRate (), timeHorizon, ntr);
      for (int s = 0; s < probInAWTG.length; s++)
         probInAWTG[s].init (gawt[s], ctmc.getJumpRate (), timeHorizon, ntr);
      this.m_timeHorizon = timeHorizon;
      numTransitions = ntr;
      
      initArrays();
      
      for (int k = 0; k < ctmc.getNumContactTypes (); k++)
         statQueueSize[k].init (ctmc.getNumTransitionsDone (), ctmc.getNumContactsInQueue (k));
      for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
         statBusyAgents[i].init (ctmc.getNumTransitionsDone (), ctmc.getNumContactsInServiceI (i));
         statTotalAgents[i].init (ctmc.getNumTransitionsDone (),
               Math.max (ctmc.getNumContactsInServiceI (i), ctmc.getNumAgents (i)));
      }
   }
   
   private void initArrays() {
      Arrays.fill (numArrivals, 0);
      Arrays.fill (numAbandoned, 0);
      Arrays.fill (numBlocked, 0);
      Arrays.fill (queueSize, 0);
      Arrays.fill (busyAgents, 0);
      Arrays.fill (totalAgents, 0);
      numFalseTransitions = 0;
      Arrays.fill (numServedBeforeAWT, 0);
      Arrays.fill (numAbandonedBeforeAWT, 0);
      Arrays.fill (numServedBeforeAWTG, 0);
      Arrays.fill (numAbandonedBeforeAWTG, 0);
      Arrays.fill (sumWaitingTimesServed, 0);
      Arrays.fill (sumWaitingTimesAbandoned, 0);
      Arrays.fill (servedRates, 0);
      Arrays.fill (maxQueueSize, 0);
      Arrays.fill (maxBusyAgents, 0);
   }
   
   private static void add (double[] src, double[] target) {
      assert src.length == target.length;
      for (int i = 0; i < src.length; i++)
         src[i] += target[i];
   }

   private static void max (double[] src, double[] target) {
      assert src.length == target.length;
      for (int i = 0; i < src.length; i++)
         if (target[i] > src[i])
            src[i] = target[i];
   }
   
   private static void div (double[] src, double val) {
      for (int i = 0; i < src.length; i++)
         src[i] /= val;
   }
   
   /**
    * For each counter of this set, replaces the
    * current value with the sum of the values
    * of all corresponding counters in the sets
    * given by the array \texttt{counters}.
    * This can be used to aggregate statistics from
    * successive individual periods into a single counter.
    * 
    * The \texttt{lastTimeAvg} boolean determines if
    * the last counter in the given array, which
    * usually corresponds to counters concerning the wrap-up
    * period, is taken into account when summing
    * the time-average queue size and number of agents.
    * This has no impact on other statistics.
    * 
    * If \texttt{statAWTG} is set to \texttt{true},
    * the number of calls waiting less than the acceptable waiting
    * time, stored in fields {@link #numServedBeforeAWT}
    * and {@link #numAbandonedBeforeAWT},
    * are determined by summing the numbers in fields
    * {@link #numServedBeforeAWTG} and
    * {@link #numAbandonedBeforeAWTG} in
    * the counters of \texttt{counters}.
    * If \texttt{statAWTG} is \texttt{false},
    * the fields {@link #numServedBeforeAWT} and
    * {@link #numAbandonedBeforeAWT}
    * are used instead.
    * @param lastTimeAvg determines whether the last element in
    * array \texttt{counters} is taken into account
    * for average queue size and number of agents.
    * @param statAWTG determines how the number of
    * calls waiting less than the acceptable waiting time
    * is summed up. 
    * @param counters the array of counters.
    */
   public void collectSum (boolean lastTimeAvg,
         boolean statAWTG,
         CallCenterCounters... counters) {
      initArrays();
      numTransitions = 0;
      for (int i = 0; i < counters.length; i++) {
         CallCenterCounters counter = counters[i];
         add (numArrivals, counter.numArrivals);
         add (numAbandoned, counter.numAbandoned);
         add (numBlocked, counter.numBlocked);
         numTransitions += counter.numTransitions;
         numFalseTransitions += counter.numFalseTransitions;
         if (statAWTG) {
            add (numServedBeforeAWT, counter.numServedBeforeAWTG);
            add (numAbandonedBeforeAWT, counter.numAbandonedBeforeAWTG);
         }
         else {
            add (numServedBeforeAWT, counter.numServedBeforeAWT);
            add (numAbandonedBeforeAWT, counter.numAbandonedBeforeAWT);
         }
         add (sumWaitingTimesServed, counter.sumWaitingTimesServed);
         add (sumWaitingTimesAbandoned, counter.sumWaitingTimesAbandoned);
         add (servedRates, counter.servedRates);
         if (lastTimeAvg || i < counters.length - 1) {
            add (queueSize, counter.queueSize);
            add (busyAgents, counter.busyAgents);
            add (totalAgents, counter.totalAgents);
         }
         max (maxQueueSize, counter.maxQueueSize);
         max (maxBusyAgents, counter.maxBusyAgents);
      }
      final int l = lastTimeAvg ? counters.length : counters.length - 1;
      div (queueSize, l);
      div (busyAgents, l);
      div (totalAgents, l);
   }

   /**
    * Collects statistics concerning the last transition of the 
    * CTMC \texttt{ctmc} with type \texttt{type} by
    * updating the appropriate counters.
    * @param ctmc the call center CTMC.
    * @param type the transition type.
    */
   public void collectStat (CallCenterCTMC ctmc, TransitionType type) {
      final int tr = ctmc.getNumTransitionsDone () - ctmc.getNumFollowingFalseTransitions ();
      int k, i, kp, n;
      switch (type) {
      case ARRIVALSERVED:
         k = ctmc.getLastSelectedContactType ();
         i = ctmc.getLastSelectedAgentGroup ();
         ++numArrivals[k];
         n = ctmc.getNumContactsInServiceI (i);
         statBusyAgents[i].update (tr, n);
         ++servedRates[k*numGroups + i];
         if (n > maxBusyAgents[i])
            maxBusyAgents[i] = n;
         if (maxBusyAgents.length > 1) {
            final int total = ctmc.getNumContactsInService ();
            if (total > maxBusyAgents[maxBusyAgents.length - 1])
               maxBusyAgents[maxBusyAgents.length - 1] = total;
         }
         break;
      case ARRIVALQUEUED:
         k = ctmc.getLastSelectedContactType ();
         ++numArrivals[k];
         n = ctmc.getNumContactsInQueue (k);
         statQueueSize[k].update (tr, n);
         if (n > maxQueueSize[k])
            maxQueueSize[k] = n;
         if (maxQueueSize.length > 1) {
            final int total = ctmc.getNumContactsInQueue ();
            if (total > maxQueueSize[maxQueueSize.length - 1])
               maxQueueSize[maxQueueSize.length - 1] = total;
         }
         break;
      case ARRIVALBALKED:
         k = ctmc.getLastSelectedContactType ();
         ++numArrivals[k];
         ++numAbandoned[k];
         break;
      case ARRIVALBLOCKED:
         k = ctmc.getLastSelectedContactType ();
         ++numArrivals[k];
         ++numBlocked[k];
         break;
      case ENDSERVICEANDDEQUEUE:
         i = ctmc.getLastSelectedAgentGroup ();
         kp = ctmc.getLastSelectedQueuedContactType ();
         ++servedRates[kp*numGroups + i];
         statQueueSize[kp].update (tr, ctmc.getNumContactsInQueue (kp));
         statBusyAgents[i].update (tr, ctmc.getNumContactsInServiceI (i));
         statTotalAgents[i].update (tr,
               Math.max (ctmc.getNumContactsInServiceI (i), ctmc.getNumAgents (i)));
         break;
      case ENDSERVICENODEQUEUE:
         i = ctmc.getLastSelectedAgentGroup ();
         statBusyAgents[i].update (tr, ctmc.getNumContactsInServiceI (i));
         statTotalAgents[i].update (tr,
               Math.max (ctmc.getNumContactsInServiceI (i), ctmc.getNumAgents (i)));
         break;
      case ABANDONMENT:
         k = ctmc.getLastSelectedContactType ();
         ++numAbandoned[k];
         statQueueSize[k].update (tr, ctmc.getNumContactsInQueue (k));
         break;
      case FALSETRANSITION:
         ++numFalseTransitions;
         break;
      default:
         throw new AssertionError ();
      }
      if (ns > 0)
         collectStatAWT (ctmc, type, tr);
   }

   /**
    * Updates the arrays {@link #queueSize},
    * {@link #busyAgents}, and
    * {@link #totalAgents} for this
    * set of counters from the corresponding
    * accumulates computing time-averages for the
    * queue size, number of busy agents, and total number of
    * agents, respectively.
    * Note that the third quantity changes with time only
    * in a multi-period setup.
    * @param ctmc the call center CTMC.
    */
   public void updateStatOnTime (CallCenterCTMC ctmc) {
      int ntr = ctmc.getNumTransitionsDone ();
      if (queueSize != null) {
         for (int k = 0; k < queueSize.length; k++) {
            statQueueSize[k].update (ntr + 1);
            if (randomHorizon)
               queueSize[k] = statQueueSize[k].sum () / ctmc.getJumpRate ();
            else
               queueSize[k] = statQueueSize[k].average ();
         }
      }
      if (busyAgents != null) {
         for (int i = 0; i < busyAgents.length; i++) {
            statBusyAgents[i].update (ntr + 1);
            if (randomHorizon)
               busyAgents[i] = statBusyAgents[i].sum () / ctmc.getJumpRate ();
            else
               busyAgents[i] = statBusyAgents[i].average ();
         }
      }
      if (totalAgents != null) {
         for (int i = 0; i < totalAgents.length; i++) {
            statTotalAgents[i].update (ntr + 1);
            if (randomHorizon)
               totalAgents[i] = statTotalAgents[i].sum () / ctmc.getJumpRate ();
            else
               totalAgents[i] = statTotalAgents[i].average ();
         }
      }
   }
   
   /**
    * Counts a call of type \texttt{k} as having waited less
    * than the acceptable waiting time, for
    * each user-defined threshold. 
    * @param k the call type.
    * @param slCounter the array of counters.
    */
   private void addToSL (int k, double[] slCounter) {
      final int nt = numTypes > 1 ? numTypes + 1 : numTypes;
      for (int s = 0; s < ns; s++)
         ++slCounter[s * nt + k];
      if (numTypes > 1) {
         for (int s = 0; s < ns; s++)
            ++slCounter[s * nt + numTypes];
      }
   }
   
   /**
    * Approximates and returns the probability that the
    * waiting time is smaller than \texttt{awt} for
    * a call spending \texttt{d1} transitions in the queue
    * during the preceding period, and
    * \texttt{d2} transitions during the period concerned by this
    * set of counters.
    * @param d1 the number of transitions spent in queue during the first period.
    * @param d2 the number of transitions spent in queue during the second period.
    * @param awt the acceptable waiting time.
    * @return the probability that the waiting time is smaller than the AWT.
    */
   private double getProbGood (int d1, int d2, double awt) {
      final int n1 = startingTransition[counterPeriod] - startingTransition[counterPeriod - 1];
      final int n2 = (int)numTransitions;
      final double q1 = jumpRate[counterPeriod - 1];
      final double q2 = jumpRate[counterPeriod];
      
      if (d2 == 0) {
         if (randomHorizon)
            return GammaDist.cdf (d1, q1, 15, awt);
         else
            return 1 - BinomialDist.cdf (n1, awt / m_timeHorizon, d1 - 1);
      }
      MathFunction func;
      if (randomHorizon)
         func = new FuncProbSLRandom (n1, n2, m_timeHorizon, q2, awt, d1, d2);
      else
         func = new FuncProbSLDet (n1, n2, m_timeHorizon, q2, awt, d1, d2);
      return MathFunctionUtil.simpsonIntegral (func, 0, awt, 50);
      
//      if (d1 > d2) {
//         double q = q1;
//         double d2p = d2*q1/q2;
//         int d2p1 = (int)Math.floor (d2p);
//         int d2p2 = (int)Math.ceil (d2p);
//         if (randomHorizon) {
//            double probr1 = GammaDist.cdf (d2p1 + d1, q, 15, awt);
//            double probr2 = GammaDist.cdf (d2p2 + d1, q, 15, awt);
//            return (d2p - d2p1)*(probr2 - probr1) + probr1;
//         }
//         else {
//            double n2p = n2*q1/q2;
//            int n2p1 = (int)Math.floor (n2p);
//            int n2p2 = (int)Math.ceil (n2p);
//            double probd1 = 1 - BinomialDist.cdf (n2p1 + n1, awt / (2*timeHorizon), d2p1 + d1 - 1);
//            double probd2 = 1 - BinomialDist.cdf (n2p2 + n1, awt / (2*timeHorizon), d2p2 + d1 - 1);
//            return (d2p - d2p1)*(probd2 - probd1) + probd1;
//         }
//      }
//      else {
//         double q = q2;
//         double d1p = d1*q2/q1;
//         int d1p1 = (int)Math.floor (d1p);
//         int d1p2 = (int)Math.ceil (d1p);
//         if (randomHorizon) {
//            double probr1 = GammaDist.cdf (d1p1 + d2, q, 15, awt);
//            double probr2 = GammaDist.cdf (d1p2 + d2, q, 15, awt);
//            return (d1p - d1p1)*(probr2 - probr1) + probr1;
//         }
//         else {
//            double n1p = n1*q2/q1;
//            int n1p1 = (int)Math.floor (n1p);
//            int n1p2 = (int)Math.ceil (n1p);
//            double probd1 = 1 - BinomialDist.cdf (n1p1 + n2, awt / (2*timeHorizon), d1p1 + d2 - 1);
//            double probd2 = 1 - BinomialDist.cdf (n1p2 + n2, awt / (2*timeHorizon), d1p2 + d2 - 1);
//            return (d1p - d1p1)*(probd2 - probd1) + probd1;
//         }
//      }
   }
   
   private abstract static class FuncProbSL implements MathFunction {
      private int n1, n2;
      protected double periodDuration;
      private double awt;
      private double q2;
      private int d1, d2;

      FuncProbSL (int n1, int n2, double periodDuration, double q2,
            double awt, int d1, int d2) {
         this.n1 = n1;
         this.n2 = n2;
         this.periodDuration = periodDuration;
         this.q2 = q2;
         this.awt = awt;
         this.d1 = d1;
         this.d2 = d2;
      }
      
      public abstract double getProb (int delta, double q, double s, int n);

      public double evaluate (double w) {
         final double prob = getProb (d2, q2, awt - w, n2);
         final double dens = BetaDist.density (d1, n1 + 1 - d1, w
               / periodDuration);
         return prob * dens / periodDuration;
      }
   }
   
   private static class FuncProbSLRandom extends FuncProbSL {
      public FuncProbSLRandom (int n1, int n2, double periodDuration,
            double q2, double awt, int d1, int d2) {
         super (n1, n2, periodDuration, q2, awt, d1, d2);
      }

      @Override
      public double getProb (int delta, double q, double s, int n) {
         return GammaDist.cdf (delta, q, 15, s);
      }
   }
   
   private static class FuncProbSLDet extends FuncProbSL {
      public FuncProbSLDet (int n1, int n2, double periodDuration, double q2,
            double awt, int d1, int d2) {
         super (n1, n2, periodDuration, q2, awt, d1, d2);
      }

      @Override
      public double getProb (int delta, double q, double s, int n) {
         return 1 - BinomialDist.cdf (n, s / periodDuration,
               delta - 1);
      }
   }

   /**
    * For each value in \texttt{slCounter} corresponding
    * to calls of type \texttt{k}, adds the probability that the call
    * waits less than the acceptable waiting time conditional to
    * the fact that the call spent \texttt{delta} transitions in the queue.
    * The probability for element \texttt{idx} of \texttt{slCounter}
    * is obtained using \texttt{probInAWT[idx]}.
    *  
    * @param ctmc the call center CTMC.
    * @param k the call type index.
    * @param slCounter the array of counters.
    * @param probInAWT the objects for computing probabilities of calls
    * waiting less than a given limit.
    * @param delta the number of transitions spent in queue by the call.
    */
   private void addToSL (CallCenterCTMC ctmc, int k, double[] slCounter, ProbInAWT[] probInAWT, int delta) {
      if (counterPeriod > 0) {
         final int startingTr = ctmc.getNumTransitionsDone () - delta;
         if (startingTr < startingTransition[counterPeriod]) {
            final int d1 = startingTransition[counterPeriod] - startingTr;
            final int d2 = ctmc.getNumTransitionsDone () - startingTransition[counterPeriod];
            final int n1 = startingTransition[counterPeriod] - startingTransition[counterPeriod - 1];
            if (d1 >= n1)
               return;
            final int nt = numTypes > 1 ? numTypes + 1 : numTypes;
            for (int s = 0; s < ns; s++) {
               final int idx = s * nt + k;
               slCounter[idx] += getProbGood (d1, d2, probInAWT[idx].getAWT ()); 
            }
            if (numTypes > 1) {
               for (int s = 0; s < ns; s++) {
                  final int idx = s * nt + numTypes;
                  slCounter[idx] += getProbGood (d1, d2, probInAWT[idx].getAWT ()); 
               }
            }
            return;
         }
      }
      final int nt = numTypes > 1 ? numTypes + 1 : numTypes;
      for (int s = 0; s < ns; s++) {
         final int idx = s * nt + k;
         slCounter[idx] += probInAWT[idx].getProbInAWT (delta);
      }
      if (numTypes > 1) {
         for (int s = 0; s < ns; s++) {
            final int idx = s * nt + numTypes;
            slCounter[idx] += probInAWT[idx].getProbInAWT (delta);
         }
      }
   }
   
   /**
    * Computes and returns the waiting time of a call
    * conditional to the call waiting for \texttt{delta}
    * transitions in the queue.
    * @param ctmc the call center CTMC.
    * @param delta the number of transitions spent in queue by the call.
    * @return the waiting time of the call.
    */
   private double getWT (CallCenterCTMC ctmc, int delta) {
      final double denum = randomHorizon ? ctmc.getJumpRate () : (numTransitions + 1) / m_timeHorizon;
      if (counterPeriod == 0)
         return delta / denum;
      final int startingTr = ctmc.getNumTransitionsDone () - delta;
      if (startingTr >= startingTransition[counterPeriod])
         return delta / denum;
      final int dp = ctmc.getNumTransitionsDone () - startingTransition[counterPeriod];
      double w = dp / denum;
      for (int mp = counterPeriod - 1, d = delta - dp; d > 0; ) {
         final int ntrp = startingTransition[mp + 1] - startingTransition[mp];
         if (startingTr >= startingTransition[mp]) {
            //w += d / jumpRate[mp];
            w += m_timeHorizon*d/(ntrp+1.0);
            d = 0;
         }
         else {
            //final double dp2 = startingTransition[mp + 1] - startingTransition[mp];
            //w += dp2 / jumpRate[mp];
            w += m_timeHorizon;
            d -= ntrp;
         }
      }
      return w;
   }
   
   /**
    * Collects statistics based on the acceptable waiting times.
    */
   private void collectStatAWT (CallCenterCTMC ctmc, TransitionType type, int tr) {
      CallCenterCTMCWithQueues ctmcQ = (CallCenterCTMCWithQueues) ctmc;
      int k, kp, delta;
      switch (type) {
      case ARRIVALSERVED:
         k = ctmc.getLastSelectedContactType ();
         addToSL (k, numServedBeforeAWT);
         if (probInAWTG.length > 0)
            addToSL (k, numServedBeforeAWTG);
         break;
      case ARRIVALBALKED:
         k = ctmc.getLastSelectedContactType ();
         addToSL (k, numAbandonedBeforeAWT);
         if (probInAWTG.length > 0)
            addToSL (k, numAbandonedBeforeAWTG);
         break;
      case ENDSERVICEANDDEQUEUE:
         kp = ctmc.getLastSelectedQueuedContactType ();
         delta = ctmcQ.getLastWaitingTime (kp);
         addToSL (ctmc, kp, numServedBeforeAWT, m_probInAWT, delta);
         if (probInAWTG.length > 0)
            addToSL (ctmc, kp, numServedBeforeAWTG, probInAWTG, delta);
         sumWaitingTimesServed[kp] += getWT (ctmc, delta);
         break;
      case ABANDONMENT:
         k = ctmc.getLastSelectedContactType ();
         delta = ctmcQ.getLastWaitingTime (k);
         addToSL (ctmc, k, numAbandonedBeforeAWT, m_probInAWT, delta);
         if (probInAWTG.length > 0)
            addToSL (ctmc, k, numAbandonedBeforeAWTG, probInAWTG, delta);
         sumWaitingTimesAbandoned[k] += getWT (ctmc, delta);
         break;
      }
   }
   
   public AccumulateWithTimes[] getStatTotalAgents() {
      return statTotalAgents;
   }
   
   public double[] getTotalAgents() {
      return totalAgents;
   }

   /**
    * Returns a copy of this set of counters.
    * This method creates a clone of each internal
    * array in the set of counters.
    */
   public CallCenterCounters clone () {
      CallCenterCounters cpy;
      try {
         cpy = (CallCenterCounters) super.clone ();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      cpy.numArrivals = numArrivals.clone ();
      cpy.numBlocked = numBlocked.clone ();
      cpy.numAbandoned = numAbandoned.clone ();
      cpy.queueSize = queueSize.clone ();
      cpy.busyAgents = busyAgents.clone ();
      cpy.totalAgents = totalAgents.clone ();
      cpy.numServedBeforeAWT = numServedBeforeAWT.clone ();
      cpy.numAbandonedBeforeAWT = numAbandonedBeforeAWT.clone ();
      cpy.numServedBeforeAWTG = numServedBeforeAWTG.clone ();
      cpy.numAbandonedBeforeAWTG = numAbandonedBeforeAWTG.clone ();
      cpy.servedRates = servedRates.clone ();
      cpy.sumWaitingTimesAbandoned = sumWaitingTimesAbandoned.clone ();
      cpy.sumWaitingTimesServed = sumWaitingTimesServed.clone ();
      cpy.statQueueSize = statQueueSize.clone ();
      for (int k = 0; k < statQueueSize.length; k++)
         cpy.statQueueSize[k] = statQueueSize[k].clone ();
      cpy.statBusyAgents = statBusyAgents.clone ();
      for (int i = 0; i < statBusyAgents.length; i++)
         cpy.statBusyAgents[i] = statBusyAgents[i].clone ();
      cpy.statTotalAgents = statTotalAgents.clone ();
      for (int i = 0; i < statTotalAgents.length; i++)
         cpy.statTotalAgents[i] = statTotalAgents[i].clone ();
      cpy.maxBusyAgents = maxBusyAgents.clone ();
      cpy.maxQueueSize = maxQueueSize.clone ();
      return cpy;
   }

   /**
    * Returns a string giving the number of counted
    * arrivals, abandoned calls, and
    * calls waiting less than the acceptable waiting time.
    */
   public String toString () {
      double arv = 0;
      for (int k = 0; k < numArrivals.length; k++)
         arv += numArrivals[k];
      double ab = 0;
      for (int k = 0; k < numAbandoned.length; k++)
         ab += numAbandoned[k];
      if (m_probInAWT == null)
         return String.format ("{%.0f arrivals, %.0f abandoned contacts}", arv,
               ab);

      int nt = numServedBeforeAWT.length / ns;
      double awt = m_probInAWT[nt - 1].getAWT ();
      double numGoodSL = numServedBeforeAWT[nt - 1]
            + numAbandonedBeforeAWT[nt - 1];
      return String
            .format (
                  "{%.0f arrivals, %.0f abandoned contacts, %.0f contacts waiting less than %f}",
                  arv, ab, numGoodSL, awt);
   }
}
