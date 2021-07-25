package umontreal.iro.lecuyer.xmlbind;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import jxl.BooleanCell;
import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Range;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.w3c.dom.Node;

import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a source array whose contents is read from a Microsoft Excel
 * workbook. Each row of the sheet with the given name, and contained in the
 * workbook pointed to by a URL becomes a row of the source array, with elements
 * of the row separated using commas. Data is read using JExcel API.
 * 
 * In the XML file, the \texttt{URL} attribute of an element representing a
 * Excel source array must be used to indicate the URL of the data file. The
 * \texttt{sheetName} attribute can also be used to indicate the name of a
 * sheet. By default, the first sheet is read.
 */
public class ExcelSourceArray2D {
   private URL url;
   private Workbook wb;
   private static DatatypeFactory datatypeFactory;

   public ExcelSourceArray2D (URL url) {
      this.url = url;
      init ();
   }

   public ExcelSourceArray2D (Node node, String uri) throws URISyntaxException,
         MalformedURLException {
      String baseURI = node.getBaseURI ();
      URI uriObject;
      if (baseURI == null)
         baseURI = node.getOwnerDocument().getDocumentURI();
      if (baseURI == null)
         uriObject = new URI (uri);
      else {
         final URI baseURIObject = new URI (baseURI);
         uriObject = baseURIObject.resolve (uri);
      }
      url = uriObject.toURL ();
      init ();
   }

   /**
    * Returns the URL of the text file containing the values of the array, in
    * CSV format.
    * 
    * @return the URL of the CSV data.
    */
   public URL getURL () {
      return url;
   }

   private void init () {
      if (url == null)
         throw new NullPointerException
         ("No URL specified");
      try {
         if (datatypeFactory == null)
            datatypeFactory = DatatypeFactory.newInstance ();
      }
      catch (final DatatypeConfigurationException dce) {
         throw new IllegalArgumentException
         ("Cannot configure data type factory");
      }
      try {
         final InputStream is = url.openStream ();
         final WorkbookSettings wbs = new WorkbookSettings ();
         wbs.setSuppressWarnings (true);
         wbs.setLocale (Locale.ENGLISH);
         wb = Workbook.getWorkbook (is, wbs);
      }
      catch (final IOException ioe) {
         final IllegalArgumentException iae = new IllegalArgumentException (
               "Could not read workbook " + url.toString ());
         iae.initCause (ioe);
         throw iae;
      }
      catch (final BiffException be) {
         final IllegalArgumentException iae = new IllegalArgumentException (
               "Could not read workbook " + url.toString ());
         iae.initCause (be);
         throw iae;
      }
   }
   
   /**
    * Closes the workbook associated with this
    * object.
    */
   public void close() {
      wb.close ();
      wb = null;
   }

   /**
    * Constructs and returns a source array corresponding to
    * the sheet with index sheetIndex in the workbook.
    * @param sheetIndex the sheet index.
    * @return the source array corresponding to the sheet.
    */
   public SourceArray2D getSheet (int sheetIndex) {
      return getSheet (wb.getSheet (sheetIndex));
   }

   /**
    * Constructs and returns a source array corresponding to
    * the sheet with name sheetName in the workbook.
    * @param sheetName the sheet name.
    * @return the source array corresponding to the sheet.
    */
   public SourceArray2D getSheet (String sheetName) {
      return getSheet (wb.getSheet (sheetName));
   }

   private SourceArray2D getSheet (Sheet sheet) {
      return new SheetArray (sheet);
   }
   
   public SourceArray2D getNamedArea (String area) {
      final Range[] ranges = wb.findByName(area);
      if (ranges == null)
         throw new IllegalArgumentException
         ("Cannot find named area with name "  + area);
      if (ranges.length > 1)
         throw new IllegalArgumentException
         ("Unsupported non-adjacent ranges");
      final Range range = ranges[0];
      final Cell topLeft = range.getTopLeft();
      final Cell bottomRight = range.getBottomRight();
      final SourceArray2D sheetArray = getSheet (range.getFirstSheetIndex());
      return new SourceSubset2D
      (sheetArray, topLeft.getRow(), topLeft.getColumn(),
            bottomRight.getRow() - topLeft.getRow() + 1,
            bottomRight.getColumn() - topLeft.getColumn() + 1, true);
   }

   private static class SheetArray implements SourceArray2D {
      private Sheet sheet;

      public SheetArray (Sheet sheet) {
         if (sheet == null)
            throw new IllegalArgumentException
            ("Invalid sheet specified");
         this.sheet = sheet;
      }

      public int rows () {
         if (sheet == null)
            throw new IllegalStateException ("Uninitialized source array");
         return sheet.getRows ();
      }

      public int columns (int row) {
         if (sheet == null)
            throw new IllegalStateException ("Uninitialized source array");
         return sheet.getColumns ();
      }

