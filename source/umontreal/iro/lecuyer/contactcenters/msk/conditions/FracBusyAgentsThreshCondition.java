package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Represents a condition comparing the fraction of
 * busy agents in a group with a threshold.
 * Let $i$ be the index of an agent group, $\eta$
 * be a threshold, and $\cdot$, a relationship.
 * The condition applies if and only if
 * $\frac{\Nb[i](t)}{N_i(t)+\Ng[i](t)}\cdot\eta$.
 */
public class FracBusyAgentsThreshCondition extends IndexThreshInfo implements Condition {
   private CallCenter cc;
   
   /**
    * Constructs a new condition on the
    * fraction of busy agents for the call
    * center model \texttt{cc}, the
    * agent group index \texttt{i},
    * the threshold \texttt{threshold}, and
    * for which comparisons are
    * made using relationship \texttt{rel}.
    * @param cc the call center model.
    * @param i the index of the agent group.
    * @param threshold the threshold on the fraction
    * of busy agents.
    * @param rel the relationship used for comparison.
    */
   public FracBusyAgentsThreshCondition (CallCenter cc, int i,
         double threshold,
         Relationship rel) {
      super (i, threshold, rel);
      this.cc = cc;
   }

   /**
    * Returns a reference to the call center associated
    * with this condition.
    */
   public CallCenter getCallCenter() {
      return cc;
   }
   
   public boolean applies (Contact contact) {
      final AgentGroup grp = cc.getAgentGroup (getIndex ());
      final double f = grp.getNumBusyAgents () / (grp.getNumAgents () + grp.getNumGhostAgents ());
      return ConditionUtil.applies (f, getThreshold (), getRelationship ());
   }
}
