package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.ServiceTimes;
import umontreal.iro.lecuyer.contactcenters.contact.SingleTypeContactFactory;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ProducedCallTypeParams;

import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;

/**
 * Contact factory used to create the calls for the simulator, and to generate
 * call-specific random variates such as patience times and service times.
 * The call factory also contains any information related to
 * call types, such as name, properties, and 
 * the probability distribution for
 * patience and service times.
 */
public class CallFactory extends SingleTypeContactFactory {
   private CallCenter cc;
   private String name;
   private Map<String, Object> properties;
   private double weight;
   private double[] weightPeriod;
   private double[] probAbandon;
   private MultiPeriodGen pgen;
   private double pgenMult;
   private ServiceTimesManager sgen;
   private ServiceTimesManager confGen;
   private ServiceTimesManager psGen;
   private ServiceTimesManager transGen;
   private ContactFactory transferTargetFactory;
   private double[][] probTransfer;
   private double[][] probTransferWait;
   private RandomStream ptStream;
   private RandomStream vqStream;
   private boolean disableCallSource = false;
   private boolean excludedFromStatTotal = false;
   private int vqTarget = -1;
   private double[] vqThresh;
   private double[] probVQ;
   private double[] probVQCallBack;
   private double[] wtMult;
   private double[] ptimesMultNoVQ;
   private double[] ptimesMultCallBack;
   private double[][] stimesMultNoVQ;
   private double[][] stimesMultCallBack;
   private double[][] stimesMultTransfer;

