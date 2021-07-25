package umontreal.iro.lecuyer.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a set providing a view of two sets. This extends the merged
 * collection for implementing the {@link Set} interface.
 *
 * @param <E>
 *           the type of the elements in the merged set.
 */
public class MergedSet<E> extends MergedCollection<E> implements Set<E> {
   /**
    * Constructs a set merging sets \texttt{set1} and \texttt{set2}.
    *
    * @param set1
    *           the first set.
    * @param set2
    *           the second set.
    * @exception NullPointerException
    *               if \texttt{set1} or \texttt{set2} are \texttt{null}.
    */
   public MergedSet (Set<? extends E> set1, Set<? extends E> set2) {
      super (set1, set2);
   }

   /**
    * Returns a reference to the first set of this merged set.
    *
    * @return the first set.
    */
   @Override
   public Set<? extends E> getFirstCollection () {
      return (Set<? extends E>) super.getFirstCollection ();
   }

   /**
    * Returns a reference to the second set of this merged set.
    *
    * @return the second set.
    */
   @Override
   public Set<? extends E> getSecondCollection () {
      return (Set<? extends E>) super.getSecondCollection ();
   }

   @Override
   public boolean equals (Object o) {
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
      int hashCode = 0;
      for (final Object e : this)
         hashCode += e == null ? 0 : e.hashCode ();
      return hashCode;
   }

   /**
    * Constructs and returns an iterator for the merged set. The returned
    * iterator enumerates all the elements in the first set, then it enumerates
    * the elements of the second set not present in the first set.
    */
   @Override
   public Iterator<E> iterator () {
      return new Itr<E> (getFirstCollection (), getSecondCollection ());
   }

   @Override
   public int size () {
      final Set<? extends E> set1 = getFirstCollection ();
      final Set<? extends E> set2 = getSecondCollection ();
      final int size1 = set1.size ();
      final int size2 = set2.size ();
      int size = size1 + size2;
      if (size1 < size2)
         for (final E e : set1)
            if (set2.contains (e))
               --size;
      else
         for (final E e2 : set2)
            if (set1.contains (e2))
               --size;
      return size;
   }

   private static class Itr<E> implements Iterator<E> {
      private Collection<? extends E> c1;
      private Iterator<? extends E> itr1;
      private Iterator<? extends E> itr2;
      private boolean nextReturned = false;
      private E nextElement;
      private boolean c1IsCurrent = true;

      public Itr (Collection<? extends E> c1, Collection<? extends E> c2) {
         this.c1 = c1;
         itr1 = c1.iterator ();
         itr2 = c2.iterator ();
      }

      public boolean hasNext () {
         if (nextReturned)
            return true;
         final boolean next1 = itr1.hasNext ();
         if (c1IsCurrent) {
            if (next1) {
               // If itr2 is fail-fast, this iterator must fail fast too
               itr2.hasNext ();
               return true;
            }
            c1IsCurrent = false;
         }
         while (itr2.hasNext ()) {
            final E e = itr2.next ();
            if (!c1.contains (e)) {
               nextElement = e;
               nextReturned = true;
               return true;
            }
         }
         return false;
      }

      public E next () {
         if (!hasNext ())
            throw new NoSuchElementException ();
         if (c1IsCurrent)
            return itr1.next ();
         else {
            assert nextReturned;
            final E e = nextElement;
            nextElement = null;
            nextReturned = false;
            return e;
         }
      }

      public void remove () {
         throw new UnsupportedOperationException ();
      }
   }
}
