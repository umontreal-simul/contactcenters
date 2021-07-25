package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.msk.params.IndexThreshParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;

/**
 * Stores information about an index, a threshold,
 * and a relationship.
 * This is similar to
 * the JAXB-derived {@link IndexThreshParams} class, except
 * that the index and threshold are stored into
 * fields of built-in types rather than wrappers.
 * This class is used as a base for some condition objects,
 * and to hold information about conditions on statistics.
 */
public class IndexThreshInfo {

   protected int i;
   protected double threshold;
   protected Relationship rel;

   /**
    * Constructs a new object holding the index
    * \texttt{i}, the threshold
    * \texttt{threshold}, and
    * the relationship \texttt{rel}.
    */
   public IndexThreshInfo (int i, double threshold, Relationship rel) {
      this.i = i;
      this.threshold = threshold;
      this.rel = rel;
   }

   /**
    * Returns the value of $i$.
    */
   public int getIndex () {
      return i;
   }

   /**
    * Returns the value of $\eta$.
    */
   public double getThreshold () {
      return threshold;
   }

   /**
    * Returns the relationship to be tested.
    */
   public Relationship getRelationship () {
      return rel;
   }

}
