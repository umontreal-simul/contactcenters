package umontreal.iro.lecuyer.contactcenters.msk;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.xml.bind.JAXBException;



import umontreal.iro.lecuyer.contactcenters.app.ContactCenterEval;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterProgressBar;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimWithObservations;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.PrintedStatParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.RandomStreams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
//import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simevents.UnusableSimulator;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import java.util.ArrayList;  //Ajouter

/**
 * Encapsulates all the components of the blend and multi-skill call center simulator, and
 * provides methods to perform simulations and obtain results.
 * This class uses
 * the {@link CallCenter} class to implement a model, and a {@link SimLogic}
 * implementation for the simulation logic. It also uses an implementation of
 * {@link CallCenterStatProbes} for statistical collecting.
 *
 * An object of this class is constructed using parameter objects usually
 * read from XML files.
 * The parameters of the model are
 * stored into an instance of {@link CallCenterParams},
 * while the parameters of the experiment are encapsulated into
 * an object of class {@link SimParams}.
 * The classes {@link CallCenterParamsConverter}
 * and {@link SimParamsConverter}
 * can be used to read parameters from XML files.
 *
 * After the simulator is constructed,
 * it can be accessed in a standardized way
 * through the {@link
 * ContactCenterEval}
 * interface, which defines methods to obtain global information about
 *          the call center, perform simulations, and retrieve
 * matrices of statistics.
 *
 * The \texttt{CallCenterSim}
 * class also provides a \texttt{main} method
 * accepting as arguments the name of the parameter files, performing
 * a simulation, and showing results.
 * This permits the simulator
 * to be launched from the
 * command-line.
 */
