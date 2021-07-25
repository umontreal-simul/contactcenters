package umontreal.iro.lecuyer.xmlbind;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.DistributionFactory;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.probdist.PiecewiseLinearEmpiricalDist;
import umontreal.ssj.probdist.TruncatedDist;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.randvar.RandomVariateGenInt;
import umontreal.ssj.randvar.RandomVariateGenWithShift;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ClassFinderWithBase;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.xmlbind.params.AbstractProperty;
import umontreal.iro.lecuyer.xmlbind.params.BooleanArrayProperty;
import umontreal.iro.lecuyer.xmlbind.params.BooleanListProperty;
import umontreal.iro.lecuyer.xmlbind.params.BooleanProperty;
import umontreal.iro.lecuyer.xmlbind.params.DBConnectionParams;
import umontreal.iro.lecuyer.xmlbind.params.DateTimeProperty;
import umontreal.iro.lecuyer.xmlbind.params.DurationListProperty;
import umontreal.iro.lecuyer.xmlbind.params.DurationProperty;
import umontreal.iro.lecuyer.xmlbind.params.IntegerArrayProperty;
import umontreal.iro.lecuyer.xmlbind.params.IntegerListProperty;
import umontreal.iro.lecuyer.xmlbind.params.IntegerProperty;
import umontreal.iro.lecuyer.xmlbind.params.NumberArrayProperty;
import umontreal.iro.lecuyer.xmlbind.params.NumberListProperty;
import umontreal.iro.lecuyer.xmlbind.params.NumberProperty;
import umontreal.iro.lecuyer.xmlbind.params.ObjectFactory;
import umontreal.iro.lecuyer.xmlbind.params.PropertiesParams;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;
import umontreal.iro.lecuyer.xmlbind.params.StringListProperty;
import umontreal.iro.lecuyer.xmlbind.params.StringProperty;

/**
 * Defines methods that can be used to construct Java objects from some parameter
 * objects whose classes are derived by the JAXB binding compiler. Objects from
 * JAXB-derived classes have behavior; they contain data only. This class
 * provides methods to convert some complex parameter objects representing
 * maps of properties,
 * database connections, and probability distributions to more useful Java and
 * SSJ objects. It complements the {@link ArrayConverter} class which is
 * specialized for 2D arrays.
 */
public class ParamReadHelper {
   private static final ClassFinderWithBase<Distribution> cfDist = new ClassFinderWithBase<Distribution> (
         Distribution.class);
   private static final ClassFinderWithBase<RandomVariateGen> cfGen = new ClassFinderWithBase<RandomVariateGen> (
         RandomVariateGen.class);
   private static DatatypeFactory datatypeFactory;

   static {
      cfDist.getImports ().add ("umontreal.ssj.probdist.*");
      cfGen.getImports ().add ("umontreal.ssj.randvar.*");
      try {
         datatypeFactory = DatatypeFactory.newInstance ();
      }
      catch (final DatatypeConfigurationException dce) {
         throw new IllegalStateException ("Could not create data type factory");
      }
   }

   /**
    * Constructs and returns a map containing the properties stored into the
    * given
    * {@link PropertiesParams} object \texttt{prop}
    * which can be considered as
    * a list of properties.
    * For each {@link AbstractProperty} stored in \texttt{prop},
    * this method adds an entry to the returned map.
    * The name of the new entry is the name of the property while
    * the value of the entry is the value of the property.
    * The \texttt{null} value is used for properties with no values.
    * The type of the value is determined by the
    * concrete subclass of {@link AbstractProperty}.
    * The supported types are {@link Boolean}, {@link String},
    * {@link Integer}, {@link Double}, {@link Duration}, {@link Date}, and
    * arrays of these preceding types except \texttt{Integer[]},
    * \texttt{Double[]}, and \texttt{Date[]}. Arrays of numbers are converted
    * to \texttt{int[]}, and \texttt{double[]} instead.
    *
    * This method returns an empty map if \texttt{prop} is \texttt{null}.
    *
    * @param prop
    *           the properties in the XML file.
    * @return the Java Properties object.
    * @exception IllegalArgumentException
    *               if more than one property with the same name was found.
    */
   public static Map<String, Object> unmarshalProperties (PropertiesParams prop) {
      final Map<String, Object> ret = new LinkedHashMap<String, Object> ();
      if (prop == null)
         return ret;
      for (final AbstractProperty p : prop.getPropertyList ()) {
         if (p == null)
            continue;
         final String name = p.getName ();
         if (ret.containsKey (name))
            throw new IllegalArgumentException ("Property with name " + name
                  + " given more than once");

         Object value;
         if (p instanceof BooleanProperty) {
            final BooleanProperty bp = (BooleanProperty) p;
            if (bp.isSetValue ()) {
               value = bp.isValue ();
               assert value instanceof Boolean;
            }
            else
               value = null;
         }
         else if (p instanceof BooleanListProperty) {
            final BooleanListProperty bp = (BooleanListProperty) p;
            if (bp.isSetValue ())
               value = bp.getValue();
            else
               value = null;
         }
         else if (p instanceof BooleanArrayProperty) {
            final BooleanArrayProperty bp = (BooleanArrayProperty) p;
            if (bp.isSetValue ())
               value = bp.getValue();
            else
               value = null;
         }
         else if (p instanceof StringProperty) {
            final StringProperty sp = (StringProperty) p;
            if (sp.isSetValue ()) {
               value = sp.getValue ();
               assert value instanceof String;
            }
            else
               value = null;
         }
         else if (p instanceof StringListProperty) {
            final StringListProperty sp = (StringListProperty) p;
            if (sp.isSetValue ()) {
               value = ((StringListProperty) p).getValue ();
               assert value instanceof String[];
            }
            else
               value = null;
         }
         else if (p instanceof IntegerProperty) {
            final IntegerProperty intP = (IntegerProperty) p;
            if (intP.isSetValue ()) {
               value = intP.getValue ();
               assert value instanceof Integer;
            }
            else
               value = null;
         }
         else if (p instanceof IntegerListProperty) {
            final IntegerListProperty intP = (IntegerListProperty) p;
            if (intP.isSetValue ())
               value = intP.getValue();
            else
               value = null;
         }
         else if (p instanceof IntegerArrayProperty) {
            final IntegerArrayProperty intArrP = (IntegerArrayProperty) p;
            if (intArrP.isSetValue ())
               value = intArrP.getValue();
            else
               value = null;
         }
         else if (p instanceof NumberProperty) {
            final NumberProperty nP = (NumberProperty) p;
            if (nP.isSetValue ()) {
               value = nP.getValue ();
               assert value instanceof Double;
            }
            else
               value = null;
         }
         else if (p instanceof NumberListProperty) {
            final NumberListProperty numP = (NumberListProperty) p;
            if (numP.isSetValue ())
               value = numP.getValue();
            else
               value = null;
         }
         else if (p instanceof NumberArrayProperty) {
            final NumberArrayProperty numArrP = (NumberArrayProperty) p;
            if (numArrP.isSetValue ())
               value = numArrP.getValue();
            else
               value = null;
         }
         else if (p instanceof DurationProperty) {
            value = ((DurationProperty) p).getValue ();
            assert value instanceof Duration;
         }
         else if (p instanceof DurationListProperty) {
            final DurationListProperty dp = (DurationListProperty) p;
            if (dp.isSetValue ()) {
               value = dp.getValue ();
               assert value instanceof Duration[];
            }
            else
               value = null;
         }
         else if (p instanceof DateTimeProperty) {
            final DateTimeProperty dp = (DateTimeProperty) p;
            if (dp.isSetValue ()) {
               value = dp.getValue ().toGregorianCalendar ().getTime ();
               assert value instanceof Date;
            }
            else
               value = null;
         }
         else
            throw new AssertionError ("Unknown property type " + p.getClass ());
         ret.put (name, value);
      }
      return ret;
   }

