package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Collections;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.params.BatchSimParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;

/**
 * \javadoc{Represents the column type for a matrix regrouping performance
 * measures. }Each type of performance measure has a column type that affects
 * the number and role of columns in any matrix of performance measures of that
 * type. Of course, the number of columns is also affected by the parameters of
 * the contact center.
 *
 * With the exception of {@link PerformanceMeasureType#SERVEDRATES} and
 * {@link PerformanceMeasureType#MAXQUEUESIZE}, each column corresponds to a
 * main period in the model, and the last column corresponds to the
 * time-aggregate values. If there is a single period, e.g., for steady-state
 * approximations or simulations, the matrix can have a single column. Note that
 * when using batch means, matrices of results do not contain a column for each
 * batch. \javadoc{To get values for each batch in a stationary simulation, one
 * must use a contact center simulator with observations and call
 * {@link ContactCenterSimWithObservations#getObs}. One must also make sure to
 * set up the simulator to keep track of the observations, which is
 * implementation-specific. For implementations using {@link BatchSimParams} for
 * experiment parameters, the method {@link SimParams#setKeepObs(boolean)} can be used
 * for this.}
 *
 * @xmlconfig.title Supported column types
 */
public enum ColumnType {
   /**
    * Columns representing main periods. More specifically,
    * let $P'\ge P$ be the number of columns of this type for a specific model of contact center.
    * If a matrix
    * has columns of this type and if there are $P$ main periods
    * in the model, column
    * $p=0,\ldots,P-1$ represents main period~$p$ while column~$P'-1$ is used for
    * representing all main periods. Columns $P,\ldots,P'-2$ represent user-defined
    * segments regrouping main periods.
    * If $P=1$, a single column represents the single
    * main period, and $P'=P$.
    *
    * @xmlconfig.title
    */
   MAINPERIOD {
      @Override
      public String getTitle () {
         return Messages.getString ("ColumnType.Periods"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int column) {
         final int P = eval.getNumMainPeriods();
         final int Ps = eval.getNumMainPeriodSegments();

         if (column >= P + Ps)
            return Messages.getString ("ColumnType.AllPeriods"); //$NON-NLS-1$
         else if (column >= P) {
            final int s = column - P;
            final String pn = eval.getMainPeriodSegmentName (s);
            return pn == null || pn.length () == 0 ? String.format (Messages
                  .getString ("ColumnType.PeriodSegment"), s) : pn; //$NON-NLS-1$
         }
         else {
            final String pn = eval.getMainPeriodName (column);
            return pn == null || pn.length () == 0 ? String.format (Messages
                  .getString ("ColumnType.Period"), column) : pn; //$NON-NLS-1$
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int column) {
         return Collections.EMPTY_MAP;
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int P = eval.getNumMainPeriods ();
         if (P <= 1)
            return P;
         return P + 1 + eval.getNumMainPeriodSegments();
      }
   },

   /**
    * Columns representing agent groups.
    * This is similar to {@link RowType#AGENTGROUP}, with
    * rows replaced with columns.
    *
    * @xmlconfig.title
    */
   AGENTGROUP {
      @Override
      public String getTitle () {
         return RowType.AGENTGROUP.getTitle ();
      }

      @Override
      public String getName (ContactCenterInfo eval, int column) {
         return RowType.AGENTGROUP.getName (eval, column);
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int column) {
         return RowType.AGENTGROUP.getProperties (eval, column);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return RowType.AGENTGROUP.count (eval);
      }
   },

   /**
    * Single column with no particular meaning. For example, the maximal queue
    * size has one row for each waiting queue but a single column.
    *
    * @xmlconfig.title
    */
   SINGLECOLUMN {
      @Override
      public String getTitle () {
         return "";
      }

      @Override
      public String getName (ContactCenterInfo eval, int column) {
         return "";
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int column) {
         return Collections.EMPTY_MAP;
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return 1;
      }
   };

   /**
    * Returns the title that should identify the rows of matrices of results for
    * this type of column. For example, this may return \texttt{Periods} for
    * {@link #MAINPERIOD}.
    *
    * @return the column title.
    */
   public abstract String getTitle ();

   /**
    * Returns the name associated with the column \texttt{column} in a matrix of
    * results for this type of column estimated by \texttt{eval}. For example,
    * if the method is called for {@link #MAINPERIOD}, and column~0, it may
    * return \texttt{Period 0}.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param column
    *           the column index.
    * @return the column name.
    */
   public abstract String getName (ContactCenterInfo eval, int column);

   /**
    * Returns the properties associated with column
    * \texttt{column}.
    * Properties are additional strings describing
    * a column.
    * This can include, e.g., the language of the customers,
    * the originating region, etc.
    * If no property is defined for the given
    * column, this method returns an empty map.
    *
    * @param eval the evaluation system.
    * @param column the column index.
    * @return the properties.
    */
   public abstract Map<String, String> getProperties (ContactCenterInfo eval, int column);

   /**
    * Returns the usual
    * number of columns in a matrix of performance measures with
    * columns of this
    * type estimated by the evaluation system \texttt{eval}.
    *
    * @param eval
    *           the queried evaluation system.
    * @return the number of rows.
    */
   public abstract int count (ContactCenterInfo eval);
}
