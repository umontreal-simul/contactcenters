package umontreal.iro.lecuyer.xmlconfig;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;

/**
 * Represents a parameter object providing the capability to be converted back
 * to a DOM element.
 */
public interface StorableParam extends Param {
   /**
    * Converts this parameter object to a DOM element using the class finder
    * \texttt{finder} for formatting class names, with parent node
    * \texttt{parent}, element name \texttt{elementName}, and \texttt{spc}
    * spaces for each indentation level. The method must create an
    * {@link Element} instance with name \texttt{elementName} and add it to the
    * node \texttt{parent} of the DOM tree. It is recommended to use
    * {@link DOMUtils} helper methods for this. After the element is created,
    * attributes can be set and nested contents can be added. The configured DOM
    * element is then returned.
    * 
    * @param finder
    *           the class finder used to format class names.
    * @param parent
    *           the parent of the new element.
    * @param elementName
    *           the name of the constructed element.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the newly-constructed element.
    */
   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc);
}
