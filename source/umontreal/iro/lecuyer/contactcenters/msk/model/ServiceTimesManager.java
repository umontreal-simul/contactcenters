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
 * agent, to each agent group also as well as a default generator
 * used when no generator is available for a given agent or agent group.
 * This class associates a multiplier to each such service time which can
 * be used to alter the mean service time. One object of this class can be
 * constructed for each part of the service time, e.g., the
 * talk time, the transfer time, etc.
 */
public class ServiceTimesManager {
   private MultiPeriodGen sgen;
   private MultiPeriodGen[] sgenGroups; // for each group
   // Multiplier for the default service time generator
   private double sgenMult;
   // Multiplier applied to all generators.
   private double sgenMultAllGroups;
   // Multipliers for group-specific generators
   private double[] sgenMultGroups;
 
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
    * @param sgenMultAllGroups the multiplier
    * applied to all service time generators.
    * @param numGroups the number of agent groups.
    * @throws CallFactoryCreationException
    * if an error occurs during the construction of the
    * service time manager.
    */
   public ServiceTimesManager (CallCenter cc, String name,
      List <ServiceTimeParams> pars, int k, RandomStream sStream,
      double sgenMultAllGroups, int numGroups)
         throws CallFactoryCreationException {
      // numGroups required because call factories are constructed before
      // agent groups in CallCenter.create. At this time,
      // we do not have the information about agent groups.
      for (final ServiceTimeParams stg: pars)
         if (stg.isSetGroup ()) {
            final int i = stg.getGroup ();
            if (sgenGroups == null) {
               sgenGroups = new MultiPeriodGen[numGroups];
               sgenMultGroups = new double[numGroups];
            } else if (i >= sgenGroups.length) {
               sgenGroups = ArrayUtil.resizeArray (sgenGroups, i + 1);
               sgenMultGroups = ArrayUtil.resizeArray (sgenMultGroups, i + 1);
            }
            try {
               sgenMultGroups[i] = stg.getMult ();
               sgenGroups[i] = CCParamReadHelper.createGenerator
                               (stg, sStream, cc.getPeriodChangeEvent ());
               sgenGroups[i].setTargetTimeUnit (cc.getDefaultUnit ());
            } catch (final DistributionCreationException dce) {
               throw new CallFactoryCreationException
               ("Cannot create " + name + " distribution for call type " +
                k + " served by agents in group " + i, dce);
            } catch (final GeneratorCreationException gce) {
               throw new CallFactoryCreationException
                  ("Cannot create " + name + " generator for call type " + k +
                  " served by agents in group " + i, gce);
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
      this.sgenMultAllGroups = sgenMultAllGroups;
   }

   /**
    * Returns the default service time generator used
    * when no agent-group-specific service time is available.
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
    * group \texttt{i}.
    * If no such generator is available, this returns the
    * result of {@link #getServiceTimeGen()}.
    * @param i the tested agent group.
    * @return the associated service time generator.
    */
   public MultiPeriodGen getServiceTimeGen (int i) {
      if (sgenGroups == null)
         return sgen;
      if (i < sgenGroups.length)
         return sgenGroups[i] == null ? sgen : sgenGroups[i];
      return sgen;
   }

   /**
    * Returns an array containing the service time generators
    * for each agent group.
    * If no service time generator is associated with
    * an agent group,
    * the element at the corresponding position in the
    * returned array is \texttt{null}.
    * @return the array of service time generators.
    */
   public MultiPeriodGen[] getServiceTimeGenGroups () {
      return sgenGroups;
   }

   /**
    * Sets the service time generators to \texttt{sgenGroups}
    * for agent groups.
    * @param sgenGroups the new array of service time generators.
    */
   public void setServiceTimeGenGroups (MultiPeriodGen[] sgenGroups) {
      this.sgenGroups = sgenGroups;
   }

   /**
    * Sets the service time generator for
    * agent group \texttt{i} to \texttt{gen}.
    * @param i the index of the agent group.
    * @param gen the new generator.
    */
   public void setServiceTimeGen (int i, MultiPeriodGen gen) {
      if (gen == null)
         return;
      if (sgenGroups == null) {
         sgenGroups = new MultiPeriodGen[i + 1];
         sgenMultGroups = new double[i + 1];
         Arrays.fill (sgenMultGroups, 1);
      } else if (i >= sgenGroups.length) {
         sgenGroups = ArrayUtil.resizeArray (sgenGroups, i + 1);
         int size = sgenMultGroups.length;
         sgenMultGroups = ArrayUtil.resizeArray (sgenMultGroups, i + 1);
         for (int j = size; j < sgenMultGroups.length; j++)
            sgenMultGroups[j] = 1;
      }
      sgenGroups[i] = gen;
   }

   /**
    * Returns an array containing the multiplier for
    * each service time generator specific to an agent group.
    * @return the array of service time multipliers.
    */
   public double[] getServiceTimesGenGroupsMult () {
      return sgenMultGroups;
   }

   /**
    * Sets the service time multipliers for the agent groups
    * using the array \texttt{sgenMultGroups}.
    * @param sgenMultGroups the array giving the multipliers.
    */
   public void setServiceTimesGenGroupsMult (double[] sgenMultGroups) {
      this.sgenMultGroups = sgenMultGroups;
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
    * agent group \texttt{i}.
    * This returns 1 if no generator is associated with
    * specific agent groups.
    * @param i the tested agent group.
    * @return the multiplier.
    */
   public double getServiceTimesMult (int i) {
      if (sgenMultGroups == null)
         return 1;
      if (i < sgenMultGroups.length)
         return sgenMultGroups[i];
      return 1;
   }

   /**
    * Sets the service time multiplier specific
    * to agent group \texttt{i} to \texttt{mult}.
    * @param i the agent group identifier.
    * @param mult the new multiplier.
    */
   public void setServiceTimesMult (int i, double mult) {
      if (mult < 0)
         throw new IllegalArgumentException ();
      if (sgenMultGroups == null || i >= sgenMultGroups.length ||
            sgenGroups[i] == null) {
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
         setServiceTimeGen (i, sgen2);
      }
      sgenMultGroups[i] = mult;
   }

   /**
    * Returns the service time multiplier applied to
    * the default generator, as well as all generators
    * specific to agent groups.
    * @return the global service time multiplier.
    */
   public double getServiceTimesMultAllGroups () {
      return sgenMultAllGroups;
   }

   /**
    * Sets the global multiplier applied to each
    * service time generator managed by this
    * object to \texttt{sgenMultAllGroups}.
    * @param sgenMultAllGroups the new multiplier.
    */
   public void setServiceTimesMultAllGroups (double sgenMultAllGroups) {
      if (sgenMultAllGroups < 0)
         throw new IllegalArgumentException ();
      this.sgenMultAllGroups = sgenMultAllGroups;
   }

   /**
    * Initializes this manager by setting the
    * multipliers for the random variate generators.
    * The used multiplier is the product of
    * \texttt{mult}, the result of
    * {@link #getServiceTimesMultAllGroups()}, and
    * the generator-specific multiplier.
    * The value of \texttt{mult} corresponds to the
    * global service time multiplier applying to all call types.
    * @param mult the global multiplier.
    */
   public void init (double mult) {
      if (sgen != null)
         sgen.setMult (sgenMult * sgenMultAllGroups * mult);
      if (sgenGroups != null)
         for (int i = 0; i < sgenGroups.length; i++) {
            if (sgenGroups[i] != null)
               sgenGroups[i].setMult (sgenMultGroups[i] * sgenMultAllGroups *
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
      if (sgenGroups != null) {
         st.ensureCapacityForServiceTime (sgenGroups.length);
         for (int i = 0; i < sgenGroups.length; i++)
            if (sgenGroups[i] != null)
               st.setServiceTime (i, sgenGroups[i].nextDouble ());
      }
   }
}
