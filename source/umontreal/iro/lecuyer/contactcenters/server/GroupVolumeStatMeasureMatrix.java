package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.ssj.simevents.Simulator;
import umontreal.ssj.stat.mperiods.IntegralMeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureSet;

/**
 * Agent group statistical collector implementing
 * {@link MeasureMatrix}.
 * This class extends {@link GroupVolumeStat} and
 * implements the {@link MeasureMatrix} interface
 * and defines measures for the service, idle,
 * working, and total volumes.
 * The service volume corresponds to the integral
 * of the number of busy agents $\Nb[i](t)$ obtained by
 * {@link AgentGroup#getNumBusyAgents}.
 * The idle volume is the integral of the number of idle agents $\Ni[i](t)$
 * over the simulation time. This is obtained using
 * {@link AgentGroup#getNumIdleAgents}.
 * The working volume is the integral
 * of the number of working agents,
 * $\Nb[i](t)+\Nf[i](t)$ over the simulation time,
 * obtained by
 * {@link AgentGroup#getNumBusyAgents()}, and
 * {@link AgentGroup#getNumFreeAgents()}.
 * The total volume corresponds to the integral of the
 * number of agents
 * $\int_0^T (N_i(t)+\Ng[i](t)) dt=\int_0^T (\Nb[i](t)+\Ni[i](t)) dt$
 * over the simulation time.
 * This quantity is given by the sum of the accumulates
 * returned by the methods
 * {@link AgentGroup#getNumAgents} and
 * {@link AgentGroup#getNumGhostAgents}.
 * These quantities can be used to compute
 * the agent group's occupancy ratio, which is the
 * ratio of the service volume and total volume,
 * or the ratio of the service volume over the working volume.
 *
 * This class can be given the number of contact types~$K$ to
 * track for computing $\int_0^T \Nb[i, k](t)\ dt$.
 * If $K>1$, the measure $ 0\le k < K$ represents the integral of
 * the number of busy agents serving contacts of type~$k$
 * over the simulation time.
 * Measures $K$ through $K+3$ represents
 * respectively the
 * service, idle, working, and total volumes.
 *
 * When $K=1$, the measure~$0$ corresponds
 * to the service volume, the measure~1, to the idle volume, and
 * the measure~$2$ is the working volume, and
 * measure~$3$
 * is the total volume.
 *
 * Since this measure matrix supports only one period,
 * it must be combined with
 * {@link IntegralMeasureMatrix}
 * for one to get the measures for each period.
 */
