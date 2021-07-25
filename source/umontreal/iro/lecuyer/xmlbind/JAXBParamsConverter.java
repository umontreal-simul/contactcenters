package umontreal.iro.lecuyer.xmlbind;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Formatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.iro.lecuyer.xmlconfig.DOMUtils;

/**
 * Convenience base class to marshal and unmarshal objects of a
 * specific class using JAXB.
 *  When using JAXB directly, one must create a context,
 * use that context to get an unmarshaller or a marshaller,
 * convert the unmarshalled JAXB element into a value object, or
 * wrap a value object into a JAXB element for marshalling.
 * This class can help in performing these tasks.
 * It also manages the association of a schema to unmarshallers, and
 * marshallers in order to perform validation.   
 * It also implements a mechanism to
 * replace JAXB-generated namespace prefixes by user-defined prefixes
 * while marshalling.
 * This does not affect the validity of marshalled XML, but
 * it can increase the readability of the output files.
 * 
 * The methods {@link #unmarshal(File)},
 * and {@link #marshal(Object, File)} can be used
 * to marshal and unmarshal objects.
 * Alternatively, simple console applications may use
 * {@link #unmarshalOrExit(File)}, and
 * {@link #marshalOrExit(Object, File)}
 * which prints a detailed message and exits in case of an error while
 * the regular methods throw exceptions.
 * 
 * This class must be extended with a specific type of parameter object to be
 * used. This type can be any class derived by the
 * JAXB-provided \texttt{xjc}
 * compiler from a XML Schema.
 * It must be passed as a type parameter when creating this class.
 * More specifically, 
 * any concrete subclass must implement the {@link #getContext()} method used
 * to create the JAXB context as well as the {@link #getSchema()} method to get
 * the schema object used for validation.
 * 
 * For example, suppose that we have a Schema describing parameters
 * of call centers, with a root element of type \texttt{CallCenterParams}.
 * The JAXB compiler produces a class named \texttt{CallCenterParams}.
 * The user-defined converter class then
 * extends the base class \texttt{JAXB\-Params\-Converter<Call\-Center\-Params>},
 * and provides an implementation for the two mandatory methods.
 * 
 * @param <T>
 *           the type of objects processed by the converter.
 */
public abstract class JAXBParamsConverter<T> {
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.xmlbind");
   // The original schemas could be parsed with Java 6 while
   // parsing with
   // Java 5 triggered an unexpected error, preventing
   // the parsing of XML files completely.
   // This field was put as a workaround disabling the schema
   // validation in cases where this reading problem happens.
   // However, the problem disappeared after we have removed
   // tilde characters from namespace URIs.
   // We kept the field for safety, if the error ever comes back.
   private boolean schemaReadBug = false;
   private boolean validating = true;
   private ValidationEventHandler handler;
   private Class<T> objClass;

   /**
    * Constructs a new converter manipulating objects of class
    * \texttt{objClass}.
    * 
    * @param objClass
    *           the class of objects.
    */
   public JAXBParamsConverter (Class<T> objClass) {
      if (objClass == null)
         throw new NullPointerException ();
      this.objClass = objClass;
   }

   /**
    * Constructs and returns the JAXB context used to read parameters. This
    * method should create and return a static instance of the JAXB context
    * since the creation of the context is costly.
    * 
    * Any concrete subclass should define a static field
    * of type {@link JAXBContext}.
    * If the field is non-\texttt{null}, the method returns
    * its value.
    * Otherwise, it initializes the field using
    * {@link JAXBContext#newInstance(String)},
    * and returns the resulting context.
    * The arguments given to JAXB depends on the
    * JAXB-derived class associated with the concrete subclass.
    * 
    * @return the JAXB context to be used.
    * @throws JAXBException if an error occurs while creating the JAXB context.
    */
   public abstract JAXBContext getContext () throws JAXBException;