   /**
    * Constructs a new call factory using the call center \texttt{cc}, the call
    * center parameters \texttt{ccParams}, the call-type parameters
    * \texttt{par}, and call type index \texttt{k}.
    * 
    * @param cc
    *           the call center.
    * @param ccParams
    *           the call center parameters.
    * @param par
    *           the call-type parameters.
    * @param k
    *           the call type index.
    * @throws CallFactoryCreationException
    *            if an exception occurs during the creation of the factory.
    */
   public CallFactory (CallCenter cc, CallCenterParams ccParams,
         CallTypeParams par, int k) throws CallFactoryCreationException {
      super (cc.getPeriodChangeEvent ().simulator (), k, null, cc.getRandomStreams ()
            .getCallFactoryStream (k, CallFactoryStreamType.BALKTEST), null, null,
            null, null, null);
      this.cc = cc;
      setProbBalkGenerator (new ProbAbandonGen ());
      name = par.getName ();
      properties = ParamReadHelper.unmarshalProperties (par.getProperties ());
      weight = par.getWeight ();
      weightPeriod = par.getWeightPeriod ();
      try {
         //probAbandon = CallCenterUtil.getDoubleArray (par.getProbAbandon (), cc
         //      .getNumMainPeriods ());
         probAbandon = par.getProbAbandon ();
      }
      catch (final IllegalArgumentException iae) {
         throw new CallFactoryCreationException (
               "Cannot read probabilities of balking", iae);
      }
      PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final TimeUnit defaultUnit = cc.getDefaultUnit ();
      try {
         if (par.getPatienceTime () != null) {
            final RandomStream pStream = cc.getRandomStreams ()
            .getCallFactoryStream (k, CallFactoryStreamType.PATIENCE);
            pgenMult = par.getPatienceTime ().getMult ();
            pgen = CCParamReadHelper.createGenerator (par.getPatienceTime (),
                  pStream, pce);
            pgen.setTargetTimeUnit (defaultUnit);
            setPatienceTimeGen (pgen);
         }
      }
      catch (final DistributionCreationException dce) {
         throw new CallFactoryCreationException (
               "Cannot create patience time distribution", dce);
      }
      catch (final GeneratorCreationException gce) {
         throw new CallFactoryCreationException (
               "Cannot create patience time generator", gce);
      }
      ptStream = cc.getRandomStreams ().getCallFactoryStream (k, CallFactoryStreamType.SERVICE);
      final RandomStream sStream = cc.getRandomStreams ().getCallFactoryStream2 (k,
            CallFactoryStreamType2.PROBTRANSFER);
      sgen = new ServiceTimesManager (cc, "service time", par.getServiceTimes (),
            k, sStream, par.getServiceTimesMult (), ccParams.getAgentGroups ()
            .size ());
      if (sgen.getServiceTimeGen () != null)
         setContactTimeGen (sgen.getServiceTimeGen ());
      if (sgen.getServiceTimeGenGroups () != null)
         setContactTimeGenGroups (sgen.getServiceTimeGenGroups ());

      final RandomStream tStream = cc.getRandomStreams ().getCallFactoryStream2 (k, CallFactoryStreamType2.HANDOFF);
      confGen = new ServiceTimesManager (cc, "conference time", par
            .getConferenceTimes (), k, tStream, par.getConferenceTimesMult (),
            ccParams.getAgentGroups ().size ());
      psGen = new ServiceTimesManager (cc, "pre-service time no conf", par
            .getPreServiceTimesNoConf (), k, tStream, par
            .getPreServiceTimesNoConfMult (), ccParams.getAgentGroups ()
            .size ());
      final RandomStream ttStream = cc.getRandomStreams ().getCallFactoryStream2 (k, CallFactoryStreamType2.TRANSFERTIME);
      transGen = new ServiceTimesManager (cc, "transfer time", par
            .getTransferTimes (), k, ttStream, par.getTransferTimesMult (),
            ccParams.getAgentGroups ().size ());

      if (par.isSetProbTransfer ()) {
         probTransfer = ArrayConverter.unmarshalArray (par.getProbTransfer ());
//       if (probTransfer.length > 1
//       && probTransfer.length != cc.getNumAgentGroups ())
//       throw new CallFactoryCreationException (
//       "The probTransfer matrix must have 1 or I rows");
         if (probTransfer.length > 0 && probTransfer[0].length > 1
               && probTransfer[0].length != cc.getNumMainPeriods ())
            throw new CallFactoryCreationException (
            "The probTransfer matrix must have 1 or P columns");
      }
      if (par.isSetProbTransferWait ()) {
         probTransferWait = ArrayConverter.unmarshalArray (par
               .getProbTransferWait ());
//       if (probTransferWait.length > 1
//       && probTransferWait.length != cc.getNumAgentGroups ())
//       throw new CallFactoryCreationException (
//       "The probTransferWait matrix must have 1 or I rows");
         if (probTransferWait.length > 0 && probTransferWait[0].length > 1
               && probTransferWait[0].length != cc.getNumMainPeriods ())
            throw new CallFactoryCreationException (
            "The probTransferWait matrix must have 1 or P columns");
      }
      vqStream = cc.getRandomStreams ().getCallFactoryStream2 (k, CallFactoryStreamType2.VQUEUE);
      if (par.isSetVirtualQueueTargetType ())
         vqTarget = par.getVirtualQueueTargetType ();
      if (par.isSetExpectedWaitingTimeThresh ()) {
         final Duration[] wqThreshDuration = par
         .getExpectedWaitingTimeThresh ();
         vqThresh = cc.getTime (wqThreshDuration);
      }
      if (par.isSetExpectedWaitingTimesMult ())
         wtMult = par.getExpectedWaitingTimesMult ();
      if (par.isSetProbVirtualQueue ())
         probVQ = par.getProbVirtualQueue ();
      if (par.isSetProbVirtualQueueCallBack ())
         probVQCallBack = par.getProbVirtualQueueCallBack ();
      if (par.isSetPatienceTimesMultNoVirtualQueue ())
         ptimesMultNoVQ = par.getPatienceTimesMultNoVirtualQueue ();
      if (par.isSetPatienceTimesMultCallBack ())
         ptimesMultCallBack = par.getPatienceTimesMultCallBack ();
      if (par.isSetServiceTimesMultNoVirtualQueue ()) {
         stimesMultNoVQ = ArrayConverter.unmarshalArray (par.getServiceTimesMultNoVirtualQueue ());
         if (stimesMultNoVQ.length > 0 && stimesMultNoVQ[0].length > 1
               && stimesMultNoVQ[0].length != cc.getNumMainPeriods ())
            throw new CallFactoryCreationException (
                  "The serviceTimesMultNoVirtualQueue matrix must have 1 or P columns");
      }
      if (par.isSetServiceTimesMultCallBack ()) {
         stimesMultCallBack = ArrayConverter.unmarshalArray (par.getServiceTimesMultCallBack ());
         if (stimesMultCallBack.length > 0 && stimesMultCallBack[0].length > 1
               && stimesMultCallBack[0].length != cc.getNumMainPeriods ())
            throw new CallFactoryCreationException (
            "The serviceTimesMultCallBack matrix must have 1 or P columns");
      }
      if (par.isSetServiceTimesMultTransfer ()) {
         stimesMultTransfer = ArrayConverter.unmarshalArray (par.getServiceTimesMultTransfer ());
         if (stimesMultTransfer.length > 0 && stimesMultTransfer[0].length > 1
               && stimesMultTransfer[0].length != cc.getNumMainPeriods ())
            throw new CallFactoryCreationException (
            "The serviceTimesMultTransfer matrix must have 1 or P columns");
      }
   }

