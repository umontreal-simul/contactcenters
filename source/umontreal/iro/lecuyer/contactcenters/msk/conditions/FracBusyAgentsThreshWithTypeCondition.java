package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Represents a condition on the fraction of busy
 * agents serving a specific call type.
 * This is similar to {@link FracBusyAgentsThreshCondition},
 * except that the number of busy agents serving a given
 * call type is used rather than the total number of
 * busy agents.
 */
public class FracBusyAgentsThreshWithTypeCondition extends
      FracBusyAgentsThreshCondition {
   private int k;

   /**
    * Constructs a new condition on the fraction of
    * busy agents for call center model \texttt{cc},
    * agent group index \texttt{i},
    * call type index \texttt{k}, and
    * threshold \texttt{threshold}, and
    * using relationship \texttt{rel}
    * for the comparisons.
    * @param cc the call center model.
    * @param i the index of the agent group.
    * @param k the index of the call type.
    * @param threshold the threshold on the fraction
    * of busy agents.
    * @param rel the relationship used for comparison.
    */
   public FracBusyAgentsThreshWithTypeCondition (CallCenter cc, int i,
         int k,
         double threshold, Relationship rel) {
      super (cc, i, threshold, rel);
      this.k = k;
   }
   
   /**
    * Returns the type identifier for
    * which this condition is evaluated.
    */
   public int getType() {
      return k;
   }

   @Override
   public boolean applies (Contact contact) {
      final AgentGroup grp = getCallCenter ().getAgentGroup (getIndex ());
      final double d = grp.getNumAgents () + grp.getNumGhostAgents ();
      final double f;
      if (k >= 0)
         f = grp.getNumBusyAgents (k) / d;
      else
         f = grp.getNumBusyAgents () / d;
      return ConditionUtil.applies (f, getThreshold (), getRelationship ());
   }
}
