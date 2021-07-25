package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Arrays;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactInstantiationException;
import umontreal.iro.lecuyer.contactcenters.dialer.DialerList;
import umontreal.iro.lecuyer.contactcenters.msk.params.DialerLimitParams;
import umontreal.ssj.rng.RandomStream;

/**
 * Represents a dialer list imposing
 * limits on the number of calls to dial.
 */
public class DialerListWithLimits implements DialerList {
   private CallCenter cc;
   // KoxP matrix initialized from the random-type call factory
   // and giving a probability of producing a call type
   // for each main period
   private double[][] probPeriod;
   // Random stream to generate call type identifiers
   private RandomStream stream;
   // The information about limits
   private DialerLimit[] limits;
   // For each limit, number of calls dialed so far for
   // which the limit applies.
   private int[] numDialed;
   // Temporary array of probabilities filled
   // each time a call type is selected
   private double[] tmpProb;
   
   // Used to determine the size of the dialer list only
   // Two arrays of size Ko.
   private int[] parents;
   private int[] sizeTypes;

   /**
    * Constructs a new dialer list with limits for the call center
    * \texttt{cc}, call type \texttt{k}, and
    * limits \texttt{limits}.
    * @param cc the call center model.
    * @param k the call type identifier.
    * @param limits the dialer's limits.
    */
   public DialerListWithLimits (CallCenter cc, int k, DialerLimitParams... limits) {
      this.cc = cc;
      probPeriod = new double[cc.getNumOutContactTypes ()][cc.getNumMainPeriods ()];
      Arrays.fill (probPeriod[k - cc.getNumInContactTypes ()], 1);
      initLimits (limits);
   }

   /**
    * Constructs a new dialer list with limits for
    * the call center \texttt{cc}, the call factory
    * \texttt{factory} which generates calls of random
    * types, and the limits \texttt{limits}.
    * @param cc the call center model.
    * @param factory the random-type call factory.
    * @param limits the dialer's limits.
    */
   public DialerListWithLimits (CallCenter cc, RandomTypeCallFactory factory,
         DialerLimitParams... limits) {
      this.cc = cc;
      double[][] pr = factory.getProbPeriod ();
      probPeriod = new double[cc.getNumOutContactTypes ()][];
      System.arraycopy (pr, cc.getNumInContactTypes (), probPeriod,
            0, cc.getNumOutContactTypes ());
      stream = factory.getStream ();
      initLimits (limits);
   }

   private void initLimits (DialerLimitParams[] lim) {
      limits = new DialerLimit[lim.length];
      numDialed = new int[lim.length];
      for (int i = 0; i < lim.length; i++)
         limits[i] = new DialerLimit (cc, lim[i]);
      tmpProb = new double[probPeriod.length];
      parents = new int[cc.getNumOutContactTypes ()];
      sizeTypes = new int[cc.getNumOutContactTypes ()];
      initParents();
   }

   public void clear () {
      Arrays.fill (numDialed, 0);
   }

   public Contact removeFirst (int[] contactTypes) {
      final int cp = cc.getPeriodChangeEvent ().getCurrentMainPeriod ();
      double sum = 0;
      int numPossibleTypes = 0;
      if (contactTypes == null)
         for (int ko = 0; ko < probPeriod.length; ko++) {
            tmpProb[ko] = probPeriod[ko][cp];
            sum += tmpProb[ko];
            if (tmpProb[ko] > 0)
               ++numPossibleTypes;
         }
      else {
         Arrays.fill (tmpProb, 0);
         for (final int k : contactTypes) {
            final int ko = k - cc.getNumInContactTypes ();
            if (ko < 0)
               continue;
            if (tmpProb[ko] > 0)
               continue;
            tmpProb[ko] = probPeriod[ko][cp];
            sum += tmpProb[ko];
            if (tmpProb[ko] > 0)
               ++numPossibleTypes;
         }
      }
      if (numPossibleTypes == 0)
         throw new NoSuchElementException ();

      final double t = cc.getPeriodChangeEvent ().simulator ().time ();
      for (int i = 0; i < limits.length; i++) {
         if (t < limits[i].getStartingTime ()
               || t >= limits[i].getEndingTime ())
            continue;
         assert numDialed[i] <= limits[i].getValue ();
         if (numDialed[i] < limits[i].getValue ())
            continue;
         for (int ko = 0; ko < cc.getNumOutContactTypes (); ko++) {
            if (limits[i].hasType (ko + cc.getNumInContactTypes ()) && tmpProb[ko] > 0) {
               sum -= tmpProb[ko];
               tmpProb[ko] = 0;
               --numPossibleTypes;
            }
         }
      }

      if (numPossibleTypes == 0)
         throw new NoSuchElementException ();
      int ko;
      if (numPossibleTypes == 1) {
         // Single type, so find the first type with tmpProb[ko] > 0
         for (ko = 0; ko < tmpProb.length; ko++)
            if (tmpProb[ko] > 0)
               break;
      }
      else {
         // Generate type randomly
         double u = stream == null ? 1 : stream.nextDouble ();
         for (ko = 0; ko < tmpProb.length; ko++) {
            final double pr = tmpProb[ko] / sum;
            if (u <= pr)
               break;
            u -= pr;
         }
      }
      final int k = ko + cc.getNumInContactTypes ();
      for (int i = 0; i < limits.length; i++)
         if (t >= limits[i].getStartingTime ()
               && t < limits[i].getEndingTime ())
            if (limits[i].hasType (k))
               ++numDialed[i];
      
      return cc.getCallFactory (k).newInstance ();
   }

