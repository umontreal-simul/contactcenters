package umontreal.iro.lecuyer.xmlconfig;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;

/**
 * Represents a parameter object whose contents
 * can be extracted from a source subset
 * created from a source array.
 * The source array is created by reading
 * data from an external source such as a CSV
 * file or a database.
 * The source subset is then constructed
 * by taking a possibly transposed 
 * portion of the source array.
 * 
 * In an XML element whose parameters are extracted from
 * a source array,  
 * the source array is described using the \texttt{CSV} or \texttt{DB}
 * subelements.
 * The \texttt{CSV} element, represented
 * by {@link CSVSourceArray2D}, takes a single \texttt{URL} attribute
 * pointing to a CSV-formatted text file containing the data.
 * The \texttt{DB} element, represented
 * by {@link DBSourceArray2D}, requires a
 * \texttt{data\-Query} attribute giving
 * the query to perform on a database described by
 * a \texttt{database} subelement.
 * 
 * A single source array can
 * contain information for multiple destination
 * arrays.
 * For example, a spreadsheet containing one column of
 * arrival rates for each call type might be exported to CSV, and used as
 * a source array.
 * Therefore, facilities are provided to subset the source array.
 * For this,
 * the \texttt{starting\-Row}, \texttt{starting\-Column},
 * \texttt{num\-Rows}, and \texttt{num\-Columns}
 * attributes of \texttt{a}
 * can be used to indicate the portion of the source array to consider.
 * The first two attributes give the (zero-based) starting row and
 * column of the subset.  These are optional and defaults to 0.
 * The last two attributes give the dimensions of the subset.
 * If the number of rows [columns] is omitted, it defaults
 * to the number of rows [columns] in the
 * source array minus the starting row [column].
 * If the resulting subarray is rectangular, it can finally be transposed
 * by using the \texttt{transposed} boolean attribute.
 * This latter boolean attribute is also optional, and defaults to
 * \texttt{false}, i.e., no transposition.
 */
public class ParamWithSourceArray extends AbstractParam implements StorableParam,Cloneable {
   private SourceArray2D sourceArray;
   private int fromRow;
   private int fromColumn;
   private int numRows = Integer.MAX_VALUE;
   private int numColumns = Integer.MAX_VALUE;
   private boolean transposed = false;
   private SourceArray2D sourceSubset;
   
   /**
    * Returns the matrix from which the data
    * is extracted.
    * @return the matrix to extract data from.
    */
   public SourceArray2D getDataMatrix() {
      return sourceArray;
   }
   
   /**
    * Sets the matrix to extract data from to
    * \texttt{dataMatrix}.
    * @param dataMatrix the new matrix to extract data from.
    */
   public void setDataMatrix (SourceArray2D dataMatrix) {
      sourceArray = dataMatrix;
   }
   
   /**
    * Determines if the matrix of data
    * must be transposed before data
    * is extracted.
    * @return the status of the transpose indicator.
    */
   public boolean isTransposed() {
      return transposed;
   }
   
   /**
    * Sets the matrix transposition indicator
    * to \texttt{transposedDataMatrix}.
    * @param transposed the new value of the indicator.
    */
   public void setTransposed (boolean transposed) {
      this.transposed = transposed;
   }
   
   /**
    * Determines the first row to read in
    * the matrix.
    * @return the starting row.
    */
   public int getStartingRow() {
      return fromRow;
   }
   
   /**
    * Sets the starting row to extract
    * external data from
    * to \texttt{startingRow}.
    * @param startingRow the starting row.
    */
   public void setStartingRow (int startingRow) {
      if (startingRow < 0)
         throw new IllegalArgumentException
         ("The starting row must not be negative");
      fromRow = startingRow;
   }
   
   /**
    * Returns the number of rows to be
    * extracted when the array comes from
    * an external source.
    * If the number of rows is set
    * to {@link Integer#MAX_VALUE},
    * all available rows, starting from
    * {@link #getStartingRow()}
    * are extracted.
    * The default number of rows is
    * {@link Integer#MAX_VALUE}.
    * @return the number of rows in the array.
    */
   public int getNumRows() {
      return numRows;
   }
   
