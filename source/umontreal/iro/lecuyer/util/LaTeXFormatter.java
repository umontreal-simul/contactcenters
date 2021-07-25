package umontreal.iro.lecuyer.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.FormatterClosedException;
import java.util.Locale;

/**
 * Provides methods for formatting times and number for typesetting with \LaTeX.
 */
public class LaTeXFormatter implements Closeable, Flushable, Appendable {
   /**
    * Default math-ensuring command, \texttt{ensuremath}.
    */
   public static final String DEFAULTMATHENSURINGCMD = "ensuremath";

   /**
    * Default string for representing NaN, \texttt{---}.
    */
   public static final String DEFAULTNAN = "\\mbox{---}";

   private Appendable out;
   private IOException ioe;
   private Locale locale;
   private boolean grouping = false;
   private String mathEnsuringCmd = DEFAULTMATHENSURINGCMD;
   private String nanString = DEFAULTNAN;
   
   public LaTeXFormatter() {
      this (Locale.getDefault ());
   }
   
   public LaTeXFormatter (Locale locale) {
      if (locale == null)
         throw new NullPointerException();
      out = new StringBuilder();
      this.locale = locale;
   }

   public LaTeXFormatter (Appendable out) {
      this (out, Locale.getDefault ());
   }
   
   public LaTeXFormatter (Appendable out, Locale locale) {
      if (locale == null)
         throw new NullPointerException();
      if (out == null)
         throw new NullPointerException();
      this.out = out;
      this.locale = locale;
   }

   public LaTeXFormatter (File file) throws IOException {
      this (file, Locale.getDefault ());
   }

   public LaTeXFormatter (File file, String csn) throws IOException {
      this (file, csn, Locale.getDefault ());
   }
   
   public LaTeXFormatter (File file, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new FileWriter (file);
      this.locale = locale;
   }
   
   public LaTeXFormatter (File file, String csn, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new OutputStreamWriter (new FileOutputStream (file), csn);
      this.locale = locale;
   }
   
   public LaTeXFormatter (OutputStream os) throws IOException {
      this (os, Locale.getDefault ());
   }

   public LaTeXFormatter (OutputStream os, String csn) throws IOException {
      this (os, csn, Locale.getDefault ());
   }
   
   public LaTeXFormatter (OutputStream os, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new OutputStreamWriter (os);
      this.locale = locale;
   }
   
   public LaTeXFormatter (OutputStream os, String csn, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new OutputStreamWriter (os, csn);
      this.locale = locale;
   }
   
   public LaTeXFormatter (String fileName) throws IOException {
      this (fileName, Locale.getDefault ());
   }

   public LaTeXFormatter (String fileName, String csn) throws IOException {
      this (fileName, csn, Locale.getDefault ());
   }
   
   public LaTeXFormatter (String fileName, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new FileWriter (fileName);
      this.locale = locale;
   }
   
   public LaTeXFormatter (String fileName, String csn, Locale locale) throws IOException {
      if (locale == null)
         throw new NullPointerException();
      out = new OutputStreamWriter (new FileOutputStream (fileName), csn);
      this.locale = locale;
   }
   
   public LaTeXFormatter (PrintStream ps) {
      this (ps, Locale.getDefault ());
   }
   
   public LaTeXFormatter (PrintStream ps, Locale locale) {
      if (ps == null || locale == null)
         throw new NullPointerException();
      out = ps;
      this.locale = locale;
   }
   
   public IOException ioException () {
      return ioe;
   }
   
   public Appendable out() {
      if (out == null)
         throw new FormatterClosedException();
      return out;
   }
   
   public Locale locale() {
      if (out == null)
         throw new FormatterClosedException();
      return locale;
   }
   
   public void setLocale (Locale locale) {
      if (locale == null)
         throw new NullPointerException();
      if (out == null)
         throw new FormatterClosedException();
      this.locale = locale;
   }
   
   public String getMathEnsuringCmd() {
      return mathEnsuringCmd;
   }
   