   /**
    * Converts the given map of properties to a Java {@link Properties} object.
    * This method accepts a map that is usually created by
    * {@link #unmarshalProperties(PropertiesParams)}, and turns it into a
    * {@link Properties} object by converting each of its
    * non-\texttt{null} values to strings,
    * with the {@link Object#toString()} method.
    * A \texttt{null} reference as a property value results
    * in a \texttt{null} reference in the returned map.
    *
    * This method returns an empty property set if its argument is
    * \texttt{null}.
    *
    * @param prop
    *           the properties represented as a map.
    * @return the properties represented as a {@link Properties} object.
    */
   public static Properties getProperties (Map<String, Object> prop) {
      final Properties ret = new Properties ();
      if (prop == null)
         return ret;
      for (final Map.Entry<String, Object> e : prop.entrySet ()) {
         final String key = e.getKey ();
         final String value = e.getValue () == null ? null : e.getValue ()
               .toString ();
         ret.setProperty (key, value);
      }
      return ret;
   }

   /**
    * Marshals the given map into a {@link PropertiesParams} object. The result
    * of this method can be associated directly with a JAXB element of type
    * {@link PropertiesParams}. Each entry of the given map is converted into
    * an object whose class extends {@link AbstractProperty}, the exact
    * subclass depending on the class of the value. This method supports the
    * same property types as {@link #unmarshalProperties(PropertiesParams)}.
    * If a property in \texttt{prop} has a value with an unsupported
    * non-array type,
    * the value is turned into a string using its \texttt{toString} method.
    * Unsupported array types are converted to strings using
    * a mechanism similar to {@link Arrays#deepToString(Object[])}
    * but working for objects and primitive types.
    *
    * This method returns \texttt{null} if its argument is \texttt{null}.
    *
    * @param prop
    *           the Java Properties.
    * @return the Properties parameters.
    */
   public static PropertiesParams marshalProperties (Map<String, Object> prop) {
      if (prop == null || prop.isEmpty ())
         return null;
      final ObjectFactory factory = new ObjectFactory ();
      final PropertiesParams ret = factory.createPropertiesParams ();
      for (final Map.Entry<String, Object> e : prop.entrySet ()) {
         if (e.getKey () == null)
            continue;
         final String name = e.getKey ();
         final Object value = e.getValue ();
         AbstractProperty p;
         if (value == null) {
            final StringProperty sP = factory.createStringProperty ();
            p = sP;
         }
         else if (value instanceof Boolean) {
            final BooleanProperty bP = factory.createBooleanProperty ();
            bP.setValue ((Boolean) value);
            p = bP;
         }
         else if (value instanceof boolean[] || value instanceof Boolean[]) {
            final BooleanListProperty bP = factory.createBooleanListProperty ();
            if (value instanceof boolean[]) {
               final boolean[] valueArray = (boolean[]) value;
               bP.setValue (valueArray);
            }
            else {
               final Boolean[] valueArray = (Boolean[]) value;
               final boolean[] array = new boolean[valueArray.length];
               for (int i = 0; i < valueArray.length; i++)
                     array[i] = valueArray[i];
               bP.setValue (array);
            }
            p = bP;
         }
         else if (value instanceof boolean[][] || value instanceof Boolean[][]) {
            final BooleanArrayProperty bP = factory.createBooleanArrayProperty ();
            boolean[][] array = null;
            if (value instanceof boolean[][]) {
               array = (boolean[][]) value;
            }
            else {
               final Boolean[][] valueArray = (Boolean[][]) value;
               array = new boolean[valueArray.length][];
               for (int i = 0; i < valueArray.length; i++) {
                  array[i] = new boolean[valueArray[i].length];
                  for (int j = 0; j < valueArray[i].length; j++)
                     array[i][j] = valueArray[i][j].booleanValue();
               }
            }
            bP.setValue(ArrayConverter.marshalArray(array));
            p = bP;
         }
         else if (value instanceof String) {
            final StringProperty sP = factory.createStringProperty ();
            sP.setValue ((String) value);
            p = sP;
         }
         else if (value instanceof String[]) {
            final StringListProperty sP = factory.createStringListProperty ();
            sP.setValue ((String[]) value);
            p = sP;
         }
         else if (isInteger (value)) {
            final IntegerProperty iP = factory.createIntegerProperty ();
            iP.setValue (((Number) value).intValue ());
            p = iP;
         }
         else if (isIntegerArray (value)) {
            final IntegerListProperty iP = factory.createIntegerListProperty ();
            final Number[] inArray = getWrapperArray (value);
            final int[] array = new int[inArray.length];
            int idx = 0;
            for (final Number n : inArray)
               array[idx++] = n.intValue ();
            iP.setValue (array);
            p = iP;
         }
         else if (isIntegerArray2D (value)) {
            final IntegerArrayProperty iP = factory.createIntegerArrayProperty ();
            final Number[][] inArray = getWrapperArray2D (value);
            final int[][] array = new int[inArray.length][];

            for (int i = 0; i < inArray.length; i++) {
               array[i] = new int[inArray[i].length];
               for (int j = 0; j < inArray[i].length; j++)
                  array[i][j] = inArray[i][j].intValue ();
            }
            iP.setValue (ArrayConverter.marshalArray(array));
            p = iP;
         }
         else if (value instanceof Number) {
            final NumberProperty nP = factory.createNumberProperty ();
            nP.setValue (((Number) value).doubleValue ());
            p = nP;
         }
         else if (isNumberArray (value)) {
            final NumberListProperty iP = factory.createNumberListProperty ();
            final Number[] inArray = getWrapperArray (value);
            final double[] array = new double[inArray.length];
            int idx = 0;
            for (final Number n : inArray)
               array[idx++] = n.doubleValue ();
            iP.setValue (array);
            p = iP;
         }
         else if (isNumberArray2D (value)) {
            final NumberArrayProperty iP = factory.createNumberArrayProperty ();
            final Number[][] inArray = getWrapperArray2D (value);
            final double[][] array = new double[inArray.length][];

            for (int i = 0; i < inArray.length; i++) {
               array[i] = new double[inArray[i].length];
               for (int j = 0; j < inArray[i].length; j++)
                  array[i][j] = inArray[i][j].doubleValue ();
            }
            iP.setValue (ArrayConverter.marshalArray(array));
            p = iP;
         }
         else if (value instanceof Duration) {
            final DurationProperty dP = factory.createDurationProperty ();
            dP.setValue ((Duration) value);
            p = dP;
         }
         else if (value instanceof Duration[]) {
            final DurationListProperty dP = factory.createDurationListProperty ();
            dP.setValue ((Duration[]) value);
            p = dP;
         }
         else if (value instanceof Time) {
            final long millis = ((Time)value).getTime ();
            final Duration d = datatypeFactory.newDuration (millis);
            final DurationProperty dP = factory.createDurationProperty ();
            dP.setValue (d);
            p = dP;
         }
         else if (value instanceof Time[]) {
            final Time[] values = (Time[])value;
            final Duration[] list = new Duration[values.length];
            for (int i = 0; i < values.length; i++)
               list[i] = datatypeFactory.newDuration (values[i] == null ? 0 : values[i].getTime ());
            final DurationListProperty dP = factory.createDurationListProperty ();
            dP.setValue (list);
            p = dP;
         }
         else if (value instanceof XMLGregorianCalendar) {
            final DateTimeProperty dP = factory.createDateTimeProperty ();
            dP.setValue ((XMLGregorianCalendar) value);
            p = dP;
         }
         else if (value instanceof Date) {
            final DateTimeProperty dP = factory.createDateTimeProperty ();
            final GregorianCalendar gcal = new GregorianCalendar ();
            gcal.setTime ((Date) value);
            final XMLGregorianCalendar cal = datatypeFactory
                  .newXMLGregorianCalendar (gcal);
            dP.setValue (cal);
            p = dP;
         }
         else {
            final StringProperty sP = factory.createStringProperty ();
            //sP.setValue (value == null ? null : value.toString ());
            sP.setValue (toString (value));
            p = sP;
         }
         p.setName (name);
         ret.getPropertyList ().add (p);
      }
      return ret;
   }

