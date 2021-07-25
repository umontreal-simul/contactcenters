package umontreal.iro.lecuyer.util;

import java.util.Arrays;

/**
 * Represents an immutable array of integers.
 * This class is similar to the {@link Integer} class,
 * but it wraps an array of integers rather than
 * an integer.
 * Instances of this class can be used as set
 * elements or map keys, because this class
 * implements the {@link #hashCode()}
 *  and {@link #equals(Object)} methods to
 *  compare the contents of the array.
 */
public class IntArray implements Comparable<IntArray> {
   /**
    * Gives the length of the wrapped array.
    */
   public final int length;
   private final int[] array;
   private final int hash;
   
   /**
    * Constructs a new array object from the
    * given array of integers.
    * A \texttt{null} value is considered as an
    * array with length 0.
    * @param array the array to be wrapped.
    */
   public IntArray (int[] array) {
      this.array = array == null ? new int[0] : array.clone();
      length = array == null ? 0 : array.length;
      hash = Arrays.hashCode (array);
   }
   
   /**
    * Returns a copy of the wrapped array.
    * Note that modifying the returned array does
    * not affect the array wrapped by this object.
    * @return a copy of the wrapped array.
    */
   public int[] getArray() {
      return array.clone();
   }

   /**
    * Returns the element with index \texttt{i}
    * of the wrapped array.
    * @param i the queried index.
    * @return the value of the element.
    * @exception ArrayIndexOutOfBoundsException if \texttt{i}
    * is negative or greater than or equal to {@link #length}.
    */
   public int getElement (int i) {
      return array[i];
   }
   
   /**
    * Returns the result of {@link Arrays#hashCode(int[])}
    * on the wrapped array.
    */
   @Override
   public int hashCode() {
      return hash;
   }
   
   /**
    * If \texttt{o} corresponds to an instance of
    * {@link IntArray}, tests the equality of the
    * wrapped arrays using {@link Arrays#equals(int[], int[])},
    * and returns the result of the test.
    * Otherwise, returns \texttt{false}.
    */
   @Override
   public boolean equals (Object o) {
      if (o instanceof IntArray)
         return Arrays.equals (array, ((IntArray)o).array);
      return false;
   }
   
   /**
    * Returns the result of {@link Arrays#toString(int[])}
    * called on the wrapped array.
    */
   @Override
   public String toString() {
      return Arrays.toString (array);
   }

   public int compareTo (IntArray o) {
      if (array.length < o.array.length)
         return -1;
      if (array.length > o.array.length)
         return 1;
      for (int i = 0; i < array.length; i++) {
         if (array[i] < o.array[i])
            return -1;
         if (array[i] > o.array[i])
            return 1;
      }
      return 0;
   }
}
