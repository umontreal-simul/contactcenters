package umontreal.iro.lecuyer.contactcenters.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import umontreal.iro.lecuyer.contactcenters.app.params.PerformanceMeasureParams;
import umontreal.iro.lecuyer.contactcenters.app.params.HistogramParams;
import umontreal.iro.lecuyer.contactcenters.app.params.PrintedStatParams;
import umontreal.iro.lecuyer.contactcenters.app.params.PropertyNameParam;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.ssj.charts.HistogramChart;
import umontreal.ssj.charts.HistogramSeriesCollection;
import umontreal.ssj.util.Misc;
import umontreal.iro.lecuyer.util.LineBreaker;
import umontreal.iro.lecuyer.util.ModifiableWorkbook;
import umontreal.iro.lecuyer.util.ArrayUtil;


/**
 * Provides basic methods for formatting matrices of performance measures.
 */
public abstract class PerformanceMeasureFormat {
   private static final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.app");
   private String[] valColumnNames;
   private String[] statColumnNames;
   protected String[] contactTypeProperties;
   protected String[] agentGroupProperties;
   protected List<HistogramChart> histogramList;

   /**
    * Return the standard deviation of this performance measure.
    * Returns $-1$ on error.
    * @param sim contact center
    * @param pm performance measure
    * @param row
    * @param column
    * @return the standard deviation
    */
   protected double getStandardDeviation (ContactCenterSimWithObservations sim,
   		PerformanceMeasureType pm, int row, int column) {
    	try {
   	   double var = sim.getVariance(pm).get(row, column);
   	   return Math.sqrt(var);
   	} catch (final Exception exc) {
      	return -1.0;
   	}
   }


   /**
    * Returns the median of obs
    */
   private double getMedian (double[] obs) {
   	int n = obs.length;
   	int k = (n+1)/2;     // median index
   	double med = Misc.quickSelect(obs, n, k);
   	double y;
   	if ((n & 1) == 0) {
         y = Misc.quickSelect(obs, n, k + 1);
         med = (med + y) / 2.0;
   	}
   	return med;
   }

   /**
    * Draw vertical lines at selected x: median, 95% quantile,
    * 90% quantile,...
    */
   private void drawVerticalLines(double[] obs, HistogramChart chart) {
      final double yfrac = 0.98;          // y-position of text
      double x = getMedian (obs);
      chart.drawVerticalLine (x, "med", yfrac, true);
      int n = obs.length;
      int k = (int) Math.ceil(0.05*n);        // 5% centile
      x = Misc.quickSelect(obs, n, k);
      chart.drawVerticalLine (x, "5%", yfrac, false);
      k = (int) Math.ceil(0.10*n);        // 10% centile
      x = Misc.quickSelect(obs, n, k);
      chart.drawVerticalLine (x, "10%", yfrac, true);
   }


   /**
    * Create a new histogram and add it to the list of histograms.
    * The histogram is built from the observations \texttt{obs} for the
    * performance measure \texttt{pmp}, whose description is given in
    * \texttt{name}. The standard deviation \texttt{sigma} is used to fix
    * the width of the bins, if positive; otherwise it is unused.
    * @param obs the observations
    * @param sigma empirical standard deviation of the observations
    * @param pmp performance measure parameters
    * @param name performance measure name
    */
   protected void createHistogram (double[] obs, double sigma,
   		PerformanceMeasureParams pmp, String name) {
    	if (! pmp.isSetHistogram())
   		return;
   	if (null == histogramList)
   		histogramList = new ArrayList<HistogramChart>();
   	HistogramChart chart = new HistogramChart (name, null, null, obs, obs.length);
      HistogramSeriesCollection collec = chart.getSeriesCollection();
      HistogramParams par = pmp.getHistogram();

      // Set boundaries of histogram
      double a, b;
      if (par.isSetLeftBoundary())
      	a = par.getLeftBoundary();
      else
	   	a = ArrayUtil.min(obs);
    	if (par.isSetRightBoundary())
    	   b = par.getRightBoundary();
    	else
   	   b = ArrayUtil.max(obs);

    	// Set number of bins
      int numBins = -1;
      if (par.isSetNumBins())
      	numBins = par.getNumBins();
      else {
         if (sigma > 0) {
            // Get number of bins from Scott formula (Wikipedia histogram)
         	double n = obs.length;
         	double width = 3.5 * sigma / Math.cbrt(n);
         	numBins = (int) Math.ceil((b - a)/width);
         } else
         	numBins = 15;
      }

      if (!par.isSetLeftBoundary() && !par.isSetRightBoundary()) {
        	collec.setBins(0, numBins);
      } else {
         collec.setBins(0, numBins, a, b);
      }

      drawVerticalLines(obs, chart);

      // Synchronize (more or less) x-axis ticks with some bins
      if (par.isSyncTicks())
         chart.setTicksSynchro(0);

      histogramList.add(chart);
   }

