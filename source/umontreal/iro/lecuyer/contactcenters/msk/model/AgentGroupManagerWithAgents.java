package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ServiceTimeParams;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;

/**
 * Manages an agent group with detailed information on each agent.
 */
public class AgentGroupManagerWithAgents extends AgentGroupManager {
	private AgentInfo[] agents;
	private ShiftEvent[] agEvents;
	private final AgentGroupParams par;

	/**
	 * Creates an agent group manager with the call center model \texttt{cc},
	 * agent group parameters \texttt{par}, and agent group index \texttt{i}.
	 * 
	 * @param cc
	 *            the call center model.
	 * @param par
	 *            the agent group parameters.
	 * @param i
	 *            the agent group index.
	 * @throws AgentGroupCreationException
	 *             if an error occurs during the creation of the agent group
	 *             manager.
	 */
	public AgentGroupManagerWithAgents(CallCenter cc, AgentGroupParams par,
			int i) throws AgentGroupCreationException {
		super(cc, par, i);
		this.par = par;
		agents = new AgentInfo[par.getAgents().size()];
		int idx = 0;
		for (final AgentParams agPar : par.getAgents())
			try {
				agents[idx] = new AgentInfo(cc, agPar);
				++idx;
			} catch (final IllegalArgumentException iae) {
				throw new AgentGroupCreationException(
						"Cannot create the agent with index " + idx, iae);
			}
		agEvents = new ShiftEvent[agents.length];
		for (int j = 0; j < agents.length; j++)
			agEvents[j] = new ShiftEvent(getAgentGroup(),
					new Agent[] { agents[j].getAgent() }, agents[j].getShift());
	}

	/**
	 * Constructs and returns a detailed agent group, which is needed to add and
	 * remove agents.
	 */
	@Override
	protected AgentGroup createAgentGroup(AgentGroupParams par, int i)
			throws AgentGroupCreationException {
		if (par.isSetDetailed() && !par.isDetailed())
			throw new AgentGroupCreationException(
					"An agent group with agents must be detailed; set the detailed attribute to true");

		DetailedAgentGroup ag = new DetailedAgentGroup(getCallCenter()
				.simulator(), 0);
		ag.setAgentGroupParam(par);

		return ag;
	}

	@Override
	public DetailedAgentGroup getAgentGroup() {
		return (DetailedAgentGroup) super.getAgentGroup();
	}

	/**
	 * Returns an array containing an information object for each agent in this
	 * group.
	 * 
	 * @return the array of agent information objects.
	 */
	public AgentInfo[] getAgents() {
		return agents.clone();
	}

	/**
	 * Returns the number of agents in this group.
	 * 
	 * @return the number of agents in this group.
	 */
	public int getNumAgents() {
		return agents.length;
	}

	/**
	 * Returns the agent with index \texttt{i} in this group.
	 * 
	 * @param i
	 *            the index of the agent.
	 * @return the agent information object.
	 */
	public AgentInfo getAgent(int i) {
		return agents[i];
	}

	@Override
	/**
	 * @throws IllegalStateException if an error occurs during the construction
	 * of a service time distribution.
	 */
	public void init() {
		super.init();
		int agentIndex=0;
		for (final AgentInfo agent : agents)
			agent.getAgent().init();

		if (getCallCenter().getPeriodChangeEvent().isLockedPeriod()) {
			getAgentGroup().setNumAgents(0);
			final int mp = getCallCenter().getPeriodChangeEvent()
					.getCurrentMainPeriod();
			for (final AgentInfo agent : agents) {
				final boolean[] shiftVector = agent.getShift().getShiftVector(
						getCallCenter().getPeriodChangeEvent());
				if (shiftVector[mp])
					getAgentGroup().addAgent(agent.getAgent());
			}
		} else
			for (final ShiftEvent event : agEvents) {
				try {
					event.init(
							getProbDisconnectStream(),
							1,
							getListMapServiceTimeAgentGroup(),agentIndex);
					agentIndex++;
					event.schedule();
				}
				catch (final DistributionCreationException dce) {
					throw new IllegalStateException(dce);
				}
				catch (final GeneratorCreationException gce) {
	               throw new IllegalStateException(gce);
	            }
			}
	}

