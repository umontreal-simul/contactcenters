/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.msk.stat;

import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.SumMatrix;
import umontreal.ssj.stat.mperiods.SumMatrixSW;
import cern.jet.math.Functions;

/**
 * Computes the maximal queue size for every waiting queue and statistical
 * period, during the simulation.
 * An object of this class registers as a listener for
 * every waiting queue of the model.
 * Each time a contact enters a queue, the object
 * checks that
 * the queue size is not greater than the current maximum,
 * and updates the maximum if necessary.
 * When the model is simulated over multiple periods,
 * such maxima are computed for each period.
 * A queue-size checker is also a period-change listener, because
 * at the beginning of periods, it needs to set
 * the per-period initial maxima to the current queue size.
 */
public final class QueueSizeChecker implements WaitingQueueListener,
      MeasureMatrix {
   private CallCenter cc;
   private StatPeriod statP;
   private SumMatrix maxSizes;
   private int total;

   /**
    * Constructs a new queue-size checker using
    * call center \texttt{cc}, and object
    * \texttt{statP} to obtain statistical periods.
    */
   public QueueSizeChecker (CallCenter cc, StatPeriod statP) {
      this.cc = cc;
      this.statP = statP;
      final int Q = cc.getNumWaitingQueues ();
      final int np = statP.getNumPeriodsForCounters ();
      if (statP.needsSlidingWindows ())
         maxSizes = new SumMatrixSW (Q > 1 ? Q + 1 : Q, np);
      else
         maxSizes = new SumMatrix (Q > 1 ? Q + 1 : Q, np);
   }

   /**
    * Resets the values of maxima to 0.
    */
   public void init () {
      maxSizes.init ();
      initForCurrentPeriod ();
   }

   public void initForCurrentPeriod () {
      final int nq = cc.getNumWaitingQueues ();
      final int cp = statP.getStatPeriod ();
      if (cp < 0)
         return;
      total = 0;
      for (int q = 0; q < nq; q++) {
         final int size = cc.getWaitingQueue (q).size ();
         maxSizes.add (q, cp, size, Functions.max);
         total += size;
      }
      if (nq > 1)
         adjustMax (cp);
   }

   private int getQueueSize () {
      final int nq = cc.getNumWaitingQueues ();
      int size = 0;
      for (int q = 0; q < nq; q++)
         size += cc.getWaitingQueue (q).size ();
      return size;
   }

   /**
    * Registers this queue-size checker with the associated
    * call center model.
    * The method adds this object to the list of observers
    * for all waiting queues of the model, and
    * registers itself as a period-change listener.
    */
   public void register () {
      for (final WaitingQueue queue : cc
            .getWaitingQueues ())
         queue.addWaitingQueueListener (this);
   }

   /**
    * Unregisters this queue-size checker with the associated
    * model.
    * This method performs the reverse task of
    * {@link #register()}.
    */
   public void unregister () {
      for (final WaitingQueue queue : cc
            .getWaitingQueues ())
         queue.removeWaitingQueueListener (this);
   }

   public void init (WaitingQueue queue) {
      total = getQueueSize ();
      checkSize (queue);
   }

   public void enqueued (DequeueEvent ev) {
      if (ev.getScheduledQueueTime () > 0)
         ++total;
      checkSize (ev.getWaitingQueue ());
   }

   public void dequeued (DequeueEvent ev) {
      if (ev.getScheduledQueueTime () > 0)
         --total;
      // checkSize (ev.getWaitingQueue ());
   }

   private void checkSize (WaitingQueue queue) {
      final int cp = statP.getStatPeriod ();
      if (cp < 0)
         return;
      final int size = queue.size ();
      final int qid = queue.getId ();
      maxSizes.add (qid, cp, size, Functions.max);
      if (cc.getNumWaitingQueues () > 1)
         adjustMax (cp);
   }

   private void adjustMax (int cp) {
      assert total == getQueueSize ();
      maxSizes.add (maxSizes.getNumMeasures () - 1, cp, total, Functions.max);
   }

   public double getMeasure (int i, int p) {
      return maxSizes.getMeasure (i, p);
   }

   public int getNumMeasures () {
      return maxSizes.getNumMeasures ();
   }

   public int getNumPeriods () {
      return maxSizes.getNumPeriods ();
   }

   public void regroupPeriods (int x) {
      throw new UnsupportedOperationException ();
   }

   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ();
   }

   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ();
   }
}
