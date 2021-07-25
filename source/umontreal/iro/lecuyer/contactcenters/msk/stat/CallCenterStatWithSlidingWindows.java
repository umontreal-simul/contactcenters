package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Collection;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Contains the necessary logic for computing statistics
 * in time windows, for a call center model.
 * Some routing or dialing policies might take decisions based
 * on some statistics collected during the last few minutes of
 * operation of the call center.
 * This class provides the necessary tools for collecting
 * such statistics.
 * One first constructs an instance using a call center model, a number
 * of periods, and a period duration.
 * The instance is then registered with the model when
 * statistics are needed, by using {@link #registerListeners()}.
 * The method {@link #getStat()} can then be called at any time
 * to obtain the statistics in the last time periods.
 * Internally, this class uses counters with sliding windows
 * to collect the observations.  
 */
public class CallCenterStatWithSlidingWindows implements Initializable, ToggleElement {
   private CallCenter cc;
   private double periodDuration;
   private int numPeriods;
   private StatPeriod statP;
   private CallCenterMeasureManager cm;
   private SimCallCenterStat stat;
   private boolean statInit = false;
   private boolean started = false;
   private int cp = 0;
   private NewPeriodEvent ev;
   
   /**
    * Constructs a new call center statistical collector with
    * sliding windows, for the call center model \texttt{cc},
    * a window of \texttt{numPeriods} periods of \texttt{periodDuration}
    * time units, and for performance measures of type \texttt{pms}.
    * The boolean \texttt{contactTypeAgentGroup} determines if
    * rows of type (call type, agent group) are needed or not for
    * performance measures concerning call types.
    * @param cc the call center model.
    * @param periodDuration the duration of the statistical periods.
    * @param numPeriods the number of statistical periods.
    * @param contactTypeAgentGroup determines if (call type, agent group)
    * rows are needed.
    * @param pms the types of performance measures for which
    * statistics are needed.
    */
   public CallCenterStatWithSlidingWindows (CallCenter cc, double periodDuration, int numPeriods,
         boolean contactTypeAgentGroup, PerformanceMeasureType... pms) {
      this.cc = cc;
      this.periodDuration = periodDuration;
      this.numPeriods = numPeriods;
      statP = new SWStatPeriod();
      cm = new SWCallCenterMeasures (cc, statP, contactTypeAgentGroup, pms);
      stat = new SimCallCenterStat (cc, cm, false, false, pms);
      ev = new NewPeriodEvent (cc.simulator ());
   }
   
   /**
    * Registers listeners with the call center model in order
    * to collect observations.
    */
   public void registerListeners() {
      cm.registerListeners ();
   }
   
   /**
    * Unregisters the listeners with the call center model.
    * This method may be used when the process using the
    * statistics is stopped, in order to avoid unnecessary collecting
    * of observations.
    */
   public void unregisterListeners() {
      cm.unregisterListeners ();
   }
   
   /**
    * Resets the internal statistical counters.
    * This method should be called at the beginning of the
    * simulation.
    */
   public void init() {
      cm.initMeasureMatrices ();
      statInit = false;
   }
   
   public void start() {
      ev.schedule (periodDuration);
      started = true;
   }
   
   public void stop() {
      ev.cancel ();
      started = false;
   }
   
   public boolean isStarted() {
      return started;
   }
   
   /**
    * Initializes an object containing the statistics
    * in the last periods.
    * The matrices of statistical collectors in the returned
    * object contain a single column corresponding to the
    * statistics.
    */
   public CallCenterStatProbes getStat() {
      if (!statInit) {
         stat.init ();
         for (cp = 0; cp < numPeriods; ++cp)
            stat.addObs ();
         statInit = true;
      }
      return stat;
   }
   
   private class SWStatPeriod implements StatPeriod {
      public int getNumPeriodsForCounters () {
         return numPeriods + 1;
      }

      public int getNumPeriodsForCountersAwt () {
         return numPeriods + 1;
      }

      public int getStatPeriod () {
         double simTime = cc.simulator ().time ();
         return (int) (simTime / periodDuration);
      }

      public int getStatPeriod (Contact contact) {
         return getStatPeriod ();
      }

      public int getStatPeriodAwt (Contact contact) {
         return getStatPeriod ();
      }

      public boolean needsSlidingWindows () {
         return true;
      }

      public boolean needsStatForPeriodSegmentsAwt () {
         return false;
      }

      public int getAwtPeriod (Contact contact) {
         return getGlobalAwtPeriod ();
      }

      public int getGlobalAwtPeriod () {
         return cc.getNumMainPeriodsWithSegments () - 1;
      }
   }
   
   private class SWCallCenterMeasures extends CallCenterMeasureManager {
      public SWCallCenterMeasures (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, Collection<MeasureType> measures) {
         super (cc, statP, contactTypeAgentGroup, measures);
      }

      public SWCallCenterMeasures (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, PerformanceMeasureType[] pms) {
         super (cc, statP, contactTypeAgentGroup, pms);
      }

      public SWCallCenterMeasures (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup) {
         super (cc, statP, contactTypeAgentGroup);
      }

      public int getNumPeriodsForStatProbes () {
         return 1;
      }

      public DoubleMatrix2D getValues (MeasureType mt, boolean norm) {
         final MeasureMatrix mm = getMeasureMatrix (mt);
         final DoubleMatrix2D mat = new DenseDoubleMatrix2D (mm.getNumMeasures (), 1);
         for (int k = 0; k < mat.rows (); k++)
            mat.setQuick (k, 0, mm.getMeasure (k, cp));
         if (norm)
            timeNormalize (mt, mat);
         return mat;
      }

      public void timeNormalize (MeasureType mt, DoubleMatrix2D m) {
         if (mt.getTimeNormalizeType () == TimeNormalizeType.ALWAYS)
            m.assign (Functions.div (periodDuration));
      }
   }
   
   private class NewPeriodEvent extends Event {
      public NewPeriodEvent (Simulator sim) {
         super (sim);
         priority = PeriodChangeEvent.PRIORITY;
      }
      
      @Override
      public void actions () {
         cm.updateCurrentPeriod ();
         statInit = false;
         if (cc.getPeriodChangeEvent ().isWrapupPeriod (cc.getPeriodChangeEvent ().getCurrentPeriod ()))
            return;
         schedule (periodDuration);
      }
   }
}
