package umontreal.iro.lecuyer.contactcenters.ctmc;


/**
 * Represents a continuous-time Markov chain (CTMC) modeling a call center with
 * possibly multiple call types and agent groups. 
 * More specifically, calls are divided into $K$
 * types, and are served by agents partitionned into $I$ groups. Each agent
 * group $i=0,\ldots,I-1$ has a skillset $S_i\subseteq\{1,\ldots,K\}$ giving the
 * types of the calls the agents can serve. Callers arriving when no agent is
 * available join a waiting queue, and can abandon if they do not receive
 * service fast enough.
 * 
 * For each $k=0,\ldots,K-1$,
 * we suppose that calls of type~$k$ arrive following a Poisson process
 * with rate $\lambda_k$.
 * Service times for calls of type~$k$ served by agents in group~$i$ are
 * i.i.d.\ exponential with rate $\mu_{k, i}$.
 * A call of type~$k$ not served immediately can balk (abandon immediately) with
 * probability $\rho_k$.
 * The patience times of callers of type~$k$ joining the queue without
 * balking are i.i.d.\ exponential with rate $\nu_k$.
 * Moreover, for each $i=0,\ldots,I-1$, agent group~$i$ contains $N_i$
 * agents.
 *
 * A router is used to assign agents to new calls, and queued calls to
 * free agents using either static lists, or matrices of priorities.
 * See {@link AgentGroupSelector}, and {@link WaitingQueueSelector} for
 * more information. 
 * If no agent group contains free agents, the call joins a waiting
 * queue specific to its type (and possibly balks).
 * Abandoning calls are lost.
 * 
 * This interface specifies methods to obtain information about the modelled
 * call center as well as methods to initialize the CTMC, and generate the next
 * state randomly from the current state.
 */
public interface CallCenterCTMC extends Cloneable {
   /**
    * Initializes the system to an empty call center, and resets the counter
    * giving the number of transitions done to 0.
    */
   public void initEmpty ();
   
   /**
    * Initializes the state of this CTMC with the
    * state of the other CTMC \texttt{ctmc}.
    * The parameters of this CTMC, e.g., arrival rates,
    * service rates, etc., are unchanged while
    * the state is set to the state of the given CTMC.
    * This method throws an {@link IllegalArgumentException}
    * if the given CTMC is incompatible with this CTMC, e.g.,
    * the number of contact types or agent groups differ.
    * @param ctmc the CTMC to initialize the state from.
    * @exception IllegalArgumentException if the given CTMC is
    * not compatible with this CTMC.
    */
   public void init (CallCenterCTMC ctmc);

   /**
    * Returns the number of contact types used in the modelled call center.
    * 
    * @return the number of contact types.
    */
   public int getNumContactTypes ();

   /**
    * Returns the number of agent groups used in the modelled call center.
    * 
    * @return the number of agent groups.
    */
   public int getNumAgentGroups ();

   /**
    * Returns the total number of contacts currently waiting in queue.
    * 
    * @return the total number of queued contacts.
    */
   public int getNumContactsInQueue ();

   /**
    * Returns the total number of contacts currently served by agents.
    * 
    * @return the total number of contacts in service.
    */
   public int getNumContactsInService ();

   /**
    * Returns the number of contacts of type \texttt{k} currently waiting in
    * queue.
    * 
    * @param k
    *           the tested contact type.
    * @return the number of contacts waiting in queue.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public int getNumContactsInQueue (int k);

   /**
    * Returns the number of contacts of type \texttt{k} currently in service.
    * 
    * @param k
    *           the tested contact type.
    * @return the number of contacts in service.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public int getNumContactsInServiceK (int k);

   /**
    * Returns the number of contacts currently in service by agents in group
    * \texttt{i}.
    * 
    * @param i
    *           the tested agent group.
    * @return the number of contacts in service.
    * @exception IllegalArgumentException
    *               if \texttt{i} is negative or greater than or equal to the
    *               result of {@link #getNumAgentGroups()}.
    */
   public int getNumContactsInServiceI (int i);

