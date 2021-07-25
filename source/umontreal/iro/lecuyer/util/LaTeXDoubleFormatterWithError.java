package umontreal.iro.lecuyer.util;

/**
 * Represents a double formatter with error formatting strings for insertion
 * into a \LaTeX\ document. This formatter uses an ordinary double formatter,
 * and calls {@link LaTeXDoubleFormatter#processForLaTeX(String, String)} on the
 * returned strings. This method processes strings to be formatted in \LaTeX,
 * e.g., convert scientific notation.
 */
public class LaTeXDoubleFormatterWithError implements DoubleFormatterWithError {
   private DoubleFormatterWithError df;
   private String ensureMathCmd;

   /**
    * Constructs a new double formatter using \texttt{df}.
    * 
    * @param df
    *           the double formatter being used.
    */
   public LaTeXDoubleFormatterWithError (DoubleFormatterWithError df) {
      this (df, "ensuremath");
   }

   /**
    * Constructs a new double formatter using \texttt{df}, and the \LaTeX\
    * command given by \texttt{ensureMathCmd} to ensure math mode when
    * necessary. The default value for \texttt{ensureMathCmd} is
    * \texttt{ensuremath}.
    * 
    * @param df
    *           the double formatter being used.
    * @param ensureMathCmd
    *           the name of the ensure-math command.
    */
   public LaTeXDoubleFormatterWithError (DoubleFormatterWithError df,
         String ensureMathCmd) {
      if (df == null || ensureMathCmd == null)
         throw new NullPointerException ();
      this.df = df;
      this.ensureMathCmd = ensureMathCmd;
   }

   /**
    * Returns the double formatter used by this formatter.
    * 
    * @return the double formatter being used.
    */
   public DoubleFormatterWithError getDoubleFormatter () {
      return df;
   }

   /**
    * Sets the double formatter used by this formatter to \texttt{df}.
    * 
    * @param df
    *           the new double formatter.
    */
   public void setDoubleFormatter (DoubleFormatterWithError df) {
      if (df == null)
         throw new NullPointerException ();
      this.df = df;
   }

   /**
    * Returns the name of the math-ensuring \LaTeX\ command.
    * 
    * @return the math-ensuring command.
    */
   public String getEnsureMathCommand () {
      return ensureMathCmd;
   }

   /**
    * Sets the name of the math-ensuring \LaTeX\ command to
    * \texttt{ensureMathCmd}.
    * 
    * @param ensureMathCmd
    *           the new name of the math-ensuring command.
    */
   public void setEnsureMathCommand (String ensureMathCmd) {
      if (ensureMathCmd == null)
         throw new NullPointerException ();
      this.ensureMathCmd = ensureMathCmd;
   }

   public String format (double x, double error) {
      final String str = df.format (x, error);
      return LaTeXDoubleFormatter.processForLaTeX (str, ensureMathCmd);
   }

   public String format (double x) {
      return format (x, 0);
   }
}
