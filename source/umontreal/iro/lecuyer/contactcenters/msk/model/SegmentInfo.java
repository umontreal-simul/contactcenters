package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Collection;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

import umontreal.iro.lecuyer.contactcenters.msk.params.SegmentParams;

import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.NamedInfo;

/**
 * Represents information about a user-defined segment regrouping some indexed
 * entities such as call types, agent groups, or periods. Each segment has a
 * name, optional user-defined properties, and a list of indices.
 * 
 * Segment information is extracted from a {@link SegmentParams} instance which
 * is read from a XML file by JAXB. The method {@link #getValues()} can be used
 * to obtain the indices regrouped by the segment, while
 * {@link #containsValue(int)} tests if a specific index is contained in the
 * segment.
 */
public class SegmentInfo extends NamedInfo {
   private int[] values;
   private int minValue, maxValue, numValues;
   private boolean[] valueSet;

   /**
    * Constructs a new segment information object from the segment parameters
    * \texttt{par}.
    * 
    * @param par
    *           the segment parameters.
    * @exception IllegalArgumentException
    *               if some segment parameters wre invalid.
    */
   public SegmentInfo (SegmentParams par) {
      super (par);
      if (par.isSetValues ()) {
         values = new int[par.getValues ().size ()];
         int idx = 0;
         for (final Integer v : par.getValues ())
            values[idx++] = v;
      }
      else
         values = new int[0];
      minValue = ArrayUtil.min (values);
      maxValue = ArrayUtil.max (values);
      if (maxValue < 0)
         throw new IllegalArgumentException (
               "The indices for the segment parameters are all negative");
      valueSet = new boolean[maxValue + 1];
      for (final int value : values) {
         if (value < 0)
            throw new IllegalArgumentException ("The value " + value
                  + " of the segment parameters is negative");
         if (!valueSet[value]) {
            valueSet[value] = true;
            ++numValues;
         }
      }
   }

   /**
    * Returns the reference to an array containing the list of values in this
    * segment. The returned array can be modified without affecting the internal
    * array in this object.
    * 
    * @return the list of indices.
    */
   public int[] getValues () {
      return values.clone ();
   }

   /**
    * Returns the number of different values in this segment.
    * 
    * @return the number of values.
    */
   public int getNumValues () {
      return numValues;
   }

   /**
    * Returns the minimal index in this segment.
    * 
    * @return the minimal index.
    */
   public int getMinValue () {
      return minValue;
   }

   /**
    * Returns the maximal index in this segment.
    * 
    * @return the maximal index.
    */
   public int getMaxValue () {
      return maxValue;
   }

   /**
    * Tests if the index \texttt{i} is included in the list of values associated
    * with this segment. Returns \texttt{true} if and only if \texttt{i} is
    * included in the list of values returned by {@link #getValues()}.
    * 
    * @param i
    *           the tested index.
    * @return the success indicator of the test.
    */
   public boolean containsValue (int i) {
      if (i >= valueSet.length)
         return false;
      return valueSet[i];
   }