   /**
    * Constructs and returns a schema for the document
    * type represented by a concrete subclass. If no
    * schema is used, this method should return \texttt{null}. If an error
    * occurs when reading or parsing the schema, this method should throws a SAX
    * exception.
    * 
    * It is recommended to use {@link SchemaFactory} to create the
    * {@link Schema} object, and to store it in a static variable for future
    * use, because loading and parsing the schema might be costly.
    * If the schema is stored at the same location as class files,
    * {@link Class#getResourceAsStream(String)} can be used
    * to obtain a stream for the schema.
    * 
    * @return the schema object, or \texttt{null}.
    * @throws SAXException
    *            if an error occurred during reading or parsing.
    */
   public abstract Schema getSchema () throws SAXException;

   /**
    * Returns a map associating prefixes with namespace URI. Each key of the
    * returned map corresponds to a prefix while each value denotes a URI.
    * This map is used while marshalling in order to assign
    * meaningful prefixes to namespace URI rather than the
    * default prefixes. 
    * An
    * empty map can be used to disable namespace prefix mapping.
    * 
    * When using namespaces, each XML element and attribute can be qualified
    * with a namespace URI which is referred to, in the XML document, using a
    * prefix. These prefixes, which are not unique in contrast with URIs, can be
    * chosen arbitrarily, but they should be human-readable for clearer
    * documents. However, by default, JAXB generates its own prefix each time it
    * finds a new namespace URI during marshalling; there is no standard way to
    * impose prefixes. This map can be used to bind user-defined prefixes to the
    * URIs used by the XML document. When this method returns a non-empty map,
    * the marshalling mechanism of this class uses the
    * {@link RemappingContentHandler} to perform the namespace prefix mapping in
    * a way independent from the JAXB implementation.
    * 
    * @return the map associating namsepace prefixes to URIs.
    */
   public abstract Map<String, String> getNamespacePrefixes ();

   /**
    * Tries to use {@link #getSchema()} to obtain the schema. If this method
    * throws a SAX exception, the exception is logged, validation is disabled,
    * and this method returns \texttt{null}. Otherwise, the schema object is
    * returned. This can be used to work around bugs in some Java implementations
    * preventing valid schemas to be read; parsing will then continue, without
    * validation.
    * 
    * @return the schema object, or \texttt{null}.
    */
   public Schema readSchema () {
      if (schemaReadBug)
         return null;
      try {
         final Schema schema = getSchema ();
         if (schema == null) {
            schemaReadBug = true;
            logger.warning ("No schema defined; disabling validation");
         }
         schemaReadBug = false;
         return schema;
      }
      catch (final SAXException se) {
         logger
               .log (
                     Level.WARNING,
                     "An exception occurred while reading schema; disabling validation",
                     se);
         schemaReadBug = true;
         return null;
      }
   }

   /**
    * Initializes the unmarshaller before it is used by this object. By default,
    * this method installs the event handler returned by
    * {@link #getEventHandler()}. It can be overridden to perform custom
    * initialization such as setting properties.
    * 
    * @param um
    *           the unmarshaller being initialized.
    */
   public void initUnmarshaller (Unmarshaller um) throws JAXBException {
      if (handler != null)
         um.setEventHandler (handler);
   }

   /**
    * Initializes the marshaller before it is used by this object. By default,
    * this method associates the event handler returned by
    * {@link #getEventHandler()} to the marshaller, and activates the
    * {@link Marshaller#JAXB_FORMATTED_OUTPUT} property. It can be overridden to
    * perform custom initialization such as setting properties.
    * 
    * @param m
    *           the marshaller being initialized.
    */
   public void initMarshaller (Marshaller m) throws JAXBException {
      if (handler != null)
         m.setEventHandler (handler);
      try {
         m.setProperty (Marshaller.JAXB_FORMATTED_OUTPUT, true);
      }
      catch (final PropertyException pe) {}
   }

   /**
    * Determines if this converter is validating unmarshalled and marshalled
    * instances. The default value is \texttt{true}, which activates validation.
    * However, validation is disabled if no schema is specified, or if the
    * schema cannot be initialized.
    * 
    * @return the status of the validating indicator.
    */
   public boolean isValidating () {
      return validating;
   }