   /**
    * Returns the number of contacts of type \texttt{k} in service by agents in
    * group \texttt{i}.
    * 
    * @param k
    *           the tested contact type.
    * @param i
    *           the tested agent group.
    * @return the number of contacts in service.
    */
   public int getNumContactsInService (int k, int i);
   
   /**
    * Returns the maximal queue capacity.
    * @return the maximal queue capacity.
    */
   public int getQueueCapacity();
   
   /**
    * Sets the capacity of the waiting queue to
    * \texttt{q}.
    * @param q the new queue capacity.
    * @exception IllegalArgumentException if
    * the new capacity is smaller than the current
    * number of contacts in queue, returned
    * by {@link #getNumContactsInQueue()}, or larger
    * than the maximum returned by {@link #getMaxQueueCapacity()}.
    */
   public void setQueueCapacity (int q);
   
   /**
    * Returns the current bound on the queue capacity
    * used to determine the transition rate of the CTMC.
    * @return the bound on the queue capacity.
    */
   public int getMaxQueueCapacity();
   
   /**
    * Sets the bound on the queue capacity to \texttt{q}.
    * Note that this method can change the transition rate, which
    * usually involves recreating search indexes.
    * Calling this method too often can thus decrease performance;
    * one should use {@link #setQueueCapacity(int)}
    * instead.
    * @param q the new maximal queue capacity.
    * @exception IllegalArgumentException if \texttt{q}
    * is smaller than the queue capacity returned by
    * {@link #getQueueCapacity()}.
    */
   public void setMaxQueueCapacity (int q);
   
   /**
    * Returns the number of thresholds on the state space.
    * When the queue size
    * or the number of busy agents are small enough, the CTMC simulator can use a smaller
    * transition rate, and generate a random number of successive false transitions before
    * every transition.  Since multiple transitions are generated using a single
    * random number, this can save CPU time.
    * This method returns the total number of transition rates the simulator can
    * use depending on the queue size, and number of busy agents. 
    * Note that using too many vectors of thresholds can increase memory usage,
    * because a separate search index is required for each vector of threshold.
    * @return the number of state thresholds.
    */
   public int getNumStateThresh();
   
   /**
    * Returns the $R\times I+1$ matrix of thresholds whose
    * used for state space partitioning. 
    * Each row of the matrix corresponds to a vector of thresholds,
    * column $i=0,\ldots,I-1$ corresponds to 
    * thresholds on the number of agents in group $i$, and
    * the last column corresponds to the
    * queue size. 
    * @return the matrix of thresholds.
    */
   public int[][] getStateThresholds();
   
   /**
    * Sets the matrix of thresholds for this CTMC
    * to \texttt{thresholds}.
    * @param thresholds the matrix of thresholds.
    * @exception NullPointerException if \texttt{thresholds} is
    * \texttt{null}.
    * @exception IllegalArgumentException if \texttt{thresholds}
    * has invalid dimensions.
    */
   public void setStateThresholds (int[][] thresholds);
   
   /**
    * Returns the thresholds on the state of the CTMC.
    * @return the state thresholds.
    */
   public StateThresh getStateThresh();

   /**
    * Returns the arrival rate $\lambda_k$ for contacts of type \texttt{k}.
    * 
    * @param k
    *           the tested contact type.
    * @return the arrival rate.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public double getArrivalRate (int k);
   
   /**
    * Sets the arrival rate for contacts of type \texttt{k}
    * to \texttt{rate}.
    * @param k the contact type.
    * @param rate the arrival rate.
    * @exception IllegalArgumentException if \texttt{rate} is
    * negative, or greater than the bound
    * returned by {@link #getMaxArrivalRate(int)}.
    */
   public void setArrivalRate (int k, double rate);
   
