package umontreal.iro.lecuyer.contactcenters.app.trace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import umontreal.iro.lecuyer.contactcenters.app.params.CallTraceParams;

/**
 * Defines an exited-contact listener used to output a trace of every call
 * processed by a simulator into a text file. Each time a new contact is
 * notified to this listener, a line is appended to a writer linked to a file.
 * This results in a call-by-call trace of the simulation. If an I/O exception
 * is thrown at any given time by the writer, the exception's stack trace is
 * logged, and this call tracer is disabled to avoid getting any further
 * exception message.
 */
public class FileContactTrace implements ContactTrace {
   private final Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.app.trace");
   private PrintWriter output;
   private boolean closed = true;
   private File traceFile;
   private int timePrecision = 3;
   private String formatP;

   /**
    * Constructs a new call trace sending the information
    * to the text file \texttt{traceFile}.
    * 
    * @param traceFile
    *           the output trace file.
    * @param timePrecision
    *           the number of digits for arrival, waiting, and service times.
    */
   public FileContactTrace (File traceFile,
         int timePrecision) {
      this.traceFile = traceFile;
      this.timePrecision = timePrecision;
   }
   
   /**
    * Creates a new contact trace facility from the
    * given parameters.
    * The class of the returned object depends
    * on the given parameters, which are usually
    * read from an XML file.
    * 
    * More specifically,
    * if these parameters include information on a database
    * connection, a {@link DBContactTrace}
    * instance is returned.
    * Otherwise, if the name of the output file of the trace ends
    * with \texttt{.xls}, an
    * {@link ExcelContactTrace} is returned.
    * Otherwise, a {@link FileContactTrace}
    * is returned.
    * @param traceParams the parameters of the trace.
    * @return the contact trace facility.
    */
   public static ContactTrace create (CallTraceParams traceParams) {
      if (traceParams == null)
         return null;
      if (traceParams.getDatabase () != null
            && traceParams.getTableName () != null)
         return new DBContactTrace (traceParams.getDatabase (), traceParams.getTableName ());
      final boolean outputExcel = traceParams.getOutputFileName().endsWith (".xls");
      if (outputExcel)
         return new ExcelContactTrace (new File (traceParams.getOutputFileName ()), traceParams.getSheetName ());
      return new FileContactTrace (new File (traceParams.getOutputFileName ()), traceParams.getNumDigits ());
   }
   
   public void init () {
      if (traceFile.exists ()) {
         // Select a new name
         final String name = traceFile.getName ();
         final int idx = name.lastIndexOf ('.');
         int n = 1;
         File outputFile2 = null;
         while (outputFile2 == null || outputFile2.exists ()) {
            final String name2 = idx == -1 ? name + n++ : name.substring (0, idx) + n++ + name.substring (idx);
            outputFile2 = new File (traceFile.getParentFile (), name2);
         }
         traceFile = outputFile2;
         logger.warning ("Writing trace to file " + traceFile.getName());
      }
      try {
         output = new PrintWriter (new BufferedWriter (new FileWriter (
               traceFile)));
         closed = false;
         writeHeader ();
      }
      catch (final IOException ioe) {
         logger.log (Level.WARNING, "Cannot output call-by-call trace", ioe);
      }
   }

   public void close () {
      output.close ();
      // catch (final IOException ioe) {
      // logger.log (Level.WARNING, "Error while closing file", ioe);
      // }
      closed = true;
   }

   /**
    * Returns a string containing the field names for this trace. This string
    * can be written as an header at the beginning of trace files.
    * 
    * @return the header string.
    */
   public String getHeader () {
      return getHeader (timePrecision);
   }

   public static String getHeader (int timePrecision) {
      final String timeP = "%" + (7 + timePrecision) + "s";
      return String.format ("  Step    Type  Period  " + timeP + "  " + timeP
            + "    Outcome   Group  " + timeP, "ArvTime", "QueueTime",
            "SrvTime");
   }
   
   /**
    * Writes the output of {@link #getHeader} in the trace file, followed by an
    * end-line character.
    */
   public void writeHeader () {
      if (closed)
         return;
      output.println (getHeader ());
      // catch (final IOException ioe) {
      // logger.logp (Level.WARNING, "FileCallTrace", "writeHeader",
      // "Call-by-call trace disabled", ioe);
      // close ();
      // }
   }

   public void writeLine (int step, int type, int period, double arvTime,
         double queueTime, String outcome, int group, double srvTime) {
      if (closed)
         return;
      output.printf (Locale.US, getFormattingPattern (), step, type, period,
            arvTime, queueTime, outcome, group, srvTime);
      output.println ();
   }
   
   private String getFormattingPattern () {
      if (formatP == null) {
         final StringBuilder sb = new StringBuilder ();
         sb.append ('%').append (7 + timePrecision).append ('.').append (
               timePrecision).append ('f');
         final String timeP = sb.toString ();
         sb.delete (0, sb.length ());
         sb.append ("%6d  %6d  %6d  ").append (timeP).append ("  ").append (
               timeP).append ("  %9s  %6d  ").append (timeP);
         formatP = sb.toString ();
      }
      return formatP;
   }
}