   /**
    * Constructs a contact factory used to generate calls
    * resulting from transfers after service termination.
    * This initialization is not included in the constructor, because
    * all call factories must be created before the contact factory
    * for transfered calls is constructed.
    * @param ccParams the parameters of the call center.
    * @param k the identifier of the call type.
    * @throws CallFactoryCreationException if an error occurs during the
    * creation of the call factory.
    */
   public void initTransferTargets (CallCenterParams ccParams, int k)
   throws CallFactoryCreationException {
      CallTypeParams par;
      if (k < ccParams.getInboundTypes ().size ())
         par = ccParams.getInboundTypes ().get (k);
      else
         par = ccParams.getOutboundTypes ().get (
               k - ccParams.getInboundTypes ().size ());
      if (par.isSetTransferTargets ())
         try {
            transferTargetFactory = createRandomTypeContactFactory (cc, par
                  .getTransferTargets (), ptStream, par
                  .isCheckAgentsForTransfer ());
            for (final ProducedCallTypeParams pct : par.getTransferTargets ()) {
               cc.getCallFactory (pct.getType ()).setDisableCallSource (true);
               cc.getCallFactory (pct.getType ()).setExcludedFromStatTotal (
                     true);
            }
         }
      catch (final CallFactoryCreationException cfe) {
         throw new CallFactoryCreationException (
               "Could not create call factory for generating the call type index of transferred calls",
               cfe);
      }
   }

   /**
    * Returns the call center object containing this call factory.
    * @return the call center object for this factory.
    */
   public CallCenter getCallCenter () {
      return cc;
   }

   /**
    * Initializes this call factory by setting
    * the multipliers for patience and service times.
    */
   public void init () {
      if (pgen != null)
         pgen.setMult (pgenMult * cc.getPatienceTimesMult ());
      sgen.init (cc.getServiceTimesMult ());
      confGen.init (cc.getConferenceTimesMult ());
      psGen.init (cc.getPreServiceTimesNoConfMult ());
      transGen.init (cc.getTransferTimesMult ());
   }

   /**
    * Returns the current multiplier for patience times
    * for calls generated by this factory.
    * Patience times are multiplied by this constant and
    * the multiplier returned by {@link CallCenter#getPatienceTimesMult()}.
    * The default value of this multiplier is 1.
    * @return the multiplier for patience times.
    */
   public double getPatienceTimesMult () {
      return pgenMult;
   }

   /**
    * Sets the multiplier for patience times to \texttt{pgenMult}.
    * @param pgenMult the new multiplier for patience times.
    * @exception IllegalArgumentException if \texttt{pgenMult} is negative.
    */
   public void setPatienceTimesMult (double pgenMult) {
      if (pgenMult < 0)
         throw new IllegalArgumentException ();
      this.pgenMult = pgenMult;
   }

   /**
    * Returns an object managing the random variate generators
    * for regular service times.
    * @return the service times manager.
    */
   public ServiceTimesManager getServiceTimesManager () {
      return sgen;
   }

   /**
    * Returns an object managing the random variate generators
    * for conference times between primary and secondary agents.
    * @return the conference times manager.
    */
   public ServiceTimesManager getConferenceTimesManager () {
      return confGen;
   }

   /**
    * Returns an object managing the random variate generators
    * for pre-service times with secondary agents if no conference
    * with primary agents.
    * @return the pre-service times manager.
    */
   public ServiceTimesManager getPreServiceTimesNoConfManager () {
      return psGen;
   }

   /**
    * Returns an object managing the random variate generators
    * for transfer times.
    * @return the transfer times manager.
    */
   public ServiceTimesManager getTransferTimesManager () {
      return transGen;
   }

