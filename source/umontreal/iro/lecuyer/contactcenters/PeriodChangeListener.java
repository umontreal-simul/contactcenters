package umontreal.iro.lecuyer.contactcenters;

/**
 * Represents a period-change listener being notified when period-change events
 * occur.
 */
public interface PeriodChangeListener {
   /**
    * Switches to the next period defined by \texttt{pce}. This can change the
    * parameters and correct scheduled events accordingly. If no parameters are
    * available for the new period, the method should try to use those of the
    * last available period. The listener is called after the period change has
    * occurred, so {@link PeriodChangeEvent#getCurrentPeriod()} returns the index
    * of the new period.
    * 
    * @param pce
    *           the source period-change event.
    */
   public void changePeriod (PeriodChangeEvent pce);

   /**
    * This method is called after the period-change event is stopped by
    * {@link PeriodChangeEvent#stop()}.
    * 
    * @param pce
    *           the period-change event being stopped.
    */
   public void stop (PeriodChangeEvent pce);
}
