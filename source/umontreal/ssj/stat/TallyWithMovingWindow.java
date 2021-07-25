package umontreal.ssj.stat;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.list.ListOfTallies;

/**
 * Represents a tally with a moving window of fixed length. Usually, a tally
 * counts the observations from the time it is initialized to the current time.
 * This tally can be used to collect observations during a moving time window
 * divided in $P$ successive intervals of fixed duration $d$. This tally defines
 * $P$ internal tallies collecting period-specific observations. When an
 * observation is given to this tally, it is added into the current tally which
 * is changed by a simulation event happening every $d$ time units. After the
 * $P$th period, the first tally becomes the current tally again, and its
 * observations are lost.
 * 
 * This tally can be used as a usual tally, except {@link #start()} must be
 * called at the simulation time corresponding to the beginning of the first
 * collecting period.
 */
public class TallyWithMovingWindow extends Tally {
   private ListOfTallies<?> tallies;
   private double collectingPeriodDuration;
   private double endingTime;

   private int startingTally;
   private int numUsedTallies;
   private Event changeEvent = new ChangeEvent ();
   private int numObs;

   /**
    * Constructs a new tally with moving window using
    * \texttt{numCollectingPeriods} of duration \texttt{collectingPeriodDuration},
    * and with simulation ending at \texttt{endingTime}.
    * 
    * @param keepObs
    *    determines if the internal tallies can keep observations.
    * @param numCollectingPeriods
    *           the number of collecting periods.
    * @param collectingPeriodDuration
    *           the duration of a collecting period.
    * @param endingTime
    *           the ending time of the simulation.
    */
   public TallyWithMovingWindow (boolean keepObs, int numCollectingPeriods,
         double collectingPeriodDuration, double endingTime) {
      this (null, keepObs, numCollectingPeriods, collectingPeriodDuration, endingTime);
   }
   
   /**
    * Constructs a new tally with moving window with name \texttt{name}, using
    * \texttt{numCollectingPeriods} of duration \texttt{collectingPeriodDuration}, and
    * with simulation ending at \texttt{endingTime}.
    * 
    * @param name
    *           the name of the tally.
    * @param keepObs
    *    determines if the internal tallies can keep observations.
    * @param numCollectingPeriods
    *           the number of collecting periods.
    * @param collectingPeriodDuration
    *           the duration of a collecting period.
    * @param endingTime
    *           the ending time of the simulation.
    */
   public TallyWithMovingWindow (String name, boolean keepObs, int numCollectingPeriods,
         double collectingPeriodDuration, double endingTime) {
      super (name);
      if (numCollectingPeriods <= 0)
         throw new IllegalArgumentException (
               "The number of collecting periods must be greater than 0");
      if (collectingPeriodDuration <= 0)
         throw new IllegalArgumentException (
               "The duration of the collecting periods must be greater than 0");
      tallies = keepObs ? ListOfTallies.createWithTallyStore (numCollectingPeriods) : ListOfTallies.createWithTally (numCollectingPeriods);
      this.collectingPeriodDuration = collectingPeriodDuration;
      this.endingTime = endingTime;
   }
   
   /**
    * Returns the $i$th internal tally. 
    * @param i the index of the internal tally.
    * @return the internal tally.
    */
   public Tally getTally (int i) {
      if (i < 0 || i >= tallies.size())
         throw new IllegalArgumentException
         ("The tally index must be greather than or equal to 0, and smaller than " + tallies.size());
      final int idx = (i + startingTally) % tallies.size();
      return tallies.get (idx);
   }

   /**
    * Returns the number of collecting periods.
    * 
    * @return the number of collecting periods.
    */
   public int getNumCollectingPeriods () {
      return tallies.size ();
   }

   /**
    * Returns the duration of the collecting periods.
    * 
    * @return the duration of the collecting periods.
    */
   public double getCollectingPeriodDuration () {
      return collectingPeriodDuration;
   }

   /**
    * Initializes this tally as well as all
    * internal tallies.
    */
   @Override
   public void init () {
      super.init();
      numObs = 0;
      startingTally = 0;
      numUsedTallies = 1;
      if (tallies != null)
         for (final Tally ta : tallies)
            ta.init ();
   }

   /**
    * Starts this tally with moving window by scheduling the first
    * period-changing event. This method should be called at the simulation time
    * corresponding to the first period.
    */
   public void start () {
      schedule ();
   }

   /**
    * Stops this tally with moving average by cancelling the currently scheduled
    * period-changing event.
    */
   public void stop () {
      changeEvent.cancel ();
   }

   @Override
   public void add (double x) {
      super.add (x);
      ++numObs;
      final Tally tally = getCurrentTally ();
      tally.add (x);
   }

   @Override
   public double average () {
      final int n = numberObs ();
      if (n == 0)
         return Double.NaN;
      return sum () / n;
   }

   /**
    * Clones this tally as well as the internal
    * tallies.
    */
   @Override
   public TallyWithMovingWindow clone () {
      final TallyWithMovingWindow cpy = (TallyWithMovingWindow) super.clone ();
      cpy.changeEvent = cpy.new ChangeEvent ();
      final ListOfTallies<Tally> lst = new ListOfTallies<Tally> ();
      cpy.tallies = lst;
      for (final Tally tally : tallies)
         lst.add (tally.clone ());
      return cpy;
   }

   @Override
   public int numberObs () {
//      int n = 0;
//      for (int i = 0; i < numUsedTallies; i++)
//         n += getTally (i).numberObs ();
//      return n;
      return numObs;
   }

   /**
    * Computes and returns the sample variance of the observations
    * in the moving window.
    * This method can be used only if the internal
    * tallies can store observations.
    */
   @Override
   public double variance () {
      double var = 0;
      for (int i = 0; i < numUsedTallies; i++) {
         final int idxi = (i + startingTally) % tallies.size();
         for (int j = 0; j < numUsedTallies; j++) {
            final int idxj = (j + startingTally) % tallies.size();
            final double cov = tallies.covariance (idxi, idxj);
            var += cov;
         }
      }
      return var;
   }

   @Override
   public double max () {
      double max = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < numUsedTallies; i++) {
         final double m = getTally (i).max ();
         if (m > max)
            max = m;
      }
      return max;
   }

   @Override
   public double min () {
      double min = Double.POSITIVE_INFINITY;
      for (int i = 0; i < numUsedTallies; i++) {
         final double m = getTally (i).min ();
         if (m < min)
            min = m;
      }
      return min;
   }

   private Tally getCurrentTally () {
      int idx = startingTally - 1;
      if (idx < 0)
         idx = numUsedTallies - 1;
      return tallies.get (idx);
   }

   private void newTally () {
      if (numUsedTallies < tallies.size ())
         ++numUsedTallies;
      else {
         ++startingTally;
         if (startingTally >= tallies.size ())
            startingTally = 0;
         final Tally ta = getCurrentTally();
         sumValue -= ta.sum();
         numObs -= ta.numberObs ();
         ta.init ();
      }
   }

   private void schedule () {
      final double nextTime = Sim.time () + collectingPeriodDuration;
      if (nextTime < endingTime)
         changeEvent.schedule (collectingPeriodDuration);
   }

   private class ChangeEvent extends Event {
      @Override
      public void actions () {
         newTally ();
         TallyWithMovingWindow.this.schedule ();
      }
   }
}
