package umontreal.iro.lecuyer.util;

import java.lang.management.MemoryPoolMXBean;

/**
 * Represents a low-memory notification listener
 * that can be registered with a low-memory notifier.
 */
public interface LowMemoryListener {
   /**
    * This method is called when a low-memory
    * condition is detected by the
    * low-memory notifier \texttt{source}
    * for the memory pool \texttt{pool},
    * \texttt{fraction} indicating the
    * number of used bytes over the
    * maximum number of bytes for the pool.
    * @param source the low-memory notifier.
    * @param pool the memory pool which has exceeded its memory usage threshold.
    * @param fraction the number of used bytes over the maximal number of bytes.
    */
   public void lowMemory (LowMemoryNotifier source, MemoryPoolMXBean pool, double fraction);
}
