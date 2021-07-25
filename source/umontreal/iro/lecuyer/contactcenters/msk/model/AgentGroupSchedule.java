package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupScheduleParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ScheduleShiftParams;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;

/**
 * Represents the schedule of agents in a certain group.
 * This object stores an array of
 * {@link ScheduleShift} elements representing shifts,
 * and provides some methods to work with shifts.
 */
public class AgentGroupSchedule {
   private ScheduleShift[] shifts;

   /**
    * Constructs a new agent group schedule using
    * the array of shifts \texttt{shifts}.
    * @param shifts the shifts in the schedule.
    */
   public AgentGroupSchedule (ScheduleShift[] shifts) {
      this.shifts = shifts.clone ();
   }
   
   /**
    * Constructs a new schedule using parameters
    * in \texttt{schedule}.  The call center
    * \texttt{cc} is used to obtain a default set of
    * shifts, if the given parameters only provides
    * a vector of agents per shift.
    * @param cc the call center from which default parameters are obtained.
    * @param schedule the parameters of the schedule.
    * @throws AgentGroupCreationException if an error occurs
    * during the creation of the schedule shifts.
    */
   public AgentGroupSchedule (CallCenter cc, AgentGroupScheduleParams schedule) throws AgentGroupCreationException {
      if (schedule.isSetNumAgents () || schedule.isSetNumAgentsData ()) {
         final boolean[][] shiftMatrix;
         if (schedule.isSetShiftMatrix ()) {
            shiftMatrix = ArrayConverter.unmarshalArray (schedule.getShiftMatrix());
            try {
               ArrayUtil.checkRectangularMatrix (shiftMatrix);
            }
            catch (final IllegalArgumentException iae) {
               throw new AgentGroupCreationException
               ("Non-rectangular matrix of shifts", iae);
            }
            if (shiftMatrix.length > 0 && shiftMatrix[0].length != cc.getNumMainPeriods())
               throw new AgentGroupCreationException
               ("The matrix of shifts must have one column per main period");
         }
         else
            shiftMatrix = cc.getDefaultShiftMatrix ();
         if (shiftMatrix == null)
            throw new AgentGroupCreationException
            ("Unspecified matrix of shifts");
         final int[]  numAgents;
         final double[] prob;
         if (schedule.isSetNumAgents () || schedule.isSetProbAgents ()) {
            numAgents = schedule.getNumAgents();
            if (numAgents.length != shiftMatrix.length)
               throw new AgentGroupCreationException
               ("A number of agents is required for each shift");
            prob = schedule.getProbAgents ();
            if (prob.length > 1 && prob.length != shiftMatrix.length)
               throw new AgentGroupCreationException
               ("A presence probability is required for each shift");
         }
         else if (schedule.isSetNumAgentsData ()) {
            int[][] data = ArrayConverter.unmarshalArray (schedule.getNumAgentsData ());
            try {
               ArrayUtil.checkRectangularMatrix (data);
            }
            catch (IllegalArgumentException iae) {
               throw new AgentGroupCreationException
               ("numAgentsData is not a rectangular matrix", iae);
            }
            if (data.length == 0)
               throw new AgentGroupCreationException
               ("No staffing data available");
            numAgents = new int[data[0].length];
            prob = new double[numAgents.length];
            int[] obs = new int[data.length];
            for (int mp = 0; mp < numAgents.length; mp++) {
               for (int j = 0; j < obs.length; j++)
                  obs[j] = data[j][mp];
               double[] bpar = BinomialDist.getMLE (obs, obs.length);
               numAgents[mp] = (int)bpar[0];
               prob[mp] = bpar[1];
            }
         }
         else {
            numAgents = new int[shiftMatrix.length];
            prob = new double[] { 1 };
         }
         shifts = new ScheduleShift[shiftMatrix.length];
         for (int j = 0; j < shifts.length; j++) {
            final List<ShiftPart> parts = new ArrayList<ShiftPart>();
            int sp = -1;
            for (int p = 0; p < shiftMatrix[j].length; p++)
               if (sp == -1 && shiftMatrix[j][p])
                  sp = p;
               else if (sp >= 0 && !shiftMatrix[j][p]) {
                  final double startingTime = cc.getPeriodChangeEvent().getPeriodStartingTime (sp + 1);
                  final double endingTime = cc.getPeriodChangeEvent().getPeriodEndingTime (p);
                  parts.add (new ShiftPart (startingTime, endingTime, ShiftPart.WORKING));
                  sp = -1;
               }
            if (sp != -1) {
               final double startingTime = cc.getPeriodChangeEvent().getPeriodStartingTime (sp + 1);
               final double endingTime = cc.getPeriodChangeEvent().getPeriodEndingTime (shiftMatrix[j].length);
               parts.add (new ShiftPart (startingTime, endingTime, ShiftPart.WORKING));
            }
            double p;
            if (prob.length == 0)
               p = 1;
            else if (prob.length == 1)
               p = prob[0];
            else
               p = prob[j];
            shifts[j] = new ScheduleShift (parts.toArray (new ShiftPart[parts.size()]), numAgents[j], p);
         }
      }
      else if (schedule.isSetShifts()) {
         final List<ScheduleShiftParams> shiftList = schedule.getShifts();
         shifts = new ScheduleShift[shiftList.size()];
         int idx = 0;
         for (final ScheduleShiftParams sp : shiftList)
            try {
               shifts[idx] = new ScheduleShift (cc, sp);
               ++idx;
            }
            catch (final IllegalArgumentException iae) {
               throw new AgentGroupCreationException
               ("Cannot create schedule shift " + idx, iae);
            }
      }
      else
         throw new AgentGroupCreationException
         ("Missing shift information");
   }
   
