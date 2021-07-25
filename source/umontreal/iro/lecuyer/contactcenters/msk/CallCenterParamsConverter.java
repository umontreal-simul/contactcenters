package umontreal.iro.lecuyer.contactcenters.msk;

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

import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.xmlbind.JAXBParamsConverter;

public class CallCenterParamsConverter extends JAXBParamsConverter<CallCenterParams> {
   private static JAXBContext context;
   private static Schema schema;
   private static final Map<String, String> prefixToUri = new HashMap<String, String>();
   
   static {
      prefixToUri.put ("ccmsk", "http://www.iro.umontreal.ca/lecuyer/contactcenters/msk");
      prefixToUri.put ("ccapp", "http://www.iro.umontreal.ca/lecuyer/contactcenters/app");
      prefixToUri.put ("cc", "http://www.iro.umontreal.ca/lecuyer/contactcenters");
      prefixToUri.put ("ssj", "http://www.iro.umontreal.ca/lecuyer/ssj");
   }
   
   public CallCenterParamsConverter() {
      super (CallCenterParams.class);
   }

   @Override
   public JAXBContext getContext () throws JAXBException {
      synchronized (CallCenterParamsConverter.class) {
         if (context == null)
            context = JAXBContext.newInstance ("umontreal.iro.lecuyer.contactcenters.msk.params:umontreal.iro.lecuyer.contactcenters.app.params:umontreal.iro.lecuyer.contactcenters.params:umontreal.iro.lecuyer.xmlbind.params");
         return context;
      }
   }

   @Override
   public Schema getSchema () throws SAXException {
      synchronized (CallCenterParamsConverter.class) {
         if (schema == null) {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final URL url = getClass().getResource ("/umontreal/iro/lecuyer/schemas/ccmsk.xsd");
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
