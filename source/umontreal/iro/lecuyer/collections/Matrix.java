package umontreal.iro.lecuyer.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

/**
 * Represents a two-dimensional matrix of objects.
 * Each element of a matrix can be referenced using
 * a two-dimensional index $(r, c)$, where
 * $r=0,\ldots,R-1$ is the row index, and
 * $c=0,\ldots,C-1$ is the column index.
 * Methods are specified by this
 * interface to resize matrices
 * by adding or removing rows or columns.
 * A class implementing this interface might
 * implement the {@link RandomAccess}
 * interface.
 * This means that the {@link #get}
 * operation is efficient, and
 * that the returned views
 * also implement {@link RandomAccess}. 
 * @param <E> the type of the elements.
 */
public interface Matrix<E> extends Collection<E> {
   /**
    * Returns the number of rows in this matrix.
    * @return the number of rows in this matrix.
    */
   public int rows();
   
   /**
    * Returns the number of columns in this matrix.
    * @return the number of columns in this matrix.
    */
   public int columns();
   
   /**
    * Sets the number of rows of this matrix to
    * \texttt{numRows}.
    * If \texttt{numRows} is smaller than
    * {@link #rows}, the
    * last rows of the matrix are
    * removed.
    * If \texttt{numRows} is greater than
    * {@link #rows}, new
    * rows filled with \texttt{null}
    * references are added to the matrix.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param numRows the new number of rows in the matrix.
    * @exception IllegalArgumentException if \texttt{numRows}
    * is negative.
    * @exception UnsupportedOperationException if this method is not supported.
    */
   public void setRows (int numRows);
   
   /**
    * Sets the number of columns of this matrix to
    * \texttt{numColumns}.
    * If \texttt{numColumns} is smaller than
    * {@link #columns}, the
    * last columns of the matrix are
    * removed.
    * If \texttt{numColumns} is greater than
    * {@link #columns}, new
    * columns filled with \texttt{null}
    * references are added to the matrix.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param numColumns the new number of columns in the matrix.
    * @exception IllegalArgumentException if \texttt{numColumns}
    * is negative.
    * @exception UnsupportedOperationException if this method is not supported.
    */
   public void setColumns (int numColumns);
   
   /**
    * Returns the element at index
    * (\texttt{r}, \texttt{c}) of the matrix.
    * @param r the row index.
    * @param c the column index.
    * @return the value of the element.
    * @exception IndexOutOfBoundsException if \texttt{r} or
    * \texttt{c} are negative, if \texttt{r} is
    * greater than or equal to {@link #rows},
    * or if \texttt{c} is greater than or equal to {@link #columns()}.
    */
   public E get (int r, int c);
   
   /**
    * Sets the element at index
    * (\texttt{r}, \texttt{c}) of the matrix
    * to \texttt{value}, and
    * returns the element previously at that
    * position.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param r the row index.
    * @param c the column index.
    * @param value the value of the element.
    * @return the previous value of the element.
    * @exception IndexOutOfBoundsException if \texttt{r} or
    * \texttt{c} are negative, if \texttt{r} is
    * greater than or equal to {@link #rows},
    * or if \texttt{c} is greater than or equal to {@link #columns()}.
    * @exception UnsupportedOperationException if this
    * method is not implemented.
    */
   public E set (int r, int c, E value);
   
   /**
    * Constructs and returns an iterator
    * traversing the elements of this
    * matrix rowise.
    * @return the constructed iterator.
    */
   public Iterator<E> iterator();
   
   /**
    * Returns a list view of this matrix.
    * Element \texttt{i=rC + c} of the returned
    * list must correspond to
    * element (\texttt{r}, \texttt{c})
    * of the matrix.
    * As a result, when considering a matrix
    * as a list and iterating over elements,
    * elements are enumerated rowise.
    * However, elements cannot be added or
    * removed from the returned list,
    * because the backing matrix
    * must remain rectangular.
    * One can however use {@link List#set}
    * to change elements.
    * The bahavior of the returned list
    * is undefined if the dimensions
    * of the backing matrix are changed.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @return the list view.
    * @exception UnsupportedOperationException if this
    * method is not implemented.
    */
   public List<E> asList();
   
