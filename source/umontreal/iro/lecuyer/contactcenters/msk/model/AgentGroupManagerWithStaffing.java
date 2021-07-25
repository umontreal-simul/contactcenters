package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeListener;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

/**
 * Manages an agent group with a staffing vector
 * giving the number of agents for each period.
 * This manager stores the staffing vector and registers a period-change
 * to update the staffing at the beginning of main periods.
 */
public class AgentGroupManagerWithStaffing extends AgentGroupManager {
   private int[] staffing;
   private int[] curStaffing;
   private double[] prob;
   private final ParamUpdater upd = new ParamUpdater();

   /**
    * Calls the superclass' constructor, and 
    * extracts the staffing from \texttt{par}.
    */
   public AgentGroupManagerWithStaffing (CallCenter cc, AgentGroupParams par, int i) throws AgentGroupCreationException {
      super (cc, par, i);
      if (par.isSetStaffingData ()) {
         int[][] data = ArrayConverter.unmarshalArray (par.getStaffingData ());
         try {
            ArrayUtil.checkRectangularMatrix (data);
         }
         catch (IllegalArgumentException iae) {
            throw new AgentGroupCreationException
            ("staffingData is not a rectangular matrix", iae);
         }
         if (data.length == 0)
            throw new AgentGroupCreationException
            ("No staffing data available");
         if (data[0].length != cc.getNumMainPeriods ())
            throw new AgentGroupCreationException
            ("staffingData must contain one column for each main period");
         staffing = new int[data[0].length];
         prob = new double[staffing.length];
         int[] obs = new int[data.length];
         for (int mp = 0; mp < staffing.length; mp++) {
            for (int j = 0; j < obs.length; j++)
               obs[j] = data[j][mp];
            double[] bpar = BinomialDist.getMLE (obs, obs.length);
            staffing[mp] = (int)bpar[0];
            prob[mp] = bpar[1];
         }
      }
      else {
         try {
            staffing = CallCenterUtil.getIntArray (par.getStaffing(), cc.getNumMainPeriods ());
         }
         catch (final IllegalArgumentException iae) {
            throw new AgentGroupCreationException
            ("Error reading staffing vector", iae);
         }
         if (staffing.length == 0)
            throw new AgentGroupCreationException
            ("Staffing vector for agent group " + i + " cannot have length 0");
         if (staffing.length > 1 && staffing.length < cc.getNumMainPeriods ())
            throw new AgentGroupCreationException
            ("The length of the staffing vector must at least correspond to the number of main periods");
         try {
            prob = CallCenterUtil.getDoubleArray (par.getProbAgents (), cc.getNumMainPeriods ());
         }
         catch (final IllegalArgumentException iae) {
            throw new AgentGroupCreationException
            ("Error reading probability vector", iae);
         }
         if (prob.length > 1 && prob.length < cc.getNumMainPeriods ())
            throw new AgentGroupCreationException
            ("The length of the probability vector must at least correspond to the number of main periods");
      }
      curStaffing = new int[cc.getNumMainPeriods ()];
      cc.getPeriodChangeEvent().addPeriodChangeListener (upd);
   }

   /**
    * Similar to the first constructor
    * {@link #AgentGroupManagerWithStaffing(CallCenter,AgentGroupParams,int)},
    * but uses the given staffing vector instead of the one
    * extracted from \texttt{par}.
    */
   public AgentGroupManagerWithStaffing (CallCenter cc, AgentGroupParams par, int i, int[] staffing) throws AgentGroupCreationException {
      super (cc, par, i);
      this.staffing = staffing;
      cc.getPeriodChangeEvent().addPeriodChangeListener (upd);
   }
   
   @Override
   public int[] getStaffing() {
      return staffing.clone();
   }
   
   @Override
   public int getStaffing (int mp) {
      return staffing[mp];
   }
   
   /**
    * Sets the staffing vector to
    * \texttt{staffing}.
    * @param staffing the new staffing vector.
    */
   public void setStaffing (int[] staffing) {
      if (staffing.length > 1 && staffing.length < getCallCenter().getNumMainPeriods())
         throw new IllegalArgumentException
         ("The length of the staffing vector must correspond to the number of main periods");
      if (staffing.length == 1)
         this.staffing = CallCenterUtil.getIntArray (staffing, getCallCenter().getNumMainPeriods ());
      else
         this.staffing = staffing.clone();
   }
   
   /**
    * Sets the staffing for main period \texttt{mp}
    * to \texttt{staffing}.
    * @param mp the index of the affected main period.
    * @param staffing the new staffing.
    */
   public void setStaffing (int mp, int staffing) {
      this.staffing[mp] = staffing;
   }
   
