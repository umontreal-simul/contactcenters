package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.simevents.Simulator;

/**
 * Defines a contact arrival process with
 * inter-arrival times following a stationary distribution.
 * When an inter-arrival time is required, a random variate generator is used
 * to get a random value.
 */
public class StationaryContactArrivalProcess
   extends ContactArrivalProcess {
   private RandomVariateGen timesGen;

   /**
    * Constructs a new contact arrival process creating contacts
    * using the given \texttt{factory} and
    * using \texttt{timesGen} to generate the
    * inter-arrival times.
    @param factory the factory creating contacts for this arrival process.
    @param timesGen the random variate generator used to generate
    times between arrivals.
    */
   public StationaryContactArrivalProcess (ContactFactory factory,
         RandomVariateGen timesGen) {
      this (Simulator.getDefaultSimulator (), factory, timesGen);
   }
   
   /**
    * Equivalent to {@link #StationaryContactArrivalProcess(ContactFactory,RandomVariateGen)},
    * using the given simulator \texttt{sim}.
    */
   public StationaryContactArrivalProcess (Simulator sim, ContactFactory factory,
                                           RandomVariateGen timesGen) {
      super (sim, factory);
      if (timesGen == null)
         throw new NullPointerException ("The given random variate generator must not be null");
      this.timesGen = timesGen;
   }

   /**
    * Returns the random variate generator used to generate
    * the times between each arrival.
    @return the random variate generator associated with this object.
    */
   public RandomVariateGen getTimesGen() {
      return timesGen;
   }

   /**
    * Sets the random variate generator for inter-arrival times
    * to \texttt{timesGen}.
    @param timesGen the new random variate generator.
    @exception NullPointerException if \texttt{timesGen} is \texttt{null}.
    */
   public void setTimesGen (RandomVariateGen timesGen) {
      if (timesGen == null)
         throw new NullPointerException ("The given random variate generator must not be null");
      this.timesGen = timesGen;
   }
   
   @Override
   public double getArrivalRate (int p) {
      return getBusynessFactor()/timesGen.getDistribution().getMean();
   }
   
   @Override
   public double getArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return getBusynessFactor()/timesGen.getDistribution().getMean();
   }
   
   @Override
   public double getExpectedArrivalRate (int p) {
      return 1.0/timesGen.getDistribution().getMean();
   }
   
   @Override
   public double getExpectedArrivalRate (double st, double et) {
      if (et <= st)
         return 0;
      return 1.0/timesGen.getDistribution().getMean();
   }

   @Override
   public double nextTime() {
      return timesGen.nextDouble()/getBusynessFactor();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (super.toString());
      sb.deleteCharAt (sb.length() - 1);
      sb.append (", random variate generator: ");
      sb.append (timesGen.toString());
      sb.append (']');
      return sb.toString();
   }
}
