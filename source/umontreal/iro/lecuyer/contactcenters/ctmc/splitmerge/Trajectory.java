package umontreal.iro.lecuyer.contactcenters.ctmc.splitmerge;

import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCTMC;
import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCounters;
import umontreal.iro.lecuyer.contactcenters.ctmc.TransitionType;
import umontreal.ssj.stat.AccumulateWithTimes;

public class Trajectory implements Cloneable {
   private CallCenterCounters m_counters;
   private AccumulateWithTimes accNumTraj = new AccumulateWithTimes();
   protected int[] lowerStaffing;
   protected int[] upperStaffing;
   private int[][] typeToGroupMap;
   private int numSplit, numMerge;
   private int trSplit = -1;
   protected boolean[] canSplit;
   protected int numCanSplit = 0;
   protected int numDiffStaffings;
   
   private List<PeriodData> periodData = new ArrayList<PeriodData>();
   
   public Trajectory (CallCenterCTMC ctmc,
         int[][] typeToGroupMap,
         CallCenterCounters counters,
         int[] lowerStaffing, int[] upperStaffing) {
      if (ctmc.getNumAgentGroups () != lowerStaffing.length)
         throw new IllegalArgumentException ("Invalid length of lowerStaffing");
      if (lowerStaffing.length != upperStaffing.length)
         throw new IllegalArgumentException (
               "lowerStaffing and upperStaffing must have the same length");
      this.lowerStaffing = lowerStaffing.clone ();
      this.upperStaffing = upperStaffing.clone ();
      this.typeToGroupMap = typeToGroupMap;
      this.m_counters = counters;
      // checkSingleStaffing();
      canSplit = new boolean[lowerStaffing.length];
      // if (singleStaffing)
      // numCanSplit = 0;
      // else {
      initCanSplit (ctmc);
      // }
      accNumTraj.update (0, 1);
   }
   
   private void initCanSplit (CallCenterCTMC ctmc) {
      numDiffStaffings = 0;
      numCanSplit = 0;
      for (int i = 0; i < canSplit.length; i++) {
         if (lowerStaffing[i] != upperStaffing[i])
            ++numDiffStaffings;
         int numNeededAgents = ctmc.getNumContactsInServiceI (i) + 1;
         canSplit[i] = numNeededAgents > lowerStaffing[i]
               && numNeededAgents <= upperStaffing[i];
         if (canSplit[i])
            ++numCanSplit;
      }
   }
   
   public void storePeriod () {
      periodData.add (new PeriodData (this.m_counters, this.lowerStaffing, this.upperStaffing));
   }
   
   public void changePeriod (CallCenterCTMC ctmc, CallCenterCounters counters,
         int[] lowerStaffing1, int[] upperStaffing1, int p) {
      storePeriod ();
      this.lowerStaffing = lowerStaffing1.clone ();
      this.upperStaffing = upperStaffing1.clone ();
      this.m_counters = counters;
      initCanSplit (ctmc);
   }   

   public void update (CallCenterCTMC ctmc, TransitionType type) {
      m_counters.collectStat (ctmc, type);
      if (numDiffStaffings > 0) {
         final int lastI = ctmc.getLastSelectedAgentGroup ();
         switch (type) {
         case ARRIVALSERVED:
         case ENDSERVICENODEQUEUE:
            int numNeededAgents = ctmc.getNumContactsInServiceI (lastI) + 1;
            boolean b = numNeededAgents > lowerStaffing[lastI]
                                                        && numNeededAgents <= upperStaffing[lastI];
            if (b && !canSplit[lastI])
               ++numCanSplit;
            if (!b && canSplit[lastI])
               --numCanSplit;
            canSplit[lastI] = b;
            assert checkCanSplit (ctmc);
         }
      }
   }
   
   public int getNumDiffStaffings() {
      return numDiffStaffings;
   }
   
   public boolean canSplit (CallCenterCTMC ctmc) {
      assert checkNumDiffStaffings ();
      assert checkCanSplit (ctmc);
      if (numDiffStaffings == 0)
         return false;
      if (numCanSplit == 0)
         return false;
      return true;
   }

