package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcessWithTimeIntervals;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.rng.RandomStream;

/**
 * Provides methods to determine
 * transitions at which arrivl rates
 * change in order to have piecewise-constant
 * arrival rates in the CTMC simulator.
 * By default, a simulator based on an
 * implementation of {@link CallCenterCTMC}
 * uses a fixed arrival rate for each call type.
 * Each replication, $N(T)$ transitions are
 * simulated.
 * This class provides methods to determine
 * how many transitions to simulate with each arrival
 * rate, and generate a sequence of
 * $(t, k, \lambda_k)$ tuples.
 * Each tuple indicates that the arrival rate
 * for call type~$k$ changes to $\lambda_k$
 * at transition number $t$.
 */
public class RateChangeTransitions {
   // Arrays of K arrays giving times and rates for
   // each call type
   private double[][] times;
   private double[][] rates;
   
   private int[][] trDist;
   //private int[] tmpIdx;
   private boolean hasChanges;
   
   /**
    * Constructs a new object for managing
    * changes of arrival rates for the call
    * center \texttt{cc}.
    * This constructor collects the arrival rates and
    * time of changes for any call type with an arrival
    * processs having piecewise-constant arrival rate.
    * Any call type with an arrival process other
    * than {@link PoissonArrivalProcessWithTimeIntervals}
    * is ignored; the associated arrival rate will remain
    * constant with time.
    * @param cc the call center from which to collect the information.
    */
   public RateChangeTransitions (CallCenter cc) {
      times = new double[cc.getNumInContactTypes ()][];
      rates = new double[cc.getNumInContactTypes ()][];
      trDist = new int[cc.getNumInContactTypes ()][];
      //tmpIdx = new int[trDist.length];
      for (int k = 0; k < times.length; k++) {
         ContactArrivalProcess cap = cc.getArrivalProcess (k);
         if (cap instanceof PoissonArrivalProcessWithTimeIntervals) {
            PoissonArrivalProcessWithTimeIntervals capIn = (PoissonArrivalProcessWithTimeIntervals)cap;
            times[k] = capIn.getTimes ();
            rates[k] = capIn.getExpectedArrivalRatesBInt ();
            trDist[k] = new int[times[k].length + 1];
            hasChanges = true;
         }
      }
   }
   
   /**
    * Returns \texttt{true} if and only if
    * the arrival rate for at least one call type of
    * the associated model changes with time.
    */
   public boolean hasChanges() {
      return hasChanges;
   }

   /**
    * Returns a 2D array with one row per call type.
    * Row $k$ of the returned array is created
    * by calling {@link #getTimeDist(double[],double,double)}
    * with the time of changes for call type $k$,
    * and the given values of \texttt{startingTime} and \texttt{endingTime}.
    * @param startingTime the starting time $a$.
    * @param endingTime the ending time $b$.
    * @return the 2D array of time distributions.
    */
   public double[][] getTimeDist (double startingTime, double endingTime) {
      double[][] tdist = new double[times.length][];
      if (hasChanges)
         for (int k = 0; k < times.length; k++)
            if (times[k] != null)
               tdist[k] = getTimeDist (times[k], startingTime, endingTime);
      return tdist;
   }
   