public class GroupVolumeStatMeasureMatrix extends GroupVolumeStat implements
      MeasureMatrix {
   /**
    * Constructs a new agent-group volume statistical probe
    * observing the agent group \texttt{group} and only
    * computing aggregate statistics.
    * This is equivalent to
    * {@link #GroupVolumeStatMeasureMatrix(AgentGroup,int) Group\-Volume\-Stat}
    * \texttt{(group, 0)}.
    @param group the observed agent group.
    */
   public GroupVolumeStatMeasureMatrix (AgentGroup group) {
      super (group);
   }

   /**
    * Equivalent to {@link #GroupVolumeStatMeasureMatrix(AgentGroup)},
    * using the given simulator \texttt{sim}
    * for creating internal probes.
    */
   public GroupVolumeStatMeasureMatrix (Simulator sim, AgentGroup group) {
      super (sim, group);
   }

   /**
    * Constructs a new agent-group volume statistical probe
    * observing the agent group \texttt{group}, and
    * supporting \texttt{numTypes} contact types.
    @param group the observed agent group.
    @param numTypes the number of contact types.
    @exception IllegalArgumentException if \texttt{numTypes} is negative.
    */
   public GroupVolumeStatMeasureMatrix (AgentGroup group, int numTypes) {
      super (group, numTypes);
   }

   /**
    * Equivalent to {@link #GroupVolumeStatMeasureMatrix(AgentGroup,int)},
    * using the given simulator \texttt{sim}
    * for creating internal probes.
    */
   public GroupVolumeStatMeasureMatrix (Simulator sim, AgentGroup group, int numTypes) {
      super (sim, group, numTypes);
   }

   public int getNumMeasures() {
      return getNumContactTypes() + 4;
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    @exception UnsupportedOperationException if this method is called.
    */
   public void setNumMeasures (int nm) {
      throw new UnsupportedOperationException ("The number of measures cannot be changed");
   }

   public int getNumPeriods() {
      return 1;
   }

   /**
    * Throws an {@link UnsupportedOperationException}.
    @exception UnsupportedOperationException if this method is called.
    */
   public void setNumPeriods (int np) {
      throw new UnsupportedOperationException ("The number of periods cannot be changed");
   }

   public double getMeasure (int i, int p) {
      if (p != 0)
         throw new ArrayIndexOutOfBoundsException ("p");
      if (i < 0)
         throw new ArrayIndexOutOfBoundsException ("i");
      final int K = getNumContactTypes();
      if (i < K)
         return getStatNumBusyAgents (i).sum();
      else if (i == K)
         return getStatNumBusyAgents().sum();
      else if (i == K + 1)
         return getStatNumIdleAgents().sum();
      else if (i == K + 2)
         return getStatNumBusyAgents().sum() + getStatNumFreeAgents().sum();
      else if (i == K + 3)
         // Equivalent to busy+idle, greater than or equal to busy+free
         return getStatNumAgents().sum() + getStatNumGhostAgents().sum();
      else
         throw new ArrayIndexOutOfBoundsException ("i");
   }

   public void regroupPeriods (int x) {}

   /**
    * Returns a measure set regrouping
    * the service volumes for several agent groups and
    * computing the sum.
    * Row~\texttt{r} of the resulting matrix corresponds to
    * the service volume stored in \texttt{vcalc[r]}, and the last
    * row contains the sum of the service volumes.
    @param vcalc the agent group volume matrices.
    @return the service volume measure set.
    */
   public static MeasureSet getServiceVolumeMeasureSet
      (MeasureMatrix[] vcalc) {
      final MeasureSet mset = new MeasureSet();
      for (final MeasureMatrix mmat : vcalc)
         mset.addMeasure (mmat, mmat.getNumMeasures() - 4);
      return mset;
   }

   /**
    * Returns a measure set regrouping the service volumes
    * stored in \texttt{vcalc} for \texttt{numTypes} contact types.
    * Row \texttt{numTypes*i + k} of the resulting measure set
    * corresponds to the integral of the number of busy
    * agents in group~\texttt{i} serving contacts of type~\texttt{k}.
    * If the measure set is computing the sum row (the default),
    * row \texttt{numTypes*vcalc.length + k} gives the integral
    * of the total number of agents serving contacts with type~\texttt{k}.
    @param vcalc the agent group volume matrices.
    @param numTypes the number of contact types.
    @return the service volume measure set.
    */
   public static MeasureSet getServiceVolumeMeasureSet
      (MeasureMatrix[] vcalc, int numTypes) {
      final MeasureSet mset = numTypes > 1 ? new VCalc (numTypes) : new MeasureSet();
      for (int i = 0; i < vcalc.length; i++) {
         final int nt = vcalc[i].getNumMeasures() - 4;
         if (nt < numTypes)
            throw new IllegalArgumentException
               ("Not enough measures in vcalc[" + i + "]");
         for (int k = 0; k < numTypes; k++)
            mset.addMeasure (vcalc[i], k);
      }
      return mset;
   }

   /**
    * Returns a measure set regrouping
    * the idle volumes for several agent groups and
    * computing the sum.
    * Row~\texttt{r} of the resulting matrix corresponds to
    * the idle volume stored in \texttt{vcalc[r]}, and the last
    * row contains the sum of the idle volumes.
    @param vcalc the agent group volume matrices.
    @return the idle volume measure set.
    */
   public static MeasureSet getIdleVolumeMeasureSet
      (MeasureMatrix[] vcalc) {
      final MeasureSet mset = new MeasureSet();
      for (final MeasureMatrix mmat : vcalc)
         mset.addMeasure (mmat, mmat.getNumMeasures() - 3);
      return mset;
   }

   /**
    * Returns a measure set regrouping
    * the working volumes for several agent groups.
    * Row~\texttt{r} of the resulting matrix corresponds to
    * the working volume stored in \texttt{vcalc[r]}, and the last
    * row contains the sum of the working volumes.
    @param vcalc the agent group volume matrices.
    @return the working volume measure set.
    */
   public static MeasureSet getWorkingVolumeMeasureSet
      (MeasureMatrix[] vcalc) {
      final MeasureSet mset = new MeasureSet();
      for (final MeasureMatrix mmat : vcalc)
         mset.addMeasure (mmat, mmat.getNumMeasures() - 2);
      return mset;
   }

   /**
    * Returns a measure set regrouping the working volumes
    * stored in \texttt{vcalc}, with each working volume repeated
    * \texttt{numTypes} times.  This is used to create a measure set matching
    * with {@link #getServiceVolumeMeasureSet(MeasureMatrix[], int)}
    * to compute per-contact type agent's occupancy ratios.
    * If the measure set is computing the sum rows (the default),
    * the last \texttt{numTypes} rows contain the sum of the working volumes for
    * all agents.
    @param vcalc the agent group volume matrices.
    @param numTypes the number of contact types.
    @return the working volume measure set.
    */
   public static MeasureSet getWorkingVolumeMeasureSet
      (MeasureMatrix[] vcalc, int numTypes) {
      final MeasureSet mset = numTypes > 1 ? new VCalc (numTypes) : new MeasureSet();
      for (final MeasureMatrix mmat : vcalc) {
         final int idx = mmat.getNumMeasures() - 2;
         for (int k = 0; k < numTypes; k++)
            mset.addMeasure (mmat, idx);
      }
      return mset;
   }

   /**
    * Returns a measure set regrouping
    * the total volumes for several agent groups.
    * Row~\texttt{r} of the resulting matrix corresponds to
    * the total volume stored in \texttt{vcalc[r]}, and the last
    * row contains the sum of the total volumes.
    @param vcalc the agent group volume matrices.
    @return the total volume measure set.
    */
   public static MeasureSet getTotalVolumeMeasureSet
      (MeasureMatrix[] vcalc) {
      final MeasureSet mset = new MeasureSet();
      for (final MeasureMatrix mmat : vcalc)
         mset.addMeasure (mmat, mmat.getNumMeasures() - 1);
      return mset;
   }

   /**
    * Returns a measure set regrouping the total volumes
    * stored in \texttt{vcalc}, with each total volume repeated
    * \texttt{numTypes} times.  This is used to create a measure set matching
    * with {@link #getServiceVolumeMeasureSet(MeasureMatrix[],int)}
    * to compute per-contact type agent's occupancy ratios.
    * If the measure set is computing the sum rows (the default),
    * the last \texttt{numTypes} rows contain the sum of the total volumes for
    * all agents.
    @param vcalc the agent group volume matrices.
    @param numTypes the number of contact types.
    @return the total volume measure set.
    */
   public static MeasureSet getTotalVolumeMeasureSet
      (MeasureMatrix[] vcalc, int numTypes) {
      final MeasureSet mset = numTypes > 1 ? new VCalc (numTypes) : new MeasureSet();
      for (final MeasureMatrix mmat : vcalc) {
         final int idx = mmat.getNumMeasures() - 1;
         for (int k = 0; k < numTypes; k++)
            mset.addMeasure (mmat, idx);
      }
      return mset;
   }

   private static class VCalc extends MeasureSet {
      private int numTypes;

      VCalc (int numTypes) {
         this.numTypes = numTypes;
      }

      @Override
      public int getNumMeasures() {
         final int nm = super.getNumMeasures();
         if (isComputingSumRow())
            return nm + numTypes - 1;
//            nm--;
//            //int nv = nm / numTypes;
//            nm += numTypes;
         return nm;
      }

      @Override
      public double getMeasure (int i, int p) {
         if (isComputingSumRow()) {
            final int nm = getNumMeasures();
            final int nv = nm / numTypes;
            final int v = i / numTypes;
            if (v == nv - 1) {
               final int t = i % numTypes;
               double value = 0;
               for (int j = 0; j < nv - 1; j++)
                  value += super.getMeasure (numTypes*j + t, p);
               return value;
            }
            else
               return super.getMeasure (i, p);
         }
         else
            return super.getMeasure (i, p);
      }
   }
}
