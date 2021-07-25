package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

import umontreal.ssj.probdist.GeometricDist;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * CTMC model of a call center with multiple call types and agent groups.
 */
public class CallCenterCTMCKI implements CallCenterCTMC {
   // Number of random bits used to generate the number of
   // successive self jumps preceding any main transition
   private static final int NUMBITSSF = 10;
   // Simple agent group and waiting queue selectors adapted
   // for a single contact type, and a single agent group, respectively
   private static final AgentGroupSelector agsSimple = new SimpleGroupSelector ();
   private static final WaitingQueueSelector wqsSimple = new SimpleQueueSelector ();

   private int numTypes;
   private int numGroups;
   // Parameters specific to contact types
   // Arrival rate
   private double[] lambda;
   private double[] lambdaBound;
   // Balking probability
   private double[] rho;
   // Patience rate
   private double[] nu;
   private double[] nuBound;
   // Maximal patience rate
   private double maxNuBound;

   // Parameters specific to agent groups
   private int[] numAgents;
   private int[] maxNumAgents;
   private int totalNumAgents, totalMaxNumAgents;
   private int[][] thresholds;

   // Service time parameters
   // mu[numGroups*k + i] gives the mu_{k,i}
   private double[] mu;
   private double[] muBound;
   // For each agent group, maximal service rate among
   // all contact types
   private double[] maxMu;
   private double[] maxMuBound;

   // Routing parameters
   private AgentGroupSelector[] ags;
   private WaitingQueueSelector[] wqs;

   private int queueCapacity;
   private int maxQueueCapacity;

   private double jumpRate;
   // Total or maximal arrival, abandonment and service rates
   private double sumLambda, sumLambdaBound;

   // State variables
   // Number of contacts waiting in queue, for each
   // contact type
   private int[] numContactsInQueue;
   private int totalNumContactsInQueue;
   // numContactsInService[numGroups*k + i]
   // gives the number of contacts of type k
   // in service by agents in group i.
   // This array may be null if this detailed
   // information is not needed.
   private int[] numContactsInService;
   private int totalNumContactsInService;
   private int[] totalNumContactsInServiceI, totalNumContactsInServiceK;

   private int qidx;
   private int m_np;
   private int m_nf;
   private int tntr;
   private StateThresh stateThresh;
   private CCEvent[] lookups;
   private int lastK, lastI, lastKp, lastPos;
   private int ntr;
   private TransitionType lastType;

   public CallCenterCTMCKI (double[] lambda,
         double[] lambdaBound,
         double[][] mu, double[][] muBound,
         int[] numAgents,
         int[] maxNumAgents, double[] rho, double[] nu,
         double[] nuBound,
         int queueCapacity, int maxQueueCapacity,
         AgentGroupSelector[] ags, WaitingQueueSelector[] wqs,
         boolean needsInServiceContactMix, int[][] thresholds) {
      if (queueCapacity < 0)
         throw new IllegalArgumentException ("capacity < 0");
      numTypes = lambda.length;
      numGroups = numAgents.length;
      this.lambda = lambda.clone ();
      this.lambdaBound = new double[lambda.length];
      for (int k = 0; k < lambda.length; k++)
         this.lambdaBound[k] = Math.max (lambda[k], lambdaBound[k]);
      this.mu = new double[numTypes * numGroups];
      this.muBound = new double[this.mu.length];
      for (int k = 0; k < numTypes; k++) {
         System.arraycopy (mu[k], 0, this.mu, k * numGroups, numGroups);
         for (int i = 0; i < numGroups; i++) {
            final int idx = numGroups*k + i;
            this.muBound[idx] = Math.max (mu[k][i], muBound[k][i]);
            assert this.mu[idx] == mu[k][i];
         }
      }
      this.numAgents = numAgents.clone ();
      this.maxNumAgents = new int[numAgents.length];
      for (int i = 0; i < numAgents.length; i++)
         this.maxNumAgents[i] = Math.max (numAgents[i], maxNumAgents[i]);
      this.rho = rho.clone ();
      this.nu = nu.clone ();
      this.nuBound = new double[nu.length];
      for (int k = 0; k < nu.length; k++)
         this.nuBound[k] = Math.max (nu[k], nuBound[k]);
      this.queueCapacity = queueCapacity;
      this.maxQueueCapacity = Math.max (queueCapacity, maxQueueCapacity);
      this.ags = ags;
      this.wqs = wqs;
      initStateVars (needsInServiceContactMix);
      this.thresholds = thresholds;
      initRates ();
   }

   private void initStateVars (boolean needsInServiceContactMix) {
      totalNumContactsInServiceI = new int[numGroups];
      numContactsInQueue = new int[numTypes];

      if (numTypes > 1) {
         boolean sameMuForGroups = true;
         if (!needsInServiceContactMix) {
            for (int i = 0; i < numGroups && sameMuForGroups; i++) {
               double muk0 = Double.NaN;
               for (int k = 1; k < numTypes && sameMuForGroups; k++) {
                  final double muki = mu[k * numGroups + i];
                  final double mukiBound = muBound[k * numGroups + i];
                  if (Math.abs (muki - mukiBound) > 1e-10) {
                     sameMuForGroups = false;
                     continue;
                  }
                  if (muki == 0)
                     continue;
                  if (Double.isNaN (muk0))
                     muk0 = muki;
                  else if (Math.abs (muki - muk0) > 1e-10)
                     sameMuForGroups = false;
               }
            }
         }

         if (needsInServiceContactMix || !sameMuForGroups) {
            numContactsInService = new int[numTypes * numGroups];
            totalNumContactsInServiceK = new int[numTypes];
         }
      }
   }

