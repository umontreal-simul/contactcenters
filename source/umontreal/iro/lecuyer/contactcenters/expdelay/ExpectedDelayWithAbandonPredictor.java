package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
/*
 * Nous avons une Erlang A.
 * On a le modele M/M/s+M . Les arrive sont exponentielle, le temps service sont des exponentielle de parametre mu, les abandon sont aussi 
 * des exponentielle de parametre alpha *  Nous tenons des abandon pour estimer le delay d'attente d'un nouveau contact arrivant dans une file i.
 * Chaque type d'appel est asssocie a une file . la formule pour estimer le temps d'attente du nouveau arrivant est donne par la formule suivante:
 * (1/alpha)log(1+(alpha*n)/(s*mu) avec n la longueur d'une file donne et le nombre agent pour traiter les apples venat de la file i 
 * 
 * 
 */
public class ExpectedDelayWithAbandonPredictor extends ExpectedDelayPredictor
{
   private Router router;
   private double []alpha; //parametre pour les abandons
   public Router getRouter ()
   {
      return router;
   }

   public double getWaitingTime (Contact contact)
   {
      return Double.NaN;
   }

   public double getWaitingTime (Contact contact, WaitingQueue queue)
   {
      if (router == null)
         throw new IllegalStateException
         ("Not enough information to predict the waiting time; use setRouter to associate a router with this predictor");
      if (queue == null)
         throw new NullPointerException
         ("The given waiting queue must not be null");
      final int i = queue.getId ();
      if (router.getWaitingQueue (i) != queue)
         throw new IllegalStateException
         ("The given queue is not associated with the correct router");
      final AgentGroup group = router.getAgentGroup (i);
      if (group == null)
         throw new IllegalStateException
         ("No agent group associated with waiting queue " + i);
      if (group.getNumFreeAgents () > 0 && queue.isEmpty ())
         return 0;
      final int qs = queue.size ();
      final int n = group.getNumAgents ();
      final double delay = (1 / alpha[i]) * Math.log((1.0 + (alpha[i] * qs) / (n * super.getMu(i)) ) );
      //  final double delay = (qs + 1.0) / (n*mu[i]);
      //	      if (contact.getNumWaitingQueues () > 0) {
      //	         DequeueEvent ev = queue.getDequeueEvent (contact);
      //	         if (ev == null)
      //	            return delay;
      //	         double wt = ev.simulator ().time () - ev.getEnqueueTime ();
      //	         return Math.max (0, delay - wt);
      //	      }
      return delay;
   }

   public void init ()
{}

   public void setRouter (Router router)
   {
      if (router != null) {
         super.setRouter(router);
         alpha = new double[router.getNumWaitingQueues ()];
         Arrays.fill (alpha, 1);
         this.router = router;
         setAlphaWithContactTypes();
         setMuWithContactTypes();

      } else {
         alpha = null;
         this.router = router;
      }

   }



   public double getAlpha (int i)
   {
      return alpha[i];
   }

   public double[] getAlpha()
   {
      return alpha.clone ();
   }


   public void setAlpha (int i, double a)
   {
      if (a <= 0)
         throw new IllegalArgumentException
         ("the value of mu_i must be positive");
      alpha[i] = a;
   }

   public void setAlpha (double[] alpha)
   {
      if (alpha.length != this.alpha.length)
         throw new IllegalArgumentException
         ("Invalid length of alpha");
      for (int i = 0; i < alpha.length; i++)
         setMu (i, alpha[i]);
   }



   public void setAlphaWithContactTypes()
   {
      if (router.getNumContactTypes () != alpha.length)
         throw new IllegalArgumentException
         ("This method requires a contact type for each waiting queue");
      for (int k = 0; k < alpha.length; k++) {
         if (router.getContactFactory (k).getPatienceTimeGen() != null) {
            double c = router.getContactFactory (k).getPatienceTimeGen().getDistribution().getMean();
            alpha[k] = 1.0 / c;
         } else
            alpha[k] = 1;
      }
   }
}
