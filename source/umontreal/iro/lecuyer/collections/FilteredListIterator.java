package umontreal.iro.lecuyer.collections;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Represents a list iterator traversing a restricted
 * subset of the elements enumerated by another list iterator.
 * A filtered list iterator encapsulates an
 * ordinary list iterator, and uses it to
 * enumerate objects.
 * However, this iterator only returns objects passing
 * the test implemented in the user-defined {@link #filter}
 * method.
 * For example, this class could be used to
 * iterate over objects of a
 * certain subclass or having certain properties.
 * Note that this iterator does not support
 * the {@link #add}, {@link #set}, and {@link #remove()} operations.
 * @param <E> the type of the accepted objects.
 */
public abstract class FilteredListIterator<E> extends FilteredIterator<E> implements ListIterator<E> {
   private E prev;

   /**
    * Constructs a new filtered iterator
    * from the iterator \texttt{it}.
    * Note that if \texttt{it} is not positionned
    * at the beginning of the list,
    * this method has to enumerate all
    * elements of \texttt{it} to
    * set the initial value of {@link #nextIndex}.
    * @param it the iterator being filtered.
    * @exception NullPointerException if \texttt{it} is \texttt{null}.
    */
   public FilteredListIterator (ListIterator<? super E> it) {
      super (it);
      nextIndex = -1;
   }

   /**
    * Constructs a new filtered iterator
    * from the iterator \texttt{it}, and
    * returning at most \texttt{maxNumElements}
    * elements.
    * Note that if \texttt{it} is not positionned
    * at the beginning of the list,
    * this method has to enumerate all
    * elements of \texttt{it} to
    * set the initial value of {@link #nextIndex}, which
    * cannot exceed \texttt{maxNumElements}.
    * @param it the iterator being filtered.
    * @param maxNumElements the maximal number of elements the iterator can return.
    * @exception NullPointerException if \texttt{it} is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{maxNumElements} is negative.
    */
   public FilteredListIterator (ListIterator<? super E> it, int maxNumElements) {
      super (it, maxNumElements);
      nextIndex = -1;
   }

   private void initNextIndex() {
      if (nextIndex != -1)
         return;
      nextIndex = 0;
      final ListIterator<? super E> it = getInnerIterator();
      final int idx;
      if ((idx = it.nextIndex ()) > 0) {
         final int maxNumElements = getMaxNumElements();
         while (nextIndex < maxNumElements && it.hasPrevious ()) {
            final Object o = it.previous ();
            if (filter (o))
               ++nextIndex;
         }
         while (it.nextIndex () < idx)
            it.next ();
      }
   }

   /**
    * Returns the inner iterator used by
    * this iterator.
    * @return the inner iterator.
    */
   @Override
   public ListIterator<? super E> getInnerIterator() {
      return (ListIterator<? super E>)super.getInnerIterator ();
   }


   public void add (E o) {
      throw new UnsupportedOperationException();
   }

   @SuppressWarnings("unchecked")
   @Override
   public boolean hasNext () {
      initNextIndex();
      final ListIterator<? super E> it = getInnerIterator();
      if (next != null)
         return true;
      if (nextIndex >= getMaxNumElements())
         return false;
      if (it.hasNext () && prev != null) {
         it.next ();
         prev = null;
      }
      while (it.hasNext()) {
         final Object o = it.next();
         if (filter (o)) {
            next = (E)o;
            return true;
         }
      }
      return false;
   }

   @SuppressWarnings("unchecked")
   public boolean hasPrevious () {
      initNextIndex();
      final ListIterator<? super E> it = getInnerIterator();
      if (prev != null)
         return true;
      if (nextIndex <= 0)
         return false;
      if (it.hasPrevious () && next != null) {
         it.previous();
         next = null;
      }
      while (it.hasPrevious()) {
         final Object o = it.previous();
         if (filter (o)) {
            prev = (E)o;
            return true;
         }
      }
      return false;
   }

   public int nextIndex () {
      initNextIndex();
      return nextIndex;
   }

   public E previous () {
      if (!hasPrevious())
         throw new NoSuchElementException();
      final E rem = prev;
      prev = next = null;
      --nextIndex;
      return rem;
   }

   @Override
   public E next () {
      if (!hasNext())
         throw new NoSuchElementException();
      final E rem = next;
      prev = next = null;
      ++nextIndex;
      return rem;
   }

   public int previousIndex () {
      initNextIndex();
      return nextIndex - 1;
   }

   public void set (E o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void remove () {
      throw new UnsupportedOperationException();
   }
}
