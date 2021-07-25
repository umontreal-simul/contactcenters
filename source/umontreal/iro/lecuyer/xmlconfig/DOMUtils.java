package umontreal.iro.lecuyer.xmlconfig;

import java.lang.reflect.Array;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import umontreal.iro.lecuyer.util.StringConvert;

/**
 * Provides utility methods to build indented DOM documents intended to be
 * converted to XML files. When using the DOM API to build a document, one must
 * manually insert spaces and end-of-lines, otherwise a one-line XML file will
 * be produced if the document is converted to a stream using a JAXP
 * transformer. JAXP does not give access to a flexible automatic indentation
 * facility when using its transformer factory; parameters for this feature are
 * specific to the XML transformer implementation. This class provides helper
 * methods capable of automatically adding some indentations and easing the
 * constructing of the most common DOM elements.
 */
public class DOMUtils {
   private DOMUtils () {}

   /**
    * Returns a string of spaces to indent future nested elements of
    * \texttt{node}, using \texttt{spc} spaces for each indentation level.
    * 
    * If \texttt{node} or its owner document are \texttt{null} or if the node is
    * a document, the empty string is returned. Otherwise, a string of
    * \texttt{spc} spaces is appended to the indentation that would be computed
    * for the parent of \texttt{node}.
    * 
    * @param node
    *           the node whose children must be indented.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the string of spaces.
    * @exception IllegalArgumentException
    *               if \texttt{spc} is negative.
    */
   public static String getIndent (Node node, int spc) {
      if (spc < 0)
         throw new IllegalArgumentException ("spc must not be negative");
      if (node == null)
         return "";
      final Document doc = node.getOwnerDocument ();
      if (doc == null)
         return "";
      final StringBuilder indent = new StringBuilder ();
      Node tnode = node;
      while (tnode != doc && tnode != null) {
         for (int i = 0; i < spc; i++)
            indent.append (' ');
         tnode = tnode.getParentNode ();
      }
      return indent.toString ();
   }

   /**
    * Adds the nested element with tag name \texttt{name} and with nested text
    * \texttt{text} to the parent node \texttt{parent}. In the parent node, a
    * text node whose contents is generated using {@link #getIndent} is added if
    * \texttt{spc} is greater than 0. A new element is constructed and added to
    * the parent. If \texttt{spc} is greater than 0, a text node containing a
    * newline character is added to the parent. In the created element a text
    * node containing \texttt{text} is added and the element is returned.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the tag name of the new element.
    * @param text
    *           the nested text.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the newly-created element.
    */
   public static Element addNestedTextElement (Node parent, String name,
         String text, int spc) {
      if (name == null)
         throw new NullPointerException ("Null node name");
      final Element el = addNestedElement (parent, name, true, spc);
      addNestedTextToElement (el, text);
      return el;
   }
   
   public static void addNestedTextToElement (Element el, String text) {
      if (text != null && text.length () > 0) {
         final Document doc = el instanceof Document ? (Document) el
               : el.getOwnerDocument ();
         el.appendChild (doc.createTextNode (text));
      }
   }

   /**
    * This method is similar to {@link #addNestedTextElement} except that the
    * created element with tag name \texttt{name} will not have nested text by
    * default. If \texttt{empty} is \texttt{true}, no child node is added in the
    * element. Otherwise, a newline is added if \texttt{spc} is greater than 0.
    * This newline is added assuming that the new element will contain other
    * elements.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the tag name of the new element.
    * @param empty
    *           if the element will be empty.
    * @param spc
    *           the number of spaces for each level of indentation.
    * @return the created element.
    */
   public static Element addNestedElement (Node parent, String name,
         boolean empty, int spc) {
      if (name == null)
         throw new NullPointerException ("Null node name");
      final Document doc = parent instanceof Document ? (Document) parent
            : parent.getOwnerDocument ();
      final String indent = getIndent (parent, spc);
      if (indent.length () > 0)
         parent.appendChild (doc.createTextNode (indent));
      final Element el = doc.createElement (name);
      parent.appendChild (el);
      if (spc > 0 && parent != doc)
         parent.appendChild (doc.createTextNode ("\n"));
      if (!empty)
         el.appendChild (doc.createTextNode ("\n"));
      return el;
   }