   /**
    * Constructs a new call factory using call center \texttt{cc}, call center
    * parameters \texttt{ccParams}, and call type index \texttt{k}. This returns
    * an instance of this class for inbound call types, or an instance of
    * {@link OutboundCallFactory} for outbound types.
    * This method calls {@link #init()}
    * on the constructed factory before returning it.
    * 
    * @param cc
    *           the call center model.
    * @param ccParams
    *           the call center parameters.
    * @param k
    *           the index of the call type.
    * @return the constructed call factory.
    * @throws CallFactoryCreationException
    *            if an exception occurs during the creation of the factory.
    */
   public static CallFactory create (CallCenter cc, CallCenterParams ccParams,
         int k) throws CallFactoryCreationException {
      CallFactory factory;
      if (k < ccParams.getInboundTypes ().size ())
         factory = new CallFactory (cc, ccParams, ccParams.getInboundTypes ()
               .get (k), k);
      else
         factory = new OutboundCallFactory (cc, ccParams, ccParams
               .getOutboundTypes ().get (
                     k - ccParams.getInboundTypes ().size ()), k);
      factory.init ();
      return factory;
   }

   /**
    * Returns the name of the call type associated with this call factory.
    * 
    * @return the name of the call type.
    */
   public String getName () {
      return name;
   }

   /**
    * Returns the user-defined properties of the call type
    * associated with this call factory.
    * 
    * @return the user-defined properties of the call type.
    */
   public Map<String, Object> getProperties () {
      return properties;
   }

   /**
    * Returns the default weight used when no per-period
    * weight is available for the call type associated with
    * this call factory. 
    * 
    * @return the weight of the call type.
    */
   public double getWeight () {
      return weight;
   }

   /**
    * Returns the weight of the associated call type during
    * main period \texttt{mp}, or the result of
    * {@link #getWeight()} if no per-period weight was
    * given.
    * @param mp the index of the main period.
    * @return the weight of the call type during the given main period.
    */
   public double getWeight (int mp) {
      if (weightPeriod.length == 0)
         return getWeight ();
      if (weightPeriod.length == 1)
         return weightPeriod[0];
      return weightPeriod[mp];
   }

   /**
    * Returns the probability of balking for main period \texttt{mp}.
    * 
    * @param mp
    *           the index of the main period.
    * @return the probability of balking.
    */
   public double getProbAbandon (int mp) {
      if (probAbandon.length == 0)
         return 0;
      if (probAbandon.length == 1)
         return probAbandon[0];
      return probAbandon[mp];
   }

   /**
    * Returns the probability of transfer for a call of the
    * associated type arrived during main period \texttt{mp}, and 
    * whose service finishes with a primary agent
    * in group \texttt{i}.
    * @param i the index of the agent group.
    * @param mp the index of the main period.
    * @return the probability of transfer.
    */
   public double getProbTransfer (int i, int mp) {
      return getValue (i, mp, probTransfer, 0);
   }

   public void setProbTransfer (int i, double[] prob) {
      if (probTransfer == null)
         throw new IllegalArgumentException();
      probTransfer[i] = prob;
   }

   /**
    * Returns the probability of a primary
    * agent waiting for transfer to finish, for a call of the
    * associated type arrived during main period \texttt{mp}, and 
    * whose service finishes with a primary agent
    * in group \texttt{i}.
    * @param i the index of the agent group.
    * @param mp the index of the main period.
    * @return the probability of waiting for transfer.
    */
   public double getProbTransferWait (int i, int mp) {
      return getValue (i, mp, probTransferWait, 0);
   }

   public void setProbTransferWait (int i, double[] prob) {
      if (probTransferWait == null)
         throw new IllegalArgumentException();
      probTransferWait[i] = prob;
   }

   /**
    * Returns the multiplier for service times of callers
    * arrived during main period \texttt{mp}, and served by an
    * agent in group \texttt{i} before a transfer to another
    * agent occurs.
    * @param i the index of the agent group.
    * @param mp the index of the main period.
    * @return the service times multiplier.
    */
   public double getServiceTimesMultTransfer (int i, int mp) {
      return getValue (i, mp, stimesMultTransfer, 1);
   }

   /**
    * Determines if call transfer is supported by this call factory.
    * This returns \texttt{true} if and only if
    * {@link #getProbTransfer(int,int)} returns a non-zero
    * value for at least one pair $(i,p)$, and
    * {@link #getTransferTargetFactory()} returns a non-\texttt{null}
    * value.
    */
   public boolean isCallTransferSupported () {
      return probTransfer != null && transferTargetFactory != null;
   }

