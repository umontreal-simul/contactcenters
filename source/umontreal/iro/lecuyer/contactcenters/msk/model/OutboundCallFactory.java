package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ServiceTimes;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.OutboundTypeParams;

import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;

/**
 * Represents a call factory for outbound calls.
 * This extends {@link CallFactory} with parameters
 * specific to outbound calls: the probability of right-party
 * connect, and the generators for reach and fail times.
 */
public class OutboundCallFactory extends CallFactory {
   private double[] probReach;
   private double[] probRPC;
   private MultiPeriodGen reachGen;
   private MultiPeriodGen failGen;
   private ServiceTimesManager prevGen;
   private RandomStream streamRPC;

   /**
    * Constructs a new call factory for outbound call.
    * @param cc the call center.
    * @param ccParams the call center parameters.
    * @param par the parameters of the outbound call type.
    * @param k the index of the call type.
    * @throws CallFactoryCreationException if an error occurs during
    * the creation of the factory.
    */
   public OutboundCallFactory (CallCenter cc,
         CallCenterParams ccParams, OutboundTypeParams par, int k)
         throws CallFactoryCreationException {
      super (cc, ccParams, par, k);
      streamRPC = cc.getRandomStreams ().getCallFactoryStream (k, CallFactoryStreamType.SERVICE);
      try {
         probReach = CallCenterUtil.getDoubleArray (par.getProbReach (), cc.getNumMainPeriods ());
      }
      catch (final IllegalArgumentException iae) {
         throw new CallFactoryCreationException
         ("Cannot initialize probabilities of reaching", iae);
      }
      try {
         probRPC = CallCenterUtil.getDoubleArray (par.getProbRPC (), cc.getNumMainPeriods ());
      }
      catch (final IllegalArgumentException iae) {
         throw new CallFactoryCreationException
         ("Cannot initialize probabilities of right party connect", iae);
      }
      final TimeUnit defaultUnit = cc.getDefaultUnit ();
      final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final RandomStream rfStream = cc.getRandomStreams ().getDialerStream
      (k - cc.getNumInContactTypes (), DialerStreamType.DIALDELAY); 
      try {
         if (par.isSetReachTime ()) {
            reachGen = CCParamReadHelper.createGenerator (par.getReachTime (),
                  rfStream, pce);
            reachGen.setTargetTimeUnit (defaultUnit);
         }
      }
      catch (final DistributionCreationException dce) {
         throw new CallFactoryCreationException (
               "Cannot create reach time distribution", dce);
      }
      catch (final GeneratorCreationException gce) {
         throw new CallFactoryCreationException (
               "Cannot create reach time generator", gce);
      }
      try {
         if (par.isSetFailTime ()) {
            failGen = CCParamReadHelper.createGenerator (par.getFailTime (),
                  rfStream, pce);
            failGen.setTargetTimeUnit (defaultUnit);
         }
      }
      catch (final DistributionCreationException dce) {
         throw new CallFactoryCreationException (
               "Cannot create fail time distribution", dce);
      }
      catch (final GeneratorCreationException gce) {
         throw new CallFactoryCreationException (
               "Cannot create fail time generator", gce);
      }
      
      final RandomStream sStream = cc.getRandomStreams ().getCallFactoryStream (k, CallFactoryStreamType.SERVICE);
      prevGen = new ServiceTimesManager (cc, "preview time", par.getPreviewTimes (), k, sStream, par.getPreviewTimesMult (), ccParams.getAgentGroups().size());
   }
   
   @Override
   public void init() {
      super.init ();
      prevGen.init (getCallCenter().getPreviewTimesMult ());
   }

   /**
    * Returns the probability of right party connect
    * for this outbound call type during main
    * period \texttt{p}.
    * @param mp the index of the main period.
    * @return the probability of right party connect.
    */
   public double getProbReach (int mp) {
      if (probReach.length == 0)
         return 0;
      if (probReach.length == 1)
         return probReach[0];
      return probReach[mp];
   }

   /**
    * Returns the probability of right party connect
    * for this outbound call type during main
    * period \texttt{p}.
    * @param mp the index of the main period.
    * @return the probability of right party connect.
    */
   public double getProbRPC (int mp) {
      if (probRPC.length == 0)
         return 1;
      if (probRPC.length == 1)
         return probRPC[0];
      return probRPC[mp];
   }
   
   /**
    * Returns the random variate generator for
    * reach times.
    * @return the random variate generator for reach times.
    */
   public MultiPeriodGen getReachGen () {
      return reachGen;
   }

   /**
    * Returns the random variate generator for
    * fail times.
    * @return the random variate generatof for fail times.
    */
   public MultiPeriodGen getFailGen () {
      return failGen;
   }
   
   @Override
   public void setRandomVariables (Contact contact) {
      super.setRandomVariables (contact);
      final Call call = (Call)contact;
      final int ap = getCallCenter().getPeriodChangeEvent ().getMainPeriod (call.getArrivalPeriod ());
      final double prob = getProbRPC (ap);
      boolean rpc = streamRPC.nextDouble () <= prob;
      call.setRightPartyConnect (rpc);
      
      if (rpc) {
         ServiceTimes st = new ServiceTimes (0);
         prevGen.generate (st);
         call.getContactTimes ().add (st);
      }
      else
         prevGen.generate (call.getContactTimes ());
//      double prevTime;
//      if (prevGen.getServiceTimeGen () != null)
//         prevTime = prevGen.getServiceTimeGen ().nextDouble ();
//      else
//         prevTime = 0;
//      final int I = getCallCenter().getNumAgentGroups ();
//      final MultiPeriodGen[] prevGenGroups = prevGen.getServiceTimeGenGroups ();
//      if (prevGenGroups != null)
//         for (int i = 0; i < I; i++) {
//            double prevTimeGroup;
//            if (i < prevGenGroups.length && prevGenGroups[i] != null)
//               prevTimeGroup = prevGenGroups[i].nextDouble ();
//            else
//               prevTimeGroup = prevTime;
//            if (prevTimeGroup != prevTime || contact.isSetDefaultContactTime (i))
//               if (rpc)
//                  contact.setDefaultContactTime (i, contact.getDefaultContactTime (i) + prevTimeGroup);
//               else
//                  contact.setDefaultContactTime (i, prevTimeGroup);
//         }
//      else if (!rpc)
//         for (int i = 0; i < I; i++)
//            // Revert to default preview time
//            contact.setDefaultContactTime (i, Double.NaN);
//      if (rpc)
//         contact.setDefaultContactTime (contact.getDefaultContactTime () + prevTime);
//      else
//         contact.setDefaultContactTime (prevTime);
   }
}
