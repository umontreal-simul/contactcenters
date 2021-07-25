package umontreal.iro.lecuyer.contactcenters.ctmc;
import java.util.NoSuchElementException;

/**
 * Represents a resizable circular array of integers.
 * Any object of this class encapsulates an ordinary
 * array of integers as well as an index of the starting
 * position.
 * Elements can be added to the array which
 * grows as necessary.
 * The first and last element of the array
 * can be removed in constant time while
 * removing any other element requires a linear time
 * shift of the other elements in the array.
 */
public class CircularIntArray implements Cloneable {
   private int[] array;
   private int size;
   private int startPosition;
   private int endPosition;
   
   /**
    * Constructs a new circular array of integers
    * with a default initial capacity of 5 elements.
    */
   public CircularIntArray () {
      this (5);
   }
   
   /**
    * Constructs a new array of integers with
    * the given initial capacity.
    * @param initialCapacity the initial capacity of the
    * array.
    */
   public CircularIntArray (int initialCapacity) {
      array = new int[initialCapacity];
   }
   
   /**
    * Clears this circular array.
    * After a call to this method, the size of the
    * array returned by {@link #size()} is 0.
    */
   public void clear() {
      size = 0;
      startPosition = 0;
      endPosition = 0;
   }
   
   /**
    * Initializes this circular array of integers
    * with the contents of another circular array
    * \texttt{a}.
    * This method replaces the internal array
    * of this object with a clone of the
    * internal array of \texttt{a}, and
    * also copies the size and starting position of
    * the given array. 
    * @param a the other circular array to copy.
    */
   public void init (CircularIntArray a) {
      size = a.size;
      startPosition = a.startPosition;
      endPosition = a.endPosition;
      array = a.array.clone();
   }
   
   /**
    * Adds the element \texttt{x} at the end
    * of this circular array.
    * If adding the element would exceed the capacity
    * of the internal array, the internal array gorows.
    * @param x the new element to add.
    */
   public void add (int x) {
      if (size == array.length) {
         // Resize the array
         int[] newArray = new int[2*array.length + 1];
         // We also manage to reset startingPosition to 0
         System.arraycopy (array, startPosition, newArray, 0, array.length - startPosition);
         if (startPosition > 0) {
            System.arraycopy (array, 0, newArray, array.length - startPosition, startPosition);
            startPosition = 0;
         }
         array = newArray;
         endPosition = size;
      }
      array[endPosition++] = x;
      if (endPosition == array.length)
         endPosition = 0;
      ++size;
   }
   
   /**
    * Removes and returns the first element of this circular array.
    * If the size of the array returned by the {@link #size()}
    * method is 0, this method throws a
    * {@link NoSuchElementException}. 
    * @return the value of the (removed) first element.
    */
   public int removeFirst () {
      if (size == 0)
         throw new NoSuchElementException();
      int e = array[startPosition];
      ++startPosition;
      if (startPosition >= array.length)
         startPosition = 0;
      --size;
      return e;
   }
   
   /**
    * Removes and returns the last element of this circular array.
    * If the size of the array returned by the {@link #size()}
    * method is 0, this method throws a
    * {@link NoSuchElementException}. 
    * @return the value of the (removed) last element.
    */
   public int removeLast() {
      if (size == 0)
         throw new NoSuchElementException();
      --endPosition;
      if (endPosition < 0)
         endPosition = array.length - 1;
      --size;
      final int e = array[endPosition];
      return e;
   }

   /*
   public void removeFirstNegativeElements () {
      while (size > 0 && array[startPosition] < 0)
         removeFirst();
   }
   */
   
   /**
    * Returns the size of this circular array.
    * @return the size of the circular array.
    */
   public int size() {
      return size;
   }
   
/*
   public int selectRandomIndex (RandomStream stream) {
      if (size == 0)
         throw new NoSuchElementException();
      int idx;
      do {
         idx = stream.nextInt (0, size - 1);
      }
      while (get (idx) < 0);
      return idx;
   }
   */
   
   /**
    * Returns the value of element \texttt{i} in this circular array.
    * @param i the index of the queried element.
    * @return the value of the element.
    */
   public int get (int i) {
      if (i < 0 || i >= size)
         throw new ArrayIndexOutOfBoundsException (i);
      final int idx = (i + startPosition) % array.length;
      return array[idx];
   }
   
   /**
    * Sets the value of element \texttt{i} to \texttt{e}.
    * @param i the index of the element to modify.
    * @param e the new value of the element.
    */
   public void set (int i, int e) {
      if (i < 0 || i >= size)
         throw new ArrayIndexOutOfBoundsException (i);
      final int idx = (i + startPosition) % array.length;
      array[idx] = e;
   }
   
   /**
    * Removes and returns the element with index \texttt{i} in the array.
    * If the given index is 0, this calls
    * {@link #removeFirst()}.
    * If the given index is {@link #size()} minus 1, this returns the
    * result of {@link #removeLast()}.
    * Otherwise, elements of the array are shifted appropriately to
    * remove the element.
    * @param i the index of the element to remove.
    * @return the removed element.
    */
   public int remove (int i) {
      if (i == 0)
         return removeFirst ();
      if (i == size - 1)
         return removeLast ();
      if (i < 0 || i >= size)
         throw new ArrayIndexOutOfBoundsException (i);
      final int idx = (i + startPosition) % array.length;
      final int e = array[idx];
      --endPosition;
      if (endPosition < 0)
         endPosition = array.length - 1;

      final int numToShift = size - i - 1;
      if (numToShift > 0) {
         final int maxFirstShift = array.length - idx - 1;
         System.arraycopy (array, idx + 1, array, idx, Math.min (numToShift, maxFirstShift));
         if (numToShift > maxFirstShift) {
            array[array.length - 1] = array[0];
            final int numSecondShift = numToShift - maxFirstShift - 1;
            System.arraycopy (array, 1, array, 0, numSecondShift);
         }
      }

      --size;
      return e;
   }
   
   /**
    * Returns a copy of this circular array of integers.
    * The returned copy is completely independent from this
    * instance since the internal array is also cloned. 
    */
   @Override
   public CircularIntArray clone() {
      CircularIntArray cpy;
      try {
         cpy = (CircularIntArray)super.clone();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError ("Clone not supported for a class implementing Cloneable");
      }
      cpy.array = array.clone();
      return cpy;
   }
   
   /**
    * Constructs and returns a string representation of the form
    * \texttt{[e1, e2, ...]} of this array.
    */
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append ('[');
      for (int i = 0; i < size; i++)
         sb.append (i > 0 ? ", " : "").append (get (i));
      sb.append (']');
      return sb.toString();
   }
   
   public static void main (String[] args) {
      CircularIntArray ar = new CircularIntArray();
      ar.add (1);
      ar.add (2);
      ar.add (4);
      ar.add (5);
      ar.add (9);
      System.out.printf ("Initial array: %s%n", ar);
      System.out.printf ("Third element: %d%n", ar.get (2));
      System.out.printf ("Removed third element: %d%n", ar.remove (2));
      System.out.printf ("Array after removal: %s%n", ar);
      ar.add (25);
      System.out.printf ("Array after adding 25: %s%n", ar);
      System.out.printf ("Removed first element: %d%n", ar.removeFirst ());
      System.out.printf ("Array after removal: %s%n", ar);
      ar.add (35);
      System.out.printf ("Array after adding 35: %s%n", ar);
      System.out.printf ("Value of second element: %d%n", ar.get (1));
      System.out.printf ("Value of removed second element: %d%n", ar.remove (1));
      System.out.printf ("Value after removal: %s%n", ar);
   }
}