   /**
    * Returns a list representing a view
    * of row \texttt{r} of this
    * matrix. Any change to this
    * matrix is reflected on the
    * returned list while the elements
    * of the returned list can be
    * modified using
    * {@link List#set(int, Object)}.
    * The bahavior of the returned list
    * is undefined if the dimensions
    * of the backing matrix are changed.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param r the index of the row to get a view for.
    * @return the view of the selected row.
    * @exception IndexOutOfBoundsException if \texttt{r}
    * is negative or greater than or equal to {@link #rows()}.
    * @exception UnsupportedOperationException if this
    * method is not implemented.
    */
   public List<E> viewRow (int r);
   
   /**
    * Returns a list representing a view
    * of column \texttt{c} of this
    * matrix. Any change to this
    * matrix is reflected on the
    * returned list while the elements
    * of the returned list can be
    * modified using
    * {@link List#set(int, Object)}.
    * The bahavior of the returned list
    * is undefined if the dimensions
    * of the backing matrix are changed.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param c the index of the column to get a view for.
    * @return the view of the selected column.
    * @exception IndexOutOfBoundsException if \texttt{c}
    * is negative or greater than or equal to {@link #columns()}.
    * @exception UnsupportedOperationException if this
    * method is not implemented.
    */
   public List<E> viewColumn (int c);
   
   /**
    * Returns a view of a portion of this matrix
    * containing rows
    * \texttt{fromRow} (inclusive) to
    * \texttt{toRow} (exclusive), and
    * columns
    * \texttt{fromColumn} (inclusive) to
    * \texttt{toColumn} (exclusive). 
    * The bahavior of the returned matrix
    * is undefined if the dimensions
    * of the backing matrix are changed.
    * This method is optional, and throws an
    * {@link UnsupportedOperationException} if
    * not implemented.
    * @param fromRow the starting row.
    * @param fromColumn the ending row.
    * @param toRow the starting column.
    * @param toColumn the ending column.
    * @return the view of the matrix.
    * @exception IndexOutOfBoundsException if row
    * or column indices are out of bounds.
    * @exception IllegalArgumentException if
    * \texttt{fromRow} is greater than
    * \texttt{toRow}, or
    * \texttt{fromColumn} is greater than
    * \texttt{toColumn}.
    * @exception UnsupportedOperationException if this
    * method is not implemented.
    */
   public Matrix<E> viewPart (int fromRow, int fromColumn, int toRow, int toColumn);
   
   /**
    * Returns a 2D array containing the
    * elements of this matrix in
    * the proper sequence.
    * @return the array containing the elements of this matrix.
    */
   public Object[][] to2DArray();
   
   /**
    * Returns a 2D array containing the
    * elements of this matrix in
    * the proper sequence; the
    * runtime type of the returned array
    * is the same as the runtime type
    * of the given array.
    * @param array the array to use.
    * @return the array containing the elements of this matrix.
    */
   public E[][] to2DArray (E[][] array);
   
   /**
    * Compares the specified object with this
    * matrix for equality.
    * Returns \texttt{true} if and only if
    * the specified object is also a
    * matrix, both matrices have
    * the same dimensions, and all
    * corresponding pairs of
    * elements in the two matrices
    * are equal.
    * (Two elements \texttt{e1} and \texttt{e2}
    * are equal if \texttt{(e1 == null ? e2 == null : e1.equals (e2))}.)
    * In other words, two matrices are defined
    * to be equal if they contain the same
    * elements in the same order.
    * This definition ensures that the \texttt{equals}
    * method works properly across different
    * implementations of the \texttt{Matrix} interface.
    */
   public boolean equals (Object o);
   
   /**
    * Returns the hash code value for the matrix.
    * The hash code of a matrix is defined to be
    * $31*(31*R + C) + H$, where $H$
    * is the hash code of the list
    * view returned by {@link #asList}.
    * This ensures that \texttt{matrix1.equals (matrix2)}
    * implies that \texttt{matrix1.hashCode() == matrix2.hashCode()}
    * for any two matrices,
    * \texttt{matrix1} and \texttt{matrix2},
    * as required by the general contract of
    * \texttt{Object.hashCode}.
    */
   public int hashCode();
}