   private void initRates () {
      double minRate = Double.POSITIVE_INFINITY;
      maxNuBound = 0;
      sumLambda = 0;
      sumLambdaBound = 0;
      for (int k = 0; k < numTypes; k++) {
         if (lambdaBound[k] * rho[k] < minRate)
            minRate = lambdaBound[k] * rho[k];
         if (nuBound[k] < minRate)
            minRate = nuBound[k];
         sumLambda += lambda[k];
         sumLambdaBound += lambdaBound[k];
         if (nuBound[k] > maxNuBound)
            maxNuBound = nuBound[k];
      }
      double sumNuBound = maxQueueCapacity * maxNuBound;
      maxMu = new double[numGroups];
      maxMuBound = new double[numGroups];

      double sumMuBound = 0;
      totalNumAgents = 0;
      totalMaxNumAgents = 0;
      for (int i = 0; i < numGroups; i++) {
         maxMuBound[i] = 0;
         for (int k = 0; k < numTypes; k++) {
            final int idx = k * numGroups + i;
            final double muki = mu[idx];
            if (muki > maxMu[i])
               maxMu[i] = muki;
            final double mukiBound = muBound[idx];
            if (mukiBound > maxMuBound[i])
               maxMuBound[i] = mukiBound;
            if (mukiBound < minRate)
               minRate = mukiBound;
         }
         sumMuBound += maxNumAgents[i] * maxMuBound[i];
         totalNumAgents += numAgents[i];
         totalMaxNumAgents += maxNumAgents[i];
      }

      jumpRate = sumLambdaBound + sumMuBound + sumNuBound;
      minRate /= jumpRate;

      int power = 1;
      int numIntervals = 2;
      while (1.0 / numIntervals > minRate && power < 8) {
         ++power;
         numIntervals *= 2;
      }

      stateThresh = new StateThresh (this, thresholds);
      // stateThresh = new QueueSizeThresh (jumpRate, maxNu, queueCapacity,
      // numStateThresh);
      lookups = new CCEvent[stateThresh.getNumVectorsOfThresholds ()];

      for (int j = 0; j < lookups.length; j++) {
         // if (stateThresh.getNumFalseTrDist (j).getP () == 1)
         // lookups[j] = createEvent (0, 1, numIntervals, 31, j);
         // else {
         lookups[j] = createEvent (0, 1, numIntervals, 31 - NUMBITSSF, j);
         lookups[j] = createEventF (stateThresh, 0, 1, 16, NUMBITSSF, j,
               lookups[j]);
         // }
      }
   }

   protected static CCEvent createEventF (StateThresh stateThresh, double minU,
         double maxU, int size, int maxBits, int lqidx, CCEvent lookupEvent) {
      GeometricDist numFalseTrQ = stateThresh.getNumFalseTrDist (lqidx);
      double[] prob;
      if (numFalseTrQ.getP () == 1)
         prob = new double[] { 1 };
      else {
         int j = 0;
         while (numFalseTrQ.prob (j) > 1e-10)
            ++j;
         prob = new double[j];
         for (int i = 0; i < prob.length; i++)
            prob[i] = numFalseTrQ.prob (i);
      }
      CCEventFactory[] factories = new CCEventFactory[prob.length];
      for (int i = 0; i < prob.length; i++)
         factories[i] = new FalseTransitionEventFactory (i, lookupEvent);
      return LookupEvent.createIndex (prob, factories, size, maxBits);
   }

   protected CCEvent createEvent (double minU, double maxU, int size,
         int maxBits, int lqidx) {
      //double[] prob = new double[2 * lambda.length + maxMu.length + 1];
      double[] prob = new double[lambdaBound.length + maxMuBound.length + 1];
      CCEventFactory[] factories = new CCEventFactory[prob.length];
      double jumpRateQ = stateThresh.getJumpRate (lqidx);
      double sum = 0;
      for (int k = 0; k < lambda.length; k++) {
//         final AgentGroupSelector agsK = numGroups == 1 ? agsSimple
//               : ags[k];
//         prob[2 * k] = lambda[k] * rho[k] / jumpRateQ;
//         prob[2 * k + 1] = lambda[k] * (1 - rho[k]) / jumpRateQ;
//         factories[2 * k] = new ArrivalEventFactory (k, true, agsK);
//         factories[2 * k + 1] = new ArrivalEventFactory (k, false, agsK);
         prob[k] = lambdaBound[k] / jumpRateQ;
         factories[k] = new ArrivalEventFactory2 (k, lqidx, sum / jumpRateQ);
         sum += lambdaBound[k];
      }
      for (int i = 0; i < maxMuBound.length; i++) {
         final double srate = maxMuBound[i]
               * stateThresh.getThreshNumAgents (lqidx, i);
         prob[lambda.length + i] = srate / jumpRateQ;
         factories[lambda.length + i] = new EndServiceEventFactory (i,
               lqidx, sum / jumpRateQ);
         sum += srate;
      }
      final double arate = maxNuBound * stateThresh.getThreshQueueSize (lqidx);
      prob[prob.length - 1] = arate / jumpRateQ;
      factories[factories.length - 1] = new AbandonmentEventFactory (lqidx, sum
            / jumpRateQ);
      return LookupEvent.createIndex (prob, factories, size, maxBits);
   }

   private void addQueuedContact (int k) {
      ++totalNumContactsInQueue;
      ++numContactsInQueue[k];
      // qidx = stateThresh.updateQIdx (qidx, totalNumContactsInQueue);
   }

   private void removeQueuedContact (int k) {
      --totalNumContactsInQueue;
      --numContactsInQueue[k];
      // qidx = stateThresh.updateQIdx (qidx, totalNumContactsInQueue);
   }

   private void addServedContact (int k, int i) {
      ++totalNumContactsInService;
      if (numContactsInService != null) {
         final int kidx = k * numGroups + i;
         ++numContactsInService[kidx];
         ++totalNumContactsInServiceK[k];
      }
      ++totalNumContactsInServiceI[i];
   }

   private void removeServedContact (int k, int i) {
      --totalNumContactsInService;
      if (numContactsInService != null) {
         final int kidx = k * numGroups + i;
         --numContactsInService[kidx];
         --totalNumContactsInServiceK[k];
      }
      --totalNumContactsInServiceI[i];
   }

   private void addQueuedContactC (int k) {
      if (totalNumContactsInQueue >= queueCapacity)
         throw new IllegalStateException ();
      addQueuedContact (k);
   }

   private void removeQueuedContactC (int k) {
      if (totalNumContactsInQueue == 0)
         throw new IllegalStateException ();
      if (numContactsInQueue[k] == 0)
         throw new IllegalStateException ();
      removeQueuedContact (k);
   }

