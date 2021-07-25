package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.StatUtil;
import umontreal.iro.lecuyer.contactcenters.app.EstimationType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.RowType;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfFunctionOfMultipleMeansTallies;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import umontreal.ssj.util.RatioFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;

/**
 * Represents call center statistics obtained directly via call center measures.
 * An instance of this class is created using an instance of
 * {@link CallCenterMeasureManager}.
 * Each time the {@link #addObs()} method is called,
 * the counters are read from the call center measures, and
 * added to associated collectors.
 */
public class SimCallCenterStat extends AbstractCallCenterStatProbes {
   private final Logger logger = Logger
         .getLogger ("umontreal.iro.lecuyer.contactcenters.msk.stat");
   private static final double TOL = 1e-6;
   private CallCenter cc;
   private CallCenterMeasureManager ccm;
   private boolean keepObs;
   private boolean normalizeToDefaultUnit;

   private MatrixCache cache;

   /**
    * Constructs a new simulation-based call center statistics object. The
    * constructor queries the given simulation logic for the supported measure
    * types, and creates the necessary statistical probes.
    *
    * @param ccm
    *           the simulation logic.
    */
   public SimCallCenterStat (CallCenter cc, CallCenterMeasureManager ccm, boolean keepObs,
         boolean normalizeToDefaultUnit, PerformanceMeasureType... pms) {
      this.cc = cc;
      this.ccm = ccm;
      this.keepObs = keepObs;
      this.normalizeToDefaultUnit = normalizeToDefaultUnit;
      final Set<PerformanceMeasureType> pmSet = EnumSet
            .noneOf (PerformanceMeasureType.class);
      pmSet.addAll (Arrays.asList (pms));
      final int nt = cc.getNumContactTypesWithSegments ();
      final int ng = cc.getNumAgentGroupsWithSegments ();
      final int np = ccm.getNumPeriodsForStatProbes ();
      final int KI = cc.getNumInContactTypes ();
      final int nti = cc.getNumInContactTypesWithSegments ();
      final int nto = cc.getNumOutContactTypesWithSegments ();
      int nq = cc.getNumWaitingQueues ();
      if (nq > 1)
         ++nq;
      final int sl = cc.getNumMatricesOfAWT ();
      final int nsl = nti * sl;

      for (final PerformanceMeasureType pm : pmSet) {
         if (!ccm.hasMeasureMatricesFor (pm))
            throw new IllegalArgumentException
            ("The call center measure manager cannot be used to estimate performance measures of type " + pm);
         if (pm == PerformanceMeasureType.RATEOFARRIVALSIN)
            continue;
         final int nr;
         switch (pm.getRowType ()) {
            case AGENTGROUP:
               nr = ng;
               break;
            case CONTACTTYPE:
               nr = nt;
               break;
            case CONTACTTYPEAGENTGROUP:
               nr = nt * ng;
               break;
            case INBOUNDTYPE:
               nr = nti;
               break;
            case INBOUNDTYPEAGENTGROUP:
               nr = nti * ng;
               break;
            case INBOUNDTYPEAWT:
               nr = nsl;
               break;
            case INBOUNDTYPEAWTAGENTGROUP:
               nr = nsl * ng;
               break;
            case OUTBOUNDTYPE:
               nr = nto;
               break;
            case OUTBOUNDTYPEAGENTGROUP:
               nr = nto * ng;
               break;
            case WAITINGQUEUE:
               nr = nq;
               break;
            default:
               throw new AssertionError ("Unknown row type "
                     + pm.getRowType ().name ());
         }
         final int nc;
         switch (pm.getColumnType ()) {
            case AGENTGROUP:
               nc = ng;
               break;
            case MAINPERIOD:
               nc = np;
               break;
            case SINGLECOLUMN:
               nc = 1;
               break;
            default:
               throw new AssertionError ("Unknown column type "
                     + pm.getColumnType ().name ());
         }

         switch (pm.getEstimationType ()) {
            case EXPECTATION:
               createProbes (pmSet, pm, nr, nc);
               break;
            case EXPECTATIONOFFUNCTION:
               createProbes (pmSet, pm, nr, nc);
               break;
            case FUNCTIONOFEXPECTATIONS:
               createRatioProbes (pmSet, pm, nr, nc, pm.getZeroOverZeroValue ());
               break;
            case RAWSTATISTIC:
               createProbes (pmSet, pm, nr, nc);
               break;
         }
      }

      if (pmSet.contains (PerformanceMeasureType.RATEOFARRIVALSIN)) {
         final MatrixOfTallies<?> arrivals = getMatrixOfTallies (PerformanceMeasureType.RATEOFARRIVALS);
         if (arrivals != null
               && pmSet.contains (PerformanceMeasureType.RATEOFARRIVALSIN)) {
            final MatrixOfTallies<Tally> mta = new MatrixOfTallies<Tally> (nti,
                  np);
            for (int k = 0; k < KI; k++)
               for (int mp = 0; mp < np; mp++)
                  mta.set (k, mp, arrivals.get (k, mp));
            if (KI > 1) {
               final int nseg = cc.getNumInContactTypeSegments ();
               for (int s = 0; s <= nseg; s++)
                  for (int mp = 0; mp < np; mp++)
                     mta.set (KI + s, mp, keepObs ? new TallyStore ()
                           : new Tally ());

            }
            tallyMap.put (PerformanceMeasureType.RATEOFARRIVALSIN, mta);
         }
      }

      cache = new MatrixCache (cc, ccm);                          //Recuperation des matrice de performance dans CallCenterMeasureManger
   }                                                              // dans le cache.

