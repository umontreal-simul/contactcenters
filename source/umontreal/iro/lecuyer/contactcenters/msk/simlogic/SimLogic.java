package umontreal.iro.lecuyer.contactcenters.msk.simlogic;

import java.util.List;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.EvalOptionType;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterMeasureManager;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;
import umontreal.iro.lecuyer.contactcenters.msk.stat.StatPeriod;

/**
 * Represents a simulation logic performing a certain
 * type of experiment on a model of a call center.
 * This interface defines methods to perform
 * simulations,
 * obtain the statistical period of contacts,
 * transform matrices of counters into matrices of observations
 * ready to be added to statistical collectors,
 * and update some simulation parameters.
 */
public interface SimLogic extends StatPeriod {
   /**
    * Returns the model associated with this simulation logic.
    * 
    * @return the associated model.
    */
   public CallCenter getCallCenter ();

   /**
    * Returns the parameters associated with this simulation logic.
    * 
    * @return the associated parameters.
    */
   public SimParams getSimParams ();

   /**
    * Returns an object containing the counters updated throughout the simulation.
    * 
    * @return the call center measures.
    */
   public CallCenterMeasureManager getCallCenterMeasureManager ();

   /**
    * Returns the call center statistical probes used by this simulation logic.
    * 
    * @return the call center statistical probes.
    */
   public CallCenterStatProbes getCallCenterStatProbes ();

   /**
    * Resets the simulation logic for a new experiment after the model has been reset.
    * This method should update or recreate the associated counters and statistical probes, since
    * the size of the model may have changed.
    */
   public void reset (PerformanceMeasureType... pms);
   
   /**
    * Initializes the simulation logic for a new
    * experiment.
    * In particular,
    * this resets the event list of the simulator,
    * the state of the model, and the
    * current number of completed steps to 0.
    */
   public void init();

   /**
    * Simulates \texttt{numSteps} steps, and
    * updates observations in statistical collectors
    * as well as the number of completed steps returned by
    * {@link #getCompletedSteps()}.
    * Usually, this method simulates the required number
    * of replications, and adds one observation to
    * each statistical collector of the matrices returned
    * by {@link #getCallCenterStatProbes()}.
    * 
    * Note that this method may be called several times
    * during a simulation experiment using sequential sampling.
    * For this reason, one should take account of every
    * observation collected since the last call to {@link #init()}. 
    */
   public void simulate (int numSteps);

   /**
    * Returns the number of completed simulation steps.
    * 
    * @return the number of completed steps.
    */
   public int getCompletedSteps ();
   
   /**
    * Determines if this simulator performs a steady-state simulation.
    * 
    * @return \texttt{true} if this is a steady-state simulator, \texttt{false}
    *         otherwise.
    */
   public boolean isSteadyState ();

   /**
    * Adds the information specific to this
    * simulation logic into
    * the evaluation information map
    * of the simulator.
    * The keys and values of this map are listed at
    * the beginning of the simulation report.
    */
   public void formatReport (Map<String, Object> evalInfo);
   
   /**
    * Returns the staffing vector used by this simulator. This vector has the
    * same format as the {@link EvalOptionType#STAFFINGVECTOR} evaluation
    * option.
    * 
    * @return the staffing vector.
    */
   public int[] getStaffing ();

   /**
    * Sets the staffing vector used by this simulator to \texttt{staffing}. This
    * vector has the same format as the {@link EvalOptionType#STAFFINGVECTOR}
    * evaluation option.
    * 
    * @param staffing
    *           the new staffing vector.
    */
   public void setStaffing (int[] staffing);
   
   /**
    * Gets the staffing matrix for the simulated
    * model. The returned 2D array has the format
    * specified by {@link EvalOptionType#STAFFINGMATRIX}.
    * @return the 2D array representing the staffing matrix.
    */
   public int[][] getStaffingMatrix();
   
   /**
    * Sets the 2D array representing the staffing matrix
    * to \texttt{staffing}.
    * @param staffing the new staffing matrix.
    */
   public void setStaffingMatrix (int[][] staffing);
   
   /**
    * Returns the 2D array of scheduled agents for
    * each shift and each agent group.
    * Element $(i,j)$ of the returned array contains
    * the number of agents scheduled in group $i$
    * during shift $j$. 
    * @return the scheduled agents.
    */
   public int[][] getScheduledAgents();
   
   /**
    * Sets the number of scheduled agents for each
    * group and shift using the given 2D array.
    * @param ag the array of scheduled agents.
    */
   public void setScheduledAgents (int[][] ag);

   /**
    * Returns the current period used by this simulator. If this simulator is
    * not steady-state, this throws an {@link UnsupportedOperationException}.
    * 
    * @return the current period.
    */
   public int getCurrentMainPeriod ();

   /**
    * Sets the current period for this simulator to \texttt{mp}. If this
    * simulator is not steady-state, this throws an
    * {@link UnsupportedOperationException}.
    * 
    * @param mp
    *           the new current period.
    */
   public void setCurrentMainPeriod (int mp);

   /**
    * Returns \texttt{true} if, after the simulation, the system seems unstable.
    * This is applicable for steady state simulations only.
    * 
    * @return the result of the stability check.
    */
   public boolean seemsUnstable ();

   /**
    * Registers any listener required by the simulator from the model.
    */
   public void registerListeners ();

   /**
    * Disconnects every listener registered by the simulator from the model.
    */
   public void unregisterListeners ();

   /**
    * Determines if the simulation logic
    * is in verbose mode.
    * @return the status of the verbose mode.
    */
   public boolean isVerbose ();

   /**
    * Sets the verbose indicator
    * to \texttt{verbose}.
    * @param verbose the value of the indicator.
    */
   public void setVerbose (boolean verbose);

   /**
    * Registers the listener \texttt{l} to be notified about the progress of the
    * simulator.
    * 
    * @param l
    *           the listener to be notified.
    * @exception NullPointerException
    *               if \texttt{l} is \texttt{null}.
    */
   public void addSimLogicListener (SimLogicListener l);

   /**
    * Removes the listener \texttt{l} from the list of listeners registered with
    * this simulator.
    * 
    * @param l
    *           the listener being removed.
    */
   public void removeSimLogicListener (SimLogicListener l);

   /**
    * Removes all the listeners registered with this simulator.
    */
   public void clearSimLogicListeners ();

   /**
    * Returns the listeners registered with this simulator.
    * 
    * @return the list of registered listeners.
    */
   public List<SimLogicListener> getSimLogicListeners ();

   /**
    * Determines if the simulation has been aborted by using the
    * {@link #setAborted} method.
    * 
    * @return \texttt{true} if the simulation was aborted, \texttt{false}
    *         otherwise.
    */
   public boolean isAborted ();

   /**
    * Aborts the current simulation.
    */
   public void setAborted (boolean aborted);
}
