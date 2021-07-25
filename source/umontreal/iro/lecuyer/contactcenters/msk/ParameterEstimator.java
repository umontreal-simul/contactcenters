package umontreal.iro.lecuyer.contactcenters.msk;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.ArrivalProcessManager;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.InboundTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.OutboundTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ServiceTimeParams;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;

/**
 * Estimates the parameters of a call center model. This class defines a main
 * method that loads a parameter file, estimates the parameters for probability
 * distribution with associated data, and writes a new file for the same model,
 * with the estimated parameters.
 */
public class ParameterEstimator {
	
	/**
	 * If the basic parameters of arrival process \texttt{par} are not set,
	 * sets them to those of the default arrival process \texttt{defPar}.
    * @param par parameters of the given arrival process
    * @param defaultPar parameters of the default arrival process
	 */
	public static void setParamsFromDefault(ArrivalProcessParams par, ArrivalProcessParams defaultPar) {
   	if (null == defaultPar)
   		throw new IllegalArgumentException(
			    "   No arrival process has been chosen");
   	final String name = defaultPar.getType();
     	if (null == name)
  		   throw new IllegalArgumentException(
		       "   No arrival process has been chosen");
   	par.setType(name);
        par.setEstimateBusyness(defaultPar.isEstimateBusyness());
        par.setEstimateDailyGammaPower(defaultPar.isEstimateDailyGammaPower());
   	par.setNormalize(defaultPar.isNormalize());
   	if (0 == name.compareTo("POISSONGAMMANORTARATES")) {
   	   par.setCorrelationFit(defaultPar.getCorrelationFit());
   	}
   	if (0 == name.compareTo("POISSONGAMMA")) {
   	   par.setGammaShapeEstimatorType(defaultPar.getGammaShapeEstimatorType());
   	   par.setGammaShapeSmoothingFactor(defaultPar.getGammaShapeSmoothingFactor());
   	   par.setMaxIter(defaultPar.getMaxIter());
   	   par.setMovingWindowSize(defaultPar.getMovingWindowSize());
   	}
   	if (0 == name.compareTo("CUBICSPLINE")) {
   		par.setSplineSmoothingFactor(defaultPar.getSplineSmoothingFactor());
   	}
	}
	
