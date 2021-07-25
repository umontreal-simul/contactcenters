package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.ArrayList;
import java.util.List;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.DetailedServingCall;
import umontreal.iro.lecuyer.contactcenters.msk.params.DetailedStartingGroupState;
import umontreal.iro.lecuyer.contactcenters.msk.params.DetailedStartingQueueState;
import umontreal.iro.lecuyer.contactcenters.msk.params.DetailedWaitingCall;
import umontreal.iro.lecuyer.contactcenters.msk.params.StartingStateParams;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

/**
 * This class represents the starting state of the waiting queues and 
 * the occupancy of the agent groups, at the beginning of the simulation.
 * The state of a queue contains the number of calls waiting and possibly their waiting times.
 * The occupancy state of an agent group contains the number of calls being served
 * by this group, and possibly the amount of service times received by each call
 * before the start of the simulation.
 * This parameter requires the waiting queues to be associated with the call types 
 * (one queue per call type).
 * 
 * For the exponential distribution case, the waited times and served times
 * are not required in order to generate the remaining patience times and service times.
 * The user only needs to input the number of calls in each queue and the
 * number of calls being served by each agent group.
 * The starting state can be retrieved by calling {@link #getQueueSizes()} and
 * {@link #getCallServiceByGroup()}.
 * 
 * For other patience time and service time distributions, the user needs to
 * give a detailed starting state of the queues and agent groups.
 * The user needs to input the waited time and served time for each call.
 * The starting state can be retrieved by calling {@link #getDetailedQueues()} and
 * {@link #getDetailedGroups()}.
 * 
 * Hence, there are 2 methods to get the starting state of the queues or the groups.
 * These methods are mutually exclusive: if one is available, then the other method is not.
 * Use the functions {@link #isQueueSizesAvailable()}, {@link #isCallServiceByGroupAvailable()},
 * {@link #isDetailedQueuesAvailable()} and {@link #isDetailedGroupsAvailable()}
 * to check if a particular parameter is available or not.
 * If a parameter is not available, then its associated function will return {@code null}.
 * 
 */
public class StartingState {
   
   private CallCenterParams ccParams;

   /**
    * Contains the initial number of calls in each waiting queue.
    * The size of this array must be equal to the number of call types.
    */
   private int[] queueSizes = null;
   
   /**
    * Contains the starting state of each waiting queue with detailed calls.
    * Each call contains the amount of time it has already waited (before the simulation).
    */
   private List<StartingWaitingQueue> detailedQueues = null;
   
   /**
    * Each element of this matrix represents the number of calls in service 
    * by an agent group at the start of the simulation, where 
    * each row represents a group and each column represents a call typeId.
    */
   private int[][] callByGroup = null;
   
   /**
    * Contains the starting state of each group with the calls being served by
    * each group.
    * Each call contains the amount of time it has waited
    * and the amount of service time it has already received.
    * These time durations are assumed to have occurred before the simulation.
    */
   private List<StartingServingGroup> detailedGroups  = null;

   /**
    * Switch to enable or disable this starting state initialization feature.
    */
   private boolean enabled;
   
   /**
    * Instantiates the starting state of the waiting queues and agent groups occupancies.
    * 
    * @param ccParams the call center parameter
    */
   public StartingState(CallCenterParams ccParams) {
      int numTypes = ccParams.getInboundTypes().size();
      int numGroups = ccParams.getAgentGroups().size();
      
      this.ccParams = ccParams;
      
      StartingStateParams ss = ccParams.getStartingState();
      if (ss == null)
         return;
      
      enabled = ss.isEnabled();
      
      // read the queue
      if (ss.getQueueSizes() != null && ss.getQueueSizes().size() > 0) {
         if (ss.getQueueSizes().size() != numTypes)
            throw new IllegalArgumentException("Starting queue size parameter error: "
                    + "the number of queue sizes must be equal to the number of call types.");
         queueSizes = new int[numTypes];
         for (int i = 0; i < ss.getQueueSizes().size(); i++)
            queueSizes[i] = ss.getQueueSizes().get(i);
      }
      else if (ss.getDetailedQueueState() != null && ss.getDetailedQueueState().size() > 0) {
         if (ss.getDetailedQueueState().size() != numTypes)
            throw new IllegalArgumentException("Starting queue size parameter error: "
                    + "the number of queue states must be equal to the number of call types.");
         detailedQueues = new ArrayList<StartingWaitingQueue>(numTypes);
         for (DetailedStartingQueueState s : ss.getDetailedQueueState())
            detailedQueues.add(new StartingWaitingQueue(ccParams, s));
      }
      else
         throw new IllegalArgumentException("Missing starting queue state parameter.");

      if (ss.getCallByGroup() != null && ss.getCallByGroup().getRows().size() > 0) {
         if (ss.getCallByGroup().getRows().size() != numGroups)
            throw new IllegalArgumentException("Starting group state matrix has an invalid number of rows. "
                    + "It must be equal to the number of agent groups.");
         callByGroup = ArrayConverter.unmarshalArray(ss.getCallByGroup());
         // check the dimensions of the matrix
         for (int i = 0; i < callByGroup.length; i++) {
            if (callByGroup[i].length != numTypes)
               throw new IllegalArgumentException("Wrong matrix size for the starting state of the queues and agent groups."
                + " The number of columns must be equal to the number of call types.");
         }
      }
      else if (ss.getDetailedGroupState() != null && ss.getDetailedGroupState().size() > 0) {
         if (ss.getDetailedGroupState().size() != numGroups)
            throw new IllegalArgumentException("Starting group state parameter error: "
                    + "the number of group states must be equal to the number of groups.");
         detailedGroups = new ArrayList<StartingServingGroup>(numGroups);
         for (DetailedStartingGroupState s : ss.getDetailedGroupState())
            detailedGroups.add(new StartingServingGroup(ccParams, s));
      }
      else
         throw new IllegalArgumentException("Missing starting group state parameter.");
   }
   