   private void addServedContactC (int k, int i) {
      if (totalNumContactsInServiceI[i] >= numAgents[i])
         throw new IllegalStateException ();
      addServedContact (k, i);
   }

   private void removeServedContactC (int k, int i) {
      if (totalNumContactsInServiceI[i] == 0)
         throw new IllegalStateException ();
      if (numContactsInService != null) {
         final int kidx = k * numGroups + i;
         if (numContactsInService[kidx] == 0)
            throw new IllegalStateException ();
      }
      removeServedContact (k, i);
   }

   private static class FalseTransitionsEvent implements CCEvent {
      private int npe;
      private CCEvent ev;
      private int maxBits;

      public FalseTransitionsEvent (int npe, CCEvent ev, int maxBits) {
         this.npe = npe;
         this.ev = ev;
         this.maxBits = maxBits;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int trAfter = tr + npe + 1;
         ctmc.m_np = npe;
         if (trAfter >= ctmc.tntr) {
            final int diff = trAfter - ctmc.tntr;
            if (diff > 0) {
               ctmc.m_np -= diff;
               return TransitionType.FALSETRANSITION;
            }
         }
         return ev.actions (ctmc, tr, rv, usedBits + maxBits, changeState);
      }
   }

   private static class FalseTransitionEventFactory implements CCEventFactory {
      private CCEvent[] evs = new CCEvent[31];
      private int npe;
      private CCEvent ev;

      public FalseTransitionEventFactory (int npe, CCEvent ev) {
         this.npe = npe;
         this.ev = ev;
      }

      public CCEvent newInstance (double u1, double u2, int maxBits) {
         if (maxBits >= evs.length) {
            CCEvent[] newEv = new CCEvent[evs.length * 2 + 1];
            System.arraycopy (evs, 0, newEv, 0, evs.length);
            evs = newEv;
         }
         if (evs[maxBits] == null)
            return evs[maxBits] = new FalseTransitionsEvent (npe, ev, maxBits);
         return evs[maxBits];
      }
   }
   
   private static class ArrivalEvent2 extends EventWithTest {
      private int k;
      private AgentGroupSelector agsK;
      private double jumpRateQ;
      
      public ArrivalEvent2 (double minU, double maxU, int numBits, double jumpRateQ, int k, AgentGroupSelector agsK) {
         super (minU, maxU, numBits);
         this.k = k;
         this.jumpRateQ = jumpRateQ;
         this.agsK = agsK;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final double lambdaNorm = ctmc.lambda[k] / jumpRateQ;
         if (getMinU() >= lambdaNorm)
            return TransitionType.FALSETRANSITION;
         double u = -1;
         if (getMaxU() >= lambdaNorm) {
            u = getU (rv, usedBits);
            if (u >= lambdaNorm)
               return TransitionType.FALSETRANSITION;
         }
         ctmc.lastK = k;
         final int i = agsK.selectAgentGroup (ctmc, tr);
         if (i >= 0) {
            // Some agents are free
            if (changeState)
               ctmc.addServedContact (k, i);
            ctmc.lastI = i;
            return TransitionType.ARRIVALSERVED;
         }
         
         boolean balking;
         double rho = ctmc.rho[k];
         if (rho == 0)
            balking = false;
         else if (rho == 1)
            balking = true;
         else {
            final double lambdaRhoNorm = lambdaNorm * rho;
            if (u >= 0)
               balking = u < lambdaRhoNorm;
            else {
               if (getMinU() >= lambdaRhoNorm)
                  balking = false;
               else if (getMaxU() >= lambdaRhoNorm) {
                  u = getU (rv, usedBits);
                  balking = u < lambdaRhoNorm;
               }
               else
                  balking = true;
            }
         }
         
         if (ctmc.totalNumContactsInQueue < ctmc.queueCapacity) {
            // Contact queued
            if (balking)
               return TransitionType.ARRIVALBALKED;
            else {
               if (changeState)
                  ctmc.addQueuedContact (k);
               return TransitionType.ARRIVALQUEUED;
            }
         }
         else
            // Contact blocked because the queue is full
            return TransitionType.ARRIVALBLOCKED;
      }
   }

//   private static class ArrivalEvent implements CCEvent {
//      protected int k;
//      protected AgentGroupSelector agsK;
//
//      public ArrivalEvent (int k, AgentGroupSelector agsK) {
//         this.k = k;
//         this.agsK = agsK;
//      }
//
//      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
//            int usedBits, boolean changeState) {
//         ctmc.lastK = k;
//         final int i = agsK.selectAgentGroup (ctmc, tr);
//         if (i >= 0) {
//            // Some agents are free
//            if (changeState)
//               ctmc.addServedContact (k, i);
//            ctmc.lastI = i;
//            return TransitionType.ARRIVALSERVED;
//         }
//         else if (ctmc.totalNumContactsInQueue < ctmc.queueCapacity) {
//            // Contact queued
//            if (changeState)
//               ctmc.addQueuedContact (k);
//            return TransitionType.ARRIVALQUEUED;
//         }
//         else
//            // Contact blocked because the queue is full
//            return TransitionType.ARRIVALBLOCKED;
//      }
//   }
//
//   private static class ArrivalEventWithBalking extends ArrivalEvent {
//      public ArrivalEventWithBalking (int k, AgentGroupSelector agsK) {
//         super (k, agsK);
//      }
//
//      @Override
//      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
//            int usedBits, boolean changeState) {
//         ctmc.lastK = k;
//         final int i = agsK.selectAgentGroup (ctmc, tr);
//         if (i >= 0) {
//            // Some agents are free
//            if (changeState)
//               ctmc.addServedContact (k, i);
//            ctmc.lastI = i;
//            return TransitionType.ARRIVALSERVED;
//         }
//         else if (ctmc.totalNumContactsInQueue < ctmc.queueCapacity)
//            // Contact queued
//            return TransitionType.ARRIVALBALKED;
//         else
//            // Contact blocked because the queue is full
//            return TransitionType.ARRIVALBLOCKED;
//      }
//   }
//
//   @SuppressWarnings("unused")
//   private static class ArrivalEventFactory implements CCEventFactory {
//      private CCEvent ev;
//
//      public ArrivalEventFactory (int k, boolean balking,
//            AgentGroupSelector agsK) {
//         if (balking)
//            ev = new ArrivalEventWithBalking (k, agsK);
//         else
//            ev = new ArrivalEvent (k, agsK);
//      }
//
//      public CCEvent newInstance (double u1, double u2, int maxBits) {
//         return ev;
//      }
//   }
   
