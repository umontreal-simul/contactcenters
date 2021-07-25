package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallTypeParams;

/**
 * Provides helper static methoeds used for the
 * initialization of call center models.
 */
public class CallCenterUtil {
   private CallCenterUtil() {}

   /**
    * Constructs and returns an array containing
    * \texttt{numPeriods} elements from the input
    * array \texttt{array}.
    * If the length of the given array is 0, this
    * returns an empty array.
    * Otherwise, if the length is 1, this
    * returns an array of length
    * \texttt{numPeriods} filled
    * with \texttt{array[0]}.
    * Otherwise, an array with the first
    * \texttt{numPeriods} elements of
    * \texttt{array} is constructed and returned.
    * @param array the input array.
    * @param numPeriods the number of elements in the output array.
    * @return the output array.
    * @exception IllegalArgumentException if the
    * given array is too short.
    */
   public static int[] getIntArray (int[] array, int numPeriods) {
      if (array.length == 0)
         return array;
      if (array.length == 1) {
         final int[] res = new int[numPeriods];
         Arrays.fill (res, array[0]);
         return res;
      }
      
      if (array.length < numPeriods)
         throw new IllegalArgumentException
         ("Array too small, needs at least one element per period");
      if (array.length == numPeriods)
         return array;
      final int[] res = new int[numPeriods];
      System.arraycopy (array, 0, res, 0, numPeriods);
      return res;
   }

   /**
    * Similar to {@link #getIntArray(int[],int)},
    * for an array of double-precision values.
    * @param array the input array.
    * @param numPeriods the number of elements in the output array.
    * @return the output array.
    * @exception IllegalArgumentException if the
    * given array is too short.
    */
   public static double[] getDoubleArray (double[] array, int numPeriods) {
      if (array.length == 0)
         return array;
      if (array.length == 1) {
         final double[] res = new double[numPeriods];
         Arrays.fill (res, array[0]);
         return res;
      }
      
      if (array.length < numPeriods)
         throw new IllegalArgumentException
         ("Array too small, needs at least one element per period");
      if (array.length == numPeriods)
         return array;
      final double[] res = new double[numPeriods];
      System.arraycopy (array, 0, res, 0, numPeriods);
      return res;
   }

   /**
    * Converts the given XML gregorian calendar into a 
    * Java gregorian calendar, with time reset to midnight
    * relative to the timezone
    * given in the XML gregorian calendar. If \texttt{xgcal} does not specify a
    * timezone offset, the default offset of the system is used. If
    * \texttt{xgcal} is \texttt{null}, the current date is used.
    * 
    * This method first creates a
    * Java {@link GregorianCalendar} by using
    * {@link XMLGregorianCalendar#toGregorianCalendar()} (this uses the default
    * timezone offset if no offset was specified explicitly), or the no-argument
    * constructor of {@link GregorianCalendar} if \texttt{xgcal} is
    * \texttt{null} (this creates a calendar
    * initialized to the current date and time).
    * It then resets the time fields of the created calendar
    * to midnight
    * before returning it.
    * 
    * @param xgcal
    *           the XML gregorian calendar to be converted to a date.
    * @return the gregorian calendar representing the date.
    */
   public static GregorianCalendar getDate (XMLGregorianCalendar xgcal) {
      GregorianCalendar gcal;
      if (xgcal != null)
         // Initialize the calendar with the date from the XML file.
         // If a timezone offset is specified in XML, we use it.
         // Otherwise, we use the default timezone offset.
         gcal = xgcal.toGregorianCalendar ();
      else
         gcal = new GregorianCalendar ();
      // Clear all time components, so that
      // cal represents midnight for the
      // selected timezone (in the XML file,
      // or default timezone).
      gcal.clear (Calendar.AM_PM);
      gcal.clear (Calendar.HOUR_OF_DAY);
      gcal.clear (Calendar.HOUR);
      gcal.clear (Calendar.MINUTE);
      gcal.clear (Calendar.SECOND);
      gcal.clear (Calendar.MILLISECOND);
      return gcal;
   }

