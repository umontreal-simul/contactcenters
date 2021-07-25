package umontreal.iro.lecuyer.util;

import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.ObjectMatrix3D;
import cern.colt.matrix.impl.AbstractMatrix1D;
import cern.colt.matrix.impl.AbstractMatrix2D;
import cern.colt.matrix.impl.Former;
import cern.colt.matrix.objectalgo.Formatter;

public class LaTeXObjectMatrixFormatter extends Formatter {
   private static final long serialVersionUID = 3655935203899900097L;

   /**
    * Constructs and returns a matrix formatter with alignment <tt>LEFT</tt>.
    */
   public LaTeXObjectMatrixFormatter () {
      this (LEFT);
   }

   /**
    * Constructs and returns a matrix formatter.
    * 
    * @param alignment
    *           the given alignment used to align a column.
    */
   public LaTeXObjectMatrixFormatter (String alignment) {
      setAlignment (alignment);
      columnSeparator = " & ";
      rowSeparator = " \\\\\n";
      sliceSeparator = " \\\\\\hline\n";
      printShape = false;
   }

   /**
    * Converts a given cell to a String; no alignment considered.
    */
   @Override
   protected String form (AbstractMatrix1D matrix, int index, Former formatter) {
      return this.form ((ObjectMatrix1D) matrix, index, formatter);
   }

   /**
    * Converts a given cell to a String; no alignment considered.
    */
   @Override
   protected String form (ObjectMatrix1D matrix, int index, Former formatter) {
      final Object value = matrix.get (index);
      if (value == null)
         return "";
      return String.valueOf (value);
   }

   /**
    * Returns a string representations of all cells; no alignment considered.
    */
   @Override
   protected String[][] format (AbstractMatrix2D matrix) {
      return this.format ((ObjectMatrix2D) matrix);
   }

   /**
    * Returns a string representations of all cells; no alignment considered.
    */
   @Override
   protected String[][] format (ObjectMatrix2D matrix) {
      final String[][] strings = new String[matrix.rows ()][matrix.columns ()];
      for (int row = matrix.rows (); --row >= 0;)
         strings[row] = formatRow (matrix.viewRow (row));
      return strings;
   }

   /**
    * Returns a string representation of the given matrix.
    * 
    * @param matrix
    *           the matrix to convert.
    */
   @Override
   protected String toString (AbstractMatrix2D matrix) {
      return this.toString ((ObjectMatrix2D) matrix);
   }

   /**
    * Returns a string representation of the given matrix.
    * 
    * @param matrix
    *           the matrix to convert.
    */
   @Override
   public String toString (ObjectMatrix1D matrix) {
      final ObjectMatrix2D easy = matrix.like2D (1, matrix.size ());
      easy.viewRow (0).assign (matrix);
      return toString (easy);
   }

   protected boolean formatHeader = true;
   protected String envName = "tabular";
   protected int lineSepCol = -1;

   /**
    * Returns a string representation of the given matrix.
    * 
    * @param matrix
    *           the matrix to convert.
    */
   @Override
   public String toString (ObjectMatrix2D matrix) {
      final StringBuilder header = new StringBuilder ();
      if (formatHeader) {
         header.append ("\\begin{").append (envName).append ("}{|");
         String a;
         if (alignment == LEFT)
            a = "l";
         else if (alignment == RIGHT)
            a = "r";
         else if (alignment == CENTER)
            a = "c";
         else
            a = "l";
         for (int c = 0; c < matrix.columns (); c++) {
            header.append (a);
            if (c == lineSepCol)
               header.append ("|");
         }
         header.append ("|}\\hline\n");
      }
      header.append (super.toString (matrix));
      header.append (" \\\\\n\\hline\n");
      if (formatHeader)
         header.append ("\\end{").append (envName).append ("}\n");
      return header.toString ();
   }

   /**
    * Returns a string representation of the given matrix.
    * 
    * @param matrix
    *           the matrix to convert.
    */
   @Override
   public String toString (ObjectMatrix3D matrix) {
      final StringBuilder buf = new StringBuilder ();
      final boolean oldPrintShape = printShape;
      printShape = false;
      for (int slice = 0; slice < matrix.slices (); slice++) {
         if (slice != 0)
            buf.append (sliceSeparator);
         buf.append (super.toString (matrix.viewSlice (slice)));
      }
      printShape = oldPrintShape;
      if (printShape)
         buf.insert (0, shape (matrix) + "\n");
      return buf.toString ();
   }