      @SuppressWarnings ("unchecked")
      public <T> T get (Class<T> pcls, int row, int column) {
         if (sheet == null)
            throw new IllegalStateException ("Uninitialized source array");
         final Cell cell = sheet.getCell (column, row);
         if (cell == null)
            return null;
         String str = null;
         if (cell.getType () == CellType.NUMBER
               || cell.getType () == CellType.NUMBER_FORMULA) {
            final NumberCell nc = (NumberCell) cell;
            final double v = nc.getValue ();
            if (pcls == double.class || pcls == Double.class)
               return (T) new Double (v);
            else if (pcls == float.class || pcls == Float.class)
               return (T) new Float ((float) v);
            else if (pcls == int.class || pcls == Integer.class)
               return (T) new Integer ((int) Math.round (v));
            else if (pcls == long.class || pcls == Long.class)
               return (T) new Long (Math.round (v));
            else if (pcls == byte.class || pcls == Byte.class)
               return (T) new Byte ((byte) Math.round (v));
            else if (pcls == short.class || pcls == Short.class)
               return (T) new Short ((short) Math.round (v));
            else if (pcls == BigInteger.class)
               return (T) BigInteger.valueOf (Math.round (v));
            else if (pcls == BigDecimal.class)
               return (T) new BigDecimal (v);
            else if (pcls == Duration.class)
               return (T) datatypeFactory.newDuration (Math.round (v*3600.0*1000.0));
            else
               str = String.valueOf (v);
         }
         else if (cell.getType () == CellType.BOOLEAN
               || cell.getType () == CellType.BOOLEAN_FORMULA) {
            final BooleanCell bc = (BooleanCell) cell;
            final boolean v = bc.getValue ();
            if (pcls == Boolean.class)
               return (T) new Boolean (v);
            else
               str = String.valueOf (v);
         }
         else if (cell.getType () == CellType.DATE
               || cell.getType () == CellType.DATE_FORMULA) {
            final DateCell dc = (DateCell) cell;
            final Date v = dc.getDate ();
            if (pcls == Date.class)
               return (T) v;
            else if (pcls == Duration.class)
               return (T)datatypeFactory.newDuration (v.getTime ());
            else if (pcls == XMLGregorianCalendar.class) {
               final GregorianCalendar gcal = new GregorianCalendar();
               gcal.setTime (v);
               return (T)datatypeFactory.newXMLGregorianCalendar (gcal);
            }
         }
         else if (cell.getType () == CellType.EMPTY) {
            final double v = 0;
            if (pcls == double.class || pcls == Double.class)
               return (T) new Double (v);
            else if (pcls == float.class || pcls == Float.class)
               return (T) new Float ((float) v);
            else if (pcls == int.class || pcls == Integer.class)
               return (T) new Integer ((int) Math.round (v));
            else if (pcls == long.class || pcls == Long.class)
               return (T) new Long (Math.round (v));
            else if (pcls == byte.class || pcls == Byte.class)
               return (T) new Byte ((byte) Math.round (v));
            else if (pcls == short.class || pcls == Short.class)
               return (T) new Short ((short) Math.round (v));
            else if (pcls == BigInteger.class)
               return (T) BigInteger.valueOf (Math.round (v));
            else if (pcls == BigDecimal.class)
               return (T) new BigDecimal (v);
            else if (pcls == Duration.class)
               return (T)datatypeFactory.newDuration (0);
            else if (pcls == XMLGregorianCalendar.class) {
               final GregorianCalendar gcal = new GregorianCalendar();
               gcal.setTimeInMillis (0);
               return (T)datatypeFactory.newXMLGregorianCalendar (gcal);
            }
         }

         if (str == null)
            str = cell.getContents ();
         if (pcls == String.class)
            return pcls.cast (str);
         try {
            return StringConvert.fromString (null, null, pcls, str);
         }
         catch (final NameConflictException ne) {
            final ClassCastException cce = new ClassCastException
            ("Cannot convert cell (" + row + ", " + column + ")=" + str +
                  ", on sheet " + sheet.getName() + ", to " + pcls.getName());
            cce.initCause (ne);
            throw cce;
         }
         catch (final UnsupportedConversionException uce) {
            final ClassCastException cce = new ClassCastException
            ("Cannot convert cell (" + row + ", " + column + ")=" + str +
                  ", on sheet " + sheet.getName() + ", to " + pcls.getName());
            cce.initCause (uce);
            throw cce;
         }
         catch (final IllegalArgumentException iaeIn) {
            final IllegalArgumentException iae = new IllegalArgumentException
            ("Cannot convert cell (" + row + ", " + column + ")=" + str +
                  ", on sheet " + sheet.getName() + ", to " + pcls.getName());
            iae.initCause (iaeIn);
            throw iae;
            
         }
      }

      public void close () {
         sheet = null;
      }
   }
}