   /**
    * Returns the list of histograms created by the call to
    * \texttt{formatObservations} in derived classes.
    * @return the list of all histograms
    */
   public List<HistogramChart> getHistogramList() {
   	return histogramList;
   }

   /**
    * Show all histograms for the chosen measures on standard output.
    */
   public void writeHistograms() {
      if (null == histogramList)
      	return;
   	for (int i = 0; i < histogramList.size(); i++)
   		histogramList.get(i).view(850, 530);
   }

   /**
    * Writes all histograms for the chosen measures in a LaTex file.
    * Each histogram is written in a separate file.
    */
   public void writeHistogramsLaTeX() {
      if (null == histogramList)
      	return;
   	HistogramChart chart;
   	for (int i = 0; i < histogramList.size(); i++) {
   		chart = histogramList.get(i);
   		String name = chart.getTitle();
   		String name1 = name.replaceAll("\\)", "");
         name = name1.replaceAll(", ", "-");
         name1 = name.replaceAll(" \\(", "-");
       	name = name1.replaceAll(" ", "\\_");
      	name1 = name.concat(".tex");
   		chart.toLatexFile(name1, 12, 8);
   	}
   }

   public PerformanceMeasureFormat() {
      contactTypeProperties = new String[0];
      agentGroupProperties = new String[0];
   }

   public PerformanceMeasureFormat (ReportParams reportParams) {
      contactTypeProperties = getShownProperties (reportParams.getShownContactTypeProperties ());
      agentGroupProperties = getShownProperties (reportParams.getShownAgentGroupProperties ());
   }

   /**
    * Converts a list of property names to an array
    * of strings.
    * @param properties the list of property names.
    * @return the array of strings.
    */
   public static String[] getShownProperties (Collection<PropertyNameParam> properties) {
      if (properties == null || properties.isEmpty ())
         return new String[0];
      final String[] res = new String[properties.size ()];
      int idx = 0;
      for (final PropertyNameParam pp : properties)
         res[idx++] = pp.getName ();
      return res;
   }

   public int getNumPropRows (PerformanceMeasureType pm, boolean singleRow) {
      final int numProp;
      if (pm.getRowType ().isContactType ())
         numProp = contactTypeProperties.length;
      else if (pm.getRowType ().isContactTypeAgentGroup ())
         numProp = contactTypeProperties.length
         + agentGroupProperties.length;
      else if (pm.getRowType () == RowType.AGENTGROUP)
         numProp = agentGroupProperties.length;
      else
         numProp = 0;

      if (singleRow)
         return numProp + getNumPropColumns (pm);
      return numProp;
   }

   public int getNumPropColumns (PerformanceMeasureType pm) {
      if (pm.getColumnType () == ColumnType.AGENTGROUP)
         return agentGroupProperties.length;
      return 0;
   }

   public String[] getPropNameRows (PerformanceMeasureType pm, boolean singleRow) {
      final int num = getNumPropRows (pm, singleRow);
      final String[] props = new String[num];
      if (num == 0)
         return props;
      if (pm.getRowType ().isContactType ())
         System.arraycopy (contactTypeProperties,
               0, props, 0, contactTypeProperties.length);
      else if (pm.getRowType () == RowType.AGENTGROUP)
         System.arraycopy (agentGroupProperties, 0,
               props, 0, agentGroupProperties.length);
      else if (pm.getRowType ().isContactTypeAgentGroup ()) {
         System.arraycopy (contactTypeProperties,
               0, props, 0, contactTypeProperties.length);
         System.arraycopy (agentGroupProperties, 0,
               props, contactTypeProperties.length, agentGroupProperties.length);
      }
      if (singleRow) {
         final String[] propsCol = getPropNameColumns (pm);
         System.arraycopy (propsCol,
               0, props, props.length - propsCol.length, propsCol.length);
      }
      return props;
   }

