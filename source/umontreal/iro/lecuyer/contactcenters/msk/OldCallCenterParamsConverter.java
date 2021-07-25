package umontreal.iro.lecuyer.contactcenters.msk;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import umontreal.iro.lecuyer.contactcenters.app.OldSimParamsConverter;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.CSVSourceArray2D;
import umontreal.iro.lecuyer.xmlbind.DBSourceArray2D;
import umontreal.iro.lecuyer.xmlbind.ExcelSourceArray2D;
import umontreal.iro.lecuyer.xmlbind.SourceArray2D;
import umontreal.iro.lecuyer.xmlbind.SourceSubset2D;
import umontreal.iro.lecuyer.xmlbind.params.DBConnectionParams;
import umontreal.iro.lecuyer.xmlbind.params.StringProperty;

public class OldCallCenterParamsConverter {
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.msk");
   private TimeUnit defaultUnit;
   private double startingTimeInSec;
   private double periodDurationInSec;
   private int numPeriods;
   
   private URI baseURI;
   private final Map<String, CSVSourceArray2D> csvCache = new HashMap<String, CSVSourceArray2D>();
   private final Map<String, ExcelSourceArray2D> excelCache = new HashMap<String, ExcelSourceArray2D>();
   private final Map<String, DBSourceArray2D> dbCache = new HashMap<String, DBSourceArray2D>();
   
   // Used only to create nodes
   private Document tmpDoc;
   
   public OldCallCenterParamsConverter (Node root, String baseURI) {
      if (root == null)
         throw new NullPointerException
         ("Cannot find root element MSKCCParams");
      try {
         this.baseURI = new URI (baseURI);
      }
      catch (final URISyntaxException use) {
         logger.warning ("Invalid base URI " + baseURI);
         this.baseURI = new File (".").toURI();
      }
      final DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance ();
      try {
         final DocumentBuilder dBuilder = bFactory.newDocumentBuilder ();
         tmpDoc = dBuilder.newDocument ();
      }
      catch (final ParserConfigurationException pce) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Error creating document builder");
         iae.initCause (pce);
         throw iae;
      }
      
