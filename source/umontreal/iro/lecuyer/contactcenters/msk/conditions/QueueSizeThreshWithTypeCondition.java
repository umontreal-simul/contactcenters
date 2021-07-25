package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition comparing the number of calls
 * of a given type in a given queue with a threshold.
 * This is similar to {@link QueueSizeThreshCondition},
 * with the possibility to restrict the number of queued
 * calls to a given type.
 */
public class QueueSizeThreshWithTypeCondition extends QueueSizeThreshCondition {
   private int type;

   /**
    * Constructs a new condition on the queue size
    * for the call center \texttt{cc}, queue
    * with index \texttt{index}, calls of type
    * \texttt{type}, with threshold
    * \texttt{threshold}, and
    * using relation \texttt{rel}
    * for comparison.
    * @param cc the call center model.
    * @param index the index of the waiting queue.
    * @param type the call type index.
    * @param threshold the threshold.
    * @param rel the relationship used to perform the comparison.
    */
   public QueueSizeThreshWithTypeCondition (CallCenter cc, int index,
         int type, int threshold, Relationship rel) {
      super (cc, index, threshold, rel);
      this.type = type;
   }

   /**
    * Returns the type identifier for
    * which this condition is evaluated.
    */
   public int getType() {
      return type;
   }
   
   @Override
   public boolean applies (Contact contact) {
      final CallCenter cc = getCallCenter ();
      final int index = getIndex ();
      final int threshold = getThreshold ();
      final int s;
      if (type >= 0)
         s = cc.getWaitingQueue (index).size (type);
      else
         s = cc.getWaitingQueue (index).size ();
      return ConditionUtil.applies (s, threshold, getRelationship ());
   }
}
