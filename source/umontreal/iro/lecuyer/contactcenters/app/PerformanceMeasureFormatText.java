package umontreal.iro.lecuyer.contactcenters.app;

import java.lang.reflect.Array;
import java.sql.Time;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.contactcenters.app.params.PerformanceMeasureParams;
import umontreal.iro.lecuyer.contactcenters.app.params.PrintedStatParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.ssj.util.AbstractChrono;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatter;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatterWithError;
import umontreal.iro.lecuyer.util.DoubleFormatter;
import umontreal.iro.lecuyer.util.DoubleFormatterWithError;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;
import cern.colt.matrix.objectalgo.Formatter;

/**
 * Defines some facilities to format performance measures as strings. For each
 * estimated performance measure, an evaluation system can produce matrices of
 * results which may need to be formatted to be displayed on-screen or included
 * in printable documents. This class defines methods to create summary reports
 * for several performance measures or a detailed report for a particular
 * measure.
 *
 * Each formatting method constructs an {@link ObjectMatrix2D} instance
 * containing {@link String} elements, each numerical value being processed
 * using a double formatter implementing interfaces {@link DoubleFormatter} or
 * {@link DoubleFormatterWithError}. After the intermediate matrix is
 * constructed, the method uses an instance of {@link Formatter} to turn it into
 * a {@link String}. By default, this class is adapted for on-screen reports,
 * but methods may be overridden for other types of formatting.
 */
public class PerformanceMeasureFormatText extends PerformanceMeasureFormat {
   private Formatter fmt;
   private DoubleFormatter dfmtVal;
   private DoubleFormatter dfmtStat;
   private String percentString = "%";

   
   /**
    * Creates a performance measure formatter using the default
    * {@link Formatter} implementation adapted for on-screen printing.
    */
   public PerformanceMeasureFormatText () {
      fmt = new Formatter ();
      dfmtVal = new DefaultDoubleFormatter ();
      dfmtStat = new DefaultDoubleFormatterWithError ();
   }

   /**
    * Constructs a performance measure formatter with the formatter
    * \texttt{fmt}.
    *
    * @param fmt
    *           the user-defined formatter object.
    * @exception NullPointerException
    *               if \texttt{fmt} is \texttt{null}.
    */
   public PerformanceMeasureFormatText (Formatter fmt) {
      if (fmt == null)
         throw new NullPointerException ("fmt must not be null"); //$NON-NLS-1$
      this.fmt = fmt;
      dfmtVal = new DefaultDoubleFormatter ();
      dfmtStat = new DefaultDoubleFormatterWithError ();
   }

   /**
    * Constructs a performance measure formatter with the matrix formatter
    * \texttt{fmt}, the double-precision formatter \texttt{fmtVal} for values
    * (with unknown error), and \texttt{fmtStat} for statistics (with an
    * estimated error).
    *
    * @param fmt
    *           the user-defined formatter object.
    * @param dfmtVal
    *           the double-precision formatter for values.
    * @param dfmtStat
    *           the doubl-precision formatter for statistics.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public PerformanceMeasureFormatText (Formatter fmt, DoubleFormatter dfmtVal,
         DoubleFormatter dfmtStat) {
      if (fmt == null || dfmtVal == null || dfmtStat == null)
         throw new NullPointerException ("fmt must not be null"); //$NON-NLS-1$
      this.fmt = fmt;
      this.dfmtVal = dfmtVal;
      this.dfmtStat = dfmtStat;
   }

   public PerformanceMeasureFormatText (ReportParams reportParams) {
      this (new Formatter(), reportParams);
   }

   public PerformanceMeasureFormatText (Formatter fmt, ReportParams reportParams) {
      super (reportParams);
      if (fmt == null)
         throw new NullPointerException();
      final int nd = reportParams.getNumDigits ();
      dfmtVal = new DefaultDoubleFormatter (nd, nd);
      dfmtStat = new DefaultDoubleFormatterWithError (nd);
      this.fmt = fmt;
   }

   /**
    * Returns the matrix formatter used by this object.
    *
    * @return the matrix formatter.
    */
   public Formatter getMatrixFormatter () {
      return fmt;
   }

   /**
    * Sets the matrix formatter used by this object to \texttt{fmt}.
    *
    * @param fmt
    *           the matrix formatter used.
    */
   public void setMatrixFormatter (Formatter fmt) {
      if (fmt == null)
         throw new NullPointerException ();
      this.fmt = fmt;
   }

   /**
    * Returns the double-precision formatter used for values.
    *
    * @return the double-precision formatter used for values.
    */
   public DoubleFormatter getDoubleFormatterValues () {
      return dfmtVal;
   }

   /**
    * Sets the double-precision formatter used for values to \texttt{dfmtVal}.
    *
    * @param dfmtVal
    *           the new double-precision formatter used for values.
    */
   public void setDoubleFormatterValues (DoubleFormatter dfmtVal) {
      if (dfmtVal == null)
         throw new NullPointerException ();
      this.dfmtVal = dfmtVal;
   }

   /**
    * Returns the double-precision formatter used for statistics.
    *
    * @return the double-precision formatter used for statistics.
    */
   public DoubleFormatter getDoubleFormatterStatistics () {
      return dfmtStat;
   }