   private MatrixOfTallies<?> createProbes (Set<PerformanceMeasureType> pmSet,
         PerformanceMeasureType pm, int numRows, int numColumns) {
      if (!pmSet.contains (pm))
         return null;
      if (!ccm.hasMeasureMatricesFor (pm))
         return null;
      final MatrixOfTallies<?> mta;
      if (keepObs)
         mta = MatrixOfTallies.createWithTallyStore (numRows, numColumns);
      else
         mta = MatrixOfTallies.createWithTally (numRows, numColumns);
      mta.setName (pm.getDescription ());
      tallyMap.put (pm, mta);
      return mta;
   }

   private MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> createRatioProbes (
         Set<PerformanceMeasureType> pmSet, PerformanceMeasureType pm,
         int numRows, int numColumns, double zeroOverZero) {
      if (!pmSet.contains (pm))
         return null;
      if (!ccm.hasMeasureMatricesFor (pm))
         return null;
      final RatioFunction ratio = new RatioFunction (zeroOverZero);
      final MatrixOfFunctionOfMultipleMeansTallies<FunctionOfMultipleMeansTally> mta = MatrixOfFunctionOfMultipleMeansTallies
            .create (ratio, 2, numRows, numColumns);
      mta.setName (pm.getDescription ());
      fmmTallyMap.put (pm, mta);
      return mta;
   }

   /**
    * Returns the call center measures used
    * for to collect observations.
    */
   public CallCenterMeasureManager getCallCenterMeasureManager () {
      return ccm;
   }

   private void addObs (PerformanceMeasureType pm, DoubleMatrix2D mo,
         int startingMainPeriod, int endingMainPeriod) {
      if (pm.getEstimationType () != EstimationType.EXPECTATION)
         throw new IllegalArgumentException ("The measure type " + pm.name ()
               + " does not estimate an expectation");
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      addObs (mta, mo, startingMainPeriod, endingMainPeriod);
   }

