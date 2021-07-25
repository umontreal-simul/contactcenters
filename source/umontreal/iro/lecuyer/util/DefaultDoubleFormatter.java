package umontreal.iro.lecuyer.util;

import java.util.Locale;

import umontreal.iro.lecuyer.contactcenters.app.Messages;

/**
 * Default formatter.
 * Let $x$ be a value to be formatted. Let $d_1$ be the number of needed
 * significant digits for $|x|\le 1$, and $d_2$ be the number of digits for
 * $|x|>1$. If $x\ge 10^{d_1}$, the formatter uses {@link umontreal.ssj.util.PrintfFormat#f} with 0
 * decimal digit of precision. If $x < 10^{-d_2}$, the formatter uses
 * {@link umontreal.ssj.util.PrintfFormat#e} with $d_2$ digits of precision. Otherwise, the
 * formatter uses {@link umontreal.ssj.util.PrintfFormat#g} with $d_1$ or $d_2$ significant digits,
 * depending if $|x|$ is smaller than or equal to 1, or greater than 1.
 */
public class DefaultDoubleFormatter implements DoubleFormatter {
   private int digitsSmall;
   private int digitsLarge;

   private double largeBound;
   private double smallBound;

   /**
    * Constructs a formatter that uses 3 significant digits for all numbers.
    */
   public DefaultDoubleFormatter () {
      this (3, 3);
   }

   /**
    * Constructs a formatter that uses \texttt{digitsSmall} significant digits
    * for values smaller than 1, and \texttt{digitsLarge} significant digits for
    * other values.
    * 
    * @param digitsSmall
    *           the number of significant digits for values smaller than 1.
    * @param digitsLarge
    *           the number of significant digits for values greater than or
    *           equal to 1.
    */
   public DefaultDoubleFormatter (int digitsSmall, int digitsLarge) {
      this.digitsSmall = digitsSmall;
      this.digitsLarge = digitsLarge;
      largeBound = Math.pow (10, digitsLarge);
      smallBound = Math.pow (10, -digitsSmall);
   }

   /**
    * Returns the number of significant digits for values smaller than 1.
    * 
    * @return the number of significant digits for values smaller than 1.
    */
   public int getDigitsSmall () {
      return digitsSmall;
   }

   /**
    * Returns the number of significant digits for values greater than or equal
    * to 1.
    * 
    * @return the number of significant digits for values greater than or equal
    *         to 1.
    */
   public int getDigitsLarge () {
      return digitsLarge;
   }

   public String format (double x) {
      if (Double.isNaN (x))
         return Messages.getString ("PerformanceMeasureFormat.NoValue"); //$NON-NLS-1$
      if (x == 0.0)
         return "0"; //$NON-NLS-1$
      final Locale locale = Locale.getDefault ();
      final double absx = Math.abs (x);
      if (absx >= largeBound)
         return String.format (locale, "%.0f", x); //$NON-NLS-1$
      if (absx < smallBound && digitsSmall > 0)
         return String.format (locale,
         "%." + (digitsSmall - 1) + "f", x);  //$NON-NLS-1$ //$NON-NLS-2$
      final int digits = absx < 1 ? digitsSmall : digitsLarge;
      // Using PrintfFormat.format often
      // makes an hard to read mix
      // of scientific and non-scientific
      // notations values.
      return String.format
      (locale, "%." + digits +"g", x);  //$NON-NLS-1$ //$NON-NLS-2$
   }
}