   /**
    * Sets the double-precision formatter used for statistics to
    * \texttt{dfmtStat}.
    *
    * @param dfmtStat
    *           the double-precision formatter used for statistics.
    */
   public void setDoubleFormatterStatistics (DoubleFormatter dfmtStat) {
      if (dfmtStat == null)
         throw new NullPointerException ();
      this.dfmtStat = dfmtStat;
   }

   /**
    * Returns the string representing the
    * percentage sign in reports, the
    * default being \texttt{\%}.
    * @return the percentage string.
    */
   public String getPercentString () {
      return percentString;
   }

   /**
    * Sets the percentage string to
    * \texttt{percentString}.
    * @param percentString the new percentage string.
    */
   public void setPercentString (String percentString) {
      this.percentString = percentString;
   }

   private String formatValue (ContactCenterInfo eval, PerformanceMeasureType pm, double val) {
      if (Double.isNaN (val))
         return Messages.getString ("PerformanceMeasureFormat.NoValue"); //$NON-NLS-1$
      if (pm.isPercentage())
         return dfmtVal.format (val * 100).trim () + percentString;
      else if (pm.isTime() && eval.getDefaultUnit() != null)
         return dfmtVal.format (val).trim() + eval.getDefaultUnit().getShortName();
      else
         return dfmtVal.format (val).trim();
   }