   private static void addObs (MatrixOfTallies<?> mt, DoubleMatrix2D mo,
         int startingMainPeriod, int endingMainPeriod) {
      if (mt == null || mo == null)
         return;
      assert mt.rows () == mo.rows ();
      assert mt.columns () == mo.columns ();
      if (startingMainPeriod == 0 && endingMainPeriod + 1 == mt.columns ())
         mt.add (mo);
      else {
         final int nr = mt.rows ();
         for (int mp = startingMainPeriod; mp < endingMainPeriod; mp++)
            for (int r = 0; r < nr; r++)
               mt.get (r, mp).add (mo.getQuick (r, mp));
      }
   }

   private void addRatio (PerformanceMeasureType pm, DoubleMatrix2D mx,
         DoubleMatrix2D my, int startingMainPeriod, int endingMainPeriod) {
      if (pm.getEstimationType () != EstimationType.EXPECTATIONOFFUNCTION)
         throw new IllegalArgumentException ("The measure type " + pm.name ()
               + " does not estimate an expectation of a function");
      final MatrixOfTallies<?> mta = getMatrixOfTallies (pm);
      addRatio (mta, mx, my, startingMainPeriod, endingMainPeriod, pm.getZeroOverZeroValue ());
   }

   private static void addRatio (MatrixOfTallies<?> mt, DoubleMatrix2D mx,
         DoubleMatrix2D my, int startingMainPeriod, int endingMainPeriod, double zeroOverZero) {
      if (mt == null || mx == null || my == null)
         return;
      assert mt.rows () == mx.rows ();
      assert mt.columns () == mx.columns ();
      assert mx.rows () == my.rows ();
      assert mx.columns () == my.columns ();
      if (mx.rows () == 0 || mx.columns () == 0)
         return;
      if (startingMainPeriod == 0 && endingMainPeriod + 1 == mt.columns ())
         StatUtil.addRatio (mt, mx, my, 1.0, zeroOverZero);
      else {
         final int nr = mt.rows ();
         for (int mp = startingMainPeriod; mp < endingMainPeriod; mp++)
            for (int r = 0; r < nr; r++) {
               double vx = mx.getQuick (r, mp);
               double vy = my.getQuick (r, mp);
               if (vx == vy && vx == 0)
                  mt.get (r, mp).add (zeroOverZero);
               else
                  mt.get (r, mp).add (vx / vy);
            }
      }
   }

   private void addObs (PerformanceMeasureType pm, DoubleMatrix2D mx,
         DoubleMatrix2D my, int startingMainPeriod, int endingMainPeriod) {
      if (pm.getEstimationType () != EstimationType.FUNCTIONOFEXPECTATIONS)
         throw new IllegalArgumentException ("The measure type " + pm.name ()
               + " does not estimate a function of expectations");
      final MatrixOfFunctionOfMultipleMeansTallies<?> mt = getMatrixOfFunctionOfMultipleMeansTallies (pm);
      addObs (mt, mx, my, startingMainPeriod, endingMainPeriod);
   }

   private static void addObs (MatrixOfFunctionOfMultipleMeansTallies<?> mt,
         DoubleMatrix2D mx, DoubleMatrix2D my, int startingMainPeriod,
         int endingMainPeriod) {
      if (mt == null || mx == null || my == null)
         return;
      assert mt.rows () == mx.rows ();
      assert mt.columns () == mx.columns ();
      assert mx.rows () == my.rows ();
      assert mx.columns () == my.columns ();
      if (mx.rows () == 0 || mx.columns () == 0)
         return;
      if (startingMainPeriod == 0 && endingMainPeriod + 1 == mt.columns ())
         mt.addSameDimension (mx, my);
      else {
         final int nr = mt.rows ();
         for (int mp = startingMainPeriod; mp < endingMainPeriod; mp++)
            for (int r = 0; r < nr; r++)
               mt.get (r, mp).add (mx.getQuick (r, mp), my.getQuick (r, mp));
      }
   }

   /**
    * Adds new observations obtained via measure matrices. This uses
    * {@link CallCenterMeasureManager#getValues} to convert every available matrix of measures
    * into matrices of double-precision values, and adds the resulting matrices
    * to matrices of Tallies.
    */
   public void addObs () {
      addObs (0, ccm.getNumPeriodsForStatProbes () - 1);
   }