   /**
    * Sets the status of the validating indicator to \texttt{validating}.
    * 
    * @param validating
    *           the new value of the indicator.
    */
   public void setValidating (boolean validating) {
      this.validating = validating;
   }

   /**
    * Returns the validation event handler associated with marshallers and
    * unmarshallers used by this converter. This returns \texttt{null} if no
    * handler was set (the default).
    * 
    * @return the validation event handler, or \texttt{null}.
    */
   public ValidationEventHandler getEventHandler () {
      return handler;
   }

   /**
    * Sets the validation event handler to \texttt{handler}.
    * 
    * @param handler
    *           the new validation event handler.
    */
   public void setEventHandler (ValidationEventHandler handler) {
      this.handler = handler;
   }

   /**
    * Converts the JAXB element returned by an unmarshaller into an instance of
    * class \texttt{T}. This uses the {@link JAXBIntrospector#getValue(Object)}
    * to obtain the object, and casts it to an instance of class \texttt{T}.
    * 
    * @param jaxbElement
    *           the JAXB element.
    * @return the cast object.
    * @throws JAXBException
    *            if an error occurs during conversion.
    */
   public T getValue (Object jaxbElement) throws JAXBException {
      final Object o = JAXBIntrospector.getValue (jaxbElement);
      if (o == null)
         throw new JAXBException (
               "The unmarshalling returned a non-JAXB object");
      if (objClass.isInstance (o))
         return objClass.cast (o);
      else
         throw new JAXBException (
               "The unmarshalling resulted in an object of class "
                     + o.getClass ().getName ()
                     + " while the expected class was " + objClass.getName ());
   }

   /**
    * Constructs and returns a JAXB element with \texttt{value} as a value. The
    * returned object can be given to a JAXB {@link Marshaller} in order to be
    * serialized to XML.
    * 
    * This method first uses {@link JAXBIntrospector#isElement(Object)} to test
    * if the given value is an element, and returns the value if the test
    * succeeds. Otherwise, this method looks for an \texttt{ObjectFactory}
    * class in the package of the class of the given value. 
    * If such a factory is found, the method searches
    * a factory method in this factory taking an object corresponding
    * to the type of the
    * given value, and calls this method to get a JAXB element encapsulating the
    * value.
    * 
    * @param value
    *           the value to encapsulate.
    * @return the JAXB element corresponding to the value.
    * @throws JAXBException
    *            if an error occurs during the process.
    */
   public Object getJAXBObject (Object value) throws JAXBException {
      final JAXBIntrospector in = getContext ().createJAXBIntrospector ();
      if (in.isElement (value))
         return value;

      Method factoryMethod = null;
      if (!objClass.isInstance (value))
         throw new JAXBException (
               "The given value must be an instance of class "
                     + objClass.getName ());
      Class<?> cls = value.getClass ();
      clsLoop: while (cls != null) {
         String factoryName;
         final Package pack = cls.getPackage ();
         if (pack != null) {
            final String pname = pack.getName ();
            factoryName = pname + ".ObjectFactory";
         }
         else
            factoryName = "ObjectFactory";
         Class<?> objectFactoryClass;
         try {
            objectFactoryClass = Class.forName (factoryName);
         }
         catch (final ClassNotFoundException cne) {
            cls = cls.getSuperclass ();
            continue;
         }
         if (objectFactoryClass.getAnnotation (XmlRegistry.class) == null) {
            cls = cls.getSuperclass ();
            continue;
         }

         for (final Method m : objectFactoryClass.getMethods ()) {
            if (m.getAnnotation (XmlElementDecl.class) == null)
               continue;
            if (!JAXBElement.class.isAssignableFrom (m.getReturnType ()))
               continue;
            final Class<?>[] args = m.getParameterTypes ();
            if (args.length != 1)
               continue;
            if (args[0].equals (cls)) {
               factoryMethod = m;
               break clsLoop;
            }
         }
         cls = cls.getSuperclass ();
      }

      if (factoryMethod == null)
         throw new JAXBException ("Cannot get JAXB element from value object "
               + value);
      try {
         final Constructor<?> ctor = factoryMethod.getDeclaringClass ()
               .getConstructor ();
         final Object factory = ctor.newInstance ();
         return factoryMethod.invoke (factory, value);
      }
      catch (final NoSuchMethodException nme) {
         final JAXBException iae = new JAXBException (
               "Cannot construct JAXB element from value " + value
                     + "; no nullary constructor in class "
                     + factoryMethod.getDeclaringClass ().getName ());
         iae.initCause (nme);
         throw iae;
      }
      catch (final IllegalAccessException iace) {
         final JAXBException iae = new JAXBException (
               "Cannot construct JAXB element from value " + value
                     + "; illegal access");
         iae.initCause (iace);
         throw iae;
      }
      catch (final InstantiationException ie) {
         final JAXBException iae = new JAXBException (
               "Cannot construct JAXB element from value " + value);
         iae.initCause (ie);
         throw iae;
      }
      catch (final InvocationTargetException ite) {
         final JAXBException iae = new JAXBException (
               "Cannot construct JAXB element from value " + value);
         iae.initCause (ite.getCause ());
         throw iae;
      }
   }