   /**
    * Determines if virtual holding (or virtual queueing) is supported
    * for the associated call type.
    * This returns \texttt{true} if and only if
    * {@link #getExpectedWaitingTimeThresh(int)} returns a
    * finite value for at least one $p$,
    * {@link #getProbVirtualQueue(int)} returns a non-zero
    * value for at least one $p$, 
    * and
    * {@link #getTargetVQType()} returns a non-negative value.
    */
   public boolean isVirtualHoldSupported () {
      return vqThresh != null && probVQ != null && vqTarget >= 0;
   }

   /**
    * Returns the contact factory used to generate
    * transferred calls from calls of the associated type. 
    * @return the contact factory for transferred calls.
    */
   public ContactFactory getTransferTargetFactory () {
      return transferTargetFactory;
   }

   /**
    * Returns the multiplier for patience times for callers
    * arrived during main period \texttt{mp}, and
    * deciding not to join the virtual queue.
    * The default multiplier is 1.
    * @param mp the main period of arrival. 
    * @return the multiplier of the patience times.
    */
   public double getPatienceTimesMultNoVirtualQueue (int mp) {
      if (ptimesMultNoVQ == null || ptimesMultNoVQ.length == 0)
         return 1;
      if (ptimesMultNoVQ.length == 1)
         return ptimesMultNoVQ[0];
      return ptimesMultNoVQ[mp];
   }

   /**
    * Returns the multiplier of patience times for
    * calls arriving during main period \texttt{mp}, joining
    * the virtual queue, successuflly called back, and
    * having to wait in regular queue.
    * The default multiplier is 1.
    * @param mp the main period of arrival.
    * @return the multiplier of the patience times.
    */
   public double getPatienceTimesMultCallBack (int mp) {
      if (ptimesMultCallBack == null || ptimesMultCallBack.length == 0)
         return 1;
      if (ptimesMultCallBack.length == 1)
         return ptimesMultCallBack[0];
      return ptimesMultCallBack[mp];
   }

   /**
    * Returns the multiplier of service times for callers
    * arrived during main period \texttt{mp}, deciding
    * not to join the virtual queue, and served
    * by an agent in group \texttt{i}.
    * @param i the index of the agent group.
    * @param mp the index of the main period.
    * @return the service times multiplier.
    */
   public double getServiceTimesMultNoVirtualQueue (int i, int mp) {
      return getValue (i, mp, stimesMultNoVQ, 1);
   }

   /**
    * Returns the multiplier of service times for callers
    * arrived during main period \texttt{mp}, and served
    * by an agent in group \texttt{i}
    * after being called back.
    * @param i the index of the agent group.
    * @param mp the index of the main period.
    * @return the service times multiplier.
    */
   public double getServiceTimesMultCallBack (int i, int mp) {
      return getValue (i, mp, stimesMultCallBack, 1);
   }

   /**
    * Applies the multipliers returned by
    * {@link #getServiceTimesMultNoVirtualQueue(int,int)}
    * to the given call \texttt{call}.
    * This changes
    * the service times returned by {@link Contact#getContactTimes()}
    * for the given call.
    * @param call the call whose service times are modified.
    */
   public void multiplyServiceTimesNoVirtualQueue (Call call) {
      if (stimesMultNoVQ == null)
         return;
      if (stimesMultNoVQ.length == 0)
         return;
      final int p = call.getArrivalPeriod ();
      final int mp = cc.getPeriodChangeEvent ().getMainPeriod (p);
      final ServiceTimes st = call.getContactTimes ();
      if (stimesMultNoVQ.length == 1) {
         double mult = getServiceTimesMultNoVirtualQueue (0, mp);
         assert !Double.isInfinite (mult);
         st.mult (mult);
      }
      else {
         st.ensureCapacityForServiceTime (cc.getNumAgentGroups ());
         for (int i = 0; i < cc.getNumAgentGroups (); i++) {
            final double smult = getServiceTimesMultNoVirtualQueue (i, mp);
            assert !Double.isInfinite (smult);
            st.setServiceTime (i, st.getServiceTime (i)*smult);
         }
      }
   }

