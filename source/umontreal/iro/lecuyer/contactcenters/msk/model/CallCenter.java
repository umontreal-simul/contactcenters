package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.app.AbstractContactCenterInfo;
import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.expdelay.LastWaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.DialerParams;
import umontreal.iro.lecuyer.contactcenters.msk.stat.AWTPeriod;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.PriorityWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.QueueWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.WaitingQueueStructure;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.BasicRandomStreamFactory;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.util.ClassFinderWithBase;
import umontreal.ssj.util.NameConflictException;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;

/**
 * Represents the model of a call center with multiple call types and agent
 * groups. The model encapsulates all the logic of the call center itself: a
 * simulator with a clock and event list, a simulation event marking the change
 * of periods, the call factories which create objects for every call, manager
 * objects for arrival processes, dialers, agent groups, and the router, etc. A
 * program can use methods in this class to obtain references to the call center
 * objects, and retrieve their parameters, or register listeners to observe
 * their evolution in time.
 * 
 * A model is created from an instance of {@link CallCenterParams}, and an
 * instance of {@link RandomStreams}. After it is created using the
 * {@link #create()} method, it can be initialized for simulation using
 * {@link #initSim()}. The encapsulated period-change event, and managed arrival
 * processes and dialers must then be started to schedule events before the
 * simulation is started using \texttt{simulator().start()}.
 */
public class CallCenter extends AbstractContactCenterInfo {
	private static final ClassFinderWithBase<WaitingTimePredictor> cfPred = new ClassFinderWithBase<WaitingTimePredictor>(
			WaitingTimePredictor.class);
	static {
		cfPred.getImports()
				.add("umontreal.iro.lecuyer.contactcenters.expdelay.*");
	}

	// private Logger modelLogger = Logger.getLogger
	// ("umontreal.iro.lecuyer.msk.model");
	private CallCenterParams ccParams;

	private Map<String, Object> properties;

	private ServiceLevelParamReadHelper[] slp;

	private TimeUnit defaultUnit;

	// Most of the times, periodDurationDouble is used,
	// but the original duration, coming from the XML
	// parameter file, is useful to compute
	// the starting date and time of periods, shown
	// in statistical reports.
	private double periodDurationDouble;

	private Duration periodDuration;

	private Date startingDate;

	private long startingTimeInMillis;

	private double startingTime;

	private boolean oneDayHorizon;

	private DateFormat periodDateFormat;

	private RandomStreams streams;

	private double b = 1.0;

	private Simulator sim;

	private PeriodChangeEvent pce;

	private RouterManager routerManager;

	// Necessary since arrivalProc.length != Ki
	// as there can be arrival processes creating
	// multiple calls. The total number of call types
	// is accessible through factories.length.
	private int numInCallTypes;

	private CallFactory[] factories;

	private AgentGroupManager[] agentGroups;

	private RandomVariateGen bgen;

	private CallTransferManager transferManager;

	private VirtualHoldManager virtualHoldManager;

	private ArrivalProcessManager[] arrivProc;

	private DialerManager[] dialers;

	private DialerObjects dialerObjects;

	private SegmentInfo[] inboundTypeSegments;

	private SegmentInfo[] outboundTypeSegments;

	private SegmentInfo[] callTypeSegments;

	private SegmentInfo[] agentGroupSegments;

	private SegmentInfo[] periodSegments;

	private WaitingQueue[] queues;

	private boolean[][] expPatienceTimes;

	private boolean[][][] expServiceTimes;

	private double arrivalsMult;

	private double patienceTimesMult;

	private double serviceTimesMult;

	private double conferenceTimesMult;

	private double preServiceTimesNoConfMult;

	private double transferTimesMult;

	private double previewTimesMult;

	private boolean convertScheduleToStaffing;

	private double agentsMult;

	private Class<? extends WaitingTimePredictor> predClass;

	private int queueCapacity;

	private boolean[][] defaultShiftMatrix;

        private StartingState startingState;
        
	/**
	 * Gets the period from which to obtain the acceptable waiting time, for the
	 * bad contact mismatch rate dialer policy.
	 */
	private AWTPeriod awtPeriod;

	/**
	 * Constructs a new call center model from the call center parameters
	 * \texttt{ccParams}, and the random streams \texttt{streams}. This
	 * constructor assumes that \texttt{ccParams} is valid. The class
	 * {@link CallCenterParamsConverter} can be used to create valid objects from
	 * XML files.
	 * 
	 * Note that the {@link #create()} method must be called after the model is
	 * constructed in order to create the model.
	 * 
	 * @param ccParams
	 * @param streams
	 * @exception NullPointerException
	 *               if \texttt{ccParams} or \texttt{streams} are \texttt{null}.
	 */
	public CallCenter(CallCenterParams ccParams, RandomStreams streams) {
		this(Simulator.getDefaultSimulator(), ccParams, streams);
	}

	/**
	 * Similar to {@link #CallCenter(CallCenterParams,RandomStreams)}, with the
	 * given simulator \texttt{sim}.
	 */
	public CallCenter(Simulator sim, CallCenterParams ccParams,
			RandomStreams streams) {
		if (sim == null)
			throw new NullPointerException();
		if (ccParams == null || streams == null)
			throw new NullPointerException();
		this.ccParams = ccParams;
		this.streams = streams;
		this.sim = sim;
	}

	/**
	 * Creates a call center model with parameters stored in \texttt{ccParams},
	 * and using the default class of random stream {@link MRG32k3a}.
	 * 
	 * Note that the {@link #create()} method must be called after the model is
	 * constructed in order to create the model.
	 * 
	 * @param ccParams
	 *           the parameters of the call center.
	 * @exception NullPointerException
	 *               if \texttt{ccParams} is \texttt{null}.
	 */
	public CallCenter(CallCenterParams ccParams) {
		this(Simulator.getDefaultSimulator(), ccParams);
	}

	/**
	 * Similar to {@link #CallCenter(CallCenterParams)}, with the given simulator
	 * \texttt{sim}.
	 */
	public CallCenter(Simulator sim, CallCenterParams ccParams) {
		if (sim == null)
			throw new NullPointerException();
		if (ccParams == null)
			throw new NullPointerException();
		this.ccParams = ccParams;
		streams = new RandomStreams(new BasicRandomStreamFactory(MRG32k3a.class),
				ccParams);
		this.sim = sim;
	}

	/**
	 * Returns the simulator associated with this call center model. The
	 * simulator is used to schedule events, and obtain simulation time when
	 * necessary.
	 * 
	 * @return the associated simulator.
	 */
	public final Simulator simulator() {
		return sim;
	}

	/**
	 * Sets the simulator of this model to \texttt{sim}. After this method is
	 * called, the model should be reset using the {@link #create()} method.
	 * 
	 * @param sim
	 *           the new simulator.
	 * @exception NullPointerException
	 *               if \texttt{sim} is \texttt{null}.
	 */
	public final void setSimulator(Simulator sim) {
		if (sim == null)
			throw new NullPointerException();
		this.sim = sim;
	}

	/**
	 * Converts the given duration \texttt{d} to a time in the default time unit.
	 * This method calls {@link Duration#getTimeInMillis(Date)} using the date
	 * returned by {@link #getStartingDate()}. It then uses
	 * {@link TimeUnit#convert(double,TimeUnit,TimeUnit)} to convert the obtained
	 * time in milliseconds to the default unit given by
	 * {@link #getDefaultUnit()}.
	 * 
	 * @param d
	 *           the duration to be converted.
	 * @return the duration in the default time unit.
	 */
	public double getTime(Duration d) {
		return TimeUnit.convert(d.getTimeInMillis(startingDate),
				TimeUnit.MILLISECOND, defaultUnit);
	}

	/**
	 * Constructs and returns an array whose elements correspond to the time
	 * durations in the given array, converted to the default time unit. This
	 * method constructs an array with the same length as \texttt{d}, and sets
	 * element \texttt{i} of the target array to the result of
	 * {@link #getTime(Duration)} called with \texttt{d[i]}.
	 * 
	 * @param d
	 *           the array of durations to convert.
	 * @return the array of converted durations.
	 */
	public double[] getTime(Duration... d) {
		final double[] res = new double[d.length];
		for (int i = 0; i < res.length; i++)
			res[i] = getTime(d[i]);
		return res;
	}

