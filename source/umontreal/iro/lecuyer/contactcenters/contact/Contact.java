package umontreal.iro.lecuyer.contactcenters.contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.ContactCenter;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

import umontreal.ssj.simevents.Simulator;

/**
 * Represents a contact (phone call, fax, e-mail, etc.)
 * into the contact center.
 * A contact enters
 * the system at a given time, and requires some form of service.  If
 * it cannot be served immediately, it joins a queue or leaves the system.
 * In more complex contact centers, contacts can be served more than once
 * and can join
 * several queues sequentially.
 * A contact object holds all the information about a single contact.
 * The arrival time, the total time spent in queue, the total
 * time spent in service, the last joined queue, and the last
 * serving agent group can be obtained from any contact object.
 * Information about the complete path of the contact into the system
 * can also be stored, but this is disabled by default to reduce memory
 * usage.
 *
 * For easier indexing in skill-based routers, every contact
 * has a numerical type identifier.  For waiting queues supporting
 * it, a contact object also holds a priority.
 * The \texttt{Contact} class implements the
 * {@link Comparable} interface
 * which allows to define the default priorities when contacts are in
 * priority queues.
 *
 * Extra information can be added to a contact object using two different
 * mechanisms: by adding attributes to the map
 * returned by {@link #getAttributes()}, or by
 * defining fields in a subclass.  The {@link #getAttributes} method returns
 * a {@link Map} that can be used to define custom contact attributes
 * dynamically.  This can be used for quick implementation of
 * user-defined attributes, but it can reduce performance
 * of the application since look-ups in a map are slower
 * than direct manipulation of fields.
 * Alternatively, this class can be extended to add new attributes
 * as fields.
 * However, the contact subclass will have to be used
 * for communication between parts of the program
 * needing the extra information, which involves casts.
 *
 * By default, no trunk group is associated with contacts.
 * As a result, every contact can enter the system, since its capacity
 * is infinite.  If a contact is associated with a trunk group using
 * {@link #setTrunkGroup},
 * a line is allocated by the router at the time of its arrival.  If
 * no line is available in the
 * associated trunk group, the contact is blocked.  Otherwise, it is
 * processed and the channel is released when it exits.
 *
 * Each contact has an associated simulator which is used to schedule
 * contact-related events such as abandonment or service termination.
 * This simulator is determined at the time of construction.
 * If a constructor accepting a {@link Simulator} instance is called,
 * the given simulator is used.
 * Otherwise, the default simulator returned by {@link Simulator#getDefaultSimulator()}
 * is used.
 */
public class Contact implements Comparable<Contact>, Named, Cloneable {
   private Simulator sim;
   private double arrivalTime;
   private double patienceTime = Double.POSITIVE_INFINITY;
   //private double contactTime = Double.POSITIVE_INFINITY;
   //private double afterContactTime = 0;
   //private double[] contactTimeGroups;
   //private double[] afterContactTimeGroups;
   private ServiceTimes contactTime = new ServiceTimes (Double.POSITIVE_INFINITY);
   private ServiceTimes afterContactTime = new ServiceTimes (0);
   private double priority = 1.0;
   private int typeId;
   private boolean hasExited = false;
   private String name = "";
   private Map<Object,Object> attributes = null;

   private double waitingTime = 0;
   private double waitingTimeEstimate=0;                              // ajouter
   private double positionInWaitingQueue=0;                           // ajouter
   private Map<Integer,Double> listeDesTypeTraitesParLesMemeAgents=null;  // ajouter
   private double startWaitingTime = -1;
   private int numQueues = 0;
   private int numAgentGroups = 0;

   private double totalServiceTime = 0;
   private double startServiceTime = -1;

   private WaitingQueue lastWaitingQueue = null;
   private AgentGroup lastAgentGroup = null;
   private ContactSource src;
   private TrunkGroup trunkGroup = null;
   private Router router = null;
   private List<ContactStepInfo> steps = null;

   /**
    * Constructs a new contact object with type identifier 0,
    * priority 1, and the default simulator.
    */
   public Contact() {
      this (Simulator.getDefaultSimulator (), 1.0, 0);
   }

   /**
    * Equivalent to constructor
    * {@link #Contact()}, with the
    * given simulator \texttt{sim}.
    * @param sim the simulator attached to the new contact.
    @exception NullPointerException if \texttt{sim} is \texttt{null}.
    */
   public Contact (Simulator sim) {
      this (sim, 1.0, 0);
   }

