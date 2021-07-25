package umontreal.iro.lecuyer.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a map merging two maps. A merged map is constructed from two maps
 * \texttt{map1} and \texttt{map2}, contains the keys of both maps, and is
 * immutable. However, any change to the inner maps is reflected on the merged
 * map. Note that if both maps contain the same key, the key appears only once
 * in the merged map, and the corresponding value comes from the entry in the
 * first map. The iterators returned by this implementation enumerates the
 * elements of the first map, then the elements of the second map.
 * 
 * @param <K>
 *           the type of the keys in the merged map.
 * @param <V>
 *           the type of the values in the merged map.
 */
public class MergedMap<K, V> extends AbstractMap<K, V> {
   private Map<? extends K, ? extends V> map1;
   private Map<? extends K, ? extends V> map2;
   private EntrySet entrySet;

   /**
    * Constructs a new merged map from maps \texttt{map1} and \texttt{map2}.
    * 
    * @param map1
    *           the first map.
    * @param map2
    *           the second map.
    * @exception NullPointerException
    *               if \texttt{map1} or \texttt{map2} are \texttt{null}.
    */
   public MergedMap (Map<? extends K, ? extends V> map1,
         Map<? extends K, ? extends V> map2) {
      if (map1 == null || map2 == null)
         throw new NullPointerException ();
      this.map1 = map1;
      this.map2 = map2;
   }

   /**
    * Returns a reference to the first map in this merged map.
    * 
    * @return the first map.
    */
   public Map<? extends K, ? extends V> getFirstMap () {
      return map1;
   }

   /**
    * Returns a reference to the second map in this merged map.
    * 
    * @return the second map.
    */
   public Map<? extends K, ? extends V> getSecondMap () {
      return map2;
   }

   @Override
   public Set<Map.Entry<K, V>> entrySet () {
      if (entrySet == null)
         entrySet = new EntrySet (map1, map2);
      return entrySet;
   }

   @Override
   public boolean containsKey (Object key) {
      return map1.containsKey (key) || map2.containsKey (key);
   }

   @Override
   public boolean containsValue (Object value) {
      if (map1.containsValue (value))
         return true;
      // The value being searched for may be in an excluded entry.
      for (final Map.Entry<? extends K, ? extends V> e : map2.entrySet ())
         if (value == null ? e.getValue () == null : value.equals (e
               .getValue ()))
            if (!map1.containsKey (e.getKey ()))
               return true;
      return false;
   }

   @Override
   public V get (Object key) {
      if (map1.containsKey (key))
         return map1.get (key);
      else
         return map2.get (key);
   }

   @Override
   public boolean isEmpty () {
      return map1.isEmpty () && map2.isEmpty ();
   }

   @Override
   public int size () {
      final int size1 = map1.size ();
      final int size2 = map2.size ();
      int size = size1 + size2;
      if (size1 < size2)
         for (final K key : map1.keySet ())
            if (map2.containsKey (key))
               --size;
      else
         for (final K key2 : map2.keySet ())
            if (map1.containsKey (key2))
               --size;
      return size;
   }

   private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
      private final Set<Map.Entry<K, V>> set1;
      private final Set<Map.Entry<K, V>> set2;

      @SuppressWarnings ("unchecked")
      public EntrySet (Map<? extends K, ? extends V> map1,
            Map<? extends K, ? extends V> map2) {
         // This is unsafe, but it works if the map is not modified.
         // Since the merged map is immutable, this is not a problem.
         set1 = ((Map<K, V>) map1).entrySet ();
         set2 = ((Map<K, V>) map2).entrySet ();
      }

      @Override
      public boolean contains (Object o) {
         if (set1.contains (o))
            return true;
         if (set2.contains (o)) {
            // If the second set contains entry (k, v2)
            // while the first set contains entry
            // (k, v1), (k, v2) must be ignored.
            // Otherwise, we have a multimap.
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (map1.containsKey (e.getKey ()))
               return false;
            return true;
         }
         return false;
      }

      @Override
      public boolean isEmpty () {
         return set1.isEmpty () && set2.isEmpty ();
      }

      @Override
      public Iterator<Map.Entry<K, V>> iterator () {
         return new Itr (set1, set2);
      }

      @Override
      public int size () {
         return MergedMap.this.size ();
      }
   }

   private class Itr implements Iterator<Map.Entry<K, V>> {
      private Iterator<Map.Entry<K, V>> itr1;
      private Iterator<Map.Entry<K, V>> itr2;
      private boolean nextReturned = false;
      private Map.Entry<K, V> nextElement;
      private boolean s1IsCurrent = true;

      public Itr (Set<Map.Entry<K, V>> set1, Set<Map.Entry<K, V>> set2) {
         itr1 = set1.iterator ();
         itr2 = set2.iterator ();
      }

      public boolean hasNext () {
         if (nextReturned)
            return true;
         // If itr2 is fail-fast, this iterator must fail fast too
         final boolean next1 = itr1.hasNext ();
         if (s1IsCurrent) {
            if (next1) {
               itr2.hasNext ();
               return true;
            }
            s1IsCurrent = false;
         }
         while (itr2.hasNext ()) {
            final Map.Entry<K, V> e = itr2.next ();
            if (!map1.containsKey (e.getKey ())) {
               nextElement = e;
               nextReturned = true;
               return true;
            }
         }
         return false;
      }

      public Map.Entry<K, V> next () {
         if (!hasNext ())
            throw new NoSuchElementException ();
         if (s1IsCurrent)
            return itr1.next ();
         else {
            assert nextReturned;
            final Map.Entry<K, V> e = nextElement;
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
