package umontreal.iro.lecuyer.xmlconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.Introspection;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Constructs a parameter object from an XML document parsed using a DOM parser.
 * For this parameter reader to be used, values must be added to the map
 * {@link #elements} in order to map root elements to class names. The method
 * {@link #read} can be used to construct a new parameter object from a DOM
 * document or an XML file.
 */
public class ParamReader {
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.xmlconfig");
   /**
    * Provides mappings for root elements to Java classes. The keys of the
    * \texttt{elements} map are strings representing XML element names whereas
    * the values are {@link Class} objects corresponding to parameter objects.
    * When an element belongs to a namespace, its corresponding key in the map
    * is given by \emph{namespaceURI}\texttt{/}\emph{tagname}, where
    * \emph{tagname} is the tag name of the element, without the namespace
    * prefix. If the element is not in a namespace, its key name is its tag
    * name. For example, if the root element was given by
    * 
    * \begin{verbatim}
    * 
    * <pr:parameters xmlns:pr="http://www.test.uri">
    * \end{verbatim}
    * 
    * The key name would be \texttt{http://www.test.uri/parameters}. If no
    * namespace is used, the name is \texttt{parameters}.
    */
   public Map<String, Class<? extends Param>> elements = new HashMap<String, Class<? extends Param>> ();

   /**
    * Contains the search path for the {@link #searchFile} method. See the
    * documentation of {@link #searchFile} for more information. The default
    * search path contains a single file referring to \texttt{.}, the current
    * directory.
    */
   public File[] searchPath = new File[] { new File (".") };
   
   /**
    * Contains the base URI used by
    * the {@link #readURL} method.
    * The default base URL corresponds to the current
    * working directory.
    */
   public URI baseURI = getDefaultBaseURI();

   private DocumentBuilder docBuilder = null;
   private final ClassFinder finder = new ClassFinder ();
   private final MethodComparator mcmp = new MethodComparator ();
   private Map<String, Element> idToElements = null;
   private final List<Element> elementStack = new ArrayList<Element> ();
   private boolean useSetAccessible = true;
   
   /**
    * Returns the default base URI, which
    * corresponds to the location
    * of the current directory.
    * @return the default base URL.
    */
   public static URI getDefaultBaseURI() {
      String dir;
      try {
         dir = AccessController.doPrivileged (new PrivilegedAction<String>() {
            public String run () {
               return System.getProperty ("user.dir");
            }
         }); 
      }
      catch (final SecurityException se) {
         dir = ".";
      }
      final File currentDir = new File (dir);
      URI uri;
      try {
         uri = AccessController.doPrivileged (new PrivilegedAction<URI>() {
            public URI run () {
               return currentDir.toURI ();
            }
         });
      }
      catch (final SecurityException se) {
         try {
            return new URI ("file:./");
         }
         catch (final URISyntaxException e) {
            throw new IllegalArgumentException
            ("Cannot create default base URI");
         }
      }
      return uri.normalize ();
   }

   /**
    * Constructs a new parameter reader.
    */
   public ParamReader () {
      finder.getImports ().add ("java.lang.*");
   }

   /**
    * Determines if the parameter reader can use the
    * {@link AccessibleObject#setAccessible(boolean)} when accessing members
    * using Relfection. This reader allows setter, adder, and dispatcher methods
    * in parameter objects to be protected or private. In this case, it uses the
    * \texttt{setAccessible} method to bypass Java access control. However, this
    * can cause problems when using applets or Java Web Start programs. As a
    * result, one can prevent this parameter reader from calling the
    * \texttt{setAccessible} method by calling {@link #setUsingSetAccessible}
    * with \texttt{false}. However, disabling the set-accessible usage flag may
    * result in {@link ParamReadException}s caused by illegal accesses. By
    * default, this returns \texttt{true}.
    * 
    * @return the status of the set-accessible usage flag, default being
    *         \texttt{true}.
    */
   public boolean isUsingSetAccessible () {
      return useSetAccessible;
   }

   /**
    * Sets the set-accessible usage flag to \texttt{useSetAccessible}.
    * 
    * @param useSetAccessible
    *           the new value of the flag.
    * @see #isUsingSetAccessible()
    */
   public void setUsingSetAccessible (boolean useSetAccessible) {
      this.useSetAccessible = useSetAccessible;
   }

   /**
    * Returns the class finder associated with this parameter reader. The import
    * declarations associated with this class finder are obtained from the XML
    * parameter file through \texttt{import} processing instructions. For each
    * parsed element, {@link ClassFinder#saveImports} is used at the beginning
    * of processing and {@link ClassFinder#restoreImports} is used at the end.
    * For each processing instruction \verb!<?import name?>!, \texttt{name} is
    * added to the list returned by {@link ClassFinder#getImports}.
    * 
    * @return the class finder associated to this parameter reader.
    */
   public ClassFinder getClassFinder () {
      return finder;
   }

   private void initIdToElementsMap (Element el) {
      if (idToElements == null)
         idToElements = new HashMap<String, Element> ();
      else
         idToElements.clear ();
      addIds (el);
   }

   private void addIds (Element el) {
      final String id = el.getAttribute ("id");
      if (id != null && id.length () > 0) {
         final Element ref = idToElements.get (id);
         if (ref != null)
            throw new ParamReadException ("Elements " + el.getNodeName ()
                  + " and " + ref.getNodeName ()
                  + " have the same id attribute " + id);
         idToElements.put (id, el);
      }
      final NodeList nl = el.getChildNodes ();
      for (int i = 0; i < nl.getLength (); i++) {
         final Node c = nl.item (i);
         if (c instanceof Element)
            addIds ((Element) c);
      }
   }

   private void clearIdToElementsMap () {
      idToElements = null;
   }

   /**
    * Returns the element, in the currently read document, having an \texttt{id}
    * attribute with value \texttt{id}. If no such element exists, \texttt{null}
    * is returned.
    * 
    * @param id
    *           the identifier of the element.
    * @return the corresponding element.
    */
   public Element getElementById (String id) {
      if (idToElements == null)
         return null;
      return idToElements.get (id);
   }

   /**
    * Reads the parameter object given
    * by the file \texttt{fileName}.
    * Uses {@link #searchFile} to find an existing file with the name
    * \texttt{fileName} on the current search path, and passes this file to the
    * {@link #read(File)} method. With the default search path, this looks in
    * the current directory only.
    * 
    * @param fileName
    *           the file name to be parsed.
    * @return the constructed parameter object.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param readFile (String fileName) throws IOException,
         ParserConfigurationException, SAXException {
      final File f = searchFile (fileName);
      return read (f);
   }
   
   /**
    * Reads the parameter object given
    * by the URL \texttt{url}.
    * Uses {@link URI#resolve}
    * to resolve \texttt{url} against
    * {@link #baseURI}, converts the
    * resulting URI into a URL,
    * and give the resulting URL
    * to {@link #read(URL)}.  
    * @param url the relative URI.
    * @return the parameter object.
    * @throws IOException if an I/O exception occurs during parameter reading.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param readURL (String url) throws IOException,
   ParserConfigurationException, SAXException {
      URL urlObject;
      if (baseURI == null)
         urlObject = new URL (url);
      else
         urlObject = baseURI.resolve (url).toURL ();
      return read (urlObject);
   }
   
   /**
    * Uses {@link #searchFile} to find an existing file with the name
    * \texttt{fileName} on the current search path, and passes this file to the
    * {@link #read(File)} method. With the default search path, this looks in
    * the current directory only. 
    * @param fileName the file name
    * @return the constructed parameter object.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    * @deprecated Use {@link #readFile} instead.
    */
   @Deprecated
   public Param read (String fileName) throws IOException,
   ParserConfigurationException, SAXException {
      return readFile (fileName);
   }

   /**
    * Reads a parameter object from the XML file \texttt{filaName}. If the given
    * file is found, the method adds its parent directory to the search path,
    * and sets it as the base URI. It
    * then creates an XML parser using JAXP, parses the XML file and passes the
    * created DOM document to {@link #read (Documnet)}. The
    * {@link DocumentBuilder} instance used to parse the XML files is created
    * only once for each instance of parameter reader, by the
    * {@link #getDocumentBuilder} method. After this process, the method
    * restores the original search path.
    * 
    * @param fileName
    *           the file name to be parsed.
    * @return the constructed parameter object.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param read (File fileName) throws IOException,
         ParserConfigurationException, SAXException {
      final DocumentBuilder docBuilder1 = getDocumentBuilder ();
      final Document doc = docBuilder1.parse (fileName);

      final File[] oldSearchPath = searchPath;
      final URI oldBaseURI = baseURI;
      searchPath = addToSearchPath (fileName);
      try {
         baseURI = fileName.toURI ();
      }
      catch (final SecurityException se) {
         logger.log (Level.WARNING, "Could not obtain the base URI from the given file name", se);
      }

      try {
         return read (doc);
      }
      finally {
         searchPath = oldSearchPath;
         baseURI = oldBaseURI;
      }
   }
   
   private File[] addToSearchPath (File fileName) {
      final File pf = fileName.getParentFile ();
      if (searchPath == null || searchPath.length == 0)
         return new File[] { pf };
      else {
         boolean pfFound = false;
         for (int i = 0; i < searchPath.length && !pfFound; i++)
            if (searchPath[i] != null && searchPath[i].equals (pf))
               pfFound = true;
         if (!pfFound) {
            final File[] newSearchPath = new File[searchPath.length + 1];
            System.arraycopy (searchPath, 0, newSearchPath, 0,
                  searchPath.length);
            newSearchPath[searchPath.length] = pf;
            return newSearchPath;
         }
      }
      return searchPath;
   }

   /**
    * Returns a file object corresponding to an existing file with name
    * \texttt{fileName} by looking on the current search path. If the given file
    * name is an absolute path, or the search path stored in the
    * {@link #searchPath} field is \texttt{null} or has length 0, this method
    * creates a file object from the given file name. If that file exists, i.e.,
    * if \texttt{fileName} is a valid relative or absolute path pointing to an
    * existing file, this method returns the file object. Otherwise, for each
    * non-\texttt{null} element \texttt{p} of the {@link #searchPath} array,
    * this method makes a file object with parent \texttt{p} and name
    * \texttt{fileName}. It returns the first file object referring to an
    * existing file. If no file with the given name can be found on the search
    * path, this method throws a {@link FileNotFoundException}.
    * 
    * @param fileName
    *           the name of the file to search for.
    * @return the found file.
    * @throws FileNotFoundException
    *            if the file cannot be found.
    */
   public File searchFile (String fileName) throws FileNotFoundException {
      File f = new File (fileName);
      if (f.isAbsolute () || searchPath == null || searchPath.length == 0) {
         if (f.exists ())
            return f;
      }
      else
         for (final File element : searchPath) {
            if (element == null)
               continue;
            f = new File (element, fileName);
            if (f.exists ())
               return f;
         }
      throw new FileNotFoundException ("Cannot find a file with name "
            + fileName + " on the search path");
   }
   
   /**
    * Reads a parameter object from the XML file located at
    * URL \texttt{url}. The method
    * sets the given URL as the base URI. 
    * If the given
    * URL corresponds to a file, the method also adds its parent directory to the search path. It
    * then creates an XML parser using JAXP, parses the XML file and passes the
    * created DOM document to {@link #read (Documnet)}. The
    * {@link DocumentBuilder} instance used to parse the XML files is created
    * only once for each instance of parameter reader, by the
    * {@link #getDocumentBuilder} method. After this process, the method
    * restores the original search path.
    * 
    * @param url
    *           the URL pointing to the file to be parsed.
    * @return the constructed parameter object.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param read (URL url) throws IOException,
         ParserConfigurationException, SAXException {
      final InputStream is = url.openStream ();
      final DocumentBuilder docBuilder1 = getDocumentBuilder ();
      final Document doc = docBuilder1.parse (is);
      is.close ();

      final URI oldBaseURI = baseURI;
      final File[] oldSearchPath = searchPath;
      URI uri = null;
      try {
         baseURI = uri = url.toURI();
      }
      catch (final URISyntaxException e) {
         logger.log (Level.WARNING, "Could not get the base URI from the given URL", e);
      }
      if (uri != null && uri.getScheme ().equals ("file")) {
         final File fileName = new File (uri);
         searchPath = addToSearchPath (fileName);
      }

      try {
         return read (doc);
      }
      finally {
         searchPath = oldSearchPath;
         baseURI = oldBaseURI;
      }
   }

   /**
    * This is similar to {@link #read(String)}, but it reads the XML document
    * from the stream \texttt{stream} instead of from a file.
    * 
    * @param stream
    *           the stream to read the XML document from.
    * @return the constructed parameter object.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    * @exception IOException
    *               if an I/O error occurs.
    * @exception SAXException
    *               if a parse error occurs.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param read (InputStream stream) throws IOException,
         ParserConfigurationException, SAXException {
      final DocumentBuilder docBuilder1 = getDocumentBuilder ();
      final Document doc = docBuilder1.parse (stream);
      return read (doc);
   }

   /**
    * Returns the document builder instance associated with this instance of
    * parameter reader. If no document builder is associated yet, one is
    * created. This coalescent document builder is configured to expand
    * entities, ignore white spaces and is not validating. See
    * {@link DocumentBuilderFactory} for more information.
    * 
    * @return the associated document builder.
    * @exception ParserConfigurationException
    *               if the parser could not be configured properly.
    */
   public DocumentBuilder getDocumentBuilder ()
         throws ParserConfigurationException {
      if (docBuilder == null) {
         final DocumentBuilderFactory factory = DocumentBuilderFactory
               .newInstance ();
         factory.setExpandEntityReferences (true);
         factory.setIgnoringElementContentWhitespace (true);
         factory.setValidating (false);
         factory.setCoalescing (true);
         docBuilder = factory.newDocumentBuilder ();
      }
      return docBuilder;
   }

   /**
    * Reads a DOM document \texttt{doc} and constructs a parameter object based
    * on its contents. If some problems occur during the extraction of
    * parameters, a {@link ParamReadException} is thrown.
    * 
    * When given an XML document, the method maps the root element name to a
    * Java {@link Class} object by using {@link #elements}.
    * 
    * Before the parameter object is created, the given document is scanned for
    * elements with non-empty \texttt{id} attributes. These elements are put
    * into an internal map that is cleared before this method ends. Each time an
    * element with a \texttt{xref} attribute is found, it is automatically
    * replaced with an element having the corresponding \texttt{id} attribute.
    * The parameter reader keeps a stack of currently-processed elements to
    * avoid infinite loops during this reference resolution process.
    * 
    * The {@link #createParameterObject(Class,Element)} method is then used to
    * construct the parameter object from the XML element.
    * 
    * @param doc
    *           the DOM document being read.
    * @return the constructed parameter object.
    * @exception ParamReadException
    *               if an extraction error occurs.
    * @exception ClassCastException
    *               if {@link #elements} contains a value which is not of class
    *               {@link Class}.
    */
   public Param read (Document doc) {
      final Element docEl = doc.getDocumentElement ();

      // Try to find a class associated with the element
      // to be processed.
      Class<? extends Param> c = null;
      final String nuri = docEl.getNamespaceURI ();
      String tn = docEl.getLocalName ();
      if (tn == null)
         tn = docEl.getTagName ();
      final String eln = nuri == null ? tn : nuri + "/" + tn;
      c = elements.get (eln);
      if (c == null)
         throw new ParamReadException ("Could not find a mapping to the " + eln
               + " element");
      final NodeList nl = doc.getChildNodes ();
      List<String> imp = null;
      try {
         for (int i = 0; i < nl.getLength (); i++) {
            final Node node = nl.item (i);
            if (node instanceof ProcessingInstruction) {
               final ProcessingInstruction pi = (ProcessingInstruction) node;
               if (pi.getTarget ().equals ("import")) {
                  if (imp == null) {
                     finder.saveImports ();
                     imp = finder.getImports ();
                  }
                  imp.add (pi.getData ());
               }
            }
         }
         initIdToElementsMap (docEl);
         return createParameterObject (c, docEl);
      }
      finally {
         if (imp != null)
            finder.restoreImports ();
         clearIdToElementsMap ();
         elementStack.clear ();
         ExcelSourceArray2D.clearCache();
      }
   }

   private static boolean isAccessibleFrom (Member source, Member target) {
      Class<?> srcClass = source.getDeclaringClass ();
      Class<?> targetClass = target.getDeclaringClass ();
      if (srcClass == targetClass)
         return true;
      while (srcClass.getDeclaringClass () != null)
         srcClass = srcClass.getDeclaringClass ();
      while (targetClass.getDeclaringClass () != null)
         targetClass = targetClass.getDeclaringClass ();
      if (srcClass == targetClass)
         return true;
      final int mod = target.getModifiers ();
      final int modClass = target.getDeclaringClass ().getModifiers ();
      if (Modifier.isPublic (mod) && Modifier.isPublic (modClass))
         return true;
      if (Modifier.isPrivate (mod) || Modifier.isPrivate (modClass))
         return false;
      if (srcClass.getPackage () == targetClass.getPackage ())
         return true;
      if (Modifier.isProtected (mod) && Modifier.isPublic (modClass))
         if (targetClass.isAssignableFrom (srcClass))
            return true;
      return false;
   }

   /**
    * Constructs a new parameter object of class \texttt{c} from the DOM element
    * \texttt{el}. The method first tries to construct the parameter object by
    * calling one of the four type of constructors, in that order:
    * \texttt{(ParamReader, Element)}, \texttt{(ParamReader)},
    * \texttt{(Element)}, and \texttt{()}. The constructors receiving the
    * processed element allow a class to override the automatic parameter
    * extraction. Otherwise, the method uses
    * {@link #processElement(Element,Param)} to perform the processing if the
    * call to the constructor succeeds. If the construction of the object fails,
    * a {@link ParamReadException} is thrown.
    * 
    * @param c
    *           the target class of the parameter object.
    * @param el
    *           the element the parameters are extracted from.
    * @return the constructed parameter object.
    * @exception ParamReadException
    *               if an error occurs during parameter extraction.
    */
   public <T extends Param> T createParameterObject (Class<T> c, Element el) {
      if (!Param.class.isAssignableFrom (c))
         throw new ParamReadException (el,
               "Cannot construct a parameter element from a class not implementing "
                     + Param.class.getName ());

      Method thisMethod;
      try {
         thisMethod = getClass ().getMethod ("createParameterObject",
               Class.class, Element.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (el,
               "Could not find the Method object for ParamReader.createParameterObject");
      }

      // First, we try to make an instance of the element object
      // with a constructor taking an Element argument
      boolean process = true;
      boolean acc = true;
      T o = null;
      for (int i = 0; i < 4 && o == null; i++) {
         Constructor<T> ctor = null;
         try {
            // Manual construction of an element
            switch (i) {
            case 0:
               try {
                  ctor = c.getDeclaredConstructor (ParamReader.class,
                        Element.class);
               }
               catch (final SecurityException se) {
                  ctor = c.getConstructor (ParamReader.class, Element.class);
               }
               break;
            case 1:
               try {
                  ctor = c.getDeclaredConstructor (ParamReader.class);
               }
               catch (final SecurityException se) {
                  ctor = c.getConstructor (ParamReader.class);
               }
               break;
            case 2:
               try {
                  ctor = c.getDeclaredConstructor (Element.class);
               }
               catch (final SecurityException se) {
                  ctor = c.getConstructor (Element.class);
               }
               break;
            case 3:
               try {
                  ctor = c.getDeclaredConstructor ();
               }
               catch (final SecurityException se) {
                  ctor = c.getConstructor ();
               }
               break;
            default:
               throw new IllegalStateException ();
            }
            acc = !useSetAccessible || isAccessibleFrom (thisMethod, ctor);
            if (!acc)
               ctor.setAccessible (true);
            switch (i) {
            case 0:
               o = ctor.newInstance (el, this);
               process = false;
               break;
            case 1:
               o = ctor.newInstance (this);
               break;
            case 2:
               o = ctor.newInstance (el);
               process = false;
               break;
            case 3:
               o = ctor.newInstance ();
               break;
            default:
               throw new IllegalStateException ();
            }
         }
         catch (final NoSuchMethodException nse) {
            if (i == 3)
               throw new ParamReadException (el, "The class " + c.getName ()
                     + " must contain an appropriate constructor");
         }
         catch (final IllegalAccessException iae) {
            throw new ParamReadException (el,
                  "Illegal access while calling constructor " + ctor.getName ());
         }
         catch (final InstantiationException ie) {
            throw new ParamReadException (el,
                  "Error constructing the parameter object with constructor "
                        + ctor.getName () + ": " + ie.getMessage ());
         }
         catch (final InvocationTargetException ite) {
            handleInvocationTargetException (el, ite,
                  "Exception occurred during parameter object construction");
         }
         finally {
            if (ctor != null && !acc)
               ctor.setAccessible (false);
         }
      }

      if (process)
         processElement (el, o);
      return o;
   }

   /**
    * Configures the parameter object \texttt{o} by processing the DOM element
    * \texttt{el}. To achieve this result with arbitrary objects, the method
    * assumes the parameter objects respect some design patterns summarized in
    * tables~\ref{tab:varpobj} and~\ref{tab:pobjects}. These patterns specify
    * methods one can implement to interact with the parameter reader.
    * 
    * These methods are not specified in the {@link Param} interface because
    * their signatures are incomplete or they are optional. Some method names
    * depend on the attribute or nested element name and the argument type
    * directs the conversion from string.
    * 
    * Some of the possible methods can take an optional URI argument which
    * corresponds to the namespace URI of the attribute or nested element being
    * set, created or added, or \texttt{null} if no namespace is used. If a
    * namespace URI is incorrect, a tester method can return \texttt{false} and
    * other methods can throw a {@link ParamReadException}.
    * 
    * \begin{table}[htb] \begin{center} \begin{tabular}{|l|p{8cm}|}\hline
    * Name\html{$\mbox{}$}&Role\\\hline\hline \texttt{uri}&Namespace URI of
    * attribute or nested element\\\hline \texttt{lname}&Local name of attribute
    * or nested element\\\hline \emph{class}&Name of a class or primitive
    * type\\\hline \emph{pclass}&Name of a class implementing {@link Param}\\\hline
    * \texttt{value}&The value converted from string\\\hline \texttt{el}&The
    * element being processed\\\hline \texttt{cel}&The nested (or child)
    * element\\\hline \texttt{attr}&An attribute in \texttt{el}\\\hline
    * \end{tabular} \end{center} \caption{Variables used in parameter object
    * methods} \label{tab:varpobj} \end{table}
    * 
    * \begin{table}[htb] \begin{center} \begin{tabular}{|l|p{7cm}|p{6cm}|}\hline
    * Method\html{$\mbox{}$}&Design pattern&Role\\\hline\hline
    * Tester&\texttt{boolean isAttributeSupported ([ParamReader r, ][String uri,
    * ]String lname)}& Determines if an attribute is supported\\\hline
    * Setter&\texttt{void set}\emph{Attr}\texttt{ ([ParamReader r, ][String uri,
    * ]}\emph{class}\texttt{ value)}& Sets \emph{attr} to \texttt{value}\\\hline
    * Tester&\texttt{boolean isNestedElementSupported ([ParamReader r, ][String
    * uri, ]String lname)}& Determines if a nested element is supported\\\hline
    * Adder&\texttt{void add}\emph{Nested}\texttt{ ([ParamReader r, ][String
    * uri, ]}\emph{class}\texttt{ paramobj)}& Adds processed nested element
    * \emph{nested}\\\hline Creater&\emph{pclass}\texttt{
    * create}\emph{Nested}\texttt{ ([ParamReader r, ][String uri|Element cel])}&
    * Creates the nested element \emph{nested}\\\hline Text&\texttt{void
    * nestedText ([ParamReader r, ]}\emph{class}\texttt{ value)}& Adds character
    * data\\\hline Dispatcher&\texttt{void defaultSetAttribute ([ParamReader r,
    * ]Attr attr)}& Fallback method for attributes with no setter method\\\hline
    * Dispatcher&\texttt{void defaultNestedElement ([ParamReader r, ]Element
    * cel)}& Fallback method for elements with no adder or creater
    * methods\\\hline Ender&\texttt{void finishReading ([ParamReader r, ]Element
    * el)}& Terminates the reading\\\hline \end{tabular} \end{center}
    * \caption{Summary of design patterns for parameter objects}
    * \label{tab:pobjects} \end{table}
    * 
    * If the class of the parameter object defines several setter, adder or
    * creater methods having the same name, the reader selects a single method
    * to call. The methods of the parameter object are sorted using the
    * comparator {@link MethodComparator} and the sorted array of methods is
    * searched linearly, the first appropriate method being selected. This
    * comparator implements an heuristic to place the most specialized methods
    * first. In particular, the method with the greatest number of arguments and
    * supported by the reader is taken. If more than one methods have the same
    * greatest number of arguments, some argument types have priority over other
    * types. \texttt{ParamReader}, {@link Param}, and {@link Node} have high
    * priority whereas arrays have low priority.
    * 
    * For each attribute with local name \texttt{attr}, a suport test is
    * performed. If the parameter object class defines a
    * \texttt{isAttributeSupported} method returning a boolean value, this
    * method is called to test \texttt{attr}. If it returns \texttt{false}, an
    * exception is thrown. If it returns \texttt{true} or the method does not
    * exist, the attribute processing continues. After the support test, the
    * method tries to call an appropriate setter method in the parameter object.
    * The {@link StringConvert#fromString(URI,ClassFinder,Class, String)} method is given the string value of the
    * attribute, and the result is given to the setter method. If no setter
    * method is provided for a given attribute, the parameter reader tries to
    * call a dispatcher method named \texttt{defaultSetAttribute}. If no setter
    * or dispatcher method can be called to set a given attribute, the reader
    * terminates with a {@link ParamReadException}.
    * 
    * After the attributes are set, nested elements can be constructed and added
    * to the parameter object. For nested text, a \texttt{nestedText} method is
    * looked for. The argument of \texttt{nestedText} can be of any type
    * provided that {@link StringConvert#fromString(URI,ClassFinder,Class, String)} is capable of converting it
    * from {@link String}.
    * 
    * A nested element with local name \texttt{nested} is processed using an
    * adder or a creater method. As with attributes, a support test is performed
    * using \texttt{isNestedElementSupported}, if it exists. Often, a nested
    * element may contain nested text only. For such elements, if the
    * \texttt{paramobj} argument does not correspond to a class implementing
    * {@link Param}, the parameter reader uses {@link StringConvert#fromString(URI,ClassFinder,Class, String)} to
    * convert the nested text into an object for the adder method. Otherwise,
    * the class of \texttt{paramobj} must implement {@link Param}. The
    * {@link #createParameterObject} method is called recursively to obtain a
    * parameter object which is passed to the adder method. The class of the
    * nested parameter object depends on the argument of the adder method.
    * 
    * If the nested element cannot be constructed by a single-argument or a
    * no-argument constructor, the adder method can be replaced by a creater
    * method of the form \texttt{createNested}. The method must construct and
    * return the parameter object, possibly after configuring it. When the
    * traversed element is received, the automatic extraction process is
    * bypassed. If both an adder and creater methods are present with the same
    * argument type, the creater method is called first and the adder method is
    * called with the processed parameter object which was constructed using the
    * creater method.
    * 
    * If no adder or creater methods are available to process a nested element,
    * the method tries to call a dispatcher method with name If no adder,
    * creater or dispatcher method can process a given element, a
    * {@link ParamReadException} is thrown.
    * 
    * When all contents is processed, the method looks for a
    * \texttt{finishReading} method in the parameter object. If such a method is
    * found, it is called with \texttt{el} as an argument. This method can be
    * used to finalize the parameter reading and processing.
    * 
    * @param el
    *           the element being processed.
    * @param o
    *           the parameter object being defined.
    * @exception ParamReadException
    *               if a problem occurs during the extraction of parameters.
    */
   public void processElement (Element el, Param o) {
      // For each attribute of the element, we try to find a setter method
      if (elementStack.contains (el))
         throw new ParamReadException (el,
               "Circular dependency of elements found");
      elementStack.add (el);
      try {
         final String xref = el.getAttribute ("xref");
         if (xref != null && xref.length () > 0) {
            final Element refEl = getElementById (xref);
            if (refEl == null)
               throw new ParamReadException (el,
                     "This element references an element with id " + xref
                           + ", which does not exist");
            processElement (refEl, o);
            return;
         }
         final Class<?> c = o.getClass ();
         Method[] mt;
         try {
            mt = Introspection.getMethods (c);
         }
         catch (final SecurityException e) {
            mt = c.getMethods ();
         }
         Arrays.sort (mt, mcmp);
         processAttributes (el, o, mt);

         final NodeList nl = el.getChildNodes ();
         List<String> imp = null;
         try {
            for (int ch = 0; ch < nl.getLength (); ch++) {
               final Node n = nl.item (ch);
               if (n instanceof Element)
                  processNested (el, o, mt, (Element) n);
               else if (n instanceof Text)
                  processText (el, o, mt, (Text) n);
               else if (n instanceof ProcessingInstruction) {
                  final ProcessingInstruction pi = (ProcessingInstruction) n;
                  if (pi.getTarget ().equals ("import")) {
                     if (imp == null) {
                        finder.saveImports ();
                        imp = finder.getImports ();
                     }
                     imp.add (pi.getData ());
                  }
               }
               // Skip comments and processing instructions
            }
            finish (el, o);
         }
         finally {
            if (imp != null)
               finder.restoreImports ();
         }
      }
      finally {
         elementStack.remove (el);
      }
   }
   
   private URI getBaseURI (Element el) {
      final String elementBaseURI = el.getBaseURI ();
      if (elementBaseURI == null)
         return baseURI;
      else {
         URI baseURI1;
         try {
            baseURI1 = new URI (elementBaseURI);
         }
         catch (final URISyntaxException use) {
            logger.log (Level.WARNING, "Cannot create base URI for element", use);
            return null;
         }
         if (!baseURI1.isAbsolute ())
            return this.baseURI;
         return baseURI1;
      }
   }

   private void processAttributes (Element el, Param o, Method[] mt) {
      final URI baseURI1 = getBaseURI (el);
      final NamedNodeMap attrs = el.getAttributes ();
      for (int a = 0; a < attrs.getLength (); a++) {
         final Node n = attrs.item (a);
         if (!(n instanceof Attr))
            continue;
         if (!isSupported (o, n))
            throw new ParamReadException (n, "Attribute " + n.getNodeName ()
                  + " not supported");
         final Attr attr = (Attr) n;
         final String methodName = getMethodName ("set", attr);
         boolean attrSet = false;
         ParamReadException e = null;
         for (int i = 0; i < mt.length && !attrSet; i++)
            if (mt[i].getName ().equals (methodName) && isSetterAdder (mt[i])) {
               final Class<?>[] par = mt[i].getParameterTypes ();
               try {
                  if (tryToCallMethod (attr, mt[i], o, StringConvert.fromString (
                        baseURI1, finder, (Class<?>) par[par.length - 1], attr
                              .getValue ())))
                     attrSet = true;
               }
               catch (final UnsupportedConversionException uce) {
                  e = new ParamReadException (attr);
                  e.initCause (uce);
               }
               catch (final NameConflictException nce) {
                  e = new ParamReadException (attr);
                  e.initCause (nce);
               }
               catch (final IllegalArgumentException iae) {
                  e = new ParamReadException (attr);
                  e.initCause (iae);
               }
            }
         if (!attrSet) {
            if (e != null)
               throw e;
            // Call the default dispatcher method
            if (!tryToCallAttributeDispatcher (el, o, attr))
               throw new ParamReadException (attr,
                     "Could not set the attribute, no appropriate "
                           + methodName
                           + " or defaultSetAttribute methods found " + " in "
                           + o.getClass ().getName ());
         }
      }
   }

   @SuppressWarnings("all")
   private boolean tryToCallAttributeDispatcher (Element el, Param o, Attr attr) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod (
               "tryToCallAttributeDispatcher", Element.class, Param.class,
               Attr.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (el,
               "Could not find the Method object for ParamReader.tryToCallAttributeDispatcher");
      }

      final Class<?> c = o.getClass ();
      Method dispatcher = null;
      int flag;
      for (flag = 0; flag < 2 && dispatcher == null; flag++) {
         Class<?>[] pt;
         switch (flag) {
         case 0:
            pt = new Class<?>[] { ParamReader.class, Attr.class };
            break;
         case 1:
            pt = new Class<?>[] { Attr.class };
            break;
         default:
            throw new IllegalStateException ();
         }
         try {
            dispatcher = Introspection.getMethod (c, "defaultSetAttribute", pt);
         }
         catch (final NoSuchMethodException nme) {}
         catch (final SecurityException s) {
            try {
               dispatcher = c.getMethod ("defaultSetAttribute", pt);
            }
            catch (final NoSuchMethodException nme) {}
         }
         if (dispatcher != null && dispatcher.getReturnType () != void.class)
            dispatcher = null;
      }
      if (dispatcher == null)
         return false;
      else {
         Object[] par;
         switch (flag) {
         case 1:
            par = new Object[] { this, attr };
            break;
         case 2:
            par = new Object[] { attr };
            break;
         default:
            throw new IllegalStateException ();
         }
         boolean acc = !useSetAccessible
               || isAccessibleFrom (thisMethod, dispatcher);
         try {
            if (!acc)
               dispatcher.setAccessible (true);
            dispatcher.invoke (o, par);
         }
         catch (final IllegalAccessException iae) {
            throw new ParamReadException (attr,
                  "Illegal access while calling method "
                        + dispatcher.getName ());
         }
         catch (final InvocationTargetException ite) {
            handleInvocationTargetException (attr, ite,
                  "Exception occurred during call to attribute dispatcher");
         }
         finally {
            if (!acc)
               dispatcher.setAccessible (false);
         }
      }
      return true;
   }

   @SuppressWarnings ("unchecked")
   private void processNested (Element el, Param o, Method[] mt, Element cel) {
      if (!isSupported (o, cel))
         throw new ParamReadException (cel, "Nested element "
               + cel.getNodeName () + " not supported");
      final URI baseURI1 = getBaseURI (el);
      final String cmethodName = getMethodName ("create", cel);
      Param eo = null;
      ParamReadException e = null;
      for (int i = 0; i < mt.length && eo == null; i++)
         if (mt[i].getName ().equals (cmethodName) && isCreater (mt[i]))
            // Call the creater method
            try {
               eo = createParamObject (cel, mt[i], o);
            }
            catch (final ParamReadException pre) {
               e = pre;
               if (e.node == null)
                  e.node = cel;
            }
      if (e != null && eo == null)
         throw e;

      e = null;
      final String amethodName = getMethodName ("add", cel);
      boolean elementAdded = false;
      for (int i = 0; i < mt.length && !elementAdded; i++)
         if (mt[i].getName ().equals (amethodName) && isSetterAdder (mt[i])) {
            final Class[] pt = mt[i].getParameterTypes ();
            try {
               // Call the adder method
               final Class<?> pcls = pt[pt.length - 1];
               if (eo == null) {
                  Object arg = null;
                  boolean argRead = false;
                  if (Param.class.isAssignableFrom (pcls)) {
                     arg = createParameterObject (
                           (Class<? extends Param>) pcls, cel);
                     argRead = true;
                  }
                  else {
                     Element testEl = cel;
                     final int s = elementStack.size ();
                     try {
                        String xref;
                        while ((xref = testEl.getAttribute ("xref")) != null
                              && xref.length () > 0) {
                           final Element refEl = getElementById (xref);
                           if (refEl == null)
                              throw new ParamReadException (testEl,
                                    "This element refers to an "
                                          + "element with id " + xref
                                          + ", which cannot be found");
                           if (elementStack.contains (refEl))
                              throw new ParamReadException (refEl,
                                    "Circular dependency of elements detected");
                           elementStack.add (refEl);
                           testEl = refEl;
                        }
                     }
                     finally {
                        while (elementStack.size () > s)
                           elementStack.remove (elementStack.size () - 1);
                     }
                     final NodeList nlc = cel.getChildNodes ();
                     final NamedNodeMap attrc = cel.getAttributes ();
                     if (nlc.getLength () == 1)
                        if (attrc.getLength () == 0
                              || attrc.getLength () == 1 && cel
                                    .hasAttribute ("id")) {
                           final Node tn = nlc.item (0);
                           if (tn instanceof Text) {
                              arg = StringConvert.fromString (baseURI1, finder, pcls, tn
                                    .getNodeValue ());
                              argRead = true;
                           }
                        }
                  }
                  if (argRead && tryToCallMethod (cel, mt[i], o, arg))
                     elementAdded = true;
               }
               else if (pcls.isAssignableFrom (eo.getClass ()))
                  if (tryToCallMethod (cel, mt[i], o, eo))
                     elementAdded = true;
            }
            catch (final UnsupportedConversionException uce) {
               e = new ParamReadException (cel);
               e.initCause (uce);
            }
            catch (final NameConflictException nce) {
               e = new ParamReadException (cel);
               e.initCause (nce);
            }
            catch (final IllegalArgumentException iae) {
               e = new ParamReadException (cel);
               e.initCause (iae);
            }
         }
      if (e != null && (eo == null || !elementAdded))
         throw e;
      if (eo == null && !elementAdded)
         // Call the nested element dispatcher method
         if (!tryToCallElementDispatcher (el, o, cel))
            throw new ParamReadException (cel,
                  "Could not add the subelement, no appropriate adder method "
                        + amethodName + ", creater method " + cmethodName
                        + " or dispatcher method defaultNestedElement"
                        + " found in class " + o.getClass ().getName ());
   }

   @SuppressWarnings("all")
   private boolean tryToCallElementDispatcher (Element el, Param o, Element cel) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod (
               "tryToCallElementDispatcher", Element.class, Param.class,
               Element.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (el,
               "Could not find the Method object for ParamReader.tryToCallElementDispatcher");
      }

      final Class<?> c = o.getClass ();
      Method dispatcher = null;
      int flag;
      for (flag = 0; flag < 2 && dispatcher == null; flag++) {
         Class<?>[] pt;
         switch (flag) {
         case 0:
            pt = new Class<?>[] { ParamReader.class, Element.class };
            break;
         case 1:
            pt = new Class<?>[] { Element.class };
            break;
         default:
            throw new IllegalStateException ();
         }
         try {
            dispatcher = Introspection
                  .getMethod (c, "defaultNestedElement", pt);
         }
         catch (final NoSuchMethodException nme) {}
         catch (final SecurityException s) {
            try {
               dispatcher = c.getMethod ("defaultNestedElement", pt);
            }
            catch (final NoSuchMethodException nme) {}
         }
         if (dispatcher != null && dispatcher.getReturnType () != void.class)
            dispatcher = null;
      }
      if (dispatcher == null)
         return false;
      else {
         Object[] par;
         switch (flag) {
         case 1:
            par = new Object[] { this, cel };
            break;
         case 2:
            par = new Object[] { cel };
         default:
            throw new IllegalStateException ();
         }
         boolean acc = !useSetAccessible
               || isAccessibleFrom (thisMethod, dispatcher);
         try {
            if (!acc)
               dispatcher.setAccessible (true);
            dispatcher.invoke (o, par);
         }
         catch (final IllegalAccessException iae) {
            throw new ParamReadException (cel,
                  "Illegal access while calling method "
                        + dispatcher.getName ());
         }
         catch (final InvocationTargetException ite) {
            handleInvocationTargetException (cel, ite,
                  "Exception occurred during call to the nested element method");
         }
         finally {
            if (!acc)
               dispatcher.setAccessible (false);
         }
      }
      return true;
   }

   private void processText (Element el, Param o, Method[] mt, Text txt) {
      if (txt.getData ().matches ("\\s*"))
         return;
      final URI baseURI1 = getBaseURI (el);
      // In Ant tasks, this is called addText, but
      // it prevents one from defining a text nested
      // element.
      final String methodName = "nestedText";
      boolean textAdded = false;
      ParamReadException e = null;
      for (int i = 0; i < mt.length && !textAdded; i++)
         if (mt[i].getName ().equals (methodName) && isNestedText (mt[i])) {
            final Class<?>[] par = mt[i].getParameterTypes ();
            try {
               if (tryToCallMethod (txt, mt[i], o, StringConvert.fromString (
                     baseURI1, finder, (Class<?>) par[par.length - 1], txt.getData ())))
                  textAdded = true;
            }
            catch (final UnsupportedConversionException uce) {
               e = new ParamReadException (txt);
               e.initCause (uce);
            }
            catch (final NameConflictException nce) {
               e = new ParamReadException (txt);
               e.initCause (nce);
            }
            catch (final IllegalArgumentException iae) {
               e = new ParamReadException (txt);
               e.initCause (iae);
            }
         }
      if (!textAdded)
         throw e == null ? new ParamReadException (txt,
               "Could not add the character data"
                     + ", no appropriate nestedText method found in class "
                     + o.getClass ().getName ()) : e;
   }

   @SuppressWarnings("all")
   private void finish (Element el, Param o) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod ("finish", Element.class,
               Param.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (el,
               "Could not find the Method object for ParamReader.finish");
      }

      final Class<?> c = o.getClass ();
      Method finish = null;
      int flag;
      for (flag = 0; flag < 2 && finish == null; flag++) {
         Class<?>[] pt;
         switch (flag) {
         case 0:
            pt = new Class<?>[] { ParamReader.class, Element.class };
            break;
         case 1:
            pt = new Class<?>[] { Element.class };
            break;
         default:
            throw new IllegalStateException ();
         }
         try {
            finish = Introspection.getMethod (c, "finishReading", pt);
         }
         catch (final NoSuchMethodException nme) {}
         catch (final SecurityException se) {
            try {
               finish = c.getMethod ("finishReading", pt);
            }
            catch (final NoSuchMethodException nme) {}
         }
         if (finish != null && finish.getReturnType () != void.class)
            finish = null;
      }
      if (finish != null) {
         Object[] par;
         switch (flag) {
         case 1:
            par = new Object[] { this, el };
            break;
         case 2:
            par = new Object[] { el };
            break;
         default:
            throw new IllegalStateException ();
         }
         boolean acc = !useSetAccessible
               || isAccessibleFrom (thisMethod, finish);
         try {
            if (!acc)
               finish.setAccessible (true);
            finish.invoke (o, par);
         }
         catch (final IllegalAccessException iae) {
            throw new ParamReadException (el,
                  "Illegal access while calling method " + finish.getName ());
         }
         catch (final InvocationTargetException ite) {
            handleInvocationTargetException (el, ite,
                  "Exception occurred during call to the finishing method");
         }
         finally {
            if (!acc)
               finish.setAccessible (false);
         }
      }
   }

   @SuppressWarnings("all")
   private boolean isSupported (Param o, Node n) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod ("isSupported",
               Param.class, Node.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (n,
               "Could not find the Method object for ParamReader.isSupported");
      }

      final Class<?> c = o.getClass ();
      Method tester = null;
      String name;
      if (n instanceof Attr)
         name = "isAttributeSupported";
      else if (n instanceof Element)
         name = "isNestedElementSupported";
      else
         throw new AssertionError ();
      boolean acc = true;
      try {
         int flag;
         for (flag = 0; flag < 4 && tester == null; flag++) {
            Class<?>[] pt;
            switch (flag) {
            case 0:
               pt = new Class<?>[] { ParamReader.class, String.class, String.class };
               break;
            case 1:
               pt = new Class<?>[] { ParamReader.class, String.class };
               break;
            case 2:
               pt = new Class[] { String.class, String.class };
               break;
            case 3:
               pt = new Class[] { String.class };
               break;
            default:
               throw new AssertionError ();
            }
            try {
               tester = Introspection.getMethod (c, name, pt);
            }
            catch (final NoSuchMethodException nme) {}
            catch (final SecurityException se) {
               try {
                  tester = c.getMethod (name, pt);
               }
               catch (final NoSuchMethodException nme) {}
            }
            if (tester != null && tester.getReturnType () != Boolean.class
                  && tester.getReturnType () != boolean.class)
               tester = null;
         }
         if (tester == null)
            return true;
         acc = !useSetAccessible || isAccessibleFrom (thisMethod, tester);
         if (!acc)
            tester.setAccessible (true);
         String ns = n.getNamespaceURI ();
         String nodeName = n.getLocalName ();
         if (nodeName == null) {
            nodeName = n.getNodeName ();
            ns = "";
         }
         Object[] par;
         switch (flag) {
         case 1:
            par = new Object[] { this, ns, nodeName };
            break;
         case 2:
            par = new Object[] { this, nodeName };
            break;
         case 3:
            par = new Object[] { ns, nodeName };
            break;
         case 4:
            par = new Object[] { nodeName };
            break;
         default:
            throw new IllegalStateException ();
         }
         return ((Boolean) tester.invoke (o, par)).booleanValue ();
      }
      catch (final IllegalAccessException iae) {
         throw new ParamReadException (n,
               "Illegal access while calling method " + tester.getName ());
      }
      catch (final InvocationTargetException ite) {
         handleInvocationTargetException (n, ite,
               "Exception occurred during call to the tester method");
         throw new IllegalStateException ();
      }
      finally {
         if (tester != null && !acc)
            tester.setAccessible (false);
      }
   }

   private static final void handleInvocationTargetException (Node node,
         InvocationTargetException ite, String msg) {
      final Throwable cause = ite.getCause ();
      ParamReadException pre = null;
      if (cause instanceof ParamReadException) {
         pre = (ParamReadException) cause;
         if (pre.node == null)
            pre.node = node;
      }
      else {
         pre = new ParamReadException (node, msg);
         pre.initCause (cause);
      }
      throw pre;
   }

   private boolean isSetterAdder (Method m) {
      // A setter/added must not return a value
      if (m.getReturnType () != void.class)
         return false;
      final Class<?>[] pt = m.getParameterTypes ();
      if (pt == null || pt.length == 0)
         return false;
      else if (pt.length == 1)
         // The method is passed the name of the thing being set.
         return true;
      else if (pt.length == 2) {
         // The two-arguments method must accept the namespace URI and the
         // object.
         if (pt[0] == String.class || pt[0] == ParamReader.class)
            return true;
      }
      else if (pt.length == 3)
         // The three-arguments form is (ParamReader, String, class)
         if (pt[0] == ParamReader.class && pt[1] == String.class)
            return true;
      return false;
   }

   private boolean isCreater (Method m) {
      if (!Param.class.isAssignableFrom (m.getReturnType ()))
         return false;
      final Class<?>[] pt = m.getParameterTypes ();
      if (pt.length == 0)
         return true;
      else if (pt.length == 1) {
         if (pt[0] == Element.class || pt[0] == String.class
               || pt[0] == ParamReader.class)
            return true;
      }
      else if (pt.length == 2) {
         if (pt[0] != ParamReader.class)
            return false;
         if (pt[1] == Element.class || pt[1] == String.class)
            return true;
      }
      return false;
   }

   private boolean isNestedText (Method m) {
      // This is a special case of setter
      if (!isSetterAdder (m))
         return false;
      final Class<?>[] pt = m.getParameterTypes ();
      if (pt.length == 2) {
         // The two-arguments method must accept the param reader and the
         // object.
         if (pt[0] != ParamReader.class)
            return false;
      }
      else if (pt.length == 3)
         return false;
      return true;
   }

   private boolean tryToCallMethod (Node node, Method m, Param o, Object arg) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod ("tryToCallMethod",
               Node.class, Method.class, Param.class, Object.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (node,
               "Could not find the Method object for ParamReader.tryToCallMethod");
      }
      // Try to call the method m on the object o
      final Class<?>[] pt = m.getParameterTypes ();
      Object[] par = null;
      if (pt.length == 3)
         par = new Object[] { this, node.getNamespaceURI (), arg };
      else if (pt.length == 2) {
         if (pt[0] == ParamReader.class)
            par = new Object[] { this, arg };
         else if (par[0] == String.class)
            par = new Object[] { node.getNamespaceURI (), arg };
         else
            throw new IllegalStateException ();
      }
      else
         par = new Object[] { arg };
      boolean acc = !useSetAccessible || isAccessibleFrom (thisMethod, m);
      try {
         if (!acc)
            m.setAccessible (true);
         m.invoke (o, par);
      }
      catch (final IllegalAccessException iae) {
         throw new ParamReadException (node,
               "Illegal access while calling method " + m.getName ());
      }
      catch (final InvocationTargetException ite) {
         // Skip to the next method but remember the exception.
         // It will be thrown if no other method can succeed.
         handleInvocationTargetException (node, ite,
               "Exception occurred during call to a method");
      }
      finally {
         if (!acc)
            m.setAccessible (false);
      }
      return true;
   }

   private Param createParamObject (Element el, Method m, Object o) {
      Method thisMethod;
      try {
         thisMethod = getClass ().getDeclaredMethod ("createParamObject",
               Element.class, Method.class, Object.class);
      }
      catch (final NoSuchMethodException nme) {
         throw new ParamReadException (el,
               "Could not find the Method object for ParamReader.createParamObject");
      }

      // Try to call the method m on the object o
      Object[] par = null;
      final Class<?>[] pt = m.getParameterTypes ();
      boolean bypassProcessElements = false;
      if (pt.length == 0)
         par = new Object[0];
      else if (pt.length == 1) {
         par = new Object[1];
         if (pt[0] == String.class)
            par[0] = el.getNamespaceURI ();
         else if (pt[0] == Element.class) {
            par[0] = el;
            bypassProcessElements = true;
         }
         else if (pt[0] == ParamReader.class)
            par[0] = this;
         else
            throw new IllegalStateException ();
      }
      else if (pt.length == 2) {
         par = new Object[2];
         par[0] = this;
         if (pt[1] == String.class)
            par[1] = el.getNamespaceURI ();
         else if (pt[1] == Element.class) {
            par[1] = el;
            bypassProcessElements = true;
         }
      }
      else
         throw new IllegalStateException ();
      Param eo = null;
      boolean acc = !useSetAccessible || isAccessibleFrom (thisMethod, m);
      try {
         if (!acc)
            m.setAccessible (true);
         eo = (Param) m.invoke (o, par);
      }
      catch (final IllegalAccessException iae) {
         throw new ParamReadException (el,
               "Illegal access while calling method " + m.getName ());
      }
      catch (final InvocationTargetException ite) {
         handleInvocationTargetException (el, ite,
               "Exception occurred during call to a creater method");
      }
      finally {
         if (!acc)
            m.setAccessible (false);
      }
      if (!bypassProcessElements)
         processElement (el, eo);
      return eo;
   }

   private static final String getMethodName (String prefix, Node node) {
      // First, try to get the local name. Namespace prefixes
      // are not unique and namespace URIs are not suitable
      // to be Java method names.
      String name = node.getLocalName ();
      if (name == null)
         // No local name (DOM Level 1 parser)
         name = node.getNodeName ();

      // Convert first letter to uppercase and let the rest
      // as is.
      return prefix + name.substring (0, 1).toUpperCase () + name.substring (1);
   }

   /**
    * Comparator for sorting the methods returned by
    * {@link Introspection#getMethods}. For more consistent searching of
    * overloaded methods, the {@link #processElement} method sorts the methods
    * returned by {@link Introspection#getMethods}. Methods with different
    * names are sorted in alphabetical order. If two or more methods have the
    * same name, they are sorted from the greatest to the smallest visibility:
    * public, protected, package-private, and private. If two methods share the
    * same name and visibility, they are sorted from the greatest to the
    * smallest number of arguments.
    * 
    * When methods with the same name, visibility, and same number of arguments
    * must be compared, the comparator applies a test to each argument, until
    * the methods can be ordered. First, argument 0 is compared with other
    * methods' argument 0. If the arguments are equal, or cannot be ordered, the
    * test is performed with argument 1, 2, etc., until the arguments can be
    * ordered. If all the arguments are equal or cannot be ordered, the methods
    * cannot be ordered and are declared equal by the comparator. In this case,
    * the final order of the methods depends on the Virtual Machine being used.
    * 
    * \begin{table}[htb] \begin{center} \begin{tabular}{|l|r|}\hline Class
    * (including subclasses)\html{$\mbox{}$}&Score\\\hline\hline
    * {@link ParamReader}&1\\\hline {@link Node}&2\\\hline {@link Param}&3\\\hline
    * {@link TimeParam} or primitive type&4\\\hline {@link Number}&5\\\hline
    * {@link java.io.File}, {@link java.net.URI}, {@link java.net.URL}&6\\\hline
    * {@link String}&7\\\hline {@link Object}&8\\\hline Other&10\\\hline
    * Arrays&2xComponent\\\hline \end{tabular} \end{center} \caption{Score
    * assigned to classes when comparing arguments} \label{tab:scores}
    * \end{table}
    * 
    * Arguments are compared based on their types only. To compare data types,
    * the comparator assigns a score to each one and orders the type with the
    * smallest score first. For array types, the score of the component type is
    * multipled by two. If types have the same score, they are equal or cannot
    * be ordered by this algorithm. Table~\ref{tab:scores} gives the score
    * assigned to each class.
    */
   public static class MethodComparator implements Comparator<Method> {
      public int compare (Method m1, Method m2) {
         if (m1 == null && m2 == null)
            return 0;
         if (m1 == null)
            return -1;
         if (m2 == null)
            return 1;
         // First, sort the methods by name
         int d = m1.getName ().compareTo (m2.getName ());
         if (d != 0)
            return d;
         d = getScoreAcc (m1) - getScoreAcc (m2);
         if (d != 0)
            return d;
         // The method with the greatest number of parameters takes precedence
         final Class<?>[] pt1 = m1.getParameterTypes ();
         final Class<?>[] pt2 = m2.getParameterTypes ();
         if (pt1.length > pt2.length)
            return -1;
         if (pt2.length > pt1.length)
            return 1;
         // If both methods take no argument, equality.
         // In fact, this cannot happen since two methods cannot
         // have the same name and signature.
         if (pt1.length == 0)
            return 0;
         for (int i = 0; i < pt1.length; i++) {
            // A score is assigned to argument i of each method
            // and the method with the smallest score goes first.
            final Class<?> p1 = pt1[i];
            final Class<?> p2 = pt2[i];
            if (p1 == p2)
               continue;
            final int s1 = getScore (p1);
            final int s2 = getScore (p2);
            d = s1 - s2;
            if (d != 0)
               return d;
         }
         return 0;
      }

      private int getScoreAcc (Member m) {
         final int mod = m.getModifiers ();
         if (Modifier.isPrivate (mod))
            return 3;
         if (Modifier.isProtected (mod))
            return 1;
         if (Modifier.isPublic (mod))
            return 0;
         return 2;
      }

      private int getScore (Class<?> c) {
         if (c.isArray ()) {
            Class<?> componentClass = c;
            do
               componentClass = componentClass.getComponentType ();
            while (componentClass.isArray ());
            return 2 * getScoreNoArray (componentClass);
         }
         else
            return getScoreNoArray (c);
      }

      private int getScoreNoArray (Class<?> c) {
         if (ParamReader.class.isAssignableFrom (c))
            return 1;
         if (Node.class.isAssignableFrom (c))
            return 2;
         if (Param.class.isAssignableFrom (c))
            return 3;
         if (c.isPrimitive () || TimeParam.class.isAssignableFrom (c))
            return 4;
         if (Number.class.isAssignableFrom (c))
            return 5;
         if (java.io.File.class.isAssignableFrom (c)
               || java.net.URI.class.isAssignableFrom (c)
               || java.net.URL.class.isAssignableFrom (c))
            return 6;
         if (String.class.isAssignableFrom (c))
            return 7;
         if (Object.class == c)
            return 8;
         return 10;
      }
   }
}
