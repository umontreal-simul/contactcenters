package umontreal.iro.lecuyer.xmlbind;

import java.util.HashMap;
import java.util.Map;

import umontreal.iro.lecuyer.xmlbind.params.Named;

/**
 * Represents the information about an entity
 * with a name and possibly properties.
 * This object is constructed from a {@link Named}
 * instance which is obtained by unmarshalling some
 * XML elements using JAXB.
 * It allows the user to access the properties using
 * a Java map rather than a list with an object
 * for each property.
 * This class is often extended to represent
 * specific entities, for example the call types of
 * a call center.
 */
public class NamedInfo {
   private String name;
   private Map<String, Object> properties;
   
   /**
    * Constructs a new named entity from the parameter
    * object \texttt{named}.
    * @param named the parameter object representing the named entity.
    */
   public NamedInfo (Named named) {
      name = named.getName ();
      properties = ParamReadHelper.unmarshalProperties (named.getProperties ());
   }
   
   /**
    * Constructs a named entity with name \texttt{name}, and
    * no property.
    * @param name the name of the new entity.
    */
   public NamedInfo (String name) {
      this.name = name;
      properties = new HashMap<String, Object>();
   }
   
   /**
    * Constructs a new named entity with name
    * \texttt{name}, and properties stored in the
    * map \texttt{properties}.
    * Each key of the given map represents the name of a property
    * while the corresponding value in the map is the value of the property.
    * @param name the name of the entity.
    * @param properties the properties of the entity.
    */
   public NamedInfo (String name, Map<String, ? extends Object> properties) {
      this.name = name;
      this.properties = new HashMap<String, Object>();
      this.properties.putAll (properties);
   }
   
   /**
    * Returns the name associated with this named
    * entity.
    * This returns \texttt{null} if no name
    * is associated. 
    * @return the associated name.
    */
   public String getName() {
      return name;
   }
   
   /**
    * Returns the properties associated with
    * the entity represented by this object.
    * Each key of the returned map represents the name of a property
    * while the corresponding value in the map is the value of the property.
    * @return the associated properties.
    */
   public Map<String, Object> getProperties() {
      return properties;
   }
   
   /**
    * Returns a map constructed by converting each value of properties
    * in map returned by {@link #getProperties()}
    * to a string.
    * If a property has the \texttt{null} value, it is converted to
    * the ``null'' string.
    * @return the properties, with their values converted to strings.
    */
   public Map<String, String> getStringProperties() {
      final Map<String, String> res = new HashMap<String, String>();
      for (final Map.Entry<String, Object> e : properties.entrySet ()) {
         final String key = e.getKey ();
         final Object val = e.getValue ();
         res.put (key, val == null ? "null" : val.toString ());
      }
      return res;
   }
}