	/**
	 * Similar to {@link #getTime(Duration...)}, for a 2D array.
	 * 
	 * @param d
	 *           the 2D array of durations to convert.
	 * @return the 2D array of converted durations.
	 */
	public double[][] getTime(Duration[][] d) {
		final double[][] res = new double[d.length][];
		for (int i = 0; i < res.length; i++)
			res[i] = d[i] == null ? null : getTime(d[i]);
		return res;
	}

	/**
	 * Converts the time returned by
	 * {@link CallCenterUtil#getTimeInMillis(XMLGregorianCalendar)} to the
	 * default time unit returned by {@link #getDefaultUnit()}.
	 * 
	 * @param xgcal
	 *           the XML gregorian calendar representing a time.
	 * @return the time in the default time unit.
	 */
	public double getTime(XMLGregorianCalendar xgcal) {
		return TimeUnit.convert(CallCenterUtil.getTimeInMillis(xgcal),
				TimeUnit.MILLISECOND, defaultUnit);
	}

	/**
	 * Similar to {@link #getTime(Duration...)}, for an array of XML gregorian
	 * calendars.
	 * 
	 * @param d
	 *           the array of times to convert.
	 * @return the array of converted times.
	 */
	public double[] getTime(XMLGregorianCalendar... d) {
		final double[] res = new double[d.length];
		for (int i = 0; i < res.length; i++)
			res[i] = getTime(d[i]);
		return res;
	}

	/**
	 * Similar to {@link #getTime(XMLGregorianCalendar...)}, for a 2D array.
	 * 
	 * @param d
	 *           the 2D array of times to convert.
	 * @return the 2D array of converted times.
	 */
	public double[][] getTime(XMLGregorianCalendar[][] d) {
		final double[][] res = new double[d.length][];
		for (int i = 0; i < res.length; i++)
			res[i] = d[i] == null ? null : getTime(d[i]);
		return res;
	}

	/**
	 * Returns the default unit used for this call center. This corresponds to
	 * the unit for simulation time, and for output such as waiting times, excess
	 * times, etc.
	 * 
	 * @return the default unit.
	 */
	public TimeUnit getDefaultUnit() {
		return defaultUnit;
	}

	/**
	 * Returns the duration of main periods, expressed in the default time unit
	 * returned by {@link #getDefaultUnit()}.
	 * 
	 * @return the period duration.
	 */
	public double getPeriodDuration() {
		return periodDurationDouble;
	}

	/**
	 * Returns the date corresponding to the environment being modeled. This
	 * corresponds to the date at which the preliminary period begins, with time
	 * set to midnight. This corresponds to the current date, at which this
	 * object was created, if no date was given explicitly in parameters.
	 * 
	 * @return the date corresponding to the considered environment.
	 */
	public Date getStartingDate() {
		return startingDate;
	}

	/**
	 * Returns the starting time of the first main period, expressed in the
	 * default time unit.
	 * 
	 * @return the starting time of the first main period.
	 */
	public double getStartingTime() {
		return startingTime;
	}

	/**
	 * Returns the date corresponding to the beginning of the main period
	 * \texttt{mp}. This method adds the starting time, and \texttt{mp} times the
	 * period duration to the date returned by {@link #getStartingDate()}, and
	 * returns the resulting date.
	 * 
	 * @param mp
	 *           the index of the main period.
	 * @return the date of the main period.
	 */
	public Date getMainPeriodStartingDate(int mp) {
		if (mp < 0 || mp >= ccParams.getNumPeriods())
			throw new IllegalArgumentException("Invalid period index " + mp);
		final Date date = new Date(startingDate.getTime());
		if (mp > 0)
			periodDuration.multiply(mp).addTo(date);
		date.setTime(date.getTime() + startingTimeInMillis);
		return date;
	}

	/**
	 * Determines if the horizon of this model spans multiple days, i.e., if the
	 * period duration times the number of periods is larger than 24 hours. This
	 * method determines the starting dates of the first and last main periods,
	 * using {@link #getMainPeriodStartingDate(int)}, and returns \texttt{true}
	 * if and only if the two dates have different days according to the
	 * Gregorian calendar.
	 * 
	 * @return the success indicator of the test.
	 */
	public boolean isHorizonSpanningDays() {
		return oneDayHorizon;
	}

	/**
	 * Returns the number of sets of parameters for the service level given by
	 * the user in parameter file.
	 * 
	 * @return the number of sets of parameters for the service level.
	 */
	public int getNumMatricesOfAWT() {
		return slp.length;
	}

	/**
	 * Returns the name of the matrix of acceptable waiting time with index
	 * \texttt{m}, or \texttt{null} if no name was given in the parameter file.
	 * 
	 * @param m
	 *           the index of the matrix.
	 * @return the name corresponding to the matrix, or \texttt{null}.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{m} is negative or greater than or equal to the
	 *               value returned by {@link #getNumMatricesOfAWT()}.
	 */
	public String getMatrixOfAWTName(int m) {
		return slp[m].getName();
	}

	/**
	 * Returns the set of parameters \texttt{m} for the service level.
	 * 
	 * @param m
	 *           the index of the set.
	 * @return the set of parameters.
	 */
	public ServiceLevelParamReadHelper getServiceLevelParams(int m) {
		return slp[m];
	}

	/**
	 * Returns the call center parameters associated with this model.
	 * 
	 * @return the associated call center parameters.
	 */
	public CallCenterParams getCallCenterParams() {
		return ccParams;
	}

	/**
	 * Returns the random streams used by this model.
	 * 
	 * @return the random streams used.
	 */
	public RandomStreams getRandomStreams() {
		return streams;
	}

	/**
	 * Returns the random streams associated with this simulator.
	 * 
	 * @return the associated random streams.
	 * @deprecated Use {@link #getRandomStreams()} instead.
	 */
	@Deprecated
	public RandomStreams getStreams() {
		return streams;
	}

	/**
	 * Sets the random streams used by this model to \texttt{streams}. This
	 * method calls {@link RandomStreams#createStreams(CallCenterParams)} on the
	 * call center parameters to create necessary streams.
	 * 
	 * Note that the new random streams are used only after {@link #create()} is
	 * called.
	 * 
	 * @param streams
	 *           the new random streams.
	 * @exception NullPointerException
	 *               if \texttt{streams} id \texttt{null}.
	 */
	public void setRandomStreams(RandomStreams streams) {
		if (streams == null)
			throw new NullPointerException();
		streams.createStreams(ccParams);
		this.streams = streams;
	}

	/**
	 * Calls {@link RandomStream#resetNextSubstream()} on every random stream of
	 * this model.
	 */
	public void resetNextSubstream() {
		final RandomStreams streams1 = getRandomStreams();
		for (final RandomStream s : streams1.getRandomStreamsInit())
			s.resetNextSubstream();
		for (final RandomStream s : streams1.getRandomStreamsSim())
			s.resetNextSubstream();
	}

	/**
	 * Calls {@link RandomStream#resetStartStream()} on every random stream of
	 * this model.
	 */
	public void resetStartStream() {
		final RandomStreams streams1 = getRandomStreams();
		for (final RandomStream s : streams1.getRandomStreamsInit())
			s.resetStartStream();
		for (final RandomStream s : streams1.getRandomStreamsSim())
			s.resetStartStream();
	}

	/**
	 * Calls {@link RandomStream#resetStartSubstream()} on each random stream of
	 * this model.
	 */
	public void resetStartSubstream() {
		final RandomStreams streams1 = getRandomStreams();
		for (final RandomStream s : streams1.getRandomStreamsInit())
			s.resetStartSubstream();
		for (final RandomStream s : streams1.getRandomStreamsSim())
			s.resetStartSubstream();
	}