   /**
    * Similar to {@link #multiplyServiceTimesNoVirtualQueue(Call)},
    * but using multipliers returned by {@link #getServiceTimesMultCallBack(int,int)}.
    * @param call the call whose service times are modified.
    */
   public void multiplyServiceTimesCallBack (Call call) {
      if (stimesMultCallBack == null)
         return;
      if (stimesMultCallBack.length == 0)
         return;
      final int p = call.getArrivalPeriod ();
      final int mp = cc.getPeriodChangeEvent ().getMainPeriod (p);
      final ServiceTimes st = call.getContactTimes ();
      if (stimesMultCallBack.length == 1) {
         double mult = getServiceTimesMultCallBack (0, mp);
         assert !Double.isInfinite (mult);
         st.mult (mult);
      }
      else {
         call.ensureCapacityForDefaultContactTime (cc.getNumAgentGroups ());
         for (int i = 0; i < cc.getNumAgentGroups (); i++) {
            final double smult = getServiceTimesMultCallBack (i, mp);
            assert !Double.isInfinite (smult);
            st.setServiceTime (i, st.getServiceTime (i)*smult);
         }
      }
   }

   private double getValue (int i, int mp, double[][] array, double def) {
      if (array == null || array.length == 0)
         return def;
      if (array.length == 1) {
         if (array[0].length == 0)
            return def;
         if (array[0].length == 1)
            return array[0][0];
         return array[0][mp];
      }

      if (array[i].length == 0)
         return def;
      if (array[i].length == 1)
         return array[i][0];
      return array[i][mp];
   }

   /**
    * Returns the index of the call type calls entering virtual queue
    * are changed to.
    * @return the target call type for virtual queueing.
    */
   public int getTargetVQType () {
      return vqTarget;
   }

   /**
    * Sets the target call type for virtual queueing to
    * \texttt{targetVQType}.
    * @param targetVQType the new target type.
    */
   public void setTargetVQType (int targetVQType) {
      vqTarget = targetVQType;
   }

   /**
    * Returns the probability that a caller arriving
    * during main period \texttt{mp} accepts to enter
    * virtual queue, and be called back later.
    * @param mp the main period of arrival.
    * @return the probability of entering virtual queue.
    */
   public double getProbVirtualQueue (int mp) {
      if (probVQ == null || probVQ.length == 0)
         return 0;
      if (probVQ.length == 1)
         return probVQ[0];
      return probVQ[mp];
   }

   /**
    * Returns the probability that a caller arriving
    * during main period \texttt{mp} is successfully called
    * back after joining the virtual queue.
    * @param mp the main period of arrival.
    * @return the probability of successful call back.
    */
   public double getProbVirtualQueueCallBack (int mp) {
      if (probVQCallBack == null || probVQCallBack.length == 0)
         return 1;
      if (probVQCallBack.length == 1)
         return probVQCallBack[0];
      return probVQCallBack[mp];
   }

   /**
    * Returns the threshold on the expected waiting time
    * for determining if a caller arrived during
    * main period \texttt{mp} has the possibility to be called back.
    * @param mp the main period of arrival.
    * @return the threshold on the expected waiting time.
    */
   public double getExpectedWaitingTimeThresh (int mp) {
      if (vqThresh == null || vqThresh.length == 0)
         return Double.POSITIVE_INFINITY;
      if (vqThresh.length == 1)
         return vqThresh[0];
      return vqThresh[mp];
   }

   /**
    * Returns the multiplier for the expected waiting time
    * used to determine the time spent by a caller
    * arriving during main period \texttt{mp}
    * in the virtual queue.
    * @param mp the main period of arrival.
    * @return the waiting time multiplier.
    */
   public double getExpectedWaitingTimeMult (int mp) {
      if (wtMult == null || wtMult.length == 0)
         return 1;
      if (wtMult.length == 1)
         return wtMult[0];
      return wtMult[mp];
   }

   @Override
   public Contact newInstance () {
      final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final int arrivalPeriod = pce.getCurrentPeriod ();
      final Call contact = new Call (pce, arrivalPeriod, getTypeId ());
      setRandomVariables (contact);
      return contact;
   }

   @Override
   public void setRandomVariables (Contact contact) {
      super.setRandomVariables (contact);
      final Call call = (Call) contact;
      if (isCallTransferSupported ()) {
         double u1 = ptStream.nextDouble ();
         double u2 = ptStream.nextDouble ();
         if (probTransfer != null)
            call.setUTransfer (u1);
         if (probTransferWait != null)
            call.setUTransferWait (u2);

         transGen.generate (call.getTransferTimes ());
      }
      if (vqTarget >= 0) {
         call.setUVQ (vqStream.nextDouble ());
         call.setUVQCallBack (vqStream.nextDouble ());
      }
   }