   /**
    * Constructs an array of length $L+1$
    * giving the proportion of interval $[a,b)$ taken
    * by each interval $[t_{j-1}, t_j)$.
    * Let $0\le t_0<\cdots<t_{L-1}<\infty$ be an increasing
    * sequence of times.
    * For each $j=0,\ldots,L$, the method
    * sets the element $j$ of the returned array to
    * \[\max(\min(t_j, b) - \max(t_{j-1}, a),0) / (b-a).\]
    * Here, $t_{-1}=0$ and $t_L=\infty$.
    * Each element of the returned array is in $[0,1]$, and
    * the sum of the values is 1.
    * Element $j$ of the returned array
    * gives the proportion of transitions, in
    * a uniformized CTMC, that needs to be simulated
    * with arrival rate $\lambda_j$ corresponding to
    * time interval $[t_j, t_{j+1})$.
    * The arrival rate is always 0 with time
    * $t<t_0$ and $t\ge t_{L-1}$.
    * @param times the sequence of times $t_0,\ldots,t_{L-1}$.
    * @param startingTime the starting time $a$.
    * @param endingTime the ending time $b$.
    * @return an array containing the proportion of total
    * time for each interval.
    */
   public static double[] getTimeDist (double[] times,
         double startingTime, double endingTime) {
      double[] prob = new double[times.length + 1];
      final double time = endingTime - startingTime;
      for (int j = 0; j < prob.length; j++) {
         final double i1 = j == 0 ? 0 : times[j-1];
         final double i2 = j == prob.length - 1 ? Double.POSITIVE_INFINITY : times[j];
         if (startingTime > i2 || endingTime <= i1)
            prob[j] = 0;
         else {
            final double a = Math.max (i1, startingTime);
            final double b = Math.min (i2, endingTime);
            prob[j] = (b - a) / time;
         }
      }
      return prob;
   }
   
   
   /**
    * Uses the random stream \texttt{stream}
    * to generate a random vector from the multinomial
    * distribution with parameters $n$
    * and $p_1,\ldots,p_d$, where the $p_i$'s are
    * stored in \texttt{prob}.
    * The generated vector is put in the array
    * \texttt{x}.
    * @param stream the random stream used to generate uniforms.
    * @param n the parameter $n$.
    * @param prob the vector of probabilities $p_1,\ldots,p_d$.
    * @param x the output vector.
    */
   private static void generateMultinomial (RandomStream stream, int n, double[] prob, int[] x) {
      assert prob.length == x.length;
      int sumN = n;
      double normP = 1;
      for (int j = 0; j < x.length - 1; j++) {
         final double u = stream.nextDouble ();
         if (prob[j] <= 0) {
            x[j] = 0;
            continue;
         }
         if (prob[j] < 0 || prob[j] > 1)
            throw new IllegalArgumentException
            ("prob[" + j + "]=" + prob[j] + " is not in [0,1]");
         double bp = prob[j] / normP;
         if (bp <= 0)
            x[j] = 0;
         else if (bp >= 1)
            x[j] = sumN;
         else
            x[j] = BinomialDist.inverseF (sumN, bp, u);
         sumN -= x[j];
         if (sumN == 0) {
            ++j;
            while (j < x.length - 1) {
               x[j] = 0;
               stream.nextDouble ();
            }
         }
         normP -= prob[j];
      }
      x[x.length - 1] = sumN;
   }
   
   /**
    * Generates and returns a sequence of
    * changes of arrival rates for simulating
    * a uniformized CTMC with time-varying arrival
    * rates for one or more call types.
    * For each call type $k$ for which
    * \texttt{timeDist[k]} is non-\texttt{null},
    * this method generates a vector from the multinomial
    * distribution giving the number of transitions
    * spent with each arrival rate.
    * The vectors are generated using random stream
    * \texttt{stream}, and parameter $n$ of the multinomials
    * is \texttt{ntr}, and the vector of probabilities for
    * call type \texttt{k} is given by \texttt{timeDist[k]}.
    * The method then creates a squence of objects representing
    * change of arrival rates, and sorts these objects in
    * increasing number of transition.  
    * @param stream the random stream used to generate random vectors.
    * @param timeDist the time distribution for each call type.
    * @param ntr the total number of transitions.
    * @return the sequence of changes of arrival rates.
    */
   public RateChangeInfo[] generateRateChanges (RandomStream stream, double[][] timeDist, int ntr) {
      int num = 0;
      if (hasChanges) {
         for (int k = 0; k < trDist.length; k++)
            if (timeDist[k] != null) {
               generateMultinomial (stream, ntr, timeDist[k], trDist[k]);
               num += trDist[k].length;
            }
         //Arrays.fill (tmpIdx, 0);
      }
      RateChangeInfo[] info = new RateChangeInfo[num];
      int idx = 0;
      for (int k = 0; k < trDist.length; k++) {
         if (trDist[k] == null)
            continue;
         int lastTr = 0;
         for (int j = 0; j < trDist[k].length; j++) {
            final int tr = lastTr + trDist[k][j];
            final double rate;
            if (j < trDist[k].length - 1 && trDist[k][j+1] == 0)
               rate = 0;
            else if (j < rates[k].length)
               rate = rates[k][j];
            else
               rate = 0;
            info[idx++] = new RateChangeInfo (tr, k, rate);
            lastTr = tr;
         }
      }
      Arrays.sort (info);
      
//      int lastTr = 0;
//      for (int idx = 0; idx < info.length; idx++) {
//         int bestK = -1;
//         int bestTr = -1;
//         for (int k = 0; k < trDist.length; k++) {
//            if (trDist[k] == null)
//               continue;
//            if (tmpIdx[k] >= trDist[k].length)
//               continue;
//            int tr = lastTr + trDist[k][tmpIdx[k]];
//            if (tr > bestTr) {
//               bestTr = tr;
//               bestK = k;
//            }
//         }
//         int j = tmpIdx[bestK]++;
//         double rate;
//         if (j >= rates[bestK].length)
//            rate = 0;
//         else
//            rate = rates[bestK][j];
//         info[idx] = new ChangeInfo (bestTr, bestK, rate);
//         lastTr = bestTr;
//      }
      return info;
   }
}
