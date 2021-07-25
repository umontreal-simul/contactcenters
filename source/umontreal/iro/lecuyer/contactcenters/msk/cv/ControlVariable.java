package umontreal.iro.lecuyer.contactcenters.msk.cv;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.msk.simlogic.SimLogic;
import umontreal.iro.lecuyer.contactcenters.msk.stat.CallCenterStatProbes;

/**
 * Represents a type of control variable that can be applied on all performance
 * measures supported by a call center simulator. An implementation of this
 * interface obtains (or computes) observations of a centered control variable
 * $\tilde{C}(m, r, c) = C(m, r, c) - E[C(m, r, c)]$ for performance measure
 * type $m$, row $r$, and column $c$. Obtaining the centered CVs is usually done
 * by querying some statistical collectors, but sometimes, sums may be computed.
 * The exact control variable used might depend on the performance measure,
 * e.g., the number of arrivals for calls of a given type, and the expectation
 * might change from observations to observations. The only important point is
 * to have $E[\tilde{C}(m, r, c)]=0$ for each observation when the CV is
 * applicable.
 */
public interface ControlVariable {
   /**
    * Determines if this control variable can be applied to the type \texttt{pm}
    * of performance measure.
    * 
    * @param pm
    *           the type of performance measure.
    * @return \texttt{true} if the control variable can be applied,
    *         \texttt{false} otherwise.
    */
   public boolean appliesTo (PerformanceMeasureType pm);

   /**
    * Tests if the control variable can be applied to the performance measure of
    * type \texttt{pm} at row \texttt{row} and column \texttt{column} when using
    * the simulation logic \texttt{sim}.
    * 
    * @param sim
    *           the simulation logic.
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row index.
    * @param col
    *           the column index.
    * @return the result of the test.
    */
   public boolean appliesTo (SimLogic sim, PerformanceMeasureType pm, int row,
         int col);

   /**
    * Returns the number of observations for the control variable used for the
    * performance measure of type \texttt{pm}, at row \texttt{row} and column
    * \texttt{col}. If no control variable of the type represented by this
    * implementation is used with the specified performance measure, this
    * returns 0.
    * 
    * @param sim
    *           the simulation logic.
    * @param inStat
    *           the call center statistics.
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row index.
    * @param col
    *           the column index.
    * @return the number of observations of the control variable.
    */
   public int numberObs (SimLogic sim, CallCenterStatProbes inStat,
         PerformanceMeasureType pm, int row, int col);

   /**
    * Returns the centered observation with index \texttt{index} of the control
    * variable used for the type of performance measure \texttt{pm} at row
    * \texttt{row} and column \texttt{col}.
    * 
    * @param sim
    *           the simulation logic.
    * @param inStat
    *           the call center statistics.
    * @param pm
    *           the type of performance measure.
    * @param row
    *           the row index.
    * @param col
    *           the column index.
    * @param index
    *           the index of the observation.
    * @return the observation.
    */
   public double getObs (SimLogic sim, CallCenterStatProbes inStat,
         PerformanceMeasureType pm, int row, int col, int index);
   
   /**
    * Initializes any data structure used by
    * this control variable.
    * @param sim the simulation logic.
    */
   public void init (SimLogic sim);
}
