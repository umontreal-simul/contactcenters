package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.msk.params.CallSourceParams;
import umontreal.iro.lecuyer.xmlbind.NamedInfo;

/**
 * Represents information concerning a call source,
 * i.e., an arrival process or a dialer.
 */
public class CallSourceManager extends NamedInfo {
   private double[] sourceToggleTimes;
   private boolean isSourceEnabled;
   
   /**
    * Constructs a new call source information object
    * with the given call center and call source
    * parameters.
    * @param cc the call center model.
    * @param par the call source parameters.
    */
   public CallSourceManager (CallCenter cc, CallSourceParams par) {
      super (par);
      if (par.isSetSourceToggleTimes ()) {
         final TimeInterval[] intervals = TimeInterval.create (cc, par.getSourceToggleTimes ());
         TimeInterval.checkIntervals (intervals);
         sourceToggleTimes = new double[intervals.length * 2];
         for (int i = 0; i < intervals.length; i++) {
            sourceToggleTimes[2*i] = intervals[i].getStartingTime ();
            sourceToggleTimes[2*i + 1] = intervals[i].getEndingTime ();
         }
      }
      isSourceEnabled = par.isSourceEnabled ();
   }
   
   /**
    * Returns \texttt{true} if the concerned call source is enabled, i.e., if
    * it produces calls.
    * @return the status of the managed call source.
    */
   public boolean isSourceEnabled() {
      return isSourceEnabled;
   }
   
   /**
    * Returns the source toggle times.
    * This array contains an even number of simulation
    * times, each value representing a starting or stopping
    * time.
    * @return the source toggle times.
    */
   public double[] getSourceToggleTimes() {
      return sourceToggleTimes == null ? null : sourceToggleTimes.clone ();
   }
   
   public void setSourceToggleTimes (double[] sourceToggleTimes) {
      if (sourceToggleTimes == null)
         this.sourceToggleTimes = null;
      else
         this.sourceToggleTimes = sourceToggleTimes.clone ();
   }
}
