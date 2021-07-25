package umontreal.iro.lecuyer.contactcenters.app;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import umontreal.iro.lecuyer.util.ExceptionUtil;

/**
 * Helper class for formatting results obtained by evaluating
 * the performance in a call center model for multiple scenarios.
 * This class provides the {@link #writeResults(File)}
 * which iterates over directories, and uncompresses ZIP files in
 * order to get files containing results.
 * For any file of results, an instance of
 * {@link ContactCenterEvalResults} is constructed, and
 * the method {@link #writeResults(String,ContactCenterEvalResults)}.
 * One needs to defines this method in a subclass in order
 * to write the relevant information extracted from the
 * object holding results.
 */
public abstract class CCResultsWriter {
   private final ContactCenterEvalResultsConverter cnv = new ContactCenterEvalResultsConverter();
   private boolean reportPropertiesToEvalInfo = false;
   
   /**
    * Constructs a new result writer.
    * This is equivalent to {@link #CCResultsWriter(boolean) CCResults\-Writer}
    * \texttt{(false)}.
    */
   public CCResultsWriter () {}
   
   /**
    * Constructs a new result writer.
    * If \texttt{reportPropertiesToEvalInfo} is
    * set to \texttt{true},
    * this copies the report properties into the evaluation information
    * for each file of results being opened.
    * This flag is necessary only for older files of results.
    * @param reportPropertiesToEvalInfo determines if report properties
    * need to be copied to evaluation information.
    */
   public CCResultsWriter (boolean reportPropertiesToEvalInfo) {
      this.reportPropertiesToEvalInfo = reportPropertiesToEvalInfo;
   }
   
   /**
    * User-defined method for writing results, for
    * a specific scenario represented by \texttt{res}, and
    * having name \texttt{resFileName}.
    * @param resFileName the result file name.
    * @param res the object holding results.
    */
   public abstract void writeResults (String resFileName, ContactCenterEvalResults res);
   
   /**
    * Writes results extracted from the file \texttt{resultFile}.
    * If the given file object corresponds to a file with name ending
    * with \texttt{.xml} or \texttt{.xml.gz}, 
    * it is opened, and
    * the parsed contents is sent to {@link #writeResults(String,ContactCenterEvalResults)}.
    * A warning is printed if there is an error while reading the file.
    * If \texttt{resultFile} corresponds to a file with the
    * \texttt{.zip} extension, the ZIP file is opened, and
    * scanned for entries whose name ends with \texttt{.xml}.
    * These entries are opened, and parsed.
    * If the given object corresponds to a directory,
    * the method is called recursively for each file
    * in the directory.
    * @param resultFile the object representing the input result file.
    */
   public void writeResults (File resultFile) {
      if (resultFile.isFile ()) {
         if (resultFile.getName ().endsWith (".xml")) {
            System.out.println ("Importing XML file " + resultFile.getName ());
            ContactCenterEvalResults res;
            try {
               res = cnv.unmarshalToEval (resultFile, reportPropertiesToEvalInfo);
            }
            catch (JAXBException je) {
               System.err.println (ExceptionUtil.throwableToString (je));
               return;
            }
            writeResults (resultFile.getName (), res);
         }
         else if (resultFile.getName ().endsWith (".xml.gz")) {
            System.out.println ("Importing GZipped XML file " + resultFile.getName ());
            ContactCenterEvalResults res;
            try {
               res = cnv.unmarshalGZippedToEval (resultFile, reportPropertiesToEvalInfo);
            }
            catch (JAXBException je) {
               System.err.println (ExceptionUtil.throwableToString (je));
               return;
            }
            writeResults (resultFile.getName (), res);
         }
         else if (resultFile.getName ().endsWith (".zip")) {
            System.out.println ("Processing zip file " + resultFile.getName ());
            ZipFile zipFile;
            try {
               zipFile = new ZipFile (resultFile);
            }
            catch (IOException ioe) {
               System.err.println (ExceptionUtil.throwableToString (ioe));
               return;
            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries ();
            while (entries.hasMoreElements ()) {
               ZipEntry e = entries.nextElement ();
               if (e.isDirectory ())
                  continue;
               if (!e.getName ().endsWith (".xml"))
                  continue;
               InputStream is;
               try {
                  is = zipFile.getInputStream (e);
               }
               catch (IOException ioe) {
                  System.err.println (ExceptionUtil.throwableToString (ioe));
                  continue;
               }
               System.out.println ("Importing zip entry " + e.getName ());
               ContactCenterEvalResults res;
               try {
                  StreamSource src = new StreamSource (is);
                  src.setPublicId (e.getName ());
                  res = cnv.unmarshalToEval (src, reportPropertiesToEvalInfo);
                  is.close ();
               }
               catch (JAXBException je) {
                  System.err.println (ExceptionUtil.throwableToString (je));
                  continue;
               }
               catch (IOException ioe) {
                  System.err.println (ExceptionUtil.throwableToString (ioe));
                  continue;
               }
               writeResults (e.getName (), res);
            }
            try {
               zipFile.close ();
            }
            catch (IOException ioe) {
               System.err.println (ExceptionUtil.throwableToString (ioe));
               return;
            }
         }
      }
      else if (resultFile.isDirectory ()) {
         System.out.println ("Processing all files in directory " + resultFile.getName ());
         for (File f : resultFile.listFiles ())
            writeResults (f);
      }
   }
}
