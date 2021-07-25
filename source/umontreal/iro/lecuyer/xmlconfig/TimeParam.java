package umontreal.iro.lecuyer.xmlconfig;

import umontreal.ssj.util.AbstractChrono;
import umontreal.ssj.util.TimeUnit;

/**
 * Represents a time duration or a rate parameter expressed in a time unit. This
 * parameter object can be constructed from a string, or a value with a
 * {@link TimeUnit} instance representing its unit. The {@link #get} method can
 * be used to obtain the value of the parameter, converted into a user-specified
 * time unit. Usually, this parameter is used to represent a duration. Rates are
 * supported for distribution scale parameters.
 * 
 * The time parameter can also be used as an XML attribute type or as a nested
 * element when using {@link ParamReader} to parse an XML document into a
 * parameter object.
 */
public class TimeParam extends AbstractParam implements Cloneable {
   private double value;
   private boolean rate;
   private TimeUnit unit;

   /**
    * Constructs a time parameter with value 0.
    */
   public TimeParam () {
      value = 0;
      unit = null;
      rate = false;
   }

   /**
    * Equivalent to {@link #TimeParam(double,boolean,TimeUnit) TimeParam}
    * \texttt{(value, false, null)}.
    * 
    * @param value
    *           the encoded value.
    * @exception IllegalArgumentException
    *               if \texttt{value} is negative.
    */
   public TimeParam (double value) {
      this (value, false, null);
   }

   /**
    * Equivalent to {@link #TimeParam(double,boolean,TimeUnit) TimeParam}
    * \texttt{(value, false, unit)}.
    * 
    * @param value
    *           the encoded value.
    * @param unit
    *           the time unit in which the value is assumed to be expressed.
    * @exception IllegalArgumentException
    *               if \texttt{value} is negative.
    */
   public TimeParam (double value, TimeUnit unit) {
      this (value, false, unit);
   }

   /**
    * Constructs a new time parameter with encoded value \texttt{value}, rate
    * indicator \texttt{rate}, and time unit \texttt{unit}. If \texttt{rate} is
    * \texttt{true}, \texttt{value} represents a rate that must be converted to
    * a time by inversion before any time unit conversion is performed.
    * \texttt{unit} represents the time unit in which \texttt{value} is assumed
    * to be expressed. If \texttt{unit} is \texttt{null}, the {@link #get}
    * method performs no time conversion.
    * 
    * @param value
    *           the encoded value.
    * @param rate
    *           the rate indicator.
    * @param unit
    *           the time unit in which the value is assumed to be expressed.
    * @exception IllegalArgumentException
    *               if \texttt{value} is negative.
    */
   public TimeParam (double value, boolean rate, TimeUnit unit) {
      if (Double.isNaN (value))
         throw new IllegalArgumentException (
               "A time duration or rate must not be NaN");
      if (value < 0)
         throw new IllegalArgumentException (
               "A time duration or rate must not be negative");
      this.value = value;
      this.unit = unit;
      this.rate = rate;
   }

   /**
    * Constructs a time parameter from the string \texttt{str}. A string
    * representing a time parameter must contain a value, an optional rate
    * indicator and an optional unit. If the value is followed by the rate
    * indicator \texttt{/}, it is inverted before time unit conversion is
    * performed by {@link #get}. The time unit must be the short name of any
    * instance of {@link TimeUnit}. For example, \texttt{12s} represents 12
    * seconds and \texttt{12/m} represents a rate of 12 units per minute.
    * 
    * @param str
    *           the string representation of the time parameter.
    * @exception IllegalArgumentException
    *               if the string is invalid.
    */
   public TimeParam (String str) {
      nestedText (str);
   }

   /**
    * Returns the value of this time parameter, without conversion.
    * 
    * @return the time parameter's value.
    */
   public double getValue () {
      return value;
   }

   /**
    * Sets the value of this time parameter to \texttt{value}.
    * 
    * @param value
    *           the new time parameter's value.
    * @exception IllegalArgumentException
    *               if \texttt{value} is negative or NaN.
    */
   public void setValue (double value) {
      if (Double.isNaN (value))
         throw new IllegalArgumentException (
               "A time duration or rate must not be NaN");
      if (value < 0)
         throw new IllegalArgumentException (
               "A time duration or rate must not be negative");
      this.value = value;
   }

   /**
    * Returns the rate indicator of this time parameter.
    * 
    * @return the time parameter's rate indicator.
    */
   public boolean getRateIndicator () {
      return rate;
   }

   /**
    * Sets the rate indicator of this time parameter to \texttt{rate}.
    * 
    * @param rate
    *           the new time parameter's rate indicator.
    */
   public void setRateIndicator (boolean rate) {
      this.rate = rate;
   }

   /**
    * Returns the time unit of this parameter.
    * 
    * @return this parameter's time unit.
    */
   public TimeUnit getTimeUnit () {
      return unit;
   }

   /**
    * Sets the time unit of this parameter to \texttt{unit}.
    * 
    * @param unit
    *           the new time unit of this parameter.
    */
   public void setTimeUnit (TimeUnit unit) {
      this.unit = unit;
   }

   /**
    * Equivalent to {@link #get(boolean,TimeUnit) get} \texttt{(false,
    * dstUnit)}.
    * 
    * @param dstUnit
    *           the destination time unit.
    * @return the computed duration.
    */
   public double get (TimeUnit dstUnit) {
      return get (false, dstUnit);
   }