	/**
	 * Recreates the model with new parameters. This class sets the call center
	 * parameters to \texttt{ccParams}, the random streams to \texttt{streams},
	 * and calls {@link #create()} to recreate the model.
	 * 
	 * @param ccParams1
	 *           the new call center parameters.
	 * @param streams1
	 *           the new random streams.
	 * @exception NullPointerException
	 *               if \texttt{ccParams} or \texttt{streams} are \texttt{null}.
	 */
	public void reset(CallCenterParams ccParams1, RandomStreams streams1)
			throws CallCenterCreationException {
		if (ccParams1 == null || streams1 == null)
			throw new NullPointerException();
		this.ccParams = ccParams1;
		this.streams = streams1;
		create();
	}

	/**
	 * Calls {@link #create(boolean) create} \texttt{(false)}.
	 * 
	 * @throws CallCenterCreationException
	 *            if an error occurs during the creation of the model.
	 */
	public void create() throws CallCenterCreationException {
		create(false);
	}

	/**
	 * Constructs the elements of the call center. This method is called by the
	 * constructor or by {@link #reset(CallCenterParams,RandomStreams)}. If
	 * \texttt{recreateStreams} is \texttt{true}, a new {@link RandomStreams}
	 * object is created and associated with this model; this results in a change
	 * of seeds for every random stream used. If \texttt{recreateStreams} is
	 * \texttt{false}, the same random streams are kept, and new ones are created
	 * just if needed.
	 * 
	 * Since this method recreates the complete structure of the call center, any
	 * listener observing the evolution of the model must be re-registered after
	 * this method returns.
	 * 
	 * @param recreateStreams
	 *           determines if random streams are recreated.
	 * @throws CallCenterCreationException
	 *            if an error occurs during the creation of the model.
	 */
	public void create(boolean recreateStreams)
			throws CallCenterCreationException {
            if (recreateStreams)
                    streams = new RandomStreams(streams.getRandomStreamFactory(), ccParams);
            else
                    streams.createStreams(ccParams);
            properties = ParamReadHelper
                            .unmarshalProperties(ccParams.getProperties());
            if (ccParams.isSetQueueCapacity())
                    queueCapacity = ccParams.getQueueCapacity();
            else
                    queueCapacity = Integer.MAX_VALUE;

            defaultUnit = TimeUnit.valueOf(ccParams.getDefaultUnit().name());
            startingDate = CallCenterUtil.getDate(ccParams.getStartingDate())
                            .getTime();
            if (ccParams.isSetStartingTime()) {
                    startingTimeInMillis = ccParams.getStartingTime().getTimeInMillis(
                                    startingDate);
                    startingTime = TimeUnit.convert(startingTimeInMillis,
                                    TimeUnit.MILLISECOND, defaultUnit);
            } else {
                    startingTimeInMillis = 0;
                    startingTime = 0;
            }
            arrivalsMult = ccParams.getArrivalsMult();
            patienceTimesMult = ccParams.getPatienceTimesMult();
            serviceTimesMult = ccParams.getServiceTimesMult();
            conferenceTimesMult = ccParams.getConferenceTimesMult();
            preServiceTimesNoConfMult = ccParams.getPreServiceTimesNoConfMult();
            transferTimesMult = ccParams.getTransferTimesMult();
            previewTimesMult = ccParams.getPreviewTimesMult();
            convertScheduleToStaffing = ccParams.isConvertSchedulesToStaffing();
            agentsMult = ccParams.getAgentsMult();
            if (ccParams.isSetShiftMatrix()) {
                    defaultShiftMatrix = ArrayConverter.unmarshalArray(ccParams
                                    .getShiftMatrix());
                    try {
                            ArrayUtil.checkRectangularMatrix(defaultShiftMatrix);
                    } catch (final IllegalArgumentException iae) {
                            throw new CallCenterCreationException(
                                            "Non-rectangular default matrix of shifts", iae);
                    }
                    if (defaultShiftMatrix.length > 0
                                    && defaultShiftMatrix[0].length != ccParams.getNumPeriods())
                            throw new CallCenterCreationException(
                                            "The default matrix of shifts must have one column per main period");
            } else
                    defaultShiftMatrix = null;

            slp = new ServiceLevelParamReadHelper[ccParams.getServiceLevelParams()
                            .size()];
            for (int i = 0; i < slp.length; i++)
                    slp[i] = new ServiceLevelParamReadHelper(ccParams
                                    .getServiceLevelParams().get(i), startingDate, defaultUnit);
            periodDuration = ccParams.getPeriodDuration();
            periodDurationDouble = getTime(periodDuration);

            final int P = ccParams.getNumPeriods();
            pce = new PeriodChangeEvent(sim, getPeriodDuration(), P + 2,
                            getStartingTime());

            final GregorianCalendar cal1 = new GregorianCalendar();
            final GregorianCalendar cal2 = new GregorianCalendar();
            cal1.setTime(getMainPeriodStartingDate(0));
            cal2.setTime(getMainPeriodStartingDate(getNumMainPeriods() - 1));
            oneDayHorizon = cal1.get(Calendar.DAY_OF_YEAR) == cal2
                            .get(Calendar.DAY_OF_YEAR);
            if (oneDayHorizon)
                    periodDateFormat = DateFormat.getTimeInstance();
            else
                    periodDateFormat = DateFormat.getDateTimeInstance();

            numInCallTypes = ccParams.getInboundTypes().size();

            final int K = numInCallTypes + ccParams.getOutboundTypes().size();
            final int KI = numInCallTypes;
            factories = new CallFactory[K];
            for (int k = 0; k < K; k++)
                    try {
                            factories[k] = CallFactory.create(this, ccParams, k);
                    } catch (final CallFactoryCreationException cfe) {
                            throw new CallCenterCreationException(
                                            "Cannot create call factory for "
                                                            + CallCenterUtil.getCallTypeInfo(ccParams, k), cfe);
                    }
            for (int k = 0; k < K; k++) {
                    try {
                            factories[k].initTransferTargets(ccParams, k);
                    } catch (final CallFactoryCreationException cfe) {
                            throw new CallCenterCreationException(
                                            "Cannot create call factory for "
                                                            + CallCenterUtil.getCallTypeInfo(ccParams, k), cfe);
                    }
                    int vqTarget = factories[k].getTargetVQType();
                    if (vqTarget >= 0)
                            factories[vqTarget].setDisableCallSource(true);
            }

            final int I = ccParams.getAgentGroups().size();
            agentGroups = new AgentGroupManager[I];
            for (int i = 0; i < I; i++)
                    try {
                            agentGroups[i] = AgentGroupManager.create(this, ccParams
                                            .getAgentGroups().get(i), i);
                    } catch (final AgentGroupCreationException ace) {
                            throw new CallCenterCreationException("Cannot create "
                                            + CallCenterUtil.getAgentGroupInfo(ccParams, i), ace);
                    }

            try {
                    inboundTypeSegments = SegmentInfo.getSegments(ccParams
                                    .getInboundTypeSegments());
                    SegmentInfo.checkRange(0, KI, inboundTypeSegments);
            } catch (final IllegalArgumentException iae) {
                    throw new CallCenterCreationException(
                                    "Error creating inbound type segments", iae);
            }
            try {
                    outboundTypeSegments = SegmentInfo.getSegments(ccParams
                                    .getOutboundTypeSegments());
                    SegmentInfo.checkRange(KI, K, outboundTypeSegments);
            } catch (final IllegalArgumentException iae) {
                    throw new CallCenterCreationException(
                                    "Error creating outbound type segments", iae);
            }
            try {
                    callTypeSegments = SegmentInfo.getSegments(ccParams
                                    .getCallTypeSegments());
                    SegmentInfo.checkRange(0, K, callTypeSegments);
            } catch (final IllegalArgumentException iae) {
                    throw new CallCenterCreationException(
                                    "Error creating call type segments", iae);
            }
            try {
                    agentGroupSegments = SegmentInfo.getSegments(ccParams
                                    .getAgentGroupSegments());
                    SegmentInfo.checkRange(0, I, agentGroupSegments);
            } catch (final IllegalArgumentException iae) {
                    throw new CallCenterCreationException(
                                    "Error creating agent group segments", iae);
            }
            try {
                    periodSegments = SegmentInfo.getSegments(ccParams.getPeriodSegments());
                    SegmentInfo.checkRange(0, P, periodSegments);
            } catch (final IllegalArgumentException iae) {
                    throw new CallCenterCreationException(
                                    "Error creating period segments", iae);
            }

            try {
                    routerManager = new RouterManager(this, ccParams.getRouter());
            } catch (final RouterCreationException rce) {
                    throw new CallCenterCreationException("Cannot create router", rce);
            }
            final Router router = routerManager.getRouter();
            router.setTotalQueueCapacity(queueCapacity);
            for (int k = 0; k < factories.length; k++)
                    router.setContactFactory(k, factories[k]);
            for (final AgentGroupManager group : agentGroups)
                    group.connectToRouter(router);

            final int KO = ccParams.getOutboundTypes().size();
            final int numDialers = KO + ccParams.getDialers().size();
            dialers = new DialerManager[numDialers];
            for (int k = 0; k < KO; k++) {
                    final DialerParams par = ccParams.getOutboundTypes().get(k)
                                    .getDialer();
                    if (par != null)
                            try {
                                    dialers[k] = new DialerManager(this, par, k);
                            } catch (final DialerCreationException dce) {
                                    throw new CallCenterCreationException(
                                                    "Cannot create dialer for outbound "
                                                                    + CallCenterUtil.getCallTypeInfo(ccParams, k + KI),
                                                    dce);
                            }
            }
            for (int k = KO; k < numDialers; k++) {
                    final DialerParams par = ccParams.getDialers().get(k - KO);
                    if (par != null)
                            try {
                                    dialers[k] = new DialerManager(this, par, k);
                            } catch (final DialerCreationException dce) {
                                    throw new CallCenterCreationException(
                                                    "Cannot create multiple-types dialer " + (k - KO));
                            }
            }

            final int numArrivProc = KI + ccParams.getArrivalProcesses().size();
            arrivProc = new ArrivalProcessManager[numArrivProc];
            for (int k = 0; k < KI; k++) {
                    final ArrivalProcessParams par = ccParams.getInboundTypes().get(k)
                                    .getArrivalProcess();
                    if (par != null)
                            try {
                                    arrivProc[k] = new ArrivalProcessManager(this, par, k);
                            } catch (final ArrivalProcessCreationException ace) {
                                    throw new CallCenterCreationException(
                                                    "Cannot create arrival process for inbound "
                                                                    + CallCenterUtil.getCallTypeInfo(ccParams, k), ace);
                            }
            }
            for (int k = KI; k < numArrivProc; k++) {
                    final ArrivalProcessParams par = ccParams.getArrivalProcesses().get(
                                    k - KI);
                    if (par != null)
                            try {
                                    arrivProc[k] = new ArrivalProcessManager(this, par, k);
                            } catch (final ArrivalProcessCreationException ace) {
                                    throw new CallCenterCreationException(
                                                    "Cannot create multiple-types arrival process " + (k - KI),
                                                    ace);
                            }
            }
            try {
                    if (ccParams.isSetBusynessGen())
                            bgen = ParamReadHelper.createGenerator(ccParams.getBusynessGen(),
                                            streams.getStreamB());
                    else
                            bgen = null;
            } catch (final DistributionCreationException dce) {
                    throw new CallCenterCreationException(
                                    "Cannot create distribution for the busyness factor", dce);
            } catch (final GeneratorCreationException gce) {
                    throw new CallCenterCreationException(
                                    "Cannot create generator for the busyness factor", gce);
            }

            final int Q = router.getNumWaitingQueues();
            queues = new WaitingQueue[Q];
            for (int q = 0; q < Q; q++) {
                    queues[q] = createWaitingQueue(q);
                    router.setWaitingQueue(q, queues[q]);
            }

            expPatienceTimes = new boolean[K][P];
            for (int k = 0; k < K; k++)
                    for (int mp = 0; mp < P; mp++) {
                            final MultiPeriodGen pgen = getCallFactory(k).getPatienceTimeGen();
                            boolean b1;
                            if (pgen == null)
                                    b1 = false;
                            else {
                                    final Distribution patienceDist = pgen.getGenerator(mp + 1)
                                                    .getDistribution();
                                    b1 = patienceDist instanceof ExponentialDist;
                            }
                            expPatienceTimes[k][mp] = b1;
                    }
            expServiceTimes = new boolean[K][I][P];
            for (int k = 0; k < K; k++)
                    for (int i = 0; i < I; i++)
                            for (int mp = 0; mp < P; mp++) {
                                    final CallFactory factory = getCallFactory(k);
                                    final MultiPeriodGen sgen = factory.getServiceTimesManager()
                                                    .getServiceTimeGen(i);
                                    boolean b1;
                                    if (sgen == null)
                                            b1 = false;
                                    else {
                                            final Distribution serviceDist = sgen.getGenerator(mp + 1)
                                                            .getDistribution();
                                            b1 = serviceDist instanceof ExponentialDist;
                                    }
                                    expServiceTimes[k][i][mp] = b1;
                            }

            for (int m = 0; m < slp.length; m++) {
                    final int Kip = getNumInContactTypesWithSegments();
                    if (slp[m].getRows() != 1 && slp[m].getRows() != Kip)
                            throw new CallCenterCreationException("The service level matrix "
                                            + m + " has " + slp[m].getRows()
                                            + " rows, but it needs 1 or " + Kip + " rows");
                    final int Pp = getNumMainPeriodsWithSegments();
                    if (slp[m].getColumns() != 1 && slp[m].getColumns() != Pp)
                            throw new CallCenterCreationException("The service level matrix "
                                            + m + " has " + slp[m].getColumns()
                                            + " columns, but it needs 1 or " + Pp + " columns");
            }
            if (ccParams.isSetWaitingTimePredictorClass())
                    try {
                            predClass = cfPred.findClass(ccParams
                                            .getWaitingTimePredictorClass());
                    } catch (final ClassNotFoundException cnfe) {
                            throw new CallCenterCreationException(
                                            "Cannot find class for waiting time predictor "
                                                            + ccParams.getWaitingTimePredictorClass(), cnfe);
                    } catch (final NameConflictException nce) {
                            throw new CallCenterCreationException(
                                            "*** Cannot find class for waiting time predictor "
                                                            + ccParams.getWaitingTimePredictorClass(), nce);
                    }
            else
                    predClass = LastWaitingTimePredictor.class;

            transferManager = null;
            for (int k = 0; k < factories.length && transferManager == null; k++)
                    if (factories[k].isCallTransferSupported())
                            transferManager = new CallTransferManager(this);
            virtualHoldManager = null;
            for (int k = 0; k < factories.length && virtualHoldManager == null; k++)
                    if (factories[k].isVirtualHoldSupported())
                            virtualHoldManager = new VirtualHoldManager(this);
            // Initialize multipliers for patience times, service times, etc.
            for (CallFactory factory : factories)
                    factory.init();
            for (ArrivalProcessManager arvProcInfo : arrivProc) {
                    if (arvProcInfo == null)
                            continue;
                    double bMean = bgen == null ? 1 : bgen.getDistribution().getMean();
                    double mult = arvProcInfo.getArrivalsMult() * arrivalsMult * bMean;
                    arvProcInfo.getArrivalProcess().setExpectedBusynessFactor(mult);
            }

            // read the starting state of the queues and groups at the start of the simulation
            if (ccParams.getStartingState() != null)
                startingState = new StartingState(ccParams);
            else
                startingState = null;
	}

