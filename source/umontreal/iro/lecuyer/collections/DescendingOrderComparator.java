/**
 *
 */
package umontreal.iro.lecuyer.collections;

import java.util.Comparator;

/**
 * Represents a comparator sorting objects in descending
 * natural order.
 * More specifically,
 * when comparing two objects, this comparator
 * returns the result of {@link Comparable#compareTo(Object)}
 * multiplied by $-1$.
 *
 * @param <T>
 */
public class DescendingOrderComparator<T extends Comparable<? super T>> implements Comparator<T> {
   public int compare (T o1, T o2) {
      return -o1.compareTo (o2);
   }
}
