package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition comparing the number of free agents
 * in two groups of a model.
 * Let $\Nf[i](t)$ be the number of free agents in group $i$ at time $t$, and
 * $\cdot$ be a relationship.
 * The condition checks that $\Nf[i_1](t)\cdot \Nf[i_2](t)$ for
 * fixed values of $i_1$, $i_2$, and $\cdot$.
 * The relationship can be $<$, $>$, $=$, $\le$, or $\ge$.
 */
public class NumFreeAgentsCondition extends TwoIndicesInfo implements Condition {
   private CallCenter cc;
   
   /**
    * Constructs a new condition on agent groups for the
    * call center model \texttt{cc}, agent groups
    * with indices \texttt{i1} and \texttt{i2}, and
    * comparing with relationship \texttt{rel}.
    * @param cc the call center model.
    * @param i1 the index of the first agent group.
    * @param i2 the index of the second agent group.
    * @param rel the relationship used for the comparison.
    */
   public NumFreeAgentsCondition (CallCenter cc, int i1, int i2, Relationship rel) {
      super (i1, i2, rel);
      if (i1 < 0 || i1 >= cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("The first index i1 is out of bounds");
      if (i2 < 0 || i2 >= cc.getNumAgentGroups ())
         throw new IllegalArgumentException
         ("The second index i2 is out of bounds");
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
      final int f1 = cc.getAgentGroup (getFirstIndex ()).getNumFreeAgents ();
      final int f2 = cc.getAgentGroup (getSecondIndex ()).getNumFreeAgents ();
      return ConditionUtil.applies (f1, f2, getRelationship ());
   }
}