   /**
    * Returns the rate or time extracted from this parameter, expressed in
    * \texttt{dstUnit}. If \texttt{dstRate} is \texttt{true}, the returned value
    * corresponds to a rate. Otherwise, it corresponds to a time.
    * 
    * If {@link #getTimeUnit} returns \texttt{null}, the method returns
    * {@link #getValue} unchanged. Otherwise, if {@link #getRateIndicator}
    * returns \texttt{true}, \texttt{1/}{@link #getValue} is used instead of
    * {@link #getValue}. The method uses {@link TimeUnit#convert} to convert
    * the value from source unit {@link #getTimeUnit} to destination unit
    * \texttt{dstUnit}. If \texttt{dstRate} is \texttt{true}, one over the
    * converted value is returned. Otherwise, the converted value is returned.
    * 
    * @param dstRate
    *           determines if the returned value must be a rate.
    * @param dstUnit
    *           the destination time unit.
    * @return the computed duration or rate.
    */
   public double get (boolean dstRate, TimeUnit dstUnit) {
      if (unit == null)
         return value;
      double val = rate ? 1.0 / value : value;
      val = TimeUnit.convert (val, unit, dstUnit);
      return dstRate ? 1.0 / val : val;
   }

   /**
    * Constructs a new time parameter from the string \texttt{str}, using
    * {@link #TimeParam(String)} and returns the constructed instance.
    * 
    * @param str
    *           the string representation of the time parameter.
    * @return the constructed time parameter.
    * @exception IllegalArgumentException
    *               if the string is invalid.
    */
   public static TimeParam valueOf (String str) {
      return new TimeParam (str);
   }

   @Override
   public boolean equals (Object other) {
      if (other == null)
         return false;
      if (!(other instanceof TimeParam))
         return false;
      final TimeParam o = (TimeParam) other;
      return value == o.getValue () && rate == o.getRateIndicator ()
            && unit == o.getTimeUnit ();
   }

   /**
    * Formats this time parameter as a string. The returned string can be used
    * by {@link #TimeParam(String)} or {@link #valueOf} to construct a time
    * parameter.
    * 
    * @return the time parameter, formatted as a string.
    */
   @Override
   public String toString () {
      String str;
      if (Double.isInfinite (value)) {
         str = "infinity";
         if (unit != null)
            str += " ";
      }
      else
         str = String.valueOf (value);
      if (unit != null) {
         if (rate)
            str += "/";
         str += unit.getShortName ();
      }
      return str;
   }
   
   public String formatHMS() {
      final double timeInSec = get (TimeUnit.SECOND);
      return AbstractChrono.format (timeInSec);
   }
   
   public String formatSI () {
      final double time = get (TimeUnit.SECOND);
      return formatSI (time);
   }

   public static String formatSI (double time) {
      final int hour = (int) (time / 3600.0);
      double remainder = hour > 0 ? time - hour * 3600.0 : time;
      final int min = (int) (remainder / 60.0);
      if (min > 0)
         remainder -= min * 60.0;
      final int second = (int) remainder;
      final int centieme = (int) (100.0 * (remainder - second) + 0.5);

      final StringBuilder out = new StringBuilder();   
      if (hour > 0)
         out.append (hour).append ("h");
      if (min > 0)
         out.append (min).append ("min");
      if (second > 0 || time < 1 && centieme > 0) {
         out.append (second);
         if (time < 1 && centieme > 0)
            out.append ('.').append (centieme);
         out.append ("s");
      }
      if (hour == 0 && min == 0 && second == 0 && centieme == 0)
         return "0s";
      return out.toString ();
   }

   /**
    * For internal use only.
    */
   public boolean isAttributeSupported (String a) {
      if (a.equals ("value") || a.equals ("rateIndicator")
            || a.equals ("timeUnit"))
         return false;
      return true;
   }

   /**
    * For internal use only.
    */
   public void nestedText (String str) {
      // Separate the value from the rate indicator and unit.
      final String trimmedStr = str.trim ();
      if (trimmedStr.length () == 0) {
         value = 0;
         rate = false;
         unit = null;
         return;
      }
      int idx = 0;
      if (Character.isDigit (trimmedStr.charAt (0)))
         // Value is a double-precision number.
         while (idx < trimmedStr.length ()
               && (Character.isDigit (trimmedStr.charAt (idx)) || trimmedStr
                     .charAt (idx) == '.'))
            idx++;
      else
         // value is a string such as infinity.
         while (idx < trimmedStr.length ()
               && !Character.isWhitespace (trimmedStr.charAt (idx))
               && trimmedStr.charAt (idx) != '/')
            idx++;
      final String valStr = trimmedStr.substring (0, idx).trim ();
      String unitStr = trimmedStr.substring (idx).trim ();

      // Parse the value into a double-precision number.
      if (valStr.equalsIgnoreCase ("infinity") ||
            valStr.equalsIgnoreCase ("INF"))
         value = Double.POSITIVE_INFINITY;
      else
         value = Double.parseDouble (valStr);
      if (Double.isNaN (value))
         throw new IllegalArgumentException (
               "A time duration or rate must not be NaN");
      if (value < 0)
         throw new IllegalArgumentException (
               "A time duration or rate must not be negative");

      rate = false;
      if (unitStr.length () > 0 && unitStr.charAt (0) == '/') {
         rate = true;
         unitStr = unitStr.substring (1).trim ();
      }
      if (unitStr.length () > 0) {
         // A time unit is given, try to match a short time unit name.
         for (final TimeUnit tu : TimeUnit.values ())
            if (tu.getShortName ().equals (unitStr)) {
               unit = tu;
               break;
            }
         if (unit == null)
            throw new IllegalArgumentException (
                  "Invalid time unit short name: " + unitStr);
      }
   }

   @Override
   public TimeParam clone () {
      try {
         return (TimeParam) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "CloneNotSupportedException for a class implementing Cloneable");
      }
   }
}
