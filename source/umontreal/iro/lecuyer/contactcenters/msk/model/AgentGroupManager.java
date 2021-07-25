package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Arrays;
import java.util.logging.Logger;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupScheduleParams;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.ContactTimeGenerator;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEventDetailed;

import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.NamedInfo;

/**
 * Manages an agent group in the call center model.
 * This class implements the mechanisms necessary
 * to construct the agent group, and to update
 * its state during the simulation.
 * It also manages agent disconnection if
 * it is enabled.
 *
 * By default, this agent group manager sets the number of
 * agents in the managed group to 0, and does not change it
 * during simulation.
 * However, subclasses such as {@link AgentGroupManagerWithStaffing}
 * can override the {@link #init()} method in order to set and
 * update the number of agents.
 */
public class AgentGroupManager extends NamedInfo {
   private final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.contactcenters.msk.model");
   private CallCenter cc;
   private AgentGroup group;
   private RandomStream dpStream;
   private double[] probDisconnect;
   private MultiPeriodGen dtGen;
   private AgentDisconnect agentDisconnect;

   private double weight;
   private int skillCount;
   private double idleCost;
   private double busyCost;
   private double perUseCost;
   private double agentsMult;
   private int maxAgents;
   private int minAgents;

   private double[] weightPeriod;
   private double[] idleCostPeriod;
   private double[] busyCostPeriod;
   private double[] perUseCostPeriod;
   private int[] maxAgentsPeriod;
   private int[] minAgentsPeriod;
   private AgentGroupSchedule schedule;

   /**
    * Constructs a new agent group manager for the call center
    * \texttt{cc}, agent group \texttt{i}, and based on agent group
    * parameters \texttt{par}.
     @param cc the call center model.
     @param par the parameters of the agent group to be managed.
     @param i the index of the agent group.
     @exception AgentGroupCreationException if an error occurs
     while constructing the agent group manager, or the
     associated agent group.
    */
   public AgentGroupManager (CallCenter cc, AgentGroupParams par, int i)
   throws AgentGroupCreationException {
      super (par);
      this.cc = cc;
      weight = par.getWeight ();
      skillCount = par.isSetSkillCount () ? par.getSkillCount () : Integer.MAX_VALUE;
      idleCost = par.getIdleCost ();
      busyCost = par.getBusyCost ();
      perUseCost = par.getPerUseCost ();
      maxAgents = par.isSetMaxAgents () ? par.getMaxAgents () : Integer.MAX_VALUE;
      minAgents = par.getMinAgents ();

      weightPeriod = par.getWeightPeriod ();
      idleCostPeriod = par.getIdleCostPeriod ();
      busyCostPeriod = par.getBusyCostPeriod ();
      perUseCostPeriod = par.getPerUseCostPeriod ();
      maxAgentsPeriod = par.getMaxAgentsPeriod ();
      minAgentsPeriod = par.getMinAgentsPeriod ();

      group = createAgentGroup (par, i);
      group.setId (i);
      final String grpn = getName();
      if (grpn != null && grpn.length () > 0)
         group.setName (grpn);
      else
         group.setName ("");
      double[] mult = par.getServiceTimesMult ();
      if (mult != null && mult.length > 0) {
         if (mult.length == 1) {
            final double m = mult[0];
            mult = new double[cc.getNumMainPeriods ()];
            Arrays.fill (mult, m);
         }
         ((ContactTimeGenerator) group.getContactTimeGenerator (0))
         .setMultipliers (mult);
      }
      if (par.isSetEfficiency())
         group.setEfficiency (par.getEfficiency ());

      dpStream = cc.getRandomStreams ().getAgentGroupStream (i,
            AgentGroupStreamType.DISCONNECTTEST);
      if (par.isSetProbDisconnect () &&
            par.isSetDisconnectTime ()) {
         probDisconnect = par.getProbDisconnect ();
         if (probDisconnect.length == 0)
            throw new AgentGroupCreationException
            ("probDisconnect must have a length greater than 0");
         if (probDisconnect.length > 1 && probDisconnect.length < cc.getNumMainPeriods ())
            throw new AgentGroupCreationException
            ("A probability of disconnect must be given for each main period");
         final RandomStream stream = cc.getRandomStreams ().getAgentGroupStream (i,
               AgentGroupStreamType.DISCONNECTTIME);
         try {
            dtGen = CCParamReadHelper.createGenerator (par
                  .getDisconnectTime (), stream, cc.getPeriodChangeEvent ());
            dtGen.setTargetTimeUnit (cc.getDefaultUnit ());
         }
         catch (final DistributionCreationException dce) {
            throw new AgentGroupCreationException
            ("Error creating disconnect time distribution", dce);
         }
         catch (final GeneratorCreationException gce) {
            throw new AgentGroupCreationException
            ("Error creating disconnect time generator", gce);
         }

         agentDisconnect = new AgentDisconnect();
         group.addAgentGroupListener (agentDisconnect);
      }
      agentsMult = par.getAgentsMult ();
   }

