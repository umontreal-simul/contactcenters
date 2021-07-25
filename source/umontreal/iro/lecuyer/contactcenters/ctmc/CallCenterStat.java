package umontreal.iro.lecuyer.contactcenters.ctmc;

import java.util.EnumMap;
import java.util.Map;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterEval;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.list.ListOfTallies;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.ssj.util.RatioFunction;

/**
 * Regroups tallies collecting observations obtained
 * from independent replications of a simulation using
 * a CTMC in the case of an individual period.
 * After an object of this class is constructed, it
 * can be initialized using {@link #init(CallCenterCTMC)}.
 * While transitions are simulated, a set of counters
 * represented by an instance of {@link CallCenterCounters}
 * is updated.
 * At the end of the replication, the set of counters is given
 * to the {@link #addObs(CallCenterCounters,double)}
 * method of this class to collect the observations.
 * Statistical collectors are regrouped into matrices concerning
 * types of performance measures.
 * The method {@link #getMatrixOfStatProbes(PerformanceMeasureType)}
 * can be used to obtain the matrix of statistical probes
 * for a given type of performance measure.
 */
public class CallCenterStat {
   private Map<PerformanceMeasureType, MatrixOfStatProbes<?>> pmTallies = new EnumMap<PerformanceMeasureType, MatrixOfStatProbes<?>> (PerformanceMeasureType.class);
   private PerformanceMeasureType[] pms;
   private double jumpRate;
   private int ns;
   private double[] lambda;
   private boolean keepQueues;
   
   protected Tally statNumTransitions;
   protected Tally statNumFalseTransitions;
   protected ListOfTallies<?> statArrivals;
   protected ListOfTallies<?> statBlocked;
   protected ListOfTallies<?> statAbandoned;
   protected ListOfTallies<?> statService;
   protected ListOfTallies<?> statQueueSize;
   protected ListOfTallies<?> statBusyAgents;
   protected ListOfTallies<?> statTotalAgents;
   protected MatrixOfTallies<?> statServedRates;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statOccupancy;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statWaitingTime;
   protected ListOfTallies<?> statSumWaitingTimesServed;
   protected ListOfTallies<?> statSumWaitingTimesAbandoned;
   protected ListOfTallies<?> statSumWaitingTimes;
   protected ListOfTallies<?> statMaxQueueSize;
   protected ListOfTallies<?> statMaxBusyAgents;

   protected ListOfTallies<?> statGoodSL;
   protected ListOfTallies<?> statServedBeforeAWT;
   protected ListOfTallies<?> statAbandonedBeforeAWT;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statSL;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statSL2;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statWaitingTimeServed;
   protected ListOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> statWaitingTimeAbandoned;