   /**
    * Constructs a new contact with priority 1,
    * type identifier \texttt{typeId}, and the
    * default simulator.
    @param typeId type identifier of the new contact.
    @exception IllegalArgumentException if the type identifier is negative.
    */
   public Contact (int typeId) {
      this (Simulator.getDefaultSimulator (), 1.0, typeId);
   }

   /**
    * Equivalent to constructor
    * {@link #Contact(int)}, with
    * the given simulator \texttt{sim}.
    @param sim the simulator attached to the new contact.
    @param typeId type identifier of the new contact.
    @exception NullPointerException if \texttt{sim} is \texttt{null}.
    @exception IllegalArgumentException if the type identifier is negative.
    */
   public Contact (Simulator sim, int typeId) {
      this (sim, 1.0, typeId);
   }
//Ajouter
   public Map<Integer, Double> getListeDesTraitesParLesMemeAgents(){
	   if (listeDesTypeTraitesParLesMemeAgents == null)
		   listeDesTypeTraitesParLesMemeAgents = new HashMap<Integer,Double>();
	   return listeDesTypeTraitesParLesMemeAgents;
   }
  
   //Ajouter
  
   public void setListeDesTraitesParLesMemeAgents(HashMap<Integer, Double> listeDesTraitesParLesMemeAgents){
	   this.listeDesTypeTraitesParLesMemeAgents=listeDesTraitesParLesMemeAgents;
   }
   
   /**
    * Constructs a new contact object with
    * a priority \texttt{priority},
    * type identifier \texttt{typeId}, and
    * the default simulator.
    * The contact
    * type identifier must be non-negative while the
    * priority can be any value.
    * The smaller is the value of \texttt{priority}, the greater is
    * the priority of the contact.
    @param priority the contact's priority.
    @param typeId the type identifier of this contact.
    @exception IllegalArgumentException if the type identifier is negative.
    */
   public Contact (double priority, int typeId) {
      this (Simulator.getDefaultSimulator (), priority, typeId);
   }

   /**
    * Equivalent to constructor
    * {@link #Contact(double,int)}, with
    * the given simulator \texttt{sim}.
    @param sim the simulator attached to the new contact.
    @param priority the contact's priority.
    @param typeId type identifier of the new contact.
    @exception NullPointerException if \texttt{sim} is \texttt{null}.
    @exception IllegalArgumentException if the type identifier is negative.
    */
   public Contact (Simulator sim, double priority, int typeId) {
      if (sim == null)
         throw new NullPointerException
         ("The attached simulator must not be null");
      if (typeId < 0)
         throw new IllegalArgumentException ("type ID must be >= 0");
      this.sim = sim;
      this.priority = priority;
      this.typeId = typeId;
      arrivalTime = sim.time ();
   }

   /**
    * Returns a reference to the simulator attached to
    * this contact.
    *
    * @return the simulator attached to this contact.
    */
   public final Simulator simulator() {
      return sim;
   }

   /**
    * Sets the simulator attached to this contact
    * to \texttt{sim}.
    * This method should not be called while
    * this contact is in a waiting queue,
    * or being served.
    * The main use of this method is for splitting:
    * a contact is cloned, and a new simulator is
    * assigned to the copy while the original
    * contact keeps the old simulator.
    * @param sim the new simulator.
    * @exception NullPointerException if \texttt{sim} is
    * \texttt{null}.
    */
   public final void setSimulator (Simulator sim) {
      if (sim == null)
         throw new NullPointerException
         ("The attached simulator must not be null");
      if (startWaitingTime >= 0)
         throw new IllegalStateException
         ("Cannot change the attached simulator while this contact is in a queue");
      if (startServiceTime >= 0)
         throw new IllegalStateException
         ("Cannot change the attached simulator while this contact is in service");
      this.sim = sim;
   }

   /**
    * Returns the contact's arrival simulation time.
    * This is the simulation time at which the contact
    * object was constructed.
    @return the arrival simulation time of this contact.
    */
   public double getArrivalTime() {
      return arrivalTime;
   }

