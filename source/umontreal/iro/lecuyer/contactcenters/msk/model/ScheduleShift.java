package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.List;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.msk.params.ScheduleShiftParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ShiftPartParams;
import umontreal.ssj.probdist.BinomialDist;

/**
 * Represents a shift in a schedule for agents.
 * A shift contains an array of parts as well as an
 * integer giving the number of agents scheduled on that
 * shift. 
 */
public class ScheduleShift {
   private ShiftPart[] parts;
   private int numAgents;
   private double prob;

   /**
    * Constructs a new shift from call center \texttt{cc}, and parameters
    * \texttt{par}.
    * 
    * @param cc
    *           the call center.
    * @param par
    *           the parameters.
    */
   public ScheduleShift (CallCenter cc, ScheduleShiftParams par) {
      List<ShiftPartParams> partList;
      if (par.isSetShiftParts ())
         partList = par.getShiftParts ();
      else if (par.isSetXref ()) {
         final Object ref = par.getXref ();
         if (!(ref instanceof ScheduleShiftParams))
            throw new IllegalArgumentException
            ("The xref attribute of the shift does not point to another shift");
         final ScheduleShiftParams parRef = (ScheduleShiftParams) ref;
         partList = parRef.getShiftParts ();
      }
      else
         throw new IllegalArgumentException ("Shift parts not available");
      parts = ShiftPart.create1 (cc, partList);
      TimeInterval.checkIntervals ((TimeInterval[]) parts);
      if (par.isSetNumAgents () || par.isSetProbAgents ()) {
         numAgents = par.getNumAgents ();
         prob = par.getProbAgents ();
      }
      else if (par.isSetNumAgentsData ()) {
         int[] obs = par.getNumAgentsData ();
         double[] bpar = BinomialDist.getMLE (obs, obs.length);
         numAgents = (int)bpar[0];
         prob = bpar[1];
      }
      else {
         numAgents = 0;
         prob = 1;
      }
   }

   /**
    * Constructs a new schedule shift from parts in the array \texttt{parts},
    * and with \texttt{numAgents} agents.
    * 
    * @param parts
    *           the shift parts.
    * @param numAgents
    *           the number of agents.
    */
   public ScheduleShift (ShiftPart[] parts, int numAgents, double probAgents) {
      TimeInterval.checkIntervals ((TimeInterval[]) parts);
      this.parts = parts.clone();
      this.numAgents = numAgents;
      this.prob = probAgents;
   }

   /**
    * Returns the number of agents on this shift.
    * 
    * @return the number of agents on this shift.
    */
   public int getNumAgents () {
      return numAgents;
   }

   /**
    * Sets the number of agents on that shift to \texttt{numAgents}.
    * 
    * @param numAgents
    *           the number of agents.
    */
   public void setNumAgents (int numAgents) {
      if (numAgents < 0)
         throw new IllegalArgumentException (
               "The number of agents must not be negative");
      this.numAgents = numAgents;
   }

   /**
    * Returns an array containing the shift parts.
    * 
    * @return the array of shift parts.
    */
   public ShiftPart[] getParts () {
      return parts.clone ();
   }

   /**
    * Returns the number of parts for this shift.
    * 
    * @return the number of parts.
    */
   public int getNumParts () {
      return parts.length;
   }

   /**
    * Returns the shift part with index \texttt{i}.
    * 
    * @param i
    *           the index of the part.
    * @return the shift part.
    */
   public ShiftPart getPart (int i) {
      return parts[i];
   }

   /**
    * Computes and returns the shift vector for this shift, relative to the
    * period-change event \texttt{pce}. Element $p$ of this $P$-dimensional
    * vector, where $P$ is the number of main periods is \texttt{true} if and
    * only if agents are scheduled to work during main period $p$.
    * 
    * @param pce
    *           the period-change event.
    * @return the shift vector.
    */
   public boolean[] getShiftVector (PeriodChangeEvent pce) {
      final boolean[] res = new boolean[pce.getNumMainPeriods ()];
      for (final ShiftPart part : parts) {
         if (!part.isWorking ())
            continue;
         final int smp = pce.getMainPeriod (pce.getPeriod (part
               .getStartingTime ()));
         int emp = pce.getMainPeriod (pce.getPeriod (part.getEndingTime ()));
         if (pce.isPeriodStartingTime (part.getEndingTime ()))
            --emp;
         for (int mp = smp; mp <= emp; mp++)
            res[mp] = true;
      }
      return res;
   }

   /**
    * Similar to {@link #getShiftVector(PeriodChangeEvent)}, but
    * returns an array of integers rather than an array of booleans.
    * Element $p$ of the returned array contains
    * 1 if agents are scheduled to work in main period
    * $p$, and 0 otherwise.
    * 
    * @param pce
    *           the period-change event.
    * @return the shift vector.
    */
   public int[] getShiftVectorInt (PeriodChangeEvent pce) {
      final int[] res = new int[pce.getNumMainPeriods ()];
      for (final ShiftPart part : parts) {
         if (!part.isWorking ())
            continue;
         final int smp = pce.getMainPeriod (pce.getPeriod (part
               .getStartingTime ()));
         int emp = pce.getMainPeriod (pce.getPeriod (part.getEndingTime ()));
         if (pce.isPeriodStartingTime (part.getEndingTime ()))
            --emp;
         for (int mp = smp; mp <= emp; mp++)
            res[mp] = 1;
      }
      return res;
   }
   
   /**
    * Returns the presence probability of each agent on that
    * shift.
    */
   public double getAgentProbability() {
      return prob;
   }
   
   /**
    * Sets the presence probability of agents on
    * this shift to \texttt{prob}.
    */
   public void setAgentProbability (double prob) {
      this.prob = prob;
   }
   
   /**
    * Estimates the \texttt{numAgents} and
    * \texttt{probAgents} parameters of the
    * shift described by \texttt{par} from
    * the \texttt{numAgentsData} array of observations,
    * assuming that the number of
    * agents follows a binomial distribution
    * and using the maximum likelihood method.
    * @param par the parameters of the shift.
    * @return \texttt{true} if and only if some
    * parameters were estimated.
    */
   public static boolean estimateParameters (ScheduleShiftParams par) {
      if (par.isSetNumAgentsData ()) {
         int[] obs = par.getNumAgentsData ();
         double[] bpar = BinomialDist.getMLE (obs, obs.length);
         par.setNumAgentsData (null);
         par.setNumAgents ((int)bpar[0]);
         par.setProbAgents (bpar[1]);
         return true;
      }
      return false;
   }
}
