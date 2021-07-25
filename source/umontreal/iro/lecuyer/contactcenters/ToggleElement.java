package umontreal.iro.lecuyer.contactcenters;

/**
 * Specifies an element that can be enabled or disabled at any time during the
 * simulation. The meaning of the ``enabled'' and ``disabled'' states depends on
 * the particular toggle element. For example, an enabled contact arrival
 * process provides contacts to the system whereas a disabled arrival process
 * does not.
 */
public interface ToggleElement {
   /**
    * Enables the element represented by this object. This method throws an
    * {@link IllegalStateException} if the element is already enabled.
    * 
    * @exception IllegalStateException
    *               if the element is already enabled.
    */
   public void start ();

   /**
    * Disables the element represented by this object. This method throws an
    * {@link IllegalStateException} if the element is already disabled.
    * 
    * @exception IllegalStateException
    *               if the element is already disabled.
    */
   public void stop ();

   /**
    * Determines if the element is enabled or disabled. Returns \texttt{true} if
    * the element is enabled, \texttt{false} otherwise.
    * 
    * @return the current state of the element.
    */
   public boolean isStarted ();
}