   public void initRawStatistics () {
      for (PerformanceMeasureType pm : getPerformanceMeasures ()) {
         if (pm.getEstimationType () == EstimationType.RAWSTATISTIC)
            getMatrixOfStatProbes (pm).init ();
      }
   }

   public void addObsRawStatistics () {
      if (hasPerformanceMeasure (PerformanceMeasureType.BUSYAGENTSENDSIM)) {
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         final int I = cc.getNumAgentGroups ();
         DoubleMatrix2D m = new DenseDoubleMatrix2D (Ip, 1);
         for (int i = 0; i < I; i++) {
            m.setQuick (i, 0, cc.getAgentGroup (i)
                  .getNumBusyAgents ());
            if (I > 1) {
               int nseg = cc.getNumAgentGroupSegments ();
               for (int s = 0; s <= nseg; s++) {
                  if (s < nseg
                        && !cc.getAgentGroupSegment (s)
                              .containsValue (i))
                     continue;
                  m.setQuick (I + s, 0, m.getQuick (I + s, 0)
                        + m.getQuick (i, 0));
               }
            }
         }
         MatrixOfTallies<?> mta = getMatrixOfTallies (PerformanceMeasureType.BUSYAGENTSENDSIM);
         mta.add (m);
      }
      if (hasPerformanceMeasure (PerformanceMeasureType.QUEUESIZEENDSIM)) {
         final int numQueues = cc.getNumWaitingQueues ();
         final int numQueuesP = numQueues > 1 ? numQueues + 1 : numQueues;
         DoubleMatrix2D m = new DenseDoubleMatrix2D (numQueuesP, 1);
         for (int q = 0; q < numQueues; q++) {
            m.setQuick (q, 0, cc.getWaitingQueue (q).size ());
            if (numQueues > 1)
               m.setQuick (numQueues, 0, m.getQuick (numQueues, 0)
                     + m.getQuick (q, 0));
         }
         MatrixOfTallies<?> mta = getMatrixOfTallies (PerformanceMeasureType.QUEUESIZEENDSIM);
         mta.add (m);
      }
      if (hasPerformanceMeasure (PerformanceMeasureType.SERVICELEVELIND01)) {
         PerformanceMeasureType pm = PerformanceMeasureType.SERVICELEVELIND01;
         cache.clear();
         DoubleMatrix2D mx = cache.getMatrix (MeasureType.NUMSERVEDBEFOREAWT, pm
               .getRowType ());
         DoubleMatrix2D  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
         DoubleMatrix2D  mz = cache.getMatrix (MeasureType.NUMABANDONEDBEFOREAWT, pm
               .getRowType ());
         my = my.copy ().assign (mz, Functions.minus);
         
         DoubleMatrix2D m = new DenseDoubleMatrix2D (mx.rows(), mx.columns());
         final int nsl = cc.getNumMatricesOfAWT ();
         final int Ki = cc.getNumInContactTypesWithSegments();
         assert (nsl*Ki == m.rows());
         
         int row;
         double target;
         for (int si = 0; si < nsl; si++) {
            final ServiceLevelParamReadHelper slp = cc.getServiceLevelParams (si);
            for (int k = 0; k < Ki; k++) {
               row = si*Ki+k;
               for (int col = 0; col < m.columns(); col++) {
                  target = slp.getTarget(k, col);
                  if (my.get(row, col) > 0) {
                     double sl = mx.get(row, col) / my.get(row, col);
                     if (sl >= target)
                        m.set(row, col, 1);
                     else
                        m.set(row, col, 0);
                  } else
                     m.set(row, col, 1);
               }
            }
         }

         MatrixOfTallies<?> mta = getMatrixOfTallies (PerformanceMeasureType.SERVICELEVELIND01);
         mta.add (m);
      }
   }

