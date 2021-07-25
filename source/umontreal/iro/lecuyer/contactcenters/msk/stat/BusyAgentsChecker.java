/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.SumMatrix;
import umontreal.ssj.stat.mperiods.SumMatrixSW;
import cern.jet.math.Functions;

/**
 * Computes the maximal number of busy agents for every agent group and
 * statistical period, during the simulation. An object of this class registers
 * as a listener for every agent group. Each time a contact enters service, 
 * the object then checks that the number of busy agents is not greater
 * than the current maximum, and updates the maximum if necessary. When the
 * model is simulated over multiple periods, such maxima are computed for each
 * period. A busy-agents checker is also a period-change listener, because at
 * the beginning of periods, it needs to set the per-period initial maxima to
 * the current number of busy agents.
 */
public final class BusyAgentsChecker implements AgentGroupListener,
      MeasureMatrix {
   private CallCenter cc;
   private StatPeriod statP;
   // A I'xP_s matrix where P_s is the number of periods in matrices
   // of counters.
   private SumMatrix maxBusy;
   // Vector containing one value per user-defined segment of agent groups
   private int[] tmp;

   /**
    * Constructs a new busy-agents checker using
    * call center \texttt{cc}, and object
    * \texttt{statP} to obtain statistical periods.
    */
   public BusyAgentsChecker (CallCenter cc, StatPeriod statP) {
      this.cc = cc;
      this.statP = statP;
      final int ng = cc.getNumAgentGroupsWithSegments ();
      final int np = statP.getNumPeriodsForCounters ();
      if (statP.needsSlidingWindows ())
         maxBusy = new SumMatrixSW (ng, np);
      else
         maxBusy = new SumMatrix (ng, np);
      tmp = new int[cc.getNumAgentGroupSegments ()];
   }

   /**
    * Initializes the counters to 0.
    */
   public void init () {
      maxBusy.init ();
      initForCurrentPeriod ();
   }

   public void initForCurrentPeriod () {
      final int cp = statP.getStatPeriod ();
      if (cp >= 0) {
         final int ng = cc.getNumAgentGroups ();
         for (int i = 0; i < ng; i++)
            maxBusy.add (i, cp, cc.getAgentGroup (i).getNumBusyAgents (), Functions.max);
         if (ng > 1)
            adjustMax (cp);
      }
   }

   /**
    * Registers this busy-agents checker with the associated
    * call center model.
    * The method adds this object to the list of observers
    * for all agent groups of the model, and
    * registers itself as a period-change listener.
    */
   public void register () {
      for (final AgentGroup group : cc.getAgentGroups ())
         group.addAgentGroupListener (this);
   }

   /**
    * Unregisters this busy-agents checker with the associated
    * model.
    * This method performs the reverse task of
    * {@link #register()}.
    */
   public void unregister () {
      for (final AgentGroup group : cc.getAgentGroups ())
         group.removeAgentGroupListener (this);
   }

   public void agentGroupChange (AgentGroup group) {
      checkBusy (group);
   }

   public void beginService (EndServiceEvent ev) {
      // We cannot use total as in SizeChecker, before
      // it sometimes happens that the begin-service
      // notification for a new call is received before
      // the end-service notification for an old call.
      // More specifically,
      // 1. EndServiceEvent.actions terminates the first part of a service
      // 2. This calls AgentGroup.completeContact, which calls completeService
      // 3. The router receives the end-service notification before the
      // busy agent checker, and
      // starts a new service if possible.
      // 4. The router and
      // the busy agent checker then receive a begin-service notification.
      // 5. The busy agent checker receives the original end-service
      // notification.
      checkBusy (ev.getAgentGroup ());
   }

   public void endContact (EndServiceEvent ev) {

   }

   public void endService (EndServiceEvent ev) {

   }

   public void init (AgentGroup group) {
      checkBusy (group);
   }

   private void checkBusy (AgentGroup group) {
      final int cp = statP.getStatPeriod ();
      if (cp < 0)
         return;
      final int nb = group.getNumBusyAgents ();
      final int gid = group.getId ();
      maxBusy.add (gid, cp, nb, Functions.max);
      if (cc.getNumAgentGroups () > 1)
         adjustMax (cp);
   }

   private void adjustMax (int cp) {
      int total = 0;
      Arrays.fill (tmp, 0);
      int ng = cc.getNumAgentGroups ();
      for (int i = 0; i < ng; i++) {
         final AgentGroup grp = cc.getAgentGroup (i);
         final int n = grp.getNumBusyAgents ();
         total += n;
         for (int j = 0; j < tmp.length; j++)
            if (cc.getAgentGroupSegment (j).containsValue (grp.getId ()))
               tmp[j] += n;
      }
      for (int j = 0; j < tmp.length; j++) {
         final int idx = cc.getNumAgentGroups () + j;
         maxBusy.add (idx, cp, tmp[j], Functions.max);
      }
      maxBusy.add (maxBusy.getNumMeasures () - 1, cp, total, Functions.max);
   }

   public double getMeasure (int i, int p) {
      return maxBusy.getMeasure (i, p);
   }

   public int getNumMeasures () {
      return maxBusy.getNumMeasures ();
   }

   public int getNumPeriods () {
      return maxBusy.getNumPeriods ();
   }

   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ();
   }

   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ();
   }
}