   /**
    * Sets the arrival rate for each contact type
    * \texttt{k} to \texttt{rates[k]}.
    * @param rates the arrival rates.
    * @exception IllegalArgumentException if \texttt{rates} does
    * not have length $K$, contains at least one negative element,
    * or if \texttt{rates[k]} is greater than the result of
    * {@link #getMaxArrivalRate(int) get\-Max\-Arrival\-Rate} \texttt{(k)}
    * for at least one \texttt{k}.
    */
   public void setArrivalRates (double[] rates);

   /**
    * Returns the total arrival rate $\lambda=\sum_{k=0}^{K-1}\lambda_k$ for all
    * contact types.
    * 
    * @return the total arrival rate.
    */
   public double getArrivalRate ();
   
   /**
    * Returns the maximal arrival rate $\tilde\lambda_k$ for contacts of type \texttt{k}.
    * 
    * @param k
    *           the tested contact type.
    * @return the maximal arrival rate.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public double getMaxArrivalRate (int k);
   
   /**
    * Sets the maximal arrival rate for contacts of type \texttt{k}
    * to \texttt{rate}.
    * This can change the transition rate, and force
    * the simulator to recompute some search indexes.
    * Using this method often can therefore degrade performance,
    * so it is recommended to call {@link #setArrivalRate(int, double)}
    * instead.
    * @param k the contact type.
    * @param rate the arrival rate.
    * @exception IllegalArgumentException if \texttt{rate} is
    * smaller than the rate returned by {@link #getArrivalRate(int)}.
    */
   public void setMaxArrivalRate (int k, double rate);
   
   /**
    * Sets the maximal arrival rate for each contact type
    * \texttt{k} to \texttt{rates[k]}.
    * @param rates the arrival rates.
    * @exception IllegalArgumentException if \texttt{rates} does
    * not have length $K$, orif \texttt{rates[k]} is smaller than the result of
    * {@link #getArrivalRate(int) get\-Arrival\-Rate} \texttt{(k)}
    * for at least one \texttt{k}.
    */
   public void setMaxArrivalRates (double[] rates);

   /**
    * Returns the total maximal arrival rate $\tilde\lambda=\sum_{k=0}^{K-1}\tilde\lambda_k$ for all
    * contact types.
    * 
    * @return the total maximal arrival rate.
    */
   public double getMaxArrivalRate ();
   

   /**
    * Returns the probability of balking $\rho_k$ for contacts of type
    * \texttt{k}.
    * 
    * @param k
    *           the tested contact type.
    * @return the probability of balking.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public double getProbBalking (int k);
   
   /**
    * Sets the balking probability to $\rho_k$ for contacts of
    * type \texttt{k} to \texttt{rhok}.
    * @param k the affected contact type.
    * @param rhok the new value of $\rho_k$.
    * @exception IllegalArgumentException if \texttt{rhok}
    * is negative or greater than 1.
    */
   public void setProbBalking (int k, double rhok);

   /**
    * Returns the patience rate $\nu_k$ for contacts of type \texttt{k}.
    * 
    * @param k
    *           the tested contact type.
    * @return the patience rate.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public double getPatienceRate (int k);
   
   /**
    * Sets the patience rate $\nu_k$ for contacts of type \texttt{k}
    * to \texttt{nuk}.
    * @param k the affected contact type.
    * @param nuk the new value of $\nu_k$.
    * @exception IllegalArgumentException if \texttt{k} is out of range,
    * \texttt{nuk} is negative, or
    * \texttt{nuk} is greater than the maximum
    * returned by {@link #getMaxPatienceRate(int)}.
    */
   public void setPatienceRate (int k, double nuk);
   
   /**
    * Returns the maximal patience rate $\tilde\nu_k$
    * for contacts of type \texttt{k}.
    * @param k the tested contact type.
    * @return the maximal patience rate.
    * @exception IllegalArgumentException
    *               if \texttt{k} is negative or greater than or equal to the
    *               result of {@link #getNumContactTypes()}.
    */
   public double getMaxPatienceRate (int k);
   
