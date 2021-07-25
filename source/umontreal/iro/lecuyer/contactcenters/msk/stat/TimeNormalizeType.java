package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;

/**
 * Possible type of time normalizations after a matrix
 * of counters is obtained.
 */
public enum TimeNormalizeType {
   /**
    * The values of the counters are never normalized
    * with respect to time.
    * This applies to measures not corresponding to counts,
    * e.g., the maximal waiting time.
    */
   NEVER,
   
   /**
    * Time normalization is always applied, because the 
    * counters are less sensible if not normalized.
    * This applies, in particular, to the time-average number
    * of agents.
    */
   ALWAYS,
   
   /**
    * Time normalization is performed depending
    * on a user-defined parameter,
    * {@link SimParams#isNormalizeToDefaultUnit()}.
    */
   CONDITIONAL
}