   public String[] getPropNameColumns (PerformanceMeasureType pm) {
      final int num = getNumPropColumns (pm);
      final String[] props = new String[num];
      if (num == 0)
         return props;
      if (pm.getColumnType () == ColumnType.AGENTGROUP)
         System.arraycopy (agentGroupProperties, 0,
               props, 0, agentGroupProperties.length);
      return props;
   }

   public String[] getPropRows (ContactCenterInfo eval, PerformanceMeasureType pm, int row, int column) {
      final String[] props1 = getPropRows (eval, pm, row);
      final String[] props2 = getPropColumns (eval, pm, column);
      final String[] props = new String[props1.length + props2.length];
      System.arraycopy (props1, 0, props, 0, props1.length);
      System.arraycopy (props2, 0, props, props1.length, props2.length);
      return props;
   }

   public String[] getPropRows (ContactCenterInfo eval, PerformanceMeasureType pm, int row) {
      if (pm.getRowType ().isContactType ())
         return getProperties (pm.rowProperties (eval, row), contactTypeProperties);
      else if (pm.getRowType () == RowType.AGENTGROUP)
         return getProperties (pm.rowProperties (eval, row), agentGroupProperties);
      else if (pm.getRowType ().isContactTypeAgentGroup ()) {
         final int ng = RowType.AGENTGROUP.count (eval);
         final int k = row / ng;
         final int i = row % ng;
         final String[] props1 = getProperties (pm.getRowType ().toContactType ().getProperties (eval, k), contactTypeProperties);
         final String[] props2 = getProperties (RowType.AGENTGROUP.getProperties (eval, i), agentGroupProperties);
         final String[] props = new String[contactTypeProperties.length + agentGroupProperties.length];
         System.arraycopy (props1, 0, props, 0, props1.length);
         System.arraycopy (props2, 0, props, props1.length, props2.length);
         return props;
      }
      return new String[0];
   }

   private String[] getProperties (Map<String, String> properties, String[] names) {
      final String[] props = new String[names.length];
      if (names.length == 0)
         return props;
      for (int p = 0; p < names.length; p++) {
         final String v = properties.get (names[p]);
         if (v == null)
            props[p] = "";
         else
            props[p] = v;
      }
      return props;
   }

   public String[] getPropColumns (ContactCenterInfo eval, PerformanceMeasureType pm, int column) {
      if (pm.getColumnType () == ColumnType.AGENTGROUP)
         return getProperties (pm.columnProperties (eval, column), agentGroupProperties);
      return new String[0];
   }

   /**
    * Returns the number of rows in the summary
    * report.
    * This corresponds to the number of elements
    * in \texttt{pms} for which
    * {@link #isIncludedInSummary(ContactCenterEval,PerformanceMeasureType)}
    * returns \texttt{true}.
    * @param pms the array of performance measure types.
    * @return the number of rows.
    */
   public int countRowsSummary (ContactCenterEval eval, PerformanceMeasureType... pms) {
      int nrows = 0;
      for (final PerformanceMeasureType pm : pms)
         if (isIncludedInSummary (eval, pm))
            nrows++;
      return nrows;
   }

   /**
    * Determines if the performance measure type \texttt{pm} is included in
    * reports.
    * By default, this returns \texttt{false} only if \texttt{pm} is
    * \texttt{null}, if it is equal to
    * {@link PerformanceMeasureType#SUMWAITINGTIMES}, or if
    * {@link PerformanceMeasureType#getEstimationType()
    * pm.get\-Estimation\-Type()} returns
    * {@link EstimationType#EXPECTATIONOFFUNCTION}.
    *
    * @param pm
    *           the tested type of performance measure.
    * @return \texttt{true} if and only if the performance measure type must be
    *         included in reports.
    */
   public boolean isIncludedInReport (ContactCenterEval eval, PerformanceMeasureType pm) {
      return pm != null && eval.hasPerformanceMeasure (pm) && pm.rows (eval) > 0 && pm.columns (eval) > 0;
   }

