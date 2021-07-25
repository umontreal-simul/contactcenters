package umontreal.iro.lecuyer.xmlconfig;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import jxl.BooleanCell;
import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a source array whose contents is
 * read from a Microsoft Excel workbook.
 * Each row of the sheet with the given name, and
 * contained in the workbook pointed to by
 * a URL becomes a row of the source array,
 * with elements of the row separated
 * using commas.
 * Data is read using JExcel API. 
 * 
 * In the XML file, the \texttt{URL} attribute
 * of an element representing a Excel source
 * array
 * must be used to indicate the
 * URL of the data file.
 * The \texttt{sheetName} attribute can also be used
 * to indicate the name of a sheet.
 * By default, the first sheet is read.
 */
public class ExcelSourceArray2D extends AbstractParam implements SourceArray2D,
      Cloneable, StorableParam {
   private static final Map<URL, Workbook> wbCache = new WeakHashMap<URL, Workbook>(); 
   private URL url;
   private String sheetName;
   private Workbook wb;
   private Sheet sheet;
   
   /**
    * Returns the URL of the text file containing
    * the values of the array, in CSV format.
    * @return the URL of the CSV data.
    */   
   public URL getURL() {
      return url;
   }
   
   /**
    * Sets the URL pointing to the CSV file
    * containing the 
    * elements of this
    * array to
    * \texttt{url}.
    * @param url the URL of the data.
    */
   public void setURL (URL url) {
      this.url = url;
   }
   
   /**
    * Returns the name of the sheet to extract data from.
    * @return the name of the sheet.
    */
   public String getSheetName() {
      return sheetName;
   }
   
   /**
    * Sets the sheet name to \texttt{sheetName}.
    * @param sheetName the sheet name.
    */
   public void setSheetName (String sheetName) {
      this.sheetName = sheetName;
   }
   
   public int columns (int row) {
      if (sheet == null)
         throw new IllegalStateException
         ("Uninitialized source array");
      return sheet.getColumns ();
   }

   @SuppressWarnings("unchecked")
   public <T> T get (Class<T> pcls, int row, int column) throws UnsupportedConversionException {
      if (sheet == null)
         throw new IllegalStateException
         ("Uninitialized source array");
      final Cell cell = sheet.getCell (column, row);
      if (cell == null)
         return null;
      String str = null;
      if (cell.getType () == CellType.NUMBER ||
            cell.getType () == CellType.NUMBER_FORMULA) {
         final NumberCell nc = (NumberCell)cell;
         final double v = nc.getValue ();
         if (pcls == double.class || pcls == Double.class)
            return (T)new Double (v);
         else if (pcls == float.class || pcls == Float.class)
            return (T)new Float ((float)v);
         else if (pcls == int.class || pcls == Integer.class)
            return (T)new Integer ((int)Math.round (v));
         else if (pcls == long.class || pcls == Long.class)
            return (T)new Long (Math.round (v));
         else if (pcls == byte.class || pcls == Byte.class)
            return (T)new Byte ((byte)Math.round (v));
         else if (pcls == short.class || pcls == Short.class)
            return (T)new Short ((short)Math.round (v));
         else if (pcls == BigInteger.class)
            return (T)BigInteger.valueOf (Math.round (v));
         else if (pcls == BigDecimal.class)
            return (T)new BigDecimal (v);
         else
            str = String.valueOf (v);
      }
      else if (cell.getType () == CellType.BOOLEAN ||
            cell.getType () == CellType.BOOLEAN_FORMULA) {
         final BooleanCell bc = (BooleanCell)cell;
         final boolean v = bc.getValue ();
         if (pcls == Boolean.class)
            return (T)new Boolean (v);
         else
            str = String.valueOf (v);
      }
      else if (cell.getType () == CellType.DATE ||
            cell.getType () == CellType.DATE_FORMULA) {
         final DateCell dc = (DateCell)cell;
         final Date v = dc.getDate ();
         if (pcls == Date.class)
            return (T)v;
      }
      else if (cell.getType () == CellType.EMPTY) {
         final double v = 0;
         if (pcls == double.class || pcls == Double.class)
            return (T)new Double (v);
         else if (pcls == float.class || pcls == Float.class)
            return (T)new Float ((float)v);
         else if (pcls == int.class || pcls == Integer.class)
            return (T)new Integer ((int)Math.round (v));
         else if (pcls == long.class || pcls == Long.class)
            return (T)new Long (Math.round (v));
         else if (pcls == byte.class || pcls == Byte.class)
            return (T)new Byte ((byte)Math.round (v));
         else if (pcls == short.class || pcls == Short.class)
            return (T)new Short ((short)Math.round (v));
         else if (pcls == BigInteger.class)
            return (T)BigInteger.valueOf (Math.round (v));
         else if (pcls == BigDecimal.class)
            return (T)new BigDecimal (v);
      }
         
      if (str == null)
         str = cell.getContents ();
      if (pcls == String.class)
         return (T)str;
      try {
         return StringConvert.fromString (null, null, pcls, str);
      }
      catch (final NameConflictException nce) {
         final UnsupportedConversionException iae = new UnsupportedConversionException
         ("Cannot convert value at (" + row + ", " + column + ")=" + str);
         iae.initCause (nce);
         throw iae;
      }
      catch (final NumberFormatException nfe) {
         final UnsupportedConversionException uce = new UnsupportedConversionException
         ("Cannot convert value at (" + row + ", " + column + ")=" + str);
         uce.initCause (nfe);
         throw uce;
      }
   }

   public void init () {
      wb = wbCache.get (url);
      if (wb == null)
         try {
            final InputStream is = url.openStream ();
            final WorkbookSettings wbs = new WorkbookSettings();
            wbs.setSuppressWarnings (true);
            wbs.setLocale (Locale.ENGLISH);
            wb = Workbook.getWorkbook (is, wbs);
            wbCache.put (url, wb);
         }
         catch (final IOException ioe) {
            final IllegalArgumentException iae = new IllegalArgumentException
            ("Could not read workbook");
            iae.initCause (ioe);
            throw iae;
         }
         catch (final BiffException be) {
            final IllegalArgumentException iae = new IllegalArgumentException
            ("Could not read workbook");
            iae.initCause (be);
            throw iae;
         }
      if (sheetName == null || sheetName.length () == 0) {
         sheet = wb.getSheet (0);
         if (sheet == null) {
            wb.close();
            wb = null;
            throw new IllegalArgumentException
               ("No sheet in the workbook");
         }
      }
      else {
         sheet = wb.getSheet (sheetName);
         if (sheet == null) {
            wb.close();
            wb = null;
            throw new IllegalArgumentException
               ("No sheet with name " + sheetName + " in the workbook");
         }
      }
   }

   public int rows () {
      if (sheet == null)
         throw new IllegalStateException
         ("Uninitialized source array");
      return sheet.getRows ();
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName, true, spc);
      if (url != null)
         el.setAttribute ("URL", url.toString ());
      if (sheetName != null)
         el.setAttribute ("sheetName", sheetName);
      return el;
   }

   public void dispose() {
      sheet = null;
      wb = null;
   }
   
   @Override
   public ExcelSourceArray2D clone() {
      ExcelSourceArray2D cpy;
      try {
         cpy = (ExcelSourceArray2D)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError
         ("Clone not supported for a class implementing Cloneable");
      }
      return cpy;
   }
   
   public String getElementName() {
      return "Excel";
   }
   
   public static void clearCache() {
      for (final Workbook wb : wbCache.values ())
         wb.close ();
      wbCache.clear ();
   }
}
