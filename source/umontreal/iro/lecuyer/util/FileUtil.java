package umontreal.iro.lecuyer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
   private static final int BUFFERSIZE = 1024;
   private FileUtil() {}

   /**
    * Deletes the file or empty directory \texttt{f}, and
    * returns the success indicator of the operation.
    * This method is similar to {@link File#delete()},
    * except that if the first call to \texttt{delete} fails,
    * it attemps to call the method a second time after
    * 10ms.
    * This is a heuristic attempt to fix a possible race
    * condition when deleting a file under Windows XP inspired
    * from Ant's source code.
    @param f the file or empty being deleted.
    @return the success indicator of the operation.
    */
   public static boolean delete (File f) {
      if (!f.exists ())
         return false;
      if (!f.delete())
         try {
            Thread.sleep (10);
            return f.delete();
         }
         catch (final InterruptedException ie) {
            return f.delete();
         }
      return true;
   }
   
   /**
    * Copies the file \texttt{srcFile} into the destination file
    * \texttt{destFile}. If \texttt{destFile} represents a directory,
    * a new file having the same name and contents
    * as \texttt{srcFile} is created in the referred
    * directory. If \texttt{destFile}
    * represents an existing file, it is overwritten with the contents
    * of \texttt{srcFile}.
    * Otherwise, a new file is created.
    @param srcFile the source file.
    @param destFile the destination file.
    @exception IOException if a problem occurs during
    the copy.
    @exception NullPointerException if any argument is \texttt{null}.
    */
   public static void copyFile (File srcFile, File destFile) throws IOException {
      if (!srcFile.canRead())
         throw new FileNotFoundException
            ("Source file cannot be read: " + srcFile.getAbsolutePath());
      if (!srcFile.isFile())
         throw new FileNotFoundException
            ("Source file is a directory: " + srcFile.getAbsolutePath());
      File dest;
      if (destFile.isDirectory()) {
         if (!destFile.exists())
            throw new FileNotFoundException
               ("Destination directory does not exist: " + destFile.getAbsolutePath());
         dest = new File (destFile, srcFile.getName());
      }
      else {
         final File parent = destFile.getParentFile();
         if (parent != null && !parent.exists())
            throw new FileNotFoundException
               ("Destination directory does not exist: " + parent.getAbsolutePath());
         dest = destFile;
      }
      if (srcFile.equals (dest))
         return;
      
      final InputStream srcStream = new FileInputStream (srcFile);
      final OutputStream destStream = new FileOutputStream (dest);
      final byte[] buf = new byte[BUFFERSIZE];
      int rem;
      while ((rem = srcStream.read (buf)) >= 0)
         destStream.write (buf, 0, rem);
      srcStream.close ();
      destStream.close ();
   }
   
   /**
    * Moves the file \texttt{srcFile} to \texttt{destFile}.
    * This method is similar to {@link #copyFile(File, File)},
    * except it deletes the source file after the copy
    * succeeds.
    * Moreover, before making a full copy of the file,
    * the method tries to use {@link File#renameTo(File)}
    * which might rename the file without copying it
    * on some platforms.
    * The copy happens only if the call to {@link File#renameTo(File)}
    * fails. 
    * @param srcFile the source file.
    * @param destFile the destination file.
    * @throws IOException if a problem occurs during file moving.
    */
   public static void moveFile (File srcFile, File destFile) throws IOException {
      if (!srcFile.exists ())
         throw new FileNotFoundException
            ("Source file does not exist: " + srcFile.getAbsolutePath());
      if (!srcFile.isFile())
         throw new FileNotFoundException
            ("Source file is a directory: " + srcFile.getAbsolutePath());
      File dest;
      if (destFile.isDirectory()) {
         if (!destFile.exists())
            throw new FileNotFoundException
               ("Destination directory does not exist: " + destFile.getAbsolutePath());
         dest = new File (destFile, srcFile.getName());
      }
      else {
         final File parent = destFile.getParentFile();
         if (parent != null && !parent.exists())
            throw new FileNotFoundException
               ("Destination directory does not exist: " + parent.getAbsolutePath());
         dest = destFile;
      }
      if (srcFile.equals (dest))
         return;
      
      if (!srcFile.renameTo (dest)) {
         copyFile (srcFile, dest);
         if (!delete (srcFile))
            throw new IOException
            ("Could not delete source file " + srcFile.getAbsolutePath ());
      }
   }
}
