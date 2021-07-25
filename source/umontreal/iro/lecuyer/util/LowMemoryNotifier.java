package umontreal.iro.lecuyer.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

/**
 * Uses the Java 5 Management API to monitor memory
 * usage, and notifies registered listeners when
 * memory becomes low.
 * Memory is partitionned into \emph{memory pools}
 * for the Management API.
 * Each pool can have a usage threshold which
 * triggers a notification when exceeded.
 * 
 * When constructing a low-memory notifier,
 * one specifies a collection of memory
 * pools to monitor.
 * For each monitored pool supporting
 * the usage threshold, the threshold is
 * set to a fraction of the maximum memory
 * available for that pool, the
 * default fraction is 0.75.
 * When memory usage exceeds this fraction,
 * a notification is emitted by the
 * memory pool, and this class notifies
 * the registered low-memory listeners.
 * If the maximum memory available for
 * the pool has increased between
 * the time the object was created and
 * the low-memory notification,
 * the usage fraction becomes smaller
 * than the threshold usage fraction,
 * and no listener is notified.
 * The usage threshold of the concerned
 * pool is then updated.  
 */
public class LowMemoryNotifier {
   private MemoryMXBean memory;   
   private final Map<String, MemoryPoolMXBean> memoryPools = new HashMap<String, MemoryPoolMXBean>();
   private double usageThreshold = 0.75;
   private final List<LowMemoryListener> listeners = new ArrayList<LowMemoryListener>();
   private final MemoryNotificationListener listener = new MemoryNotificationListener();
   
   /**
    * Constructs a low-memory notifier with
    * usage threshold set to 0.75 and
    * monitoring every memory pool
    * supporting usage threshold.
    */
   public LowMemoryNotifier() {
      this (ManagementFactory.getMemoryPoolMXBeans (), 0.75);
   }
   
   /**
    * Constructs a low-memory notifier with
    * usage threshold set to \texttt{usageThreshold} and
    * monitoring every memory pool
    * supporting usage threshold.
    * 
    * @param usageThreshold the usage threshold.
    * @exception IllegalArgumentException if \texttt{usageThreshold}
    * is smaller than 0 or greater than 1.
    */
   public LowMemoryNotifier (double usageThreshold) {
      this (ManagementFactory.getMemoryPoolMXBeans (), usageThreshold);
   }
   
   /**
    * Constructs a low-memory notifier with
    * usage threshold set to 0.75, and
    * monitoring every memory pool
    * in \texttt{pools} supporting
    * usage threshold.
    * @param pools the collection of memory pools.
    * @exception NullPointerException if \texttt{pools} is \texttt{null}.
    */
   public LowMemoryNotifier (Collection<MemoryPoolMXBean> pools) {
      this (pools, 0.75);
   }   
   
   /**
    * Constructs a low-memory notifier with
    * usage threshold set to \texttt{usageThreshold}, and
    * monitoring every memory pool
    * in \texttt{pools} supporting
    * usage threshold.
    * 
    * @param pools the collection of memory pools.
    * @param usageThreshold the usage threshold.
    * @exception NullPointerException if \texttt{pools} is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{usageThreshold}
    * is smaller than 0 or greater than 1.
    */
   public LowMemoryNotifier (Collection<MemoryPoolMXBean> pools, double usageThreshold) {
      if (usageThreshold < 0 || usageThreshold > 1)
         throw new IllegalArgumentException
         ("Invalid usage threshold " + usageThreshold);
      this.usageThreshold = usageThreshold;
      for (final MemoryPoolMXBean pool : pools)
         if (pool.isUsageThresholdSupported () &&
               pool.getType () == MemoryType.HEAP)
            memoryPools.put (pool.getName (), pool);
      updateUsageThresholds();
      
      memory = ManagementFactory.getMemoryMXBean ();
   }
   
   private void register() {
      final NotificationEmitter emitter = (NotificationEmitter)memory;
      emitter.addNotificationListener (listener, null, null);
   }
   