   private class ArrivalEventFactory2 implements CCEventFactory {
      private int k;
      private int lqidx;
      private double sum;
      
      public ArrivalEventFactory2 (int k, int lqidx, double sum) {
         this.k = k;
         this.lqidx = lqidx;
         this.sum = sum;
      }

      public CCEvent newInstance (double u1, double u2, int maxExtraBits) {
         final double jumpRateQ = stateThresh.getJumpRate (lqidx);
         return new ArrivalEvent2 (u1 - sum, u2 - sum, maxExtraBits,
               jumpRateQ,
               k, numGroups == 1 ? agsSimple : ags[k]);
      }
   }

   private static class AbandonmentEvent extends EventWithSelection {
      private double jumpRateQ;

      public AbandonmentEvent (double minU, double maxU, int numBits,
            int numTypes, double maxNuBound, double jumpRateQ) {
         super (minU, maxU, numBits, numTypes, maxNuBound / jumpRateQ);
         this.jumpRateQ = jumpRateQ;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int k = selectType (ctmc, tr, rv, usedBits);
         if (k == getNumTypes ())
            return TransitionType.FALSETRANSITION;

         ctmc.lastK = k;
         ctmc.lastPos = getLastSelectedEvent ();
         if (changeState)
            ctmc.removeQueuedContact (k);
         return TransitionType.ABANDONMENT;
      }

      @Override
      public int getNumValues (CallCenterCTMCKI ctmc) {
         return ctmc.totalNumContactsInQueue;
      }

      @Override
      public int getNumValues (CallCenterCTMCKI ctmc, int k) {
         return ctmc.numContactsInQueue[k];
      }

      @Override
      public double getWeight (CallCenterCTMCKI ctmc, int k) {
         return ctmc.nu[k] / jumpRateQ;
      }

      @Override
      public double getMaxWeight (CallCenterCTMCKI ctmc, int k) {
         return ctmc.nuBound[k] / jumpRateQ;
      }
   }

   private static class AbandonmentEventSingleType extends EventWithTest {
      private double jumpRateQ;
      private double maxWeight;
      
      public AbandonmentEventSingleType (double minU, double maxU, int numBits, double jumpRateQ, double maxWeight) {
         super (minU, maxU, numBits);
         this.jumpRateQ = jumpRateQ;
         this.maxWeight = maxWeight;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int maxS = ctmc.totalNumContactsInQueue;
         assert ctmc.nu.length == 1;
         final double weight = ctmc.nu[0] / jumpRateQ;
         final int kpos = getPosition (rv, usedBits, weight, maxWeight, maxS);
         if (kpos >= maxS)
            return TransitionType.FALSETRANSITION;

         ctmc.lastPos = kpos;
         ctmc.lastK = 0;
         if (changeState)
            ctmc.removeQueuedContact (0);
         return TransitionType.ABANDONMENT;
      }
   }

   private class AbandonmentEventFactory implements CCEventFactory {
      private int lqidx;
      private double sum;

      public AbandonmentEventFactory (int lqidx, double sum) {
         this.lqidx = lqidx;
         this.sum = sum;
      }

      public CCEvent newInstance (double u1, double u2, int maxBits) {
         final double jumpRateQ = stateThresh.getJumpRate (lqidx);
         if (numTypes == 1) {
            final double nuBoundNorm = nuBound[0] / jumpRateQ;
            return new AbandonmentEventSingleType (u1 - sum,
                  u2 - sum, maxBits, jumpRateQ, nuBoundNorm);
         }
         else
            return new AbandonmentEvent (u1 - sum, u2 - sum, maxBits, numTypes,
                  maxNuBound, jumpRateQ);
      }
   }

   private static class EndServiceEvent extends EventWithSelection {
      private int i;
      private WaitingQueueSelector wqsI;
      private double jumpRateQ;

      public EndServiceEvent (int i, double minU, double maxU, int numBits,
            int numTypes, double maxWeight, double jumpRateQ,
            WaitingQueueSelector wqsI) {
         super (minU, maxU, numBits, numTypes, maxWeight);
         this.i = i;
         this.wqsI = wqsI;
         this.jumpRateQ = jumpRateQ;
      }

      @Override
      public int getNumValues (CallCenterCTMCKI ctmc) {
         return ctmc.totalNumContactsInServiceI[i];
      }

      @Override
      public int getNumValues (CallCenterCTMCKI ctmc, int k) {
         final int kidx = ctmc.numGroups * k + i;
         return ctmc.numContactsInService[kidx];
      }

      @Override
      public double getWeight (CallCenterCTMCKI ctmc, int k) {
         final int kidx = ctmc.numGroups * k + i;
         return ctmc.mu[kidx] / jumpRateQ;
      }

      @Override
      public double getMaxWeight (CallCenterCTMCKI ctmc, int k) {
         final int kidx = ctmc.numGroups * k + i;
         return ctmc.muBound[kidx] / jumpRateQ;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int k = selectType (ctmc, tr, rv, usedBits);
         if (k == getNumTypes ())
            return TransitionType.FALSETRANSITION;
         ctmc.lastI = i;

         ctmc.lastK = k;
         ctmc.lastKp = -1;

         if (ctmc.totalNumContactsInServiceI[i] - 1 < ctmc.numAgents[i]) {
            if (changeState)
               ctmc.removeServedContact (k, i);
            final int kp = wqsI.selectWaitingQueue (ctmc, k, tr);
            if (kp >= 0) {
               ctmc.lastKp = kp;
               ctmc.lastPos = 0;
               if (changeState) {
                  ctmc.removeQueuedContact (kp);
                  ctmc.addServedContact (kp, i);
               }
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else
               return TransitionType.ENDSERVICENODEQUEUE;
         }
         else {
            if (changeState)
               ctmc.removeServedContact (k, i);
            return TransitionType.ENDSERVICENODEQUEUE;
         }
      }
   }

