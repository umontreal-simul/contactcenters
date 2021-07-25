package umontreal.iro.lecuyer.contactcenters.msk;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureFormat;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.params.RepSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SequentialSamplingParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatCallCenterStat;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatType;
import umontreal.ssj.hups.DigitalNet;
import umontreal.ssj.hups.PointSet;
import umontreal.ssj.hups.PointSetIterator;
import umontreal.ssj.hups.SobolSequence;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ExceptionUtil;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;

/**
 * Extends the {@link CallCenterSim} class for randomized Quasi-Monte Carlo
 * simulation.
 */
public class CallCenterSimRQMC extends AbstractCallCenterSim {
   private PointSet pointSet;
   private PointSetIterator pointSetIter;
   private StatCallCenterStat statAvg;

   /**
    * Constructs a new randomized Quasi-Monte Carlo call center simulator using
    * the call center parameters \texttt{ccParams}, and simulation parameters
    * \texttt{simParams}, with a point set containing \texttt{numPoints} points.
    *
    * @param ccParams
    *           the call center parameters.
    * @param simParams
    *           the simulation parameters.
    * @param numPoints
    *           the number of points in the point set.
    */
   public CallCenterSimRQMC (CallCenterParams ccParams, RepSimParams simParams,
         int numPoints) throws CallCenterCreationException {
      super (ccParams, simParams);
      final SimLogic simLogic = getSimLogic ();
      pointSet = createPointSet (numPoints);
      statAvg = new StatCallCenterStat (simLogic.getCallCenterStatProbes (),
            StatType.AVERAGE, true);
   }

   /**
    * Returns the point set used by this simulator.
    *
    * @return the point set being used.
    */
   public PointSet getPointSet () {
      return pointSet;
   }

   /**
    * Creates the point set used for Quasi-Monte Carlo, which contains
    * \texttt{numPoints} points. By default, this creates a Sobol sequence with
    * one dimension, and containing \texttt{numPoints} points.
    *
    * @param numPoints
    *           the number of points in the point set.
    * @return the constructed point set.
    */
   protected PointSet createPointSet (int numPoints) {
      return new SobolSequence (numPoints, 1);
   }

   /**
    * Configures the simulator for generating random numbers from the point set
    * rather than from the default random streams. By default, this changes the
    * busyness generator to obtain the busyness factor from the first dimension
    * of the point set.
    */
   protected void installPointSet () {
      pointSetIter = pointSet.iterator ();
      final CallCenter cc = getCallCenter ();
      cc.setBusynessGen (new RandomVariateGen (pointSetIter, cc
            .getBusynessGen ().getDistribution ()));
      cc.getRandomStreams ().getRandomStreamsInit ().add (pointSetIter);
   }

   /**
    * Restors the simulator to stop using the point set.
    */
   protected void uninstallPointSet () {
      final CallCenter cc = getCallCenter ();
      RandomVariateGen rvg;
      try {
         rvg = ParamReadHelper.createGenerator (
         		cc.getCallCenterParams ().getBusynessGen (), cc.getRandomStreams ().getStreamB ());
      }
      catch (final DistributionCreationException dce) {
         throw new IllegalStateException
         ("Cannot create the distribution for the busyness factor");
      }
      catch (final GeneratorCreationException gce) {
         throw new IllegalStateException
         ("Cannot create the generator for the busyness factor");
      }
      cc.setBusynessGen (rvg);
      cc.getRandomStreams ().getRandomStreamsInit ().remove (pointSetIter);
   }

   /**
    * Randomize the point set for a new macro-replication. By default, this
    * applies an affine matrix scrambbling followed by a random digital shift.
    */
   protected void randomizePointSet () {
      final RandomStream stream = getCallCenter ().getRandomStreams ().getStreamB ();
      final DigitalNet net = (DigitalNet) pointSet;
      net.leftMatrixScramble (stream);
      net.addRandomShift (0, net.getDimension (), stream);
   }

   @Override
   public void eval () {
      final RepSimParams simParams = (RepSimParams) getSimLogic ()
            .getSimParams ();
      final List<SequentialSamplingParams> seqParams = simParams.getSequentialSampling();
      final int nr = simParams.getMinReplications ();
      final boolean ar = getAutoResetStartStream ();
      try {
         simParams.unsetSequentialSampling();
         simParams.setMinReplications (pointSet.getNumPoints ());
         setAutoResetStartStream (false);
         installPointSet ();
         statAvg.init ();
         for (int i = 0; i < nr; i++) {
            randomizePointSet ();
            pointSetIter.resetStartStream ();
            super.eval ();
            statAvg.addStat ();
         }
         applyControlVariables ();
         if (ar)
            resetStartStream ();
      }
      finally {
         uninstallPointSet ();
         simParams.setMinReplications (nr);
         simParams.getSequentialSampling().addAll (seqParams);
         setAutoResetStartStream (ar);
      }
   }

   @Override
   public void reset () {
      super.reset ();
      statAvg = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.AVERAGE, true);
   }

   @Override
   public void reset (CallCenterParams ccParams, SimParams simParams) throws CallCenterCreationException {
      super.reset (ccParams, simParams);
      statAvg = new StatCallCenterStat (getSimLogic ()
            .getCallCenterStatProbes (), StatType.AVERAGE, true);
   }

   @Override
   public CallCenterStatProbes getCallCenterStatProbes() {
      return statAvg;
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
   public static void main (String[] args) throws IOException,
   CallCenterCreationException,
         JAXBException {
      if (args.length != 2 && args.length != 3) {
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.msk.CallCenterSimRQMC "
                     + "<call center data file name> <experiment parameter file> "
                     + "[<output file name>]");
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
      File outputFile = null;
      if (args.length == 3)
         outputFile = new File (args[2]);

      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      final SimParamsConverter cnvSim = new SimParamsConverter();
      final CallCenterParams ccParams;
      final RepSimParams simParams;
      try {
         ccParams = cnvCC.unmarshal (new File (args[0]));
         simParams = (RepSimParams)cnvSim.unmarshal (new File (args[1]));
      }
      catch (final JAXBException je) {
         System.err.println (ExceptionUtil.throwableToString (je));
         System.exit (1);
         return;
      }

      final CallCenterSimRQMC sim = new CallCenterSimRQMC (ccParams, simParams,
            512);
      System.out.println (sim.getPointSet ().toString ());
      System.out.println (sim.getPointSet ().getNumPoints ());

      PerformanceMeasureFormat.addExperimentInfo (sim.getEvalInfo (), args[0], args[1]);
      // sim.setUsingControlVariables (true);
      System.out.println ("Making production runs");
      sim.eval ();
      PerformanceMeasureFormat.formatResults (sim, outputFile);
   }
}
