package umontreal.iro.lecuyer.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.NameConflictException;

/**
 * Provides utility methods to convert strings into Java objects. The Java class
 * library already contains some facilities to convert strings to many object
 * types, but the target class must be known at compile time. With these utility
 * methods, a conversion can be performed with a target class known at run time.
 */
public class StringConvert {
   private static DatatypeFactory datatypeFactory;
   private StringConvert () {}

   /**
    * Tries to convert the string \texttt{val} into an object of the class
    * \texttt{cls} or one of its subclasses or implementations. \texttt{cls} can
    * be a primitive type, a class type, or an array. When a primitive target
    * type is given, the method returns an object from the corresponding wrapper
    * class. The class finder \texttt{finder} is used to resolve class names and
    * will be replaced by {@link Class#forName} if \texttt{null}.
    *
    * If \texttt{cls} is {@link String} or {@link Object}, \texttt{val} is
    * returned unchanged. If it is \texttt{null}, a null pointer is returned.
    * The method tries to use {@link #numberFromString(Class,String)} for numeric types.
    * If \texttt{cls} corresponds to the {@link Duration} class, this method
    * calls {@link #durationFromString(String)}.  If
    * \texttt{cls.isEnum()} returns \texttt{true}, it uses {@link Enum#valueOf}
    * to convert the string to an object representing an enum constant. For the
    * \texttt{char} type, the first character of the string is returned into the
    * {@link Character} wrapper object. For boolean type, the method uses
    * {@link #booleanFromString(String)}.
    * Objects of class {@link URI} are obtained using
    * {@link #uriFromString(URI,String)}
    * while objects of class {@link URL}
    * are obtained by
    * {@link #urlFromString(URI,String)}.
    * If no previous class is compatible with the
    * target class, the method tries to use {@link #wrapperFromString(Class,String)} to perform
    * the conversion.
    *
    * If the target object is from class {@link Class}, the given string is
    * interpreted as a class name and passed to \texttt{findClass} to get a
    * {@link Class} object, using the class finder to resolve the class name. If
    * \texttt{finder} is \texttt{null}, {@link Class#forName} is used instead.
    *
    * The method also processes arrays in arbitrary depths. For a
    * one-dimensional array, the string is read as a list of tokens separated
    * with commas and for each token, the method is called recursively. The
    * {@link #getArrayElements} method is used to perform the tokenization. To
    * encode multi-dimensional arrays, one must encode arrays into tokens by
    * using the brackets. For example, if the target class is \texttt{int[][]},
    * \texttt{[1,2],[3,4]} will be converted as a 2D array with \texttt{1,2} in
    * the index 0 and \texttt{3,4} in the index 1.
    *
    * If the target class is not an array and cannot be converted in any ways,
    * the method forwards the call to {@link #getStaticValue}.
    * @param baseURI the base URI used by
    * {@link #uriFromString(URI,String)} and
    * {@link #urlFromString(URI,String)}.
    * @param finder
    *           the class finder being used to look for classes if necessary.
    * @param cls
    *           the target class of the returned object.
    * @param val
    *           the string to be converted.
    *
    * @exception IllegalArgumentException
    *               if the string cannot be converted, although conversion to
    *               the target class is supported.
    * @exception UnsupportedConversionException
    *               if the target class is not supported by the converter.
    * @exception NameConflictException
    *               if a name conflict occurs when resolving a class name.
    * @return the converted object.
    */
   @SuppressWarnings ("unchecked")
   public static <T> T fromString (URI baseURI, ClassFinder finder, Class<T> cls, String val)
         throws UnsupportedConversionException, NameConflictException {
      // Built-in types
      if (val == null)
         return null;
      if (cls == String.class || cls == Object.class)
         return (T) val;
      final String trimmedVal = val.trim ();
      try {
         return numberFromString (cls, trimmedVal);
      }
      catch (final UnsupportedConversionException uce) {}
      if (cls == Duration.class)
         return (T) durationFromString (trimmedVal);
      if (cls.isEnum ())
         return (T) Enum.valueOf ((Class) cls, trimmedVal);
      if (cls == Boolean.class || cls == boolean.class)
         return (T) booleanFromString (trimmedVal);
      else if ((cls == Character.class || cls == char.class)
            && trimmedVal.length () > 0)
         return (T) new Character (trimmedVal.charAt (0));

      if (cls == URI.class)
         return (T)uriFromString (baseURI, trimmedVal);
      if (cls == URL.class)
         return (T)urlFromString (baseURI, trimmedVal);
      try {
         return wrapperFromString (cls, trimmedVal);
      }
      catch (final UnsupportedConversionException oce) {}

      if (cls == Class.class)
         try {
            if (finder == null)
               return (T) Class.forName (trimmedVal);
            else
               return (T) finder.findClass (trimmedVal);
         }
         catch (final ClassNotFoundException cnfe) {
            throw new IllegalArgumentException ("Cannot find class "
                  + trimmedVal);
         }

      if (cls.isArray ()) {
         // Tokenize the string into array elements
         final String[] ar = getArrayElements (trimmedVal);
         // For each element, call this method recursively
         // to convert it into its component type.
         final Class<?> ctype = cls.getComponentType ();
         final Object array = Array.newInstance (ctype, ar.length);
         for (int i = 0; i < ar.length; i++)
            Array.set (array, i, fromString (baseURI, finder, ctype, ar[i]));
         return (T) array;
      }
      else
         // Try to consider the string as the name
         // of a class to construct an object from or
         // a reference to a static field or method.
         return getStaticValue (baseURI, finder, cls, trimmedVal);
   }