   private void unregister() {
      final NotificationEmitter emitter = (NotificationEmitter)memory;
      try {
         emitter.removeNotificationListener (listener);
      }
      catch (final ListenerNotFoundException lnfe) {}
   }
   
   /**
    * Registers the low-memory listener
    * \texttt{listener}.
    * @param listener1 the new low-memory listener to be registered.
    * @exception NullPointerException if \texttt{listener} is \texttt{null}.    */
   public void addLowMemoryListener (LowMemoryListener listener1) {
      if (listener1 == null)
         throw new NullPointerException();
      if (listeners.isEmpty ())
         register();
      if (!listeners.contains (listener1))
         listeners.add (listener1);
   }
   
   /**
    * Unregisters the low-memory listener \texttt{listener}.
    * @param listener1 the low-memory listener to be unregistered.
    */
   public void removeLowMemoryListener (LowMemoryListener listener1) {
      final boolean modified = listeners.remove (listener1);
      if (modified && listeners.isEmpty ())
         unregister();
   }
   
   /**
    * Removes all low-memory listeners registered
    * with this object.
    */
   public void removeLowMemoryListeners() {
      final boolean modified = !listeners.isEmpty ();
      listeners.clear ();
      if (modified)
         unregister();
   }
   
   /**
    * Returns the low-memory listeners currently
    * registered with this object.
    * @return the list of currently-registered low-memory listeners.
    */
   public List<LowMemoryListener> getLowMemoryListeners() {
      return Collections.unmodifiableList (listeners);
   }
   
   /**
    * Returns the usage threshold used by this
    * low-memory notifier.
    * @return the current usage threshold.
    */
   public double getUsageThreshold() {
      return usageThreshold;
   }
   
   /**
    * Sets the usage threshold used by this
    * low-memory notifier to \texttt{usageThreshold}
    * @param usageThreshold the current usage threshold.
    * @exception IllegalArgumentException if \texttt{usageThreshold}
    * is smaller than 0 or greater than 1.
    */
   public void setUsageThreshold (double usageThreshold) {
      if (usageThreshold < 0 || usageThreshold > 1)
         throw new IllegalArgumentException
         ("Invalid usage threshold " + usageThreshold);
      this.usageThreshold = usageThreshold;
      updateUsageThresholds();
   }
   
   /**
    * Returns the memory pools monitored by this
    * object.
    * @return the monitored memory pools.
    */
   public Collection<MemoryPoolMXBean> getMemoryPools() {
      return Collections.unmodifiableCollection (memoryPools.values ());
   }
   
   private void updateUsageThresholds() {
      for (final MemoryPoolMXBean pool : memoryPools.values ())
         updateUsageThreshold (pool);
   }
   
   private void updateUsageThreshold (MemoryPoolMXBean pool) {
      if (!pool.isUsageThresholdSupported ())
         return;
      final MemoryUsage usage = pool.getUsage ();
      final long absUsageThreshold = (long)(usage.getMax ()*usageThreshold);
      pool.setUsageThreshold (absUsageThreshold);
   }
   
   private class MemoryNotificationListener implements NotificationListener {
      public void handleNotification (Notification notification, Object handback) {
         final String type = notification.getType ();
         if (!type.equals (MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED))
            return;
         final CompositeData cd = (CompositeData)notification.getUserData ();
         final MemoryNotificationInfo info = MemoryNotificationInfo.from (cd);
         final MemoryPoolMXBean pool = memoryPools.get (info.getPoolName ());
         if (pool == null)
            return;
         final MemoryUsage usage = info.getUsage ();
         final double fraction = (double)usage.getUsed () / usage.getMax ();
         if (fraction < usageThreshold)
            updateUsageThreshold (pool);
         else
            for (final LowMemoryListener l : listeners)
               l.lowMemory (LowMemoryNotifier.this, pool, fraction);
      }
   }
}
