package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.ssj.util.TimeUnit;

/**
 * Represents an object capable of returning
 * general information about a contact center.
 */
public interface ContactCenterInfo {

   /**
    * Returns the total number of contact types
    * supported by this contact center.
    * This should be the same as
    * {@link #getNumInContactTypes}\texttt{ + }{@link #getNumOutContactTypes}.
    @return the total number of contact types.
    */
   public int getNumContactTypes ();

   /**
    * Returns the total number of inbound contact types
    * for this contact center.
    @return the total number of inbound contact types.
    */
   public int getNumInContactTypes ();

   /**
    * Returns the total number of outbound contact types
    * for this contact center.
    @return the total number of outbound contact types.
    */
   public int getNumOutContactTypes ();

   /**
    * Returns the total number of agent groups
    * supported by this contact center.
    @return the total number of agent groups.
    */
   public int getNumAgentGroups ();

   /**
    * Returns the total number of waiting queues capable
    * of storing contacts.
    @return the number of waiting queues.
    */
   public int getNumWaitingQueues ();

   /**
    * Returns the number of main periods
    * used for evaluation, as defined
    * in {@link PeriodChangeEvent}.
    * For a steady-state evaluation on
    * a single period, this
    * always returns 1, even if
    * the model defines several period.
    @return the total number of main periods.
    */
   public int getNumMainPeriods ();

   /**
    * Returns the number of user-defined segments
    * regrouping contact types.
    * @return the number of segments regrouping
    * contact types.
    */
   public int getNumContactTypeSegments ();

   /**
    * Returns the number of user-defined segments
    * regrouping inbound contact types.
    * @return the number of segments regrouping
    * inbound contact types.
    */
   public int getNumInContactTypeSegments ();

   /**
    * Returns the number of user-defined segments
    * regrouping outbound contact types.
    * @return the number of segments regrouping
    * outbound contact types.
    */
   public int getNumOutContactTypeSegments ();

   /**
    * Returns the number of user-defined segments
    * regrouping agent groups.
    * @return the number of segments regrouping
    * agent groups.
    */
   public int getNumAgentGroupSegments ();

   /**
    * Returns the number of user-defined segments
    * regrouping main periods.
    * @return the number of segments regrouping
    * main periods.
    */
   public int getNumMainPeriodSegments ();

   /**
    * Returns the number of user-defined segments
    * regrouping waiting queues.
    *
    * The result of this method depends on the role
    * of the waiting queues, which depends on the
    * router's policy.
    * For example, if waiting queues correspond
    * to contact types, this returns
    * the result of
    * {@link #getNumContactTypeSegments()}.
    * @return the number of segments regrouping
    * waiting queues.
    */
   public int getNumWaitingQueueSegments ();

   /**
    * Returns the number of contact types including
    * segments regrouping several contact types.
    * If $K\le 1$, this returns the result of {@link #getNumContactTypes()}.
    * Otherwise, this returns the sum of {@link #getNumContactTypes()},
    * {@link #getNumContactTypeSegments()}, and 1.
    * @return the number of
    * contact types including segments.
    */
   public int getNumContactTypesWithSegments ();

   /**
    * Returns the number of inbound contact types including
    * segments regrouping several inbound contact types.
    * If $\Ki\le 1$, this returns the result of {@link #getNumInContactTypes()}.
    * Otherwise, this returns the sum of {@link #getNumInContactTypes()},
    * {@link #getNumInContactTypeSegments()}, and 1.
    * @return the number of
    * inbound contact types including segments.
    */
   public int getNumInContactTypesWithSegments ();

   /**
    * Returns the number of outbound contact types including
    * segments regrouping several outbound contact types.
    * If $\Ko\le 1$, this returns the result of {@link #getNumOutContactTypes()}.
    * Otherwise, this returns the sum of {@link #getNumOutContactTypes()},
    * {@link #getNumOutContactTypeSegments()}, and 1.
    * @return the number of
    * outbound contact types including segments.
    */
   public int getNumOutContactTypesWithSegments ();

