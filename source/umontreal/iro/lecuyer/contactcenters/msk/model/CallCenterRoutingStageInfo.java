package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.conditions.Condition;
import umontreal.iro.lecuyer.contactcenters.msk.params.RoutingStageParams;
import umontreal.iro.lecuyer.contactcenters.router.OverflowAndPriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.RankFunction;
import umontreal.iro.lecuyer.contactcenters.router.RoutingStageInfo;

/**
 * Provides information on a routing stage, for
 * the {@link OverflowAndPriorityRouter} router.
 * The information includes a waiting time, and
 * a list of routing cases which are used to
 * compute the functions returning vectors of ranks.
 */
public class CallCenterRoutingStageInfo implements RoutingStageInfo {
   private double waitingTime;
   private RankFunction rfAgents;
   private RankFunction rfQueues;
   private RoutingCase[] cases;
   
   
   /**
    * Constructs call canter routing stage from the 
    * model \texttt{cc}, and parameters \texttt{par}.
    * @param cc the call center model.
    * @param par the parameters for the routing stage.
    */
   public CallCenterRoutingStageInfo (CallCenter cc, int k, RoutingStageParams par) {
      this.waitingTime = cc.getTime (par.getWaitingTime ()); 
      rfAgents = new RankForAgentSelection();
      rfQueues = new RankForContactSelection();
      cases = new RoutingCase[par.getCase ().size () + (par.isSetDefault () ? 1 : 0)];
      for (int i = 0; i < par.getCase ().size (); i++) {
         try {
            cases[i] = new RoutingCase (cc, k, par.getCase ().get (i));
         }
         catch (IllegalArgumentException iae) {
            IllegalArgumentException iae2 = new IllegalArgumentException
            ("Error constructing routing case " + i);
            iae2.initCause (iae);
            throw iae2;
         }
      }
      if (par.isSetDefault ()) {
         try {
            cases[cases.length - 1] = new RoutingCase (cc, k, par.getDefault ());
         }
         catch (IllegalArgumentException iae) {
            IllegalArgumentException iae2 = new IllegalArgumentException
            ("Error constructing default routing case");
            iae2.initCause (iae);
            throw iae2;
         }
      }
   }
   
   public RankFunction getRankFunctionForAgentSelection () {
      return rfAgents;
   }

   public RankFunction getRankFunctionForContactSelection () {
      return rfQueues;
   }

   public double getWaitingTime () {
      return waitingTime;
   }
   
   public RoutingCase[] getCases() {
      return cases;
   }
   
   private static class CaseInfo {
      private RoutingCase rCase;

      public CaseInfo (RoutingCase case1) {
         rCase = case1;
      }
      
      public RoutingCase getCase() {
         return rCase;
      }
   }

   private RoutingCase getCase (Contact contact) {
      CaseInfo info = (CaseInfo)contact.getAttributes ().get (this);
      if (info != null)
         return info.getCase ();
      for (int l = 0; l < cases.length; l++) {
         Condition cond = cases[l].getCondition ();
         if (cond == null || cond.applies (contact)) {
            info = new CaseInfo (cases[l]);
            contact.getAttributes ().put (this, info);
            return cases[l];
         }
      }
      info = new CaseInfo (null);
      contact.getAttributes ().put (this, info);
      return null;
   }
   
   private class RankForAgentSelection implements RankFunction {
      public boolean canReturnFiniteRank (int i) {
         for (int l = 0; l < cases.length; l++) {
            if (cases[l].getAgentGroupRanksFunction ().canReturnFiniteRank (i))
               return true;
         }
         return false;
      }

      public boolean updateRanks (Contact contact, double[] ranks) {
         RoutingCase rCase = getCase (contact);
         if (rCase == null)
            return false;
         return rCase.getAgentGroupRanksFunction ().updateRanks (contact, ranks);
      }
   }

   private class RankForContactSelection implements RankFunction {
      public boolean canReturnFiniteRank (int i) {
         for (int l = 0; l < cases.length; l++) {
            if (cases[l].getQueueRanksFunction ().canReturnFiniteRank (i))
               return true;
         }
         return false;
      }

      public boolean updateRanks (Contact contact, double[] ranks) {
         RoutingCase rCase = getCase (contact);
         if (rCase == null)
            return false;
         return rCase.getQueueRanksFunction ().updateRanks (contact, ranks);
      }
   }
}
