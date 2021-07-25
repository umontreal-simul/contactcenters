package umontreal.iro.lecuyer.collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Represents a map that dynamically transforms the elements of another map.
 * This abstract class defines a map containing an inner map of elements with
 * certain key and value types, and provides facilities to convert keys and
 * values to outer other types. A concrete subclass simply needs to provide
 * methods for converting keys and values between the inner and the outer types.
 * The mapping established for the keys by these methods must be one-to-one.
 * Otherwise, the size of the outer map might be incorrect, and the iterators
 * may give the same entry multiple times. Also, a \texttt{null} key or value
 * should always correspond to a \texttt{null} element.
 * 
 * @param <OK>
 *           the type of the outer keys
 * @param <OV>
 *           the type of the outer values
 * @param <IK>
 *           the type of the inner keys
 * @param <IV>
 *           the type of the inner values
 */
public abstract class TransformingMap<OK, OV, IK, IV> extends
      AbstractMap<OK, OV> {
   private Map<IK, IV> innerMap;
   private EntrySet entrySet;
   private KeySet keySet;
   private Values values;

   /**
    * Constructs a new transforming map converting keys and values of map
    * \texttt{innerMap}.
    * 
    * @param innerMap
    *           the inner map.
    * @exception NullPointerException
    *               if \texttt{innerMap} is \texttt{null}.
    */
   public TransformingMap (Map<IK, IV> innerMap) {
      if (innerMap == null)
         throw new NullPointerException ();
      this.innerMap = innerMap;
   }

   /**
    * Returns the inner map associated with this map.
    * 
    * @return the inner map.
    */
   public Map<IK, IV> getInnerMap () {
      return innerMap;
   }

   /**
    * Converts the key for the inner map to a key for the outer map.
    * 
    * @param key
    *           the inner key.
    * @return the outer key.
    */
   public abstract OK convertKeyFromInnerType (IK key);

   /**
    * Converts the key for the outer map to a key for the inner map.
    * 
    * @param key
    *           the outer key.
    * @return the inner key.
    */
   public abstract IK convertKeyToInnerType (OK key);

   /**
    * Converts the value for the inner map to a value for the outer map.
    * 
    * @param value
    *           the inner value.
    * @return the outer value.
    */
   public abstract OV convertValueFromInnerType (IV value);

   /**
    * Converts the value for the outer map to a value for the inner map.
    * 
    * @param value
    *           the outer value.
    * @return the inner value.
    */
   public abstract IV convertValueToInnerType (OV value);

   @Override
   public void clear () {
      innerMap.clear ();
   }

   @SuppressWarnings ("unchecked")
   @Override
   public boolean containsKey (Object key) {
      final OK okey;
      try {
         okey = (OK) key;
      }
      catch (final ClassCastException c) {
         return false;
      }
      return innerMap.containsKey (convertKeyToInnerType (okey));
   }

   @SuppressWarnings ("unchecked")
   @Override
   public boolean containsValue (Object value) {
      final OV ovalue;
      try {
         ovalue = (OV) value;
      }
      catch (final ClassCastException c) {
         return false;
      }
      return innerMap.containsValue (convertValueToInnerType (ovalue));
   }

   @Override
   public Set<Map.Entry<OK, OV>> entrySet () {
      if (entrySet == null)
         entrySet = new EntrySet (innerMap.entrySet ());
      return entrySet;
   }

   @Override
   public Set<OK> keySet () {
      if (keySet == null)
         keySet = new KeySet (innerMap.keySet ());
      return keySet;
   }

   @Override
   public Collection<OV> values () {
      if (values == null)
         values = new Values (innerMap.values ());
      return values;
   }

   @SuppressWarnings ("unchecked")
   @Override
   public OV get (Object key) {
      final OK okey;
      try {
         okey = (OK) key;
      }
      catch (final ClassCastException c) {
         return null;
      }
      return convertValueFromInnerType (innerMap
            .get (convertKeyToInnerType (okey)));
   }

   @Override
   public boolean isEmpty () {
      return innerMap.isEmpty ();
   }

   @Override
   public OV put (OK key, OV value) {
      return convertValueFromInnerType (innerMap.put (
            convertKeyToInnerType (key), convertValueToInnerType (value)));
   }

   @SuppressWarnings ("unchecked")
   @Override
   public OV remove (Object key) {
      final OK okey;
      try {
         okey = (OK) key;
      }
      catch (final ClassCastException c) {
         return null;
      }
      return convertValueFromInnerType (innerMap
            .remove (convertKeyToInnerType (okey)));
   }

   @Override
   public int size () {
      return innerMap.size ();
   }

   private class EntrySet extends
         TransformingSet<Map.Entry<OK, OV>, Map.Entry<IK, IV>> {
      public EntrySet (Set<Map.Entry<IK, IV>> innerSet) {
         super (innerSet);
      }

      @Override
      public Map.Entry<OK, OV> convertFromInnerType (Map.Entry<IK, IV> e) {
         return new OuterEntry (e);
      }

      @Override
      public Map.Entry<IK, IV> convertToInnerType (Map.Entry<OK, OV> e) {
         return new InnerEntry (e);
      }
   }

   private class InnerEntry implements Map.Entry<IK, IV> {
      private Map.Entry<OK, OV> outerEntry;

      public InnerEntry (Map.Entry<OK, OV> outerEntry) {
         this.outerEntry = outerEntry;
      }

      public Map.Entry<OK, OV> getOuterEntry () {
         return outerEntry;
      }

      @Override
      public boolean equals (Object obj) {
         if (!(obj instanceof Map.Entry))
            return false;
         final Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
         final IK key = getKey ();
         final IV value = getValue ();
         final Object okey = e.getKey ();
         final Object ovalue = e.getValue ();
         return (key == null ? okey == null : key.equals (okey))
               && (value == null ? ovalue == null : value.equals (ovalue));
      }

      @Override
      public int hashCode () {
         final IK key = getKey ();
         final IV value = getValue ();
         return (key == null ? 0 : key.hashCode ())
               ^ (value == null ? 0 : value.hashCode ());
      }

      public IK getKey () {
         return convertKeyToInnerType (outerEntry.getKey ());
      }

      public IV getValue () {
         return convertValueToInnerType (outerEntry.getValue ());
      }

      public IV setValue (IV value) {
         return convertValueToInnerType (outerEntry
               .setValue (convertValueFromInnerType (value)));
      }

      @Override
      public String toString () {
         final IK key = getKey ();
         final IV value = getValue ();
         return (key == null ? "null" : key.toString ()) + "="
               + (value == null ? "null" : value.toString ());
      }
   }

   private class OuterEntry implements Map.Entry<OK, OV> {
      private Map.Entry<IK, IV> innerEntry;

      public OuterEntry (Map.Entry<IK, IV> innerEntry) {
         this.innerEntry = innerEntry;
      }

      @SuppressWarnings("unused")
		public Map.Entry<IK, IV> getInnerEntry () {
         return innerEntry;
      }

      @Override
      public boolean equals (Object obj) {
         if (!(obj instanceof Map.Entry))
            return false;
         final Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
         final OK key = getKey ();
         final OV value = getValue ();
         final Object okey = e.getKey ();
         final Object ovalue = e.getValue ();
         return (key == null ? okey == null : key.equals (okey))
               && (value == null ? ovalue == null : value.equals (ovalue));
      }

      @Override
      public int hashCode () {
         final OK key = getKey ();
         final OV value = getValue ();
         return (key == null ? 0 : key.hashCode ())
               ^ (value == null ? 0 : value.hashCode ());
      }

      public OK getKey () {
         return convertKeyFromInnerType (innerEntry.getKey ());
      }

      public OV getValue () {
         return convertValueFromInnerType (innerEntry.getValue ());
      }

      public OV setValue (OV value) {
         return convertValueFromInnerType (innerEntry
               .setValue (convertValueToInnerType (value)));
      }

      @Override
      public String toString () {
         final OK key = getKey ();
         final OV value = getValue ();
         return (key == null ? "null" : key.toString ()) + "="
               + (value == null ? "null" : value.toString ());
      }
   }

   private class KeySet extends TransformingSet<OK, IK> {
      public KeySet (Set<IK> innerSet) {
         super (innerSet);
      }

      @Override
      public OK convertFromInnerType (IK e) {
         return convertKeyFromInnerType (e);
      }

      @Override
      public IK convertToInnerType (OK e) {
         return convertKeyToInnerType (e);
      }
   }

   private class Values extends TransformingCollection<OV, IV> {
      public Values (Collection<IV> innerCollection) {
         super (innerCollection);
      }

      @Override
      public OV convertFromInnerType (IV e) {
         return convertValueFromInnerType (e);
      }

      @Override
      public IV convertToInnerType (OV e) {
         return convertValueToInnerType (e);
      }
   }
}
