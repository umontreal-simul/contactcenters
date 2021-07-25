package umontreal.iro.lecuyer.contactcenters.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import umontreal.iro.lecuyer.contactcenters.app.params.ContactCenterEvalResultsParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ContactCenterSimResultsParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ObsMatrix;
import umontreal.iro.lecuyer.contactcenters.app.params.PMMatrix;
import umontreal.iro.lecuyer.contactcenters.app.params.PMMatrixInt;
import umontreal.iro.lecuyer.contactcenters.app.params.ObsMatrix.Obs;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.StudentDist;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.DefaultDoubleFormatterWithError;
import umontreal.iro.lecuyer.util.DoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXDoubleFormatter;
import umontreal.iro.lecuyer.util.LaTeXObjectMatrixFormatter;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.params.DoubleArray;
import umontreal.iro.lecuyer.xmlbind.params.IntArray;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Extends {@link ContactCenterEvalResults} to store
 * additional information related to the simulation of a call
 * center.
 */
public class ContactCenterSimResults extends ContactCenterEvalResults implements
      ContactCenterSim {
   private final Map<PerformanceMeasureType, DoubleMatrix2D> varMap = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (
         PerformanceMeasureType.class);
   private final Map<PerformanceMeasureType, DoubleMatrix2D> minMap = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (
         PerformanceMeasureType.class);
   private final Map<PerformanceMeasureType, DoubleMatrix2D> maxMap = new EnumMap<PerformanceMeasureType, DoubleMatrix2D> (
         PerformanceMeasureType.class);
   private final Map<PerformanceMeasureType, int[][]> nMap = new EnumMap<PerformanceMeasureType, int[][]> (
         PerformanceMeasureType.class);
   private final transient Map<PerformanceMeasureType, MatrixOfTallies<?>> tallyMap = new EnumMap<PerformanceMeasureType, MatrixOfTallies<?>> (
         PerformanceMeasureType.class);
   private final transient Map<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<?>> fmmTallyMap = new EnumMap<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<?>> (
         PerformanceMeasureType.class);
   private int nstep;

   /**
    * Constructs a new object containing results read from
    * the parameter object \texttt{ccp}.
    * It is not recommended to use this constructor directly;
    * one should use the {@link ContactCenterEvalResults#createFromParams(ContactCenterEvalResultsParams)}
    * method to create instances of this class.
    * @param ccp the contact centers results.
    */
   public ContactCenterSimResults (ContactCenterSimResultsParams ccp) {
      super (ccp);
      nstep = ccp.getNumSteps ();

      for (final PMMatrix pm : ccp.getVarianceMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (pm.getMeasure ());
         if (pmt == null)
            continue;
         final DoubleMatrix2D m = new DenseDoubleMatrix2D
         (ArrayConverter.unmarshalArray (pm));
         varMap.put (pmt, m);
      }
      for (final PMMatrix pm : ccp.getMinMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (pm.getMeasure ());
         if (pmt == null)
            continue;
         final DoubleMatrix2D m = new DenseDoubleMatrix2D
         (ArrayConverter.unmarshalArray (pm));
         minMap.put (pmt, m);
      }
      for (final PMMatrix pm : ccp.getMaxMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (pm.getMeasure ());
         if (pmt == null)
            continue;
         final DoubleMatrix2D m = new DenseDoubleMatrix2D
         (ArrayConverter.unmarshalArray (pm));
         maxMap.put (pmt, m);
      }
      for (final PMMatrixInt pm : ccp.getNumObsMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (pm.getMeasure ());
         if (pmt == null)
            continue;
         final int[][] m =
         ArrayConverter.unmarshalArray (pm);
         nMap.put (pmt, m);
      }
      for (final ObsMatrix obsMat : ccp.getObservationMatrices ()) {
         final PerformanceMeasureType pmt = getPerformanceMeasureType (obsMat.getMeasure ());
         if (pmt == null)
            continue;
         final double[][][] obs = new double[pmt.rows (this)][pmt.columns (this)][];
         for (final Obs o : obsMat.getObs ()) {
            final int r = o.getRow ();
            final int c = o.getColumn ();
            obs[r][c] = ArrayConverter.unmarshalArray (o.getValue ()); 
         }
         final MatrixOfTallies<?> mta = getMatrixOfTallies (pmt.getDescription (),
               obs);
         if (mta != null)
            tallyMap.put (pmt, mta);
      }
   }
   
   @Override
   public void writeParams (ContactCenterEvalResultsParams ccp) {
      super.writeParams (ccp);
      if (!(ccp instanceof ContactCenterSimResultsParams))
         return;
      
      final ContactCenterSimResultsParams ccp2 = (ContactCenterSimResultsParams)ccp;
      ccp2.setNumSteps (nstep);
      ccp2.getVarianceMatrices().clear();
      for (final Map.Entry<PerformanceMeasureType, DoubleMatrix2D> e : varMap.entrySet ()) {
         final PMMatrix mat = new PMMatrix();
         mat.setMeasure (e.getKey ().name ());
         final DoubleArray dp = ArrayConverter.marshalArray (e.getValue ().toArray ());
         mat.getRows ().addAll (dp.getRows ());
         ccp2.getVarianceMatrices().add (mat);
      }
      ccp2.getMinMatrices().clear();
      for (final Map.Entry<PerformanceMeasureType, DoubleMatrix2D> e : minMap.entrySet ()) {
         final PMMatrix mat = new PMMatrix();
         mat.setMeasure (e.getKey ().name ());
         final DoubleArray dp = ArrayConverter.marshalArray (e.getValue ().toArray ());
         mat.getRows ().addAll (dp.getRows ());
         ccp2.getMinMatrices().add (mat);
      }
      ccp2.getMaxMatrices().clear();
      for (final Map.Entry<PerformanceMeasureType, DoubleMatrix2D> e : maxMap.entrySet ()) {
         final PMMatrix mat = new PMMatrix();
         mat.setMeasure (e.getKey ().name ());
         final DoubleArray dp = ArrayConverter.marshalArray (e.getValue ().toArray ());
         mat.getRows ().addAll (dp.getRows ());
         ccp2.getMaxMatrices().add (mat);
      }
      ccp2.getNumObsMatrices().clear();
      for (final Map.Entry<PerformanceMeasureType, int[][]> e : nMap.entrySet()) {
         final PMMatrixInt mat = new PMMatrixInt();
         mat.setMeasure (e.getKey ().name ());
         final IntArray ip = ArrayConverter.marshalArray (e.getValue());
         mat.getRows().addAll (ip.getRows());
         ccp2.getNumObsMatrices().add (mat);
      }
      ccp2.getObservationMatrices().clear();
      for (final Map.Entry<PerformanceMeasureType, MatrixOfTallies<?>> e : tallyMap.entrySet()) {
         boolean hasObs = false;
         final MatrixOfTallies<?> mta = e.getValue();
         final ObsMatrix obsM = new ObsMatrix();
         obsM.setMeasure (e.getKey ().name ());
         for (int r = 0; r < mta.rows(); r++)
            for (int c = 0; c < mta.columns(); c++) {
               final Tally ta = mta.get (r, c);
               if (ta instanceof TallyStore) {
                  final TallyStore ta2 = (TallyStore)ta;
                  final ObsMatrix.Obs obs = new ObsMatrix.Obs();
                  obs.setRow (r);
                  obs.setColumn (c);
                  obs.getValue().clear();
                  final DoubleArrayList obsList = ta2.getDoubleArrayList();
                  obsList.trimToSize();
                  obs.getValue().addAll (ArrayConverter.marshalArray (obsList.elements()));
                  obsM.getObs().add (obs);
                  hasObs = true;
               }
            }
         if (hasObs)
            ccp2.getObservationMatrices().add (obsM);
      }
   }

   /**
    * Constructs a new contact center results container by getting simulation
    * results from the simulator \texttt{sim}, and compoting confidence
    * intervals with level \texttt{level}.
    * 
    * @param sim
    *           the contact center simulator.
    */
   public ContactCenterSimResults (ContactCenterSim sim) {
      super (sim);
      for (final PerformanceMeasureType pm : sim.getPerformanceMeasures ()) {
         try {
            varMap.put (pm, sim.getVariance (pm));
         }
         catch (final NoSuchElementException ne) {}
         try {
            minMap.put (pm, sim.getMin (pm));
         }
         catch (final NoSuchElementException ne) {}
         try {
            maxMap.put (pm, sim.getMax (pm));
         }
         catch (final NoSuchElementException ne) {}
         try {
            final int nr = pm.rows (sim);
            final int nc = pm.columns (sim);
            final int[][] nMat = new int[nr][nc];
            if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
               final MatrixOfFunctionOfMultipleMeansTallies<?> mta = sim
                     .getMatrixOfFunctionOfMultipleMeansTallies (pm);
               fmmTallyMap.put (pm, mta);
               for (int r = 0; r < nr; r++)
                  for (int c = 0; c < nc; c++)
                     nMat[r][c] = mta.get (r, c).numberObs ();
               nMap.put (pm, nMat);
            }
            else {
               final MatrixOfTallies<?> mta = sim.getMatrixOfTallies (pm);
               tallyMap.put (pm, mta.clone ());
               for (int r = 0; r < nr; r++)
                  for (int c = 0; c < nc; c++)
                     nMat[r][c] = mta.get (r, c).numberObs ();
               nMap.put (pm, nMat);
            }
         }
         catch (final NoSuchElementException ne) {}
      }
      nstep = sim.getCompletedSteps ();
   }

   public boolean getAutoResetStartStream () {
      throw new UnsupportedOperationException ();
   }
   
   public boolean getSeqSampEachEval () {
      throw new UnsupportedOperationException ();
   }

   public void setSeqSampEachEval (boolean seqSamp) {
      throw new UnsupportedOperationException ();
   }

   public int getCompletedSteps () {
      return nstep;
   }

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType m,
         double level) {
      final DoubleMatrix2D avg = getPerformanceMeasure (m);
      final DoubleMatrix2D var = getVariance (m);
      final int[][] nMat = nMap.get (m);
      if (avg == null || var == null || nMat == null)
         throw new NoSuchElementException ("No confidence inteval for "
               + m.name ());
      final double a = 0.5 * (level + 1.0);
      final int nr = avg.rows ();
      final int nc = avg.columns ();
      final DoubleMatrix2D[] ci = new DoubleMatrix2D[2];
      ci[0] = new DenseDoubleMatrix2D (nr, nc);
      ci[1] = new DenseDoubleMatrix2D (nr, nc);
      for (int r = 0; r < nr; r++)
         for (int c = 0; c < nc; c++) {
            double t;
            if (m.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS)
               t = NormalDist.inverseF01 (a);
            else
               t = nMat[r][c] < 2 ? Double.NaN : StudentDist.inverseF (
                     nMat[r][c], a);
            final double center = avg.getQuick (r, c);
            final double radius = t
                  * Math.sqrt (var.getQuick (r, c) / nMat[r][c]);
            ci[0].setQuick (r, c, center - radius);
            ci[1].setQuick (r, c, center + radius);
         }
      return ci;
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      MatrixOfStatProbes<? extends StatProbe> probe = tallyMap.get (m);
      if (probe == null)
         probe = fmmTallyMap.get (m);
      if (probe == null)
         throw new NoSuchElementException ("No variance for " + m.name ());
      return probe;
   }

   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType m) {
      final MatrixOfTallies<?> mta = tallyMap.get (m);
      if (mta == null)
         throw new NoSuchElementException ("No matrix of tallies for "
               + m.name ());
      return mta;
   }

   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType m) {
      final MatrixOfFunctionOfMultipleMeansTallies<?> mta = fmmTallyMap.get (m);
      if (mta == null)
         throw new NoSuchElementException (
               "No matrix of function of multiple means tallies for "
                     + m.name ());
      return mta;
   }

   public DoubleMatrix2D getMax (PerformanceMeasureType m) {
      final DoubleMatrix2D mat = maxMap.get (m);
      if (mat == null)
         throw new NoSuchElementException ("No matrix of maxima for "
               + m.name ());
      return mat;
   }

   public DoubleMatrix2D getMin (PerformanceMeasureType m) {
      final DoubleMatrix2D mat = minMap.get (m);
      if (mat == null)
         throw new NoSuchElementException ("No matrix of minima for "
               + m.name ());
      return mat;
   }

   public DoubleMatrix2D getVariance (PerformanceMeasureType m) {
      final DoubleMatrix2D mat = varMap.get (m);
      if (mat == null)
         throw new NoSuchElementException ("No matrix of variances for "
               + m.name ());
      return mat;
   }

   public void newSeeds () {
      throw new UnsupportedOperationException ();
   }

   public void resetNextSubstream () {
      throw new UnsupportedOperationException ();
   }

   public void resetStartStream () {
      throw new UnsupportedOperationException ();
   }

   public void resetStartSubstream () {
      throw new UnsupportedOperationException ();
   }

   public void setAutoResetStartStream (boolean r) {
      throw new UnsupportedOperationException ();
   }

   private MatrixOfTallies<TallyStore> getMatrixOfTallies (String name,
         double[][][] obs) {
      if (obs == null)
         return null;
      final MatrixOfTallies<TallyStore> mta = MatrixOfTallies
            .createWithTallyStore (obs.length, obs[0].length);
      mta.setName (name);
      for (int r = 0; r < obs.length; r++)
         for (int c = 0; c < obs[r].length; c++) {
            if (obs[r][c] == null)
               continue;
            final TallyStore ts = mta.get (r, c);
            for (int j = 0; j < obs[r][c].length; j++)
               ts.add (obs[r][c][j]);
         }
      return mta;
   }
   
   @Override
   public String formatStatistics () {
      final DoubleFormatter dfmt = new DefaultDoubleFormatterWithError (getReportParams().getNumDigits ());
      final PerformanceMeasureFormatText pfmt = new PerformanceMeasureFormatText ();
      pfmt.setDoubleFormatterStatistics (dfmt);
   	final StringBuilder sb = new StringBuilder ();
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
      pfmt.setMaxColumns ((short)getReportParams().getMaxColumns ());
    /*  if (null != pfmt.getHistogramList())
      	pfmt.writeHistograms(); */
      try {
         return pfmt.formatStatistics (this, getReportParams());
      }
      catch (final WriteException e) {
         final IllegalArgumentException iae = new IllegalArgumentException ("Could not write workbook");
         iae.initCause (e);
         throw iae;
      }
   }
   
   @SuppressWarnings ("unchecked")
   private void readObject (ObjectInputStream is) throws IOException,
         ClassNotFoundException {
      is.defaultReadObject ();
      final Map<PerformanceMeasureType, double[][][]> obsMap = (Map<PerformanceMeasureType, double[][][]>) is
            .readObject ();
      for (final Map.Entry<PerformanceMeasureType, double[][][]> e : obsMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final double[][][] obs = e.getValue ();
         final MatrixOfTallies mta = getMatrixOfTallies (pm.getDescription (),
               obs);
         if (mta != null)
            tallyMap.put (pm, mta);
      }
      internalCheck ();
   }

   private double[][][] getObs (MatrixOfTallies<?> mta) {
      final double[][][] obs = new double[mta.rows ()][mta.columns ()][];
      boolean obsFound = false;
      boolean obsNotFound = false;
      for (int r = 0; r < obs.length; r++)
         for (int c = 0; c < obs[r].length; c++) {
            final Tally ta = mta.get (r, c);
            if (ta instanceof TallyStore) {
               final TallyStore ts = (TallyStore) ta;
               final double[] a = ts.getArray ();
               obs[r][c] = new double[ts.numberObs()];
               System.arraycopy (a, 0, obs[r][c], 0, ts.numberObs());
               obsFound = true;
            }
            else
               obsNotFound = true;
         }
      if (obsFound) {
         if (obsNotFound)
            for (final double[][] row : obs)
               for (int c = 0; c < row.length; c++)
                  if (row[c] == null)
                     row[c] = new double[0];
         return obs;
      }
      return null;
   }

   private void writeObject (ObjectOutputStream os) throws IOException {
      os.defaultWriteObject ();
      final Map<PerformanceMeasureType, double[][][]> obsMap = new EnumMap<PerformanceMeasureType, double[][][]> (
            PerformanceMeasureType.class);
      for (final Map.Entry<PerformanceMeasureType, MatrixOfTallies<? extends Tally>> e : tallyMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final MatrixOfTallies<? extends Tally> ms = e.getValue ();
         final double[][][] obs = getObs (ms);
         if (obs != null)
            obsMap.put (pm, obs);
      }
      os.writeObject (obsMap);
   }

   private void internalCheck () {
      checkNulls ("Variance map", varMap);
      checkNulls ("Minima map", minMap);
      checkNulls ("Maxima map", maxMap);
      checkNulls ("numObs map", nMap);
      for (final Map.Entry<PerformanceMeasureType, int[][]> e : nMap
            .entrySet ()) {
         final int[][] n = e.getValue ();
         for (int i = 0; i < n.length; i++)
            if (n[i] == null)
               throw new NullPointerException ("Row " + i
                     + " in the matrix of numObsMap(" + e.getKey ().toString ()
                     + ") is null");
      }
      if (nstep < 0)
         throw new IllegalArgumentException ("nrep < 0");
   }

   @Override
   public void check () {
      super.check ();
      internalCheck ();
   }

   public double getConfidenceLevel () {
      return getReportParams ().getConfidenceLevel ();
   }

   public void setConfidenceLevel (double level) {
      getReportParams ().setConfidenceLevel (level);
   }
   
   @Override
   public ContactCenterSimResultsParams createParams () {
      final ContactCenterSimResultsParams ccp = new ContactCenterSimResultsParams();
      writeParams (ccp);
      return ccp;
   }
}
