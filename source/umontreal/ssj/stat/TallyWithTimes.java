package umontreal.ssj.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import umontreal.ssj.simevents.Sim;

public class TallyWithTimes extends Tally {
   private final List<Observation> observations = new ArrayList<Observation>();
   
   @Override
   public void init() {
      super.init ();
      if (observations != null)
         observations.clear ();
   }
   
   @Override
   public void add (double x) {
      if (collect)
         observations.add (new Observation (Sim.time(), x));
      super.add (x);
   }
   
   public List<Observation> getObservations() {
      return Collections.unmodifiableList (observations);
   }
   
   public double averageOnInterval (double t1, double t2) {
      if (t1 > t2)
         throw new IllegalArgumentException
         ("t1 > t2");
      if (t1 == t2)
         return Double.NaN;
      final int i1 = getNextObs (t1);
      final int i2 = getNextObs (t2);
      if (i1 == -1 || i2 == -1)
         return Double.NaN;
      final int n = i2 - i1;
      if (n == 0)
         return Double.NaN;
      double sum = 0;
      for (int i = i1; i < i2; i++)
         sum += observations.get (i).getValue ();
      return sum / n;
   }
   
   public double averageFrom (double t) {
      final int i1 = getNextObs (t);
      final int i2 = observations.size () - 1;
      if (i1 == -1 || i2 == -1)
         return Double.NaN;
      final int n = i2 - i1;
      if (n == 0)
         return Double.NaN;
      double sum = 0;
      for (int i = i1; i < i2; i++)
         sum += observations.get (i).getValue ();
      return sum / n;
   }
   
   public int getNextObs (double time) {
      if (observations.isEmpty ())
         return -1;
      final double firstTime = observations.get (0).getTime ();
      if (time < firstTime)
         return 0;
      if (observations.size () == 1)
         return -1;
      final double lastTime = observations.get (observations.size () - 1).getTime ();
      if (time > lastTime)
         return -1;

      // Perform binary search to find the interval index
      int start = 0;
      int end = observations.size () - 1;
      int mid = (start + end) / 2;
      // Test if t is inside the interval mid.
      // The interval mid starts at times[mid],
      // and the interval mid+1 starts at times[mid + 1].
      while (time < observations.get (mid).getTime () ||
            time >= observations.get (mid + 1).getTime ()) {
         if (start == end)
            // Should not happen, safety check to avoid infinite loops.
            throw new IllegalStateException();
         if (time < observations.get (mid).getTime ())
            // time corresponds to an interval before mid.
            end = mid - 1;
         else
            // time corresponds to an interval after mid.
            start = mid + 1;
         mid = (start + end)/2;
      }
      return mid;
   }
   
   public static class Observation {
      private double time;
      private double x;
      
      public Observation (double time, double x) {
         this.time = time;
         this.x = x;
      }
      
      public double getTime() {
         return time;
      }
      
      public double getValue() {
         return x;
      }
   }
}