   /**
    * Sets the maximal patience rate $\tilde\nu_k$
    * for contacts of type \texttt{k} to \texttt{nuk}.
    * This method can change the transition rate and
    * recompute search indexes so using
    * it repeatedly might degrade performance.
    * It is recommended to use {@link #setPatienceRate(int,double)}
    * instead.
    * @param k the affected contact type.
    * @param nuk the new patience rate.
    * @exception IllegalArgumentException if \texttt{nuk}
    * is smaller than the result of {@link #getPatienceRate(int)},
    * or if \texttt{k} is out of range.
    */
   public void setMaxPatienceRate (int k, double nuk);

   /**
    * Returns the service rate $\mu_{k,i}$ for contacts of type \texttt{k}
    * served by agents in group \texttt{i}.
    * 
    * @param k
    *           the tested contact type.
    * @param i
    *           the tested agent group.
    * @return the service rate.
    * @exception IllegalArgumentException if \texttt{k} or
    * \texttt{i} are out of range.
    */
   public double getServiceRate (int k, int i);
   
   /**
    * Sets the service rate $\mu_{k,i}$ to
    * for contacts of type \texttt{k} served
    * by agents in group \texttt{i}
    * to \texttt{muki}. 
    * @param k the affected contact type.
    * @param i the affected agent group.
    * @param muki the new service rate.
    * @exception IllegalArgumentException if \texttt{k} or
    * \texttt{i} are out of range, if \texttt{muki} is
    * negative, or if \texttt{muki} is greater than
    * the maximum returned by {@link #getMaxServiceRate(int,int)}.
    */
   public void setServiceRate (int k, int i, double muki);
   
   /**
    * Returns the maximal service rate $\tilde\mu_{k,i}$
    * for contacts of type \texttt{k} served by agents in group \texttt{i}.
    * @param k the tested contact type.
    * @param i the tested agent group.
    * @return the service rate.
    * @exception IllegalArgumentException if \texttt{k} or
    * \texttt{i} are out of range.
    */
   public double getMaxServiceRate (int k, int i);
   
   /**
    * Sets the maximal service rate for contacts of type
    * \texttt{k} served by agents in group \texttt{i}
    * to \texttt{muki}.
    * This method can change the transition and recompute
    * search indexes, so using it repeatedly might
    * degrade performance.
    * It is recommended to use {@link #setServiceRate(int,int,double)}
    * instead.
    * @param k the affected contact type.
    * @param i the affected agent group.
    * @param muki the new maximal service rate.
    * @exception IllegalArgumentException if \texttt{k} or
    * \texttt{i} are out of range, or if \texttt{muki}
    * is smaller than the service rate returned
    * by {@link #getServiceRate(int,int)}.
    */
   public void setMaxServiceRate (int k, int i, double muki);

   /**
    * Returns the uniformized transition rate used by this CTMC.
    * 
    * @return the uniformized transition rate.
    */
   public double getJumpRate ();

   /**
    * Returns the total number of agents available for serving contacts.
    * 
    * @return the total number of agents.
    */
   public int getNumAgents ();
   
   /**
    * Returns the maximal total number of agents
    * that can be used for the CTMC.
    * @return the maximal number of agents.
    */
   public int getMaxNumAgents();

   /**
    * Returns the number of agents in group \texttt{i}.
    * 
    * @param i
    *           the tested agent group.
    * @return the number of agents.
    * @exception IllegalArgumentException
    *               if \texttt{i} is negative or greater than or equal to the
    *               result of {@link #getNumAgentGroups()}.
    */
   public int getNumAgents (int i);
   
   /**
    * Returns an array of length $I$ containing the number of agents in
    * each agent group.
    * @return the number of agents in each group.
    */
   public int[] getNumAgentsArray();
   
   /**
    * Returns the maximal number of agents in group \texttt{i}.
    * @param i the tested agent group.
    * @return the maximal number of agents.
    */
   public int getMaxNumAgents (int i);
   
   /**
    * Returns an array of length $I$ containing the
    * maximal number of agents in each agent group.
    * @return the maximal number of agents in each group.
    */
   public int[] getMaxNumAgentsArray();

