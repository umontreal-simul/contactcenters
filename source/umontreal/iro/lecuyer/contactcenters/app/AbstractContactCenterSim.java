package umontreal.iro.lecuyer.contactcenters.app;

import java.util.NoSuchElementException;

import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatterWithError;
import umontreal.iro.lecuyer.util.DoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXDoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXObjectMatrixFormatter;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Helper class to implement a contact center simulator.
 */
public abstract class AbstractContactCenterSim extends AbstractContactCenterEval implements
      ContactCenterSim {
   /**
    * Determines if at least one simulation has been performed since the
    * construction of the simulaator or the last reset.
    */
   private boolean oneSimDone = false;

   /**
    * Determines if random streams are automatically reset.
    */
   protected boolean autoResetStartStream = true;
   
   protected boolean seqSampEachEval = false;

   protected boolean getOneSimDone () {
      return oneSimDone;
   }

   protected void setOneSimDone (boolean oneSimDone) {
      this.oneSimDone = oneSimDone;
   }

   public boolean getAutoResetStartStream () {
      return autoResetStartStream;
   }

   public void setAutoResetStartStream (boolean r) {
      autoResetStartStream = r;
   }
   
   public boolean getSeqSampEachEval () {
      return seqSampEachEval;
   }

   public void setSeqSampEachEval (boolean seqSampEachEval) {
      this.seqSampEachEval = seqSampEachEval;
   }

   /**
    * Calls {@link #getPerformanceMeasures} and searches for \texttt{m} in the
    * returned array.
    * 
    * @param m
    *           the performance measure being tested.
    * @return a \texttt{true} value if the measure is supported, \texttt{false}
    *         otherwise.
    * @exception NullPointerException
    *               if \texttt{m} is \texttt{null}.
    */
   public boolean hasPerformanceMeasure (PerformanceMeasureType m) {
      if (m == null)
         throw new NullPointerException (
               "The queried performance measure must not be null"); //$NON-NLS-1$
      final PerformanceMeasureType[] pms = getPerformanceMeasures ();
      for (final PerformanceMeasureType pm : pms)
         if (pm == m)
            return true;
      return false;
   }

   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType m) {
      if (m.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS)
         throw new NoSuchElementException ("No matrix of tallies for " //$NON-NLS-1$
               + m.name ());
      return (MatrixOfTallies<?>) getMatrixOfStatProbes (m);
   }

   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType m) {
      if (m.getEstimationType () != EstimationType.FUNCTIONOFEXPECTATIONS)
         throw new NoSuchElementException (
               "No matrix of function of multiple means tallies for " //$NON-NLS-1$
                     + m.name ());
      return (MatrixOfFunctionOfMultipleMeansTallies<?>) getMatrixOfStatProbes (m);
   }

   public DoubleMatrix2D getPerformanceMeasure (PerformanceMeasureType m) {
      if (!getOneSimDone ())
         if (hasPerformanceMeasure (m))
            throw new IllegalStateException (
                  "The evaluation was not been performed"); //$NON-NLS-1$
         else
            throw new NoSuchElementException (
                  "Performance measure type not supported: " + m.name ()); //$NON-NLS-1$
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      final DoubleMatrix2D mat = new DenseDoubleMatrix2D (sm.rows (), sm
            .columns ());
      sm.average (mat);
      return mat;
   }

   public DoubleMatrix2D getVariance (PerformanceMeasureType m) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      final DoubleMatrix2D mat = new DenseDoubleMatrix2D (sm.rows (), sm
            .columns ());
      if (sm instanceof MatrixOfTallies)
         ((MatrixOfTallies<?>) sm).variance (mat);
      else if (sm instanceof MatrixOfFunctionOfMultipleMeansTallies)
         ((MatrixOfFunctionOfMultipleMeansTallies<?>) sm).variance (mat);
      else
         throw new NoSuchElementException ("No variance available for " //$NON-NLS-1$
               + m.name ());
      return mat;
   }

   public DoubleMatrix2D getMin (PerformanceMeasureType m) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      final DoubleMatrix2D mat = new DenseDoubleMatrix2D (sm.rows (), sm
            .columns ());
      for (int i = 0; i < mat.rows (); i++)
         for (int j = 0; j < mat.columns (); j++)
            mat.setQuick (i, j, sm.get (i, j).min ());
      return mat;
   }

   public DoubleMatrix2D getMax (PerformanceMeasureType m) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      final DoubleMatrix2D mat = new DenseDoubleMatrix2D (sm.rows (), sm
            .columns ());
      for (int i = 0; i < mat.rows (); i++)
         for (int j = 0; j < mat.columns (); j++)
            mat.setQuick (i, j, sm.get (i, j).max ());
      return mat;
   }

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType m,
         double level) {
      final MatrixOfStatProbes<?> sm = getMatrixOfStatProbes (m);
      if (!(sm instanceof MatrixOfTallies)
            && !(sm instanceof MatrixOfFunctionOfMultipleMeansTallies))
         throw new NoSuchElementException (
               "No available confidence interval for " + m.name ()); //$NON-NLS-1$
      return getConfidenceInterval (sm, level);
   }
   
   public static DoubleMatrix2D[] getConfidenceInterval (MatrixOfStatProbes<?> sm, double level) {
      final DoubleMatrix2D[] res = new DoubleMatrix2D[2];
      res[0] = new DenseDoubleMatrix2D (sm.rows (), sm.columns ());
      res[1] = new DenseDoubleMatrix2D (sm.rows (), sm.columns ());
      final double[] cr = new double[2];
      for (int i = 0; i < sm.rows (); i++)
         for (int j = 0; j < sm.columns (); j++) {
            final StatProbe probe = sm.get (i, j);
            try {
               if (probe instanceof Tally)
                  ((Tally) probe).confidenceIntervalStudent (level, cr);
               else if (probe instanceof FunctionOfMultipleMeansTally)
                  ((FunctionOfMultipleMeansTally) probe)
                        .confidenceIntervalDelta (level, cr);
            }
            catch (final RuntimeException re) {
               throw new NoSuchElementException (
                     "No available confidence interval"); //$NON-NLS-1$
            }
            res[0].set (i, j, cr[0] - cr[1]);
            res[1].set (i, j, cr[0] + cr[1]);
         }
      return res;
   }
   
   @Override
   public String formatStatistics () {
   	final StringBuilder sb = new StringBuilder ();
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (getReportParams());
      sb.append (pfmt.formatStatistics (this, getReportParams ()));
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms();
      return sb.toString ();
   }
   
   @Override
   public String formatStatisticsLaTeX () {
      final DoubleFormatter dfmt = new DefaultDoubleFormatterWithError (getReportParams().getNumDigits ());
      final DoubleFormatter dfmtLaTeX = new LaTeXDoubleFormatter (dfmt);
      final LaTeXObjectMatrixFormatter fmt = new LaTeXObjectMatrixFormatter();
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText (fmt);
      pfmt.setDoubleFormatterStatistics (dfmtLaTeX);
      pfmt.setPercentString ("\\%");
   	final StringBuilder sb = new StringBuilder ();
      sb.append (pfmt.formatStatistics (this, getReportParams()));
      if (null != pfmt.getHistogramList())
      	pfmt.writeHistogramsLaTeX();
      return sb.toString ();
   }

   @Override
   public boolean formatStatisticsExcel (WritableWorkbook wb) {
      final PerformanceMeasureFormatExcel pfmt = new PerformanceMeasureFormatExcel (wb, getReportParams ());
    /*  if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms(); */
      pfmt.setMaxColumns (getReportParams().getMaxColumns ());
      try {
         return pfmt.formatStatistics (this, getReportParams());
      }
      catch (final WriteException e) {
         final IllegalArgumentException iae = new IllegalArgumentException ("Could not write workbook");
         iae.initCause (e);
         throw iae;
      }
   }
}
