package umontreal.iro.lecuyer.xmlbind;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
public class CSVSourceArray2D implements SourceArray2D {
   private URL url;
   private String[][] data;
   
   public CSVSourceArray2D (URL url) {
      this.url = url;
      init();
   }
   
   public CSVSourceArray2D (Node node, String uri) throws URISyntaxException, MalformedURLException {
      final String baseURI = node.getBaseURI ();
      URI uriObject;
      if (baseURI == null)
         uriObject = new URI (uri);
      else {
         final URI baseURIObject = new URI (baseURI);
         uriObject = baseURIObject.resolve (uri);
      }
      url = uriObject.toURL ();
      init();
   }
   
   /**
    * Returns the URL of the text file containing
    * the values of the array, in CSV format.
    * @return the URL of the CSV data.
    */   
   public URL getURL() {
      return url;
   }
   
   public int columns (int row) {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      if (row < 0)
         throw new IllegalArgumentException
         ("Negative row index");
      return data[row].length;
   }

   public <T> T get (Class<T> pcls, int row, int column) {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      try {
         return StringConvert.fromString (null, null, pcls, data[row][column]);
      }
      catch (final NameConflictException nce) {
         final ClassCastException iae = new ClassCastException
         ("Cannot convert value at (" + row + ", " + column + ")=" + data[row][column] + " from string to " + pcls.getName());
         iae.initCause (nce);
         throw iae;
      }
      catch (final UnsupportedConversionException uce) {
         throw new ClassCastException
         ("Cannot convert value at (" + row + ", " + column + ")=" + data[row][column] + " from string to " + pcls.getName());
      }
      catch (final IllegalArgumentException iaeIn) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Cannot convert value at (" + row + ", " + column + ")=" + data[row][column] + " from string to " + pcls.getName());
         iae.initCause (iaeIn);
         throw iae;
      }
   }

   private void init () {
      if (url == null)
         throw new IllegalStateException
         ("The URL of the data file must be specified");
      try {
         data = TextDataReader.readCSVData (url, ',', '\"');
      }
      catch (final IOException ioe) {
         throw new IllegalArgumentException
         ("Could not read text data URL " + url.toString ());
      }
   }

   public int rows () {
      if (data == null)
         throw new IllegalStateException
         ("Uninitialized matrix");
      return data.length;
   }

   public void close() {
      data = null;
   }
}