   /**
    * Sets the number of agents in group \texttt{i} to \texttt{n}. This method
    * might cause the transition rate to increase so it should never be called
    * during the simulation of the CTMC.
    * 
    * @param i
    *           the tested agent group.
    * @param n
    *           the new number of agents in the group.
    * @exception IllegalArgumentException
    *               if \texttt{i} is negative or greater than or equal to the
    *               result of {@link #getNumAgentGroups()}, or if \texttt{n} is
    *               negative.
    */
   public void setNumAgents (int i, int n);
   
   /**
    * For each agent group $i=0,\ldots,I-1$, sets the number
    * of agents in group $i$ to \texttt{numAgents[i]}.
    * @param numAgents the array containing the number of agents.
    * @exception IllegalArgumentException if \texttt{numAgents} has
    * a length different from $I$, or if it contains at least one
    * negative value.
    */
   public void setNumAgents (int[] numAgents);
   
   /**
    * Sets the maximal number of agents in group \texttt{i} to \texttt{n}. This method
    * might cause the maximal transition rate
    * returned by {@link #getJumpRate()}
    * to increase so it should never be called
    * during the simulation of the CTMC.
    * 
    * @param i
    *           the tested agent group.
    * @param n
    *           the new maximal number of agents in the group.
    * @exception IllegalArgumentException
    *               if \texttt{i} is negative or greater than or equal to the
    *               result of {@link #getNumAgentGroups()}, or if \texttt{n} is
    *               negative.
    */
   public void setMaxNumAgents (int i, int n);
   
   /**
    * For each agent group $i=0,\ldots,I-1$, sets the maximal number
    * of agents in group $i$ to \texttt{numAgents[i]}.
    * @param numAgents the array containing the number of agents.
    * @exception IllegalArgumentException if \texttt{numAgents} has
    * a length different from $I$, or if it contains at least one
    * negative value.
    */
   public void setMaxNumAgents (int[] numAgents);
   
   /**
    * Returns the type-to-group matrix of ranks
    * associating a priority to each contact
    * type and agent group when selecting an agent
    * group for a new arrival.
    * @return the matrix of ranks being used.
    */
   public double[][] getRanksTG();
   
   /**
    * Returns the group-to-type matrix of ranks
    * associating a priority to each agent group
    * and contact type when selecting a waiting
    * queue for a free agent.
    * @return the matrix of ranks being used.
    */
   public double[][] getRanksGT();

   /**
    * Returns the type of the last transition,
    * or \texttt{null} if no transition occurred
    * since the last call to
    * {@link #initEmpty()}.
    * @return the type of the last transition.
    */
   public TransitionType getLastTransitionType();
   
   /**
    * Returns the last contact type selected by the
    * {@link #nextState(double)} method.
    * 
    * @return the last selected contact type.
    */
   public int getLastSelectedContactType ();
   
   /**
    * Returns the index of the last selected contact having abandoned.
    * This returns the position of the contact having abandoned
    * within a queue
    * containing contacts of type $k$ only.
    * The returned value is thus in $0,\ldots,Q_k-1$ where
    * $k$ is the result of {@link #getLastSelectedContactType()}, and
    * $Q_k$ is the number of contacts of type $k$ in queue. 
    * @return the last selected contact.
    */
   public int getLastSelectedContact();

   /**
    * Returns the last agent group selected by the
    * {@link #nextState(double)} method.
    * 
    * @return the last selected agent group.
    */
   public int getLastSelectedAgentGroup ();

   /**
    * Returns the type of the last contact removed from a waiting queue for
    * service by the {@link #nextState(double)} method.
    * 
    * @return the last selected queued contact type.
    */
   public int getLastSelectedQueuedContactType ();
   
