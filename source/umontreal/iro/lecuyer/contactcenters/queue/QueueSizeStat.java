package umontreal.iro.lecuyer.contactcenters.queue;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Simulator;

/**
 * Computes statistics for a specific waiting queue. Using accumulates, this
 * class can compute the integral of the queue size from the last call to
 * {@link #init} to the current simulation time. Optionally, it can also
 * compute the integral of the number of contacts of each type $k$ in queue.
 */
public class QueueSizeStat implements Cloneable {
   private Accumulate[] sizes;
   private Accumulate size;
   private WaitingQueue queue;
   private SizeListener sl = new SizeListener ();

   /**
    * Constructs a new queue size statistical probe for the waiting queue
    * \texttt{queue} and only computing aggregate queue size. This is equivalent
    * to {@link #QueueSizeStat(WaitingQueue,int) Queue\-Size\-Stat}
    * \texttt{(queue, 0)}.
    *
    * @param queue
    *           the observed waiting queue.
    */
   public QueueSizeStat (WaitingQueue queue) {
      this (Simulator.getDefaultSimulator(), queue, 0);
   }

   /**
    * Equivalent to {@link #QueueSizeStat(WaitingQueue)},
    * using the given simulator \texttt{sim} to
    * construct accumulates.
    */
   public QueueSizeStat (Simulator sim, WaitingQueue queue) {
      this (sim, queue, 0);
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
   public QueueSizeStat (WaitingQueue queue, int numTypes) {
      this (Simulator.getDefaultSimulator(), queue, numTypes);
   }

   /**
    * Equivalent ot {@link #QueueSizeStat(WaitingQueue,int)},
    * using the simulator \texttt{sim} to construct
    * accumulates.
    */
   public QueueSizeStat (Simulator sim, WaitingQueue queue, int numTypes) {
      if (numTypes < 0)
         throw new IllegalArgumentException ("numTypes < 0");
      size = new Accumulate (sim, "Total queue size");
      sizes = new Accumulate[numTypes];
      for (int i = 0; i < sizes.length; i++)
         sizes[i] = new Accumulate (sim);
      setWaitingQueue (queue);
   }

   /**
    * Sets the simulator attached to internal accumulates
    * to \texttt{sim}.
    * @param sim the new simulator.
    * @exception NullPointerException if \texttt{sim} is \texttt{null}.
    */
   public void setSimulator (Simulator sim) {
      for (final Accumulate element : sizes)
         element.setSimulator (sim);
   }

   private String getProbeName (int i) {
      String qn;
      if (queue == null)
         qn = "";
      else
         qn = " (" + ContactCenter.toShortString (queue) + ")";
      return i < sizes.length ? "Number of contacts of type " + i + qn
            : "Number of contacts";
   }

   /**
    * Returns the waiting queue currently associated with this object.
    *
    * @return the currently associated waiting queue.
    */
   public final WaitingQueue getWaitingQueue () {
      return queue;
   }

   /**
    * Sets the associated waiting queue to \texttt{queue}. If the given queue is
    * \texttt{null}, the statistical collector is disabled until a
    * non-\texttt{null} waiting queue is given. This can be used during a
    * replication if the integrals must be computed during some periods only.
    *
    * @param queue
    *           the new associated waiting queue.
    */
   public final void setWaitingQueue (WaitingQueue queue) {
      if (queue == this.queue)
         return;
      if (this.queue != null)
         this.queue.removeWaitingQueueListener (sl);
      this.queue = queue;
      if (queue != null)
         queue.addWaitingQueueListener (sl);
      for (int i = 0; i < sizes.length; i++)
         sizes[i].setName (getProbeName (i));
      size.setName (getProbeName (sizes.length));
      updateValues ();
   }

   /**
    * Returns the statistical collector for the queue size over the simulation
    * time.
    *
    * @return the queue size statistical collector.
    */
   public Accumulate getStatQueueSize () {
      return size;
   }

   /**
    * Returns the number of contact types supported by this
    * object.
    * @return the number of supported contact types.
    */
   public int getNumContactTypes() {
      return sizes.length;
   }

   /**
    * Returns the statistical collector for the number of contacts of type
    * \texttt{type} in the queue.
    *
    * @param type
    *           the target contact type.
    * @return the size collector for the target type.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{type} is negative or greater than or equal to the
    *               number of supported contact types.
    */
   public Accumulate getStatQueueSize (int type) {
      return sizes[type];
   }

   private void updateValues () {
      if (queue == null) {
         for (final Accumulate stat : sizes)
            stat.update (0);
         size.update (0);
         return;
      }

      size.update (queue.size ());
      if (sizes.length > 0)
         if (queue.isEmpty())
            for (final Accumulate element : sizes)
               element.update (0);
         else
            //            final int[] nq = new int[sizes.length];
//            for (final DequeueEvent ev : queue) {
//               final int tid = ev.getContact ().getTypeId ();
//               if (tid < nq.length)
//                  ++nq[tid];
//            }
//            for (int i = 0; i < nq.length; i++)
//               sizes[i].update (nq[i]);
            for (int i = 0; i < sizes.length; i++)
               sizes[i].update (queue.size (i));
   }

   public void init () {
      for (final Accumulate stat : sizes)
         stat.init ();
      size.init ();
      updateValues ();
   }

   private final class SizeListener implements WaitingQueueListener {
      public void enqueued (DequeueEvent ev) {
         final Contact contact = ev.getContact ();
         final WaitingQueue queue1 = ev.getWaitingQueue ();
         final int type = contact.getTypeId ();
         if (sizes.length > 0 && type >= 0 && type < sizes.length)
            sizes[type].update (sizes[type].getLastValue () + 1);
         size.update (queue1.size ());
      }

      public void dequeued (DequeueEvent ev) {
         final Contact contact = ev.getContact ();
         final WaitingQueue queue1 = ev.getWaitingQueue ();
         final int type = contact.getTypeId ();
         if (sizes.length > 0 && type >= 0 && type < sizes.length)
            sizes[type].update (sizes[type].getLastValue () - 1);
         size.update (queue1.size ());
      }

      public void init (WaitingQueue queue1) {
         for (final Accumulate stat : sizes)
            stat.update (0);
         size.update (0);
      }

      @Override
      public String toString () {
         final StringBuilder sb = new StringBuilder (getClass ()
               .getSimpleName ());
         sb.append ('[');
         sb.append ("associated waiting queue: ").append (
               ContactCenter.toShortString (queue));
         sb.append (']');
         return sb.toString ();
      }
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append ("associated waiting queue: ").append (
            ContactCenter.toShortString (queue));
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Constructs and returns a clone of this queue-size collector. This method
    * clones the internal statistical collectors, but the clone has no
    * associated waiting queue. This can be used to save the state of the
    * statistical collector for future restoration.
    *
    * @return a clone of this object.
    */
   @Override
   public QueueSizeStat clone () {
      QueueSizeStat cpy;
      try {
         cpy = (QueueSizeStat) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      cpy.sizes = sizes.clone ();
      for (int i = 0; i < sizes.length; i++)
         cpy.sizes[i] = sizes[i].clone ();
      cpy.size = size.clone ();
      cpy.sl = cpy.new SizeListener ();
      cpy.queue = null;
      cpy.updateValues ();
      return cpy;
   }
}
