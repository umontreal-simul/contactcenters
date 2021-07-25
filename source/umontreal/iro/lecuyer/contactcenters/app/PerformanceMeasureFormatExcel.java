package umontreal.iro.lecuyer.contactcenters.app;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import jxl.CellView;
import jxl.biff.DisplayFormat;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.CellFormat;
import jxl.format.Colour;
import jxl.format.Orientation;
import jxl.format.VerticalAlignment;
import jxl.write.Blank;
import jxl.write.DateFormats;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.NumberFormats;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import umontreal.iro.lecuyer.contactcenters.app.params.PerformanceMeasureParams;
import umontreal.iro.lecuyer.contactcenters.app.params.PrintedStatParams;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.util.Pair;
import umontreal.ssj.util.TimeUnit;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Provides methods used to format matrices of performance measures into
 * Microsoft Excel spreadsheets. This class uses the JExcel API library to
 * construct a workbook in memory, i.e., an instance of {@link WritableWorkbook}.
 * Some methods are provided to add sheets to the current workbook, and tables
 * of results to the current sheet. Methods are finally available to transfer
 * the in-memory workbook into a disk file that can be read by Microsoft Excel,
 * OpenOffice.org Calc, etc.
 * 
 * For example, the following code creates an Excel file containing three sheets
 * containing the statistics for each aggregate performance measures (e.g.,
 * service level for all contact types, over all periods), the statistics for
 * time-aggregate measures (e.g., service level for contacts of type~$k$ over
 * all periods, for all $k$), and statistics for all performance measures (e.g.,
 * service level for contacts of type~$k$ during period~$p$, for all $k$ and
 * $p$). The variable \texttt{sim} corresponds to any instance of
 * {@link ContactCenterSim}. 
 * 
 * \begin{verbatim} 
 *    PerformanceMeasureFormatExcel2fmt = 
 *       new PerformanceMeasureFormatExcel2();
 *    fmt.newSheet ("Summary");
 *    fmt.formatStatisticsSummary (sim, 0.95, sim.getPerformanceMeasures());
 *    fmt.newSheet ("Detailed, without individual period");
 *    for (PerformanceMeasureType pm : sim.getPerformanceMeasures())
 *       fmt.formatStatisticsDetailedHidePeriods (sim, 0.95, pm);
 *    fmt.newSheet ("Detailed, with individual periods");
 *    for (PerformanceMeasureType pm : sim.getPerformanceMeasures())
 *       fmt.formatStatisticsDetailed (sim, 0.95, pm);
 *    fmt.writeWorkbook (new File ("output.xls")); 
 * \end{verbatim}
 */
public class PerformanceMeasureFormatExcel extends PerformanceMeasureFormat {
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.app");
   private static final long msInADay = 24*60*60*1000;
   public static final String defaultSummarySheetName = Messages.getString("PerformanceMeasureFormatExcel.SummaryStatistics"); //$NON-NLS-1$;
   public static final String defaultDetailedSheetNameWithoutPeriods = Messages.getString("PerformanceMeasureFormatExcel.DetailedStatisticsWithoutPeriods"); // $NON-NLS-1$;
   public static final String defaultDetailedSheetNameWithPeriods = Messages.getString("PerformanceMeasureFormatExcel.DetailedStatisticsWithPeriods"); //$NON-NLS-1$;
   public static final String defaultObsSheetName = Messages.getString("PerformanceMeasureFormatExcel.Observations"); //$NON-NLS-1$;

   private WritableWorkbook workbook;
   private WritableSheet sheet;
   private int currentRow;
   private int startingColumn;
   private final boolean addHeaders = true;
   private final boolean fillHeaders = false;
   private boolean rowOverwrite = false;
   private boolean rowOutlines = false;
   private boolean columnOutlines = false;
   private int maxColumns = 256;
   private final Set<Pair<Integer, Integer>> rowGroups = new HashSet<Pair<Integer, Integer>>();
   private final Set<Pair<Integer, Integer>> columnGroups = new HashSet<Pair<Integer, Integer>>();
   
   private CellStyleManager titleCellStyle;
   private CellStyleManager verticalTitleCellStyle;
   private CellStyleManager cellStyle;
   private CellStyleManager wrapCellStyle;
   private CellStyleManager numberCellStyle;
   private CellStyleManager integerCellStyle;
   private CellStyleManager percentCellStyle;
   private CellStyleManager dateCellStyle;
   private CellStyleManager timeCellStyle;
   
   private void resetCellStyles() {
      titleCellStyle = new TitleCellStyleManager();
      verticalTitleCellStyle = new VerticalTitleCellStyleManager();
      cellStyle = new GeneralCellStyleManager();
      wrapCellStyle = new WrapCellStyleManager();
      numberCellStyle = new NumberCellStyleManager (NumberFormats.DEFAULT); 
      integerCellStyle = new NumberCellStyleManager (NumberFormats.INTEGER); 
      percentCellStyle = new NumberCellStyleManager (NumberFormats.PERCENT_FLOAT);
      dateCellStyle = new NumberCellStyleManager (DateFormats.FORMAT9);
      timeCellStyle = new NumberCellStyleManager (DateFormats.FORMAT11);
   }

   /**
    * Constructs a new performance measure formatter with the workbook
    * \texttt{wb}.
    * 
    * @param wb
    *           the workbook used for formatting.
    */
   public PerformanceMeasureFormatExcel (WritableWorkbook wb) {
      if (wb == null)
         throw new NullPointerException ("The given workbook must not be null");
      workbook = wb;
      resetCellStyles ();
   }

   public PerformanceMeasureFormatExcel (WritableWorkbook wb, ReportParams reportParams) {
      super (reportParams);
      if (wb == null)
         throw new NullPointerException ("The given workbook must not be null");
      workbook = wb;
      resetCellStyles ();
   }
   
   /**
    * Creates a new {@link WritableSheet} with name \texttt{sheetName}, and sets
    * this new sheet as the current one. This method resets the current row
    * index to 0, and the starting column to 0.
    * 
    * If a sheet with the given name already exists, this method appends a
    * number to the given name. The number is incremented until the resulting
    * resulting sheet name is unused.
    * 
    * @param sheetName
    *           the name of the new sheet.
    */
   public void newSheet (String sheetName) {
      if (workbook.getSheet (sheetName) == null) 
         sheet = workbook.createSheet (sheetName, workbook.getNumberOfSheets ());
      else {
         int n = 1;
         sheet = null;
         while (sheet == null) {
            final String sn = sheetName + "_" + n++;
            if (workbook.getSheet (sn) == null) {
               logger.warning ("Sheet with name " + sheetName + " already exists; creating a new sheet with name " + sn);
               sheet = workbook.createSheet (sn, workbook.getNumberOfSheets ());
            }
         }
      }
      currentRow = 0;
      startingColumn = 0;
      rowGroups.clear ();
      columnGroups.clear ();
   }
   
   /**
    * Returns the index of the current row into the current spreadsheet. This
    * corresponds to the row at which subsequent tables of results will be
    * inserted. After each insertion, this index is incremented automatically.
    * Consequently, the default index corresponds to the last row in the sheet.
    * 
    * @return the index of the current row.
    */
   public int getCurrentRow () {
      return currentRow;
   }

   /**
    * Sets the index of the current row to \texttt{currentRow}
    * 
    * @param currentRow
    *           the new index of the current row.
    */
   public void setCurrentRow (int currentRow) {
      if (currentRow < 0)
         throw new IllegalArgumentException ("currentRow must be positive or 0");
      this.currentRow = currentRow;
   }

   /**
    * Returns the index of the starting column of subsequent tables of results.
    * This defaults to 0, but can be changed to format, e.g., side-by-side
    * tables of results. This index is not modified by methods writing data to
    * the spreadsheet.
    * 
    * @return the index of the starting column.
    */
   public int getStartingColumn () {
      return startingColumn;
   }

   /**
    * Sets the index of the starting column to \texttt{startingColumn}.
    * 
    * @param startingColumn
    *           the new starting column.
    */
   public void setStartingColumn (int startingColumn) {
      if (startingColumn < 0)
         throw new IllegalArgumentException (
               "startingColumn must be positive or 0");
      this.startingColumn = startingColumn;
   }

   /**
    * Returns the high-level object representing the current workbook.
    * 
    * @return the current workbook.
    */
   public WritableWorkbook getCurrentWorkbook () {
      return workbook;
   }
   
   /**
    * Sets the current workbook to \texttt{workbook}. This also resets the
    * current sheet to \texttt{null}.
    * 
    * @param workbook
    *           the new current workbook.
    */
   public void setCurrentWorkbook (WritableWorkbook workbook) {
      if (workbook == null)
         throw new NullPointerException();
      this.workbook = workbook;
      sheet = null;
      currentRow = 0;
      startingColumn = 0;
      resetCellStyles ();
      rowGroups.clear ();
      columnGroups.clear ();
   }

   /**
    * Returns the high-level object representing the current spreadsheet.
    * 
    * @return the current spreadsheet.
    */
   public WritableSheet getCurrentSheet () {
      return sheet;
   }
   
   /**
    * Sets the current sheet to \texttt{sheet}, and resets the current row and
    * starting column to 0.
    * 
    * @param sheet
    *           the new current sheet.
    */
   public void setCurrentSheet (WritableSheet sheet) {
      if (sheet == null)
         throw new NullPointerException ("The sheet must not be null");
      this.sheet = sheet;
      currentRow = 0;
      startingColumn = 0;
      rowGroups.clear ();
      columnGroups.clear ();
   }
   
   /**
    * Determines the status of row overwriting which affects how rows are
    * managed when the current row index is smaller than the number of rows.
    * When overwriting is \texttt{true}, the formating methods reuse the already
    * created rows. When overwriting is \texttt{false} (the default), formatting
    * methods always inert new rows.
    * 
    * @return the status of row overwriting.
    */
   public boolean getRowOverwrite() {
      return rowOverwrite;
   }
   