   /**
    * Generates the next state of the CTMC randomly from the current state,
    * using the given uniform \texttt{u}, and changes the current state to
    * this new state. The method then returns the type of transition being
    * generated. Depending on the transition type, additional information about
    * the selected contact type or agent group can be obtained using
    * {@link #getLastSelectedContactType()},
    * {@link #getLastSelectedQueuedContactType()}, or
    * {@link #getLastSelectedAgentGroup()}.
    * 
    * @param u
    *           the uniform used to generate the new state.
    * @return the type of the generated transition.
    */
   public TransitionType nextState (double u);
   
   /**
    * Similar to {@link #nextState(double)}, except that
    * the given random variate \texttt{v} is uniformly distributed
    * over $[0,2^{31} - 1]$.  
    * @param v the uniform random integer.
    * @return the type of the generated transition.
    */
   public TransitionType nextStateInt (int v);

   /**
    * Returns the type of the next transition generated
    * using the random number \texttt{u}.
    * This method is similar to {@link #nextState(double)},
    * except that it does not alter the state of the CTMC.
    * @param u the random number for the state generation.
    * @return the type of the next transition.
    */
   public TransitionType getNextTransition (double u);
   
   /**
    * Similar to {@link #getNextTransition(double)},
    * using a random integer rather than a uniform
    * number.
    * @param u the random number used for generating the transition.
    * @return the type of the next transition.
    */
   public TransitionType getNextTransitionInt (int u);
   
   /**
    * Generates the arrival of a contact of type \texttt{k} served
    * by an agent in group \texttt{i}.
    * If all agents are busy in group \texttt{i}, this method
    * throws an {@link IllegalStateException}.
    * @param k the contact type.
    * @param i the agent group.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    * @exception IllegalStateException if all agents in group \texttt{i}
    * are busy.
    */
   public void generateArrivalServed (int k, int i, int np, int nf);
   
   /**
    * Generates the arrival of a contact of type \texttt{k}, and
    * adds the new contact to the waiting queue.
    * This method throws an {@link IllegalStateException} if
    * the queue is full before the arrival.
    * @param k the type of the new contact.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    * @exception IllegalStateException if the queue capacity is
    * exceeded.
    */
   public void generateArrivalQueued (int k, int np, int nf);
   
   /**
    * Generates the arrival of a contact of type \texttt{k}
    * being blocked or balking.
    * @param k the type of the arrival.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    */
   public void generateArrival (int k, int np, int nf);
   
   /**
    * Generates the end of the service for a contact of type \texttt{k}
    * served by an agent in group \texttt{i}.
    * If no contact of type \texttt{k} are in service by agents in group
    * \texttt{i}, this method throws an
    * {@link IllegalStateException}.
    * @param k the type of the contact.
    * @param i the group of the agent.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    * @exception IllegalStateException if no contact of type \texttt{k} is
    * in service by agents in group \texttt{i}.
    */
   public void generateEndService (int k, int i, int np, int nf);
   
   /**
    * Generates the end of the service for a contact of type \texttt{k}
    * served by an agent in group \texttt{i}, and
    * assigns the \texttt{kpos}th queued contact of type \texttt{kp}
    * to the free agent.
    * If no contact of type \texttt{k} are in service by agents in group
    * \texttt{i},
    * this method throws an
    * {@link IllegalStateException}.
    * 
    * @param k the type of the contact ending service.
    * @param i the group of the agent ending service.
    * @param kp the type of the dequeued contact.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    */
   public void generateEndService (int k, int i, int kp, int np, int nf);
   
   /**
    * Generates the abandonment of the \texttt{kpos}th
    * contact of type \texttt{k}.
    * This method throws an {@link IllegalStateException}
    * if \texttt{kpos} is negative or greater than or equal
    * to the number of queued contacts of type \texttt{k}.
    * @param k the contact type.
    * @param kpos the position of the contact in queue.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    */
   public void generateAbandonment (int k, int kpos, int np, int nf);
   