   public void addObs (int startingMainPeriod, int endingMainPeriod) {
      cache.clear ();
      for (final PerformanceMeasureType pm : getPerformanceMeasures ()) {
         if (pm.getEstimationType () != EstimationType.EXPECTATION)
            continue;
         if (pm == PerformanceMeasureType.RATEOFARRIVALSIN
               || pm == PerformanceMeasureType.SERVEDRATES)
            continue;

         final MeasureType[] mt = CallCenterMeasureManager.getMeasureTypesPm (pm);     //Recuperation des tableaux matrice de perf.
         if (mt == null || mt.length == 0)                                             // CallCenterMeasureManager
            continue;
         DoubleMatrix2D mat = cache.getMatrix (mt[0], pm.getRowType ());
         if (mat == null)
            continue;
         switch (pm) {
            case RATEOFOFFERED:
               final DoubleMatrix2D arv = cache.getMatrix (
                     MeasureType.NUMARRIVALS, pm.getRowType ());
               if (arv == null)
                  continue;
               final DoubleMatrix2D blk = cache.getMatrix (MeasureType.NUMBLOCKED,
                     pm.getRowType ());
               if (blk == null)
                  continue;
               final DoubleMatrix2D off = arv.copy ()
                     .assign (blk, Functions.minus);
               addObs (pm, off, startingMainPeriod, endingMainPeriod);
               break;
            case MAXWAITINGTIME:
            case MAXWAITINGTIMEG:
               if (mt.length > 1) {
                  mat = mat.copy ();
                  for (int i = 1; i < mt.length; i++) {
                     final DoubleMatrix2D mat2 = cache.getMatrix (mt[i], pm
                           .getRowType ());
                     if (mat2 == null)
                        continue;
                     mat.assign (mat2, Functions.max);
                  }
               }
               addObs (pm, mat, startingMainPeriod, endingMainPeriod);
               break;
            default:
               if (mt.length > 1) {
                  mat = mat.copy ();
                  for (int i = 1; i < mt.length; i++) {
                     final DoubleMatrix2D mat2 = cache.getMatrix (mt[i], pm
                           .getRowType ());
                     if (mat2 == null)
                        continue;
                     mat.assign (mat2, Functions.plus);
                  }
               }
               addObs (pm, mat, startingMainPeriod, endingMainPeriod);
         }
      }
      for (final PerformanceMeasureType pm : getPerformanceMeasures ()) {
         if (pm.getEstimationType () != EstimationType.EXPECTATIONOFFUNCTION
               && pm.getEstimationType () != EstimationType.FUNCTIONOFEXPECTATIONS)
            continue;
         final MeasureType[] mt = CallCenterMeasureManager.getMeasureTypesPm (pm);
         if (mt == null || mt.length < 2)
            continue;
         DoubleMatrix2D mx;
         DoubleMatrix2D my;
         DoubleMatrix2D mz;
         if (mt.length == 2) {
            mx = cache.getMatrix (mt[0], pm.getRowType ());
            if (mx == null)
               continue;
            my = cache.getMatrix (mt[1], pm.getRowType ());
            if (my == null)
               continue;
         }
         else {
            switch (pm) {
               case SERVICELEVEL:
               case SERVICELEVELREP:
                  mx = cache.getMatrix (MeasureType.NUMSERVEDBEFOREAWT, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.NUMABANDONEDBEFOREAWT, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  my = my.copy ().assign (mz, Functions.minus);
                  break;
               case SERVICELEVELG:
                  mx = cache.getMatrix (MeasureType.NUMSERVEDBEFOREAWT, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMSERVED, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.NUMABANDONEDAFTERAWT, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  my = my.copy ().assign (mz, Functions.plus);
                  mz = cache.getMatrix (MeasureType.NUMBLOCKED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  my = my.assign (mz, Functions.plus);
                  break;
               case SERVICELEVEL2:
               case SERVICELEVEL2REP:
                  mx = cache.getMatrix (MeasureType.NUMSERVEDBEFOREAWT, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.NUMABANDONEDBEFOREAWT, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
               case EXCESSTIME:
               case EXCESSTIMEREP:
                  mx = cache.getMatrix (MeasureType.SUMEXCESSTIMESSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMEXCESSTIMESABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
               case WAITINGTIME:
               case WAITINGTIMEREP:
                  mx = cache.getMatrix (MeasureType.SUMWAITINGTIMESSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMWAITINGTIMESABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
               case MSEWAITINGTIME:                                                            //Ajouter
                  mx = cache.getMatrix (MeasureType.SUMSEWAITINGTIMESSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMSEWAITINGTIMESABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
               case WAITINGTIMEG:
                  mx = cache.getMatrix (MeasureType.SUMWAITINGTIMESSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMWAITINGTIMESABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  my = cache.getMatrix (MeasureType.NUMSERVED, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.NUMABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  my = my.copy ().assign (mz, Functions.plus);
                  mz = cache.getMatrix (MeasureType.NUMBLOCKED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  my = my.assign (mz, Functions.plus);
                  break;
               case WAITINGTIMEVQ:
               case WAITINGTIMEVQREP:
                  mx = cache.getMatrix (MeasureType.SUMWAITINGTIMESVQSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMWAITINGTIMESVQABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
              case MSEWAITINGTIMEVQ:                                                                //Ajouter
                  mx = cache.getMatrix (MeasureType.SUMSEWAITINGTIMESVQSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMARRIVALS, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMSEWAITINGTIMESVQABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;

               case WAITINGTIMEWAIT:
               case WAITINGTIMEWAITREP:
                  mx = cache.getMatrix (MeasureType.SUMWAITINGTIMESSERVED, pm
                        .getRowType ());
                  if (mx == null)
                     continue;
                  my = cache.getMatrix (MeasureType.NUMDELAYED, pm.getRowType ());
                  if (my == null)
                     continue;
                  mz = cache.getMatrix (MeasureType.SUMWAITINGTIMESABANDONED, pm
                        .getRowType ());
                  if (mz == null)
                     continue;
                  mx = mx.copy ().assign (mz, Functions.plus);
                  break;
               default:
                  throw new IllegalArgumentException (
                        "Unsupported performance measure type " + pm.name ());
            }
         }
         // if (mx.rows () != my.rows ())
         // throw new IllegalArgumentException(pm + " " + mt[0] + " " + mt[1]);
         switch (pm.getEstimationType ()) {
            case FUNCTIONOFEXPECTATIONS:
               addObs (pm, mx, my, startingMainPeriod, endingMainPeriod);
               break;
            case EXPECTATIONOFFUNCTION:
               addRatio (pm, mx, my, startingMainPeriod, endingMainPeriod);
               break;
            default:
               throw new AssertionError ();
            }
      }

      final int KI = cc.getNumInContactTypes ();
      // final int sl = sim.getModel ().getCallCenterParams ()
      // .getServiceLevelParams ().size ();

      if (hasPerformanceMeasure (PerformanceMeasureType.RATEOFARRIVALSIN)) {
         final DoubleMatrix2D arrivalsIn = cache.getMatrix (
               MeasureType.NUMARRIVALS, RowType.INBOUNDTYPE);
         final MatrixOfTallies<?> statArrivalsIn = getMatrixOfTallies (PerformanceMeasureType.RATEOFARRIVALSIN);
         if (statArrivalsIn != null && statArrivalsIn.rows () > 1)
            for (int k = KI; k < statArrivalsIn.rows (); k++) {
               for (int mp = startingMainPeriod; mp < endingMainPeriod; mp++)
                  statArrivalsIn.get (KI, mp).add (arrivalsIn.get (KI, mp));
               if (startingMainPeriod == 0
                     && endingMainPeriod + 1 == statArrivalsIn.columns ()) {
                  final int P = statArrivalsIn.columns () - 1;
                  statArrivalsIn.get (KI, P).add (arrivalsIn.get (KI, P));
               }
            }
      }

      if (hasPerformanceMeasure (PerformanceMeasureType.SERVEDRATES)) {
         final MatrixOfTallies<?> servedRates = getMatrixOfTallies (PerformanceMeasureType.SERVEDRATES);
         //final DoubleMatrix2D sr = sim.getValues (MeasureType.SUMSERVED, true);
         final DoubleMatrix2D sr = cache.getMatrix (MeasureType.SUMSERVED, RowType.CONTACTTYPEAGENTGROUP);
         assert allElementsPositive (sr, servedRates.getName (), 0, sr
               .columns ());
         final int Kp = cc.getNumContactTypesWithSegments ();
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         final DoubleMatrix2D srm = new DenseDoubleMatrix2D (Kp, Ip);
         for (int k = 0; k < Kp; k++)
            for (int i = 0; i < Ip; i++) {
               final double v = sr.getQuick (Ip * k + i, 0);
               srm.setQuick (k, i, v);
               // sum += v;
            }
         // if (srm.columns () > 1)
         // srm.setQuick (k, srm.columns () - 1, sum);
         // if (srm.rows () > 1)
         // for (int i = 0; i < srm.columns (); i++) {
         // double sum = 0;
         // for (int k = 0; k < srm.rows () - 1; k++)
         // sum += srm.getQuick (k, i);
         // srm.setQuick (srm.rows () - 1, i, sum);
         // }
         // servedRates.add (ContactCenter.addSumColumn (ContactCenter
         // .addSumRow (srm)));
         servedRates.add (srm);
      }
   }

   private static final class Zero implements DoubleFunction {
      public double apply (double argument) {
         if (argument < 0 && argument > TOL)
            return 0;
         return argument;
      }
   }

   @SuppressWarnings ("unused")
   private static final Zero ZERO = new Zero ();

   private void recomputeTimeAggregates (MatrixOfTallies<?> mta) {
      final int nr = mta.rows ();
      final int np = mta.columns () - 1;
      final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final double time = pce.getPeriodEndingTime (np)
            - pce.getPeriodEndingTime (0);
      for (int r = 0; r < nr; r++) {
         double sum = 0;
         for (int mp = 0; mp < np; mp++) {
            final Tally ta = mta.get (r, mp);
            if (ta.numberObs () == 0)
               continue;
            double v = ta.average ();
            if (normalizeToDefaultUnit)
               v *= pce.getPeriodDuration (mp + 1);
            sum += v;
         }
         if (normalizeToDefaultUnit)
            sum /= time;
         final Tally ta = mta.get (r, np);
         ta.init ();
         ta.add (sum);
      }
   }

   private void recomputeTimeAggregates (
         MatrixOfFunctionOfMultipleMeansTallies<?> mta) {
      final int nr = mta.rows ();
      final int np = mta.columns () - 1;
      final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final int d = mta.getDimension ();
      final double time = pce.getPeriodEndingTime (np)
            - pce.getPeriodEndingTime (0);
      final double[] sums = new double[d];
      for (int r = 0; r < nr; r++) {
         for (int j = 0; j < d; j++)
            sums[j] = 0;
         for (int mp = 0; mp < np; mp++) {
            final FunctionOfMultipleMeansTally fta = mta.get (r, mp);
            assert fta.getDimension () == d;
            for (int j = 0; j < d; j++) {
               final Tally ta = fta.getListOfTallies ().get (j);
               if (ta.numberObs () == 0)
                  continue;
               double v = ta.average ();
               if (normalizeToDefaultUnit)
                  v *= pce.getPeriodDuration (mp + 1);
               sums[j] += v;
            }
         }
         if (normalizeToDefaultUnit)
            for (int j = 0; j < sums.length; j++)
               sums[j] /= time;
         final FunctionOfMultipleMeansTally fta = mta.get (r, np);
         fta.init ();
         fta.add (sums);
      }
   }

   /**
    * Recomputes time-aggregate statistics in a setting where the number of
    * observations in statistical collectors differs from periods to periods.
    * This method processes each matrix of Tallies containing observations for
    * several periods in the following way. Assuming that the processed matrix
    * contains $P+1$ columns, for each row $r$, this method gets the average for
    * each period $p=0,\ldots,P-1$ and adds them up to get the time-aggregate
    * average. Then, the Tally at position $(r, P)$ is reset and the newly
    * computed time-aggregate average is added.
    */
   public void recomputeTimeAggregates () {
      final MatrixOfTallies<?> servedRates = getMatrixOfTallies (PerformanceMeasureType.SERVEDRATES);
      for (final MatrixOfTallies<?> ms : tallyMap.values ()) {
         if (ms == servedRates)
            continue;
         recomputeTimeAggregates (ms);
      }
      for (final MatrixOfFunctionOfMultipleMeansTallies<?> ms : fmmTallyMap
            .values ())
         recomputeTimeAggregates (ms);
   }

   // More expansive checks intended to be used in assertions
   // These checks are bypassed when assertions are disabled (the default).

   private boolean allElementsPositive (DoubleMatrix2D m, String msg,
         int startColumn, int endColumn) {
      boolean ret = true;
      final StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < m.rows (); i++)
         for (int j = startColumn; j < endColumn; j++)
            if (m.get (i, j) < 0) {
               if (!ret)
                  sb.append ('\n');
               sb.append ("Element (").append (i).append (", ").append (j)
                     .append (") of ").append (msg).append (", with value ")
                     .append (m.get (i, j)).append (", is negative");
               ret = false;
            }
      if (!ret)
         logger.severe (sb.toString ());
      return ret;
   }

   @SuppressWarnings ("unused")
   private boolean allElementsSmaller (DoubleMatrix2D a, DoubleMatrix2D b,
         String msga, String msgb, int startColumn, int endColumn) {
      if (a.rows () != b.rows () || a.columns () != b.columns ()) {
         logger.severe ("The dimensions of " + msga + " and " + msgb
               + " are different");
         return false;
      }
      boolean ret = true;
      final StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < a.rows (); i++)
         for (int j = startColumn; j < endColumn; j++)
            if (a.get (i, j) - b.get (i, j) > TOL) {
               if (!ret)
                  sb.append ('\n');
               sb.append ("Element (").append (i).append (", ").append (j)
                     .append (") of ").append (msga).append (", with value ")
                     .append (a.get (i, j)).append (
                           ", is greater than the corresponding element in ")
                     .append (msgb).append (", whose value is ").append (
                           b.get (i, j));
               ret = false;
            }
      if (!ret)
         logger.severe (sb.toString ());
      return ret;
   }

   @SuppressWarnings ("unused")
   private boolean allElementsEqual (DoubleMatrix2D a, DoubleMatrix2D b,
         String msga, String msgb, int startColumn, int endColumn) {
      if (a.rows () != b.rows () || a.columns () != b.columns ()) {
         logger.severe ("The dimensions of " + msga + " and " + msgb
               + " are different");
         return false;
      }
      final StringBuilder sb = new StringBuilder ();
      boolean ret = true;
      for (int i = 0; i < a.rows (); i++)
         for (int j = startColumn; j < endColumn; j++)
            if (Math.abs (a.get (i, j) - b.get (i, j)) > TOL) {
               if (!ret)
                  sb.append ('\n');
               sb.append ("Element (").append (i).append (", ").append (j)
                     .append (") of ").append (msga).append (", with value ")
                     .append (a.get (i, j)).append (
                           ", is different from the corresponding element in ")
                     .append (msgb).append (", whose value is ").append (
                           b.get (i, j));
               ret = false;
            }
      if (!ret)
         logger.severe (sb.toString ());
      return ret;
   }
}