   /**
    * Returns a string representation of the given matrix with axis as well as
    * rows and columns labeled. Pass <tt>null</tt> to one or more parameters
    * to indicate that the corresponding decoration element shall not appear in
    * the string converted matrix.
    * 
    * @param matrix
    *           The matrix to format.
    * @param rowNames
    *           The headers of all rows (to be put to the left of the matrix).
    * @param columnNames
    *           The headers of all columns (to be put to above the matrix).
    * @param rowAxisName
    *           The label of the y-axis.
    * @param columnAxisName
    *           The label of the x-axis.
    * @param title
    *           The overall title of the matrix to be formatted.
    * @return the matrix converted to a string.
    */
   @Override
   public String toTitleString (ObjectMatrix2D matrix, String[] rowNames,
         String[] columnNames, String rowAxisName, String columnAxisName,
         String title) {
      if (matrix.size () == 0)
         return "Empty matrix";
      final String oldFormat = format;
      format = LEFT;
      final int oldLineSepCol = lineSepCol;

      final int rows = matrix.rows ();
      final int columns = matrix.columns ();

      // determine how many rows and columns are needed
      int r = 0;
      int c = 0;
      r += columnNames == null ? 0 : 1;
      c += rowNames == null ? 0 : 1;
      c += rowAxisName == null ? 0 : 1;
      // c += (rowNames != null || rowAxisName != null ? 1 : 0);
      lineSepCol = rowNames != null || rowAxisName != null ? 1 : -1;

      final int height = r
            + Math.max (rows, rowAxisName == null ? 0 : rowAxisName.length ());
      final int width = c + columns;

      // make larger matrix holding original matrix and naming strings
      final cern.colt.matrix.ObjectMatrix2D titleMatrix = matrix.like (height,
            width);

      // insert original matrix into larger matrix
      titleMatrix.viewPart (r, c, rows, columns).assign (matrix);

      // insert column axis name in leading row
      if (r > 0)
         titleMatrix.viewRow (0).viewPart (c, columns).assign (columnNames);

      // insert row axis name in leading column
      if (rowAxisName != null) {
         final String[] rowAxisStrings = new String[rowAxisName.length ()];
         for (int i = rowAxisName.length (); --i >= 0;)
            rowAxisStrings[i] = rowAxisName.substring (i, i + 1);
         titleMatrix.viewColumn (0).viewPart (r, rowAxisName.length ()).assign (
               rowAxisStrings);
      }
      // insert row names in next leading columns
      if (rowNames != null)
         titleMatrix.viewColumn (c - 1).viewPart (r, rows).assign (rowNames);

      // insert vertical "---------" separator line in next leading column
      // if (c > 0)
      // titleMatrix.viewColumn (c - 2 + 1).viewPart (0, rows + r).assign ("|");

      // convert the large matrix to a string
      final boolean oldPrintShape = printShape;
      printShape = false;
      final String str = toString (titleMatrix);
      printShape = oldPrintShape;

      // insert horizontal "--------------" separator line
      final StringBuilder total = new StringBuilder (str);
      if (columnNames != null) {
         final int i = str.indexOf (rowSeparator);
         total.insert (i + rowSeparator.length (), "\\hline\n");
      }
      // else if (columnAxisName != null) {
      // //int i = str.indexOf (rowSeparator);
      // total.insert (0, "\\hline\n");
      // }

      // insert line for column axis name
      // if (columnAxisName != null) {
      // int j = 0;
      // if (c > 0)
      // j = str.indexOf ('|');
      // String s = blanks (j);
      // if (c > 0)
      // s = s + "| ";
      // s = s + columnAxisName + "\n";
      // total.insert (0, s);
      // }

      // insert title
      if (title != null)
         total.insert (0, title + "\n\n");

      format = oldFormat;
      lineSepCol = oldLineSepCol;

      return total.toString ();
   }

   /**
    * Returns a string representation of the given matrix with axis as well as
    * rows and columns labeled. Pass <tt>null</tt> to one or more parameters
    * to indicate that the corresponding decoration element shall not appear in
    * the string converted matrix.
    * 
    * @param matrix
    *           The matrix to format.
    * @param sliceNames
    *           The headers of all slices (to be put above each slice).
    * @param rowNames
    *           The headers of all rows (to be put to the left of the matrix).
    * @param columnNames
    *           The headers of all columns (to be put to above the matrix).
    * @param sliceAxisName
    *           The label of the z-axis (to be put above each slice).
    * @param rowAxisName
    *           The label of the y-axis.
    * @param columnAxisName
    *           The label of the x-axis.
    * @param title
    *           The overall title of the matrix to be formatted.
    * @return the matrix converted to a string.
    */
   @Override
   public String toTitleString (ObjectMatrix3D matrix, String[] sliceNames,
         String[] rowNames, String[] columnNames, String sliceAxisName,
         String rowAxisName, String columnAxisName, String title) {
      if (matrix.size () == 0)
         return "Empty matrix";
      final StringBuilder buf = new StringBuilder ();
      for (int i = 0; i < matrix.slices (); i++) {
         if (i != 0)
            buf.append ("\n\n");
         buf.append (toTitleString (matrix.viewSlice (i), rowNames,
               columnNames, rowAxisName, columnAxisName, title + "\n"
                     + sliceAxisName + "=" + sliceNames[i]));
      }
      return buf.toString ();
   }

   @Override
   public LaTeXObjectMatrixFormatter clone () {
      return (LaTeXObjectMatrixFormatter) super.clone ();
   }
}
