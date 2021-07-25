package umontreal.iro.lecuyer.contactcenters.ctmc;

import umontreal.ssj.probdist.GeometricDist;
import umontreal.iro.lecuyer.util.ArrayUtil;



/**
 * CTMC model for a call center with a single
 * call type and a single agent group.
 */
public class CallCenterCTMC11 implements CallCenterCTMC {
   private double lambda, mu, nu, rho;
   private double maxLambda, maxMu, maxNu;
   private int queueCapacity;
   private int maxQueueCapacity;
   private int numAgents;
   private int maxNumAgents;

   private int[][] thresholds;
   private double jumpRate;
   private double[] probArrivalAgents;
   private double[] probArrivalNoAgents;
   private double[] probBalking;
   private double[] probArrivalBound;
   private double[][] probEndService;
   private double[][] probAbandon;

   private StateThresh stateThresh;
   private GeometricDist curNumFalseTrDist;
   private double[][] nftrCdf;
   private double[] curNftrCdf;
   
   private int qidx;
   private double curProbArrivalAgents;
   private double curProbArrivalNoAgents;
   private double curProbBalking;
   private double curProbArrivalBound;
   private double[] curProbEndService;
   private double[] curProbAbandon;
   
   private int numContactsInService;
   private int numContactsInQueue;
   private int lastPos;
   private int ntr;
   private int m_np;
   private int m_nf;
   private int tntr;
   private TransitionType lastType;
   
   public CallCenterCTMC11 (double lambda,
         double maxLambda,
         double mu,
         double maxMu,
         int numAgents,
         int maxNumAgents,
         double rho, double nu, double maxNu,
         int queueCapacity, int maxQueueCapacity, int[][] thresholds) {
      if (lambda < 0 || mu < 0 || nu < 0)
         throw new IllegalArgumentException ("lambda, mu or nu is negative");
      if (numAgents < 0 || queueCapacity < 0)
         throw new IllegalArgumentException ("numAgents or queueCapacity is negative");
      if (rho < 0 || rho > 1)
         throw new IllegalArgumentException ("rho not in [0,1]");
      this.lambda = lambda;
      this.maxLambda = Math.max (lambda, maxLambda);
      this.mu = mu;
      this.maxMu = Math.max (mu, maxMu);
      this.nu = nu;
      this.maxNu = Math.max (nu, maxNu);
      this.rho = rho;
      this.numAgents = numAgents;
      this.maxNumAgents = Math.max (maxNumAgents, numAgents);
      this.queueCapacity = queueCapacity;
      this.maxQueueCapacity = Math.max (queueCapacity, maxQueueCapacity);
      this.thresholds = thresholds;
      init ();
   }
   
   private void init () {
      jumpRate = maxLambda + maxNumAgents * maxMu + maxQueueCapacity * maxNu;
      
      stateThresh = new StateThresh (this, thresholds);
      probArrivalAgents = new double[stateThresh.getNumVectorsOfThresholds ()];
      probArrivalNoAgents = new double[probArrivalAgents.length];
      probBalking = new double[probArrivalAgents.length];
      probArrivalBound = new double[probArrivalAgents.length];
      probEndService = new double[probArrivalAgents.length][];
      probAbandon = new double[probArrivalAgents.length][];
      nftrCdf = new double[probArrivalAgents.length][];
      for (int qs = 0; qs < probArrivalAgents.length; qs++) {
         final double jumpRateQ = stateThresh.getJumpRate (qs);
         probArrivalAgents[qs] = lambda / jumpRateQ;
         probArrivalNoAgents[qs] = (1 - rho) * probArrivalAgents[qs];
         probBalking[qs] = rho * probArrivalAgents[qs];
         probArrivalBound[qs] = maxLambda / jumpRateQ;
         probEndService[qs] = new double[stateThresh.getThreshNumAgents (qs, 0) + 1];
         for (int i = 0; i < probEndService[qs].length; i++)
            probEndService[qs][i] = i * maxMu / jumpRateQ;
         probAbandon[qs] = new double[stateThresh.getThreshQueueSize (qs) + 1];
         for (int i = 0; i < probAbandon[qs].length; i++)
            probAbandon[qs][i] = i * maxNu / jumpRateQ;
         final GeometricDist numFalseTrDistQ = stateThresh.getNumFalseTrDist (qs);
         if (numFalseTrDistQ.getP() == 1)
            nftrCdf[qs] = null;
         else {
            double n = numFalseTrDistQ.getMean() + 5*numFalseTrDistQ.getStandardDeviation();
            nftrCdf[qs] = new double[(int)n];
            for (int j = 0; j < nftrCdf[qs].length; j++)
               nftrCdf[qs][j] = numFalseTrDistQ.cdf (j);
         }
         
      }
   }
   
