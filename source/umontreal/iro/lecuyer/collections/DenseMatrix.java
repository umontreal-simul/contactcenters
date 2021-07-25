package umontreal.iro.lecuyer.collections;

import java.io.Serializable;
import java.util.RandomAccess;

/**
 * Represents a matrix storing elements in an array.
 * @param <E> the element type.
 */
public class DenseMatrix<E> extends AbstractMatrix<E> implements Cloneable, Serializable, RandomAccess {
   private static final long serialVersionUID = -162579649651016267L;
   private int rows;
   private int columns;
   private E[] elements;
   
   /**
    * Constructs a new dense matrix with
    * \texttt{rows} rows, \texttt{columns}
    * columns, and filled with
    * \texttt{null} elements.
    * @param rows the number of rows.
    * @param columns the number of columns.
    */
   @SuppressWarnings("unchecked")
   public DenseMatrix (int rows, int columns) {
      if (rows < 0 || columns < 0)
         throw new IllegalArgumentException
         ("rows and columns must not be negative");
      this.rows = rows;
      this.columns = columns;
      elements = (E[])new Object[rows*columns];
   }
   
   /**
    * Constructs a new matrix from the 2D
    * array \texttt{elements}.
    * @param elements the elements of the matrix.
    */
   @SuppressWarnings("unchecked")
   public DenseMatrix (E[][] elements) {
      rows = elements.length;
      if (rows == 0) {
         this.columns = 0;
         this.elements = (E[])new Object[0];
      }
      else {
         columns = elements[0].length;
         if (columns == 0)
            this.elements = (E[])new Object[0];
         else {
            this.elements = (E[])new Object[rows*columns];
            for (int r = 0; r < rows; r++) {
               if (elements[r].length != columns)
                  throw new IllegalArgumentException
                  ("The given 2D array is not rectangular");
               System.arraycopy (elements[r], 0, this.elements, r*columns, columns);
            }
         }
      }
   }
   
   /**
    * Constructs a new matrix from the
    * matrix \texttt{matrix}.
    * @param matrix the source matrix.
    */
   public DenseMatrix (Matrix<? extends E> matrix) {
      this (matrix.rows (), matrix.columns ());
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++)
            elements[r*columns + c] = matrix.get (r, c);
   }

   public int columns () {
      return columns;
   }
   
   public int rows () {
      return rows;
   }

   public E get (int r, int c) {
      if (r < 0 || c < 0)
         throw new IndexOutOfBoundsException
         ("r or c are negative");
      if (r >= rows || c >= columns)
         throw new IndexOutOfBoundsException
         ("r or c are greater than or equal to the number of rows or columns");
      return elements[r*columns + c];
   }

   @SuppressWarnings("unchecked")
   public void setColumns (int numColumns) {
      if (numColumns < 0)
         throw new IllegalArgumentException
            ("The given number of columns is negative");
      if (columns() == numColumns)
         return;
      final E[] newElements = (E[])new Object[rows()*numColumns];
      final int m = Math.min (columns(), numColumns);
      for (int r = 0; r < rows(); r++)
         System.arraycopy (elements, columns()*r, newElements, numColumns*r, m);
      elements = newElements;
      columns = numColumns;
      ++modCount;
   }

   @SuppressWarnings("unchecked")
   public void setRows (int numRows) {
      if (numRows < 0)
         throw new NegativeArraySizeException
            ("The given number of rows is negative");
      if (rows() == numRows)
         return;
      final E[] newElements = (E[])new Object[numRows*columns()];
      final int m = Math.min (rows(), numRows);
      System.arraycopy (elements, 0, newElements, 0, m*columns());
      elements = newElements;
      rows = numRows;
      ++modCount;
   }

   @Override
   public E set (int r, int c, E value) {
      if (r < 0 || c < 0)
         throw new IndexOutOfBoundsException
         ("r or c are negative");
      if (r >= rows || c >= columns)
         throw new IndexOutOfBoundsException
         ("r or c are greater than or equal to the number of rows or columns");
      final int idx = columns*r + c;
      final E old = elements[idx];
      elements[idx] = value;
      return old;
   }


   @SuppressWarnings("unchecked")
   @Override
   protected DenseMatrix<E> clone () {
      DenseMatrix<E> cpy;
      try {
         cpy = (DenseMatrix<E>)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("CloneNotSupportedException for a class implementing Cloneable");
      }
      cpy.elements = elements.clone ();
      return cpy;
   }
}
