package umontreal.iro.lecuyer.contactcenters.msk.cv;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.EstimationType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;

import umontreal.iro.lecuyer.collections.DenseMatrix;
import umontreal.iro.lecuyer.collections.Matrix;
import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.lincv.FunctionOfMultipleMeansTallyWithCV;
import umontreal.ssj.stat.list.lincv.ListOfTalliesWithCV;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;

/**
 * Represents call center statistics on which control variables are applied. An
 * instance of this class is constructed from a {@link CallCenterStatProbes}
 * object, and defines statistical probes for controlled estimators. When
 * {@link #applyControlVariables} is called, observations are extracted from the
 * probes in the inner {@link CallCenterStatProbes} object, and controlled
 * observations are added to the encapsulated probes. This way, control
 * variables are applied after the simulation is finished and do not require
 * modifying the simulator.
 */
public class CVCallCenterStat implements CallCenterStatProbes {
   private SimLogic sim;
   private CallCenterStatProbes inStat;
   private ControlVariable[] cvs;

   private final Map<PerformanceMeasureType, Matrix<ListOfTalliesWithCV<?>>> outTallyMap = new EnumMap<PerformanceMeasureType, Matrix<ListOfTalliesWithCV<?>>> (
         PerformanceMeasureType.class);
   private final Map<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV>> outFmmTallyMap = new EnumMap<PerformanceMeasureType, MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV>> (
         PerformanceMeasureType.class);
   private final Map<PerformanceMeasureType, DoubleMatrix2D[][]> betaMapFmm = new EnumMap<PerformanceMeasureType, DoubleMatrix2D[][]> (
         PerformanceMeasureType.class);
   private PerformanceMeasureType[] pms;

   /**
    * Constructs a new CV call center statistical object using the simulation
    * logic \texttt{sim}, taking statistics in the \texttt{stat} object, and
    * applying the control variables \texttt{cvs}. If \texttt{fmm} is
    * \texttt{false}, functions of multiple means tallies are ignored.
    * Otherwise, control variables are applied on them when appropriate.
    * 
    * @param sim
    *           the simulation logic.
    * @param inStat
    *           the call center statistics.
    * @param fmm
    *           if control variables are applied on functions of multiple
    *           averages.
    * @param cvs
    *           the array of control variables to apply.
    */
   public CVCallCenterStat (SimLogic sim, CallCenterStatProbes inStat,
         boolean fmm, ControlVariable... cvs) {
      if (sim == null || inStat == null || cvs == null)
         throw new NullPointerException ();
      this.sim = sim;
      this.inStat = inStat;
      this.cvs = cvs;
      for (final PerformanceMeasureType pm : inStat.getPerformanceMeasures ()) {
         boolean useCv = false;
         for (int j = 0; j < cvs.length && !useCv; j++)
            if (cvs[j].appliesTo (pm))
               useCv = true;
         if (!useCv)
            continue;
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
            if (isFmmSupported (pm)) {
               // MatrixOfFunctionOfMultipleMeansTallies<?>
               // mta = inStat.getMatrixOfFunctionOfMultipleMeansTallies (pm);
               final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> mtaOut = createMatrixOfFunctionOfMultipleMeansTallies (pm);
               outFmmTallyMap.put (pm, mtaOut);
            }
         }
         else {
            // MatrixOfTallies<?> mta =
            // inStat.getMatrixOfTallies (pm);
            final Matrix<ListOfTalliesWithCV<?>> mtaOut = createMatrixOfTallies (pm);
            outTallyMap.put (pm, mtaOut);
         }
      }

