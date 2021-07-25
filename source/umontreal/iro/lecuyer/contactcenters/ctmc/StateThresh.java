package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import umontreal.ssj.probdist.GeometricDist;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents thresholds on the queue size, and the number of agents in each
 * group. The transition rate, and the distribution for the number of successive
 * self jumps preceding any transition are also computed and stored.
 */
public class StateThresh implements Cloneable {
   // Let R be the number of thresholds
   // RxI 2D array of thresholds on the number of agents
   private int[][] threshAgents;
   // R-dimensional vector of thresholds on the queue size
   private int[] threshQueue;
   // One jump rate, and one geometric distribution for each value of r=0,...,R-1
   private double[] jumpRate;
   private GeometricDist[] numFalseTrDist;

   // Array i, for i=0,...,I-1, gives all differing thresholds in threshAgents[.][i],
   // sorted in increasing order
   private int[][] diffThreshAgents;
   // Differing thresholds on queue size, sorted in increasing order
   private int[] diffThreshQueue;
   // Current value of r_i, for each agent group
   private int[] modeVectorAgents;
   // Current value of r_I for the queue
   private int modeVectorQueue;
   // Hash code depending on the mode vector (r_0,...,r_I), updated
   // each time r_i changes.
   // The hash code is a linear combination of coefficients.
   private int modeHash;
   // Coefficient associated with each r_i, when computing the hash code.
   private int[] hashMultAgents;
   private int hashMultQueue;
   // Hash map associating a vector of thresholds to each mode vector.
   // Mode vectors are represented here by their hash codes.
   private Map<Integer, Integer> modeHashToIdx;

   /**
    * Constructs a new state thresholds object using the given thresholds on the
    * queue size and agent groups. This constructor accepts a CTMC
    * \texttt{ctmc}, a matrix of thresholds \texttt{thresholds} on the number
    * of agents, and the queue
    * size. Elements \texttt{thresholds[r][i]},
    * for $i=0,\ldots,I-1$,
    * give the $r$th threshold on the number of agents in group \texttt{i}, while
    * element \texttt{thresholds[r][I]},
    * the threshold on queue size.
    * If \texttt{thresholds} is \texttt{null}, a single vector of thresholds
    * $(\tilde N_0,\ldots, \tilde N_{I-1}, H)$ is used.
    * This vector of thresholds is also added if \texttt{thresholds} is not \texttt{null}, and
    *  the vector is not present in the 2D array
    * \texttt{thresholds}.
    * 
    * @param ctmc
    *           the call center CTMC.
    * @param thresholds
    *           the thresholds on the number of agents, and queue size.
    * @exception IllegalArgumentException if \texttt{thresholds} has invalid
    * dimensions, or contains a negative value.
    */
   public StateThresh (CallCenterCTMC ctmc, int[][] thresholds) {
      // Creates a list of thresholds from input
      List<int[]> threshAgentsList = new ArrayList<int[]> ();
      List<Integer> threshQueueList = new ArrayList<Integer> ();

      boolean lastThreshFound = false;
      if (thresholds != null) {
         ArrayUtil.checkRectangularMatrix (thresholds);
         if (thresholds[0].length != ctmc.getNumAgentGroups () + 1)
            throw new IllegalArgumentException (
                  "threshAgents must have one column for each agent group");
         rloop: for (int r = 0; r < thresholds.length; r++) {
            if (thresholds[r][ctmc.getNumAgentGroups ()] < 0)
               throw new IllegalArgumentException ("The threshold " + r
                     + " on the queue size is negative");
            if (thresholds[r][ctmc.getNumAgentGroups ()] > ctmc
                  .getMaxQueueCapacity ())
               continue;
            boolean lastThresh = true;
            if (thresholds[r][ctmc.getNumAgentGroups ()] != ctmc
                  .getMaxQueueCapacity ())
               lastThresh = false;

            for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
               if (thresholds[r][i] < 0)
                  throw new IllegalArgumentException ("The threshold " + r
                        + " on the agent group " + i + " is negative");
               if (thresholds[r][i] > ctmc.getMaxNumAgents (i))
                  continue rloop;
               if (thresholds[r][i] != ctmc.getMaxNumAgents (i))
                  lastThresh = false;
            }

            threshQueueList.add (thresholds[r][ctmc.getNumAgentGroups ()]);
            int[] tr = new int[ctmc.getNumAgentGroups ()];
            System.arraycopy (thresholds[r], 0, tr, 0, tr.length);
            threshAgentsList.add (tr);
            if (lastThresh)
               lastThreshFound = true;
         }
      }

      // Ensures that a threshold for the maximal number of agents and queue
      // capacity exists.
      if (!lastThreshFound) {
         threshQueueList.add (ctmc.getMaxQueueCapacity ());
         int[] tr = new int[ctmc.getNumAgentGroups ()];
         for (int i = 0; i < tr.length; i++)
            tr[i] = ctmc.getMaxNumAgents (i);
         threshAgentsList.add (tr);
      }

