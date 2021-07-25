package umontreal.iro.lecuyer.xmlconfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import umontreal.ssj.util.ClassFinder;

/**
 * Provides common attributes for parameter objects. This class pvovides the
 * \texttt{id} attribute which can be used to identify an element in the XML
 * file. The \texttt{xref} attribute can be used to reference an identified
 * element. Both attributes are of type string.
 * 
 * \paragraph{Examples} \verb!<element id="element-id"/>!
 * 
 * \verb!<element xref="element-id"/>!
 */
public abstract class AbstractParam implements Param {
   /*
    * Unfortunately, this class
    * cannot enforce the fact that if xref is specified, no other attribute or
    * nested element must be specified.
    */
   private String id = "";
   private String xref = null;

   /**
    * Returns the identifier associated with this element. By default, this is
    * the empty string.
    * 
    * @return the identifier of the element.
    */
   public String getId () {
      return id;
   }

   /**
    * Sets the identifier of this parameter element to \texttt{id}.
    * 
    * @param id
    *           the new identifier of the element.
    * @exception NullPointerException
    *               if \texttt{id} is \texttt{null}.
    */
   public void setId (String id) {
      if (xref != null)
         throw new ParamReadException ("Cannot have both an id and xref");
      if (id.length () == 0)
         throw new ParamReadException ("Invalid empty id string");
      this.id = id;
   }

   /**
    * Returns the identifier of the referenced element. By default, this is
    * \texttt{null}.
    * 
    * @return the referenced element identifier.
    */
   public String getXref () {
      return xref;
   }

   /**
    * Sets the referenced identifier to \texttt{xref}.
    * 
    * @param xref
    *           the new referenced identifier.
    * @exception NullPointerException
    *               if \texttt{xref} is \texttt{null}.
    */
   public void setXref (String xref) {
      if (xref == null)
         throw new NullPointerException ();
      if (id != null && id.length () > 0)
         throw new ParamReadException ("Cannot have both an id and xref");
      this.xref = xref;
   }

   /**
    * Verifies that every needed parameter was specified. Throws a
    * {@link ParamReadException} in case of missing parameters.
    * 
    * @exception ParamReadException
    *               if some parameters are missing or invalid.
    */
   public void check () {}

   @Override
   public String toString () {
      if (id.equals ("") || xref == null || xref.equals (""))
         return super.toString ();
      if (xref != null)
         return getClass ().getName () + "[xref: " + xref + "]";
      else
         return getClass ().getName () + "[id: " + id + "]";
   }

   /**
    * Equivalent to
    * {@link #write(ClassFinder, String, StorableParam, String, int) write (new
    * ClassFinder(), fileName, par, rootName, spc)}.
    */
   public static void write (String fileName, StorableParam par,
         String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      write (new ClassFinder (), fileName, par, rootName, spc);
   }

   /**
    * Convenience method to write the parameters \texttt{par} into an XML file
    * \texttt{file}, with a root element name \texttt{rootName} and \texttt{spc}
    * spaces for each needed indentation level. The class finder \texttt{finder}
    * is used to convert {@link Class} objects into simple names. This method
    * uses an XML transformer to write the document obtained using the
    * {@link #createDocument} method to an XML file.
    * 
    * @param finder
    *           the class finder used to format class names.
    * @param fileName
    *           the name of the output file.
    * @param par
    *           the parameter object to be stored.
    * @param rootName
    *           the name of the root element of the XML file.
    * @param spc
    *           the number of spaces per indentation level.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception ParserConfigurationException
    *               if the XML document builder could not be created.
    * @exception TransformerException
    *               if the XML transformer could not be created properly.
    */
   public static void write (ClassFinder finder, String fileName,
         StorableParam par, String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      /*
       * This would be better to
       * put this method in StorableParam or another utility class, but no
       * methods can be defined in interfaces and no utility class should have
       * a single method.
       */
      final Document doc = createDocument (finder, par, rootName, spc);

      final TransformerFactory factory2 = TransformerFactory.newInstance ();
      final Transformer trans = factory2.newTransformer ();
      final Source source = new DOMSource (doc);
      final Result res = new StreamResult (fileName);
      trans.transform (source, res);
   }

   /**
    * Equivalent to {@link #write(ClassFinder, File, StorableParam, String, int)
    * write (new ClassFinder(), file, par, rootName, spc)}.
    */
   public static void write (File file, StorableParam par, String rootName,
         int spc) throws IOException, ParserConfigurationException,
         TransformerException {
      write (new ClassFinder (), file, par, rootName, spc);
   }

