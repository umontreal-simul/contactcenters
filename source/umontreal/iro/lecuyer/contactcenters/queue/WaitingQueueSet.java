package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a group of waiting queues for which it is possible to get the
 * total size. This can be used when the total number of contacts in a subset of
 * the contact center's waiting queues is needed for statistical collecting or
 * for capacity limitation.
 */
public class WaitingQueueSet extends AbstractSet<WaitingQueue> implements
      Initializable, Named, Cloneable {
   private String name = "";
   private Set<WaitingQueue> queues = new LinkedHashSet<WaitingQueue> ();
   private SizeChecker sc = new SizeChecker ();

   private boolean collect = false;
   private Accumulate statSize;

   public String getName () {
      return name;
   }

   public void setName (String n) {
      if (n == null)
         throw new NullPointerException ("The given name must not be null");
      name = n;
      if (statSize != null)
         statSize.setName (getProbeName ());
   }

   /**
    * Returns the total size of the queues currently in this group of waiting
    * queues.
    *
    * @return the size of all contained queues.
    */
   public int queueSize () {
      int size = 0;
      for (final WaitingQueue queue : queues)
         size += queue.size ();
      return size;
   }

   /**
    * Adds the waiting queue \texttt{queue} to this set of waiting queues.
    *
    * @param queue
    *           the waiting queue being added.
    * @exception NullPointerException
    *               if \texttt{queue} is \texttt{null}.
    */
   @Override
   public boolean add (WaitingQueue queue) {
      if (queue == null)
         throw new NullPointerException ();
      final boolean added = queues.add (queue);
      if (added) {
         queue.addWaitingQueueListener (sc);
         if (collect)
            statSize.update (queueSize ());
         return true;
      }
      return false;
   }

   @Override
   public int size () {
      return queues.size ();
   }

   @Override
   public boolean isEmpty () {
      return queues.isEmpty ();
   }

   @Override
   public boolean contains (Object o) {
      return queues.contains (o);
   }

   @Override
   public Iterator<WaitingQueue> iterator () {
      return new MyIterator (queues.iterator ());
   }

   /**
    * Removes the waiting queue \texttt{queue} from this set of waiting queues.
    *
    * @param queue
    *           the waiting queue being removed.
    * @exception NullPointerException
    *               if \texttt{queue} is \texttt{null}.
    */
   @Override
   public boolean remove (Object queue) {
      final boolean removed = queues.remove (queue);
      if (removed) {
         ((WaitingQueue) queue).removeWaitingQueueListener (sc);
         if (collect)
            statSize.update (queueSize ());
         return true;
      }
      return false;
   }

   /**
    * Removes all the waiting queues contained in this set of waiting queues.
    */
   @Override
   public void clear () {
      for (final WaitingQueue queue : queues)
         queue.removeWaitingQueueListener (sc);
      queues.clear ();
      if (collect)
         statSize.update (0);
   }

   /**
    * Initializes all the waiting queues contained in this set.
    */
   public void init () {
      for (final WaitingQueue queue : queues)
         queue.init ();
      if (collect)
         initStat ();
   }

   /**
    * Initializes the statistical collector for the size of the queues in this
    * set. If statistical collecting is turned OFF, this throws an
    * {@link IllegalStateException}.
    *
    * @exception IllegalStateException
    *               if statistical collecting is turned OFF.
    */
   public void initStat () {
      if (!collect)
         throw new IllegalStateException ("Statistical collecting is disabled");
      statSize.init (queueSize ());
   }

   /**
    * Determines if this set of waiting queues is collecting statistics about
    * the total size of the queues. If this returns \texttt{true}, statistical
    * collecting is turned ON. Otherwise (the default), it is turned OFF.
    *
    * @return the state of statistical collecting.
    */
   public boolean isStatCollecting () {
      return collect;
   }

   /**
    * Sets the state of statistical collecting to \texttt{b}. If \texttt{b} is
    * \texttt{true}, statistical collecting is turned ON. The statistical
    * collectors are created or reinitialized. If \texttt{b} is \texttt{false},
    * statistical collecting is turned OFF.
    *
    * @param b
    *           the new state of statistical collecting.
    */
   public void setStatCollecting (boolean b) {
      if (b)
         setStatCollecting (Simulator.getDefaultSimulator ());
      else
         collect = false;
   }

   /**
    * Enables statistical collecting, but associates
    * the given simulator to the internal
    * accumulate.
    * @param sim the simulator associated to
    * the internal accumulate.
    */
   public void setStatCollecting (Simulator sim) {
      if (sim == null)
         throw new NullPointerException();
      collect = true;
      if (statSize == null)
         statSize = new Accumulate (sim, getProbeName ());
      else
         statSize.setSimulator (sim);
      initStat ();
   }

   /**
    * Returns the statistical collector for the size of the queues in the set.
    * This returns a non-\texttt{null} value only if statistical collecting was
    * turned ON since this object was constructed.
    *
    * @return the queue size statistical collector.
    */
   public Accumulate getStatQueueSize () {
      return statSize;
   }

   private final class SizeChecker implements WaitingQueueListener {
      public void dequeued (DequeueEvent ev) {
         assert queues.contains (ev.getWaitingQueue ()) : "The waiting queue "
               + ev.getWaitingQueue ().toString ()
               + " is not a member of the set "
               + WaitingQueueSet.this.toString ();
         if (collect)
            statSize.update (queueSize ());
      }

      public void enqueued (DequeueEvent ev) {
         assert queues.contains (ev.getWaitingQueue ()) : "The waiting queue "
               + ev.getWaitingQueue ().toString ()
               + " is not a member of the set "
               + WaitingQueueSet.this.toString ();
         if (collect)
            statSize.update (queueSize ());
      }

      public void init (WaitingQueue queue) {
         assert queues.contains (queue) : "The waiting queue "
               + queue.toString () + " is not a member of the set "
               + WaitingQueueSet.this.toString ();
         if (collect)
            statSize.update (queueSize ());
      }

      @Override
      public String toString () {
         final StringBuilder sb = new StringBuilder (getClass ()
               .getSimpleName ());
         sb.append ('[');
         sb.append ("associated set of waiting queues: ").append (
               ContactCenter.toShortString (WaitingQueueSet.this));
         sb.append (']');
         return sb.toString ();
      }
   }

   private String getProbeName () {
      final String n = getName ();
      if (n.length () > 0)
         return "Size of queues (" + n + ")";
      else
         return "Size of queues";
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      if (getName ().length () > 0)
         sb.append ("name: ").append (getName ()).append (", ");
      sb.append ("waiting queues in the set: ").append (queues.size ());
      if (queues.size () > 0)
         sb.append (", total size of the queues: ").append (queueSize ());
      sb.append (", statistical collecting ");
      if (collect)
         sb.append ("ON");
      else
         sb.append ("OFF");
      sb.append (']');
      return sb.toString ();
   }

   /**
    * Constructs and returns a copy of this set of waiting queues. This method
    * clones the internal set of waiting queues as well as the statistical
    * collectors if they exist. This does not clone the waiting queues
    * themselves.
    *
    * @return a clone of this object.
    */
   @Override
   public WaitingQueueSet clone () {
      WaitingQueueSet cpy;
      try {
         cpy = (WaitingQueueSet) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      cpy.queues = new LinkedHashSet<WaitingQueue> (queues);
      cpy.statSize = statSize.clone ();
      cpy.sc = cpy.new SizeChecker ();
      for (final WaitingQueue queue : cpy.queues)
         queue.addWaitingQueueListener (cpy.sc);
      return cpy;
   }

   private class MyIterator implements Iterator<WaitingQueue> {
      private Iterator<WaitingQueue> itr;
      private WaitingQueue lastRet;

      public MyIterator (Iterator<WaitingQueue> itr) {
         this.itr = itr;
      }

      public boolean hasNext () {
         return itr.hasNext ();
      }

      public WaitingQueue next () {
         lastRet = itr.next ();
         return lastRet;
      }

      public void remove () {
         itr.remove ();
         lastRet.removeWaitingQueueListener (sc);
         if (collect)
            statSize.update (queueSize ());
         lastRet = null;
      }
   }
}
