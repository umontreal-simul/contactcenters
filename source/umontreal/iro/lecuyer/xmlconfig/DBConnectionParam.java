package umontreal.iro.lecuyer.xmlconfig;

import java.net.URI;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;

/**
 * Represents the parameters for a database connection established using JDBC. A
 * connection can be established using a JNDI name corresponding to a
 * {@link DataSource}, or using a JDBC URI intended for the
 * {@link DriverManager}. Connection information is given using attributes. For
 * example, the following code gives parameters for a JDBC connection using
 * MySQL.
 * \begin{verbatim}
 * ...
 *    <db jdbcDriverClass="com.mysql.jdbc.Driver"
 *        jdbcURI="jdbc:mysql://mysql.iro.umontreal.ca/database">
 *       <property name="user" value="foo"/>
 *       <property name="password" value="bar"/>
 *    </db>
 * \end{verbatim}
 */
public class DBConnectionParam extends AbstractParam implements StorableParam,
      Cloneable {
   private String jndiDataSourceName;
   private Class<? extends Driver> jdbcDriverClass;
   private URI jdbcURI;
   private Set<PropertyParam> properties = new LinkedHashSet<PropertyParam> ();

   /**
    * Nullary constructor for the parameter reader.
    */
   public DBConnectionParam () {}

   /**
    * Constructs parameters for a JDBC connection using a data source obtained
    * via JNDI, with name \texttt{jndiDataSourceName}.
    * 
    * @param jndiDataSourceName
    *           the name of the data source.
    * @exception NullPointerException
    *               if \texttt{jndiDataSourceName} is \texttt{null}.
    */
   public DBConnectionParam (String jndiDataSourceName) {
      if (jndiDataSourceName == null)
         throw new NullPointerException ();
      this.jndiDataSourceName = jndiDataSourceName;
   }

   /**
    * Constructs parameters for a JDBC connection using the driver manager, with
    * URI \texttt{jdbcURI}, and driver class \texttt{jdbcDriverClass}.
    * 
    * @param jdbcDriverClass
    *           the driver class, can be \texttt{null}.
    * @param jdbcURI
    *           the JDBC URI.
    * @exception NullPointerException
    *               if \texttt{jdbcURI} is \texttt{null}.
    */
   public DBConnectionParam (Class<? extends Driver> jdbcDriverClass,
         URI jdbcURI) {
      if (jdbcURI == null)
         throw new NullPointerException ();
      if (jdbcDriverClass != null
            && Driver.class.isAssignableFrom (jdbcDriverClass))
         throw new IllegalArgumentException ("The given driver class, "
               + jdbcDriverClass.getName ()
               + ", does not implement the Driver interface");
      this.jdbcDriverClass = jdbcDriverClass;
      this.jdbcURI = jdbcURI;
   }

   /**
    * Creates the database connection from the parameters stored in this object.
    * This method first constructs a {@link Properties} object from the
    * properties in this object. If a JNDI name is specified, these properties
    * are used as an environment for the {@link InitialContext} constructor, the
    * constructed context is used to look for a data source, and the connection
    * is obtained. Otherwise, the driver is loaded if its class is not
    * \texttt{null}, and the connection is established using the URI and
    * properties.
    * 
    * @return the established database connection.
    * @throws SQLException
    *            if a connection error occurred.
    */
   public Connection createConnection () throws SQLException {
      Properties props = null;
      if (!properties.isEmpty ()) {
         props = new Properties ();
         for (final PropertyParam p : properties)
            props.setProperty (p.getName (), p.getValue ());
      }
      if (jndiDataSourceName != null)
         try {
            final InitialContext context = new InitialContext (props);
            final DataSource source = (DataSource) context
                  .lookup (jndiDataSourceName);
            return source.getConnection ();
         }
         catch (final NamingException ne) {
            throw (IllegalArgumentException) new IllegalArgumentException (
                  "Cannot find the data source corresponding to the JNDI name"
                        + jndiDataSourceName).initCause (ne);
         }
      else {
         if (jdbcDriverClass != null)
            try {
               jdbcDriverClass.newInstance ();
            }
            catch (final IllegalAccessException iae) {}
            catch (final InstantiationException ie) {}
         if (props == null)
            return DriverManager.getConnection (jdbcURI.toString ());
         else
            return DriverManager.getConnection (jdbcURI.toString (), props);
      }
   }

   /**
    * Returns the JDBC driver class.
    * 
    * @return the JDBC driver class.
    */
   public Class<? extends Driver> getJdbcDriverClass () {
      return jdbcDriverClass;
   }

   /**
    * Sets the JDBC driver class to \texttt{jdbcDriverClass}.
    * 
    * @param jdbcDriverClass
    *           the new JDBC driver class.
    * @exception IllegalArgumentException
    *               if the driver class is non-\texttt{null}, and does not
    *               implement the {@link Driver} interface.
    */
   public void setJdbcDriverClass (Class<? extends Driver> jdbcDriverClass) {
      if (jdbcDriverClass != null
            && !Driver.class.isAssignableFrom (jdbcDriverClass))
         throw new IllegalArgumentException ("The given driver class, "
               + jdbcDriverClass.getName ()
               + ", does not implement the Driver interface");
      this.jdbcDriverClass = jdbcDriverClass;
   }

   /**
    * Returns the JDBC URI.
    * 
    * @return the JDBC URI.
    */
   public URI getJdbcURI () {
      return jdbcURI;
   }

   /**
    * Sets the JDBC URI to \texttt{jdbcURI}.
    * 
    * @param jdbcURI
    *           the new JDBC URI.
    */
   public void setJdbcURI (URI jdbcURI) {
      this.jdbcURI = jdbcURI;
   }

   /**
    * Returns the JNDI name of the data source that will be used to obtain the
    * connection.
    * 
    * @return the JNDI name of the data source.
    */
   public String getJndiDataSourceName () {
      return jndiDataSourceName;
   }

   /**
    * Sets the JNDI name of the data source to \texttt{jndiDataSourceName}.
    * 
    * @param jndiDataSourceName
    */
   public void setJndiDataSourceName (String jndiDataSourceName) {
      this.jndiDataSourceName = jndiDataSourceName;
   }

   /**
    * Returns the properties used by this parameter object. The properties are
    * used as an environment for configuring JNDI, or as parameters to the
    * driver manager, depending on how the connection is established.
    * 
    * @return the connection properties.
    */
   public Set<PropertyParam> getProperties () {
      return properties;
   }

   /**
    * Adds a new connection property.
    * 
    * @param p
    *           the new property.
    */
   public void addProperty (PropertyParam p) {
      properties.add (p);
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName,
            properties.isEmpty (), spc);
      if (jndiDataSourceName != null)
         el.setAttribute ("jndiName", jndiDataSourceName);
      if (jdbcDriverClass != null)
         el.setAttribute ("jdbcDriverClass", finder
               .getSimpleName (jdbcDriverClass));
      if (jdbcURI != null)
         el.setAttribute ("jdbcURI", jdbcURI.toString ());
      for (final PropertyParam p : properties)
         p.toElement (finder, el, "property", spc);
      return el;
   }

   @Override
   public DBConnectionParam clone () {
      final DBConnectionParam cpy;
      try {
         cpy = (DBConnectionParam) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "CloneNotSupportedException for a class implementing Cloneable");
      }
      cpy.properties = new LinkedHashSet<PropertyParam> ();
      for (final PropertyParam p : properties)
         cpy.properties.add (p == null ? null : p.clone ());
      return cpy;
   }
}
