package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupSet;

/**
 * Regroups objects used by dialers.
 * This class encapsulates the testing set containing
 * all the agent groups, and value generators
 * for the reaching probability, and reaching
 * and failing times.
 * These parameters are the same for every dialer, but they are
 * not needed if no dialer is used.
 */
public class DialerObjects {
   private AgentGroupSet testSet;
   private ValueGenerator pReachGen;
   private ValueGenerator reachGen;
   private ValueGenerator failGen;
   
   /**
    * Constructs a new set of dialer objects from
    * the given call center model.
    * @param cc the call center model.
    */
   public DialerObjects (CallCenter cc) {
      pReachGen = new ProbReachGen (cc);
      reachGen = new ReachTimeGen (cc);
      failGen = new FailTimeGen (cc);
      testSet = new AgentGroupSet();
      for (final AgentGroupManager group : cc.getAgentGroupManagers ())
         testSet.add (group.getAgentGroup());
   }
   
   /**
    * Returns the testing set of agent groups
    * used by some dialing policies.
    * @return the testing set of agent groups.
    */
   public AgentGroupSet getAgentGroupTestSet () {
      return testSet;
   }
   
   
   /**
    * Returns the value generator giving the probability
    * of right party connect for any outbound call.
    * The probability often depends on the call type and
    * period of arrival of the call.
    * @return the value generator for the probability of
    * right party connect.
    */
   public ValueGenerator getProbReachGen() {
      return pReachGen;
   }
   
   /**
    * Returns the value generator giving the needed time
    * for a caller to be reached.
    * By using a value generator, the distribution of this
    * (random) time can depend on the call type and
    * period of arrival.
    * @return the value generator for the reach times.
    */
   public ValueGenerator getReachTimeGen() {
      return reachGen;
   }
   
   /**
    * Returns the value generator for the needed time
    * for an outbound call to fail.
    * This method is similar to {@link #getReachTimeGen()}, for
    * fail times.
    * @return the value generator for fail times.
    */
   public ValueGenerator getFailTimeGen() {
      return failGen;
   }
   
   private static class ProbReachGen implements ValueGenerator {
      private CallCenter cc;
      
      public ProbReachGen (CallCenter cc) {
         this.cc = cc;
      }

      public void init () {
      }

      public double nextDouble (Contact contact) {
         final int k = contact.getTypeId();
         final int mp = cc.getPeriodChangeEvent().getCurrentMainPeriod();
         final CallFactory factory = cc.getCallFactory (k);
         if (factory instanceof OutboundCallFactory)
            return ((OutboundCallFactory)factory).getProbReach (mp);
         return 0;
      }
   }
   
   private static class ReachTimeGen implements ValueGenerator {
      private CallCenter model;
      
      public ReachTimeGen (CallCenter model) {
         this.model = model;
      }

      public void init () {
      }

      public double nextDouble (Contact contact) {
         final int k = contact.getTypeId();
         final CallFactory factory = model.getCallFactory (k);
         if (factory instanceof OutboundCallFactory) {
            final OutboundCallFactory oFactory = (OutboundCallFactory)factory;
            if (oFactory.getReachGen () != null)
               return oFactory.getReachGen ().nextDouble ();
         }
         return 0;
      }
   }
   
   private static class FailTimeGen implements ValueGenerator {
      private CallCenter model;
      
      public FailTimeGen (CallCenter model) {
         this.model = model;
      }

      public void init () {
      }

      public double nextDouble (Contact contact) {
         final int k = contact.getTypeId();
         final CallFactory factory = model.getCallFactory (k);
         if (factory instanceof OutboundCallFactory) {
            final OutboundCallFactory oFactory = (OutboundCallFactory)factory;
            if (oFactory.getFailGen () != null)
               return oFactory.getFailGen ().nextDouble ();
         }
         return 0;
      }
   }
}
