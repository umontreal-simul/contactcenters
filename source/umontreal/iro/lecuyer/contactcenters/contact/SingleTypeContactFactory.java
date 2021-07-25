package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a contact factory used to create contacts
 * of a single type.  This factory also
 * associates default patience, contact, and after-contact times
 * to the constructed contacts.
 * All random variates are generated at the
 * time the contact is created.
 */
public class SingleTypeContactFactory implements ContactFactory {
   private Simulator sim;
   private int type;
   private ValueGenerator probBalkGen;
   private RandomStream streamBalk;
   private RandomVariateGen pgen;
   private RandomVariateGen cgen;
   private RandomVariateGen[] cgenGroups;
   private RandomVariateGen acgen;
   private RandomVariateGen[] acgenGroups;

   /**
    * Constructs a new contact factory constructing
    * contacts of type \texttt{type}.
    * The \texttt{probBalkGen} value generator is used
    * to generate probabilities of balking
    * while \texttt{streamBalk} is
    * used to determine if the contact balks
    * if not served immediately.
    * The generators \texttt{pgen},
    * \texttt{cgen}, and \texttt{acgen}
    * are used to generate patience times
    * for contacts that do not balk,
    * contact times, and after-contact times.
    * \texttt{cgenGroups} and \texttt{acgenGroups}
    * can be used to generate contact and
    * after contact times
    * used if the contact is served
    * by a specific agent group.
    * 
    * If \texttt{probBalkGen} or \texttt{streamBalk} are \texttt{null},
    * the probability of balking will always be 0.
    * If \texttt{pgen} is \texttt{null}, the patience
    * time will always be infinite.
    * The default contact time when the
    * given generator is \texttt{null} is infinite while
    * the default after-contact time is 0.
    * 
    * The constructed call factory assigns the
    * default simulator returned by {@link Simulator#getDefaultSimulator()}
    * to each new contact.
    * @param type the contact type identifier of all new contacts.
    * @param probBalkGen the generator for balking probabilities.
    * @param streamBalk the random stream for balking.
    * @param pgen the patience time generator.
    * @param cgen the default contact time generator.
    * @param cgenGroups the agent-group specific contact time generators.
    * @param acgen the default after-contact time generator.
    * @param acgenGroups the agent-group specific after-contact time generators.
    */
   public SingleTypeContactFactory (int type, ValueGenerator probBalkGen, RandomStream streamBalk,
         RandomVariateGen pgen, RandomVariateGen cgen,
         RandomVariateGen[] cgenGroups,
         RandomVariateGen acgen,
         RandomVariateGen[] acgenGroups) {
      this (Simulator.getDefaultSimulator (), type, probBalkGen, streamBalk, pgen, cgen, cgenGroups, acgen, acgenGroups);
   }
   
