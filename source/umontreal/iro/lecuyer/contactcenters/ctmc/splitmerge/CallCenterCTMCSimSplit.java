package umontreal.iro.lecuyer.contactcenters.ctmc.splitmerge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import javax.xml.bind.JAXBException;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;
import cern.colt.matrix.objectalgo.Formatter;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.CTMCRepSimParams;
import umontreal.iro.lecuyer.contactcenters.ctmc.AbstractCallCenterCTMCSim;
import umontreal.iro.lecuyer.contactcenters.ctmc.CTMCCreationException;
import umontreal.iro.lecuyer.contactcenters.ctmc.CallCenterStat;
import umontreal.iro.lecuyer.contactcenters.ctmc.RateChangeInfo;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stat.AccumulateWithTimes;
import umontreal.ssj.stat.Tally;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.iro.lecuyer.util.IntArray;

public class CallCenterCTMCSimSplit extends AbstractCallCenterCTMCSim {
   private static Comparator<Replication> repcmp = new ReplicationComparator();
   private static final int LIST_TEST_THRESH = 5;
   private double numExpectedTransitions;
   private List<Replication> replications = new LinkedList<Replication> ();
   private List<Trajectory> finishedTrajectories = new ArrayList<Trajectory> ();
   private int[] lowerStaffing;
   private int[] upperStaffing;
   private int[] initStaffing;
   private Map<IntArray, CallCenterStat> ccStatMap = new HashMap<IntArray, CallCenterStat> ();
   private Map<IntArray, Tally> statSplitMap = new HashMap<IntArray, Tally> ();
   private Map<IntArray, Tally> statMergeMap = new HashMap<IntArray, Tally> ();
   private Map<IntArray, Tally> statTrSplitMap = new HashMap<IntArray, Tally> ();
   private Map<IntArray, Tally> statNumTrajMap = new HashMap<IntArray, Tally> ();

   private AccumulateWithTimes accNumReps = new AccumulateWithTimes ();
   private int numMerge;
   private Tally statNumReps = new Tally (
         "Time-average number of parallel replications");
   private Tally statNumMerge = new Tally ("Number of merges");
   private double[][] timeDist;

   private boolean noMerge = false;
   private boolean subgradientOnly;
   
   private Replication baseConfigRep;
   private List<Replication>[] groupReps;
   
   public CallCenterCTMCSimSplit (CallCenter cc, CTMCRepSimParams simParams,
         int mp) throws CTMCCreationException {
      super (cc, simParams, mp);
      init ();
   }

