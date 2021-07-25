package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.IndexThreshParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.StatConditionParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.TwoIndicesParams;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatWithSlidingWindows;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Represents a condition on statistics observed
 * during a given number of periods preceding the
 * times at which the condition is checked.
 */
public class StatCondition implements Condition, Initializable, ToggleElement {
   private CallCenterStatWithSlidingWindows ccStat;
   private PerformanceMeasureType pm;
   private CallCenter cc;
   private double pd;
   private int np;
   private TwoIndicesInfo[] statWithStat;
   private IndexThreshInfo[] statWithThresh;

   /**
    * Constructs a new condition on statistics based
    * on a call center model \texttt{cc}, and parameters
    * \texttt{par}.
    */
   public StatCondition (CallCenter cc, StatConditionParams par) {
      this.cc = cc;
      pm = PerformanceMeasureType.valueOf (par.getMeasure ());
      pd = cc.getTime (par.getCheckedPeriodDuration ());
      np = par.getNumCheckedPeriods ();
      // We cannot initialize ccStat yet, because at the time the condition
      // is constructed, the CallCenter object might not be fully initialized.
      // We thus postpone the initialization in the init method.
      int c1 = 0, c2 = 0;
      for (Object o : par.getStatWithThreshOrStatWithStat ()) {
         if (o instanceof TwoIndicesParams)
            ++c1;
         else if (o instanceof IndexThreshParams)
            ++c2;
         else
            throw new AssertionError();
      }
      statWithStat = new TwoIndicesInfo[c1];
      statWithThresh = new IndexThreshInfo[c2];
      c1 = c2 = 0;
      for (Object o : par.getStatWithThreshOrStatWithStat ()) {
         if (o instanceof TwoIndicesParams) {
            TwoIndicesParams i = (TwoIndicesParams)o;
            statWithStat[c1++] = new TwoIndicesInfo (i.getFirst (), i.getSecond (), i.getRel ());
         }
         else if (o instanceof IndexThreshParams) {
            IndexThreshParams i = (IndexThreshParams)o;
            statWithThresh[c2++] = new IndexThreshInfo (i.getIndex (), i.getThreshold (), i.getRel ());
         }
      }
   }

   public boolean applies (Contact contact) {
      CallCenterStatProbes probes = ccStat.getStat ();
      DoubleMatrix2D avg = probes.getAverage (pm);
      for (TwoIndicesInfo i : statWithStat) {
         double v1 = avg.get (i.getFirstIndex (), 0);
         double v2 = avg.get (i.getSecondIndex (), 0);
         if (!ConditionUtil.applies (v1, v2, i.getRelationship ()))
            return false;
      }
      for (IndexThreshInfo i : statWithThresh) {
         double v1 = avg.get (i.getIndex (), 0);
         if (!ConditionUtil.applies (v1, i.getThreshold (), i.getRelationship ()))
            return false;
      }
      return true;
   }

   public void init () {
      if (ccStat == null) {
         ccStat = new CallCenterStatWithSlidingWindows
         (cc, pd, np, pm.getRowType ().isContactTypeAgentGroup (), pm);
         ccStat.registerListeners ();
      }
      else
         ccStat.init ();
   }

   public boolean isStarted () {
      return ccStat.isStarted ();
   }

   public void start () {
      ccStat.start ();
   }

   public void stop () {
      ccStat.stop ();
   }
}