	/**
	 * Returns the instance of {@link DialerObjects} associated with this model.
	 * If no such instance exists, it is constructed, stored for future use, and
	 * returned.
	 * 
	 * @return the dialer objects of this model.
	 */
	public DialerObjects getDialerObjects() {
            if (dialerObjects == null)
                    dialerObjects = new DialerObjects(this);
            return dialerObjects;
	}

	/**
	 * Constructs and returns the \texttt{q}th waiting queue for this call
	 * center. By default, this returns an instance of
	 * {@link StandardWaitingQueue} which is a FIFO queue without priority.
	 * 
	 * @param q
	 *           the index of the created waiting queue.
	 * @return the constructed waiting queue.
	 */
	protected WaitingQueue createWaitingQueue(int q) {
            final WaitingQueueStructure struct = getRouter()
                            .getNeededWaitingQueueStructure(q);
            final Comparator<? super DequeueEvent> cmp = getRouter()
                            .getNeededWaitingQueueComparator(q);
            final WaitingQueue queue;
            switch (struct) {
            case LIST:
                    queue = new StandardWaitingQueue();
                    break;
            case PRIORITY:
                    if (cmp == null)
                            queue = new QueueWaitingQueue();
                    else
                            queue = new QueueWaitingQueue(cmp);
                    break;
            case SORTEDSET:
                    if (cmp == null)
                            queue = new PriorityWaitingQueue();
                    else
                            queue = new PriorityWaitingQueue(cmp);
                    break;
            default:
                    throw new AssertionError();
            }
            final String wqn = getWaitingQueueName(q);
            if (wqn != null && wqn.length() > 0)
                    queue.setName(wqn);
            else
                    queue.setName("");
            return queue;
	}