   /**
    * Determines if the performance measure type \texttt{pm} is included in
    * reports when printed statistics are not specified
    * by the user.
    * By default, this returns \texttt{false} only if \texttt{pm} is
    * \texttt{null}, if it is equal to
    * {@link PerformanceMeasureType#SUMWAITINGTIMES}, or if
    * {@link PerformanceMeasureType#getEstimationType()
    * pm.get\-Estimation\-Type()} returns
    * {@link EstimationType#EXPECTATIONOFFUNCTION}.
    *
    * @param pm
    *           the tested type of performance measure.
    * @return \texttt{true} if and only if the performance measure type must be
    *         included in reports.
    */
   public boolean isIncludedInDefaultReport (PerformanceMeasureType pm) {
      if (pm == null
            || pm.getEstimationType () == EstimationType.EXPECTATIONOFFUNCTION)
         return false;
      switch (pm) {
      case SUMWAITINGTIMES:
      case SUMWAITINGTIMESSERVED:
      case SUMEXCESSTIMES:
      case SUMEXCESSTIMESSERVED:
      case SUMEXCESSTIMESABANDONED:
      case SUMWAITINGTIMESABANDONED:
      case SUMSERVICETIMES:
      case SUMWAITINGTIMESVQ:
      case SUMWAITINGTIMESVQABANDONED:
      case SUMWAITINGTIMESVQSERVED:
      case RATEOFARRIVALSIN:
         return false;
      }
      return true;
   }

   /**
    * Determines if the performance measure type \texttt{pm} is included in the
    * summary of reports.
    *
    * By default, this method returns \texttt{true} if
    * {@link #isIncludedInReport(ContactCenterEval,PerformanceMeasureType)
    * is\-Included\-In\-Report (eval, pm)} returns \texttt{true}, and if
    * \texttt{pm} does not correspond to
    * {@link PerformanceMeasureType#SERVEDRATES}.
    *
    * @param pm
    *           the tested type of performance measure.
    * @return \texttt{true} if and only if \texttt{pm} is included in summary
    *         for reports.
    */
   public boolean isIncludedInSummary (ContactCenterEval eval, PerformanceMeasureType pm) {
      return isIncludedInReport (eval, pm) && pm != null
         && pm != PerformanceMeasureType.SERVEDRATES;
   }

   private String formatProperties (Map<String, String> properties, String[] names) {
      final String[] props = getProperties (properties, names);
      if (props.length == 0)
         return "";

      boolean first = true;
      final StringBuilder sb = new StringBuilder();
      for (int p = 0; p < props.length; p++) {
         if (props[p] == null || props[p].length () == 0)
            continue;
         if (first) {
            sb.append ("(");
            first = false;
         }
         else
            sb.append (", ");
         sb.append (names[p]).append ('=')
         .append (props[p]);
      }
      if (!first)
         sb.append (")");
      return sb.toString ();
   }

   private String formatNameAndProperties (String name, Map<String, String> properties, String[] names) {
      final String str = formatProperties (properties, names);
      if (str.length () == 0)
         return name;
      return name + " " + str;
   }

   public String rowNameWithProperties (ContactCenterInfo eval, PerformanceMeasureType pm, int row) {
      final String rowName = pm.rowName (eval, row);
      if (pm.getRowType ().isContactType () &&
            contactTypeProperties.length > 0)
         return formatNameAndProperties (rowName, pm.rowProperties (eval, row), contactTypeProperties);
      else if (pm.getRowType ().isContactTypeAgentGroup () &&
            (contactTypeProperties.length > 0 ||
                  agentGroupProperties.length > 0)) {
         final int ng = RowType.AGENTGROUP.count (eval);
         final int k = row / ng;
         final int i = row % ng;
         final StringBuilder sb = new StringBuilder();
         final RowType typeRow = pm.getRowType ().toContactType ();
         final RowType groupRow = RowType.AGENTGROUP;
         sb.append (formatNameAndProperties (typeRow.getName (eval, k),
               typeRow.getProperties (eval, k), contactTypeProperties));
         sb.append (", ");
         sb.append (formatNameAndProperties (groupRow.getName (eval, i),
               groupRow.getProperties (eval, i), agentGroupProperties));
         return sb.toString ();
      }
      else if (pm.getRowType () == RowType.AGENTGROUP &&
            agentGroupProperties.length > 0)
         return formatNameAndProperties
         (rowName, pm.rowProperties (eval, row), agentGroupProperties);
      return rowName;
   }

