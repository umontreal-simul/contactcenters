package umontreal.iro.lecuyer.contactcenters.msk.simlogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides some basic methods for implementing the
 * {@link SimLogic} interface.
 * This class encapsulates a boolean variable indicating
 * if the simulation was aborted by some thread as well
 * as a list of observers notified at each simulation step.
 */
public class SimLogicBase {
   private boolean aborted;
   private boolean verbose = false;
   private final List<SimLogicListener> listeners = new ArrayList<SimLogicListener> ();
   private final List<SimLogicListener> umListeners = Collections
         .unmodifiableList (listeners);
   
   public SimLogicBase () {
   }

   public synchronized boolean isAborted () {
      return aborted;
   }

   public synchronized void setAborted (boolean aborted) {
      this.aborted = aborted;
   }
   
   public boolean isVerbose () {
      return verbose;
   }

   public void setVerbose (boolean verbose) {
      this.verbose = verbose;
   }

   public void addSimLogicListener (SimLogicListener l) {
      if (l == null)
         throw new NullPointerException ();
      if (!listeners.contains (l))
         listeners.add (l);
   }

   public void clearSimLogicListeners () {
      listeners.clear ();
   }

   public List<SimLogicListener> getSimLogicListeners () {
      return umListeners;
   }

   public void removeSimLogicListener (SimLogicListener l) {
      listeners.remove (l);
   }
}
