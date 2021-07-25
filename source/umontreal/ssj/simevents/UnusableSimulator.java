package umontreal.ssj.simevents;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simevents.eventlist.EventList;

/**
 * Simulator for which all methods throw an
 * {@link UnsupportedOperationException}.
 * By setting {@link Simulator#defaultSimulator}
 * to an instance of this class, one can
 * detect unexpected usage of the static
 * {@link Sim} class.
 * This can be useful for  
 * adapting a program for parallel simulations, because
 * such a program must
 * use an instance of {@link Simulator}
 * for each parallel replication rather than
 * the static class.
 * An unexpected use of {@link Sim} may lead
 * to unpredictable results in such cases.
 */
public class UnusableSimulator extends Simulator {
   @Override
   public EventList getEventList () {
      throw new UnsupportedOperationException();
   }

   @Override
   public void init () {
      throw new UnsupportedOperationException();
   }

   @Override
   public void init (EventList evlist) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isSimulating () {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isStopped () {
      throw new UnsupportedOperationException();
   }

   @Override
   public void start () {
      throw new UnsupportedOperationException();
   }

   @Override
   public void stop () {
      throw new UnsupportedOperationException();
   }

   @Override
   public double time () {
      throw new UnsupportedOperationException();
   }
}