   /**
    * Generates conference times for the given call
    * \texttt{call}, and adds these conference times
    * to the regular service times.
    * @param call the call being processed.
    */
   public void setConferenceTimes (Call call) {
      confGen.generate (call.getConferenceTimes ());
      call.getContactTimes ().add (call.getConferenceTimes ());
   }

   /**
    * Similar to {@link #setConferenceTimes(Call)}, for
    * pre-service times in the case when no conference
    * occurs.
    * @param call the call being processed.
    */
   public void setPreServiceTimesNoConf (Call call) {
      psGen.generate (call.getPreServiceTimesNoConf ());
      call.getContactTimes ().add (call.getPreServiceTimesNoConf ());
   }

   /**
    * Returns the patience time, converted to
    * {@link MultiPeriodGen}.
    * Note that calling {@link MultiPeriodGen#setMult(double)}
    * on the returned instance is not recommended as the
    * multipliers are reset by {@link #init()}.
    * One should use {@link #setPatienceTimesMult(double)}
    * or {@link CallCenter#setPatienceTimesMult(double)}
    * to alter the multipliers of the patience times.
    */
   @Override
   public MultiPeriodGen getPatienceTimeGen () {
      return (MultiPeriodGen) super.getPatienceTimeGen ();
   }

   @Override
   public void setPatienceTimeGen (RandomVariateGen gen) {
      if (!(gen instanceof MultiPeriodGen))
         throw new IllegalArgumentException
         ("Invalid class for gen");
      super.setPatienceTimeGen (gen);
   }

   /**
    * Returns the random variate generator for
    * the default service times used when
    * no agent group specific service times are
    * available.
    * 
    * Note that it is not recommended to use
    * {@link MultiPeriodGen#setMult(double)}
    * on the returned object.
    * One should alter the
    * multipliers provided by {@link #getServiceTimesManager()}
    * instead.
    * @return the service time generator.
    */
   public MultiPeriodGen getServiceTimeGen () {
      return (MultiPeriodGen) getContactTimeGen ();
   }

   /**
    * Similar to {@link SingleTypeContactFactory#getContactTimeGenGroups()},
    * but returns an array of {@link MultiPeriodGen}
    * instead.
    * The same note for multipliers as
    * in method {@link #getServiceTimeGen()}
    * applies here.
    * @return the array of service times.
    */
   public MultiPeriodGen[] getServiceTimeGenGroups () {
      return sgen.getServiceTimeGenGroups ();
   }

   /**
    * Determines if calls of the associated type can
    * be produced using a call source, e.g., an arrival
    * process or a dialer.
    * By default, this returns \texttt{false} for regular call
    * types, and \texttt{true} for call types corresponding
    * to transfer or virtual queueing targets.
    */
   public boolean isDisableCallSource () {
      return disableCallSource;
   }

   /**
    * Sets the indicator for disabled call source to
    * \texttt{disableCallSource}.
    * @see #isDisableCallSource()
    */
   public void setDisableCallSource (boolean disableCallSource) {
      this.disableCallSource = disableCallSource;
   }

   /**
    * Determines if calls of the associated type are excluded from
    * the totals in statistical reports.
    * By default, this returns \texttt{false} for regular call
    * types, and \texttt{true} for call types corresponding
    * to transfer or virtual queueing targets.
    */
   public boolean isExcludedFromStatTotal () {
      return excludedFromStatTotal;
   }

   /**
    * Sets the indicator for exclusion in totals to
    * \texttt{excludedFromStatTotal}.
    * @see #isExcludedFromStatTotal()
    */
   public void setExcludedFromStatTotal (boolean excludedFromStatTotal) {
      this.excludedFromStatTotal = excludedFromStatTotal;
   }

   private class ProbAbandonGen implements ValueGenerator {

      public ProbAbandonGen () {
      }

      public void init () {}

      public double nextDouble (Contact contact) {
         final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
         final int mp = pce.getCurrentMainPeriod ();
         return getProbAbandon (mp);
      }
   }