   /**
    * Returns the factor by which the number of agents
    * in the managed group given in parameter file is
    * multiplied.
    * This multiplier is reset to 1
    * if the number of agents is changed programmatically
    * by, e.g., {@link AgentGroupManagerWithStaffing#setStaffing(int[])}.
    * @return the multiplier for the managed agent group.
    */
   public double getAgentsMult() {
      return agentsMult;
   }

   /**
    * Sets the multiplier of the managed agent group to
    * \texttt{mult}.
    * @param mult the new multiplier.
    */
   public void setAgentsMult (double mult) {
      if (mult < 0)
         throw new IllegalArgumentException ("mult < 0");
      agentsMult = mult;
   }

   /**
    * Connects the managed agent group to the router
    * \texttt{router} by using the {@link Router#setAgentGroup(int,AgentGroup)}
    * method.
    * If agent disconnection is enabled, this method also ensures
    * that the listener handling disconnections is notified of
    * events related to the agent group before the router.
     @param router the router the agent group is connected to.
    */
   public void connectToRouter (Router router) {
      router.setAgentGroup (group.getId (), group);
      // Ensures that agentDisconnect listener occurs before rl
      if (agentDisconnect != null) {
         final AgentGroupListener rl = router.getAgentGroupListener ();
         group.removeAgentGroupListener (rl);
         group.addAgentGroupListener (rl);
      }
   }

   /**
    * Returns the weight associated with the managed agent group.
    * @return the weight of the managed agent group.
    */
   public double getWeight() {
      return weight;
   }

   /**
    * Returns the skill count associated with the managed agent group, or
    * {@link Integer#MAX_VALUE} if no skill count was set explicitly by
    * the user.
    *
    * This method is mainly for internal use;
    * the recommended way to obtain the skill count is by using
    * {@link RouterManager#getSkillCount(int)} after
    * {@link RouterManager#initSkillCounts(RouterParams)} was called.
    * @return the explicitly set skill count.
    */
   public int getSkillCount() {
      return skillCount;
   }

   /**
    * Returns the cost of an idle agent
    * in the managed group during one simulation time unit.
    * @return the cost of an idle agent.
    */
   public double getIdleCost() {
      return idleCost;
   }

   /**
    * Returns the cost of a busy agent
    * in the managed group during one simulation time unit.
    * @return the cost of a busy agent.
    */
   public double getBusyCost() {
      return busyCost;
   }

   /**
    * Returns the cost incurred each time an agent
    * in the managed group starts the service of a call.
    * @return the per-use cost of agents in the managed group.
    */
   public double getPerUseCost() {
      return perUseCost;
   }

   /**
    * Returns the cost of an idle agent managed by this group
    * during main period \texttt{mp}, during
    * one simulation time unit.
    * This returns the result of {@link #getIdleCost()}
    * if no per-period cost were given by the user
    * in parameter file.
    * @param mp the index of the tested main period.
    * @return the idle cost.
    */
   public double getIdleCost (int mp) {
      if (idleCostPeriod.length == 0)
         return getIdleCost ();
      if (idleCostPeriod.length == 1)
         return idleCostPeriod[0];
      return idleCostPeriod[mp];
   }

   /**
    * Returns the cost of a busy agent managed by this group
    * during main period \texttt{mp}, during
    * one simulation time unit.
    * This returns the result of {@link #getBusyCost()}
    * if no per-period cost were given by the user
    * in parameter file.
    * @param mp the index of the tested main period.
    * @return the busy cost.
    */
   public double getBusyCost (int mp) {
      if (busyCostPeriod.length == 0)
         return getBusyCost ();
      if (busyCostPeriod.length == 1)
         return busyCostPeriod[0];
      return busyCostPeriod[mp];
   }

