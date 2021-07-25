package umontreal.ssj.stat.mperiods;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of related measures computed using different measure
 * matrices. Each measure of such a set corresponds to a measure computed by
 * another matrix. For example, this class can regroup the queue size for
 * different waiting queues. It can compute the sum of the measures for each
 * period, and give statistical collecting mechanisms access to the measures
 * using the {@link MeasureMatrix} interface.
 */
public class MeasureSet implements MeasureMatrix, Cloneable {
   /**
    * List of measure information objects.
    */
   private List<MeasureInfo> measures = new ArrayList<MeasureInfo> ();
   /**
    * Determines if the sum row is computed, default being \texttt{true}.
    */
   private boolean computeSum = true;

   /**
    * Constructs a new empty measure set. The {@link #addMeasure} method must be
    * used to add some measures.
    */
   public MeasureSet () {}

   /**
    * Determines if the measure set contains an additional row containing the
    * sum of each column. If this returns \texttt{true} (the default), the row
    * of sums is computed. Otherwise, it is not computed. The sum row adds one
    * additional measure to the measure set only if the number of measures is
    * greater than 1.
    *
    * @return the sum row computing indicator.
    */
   public boolean isComputingSumRow () {
      return computeSum;
   }

   /**
    * Sets the computing sum row indicator to \texttt{b}. See
    * {@link #isComputingSumRow} for more information.
    *
    * @param b
    *           the new sum row computing indicator.
    */
   public void setComputingSumRow (boolean b) {
      computeSum = b;
   }

   /**
    * Adds the measure \texttt{imat} calculated by \texttt{mat} to this set of
    * measures. It is recommended that every added measure matrix has the same
    * number of periods.
    *
    * @param mat
    *           the measure matrix computing the added measure.
    * @param imat
    *           the index of the added measure, in \texttt{mat}.
    * @exception NullPointerException
    *               if \texttt{mat} is \texttt{null}.
    */
   public void addMeasure (MeasureMatrix mat, int imat) {
      final MeasureInfo info = new MeasureInfo (mat, imat);
      measures.add (info);
   }

   /**
    * Clears all measures contained in this set.
    */
   public void clearMeasures () {
      measures.clear ();
   }

   /**
    * Returns the measure information object for measure \texttt{i}.
    *
    * @return the measure information object.
    * @exception IndexOutOfBoundsException
    *               if \texttt{i} is out of bounds.
    */
   public MeasureInfo getMeasureInfo (int i) {
      return measures.get (i);
   }

   /**
    * Returns the number of supported measures. If the set contains 0 or 1
    * measure, this method returns 0 or 1, respectively. If the set contains
    * $n>1$ measures, $n+1$ is returned if {@link #isComputingSumRow} returns
    * \texttt{true}, or $n$ is returned otherwise.
    *
    * @return the number of supported measures.
    */
   public int getNumMeasures () {
      final int nm = measures.size ();
      return nm > 1 ? nm + (computeSum ? 1 : 0) : nm;
   }

   /**
    * Returns the number of supported periods. If the set is empty, this returns
    * 0. Otherwise, this returns the maximal number of periods of the contained
    * measure matrices.
    *
    * @return the supported number of periods.
    */
   public int getNumPeriods () {
      int np = 0;
      for (int i = 0; i < measures.size (); i++) {
         final MeasureInfo info = measures.get (i);
         final int tnp = info.mat.getNumPeriods ();
         if (tnp > np)
            np = tnp;
      }
      return np;
   }

   /**
    * This implementation does not support changing the number of measures.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ();
   }

   /**
    * This implementation does not support changing the number of periods.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ();
   }

   /**
    * This implementation does not support period regrouping.
    *
    * @exception UnsupportedOperationException
    *               if this method is called.
    */
   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   /**
    * This method does nothing in this implementation.
    */
   public void init () {}