      // Converts the lists of thresholds to arrays, for
      // more efficiency
      threshQueue = new int[threshQueueList.size ()];
      int r = 0;
      for (Integer tq : threshQueueList)
         threshQueue[r++] = tq;
      threshAgents = threshAgentsList
            .toArray (new int[threshAgentsList.size ()][]);

      // Computes jump rate for each vector of thresholds
      final double[] mu = getMu (ctmc);
      final double nu = getNu (ctmc);
      jumpRate = new double[threshQueue.length];
      numFalseTrDist = new GeometricDist[threshQueue.length];
      final double lambda = ctmc.getMaxArrivalRate ();
      for (r = 0; r < threshQueue.length; r++) {
         double sumMu = 0;
         for (int i = 0; i < threshAgents[r].length; i++)
            sumMu += mu[i] * threshAgents[r][i];
         double sumNu = nu * threshQueue[r];
         jumpRate[r] = lambda + sumMu + sumNu;
         numFalseTrDist[r] = new GeometricDist (jumpRate[r]
               / ctmc.getJumpRate ());
      }

      // Creates the arrays of differing thresholds
      diffThreshQueue = getDiffThresholds (threshQueue);
      diffThreshAgents = new int[ctmc.getNumAgentGroups ()][];
      for (int i = 0; i < diffThreshAgents.length; i++)
         diffThreshAgents[i] = getDiffThresholds (threshAgents, i);
      // Computes coefficients for hash code
      int mult = 1;
      hashMultAgents = new int[diffThreshAgents.length];
      for (int i = 0; i < hashMultAgents.length; i++) {
         hashMultAgents[i] = mult;
         mult *= diffThreshAgents[i].length;
      }
      hashMultQueue = mult;
      mult *= diffThreshQueue.length;