   /**
    * Sets the arrival time of this contact to
    * \texttt{arrivalTime}.
    * This method should be called before
    * the contact enters into a waiting queue or
    * an agent group.
    * @param arrivalTime the new arrival time.
    * @exception IllegalArgumentException if \texttt{arrivalTime} is negative.
    */
   public void setArrivalTime (double arrivalTime) {
      if (arrivalTime < 0)
         throw new IllegalArgumentException
         ("The arrival time should not be negative");
      this.arrivalTime = arrivalTime;
   }

   /**
    * Returns the default patience time for this contact
    * object.  This corresponds to the maximal queue time
    * before the contact abandons the queue.
    * By default, this is \texttt{Double.POSI\-TIVE\_IN\-FIN\-ITY}, i.e.,
    * no abandonment occurs.
    @return the patience time of the contact.
    */
   public double getDefaultPatienceTime() {
      return patienceTime;
   }

   /**
    * Sets the default patience time of this contact to
    * \texttt{patienceTime}.
    @param patienceTime the new patience time of the contact.
    @exception IllegalArgumentException if the given patience time
    is negative or NaN.
    */
   public void setDefaultPatienceTime (double patienceTime) {
      if (patienceTime < 0 || Double.isNaN (patienceTime))
         throw new IllegalArgumentException ("patienceTime must be positive");
      this.patienceTime = patienceTime;
   }

   /**
    * Returns the contact times for this contact.
    * @return the contact times.
    */
   public ServiceTimes getContactTimes() {
      return contactTime;
   }

   /**
    * Returns the after-contact times for
    * this contact.
    * @return the after-contact times.
    */
   public ServiceTimes getAfterContactTimes() {
      return afterContactTime;
   }

   /**
    * Returns the default service time for this contact
    * object.  This corresponds to the result of
    * the sum of {@link #getDefaultContactTime}, and
    * {@link #getDefaultAfterContactTime}.
    @return the service time of the contact.
    */
   public double getDefaultServiceTime() {
      return getDefaultContactTime () + getDefaultAfterContactTime ();
   }

   /**
    * Sets the default service time of this contact to
    * \texttt{serviceTime}.
    * This method sets the contact time to \texttt{serviceTime}, and
    * resets the after-contact time to 0.
    @param serviceTime the new service time of the contact.
    @exception IllegalArgumentException if the given service
    time is negative or NaN.
    */
   public void setDefaultServiceTime (double serviceTime) {
      if (serviceTime < 0 || Double.isNaN (serviceTime))
         throw new IllegalArgumentException ("serviceTime must be positive");
      setDefaultContactTime (serviceTime);
      setDefaultAfterContactTime (0);
   }

   /**                                                                  
    * Returns the estimate waiting time that a call must before begining 
    *  it service in the call center. 
    * @return the waiting time estimate for a call when it arrive in the call center.
    */
   public double getWaitingTimeEstimate () {                //Ajouter pour recuperer le temps attente estimer
      return waitingTimeEstimate;
   }
   
   /**
    * Sets the waiting time estimate value for a call arivie in the call center. 
    * to \texttt{waitingTimeEstimate}.
    * @param waitingTimeVQ the new waiting time in virtual queue.
    */
   public void setWaitingTimeEstimate (double waitingTimeEstimate) {  // Ajouter pour affecter une valeur aux temps d attente estimer   
      this.waitingTimeEstimate = waitingTimeEstimate;
   }
   /**
    * Returns the default contact time with an agent.
    * By default, this is set to \texttt{Double.POSI\-TIVE\_IN\-FIN\-ITY}.
    @return the default contact time.
    */
   
                                                                 //Ajouter recuperer la position a la file
   public double getPositionInWaitingQueue() {  
	   return this.positionInWaitingQueue;}
   
                                                                  //Ajouter pour affecter une valeur 
   public void setPositionInWaitingQueue(double positionInWaitingQueue){
        this.positionInWaitingQueue=positionInWaitingQueue;
   }
   
   public double getDefaultContactTime() {
      return contactTime.getServiceTime ();
   }

   /**
    * Sets the default contact time to \texttt{contactTime}.
    @param contactTime the new contact time.
    @exception IllegalArgumentException if the contact time is negative or NaN.
    */
   public void setDefaultContactTime (double contactTime) {
      this.contactTime.setServiceTime (contactTime);
   }

