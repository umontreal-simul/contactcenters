package umontreal.iro.lecuyer.contactcenters.app;


/**
 * \javadoc{Represents the type of estimation specified for a group of performance
 * measures.
 * }The estimation type gives clues on how
 * performance measures are estimated.
 * 
 * @xmlconfig.title Supported estimation types
 */
public enum EstimationType {
   /**
    * Raw statistics which do not estimate expectations. For example,
    * this can be the maximal queue size during a simulation,
    * which has no average or sample variance.
    * When simulating multiple replications, one observation of
    * each raw statistic is available for each replication.
    * On the other hand, if a single replication is simulated, which
    * occurs when using batch means, only a single
    * observation of the raw statistics is generated.
    * 
    * @xmlconfig.title
    */
   RAWSTATISTIC,

   /**
    * Estimation of an expectation, by
    * an average in the case of simulation.
    * Most expectations correspond to rates, which are
    * part of groups of performance measures
    * whose names begin with \texttt{RATEOF}, and which are expected counts of
    * certain event types occurring during a time interval, For example,
    * {@link PerformanceMeasureType#RATEOFABANDONMENT} is defined as the expected rate of contacts
    * having abandoned without receiving service during some time interval.
    * Types of performance measures whose names begin with
    * \texttt{SUM} are also normalized the same way as rates.
    * By default, rates are considered relative to one main period, so
    * {@link PerformanceMeasureType#RATEOFABANDONMENT} corresponds to the expected number
    * of contacts having abandoned during a main period.
    * However, if the \texttt{normalize\-To\-Default\-Unit}
    * attribute in simulation parameters is set to \texttt{true},
    * rates are treated as relative to one simulation time unit.
    * Expected
    * time-averages, which are not normalized as rates,
    * are part of groups with names beginning with \texttt{AVG},
    * e.g., {@link PerformanceMeasureType#AVGQUEUESIZE} for the time-average queue size.
    * 
    * @xmlconfig.title
    */
   EXPECTATION,

   /**
    * Estimation of a function of multiple expectations, e.g., a
    * ratio of expectations.
    * Functions of expectations, estimated by functions of averages in the case of
    * simulation, are part of groups whose names 
    * do not have the \texttt{RATEOF} or \texttt{AVG} prefixes, e.g.,
    * {@link PerformanceMeasureType#SERVICELEVEL}, and
    * {@link PerformanceMeasureType#ABANDONMENTRATIO}. For now, these
    * functions are ratios estimated as follows.
    * Let $(X_0, Y_0), \ldots, (X_{n-1}, Y_{n-1})$ be
    * random vectors generated during an experiment. Pairs of
    * observations can come from independent replications or from batches,
    * depending on the method of experiment.
    * Assuming that 
    * \[\bar{X}_n=\frac{1}{n}\sum_{r=0}^{n-1} X_r\to \E[X]\]
    * and
    * \[\bar{Y}_n=\frac{1}{n}\sum_{r=0}^{n-1} Y_r\to \E[Y]\]
    * as $n\to\infty$,
    * a simulator estimates the ratio by computing
    * \[\bar{\nu}_n=\frac{\bar{X}_n}{\bar{Y}_n}\]
    * which is an estimator of
    * \[\frac{\E[X]}{\E[Y]}=\nu.\]
    * At the end of an experiment, a single copy of the
    * estimator is available, and only sample variance and confidence interval are
    * available for $\bar{\nu}_n$, not observations.
    * 
    * @xmlconfig.title
    */
   FUNCTIONOFEXPECTATIONS,

   /**
    * Estimation of the expectation of a function of several
    * random variables whose expectations are themselves represented by other
    * types of performance measures. For example, this can be the expectation of
    * a ratio.
    * Expectations of functions are part of groups
    * with names 
    * having the \texttt{REP} suffix, and
    * have corresponding functions of expectations. They are not recommended for
    * analysis, because their estimators, averages of functions, are more noisy
    * than functions of averages. They correspond to
    * \[
    * \frac{1}{n}\sum_{r=0}^{n-1}
    * \frac{X_r}{Y_r},
    * \]
    * an estimator of
    * \[\E\left[\frac{X}{Y}\right].\]
    * When $n\to\infty$, this also estimates $\E[X]/\E[Y]$. An average of
    * ratios can be
    * used to estimate a short-term expectation. It is needed when several
    * observations are necessary to compute statistics different from average,
    * sample variance, and confidence intervals, e.g., quantiles.
    * 
    * @xmlconfig.title
    */
   EXPECTATIONOFFUNCTION
}
