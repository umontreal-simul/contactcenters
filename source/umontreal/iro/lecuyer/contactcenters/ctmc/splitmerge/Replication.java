package umontreal.iro.lecuyer.contactcenters.ctmc.splitmerge;

import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCTMC;
import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterCounters;
import umontreal.iro.lecuyer.contactcenters.ctmc.RateChangeInfo;
import umontreal.iro.lecuyer.contactcenters.ctmc.TransitionType;
import umontreal.iro.lecuyer.contactcenters.router.RoutingTableUtils;

import umontreal.iro.lecuyer.util.ArrayUtil;

public class Replication implements Cloneable {
   //private static int counter = 0;
   //private int num = counter++;
   private CallCenterCTMC m_ctmc;
   private int ctmcHash;
   private int[] hashCoeff;
   private Trajectory[] trajectories;
   private boolean[] queueNonEmpty;
   private int numNonEmptyQueues = 0;

   private int[] m_lowerStaffing;
   private int[] m_upperStaffing;
   private int[][] typeToGroupMap;
   private RateChangeInfo[] m_changes;
   private int tridx;
   private int minTr;
   private int minNumSplit;

   public Replication (CallCenterCTMC ctmc, CallCenterCounters counters,
         boolean subgradientOnly,
         int[] lowerStaffing, int[] upperStaffing,
         RateChangeInfo[] changes) {
      double[][] ranksTG = ctmc.getRanksTG ();
      typeToGroupMap = RoutingTableUtils.getTypeToGroupMap (ranksTG);
      final Trajectory traj;
      if (subgradientOnly)
         traj = new TrajectoryForSubgradient (ctmc, 
               typeToGroupMap, counters,
               lowerStaffing, upperStaffing);
      else
         traj = new Trajectory (ctmc, 
               typeToGroupMap, counters,
               lowerStaffing, upperStaffing);
      trajectories = new Trajectory[] { traj };
      this.m_lowerStaffing = lowerStaffing.clone ();
      this.m_upperStaffing = upperStaffing.clone ();
      this.m_ctmc = ctmc;
      this.m_changes = changes;
      initHashcode ();
      ctmc.setNumAgents (lowerStaffing);
      queueNonEmpty = new boolean[ctmc.getNumContactTypes ()];
      initQueueNonEmpty ();
   }
   
   private void initHashcode() {
      ctmcHash = m_ctmc.hashCodeState ();
      hashCoeff = new int[m_ctmc.getNumAgentGroups () + 2];
      hashCoeff[0] = 1;
      for (int i = 0; i < m_ctmc.getNumAgentGroups (); i++)
         hashCoeff[i + 1] = hashCoeff[i] * m_ctmc.getMaxNumAgents (i);
      hashCoeff[hashCoeff.length - 1] = hashCoeff[hashCoeff.length - 2]
            * m_ctmc.getQueueCapacity ();
   }
   
   private void initQueueNonEmpty() {
      numNonEmptyQueues = 0;
      for (int k = 0; k < queueNonEmpty.length; k++) {
         if (m_ctmc.getNumContactsInQueue (k) > 0) {
            queueNonEmpty[k] = true;
            ++numNonEmptyQueues;
         }
      }
   }
   
   public void changePeriod (CallCenterCTMC ctmcP, CallCenterCounters countersProto,
         int[] lowerStaffing, int[] upperStaffing, int p,
         RateChangeInfo[] changes) {
      ctmcP.init (m_ctmc);
      m_ctmc = ctmcP;
      minTr = m_ctmc.getNumTransitionsDone ();
      this.m_changes = changes;
      tridx = 0;
      initHashcode ();
      initQueueNonEmpty ();
      for (Trajectory traj : trajectories)
         traj.changePeriod (m_ctmc, countersProto.clone (), lowerStaffing, upperStaffing, p);
   }
   