   private static String toString (Object value) {
      if (value == null)
         return "null";
      if (value.getClass ().isArray ())
         return toStringArray (value);
      return value.toString ();
   }

   private static String toStringArray (Object value) {
      // This is similar to Arrays.deepToString in java.util,
      // but here, we abstract away the type of the array
      // by using the Array class.
      final int length = Array.getLength (value);
      if (length == 0)
         return "[]";
      StringBuilder sb = new StringBuilder ();
      sb.append ('[');
      for (int i = 0; i < length; i++) {
         if (i > 0)
            sb.append (", ");
         final Object e = Array.get (value, i);
         sb.append (toString (e));
      }
      sb.append (']');
      return sb.toString ();
   }

   private static boolean isInteger (Object n) {
      return n instanceof Integer || n instanceof BigInteger
            || n instanceof Byte || n instanceof Short || n instanceof Long;
   }

   private static boolean isIntegerArray (Object array) {
      return array instanceof Integer[] || array instanceof BigInteger[]
            || array instanceof Byte[] || array instanceof Short[]
            || array instanceof Long[] || array instanceof byte[]
            || array instanceof short[] || array instanceof int[]
            || array instanceof long[];
   }

   private static boolean isIntegerArray2D (Object array) {
      return array instanceof Integer[][] || array instanceof BigInteger[][]
            || array instanceof Byte[][] || array instanceof Short[][]
            || array instanceof Long[][] || array instanceof byte[][]
            || array instanceof short[][] || array instanceof int[][]
            || array instanceof long[][];
   }

   private static boolean isNumberArray (Object array) {
      return array instanceof Number[] ||
             array instanceof byte[]   ||
             array instanceof short[]  ||
             array instanceof int[]    ||
             array instanceof long[]   ||
             array instanceof float[]  ||
             array instanceof double[] ;
   }