   /**
    * Sets the status of row overwriting to \texttt{rowOverwrite}.
    * 
    * @param rowOverwrite
    *           the new status of row overwriting.
    */
   public void setRowOverwrite (boolean rowOverwrite) {
      this.rowOverwrite = rowOverwrite;
   }
   
   /**
    * Determines if column outlines are created by formatting methods. The
    * default value of this boolean is \texttt{false}.
    * 
    * @return the status of the outline creation flag.
    */
   public boolean getColumnOutlines () {
      return columnOutlines;
   }

   /**
    * Sets the column outlines flag to \texttt{columnOutlines}.
    * 
    * @param columnOutlines
    *           the new value of the flag.
    */
   public void setColumnOutlines (boolean columnOutlines) {
      this.columnOutlines = columnOutlines;
   }

   /**
    * Determines if row outlines are created by formatting methods. The default
    * value of this boolean is \texttt{false}.
    * 
    * @return the status of the outline creation flag.
    */
   public boolean getRowOutlines () {
      return rowOutlines;
   }

   /**
    * Sets the row outlines flag to \texttt{rowOutlines}.
    * 
    * @param rowOutlines
    *           the new value of the flag.
    */
   public void setRowOutlines (boolean rowOutlines) {
      this.rowOutlines = rowOutlines;
   }

   /**
    * Returns the maximal number of columns a spreadsheet may contain. The
    * default value of this variable is 256. This affects how
    * {@link #formatValuesMatrix(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,boolean,String)},
    * and
    * {@link #formatStatisticsDetailedMatrix(ContactCenterSim,double,PerformanceMeasureType)}
    * work.
    * 
    * @return the maximal number of columns in a spreadsheet.
    */
   public int getMaxColumns () {
      return maxColumns;
   }

   /**
    * Sets the maximal number of columns in a spreadsheet to
    * \texttt{maxColumns}.
    * 
    * @param maxColumns
    *           the maximal number of columns in a spreadsheet.
    */
   public void setMaxColumns (int maxColumns) {
      if (maxColumns <= 0)
         throw new IllegalArgumentException
         ("The maximal number of columns should be a positive value");
      this.maxColumns = maxColumns;
   }

   private void prepareRow (int row) {
      if (row >= sheet.getRows ())
         // Beyond last row
         return;
      if (!rowOverwrite)
         sheet.insertRow (row);
   }

   /**
    * Writes the current workbook..
    * 
    * @throws IOException
    *            if an I/O error occurs during writing.
    */
   public void writeWorkbook () throws IOException {
      workbook.write ();
   }

   /**
    * This method does nothing as JExcel API does not support outlining yet.
    * Creates an outline for rows \texttt{fromRow} to \texttt{toRow}. Calling
    * the \texttt{groupRow} method repeatedly with the same values creates
    * multiple identical outlines in the spreadsheet. This method calls
    * \texttt{groupRow} if and only if the outline was not created previously.
    * 
    * @param fromRow
    *           the starting row of the outline.
    * @param toRow
    *           the ending row of the outline.
    */
   public void groupRow (int fromRow, int toRow) {
      if (!rowOutlines)
         return;
// final Pair<Integer, Integer> grp = new Pair<Integer, Integer> (fromRow,
// toRow);
// if (!rowGroups.contains (grp)) {
// sheet.groupRow (fromRow, toRow);
// rowGroups.add (grp);
// }
   }
   
   /**
    * Similar to {@link #groupRow(int,int)}, for creating column outlines.
    * 
    * @param fromColumn
    *           the starting column.
    * @param toColumn
    *           the ending column.
    */
   public void groupColumn (int fromColumn, int toColumn) {
      if (!columnOutlines)
         return;
// final Pair<int, int> grp = new Pair<int, int> (fromColumn, toColumn);
// if (!columnGroups.contains (grp)) {
// sheet.groupColumn (fromColumn, toColumn);
// columnGroups.add (grp);
// }
   }

   /**
    * Creates a cell style for cells containing titles for tables of results.
    * 
    * By default, this method uses the {@link CellFormat} no-argument
    * constructor to create the cell style, and sets its alignment to
    * ``center''. One can override this method to apply user-defined cell styles
    * (colors, fill patterns, borders, etc.).
    * 
    * @return the created cell style.
    */
   public WritableCellFormat createTitleCellStyle () throws WriteException {
      final WritableCellFormat style = new WritableCellFormat();
      style.setAlignment (Alignment.CENTRE);
      return style;
   }
   
   public WritableCellFormat createVerticalTitleCellStyle () throws WriteException {
      final WritableCellFormat style = new WritableCellFormat();
      style.setOrientation (Orientation.PLUS_90);
      style.setAlignment (Alignment.RIGHT);
      style.setVerticalAlignment (VerticalAlignment.CENTRE);
      return style;
   }
   
   private void formatHorizontalTitle (String title, int row,
         int startingColumn1, int numColumns) throws WriteException {
      formatHorizontalTitle (title, row, startingColumn1, numColumns, false, false);
   }
   
   private void formatHorizontalTitle (String title, int row,
         int startingColumn1, int numColumns,
         boolean filled, boolean borders) throws WriteException {
      final CellFormat fmt = titleCellStyle.get (filled, borders, borders, borders, borders); 
      final Label titleCell = new Label (startingColumn1, row, title, fmt);
      sheet.addCell (titleCell);
      if (numColumns > 1) {
         final Blank cell2 = new Blank (startingColumn1 + numColumns - 1, row, fmt);
         sheet.addCell (cell2);
         sheet.mergeCells (startingColumn1, row, startingColumn1 + numColumns - 1, row);
      }
   }

   private void formatVerticalTitle (String title, int column,
         int startingRow, int numRows) throws WriteException {
      final CellFormat fmt = verticalTitleCellStyle.get (false, false, false, false, false); 
      final Label titleCell = new Label (column, startingRow, title, fmt);
      sheet.addCell (titleCell);
      if (numRows > 1) {
         final Blank cell2 = new Blank (column, startingRow + numRows - 1, fmt);
         sheet.addCell (cell2);
         sheet.mergeCells (column, startingRow, column, startingRow + numRows - 1);
      }
   }

   private void formatHeader (boolean firstColumn, String[] columnNames) throws WriteException {
      if (firstColumn) {
         final CellFormat fmt = cellStyle.get (false, true, false, true);
         final Blank blank = new Blank (startingColumn, currentRow, fmt);
         sheet.addCell (blank);
      }
      for (int j = 0; j < columnNames.length; j++) {
         final CellFormat fmt = titleCellStyle.get (fillHeaders, true, true, j == 0, j == columnNames.length - 1);
         final Label label = new Label (startingColumn + j + 1, currentRow, columnNames[j], fmt);
         sheet.addCell (label);
         adjustColumnWidth (startingColumn + j + 1, columnNames[j]
               .length ());
      }
   }

   private void formatHeaderStat (boolean firstColumn) throws WriteException {
      formatHeader (firstColumn, getStatColumnNames());
      final int headerRow = currentRow;
      WritableCell ciCell = sheet.getWritableCell (startingColumn + getStatColumnNames().length, headerRow);
      ciCell.setCellFormat (titleCellStyle.get (fillHeaders, true, true, false, false));
      ciCell = new Blank (startingColumn
            + getStatColumnNames().length + 1, headerRow,
            titleCellStyle.get (fillHeaders, true, true, false, true));
      sheet.addCell (ciCell);
      sheet.mergeCells (startingColumn + getStatColumnNames().length, headerRow, startingColumn + getStatColumnNames().length + 1, headerRow);
   }

   private void adjustColumnWidth (int column, int length) {
      final int width = 256 * length + 128;
      CellView view = sheet.getColumnView (column);
      if (view == null) {
         view = new CellView ();
         view.setSize (width);
         sheet.setColumnView (column, view);
      }
      else {
         final int currentWidth = view.getSize ();
         if (currentWidth < width)
            view.setSize (width);
         sheet.setColumnView (column, view);
      }
   }

   private Label createStringCell (String val, int row,
         int column, boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight, boolean wrapText) throws WriteException {
      final CellFormat fmt =
         wrapText ?
               wrapCellStyle.get (borderTop, borderBottom, borderLeft, borderRight)
               : cellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
      final Label label = new Label (column, row, val, fmt);
      sheet.addCell (label);
      adjustColumnWidth (column, val.length ());
      return label;
   }

   private DateTime createDateCell (Date val, int row, int column,
         boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
      final CellFormat fmt = dateCellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
      final DateTime cell = new DateTime (column, row, val, fmt);
      sheet.addCell (cell);
      adjustColumnWidth (column, 15);
      return cell;
   }

   private WritableCell createTimeCell (long timeInMillis, int row, int column,
         boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
      final CellFormat fmt = timeCellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
      // final DateTime cell = new DateTime (column, row, val, fmt, true);
      final WritableCell cell = new Number (column, row, timeInMillis / (double)msInADay, fmt);
      sheet.addCell (cell);
      adjustColumnWidth (column, 15);
      return cell;
   }

   private WritableCell createValueCell (double val, boolean percent,
         TimeUnit timeUnit,
         int row, int column, boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
      WritableCell cell;
      if (Double.isNaN (val)) {
         final CellFormat fmt = cellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
         cell = new Blank (column, row, fmt);
      }
      else if (timeUnit != null) {
         final CellFormat fmt =
            timeCellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
         final long time = Math.round (TimeUnit.convert (val, timeUnit, TimeUnit.MILLISECOND));
         // cell = new DateTime (column, row, new Date (time), fmt, true);
         cell = new Number (column, row, time / (double)msInADay, fmt);
         sheet.addCell (cell);
         adjustColumnWidth (column, 15);
      }
      else { 
         final CellFormat fmt =
            percent ? percentCellStyle.get (borderTop, borderBottom, borderLeft, borderRight)
                  : numberCellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
            cell = new Number (column, row, val, fmt);
            sheet.addCell (cell);
            adjustColumnWidth (column, 15);
      }
      return cell;
   }

