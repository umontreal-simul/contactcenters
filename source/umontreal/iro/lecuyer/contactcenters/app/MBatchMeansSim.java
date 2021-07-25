package umontreal.iro.lecuyer.contactcenters.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.ssj.util.TimeUnit;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * This simulator performs a certain number of replications of a simulation with
 * batch means. An object from this class is constructed using a simulator which
 * is assumed to use batch means. When an evaluation is triggered, the internal
 * simulator is called several times to get values. This allows, e.g., to
 * simulate with different values of $\lambda$ for a Poisson-gamma arrival
 * process.
 */
public class MBatchMeansSim extends AbstractContactCenterSim {
   private final Map<PerformanceMeasureType, MatrixOfTallies<Tally>> pmMap = new EnumMap<PerformanceMeasureType, MatrixOfTallies<Tally>> (
         PerformanceMeasureType.class);
   // Needed to preserve order
   private PerformanceMeasureType[] pms;
   private ContactCenterSim sim;
   private int m_numReps;
   private int numDoneReps;

   /**
    * Constructs a simulator running \texttt{numReps} independent batch means
    * experiments using the internal simulator \texttt{sim}. Confidence
    * intervals are reported with level \texttt{level}.
    * 
    * @param sim
    *           the internal simulator which is assumed to use batch means.
    * @param numReps
    *           the number of independent batch means experiments.
    */
   public MBatchMeansSim (ContactCenterSim sim, int numReps) {
      this.sim = sim;
      pms = sim.getPerformanceMeasures ();
      this.m_numReps = numReps;
   }

   /**
    * Returns the internal simulator used by this object.
    * 
    * @return the internal simulator.
    */
   public ContactCenterSim getSimulator () {
      return sim;
   }
   

   public double getConfidenceLevel () {
      return sim.getConfidenceLevel ();
   }

   public void setConfidenceLevel (double level) {
      sim.setConfidenceLevel (level);
   }
   
   public TimeUnit getDefaultUnit() {
      return sim.getDefaultUnit();
   }

   /**
    * Returns the default target number of replications.
    * 
    * @return the target number of replications.
    */
   public int getNumTargetReplications () {
      return m_numReps;
   }

   public MatrixOfTallies<Tally> getMatrixOfStatProbes (
         PerformanceMeasureType pm) {
      final MatrixOfTallies<Tally> mta = pmMap.get (pm);
      if (mta == null) {
         if (hasPerformanceMeasure (pm))
            return null;
         throw new NoSuchElementException (
               "Performance measure not supported: " + pm);
      }
      return mta;
   }

   /**
    * Equivalent to {@link #eval(int) eval}
    * \texttt{(getNumTargetReplications())}.
    */
   public void eval () {
      eval (m_numReps);
   }

   /**
    * Performs \texttt{numReps} independent replications of a batch means
    * simulation.
    * 
    * @param numReps
    *           the number of replications.
    */
   public void eval (int numReps) {
      sim.setAutoResetStartStream (false);
      for (int r = 0; r < numReps; r++) {
         sim.eval ();
         for (final PerformanceMeasureType pm : pms) {
            final DoubleMatrix2D mat = sim.getPerformanceMeasure (pm);
            MatrixOfTallies<Tally> mta;
            if (r == 0) {
               mta = MatrixOfTallies.createWithTally (mat.rows (), mat
                     .columns ());
               mta.setName (pm.getDescription ());
               pmMap.put (pm, mta);
            }
            else
               mta = pmMap.get (pm);
            mta.add (mat);
         }
         sim.resetNextSubstream ();
      }
      setOneSimDone (true);
      numDoneReps = numReps;
      if (autoResetStartStream)
         sim.resetStartStream ();
   }

   public void resetStartStream () {
      sim.resetStartStream ();
   }

   public void resetStartSubstream () {
      sim.resetStartSubstream ();
   }

   public void resetNextSubstream () {
      sim.resetNextSubstream ();
   }

   public PerformanceMeasureType[] getPerformanceMeasures () {
      return pms;
   }

