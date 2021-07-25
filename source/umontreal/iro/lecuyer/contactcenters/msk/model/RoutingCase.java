package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.conditions.Condition;
import umontreal.iro.lecuyer.contactcenters.msk.conditions.ConditionUtil;
import umontreal.iro.lecuyer.contactcenters.msk.params.ConditionParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.DefaultCaseParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.RoutingCaseParams;
import umontreal.iro.lecuyer.contactcenters.router.OverflowAndPriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.RankFunction;

/**
 * Represents a routing case part of a routing stage, for
 * the {@link OverflowAndPriorityRouter}.
 * A case is defined by a condition, represented by
 * an instance of {@link Condition}, and vectors of
 * ranks for agent selection, and queue priorities.
 * An instance with condition set to \texttt{null} is also
 * possible to represent the default case.
 */
public class RoutingCase {
   private Condition cond;
   private double[] aRanks;
   private boolean aRanksRel;
   private double[] qRanks;
   private boolean qRanksRel;
   
   private RankFunction aRanksFunc;
   private RankFunction qRanksFunc;
   
   /**
    * Constructs a new routing case using the call center
    * model \texttt{cc}, and parameters \texttt{par}.
    * The vectors of ranks are extracted directly from
    * \texttt{par} while the condition is parsed
    * with the help of {@link ConditionUtil#createCondition(CallCenter,int,ConditionParams)}.
    * @param cc the call center model.
    * @param k the call type for which the routing case concerns.
    * @param par the case parameters.
    */
   public RoutingCase (CallCenter cc, int k, RoutingCaseParams par) {
      if (par.isSetAgentGroupRanks ()) {
         aRanks = par.getAgentGroupRanks ();
         aRanksRel = false;
         aRanksFunc = new RankForAgentSelection();
      }
      else if (par.isSetAgentGroupRanksRel ()) {
         aRanks = par.getAgentGroupRanksRel ();
         aRanksRel = true;
         aRanksFunc = new RankForAgentSelection();
      }
      else if (par.isSetAgentGroupRanksFunc ())
         aRanksFunc = ConditionUtil.createCustom (RankFunction.class, cc, k, par.getAgentGroupRanksFunc ());
      else
         aRanksFunc = new RankForAgentSelection();
      if (aRanks != null && aRanks.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("Invalid length of agentGroupRanks");
      
      if (par.isSetQueueRanks ()) {
         qRanks = par.getQueueRanks ();
         qRanksRel = false;
         qRanksFunc = new RankForContactSelection();
      }
      else if (par.isSetQueueRanksRel ()) {
         qRanks = par.getQueueRanksRel ();
         qRanksRel = true;
         qRanksFunc = new RankForContactSelection();
      }
      else if (par.isSetQueueRanksFunc ())
         qRanksFunc = ConditionUtil.createCustom (RankFunction.class, cc, k, par.getQueueRanksFunc ());
      else
         qRanksFunc = new RankForContactSelection();
      if (qRanks != null && qRanks.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("Invalid length of queueRanks");
      cond = ConditionUtil.createCondition (cc, k, par);
   }

   /**
    * Similar to constructor {@link #RoutingCase(CallCenter,int,RoutingCaseParams)},
    * for the default case with no condition.
    */
   public RoutingCase (CallCenter cc, int k, DefaultCaseParams par) {
      if (par.isSetAgentGroupRanks ()) {
         aRanks = par.getAgentGroupRanks ();
         aRanksRel = false;
         aRanksFunc = new RankForAgentSelection();
      }
      else if (par.isSetAgentGroupRanksRel ()) {
         aRanks = par.getAgentGroupRanksRel ();
         aRanksRel = true;
         aRanksFunc = new RankForAgentSelection();
      }
      else if (par.isSetAgentGroupRanksFunc ())
         aRanksFunc = ConditionUtil.createCustom (RankFunction.class, cc, k, par.getAgentGroupRanksFunc ());
      else
         aRanksFunc = new RankForAgentSelection();
      if (aRanks != null && aRanks.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("Invalid length of agentGroupRanks");
      
      if (par.isSetQueueRanks ()) {
         qRanks = par.getQueueRanks ();
         qRanksRel = false;
         qRanksFunc = new RankForContactSelection();
      }
      else if (par.isSetQueueRanksRel ()) {
         qRanks = par.getQueueRanksRel ();
         qRanksRel = true;
         qRanksFunc = new RankForContactSelection();
      }
      else if (par.isSetQueueRanksFunc ())
         qRanksFunc = ConditionUtil.createCustom (RankFunction.class, cc, k, par.getQueueRanksFunc ());
      else
         qRanksFunc = new RankForContactSelection();
      if (qRanks != null && qRanks.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("Invalid length of queueRanks");
   }
   
   /**
    * Creates a new routing case with condition
    * \texttt{cond}, and vectors of ranks
    * \texttt{aRanks} and \texttt{qRanks} for
    * agent selection and queue priority.
    */
   public RoutingCase (Condition cond, double[] aRanks, double[] qRanks) {
      this.cond = cond;
      this.aRanks = aRanks;
      this.qRanks = qRanks;
   }
   
   /**
    * Returns the condition associated with this case, or
    * \texttt{null} for the default case.
    */
   public Condition getCondition() {
      return cond;
   }
   
   /**
    * Returns the vector of ranks for agent selection,
    * for this routing case.
    */
   public double[] getAgentGroupRanks () {
      return aRanks;
   }
   
   /**
    * Returns the vector of ranks for queue priority,
    * for this routing case.
    */
   public double[] getQueueRanks () {
      return qRanks;
   }
   
   /**
    * Determines if the vector of ranks for agent
    * groups is relative for this routing case.
    */
   public boolean isAgentGroupRanksRelative() {
      return aRanksRel;
   }
   
   /**
    * Same as {@link #isAgentGroupRanksRelative()},
    * for the vector of ranks of waiting queues.
    */
   public boolean isQueueRanksRelative() {
      return qRanksRel;
   }
   
   public RankFunction getAgentGroupRanksFunction() {
      return aRanksFunc;
   }
   
   public RankFunction getQueueRanksFunction() {
      return qRanksFunc;
   }
   
   private class RankForAgentSelection implements RankFunction {
      public boolean canReturnFiniteRank (int i) {
         if (aRanks != null && !Double.isInfinite (aRanks[i]))
            return true;
         return false;
      }

      public boolean updateRanks (Contact contact, double[] ranks) {
         if (aRanks == null)
            return false;
         if (aRanksRel) {
            for (int i = 0; i < ranks.length; i++) {
               if (Double.isInfinite (ranks[i]) || 
                     Double.isInfinite (aRanks[i]))
                  ranks[i] = aRanks[i];
               else
                  ranks[i] += aRanks[i];
            }
         }
         else
            System.arraycopy (aRanks, 0, ranks, 0, ranks.length);
         return true;
      }
   }

   private class RankForContactSelection implements RankFunction {
      public boolean canReturnFiniteRank (int i) {
         if (qRanks == null)
            return aRanksFunc.canReturnFiniteRank (i);
         return qRanks != null && !Double.isInfinite (qRanks[i]);
      }

      public boolean updateRanks (Contact contact, double[] ranks) {
         if (qRanks == null)
            return aRanksFunc.updateRanks (contact, ranks);
         if (qRanksRel) {
            for (int i = 0; i < ranks.length; i++) {
               if (Double.isInfinite (ranks[i]) || 
                     Double.isInfinite (qRanks[i]))
                  ranks[i] = qRanks[i];
               else
                  ranks[i] += qRanks[i];
            }
         }
         else
            System.arraycopy (qRanks, 0, ranks, 0, ranks.length);
         return true;
      }
   }
}
