package umontreal.iro.lecuyer.contactcenters.app;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import umontreal.iro.lecuyer.contactcenters.app.params.ContactCenterEvalResultsParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ContactCenterSimResultsParams;
import umontreal.iro.lecuyer.contactcenters.app.params.PMMatrix;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatter;
import umontreal.iro.lecuyer.util.DoubleFormatter;
import umontreal.ssj.util.Introspection;
import umontreal.iro.lecuyer.util.LaTeXDoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXObjectMatrixFormatter;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.NamedInfo;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;
import umontreal.iro.lecuyer.xmlbind.params.DoubleArray;
import umontreal.iro.lecuyer.xmlbind.params.Named;
import umontreal.iro.lecuyer.xmlbind.params.PropertiesParams;
import umontreal.iro.lecuyer.xmlbind.params.TimeUnitParam;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Contains results obtained by another contact center evaluation system. This
 * class can be used to store the results produced by a simulator, to retrieve
 * them efficiently, or to serialize them into an XML file. Results can be
 * retrieved by using the methods of {@link ContactCenterEval}. The
 * implementation of the methods of the interface that perform evaluations
 * throws an {@link UnsupportedOperationException}.
 *
 * An object from this class can be constructed using any implementation
 * of {@link ContactCenterEval}, or from an instance of
 * {@link ContactCenterEvalResultsParams}.
 * The {@link #createParams()} method can be used to
 * turn any instance of this class into a parameter object
 * of class {@link ContactCenterEvalResultsParams}.
 *
 * The class {@link ContactCenterEvalResultsConverter}
 * provides convenience method to create an object containing results
 * from an XML file, or to export an object of this class into
 * XML.
 * One can also use {@link PerformanceMeasureFormat#formatResults(ContactCenterEval,java.io.File)}
 * to export simulation results to a file.
 */
public class ContactCenterEvalResults implements ContactCenterEval {
   private final Map<PerformanceMeasureType, DoubleMatrix2D> avgMap = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (
         PerformanceMeasureType.class);
   private NamedInfo[] typeNames;
   private int ki;
   private NamedInfo[] groupNames;
   private NamedInfo[] queueNames;
   private NamedInfo[] mainPeriodNames;
   private int np;
   private String[] mawt;
   private NamedInfo[] typeSegmentNames;
   private NamedInfo[] inboundTypeSegmentNames;
   private NamedInfo[] outboundTypeSegmentNames;
   private NamedInfo[] groupSegmentNames;
   private NamedInfo[] queueSegmentNames;
   private NamedInfo[] mainPeriodSegmentNames;
   private Map<String, Object> evalInfo;
   private ReportParams reportParams;
   private TimeUnit defaultUnit;

   /**
    * Constructs a new object containing results read from the parameter object
    * \texttt{ccp}. It is not recommended to use this constructor directly; one
    * should use the {@link #createFromParams(ContactCenterEvalResultsParams)}
    * method to create instances of this class.
    *
    * @param ccp
    *           the contact centers results.
    */
   public ContactCenterEvalResults (ContactCenterEvalResultsParams ccp) {
      evalInfo = new LinkedHashMap<String, Object> ();
      reportParams = ccp.getReport ();
      defaultUnit = TimeUnit.valueOf (ccp.getDefaultUnit ().name ());
      ki = ccp.getInboundTypes ().size ();
      typeNames = new NamedInfo[ki + ccp.getOutboundTypes ().size ()];
      int idx = 0;
      for (final Named named : ccp.getInboundTypes ())
         typeNames[idx++] = new NamedInfo (named);
      for (final Named named : ccp.getOutboundTypes ())
         typeNames[idx++] = new NamedInfo (named);

      groupNames = getNamedInfo (ccp.getAgentGroups ());
      queueNames = getNamedInfo (ccp.getWaitingQueues ());
      mainPeriodNames = getNamedInfo (ccp.getPeriods ());

      typeSegmentNames = getNamedInfo (ccp.getContactTypeSegments ());
      inboundTypeSegmentNames = getNamedInfo (ccp.getInboundTypeSegments ());
      outboundTypeSegmentNames = getNamedInfo (ccp.getOutboundTypeSegments ());
      groupSegmentNames = getNamedInfo (ccp.getAgentGroupSegments ());
      queueSegmentNames = getNamedInfo (ccp.getWaitingQueueSegments ());
      mainPeriodSegmentNames = getNamedInfo (ccp.getPeriodSegments ());

      np = mainPeriodNames.length;
      mawt = new String[ccp.getNumMatricesOfAWT ()];
      int idx2 = 0;
      for (Named named : ccp.getMatricesOfAWT ()) {
         if (idx2 >= mawt.length)
            break;
         mawt[idx2++] = named.getName ();
      }

      for (final PMMatrix pm : ccp.getPerformanceMeasureMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (pm.getMeasure ());
         if (pmt == null)
            continue;
         final DoubleMatrix2D m = new DenseDoubleMatrix2D (ArrayConverter
               .unmarshalArray (pm));
         avgMap.put (pmt, m);
      }

      final PropertiesParams props = ccp.getEvalInfo ();
      if (props != null)
         evalInfo = ParamReadHelper.unmarshalProperties (props);
   }

   protected static PerformanceMeasureType getPerformanceMeasureType (String name) {
      try {
         return PerformanceMeasureType.valueOf (name);
      }
      catch (IllegalArgumentException iae) {
         // Try with deprecated performance measures
         try {
            return Introspection.valueOf (PerformanceMeasureType.class, name);
         }
         catch (IllegalArgumentException iae2) {
            Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.app");
            logger.warning ("Unrecognized performance measure " + name);
            return null;
         }
      }
   }

   /**
    * Fills \texttt{ccp} with parameters stored in this object. This method is
    * rarely used directly; one should call {@link #createParams()} instead.
    *
    * @param ccp
    *           the output parameter object.
    */
   public void writeParams (ContactCenterEvalResultsParams ccp) {
      if (defaultUnit != null)
         ccp.setDefaultUnit (TimeUnitParam.valueOf (defaultUnit.name ()));
      ccp.setNumMatricesOfAWT (mawt.length);
      ccp.setReport (reportParams);
      ccp.getMatricesOfAWT ().clear ();
      for (int idx = 0; idx < mawt.length; idx++) {
         Named named = new Named();
         named.setName (mawt[idx]);
         ccp.getMatricesOfAWT ().add (named);
      }

      putNamedInfo (ccp.getInboundTypes (), typeNames);
      ccp.getOutboundTypes ().addAll (
            ccp.getInboundTypes ().subList (ki, typeNames.length));
      ccp.getInboundTypes ().subList (ki, typeNames.length).clear ();
      putNamedInfo (ccp.getAgentGroups (), groupNames);
      putNamedInfo (ccp.getWaitingQueues (), queueNames);
      putNamedInfo (ccp.getPeriods (), mainPeriodNames);

      putNamedInfo (ccp.getContactTypeSegments (), typeSegmentNames);
      putNamedInfo (ccp.getInboundTypeSegments (), inboundTypeSegmentNames);
      putNamedInfo (ccp.getOutboundTypeSegments (), outboundTypeSegmentNames);
      putNamedInfo (ccp.getAgentGroupSegments (), groupSegmentNames);
      putNamedInfo (ccp.getWaitingQueueSegments (), queueSegmentNames);
      putNamedInfo (ccp.getPeriodSegments (), mainPeriodSegmentNames);

      ccp.getPerformanceMeasureMatrices ().clear ();
      for (final Map.Entry<PerformanceMeasureType, DoubleMatrix2D> e : avgMap
            .entrySet ()) {
         final PMMatrix mat = new PMMatrix ();
         mat.setMeasure (e.getKey ().name ());
         final DoubleArray dp = ArrayConverter.marshalArray (e.getValue ()
               .toArray ());
         mat.getRows ().addAll (dp.getRows ());
         ccp.getPerformanceMeasureMatrices ().add (mat);
      }

      // Transfer evaluation information
      if (!getEvalInfo ().isEmpty ()) {
         PropertiesParams props = ParamReadHelper
               .marshalProperties (getEvalInfo ());
         ccp.setEvalInfo (props);
      }
   }

   private void putNamedInfo (List<Named> list, NamedInfo... infos) {
      list.clear ();
      if (infos == null)
         return;
      for (final NamedInfo info : infos) {
         final Named named = new Named ();
         if (info != null) {
            named.setName (info.getName ());
            named.setProperties (ParamReadHelper.marshalProperties (info
                  .getProperties ()));
         }
         list.add (named);
      }
   }

   private NamedInfo[] getNamedInfo (List<Named> list) {
      final NamedInfo[] res = new NamedInfo[list.size ()];
      int idx = 0;
      for (final Named named : list)
         res[idx++] = new NamedInfo (named);
      return res;
   }

   /**
    * Constructs a new contact center results container by getting evaluation
    * results from the system \texttt{eval}. It is not recommended to use this
    * constructor directly; one should call
    * {@link #createFromEval(ContactCenterEval)} instead.
    *
    * @param eval
    *           the contact center evaluator.
    */
   public ContactCenterEvalResults (ContactCenterEval eval) {
      defaultUnit = eval.getDefaultUnit ();
      reportParams = eval.getReportParams ();
      for (final PerformanceMeasureType pm : eval.getPerformanceMeasures ())
         avgMap.put (pm, eval.getPerformanceMeasure (pm));
      typeNames = new NamedInfo[eval.getNumContactTypes ()];
      for (int k = 0; k < typeNames.length; k++)
         typeNames[k] = new NamedInfo (eval.getContactTypeName (k), eval
               .getContactTypeProperties (k));
      groupNames = new NamedInfo[eval.getNumAgentGroups ()];
      for (int i = 0; i < groupNames.length; i++)
         groupNames[i] = new NamedInfo (eval.getAgentGroupName (i), eval
               .getAgentGroupProperties (i));
      queueNames = new NamedInfo[eval.getNumWaitingQueues ()];
      for (int q = 0; q < queueNames.length; q++)
         queueNames[q] = new NamedInfo (eval.getWaitingQueueName (q), eval
               .getWaitingQueueProperties (q));
      mainPeriodNames = new NamedInfo[eval.getNumMainPeriods ()];
      for (int mp = 0; mp < mainPeriodNames.length; mp++)
         mainPeriodNames[mp] = new NamedInfo (eval.getMainPeriodName (mp));

      typeSegmentNames = new NamedInfo[eval.getNumContactTypeSegments ()];
      for (int k = 0; k < typeSegmentNames.length; k++)
         typeSegmentNames[k] = new NamedInfo (eval
               .getContactTypeSegmentName (k), eval
               .getContactTypeSegmentProperties (k));
      inboundTypeSegmentNames = new NamedInfo[eval
            .getNumInContactTypeSegments ()];
      for (int k = 0; k < inboundTypeSegmentNames.length; k++)
         inboundTypeSegmentNames[k] = new NamedInfo (eval
               .getInContactTypeSegmentName (k), eval
               .getInContactTypeSegmentProperties (k));
      outboundTypeSegmentNames = new NamedInfo[eval
            .getNumOutContactTypeSegments ()];
      for (int k = 0; k < outboundTypeSegmentNames.length; k++)
         outboundTypeSegmentNames[k] = new NamedInfo (eval
               .getOutContactTypeSegmentName (k), eval
               .getOutContactTypeSegmentProperties (k));
      groupSegmentNames = new NamedInfo[eval.getNumAgentGroupSegments ()];
      for (int i = 0; i < groupSegmentNames.length; i++)
         groupSegmentNames[i] = new NamedInfo (eval
               .getAgentGroupSegmentName (i), eval
               .getAgentGroupSegmentProperties (i));
      queueSegmentNames = new NamedInfo[eval.getNumWaitingQueueSegments ()];
      for (int q = 0; q < queueSegmentNames.length; q++)
         queueSegmentNames[q] = new NamedInfo (eval
               .getWaitingQueueSegmentName (q), eval
               .getWaitingQueueSegmentProperties (q));
      mainPeriodSegmentNames = new NamedInfo[eval.getNumMainPeriodSegments ()];
      for (int mp = 0; mp < mainPeriodSegmentNames.length; mp++)
         mainPeriodSegmentNames[mp] = new NamedInfo (eval
               .getMainPeriodSegmentName (mp));

      ki = eval.getNumInContactTypes ();
      np = eval.getNumMainPeriods ();
      mawt = new String[eval.getNumMatricesOfAWT ()];
      for (int m = 0; m < mawt.length; m++)
         mawt[m] = eval.getMatrixOfAWTName (m);
      evalInfo = new LinkedHashMap<String, Object> (eval.getEvalInfo ());
      reportParams = eval.getReportParams ();
   }

   public Map<String, Object> getEvalInfo () {
      return evalInfo;
   }

   public int getNumContactTypes () {
      return typeNames.length;
   }

   public int getNumInContactTypes () {
      return ki;
   }

   public int getNumOutContactTypes () {
      return typeNames.length - ki;
   }

   public int getNumAgentGroups () {
      return groupNames.length;
   }

   public int getNumWaitingQueues () {
      return queueNames.length;
   }

   public int getNumMainPeriods () {
      return np;
   }

   public String getContactTypeName (int k) {
      return typeNames[k].getName ();
   }

   public String getAgentGroupName (int i) {
      return groupNames[i].getName ();
   }

   public String getWaitingQueueName (int q) {
      return queueNames[q].getName ();
   }

   public String getMainPeriodName (int mp) {
      return mainPeriodNames[mp].getName ();
   }

   public int getNumMatricesOfAWT () {
      return mawt.length;
   }

   public String getMatrixOfAWTName (int m) {
      return mawt[m];
   }

   public EvalOptionType[] getEvalOptions () {
      return new EvalOptionType[0];
   }

   public boolean hasEvalOption (EvalOptionType option) {
      return false;
   }

   public Object getEvalOption (EvalOptionType option) {
      throw new NoSuchElementException ("Unsupported evaluation option "
            + option.name ());
   }

   public void setEvalOption (EvalOptionType option, Object value) {
      throw new NoSuchElementException ("Unsupported evaluation option "
            + option.name ());
   }

   public void eval () {
      throw new UnsupportedOperationException ();
   }

   public boolean seemsUnstable () {
      return false;
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      return avgMap.keySet ().toArray (
            new PerformanceMeasureType[avgMap.size ()]);
   }

   public boolean hasPerformanceMeasure (PerformanceMeasureType m) {
      return avgMap.containsKey (m);
   }

   public DoubleMatrix2D getPerformanceMeasure (PerformanceMeasureType m) {
      final DoubleMatrix2D mat = avgMap.get (m);
      if (mat == null)
         throw new NoSuchElementException ("Performance measure type "
               + m.name () + " not available");
      return mat;
   }

   public void reset () {
      throw new UnsupportedOperationException ();
   }

   public boolean isVerbose () {
      return false;
   }

   public void setVerbose (boolean v) {
      throw new UnsupportedOperationException ();
   }

   public ReportParams getReportParams () {
      if (reportParams == null)
         reportParams = new ReportParams ();
      return reportParams;
   }

   public void setReportParams (
         umontreal.iro.lecuyer.contactcenters.app.params.ReportParams reportParams) {
      this.reportParams = reportParams;
   }

   public String formatStatistics () {
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (
            getReportParams ());
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms();
      return pfmt.formatValues (this, getReportParams ());
   }

   public String formatStatisticsLaTeX () {
      final DoubleFormatter dfmt = new DefaultDoubleFormatter (
            getReportParams ().getNumDigits (), getReportParams ()
                  .getNumDigits ());
      final DoubleFormatter dfmtLaTeX = new LaTeXDoubleFormatter (dfmt);
      final LaTeXObjectMatrixFormatter fmt = new LaTeXObjectMatrixFormatter ();
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (
            fmt, getReportParams ());
      pfmt.setDoubleFormatterValues (dfmtLaTeX);
      pfmt.setPercentString ("\\%");
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistogramsLaTeX();
      return pfmt.formatValues (this, getReportParams ());
   }

   public boolean formatStatisticsExcel (WritableWorkbook wb) {
      final PerformanceMeasureFormatExcel pfmt = new PerformanceMeasureFormatExcel (
            wb, getReportParams ());
     /* if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms(); */
      pfmt.setMaxColumns ((short) getReportParams ().getMaxColumns ());
      try {
         return pfmt.formatValues (this, getReportParams ());
      }
      catch (final WriteException e) {
         final IllegalArgumentException iae = new IllegalArgumentException (
               "Could not write workbook");
         iae.initCause (e);
         throw iae;
      }
   }

   void checkNulls (String mapName, Map<?, ?> map) {
      for (final Map.Entry<?, ?> e : map.entrySet ()) {
         if (e.getKey () == null)
            throw new NullPointerException (mapName + " contains the null key");
         if (e.getValue () == null)
            throw new NullPointerException ("The key "
                  + e.getKey ().toString () + " in " + mapName
                  + " corresponds to a null value");
      }
   }

   public void check () {
      internalCheck ();
   }

   private void internalCheck () {
      // Scans the internal structures for any problem
      if (typeNames == null)
         throw new NullPointerException ("typeNames is null");
      if (queueNames == null)
         throw new NullPointerException ("queueNames is null");
      if (groupNames == null)
         throw new NullPointerException ("groupNames is null");
      if (mainPeriodNames == null)
         throw new NullPointerException ("mainPeriodNames is null");
      if (ki < 0 || ki > typeNames.length)
         throw new IllegalArgumentException ("Invalid value of ki");
      if (np < 0)
         throw new IllegalArgumentException (
               "The number of main periods must not be negative");
      checkNulls ("Average map", avgMap);
   }

   public TimeUnit getDefaultUnit () {
      return defaultUnit;
   }

   public Map<String, String> getAgentGroupProperties (int i) {
      return groupNames[i].getStringProperties ();
   }

   public String getAgentGroupSegmentName (int i) {
      return groupSegmentNames[i].getName ();
   }

   public Map<String, String> getAgentGroupSegmentProperties (int i) {
      return groupSegmentNames[i].getStringProperties ();
   }

   public Map<String, String> getContactTypeProperties (int k) {
      return typeNames[k].getStringProperties ();
   }

   public String getContactTypeSegmentName (int k) {
      return typeSegmentNames[k].getName ();
   }

   public Map<String, String> getContactTypeSegmentProperties (int k) {
      return typeSegmentNames[k].getStringProperties ();
   }

   public String getInContactTypeSegmentName (int k) {
      return inboundTypeSegmentNames[k].getName ();
   }

   public Map<String, String> getInContactTypeSegmentProperties (int k) {
      return inboundTypeSegmentNames[k].getStringProperties ();
   }

   public String getMainPeriodSegmentName (int mp) {
      return mainPeriodSegmentNames[mp].getName ();
   }

   public int getNumAgentGroupSegments () {
      return groupSegmentNames.length;
   }

   public int getNumContactTypeSegments () {
      return typeSegmentNames.length;
   }

   public int getNumInContactTypeSegments () {
      return inboundTypeSegmentNames.length;
   }

   public int getNumMainPeriodSegments () {
      return mainPeriodSegmentNames.length;
   }

   public int getNumOutContactTypeSegments () {
      return outboundTypeSegmentNames.length;
   }

   public int getNumWaitingQueueSegments () {
      return queueSegmentNames.length;
   }

   public String getOutContactTypeSegmentName (int k) {
      return outboundTypeSegmentNames[k].getName ();
   }

   public Map<String, String> getOutContactTypeSegmentProperties (int k) {
      return outboundTypeSegmentNames[k].getStringProperties ();
   }

   public Map<String, String> getWaitingQueueProperties (int q) {
      return queueNames[q].getStringProperties ();
   }

   public String getWaitingQueueSegmentName (int k) {
      return queueSegmentNames[k].getName ();
   }

   public Map<String, String> getWaitingQueueSegmentProperties (int q) {
      return queueSegmentNames[q].getStringProperties ();
   }

   /**
    * Constructs a new object storing the last results produced by the
    * evaluation system \texttt{eval}. The resulting object is completely
    * independent of \texttt{eval}. As a result, this method can be used to take
    * snapshots of evaluation results, and store them, e.g., to compare
    * different scenarios.
    *
    * If \texttt{eval} is an instance of {@link ContactCenterSim}, this method
    * returns an instance of {@link ContactCenterSimResults}. Otherwise, this
    * returns an instance of {@link ContactCenterEvalResults}.
    *
    * @param eval
    *           the source evaluation system.
    * @return an object containing a copy of the evaluation results.
    */
   public static ContactCenterEvalResults createFromEval (ContactCenterEval eval) {
      if (eval instanceof ContactCenterSim)
         return new ContactCenterSimResults ((ContactCenterSim) eval);
      return new ContactCenterEvalResults (eval);

   }

   /**
    * Parses the given parameter object \texttt{ccp}, and constructs an object
    * containing evaluation results. The class of \texttt{ccp},
    * {@link ContactCenterEvalResultsParams}, was generated using JAXB, so it
    * does not implement the {@link ContactCenterEval} interface. Usually,
    * \texttt{ccp} is constructed by using JAXB. This method reads the
    * parameters of \texttt{ccp}, and stores them in such a way that they can be
    * accessed efficiently using methods of {@link ContactCenterEval}. The
    * resulting object is an instance of {@link ContactCenterSimResults} if
    * \texttt{ccp} is an instance of {@link ContactCenterSimResultsParams}.
    *
    * @param ccp
    *           the parameter object containing results.
    * @return the object storing results.
    */
   public static ContactCenterEvalResults createFromParams (
         ContactCenterEvalResultsParams ccp) {
      if (ccp instanceof ContactCenterSimResultsParams)
         return new ContactCenterSimResults (
               (ContactCenterSimResultsParams) ccp);
      return new ContactCenterEvalResults (ccp);
   }

   /**
    * Similar to the method {@link #createFromParams(ContactCenterEvalResultsParams)},
    * but if the flag \texttt{reportPropertiesToEvalInfo} is set to \texttt{true},
    * copies the report properties into the evaluation information.
    * In the early version of the output format, evaluation information, e.g.,
    * properties specific to a given experiment, were stored into the properties
    * of the \texttt{report} child element. An \texttt{evalInfo}
    * element has recently been added to store the evaluation information,
    * reserving report properties for user-defined options specific to
    * reporting.
    * This method is provided to read old-style output files, and should
    * not be used for newer output files.
    * @param ccp the contact center parameters.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the contact center evaluation results.
    */
   public static ContactCenterEvalResults createFromParams (
         ContactCenterEvalResultsParams ccp, boolean reportPropertiesToEvalInfo) {
      ContactCenterEvalResults res = createFromParams (ccp);
      if (reportPropertiesToEvalInfo && !ccp.isSetEvalInfo ())
         addToEvalInfo (res.getEvalInfo (), ccp.getReport ().getProperties ());
      return res;
   }

   private static void addToEvalInfo (Map<String, Object> evalInfo, PropertiesParams props) {
      if (props == null)
         return;
      evalInfo.putAll (ParamReadHelper.unmarshalProperties (props));
   }

   /**
    * Creates a parameter object that can be marshalled using JAXB from this
    * object, and copies its evaluation results..
    *
    * @return the parameter object containing results.
    */
   public ContactCenterEvalResultsParams createParams () {
      final ContactCenterEvalResultsParams ccp = new ContactCenterEvalResultsParams ();
      writeParams (ccp);
      return ccp;
   }

   public int getNumContactTypesWithSegments () {
      final int K = getNumContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumInContactTypesWithSegments () {
      final int K = getNumInContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumInContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumOutContactTypesWithSegments () {
      final int K = getNumOutContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumOutContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumAgentGroupsWithSegments () {
      final int I = getNumAgentGroups ();
      if (I <= 1)
         return I;
      final int s = getNumAgentGroupSegments ();
      return I + s + 1; // Include implicit segment
   }

   public int getNumMainPeriodsWithSegments () {
      final int P = getNumMainPeriods ();
      if (P <= 1)
         return P;
      final int s = getNumMainPeriodSegments ();
      return P + s + 1; // Include implicit segment
   }

   public int getNumWaitingQueuesWithSegments () {
      final int Q = getNumWaitingQueues ();
      if (Q <= 1)
         return Q;
      final int s = getNumWaitingQueueSegments ();
      return Q + s + 1; // Include implicit segment
   }
}
