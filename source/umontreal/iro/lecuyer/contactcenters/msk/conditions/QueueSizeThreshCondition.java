package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition comparing the size of a waiting queue
 * with a fixed threshold.
 * Let $Q_q(t)$ be the queue size of queue $q$ at time $t$, and
 * $\cdot$ be a relationship.
 * The condition checks that $Q_{q}(t)\cdot \eta$ for
 * fixed values of $q$, $\eta$, and $\cdot$.
 * The relationship can be $<$, $>$, $=$, $\le$, or $\ge$.
 */
public class QueueSizeThreshCondition implements Condition {
   private CallCenter cc;
   private int index;
   private int threshold;
   private Relationship rel;
   
   /**
    * Constructs a new condition on the queue size for
    * the call center model \texttt{cc}, first
    * waiting queue \texttt{index},
    * threshold \texttt{threshold}, and
    * relationship \texttt{rel}.
    * @param cc the call center model.
    * @param index the index of the waiting queue.
    * @param threshold the threshold.
    * @param rel the relationship used to perform the comparison.
    */
   public QueueSizeThreshCondition (CallCenter cc, int index, int threshold, Relationship rel) {
      this.cc = cc;
      this.index = index;
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
    * Returns the value of $q$.
    */
   public int getIndex() {
      return index;
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
      final int s = cc.getWaitingQueue (index).size ();
      return ConditionUtil.applies (s, threshold, rel);
   }
}
