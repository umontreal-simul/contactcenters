package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.msk.params.TwoIndicesParams;

/**
 * Stores information about two indices and a relationship.
 * This is similar to
 * the JAXB-derived {@link TwoIndicesParams} class, except
 * that the indices are stored into
 * \texttt{int} fields instead of {@link Integer}
 * fields.
 * This class is used as a base for some condition objects,
 * and to hold information about conditions on statistics.
 */
public class TwoIndicesInfo {

   private int i1;
   private int i2;
   private Relationship rel;

   /**
    * Constructs a new data object holding
    * indices \texttt{i1} and \texttt{i2}, as
    * well as relationship \texttt{rel}.
    */
   public TwoIndicesInfo (int i1, int i2, Relationship rel) {
      this.i1 = i1;
      this.i2 = i2;
      this.rel = rel;
   }

   /**
    * Returns the value of $i_1$.
    */
   public int getFirstIndex () {
      return i1;
   }

   /**
    * Returns the value of $i_2$.
    */
   public int getSecondIndex () {
      return i2;
   }

   /**
    * Returns the relationship to be tested.
    */
   public Relationship getRelationship () {
      return rel;
   }
}
