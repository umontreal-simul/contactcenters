package umontreal.iro.lecuyer.contactcenters.app;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;

import umontreal.iro.lecuyer.util.DefaultDoubleFormatterWithError;
import umontreal.iro.lecuyer.util.DoubleFormatterWithError;

/**
 * Provides static methods that can be used to compare
 * simulation results.
 */
public class CompareSimResults {
   /**
    * Returns a set containing the performance measure types
    * supported by both \texttt{res1} and \texttt{res2}, and
    * providing matrices of results of the same dimensions.
    * For performance measures supported by one result set only,
    * this method can output a message on
    * the formatter \texttt{fmt}.  
    * @param res1 the first result set.
    * @param res2 the second result set.
    * @param fmt the formatter to output messages, or \texttt{null}.
    * @return the set of performance measures.
    */
   public static Set<PerformanceMeasureType> getCommonPerformanceMeasures (ContactCenterEval res1,
         ContactCenterEval res2, Formatter fmt) {
      final Set<PerformanceMeasureType> pmSet = EnumSet.noneOf (PerformanceMeasureType.class);
      for (final PerformanceMeasureType pm : res1.getPerformanceMeasures ()) {
         if (!res2.hasPerformanceMeasure (pm)) {
            if (fmt != null)
               fmt.format ("First system has performance measure %s while second system does not\n",
                     pm.name());
            continue;
         }
         final int rows1 = pm.rows (res1);
         final int rows2 = pm.rows (res2);
         if (rows1 != rows2) {
            if (fmt != null)
               fmt.format ("For %s, the first system gives matrices of results containing %d rows " +
                     "while the second system produces matrices with %d rows\n",
                     pm.name(), rows1, rows2);
            continue;
         }
         final int cols1 = pm.columns (res1);
         final int cols2 = pm.columns (res2);
         if (cols1 != cols2) {
            if (fmt != null)
               fmt.format ("For %s, the first system gives matrices of results containing %d columns " +
                     "while the second system produces matrices with %d columns\n",
                     pm.name(), cols1, cols2);
            continue;
         }
         for (int r = 0; r < rows1; r++) {
            if (!pm.rowName (res1, r).equals (pm.rowName (res2, r))) {
               fmt.format ("Row with index %d has name %s in system 1 but name %s in system 2%n",
                     r, pm.rowName (res1, r), pm.rowName (res2, r));
               continue;
            }
         }
         for (int c = 0; c < cols1; c++) {
            if (!pm.columnName (res1, c).equals (pm.columnName (res2, c))) {
               fmt.format ("Column with index %d has name %s in system 1 but name %s in system 2%n",
                     c, pm.columnName (res1, c), pm.columnName (res2, c));
               continue;
            }
         }
         pmSet.add (pm);
      }
      for (final PerformanceMeasureType pm : res2.getPerformanceMeasures ())
         if (!res1.hasPerformanceMeasure (pm)) {
            if (fmt != null)
               fmt.format ("First system does not have performance measure %s while second system does\n",
                     pm.name());
            continue;
         }
      return pmSet;
   }
   
