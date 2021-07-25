package umontreal.iro.lecuyer.contactcenters.queue;

import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Value generator for the patience time of contacts. This implementation simply
 * calls the {@link Contact#getDefaultPatienceTime} method to get the patience
 * times. For each new waiting queue, such a value generator is created and used
 * by default.
 */
public class ContactPatienceTimeGenerator implements ValueGenerator {
   public void init () {}

   public double nextDouble (Contact contact) {
      return contact.getDefaultPatienceTime ();
   }
}
