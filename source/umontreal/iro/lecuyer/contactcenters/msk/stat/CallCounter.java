package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.app.RowType;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactSumMatrix;
import umontreal.iro.lecuyer.contactcenters.contact.NewContactListener;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.ssj.stat.mperiods.SumMatrix;

/**
 * Defines a new-contact listener for counting calls. This encapsulates a measure
 * matrix with $K$ rows and a column for
 * each statistical period.
 * Each time a new contact is notified, the element with row $k$ and
 * column $p$ is incremented, where $k$ is the type of the new contact and $p$
 * is its statistical period.
 */
public class CallCounter implements NewContactListener {
   private SumMatrix count;
   private StatPeriod statP;

   /**
    * Constructs a new call counter using
    * call center \texttt{cc}, for type of measure
    * \texttt{mt}, and using \texttt{statP} to obtain
    * statistical periods.
    * The measure type is used to determine if we have
    * a measure using AWT, for which statistical periods
    * are different than with regular measures.
    * @param cc the call center model.
    * @param statP the object for obtaining statistical periods of calls.
    * @param mt the type of measure for the counter.
    */
   public CallCounter (CallCenter cc, StatPeriod statP, MeasureType mt) {
      final int nt = cc.getNumContactTypes();
      final int np;
      if (mt.getRowType (false) == RowType.INBOUNDTYPEAWT)
         np = statP.getNumPeriodsForCountersAwt ();
      else
         np = statP.getNumPeriodsForCounters ();
      count = new ContactSumMatrix (nt, np);
      this.statP = statP;
   }

   /**
    * Returns the matrix containing the counts.
    */
   public SumMatrix getCount () {
      return count;
   }

   /**
    * Initializes the call counter.
    */
   public void init () {
      count.init ();
   }

   public void newContact (Contact contact) {
      final int type = contact.getTypeId();
      final int period = statP.getStatPeriod (contact);
      count.add (type, period, 1);
   }
}