   /**
    * Converts the given collection of segment parameters into an array of
    * segment information objects. This method first creates an array of segment
    * information objects whose length corresponds to the size of the given
    * collection. It then iterates over the collection, and creates one
    * information object for each parameter object in the collection. The
    * constructed array is then returned.
    * 
    * @param par
    *           collection of segment parameters.
    * @return the corresponding array of segment information objects.
    * @exception IllegalArgumentException
    *               if an error occurs during the creation of a segment
    *               information object.
    */
   public static SegmentInfo[] getSegments (
         Collection<? extends SegmentParams> par) {
      final SegmentInfo[] res = new SegmentInfo[par.size ()];
      int idx = 0;
      for (final SegmentParams spar : par) {
         try {
            res[idx] = new SegmentInfo (spar);
            ++idx;
         }
         catch (IllegalArgumentException iae) {
            IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Error parsing segment information for user-defined segment with index "
                        + idx);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      return res;
   }

   /**
    * Checks that the minimal value stored in all the segments \texttt{segments}
    * is greater than or equal to \texttt{lower}, and the maximal value is
    * smaller than \texttt{upper}. If this condition is violated for at least
    * one segment, an illegal-argument exception is thrown. This method is used
    * for validating parameters when the call center model is constructed. For
    * example, it is used to ensure that segments of inbound call types does not
    * contain any value greater than or equal to $\Ki$.
    * 
    * @param lower
    *           the lower bound (inclusive).
    * @param upper
    *           the upper bount (non-inclusive).
    * @param segments
    *           the array of segments to test.
    * @exception IllegalArgumentException
    *               if at least one segment contains an out-of-bounds value.
    */
   public static void checkRange (int lower, int upper, SegmentInfo... segments) {
      int idx = 0;
      for (final SegmentInfo seg : segments) {
         if (seg.getMinValue () < lower)
            throw new IllegalArgumentException ("The segment with index " + idx
                  + " contains at least one value smaller than " + lower);
         if (seg.getMaxValue () >= upper)
            throw new IllegalArgumentException ("The segment with index " + idx
                  + " contains at least one index greater than or equal to "
                  + upper);
         ++idx;
      }
   }

   /**
    * Calls
    * {@link #addRowSegments(DoubleMatrix2D,DoubleDoubleFunction,boolean[],SegmentInfo...)
    * add\-Row\-Segments} \texttt{(mat, func, null, segments)}.
    * 
    * @param mat
    *           the matrix to process.
    * @param func
    *           the function $f$.
    * @param segments
    *           the segments for which rows are added in the matrix.
    * @return the matrix with extra rows.
    */
   public static DoubleMatrix2D addRowSegments (DoubleMatrix2D mat,
         DoubleDoubleFunction func, SegmentInfo... segments) {
      return addRowSegments (mat, func, null, segments);
   }

   /**
    * Constructs and returns a matrix with all the rows in \texttt{mat}, extra
    * rows corresponding to the segments in \texttt{segments}, and an additional
    * row representing the aggregation of all rows in the original matrix. Let
    * \texttt{mat} be a $a\times b$ matrix. If $a\le 1$, the method returns
    * \texttt{mat} unchanged. Otherwise, it creates a new matrix $M$ with $a + s +
    * 1$ rows and $b$ columns, where $s$ is the length of the \texttt{segments}
    * array. Let $m_{i,j}$ be the element in \texttt{mat} at position $(i,j)$,
    * for $i=0,\ldots,a-1$ and $j=0,\ldots,b-1$, and let $M_{i,j}$ be an element
    * in the resulting matrix, with $i=0,\ldots,a+s$, and $j=0,\ldots,b-1$.
    * Then, for any $j=0,\ldots, b-1$, \[ M_{i,j}=\left\{\begin{array}{ll}
    * f(0,m_{i,j}) &\mbox{ for }i=0,\ldots,a-1,\\ f_{l=0}^{a-1}
    * m_{l,j}s_{i-a,l}&\mbox{ for }i=a,\ldots,a+s, \end{array}\right. \] where \[
    * f_{i=a}^b x_is_i=\left\{\begin{array}{ll} f(f_{i=a}^{b-1} x_is_i, x_b) &
    * \mbox{ if }a<b\mbox{ and }s_b=1,\\ f_{i=a}^{b-1} x_is_i & \mbox{ if }a<b\mbox{
    * and }s_b=0,\\ f(0, x_a) & \mbox{ if }a=b\mbox{ and }s_a=1,\\ 0 & \mbox{
    * otherwise}. \end{array}\right. \] Here, $f:\RR^2\to\RR$ is a function, and
    * $s_{r,i}=1$ if index $i$ is included in the $r$th segment, and 0
    * otherwise. For $r=0,\ldots,s-1$, $s_{r,i}$ is determined using the
    * {@link #containsValue(int)} method of \texttt{segments[r]} while $s_{s,i}$
    * is 1 if and only if \texttt{globalSegmentValues[i]} is \texttt{true}. If
    * \texttt{globalSegmentValues} is \texttt{null}, $s_{s,i}$ is set to 1 for
    * all $i$ in the last row.
    * 
    * Usually, \texttt{func} which represents $f$ is set to
    * {@link Functions#plus}, or {@link Functions#max}. In the former case,
    * $f(x,y)=x+y$, and $M_{i,j}$ is \[\sum_{l=0}^{a-1} m_{l,j}s_{i-a,l}.\]
    * 
    * @param mat
    *           the matrix to process.
    * @param func
    *           the function $f$.
    * @param globalSegmentValues
    *           determines which rows are summed up in the global segment.
    * @param segments
    *           the segments for which rows are added in the matrix.
    * @return the matrix with extra rows.
    */
   public static DoubleMatrix2D addRowSegments (DoubleMatrix2D mat,
         DoubleDoubleFunction func, boolean[] globalSegmentValues,
         SegmentInfo... segments) {
      final int rows = mat.rows ();
      if (rows <= 1)
         return mat;
      final int nseg = segments.length;

      final DoubleMatrix2D res = mat.like (rows + 1 + nseg, mat.columns ());
      res.viewPart (0, 0, rows, mat.columns ()).assign (mat, func);

      for (int seg = 0; seg <= nseg; seg++) {
         final SegmentInfo sinfo = seg < nseg ? segments[seg] : null;
         for (int i = 0; i < rows; i++) {
            if (sinfo == null && globalSegmentValues != null
                  && !globalSegmentValues[i])
               continue;
            if (sinfo != null && !sinfo.containsValue (i))
               continue;
            res.viewRow (rows + seg).assign (mat.viewRow (i), func);
         }
      }

      return res;
   }

   /**
    * Constructs and returns a matrix with all the rows in \texttt{mat}, and
    * extra rows corresponding to the segments in \texttt{segments1} and
    * \texttt{segments2}. Let \texttt{mat} be a $(a*c)\times b$ matrix. If
    * $a*c\le 1$, the method returns \texttt{mat} unchanged. Otherwise, it
    * creates a new matrix $M$ with $(a + s_1 + 1)(c + s_2 + 1)$ rows and $b$
    * columns, where $s_1$ and $s_2$ are the lengths of the \texttt{segments1}
    * and \texttt{segments2} arrays, respectively. Let $m_{i,j,p}$ be element at
    * row $i*c+j$ and column $p$ in \texttt{mat}, with $i=0,\ldots,a-1$ and
    * $j=0,\ldots,c-1$, and $p=0,\ldots,b-1$. Also let $M_{i,j,p}$ be element at
    * row $i*(c+s_2-1)+j$ and column $p$ in $M$, with $i=0,\ldots,a+s_1$, and
    * $j=0,\ldots,c+s_2$. Then, \[ M_{i,j,p}=\left\{\begin{array}{ll}
    * f(0,m_{i,j,p}) &\mbox{ if }i<a\mbox{ and }j < c,\\
    * f_{l=0}^{c-1}m_{i,l,p}s_{2,j-c,l} &\mbox{ if }i<a\mbox{ and
    * }j=c,\ldots,c+s_2,\\ f_{l=0}^{a-1}m_{l,j,p}s_{1,i-a,l} &\mbox{ if
    * }i=a,\ldots,a+s_1\mbox{ and}j<c,\\
    * f_{l_1=0}^{a-1}(f_{l_2=0}^{c-1}m_{l_1,l_2,p}s_{2,j-c,l_2})s_{1,i-a,l_1}&
    * \mbox{ otherwise}. \end{array}\right. \] Here, $s_{d,r,i}$ is 1 if and
    * only if segment $r$ in dimension $d$ contains element $i$, for $d=1,2$. In
    * particular, $s_{1,r,i}$, for $r=0,\ldots,s_1-1$, is determined using
    * \texttt{segments1[r]} while $s_{2,r,i}$, for $r=0,\ldots,s_2-1$, is set
    * using \texttt{segments2[r]}. The variables $s_{1,s_1,i}$ and $s_{2,s_2,i}$
    * are set using \texttt{globalSegmentValues1[i]} and
    * texttt{globalSegmentValues2[i]} respectively, or 1 if the corresponding
    * array is \texttt{null}. The definition of $f_{i=a}^b x_is_i$ is the same
    * as in method
    * {@link #addRowSegments(DoubleMatrix2D,DoubleDoubleFunction,boolean[],SegmentInfo...)}.
    * 
    * @param mat
    *           the matrix to process.
    * @param numGroups
    *           the value of $c$, $a$ being determined using \texttt{mat}.
    * @param func
    *           the function $f$.
    * @param globalSegmentValues1
    *           determines which rows are summed up in the global segment for
    *           the first dimension.
    * @param globalSegmentValues2
    *           determines which rows are summed up in the global segment for
    *           the second dimension.
    * @param segments1
    *           the segments for which rows are added in the matrix, for the
    *           first dimension.
    * @param segments2
    *           the segments for which rows are added in the matrix, for the
    *           second dimension.
    * @return the matrix with extra rows.
    */
   public static DoubleMatrix2D addRowSegments (DoubleMatrix2D mat,
         int numGroups, DoubleDoubleFunction func,
         boolean[] globalSegmentValues1, boolean[] globalSegmentValues2,
         SegmentInfo[] segments1, SegmentInfo[] segments2) {
      final int rows = mat.rows ();
      final int columns = mat.columns ();
      if (rows <= 1 || columns == 0)
         return mat;
      if (numGroups <= 0)
         throw new IllegalArgumentException ("numGroups must be positive");
      if (numGroups == 1)
         return addRowSegments (mat, func, globalSegmentValues1, segments1);
      final int numTypes = rows / numGroups;
      if (rows % numGroups != 0)
         throw new IllegalArgumentException (
               "The number of rows in mat must be a multiple of the number of groups");
      if (numTypes == 1)
         return addRowSegments (mat, func, globalSegmentValues2, segments2);

      final int nseg1 = segments1.length;
      final int nseg2 = segments2.length;

      final DoubleMatrix2D res = mat.like ((numTypes + 1 + nseg1)
            * (numGroups + 1 + nseg2), columns);
      for (int i = 0; i < numTypes; i++)
         for (int j = 0; j < numGroups; j++) {
            final int idxSrc = numGroups * i + j;
            for (int k = -1; k <= nseg1; k++) {
               final boolean inSeg1;
               final int ii;
               if (k == -1) {
                  inSeg1 = true;
                  ii = i;
               }
               else if (k == nseg1) {
                  inSeg1 = globalSegmentValues1 == null ? true
                        : globalSegmentValues1[i];
                  ii = numTypes + nseg1;
               }
               else {
                  inSeg1 = segments1[k].containsValue (i);
                  ii = numTypes + k;
               }
               if (!inSeg1)
                  continue;
               for (int l = -1; l <= nseg2; l++) {
                  final boolean inSeg2;
                  final int jj;
                  if (l == -1) {
                     inSeg2 = true;
                     jj = j;
                  }
                  else if (l == nseg2) {
                     inSeg2 = globalSegmentValues2 == null ? true
                           : globalSegmentValues2[j];
                     jj = numGroups + nseg2;
                  }
                  else {
                     inSeg2 = segments2[l].containsValue (j);
                     jj = numGroups + l;
                  }
                  if (!inSeg2)
                     continue;
                  final int idxRes = (numGroups + nseg2 + 1) * ii + jj;
                  res.viewRow (idxRes).assign (mat.viewRow (idxSrc), func);
               }
            }
         }

      return res;
   }

   /**
    * Similar to
    * {@link #addRowSegments(DoubleMatrix2D, DoubleDoubleFunction,SegmentInfo...)},
    * for adding extra columns to matrix \texttt{mat}.
    * 
    * @param mat
    *           the matrix to process.
    * @param func
    *           the function $f$.
    * @param segments
    *           the segments for which rows are added in the matrix.
    * @return the matrix with extra rows.
    */
   public static DoubleMatrix2D addColumnSegments (DoubleMatrix2D mat,
         DoubleDoubleFunction func, SegmentInfo... segments) {
      final int columns = mat.columns ();
      if (columns <= 1)
         return mat;
      final int nseg = segments.length;

      final DoubleMatrix2D res = mat.like (mat.rows (), columns + 1 + nseg);
      res.viewPart (0, 0, mat.rows (), columns).assign (mat, func);

      for (int seg = 0; seg <= nseg; seg++) {
         final SegmentInfo sinfo = seg < nseg ? segments[seg] : null;
         for (int mp = 0; mp < columns; mp++)
            if (sinfo == null || sinfo.containsValue (mp))
               res.viewColumn (columns + seg)
                     .assign (mat.viewColumn (mp), func);
      }

      return res;
   }
}
