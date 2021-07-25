package umontreal.iro.lecuyer.contactcenters.ctmc;

import umontreal.ssj.probdist.GeometricDist;

/**
 * Encapsulates thresholds on the queue size with
 * the corresponding transition rates and
 * geometric distributions for the number of successive
 * self jumps preceding any generated transition.
 */
public class QueueSizeThresh {
   private double[] jumpRateQ;
   private int[] queueSizeThresh;
   private GeometricDist[] numFalseTrDist;

   /**
    * Constructs a new manager for queue size thresholds, for
    * a CTMC with the given jump rate \texttt{jumpRate}, representing a call
    * center with maximal abandonment rate \texttt{nu},
    * and maximal queue capacity \texttt{queueCapacity}.
    * The computed thresholds is an increasing sequence of
    * \texttt{numThresh}
    * integers distributed evenly on the
    * interval $[0, H]$, where $H$ is the queue capacity.
    * @param jumpRate the jump rate for the CTMC.
    * @param nu the maximal abandonment rate.
    * @param queueCapacity the queue capacity.
    * @param numThresh the number of thresholds to create.
    */
   public QueueSizeThresh (double jumpRate, double nu, int queueCapacity, int numThresh) {
      final int n = Math.min (numThresh, queueCapacity + 1);
      if (n < 1)
         throw new IllegalArgumentException();
      final boolean oneThresh;
      if (n == 1 || nu == 0)
         oneThresh = true;
      else {
//         double jumpRateMinNQ = jumpRate - queueCapacity * nu;
//         double prob = jumpRateMinNQ / jumpRate;
//         double nfMean = (1 - prob) / prob;
//         oneThresh = nfMean < 1;
         oneThresh = false;
      }
      if (oneThresh) {
         jumpRateQ = new double[] { jumpRate };
         queueSizeThresh = new int[] { queueCapacity };
         numFalseTrDist = new GeometricDist[] {
           new GeometricDist (1)    
         };
      }
      else {
         jumpRateQ = new double[n];
         queueSizeThresh = new int[jumpRateQ.length];
         numFalseTrDist = new GeometricDist[jumpRateQ.length];
         //jumpRateQ[0] = jumpRate - queueCapacity*nu;
         //queueSizeThresh[0] = 0;
         int delta = queueCapacity / (n - 1);
         for (int i = 0; i < n - 1; i++) {
            queueSizeThresh[i] = delta*i;
            jumpRateQ[i] = jumpRate - (queueCapacity - queueSizeThresh[i])*nu; 
         }
         jumpRateQ[jumpRateQ.length - 1] = jumpRate;
         queueSizeThresh[queueSizeThresh.length - 1] = queueCapacity;
         for (int i = 0; i < n; i++)
            numFalseTrDist[i] = new GeometricDist (jumpRateQ[i] / jumpRate);
      }
   }

   /**
    * Returns the number of thresholds managed by
    * this object.
    * @return the number of managed thresholds.
    */
   public int getNumThresholds () {
      return queueSizeThresh.length;
   }

   /**
    * Returns the maximal transition rate if
    * the queue size is smaller than or equal to
    * the the threshold with index \texttt{r}. 
    * @param r the index of the tested threshold.
    * @return the transition rate.
    */
   public double getJumpRate (int r) {
      return jumpRateQ[r];
   }

   /**
    * Returns the threshold on the queue size with
    * index \texttt{r}.
    * @param r the index of the threshold.
    * @return the threshold corresponding to the index.
    */
   public int getQueueSizeThresh (int r) {
      return queueSizeThresh[r];
   }

   /**
    * Returns the geometric distribution for the
    * successive number of self jumps before any transition,
    * while the queue size is smaller than or equal to
    * to threshold with index \texttt{r}.
    * @param r the index of the queue size threshold.
    * @return the distribution of the successive number of self jumps
    * before any transition.
    */
   public GeometricDist getNumFalseTrDist (int r) {
      return numFalseTrDist[r];
   }

   /**
    * Returns the smallest index for which
    * the queue size is smaller than or equal to the
    * corresponding threshold, given that
    * the current index is \texttt{qidx}.
    * The given index is used as a starting point
    * for searching the correct index.
    * @param qidx the current threshold index.
    * @param queueSize the current queue size.
    * @return the appropriate queue size threshold index.
    */
   public int updateQIdx (int qidx, int queueSize) {
      int nqidx = Math.min (qidx, queueSizeThresh.length - 1);
      while (queueSize > queueSizeThresh[nqidx])
         ++nqidx;
      while (nqidx > 0 && queueSize <= queueSizeThresh[nqidx - 1])
         --nqidx;
      return nqidx;
   }
}