   public double getArrivalRate (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return lambda;
   }
   
   public void setArrivalRate (int k, double rate) {
      if (k != 0 || rate < 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (rate < 0)
         throw new IllegalArgumentException
         ("rate < 0");
      if (rate > maxLambda)
         throw new IllegalArgumentException ("Arrival rate too large");
      lambda = rate;
      for (int qs = 0; qs < probArrivalAgents.length; qs++) {
         final double jumpRateQ = stateThresh.getJumpRate (qs);
         probArrivalAgents[qs] = lambda / jumpRateQ;
         probArrivalNoAgents[qs] = (1 - rho) * probArrivalAgents[qs];
         probBalking[qs] = rho * probArrivalAgents[qs];
      }
      curProbArrivalAgents = probArrivalAgents[qidx];
      curProbArrivalNoAgents = probArrivalNoAgents[qidx];
      curProbBalking = probBalking[qidx];
   }
   
   public void setArrivalRates (double[] rates) {
      if (rates.length != 1)
         throw new IllegalArgumentException();
      setArrivalRate (0, rates[0]);
   }

   public double getArrivalRate () {
      return lambda;
   }
   
   public double getMaxArrivalRate (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return maxLambda;
   }
   
   public double getMaxArrivalRate () {
      return maxLambda;
   }
   
   public void setMaxArrivalRate (int k, double maxLambda) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (maxLambda < lambda)
         throw new IllegalArgumentException
         ("maxLambda < lambda");
      this.maxLambda = maxLambda;
      init();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }

   public void setMaxArrivalRates (double[] rates) {
      if (rates.length != 1)
         throw new IllegalArgumentException();
      setMaxArrivalRate (0, rates[0]);
   }
   
   public double getJumpRate () {
      return jumpRate;
   }
   
   public TransitionType getLastTransitionType() {
      return lastType;
   }

   public int getLastSelectedAgentGroup () {
      return 0;
   }

   public int getLastSelectedContactType () {
      return 0;
   }
   
   public int getLastSelectedContact () {
      return lastPos;
   }

   public int getLastSelectedQueuedContactType () {
      return 0;
   }

   public int getNumAgentGroups () {
      return 1;
   }

   public int getNumAgents () {
      return numAgents;
   }

   public int getNumAgents (int i) {
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return numAgents;
   }

   public int getMaxNumAgents () {
      return maxNumAgents;
   }

   public int getMaxNumAgents (int i) {
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return maxNumAgents;
   }
   
   public int[] getMaxNumAgentsArray () {
      return new int[] { maxNumAgents };
   }

   public int[] getNumAgentsArray () {
      return new int[] { numAgents };
   }

   public void setMaxNumAgents (int i, int n) {
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      if (n == numAgents)
         return;
      if (n < numAgents)
         throw new IllegalArgumentException
         ("Cannot set the maximal number of agents to a value smaller than the current number of agents");
      maxNumAgents = n;
      init ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }

   public void setMaxNumAgents (int[] numAgents) {
      if (numAgents.length != 1)
         throw new IllegalArgumentException
         ("numAgents must have a single element");
      setMaxNumAgents (0, numAgents[0]);
   }

   public void setNumAgents (int[] numAgents) {
      if (numAgents.length != 1)
         throw new IllegalArgumentException
         ("numAgents must have a single element");
      setNumAgents (0, numAgents[0]);
   }
   
   public double[][] getRanksTG() {
      return new double[][] {{1}};
   }
   
   public double[][] getRanksGT() {
      return new double[][] {{1}};
   }