   public void setMathEnsuringCmd (String mathEnsuringCmd) {
      if (mathEnsuringCmd == null)
            throw new NullPointerException();
      this.mathEnsuringCmd = mathEnsuringCmd;
   }
   
   public String getNanString() {
      return nanString;
   }
   
   public void setNanString (String nanString) {
      if (nanString == null)
         throw new NullPointerException();
      this.nanString = nanString;
   }
   
   public boolean isGroupingUsed() {
      return grouping;
   }
   
   public void setGroupingUsed (boolean grouping) {
      this.grouping = grouping;
   }
   
   public void flush () {
      if (out instanceof Flushable)
         try {
            ((Flushable)out).flush ();
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
   }

   public void close () {
      if (out instanceof Closeable)
         try {
            ((Closeable)out).close ();
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
      out = null;
   }

   @Override
   public String toString() {
      if (out == null)
         throw new FormatterClosedException();
      return out.toString ();
   }
   
   /**
    * Formats a string representing the time duration \texttt{time}, given
    * in seconds. The returned string is of the form \texttt{1h2min3s}.
    * 
    * @param time
    *           the time to be formatted.
    * @return the latex formatter.
    */
   public LaTeXFormatter formatTime (double time) {
      if (out == null)
         throw new FormatterClosedException();
      final String res = processInfiniteAndNaN (time, mathEnsuringCmd, nanString);
      if (res != null) {
         try {
            out.append (res);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      final int hour = (int) (time / 3600.0);
      double remainder = time;
      if (hour > 0)
         remainder -= hour * 3600.0;
      final int min = (int) (remainder / 60.0);
      if (min > 0)
         remainder -= min * 60.0;
      final int second = (int) remainder;
      final int centieme = (int) (100.0 * (remainder - second) + 0.5);

      try {   
         if (hour > 0)
            out.append (String.valueOf (hour)).append ("\\mbox{h}");
         if (min > 0)
            out.append (String.valueOf (min)).append ("\\mbox{min}");
         if (second > 0 || centieme > 0) {
            out.append (String.valueOf (second));
            if (centieme > 0) {
               final DecimalFormatSymbols dfs = new DecimalFormatSymbols (locale);
               final char ch = dfs.getDecimalSeparator();
               //final String sep = ch == ',' ? "\\mbox{,}" : "" + ch;
               out.append (ch).append (String.valueOf (centieme));
            }
            out.append ("\\mbox{s}");
         }
         if (hour == 0 && min == 0 && second == 0 && centieme == 0 && time != 0)
            return formatNumber (3, 3, time).append ("\\mbox{s}");
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }
   
   public LaTeXFormatter formatInteger (long x) {
      return formatInteger (1, x);
   }

   /**
    * Formats the integer \texttt{x} for the locale \texttt{locale} with at
    * least \texttt{precision} digits. If \texttt{grouping} is \texttt{true}, a
    * locale-specific delimiter is used to separate groups, usually thousands.
    * If the grouping corresponds to a whitespace, it is converted to
    * \texttt{\~{}}, the \LaTeX\ unbreakable space.
    * 
    * @param precision
    *           the minimal number of digits.
    * @param x
    *           the integer being formatted.
    * @return the formatter object.
    * @exception IllegalArgumentException
    *               if \texttt{precision} is negative.
    */
   public LaTeXFormatter formatInteger (int precision, long x) {
      if (out == null)
         throw new FormatterClosedException();
      if (precision < 0)
         throw new IllegalArgumentException ("precision must be greater than 0");
      final NumberFormat nf = getIntegerFormatter (locale, precision, grouping);
      final String str = nf.format (x);
      try {
         if (grouping)
            out.append (fixGroupingAndDecimal (nf, str));
         else
            out.append (str);
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }
   
   public static NumberFormat getIntegerFormatter (Locale locale, int precision, boolean grouping) {
      final NumberFormat nf = NumberFormat.getInstance (locale);
      nf.setGroupingUsed (grouping);
      nf.setMinimumIntegerDigits (precision);
      nf.setMaximumFractionDigits (0);
      return nf;
   }
   
   public LaTeXFormatter formatFixed (double x) {
      return formatFixed (6, x);
   }

   /**
    * Formats the given double-precision number \texttt{x} to a string with a
    * fixed number \texttt{precision} of decimal digits, for the locale
    * \texttt{locale}. If \texttt{grouping} is \texttt{true}, a locale-specific
    * delimiter is used to separate groups, usually thousands. If the grouping
    * corresponds to a whitespace, it is converted to \texttt{\~{}}, the \LaTeX\
    * unbreakable space. If the formatted string has to be typeset in math mode,
    * it is surrounded with the given math-ensuring command
    * \texttt{mathEnsuringCmd}.
    * 
    * @param precision
    *           the number of decimal digits.
    * @param x
    *           the number being formatted.
    * @return this formatter object.
    * @exception IllegalArgumentException
    *               if \texttt{precision} is negative.
    */
   public LaTeXFormatter formatFixed (int precision, double x) {
      if (out == null)
         throw new FormatterClosedException();
      if (precision < 0)
         throw new IllegalArgumentException (
               "precision must be greater than or equal to 0");
      final String res = processInfiniteAndNaN (x, mathEnsuringCmd, nanString);
      if (res != null) {
         try {
            out.append (res);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      final NumberFormat nf = getFixedFormatter (locale, precision, grouping);
      final String str = nf.format (x);
      try {
         if (grouping)
            out.append (fixGroupingAndDecimal (nf, str));
         else
            out.append (str);
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }
   
   public static NumberFormat getFixedFormatter (Locale locale, int precision, boolean grouping) {
      final NumberFormat nf = NumberFormat.getInstance (locale);
      nf.setGroupingUsed (grouping);
      nf.setMinimumIntegerDigits (1);
      nf.setMinimumFractionDigits (precision);
      nf.setMaximumFractionDigits (precision);
      return nf;      
   }
   
   public LaTeXFormatter formatScientific (double x) {
      return formatScientific (6, x);
   }

   /**
    * Formats the double-precision value \texttt{x} into a string with the
    * scientific notation for the locale \texttt{locale}, with
    * \texttt{precision} decimal digits of precision. If the formatted string
    * has to be typeset in math mode, it is surrounded with the given
    * math-ensuring command \texttt{mathEnsuringCmd}.
    * 
    * @param precision
    *           the number of decimal digits.
    * @param x
    *           the value being formatted.
    * @return this formatter object.
    * @exception IllegalArgumentException
    *               if \texttt{precision} is negative.
    */
   public LaTeXFormatter formatScientific (int precision, double x) {
      
      if (out == null)
         throw new FormatterClosedException();
      if (precision < 0)
         throw new IllegalArgumentException (
               "precision must be greater than or equal to 0");
      final String res = processInfiniteAndNaN (x, mathEnsuringCmd, nanString);
      if (res != null) {
         try {
            out.append (res);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      final NumberFormat nf = getScientificFormatter (locale, precision, true);
      try {
         out.append (fixGroupingAndDecimal (nf, fixScientific (nf.format (x), mathEnsuringCmd)));
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }
   
   public static DecimalFormat getScientificFormatter (Locale locale, int precision, boolean showAllDecimals) {
      final NumberFormat nf = NumberFormat.getInstance (locale);

      final char patCh = showAllDecimals ? '0' : '#'; 
      final StringBuilder pattern = new StringBuilder ("0.");
      for (int i = 0; i < precision; i++)
         pattern.append (patCh);
      pattern.append ("E00");
      final DecimalFormat df;
      if (nf instanceof DecimalFormat) {
         df = (DecimalFormat) nf;
         df.applyPattern (pattern.toString ());
      }
      else {
         final DecimalFormatSymbols dfs = new DecimalFormatSymbols (locale);
         df = new DecimalFormat (pattern.toString (), dfs);
      }
      return df;
   }
   
   public LaTeXFormatter formatNumber (double x) {
      return formatNumber (6, x);
   }

   /**
    * Formats the given number \texttt{x} for the locale \texttt{locale}, with
    * \texttt{precision} significant digits. This method uses scientific
    * notation when the exponent is smaller than $-4$, or greater than or equal
    * to \texttt{precision}. Otherwise, it uses standard notation. If
    * \texttt{grouping} is \texttt{true}, a locale-specific delimiter is used to
    * separate groups, usually thousands. If the grouping corresponds to a
    * whitespace, it is converted to \texttt{\~{}}, the \LaTeX\ unbreakable
    * space. If the formatted string has to be typeset in math mode, it is
    * surrounded with the given math-ensuring command \texttt{mathEnsuringCmd}.
    * 
    * @param precision
    *           the number of significant digits.
    * @param x
    *           the value being formatted.
    * @return this formatter object.
    * @exception IllegalArgumentException
    *               if \texttt{precision} is negative or 0.
    */
   public LaTeXFormatter formatNumber (int precision, double x) {
      if (out == null)
         throw new FormatterClosedException();
      if (precision <= 0)
         throw new IllegalArgumentException ("precision must be greater than 0");
      final String res = processInfiniteAndNaN (x, mathEnsuringCmd, nanString);
      if (res != null) {
         try {
            out.append (res);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      final int exp = x == 0 ? 0 : (int) Math.floor (Math.log10 (Math.abs (x)));
      if (exp < -4 || exp >= precision) {
         final DecimalFormat df = getScientificFormatter (locale, precision - 1, false);
         try {
            out.append (fixGroupingAndDecimal (df, fixScientific (df.format (x), mathEnsuringCmd)));
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      else {
         final NumberFormat nf = NumberFormat.getInstance (locale);
         nf.setGroupingUsed (grouping);
         nf.setMinimumIntegerDigits (1);
         nf.setMinimumFractionDigits (0);
         nf.setMaximumFractionDigits (precision - exp - 1);
         final String str = nf.format (x);
         try {
            if (grouping)
               out.append (fixGroupingAndDecimal (nf, str));
            else
               out.append (str);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
   }

   /**
    * Formats \texttt{x} with \texttt{digitsSmall} significant digits if it is
    * smaller than 1, and \texttt{digitsLarge} significant digits if it is
    * larger than 1. Let $x$ be a value to be formatted. Let $d_1$ be the number
    * of needed significant digits for $|x|\le 1$, and $d_2$ be the number of
    * digits for $|x|>1$. If $x\ge 10^{d_1}$, the formatter uses
    * {@link #formatFixed(int,double)} with 0 decimal digit of precision. If $x <
    * 10^{-d_2}$, the formatter uses {@link #formatScientific} with $d_2$ digits
    * of precision. Otherwise, the formatter uses
    * {@link #formatNumber(int,int,double)} with $d_1$ or
    * $d_2$ significant digits, depending if $|x|$ is smaller than or equal to
    * 1, or greater than 1.
    * 
    * @param digitsSmall
    *           the number of significant digits for small numbers.
    * @param digitsLarge
    *           the number of significant digits for large numbers.
    * @param x
    *           the value to be formatted.
    * @return this formatter object.
    */
   public LaTeXFormatter formatNumber (int digitsSmall,
         int digitsLarge, double x) {
      if (out == null)
         throw new FormatterClosedException();
      if (digitsSmall < 0 || digitsLarge < 0)
         throw new IllegalArgumentException (
               "digitsSmall and digitsLarge must not be negative");
      final String res = processInfiniteAndNaN (x, mathEnsuringCmd, nanString);
      if (res != null) {
         try {
            out.append (res);
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      if (x == 0.0) {
         try {
            out.append ("0");
         }
         catch (final IOException ioe1) {
            this.ioe = ioe1;
         }
         return this;
      }
      final double largeBound = Math.pow (10, digitsLarge);
      final double smallBound = Math.pow (10, -digitsSmall);
      final double absx = Math.abs (x);
      if (absx >= largeBound)
         return formatFixed (0, x);
      if (absx < smallBound && digitsSmall > 0)
         return formatScientific (digitsSmall - 1,
                x);
      final int digits = absx < 1 ? digitsSmall : digitsLarge;
      // Using PrintfFormat.format often
      // makes an hard to read mix
      // of scientific and non-scientific
      // notations values.
      return formatNumber (digits,
             x);
   }

   /**
    * Returns a string representing infinite or NaN for \texttt{x}, or
    * \texttt{null} if \texttt{x} is finite. This method is used by
    * {@link #formatFixed(int, double)},
    * {@link #formatScientific(int, double)}, and
    * {@link #formatNumber(int, int, double)}.
    * 
    * @param x
    *           the number being processed.
    * @param mathEnsuringCmd
    *           the math-ensuring command.
    * @param nanString
    *           the string representing NaN.
    * @return a string representing infinity or NaN, or \texttt{null}.
    */
   public static String processInfiniteAndNaN (double x,
         String mathEnsuringCmd, String nanString) {
      if (Double.isInfinite (x))
         return new StringBuilder ("\\").append (mathEnsuringCmd).append ('{')
               .append (x < 0 ? "-" : "").append ("\\infty}").toString ();
      if (Double.isNaN (x))
         return nanString;
      return null;
   }

   /**
    * Fixes groupings of the string \texttt{str} formatted by the number
    * formatter \texttt{nf}, for \LaTeX\ compatibility. If the grouping used by
    * the given number formatter is a whitespace, this method replaces it with a
    * \LaTeX\ unbreakable space. Otherwise, the string is unchanged.
    * 
    * @param str
    *           the string to fix.
    * @param nf
    *           the number formatter having formatted the string.
    * @return the fixed string.
    */
   public static String fixGroupingAndDecimal (NumberFormat nf, String str) {
      if (nf instanceof DecimalFormat) {
         final DecimalFormat df = (DecimalFormat) nf;
         final DecimalFormatSymbols dfs = df.getDecimalFormatSymbols ();
         final StringBuilder sb = new StringBuilder (str);
         final char grp = dfs.getGroupingSeparator ();
         if (Character.isWhitespace (grp) || grp == 160)
            for (int i = 0; i < sb.length (); i++)
               if (sb.charAt (i) == grp)
                  sb.setCharAt (i, '~');
         //final char dec = dfs.getDecimalSeparator ();
//         if (dec == ',')
//            for (int i = sb.length () - 1; i >= 0; i--)
//               if (sb.charAt (i) == dec) {
//                  sb.deleteCharAt (i);
//                  sb.insert (i, "\\mbox{,}");
//               }
         return sb.toString ();
      }
      return str;
   }
   
   /**
    * Converts the string \texttt{str} using Java scientific notation to \LaTeX\
    * notation. For example, this converts \texttt{2e4} to
    * \texttt{2*10\^{}\{4\}}.
    * 
    * @param str
    *           the string being fixed.
    * @param mathEnsuringCmd
    *           the math-ensuring \LaTeX\ command.
    * @return the fixed string.
    */
   public static String fixScientific (String str, String mathEnsuringCmd) {
      int idx = str.indexOf ('E');
      if (idx == -1)
         idx = str.indexOf ('e');
      if (idx == -1)
         return str;
      return new StringBuilder ("\\").append (mathEnsuringCmd).append ('{')
            .append (str.substring (0, idx)).append ("*10^{").append (
                  str.substring (idx + 1)).append ("}}").toString ();
   }

   public LaTeXFormatter append (char c) {
      if (out == null)
         throw new FormatterClosedException();
      try {
         out.append (c);
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }

   public LaTeXFormatter append (CharSequence csq, int start, int end) {
      if (out == null)
         throw new FormatterClosedException();
      try {
         out.append (csq, start, end);
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }

   public LaTeXFormatter append (CharSequence csq) {
      if (out == null)
         throw new FormatterClosedException();
      try {
         out.append (csq);
      }
      catch (final IOException ioe1) {
         this.ioe = ioe1;
      }
      return this;
   }
}