   private static boolean isNumberArray2D (Object array) {
      return array instanceof Number[][] ||
             array instanceof byte[][]   ||
             array instanceof short[][]  ||
             array instanceof int[][]    ||
             array instanceof long[][]   ||
             array instanceof float[][]  ||
             array instanceof double[][];
   }

   private static Number[] getWrapperArray (Object array) {
      final int length = Array.getLength (array);
      final Number[] res = new Number[length];
      for (int i = 0; i < res.length; i++)
         res[i] = (Number) Array.get (array, i);
      return res;
   }

   // must be rectangular matrix
   private static Number[][] getWrapperArray2D (Object array) {
      Class cl = array.getClass();
      int numRow = 0;
      int numCol = 0;
      Number[][] res = new Number[0][0];

      if (cl.isArray()) {
         numRow = Array.getLength(array);
         if (numRow > 0) {
            res = new Number[numRow][];
            for (int i = 0; i < numRow; i++) {
               cl = Array.get(array, i).getClass();
               if (cl.isArray()) {
                  numCol = Array.getLength(Array.get(array, i));
                  res[i] = new Number[numCol];
                  for (int j = 0; j < numCol; j++)
                     res[i][j] = (Number) Array.get (((Object[]) array)[i], j);
               }
               else
                  res[i] = new Number[0];
            }
         }
      }
      return res;
   }

   /**
    * Creates a database connection from the parameters stored in the
    * given \texttt{dbParams} object.
    * This method first constructs a {@link Properties} object from the
    * properties in \texttt{dbParams} with the help of the
    * {@link #unmarshalProperties(PropertiesParams)}, and
    * {@link #getProperties(Map)} methods. It then checks attributes
    * in \texttt{dbParams} to decide
    * the way the connection is established. More specifically, if a JNDI name
    * is specified, i.e., {@link DBConnectionParams#isSetJndiDataSourceName()}
    * returns \texttt{true}, the properties are used as an environment to create
    * an {@link InitialContext} for JNDI, the constructed context is used to look for a
    * JDBC {@link DataSource}, and the connection is obtained.
    * Otherwise, the driver is
    * loaded if its class is not \texttt{null}, and the connection is
    * established using the URI and properties.
    *
    * @param dbParams
    *           the parameters of the connection.
    * @return the reference to an object
    * representing the established database connection.
    * @throws SQLException
    *            if a connection error occurred.
    */
   public static Connection createConnection (DBConnectionParams dbParams)
         throws SQLException {
      final Properties props;
      if (dbParams.isSetProperties ()
            && !dbParams.getProperties ().getPropertyList ().isEmpty ())
         try {
            props = getProperties (unmarshalProperties (dbParams
                  .getProperties ()));
         }
         catch (final IllegalArgumentException iae) {
            final SQLException se = new SQLException (
                  "Errror parsing connection properties");
            se.initCause (iae);
            throw se;
         }
      else
         props = null;
      if (dbParams.isSetJndiDataSourceName ()) {
         final String jndiDataSourceName = dbParams.getJndiDataSourceName ();
         try {
            final InitialContext context = new InitialContext (props);
            final DataSource source = (DataSource) context
                  .lookup (jndiDataSourceName);
            return source.getConnection ();
         }
         catch (final NamingException ne) {
            final SQLException se = new SQLException (
                  "Cannot find the data source corresponding to the JNDI name"
                        + jndiDataSourceName);
            se.initCause (ne);
            throw se;
         }
         catch (final ClassCastException cce) {
            final SQLException se = new SQLException (
                  "The object corresponding to the given name is not an instance of "
                        + DataSource.class.getName ());
            se.initCause (cce);
            throw se;
         }
      }
      else if (dbParams.isSetJdbcURI ()) {
         final String jdbcDriverClass = dbParams.getJdbcDriverClass ();
         if (jdbcDriverClass != null)
            try {
               // We do not use Class.newInstance directly, because we want
               // to catch any exception thrown by the constructor.
               Class.forName (jdbcDriverClass).getConstructor ().newInstance ();
            }
            catch (final ClassNotFoundException cne) {
               final Logger logger = Logger
                     .getLogger ("umontreal.iro.lecuyer.xmlbind");
               logger.log (Level.WARNING,
                     "Could not find the JDBC driver class with name "
                           + jdbcDriverClass
                           + "; the database connection may fail", cne);
            }
            catch (final IllegalAccessException iae) {
               final Logger logger = Logger
                     .getLogger ("umontreal.iro.lecuyer.xmlbind");
               logger.log (Level.WARNING,
                     "Could not access the JDBC driver class with name "
                           + jdbcDriverClass
                           + "; the database connection may fail", iae);
            }
            catch (final InstantiationException ie) {
               final Logger logger = Logger
                     .getLogger ("umontreal.iro.lecuyer.xmlbind");
               logger.log (Level.WARNING,
                     "Could not instantiate the JDBC driver class with name "
                           + jdbcDriverClass
                           + "; the database connection may fail", ie);
            }
            catch (final NoSuchMethodException ie) {
               final Logger logger = Logger
                     .getLogger ("umontreal.iro.lecuyer.xmlbind");
               logger.log (Level.WARNING,
                     "No empty constructor in the JDBC driver class with name "
                           + jdbcDriverClass
                           + "; the database connection may fail", ie);
            }
            catch (final InvocationTargetException ite) {
               final Logger logger = Logger
                     .getLogger ("umontreal.iro.lecuyer.xmlbind");
               logger
                     .log (
                           Level.WARNING,
                           "An error occurred when calling the constructor of the JDBC driver class with name "
                                 + jdbcDriverClass
                                 + "; the database connection may fail", ite
                                 .getCause ());
            }
         // We continue the connection even if the class loading failed, because
         // the driver class could be found another way, e.g., using SPI
         // in the case of Java 6.
         final String jdbcURI = dbParams.getJdbcURI ();
         if (props == null)
            return DriverManager.getConnection (jdbcURI.toString ());
         else
            return DriverManager.getConnection (jdbcURI.toString (), props);
      }
      else
         throw new SQLException ("No parameter for the database connection");
   }

