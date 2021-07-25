package umontreal.iro.lecuyer.contactcenters.app.trace;


/**
 * Represents an object capable of creating a contact-by-contact trace.
 * The format and location of the produced trace depends on the
 * implementation.
 * 
 * The tracing facility is used as follows.
 * An
 * implementation of this interface is initialized at the beginning of the
 * simulation using the {@link #init()} method.
 * Each time a contact is processed, the 
 * {@link #writeLine(int,int,int,double,double,String,int,double)}
 * method is called by some listener.
 * At the end of the simulation, the {@link #close()}
 * method is called to close the file or database connection the trace
 * is sent to.
 */
public interface ContactTrace {
   public static final String OUTCOME_BLOCKED = "Blocked";
   public static final String OUTCOME_ABANDONED = "Abandoned";
   public static final String OUTCOME_SERVED = "Served";
   public static final String OUTCOME_FAILED = "Failed";
   
   /**
    * Initializes the tracing mechanism. This method opens the trace file or
    * database connection, and writes headers, etc. If an error occurs during
    * the initialization, this method should log the error, and disable tracing.
    */
   public void init ();

   /**
    * Closes the trace facility after a simulation. This method closes files,
    * database connections, etc.
    */
   public void close ();
   
   /**
    * Writes a new line in the trace representing a simulated contact.
    * The line includes the step of the simulation at which the contact
    * occurred, the type of the contact, the period of its arrival,
    * its time spent in queue, its outcome, the group of its serving agent, and
    * its service time.
    * Some of these fields might be {@link Double#NaN}
    * if the information does not exist. For example,
    * a blocked or abandoned call does not have a serving agent group, or
    * a service time.
    * @param step the step, in the experiment, during which the call occurred.
    * @param type the type of the call.
    * @param period the period of arrival of the call.
    * @param arvTime the arrival time.
    * @param queueTime the time spent by the call in the queue.
    * @param outcome the outcome of the call.
    * @param group the group of the serving agent.
    * @param srvTime the service time of the call.
    */
   public void writeLine (int step, int type, int period, double arvTime,
         double queueTime, String outcome, int group, double srvTime);
}
