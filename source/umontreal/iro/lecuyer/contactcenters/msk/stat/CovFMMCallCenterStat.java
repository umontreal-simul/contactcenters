package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;

import umontreal.iro.lecuyer.contactcenters.app.EstimationType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Represents a set of probes that collect covariances in matrices of functions
 * of multiple means tallies. An instance of this class is constructed using a
 * {@link CallCenterStatProbes} object. For each matrix of functions of multiple
 * means tallies defined in the inner set of probes, this class can extract the
 * covariance matrix of the functions' domain, and add these covariances into
 * matrices of tallies. This results in averages of covariances which are useful
 * for estimating the variance of stratified estimators.
 * 
 * More specifically, let $\boldX\in\RR^d$ be a vector used to compute the
 * function associated with position $(r, c)$ in a matrix of performance
 * measures $m$. Let $\boldSigma_\rmX$ be the matrix of covariances of $\boldX$.
 * We suppose that $\boldX_0, \ldots, \boldX_{k-1}$ are i.i.d.\ and $\boldX_s$
 * is an average of $n_s$ vectors. The average covariance is \[\boldSigma_\rmX =
 * \frac{1}{k}\sum_{s=0}^{k-1} \boldSigma_{\rmX, s}\] and the average weighted
 * covariance is \[\boldSigma_\rmX = \frac{1}{k}\sum_{s=0}^{k-1}
 * \boldSigma_{\rmX, s}/n_s.\]
 */
public class CovFMMCallCenterStat {
   private final Map<PerformanceMeasureType, MatrixOfTallies<?>[][]> statMap = new EnumMap<PerformanceMeasureType, MatrixOfTallies<?>[][]> (
         PerformanceMeasureType.class);
   private CallCenterStatProbes stat;
   private boolean varWeighted;

   /**
    * Constructs a new group of statistical probes for covariances from the
    * inner call center statistics \texttt{stat}. If \texttt{varProp} is
    * \texttt{true}, the returned covariances correspond to the proportional
    * allocation in stratification. Otherwise, they depend on the number of
    * observations in each stratum.
    * 
    * @param stat
    *           the call center statistical object.
    * @param varWeighted
    *           the proportional allocation indicator.
    */
   public CovFMMCallCenterStat (CallCenterStatProbes stat, boolean varWeighted) {
      this.stat = stat;
      this.varWeighted = varWeighted;
      for (final PerformanceMeasureType pm : stat.getPerformanceMeasures ())
         if (pm.getEstimationType () == EstimationType.FUNCTIONOFEXPECTATIONS) {
            final MatrixOfFunctionOfMultipleMeansTallies<?> inStatC = stat
                  .getMatrixOfFunctionOfMultipleMeansTallies (pm);
            final MatrixOfTallies<?>[][] outStat = new MatrixOfTallies<?>[inStatC
                  .rows ()][inStatC.columns ()];
            for (int r = 0; r < outStat.length; r++)
               for (int c = 0; c < outStat[r].length; c++) {
                  final int d = inStatC.get (r, c).getDimension ();
                  outStat[r][c] = MatrixOfTallies.createWithTally (d, d);
               }
            statMap.put (pm, outStat);
         }
   }

   /**
    * Returns the covariance matrix for the function of multiple means tally
    * corresponding to the element \texttt{(row, col)} of the matrix of
    * performance measures \texttt{pm}. If covariances are not queried for
    * proportional allocation, the covariances are divided by the number of
    * observations in the encapsulated tallies.
    * 
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row in the matrix.
    * @param col
    *           the column in the matrix.
    * @param cov
    *           the 2D matrix filled with covariances.
    */
   public void covariance (PerformanceMeasureType pm, int row, int col,
         DoubleMatrix2D cov) {
      final MatrixOfTallies<?>[][] outStat = statMap.get (pm);
      if (outStat == null)
         throw new NoSuchElementException ("No covariance matrix for "
               + pm.toString ());
      final MatrixOfTallies<?> mt = outStat[row][col];
      mt.average (cov);
   }

   /**
    * Initializes every matrix of tallies encapsulated in this object.
    */
   public void init () {
      for (final MatrixOfTallies<?>[][] m : statMap.values ())
         for (final MatrixOfTallies<?>[] row : m)
            for (final MatrixOfTallies<?> msp : row)
               msp.init ();
   }

   /**
    * Adds new observations in each associated matrix of tallies. This method is
    * called at the end of each stratum or macroreplication and extracts the
    * covariances from the matrices of functions of multiple means tallies. It
    * then adds the covariances to the encapsulated matrices of tallies. If
    * covariances are not queried for proportionnal allocation, the covariances
    * are divided by the number of observations before they are added to the
    * tallies, resulting in a weighted sum of covariances.
    */
   public void addStat () {
      DoubleMatrix2D cov = null;
      for (final Map.Entry<PerformanceMeasureType, MatrixOfTallies<?>[][]> e : statMap
            .entrySet ()) {
         final PerformanceMeasureType pm = e.getKey ();
         final MatrixOfTallies<?>[][] m = e.getValue ();
         final MatrixOfFunctionOfMultipleMeansTallies<?> inStat = stat
               .getMatrixOfFunctionOfMultipleMeansTallies (pm);
         for (int r = 0; r < m.length; r++)
            for (int c = 0; c < m[r].length; c++) {
               final FunctionOfMultipleMeansTally ta = inStat.get (r, c);
               final int d = ta.getDimension ();
               if (cov == null || cov.rows () != d || cov.columns () != d)
                  cov = new DenseDoubleMatrix2D (d, d);
               ta.getListOfTallies ().covariance (cov);
               if (varWeighted)
                  cov.assign (Functions.div (ta.numberObs ()));
               m[r][c].add (cov);
            }
      }
   }
}
