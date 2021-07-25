/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.ctmc;

public class FalseTransitionEvent implements CCEvent {

   public TransitionType actions (CallCenterCTMCKI ctmc, int tr, int rv, int usedBits, boolean changeState) {
      return TransitionType.FALSETRANSITION;
   }
}