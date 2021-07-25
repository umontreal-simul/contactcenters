package umontreal.iro.lecuyer.contactcenters.app;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.xmlbind.JAXBParamsConverter;

/**
 * Parameter converter for
 * {@link SimParams}.
 */
public class SimParamsConverter extends JAXBParamsConverter<SimParams> {
   private static JAXBContext context;
   private static Schema schema;
   private static final Map<String, String> prefixToUri = new HashMap<String, String>();
   
   static {
      prefixToUri.put ("ccapp", "http://www.iro.umontreal.ca/lecuyer/contactcenters/app");
      prefixToUri.put ("ssj", "http://www.iro.umontreal.ca/lecuyer/ssj");
   }
   
   public SimParamsConverter() {
      super (SimParams.class);
   }
   
   @Override
   public JAXBContext getContext() throws JAXBException {
      synchronized (SimParamsConverter.class) {
         if (context == null)
            context = JAXBContext.newInstance ("umontreal.iro.lecuyer.xmlbind.params:umontreal.iro.lecuyer.contactcenters.app.params");
         return context;
      }
   }
   
   @Override
   public Schema getSchema() throws SAXException {
      synchronized (SimParamsConverter.class) {
         if (schema == null) {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final URL url = getClass().getResource ("/umontreal/iro/lecuyer/schemas/ccapp.xsd");
            schema = schemaFactory.newSchema (url);
         }
         return schema;
      }
   }
   
   @Override
   public Map<String, String> getNamespacePrefixes () {
      return Collections.unmodifiableMap (prefixToUri);
   }
}