   public int getNumContactTypes () {
      return 1;
   }

   public int getNumContactsInQueue () {
      return numContactsInQueue;
   }

   public int getNumContactsInQueue (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return numContactsInQueue;
   }

   public int getNumContactsInService () {
      return numContactsInService;
   }

   public int getNumContactsInService (int k, int i) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return numContactsInService;
   }

   public int getNumContactsInServiceI (int i) {
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return numContactsInService;
   }

   public int getNumContactsInServiceK (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return numContactsInService;
   }

   public int getNumTransitionsDone () {
      assert ntr <= tntr;
      return ntr;
   }
   
   public int getNumPrecedingFalseTransitions () {
      return m_np;
   }
   
   public int getNumFollowingFalseTransitions () {
      return m_nf;
   }

   public double getPatienceRate (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return nu;
   }
   
   public void setPatienceRate (int k, double nu) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (nu < 0)
         throw new IllegalArgumentException
         ("nu < 0");
      if (nu < maxNu)
         throw new IllegalArgumentException ("nu < maxNu");
      this.nu = nu;
   }

   public double getMaxPatienceRate (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return maxNu;
   }
   
   public void setMaxPatienceRate (int k, double maxNu) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (maxNu < nu)
         throw new IllegalArgumentException ("maxNu < nu");
      this.maxNu = maxNu;
      init();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }
   
   public double getProbBalking (int k) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      return rho;
   }
   
   public void setProbBalking (int k, double rho) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (rho < 0 || rho > 1)
         throw new IllegalArgumentException ("rho not in [0,1]");
      this.rho = rho;
      for (int qs = 0; qs < stateThresh.getNumVectorsOfThresholds (); qs++) {
         probArrivalNoAgents[qs] = (1 - rho) * probArrivalAgents[qs];
         probBalking[qs] = rho * probArrivalAgents[qs];
      }
      curProbArrivalNoAgents = probArrivalNoAgents[qidx];
      curProbBalking = probBalking[qidx];
   }

   public int getQueueCapacity () {
      return queueCapacity;
   }
   
   public void setQueueCapacity (int q) {
      if (q < numContactsInQueue)
         throw new IllegalArgumentException
         ("Queue capacity too small");
      if (q > maxQueueCapacity)
         throw new IllegalArgumentException
         ("Queue capacity too large");
      queueCapacity = q;
   }
   
   public int getMaxQueueCapacity () {
      return maxQueueCapacity;
   }
   
   public void setMaxQueueCapacity (int maxQueueCapacity) {
      if (maxQueueCapacity < queueCapacity)
         throw new IllegalArgumentException
         ("Maximal queue capacity too small");
      this.maxQueueCapacity = maxQueueCapacity;
      init ();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }
   
   public int getNumStateThresh() {
      return stateThresh.getNumVectorsOfThresholds ();
   }
   
   public StateThresh getStateThresh() {
      return stateThresh;
   }
   
   public int[][] getStateThresholds() {
      int[][] thresh = new int[stateThresh.getNumVectorsOfThresholds ()][2];
      for (int r = 0; r < thresh.length; r++) {
         thresh[r][0] = stateThresh.getThreshNumAgents (r, 0);
         thresh[r][1] = stateThresh.getThreshQueueSize (r);
      }
      return thresh;
   }
   
   public void setStateThresholds (int[][] thresholds) {
      if (thresholds != null && thresholds.length > 0) {
         ArrayUtil.checkRectangularMatrix (thresholds);
         if (thresholds[0].length != 2)
            throw new IllegalArgumentException
            ("Invalid dimensions for thresholds");
      }
      if (thresholds != null)
         this.thresholds = ArrayUtil.deepClone (thresholds);
      init();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }
   
   public double getServiceRate (int k, int i) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return mu;
   }
   
   public void setServiceRate (int k, int i, double mu) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      if (mu < 0)
         throw new IllegalArgumentException
         ("mu < 0");
      if (mu > maxMu)
         throw new IllegalArgumentException
         ("mu > maxMu");
      this.mu = mu;
   }
   
   public double getMaxServiceRate (int k, int i) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      return maxMu;
   }
   
   public void setMaxServiceRate (int k, int i, double maxMu) {
      if (k != 0)
         throw new IllegalArgumentException ("Contact type identifier out of range");
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      if (maxMu < mu)
         throw new IllegalArgumentException
         ("maxMu < mu");
      this.maxMu = maxMu;
      init();
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
   }

   public void initEmpty () {
      numContactsInService = numContactsInQueue = 0;
      m_np = m_nf = ntr = 0;
      lastPos = -1;
      stateThresh.initOperatingMode (this);
      qidx = stateThresh.getOperatingMode ();
      updateProb ();
      tntr = Integer.MAX_VALUE;
      lastType = null;
   }
   
   public void init (CallCenterCTMC ctmc) {
      if (ctmc instanceof CallCenterCTMC11) {
         CallCenterCTMC11 ctmc1 = (CallCenterCTMC11)ctmc;
         if (ctmc1.numContactsInService > maxNumAgents)
            throw new IllegalStateException();
         ntr = ctmc1.ntr;
         m_np = m_nf = 0;
         numContactsInQueue = ctmc1.numContactsInQueue;
         numContactsInService = ctmc1.numContactsInService;
         lastPos = -1;
         stateThresh.initOperatingMode (this);
         qidx = stateThresh.getOperatingMode ();
         updateProb();
         tntr = ctmc1.tntr;
         lastType = null;
      }
      else
         throw new IllegalArgumentException();
   }
   
   public boolean selectContact (int i) {
      if (numContactsInService >= numAgents || numContactsInQueue == 0)
         return false;
      ++numContactsInService;
      --numContactsInQueue;
      lastPos = 0;
      updateIdx (TransitionType.ENDSERVICEANDDEQUEUE);
      return true;
   }

   public TransitionType nextStateInt (int v) {
      TransitionType type = nextStateU ((double)v / Integer.MAX_VALUE);
      updateIdx (type);
      return lastType = type;
   }

   public TransitionType nextState (double uu) {
      TransitionType type = nextStateU (uu);
      updateIdx (type);
      return lastType = type;
   }

   public TransitionType getNextTransition (double u) {
      return lastType = getNextTransitionU (u);
   }

   public TransitionType getNextTransitionInt (int u) {
      return lastType = getNextTransitionU ((double)u / Integer.MAX_VALUE);
   }

   private void updateIdx (TransitionType type) {
      if (stateThresh.updateOperatingMode (this, type)) {
         qidx = stateThresh.getOperatingMode ();
         updateProb();
      }
      assert qidx == stateThresh.getOperatingMode ();
   }
   
   private void updateProb() {
      assert qidx == stateThresh.getOperatingMode ();
      curProbArrivalAgents = probArrivalAgents[qidx];
      curProbArrivalNoAgents = probArrivalNoAgents[qidx];
      curProbArrivalBound = probArrivalBound[qidx];
      curProbBalking = probBalking[qidx];
      curProbEndService = probEndService[qidx];
      curProbAbandon = probAbandon[qidx];
      curNumFalseTrDist = stateThresh.getNumFalseTrDist (qidx);
      curNftrCdf = nftrCdf[qidx];
   }
   
   private double getU (double uu) {
      if (curNumFalseTrDist.getP() < 1) {
         m_np = curNumFalseTrDist.inverseFInt (uu);
         final int trAfter = ntr + m_np + 1;
         if (trAfter >= tntr) {
            final int diff = trAfter - tntr;
            if (diff > 0) {
               m_np -= diff;
               assert m_np >= 0;
               return -1;
            }
         }
         final double cdfnpm1;
         if (m_np == 0)
            cdfnpm1 = 0;
         else
            cdfnpm1 = m_np - 1 < curNftrCdf.length ? curNftrCdf[m_np - 1] : curNumFalseTrDist.cdf (m_np - 1);
         double cdfnp = m_np < curNftrCdf.length ? curNftrCdf[m_np] : curNumFalseTrDist.cdf (m_np);
         double u = (uu - cdfnpm1) / (cdfnp - cdfnpm1);
         assert u >= 0 && u < 1;
         return u;
      }
      else
         return uu;
      
   }
   
   private TransitionType nextStateU (double uu) {
      if (ntr >= tntr)
         throw new IllegalStateException();
      m_nf = m_np = 0;
//      final double pf = probArrivalAgents + probEndService[numContactsInService] + probAbandon[numContactsInQueue];
//      if (uu >= pf) {
//         nf = GeometricDist.getMean (1-pf) > 10 ? GeometricDist.inverseF (1-pf, 1-uu) - 1 : 0;
//         ntr += nf;
//         return TransitionType.FALSETRANSITION;
//      }
      double u = getU (uu);
      ntr += m_np + m_nf + 1;
      if (u < 0)
         return TransitionType.FALSETRANSITION;
      assert qidx == stateThresh.getOperatingMode ();
      assert curProbEndService == probEndService[qidx];
      assert curProbAbandon == probAbandon[qidx];
      assert curProbArrivalAgents == probArrivalAgents[qidx];
      assert curProbArrivalNoAgents == probArrivalNoAgents[qidx];
      assert curProbArrivalBound == probArrivalBound[qidx];
      if (numContactsInService < numAgents) {
         assert numContactsInQueue == 0;
         //assert qidx == 0;
         if (u < curProbArrivalBound) {
            if (u < curProbArrivalAgents) {
               ++numContactsInService;
               return TransitionType.ARRIVALSERVED;
            }
            else
               return TransitionType.FALSETRANSITION;
         }
         else {
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u*stateThresh.getJumpRate (qidx) % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               --numContactsInService;
               return TransitionType.ENDSERVICENODEQUEUE;
            }
            else {
               //nf = ntrNQ - 1;
               //ntr += nf;
               return TransitionType.FALSETRANSITION;
            }
         }
      }
      else if (numContactsInQueue < queueCapacity) {
         assert numContactsInQueue <= stateThresh.getThreshQueueSize (qidx);
         if (u < curProbArrivalBound) {
            if (u < curProbBalking)
               return TransitionType.ARRIVALBALKED;
            else {
               u -= curProbBalking;
               if (u < curProbArrivalNoAgents) {
                  ++numContactsInQueue;
                  //updateQIdx ();
                  //qidx = stateThresh.updateIdx (this, TransitionType.ARRIVALQUEUED, qidx);
                  return TransitionType.ARRIVALQUEUED;
               }
               else
                  return TransitionType.FALSETRANSITION;
            }
         }
         else {
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u*stateThresh.getJumpRate (qidx) % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               if (numContactsInService > numAgents || numContactsInQueue == 0) {
                  --numContactsInService;
                  return TransitionType.ENDSERVICENODEQUEUE;
               }
               --numContactsInQueue;
               //updateQIdx();
               //qidx = stateThresh.updateIdx (this, TransitionType.ENDSERVICEANDDEQUEUE, qidx);
               lastPos = 0;
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else {
               final double prob2 = curProbEndService[curProbEndService.length - 1];
               if (u < prob2)
                  return TransitionType.FALSETRANSITION;
               u -= prob2;
               final double prob3 = curProbAbandon[numContactsInQueue]; 
               if (u < prob3) {
                  if (nu < maxNu) {
                     final double v = u*stateThresh.getJumpRate (qidx) % maxNu;
                     if (v >= nu)
                        return TransitionType.FALSETRANSITION;
                  }
                  --numContactsInQueue;
                  final double w = u / curProbAbandon[1];
                  lastPos = (int)w;
                  //updateQIdx();
                  //qidx = stateThresh.updateIdx (this, TransitionType.ABANDONMENT, qidx);
                  return TransitionType.ABANDONMENT;
               }
               else
                  return TransitionType.FALSETRANSITION;
            }
         }
      }
      else {
         if (u < curProbArrivalAgents)
            return TransitionType.ARRIVALBLOCKED;
         else {
            if (u < curProbArrivalBound)
               return TransitionType.FALSETRANSITION;
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u*stateThresh.getJumpRate (qidx) % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               if (numContactsInService > numAgents || numContactsInQueue == 0) {
                  --numContactsInService;
                  return TransitionType.ENDSERVICENODEQUEUE;
               }
               lastPos = 0;
               --numContactsInQueue;
               //updateQIdx();
               //qidx = stateThresh.updateIdx (this, TransitionType.ENDSERVICEANDDEQUEUE, qidx);
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else {
               final double prob2 = curProbEndService[curProbEndService.length - 1];
               if (u < prob2)
                  return TransitionType.FALSETRANSITION;
               u -= prob2;
               assert u < curProbAbandon[queueCapacity];
               if (nu < maxNu) {
                  final double v = u*stateThresh.getJumpRate (qidx) % maxNu;
                  if (v >= nu)
                     return TransitionType.FALSETRANSITION;
               }
               //if (u < probAbandonQ[queueCapacity]) {
               --numContactsInQueue;
               final double w = u / curProbAbandon[1];
               lastPos = (int)w;
               //updateQIdx();
               //qidx = stateThresh.updateIdx (this, TransitionType.ABANDONMENT, qidx);
               return TransitionType.ABANDONMENT;
            }
         }
      }
   }

   private TransitionType getNextTransitionU (double uu) {
      m_np = m_nf = 0;
      double u = getU (uu);
      if (u < 0)
         return TransitionType.FALSETRANSITION;
      assert qidx == stateThresh.getOperatingMode ();
      if (numContactsInService < numAgents) {
         assert numContactsInQueue == 0;
         //assert qidx == 0;
         if (u < curProbArrivalBound) {
            if (u < curProbArrivalAgents)
               return TransitionType.ARRIVALSERVED;
            else
               return TransitionType.FALSETRANSITION;
         }
         else {
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               return TransitionType.ENDSERVICENODEQUEUE;
            }
            else
               return TransitionType.FALSETRANSITION;
         }
      }
      else if (numContactsInQueue < queueCapacity) {
         assert numContactsInQueue <= stateThresh.getThreshQueueSize (qidx);
         if (u < curProbArrivalBound) {
            if (u < curProbBalking)
               return TransitionType.ARRIVALBALKED;
            else {
               u -= curProbBalking;
               if (u < curProbArrivalNoAgents)
                  return TransitionType.ARRIVALQUEUED;
               else
                  return TransitionType.FALSETRANSITION;
            }
         }
         else {
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               if (numContactsInService > numAgents || numContactsInQueue == 0)
                  return TransitionType.ENDSERVICENODEQUEUE;
               lastPos = 0;
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else {
               final double prob2 = curProbEndService[curProbEndService.length - 1];
               if (u < prob2)
                  return TransitionType.FALSETRANSITION;
               u -= prob2;
               final double prob3 = curProbAbandon[numContactsInQueue]; 
               if (u < prob3) {
                  if (nu < maxNu) {
                     final double v = u % maxNu;
                     if (v >= nu)
                        return TransitionType.FALSETRANSITION;
                  }
                  final double w = u / curProbAbandon[1];
                  lastPos = (int)w;
                  return TransitionType.ABANDONMENT;
               }
               else
                  return TransitionType.FALSETRANSITION;
            }
         }
      }
      else {
         if (u < curProbArrivalAgents)
            return TransitionType.ARRIVALBLOCKED;
         else {
            if (u < curProbArrivalBound)
               return TransitionType.FALSETRANSITION;
            u -= curProbArrivalBound;
            final double prob = curProbEndService[numContactsInService]; 
            if (u < prob) {
               if (mu < maxMu) {
                  final double v = u % maxMu;
                  if (v >= mu)
                     return TransitionType.FALSETRANSITION;
               }
               if (numContactsInService > numAgents || numContactsInQueue == 0)
                  return TransitionType.ENDSERVICENODEQUEUE;
               lastPos = 0;
               return TransitionType.ENDSERVICEANDDEQUEUE;
            }
            else {
               final double prob2 = curProbEndService[curProbEndService.length - 1];
               if (u < prob2)
                  return TransitionType.FALSETRANSITION;
               u -= prob2;
               assert u < curProbAbandon[queueCapacity];
               if (nu < maxNu) {
                  final double v = u % maxNu;
                  if (v >= nu)
                     return TransitionType.FALSETRANSITION;
               }
               final double w = u / curProbAbandon[1];
               lastPos = (int)w;
               return TransitionType.ABANDONMENT;
            }
         }
      }
   }
   
   public void generateArrivalQueued (int k, int np, int nf) {
      if (numContactsInQueue >= queueCapacity)
         throw new IllegalStateException();
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      ++numContactsInQueue;
      //updateQIdx ();
      updateIdx (TransitionType.ARRIVALQUEUED);
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void generateArrivalServed (int k, int i, int np, int nf) {
      if (numContactsInService >= numAgents)
         throw new IllegalStateException();
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      ++numContactsInService;
      updateIdx (TransitionType.ARRIVALSERVED);
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void generateEndService (int k, int i, int kp, int np, int nf) {
      if (numContactsInService <= 0)
         throw new IllegalStateException();
      if (numContactsInQueue <= 0)
         throw new IllegalStateException ();
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      lastPos = 0;
      --numContactsInQueue;
      //updateQIdx();
      updateIdx (TransitionType.ENDSERVICEANDDEQUEUE);
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void generateEndService (int k, int i, int np, int nf) {
      if (numContactsInService <= 0)
         throw new IllegalStateException();
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      --numContactsInService;
      updateIdx (TransitionType.ENDSERVICENODEQUEUE);
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void generateFalseTransition (int np, int nf) {
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }
   
   public void generateAbandonment (int k, int kpos, int np, int nf) {
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      if (kpos < 0 || kpos >= numContactsInQueue)
         throw new IllegalStateException();
      --numContactsInQueue;
      //updateQIdx ();
      updateIdx (TransitionType.ABANDONMENT);
      lastPos = kpos;
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void generateArrival (int k, int np, int nf) {
      if (ntr + np + nf + 1 > tntr)
         throw new IllegalStateException();
      this.m_np = np;
      this.m_nf = nf;
      ntr += np + nf + 1;
   }

   public void setNumAgents (int i, int n) {
      if (i != 0)
         throw new IllegalArgumentException
         ("Agent group identifier out of range");
      if (numAgents < 0 || numAgents > maxNumAgents)
         throw new IllegalArgumentException
         ("The given number of agents must not be negative or exceed " + maxNumAgents);
      numAgents = n;
   }
   
   @Override
   public CallCenterCTMC11 clone() {
      CallCenterCTMC11 cpy;
      try {
         cpy = (CallCenterCTMC11)super.clone();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
      cpy.stateThresh = stateThresh.clone ();
      cpy.probAbandon = probAbandon.clone ();
      cpy.probArrivalAgents = probArrivalAgents.clone ();
      cpy.probArrivalNoAgents = probArrivalNoAgents.clone ();
      cpy.probBalking = probBalking.clone ();
      return cpy;
   }

   public boolean equalsState (Object obj) {
      if (this == obj)
         return true;
      if (obj instanceof CallCenterCTMC11) {
         CallCenterCTMC11 ctmc = (CallCenterCTMC11)obj;
         return ntr == ctmc.ntr && numContactsInQueue == ctmc.numContactsInQueue &&
         numContactsInService == ctmc.numContactsInService;
      }
      if (obj instanceof CallCenterCTMC) {
         CallCenterCTMC ctmc = (CallCenterCTMC)obj;
         if (ctmc.getNumContactTypes() != 1 || ctmc.getNumAgentGroups() != 1)
            return false;
         return ntr == ctmc.getNumTransitionsDone() &&
         numContactsInQueue == ctmc.getNumContactsInQueue()
         && numContactsInService == ctmc.getNumContactsInService();
      }
      return false;
   }

   public int hashCodeState () {
      final int hash = maxNumAgents*(queueCapacity*ntr + numContactsInQueue) + numContactsInService;
      return hash;
   }
   
   @Override
   public String toString() {
      return String.format
      ("{%d transitions done, Q=[%d], S=[%d]}",
            ntr, numContactsInQueue, numContactsInService);
   }

   public int getTargetNumTransitions () {
      return tntr;
   }

   public void setTargetNumTransitions (int tntr) {
      if (tntr < ntr)
         throw new IllegalArgumentException ("Target number of transitions too small");
      this.tntr = tntr;
   }
}