   private Number createIntegerCell (int val,
         int row, int column, boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
      final CellFormat fmt = integerCellStyle.get (borderTop, borderBottom, borderLeft, borderRight); 
      final Number cell = new Number (column, row, val, fmt);
      sheet.addCell (cell);
      adjustColumnWidth (column, 15);
      return cell;
   }

   private jxl.write.Boolean createBooleanCell (boolean val,
         int row, int column, boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
      final CellFormat fmt = cellStyle.get (borderTop, borderBottom, borderLeft, borderRight);
      final jxl.write.Boolean cell = new jxl.write.Boolean (column, row, val, fmt);
      sheet.addCell (cell);
      return cell;
   }
   
   /**
    * Adds a new row containing the value \texttt{val} of a string with name
    * \texttt{name}. The first column of the row contains the string
    * \texttt{name} while the second column contains \texttt{val}.
    * 
    * @param name
    *           the name of the quantity.
    * @param val
    *           the value of the quantity.
    * @param borderTop
    *           determines if a top border must be set for the cells.
    * @param borderBottom
    *           determines if a bottom border must be set for the cells.
    * @param borderBefore
    *           determines if a left border must be set for the first cell.
    * @param borderBetween
    *           determines if a border must separate the two cells.
    * @param borderAfter
    *           determines if a right border must be set for the second cell.
    * @param wrapText
    *           determines if text can be wrapped.
    */
   public void formatValueRow (String name, String val, boolean borderTop,
         boolean borderBottom, boolean borderBefore, boolean borderBetween, boolean borderAfter, boolean wrapText) throws WriteException {
      prepareRow (currentRow);
      createStringCell (name, currentRow, startingColumn, borderTop,
            borderBottom, borderBefore, borderBetween, false);
      createStringCell (val, currentRow, startingColumn + 1,
            borderTop, borderBottom, borderBetween, borderAfter, wrapText);
      ++currentRow;
   }

   /**
    * Similar to
    * {@link #formatValueRow(String,String,boolean,boolean,boolean,boolean,boolean,boolean)},
    * with \texttt{val} being a numeric value. The value is formatted with the
    * general (default) style if \texttt{percent} is \texttt{false}, or in
    * percentage notation if \texttt{percent} is \texttt{true}.
    * 
    * @param name
    *           the name of the quantity.
    * @param val
    *           the value of the quantity.
    * @param percent
    *           determines if the percentage notation must be used.
    * @param timeUnit
    *           the time unit of the formatted value, or
    *           \texttt{null} if the value does not correspond to a time.
    * @param borderTop
    *           determines if the cell containing the value has a top border.
    * @param borderBottom
    *           determines if the cell containing the value has a bottom border.
    * @param borderBefore
    *           determines if a left border must be set for the first cell.
    * @param borderBetween
    *           determines if a border must separate the two cells.
    * @param borderAfter
    *           determines if a right border must be set for the second cell.
    */
   public void formatValueRow (String name, double val, boolean percent,
         TimeUnit timeUnit, boolean borderTop, boolean borderBottom, boolean borderBefore, boolean borderBetween, boolean borderAfter) throws WriteException {
      prepareRow (currentRow);
      createStringCell (name, currentRow, startingColumn, borderTop,
            borderBottom, borderBefore, borderBetween, false);
      createValueCell (val, percent, timeUnit, currentRow, startingColumn + 1,
            borderTop, borderBottom, borderBetween, borderAfter);
      ++currentRow;
   }

   /**
    * Similar to
    * {@link #formatValueRow(String,String,boolean,boolean,boolean,boolean,boolean,boolean)},
    * with \texttt{val} being an integer.
    * 
    * @param name
    *           the name of the quantity.
    * @param value
    *           the value of the quantity.
    * @param borderTop
    *           determines if a top border must be set for the first cell.
    * @param borderBottom
    *           determines if a bottom border must be set for the first cell.
    * @param borderBefore
    *           determines if a left border must be set for the first cell.
    * @param borderBetween
    *           determines if a border must separate the two cells.
    * @param borderAfter
    *           determines if a right border must be set for the second cell.
    */
   public void formatValueRow (String name, int value,
         boolean borderTop, boolean borderBottom, boolean borderBefore, boolean borderBetween, boolean borderAfter) throws WriteException {
      prepareRow (currentRow);
      createStringCell (name, currentRow, startingColumn, borderTop,
            borderBottom, borderBefore, borderBetween, false);
      createIntegerCell (value, currentRow, startingColumn + 1,
            borderTop, borderBottom, borderBetween, borderAfter);
      ++currentRow;
   }
   
   /**
    * Similar to
    * {@link #formatValueRow(String,String,boolean,boolean,boolean,boolean,boolean,boolean)},
    * with \texttt{val} being a date.
    * 
    * @param name
    *           the name of the quantity.
    * @param val
    *           the value of the quantity.
    * @param borderTop
    *           determines if a top border must be set for the first cell.
    * @param borderBottom
    *           determines if a bottom border must be set for the first cell.
    * @param borderBefore
    *           determines if a left border must be set for the first cell.
    * @param borderBetween
    *           determines if a border must separate the two cells.
    * @param borderAfter
    *           determines if a right border must be set for the second cell.
    */
   public void formatValueRow (String name, Date val, boolean borderTop,
         boolean borderBottom, boolean borderBefore, boolean borderBetween, boolean borderAfter) throws WriteException {
      prepareRow (currentRow);
      createStringCell (name, currentRow, startingColumn, borderTop,
            borderBottom, borderBefore, borderBetween, false);
      createDateCell (val, currentRow, startingColumn + 1,
            borderTop, borderBottom, borderBetween, borderAfter);
      ++currentRow;
   }

   /**
    * Increments the current row index to leave a blank row in the current
    * spreadsheet.
    */
   public void skipRow () {
      ++currentRow;
   }
   
   private void formatValue (String desc, double avg, boolean percent, TimeUnit timeUnit, boolean first, boolean last) throws WriteException {
      formatValueRow (desc, avg, percent, timeUnit, first, last, true, true, true);
      final WritableCell cell = sheet.getWritableCell (startingColumn, currentRow - 1);
      cell.setCellFormat (cellStyle.get (fillHeaders, first, last, true, true));
   }

