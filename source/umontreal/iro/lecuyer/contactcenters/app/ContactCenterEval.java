package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Map;
import java.util.NoSuchElementException;

import jxl.write.WritableWorkbook;
import umontreal.iro.lecuyer.contactcenters.app.params.ReportParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Represents a system evaluating some performance measures of
 * a contact center.
 * An \emph{evaluation} is a process estimating some
 * performance measures according to user-defined parameters.
 * It can be performed by approximation formulas or by simulations while
 * parameters can be obtained from any data source, e.g., files, programs, etc.
 *
 * This interface represents an evaluation system adapted
 * for contact centers.  Each system uses internal, implementation-specific
 * parameters usually read from files and stored into
 * dedicated objects, e.g., {@link CallCenterParams}.
 * It also defines some external parameters called \emph{evaluation options}
 * which can be modified through this interface, by using the
 * {@link #setEvalOption} method.
 * Evaluations are triggered by using the {@link #eval} method,
 * and matrices of performance measures are accessible
 * through the {@link #getPerformanceMeasure} method.
 *
 * The methods {@link #setEvalOption} and {@link #getPerformanceMeasure} are
 * generic methods accepting an argument indicating the evaluation
 * option or group of performance measures of interest, respectively.
 * This design allows the interface to be extended in the future,
 * by adding new options or groups of measures, without affecting
 * current implementations.
 *
 * The construction of the contact center and
 * the supported evaluation options and performance
 * measures are specific to the implementation of
 * this interface, but the access to the evaluation
 * mechanism is unified.
 * A contact center evaluation system is not forced
 * to support all of the defined
 * performance measures and evaluation options, because the
 * interface specifies facilities to enumerate supported
 * groups of performance measures and evaluation options, and
 * to test the support of a specific
 * measure or option.
 */
public interface ContactCenterEval extends ContactCenterInfo {
   /**
    * Returns the array of the evaluation option types
    * supported by the implementing object.
    * The evaluation options are the variable
    * parameters of the contact center
    * which can be changed between
    * calls to {@link #eval}.
    * This should never return a \texttt{null} pointer;
    * if no evaluation options are supported for some reasons,
    * this should return an array with length 0.
    @return the array of supported evaluation options.
    */
   public EvalOptionType[] getEvalOptions();

   /**
    * Determines if the evaluation option \texttt{option}
    * is supported by the implemented system.
    * It should return \texttt{true} if and only if
    * the \texttt{option} is in the array returned by
    * {@link #getEvalOptions}. Otherwise, it
    * returns \texttt{false}.
    @param option the queried evaluation option.
    @return the support status of the option.
    @exception NullPointerException if \texttt{option} is \texttt{null}.
    */
   public boolean hasEvalOption (EvalOptionType option);

   /**
    * Returns the current value of the evaluation option
    * \texttt{option}. The class of the returned object
    * must be assignable to
    * the class given by {@link EvalOptionType#getType}.
    * If the option is not supported, this should
    * throw a {@link NoSuchElementException}.
    * This exception can be thrown if and only if
    * {@link #hasEvalOption} returns \texttt{false} for
    * the given \texttt{option}.
    @param option the queried evaluation option.
    @return the current value of the option.
    @exception NoSuchElementException if the option
    is not available.
    @exception NullPointerException if \texttt{option} is \texttt{null}.
    */
   public Object getEvalOption (EvalOptionType option);

   /**
    * Sets the evaluation option \texttt{option} to \texttt{value}.
    * If the given option is not supported, this throws
    * a {@link NoSuchElementException}. If the class of the given
    * value is incompatible, this throws a {@link ClassCastException}.
    * If the evaluation option cannot be changed, this
    * throws an {@link UnsupportedOperationException}.
    @param option the option to be set.
    @param value the new value of the option.
    @exception NoSuchElementException if the given option
    is not supported.
    @exception ClassCastException if the given value is
    from an incompatible class.
    @exception IllegalArgumentException if the given value
    is invalid.
    @exception UnsupportedOperationException if the
    evaluation option is read-only.
    @exception NullPointerException if the option is \texttt{null} or
    the value is unexpectedly \texttt{null}.
    */
   public void setEvalOption (EvalOptionType option, Object value);

   /**
    * Evaluates the contact center's supported
    * performance measures using
    * the current values of implementation-specific
    * parameters and evaluation
    * options.  If parameters or seeds of random streams
    * are not changed,
    * multiple calls to this method should return
    * the same results. In consequence of this,
    * simulation should reset the random number
    * streams before returning.
    * This method can throw an
    * {@link IllegalStateException} if there is
    * an inconsistency in the system's parameters.
    * It can throw an {@link ArithmeticException} if,
    * for the given parameters, the performance
    * measures cannot be estimated.
    @exception IllegalStateException if there are
    invalid parameter values.
    @exception ArithmeticException if no solution
    can be computed.
    */
   public void eval();

   /**
    * Determines if the system seems to be unstable.
    * When a system is unstable, the returned steady-state
    * performance measures are not reliable.
    * This method mainly applies for stationary
    * simulators which return \texttt{true}
    * when the system appears to be unstable, i.e.,
    * some waiting queues grow up with simulation time.
    * The method must throw an {@link IllegalStateException}
    * if it is called before {@link #eval} and
    * always return \texttt{false} if no
    * stability check applies.
    @return the result of the stability check.
    @exception IllegalStateException if this method
    is called before the evaluation is performed.
    */
   public boolean seemsUnstable();

   /**
    * Represents information about this evaluation system
    * that should be included in any report
    * produced by {@link #formatStatistics()}.
    * The information is organized as
    * (key, value) pairs in a map.  This information may include
    * steps of an approximation, number of iterations, etc.
    * One can modify the returned map to add
    * custom information.
    * The content of this map should not
    * affect the process of the evaluation;
    * it is used only for building
    * the statistical report.
    * One can use evaluation options for
    * system parameters.
    * @return the evaluation information.
    */
   public Map<String, Object> getEvalInfo();

   /**
    * Formats information about every performance
    * measure after {@link #eval} is called.
    * It can be simulation statistics, information
    * about the steps of an approximation algorithm,
    * or simply the values of all performance measures.
    * This method should call {@link #getEvalInfo()}
    * to obtain general information about the evaluation
    * and incorporate the information into the
    * returned string.
    * For each entry in the map, the method
    * should add a \texttt{key: value} line
    * in the string.
    * Then, the method appends the performance measures
    * to the returned string.
    * The {@link PerformanceMeasureFormatText} class
    * can be used to convert matrices of performance measures
    * into strings.
    * If the evaluation was not triggered by
    * calling {@link #eval} before this method is called,
    * an {@link IllegalStateException} is thrown.
    * If no statistical information is available even
    * after the evaluation, this method should return
    * an empty string instead of throwing an exception.
    @return a string containing a statistical report.
    @exception IllegalStateException if no statistical information
    is available because the evaluation was not triggered.
    */
   public String formatStatistics();

   /**
    * Formats and returns a statistical report that
    * can be included into a \LaTeX\ document.
    * This is similar to {@link #formatStatistics()},
    * except the generated report is in \LaTeX\ rather than
    * plain text.
    * @return the formatted report.
    */
   public String formatStatisticsLaTeX();

   /**
    * Constructs and returns an JExcel API workbook containing
    * the results of the evaluation, and appends the
    * contents of the generated report to the workbook
    * \texttt{wb}.
    * This method may add multiple sheets, e.g.,
    * for general and detailed information.
    * This method should add the information
    * in the map returned by
    * {@link #getEvalInfo()} to a sheet in the
    * workbook.
    * This method returns \texttt{true} if and only if the given
    * workbook was modified.
    *
    * One can then customize the returned workbook as needed.
    * The method {@link WritableWorkbook#write()} can
    * be used to export the workbook to an output
    * stream.
    * This can be used to create files that can be opened
    * directly by Microsoft Excel for
    * results analysis and reporting.
    * Excel documents can also be opened by
    * open source software such as OpenOffice.org,
    * KOffice, etc.
    * @param wb the workbook to append report to.
    * @return \texttt{true} if and only if the given workbook was modified.
    */
   public boolean formatStatisticsExcel (WritableWorkbook wb);

   /**
    * Returns the parameters for reports formatted by
    * {@link #formatStatistics()}, or
    * {@link #formatStatisticsExcel(WritableWorkbook)}.
    * If no object containing report parameters
    * is available, this method
    * should create a new one using
    * the default constructor of
    * {@link ReportParams}.
    * @return the printed statistics.
    */
   public ReportParams getReportParams();

   /**
    * Sets the report parameters to
    * \texttt{report\-Params}.
    * @param reportParams the report parameters..
    * @see #getReportParams()
    */
   public void setReportParams (ReportParams reportParams);

   /**
    * Returns an array containing all the groups of performance
    * measures
    * this object can estimate.  If no performance measure is
    * supported by a given implementation, this method should
    * return an array with length 0 instead of \texttt{null}.
    @return the array of groups of performance measures.
    */
   public PerformanceMeasureType[] getPerformanceMeasures();

   /**
    * Determines if the evaluation system
    * can estimate performance measures in group \texttt{m}.
    * This should return \texttt{true} if and only if the group of performance
    * measures is listed in the array returned by
    * {@link #getPerformanceMeasures}.
    @param m the group of performance measures being tested.
    @return a \texttt{true} value if the measure is supported,
    \texttt{false} otherwise.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public boolean hasPerformanceMeasure (PerformanceMeasureType m);

   /**
    * Returns the matrix of values corresponding to the
    * group of performance measures \texttt{m} estimated by
    * the last call to {@link #eval}.  The dimensions
    * of the matrix and the role of its elements depend on the
    * queried group of performance
    * measures, and the capabilities of the implementing
    * evaluation system.  See the {@link PerformanceMeasureType}
    * class for more information about the defined performance
    * measures.
    * If the queried measure is not supported by this evaluation object, this throws a
    * {@link NoSuchElementException}.  If the values of
    * the measures are not available, e.g., the {@link #eval}
    * method was never called after the last call to {@link #reset},
    * this throws an {@link IllegalStateException}.
    @param m the queried group of performance measures.
    @return the matrix of values computed by the evaluation system.
    @exception NoSuchElementException if the given performance
    measure is not supported by this object.
    @exception IllegalStateException if the values are not
    available.
    @exception NullPointerException if \texttt{m} is \texttt{null}.
    */
   public DoubleMatrix2D getPerformanceMeasure (PerformanceMeasureType m);

   /**
    * Resets this contact center evaluation system
    * for new parameters.
    * Every cached or processed parameter should be
    * reread from the parameter objects.
    * In the case of the simulation, one should
    * try to preserve random number seeds whenever
    * possible, even if the contact center needs to
    * be reconstructed.  Some implementatios of this
    * interface provide specialized reset methods allowing
    * to change the associated parameter objects.
    */
   public void reset();

   /**
    * Determines if the implementation should print
    * information during the evaluation of the performance
    * measures.  The information depends on the
    * type of evaluation method being involved.
    * It can include steps or iterations of an approximation algorithm, or
    * information about important elements of a simulation.
    * Summary information should be printed on the standard output while
    * debugging data, e.g., traces of every event in a simulation, should be
    * sent to log files if they are generated.
    * By default, the verbose mode is disabled.
    @return \texttt{true} if the implementation is in verbose
    mode, \texttt{false} otherwise (the default).
    */
   public boolean isVerbose();

   /**
    * Sets the verbose status to \texttt{v}.
    * If \texttt{v} is \texttt{true}, verbose mode is enabled.
    * Otherwise, it is disabled.
    @param v \texttt{true} to activate verbose mode, \texttt{false} to disable it.
    @see #isVerbose
    */
   public void setVerbose (boolean v);
}
