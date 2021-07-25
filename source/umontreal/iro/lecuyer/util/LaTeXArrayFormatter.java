package umontreal.iro.lecuyer.util;

import java.io.Closeable;
import java.io.Flushable;

/**
 * Provides utility methods to format every
 * element of arrays using methods
 * of {@link LaTeXFormatter}.
 * All methods in this class format each
 * element of given arrays, separated by
 * a comma and a space.
 * For example, the method {@link #formatIntegerArray}
 * may produce \texttt{2, 5, 1, 6}.
 */
public class LaTeXArrayFormatter implements Closeable, Flushable {
   private LaTeXFormatter lf;
   
   public LaTeXArrayFormatter (LaTeXFormatter lf) {
      if (lf == null)
         throw new NullPointerException();
      this.lf = lf;
   }
   
   public LaTeXFormatter getLaTeXFormat() {
      return lf;
   }
   
   public void close() {
      lf.close ();
   }
   
   public void flush() {
      lf.flush ();
   }
   
   @Override
   public String toString() {
      return lf.toString ();
   }
   
   /**
    * Formats each integer of the array \texttt{array} with
    * \texttt{precision} digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatIntegerArray (int precision, byte[] array) {
      boolean first = true;
      for (final byte v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatInteger (precision, v);
      }
      return this;
   }

   /**
    * Formats each integer of the array \texttt{array} with
    * \texttt{precision} digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatIntegerArray (int precision, short[] array) {
      boolean first = true;
      for (final short v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatInteger (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each integer of the array \texttt{array} with
    * \texttt{precision} digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatIntegerArray (int precision, int[] array) {
      boolean first = true;
      for (final int v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatInteger (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each integer of the array \texttt{array} with
    * \texttt{precision} digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatIntegerArray (int precision, long[] array) {
      boolean first = true;
      for (final long v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatInteger (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * using a fixed \texttt{precision} number of
    * decimal digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatFixedArray (int precision,
         float[] array) {
      boolean first = true;
      for (final float v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatFixed (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * using a fixed \texttt{precision} number of
    * decimal digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatFixedArray (int precision,
         double[] array) {
      boolean first = true;
      for (final double v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatFixed (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * in scientific notation with \texttt{precision}
    * decimal digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatScientificArray (int precision,
         float[] array) {
      boolean first = true;
      for (final float v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatScientific (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * in scientific notation with \texttt{precision}
    * decimal digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatScientificArray (int precision,
         double[] array) {
      boolean first = true;
      for (final double v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatScientific (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         byte[] array) {
      boolean first = true;
      for (final byte v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         short[] array) {
      boolean first = true;
      for (final short v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         int[] array) {
      boolean first = true;
      for (final int v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         long[] array) {
      boolean first = true;
      for (final long v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         float[] array) {
      boolean first = true;
      for (final float v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array} with
    * \texttt{precision} significant digits.
    * @param precision the minimal number of digits of precision.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int precision,
         double[] array) {
      boolean first = true;
      for (final double v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (precision, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         byte[] array) {
      boolean first = true;
      for (final byte v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         short[] array) {
      boolean first = true;
      for (final short v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         int[] array) {
      boolean first = true;
      for (final int v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }

   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         long[] array) {
      boolean first = true;
      for (final long v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         float[] array) {
      boolean first = true;
      for (final float v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }
   
   /**
    * Formats each number of the array \texttt{array}
    * using {@link LaTeXFormatter#formatNumber(int, int, double)}.
    * @param digitsSmall the number of significant digits for numbers smaller than 1.
    * @param digitsLarge the number of significant digits for numbers greater than 1.
    * @param array the array to be formatted.
    * @return this array formatter.
    */
   public LaTeXArrayFormatter formatNumberArray (int digitsSmall, int digitsLarge,
         double[] array) {
      boolean first = true;
      for (final double v : array) {
         if (first)
            first = false;
         else
            lf.append (", ");
         lf.formatNumber (digitsSmall, digitsLarge, v);
      }
      return this;
   }
}