   private static Class<? extends Distribution> getDistributionClass (
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      if (!rvgp.isSetDistributionClass ())
         throw new DistributionCreationException ("No distribution class given");
      try {
         return cfDist.findClass (rvgp.getDistributionClass ());
      }
      catch (final ClassNotFoundException cne) {
         final DistributionCreationException dce = new DistributionCreationException (
               "The string "
                     + rvgp.getDistributionClass ()
                     + " does not correspond to a fully-qualified class name, or to a class in package umontral.iro.lecuyer.probdist,"
                     + " or it maps to a class not implementing umontreal.ssj.probdist.Distribution");
         dce.initCause (cne);
         throw dce;
      }
      catch (final NameConflictException nce) {
         // This should not happen as we have fixed the import rules
         throw new AssertionError ();
      }
   }

   private static boolean isNumber (Class<?> cls) {
      if (Number.class.isAssignableFrom (cls))
         return true;
      if (!cls.isPrimitive ())
         return false;
      if (cls == char.class || cls == boolean.class)
         return false;
      return true;
   }

   /**
    * Constructs and returns an object representing the distribution based on
    * the parameters in \texttt{rvgp}. This method first resolves the class name
    * given by {@link RandomVariateGenParams#getDistributionClass()}. The
    * creation of the distribution object then depends on whether data or
    * parameters are specified in \texttt{rvgp}, which
    * is determined using {@link RandomVariateGenParams#isEstimateParameters()}.
    * The constructed distribution
    * does not take account of
    * the truncation bounds {@link RandomVariateGenParams#getLowerBound()}, and
    * {@link RandomVariateGenParams#getUpperBound()}; this
    * is considered in {@link #createTruncatedDist(Distribution, RandomVariateGenParams)}.
    * Moreover, the shift returned by
    * {@link RandomVariateGenParams#getShift()} is only used
    * for parameter estimation; it is considered by
    * {@link #createGenerator(RandomVariateGenParams, RandomStream)}.
    *
    * If {@link RandomVariateGenParams#isEstimateParameters()}
    * returns \texttt{true},
    * the result of {@link RandomVariateGenParams#getParams()}
    * is considered as an array of observations, and parameter estimation
    * is performed.  For this,
    * the array is copied into an array of
    * double-precision values. If a shift is specified using
    * {@link RandomVariateGenParams#getShift()}, it is added to each
    * observation in the intermediate array.
    * If the distribution class corresponds to
    * {@link EmpiricalDist} or {@link PiecewiseLinearEmpiricalDist}, the
    * observations are sorted, and used to construct the distribution directly.
    * Otherwise, parameter estimation is performed by using
    * {@link DistributionFactory#getDistributionMLE(Class,double[],int)}. In the
    * case of discrete distributions over the integers, each double-precision
    * observation is rounded to the nearest integer, and used for parameter
    * estimation by the method
    * {@link DistributionFactory#getDistributionMLE(Class,int[],int)}.
    *
    * If {@link RandomVariateGenParams#isEstimateParameters()}
    * returns \texttt{false}, the array returned by
    * {@link RandomVariateGenParams#getParams()} represents
    * parameters. This method then looks for a
    * constructor, in the selected distribution class, taking an array of
    * double-precision values, and calls that constructor if it exists. If such
    * a constructor cannot be found, it searches for a constructor taking $n$
    * numerical parameters, where $n$ is the number of parameters given. A
    * distribution-creation exception is thrown if no suitable constructor can
    * be found.
    *
    * @param rvgp
    *           the parameters of the random variate generator.
    * @return a reference to the object representing the distribution.
    * @throws DistributionCreationException
    *            if an exception occurred during the creation of the
    *            distribution.
    */
   public static Distribution createBasicDistribution (
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      final Class<? extends Distribution> distClass = getDistributionClass (rvgp);
      if (rvgp.isEstimateParameters ()) {
         // Parameter estimation
         double[] data = rvgp.getParams ();
         double[] shiftedData;
         if (rvgp.isSetShift ()) {
            shiftedData = new double[data.length];
            final double shift = rvgp.getShift ();
            for (int i = 0; i < data.length; i++)
               shiftedData[i] = data[i] + shift;
         }
         else
            shiftedData = data;
         data = null;
         if (distClass == EmpiricalDist.class) {
            Arrays.sort (shiftedData);
            return new EmpiricalDist (shiftedData);
         }
         else if (distClass == PiecewiseLinearEmpiricalDist.class) {
            Arrays.sort (shiftedData);
            return new PiecewiseLinearEmpiricalDist (shiftedData);
         }
         else if (DiscreteDistributionInt.class.isAssignableFrom (distClass)) {
            final int[] intData = new int[shiftedData.length];
            for (int x = 0; x < intData.length; x++)
               intData[x] = (int) Math.round (shiftedData[x]);
            Distribution dist;
            try {
               dist = DistributionFactory.getDistributionMLE (distClass
                     .asSubclass (DiscreteDistributionInt.class), intData,
                     intData.length);
            }
            catch (final IllegalArgumentException iae) {
               final DistributionCreationException dce = new DistributionCreationException (
                     distClass, null,
                     "Cannot create distribution using parameter estimation");
               dce.initCause (iae);
               throw dce;
            }
            if (dist == null)
               throw new DistributionCreationException (distClass, null,
                     "Could not create distribution using parameter estimation: the distribution class may not support this");
            return dist;
         }
         else {
            Distribution dist;
            try {
               dist = DistributionFactory.getDistributionMLE (distClass
                     .asSubclass (ContinuousDistribution.class), shiftedData,
                     shiftedData.length);
            }
            catch (final IllegalArgumentException iae) {
               final DistributionCreationException dce = new DistributionCreationException (
                     distClass, null,
                     "Cannot create distribution using parameter estimation");
               dce.initCause (iae);
               throw dce;
            }
            if (dist == null)
               throw new DistributionCreationException (distClass, null,
                     "Could not create distribution using parameter estimation");
            return dist;
         }
      }
      else {
         final double[] params = rvgp.getParams ();
         Throwable problem = null;
         for (final Constructor<?> ctor : distClass.getConstructors ()) {
            final Class<?>[] ptypes = ctor.getParameterTypes ();
            if (ptypes.length == 1 && ptypes[0] == double[].class) {
               try {
                  return (Distribution) ctor
                        .newInstance (new Object[] { params });
               }
               catch (final InvocationTargetException ite) {
                  problem = ite.getCause ();
               }
               catch (final IllegalAccessException iae) {
                  problem = iae;
               }
               catch (final InstantiationException ie) {
                  problem = ie;
               }
               // This might work with another constructor
               continue;
            }
            if (ptypes.length != params.length)
               continue;
            boolean allNumbers = true;
            for (final Class<?> pcls : ptypes)
               if (!isNumber (pcls)) {
                  allNumbers = false;
                  break;
               }
            if (!allNumbers)
               continue;

            final Object[] args = new Object[ptypes.length];
            boolean paramConvProblem = false;
            for (int i = 0; i < params.length && !paramConvProblem; i++)
               if (ptypes[i] == Byte.class || ptypes[i] == byte.class)
                  args[i] = (byte) params[i];
               else if (ptypes[i] == Short.class || ptypes[i] == short.class)
                  args[i] = (short) params[i];
               else if (ptypes[i] == Integer.class || ptypes[i] == int.class)
                  args[i] = (int) params[i];
               else if (ptypes[i] == Long.class || ptypes[i] == long.class)
                  args[i] = (long) params[i];
               else if (ptypes[i] == Float.class || ptypes[i] == float.class)
                  args[i] = (float) params[i];
               else if (ptypes[i] == Double.class || ptypes[i] == double.class)
                  args[i] = params[i];
               else if (ptypes[i] == BigInteger.class)
                  args[i] = BigInteger.valueOf ((long)params[i]);
               else if (ptypes[i] == BigDecimal.class)
                  args[i] = BigDecimal.valueOf (params[i]);
               else {
                  problem = new IllegalArgumentException ("Incompatible type "
                        + ptypes[i].getClass ().getName ());
                  paramConvProblem = true;
               }
            try {
               if (!paramConvProblem)
                  return (Distribution) ctor.newInstance (args);
            }
            catch (final InvocationTargetException ite) {
               problem = ite.getCause ();
            }
            catch (final IllegalAccessException iae) {
               problem = iae;
            }
            catch (final InstantiationException ie) {
               problem = ie;
            }
         }

         if (problem != null) {
            final DistributionCreationException dce = new DistributionCreationException (
                  distClass, params,
                  "An error occurred during call to constructor");
            dce.initCause (problem);
            throw dce;
         }
         throw new DistributionCreationException (distClass, params,
               "Cannot find a suitable constructor; check the number of specified parameters");
      }
   }