   private static class EndServiceEventSameMu extends EventWithTest {
      private int i;
      private WaitingQueueSelector wqsI;
      private double jumpRateQ, maxWeight;

      public EndServiceEventSameMu (int i, double minU, double maxU,
            int numBits, WaitingQueueSelector wqsI, double jumpRateQ, double maxWeight) {
         super (minU, maxU, numBits);
         this.i = i;
         this.wqsI = wqsI;
         this.jumpRateQ = jumpRateQ;
         this.maxWeight = maxWeight;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int maxS = ctmc.totalNumContactsInServiceI[i];
         final double weight = ctmc.maxMu[i] / jumpRateQ;
         final int kpos = getPosition (rv, usedBits, weight, maxWeight, maxS);
         if (kpos >= maxS)
            return TransitionType.FALSETRANSITION;

         ctmc.lastI = i;
         ctmc.lastK = -1;
         ctmc.lastKp = -1;

         if (ctmc.totalNumContactsInServiceI[i] - 1 < ctmc.numAgents[i]) {
            if (changeState)
               ctmc.removeServedContact (-1, i);
            final int kp = wqsI.selectWaitingQueue (ctmc, -1, tr);
            if (kp >= 0) {
               ctmc.lastKp = kp;
               ctmc.lastPos = 0;
               if (changeState) {
                  ctmc.removeQueuedContact (kp);
                  ctmc.addServedContact (kp, i);
               }
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else
               return TransitionType.ENDSERVICENODEQUEUE;
         }
         else {
            if (changeState)
               ctmc.removeServedContact (-1, i);
            return TransitionType.ENDSERVICENODEQUEUE;
         }
      }
   }

   private static class EndServiceEventSingleType extends EventWithTest {
      private int i;
      private double jumpRateQ, maxWeight;

      public EndServiceEventSingleType (int i, double minU, double maxU,
            int numBits, double jumpRateQ, double maxWeight) {
         super (minU, maxU, numBits);
         this.i = i;
         this.jumpRateQ = jumpRateQ;
         this.maxWeight = maxWeight;
      }

      public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv,
            int usedBits, boolean changeState) {
         final int maxS = ctmc.totalNumContactsInServiceI[i];
         final double weight = ctmc.mu[i] / jumpRateQ;
         final int kpos = getPosition (rv, usedBits, weight, maxWeight, maxS);
         if (kpos >= maxS)
            return TransitionType.FALSETRANSITION;

         ctmc.lastI = i;
         ctmc.lastK = 0;
         ctmc.lastKp = -1;

         if (ctmc.totalNumContactsInServiceI[i] - 1 < ctmc.numAgents[i]) {
            if (changeState)
               ctmc.removeServedContact (0, i);
            final int kp = wqsSimple.selectWaitingQueue (ctmc, 0, tr);
            if (kp >= 0) {
               ctmc.lastKp = kp;
               ctmc.lastPos = 0;
               if (changeState) {
                  ctmc.removeQueuedContact (kp);
                  ctmc.addServedContact (kp, i);
               }
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else
               return TransitionType.ENDSERVICENODEQUEUE;
         }
         else {
            if (changeState)
               ctmc.removeServedContact (0, i);
            return TransitionType.ENDSERVICENODEQUEUE;
         }
      }
   }

   private class EndServiceEventFactory implements CCEventFactory {
      private int i;
      private int lqidx;
      private double sum;

      public EndServiceEventFactory (int i, int lqidx, double sum) {
         this.i = i;
         this.lqidx = lqidx;
         this.sum = sum;
      }

      public CCEvent newInstance (double u1, double u2, int maxBits) {
         final double jumpRateQ = stateThresh.getJumpRate (lqidx);
         final double muBoundNorm = maxMuBound[i] / jumpRateQ;
         if (numTypes == 1)
            return new EndServiceEventSingleType (i, u1 - sum,
                  u2 - sum, maxBits, jumpRateQ, muBoundNorm);
         if (numContactsInService == null) {
            return new EndServiceEventSameMu (i, u1 - sum,
                  u2 - sum, maxBits, wqs[i], jumpRateQ, muBoundNorm);
         }
         return new EndServiceEvent (i, u1 - sum, u2 - sum, maxBits, numTypes,
               muBoundNorm, jumpRateQ, wqs[i]);
      }
   }

   public int getNumAgentGroups () {
      return numGroups;
   }

   public int getNumContactsInQueue () {
      return totalNumContactsInQueue;
   }

   public int getNumContactsInQueue (int k) {
      // if (numContactsInQueue == null) {
      // if (numTypes > 1)
      // throw new UnsupportedOperationException();
      // if (k != 0)
      // throw new IllegalArgumentException();
      // return totalNumContactsInQueue;
      // }
      return numContactsInQueue[k];
   }

   public int getNumContactTypes () {
      return numTypes;
   }

   public double getJumpRate () {
      return jumpRate;
   }

   public int getQueueCapacity () {
      return queueCapacity;
   }

   public void setQueueCapacity (int q) {
      if (q < totalNumContactsInQueue)
         throw new IllegalArgumentException ("Queue capacity too small");
      if (q > maxQueueCapacity)
         throw new IllegalArgumentException ("Queue capacity too large");
      queueCapacity = q;
   }
   
   public int getMaxQueueCapacity() {
      return maxQueueCapacity;
   }
   
   public void setMaxQueueCapacity (int q) {
      if (q < queueCapacity)
         throw new IllegalArgumentException ("Maximal queue capacity too small");
      this.maxQueueCapacity = q;
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }
   
   public double getArrivalRate (int k) {
      return lambda[k];
   }

   public void setArrivalRate (int k, double rate) {
      if (rate < 0)
         throw new IllegalArgumentException ();
      if (rate > lambdaBound[k])
         throw new IllegalArgumentException ();
      sumLambda += rate - lambda[k];
      lambda[k] = rate;
   }
   
   public void setArrivalRates (double[] rates) {
      if (rates.length != lambda.length)
         throw new IllegalArgumentException ();
      for (int k = 0; k < rates.length; k++) {
         if (rates[k] < 0)
            throw new IllegalArgumentException ();
         if (rates[k] > lambdaBound[k])
            throw new IllegalArgumentException ();
      }
      System.arraycopy (rates, 0, lambda, 0, rates.length);
      sumLambda = 0;
      for (int k = 0; k < lambda.length; k++)
         sumLambda += lambda[k];
   }

