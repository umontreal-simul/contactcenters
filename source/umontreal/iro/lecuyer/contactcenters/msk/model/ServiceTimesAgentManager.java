package umontreal.iro.lecuyer.contactcenters.msk.model;
import java.util.Arrays;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.CCParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.MultiPeriodGen;
import umontreal.iro.lecuyer.contactcenters.contact.ServiceTimes;
import umontreal.iro.lecuyer.contactcenters.msk.params.ServiceTimeParams;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;


/**
 * Manages the construction of service time generators specific to each
 * agent, as well as a default generator
 * used when no generator is available for a given agent.
 * This class associates a multiplier to each such service time which can
 * be used to alter the mean service time. One object of this class can be
 * constructed for each part of the service time, e.g., the
 * talk time, the transfer time, etc.
 */
public class ServiceTimesAgentManager {
   private MultiPeriodGen sgen;
   private MultiPeriodGen[] sgenAgents;    // for each agent
   // Multiplier for the default service time generator
   private double sgenMult;
   // Multiplier applied to all generators
   private double sgenMultAllAgents;
   // Multipliers for agent-specific generators
   private double[] sgenMultAgents;

   /**
    * Constructs a new service times manager
    * using call center parameters \texttt{cc}.
    * This method uses the given list of service time
    * parameters \texttt{pars}, and the
    * stream \texttt{sStream} to construct service
    * time generators.
    * @param cc the call center model.
    * @param name the name of the part of the
    * service time this object concerns, used
    * in error messages.
    * @param pars the service time parameters.
    * @param k the concerned call type.
    * @param sStream the random stream used to generate
    * the service times.
    * @param sgenMultAllAgents the multiplier
    * applied to all service time generators.
    * @param numAgents the number of agents.
    * @throws CallFactoryCreationException
    * if an error occurs during the construction of the
    * service time manager.
    */
   public ServiceTimesAgentManager (CallCenter cc, String name,
      List <ServiceTimeParams> pars, int k, RandomStream sStream,
      double sgenMultAllAgents, int numAgents)
         throws CallFactoryCreationException {
      // numAgents required because call factories are constructed before
      // agents in CallCenter.create. At this time,
      // we do not have the information about agents.
      for (final ServiceTimeParams stg: pars)
         if (stg.isSetAgent ()) {
            final int j = stg.getAgent ();
            if (sgenAgents == null) {
               sgenAgents = new MultiPeriodGen[numAgents];
               sgenMultAgents = new double[numAgents];
            } else if (j >= sgenAgents.length) {
               sgenAgents = ArrayUtil.resizeArray (sgenAgents, j + 1);
               sgenMultAgents = ArrayUtil.resizeArray (sgenMultAgents, j + 1);
            }
            try {
               sgenMultAgents[j] = stg.getMult ();
               sgenAgents[j] = CCParamReadHelper.createGenerator
                               (stg, sStream, cc.getPeriodChangeEvent ());
               sgenAgents[j].setTargetTimeUnit (cc.getDefaultUnit ());
            } catch (final DistributionCreationException dce) {
               throw new CallFactoryCreationException
               ("Cannot create " + name + " distribution for call type " +
                k + " served by agents in agent " + j, dce);
            } catch (final GeneratorCreationException gce) {
               throw new CallFactoryCreationException
                  ("Cannot create " + name + " generator for call type " + k +
                  " served by agents in agent " + j, gce);
            }
         } else
            try {
               sgenMult = stg.getMult ();
               sgen = CCParamReadHelper.createGenerator
                      (stg, sStream, cc.getPeriodChangeEvent ());
               sgen.setTargetTimeUnit (cc.getDefaultUnit ());
            } catch (final DistributionCreationException dce) {
               throw new CallFactoryCreationException
               ("Cannot create " + name + " distribution", dce);
            } catch (final GeneratorCreationException gce) {
               throw new CallFactoryCreationException
               ("Cannot create " + name + " generator", gce);
            }
      this.sgenMultAllAgents = sgenMultAllAgents;
   }


   /**
    * Returns the default service time generator used
    * when no agent-agent-specific service time is available.
    * @return the default service time generator.
    */
   public MultiPeriodGen getServiceTimeGen () {
      return sgen;
   }

   /**
    * Sets the default service time generator to
    * \texttt{sgen}.
    * @param sgen the new default service time generator.
    */
   public void setServiceTimeGen (MultiPeriodGen sgen) {
      this.sgen = sgen;
   }

   /**
    * Returns the service time generator for agent
    * agent \texttt{i}.
    * If no such generator is available, this returns the
    * result of {@link #getServiceTimeGen()}.
    * @param j the tested agent.
    * @return the associated service time generator.
    */
   public MultiPeriodGen getServiceTimeGen (int j) {
      if (sgenAgents == null)
         return sgen;
      if (j < sgenAgents.length)
         return sgenAgents[j] == null ? sgen : sgenAgents[j];
      return sgen;
   }

   /**
    * Returns an array containing the service time generators
    * for each agent.
    * If no service time generator is associated with
    * an agent,
    * the element at the corresponding position in the
    * returned array is \texttt{null}.
    * @return the array of service time generators.
    */
   public MultiPeriodGen[] getServiceTimeGenAgents () {
      return sgenAgents;
   }

   /**
    * Sets the service time generators to \texttt{sgenAgents}
    * for agents.
    * @param sgenAgents the new array of service time generators.
    */
   public void setServiceTimeGenAgents (MultiPeriodGen[] sgenAgents) {
      this.sgenAgents = sgenAgents;
   }