   private static Method getMethod (Class<?> cls, String name1, String name2, Class<?>... args) throws NoSuchMethodException {
      try {
         return cls.getMethod (name1, args);
      }
      catch (NoSuchMethodException nme) {
         return cls.getMethod (name2, args);
      }
   }

   /**
    * Replaces the array of observations returned by
    * {@link RandomVariateGenParams#getParams()} with an array obtained by
    * parameter estimation. Parameters are estimated a way similar to
    * {@link #createBasicDistribution(RandomVariateGenParams)}, and copied into
    * the array returned by {@link RandomVariateGenParams#getParams()}.
    * However, instead of using {@link DistributionFactory}, this method calls
    * the \texttt{getMaximumLikelohoodEstimate}
    * or \texttt{getMLE} static methods directly to get
    * the array of parameters.
    * After this method returns, the array of estimated
    * parameters can be obtained using {@link RandomVariateGenParams#getParams()}
    * while the array of observations is lost,
    * and {@link RandomVariateGenParams#isEstimateParameters()}
    * returns \texttt{false}.
    * This method does nothing if
    * {@link RandomVariateGenParams#isEstimateParameters()} returns
    * \texttt{false} for the given \texttt{rvgp} object.
    *
    * @param rvgp
    *           the random variate generator parameters.
    * @return \texttt{true} if and only if the data is replaced by estimated
    *         parameters.
    * @throws DistributionCreationException
    *            if an exception occurred when getting the distribution class.
    */
   public static boolean estimateParameters (RandomVariateGenParams rvgp)
         throws DistributionCreationException {
      if (rvgp == null)
         return false;
      if (!rvgp.isEstimateParameters ())
         return false;
      final Class<? extends Distribution> distClass = getDistributionClass (rvgp);
      if (distClass == EmpiricalDist.class
            || distClass == PiecewiseLinearEmpiricalDist.class)
         return false;
      double[] data = rvgp.getParams ();
      double[] shiftedData;
      if (rvgp.isSetShift ()) {
         shiftedData = new double[data.length];
         final double shift = rvgp.getShift ();
         for (int i = 0; i < data.length; i++)
            shiftedData[i] = data[i] + shift;
      }
      else
         shiftedData = data;
      data = null;

      double[] params;
      try {
         if (DiscreteDistributionInt.class.isAssignableFrom (distClass)) {
            final int[] intData = new int[shiftedData.length];
            for (int x = 0; x < intData.length; x++)
               intData[x] = (int) Math.round (shiftedData[x]);
            final Method mt = getMethod (distClass, "getMLE",
                  "getMaximumLikelihoodEstimate", int[].class, int.class);
            params = (double[]) mt.invoke (null, intData, intData.length);
         }
         else {
            final Method mt = getMethod (distClass, "getMLE",
                  "getMaximumLikelihoodEstimate", double[].class, int.class);
            params = (double[]) mt.invoke (null, shiftedData,
                  shiftedData.length);
         }
      }
      catch (final NoSuchMethodException nme) {
         final DistributionCreationException dce = new DistributionCreationException (
               distClass, null,
               "No appropriate method for parameter estimation");
         dce.initCause (nme);
         throw dce;
      }
      catch (final IllegalAccessException iae) {
         final DistributionCreationException dce = new DistributionCreationException (
               distClass, null,
               "Illegal access to the method for parameter estimation");
         dce.initCause (iae);
         throw dce;
      }
      catch (final InvocationTargetException ite) {
         final DistributionCreationException dce = new DistributionCreationException (
               distClass, null,
               "An error occurred during call to getMaximumLikelihoodEstimate");
         dce.initCause (ite.getCause ());
         throw dce;
      }

      rvgp.setParams (params);
      rvgp.setEstimateParameters (false);
      return true;
   }