   public double getArrivalRate () {
      return sumLambda;
   }

   public double getMaxArrivalRate (int k) {
      return lambdaBound[k];
   }

   public void setMaxArrivalRate (int k, double rate) {
      if (rate < lambda[k])
         throw new IllegalArgumentException ();
      lambdaBound[k] = rate;
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }
   
   public void setMaxArrivalRates (double[] rates) {
      if (rates.length != lambda.length)
         throw new IllegalArgumentException ();
      for (int k = 0; k < rates.length; k++) {
         if (rates[k] < lambda[k])
            throw new IllegalArgumentException ();
      }
      System.arraycopy (rates, 0, lambdaBound, 0, rates.length);
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }

   public double getMaxArrivalRate () {
      return sumLambdaBound;
   }
   
   public TransitionType getLastTransitionType() {
      return lastType;
   }

   public int getLastSelectedAgentGroup () {
      return lastI;
   }

   public int getLastSelectedContactType () {
      return lastK;
   }

   public int getLastSelectedContact () {
      return lastPos;
   }

   public int getLastSelectedQueuedContactType () {
      return lastKp;
   }

   public int getNumAgents () {
      return totalNumAgents;
   }

   public int getNumAgents (int i) {
      return numAgents[i];
   }

   public int getMaxNumAgents () {
      return totalMaxNumAgents;
   }

   public int getMaxNumAgents (int i) {
      return maxNumAgents[i];
   }

   public void setNumAgents (int i, int n) {
      if (n < 0 || n > maxNumAgents[i])
         throw new IllegalArgumentException (
               "The number of agents must be non-negative, and not greater than "
                     + maxNumAgents[i]);
      totalNumAgents += n - numAgents[i];
      numAgents[i] = n;
   }

   public int[] getMaxNumAgentsArray () {
      return maxNumAgents.clone ();
   }

   public int[] getNumAgentsArray () {
      return numAgents.clone ();
   }

   public void setMaxNumAgents (int i, int n) {
      if (n == maxNumAgents[i])
         return;
      if (n < numAgents[i])
         throw new IllegalArgumentException (
               "The maximal number of agents must not be smaller than"
                     + " the current number of agents");
      maxNumAgents[i] = n;
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }

   public void setMaxNumAgents (int[] maxNumAgents) {
      if (maxNumAgents.length != numGroups)
         throw new IllegalArgumentException (
               "The length of the numAgents array must be equal to the number of agent groups");
      boolean change = false;
      for (int i = 0; i < maxNumAgents.length; i++) {
         if (maxNumAgents[i] != this.maxNumAgents[i]) {
            change = true;
            if (maxNumAgents[i] < numAgents[i])
               throw new IllegalArgumentException (
                     "The maximal number of agents must not be smaller than"
                           + " the current number of agents");
         }
      }
      if (change) {
         System.arraycopy (maxNumAgents, 0, this.maxNumAgents, 0,
               maxNumAgents.length);
         initRates ();
         stateThresh.initOperatingMode (this);
         qidx = stateThresh.getOperatingMode ();
      }
   }

   public void setNumAgents (int[] numAgents) {
      if (numAgents.length != numGroups)
         throw new IllegalArgumentException (
               "The length of the numAgents array must be equal to the number of agent groups");
      int diff = 0;
      for (int i = 0; i < numAgents.length; i++) {
         if (numAgents[i] < 0 || numAgents[i] > maxNumAgents[i])
            throw new IllegalArgumentException (
                  "The number of agents in group " + i
                        + " must be non-negative, and not greater than "
                        + maxNumAgents[i]);
         diff += numAgents[i] - this.numAgents[i];
      }
      System.arraycopy (numAgents, 0, this.numAgents, 0, numAgents.length);
      totalNumAgents += diff;
   }

   public int getNumStateThresh () {
      return stateThresh.getNumVectorsOfThresholds ();
   }

   public StateThresh getStateThresh () {
      return stateThresh;
   }

   public int[][] getStateThresholds () {
      int[][] thresh = new int[stateThresh.getNumVectorsOfThresholds ()][getNumAgentGroups () + 1];
      for (int r = 0; r < thresh.length; r++) {
         for (int i = 0; i < getNumAgentGroups (); i++)
            thresh[r][i] = stateThresh.getThreshNumAgents (r, i);
         thresh[r][getNumAgentGroups ()] = stateThresh.getThreshQueueSize (r);
      }
      return thresh;
   }

   public void setStateThresholds (int[][] thresholds) {
      if (thresholds != null && thresholds.length > 0) {
         ArrayUtil.checkRectangularMatrix (thresholds);
         if (thresholds[0].length != getNumAgentGroups () + 1)
            throw new IllegalArgumentException (
                  "Invalid dimensions for thresholds");
      }
      if (thresholds != null)
         this.thresholds = ArrayUtil.deepClone (thresholds);
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }

   public double[][] getRanksTG () {
      double[][] ranksTG = new double[numTypes][];
      for (int k = 0; k < ranksTG.length; k++)
         ranksTG[k] = ags[k].getRanks ();
      return ranksTG;
   }

   public double[][] getRanksGT () {
      double[][] ranksGT = new double[numGroups][];
      for (int i = 0; i < ranksGT.length; i++)
         ranksGT[i] = wqs[i].getRanks ();
      return ranksGT;
   }

   public double getPatienceRate (int k) {
      return nu[k];
   }
   
   public void setPatienceRate (int k, double nuk) {
      if (nuk < 0)
         throw new IllegalArgumentException ("nuk < 0");
      if (nuk > nuBound[k])
         throw new IllegalArgumentException ("nuk too large");
      nu[k] = nuk;
   }
   
   public double getMaxPatienceRate (int k) {
      return nuBound[k];
   }
   
   public void setMaxPatienceRate (int k, double nuk) {
      if (nuk < nu[k])
         throw new IllegalArgumentException ("nuk too small");
      nuBound[k] = nuk;
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }

   public double getProbBalking (int k) {
      return rho[k];
   }
   
