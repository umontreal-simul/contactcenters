package umontreal.iro.lecuyer.contactcenters.msk.model;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.app.ArrivalProcessType;
import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.DirichletArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.DirichletCompoundArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.FixedCountsArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.NORTADrivenArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PiecewiseConstantPoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcessWithThinning;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonArrivalProcessWithTimeIntervals;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaNortaRatesArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonGammaPowArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.contact.PoissonUniformArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.msk.ParameterEstimator;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CorrelationFit;
import umontreal.iro.lecuyer.contactcenters.msk.params.ProducedCallTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.spi.ArrivalProcessFactory;
import umontreal.ssj.functionfit.SmoothingCubicSpline;
import umontreal.ssj.functions.MathFunction;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;

/**
 * Encapsulates the parameters of an arrival process, constructs the
 * corresponding {@link ContactArrivalProcess} object, and updates its state
 * during simulation.
 */
public class ArrivalProcessManager extends CallSourceManager {
	private static final List<ArrivalProcessFactory> arrivalProcessFactories = new ArrayList<ArrivalProcessFactory>();

	private static final ServiceLoader<ArrivalProcessFactory> arrivalProcessFactoryLoader = ServiceLoader
			.load(ArrivalProcessFactory.class);

	private CallCenter cc;
	private ContactArrivalProcess arrivProc;
	private double arrivalsMult;

	/**
	 * This field is initialized by
	 * {@link #estimateParameters(CallCenterParams,ArrivalProcessParams,int,double)} when the
	 * distribution of a busyness factor is estimated in addition to parameters
	 * of arrival process. In such a case, the field is initialized with the
	 * parameters of a random variate generator for the busyness factor.
	 * Otherwise, this field is \texttt{null}.
	 */
	public static RandomVariateGenParams s_bgenParams;

	/**
	 * Constructs a new arrival process manager for the call center model
	 * \texttt{cc}, the parameters \texttt{par}, and with index \texttt{k}. If
	 * \texttt{k} is smaller than the number of inbound call types defined by the
	 * model, this creates an arrival process producing calls of the single type
	 * \texttt{k}. Otherwise, the arrival process can produce calls of multiple
	 * types.
	 * 
	 * @param cc
	 *           the call center model.
	 * @param par
	 *           the parameters of the arrival process.
	 * @param k
	 *           the index of the arrival process.
	 * @throws ArrivalProcessCreationException
	 *            if an error occurs during the creation of the arrival process.
	 */
	public ArrivalProcessManager(CallCenter cc, ArrivalProcessParams par, int k)
			throws ArrivalProcessCreationException {
		super(cc, par);
		this.cc = cc;
		final ContactFactory factory;
		if (k < cc.getNumInContactTypes()) {
			if (cc.getCallFactory(k).isDisableCallSource())
				throw new ArrivalProcessCreationException("Calls of type " + k
						+ " cannot be generated by an arrival process");
			factory = cc.getCallFactory(k);
		} else {
			final RandomStream stream = cc.getRandomStreams()
					.getArrivalProcessPStream(k - cc.getNumInContactTypes());
			try {
				CallFactory.checkInbound(cc.getNumInContactTypes(),
						par.getProducedCallTypes());
			} catch (final IllegalArgumentException iae) {
				throw new ArrivalProcessCreationException(
						"Cannot create multiple-call types arrival process", iae);
			}
			for (final ProducedCallTypeParams pct : par.getProducedCallTypes())
				if (cc.getCallFactory(pct.getType()).isDisableCallSource())
					throw new ArrivalProcessCreationException("Calls of type "
							+ pct.getType()
							+ " cannot be generated by an arrival process");
			try {
				factory = CallFactory.createRandomTypeContactFactory(cc,
						par.getProducedCallTypes(), stream,
						par.isCheckAgentsForCall());
			} catch (final CallFactoryCreationException cfe) {
				throw new ArrivalProcessCreationException(
						"Cannot create multiple-types call factory", cfe);
			}
		}

		if (par.isSetData()) {
			final CallCenterParams ccParams = cc.getCallCenterParams();
			s_bgenParams = null;
			try {
				estimateParameters(ccParams, par, cc.getNumMainPeriods(),
						cc.getPeriodDuration());

			} catch (final IllegalArgumentException iae) {
				throw new ArrivalProcessCreationException(
						"Error estimating parameters", iae);
			}
			if (s_bgenParams != null) {
				if (ccParams.isSetBusynessGen())
					throw new ArrivalProcessCreationException(
							"Cannot estimate or specify the busyness factor more than once");
				ccParams.setBusynessGen(s_bgenParams);
				s_bgenParams = null;
			}
		}
		arrivProc = createArrivalProcess(par, k, factory);
		arrivProc.addNewContactListener(cc.getRouter());
		arrivalsMult = par.getArrivalsMult();

		final double mult = arrivalsMult * cc.getArrivalsMult();
		arrivProc.setExpectedBusynessFactor(mult);
		// CallCenterParams ccParams = cc.getCallCenterParams ();
		// arrivProc.setBusynessGen (ccParams.getBusynessGen());
		PiecewiseConstantPoissonArrivalProcess.setVarianceEpsilon(par
				.getVarianceEpsilon());
		PoissonGammaArrivalProcess.setNumMC(par.getNumMonteCarlo());
	}

