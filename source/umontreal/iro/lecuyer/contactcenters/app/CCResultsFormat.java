package umontreal.iro.lecuyer.contactcenters.app;

import java.io.OutputStream;

/**
 * Result formats available for
 * {@link PerformanceMeasureFormat#formatResults(ContactCenterEval,OutputStream,CCResultsFormat)}.
 */
public enum CCResultsFormat {
   /**
    * Results are written to the standard output.
    */
   STDOUT { @Override
   public String getFileExtension() { return null; }},
   
   /**
    * Results are written to a plain text file.
    * The file extension for this format
    * is \texttt{txt}.
    */
   TEXT { @Override
   public String getFileExtension() { return "txt"; }},

   /**
    * Results are written to a \LaTeX\ file.
    * The file extension for this format
    * is \texttt{tex}.
    */
   LATEXT { @Override
   public String getFileExtension() { return "tex"; }},
   
   /**
    * Results are converted to an instance of
    * {@link ContactCenterSimResults}, and
    * serialized into an XML file.  This results in
    * an XML file that can be loaded at a later time.
    * The file extension for this format
    * is \texttt{xml}.
    */
   XML { @Override
   public String getFileExtension() { return "xml"; }},
   
   /**
    * Results are converted to an instance of
    * {@link ContactCenterSimResults}, and
    * serialized into an XML file,
    * and compressed in the GZIP format.  This results in
    * an XML file that can be loaded at a later time.
    * The file extension for this format
    * is \texttt{xml.gz}.
    */
   XMLGZ { @Override
      public String getFileExtension () { return ".xml.gz"; }},
   
   /**
    * Results are formatted to an Excel workbook with
    * three spreadsheets.
    * The first sheet of the workbook contains
    * summary statistics, i.e., statistics
    * for aggregate performance measures.
    * The second sheet contains detailed statistics
    * concerning the complete simulation
    * (no separate periods).
    * The last sheet contains statistics
    * for all individual periods.
    * For steady-state simulations,
    * the workbook has only two sheets. 
    * The file extension for this format
    * is \texttt{xls}.
    */
   EXCEL { @Override
   public String getFileExtension() { return "xls"; }};
   
   
   /**
    * Results the file extension corresponding
    * to this format.
    * This returns \texttt{null} for
    * the standard output.
    * @return the file extension.
    */
   public abstract String getFileExtension();
   
   /**
    * Returns the format corresponding
    * to the given file name \texttt{name}.
    * This method extracts the extension of
    * the given file name, and
    * pass this extension to
    * {@link #valueOfFromFileExtension(String)}.
    * @param name the file name.
    * @return the results format.
    */
   public static CCResultsFormat valueOfFromFileName (String name) {
      for (final CCResultsFormat fmt : values()) {
         final String testExt = fmt.getFileExtension ();
         if (testExt != null && name.endsWith (testExt))
            return fmt;
      }
      return TEXT;
   }
   
   /**
    * Returns the format corresponding
    * to the given file extension \texttt{ext}.
    * This throws an illegal-argument exception if
    * the extension is unknown.
    * @param ext the file extension.
    * @return the results format.
    */
   public static CCResultsFormat valueOfFromFileExtension (String ext) {
      for (final CCResultsFormat fmt : values()) {
         final String testExt = fmt.getFileExtension ();
         if (testExt != null && testExt.equals (ext))
            return fmt;
      }
      throw new IllegalArgumentException
      ("Unrecognized file extension " + ext);
   }
   
   /**
    * Formats and returns a string containing
    * the values of this enum, separated by
    * the \texttt{|} character.
    * This string can be useful to provide
    * the user with the list of possible values
    * of this enum when passed as a command-line
    * argument.
    * @return the values of this enum, separated by \texttt{|}.
    */
   public static String getArgValues () {
      final StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (final CCResultsFormat v : values()) {
         if (first)
            first = false;
         else
            sb.append ('|');
         sb.append (v.name ());
      }
      return sb.toString ();
   }
}
