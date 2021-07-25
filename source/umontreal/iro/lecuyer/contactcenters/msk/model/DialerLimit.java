package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.msk.params.DialerLimitParams;

/**
 * Represents a limit on the number of calls to dial.
 * Such a limit is described by a time interval on which
 * it applies, the maximal number of outbound calls allowed
 * for this dialer during the interval, and the call types on
 * which the limit applies.
 * This class extends the {@link TimeInterval} class
 * for the information about the time interval on which
 * the limit applies.
 */
public class DialerLimit extends TimeInterval {
   private int value;
   private int[] types;
   private boolean[] typeSet;
   private int numInTypes;

   /**
    * Constructs a new dialer limit using the call center
    * \texttt{cc}, and limit parameters
    * \texttt{par}.
    * @param cc the call center model.
    * @param par the limit parameters.
    */
   public DialerLimit (CallCenter cc, DialerLimitParams par) {
      super (cc, par);
      value = par.getValue ();
      typeSet = new boolean[cc.getNumOutContactTypes ()];
      if (par.isSetTypes ()) {
         types = par.getTypes ();
         for (int i = 0; i < types.length; i++) {
            if (types[i] < cc.getNumInContactTypes () || types[i] >= cc.getNumContactTypes ())
               throw new IllegalArgumentException
               ("Type index " + i + " does not correspond to an outbound call type");
            typeSet[types[i] - cc.getNumInContactTypes ()] = true;
         }
      }
      else {
         types = new int[0];
         Arrays.fill (typeSet, true);
      }
      numInTypes = cc.getNumInContactTypes ();
   }
   
   /**
    * Returns the maximal number of calls of the specified
    * typeset during the given interval.
    * @return the value of the limit.
    */
   public int getValue() {
      return value;
   }
   
   /**
    * Returns an array giving the list of call types on
    * which the limit applies.
    * @return the list of call types on which the limit applies.
    */
   public int[] getTypes() {
      return types.clone ();
   }
   
   /**
    * Returns \texttt{true} if
    * and only if this limit applies to call
    * type \texttt{k}.
    * This method always returns \texttt{false}
    * for inbound call types.
    * @param k the tested call type.
    * @return the success indicator of the test.
    */
   public boolean hasType (int k) {
      final int ko = k - numInTypes;
      if (ko < 0 || ko >= typeSet.length)
         return false;
      return typeSet[ko];
   }
}
