package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition comparing the size of a waiting queue
 * with the size of another queue.
 * Let $Q_q(t)$ be the queue size of queue $q$ at time $t$, and
 * $\cdot$ be a relationship.
 * The condition checks that $Q_{q_1}(t)\cdot Q_{q_2}(t)$ for
 * fixed values of $q_1$, $q_2$, and $\cdot$.
 * The relationship can be $<$, $>$, $=$, $\le$, or $\ge$.
 */
public class QueueSizesCondition extends TwoIndicesInfo implements Condition {
   private CallCenter cc;
   
   /**
    * Constructs a new condition on the queue size for
    * the call center model \texttt{cc}, first
    * waiting queue \texttt{q1},
    * second waiting queue \texttt{q2}, and
    * relationship \texttt{rel}.
    * @param cc the call center model.
    * @param q1 the index of the first waiting queue.
    * @param q2 the index of the second waiting queue.
    * @param rel the relationship used to perform the comparison.
    */
   public QueueSizesCondition (CallCenter cc, int q1, int q2, Relationship rel) {
      super (q1, q2, rel);
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
      int s1 = cc.getWaitingQueue (getFirstIndex ()).size ();
      int s2 = cc.getWaitingQueue (getSecondIndex ()).size ();
      return ConditionUtil.applies (s1, s2, getRelationship ());
   }
}
