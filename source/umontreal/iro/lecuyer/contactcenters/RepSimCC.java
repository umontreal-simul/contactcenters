package umontreal.iro.lecuyer.contactcenters;

import java.util.ArrayList;
import java.util.List;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.simexp.RepSim;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Extends {@link RepSim} to use measure matrices as counters, to compute
 * observations. This class defines a list matrices of measures that can be added to.
 * At the beginning of each replication, the matrices are initialized, and the
 * program updates them. Each column of such matrices usually corresponds to a
 * period as defined by a period-change event. At the end of the replication, in
 * the {@link #addReplicationObs} method, values are extracted from the matrices
 * before they are added to matrices of tallies.
 */
public abstract class RepSimCC extends RepSim {
   private final List<MeasureMatrix> matrices = new ArrayList<MeasureMatrix> ();

   /**
    * Calls {@link RepSim#RepSim(int) super} \texttt{(minReps)}.
    */
   public RepSimCC (int minReps) {
      this (Simulator.getDefaultSimulator (), minReps);
   }

   /**
    * Calls {@link RepSim#RepSim(Simulator,int)}.
    */
   public RepSimCC (Simulator sim, int minReps) {
      super (sim, minReps);
   }

   /**
    * Calls {@link RepSim#RepSim(int,int) super} \texttt{(minReps, maxReps)}.
    */
   public RepSimCC (int minReps, int maxReps) {
      super (minReps, maxReps);
   }

   /**
    * Calls {@link RepSim#RepSim(Simulator,int,int)}.
    */
   public RepSimCC (Simulator sim, int minReps, int maxReps) {
      super (sim, minReps, maxReps);
   }

   /**
    * Returns the matrices of measures registered to this object. These matrices
    * must be capable of supporting multiple periods. The returned list should
    * contain non-\texttt{null} instances of {@link MeasureMatrix} only.
    *
    * @return the list of measure matrices.
    */
   public List<MeasureMatrix> getMeasureMatrices () {
      return matrices;
   }

   /**
    * This method is overridden to initialize the matrices of measures after
    * the simulator is initialized.
    */
   @Override
   public void performReplication (int r) {
      simulator().init ();
      ContactCenter.initElements (getMeasureMatrices ());
      initReplication (r);
      simulator().start ();
      replicationDone ();
      addReplicationObs (r);
   }

   /**
    * Computes the matrix of observations from the matrix of measures
    * \texttt{mat}, and stores the result in \texttt{m}. The returned matrix has
    * the same number of rows as the number of measures and the same number of
    * columns as the number of periods. Element \texttt{(r, c)} of the matrix is
    * given by \texttt{mat.getMeasure (r, c)}.
    *
    * @param mat
    *           the matrix of measures for which observations are queried.
    * @param m
    *           the matrix of \texttt{double}'s filled with the result.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the dimensions of the matrix are invalid.
    * @exception NullPointerException
    *               if \texttt{mat} or \texttt{m} are \texttt{null}.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat,
         DoubleMatrix2D m) {
      final int nm = mat.getNumMeasures ();
      final int np = mat.getNumPeriods ();
      if (m.rows () != nm || m.columns () != np)
         throw new IllegalArgumentException ("Invalid matrix dimensions for "
               + mat.toString () + ", found " + m.rows () + "x" + m.columns ()
               + ", but needs " + nm + "x" + np);
      for (int p = 0; p < np; p++)
         for (int i = 0; i < nm; i++)
            m.setQuick (i, p, mat.getMeasure (i, p));
      return m;
   }

   /**
    * Constructs a matrix \texttt{m} with as many rows as the number of measures
    * in \texttt{mat} and as many columns as the number of periods, calls
    * {@link #getReplicationValues(MeasureMatrix,DoubleMatrix2D) get\-Replication\-Values}
    * \texttt{(mat, m)} to fill the matrix, and returns it.
    *
    * @param mat
    *           the measure matrix for which observations are queried.
    * @return the matrix filled with the result.
    * @exception NullPointerException
    *               if \texttt{mat} is \texttt{null}.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat) {
      final DoubleMatrix2D m = new DenseDoubleMatrix2D (mat.getNumMeasures (),
            mat.getNumPeriods ());
      getReplicationValues (mat, m);
      return m;
   }

   /**
    * Computes the matrix of observations for the measure matrix \texttt{mat}
    * and stores the result in \texttt{m}. It is assumed that the matrix
    * contains observations for \texttt{np} periods, including a preliminary and
    * a wrap-up periods. The matrix \texttt{m} must contain observations for
    * main periods only as well as the time-aggregate observations. If
    * \texttt{preliminary} is set to \texttt{true}, the observations of the
    * preliminary period will be included in the time-aggregate count. If
    * \texttt{wrapup} is \texttt{true}, the observations in the wrap-up period
    * will be included. If \texttt{mainPeriods} is \texttt{null}, all main
    * periods will be included in the aggregate values. Otherwise, the value for
    * (main) period \texttt{p} will be included in the aggregated sum if and
    * only if \texttt{mainPeriods[p - 1]} is \texttt{true}.
    *
    * Each column of the matrix corresponds to one period and the last column
    * contains the values for the whole replication. Each row corresponds to one
    * type of measure.
    *
    * @param mat
    *           the measure matrix for which observations are queried.
    * @param m
    *           the matrix filled with the result.
    * @param preliminary
    *           if the preliminary period is included in the last column of the
    *           matrix.
    * @param wrapup
    *           if the wrap-up period is included in the last column of the
    *           matrix.
    * @param mainPeriods
    *           indicates which main periods are included in the aggregate
    *           measure.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the dimensions of the matrix are invalid, or if
    *               \texttt{mainPeriods} is non-\texttt{null} and has an invalid
    *               length.
    * @exception NullPointerException
    *               if \texttt{mat} or \texttt{m} are \texttt{null}.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat,
         DoubleMatrix2D m, boolean preliminary, boolean wrapup,
         boolean[] mainPeriods) {
      final int nm = mat.getNumMeasures ();
      final int np = mat.getNumPeriods ();
      if (m.rows () != nm || m.columns () != np - 1)
         throw new IllegalArgumentException ("Invalid matrix dimensions for "
               + mat.toString () + ", found " + m.rows () + "x" + m.columns ()
               + ", but needs " + nm + "x" + (np - 1));
      if (mainPeriods != null && mainPeriods.length != np - 2)
         throw new IllegalArgumentException ("Invalid length of mainPeriods: "
               + mainPeriods.length);
      for (int i = 0; i < nm; i++)
         m.setQuick (i, np - 2, preliminary ? mat.getMeasure (i, 0) : 0);
      for (int p = 1; p < np - 1; p++)
         for (int i = 0; i < nm; i++) {
            final double v = mat.getMeasure (i, p);
            m.setQuick (i, p - 1, v);
            if (mainPeriods == null || mainPeriods[p - 1])
               m.setQuick (i, np - 2, m.getQuick (i, np - 2) + v);
         }
      if (wrapup)
         for (int i = 0; i < nm; i++)
            m.setQuick (i, np - 2, m.getQuick (i, np - 2)
                  + mat.getMeasure (i, np - 1));
      return m;
   }

   /**
    * Equivalent to
    * {@link #getReplicationValues(MeasureMatrix,DoubleMatrix2D,boolean,boolean,boolean[]) getReplicationValues}
    * \texttt{(mat, m, preliminary, wrapup, null)}.
    *
    * @param mat
    *           the measure matrix for which observations are queried.
    * @param m
    *           the matrix filled with the result.
    * @param preliminary
    *           if the preliminary period is included in the last column of the
    *           matrix.
    * @param wrapup
    *           if the wrap-up period is included in the last column of the
    *           matrix.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the dimensions of the matrix are invalid.
    * @exception NullPointerException
    *               if \texttt{mat} or \texttt{m} are \texttt{null}.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat,
         DoubleMatrix2D m, boolean preliminary, boolean wrapup) {
      return getReplicationValues (mat, m, preliminary, wrapup, null);
   }

   /**
    * Computes the matrix of observations for the measure matrix \texttt{mat},
    * and returns the result in a matrix. This method uses
    * {@link #getReplicationValues(MeasureMatrix,DoubleMatrix2D,boolean,boolean,boolean[]) getReplicationValues}
    * for the computation.
    *
    * @param mat
    *           the measure matrix for which observations are queried.
    * @param preliminary
    *           if the preliminary period is included in the last column of the
    *           matrix.
    * @param wrapup
    *           if the wrap-up period is included in the last column of the
    *           matrix.
    * @param mainPeriods
    *           indicates which main periods are included in the aggregated
    *           measure.
    * @return the matrix filled with the result.
    * @exception NullPointerException
    *               if \texttt{mat} or \texttt{m} are \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{mainPeriods} is not \texttt{null} and has an
    *               invalid length.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat,
         boolean preliminary, boolean wrapup, boolean[] mainPeriods) {
      final DoubleMatrix2D m = new DenseDoubleMatrix2D (mat.getNumMeasures (),
            mat.getNumPeriods () - 1);
      getReplicationValues (mat, m, preliminary, wrapup, mainPeriods);
      return m;
   }

   /**
    * Equivalent to
    * {@link #getReplicationValues(MeasureMatrix,boolean,boolean,boolean[]) getReplicationValues}
    * \texttt{(mat, preliminary, wrapup, null)}.
    *
    * @param mat
    *           the measure matrix for which observations are queried.
    * @param preliminary
    *           if the preliminary period is included in the last column of the
    *           matrix.
    * @param wrapup
    *           if the wrap-up period is included in the last column of the
    *           matrix.
    * @return the matrix filled with the result.
    * @exception NullPointerException
    *               if \texttt{mat} or \texttt{m} are \texttt{null}.
    */
   public static DoubleMatrix2D getReplicationValues (MeasureMatrix mat,
         boolean preliminary, boolean wrapup) {
      final DoubleMatrix2D m = new DenseDoubleMatrix2D (mat.getNumMeasures (),
            mat.getNumPeriods () - 1);
      getReplicationValues (mat, m, preliminary, wrapup, null);
      return m;
   }

   /**
    * Normalizes the matrix \texttt{m} using simulation time. Usually, this
    * method receives a matrix produced by {@link #getReplicationValues}. It
    * assumes that each row corresponds to a count or an integral and one column
    * corresponds to a main period. If there is one more column than the number
    * of main periods, the last column corresponds to values for the whole
    * replication. Each element of the matrix is divided by a simulation time
    * determined by the period-change event \texttt{pce}. For each column
    * \texttt{c} corresponding to one main period, each row is divided by the
    * period duration obtained using
    * {@link PeriodChangeEvent#getPeriodDuration Period\-Change\-Event.get\-Period\-Duration}
    * \texttt{(c + 1)}. For the column corresponding to the whole replication,
    * the rows are divided by the total simulation time \texttt{totalTime}.
    *
    * @param pce
    *           the period-change event defining the periods.
    * @param m
    *           the matrix being normalized.
    * @param totalTime
    *           the supplied total simulation time.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the number of columns of \texttt{m} is incorrect.
    */
   public static DoubleMatrix2D timeNormalize (PeriodChangeEvent pce,
         DoubleMatrix2D m, double totalTime) {
      final int numMainPeriods = pce.getNumMainPeriods ();
      if (m.columns () != numMainPeriods && m.columns () != numMainPeriods + 1)
         throw new IllegalArgumentException ("Invalid number of columns in m: "
               + m.columns ());
      for (int c = 0; c < numMainPeriods; c++) {
         final double t = pce.getPeriodDuration (c + 1);
         m.viewPart (0, c, m.rows (), 1).assign (Functions.div (t));
      }
      if (m.columns () == numMainPeriods + 1)
         m.viewPart (0, m.columns () - 1, m.rows (), 1).assign (
               Functions.div (totalTime));
      return m;
   }

   /**
    * Equivalent to
    * {@link #timeNormalize(PeriodChangeEvent,DoubleMatrix2D,double)} with
    * automatic computation of total simulation time. The total time is computed
    * by summing the duration of the periods defined by \texttt{pce}. The
    * parameters \texttt{preliminary}, \texttt{wrapup} and \texttt{mainPeriods}
    * play the same role as with {@link #getReplicationValues}.
    *
    * @param pce
    *           the period-change event defining the periods.
    * @param m
    *           the matrix being normalized.
    * @param preliminary
    *           determines if the preliminary period is included in the
    *           time-aggregate values.
    * @param wrapup
    *           determines if the wrap-up period is included in the
    *           time-aggregate values.
    * @param mainPeriods
    *           indicates which main periods are included in the aggregated
    *           measure.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the number of columns of \texttt{m} is incorrect.
    */
   public static DoubleMatrix2D timeNormalize (PeriodChangeEvent pce,
         DoubleMatrix2D m, boolean preliminary, boolean wrapup,
         boolean[] mainPeriods) {
      final int numMainPeriods = pce.getNumMainPeriods ();
      if (mainPeriods != null && mainPeriods.length != numMainPeriods)
         throw new IllegalArgumentException ("Invalid length of mainPeriods: "
               + mainPeriods.length);
      double total = 0;
      for (int c = 0; c < numMainPeriods; c++)
         if (mainPeriods == null || mainPeriods[c])
            total += pce.getPeriodDuration (c + 1);
      if (preliminary)
         total += pce.getPeriodDuration (0);
      if (wrapup)
         total += pce.getPeriodDuration (pce.getNumPeriods () - 1);
      return timeNormalize (pce, m, total);
   }

   /**
    * Equivalent to
    * {@link #timeNormalize(PeriodChangeEvent,DoubleMatrix2D,boolean,boolean,boolean[]) timeNormalize}
    * \texttt{(pce, m, preliminary, wrapup, null)}.
    *
    * @param pce
    *           the period change event defining the periods.
    * @param m
    *           the matrix being normalized.
    * @param preliminary
    *           determines if the preliminary period is included in the
    *           time-aggregate values.
    * @param wrapup
    *           determines if the wrap-up period is included in the
    *           time-aggregate values.
    * @return the given matrix \texttt{m}.
    * @exception IllegalArgumentException
    *               if the number of columns of \texttt{m} is incorrect.
    */
   public static DoubleMatrix2D timeNormalize (PeriodChangeEvent pce,
         DoubleMatrix2D m, boolean preliminary, boolean wrapup) {
      return timeNormalize (pce, m, preliminary, wrapup, null);
   }
}