   private boolean checkQueueNonEmpty() {
      int n = 0;
      for (int k = 0; k < queueNonEmpty.length; k++) {
         boolean b = m_ctmc.getNumContactsInQueue (k) > 0;
         if (queueNonEmpty[k] != b)
            return false;
         if (b)
            ++n;
      }
      return n == numNonEmptyQueues;
   }

   public Replication[] simulateStep (int bits) {
      assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
      assert checkQueueNonEmpty ();
      final int tr = m_ctmc.getNumTransitionsDone ();
      while (tridx < m_changes.length && m_changes[tridx].getTransition () + minTr <= tr) {
         m_ctmc.setArrivalRate (m_changes[tridx].getK (), m_changes[tridx].getRate ());
         ++tridx;
      }
      if (numNonEmptyQueues == queueNonEmpty.length ||
            (trajectories.length == 1 && !trajectories[0].canSplit (m_ctmc))) {
         // No waiting queue is empty, so no splitting possible
         final TransitionType type = m_ctmc.nextStateInt (bits);
         //final int np = ctmc.getNumPrecedingFalseTransitions ();
         //final int nf = ctmc.getNumFollowingFalseTransitions ();
         //ctmcHash += (np + nf + 1) * hashCoeff[hashCoeff.length - 1];
         int lastK, lastI;
         switch (type) {
         case ENDSERVICENODEQUEUE:
            lastI = m_ctmc.getLastSelectedAgentGroup ();
            ctmcHash -= hashCoeff[lastI];
            break;
         case ENDSERVICEANDDEQUEUE:
            lastK = m_ctmc.getLastSelectedQueuedContactType ();
            if (m_ctmc.getNumContactsInQueue (lastK) == 0) {
               --numNonEmptyQueues;
               queueNonEmpty[lastK] = false;
            }
            ctmcHash -= hashCoeff[hashCoeff.length - 2];
            break;
         case ABANDONMENT:
            lastK = m_ctmc.getLastSelectedContactType ();
            if (m_ctmc.getNumContactsInQueue (m_ctmc.getLastSelectedContactType ()) == 0) {
               --numNonEmptyQueues;
               queueNonEmpty[lastK] = false;
            }
            ctmcHash -= hashCoeff[hashCoeff.length - 2];
            break;
         case ARRIVALSERVED:
            lastI = m_ctmc.getLastSelectedAgentGroup ();
            ctmcHash += hashCoeff[lastI];
            break;
         case ARRIVALQUEUED:
            lastK = m_ctmc.getLastSelectedContactType ();
            if (!queueNonEmpty[lastK]) {
               queueNonEmpty[lastK] = true;
               ++numNonEmptyQueues;
            }
            ctmcHash += hashCoeff[hashCoeff.length - 2];
            break;
         }
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         updateCounters (type);
         return null;
      }

      final TransitionType type = m_ctmc.getNextTransitionInt (bits);
      final int lastK = m_ctmc.getLastSelectedContactType ();
      final int lastI = m_ctmc.getLastSelectedAgentGroup ();
      final int lastKp = m_ctmc.getLastSelectedQueuedContactType ();
      final int lastPos = m_ctmc.getLastSelectedContact ();
      final int np = m_ctmc.getNumPrecedingFalseTransitions ();
      final int nf = m_ctmc.getNumFollowingFalseTransitions ();
      //ctmcHash += (np + nf + 1) * hashCoeff[hashCoeff.length - 1];
      switch (type) {
      case ENDSERVICENODEQUEUE:
         m_ctmc.generateEndService (lastK, lastI, np, nf);
         ctmcHash -= hashCoeff[lastI];
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         updateCounters (type);
         return null;
      case ENDSERVICEANDDEQUEUE:
         m_ctmc.generateEndService (lastK, lastI, lastKp, np, nf);
         ctmcHash -= hashCoeff[hashCoeff.length - 2];
         if (m_ctmc.getNumContactsInQueue (lastKp) == 0) {
            // if (queueNonEmpty[lastKp])
            --numNonEmptyQueues;
            queueNonEmpty[lastKp] = false;
         }
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         updateCounters (type);
         return null;
      case ABANDONMENT:
         m_ctmc.generateAbandonment (lastK, lastPos, np, nf);
         ctmcHash -= hashCoeff[hashCoeff.length - 2];
         if (m_ctmc.getNumContactsInQueue (lastK) == 0) {
            // if (queueNonEmpty[lastK])
            --numNonEmptyQueues;
            queueNonEmpty[lastK] = false;
         }
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         updateCounters (type);
         return null;
      case FALSETRANSITION:
         m_ctmc.generateFalseTransition (np, nf);
         updateCounters (type);
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         return null;
      }
      // Arrival of type lastK
      // ctmc.getNumTransitionsDone () + np + nf + 1 >= ctmc
      //.getTargetNumTransitions ()
      //|| 
      if (!canSplit (lastK)) {
         // No splitting
         switch (type) {
         case ARRIVALBALKED:
         case ARRIVALBLOCKED:
            m_ctmc.generateArrival (lastK, np, nf);
            break;
         case ARRIVALQUEUED:
            m_ctmc.generateArrivalQueued (lastK, np, nf);
            ctmcHash += hashCoeff[hashCoeff.length - 2];
            if (!queueNonEmpty[lastK]) {
               queueNonEmpty[lastK] = true;
               ++numNonEmptyQueues;
            }
            break;
         case ARRIVALSERVED:
            m_ctmc.generateArrivalServed (lastK, lastI, np, nf);
            ctmcHash += hashCoeff[lastI];
            break;
         }
         assert ctmcHash + m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == m_ctmc.hashCodeState ();
         updateCounters (type);
         return null;
      }

      // Try to split each trajectory managed by the replication
      Replication[] children = new Replication[m_ctmc.getNumAgentGroups () + 1];
      List<Trajectory>[] trajChildren = newArray (children.length);
      int[] minNumSplit1 = new int[children.length];
      boolean first = true;
      int thisIdx = -1;
      for (Trajectory t : trajectories) {
         if (t.canSplit (m_ctmc, lastK)) {
            Trajectory[] childrenT = t.split (m_ctmc, lastK);
            for (int i = childrenT.length - 1; i >= 0; i--) {
               if (childrenT[i] == null)
                  continue;
               final int numSplit = childrenT[i].getNumSplit ();
               if (children[i] == null) {
                  if (first) {
                     children[i] = this;
                     thisIdx = i;
                     first = false;
                  }
                  else
                     children[i] = clone();
                  minNumSplit1[i] = numSplit;
                  trajChildren[i] = new ArrayList<Trajectory> ();
               }
               else if (numSplit < minNumSplit1[i])
                  minNumSplit1[i] = numSplit;
               trajChildren[i].add (childrenT[i]);
            }
         }
         else {
            int i = t.getAgentGroup (m_ctmc, lastK);
            final int numSplit = t.getNumSplit ();
            if (children[i] == null) {
               if (first) {
                  children[i] = this;
                  thisIdx = i;
                  first = false;
               }
               else
                  children[i] = clone();
               minNumSplit1[i] = numSplit;
               trajChildren[i] = new ArrayList<Trajectory> ();
            }
            else if (numSplit < minNumSplit1[i])
               minNumSplit1[i] = numSplit;
            trajChildren[i].add (t);
         }
      }
      
      // We would like children[i] == this for
      // where i children[i] corresponds to the basic
      // configuration.
      int minSplitIdx = -1;
      for (int i = 0; i < children.length; i++) {
         if (children[i] == null)
            continue;
         if (minSplitIdx == -1 || minNumSplit1[i] < minNumSplit1[minSplitIdx])
            minSplitIdx = i;
      }
      if (minSplitIdx != thisIdx) {
         final Replication tmp = children[minSplitIdx];
         children[minSplitIdx] = children[thisIdx];
         children[thisIdx] = tmp;
      }
      
      // Setup each replication
      int numChildren = 0;
      for (int i = 0; i < children.length; i++) {
         if (children[i] == null)
            continue;
         children[i].minNumSplit = minNumSplit1[i];
         ++numChildren;
         children[i].trajectories = trajChildren[i]
               .toArray (new Trajectory[trajChildren[i].size ()]);
         children[i].initStaffingBounds ();
         for (Trajectory t : children[i].trajectories)
            t.getStatNumTrajectories ().update (children[i].m_ctmc.getNumTransitionsDone (), children[i].trajectories.length);
         if (i < children.length - 1) {
            children[i].m_ctmc.generateArrivalServed (lastK, i, np, nf);
            children[i].ctmcHash += hashCoeff[i];
            children[i].updateCounters (TransitionType.ARRIVALSERVED);
         }
         else {
            switch (type) {
            case ARRIVALBALKED:
            case ARRIVALBLOCKED:
               children[i].m_ctmc.generateArrival (lastK, np, nf);
               break;
            case ARRIVALQUEUED:
               children[i].m_ctmc.generateArrivalQueued (lastK, np, nf);
               children[i].ctmcHash += hashCoeff[hashCoeff.length - 2];
               if (!children[i].queueNonEmpty[lastK]) {
                  children[i].queueNonEmpty[lastK] = true;
                  ++children[i].numNonEmptyQueues;
               }
               break;
            default:
               throw new AssertionError (type);
            }
            assert children[i].ctmcHash +
            children[i].m_ctmc.getNumTransitionsDone () * hashCoeff[hashCoeff.length - 1] == children[i].m_ctmc.hashCodeState ();
            children[i].updateCounters (type);
         }
      }
      assert numChildren > 0;
      if (numChildren < 2)
         return null;
      return children;
   }
   