   /**
    * Returns the measure~\texttt{i} in period~\texttt{p} for this matrix. Let
    * $n$ be the number of measures in this set, i.e., the value of
    * {@link #getNumMeasures} if {@link #isComputingSumRow} returns
    * \texttt{false}. If $i < n$, this returns the $i$th measure added to this
    * set. If $i = n$, $n>1$ and the measure set is computing the sum row, this
    * returns the sum of all the contained measures for period \texttt{p}. Let
    * $P$ be the number of periods as returned by {@link #getNumPeriods}. If
    * \texttt{p} is greater than or equal to the number of periods in the
    * queried measure matrix but smaller than $P$, {@link Double#NaN} is
    * returned. In the sum of measures, the NaN value is not counted to avoid a
    * NaN sum.
    *
    * @param i
    *           the index of the measure.
    * @param p
    *           the index of the period.
    * @return the value of the measure.
    * @exception IndexOutOfBoundsException
    *               if the measure or period indices are out of bounds.
    */
   public double getMeasure (int i, int p) {
      if (i < 0 || measures.isEmpty ())
         throw new IndexOutOfBoundsException ("Invalid measure index: " + i);
      if (i < measures.size ()) {
         final MeasureInfo info = measures.get (i);
         final int np = info.mat.getNumPeriods ();
         if (p >= np) {
            final int tnp = getNumPeriods ();
            if (p >= tnp)
               throw new IllegalArgumentException ("Invalid period index: " + p);
            return Double.NaN;
         }
         return info.mat.getMeasure (info.index, p);
      }
      else if (i == measures.size () && computeSum && measures.size () > 1) {
         double res = 0;
         boolean oneTerm = false;
         for (int m = 0; m < measures.size (); m++) {
            final MeasureInfo info = measures.get (m);
            if (p >= info.mat.getNumPeriods ())
               continue;
            res += info.mat.getMeasure (info.index, p);
            oneTerm = true;
         }
         if (!oneTerm)
            throw new IllegalArgumentException ("Invalid period index: " + p);
         return res;
      }
      else
         throw new IndexOutOfBoundsException ("Invalid measure index: " + i);
   }

   /**
    * Contains information about a measure added to a measure set.
    */
   public static final class MeasureInfo implements Cloneable {
      /**
       * Measure matrix.
       */
      MeasureMatrix mat;
      /**
       * Index of the measure.
       */
      int index;

      /**
       * Constructs a new measure information object for the
       * measure~\texttt{index} in the matrix~\texttt{mat}.
       *
       * @param mat
       *           the measure matrix to take the measure from.
       * @param index
       *           the index of the measure, in \texttt{mat}.
       * @exception NullPointerException
       *               if \texttt{mat} is \texttt{null}.
       */
      public MeasureInfo (MeasureMatrix mat, int index) {
         if (mat == null)
            throw new NullPointerException ();
         this.mat = mat;
         this.index = index;
      }

      /**
       * Returns the measure matrix from which the measure is extracted.
       *
       * @return the associated measure matrix.
       */
      public MeasureMatrix getMeasureMatrix () {
         return mat;
      }

      /**
       * Returns the index, in the associated measure matrix, of the represented
       * measure.
       *
       * @return the index of the measure.
       */
      public int getMeasureIndex () {
         return index;
      }

      @Override
      public boolean equals (Object o) {
         if (!(o instanceof MeasureInfo))
            return false;
         final MeasureInfo other = (MeasureInfo) o;
         return mat == other.mat && index == other.index;
      }

      @Override
      public int hashCode () {
         return mat.hashCode () + index;
      }

      @Override
      public String toString () {
         final StringBuilder sb = new StringBuilder (getClass ().getName ());
         sb.append ('[');
         sb.append ("matrix: ").append (mat.toString ()).append (", ");
         sb.append ("index: ").append (index);
         sb.append (']');
         return sb.toString ();
      }

      @Override
      public MeasureInfo clone () {
         try {
            return (MeasureInfo) super.clone ();
         }
         catch (final CloneNotSupportedException cne) {
            throw new IllegalStateException ("Clone not supported");
         }
      }
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append ('[');
      sb.append ("number of regrouped measures: ").append (measures.size ());
      sb.append (", ");
      if (computeSum)
         sb.append ("computing sum row");
      else
         sb.append ("not computing sum row");
      sb.append (']');
      return sb.toString ();
   }

   @Override
   public MeasureSet clone () {
      try {
         final MeasureSet mset = (MeasureSet) super.clone ();
         mset.measures = new ArrayList<MeasureInfo> (measures);
         return mset;
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
   }
}
