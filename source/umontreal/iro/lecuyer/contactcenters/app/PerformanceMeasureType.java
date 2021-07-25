package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Map;
import java.util.NoSuchElementException;

import cern.colt.matrix.DoubleMatrix2D;

/**
 * \javadoc{Represents types of performance measures for contact centers. }A
 * performance measure estimated by approximation formulas or simulation can be
 * described by a type, an index, and a time interval. The type might be, for
 * example, {@link #SERVICELEVEL}, while the index might represent a group of
 * contact types called a segment. All statistics concerning a given type of
 * performance measure are regrouped into a matrix with rows corresponding to
 * the index, and columns generally matching the time intervals. See
 * {@link RowType} and {@link ColumnType} for the possible types of rows and
 * columns in matrices of statistics. Statistics can be point estimators,
 * minima, maxima, variances, or confidence intervals. Point estimators can be
 * computed, depending on the type of performance measure, using averages,
 * functions of averages, averages of functions, or raw statistics. See
 * {@link EstimationType} for the possible types of point estimators.
 *
 * \javadoc{Constants of this enum are used to select a group of measures when
 * obtaining a matrix of results from an evaluation system. This enum defines
 * groups of performance measures, and provides facilities to format results. It
 * does not calculate any matrix of statistics. }
 *
 * Table~\ref{tab:pmmatrix} presents a typical matrix of performance measures
 * whose rows correspond to segments of contact types, and columns to segments
 * of main periods. The upper left part of the table regroups the performance
 * measures concerning specific contact types, and specific main periods. The
 * lower part of the table regroups performance measures concerning segments of
 * several contact types. This lower part appears in matrices of performance
 * measures if $K>1$, and contains several rows only if segments of contact
 * types are defined by the user. However, an implicit segment regrouping all
 * contact types always appears provided that $K>1$.
 *
 * In a similar way, the right part of the table regroups performance measures
 * concerning segments regrouping several main periods. These segments, which
 * are time intervals too, can be used, e.g., to get statistics for the morning,
 * the afternoon, the evening, a day of a week, etc. In a similar way to the
 * lower part, the right part of the table shows up only if $P>1$, and an
 * implicit segment regrouping all main periods is always displayed. Note that
 * the bottom right element of the matrix corresponds to the performance measure
 * concerning all contact types and main periods.
 *
 * \begin{table} \caption{Example of a matrix of performance measures}
 * \label{tab:pmmatrix}
 *
 * \[\begin{array}{r|ccccc|ccc|} \multicolumn{1}{c}{}
 * &\multicolumn{5}{c}{\mbox{Main periods}} & \multicolumn{3}{c}{\mbox{Segments
 * of main periods}} \\ \cline{2-9} &X_{0, 0} & \cdots & X_{0, p} & \cdots &
 * X_{0, P-1}\html{$\mbox{}$} & X_{0, P} & \cdots & X_{0, \cdot} \\ &\vdots &
 * \ddots & \vdots & \ddots & & \vdots & \ddots & \\ \mbox{Contact types}&X_{k,
 * 0} & \cdots & X_{k, p} & \cdots & X_{k, P-1} & X_{k, P}& \cdots & X_{k,
 * \cdot} \\ &\vdots & \ddots & \vdots & \ddots & & \vdots & \ddots & \\
 * &X_{K-1,0} & \cdots & X_{K-1, p} & \cdots & X_{K-1, P-1} & X_{K-1,P}& \cdots
 * & X_{K-1,\cdot} \\ \cline{2-9} \mbox{Segments of}&X_{K,0} & \cdots & X_{K, p}
 * & \cdots & X_{K, P-1} & X_{K,P}& \cdots & X_{K,\cdot} \\ \mbox{contact
 * types}&\vdots & \ddots & \vdots & \ddots & & \vdots & \ddots & \\
 * &X_{\cdot,0} & \cdots & X_{\cdot, p} & \cdots & X_{\cdot, P-1} & X_{\cdot,P}&
 * \cdots & X \\ \cline{2-9} \end{array}\] \end{table}
 *
 * Segments can also be defined to regroup inbound and outbound contact types,
 * and agent groups. A segment of inbound contact types affects only matrices of
 * performance measures concerning inbound contact types, e.g.,
 * {@link #SERVICELEVEL}. Similarly, a segment of outbound contact types affects
 * only matrices of performance measures concerning outbound types, e.g.,
 * {@link #RATEOFTRIEDOUTBOUND}.
 *
 * Many types of performance measures we now describe correspond to the expected
 * number of calls counted in a time interval $[t_1, t_2]$ meeting a certain
 * condition, e.g., served calls. By default, a call is counted in a time
 * interval if it arrives during that interval. But using the
 * \texttt{perPeriodCollectingMode} attribute of simulation parameters, this can
 * be changed, e.g., to count a call if it ends its service or abandons during
 * the interval.
 *
 * @xmlconfig.title Available performance measures
 */
public enum PerformanceMeasureType {
   // Note that after adding enum constants there, one should
   // update the static block in CallCenterMeasureManager.