   /**
    * Returns the cost incurred each time an agent
    * in the managed group starts a service during
    * main period \texttt{mp}.
    * This method returns the result of
    * {@link #getPerUseCost()} if no
    * per-period costs were given in parameter file.
    * @param mp the index of the tested main period.
    * @return the per-use cost.
    */
   public double getPerUseCost (int mp) {
      if (perUseCostPeriod.length == 0)
         return getPerUseCost ();
      if (perUseCostPeriod.length == 1)
         return perUseCostPeriod[0];
      return perUseCostPeriod[mp];
   }

   /**
    * Returns the weight of the managed agent group during
    * main period \texttt{mp}.
    * If no per-period weights were given in parameter file,
    * this method returns the result of
    * {@link #getWeight()}.
    * @param mp the index of the tested main period.
    * @return the weight of the managed agent group.
    */
   public double getWeight (int mp) {
      if (weightPeriod.length == 0)
         return getWeight ();
      if (weightPeriod.length == 1)
         return weightPeriod[0];
      return weightPeriod[mp];
   }

   /**
    * Returns the maximal number of agents in the managed
    * group.
    * @return the maximal number of agents in the managed group.
    */
   public int getMaxAgents() {
      return maxAgents;
   }

   /**
    * Returns the minimal number of agents in the managed group.
    * @return the minimal number of agents in the managed group.
    */
   public int getMinAgents() {
      return minAgents;
   }

   /**
    * Returns the maximal number of agents in the managed group
    * during main period \texttt{mp}.
    * This method returns the result of {@link #getMaxAgents()}
    * if no per-period maximum number of agents were given
    * in parameter file.
    * @param mp the index of the tested main period.
    * @return the maximal number of agents.
    */
   public int getMaxAgents (int mp) {
      if (maxAgentsPeriod.length == 0)
         return getMaxAgents ();
      if (maxAgentsPeriod.length == 1)
         return maxAgentsPeriod[0];
      return maxAgentsPeriod[mp];
   }

   /**
    * Returns the minimal number of agents in the managed group
    * during main period \texttt{mp}.
    * This method returns the result of {@link #getMinAgents()}
    * if no per-period minimum number of agents were given
    * in parameter file.
    * @param mp the index of the tested main period.
    * @return the minimal number of agents.
    */
   public int getMinAgents (int mp) {
      if (minAgentsPeriod.length == 0)
         return getMinAgents ();
      if (minAgentsPeriod.length == 1)
         return minAgentsPeriod[0];
      return minAgentsPeriod[mp];
   }

   /**
    * Constructs and returns a new agent group manager for call center
    * \texttt{cc}, agent group with index \texttt{i}, and parameters \texttt{par}.
    * If the given parameters contain a staffing, an instance of
    * {@link AgentGroupManagerWithStaffing} is created.
    * If the parameters contain a schedule, an instance of
    * {@link AgentGroupManagerWithSchedule} is constructed.
    * If the parameters contain information about individual agents,
    * an {@link AgentGroupManagerWithAgents} object is created.
    * Otherwise, a plain {@link AgentGroupManager} object is
    * created.
    * The created object can, depending on parameters, be converted
    * to an instance of {@link AgentGroupManagerWithStaffing}.
    * The constructed (or converted) object is returned.
     @param cc the call center model.
     @param par the parameters of the agent group to be managed.
     @param i the index of the agent group.
     @exception AgentGroupCreationException if an error occurs
     while constructing the agent group manager, or the
     associated agent group.
    */
   public static AgentGroupManager create (CallCenter cc, AgentGroupParams par, int i) throws AgentGroupCreationException {
      AgentGroupManager res;
      if (par.isSetStaffing() || par.isSetStaffingData ())
         res = new AgentGroupManagerWithStaffing (cc, par, i);
      else if (par.isSetSchedule())
         res = new AgentGroupManagerWithSchedule (cc, par, i);
      else if (par.isSetAgents())
         res = new AgentGroupManagerWithAgents (cc, par, i);
      else
         res = new AgentGroupManager (cc, par, i);
      final boolean cnv;
      if (par.isSetConvertSchedulesToStaffing ())
         cnv = par.isConvertSchedulesToStaffing ();
      else
         cnv = cc.isConvertScheduleToStaffing ();
      if (cnv && !(res instanceof AgentGroupManagerWithStaffing)) {
         AgentGroupManagerWithStaffing ag = new AgentGroupManagerWithStaffing (cc, par, i, res.getStaffing ());
         if (res instanceof AgentGroupManagerWithSchedule)
            ag.setSchedule (((AgentGroupManagerWithSchedule)res).getSchedule ());
         return ag;
      }
      return res;
   }