   /**
    * Returns the number of agent groups including
    * segments regrouping several agent groups.
    * If $I\le 1$, this returns the result of {@link #getNumAgentGroups()}.
    * Otherwise, this returns the sum of {@link #getNumAgentGroups()},
    * {@link #getNumAgentGroupSegments()}, and 1.
    * @return the number of
    * agent groups including segments.
    */
   public int getNumAgentGroupsWithSegments ();

   /**
    * Returns the number of main periods including
    * segments regrouping several main periods.
    * If $P\le 1$, this returns the result of {@link #getNumMainPeriods()}.
    * Otherwise, this returns the sum of {@link #getNumMainPeriods()},
    * {@link #getNumMainPeriodSegments()}, and 1.
    * @return the number of
    * main periods including segments.
    */
   public int getNumMainPeriodsWithSegments ();

   /**
    * Returns the number of waiting queues including
    * segments regrouping several waiting queues.
    * If the number of waiting queues is
    * smaller than two, this returns the result of {@link #getNumWaitingQueues()}.
    * Otherwise, this returns the sum of {@link #getNumWaitingQueues()},
    * {@link #getNumWaitingQueueSegments()}, and 1.
    * @return the number of
    * waiting queues including segments.
    */
   public int getNumWaitingQueuesWithSegments ();

   /**
    * Returns the name associated with the contact
    * type \texttt{k}, where \texttt{k} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumContactTypes}. The first {@link #getNumInContactTypes}
    * indices are inbound contact types whereas the
    * remaining indices are outbound contact types.
    * If no contact type name is available, this
    * returns \texttt{null}.
    @param k the contact type identifier.
    @return the contact type name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the contact type identifier is negative
    or greater than or equal to {@link #getNumContactTypes}.
    */
   public String getContactTypeName (int k);

   /**
    * Returns the name associated with the agent group
    * identifier \texttt{i}. If no name is associated
    * with a given agent group, this returns \texttt{null}.
    @param i the identifier of the agent group.
    @return the agent group name, or \texttt{null}.
    @exception IndexOutOfBoundsException if
    the agent group identifier is negative or
    greater than or equal to {@link #getNumAgentGroups}.
    */
   public String getAgentGroupName (int i);

   /**
    * Returns the name corresponding to the 
    * main period \texttt{mp}.
    * This can return \texttt{null} or an
    * empty string for unnamed periods.
    * @param mp the index of the main period.
    * @return the name of the main period. 
    */
   public String getMainPeriodName (int mp);

   /**
    * Returns the name of the waiting queue with index
    * \texttt{q} used by the evaluation. If the waiting
    * queue has no name, returns \texttt{null}.
    @param q the index of the waiting queue.
    @return the name of the waiting queue.
    @exception IndexOutOfBoundsException if
    the waiting queue identifier is negative or
    greater than or equal to {@link #getNumWaitingQueues}.
    */
   public String getWaitingQueueName (int q);

   /**
    * Returns the name associated with the contact
    * type segment \texttt{k}, where \texttt{k} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumContactTypeSegments}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param k the contact type segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the contact type segment identifier is negative
    or greater than or equal to {@link #getNumContactTypeSegments}.
    */
   public String getContactTypeSegmentName (int k);

   /**
    * Returns the name associated with the inbound contact
    * type segment \texttt{k}, where \texttt{k} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumInContactTypeSegments}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param k the inbound contact type segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the identifier of the segment regrouping inbound contact
    types is negative
    or greater than or equal to {@link #getNumInContactTypeSegments}.
    */
   public String getInContactTypeSegmentName (int k);

   /**
    * Returns the name associated with the outbound contact
    * type segment \texttt{k}, where \texttt{k} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumOutContactTypeSegments}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param k the outbound contact type segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the identifier of the segment regrouping outbound contact
    types is negative
    or greater than or equal to {@link #getNumOutContactTypeSegments}.
    */
   public String getOutContactTypeSegmentName (int k);