   public String columnNameWithProperties (ContactCenterInfo eval, PerformanceMeasureType pm, int col) {
      final String columnName = pm.columnName (eval, col);
      if (pm.getColumnType () == ColumnType.AGENTGROUP)
         return formatNameAndProperties
         (columnName, pm.columnProperties (eval, col), agentGroupProperties);
      return columnName;
   }

   /**
    * Returns the name associated with the performance measure of type
    * \texttt{pm}, at row \texttt{row}, and column \texttt{col}. This name is
    * constructed by using {@link PerformanceMeasureType#rowName}, and
    * {@link PerformanceMeasureType#columnName}.
    *
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the performance measure type.
    * @param row
    *           the row index.
    * @param col
    *           the column index.
    * @return the name of the measure.
    */
   public String getName (ContactCenterInfo eval, PerformanceMeasureType pm,
         int row, int col) {
      final String rowName = pm.rowName (eval, row);
      //final String rowName = rowNameWithProperties (eval, pm, row);
      final String rowNameCap = capitalizeFirstLetter (rowName);
      if (pm.columns (eval) > 1) {
         final String colName = pm.columnName (eval, col);
         if (colName.length () > 0)
            return rowNameCap + ", " + colName; //$NON-NLS-1$
      }
      return rowNameCap;
   }

   public String getNameWithProperties (ContactCenterInfo eval, PerformanceMeasureType pm,
         int row, int col) {
      final String rowName = rowNameWithProperties (eval, pm, row);
      final String rowNameCap = capitalizeFirstLetter (rowName);
      if (pm.columns (eval) > 1) {
         final String colName = columnNameWithProperties (eval, pm, col);
         if (colName.length () > 0)
            return rowNameCap + ", " + colName; //$NON-NLS-1$
      }
      return rowNameCap;
   }

   /**
    * Returns the string \texttt{s} with the first letter in uppercase. If
    * \texttt{s} is empty or \texttt{null}, this returns \texttt{s} unchanged.
    *
    * @param s
    *           the string to capitalized.
    * @return the new string with the first letter in upper case.
    */
   public String capitalizeFirstLetter (String s) {
      if (s == null || s.length() == 0)
         return s;
      if (s.length() == 1)
         return s.toUpperCase();
      return s.substring (0, 1).toUpperCase () + s.substring (1);
   }

   /**
    * Name of the columns for tables containing values of performance measures.
    * This array contains a single string representing the ``Values'' column of
    * tables of results.
    */
   public String[] getValColumnNames () {
      if (valColumnNames == null)
         valColumnNames = new String[] { Messages
               .getString ("PerformanceMeasureFormat.Value") }; //$NON-NLS-1$
      return valColumnNames;
   }

   /**
    * Name of the columns for tables containing statistics concerning
    * performance measures. This array contains five elements representing
    * columns for the minimum, the maximum, the average, the standard deviation,
    * and the confidence interval.
    */
   public String[] getStatColumnNames () {
      if (statColumnNames == null)
         statColumnNames = new String[] {
               Messages.getString ("PerformanceMeasureFormat.Min"), //$NON-NLS-1$
               Messages.getString ("PerformanceMeasureFormat.Max"), //$NON-NLS-1$
               Messages.getString ("PerformanceMeasureFormat.Average"),//$NON-NLS-1$
               Messages.getString ("PerformanceMeasureFormat.StdDev"),//$NON-NLS-1$
               Messages.getString ("PerformanceMeasureFormat.ConfInt") };//$NON-NLS-1$
      return statColumnNames;
   }

