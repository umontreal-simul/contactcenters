package umontreal.iro.lecuyer.contactcenters.msk.stat;

import cern.colt.function.DoubleDoubleFunction;
import cern.jet.math.Functions;
import umontreal.iro.lecuyer.contactcenters.app.RowType;

/**
 * Defines the types of
 * matrices of measures, or raw statistics, supported by the call
 * center simulator.
 * During simulation, matrices of counters are updated in order
 * to get matrices of observations which are added to statistical probes.
 * Each matrix of counters regroups counts for a certain type of measure, e.g., the
 * number of served calls, the sum of waiting times, the
 * total time spent by busy agents, etc.
 * Each row of such a matrix concerns a call type, agent group
 * or (call type, agent group) pair, while each column
 * concerns a period.
 * If a single period is simulated, all matrices
 * contain a single column.
 *
 * There are two types of matrices of counters:
 * a regular type for most statistics, and a special type for statistics
 * based on an acceptable waiting time.
 * Regular matrices have $P+2$ columns, e.g., one column per period,
 * and a certain number of $R$ of rows.
 * When such a matrix of counters is updated, only one element is changed;
 * this ensures that the matrix update does not take too much time.
 * When the matrix is transformed into a matrix of observations,
 * only results for the $P$ main periods are retained,
 * and aggregates are computed for segments regrouping main periods.
 * Aggregates are also computed for rows, which results in the
 * matrix of observations having extra rows.
 *
 * Matrices of counters using acceptable waiting times are different, because
 * rows and columns cannot be aggregated to make matrices of
 * observations. Aggregation cannot be done, because each counter
 * may be updated with a different acceptable waiting time in general.
 *
 * This type can be determined for any enum constant by
 * getting its associated row type, using {@link #getRowType(boolean)
 * getRowType} \texttt{(false)}. The matrix type is AWT-based
 * only if its associated row type is {@link RowType#INBOUNDTYPEAWT}.
 *
 * The operator used for aggregation is often the sum, but this
 * can also be the maximum for some types of measures.
 * This operator can be obtained using the {@link #getAggregationFunction()}.
 */
public enum MeasureType {
   MAXBUSYAGENTS (RowType.AGENTGROUP, TimeNormalizeType.NEVER, Functions.max),

   MAXQUEUESIZE (RowType.WAITINGQUEUE, TimeNormalizeType.NEVER, Functions.max),

   MAXWAITINGTIMEABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.NEVER, Functions.max),

   MAXWAITINGTIMESERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.NEVER, Functions.max),

   NUMABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMABANDONEDAFTERAWT (RowType.INBOUNDTYPEAWT, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMABANDONEDBEFOREAWT (RowType.INBOUNDTYPEAWT, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMARRIVALS (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMBLOCKED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMBUSYAGENTS (RowType.AGENTGROUP, TimeNormalizeType.ALWAYS, Functions.plus),

   NUMDELAYED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMSCHEDULEDAGENTS (RowType.AGENTGROUP, TimeNormalizeType.ALWAYS, Functions.plus),

   NUMSERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMSERVEDAFTERAWT (RowType.INBOUNDTYPEAWTAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMSERVEDBEFOREAWT (RowType.INBOUNDTYPEAWTAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMTRIEDDIAL (RowType.OUTBOUNDTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   NUMWORKINGAGENTS (RowType.AGENTGROUP, TimeNormalizeType.ALWAYS, Functions.plus),

   NUMWRONGPARTYCONNECTS (RowType.OUTBOUNDTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   QUEUESIZE (RowType.WAITINGQUEUE, TimeNormalizeType.ALWAYS, Functions.plus),

   SUMEXCESSTIMESABANDONED (RowType.INBOUNDTYPEAWT, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMEXCESSTIMESSERVED (RowType.INBOUNDTYPEAWTAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMSERVED (RowType.CONTACTTYPEAGENTGROUP, RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMSERVICETIMES (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMWAITINGTIMESABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMWAITINGTIMESSERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMWAITINGTIMESVQABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),

   SUMWAITINGTIMESVQSERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),


   SUMSEWAITINGTIMESSERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus),   //ajout pour MSE
   SUMSEWAITINGTIMESABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),          //ajout pour MSE
   SUMSEWAITINGTIMESVQABANDONED (RowType.CONTACTTYPE, TimeNormalizeType.CONDITIONAL, Functions.plus),        //ajout pour MSE
   SUMSEWAITINGTIMESVQSERVED (RowType.CONTACTTYPEAGENTGROUP, TimeNormalizeType.CONDITIONAL, Functions.plus); //ajout pour MSE

   private DoubleDoubleFunction aggrFunc;
   private RowType rowType;
   private RowType rowTypeGroup;
   private TimeNormalizeType timeNormalizeType;

   private MeasureType (RowType rowType, RowType rowTypeGroup, TimeNormalizeType timeNormalizeType, DoubleDoubleFunction aggrFunc) {
      this.rowType = rowType;
      this.rowTypeGroup = rowTypeGroup;
      this.timeNormalizeType = timeNormalizeType;
      this.aggrFunc = aggrFunc;
   }

   private MeasureType (RowType rowType, TimeNormalizeType timeNormalizeType, DoubleDoubleFunction aggrFunc) {
      if (rowType.isContactTypeAgentGroup ()) {
         rowTypeGroup = rowType;
         this.rowType = rowType.toContactType ();
      }
      else {
         this.rowType = rowType;
         rowTypeGroup = rowType;
      }
      this.timeNormalizeType = timeNormalizeType;
      this.aggrFunc = aggrFunc;
   }

   /**
    * Returns the functions which is applied in order to
    * aggregate two values of counters of this type.
    * This usually returns {@link Functions#plus}, but
    * this can also return {@link Functions#max} for
    * example with {@link #MAXWAITINGTIMEABANDONED}.
    */
   public DoubleDoubleFunction getAggregationFunction() {
      return aggrFunc;
   }

   /**
    * Returns the row type for this type of measure.
    * If \texttt{contactTypeAgentGroup} is \texttt{true},
    * this returns the row type when statistics are collected
    * separately for (call type, agent group) pairs.
    * Otherwise, this returns the row type when
    * statistics are counted only for call types.
    *
    * @param contactTypeAgentGroup
    * @return the row type for this measure type.
    */
   public RowType getRowType (boolean contactTypeAgentGroup) {
      return contactTypeAgentGroup ? rowTypeGroup : rowType;
   }

   /**
    * Returns a constant indicating how time normalization
    * should be perform on matrix of counters of this type.
    */
   public TimeNormalizeType getTimeNormalizeType() {
      return timeNormalizeType;
   }
}
