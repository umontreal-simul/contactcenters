package umontreal.iro.lecuyer.contactcenters.msk.cv;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

/**
 * Represents an object returning the $\beta$ function used for control
 * variables.
 */
public interface CVBetaFunction {
   /**
    * Returns the $\beta$ function for a performance measure.
    * 
    * @param m
    *           the type of performance measure.
    * @param row
    *           the row in the matrix of statistical probes.
    * @param col
    *           the column in the matrix of statistical probes.
    * @param cv
    *           the index of the control variable.
    * @param obs
    *           the observation this function is applied to.
    * @return the value of $\beta$.
    */
   public double getBeta (PerformanceMeasureType m, int row, int col, int cv,
         int obs);
}