   /**
    * Returns a default array of parameters for printed statistics, for the
    * evaluation system \texttt{eval}. This method uses
    * {@link ContactCenterEval#getPerformanceMeasures} to obtain an array of
    * performance measures. For each element of this array, it creates a
    * {@link PrintedStatParams} instance, and adds it into the returned list.
    * Parameters for printed statistics are set to default, i.e., detailed
    * statistics for all periods are printed.
    *
    * @param eval
    *           the evaluation system.
    * @return the array of parameters for printed statistics.
    */
   public PrintedStatParams[] getDefaultPrintedStatParams (
         ContactCenterEval eval, ReportParams reportParams) {
      final List<PrintedStatParams> list = new ArrayList<PrintedStatParams> ();
      for (final PerformanceMeasureType pm : eval.getPerformanceMeasures ())
         if (isIncludedInDefaultReport (pm)) {
            final PrintedStatParams ps = new PrintedStatParams ();
            ps.setMeasure (pm.name ());
            ps.setDetailed (reportParams.isDefaultDetailed ());
            ps.setOnlyAverages (reportParams.isDefaultOnlyAverages ());
            ps.setPeriods (reportParams.isDefaultPeriods ());
            list.add (ps);
         }
      return list.toArray (new PrintedStatParams[list.size ()]);
   }

   /**
    * Constructs an array of performance measure types from the given array of
    * printed statistics.
    *
    * @param pstats
    *           the array of printed statistics.
    * @return the array of performance measure types.
    */
   public PerformanceMeasureType[] getPerformanceMeasures (
         PrintedStatParams[] pstats) {
      final List<PerformanceMeasureType> pmsList = new ArrayList<PerformanceMeasureType> ();
      for (final PrintedStatParams p : pstats)
         pmsList.add (PerformanceMeasureType.valueOf (p.getMeasure ()));
      return pmsList.toArray (new PerformanceMeasureType[pmsList.size ()]);
   }

   /**
    * Constructs an array of performance measure types from the given array of
    * printed statistics, and a row type.
    * This method is similar to
    * {@link #getPerformanceMeasures(PrintedStatParams[])}
    * except it returns measure types with
    * a row type corresponding to \texttt{rowType}.
    *
    * @param pstats
    *           the array of printed statistics.
    * @param rowTypes the row types.
    * @return the array of performance measure types.
    */
   public PerformanceMeasureType[] getPerformanceMeasures (PrintedStatParams[] pstats, RowType... rowTypes) {
      final List<PerformanceMeasureType> pmsList = new ArrayList<PerformanceMeasureType> ();
      for (final PrintedStatParams p : pstats) {
         final PerformanceMeasureType m = PerformanceMeasureType.valueOf (p.getMeasure ());
         for (final RowType rt : rowTypes)
            if (m.getRowType () == rt) {
               pmsList.add (m);
               break;
            }
      }
      return pmsList.toArray (new PerformanceMeasureType[pmsList.size ()]);
   }

   /**
    * Constructs an array of performance measure types from the given array of
    * printed statistics, and a row type.
    * This method is similar to
    * {@link #getPerformanceMeasures(PrintedStatParams[])}
    * except it returns measure types with
    * a row type corresponding to \texttt{rowType}, and
    * for which \texttt{p.getOnlyAverages} corresponds
    * to \texttt{onlyAverages}.
    *
    * @param pstats
    *           the array of printed statistics.
    * @param onlyAverages determines the required status of the \texttt{onlyAverages} flag.
    * @param rowTypes the row types.
    * @return the array of performance measure types.
    */
   public PerformanceMeasureType[] getPerformanceMeasures (PrintedStatParams[] pstats,
   		boolean onlyAverages, RowType... rowTypes) {
      final List<PerformanceMeasureType> pmsList = new ArrayList<PerformanceMeasureType> ();
      for (final PrintedStatParams p : pstats) {
         if (onlyAverages != p.isOnlyAverages ())
            continue;
         final PerformanceMeasureType m = PerformanceMeasureType.valueOf (p.getMeasure ());
         for (final RowType rt : rowTypes)
            if (m.getRowType () == rt) {
               pmsList.add (m);
               break;
            }
      }
      return pmsList.toArray (new PerformanceMeasureType[pmsList.size ()]);
   }

