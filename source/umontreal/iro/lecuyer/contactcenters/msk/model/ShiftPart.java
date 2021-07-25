package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.List;

import umontreal.iro.lecuyer.contactcenters.msk.params.ShiftPartParams;

/**
 * Represents the part of a shift in a schedule.
 * A shift part is a time interval with an additional
 * field giving its type.
 */
public class ShiftPart extends TimeInterval {
   /**
    * The text ``Working''.
    */
   public static String WORKING = "Working";
   private String type;

   /**
    * Constructs a new shift part using the call ceneter
    * \texttt{cc}, and parameters \texttt{par}.
    * @param cc the call center.
    * @param par the parameters for the part.
    */
   public ShiftPart (CallCenter cc, ShiftPartParams par) {
      super (cc, par);
      type = par.getType();
   }
   
   /**
    * Constructs a new shift part using the given starting
    * time, ending time, and type.
    * @param startingTime the starting time of the shift part.
    * @param endingTime the ending time of the shift part.
    * @param type the type of the part.
    */
   public ShiftPart (double startingTime, double endingTime, String type) {
      super (startingTime, endingTime);
      this.type = type;
   }
   
   /**
    * Returns the type associated with this shift part.
    * @return the type of this shift part.
    */
   public String getType() {
      return type;
   }
   
   /**
    * Determines if agents are working during this part of
    * the shift. This method returns \texttt{true}
    * if and only if the string returns by
    * {@link #getType()} is equal to
    * \texttt{Working}, case insensitive.
    * @return the success indicator of the test.
    */
   public boolean isWorking() {
      return type != null && type.equalsIgnoreCase (WORKING);
   }
   
   /**
    * Constructs an array of shift parts
    * from the list of part parameters.
    * @param cc the call center.
    * @param intervalList the list of part parameters.
    * @return the array of shift parts.
    */
   public static ShiftPart[] create1 (CallCenter cc, List<ShiftPartParams> intervalList) {
      final ShiftPart[] intervals = new ShiftPart[intervalList.size ()];
      int idx = 0;
      for (final ShiftPartParams tp : intervalList)
         try {
            intervals[idx] = new ShiftPart (cc, tp);
            ++idx;
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            ("Cannot initialize shift part " + idx);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      return intervals;
   }
}
