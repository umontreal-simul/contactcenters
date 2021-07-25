package umontreal.iro.lecuyer.collections;

import java.util.Set;

/**
 * Represents a set that dynamically transforms the elements of another set.
 * This class extends the transforming collection to implement the {@link Set}
 * interface.
 * 
 * @param <OE>
 *           the type of the outer elements
 * @param <IE>
 *           the type of the inner elements
 */
public abstract class TransformingSet<OE, IE> extends
      TransformingCollection<OE, IE> implements Set<OE> {
   /**
    * Constructs a new transforming set mapping the elements of the inner set
    * \texttt{innerSet}.
    * 
    * @param innerSet
    *           the inner set.
    * @exception NullPointerException
    *               if \texttt{innerSet} is \texttt{null}.
    */
   public TransformingSet (Set<IE> innerSet) {
      super (innerSet);
   }

   /**
    * Returns the inner set.
    * 
    * @return the inner set.
    */
   @Override
   public Set<IE> getInnerCollection () {
      return (Set<IE>) super.getInnerCollection ();
   }

   @Override
   public boolean equals (Object o) {
      // This is implemented in AbstractSet, but we cannot
      // inherit both AbstractSet and TransformingCollection.
      if (!(o instanceof Set))
         return false;
      final Set<?> set = (Set<?>) o;
      if (size () != set.size ())
         return false;
      for (final Object e : this)
         if (!set.contains (e))
            return false;
      return true;
   }

   @Override
   public int hashCode () {
      // This is implemented in AbstractSet, but we cannot
      // inherit both AbstractSet and TransformingCollection.
      int hashCode = 0;
      for (final Object e : this)
         hashCode += e == null ? 0 : e.hashCode ();
      return hashCode;
   }
}
