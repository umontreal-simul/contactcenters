package umontreal.iro.lecuyer.util;

/**
 * Represents an object that can format a double-precision value into a string,
 * while formatting can possibly be affected by the error on the formatted
 * value.
 */
public interface DoubleFormatterWithError extends DoubleFormatter {
   /**
    * Formats the value \texttt{x} with error \texttt{error} into a string, and
    * returns the formatted string. The error can be, e.g., the radius of a
    * confidence interval, or a standard deviation. The given error must be used
    * only to affect how \texttt{x} is formatted; it must be formatted into the
    * returned string.
    * 
    * @param x
    *           the value being formatted.
    * @param error
    *           the error on the formatted value.
    * @return the formatted value.
    */
   public String format (double x, double error);

   /**
    * This should be equivalent to {@link #format(double,double) format (x, 0)}.
    */
   public String format (double x);
}
