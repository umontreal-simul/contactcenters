package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Represents a condition on queue sizes possibly for
 * specific call types.
 * This is similar to {@link QueueSizesCondition}, except
 * that the compared queue sizes are determined using an index and
 * a call type.
 * If the given call type is non-negative, the compared size is the
 * number of calls in the identified queue of the identified type.
 * Otherwise, the total number of calls in the identified queue is used. 
 */
public class QueueSizesWithTypesCondition extends QueueSizesCondition {
   private int k1;
   private int k2;

   /**
    * Constructs a new condition on the queue size for call center
    * \texttt{cc}, using queue indices \texttt{q1} and
    * \texttt{q2}, the call type indices
    * \texttt{k1} and \texttt{k2}, and the
    * relationshiop \texttt{rel}.  
    * @param cc the call center model.
    * @param q1 the index of the first waiting queue.
    * @param q2 the index of the second waiting queue.
    * @param k1 the index of the first call type.
    * @param k2 the index of the second call type.
    * @param rel the relationship used to perform the comparison.
    */
   public QueueSizesWithTypesCondition (CallCenter cc, int q1, int q2,
         int k1, int k2,
         Relationship rel) {
      super (cc, q1, q2, rel);
      this.k1 = k1;
      this.k2 = k2;
   }
   
   /**
    * Returns the call type index for the first compared
    * waiting queue.
    */
   public int getFirstType() {
      return k1;  
   }
   
   /**
    * Returns the call type index for the second compared
    * waiting queue.
    */
   public int getSecondType() {
      return k2;
   }
   
   @Override
   public boolean applies (Contact contact) {
      final CallCenter cc = getCallCenter ();
      final int q1 = getFirstIndex ();
      final int q2 = getSecondIndex ();
      int s1;
      if (k1 >= 0)
         s1 = cc.getWaitingQueue (q1).size (k1);
      else
         s1 = cc.getWaitingQueue (q1).size ();
      int s2;
      if (k2 >= 0)
         s2 = cc.getWaitingQueue (q2).size (k2);
      else
         s2 = cc.getWaitingQueue (q2).size ();
      return ConditionUtil.applies (s1, s2, getRelationship ());
   }
}