   /**
    * Equivalent to {@link #SingleTypeContactFactory(int,ValueGenerator,RandomStream,RandomVariateGen,RandomVariateGen,RandomVariateGen[],RandomVariateGen,RandomVariateGen[])},
    * using the given simulator \texttt{sim}.
    */
   public SingleTypeContactFactory (Simulator sim, int type, ValueGenerator probBalkGen, RandomStream streamBalk,
         RandomVariateGen pgen, RandomVariateGen cgen,
         RandomVariateGen[] cgenGroups,
         RandomVariateGen acgen,
         RandomVariateGen[] acgenGroups) {
      this.type = type;
      this.streamBalk = streamBalk;
      this.probBalkGen = probBalkGen;
      this.pgen = pgen;
      this.cgen = cgen;
      this.cgenGroups = cgenGroups == null ? null : cgenGroups.clone ();
      this.acgen = acgen;
      this.acgenGroups = acgenGroups == null ? null : acgenGroups.clone ();
      this.sim = sim;
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
    * Creates a new instance of class
    * {@link Contact}, and initializes it by
    * calling the {@link #setRandomVariables(Contact)}
    * method.
    */
   public Contact newInstance () {
      final Contact contact = new Contact (sim, type);
      setRandomVariables (contact);
      return contact;
   }
   
   /**
    * Generates the random variates related to a contact, and
    * assigns the generated value to the given \texttt{contact}
    * object.
    * @param contact the contact object to set up.
    */
   public void setRandomVariables (Contact contact) {
      assert sim == contact.simulator ();
      final double prob = probBalkGen == null ? 0 : probBalkGen.nextDouble (contact); 
      final double u = streamBalk == null ? 1 : streamBalk.nextDouble ();
      if (u < prob)
         contact.setDefaultPatienceTime (0);
      else if (pgen == null)
         contact.setDefaultPatienceTime (Double.POSITIVE_INFINITY);
      else
         contact.setDefaultPatienceTime (pgen.nextDouble ());
      if (cgen != null)
         // If cgen is null, keep the default Double.POSITIVE_INFINITY
         // service time. A 0 service time can cause infinite loops
         // in the simulation.
         contact.getContactTimes ().setServiceTime (cgen.nextDouble ());
      if (cgenGroups != null) {
         contact.getContactTimes ().ensureCapacityForServiceTime (cgenGroups.length);
         for (int i = 0; i < cgenGroups.length; i++)
            if (cgenGroups[i] != null)
               contact.getContactTimes ().setServiceTime (i, cgenGroups[i].nextDouble ());
      }
      if (acgen != null)
         contact.getAfterContactTimes ().setServiceTime (acgen.nextDouble ());
      if (acgenGroups != null) {
         contact.getAfterContactTimes ().ensureCapacityForServiceTime (acgenGroups.length);
         for (int i = 0; i < acgenGroups.length; i++)
            if (acgenGroups[i] != null)
               contact.getAfterContactTimes ().setServiceTime (i, acgenGroups[i].nextDouble ());
      }
   }

   /**
    * Returns the random stream used for balking.
    * @return the random stream used for balking.
    */
   public RandomStream getStreamBalk () {
      return streamBalk;
   }

   /**
    * Sets the random stream used for
    * balking to \texttt{streamBalk}.
    * @param streamBalk the new random stream for balking.
    */
   public void setStreamBalk (RandomStream streamBalk) {
      this.streamBalk = streamBalk;
   }
   
   /**
    * Returns a reference to the value generator used
    * for generating probabilities of balking.
    */
   public ValueGenerator getProbBalkGenerator() {
      return probBalkGen;
   }
   
   /**
    * Sets the value generator for probability of
    * balking to \texttt{probBalkGen}.
    */
   public void setProbBalkGenerator (ValueGenerator probBalkGen) {
      this.probBalkGen = probBalkGen;
   }

   /**
    * Returns the random-variate generator for patience times.
    * @return the random variate generator for patience times.
    */
   public RandomVariateGen getPatienceTimeGen () {
      return pgen;
   }

   /**
    * Sets the random variate generator for
    * patience times to \texttt{pgen}.
    * @param pgen the new random variate generator for patience times.
    */
   public void setPatienceTimeGen (RandomVariateGen pgen) {
      this.pgen = pgen;
   }

   /**
    * Returns the random-variate generator for default contact times.
    * This generates the contact times used when
    * no contact time specific to the agent group
    * performing the service is available.
    * @return the random variate generator for contact times.
    */
   public RandomVariateGen getContactTimeGen () {
      return cgen;
   }

   /**
    * Sets the random variate generator for
    * default contact times to \texttt{cgen}.
    * @param cgen the new random variate generator for contact times.
    */
   public void setContactTimeGen (RandomVariateGen cgen) {
      this.cgen = cgen;
   }

   /**
    * Returns the random-variate generator for default after-contact times.
    * This generates the after-contact times used when
    * no after-contact time specific to the agent group
    * performing the service is available.
    * @return the random variate generator for default after-contact times.
    */
   public RandomVariateGen getAfterContactTimeGen () {
      return acgen;
   }

   /**
    * Sets the random variate generator for
    * default after-contact times to \texttt{acgen}.
    * @param acgen the new random variate generator for default after-contact times.
    */
   public void setAfterContactTimeGen (RandomVariateGen acgen) {
      this.acgen = acgen;
   }
   
   /**
    * Returns the random variate generators for
    * contact times when served by agents in
    * specific groups.
    * @return the contact time generators.
    */
   public RandomVariateGen[] getContactTimeGenGroups () {
      return cgenGroups == null ? null : cgenGroups.clone ();
   }
   
   /**
    * Returns the random variate generator for contacts
    * served by agents in group \texttt{i}.
    * @param i the agent group index.
    * @return the contact time generator.
    */
   public RandomVariateGen getContactTimeGen (int i) {
      return cgenGroups == null || i >= cgenGroups.length ? null : cgenGroups[i];
   }
   
   /**
    * Sets the contact-time generators for
    * contacts served by specific agent groups to
    * \texttt{cgenGroups}. 
    * @param cgenGroups the new contact-time generators.
    */
   public void setContactTimeGenGroups (RandomVariateGen[] cgenGroups) {
      this.cgenGroups = cgenGroups == null ? null : cgenGroups.clone ();
   }

   /**
    * Returns the random variate generators for
    * after-contact times when served by agents in
    * specific groups.
    * @return the after-contact time generators.
    */
   public RandomVariateGen[] getAfterContactTimeGenGroups () {
      return acgenGroups == null ? null : acgenGroups.clone ();
   }
   
   /**
    * Returns the random variate generator for contacts
    * served by agents in group \texttt{i}.
    * @param i the agent group index.
    * @return the after-contact time generator.
    */
   public RandomVariateGen getAfterContactTimeGen (int i) {
      return acgenGroups == null || i >= acgenGroups.length ? null : acgenGroups[i];
   }

   /**
    * Sets the contact-time generators for
    * contacts served by specific agent groups to
    * \texttt{cgenGroups}. 
    * @param acgenGroups the new contact-time generators.
    */
   public void setAfterContactTimeGenGroups (RandomVariateGen[] acgenGroups) {
      this.acgenGroups = acgenGroups == null ? null : acgenGroups.clone ();
   }
   
   /**
    * Returns the mean contact time for a new
    * contact served by an agent in group \texttt{i}.
    * @param i the agent group identifier.
    * @return the mean contact time.
    */
   public double getMeanContactTime (int i) {
      RandomVariateGen gen = getContactTimeGen (i);
      if (gen != null)
         return gen.getDistribution ().getMean ();
      gen = getContactTimeGen();
      if (gen != null)
         return gen.getDistribution ().getMean ();
      return Double.POSITIVE_INFINITY;
   }

   /**
    * Returns the mean after-contact time for a new
    * contact served by an agent in group \texttt{i}.
    * @param i the agent group identifier.
    * @return the mean contact time.
    */
   public double getMeanAfterContactTime (int i) {
      RandomVariateGen gen = getAfterContactTimeGen (i);
      if (gen != null)
         return gen.getDistribution ().getMean ();
      gen = getAfterContactTimeGen();
      if (gen != null)
         return gen.getDistribution ().getMean ();
      return 0;
   }
   
   /**
    * Returns the type identifier for contacts returned
    * by this factory.
    * @return the type identifier of constructed contacts.
    */
   public int getTypeId () {
      return type;
   }
   
   /**
    * Sets the type identifier of constructed contacts
    * to \texttt{type}.
    * @param type the type identifier of constructed contacts.
    */
   public void setTypeId (int type) {
      this.type = type;
   }
}
