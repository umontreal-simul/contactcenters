package umontreal.iro.lecuyer.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents a collection providing a view of two collections merged together.
 * The merged collection contains the elements of collections \texttt{col1} and
 * \texttt{col2}, and its iterator traverses both collections. A merged
 * collection is immutable, but any change to the inner collections is reflected
 * on the merged collection.
 *
 * @param <E>
 *           the type of the elements in the merged collection.
 */
public class MergedCollection<E> extends AbstractCollection<E> {
   private Collection<? extends E> col1;
   private Collection<? extends E> col2;

   /**
    * Constructs a collection merging collections \texttt{col1} and
    * \texttt{col2}.
    *
    * @param col1
    *           the first collection.
    * @param col2
    *           the second collection.
    * @exception NullPointerException
    *               if \texttt{col1} or \texttt{col2} are \texttt{null}.
    */
   public MergedCollection (Collection<? extends E> col1,
         Collection<? extends E> col2) {
      if (col1 == null || col2 == null)
         throw new NullPointerException ();
      this.col1 = col1;
      this.col2 = col2;
   }

   /**
    * Returns a reference to the first collection of this merged collection.
    *
    * @return the first collection.
    */
   public Collection<? extends E> getFirstCollection () {
      return col1;
   }

   /**
    * Returns a reference to the second collection of this merged collection.
    *
    * @return the second collection.
    */
   public Collection<? extends E> getSecondCollection () {
      return col2;
   }

   @Override
   public boolean contains (Object o) {
      return col1.contains (o) || col2.contains (o);
   }

   @Override
   public boolean isEmpty () {
      return col1.isEmpty () && col2.isEmpty ();
   }

   @Override
   public Iterator<E> iterator () {
      return new Itr<E> (col1, col2);
   }

   @Override
   public int size () {
      return col1.size () + col2.size ();
   }

   private static class Itr<E> implements Iterator<E> {
      private Iterator<? extends E> itr1;
      private Iterator<? extends E> itr2;
      private boolean c1IsCurrent = true;

      public Itr (Collection<? extends E> c1, Collection<? extends E> c2) {
         itr1 = c1.iterator ();
         itr2 = c2.iterator ();
      }

      public boolean hasNext () {
         if (c1IsCurrent) {
            if (itr1.hasNext ()) {
               // If itr2 is fail-fast, this iterator must fail fast too
               itr2.hasNext ();
               return true;
            }
            c1IsCurrent = false;
         }
         return itr2.hasNext ();
      }

      public E next () {
         if (!hasNext ())
            throw new NoSuchElementException ();
         if (c1IsCurrent)
            return itr1.next ();
         else
            return itr2.next ();
      }

      public void remove () {
         throw new UnsupportedOperationException ();
      }
   }
}
