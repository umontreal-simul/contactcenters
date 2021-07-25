package umontreal.iro.lecuyer.contactcenters.app;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;

/**
 * This class contains a main method that loads and displays results from a
 * simulator. These results can be produced by, e.g., {@link CallCenterSim}.
 */
public class LoadSimResults {
   /**
    * Main method for this class.
    * 
    * @param args
    *           the arguments.
    * @throws IOException
    *            if an I/O error occurs.
    * @throws ClassNotFoundException
    *            if a class cannot be found during loading.
    * @throws ParserConfigurationException
    *            if an error occurs during parser configuration.
    * @throws SAXException
    *            if an error occurs when parsing the XML input file.
    */
   public static void main (String[] args) throws IOException,
         JAXBException {
      if (args.length < 1 || args.length > 2) {
         if (args.length > 0)
            System.err.println ("Wrong number of arguments");
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.app.LoadSimResults <input file name> [output file name]");
         System.exit (1);
      }
      final String inputFileName = args[0];
      final ContactCenterEvalResultsConverter cnv = new ContactCenterEvalResultsConverter();
      final ContactCenterEvalResults simRes = cnv.unmarshalToEvalOrExit (new File (inputFileName));
      if (args.length == 1)
         System.out.println (simRes.formatStatistics ());
      else {
         final String outputFileName = args[1];
         PerformanceMeasureFormat.formatResults (simRes, outputFileName);
      }
   }
}