   /**
    * Sets the service time generator for
    * agent \texttt{i} to \texttt{gen}.
    * @param j the index of the agent.
    * @param gen the new generator.
    */
   public void setServiceTimeGen (int j, MultiPeriodGen gen) {
      if (gen == null)
         return;
      if (sgenAgents == null) {
         sgenAgents = new MultiPeriodGen[j + 1];
         sgenMultAgents = new double[j + 1];
         Arrays.fill (sgenMultAgents, 1);
      } else if (j >= sgenAgents.length) {
         sgenAgents = ArrayUtil.resizeArray (sgenAgents, j + 1);
         int size = sgenMultAgents.length;
         sgenMultAgents = ArrayUtil.resizeArray (sgenMultAgents, j + 1);
         for (int s = size; s < sgenMultAgents.length; s++)
            sgenMultAgents[s] = 1;
      }
      sgenAgents[j] = gen;
   }

   /**
    * Returns an array containing the multiplier for
    * each service time generator specific to an agent.
    * @return the array of service time multipliers.
    */
   public double[] getServiceTimesGenAgentsMult () {
      return sgenMultAgents;
   }

   /**
    * Sets the service time multipliers for the agents
    * using the array \texttt{sgenMultAgents}.
    * @param sgenMultAgents the array giving the multipliers.
    */
   public void setServiceTimesGenAgentsMult (double[] sgenMultAgents) {
      this.sgenMultAgents = sgenMultAgents;
   }

   /**
    * Returns the multiplier applied to the default service
    * time generator.
    * @return the multiplier for the default service time generator.
    */
   public double getServiceTimesMult () {
      return sgenMult;
   }

   /**
    * Sets the multiplier for the default service time
    * generator to \texttt{sgenMult}.
    * @param sgenMult the multiplier for the default service time
    * multiplier.
    */
   public void setServiceTimesMult (double sgenMult) {
      if (sgenMult < 0)
         throw new IllegalArgumentException ();
      this.sgenMult = sgenMult;
   }

   /**
    * Returns the service time multiplier specific to
    * agent \texttt{i}.
    * This returns 1 if no generator is associated with
    * specific agents.
    * @param j the tested agent.
    * @return the multiplier.
    */
   public double getServiceTimesMult (int j) {
      if (sgenMultAgents == null)
         return 1;
      if (j < sgenMultAgents.length)
         return sgenMultAgents[j];
      return 1;
   }

   /**
    * Sets the service time multiplier specific
    * to agent \texttt{i} to \texttt{mult}.
    * @param j the agent identifier.
    * @param mult the new multiplier.
    */
   public void setServiceTimesMult (int j, double mult) {
      if (mult < 0)
         throw new IllegalArgumentException ();
      if (sgenMultAgents == null || j >= sgenMultAgents.length ||
            sgenAgents[j] == null) {
         if (mult == 1)
            return;
         MultiPeriodGen sgen2;
         if (sgen == null)
            sgen2 = null;
         else {
            // Make a copy of sgen to get a copy of the internal multiplier
            sgen2 = new MultiPeriodGen (sgen.getPeriodChangeEvent (),
                                        sgen.getGenerators ());
            sgen2.setSourceTimeUnit (sgen.getSourceTimeUnit ());
            sgen2.setTargetTimeUnit (sgen.getTargetTimeUnit ());
         }
         setServiceTimeGen (j, sgen2);
      }
      sgenMultAgents[j] = mult;
   }

   /**
    * Returns the service time multiplier applied to
    * the default generator, as well as all generators
    * specific to agents.
    * @return the global service time multiplier.
    */
   public double getServiceTimesMultAllAgents () {
      return sgenMultAllAgents;
   }

   /**
    * Sets the global multiplier applied to each
    * service time generator managed by this
    * object to \texttt{sgenMultAllAgents}.
    * @param sgenMultAllAgents the new multiplier.
    */
   public void setServiceTimesMultAllAgents (double sgenMultAllAgents) {
      if (sgenMultAllAgents < 0)
         throw new IllegalArgumentException ();
      this.sgenMultAllAgents = sgenMultAllAgents;
   }

   /**
    * Initializes this manager by setting the
    * multipliers for the random variate generators.
    * The used multiplier is the product of
    * \texttt{mult}, the result of
    * {@link #getServiceTimesMultAllAgents()}, and
    * the generator-specific multiplier.
    * The value of \texttt{mult} corresponds to the
    * global service time multiplier applying to all call types.
    * @param mult the global multiplier.
    */
   public void init (double mult) {
      if (sgen != null)
         sgen.setMult (sgenMult * sgenMultAllAgents * mult);
      if (sgenAgents != null)
         for (int j = 0; j < sgenAgents.length; j++) {
            if (sgenAgents[j] != null)
               sgenAgents[j].setMult (sgenMultAgents[j] * sgenMultAllAgents *
                                      mult);
         }
   }

   /**
    * Uses the random variate generators attached with this service times manager
    * to generate service times, and store the times in
    * \texttt{st}.
    * @param st the object holding service times.
    */
   public void generate (ServiceTimes st) {
      if (sgen != null)
         st.setServiceTime (sgen.nextDouble ());
      if (sgenAgents != null) {
         st.ensureCapacityForServiceTime (sgenAgents.length);
         for (int j = 0; j < sgenAgents.length; j++)
            if (sgenAgents[j] != null)
               st.setServiceTime (j, sgenAgents[j].nextDouble ());
      }
   }
}
