package umontreal.iro.lecuyer.contactcenters.app.trace;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import umontreal.iro.lecuyer.util.ModifiableWorkbook;

/**
 * Outputs trace to an Excel spreadsheet using
 * JExcel API.
 */
public class ExcelContactTrace implements ContactTrace {
   private final Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.app.trace");
   private ModifiableWorkbook mwb;
   private WritableSheet sheet;
   private int currentRow;
   private boolean closed = true;
   private File traceFile;
   private String sheetName;

   /**
    * Creates a new call trace to a spreadsheet
    * \texttt{sheetName} in an Excel file named
    * \texttt{traceFile}.
    * 
    * @param traceFile
    *           the output trace file.
    * @param sheetName
    * the name of the sheet name containing the trace. 
    */
   public ExcelContactTrace (File traceFile, String sheetName) {
      this.traceFile = traceFile;
      this.sheetName = sheetName;
   }

   public void init () {
      try {
         mwb = new ModifiableWorkbook (traceFile);
      }
      catch (final IOException e) {
         logger.throwing (ExcelContactTrace.class.getName (), "init", e);
         logger.warning ("Could not read the existing workbook " + traceFile.getName ());
      }
      catch (final BiffException e) {
         logger.throwing (ExcelContactTrace.class.getName (), "init", e);
         logger.warning ("Could not read the existing workbook " + traceFile.getName ());
      }

      if (mwb == null) {
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
         try {
            mwb = new ModifiableWorkbook (traceFile);
         }
         catch (final IOException ioe) {
            logger.warning ("Could not create trace file");
            return;
         }
         catch (final BiffException be) {
            logger.warning ("Could not create trace file");
            return;
         }
      }
      newSheet ();
      closed = false;
      currentRow = 0;
      writeHeader ();
   }
   
   private void newSheet() {
      WritableWorkbook wb = mwb.getWorkbook ();
      if (wb.getSheet (sheetName) == null) 
         sheet = wb.createSheet (sheetName, wb.getNumberOfSheets ());
      else {
         int n = 1;
         sheet = null;
         while (sheet == null) {
            final String sn = sheetName + "_" + n++;
            if (wb.getSheet (sn) == null) {
               logger.warning ("Creating a new sheet with name " + sn);
               sheet = wb.createSheet (sn, wb.getNumberOfSheets ());
            }
         }
      }
   }

   public void close () {
      try {
         mwb.close ();
      }
      catch (final IOException ioe) {
         logger.log (Level.WARNING, "Error while writing file", ioe);
      }
      catch (final WriteException we) {
         logger.log (Level.WARNING, "Error while writing file", we);
      }
      mwb = null;
      sheet = null;
      closed = true;
   }
   
   private static final String[] FIELDS = {
      "Step",
      "Type",
      "Period",
      "ArvTime",
      "QueueTime",
      "Outcome",
      "Group",
      "SrvTime"
   };

   public void writeHeader () {
      if (closed)
         return;
      short col = 0;
      for (final String f : FIELDS) {
         final Label label = new Label (col++, currentRow, f);
         try {
            sheet.addCell (label);
         }
         catch (final WriteException we) {
            logger.warning ("Could not write header row");
            close();
         }
      }
      ++currentRow;
   }
   
   public void writeLine (int step, int type, int period,
         double arvTime, double queueTime, String outcome,
         int group, double srvTime) {
      if (closed)
         return;
      if (currentRow == 65535) {
         newSheet ();
         currentRow = 0;
         writeHeader ();
      }
      try {
         jxl.write.Number cell = new jxl.write.Number (0, currentRow, step);
         sheet.addCell (cell);
         cell = new jxl.write.Number (1, currentRow, type);
         sheet.addCell (cell);
         cell = new jxl.write.Number (2, currentRow, period);
         sheet.addCell (cell);
         cell = new jxl.write.Number (3, currentRow, arvTime);
         sheet.addCell (cell);
         cell = new jxl.write.Number (4, currentRow, queueTime);
         sheet.addCell (cell);
         final Label cell2 = new Label (5, currentRow, outcome);
         sheet.addCell (cell2);
         cell = new jxl.write.Number (6, currentRow, group);
         sheet.addCell (cell);
         cell = new jxl.write.Number (7, currentRow, srvTime);
         sheet.addCell (cell);
         ++currentRow;
      }
      catch (final WriteException we) {
         logger.warning ("Could not write trace line");
         close();
      }
   }
}