	/**
	 * Returns the value of the multiplier for the arrival rates.
	 * 
	 * @return the multiplier for the arrival rates.
	 */
	public double getArrivalsMult() {
		return arrivalsMult;
	}

	/**
	 * Sets the multiplier for arrival rates to \texttt{arrivalsMult}.
	 * 
	 * @param arrivalsMult
	 *           the new multiplier.
	 * @exception IllegalArgumentException
	 *               if \texttt{arrivalsMult} is negative.
	 */
	public void setArrivalsMult(double arrivalsMult) {
		if (arrivalsMult < 0)
			throw new IllegalArgumentException();
		this.arrivalsMult = arrivalsMult;
	}

	/**
	 * Returns the associated arrival process.
	 * 
	 * @return the associated arrival process.
	 */
	public ContactArrivalProcess getArrivalProcess() {
		return arrivProc;
	}

	/**
	 * Constructs and returns the arrival process to be managed. This method uses
	 * {@link ArrivalProcessParams#getType()} to get a type identifier for the
	 * arrival process. It then retrieves parameters and initializes an arrival
	 * process specific to the given type. If the name of the arrival process
	 * corresponds to a constant in {@link ArrivalProcessType}, the method
	 * handles its construction directly. Otherwise, it queries every factory
	 * registered using {@link #addArrivalProcessFactory(ArrivalProcessFactory)}
	 * until it finds one capable of creating the arrival process. If no such
	 * factory can create the process, it uses the {@link ServiceLoader} class to
	 * find an arrival process factory. If that last step fails, an
	 * arrival-process creation exception is thrown.
	 * 
	 * @param par
	 *           the parameters of the arrival process.
	 * @param k
	 *           the call type identifier.
	 * @param factory
	 *           the call factory that will be attached to the new process.
	 * @return the constructed arrival process.
	 * @throws ArrivalProcessCreationException
	 *            if an error occurs during the construction of the arrival
	 *            process.
	 */
	protected ContactArrivalProcess createArrivalProcess(
			ArrivalProcessParams par, int k, ContactFactory factory)
			throws ArrivalProcessCreationException {
		PiecewiseConstantPoissonArrivalProcess.setVarianceEpsilon(par
				.getVarianceEpsilon());
		ContactArrivalProcess cap = null;
		ArrivalProcessType arrivalProcess;
		try {
			arrivalProcess = ArrivalProcessType.valueOf(par.getType());
		} catch (IllegalArgumentException iae) {
			arrivalProcess = null;
		}
		if (arrivalProcess != null) {
			final PeriodChangeEvent pce = cc.getPeriodChangeEvent();
			final RandomStream iStream = cc.getRandomStreams()
					.getArrivalProcessStream(k,
							ArrivalProcessStreamType.INTERARRIVAL);
			final RandomStream gStream = cc.getRandomStreams()
					.getArrivalProcessStream(k, ArrivalProcessStreamType.RATES);
			final double[] arrivals = par.getArrivals();

			switch (arrivalProcess) {

			case POISSON:
				final int mp = pce.getCurrentMainPeriod();
				double lambda;
				if (arrivals.length == 0)
					throw new ArrivalProcessCreationException(
							"Arrival rates must be specified using the arrivals attribute");
				if (arrivals.length == 1)
					lambda = arrivals[0];
				else if (arrivals.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"An arrival rate is required for each main period");
				else
					lambda = arrivals[mp];
				if (par.isNormalize())
					lambda /= cc.getPeriodDuration();
				final PoissonArrivalProcess pap = new PoissonArrivalProcess(
						pce.simulator(), factory, lambda, iStream);
				cap = pap;
				break;

			case PIECEWISECONSTANTPOISSON:
				if (arrivals.length != 1
						&& arrivals.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"An arrival rate is required for each main period");
				final double[] arv = new double[pce.getNumPeriods()];
				if (arrivals.length == 1) {
					for (int p = 1; p <= pce.getNumMainPeriods(); p++)
						arv[p] = arrivals[0];
				} else
					System.arraycopy(arrivals, 0, arv, 1,
							Math.min(arrivals.length, pce.getNumMainPeriods()));
				arv[0] = arv[1];
				PiecewiseConstantPoissonArrivalProcess pcpp = null;
				if (par.isSetBusyGen()) {
					try {
						RandomVariateGen bgen = createBusyGen (par, gStream);
						pcpp = new PiecewiseConstantPoissonArrivalProcess(pce, factory, arv,
					   		     iStream, bgen);
					} catch (final DistributionCreationException dce) {
						throw new ArrivalProcessCreationException(
								"Cannot create the distribution for the busyness generator ", dce);
					} catch (final GeneratorCreationException gce) {
						throw new ArrivalProcessCreationException(
								"Cannot create the generator for the busyness generator", gce);
					}
				} else
				   pcpp = new PiecewiseConstantPoissonArrivalProcess(pce, factory, arv, iStream);
				pcpp.setNormalizing(par.isNormalize());
				cap = pcpp;
				break;

			case UNIFORM:
				if (arrivals.length != 1
						&& arrivals.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"An arrival rate is required for each main period");
				final int[] arv2 = new int[pce.getNumPeriods()];
				for (int pe = 1; pe <= pce.getNumMainPeriods(); pe++) {
					final double a = arrivals.length == 1 ? arrivals[0]
							: arrivals[pe - 1];
					arv2[pe] = (int) Math.round(a);
				}
				cap = new PoissonUniformArrivalProcess(pce, factory, arv2, iStream);
				break;

			case FIXEDCOUNTS:
				if (arrivals.length != 1
						&& arrivals.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"An arrival count is required for each main period");
				final int[] counts = new int[pce.getNumPeriods()];
				for (int pe = 1; pe <= pce.getNumMainPeriods(); pe++) {
					final double a = arrivals.length == 1 ? arrivals[0]
							: arrivals[pe - 1];
					counts[pe] = (int) Math.round(a);
				}
				cap = new FixedCountsArrivalProcess(pce, factory, counts, iStream);
				break;

			case POISSONGAMMA:
				final double[] galphasIn = par.getPoissonGammaShape();
				final double[] glambdasIn = par.getPoissonGammaRate();
				final double[] galphas;
				final double[] glambdas;
                                final double[] gpowerIn = par.getDailyGammaPower();
                                final double[] gpower;
                                
                                
				if (galphasIn.length == 0 || glambdasIn.length == 0)
					throw new ArrivalProcessCreationException(
							"Missing parameters for the Poisson-gamma arrival process");
				if (galphasIn.length == 1) {
					galphas = new double[pce.getNumPeriods()];
					Arrays.fill(galphas, galphasIn[0]);
					galphas[galphas.length - 1] = 0;
				} else if (galphasIn.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"A shape parameter is needed for each main period for the Poisson-gamma arrival process");
				else {
					galphas = new double[pce.getNumPeriods()];
					System.arraycopy(galphasIn, 0, galphas, 1,
							Math.min(galphasIn.length, pce.getNumMainPeriods()));
					galphas[0] = galphas[1];
				}

				if (glambdasIn.length == 1) {
					glambdas = new double[pce.getNumPeriods()];
					Arrays.fill(glambdas, glambdasIn[0]);
					glambdas[glambdas.length - 1] = 0;
				} else if (glambdasIn.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"A rate parameter is needed for each main period for the Poisson-gamma arrival process");
				else {
					glambdas = new double[pce.getNumPeriods()];
					System.arraycopy(glambdasIn, 0, glambdas, 1,
							Math.min(glambdasIn.length, pce.getNumMainPeriods()));
					glambdas[0] = glambdas[1];
				}
                                
                                if (gpowerIn.length == 0) {
                                    gpower = null; // do nothing
                                }
                                else if (gpowerIn.length == 1) {
					gpower = new double[pce.getNumPeriods()];
					Arrays.fill(gpower, gpowerIn[0]);
					gpower[gpower.length - 1] = 0;
				} else if (gpowerIn.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
						"A daily gamma power parameter is needed for each main period for the Poisson-gamma arrival process");
				else {
					gpower = new double[pce.getNumPeriods()];
					System.arraycopy(gpowerIn, 0, gpower, 1,
							Math.min(gpowerIn.length, pce.getNumMainPeriods()));
					gpower[0] = gpower[1];
				}

                                if (gpowerIn.length > 0 && par.isSetBusyGen() == false)
                                    throw new ArrivalProcessCreationException(
                                            "The parameter busyGen must be given in order to use"
                                                    + " the parameter dailyGammaPower.");
                                
				PoissonGammaArrivalProcess pg;
				if (par.isSetBusyGen()) {
					try {
						RandomVariateGen bgen = createBusyGen (par, gStream);
                                           if (gpower == null)
                                               pg = new PoissonGammaArrivalProcess(
								  pce, factory, galphas, glambdas, iStream, gStream, bgen);
                                           else
                                               pg = new PoissonGammaPowArrivalProcess(
								  pce, factory, galphas, glambdas, gpower, iStream, gStream, bgen);
					} catch (final DistributionCreationException dce) {
						throw new ArrivalProcessCreationException(
								"Cannot create the distribution for the busyness generator ", dce);
					} catch (final GeneratorCreationException gce) {
						throw new ArrivalProcessCreationException(
								"Cannot create the generator for the busyness generator", gce);
					}
				} else {
					 pg = new PoissonGammaArrivalProcess(
						  pce, factory, galphas, glambdas, iStream, gStream);
				}
				pg.setNormalizing(par.isNormalize());
				cap = pg;
				break;

			case POISSONGAMMANORTARATES:
				final double[] galphasIn1 = par.getPoissonGammaShape();
				final double[] glambdasIn1 = par.getPoissonGammaRate();
				final double[] galphas1;
				final double[] glambdas1;
				double[][] extendedCorrMtx;
				final double[][] corrMtx = ArrayConverter.unmarshalArray(par
						.getCopulaSigma());

				if (galphasIn1.length == 0 || glambdasIn1.length == 0)
					throw new ArrivalProcessCreationException(
							"Missing parameters for the Poisson-gamma arrival process");
				if (galphasIn1.length == 1) {
					galphas1 = new double[pce.getNumPeriods()];
					Arrays.fill(galphas1, galphasIn1[0]);
					galphas1[galphas1.length - 1] = 0;
				} else if (galphasIn1.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"A shape parameter is needed for each main period for the Poisson-gamma arrival process");
				else {
					galphas1 = new double[pce.getNumPeriods()];
					System.arraycopy(galphasIn1, 0, galphas1, 1,
							Math.min(galphasIn1.length, pce.getNumMainPeriods()));
					galphas1[0] = galphas1[1];
				}

				if (glambdasIn1.length == 1) {
					glambdas1 = new double[pce.getNumPeriods()];
					Arrays.fill(glambdas1, glambdasIn1[0]);
					glambdas1[glambdas1.length - 1] = 0;
				} else if (glambdasIn1.length < pce.getNumMainPeriods())
					throw new ArrivalProcessCreationException(
							"A rate parameter is needed for each main period for the Poisson-gamma arrival process");
				else {
					glambdas1 = new double[pce.getNumPeriods()];
					System.arraycopy(glambdasIn1, 0, glambdas1, 1,
							Math.min(glambdasIn1.length, pce.getNumMainPeriods()));
					glambdas1[0] = glambdas1[1];
				}

				if (corrMtx[0].length != glambdasIn1.length
						|| corrMtx.length != glambdasIn1.length) {
					throw new IllegalArgumentException(
							"The dimensions of the correlation matrix for the Gamma-Poisson NORTA rates arrival process do not match the length of the Gamma shape parameter vector");
				}
				extendedCorrMtx = new double[galphas1.length][galphas1.length];
				extendedCorrMtx[0][0] = 1.0;
				extendedCorrMtx[galphas1.length - 1][galphas1.length - 1] = 1.0;
				for (int i = 0; i < glambdasIn1.length; i++) {
					System.arraycopy(corrMtx[i], 0, extendedCorrMtx[i + 1], 1,
							glambdasIn1.length);
				}

				try {
					final PoissonGammaNortaRatesArrivalProcess pgnr = new PoissonGammaNortaRatesArrivalProcess(
							pce, factory, galphas1, glambdas1, extendedCorrMtx,
							iStream, gStream);
					pgnr.setNormalizing(par.isNormalize());
					cap = pgnr;
				} catch (final IllegalArgumentException iae) {
					throw new ArrivalProcessCreationException(
							"Cannot create the Poisson-Gamma NORTA rates arrival process",
							iae);
				}
				break;

			case DIRICHLETCOMPOUND:
				if (arrivals.length < pce.getNumPeriods() - 1)
					throw new ArrivalProcessCreationException(
							"At least P+1 Dirichlet parameters are required for the Dirichlet-compound arrival process");
				final DirichletCompoundArrivalProcess dc = new DirichletCompoundArrivalProcess(
						pce, factory, arrivals, iStream, gStream);
				dc.setNormalizing(par.isNormalize());
				cap = dc;
				break;

			case DIRICHLET:
				if (arrivals.length < pce.getNumPeriods() - 2)
					throw new ArrivalProcessCreationException(
							"At least P Dirichlet parameters are required for the Dirichlet arrival process");
				try {
					final DirichletArrivalProcess dc2 = new DirichletArrivalProcess(
							pce, factory, arrivals, gStream,
							createArvGen(par, gStream));
					cap = dc2;
				} catch (final DistributionCreationException dce) {
					throw new ArrivalProcessCreationException(
							"Cannot create the distribution for the number of arrivals of type "
									+ k, dce);
				} catch (final GeneratorCreationException gce) {
					throw new ArrivalProcessCreationException(
							"Cannot create the generator for the number of arrivals of type "
									+ k, gce);
				}
				break;

			case NORTADRIVEN:
				try {
					final NORTADrivenArrivalProcess nd = new NORTADrivenArrivalProcess(
							pce, factory, new DenseDoubleMatrix2D(
									ArrayConverter.unmarshalArray(par.getNortaSigma())),
							par.getNortaGamma(), par.getNortaP(), gStream);
					cap = nd;
				} catch (final IllegalArgumentException iae) {
					throw new ArrivalProcessCreationException(
							"Cannot create the NORTA-driven arrival process", iae);
				}
				break;

			case PIECEWISECONSTANTPOISSONINT:
				final double[] times = cc.getTime(par.getTimes());
				final double[] lambdas = par.getLambdas();
				try {
					PoissonArrivalProcessWithTimeIntervals ap = new PoissonArrivalProcessWithTimeIntervals(
							pce.simulator(), factory, times, lambdas, iStream);
					ap.setNormalizing(par.isNormalize());
					cap = ap;
				} catch (final IllegalArgumentException iae) {
					throw new ArrivalProcessCreationException(
							"Cannot create the arrival process", iae);
				}
				break;

			case CUBICSPLINE:
				final MathFunction spline;
				final double[] x = cc.getTime(par.getTimes());
				final double[] y = par.getLambdas();
				if (x.length != y.length)
					throw new ArrivalProcessCreationException(
							"A value lambda(t) is required for each given time t to create an arrival process using a cubic spline");
				try {
					double eps = par.getSplineSmoothingFactor();
					spline = new SmoothingCubicSpline(x, y, eps);
				} catch (final IllegalArgumentException iae) {
					throw new ArrivalProcessCreationException(
							"Error creating the cubic spline for the arrival rates",
							iae);
				}
				double maxTime = x[0];
				double maxLambda = y[0];
				for (int i = 1; i < x.length; i++) {
					if (x[i] > maxTime)
						maxTime = x[i];
					if (y[i] > maxLambda)
						maxLambda = y[i];
				}
				cap = new PoissonArrivalProcessWithThinning(pce.simulator(),
						factory, iStream, gStream, spline, maxLambda, maxTime);
				break;

			default:
				throw new ArrivalProcessCreationException(
						"Unsupported arrival process: " + arrivalProcess);
			}
		} else {
			for (final ArrivalProcessFactory apf : arrivalProcessFactories) {
				cap = apf.createArrivalProcess(cc, this, par);
				if (cap != null)
					break;
			}
			if (cap != null)
				for (final ArrivalProcessFactory apf : arrivalProcessFactoryLoader) {
					cap = apf.createArrivalProcess(cc, this, par);
					if (cap != null)
						break;
				}
			if (cap == null)
				throw new ArrivalProcessCreationException(
						"Unsupported arrival process: " + arrivalProcess);
		}

		final String typn = par.getName();
		if (typn != null && typn.length() > 0)
			cap.setName(typn);
		else
			cap.setName("");
		return cap;
	}