      modeVectorAgents = new int[diffThreshAgents.length];
      modeHashToIdx = new HashMap<Integer, Integer> (Math.min (1024, mult));
   }

   // Gets the differing values of array thresh, sorted in increasing order.
   private static int[] getDiffThresholds (int[] thresh) {
      Set<Integer> set = new TreeSet<Integer> ();
      for (int v : thresh)
         set.add (v);
      int[] res = new int[set.size ()];
      int idx = 0;
      for (Integer v : set)
         res[idx++] = v;
      return res;
   }

   // Gets the differing values among thresh[.][dim], sorted in increasing order
   private static int[] getDiffThresholds (int[][] thresh, int dim) {
      Set<Integer> set = new TreeSet<Integer> ();
      for (int[] v : thresh)
         set.add (v[dim]);
      int[] res = new int[set.size ()];
      int idx = 0;
      for (Integer v : set)
         res[idx++] = v;
      return res;
   }

   // Computes the maximal service rate, for each agent group
   private static double[] getMu (CallCenterCTMC ctmc) {
      double[] mu = new double[ctmc.getNumAgentGroups ()];
      for (int i = 0; i < mu.length; i++) {
         for (int k = 0; k < ctmc.getNumContactTypes (); k++) {
            final double muki = ctmc.getMaxServiceRate (k, i);
            if (mu[i] < muki)
               mu[i] = muki;
         }
      }
      return mu;
   }

   // Computes the maximal abandonment rate
   private static double getNu (CallCenterCTMC ctmc) {
      double nu = 0;
      for (int k = 0; k < ctmc.getNumContactTypes (); k++) {
         final double nuk = ctmc.getMaxPatienceRate (k);
         if (nu < nuk)
            nu = nuk;
      }
      return nu;
   }

   /**
    * Returns the number of vectors of thresholds stored by this object.
    * 
    * @return the number of vectors of thresholds.
    */
   public int getNumVectorsOfThresholds () {
      return threshAgents.length;
   }

   /**
    * Returns the transition rate corresponding to vector of thresholds with
    * index \texttt{r}.
    * 
    * @param r
    *           the index of the vector of thresholds.
    * @return the corresponding transition rate.
    */
   public double getJumpRate (int r) {
      return jumpRate[r];
   }

   /**
    * Returns the geometric distribution for the successive number of self jumps
    * before any transition, while the queue size and number of agents are
    * smaller than or equal to to thresholds with index \texttt{r}.
    * 
    * @param r
    *           the index of the vector of thresholds.
    * @return the distribution of the successive number of self jumps before any
    *         transition.
    */
   public GeometricDist getNumFalseTrDist (int r) {
      return numFalseTrDist[r];
   }

   /**
    * Returns the threshold on the number of agents in group \texttt{i}
    * corresponding to vector with index \texttt{r}.
    * 
    * @param r
    *           the index of the vector of thresholds.
    * @param i
    *           the index of the agent group.
    * @return the value of the threshold.
    */
   public int getThreshNumAgents (int r, int i) {
      return threshAgents[r][i];
   }

   /**
    * Returns a 2D array containing the thresholds on the number of agents.
    * Element \texttt{[r][i]} of the array gives the threshold with index
    * \texttt{r} for agent group \texttt{i}.
    * 
    * @return the array of thresholds.
    */
   public int[][] getThreshNumAgents () {
      return ArrayUtil.deepClone (threshAgents);
   }

   /**
    * Returns the threshold on the queue size corresponding to vector with index
    * \texttt{r}.
    * 
    * @param r
    *           the index of the vector of thresholds.
    * @return the threshold on the queue size.
    */
   public int getThreshQueueSize (int r) {
      return threshQueue[r];
   }

   /**
    * Returns the array of thresholds on the queue size. Element \texttt{r} of
    * the returned array gives the threshold for index \texttt{r}.
    * 
    * @return the array of thresholds on the queue size.
    */
   public int[] getThreshQueueSize () {
      return threshQueue.clone ();
   }

   /**
    * Determines the current operating mode $r$ depending on the state of the given CTMC
    * \texttt{ctmc}.
    * This method is called after the CTMC is initialized.
    * The more efficient {@link #updateOperatingMode(CallCenterCTMC,TransitionType)}
    * can be used to update $r$ at each transition.
    * The value of $r$ can be obtained using {@link #getOperatingMode()}.
    * 
    * @param ctmc
    *           the call center CTMC.
    */
   public void initOperatingMode (CallCenterCTMC ctmc) {
      modeHash = 0;
      for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
         final int n = ctmc.getNumContactsInServiceI (i);
         modeVectorAgents[i] = 0;
         while (n > diffThreshAgents[i][modeVectorAgents[i]])
            ++modeVectorAgents[i];
         modeHash += modeVectorAgents[i] * hashMultAgents[i];
      }
      int nq = ctmc.getNumContactsInQueue ();
      modeVectorQueue = 0;
      while (nq > diffThreshQueue[modeVectorQueue])
         ++modeVectorQueue;
      modeHash += modeVectorQueue * hashMultQueue;
   }

   /**
    * Returns the current operating mode.
    * The operating mode corresponds to the
    * vector of thresholds $r$ offering the smallest jump rate, and
    * thresholds greater than or equal to the current number of agents and queue size.
    * @return the current operating mode.
    */
   public int getOperatingMode () {
      Integer modeHashInt = modeHash;
      Integer v = modeHashToIdx.get (modeHashInt);
      if (v == null) {
         double minJumpRate = Double.POSITIVE_INFINITY;
         int minR = Integer.MAX_VALUE;
         rloop: for (int r = 0; r < threshAgents.length; r++) {
            if (diffThreshQueue[modeVectorQueue] > threshQueue[r])
               continue;
            for (int i = 0; i < modeVectorAgents.length; i++)
               if (diffThreshAgents[i][modeVectorAgents[i]] > threshAgents[r][i])
                  continue rloop;
            if (minJumpRate > jumpRate[r]) {
               minJumpRate = jumpRate[r];
               minR = r;
            }
         }
         modeHashToIdx.put (modeHashInt, minR);
         return minR;
      }
      return v;
   }

   /**
    * Updates the current vector of thresholds after a transition of type
    * \texttt{type} of the CTMC model \texttt{ctmc}.
    * This returns \texttt{true} if the operating mode changed, and needs
    * to be queried using {@link #getOperatingMode()}.
    * 
    * @param ctmc
    *           the call center CTMC.
    * @param type
    *           the transition type.
    * @return \texttt{true} if the operating mode changed.
    */
   public boolean updateOperatingMode (CallCenterCTMC ctmc, TransitionType type) {
      int oldModeHash = modeHash;
      int i, n;
      switch (type) {
      case ARRIVALSERVED:
         i = ctmc.getLastSelectedAgentGroup ();
         n = ctmc.getNumContactsInServiceI (i);
         while (n > diffThreshAgents[i][modeVectorAgents[i]]) {
            ++modeVectorAgents[i];
            modeHash += hashMultAgents[i];
         }
         break;
      case ARRIVALQUEUED:
         n = ctmc.getNumContactsInQueue ();
         while (n > diffThreshQueue[modeVectorQueue]) {
            ++modeVectorQueue;
            modeHash += hashMultQueue;
         }
         break;
      case ENDSERVICEANDDEQUEUE:
      case ABANDONMENT:
         n = ctmc.getNumContactsInQueue ();
         while (modeVectorQueue > 0 && n <= diffThreshQueue[modeVectorQueue - 1]) {
            --modeVectorQueue;
            modeHash -= hashMultQueue;
         }
         break;
      case ENDSERVICENODEQUEUE:
         i = ctmc.getLastSelectedAgentGroup ();
         n = ctmc.getNumContactsInServiceI (i);
         while (modeVectorAgents[i] > 0
               && n <= diffThreshAgents[i][modeVectorAgents[i] - 1]) {
            --modeVectorAgents[i];
            modeHash -= hashMultAgents[i];
         }
         break;
      }
      return modeHash != oldModeHash;
   }
   
   public StateThresh clone() {
      StateThresh cpy;
      try {
         cpy = (StateThresh)super.clone ();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalError();
      }
      cpy.modeVectorAgents = modeVectorAgents.clone ();
      return cpy;
   }
}
