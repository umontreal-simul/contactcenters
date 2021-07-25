package umontreal.iro.lecuyer.util;

/**
 * Represents a double formatter formatting strings for insertion into a \LaTeX\
 * document. This formatter uses an ordinary double formatter, and calls
 * {@link #processForLaTeX(String, String)} on the returned strings. This method
 * processes strings to be formatted in \LaTeX, e.g., convert scientific
 * notation.
 */
public class LaTeXDoubleFormatter implements DoubleFormatter {
   private DoubleFormatter df;
   private String ensureMathCmd;

   /**
    * Constructs a new double formatter using \texttt{df}.
    * 
    * @param df
    *           the double formatter being used.
    */
   public LaTeXDoubleFormatter (DoubleFormatter df) {
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
   public LaTeXDoubleFormatter (DoubleFormatter df, String ensureMathCmd) {
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
   public DoubleFormatter getDoubleFormatter () {
      return df;
   }

   /**
    * Sets the double formatter used by this formatter to \texttt{df}.
    * 
    * @param df
    *           the new double formatter.
    */
   public void setDoubleFormatter (DoubleFormatter df) {
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

   public String format (double x) {
      final String str = df.format (x);
      return processForLaTeX (str, ensureMathCmd);
   }

   /**
    * Prepares the string \texttt{str} to be formatted in \LaTeX, and returns
    * the processed string. This method converts \texttt{E} and \texttt{e} to
    * \texttt{10\^{}}. When math mode needs to be ensured, e.g., when the string
    * contains \texttt{\^{}}, the string is prepended with a backslash, the
    * string returned by \texttt{ensureMathCmd}, and an opening brace, and A
    * matching closing brace is added at the end of the string.
    * 
    * @param str
    *           the string being processed.
    * @param ensureMathCmd
    *           the command to ensure math mode.
    * @return the processed string.
    */
   public static String processForLaTeX (String str, String ensureMathCmd) {
      int idx = str.indexOf ('E');
      if (idx == -1)
         idx = str.indexOf ('e');
      boolean mathMode = false;
      final String res;
      if (idx != -1) {
         res = str.substring (0, idx) + "*10^{" + str.substring (idx + 1) + "}";
         mathMode = true;
      }
      else
         res = str;
      for (int i = 0; i < res.length () && !mathMode; i++) {
         final char ch = res.charAt (i);
         if (ch == '^' || ch == '_')
            mathMode = true;
      }
      if (mathMode)
         return "\\" + ensureMathCmd + "{" + res + "}";
      return res;
   }
}
