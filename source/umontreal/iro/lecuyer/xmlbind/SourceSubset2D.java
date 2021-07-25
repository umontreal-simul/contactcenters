package umontreal.iro.lecuyer.xmlbind;


/**
 * Represents a source subset obtained from
 * a source array.
 * Such a 2D array is obtained by taking
 * a subset of the rows and the columns
 * of another source array. 
 */
public class SourceSubset2D implements SourceArray2D {
   private SourceArray2D sourceArray;
   private int fromRow;
   private int fromColumn;
   private int toRow;
   private int[] toColumn;
   private boolean transposed = false;

   /**
    * Constructs a new subset from the array
    * \texttt{sourceArray}.
    * @param sourceArray the original source array.
    * @param fromRow the starting row in the original array.
    * @param fromColumn the starting column in the original array.
    * @param numRows the number of rows in the subset.
    * @param numColumns  the number of columns in the subset.
    * @param transposed determines if the subset needs to be transposed.
    * @exception IllegalArgumentException if one or more arguments
    * are negative or out of bounds.
    * @exception IllegalStateException if \texttt{sourceArray}
    * is not initialized.
    */
   public SourceSubset2D (SourceArray2D sourceArray, int fromRow, int fromColumn, int numRows, int numColumns, boolean transposed) {
      super ();
      this.sourceArray = sourceArray;
      this.fromRow = fromRow;
      this.fromColumn = fromColumn;
      this.transposed = transposed;
      if (fromRow < 0 || fromColumn < 0 ||
            numRows < 0 || numColumns < 0)
         throw new IllegalArgumentException
         ("The arguments must not be negative");
      if (fromRow >= sourceArray.rows ())
         throw new IllegalArgumentException
         ("The starting row " + fromRow + " is greater than the number of rows " + sourceArray.rows ());
      toRow = numRows == Integer.MAX_VALUE ? sourceArray.rows () : fromRow + numRows;
      if (toRow > sourceArray.rows ())
         throw new IllegalArgumentException
         ("The ending row " + toRow + " is greater than the number of rows " + sourceArray.rows ());
      toColumn = new int[toRow - fromRow];
      for (int r = fromRow; r < toRow; r++) {
         if (fromColumn >= sourceArray.columns (r))
            throw new IllegalArgumentException
            ("The starting column " + fromColumn + " is greater than the number of columns " + sourceArray.columns (r)
                  + " in row " + r);
         toColumn[r - fromRow] = numColumns == Integer.MAX_VALUE ? sourceArray.columns (r) : fromColumn + numColumns;
         if (toColumn[r - fromRow] > sourceArray.columns (r))
            throw new IllegalArgumentException
            ("The ending column " + toColumn[r - fromRow] + " is greater than the number of columns " + sourceArray.columns (r)
                  + " in row " + r);
         
      }
      if (transposed) {
         final int v = toColumn[0];
         for (int c = 1; c < toColumn.length; c++)
            if (v != toColumn[c])
               throw new IllegalArgumentException
               ("The submatrix cannot be transposed because it is not rectangular");
      }
   }

   public int columns (int row) {
      if (transposed)
         return toRow - fromRow;
      return toColumn[row] - fromColumn;
   }

   public void close () {
   }

   public <T> T get (Class<T> pcls, int row, int column) {
      if (row < 0 || row >= rows())
         throw new IllegalArgumentException
         ("Invalid row index " + row);
      if (column < 0 || column >= columns (row))
         throw new IllegalArgumentException
         ("Invalid column index " + column);
      if (transposed)
         return sourceArray.get (pcls, fromRow + column, fromColumn + row);
      return sourceArray.get (pcls, fromRow + row, fromColumn + column);
   }

   public void init () {
   }

   public int rows () {
      if (transposed)
         return toColumn[0] - fromColumn;
      return toRow - fromRow;
   }
   
   public String getElementName() {
      return "SubSet";
   }
}