   /**
    * Returns the default duration of after-contact work performed
    * by an agent after this contact is served.
    * By default, this is set to 0.
    @return the default after-contact time.
    */
   public double getDefaultAfterContactTime() {
      return afterContactTime.getServiceTime ();
   }

   /**
    * Sets the default after-contact time to \texttt{afterContactTime}.
    @param afterContactTime the new after-contact time.
    @exception IllegalArgumentException if the after-contact time is negative or NaN.
    */
   public void setDefaultAfterContactTime (double afterContactTime) {
      this.afterContactTime.setServiceTime (afterContactTime);
   }

   /**
    * Returns the default contact time if this contact
    * is served by an agent in group \texttt{i}.
    * If this contact time was never set, this
    * returns the result of
    * {@link #getDefaultContactTime()}.
    * @param i the index of the agent group.
    * @return the contact time.
    */
   public double getDefaultContactTime (int i) {
      return contactTime.getServiceTime (i);
   }

   /**
    * Determines if a contact time was set specifically
    * for agent group \texttt{i}, by using
    * {@link #setDefaultContactTime(int,double)}.
    * @param i the tested agent group index.
    * @return the result of the test.
    */
   public boolean isSetDefaultContactTime (int i) {
      return contactTime.isSetServiceTime (i);
   }

   /**
    * Sets the default contact time for this contact if
    * served by an agent in group \texttt{i}
    * to \texttt{t}.
    * Note that setting \texttt{t}
    * to {@link Double#NaN}
    * unsets the contact time for the
    * specified agent group.
    * @param i the index of the agent group to set.
    * @param t the new contact time.
    */
   public void setDefaultContactTime (int i, double t) {
      contactTime.setServiceTime (i, t);
   }

   /**
    * Makes sure that the array containing default contact
    * times specific to each agent group contains
    * at least \texttt{capacity} elements.
    * This method should be called before
    * {@link #setDefaultContactTime(int,double)}
    * to avoid multiple array reallocation.
    * @param capacity the new capacity.
    */
   public void ensureCapacityForDefaultContactTime (int capacity) {
      contactTime.ensureCapacityForServiceTime (capacity);
   }

   /**
    * Returns the default after-contact time if this contact
    * is served by an agent in group \texttt{i}.
    * If this after-contact time was never set, this
    * returns the result of
    * {@link #getDefaultAfterContactTime()}.
    * @param i the index of the agent group.
    * @return the after-contact time.
    */
   public double getDefaultAfterContactTime (int i) {
      return afterContactTime.getServiceTime (i);
   }

   /**
    * Determines if an after-contact time was set specifically
    * for agent group \texttt{i}, by using
    * {@link #setDefaultAfterContactTime(int,double)}.
    * @param i the tested agent group index.
    * @return the result of the test.
    */
   public boolean isSetDefaultAfterContactTime (int i) {
      return afterContactTime.isSetServiceTime (i);
   }

   /**
    * Sets the default after-contact time for this contact to \texttt{t},
    * if served by an agent in group \texttt{i}.
    * Note that setting \texttt{t}
    * to {@link Double#NaN}
    * unsets the after-contact time for the
    * specified agent group.
    * @param i the index of the agent group to set.
    * @param t the new after-contact time.
    */
   public void setDefaultAfterContactTime (int i, double t) {
      afterContactTime.setServiceTime (i, t);
   }

   /**
    * Makes sure that the array containing default after-contact
    * times specific to each agent group contains
    * at least \texttt{capacity} elements.
    * This method should be called before
    * {@link #setDefaultAfterContactTime(int,double)}
    * to avoid multiple array reallocation.
    * @param capacity the new capacity.
    */
   public void ensureCapacityForDefaultAfterContactTime (int capacity) {
      afterContactTime.ensureCapacityForServiceTime (capacity);
   }

   /**
    * Returns the default service time for this contact if served
    * by an agent in group \texttt{i}.
    * This returns the sum of
    * {@link #getDefaultContactTime(int)}
    * and {@link #getDefaultAfterContactTime(int)}.
    * @param i the tested agent group.
    * @return the default service time.
    */
   public double getDefaultServiceTime (int i) {
      return getDefaultContactTime (i) + getDefaultAfterContactTime (i);
   }

   public String getName() {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
   }

