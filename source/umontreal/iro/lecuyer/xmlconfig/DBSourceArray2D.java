package umontreal.iro.lecuyer.xmlconfig;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.JDBCManager;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a source array whose data is
 * extracted from a database using JDBC.
 * The elements of the array are obtained by
 * performing a query on a database.
 * Each row of the resulting result set
 * is a row in the source array, while each column
 * corresponding to a field of the result set
 * becomes a column in the array.
 * The JDBC connection is initialized using
 * {@link DBConnectionParam}, and the
 * result set is converted into an array
 * of objects using
 * {@link JDBCManager#readObjectData2D(Connection,String)}.
 * Any numeric object (instances of {@link Number})
 * is converted to the target class while
 * other objects not corresponding to the
 * target class are converted to string before
 * they are passed to
 * {@link StringConvert#fromString(URI,ClassFinder,Class, String)}.
 * 
 * In a XML file, the \texttt{dataQuery} attribute
 * of an element representing a database-based source
 * array
 * is used to specify the query on the database.
 * The \texttt{database} nested element
 * is then used to describe the connection
 * to the database.
 */
public class DBSourceArray2D extends AbstractParam implements SourceArray2D,
      Cloneable, StorableParam {
   private Object[][] data;
   private DBConnectionParam dbParams;
   private String dataQuery;

   /**
    * Returns the parameters of the database connection used to obtain data for
    * this source array.
    * 
    * @return the database connection parameters.
    */
   public DBConnectionParam getDatabase() {
      return dbParams;
   }
   
   /**
    * Sets the parameters of the database connection to \texttt{dbParams}.
    * 
    * @param dbParams
    *           the parameters for the database connection.
    */
   public void setDatabase (DBConnectionParam dbParams) {
      this.dbParams = dbParams;
   }
   
   /**
    * Returns the SQL query used to obtain data for the element in the source array.
    * The query, e.g., \texttt{SELECT Column FROM Table}, is made on
    * the database whose parameters are given by {@link #getDatabase()}.
    * 
    * @return the SQL query for data.
    */
   public String getDataQuery() {
      return dataQuery;
   }
   
   /**
    * Sets the SQL query for data to \texttt{dataQuery}.
    * 
    * @param dataQuery
    *           the SQL query for data.
    */
   public void setDataQuery (String dataQuery) {
      this.dataQuery = dataQuery;
   }

   public int columns (int row) {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      if (row < 0)
         throw new IllegalArgumentException
         ("Negative row index");
      return data[row].length;
   }

   public void dispose () {
      data = null;
   }

   @SuppressWarnings("unchecked")
   public <T> T get (Class<T> pcls, int row, int column) throws UnsupportedConversionException {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      final Object e = data[row][column];
      if (e == null)
         return null;
      if (pcls.isAssignableFrom (e.getClass ()))
         return (T)e;
      if (e instanceof Number) {
         final Number ne = (Number)e;
         if (pcls == double.class || pcls == Double.class)
            return (T)new Double (ne.doubleValue ());
         else if (pcls == float.class || pcls == Float.class)
            return (T)new Float (ne.floatValue ());
         else if (pcls == int.class || pcls == Integer.class)
            return (T)new Integer (ne.intValue ());
         else if (pcls == long.class || pcls == Long.class)
            return (T)new Long (ne.longValue ());
         else if (pcls == byte.class || pcls == Byte.class)
            return (T)new Byte (ne.byteValue ());
         else if (pcls == short.class || pcls == Short.class)
            return (T)new Short (ne.shortValue ());
         else if (pcls == BigInteger.class) {
            if (e instanceof BigDecimal)
               return (T)((BigDecimal)e).toBigInteger ();
            return (T)BigInteger.valueOf (ne.longValue ());
         }
         else if (pcls == BigDecimal.class) {
            if (e instanceof BigInteger)
               return (T)new BigDecimal ((BigInteger)e);
            return (T)new BigDecimal (ne.doubleValue ());
         }
      }
      final String str = e.toString ();
      try {
         return StringConvert.fromString (null, null, pcls, str);
      }
      catch (final NameConflictException nce) {
         final UnsupportedConversionException iae = new UnsupportedConversionException
         ("Cannot convert value at (" + row + ", " + column + ")=" + data[row][column]);
         iae.initCause (nce);
         throw iae;
      }
   }

   /**
    * Initializes the source array by
    * performing the SQL query.
    * This method
    * establishes the connection to the
    * database using JDBC,
    * issues the SQL query, and
    * copies the elements of the result set
    * in an internal 2D array.
    * The resulting internal array, which
    * is the contents of the source array,
    * is a snapshot of the
    * result of the query; it is not
    * updated automatically if the database
    * changes. 
    */
   public void init () {
      if (dataQuery == null)
         throw new IllegalStateException
         ("No dataQuery specified");
      try {
         final Connection conn = dbParams.createConnection ();
         data = JDBCManager.readObjectData2D (conn, dataQuery);
         conn.close ();
      }
      catch (final SQLException e) {
         throw new ParamReadException
         ("Cannot read data from the database");
      }
   }

   public int rows () {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      return data.length;
   }
   
   /**
    * For internal use only.
    */
   public void addDatabase (DBConnectionParam dbParams1) {
      setDatabase (dbParams1);
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName, dbParams == null, spc);
      if (dataQuery != null)
         el.setAttribute ("dataQuery", String.valueOf (dataQuery));
      if (dbParams != null)
         dbParams.toElement (finder, el, "database", spc);
      return el;
   }

   @Override
   public DBSourceArray2D clone() {
      DBSourceArray2D cpy;
      try {
         cpy = (DBSourceArray2D)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError
         ("Clone not supported for a class implementing Cloneable");
      }
      if (dbParams != null)
         cpy.dbParams = dbParams.clone ();
      return cpy;
   }
   
   public String getElementName() {
      return "DB";
   }
}