   /**
    * Determines the mean of the distribution corresponding to the parameters
    * given by \texttt{rvgp}. This method creates the distribution, and uses
    * {@link Distribution#getMean()} to obtain the mean. It also applies the
    * shift given by {@link RandomVariateGenParams#getShift()}.
    *
    * @param rvgp
    *           the parameters of the random variate generator.
    * @return the mean.
    * @throws DistributionCreationException
    *            if a problem occurred while creating the distribution.
    */
   public static double getMean (RandomVariateGenParams rvgp)
         throws DistributionCreationException {
      final Distribution dist = createBasicDistribution (rvgp);
      double basicMean;
      if (rvgp.isSetLowerBound () || rvgp.isSetUpperBound ()) {
         final Distribution tdist = createTruncatedDist (dist, rvgp);
         basicMean = tdist.getMean ();
      }
      else
         basicMean = dist.getMean ();
      if (rvgp.isSetShift ())
         return basicMean - rvgp.getShift ();
      return basicMean;
   }

   /**
    * Determines the variance of the distribution corresponding to the
    * parameters given by \texttt{rvgp}. This method creates the distribution,
    * and uses {@link Distribution#getVariance()} to obtain the variance.
    *
    * @param rvgp
    *           the parameters of the random variate generator.
    * @return the variance.
    * @throws DistributionCreationException
    *            if a problem occurred while creating the distribution.
    */
   public static double getVariance (RandomVariateGenParams rvgp)
         throws DistributionCreationException {
      final Distribution dist = createBasicDistribution (rvgp);
      double basicVar;
      if (rvgp.isSetLowerBound () || rvgp.isSetUpperBound ()) {
         final Distribution tdist = createTruncatedDist (dist, rvgp);
         basicVar = tdist.getVariance ();
      }
      else
         basicVar = dist.getVariance ();
      return basicVar;
   }

   /**
    * Constructs and returns a truncated distribution object from distribution
    * \texttt{dist}, and parameters \texttt{tp}. This method throws a
    * distribution-creation exception if the given distribution is not
    * continuous, or if parameters are invalid.
    *
    * @param dist
    *           the distribution to truncate.
    * @param rvgp
    *           the parameters of the random variate generator.
    * @return the truncated distribution.
    * @throws DistributionCreationException
    *            if an error occurs during the construction of the distribution.
    */
   public static TruncatedDist createTruncatedDist (Distribution dist,
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      if (!(dist instanceof ContinuousDistribution))
         throw new DistributionCreationException ("Distribution " + dist
               + " is not continuous");
      try {
         final double lower = rvgp.isSetLowerBound () ?
            rvgp.getLowerBound ()
            : Double.NEGATIVE_INFINITY;
         final double upper = rvgp.isSetUpperBound () ?
               rvgp.getUpperBound () :
                  Double.POSITIVE_INFINITY;
         return new TruncatedDist ((ContinuousDistribution) dist, lower, upper);
      }
      catch (final IllegalArgumentException iae) {
         final DistributionCreationException dce = new DistributionCreationException (
               dist.getClass (), null,
               "An error occurred during the construction of the truncated distribution");
         dce.initCause (iae);
         throw dce;
      }
   }

   /**
    * Constructs and returns a random variate generator from the parameters
    * given by \texttt{rvgp}, and the random stream \texttt{stream}. This method
    * uses {@link #createBasicDistribution(RandomVariateGenParams)} to create
    * the basic distribution, and
    * {@link #createGenerator(RandomVariateGenParams,RandomStream,Distribution)}
    * to create the random variate generator.
    *
    * @param rvgp
    *           the parameters of the generator.
    * @param stream
    *           the random stream.
    * @return the random variate generator.
    * @throws DistributionCreationException
    *            if an eror occurred during the creation of the distribution.
    * @throws GeneratorCreationException
    *            if an error occurred during the creation of the generator.
    */
   public static RandomVariateGen createGenerator (RandomVariateGenParams rvgp,
         RandomStream stream) throws DistributionCreationException,
         GeneratorCreationException {
      final Distribution dist = createBasicDistribution (rvgp);
      return createGenerator (rvgp, stream, dist);
   }

   private static Class<? extends RandomVariateGen> getGeneratorClass (RandomVariateGenParams rvgp) throws GeneratorCreationException {
      if (!rvgp.isSetGeneratorClass ())
         return RandomVariateGen.class;
      try {
         return cfGen.findClass (rvgp.getGeneratorClass ());
      }
      catch (final ClassNotFoundException cne) {
         final GeneratorCreationException gne = new GeneratorCreationException (
               "The string "
                     + rvgp.getGeneratorClass ()
                     + " does not correspond to a fully-qualified class name, or to a class in package umontral.iro.lecuyer.randvar,"
                     + " or it maps to a class not extending umontreal.ssj.randvar.RandomVariateGen");
         gne.initCause (cne);
         throw gne;
      }
      catch (final NameConflictException nce) {
         // This should not happen
         throw new AssertionError ();
      }
   }

