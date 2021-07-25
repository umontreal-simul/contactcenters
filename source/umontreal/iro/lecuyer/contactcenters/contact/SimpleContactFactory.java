package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.simevents.Simulator;

/**
 * This implements the {@link ContactFactory} interface to
 * instantiate {@link Contact} objects
 * with fixed parameters.
 */
public class SimpleContactFactory implements ContactFactory {
   private Simulator sim;
   private double priority = 1.0;
   private int typeId = 0;
   private boolean tracing = false;

   /**
    * Constructs a new contact factory which will
    * create contact objects with priority 1 and
    * type ID 0.
    */
   public SimpleContactFactory() {
      sim = Simulator.getDefaultSimulator ();
   }

   /**
    * Equivalent to {@link #SimpleContactFactory()},
    * using the given simulator \texttt{sim}.
    */
   public SimpleContactFactory (Simulator sim) {
      if (sim == null)
         throw new NullPointerException();
      this.sim = sim;
   }

   /**
    * Constructs a new contact factory which will
    * create contact objects with priority 1 and
    * type ID \texttt{typeId}.
    @param typeId the type ID of the contacts.
    */
   public SimpleContactFactory (int typeId) {
      this (Simulator.getDefaultSimulator (), typeId);
   }

   /**
    * Equivalent to {@link #SimpleContactFactory(int)},
    * using the given simulator \texttt{sim}.
    */
   public SimpleContactFactory (Simulator sim, int typeId) {
      if (sim == null)
         throw new NullPointerException();
      this.sim = sim;
      this.typeId = typeId;
   }

   /**
    * Constructs a new contact factory which will
    * create contact objects with priority \texttt{priority} and
    * type ID \texttt{typeId}.
    * If \texttt{tracing} is \texttt{true}, contact objects
    * with steps tracing enabled will be created.
    @param priority the priority of the contact.
    @param typeId the type ID of the contacts.
    @param tracing the contact steps tracing indicator.
    */
   public SimpleContactFactory (double priority, int typeId,
         boolean tracing) {
      this (Simulator.getDefaultSimulator (), priority, typeId, tracing);
   }

   /**
    * Equivalent to {@link #SimpleContactFactory(double,int,boolean)},
    * using the given simulator \texttt{sim}.
    */
   public SimpleContactFactory (Simulator sim, double priority, int typeId,
                                boolean tracing) {
      if (sim == null)
         throw new NullPointerException();
      this.sim = sim;
      this.priority = priority;
      this.typeId = typeId;
      this.tracing = tracing;
   }

   /**
    * Returns the simulator associated with this contact factory.
    * This simulator is associated with every
    * contact instantiated by the factory.
    * @return the associated simulator.
    */
   public Simulator simulator() {
      return sim;
   }

   /**
    * Sets the simulator associated with this contact factory to
    * \texttt{sim}.
    * @param sim the new associated simulator.
    */
   public void setSimulator (Simulator sim) {
      if (sim == null)
         throw new NullPointerException();
      this.sim = sim;
   }

   /**
    * Returns the priority of the created and reused
    * contact objects.
    @return the priority of the generated contact.
    */
   public double getPriority() {
      return priority;
   }

   /**
    * Returns the type ID of the created and reused
    * contact objects.
    @return the type ID of the generated contact.
    */
   public int getTypeId() {
      return typeId;
   }

   /**
    * Returns \texttt{true} if the created
    * contacts will support steps tracing.
    @return the contact steps tracing indicator.
    */
   public boolean getTracing() {
      return tracing;
   }

   @Override
   public Contact newInstance() {
      final Contact contact = new Contact (sim, priority, typeId);
      if (tracing)
         contact.enableStepsTracing();
      return contact;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder
         (getClass().getSimpleName());
      sb.append ('[');
      sb.append ("priority: ").append (priority);
      sb.append (", type identifier: ").append (typeId);
      sb.append (", ").append (tracing ? "with" : "without");
      sb.append (" steps tracing]");
      return sb.toString();
   }
}