   /**
    * Unmarshals the given input file to an object.
    * 
    * This method first calls {@link #getContext()} to obtain a JAXB context. It
    * then uses the obtained JAXB context to create an unmarshaller, sets its
    * schema to the value returned by {@link #readSchema()}, and
    * initializes it by
    * calling {@link #initUnmarshaller(Unmarshaller)}. It then uses the
    * unmarshaller to process the given file, and calls
    * {@link #getValue(Object)} to obtain the value of the resulting JAXB
    * element.
    * 
    * @param file
    *           the input file.
    * @return the unmarshalled object.
    * @throws JAXBException
    *            if an error occurs during unmarshalling.
    */
   public T unmarshal (File file) throws JAXBException {
      final Unmarshaller um = getContext ().createUnmarshaller ();
      if (validating && !schemaReadBug)
         um.setSchema (readSchema ());
      initUnmarshaller (um);
      final Object res = um.unmarshal (file);
      return getValue (res);
   }

   /**
    * Unmarshals the given URL into an object, and returns the constructed
    * object. This is simular to {@link #unmarshal(File)}, for a URL.
    * 
    * @param url
    *           the URL of the input.
    * @return the output object.
    * @throws JAXBException
    *            if an error occurs during unmarshalling.
    */
   public T unmarshal (URL url) throws JAXBException {
      final Unmarshaller um = getContext ().createUnmarshaller ();
      if (validating && !schemaReadBug)
         um.setSchema (readSchema ());
      initUnmarshaller (um);
      final Object res = um.unmarshal (url);
      return getValue (res);
   }

   /**
    * Reads the given input file as a GZipped file,
    * using {@link GZIPInputStream},
    * and unmarshals the uncompressed XML data.
    * If the given file is not in GZIP format,
    * this method calls {@link #unmarshal(File)}
    * to unmarshal a plain text file.  
    * @param file the file to be read.
    * @return the unmarshalled object.
    * @throws JAXBException if an error occurs during file reading.
    */
   public T unmarshalGZipped (File file) throws JAXBException {
      try {
         final GZIPInputStream gz = new GZIPInputStream (new FileInputStream (file));
         final Unmarshaller um = getContext ().createUnmarshaller ();
         if (validating && !schemaReadBug)
            um.setSchema (readSchema ());
         initUnmarshaller (um);
         final StreamSource src = new StreamSource (gz);
         src.setSystemId (file);
         final Object res = um.unmarshal (src);
         gz.close ();
         return getValue (res);
      }
      catch (final IOException ioe) {
         return unmarshal (file);
      }
   }

