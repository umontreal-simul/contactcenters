package umontreal.iro.lecuyer.xmlconfig;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * This exception is thrown when a problem happens when converting a DOM
 * document into a tree of parameter objects.
 */
public class ParamReadException extends RuntimeException {
   private static final long serialVersionUID = 3535119310528089586L;
   Node node;

   /**
    * Constructs a new parameter reading exception with no message and no node.
    */
   public ParamReadException () {
      super ();
   }

   /**
    * Constructs a new parameter reading exception with no message and the node
    * \texttt{node}.
    * 
    * @param node
    *           the node the exception happened into.
    */
   public ParamReadException (Node node) {
      super ();
      this.node = node;
   }

   /**
    * Constructs a new parameter reading exception with the given
    * \texttt{message}.
    * 
    * @param message
    *           the error message describing the exception.
    */
   public ParamReadException (String message) {
      super (message);
   }

   /**
    * Constructs a new parameter reading exception with the given
    * \texttt{message} and the node \texttt{node}.
    * 
    * @param node
    *           the node the exception happened into.
    * @param message
    *           the error message describing the exception.
    */
   public ParamReadException (Node node, String message) {
      super (message);
      this.node = node;
   }

   /**
    * Returns the DOM node in which the exception happened. If no node was
    * associated with this exception, this returns \texttt{null}.
    * 
    * @return the node in which the exception happened, or \texttt{null}.
    */
   public Node getNode () {
      return node;
   }

   /**
    * Returns a short description of this exception. If no DOM node is
    * associated with the exception, this calls the base class's
    * \texttt{toString} method. Otherwise, a string containing the following
    * elements is constructed and returned. \begin{itemize} \item The name of
    * the class of this object. \item \texttt{": In "} \item Information about
    * the concerned node \item If a description message was provided
    * \begin{itemize} \item \texttt{", "} \item The description message
    * \end{itemize} \end{itemize} The node information depends on its type. For
    * an element, the formatted node name is given. For an attribute, the name,
    * value and owner element formatted name are given. For a text node, the
    * contents of the node is returned, as well as the formatted name of its
    * parent, are given. For a generic node, the name and value are returned.
    * All node names are formatted using {@link DOMUtils#formatNodeName} and
    * values are formatted using {@link DOMUtils#formatNodeValue} with a maximal
    * length of 100.
    * 
    * @return the short string describing the exception.
    */
   @Override
   public String toString () {
      if (node == null)
         return super.toString ();

      final StringBuilder msg = new StringBuilder (getClass ().getName ());
      msg.append (": In ");
      if (node instanceof Element) {
         msg.append ("element ");
         msg.append (DOMUtils.formatNodeName (node));
      }
      else if (node instanceof Attr) {
         msg.append ("attribute ");
         msg.append (node.getNodeName ());
         msg.append ("=\"");
         msg.append (DOMUtils.formatNodeValue (node, 100));
         msg.append ("\"");
         final Element owner = ((Attr) node).getOwnerElement ();
         if (owner != null)
            msg.append (" owned by element ").append (
                  DOMUtils.formatNodeName (owner));
      }
      else if (node instanceof Text) {
         msg.append ("text \"").append (DOMUtils.formatNodeValue (node, 100))
               .append ("\"");
         final Node parent = node.getParentNode ();
         if (parent != null)
            msg.append (" with parent ").append (
                  DOMUtils.formatNodeName (parent));
      }
      else {
         msg.append ("node ");
         msg.append (DOMUtils.formatNodeName (node)).append ("=\"").append (
               DOMUtils.formatNodeValue (node, 100)).append ('\"');
      }

      final String desc = getMessage ();
      if (desc != null)
         msg.append (", " + desc);
      return msg.toString ();
   }
}
