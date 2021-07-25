package umontreal.iro.lecuyer.xmlconfig;

import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a 2D array obtained from
 * a data source such a text file, or
 * a database.
 * Such a source array can be used
 * to create 1D or 2D arrays.
 * Any implementation of this interface
 * must be initialized through
 * the {@link #init()}
 * method before elements can be
 * extracted from the source array.
 * The {@link #rows()},
 * {@link #columns(int)}, and
 * {@link #get(Class,int,int)}
 * methods can then be used to
 * inspect the array.
 */
public interface SourceArray2D {
   /**
    * Returns the number of rows in
    * the source array. 
    * @return the number of rows in the array.
    * @exception IllegalStateException if the array was not
    * initialized.
    */
   public int rows();
   
   /**
    * Returns the number of columns
    * in row \texttt{row}
    * of the source array.
    * @param row the row to test.
    * @return the number of columns in the row.
    * @exception IllegalStateException if the source array was not
    * initialized.
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
    * @exception IllegalStateException if the array was not
    * initialized.
    * @exception ClassCastException if the
    * element cannot be converted to the
    * target class.
    */
   public <T> T get (Class<T> pcls, int row, int column)
   throws UnsupportedConversionException;
   
   /**
    * Initializes the source array.
    * @exception IllegalStateException
    * if the source array cannot be initialized.
    */
   public void init();
   
   /**
    * Clears the data in the source array.
    */
   public void dispose();
   
   /**
    * Returns the name of the XML element
    * representing the type of source array
    * implemented.
    * @return the name of the XML representing the array type.
    */
   public String getElementName();
}