   /**
    * Returns an array containing the 
    * shifts of this schedule.
    * @return the array of shifts.
    */
   public ScheduleShift[] getShifts() {
      return shifts.clone ();
   }
   
   /**
    * Returns the number of shifts in the schedule.
    * @return the number of shifts in the schedule.
    */
   public int getNumShifts() {
      return shifts.length;
   }

   /**
    * Returns the shift with index \texttt{i} of this schedule.
    * @param i the index of the shift.
    * @return the corresponding shift.
    */
   public ScheduleShift getShift (int i) {
      return shifts[i];
   }
   
   /**
    * Returns a vector giving the number of agents
    * for each shift.
    * This method is for internal use; it is
    * recommended to use {@link AgentGroupManagerWithSchedule#getEffectiveNumAgents()}. 
    * @return the number of agents per shift.
    */
   public int[] getNumAgents() {
      final int[] res = new int[shifts.length];
      for (int i = 0; i < res.length; i++)
         res[i] = shifts[i].getNumAgents();
      return res;
   }
   
   /**
    * Similar to {@link #getNumAgents()}, but returns
    * the number of agents on a given shift
    * \texttt{shift}.
    * @param shift the shift index to look at.
    * @return the number of agents on the shift.
    */
   public int getNumAgents (int shift) {
      return shifts[shift].getNumAgents ();
   }
   
   /**
    * Sets the number of agents on the shift
    * \texttt{shift} to \texttt{n}.
    * @param shift the affected shift.
    * @param n the new number of agents.
    */
   public void setNumAgents (int shift, int n) {
      shifts[shift].setNumAgents (n);
   }
   
   /**
    * Sets the vector of number of agents of this
    * schedule to \texttt{numAgents}.
    * @param numAgents the new vector of agents.
    */
   public void setNumAgents (int[] numAgents) {
      for (int i = 0; i < numAgents.length; i++)
         shifts[i].setNumAgents (numAgents[i]);
   }
   
