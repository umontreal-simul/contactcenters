package umontreal.iro.lecuyer.collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * Represents a list providing a view of two lists side by side. This extends
 * the merged collection for implementing the {@link List} interface.
 *
 * @param <E>
 *           the type of the element in the merged list.
 */
public class MergedList<E> extends MergedCollection<E> implements List<E> {
   /**
    * Constructs a new merged list from lists \texttt{list1} and \texttt{list2}.
    *
    * @param list1
    *           the first list.
    * @param list2
    *           the second list.
    * @exception NullPointerException
    *               if \texttt{list1} or \texttt{list2} are \texttt{null}.
    */
   public MergedList (List<? extends E> list1, List<? extends E> list2) {
      super (list1, list2);
   }

   /**
    * Returns the reference to the first list in this merged list.
    *
    * @return the first list.
    */
   @Override
   public List<? extends E> getFirstCollection () {
      return (List<? extends E>) super.getFirstCollection ();
   }

   /**
    * Returns the reference to the second list in this merged list.
    *
    * @return the second list.
    */
   @Override
   public List<? extends E> getSecondCollection () {
      return (List<? extends E>) super.getSecondCollection ();
   }

   /**
    * Attempts to make this merged list random-accessible, i.e., supporting fast
    * random access. If both inner lists implement the {@link RandomAccess}
    * interface, this method returns a merged list implementing
    * {@link RandomAccess}. Otherwise, the method returns this reference.
    *
    * @return the merged list, possibly random-accessible.
    */
   public MergedList<E> tryToMakeRandomAccess () {
      if (getFirstCollection () instanceof RandomAccess
            && getSecondCollection () instanceof RandomAccess)
         return new RandomAccessMergedList<E> (getFirstCollection (),
               getSecondCollection ());
      else
         return this;
   }

   /**
    * Attempts to construct a random-accessible merged list. If \texttt{list1}
    * and \texttt{list2} both implement {@link RandomAccess}, this constructs
    * and returns a list implementing {@link RandomAccess}. Otherwise, the
    * constructed list does not implement the interface.
    *
    * @param <E>
    *           the type of elements in the merged list.
    * @param list1
    *           the first list.
    * @param list2
    *           the second list.
    * @return the constructed list.
    * @exception NullPointerException
    *               if \texttt{list1} or \texttt{list2} are \texttt{null}.
    */
   public static <E> MergedList<E> newRandomAccess (List<? extends E> list1,
         List<? extends E> list2) {
      if (list1 instanceof RandomAccess && list2 instanceof RandomAccess)
         return new RandomAccessMergedList<E> (list1, list2);
      else
         return new MergedList<E> (list1, list2);
   }

   @Override
   public boolean equals (Object o) {
      if (!(o instanceof List))
         return false;
      final List<?> l = (List<?>) o;
      if (size () != l.size ())
         return false;
      final Iterator<?> itr1 = iterator ();
      final Iterator<?> itr2 = l.iterator ();
      while (itr1.hasNext ()) {
         final Object e1 = itr1.next ();
         final Object e2 = itr2.next ();
         boolean eq = e1 == null ? e2 == null : e1.equals (e2);
         if (!eq)
            return false;
      }
      return true;
   }

