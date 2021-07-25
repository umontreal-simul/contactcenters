package umontreal.iro.lecuyer.util;

import java.io.File;
import java.io.IOException;

import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Encapsulates a workbook from JExcel API that can be created from
 * an already existing file, modified, and written back to the input file.
 * JExcel API can be used to
 * read workbooks compatible with
 * Microsoft's Excel, or create workbooks from scratch.
 * However, modifying already existing workbooks
 * requires the original workbook to be read, changed
 * in a temporary file, and copied back to the
 * original location.
 * This class takes care of these steps.
 * After an instance is constructed using a file object,
 * the method {@link #getWorkbook()} can be used
 * to get the currently opened workbook.
 * The {@link #close()} method closes the workbook.  
 *
 */
public class ModifiableWorkbook {
   private Workbook inwb;
   private WritableWorkbook wb;
   private File outputFile;
   private File tmpFile;
   
   /**
    * Constructs a new modifiable workbook from
    * the Excel file referred to by \texttt{outputFile}.
    * If the given file does not exists, this method
    * creates a new workbook.
    * Otherwise, it reads the existing workbook, and
    * creates a temporary file containing a new workbook
    * starting with the existing one.
    * @param outputFile the output file.
    * @throws IOException if an error occurs when reading
    * the output file.
    * @throws BiffException if an error occurs
    * while parsing an existing workbook.
    */
   public ModifiableWorkbook (File outputFile) throws IOException, BiffException {
      if (outputFile.exists ()) {
         inwb = Workbook.getWorkbook (outputFile);
         tmpFile = File.createTempFile ("wrtmp", ".xls");
         wb = Workbook.createWorkbook (tmpFile, inwb);
      }
      else
         wb = Workbook.createWorkbook (outputFile);      
      this.outputFile = outputFile;
   }
   
   /**
    * Similar to the constructor {@link #ModifiableWorkbook(File)},
    * with a string instead of a file object.
    */
   public ModifiableWorkbook (String outputFile) throws IOException, BiffException {
      this (new File (outputFile));
   }
   
   /**
    * Returns a reference to the encapsulated workbook.
    * @throws IllegalStateException if this object is closed.
    */
   public WritableWorkbook getWorkbook() {
      if (wb == null)
         throw new IllegalStateException
         ("Workbook closed");
      return wb;
   }
   
   /**
    * Returns a reference to the file object representing
    * the output file.
    * @throws IllegalStateException if this object is closed.
    */
   public File getOutputFile() {
      if (wb == null)
         throw new IllegalStateException
         ("Workbook closed");
      return outputFile;
   }
   
   /**
    * Closes the encapsulated workbook.
    * This method calls {@link WritableWorkbook#write()}
    * followed by {@link WritableWorkbook#close()} on
    * the encapsulated workbook returned by {@link #getWorkbook()}.
    * If the workbook was created from another one,
    * the temporary file containing the written
    * workbook is copied back
    * to the output file returned by
    * {@link #getOutputFile()}.
    * @throws WriteException if an error occurs while
    * formating the encapsulated workbook.
    * @throws IOException if an error occurs while
    * writing the encapsulated workbook to disk.
    */
   public void close() throws WriteException, IOException {
      if (wb == null)
         return;
      wb.write ();
      wb.close ();
      wb = null;
      if (tmpFile != null) {
         inwb.close ();
         inwb = null;
         outputFile.delete ();
         FileUtil.moveFile (tmpFile, outputFile);
      }
   }
   
   /**
    * Discards all the changes made in the encapsulated workbook.
    * This method closes the encapsulated workbook, and
    * deletes the temporary files that contains it.
    * If the workbook was created from an existing file,
    * the existing file is unchanged.
    * @throws WriteException if an error occurs while closing the workbook.
    * @throws IOException if an error occurs while closing the workbook.
    */
   public void discardChanges() throws WriteException, IOException {
      wb.write ();
      wb.close ();
      wb = null;
      if (tmpFile == null)
         outputFile.delete ();
      else {
         inwb.close ();
         tmpFile.delete ();
      }
   }

   /**
    * This method calls the {@link #close()} method.
    */
   @Override
   protected void finalize () throws Throwable {
      close();
   }
}