   /**
    * Compares \texttt{res1} and \texttt{res2} based on
    * the performance measures in \texttt{pmSet}, and
    * adds a point $(r, c)$ for each performance measure
    * whose estimated value differs more than \texttt{tol}.
    * This method uses {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)}
    * to get a matrix of point estimates for each performance measure type
    * in \texttt{pmSet}, for \texttt{res1}, and \texttt{res2}.
    * Then, assuming that both matrices of estimates share the same
    * dimensions, the method compares all corresponding
    * elements $(r, c)$ in the matrices.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param pmSet the set of tested performance measures.
    * @return the map giving the list of differing points for
    * each performance measure.
    */
   public static Map<PerformanceMeasureType, List<Point>> getDifferent
   (ContactCenterEval res1, ContactCenterEval res2, Set<PerformanceMeasureType> pmSet) {
      final Map<PerformanceMeasureType, List<Point>> diffMap = new EnumMap<PerformanceMeasureType, List<Point>> (PerformanceMeasureType.class);
      for (final PerformanceMeasureType pm : pmSet) {
         final DoubleMatrix2D avg1 = res1.getPerformanceMeasure (pm);
         final DoubleMatrix2D avg2 = res2.getPerformanceMeasure (pm);
         final int nr = avg1.rows();
         final int nc = avg1.columns();
         if (nr != avg1.rows() || nc != avg2.columns())
            continue;
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++) {
               final double v1 = avg1.getQuick (r, c);
               final double v2 = avg2.getQuick (r, c);
               if (v1 != v2) {
                  List<Point> el = diffMap.get (pm);
                  if (el == null) {
                     el = new ArrayList<Point> ();
                     diffMap.put (pm, el);
                  }
                  el.add (new Point (r, c));
               }
            }         
      }      
      return diffMap;
   }
   
   /**
    * Equivalent to {@link #getDifferent(ContactCenterEval,ContactCenterEval,Set)},
    * except that
    * two real numbers $v_1$ and $v_2$ are considered different
    * if $|v_2 - v_1|>\epsilon$, where $\epsilon=$~\texttt{tol}.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param tol the tolerance.
    * @param pmSet the set of tested performance measures.
    * @return the map giving the list of differing points for
    * each performance measure.
    */
   public static Map<PerformanceMeasureType, List<Point>> getDifferent
   (ContactCenterEval res1, ContactCenterEval res2, double tol, Set<PerformanceMeasureType> pmSet) {
      final Map<PerformanceMeasureType, List<Point>> diffMap = new EnumMap<PerformanceMeasureType, List<Point>> (PerformanceMeasureType.class);
      for (final PerformanceMeasureType pm : pmSet) {
         final DoubleMatrix2D avg1 = res1.getPerformanceMeasure (pm);
         final DoubleMatrix2D avg2 = res2.getPerformanceMeasure (pm);
         final int nr = avg1.rows();
         final int nc = avg1.columns();
         if (nr != avg1.rows() || nc != avg2.columns())
            continue;
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++) {
               final double v1 = avg1.getQuick (r, c);
               final double v2 = avg2.getQuick (r, c);
               if (Math.abs (v2 - v1) > tol) {
                  List<Point> el = diffMap.get (pm);
                  if (el == null) {
                     el = new ArrayList<Point> ();
                     diffMap.put (pm, el);
                  }
                  el.add (new Point (r, c));
               }
            }         
      }      
      return diffMap;
   }
   
   /**
    * Compares \texttt{res1} and \texttt{res2} based on
    * the performance measures in \texttt{pmSet}, and
    * adds a point $(r, c)$ for each performance measure
    * whose confidence intervals with confidence level
    * \texttt{confidenceLevel}, for
    * both system, 
    * do not overlap.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param confidenceLevel the confidence level.
    * @param pmSet the set of tested performance measures.
    * @return the map giving the list of differing points for
    * each performance measure.
    */
   public static Map<PerformanceMeasureType, List<Point>> getNonOverlappingCI
   (ContactCenterSim res1, ContactCenterSim res2, double confidenceLevel, double tol, Set<PerformanceMeasureType> pmSet) {
      final Map<PerformanceMeasureType, List<Point>> nonOverlapMap = new EnumMap<PerformanceMeasureType, List<Point>> (PerformanceMeasureType.class);
      for (final PerformanceMeasureType pm : pmSet) {
         DoubleMatrix2D[] ci1 = null;
         try {
            ci1 = res1.getConfidenceInterval (pm, confidenceLevel);
         }
         catch (final NoSuchElementException nse) {}
         DoubleMatrix2D[] ci2 = null;
         try {
            ci2 = res2.getConfidenceInterval (pm, confidenceLevel);
         }
         catch (final NoSuchElementException nse) {}
         if (ci1 == null || ci2 == null)
            continue;
         assert ci1.length == 2 && ci2.length == 2;
         if (ci1[0].rows () != ci2[0].rows ())
            continue;
         if (ci1[0].columns () != ci2[0].columns ())
            continue;
         final int nr = ci1[0].rows ();
         final int nc = ci1[0].columns ();
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
               if (ci1[1].get (r, c) + tol < ci2[0].get (r, c)
                     || ci2[1].get (r, c) + tol < ci1[0].get (r, c)) {
                  List<Point> el = nonOverlapMap.get (pm);
                  if (el == null) {
                     el = new ArrayList<Point> ();
                     nonOverlapMap.put (pm, el);
                  }
                  el.add (new Point (r, c));
               }
      }
      return nonOverlapMap;
   }
   
   /**
    * Formats the values of the performance measures for
    * each differing point given by \texttt{diffMap}.
    * For each key in \texttt{diffMap}, this method
    * obtains a list of points $(r, c)$ corresponding
    * to performance measures.
    * For each such point, the method formats
    * the average for both systems.  
    * @param res1 the first system.
    * @param res2 the second system.
    * @param diffMap the map containing differing performance measures.
    * @return the string containing the results.
    */
   public static String formatDifferentPoints (ContactCenterEval res1, ContactCenterEval res2,
         Map<PerformanceMeasureType, List<Point>> diffMap) {
      final DoubleFormatterWithError dfmt = new DefaultDoubleFormatterWithError ();
      final cern.colt.matrix.objectalgo.Formatter fmtMat = new cern.colt.matrix.objectalgo.Formatter ();
      final String[] columnNames = new String[] { "Avg1",
            "Avg2" };
      final StringBuilder sb = new StringBuilder();
      for (final Map.Entry<PerformanceMeasureType, List<Point>> e : diffMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final List<Point> el = e.getValue ();
         final DoubleMatrix2D avgm1 = res1.getPerformanceMeasure (pm);
         final DoubleMatrix2D avgm2 = res2.getPerformanceMeasure (pm);
         final ObjectMatrix2D m = new DenseObjectMatrix2D (el.size (),
               columnNames.length);
         final String[] rowNames = new String[m.rows ()];

         int v = 0;
         for (final Point pt : el) {
            final int i = (int) pt.getX ();
            final int j = (int) pt.getY ();
            String rowName = pm.rowName (res1, i);
            rowName = rowName.substring (0, 1).toUpperCase ()
                  + rowName.substring (1);
            if (avgm1.columns () > 1) {
               final String colName = pm.columnName (res1, j);
               if (colName.length () > 0)
                  rowName += ", " + colName;
            }
            rowNames[v] = rowName;

            final double avg1 = avgm1.get (i, j);
            final double avg2 = avgm2.get (i, j);
            final double error = Double.isNaN (avg1) || Double.isNaN (avg2) ? 0 : avg2 - avg1;
            if (Double.isNaN (avg1))
               m.set (v, 0, "---");
            else
               m.set (v, 0, dfmt.format (avg1, error).trim ());

            if (Double.isNaN (avg2))
               m.set (v, 1, "---");
            else
               m.set (v, 1, dfmt.format (avg2, error));
            ++v;
         }

         if (sb.length() > 0)
            sb.append ("\n\n");
         sb.append (fmtMat.toTitleString (m, rowNames, columnNames, pm
               .rowTitle (), "Values", pm.getDescription ()));
      }
      return sb.toString();
   }
   
   /**
    * Sends the values of the performance measures for
    * each differing point given by \texttt{diffMap}.
    * For each key in \texttt{diffMap}, this method
    * obtains a list of points $(r, c)$ corresponding
    * to performance measures.
    * For each such point, the method formats
    * the average, standard deviation, and
    * confidence interval, for both systems.  
    * @param res1 the first system.
    * @param res2 the second system.
    * @param diffMap the map containing differing performance measures.
    * @param confidenceLevel the level of confidence of the intervals.
    * @return the string containing the results.
    */
   public static String formatDifferentPoints (ContactCenterSim res1, ContactCenterSim res2,
         Map<PerformanceMeasureType, List<Point>> diffMap, double confidenceLevel) {
      final DoubleFormatterWithError dfmt = new DefaultDoubleFormatterWithError ();
      final cern.colt.matrix.objectalgo.Formatter fmtMat = new cern.colt.matrix.objectalgo.Formatter ();
      final String[] columnNames = new String[] { "Avg1", "StdDev1",
            "ConfInt1", "Avg2", "StdDev2", "ConfInt2" };
      final StringBuilder sb = new StringBuilder();
      for (final Map.Entry<PerformanceMeasureType, List<Point>> e : diffMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final List<Point> el = e.getValue ();
         final DoubleMatrix2D avgm1 = res1.getPerformanceMeasure (pm);
         final DoubleMatrix2D avgm2 = res2.getPerformanceMeasure (pm);
         DoubleMatrix2D varm1 = null;
         DoubleMatrix2D varm2 = null;
         DoubleMatrix2D[] ci1 = null;
         DoubleMatrix2D[] ci2 = null;
         final ObjectMatrix2D m = new DenseObjectMatrix2D (el.size (),
               columnNames.length);
         final String[] rowNames = new String[m.rows ()];

         try {
            varm1 = res1.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            varm2 = res2.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            ci1 = res1.getConfidenceInterval (pm, confidenceLevel);
         }
         catch (final NoSuchElementException nse) {}
         try {
            ci2 = res2.getConfidenceInterval (pm, confidenceLevel);
         }
         catch (final NoSuchElementException nse) {}

         int v = 0;
         for (final Point pt : el) {
            final int i = (int) pt.getX ();
            final int j = (int) pt.getY ();
            String rowName = pm.rowName (res1, i);
            rowName = rowName.substring (0, 1).toUpperCase ()
                  + rowName.substring (1);
            if (avgm1.columns () > 1) {
               final String colName = pm.columnName (res1, j);
               if (colName.length () > 0)
                  rowName += ", " + colName;
            }
            rowNames[v] = rowName;

            double radius1;
            if (ci1 != null) {
               final double lower = ci1[0].get (i, j);
               final double upper = ci1[1].get (i, j);
               radius1 = (upper - lower) / 2;
            }
            else if (varm1 != null)
               radius1 = Math.sqrt (varm1.get (i, j));
            else
               radius1 = 0;

            double radius2;
            if (ci2 != null) {
               final double lower = ci2[0].get (i, j);
               final double upper = ci2[1].get (i, j);
               radius2 = (upper - lower) / 2;
            }
            else if (varm2 != null)
               radius2 = Math.sqrt (varm2.get (i, j));
            else
               radius2 = 0;

            final double avg1 = avgm1.get (i, j);
            final double avg2 = avgm2.get (i, j);
            if (Double.isNaN (avg1))
               for (int c = 0; c < 3; c++)
                  m.set (v, c, "---");
            else {
               m.set (v, 0, dfmt.format (avg1, radius1).trim ());
               if (varm1 == null)
                  m.set (v, 1, "---");
               else
                  m.set (v, 1, dfmt.format (Math.sqrt (varm1.get (i, j)),
                        radius1));
               if (ci1 == null)
                  m.set (v, 2, "---");
               else {
                  final double lower = ci1[0].get (i, j);
                  final double upper = ci1[1].get (i, j);
                  m.set (v, 2, "[" + dfmt.format (lower, radius1) + ", "
                        + dfmt.format (upper, radius1) + "]");
               }
            }

            if (Double.isNaN (avg2))
               for (int c = 3; c < 6; c++)
                  m.set (v, c, "---");
            else {
               m.set (v, 3, dfmt.format (avg2, radius2));
               if (varm2 == null)
                  m.set (v, 4, "---");
               else
                  m.set (v, 4, dfmt.format (Math.sqrt (varm2.get (i, j)),
                        radius2));
               if (ci2 == null)
                  m.set (v, 5, "---");
               else {
                  final double lower = ci2[0].get (i, j);
                  final double upper = ci2[1].get (i, j);
                  m.set (v, 5, "[" + dfmt.format (lower, radius2) + ", "
                        + dfmt.format (upper, radius2) + "]");
               }
            }
            ++v;
         }

         if (sb.length() > 0)
            sb.append ("\n\n");
         sb.append (fmtMat.toTitleString (m, rowNames, columnNames, pm
               .rowTitle (), "Values", pm.getDescription ()));
      }
      return sb.toString();
   }
   
   /**
    * Determines if systems \texttt{res1} and \texttt{res2}
    * seem equal, and formats any
    * detected error using \texttt{fmt}.
    * This method uses {@link #getCommonPerformanceMeasures(ContactCenterEval,ContactCenterEval,Formatter)}
    * to obtain the set of common performance measures.
    * Then, it uses {@link #getDifferent(ContactCenterEval,ContactCenterEval,Set)}
    * to obtain the list of different points.
    * The method returns \texttt{true} if and only if
    * all point estimators are equal.
    * Otherwise, {@link #formatDifferentPoints(ContactCenterEval,ContactCenterEval,Map)}
    * is used to format differing points.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param fmt the formatter, or \texttt{null}.
    * @return the result of the test.
    */
   public static boolean equals (ContactCenterEval res1,
         ContactCenterEval res2, Formatter fmt) {
      final Set<PerformanceMeasureType> pmSet = getCommonPerformanceMeasures (res1, res2, fmt); 
      final Map<PerformanceMeasureType, List<Point>> diffMap = getDifferent (res1, res2, pmSet);
      if (diffMap.isEmpty ())
         return true;
      if (fmt != null) {
         final String res = formatDifferentPoints (res1, res2, diffMap);
         fmt.format ("%s%n", res);
      }
      return false;
   }
   
   /**
    * Determines if systems \texttt{res1} and \texttt{res2}
    * seem equal, and formats any
    * detected error using \texttt{fmt}.
    * This method uses 
    * {@link #getCommonPerformanceMeasures(ContactCenterEval,ContactCenterEval,Formatter)}
    * to obtain the set of common performance measures.
    * Then, it uses {@link #getDifferent(ContactCenterEval,ContactCenterEval,double,Set)}
    * to obtain the list of different points.
    * The method returns \texttt{true} if and only if
    * all point estimators are equal.
    * Otherwise, {@link #formatDifferentPoints(ContactCenterEval,ContactCenterEval,Map)}
    * is used to format differing points.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param tol the tolerance.
    * @param fmt the formatter, or \texttt{null}.
    * @return the result of the test.
    */
   public static boolean equals (ContactCenterEval res1,
         ContactCenterEval res2, double tol, Formatter fmt) {
      final Set<PerformanceMeasureType> pmSet = getCommonPerformanceMeasures (res1, res2, fmt); 
      final Map<PerformanceMeasureType, List<Point>> diffMap = getDifferent (res1, res2, tol, pmSet);
      if (diffMap.isEmpty ())
         return true;
      if (fmt != null) {
         final String res = formatDifferentPoints (res1, res2, diffMap);
         fmt.format ("%s%n", res);
      }
      return false;
   }
   
   /**
    * Determines if systems \texttt{res1} and \texttt{res2}
    * seem statistically equal, and formats any
    * detected error using \texttt{fmt}.
    * This method uses {@link #getCommonPerformanceMeasures(ContactCenterEval,ContactCenterEval,Formatter)}
    * to obtain the set of common performance measures.
    * Then, it uses {@link #getNonOverlappingCI(ContactCenterSim,ContactCenterSim,double,double,Set)}
    * to obtain the list of non-overlapping confidence
    * intervals.
    * The method returns \texttt{true} if and only if
    * all confidence intervals overlap.
    * Otherwise, {@link #formatDifferentPoints(ContactCenterSim,ContactCenterSim,Map,double)}
    * is used to format differing points.
    * @param res1 the first system.
    * @param res2 the second system.
    * @param confidenceLevel the confidence level.
    * @param fmt the formatter, or \texttt{null}.
    * @return the result of the test.
    */
   public static boolean equalsStat (ContactCenterSim res1,
         ContactCenterSim res2, double confidenceLevel, double tol, Formatter fmt) {
      final Set<PerformanceMeasureType> pmSet = getCommonPerformanceMeasures (res1, res2, fmt); 
      final Map<PerformanceMeasureType, List<Point>> nonOverlapMap = getNonOverlappingCI (res1, res2, confidenceLevel, tol, pmSet);
      if (nonOverlapMap.isEmpty ())
         return true;
      if (fmt != null) {
         final String res = formatDifferentPoints (res1, res2, nonOverlapMap, confidenceLevel);
         fmt.format ("%s%n", res);
      }
      return false;
   }
   
   public static void main (String[] args) throws IOException,
         ClassNotFoundException, ParserConfigurationException, SAXException {
      if (args.length != 3) {
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.app.CompareSimResults "
                     + "<first input file> <second input file> <confidence level>");
         System.exit (1);
      }
      final String resFile1 = args[0];
      final String resFile2 = args[1];
      final double level = Double.parseDouble (args[2]);

      final ContactCenterEvalResultsConverter cnv = new ContactCenterEvalResultsConverter();
      final ContactCenterSimResults res1 = (ContactCenterSimResults)cnv.unmarshalToEvalOrExit(new File (resFile1)); 
      final ContactCenterSimResults res2 = (ContactCenterSimResults)cnv.unmarshalToEvalOrExit(new File (resFile2)); 

      if (equalsStat (res1, res2, level, 0, new Formatter (System.out)))
         System.out.println ("All confidence intervals overlap: results seem statistically identical");
   }
}
