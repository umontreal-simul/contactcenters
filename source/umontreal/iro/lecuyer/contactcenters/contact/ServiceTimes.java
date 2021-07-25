package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.Arrays;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.ssj.util.PrintfFormat;

/**
 * Stores service times for a contact. By default, there are two types of
 * service times: contact times and after-contact times.
 * However, a model can define additional types of service times.
 * For each of these types, one may generate a default service time $v$
 * which applies for all agents, one service time $v_i$ for each agent 
 * group $i$. This class can be used to store and retrieve such service 
 * times. For greater efficiency, it is recommended to call method
 * \texttt{ensureCapacityForServiceTime} before using \texttt{setServiceTime}
 * in order to avoid multiple array reallocations.
 * 
 */
public class ServiceTimes implements Cloneable {
   protected double servTime;          // service time
   private double[] servTimeGroups = null; // service time for each group

   /**
    * Constructs a new container for service times using
    * the default service time \texttt{serviceTime}.
    * It is used when no service time is given for a specific agent group.
    * @param serviceTime the default service time $v$.
    @exception IllegalArgumentException if the given service
    time is negative or NaN.
    */
   public ServiceTimes (double serviceTime) {
   	setServiceTime (serviceTime);
   }

   /**
    * Returns the default service time $v$ for this object.
    @return the default service time.
    */
   public double getServiceTime() {
      return servTime;
   }

   /**
    * Sets the default service time $v$ of this object to
    * \texttt{serviceTime}.
    @param serviceTime the new default service time.
    @exception IllegalArgumentException if the given service
    time is negative or NaN.
    */
   public void setServiceTime (double serviceTime) {
      if (serviceTime < 0 || Double.isNaN (serviceTime))
         throw new IllegalArgumentException ("serviceTime must be positive");
      this.servTime = serviceTime;
   }

   /**
    * Returns the service time $v_i$ for contacts served
    * by an agent in group \texttt{i}.
    * If this service time was never set, i.e., if
    * {@link #isSetServiceTime(int)} returns \texttt{false},
    * this returns the result of {@link #getServiceTime()}.
    * @param i the index of the agent group.
    * @return the service time $v_i$, or $v$ if $v_i$ is not set.
    */
   public double getServiceTime (int i) {
      if (isSetServiceTime (i))
         return servTimeGroups[i];
      else
         return getServiceTime();
   }
   
   /**
    * Returns the array of service times for all groups.
    * @return the service times of all groups
    */
   public double[] getServiceTimes () {
      return servTimeGroups;
   }

   /**
    * Determines if a service time was set specifically
    * for agent group \texttt{i}, by using
    * {@link #setServiceTime(int,double)}.
    * @param i the tested agent group index.
    * @return the result of the test.
    */
   public boolean isSetServiceTime (int i) {
      return servTimeGroups != null &&
             i < servTimeGroups.length && i >= 0 &&
             !Double.isNaN (servTimeGroups[i]);
   }

   /**
    * Sets the service time $v_i$ for contacts
    * served by an agent in group \texttt{i} to \texttt{t}.
    * Note that setting \texttt{t} to {@link Double#NaN}
    * unsets the service time for the specified agent group.
    * @param i the index of the agent group to set.
    * @param t the new service time.
    * @exception IllegalArgumentException if \texttt{t} is negative.
    */
   public void setServiceTime (int i, double t) {
      if (t < 0)
         throw new IllegalArgumentException ("t must be positive");
      if (Double.isNaN (t))
         return;
      if (servTimeGroups == null) {
         servTimeGroups = new double[i + 1];
         Arrays.fill (servTimeGroups, Double.NaN);
      }
      else if (servTimeGroups.length <= i) {
         final int s = servTimeGroups.length;
         servTimeGroups = ArrayUtil.resizeArray (servTimeGroups, i + 1);
         for (int j = s; j < servTimeGroups.length; j++)
            servTimeGroups[j] = Double.NaN;
      }
      servTimeGroups[i] = t;
   }
   
