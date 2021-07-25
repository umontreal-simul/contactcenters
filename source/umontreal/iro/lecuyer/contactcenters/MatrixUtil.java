package umontreal.iro.lecuyer.contactcenters;

import cern.colt.matrix.DoubleMatrix2D;

/**
 * Contains utility methods to add rows or
 * columns to matrices, and to construct a matrix
 * by repeating a submatrix several times.
 */
public class MatrixUtil {
   private MatrixUtil() {}

   /**
    * Converts the matrix \texttt{m} into a matrix of costs using the cost
    * vector \texttt{cost}. The matrix \texttt{m} should contain counts of
    * events, e.g., the number of arrivals, or the integral over simulation time
    * of a quantity, e.g., the queue size. Each row corresponds to one count and
    * each column represents one period. Assuming the cost vector is a row
    * vector in $\RR^d$, the method computes $C*M$, and stores the result in the
    * matrix \texttt{m}. $C$ is a $d\times d$ matrix with the costs on its
    * diagonal, i.e., $C_{k, k}=$~\texttt{costs[k]}, and $C_{i, j}=0$ for $i\ne
    * j$. $M$ is a $d\times P$ matrix stored in \texttt{m}. If \texttt{m} has
    * $d+1$ rows, the last row of the matrix is filled with the total costs,
    * i.e., \[M_{d, j} = \sum_{i=0}^{d-1} M_{i, j}C_{i,i}\] for
    * $j=0,\ldots,P-1$.
    *
    * @param m
    *           the matrix of values.
    * @param cost
    *           the cost vector.
    * @exception IllegalArgumentException
    *               if the length of cost does not correspond to
    *               \texttt{m.rows()} or \texttt{m.rows() - 1}.
    * @return the given matrix \texttt{m}.
    */
   public static DoubleMatrix2D getCost (DoubleMatrix2D m, double[] cost) {
      boolean needSum;
      if (cost.length == m.rows ())
         needSum = false;
      else if (cost.length == m.rows () - 1)
         needSum = true;
      else
         throw new IllegalArgumentException (
               "Invalid matrix of values or cost vector");
      for (int c = 0; c < m.columns (); c++) {
         double sum = 0;
         for (int r = 0; r < cost.length; r++) {
            final double v = m.get (r, c) * cost[r];
            sum += v;
            m.set (r, c, v);
         }
         if (needSum)
            m.set (m.rows () - 1, c, sum);
      }
      return m;
   }

   /**
    * Equivalent to {@link #addSumRow(DoubleMatrix2D,boolean) addSumRow}
    * \texttt{(m, false)}.
    *
    * @param m
    *           the matrix being processed.
    * @return the matrix with the added row of sums.
    */
   public static DoubleMatrix2D addSumRow (DoubleMatrix2D m) {
      return addSumRow (m, false);
   }

   /**
    * Makes a copy of the matrix \texttt{m} with a new row containing the sum of
    * each column. If \texttt{m} has a single row and if \texttt{always} is set
    * to \texttt{false}, the matrix is returned unchanged. Otherwise, a new
    * matrix is created with the additional row of sums. The sums of columns
    * are stored in the last row of the returned matrix.
    *
    * @param m
    *           the matrix being processed.
    * @param always
    *           if \texttt{true}, the row is added even if \texttt{m} has one row.
    * @return the matrix with the added row of sums.
    */
   public static DoubleMatrix2D addSumRow (DoubleMatrix2D m, boolean always) {
      if (m.rows () == 0)
         return m;
      if (m.rows () == 1 && !always)
         return m;
      final DoubleMatrix2D r = m.like (m.rows () + 1, m.columns ());
      for (int j = 0; j < m.columns (); j++) {
         double total = 0;
         for (int i = 0; i < m.rows (); i++) {
            final double v = m.getQuick (i, j);
            r.setQuick (i, j, v);
            total += v;
         }
         r.setQuick (r.rows () - 1, j, total);
      }
      return r;
   }

   /**
    * Equivalent to {@link #addSumColumn(DoubleMatrix2D,boolean) addSumColumn}
    * \texttt{(m, false)}.
    *
    * @param m
    *           the matrix being processed.
    * @return the matrix with the added column of sums.
    */
   public static DoubleMatrix2D addSumColumn (DoubleMatrix2D m) {
      return addSumColumn (m, false);
   }

   /**
    * This method, similar to {@link #addSumRow(DoubleMatrix2D,boolean)}, adds
    * an extra column to the matrix \texttt{m} for the sum of each column.
    *
    * @param m
    *           the matrix being processed.
    * @param always
    *           determines if the column is always added.
    * @return the matrix with the added column of sums.
    */
   public static DoubleMatrix2D addSumColumn (DoubleMatrix2D m, boolean always) {
      if (m.columns () == 1 && !always)
         return m;
      final DoubleMatrix2D r = m.like (m.rows (), m.columns () + 1);
      for (int i = 0; i < m.rows (); i++) {
         double total = 0;
         for (int j = 0; j < m.columns (); j++) {
            final double v = m.getQuick (i, j);
            r.setQuick (i, j, v);
            total += v;
         }
         r.set (i, r.columns () - 1, total);
      }
      return r;
   }

   /**
    * Constructs a matrix by copying \texttt{m} a certain number of times. The
    * new matrix contains \texttt{numRows*numCols} copies of \texttt{m} tiled in
    * a grid with dimensions \texttt{numRows}$\times$\texttt{numCols}. If
    * \texttt{numRows} and \texttt{numCols} are both 1, the matrix is returned
    * unchanged.
    *
    * @param m
    *           the matrix to be tiled.
    * @param numRows
    *           the number of rows containing copies of \texttt{m}.
    * @param numCols
    *           the number of columns containing copies of \texttt{m}.
    * @return the matrix containing copies of \texttt{m}.
    */
   public static DoubleMatrix2D repMat (DoubleMatrix2D m, int numRows,
         int numCols) {
      if (numRows == 1 && numCols == 1)
         return m;
      final DoubleMatrix2D cpy = m.like (numRows * m.rows (), numCols
            * m.columns ());
      for (int r = 0; r < numRows; r++)
         for (int c = 0; c < numCols; c++)
            cpy.viewPart (r * m.rows (), c * m.columns (), m.rows (),
                  m.columns ()).assign (m);
      return cpy;
   }
}
