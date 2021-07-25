package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.List;

import umontreal.iro.lecuyer.contactcenters.msk.params.TimeIntervalParams;

/**
 * Represents a time interval.
 */
public class TimeInterval {
   private double startingTime;
   private double endingTime;
   
   /**
    * Constructs a time interval from the call center
    * \texttt{cc}, and the parameters \texttt{par}.
    * This constructor converts times in
    * \texttt{par}, expressed as XML durations,
    * to the default time unit used by call center
    * \texttt{cc}.
    * It then checks that the starting time
    * of the interval is not greater than its ending time.
    * @param cc the call center.
    * @param par the parameters.
    */
   public TimeInterval (CallCenter cc, TimeIntervalParams par) {
      startingTime = cc.getTime (par.getStartingTime ());
      endingTime = cc.getTime (par.getEndingTime ());
      if (startingTime > endingTime)
         throw new IllegalArgumentException
         ("The starting time " + par.getStartingTime () + " must be smaller than the ending time " + par.getEndingTime ());
   }
   
   /**
    * Constructs a new time interval from the given
    * starting and ending times.
    * @param startingTime the starting time.
    * @param endingTime the ending time.
    */
   public TimeInterval (double startingTime, double endingTime) {
      if (startingTime > endingTime)
         throw new IllegalArgumentException
         ("The starting time " + startingTime + " must be smaller than the ending time " + endingTime);
      this.startingTime = startingTime;
      this.endingTime = endingTime;
   }
   
   /**
    * Returns the starting time of this
    * interval.
    * @return the starting time.
    */
   public double getStartingTime() {
      return startingTime;
   }
   
   /**
    * Returns the ending time of this interval.
    * @return the ending time.
    */
   public double getEndingTime() {
      return endingTime;
   }
   
   /**
    * Verifies that the intervals of the given array are
    * non-decreasing and do not overlap.
    * This method throws an illegal-argument exception
    * if the check fails.
    * @param intervals the array of intervals to check.
    */
   public static void checkIntervals (TimeInterval... intervals) {
      if (intervals.length < 2)
         return;
      for (int i = 0; i < intervals.length - 1; i++) {
         final TimeInterval interval = intervals[i];
         final TimeInterval nextInterval = intervals[i+1];
         if (nextInterval.getStartingTime () < interval.getEndingTime ())
            throw new IllegalArgumentException
            ("The starting time of the interval " +
                  (i+1) + " must not be smaller than the ending time of the interval " + i);
      }
   }
   
   /**
    * Constructs an array of time intervals
    * from the list of interval parameters.
    * @param cc the call center.
    * @param intervalList the list of interval parameters.
    * @return the array of intervals.
    */
   public static TimeInterval[] create (CallCenter cc, List<TimeIntervalParams> intervalList) {
      final TimeInterval[] intervals = new TimeInterval[intervalList.size ()];
      int idx = 0;
      for (final TimeIntervalParams tp : intervalList)
         try {
            intervals[idx] = new TimeInterval (cc, tp);
            ++idx;
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            ("Cannot initialize time interval " + idx);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      return intervals;
   }
}
