package umontreal.iro.lecuyer.contactcenters.msk.cv;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.app.ColumnType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Represents the control variable $A$, which is the number of arrivals of
 * inbound contacts. When applied to a performance measure concerning inbound
 * contact type $k$ during period $p$, the used CV is the number of arrived
 * contacts of type $k$ during period $p$. For outbound contact types, the total
 * number of arrived inbound contacts is used.
 */
public class NumArrivalsCV implements ControlVariable {
   private double b = 1;
   private double[] bs;
   private DoubleMatrix2D expArrivals;

   public boolean appliesTo (PerformanceMeasureType pm) {
      switch (pm.getEstimationType ()) {
      case RAWSTATISTIC:
      case EXPECTATIONOFFUNCTION:
         return false;
      }
      if (pm.getColumnType () != ColumnType.MAINPERIOD)
         return false;
//      switch (pm.getRowType ()) {
//      case CONTACTTYPE:
//      case INBOUNDTYPEAWT:
//      case INBOUNDTYPE:
//      case OUTBOUNDTYPE:
//         return true;
//      }
//      return false;
      return true;
   }

   public boolean appliesTo (SimLogic sim, PerformanceMeasureType pm, int row,
         int col) {
      if (!appliesTo (pm))
         return false;
      if (!sim.getCallCenterStatProbes ().hasPerformanceMeasure (PerformanceMeasureType.RATEOFARRIVALSIN))
         return false;
      return true;
   }

   public int numberObs (SimLogic sim, CallCenterStatProbes inStat,
         PerformanceMeasureType pm, int row, int col) {
      final int KI = sim.getCallCenter ().getNumInContactTypes ();
      final int qRow = getRow (pm, row, KI);
      final MatrixOfTallies<?> mta = inStat
            .getMatrixOfTallies (PerformanceMeasureType.RATEOFARRIVALSIN);
      return mta.get (qRow, col).numberObs ();
   }

   public double getObs (SimLogic sim, CallCenterStatProbes inStat,
         PerformanceMeasureType pm, int row, int col, int index) {
      final double x = getNonCenteredObs (sim, inStat, pm, row, col, index);
      final double e = getExpectation (sim, pm, row, col);
      final double bf = bs == null ? b : bs[index];
      return x - e * bf;
   }

   public double getNonCenteredObs (SimLogic sim, CallCenterStatProbes inStat,
         PerformanceMeasureType pm, int row, int col, int index) {
      final MatrixOfTallies<TallyStore> mta = inStat
            .getMatrixOfTallyStores (PerformanceMeasureType.RATEOFARRIVALSIN);
      final int KI = sim.getCallCenter ().getNumInContactTypes ();
      final int qRow = getRow (pm, row, KI);
//      if (qRow == KI) {
//         double sum = 0;
//         for (int r = 0; r < KI; r++)
//            sum += mta.get (r, col).getArray ().get (index);
//         return sum;
//      }
      return mta.get (qRow, col).getArray ()[index];
   }

   private static double getExpectedArrivalRate (SimLogic sim, boolean norm, int k, int p) {
      // p is the index of a main period
      final int KI = sim.getCallCenter ().getNumInContactTypes ();
      if (k == KI) {
         double sum = 0;
         for (int i = 0; i < KI; i++)
            sum += getExpectedArrivalRate (sim, norm, i, p);
         return sum;
      }
      final CallCenter model = sim.getCallCenter ();
      final PeriodChangeEvent pce = model.getPeriodChangeEvent ();
      if (sim.isSteadyState ()) {
         final int cp = sim.getCurrentMainPeriod ();
         final double expArrivals = model.getArrivalProcess (k)
               .getExpectedArrivalRate (cp + 1);
         if (!norm)
            // If results are normalized to default time unit, then
            // we need the expected arrival rate.
            // Otherwise, we need the expected number of
            // arrivals. So we multiply by the period
            // duration in the non-normalized case only.
            return expArrivals * pce
                  .getPeriodDuration (cp + 1);
         return expArrivals;
      }
      else if (p == pce
            .getNumMainPeriods ()) {
         final int P = pce
               .getNumMainPeriods ();
         // Compute the total expected number of arrivals for all periods
         double expArrivals = 0;
         for (int i = 0; i < P; i++) {
            final double d = pce
                  .getPeriodDuration (i + 1);
            final double exp = model.getArrivalProcess (k)
                  .getExpectedArrivalRate (i + 1)
                  * d;
            expArrivals += exp;
         }
         // And normalize to a rate if appropriate
         if (norm)
            return expArrivals / (pce
                  .getPeriodEndingTime (P)
                  - pce
                        .getPeriodEndingTime (0));
         return expArrivals;
      }
      else {
         final double expArrivals = model.getArrivalProcess (k)
               .getExpectedArrivalRate (p + 1);
         if (!norm)
            // If results are normalized to default time unit, then
            // we need the expected arrival rate.
            // Otherwise, we need the expected number of
            // arrivals. So we multiply by the period
            // duration in the non-normalized case only.
            return expArrivals * pce
                  .getPeriodDuration (p + 1);
         return expArrivals;
      }
   }
   
   private static int getRow (PerformanceMeasureType pm, int row, int numInboundTypes) {
      switch (pm.getRowType ()) {
      case INBOUNDTYPEAWT:
         int nit = numInboundTypes;
         if (nit > 1)
            ++nit;
         return row % nit;
      case OUTBOUNDTYPE:
      case AGENTGROUP:
      case WAITINGQUEUE:
         return numInboundTypes > 1 ? numInboundTypes : numInboundTypes - 1;
      }
      if (row > numInboundTypes)
         return numInboundTypes;
      else
         return row;
   }

   public double getExpectation (SimLogic sim, PerformanceMeasureType pm,
         int row, int col) {
      final int KI = sim.getCallCenter ().getNumInContactTypes ();
      final int qRow = getRow (pm, row, KI);
      return expArrivals.get (qRow, col);
   }

   public double[] getBusynessFactors () {
      return bs;
   }

   public void setBusynessFactors (double[] bs) {
      this.bs = bs;
   }
   
   public double getBusynessFactor() {
      return b;
   }
   
   public void setBusynessFactor (double b) {
      this.b = b;
   }
   
   public static DoubleMatrix2D getExpArrivals (SimLogic sim, boolean norm) {
      final CallCenter model = sim.getCallCenter ();
      final int KI = model.getNumInContactTypes ();
      final int P = sim.getCallCenterMeasureManager ().getNumPeriodsForStatProbes ();
      final DoubleMatrix2D expArrivals = new DenseDoubleMatrix2D (KI + 1, P);
      for (int k = 0; k <= KI; k++)
         for (int p = 0; p < P; p++)
            expArrivals.setQuick (k, p, getExpectedArrivalRate (sim, norm, k, p));
      return expArrivals;
   }
   
   public void init (SimLogic sim) {
      expArrivals = getExpArrivals (sim, sim.getSimParams ().isNormalizeToDefaultUnit ());
   }
}