   /**
    * Constructs a new set of statistical probes based on the
    * CTMC model \texttt{ctmc}, with
    * \texttt{ns} matrices of acceptable waiting times.
    * The boolean \texttt{keepObs} indicates if observations
    * are kept while collecting statistics.
    * @param ctmc the call center CTMC.
    * @param ns the number of matrices of acceptable waiting times.
    * @param keepObs determines if collected observations are kept.
    */
   public CallCenterStat (CallCenterCTMC ctmc, int ns, boolean keepObs) {
      //this.ctmc = ctmc;
      int numTypes = ctmc.getNumContactTypes ();
      int numGroups = ctmc.getNumAgentGroups ();
      int nt = numTypes > 1 ? numTypes + 1 : numTypes;
      int ng = numGroups > 1 ? numGroups + 1 : numGroups;
      this.ns = ns;
      if (keepObs) {
         statNumTransitions = new TallyStore ("Number of transitions");
         statNumFalseTransitions = new TallyStore (
               "Number of false transitions");
      }
      else {
         statNumTransitions = new Tally ("Number of transitions");
         statNumFalseTransitions = new Tally (
               "Number of false transitions");
      }
      
      statArrivals = createTallies (nt, keepObs);
      statBlocked = createTallies (nt, keepObs);
      statAbandoned = createTallies (nt, keepObs);
      statService = createTallies (nt, keepObs);
      statQueueSize = createTallies (nt, keepObs);
      statSumWaitingTimes = createTallies (nt, keepObs);
      statSumWaitingTimesAbandoned = createTallies (nt, keepObs);
      statSumWaitingTimesServed = createTallies (nt, keepObs);
      statMaxQueueSize = createTallies (nt, keepObs);
      addPMTally (PerformanceMeasureType.RATEOFARRIVALS, statArrivals);
      addPMTally (PerformanceMeasureType.RATEOFBLOCKING, statBlocked);
      addPMTally (PerformanceMeasureType.RATEOFABANDONMENT, statAbandoned);
      addPMTally (PerformanceMeasureType.RATEOFSERVICES, statService);
      addPMTally (PerformanceMeasureType.AVGQUEUESIZE, statQueueSize);
      addPMTally (PerformanceMeasureType.MAXQUEUESIZE, statMaxQueueSize);

      statBusyAgents = createTallies (ng, keepObs);
      statMaxBusyAgents = createTallies (ng, keepObs);
      statTotalAgents = createTallies (ng, keepObs);
      statOccupancy = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, ng);
      statWaitingTime = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, nt);
      addPMTally (PerformanceMeasureType.AVGBUSYAGENTS, statBusyAgents);
      addPMTally (PerformanceMeasureType.AVGSCHEDULEDAGENTS, statTotalAgents);
      addPMFMMTally (PerformanceMeasureType.OCCUPANCY, statOccupancy);
      addPMFMMTally (PerformanceMeasureType.WAITINGTIME, statWaitingTime);
      addPMTally (PerformanceMeasureType.MAXBUSYAGENTS, statMaxBusyAgents);

      statGoodSL = createTallies (nt*ns, keepObs);
      statServedBeforeAWT = createTallies (nt*ns, keepObs);
      statAbandonedBeforeAWT = createTallies (nt*ns, keepObs);
      statSL = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, nt*ns);
      statSL2 = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, nt*ns);
      statWaitingTimeServed = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, nt);
      statWaitingTimeAbandoned = ListOfFunctionOfMultipleMeansTallies.create (
            new RatioFunction (), 2, nt);
      if (ctmc instanceof CallCenterCTMCWithQueues) {
         keepQueues = true;
         if (ns > 0) {
            addPMTally (PerformanceMeasureType.RATEOFSERVICESBEFOREAWT,
                  statServedBeforeAWT);
            addPMTally (PerformanceMeasureType.RATEOFABANDONMENTBEFOREAWT,
                  statAbandonedBeforeAWT);
            addPMTally (PerformanceMeasureType.RATEOFINTARGETSL, statGoodSL);
            addPMFMMTally (PerformanceMeasureType.SERVICELEVEL, statSL);
            addPMFMMTally (PerformanceMeasureType.SERVICELEVEL2, statSL2);
         }
         addPMFMMTally (PerformanceMeasureType.SPEEDOFANSWER,
               statWaitingTimeServed);
         addPMFMMTally (PerformanceMeasureType.TIMETOABANDON,
               statWaitingTimeAbandoned);
         addPMTally (PerformanceMeasureType.SUMWAITINGTIMES, statSumWaitingTimes);
         addPMTally (PerformanceMeasureType.SUMWAITINGTIMESSERVED, statSumWaitingTimesServed);
         addPMTally (PerformanceMeasureType.SUMWAITINGTIMESABANDONED, statSumWaitingTimesAbandoned);
      }
      else
         keepQueues = false;
      statServedRates = createTallies (nt, ng, keepObs);
      pmTallies.put (PerformanceMeasureType.SERVEDRATES, statServedRates);
   }
   
   private ListOfTallies<?> createTallies (int size, boolean keepObs) {
      if (keepObs)
         return ListOfTallies.createWithTallyStore (size);
      else
         return ListOfTallies.createWithTally (size);
   }
   
   private MatrixOfTallies<?> createTallies (int rows, int columns, boolean keepObs) {
      if (keepObs)
         return MatrixOfTallies.createWithTallyStore (rows, columns);
      else
         return MatrixOfTallies.createWithTally (rows, columns);
   }
   
   /**
    * Creates a single-column matrix of tallies
    * from the list of tallies \texttt{lta}, and binds
    * the performance measure type \texttt{pm} to that
    * new matrix of tallies.
    * @param pm the type of the performance measure.
    * @param lta the list of tallies.
    */
   private void addPMTally (PerformanceMeasureType pm, ListOfTallies<?> lta) {
      MatrixOfTallies<Tally> mta = new MatrixOfTallies<Tally> (lta.size (), 1);
      for (int i = 0; i < lta.size (); i++)
         mta.set (i, 0, lta.get (i));
      pmTallies.put (pm, mta);
      pms = null;
   }

   /**
    * Similar to {@link #addPMTally(PerformanceMeasureType,ListOfTallies)},
    * for a matrix and a list containing functions of multiple
    * means tallies.
    * @param pm the type of performance measure.
    * @param lta the list of tallies.
    */
   private void addPMFMMTally (PerformanceMeasureType pm,
         ListOfFunctionOfMultipleMeansTallies<?> lta) {
      MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta = new MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> (
            lta.size (), 1);
      for (int i = 0; i < lta.size (); i++)
         mta.set (i, 0, lta.get (i));
      pmTallies.put (pm, mta);
      pms = null;
   }
   
   
   /**
    * Returns the tally for statistics on the number
    * of false transitions, also called self jumps.
    * @return the tally for collecting the number of false transitions.
    */
   public Tally getStatNumFalseTransitions () {
      return statNumFalseTransitions;
   }

   /**
    * Returns the tally for statistics on the number
    * of simulated transitions.
    * @return the tally for collecting the number of simulated transitions.
    */
   public Tally getStatNumTransitions () {
      return statNumTransitions;
   }

   /**
    * Returns an array of types of performance measures for
    * which statistics are collected by this object.
    * @return the array of performance measures.
    */
   public PerformanceMeasureType[] getPerformanceMeasures () {
      if (pms == null)
         pms = pmTallies.keySet ().toArray (
               new PerformanceMeasureType[pmTallies.size ()]);
      return pms;
   }
   
   /**
    * Returns a map associating each supported type of performance
    * measure with a matrix of statistical probes.
    * @return the map associating the performance measures
    * with matrices of statistical probes.
    */
   public Map<PerformanceMeasureType, MatrixOfStatProbes<?>> getMatricesOfStatProbes() {
      return pmTallies;
   }
   
   /**
    * Returns the matrix of statistical probes
    * corresponding to the performance measure type
    * \texttt{m}.
    * @param m the type of the performance measure.
    * @return the associated matrix of statistical probes.
    */
   public MatrixOfStatProbes<?> getMatrixOfStatProbes (PerformanceMeasureType m) {
      return (MatrixOfStatProbes<?>) pmTallies.get (m);
   }
   
   /**
    * Initializes the statistical probes in this object.
    * The given CTMC model is used for initializing
    * the arrival rates which are used
    * for some statistics.
    */
   public void init (CallCenterCTMC ctmc) {
      statArrivals.init ();
      statAbandoned.init ();
      statBlocked.init ();
      statService.init ();
      statQueueSize.init ();
      statBusyAgents.init ();
      statTotalAgents.init ();
      statOccupancy.init ();
      statGoodSL.init ();
      statServedBeforeAWT.init ();
      statAbandonedBeforeAWT.init ();
      statWaitingTime.init ();
      statWaitingTimeServed.init ();
      statWaitingTimeAbandoned.init ();
      statServedRates.init ();
      statSL.init ();
      statSL2.init ();
      statSumWaitingTimes.init ();
      statSumWaitingTimesAbandoned.init ();
      statSumWaitingTimesServed.init ();
      statMaxQueueSize.init ();
      statMaxBusyAgents.init ();
      statNumTransitions.init ();
      statNumFalseTransitions.init ();

      lambda = new double[statArrivals.size ()];
      int numTypes = ctmc.getNumContactTypes ();
      for (int k = 0; k < numTypes; k++)
         lambda[k] = ctmc.getArrivalRate (k);
      if (numTypes > 1)
         lambda[lambda.length - 1] = ctmc.getArrivalRate ();
      jumpRate = ctmc.getJumpRate ();
   }
   
   /**
    * Initializes the arrival rates used by this
    * statistical collector by summing
    * the arrival rates for all the CTMCs in the given array.
    * The arrival rates are used by {@link #addObs(CallCenterCounters,double)}
    * to compute the expected number of arrivals during the considered
    * period. By default, the arrival rates are initialized
    * from the parameters of the CTMC given to the
    * {@link #init(CallCenterCTMC)} method.
    * With this method, the arrival rates can be
    * replaced with sums over several periods, to get
    * the total arrival rate over all periods of an horizon.
    * @param ctmcs the array of CTMCs.
    */
   public void initLambda (CallCenterCTMC[] ctmcs) {
      lambda = new double[statArrivals.size ()];
      final int numTypes = ctmcs[0].getNumContactTypes ();
      for (int mp = 0; mp < ctmcs.length; mp++) {
         for (int k = 0; k < numTypes; k++) {
            final double l = ctmcs[mp].getArrivalRate (k);
            lambda[k] += l;
         }
         if (numTypes > 1) {
            final double l = ctmcs[mp].getArrivalRate ();
            lambda[lambda.length - 1] += l;
         }
      }
   }
   
   /**
    * Adds new observations obtained from \texttt{counters}
    * to the statistical probes managed by this object.
    * The period duration \texttt{periodDuration} is used
    * to multiply the arrival rates in order to get the
    * expected number of arrivals over the considered period.
    * @param counters the counters to get statistics from.
    * @param periodDuration the period duration.
    */
   public void addObs (CallCenterCounters counters, double periodDuration) {
      double[] arv = addSumElement (counters.numArrivals);
      statArrivals.add (arv);
      double[] ab = addSumElement (counters.numAbandoned);
      statAbandoned.add (ab);
      double[] bl = addSumElement (counters.numBlocked);
      statBlocked.add (bl);
      //double[] srv = addSumElement (counters.numServed);
      double[] srv = ab.clone();
      //double[] expArv = new double[arv.length];
      //for (int k = 0; k < expArv.length; k++)
      //   expArv[k] = lambda[k] * periodDuration;
      for (int k = 0; k < srv.length; k++)
         srv[k] = arv[k] - ab[k] - bl[k];
      statService.add (srv);
      double[] qs = addSumElement (counters.queueSize);
      statQueueSize.add (qs);
      double[] ba = addSumElement (counters.busyAgents);
      statBusyAgents.add (ba);
      double[] na = addSumElement (counters.totalAgents);
      statTotalAgents.add (na);
      statOccupancy.addSameDimension (ba, na);
      statNumTransitions.add (counters.numTransitions);
      statNumFalseTransitions.add (counters.numFalseTransitions);
      statMaxQueueSize.add (counters.maxQueueSize);
      statMaxBusyAgents.add (counters.maxBusyAgents);
      if (keepQueues) {
         double[] sw = addSumElement (counters.sumWaitingTimesServed);
         statWaitingTimeServed.addSameDimension (sw, srv);
         statSumWaitingTimesServed.add (sw);
         double[] sw2 = addSumElement (counters.sumWaitingTimesAbandoned);
         statWaitingTimeAbandoned.addSameDimension (sw2, ab);
         statSumWaitingTimesAbandoned.add (sw2);
         for (int k = 0; k < sw.length; k++)
            sw[k] += sw2[k];
         statSumWaitingTimes.add (sw);
         statWaitingTime.addSameDimension (sw, arv);

         if (ns > 0) {
            double[] numGoodSL = new double[counters.numServedBeforeAWT.length];
            for (int k = 0; k < numGoodSL.length; k++)
               numGoodSL[k] = counters.numServedBeforeAWT[k] + counters.numAbandonedBeforeAWT[k];
            statGoodSL.add (numGoodSL);
            statServedBeforeAWT.add (counters.numServedBeforeAWT);
            statAbandonedBeforeAWT.add (counters.numAbandonedBeforeAWT);

            double[] num = new double[counters.numServedBeforeAWT.length];
            double[] denum = new double[num.length];
            int nt = counters.numServedBeforeAWT.length / ns;
            for (int k = 0; k < num.length; k++) {
               num[k] = counters.numServedBeforeAWT[k];
               double v = counters.numAbandonedBeforeAWT[k];
               denum[k] = arv[k % nt] - v;
            }
            statSL.addSameDimension (num, denum);
            for (int k = 0; k < num.length; k++) {
               num[k] = counters.numServedBeforeAWT[k] + counters.numAbandonedBeforeAWT[k];
               denum[k] = arv[k % nt];
            }
            statSL2.addSameDimension (num, denum);
         }
      }
      else
         statWaitingTime.addSameDimension (qs, lambda);
      
      DoubleMatrix2D srM = new DenseDoubleMatrix2D (statArrivals.size (), statBusyAgents.size ());
      for (int ki = 0; ki < counters.servedRates.length; ki++) {
         final int k = ki / counters.busyAgents.length;
         final int i = ki % counters.busyAgents.length;
         srM.setQuick (k, i, counters.servedRates[ki]);
      }
      if (counters.numArrivals.length > 1) {
         for (int i = 0; i < srM.columns (); i++) {
            double sum = 0;
            for (int r = 0; r < srM.rows () - 1; r++)
               sum += srM.getQuick (r, i);
            srM.setQuick (srM.rows () - 1, i, sum);
         }
      }
      if (counters.busyAgents.length > 1) {
         for (int k = 0; k < srM.rows (); k++) {
            double sum = 0;
            for (int c = 0; c < srM.columns () - 1; c++)
               sum += srM.getQuick (k, c);
            srM.setQuick (k, srM.columns () - 1, sum);
         }
      }
      statServedRates.add (srM);
   }
   
   public static double[] addSumElement (double[] array) {
      if (array.length <= 1)
         return array;
      double[] res = new double[array.length + 1];
      System.arraycopy (array, 0, res, 0, array.length);
      double sum = 0;
      for (int i = 0; i < array.length; i++)
         sum += array[i];
      res[array.length] = sum;
      return res;
   }
   
   /**
    * Adds statistical information about the number of transitions
    * to a map of evaluation information.
    * Usually, the map is obtained using {@link ContactCenterEval#getEvalInfo()}, and
    * generated information is displayed in reports produced by a simulator.
    * @param evalInfo the evaluation information.
    * @param numExpectedTransitions the expected number of transitions.
    */
   public void formatReport (Map<String, Object> evalInfo, double numExpectedTransitions) {
      evalInfo.put ("Maximal transition rate", jumpRate);
      evalInfo.put ("Average number of transitions",
            statNumTransitions.average ());
      if (statNumTransitions.numberObs () > 1)
         evalInfo.put ("Variance on number of transitions",
               statNumTransitions.variance ());
      evalInfo.put ("Average number of false transitions",
            statNumFalseTransitions.average ());
      if (statNumFalseTransitions.numberObs () > 1)
         evalInfo.put ("Variance on number of false transitions",
               statNumFalseTransitions.variance ());
      evalInfo.put ("Proportion of false transitions",
            statNumFalseTransitions.average () / numExpectedTransitions);
   }
}