   public CallCenterCTMCSimSplit (CallCenter cc, CTMCRepSimParams simParams,
         int mp, int[] lowerStaffing, int[] upperStaffing)
         throws CTMCCreationException {
      super (cc, simParams, mp);
      if (lowerStaffing.length != upperStaffing.length)
         throw new IllegalArgumentException();
      if (lowerStaffing.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException();
      this.lowerStaffing = lowerStaffing.clone ();
      this.upperStaffing = upperStaffing.clone ();
      init ();
   }

   public CallCenterCTMCSimSplit (CallCenterParams ccParams,
         CTMCRepSimParams simParams, int mp)
         throws CallCenterCreationException, CTMCCreationException {
      super (ccParams, simParams, mp);
      init ();
   }

   public CallCenterCTMCSimSplit (CallCenterParams ccParams,
         CTMCRepSimParams simParams, RandomStreams streams, int mp)
         throws CallCenterCreationException, CTMCCreationException {
      super (ccParams, simParams, streams, mp);
      init ();
   }

   public void setNoMerge (boolean noMerge) {
      this.noMerge = noMerge;
   }

   public void setSubgradientOnly (boolean subgradientOnly) {
      this.subgradientOnly = subgradientOnly;
   }

   public Tally getStatNumReps () {
      return statNumReps;
   }

   public Tally getStatNumMerge () {
      return statNumMerge;
   }
   
   public List<Replication> getReplications() {
      return replications;
   }

   private void init () {
      if (lowerStaffing == null || upperStaffing == null) {
         lowerStaffing = new int[getCTMC ().getNumAgentGroups ()];
         upperStaffing = getCTMC ().getMaxNumAgentsArray ();
         int[] minNumAgents = simParams.getMinNumAgents ();
         if (minNumAgents != null && minNumAgents.length > 0) {
            if (minNumAgents.length == 1)
               Arrays.fill (lowerStaffing, minNumAgents[0]);
            else {
               if (minNumAgents.length != lowerStaffing.length)
                  throw new IllegalArgumentException (
                        "The length of minNumAgents must be equal to the number of agent groups");
               System.arraycopy (minNumAgents, 0, lowerStaffing, 0,
                     lowerStaffing.length);
            }
            for (int i = 0; i < lowerStaffing.length; i++)
               if (lowerStaffing[i] > upperStaffing[i])
                  lowerStaffing[i] = upperStaffing[i];
         }
      }
      initStaffing = ctmc.getNumAgentsArray ();
      for (int i = 0; i < initStaffing.length; i++) {
         if (initStaffing[i] > upperStaffing[i])
            initStaffing[i] = upperStaffing[i];
         if (initStaffing[i] < lowerStaffing[i])
            initStaffing[i] = lowerStaffing[i];
      }
      groupReps = newArrayRepSet (initStaffing.length);
      for (int i = 0; i < groupReps.length; i++)
         groupReps[i] = new LinkedList<Replication> ();
   }
   
   @SuppressWarnings ("unchecked")
   private List<Replication>[] newArrayRepSet (int length) {
      return new List[length];
   }

   private void addObsStaffing (int[] staffing,
         Trajectory t) {
      IntArray key = new IntArray (staffing);
      CallCenterStat stat = ccStatMap.get (key);
      if (stat == null) {
         boolean curStaffing = true;
         for (int i = 0; i < staffing.length && curStaffing; i++) {
            if (staffing[i] != initStaffing[i])
               curStaffing = false;
         }
         if (curStaffing)
            stat = ccStat;
         else
            stat = new CallCenterStat (ctmc, getNumMatricesOfAWT (), simParams
                  .isKeepObs ());
         stat.init (ctmc);
         ccStatMap.put (key, stat);
      }
      double[] totalAgents = t.getCounters ().getTotalAgents ();
      for (int i2 = 0; i2 < totalAgents.length; i2++)
         totalAgents[i2] = staffing[i2];
      stat.addObs (t.getCounters (), getTimeHorizon ());

      Tally taSplit = statSplitMap.get (key);
      if (taSplit == null) {
         taSplit = new Tally ();
         statSplitMap.put (key, taSplit);
      }
      taSplit.add (t.getNumSplit ());
      Tally taMerge = statMergeMap.get (key);
      if (taMerge == null) {
         taMerge = new Tally ();
         statMergeMap.put (key, taMerge);
      }
      taMerge.add (t.getNumMerge ());
      Tally taTrSplit = statTrSplitMap.get (key);
      if (taTrSplit == null) {
         taTrSplit = new Tally ();
         statTrSplitMap.put (key, taTrSplit);
      }
      taTrSplit.add (t.getSplitTransition ());
      Tally taNumTraj = statNumTrajMap.get (key);
      if (taNumTraj == null) {
         taNumTraj = new Tally ();
         statNumTrajMap.put (key, taNumTraj);
      }
      taNumTraj.add (t.getStatNumTrajectories ().average ());
   }

   @Override
   protected void addObs () {
      // super.addObs ();
      for (Trajectory t : finishedTrajectories) {
         int[] lowerStaffing1 = t.getLowerStaffing ();
         int[] upperStaffing1 = t.getUpperStaffing ();
         int[] staffing = lowerStaffing1.clone ();
         if (subgradientOnly) {
            addObsStaffing (staffing, t);
            for (int i = 0; i < staffing.length; i++) {
               staffing[i] = lowerStaffing1[i] + 1;
               while (staffing[i] <= upperStaffing1[i]) {
                  addObsStaffing (staffing, t);
                  ++staffing[i];
               }
               staffing[i] = lowerStaffing1[i];
            }
         }
         else {
            boolean hasStaffing = true;
            while (hasStaffing) {
               addObsStaffing (staffing, t);
               hasStaffing = false;
               for (int idx = staffing.length - 1; idx >= 0 && !hasStaffing; --idx) {
                  if (staffing[idx] < upperStaffing1[idx]) {
                     ++staffing[idx];
                     for (int idx2 = idx + 1; idx2 < staffing.length; ++idx2)
                        staffing[idx2] = lowerStaffing1[idx2];
                     hasStaffing = true;
                  }
               }
            }
         }
      }
      IntArray initKey = new IntArray (initStaffing);
      if (!ccStatMap.containsKey (initKey)) {
         initKey = new IntArray (lowerStaffing);
         if (!ccStatMap.containsKey (initKey)) {
            Iterator<IntArray> it = ccStatMap.keySet ().iterator ();
            initKey = it.next ();
         }
         initStaffing = initKey.getArray ();
         setStat (ccStatMap.get (initKey));
      }
      statNumReps.add (accNumReps.average ());
      statNumMerge.add (numMerge);
   }

   @Override
   public double getNumExpectedTransitions () {
      return numExpectedTransitions;
   }

   public Set<IntArray> getStaffings () {
      return ccStatMap.keySet ();
   }

   public Map<IntArray, CallCenterStat> getStatMap () {
      return ccStatMap;
   }

   public CallCenterStat getStat (int[] staffing) {
      return ccStatMap.get (new IntArray (staffing));
   }

   @Override
   public void setStaffing (int[] staffing) {
      super.setStaffing (staffing);
      System.arraycopy (staffing, 0, initStaffing, 0, staffing.length);
      if (ccStatMap.isEmpty ())
         return;
      IntArray key = new IntArray (staffing);
      CallCenterStat ccStat1 = ccStatMap.get (key);
      if (ccStat1 == null)
         setOneSimDone (false);
      else {
         setOneSimDone (true);
         setStat (ccStat1);
         formatReport ();
      }
   }

   @Override
   public void formatReport () {
      super.formatReport ();
      IntArray key = new IntArray (initStaffing);
      Tally taSplit = statSplitMap.get (key);
      if (taSplit != null)
         getEvalInfo ().put ("Average number of splits", taSplit.average ());
      Tally taMerge = statMergeMap.get (key);
      if (taMerge != null)
         getEvalInfo ().put ("Average number of merges", taMerge.average ());
      Tally taTrSplit = statTrSplitMap.get (key);
      if (taTrSplit != null)
         getEvalInfo ()
               .put (
                     "Average starting transition of configuration-specific trajectory",
                     taTrSplit.average ());
      Tally taNumTraj = statNumTrajMap.get (key);
      if (taNumTraj != null)
         getEvalInfo ().put (
               "Time-average number of trajectories in replication",
               taNumTraj.average ());
      getEvalInfo ().put ("Time-average number of parallel replications",
            statNumReps.average ());
      getEvalInfo ().put ("Total average number of merges",
            statNumMerge.average ());
   }

   @Override
   protected void initReplication (RandomStream stream, double timeHorizon, int ntr) {
      RateChangeInfo[] changes = rateChange.generateRateChanges (stream, timeDist, ntr);
      ctmc.initEmpty ();
      ctmc.setNumAgents (lowerStaffing);
      ctmc.setTargetNumTransitions (ntr);
      replications.clear ();
      for (int i = 0; i < groupReps.length; i++)
         groupReps[i].clear ();
      finishedTrajectories.clear ();
      super.initReplication (stream, timeHorizon, ntr);
      Replication firstRep = new Replication (ctmc, counters, subgradientOnly,
            lowerStaffing, upperStaffing, changes);
      baseConfigRep = firstRep;
      replications.add (firstRep);
      accNumReps.init (0, 1);
      numMerge = 0;
   }

   public void simulateTransitions (RandomStream stream, int r, double timeHorizon,
         int ntr) {
      initReplication (stream, timeHorizon, ntr);
      notifyInit (r, mp, ctmc);

      List<Replication> newReplications = new ArrayList<Replication> ();
      int time = 0;
      while (!replications.isEmpty ()) {
         ++time;
         boolean changedNumReps = false;
         final int rv = stream.nextInt (0, Integer.MAX_VALUE);
         boolean merge = false;
         for (Iterator<Replication> it = replications.iterator (); it
               .hasNext ();) {
            Replication rep = it.next ();
            if (rep.getTrajectories ().length == 0) {
               it.remove ();
               continue;
            }
            Replication[] children = rep.simulateStep (rv);
            if (children == null) {
               if (rep.getCTMC ().getNumTransitionsDone () >= ntr) {
                  it.remove ();
                  changedNumReps = true;
                  finishReplication (rep);
               }
               else {
                  if (!noMerge) {
                     switch (rep.getCTMC ().getLastTransitionType ()) {
                     case ENDSERVICEANDDEQUEUE:
                     case ENDSERVICENODEQUEUE:
                     case ABANDONMENT:
                        merge = true;
                        break;
                     }
                  }
               }
            }
            else {
               it.remove ();
               if (subgradientOnly && !noMerge) {
                  if (rep == baseConfigRep) {
                     // Replication for base configuration
                     for (int i = children.length - 1; i >= 0; i--) {
                        if (children[i] == null)
                           continue;
                        if (children[i].getMinNumSplit () == 0)
                           baseConfigRep = children[i];
                        else if (children[i].getCTMC ().getNumTransitionsDone () < ntr)
                           //groupReps[i].add (children[i]);
                           addToGroupReps (i, children[i]);
                     }
                  }
                  else {
                     int group = -1;
                     int maxNumSplit = -1;
                     for (int i = 0; i < children.length; i++) {
                        if (children[i] == null)
                           continue;
                        int numSplit = children[i].getMinNumSplit ();
                        if (numSplit > maxNumSplit) {
                           group = i;
                           maxNumSplit = numSplit;
                        }
                     }
                     //if (!groupReps[group].remove (rep))
                     //   throw new AssertionError();
                     for (int i = 0; i < children.length; i++) {
                        if (children[i] == null || children[i] == rep)
                           // rep is null or already in groupReps[group]
                           continue;
                        if (children[i].getCTMC ().getNumTransitionsDone () < ntr)                        
                           //groupReps[group].add (children[i]);
                           addToGroupReps (group, children[i]);
                     }                     
                  }
               }
               for (Replication child : children)
                  if (child != null) {
                     if (child.getCTMC ().getNumTransitionsDone () >= ntr)
                        finishReplication (child);
                     else
                        newReplications.add (child);
                  }
               changedNumReps = true;
            }
         }
         int nm = 0;
         if (merge && (nm = testMerge ()) > 0) {
            // After testMerge returns, replications
            // contains nm dummy Replication objects
            // with no trajectories.
            // By removing these objects on the next
            // pass of the main while loop, we avoid
            // creating an extra iterator.
            if (replications.size () - nm <= 0)
               // Merging cleared all replications
               replications.clear ();
//            for (Iterator<Replication> it = replications.iterator (); it
//                  .hasNext ();) {
//               Replication rep = it.next ();
//               if (rep.getTrajectories ().length == 0)
//                  // Remove merged replication
//                  it.remove ();
//            }
            changedNumReps = true;
            numMerge += nm;
         }
         replications.addAll (newReplications);
         newReplications.clear ();
         if (changedNumReps)
            accNumReps.update (time, replications.isEmpty () ? 0 : replications.size () - nm);
         notifyTransition (r, mp, ctmc, ctmc.getLastTransitionType ());
      }
      addObs ();
   }
   
   private void addToGroupReps (int i, Replication rep) {
      if (groupReps[i].isEmpty ())
         groupReps[i].add (rep);
      else if (repcmp.compare (rep, groupReps[i].get (0)) <= 0)
         groupReps[i].add (0, rep);
      else {
         ListIterator<Replication> it;
         for (it = groupReps[i].listIterator (); it.hasNext ();) {
            final Replication repInList = it.next ();
            if (repcmp.compare (rep, repInList) <= 0)
               break;
         }
         it.add (rep);
      }
   }

   private void finishReplication (Replication rep) {
      for (Trajectory t : rep.getTrajectories ()) {
         t.getCounters ().updateStatOnTime (rep.getCTMC ());
         t.getStatNumTrajectories ().update (
               rep.getCTMC ().getNumTransitionsDone ());
         // int[] lowerStaffing = t.getLowerStaffing ();
         // double[] totalAgents = t.getCounters ().getTotalAgents ();
         // for (int i2 = 0; i2 < totalAgents.length; i2++)
         // totalAgents[i2] = lowerStaffing[i2];
         finishedTrajectories.add (t);
      }
      rep.finish();
   }

   private int testMerge () {
      if (replications.size () <= 1)
         return 0;
      if (subgradientOnly) {
         if (baseConfigRep.getTrajectories ().length == 0)
            return 0;
         int numMerge1 = 0;
         for (int i = 0; i < groupReps.length; i++) {
            Replication oldRep = baseConfigRep;
            if (groupReps[i].isEmpty ())
               continue;
            if (groupReps[i].size () == 1) {
               Replication rep = groupReps[i].get (0);
               if (rep.getTrajectories ().length == 0)
                  groupReps[i].remove (0);
               else {
                  if (testMerge (oldRep, rep)) {
                     ++numMerge1;
                     groupReps[i].remove (0);
                  }
                  else
                     oldRep = rep;
               }
            }
            else
               for (Iterator<Replication> it = groupReps[i].iterator (); it
               .hasNext ();) {
                  Replication rep = it.next ();
                  if (rep.getTrajectories ().length == 0)
                     it.remove ();
                  else {
                     if (testMerge (oldRep, rep)) {
                        ++numMerge1;
                        it.remove ();
                     }
                     else
                        oldRep = rep;
                  }
               }
         }
         return numMerge1;
      }
      return testMerge (replications);
   }

   protected static int testMerge (List<Replication> replications) {
      int numMerge = 0;
      if (!(replications instanceof RandomAccess) && replications.size () > LIST_TEST_THRESH) {
         mainLoop: for (ListIterator<Replication> it1 = replications
               .listIterator (); it1.hasNext ();) {
            final Replication r1 = it1.next ();
            if (!it1.hasNext ())
               continue;
            if (r1.getTrajectories ().length == 0)
               continue;
            assert it1.nextIndex () < replications.size ();
            for (ListIterator<Replication> it2 = replications.listIterator (it1
                  .nextIndex ()); it2.hasNext ();) {
               final Replication r2 = it2.next ();
               assert r1 != r2;
               if (r2.getTrajectories ().length == 0)
                  continue;
               if (testMerge (r1, r2)) {
                  ++numMerge;
                  if (r1.getTrajectories ().length == 0)
                     continue mainLoop;
               }
            }
         }
      }
      else {
         mainLoop: for (int i = 0; i < replications.size () - 1; i++) {
            final Replication r1 = replications.get (i);
            if (r1.getTrajectories ().length == 0)
               continue;
            for (int j = i + 1; j < replications.size (); j++) {
               final Replication r2 = replications.get (j);
               if (r2.getTrajectories ().length == 0)
                  continue;
               if (testMerge (r1, r2)) {
                  ++numMerge;
                  if (r1.getTrajectories ().length == 0)
                     continue mainLoop;
               }
            }
         }
      }
      return numMerge;
   }
   
   private static boolean testMerge (Replication r1, Replication r2) {
      if (r1.getCTMCHashCode () != r2.getCTMCHashCode ())
         return false;
      if (!r1.getCTMC ().equalsState (r2.getCTMC ()))
         return false;
      r1.merge (r2);
      return true;
   }

   @Override
   public void simulate (RandomStream stream, double timeHorizon, int n) {
      double st = cc.getStartingTime ();
      double et = st + timeHorizon;
      timeDist = rateChange.getTimeDist (st, et);
      ccStatMap.clear ();
      statSplitMap.clear ();
      statMergeMap.clear ();
      statTrSplitMap.clear ();
      statNumTrajMap.clear ();
      statNumReps.init ();
      statNumMerge.init ();
      numExpectedTransitions = ctmc.getJumpRate () * timeHorizon;
      PoissonDist pdist = new PoissonDist (numExpectedTransitions);
      for (int i = 0; i < n; i++) {
         int ntr = pdist.inverseFInt (stream.nextDouble ());
         simulateTransitions (stream, i, timeHorizon, ntr);
         stream.resetNextSubstream ();
      }
   }

   public void reportSL () {
      IntArray[] staffings = ccStatMap.keySet ().toArray (
            new IntArray[ccStatMap.size ()]);
      Arrays.sort (staffings);
      String[] rows = new String[staffings.length];
      String[] columns = new String[] { "Avg. splits", "Avg. merges",
            "Avg. starting trans.", "Time-avg. nbr of trajectories",
            "Avg. nbr false trans.", "Avg. max. nbr of busy agents",
            "Calls waiting <= AWT", "Abandonment rate" };
      ObjectMatrix2D objm = new DenseObjectMatrix2D (staffings.length,
            columns.length);
      int idx = 0;
      for (IntArray staffing : staffings) {
         rows[idx] = staffing.toString ();
         setStaffing (staffing.getArray ());
         Tally taSplit = statSplitMap.get (staffing);
         if (taSplit != null)
            objm.set (idx, 0, String.format ("%.3f", taSplit.average ()));
         Tally taMerge = statMergeMap.get (staffing);
         if (taMerge != null)
            objm.set (idx, 1, String.format ("%.0f", taMerge.average ()));
         Tally taTrSplit = statTrSplitMap.get (staffing);
         if (taTrSplit != null)
            objm.set (idx, 2, String.format ("%.0f", taTrSplit.average ()));
         Tally taNumTraj = statNumTrajMap.get (staffing);
         if (taNumTraj != null)
            objm.set (idx, 3, String.format ("%.3f", taNumTraj.average ()));
         objm.set (idx, 4, String.format ("%.3f", getStat ()
               .getStatNumFalseTransitions ().average ()));
         DoubleMatrix2D m;
         m = getPerformanceMeasure (PerformanceMeasureType.MAXBUSYAGENTS);
         objm.set (idx, 5, String.format ("%.0f", m.get (m.rows () - 1, m
               .columns () - 1)));
         m = getPerformanceMeasure (PerformanceMeasureType.RATEOFINTARGETSL);
         objm.set (idx, 6, String.format ("%.3f", m.get (m.rows () - 1, m
               .columns () - 1)));
         m = getPerformanceMeasure (PerformanceMeasureType.RATEOFABANDONMENT);
         objm.set (idx, 7, String.format ("%.3f", m.get (m.rows () - 1, m
               .columns () - 1)));
         ++idx;
      }
      Formatter fmt = new Formatter ();
      System.out.println (fmt.toTitleString (objm, rows, columns, "Staffing",
            "", "Results"));
      System.out.println ();

      if (statNumReps != null)
         System.out.printf (
               "Time-average number of parallel replications: %.3f%n",
               statNumReps.average ());
      if (statNumMerge != null)
         System.out.printf ("Average number of merges: %.3f%n", statNumMerge
               .average ());

      System.out.println ("Abandonment rate");
      for (IntArray staffing : staffings) {
         setStaffing (staffing.getArray ());
         System.out.print (staffing.toString ());
         System.out.print (": ");
         // MatrixOfStatProbes<?> sp = getMatrixOfStatProbes
         // (PerformanceMeasureType.RATEOFABANDONMENT);
         // DoubleMatrix2D m = new DenseDoubleMatrix2D (sp.rows (), sp.columns
         // ());
         // sp.average (m);
         DoubleMatrix2D m = getPerformanceMeasure (PerformanceMeasureType.RATEOFABANDONMENT);
         System.out.print ("[");
         for (int k = 0; k < m.rows (); k++) {
            if (k > 0)
               System.out.print (", ");
            System.out.printf ("%f", m.get (k, m.columns () - 1));
         }
         System.out.println ("]");
      }
      System.out.println ();
      System.out.println ("Calls waiting less than AWT");
      for (IntArray staffing : staffings) {
         setStaffing (staffing.getArray ());
         System.out.print (staffing.toString ());
         System.out.print (": ");
         DoubleMatrix2D m = getPerformanceMeasure (PerformanceMeasureType.RATEOFINTARGETSL);
         System.out.print ("[");
         for (int k = 0; k < m.rows (); k++) {
            if (k > 0)
               System.out.print (", ");
            System.out.printf ("%f", m.get (k, m.columns () - 1));
         }
         System.out.println ("]");
      }
      System.out.println ();
      System.out.println ("Maximal number of busy agents");
      for (IntArray staffing : staffings) {
         setStaffing (staffing.getArray ());
         System.out.print (staffing.toString ());
         System.out.print (": ");
         DoubleMatrix2D m = getPerformanceMeasure (PerformanceMeasureType.MAXBUSYAGENTS);
         System.out.print ("[");
         for (int i = 0; i < m.rows (); i++) {
            if (i > 0)
               System.out.print (", ");
            System.out.printf ("%f", m.get (i, m.columns () - 1));
         }
         System.out.println ("]");
      }
      System.out.println ();
   }
   
   private static class ReplicationComparator implements Comparator<Replication> {

      public int compare (Replication o1, Replication o2) {
         final int a1 = o1.getMinNumSplit ();
         final int a2 = o2.getMinNumSplit (); 
         if (a1 < a2)
            return -1;
         if (a1 > a2)
            return 1;
         return 0;
      }
   }

   public static void main (String[] args) {
      if (args.length != 4 && args.length != 5) {
         System.err
               .println ("Usage: java CallCenterCTMCSimSplit2 ccParams simParams mp subgradientOnly [output file]");
         System.exit (1);
      }

      String ccPsFn = args[0];
      String simPsFn = args[1];
      int mp = Integer.parseInt (args[2]);
      boolean subgradientOnly = Boolean.parseBoolean (args[3]);
      File outputFile;
      if (args.length > 4)
         outputFile = new File (args[4]);
      else
         outputFile = null;

      CallCenterParamsConverter cnv = new CallCenterParamsConverter ();
      CallCenterParams ccParams = cnv.unmarshalOrExit (new File (ccPsFn));
      SimParamsConverter cnvSim = new SimParamsConverter ();
      CTMCRepSimParams simParams = (CTMCRepSimParams) cnvSim
            .unmarshalOrExit (new File (simPsFn));

      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());
      CallCenterCTMCSimSplit sim;
      try {
         sim = new CallCenterCTMCSimSplit (ccParams, simParams, mp);
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
      sim.setSubgradientOnly (subgradientOnly);
      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (), ccPsFn,
            simPsFn);
      sim.eval ();
      sim.reportSL ();

      try {
         PerformanceMeasureFormat.formatResults (sim, outputFile);
      }
      catch (IOException ioe) {
         System.err.println (ExceptionUtil.throwableToString (ioe));
         System.exit (1);
      }
      catch (JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
      }
   }
}
