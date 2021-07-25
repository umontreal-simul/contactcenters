package umontreal.iro.lecuyer.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents an iterator traversing a restricted
 * subset of the elements enumerated by another iterator.
 * A filtered iterator encapsulates an
 * ordinary iterator, and uses it to
 * enumerate objects.
 * However, this iterator only returns objects passing
 * the test implemented in the user-defined {@link #filter}
 * method.
 * For example, this class could be used to
 * iterate over objects of a
 * certain subclass or having certain properties.
 * Note that this iterator does not support
 * the {@link #remove()} operation. 
 * @param <E> the type of the accepted objects.
 */
public abstract class FilteredIterator<E> implements Iterator<E> {
   private final Iterator<? super E> it;
   E next = null;
   int nextIndex = 0;
   private int maxNumElements;
   
   /**
    * Constructs a new filtered iterator
    * from the iterator \texttt{it}.
    * @param it the iterator being filtered.
    * @exception NullPointerException if \texttt{it} is \texttt{null}.
    */
   public FilteredIterator (Iterator<? super E> it) {
      this (it, Integer.MAX_VALUE);
   }
   
   /**
    * Constructs a new filtered iterator
    * from the iterator \texttt{it}, and
    * returning at most \texttt{maxNumElements} elements.
    * @param it the iterator being filtered.
    * @param maxNumElements the maximal number of elements the iterator can return.
    * @exception NullPointerException if \texttt{it} is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{maxNumElements}
    * is negative. 
    */
   public FilteredIterator (Iterator<? super E> it, int maxNumElements) {
      if (it == null)
         throw new NullPointerException();
      if (maxNumElements < 0)
         throw new IllegalArgumentException();
      this.it = it;
      this.maxNumElements = maxNumElements;
   }
   
   /**
    * Returns the inner iterator used by
    * this iterator.
    * @return the inner iterator.
    */
   public Iterator<? super E> getInnerIterator() {
      return it;
   }
   
   /**
    * Returns the maximal number of elements that can
    * be traversed by this iterator,
    * or {@link Integer#MAX_VALUE} if
    * the number of elements is not bounded.
    * This does not affect the number of elements
    * traversed by the inner iterator and passed
    * to the {@link #filter} method.
    * @return the maximal number of elements the iterator can return.
    */
   public int getMaxNumElements() {
      return maxNumElements;
   }
   
   /**
    * Determines if the object \texttt{o} is returned
    * by this iterator.
    * Returns \texttt{true} if the object
    * is accepted, \texttt{false} otherwise.
    * This iterator assumes that this method
    * returns \texttt{true} for a particular object \texttt{o}
    * only if \texttt{o} can be cast into an instance of
    * \texttt{E}.
    * @param o the tested object.
    * @return the result of the test.
    */
   public abstract boolean filter (Object o);

   @SuppressWarnings("unchecked")
   public boolean hasNext () {
      if (next != null)
         return true;
      if (nextIndex >= maxNumElements)
         return false;
      while (it.hasNext()) {
         final Object o = it.next();
         if (filter (o)) {
            next = (E)o;
            return true;
         }
      }
      return false;
   }

   public E next () {
      if (!hasNext())
         throw new NoSuchElementException();
      final E rem = next;
      next = null;
      ++nextIndex;
      return rem;
   }

   public void remove () {
      throw new UnsupportedOperationException();
   }
}