   /**
    * Creates a comment node in the element \texttt{parent}, with text
    * \texttt{text}, using \texttt{spc} spaces per indentation level. Before the
    * created and returned comment node, the method adds a text node containing
    * the string returned by {@link #getIndent} to the \texttt{parent}. After
    * the comment node, it adds a text node containing a newline if \texttt{spc}
    * is greater than 0. In the comment \texttt{text}, any spaces or newlines at
    * the beginning and the end of the string is removed. After each newline,
    * the method adds the string returned by {@link #getIndent} folloed by
    * \texttt{spc} spaces.
    * 
    * @param parent
    *           the parent node.
    * @param text
    *           the text of the comment.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the created comment node.
    */
   public static Comment addNestedComment (Node parent, String text, int spc) {
      if (text == null)
         throw new NullPointerException ("Null text comment");
      final Document doc = parent instanceof Document ? (Document) parent
            : parent.getOwnerDocument ();
      String indent = getIndent (parent, spc);
      if (indent.length () > 0)
         parent.appendChild (doc.createTextNode (indent));
      final StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < spc; i++)
         sb.append (' ');
      indent += sb.toString ();
      final String processedText;
      if (spc > 0)
         processedText = text.replaceAll ("^\\s*", "").replaceAll ("\\s*$", "")
               .replaceAll ("(\n|\r|\r\n)", "$1" + indent + " ");
      else
         processedText = text;
      final Comment comm = doc.createComment (" " + processedText + " ");
      parent.appendChild (comm);
      if (spc > 0 && parent != doc)
         parent.appendChild (doc.createTextNode ("\n"));
      return comm;
   }

   /**
    * Creates a new element with name \texttt{name}, with the contents of the
    * array \texttt{array}, and adds it to node \texttt{parent}. It outputs an
    * array intended to be read by {@link ArrayParam}.
    * 
    * The method uses {@link #addNestedElement} to create the element and for
    * each element in the array, it creates a \texttt{row} subelement containing
    * the value. Before adding the \texttt{row i} using
    * {@link #addNestedTextElement}, if \texttt{rowNames[i]} is
    * non-\texttt{null}, a comment containing \texttt{rowNames[i]} is added
    * using {@link #addNestedComment}. Elements are extracted from the array
    * using {@link Array#get} and converted to {@link String} using
    * {@link StringConvert#numberToString(Number)} for numeric types, and
    * {@link Object#toString} for other types. The given array must not contain
    * \texttt{null} values. If an element in the array appears several times
    * consecutively, as tested by {@link Object#equals}, a \texttt{repeat}
    * attribute is added to the corresponding \texttt{row} subelement instead of
    * repeating the value.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the name of the created element.
    * @param array
    *           the array being formatted.
    * @param rowNames
    *           an array containing a name for each array element, or
    *           \texttt{null}.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the created array element.
    * @exception IllegalArgumentException
    *               if \texttt{array} is not an instance of an array class.
    */
   public static Element addNestedArrayElement (Node parent, String name,
         Object array, String[] rowNames, int spc) {
      final Element el = addNestedElement (parent, name, true, spc);
      addArrayToElement (el, array, rowNames, spc);
      return el;
   }
   
   private static String toString (Object e) {
      if (e == null)
         return "null";
      if (e instanceof Number)
         return StringConvert.numberToString ((Number)e);
      return e.toString ();
   }
   
   public static void addArrayToElement (Element el, Object array, String[] rowNames, int spc) {
      if (!array.getClass ().isArray ())
         throw new IllegalArgumentException (
               "The given argument is not an array");
      if (array.getClass ().getComponentType ().isArray ())
         throw new IllegalArgumentException (
               "Only one-dimensional arrays are supported");
      final int length = Array.getLength (array);
      if (length == 0)
         return;
      else if (length == 1) {
         final Object e = Array.get (array, 0);
         final String str = toString (e);
               
         addNestedTextToElement (el, str);
         return;
      }
      addNestedTextToElement (el, "\n");
      final Class<?> cClass = array.getClass ().getComponentType ();
      if (rowNames == null && (cClass.isPrimitive () ||
            Number.class.isAssignableFrom (cClass) ||
            Boolean.class.isAssignableFrom (cClass) ||
            Character.class.isAssignableFrom (cClass))) {
         // We have an array of values of primitive type
         final StringBuilder sb = new StringBuilder();
         for (int i = 0; i < length; i++) {
            final Object e = Array.get (array, i);
            final String str = toString (e);
            sb.append (str);
            sb.append ('\n');
         }         
         addNestedTextToElement (el, sb.toString ());
      }
      else
         for (int i = 0; i < length; i++) {
            int rep = 1;
            final Object e = Array.get (array, i);
            while (i < length - 1) {
               if (rowNames != null && i + 1 < rowNames.length) {
                  final String n1 = rowNames[i];
                  final String n2 = rowNames[i + 1];
                  if (n1 == null && n2 != null)
                     break;
                  if (n1 != null && n2 == null)
                     break;
                  if (n1 != null && n2 != null && !n1.equals (n2))
                     break;
               }
               final Object eNext = Array.get (array, i + 1);
               if (!e.equals (eNext))
                  break;
               ++i;
               ++rep;
            }
            if (rowNames != null && i < rowNames.length && rowNames[i] != null)
               addNestedComment (el, rowNames[i], spc);
            final String str = toString (e);
            final Element row = addNestedTextElement (el, "row", str, spc);
            if (rep > 1)
               row.setAttribute ("repeat", String.valueOf (rep));
         }
      endElement (el, spc);
   }

   /**
    * Equivalent to
    * {@link #addNestedArrayElement(Node,String,Object,String[],int)
    * add\-Nested\-Array\-Element} \texttt{(parent, name, array, null, spc)}.
    * This adds an array element with no comments describing rows.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the name of the created element.
    * @param array
    *           the array being formatted.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the created array element.
    * @exception IllegalArgumentException
    *               if \texttt{array} is not an instance of an array class.
    */
   public static Element addNestedArrayElement (Node parent, String name,
         Object array, int spc) {
      return addNestedArrayElement (parent, name, array, null, spc);
   }

   /**
    * Creates a new element with name \texttt{name}, with the contents of the 2D
    * array \texttt{array2D}, and adds it to node \texttt{parent}. It outputs a
    * 2D array intended to be read by {@link ArrayParam2D}.
    * 
    * The method uses {@link #addNestedElement} to create the element and for
    * each row in the 2D array, it creates a \texttt{row} subelement containing
    * the array values. Before adding the \texttt{row i} using
    * {@link #addNestedTextElement}, if \texttt{rowNames[i]} is
    * non-\texttt{null}, a comment containing \texttt{rowNames[i]} is added
    * using {@link #addNestedComment}. Each row of the 2D array corresponds to
    * a 1D array. Each \texttt{row} element contains a comma-separated list of
    * array values. Each value is formatted with
    * {@link StringConvert#numberToString(Number)} for numeric types, or
    * {@link Object#toString} for other types. If consecutive rows contain the
    * same number of columns and the same elements, as tested with
    * {@link Object#equals}, only one \texttt{row} element is added to
    * represent the repeated row, using the \texttt{repeat} attribute.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the name of the created element.
    * @param array2D
    *           the 2D array being formatted.
    * @param rowNames
    *           an array containing a name associated with each row, or
    *           \texttt{null}.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the created matrix element.
    * @exception IllegalArgumentException
    *               if \texttt{matrix} is not an instance of a 2D array class.
    */
   public static Element addNestedArray2DElement (Node parent, String name,
         Object array2D, String[] rowNames, int spc) {
      final Element el = addNestedElement (parent, name, true, spc);
      addArray2DToElement (el, array2D, rowNames, spc);
      return el;
   }
   
   public static void addArray2DToElement (Element el, Object array2D, String[] rowNames, int spc) {
      final Class<?> cl = array2D.getClass ();
      if (!cl.isArray ())
         throw new IllegalArgumentException (
               "The given argument is not an array");
      if (!cl.getComponentType ().isArray ())
         throw new IllegalArgumentException (
               "The given argument is not a 2D array");
      if (cl.getComponentType ().getComponentType ().isArray ())
         throw new IllegalArgumentException ("3D arrays not supported");
      final int numRows = Array.getLength (array2D);
      if (numRows == 0)
         return;
      addNestedTextToElement (el, "\n");
      for (int i = 0; i < numRows; i++) {
         final Object array = Array.get (array2D, i);
         final int numCols = Array.getLength (array);
         int rep = 1;
         while (i < numRows - 1) {
            final Object array2 = Array.get (array2D, i + 1);
            final int numCols2 = Array.getLength (array2);
            if (rowNames != null && i + 1 < rowNames.length) {
               final String n1 = rowNames[i];
               final String n2 = rowNames[i + 1];
               if (n1 == null && n2 != null)
                  break;
               if (n1 != null && n2 == null)
                  break;
               if (n1 != null && n2 != null && !n1.equals (n2))
                  break;
            }
            if (numCols != numCols2)
               break;
            boolean different = false;
            for (int j = 0; j < numCols && !different; j++) {
               final Object e1 = Array.get (array, j);
               final Object e2 = Array.get (array2, j);
               if (!e1.equals (e2))
                  different = true;
            }
            if (different)
               break;
            ++rep;
            ++i;
         }

         final StringBuilder sb = new StringBuilder ();
         for (int j = 0; j < numCols; j++) {
            final Object e = Array.get (array, j);
            final String str = toString (e);
            sb.append (j > 0 ? "  " : "").append (str);
         }
         if (rowNames != null && i < rowNames.length && rowNames[i] != null)
            addNestedComment (el, rowNames[i], spc);
         final Element row = addNestedTextElement (el, "row", sb.toString (),
               spc);
         if (rep > 1)
            row.setAttribute ("repeat", String.valueOf (rep));
      }
      endElement (el, spc);
   }

   /**
    * Equivalent to
    * {@link #addNestedArray2DElement(Node,String,Object,String[],int)
    * add\-Nested\-Array2D\-Element} \texttt{(parent, name, array2D, null, spc)}.
    * This adds a 2D array element with no comments describing rows.
    * 
    * @param parent
    *           the parent node.
    * @param name
    *           the name of the created element.
    * @param array2D
    *           the 2D array being formatted.
    * @param spc
    *           the number of spaces for each indentation level.
    * @return the created matrix element.
    * @exception IllegalArgumentException
    *               if \texttt{matrix} is not an instance of a 2D array class.
    */
   public static Element addNestedArray2DElement (Node parent, String name,
         Object array2D, int spc) {
      return addNestedArray2DElement (parent, name, array2D, null, spc);
   }

   /**
    * Terminates the nested element \texttt{el}. This method uses
    * {@link #getIndent} to add a text node composed of spaces in the element.
    * This allows the closing tag of the element to appear at the same
    * indentation level as the opening tag.
    * 
    * @param el
    *           the terminated element.
    * @param spc
    *           the number of spaces for each indentation level.
    */
   public static void endElement (Element el, int spc) {
      final Document doc = el.getOwnerDocument ();
      String indent = getIndent (el, spc);
      if (indent.length () > 0)
         indent = indent.substring (0, indent.length () - spc);
      if (indent.length () > 0)
         el.appendChild (doc.createTextNode (indent));
   }

   /**
    * Suppresses any indenting text node from the DOM node \texttt{node}. This
    * recursively removes any child text node containing only newlines and
    * spaces. After the cleanup process, the node is normalized using
    * {@link Node#normalize}.
    * 
    * @param node
    *           the node (or document) being unindented.
    */
   public static void unindent (Node node) {
      doUnindent (node);
      node.normalize ();
   }

   private static void doUnindent (Node node) {
      final NodeList nl = node.getChildNodes ();
      for (int i = 0; i < nl.getLength (); i++) {
         final Node child = nl.item (i);
         if (child instanceof Text && child.getNodeValue ().matches ("\\s*")) {
            node.removeChild (child);
            i--;
         }
         else
            doUnindent (child);
      }
   }

   /**
    * Adds newlines and whitespaces for the node \texttt{node} to be indented in
    * an XML output file. For each child element of \texttt{node}, an indent
    * string obtained with {@link #getIndent getIndent} \texttt{(node, spc)} is
    * prepended and a newline is appended.
    * 
    * @param node
    *           the node being reindented.
    * @param spc
    *           the number of spaces for each indentation level.
    */
   public static void reindent (Node node, int spc) {
      doReindent (node, spc);
   }

   private static boolean doReindent (Node node, int spc) {
      final Document doc = node instanceof Document ? (Document) node : node
            .getOwnerDocument ();
      if (doc == null)
         throw new NullPointerException (
               "The given node does not belong to a Document");
      final NodeList nl = node.getChildNodes ();
      String indent = getIndent (node, spc);
      String cindent = indent;
      for (int i = 0; i < spc; i++)
         cindent += " ";
      boolean foundElement = false;
      for (int i = 0; i < nl.getLength (); i++) {
         final Node child = nl.item (i);
         if (node != doc
               && (child instanceof Element || child instanceof Comment || child instanceof ProcessingInstruction)) {
            node.insertBefore (doc.createTextNode (indent), child);
            i++;
            node.insertBefore (doc.createTextNode ("\n"), nl.item (i + 1));
         }
         if (child instanceof CharacterData && !(child instanceof CDATASection)) {
            final CharacterData cdata = (CharacterData) child;
            String data = cdata.getData ();
            if (!data.matches ("\\s*")) {
               data = data.replaceAll ("^\\s*", "").replaceAll ("\\s*$", "");
               data = data.replaceAll ("\n\\s*", "\n" + cindent);
               if (child instanceof Comment)
                  data = " " + data + " ";
               cdata.setData (data);
            }
         }
         else if (child instanceof Element) {
            foundElement = true;
            if (doReindent (child, spc))
               child.insertBefore (doc.createTextNode ("\n"), child
                     .getChildNodes ().item (0));
         }
      }
      if (foundElement)
         if (indent.length () > 0) {
            indent = indent.substring (0, indent.length () - spc);
            node.appendChild (doc.createTextNode (indent));
         }
      return foundElement;
   }

   /**
    * Formats a string representing the name of this node \texttt{node} in the
    * XML document, in a XPath-like format. If the given node is an attribute,
    * the string \texttt{[@name]}, where \texttt{name} is the attribute name, is
    * appended to the return value of this method for its owner element. The
    * format of the string for an element is given by
    * \texttt{[parent/]tagname(index)}, where \texttt{parent} is the result of
    * this method for the parent node, \texttt{tagname} is the name of the
    * element and \texttt{index} is the return value of {@link #getNodeIndex}.
    * If the node has no sibling, as tested by {@link #nodeHasSiblings}, the
    * index as well as the parentheses are omitted.
    * 
    * @param node
    *           the node name.
    * @return the string representation.
    */
   public static String formatNodeName (Node node) {
      final StringBuilder sb = new StringBuilder ();
      if (node instanceof Attr) {
         sb.append (formatNodeName (((Attr) node).getOwnerElement ()));
         sb.append ('[').append (node.getNodeName ()).append (']');
         return sb.toString ();
      }
      boolean first = true;
      Node testedNode = node;
      while (testedNode != null && !(testedNode instanceof Document)) {
         if (first)
            first = false;
         else
            sb.insert (0, '/');
        
         final String ind;
         if (nodeHasSiblings (testedNode))
            ind = "(" + getNodeIndex (testedNode) + ")";
         else
            ind = "";
         sb.insert (0, testedNode.getNodeName () + ind);
         testedNode = testedNode.getParentNode ();
      }
      return sb.toString ();
   }

   /**
    * Determines if the node \texttt{node} has at least one sibling.
    * 
    * @param node
    *           the tested node.
    * @return \texttt{true} if the node has at least a previous or a next
    *         sibling, \texttt{false} otherwise.
    */
   public static boolean nodeHasSiblings (Node node) {
      Node tnode = node;
      final int ntype = node.getNodeType ();
      final String nname = node.getNodeName ();
      while (tnode.getPreviousSibling () != null) {
         tnode = tnode.getPreviousSibling ();
         if (tnode.getNodeType () == ntype
               && tnode.getNodeName ().equals (nname))
            return true;
      }
      tnode = node;
      while (tnode.getNextSibling () != null) {
         tnode = tnode.getNextSibling ();
         if (tnode.getNodeType () == ntype
               && tnode.getNodeName ().equals (nname))
            return true;
      }
      return false;
   }

   /**
    * Returns the index of the child node \texttt{node} for its parent.
    * 
    * @param node
    *           the queried node.
    * @return the index of the node.
    */
   public static int getNodeIndex (Node node) {
      int i = 0;
      final short ntype = node.getNodeType ();
      final String nname = node.getNodeName ();
      Node testedNode = node;
      while (testedNode.getPreviousSibling () != null) {
         testedNode = testedNode.getPreviousSibling ();
         if (testedNode.getNodeType () == ntype
               && testedNode.getNodeName ().equals (nname))
            ++i;
      }
      return i;
   }

   /**
    * Formats the value of the node \texttt{node} with maximal string length
    * \texttt{maxLength}. The method first gets the result of \texttt{node.}{@link Node#getNodeValue getNodeValue()}
    * If this result is \texttt{null}, it returns \texttt{null}. Otherwise, if
    * the length of the string is smaller than \texttt{maxLength}, the string is
    * returned unchanged. Otherwise, it is truncated to \texttt{maxLength} and
    * \texttt{...} is appended to the result.
    * 
    * @param node
    *           the node to be processed.
    * @return the formatted value.
    */
   public static String formatNodeValue (Node node, int maxLength) {
      final String v = node.getNodeValue ();
      if (v == null)
         return null;
      if (v.length () > maxLength)
         return v.substring (0, maxLength) + "...";
      else
         return v;
   }
}