   /**
    * Constructs and returns the \texttt{i}th agent group for this call center.
    * By default, this constructs an {@link AgentGroup} or
    * {@link DetailedAgentGroup} instance, depending on the
    * return value of the
    * {@link AgentGroupParams#isDetailed()} method.
    *
    * @param i
    *           the agent group index.
    * @return the constructed agent group.
    */
   protected AgentGroup createAgentGroup (AgentGroupParams par, int i) throws AgentGroupCreationException {
      final boolean detailed;
      final boolean probDisconnect1 = par.isSetProbDisconnect () &&
      par.isSetDisconnectTime ();
      if (par.isSetDetailed ())
         // The detailed attribute has precedence
         detailed = par.isDetailed ();
      else
         detailed = probDisconnect1;
      if (probDisconnect1 && !detailed)
         logger.warning ("Agents can disconnect after services in group " + i + ", but the detailed attribute was set to false for this agent group");

      final AgentGroup group1 = detailed ? new DetailedAgentGroup (
            cc.simulator (), 0)
      : new AgentGroup (0);
            return group1;
   }

   private final class AgentDisconnect implements AgentGroupListener {
      public void agentGroupChange (AgentGroup group1) {}

      public void beginService (EndServiceEvent ev) {}

      public void endContact (EndServiceEvent ev) {}

      public void endService (EndServiceEvent ev) {
         if (ev.wasGhostAgent ())
            return;
         if (ev instanceof EndServiceEventDetailed) {
            final Agent agent = ((EndServiceEventDetailed) ev).getAgent ();
            assert agent != null : "The agent associated to a detailed end-service event is null";
            final int mp = cc.getPeriodChangeEvent ().getCurrentMainPeriod ();
            final double prob = getProbDisconnect (mp);
            final double u = dpStream.nextDouble ();
            if (u <= prob) {
               agent.setAvailable (false);
               final double t = dtGen.nextDouble ();
               new MakeAgentAvailableEvent (cc, agent).schedule (t);
            }
         }
      }

      public void init (AgentGroup group1) {}
   }

   /**
    * Returns a reference to the call center containing
    * this agent group manager.
    */
   public CallCenter getCallCenter() {
      return cc;
   }

   /**
    * Returns the random stream used to test if
    * an agent disconnects after the end of a service.
    */
   public RandomStream getProbDisconnectStream () {
      return dpStream;
   }

   /**
    * Sets the random stream used to test if an agents
    * disconnects after the end of a service to
    * \texttt{dpStream}.
    */
   public void setProbDisconnectStream (RandomStream dpStream) {
      if (dpStream == null)
         throw new NullPointerException();
      this.dpStream = dpStream;
   }

   /**
    * Returns the random variate generator used
    * for disconnect times.
    */
   public MultiPeriodGen getDisconnectTimeGen () {
      return dtGen;
   }

   /**
    * Returns a reference to the managed agent group.
    */
   public AgentGroup getAgentGroup () {
      return group;
   }

   /**
    * Returns an array giving the probabilities of disconnection,
    * for each main period.
    */
   public double[] getProbDisconnect () {
      return probDisconnect;
   }

   /**
    * Returns the probability that an agent ending a service
    * during main period \texttt{mp} disconnects for
     a random time.
    */
   public double getProbDisconnect (int mp) {
      if (probDisconnect == null || probDisconnect.length == 0)
         return 0;
      if (probDisconnect.length == 1)
         return probDisconnect[0];
      else
         return probDisconnect[mp];
   }

   /**
    * Calls \texttt{init} on the managed agent group.
    */
   public void init() {
      group.init();
   }

