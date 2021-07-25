package umontreal.iro.lecuyer.contactcenters;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.randvar.RandomVariateGen;

/**
 * Represents a value generator for random variates used for simulating contact
 * centers. Implementations of this interface are linked to contact center
 * objects and usually uses a random variate generator to obtain some continuous
 * variates. The generator used or some adjustments made to the value can depend
 * on the concerned contact, the object containing this value generator, the
 * simulation time, etc. This interface defines a method similar to
 * {@link RandomVariateGen#nextDouble} but taking a {@link Contact} object as an
 * argument. This way, random values can depend on the particular contact.
 */
public interface ValueGenerator extends Initializable {
   /**
    * Generates and returns a new value for the contact \texttt{contact}. If
    * \texttt{contact} is \texttt{null} and this is not allowed by the
    * implementation, this method should throw a {@link NullPointerException}.
    * 
    * @param contact
    *           the contact being concerned.
    * @return the generated value.
    * @exception NullPointerException
    *               if \texttt{contact} is illegally \texttt{null}.
    */
   public double nextDouble (Contact contact);

   /**
    * Initializes the generator at the beginning of the simulation.
    */
   public void init ();
}