   @Override
   public int hashCode () {
      int hashCode = 1;
      for (final Object e : this)
         hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode ());
      return hashCode;
   }

   public E get (int index) {
      if (index < 0)
         throw new IndexOutOfBoundsException ("Index is negative");
      final List<? extends E> list1 = getFirstCollection ();
      final int size1 = list1.size ();
      if (index < size1)
         return list1.get (index);
      final List<? extends E> list2 = getSecondCollection ();
      final int size2 = list2.size ();
      final int correctedIndex = index - size1;
      if (correctedIndex >= size2)
         throw new IndexOutOfBoundsException ("Index >= " + (size1 + size2));
      return list2.get (correctedIndex);
   }

   public int indexOf (Object o) {
      final List<? extends E> list1 = getFirstCollection ();
      final int ind1 = list1.indexOf (o);
      if (ind1 >= 0)
         return ind1;
      final List<? extends E> list2 = getSecondCollection ();
      final int ind2 = list2.indexOf (o);
      if (ind2 >= 0)
         return ind2 + list1.size ();
      return -1;
   }

   public int lastIndexOf (Object o) {
      final List<? extends E> list2 = getSecondCollection ();
      final int ind2 = list2.lastIndexOf (o);
      final List<? extends E> list1 = getFirstCollection ();
      if (ind2 >= 0)
         return ind2 + list1.size ();
      final int ind1 = list1.lastIndexOf (o);
      if (ind1 >= 0)
         return ind1;
      return -1;
   }

   public ListIterator<E> listIterator () {
      return new ListItr<E> (getFirstCollection (), getSecondCollection (), 0,
            0, size ());
   }

   public ListIterator<E> listIterator (int index) {
      return new ListItr<E> (getFirstCollection (), getSecondCollection (),
            index, 0, size ());
   }

   public void add (int index, E element) {
      throw new UnsupportedOperationException ();
   }

   public boolean addAll (int index, Collection<? extends E> c) {
      throw new UnsupportedOperationException ();
   }

   public E remove (int index) {
      throw new UnsupportedOperationException ();
   }

   public E set (int index, E element) {
      throw new UnsupportedOperationException ();
   }

   public List<E> subList (int fromIndex, int toIndex) {
      return new SubList<E> (this, fromIndex, toIndex);
   }

   private static class ListItr<E> implements ListIterator<E> {
      private boolean list1IsCurrent;
      private final ListIterator<? extends E> itr1;
      private final ListIterator<? extends E> itr2;
      private final List<? extends E> list1;
      private final int fromIndex;
      private final int toIndex;

      public ListItr (List<? extends E> list1, List<? extends E> list2,
            int index, int fromIndex, int toIndex) {
         if (fromIndex < 0 || toIndex > list1.size () + list2.size ())
            throw new IndexOutOfBoundsException (
                  "fromIndex < 0 or toIndex greater than "
                        + (list1.size () + list2.size ()));
         if (fromIndex > toIndex)
            throw new IllegalArgumentException ("fromIndex > toIndex");
         this.list1 = list1;
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
         if (index == 0) {
            itr1 = list1.listIterator ();
            itr2 = list2.listIterator ();
            list1IsCurrent = true;
         }
         else if (index < list1.size ()) {
            itr1 = list1.listIterator (index);
            itr2 = list2.listIterator ();
            list1IsCurrent = true;
         }
         else {
            itr1 = list1.listIterator (list1.size ());
            itr2 = list2.listIterator (index - list1.size ());
            list1IsCurrent = false;
         }
      }

      public void add (E o) {
         throw new UnsupportedOperationException (
               "Cannot add objects using this iterator");
      }

      public boolean hasNext () {
         if (list1IsCurrent) {
            if (itr1.hasNext ()) {
               // If itr2 is fail-fast, this iterator must fail fast too
               itr2.hasNext ();
               return itr1.nextIndex () < toIndex;
            }
            list1IsCurrent = false;
         }
         return itr2.hasNext () && itr2.nextIndex () + list1.size () < toIndex;
      }

      public boolean hasPrevious () {
         if (!list1IsCurrent) {
            if (itr2.hasPrevious ()) {
               itr1.hasPrevious ();
               return itr2.previousIndex () + list1.size () >= fromIndex;
            }
            list1IsCurrent = true;
         }
         return itr1.hasPrevious () && itr1.previousIndex () >= fromIndex;
      }

      public E next () {
         if (!hasNext ())
            throw new NoSuchElementException ();
         if (list1IsCurrent)
            return itr1.next ();
         else
            return itr2.next ();
      }

      public int nextIndex () {
         if (!hasNext ())
            return toIndex - fromIndex;
         if (list1IsCurrent)
            return itr1.nextIndex () - fromIndex;
         else
            return itr2.nextIndex () + list1.size () - fromIndex;
      }

      public E previous () {
         if (!hasPrevious ())
            throw new NoSuchElementException ();
         if (list1IsCurrent)
            return itr1.previous ();
         else
            return itr2.previous ();
      }

      public int previousIndex () {
         if (!hasPrevious ())
            return -1;
         if (list1IsCurrent)
            return itr1.previousIndex () - fromIndex;
         else
            return itr2.previousIndex () + list1.size () - fromIndex;
      }

      public void remove () {
         throw new UnsupportedOperationException (
               "Cannot remove contacts using this iterator");
      }

      public void set (E o) {
         throw new UnsupportedOperationException (
               "Cannot change contact types using this iterator");
      }
   }

   private static class SubList<E> extends AbstractList<E> {
      private final MergedList<E> list;
      private final int fromIndex;
      private final int toIndex;
      private final int size;
      private final int size1;
      private final int size2;

      public SubList (MergedList<E> list, int fromIndex, int toIndex) {
         if (fromIndex < 0 || toIndex > list.size ())
            throw new IndexOutOfBoundsException (
                  "fromIndex < 0 or toIndex greater than " + list.size ());
         if (fromIndex > toIndex)
            throw new IllegalArgumentException ("fromIndex > toIndex");
         this.list = list;
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
         this.size = toIndex - fromIndex;
         size1 = list.getFirstCollection ().size ();
         size2 = list.getSecondCollection ().size ();
      }

      private void checkRange (int index) {
         if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException ();
      }

      private void checkSizeChange () {
         if (list.getFirstCollection ().size () != size1
               || list.getSecondCollection ().size () != size2)
            throw new ConcurrentModificationException ();
      }

      @Override
      public E get (int index) {
         checkSizeChange ();
         checkRange (index);
         return list.get (index + fromIndex);
      }

      @Override
      public Iterator<E> iterator () {
         return listIterator ();
      }

      @Override
      public ListIterator<E> listIterator () {
         checkSizeChange ();
         return new ListItr<E> (list.getFirstCollection (), list
               .getSecondCollection (), fromIndex, fromIndex, toIndex);
      }

      @Override
      public ListIterator<E> listIterator (int index) {
         checkSizeChange ();
         if (index < 0 || index > size)
            throw new IndexOutOfBoundsException ();
         return new ListItr<E> (list.getFirstCollection (), list
               .getSecondCollection (), index + fromIndex, fromIndex, toIndex);
      }

      @Override
      public boolean isEmpty () {
         checkSizeChange ();
         return size == 0;
      }

      @Override
      public int size () {
         checkSizeChange ();
         return size;
      }
   }

   private static class RandomAccessMergedList<E> extends MergedList<E>
         implements RandomAccess {
      public RandomAccessMergedList (List<? extends E> list1,
            List<? extends E> list2) {
         super (list1, list2);
      }
   }
}