	/**
	 * Registers the arrival process factory \texttt{apf} for arrival process
	 * managers. If the user-specified type of arrival process does not
	 * correspond to a predefined process, the registered factories are queried
	 * to find one capable of creating an arrival process. This method must be
	 * called before the call-center simulator is initialized.
	 * 
	 * @param apf
	 *           the new arrival process factory to register.
	 */
	public static void addArrivalProcessFactory(ArrivalProcessFactory apf) {
		if (apf == null)
			throw new NullPointerException();
		if (!arrivalProcessFactories.contains(apf))
			arrivalProcessFactories.add(apf);
	}

	private RandomVariateGen createArvGen(ArrivalProcessParams par,
			RandomStream stream) throws DistributionCreationException,
			GeneratorCreationException {
		if (par.getArvGen() == null)
			return null;
		return ParamReadHelper.createGenerator(par.getArvGen(), stream);
	}

	/*
	 * Create the busyness generator for this arrival process
	 */
	private RandomVariateGen createBusyGen(ArrivalProcessParams par,
			RandomStream stream) throws DistributionCreationException,
	      GeneratorCreationException {
		if (null == par.getBusyGen())
			return null;
		return ParamReadHelper.createGenerator(par.getBusyGen(), stream);
   }
	
	/**
	 * Create the gamma generator params for busyness factor
	 * @return
	 */
	private static RandomVariateGenParams createGenParams(double alpha0)  {
		double BIG = 1.0 / PiecewiseConstantPoissonArrivalProcess
				.getVarianceEpsilon();
		RandomVariateGenParams bgenParams;
		if (alpha0 < BIG) {
			bgenParams = new RandomVariateGenParams();
			bgenParams.setDistributionClass(GammaDist.class.getSimpleName());
			bgenParams.setParams(new double[] { alpha0, alpha0 });
		} else {
			bgenParams = null;
		}
		return bgenParams;
	}
	
