import java.io.File;
import java.util.Formatter;

import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.CCResultsWriter;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterEvalResults;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;


public class WriteSummary extends CCResultsWriter {
   // The evaluation information of interest
   String[] keys = {
         SimulateScenarios.VSTR,
         SimulateScenarios.PSTR,
         SimulateScenarios.SSTR,
         SimulateScenarios.ASTR
   };
   // The type of performance measures desired in the summary
   PerformanceMeasureType[] pms = {
         PerformanceMeasureType.SERVICELEVEL,
         PerformanceMeasureType.ABANDONMENTRATIO,
         PerformanceMeasureType.SPEEDOFANSWER,
         PerformanceMeasureType.OCCUPANCY
   };

   // Used to format the summary
   Formatter fmt;
   // Pattern for lines
   String fmtStr;
   // The arguments given to printf-like methods
   Object[] tmp;

   public WriteSummary() {
      // Creates the patterns, and formats
      // the first line, which is an header.
      StringBuilder sbHead = new StringBuilder();
      StringBuilder sb = new StringBuilder();
      tmp = new Object[keys.length + pms.length];
      int idx = 0;
      for (String key : keys) {
         sbHead.append ("%").append (key.length ()).append ("s ");
         sb.append ("%").append (key.length ()).append ("f ");
         tmp[idx++] = key;
      }
      for (PerformanceMeasureType pm : pms) {
         sbHead.append ("%").append (pm.name ().length ()).append ("s ");
         sb.append ("%").append (pm.name ().length ()).append ("f ");
         tmp[idx++] = pm.name ();
      }
      sbHead.append ("%n");
      sb.append ("%n");
      String fmtHead = sbHead.toString ();
      fmtStr = sb.toString ();

      fmt = new Formatter();
      fmt.format (fmtHead, tmp);
   }

   @Override
   public void writeResults (String resFileName, ContactCenterEvalResults res) {
      int idx = 0;
      // Obtain evaluation information
      for (String key : keys) {
         Object value = res.getEvalInfo ().get (key);
         if (value == null || !(value instanceof Double))
            value = Double.NaN;
         tmp[idx++] = value;
      }
      // Obtain global averages
      for (PerformanceMeasureType pm : pms) {
         DoubleMatrix2D m = res.getPerformanceMeasure (pm);
         tmp[idx++] = m.get (m.rows () - 1, m.columns () - 1);
      }
      // Format everything
      fmt.format (fmtStr, tmp);
   }

   public String toString() {
      return fmt.toString ();
   }

   public static void main (String[] args) {
      WriteSummary writer = new WriteSummary();
      for (String fn : args)
         writer.writeResults (new File (fn));
      System.out.println (writer);
   }
}