   /**
    * Returns the raw staffing of the managed agent group.
    * The returned array gives the number of agents in the
    * managed group during each main period in the model, before
    * any multiplier is applied.
    *
    * This method is mainly for internal use;
    * the {@link #getEffectiveStaffing()}
    * method should be used instead to take multipliers into
    * account.
    *
    * The default behavior of this method is to return
    * an array of 0's.
    * @return the raw staffing for the managed agent group.
    */
   public int[] getStaffing() {
      return new int[cc.getNumMainPeriods ()];
   }

   /**
    * Returns element \texttt{mp} of the array that would
    * be returned by {@link #getStaffing()}.
    *
    * As with {@link #getStaffing()}, this method is for
    * internal use.
    * The method {@link #getEffectiveStaffing(int)} should be used instead.
    * @param mp the index of the tested main period.
    * @return the raw staffing.
    */
   public int getStaffing (int mp) {
      return 0;
   }

   /**
    * Returns the staffing determining
    * the effective number of agents in the managed
    * group for each main period in the model.
    * This method calls {@link #getStaffing()}, and
    * multiplies each element of the returned array
    * by $m*m_i$, where $m$ is determined by
    * {@link CallCenter#getAgentsMult()} and
    * $m_i$ is given by {@link #getAgentsMult()}.
    * The resulting numbers are rounded to the nearest integers, and
    * stored in the array being returned.
    * @return the effective staffing.
    */
   public int[] getEffectiveStaffing() {
      final int[] baseStaffing = getStaffing();
      for (int mp = 0; mp < baseStaffing.length; mp++)
         baseStaffing[mp] = (int)Math.round (baseStaffing[mp]*agentsMult*cc.getAgentsMult ());
      return baseStaffing;
   }

   /**
    * Returns element \texttt{mp} of the array that would
    * be returned by {@link #getEffectiveStaffing()}.
    * @param mp the index of the tested main period.
    * @return the effective staffing.
    */
   public int getEffectiveStaffing (int mp) {
      final int baseStaffing = getStaffing (mp);
      return (int)Math.round (baseStaffing*agentsMult*cc.getAgentsMult ());
   }

   /**
    * Returns the schedule associated with the managed agent group.
    * This corresponds to the effective schedule if this object is an instance of
    * {@link AgentGroupManagerWithSchedule}.
    * If this object is an instance of {@link AgentGroupManagerWithStaffing}
    * converted from an instance with schedule, this returns the schedule
    * of the original agent group manager with schedule.
    * Otherwise, this method returns \texttt{null}.
    */
   public AgentGroupSchedule getSchedule () {
      return schedule;
   }

   protected void setSchedule (AgentGroupSchedule schedule) {
      this.schedule = schedule;
   }

   /**
    * Estimates parameters relative to the agent group
    * described by \texttt{par}.
    * The method estimates the parameters of the
    * distribution for disconnect times.
    * If the agent group has staffing information,
    * the method then estimates \texttt{staffing}
    * and \texttt{probAgents} from
    * \texttt{staffingData} if \texttt{staffingData}
    * is given.
    * If scheduling information is used, the
    * method calls {@link AgentGroupSchedule#estimateParameters(AgentGroupScheduleParams)}
    * to complete parameter estimation.
    * @param par the parameters of the agent group.
    * @return \texttt{true} if and only if some
    * parameters were estimated.
    * @throws DistributionCreationException if an error
    * occurs during parameter estimation.
    */
   public static boolean estimateParameters (AgentGroupParams par) throws DistributionCreationException {
      boolean res = CCParamReadHelper.estimateParameters (par.getDisconnectTime ());
      if (par.isSetStaffingData ()) {
         int[][] data = ArrayConverter.unmarshalArray (par.getStaffingData ());
         try {
            ArrayUtil.checkRectangularMatrix (data);
         }
         catch (IllegalArgumentException iae) {
            DistributionCreationException dce = new DistributionCreationException
            ("StaffingData is not a rectangular matrix");
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
            par.setStaffingData (null);
            par.setStaffing (staffing);
            par.setProbAgents (prob);
            res = true;
         }
      }
      if (par.isSetSchedule ())
         res |= AgentGroupSchedule.estimateParameters (par.getSchedule ());
      return res;
   }

}