public class CallCenterSim extends AbstractCallCenterSim implements
      ContactCenterSimWithObservations {
   /**
    * Constructs a new call center simulator using call center parameters
    * \texttt{ccParams}, and simulation parameters \texttt{simParams}.
    *
    * This calls {@link #createModel} to create the model,
    * {@link #createSimLogic} to create the simulation logic.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    */
   public CallCenterSim (CallCenterParams ccParams, SimParams simParams)
         throws CallCenterCreationException {
      super (ccParams, simParams);
   }

   /**
    * Constructs a new call center simulator using call center parameters
    * \texttt{ccParams}, simulation parameters \texttt{simParams}, and random
    * streams \texttt{streams}.
    *
    * This calls {@link #createModel} to create the model,
    * {@link #createSimLogic} to create the simulation logic.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    * @param streams
    *           the random streams used by the simulator.
    */
   public CallCenterSim (CallCenterParams ccParams, SimParams simParams,
         RandomStreams streams) throws CallCenterCreationException {
      super (ccParams, simParams, streams);
   }


   /**
    * Similar to {@link #CallCenterSim(CallCenterParams,SimParams,RandomStreams)},
    * with the given simulator \texttt{sim}.
    */
   public CallCenterSim (Simulator sim, CallCenterParams ccParams,
         SimParams simParams, RandomStreams streams)
         throws CallCenterCreationException {
      super (sim, ccParams, simParams, streams);
   }

   /**
    * Similar to {@link #CallCenterSim(CallCenterParams,SimParams)},
    * with the given simulator \texttt{sim}.
    */
   public CallCenterSim (Simulator sim, CallCenterParams ccParams,
         SimParams simParams) throws CallCenterCreationException {
      super (sim, ccParams, simParams);
   }

   public double[] getObs (PerformanceMeasureType pm, int row, int column) {
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      final Tally tally = mta.get (row, column);
      if (tally instanceof TallyStore)
         return CallCenterSimUtil.getObs ((TallyStore)tally);
      else
         throw new NoSuchElementException (
               "The simulator does not keep observations");
   }

   public int numberObs (PerformanceMeasureType pm, int row, int column) {
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      return mta.get (row, column).numberObs ();
   }

   public static CallCenterParams readCallCenterParams (String ccParamsFn) {
      return new CallCenterParamsConverter().unmarshalOrExit (new File (ccParamsFn));
   }

   public static SimParams readSimParams (String simParamsFn) {
      return new SimParamsConverter().unmarshalOrExit (new File (simParamsFn));
   }

   public static void write (CallCenterParams ccParams, String outputFn) {
      new CallCenterParamsConverter().marshalOrExit(ccParams, new File (outputFn));
   }

  //Ajouter methode pour sauvegarder la distribution des attente estimer moins l'attente real  dans le fichier distribution.text

   public static void ecrire(File outputFile, ArrayList<String>[]tab) throws IOException,
      JAXBException
   {    String TypeClient[]={"CLIENTS REELS SERVIS","CLIENT REELS ABANDONES",
		                          "CLIENTS VIRTUELS SERVIS","CLIENT VIRTUELS ABANDONES"};
	   final OutputStream stream = new FileOutputStream (outputFile, true);
       final PrintWriter out = new PrintWriter (new OutputStreamWriter (stream));
       for(int i=0;i<tab.length;i++)                                                              //Ajouter
       {   ArrayList<String> liste=tab[i];                                                        //Ajouter
    	   out.println ("###################### "+TypeClient[i]+"("+liste.size()+")"+             //Ajouter
                                  " #####################");                                      //Ajouter
     	 for(String ch:liste){                                                                    //Ajouter
     	   out.println(ch);                                                                       //Ajouter
          }                                                                                       //Ajouter                                                                               //Ajouter
        }                                                                                         //Ajouter
       out.close ();                                                                              //Ajouter
   }                                                                                              //Ajouter

   //Ajout de la methode qui teste si la distribution doit etre sauvegarder ou non
   //Cette methode verifie si le MSe du temps d attente est calculer pour en fin enregistrer la distribution
   // de la difference attente reel - attente estimer dans le repertoire courant dans un fichier nom distribution.txt

   public void testSauvegarde(SimParams simParams,CallCenterSim sim) throws IOException, JAXBException{
	   PerformanceMeasureType tabs[]=  new PerformanceMeasureType[simParams
                                                                  .getReport ().getPrintedStats().size()];
              int i=0;
              for (final PrintedStatParams par : simParams.getReport().getPrintedStats())
                  tabs[i++] = PerformanceMeasureType.valueOf (par.getMeasure ());
        boolean test=false;
        for(int j=0;j<tabs.length;j++){
      	  // System.out.println(tabs[j]);
      	 if(tabs[j]== PerformanceMeasureType.MSEWAITINGTIME
      			   || tabs[j]==PerformanceMeasureType.MSEWAITINGTIMEVQ
      			   || tabs[j]==PerformanceMeasureType.MSEWAITINGTIMEABANDONED
      			   || tabs[j]==PerformanceMeasureType.MSEWAITINGTIMESERVED
      			   || tabs[j]==PerformanceMeasureType.MSEWAITINGTIMEVQABANDONED
      			   || tabs[j]==PerformanceMeasureType.MSEWAITINGTIMEVQSERVED
      			    ){
      		  test=true;
      	 }
         }
                                                                                                         //Ajouter
         if(test==true)                                                                                   //Ajouter
          {  ArrayList<String>[]tab=  sim.getCallCenterMeasureManager().getMeasureTypesMse();
      	   String path= System.getProperty("user.dir");                                                 //Ajouter
              String sep=File.separator;                                                                  //Ajouter
      	    File  monfichier = new File (path+sep+"distribution.txt");                                  //Ajouter
      	    CallCenterSim.ecrire(monfichier,tab);                                                        //Ajouter
          }                                                                                             // Fin Ajouter

   }

   /**
    * Main method allowing to run this class from the command-line. The needed
    * command-line arguments are the name of an XML file containing the
    * non-stationary simulation parameters (root element \texttt{mskccparams}),
    * and the name of a second XML file containing the simulation parameters
    * (root elements \texttt{batchsimparams} or \texttt{repsimparams}).
    *
    * @param args
    *           the command-line arguments.
    */
   public static void main (String[] args) {
      if (args.length != 2 && args.length != 3) {
         if (args.length > 0)
            System.err.println ("Wrong number of arguments");
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim "
                     + "<call center data file name> <experiment parameter file> [<output file name>]");
         System.exit (1);
      }
      final String ccParamsFn = args[0];
      final String simParamsFn = args[1];
      final CallCenterParams ccParams = readCallCenterParams (ccParamsFn);
      final SimParams simParams = readSimParams (simParamsFn);

      File outputFile = null;
      if (args.length == 3)
         outputFile = new File (args[2]);

      SimRandomStreamFactory.initSeed (simParams.getRandomStreams ());
      Simulator.defaultSimulator = new UnusableSimulator();
      try {
         final CallCenterSim sim = new CallCenterSim (new Simulator(), ccParams, simParams);
         // Requires setting a property using -D option.
         // Another possibility would be to allow a --no-progress-bar
         // argument to the CallCenterSim program.
         // For this, a library such as http://jopt-simple.sourceforge.net/
         // would be needed to parse command-line
         // arguments.
         if (System.getProperty ("cc.noprogressbar") == null)
            sim.addContactCenterSimListener (new ContactCenterProgressBar ());

         PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (),
               ccParamsFn, simParamsFn);
         sim.eval ();
         PerformanceMeasureFormat.formatResults (sim, outputFile);

        //Ajouter pour voir si la distribution doit etre sauvegarder ou pas                        // Ajouter
         sim.testSauvegarde(simParams,sim);                                                               //Ajouter


         }
      catch (final CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
      }
      catch (final JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
      }
      catch (final IOException ioe) {
         System.err.println (ExceptionUtil.throwableToString (ioe));
         System.exit (1);
      }
   }
}
