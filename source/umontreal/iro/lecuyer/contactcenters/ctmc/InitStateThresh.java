package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.CTMCRepSimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.router.RoutingTableUtils;

import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

/**
 * Used to initialize vectors of thresholds automatically. This program provides
 * a method {@link #getThresholds(CallCenterCTMC,int,int,boolean)} returning a matrix of
 * thresholds. The program can also be called from the command-line to perform
 * the initialization.
 */
public class InitStateThresh {

   /**
    * Returns a matrix with \texttt{numStateThresh*numGroupSlices} rows
    * representing vectors of thresholds. Columns $i=0, \ldots, I-1$ of the
    * returned matrix give thresholds for the number of agents while column $I$
    * gives thresholds on the waiting queue. The vectors of thresholds are
    * constructed based on the CTMC model \texttt{ctmc}. The constructed matrix
    * has $M=$~\texttt{numGroupSlices} sets of \texttt{numStateThresh} vectors
    * of thresholds. Set $m$, for $m=0,\ldots,M-1$, has thresholds with number
    * of agents in $(\lfloor m\tilde N_i/M\rfloor, \lfloor (m+1)\tilde
    * N_i/M\rfloor]$.
    * 
    * @param ctmc
    *           the call center CTMC model.
    * @param numStateThresh
    *           the number of vectors of thresholds on the state.
    * @param numGroupSlices
    *           the number of slices for the number of agents in groups.
    * @return the 2D array of thresholds.
    * @exception NullPointerException
    *               if \texttt{ctmc} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{numStateThresh} or \texttt{numGroupSlices} are
    *               smaller than 1.
    */
   public static int[][] getThresholds (CallCenterCTMC ctmc,
         int numStateThresh, int numGroupSlices,
         boolean threshOnQueueSize) {
      if (numStateThresh < 1)
         throw new IllegalArgumentException (
               "Invalid number of state thresholds");
      if (numGroupSlices < 1)
         throw new IllegalArgumentException ("Invalid number of gorup slices");
      final double[] mu = getMu (ctmc);
      final double nu = getNu (ctmc);

      int[][] threshAgents = new int[numStateThresh * numGroupSlices][ctmc
            .getNumAgentGroups () + 1];

      int[][] adj = getOverflowAdj (ctmc);
      double[] weights = new double[adj.length];
      int[] prec = new int[adj.length];
      dijstra (adj, weights, prec);

      // boolean diffMax = false;
      // for (int i = 0; i < ctmc.getNumAgentGroups () && !diffMax; i++)
      // if (ctmc.getNumAgents (i) != ctmc.getMaxNumAgents (i))
      // diffMax = true;
      // int[] numThreshPerGroup = getNumThreshPerGroup (ctmc, mu, nu, weights,
      // diffMax, numStateThresh);
      int[] minThresh = new int[ctmc.getNumAgentGroups () + 1];
      int[] maxThresh = new int[ctmc.getNumAgentGroups () + 1];
      for (int slice = 0, r = 0; slice < numGroupSlices; slice++) {
         for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
            minThresh[i] = (int) (ctmc.getMaxNumAgents (i) * (double) slice / numGroupSlices);
            maxThresh[i] = (int) (ctmc.getMaxNumAgents (i) * (slice + 1.0) / numGroupSlices);
         }
         if (threshOnQueueSize)
            minThresh[minThresh.length - 1] = 0;
         else
            minThresh[minThresh.length - 1] = ctmc.getQueueCapacity ();
         maxThresh[maxThresh.length - 1] = ctmc.getQueueCapacity ();
         int[] numThreshPerGroup = getNumThreshPerGroup (ctmc, mu, nu, weights,
               minThresh, maxThresh, numStateThresh);

         // boolean firstThreshGroup = true;
         for (int grp = 0; grp < numThreshPerGroup.length; grp++) {
            if (numThreshPerGroup[grp] == 0)
               continue;
            // int ngrp = numThreshPerGroup.length;
            // if (diffMax)
            // --ngrp;
            boolean lastThreshGroup = true;
            for (int grp2 = grp + 1; grp2 < numThreshPerGroup.length
                  && lastThreshGroup; grp2++) {
               if (numThreshPerGroup[grp2] > 0)
                  lastThreshGroup = false;
            }
            // if (diffMax && grp == numThreshPerGroup.length - 1) {
            // for (int jgrp = 0; jgrp < numThreshPerGroup[grp]; jgrp++, r++) {
            // final double ratio = getRatio (firstThreshGroup, jgrp,
            // numThreshPerGroup[grp]);
            // for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
            // final int min = ctmc.getNumAgents (i);
            // final int max = ctmc.getMaxNumAgents (i);
            // threshAgents[r][i] = min + (int) (ratio * (max - min));
            // }
            // final int max2 = ctmc.getQueueCapacity ();
            // threshAgents[r][ctmc.getNumAgentGroups ()] = max2;
            // }
            // }
            // else {
            final double w = grp + 2;

            for (int jgrp = 0; jgrp < numThreshPerGroup[grp]; jgrp++, r++) {
               final double ratio = getRatio (false, jgrp,
                     numThreshPerGroup[grp]);
               final boolean lastThreshGroupAndLastJ = lastThreshGroup &&
               jgrp < numThreshPerGroup[grp];
               for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
                  // final int max;
                  // if (diffMax
                  // && numThreshPerGroup[numThreshPerGroup.length - 1] == 0)
                  // max = ctmc.getMaxNumAgents (i);
                  // else
                  // max = ctmc.getNumAgents (i);
                  if (weights[ctmc.getNumContactTypes () + 1 + i] == w)
                     threshAgents[r][i] = minThresh[i]
                           + (int) ((maxThresh[i] - minThresh[i]) * ratio);
                  else if (lastThreshGroupAndLastJ
                        || weights[ctmc.getNumContactTypes () + 1 + i] < w)
                     threshAgents[r][i] = maxThresh[i];
                  else
                     threshAgents[r][i] = minThresh[i];
               }
               // final int max2 = ctmc.getQueueCapacity ();
               final int I = ctmc.getNumAgentGroups ();
               if (weights[weights.length - 1] == w)
                  threshAgents[r][I] = minThresh[I]
                        + (int) ((maxThresh[I] - minThresh[I]) * ratio);
               else if (lastThreshGroupAndLastJ || weights[weights.length - 1] < w)
                  threshAgents[r][I] = maxThresh[I];
               else
                  threshAgents[r][I] = minThresh[I];
            }
         }
         // firstThreshGroup = false;
      }
      // if (threshAgents[threshAgents.length - 1][ctmc.getNumAgentGroups ()] !=
      // ctmc.getQueueCapacity ())
      // throw new IllegalStateException ("Last threshold on queue size, "
      // + threshQueue[threshQueue.length - 1]
      // + ", smaller than queue capacity " + ctmc.getQueueCapacity ());
      // for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
      // if (threshAgents[threshAgents.length - 1][i] != ctmc
      // .getMaxNumAgents (i))
      // throw new IllegalStateException (
      // "Last threshold on number of agents in group " + i + ", "
      // + threshAgents[threshAgents.length - 1][i]
      // + ", smaller than the maximal number of agents "
      // + ctmc.getMaxNumAgents (i));
      // }
      return threshAgents;
   }

   // Computes the maximal service rate, for each agent group
   private static double[] getMu (CallCenterCTMC ctmc) {
      double[] mu = new double[ctmc.getNumAgentGroups ()];
      for (int i = 0; i < mu.length; i++) {
         for (int k = 0; k < ctmc.getNumContactTypes (); k++) {
            final double muki = ctmc.getServiceRate (k, i);
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
         final double nuk = ctmc.getPatienceRate (k);
         if (nu < nuk)
            nu = nuk;
      }
      return nu;
   }

   // Computes and returns the adjacence matrix
   // of the overflow graph, for the given
   // call center model.
   private static int[][] getOverflowAdj (CallCenterCTMC ctmc) {
      // Each element of adj is a list of edges out of a given node
      double[][] ranksTG = ctmc.getRanksTG ();
      // One node per contact type, one node per agent group, one node for the
      // source, one node for the queue.
      int[][] adj = new int[2 + ctmc.getNumContactTypes ()
            + ctmc.getNumAgentGroups ()][];

      // Link the source to each contact type
      adj[0] = new int[ctmc.getNumContactTypes ()];
      for (int k = 0; k < ctmc.getNumContactTypes (); k++)
         adj[0][k] = k + 1;
      // No edge out of the node representing the queue
      adj[adj.length - 1] = new int[0];

      int[][][] overflowLists = RoutingTableUtils.getOverflowLists (ranksTG);
      // For each contact type, add edges to the primary agent groups
      // in the overflow list.
      for (int k = 0; k < ctmc.getNumContactTypes (); k++) {
         if (overflowLists[k].length == 0)
            adj[k + 1] = new int[0];
         else {
            final int[] ik0 = overflowLists[k][0];
            adj[k + 1] = new int[ik0.length];
            for (int i = 0; i < ik0.length; i++)
               adj[k + 1][i] = ik0[i] + ctmc.getNumContactTypes () + 1;
         }
      }

      // Create links between agent groups, and the waiting queue
      Set<Integer>[] groupEdges = newArray (ctmc.getNumAgentGroups ());
      for (int i = 0; i < groupEdges.length; i++)
         groupEdges[i] = new HashSet<Integer> ();
      for (int k = 0; k < ctmc.getNumContactTypes (); k++) {
         for (int j = 0; j < overflowLists[k].length; j++) {
            int[] ikj = overflowLists[k][j];
            int[] ikj1;
            if (j < overflowLists[k].length - 1)
               ikj1 = overflowLists[k][j + 1];
            else
               ikj1 = null;
            if (ikj1 == null) {
               final Integer q = ctmc.getNumContactTypes ()
                     + ctmc.getNumAgentGroups () + 1;
               for (int i = 0; i < ikj.length; i++)
                  groupEdges[ikj[i]].add (q);
            }
            else {
               for (int i1 = 0; i1 < ikj.length; i1++)
                  for (int i2 = 0; i2 < ikj1.length; i2++)
                     groupEdges[ikj[i1]].add (ikj1[i2]
                           + ctmc.getNumContactTypes () + 1);
            }
         }
      }

      for (int i = 0; i < groupEdges.length; i++) {
         final int idx = i + 1 + ctmc.getNumContactTypes ();
         adj[idx] = new int[groupEdges[i].size ()];
         int idx2 = 0;
         for (Integer e : groupEdges[i])
            adj[idx][idx2++] = e;
      }

      return adj;
   }

   @SuppressWarnings ("unchecked")
   private static Set<Integer>[] newArray (int size) {
      return (Set<Integer>[]) new Set[size];
   }

   // Computes the shortest path between the first node, and
   // any node of the graph with the given adjacence matrix,
   // using the Dijkstra algorithm.
   // This method assumes that each arc has a cost of 1.
   // The length of the paths are stored in outputWeights,
   // while the predecessors are put in outputPred
   private static void dijstra (int[][] adj, double[] outputWeights,
         int[] outputPred) {
      if (outputWeights.length != outputPred.length)
         throw new IllegalArgumentException ();
      if (outputWeights.length != adj.length)
         throw new IllegalArgumentException ();
      Arrays.fill (outputWeights, Double.POSITIVE_INFINITY);
      Arrays.fill (outputPred, -1);
      outputWeights[0] = 0;

      Queue<Integer> queue = new PriorityQueue<Integer> (adj.length,
            new WeightComparator (outputWeights));
      for (int k = 0; k < adj.length; k++)
         queue.add (k);

      while (!queue.isEmpty ()) {
         final int u = queue.remove ();
         final double uWeight = outputWeights[u];
         for (int k = 0; k < adj[u].length; k++) {
            final int v = adj[u][k];
            final double vWeight = outputWeights[v];
            if (vWeight > uWeight + 1) {
               queue.remove (v);
               outputWeights[v] = uWeight + 1;
               outputPred[v] = u;
               queue.add (v);
            }
         }
      }
   }

   // Weight comparator used to implement the priority queue
   // using Java's PriorityQueue, for the Dijkstra algorithm.
   private static final class WeightComparator implements Comparator<Integer> {
      private double[] weights;

      public WeightComparator (double[] weights) {
         this.weights = weights;
      }

      public int compare (Integer o1, Integer o2) {
         double w1 = weights[o1];
         double w2 = weights[o2];
         if (w1 < w2)
            return -1;
         if (w2 < w1)
            return 1;
         return 0;
      }
   }

   /**
    * Computes and returns the number of thresholds for each agent group.
    * 
    * @param ctmc
    *           the call center CTMC.
    * @param weights
    *           the lenghts of the shortest paths from the source to each node,
    *           in the overflow graph.
    * @param minThresh
    *           the minimal values of the thresholds.
    * @param maxThresh
    *           the maximal values of the thresholds.
    * @param numStateThresh
    *           the total number of thresholds.
    * @return an array giving the number of thresholds for each agent group.
    */
   private static int[] getNumThreshPerGroup (CallCenterCTMC ctmc, double[] mu,
         double nu, double[] weights, int[] minThresh, int[] maxThresh,
         int numStateThresh) {
      final double maxWeight = ArrayUtil.max (weights);
      // final int numThreshGroups = (int) maxWeight - (diffMax ? 0 : 1);
      final int numThreshGroups = (int) maxWeight - 1;
      double[] weightGroup = new double[numThreshGroups];
      double weightGroupTotal = 0;
      double maxWeightGrp = 0;
      int maxWeightGrpIdx = 0;
      for (int grp = 0; grp < numThreshGroups; grp++) {
         // if (diffMax && grp == numThreshGroups - 1) {
         // for (int i = 0; i < ctmc.getNumAgentGroups (); i++)
         // weightGroup[grp] += mu[i]
         // * (ctmc.getMaxNumAgents (i) - ctmc.getNumAgents (i));
         // }
         // else {
         final double w = grp + 2;
         for (int i = 0; i < ctmc.getNumAgentGroups (); i++) {
            final int delta = maxThresh[i] - minThresh[i];
            if (weights[ctmc.getNumContactTypes () + 1 + i] == w)
               weightGroup[grp] += delta * mu[i];
         }
         final int delta2 = maxThresh[ctmc.getNumAgentGroups ()]
               - minThresh[ctmc.getNumAgentGroups ()];
         if (weights[weights.length - 1] == w)
            weightGroup[grp] += delta2 * nu;
         // }
         weightGroupTotal += weightGroup[grp];
         if (weightGroup[grp] > maxWeightGrp) {
            maxWeightGrp = weightGroup[grp];
            maxWeightGrpIdx = grp;
         }
      }
      for (int grp = 0; grp < weightGroup.length; grp++)
         weightGroup[grp] /= weightGroupTotal;
      int[] numThreshPerGroup = new int[weightGroup.length];
      int sum = 0;
      for (int grp = 0; grp < weightGroup.length; grp++) {
         numThreshPerGroup[grp] = (int) (weightGroup[grp] * numStateThresh);
         sum += numThreshPerGroup[grp];
      }
      numThreshPerGroup[maxWeightGrpIdx] += numStateThresh - sum;
      //numThreshPerGroup[numThreshPerGroup.length - 1] += numStateThresh - sum;
      return numThreshPerGroup;
   }

   private static double getRatio (boolean firstThreshGroup, int jgrp, int num) {
      if (firstThreshGroup) {
         if (num == 1)
            return 0;
         else
            return jgrp / (num - 1.0);
      }
      else
         return (jgrp + 1.0) / num;
   }

   /**
    * Main method of the class, to be called from the command-line. This method
    * accepts the name of the parameter file for the call center, the name of
    * the parameter file for the experiments, the number of vectors of
    * thresholds to create, and an ouput parameter file. The program computes
    * the vectors of thresholds for each main period in the model, and outputs a
    * modified version of the given experiment parameter file, with the vectors
    * of thresholds.
    * 
    * @param args
    *           the command-line arguments of the program.
    */
   public static void main (String[] args) {
      if (args.length != 5) {
         System.err
               .println ("Usage: java InitStateThresh ccParams simParams numStateThresh numGroupSlices outputRepSimParams");
         System.exit (1);
      }

      String ccPsFn = args[0];
      String simPsFn = args[1];
      int numStateThresh = Integer.parseInt (args[2]);
      int numGroupSlices = Integer.parseInt (args[3]);
      File outputFile = new File (args[4]);

      CallCenterParamsConverter cnv = new CallCenterParamsConverter ();
      CallCenterParams ccParams = cnv.unmarshalOrExit (new File (ccPsFn));
      SimParamsConverter cnvSim = new SimParamsConverter ();
      CTMCRepSimParams simParams = (CTMCRepSimParams) cnvSim
            .unmarshalOrExit (new File (simPsFn));

      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());
      BasicCallCenterCTMCSimMP sim;
      try {
         sim = new BasicCallCenterCTMCSimMP (ccParams, simParams);
      }
      catch (CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }
      catch (CTMCCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }

      int P = sim.getNumMainPeriods ();
      simParams.getThresholds ().clear ();
      for (int p = 0; p < P; p++) {
         int[][] thresh = getThresholds (sim.getCTMC (p), numStateThresh,
               numGroupSlices, true);
         simParams.getThresholds ().add (ArrayConverter.marshalArray (thresh));
      }
      cnvSim.marshalOrExit (simParams, outputFile);
   }
}