      final List<PerformanceMeasureType> types = new ArrayList<PerformanceMeasureType> (
            outTallyMap.size () + outFmmTallyMap.size ());
      types.addAll (outTallyMap.keySet ());
      types.addAll (outFmmTallyMap.keySet ());
      pms = types.toArray (new PerformanceMeasureType[types.size ()]);
   }

   private Matrix<ListOfTalliesWithCV<?>> createMatrixOfTallies (
         PerformanceMeasureType pm) {
      if (!inStat.hasPerformanceMeasure (pm))
         return null;
      final MatrixOfStatProbes<?> mt = inStat.getMatrixOfStatProbes (pm);
      if (mt == null)
         return null;
      return new DenseMatrix<ListOfTalliesWithCV<?>> (mt.rows (), mt.columns ());
   }

   private MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> createMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType pm) {
      if (!inStat.hasPerformanceMeasure (pm))
         return null;
      final MatrixOfFunctionOfMultipleMeansTallies<?> mt = inStat
            .getMatrixOfFunctionOfMultipleMeansTallies (pm);
      if (mt == null)
         return null;
      final int nr = mt.rows ();
      final int nc = mt.columns ();
      final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> fmt = new MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> (
            nr, nc);
      return fmt;
   }
   
   public ControlVariable[] getControlVariables() {
      return cvs.clone();
   }

   public void init () {
      for (final Matrix<ListOfTalliesWithCV<?>> m : outTallyMap.values ())
         for (final ListOfTalliesWithCV<?> l : m)
            if (l != null)
               l.init ();
      for (final MatrixOfFunctionOfMultipleMeansTallies<?> m : outFmmTallyMap
            .values ())
         m.init ();
      betaMapFmm.clear ();
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      return pms.clone ();
   }

   public boolean hasPerformanceMeasure (PerformanceMeasureType pm) {
      return outTallyMap.containsKey (pm) || outFmmTallyMap.containsKey (pm);
   }
   
   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes () {
      return inStat.getMatricesOfStatProbes();
   }

   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMatrixOfStatProbes (pm);
   }

   public MatrixOfTallies<?> getMatrixOfTallies (PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMatrixOfTallies (pm);
   }

   public MatrixOfTallies<TallyStore> getMatrixOfTallyStores (
         PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMatrixOfTallyStores (pm);
   }

   public MatrixOfFunctionOfMultipleMeansTallies<?> getMatrixOfFunctionOfMultipleMeansTallies (
         PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMatrixOfFunctionOfMultipleMeansTallies (pm);
   }

   private void initBetaMapFmm (PerformanceMeasureType pm, int nr, int nc) {
      final DoubleMatrix2D[][] val = new DoubleMatrix2D[nr][nc];
      betaMapFmm.put (pm, val);
   }

   private void putInBetaMapFmm (PerformanceMeasureType pm, int row, int col,
         DoubleMatrix2D beta) {
      final DoubleMatrix2D[][] val = betaMapFmm.get (pm);
      val[row][col] = beta;
   }

   /**
    * Returns the $\boldbeta$ arrays for performance measure of type
    * \texttt{pm}. Element \texttt{[r][c][i]} of the returned array corresponds
    * to the $i$th control variable applied to the performance measure of type
    * \texttt{pm}, at position $($\texttt{r}, \texttt{c}$)$.
    * 
    * @param pm
    *           the type of performance measure.
    * @return the $\boldbeta$ vectors.
    */
   public double[][][] getBetas (PerformanceMeasureType pm) {
      if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
         final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> mta = outFmmTallyMap
               .get (pm);
         if (mta == null)
            throw new NoSuchElementException ();
         final double[][][] betas = new double[mta.rows ()][mta.columns ()][];
         for (int r = 0; r < betas.length; r++)
            for (int c = 0; c < betas[r].length; c++) {
               final FunctionOfMultipleMeansTallyWithCV ta = mta.get (r, c);
               betas[r][c] = ta.getBeta ().clone ();
            }
         return betas;
      }
      else {
         final Matrix<ListOfTalliesWithCV<?>> mta = outTallyMap.get (pm);
         if (mta == null)
            throw new NoSuchElementException ();
         final double[][][] betas = new double[mta.rows ()][mta.columns ()][];
         for (int r = 0; r < betas.length; r++)
            for (int c = 0; c < betas[r].length; c++) {
               final ListOfTalliesWithCV<?> lst = mta.get (r, c);
               betas[r][c] = lst.getBeta ().viewColumn (0).toArray ();
            }
         return betas;
      }
   }

   /**
    * Returns a 2D array of matrices representing the $\beta$ constants for the
    * control variables applied to the components of functions of multiple means
    * represented by the type of performance measure \texttt{pm}. Element
    * \texttt{[r][c]} of the returned array contains the $\boldbeta$ matrix for
    * performance measure at position $($\texttt{r}, \texttt{c}$)$.
    * 
    * @param pm
    *           the type of performance measure.
    * @return the $\boldbeta$ matrices.
    */
   public DoubleMatrix2D[][] getBetaMatrixFmm (PerformanceMeasureType pm) {
      final DoubleMatrix2D[][] val = betaMapFmm.get (pm);
      if (val == null)
         throw new NoSuchElementException ();
      return val;
   }

   public DoubleMatrix2D getAverage (PerformanceMeasureType pm) {
      if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
         final MatrixOfStatProbes<?> probes = outFmmTallyMap.get (pm);
         if (probes == null)
            throw new NoSuchElementException ();
         final DoubleMatrix2D avg = new DenseDoubleMatrix2D (probes.rows (),
               probes.columns ());
         probes.average (avg);
         return avg;
      }
      else {
         final Matrix<ListOfTalliesWithCV<?>> probes = outTallyMap.get (pm);
         if (probes == null)
            throw new NoSuchElementException ();
         final DoubleMatrix2D avg = new DenseDoubleMatrix2D (probes.rows (),
               probes.columns ());
         for (int r = 0; r < probes.rows (); r++)
            for (int c = 0; c < probes.columns (); c++) {
               final ListOfTalliesWithCV<?> lst = probes.get (r, c);
               avg.setQuick (r, c, lst == null ? Double.NaN : lst
                     .averageWithCV (0));
            }
         return avg;
      }
   }

   public DoubleMatrix2D getMax (PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMax (pm);
   }

   public DoubleMatrix2D getMin (PerformanceMeasureType pm) {
      if (!hasPerformanceMeasure (pm))
         throw new NoSuchElementException ();
      return inStat.getMin (pm);
   }

   public DoubleMatrix2D getVariance (PerformanceMeasureType pm) {
      if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> mta = outFmmTallyMap
               .get (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         mta.variance (var);
         return var;
      }
      else {
         final Matrix<ListOfTalliesWithCV<?>> mta = outTallyMap.get (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         for (int r = 0; r < mta.rows (); r++)
            for (int c = 0; c < mta.columns (); c++) {
               final ListOfTalliesWithCV<?> lst = mta.get (r, c);
               var.setQuick (r, c, lst == null ? Double.NaN : lst
                     .covarianceWithCV (0, 0));
            }
         return var;
      }
   }

   public DoubleMatrix2D getVarianceOfAverage (PerformanceMeasureType pm) {
      if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> mta = outFmmTallyMap
               .get (pm);
         final int nr = mta.rows ();
         final int nc = mta.columns ();
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (nr, nc);
         mta.variance (var);
         for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
               var.setQuick (r, c, var.getQuick (r, c)
                     / mta.get (r, c).numberObs ());
         return var;
      }
      else {
         final Matrix<ListOfTalliesWithCV<?>> mta = outTallyMap.get (pm);
         final DoubleMatrix2D var = new DenseDoubleMatrix2D (mta.rows (), mta
               .columns ());
         for (int r = 0; r < mta.rows (); r++)
            for (int c = 0; c < mta.columns (); c++) {
               final ListOfTalliesWithCV<?> lst = mta.get (r, c);
               var.setQuick (r, c, lst == null ? Double.NaN : lst
                     .covarianceWithCV (0, 0)
                     / lst.numberObs ());
            }
         return var;
      }
   }

   public DoubleMatrix2D[] getConfidenceInterval (PerformanceMeasureType pm,
         double level) {
      final DoubleMatrix2D[] res = new DoubleMatrix2D[2];
      final double[] cr = new double[2];
      if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
         final MatrixOfFunctionOfMultipleMeansTallies<?> mta = outFmmTallyMap
               .get (pm);
         for (int i = 0; i < 2; i++)
            res[i] = new DenseDoubleMatrix2D (mta.rows (), mta.columns ());
         for (int r = 0; r < mta.rows (); r++)
            for (int c = 0; c < mta.columns (); c++) {
               final FunctionOfMultipleMeansTally ta = mta.get (r, c);
               ta.confidenceIntervalDelta (level, cr);
               res[0].setQuick (r, c, cr[0] - cr[1]);
               res[1].setQuick (r, c, cr[0] + cr[1]);
            }
         return res;
      }
      else {
         final Matrix<ListOfTalliesWithCV<?>> mta = outTallyMap.get (pm);
         for (int i = 0; i < 2; i++)
            res[i] = new DenseDoubleMatrix2D (mta.rows (), mta.columns ());
         for (int r = 0; r < mta.rows (); r++)
            for (int c = 0; c < mta.columns (); c++) {
               final ListOfTalliesWithCV<?> lst = mta.get (r, c);
               cr[0] = cr[1] = Double.NaN;
               if (lst != null)
                  lst.confidenceIntervalStudentWithCV (0, level, cr);
               res[0].setQuick (r, c, cr[0] - cr[1]);
               res[1].setQuick (r, c, cr[0] + cr[1]);
            }
         return res;
      }
   }

   public void initCV () {
      for (final ControlVariable cv : cvs)
         cv.init (sim);
      arv.init (sim);
   }

   private void applyControlVariables (PerformanceMeasureType pm,
         MatrixOfTallies<TallyStore> mta, CVBetaFunction cvBeta) {
      final Matrix<ListOfTalliesWithCV<?>> mtaOut = outTallyMap.get (pm);
      if (mtaOut == null)
         return;
      final int nr = mta.rows ();
      final int nc = mta.columns ();
      final boolean[] useCvs = new boolean[cvs.length];
      for (int r = 0; r < nr; r++)
         for (int c = 0; c < nc; c++) {
            final TallyStore ta = mta.get (r, c);
            final int n = ta.numberObs ();
            int ncv = 0;
            for (int i = 0; i < cvs.length; i++)
               if (cvs[i].appliesTo (sim, pm, r, c)) {
                  ++ncv;
                  useCvs[i] = true;
               }
               else
                  useCvs[i] = false;
            ListOfTalliesWithCV<?> lstOut = mtaOut.get (r, c);
            if (lstOut == null || lstOut.getNumControlVariables () != 0)
               mtaOut.set (r, c, lstOut = ListOfTalliesWithCV.createWithTally (
                     1, 0));
            else
               lstOut.init ();
            final double[] taArray = ta.getArray();
            for (int j = 0; j < n; j++) {
               double x = taArray[j];
               for (int i = 0, icv = 0; i < cvs.length; i++)
                  if (useCvs[i]) {
                     final double beta = cvBeta.getBeta (pm, r, c, icv, j);
                     final double ctilde = cvs[i].getObs (sim, inStat, pm, r,
                           c, j);
                     x -= beta * ctilde;
                  }
               lstOut.get (0).add (x);
            }
         }
   }

   private void applyControlVariables (PerformanceMeasureType pm,
         MatrixOfTallies<TallyStore> mta, double[][][] betas) {
      final Matrix<ListOfTalliesWithCV<?>> mtaOut = outTallyMap.get (pm);
      if (mtaOut == null)
         return;
      final int nr = mta.rows ();
      final int nc = mta.columns ();
      final boolean[] useCvs = new boolean[cvs.length];
      for (int r = 0; r < nr; r++)
         for (int c = 0; c < nc; c++) {
            final TallyStore ta = mta.get (r, c);
            final int n = ta.numberObs ();
            int ncv = 0;
            for (int i = 0; i < cvs.length; i++)
               if (cvs[i].appliesTo (sim, pm, r, c)) {
                  ++ncv;
                  useCvs[i] = true;
               }
               else
                  useCvs[i] = false;
            final double[] cv = new double[ncv];
            ListOfTalliesWithCV<?> lstOut = mtaOut.get (r, c);
            if (lstOut == null || lstOut.getNumControlVariables () != ncv)
               mtaOut.set (r, c, lstOut = ListOfTalliesWithCV.createWithTally (
                     1, ncv));
            else
               lstOut.init ();
            final double[] taArray = ta.getArray();
            for (int j = 0; j < n; j++) {
               final double x = taArray[j];
               for (int i = 0, icv = 0; i < cvs.length; i++)
                  if (useCvs[i])
                     cv[icv++] = cvs[i].getObs (sim, inStat, pm, r, c, j);
               lstOut.add (x, cv);
            }
            if (betas != null && betas[r][c] != null) {
               final DoubleMatrix2D betaMatrix = new DenseDoubleMatrix2D (
                     betas[r][c].length, 1);
               betaMatrix.viewColumn (0).assign (betas[r][c]);
               lstOut.setBeta (betaMatrix);
            }
            else
               lstOut.estimateBeta ();
         }
   }

   private void applyControlVariables (PerformanceMeasureType pm,
         MatrixOfFunctionOfMultipleMeansTallies<?> mta, double[][][] betas) {
      final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTallyWithCV> mtaOut = outFmmTallyMap
            .get (pm);
      if (mtaOut == null)
         return;
      final int nr = mta.rows ();
      final int nc = mta.columns ();
      final boolean[] useCvs = new boolean[cvs.length];
      initBetaMapFmm (pm, nr, nc);
      for (int r = 0; r < nr; r++)
         for (int c = 0; c < nc; c++) {
            final FunctionOfMultipleMeansTally ta = mta.get (r, c);
            FunctionOfMultipleMeansTallyWithCV taOut = mtaOut.get (r, c);
            final int n = ta.numberObs ();
            int ncv = 0;
            for (int i = 0; i < cvs.length; i++)
               if (cvs[i].appliesTo (sim, pm, r, c)) {
                  ++ncv;
                  useCvs[i] = true;
               }
               else
                  useCvs[i] = false;
            final int d = ta.getDimension ();
            final double[] tmp = new double[d + ncv];
            if (taOut == null || taOut.getNumControlVariables () != ncv)
               mtaOut.set (r, c, taOut = new FunctionOfMultipleMeansTallyWithCV (ta.getFunction (), d, ncv));
            else
               taOut.init ();
            for (int j = 0; j < n; j++) {
               for (int i = 0; i < d; i++)
                  tmp[i] = getFmmElement (pm, r, c, i, j);
               for (int i = 0, icv = d; i < cvs.length; i++)
                  if (useCvs[i])
                     tmp[icv++] = cvs[i].getObs (sim, inStat, pm, r, c, j);
               taOut.add (tmp);
            }
            if (betas != null && betas[r][c] != null)
               taOut.setBeta (betas[r][c]);
            else {
               taOut.estimateBeta ();
               putInBetaMapFmm (pm, r, c, taOut.getListOfTalliesWithCV ()
                     .getBeta ().copy ());
            }
         }
   }

   /**
    * Equivalent to
    * {@link #applyControlVariables(CVBetaFunction) applyControlVariables}
    * \texttt{(null)}.
    */
   public void applyControlVariables () {
      for (final PerformanceMeasureType pm : inStat.getPerformanceMeasures ())
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS)
            applyControlVariables (pm, inStat
                  .getMatrixOfFunctionOfMultipleMeansTallies (pm), null);
         else
            applyControlVariables (pm, inStat.getMatrixOfTallyStores (pm),
                  (double[][][]) null);
   }

   /**
    * Applies the control variables for the supported estimators. If
    * \texttt{betaFunc} is non-\texttt{null}, it is used for obtaining the
    * $\beta$ constants. Otherwise, the constants are estimated from the
    * statistics.
    * 
    * @param cvBeta
    *           the beta function calculator, or \texttt{null}.
    */
   public void applyControlVariables (CVBetaFunction cvBeta) {
      for (final PerformanceMeasureType pm : inStat.getPerformanceMeasures ())
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS)
            applyControlVariables (pm, inStat
                  .getMatrixOfFunctionOfMultipleMeansTallies (pm), null);
         else
            applyControlVariables (pm, inStat.getMatrixOfTallyStores (pm),
                  cvBeta);
   }

   public void applyControlVariables (
         Map<PerformanceMeasureType, double[][][]> betas) {
      for (final PerformanceMeasureType pm : inStat.getPerformanceMeasures ())
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS)
            applyControlVariables (pm, inStat
                  .getMatrixOfFunctionOfMultipleMeansTallies (pm), betas
                  .get (pm));
         else
            applyControlVariables (pm, inStat.getMatrixOfTallyStores (pm),
                  betas.get (pm));
   }

   // Only this part depends on the contents of the
   // statistical probes.

   private final NumArrivalsCV arv = new NumArrivalsCV ();
   private static final int NUMERATOR = 0;
   private static final int DENOMINATOR = 1;

   private boolean isFmmSupported (PerformanceMeasureType pm) {
      switch (pm) {
      case SERVICELEVEL:
      case SERVICELEVEL2:
      case ABANDONMENTRATIO:
      case ABANDONMENTRATIOBEFOREAWT:
      case ABANDONMENTRATIOAFTERAWT:
      case DELAYRATIO:
      case WAITINGTIME:
      case WAITINGTIMEWAIT:
      case OCCUPANCY:
      case OCCUPANCY2:
         return true;
      default:
         return false;
      }
   }

   private double getFmmElement (PerformanceMeasureType pm, int row, int col,
         int index, int obs) {
      MatrixOfTallies<TallyStore> inTarget;
      TallyStore inTargetTally;
      double a;
      switch (index) {
      case NUMERATOR:
         switch (pm) {
         case SERVICELEVEL:
            inTarget = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFSERVICESBEFOREAWT);
            inTargetTally = inTarget.get (row, col);
            return inTargetTally.getArray ()[obs];
         case SERVICELEVEL2:
            inTarget = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFSERVICESBEFOREAWT);
            inTargetTally = inTarget.get (row, col);
            final MatrixOfTallies<TallyStore> inAbandonedBeforeAwt = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFABANDONMENTBEFOREAWT);
            final TallyStore inAbandonedBeforeAwtTally = inAbandonedBeforeAwt
                  .get (row, col);
            return inTargetTally.getArray ()[obs]
                  + inAbandonedBeforeAwtTally.getArray ()[obs];
         case WAITINGTIME:
         case WAITINGTIMEWAIT:
            final MatrixOfTallies<TallyStore> inSumWaitingTime = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.SUMWAITINGTIMES);
            final TallyStore inSumWaitingTimeTally = inSumWaitingTime.get (row,
                  col);
            return inSumWaitingTimeTally.getArray ()[obs];
         case DELAYRATIO:
            final MatrixOfTallies<TallyStore> inPosWait = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFDELAY);
            final TallyStore inPosWaitTally = inPosWait.get (row, col);
            return inPosWaitTally.getArray ()[obs];
         case ABANDONMENTRATIO:
            final MatrixOfTallies<TallyStore> inAbandonments = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFABANDONMENT);
            final TallyStore inAbandonmentsTally = inAbandonments.get (row,
                  col);
            return inAbandonmentsTally.getArray ()[obs];
         case ABANDONMENTRATIOBEFOREAWT:
            final MatrixOfTallies<TallyStore> inAbandonmentsB = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFABANDONMENTBEFOREAWT);
            final TallyStore inAbandonmentsTallyB = inAbandonmentsB.get (row,
                  col);
            return inAbandonmentsTallyB.getArray ()[obs];
         case ABANDONMENTRATIOAFTERAWT:
            final MatrixOfTallies<TallyStore> inAbandonmentsA = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFABANDONMENTAFTERAWT);
            final TallyStore inAbandonmentsTallyA = inAbandonmentsA.get (row,
                  col);
            return inAbandonmentsTallyA.getArray ()[obs];
         case OCCUPANCY:
         case OCCUPANCY2:
            final MatrixOfTallies<TallyStore> inBusyAgents = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.AVGBUSYAGENTS);
            final TallyStore inBusyAgentsTally = inBusyAgents.get (row,
                  col);
            return inBusyAgentsTally.getArray ()[obs];
         }
         break;
      case DENOMINATOR:
         // Problem for obtaining the total number of inbound calls.
         switch (pm) {
         case WAITINGTIME:
         case ABANDONMENTRATIO:
         case ABANDONMENTRATIOBEFOREAWT:
         case ABANDONMENTRATIOAFTERAWT:
         case DELAYRATIO:
            final MatrixOfTallies<TallyStore> inArv = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFARRIVALS);
            final TallyStore inArvTally = inArv.get (row, col);
            return inArvTally.getArray ()[obs];
         case WAITINGTIMEWAIT:
            final MatrixOfTallies<TallyStore> inPosWait = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFDELAY);
            final TallyStore inPosWaitTally = inPosWait.get (row, col);
            return inPosWaitTally.getArray ()[obs];
         case SERVICELEVEL:
            a = arv.getNonCenteredObs (sim, inStat,
                  PerformanceMeasureType.SERVICELEVEL, row, col, obs);
            final MatrixOfTallies<TallyStore> inAbandonedBeforeAwt = inStat
                  .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFABANDONMENTBEFOREAWT);
            final TallyStore inAbandonedBeforeAwtTally = inAbandonedBeforeAwt
                  .get (row, col);
            return a - inAbandonedBeforeAwtTally.getArray ()[obs];
         case SERVICELEVEL2:
            a = arv.getNonCenteredObs (sim, inStat,
                  PerformanceMeasureType.SERVICELEVEL2, row, col, obs);
            return a;
         case OCCUPANCY:
            final MatrixOfTallies<TallyStore> inNumScheduledAgents = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.AVGSCHEDULEDAGENTS);
            final TallyStore inNumScheduledAgentsTally = inNumScheduledAgents.get (row, col);
            return inNumScheduledAgentsTally.getArray ()[obs];
         case OCCUPANCY2:
            final MatrixOfTallies<TallyStore> inNumWorkingAgents = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.AVGWORKINGAGENTS);
            final TallyStore inNumWorkingAgentsTally = inNumWorkingAgents.get (row, col);
            return inNumWorkingAgentsTally.getArray ()[obs];
         }
         break;
      }
      throw new IllegalArgumentException ();
   }
}
