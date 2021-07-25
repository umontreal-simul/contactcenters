package umontreal.iro.lecuyer.contactcenters.msk;

import java.net.URL;
import java.util.Formatter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import junit.framework.TestCase;
import umontreal.iro.lecuyer.contactcenters.app.CompareSimResults;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterEval;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSimResults;
import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.params.BatchSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.RepSimParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import cern.colt.matrix.DoubleMatrix2D;

public class CallCenterSimTest extends TestCase {
   CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
   SimParamsConverter cnvSim = new SimParamsConverter();
   ContactCenterSimResults refRes;
   DatatypeFactory df;

   public CallCenterSimTest (String name) {
      super (name);
      try {
         df = DatatypeFactory.newInstance ();
      }
      catch (DatatypeConfigurationException dce) {
         df = null;
      }
   }

   @Override
   public void setUp () {
   }

   @Override
   public void tearDown () {
   }

   public void testParams () throws Exception {
      final URL url = getURL ("mskccParamsThreeTypesReg.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      assertEquals ("Number of call types", 6, ccPs.getInboundTypes ().size ());
      assertEquals ("Number of agent groups", 4, ccPs.getAgentGroups ().size ());
      assertEquals ("Number of periods", 1, ccPs.getNumPeriods ());
//      final InboundTypeParams par = ccPs.getInboundTypes().get (0);
//      final double prate = par.getPatienceTime ().getGenParams (0)
//            .getExpLambda ();
//      assertEquals ("Patience rate for call type 0", 12, prate, 1e-6);
//      final double srate = par.getServiceTime ().getGenParams (0)
//            .getExpLambda ();
//      assertEquals ("Service rate for call type 0", 60, srate, 1e-6);
//      assertEquals ("Arrival rate of call type 0", 60, par.getArrivalProcess ()
//            .getArrivals ()[0], 1e-6);
   }

   public void testParamsMultiPeriods () throws Exception {
      final URL url = getURL ("mskInOutSim.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      assertEquals ("Number of call types", 4, ccPs.getInboundTypes ().size () + ccPs.getOutboundTypes ().size ());
      assertEquals ("Number of agent groups", 4, ccPs.getAgentGroups ().size ());
      assertEquals ("Number of periods", 5, ccPs.getNumPeriods ());
//      final InboundTypeParams par = ccPs.getInboundType (1);
//      final double prate = par.getPatienceTime ().getGenParams (1)
//            .getExpLambda ();
//      assertEquals ("Patience rate for call type 1 in period 1", 6, prate, 1e-6);
//      final double srate = par.getServiceTime (3).getGenParams (1)
//            .getExpLambda ();
//      assertEquals ("Service rate for call type 1 by agent group 3", 29, srate,
//            1e-6);
   }

   public void testBatchMultipleResets () throws Exception {
      URL url = getURL ("mskccParamsThreeTypesReg.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      url = getURL ("batchSimParams.xml");
      final BatchSimParams simPs = (BatchSimParams)cnvSim.unmarshal (url);
      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.eval ();
      makeInitialRun (sim);
      makeSameRun (sim,
            "Stationary simulation with same parameters, without reset");
      sim.reset ();
      makeSameRun (sim, "Stationary simulation with same parameters and reset");
      sim.newSeeds ();
      makeSameRunStat (sim,
            "Stationary simulation with same parameters and new seeds");
   }

//   public void testBatchParamsChange () throws Exception {
//      URL url = getURL ("mskccParamsThreeTypesReg.xml");
//      final CallCenterParams ccPs = cnvCC.unmarshal (url);
//      url = getURL ("batchSimParams.xml");
//      final BatchSimParams simPs = (BatchSimParams)cnvSim.unmarshal (url);
//      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
//      sim.eval ();
//      makeInitialRun (sim);
//      assertEquals ("Number of performed batches", 15, sim.getCompletedSteps ());
//      simPs.setBatchSize (df.newDuration ("PT5H"));
//      makeSameRunStat (sim, "Increasing batch size");
//      assertEquals ("Number of performed batches", 15, sim.getCompletedSteps ());
//      simPs.setBatchSize (df.newDuration ("PT5H"));
//      simPs.setMinBatches (45);
//      makeSameRunStat (sim, "Increasing the number of batches");
//      assertEquals ("Number of performed batches", 45, sim.getCompletedSteps ());
//      simPs.setAggregation (false);
//      makeSameRunStat (sim, "Disabling batch aggregation");
//      assertEquals ("Number of performed batches", 45, sim.getCompletedSteps ());
//      simPs.setMinBatches (15);
////    simPs.setTargetError (0.02);
////    makeSameRunStat (sim, "Using a target relative error");
////    assertTrue ("Number of performed batches should be larger than 30", sim
////    .getCompletedSteps () > 15);
//   }

   public void testBatchMatrixDimensions () throws Exception {
      URL url = getURL ("mskccParamsThreeTypesReg.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      url = getURL ("batchSimParams.xml");
      final BatchSimParams simPs = (BatchSimParams) cnvSim.unmarshal (url);
      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.eval ();
      testDimensions (sim);
   }

   public void testRepMatrixDimensions () throws Exception {
      URL url = getURL ("mskInOutSim.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      url = getURL ("repSimParams.xml");
      final RepSimParams simPs = (RepSimParams) cnvSim.unmarshal (url);
      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.eval ();
      testDimensions (sim);
   }

   private void testDimensions (ContactCenterEval eval) {
      for (final PerformanceMeasureType pm : eval.getPerformanceMeasures ()) {
         final DoubleMatrix2D m = eval.getPerformanceMeasure (pm);
         assertEquals ("Number of rows for " + pm.name (), pm.rows (eval), m
               .rows ());
         assertEquals ("Number of columns for " + pm.name (),
               pm.columns (eval), m.columns ());
      }
   }

   public void testRepMultipleResets () throws Exception {
      URL url = getURL ("mskInOutSimNoOutbound.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      url = getURL ("repSimParams.xml");
      final RepSimParams simPs = (RepSimParams) cnvSim.unmarshal (url);
      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.eval ();
      makeInitialRun (sim);
      makeSameRun (sim,
            "Non-stationary simulation with same parameters, without reset");
      sim.reset ();
      makeSameRun (sim,
            "Non-stationary simulation with same parameters and reset");
      sim.newSeeds ();
      makeSameRunStat (sim,
            "Non-stationary simulation with same parameters and new seeds");
   }

//   public void testRepChangeParams () throws Exception {
//      URL url = getURL ("mskInOutSim.xml");
//      final CallCenterParams ccPs = cnvCC.unmarshal (url);
//      url = getURL ("repSimParams.xml");
//      final RepSimParams simPs = (RepSimParams) cnvSim.unmarshal (url);
//      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
//      sim.eval ();
//      makeInitialRun (sim);
//
//      // Increase service time of calls of type 0 by agent group 0
//      ccPs.getCallType (0).getServiceTime (0).setExpLambda (41.5);
//      sim.reset ();
//      makeDifferentRunStat (sim,
//            "Increased service rate for first inbound call type");
//      ccPs.getCallType (0).getServiceTime (0).setExpLambda (60.0);
//      sim.reset ();
//      makeSameRun (sim, "Service time reset to its initial value");
//
//      // Change the arrival rates for the first inbound type
//      final double[] oldArrivals = ccPs.getInboundType (0).getArrivalProcess ()
//            .getArrivals ();
//      ccPs.getInboundType (0).getArrivalProcess ().setArrivals (
//            new double[] { 30.0, 80.0, 60.0, 25.0, 34.0 });
//      sim.reset ();
//      makeDifferentRunStat (sim,
//            "Different arrival rates for inbound call type 0");
//      ccPs.getInboundType (0).getArrivalProcess ().setArrivals (oldArrivals);
//      sim.reset ();
//      makeSameRun (sim, "Arrival process reset to its initial value");
//
//      final int[] staffing = (int[]) sim
//            .getEvalOption (EvalOptionType.STAFFINGVECTOR);
//      final int[] expStaffing = { 1, 5, 6, 9, 3, 2, 7, 6, 3, 9, 3, 5, 5, 4, 4,
//            2, 4, 6, 4, 5 };
//      assertArrayEquals ("Invalid staffing vector", expStaffing, staffing);
//      sim.setEvalOption (EvalOptionType.STAFFINGVECTOR, new int[] { 2, 3, 1, 4,
//            6, 1, 2, 3, 6, 4, 4, 3, 2, 5, 5, 5, 9, 8, 6, 4 });
//      makeDifferentRunStat (sim, "Different staffing vector");
//      sim.setEvalOption (EvalOptionType.STAFFINGVECTOR, staffing);
//      makeSameRun (sim, "Staffing reset to its initial value");
//
//      // Try to change dialer policy
//      /*
//       * ccPs.getOutboundType (0).getDialer().setDialerPolicy
//       * (DialerPolicyType.DIALFREE_BADCALLMISMATCHRATES); sim.reset();
//       * makeDifferentRunStat (sim, "Different dialer policy for outbound call
//       * type 0"); makeInitialRun (sim); sim = new CallCenterSim (ccPs, simPs,
//       * sim.getStreams()); makeSameRun (sim, "Same run with changed dialer
//       * policy, after reconstructing " + "the CallCenterSim object");
//       */
//   }

   public void testBatchMultiplePeriods () throws Exception {
      URL url = getURL ("mskInOutSim.xml");
      final CallCenterParams ccPs = cnvCC.unmarshal (url);
      url = getURL ("batchSimParams.xml");
      final BatchSimParams simPs = (BatchSimParams) cnvSim.unmarshal (url);
      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.eval ();
      makeInitialRun (sim);
      int[] staffing = (int[]) sim
            .getEvalOption (EvalOptionType.STAFFINGVECTOR);
      final int[][] allStaffing = new int[][] { { 1, 2, 3, 2 }, { 5, 7, 5, 4 },
            { 6, 6, 5, 6 }, { 9, 3, 4, 4 }, { 3, 9, 4, 5 } };
      assertArrayEquals ("Invalid staffing vector at period 0", allStaffing[0],
            staffing);
      sim.setEvalOption (EvalOptionType.STAFFINGVECTOR,
            new int[] { 5, 3, 1, 1 });
      makeDifferentRunStat (sim,
            "Simulation from period 0 with different staffing");
      for (int p = 1; p < ccPs.getNumPeriods (); p++) {
         simPs.setCurrentPeriod (p);
         staffing = (int[]) sim.getEvalOption (EvalOptionType.STAFFINGVECTOR);
         assertArrayEquals ("Invalid staffing vector at period " + p,
               allStaffing[p], staffing);
         makeDifferentRunStat (sim, "Simulation for period " + p);
      }
   }

//   public void testRepCaching () throws Exception {
//      InputStream is = openFile ("mskInOutSim.xml");
//      final CallCenterParams ccPs = (CallCenterParams) reader.read (is);
//      is.close ();
//      ccPs.check ();
//      is = openFile ("repSimParams.xml");
//      final RepSimParams simPs = (RepSimParams) reader.read (is);
//      is.close ();
//      simPs.setCaching (true);
//      simPs.check ();
//      final CallCenterSim sim = new CallCenterSim (ccPs, simPs);
//      sim.eval ();
//      makeInitialRun (sim);
//      makeSameRun (sim, "Using cached random values");
//   }

   private void assertArrayEquals (String msg, int[] exp, int[] res) {
      if (res.length != exp.length)
         fail (msg + ": array length (" + res.length
               + ") different from expected array length (" + exp.length + ")");
      for (int i = 0; i < exp.length; i++)
         assertTrue (msg + ": value at element " + i + " (" + res[i]
               + ") diffrent from " + "expected value (" + exp[i] + ")",
               exp[i] == res[i]);
   }

   private void makeInitialRun (ContactCenterSim sim) {
      refRes = new ContactCenterSimResults (sim);
   }

   private void makeSameRun (ContactCenterSim sim, String comment) {
      sim.eval ();
      final Formatter fmt = new Formatter();
      if (!CompareSimResults.equals (refRes, sim, 1e-6, fmt)) {
         final String failMsg = comment + ": values of some performance measures changed";
         System.err.println (failMsg);
         System.err.println (fmt.toString());
         System.err.println();
         fail (failMsg);
      }
   }

   private void makeSameRunStat (ContactCenterSim sim, String comment) {
      sim.eval ();
      final Formatter fmt = new Formatter();
      if (!CompareSimResults.equalsStat (refRes, sim, 0.99, 1e-6, fmt)) {
         final String failMsg = comment + ": values of some performance measures changed significantly";
         System.err.println (failMsg);
         System.err.println (fmt.toString());
         System.err.println();
         fail (failMsg);
      }
   }

   void makeDifferentRun (ContactCenterSim sim, String comment) {
      sim.eval ();
      if (CompareSimResults.equals (refRes, sim, 1e-6, null))
         fail (comment + ": values of performance measures did not change");
   }

   private void makeDifferentRunStat (ContactCenterSim sim, String comment) {
      sim.eval ();
      if (CompareSimResults.equalsStat (refRes, sim, sim.getConfidenceLevel(), 1e-6, null))
         fail (comment + ": values of performance measures did not change significantly");
   }
   
   private URL getURL (String name) {
      URL url = getClass ().getClassLoader ().getResource ("umontreal/iro/lecuyer/contactcenters/msk/" + name);
      assertNotNull ("Cannot find file " + name, url);
      return url;
   }
}