   /**
    * Makes sure that the length of the array containing the $v_i$'s is
    * at least \texttt{capacity} for the number of groups. 
    * This method should be called before
    * {@link #setServiceTime(int,double)}
    * to avoid multiple array reallocations.
    * @param capacity the new capacity for the groups
    */
   public void ensureCapacityForServiceTime (int capacity) {
      if (capacity < 0)
         throw new IllegalAccessError ("Negative capacity not allowed");
      if (servTimeGroups == null) {
         servTimeGroups = new double[capacity];
         Arrays.fill (servTimeGroups, Double.NaN);
      }
      else if (servTimeGroups.length < capacity) {
         final int s = servTimeGroups.length;
         servTimeGroups = ArrayUtil.resizeArray (servTimeGroups, capacity);
         for (int j = s; j < servTimeGroups.length; j++)
            servTimeGroups[j] = Double.NaN;
      }
   }
 
   /**
    * Replaces the service times $v$ and $v_i$'s stored in
    * this object with the values obtained from \texttt{st}.
    * @param st the input service times.
    */
   public void set (ServiceTimes st) {
      servTime = st.servTime;
      if (st.servTimeGroups != null)
         servTimeGroups = st.servTimeGroups.clone();
      else
      	servTimeGroups = null;
   }

   /**
    * Adds the service times stored in \texttt{st} to the corresponding 
    * service times in this object. Let $v$ and $v_i$ for
    * $i=0,\ldots$, be the service times in this object, and
    * $w$ and $w_i$, the service times in \texttt{st}.
    * This method replaces $v$ with $v+w$; and $v_i$ with $v_i+w_i$ for any
    * $i$ such that $v_i$ or $w_i$ exists.
    * When $v_i$ or $w_i$ does not exists, $v$ or $w$ is used.
    * @param st the service times to add to this object.
    */
   public void add (ServiceTimes st) {
      final int len1 = servTimeGroups == null ? 0 : servTimeGroups.length;
      final int len2 = st.servTimeGroups == null ? 0 : st.servTimeGroups.length;
      final int n = Math.max (len1, len2);
      ensureCapacityForServiceTime (n);
      st.ensureCapacityForServiceTime (n);
      for (int i = 0; i < n; i++) {
         if (!isSetServiceTime (i) && !st.isSetServiceTime (i))
            continue;
         setServiceTime (i, getServiceTime (i) + st.getServiceTime (i));
      }
      setServiceTime (getServiceTime () + st.getServiceTime ());
   }

   /**
    * Multiplies each service time $v$ and $v_i$ stored in 
    * this object by the given constant \texttt{mult}.
    * @param mult the multiplier for service times.
    * @exception IllegalArgumentException if \texttt{mult} is negative.
    */
   public void mult (double mult) {
      if (mult < 0)
         throw new IllegalArgumentException ("mult must not be negative");
      servTime *= mult;
      if (servTimeGroups != null) {
         for (int i = 0; i < servTimeGroups.length; i++)
            servTimeGroups[i] *= mult;
      }
   }

   /**
    * Clones this object, and its internal arrays
    * of service times.
    */
   @Override
   public ServiceTimes clone() {
      ServiceTimes cpy;
      try {
         cpy = (ServiceTimes)super.clone ();
      }
      catch (CloneNotSupportedException cne) {
         throw new InternalError();
      }
      
      if (servTimeGroups != null)
         cpy.servTimeGroups = servTimeGroups.clone ();
      return cpy;
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder ("servTime = ");
      sb.append (servTime);
      sb.append (PrintfFormat.NEWLINE);
      sb.append ("servTimeGroups = [");
      if (servTimeGroups != null) {
         for (int i = 0; i < servTimeGroups.length; i++)
            sb.append (i > 0 ? ", " : "").append (servTimeGroups[i]);
      }
      sb.append ("]");
      sb.append (PrintfFormat.NEWLINE);
      return sb.toString();
   }

}
