package umontreal.iro.lecuyer.contactcenters.app.trace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import umontreal.ssj.util.JDBCManager;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;
import umontreal.iro.lecuyer.xmlbind.params.DBConnectionParams;

/**
 * Defines an exited-contact listener used to output a trace of every call
 * processed by a simulator into a database. Each time a new contact is notified
 * to this listener, a SQL request is used to update a table through JDBC. This
 * results in a call-by-call trace of the simulation. If an SQL exception is
 * thrown at any given time by the writer, the exception's stack trace is
 * printed, and this call logger is disabled to avoid getting any further
 * exception message.
 */
public class DBContactTrace implements ContactTrace {
   private final Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.app.trace");
   private Connection connection;
   private PreparedStatement pstmt;
   private boolean closed = true;
   private DBConnectionParams dbProperties;
   private String dbTable;

   /**
    * Constructs a new call trace to a database, using
    * the given parameters to establish the connection, and
    * and sending the data to the table with the given name.
    * 
    * @param dbProperties
    *           the database properties, for {@link JDBCManager}.
    * @param dbTable
    *           the output table for the trace.
    */
   public DBContactTrace (DBConnectionParams dbProperties, String dbTable) {
      this.dbProperties = dbProperties;
      this.dbTable = dbTable;
   }

   public void init () {
      try {
         connection = ParamReadHelper.createConnection (dbProperties);
         pstmt = connection.prepareStatement ("INSERT INTO " + dbTable
               + " VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
         if (pstmt == null)
            throw new IllegalArgumentException (
                  "The JDBC driver cannot create prepared statements");
      }
      catch (final SQLException e) {
         logger.log (Level.WARNING, "Error while opening database connection",
               e);
         close ();
         return;
      }

      try {
         final Statement stmt = connection.createStatement ();
         stmt.executeUpdate ("DELETE FROM " + dbTable);
         stmt.close ();
      }
      catch (final SQLException e) {
         logger.log (Level.WARNING, "Error while clearing the table", e);
      }
      closed = false;
   }

   public void close () {
      try {
         if (pstmt != null)
            pstmt.close ();
         if (connection != null)
            connection.close ();
      }
      catch (final SQLException e) {
         logger.log (Level.WARNING,
               "Error while closing the database connection", e);
      }
      closed = true;
   }
   
   public void writeLine (int step, int type, int period, double arvTime,
         double queueTime, String outcome, int group, double srvTime) {
      if (closed)
         return;
      try {
         pstmt.setInt (1, step);
         pstmt.setInt (2, type);
         pstmt.setInt (3, period);
         pstmt.setDouble (4, arvTime);
         pstmt.setDouble (5, queueTime);
         pstmt.setString (6, outcome);
         pstmt.setInt (7, group);
         pstmt.setDouble (8, srvTime);
         pstmt.executeUpdate ();
      }
      catch (final SQLException e) {
         logger.log (Level.WARNING, "Error while tracing blocked call", e);
         close ();
      }
   }
}