   /**
    * Converts an array of strings \texttt{vals} to an array of arguments to be
    * passed to a constructor or method with parameter types
    * \texttt{paramTypes}. The method tries to convert \texttt{vals[p]} to the
    * class given by \texttt{paramTypes[p]} using the {@link #fromString(URI,ClassFinder,Class, String)} method
    * and stores the result in the returned array.
    * @param baseURI the base URI used
    * by {@link #fromString(URI,ClassFinder,Class,String)}
    * @param paramTypes
    *           the target type of each argument.
    * @param vals
    *           the string arguments.
    *
    * @return an array of objects representing each argument.
    * @exception IllegalArgumentException
    *               if \texttt{vals} and \texttt{paramTypes} have different
    *               lengths or a conversion error occurred.
    * @exception NullPointerException
    *               if \texttt{vals}, \texttt{paramTypes} or any elements of
    *               these arrays are \texttt{null}.
    * @exception UnsupportedConversionException
    *               if the converter does not support one or more argument
    *               class.
    */
   public static Object[] getArgumentsFromString (URI baseURI,
         ClassFinder finder, Class<?>[] paramTypes, String[] vals)
         throws UnsupportedConversionException, NameConflictException {
      if (vals.length != paramTypes.length)
         throw new IllegalArgumentException ("Incompatible array dimensions");
      final Object[] par = new Object[vals.length];
      for (int p = 0; p < vals.length; p++)
         par[p] = fromString (baseURI, finder, paramTypes[p], vals[p]);
      return par;
   }

