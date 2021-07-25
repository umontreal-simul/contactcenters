package umontreal.iro.lecuyer.contactcenters.app;

import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.app.params.ContactCenterEvalResultsParams;
import umontreal.iro.lecuyer.xmlbind.JAXBParamsConverter;

/**
 * Converter for marshalling and unmarshalling
 * objects containing evaluation results.
 */
public class ContactCenterEvalResultsConverter extends JAXBParamsConverter<ContactCenterEvalResultsParams> {
   // SimParamsConverter extends JAXBParamsConverter<SimParams>, but
   // we need to extend JAXBParamsConverter<ContactCenterEvalResult>, i.e.,
   // the same class with a different type parameter.
   // We therefore cannot make ContactCenterEvalResultsConverter extend
   // SimParamsConverter.
   private final SimParamsConverter cnv = new SimParamsConverter();

   public ContactCenterEvalResultsConverter () {
      super (ContactCenterEvalResultsParams.class);
   }
   
   @Override
   public JAXBContext getContext () throws JAXBException {
      return cnv.getContext();
   }

   @Override
   public Schema getSchema () throws SAXException {
      return cnv.getSchema();
   }

   @Override
   public Map<String, String> getNamespacePrefixes () {
      return cnv.getNamespacePrefixes();
   }
   
