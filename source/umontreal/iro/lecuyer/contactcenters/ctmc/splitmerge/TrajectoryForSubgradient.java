package umontreal.iro.lecuyer.contactcenters.ctmc.splitmerge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCTMC;
import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCounters;

public class TrajectoryForSubgradient extends Trajectory {
   private boolean oneSplitDone = false;
   
   public TrajectoryForSubgradient (CallCenterCTMC ctmc,
         int[][] typeToGroupMap, CallCenterCounters counters,
         int[] lowerStaffing, int[] upperStaffing) {
      super (ctmc, typeToGroupMap, counters, lowerStaffing, upperStaffing);
   }
   
   @Override
   public void changePeriod (CallCenterCTMC ctmc,
         CallCenterCounters countersProto, int[] lowerStaffing,
         int[] upperStaffing, int p) {
      if (oneSplitDone)
         super.changePeriod (ctmc, countersProto, lowerStaffing, lowerStaffing, p);
      else
         super.changePeriod (ctmc, countersProto, lowerStaffing, upperStaffing, p);
   }

   @Override
   protected Trajectory[] splitNewPeriod (CallCenterCTMC ctmc,
         int[] maxAgentsSplit) {
      if (oneSplitDone) {
         System.arraycopy (lowerStaffing, 0, upperStaffing, 0, lowerStaffing.length);
         Arrays.fill (canSplit, false);
         numCanSplit = 0;
         numDiffStaffings = 0;
         return new Trajectory[] { this };
      }
      List<Trajectory> traj = new ArrayList<Trajectory>();
      int[] lowerStaffing = this.lowerStaffing;
      int[] upperStaffing = this.upperStaffing;
      
      this.lowerStaffing = lowerStaffing.clone ();
      this.upperStaffing = upperStaffing.clone ();
      this.canSplit = new boolean[lowerStaffing.length];
      traj.add (this);
      for (int i = 0; i < lowerStaffing.length; i++) {
         if (lowerStaffing[i] > maxAgentsSplit[i])
            continue;
         if (this.lowerStaffing[i] == this.upperStaffing[i])
            continue;
         this.lowerStaffing[i] = this.upperStaffing[i];
         this.canSplit[i] = false;
         --this.numCanSplit;
         --this.numDiffStaffings;
         final int max = Math.min (maxAgentsSplit[i], upperStaffing[i]);
         for (int n = lowerStaffing[i] + 1; n <= max; n++) {
            TrajectoryForSubgradient child = clone();
            System.arraycopy (lowerStaffing, 0, child.lowerStaffing, 0, lowerStaffing.length);
            System.arraycopy (lowerStaffing, 0, child.upperStaffing, 0, lowerStaffing.length);
            child.lowerStaffing[i] = n;
            child.upperStaffing[i] = n;
            Arrays.fill (child.canSplit, false);
            child.numCanSplit = 0;
            child.numDiffStaffings = 0;
            child.oneSplitDone = true;
            traj.add (child);
         }
         if (maxAgentsSplit[i] < upperStaffing[i]) {
            TrajectoryForSubgradient child = clone();
            System.arraycopy (lowerStaffing, 0, child.lowerStaffing, 0, lowerStaffing.length);
            System.arraycopy (lowerStaffing, 0, child.upperStaffing, 0, lowerStaffing.length);
            child.lowerStaffing[i] = maxAgentsSplit[i] + 1;
            child.upperStaffing[i] = upperStaffing[i];
            Arrays.fill (child.canSplit, false);
            if (child.lowerStaffing[i] == child.upperStaffing[i]) {
               child.numCanSplit = 0;
               child.numDiffStaffings = 0;
            }
            else {
               child.numCanSplit = 1;
               child.numDiffStaffings = 1;
            }
            child.oneSplitDone = true;
            traj.add (child);
         }
      }
      
      return traj.toArray (new Trajectory[traj.size ()]);
   }

   @Override
   public Trajectory[] split (CallCenterCTMC ctmc, int k) {
      if (numDiffStaffings <= 1)
         return super.split (ctmc, k);
      int oldNumSplit = getNumSplit ();
      Trajectory[] children = super.split (ctmc, k);
      if (children != null) {
         for (int i = 0; i < children.length; i++) {
            if (children[i] == null)
               continue;
            if (children[i].getNumSplit () == oldNumSplit)
               continue;
            ((TrajectoryForSubgradient)children[i]).oneSplitDone = true;
            if (i >= children.length - 1)
               continue;
            for (int l = 0; l < children.length - 1; l++) {
               if (l == i)
                  continue;
               // final int numNeededAgents = ctmc.getNumContactsInServiceI (l);
               // children[i].upperStaffing[l] = Math.min (numNeededAgents,
               // children[i].upperStaffing[l]);
               children[i].upperStaffing[l] = children[i].lowerStaffing[l];
            }
            Arrays.fill (children[i].canSplit, false);
            if (children[i].lowerStaffing[i] == children[i].upperStaffing[i]) {
               children[i].numCanSplit = 0;
               children[i].numDiffStaffings = 0;
            }
            else {
               children[i].numCanSplit = 1;
               children[i].numDiffStaffings = 1;
               children[i].canSplit[i] = true;
            }
         }
      }
      return children;
   }
   
   public TrajectoryForSubgradient clone() {
      return (TrajectoryForSubgradient)super.clone ();
   }
}