   /**
    * Converts \texttt{val} to an instance of \texttt{cls} by considering the
    * string as a call to a constructor, a static method or a static field. The
    * string must be speficied as a Java expression. The arguments of
    * constructors and static methods are processed by {@link #fromString(URI,ClassFinder,Class, String)}. The
    * constructed class, the type of the static field, or the return value of
    * the static method must be assignable to the target class. If the
    * conversion fails, an {@link IllegalArgumentException} is thrown.
    * @param baseURI the base URI used
    * by {@link #uriFromString(URI,String)}
    * and {@link #urlFromString(URI,String)}.
    * @param finder
    *           the class finder being used.
    * @param cls
    *           the target class for the conversion.
    * @param val
    *           the expression being converted.
    *
    * @return the constructed object.
    * @exception UnsupportedConversionException
    *               if {@link #fromString(URI,ClassFinder,Class, String)} fails processing the arguments because
    *               the class of the called constructor's arguments, the type of
    *               the referenced field, or the return value of the called
    *               method are not supported as target classes.
    * @exception NameConflictException
    *               if a class name conflict arises.
    * @exception IllegalArgumentException
    *               if the target class is supported but a conversion problem
    *               occurs.
    */
   @SuppressWarnings ("unchecked")
   public static <T> T getStaticValue (URI baseURI, ClassFinder finder,
         Class<T> cls, String val) throws UnsupportedConversionException,
         NameConflictException {
      // Separate the parameter specification from
      // the class or member name.
      int idx = val.indexOf ('(');
      String mName = null;
      String params = null;
      if (idx == -1) {
         // No parentheses
         mName = val;
         params = "";
      }
      else {
         // Arguments are given
         if (val.charAt (val.length () - 1) != ')')
            throw new IllegalArgumentException (
                  "Missing ) in the value string: " + val);
         mName = val.substring (0, idx).trim ();
         params = val.substring (idx);
      }
      // Returns the array of parameters, if parameters are given.
      // Parentheses are suppressed.
      final String[] paramArray = params.length () == 0 ? new String[0]
            : getArrayElements (params.substring (1, params.length () - 1)
                  .trim ());

      // First, interpret the string as a class name and try
      // to construct an object with the empty constructo
      // if this is the right class.
      Class<?> c = null;
      try {
         if (finder == null)
            c = Class.forName (mName);
         else
            c = finder.findClass (mName);
      }
      catch (final ClassNotFoundException cnfe) {}

      if (c != null)
         if (cls.isAssignableFrom (c))
            // Since a compatible class was found,
            // we will try to construct an object
            // from it and will not consider
            // the string as a member name.
            try {
               return (T) tryToCallConstructor (baseURI, finder, c, paramArray);
            }
            catch (final NoSuchMethodException nme) {
               throw new IllegalArgumentException ("The class " + c.getName ()
                     + " does not have an appropriate constructor");
            }
         else
            throw new IllegalArgumentException ("Incompatible class " + mName);

      idx = mName.lastIndexOf ('.');
      if (idx == -1)
         throw new UnsupportedConversionException (cls, val,
               "Cannot convert value: " + val);
      final String clsName = mName.substring (0, idx);
      final String memberName = mName.substring (idx + 1);

      try {
         if (finder == null)
            c = Class.forName (clsName);
         else
            c = finder.findClass (clsName);
      }
      catch (final ClassNotFoundException cnfe) {
         throw new IllegalArgumentException ("Cannot find class with name "
               + clsName);
      }

      if (params.length () == 0) {
         Field f = null;
         try {
            f = c.getField (memberName);
            if (!Modifier.isStatic (f.getModifiers ()))
               throw new IllegalArgumentException ("The field " + memberName
                     + " in class " + clsName + " must be "
                     + "public and static");
            if (!cls.isAssignableFrom (f.getType ()))
               throw new IllegalArgumentException ("The field " + memberName
                     + " in class " + clsName + " does not have "
                     + "a compatible type");
            return (T) f.get (null);
         }
         catch (final NoSuchFieldException nsfe) {
            throw new IllegalArgumentException ("Cannot find field "
                  + memberName + " in class " + clsName);
         }
         catch (final IllegalAccessException iae) {
            throw new IllegalArgumentException ("Inaccessible field "
                  + memberName + " in class " + clsName);
         }
      }
      else
         try {
            return tryToCallMethod (baseURI, finder, cls, c, null,
                  memberName, paramArray);
         }
         catch (final NoSuchMethodException nme) {
            throw new IllegalArgumentException ("No appropriate method "
                  + memberName + " in class " + c.getName ());
         }
   }

   private static final Pattern arraySepPattern = Pattern.compile ("(\\s*,\\s*|\\s+)");
   private static final Pattern braceAndParenPattern = Pattern.compile ("(\\{|\\}|\\(|\\))");