	/**
	 * Initializes the model for a new simulation with a random busyness factor.
	 * This method first generates the busyness factor $B$ using the generator
	 * returned by {@link #getBusynessGen()}, or sets $B=1$ if no generator was
	 * given in parameter file for the busyness factor. It then calls
	 * {@link #initSim(double)} with the generated $B$ to complete
	 * initialization.
	 */
	public void initSim() {
		final double b1 = bgen == null ? 1.0 : bgen.nextDouble();
		initSim(b1);
	}

	/**
	 * Returns the current value of $B$ used by arrival processes.
	 * 
	 * @return the current value of $B$.
	 */
	public double getBusynessFactor() {
		return b;
	}

	/**
	 * Initializes the model for a new simulation setting the busyness factor of
	 * arrival processes to the given value \texttt{b}. This method initializes
	 * arrival processes, dialers, agent groups, waiting queues, and the router,
	 * without scheduling any event. Methods such as
	 * {@link PeriodChangeEvent#start()}, {@link ContactArrivalProcess#start()},
	 * etc.\ must be used to schedule events before starting the simulation.
	 * 
	 * @param b1 the busyness factor used.
	 */
	public void initSim(double b1) {
		// When using batch means, we use pce.setCurrentPeriod
		// to lock the period instead of pce.init.
		for (final CallFactory factory : factories)
			factory.init();
		this.b = b1;
		for (final ArrivalProcessManager arvProcInfo : arrivProc)
			if (arvProcInfo != null)
				arvProcInfo.init(b1);
		for (final AgentGroupManager group : agentGroups)
			group.init();
		for (final WaitingQueue queue : queues)
			queue.init();
		for (final DialerManager dialerInfo : dialers)
			if (dialerInfo != null)
				dialerInfo.init();
		routerManager.init();
		if (virtualHoldManager != null)
			virtualHoldManager.init();
	}
        
        
        /**
         * Returns the starting state of the waiting queues and agent groups.
         * Note that this option is only available with the simulation by replications
         * using {@link umontreal.iro.lecuyer.contactcenters.msk.simlogic.RepLogic}.
         * This parameter is read from {@link StartingStateParams} in the
         * call center parameter {@link CallCenterParams}.
         * 
         * @return the starting state of the waiting queues and agent groups
         */
        public StartingState getStartingState() {
           return startingState;
        }

        
	/**
	 * Determines if this model supports call transfers. This returns
	 * \texttt{true} if {@link CallFactory#isCallTransferSupported()} returns
	 * \texttt{true} for at least one call factory returned by
	 * {@link #getCallFactories()}.
	 * 
	 * @return \texttt{true} if and only if this model supports call transfers.
	 */
	public boolean isCallTransferSupported() {
		return transferManager != null;
	}

	/**
	 * Determines if this model supports virtual holding. This returns
	 * \texttt{true} if {@link CallFactory#isVirtualHoldSupported()} returns
	 * \texttt{true} for at least one call factory returned by
	 * {@link #getCallFactories()}.
	 * 
	 * @return \texttt{true} if and only if this model supports call virtual
	 *         holding.
	 */
	public boolean isVirtualHoldSupported() {
		return virtualHoldManager != null;
	}

	public int getNumMainPeriods() {
		return getPeriodChangeEvent().getNumMainPeriods();
	}

	public int getNumContactTypes() {
		return factories.length;
	}

	public int getNumInContactTypes() {
		return numInCallTypes;
	}

	public int getNumOutContactTypes() {
		return factories.length - numInCallTypes;
	}

	public int getNumAgentGroups() {
		return agentGroups.length;
	}

	public int getNumWaitingQueues() {
		return routerManager.getRouter().getNumWaitingQueues();
	}

	public String getContactTypeName(int k) {
		return factories[k].getName();
	}

	public Map<String, String> getContactTypeProperties(int k) {
		return CallCenterUtil.toStringValues(factories[k].getProperties());
	}

	public String getContactTypeSegmentName(int k) {
		return callTypeSegments[k].getName();
	}

	public Map<String, String> getContactTypeSegmentProperties(int k) {
		return CallCenterUtil.toStringValues(callTypeSegments[k].getProperties());
	}

	public String getInContactTypeSegmentName(int k) {
		return inboundTypeSegments[k].getName();
	}

	public Map<String, String> getInContactTypeSegmentProperties(int k) {
		return CallCenterUtil.toStringValues(inboundTypeSegments[k]
				.getProperties());
	}

	public String getOutContactTypeSegmentName(int k) {
		return outboundTypeSegments[k].getName();
	}

	public Map<String, String> getOutContactTypeSegmentProperties(int k) {
		return CallCenterUtil.toStringValues(outboundTypeSegments[k]
				.getProperties());
	}

	public String getAgentGroupName(int i) {
		return agentGroups[i].getName();
	}

	public Map<String, String> getAgentGroupProperties(int i) {
		return CallCenterUtil.toStringValues(agentGroups[i].getProperties());
	}

	public String getAgentGroupSegmentName(int i) {
		return agentGroupSegments[i].getName();
	}

	public Map<String, String> getAgentGroupSegmentProperties(int i) {
		return CallCenterUtil.toStringValues(agentGroupSegments[i]
				.getProperties());
	}

	public String getMainPeriodName(int mp) {
		return periodDateFormat.format(getMainPeriodStartingDate(mp));
	}

	public String getMainPeriodSegmentName(int mp) {
		return periodSegments[mp].getName();
	}

	public String getWaitingQueueName(int q) {
		final Router router = getRouter();
		switch (router.getWaitingQueueType()) {
		case AGENTGROUP:
			return q < getNumAgentGroups() ? getAgentGroupName(q) : "Extra queue "
					+ (q - getNumAgentGroups());
		case CONTACTTYPE:
			return q < getNumContactTypes() ? getContactTypeName(q)
					: "Extra queue" + (q - getNumContactTypes());
		default:
			return null;
		}
	}