   /**
    * Probability of abandonment, i.e., the fraction of the expected number of
    * contacts having left the system without service over the total expected
    * number of arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   ABANDONMENTRATIO(
      Messages.getString("PerformanceMeasureType.AbandonmentRatio"),  //$NON-NLS-1$
      EstimationType.FUNCTIONOFEXPECTATIONS, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Probability of abandonment after the acceptable waiting time. This
    * corresponds to the fraction of the expected number of contacts having left
    * the system without service and after a waiting time greater than or equal
    * to the acceptable waiting time, over the total expected number of
    * arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   ABANDONMENTRATIOAFTERAWT(
      Messages.getString("PerformanceMeasureType.AbandonmentRatioAfterAWT"),  //$NON-NLS-1$
      EstimationType.FUNCTIONOFEXPECTATIONS, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Probability of abandonment before the acceptable waiting time. This
    * corresponds to the fraction of the expected number of contacts having left
    * the system without service and waiting at most for the acceptable waiting
    * time, over the total expected number of arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   ABANDONMENTRATIOBEFOREAWT(
      Messages.getString("PerformanceMeasureType.AbandonmentRatioBeforeAWT"),  //$NON-NLS-1$
      EstimationType.FUNCTIONOFEXPECTATIONS, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Corresponds to the expectation of ratio version of
    * {@link #ABANDONMENTRATIO}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   ABANDONMENTRATIOREP(
      Messages.getString("PerformanceMeasureType.AbandonmentRatioRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Expected time-average number of busy agents over the simulation time, for
    * each agent group and period. More specifically, if $\Nb(t)$ is the number
    * of busy agents at time $t$, for a time interval $[t_1,t_2]$, the
    * performance measure is given by
    * \[\frac{1}{t_2-t_1}\E\left[\int_{t_1}^{t_2}\Nb(t)dt\right].\]
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   AVGBUSYAGENTS(
      Messages.getString("PerformanceMeasureType.AvgBusyAgents"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.AGENTGROUP, ColumnType.MAINPERIOD,
      false, false, 0),

   /**
    * Represents the expected time-average queue size for each waiting queue.
    * This measure corresponds to the integral of the queue size over simulation
    * time whereas {@link #MAXQUEUESIZE} gives the maximal observed queue size.
    * More specifically, if $Q(t)$ is the queue size at time $t$, for any time
    * interval $[t_1,t_2]$, the performance measure is given by
    * \[\frac{1}{t_2-t_1}\E\left[\int_{t_1}^{t_2}Q(t)dt\right].\]
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   AVGQUEUESIZE(
      Messages.getString("PerformanceMeasureType.AvgQueueSize"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.WAITINGQUEUE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the expected time-average number of scheduled agents over the
    * simulation time, for each agent group and period. This includes the busy
    * and idle agents (available or not), as well as the ghost agents, i.e.,
    * agents finishing the service of contacts before leaving. More
    * specifically, if $N(t)$ is the number of agents scheduled at time $t$, and
    * $\Ng(t)$ is the number of extra ghost agents, for a time interval
    * $[t_1,t_2]$, the performance measure is given by
    * \[\frac{1}{t_2-t_1}\E\left[\int_{t_1}^{t_2}(N(t)+\Ng(t))dt\right].\] As
    * $N(t)$ is set according to the staffing given by the user, it is constant
    * during main periods, and the above quantity is random only because of
    * $\Ng(t)$. Moreover, because of the ghost agents, if this performance
    * measure is estimated for a specific main period, the obtained estimate
    * will often be higher than the input staffing for the same period.
    *
    * Also note that this performance measure on the whole horizon does not
    * correspond to the mean number of full-time equivalents (FTE). To get the
    * FTE, one should multiply the time-average number of agents by $(t_P -
    * t_0)/h$ where $t_0$ and $t_P$ are the starting and ending times of the
    * main periods, and $h$ is the duration of an average working day for
    * agents. Of course, every one of these quantities must be expressed in the
    * same time unit to get a valid ratio.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   AVGSCHEDULEDAGENTS(
      Messages.getString("PerformanceMeasureType.AvgScheduledAgents"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.AGENTGROUP, ColumnType.MAINPERIOD,
      false, false, 0),

   /**
    * Represents the expected time-average number of working agents over the
    * simulation time, for each agent group and period. This is similar to
    * {@link #AVGSCHEDULEDAGENTS} but excludes the non-available idle agents.
    * More specifically, if $\Nb(t)$ is the number of busy agents at time $t$,
    * and $\Nf(t)$ is the number of idle but available agents, for a time
    * interval $[t_1,t_2]$, the performance measure is given by
    * \[\frac{1}{t_2-t_1}\E\left[\int_{t_1}^{t_2}(\Nb(t) + \Nf(t))dt\right].\]
    * If agents cannot become unavailable, e.g., by disconnecting temporarily
    * after service terminations, the two performance measures are identical.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   AVGWORKINGAGENTS(
      Messages.getString("PerformanceMeasureType.AvgWorkingAgents"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.AGENTGROUP, ColumnType.MAINPERIOD,
      false, false, 0),

   /**
    * Probability of blocking, i.e., the fraction of the expected number of
    * blocked contacts over the total expected number of arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   BLOCKRATIO(
      Messages.getString("PerformanceMeasureType.BlockRatio"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #BLOCKRATIO}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   BLOCKRATIOREP(
      Messages.getString("PerformanceMeasureType.BlockRatioRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Number of busy agents at the end of the simulation. When the simulation
    * horizon is finite, this should always be 0.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   BUSYAGENTSENDSIM(
      Messages.getString("PerformanceMeasureType.BusyAgentsEndSim"),  //$NON-NLS-1$
      EstimationType.RAWSTATISTIC, RowType.AGENTGROUP,
      ColumnType.SINGLECOLUMN, false, false, 0),

   /**
    * Probability of delay, i.e., the fraction of the expected number of
    * contacts not served immediately over the total expected number of
    * arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   DELAYRATIO(
      Messages.getString("PerformanceMeasureType.DelayRatio"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #DELAYRATIO}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   DELAYRATIOREP(
      Messages.getString("PerformanceMeasureType.DelayRatioRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Average excess time performance measure. This corresponds to the expected
    * sum of excess times for all contacts over the expected number of arrivals.
    * Let $A(t_2,t_2)$ be the total number of calls counted during interval
    * $[t_1,t_2]$ and $W_i$ the waiting time of the $i$th contact counted during
    * the interval, and $s$ the acceptable waiting time. The average excess time
    * is \[ \frac{\E\left[\sum_{i=0}^{A(t_1,t_2)-1} (W_i -
    * s)^+\right]}{\E[A(t_1,t_2)]}. \] The numerator of the ratio corresponds to
    * {@link #SUMEXCESSTIMES}, while the denominator corresponds to
    * {@link #RATEOFARRIVALS}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIME(Messages.getString("PerformanceMeasureType.ExcessTime"),
              EstimationType.FUNCTIONOFEXPECTATIONS, RowType.INBOUNDTYPEAWT,
              ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average excess time performance measure for contacts having abandoned.
    * This corresponds to the expected total excess time for contacts having
    * abandoned over the expected number of abandoned contacts. Let $L(t_1,t_2)$
    * be the number of contacts counted during time interval $[t_1,t_2]$ and
    * having abandoned, and $W_i$ the waiting time of the $i$th contact counted
    * during $[t_1,t_2]$, and $s$ the acceptable waiting time. The average
    * excess time is \[ \frac{\E\left[\sum_{i=0}^{L(t_1,t_2)-1} (W_i -
    * s)^+\I[\mbox{Call $i$ abandoned}]\right]}{\E[L(t_1,t_2)]}. \] The
    * numerator of the ratio corresponds to {@link #SUMEXCESSTIMESABANDONED},
    * while the denominator corresponds to {@link #RATEOFABANDONMENT}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIMEABANDONED(Messages
                       .getString("PerformanceMeasureType.ExcessTimeAbandoned"),
                       EstimationType.FUNCTIONOFEXPECTATIONS, RowType.INBOUNDTYPEAWT,
                       ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expectation of ratio version of {@link #EXCESSTIMEABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIMEABANDONEDREP(Messages
                          .getString("PerformanceMeasureType.ExcessTimeAbandonedRep"),
                          EstimationType.EXPECTATIONOFFUNCTION, RowType.INBOUNDTYPEAWT,
                          ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expectation of ratio version of {@link #EXCESSTIME}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIMEREP(Messages.getString("PerformanceMeasureType.ExcessTimeRep"),
                 EstimationType.EXPECTATIONOFFUNCTION, RowType.INBOUNDTYPEAWT,
                 ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average excess time performance measure for served contacts. This
    * corresponds to the expected total excess time for contacts having been
    * served over the expected number of served contacts. Let $S(t_1,t_2)$ be
    * the number of served contacts counted during interval $[t_1,t_2]$ and
    * $W_i$ the waiting time of the $i$th contact counted during $[t_1,t_2]$,
    * and $s$ the acceptable waiting time. The average excess time is \[
    * \frac{\E\left[\sum_{i=0}^{S( t_1, t_2)-1} (W_i- s)^+\I[\mbox{Call $i$
    * served}]\right]}{\E[S( t_1, t_2)]}. \] The numerator of the ratio
    * corresponds to {@link #SUMEXCESSTIMESSERVED}, while the denominator
    * corresponds to {@link #RATEOFSERVICES}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIMESERVED(Messages
                    .getString("PerformanceMeasureType.ExcessTimeServed"),
                    EstimationType.FUNCTIONOFEXPECTATIONS, RowType.INBOUNDTYPEAWT,
                    ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expectation of ratio version of {@link #EXCESSTIMESERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   EXCESSTIMESERVEDREP(Messages
                       .getString("PerformanceMeasureType.ExcessTimeServedRep"),
                       EstimationType.EXPECTATIONOFFUNCTION, RowType.INBOUNDTYPEAWT,
                       ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the expected maximal number of busy agents observed for a set
    * of agent groups. This expectation often corresponds to the number of
    * scheduled agents, because for most models, all agents are busy at some
    * times. However, the maximal number of busy agents may be smaller than the
    * number of agents if too many agents were planned. If the expectation is
    * estimated by an average of observations, taking the maximum of these
    * observations gives the maximal number of busy agents over all the
    * simulation.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXBUSYAGENTS(
      Messages.getString("PerformanceMeasureType.MaxBusyAgents"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.AGENTGROUP, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the expected maximal size observed for a waiting queue. If the
    * expectation is estimated by an average of observations, taking the maximum
    * of these observations gives the maximal queue size observed during all the
    * simulation.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXQUEUESIZE(
      Messages.getString("PerformanceMeasureType.MaxQueueSize"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.WAITINGQUEUE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the expected maximal waiting time observed for a set of contact
    * types. This performance measure can be defined as follows for a specific
    * contact type. Let $W_k$ be the (random) waiting time for a contact of type
    * $k$. The maximal waiting time for contacts of type $k$ during the
    * simulated horizon is $\max(W_k)$ while the performance measure is
    * $\E[\max(W_k)]$. In a similar way, we can define the measure for all
    * contacts. For this, let $W$ be the waiting time for a contact of any type.
    * The performance measure is then $\E[\max(W)]$. Note that although
    * $\max(W)=\max(W_1,\ldots,W_K)$, in general, \[ \E[\max(W)] \ne
    * \max(\E[W_1], \ldots, \E[W_K]).\] The performance can be defined similarly
    * for specific time intervals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXWAITINGTIME(
      Messages.getString("PerformanceMeasureType.MaxWaitingTime"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Same as {@link #MAXWAITINGTIME}, for (contact type, agent group) pairs.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXWAITINGTIMEG(
      Messages.getString("PerformanceMeasureType.MaxWaitingTimeG"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the expected maximal waiting time of contacts having abandoned,
    * for each contact type and period.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXWAITINGTIMEABANDONED(
      Messages.getString("PerformanceMeasureType.MaxWaitingTimeAbandoned"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the maximal expected waiting time of served contacts, for each
    * contact type and period.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXWAITINGTIMESERVED(
      Messages.getString("PerformanceMeasureType.MaxWaitingTimeServed"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the maximal expected waiting time of served contacts, for each
    * (contact type, agent group) pair and period.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MAXWAITINGTIMESERVEDG(
      Messages.getString("PerformanceMeasureType.MaxWaitingTimeServedG"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Agents' occupancy ratio. Defined as the expected number of busy agents
    * over the expected total number of scheduled agents, over the simulation
    * time. The expectation at the numerator corresponds to the
    * {@link #AVGBUSYAGENTS} type of performance measure while the expectation
    * at the denominator corresponds to {@link #AVGSCHEDULEDAGENTS}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   OCCUPANCY(
      Messages.getString("PerformanceMeasureType.Occupancy"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.AGENTGROUP, ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Alternate agents' occupancy ratio. Defined as the expected number of busy
    * agents over the expected total number of working agents, over the
    * simulation time. This differs from {@link #OCCUPANCY} only when agents are
    * allowed to disconnect after services. The expectation at the numerator
    * corresponds to the {@link #AVGBUSYAGENTS} type of performance measure
    * while the expectation at the denominator corresponds to
    * {@link #AVGWORKINGAGENTS}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   OCCUPANCY2(
      Messages.getString("PerformanceMeasureType.Occupancy2"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.AGENTGROUP, ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #OCCUPANCY2}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   OCCUPANCY2REP(
      Messages.getString("PerformanceMeasureType.OccupancyRep2"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.AGENTGROUP,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #OCCUPANCY}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   OCCUPANCYREP(
      Messages.getString("PerformanceMeasureType.OccupancyRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.AGENTGROUP,
      ColumnType.MAINPERIOD, true, false, 0),

   /**
    * Gives the queue size at the end of the simulation. This quantity should be
    * 0 for simulations over a finite horizon, since the waiting queues are
    * emptied at the end of each replication.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   QUEUESIZEENDSIM(
      Messages.getString("PerformanceMeasureType.QueueSizeEndSim"),  //$NON-NLS-1$
      EstimationType.RAWSTATISTIC, RowType.WAITINGQUEUE,
      ColumnType.SINGLECOLUMN, false, false, 0),

   /**
    * Corresponds to the rate of contacts of each type having abandoned,
    * excluding contacts blocked because of insufficient queue capacity.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFABANDONMENT(
      Messages.getString("PerformanceMeasureType.RateOfAbandonment"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of contacts of each inbound type having waited
    * more than the acceptable waiting time, before they abandon.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFABANDONMENTAFTERAWT(
      Messages.getString("PerformanceMeasureType.RateOfAbandonmentAfterAWT"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of contacts of each inbound type having waited
    * less than the acceptable waiting time, before they abandon.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFABANDONMENTBEFOREAWT(
      Messages
      .getString("PerformanceMeasureType.RateOfAbandonmentBeforeAWT"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Defined as the rate of contacts arriving into the router for being
    * assigned an agent. This includes blocked and served contacts, as well as
    * contacts having abandoned. For inbound contacts, the arrival rate can be
    * computed easily from the input data, except for call types corresponding
    * to transfer targets. For outbound contacts, this corresponds to the rate
    * of right party connects during the simulation.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFARRIVALS(
      Messages.getString("PerformanceMeasureType.RateOfArrivals"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Same as {@link #RATEOFARRIVALS}, for inbound contacts only.
    */
   RATEOFARRIVALSIN(
      Messages.getString("PerformanceMeasureType.RateOfArrivalsIn"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPE,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of contacts blocked because the queue capacity was
    * exceeded at the time of their arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFBLOCKING(
      Messages.getString("PerformanceMeasureType.RateOfBlocking"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of delayed contacts, i.e., the rate of contacts
    * not served immediately upon arrival. Since blocked contacts would have to
    * wait if they were not blocked, they are counted as positive waits too. For
    * outbound contacts, this corresponds to mismatches.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFDELAY(
      Messages.getString("PerformanceMeasureType.RateOfDelay"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of served or abandoned inbound contacts of each
    * type having waited less than the acceptable waiting time. This corresponds
    * to the sum of performance measures {@link #RATEOFABANDONMENTBEFOREAWT} and
    * {@link #RATEOFSERVICESBEFOREAWT}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFINTARGETSL(
      Messages.getString("PerformanceMeasureType.RateOfInTargetSL"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Defined as the rate of contacts offered. This includes served contacts as
    * well as contacts still in queue after the end of experiment or having
    * abandoned, but this excludes blocked contacts. For outbound contacts, this
    * corresponds to the rate of right party connects during the simulation.
    * When the total queue capacity is infinite, this corresponds to the number
    * of arrivals {@link #RATEOFARRIVALS}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFOFFERED(
      Messages.getString("PerformanceMeasureType.RateOfOffered"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the rate of served contacts for each contact type and period.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFSERVICES(
      Messages.getString("PerformanceMeasureType.RateOfServices"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of served inbound contacts of each type having
    * waited more than the acceptable waiting time.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFSERVICESAFTERAWT(
      Messages.getString("PerformanceMeasureType.RateOfServicesAfterAWT"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Corresponds to the rate of served inbound contacts of each type having
    * waited less than the acceptable waiting time.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFSERVICESBEFOREAWT(
      Messages.getString("PerformanceMeasureType.RateOfServicesBeforeAWT"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the rate of served contacts for each contact type, agent group,
    * and period. This is similar to {@link #RATEOFSERVICES}, but this gives the
    * rate at which each agent group serves contacts of each type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFSERVICESG(
      Messages.getString("PerformanceMeasureType.RateOfServicesG"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Defined as the rate of contacts of each outbound type the dialer or agents
    * have tried to make. This includes the number of reached (arrived) contacts
    * as well as the number of failed contacts.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFTRIEDOUTBOUND(
      Messages.getString("PerformanceMeasureType.RateOfTriedOutbound"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.OUTBOUNDTYPE,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Defined as the rate of contacts of each outbound type the dialer or agents
    * have tried to make, and for which the wrong party was reached.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   RATEOFWRONGPARTYCONNECT(
      Messages.getString("PerformanceMeasureType.RateOfWrongPartyConnect"),  //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.OUTBOUNDTYPE,
      ColumnType.MAINPERIOD, false, false, 0),

   /**
    * Represents the rate of contacts of a given type served by agents in a
    * specific group, per simulation time unit. The element $(k, i)$ of a served
    * rates matrix corresponds to the rate of served contacts of type~$k$ by
    * agents in the group~$i$ during one simulation time unit. Column~$i$ of the
    * last row corresponds to the total number of served contacts by agents in
    * the group~$i$, per simulation time unit. Row~$k$ of the last column
    * represents the total number of contacts with type~$k$ served by any agent,
    * per simulation time unit.
    *
    * This performance measure is similar to {@link #RATEOFSERVICESG}, except
    * that it is estimated only globally, not for each main period, with less
    * memory than {@link #RATEOFSERVICESG}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVEDRATES(
      Messages.getString("PerformanceMeasureType.ServedRates"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.AGENTGROUP, false, false, 0),

   /**
    * Service level performance measure. Let $\Sg(s, t_1, t_2)$ be the number of
    * contacts counted during interval $[t_1,t_2]$, and served after a waiting
    * time less than or equal to the acceptable waiting time $s$, and $S(t_1,
    * t_2)$ be the total number of served contacts counted during $[t_1,t_2]$.
    * Let $\Lg(s, t_1, t_2)$ be the number of contacts counted during interval
    * $[t_1,t_2]$ having abandoned after a waiting time smaller than or equal to
    * the acceptable waiting time, and $A(t_1, t_2)$ be the total number of
    * contacts counted in the $[t_1,t_2]$ interval. The service level is defined
    * by \[g_1(s, t_1, t_2)=\E[\Sg(s, t_1, t_2)]/\E[A(t_1, t_2) - \Lg(s, t_1,
    * t_2)].\]
    *
    * NOTE: since this performance measure is of type
    * \texttt{FUNCTIONOFEXPECTATIONS}, the complete list of observations
    * generated by the simulator are not available directly; instead, one must
    * use the performance measure \texttt{SERVICELEVELREP}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVEL(
      Messages.getString("PerformanceMeasureType.ServiceLevel"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.INBOUNDTYPEAWT, ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Represents the expectation of ratio version of {@link #SERVICELEVEL}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVELREP(
      Messages.getString("PerformanceMeasureType.ServiceLevelRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Represents an indicator function of {@link #SERVICELEVELREP} at the end of a replication.
    * The value is 1 if the service level meets or exceeds the service level target,
    * else it is 0.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVELIND01(
      Messages.getString("PerformanceMeasureType.ServiceLevelInd01"), 
      EstimationType.RAWSTATISTIC, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, true, false, 1),
   
   /**
    * Alternate service level performance measure. This service level is defined
    * as \[g_2(s, t_1, t_2)=\E[\Sg(s, t_1, t_2) + \Lg(s, t_1, t_2)]/\E[A( t_1,
    * t_2)],\] with the same notation as in {@link #SERVICELEVEL}. The
    * performance measure matrix has the same format as {@link #SERVICELEVEL},
    * and this type of measure is equivalent to {@link #SERVICELEVEL} if there
    * is no abandonment, and all contacts exit the waiting queues before the end
    * of the simulation.
    *
    * {NOTE}: since this performance measure is of type
    * \texttt{FUNCTIONOFEXPECTATIONS}, the complete list of observations
    * generated by the simulator are not available directly; instead, one must
    * use the performance measure \texttt{SERVICELEVEL2REP}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVEL2(
      Messages.getString("PerformanceMeasureType.ServiceLevel2"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.INBOUNDTYPEAWT, ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Represents the expectation of ratio version of {@link #SERVICELEVEL2}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVEL2REP(
      Messages.getString("PerformanceMeasureType.ServiceLevel2Rep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Service level performance measure for contact types and agent groups. Let
    * $\Sg[k, i](s, t_1, t_2)$ be the number of contacts of type $k$ counted
    * during time interval $[t_1,t_2]$ and served by agents in group $i$ after a
    * waiting time less than or equal to the acceptable waiting time $s$. Let
    * $S_{k,i}(t_1,t_2)$ be the number of type-$k$ contacts counted during the
    * interval, and served by agents in groupe $i$. Let $B_k(t_1,t_2)$ and
    * $\Lb[k](s, t_1, t_2)$ be the number of contacts of type $k$ counted during
    * $[t_1,t_2]$, blocked and having abandoned after a waiting time greater
    * than the acceptable waiting time, respectively. The service level is
    * defined by \[g_3(s, t_1, t_2)=\E[\Sg[k, i](s, t_1,
    * t_2)]/\E[S_{k,i}(t_1,t_2) + \Lb[k](s, t_1, t_2) + B_k(t_1,t_2)].\]
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICELEVELG(
      Messages.getString("PerformanceMeasureType.ServiceLevelG"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.INBOUNDTYPEAWTAGENTGROUP, ColumnType.MAINPERIOD, true, false,
      1),

   /**
    * Probability of service, i.e., the fraction of the expected number of
    * contacts served over the total expected number of arrivals.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICERATIO(
      Messages.getString("PerformanceMeasureType.ServiceRatio"),  //$NON-NLS-1$
      EstimationType.FUNCTIONOFEXPECTATIONS, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Corresponds to the expectation of ratio version of {@link #SERVICERATIO}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICERATIOREP(
      Messages.getString("PerformanceMeasureType.ServiceRatioRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, true, false, 1),

   /**
    * Expected total service time over the expected number of services, for each
    * contact type, whether inbound or outbound. Usually, this can be computed
    * easily from the input service time, and can therefore be used for checking
    * parameter files. However, when call transfers or virtual queueing occur,
    * service times can be altered by multipliers or additional random
    * variables.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICETIME(
      Messages.getString("PerformanceMeasureType.ServiceTime"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expected total service time over the expected number of services, for each
    * (contact type, agent group).
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICETIMEG(
      Messages.getString("PerformanceMeasureType.ServiceTimeG"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #SERVICETIME}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SERVICETIMEREP(
      Messages.getString("PerformanceMeasureType.ServiceTimeRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average speed of answer, i.e., the expected total waiting time of served
    * contacts over the expected number of served contacts, for each contact
    * type, whether inbound or outbound. The numerator of the ratio corresponds
    * to {@link #SUMWAITINGTIMESSERVED}, while the denominator corresponds to
    * {@link #RATEOFSERVICES}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SPEEDOFANSWER(
      Messages.getString("PerformanceMeasureType.SpeedOfAnswer"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average speed of answer for (contact type, agent group), i.e., the
    * expected total waiting time of served contacts over the expected number of
    * served contacts, for each (contact type, agent group) pair.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SPEEDOFANSWERG(
      Messages.getString("PerformanceMeasureType.SpeedOfAnswerG"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #SPEEDOFANSWER}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SPEEDOFANSWERREP(
      Messages.getString("PerformanceMeasureType.SpeedOfAnswerRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the expected sum of excess times of contacts. For a contact
    * with waiting time $W$ and acceptable waiting time $s$ used for computing
    * the service level, the excess time is $(W-s)^+$.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMEXCESSTIMES(
      Messages.getString("PerformanceMeasureType.SumExcessTimes"), //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the expected sum of excess times of contacts having abandoned.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMEXCESSTIMESABANDONED(
      Messages.getString("PerformanceMeasureType.SumExcessTimesAbandoned"), //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the expected sum of excess times of served contacts.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMEXCESSTIMESSERVED(
      Messages.getString("PerformanceMeasureType.SumExcessTimesServed"), //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.INBOUNDTYPEAWT,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of service times of contacts.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSERVICETIMES(
      Messages.getString("PerformanceMeasureType.SumServiceTimes"), //$NON-NLS-1$
      EstimationType.EXPECTATION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of waiting times, for each contact type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMES(
      Messages.getString("PerformanceMeasureType.SumWaitingTimes"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time.
    * The structure is similar to {@link #SUMWAITINGTIMES}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMES(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimes"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of waiting times of contacts having abandoned, for each
    * contact type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMESABANDONED(
      Messages.getString("PerformanceMeasureType.SumWaitingTimesAbandoned"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time, of contacts that have abandoned.
    * The structure is similar to {@link #SUMWAITINGTIMESABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMESABANDONED(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimesAbandoned"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of waiting times of served contacts, for each contact
    * type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMESSERVED(
      Messages.getString("PerformanceMeasureType.SumWaitingTimesServed"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time, of served contacts.
    * The structure is similar to {@link #SUMWAITINGTIMESSERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMESSERVED(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimesServed"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of waiting times in virtual queue, for each contact
    * type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMESVQ(
      Messages.getString("PerformanceMeasureType.SumWaitingTimesVQ"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   // Ajouter pour le calcul du MSE
   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time.
    * The structure is similar to {@link #SUMWAITINGTIMESVQ}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMESVQ(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimesVQ"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),
   /**
    * Represents the sum of waiting times in virtual queue of contacts having
    * abandoned, for each contact type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMESVQABANDONED(
      Messages
      .getString("PerformanceMeasureType.SumWaitingTimesVQAbandoned"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time, of contacts that have abandoned.
    * The structure is similar to {@link #SUMWAITINGTIMESVQABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMESVQABANDONED(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimesVQAbandoned"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Represents the sum of waiting times in virtual queue of served contacts,
    * for each contact type.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMWAITINGTIMESVQSERVED(
      Messages.getString("PerformanceMeasureType.SumWaitingTimesVQServed"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),


   /**
    * Represents the sum of square of difference between the predicted waiting
    * time and the real waiting time, of served contacts.
    * The structure is similar to {@link #SUMWAITINGTIMESVQSERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   SUMSEWAITINGTIMESVQSERVED(
      Messages
      .getString("PerformanceMeasureType.SumSEWaitingTimesVQServed"), EstimationType.EXPECTATION,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Time to abandon of contacts, i.e., the expected total waiting time of
    * contacts having abandoned over the expected number of contacts having
    * abandoned, for each contact type, whether inbound or outbound. The
    * numerator of the ratio corresponds to {@link #SUMWAITINGTIMESABANDONED},
    * while the denominator corresponds to {@link #RATEOFABANDONMENT}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   TIMETOABANDON(
      Messages.getString("PerformanceMeasureType.TimeToAbandon"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #TIMETOABANDON}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   TIMETOABANDONREP(
      Messages.getString("PerformanceMeasureType.TimeToAbandonRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expected total waiting time over the expected number of arrivals, for each
    * contact type, whether inbound or outbound, whether served or having
    * abandoned. For outbound contacts, the expected waiting times are non-zero
    * only when mismatches are not dropped. The numerator of the ratio
    * corresponds to {@link #SUMWAITINGTIMES}, while the denominator corresponds
    * to {@link #RATEOFARRIVALS}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIME(
      Messages.getString("PerformanceMeasureType.WaitingTime"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor}.
    * This measure is identical to {@link #WAITINGTIME}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMES}
    * instead of {@link #SUMWAITINGTIMES}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIME(
      Messages.getString("PerformanceMeasureType.MseWaitingTime"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),


   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor},
    * for the contacts that have abandoned.
    * This measure is identical to {@link #TIMETOABANDON}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMESABANDONED}
    * instead of {@link #SUMWAITINGTIMESABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIMEABANDONED(
      // Ajouter
      Messages.getString("PerformanceMeasureType.MseWaitingTimeAbandoned"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor},
    * for the served contacts.
    * This measure is identical to {@link #SPEEDOFANSWER}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMESSERVED}
    * instead of {@link #SUMWAITINGTIMESSERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIMESERVED(
      // Ajouter
      Messages.getString("PerformanceMeasureType.MseWaitingTimeServed"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average waiting time, for each (contact type, agent group) pair, whether
    * inbound or outbound, whether served or having abandoned. Let
    * $\WS[k,i](t_1,t_2)$ be the sum of waiting times for contacts of type $k$
    * served by agents in groupe $i$, counted during time interval $[t_1, t_2]$.
    * Let $S_{k,i}(t_1, t_2)$ be the number of type-$k$ contacts served by
    * agents in group $i$, and counted during the time interval $[t_1,t_2]$. Let
    * $L_k(t_1,t_2)$ and $B_k(t_1,t_2)$ the number of type-$k$ blocked contacts
    * counted during the interval. The average waiting time for type $k$ and
    * group $i$ is then defined as \[ \E[\WS[k,i](t_1,t_2)] /
    * \E[S_{k,i}(t_1,t_2)+L_k(t_1,t_2)+B_k(t_1,t_2)]. \]
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEG(
      Messages.getString("PerformanceMeasureType.WaitingTimeG"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPEAGENTGROUP, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #WAITINGTIME}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEREP(
      Messages.getString("PerformanceMeasureType.WaitingTimeRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expected total waiting time in virtual queue over the expected number of
    * arrivals, for each contact type, whether inbound or outbound, whether
    * served or having abandoned. The numerator of the ratio corresponds to
    * {@link #SUMWAITINGTIMESVQ}, while the denominator corresponds to
    * {@link #RATEOFARRIVALS}. Note that this waiting time is not counted in the
    * regular waiting time corresponding to {@link #WAITINGTIME} type of
    * performance measure.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQ(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQ"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor},
    * of contacts that entered in the virtual queues.
    * This measure is identical to {@link #WAITINGTIMEVQ}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMESVQ}
    * instead of {@link #SUMWAITINGTIMESVQ}.
    * Note that this MSE is not included in the
    * regular MSE waiting time corresponding to {@link #MSEWAITINGTIME}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIMEVQ(
      Messages.getString("PerformanceMeasureType.MseWaitingTimeVQ"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average time spent in virtual queue before contact back followed by
    * abandonment. The numerator of the ratio corresponds to
    * {@link #SUMWAITINGTIMESVQABANDONED}, while the denominator corresponds to
    * {@link #RATEOFABANDONMENT}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQABANDONED(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQAbandoned"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor},
    * for contacts that have abandoned the virtual queues.
    * This measure is identical to {@link #WAITINGTIMEVQABANDONED}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMESVQABANDONED}
    * instead of {@link #SUMWAITINGTIMESVQABANDONED}.
    * Note that this MSE is not included in the
    * regular MSE waiting time corresponding to {@link #MSEWAITINGTIMEABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIMEVQABANDONED(
      Messages.getString("PerformanceMeasureType.MseWaitingTimeVQAbandoned"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of
    * {@link #WAITINGTIMEVQABANDONED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQABANDONEDREP(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQAbandonedRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of {@link #WAITINGTIMEVQ}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQREP(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Average time spent in virtual queue for contacts served after they are
    * contacted back. The numerator of the ratio corresponds to
    * {@link #SUMWAITINGTIMESVQSERVED}, while the denominator corresponds to
    * {@link #RATEOFSERVICES}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQSERVED(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQServed"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * The empirical mean square error (MSE) of the waiting time predictor, given by
    * {@link umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor},
    * of served contacts from the virtual queues.
    * This measure is identical to {@link #WAITINGTIMEVQSERVED}, except that
    * the numerator of the ratio is {@link #SUMSEWAITINGTIMESVQSERVED}
    * instead of {@link #SUMWAITINGTIMESVQSERVED}.
    * Note that this MSE is not included in the
    * regular MSE waiting time corresponding to {@link #MSEWAITINGTIMESERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   MSEWAITINGTIMEVQSERVED(
      Messages.getString("PerformanceMeasureType.MseWaitingTimeVQServed"), EstimationType.FUNCTIONOFEXPECTATIONS,  //$NON-NLS-1$
      RowType.CONTACTTYPE, ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of
    * {@link #WAITINGTIMEVQSERVED}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEVQSERVEDREP(
      Messages.getString("PerformanceMeasureType.WaitingTimeVQServedRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Expected total waiting time over the expected number of contacts having to
    * wait in queue. The numerator of the ratio corresponds to
    * {@link #SUMWAITINGTIMES}, while the denominator corresponds to
    * {@link #RATEOFDELAY}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEWAIT(
      Messages.getString("PerformanceMeasureType.WaitingTimeWait"),  //$NON-NLS-1$
      EstimationType.FUNCTIONOFEXPECTATIONS, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0),

   /**
    * Corresponds to the expectation of ratio version of
    * {@link #WAITINGTIMEWAIT}.
    *
    * @xmlconfig.title
    * @xmlconfig.pm
    */
   WAITINGTIMEWAITREP(
      Messages.getString("PerformanceMeasureType.WaitingTimeWaitRep"),  //$NON-NLS-1$
      EstimationType.EXPECTATIONOFFUNCTION, RowType.CONTACTTYPE,
      ColumnType.MAINPERIOD, false, true, 0);

   /**
    * @deprecated Use {@link #ABANDONMENTRATIO} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONMENTRATE = ABANDONMENTRATIO;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOAFTERAWT} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONMENTRATEAFTERAWT = ABANDONMENTRATIOAFTERAWT;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOBEFOREAWT} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONMENTRATEBEFOREAWT = ABANDONMENTRATIOBEFOREAWT;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONMENTRATEREP = ABANDONMENTRATIOREP;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIO} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONRATE = ABANDONMENTRATIO;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOAFTERAWT} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONRATEAFTERAWT = ABANDONMENTRATIOAFTERAWT;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOBEFOREAWT} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONRATEBEFOREAWT = ABANDONMENTRATIOBEFOREAWT;

   /**
    * @deprecated Use {@link #ABANDONMENTRATIOREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType ABANDONRATEREP = ABANDONMENTRATIOREP;

   /**
    * @deprecated Use {@link #BLOCKRATIO} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType BLOCKRATE = BLOCKRATIO;

   /**
    * @deprecated Use {@link #BLOCKRATIOREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType BLOCKRATEREP = BLOCKRATIOREP;

   /**
    * @deprecated Use {@link #TIMETOABANDON} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType PATIENCETIME = TIMETOABANDON;

   /**
    * @deprecated Use {@link #TIMETOABANDONREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType PATIENCETIMEREP = TIMETOABANDONREP;

   /**
    * @deprecated Use {@link #DELAYRATIO} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType POSWAITRATIO = DELAYRATIO;

   /**
    * @deprecated Use {@link #DELAYRATIOREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType POSWAITRATIOREP = DELAYRATIOREP;

   /**
    * @deprecated Use {@link #SERVICELEVEL} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType QOS = SERVICELEVEL;

   /**
    * @deprecated Use {@link #SERVICELEVEL2} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType QOS2 = SERVICELEVEL2;

   /**
    * @deprecated Use {@link #SERVICELEVEL2REP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType QOS2REP = SERVICELEVEL2REP;

   /**
    * @deprecated Use {@link #SERVICELEVELREP} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType QOSREP = SERVICELEVELREP;

   /**
    * @deprecated Use {@link #RATEOFDELAY} instead.
    */
   @Deprecated
   public static final PerformanceMeasureType RATEOFPOSWAIT = RATEOFDELAY;

   // Ajouter
   public static final PerformanceMeasureType MseReal = MSEWAITINGTIME;
   // Ajouter
   public static final PerformanceMeasureType MseVQ = MSEWAITINGTIMEVQ;

   /**
    * Constructs and returns the agent-to-contact traffic matrix for the contact
    * center evaluation system \texttt{eval}. This traffic matrix has dimensions
    * $I'\times K$, where $I'=I+1$ if $I>1$, and $I$ otherwise. Element $(i, k)$
    * of the matrix gives the fraction of contacts of type $k$ served by agents
    * in group $i$ over the total number of contacts served by agents in group
    * $i$. This fraction is 0 if the corresponding routing is not allowed.
    * Element $(I, k)$ gives the total fraction of contacts of type $k$ served
    * by any agent. Each column of a given row always sums to 1. This matrix is
    * computed from the served rates (see {@link #SERVEDRATES}).
    *
    * @param eval
    *           the evaluation system.
    * @return the agent-to-contact traffic matrix.
    * @exception NullPointerException
    *               if \texttt{eval} is \texttt{null}.
    * @exception IllegalStateException
    *               if {@link ContactCenterEval#eval} was never called on
    *               \texttt{eval}.
    * @exception NoSuchElementException
    *               if the {@link #SERVEDRATES} performance measure type is not
    *               supported by \texttt{eval}.
    */
   public static DoubleMatrix2D getAgentToContactTrafficMatrix(
      ContactCenterEval eval) {
      final DoubleMatrix2D sr = eval.getPerformanceMeasure(SERVEDRATES);
      final int K = eval.getNumContactTypes();
      final int I = eval.getNumAgentGroups();
      final DoubleMatrix2D res = sr.like(I > 1 ? I + 1 : I, K);
      // assert res.rows () == sr.columns ();
      for (int i = 0; i < res.rows(); i++)
         if (K == 1)
            res.setQuick(i, 0, 1.0);
         else {
            final double sum = sr.getQuick(sr.rows() - 1, i);
            for (int k = 0; k < K; k++)
               res.setQuick(i, k, sr.getQuick(k, i) / sum);
         }
      return res;
   }

   /**
    * Constructs and returns the contact-to-agent traffic matrix for the contact
    * center evaluation system \texttt{eval}. This traffic matrix has dimensions
    * $K'\times I$, where $K'=K+1$ if $K>1$, and $K$ otherwise. Element $(k, i)$
    * of the matrix gives the fraction of contacts of type $k$ sent to agents in
    * group $i$, over the total number of served contacts of type~$k$. This
    * fraction is 0 if the corresponding routing is not allowed. Element $(K,
    * i)$ gives the total fraction of contacts served by agents in group $i$.
    * Each column of a given row always sums to 1. This matrix is computed from
    * the served rates (see {@link #SERVEDRATES}).
    *
    * @param eval
    *           the evaluation system.
    * @return the contact-to-agent traffic matrix.
    * @exception NullPointerException
    *               if \texttt{eval} is \texttt{null}.
    * @exception IllegalStateException
    *               if {@link ContactCenterEval#eval} was never called on
    *               \texttt{eval}.
    * @exception NoSuchElementException
    *               if the {@link #SERVEDRATES} performance measure type is not
    *               supported by \texttt{eval}.
    */
   public static DoubleMatrix2D getContactToAgentTrafficMatrix(
      ContactCenterEval eval) {
      final DoubleMatrix2D sr = eval.getPerformanceMeasure(SERVEDRATES);
      final int K = eval.getNumContactTypes();
      final int I = eval.getNumAgentGroups();
      final DoubleMatrix2D res = sr.like(K > 1 ? K + 1 : K, I);
      // assert res.rows () == sr.rows ();
      for (int k = 0; k < res.rows(); k++)
         if (I == 1)
            res.setQuick(k, 0, 1.0);
         else {
            final double sum = sr.getQuick(k, sr.columns() - 1);
            for (int i = 0; i < I; i++)
               res.setQuick(k, i, sr.getQuick(k, i) / sum);
         }
      return res;
   }

   private ColumnType columnType;

   private EstimationType estimationType;

   private String name;

   private boolean percent;

   private RowType rowType;

   private boolean time;

   private double zeroOverZeroValue;

   private PerformanceMeasureType(String name, EstimationType estimationType,
                                  RowType rowType, ColumnType columnType, boolean percent, boolean time,
                                  double zeroOverZeroValue) {
      this.name = name;
      this.estimationType = estimationType;
      this.rowType = rowType;
      this.columnType = columnType;
      this.percent = percent;
      this.time = time;
      this.zeroOverZeroValue = zeroOverZeroValue;
   }

   /**
    * Returns the name associated with the column \texttt{col} in the matrix of
    * results for this type of performance measure estimated by \texttt{eval}.
    * For example, this may return \texttt{period 0} if called with index~0 for
    * most performance measures.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param col
    *           the column index.
    * @return the column name.
    */
   public String columnName(ContactCenterInfo eval, int col) {
      return columnType.getName(eval, col);
   }

   /**
    * Returns the properties associated with the column \texttt{column} in a
    * matrix of results for this type of performance measure estimated by
    * \texttt{eval}.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param column
    *           the column index.
    * @return the column properties.
    */
   public Map<String, String> columnProperties(ContactCenterInfo eval,
         int column) {
      return columnType.getProperties(eval, column);
   }

   /**
    * Returns the number of columns in a matrix of performance measures of this
    * type estimated by the evaluation system \texttt{eval}.
    *
    * @param eval
    *           the queried evaluation system.
    * @return the number of columns.
    */
   public int columns(ContactCenterInfo eval) {
      return columnType.count(eval);
   }

   /**
    * Returns the title that should identify the columns of the matrix of
    * results for this type of performance measure. This returns
    * \texttt{Periods} for most performance measures.
    *
    * @return the column title.
    */
   public String columnTitle() {
      return columnType.getTitle();
   }

   /**
    * Returns the type of the columns in any matrix of this type of performance
    * measure. Usually, columns represent main periods.
    *
    * @return the column type.
    */
   public ColumnType getColumnType() {
      return columnType;
   }

   /**
    * Returns the descriptive name of this group of performance measures. The
    * returned name is intended to be used in reports, while the name returned
    * by the method {@link #name()} corresponds to the internal name of this
    * type of performance measure, used in programs.
    *
    * @return the name of the group of performance measures.
    */
   public String getDescription() {
      return name;
   }

   /**
    * Returns the type of estimation specified for this type of performance
    * measure. This can be an expectation, a ratio of expectations, an
    * expectation of ratios, or a raw statistic.
    *
    * @return the type of estimation for this performance measure type.
    */
   public EstimationType getEstimationType() {
      return estimationType;
   }

   /**
    * Returns the type of the rows in any matrix of this type of performance
    * measure. For example, rows can represent contact types, or agent groups.
    *
    * @return the row type.
    */
   public RowType getRowType() {
      return rowType;
   }

   /**
    * Determines the value associated with the undefined $0/0$ ratio, for
    * performance measures of this type.
    *
    * @return the value assocaited with $0/0$.
    */
   public double getZeroOverZeroValue() {
      return zeroOverZeroValue;
   }

   /**
    * Returns \texttt{true} if and only if performance measures of this type can
    * be expressed in percentage. Such measures are ratios defined on $[0, 1]$,
    * e.g., the service level, and may be formatted in percentage by reporting
    * facilities.
    *
    * @return \texttt{true} if and only if performance measures of this type can
    *         be expressed as percentages.
    */
   public boolean isPercentage() {
      return percent;
   }

   /**
    * Determines if performance measures of this type represent time durations.
    * This includes, e.g., waiting times and service times. Times produced by
    * evaluation systems are expressed in the default unit returned by
    * {@link ContactCenterEval#getDefaultUnit()}. Reporting facilities can
    * convert this time to the appropriate visual representation.
    *
    * @return \texttt{true} if and only if performance measures of this type
    *         represent times.
    */
   public boolean isTime() {
      return time;
   }

   /**
    * Returns the name associated with the row \texttt{row} in a matrix of
    * results for this type of performance measure estimated by \texttt{eval}.
    * For example, if the method is called for the service level, and row~0, it
    * may return \texttt{inbound type 0}.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param row
    *           the row index.
    * @return the row name.
    */
   public String rowName(ContactCenterInfo eval, int row) {
      return rowType.getName(eval, row);
   }

   /**
    * Returns the properties associated with the row \texttt{row} in a matrix of
    * results for this type of performance measure estimated by \texttt{eval}.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param row
    *           the row index.
    * @return the row properties.
    */
   public Map<String, String> rowProperties(ContactCenterInfo eval, int row) {
      return rowType.getProperties(eval, row);
   }

   /**
    * Returns the number of rows in a matrix of performance measures of this
    * type estimated by the evaluation system \texttt{eval}.
    *
    * @param eval
    *           the queried evaluation system.
    * @return the number of rows.
    */
   public int rows(ContactCenterInfo eval) {
      return rowType.count(eval);
   }

   /**
    * Returns the title that should identify the rows of matrices of results for
    * this type of performance measure. For example, this may return
    * \texttt{Groups} for agents' occupancy ratio.
    *
    * @return the row title.
    */
   public String rowTitle() {
      return rowType.getTitle();
   }
}
