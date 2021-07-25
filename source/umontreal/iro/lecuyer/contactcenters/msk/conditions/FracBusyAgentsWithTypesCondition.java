package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Represents a condition comparing the fraction of busy agents in two groups,
 * possibly restricted to specific call types.
 * This is similar to {@link FracBusyAgentsCondition},
 * except that the number of busy agents serving a contact
 * of a given type can be used rather than
 * the total number of busy agents.
 * More specifically, the fraction of busy agents
 * for group $i_1$ is determined using the number
 * of busy agents serving calls of type $k_1$.
 * If $k_1<0$, the total number of busy agents is used
 * instead.
 * A similar logic is used to get the fraction of busy
 * agents in group $i_2$.
 */
public class FracBusyAgentsWithTypesCondition extends FracBusyAgentsCondition {
   private int k1;
   private int k2;

   /**
    * Constructs a new condition on the fraction of
    * busy agents for call center \texttt{cc},
    * agent groups \texttt{i1} and \texttt{i2},
    * call types \texttt{k1} and \texttt{k2}, and using
    * relationship \texttt{rel} for comparison.
    * @param cc the call center model.
    * @param i1 the index of the first agent group.
    * @param i2 the index of the second agent group.
    * @param k1 the index of the first call type.
    * @param k2 the index of the second call type.
    * @param rel the relationship used for comparison.
    */
   public FracBusyAgentsWithTypesCondition (CallCenter cc, int i1, int i2,
         int k1, int k2,
         Relationship rel) {
      super (cc, i1, i2, rel);
      this.k1 = k1;
      this.k2 = k2;
   }

   /**
    * Returns the call type index for the first compared
    * agent group.
    */
   public int getFirstType() {
      return k1;  
   }
   
   /**
    * Returns the call type index for the second compared
    * agent group.
    */
   public int getSecondType() {
      return k2;
   }

   @Override
   public boolean applies (Contact contact) {
      final CallCenter cc = getCallCenter ();
      final AgentGroup grp1 = cc.getAgentGroup (getFirstIndex ());
      final double d1 = grp1.getNumAgents () + grp1.getNumGhostAgents ();
      final double f1;
      if (k1 >= 0)
         f1 = grp1.getNumBusyAgents (k1) / d1;
      else
         f1 = grp1.getNumBusyAgents () / d1;
      final AgentGroup grp2 = cc.getAgentGroup (getSecondIndex ());
      final double d2 = grp2.getNumAgents () + grp2.getNumGhostAgents ();
      final double f2;
      if (k2 >= 0)
         f2 = grp2.getNumBusyAgents (k2) / d2;
      else
         f2 = grp2.getNumBusyAgents () / d2;
      return ConditionUtil.applies (f1, f2, getRelationship ());
   }
}
