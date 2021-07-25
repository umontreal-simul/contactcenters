package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ContactFactory;
import umontreal.iro.lecuyer.contactcenters.contact.RandomTypeContactFactory;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * This class is similar to {@link RandomTypeContactFactory},
 * but it allows the probability of generating each contact type to
 * change from periods to periods, and possibly depends on the
 * presence of agents in groups.
 * More specifically, the factory contains
 * a $K\times P$ 2D array giving a weight
 * $p_{k,p}$ to each call type~$k$
 * and main period~$p$.
 * Each time a call is requested, the current main period is determined,
 * and a weight is assigned to each call type.
 * If the selection takes account of the presence of agents,
 * weights corresponding to call types for which no agent is available
 * are reset to 0.
 * The weights are then summed up, and normalized to give
 * probabilities which are used to select a call type.
 */
public class RandomTypeCallFactory implements ContactFactory {
   private CallCenter cc;
   private ContactFactory[] factories;
   private double[][] probMainPeriod;
   private RandomStream stream;
   private double[] sumMainPeriod;
   private boolean checkAgents = false;
   private double[] temp;

   /**
    * Constructs a new random-type call factory
    * using period-change event associated with \texttt{cc} to
    * obtain the current main period,
    * and random stream \texttt{stream} to
    * generate random numbers.
    * The probabilities of selection 
    * $p_{k,p}$ are initialized using
    * the given \texttt{probMainPeriod} $K\times P$ 2D array as follows.
    * For each factory \texttt{k},
    * $p_{k, p}=0$ for $p=1,\ldots,P$ if
    * \texttt{probMainPeriod[k]} is \texttt{null}
    * or has length 0.
    * The probability $p_{k, p}=q$ for $p=1,\ldots,P$
    * if \texttt{probMainPeriod[k]} has a single element $q$.
    * Otherwise, $p_{k, p}$ is given by
    * \texttt{probMainPeriod[k][p]}.
    * @param cc the call center object.
    * @param probMainPeriod the main period and call factory specific probabilities.
    * @param stream the random stream used to generate random numbers.
    * @param checkAgents determines if the call factory
    * checks that there are agents capable of serving the
    * call before producing a call of a given type.
    * @exception NullPointerException if any argument is \texttt{null}.
    * @exception IllegalArgumentException if the lengths of
    * \texttt{factories} and \texttt{probMainPeriod} are different. 
    */
   public RandomTypeCallFactory (CallCenter cc, double[][] probMainPeriod, RandomStream stream, boolean checkAgents) {
      this.checkAgents = checkAgents;
      final ContactFactory[] factories1 = cc.getCallFactories ();
      if (factories1.length != probMainPeriod.length)
         throw new IllegalArgumentException
         ("An array of probabilities is required for each contact factory");
      final PeriodChangeEvent pce = cc.getPeriodChangeEvent ();
      final int P = pce.getNumMainPeriods ();
      this.cc = cc;
      this.factories = factories1;
      this.stream = stream;

      this.probMainPeriod = new double[factories1.length][P];
      for (int k = 0; k < factories1.length; k++) {
         if (factories1[k] == null)
            throw new NullPointerException ("Contact factories must not be null");
         if (probMainPeriod[k] == null || probMainPeriod[k].length == 0)
            Arrays.fill (this.probMainPeriod[k], 0);
         else if (probMainPeriod[k].length == 1)
            Arrays.fill (this.probMainPeriod[k], probMainPeriod[k][0]);
         else {
            if (probMainPeriod[k].length < P)
               throw new IllegalArgumentException
               ("A probability is required for each main period");
            System.arraycopy (probMainPeriod[k], 0, this.probMainPeriod[k], 0, P);
         }
      }

      sumMainPeriod = new double[P];
      for (final double[] element : this.probMainPeriod)
         for (int mp = 0; mp < P; mp++)
            sumMainPeriod[mp] += element[mp];
      temp = new double[probMainPeriod.length];
   }

   /**
    * Generates and returns a new type identifier.
    */
   public int nextIndex() {
      final int cp = cc.getPeriodChangeEvent ().getCurrentMainPeriod ();
      double sum;
      if (checkAgents) {
         sum = 0;
         final int I = cc.getNumAgentGroups ();
         for (int k = 0; k < probMainPeriod.length; k++) {
            temp[k] = 0;
            if (probMainPeriod[k][cp] == 0)
               continue;
            for (int i = 0; i < I && temp[k] == 0; i++)
               if (cc.getRouter ().canServe (i, k) && cc.getAgentGroup (i).getNumAgents () > 0)
                  temp[k] = probMainPeriod[k][cp];
            sum += temp[k];
         }
      }
      else {
         sum = sumMainPeriod[cp];
         for (int k = 0; k < probMainPeriod.length; k++)
            temp[k] = probMainPeriod[k][cp];
      }
      if (sum == 0)
         throw new IllegalStateException
         ("Cannot create calls during main period " + cp);
      double u = stream.nextDouble ();
      for (int k = 0; k < factories.length; k++) {
         final double prob = temp[k] / sum;
         if (u <= prob)
            return k;
         u -= prob;
      }
      throw new AssertionError();
   }

   public Contact newInstance () {
      return factories[nextIndex()].newInstance ();
   }

   /**
    * Returns a copy of the $K\times P$ 2D array giving
    * the values of $p_{k,p}$.
    */
   public double[][] getProbPeriod () {
      return ArrayUtil.deepClone (probMainPeriod);
   }

   /**
    * Returns the value of $p_{k,p}$.
    */
   public double getProbPeriod (int k, int p) {
      return probMainPeriod[k][p];
   }

   /**
    * Returns the random stream used by this factory.
    */
   public RandomStream getStream() {
      return stream;
   }
}