	/**
	 * Estimates the parameters of the arrival process described by \texttt{par},
	 * for a call center with \texttt{numPeriods} main periods with duration
	 * \texttt{periodDuration}. The method replaces the data stored in
	 * \texttt{par} with estimated parameters using an algorithm depending on the
	 * type of arrival process. If returns \texttt{true} if parameters were
	 * estimated, and \texttt{false} if there were no parameters to estimate. If
	 * parameter estimation is needed but fails, an illegal-argument exception is
	 * thrown.
	 * 
	 * More specifically, the method returns \texttt{false} if no data is stored
	 * in the given parameter object. If the type of arrival process corresponds
	 * to a constant in {@link ArrivalProcessType}, parameter estimation is
	 * handled directly by this method. Otherwise, the method queries every
	 * arrival process factory registered using
	 * {@link #addArrivalProcessFactory(ArrivalProcessFactory)} until it finds a
	 * factory capable of performing the estimation. If no such factory exists,
	 * it uses the {@link ServiceLoader} class to find a factory dynamically. If
	 * this last step fails, the method throws an illegal-argument exception.
	 * 
	 * @param par
	 *           the parameters of the arrival process.
	 * @param numPeriods
	 *           the number of main periods.
	 * @param periodDuration
	 *           the duration of main periods.
	 * @return a boolean indicating if parameter estimation was performed.
	 */
	public static boolean estimateParameters(CallCenterParams ccParams,
			ArrivalProcessParams par, int numPeriods, double periodDuration) {
		if (par == null)
			return false;
		if (!par.isSetData())
			return false;
		ArrivalProcessType arrivalProcess;
		try {
			arrivalProcess = ArrivalProcessType.valueOf(par.getType());
		} catch (IllegalArgumentException iae) {
			arrivalProcess = null;
		}
		if (arrivalProcess == null) {
	      ArrivalProcessParams defArr = ccParams.getDefaultArrivalProcess();
     	   String name = defArr.getType();
      	if (null != name) {
            arrivalProcess = ArrivalProcessType.valueOf(name);
      		ParameterEstimator.setParamsFromDefault(par, defArr);
      	} else
      		throw new IllegalArgumentException(
				"   No arrival process has been chosen");
		}
		final int[][] data = ArrayConverter.unmarshalArray(par.getData());
		if (data == null)
			return false;
		ArrayUtil.checkRectangularMatrix(data);
		final int n = data.length;
		if (n == 0)
			return false;
		final int d = data[0].length;
		double varEps = par.getVarianceEpsilon();
		PiecewiseConstantPoissonArrivalProcess.setVarianceEpsilon(varEps);
		PoissonGammaArrivalProcess.setNumMC(par.getNumMonteCarlo());
		RandomVariateGenParams bgenParams = null;


		if (arrivalProcess != null) {
			boolean failed = false;
			double[] arrivals;
			double[] gammaParams;
			double[] shapeParams;
			double[] rateParams;

			switch (arrivalProcess) {
			case POISSON:
				if (par.isEstimateBusyness())
					throw new IllegalArgumentException(
							"Cannot estimate the busyness factor for Poisson");
				arrivals = PoissonArrivalProcess.getMLE(data, n, d);
				par.setNormalize(false);
				arrivals[0] /= numPeriods * periodDuration;
				par.setArrivals(arrivals);
				break;

			case PIECEWISECONSTANTPOISSON:
				if (par.isEstimateBusyness()) {
					arrivals = PiecewiseConstantPoissonArrivalProcess
							.getMLENegMulti(data, n, d);
					final double alpha0 = PiecewiseConstantPoissonArrivalProcess.s_bgammaParam;
					bgenParams = createGenParams(alpha0);
					par.unsetEstimateBusyness();
					par.setBusyGen (bgenParams);
					s_bgenParams = null;
				} else
					arrivals = PiecewiseConstantPoissonArrivalProcess.getMLE(data,
							n, d);
				par.setArrivals(arrivals);
				par.setNormalize(true);
				break;

			case UNIFORM:
				arrivals = PiecewiseConstantPoissonArrivalProcess
						.getMLE(data, n, d);
				par.setArrivals(arrivals);
				par.unsetNormalize();
				break;

			case POISSONGAMMA:
				if (par.isEstimateBusyness()) {
					int nmc = par.getNumMonteCarlo();
					gammaParams = PoissonGammaArrivalProcess.getMLEBB(data, n, d, nmc, par);
					final double alpha0 = PiecewiseConstantPoissonArrivalProcess.s_bgammaParam;
					bgenParams = createGenParams(alpha0);
					par.setBusyGen (bgenParams);
					s_bgenParams = null;
					par.unsetEstimateBusyness();
		   	   par.setGammaShapeEstimatorType(null);
		   	   par.unsetGammaShapeSmoothingFactor();
		   	   par.unsetMaxIter();
		   	   par.unsetMovingWindowSize();

				} else {
					gammaParams = PoissonGammaArrivalProcess.getMLE(data, n, d);
				}
				shapeParams = new double[d]; // d = numPeriods
				rateParams = new double[d];
				for (int p = 0; p < shapeParams.length; p++) {
					shapeParams[p] = gammaParams[2 * p];
					rateParams[p] = gammaParams[2 * p + 1];
				}
		    	par.setPoissonGammaShape(shapeParams);
				par.setPoissonGammaRate(rateParams);
				par.setNormalize(true);
				par.unsetArrivals();
				break;

			case POISSONGAMMANORTARATES:
				int nmc = par.getNumMonteCarlo();
				CorrelationFit fit = par.getCorrelationFit();
				double[][] corr = new double[d][d];
				gammaParams = PoissonGammaNortaRatesArrivalProcess.getMLE(data, n,
						d, nmc, fit, corr);
				shapeParams = new double[d];    // d = numPeriods
				rateParams = new double[d];
				for (int p = 0; p < shapeParams.length; p++) {
					shapeParams[p] = gammaParams[2 * p];
					rateParams[p] = gammaParams[2 * p + 1];
				}
				par.setPoissonGammaShape(shapeParams);
				par.setPoissonGammaRate(rateParams);
				par.setCopulaSigma(ArrayConverter.marshalArray(corr));
				par.setNormalize(true);
				par.unsetArrivals();
				par.setCorrelationFit(null);
				break;

			case DIRICHLETCOMPOUND:
				if (!par.isEstimateBusyness())
					throw new IllegalArgumentException(
							"Must estimate the busyness factor for Dirichlet compound arrival process");
				arrivals = DirichletCompoundArrivalProcess.getMLE(data, n, d);
				par.setArrivals(arrivals);
				par.setNormalize(true);
				s_bgenParams = new RandomVariateGenParams();
				s_bgenParams.setDistributionClass(GammaDist.class.getSimpleName());
				final double alp0 = PiecewiseConstantPoissonArrivalProcess.s_bgammaParam;
				s_bgenParams.setParams(new double[] { alp0, 1 });
				break;

			case DIRICHLET:
				if (par.isEstimateBusyness())
					throw new IllegalArgumentException(
							"Cannot estimate the busyness factor for Dirichlet arrival process");
				if (par.getArvGen() == null)
					throw new IllegalArgumentException("No arvGen element");
				arrivals = DirichletArrivalProcess.getMLE(data, n, d);
				final double[] totalArrivals = new double[n];
				for (int i = 0; i < n; i++)
					for (int p = 0; p < d; p++)
						totalArrivals[i] += data[i][p];
				par.getArvGen().setParams(totalArrivals);
				par.getArvGen().setEstimateParameters(true);
				try {
					ParamReadHelper.estimateParameters(par.getArvGen());
				} catch (final DistributionCreationException dce) {
					final IllegalArgumentException iae = new IllegalArgumentException(
							"Cannot estimate parameters for arvGen");
					iae.initCause(dce);
					throw iae;
				}
				break;
			default:
				failed = true;
			}
			if (!failed) {
				par.setData(null);
				return true;
			}
		}
		for (ArrivalProcessFactory apf : arrivalProcessFactories)
			if (apf.estimateParameters(par, data, periodDuration)) {
				par.setData(null);
				return true;
			}
		for (ArrivalProcessFactory apf : arrivalProcessFactoryLoader)
			if (apf.estimateParameters(par, data, periodDuration)) {
				par.setData(null);
				return true;
			}
		throw new IllegalArgumentException(
				"Cannot estimate parameters for the specified type arrival process: "
						+ par.getType());
	}

	/**
	 * Initializes the managed arrival process by calling
	 * {@link ContactArrivalProcess#init(double)}. The busyness factor given to
	 * the arrival process is the argument \texttt{b} multiplied by the product
	 * of the multiplier returned by {@link #getArrivalsMult()}, and the global
	 * multiplier returned by {@link CallCenter#getArrivalsMult()}. The
	 * expectation $\E[B]$ is also set to these product of multipliers,
	 * multiplied by the mean value of $B$ that can be generated using the
	 * generator returned by {@link CallCenter#getBusynessGen()}.
	 * 
	 * @param b
	 *           the generated base busyness factor.
	 */
	public void init(double b) {
		double mult = arrivalsMult * cc.getArrivalsMult();
		RandomVariateGen bgen = cc.getBusynessGen();
		double bMean;
		if (bgen == null)
			bMean = 1;
		else
			bMean = bgen.getDistribution().getMean();
		arrivProc.setExpectedBusynessFactor(mult * bMean);
		arrivProc.init(b * mult);
	}
}