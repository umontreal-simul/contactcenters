package umontreal.iro.lecuyer.contactcenters.app;

import java.util.LinkedHashMap;
import java.util.Map;

import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatter;
import umontreal.iro.lecuyer.util.DoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXDoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXObjectMatrixFormatter;

/**
 * Defines basic methods to implement a contact
 * center evaluation system.
 */
public abstract class AbstractContactCenterEval extends AbstractContactCenterInfo implements ContactCenterEval {
   /**
    * Determines if the simulator is in verbose mode.
    */
   private boolean verbose = false;
   
   private final Map<String, Object> evalInfo = new LinkedHashMap<String, Object>();
   
   private ReportParams reportParams;

   public boolean isVerbose () {
      return verbose;
   }

   public void setVerbose (boolean v) {
      verbose = v;
   }
   
   public Map<String, Object> getEvalInfo() {
      return evalInfo;
   }
   
   public ReportParams getReportParams() {
      if (reportParams == null)
         reportParams = new ReportParams();
      return reportParams;
   }
   
   public void setReportParams (ReportParams reportParams) {
      this.reportParams = reportParams;
   }
   
   public String formatStatistics () {
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (getReportParams());
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms();
     return pfmt.formatValues (this, getReportParams());
   }

   public String formatStatisticsLaTeX () {
      final DoubleFormatter dfmt = new DefaultDoubleFormatter (getReportParams().getNumDigits (), getReportParams().getNumDigits ());
      final DoubleFormatter dfmtLaTeX = new LaTeXDoubleFormatter (dfmt);
      final LaTeXObjectMatrixFormatter fmt = new LaTeXObjectMatrixFormatter();
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (fmt);
      pfmt.setDoubleFormatterValues (dfmtLaTeX);
      pfmt.setPercentString ("\\%");
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistogramsLaTeX();
     return pfmt.formatValues (this, getReportParams());
   }
   
   public boolean formatStatisticsExcel (WritableWorkbook wb) {
      final PerformanceMeasureFormatExcel pfmt = new PerformanceMeasureFormatExcel (wb, getReportParams ());
     /* if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms(); */
      pfmt.setMaxColumns (getReportParams().getMaxColumns ());
      try {
         return pfmt.formatValues (this, getReportParams());
      }
      catch (final WriteException e) {
         final IllegalArgumentException iae = new IllegalArgumentException ("Could not write workbook");
         iae.initCause (e);
         throw iae;
      }
   }
}