   /**
    * Estimates the parameters for each element
    * in the call center parameter objects for
    * which raw observations are specified.
    * Returns \texttt{true} if at least one
    * parameter has been estimated by this
    * method.
    * @param ccParams the call center parameters.
    * @return true if method was successfull.
    * @throws DistributionCreationException if an error
    * occurs during the creation of a distribution.
    */
   public static boolean estimateParameters (CallCenterParams ccParams) throws DistributionCreationException {
      boolean res = false;
      String name;
      ArrivalProcessParams defArr, arr;
      arr = ccParams.getInboundTypes().get(0).getArrivalProcess();
      PiecewiseConstantPoissonArrivalProcess.setVarianceEpsilon(
      		arr.getVarianceEpsilon());
      PoissonGammaArrivalProcess.setNumMC(arr.getNumMonteCarlo());
      defArr = ccParams.getDefaultArrivalProcess();   // may be null
      final int numPeriods = ccParams.getNumPeriods ();
      final double periodDurationMillis = ccParams.getPeriodDuration ().getTimeInMillis (new Date());
      final TimeUnit defaultUnit = TimeUnit.valueOf (ccParams.getDefaultUnit ().name ()); 
      final double periodDuration = TimeUnit.convert (periodDurationMillis, TimeUnit.MILLISECOND, defaultUnit);
      res |= ParamReadHelper.estimateParameters (ccParams.getBusynessGen ());
      for (final InboundTypeParams par : ccParams.getInboundTypes ()) {
         res |= CCParamReadHelper.estimateParameters (par.getPatienceTime ());
         for (final ServiceTimeParams sp : par.getServiceTimes ())
            res |= CCParamReadHelper.estimateParameters (sp);
         if (par.isSetArrivalProcess()) {
         	arr = par.getArrivalProcess();
            name = arr.getType();
            if (null == name)
            	setParamsFromDefault(arr, defArr);
            	
            ArrivalProcessManager.s_bgenParams = null;
            res |= ArrivalProcessManager.estimateParameters (ccParams,
            		arr, numPeriods, periodDuration);
            if (ArrivalProcessManager.s_bgenParams != null) {
            	/*   if (ccParams.isSetBusynessGen ())
            	    throw new IllegalArgumentException
                  ("The busyness factor must be specified or estimated only once");*/
               ccParams.setBusynessGen (ArrivalProcessManager.s_bgenParams);
            }
            arr.setData (null);
            arr.unsetEstimateBusyness ();
        }
      }
      for (final OutboundTypeParams par : ccParams.getOutboundTypes ()) {
         res |= CCParamReadHelper.estimateParameters (par.getPatienceTime ());
         for (final ServiceTimeParams sp : par.getServiceTimes ())
            res |= CCParamReadHelper.estimateParameters (sp);
         res |= CCParamReadHelper.estimateParameters (par.getReachTime ());
         res |= CCParamReadHelper.estimateParameters (par.getFailTime ());
      }
      for (final AgentGroupParams par : ccParams.getAgentGroups ())
         res |= AgentGroupManager.estimateParameters (par); 
      for (final ArrivalProcessParams par : ccParams.getArrivalProcesses ()) {
         name = par.getType();
         if (null == name)
         	setParamsFromDefault(par, defArr);
         ArrivalProcessManager.s_bgenParams = null;
         res |= ArrivalProcessManager.estimateParameters (ccParams, par, 
         		numPeriods, periodDuration);
         if (ArrivalProcessManager.s_bgenParams != null) {
          /*  if (ccParams.isSetBusynessGen ())
               throw new IllegalArgumentException
               ("The busyness factor must be specified or estimated only once");*/
            ccParams.setBusynessGen (ArrivalProcessManager.s_bgenParams);
         }
         
         par.unsetEstimateBusyness ();
         par.setData (null);
      }
      ArrivalProcessManager.s_bgenParams = null;
      if (defArr != null)
         defArr.unsetEstimateBusyness ();
      return res;
   }
   
   /**
    * Main method of this class taking, as arguments, the names of the input and
    * the output files.
    * 
    * @param args
    *           the arguments given to the program.
    * @throws IOException
    *            if an I/O error occurs when reading or writing files.
    * @throws ParserConfigurationException
    *            if an error occurs when parsing the XML file.
    * @throws SAXException
    *            if an error occurs with SAX, when parsing the XML file.
    * @throws TransformerException
    *            if an error occurs when creating the output XML file.
    */
   public static void main (String[] args) {
      if (args.length != 2) {
         if (args.length > 0)
            System.err.println ("Wrong number of arguments");
         System.err.println
           ("Usage: java umontreal.iro.lecuyer.contactcenters.msk.ParameterEstimator "
           + "<input call center data file name> <output call center data file name>");
         System.exit (1);
      }
      if (!new File (args[0]).exists ()) {
         System.err.println ("Cannot find the file " + args[0]);
         System.exit (1);
      }

      final CallCenterParamsConverter cnv = new CallCenterParamsConverter();
      final CallCenterParams ccParams;
      try {
         ccParams = cnv.unmarshal (new File (args[0]));
      }
      catch (final JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
         return;
      }
      
      try {
         if (!estimateParameters (ccParams))
            System.err.println ("Parameters unchanged");
      }
      catch (final DistributionCreationException dce) {
         System.err.println (ExceptionUtil.throwableToString (dce));
         System.exit (1);
         return;
      }
      try {
         cnv.marshal (ccParams, new File (args[1]));
      }
      catch (final JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
      }
   }
}