   /**
    * Returns the number of calls waiting in each queue at the start of the simulation.
    * The size of this array is equal to the number of call types.
    * 
    * @return the number of calls waiting in each queue at the start of the simulation
    */
   public int[] getQueueSizes() { return queueSizes; }
   
   /**
    * Returns the number of calls in service by each agent group at the start 
    * of the simulation.
    * Each row represents an agent group and each column represents a call type.
    * 
    * @return the number of calls in service by each agent group at the start 
    * of the simulation
    */
   public int[][] getCallServiceByGroup() { return callByGroup; }
   
   /**
    * Returns a detailed starting state of the waiting queues, before the start 
    * of the simulation.
    * 
    * @return a detailed starting state of the waiting queues
    */
   public List<StartingWaitingQueue> getDetailedQueues() { return detailedQueues; }
   
   /**
    * Returns a detailed starting state of the groups and the calls they are 
    * serving, before the start of the simulation.
    * 
    * @return a detailed starting state of the groups and the calls they are serving 
    */
   public List<StartingServingGroup> getDetailedGroups() { return detailedGroups; }

   
   /**
    * Checks if the parameter associated with the function {@link #getQueueSizes()} is defined.
    * 
    * @return {@code true} if the starting state parameter is defined
    */
   public boolean isQueueSizesAvailable() { return queueSizes != null; }
   
   /**
    * Checks if the parameter associated with the function {@link #getCallServiceByGroup()} is defined.
    * 
    * @return {@code true} if the starting state parameter is defined
    */
   public boolean isCallServiceByGroupAvailable() { return callByGroup != null; }
   
   /**
    * Checks if the parameter associated with the function {@link #getDetailedQueues()} is defined. 
    * 
    * @return {@code true} if the starting state parameter is defined
    */
   public boolean isDetailedQueuesAvailable() { return detailedQueues != null; }
   
   /**
    * Checks if the parameter associated with the function {@link #getDetailedGroups()} is defined.
    * 
    * @return {@code true} if the starting state parameter is defined
    */
   public boolean isDetailedGroupsAvailable() { return detailedGroups != null; }
   
   /**
    * Checks if this starting state initialization feature is enabled or not.
    * 
    * @return if this starting state initialization feature is enabled
    */
   public boolean isEnabled() { return enabled; }
   
   /**
    * Sets this starting state initialization feature to be enabled or disabled.
    * 
    * @param e set to {@code true} to enable this starting state initialization
    */
   public void setEnabled(boolean e) { this.enabled = e; }
   
   
   /**
    * Represents the starting state of a waiting queue with the calls inside.
    */
   public class StartingWaitingQueue {
      
      private final List<WaitingCall> queue; // the queue that contains the waiting calls
      
      /**
       * Instantiates the starting state of a waiting queue with detailed waiting calls.
       * 
       * @param ccParams the call center parameter
       * @param params the waiting queue state parameter
       */
      public StartingWaitingQueue(CallCenterParams ccParams, DetailedStartingQueueState params) {
         
         queue = new ArrayList<WaitingCall>(params.getWaitingCall().size());
         
         for(DetailedWaitingCall c : params.getWaitingCall()) {
            double millis = c.getWaitedTime().getTimeInMillis(CallCenterUtil.getDate(ccParams.getStartingDate()));
            queue.add(new WaitingCall(c.getType(), TimeUnit.convert(millis, TimeUnit.MILLISECOND, TimeUnit.valueOf(ccParams.getDefaultUnit().name()))));
         }
      }
      
      /**
       * Returns the state of this waiting queue and the waiting calls.
       * 
       * @return the state of this waiting queue
       */
      public List<WaitingCall> getQueue() {return queue;}
   }
   