   /**
    * Formats a report for all the performance measures \texttt{pms} supported
    * by the evaluation system \texttt{eval}. It uses the
    * {@link ContactCenterEval#getPerformanceMeasure} method to obtain a matrix
    * of values for each performance measure in \texttt{pms} supported by
    * \texttt{eval}. Considering the element at the bottom right of this matrix
    * as the aggregate value, the method then formats this value for each
    * performance measure, using {@link #getDoubleFormatterValues()} to convert
    * double-precision values to strings.
    *
    * The intermediate formatting matrix contains one row for each performance
    * measure, and a single column containing its aggregate value.
    *
    * @param eval
    *           the contact center evaluation system.
    * @param pms
    *           the array of performance measures.
    * @return the string containing the values of performance measures.
    */
   public String formatValuesSummary (ContactCenterEval eval,
         PerformanceMeasureType... pms) {
      final int nrows = countRowsSummary (eval, pms);
      if (nrows == 0)
         return "";
      final String[] rowNames = new String[nrows];
      final String[] columnNames = getValColumnNames ();
      final ObjectMatrix2D m = new DenseObjectMatrix2D (nrows,
            columnNames.length);
      int i = 0;
      for (final PerformanceMeasureType pm : pms) {
         if (!isIncludedInSummary (eval, pm))
            continue;
         rowNames[i] = pm.getDescription ();
         final DoubleMatrix2D avgm = eval.getPerformanceMeasure (pm);
         if (avgm.rows () == 0 || avgm.columns () == 0)
            m.set (i, 0, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
         else {
            final double avg = avgm.get (avgm.rows () - 1, avgm.columns () - 1);
            m.set (i, 0, formatValue (eval, pm, avg));
         }
         i++;
      }

      return fmt.toTitleString (m, rowNames, columnNames,
                  Messages.getString ("PerformanceMeasureFormat.Measures"), //$NON-NLS-1$
                  Messages.getString ("PerformanceMeasureFormat.Values"), //$NON-NLS-1$
                  Messages.getString ("PerformanceMeasureFormat.AggregateMeasures")); //$NON-NLS-1$
   }

   /**
    * Returns a string containing the current values of the performance measures
    * of type \texttt{pm} estimated by the evaluation system \texttt{eval}. This
    * method uses
    * {@link #formatValuesSingleRow(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,String)}
    * with a matrix of values obtained via
    * {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)
    * eval.getPerformanceMeasure}, and a description obtained via
    * {@link PerformanceMeasureType#getDescription() pm.getDescription()}.
    *
    * @param eval
    *           the contact center evaluation system.
    * @param pm
    *           the performance measure of interest.
    * @return the string containing the values of performance measure.
    */
   public String formatValuesDetailed (ContactCenterEval eval,
         PerformanceMeasureType pm) {
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int rows = pmm.rows ();
      final int columns = pmm.columns ();
      return formatValuesSingleRow (eval, pm, pmm, 0, 0, rows, columns, pm
            .getDescription ());
   }

   /**
    * Returns a string containing the current values of the performance measures
    * of type \texttt{pm} estimated by the evaluation system \texttt{eval}. This
    * method uses
    * {@link #formatValuesMatrix(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,boolean,String)}
    * with a matrix of values obtained via
    * {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)
    * eval.getPerformanceMeasure}, and a description obtained via
    * {@link PerformanceMeasureType#getDescription() pm.getDescription()}.
    *
    * @param eval
    *           the contact center evaluation system.
    * @param pm
    *           the performance measure of interest.
    * @return the string containing the values of performance measure.
    */
   public String formatValuesDetailedMatrix (ContactCenterEval eval,
         PerformanceMeasureType pm) {
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int rows = pmm.rows ();
      final int columns = pmm.columns ();
      return formatValuesMatrix (eval, pm, pmm, 0, 0, rows, columns, true, pm
            .getDescription ());
   }

   /**
    * Similar to
    * {@link #formatValuesDetailed(ContactCenterEval,PerformanceMeasureType)}
    * except per-period values are not displayed.
    *
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measure.
    * @return the formatted string.
    */
   public String formatValuesDetailedHidePeriods (ContactCenterEval eval,
         PerformanceMeasureType pm) {
      if (pm.getColumnType () != ColumnType.MAINPERIOD)
         return formatValuesDetailed (eval, pm);
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int row = 0;
      final int column = pmm.columns () - 1;
      final int height = pmm.rows ();
      final int width = 1;
      return formatValuesSingleRow (eval, pm, pmm, row, column, height, width,
            pm.getDescription ());
   }

   /**
    * Formats the values in a matrix 
    * \texttt{valm.}{@link DoubleMatrix2D#viewPart(int,int,int,int) viewPart}
    * \texttt{(row, column, height, width)} concerning performance measures of
    * type \texttt{pm} obtained with the evaluation system \texttt{eval}.
    * Numbers are formatted using {@link #getDoubleFormatterValues()}, and
    * \texttt{description} provides a description for the matrix.
    *
    * Suppose that the given matrix has dimensions $a\times b$. For example, the
    * matrix can contain averages or sample variances for different contact
    * types and periods. This method creates a $ab\times 1$ intermediate matrix
    * of strings with one row for each element of \texttt{valm}. The names of
    * rows are constructed using
    * {@link #getName(ContactCenterInfo,PerformanceMeasureType,int,int)
    * getName} \texttt{(eval, pm, i, j)}.
    *
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measures concerned.
    * @param valm
    *           the matrix of values.
    * @param row
    *           the starting row of the matrix to be formatted.
    * @param column
    *           the starting column of the matrix to be formatted.
    * @param height
    *           the height of the formatted matrix.
    * @param width
    *           the width of the formatted matrix.
    * @param description
    *           the description for the formatted matrix.
    * @return the formatted matrix.
    */
   public String formatValuesSingleRow (ContactCenterInfo eval,
         PerformanceMeasureType pm, DoubleMatrix2D valm, int row, int column,
         int height, int width, String description) {
      final DoubleMatrix2D valmPart = valm
            .viewPart (row, column, height, width);
      if (height == 0 || width == 0)
         return ""; //$NON-NLS-1$
      final ObjectMatrix2D m = new DenseObjectMatrix2D (height * width, 1);
      final String[] rowNames = new String[height * width];
      final String[] columnNames = getValColumnNames ();

      for (int i = 0; i < height; i++)
         for (int j = 0; j < width; j++) {
            final int v = i * width + j;
            String name;
            if (width == 1 && height > 1)
               //name = capitalizeFirstLetter (pm.rowName (eval, i));
               name = capitalizeFirstLetter (rowNameWithProperties (eval, pm, i));
            else if (width > 1 && height == 1)
               //name = capitalizeFirstLetter (pm.columnName (eval, j));
               name = capitalizeFirstLetter (columnNameWithProperties (eval, pm, j));
            else
               // name = getName (eval, pm, row + i, column + j);
               name = getNameWithProperties (eval, pm, row + i, column + j);
            rowNames[v] = name;
            final double val = valmPart.get (i, j);
            m.set (v, 0, formatValue (eval, pm, val));
         }

      return fmt.toTitleString (m, rowNames, columnNames, pm.rowTitle (), Messages
                  .getString ("PerformanceMeasureFormat.Values"), description); //$NON-NLS-1$
//      return formatWithProperties (eval, pm, m, rowNames, columnNames, pm.rowTitle (), Messages
//            .getString ("PerformanceMeasureFormat.Values"), description, true, false, true); //$NON-NLS-1$
   }

//   private String formatWithProperties
//   (ContactCenterEval eval, PerformanceMeasureType pm, ObjectMatrix2D m, String[] rowNames,
//         String[] columnNames, String rowTitle,
//         String columnTitle, String description,
//         boolean rowProperties,
//         boolean columnProperties,
//         boolean singleRow) {
//      ObjectMatrix2D mRes = m;
//      String[] rowNamesRes = rowNames;
//      String[] columnNamesRes = columnNames;
//      if (rowProperties) {
//         final int numProp = getNumPropRows (pm, singleRow);
//         if (numProp > 0) {
//            columnNamesRes = new String[columnNames.length + numProp];
//            System.arraycopy (columnNames, 0, columnNamesRes, numProp, columnNames.length);
//            String[] names = getPropNameRows (pm, singleRow);
//            System.arraycopy (names, 0, columnNamesRes, 0, names.length);
//            mRes = m.like (m.rows (), m.columns () + numProp);
//            mRes.viewPart (0, numProp, m.rows (), m.columns ()).assign (m);
//            for (int r = 0; r < mRes.rows (); r++) {
//               String[] values = getPropRows (eval, pm, r);
//               for (int c = 0; c < values.length; c++)
//                  mRes.set (r, c, values[c]);
//            }
//         }
//      }
//      if (columnProperties) {
//         final int numProp = getNumPropColumns (pm);
//         if (numProp > 0) {
//            rowNamesRes = new String[rowNames.length + numProp];
//            System.arraycopy (rowNames, 0, rowNamesRes, numProp, rowNames.length);
//            String[] names = getPropNameColumns (pm);
//            System.arraycopy (names, 0, rowNamesRes, 0, names.length);
//            ObjectMatrix2D mRes2 = mRes.like (mRes.rows () + numProp, mRes.columns ());
//            mRes2.viewPart (numProp, 0, mRes.rows (), mRes.columns ()).assign (mRes);
//            for (int c = 0; c < mRes2.columns (); c++) {
//               String[] values = getPropColumns (eval, pm, c);
//               for (int r = 0; r < values.length; r++)
//                  mRes2.set (r, c, values[r]);
//            }
//            mRes = mRes2;
//         }
//      }
//
//      return fmt.toTitleString (mRes, rowNamesRes, columnNamesRes, rowTitle, columnTitle, description);
//   }

   /**
    * This is similar to
    * {@link #formatValuesSingleRow(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,String)},
    * except that the intermediate matrix of strings has dimensions $a\times b$.
    * The formatted strings obtained via this method are often more readable
    * than with the preceding method, but they can contain excessively long
    * lines if the \texttt{width} is large.
    *
    * If \texttt{transposedValm} is \texttt{true}, the given matrix is
    * considered to be transposed, i.e., the meaning of its rows and columns is
    * inverted with respect to a typical matrix of performance measures of type
    * \texttt{pm}. For example, let \texttt{pm} correspond to
    * {@link PerformanceMeasureType#SERVEDRATES}. Usually, each row of
    * \texttt{valm} corresponds to a contact type. With \texttt{transposedValm}
    * enabled, each row of \texttt{valm} corresponds to an agent group. This
    * flag only affects how rows and columns are named; it does not change the
    * values being formatted.
    *
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measures concerned.
    * @param valm
    *           the matrix of values.
    * @param row
    *           the starting row of the matrix to be formatted.
    * @param column
    *           the starting column of the matrix to be formatted.
    * @param height
    *           the height of the formatted matrix.
    * @param width
    *           the width of the formatted matrix.
    * @param transposedValm
    *           determines if \texttt{valm} is transposed with respect to a
    *           typical matrix of performance measures of type \texttt{pm}.
    * @param description
    *           the description for the formatted matrix.
    * @return the formatted matrix.
    */
   public String formatValuesMatrix (ContactCenterInfo eval,
         PerformanceMeasureType pm, DoubleMatrix2D valm, int row, int column,
         int height, int width, boolean transposedValm, String description) {
      final DoubleMatrix2D valmPart = valm
            .viewPart (row, column, height, width);
      if (height == 0 || width == 0)
         return ""; //$NON-NLS-1$
      final ObjectMatrix2D m = new DenseObjectMatrix2D
      (height, width);

      final String[] rowNames = new String[m.rows ()];
      final String[] columnNames = new String[m.columns ()];

      for (int i = 0; i < height; i++) {
         if (transposedValm)
            //rowNames[i] = pm.columnName (eval, i + column);
            rowNames[i] = columnNameWithProperties (eval, pm, i + column);
         else
            rowNames[i] = rowNameWithProperties (eval, pm, row + i);
            //rowNames[i] = pm.rowName (eval, i + row);

         rowNames[i] = capitalizeFirstLetter (rowNames[i]);
         for (int j = 0; j < width; j++) {
            if (transposedValm)
               //columnNames[j] = pm.rowName (eval, j + row);
               columnNames[j] = rowNameWithProperties (eval, pm, row + j);
            else
               //columnNames[j] = pm.columnName (eval, j + column);
               columnNames[j] = columnNameWithProperties (eval, pm, j + column);
            columnNames[j] = capitalizeFirstLetter (columnNames[j]);
            final double val = valmPart.get (i, j);
            m.set (i, j, formatValue (eval, pm, val));
         }
      }
      final String rowTitle, columnTitle;
      if (transposedValm) {
         rowTitle = pm.columnTitle ();
         columnTitle = pm.rowTitle ();
      }
      else {
         rowTitle = pm.rowTitle ();
         columnTitle = pm.columnTitle ();
      }

      return fmt.toTitleString (m, rowNames, columnNames, rowTitle, columnTitle,
            description);
//      return formatWithProperties (eval, pm, m, rowNames, columnNames, rowTitle, columnTitle,
//            description, true, true, false); //$NON-NLS-1$
   }

   private String formatValue (ContactCenterInfo eval, PerformanceMeasureType pm, double val, double err, DoubleFormatterWithError dfmte) {
      if (Double.isNaN (val))
         return Messages.getString ("PerformanceMeasureFormat.NoValue"); //$NON-NLS-1$
      if (pm.isPercentage())
         return dfmte.format (val * 100, err * 100).trim () + percentString;
      else if (pm.isTime() && eval.getDefaultUnit() != null)
         return dfmte.format (val, err).trim() + eval.getDefaultUnit().getShortName();
      else
         return dfmte.format (val, err).trim();
   }

   private void formatStat (ContactCenterSim sim,
         PerformanceMeasureType pm,
         DoubleMatrix2D avgm, DoubleMatrix2D varm,
         DoubleMatrix2D minm, DoubleMatrix2D maxm, DoubleMatrix2D[] ci,
         ObjectMatrix2D out, int row, int col, int outRow,
         DoubleFormatterWithError dfmte) {
      final double avg = avgm.get (row, col);
      if (Double.isNaN (avg))
         for (int c = 0; c < out.columns (); c++)
            out.set (outRow, c, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
      else {
         double radius;
         if (ci != null) {
            final double lower = ci[0].get (row, col);
            final double upper = ci[1].get (row, col);
            radius = (upper - lower) / 2;
         }
         else if (varm != null)
            radius = Math.sqrt (varm.get (row, col));
         else
            radius = 0;

         out.set (outRow, 2, formatValue (sim, pm, avg, radius, dfmte));
         if (varm == null)
            out.set (outRow, 3, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
         else {
            final double stdDev = Math.sqrt (varm.get (row, col));
            out.set (outRow, 3, formatValue (sim, pm, stdDev, radius, dfmte));
         }
         if (minm == null)
            out.set (outRow, 0, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
         else
            out.set (outRow, 0, formatValue (sim, pm, minm.get (row, col), radius, dfmte));
         if (maxm == null)
            out.set (outRow, 1, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
         else
            out.set (outRow, 1, formatValue (sim, pm, maxm.get(row, col), radius, dfmte));
         if (ci == null)
            out.set (outRow, 4, Messages
                  .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
         else {
            final double lower = ci[0].get (row, col);
            final double upper = ci[1].get (row, col);
            final StringBuilder sbCI = new StringBuilder();
            sbCI.append ('[');
            sbCI.append (formatValue (sim, pm, lower, radius, dfmte));
            sbCI.append (", ");
            sbCI.append (formatValue (sim, pm, upper, radius, dfmte));
            sbCI.append (']');
            out.set (outRow, 4, sbCI.toString ());
         }
      }
   }

   /**
    * Formats a statistical report for all the performance measures in
    * \texttt{pms} supported by the contact center simulator \texttt{sim}. This
    * is similar to {@link #formatValuesSummary}, with additional statistical
    * information such as miminum, maximum, standard deviation, and confidence
    * intervals with confidence level \texttt{level}, if available. Values are
    * formatted using the formatter {@link #getDoubleFormatterStatistics()}.
    *
    * The format of the intermediate matrix is the same as
    * {@link #formatValuesSummary}, except that columns are defined for the
    * minimum, maximum, average, standard deviation, and confidence interval.
    *
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the level of confidence of the intervals
    * @param pms
    *           the array of performance measures.
    * @return the statistics formatted as a string.
    */
   public String formatStatisticsSummary (ContactCenterSim sim, double level,
         PerformanceMeasureType... pms) {
      final DoubleFormatterWithError dfmte;
      if (dfmtStat instanceof DoubleFormatterWithError)
         dfmte = (DoubleFormatterWithError) dfmtStat;
      else
         dfmte = new DoubleFormatterIgnoreError (dfmtStat);
      final int nrows = countRowsSummary (sim, pms);
      if (nrows == 0)
         return "";
      final String[] rowNames = new String[nrows];
      final ObjectMatrix2D m = new DenseObjectMatrix2D (rowNames.length,
            getStatColumnNames ().length);
      DoubleMatrix2D avgm = null;
      DoubleMatrix2D varm = null;
      DoubleMatrix2D minm = null;
      DoubleMatrix2D maxm = null;
      DoubleMatrix2D[] ci = null;
      int i = 0;
      for (final PerformanceMeasureType pm : pms) {
         if (!isIncludedInSummary (sim, pm))
            continue;
         rowNames[i] = pm.getDescription ();
         avgm = sim.getPerformanceMeasure (pm);
         if (avgm.rows () == 0 || avgm.columns () == 0) {
            for (int c = 0; c < m.columns (); c++)
               m.set (i, c, Messages
                     .getString ("PerformanceMeasureFormat.NoValue")); //$NON-NLS-1$
            i++;
            continue;
         }

         try {
            varm = sim.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {
            varm = null;
         }
         try {
            minm = sim.getMin (pm);
         }
         catch (final NoSuchElementException nse) {
            minm = null;
         }
         try {
            maxm = sim.getMax (pm);
         }
         catch (final NoSuchElementException nse) {
            maxm = null;
         }
         try {
            ci = sim.getConfidenceInterval (pm, level);
         }
         catch (final NoSuchElementException nse) {
            ci = null;
         }
         formatStat (sim, pm, avgm, varm, minm, maxm, ci, m, avgm.rows () - 1, avgm
               .columns () - 1, i, dfmte);
         i++;
      }

      return fmt
            .toTitleString (
                  m,
                  rowNames,
                  getStatColumnNames (),
                  Messages.getString ("PerformanceMeasureFormat.Measures"), //$NON-NLS-1$
                  Messages.getString ("PerformanceMeasureFormat.Values"),   //$NON-NLS-1$
                  Messages.getString ("PerformanceMeasureFormat.AggregateMeasures")); //$NON-NLS-1$ 
   }

   /**
    * Returns a statistical report for all the values of the performance measure
    * \texttt{pm} estimated by the simulator \texttt{sim}, with confidence
    * intervals with level \texttt{level}. Values are formatted using
    * {@link #getDoubleFormatterStatistics()}.
    *
    * The intermediate matrix has a format similar to the one constructed by
    * {@link #formatValuesDetailed(ContactCenterEval,PerformanceMeasureType)},
    * except that a column is defined for the minimum, maximum, average,
    * standard deviation, and confidence interval.
    *
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the level of confidence intervals.
    * @param pm
    *           the performance measure of interest.
    * @return the statistical report formatted as a string.
    */
   public String formatStatisticsDetailed (ContactCenterSim sim, double level,
         PerformanceMeasureType pm) {
      final DoubleMatrix2D avgm = sim.getPerformanceMeasure (pm);
      if (avgm.rows () == 0 || avgm.columns () == 0)
         return ""; //$NON-NLS-1$
      final DoubleFormatterWithError dfmte;
      if (dfmtStat instanceof DoubleFormatterWithError)
         dfmte = (DoubleFormatterWithError) dfmtStat;
      else
         dfmte = new DoubleFormatterIgnoreError (dfmtStat);
      DoubleMatrix2D varm = null;
      DoubleMatrix2D minm = null;
      DoubleMatrix2D maxm = null;
      DoubleMatrix2D[] ci = null;
      final ObjectMatrix2D m = new DenseObjectMatrix2D (avgm.rows ()
            * avgm.columns (), getStatColumnNames ().length);
      final String[] rowNames = new String[m.rows ()];

      try {
         varm = sim.getVariance (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         minm = sim.getMin (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         maxm = sim.getMax (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         ci = sim.getConfidenceInterval (pm, level);
      }
      catch (final NoSuchElementException nse) {}

      for (int i = 0; i < avgm.rows (); i++)
         for (int j = 0; j < avgm.columns (); j++) {
            final int v = i * avgm.columns () + j;
            rowNames[v] = getName (sim, pm, i, j);
            formatStat (sim, pm, avgm, varm, minm, maxm, ci, m, i, j, v, dfmte);
         }

      return fmt.toTitleString (m,
                  rowNames,
                  getStatColumnNames (),
                  pm.rowTitle (),
                  Messages.getString ("PerformanceMeasureFormat.Values"), pm.getDescription ()); //$NON-NLS-1$
//      return formatWithProperties (sim, pm, m,
//            rowNames,
//            getStatColumnNames (),
//            pm.rowTitle (),
//            Messages.getString ("PerformanceMeasureFormat.Values"), pm.getDescription (), true, false, true); //$NON-NLS-1$
   }

   /**
    * Similar to
    * {@link #formatStatisticsDetailed(ContactCenterSim,double,PerformanceMeasureType)}
    * but does not format per-period statistics.
    *
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the confidence level of the intervals.
    * @param pm
    *           the type of performance measures.
    * @return the formatted string.
    */
   public String formatStatisticsDetailedHidePeriods (ContactCenterSim sim,
         double level, PerformanceMeasureType pm) {
      if (pm.getColumnType () != ColumnType.MAINPERIOD)
         return formatStatisticsDetailed (sim, level, pm);
      final DoubleMatrix2D avgm = sim.getPerformanceMeasure (pm);
      if (avgm.rows () == 0 || avgm.columns () == 0)
         return ""; //$NON-NLS-1$
      final DoubleFormatterWithError dfmte;
      if (dfmtStat instanceof DoubleFormatterWithError)
         dfmte = (DoubleFormatterWithError) dfmtStat;
      else
         dfmte = new DoubleFormatterIgnoreError (dfmtStat);
      DoubleMatrix2D varm = null;
      DoubleMatrix2D minm = null;
      DoubleMatrix2D maxm = null;
      DoubleMatrix2D[] ci = null;
      final ObjectMatrix2D m = new DenseObjectMatrix2D (avgm.rows (),
            getStatColumnNames ().length);
      final String[] rowNames = new String[m.rows ()];

      try {
         varm = sim.getVariance (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         minm = sim.getMin (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         maxm = sim.getMax (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         ci = sim.getConfidenceInterval (pm, level);
      }
      catch (final NoSuchElementException nse) {}

      final int j = avgm.columns () - 1;
      for (int i = 0; i < avgm.rows (); i++) {
         //rowNames[i] = capitalizeFirstLetter (pm.rowName (sim, i));
         rowNames[i] = capitalizeFirstLetter (rowNameWithProperties (sim, pm, i));
         formatStat (sim, pm, avgm, varm, minm, maxm, ci, m, i, j, i, dfmte);
      }

      return fmt.toTitleString (m,
                  rowNames,
                  getStatColumnNames (),
                  pm.rowTitle (),
                  Messages.getString ("PerformanceMeasureFormat.Values"), pm.getDescription ()); //$NON-NLS-1$
//      return formatWithProperties (sim, pm, m,
//            rowNames,
//            getStatColumnNames (),
//            pm.rowTitle (),
//            Messages.getString ("PerformanceMeasureFormat.Values"), pm.getDescription (), true, false, false); //$NON-NLS-1$
   }

   private final class DoubleFormatterIgnoreError implements
         DoubleFormatterWithError {
      private final DoubleFormatter dfmt;

      public DoubleFormatterIgnoreError (DoubleFormatter dfmt) {
         this.dfmt = dfmt;
      }

      public String format (double x, double error) {
         return dfmt.format (x);
      }

      public String format (double x) {
         return dfmt.format (x);
      }
   }

   /**
    * Constructs and returns a string containing the evaluation information
    * \texttt{info}. For each entry in the given map, this method formats a line
    * of the form \texttt{key: value}. Values are formatted as follows. Any
    * \texttt{null} reference becomes the string \texttt{null}, instances of
    * {@link  Number} are formatted using {@link NumberFormat}, and instances
    * of {@link Date} are formatted using {@link DateFormat}. Any other
    * non-\texttt{null} value is formatted using the {@link #toString()} method.
    *
    * @param info
    *           the evaluation information.
    * @return the string with the formatted information.
    */
   public String formatInfo (Map<String, Object> info) {
      final int count = info.size ();
      final NumberFormat nf = NumberFormat.getInstance ();
      final DateFormat df = DateFormat.getDateTimeInstance ();
      final ObjectMatrix2D mat = new DenseObjectMatrix2D (count, 2);
      int row = 0;
      for (final Map.Entry<String, Object> e : info.entrySet ()) {
         final String key = e.getKey ();
         final Object value = e.getValue ();
         mat.setQuick (row, 0, key);
         mat.setQuick (row, 1, format (nf, df, value));
         ++row;
      }
      fmt.setPrintShape (false);
      try {
         return fmt.toString (mat);
      }
      finally {
         fmt.setPrintShape (true);
      }
   }

   private String format (NumberFormat nf, DateFormat df, Object value) {
      if (value == null)
         return "null";
      else if (value instanceof Number)
         return dfmtVal.format (((Number)value).doubleValue ());
      else if (value instanceof Time)
         return AbstractChrono.format (((Date)value).getTime ()/1000.0);
      else if (value instanceof Duration)
         return AbstractChrono.format (((Duration)value).getTimeInMillis (new Date())/1000.0);
      else if (value instanceof Date)
         return df.format ((Date) value);
      else if (value.getClass ().isArray ()) {
         final StringBuilder sb = new StringBuilder();
         sb.append ('[');
         final int length = Array.getLength (value);
         for (int i = 0; i < length; i++) {
            if (i > 0)
               sb.append (", ");
            final Object e = Array.get (value, i);
            sb.append (format (nf, df, e));
         }
         sb.append (']');
         return sb.toString ();
      }
      else
         return value.toString ();
   }

   /**
    * For each element {@link PerformanceMeasureParams} in the list returned
    * by {@link ReportParams#getPrintedObs()},
    * formats a report containing the complete list of observations
    * generated by the simulator \texttt{sim}
    * for the referred performance measure.
    * Each measure-specific report is concatenated, and
    * the resulting string is returned.
    * @param sim the queried simulator.
    * @param reportParams the report parameters.
    * @return the string containing observations.
    */
   public String formatObservations (ContactCenterSimWithObservations sim, ReportParams reportParams) {
      final StringBuilder sbglo = new StringBuilder();
      for (final PerformanceMeasureParams ppar : reportParams.getPrintedObs ()) {
         final StringBuilder sb = new StringBuilder();
         final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ppar.getMeasure ());
         sbglo.append ('\n').append (Messages.getString ("PerformanceMeasureFormat.ObservationsFor")).append (' ');
         sb.append (pm.getDescription ());
         int row, column;
         if (ppar.isSetRow () || ppar.isSetColumn ())
            sb.append (" (");
         if (ppar.isSetRow ()) {
            row = ppar.getRow ();
            if (row < 0)
               row += pm.rows (sim);
            sb.append (pm.rowName (sim, row));
         }
         else
            row = pm.rows (sim) - 1;
         if (ppar.isSetColumn ()) {
            if (ppar.isSetRow ())
               sb.append (", ");
            column = ppar.getColumn ();
            if (column < 0)
               column += pm.columns (sim);
            sb.append (pm.columnName (sim, column));
         }
         else
            column = pm.columns (sim) - 1;
         if (ppar.isSetRow () || ppar.isSetColumn ())
            sb.append (')');
         sbglo.append(sb.toString ());
         sbglo.append ('\n');
         double[] obs;
         try {
            obs = sim.getObs (pm, row, column);
          	if (ppar.isSetHistogram()) {
               double std = getStandardDeviation (sim, pm, row, column);
               createHistogram(obs, std, ppar, sb.toString ());
          	}
         }
         catch (final NoSuchElementException nse) {
            sbglo.append (Messages.getString ("PerformanceMeasureFormat.NoObs"));
            sbglo.append('\n');
            continue;
         }
         for (double element : obs) {
            sbglo.append (formatValue (sim, pm, element));
            sbglo.append ('\n');
         }
      }
      return sbglo.toString ();
   }

   /**
    * Formats and returns a string containing the report of the last evaluation
    * performed by the system \texttt{eval}. This method can be called by the
    * implementation of {@link ContactCenterEval#formatStatistics()}.
    *
    * This method first calls {@link #formatInfo(Map)} with the evaluation
    * information of \texttt{eval}. It then formats a summary report using
    * {@link #formatValuesSummary(ContactCenterEval,PerformanceMeasureType...)}.
    * Then, for each performance measure a detailed report is requested for, the
    * method appends the contents of
    * {@link #formatStatisticsDetailed(ContactCenterSim,double,PerformanceMeasureType)}
    * or
    * {@link #formatValuesDetailedHidePeriods(ContactCenterEval,PerformanceMeasureType)}
    * to the report.
    * The types of performance measures to include in the report are selected
    * using \texttt{reportParams.}{@link ReportParams#getPrintedStats() getPrintedStats()}
    *  which can be \texttt{null} or empty; in these
    * two latter cases, the report includes all performance measures supported
    * by \texttt{eval}. Each element of \texttt{printedStats} specifies a type
    * of performance measure to include in the report (if supported by
    * \texttt{eval}), whether a detailed report must be included, and if this
    * detailed report includes information about each individual period.
    *
    * @param eval
    *           the evaluation system.
    * @param reportParams the report parameters.
    * @return the string containing the formatted report.
    */
   public String formatValues (ContactCenterEval eval,
         ReportParams reportParams) {
      final PrintedStatParams[] printedStats = reportParams == null ? null : reportParams.getPrintedStats ().toArray (new PrintedStatParams[0]);
      PrintedStatParams[] pstats;
      if (printedStats == null || printedStats.length == 0)
         pstats = getDefaultPrintedStatParams (eval, reportParams);
      else
         pstats = printedStats;
      final PerformanceMeasureType[] pms = getPerformanceMeasures (pstats);
      final StringBuilder sb = new StringBuilder ();
      if (!eval.getEvalInfo ().isEmpty ()) {
         sb.append (formatInfo (eval.getEvalInfo ()));
         sb.append ("\n\n");
      }
      sb.append (formatValuesSummary (eval, pms));
      for (final PrintedStatParams ps : pstats) {
         boolean psDetailed = ps.isSetDetailed () ? ps.isDetailed () : reportParams.isDefaultDetailed ();
         if (!psDetailed)
            continue;
         final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
         // If the performance matrix is 1x1, it is not worth displaying it
         // since the information is all contained in the summary report.
         if (!isIncludedInReport (eval, pm))
            continue;
         boolean psPeriods = ps.isSetPeriods () ? ps.isPeriods () : reportParams.isDefaultPeriods ();
         if (isIncludedInSummary (eval, pm)) {
            if (pm.rows (eval) == 1
                  && pm.columns (eval) == 1)
               continue;
            if (!psPeriods
                  && pm.getColumnType () == ColumnType.MAINPERIOD
                  && pm.rows (eval) == 1)
               continue;
         }
         sb.append ("\n\n"); //$NON-NLS-1$
         if (psPeriods)
            sb.append (formatValuesDetailed (eval, pm));
         else
            sb.append (formatValuesDetailedHidePeriods (eval, pm));
      }
      return sb.toString ();
   }

   /**
    * Similar to {@link #formatValues(ContactCenterEval,ReportParams)},
    * except this formats a full statistical report using
    * {@link #formatStatisticsSummary(ContactCenterSim,double,PerformanceMeasureType[])},
    * and
    * {@link #formatStatisticsDetailed(ContactCenterSim,double,PerformanceMeasureType)}.
    * If the given report parameters contains information
    * about observations to print,
    * the method also calls
    * {@link #formatObservations(ContactCenterSimWithObservations,ReportParams)},
    * and appends the result to the returned string.
    *
    * @param sim
    *           the contact center simulator.
    * @param reportParams the report parameters.
    * @return the formatted report.
    */
   public String formatStatistics (ContactCenterSim sim,
         ReportParams reportParams) {
      final double level = reportParams == null ? sim.getConfidenceLevel () : reportParams.getConfidenceLevel ();
      final PrintedStatParams[] printedStats = reportParams == null ? null : reportParams.getPrintedStats ().toArray (new PrintedStatParams[0]);
      PrintedStatParams[] pstats;
      if (printedStats == null || printedStats.length == 0)
         pstats = getDefaultPrintedStatParams (sim, reportParams);
      else
         pstats = printedStats;
      final StringBuilder sb = new StringBuilder ();
      if (!sim.getEvalInfo ().isEmpty ()) {
         sb.append (formatInfo (sim.getEvalInfo ()));
         sb.append ("\n\n");
      }
      final PerformanceMeasureType[] pmsVal = getPerformanceMeasures (pstats,
            true, RowType.values ());
      if (pmsVal.length > 0)
         sb.append (formatValuesSummary (sim, pmsVal));
      final PerformanceMeasureType[] pmsStat = getPerformanceMeasures (pstats,
            false, RowType.values ());
      if (pmsStat.length > 0) {
         if (pmsVal.length > 0)
            sb.append ("\n\n");
         sb.append (formatStatisticsSummary (sim, level, pmsStat));
      }
      for (final PrintedStatParams ps : pstats) {
         boolean psDetailed = ps.isSetDetailed () ? ps.isDetailed () : reportParams.isDefaultDetailed ();
         if (!psDetailed)
            continue;
         final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
         // If the performance matrix is 1x1, it is not worth displaying it
         // since the information is all contained in the summary report.
         if (!isIncludedInReport (sim, pm))
            continue;
         final boolean psPeriods = ps.isSetPeriods () ? ps.isPeriods () : reportParams.isDefaultPeriods ();
         if (isIncludedInSummary (sim, pm)) {
            if (pm.rows (sim) == 1 && pm.columns (sim) == 1)
               continue;
            if (!psPeriods
                  && pm.getColumnType () == ColumnType.MAINPERIOD
                  && pm.rows (sim) == 1)
               continue;
         }
         sb.append ("\n\n"); //$NON-NLS-1$
         final boolean psOnlyAverages = ps.isSetOnlyAverages () ? ps.isOnlyAverages () : reportParams.isDefaultOnlyAverages ();
         if (psOnlyAverages) {
            if (psPeriods)
               sb.append (formatValuesDetailed (sim, pm));
            else
               sb.append (formatValuesDetailedHidePeriods (sim, pm));
         }
         else if (psPeriods)
            sb.append (formatStatisticsDetailed (sim, level, pm));
         else
            sb.append (formatStatisticsDetailedHidePeriods (sim, level, pm));
      }
      if (sim instanceof ContactCenterSimWithObservations && reportParams.isSetPrintedObs ()) {
         sb.append ("\n\n").append (formatObservations ((ContactCenterSimWithObservations)sim,
               reportParams));
      //   formatHistograms ((ContactCenterSimWithObservations)sim, reportParams);
      }
      return sb.toString ();
   }
}
