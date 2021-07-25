package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents a condition that checks if all
 * conditions of a given list applies.
 */
public class AndCondition implements Condition, Initializable, ToggleElement {
   private Condition[] condList;
   private boolean started = false;
   
   /**
    * Constructs a new and condition using
    * the list of conditions \texttt{condList}.
    * @param condList the list of conditions to check.
    */
   public AndCondition (Condition... condList) {
      this.condList = condList;
   }
   
   /**
    * Returns the associated list of conditions.
    */
   public Condition[] getConditions() {
      return condList.clone ();
   }

   public boolean applies (Contact contact) {
      for (Condition cond : condList)
         if (!cond.applies (contact))
            return false;
      return true;
   }

   public boolean isStarted () {
      return started;
   }

   /**
    * Calls \texttt{start} for each associated condition
    * implementing the {@link ToggleElement} interface.
    */
   public void start () {
      for (Condition cond : condList)
         if (cond instanceof ToggleElement)
            ((ToggleElement)cond).start ();
      started = true;
   }

   /**
    * Calls \texttt{stop} for each associated condition
    * implementing the {@link ToggleElement} interface.
    */
   public void stop () {
      for (Condition cond : condList)
         if (cond instanceof ToggleElement)
            ((ToggleElement)cond).stop ();
      started = false;
   }

   /**
    * Calls {@link #stop()}, then calls
    * \texttt{init} for each initializable condition
    * associated with this object.
    */
   public void init () {
      stop();
      for (Condition cond : condList)
         if (cond instanceof Initializable)
            ((Initializable)cond).init ();
   }
}