   /**
    * Represents the calls being served by a particular group.
    */
   public class StartingServingGroup {
      
      private final List<ServingCall> servingList; // contains the calls being served by this group
      
      /**
       * Instantiates the starting state of an agent group.
       * 
       * @param ccParams the call center parameter
       * @param params the group state parameter
       */
      public StartingServingGroup(CallCenterParams ccParams, DetailedStartingGroupState params) {
         servingList = new ArrayList<ServingCall>(params.getServingCall().size());
         
         for(DetailedServingCall c : params.getServingCall()) {
            double serv = c.getServedTime().getTimeInMillis(CallCenterUtil.getDate(ccParams.getStartingDate()));
            serv = TimeUnit.convert(serv, TimeUnit.MILLISECOND, TimeUnit.valueOf(ccParams.getDefaultUnit().name()));
            double wait = c.getWaitedTime().getTimeInMillis(CallCenterUtil.getDate(ccParams.getStartingDate()));
            wait = TimeUnit.convert(wait, TimeUnit.MILLISECOND, TimeUnit.valueOf(ccParams.getDefaultUnit().name()));
            servingList.add(new ServingCall(c.getType(), wait, serv, c.getServingAgentID()));
         }
      }
      
      /**
       * Returns the list of calls being served by this group at the start of the simulation.
       * 
       * @return the list of calls being served by this group
       */
      public List<ServingCall> getServingList() { return servingList;}
      
   }
   
   /**
    * Represents a waiting call inside a queue, when initializing the simulation
    * with non-empty queues in the call center.
    */
   public class WaitingCall {
      
      /**
       * The waited time by this call before the start of the simulation.
       * The time unit must be the same as the simulator.
       */
      protected double waitedTime; 
      
      /**
       * The call type ID of this call.
       */
      protected final int typeId;
      
      /**
       * Constructs a new waiting call with a given waited time.
       * 
       * @param type the call type ID of this call
       * @param waitedTime the amount of time this call has already waited in queue,
       * before starting the simulation. The time unit must be the same as the simulator.
       */
      public WaitingCall(int type, double waitedTime) {
         if (waitedTime < 0)
            throw new IllegalArgumentException("Waited time duration cannot be negative.");
         this.typeId = type;
         this.waitedTime = waitedTime;
      }
      
      /**
       * Returns the amount of time this call has already waited, before the start of the simulation.
       * The time unit is the same as the simulator.
       * 
       * @return the amount of time this call has already waited
       */
      public double getWaitedTime() { return waitedTime; }
      
      /**
       * Returns the call type ID of this call.
       * 
       * @return the call type of this call
       */
      public int getTypeId() { return typeId; }
   }
   
   /**
    * Represents a call being served, before the start of a simulation.
    */
   public class ServingCall extends WaitingCall {
      
      /**
       * The time this call has been in service before the start of the simulation.
       * The time unit must be the same as the simulator.
       */
      protected double servedTime;
      
      /**
       * The ID number of the agent that is serving this call. This parameter is optional.
       * This parameter will be ignored if its value is negative.
       */
      protected int agentID = -1;
      
      /**
       * Constructs a call being served at the start of the simulation.
       * The parameter {@link #agentID} is set to -1.
       * 
       * @param type the call type ID of this call. This parameter is required and it cannot be negative.
       * @param waitedTime the time waited before being entering service. 
       * The time unit must be the same as the simulator.
       * @param servedTime the amount of time this call has been in service, before the start of
       * the simulation. The time unit must be the same as the simulator.
       */
      public ServingCall(int type, double waitedTime, double servedTime) {
         this(type, waitedTime, servedTime, -1);
      }
      
      /**
       * Constructs a call being served at the start of the simulation.
       * 
       * @param type the call type ID of this call. This parameter is required and it cannot be negative.
       * @param waitedTime the time waited before being entering service. 
       * The time unit must be the same as the simulator.
       * @param servedTime the time this call has been in service, before the start of
       * the simulation. The time unit must be the same as the simulator.
       * @param agentID the ID number of the agent that is serving this call.
       * Give a negative number if the agent has no ID number.
       */
      public ServingCall(int type, double waitedTime, double servedTime, int agentID) {
         super(type, waitedTime);
         if (type < 0 || type >= ccParams.getInboundTypes().size())
            throw new IllegalArgumentException("Invalid call type attribute when defining the "
                    + "starting calls already in service.");
         this.servedTime = servedTime;
         this.agentID = agentID;
      }
      
      /**
       * Returns the served time of this call before the start of the simulation.
       * The time unit is the same as the simulator.
       * 
       * @return the served time of this call before the start of the simulation
       */
      public double getServedTime() { return servedTime; }

      
      /**
       * Returns the ID number of the agent that is serving this call.
       * 
       * @return the ID number of the agent that is serving this call
       */
      public int getAgentID() { return agentID; }
   }
}