      final Element el = (Element)root;
      try {
         if (el.hasAttribute ("defaultUnit"))
            defaultUnit = TimeUnit.valueOf (el.getAttribute ("defaultUnit"));
         else
            defaultUnit = TimeUnit.HOUR;
      }
      catch (final IllegalArgumentException iae) {
         logger.severe ("Error converting the default time unit");
         throw iae;
      }
      try {
         if (el.hasAttribute ("numPeriods"))
            numPeriods = Integer.parseInt (el.getAttribute("numPeriods"));
         else
            numPeriods = 0;
      }
      catch (final IllegalArgumentException iae) {
         logger.severe ("Error converting the number of periods");
         throw iae;
      }
      try {
         if (el.hasAttribute ("startingTime"))
            startingTimeInSec = OldSimParamsConverter.getTime (el.getAttribute ("startingTime"), TimeUnit.SECOND, defaultUnit); 
         else if (el.hasAttribute ("startTime"))
            startingTimeInSec = OldSimParamsConverter.getTime (el.getAttribute ("startTime"), TimeUnit.SECOND, defaultUnit);
      }
      catch (final IllegalArgumentException iae) {
         logger.severe ("Error converting the starting time");
         throw iae;
      }
      try {
         if (el.hasAttribute ("periodDuration"))
            periodDurationInSec = OldSimParamsConverter.getTime (el.getAttribute ("periodDuration"), TimeUnit.SECOND, defaultUnit);
         else
            periodDurationInSec = 0;
      }
      catch (final IllegalArgumentException iae) {
         logger.severe ("Error converting the period duration");
         throw iae;
      }
   }
   
   public String getStartingTime() {
      return OldSimParamsConverter.toDuration (startingTimeInSec);
   }
   
   public String getPeriodDuration() {
      return OldSimParamsConverter.toDuration (periodDurationInSec);
   }
   
   public String timeToDuration (String timeStr) {
      final double timeInSec = OldSimParamsConverter.getTime (timeStr, TimeUnit.SECOND, defaultUnit);
      if (Double.isInfinite (timeInSec))
         return OldSimParamsConverter.toDuration (10*numPeriods*periodDurationInSec);
      return OldSimParamsConverter.toDuration (timeInSec);
   }
   
   public static String getTextContent (Node node, boolean includeChildren) {
      final StringBuilder sb = new StringBuilder();
      final NodeList children = node.getChildNodes ();
      for (int j = 0; j < children.getLength (); j++) {
         final Node child = children.item (j);
         if (child.getNodeType() == Node.TEXT_NODE ||
               child.getNodeType() == Node.CDATA_SECTION_NODE) {
            if (sb.length () > 0)
               sb.append (" ");
            final String val = child.getNodeValue ();
            if (val != null)
               sb.append (val.trim ());
         }
         else if (includeChildren && child.getNodeType() == Node.ELEMENT_NODE)
            sb.append (getTextContent (child, includeChildren));
      }
      return sb.toString ();
   }
   
   public String convertValue (String value, String typeString) {
      if (typeString.equals ("string"))
         return value;
      else if (typeString.equals ("int"))
         return value;
      else if (typeString.equals ("double")) {
         if (value.equalsIgnoreCase ("NaN"))
            return "NaN";
         if (value.equalsIgnoreCase ("infinity") ||
               value.equalsIgnoreCase ("inf"))
            return "INF";
         if (value.equalsIgnoreCase ("-infinity") ||
               value.equalsIgnoreCase ("-inf"))
            return "-INF";
         return value;
      }
      else if (typeString.equals ("duration"))
         return timeToDuration (value);
      else {
         logger.warning ("Unrecognized type string " + typeString + "; ignoring");
         return value;
      }
   }
   
   private SourceArray2D createSubset (Element el, SourceArray2D sourceArray) {
      final Element parent = (Element) el.getParentNode ();
      boolean hasAttr = false;
      final int startingRow;
      if (parent.hasAttribute ("startingRow")) {
         startingRow = Integer.parseInt (parent.getAttribute ("startingRow"));
         hasAttr = true;
      }
      else
         startingRow = 0;
      final int startingColumn;
      if (parent.hasAttribute ("startingColumn")) {
         startingColumn = Integer.parseInt (parent
               .getAttribute ("startingColumn"));
         hasAttr = true;
      }
      else
         startingColumn = 0;
      final int numRows;
      if (parent.hasAttribute ("numRows")) {
         numRows = Integer.parseInt (parent.getAttribute ("numRows"));
         hasAttr = true;
      }
      else
         numRows = Integer.MAX_VALUE;
      final int numColumns;
      if (parent.hasAttribute ("numColumns")) {
         numColumns = Integer.parseInt (parent.getAttribute ("numColumns"));
         hasAttr = true;
      }
      else
         numColumns = Integer.MAX_VALUE;
      final boolean transposed;
      if (parent.hasAttribute ("transposed")) {
         transposed = Boolean.parseBoolean (parent.getAttribute ("transposed"));
         hasAttr = true;
      }
      else
         transposed = false;

      if (hasAttr)
         return new SourceSubset2D (sourceArray, startingRow, startingColumn,
               numRows, numColumns, transposed);
      else
         return sourceArray;
   }
   
   private SourceArray2D getCSVArray (Element csvChild) {
      final String url = csvChild.getAttribute ("URL");
      CSVSourceArray2D array = csvCache.get (url);
      if (array == null) {
         array = new CSVSourceArray2D (getURL (url));
         csvCache.put (url, array);
      }

      return createSubset (csvChild, array);
   }
   
   private URL getURL (String url) {
      final URI uri = baseURI.resolve (url);
      try {
         return uri.toURL();
      }
      catch (final MalformedURLException mue) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Cannot convert URI to URL");
         iae.initCause (mue);
         throw iae;
      }
   }
   
   private SourceArray2D getExcelArray (Element excelChild) {
      final String url = excelChild.getAttribute ("URL");
      ExcelSourceArray2D array = excelCache.get (url);
      if (array == null) {
         array = new ExcelSourceArray2D (getURL (url));
         excelCache.put (url, array);
      }

      final String sheetName = excelChild.getAttribute ("sheetName");
      final SourceArray2D sourceArray = array.getSheet (sheetName);
      return createSubset (excelChild, sourceArray);
   }
   
   private SourceArray2D getDBArray (Element dbChild) {
      final DBConnectionParams dbParams = new DBConnectionParams();
      if (dbChild.hasAttribute ("jndiDataSourceName"))
         dbParams.setJndiDataSourceName (dbChild.getAttribute ("jndiDataSourceName"));
      if (dbChild.hasAttribute ("jdbcDriverClass"))
         dbParams.setJdbcDriverClass (dbChild.getAttribute ("jdbcDriverClass"));
      if (dbChild.hasAttribute ("jdbcURI"))
         dbParams.setJdbcURI (dbChild.getAttribute ("jdbcURI"));
      String key;
      if (dbParams.getJndiDataSourceName() == null)
         key = dbParams.getJdbcURI();
      else
         key = dbParams.getJndiDataSourceName();         
      DBSourceArray2D array = dbCache.get (key);
      if (array == null) {
         final NodeList propChildren = dbChild.getElementsByTagName ("property");
         for (int j = 0; j < propChildren.getLength(); j++) {
            final Element propChild = (Element)propChildren.item (j);
            final String name = propChild.getAttribute ("name");
            String value;
            if (propChild.hasAttribute ("value"))
               value = propChild.getAttribute ("value");
            else
               value = null;
            final StringProperty sp = new StringProperty();
            sp.setName (name);
            if (value != null)
               sp.setValue (value);
            dbParams.getProperties().getPropertyList().add (sp);
         }
         array = new DBSourceArray2D (dbParams);
         dbCache.put (key, array);
      }
      
      final String query = dbChild.getAttribute ("dataQuery");
      final SourceArray2D sourceArray = array.getQuery (query);
      return createSubset (dbChild, sourceArray);
   }
   
   private Class<?> getTargetClass (String typeString) {
      if (typeString.equals ("string"))
         return String.class;
      else if (typeString.equals ("int"))
         return Integer.class;
      else if (typeString.equals ("double"))
         return Double.class;
      else if (typeString.equals ("duration"))
         return Duration.class;
      else {
         logger.warning ("Unrecognized type string " + typeString + "; using string");
         return String.class;
      }
   }
   
   private String getValue (SourceArray2D sourceArray, Class<?> targetClass, int r, int c) {
      if (targetClass == Duration.class) {
         // Hack for durations
         try {
            final Object o = sourceArray.get (targetClass, r, c);
            if (o == null)
               return "null";
            return o.toString();
         }
         catch (final IllegalArgumentException iae) {}
         catch (final ClassCastException cce) {}
         
         final String timeStr = sourceArray.get (String.class, r, c);
         return timeToDuration (timeStr);
      }
      final Object o = sourceArray.get (targetClass, r, c);
      if (o == null)
         return "null";
      if (o instanceof Double) {
         final double v = (Double)o;
         if (v < 0 && Double.isInfinite (v))
            return "-INF";
         if (v > 0 && Double.isInfinite (v))
            return "INF";
         if ((int)v == v)
            return String.valueOf ((int)v);
      }
      return o.toString();
   }
   
   public Node getArray (SourceArray2D sourceArray, String nodeName, String typeString) {
      final Class<?> targetClass = getTargetClass (typeString);
      final StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (int r = 0; r < sourceArray.rows(); r++)
         for (int c = 0; c < sourceArray.columns (r); c++) {
            if (first)
               first = false;
            else
               sb.append(' ');
            sb.append (getValue (sourceArray, targetClass, r, c));
         }
      
      final Node output = tmpDoc.createElement (nodeName);
      output.appendChild (tmpDoc.createTextNode (sb.toString()));
      return output;
   }
   
   public Node convertArray (Node input, String typeString) {
      if (input == null)
         return tmpDoc.createTextNode ("");
      final Element inputEl = (Element)input;
      final NodeList csvChildren = inputEl.getElementsByTagName ("CSV");
      final NodeList excelChildren = inputEl.getElementsByTagName ("excel");
      final NodeList dbChildren = inputEl.getElementsByTagName ("DB");
      final NodeList rowChildren = inputEl.getElementsByTagName ("row");
      
      String outputStr = "";
      if (csvChildren.getLength () > 0) {
         final Element csvChild = (Element)csvChildren.item (0);
         final SourceArray2D sourceArray = getCSVArray (csvChild);
         return getArray (sourceArray, input.getNodeName(), typeString);
      }
      else if (excelChildren.getLength () > 0) {
         final Element excelChild = (Element)excelChildren.item (0);
         final SourceArray2D sourceArray = getExcelArray (excelChild);
         return getArray (sourceArray, input.getNodeName(), typeString);
      }
      else if (dbChildren.getLength () > 0) {
         final Element dbChild = (Element)dbChildren.item (0);
         final SourceArray2D sourceArray = getDBArray (dbChild);
         return getArray (sourceArray, input.getNodeName(), typeString);
      }
      else if (rowChildren.getLength () > 0) {
         final StringBuilder output = new StringBuilder ();
         boolean first = true;
         for (int j = 0; j < rowChildren.getLength (); j++) {
            final Node child = rowChildren.item (j);
            if (!child.getParentNode().isEqualNode (input))
               continue;
            final Element childEl = (Element)child;
            final String valIn = getTextContent (child, false);
            final String val = convertValue (valIn, typeString);
            int nrep;
            try {
               if (childEl.hasAttribute ("repeat"))
                  nrep = Integer.parseInt (childEl.getAttribute ("repeat"));
               else
                  nrep = 1;
            }
            catch (final NumberFormatException nfe) {
               logger.warning ("Error converting the repeat attribute with value " + childEl.getAttribute ("repeat") + "; defaulting to 1");
               nrep = 1;
            }
            if (first)
               first = false;
            else
               output.append (" ");
            for (int r = 0; r < nrep; r++)
               output.append (r > 0 ? " " : "").append (val);
         }
         outputStr = output.toString ();
      }
      else {
         final String value = getTextContent (input, false);
         final StringBuilder output = new StringBuilder();
         boolean first = true;
         for (final String part : StringConvert.getArrayElements (value)) {
            if (first)
               first = false;
            else
               output.append (' ');
            output.append (convertValue (part, typeString));
         }
         outputStr = output.toString ();
      }
      
      final Node node = tmpDoc.createElement (input.getNodeName ());
      node.appendChild (tmpDoc.createTextNode (outputStr));
      return node;
   }
   
   private Node getArray2D (SourceArray2D sourceArray, String nodeName, String typeString) {
      final Class<?> targetClass = getTargetClass (typeString);
      final Element output = tmpDoc.createElement (nodeName);
      for (int r = 0; r < sourceArray.rows(); r++) {
         final Element row = tmpDoc.createElement ("row");
         final StringBuilder sb = new StringBuilder();
         for (int c = 0; c < sourceArray.columns (r); c++) {
            if (c > 0)
               sb.append(' ');
            sb.append (getValue (sourceArray, targetClass, r, c));
         }
         row.appendChild (tmpDoc.createTextNode (sb.toString()));
         output.appendChild (row);
      }
      return output;
   }
   
   public Node convertArray2D (Node input, String typeString) {
      if (input == null)
         return tmpDoc.createTextNode ("");
      final Element inputEl = (Element)input;
      final NodeList csvChildren = inputEl.getElementsByTagName ("CSV");
      final NodeList excelChildren = inputEl.getElementsByTagName ("excel");
      final NodeList dbChildren = inputEl.getElementsByTagName ("DB");
      final NodeList rowChildren = inputEl.getElementsByTagName ("row");
      
      final Node outputNode = tmpDoc.createElement (input.getNodeName ());
      if (csvChildren.getLength () > 0) {
         final Element csvChild = (Element)csvChildren.item (0);
         final SourceArray2D sourceArray = getCSVArray (csvChild);
         return getArray2D (sourceArray, input.getNodeName(), typeString);
      }
      else if (excelChildren.getLength () > 0) {
         final Element excelChild = (Element)excelChildren.item (0);
         final SourceArray2D sourceArray = getExcelArray (excelChild);
         return getArray2D (sourceArray, input.getNodeName(), typeString);
      }
      else if (dbChildren.getLength () > 0) {
         final Element dbChild = (Element)dbChildren.item (0);
         final SourceArray2D sourceArray = getExcelArray (dbChild);
         return getArray2D (sourceArray, input.getNodeName(), typeString);
      }
      else if (rowChildren.getLength () > 0)
         for (int j = 0; j < rowChildren.getLength (); j++) {
            final Node child = rowChildren.item (j);
            if (!child.getParentNode().isEqualNode (input))
               continue;
            final Element childEl = (Element)child;
            int nrep;
            try {
               if (childEl.hasAttribute ("repeat"))
                  nrep = Integer.parseInt (childEl.getAttribute ("repeat"));
               else
                  nrep = 1;
            }
            catch (final NumberFormatException nfe) {
               logger.warning ("Error converting the repeat attribute with value " + childEl.getAttribute ("repeat") + "; defaulting to 1");
               nrep = 1;
            }

            final Node outputRow = convertArray (child, typeString);
            outputNode.appendChild (outputRow);
            for (int r = 1; r < nrep; r++) {
               final Node clone = outputRow.cloneNode (true);
               outputNode.appendChild (clone);
            }
         }
      else {
         final String val = getTextContent (input, false);
         for (final String part : StringConvert.getArrayElements (val)) {
            final Node tmpRow = tmpDoc.createElement ("row");
            tmpRow.appendChild (tmpDoc.createTextNode (part));
            final Node outputRow = convertArray (tmpRow, typeString);
            outputNode.appendChild (outputRow);
         }
      }
      return outputNode;
   }
   
   private void copyRVGAttributes (Element output, Element inputEl) {
      final String DISTCLASS = "distributionClass";
      if (inputEl.hasAttribute (DISTCLASS))
         output.setAttribute (DISTCLASS, inputEl.getAttribute (DISTCLASS));
      final String GENCLASS = "generatorClass";
      if (inputEl.hasAttribute (GENCLASS))
         output.setAttribute (GENCLASS, inputEl.getAttribute (GENCLASS));
      final String SHIFT = "shift";
      if (inputEl.hasAttribute (SHIFT))
         output.setAttribute (SHIFT, inputEl.getAttribute (SHIFT));
   }
   
   public Node convertRVG (Node input, String outputName) {
      if (input == null)
         return tmpDoc.createTextNode ("");
      final Element inputEl = (Element)input;
      final Element output = tmpDoc.createElement
      (outputName == null ? input.getNodeName () : outputName);
      copyRVGAttributes (output, inputEl);
      
      final NodeList dataChildren = inputEl.getElementsByTagName ("data");
      if (dataChildren.getLength () > 0) {
         output.setAttribute ("estimateParameters", "true");
         Node dataOutput = convertArray (dataChildren.item (0), "double");
         final String str = getTextContent (dataOutput, false);
         dataOutput = null;
         output.appendChild (tmpDoc.createTextNode (str));
      }
      else {
         final Node dataOutput = convertArray (input, "double"); 
         final String str = getTextContent (dataOutput, false);
         output.appendChild (tmpDoc.createTextNode (str));
      }
      return output;
   }
   
   public Node convertMPG (Node input, String outputName) {
      if (input == null)
         return tmpDoc.createTextNode ("");
      final Element inputEl = (Element)input;
      final Element output = tmpDoc.createElement
      (outputName == null ? input.getNodeName ()
            : outputName);
      copyRVGAttributes (output, inputEl);
      final String UNIT = "unit";
      if (inputEl.hasAttribute (UNIT))
         output.setAttribute (UNIT, inputEl.getAttribute (UNIT));
      else
         output.setAttribute (UNIT, defaultUnit.name());
      final String GROUP = "group";
      if (inputEl.hasAttribute (GROUP))
         output.setAttribute (GROUP, inputEl.getAttribute (GROUP));

      final NodeList data2DChild = inputEl.getElementsByTagName ("data2D");
      final NodeList paramsChild = inputEl.getElementsByTagName ("params");
      if (data2DChild.getLength () > 0) {
         final Node dataOutput = convertArray2D (data2DChild.item (0), "double");
         final NodeList rows = ((Element)dataOutput).getElementsByTagName ("row");
         for (int j = 0; j < rows.getLength (); j++) {
            final Node row = rows.item (j);
            final Node rowOut = tmpDoc.renameNode (row, null, "periodGen");
            output.appendChild (rowOut);
         }
         output.setAttribute ("estimateParameters", "true");
      }
      else if (paramsChild.getLength () > 0) {
         final Node paramsOutput = convertArray2D (paramsChild.item (0), "double");
         final NodeList rows = ((Element)paramsOutput).getElementsByTagName ("row");
         for (int j = 0; j < rows.getLength (); j++) {
            final Node row = rows.item (j);
            final Node rowCpy = row.cloneNode (true);
            final Node rowOut = tmpDoc.renameNode (rowCpy, null, "periodGen");
            output.appendChild (rowOut);
         }
      }
      else {
         boolean est = false;
         final NodeList dataChildren = inputEl.getElementsByTagName ("data");
         if (dataChildren.getLength() > 0)
            for (int j = 0; j < dataChildren.getLength() && !est; j++) {
               final Node child = dataChildren.item (j);
               if (!child.getParentNode().isEqualNode (inputEl))
                  continue;
               est = true;
               Node rowOutput = convertArray (child, "double");
               rowOutput = tmpDoc.renameNode (rowOutput, null, "defaultGen");
               ((Element)rowOutput).setAttribute ("estimateParameters", "true");
               output.appendChild (rowOutput);
            }
         if (!est) {
            final String defaultPar = getTextContent (inputEl, false);
            if (defaultPar.length () > 0) {
               final Node rowInput = tmpDoc.createElement ("defaultGen");
               rowInput.appendChild (tmpDoc.createTextNode (defaultPar));
               final Node rowOutput = convertArray (rowInput, "double");
               output.appendChild (rowOutput);
            }
         }
         final NodeList preGenChild = inputEl.getElementsByTagName ("preliminary");
         if (preGenChild.getLength () > 0) {
            final Node preGenOut = convertRVG (preGenChild.item (0), "preGen");
            output.appendChild (preGenOut);
         }
         final NodeList wrapGenChild = inputEl.getElementsByTagName ("wrapup");
         if (wrapGenChild.getLength () > 0) {
            final Node wrapGenOut = convertRVG (wrapGenChild.item (0), "wrapGen");
            output.appendChild (wrapGenOut);
         }
         final NodeList periodGenChild = inputEl.getElementsByTagName ("row");
         for (int j = 0; j < periodGenChild.getLength (); j++) {
            final Node child = periodGenChild.item (j);
            final Node periodGenOut = convertRVG (child, "periodGen");
            final Element childEl = (Element)child;
            final String REP = "repeat";
            if (childEl.hasAttribute (REP))
               ((Element)periodGenOut).setAttribute (REP, childEl.getAttribute (REP));
            output.appendChild (periodGenOut);
         }
      }
      
      return output;
   }
   
   public Node convertAWT (NodeList awtNodes, int numTypes, int numPeriods1) {
      if (awtNodes == null || awtNodes.getLength () == 0)
         return tmpDoc.createTextNode ("");
      
      if (awtNodes.getLength () == 1) {
         final Element awtElement = (Element)awtNodes.item (0);
         if (!awtElement.hasAttribute ("value"))
            return convertArray2D (awtElement, "duration");
      }
      
      final int Kp = numTypes > 1 ? numTypes + 1 : numTypes;
      final int Pp = numPeriods1 > 1 ? numPeriods1 + 1 : numPeriods1;
      final String[][] awtStr = new String[Kp][Pp];
      for (int j = 0; j < awtNodes.getLength (); j++) {
         final Element awtElement = (Element)awtNodes.item (j);
         if (!awtElement.hasAttribute ("value"))
            continue;
         final String val = awtElement.getAttribute ("value");
         final String valD = timeToDuration (val);
         int k, p;
         if (awtElement.hasAttribute ("k"))
            k = Integer.parseInt (awtElement.getAttribute ("k"));
         else
            k = Kp - 1;
         if (awtElement.hasAttribute ("p"))
            p = Integer.parseInt (awtElement.getAttribute ("p"));
         else
            p = Pp - 1;
         awtStr[k][p] = valD;
      }
      
      for (int k = 0; k < awtStr.length; k++)
         for (int mp = 0; mp < awtStr[k].length; mp++) {
            if (awtStr[k][mp] != null)
               continue;
            if (numTypes > 1 && numPeriods1 > 1 && k < numTypes && mp < numPeriods1) {
               if (awtStr[numTypes][mp] != null)
                  awtStr[k][mp] = awtStr[numTypes][mp];
               else if (awtStr[k][numPeriods1] != null)
                  awtStr[k][mp] = awtStr[k][numPeriods1];
               else
                  awtStr[k][mp] = awtStr[numTypes][numPeriods1];
            }
            else if (numTypes > 1 && numPeriods1 > 1)
               awtStr[k][mp] = awtStr[numTypes][numPeriods1];
            else if (numTypes > 1)
               awtStr[k][mp] = awtStr[numTypes][mp];
            else if (numPeriods1 > 1)
               awtStr[k][mp] = awtStr[k][numPeriods1];
         }
      
      
      final Element awtOutput = tmpDoc.createElement ("awt");
      for (final String[] element : awtStr) {
         final Element awtRow = tmpDoc.createElement ("row");
         final StringBuilder sb = new StringBuilder();
         for (int mp = 0; mp < element.length; mp++)
            sb.append (mp > 0 ? " " : "").append 
            (element[mp] == null ? "PT0S" : element[mp]);
         awtRow.appendChild (tmpDoc.createTextNode (sb.toString ()));
         awtOutput.appendChild (awtRow);
      }
      return awtOutput;
   }

   public Node convertTarget (NodeList targetNodes, int numTypes, int numPeriods1) {
      if (targetNodes == null || targetNodes.getLength () == 0)
         return tmpDoc.createTextNode ("");
      
      if (targetNodes.getLength () == 1) {
         final Element targetElement = (Element)targetNodes.item (0);
         if (!targetElement.hasAttribute ("value"))
            return convertArray2D (targetElement, "double");
      }
      
      final int Kp = numTypes > 1 ? numTypes + 1 : numTypes;
      final int Pp = numPeriods1 > 1 ? numPeriods1 + 1 : numPeriods1;
      final String[][] targetStr = new String[Kp][Pp];
      for (int j = 0; j < targetNodes.getLength (); j++) {
         final Element targetElement = (Element)targetNodes.item (j);
         if (!targetElement.hasAttribute ("value"))
            continue;
         final String val = targetElement.getAttribute ("value");
         int k, p;
         if (targetElement.hasAttribute ("k"))
            k = Integer.parseInt (targetElement.getAttribute ("k"));
         else
            k = Kp - 1;
         if (targetElement.hasAttribute ("p"))
            p = Integer.parseInt (targetElement.getAttribute ("p"));
         else
            p = Pp - 1;
         targetStr[k][p] = val;
      }
      
      final Element targetOutput = tmpDoc.createElement ("target");
      for (final String[] element : targetStr) {
         final Element targetRow = tmpDoc.createElement ("row");
         final StringBuilder sb = new StringBuilder();
         for (int mp = 0; mp < element.length; mp++)
            sb.append (mp > 0 ? " " : "").append 
            (element[mp] == null ? "0" : element[mp]);
         targetRow.appendChild (tmpDoc.createTextNode (sb.toString ()));
         targetOutput.appendChild (targetRow);
      }
      return targetOutput;
   }
   
   public Node convertSourceToggleTimes (String toggleTimes) {
      if (toggleTimes == null || toggleTimes.length () == 0)
         return tmpDoc.createTextNode ("");
      final String[] times = StringConvert.getArrayElements (toggleTimes);
      final Element output = tmpDoc.createElement ("times");
      for (int i = 0; i < times.length / 2; i++) {
         final String start = timeToDuration (times[2*i]);
         final String end = timeToDuration (times[2*i + 1]);
         final Element st = tmpDoc.createElement ("sourceToggleTime");
         st.setAttribute ("startingTime", start);
         st.setAttribute ("endingTime", end);
         output.appendChild (st);
      }
      return output;
   }
   
   public Node convertScheduleShift (Node shift) {
      if (shift == null)
         return tmpDoc.createTextNode ("");
      final Element output = tmpDoc.createElement ("shift");
      final Element shiftEl = (Element)shift;
      final String NUMAGENTS = "numAgents";
      if (shiftEl.hasAttribute (NUMAGENTS))
         output.setAttribute (NUMAGENTS, shiftEl.getAttribute (NUMAGENTS));
      
      final NodeList parts = shiftEl.getElementsByTagName ("shiftPart");
      if (parts.getLength() > 1)
         for (int j = 0; j < parts.getLength() - 1; j++) {
            final Element part = (Element)parts.item (j);
            final Element partOut = tmpDoc.createElement ("shiftPart");
            final String TYPE = "type";
            if (part.hasAttribute (TYPE))
               partOut.setAttribute (TYPE, part.getAttribute (TYPE));
            final double st;
            if (part.hasAttribute ("startPeriod")) {
               final int sp = Integer.parseInt (part.getAttribute("startPeriod"));
               st = startingTimeInSec + periodDurationInSec*sp;
            }
            else
               st = startingTimeInSec;
            partOut.setAttribute ("startingTime", OldSimParamsConverter.toDuration (st));
            
            final Element nextPart = (Element)parts.item (j + 1);
            final double et;
            if (nextPart.hasAttribute ("startPeriod")) {
               final int ep = Integer.parseInt (nextPart.getAttribute ("startPeriod"));
               et = startingTimeInSec + periodDurationInSec*ep;
            }
            else
               et = startingTimeInSec + numPeriods*periodDurationInSec;
            partOut.setAttribute ("endingTime", OldSimParamsConverter.toDuration (et));
            
            output.appendChild (partOut);
         }
      return output;
   }
   
   public Node convertPoissonGammaParams (Node poissonGammaParams) {
      if (poissonGammaParams == null)
         return tmpDoc.createTextNode ("");
      final Node array = convertArray2D (poissonGammaParams, "double");
      final Element arrayEl = (Element)array;
      final NodeList rows = arrayEl.getElementsByTagName ("row");
      final StringBuilder shape = new StringBuilder(), scale = new StringBuilder();
      boolean first = true;
      for (int j = 0; j < rows.getLength(); j++) {
         final Element row = (Element)rows.item (j);
         final String val = getTextContent (row, false);
         final String[] parts = val.split ("\\s+");
         if (parts.length == 2) {
            if (first)
               first = false;
            else {
               shape.append (' ');
               scale.append (' ');
            }
            shape.append (parts[0]);
            scale.append (parts[1]);
         }
      }
      
      if (shape.length() == 0)
         return tmpDoc.createTextNode ("");
      
      final Node output = tmpDoc.createElement ("params");
      final Node shapeNode = tmpDoc.createElement ("poissonGammaShape");
      shapeNode.appendChild (tmpDoc.createTextNode(shape.toString()));
      output.appendChild (shapeNode);
      final Node scaleNode = tmpDoc.createElement ("poissonGammaScale");
      scaleNode.appendChild (tmpDoc.createTextNode(scale.toString()));
      output.appendChild (scaleNode);
      return output;
   }
   
   public static void main (String[] args) {
      if (args.length != 2) {
         if (args.length > 0)
            System.err.println ("Wrong number of arguments");
         System.err.println ("Usage: java umontreal.iro.lecuyer.contactcenters.msk.OldCallCenterParamsConverter <input file> <output file>");
         System.exit (1);
      }
      final String inputFile = args[0];
      final String outputFile = args[1];
      if (inputFile.equals (outputFile)) {
         System.err.println ("Input and output files must be different");
         System.exit (1);
      }
      if (!new File (inputFile).exists ()) {
         System.err.println ("Cannot find input file " + inputFile);
         System.exit (1);
      }
      
      final URL xslUrl = OldCallCenterParamsConverter.class.getResource ("cnvCCParams.xsl");
      if (xslUrl == null) {
         System.err.print ("Cannot find XSL stylesheet for the transformation");
         System.exit (1);
      }
      
      final TransformerFactory tFactory = TransformerFactory.newInstance ();
      Transformer trans;
      try {
         trans = tFactory.newTransformer (new StreamSource (xslUrl.toString ()));
      }
      catch (final TransformerConfigurationException tce) {
         System.err.println (ExceptionUtil.throwableToString (tce));
         return;
      }
      trans.setOutputProperty (
            "{http://xml.apache.org/xalan}indent-amount", "3");
      
      final Source src = new StreamSource (new File (inputFile));
      final Result res = new StreamResult (new File (outputFile));
      trans.setParameter ("baseURI", src.getSystemId());
      try {
         trans.transform (src, res);
      }
      catch (final TransformerException te) {
         System.err.println (ExceptionUtil.throwableToString (te));
         System.exit (1);
      }
   }
}