   /**
    * Adds a report for all the performance measures \texttt{pms} supported by
    * the evaluation system \texttt{eval} into the current spreadsheet. This
    * uses the {@link ContactCenterEval#getPerformanceMeasure} method to obtain
    * a matrix of values for each performance measure in \texttt{pms} supported
    * by \texttt{eval}. Considering the element at the bottom right of this
    * matrix as the aggregate value, the method then, for each performance
    * measure, adds a row containing the aggregate value.
    * 
    * The resulting table of results contains one row for each type of
    * performance measure, and two columns (name of measure, and value).
    * 
    * @param eval
    *           the contact center evaluation system.
    * @param pms
    *           the array of performance measures.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesSummary (ContactCenterEval eval,
         String description,
         PerformanceMeasureType... pms) throws WriteException {
      final int numRows = countRowsSummary (eval, pms);
      if (numRows == 0)
         return false;

      if (addHeaders) {
         if (description != null) {
            prepareRow (currentRow);
            formatHorizontalTitle (description,
                  currentRow++, startingColumn,
                  getValColumnNames ().length + 1);
         }
         prepareRow (currentRow);
         formatHeader (true, getValColumnNames());
         ++currentRow;
      }
      final int firstRow = currentRow;
      for (final PerformanceMeasureType pm : pms) {
         if (!isIncludedInSummary (eval, pm))
            continue;
         final TimeUnit timeUnit = pm.isTime() && eval.getDefaultUnit() != null ? eval.getDefaultUnit() : null;
         final DoubleMatrix2D avgm = eval.getPerformanceMeasure (pm);
         final String desc = pm.getDescription ();
         final double avg = avgm.get (avgm.rows () - 1, avgm.columns () - 1);
         final boolean percent = pm.isPercentage ();
         final boolean borderTop = firstRow == currentRow;
         final boolean borderBottom = firstRow + numRows - 1 == currentRow;
         formatValue (desc, avg, percent, timeUnit, borderTop, borderBottom);
      }
// if (firstRow < currentRow) {
// putThinTopBorder (firstRow, startingColumn, (int) 2);
// putThinBottomBorder (currentRow - 1, startingColumn, (int) 2);
// }
      groupRow (firstRow - 1, currentRow - 1);
      groupColumn ((startingColumn + 1), (startingColumn + 1));
      ++currentRow;
      return true;
   }

   /**
    * Adds a table containing the current values of the performance measures of
    * type \texttt{pm} estimated by the evaluation system \texttt{eval} to the
    * current spreadsheet. This method uses
    * {@link #formatValuesSingleColumn(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,String)}
    * with a matrix of values obtained via
    * {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)
    * eval.getPerformanceMeasure}, and a description obtained via
    * {@link PerformanceMeasureType#getDescription() pm.getDescription()}.
    * 
    * @param eval
    *           the contact center evaluation system.
    * @param pm
    *           the performance measure of interest.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesDetailed (ContactCenterEval eval,
         PerformanceMeasureType pm) throws WriteException {
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int rows = pmm.rows ();
      final int columns = pmm.columns ();
      final String name = pm.getDescription ();
      return formatValuesSingleColumn (eval, pm, pmm, 0, 0, rows, columns, name);
   }
   
   /**
    * Adds a table containing the current values of the performance measures of
    * type \texttt{pm} estimated by the evaluation system \texttt{eval} to the
    * current spreadsheet. This method uses
    * {@link #formatValuesMatrix(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,boolean,String)}
    * with a matrix of values obtained via
    * {@link ContactCenterEval#getPerformanceMeasure(PerformanceMeasureType)
    * eval.getPerformanceMeasure}, and a description obtained via
    * {@link PerformanceMeasureType#getDescription() pm.getDescription()}.
    * 
    * @param eval
    *           the contact center evaluation system.
    * @param pm
    *           the performance measure of interest.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesDetailedMatrix (ContactCenterEval eval,
         PerformanceMeasureType pm) throws WriteException {
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int rows = pmm.rows ();
      final int columns = pmm.columns ();
      final String name = pm.getDescription ();
      return formatValuesMatrix (eval, pm, pmm, 0, 0, rows, columns, false, name);
   }
   

   /**
    * Similar to
    * {@link #formatValuesDetailedMatrix(ContactCenterEval,PerformanceMeasureType)}
    * except per-period values are not displayed.
    * 
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measure.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesDetailedHidePeriods (ContactCenterEval eval,
         PerformanceMeasureType pm) throws WriteException {
      if (pm.getColumnType () != ColumnType.MAINPERIOD)
         return formatValuesDetailedMatrix (eval, pm);
      final DoubleMatrix2D pmm = eval.getPerformanceMeasure (pm);
      final int row = 0;
      final int column = pmm.columns () - 1;
      final int height = pmm.rows ();
      final int width = 1;
      final String name = pm.getDescription ();
      return formatValuesSingleColumn (eval, pm, pmm, row, column, height, width, name);
   }

   /**
    * Adds a table to the current spreadsheet containing the values in a matrix
    * \texttt{valm.}{@link DoubleMatrix2D#viewPart(int,int,int,int) viewPart}
    * \texttt{(row, column, height, width)} concerning performance measures of
    * type \texttt{pm} obtained with the evaluation system \texttt{eval}. The
    * string \texttt{description} provides a description for the matrix which is
    * displayed in a row preceding the table.
    * 
    * Suppose that the given matrix has dimensions $a\times b$. For example, the
    * matrix can contain averages or sample variances for different contact
    * types and periods. This method formats the results as a $ab\times 1$
    * matrix with one row for each element of \texttt{valm}. The names of rows
    * are constructed using
    * {@link #getName(ContactCenterInfo,PerformanceMeasureType,int,int)
    * getName} \texttt{(eval, pm, i, j)}.
    * 
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measures concerned.
    * @param valm
    *           the matrix of values.
    * @param row
    *           the starting row of the matrix to be formatted.
    * @param column
    *           the starting column of the matrix to be formatted.
    * @param height
    *           the height of the formatted matrix.
    * @param width
    *           the width of the formatted matrix.
    * @param description
    *           the description for the formatted matrix.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesSingleColumn (ContactCenterInfo eval,
         PerformanceMeasureType pm, DoubleMatrix2D valm, int row, int column,
         int height, int width, String description) throws WriteException {
      final DoubleMatrix2D valmPart = valm.viewPart (row, column, height, width);
      if (height == 0 || width == 0)
         return false;
      final boolean percent = pm.isPercentage ();
      final TimeUnit timeUnit = pm.isTime() && eval.getDefaultUnit() != null ? eval.getDefaultUnit() : null;
      int firstRowGroup = currentRow;
      ++startingColumn;
      try {
         if (addHeaders) {
            if (description != null) {
               prepareRow (currentRow);
               formatHorizontalTitle (description, currentRow++,
                     startingColumn,
                     (getValColumnNames().length + 1));
               ++firstRowGroup;
            }
            prepareRow (currentRow);
            formatHeader (true, getValColumnNames());
            ++currentRow;
         }

         // int firstRow = currentRow;
         for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
               String name;
               if (width == 1 && height > 1)
                  //name = capitalizeFirstLetter (pm.rowName (eval, i));
                  name = capitalizeFirstLetter (rowNameWithProperties (eval, pm, i));
               else if (width > 1 && height == 1)
                  //name = capitalizeFirstLetter (pm.columnName (eval, j));
                  name = capitalizeFirstLetter (columnNameWithProperties (eval, pm, j));
               else
                  //name = getName (eval, pm, row + i, column + j);
                  name = getNameWithProperties (eval, pm, row + i, column + j);
               final double val = valmPart.get (i, j);
               boolean borderTop, borderBottom;
               if (width == 1) {
                  borderTop = i == 0 && j == 0;
                  borderBottom = i == height - 1 && j == width - 1;
               }
               else {
                  borderTop = j == 0;
                  borderBottom = j == width - 1;
               }
               formatValue (name, val, percent, timeUnit, borderTop, borderBottom);
            }
            groupRow (currentRow - width, currentRow - 1);
         }
      }
      finally {
         --startingColumn;
      }

// if (firstRow < currentRow) {
// putThinTopBorder (firstRow, startingColumn, (int) 2);
// putThinBottomBorder (currentRow - 1, startingColumn, (int) 2);
// }
      groupRow (firstRowGroup, currentRow - 1);
      groupColumn ((startingColumn + 2), (startingColumn + 2));
      ++currentRow;
      return true;
   }

   /**
    * This is similar to
    * {@link #formatValuesSingleColumn(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,String)},
    * except that the formatted table has dimensions $a\times b$. Tables
    * obtained via this method are often more readable than with the preceding
    * one, but they can contain excessively long lines if \texttt{width} is
    * large. If \texttt{width} is greater than {@link #getMaxColumns()}, this
    * method calls the result of
    * {@link #formatValuesSingleColumn(ContactCenterInfo,PerformanceMeasureType,DoubleMatrix2D,int,int,int,int,String)}.
    * 
    * If \texttt{transposedValm} is \texttt{true}, the given matrix is
    * considered to be transposed, i.e., the meaning of its rows and columns is
    * inverted with respect to a typical matrix of performance measures of type
    * \texttt{pm}. For example, let \texttt{pm} correspond to
    * {@link PerformanceMeasureType#SERVEDRATES}. Usually, each row of
    * \texttt{valm} corresponds to a contact type. With \texttt{transposedValm}
    * enabled, each row of \texttt{valm} corresponds to an agent group. This
    * flag only affects how rows and columns are named; it does not change the
    * values being formatted.
    * 
    * @param eval
    *           the evaluation system.
    * @param pm
    *           the type of performance measures concerned.
    * @param valm
    *           the matrix of values.
    * @param row
    *           the starting row of the matrix to be formatted.
    * @param column
    *           the starting column of the matrix to be formatted.
    * @param height
    *           the height of the formatted matrix.
    * @param width
    *           the width of the formatted matrix.
    * @param transposedValm
    *           determines if \texttt{valm} is transposed with respect to a
    *           typical matrix of performance measures of type \texttt{pm}.
    * @param description
    *           the description for the formatted matrix.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatValuesMatrix (ContactCenterInfo eval,
         PerformanceMeasureType pm, DoubleMatrix2D valm, int row, int column,
         int height, int width, boolean transposedValm, String description) throws WriteException {
      final DoubleMatrix2D valmPart = valm.viewPart (row, column, height, width);
      if (height == 0 || width == 0)
         return false;
      if (width + 2 > maxColumns)
         return formatValuesSingleColumn (eval, pm, valm, row, column, height, width, description);
      final boolean percent = pm.isPercentage ();
      final TimeUnit timeUnit = pm.isTime() && eval.getDefaultUnit() != null ? eval.getDefaultUnit() : null;
      final String rowTitle, columnTitle;
      if (transposedValm) {
         rowTitle = pm.columnTitle ();
         columnTitle = pm.rowTitle ();
      }
      else {
         rowTitle = pm.rowTitle ();
         columnTitle = pm.columnTitle ();
      }
      int firstRowGroup = currentRow;
      if (addHeaders) {
         ++startingColumn;
         if (description != null) {
            prepareRow (currentRow);
            formatHorizontalTitle (description, currentRow++, startingColumn,
                  (width + 1));
            ++firstRowGroup;
         }
         prepareRow (currentRow);
         formatHorizontalTitle (columnTitle, currentRow++,
               (startingColumn + 1), width);
      }

      final String[] rowNames = new String[height];
      final String[] columnNames = new String[width];

      for (int i = 0; i < height; i++) {
         if (transposedValm)
            //rowNames[i] = pm.columnName (eval, i + column);
            rowNames[i] = columnNameWithProperties (eval, pm, i + column);
         else
            //rowNames[i] = pm.rowName (eval, i + row);
            rowNames[i] = rowNameWithProperties (eval, pm, i + row);
         rowNames[i] = capitalizeFirstLetter (rowNames[i]);
         for (int j = 0; j < width; j++) {
            if (transposedValm)
               //columnNames[j] = pm.rowName (eval, j + row);
               columnNames[j] = rowNameWithProperties (eval, pm, j + row);
            else
               //columnNames[j] = pm.columnName (eval, j + column);
               columnNames[j] = columnNameWithProperties (eval, pm, j + column);
            columnNames[j] = capitalizeFirstLetter (columnNames[j]);
         }
      }
      prepareRow (currentRow);
      formatHeader (true, columnNames);
      ++currentRow;
      for (int i = 0; i < height; i++) {
         prepareRow (currentRow);
         final boolean borderTop = i == 0;
         final boolean borderBottom = i == height - 1;
         final WritableCell cell = createStringCell (rowNames[i], currentRow, startingColumn, borderTop, borderBottom, true, true, false);
         cell.setCellFormat (cellStyle.get (fillHeaders, borderTop, borderBottom, true, true));

         for (int j = 0; j < width; j++) {
            final double val = valmPart.get (i, j);
            final int col = startingColumn + j + 1;
            createValueCell (val, percent, timeUnit, currentRow, col, borderTop, borderBottom, j == 0, j == width - 1);
         }
         ++currentRow;
      }
      final int firstColumnGroup = startingColumn + 1;
      final int lastColumnGroup = startingColumn + width;
// putThinTopBorder (currentRow - height, startingColumn,
// (int) (width + 1));
// putThinBottomBorder (currentRow - 1, startingColumn, (int) (width + 1));
      if (addHeaders) {
         --startingColumn;
         formatVerticalTitle (rowTitle, startingColumn, currentRow - height,
               height);
      }
      groupRow (firstRowGroup, currentRow - 1);
      groupColumn (firstColumnGroup, lastColumnGroup);
      ++currentRow;
      return true;
   }

   private void formatStatRow (DoubleMatrix2D avgm, DoubleMatrix2D varm,
         DoubleMatrix2D minm, DoubleMatrix2D maxm, DoubleMatrix2D[] ci,
         int row, int col, String name, boolean percent, TimeUnit timeUnit, boolean firstColumn, boolean borderTop, boolean borderBottom) throws WriteException {
      if (firstColumn) {
         final Label cell = createStringCell (name, currentRow, startingColumn, borderTop, borderBottom, true, true, false);
         cell.setCellFormat (cellStyle.get (fillHeaders, borderTop, borderBottom, true, true));
      }

      final double min = minm == null ? Double.NaN : minm.get (row, col);
      createValueCell (min, percent, timeUnit, currentRow, (startingColumn + 1),
            borderTop, borderBottom, true, false);
      final double max = maxm == null ? Double.NaN : maxm.get (row, col);
      createValueCell (max, percent, timeUnit, currentRow, (startingColumn + 2),
            borderTop, borderBottom, false, false);
      final double avg = avgm == null ? Double.NaN : avgm.get (row, col);
      createValueCell (avg, percent, timeUnit, currentRow, (startingColumn + 3),
            borderTop, borderBottom, false, false);
      final double var = varm == null ? Double.NaN : varm.get (row, col);
      createValueCell (Math.sqrt (var), percent, timeUnit, currentRow,
            (startingColumn + 4), borderTop, borderBottom, false, false);
      final double lower = ci == null || ci[0] == null ? Double.NaN : ci[0].get (row,
            col);
      createValueCell (lower, percent, timeUnit, currentRow, (startingColumn + 5),
            borderTop, borderBottom, false, false);
      final double upper = ci == null || ci[1] == null ? Double.NaN : ci[1].get (row,
            col);
      createValueCell (upper, percent, timeUnit, currentRow, (startingColumn + 6),
            borderTop, borderBottom, false, true);
   }

   /**
    * Adds a statistical report for all the performance measures in \texttt{pms}
    * supported by the contact center simulator \texttt{sim} to the current
    * spreadsheet. This is similar to {@link #formatValuesSummary}, with
    * additional statistical information such as miminum, maximum, standard
    * deviation, and confidence intervals with confidence level \texttt{level},
    * if available.
    * 
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the confidence level of the confidence intervals.
    * @param pms
    *           the array of performance measures.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatStatisticsSummary (ContactCenterSim sim, double level,
         String description,
         PerformanceMeasureType... pms) throws WriteException {
      final int numRows = countRowsSummary (sim, pms);
      if (numRows == 0)
         return false;
      if (addHeaders) {
         if (description != null) {
            prepareRow (currentRow);
            formatHorizontalTitle (description,
                  currentRow++, startingColumn,
                  (getStatColumnNames ().length + 2));
         }
         prepareRow (currentRow);
         formatHeaderStat (true);
         ++currentRow;
      }
      final int firstRow = currentRow;
      DoubleMatrix2D avgm = null;
      DoubleMatrix2D varm = null;
      DoubleMatrix2D minm = null;
      DoubleMatrix2D maxm = null;
      DoubleMatrix2D[] ci = null;
      for (final PerformanceMeasureType pm : pms) {
         if (!isIncludedInSummary (sim, pm))
            continue;
         final String name = pm.getDescription ();
         avgm = sim.getPerformanceMeasure (pm);
         if (avgm.rows () == 0 || avgm.columns () == 0)
            continue;

         try {
            varm = sim.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {
            varm = null;
         }
         try {
            minm = sim.getMin (pm);
         }
         catch (final NoSuchElementException nse) {
            minm = null;
         }
         try {
            maxm = sim.getMax (pm);
         }
         catch (final NoSuchElementException nse) {
            maxm = null;
         }
         try {
            ci = sim.getConfidenceInterval (pm, level);
         }
         catch (final NoSuchElementException nse) {
            ci = null;
         }
         final boolean percent = pm.isPercentage ();
         final TimeUnit timeUnit = pm.isTime() && sim.getDefaultUnit() != null ? sim.getDefaultUnit() : null;
         final boolean borderTop = firstRow == currentRow;
         final boolean borderBottom = firstRow + numRows - 1 == currentRow;
         prepareRow (currentRow);
         formatStatRow (avgm, varm, minm, maxm, ci, avgm.rows () - 1, avgm
               .columns () - 1, name, percent, timeUnit, true, borderTop, borderBottom);
         ++currentRow;
      }
// if (firstRow < currentRow) {
// putThinTopBorder (firstRow, startingColumn,
// (int) (getStatColumnNames().length + 2));
// putThinBottomBorder (currentRow - 1, startingColumn,
// (int) (getStatColumnNames().length + 2));
// }
      groupRow (firstRow - 1, currentRow - 1);
      groupColumn ((startingColumn + 1), (startingColumn + getStatColumnNames().length + 1));
      groupColumn ((startingColumn + 1), (startingColumn + 2));
      groupColumn ((startingColumn + 4), (startingColumn + 6));
      ++currentRow;
      return true;
   }

   /**
    * Adds a statistical report for all the values of the performance measure
    * \texttt{pm} estimated by the simulator \texttt{sim}, with confidence
    * intervals with level \texttt{level}, to the current spreadsheet.
    * 
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the level of confidence intervals.
    * @param pm
    *           the performance measure of interest.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatStatisticsDetailed (ContactCenterSim sim, double level,
         PerformanceMeasureType pm) throws WriteException {
      final DoubleMatrix2D avgm = sim.getPerformanceMeasure (pm);
      if (avgm.rows () == 0 || avgm.columns () == 0)
         return false;
      int firstRowGroup = currentRow;
      final String description = pm.getDescription ();
      ++startingColumn;
      try {
         if (addHeaders) {
            if (description != null) {
               prepareRow (currentRow);
               formatHorizontalTitle (description, currentRow++, startingColumn,
                     (getStatColumnNames().length + 2));
               ++firstRowGroup;
            }
            prepareRow (currentRow);
            formatHeaderStat (true);
            ++currentRow;
         }
         DoubleMatrix2D varm = null;
         DoubleMatrix2D minm = null;
         DoubleMatrix2D maxm = null;
         DoubleMatrix2D[] ci = null;

         try {
            varm = sim.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            minm = sim.getMin (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            maxm = sim.getMax (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            ci = sim.getConfidenceInterval (pm, level);
         }
         catch (final NoSuchElementException nse) {}

         final boolean percent = pm.isPercentage ();
         final TimeUnit timeUnit = pm.isTime() && sim.getDefaultUnit() != null ? sim.getDefaultUnit() : null;
         // int firstRow = currentRow;
         for (int i = 0; i < avgm.rows (); i++)
            for (int j = 0; j < avgm.columns (); j++) {
               boolean borderTop, borderBottom;
               if (avgm.columns() == 1) {
                  borderTop = i == 0 && j == 0;
                  borderBottom = i == avgm.rows() - 1 && j == avgm.columns() - 1;
               }
               else {
                  borderTop = j == 0;
                  borderBottom = j == avgm.columns () - 1;
               }
               final String name = getName (sim, pm, i, j);
               prepareRow (currentRow);
               formatStatRow (avgm, varm, minm, maxm, ci, i, j, name, percent, timeUnit, true, borderTop, borderBottom);
               ++currentRow;
            }
// if (firstRow < currentRow) {
// putThinTopBorder (firstRow, startingColumn,
// (int) (getStatColumnNames().length + 2));
// putThinBottomBorder (currentRow - 1, startingColumn,
// (int) (getStatColumnNames().length + 2));
// }
         groupRow (firstRowGroup, currentRow - 1);
         groupColumn ((startingColumn + 1), (startingColumn + getStatColumnNames().length + 1));
         groupColumn ((startingColumn + 1), (startingColumn + 2));
         groupColumn ((startingColumn + 4), (startingColumn + 6));
         ++currentRow;
      }
      finally {
         --startingColumn;
      }
      return true;
   }

   public boolean formatStatisticsDetailedMatrix (ContactCenterSim sim, double level,
         PerformanceMeasureType pm) throws WriteException {
      final DoubleMatrix2D avgm = sim.getPerformanceMeasure (pm);
      if (avgm.rows () == 0 || avgm.columns () == 0)
         return false;
      if (avgm.columns ()*(getStatColumnNames ().length + 1) + 2 > maxColumns)
         return formatStatisticsDetailed (sim, level, pm);
      int firstRowGroup = currentRow;
      final String description = pm.getDescription ();
      final int numStatColumns = getStatColumnNames ().length + 1;
      final int width = numStatColumns*avgm.columns (); 
      int initialStartingColumn = startingColumn;
      if (addHeaders) {
         ++startingColumn;
         ++initialStartingColumn;
         if (description != null) {
            prepareRow (currentRow);
            formatHorizontalTitle (description, currentRow++, startingColumn,
                  (width + 1));
            ++firstRowGroup;
         }
         prepareRow (currentRow);
         formatHorizontalTitle (pm.columnTitle(),
               currentRow++,
               (startingColumn + 1), width);
         for (int j = 0; j < avgm.columns (); j++) {
            //final String name = capitalizeFirstLetter (pm.columnName (sim, j));
            final String name = capitalizeFirstLetter (columnNameWithProperties (sim, pm, j));
            final int sc = initialStartingColumn + numStatColumns*j + 1;
            formatHorizontalTitle (name, currentRow,
                  sc, numStatColumns, fillHeaders, true); 
         }
         ++currentRow;
         prepareRow (currentRow);
         for (int j = 0; j < avgm.columns (); j++) {
            startingColumn = initialStartingColumn + numStatColumns*j;
            formatHeaderStat (j == 0);
         }
         startingColumn = initialStartingColumn;
         ++currentRow;
      }
      
      DoubleMatrix2D varm = null;
      DoubleMatrix2D minm = null;
      DoubleMatrix2D maxm = null;
      DoubleMatrix2D[] ci = null;

      try {
         varm = sim.getVariance (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         minm = sim.getMin (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         maxm = sim.getMax (pm);
      }
      catch (final NoSuchElementException nse) {}
      try {
         ci = sim.getConfidenceInterval (pm, level);
      }
      catch (final NoSuchElementException nse) {}

      final boolean percent = pm.isPercentage ();
      final TimeUnit timeUnit = pm.isTime() && sim.getDefaultUnit() != null ? sim.getDefaultUnit() : null;
      // int firstRow = currentRow;
      for (int i = 0; i < avgm.rows (); i++) {
         prepareRow (currentRow);
         final boolean borderTop = i == 0;
         final boolean borderBottom = i == avgm.rows () - 1;
         for (int j = 0; j < avgm.columns (); j++) {
            startingColumn = initialStartingColumn + numStatColumns*j;
            //final String name = capitalizeFirstLetter (pm.rowName (sim, i));
            final String name = capitalizeFirstLetter (rowNameWithProperties (sim, pm, i));
            formatStatRow (avgm, varm, minm, maxm, ci, i, j, name, percent, timeUnit, j == 0, borderTop, borderBottom);
         }
         ++currentRow;
      }
      startingColumn = initialStartingColumn;
      groupRow (firstRowGroup, currentRow - 1);
      for (int j = 0; j < avgm.columns (); j++) {
         final int sc = startingColumn + numStatColumns*j;
         groupColumn ((sc + 1), (sc + getStatColumnNames().length + 1));
         groupColumn ((sc + 1), (sc + 2));
         groupColumn ((sc + 4), (sc + 6));
      }
      if (addHeaders) {
         --startingColumn;
         formatVerticalTitle (pm.rowTitle (),
               startingColumn, currentRow - avgm.rows (),
               avgm.rows ());
      }
      ++currentRow;
      return true;
   }
   
   /**
    * Similar to
    * {@link #formatStatisticsDetailedMatrix(ContactCenterSim,double,PerformanceMeasureType)}
    * but does not format per-period statistics.
    * 
    * @param sim
    *           the contact center simulator.
    * @param level
    *           the confidence level of the intervals.
    * @param pm
    *           the type of performance measures.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatStatisticsDetailedHidePeriods (ContactCenterSim sim,
         double level, PerformanceMeasureType pm) throws WriteException {
      if (pm.getColumnType () != ColumnType.MAINPERIOD)
         return formatStatisticsDetailedMatrix (sim, level, pm);
      final DoubleMatrix2D avgm = sim.getPerformanceMeasure (pm);
      if (avgm.rows () == 0 || avgm.columns () == 0)
         return false; 
      int firstRowGroup = currentRow;
      final String description = pm.getDescription ();
      ++startingColumn;
      try {
         if (addHeaders) {
            if (description != null) {
               prepareRow (currentRow);
               formatHorizontalTitle (description, currentRow++, startingColumn,
                     (getStatColumnNames().length + 2));
               ++firstRowGroup;
            }
            prepareRow (currentRow);
            formatHeaderStat (true);
            ++currentRow;
         }
         DoubleMatrix2D varm = null;
         DoubleMatrix2D minm = null;
         DoubleMatrix2D maxm = null;
         DoubleMatrix2D[] ci = null;

         try {
            varm = sim.getVariance (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            minm = sim.getMin (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            maxm = sim.getMax (pm);
         }
         catch (final NoSuchElementException nse) {}
         try {
            ci = sim.getConfidenceInterval (pm, level);
         }
         catch (final NoSuchElementException nse) {}

         // int firstRow = currentRow;
         final int j = avgm.columns () - 1;
         final boolean percent = pm.isPercentage ();
         final TimeUnit timeUnit = pm.isTime() && sim.getDefaultUnit() != null ? sim.getDefaultUnit() : null;
         for (int i = 0; i < avgm.rows (); i++) {
            //final String name = capitalizeFirstLetter (pm.rowName (sim, i));
            final String name = capitalizeFirstLetter (rowNameWithProperties (sim, pm, i));
            final boolean borderTop = i == 0;
            final boolean borderBottom = i == avgm.rows () - 1;
            prepareRow (currentRow);
            formatStatRow (avgm, varm, minm, maxm, ci, i, j, name, percent, timeUnit, true, borderTop, borderBottom);
            ++currentRow;
         }
// if (firstRow < currentRow) {
// putThinTopBorder (firstRow, startingColumn,
// (int) (getStatColumnNames().length + 2));
// putThinBottomBorder (currentRow - 1, startingColumn,
// (int) (getStatColumnNames().length + 2));
// }
         groupRow (firstRowGroup, currentRow - 1);
         groupColumn ((startingColumn + 1), (startingColumn + getStatColumnNames().length + 1));
         groupColumn ((startingColumn + 1), (startingColumn + 2));
         groupColumn ((startingColumn + 4), (startingColumn + 6));
         ++currentRow;
      }
      finally {
         --startingColumn;
      }
      return true;
   }
   
   /**
    * Appends rows containing the evaluation information \texttt{info} to the
    * current spreadsheet. For each entry in the given map, this method creates
    * a row containing one cell for the key, and a second cell for the value.
    * Values are formatted as follows. Any \texttt{null} reference becomes the
    * string \texttt{null}, instances of {@link  Number} are turned into numeric
    * cells, and instances of {@link Date} are converted to numeric cells
    * formatted as dates. Any other non-\texttt{null} value is formatted using
    * the {@link #toString()} method to become a string cell.
    * 
    * @param info
    *           the evaluation information.
    * @return \texttt{true} if and only if the sheet was modified.
    */
   public boolean formatInfo (Map<String, Object> info) throws WriteException {
      for (final Map.Entry<String, Object> e : info.entrySet ()) {
         final String key = e.getKey ();
         final Object value = e.getValue ();
         prepareRow (currentRow);
         createStringCell (key, currentRow, startingColumn, false,
               false, false, false, false);
         format (startingColumn + 1, value);
         ++currentRow;
      }
      return !info.isEmpty();
   }
   