   /**
    * Returns the list containing the steps in the
    * life cycle of this contact.  This list should
    * contain {@link ContactStepInfo} implementations only.
    * If steps tracing was not enabled for this
    * contact, this returns \texttt{null}.
    @return the list of contact steps, or \texttt{null} if
    steps tracing is disabled.
    */
   public List<ContactStepInfo> getSteps() {
      return steps;
   }

   /**
    * Enables steps tracing for this contact
    * object.  By default, steps tracing is disabled
    * for better performance and memory usage.
    * This method should be called as soon as the
    * contact is constructed to avoid
    * any loss of information.
    */
   public void enableStepsTracing() {
      if (steps == null)
         steps = new ArrayList<ContactStepInfo>();
   }

   /**
    * Returns the priority for this contact.  The
    * priority is a number which indicates the level of
    * emergency of the contact.  A low value represents a high
    * priority.
    @return the contact's priority.
    */
   public double getPriority() {
      return priority;
   }

   /**
    * Changes the contact's priority to \texttt{newPriority}.
    * This method should not be called when a contact is in a priority queue.
    @param newPriority the new contact's priority.
    */
   public void setPriority (double newPriority) {
      priority = newPriority;
   }

   /**
    * Returns the total time the contact
    * has spent waiting in queues.
    * This returns the cumulative waiting time, for all
    * waiting queues visited by the contact.
    @return the contact's queue time.
    */
   public double getTotalQueueTime() {
      if (startWaitingTime >= 0) {
         assert numQueues > 0;
         return waitingTime + sim.time() - startWaitingTime;
      }
      return waitingTime;
   }

   /**
    * Adds \texttt{delta} to the currently recorded
    * total queue time returned by
    * {@link #getTotalQueueTime()}.
    * This method can be used, e.g., to subtract
    * time passed in a virtual queue from the queue
    * time of a contact.
    * @param delta the amount to add.
    */
   public void addToTotalQueueTime (double delta) {
      waitingTime += delta;
   }

   /**
    * Returns the last waiting queue this contact
    * entered in.  If the contact was never queued,
    * this returns \texttt{null}.
    @return the last waiting queue of the contact.
    */
   public WaitingQueue getLastWaitingQueue() {
      return lastWaitingQueue;
   }

   /**
    * Returns the total time this contact
    * has spent being served by agents.
    * This returns the cumulative contact time (not after-contact time) for
    * all agents visited by the contact.
    @return the contact's service time.
    */
   public double getTotalServiceTime() {
      if (startServiceTime >= 0)
         return totalServiceTime + sim.time() - startServiceTime;
      return totalServiceTime;
   }

   /**
    * Adds \texttt{delta} to the currently recorded
    * total service time returned by
    * {@link #getTotalServiceTime()} for this contact.
    * @param delta the amount to add.
    */
   public void addToTotalServiceTime (double delta) {
      totalServiceTime += delta;
   }

   /**
    * Returns the last agent group who began
    * serving this contact.  If the contact was never served,
    * this returns \texttt{null}.
    @return the last agent group serving the contact.
    */
   public AgentGroup getLastAgentGroup() {
      return lastAgentGroup;
   }

   /**
    * Returns the type identifier for this contact object.
    @return the contact's type identifier.
    */
   public int getTypeId() {
      return typeId;
   }

   /**
    * Changes the type identifier for this contact object
    * to \texttt{newTypeId}.
    * The type identifier of a contact should not change
    * when it is in a waiting queue or served by an agent.
    @param newTypeId the contact's new type identifier.
    @exception IllegalArgumentException if the type identifier
    is smaller than 0.
    */
   public void setTypeId (int newTypeId) {
      if (newTypeId < 0)
         throw new IllegalArgumentException ("Type identifier must not be negative");
      typeId = newTypeId;
   }

   /**
    * Returns the map containing the attributes for this
    * contact.  Attributes can be used to add user-defined information
    * to contact objects at runtime, without creating
    * a subclass.  However, for maximal efficiency,
    * it is recommended to create a subclass of \texttt{Contact}
    * instead of using attributes.
    @return the map containing the attributes for this object.
    */
   public Map<Object,Object> getAttributes() {
      if (attributes == null)
         attributes = new HashMap<Object,Object>();
      return attributes;
   }