   /**
    * Sets the number of rows to be
    * extracted from an external source to
    * construct this
    * array to \texttt{numRows}
    * @param numRows
    */
   public void setNumRows (int numRows) {
      if (numRows < 0)
         throw new IllegalArgumentException
         ("The number of rows must not be negative");
      this.numRows = numRows;
   }

   /**
    * Determines the first column to read in
    * the matrix.
    * @return the starting column.
    */
   public int getStartingColumn() {
      return fromColumn;
   }
   
   /**
    * Sets the starting column to extract
    * external data from
    * to \texttt{startingColumn}.
    * @param startingColumn the starting column.
    */
   public void setStartingColumn (int startingColumn) {
      if (startingColumn < 0)
         throw new IllegalArgumentException
         ("The starting column must not be negative");
      fromColumn = startingColumn;
   }
   
   /**
    * Returns the number of columns to be
    * extracted when the array comes from
    * an external source.
    * If the number of columns is set
    * to {@link Integer#MAX_VALUE},
    * all available columns, starting from
    * {@link #getStartingColumn()}
    * are extracted.
    * The default number of rows is
    * {@link Integer#MAX_VALUE}.
    * @return the number of columns in the array.
    */
   public int getNumColumn() {
      return numColumns;
   }
   
   /**
    * Sets the number of columns to be
    * extracted from an external source to
    * construct this
    * array to \texttt{numColumns}
    * @param numColumns the number of columns.
    */
   public void setNumColumns (int numColumns) {
      if (numColumns < 0)
         throw new IllegalArgumentException
         ("The number of columns must not be negative");
      this.numColumns = numColumns;
   }
   
   public void addCSV (CSVSourceArray2D dataMatrix) {
      setDataMatrix (dataMatrix);
   }
   
   public void addDB (DBSourceArray2D dataMatrix) {
      setDataMatrix (dataMatrix);
   }

   public void addExcel (ExcelSourceArray2D dataMatrix) {
      setDataMatrix (dataMatrix);
   }
   
   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName, sourceArray == null, spc);
      if (fromRow > 0)
         el.setAttribute ("startingRow", String.valueOf (fromRow));
      if (fromColumn > 0)
         el.setAttribute ("startingColumn", String.valueOf (fromColumn));
      if (numRows != Integer.MAX_VALUE)
         el.setAttribute ("numRows", String.valueOf (numRows));
      if (numColumns != Integer.MAX_VALUE)
         el.setAttribute ("numColumns", String.valueOf (numColumns));
      if (transposed)
         el.setAttribute ("transposed", "true");
      if (sourceArray != null && sourceArray instanceof StorableParam)
         ((StorableParam)sourceArray).toElement (finder, el, sourceArray.getElementName (), spc);
      return el;
   }
   
   @Override
   public ParamWithSourceArray clone() {
      ParamWithSourceArray cpy;
      try {
         cpy = (ParamWithSourceArray)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError
         ("CloneNotSupportedException for a class implementing Cloneable");
      }
      cpy.sourceSubset = null;
      return cpy;
   }
   
   /**
    * Initializes the source array associated with
    * this object.
    */
   public void initSourceArray() {
      if (sourceArray == null)
         throw new NullPointerException
         ("The data matrix must not be null");
      sourceArray.init ();
   }
   
   /**
    * Clears the data in the source array.
    */
   public void disposeSourceArray() {
      sourceArray.dispose ();
      sourceSubset = null;
   }

   /**
    * Returns the source subset used
    * to extract data.
    * @return the source subset.
    */
   public SourceArray2D getSourceSubset() {
      if (sourceSubset == null)
         sourceSubset = new SourceSubset2D (sourceArray, fromRow, fromColumn, numRows, numColumns, transposed);
      return sourceSubset;
   }
}
