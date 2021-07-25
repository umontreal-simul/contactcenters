package umontreal.iro.lecuyer.contactcenters.queue;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureSet;

/**
 * Queue size statistical collector implementing
 * {@link MeasureMatrix}.
 * This class extends {@link QueueSizeStat}
 * and implements the {@link MeasureMatrix} interface and defines
 * measures for queue sizes. If the object supports $K>1$ contact types, the
 * measure $0\le k<K$ corresponds to the integral of the number of contacts of
 * type~$k$ over the simulation time. The measure~$K$ corresponds to the
 * integral of the queue size over the simulation time. If $K=1$, only the
 * integral of the queue size is computed and stored in measure~0. Since this
 * measure matrix supports only one period, it must be combined with
 * {@link umontreal.ssj.stat.mperiods.IntegralMeasureMatrix} for the
 * integral of the queue size to be obtained for each period.
 */
public class QueueSizeStatMeasureMatrix extends QueueSizeStat implements MeasureMatrix {
   /**
    * Constructs a new queue size statistical probe for the waiting queue
    * \texttt{queue} and only computing aggregate queue size. This is equivalent
    * to {@link #QueueSizeStatMeasureMatrix(WaitingQueue,int) Queue\-Size\-Stat}
    * \texttt{(queue, 0)}.
    * 
    * @param queue
    *           the observed waiting queue.
    */
   public QueueSizeStatMeasureMatrix (WaitingQueue queue) {
      super (queue);
   }
   
   /**
    * Equivalent to {@link #QueueSizeStatMeasureMatrix(WaitingQueue)},
    * using the given simulator \texttt{sim}
    * to create internal probes.
    */
   public QueueSizeStatMeasureMatrix (Simulator sim, WaitingQueue queue) {
      super (sim, queue);
   }

   /**
    * Constructs a new queue size statistical probe for the waiting queue
    * \texttt{queue} supporting \texttt{numTypes} contact types.
    * 
    * @param queue
    *           the observed waiting queue.
    * @param numTypes
    *           the supported number of contact types.
    * @exception IllegalArgumentException
    *               if the number of contact types is smaller than 0.
    */
   public QueueSizeStatMeasureMatrix (WaitingQueue queue, int numTypes) {
      super (queue, numTypes);
   }
   
   /**
    * Equivalent to {@link #QueueSizeStatMeasureMatrix(WaitingQueue,int)},
    * using the given simulator \texttt{sim} to create
    * internal probes.
    */
   public QueueSizeStatMeasureMatrix (Simulator sim, WaitingQueue queue, int numTypes) {
      super (sim, queue, numTypes);
   }

   public int getNumMeasures () {
      return getNumContactTypes() + 1;
   }

   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException (
      "Cannot change the number of measures");
   }

   public int getNumPeriods () {
      return 1;
   }

   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException (
      "Cannot change the number of periods");
   }

   public double getMeasure (int i, int p) {
      if (p != 0)
         throw new ArrayIndexOutOfBoundsException ("Invalid period index:" + p);
      if (i == getNumContactTypes())
         return getStatQueueSize().sum ();
      else
         return getStatQueueSize (i).sum ();
   }

   public void regroupPeriods (int x) {}

   /**
    * Returns a measure set regrouping the queue size integrals for several
    * waiting queues. Row \texttt{r} of the resulting matrix corresponds to the
    * queue size integral stored in \texttt{qscalc[r]}, and the last row
    * contains the total queue size.
    * 
    * @param qscalc
    *           the queue size matrices.
    * @return the queue size integral measure set.
    */
   public static MeasureSet getQueueSizeIntegralMeasureSet (
         MeasureMatrix[] qscalc) {
      final MeasureSet mset = new MeasureSet ();
      for (final MeasureMatrix mmat : qscalc)
         mset.addMeasure (mmat, mmat.getNumMeasures () - 1);
      return mset;
   }

   /**
    * Returns a measure set regrouping the integrals of the number of contacts
    * of each type in a set of waiting queues. The row \texttt{numTypes*q + k}
    * contains the integral of the number of contact of type \texttt{k} stored
    * in \texttt{qscalc[q]} over the simulation time. If the measure set is
    * computing the sum row (the default), row \texttt{numTypes*qscalc.length +
    * k} corresponds to the integral of the total number of queued contacts of
    * type \texttt{k}, over the simulation time.
    * 
    * @param qscalc
    *           the queue size integral matrices.
    * @param numTypes
    *           the number of contact types.
    * @return the queue size integral measure set.
    */
   public static MeasureSet getQueueSizeIntegralMeasureSet (
         MeasureMatrix[] qscalc, int numTypes) {
      final MeasureSet mset = numTypes > 1 ? new QCalc (numTypes)
            : new MeasureSet ();
      for (int q = 0; q < qscalc.length; q++) {
         int nt = qscalc[q].getNumMeasures ();
         if (nt > 1)
            nt--;
         if (nt < numTypes)
            throw new IllegalArgumentException (
                  "Not enough measures in qscalc[" + q + "]");
         for (int k = 0; k < numTypes; k++)
            mset.addMeasure (qscalc[q], k);
      }
      return mset;
   }

   private static class QCalc extends MeasureSet {
      private int numTypes;

      QCalc (int numTypes) {
         this.numTypes = numTypes;
      }

      @Override
      public int getNumMeasures () {
         int nm = super.getNumMeasures ();
         if (isComputingSumRow ()) {
            nm--;
            // int nq = nm / numTypes;
            nm += numTypes;
         }
         return nm;
      }

      @Override
      public double getMeasure (int i, int p) {
         if (isComputingSumRow ()) {
            final int nm = getNumMeasures ();
            final int nq = nm / numTypes;
            final int q = i / numTypes;
            final int t = i % numTypes;
            if (q == nq - 1) {
               double value = 0;
               for (int j = 0; j < nq - 1; j++)
                  value += super.getMeasure (numTypes * j + t, p);
               return value;
            }
            else
               return super.getMeasure (i, p);
         }
         else
            return super.getMeasure (i, p);
      }
   }
}
