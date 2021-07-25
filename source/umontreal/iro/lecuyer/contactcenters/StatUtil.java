package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfTallies;
import umontreal.ssj.stat.list.ListOfTalliesWithCovariance;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.ssj.util.RatioFunction;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Provides methods to add ratios
 * into lists and matrices of statistical probes as
 * well as a method to trim arrays of observations
 * in statistical probes in order to save memory.
 */
public class StatUtil {
   private StatUtil() {}

   /**
    * Equivalent to
    * {@link #addRatio(MatrixOfTallies,DoubleMatrix2D,DoubleMatrix2D,double,double) add}
    * \texttt{(mt, x, y, 1.0, Double.NaN)}.
    */
   public static void addRatio (MatrixOfTallies<?> mt, DoubleMatrix2D x,
         DoubleMatrix2D y) {
      addRatio (mt, x, y, 1.0, Double.NaN);
   }

   /**
    * Equivalent to
    * {@link #addRatio(MatrixOfTallies,DoubleMatrix2D,DoubleMatrix2D,double,double) add}
    * \texttt{(mt, x, y, mult, Double.NaN)}.
    */
   public static void addRatio (MatrixOfTallies<?> mt, DoubleMatrix2D x,
         DoubleMatrix2D y, double mult) {
      addRatio (mt, x, y, mult, Double.NaN);
   }

   /**
    * For each tally $(r,c)$ in the matrix of tallies \texttt{mt}, adds the
    * ratio \texttt{mult*x.get (r, c)/y.get (r, c)}. If a $0/0$ ratio must be
    * added, the value \texttt{zeroOverZero} is used instead of
    * {@link Double#NaN}.
    * 
    * @param mt
    *           the target matrix of tallies.
    * @param x
    *           the numerator matrix.
    * @param y
    *           the denominator matrix.
    * @param mult
    *           the multiplier of the ratio.
    * @param zeroOverZero
    *           the value for $0/0$.
    * @exception IllegalArgumentException
    *               if the dimensions of \texttt{x} or \texttt{y} do not
    *               correspond to the dimensions of the matrix of tallies.
    */
   public static void addRatio (MatrixOfTallies<?> mt, DoubleMatrix2D x,
         DoubleMatrix2D y, double mult, double zeroOverZero) {
      if (mt.rows () != x.rows () || x.rows () != y.rows ())
         throw new IllegalArgumentException (
               "The given matrices must have the same number of rows");
      if (mt.columns () != x.columns () || x.columns () != y.columns ())
         throw new IllegalArgumentException (
               "The given matrices must have the same number of columns");
      final DoubleMatrix2D temp = new DenseDoubleMatrix2D (mt.rows (), mt
            .columns ());
      for (int i = 0; i < mt.rows (); i++)
         for (int j = 0; j < mt.columns (); j++) {
            final double num = mult * x.getQuick (i, j);
            final double denom = y.getQuick (i, j);
            if (num == denom && num == 0)
               temp.setQuick (i, j, zeroOverZero);
            else
               temp.setQuick (i, j, mult * x.getQuick (i, j)
                     / y.getQuick (i, j));
         }
      mt.add (temp);
   }

   /**
    * Equivalent to
    * {@link #addRatio(ListOfTallies,double[],double[],double) add}
    * \texttt{(mt, x, y, 1.0)}.
    */
   public static void addRatio (ListOfTallies<?> mt, double[] x, double[] y) {
      addRatio (mt, x, y, 1.0);
   }

   /**
    * For each tally $i$ in the list of tallies \texttt{at}, adds the ratio
    * \texttt{mult*x[i]/y[i]}.
    * 
    * @param at
    *           the target list of tallies.
    * @param x
    *           the numerator array.
    * @param y
    *           the denominator array.
    * @param mult
    *           the multiplier of the ratio.
    * @exception IllegalArgumentException
    *               if the length of \texttt{x} or \texttt{y} do not correspond
    *               to the length of the list of tallies.
    */
   public static void addRatio (ListOfTallies<?> at, double[] x, double[] y,
         double mult) {
      if (at.size () != x.length || x.length != y.length)
         throw new IllegalArgumentException (
               "The given arrays must have the same length");
      final double[] temp = new double[at.size ()];
      for (int i = 0; i < at.size (); i++)
         temp[i] = mult * x[i] / y[i];
      at.add (temp);
   }

   /**
    * Creates a matrix of ratio tallies from two matrices of tallies. This
    * method takes two matrices which must have the same dimensions. The ratio
    * tally for row~$r$ and column~$c$ is constructed by taking the tallies at
    * corresponding row and column in \texttt{upper} and \texttt{lower}.
    * 
    * @param upper
    *           the matrix of upper parts of ratios.
    * @param lower
    *           the matrix of lower parts of ratios.
    * @return the new matrix of ratio tallies.
    */
   public static MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> createMatrixOfRatioTallies (
         MatrixOfTallies<?> upper, MatrixOfTallies<?> lower) {
      if (upper.rows () != lower.rows ()
            || upper.columns () != lower.columns ())
         throw new IllegalArgumentException (
               "The dimensions of the two matrices must be the same");
      final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta = new MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> (
            upper.rows (), lower.columns ());
      for (int row = 0; row < upper.rows (); row++)
         for (int col = 0; col < upper.columns (); col++) {
            final ListOfTalliesWithCovariance<Tally> ta = new ListOfTalliesWithCovariance<Tally> ();
            ta.add (upper.get (row, col));
            ta.add (lower.get (row, col));
            final FunctionOfMultipleMeansTally fta = new FunctionOfMultipleMeansTally (
                  new RatioFunction (), ta);
            mta.set (row, col, fta);
         }
      return mta;
   }

   /**
    * Trims the internal arrays of statistical probes listed in \texttt{probes}
    * to minimize memory utilization. For each {@link TallyStore} object in
    * \texttt{probes} or in an array or matrix added to \texttt{probes}, calls
    * {@link TallyStore#getArray}\texttt{.}{@link DoubleArrayList#trimToSize trimToSize()}.
    * 
    * @param probes
    *           the list of statistical probes to process.
    */
   public static void compactProbes (Iterable<?> probes) {
      for (final Object o : probes) {
         if (o == null)
            continue;
         if (o instanceof TallyStore) {
            final DoubleArrayList l = ((TallyStore) o).getDoubleArrayList();
            if (l != null)
               // Should not be null
               l.trimToSize ();
         }
         else if (o instanceof Iterable)
            compactProbes ((Iterable<?>)o);
      }
   }
   
   
}
