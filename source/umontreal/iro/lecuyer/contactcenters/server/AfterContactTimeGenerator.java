package umontreal.iro.lecuyer.contactcenters.server;

import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

/**
 * Value generator for the after-contact time of contacts.
 * This implementation simply calls
 * the {@link Contact#getDefaultAfterContactTime} method to
 * get the after contact times.
 * For each new agent group, such a value generator is created
 * and used by default.
 */
public class AfterContactTimeGenerator implements ValueGenerator {
   private AgentGroup group;
   private double[] mult;

   /**
    * Constructs an after-contact time generator
    * returning the same after-contact time
    * for each contact type.
    * @param group the associated agent group.
    */
   public AfterContactTimeGenerator (AgentGroup group) {
      this.group = group;
   }

   /**
    * Constructs a new after-contact time generator with a
    * different multiplier for each contact type.
    * When an after-contact time is required for a contact of type
    * \texttt{k}, the result of {@link Contact#getDefaultAfterContactTime}
    * is multiplied by \texttt{mult[k]}.
    @param group the associated agent group.
    @param mult the vector of after-contact time multipliers.
    */
   public AfterContactTimeGenerator (AgentGroup group, double[] mult) {
      this.group = group;
      this.mult = mult == null ? null : mult.clone ();
   }

   /**
    * Returns the vector of multipliers
    * for this after-contact time generator.
    * For contact type \texttt{k}, the multiplier of
    * the after-contact times is given by the
    * element with index \texttt{k} in the array.
    * If this returns \texttt{null}, after-contact times
    * all have multiplier 1.
    @return the vector of after-contact times multipliers.
    */
   public double[] getMultipliers() {
      return mult.clone ();
   }

   /**
    * Sets the after-contact time multiplier for each contact type
    * to \texttt{mult}.
    @param mult the new vector of after-contact times multipliers.
    */
   public void setMultipliers (double[] mult) {
      this.mult = mult == null ? null : mult.clone ();
   }
   
   /**
    * Returns the reference to the associated agent group.
    * @return the associated agent group.
    */
   public AgentGroup getAgentGroup() {
      return group;
   }
   
   /**
    * Sets the associated agent group to \texttt{group}.
    * @param group the new associated agent group.
    */
   public void setAgentGroup (AgentGroup group) {
      this.group = group;
   }

   public void init() {}

   public double nextDouble (Contact contact) {
      final int i = group == null ? -1 : group.getId ();
      final double t = i == -1 ? contact.getDefaultAfterContactTime() : contact.getDefaultAfterContactTime (i);
      if (mult == null)
         return t;
      final int tid = contact.getTypeId();
      double f = 1.0;
      if (tid < mult.length)
         f = mult[tid];
      return t*f;
   }

   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      if (mult == null)
         sb.append ("no multipliers");
      else
         sb.append ("using multipliers");
      sb.append (']');
      return sb.toString();
   }
}