   public boolean canSplit (CallCenterCTMC ctmc, int k) {
      if (!canSplit (ctmc))
         return false;
      for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
         final int i = typeToGroupMap[k][idx];
         if (canSplit[i])
            return true;
         final int numNeededAgents = ctmc.getNumContactsInServiceI (i) + 1;
         if (numNeededAgents <= lowerStaffing[i])
            return false;
      }      
      return false;
   }
   
   private boolean checkCanSplit (CallCenterCTMC ctmc) {
      int n = 0;
      for (int i = 0; i < canSplit.length; i++) {
         final int numNeededAgents = ctmc.getNumContactsInServiceI (i) + 1;
         boolean b = numNeededAgents > lowerStaffing[i]
                                                     && numNeededAgents <= upperStaffing[i];
         if (canSplit[i] != b)
            return false;
         if (b)
            ++n;
      }
      return n == numCanSplit;
   }
   
   private boolean checkNumDiffStaffings () {
      int n = 0;
      for (int i = 0; i < canSplit.length; i++) {
         if (lowerStaffing[i] != upperStaffing[i])
            ++n;
      }
      return n == numDiffStaffings;
   }

   public int getAgentGroup (CallCenterCTMC ctmc, int k) {
      for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
         final int i = typeToGroupMap[k][idx];
         // Determines the minimal number of agents needed in group i for
         // the contact to be sent into this group
         final int numNeededAgents = ctmc.getNumContactsInServiceI (i) + 1;
         if (numNeededAgents > upperStaffing[i])
            // The number of required agents is larger than the upper bound
            // on the number of agents so the contact is never sent
            // to this agent group.
            continue;
         return i;
      }
      return ctmc.getNumAgentGroups ();
   }

   public Trajectory[] split (CallCenterCTMC ctmc, int k) {
      Trajectory[] children = new Trajectory[lowerStaffing.length + 1];
      boolean first = true;
      boolean atLeastTwo = false;
      boolean done = false;
      final int[] lowerStaffing1 = this.lowerStaffing;
      final int[] upperStaffing1 = this.upperStaffing;
      final boolean[] canSplit1 = this.canSplit;
      int numCanSplit1 = this.numCanSplit;
      int numDiffStaffings1 = this.numDiffStaffings;
      int oldTrSplit = trSplit;
      int lastAddedChildIndex = -1;
      for (int idx = 0; idx < typeToGroupMap[k].length && !done; idx++) {
         final int i = typeToGroupMap[k][idx];
         // Determines the minimal number of agents needed in group i for
         // the contact to be sent into this group
         final int numNeededAgents = ctmc.getNumContactsInServiceI (i) + 1;
         if (numNeededAgents > upperStaffing1[i])
            // The number of required agents is larger than the upper bound
            // on the number of agents so the contact is never sent
            // to this agent group.
            continue;
         canSplit1[i] = false;
         lastAddedChildIndex = i;
         if (first) {
            children[i] = this;
            children[i].canSplit = canSplit1.clone ();
            children[i].lowerStaffing = lowerStaffing1.clone ();
            children[i].upperStaffing = upperStaffing1.clone ();
            first = false;
         }
         else {
            children[i] = clone();
            System.arraycopy (canSplit1, 0, children[i].canSplit, 0, canSplit1.length);
            System.arraycopy (lowerStaffing1, 0, children[i].lowerStaffing, 0, lowerStaffing1.length);
            System.arraycopy (upperStaffing1, 0, children[i].upperStaffing, 0, upperStaffing1.length);
            atLeastTwo = true;
         }
         children[i].numDiffStaffings = numDiffStaffings1;

         if (children[i].lowerStaffing[i] != children[i].upperStaffing[i]) {
            children[i].lowerStaffing[i] = Math.max (numNeededAgents,
                  lowerStaffing1[i]);
            if (children[i].lowerStaffing[i] == children[i].upperStaffing[i])
               --children[i].numDiffStaffings;
         }
         assert children[i].checkNumDiffStaffings ();
         if (numNeededAgents <= lowerStaffing1[i])
            // The contact is always sent to agent group i
            done = true;
         else {
            upperStaffing1[i] = Math.min (numNeededAgents - 1, upperStaffing1[i]);
            --numCanSplit1;
            if (lowerStaffing1[i] == upperStaffing1[i])
               --numDiffStaffings1;
         }
         children[i].numCanSplit = numCanSplit1;
         //if (children[i].lowerStaffing[i] == children[i].upperStaffing[i])
         //   children[i].checkSingleStaffing ();
      }

      if (!done) {
         int i = children.length - 1;
         lastAddedChildIndex = i;
         if (first) {
            children[i] = this;
            children[i].canSplit = canSplit1.clone ();
            children[i].lowerStaffing = lowerStaffing1.clone ();
            children[i].upperStaffing = upperStaffing1.clone ();
            first = false;
         }
         else {
            children[i] = clone();
            System.arraycopy (canSplit1, 0, children[i].canSplit, 0, canSplit1.length);
            System.arraycopy (lowerStaffing1, 0, children[i].lowerStaffing, 0, lowerStaffing1.length);
            System.arraycopy (upperStaffing1, 0, children[i].upperStaffing, 0, upperStaffing1.length);
            atLeastTwo = true;
         }
         children[i].numCanSplit = numCanSplit1;
         children[i].numDiffStaffings = numDiffStaffings1;
         assert children[i].checkNumDiffStaffings ();
         //children[i].checkSingleStaffing ();
      }

      if (atLeastTwo) {
         for (int i = 0; i < children.length; i++)
            if (children[i] != null) {
               ++children[i].numSplit;
               children[i].trSplit = ctmc.getNumTransitionsDone ();
            }
         --children[lastAddedChildIndex].numSplit;
         children[lastAddedChildIndex].trSplit = oldTrSplit;
      }
      return children;
   }

   public Trajectory[] splitNewPeriod (CallCenterCTMC ctmc) {
      int[] maxAdd = new int[ctmc.getNumAgentGroups ()];
      double[][] ranksGT = ctmc.getRanksGT ();
      for (int i = 0; i < ranksGT.length; i++) {
         for (int k = 0; k < ranksGT[i].length; k++) {
            if (Double.isInfinite (ranksGT[i][k]))
                  continue;
            maxAdd[i] += ctmc.getNumContactsInQueue (k);
         }
         maxAdd[i] += ctmc.getNumContactsInServiceI (i);
      }
//      int[] minStaffingSplit = new int[maxAdd.length];
//      int[] maxStaffingSplit = new int[maxAdd.length];
//      for (int i = 0; i < maxAdd.length; i++) {
//         final int maxAgents = ctmc.getNumContactsInServiceI (i) + maxAdd[i];
//         minStaffingSplit[i] = Math.max (lowerStaffing[i], maxAgents);
//         maxStaffingSplit[i] = Math.min (upperStaffing[i], maxAgents);
//      }
      return splitNewPeriod (ctmc, maxAdd);
   }
   
   protected Trajectory[] splitNewPeriod (CallCenterCTMC ctmc, int[] maxAgentsSplit) {
      List<Trajectory> traj = new ArrayList<Trajectory>();
      boolean hasStaffing = true;
      boolean first = true;
      int[] lowerStaffing1 = this.lowerStaffing;
      int[] upperStaffing1 = this.upperStaffing;
      int[] staffingIdx = new int[lowerStaffing1.length];
      for (int i = 0; i < staffingIdx.length; i++) {
         if (lowerStaffing1[i] > maxAgentsSplit[i])
            staffingIdx[i] = -1;
         else
            staffingIdx[i] = 0;
      }
      while (hasStaffing) {
         int[] lowerStaffingTraj = new int[staffingIdx.length];
         int[] upperStaffingTraj = new int[staffingIdx.length];
         for (int i = 0; i < lowerStaffing1.length; i++) {
            if (staffingIdx[i] == -1) {
               lowerStaffingTraj[i] = lowerStaffing1[i];
               upperStaffingTraj[i] = upperStaffing1[i];
            }
            else if (staffingIdx[i] == Integer.MAX_VALUE) {
               lowerStaffingTraj[i] = maxAgentsSplit[i] + 1;
               upperStaffingTraj[i] = upperStaffing1[i];
            }
            else {
               lowerStaffingTraj[i] = lowerStaffing1[i] + staffingIdx[i];
               upperStaffingTraj[i] = lowerStaffingTraj[i];
            }
         }
         Trajectory child;
         if (first) {
            child = this;
            child.canSplit = canSplit.clone ();
            child.lowerStaffing = lowerStaffingTraj.clone ();
            child.upperStaffing = upperStaffingTraj.clone ();
            first = false;
         }
         else {
            child = clone();
            System.arraycopy (canSplit, 0, child.canSplit, 0, canSplit.length);
            System.arraycopy (lowerStaffingTraj, 0, child.lowerStaffing, 0, lowerStaffing1.length);
            System.arraycopy (upperStaffingTraj, 0, child.upperStaffing, 0, upperStaffing1.length);
         }
         child.initCanSplit (ctmc);
         traj.add (child);
         
         hasStaffing = false;
         for (int idx = staffingIdx.length - 1; idx >= 0 && !hasStaffing; --idx) {
            if (staffingIdx[idx] == -1)
               continue;
            final int min = 0, max;
            if (upperStaffing1[idx] <= maxAgentsSplit[idx])
               max = upperStaffing1[idx] - lowerStaffing1[idx];
            else
               max = maxAgentsSplit[idx] - lowerStaffing1[idx] + 1;
            if (staffingIdx[idx] < max && staffingIdx[idx] != Integer.MAX_VALUE) {
               ++staffingIdx[idx];
               if (staffingIdx[idx] == max && upperStaffing1[idx] > maxAgentsSplit[idx])
                  staffingIdx[idx] = Integer.MAX_VALUE;
               for (int idx2 = idx + 1; idx2 < staffingIdx.length; ++idx2)
                  staffingIdx[idx2] = min;
               hasStaffing = true;
            }
         }
      }
      
      return traj.toArray (new Trajectory[traj.size ()]);
   }
   
   public int[][] getTypeToGroupMap() {
      return typeToGroupMap;
   }

   public CallCenterCounters getCounters () {
      return m_counters;
   }
   
   public CallCenterCounters getCounters (int p) {
      return periodData.get (p).counters;
   }
   
   public AccumulateWithTimes getStatNumTrajectories() {
      return accNumTraj;
   }

   public int[] getLowerStaffing () {
      return lowerStaffing;
   }
   
   public int[] getLowerStaffing (int p) {
      return periodData.get (p).lowerStaffing;
   }

   public int[] getUpperStaffing () {
      return upperStaffing;
   }
   
   public int[] getUpperStaffing (int p) {
      return periodData.get (p).upperStaffing;
   }

   public void newMerge () {
      ++numMerge;
   }

   public int getNumSplit () {
      return numSplit;
   }

   public int getNumMerge () {
      return numMerge;
   }
   
   public int getSplitTransition() {
      return trSplit;
   }

   // private void checkSingleStaffing() {
   // singleStaffing = true;
   // for (int i = 0; i < lowerStaffing.length && singleStaffing; i++)
   // if (lowerStaffing[i] != upperStaffing[i])
   // singleStaffing = false;
   // }
   
   public Trajectory clone() {
      Trajectory cpy;
      try {
         cpy = (Trajectory) super.clone ();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalError (e.toString ());
      }
      cpy.m_counters = m_counters.clone ();
      cpy.accNumTraj = accNumTraj.clone ();
      cpy.lowerStaffing = lowerStaffing.clone ();
      cpy.upperStaffing = upperStaffing.clone ();
      cpy.canSplit = canSplit.clone ();
      cpy.periodData = new ArrayList<PeriodData>();
      cpy.periodData.addAll (periodData);
      return cpy;
   }
   
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append ("{counters=").append (m_counters.toString ());
      sb.append (", staffings=[");
      for (int i = 0; i < lowerStaffing.length; i++) {
         if (i > 0)
            sb.append (", ");
         if (lowerStaffing[i] == upperStaffing[i])
            sb.append (lowerStaffing[i]);
         else
            sb.append (lowerStaffing[i]).append ('-').append (upperStaffing[i]);
      }
      sb.append ("]}");
      return sb.toString ();
   }
   
   private static class PeriodData {
      private CallCenterCounters counters;
      private int[] lowerStaffing;
      private int[] upperStaffing;
      
      public PeriodData (CallCenterCounters counters, int[] lowerStaffing,
            int[] upperStaffing) {
         super ();
         this.counters = counters;
         this.lowerStaffing = lowerStaffing;
         this.upperStaffing = upperStaffing;
      }
   }
}