   /**
    * Returns the header for simulation results. This string contains the name
    * of the parameter file for the model, i.e., \texttt{ccParamsFn}, the name
    * of the parameter file for the experiment, i.e., \texttt{simParamsFn}, and
    * the current date.
    *
    * @param ccParamsFn
    *           the name of the parameter file for the model.
    * @param simParamsFn
    *           the name of the parameter file for the experiment.
    */
   public static void addExperimentInfo (Map<String, Object> evalInfo,
         String ccParamsFn, String simParamsFn) {
      if (ccParamsFn != null)
         evalInfo.put (Messages.getString
         	("PerformanceMeasureFormat.CallCenterParameters"), ccParamsFn); //$NON-NLS-1$
      if (simParamsFn != null)
         evalInfo.put (Messages.getString
            ("PerformanceMeasureFormat.SimulationParameters"), simParamsFn); //$NON-NLS-1$
      evalInfo.put (Messages.getString
      		("PerformanceMeasureFormat.ExperimentStartingDate"), new Date ()); //$NON-NLS-1$
   }

   /**
    * Equivalent to {@link #formatResults(ContactCenterEval,File)},
    * with a string given the file name instead of
    * a file object.
    */
   public static void formatResults (ContactCenterEval eval, String outputFileName)
         throws IOException, JAXBException {
      formatResults (eval, outputFileName == null ? null : new File (outputFileName));
   }

   /**
    * Formats the results of the last evaluation performed by \texttt{eval}
    * into the file with name \texttt{outputFile}.
    * The format of the file is determined automatically
    * based on its extension.
    * @param eval the evaluation system.
    * @param outputFile the output file
    * @throws IOException if an I/O error occurs.
    * @throws ParserConfigurationException
    *            if the output format is XML, and an error occurred while
    *            constructing the intermediate DOM document.
    * @throws TransformerException
    *            if the output format is XML, and an error occurs during the
    *            transformation of the DOM document into text.
    */
   public static void formatResults (ContactCenterEval eval, File outputFile)
         throws IOException, JAXBException {
      if (outputFile == null)
         System.out.println (eval.formatStatistics ());
      else {
         final String name = outputFile.getName ();
         CCResultsFormat fmt;
         try {
            fmt = CCResultsFormat.valueOfFromFileName (name);
         }
         catch (final IllegalArgumentException e) {
            logger.warning ("Unknown extension for file with name " + name + ", defaulting to plain text format" );
            fmt = CCResultsFormat.TEXT;
         }
         final File f;
         if (outputFile.exists ()) {
            switch (fmt) {
            case EXCEL:
               ModifiableWorkbook mwb;
               try {
                  mwb = new ModifiableWorkbook (outputFile);
               }
               catch (final IOException e) {
                  logger.throwing (PerformanceMeasureFormat.class.getName (), "formatResults", e);
                  logger.warning ("Could not read the existing workbook " + outputFile.getName ());
                  break;
               }
               catch (final BiffException e) {
                  logger.throwing (PerformanceMeasureFormat.class.getName (), "formatResults", e);
                  logger.warning ("Could not read the existing workbook " + outputFile.getName ());
                  break;
               }
               WritableWorkbook wb = mwb.getWorkbook ();
               if (eval.formatStatisticsExcel (wb)) {
                  try {
                     mwb.close ();
                  }
                  catch (WriteException we) {
                     throw new IOException ("Error writing to the workbook", we);
                  }
               }
               else {
                  try {
                     mwb.discardChanges ();
                  }
                  catch (WriteException we) {
                     throw new IOException ("Error writing to the workbook", we);
                  }
               }
               return;
            case TEXT:
               final OutputStream stream = new FileOutputStream (outputFile, true);
               final PrintWriter out = new PrintWriter (new OutputStreamWriter (stream));
               out.println ();
               out.println ();
               LineBreaker.writeLines (out, eval.formatStatistics ());
               out.close ();
               return;
            case LATEXT:
               final OutputStream streamL = new FileOutputStream (outputFile, true);
               final PrintWriter outL = new PrintWriter (new OutputStreamWriter (streamL));
               outL.println ();
               outL.println ();
               LineBreaker.writeLines (outL, eval.formatStatisticsLaTeX ());
               outL.close ();
               return;
            }
            final int idx = name.lastIndexOf ('.');
            int n = 1;
            File outputFile2 = null;
            while (outputFile2 == null || outputFile2.exists ()) {
               final String name2 = idx == -1 ? name + n++ : name.substring (0, idx) + n++ + name.substring (idx);
               outputFile2 = new File (outputFile.getParentFile (), name2);
            }
            f = outputFile2;
            logger.warning ("Output file " + outputFile.getName () + " already exists, writing output to " + outputFile2.getName ());
         }
         else
            f = outputFile;
         final OutputStream stream = new FileOutputStream (f);
         boolean done = false;
         try {
            formatResults (eval, stream, fmt);
            done = true;
         }
         finally {
            stream.close ();
            if (!done)
               f.delete();
         }
      }
   }