   /**
    * Similar to {@link #unmarshalGZipped(File)},
    * for a URL instead of a file.
    * @param url the URL pointing to the XML data.
    * @return the unmarshalled object.
    * @throws JAXBException if an error occurs during the unmarshalling process.
    */
   public T unmarshalGZipped (URL url) throws JAXBException {
      try {
         final GZIPInputStream gz = new GZIPInputStream (url.openStream ());
         final Unmarshaller um = getContext ().createUnmarshaller ();
         if (validating && !schemaReadBug)
            um.setSchema (readSchema ());
         initUnmarshaller (um);
         final StreamSource src = new StreamSource (gz);
         src.setSystemId (url.toString ());
         final Object res = um.unmarshal (src);
         gz.close ();
         return getValue (res);
      }
      catch (final IOException ioe) {
         return unmarshal (url);
      }
   }
   
   /**
    * Unmarshals the given node into a JAXB object, and returns the constructed
    * object. This is similar to {@link #unmarshal(File)}, for a DOM node.
    * 
    * @param node
    *           the DOM node to be unmarshalled.
    * @return the resulting object.
    * @throws JAXBException
    *            if an error occurs during unmarshalling.
    */
   public T unmarshal (Node node) throws JAXBException {
      final Unmarshaller um = getContext ().createUnmarshaller ();
      if (validating && !schemaReadBug)
         um.setSchema (readSchema ());
      initUnmarshaller (um);
      final Object res = um.unmarshal (node);
      return getValue (res);
   }

   /**
    * Unmarshals the given source into an object, and returns the constructed
    * object. This is similar to {@link #unmarshal(File)}, for a source.
    * 
    * @param source
    *           the source to be unmarshalled.
    * @return the constructed object.
    * @throws JAXBException
    *            if an error occurs during unmarshalling.
    */
   public T unmarshal (Source source) throws JAXBException {
      final Unmarshaller um = getContext ().createUnmarshaller ();
      if (validating && !schemaReadBug)
         um.setSchema (readSchema ());
      initUnmarshaller (um);
      final Object res = um.unmarshal (source);
      return getValue (res);
   }
   
   /**
    * Unmarshals the given file using
    * {@link #unmarshalGZipped(File)}, but
    * if an error occurs, messages are
    * printed on the standard error output,
    * and the method exits the VM
    * using {@link System#exit(int)}.
    * This method is intended to be used
    * in \texttt{main} methods of command-line
    * programs.
    * 
    * @param file the input file.
    * @return the parameter object.
    */
   public T unmarshalOrExit (File file) {
      if (!file.exists ()) {
         System.err.println ("Cannot find the file " + file.getAbsolutePath ());
         System.exit (1);
      }

      final ValidationEventCollector col = new ValidationEventCollector();
      final ValidationEventHandler ev = getEventHandler ();

      try {
         setEventHandler (col);
         final T params = unmarshalGZipped (file);
         showProblems ("unmarshalling", file, col);
         return params;
      }
      catch (final UnmarshalException ume) {
         showErrors ("unmarshalling", file, col, ume);
         System.exit (1);
         return null;
      }
      catch (final JAXBException je) {
         showErrors ("unmarshalling", file, null, je);
         System.exit (1);
         return null;
      }
      finally {
         setEventHandler (ev);
      }
   }
   
   private static void showProblems (String action, File file, ValidationEventCollector col) {
      if (!col.hasEvents ())
         return;
      if (col.getEvents ().length == 1)
         System.err.printf ("The following problem occurred during %s, but did not abort the operation.%n",
               action);
      else
         System.err.printf ("The following problems occurred during %s, but did not abort the operation.%n",
               action);
      System.err.println (validationEventsToString (col.getEvents ()));
   }
   
   private static void showErrors (String action, File file, ValidationEventCollector col, Throwable tr) {
      boolean hasEvents = col != null && col.hasEvents ();
      if (col == null || col.getEvents ().length == 1)
         System.err.printf ("The following problem occurred during %s%s.%n", action,
               hasEvents ? "" : " of file " + file.getAbsolutePath ());
      else
         System.err.printf ("The following problems occurred during %s.%n", action);
      if (hasEvents)
         System.err.println (validationEventsToString (col.getEvents ()));
      else
         System.err.println (ExceptionUtil.throwableToString (tr));
   }