   /**
    * Constructs and returns a contact factory that can produce calls of
    * randomly selected types.
    * This constructs and returns an instance of {@link RandomTypeCallFactory}
    * by using the probabilities obtained by parsing the list \texttt{types}.
    * Each element of this list gives a type identifier with associated probability
    * of selection.
    * See {@link RandomTypeCallFactory} for more information
    * about how the selection is performed.
    *
    * @param cc the call center model.
    * @param types
    *           the list of produced contact types.
    * @param stream
    *           the random stream used to select contact type.
    * @param checkAgents
    *           determines if the call factory checks that there are agents
    *           capable of serving the call before producing a call of a given
    *           type.
    * @return the contact type factory.
    * @throws CallFactoryCreationException
    *            if an error occurs during the creation of the factory.
    */
   public static RandomTypeCallFactory createRandomTypeContactFactory (
         CallCenter cc, List<ProducedCallTypeParams> types,
         RandomStream stream, boolean checkAgents)
   throws CallFactoryCreationException {
      //final ContactFactory[] factories = cc.getCallFactories ();
      final double[][] prob = new double[cc.getNumContactTypes()][];
      // Arrays.fill (prob, Double.NaN);
      for (final ProducedCallTypeParams pct : types) {
         final int k = pct.getType ();
         // We do not check if the call source producing calls of
         // type k is disabled, because this method can also be called
         // to construct the call factories generating transfer targets.
         if (k < 0 || k >= prob.length)
            throw new CallFactoryCreationException ("Call type index " + k
                  + " out of bounds");
         final double[] p;
         if (pct.isSetProbPeriod ())
            p = pct.getProbPeriod ();
         else if (pct.isSetProbability ())
            p = new double[] { pct.getProbability () };
         else
            throw new CallFactoryCreationException ("Call type index " + k
                  + " has no associated probability");
         // final double p = pct.getProbability ();
         if (prob[k] == null)
            prob[k] = p;
         else
            throw new CallFactoryCreationException ("Call type index " + k
                  + " given more than once");
      }
      for (int k = 0; k < prob.length; k++)
         if (prob[k] == null)
            prob[k] = new double[] { 0 };
      // return new RandomTypeContactFactory
      // (factories, prob, stream);
      return new RandomTypeCallFactory (cc, prob, stream, checkAgents);
   }

   /**
    * For each element in the list \texttt{types},
    * tests that the type identifier
    * returned by the {@link ProducedCallTypeParams#getType()}
    * method is smaller than the given constant
    * \texttt{numInCallTypes}. This condition is necessary for the indices to
    * represent inbound call types.
    * 
    * @param numInCallTypes
    *           the number of inbound call types.
    * @param types
    *           the list of call type records to test.
    * @exception IllegalArgumentException
    *               if at least one type identifier is invalid.
    */
   public static void checkInbound (int numInCallTypes,
         List<ProducedCallTypeParams> types) {
      int idx = 0;
      for (final ProducedCallTypeParams ct : types) {
         final int type = ct.getType ();
         if (type < 0)
            throw new IllegalArgumentException ("The type with index " + idx
                  + " must not be negative");
         if (type >= numInCallTypes)
            throw new IllegalArgumentException ("The type with index " + idx
                  + " does not correspond to an inbound call type");
         ++idx;
      }
   }

   /**
    * For each element in the list \texttt{types},
    * tests that the type identifier
    * returned by the {@link ProducedCallTypeParams#getType()}
    * method is greater than or equal to \texttt{numInCallTypes}
    * but
    * smaller than
    * \texttt{numCallTypes}. This condition is necessary for the indices to
    * represent inbound call types.
    * 
    * @param numInCallTypes
    *           the number of inbound call types.
    * @param numCallTypes
    *           the number of call types, inbound or outbound.
    * @param types
    *           the list of call type records to test.
    * @exception IllegalArgumentException
    *               if \texttt{numInCallTypes} is greater than
    *               \texttt{numCallTypes}, or
    *               if at least one type identifier is invalid.
    */
   public static void checkOutbound (int numInCallTypes, int numCallTypes,
         List<ProducedCallTypeParams> types) {
      if (numInCallTypes > numCallTypes)
         throw new IllegalArgumentException (
         "The number of inbound call types must be smaller than or equal to the number of call types");
      int idx = 0;
      for (final ProducedCallTypeParams ct : types) {
         final int type = ct.getType ();
         if (type < numInCallTypes)
            throw new IllegalArgumentException ("The type with index " + idx
                  + " corresponds to an inbound call type");
         if (type >= numCallTypes)
            throw new IllegalArgumentException ("The type with index " + idx
                  + " does not correspond to an outbound call type");
         ++idx;
      }
   }
}
