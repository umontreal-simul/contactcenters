package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.rng.RandomStream;

/**
 * Represents a contact factory that can
 * create contacts of random types.
 * Any instance of this class encapsulates an array of contact factories,
 * and a probability of selection for each factory.
 * Each time a new contact is needed, an internal factory
 * is selected randomly based on
 * the selection probabilities, and used to instantiate the contact.
 */
public class RandomTypeContactFactory implements ContactFactory {
   private ContactFactory[] factories;
   private double[] prob;
   private DiscreteDistribution dist;
   private RandomStream stream;

   /**
    * Constructs a random-type contact factory selecting
    * contact factories from the array \texttt{factories},
    * with probabilities given by \texttt{prob}, and using
    * the random stream \texttt{stream}.
    * Contact factory \texttt{factories[k]}
    * is selected with
    * probability \texttt{prob[k]}, for \texttt{k=0,...,factories.length-1}.
    * If the values in \texttt{prob} do not sum to 1,
    * they are normalized by dividing by their sum.
    * @param factories the array of contact factories.
    * @param prob the array of probabilities of selection.
    * @param stream the random stream.
    * @exception NullPointerException if any of the above argument is \texttt{null},
    * or if at least one given contact factory is \texttt{null}.
    * @exception IllegalArgumentException if \texttt{factories} and
    * \texttt{prob} do not share the same length, or if
    * \texttt{prob} contains at least one negative value.
    */
   public RandomTypeContactFactory (ContactFactory[] factories, double[] prob, RandomStream stream) {
      if (stream == null)
         throw new NullPointerException();
      this.stream = stream;
      if (factories.length != prob.length)
         throw new IllegalArgumentException
         ("factories and prob must share the same length");
      this.factories = factories.clone ();
      final double[] obs = new double[prob.length];
      final double[] checkedProb = new double[prob.length];
      double sum = 0;
      for (int i = 0; i < prob.length; i++) {
         if (factories[i] == null)
            throw new NullPointerException
            ("factories[" + i + "] is null");
         if (prob[i] < 0)
            throw new IllegalArgumentException
            ("prob[" + i + "] is negative");
         sum += prob[i];
         obs[i] = i;
      }
      for (int i = 0; i < prob.length; i++)
         checkedProb[i] = prob[i] / sum;
      this.prob = checkedProb;
      dist = new DiscreteDistribution (obs, prob, obs.length);
   }

   /**
    * Returns an array giving each internal contact factory that
    * can be selected.
    * @return the array of contact factories.
    */
   public ContactFactory[] getContactFactories() {
      return factories.clone ();
   }

   /**
    * Returns an array giving the probability of selection for
    * each internal contact factory.
    * @return the array containing probabilities of selection.
    */
   public double[] getProbabilities() {
      return prob.clone ();
   }

   /**
    * Returns the random stream used for performing the selection.
    * @return the random stream for selection.
    */
   public RandomStream getStream() {
      return stream;
   }

   /**
    * Sets the random stream for performing further selections
    * to \texttt{stream}.
    * @param stream the new random stream for selection.
    * @exception NullPointerException if \texttt{stream} is \texttt{null}.
    */
   public void setStream (RandomStream stream) {
      if (stream == null)
         throw new NullPointerException();
      this.stream = stream;
   }

   public Contact newInstance () {
      final int k = (int)dist.inverseF (stream.nextDouble ());
      return factories[k].newInstance ();
   }
}