   public int getMinNumSplit() {
      return minNumSplit;
   }
   
   public void finish() {
      trajectories = new Trajectory[0];
      m_ctmc = null;
   }

   @SuppressWarnings ("unchecked")
   private List<Trajectory>[] newArray (int length) {
      return new List[length];
   }
   
   public Replication[] splitNewPeriod (CallCenterCTMC ctmc) {
      Trajectory[][] children = new Trajectory[trajectories.length][];
      int idx = 0;
      for (Trajectory traj : trajectories)
         children[idx++] = traj.splitNewPeriod (ctmc);
      for (int i = 0; i < trajectories.length; i++)
         trajectories[i] = children[i][0];
      initStaffingBounds ();
      
      List<Replication> reps = new ArrayList<Replication>();
      reps.add (this);
      for (int i = 0; i < children.length; i++)
         for (int j = 1; j < children[i].length; j++) {
            final Trajectory traj = children[i][j];
            Replication child = clone();
            child.trajectories = new Trajectory[] { traj };
            child.initStaffingBounds ();
            reps.add (child);
         }
      return reps.toArray (new Replication[reps.size ()]);
   }
   

   private void updateCounters (TransitionType type) {
      for (Trajectory t : trajectories)
         t.update (m_ctmc, type);
   }

   private boolean canSplit (int k) {
      if (m_ctmc.getNumContactsInQueue (k) > 0)
         return false;
      for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
         final int i = typeToGroupMap[k][idx];
         if (m_lowerStaffing[i] >= m_upperStaffing[i])
            continue;
         final int numNeededAgents = m_ctmc.getNumContactsInServiceI (i) + 1;
         if (numNeededAgents <= m_lowerStaffing[i])
            return false;
         if (numNeededAgents <= m_upperStaffing[i])
            return true;
      }
      return false;
   }

   private void initStaffingBounds () {
      if (trajectories.length == 1) {
         Trajectory t = trajectories[0];
         System.arraycopy (t.getLowerStaffing (), 0, m_lowerStaffing, 0,
               m_lowerStaffing.length);
         System.arraycopy (t.getUpperStaffing (), 0, m_upperStaffing, 0,
               m_upperStaffing.length);
      }
      else {
         boolean first = true;
         for (Trajectory t : trajectories) {
            if (first) {
               System.arraycopy (t.getLowerStaffing (), 0, m_lowerStaffing, 0,
                     m_lowerStaffing.length);
               System.arraycopy (t.getUpperStaffing (), 0, m_upperStaffing, 0,
                     m_upperStaffing.length);
               first = false;
            }
            else {
               int[] lowerStaffingT = t.getLowerStaffing ();
               int[] upperStaffingT = t.getUpperStaffing ();
               for (int i = 0; i < m_lowerStaffing.length; i++) {
                  if (lowerStaffingT[i] < m_lowerStaffing[i])
                     m_lowerStaffing[i] = lowerStaffingT[i];
                  if (upperStaffingT[i] > m_upperStaffing[i])
                     m_upperStaffing[i] = upperStaffingT[i];
               }
            }
         }
      }
      m_ctmc.setNumAgents (m_lowerStaffing);
   }

   public void merge (Replication other) {
      assert this != other;
      assert ctmcHash == other.ctmcHash;
      assert m_ctmc.equalsState (other.m_ctmc);
      // Makes sure that the most recently replication
      // is killed, not the older one.
      // This has an impact only when collecting
      // some statistics about the performance of
      // the split and merge technique.
      if (minNumSplit <= other.minNumSplit) {
         for (Trajectory t : other.trajectories)
            t.newMerge ();
         trajectories = ArrayUtil.merge (trajectories, other.trajectories);
         for (Trajectory t : trajectories)
            t.getStatNumTrajectories ().update (m_ctmc.getNumTransitionsDone (), trajectories.length);
         other.trajectories = new Trajectory[0];
         for (int i = 0; i < m_lowerStaffing.length; i++) {
            if (other.m_lowerStaffing[i] < m_lowerStaffing[i])
               m_lowerStaffing[i] = other.m_lowerStaffing[i];
            if (other.m_upperStaffing[i] > m_upperStaffing[i])
               m_upperStaffing[i] = other.m_upperStaffing[i];
         }
         m_ctmc.setNumAgents (m_lowerStaffing);
      }
      else {
         for (Trajectory t : trajectories)
            t.newMerge ();
         other.trajectories = ArrayUtil
               .merge (other.trajectories, trajectories);
         for (Trajectory t : other.trajectories)
            t.getStatNumTrajectories ().update (other.m_ctmc.getNumTransitionsDone (), other.trajectories.length);
         trajectories = new Trajectory[0];
         for (int i = 0; i < m_lowerStaffing.length; i++) {
            if (m_lowerStaffing[i] < other.m_lowerStaffing[i])
               other.m_lowerStaffing[i] = m_lowerStaffing[i];
            if (m_upperStaffing[i] > other.m_upperStaffing[i])
               other.m_upperStaffing[i] = m_upperStaffing[i];
         }
         other.m_ctmc.setNumAgents (other.m_lowerStaffing);
      }
   }

   public CallCenterCTMC getCTMC () {
      return m_ctmc;
   }

   public int getCTMCHashCode () {
      return ctmcHash;
   }

   public Trajectory[] getTrajectories () {
      return trajectories;
   }
   
   public Replication clone() {
      Replication cpy;
      try {
         cpy = (Replication) super.clone ();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalError (e.toString ());
      }
      cpy.m_ctmc = m_ctmc.clone ();
      cpy.queueNonEmpty = queueNonEmpty.clone ();
      cpy.m_lowerStaffing = m_lowerStaffing.clone ();
      cpy.m_upperStaffing = m_upperStaffing.clone ();
      //cpy.num = counter++; 
      return cpy;
   }
   
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append ("{CTMC=").append (m_ctmc);
      sb.append (", trajectories={\n");
      for (Trajectory t : trajectories)
         sb.append (t.toString ()).append ('\n');
      sb.append ('}');
      return sb.toString ();
   }

   public int[] getLowerStaffing () {
      return m_lowerStaffing;
   }
   
   public int[] getUpperStaffing () {
      return m_upperStaffing;
   }
}
