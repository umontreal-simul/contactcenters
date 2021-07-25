package umontreal.iro.lecuyer.xmlbind;

import java.io.Closeable;

/**
 * Represents a 2D array obtained from
 * a data source such a text file, or
 * a spreadsheet.
 * Such a source array can be used
 * to create 1D or 2D arrays.
 * The {@link #rows()},
 * {@link #columns(int)}, and
 * {@link #get(Class,int,int)}
 * methods can then be used to
 * inspect the array.
 */
public interface SourceArray2D extends Closeable {
   /**
    * Returns the number of rows in
    * the source array. 
    * @return the number of rows in the array.
    */
   public int rows();
   
   /**
    * Returns the number of columns
    * in row \texttt{row}
    * of the source array.
    * @param row the row to test.
    * @return the number of columns in the row.
    * @exception IllegalArgumentException if
    * the row index is out of bounds.
    */
   public int columns (int row);
   
   /**
    * Returns the element at row
    * \texttt{row} and column
    * \texttt{column} of the source array,
    * converted to class \texttt{pcls}.
    * @param <T> the target class.
    * @param pcls the target class. 
    * @param row the row index.
    * @param column the column index.
    * @return the element.
    * @exception IllegalArgumentException if
    * the row or column indices are out of bounds.
    * @exception ClassCastException if the
    * element cannot be converted to the
    * target class.
    */
   public <T> T get (Class<T> pcls, int row, int column);
   
   /**
    * Clears the data in the source array.
    */
   public void close();
}
