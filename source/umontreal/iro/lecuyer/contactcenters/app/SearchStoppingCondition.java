package umontreal.iro.lecuyer.contactcenters.app;

import umontreal.ssj.stat.FunctionOfMultipleMeansTally;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;

/**
 * Early stopping condition allowing to perform a first cut when using
 * neighborhood search. When checked, this condition computes a confidence
 * interval on the aggregate value of a given performance measure and the
 * simulation exits when a threshold value $\delta$ falls outside the confidence
 * interval with confidence level $\beta$. The simulation also stops when a
 * certain number of batches or replications is reached or when the default
 * stopping condition of the simulator is satisfied. If the stopping condition
 * fails, only one additional batch or replication is performed before the
 * condition is checked again.
 */
public class SearchStoppingCondition implements SimStoppingCondition {
   private double beta;
   private double delta;
   private PerformanceMeasureType pm;
   private int maxReps = Integer.MAX_VALUE;
   private final double[] cl = new double[2];

   /**
    * Constructs a new search stopping condition with confidence level $\beta$,
    * threshold value $\delta$, on performance measure type \texttt{pm} and with
    * a maximal number of replications or batches \texttt{maxReps}.
    * 
    * @param beta
    *           the confidence level of the confidence intervals.
    * @param delta
    *           the threshold value.
    * @param pm
    *           the target performance measure.
    * @param maxReps
    *           the maximal number of replications or batches.
    * @exception IllegalArgumentException
    *               if $\beta$ is not in $(0, 1)$ or \texttt{maxReps} is
    *               negative.
    * @exception NullPointerException
    *               if \texttt{pm} is \texttt{null}.
    */
   public SearchStoppingCondition (double beta, double delta,
         PerformanceMeasureType pm, int maxReps) {
      if (beta <= 0 || beta >= 1)
         throw new IllegalArgumentException ("beta must be in (0, 1)");
      if (pm == null)
         throw new NullPointerException ("pm must not be null");
      if (maxReps < 0)
         throw new IllegalArgumentException ("maxReps < 0");
      this.beta = beta;
      this.delta = delta;
      this.pm = pm;
      this.maxReps = maxReps;
   }

   /**
    * Returns the $\beta$ confidence level.
    * 
    * @return the confidence level.
    */
   public double getBeta () {
      return beta;
   }

   /**
    * Sets the $\beta$ confidence level to \texttt{beta}.
    * 
    * @param beta
    *           the new confidence level.
    * @exception IllegalArgumentException
    *               if \texttt{beta} is not in $(0, 1)$.
    */
   public void setBeta (double beta) {
      if (beta <= 0 || beta >= 1)
         throw new IllegalArgumentException ("beta must be in (0, 1)");
      this.beta = beta;
   }

   /**
    * Returns the treshold value $\delta$.
    * 
    * @return the threshold value.
    */
   public double getDelta () {
      return delta;
   }

   /**
    * Sets the treshold value $\delta$ to \texttt{delta}.
    * 
    * @param delta
    *           the new threshold value.
    */
   public void setDelta (double delta) {
      this.delta = delta;
   }

   /**
    * Returns the maximal number of replications or batches to simulate if the
    * stopping condition does not apply.
    * 
    * @return the maximal number of replications or batches.
    */
   public int getMaxReplications () {
      return maxReps;
   }

   /**
    * Sets the maximal number of replications or batches to \texttt{maxReps}.
    * 
    * @param maxReps
    *           the new maximal number of replications or batches.
    * @exception IllegalArgumentException
    *               if \texttt{maxReps} is negative.
    */
   public void setMaxReplications (int maxReps) {
      if (maxReps < 0)
         throw new IllegalArgumentException ("maxReps < 0");
      this.maxReps = maxReps;
   }

   /**
    * Returns the checked performance measure.
    * 
    * @return the checked performance measure.
    */
   public PerformanceMeasureType getPerformanceMeasureType () {
      return pm;
   }

   /**
    * Sets the checked performance measure to \texttt{pm}.
    * 
    * @param pm
    *           the new checked performance measure.
    * @exception NullPointerException
    *               if \texttt{pm} is \texttt{null}.
    */
   public void setPerformanceMeasureType (PerformanceMeasureType pm) {
      if (pm == null)
         throw new NullPointerException (
               "The type of performance measure must not be null");
      this.pm = pm;
   }

   public int check (ContactCenterSim sim, int newReps) {
      if (newReps == 0)
         // Take account of the default stopping condition (target error)
         return 0;
      final int nr = sim.getCompletedSteps ();
      if (nr >= maxReps)
         return 0;
      final MatrixOfStatProbes<?> sm = sim.getMatrixOfStatProbes (pm);
      final StatProbe sp = sm.get (sm.rows () - 1, sm.columns () - 1);
      if (sp instanceof Tally)
         ((Tally) sp).confidenceIntervalStudent (beta, cl);
      else if (sp instanceof FunctionOfMultipleMeansTally)
         ((FunctionOfMultipleMeansTally) sp).confidenceIntervalDelta (beta, cl);
      else
         throw new IllegalStateException ("Invalid statistical probe class: "
               + sp.getClass ().getName ());
      final double lower = cl[0] - cl[1];
      final double upper = cl[0] + cl[1];
      if (delta < lower || delta > upper)
         return 0;
      else
         return 1;
   }
}