   /**
    * Returns the time duration, in milliseconds, elapsed
    * between midnight and the time given by
    * \texttt{xgcal}, at the date set by
    * \texttt{xgcal}. 
    * This method uses
    * {@link XMLGregorianCalendar#toGregorianCalendar(TimeZone,Locale,XMLGregorianCalendar)}
    * with a default timezone corresponding to GMT, the default locale, and no
    * default XML gregorian calendar. It then clears all fields of the resulting
    * calendar corresponding to date components, and returns
    * {@link Calendar#getTimeInMillis()}. If \texttt{xgcal} is \texttt{null},
    * this returns 0.
    * 
    * @param xgcal
    *           the XML gregorian calendar.
    * @return the time in milliseconds.
    */
   public static long getTimeInMillis (XMLGregorianCalendar xgcal) {
      if (xgcal == null)
         return 0;
      // If no timezone offset is specified explicitly,
      // do not use the offset of the current timezone.
      final GregorianCalendar gcal = xgcal.toGregorianCalendar (TimeZone
            .getTimeZone ("GMT"), null, null);
      gcal.clear (Calendar.ERA);
      gcal.clear (Calendar.YEAR);
      gcal.clear (Calendar.MONTH);
      gcal.clear (Calendar.DAY_OF_MONTH);
      gcal.clear (Calendar.DAY_OF_WEEK);
      gcal.clear (Calendar.DAY_OF_WEEK_IN_MONTH);
      gcal.clear (Calendar.DAY_OF_YEAR);
      return gcal.getTimeInMillis ();
   }

   /**
    * Returns information about a call type \texttt{k}
    * defined in call center parameters \texttt{ccParams}.
    * This method returns a string of the form
    * \texttt{call type k (name)} which is included
    * in some error messages.
    * @param ccParams the call center parameters.
    * @param k the index of the call type.
    * @return the string representation for the call type.
    */
   public static String getCallTypeInfo (CallCenterParams ccParams, int k) {
      final StringBuilder sb = new StringBuilder ();
      sb.append ("call type ");
      sb.append (k);
      CallTypeParams par;
      if (k < ccParams.getInboundTypes ().size ())
         par = ccParams.getInboundTypes ().get (k);
      else
         par = ccParams.getOutboundTypes ().get (
               k - ccParams.getInboundTypes ().size ());
      if (par.isSetName ()) {
         sb.append (" (");
         sb.append (par.getName ());
         sb.append (')');
      }
      return sb.toString ();
   }

   /**
    * Similar to {@link #getCallTypeInfo(CallCenterParams,int)},
    * for agent group \texttt{i}.
    * This method returns a string of the form
    * \texttt{agent group i (name)} included
    * in some error messages.
    * @param ccParams the call center parameters.
    * @param i the index of the agent group.
    * @return the string representation of the agent group.
    */
   public static String getAgentGroupInfo (CallCenterParams ccParams, int i) {
      final StringBuilder sb = new StringBuilder ();
      sb.append ("agent group ");
      sb.append (i);
      final AgentGroupParams par = ccParams.getAgentGroups ().get (i);
      if (par.isSetName ()) {
         sb.append (" (");
         sb.append (par.getName ());
         sb.append (')');
      }
      return sb.toString ();
   }

   /**
    * Constructs and returns a map for which
    * each entry $(k,v')$ is created from
    * entry $(k,v)$ in map \texttt{map},
    * where $k$ is a key, and $v'$
    * is the string representation of the value $v$.
    * The string representation of $v$ is
    * the string ``\texttt{null}'' if $v$
    * is \texttt{null}, or the result of
    * \texttt{v.toString()} if $v$
    * is non-\texttt{null}.
    * @param <K> the type of keys in the maps.
    * @param map the source map.
    * @return the map with string representations as values.
    */
   public static <K> Map<K, String> toStringValues (Map<? extends K, ? extends Object> map) {
      final Map<K, String> out = new HashMap<K, String>();
      for (final Map.Entry<? extends K, ? extends Object> e : map.entrySet()) {
         final K key = e.getKey();
         final Object value = e.getValue();
         out.put (key, value == "null" ? null : value.toString());
      }
      return out;
   }
}
