package umontreal.iro.lecuyer.collections;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * Provides default implementation for most
 * methods of the {@link Matrix} interface.  
 *
 * @param <E> the type of the elements.
 */
public abstract class AbstractMatrix<E> extends AbstractCollection<E> implements Matrix<E> {
   /**
    * This must be incremented each time {@link Matrix#setRows}
    * or {@link Matrix#setColumns} modify
    * the number of rows or columns.
    */
   protected int modCount = 0;

   /**
    * Returns a list using
    * {@link #size} to get the
    * number of elements, and
    * {@link #get} to access elements.
    */
   public List<E> asList() {
      if (this instanceof RandomAccess)
         return new RandomAccessMatrixList();
      return new MatrixList();
   }
   
   /**
    * Returns a list using
    * {@link #columns} to get the
    * number of elements, and
    * {@link #get} to access elements.
    */
   public List<E> viewRow (int r) {
      if (this instanceof RandomAccess)
         new RandomAccessRowView (r);
      return new RowView (r);
   }
   
   /**
    * Returns a list using
    * {@link #rows} to get the
    * number of elements, and
    * {@link #get} to access elements.
    */
   public List<E> viewColumn (int c) {
      if (this instanceof RandomAccess)
         new RandomAccessColumnView (c);
      return new ColumnView (c);
   }

   public Matrix<E> viewPart (int fromRow, int fromColumn, int toRow, int toColumn) {
      if (this instanceof RandomAccess)
         return new RandomAccessSubMatrix<E> (this, fromRow, fromColumn, toRow, toColumn);
      return new SubMatrix<E> (this, fromRow, fromColumn, toRow, toColumn);
   }
   
   public E set (int r, int c, E value) {
      throw new UnsupportedOperationException();
   }
   
   /**
    * Returns the product of {@link Matrix#rows}
    * and {@link Matrix#columns}.
    */
   @Override
   public int size() {
      return rows()*columns();
   }
   
   /**
    * Returns \texttt{true} if
    * {@link Matrix#rows} or
    * {@link Matrix#columns} return 0.
    */
   @Override
   public boolean isEmpty() {
      return rows() == 0 || columns() == 0;
   }
   
   @Override
   public Iterator<E> iterator() {
      return new MyIterator();
   }
   
   @Override
   public boolean equals (Object o) {
      if (!(o instanceof Matrix))
         return false;
      final Matrix<?> m = (Matrix<?>)o;
      final int rows = rows();
      final int columns = columns();
      if (rows != m.rows ())
         return false;
      if (columns != m.columns ())
         return false;
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++) {
            final Object e1 = get (r, c);
            final Object e2 = m.get (r, c);
            boolean eq = e1 == null ? e2 == null : e1.equals (e2);
            if (!eq)
               return false;
         }
      return true;
   }
   
   @Override
   public int hashCode () {
      final int rows = rows();
      final int columns = columns();
      int listHashcode = 1;
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++) {
            final E e = get (r, c);
            final int h = e == null ? 0 : e.hashCode ();
            listHashcode = 31*listHashcode + h;
         }      
      final int hashcode = 31*rows + columns;
      return 31*hashcode + listHashcode;
   }
   
   @Override
   public String toString () {
      if (isEmpty())
         return "Empty matrix";
      final StringBuilder sb = new StringBuilder();
      final int rows = rows();
      final int columns = columns();
      sb.append ('[');
      for (int r = 0; r < rows; r++) {
         if (r > 0)
            sb.append (", ");
         sb.append ('[');
         for (int c = 0; c < columns; c++) {
            final E e = get (r, c);
            sb.append (c > 0 ? ", " : "").append
            (e == null ? "null" : e.toString ());
         }
         sb.append (']');
      }
      sb.append (']');
      return sb.toString ();
   }
   
   @SuppressWarnings("unchecked")
   public E[][] to2DArray (E[][] array) {
      final Class<?> componentType = array.getClass ().getComponentType ().getComponentType ();
      E[][] arr;
      final int rows = rows();
      final int columns = columns();
      if (array.length == rows) {
         for (int r = 0; r < rows; r++)
            if (array[r] == null || columns != array[r].length)
               array[r] = (E[])Array.newInstance (componentType, columns);
         arr = array;
      }
      else
         arr = (E[][])Array.newInstance (componentType, new int[] { rows, columns });
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++)
            arr[r][c] = get (r, c);
      return arr;
   }

   public Object[][] to2DArray () {
      final int rows = rows();
      final int columns = columns();
      final Object[][] arr = new Object[rows][columns];
      for (int r = 0; r < rows; r++)
         for (int c = 0; c < columns; c++)
            arr[r][c] = get (r, c);
      return arr;
   }
   
   private class MatrixList extends AbstractList<E> {
      private final int expectedModCount = modCount;
      
      @Override
      public int size() {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return rows()*columns();
      }
      
      @Override
      public boolean isEmpty() {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return rows() == 0 || columns() == 0;
      }
      
      @Override
      public E get (int index) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         final int columns = columns();
         final int r = index / columns;
         final int c = index % columns;
         return AbstractMatrix.this.get (r, c);
      }
      
      @Override
      public E set (int index, E value) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         final int columns = columns();
         final int r = index / columns;
         final int c = index % columns;
         return AbstractMatrix.this.set (r, c, value);
      }
   }

   private class RandomAccessMatrixList extends MatrixList implements RandomAccess {}
   
   private class RowView extends AbstractList<E> implements RandomAccess {
      private int row;
      private final int expectedModCount = modCount;
      
      public RowView (int row) {
         if (row < 0 || row >= rows())
            throw new IndexOutOfBoundsException
            ("Invalid row index " + row);
         this.row = row;
      }
      
      @Override
      public int size() {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return columns();
      }
      
      @Override
      public E get (int c) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return AbstractMatrix.this.get (row, c);
      }
      
      @Override
      public E set (int c, E value) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return AbstractMatrix.this.set (row, c, value);
      }
   }
   
   private class RandomAccessRowView extends RowView implements RandomAccess {
      public RandomAccessRowView (int r) {
         super (r);
      }
   }
   
   private class ColumnView extends AbstractList<E> implements RandomAccess {
      private int column;
      private final int expectedModCount = modCount;
      
      public ColumnView (int column) {
         if (column < 0 || column >= columns())
            throw new IndexOutOfBoundsException
            ("Invalid column index " + column);
         this.column = column;
      }
      
      @Override
      public int size() {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return rows();
      }
      
      @Override
      public E get (int r) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return AbstractMatrix.this.get (r, column);
      }

      @Override
      public E set (int r, E value) {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return AbstractMatrix.this.set (r, column, value);
      }
   }
   
   private class RandomAccessColumnView extends ColumnView implements RandomAccess {
      public RandomAccessColumnView (int c) {
         super (c);
      }
   }
   
   private class MyIterator implements Iterator<E> {
      private final int expectedModCount = modCount;
      private int r = 0;
      private int c = 0;

      public boolean hasNext () {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
         return r < rows() && c < columns();
      }

      public E next () {
         if (!hasNext())
            throw new NoSuchElementException();
         final E res = get (r, c);
         ++c;
         if (c == columns()) {
            c = 0;
            ++r;
         }
         return res;
      }

      public void remove () {
         throw new UnsupportedOperationException();
      }
   }
}