   public void setProbBalking (int k, double rhok) {
      if (rhok < 1 || rhok > 1)
         throw new IllegalArgumentException ("rhok not in [0,1]");
      rho[k] = rhok;
   }

   public int getNumTransitionsDone () {
      return ntr;
   }

   public int getNumPrecedingFalseTransitions () {
      return m_np;
   }

   public int getNumFollowingFalseTransitions () {
      return m_nf;
   }

   public int getNumContactsInService () {
      return totalNumContactsInService;
   }

   public int getNumContactsInService (int k, int i) {
      if (numContactsInService == null) {
         if (numTypes == 1 && numGroups == 1) {
            if (k != 0 || i != 0)
               throw new IllegalArgumentException ();
            return totalNumContactsInService;

         }
         else if (numTypes == 1) {
            if (k != 0)
               throw new IllegalArgumentException ();
            return totalNumContactsInServiceI[i];
         }
         else
            throw new UnsupportedOperationException ();
      }
      return numContactsInService[k * numGroups + i];
   }

   public int getNumContactsInServiceI (int i) {
      return totalNumContactsInServiceI[i];
   }

   public int getNumContactsInServiceK (int k) {
      if (totalNumContactsInServiceK == null) {
         if (numTypes == 1) {
            if (k != 0)
               throw new IllegalArgumentException ();
            return totalNumContactsInService;
         }
         else
            throw new UnsupportedOperationException ();
      }
      return totalNumContactsInServiceK[k];
   }

   public double getServiceRate (int k, int i) {
      return mu[numGroups * k + i];
   }
   
   public void setServiceRate (int k, int i, double muki) {
      if (numContactsInService == null)
         throw new IllegalStateException ("Cannot change service rates");
      if (muki < 0)
         throw new IllegalArgumentException ("muki < 0");
      final int idx = numGroups * k + i;
      if (muki > muBound[idx])
         throw new IllegalArgumentException ("muki too large");
      mu[idx] = muki;
      
      if (muki > maxMu[i])
         maxMu[i] = muki;
      else {
         maxMu[i] = 0;
         for (int k2 = 0; k2 < numTypes; k2++) {
            final int idx2 = numGroups * k2 + i;
            final double muki2 = mu[idx2];
            if (maxMu[i] < muki2)
               maxMu[i] = muki2;
         }
      }
   }
   
   public double getMaxServiceRate (int k, int i) {
      return muBound[numGroups * k + i];
   }
   
   public void setMaxServiceRate (int k, int i, double muki) {
      if (numContactsInService == null)
         throw new IllegalStateException ("Cannot change service rates");
      final int idx = numGroups * k + i;
      if (muki < mu[idx])
         throw new IllegalArgumentException ("muki too small");
      muBound[idx] = muki;
      initRates ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
   }

   public void initEmpty () {
      Arrays.fill (numContactsInQueue, 0);
      if (numContactsInService != null)
         Arrays.fill (numContactsInService, 0);
      totalNumContactsInQueue = totalNumContactsInService = 0;
      Arrays.fill (totalNumContactsInServiceI, 0);
      if (totalNumContactsInServiceK != null)
         Arrays.fill (totalNumContactsInServiceK, 0);
      lastK = lastI = lastKp = lastPos = -1;
      ntr = m_np = m_nf = 0;
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      tntr = Integer.MAX_VALUE;
      lastType = null;
      // qidx = queueSizeThresh.length - 1;
   }

   public void init (CallCenterCTMC ctmc) {
      if (ctmc instanceof CallCenterCTMCKI) {
         CallCenterCTMCKI ctmc1 = (CallCenterCTMCKI) ctmc;
         if (numContactsInQueue.length != ctmc1.numContactsInQueue.length)
            throw new IllegalArgumentException ();
         if (totalNumContactsInServiceI.length != ctmc1.totalNumContactsInServiceI.length)
            throw new IllegalArgumentException ();
         ntr = ctmc1.ntr;
         tntr = ctmc1.tntr;
         m_np = m_nf = 0;
         System.arraycopy (ctmc1.numContactsInQueue, 0, numContactsInQueue, 0,
               numContactsInQueue.length);
         if (numContactsInService != null)
            System.arraycopy (ctmc1.numContactsInService, 0,
                  numContactsInService, 0, numContactsInService.length);
         totalNumContactsInQueue = ctmc1.totalNumContactsInQueue;
         totalNumContactsInService = ctmc1.totalNumContactsInService;
         System
               .arraycopy (ctmc1.totalNumContactsInServiceI, 0,
                     totalNumContactsInServiceI, 0,
                     totalNumContactsInServiceI.length);
         if (totalNumContactsInServiceK != null)
            System.arraycopy (ctmc1.totalNumContactsInServiceK, 0,
                  totalNumContactsInServiceK, 0,
                  totalNumContactsInServiceK.length);
         // qidx = stateThresh.updateQIdx (0, totalNumContactsInQueue);
         stateThresh.initOperatingMode (this);
         qidx = stateThresh.getOperatingMode ();
         lastType = null;
      }
      else
         throw new IllegalArgumentException ();
   }

   public boolean selectContact (int i) {
      if (totalNumContactsInServiceI[i] >= numAgents[i])
         return false;
      final int kp = wqs[i].selectWaitingQueue (this, -1, ntr);
      if (kp >= 0) {
         lastI = i;
         lastKp = kp;
         lastPos = 0;
         removeQueuedContact (kp);
         addServedContact (kp, i);
         if (stateThresh.updateOperatingMode (this,
               TransitionType.ENDSERVICEANDDEQUEUE))
            qidx = stateThresh.getOperatingMode ();
         return true;
      }
      return false;
   }

   public TransitionType nextState (double u) {
      int rv = (int) (u * (Integer.MAX_VALUE + 1.0));
      return lastType = nextStateI (rv);
   }

   public TransitionType nextStateInt (int rv) {
      return lastType = nextStateI (rv);
   }

   public TransitionType getNextTransition (double u) {
      int rv = (int) (u * (Integer.MAX_VALUE + 1.0));
      return lastType = getNextTransitionI (rv);
   }

   public TransitionType getNextTransitionInt (int rv) {
      return lastType = getNextTransitionI (rv);
   }

