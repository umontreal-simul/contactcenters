package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;

import umontreal.ssj.stat.mperiods.SumMatrix;
import umontreal.ssj.stat.mperiods.SumMatrixSW;

/**
 * Defines a new-contact listener that counts the number of outbound calls. This
 * object encapsulates a measure matrix containing $\Ko$ lines. When a contact
 * of type $k$ is notified, it is added in row $k - \Ki$ of the matrix, and
 * column corresponding to its statistical period.
 */
public class OutboundCallCounter implements NewContactListener {
   private SumMatrix count;
   private int ki;
   private StatPeriod statP;

   /**
    * Constructs a new call counter for the call center model
    * \texttt{cc}, and using \texttt{statP}
    * to get statistical periods of calls. 
    * @param cc the call center model.
    * @param statP the object for obtaining statistical periods.
    */
   public OutboundCallCounter (CallCenter cc, StatPeriod statP) {
      ki = cc.getNumInContactTypes();
      final int np = statP.getNumPeriodsForCounters ();
      if (statP.needsSlidingWindows ())
         count = new SumMatrixSW (cc.getNumOutContactTypes (), np);
      else
         count = new SumMatrix (cc.getNumOutContactTypes(), np);
      this.statP = statP;
   }

   /**
    * Returns the sum matrix that contains the counts.
    * 
    * @return the sum matrix.
    */
   public SumMatrix getCount () {
      return count;
   }

   /**
    * Initializes the sum matrix for counting contacts.
    */
   public void init () {
      count.init ();
   }

   public void newContact (Contact contact) {
      final int type = contact.getTypeId ();
      // int period = ((Call)contact).getArrivalPeriod();
      final int period = statP.getStatPeriod (contact);
      if (period < 0)
         return;
      count.add (type - ki, period, 1);
   }
}