   /**
    * Marshals the given object
    * by generating SAX events and sending them to 
    * the given content handler.
    * 
    * This method calls {@link #getJAXBObject(Object)} on the given object to
    * convert it into a JAXB object. It then creates a marshaller using the
    * context returned by {@link #getContext()}, sets its schema to the value
    * returned by {@link #readSchema()}, initializes it with
    * {@link #initMarshaller(Marshaller)}, and gives it the JAXB object
    * obtained from the given value. If {@link #getNamespacePrefixes()} returns
    * a non-empty map, the supplied handler is wrapped around a
    * {@link RemappingContentHandler} in order to take namespace prefixes into
    * account, and wrapped handler is passed
    * to the marshaller. Otherwise, the content handler is passed directly
    * to the marshaller.
    * 
    * @param object
    *           the object to marshal.
    * @param handler1
    *           the content handler.
    * @throws JAXBException
    *            if an exception occurs during marshalling.
    */
   public void marshal (T object, ContentHandler handler1) throws JAXBException {
      final Marshaller m = getContext ().createMarshaller ();
      if (validating && !schemaReadBug)
         m.setSchema (readSchema ());
      initMarshaller (m);
      final Object el = getJAXBObject (object);
      final Map<String, String> prefixToUri = getNamespacePrefixes ();
      // This tweak is necessary to keep the code independent from
      // the JAXB provider. The Sun JAXB RI defines
      // a property to supply a nemspace prefix mapper,
      // but this property is not available in the JAXB
      // implementation included in Java 6.
      if (prefixToUri == null || prefixToUri.isEmpty ())
         m.marshal (el, handler1);
      else
         m.marshal (el, new RemappingContentHandler (prefixToUri, handler1));
   }

   /**
    * Marshals the given object \texttt{object} to the
    * target set by \texttt{res}.
    * 
    * This method calls {@link #getJAXBObject(Object)} to turn the given object
    * into a JAXB object. If {@link #getNamespacePrefixes()} returns an empty
    * map, this method creates a marshaller the same way as
    * {@link #marshal(Object, ContentHandler)}, and uses it
    * directly to marshal the JAXB
    * object. Otherwise, it creates a {@link TransformerHandler}, and gives it
    * to {@link #marshal(Object, ContentHandler)}.
    * 
    * @param object
    *           the object to marshal.
    * @param res
    *           the result object.
    * @throws JAXBException
    *            if an error occurs during marshalling.
    */
   public void marshal (T object, Result res) throws JAXBException {
      final Map<String, String> prefixToUri = getNamespacePrefixes ();
      if (prefixToUri != null && !prefixToUri.isEmpty ()) {
         TransformerHandler hand = null;
         try {
            final SAXTransformerFactory tFactory = (SAXTransformerFactory) TransformerFactory
                  .newInstance ();
            hand = tFactory.newTransformerHandler ();
            hand.getTransformer ().setOutputProperty (OutputKeys.INDENT, "yes");
            hand.getTransformer ().setOutputProperty (OutputKeys.METHOD, "xml");
            hand.getTransformer ().setOutputProperty (OutputKeys.ENCODING, "utf-8");
            // Xalan-specific, unfortunately
            hand.getTransformer ().setOutputProperty (
                  "{http://xml.apache.org/xalan}indent-amount", "2");
         }
         catch (final TransformerConfigurationException tce) {
            logger
                  .log (
                        Level.WARNING,
                        "Exception occurred when configuring transformer; falling back to default marshalling, without namespace prefix mapping",
                        tce);
         }
         catch (final ClassCastException cce) {
            logger
                  .log (
                        Level.WARNING,
                        "Exception occurred when configuring transformer; falling back to default marshalling, without namespace prefix mapping",
                        cce);
         }
         if (hand != null) {
            hand.setResult (res);
            marshal (object, hand);
            return;
         }
      }

      final Marshaller m = getContext ().createMarshaller ();
      if (validating && !schemaReadBug)
         m.setSchema (readSchema ());
      initMarshaller (m);
      final Object el = getJAXBObject (object);
      m.marshal (el, res);
   }

