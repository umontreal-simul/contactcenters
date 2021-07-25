package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Represents a condition comparing the fraction of busy agents
 * for two groups.
 * Let $i_1$ and $i_2$ be indices of agent groups and
 * $\cdot$ be a relationship.
 * This condition applies if and only if
 * $\frac{\Nb[i_1](t)}{N_{i_1}(t) + \Ng[i_1](t)} \cdot
 * \frac{\Nb[i_2](t)}{N_{i_2}(t) + \Ng[i_2](t)}$.
 */
public class FracBusyAgentsCondition extends TwoIndicesInfo implements Condition {
   private CallCenter cc;
   /**
    * Constructs a new condition on the fraction of busy agents
    * for call center \texttt{cc}, agent groups
    * with indices \texttt{i1} and \texttt{i2}, and
    * relationship \texttt{rel}.
    * @param cc the call center model.
    * @param i1 the index of the first agent group.
    * @param i2 the index of the second agent group.
    * @param rel the relationship used for comparison.
    */
   public FracBusyAgentsCondition (CallCenter cc, int i1, int i2,
         Relationship rel) {
      super (i1, i2, rel);
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
      final AgentGroup grp1 = cc.getAgentGroup (getFirstIndex ());
      final double f1 = grp1.getNumBusyAgents () / (grp1.getNumAgents () + grp1.getNumGhostAgents ());
      final AgentGroup grp2 = cc.getAgentGroup (getSecondIndex ());
      final double f2 = grp2.getNumBusyAgents () / (grp2.getNumAgents () + grp2.getNumGhostAgents ());
      return ConditionUtil.applies (f1, f2, getRelationship ());
   }
}