   public boolean hasEvalOption (EvalOptionType option) {
      return sim.hasEvalOption (option);
   }

   public EvalOptionType[] getEvalOptions () {
      return sim.getEvalOptions ();
   }

   public Object getEvalOption (EvalOptionType option) {
      return sim.getEvalOption (option);
   }

   public void setEvalOption (EvalOptionType option, Object val) {
      sim.setEvalOption (option, val);
   }

   @Override
   public String getAgentGroupName (int i) {
      return sim.getAgentGroupName (i);
   }

   @Override
   public String getContactTypeName (int k) {
      return sim.getContactTypeName (k);
   }

   @Override
   public String getWaitingQueueName (int q) {
      return sim.getWaitingQueueName (q);
   }

   public int getNumAgentGroups () {
      return sim.getNumAgentGroups ();
   }

   public int getNumContactTypes () {
      return sim.getNumContactTypes ();
   }

   public int getNumInContactTypes () {
      return sim.getNumInContactTypes ();
   }

   public int getNumMainPeriods () {
      return sim.getNumMainPeriods ();
   }

   public int getNumOutContactTypes () {
      return sim.getNumOutContactTypes ();
   }

   public int getNumWaitingQueues () {
      return sim.getNumWaitingQueues ();
   }

   public boolean seemsUnstable () {
      return sim.seemsUnstable ();
   }

   public void reset () {
      sim.reset ();
      pms = sim.getPerformanceMeasures ();
      pmMap.clear ();
      setOneSimDone (false);
      numDoneReps = 0;
   }

   public int getCompletedSteps () {
      return numDoneReps;
   }

   public void newSeeds () {
      sim.newSeeds ();
      pms = sim.getPerformanceMeasures ();
      pmMap.clear ();
      setOneSimDone (false);
      numDoneReps = 0;
   }

   @Override
   public String formatStatistics () {
      return formatStatistics ();
   }

   public int getNumMatricesOfAWT () {
      return sim.getNumMatricesOfAWT ();
   }

   /**
    * Main method allowing to run this class from the command-line with
    * {@link CallCenterSim} as the internal simulator. The needed command-line
    * arguments are the name of an XML file containing the simulation parameters
    * (root element \texttt{mskccparams}), and the name of a second XML file
    * containing the simulation parameters (root elements
    * \texttt{batchsimparams}).
    * 
    * @param args
    *           the command-line arguments.
    */
   public static void main (String[] args) throws IOException,
         ParserConfigurationException, SAXException {
      if (args.length != 3) {
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.optim.MBatchMeansSim "
                     + "[simulation data file name] [batch means parameter file] [number of reps]");
         System.exit (1);
      }
      if (!new File (args[0]).exists ()) {
         System.err.println ("Cannot find the file " + args[0]);
         System.exit (1);
      }
      if (!new File (args[1]).exists ()) {
         System.err.println ("Cannot find the file " + args[1]);
         System.exit (1);
      }
      final int numReps = Integer.parseInt (args[2]);


      final CallCenterParamsConverter cnv = new CallCenterParamsConverter();
      final SimParamsConverter cnvSim = new SimParamsConverter();
      final CallCenterParams ccParams = cnv.unmarshalOrExit (new File (args[0]));
      final SimParams simParams = cnvSim.unmarshalOrExit (new File (args[1]));
      final CallCenterSim isim;
      try {
         isim = new CallCenterSim (ccParams, simParams);
      }
      catch (final CallCenterCreationException cce) {
         System.err.println (ExceptionUtil.throwableToString (cce));
         System.exit (1);
         return;
      }
      final MBatchMeansSim sim = new MBatchMeansSim (isim, numReps);

      System.out.println ("Call center parameter file: " + args[0]);
      System.out.println ("Simulation parameter file: " + args[1]);
      System.out.println ("Experiment started on " + new Date ().toString ());
      sim.eval ();
      System.out.println ("Number of completed replications: "
            + sim.getCompletedSteps ());
      System.out.println (sim.formatStatistics ());
   }
}
