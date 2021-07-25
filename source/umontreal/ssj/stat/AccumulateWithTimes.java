
/* *
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      <p>
 * @author
 * @version 1.0
 */


package umontreal.ssj.stat;
 
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.util.PrintfFormat;


/**
 * A subclass of {@link StatProbe},
 * for collecting statistics on a
 * variable that evolves in time, with a piecewise-constant trajectory.
 * Each time the variable changes its value, the method {@link #update(double,double) update} 
 * must be called to inform the probe of the new value.
 * The probe can be reinitialized by {@link #init() init}.
 * This class is similar to {@link Accumulate}, but it uses a user-specified
 * times rather than simulation times.
 */
public class AccumulateWithTimes extends StatProbe implements Cloneable  { 

   private double initTime;    // Initialization time.
   private double lastTime;    // Last update time.
   private double lastValue;   // Value since last update.


   /**
    * Constructs a new \texttt{Accumulate} statistical probe 
    * and initializes it by invoking {@link #init()}.
    * 
    */
   public AccumulateWithTimes()  {
      super();
      init();
   } 


   /**
    * Construct and initializes a new \texttt{Accumulate} 
    *    statistical probe with name \texttt{name} and initial time 0.
    * 
    */
   public AccumulateWithTimes (String name)  {
      super();
      this.name = name;
      init();
   } 

   /**
    * Initializes the statistical collector and puts the current 
    *    value of the corresponding variable to 0 at time 0.
    *    A call to \texttt{init} should normally be followed immediately by
    *    a call to {@link #update(double,double) update} to give the value of the variable at the
    *    initialization time.
    * 
    */
   public void init()  {
       maxValue = Double.MIN_VALUE;
       minValue = Double.MAX_VALUE;
       lastValue = 0.0;
       sumValue = 0.0;
       initTime = lastTime = 0;
   } 

   /**
    * Similar to {@link #init()}, but the initial time is given by
    * \texttt{time}.
    */
   public void init (double time) {
      init();
      initTime = lastTime = time;
   }

   /**
    * Same as {@link #init(double) init} followed by {@link #update(double,double) update}\texttt{(time, x)}.
    *  
    * @param x initial value of the probe
    * 
    * 
    */
   public void init (double time, double x)  {
       init (time);  update (time, x);
   } 


   /**
    * Updates the accumulator using the last value passed
    *   to {@link #update(double,double) update}.
    * 
    */
   public void update (double time) {
      update (time, lastValue);
   }


   /**
    * Gives a new observation (\texttt{time}, \texttt{x}) to the statistical collector.
    *    If broadcasting to observers is activated for this object, 
    *    this method will also transmit the new information to the
    *    registered observers by invoking the methods
    * {@link #notifyListeners(double) notifyListeners}.
    * 
    * @param time the time of the observation
    * @param x new observation given to the probe
    * 
    * 
    */
   public void update (double time, double x)  {
      assert time >= lastTime : "Time " + time + " must be greater than or equal to last time " + lastTime;
      if (collect) {
         if (x < minValue) minValue = x;
         if (x > maxValue) maxValue = x;
         sumValue += lastValue * (time - lastTime);
         lastValue = x;
         lastTime = time;
      }
      if (broadcast) {
         //setChanged();
         notifyListeners (x);
      }
   }


   public double sum()  { 
      return sumValue; 
   } 

   /**
    * Returns the time-average since the last initialization
    *     to the last call to {@link #update(double)}.
    * 
    */
   public double average()  {
      double periode = lastTime - initTime;
      if (periode > 0.0)  return sumValue/periode;
      else  return 0.0;
   }

   public String shortReportHeader() {
      PrintfFormat pf = new PrintfFormat();
      pf.append (-9, "from time").append ("   ");
      pf.append (-9, "to time").append ("   ");
      pf.append (-8, "   min").append ("   ");
      pf.append (-8, "   max").append ("   ");
      pf.append (-10, " average");
      return pf.toString();
   }

   public String shortReport() {
      PrintfFormat pf = new PrintfFormat();
      pf.append (9, 2, 2, getInitTime()).append ("   ");
      pf.append (9, 2, 2, getLastTime()).append ("   ");
      pf.append (8, 3, 2, min()).append ("   ");
      pf.append (8, 3, 2, max()).append ("   ");
      pf.append (10, 3, 2, average());
      return pf.toString();
   }



   public String report()  {
      update (lastValue);
      PrintfFormat str = new PrintfFormat();
      str.append ("REPORT on Accumulate stat. collector ==> " + name);
      str.append ("\n      from time   to time       min         max");
      str.append ("         average").append('\n');
      str.append (12, 2, 2, initTime); 
      str.append (13, 2, 2, lastTime);
      str.append (11, 3, 2, minValue);
      str.append (12, 3, 2, maxValue);
      str.append (14, 3, 2, average()).append ('\n');

      return str.toString();
    }


   /**
    * Returns the initialization time for this object.
    *   This is the simulation time when {@link #init() init} was called for
    *   the last time.
    * 
    * @return the initialization time for this object
    * 
    */
   public double getInitTime() {
      return initTime;
   }


   /**
    * Returns the last update time for this object.
    *    This is the simulation time of the last call to {@link #update(double,double) update} or
    *    the initialization time if {@link #update(double,double) update} was never called after
    *    {@link #init() init}.
    * 
    * @return the last update time of this object
    * 
    */
   public double getLastTime() {
      return lastTime;
   }


   /**
    * Returns the value passed to this probe by the last call
    *    to its {@link #update(double,double) update} method (or the initial value if 
    *    {@link #update update(double,double)} was never called after {@link #init() init}).
    * 
    * @return the last update value for this object
    * 
    */
   public double getLastValue() {
      return lastValue;
   }


   /**
    * Clone this object.
    * 
    */
   public AccumulateWithTimes clone() {
      try {
         return (AccumulateWithTimes)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException ("Accumulate can't clone");
      }
   }

}
