package umontreal.iro.lecuyer.util;

import java.util.Locale;

import umontreal.iro.lecuyer.contactcenters.app.Messages;
import umontreal.ssj.util.PrintfFormat;

/**
 * Default double formatter with error.
 * Uses
 * {@link umontreal.ssj.util.PrintfFormat#formatWithError(int, int, int, double, double, String[]) }
 * to format values with 3 significant digits.
 */
public class DefaultDoubleFormatterWithError implements
      DoubleFormatterWithError {
   private int numDigits;

   /**
    * Constructs a formatter formatting values with 3 significant digits.
    */
   public DefaultDoubleFormatterWithError () {
      numDigits = 3;
   }

   /**
    * Constructs a formatter formatting values with \texttt{numDigits}
    * significant digits.
    * 
    * @param numDigits
    *           the number of significant digits for formatting.
    */
   public DefaultDoubleFormatterWithError (int numDigits) {
      this.numDigits = numDigits;
   }

   /**
    * Returns the number of significant digits for formatting.
    * 
    * @return the number of significant digits.
    */
   public int getNumDigits () {
      return numDigits;
   }

   public String format (double x, double error) {
      if (Double.isNaN (x))
         return Messages.getString ("PerformanceMeasureFormat.NoValue"); //$NON-NLS-1$
      final double usedError;
      if (Math.abs (error) < 1e-6)
         usedError = 0;
      else
         usedError = error;
      final String[] res = new String[2];
      PrintfFormat.formatWithError (Locale.getDefault (), 8, 6, numDigits, x, usedError, res);
      return removeZeros (res[0].trim ());
   }

   public String format (double x) {
      return format (x, 0);
   }

   private static String removeZeros (String s) {
      final int idx = s.indexOf ('.');
      if (idx == -1)
         return s;
      for (int i = idx + 1; i < s.length (); i++)
         if (s.charAt (i) != '0')
            return s;
      return s.substring (0, idx);
   }
}
