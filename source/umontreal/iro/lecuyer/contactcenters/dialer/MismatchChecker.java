package umontreal.iro.lecuyer.contactcenters.dialer;

import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroupListener;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

/**
 * This agent-group listener checks that the number of
 * free agents in the test and target sets
 * for a given dialer never fall outside the
 * user-defined thresholds while dialing is in-progress.
 * This listener is constructed using a dialer using
 * an instance of {@link ThresholdDialerPolicy} as a dialer's policy.
 * It should then be registered with all agent groups in
 * the target sets.
 * 
 * Each time a service begins (and the number of free agents is reduced),
 * the method {@link #checkThresh()} is called, and
 * checks for the thresholds.
 * If the number of free agents becomes smaller than the given
 * threshold, in-progress dialing is stopped.
 * If the policy is not an instance of {@link ThresholdDialerPolicy},
 * this listener does nothing. 
 */
public class MismatchChecker implements AgentGroupListener {
   private Dialer dialer;

   /**
    * Constructs a new mismatch checker for
    * the dialer \texttt{dialer}.
    * @param dialer the dialer for which mismatches are checked.
    */
   public MismatchChecker (Dialer dialer) {
      if (dialer == null)
         throw new NullPointerException();
      this.dialer = dialer;
   }

   public void agentGroupChange (AgentGroup group) {
      checkThresh ();
   }

   public void beginService (EndServiceEvent ev) {
      checkThresh ();
   }

   public void endContact (EndServiceEvent ev) {}

   public void endService (EndServiceEvent ev) {}

   public void init (AgentGroup group) {}

   /**
    * Checks the thresholds on the number of free agents
    * in the test and target sets for the dialer's policy
    * of the associated dialer.
    */
   public void checkThresh () {
      final DialerPolicy pol = dialer.getDialerPolicy ();
      if (pol instanceof ThresholdDialerPolicy) {
         final ThresholdDialerPolicy tpol = (ThresholdDialerPolicy) pol;
         int nFree = tpol.getTestSet ().getNumFreeAgents ();
         int nf = tpol.getMinFreeAgentsTest ();
         if (nFree < nf)
            dialer.stopDial ();
         else {
            nf = tpol.getMinFreeAgentsTarget ();
            nFree = tpol.getTargetSet ().getNumFreeAgents ();
            if (nFree < nf)
               dialer.stopDial ();
         }
      }
   }
}
