package umontreal.iro.lecuyer.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a collection that dynamically transforms the elements of another
 * collection. This abstract class defines a collection containing an inner
 * collection of elements of a certain type, and provides facilities to convert
 * these inner elements to outer elements of another type. A concrete subclass
 * simply needs to implement the {@link #convertFromInnerType} and
 * {@link #convertToInnerType} methods for converting between the inner and the
 * outer types.
 * 
 * It is strongly recommended that the mapping established by the conversion
 * methods be one-to-one, i.e., an element in the inner collection corresponds
 * to a single element in the outer collection. Otherwise, the size of the outer
 * collection might be incorrect, and the iterator may unexpectedly give the
 * same elements multiple times. Also, \texttt{null} should always correspond to
 * \texttt{null}.
 * 
 * @param <OE>
 *           the outer type of the elements.
 * @param <IE>
 *           the inner type of the elements.
 */
public abstract class TransformingCollection<OE, IE> extends
      AbstractCollection<OE> {
   private Collection<IE> innerCollection;

   /**
    * Constructs a new transforming collection mapping the elements of the inner
    * collection \texttt{innerCollection}.
    * 
    * @param innerCollection
    *           the inner collection.
    * @exception NullPointerException
    *               if \texttt{innerCollection} is \texttt{null}.
    */
   public TransformingCollection (Collection<IE> innerCollection) {
      if (innerCollection == null)
         throw new NullPointerException ();
      this.innerCollection = innerCollection;
   }

   /**
    * Returns the inner collection.
    * 
    * @return the inner collection.
    */
   public Collection<IE> getInnerCollection () {
      return innerCollection;
   }

   /**
    * Converts an element in the inner collection to an element of the outer
    * type.
    * 
    * @param e
    *           the inner element.
    * @return the outer element.
    */
   public abstract OE convertFromInnerType (IE e);

   /**
    * Converts an element of the outer type to an element of the inner
    * collection.
    * 
    * @param e
    *           the outer element.
    * @return the inner element.
    */
   public abstract IE convertToInnerType (OE e);

   @Override
   public Iterator<OE> iterator () {
      return new MyItr (innerCollection.iterator ());
   }

   @Override
   public int size () {
      return innerCollection.size ();
   }

   @Override
   public boolean add (OE o) {
      return innerCollection.add (convertToInnerType (o));
   }

   @Override
   public void clear () {
      innerCollection.clear ();
   }

   @SuppressWarnings ("unchecked")
   @Override
   public boolean contains (Object o) {
      final OE oe;
      try {
         oe = (OE) o;
      }
      catch (final ClassCastException e) {
         return false;
      }
      return innerCollection.contains (convertToInnerType (oe));
   }

   @Override
   public boolean isEmpty () {
      return innerCollection.isEmpty ();
   }

   @SuppressWarnings ("unchecked")
   @Override
   public boolean remove (Object o) {
      final OE oe;
      try {
         oe = (OE) o;
      }
      catch (final ClassCastException e) {
         return false;
      }
      return innerCollection.remove (convertToInnerType (oe));
   }

   private class MyItr implements Iterator<OE> {
      private Iterator<IE> itr;

      public MyItr (Iterator<IE> itr) {
         this.itr = itr;
      }

      public boolean hasNext () {
         return itr.hasNext ();
      }

      public OE next () {
         return convertFromInnerType (itr.next ());
      }

      public void remove () {
         itr.remove ();
      }
   }
}
