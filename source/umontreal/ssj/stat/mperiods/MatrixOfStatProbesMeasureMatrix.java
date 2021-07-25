package umontreal.ssj.stat.mperiods;

import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;

/**
 * Matrix of measures whose values are obtained using a matrix of statistical
 * probes. This matrix contains one measure for each row of the matrix, and one
 * period for each column. The measures are obtained using the
 * {@link StatProbe#sum} method. Since the sum can be considered as an integral,
 * the {@link IntegralMeasureMatrix} can be used to turn this matrix into a
 * multiple-periods one if the associated matrix contains a single column.
 */
public class MatrixOfStatProbesMeasureMatrix implements MeasureMatrix,
      Cloneable {
   private MatrixOfStatProbes<?> matrix;

   /**
    * Constructs a new matrix of measures using the matrix of probes
    * \texttt{matrix}.
    *
    * @param matrix
    *           the matrix of statistical probes being used.
    */
   public MatrixOfStatProbesMeasureMatrix (MatrixOfStatProbes<?> matrix) {
      this.matrix = matrix;
   }

   /**
    * Returns the matrix of statistical probes associated with this matrix of
    * measures.
    *
    * @return the associated matrix of statistical probes.
    */
   public MatrixOfStatProbes<?> getMatrixOfStatProbes () {
      return matrix;
   }

   /**
    * Sets the associated matrix of statistical probes to \texttt{matrix}. If
    * the given matrix is \texttt{null}, the number of measures and periods are
    * set to 0. Otherwise, they correspond to the number of rows and columns of
    * the matrix, respectively.
    *
    * @param matrix
    *           the new matrix of statistical probes.
    */
   public void setMatrixOfStatProbes (MatrixOfStatProbes<?> matrix) {
      this.matrix = matrix;
   }

   public void init () {}

   public int getNumMeasures () {
      return matrix == null ? 0 : matrix.rows ();
   }

   public int getNumPeriods () {
      return matrix == null ? 0 : matrix.columns ();
   }

   public void setNumMeasures (int nm) {
      matrix.setRows (nm);
   }

   public void setNumPeriods (int np) {
      matrix.setColumns (np);
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   public double getMeasure (int i, int p) {
      if (matrix == null)
         throw new IndexOutOfBoundsException ("Invalid measure or period index");
      return matrix.get (i, p).sum ();
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      if (matrix == null)
         sb.append ("no associated matrix of probes");
      else
         sb.append ("associated matrix of probes: " + matrix.toString ());
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Makes a copy of this matrix of measures. The statistical probe matrix is
    * not cloned.
    *
    * @return a clone of this instance.
    */
   @Override
   public MatrixOfStatProbesMeasureMatrix clone () {
      try {
         return (MatrixOfStatProbesMeasureMatrix) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }
}