   /**
    * Returns the contact's primary source which has produced
    * this contact object.
    * If no source has created this
    * contact, this returns \texttt{null}.
    * If a contact results from a call back managed by a dialer,
    * this returns the preceding arrival process which
    * created the contact, not the dialer managing the call back.
    @return the source having created this contact.
    */
   public ContactSource getSource() {
      return src;
   }

   /**
    * Sets the source of this contact to
    * \texttt{src}.  Once a non-\texttt{null} source
    * was given, it cannot be changed.
    * If one tries to change the contact source,
    * an {@link IllegalStateException} is thrown.
    @param src the new contact source.
    @exception IllegalStateException if one tries
    to change the contact source.
    */
   public void setSource (ContactSource src) {
      if (this.src != null && this.src != src)
         throw new IllegalStateException
            ("Cannot change the contact source");
      this.src = src;
   }

   /**
    * Returns the trunk group this contact will take a trunk from.
    * By default, this returns \texttt{null}.
    @return the associated trunk group.
    */
   public TrunkGroup getTrunkGroup() {
      return trunkGroup;
   }

   /**
    * Sets the trunk group for this contact to \texttt{tg}.
    * This method does not allocate a trunk in the group;
    * this task is performed by the router.
    @param tg the new trunk group.
    */
   public void setTrunkGroup (TrunkGroup tg) {
      trunkGroup = tg;
   }

   /**
    * Returns a reference to the router currently managing
    * this contact, or \texttt{null} if the contact is not
    * currently in a router.
    @return the router taking care of this contact.
    */
   public Router getRouter() {
      return router;
   }

   /**
    * This should only be called by the router.
    * Associates the router \texttt{router} to this contact.
    @param router the new router.
    */
   public void setRouter (Router router) {
      this.router = router;
   }

   /**
    * Determines if the contact has exited the
    * system.  If a contact has exited the system,
    * it will not be admitted into a router
    * for further processing.
    * This is used to prevent contacts from incorrectly
    * entering in the router several times.
    @return the exited indicator.
    */
   public boolean hasExited() {
      return hasExited;
   }

   /**
    * Sets the exited indicator to \texttt{b}.
    @param b the exited indicator.
    */
   public void setExited (boolean b) {
      hasExited = b;
   }

   /**
    * This method is called by a waiting queue object when
    * a contact is put in queue,
    * the dequeue event \texttt{ev} representing the queued contact.
    @param ev the dequeue event associated with the contact.
    */
   public void enqueued (DequeueEvent ev) {
      if (startWaitingTime == -1)
         startWaitingTime = sim.time ();
      lastWaitingQueue = ev.getWaitingQueue();
      ++numQueues;
   }

   /**
    * This method is called when a contact leaves a queue, the
    * dequeue event \texttt{ev} representing the queued contact.
    @param ev the dequeue event associated with the contact.
    */
   public void dequeued (DequeueEvent ev) {
      if (numQueues == 0)
         throw new IllegalStateException
            ("The dequeued method was called while the contact was not queued");
      assert sim == ev.simulator ();
      assert this == ev.getContact ();
      --numQueues;
      assert startWaitingTime >= 0 : "Enqueue time not initialized";
      if (numQueues == 0) {
         assert sim.time() >= startWaitingTime
            : "The contact was dequeued before it was enqueued";
         final double time = sim.time ();
         if (startWaitingTime < time)
            waitingTime += time - startWaitingTime;
         startWaitingTime = -1;
      }
      if (steps != null)
         steps.add (ev);
   }

   /**
    * This method is called when the contact is blocked
    * by its current router with blocking type \texttt{bType}.
    * The {@link #getRouter} method can be used to access
    * the reference to the router which blocked this contact while
    * \texttt{bType} indicates the reason why the contact was blocked.
    @param bType the contact blocking type.
    */
   public void blocked (int bType) {}

   /**
    * This method is called when the service of this contact by an
    * agent  begins, the end-service event \texttt{ev} representing
    * the contact being served.
    @param ev the event occurring at the end of the service.
    */
   public void beginService (EndServiceEvent ev) {
      if (startServiceTime == -1)
         startServiceTime = sim.time ();
      ++numAgentGroups;
      lastAgentGroup = ev.getAgentGroup();
   }

