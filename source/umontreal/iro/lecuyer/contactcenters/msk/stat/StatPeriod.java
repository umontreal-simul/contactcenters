package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents an object capable of assigning
 * a statistical period to any observed call.
 * An object implementing this interface is used
 * by {@link CallCenterMeasureManager} and other
 * associated observers to separate calls in periods.
 * 
 * Note that the values returned by
 * {@link #getNumPeriodsForCounters()},
 * {@link #getNumPeriodsForCountersAwt()}, and
 * {@link #needsSlidingWindows()}
 * should never change from call to call, for
 * a given object implementing this interface.
 */
public interface StatPeriod extends AWTPeriod {
   /**
    * Returns the number of periods in usual matrices of counters
    * updated throughout the simulation.
    * Usually, this corresponds to $P+2$, the total number
    * of periods, but this returns 1 for a steady-state
    * simulation over a single period.
    * 
    * @return the number of periods for matrices of counters.
    */
   public int getNumPeriodsForCounters ();

   /**
    * Similar to {@link #getNumPeriodsForCounters()}, for
    * matrices of counters using acceptable waiting times.
    * This usually returns $P'$, the total number of
    * segments regrouping main periods.
    * But this returns 1 for a steady-state
    * simulation over a single period.
    * 
    * @return the number of periods for matrices of counters.
    */
   public int getNumPeriodsForCountersAwt ();

   /**
    * Returns the statistical period of a contact \texttt{contact}.
    * If a negative index is returned for a given contact, this
    * contact is not counted in statistics.
    * This often corresponds to the period during which the contact
    * arrives, but this always returns 0 for steady-state simulations.
    */
   public int getStatPeriod (Contact contact);

   /**
    * Similar to {@link #getStatPeriod(Contact)}, for
    * a statistic using an acceptable waiting time.
    * If a negative index is returned for a given contact, this
    * contact is not counted in statistics.
    * Often, this returns {@link #getStatPeriod(Contact)} minus 1. 
    */
   public int getStatPeriodAwt (Contact contact);
   
   /**
    * Determines if statistics for segments
    * regrouping main periods are collected for measure types
    * using acceptable waiting times.
    * Usually, statistics are collected for each main
    * period, and sums are computed at a later time if
    * needed.
    * However, statistics based on acceptable waiting times
    * cannot be summed, because the AWT may change
    * from periods to periods in general.
    * This method thus indicates if observers
    * must collect observations for groups of main
    * periods in addition to the statistical periods
    * of calls.
    */
   public boolean needsStatForPeriodSegmentsAwt();

   /**
    * Returns the default statistical period.
    * This usually corresponds to the current period.
    */
   public int getStatPeriod ();
   
   /**
    * Determines if sliding windows are needed by
    * statistical counters using
    * an object implementing this interface
    * to get the statistical periods of calls.
    * Usually, the period index returned by
    * {@link #getStatPeriod(Contact)} is never greater than
    * the integer returned by {@link #getNumPeriodsForCounters()}, and
    * the same relationship holds for
    * {@link #getStatPeriodAwt(Contact)} and
    * {@link #getNumPeriodsForCountersAwt()}.
    * However, this assumption can be violated if
    * one needs to get real-time statistics concerning
    * the last observed periods.
    * In such cases, matrices of counters need to be implemented
    * using sliding windows:
    * when the index a statistical period becomes higher than
    * the number of stored periods, the first periods are discarded.
    * This method determines if such sliding windows
    * are needed.       
    */
   public boolean needsSlidingWindows();
}