	public int getNumWaitingQueueSegments() {
		// Adding waiting queue segments impose changes
		// in SimCallCenterStat
		// final Router router = getRouter ();
		// switch (router.getWaitingQueueType ()) {
		// case AGENTGROUP:
		// return getNumAgentGroupSegments ();
		// case CONTACTTYPE:
		// return getNumContactTypeSegments ();
		// default:
		// return 0;
		// }
		return 0;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getWaitingQueueProperties(int q) {
		final Router router = getRouter();
		switch (router.getWaitingQueueType()) {
		case AGENTGROUP:
			return q < getNumAgentGroups() ? getAgentGroupProperties(q)
					: Collections.EMPTY_MAP;
		case CONTACTTYPE:
			return q < getNumContactTypes() ? getContactTypeProperties(q)
					: Collections.EMPTY_MAP;
		default:
			return Collections.EMPTY_MAP;
		}
	}

	public String getWaitingQueueSegmentName(int q) {
		// final Router router = getRouter ();
		// switch (router.getWaitingQueueType ()) {
		// case AGENTGROUP:
		// return getAgentGroupSegmentName (q);
		// case CONTACTTYPE:
		// return getContactTypeSegmentName (q);
		// default:
		// return null;
		// }
		return null;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getWaitingQueueSegmentProperties(int q) {
		// final Router router = getRouter ();
		// switch (router.getWaitingQueueType ()) {
		// case AGENTGROUP:
		// return getAgentGroupSegmentProperties (q);
		// case CONTACTTYPE:
		// return getContactTypeSegmentProperties (q);
		// default:
		// return Collections.EMPTY_MAP;
		// }
		return Collections.EMPTY_MAP;
	}

	/**
	 * Returns the object used to compute the AWT period of contacts. This method
	 * returns \texttt{null} unless {@link #setAwtPeriod(AWTPeriod)} was called
	 * with a non-\texttt{null} value.
	 * 
	 * @return the object for AWT periods.
	 */
	public AWTPeriod getAwtPeriod() {
		return awtPeriod;
	}

	/**
	 * Sets the object for computing AWT periods to \texttt{awtPeriod}.
	 * 
	 * @param awtPeriod
	 *           the object for computing AWT periods.
	 */
	public void setAwtPeriod(AWTPeriod awtPeriod) {
		this.awtPeriod = awtPeriod;
	}

	/**
	 * Returns the period index used to obtain the period-specific acceptable
	 * waiting time for contact \texttt{contact}. If {@link #getAwtPeriod()}
	 * returns \texttt{null}, this method returns the main period index of the
	 * contact's arrival. Otherwise, it returns the result of
	 * \texttt{getAwtPeriod().getAwtPeriod (contact)}.
	 * 
	 * @param contact
	 *           the contact to be tested.
	 * @return the AWT period of the contact.
	 */
	public int getAwtPeriod(Contact contact) {
		if (awtPeriod == null)
			return getPeriodChangeEvent().getMainPeriod(
					((Call) contact).getArrivalPeriod());
		return awtPeriod.getAwtPeriod(contact);
	}

	/**
	 * Returns a reference to the period-change event used by this model. This
	 * event occurs at the beginning of each period of the horizon, and triggers
	 * updates of some parameters such as the staffing in agent groups. One
	 * should start this event using {@link PeriodChangeEvent#start()} to
	 * simulate the horizon, or use
	 * {@link PeriodChangeEvent#setCurrentPeriod(int)} to simulate a single
	 * period as if it was infinite in the model.
	 * 
	 * @return the period-change event.
	 */
	public PeriodChangeEvent getPeriodChangeEvent() {
		return pce;
	}

	/**
	 * @deprecated Use {@link RouterManager#setRouter(Router)} instead.
	 */
	@Deprecated
	public void setRouter(Router router) {
		routerManager.setRouter(router);
	}

	/**
	 * Returns a reference to the router used by this model. This method calls
	 * \texttt{getRouterManager().getRouter()}.
	 */
	public Router getRouter() {
		return routerManager.getRouter();
	}

	/**
	 * Returns a reference to the router manager of this model.
	 */
	public RouterManager getRouterManager() {
		return routerManager;
	}

	/**
	 * Returns a reference to the random variate generator used for the global
	 * busyness factor $B$ multiplying the arrival rates or number of arrivals of
	 * calls. This method returns \texttt{null} if no busyness factor is used.
	 * 
	 * @return the random variate generator for busyness factor.
	 */
	public RandomVariateGen getBusynessGen() {
		return bgen;
	}

	/**
	 * Sets the random variate generator for the global busyness factor to
	 * \texttt{bgen}.
	 * 
	 * @param bgen
	 *           the new random variate generator.
	 */
	public void setBusynessGen(RandomVariateGen bgen) {
		this.bgen = bgen;
	}

	/**
	 * Returns the maximal number of dialer managers in this model. This
	 * corresponds to the number of outbound call types plus the number of
	 * dialers that can generate calls of several types.
	 * 
	 * @return the number of dialers.
	 */
	public int getNumDialers() {
		return dialers.length;
	}

	/**
	 * Returns the dialer manager with index \texttt{k}. The first $\Ko$ dialers
	 * generate outbound calls of a single type while other dialers can generate
	 * calls of several types. This method returns \texttt{null} if $k$ is
	 * smaller than $\Ko$, and no dialer dedicated to calls of outbound type $k$
	 * exists.
	 * 
	 * @param k
	 *           the index of the dialer manager.
	 * @return the dialer manager.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumDialers()}.
	 */
	public DialerManager getDialerManager(int k) {
		return dialers[k];
	}

	/**
	 * Returns the dialer with index \texttt{k}, or \texttt{null} if $k$ is
	 * smaller than $\Ko$, and no dialer is dedicated to outbound calls of type
	 * \texttt{k}. This method calls {@link #getDialerManager(int)} with the
	 * given value of \texttt{k}, and returns the dialer associated with the
	 * returned dialer manager.
	 * 
	 * @param k
	 *           the index of the dialer.
	 * @return the dialer, or \texttt{null}.
	 */
	public Dialer getDialer(int k) {
		return dialers[k] == null ? null : dialers[k].getDialer();
	}

	/**
	 * Returns the array of dialer managers in this model. The first $\Ko$
	 * elements of the returned array represent dialers dedicated to a single
	 * type of outbound call, and may be \texttt{null} if no dialer is dedicated
	 * to a given call type.
	 */
	public DialerManager[] getDialerManagers() {
		return dialers;
	}

	/**
	 * Returns an array containing the dialers of this model. This method calls
	 * {@link #getDialerManagers()}, and creates an array with each element $k$
	 * being the dialer associated with the dialer manager $k$. As with
	 * {@link #getDialerManagers()}, some elements in the returned array might be
	 * \texttt{null}.
	 */
	public Dialer[] getDialers() {
		final Dialer[] res = new Dialer[dialers.length];
		for (int k = 0; k < dialers.length; k++)
			res[k] = dialers[k] == null ? null : dialers[k].getDialer();
		return res;
	}

	/**
	 * Returns the maximal number of arrival process managers in this model. This
	 * corresponds to the number of inbound call types plus the number of arrival
	 * processes that can generate calls of several types.
	 * 
	 * @return the number of arrival processes.
	 */
	public int getNumArrivalProcesses() {
		return arrivProc.length;
	}

	/**
	 * Returns the arrival process manager with index \texttt{k}. The first $\Ki$
	 * arrival processes generate inbound calls of a single type while other
	 * processes can generate calls of several types. This method returns
	 * \texttt{null} if $k$ is smaller than $\Ki$, and no arrival process
	 * dedicated to calls of inbound type $k$ exists.
	 * 
	 * @param k
	 *           the index of the arrival process manager.
	 * @return the arrival process manager.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumArrivalProcesses()}.
	 */
	public ArrivalProcessManager getArrivalProcessManager(int k) {
		return arrivProc[k];
	}

	/**
	 * Returns the arrival process with index \texttt{k}, or \texttt{null} if $k$
	 * is smaller than $\Ki$, and no arrival process is dedicated to inbound
	 * calls of type \texttt{k}. This method calls
	 * {@link #getArrivalProcessManager(int)} with the given value of \texttt{k},
	 * and returns the arrival process associated with the returned manager.
	 * 
	 * @param k
	 *           the index of the arrival process.
	 * @return the arrival process, or \texttt{null}.
	 */
	public ContactArrivalProcess getArrivalProcess(int k) {
		return arrivProc[k] == null ? null : arrivProc[k].getArrivalProcess();
	}

	/**
	 * Returns the array of arrival process managers in this model. The first
	 * $\Ki$ elements of the returned array represent arrival processes dedicated
	 * to a single type of inbound call, and may be \texttt{null} if no arrival
	 * process is dedicated to a given call type.
	 */
	public ArrivalProcessManager[] getArrivalProcesManagers() {
		return arrivProc.clone();
	}

	/**
	 * Returns an array containing the arrival processes of this model. This
	 * method calls {@link #getArrivalProcesManagers()}, and creates an array
	 * with each element $k$ being the arrival process associated with the
	 * manager $k$. As with {@link #getArrivalProcesManagers()}, some elements in
	 * the returned array might be \texttt{null}.
	 */
	public ContactArrivalProcess[] getArrivalProcesses() {
		final ContactArrivalProcess[] res = new ContactArrivalProcess[arrivProc.length];
		for (int k = 0; k < arrivProc.length; k++)
			res[k] = arrivProc[k] == null ? null : arrivProc[k]
					.getArrivalProcess();
		return res;
	}

	/**
	 * Returns the agent group manager with index \texttt{i}.
	 * 
	 * @param i
	 *           the index of the agent group.
	 * @return the agent group manager.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{i} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumAgentGroups()}.
	 */
	public AgentGroupManager getAgentGroupManager(int i) {
		return agentGroups[i];
	}

	/**
	 * Returns the agent group with index \texttt{i}. This method is equivalent
	 * to calling {@link #getAgentGroupManager(int)} and using
	 * {@link AgentGroupManager#getAgentGroup()}.
	 * 
	 * @param i
	 *           the index of the agent group.
	 * @return the agent group.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{i} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumAgentGroups()}.
	 */
	public AgentGroup getAgentGroup(int i) {
		return agentGroups[i] == null ? null : agentGroups[i].getAgentGroup();
	}

	/**
	 * Returns an array containing the agent group managers of this model.
	 */
	public AgentGroupManager[] getAgentGroupManagers() {
		return agentGroups.clone();
	}

	/**
	 * Returns an array containing the agent groups of this model.
	 */
	public AgentGroup[] getAgentGroups() {
		final AgentGroup[] res = new AgentGroup[agentGroups.length];
		for (int i = 0; i < agentGroups.length; i++)
			res[i] = agentGroups[i] == null ? null : agentGroups[i]
					.getAgentGroup();
		return res;
	}

	/**
	 * Returns an array containing the waiting queues of this model.
	 */
	public WaitingQueue[] getWaitingQueues() {
		return queues.clone();
	}

	/**
	 * Returns the waiting queue with index \texttt{q} in this model.
	 * 
	 * @param q
	 *           the index of the waiting queue.
	 * @return a reference to the waiting queue.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{q} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumWaitingQueues()}.
	 */
	public WaitingQueue getWaitingQueue(int q) {
		return queues[q];
	}

	/**
	 * Returns the array of call factories for this model.
	 */
	public CallFactory[] getCallFactories() {
		return factories.clone();
	}

	/**
	 * Returns the call factory generating calls of type \texttt{k} in this
	 * model.
	 * 
	 * @param k
	 *           the index of the call type.
	 * @return a reference to the call factory.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to the
	 *               value returned by {@link #getNumContactTypes()}.
	 */
	public CallFactory getCallFactory(int k) {
		return factories[k];
	}

	/**
	 * Returns an array containing information objects for all user-defined
	 * segments regrouping inbound contact types.
	 */
	public SegmentInfo[] getInContactTypeSegments() {
		return inboundTypeSegments.clone();
	}

	/**
	 * Returns the information object for the \texttt{k}th user-defined segment
	 * regrouping inbound contact types.
	 * 
	 * @param k
	 *           the index of the user-defined segment.
	 * @return the segment information object.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to
	 *               {@link #getNumInContactTypeSegments()}.
	 */
	public SegmentInfo getInContactTypeSegment(int k) {
		return inboundTypeSegments[k];
	}

	public int getNumInContactTypeSegments() {
		return inboundTypeSegments.length;
	}

	/**
	 * Returns an array containing information objects for all user-defined
	 * segments regrouping outbound contact types.
	 */
	public SegmentInfo[] getOutContactTypeSegments() {
		return outboundTypeSegments.clone();
	}

	public int getNumOutContactTypeSegments() {
		return outboundTypeSegments.length;
	}

	/**
	 * Returns the information object for the \texttt{k}th user-defined segment
	 * regrouping outbound contact types.
	 * 
	 * @param k
	 *           the index of the user-defined segment.
	 * @return the segment information object.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to
	 *               {@link #getNumOutContactTypeSegments()}.
	 */
	public SegmentInfo getOutContactTypeSegment(int k) {
		return outboundTypeSegments[k];
	}

	/**
	 * Returns an array containing information objects for all user-defined
	 * segments regrouping contact types.
	 */
	public SegmentInfo[] getContactTypeSegments() {
		return callTypeSegments.clone();
	}

	public int getNumContactTypeSegments() {
		return callTypeSegments.length;
	}

	/**
	 * Returns the information object for the \texttt{k}th user-defined segment
	 * regrouping contact types.
	 * 
	 * @param k
	 *           the index of the user-defined segment.
	 * @return the segment information object.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{k} is negative, or greater than or equal to
	 *               {@link #getNumContactTypeSegments()}.
	 */
	public SegmentInfo getContactTypeSegment(int k) {
		return callTypeSegments[k];
	}

	/**
	 * Returns an array containing information objects for all user-defined
	 * segments regrouping agent groups.
	 */
	public SegmentInfo[] getAgentGroupSegments() {
		return agentGroupSegments.clone();
	}

	public int getNumAgentGroupSegments() {
		return agentGroupSegments.length;
	}

	/**
	 * Returns the information object for the \texttt{i}th user-defined segment
	 * regrouping agent groups.
	 * 
	 * @param i
	 *           the index of the user-defined segment.
	 * @return the segment information object.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{i} is negative, or greater than or equal to
	 *               {@link #getNumAgentGroups()}.
	 */
	public SegmentInfo getAgentGroupSegment(int i) {
		return agentGroupSegments[i];
	}

	/**
	 * Returns an array containing information objects for all user-defined
	 * segments regrouping main periods.
	 */
	public SegmentInfo[] getMainPeriodSegments() {
		return periodSegments.clone();
	}

	public int getNumMainPeriodSegments() {
		return periodSegments.length;
	}

	/**
	 * Returns the information object for the \texttt{p}th user-defined segment
	 * regrouping main periods.
	 * 
	 * @param p
	 *           the index of the user-defined segment.
	 * @return the segment information object.
	 * @exception ArrayIndexOutOfBoundsException
	 *               if \texttt{p} is negative, or greater than or equal to
	 *               {@link #getNumMainPeriods()}.
	 */
	public SegmentInfo getMainPeriodSegment(int p) {
		return periodSegments[p];
	}

	/**
	 * Returns the global multiplier applied to the arrival rates or number of
	 * arrivals for each arrival process in this model.
	 * 
	 * @return the global multiplier for arrivals.
	 */
	public double getArrivalsMult() {
		return arrivalsMult;
	}

	/**
	 * Returns the global multiplier for patience times which is applied on every
	 * generated patience time.
	 * 
	 * @return the global multiplier for patience times.
	 */
	public double getPatienceTimesMult() {
		return patienceTimesMult;
	}

	/**
	 * Returns the global multiplier for service times which is applied on every
	 * generated service time.
	 * 
	 * @return the global multiplier for service times.
	 */
	public double getServiceTimesMult() {
		return serviceTimesMult;
	}

	/**
	 * Returns the global multiplier for conference times of calls transferred to
	 * a new agent with the primary agent waiting for the secondary agent.
	 * 
	 * @return the global multiplier for conference times.
	 */
	public double getConferenceTimesMult() {
		return conferenceTimesMult;
	}

	/**
	 * Returns the global multiplier for pre-service times of calls transferred
	 * to a new agent without the primary agent waiting for the secondary agent.
	 * 
	 * @return the global multiplier for pre-service times.
	 */
	public double getPreServiceTimesNoConfMult() {
		return preServiceTimesNoConfMult;
	}

	/**
	 * Returns the global multiplier applied on any generated transfer time.
	 * 
	 * @return the global multiplier for transfer times.
	 */
	public double getTransferTimesMult() {
		return transferTimesMult;
	}

	/**
	 * Returns the global multiplier applied to all generated preview times of
	 * outbound calls.
	 * 
	 * @return the global multiplier for preview times.
	 */
	public double getPreviewTimesMult() {
		return previewTimesMult;
	}

	/**
	 * Returns the global multiplier for the number of agents in any group during
	 * any period.
	 * 
	 * @return the global multiplier for staffing.
	 */
	public double getAgentsMult() {
		return agentsMult;
	}

	/**
	 * Sets the global multiplier for arrivals to \texttt{arrivalsMult}. This
	 * multiplier takes effect only after the next call to {@link #initSim()}.
	 * 
	 * @param arrivalsMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setArrivalsMult(double arrivalsMult) {
		if (arrivalsMult < 0)
			throw new IllegalArgumentException();
		this.arrivalsMult = arrivalsMult;
	}

	/**
	 * Sets the global multiplier for patience times to
	 * \texttt{patienceTimesMult}. This multiplier takes effect only after the
	 * next call to {@link #initSim()}.
	 * 
	 * @param patienceTimesMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setPatienceTimesMult(double patienceTimesMult) {
		if (patienceTimesMult < 0)
			throw new IllegalArgumentException();
		this.patienceTimesMult = patienceTimesMult;
	}

	/**
	 * Sets the global multiplier for service times to \texttt{serviceTimesMult}.
	 * This multiplier takes effect only after the next call to
	 * {@link #initSim()}.
	 * 
	 * @param serviceTimesMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setServiceTimesMult(double serviceTimesMult) {
		if (serviceTimesMult < 0)
			throw new IllegalArgumentException();
		this.serviceTimesMult = serviceTimesMult;
	}

	/**
	 * Sets the global multiplier for conference times to
	 * \texttt{conferenceTimesMult}. This multiplier takes effect only after the
	 * next call to {@link #initSim()}.
	 * 
	 * @param conferenceTimesMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setConferenceTimesMult(double conferenceTimesMult) {
		if (conferenceTimesMult < 0)
			throw new IllegalArgumentException();
		this.conferenceTimesMult = conferenceTimesMult;
	}

	/**
	 * Sets the global multiplier for pre-service times to
	 * \texttt{preServiceTimesNoConfMult}. This multiplier takes effect only
	 * after the next call to {@link #initSim()}.
	 * 
	 * @param preServiceTimesNoConfMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setPreServiceTimesNoConfMult(double preServiceTimesNoConfMult) {
		if (preServiceTimesNoConfMult < 0)
			throw new IllegalArgumentException();
		this.preServiceTimesNoConfMult = preServiceTimesNoConfMult;
	}

	/**
	 * Sets the global multiplier for transfer times to
	 * \texttt{transferTimesMult}. This multiplier takes effect only after the
	 * next call to {@link #initSim()}.
	 * 
	 * @param transferTimesMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setTransferTimesMult(double transferTimesMult) {
		if (transferTimesMult < 0)
			throw new IllegalArgumentException();
		this.transferTimesMult = transferTimesMult;
	}

	/**
	 * Sets the global multiplier for preview times to \texttt{previewTimesMult}.
	 * This multiplier takes effect only after the next call to
	 * {@link #initSim()}.
	 * 
	 * @param previewTimesMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setPreviewTimesMult(double previewTimesMult) {
		if (previewTimesMult < 0)
			throw new IllegalArgumentException();
		this.previewTimesMult = previewTimesMult;
	}

	/**
	 * Sets the global multiplier for the number of agents to
	 * \texttt{agentsMult}. This multiplier takes effect only after the next call
	 * to {@link #initSim()}.
	 * 
	 * @param agentsMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if the given multiplier is negative.
	 */
	public void setAgentsMult(double agentsMult) {
		if (agentsMult < 0)
			throw new IllegalArgumentException();
		this.agentsMult = agentsMult;
	}

	/**
	 * Returns the $J\times P$ default shift matrix used for any agent group with
	 * a schedule giving only a vector of agents. Here, $J$ is the number of
	 * shifts in the matrix. Element $(j, p)$ of the returned matrix is
	 * \texttt{true} if and only if agents on shift $j$ work during main period
	 * $p$.
	 * 
	 * @return the default matrix of shifts.
	 */
	public boolean[][] getDefaultShiftMatrix() {
		return defaultShiftMatrix;
	}

	/**
	 * Determines if the schedules of agent groups have been converted into
	 * staffing vectors during the construction of agent groups.
	 */
	boolean isConvertScheduleToStaffing() {
		return convertScheduleToStaffing;
	}

	/**
	 * Sets the multiplier returned by {@link #getAgentsMult()} to 1, and adjusts
	 * the multipliers for each agent group. Let $m$ be the multiplier returned
	 * by {@link #getAgentsMult()} before this method is called. This method
	 * changes the multiplier for each agent group $i$ from $m_i$ to $m*m_i$, and
	 * resets the global multiplier $m$ to 1.
	 */
	public void resetAgentsMult() {
		if (agentsMult == 1.0)
			return;
		for (AgentGroupManager grp : agentGroups)
			grp.setAgentsMult(agentsMult * grp.getAgentsMult());
		agentsMult = 1.0;
	}

	/**
	 * Returns the class of waiting time predictors used by some routing
	 * policies, and virtual holding.
	 * 
	 * @return the class of predictor for waiting times.
	 */
	public Class<? extends WaitingTimePredictor> getWaitingTimePredictorClass() {
		return predClass;
	}

	/**
	 * Returns the current queue capacity in this model. This corresponds to the
	 * maximal total number of calls in queue at any time during the simulation,.
	 * An infinite queue capacity is represented by {@link Integer#MAX_VALUE}.
	 * 
	 * @return the total queue capacity.
	 */
	public int getQueueCapacity() {
		return queueCapacity;
	}

	/**
	 * Sets the total queue capacity to \texttt{q}.
	 * 
	 * @param q
	 *           the new queue capacity.
	 * @exception IllegalArgumentException
	 *               if the given queue capacity is smaller than the current
	 *               total number of calls in queue.
	 */
	public void setQueueCapacity(int q) {
		getRouter().setTotalQueueCapacity(q);
		this.queueCapacity = q;
	}

	/**
	 * Determines if patience times for contacts of type \texttt{k} arrived
	 * during period \texttt{mp} are exponential, and returns the result of the
	 * test.
	 * 
	 * @param k
	 *           the tested contact type.
	 * @param mp
	 *           the tester arrival period.
	 * @return \texttt{true} if and only if patience times are exponential.
	 */
	public boolean isExponentialPatienceTime(int k, int mp) {
		return expPatienceTimes[k][mp];
	}

	/**
	 * Returns an array containing \texttt{true} at position \texttt{[k][p]} if
	 * contacts of type \texttt{k} arrived during period \texttt{p} have
	 * exponential patience times.
	 * 
	 * @return the status of patience times for all contact types and periods.
	 */
	public boolean[][] isExponentialPatienceTime() {
		return expPatienceTimes;
	}

	/**
	 * Determines if service times for contacts of type \texttt{k} arrived during
	 * period \texttt{mp}, and served by agents in group \texttt{i} are
	 * exponential, and returns the result of the test.
	 * 
	 * @param k
	 *           the tested contact type.
	 * @param i
	 *           the tested agent group.
	 * @param mp
	 *           the tested arrival period.
	 * @return \texttt{true} if and only if service times are exponential.
	 */
	public boolean isExponentialServiceTime(int k, int i, int mp) {
		return expServiceTimes[k][i][mp];
	}

	/**
	 * Returns an array containing \texttt{true} at position \texttt{[k][i][p]}
	 * if contacts of type \texttt{k} arrived during period \texttt{p}, and
	 * served by agents in group \texttt{i} have exponential service times.
	 * 
	 * @return the status of service times for all contact types and periods.
	 */
	public boolean[][][] isExponentialServiceTime() {
		return expServiceTimes;
	}

	/**
	 * Returns a map containing the user-defined properties associated with this
	 * model.
	 * 
	 * @return the map of user-defined properties.
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
}
