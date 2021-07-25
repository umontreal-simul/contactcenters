package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition comparing the number of free agents
 * in a groups of a model with a fixed threshold.
 * Let $\Nf[i](t)$ be the number of free agents in group $i$ at time $t$, and
 * $\cdot$ be a relationship.
 * The condition checks that $\Nf[i](t)\cdot \eta$ for
 * fixed values of $i$, $\eta$, and $\cdot$.
 * The relationship can be $<$, $>$, $=$, $\le$, or $\ge$.
 */
public class NumFreeAgentsThreshCondition implements Condition {
   private CallCenter cc;
   private int i;
   private int threshold;
   private Relationship rel;
   
   /**
    * Constructs a new condition on agent group for the
    * call center model \texttt{cc}, agent group
    * with index \texttt{i}, threshold  \texttt{threshold}, and
    * comparing with relationship \texttt{rel}.
    * @param cc the call center model.
    * @param i the index of the agent group.
    * @param threshold the threshold.
    * @param rel the relationship used for the comparison.
    */
   public NumFreeAgentsThreshCondition (CallCenter cc, int i, int threshold, Relationship rel) {
      if (i < 0 || i >= cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("The first index i1 is out of bounds");
      this.cc = cc;
      this.i = i;
      this.threshold = threshold;
      this.rel = rel;
   }
   
   /**
    * Returns a reference to the call center associated
    * with this condition.
    */
   public CallCenter getCallCenter() {
      return cc;
   }
   
   /**
    * Returns the value of $i$.
    */
   public int getIndex() {
      return i;
   }
   
   /**
    * Returns the value of $\eta$.
    */
   public int getThreshold() {
      return threshold;
   }
   
   /**
    * Returns the relationship to be tested.
    */
   public Relationship getRelationship() {
      return rel;
   }

   public boolean applies (Contact contact) {
      final int f = cc.getAgentGroup (i).getNumFreeAgents ();
      return ConditionUtil.applies (f, threshold, rel);
   }
}
