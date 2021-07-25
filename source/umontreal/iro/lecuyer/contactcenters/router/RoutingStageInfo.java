package umontreal.iro.lecuyer.contactcenters.router;

/**
 * Represents a stage for routing, with
 * a minimal waiting time, and two rank functions for
 * agent and contact selections.
 * Instances of this class are used by the
 * {@link OverflowAndPriorityRouter} router.
 */
public interface RoutingStageInfo {
   /**
    * Returns the minimal waiting time for this routing stage.
    */
   public double getWaitingTime ();

   /**
    * Returns the rank function for agent selection at this
    * stage of routing.
    */
   public RankFunction getRankFunctionForAgentSelection ();

   /**
    * Returns the rank function for contact selection at this
    * stage of routing.
    */
   public RankFunction getRankFunctionForContactSelection ();
}