   /**
    * Formats the results of the evaluation system \texttt{eval}. If
    * \texttt{fmt} is \texttt{null} or empty, this method simply prints the
    * contents returned by \texttt{eval.}{@link ContactCenterEval#formatStatistics()
    * formatStatistics}. Otherwise, it saves the results in \texttt{stream}.
    * Depending on the value of \texttt{fmt}, i.e., \texttt{TEXT},
    * \texttt{BINARY}, \texttt{XML}, or \texttt{EXCEL}, the format of the output
    * file is plain text, binary, XML, or MS Excel, respectively.
    *
    * @param eval
    *           the evaluation system being processed.
    * @param stream
    *           the output stream.
    * @param fmt
    *           the format of the output.
    * @throws IOException
    *            if an I/O error occurs.
    * @throws ParserConfigurationException
    *            if the output format is XML, and an error occurred while
    *            constructing the intermediate DOM document.
    * @throws TransformerException
    *            if the output format is XML, and an error occurs during the
    *            transformation of the DOM document into text.
    */
   public static void formatResults (ContactCenterEval eval, OutputStream stream,
         CCResultsFormat fmt) throws IOException, JAXBException {
      if (fmt == null || fmt == CCResultsFormat.STDOUT)
         System.out.println (eval.formatStatistics ());
      else if (fmt == CCResultsFormat.TEXT) {
         final PrintWriter output = new PrintWriter (new OutputStreamWriter (stream));
         LineBreaker.writeLines (output, eval.formatStatistics ());
         output.close ();
      }
      else if (fmt == CCResultsFormat.LATEXT) {
         final PrintWriter output = new PrintWriter (new OutputStreamWriter (stream));
         LineBreaker.writeLines (output, eval.formatStatisticsLaTeX ());
         output.close ();
      }
      else if (fmt == CCResultsFormat.EXCEL) {
         final WritableWorkbook wb = Workbook.createWorkbook (stream);
         eval.formatStatisticsExcel (wb);
         wb.write ();
         try {
            wb.close ();
         }
         catch (final WriteException e) {}
      }
      else {
         final ContactCenterEvalResults evalRes = ContactCenterEvalResults.createFromEval (eval);

//         if (fmt == CCResultsFormat.BINARY) {
//            final ObjectOutputStream os = new ObjectOutputStream (
//                  stream);
//            os.writeObject (evalRes);
//            os.close ();
//         }
         if (fmt == CCResultsFormat.XMLGZ) {
            final ContactCenterEvalResultsConverter cnv = new ContactCenterEvalResultsConverter();
            final GZIPOutputStream gz = new GZIPOutputStream (stream);
            cnv.marshalEval (evalRes, new StreamResult (gz));
            gz.close ();
         }
         else if (fmt == CCResultsFormat.XML) {
            final ContactCenterEvalResultsConverter cnv = new ContactCenterEvalResultsConverter();
            cnv.marshalEval (evalRes, new StreamResult (stream));
         }
         else
            throw new IllegalArgumentException ("Invalid value of fmt: " + fmt);
      }
   }
}