   /**
    * Sets the effective staffing for the managed agent group
    * to \texttt{staffing}.
    * This method sets the staffing to \texttt{staffing}
    * using {@link #setStaffing(int[])}, but it also
    * resets the value of the multiplier $m*m_i$ to 1.
    * This makes sure that {@link #getEffectiveStaffing()}
    * will return the same value as the staffing
    * passed to this method.
    * @param staffing the new effective staffing.
    */
   public void setEffectiveStaffing (int[] staffing) {
      getCallCenter().resetAgentsMult ();
      setStaffing (staffing);
      setAgentsMult (1.0);
   }

   /**
    * Similar to {@link #setEffectiveStaffing(int[])},
    * for a single main period.
    * @param mp the index of the affected main period.
    * @param ns the new number of agents.
    */
   public void setEffectiveStaffing (int mp, int ns) {
      getCallCenter ().resetAgentsMult ();
      if (getAgentsMult() == 1.0)
         setStaffing (mp, ns);
      else {
         int[] staffing1 = getEffectiveStaffing ();
         setAgentsMult (1.0);
         staffing1[mp] = ns;
         setStaffing (staffing1);
      }
   }
   
   /**
    * Returns the number of agents in the managed group
    * for the current simulation replication.
    * If the number of agents is deterministic,
    * this method returns the result of {@link #getEffectiveStaffing()}.
    * Otherwise, it returns the current (random)
    * number of agents for each main period.
    * @return the number of agents in the current
    * replication.
    */
   public int[] getCurNumAgents () {
      return curStaffing.clone();
   }
   
   /**
    * Similar to {@link #getCurNumAgents()}, for
    * a given main period \texttt{mp}.
    * @param mp the index of the main period.
    * @return the number of agents.
    */
   public int getCurNumAgents (int mp) {
      return curStaffing[mp];
   }
   
   /**
    * Returns the per-period probabilities of
    * presence for each agent in the group.
    * If no such probabilities wre given by the user,
    * this returns an array of 1's. 
    * @return the presence probability, for
    * each main period.
    */
   public double[] getAgentProbability() {
      return prob;
   }
   
   /**
    * Similar to {@link #getAgentProbability()},
    * for a given main period \texttt{mp}.
    * @param mp the index of the main period.
    * @return the presence probability.
    */
   public double getAgentProbability (int mp) {
      if (prob.length == 0)
         return 1;
      if (prob.length == 1)
         return prob[0];
      return prob[mp];
   }
   
   /**
    * Sets the per-period presence probabilities of agents to
    * \texttt{prob}.
    * @param prob the per-period presence probabilities.
    */
   public void setAgentProbability (double[] prob) {
      if (prob.length > 1 && prob.length < staffing.length)
         throw new IllegalArgumentException ("Invalid length of prob");
      if (prob.length == 1)
         this.prob = CallCenterUtil.getDoubleArray (prob, getCallCenter().getNumMainPeriods ());
      else
         this.prob = prob.clone ();
   }
   
   /**
    * Sets the presence probability of agents to
    * \texttt{prob} for main period \texttt{mp}.
    */
   public void setAgentProbability (int mp, double prob) {
      this.prob[mp] = prob;
   }
   
   @Override
   public void init() {
      super.init();
      final PeriodChangeEvent pce = getCallCenter().getPeriodChangeEvent();
      if (pce.isLockedPeriod()) {
         final int mp = pce.getCurrentMainPeriod();
         final int na = getEffectiveStaffing (mp);
         getAgentGroup().setNumAgents (na);
      }
      else {
         for (int mp = 0; mp < curStaffing.length; mp++) {
            double p = getAgentProbability (mp);
            int s = getEffectiveStaffing (mp);
            if (p == 1)
               curStaffing[mp] = s;
            else
               curStaffing[mp] = BinomialDist.inverseF 
               (s, p, getProbDisconnectStream ().nextDouble ());
         }
      }
   }

   private final class ParamUpdater implements PeriodChangeListener {
      public void changePeriod (PeriodChangeEvent pce) {
         final int p = pce.getCurrentPeriod();
         if (p <= 0)
            getAgentGroup().setNumAgents (0);
         else {
            final int mp = pce.getMainPeriod (p);
            final int na = curStaffing[mp];
            getAgentGroup().setNumAgents (na);
//            int nb = getAgentGroup().getNumBusyAgents ();
//            int max = na;
//            for (int mp2 = 0; mp2 < mp; mp2++) {
//               final int na2 = getEffectiveStaffing (mp2);
//               if (max < na2)
//                  max = na2;
//            }
//            if (nb > max)
//               throw new AssertionError();
         }
      }

      public void stop (PeriodChangeEvent pce) {
      }
   }
}