   /**
    * Same as {@link #write(ClassFinder, String, StorableParam, String, int)},
    * for a file object rather than a file name.
    * 
    * @param finder
    *           the class finder used to format class names.
    * @param file
    *           the object representing the output file.
    * @param par
    *           the parameter object to be stored.
    * @param rootName
    *           the name of the root element of the XML file.
    * @param spc
    *           the number of spaces per indentation level.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception ParserConfigurationException
    *               if the XML document builder could not be created.
    * @exception TransformerException
    *               if the XML transformer could not be created properly.
    */
   public static void write (ClassFinder finder, File file, StorableParam par,
         String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      final Document doc = createDocument (finder, par, rootName, spc);

      final TransformerFactory factory2 = TransformerFactory.newInstance ();
      final Transformer trans = factory2.newTransformer ();
      final Source source = new DOMSource (doc);
      final Result res = new StreamResult (file);
      trans.transform (source, res);
   }

   /**
    * Equivalent to
    * {@link #write(ClassFinder, OutputStream, StorableParam, String, int) write
    * (new ClassFinder(), out, par, rootName, spc)}.
    */
   public static void write (OutputStream out, StorableParam par,
         String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      write (new ClassFinder (), out, par, rootName, spc);
   }

   /**
    * Same as {@link #write(ClassFinder, String, StorableParam, String, int)},
    * but writes the XML contents to the output stream \texttt{out}.
    * 
    * @param finder
    *           the class finder used to format class names.
    * @param out
    *           the output stream for the XML contents.
    * @param par
    *           the parameter object to be stored.
    * @param rootName
    *           the name of the root element of the XML file.
    * @param spc
    *           the number of spaces per indentation level.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception ParserConfigurationException
    *               if the XML document builder could not be created.
    * @exception TransformerException
    *               if the XML transformer could not be created properly.
    */
   public static void write (ClassFinder finder, OutputStream out,
         StorableParam par, String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      final Document doc = createDocument (finder, par, rootName, spc);

      final TransformerFactory factory2 = TransformerFactory.newInstance ();
      final Transformer trans = factory2.newTransformer ();
      final Source source = new DOMSource (doc);
      final Result res = new StreamResult (out);
      trans.transform (source, res);
   }

   /**
    * Equivalent to
    * {@link #write(ClassFinder, Writer, StorableParam, String, int) write (new
    * ClassFinder(), out, par, rootName, spc)}.
    */
   public static void write (Writer out, StorableParam par, String rootName,
         int spc) throws IOException, ParserConfigurationException,
         TransformerException {
      write (new ClassFinder (), out, par, rootName, spc);
   }

   /**
    * Same as {@link #write(ClassFinder, String, StorableParam, String, int)},
    * but writes the XML contents to the writer \texttt{out}.
    * 
    * @param finder
    *           the class finder used to format class names.
    * @param out
    *           the writer for the XML contents.
    * @param par
    *           the parameter object to be stored.
    * @param rootName
    *           the name of the root element of the XML file.
    * @param spc
    *           the number of spaces per indentation level.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception ParserConfigurationException
    *               if the XML document builder could not be created.
    * @exception TransformerException
    *               if the XML transformer could not be created properly.
    */
   public static void write (ClassFinder finder, Writer out, StorableParam par,
         String rootName, int spc) throws IOException,
         ParserConfigurationException, TransformerException {
      final Document doc = createDocument (finder, par, rootName, spc);

      final TransformerFactory factory2 = TransformerFactory.newInstance ();
      final Transformer trans = factory2.newTransformer ();
      final Source source = new DOMSource (doc);
      final Result res = new StreamResult (out);
      trans.transform (source, res);
   }

   /**
    * Constructs a DOM document from the storable parameter object \texttt{par},
    * using the class finder \texttt{finder} to resolve simple class names, with
    * root element with name \texttt{rootName}, and with \texttt{spc} spaces of
    * indentation.
    * 
    * @param finder
    *           the class finder used to resolve simple class names.
    * @param par
    *           the parameter object to write.
    * @param rootName
    *           the name of the root element.
    * @param spc
    *           the number of spaces of indentation.
    * @return the constructed document.
    * @throws ParserConfigurationException
    */
   public static Document createDocument (ClassFinder finder,
         StorableParam par, String rootName, int spc)
         throws ParserConfigurationException {
      final DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance ();
      factory.setExpandEntityReferences (true);
      factory.setIgnoringElementContentWhitespace (true);
      factory.setValidating (false);
      factory.setCoalescing (true);
      final DocumentBuilder builder = factory.newDocumentBuilder ();
      final Document doc = builder.newDocument ();
      if (finder == null)
         return createDocument (new ClassFinder (), par, rootName, spc);
      for (final String importString : finder.getImports ()) {
         if (importString.equals ("java.lang.*"))
            continue;
         doc.appendChild (doc.createProcessingInstruction ("import",
               importString));
      }
      par.toElement (finder, doc, rootName, spc);
      return doc;
   }
}