   /**
    * Tokenizes the string \texttt{val} into an array of strings
    * using whitespaces or commas as delimiters, and merging
    * back parts surrounded by braces or parentheses.
    * Parentheses always appear in resulting tokens while the surrounding
    * braces are removed.
    * Any consecutive substring of whitespace is replaced by
    * a single whitespace character.
    * This method is different from
    * {@link String#split} because it takes braces and parentheses
    * into account.
    *
    * For example, the string \texttt{1 2 3   4} given to this method would
    * produce an array containing four elements: \texttt{1},
    * \texttt{2}, \texttt{3}, and \texttt{4}.
    * The string \texttt{\{1,2\} \{3 4\}} will be
    * separated in two strings by this method: \texttt{1 2} and \texttt{3 4}.
    * The string \texttt{\{\{1 2\} 3\} a (4 5)} would become
    * an array containing \texttt{\{ 1 2 \} 3}, and \texttt{a ( 4 5 )}.
    *
    * @param val
    *           the value to be tokenized.
    * @return an array of strings representing each token.
    */
   public static String[] getArrayElements (String val) {
      // Ensures that parentheses and braces are isolated with spaces
      final Matcher m = braceAndParenPattern.matcher (val);
      final StringBuffer valBuffer = new StringBuffer();
      while (m.find ())
         m.appendReplacement (valBuffer, " $1 ");
      m.appendTail (valBuffer);
      // Tokenizes the string
      final String[] parts = arraySepPattern.split (valBuffer);
      final List<String> ar = new ArrayList<String> ();
      final StringBuilder currentString = new StringBuilder();
      int braceLevel = 0;
      int parenLevel = 0;
      for (final String part : parts) {
         final int partLength = part.length ();
         if (partLength == 0)
            continue;
         boolean appendPart = false;
         boolean makeNewString = false;
         boolean appendToPreviousString = false;
         if (partLength > 1) {
            appendPart = true;
            if (parenLevel == 0 && braceLevel == 0)
               makeNewString = true;
         }
         else if (part.equals ("(")) {
            ++parenLevel;
            appendPart = true;
         }
         else if (part.equals (")")) {
            --parenLevel;
            if (parenLevel < 0)
               throw new IllegalArgumentException
               ("Too many closing parentheses in string " + val);
            appendPart = true;
            if (parenLevel == 0 && braceLevel == 0) {
               makeNewString = true;
               appendToPreviousString = true;
            }
         }
         else if (parenLevel == 0) {
            if (part.equals ("{")) {
               if (braceLevel > 0)
                  appendPart = true;
               ++braceLevel;
            }
            else if (part.equals ("}")) {
               --braceLevel;
               if (braceLevel > 0)
                  appendPart = true;
               if (braceLevel < 0)
                  throw new IllegalArgumentException
                  ("Too many closing braces in string " + val);
            }
            else
               appendPart = true;
            if (braceLevel == 0)
               makeNewString = true;
         }
         else
            appendPart = true;

         if (appendPart) {
            if (currentString.length () > 0)
               currentString.append (' ');
            currentString.append (part);
         }
         if (makeNewString) {
            if (appendToPreviousString && !ar.isEmpty ())
               ar.set (ar.size () - 1, ar.get (ar.size () - 1) + " " + currentString.toString ());
            else
               ar.add (currentString.toString ());
            currentString.delete (0, currentString.length ());
         }
      }
//      int beginIdx = 0;
//      int endIdx = -1;
//      final int valLength = val.length ();
//      for (int i = 0; i < valLength; i++) {
//         final char ch = val.charAt (i);
//         if (ch == '(')
//            ++parenLevel;
//         else if (ch == ')')
//            --parenLevel;
//         else if (ch == '{' && parenLevel == 0) {
//            if (braceLevel == 0)
//               beginIdx = i + 1;
//            ++braceLevel;
//         }
//         else if (ch == '}' && parenLevel == 0) {
//            --braceLevel;
//            if (braceLevel < 0)
//               throw new IllegalArgumentException ("Too many } in the value "
//                     + val);
//            if (braceLevel == 0)
//               endIdx = i;
//         }
//         else if (braceLevel == 0 && parenLevel == 0 && ch == ',') {
//            if (endIdx == -1)
//               endIdx = i;
//            final String str = val.substring (beginIdx, endIdx).trim ();
//            if (str.length () > 0)
//               ar.add (str);
//            beginIdx = i + 1;
//            endIdx = -1;
//         }
//      }
      if (parenLevel > 0)
         throw new IllegalArgumentException ("Missing closing parentheses in value " + val);
      if (braceLevel > 0)
         throw new IllegalArgumentException ("Missing closing braces in value " + val);
//      if (beginIdx != val.length () && beginIdx != endIdx) {
//         if (endIdx == -1)
//            endIdx = val.length ();
//         final String str = val.substring (beginIdx, endIdx).trim ();
//         if (str.length () > 0)
//            ar.add (str);
//      }
      return ar.toArray (new String[ar.size ()]);
   }