   public Contact newInstance () {
      try {
         return removeFirst (null);
      }
      catch (final NoSuchElementException nse) {
         throw new ContactInstantiationException (this);
      }
   }

   private void initParents() {
      // Resets to Ko disjoint subsets
      for (int k = 0; k < parents.length; k++)
         parents[k] = k;
   }
   
   private int getRoot (int k) {
      // Finds the root for type k
      // Types k1 and k2 are in the same set if
      // getRoot (k1) == getRoot (k2).
      int root = k;
      while (parents[root] != root)
         root = parents[root];
      return root;
   }
   
   private void merge (int k1, int k2) {
      // Merges the set containing type k1 with the
      // set containing type k2
      final int root1 = getRoot (k1);
      final int root2 = getRoot (k2);
      if (root1 != root2)
         parents[root2] = root1;
   }
   
   private int sizeForType (int k) {
      // Computes and returns the maximal number
      // of contacts of type k in the dialer's list.
      final int ko = k - cc.getNumInContactTypes ();
      if (ko < 0)
         // Inbound type
         return 0;
      final int cp = cc.getPeriodChangeEvent ().getCurrentMainPeriod ();
      if (probPeriod[ko][cp] == 0)
         // Contacts of that type cannot be produced at current time
         return 0;
      final double t = cc.getPeriodChangeEvent ().simulator ().time ();
      // Default unbounded size if no limit applies
      int num = Integer.MAX_VALUE;
      for (int i = 0; i < limits.length; i++)
         if (t >= limits[i].getStartingTime ()
               && t < limits[i].getEndingTime ())
            if (limits[i].hasType (k)) {
               final int lim = limits[i].getValue () - numDialed[i];
               // The most restrictive limit has precedence
               // over the other limits.
               num = Math.min (num, lim);
            }
      return num;
   }

   public int size (int[] contactTypes) {
      if (limits.length == 0)
         return Integer.MAX_VALUE;
      if (contactTypes != null && contactTypes.length == 1)
         return sizeForType (contactTypes[0]);

      // We construct a set of call types for each limit that applies
      // at current time.
      initParents();
      final double t = cc.getPeriodChangeEvent ().simulator ().time ();
      for (final DialerLimit lim : limits)
         if (t >= lim.getStartingTime ()
               && t < lim.getEndingTime ()) {
            int firstType = -1;
            for (int ko = 0; ko < parents.length; ko++) {
               if (!lim.hasType (ko + cc.getNumInContactTypes ()))
                  continue;
               if (firstType == -1)
                  firstType = ko;
               else
                  merge (firstType, ko);
            }
         }
      
      if (contactTypes == null) {
         // No restriction of contact type given to size.
         // First, we need to figure out the maximal number of contacts
         // of each type.
         for (int ko = 0; ko < sizeTypes.length; ko++)
            sizeTypes[ko] = sizeForType (ko + cc.getNumInContactTypes ());
         // If the typesets defined for
         // every limit that applies are mutually disjoint,
         // the size of the list
         // is the sum of elements in sizeTypes.
         // In general, this is not true so we have
         // to reset some elements of sizeTypes to 0.

         for (int ko = 0; ko < sizeTypes.length; ko++) {
            final int root = getRoot (ko);
            if (root != ko) {
               // The set containing contact type ko contains
               // other contact types so we keep only the greatest
               // size, and reset the other size to 0.
               sizeTypes[root] = Math.max (sizeTypes[root], sizeTypes[ko]);
               sizeTypes[ko] = 0;
            }
         }
      }
      else {
         // Same logic as above, but we iterate over a user-defined
         // set of contact types instead of all outbound contact types.
         Arrays.fill (sizeTypes, 0);
         for (final int k : contactTypes) {
            final int ko = k - cc.getNumInContactTypes ();
            if (ko < 0)
               // Inbound type: skip
               continue;
            sizeTypes[ko] = sizeForType (ko);
         }
         
         for (final int k : contactTypes) {
            final int ko = k - cc.getNumInContactTypes ();
            if (ko < 0)
               continue;
            final int root = getRoot (ko);
            if (root != ko) {
               sizeTypes[root] = Math.max (sizeTypes[root], sizeTypes[ko]);
               sizeTypes[ko] = 0;
            }
         }
      }
      
      // Sum up the remaining sizes to get the total list size
      int size = 0;
      for (final int sizeT : sizeTypes) {
         if (sizeT == Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
         size += sizeT;
      }
       return size;
   }
}