   /**
    * Marshals the given value object into the given output file. This creates a
    * new {@link StreamResult} with the given file object, and calls
    * {@link #marshal(Object, Result)} to performing marshalling.
    * 
    * @param object
    *           the value to marshal.
    * @param res
    *           the output file.
    * @throws JAXBException
    *            if an error occurs during marshalling.
    */
   public void marshal (T object, File res) throws JAXBException {
      marshal (object, new StreamResult (res));
   }
   
   /**
    * Marshals the given object to the given file,
    * and gzips the marshalled contents.
    * @param object the object to be marshalled.
    * @param res the target file.
    * @throws JAXBException if an error occurs during marshalling.
    */
   public void marshalAndGZip (T object, File res) throws JAXBException {
      try {
         final GZIPOutputStream gz = new GZIPOutputStream (new FileOutputStream (res));
         final StreamResult sr = new StreamResult (gz);
         sr.setSystemId (res);
         marshal (object, sr);
         gz.close ();
      }
      catch (final IOException ioe) {
         final JAXBException je = new JAXBException ("Could not create output file " + res.getName ());
         je.initCause (ioe);
         throw je;
      }
   }

   /**
    * Similar to {@link #marshal(Object, File)}, for a DOM node.
    * 
    * @param object
    *           the value object to marshal.
    * @param node
    *           the output node.
    * @throws JAXBException
    *            if an error occurs during marshalling.
    */
   public void marshal (T object, Node node) throws JAXBException {
      marshal (object, new DOMResult (node));
   }
   
   /**
    * Marshals the object \texttt{object} to
    * the file \texttt{file}, using the
    * {@link #marshal(Object, File)} method, but
    * if an error occurs, this method prints messages
    * on the standard error output, and exits the
    * VM using {@link System#exit(int)}.
    * This method is intended to be used in
    * \texttt{main} methods of command-line
    * programs.
    * @param object the object to marshal.
    * @param file the output file.
    */
   public void marshalOrExit (T object, File file) {
      final ValidationEventCollector col = new ValidationEventCollector();
      final ValidationEventHandler ev = getEventHandler ();
      setEventHandler (col);

      try {
         marshal (object, file);
         showProblems ("marshalling", file, col);
      }
      catch (final MarshalException me) {
         showErrors ("marshalling", file, col, me);
         System.exit (1);
      }
      catch (final JAXBException je) {
         showErrors ("marshalling", file, null, je);
         System.exit (1);
      }
      finally {
         setEventHandler (ev);
      }
   }

   /**
    * Similar to {@link #marshalOrExit(Object, File)},
    * except that the method {@link #marshalAndGZip(Object, File)}
    * is called instead of {@link #marshal(Object, File)}.
    * @param object the object to be marshalled.
    * @param file the output file.
    */
   public void marshalAndGZipOrExit (T object, File file) {
      final ValidationEventCollector col = new ValidationEventCollector();
      final ValidationEventHandler ev = getEventHandler ();
      setEventHandler (col);

      try {
         marshalAndGZip (object, file);
         showProblems ("marshalling", file, col);
      }
      catch (final MarshalException me) {
         showErrors ("marshalling", file, col, me);
         System.exit (1);
      }
      catch (final JAXBException je) {
         showErrors ("marshalling", file, null, je);
         System.exit (1);
      }
      finally {
         setEventHandler (ev);
      }
   }
   
   /**
    * Determines if the given list of validation events
    * contains at least one event representing a warning.
    * This method tests each event of the given list for
    * their severity returned by {@link ValidationEvent#getSeverity()}, and
    * returns \texttt{true} as soon as one event with
    * severity {@link ValidationEvent#WARNING}
    * is found.
    * This returns \texttt{false} if the list is empty,
    * or if it contains only events representing errors
    * or fatal errors.
    * @param evs the tested list.
    * @return \texttt{true} if and only if the given
    * list contains at least one warning event.
    */
   public static boolean hasWarnings (ValidationEvent... evs) {
      for (final ValidationEvent ev : evs)
         if (ev.getSeverity () == ValidationEvent.WARNING)
            return true;
      return false;
   }
   