   /**
    * Computes and returns the matrix of shifts.
    * Element $(j, p)$ of this $J\times P$ matrix, where
    * $J$ corresponds to the number of shifts and
    * $P$, to the number of main periods,
    * is \texttt{true} if and only if agents are scheduled to work
    * on shift \texttt{j} during main period \texttt{p}.
    * @return the matrix of shifts.
    */
   public boolean[][] getShiftMatrix (PeriodChangeEvent pce) {
      final boolean[][] res = new boolean[shifts.length][];
      for (int i = 0; i < shifts.length; i++)
         res[i] = shifts[i].getShiftVector (pce);
      return res;
   }

   /**
    * Similar to {@link #getShiftMatrix(PeriodChangeEvent)}, but
    * returns a 2D array of integers rather than booleans.
    * Element $(j, p)$ of this $J\times P$ matrix, where
    * $J$ corresponds to the number of shifts and
    * $P$, to the number of main periods,
    * is 1 if agents are scheduled to work
    * on shift \texttt{j} during main period \texttt{p}, and
    * 0 otherwise.
    * @return the matrix of shifts.
    */
   public int[][] getShiftMatrixInt (PeriodChangeEvent pce) {
      final int[][] res = new int[shifts.length][];
      for (int i = 0; i < shifts.length; i++)
         res[i] = shifts[i].getShiftVectorInt (pce);
      return res;
   }
   
   /**
    * Computes and returns the staffing vector.
    * This corresponds to the column vector returned by
    * {@link #getNumAgents()} multiplied by
    * the matrix returned by {@link #getShiftMatrix(PeriodChangeEvent)}.
    */
   public int[] getStaffing (PeriodChangeEvent pce) {
      final int[] numAgents = getNumAgents();
      final int[][] shiftMatrix = getShiftMatrixInt (pce);
      final int[] staffing = new int[pce.getNumMainPeriods()];
      for (int mp = 0; mp < staffing.length; mp++)
         for (int j = 0; j < numAgents.length; j++)
            staffing[mp] += numAgents[j]*shiftMatrix[j][mp];
      return staffing;
   }
   
   /**
    * Estimates the \texttt{numAgents} and
    * \texttt{probAgents} parameters for this schedule,
    * using the \texttt{numAgentsData} matrix and
    * the maximum likelihood method.
    * For each shift, the number of agents is assumed
    * to be a binomial random variable.
    * The method also calls
    * {@link ScheduleShift#estimateParameters(ScheduleShiftParams)}
    * for every shift described by \texttt{par}.
    * @param par the schedule parameters.
    * @return \texttt{true} if and only if some
    * parameters were estimated.
    * @throws DistributionCreationException if
    * an error occurs during parameter estimation.
    */
   public static boolean estimateParameters (AgentGroupScheduleParams par) throws DistributionCreationException {
      if (par.isSetNumAgentsData ()) {
         int[][] data = ArrayConverter.unmarshalArray (par.getNumAgentsData ());
         try {
            ArrayUtil.checkRectangularMatrix (data);
         }
         catch (IllegalArgumentException iae) {
            DistributionCreationException dce = new DistributionCreationException
            ("numAgentsData is not a rectangular matrix");
            dce.initCause (iae);
            throw dce;
         }
         if (data.length > 0) {
            int[] staffing = new int[data[0].length];
            double[] prob = new double[staffing.length];
            int[] obs = new int[data.length];
            for (int mp = 0; mp < staffing.length; mp++) {
               for (int i = 0; i < obs.length; i++)
                  obs[i] = data[i][mp];
               double[] bpar = BinomialDist.getMLE (obs, obs.length);
               staffing[mp] = (int)bpar[0];
               prob[mp] = bpar[1];
            }
            par.setNumAgentsData (null);
            par.setNumAgents (staffing);
            par.setProbAgents (prob);
            return true;
         }
      }
      boolean res = false;
      for (ScheduleShiftParams sp : par.getShifts ())
         res |= ScheduleShift.estimateParameters (sp);
      return res;
   }
}
