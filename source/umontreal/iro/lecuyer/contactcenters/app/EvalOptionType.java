package umontreal.iro.lecuyer.contactcenters.app;

/**
 * Represents an evaluation option type for
 * a contact center evaluation system.
 * An evaluation option is an external parameter that can be changed
 * from evaluations to evaluations.  In particular, it can
 * be a decision variable or a simulation stopping condition.
 * An object of this class must be passed as a key
 * to {@link ContactCenterEval#setEvalOption} to select
 * which evaluation option to modify.
 */
public enum EvalOptionType {
   /**
    * Corresponds to an array of integers giving the number of agents in each
    * group and main period. For a single-period or stationary simulation, the
    * length of the array corresponds to $I$ and element $i$ gives the number of
    * agents in group $i$. For a multi-periods simulation, the length must be
    * $IP$, and for a period~$p$ and the agent group~$i$, the number of agents
    * is given by the element with index $Pi + p$. For example, in a simulation
    * with two agent groups and two main periods, the vector $\{1,2,3,4\}$ would
    * set the number of agents in group~0 to 1 for the first main period and 2
    * for the second one.
    */
   STAFFINGVECTOR (Messages.getString("EvalOptionType.StaffingVector"), int[].class), //$NON-NLS-1$

   /**
    * Corresponds to a 2D array of integers giving the number of agents
    * in each group and main period.
    * Element $(i, p)$ of this array gives the number of agents
    * in group $i$ during main period $p$.
    * If a single period is simulated as if it was infinite in the model,
    * the matrix has a single column.
    * Otherwise, it has $P$ columns.
    */
   STAFFINGMATRIX (Messages.getString ("EvalOptionType.StaffingMatrix"), int[][].class), //$NON-NLS-1$
   
   /**
    * Corresponds to a 2D array of integers giving the number of
    * agents in each shift for each agent group.
    * Element $(i,j)$ of the 2D array gives the number of agents
    * in shift $j$ for agent group $i$.
    * If agent group $i$ does not use a schedule,
    * the corresponding array in the 2D array is \texttt{null}.
    */
   SCHEDULEDAGENTS (Messages.getString ("EvalOptionType.ScheduledAgents"), int[][].class),
   
   /**
    * Corresponds to an integer giving the maximal capacity of the waiting queue.
    * An infinite capacity is represented by the value
    * {@link Integer#MAX_VALUE}.
    */
   QUEUECAPACITY (Messages.getString("EvalOptionType.QueueCapacity"), Integer.class),

   /**
    * Can be used to define an additionnal stopping condition for a simulation.
    * By default, a simulator stops the simulation when some conditions apply,
    * e.g., a fixed simulation length or a target relative error on some
    * predetermined performance measures. This option can be used to add a
    * user-defined stopping condition which will be checked in addition to the
    * default conditions. The value of this option can be \texttt{null} (the
    * default) or a reference to a {@link SimStoppingCondition} object.
    */
   SIMSTOPPINGCONDITION (Messages.getString("EvalOptionType.StoppingCondition"), SimStoppingCondition.class), //$NON-NLS-1$

   /**
    * This integer can be used to set the current period for a multi-period model evaluated
    * in a single period, as if this period length was infinite.
    */
   CURRENTPERIOD (Messages.getString("EvalOptionType.CurrentPeriod"), Integer.class); //$NON-NLS-1$

   private String name;
   private Class<?> type;

   /**
    * Constructs a new evaluation option with descriptive name \texttt{name} and
    * type \texttt{type}. The type of the option corresponds to the class of
    * objects that should be returned by {@link ContactCenterEval#getEvalOption}
    * or passed to {@link ContactCenterEval#setEvalOption}.
    * 
    * @param name
    *           the name of the evaluation option.
    * @param type
    *           the class of the objects representing the option values.
    */
   private EvalOptionType (String name, Class<?> type) {
      if (name == null)
         throw new NullPointerException ("name must not be null"); //$NON-NLS-1$
      if (type == null)
         throw new NullPointerException ("type must not be null"); //$NON-NLS-1$
      this.name = name;
      this.type = type;
   }

   /**
    * Returns the name of this evaluation option.
    * 
    * @return the name of the option.
    */
   public String getName () {
      return name;
   }

   /**
    * Returns the class of the objects representing values for this option.
    * 
    * @return the type of the option.
    */
   public Class<?> getType () {
      return type;
   }

   @Override
   public String toString () {
      final StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
      sb.append ('[');
      sb.append ("name: ").append (name); //$NON-NLS-1$
      sb.append (", type: ").append (type.getName ()); //$NON-NLS-1$
      sb.append (']');
      return sb.toString ();
   }
}