   /**
    * Similar to {@link #hasWarnings(ValidationEvent[])}
    * for errors instead of warnings.
    * @param evs the tested list.
    * @return \texttt{true} if and only if the given
    * list contains at least one error event.
    */
   public static boolean hasErrors (ValidationEvent... evs) {
      for (final ValidationEvent ev : evs)
         if (ev.getSeverity () == ValidationEvent.ERROR)
            return true;
      return false;
   }
   
   /**
    * Similar to {@link #hasWarnings(ValidationEvent[])}
    * for fatal errors instead of warnings.
    * @param evs the tested list.
    * @return \texttt{true} if and only if the given
    * list contains at least one fatal error event.
    */
   public static boolean hasFatalErrors (ValidationEvent... evs) {
      for (final ValidationEvent ev : evs)
         if (ev.getSeverity () == ValidationEvent.FATAL_ERROR)
            return true;
      return false;
   }
   
   /**
    * Formats and returns a string containing a description
    * for each validation event in the given list.
    * For each event, this method formats
    * a string using {@link #validationEventToString(ValidationEvent)},
    * and separates each event with two newlines.
    * @param evs the list of validation events.
    * @return the formatted string.
    */
   public static String validationEventsToString (ValidationEvent... evs) {
      final Formatter fmt = new Formatter();
      boolean first = true;
      for (final ValidationEvent ev : evs) {
         if (first)
            first = false;
         else
            fmt.format ("%n%n");
         fmt.format ("%s", validationEventToString (ev));
      }
      return fmt.toString ();
   }
   
   /**
    * Constructs and returns a string
    * representing the validation event \texttt{ev}.
    * This string contains the severity (warning, error, or fatal error)
    * of the event,
    * and its descriptive message.
    * It also contains the result of
    * {@link #locatorToString(ValidationEventLocator)}
    * which converts the location information into
    * a string.
    * @param ev the validation event being formatted.
    * @return the formatted string.
    */
   public static String validationEventToString (ValidationEvent ev) {
      final StringBuilder sb = new StringBuilder();
      switch (ev.getSeverity ()) {
      case ValidationEvent.WARNING:
         sb.append ("[WARNING] ");
         break;
      case ValidationEvent.ERROR:
         sb.append ("[ERROR] ");
         break;
      case ValidationEvent.FATAL_ERROR:
         sb.append ("[FATAL ERROR] ");
      }
      sb.append (ev.getMessage ());
      sb.append (" at ");
      sb.append (locatorToString (ev.getLocator ()));
      return sb.toString ();
   }
   
   /**
    * Formats the given locator \texttt{locator}
    * into a string.
    * If the locator specifies a URL,
    * this method returns the URL as well as the
    * line and column.
    * If it contains an object, it
    * returns the result of its
    * \texttt{toString} method.
    * If it specifies a DOM node, the node is formatted
    * to an XPath-like expression.
    * Otherwise, 
    * \texttt{Unknown location} is returned.
    * @param locator
    * @return the string representation of the locator.
    */
   public static String locatorToString (ValidationEventLocator locator) {
      final StringBuilder sb = new StringBuilder();
      if (locator.getURL () != null) {
         sb.append (locator.getURL ().toString ());
         sb.append (", line ");
         sb.append (locator.getLineNumber ());
         sb.append (", column ");
         sb.append (locator.getColumnNumber ());
      }
      else if (locator.getObject () != null)
         sb.append ("JAXB object ").append (locator.getObject ().toString ());
      else if (locator.getNode () != null)
         sb.append ("DOM node").append (DOMUtils.formatNodeName (locator.getNode ()));
      else
         sb.append ("Unknown");
      return sb.toString ();
   }
}