   /**
    * Converts the string \texttt{val} to an instance of \texttt{cls} being a
    * subclass of {@link Number} or a primitive numeric type. In the
    * \texttt{val} argument, the method accepts a number or the special strings
    * \texttt{Infinity}, \texttt{INF}, \texttt{-Infinity}, and
    * \texttt{-INF}. For an integer, a short, a long
    * or a byte, infinity is represented using the greatest or smallest
    * acceptable value for the built-in types. For single or double-precision
    * floating points, the infinity can be represented directly. For
    * floating-points, the \texttt{nan} string is accepted to denote a NaN.
    *
    * @param cls
    *           the target class.
    * @param val
    *           the string being converted.
    * @return the constructed numeric wrapper object.
    * @exception NumberFormatException
    *               if a conversion problem occurred.
    * @exception UnsupportedConversionException
    *               if the class is not a supported subclass of {@link Number}
    *               or a primitive numeric type.
    */
   @SuppressWarnings ("unchecked")
   public static <T> T numberFromString (Class<T> cls, String val)
         throws UnsupportedConversionException {
      if (val == null)
         return null;
      if (val.equalsIgnoreCase ("Infinity") ||
            val.equalsIgnoreCase ("INF")) {
         if (cls == Double.class || cls == double.class)
            return (T) new Double (Double.POSITIVE_INFINITY);
         else if (cls == Float.class || cls == float.class)
            return (T) new Float (Float.POSITIVE_INFINITY);
         else if (cls == Integer.class || cls == int.class)
            return (T) new Integer (Integer.MAX_VALUE);
         else if (cls == Long.class || cls == long.class)
            return (T) new Long (Long.MAX_VALUE);
         else if (cls == Short.class || cls == short.class)
            return (T) new Short (Short.MAX_VALUE);
         else if (cls == Byte.class || cls == byte.class)
            return (T) new Byte (Byte.MAX_VALUE);
      }
      else if (val.equalsIgnoreCase ("-Infinity") ||
            val.equalsIgnoreCase ("-INF")) {
         if (cls == Double.class || cls == double.class)
            return (T) new Double (Double.NEGATIVE_INFINITY);
         else if (cls == Float.class || cls == float.class)
            return (T) new Float (Float.NEGATIVE_INFINITY);
         else if (cls == Integer.class || cls == int.class)
            return (T) new Integer (Integer.MIN_VALUE);
         else if (cls == Long.class || cls == long.class)
            return (T) new Long (Long.MIN_VALUE);
         else if (cls == Short.class || cls == short.class)
            return (T) new Short (Short.MIN_VALUE);
         else if (cls == Byte.class || cls == byte.class)
            return (T) new Byte (Byte.MIN_VALUE);
      }
      else if (val.equalsIgnoreCase ("nan"))
         if (cls == Double.class || cls == double.class)
            return (T) new Double (Double.NaN);
         else if (cls == Float.class || cls == float.class)
            return (T) new Float (Float.NaN);

      if (cls == Double.class || cls == double.class)
         return (T) new Double (val);
      else if (cls == Float.class || cls == float.class)
         return (T) new Float (val);
      else if (cls == Integer.class || cls == int.class)
         return (T) new Integer (val);
      else if (cls == Long.class || cls == long.class)
         return (T) new Long (val);
      else if (cls == Short.class || cls == short.class)
         return (T) new Short (val);
      else if (cls == Byte.class || cls == byte.class)
         return (T) new Byte (val);
      else if (Number.class.isAssignableFrom (cls))
         // Supports BigInteger, BigDecimal and any custom
         // subclasses of Number providing a constructor
         // taking a String parameter or a valueOf method.
         return wrapperFromString (cls, val);
      else
         throw new UnsupportedConversionException ("Unsupported target class: "
               + cls.getName ());
   }

   /**
    * Converts a string \texttt{val} to a boolean wrapper object. The method
    * accepts \texttt{1}, \texttt{true}, \texttt{yes}, and \texttt{on} as a true
    * value and \texttt{0}, \texttt{false}, \texttt{no}, and \texttt{off} as a
    * false value.
    *
    * @param val
    *           the value being converted.
    * @return the result of the conversion.
    * @exception IllegalArgumentException
    *               if the value cannot be converted.
    */
   public static Boolean booleanFromString (String val) {
      if (val == null)
         return null;
      if (val.equals ("1") || val.equalsIgnoreCase ("true")
            || val.equalsIgnoreCase ("yes") || val.equalsIgnoreCase ("on"))
         return Boolean.TRUE;
      else if (val.equals ("0") || val.equalsIgnoreCase ("false")
            || val.equalsIgnoreCase ("no") || val.equalsIgnoreCase ("off"))
         return Boolean.FALSE;
      else
         throw new IllegalArgumentException ("Invalid boolean value: " + val);
   }