   /**
    * Reads evaluation results from the file
    * \texttt{file}.
    * This method uses {@link JAXBParamsConverter#unmarshal(File)}
    * to unmarshal the given file to
    * a parameter object,
    * and {@link ContactCenterEvalResults#createFromParams(ContactCenterEvalResultsParams)}
    * to create the final object containing results.
    * @param file the input file.
    * @return the evaluation results.
    * @throws JAXBException if an exception occurs
    * during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (File file) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (file);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File)}, but uses
    * {@link ContactCenterEvalResults#createFromParams(ContactCenterEvalResultsParams,boolean)}
    * instead of
    * {@link ContactCenterEvalResults#createFromParams(ContactCenterEvalResultsParams)}.
    * @param file the input file
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the evaluation results.
    * @throws JAXBException if an exception occurs
    * during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (File file, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (file);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(File)},
    * but calls {@link #unmarshalGZipped(File)}
    * for unmarshalling.
    * @param file the input file.
    * @return the evaluation results.
    * @throws JAXBException if an error occurs during the unmarshalling.
    */
   public ContactCenterEvalResults unmarshalGZippedToEval (File file) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshalGZipped (file);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File,boolean)},
    * but calls {@link #unmarshalGZipped(File)}
    * for unmarshalling.
    * @param file the input file
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the evaluation results.
    * @throws JAXBException if an error occurs during the unmarshalling.
    */
   public ContactCenterEvalResults unmarshalGZippedToEval (File file, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshalGZipped (file);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(URL)},
    * but calls {@link #unmarshalGZipped(URL)} for unmarshalling.
    * @param url the input URL.
    * @return the evaluation results.
    * @throws JAXBException if an error occurs during the unmarshalling.
    */
   public ContactCenterEvalResults unmarshalGZippedToEval (URL url) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshalGZipped (url);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(URL,boolean)}
    * but calls {@link #unmarshalGZipped(URL)} for
    * unmarshalling.
    * @param url the input URL.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the evaluation results.
    * @throws JAXBException if an error occurs during the unmarshalling.
    */
   public ContactCenterEvalResults unmarshalGZippedToEval (URL url, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshalGZipped (url);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(File)},
    * with an URL instead of a file.
    * @param url the URL of the XML data.
    * @return the constructed object containing
    * results.
    * @throws JAXBException if an exception occurs
    * during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (URL url) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (url);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File,boolean)},
    * with an URL instead of a file.
    * @param url the input URL of the XML data.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the evaluation results.
    * @throws JAXBException if an error occurs during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (URL url, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (url);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(File)},
    * with a node instead of a file.
    * @param node the input node.
    * @return the constructed object containing
    * results.
    * @throws JAXBException if an exception occurs
    * during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (Node node) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (node);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File,boolean)}
    * with a node instead of a file.
    * @param node the input node.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the constructed object containing results.
    * @throws JAXBException if an exception occurs during
    * unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (Node node, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (node);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(File)},
    * with a source instead of a file.
    * @param source the input source to read XML from.
    * @return the constructed object containing
    * results.
    * @throws JAXBException if an exception occurs
    * during unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (Source source) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (source);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File,boolean)},
    * with a source instead of a file.
    * @param source the input source to read XML from.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the constructed object containing results.
    * @throws JAXBException if an exception occurs during
    * unmarshalling.
    */
   public ContactCenterEvalResults unmarshalToEval (Source source, boolean reportPropertiesToEvalInfo) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = unmarshal (source);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Similar to {@link #unmarshalToEval(File)},
    * but calls {@link JAXBParamsConverter#unmarshalOrExit(File)}
    * to perform unmarshalling.
    * This method should be used in simple programs
    * with no graphical user interface, because
    * it can print information on the standard output,
    * and exit the program if an error occurs.
    * @param file the input file.
    * @return the constructed object containing
    * results.
    */
   public ContactCenterEvalResults unmarshalToEvalOrExit (File file) {
      final ContactCenterEvalResultsParams ccp = unmarshalOrExit (file);
      return ContactCenterEvalResults.createFromParams (ccp);
   }

   /**
    * Similar to {@link #unmarshalToEval(File,boolean)},
    * but calls {@link JAXBParamsConverter#unmarshalOrExit(File)}
    * to perform unmarshalling. 
    * @param file the input file.
    * @param reportPropertiesToEvalInfo determines if report properties
    * are copied to evaluation information.
    * @return the constructed object containing results.
    */
   public ContactCenterEvalResults unmarshalToEvalOrExit (File file, boolean reportPropertiesToEvalInfo) {
      final ContactCenterEvalResultsParams ccp = unmarshalOrExit (file);
      return ContactCenterEvalResults.createFromParams (ccp, reportPropertiesToEvalInfo);
   }
   
   /**
    * Marshals an object containing evaluation results
    * into an XML document, and writes the
    * resulting output to the content handler
    * \texttt{handler}.
    * This method uses {@link ContactCenterEvalResults#createParams()}
    * to create a parameter object from the evaluation results,
    * and uses 
    * {@link JAXBParamsConverter#marshal(Object,ContentHandler)}
    * to marshal the parameters to XML.
    * @param res the object containing evaluation results.
    * @param handler the target content handler receiving XML events.
    * @throws JAXBException if an exception occurs during
    * marshalling.
    */
   public void marshalEval (ContactCenterEvalResults res, ContentHandler handler) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshal (ccp, handler);
   }

   /**
    * Similar to {@link #marshalEval(ContactCenterEvalResults,ContentHandler)}, but
    * uses {@link JAXBParamsConverter#marshal(Object,Result)}
    * for marshalling instead.
    * @param res the object containing evaluation results.
    * @param result the output result for the XML contents.
    * @throws JAXBException if an error occurs during
    * marshalling.
    */
   public void marshalEval (ContactCenterEvalResults res, Result result) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshal (ccp, result);
   }

   /**
    * Similar to {@link #marshalEval(ContactCenterEvalResults,ContentHandler)}, but
    * uses {@link JAXBParamsConverter#marshal(Object,File)}
    * for marshalling into a file instead.
    * @param res the object containing evaluation results.
    * @param file the output file.
    * @throws JAXBException if an error occurs during
    * marshalling.
    */
   public void marshalEval (ContactCenterEvalResults res, File file) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshal (ccp, file);
   }

   /**
    * Similar to {@link #marshalEval(ContactCenterEvalResults,File)},
    * but calls {@link #marshalAndGZip(Object,File)}.
    * @param res the object to be marshalled.
    * @param file the output file.
    * @throws JAXBException if an error occurs during the process.
    */
   public void marshalEvalAndGZip (ContactCenterEvalResults res, File file) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshalAndGZip (ccp, file);
   }
   
   /**
    * Similar to {@link #marshalEval(ContactCenterEvalResults,ContentHandler)}, but
    * uses {@link JAXBParamsConverter#marshal(Object,Node)}
    * for marshalling instead.
    * @param res the object containing evaluation results.
    * @param node the output node.
    * @throws JAXBException if an error occurs during
    * marshalling.
    */
   public void marshalEval (ContactCenterEvalResults res, Node node) throws JAXBException {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshal (ccp, node);
   }

   /**
    * Similar to {@link #marshalEval(ContactCenterEvalResults,File)},
    * but uses {@link JAXBParamsConverter#marshalOrExit(Object,File)}
    * for marshalling.
    * This method should be used in simple programs
    * with no graphical user interface, because
    * it can print information on the standard output,
    * and exit the program if an error occurs.
    * @param res
    * @param file
    */
   public void marshalEvalOrExit (ContactCenterEvalResults res, File file) {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshalOrExit (ccp, file);
   }

   /**
    * Similar to {@link #marshalEvalOrExit(ContactCenterEvalResults,File)},
    * but calls {@link #marshalAndGZipOrExit(Object,File)}.
    * @param res the object to be marshalled.
    * @param file the output file.
    */
   public void marshalEvalAndGZipOrExit (ContactCenterEvalResults res, File file) {
      final ContactCenterEvalResultsParams ccp = res.createParams();
      marshalAndGZipOrExit (ccp, file);
   }
}