   private boolean checkState () {
      for (int i = 0; i < getNumAgentGroups (); i++) {
         int si = getNumContactsInServiceI (i);
         int th = stateThresh.getThreshNumAgents (qidx, i);
         if (si > th) {
            System.err
                  .println ("Number of contacts in service with agents in group "
                        + i
                        + " is "
                        + si
                        + ", which is greater than current threshold " + th);
            return false;
         }
      }
      int q = getNumContactsInQueue ();
      int thq = stateThresh.getThreshQueueSize (qidx);
      if (q > thq) {
         System.err.println ("Number of contacts in queue is " + q
               + ", which is greater than current threshold " + thq);
         return false;
      }
      return true;
   }

   private TransitionType nextStateI (int rv) {
      if (ntr >= tntr)
         throw new IllegalStateException ();
      // int n = queueSizeThresh.getNumTransitions (qidx) - 1;
      // assert n >= 0;
      assert checkState () : "State inconsistent with thresholds";
      m_np = m_nf = 0;
      final TransitionType type = lookups[qidx]
            .actions (this, ntr, rv, 0, true);
      ntr += m_np + m_nf + 1;
      if (stateThresh.updateOperatingMode (this, type))
         qidx = stateThresh.getOperatingMode ();
      return type;
   }

   private TransitionType getNextTransitionI (int rv) {
      m_np = m_nf = 0;
      final TransitionType type = lookups[qidx].actions (this, ntr, rv, 0,
            false);
      return type;
   }

   public void generateArrivalQueued (int k, int np1, int nf1) {
      addQueuedContactC (k);
      lastK = k;
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
      if (stateThresh.updateOperatingMode (this, TransitionType.ARRIVALQUEUED))
         qidx = stateThresh.getOperatingMode ();
   }

   public void generateArrivalServed (int k, int i, int np1, int nf1) {
      addServedContactC (k, i);
      lastK = k;
      lastI = i;
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
      if (stateThresh.updateOperatingMode (this, TransitionType.ARRIVALSERVED))
         qidx = stateThresh.getOperatingMode ();
   }

   public void generateEndService (int k, int i, int kp, int np1, int nf1) {
      removeServedContactC (k, i);
      removeQueuedContactC (kp);
      addServedContactC (kp, i);
      lastK = k;
      lastI = i;
      lastKp = kp;
      lastPos = 0;
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
      if (stateThresh.updateOperatingMode (this,
            TransitionType.ENDSERVICEANDDEQUEUE))
         qidx = stateThresh.getOperatingMode ();
   }

   public void generateEndService (int k, int i, int np1, int nf1) {
      removeServedContactC (k, i);
      lastK = k;
      lastI = i;
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
      if (stateThresh.updateOperatingMode (this,
            TransitionType.ENDSERVICENODEQUEUE))
         qidx = stateThresh.getOperatingMode ();
   }

   public void generateFalseTransition (int np1, int nf1) {
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
   }

   public void generateAbandonment (int k, int kpos, int np1, int nf1) {
      if (kpos < 0 || kpos >= numContactsInQueue[k])
         throw new IllegalStateException ();
      lastPos = kpos;
      lastK = k;
      removeQueuedContact (k);
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
      if (stateThresh.updateOperatingMode (this, TransitionType.ABANDONMENT))
         qidx = stateThresh.getOperatingMode ();
   }

   public void generateArrival (int k, int np1, int nf1) {
      lastK = k;
      this.m_np = np1;
      this.m_nf = nf1;
      ntr += np1 + nf1 + 1;
   }

   @Override
   public CallCenterCTMCKI clone () {
      CallCenterCTMCKI cpy;
      try {
         cpy = (CallCenterCTMCKI) super.clone ();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      cpy.lambda = lambda.clone ();
      cpy.lambdaBound = lambdaBound.clone ();
      cpy.mu = mu.clone ();
      cpy.muBound = muBound.clone ();
      cpy.maxMuBound = maxMuBound.clone ();
      cpy.nu = nu.clone ();
      cpy.nuBound = nuBound.clone ();
      cpy.rho = rho.clone ();
      cpy.maxNumAgents = maxNumAgents.clone ();
      cpy.numAgents = numAgents.clone ();
      cpy.numContactsInQueue = numContactsInQueue.clone ();
      if (numContactsInService != null)
         cpy.numContactsInService = numContactsInService.clone ();
      cpy.totalNumContactsInServiceI = totalNumContactsInServiceI.clone ();
      if (totalNumContactsInServiceK != null)
         cpy.totalNumContactsInServiceK = totalNumContactsInServiceK.clone ();
      if (stateThresh != null)
         cpy.stateThresh = stateThresh.clone ();
      return cpy;
   }

   public boolean equalsState (Object obj) {
      if (this == obj)
         return true;
      if (obj instanceof CallCenterCTMCKI) {
         CallCenterCTMCKI ctmc = (CallCenterCTMCKI) obj;
         if (ntr != ctmc.ntr)
            return false;
         if (!Arrays.equals (numContactsInQueue, ctmc.numContactsInQueue))
            return false;
         if (numContactsInService == null) {
            if (ctmc.numContactsInService != null)
               return false;
            return Arrays.equals (totalNumContactsInServiceI,
                  ctmc.totalNumContactsInServiceI);
         }
         else {
            if (ctmc.numContactsInService == null)
               return false;
            return Arrays.equals (numContactsInService,
                  ctmc.numContactsInService);
         }
      }
      return false;
   }

   public int hashCodeState () {
      int hash = queueCapacity*ntr + totalNumContactsInQueue;
      for (int i = maxNumAgents.length - 1; i >= 0; i--) {
         hash *= maxNumAgents[i];
         hash += totalNumContactsInServiceI[i];
      }
      return hash;
   }

   @Override
   public String toString () {
      return String.format ("{%d transitions done, Q=%s, S=%s}", ntr, Arrays
            .toString (numContactsInQueue), Arrays
            .toString (totalNumContactsInServiceI));
   }

   public int getTargetNumTransitions () {
      return tntr;
   }

   public void setTargetNumTransitions (int tntr) {
      if (tntr < ntr)
         throw new IllegalArgumentException (
               "Target number of transitions too small");
      this.tntr = tntr;
   }
}