	/**
	 * Computes and returns the shift matrix. Element $(j, p)$ of this $J\times
	 * P$ matrix, where $J$ corresponds to the number of shifts and $P$, to the
	 * number of main periods, is \texttt{true} if and only if agents are
	 * scheduled to work on shift \texttt{j} during main period \texttt{p}.
	 */
	public boolean[][] getShiftMatrix() {
		final PeriodChangeEvent pce = getCallCenter().getPeriodChangeEvent();
		final boolean[][] res = new boolean[agents.length][];
		for (int i = 0; i < agents.length; i++)
			res[i] = agents[i].getShift().getShiftVector(pce);
		return res;
	}

	/**
	 * Similar to {@link #getShiftMatrix()}, but returns a matrix of integers,
	 * with 0 meaning \texttt{false}, and 1 meaning \texttt{true}.
	 */
	public int[][] getShiftMatrixInt() {
		final PeriodChangeEvent pce = getCallCenter().getPeriodChangeEvent();
		final int[][] res = new int[agents.length][];
		for (int i = 0; i < agents.length; i++)
			res[i] = agents[i].getShift().getShiftVectorInt(pce);
		return res;
	}

	@Override
	public int[] getStaffing() {
		final boolean[][] shiftMatrix = getShiftMatrix();
		final int[] staffing = new int[getCallCenter().getNumMainPeriods()];
		for (int mp = 0; mp < staffing.length; mp++)
			for (final boolean[] element : shiftMatrix)
				staffing[mp] += element[mp] ? 1 : 0;
		return staffing;
	}

	@Override
	public int getStaffing(int mp) {
		final int[] staffing = getStaffing();
		return staffing[mp];
	}

	/**
	 * Returns a list of maps of service time distributions for each agent in this group.
	 * 
	 * Let $I$ be the number of agents in this group,
	 * the element at index $i-1$ of this list will contain a map that holds
	 * the service time distributions defined specifically for each call type for agent $i$.
	 * If no specific distribution for any call type has been given for agent $i$,
	 * this element will be \texttt{null}.
	 * 
	 * The keys of a map are the call type ids and the values are the service time distributions. 
	 * 
	 * @return a list of maps of specifically defined service time distributions for each agent of this group.
	 * @throws DistributionCreationException if an error occurs during the construction
	 * of a service time distribution.
	 * @throws GeneratorCreationException if an error occurs during the construction
	 * of a service time distribution.
	 */
	public ArrayList<Map<Integer, MultiPeriodGen>> getListMapServiceTimeAgentGroup() throws DistributionCreationException, GeneratorCreationException {
		CallCenter cc = getCallCenter();
		
		ArrayList<Map<Integer, MultiPeriodGen>> list = new ArrayList<Map<Integer, MultiPeriodGen>>();
		for (int i = 0; i < par.getAgents().size(); i++) {

			if (par.getAgents().get(i).isSetServiceTime()) {
				Map<Integer, MultiPeriodGen> map = null;
				MultiPeriodGen sgen = null;
				RandomStream sStream;
				RandomStreams sStream1 = cc.getRandomStreams();
				map = new HashMap<Integer, MultiPeriodGen>();

				int cpt = 0;
				for (ServiceTimeParams stg : par.getAgents().get(i)
						.getServiceTime()) {

							sStream=sStream1.getAgServiceTimeStream1().get(getAgentGroup().getId()).get(i).get(cpt);
					
					cpt++;
					try {
						sgen = CCParamReadHelper.createGenerator(stg, sStream,
								cc.getPeriodChangeEvent());

						sgen.setTargetTimeUnit(cc.getDefaultUnit());
						map.put(stg.getCallType(), sgen);
					} catch (final DistributionCreationException dce) {
			               throw dce;
		            } catch (final GeneratorCreationException gce) {
		               throw gce;
		            }
				}
				list.add(map);
			} else
				list.add(null);
		}
		return list;
	}

}