   /**
    * Returns the name associated with the agent
    * group segment \texttt{i}, where \texttt{i} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumAgentGroupSegments}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param i the agent group segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the agent group segment identifier is negative
    or greater than or equal to {@link #getNumAgentGroupSegments}.
    */
   public String getAgentGroupSegmentName (int i);

   /**
    * Returns the name associated with the main
    * period segment \texttt{mp}, where \texttt{mp} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumMainPeriodSegments()}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param mp the main period segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the main period segment identifier is negative
    or greater than or equal to {@link #getNumMainPeriodSegments}.
    */
   public String getMainPeriodSegmentName (int mp);

   /**
    * Returns the name associated with the waiting
    * queue segment \texttt{q}, where \texttt{q} is a number
    * greater than or equal to 0 and smaller than
    * {@link #getNumMainPeriodSegments}.
    * If no segment name is available, this
    * returns \texttt{null}.
    @param q the waiting queue segment identifier.
    @return the segment name or \texttt{null} if
    no name is defined.
    @exception IndexOutOfBoundsException if
    the main period segment identifier is negative
    or greater than or equal to {@link #getNumMainPeriodSegments}.
    */
   public String getWaitingQueueSegmentName (int q);

   /**
    * Returns the name associated with the
    * matrix of AWTs with index \texttt{m}.
    * This method returns \texttt{null} if
    * no name is associated with the matrix.
    * This name can be used, e.g., to
    * give the AWT if the same AWT is
    * used for all contact types and periods.
    * @param m the index of the matrix of AWTs.
    * @return the name associated with the matrix.
    * @exception IllegalArgumentException if
    * \texttt{m} is negative or greater than
    * or equal to the value returned by
    * {@link #getNumMatricesOfAWT()}.
    */
   public String getMatrixOfAWTName (int m);

   /**
    * Returns the properties associated with contact
    * type \texttt{k}.
    * Properties are additional strings describing
    * a contact type.
    * This can include the language of the customers,
    * the originating region, etc.
    * If no property is defined for the given
    * contact type, this method returns an empty map.
    * @param k the contact type identifier.
    * @return the map of properties.
    @exception IndexOutOfBoundsException if
    the contact type identifier is negative
    or greater than or equal to {@link #getNumContactTypes}.
    */
   public Map<String, String> getContactTypeProperties (int k);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for agent groups instead of contact types.
    * @param i the agent group identifier.
    * @return the map of properties.
    */
   public Map<String, String> getAgentGroupProperties (int i);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for waiting queues instead of contact types.
    * @param q the waiting queue identifier.
    * @return the map of properties.
    */
   public Map<String, String> getWaitingQueueProperties (int q);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for contact type segments instead of contact types.
    * @param k the segment identifier.
    * @return the map of properties.
    */
   public Map<String, String> getContactTypeSegmentProperties (int k);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for inbound contact type segments instead of contact types.
    * @param k the segment identifier.
    * @return the map of properties.
    */
   public Map<String, String> getInContactTypeSegmentProperties (int k);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for outbound contact type segments instead of contact types.
    * @param k the segment identifier.
    * @return the map of properties.
    */
   public Map<String, String> getOutContactTypeSegmentProperties (int k);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for agent group segments instead of contact types.
    * @param i the segment identifier.
    * @return the map of properties.
    */
   public Map<String, String> getAgentGroupSegmentProperties (int i);

   /**
    * This method is similar to {@link #getContactTypeProperties(int)},
    * for waiting queue segments instead of contact types.
    * @param q the segment identifier.
    * @return the map of properties.
    */
   public Map<String, String> getWaitingQueueSegmentProperties (int q);

   /**
    * Returns the number of matrices
    * containing acceptable waiting times, for
    * estimating service levels.
    * Usually, this returns 1.
    * @return the number of matrices of acceptable waiting times.
    */
   public int getNumMatricesOfAWT ();

   /**
    * Returns the time unit in which output performance
    * measures representing times are expressed.
    * If this method returns \texttt{null}, performance
    * measures representing time are output as any
    * other performance measures; no time
    * conversion can be performed or time unit displayed.
    * @return the default time unit.
    */
   public TimeUnit getDefaultUnit ();
}
