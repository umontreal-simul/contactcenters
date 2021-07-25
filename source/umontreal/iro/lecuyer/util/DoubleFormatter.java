package umontreal.iro.lecuyer.util;

/**
 * Represents an object that can format a double-precision value into a string.
 */
public interface DoubleFormatter {
   /**
    * Formats the double \texttt{x} as a string, and returns the resulting
    * string.
    * 
    * @param x
    *           the value being formatted.
    * @return the formatted value.
    */
   public String format (double x);
}
