package umontreal.iro.lecuyer.contactcenters.msk.stat;

/**
 * Represents a type of statistic used by {@link StatCallCenterStat}.
 */
public enum StatType {
   /**
    * Average.
    */
   AVERAGE,

   /**
    * Variance.
    */
   VARIANCE,

   /**
    * Standard deviation.
    */
   STANDARDDEVIATION,

   /**
    * Variance divided by the number of observations in the inner tally.
    */
   VARIANCEOFAVERAGE,

   /**
    * Standard deviation divided by the square root of the number of
    * observations in the inner tally.
    */
   STANDARDDEVIATIONOFAVERAGE
}
