package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Represents a factory for creating new call center events.
 */
public interface CCEventFactory {
   /**
    * Creates a new call center event resulting from a uniform
    * generated in interval $[u_1, u_2]$, and using a maximum of
    * \texttt{maxExtraBits} additional random bits to take decisions.
    * @param u1 the uniform $u_1$.
    * @param u2 the uniform $u_2$.
    * @param maxExtraBits the maximal number of additional bits that
    * can be used by the created events to take decisions.
    * @return the constructed event.
    */
   public CCEvent newInstance (double u1, double u2, int maxExtraBits);
}