   /**
    * Selects a new queued contact for a free agent in
    * group \texttt{i}, and returns a boolean indicator
    * determining if a contact could be selected.
    * After this method returns \texttt{true}, the
    * method {@link #getLastSelectedAgentGroup()}
    * returns the value of \texttt{i} while
    * {@link #getLastSelectedQueuedContactType()}
    * returns the type of the contact assigned
    * to the free agent.
    * This method can be used, e.g., when agents
    * are added in some groups during a simulation. 
    * @param i the agent group index.
    * @return determines whether a contact is removed
    * from a queue.
    */
   public boolean selectContact (int i);
   
   /**
    * Generates a false transition. This method only updates
    * the transition counter of the CTMC.
    * @param np the number of false transitions preceding the main
    * transition.
    * @param nf the number of false transitions following the main
    * transition.
    */
   public void generateFalseTransition(int np, int nf);
   
   /**
    * Returns the number of additionnal false transitions
    * generated by the last call to {@link #nextState(double)}
    * before the main transition.
    * Some implementation of this interface might
    * generate several false transitions using a single
    * random number. In this case, the call to
    * {@link #nextState(double)} will return a transition type while
    * this method should be used to obtain the number of
    * additional false transitions that were generated
    * before the main transition.
    * @return the number of additional false transitions
    * generated by the last call to {@link #nextState(double)}
    * before the main transition.
    */
   public int getNumPrecedingFalseTransitions();
   
   /**
    * Similar to {@link #getNumPrecedingFalseTransitions()}, but for
    * the number of false transitions generated after the
    * main transition.
    * @return the number of additional false transitions
    * generated by the last call to {@link #nextState(double)}
    * after the main transition.
    */
   public int getNumFollowingFalseTransitions();

   /**
    * Returns the number of generated transitions. This corresponds to the
    * number of times the {@link #nextState(double)} method was called
    * since the last call to {@link #initEmpty()}.
    * 
    * @return the number of transitions done.
    */
   public int getNumTransitionsDone ();
   
   /**
    * Returns an independent copy of this call center CTMC.
    * In particular, calling {@link #nextState(double)}
    * on the returned CTMC should not affect the state of any other
    * CTMC. 
    * @return the clone of the chain.
    */
   public CallCenterCTMC clone();
   
   /**
    * Computes and returns a hash code using
    * the current state of the CTMC.
    * The returned hash code should be
    * $\sum_{i=1}^IK_i\sum_{k=1}^KS_{k,i} + K_{I+1}\sum_{k=1}^KQ_k+K_{I+2}n$
    * where $K_i = \prod_{j=1}^{i-1}\tilde N_j$ for $i=1,\ldots,I+1$, and
    * $K_{I+1}=K_{I+1}H$.
    * @return the computed hash code.
    */
   public int hashCodeState();
   
   /**
    * Determines if the state of this CTMC is the
    * same as the state of the CTMC \texttt{o}.
    * If \texttt{o} does not correspond to an
    * instance of \texttt{CallCenterCTMC},
    * this method should return \texttt{false}. 
    * @param o the object to test.
    * @return the result of the equality test.
    */
   public boolean equalsState (Object o);
   
   /**
    * Returns the current target number of transitions.
    * Sometimes, {@link #nextState(double)}
    * or {@link #nextStateInt(int)}
    * generate several false transitions with
    * a single random number.
    * When the number of transitions done is
    * near the target number of transitions,
    * the final number of transitions may then
    * exceed the target.
    * 
    * When a target number of transition is
    * specified, no more state change occur
    * after the target is reach.
    * Moreover,
    * when the return value of
    * {@link #getNumTransitionsDone()}
    * is greater than or equal to the
    * value returned by this method,
    * {@link #nextState(double)}, and
    * {@link #nextStateInt(int)}
    * throw an {@link IllegalStateException}.
    * 
    *  The default target number of
    *  transitions is {@link Integer#MAX_VALUE}.
    * @return the current target number of transitions.
    */
   public int getTargetNumTransitions();
   
   /**
    * Sets the target number of transitions
    * to \texttt{tntr}.
    * @param tntr the new target number of transitions.
    * @exception IllegalArgumentException if \texttt{tntr}
    * is negative.
    */
   public void setTargetNumTransitions (int tntr);
}
