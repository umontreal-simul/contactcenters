package umontreal.iro.lecuyer.xmlbind;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.JDBCManager;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;
import umontreal.iro.lecuyer.xmlbind.params.DBConnectionParams;
import umontreal.iro.lecuyer.xmlconfig.DBConnectionParam;

/**
 * Represents a source array whose data is extracted from a database using JDBC.
 * The elements of the array are obtained by performing a query on a database.
 * Each row of the resulting result set is a row in the source array, while each
 * column corresponding to a field of the result set becomes a column in the
 * array. The JDBC connection is initialized using {@link DBConnectionParam},
 * and the result set is converted into an array of objects using
 * {@link JDBCManager#readObjectData2D(Connection,String)}. Any numeric object
 * (instances of {@link Number}) is converted to the target class while other
 * objects not corresponding to the target class are converted to string before
 * they are passed to
 * {@link StringConvert#fromString(URI,ClassFinder,Class, String)}.
 * 
 * In a XML file, the \texttt{dataQuery} attribute of an element representing a
 * database-based source array is used to specify the query on the database. The
 * \texttt{database} nested element is then used to describe the connection to
 * the database.
 */
public class DBSourceArray2D {
   private static JAXBContext context;
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.xmlbind");
   private DBConnectionParams dbParams;
   private Connection conn;

   public DBSourceArray2D (DBConnectionParams dbParams) {
      if (dbParams == null)
         throw new NullPointerException ();
      this.dbParams = dbParams;
      init ();
   }

   public DBSourceArray2D (Node node) {
      Object o;
      try {
         if (context == null)
            context = JAXBContext
                  .newInstance ("umontreal.iro.lecuyer.xmlbind.params");
         final Unmarshaller um = context.createUnmarshaller ();
         o = um.unmarshal (node);
      }
      catch (final JAXBException je) {
         final IllegalArgumentException iae = new IllegalArgumentException (
               "Error parsing the database element");
         iae.initCause (je);
         throw iae;
      }
      final JAXBElement<?> el = (JAXBElement<?>) o;
      dbParams = (DBConnectionParams) el.getValue ();
      init ();
   }

   private void init () {
      try {
         conn = ParamReadHelper.createConnection (dbParams);
      }
      catch (final SQLException e) {
         final IllegalArgumentException iae = new IllegalArgumentException (
               "Cannot establish connection to database");
         iae.initCause (e);
         throw iae;
      }
   }

   /**
    * Returns the parameters of the database connection used to obtain data for
    * this source array.
    * 
    * @return the database connection parameters.
    */
   public DBConnectionParams getDatabase () {
      return dbParams;
   }

   /**
    * Returns the connection to the database.
    * 
    * @return the current connection.
    */
   public Connection getConnection () {
      return conn;
   }

   public void close () {
      try {
         conn.close ();
      }
      catch (final SQLException e) {
         logger.log (Level.INFO, "Cannot close database connection", e);
      }
   }

   public SourceArray2D getQuery (String dataQuery) {
      return new DBQueryArray (conn, dataQuery);
   }

   private static class DBQueryArray implements SourceArray2D {
      private Object[][] data;

      public DBQueryArray (Connection conn, String dataQuery) {
         if (dataQuery == null)
            throw new IllegalStateException ("No dataQuery specified");
         try {
            data = JDBCManager.readObjectData2D (conn, dataQuery);
            conn.close ();
         }
         catch (final SQLException e) {
            final IllegalArgumentException iae = new IllegalArgumentException (
                  "Cannot read data from the database");
            iae.initCause (e);
            throw iae;
         }
      }

      public int columns (int row) {
         if (data == null)
            throw new IllegalStateException ("Uninitialized matrix");
         if (row < 0)
            throw new IllegalArgumentException ("Negative row index");
         return data[row].length;
      }

      public void close () {
         data = null;
      }

      @SuppressWarnings ("unchecked")
      public <T> T get (Class<T> pcls, int row, int column) {
         if (data == null)
            throw new IllegalStateException ("Uninitialized matrix");
         final Object e = data[row][column];
         if (e == null)
            return null;
         if (pcls.isAssignableFrom (e.getClass ()))
            return (T) e;
         if (e instanceof Number) {
            final Number ne = (Number) e;
            if (pcls == double.class || pcls == Double.class)
               return (T) new Double (ne.doubleValue ());
            else if (pcls == float.class || pcls == Float.class)
               return (T) new Float (ne.floatValue ());
            else if (pcls == int.class || pcls == Integer.class)
               return (T) new Integer (ne.intValue ());
            else if (pcls == long.class || pcls == Long.class)
               return (T) new Long (ne.longValue ());
            else if (pcls == byte.class || pcls == Byte.class)
               return (T) new Byte (ne.byteValue ());
            else if (pcls == short.class || pcls == Short.class)
               return (T) new Short (ne.shortValue ());
            else if (pcls == BigInteger.class) {
               if (e instanceof BigDecimal)
                  return (T) ((BigDecimal) e).toBigInteger ();
               return (T) BigInteger.valueOf (ne.longValue ());
            }
            else if (pcls == BigDecimal.class) {
               if (e instanceof BigInteger)
                  return (T) new BigDecimal ((BigInteger) e);
               return (T) new BigDecimal (ne.doubleValue ());
            }
         }
         final String str = e.toString ();
         try {
            return StringConvert.fromString (null, null, pcls, str);
         }
         catch (final NameConflictException nce) {
            final IllegalArgumentException iae = new IllegalArgumentException (
                  "Cannot convert value at (" + row + ", " + column + ")="
                        + data[row][column]);
            iae.initCause (nce);
            throw iae;
         }
         catch (final UnsupportedConversionException uce) {
            final ClassCastException cce = new ClassCastException
            ("Cannot convert value at (" + row + ", " + column + ")="
            + data[row][column]);
            cce.initCause (uce);
            throw cce;
         }
      }

      public int rows () {
         if (data == null)
            throw new IllegalStateException ("Uninitialized matrix");
         return data.length;
      }
   }
}
