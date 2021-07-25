package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.Arrays;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.ssj.util.PrintfFormat;

/**
 * Stores service times for a contact. 
 * For each of these types, one may generate a default service time $v$
 * which applies for all agents, or one service time $va_j$ for each agent $j$.
 * This class is used to store and retrieve service times, when
 * they are different for each agent. Otherwise, one should use the mother
 * class \texttt{ServiceTimes}, where service times are defined only for
 * groups of agents.
 * For greater efficiency, it is recommended to call methods
 * \texttt{ensureCapacityForServiceTime}
 * before using \texttt{setServiceTime}
 * in order to avoid multiple array reallocations.
 * 
 */
public class ServiceTimesAgent extends ServiceTimes {
   private double[] servTimeAgents;    // service time for each agent

   /**
    * Constructs a new container for service times using
    * the default service time \texttt{serviceTime}
    * if no service time was given for a specific agent or group.
    * @param serviceTime the default service time $v$.
    @exception IllegalArgumentException if the given service
    time is negative or NaN.
    */
   public ServiceTimesAgent (double serviceTime) {
   	super (serviceTime);
   }

   /**
    * Returns the service time $va_j$ for contacts served by agent \texttt{j}.
    * If this service time was never set, i.e., if
    * {@link #isSetServiceTime(int)} returns \texttt{false},
    * this returns the result of {@link #getServiceTime()}.
    * @param j the index of the agent.
    * @return the service time $va_j$, or $v_i$ if $va_j$ is not
    * set, or $v$ if $v_i$ is not set.
    */
   @Override
   public double getServiceTime (int j) {
      if (isSetServiceTime (j))
         return servTimeAgents[j];
      else
         return getServiceTime();
   }

   /**
    * Returns the array of service times for all agents.
    * @return the service times of all agents
    */
   @Override
   public double[] getServiceTimes () {
      return servTimeAgents;
   }

   /**
    * Determines if a service time was set specifically
    * for agent \texttt{j} by using
    * {@link #setServiceTime(int,double)}.
    * @param j the tested agent index.
    * @return the result of the test.
    */
   @Override
   public boolean isSetServiceTime (int j) {
      return servTimeAgents != null &&
             j < servTimeAgents.length && j >= 0 &&
             !Double.isNaN (servTimeAgents[j]);
   }

   /**
    * Sets the service time $va_j$ for contacts
    * served by an agent \texttt{j} to \texttt{t}.
    * Note that setting \texttt{t} to {@link Double#NaN}
    * unsets the service time for the specified agent.
    * @param j index of the agent to set.
    * @param t new service time.
    * @exception IllegalArgumentException if \texttt{t} is negative.
    */
   @Override
   public void setServiceTime(int j, double t) {
		if (t < 0)
			throw new IllegalArgumentException("t must be positive");
		if (Double.isNaN(t))
			return;
		ensureCapacityForServiceTime (j+1);
		servTimeAgents[j] = t;
	}

   /**
    * Makes sure that the length of the array containing the $va_j$'s is
    * at least \texttt{capacity} for the number of agents.
    * This method should be called before
    * {@link #setServiceTime(int,double)}
    * to avoid multiple array reallocations.
    * @param capacity the new capacity for the agents.
    */
   @Override
   public void ensureCapacityForServiceTime (int capacity) {
      if (capacity < 0)
         throw new IllegalAccessError ("Negative capacity not allowed");
      
		if (servTimeAgents == null || servTimeAgents.length == 0) {
         servTimeAgents = new double[capacity];
			if (servTimeAgents.length == 0)
				return;
         Arrays.fill (servTimeAgents, Double.NaN);
         
		} else if (servTimeAgents.length < capacity) {
         int s = servTimeAgents.length;
         servTimeAgents = ArrayUtil.resizeArray (servTimeAgents, capacity);
         for (int j = s; j < servTimeAgents.length; j++)
            servTimeAgents[j] = Double.NaN;
      }
   }


   /**
    * Replaces the service times $v$, $v_i$'s  and $va_j$'s stored in
    * this object with the values obtained from \texttt{st}.
    * @param st the input service times.
    */
   @Override
   public void set (ServiceTimes st) {
   	super.set(st);
      servTimeAgents = st.getServiceTimes().clone();
   }

   /**
    * Adds the service times stored in \texttt{st} to the corresponding 
    * service times in this object. Let $v$ and $va_j$, for
    * $j=0,\ldots$, be the service times in this object, and
    * $w$ and $wa_j$, the service times in \texttt{st}.
    * This method replaces $v$ with $v+w$; and $va_j$ with $va_j + wa_j$
    * for any $j$ such that $va_j$ or $wa_j$ exists.
    * When $va_j$ or $wa_j$ does not exists, $v$ or $w$ is used.
    * @param st the service times to add to this object.
    */
   @Override
   public void add (ServiceTimes st) {
      final int len1 = servTimeAgents == null ? 0 : servTimeAgents.length;
      final int len2 = st.getServiceTimes() == null ? 0 :
      	                                     st.getServiceTimes().length;
      final int na = Math.max (len1, len2);
      ensureCapacityForServiceTime (na);
      st.ensureCapacityForServiceTime (na);
      for (int j = 0; j < na; j++) {
         if (!isSetServiceTime (j) && !st.isSetServiceTime (j))
            continue;
         double t = getServiceTime(j) + st.getServiceTime(j);
         setServiceTime (j, t);
      }
      setServiceTime (getServiceTime () + st.getServiceTime ());
   }

   
   /**
    * Multiplies each service time $v$ and $va_j$ stored in 
    * this object by the given constant \texttt{mult}.
    * @param mult the multiplier for service times.
    * @exception IllegalArgumentException if \texttt{mult} is negative.
    */
   @Override
   public void mult (double mult) {
      if (mult < 0)
         throw new IllegalArgumentException ("mult must not be negative");
      servTime *= mult;
      if (servTimeAgents != null) {
         for (int j = 0; j < servTimeAgents.length; j++)
            servTimeAgents[j] *= mult;
      }
   }

   /**
    * Clones this object, and its internal arrays
    * of service times.
    */
   @Override
   public ServiceTimesAgent clone() {
      ServiceTimesAgent cpy;
      try {
         cpy = (ServiceTimesAgent)super.clone ();
      }
      catch (Exception cne) {
         throw new InternalError();
      }
      
      if (servTimeAgents != null)
         cpy.servTimeAgents = servTimeAgents.clone ();
      return cpy;
   }
   
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder ("servTime = ");
      sb.append (servTime);
      sb.append (PrintfFormat.NEWLINE);
      sb.append ("servTimeAgents = [");
      if (servTimeAgents != null) {
         for (int j = 0; j < servTimeAgents.length; j++)
            sb.append (j > 0 ? ", " : "").append (servTimeAgents[j]);
      }
      sb.append ("]");
      sb.append (PrintfFormat.NEWLINE);  
      sb.append ("]");
      sb.append (PrintfFormat.NEWLINE);      
      return sb.toString();
   }

}