   /**
    * Constructs and returns a random variate generator from the parameters
    * given in \texttt{rvgp}, the random stream \texttt{stream}, and the
    * probability distribution \texttt{dist}. First, if truncation parameters
    * are specified, this method uses
    * {@link #createTruncatedDist(Distribution, RandomVariateGenParams)} to construct
    * the truncated distribution, and creates a random variate generator using
    * inversion. Otherwise, the method creates a generator from the class given
    * by {@link RandomVariateGenParams#getGeneratorClass()}. If no generator
    * class is specified, a {@link RandomVariateGen} is used for any
    * distribution, except discrete distributions over the integers for
    * which the method uses {@link RandomVariateGenInt}.
    * This results in using inversion if no random variate generator
    * was specified.
    * The constructed
    * generator is finally wrapped around a {@link RandomVariateGenWithShift} instance if
    * a shift was defined in \texttt{rvgp}.
    *
    * @param rvgp
    *           the random variate generator parameters.
    * @param stream
    *           the random stream.
    * @param dist
    *           the probability distribution.
    * @return the random variate generator.
    * @throws DistributionCreationException
    *            if an exception occurred during construction of the truncated
    *            distribution.
    * @throws GeneratorCreationException
    *            if an exception occurred when constructing the generator.
    */
   public static RandomVariateGen createGenerator (RandomVariateGenParams rvgp,
         RandomStream stream, Distribution dist)
         throws DistributionCreationException, GeneratorCreationException {
      RandomVariateGen rvg;
      if (rvgp.isSetLowerBound () || rvgp.isSetUpperBound ()) {
         if (rvgp.isSetGeneratorClass ()) {
            final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.xmlbind");
            logger
                  .warning ("Variates can be generated from truncated distributions only using inversion; ignoring the generatorClass attribute");
         }
         final TruncatedDist tdist = createTruncatedDist (dist, rvgp);
         rvg = new RandomVariateGen (stream, tdist);
      }
      else if (!rvgp.isSetGeneratorClass ()) {
         if (dist instanceof DiscreteDistributionInt)
            rvg = new RandomVariateGenInt (stream,
                  (DiscreteDistributionInt) dist);
         else
            rvg = new RandomVariateGen (stream, dist);
      }
      else {
         final Class<? extends RandomVariateGen> rvgClass = getGeneratorClass (rvgp);
         for (final Constructor<?> ctor : rvgClass.getConstructors ()) {
            final Class<?>[] pt = ctor.getParameterTypes ();
            if (pt.length != 2 || !pt[0].isAssignableFrom (stream.getClass ())
                  || !pt[1].isAssignableFrom (dist.getClass ()))
               continue;
            try {
               return (RandomVariateGen) ctor.newInstance (stream, dist);
            }
            catch (final IllegalAccessException iae) {}
            catch (final InstantiationException ie) {
               final GeneratorCreationException gce = new GeneratorCreationException (
                     dist, rvgClass,
                     "An instantiation exception occurred when constructing the generator of class "
                           + rvgClass.getName ());
               gce.initCause (ie);
               throw gce;
            }
            catch (final InvocationTargetException ite) {
               final GeneratorCreationException gce = new GeneratorCreationException (
                     dist, rvgClass,
                     "Exception occured during call to constructor of "
                           + rvgClass.getName ());
               gce.initCause (ite.getCause ());
               throw gce;
            }
         }
         throw new GeneratorCreationException (
               dist,
               rvgClass,
               "Cannot find a suitable constructor to create a random variate generator using distribution "
                     + dist);
      }
      if (rvgp.isSetShift ())
         return new RandomVariateGenWithShift (rvg, rvgp.getShift ());
      else
         return rvg;
   }

   private static boolean sameShift (RandomVariateGenParams rvgp1, RandomVariateGenParams rvgp2, double tol) {
      if (!rvgp1.isSetShift () && !rvgp2.isSetShift ())
         return true;
      if (rvgp1.isSetShift () && !rvgp2.isSetShift ())
         return false;
      if (rvgp2.isSetShift () && !rvgp1.isSetShift ())
         return false;
      return Math.abs (rvgp1.getShift () - rvgp2.getShift ()) < tol;
   }

   /**
    * Determines if \texttt{rvgp1} and \texttt{rvgp2} describe
    * two equivalent random variate generators. That is, if they
    * have the same distribution, generation method, and same parameters
    * within tolerance \texttt{tol}.
    * Numerical parameters are compared as follows by this method:
    * parameters $a$ and $b$ are equal if and only if
    * $|b - a|<$~\texttt{tol}.
    * @param rvgp1 the first random variate generator.
    * @param rvgp2 the second random variate generator.
    * @param tol the tolerance for comparing numbers.
    * @return \texttt{true} if and only if the generators are
    * considered to be equivalent.
    */
   public static boolean sameGenerators (RandomVariateGenParams rvgp1, RandomVariateGenParams rvgp2, double tol) {
      if (rvgp1 == null && rvgp2 == null)
         return true;
      if (rvgp1 == null || rvgp2 == null)
         return false;
      if (rvgp1.isSetDistributionClass () || rvgp2.isSetDistributionClass ())
         try {
            final Class<? extends Distribution> distClass1 = getDistributionClass (rvgp1);
            final Class<? extends Distribution> distClass2 = getDistributionClass (rvgp2);
            if (distClass1 != distClass2)
               return false;
         }
         catch (final DistributionCreationException dce) {
            return false;
         }

      if (rvgp1.isSetGeneratorClass () || rvgp2.isSetGeneratorClass ())
         try {
            final Class<? extends RandomVariateGen> genClass1 = getGeneratorClass (rvgp1);
            final Class<? extends RandomVariateGen> genClass2 = getGeneratorClass (rvgp2);
            if (genClass1 != genClass2)
               return false;
         }
         catch (final GeneratorCreationException gce) {
            return false;
         }

      if (rvgp1.isEstimateParameters () != rvgp2.isEstimateParameters ())
         return false;
      if (!sameShift (rvgp1, rvgp2, tol))
         return false;

      final double[] par1 = rvgp1.getParams ();
      final double[] par2 = rvgp2.getParams ();
      if (par1.length != par2.length)
         return false;

      for (int j = 0; j < par1.length; j++)
         if (Math.abs (par1[j] - par2[j]) >= tol)
            return false;
      return true;
   }
}
