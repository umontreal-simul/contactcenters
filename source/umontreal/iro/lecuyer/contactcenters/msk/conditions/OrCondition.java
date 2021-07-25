package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Represents a condition checking that at least
 * one of a list of conditions applies.
 */
public class OrCondition implements Condition, Initializable, ToggleElement {
   private Condition[] condList;
   private boolean started = false;
   
   /**
    * Constructs a new or condition based on
    * the list of conditions \texttt{condList}.
    * @param condList the list on conditions used
    * to perform the test.
    */
   public OrCondition (Condition... condList) {
      if (condList == null)
         throw new NullPointerException();
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
         if (cond.applies (contact))
            return true;
      return false;
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
