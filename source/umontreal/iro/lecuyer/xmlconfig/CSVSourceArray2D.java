package umontreal.iro.lecuyer.xmlconfig;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.ssj.util.TextDataReader;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a source array whose contents is
 * read from a CSV-formatted text file.
 * Each line of the text file pointed to by
 * a URL becomes a row of the source array,
 * with elements of the row separated
 * using commas. 
 * Text is read using {@link TextDataReader#readCSVData(URL,char,char)}
 * with \texttt{,} as the column delimiter and
 * \texttt{"} as the string delimiter,
 * while {@link StringConvert#fromString(URI,ClassFinder,Class,String)}
 * is used to convert strings to target objects.
 * 
 * In the XML file, the \texttt{URL} attribute
 * of an element representing a CSV source
 * array
 * must be used to indicate the
 * URL of the CSV data file.
 */
public class CSVSourceArray2D extends AbstractParam implements SourceArray2D,
      Cloneable, StorableParam {
   private URL url;
   private String[][] data;
   
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
   
//   public void setURL (ParamReader reader, String url) {
//      try {
//         this.url = reader.baseURI.resolve (url).toURL ();
//      }
//      catch (final MalformedURLException me) {
//         throw new ParamReadException
//         ("Could not get the URL for the text data");
//      }
//   }
   
   public int columns (int row) {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      if (row < 0)
         throw new IllegalArgumentException
         ("Negative row index");
      return data[row].length;
   }

   public <T> T get (Class<T> pcls, int row, int column) throws UnsupportedConversionException {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      try {
         return StringConvert.fromString (null, null, pcls, data[row][column]);
      }
      catch (final NameConflictException nce) {
         final UnsupportedConversionException iae = new UnsupportedConversionException
         ("Cannot convert value at (" + row + ", " + column + ")=" + data[row][column]);
         iae.initCause (nce);
         throw iae;
      }
   }

   public void init () {
      if (url == null)
         throw new IllegalStateException
         ("The URL of the data file must be specified");
      try {
         data = TextDataReader.readCSVData (url, ',', '\"');
      }
      catch (final IOException ioe) {
         throw new ParamReadException
         ("Could not read text data URL " + url.toString ());
      }
   }

   public int rows () {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      return data.length;
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedElement (parent, elementName, true, spc);
      if (url != null)
         el.setAttribute ("URL", url.toString ());
      return el;
   }

   public void dispose() {
      data = null;
   }
   
   @Override
   public CSVSourceArray2D clone() {
      CSVSourceArray2D cpy;
      try {
         cpy = (CSVSourceArray2D)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError
         ("Clone not supported for a class implementing Cloneable");
      }
      return cpy;
   }
   
   public String getElementName() {
      return "CSV";
   }
}