   /**
    * Constructs and returns a new
    * URI from the string \texttt{url},
    * resolved against the base URI
    * \texttt{baseURI}.
    * If \texttt{baseURI} is \texttt{null},
    * this method creates a new URI
    * using the {@link URI#URI(String)}
    * constructor.
    * Otherwise, it resolves the
    * string \texttt{uri} against
    * the base URI to obtain the URI
    * object.
    * Any exception is wrapped into
    * an illegal-argument exception.
    * @param baseURI the base URI.
    * @param uri the URI to resolve.
    * @return the resolved URI object.
    */
   public static URI uriFromString (URI baseURI, String uri) {
      try {
         if (baseURI == null)
            return new URI (uri);
         else
            return baseURI.resolve (uri);
      }
      catch (final URISyntaxException use) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Invalid URI " + uri);
         iae.initCause (use);
         throw iae;
      }
   }

   /**
    * This method calls
    * {@link #uriFromString(URI,String)}
    * with the given base URI, and URL, and
    * converts the resulting URI into a
    * URL using {@link URI#toURL()}.
    * @param baseURI the base URI.
    * @param url the URL.
    * @return the resulting URL object.
    */
   public static URL urlFromString (URI baseURI, String url) {
      try {
         return uriFromString (baseURI, url).toURL ();
      }
      catch (final MalformedURLException me) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Invalid URL " + url);
         iae.initCause (me);
         throw iae;
      }
   }

   /**
    * Constructs and returns a {@link Duration}
    * object obtained from the string
    * \texttt{str}.
    * This method uses {@link DatatypeFactory#newDuration(String)}
    * to perform the conversion.
    * @param str the string to convert.
    * @return the obtained duration.
    * @throws UnsupportedConversionException if the data type
    * factory could not be created.
    */
   public static Duration durationFromString (String str) throws UnsupportedConversionException {
      if (datatypeFactory == null)
         try {
            datatypeFactory = DatatypeFactory.newInstance ();
         }
         catch (final DatatypeConfigurationException dce) {
            final UnsupportedConversionException uce = new UnsupportedConversionException
            ("Cannot create data type factory");
            uce.initCause (dce);
            throw uce;
         }
      return datatypeFactory.newDuration (str);
   }

   /**
    * Converts a string to a wrapper object of class \texttt{cls}. This method
    * tries to find a constructor for class \texttt{cls} taking a {@link String}
    * as its unique argument. If such a constructor can be found, it is used to
    * instantiate a new object which is returned. If no such constructor can be
    * found and invoked, the method searches for a \texttt{valueOf} public
    * static method taking a {@link String} argument and returning an instance
    * of \texttt{cls} or a subclass of \texttt{cls}. If the class does not
    * contain an appropriate constructor or static method, an
    * {@link UnsupportedConversionException} is thrown. If the constructor or
    * method is found but cannot be called successfully, an
    * {@link IllegalArgumentException} is thrown.
    *
    * @param cls
    *           the target class for the conversion.
    * @param val
    *           the value being converted.
    * @return the converted value.
    * @exception IllegalArgumentException
    *               if an exception occurred during the construction of the
    *               wrapper object.
    * @exception UnsupportedConversionException
    *               if the target class does not contain an appropriate
    *               constructor or static method.
    */
   @SuppressWarnings ("unchecked")
   public static <T> T wrapperFromString (Class<T> cls, String val)
         throws UnsupportedConversionException {
      if (val == null)
         return null;
      IllegalArgumentException iae = null;
      try {
         final Constructor<T> ctor = cls.getConstructor (String.class);
         return ctor.newInstance (val);
      }
      catch (final NoSuchMethodException nse) {}
      catch (final IllegalAccessException iace) {
         iae = new IllegalArgumentException ("Cannot access constructor: "
               + iace.getMessage ());
      }
      catch (final InstantiationException ie) {
         iae = new IllegalArgumentException (
               "Cannot construct a wrapper object: " + ie.getMessage ());
      }
      catch (final InvocationTargetException ite) {
         try {
            handleInvocationTargetException (ite,
                  "Exception occurred in constructor");
         }
         catch (final IllegalArgumentException iae2) {
            iae = iae2;
         }
      }

      try {
         final Method valueOfMethod = cls.getMethod ("valueOf", String.class);
         if (Modifier.isStatic (valueOfMethod.getModifiers ())
               && cls.isAssignableFrom (valueOfMethod.getReturnType ()))
            return (T) valueOfMethod.invoke (null, val);
      }
      catch (final NoSuchMethodException nme) {}
      catch (final IllegalAccessException iace) {
         throw new IllegalArgumentException ("Cannot access valueOf method: "
               + iace.getMessage ());
      }
      catch (final InvocationTargetException ite) {
         handleInvocationTargetException (ite,
               "Exception occurred during call to valueOf");
      }
      if (iae == null)
         throw new UnsupportedConversionException ("Class " + cls.getName ()
               + " does not have a constructor or a valueOf static method "
               + "taking a String");
      else
         throw iae;
   }

   /**
    * Tries to find a compatible constructor in class \texttt{cls}, converts the
    * arguments stored into \texttt{vals} to the arguments of the constructor,
    * calls it and returns the constructed instance. For each constructor with
    * the appropriate number of arguments, argument \texttt{vals[i]} is
    * converted to the type of the argument \texttt{i} of the constructor, using
    * {@link #getArgumentsFromString(URI,ClassFinder,Class[], String[])}. If one conversion fails, the other
    * constructors are tried. If all conversion fail, an exception is thrown.
    * @param baseURI the base URI used by
    * {@link #uriFromString(URI,String)}
    * and {@link #urlFromString(URI,String)}.
    * @param finder
    *           the class finder used by {@link #getArgumentsFromString(URI,ClassFinder,Class[], String[])}.
    * @param cls
    *           the target class.
    * @param vals
    *           the arguments for the constructor.
    *
    * @return the constructed instance.
    * @exception UnsupportedConversionException
    *               if some arguments could not be converted.
    * @exception NameConflictException
    *               if a name conflict occurred during class name resolving.
    * @exception IllegalArgumentException
    *               if the conversion of an argument is supported by
    *               {@link #fromString(URI,ClassFinder,Class, String)}, but some errors occurred, or an exception
    *               occurred during the call to the constructor.
    * @exception NoSuchMethodException
    *               if no appropriate constructor could be found.
    */
   @SuppressWarnings("unchecked")
   public static <T> T tryToCallConstructor (URI baseURI, ClassFinder finder,
         Class<T> cls, String... vals) throws UnsupportedConversionException,
         NameConflictException, NoSuchMethodException {
      if (vals == null || vals.length == 0)
         // No parameters given, call an empty constructor
         try {
            final Constructor<T> ctor = cls.getConstructor ();
            return ctor.newInstance (new Object[0]);
         }
         catch (final IllegalAccessException iae) {
            throw new IllegalArgumentException (
                  "Cannot access nullary constructor in " + cls.getName ());
         }
         catch (final InstantiationException ie) {
            throw new IllegalArgumentException (
                  "Cannot call the nullary constructor for class "
                        + cls.getName ());
         }
         catch (final InvocationTargetException ite) {
            handleInvocationTargetException (ite,
                  "Exception occurred during call to constructor");
            return null;
         }
      else {
         // Try to find a compatible constructor and call it
         IllegalArgumentException iae = null;
         NameConflictException nce = null;
         UnsupportedConversionException uce = null;
         for (final Constructor<?> ctor : cls.getConstructors ()) {
            final Class[] pt = ctor.getParameterTypes ();
            if (pt.length == vals.length)
               try {
                  return (T)ctor.newInstance (getArgumentsFromString (baseURI, finder, pt, vals));
               }
               catch (final IllegalArgumentException niae) {
                  iae = niae;
               }
               catch (final UnsupportedConversionException nuce) {
                  uce = nuce;
               }
               catch (final NameConflictException nnce) {
                  nce = nnce;
               }
               catch (final IllegalAccessException niae) {}
               catch (final InstantiationException ie) {
                  throw new IllegalArgumentException (ie.toString ());
               }
               catch (final InvocationTargetException ite) {
                  handleInvocationTargetException (ite,
                        "Exception occurred during call to constructor");
                  return null;
               }
         }
         if (uce != null)
            throw nce;
         if (nce != null)
            throw nce;
         if (iae != null)
            throw iae;
         throw new NoSuchMethodException (
               "Cannot find an appropriate constructor for class "
                     + cls.getName ());
      }
   }

   /**
    * Tries to call a method named \texttt{name} on object \texttt{o} with
    * arguments given by \texttt{vals}, with expected return value class
    * \texttt{rcls}. The method searches for all public methods in \texttt{cls}
    * taking \texttt{vals.length} arguments (0 argument if \texttt{vals} is
    * \texttt{null}), and tries to convert the arguments using
    * {@link #getArgumentsFromString(URI,ClassFinder,Class[], String[])}. If \texttt{o} is \texttt{null}, the search
    * will be restricted to static methods.
    * @param baseURI the base URI used by
    * {@link #uriFromString(URI,String)}
    * and {@link #urlFromString(URI,String)}.
    * @param finder
    *           the class finder used by {@link #getArgumentsFromString(URI,ClassFinder,Class[], String[])}.
    * @param rcls
    *           the target class of the return type.
    * @param cls
    *           the class defining the method to be invoked.
    * @param o
    *           the object on which the method is invoked, should be an instance
    *           of the class represented by \texttt{cls}.
    * @param name
    *           the name of the method.
    * @param vals
    *           the arguments for the method.
    *
    * @return the return value.
    * @exception UnsupportedConversionException
    *               if some arguments are not compatible with {@link #fromString(URI,ClassFinder,Class, String)}.
    * @exception NameConflictException
    *               if a name conflict occurs during a class name resolution.
    * @exception IllegalArgumentException
    *               if no method can be found or some conversions failed.
    * @exception NoSuchMethodException
    *               if no appropriate method could be found.
    */
   @SuppressWarnings ("unchecked")
   public static <T> T tryToCallMethod (URI baseURI, ClassFinder finder,
         Class<T> rcls, Class<?> cls, Object o, String name, String... vals)
         throws UnsupportedConversionException, NameConflictException,
         NoSuchMethodException {
      if (o != null && !cls.isInstance (o))
         throw new IllegalArgumentException ("The given object "
               + o.toString () + " is not an instance of the given class "
               + cls.getName ());
      IllegalArgumentException iae = null;
      UnsupportedConversionException uce = null;
      NameConflictException nce = null;
      if (vals == null)
         return tryToCallMethod (baseURI, finder, rcls, cls, o, name);
      for (final Method method : cls.getMethods ())
         if (method.getName ().equals (name)
               && rcls.isAssignableFrom (method.getReturnType ())
               && method.getParameterTypes ().length == vals.length) {
            if (o == null && !Modifier.isStatic (method.getModifiers ()))
               continue;
            final Class[] pt = method.getParameterTypes ();
            try {
               return (T) method
                     .invoke (o, getArgumentsFromString (baseURI, finder, pt, vals));
            }
            catch (final IllegalArgumentException niae) {
               iae = niae;
            }
            catch (final UnsupportedConversionException nuce) {
               uce = nuce;
            }
            catch (final NameConflictException nnce) {
               nce = nnce;
            }
            catch (final IllegalAccessException niae) {}
            catch (final InvocationTargetException ite) {
               handleInvocationTargetException (ite,
                     "Exception occurred during call to method" + name);
            }
         }
      if (uce != null)
         throw nce;
      if (nce != null)
         throw nce;
      if (iae != null)
         throw iae;
      throw new NoSuchMethodException ("Cannot find an appropriate method in "
            + cls.getName ());
   }

   private static void handleInvocationTargetException (
         InvocationTargetException ite, String msg) {
      final Throwable cause = ite.getTargetException ();
      if (cause instanceof Error)
         throw (Error) cause;
      else if (cause instanceof IllegalArgumentException)
         throw (IllegalArgumentException) cause;
      throw new IllegalArgumentException (msg + ": " + cause.toString ());
   }

   /**
    * Converts the integer value \texttt{i} to a string by using
    * {@link String#valueOf(int)}. If \texttt{i} is equal to the minimum or
    * maximum value of an integer, \texttt{-Infinity} or \texttt{Infinity} are
    * returned, respectively.
    *
    * @param i
    *           the integer being formatted.
    * @return the formatted integer.
    */
   public static String intToString (int i) {
      if (i == Integer.MAX_VALUE)
         return "Infinity";
      else if (i == Integer.MIN_VALUE)
         return "-Infinity";
      else
         return String.valueOf (i);
   }

   /**
    * Same as {@link #intToString(int)} for a byte.
    *
    * @param b
    *           the byte being formatted.
    * @return the formatted byte.
    */
   public static String byteToString (byte b) {
      if (b == Byte.MAX_VALUE)
         return "Infinity";
      else if (b == Byte.MIN_VALUE)
         return "-Infinity";
      else
         return String.valueOf (b);
   }

   /**
    * Same as {@link #intToString(int)} for a short.
    *
    * @param s
    *           the short integer being formatted.
    * @return the formatted value.
    */
   public static String shortToString (short s) {
      if (s == Short.MAX_VALUE)
         return "Infinity";
      else if (s == Short.MIN_VALUE)
         return "-Infinity";
      else
         return String.valueOf (s);
   }

   /**
    * Same as {@link #intToString(int)} for a long.
    *
    * @param l
    *           the long integer being formatted.
    * @return the formatted value.
    */
   public static String longToString (long l) {
      if (l == Long.MAX_VALUE)
         return "Infinity";
      else if (l == Long.MIN_VALUE)
         return "-Infinity";
      else
         return String.valueOf (l);
   }

   /**
    * Formats the number \texttt{n} into a string. If \texttt{n} is an instance
    * of wrapped primitive numeric type, the appropriate \texttt{convert} static
    * method of this class is called to perform the conversion. Otherwise,
    * {@link Number#toString} is used. This method is used to return
    * \texttt{Infinity}, and \texttt{NaN} as required, instead of large or small
    * values.
    *
    * @param n
    *           the number being formatted.
    * @return the formatted value.
    */
   public static String numberToString (Number n) {
      if (n instanceof Integer)
         return intToString (n.intValue ());
      else if (n instanceof Byte)
         return byteToString (n.byteValue ());
      else if (n instanceof Short)
         return shortToString (n.shortValue ());
      else if (n instanceof Long)
         return longToString (n.longValue ());
      else
         return n.toString ();
   }
}