   /**
    * This method is called when the communication between
    * this contact and an agent is terminated.  The end-service event \texttt{ev}
    * can be used to obtain information about the end of
    * communication.
    @param ev the end-service event associated with the contact.
    */
   public void endContact (EndServiceEvent ev) {
      if (numAgentGroups == 0)
         throw new IllegalStateException
            ("The endContact method was called while the contact was not being served by an agent");
      assert sim == ev.simulator ();
      assert this == ev.getContact ();
      --numAgentGroups;
      assert startServiceTime >= 0 : "Begin service time not initialized";
      if (numAgentGroups == 0) {
         assert sim.time() >= startServiceTime
            : "The service started before it ends.";
         final double time = sim.time ();
         if (startServiceTime < time)
            totalServiceTime += time - startServiceTime;
         startServiceTime = -1;
      }
      if (steps != null)
         steps.add (ev);
   }

   /**
    * This method is called when the service of this contact (communication and
    * after-contact work) was terminated,
    * \texttt{ev} containing information about the served contact.
    @param ev the end-service event associated with the contact.
    */
   public void endService (EndServiceEvent ev) {
   }

   /**
    * Returns the number of waiting queues this
    * contact is waiting in
    * simultaneously, at the current simulation time.
    * @return the number of waiting queues this contact is waiting in.
    */
   public int getNumWaitingQueues() {
      return numQueues;
   }

   /**
    * Returns the number of agent groups serving
    * this contact simultaneously
    * at the current simulation time.
    * @return the number of agent groups serving this
    * contact simultaneously.
    */
   public int getNumAgentGroups() {
      return numAgentGroups;
   }

   /**
    * Compares this contact with \texttt{otherContact}.
    * By default, the contacts are ordered in ascending order of priority.
    * The lower the priority value, the more important the contact will be.
    * If two compared contacts share the same priority, they are ordered
    * using their arrival times.
    @param otherContact the other contact this contact is compared to.
    @return a value smaller than 0 if this contact is greater
    than the other object, 0 if it is equal,
    or a value greater than 0 if it is smaller.
    */
   public int compareTo (Contact otherContact) {
      if (priority < otherContact.getPriority())
         return -1;
      else if (priority > otherContact.getPriority())
         return 1;
      else
         return (int)(arrivalTime - otherContact.getArrivalTime());
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      if (getName().length() > 0)
         sb.append ("name: ").append (getName()).append (", ");
      sb.append ("arrival time: ").append (arrivalTime);
      sb.append (", priority: ").append (priority);
      sb.append (", type identifier: ").append (typeId);
      if (lastWaitingQueue != null) {
         sb.append (", total queue time: ").append (getTotalQueueTime());
         sb.append (", last waiting queue: ").append
            (ContactCenter.toShortString (lastWaitingQueue));
      }
      if (lastAgentGroup != null) {
         sb.append (", total service time: ").append (getTotalServiceTime());
         sb.append (", last agent group: ").append
            (ContactCenter.toShortString (lastAgentGroup));
      }
      // We cannot add the contact's steps into
      // the string, because dequeue and end-service
      // events' toString methods obtain
      // the string representation of the
      // associated contact.
      // This would therefore result in an
      // infinite loop.
      sb.append (']');
      return sb.toString();
   }

   /**
    * Returns a copy of this contact object.
    * In contrast with the original contact object,
    * the returned copy is not in any waiting
    * queue, router, or agent group.
    * The map containing the attributes,
    * if {@link #getAttributes} returns
    * a non-\texttt{null} value,
    * is cloned, but the elements in the
    * map are not cloned.
    * If contact steps tracing is enabled,
    * the list of steps as well as
    * the step objects are cloned.
    * @return the copy of the contact.
    */
   @Override
   public Contact clone() {
      Contact cpy;
      try {
         cpy = (Contact)super.clone();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError ("CloneNotSupportedException for a class implementing Cloneable");
      }
      cpy.startWaitingTime = -1;
      cpy.startServiceTime = -1;
      cpy.router = null;
      cpy.numAgentGroups = 0;
      cpy.numQueues = 0;
      if (steps != null) {
         cpy.steps = new ArrayList<ContactStepInfo> ();
         for (final ContactStepInfo step : steps)
            cpy.steps.add (step.clone (this));
      }
      if (attributes != null)
         cpy.attributes = new HashMap<Object,Object> (attributes);
      cpy.contactTime = contactTime.clone ();
      cpy.afterContactTime = afterContactTime.clone ();
      return cpy;
   }
}