class SubMatrix<E> extends AbstractMatrix<E> {
   private AbstractMatrix<E> matrix;
   private int expectedModCount;
   private int fromRow;
   private int fromColumn;
   private int rows;
   private int columns;
   
   public SubMatrix (AbstractMatrix<E> matrix, int fromRow, int fromColumn, int toRow, int toColumn) {
      if (fromRow < 0 || toRow < 0 || fromColumn < 0 || toColumn < 0)
         throw new IndexOutOfBoundsException
         ("rows and columns must not be negative");
      if (fromRow >= matrix.rows () || toRow > matrix.rows ())
         throw new IndexOutOfBoundsException
         ("Too large row indices");
      if (fromColumn >= matrix.columns () || toColumn > matrix.columns ())
         throw new IndexOutOfBoundsException
         ("Too large column indices");
      if (fromRow > toRow || fromColumn > toColumn)
         throw new IllegalArgumentException
         ("fromRow > toRow or fromColumn > toColumn");
      this.matrix = matrix;
      expectedModCount = matrix.modCount;
      this.fromRow = fromRow;
      this.fromColumn = fromColumn;
      rows = toRow - fromRow;
      columns = toColumn - fromColumn;
   }

   public int columns () {
      if (matrix.modCount != expectedModCount)
         throw new ConcurrentModificationException();
      return columns;
   }
   
   public int rows () {
      if (matrix.modCount != expectedModCount)
         throw new ConcurrentModificationException();
      return rows;
   }

   public E get (int r, int c) {
      if (matrix.modCount != expectedModCount)
         throw new ConcurrentModificationException();
      if (r < 0 || c < 0)
         throw new IndexOutOfBoundsException
         ("r or c are negative");
      if (r >= rows || c >= columns)
         throw new IndexOutOfBoundsException
         ("r or c are greater than or equal to the number of rows or columns");
      return matrix.get (r + fromRow, c + fromColumn);
   }

   public void setColumns (int numColumns) {
      throw new UnsupportedOperationException();
   }

   public void setRows (int numRows) {
      throw new UnsupportedOperationException();
   }

   @Override
   public E set (int r, int c, E value) {
      if (matrix.modCount != expectedModCount)
         throw new ConcurrentModificationException();
      if (r < 0 || c < 0)
         throw new IndexOutOfBoundsException
         ("r or c are negative");
      if (r >= rows || c >= columns)
         throw new IndexOutOfBoundsException
         ("r or c are greater than or equal to the number of rows or columns");
      return matrix.set (r + fromRow, c + fromColumn, value);
   }

   @Override
   public Matrix<E> viewPart (int fromRow, int fromColumn, int toRow, int toColumn) {
      return new SubMatrix<E> (this, fromRow, fromColumn, toRow, toColumn);
   }
}

class RandomAccessSubMatrix<E> extends SubMatrix<E> implements RandomAccess {
   public RandomAccessSubMatrix (AbstractMatrix<E> matrix, int fromRow, int fromColumn, int toRow, int toColumn) {
      super (matrix, fromRow, fromColumn, toRow, toColumn);
   }

   @Override
   public Matrix<E> viewPart (int fromRow, int fromColumn, int toRow, int toColumn) {
      return new RandomAccessSubMatrix<E> (this, fromRow, fromColumn, toRow, toColumn);
   }
}
