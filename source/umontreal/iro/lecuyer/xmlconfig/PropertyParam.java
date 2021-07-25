/**
 * 
 */
package umontreal.iro.lecuyer.xmlconfig;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;

/**
 * Represents a property, i.e., a name-value pair.
 */
public class PropertyParam extends AbstractParam implements StorableParam,
      Cloneable {
   private String name;
   private String value;

   /**
    * Nullary constructor for the parameter reader.
    */
   public PropertyParam () {}

   /**
    * Constructs a new property with name \texttt{name}, and value
    * \texttt{value}.
    * 
    * @param name
    *           the name.
    * @param value
    *           the value.
    * @exception NullPointerException
    *               if \texttt{name} is \texttt{null}.
    */
   public PropertyParam (String name, String value) {
      if (name == null || value == null)
         throw new NullPointerException ();
      this.name = name;
      this.value = value;
   }

   /**
    * Returns the name of this property.
    * 
    * @return the name of the property.
    */
   public String getName () {
      return name;
   }

   /**
    * Sets the name of the property to \texttt{name}.
    * 
    * @param name
    *           the new name of the property.
    * @exception NullPointerException
    *               if \texttt{name} is \texttt{null}.
    */
   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ();
      this.name = name;
   }

   /**
    * Returns the value of this property.
    * 
    * @return the value of this property.
    */
   public String getValue () {
      return value;
   }

   /**
    * Sets the value of this property to \texttt{value}.
    * 
    * @param value
    *           the value of this property.
    * @exception NullPointerException
    *               if \texttt{value} is \texttt{null}.
    */
   public void setValue (String value) {
      this.value = value;
   }

   @Override
   public void check () {
      super.check ();
      if (name == null)
         throw new NullPointerException ();
   }

   @Override
   public boolean equals (Object o) {
      if (!(o instanceof PropertyParam))
         return false;
      final PropertyParam p = (PropertyParam) o;
      return (name == null ? p.getName () == null : name.equals (p.getName ()))
            && (value == null ? p.getValue () == null : value.equals (p
                  .getValue ()));
   }

   @Override
   public int hashCode () {
      return (name == null ? 0 : name.hashCode ())
            ^ (value == null ? 0 : value.hashCode ());
   }

   @Override
   public String toString () {
      return (name == null ? "null" : name) + "="
            + (value == null ? "null" : value);
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName, true,
            spc);
      el.setAttribute ("name", name);
      el.setAttribute ("value", value);
      return el;
   }

   @Override
   public PropertyParam clone () {
      try {
         return (PropertyParam) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
   }
}