   private void format (int column, Object value) throws WriteException {
      if (value instanceof Boolean)
         createBooleanCell (((java.lang.Boolean)value).booleanValue (), currentRow, column, false, false, false, false);
      else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal)
         createValueCell (((java.lang.Number)value).doubleValue (), false, null, currentRow, column, false, false, false, false);
      else if (value instanceof java.lang.Number)
         createIntegerCell (((java.lang.Number)value).intValue (), currentRow, column, false, false, false, false);
         //createValueCell (((java.lang.Number)value).intValue (), false, null, currentRow, column, false, false, false, false);
      else if (value instanceof Time)
         createTimeCell (((Date)value).getTime (), currentRow, column, false, false, false, false);
      else if (value instanceof Date)
         createDateCell ((Date)value, currentRow, column, false, false, false, false);
      else if (value instanceof Duration)
         createTimeCell (((Duration)value).getTimeInMillis (new Date()), currentRow, column, false, false, false, false);
      else if (value != null && value.getClass ().isArray ()) {
         final int length = Array.getLength (value);
         int sc = column;
         for (int i = 0; i < length; i++) {
            if (i > 0 && sc > maxColumns) {
               prepareRow (++currentRow);
               sc = column;
            }
            final Object e = Array.get (value, i);
            if (e != null && e.getClass ().isArray ())
               format (sc, Arrays.toString (makeArrayOfObjects (e)));
            else
               format (sc, e);
            ++sc;
         }
      }
      else
         createStringCell (value == null ? "null" : value.toString (), currentRow, column, false, false, false, false, true);
   }
   
   private Object[] makeArrayOfObjects (Object value) {
      final int length = Array.getLength (value);
      final Object[] v = new Object[length];
      for (int i = 0; i < length; i++)
         v[i] = Array.get (value, i);
      return v;
   }
   
   private static final RowType[] SOURCEROWTYPES = {
      RowType.INBOUNDTYPE, RowType.INBOUNDTYPEAWT,
      RowType.OUTBOUNDTYPE,
      RowType.CONTACTTYPE            
   };
   private static final RowType[] DESTINATIONROWTYPES = {
      RowType.AGENTGROUP,
      RowType.WAITINGQUEUE
   };
   
   private RowType[] getSourceRowTypes() {
      return SOURCEROWTYPES;
   }
   
   public RowType[] getDestinationRowTypes() {
      return DESTINATIONROWTYPES;
   }
   
   /**
    * For each element {@link PerformanceMeasureParams}
    * returned by {@link ReportParams#getPrintedObs()},
    * formats the complete list of observations
    * generated by the simulator \texttt{sim}
    * for the referred performance measure.
    * Each performance measure is formatted as a separate
    * column in the current sheet.
    * @param sim the simulator to get observations from.
    * @param reportParams the report parameters.
    * @return \texttt{true} if the current sheet is modified
    * by this operation.
    * @throws WriteException if an error happens
    * when writing to the current sheet.
    */
   public boolean formatObservations (ContactCenterSimWithObservations sim, ReportParams reportParams)
   throws WriteException {
      if (!reportParams.isSetPrintedObs ())
         return false;
      int idx = 0;
      int maxNumObs = 0;
      for (final PerformanceMeasureParams ppar : reportParams.getPrintedObs ()) {
         final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ppar.getMeasure ());
         if (idx == maxColumns) {
            currentRow += maxNumObs + 1;
            maxNumObs = 0;
         }
         final StringBuilder sb = new StringBuilder();
         sb.append (pm.getDescription ());
         int row, column;
         if (ppar.isSetRow () || ppar.isSetColumn ())
            sb.append (" (");
         if (ppar.isSetRow ()) {
            row = ppar.getRow ();
            if (row < 0)
               row += pm.rows (sim);
            sb.append (pm.rowName (sim, row));
         }
         else
            row = pm.rows (sim) - 1;
         if (ppar.isSetColumn ()) {
            if (ppar.isSetRow ())
               sb.append (", ");
            column = ppar.getColumn ();
            if (column < 0)
               column += pm.columns (sim);
            sb.append (pm.columnName (sim, column));
         }
         else
            column = pm.columns (sim) - 1;
         if (ppar.isSetRow () || ppar.isSetColumn ())
            sb.append (')');
         createStringCell (sb.toString (), currentRow, startingColumn + idx,
               true, true, true, true, false);
         double[] obs;
         try {
            obs = sim.getObs (pm, row, column);
            /*
          	if (ppar.isSetHistogram()) {
               double std = getStandardDeviation (sim, pm, row, column);
               createHistogram(obs, std, ppar, sb.toString ());
          	}
            */
         }
         catch (final NoSuchElementException nse) {
            final String noObs = Messages.getString ("PerformanceMeasureFormat.NoObs");
            createStringCell (noObs, currentRow + 1, startingColumn + idx,
                  true, true, true, true, false);
            if (maxNumObs < 1)
               maxNumObs  = 1;
            ++idx;
            continue;
         }
         for (int i = 0; i < obs.length; i++)
            createValueCell (obs[i], pm.isPercentage (),
                  pm.isTime () ?
                  sim.getDefaultUnit () : null,
                  currentRow + i + 1,
                  startingColumn + idx,
                  false, i == obs.length - 1,
                  true, true);
         if (obs.length > maxNumObs)
            maxNumObs = obs.length;
         ++idx;
      }
      currentRow += maxNumObs + 1;
      return true;
   }

   /**
    * Formats a workbook containing the report of the last evaluation performed
    * by the system \texttt{eval}. This method can be called by the
    * implementation of
    * {@link ContactCenterEval#formatStatisticsExcel(WritableWorkbook)}.
    * 
    * Assuming that the current workbook is empty, this method creates a new
    * sheet with name given by \texttt{summarySheetName}, and calls
    * {@link #formatInfo(Map)} with the evaluation information of \texttt{eval}
    * to add the information to the new sheet. It then formats a summary report
    * using
    * {@link #formatValuesSummary(ContactCenterEval,String,PerformanceMeasureType[])}
    * into the same sheet. The method then creates a second sheet with name
    * given by \texttt{detailedSheetNameWithoutPeriods} for the detailed report.
    * For each performance measure a detailed report is reuqested for, the
    * method calls
    * {@link #formatValuesDetailedHidePeriods(ContactCenterEval,PerformanceMeasureType)}.
    * Finally, the method creates a third sheet with name given by
    * \texttt{detailedSheetNameWithPeriods} and containing detailed information
    * with individual periods, using
    * {@link #formatStatisticsDetailed(ContactCenterSim,double,PerformanceMeasureType)}
    * to create the new cells. The two last sheets are not created if they would
    * be empty. The creation of any of the third sheet can be disabled by giving
    * a \texttt{null} or empty name for that sheet. Information that would be
    * presented on an omitted sheet is written on the next requested sheet.
    * 
    * The types of performance measures to include in the report are selected
    * using \texttt{printedStats} which can be \texttt{null} or empty; in these
    * two latter cases, the report includes all performance measures supported
    * by \texttt{eval}. Each element of \texttt{printedStats} specifies a type
    * of performance measure to include in the report (if supported by
    * \texttt{eval}), whether a detailed report must be included, and if this
    * detailed report includes information about each individual period.
    * 
    * @param eval
    *           the evaluation system.
    * @param reportParams
    *           the report parameters.
    */
   public boolean formatValues (
         ContactCenterEval eval,
         ReportParams reportParams) throws WriteException {
      final PrintedStatParams[] printedStats = reportParams.getPrintedStats ().toArray (new PrintedStatParams[0]);
      PrintedStatParams[] pstats;
      if (printedStats == null || printedStats.length == 0)
         pstats = getDefaultPrintedStatParams (eval, reportParams);
      else
         pstats = printedStats;
      final String summarySheetName = reportParams.getSummarySheetName () == null ? defaultSummarySheetName
            : reportParams.getSummarySheetName ();
      final String detailedSheetNameWithoutPeriods = reportParams.getDetailedSheetNameWithoutPeriods () == null ?
            defaultDetailedSheetNameWithoutPeriods : reportParams.getDetailedSheetNameWithoutPeriods ();
      final String detailedSheetNameWithPeriods = reportParams.getDetailedSheetNameWithPeriods () == null ?
            defaultDetailedSheetNameWithPeriods : reportParams.getDetailedSheetNameWithPeriods ();
      
      rowOutlines = false;
      columnOutlines = false;
      boolean hasSummarySheet;
      if (summarySheetName != null && summarySheetName.length () > 0) {
         newSheet (summarySheetName);
         if (!eval.getEvalInfo ().isEmpty ()) {
            formatInfo (eval.getEvalInfo ());
            skipRow ();
         }
         final int firstRow = currentRow;
         final boolean sfmt = formatValuesSummary (eval, "Source", getPerformanceMeasures
               (pstats, getSourceRowTypes ()));
         final int lastRow = currentRow;
         if (sfmt) {
            currentRow = firstRow;
            startingColumn += getValColumnNames ().length + 2;
            rowOverwrite = true;
         }
         formatValuesSummary (eval, "Destination", getPerformanceMeasures
               (pstats, getDestinationRowTypes ()));
         if (sfmt) {
            rowOverwrite = false;
            startingColumn -= getValColumnNames ().length + 2;
            currentRow = Math.max (currentRow, lastRow);
         }
         hasSummarySheet = true;
      }
      else
         hasSummarySheet = false;

      // Create the last two sheets only if needed
      rowOutlines = true;
      boolean hasDetailedSheet;
      if (detailedSheetNameWithoutPeriods != null && detailedSheetNameWithoutPeriods.length () > 0) {
         boolean sheetCreated = false;
         if (!hasSummarySheet && !eval.getEvalInfo ().isEmpty ()) {
            newSheet (detailedSheetNameWithoutPeriods);
            sheetCreated = true;
            ++startingColumn;
            formatInfo (eval.getEvalInfo ());
            --startingColumn;
            skipRow ();
         }
         for (final PrintedStatParams ps : pstats) {
            if (!isIncludedInDetailedSheetWithoutPeriods (eval, reportParams, ps, hasSummarySheet))
               continue;
            final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
            if (!sheetCreated) {
               newSheet (detailedSheetNameWithoutPeriods);
               sheetCreated = true;
            }
            formatValuesDetailedHidePeriods (eval, pm);
         }
         hasDetailedSheet = true;
      }
      else
         hasDetailedSheet = false;
      if (detailedSheetNameWithPeriods != null && detailedSheetNameWithPeriods.length () > 0) {
         boolean sheetCreated = false;
         if (!hasSummarySheet && !hasDetailedSheet && !eval.getEvalInfo ().isEmpty ()) {
            newSheet (detailedSheetNameWithPeriods);
            sheetCreated = true;
            ++startingColumn;
            formatInfo (eval.getEvalInfo ());
            --startingColumn;
            skipRow ();
         }
         for (final PrintedStatParams ps : pstats) {
            if (!isIncludedInDetailedSheetWithPeriods (eval, reportParams, ps, hasSummarySheet, hasDetailedSheet))
               continue;
            final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
            if (!sheetCreated) {
               newSheet (detailedSheetNameWithPeriods);
               sheetCreated = true;
            }
            final boolean psPeriods = ps.isSetPeriods () ? ps.isPeriods () : reportParams.isDefaultPeriods ();
            if (psPeriods)
               formatValuesDetailedMatrix (eval, pm);
            else
               formatValuesDetailedHidePeriods (eval, pm);
         }
      }
      return true;
   }
   
   private boolean isIncludedInDetailedSheetWithoutPeriods (ContactCenterEval eval, ReportParams reportParams, PrintedStatParams ps, boolean hasSummarySheet) {
      boolean psDetailed = ps.isSetDetailed () ? ps.isDetailed () : reportParams.isDefaultDetailed();
      if (!psDetailed)
         return false;
      final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
      // If the performance matrix is 1x1, it is not worth displaying it
      // since the information is all contained in the summary report.
      if (!isIncludedInReport (eval, pm))
         return false;
      final int rows = pm.rows (eval);
      final int columns = pm.columns (eval);
      final boolean periodColumn = pm.getColumnType () == ColumnType.MAINPERIOD;
      if (hasSummarySheet && isIncludedInSummary (eval, pm))
         if (rows <= 1 && (periodColumn || columns <= 1))
            return false;
      return true;
   }
   
   private boolean isIncludedInDetailedSheetWithPeriods (ContactCenterEval eval, ReportParams reportParams, PrintedStatParams ps, boolean hasSummarySheet, boolean hasDetailedSheet) {
      boolean psDetailed = ps.isSetDetailed () ? ps.isDetailed () : reportParams.isDefaultDetailed();
      if (!psDetailed)
         return false;
      final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
      // If the performance matrix is 1x1, it is not worth displaying it
      // since the information is all contained in the summary report.
      if (!isIncludedInReport (eval, pm))
         return false;
// boolean inSummary = hasSummarySheet && isIncludedInSummary (eval, pm);
  // boolean inDetailed = hasDetailedSheet &&
   // isIncludedInDetailedSheetWithoutPeriods (eval, ps, hasSummarySheet);
      final int rows = pm.rows (eval);
      final int columns = pm.columns (eval);
      final boolean periodColumn = pm.getColumnType () == ColumnType.MAINPERIOD;
      final boolean psPeriods = ps.isSetPeriods () ? ps.isPeriods () : reportParams.isDefaultPeriods ();
      if (periodColumn && psPeriods && columns > 1)
         return true;
      else {
         if (hasDetailedSheet)
            return false;
         if (rows <= 1 && hasSummarySheet && isIncludedInSummary (eval, pm))
            return false;
      }
      return true;
// if (hasDetailedSheet)
// if (!periodColumn || columns <= 1)
// // All the information is already on the preceding sheet
// return false;
// if (hasDetailedSheet || (hasSummarySheet && isIncludedInSummary (eval, pm)))
// {
// if (!ps.getPeriods () && periodColumn &&
// rows == 1)
// return false;
// if (ps.getPeriods () && rows == 1 && columns == 1)
// return false;
// }
// return true;
   }   
   
   /**
    * Similar to {@link #formatValues(ContactCenterEval,ReportParams)}, except
    * this formats a full statistical report using
    * {@link #formatStatisticsSummary(ContactCenterSim,double,String,PerformanceMeasureType[])},
    * and
    * {@link #formatStatisticsDetailed(ContactCenterSim,double,PerformanceMeasureType)}.
    * If the given report parameters contains information
    * about observations to print,
    * the method also calls
    * {@link #formatObservations(ContactCenterSimWithObservations,ReportParams)},
    * and creates a sheet with the results. 
    * 
    * @param sim
    *           the contact center simulator.
    * @param reportParams
    *           the report parameters.
    * @return the current workbook.
    */
   public boolean formatStatistics (
         ContactCenterSim sim,
         ReportParams reportParams) throws WriteException {
      final PrintedStatParams[] printedStats = reportParams.getPrintedStats ().toArray (new PrintedStatParams[0]);
      PrintedStatParams[] pstats;
      if (printedStats == null || printedStats.length == 0)
         pstats = getDefaultPrintedStatParams (sim, reportParams);
      else
         pstats = printedStats;
      final String summarySheetName = reportParams.getSummarySheetName () == null ? defaultSummarySheetName
            : reportParams.getSummarySheetName ();
      final String detailedSheetNameWithoutPeriods = reportParams.getDetailedSheetNameWithoutPeriods () == null ?
            defaultDetailedSheetNameWithoutPeriods : reportParams.getDetailedSheetNameWithoutPeriods ();
      final String detailedSheetNameWithPeriods = reportParams.getDetailedSheetNameWithPeriods () == null ?
            defaultDetailedSheetNameWithPeriods : reportParams.getDetailedSheetNameWithPeriods ();
      final String obsSheetName = reportParams.getObsSheetName () == null ?
            defaultObsSheetName : reportParams.getObsSheetName ();
      final double level = reportParams.getConfidenceLevel ();

      rowOutlines = false;
      columnOutlines = false;
      boolean hasSummarySheet;
      if (summarySheetName != null && summarySheetName.length () > 0) {
         newSheet (summarySheetName);
         final int numValColumns = getValColumnNames ().length;
         final int numStatColumns = getStatColumnNames ().length + 1;
         if (!sim.getEvalInfo ().isEmpty ()) {
            formatInfo (sim.getEvalInfo ());
            skipRow ();
         }
         int nc = 2;
         final int firstRow = currentRow;
         boolean sfmt = formatValuesSummary (sim, "Source", getPerformanceMeasures
               (pstats, true, getSourceRowTypes ()));
         if (sfmt)
            nc = numValColumns + 1;
         final boolean sfmt2 = formatStatisticsSummary (sim, level, "Source", getPerformanceMeasures
               (pstats, false, getSourceRowTypes ()));
         if (sfmt2)
            nc = numStatColumns + 1;
         sfmt |= sfmt2;
         final int lastRow = currentRow;
         if (sfmt) {
            currentRow = firstRow;
            startingColumn += nc + 1;
            rowOverwrite = true;
         }
         formatValuesSummary (sim, "Destination", getPerformanceMeasures
               (pstats, true, getDestinationRowTypes ()));
         formatStatisticsSummary (sim, level, "Destination", getPerformanceMeasures
               (pstats, false, getDestinationRowTypes ()));
         if (sfmt) {
            currentRow = Math.max (currentRow, lastRow);
            startingColumn -= getStatColumnNames ().length + 3;
            rowOverwrite = false;
         }
         hasSummarySheet = true;
      }
      else
         hasSummarySheet = false;

      boolean hasDetailedSheet;
      rowOutlines = true;
      if (detailedSheetNameWithoutPeriods != null && detailedSheetNameWithoutPeriods.length () > 0) {
         // Create the last two sheets only if needed
         boolean sheetCreated = false;
         if (!hasSummarySheet && !sim.getEvalInfo ().isEmpty ()) {
            newSheet (detailedSheetNameWithoutPeriods);
            sheetCreated = true;
            ++startingColumn;
            formatInfo (sim.getEvalInfo ());
            --startingColumn;
            skipRow ();
         }
         for (final PrintedStatParams ps : pstats) {
            if (!isIncludedInDetailedSheetWithoutPeriods (sim, reportParams, ps, hasSummarySheet))
               continue;
            final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
            if (!sheetCreated) {
               newSheet (detailedSheetNameWithoutPeriods);
               sheetCreated = true;
            }
            final boolean psOnlyAverages = ps.isSetOnlyAverages () ? ps.isOnlyAverages () : reportParams.isDefaultOnlyAverages ();
            if (psOnlyAverages)
               formatValuesDetailedHidePeriods (sim, pm);
            else
               formatStatisticsDetailedHidePeriods (sim, level, pm);
         }
         hasDetailedSheet = true;
      }
      else
         hasDetailedSheet = false;
      if (detailedSheetNameWithPeriods != null && detailedSheetNameWithPeriods.length () > 0) {
         boolean sheetCreated = false;
         if (!hasSummarySheet && !hasDetailedSheet && !sim.getEvalInfo ().isEmpty ()) {
            newSheet (detailedSheetNameWithPeriods);
            sheetCreated = true;
            ++startingColumn;
            formatInfo (sim.getEvalInfo ());
            --startingColumn;
            skipRow ();
         }
         for (final PrintedStatParams ps : pstats) {
            if (!isIncludedInDetailedSheetWithPeriods (sim, reportParams, ps, hasSummarySheet, hasDetailedSheet))
               continue;
            final PerformanceMeasureType pm = PerformanceMeasureType.valueOf (ps.getMeasure ());
            if (!sheetCreated) {
               newSheet (detailedSheetNameWithPeriods);
               sheetCreated = true;
            }
            final boolean psPeriods = ps.isSetPeriods () ? ps.isPeriods () : reportParams.isDefaultPeriods ();
            final boolean psOnlyAverages = ps.isSetOnlyAverages () ? ps.isOnlyAverages () : reportParams.isDefaultOnlyAverages ();
            if (psPeriods) {
               if (psOnlyAverages)
                  formatValuesDetailedMatrix (sim, pm);
               else
                  formatStatisticsDetailedMatrix (sim, level, pm);
            }
            else if (psOnlyAverages)
               formatValuesDetailedHidePeriods (sim, pm);
            else
               formatStatisticsDetailedHidePeriods (sim, level, pm);
         }
      }
      if (obsSheetName != null && obsSheetName.length () > 0 && sim instanceof ContactCenterSimWithObservations && reportParams.isSetPrintedObs ()) {
         newSheet (obsSheetName);
         formatObservations ((ContactCenterSimWithObservations)sim, reportParams);
      }
      return true;
   }
   
   private static abstract class CellStyleManager {
      private final WritableCellFormat[] styles = new WritableCellFormat[16];
      private final WritableCellFormat[] stylesFilled = new WritableCellFormat[16];
      
      public abstract WritableCellFormat createBasicCellStyle() throws WriteException;
      
      public CellFormat get (boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
         return get (false, borderTop, borderBottom, borderLeft, borderRight);
      }
      
      public CellFormat get (boolean filled, boolean borderTop, boolean borderBottom, boolean borderLeft, boolean borderRight) throws WriteException {
         final int index = (borderRight ? 1 : 0) + 2*(borderLeft ? 1 : 0) + 4*(borderBottom ? 1 : 0) + 8*(borderTop ? 1 : 0);
         final WritableCellFormat[] st = filled ? stylesFilled : styles;
         if (st[index] == null) {
            st[index] = createBasicCellStyle ();
            st[index].setBorder (Border.TOP, borderTop ? BorderLineStyle.THIN : BorderLineStyle.NONE);
            st[index].setBorder (Border.BOTTOM, borderBottom ? BorderLineStyle.THIN : BorderLineStyle.NONE);
            st[index].setBorder (Border.LEFT, borderLeft ? BorderLineStyle.THIN : BorderLineStyle.NONE);
            st[index].setBorder (Border.RIGHT, borderRight ? BorderLineStyle.THIN : BorderLineStyle.NONE);
            if (filled)
               st[index].setBackground (Colour.GRAY_50);
         }
         return st[index];
      }
   }
   
   private class GeneralCellStyleManager extends CellStyleManager {
      @Override
      public WritableCellFormat createBasicCellStyle() throws WriteException {
         return new WritableCellFormat();
      }
   }

   private class TitleCellStyleManager extends CellStyleManager {
      @Override
      public WritableCellFormat createBasicCellStyle() throws WriteException {
         return createTitleCellStyle ();
      }
   }

   private class VerticalTitleCellStyleManager extends CellStyleManager {
      @Override
      public WritableCellFormat createBasicCellStyle() throws WriteException {
         return createVerticalTitleCellStyle ();
      }
   }
   
   private class WrapCellStyleManager extends CellStyleManager {
      @Override
      public WritableCellFormat createBasicCellStyle() throws WriteException {
         final WritableCellFormat style = new WritableCellFormat();
         style.setWrap (true);
         return style;
      }
   }
   
   private class NumberCellStyleManager extends CellStyleManager {
      private DisplayFormat format;
      
      public NumberCellStyleManager (DisplayFormat format) {
         this.format = format;
      }
      
      @Override
      public WritableCellFormat createBasicCellStyle() throws WriteException {
         return new WritableCellFormat (format);
      }
   }
}
