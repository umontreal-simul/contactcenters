package umontreal.iro.lecuyer.contactcenters.app;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.ssj.util.TimeUnit;

public class OldSimParamsConverter {
   private static TimeUnit getUnitFromShortName (String shortName) {
      for (final TimeUnit tu : TimeUnit.values ())
         if (tu.getShortName ().equals (shortName))
            return tu;
      throw new IllegalArgumentException ("Invalid time unit short name: "
            + shortName);
   }

   public static double getTime (String str, TimeUnit dstUnit, TimeUnit defaultUnit) {
      final double value;
      final boolean rate;
      final TimeUnit unit;
      final String trimmedStr = str.trim ();
      if (trimmedStr.length () == 0)
         return 0;
      else {
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
         if (valStr.equalsIgnoreCase ("infinity")
               || valStr.equalsIgnoreCase ("INF"))
            return Double.POSITIVE_INFINITY;
         else
            value = Double.parseDouble (valStr);
         if (Double.isNaN (value))
            throw new IllegalArgumentException (
                  "A time duration or rate must not be NaN");
         if (value < 0)
            throw new IllegalArgumentException (
                  "A time duration or rate must not be negative");

         if (unitStr.length () > 0 && unitStr.charAt (0) == '/') {
            rate = true;
            unitStr = unitStr.substring (1).trim ();
         }
         else
            rate = false;
         if (unitStr.length () > 0)
            // A time unit is given, try to match a short time unit name.
            unit = getUnitFromShortName (unitStr);
         else
            unit = defaultUnit;
      }

      if (unit == null)
         return value;
      final double val = rate ? 1.0 / value : value;
      return TimeUnit.convert (val, unit, dstUnit);
   }
   
   public static String toDuration (double timeInSec) {
      final int hour = (int)(timeInSec / 3600.0);
      final double rem = timeInSec - hour*3600;
      final int minute = (int)(rem / 60);
      final double second = rem - minute*60;
      
      final StringBuilder sb = new StringBuilder();
      sb.append ("PT");
      if (hour > 0)
         sb.append (hour).append ('H');
      if (minute > 0)
         sb.append (minute).append ('M');
      if (second > 0)
         if ((int)second == second)
            sb.append ((int)second).append ('S');
         else
            sb.append (second).append ('S');
      if (hour == 0 && minute == 0 && second == 0)
         sb.append ("0S");
      return sb.toString ();
   }

   public static String timeToDuration (String time) {
      double cnvTime;
      try {
         cnvTime = getTime (time, TimeUnit.SECOND, null); 
      }
      catch (final IllegalArgumentException iae) {
         final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.app");
         logger.warning ("Time parameter " + time + " invalid, returning PT0S");
         return "PT0S";
      }

      return toDuration (cnvTime);
   }
   
   public static void main (String[] args) {
      if (args.length != 2) {
         if (args.length > 0)
            System.err.println ("Wrong number of arguments");
         System.err.println ("Usage: java umontreal.iro.lecuyer.contactcenters.app.OldSimParamsConverter <input file> <output file>");
         System.exit (1);
      }
      final String inputFile = args[0];
      final String outputFile = args[1];
      if (inputFile.equals (outputFile)) {
         System.err.println ("Input and output files must be different");
         System.exit (1);
      }
      if (!new File (inputFile).exists ()) {
         System.err.println ("Cannot find input file " + inputFile);
         System.exit (1);
      }
      
      final URL xslUrl = OldSimParamsConverter.class.getResource ("cnvSimParams.xsl");
      if (xslUrl == null) {
         System.err.print ("Cannot find XSL stylesheet for the transformation");
         System.exit (1);
      }
      
      final TransformerFactory tFactory = TransformerFactory.newInstance ();
      Transformer trans;
      try {
         trans = tFactory.newTransformer (new StreamSource (xslUrl.toString ()));
      }
      catch (final TransformerConfigurationException tce) {
         System.err.println (ExceptionUtil.throwableToString (tce));
         return;
      }
      trans.setOutputProperty (
            "{http://xml.apache.org/xalan}indent-amount", "3");
      
      final Source src = new StreamSource (new File (inputFile));
      final Result res = new StreamResult (new File (outputFile));
      try {
         trans.transform (src, res);
      }
      catch (final TransformerException te) {
         System.err.println (ExceptionUtil.throwableToString (te));
         System.exit (1);
      }
   }
}
