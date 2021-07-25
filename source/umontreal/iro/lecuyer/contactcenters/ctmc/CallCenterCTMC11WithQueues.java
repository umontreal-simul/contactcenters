package umontreal.iro.lecuyer.contactcenters.ctmc;

/**
 * Extension of the CTMC model for a single contact type and agent group, with
 * information on contacts waiting in queue.
 */
public class CallCenterCTMC11WithQueues extends CallCenterCTMC11 implements
      CallCenterCTMCWithQueues {
   private CallCenterCTMCQueues queues;

   public CallCenterCTMC11WithQueues (double lambda, double maxLambda,
         double mu, double maxMu, int numAgents, int maxNumAgents, double rho,
         double nu, double maxNu, int queueCapacity, int maxQueueCapacity,
         int[][] thresholds) {
      super (lambda, maxLambda, mu, maxMu, numAgents, maxNumAgents, rho, nu,
            maxNu, queueCapacity, maxQueueCapacity, thresholds);
      queues = new CallCenterCTMCQueues (this);
   }

   public int getLastWaitingTime (int k) {
      return queues.getLastWaitingTime (k);
   }

   public int getLongestWaitingTime (int k) {
      CircularIntArray queue = queues.getQueue (k);
      if (queue.size () == 0)
         return 0;
      return getNumTransitionsDone () - queue.get (0);
   }

   @Override
   public void initEmpty () {
      super.initEmpty ();
      queues.init ();
   }

   @Override
   public void init (CallCenterCTMC ctmc) {
      super.init (ctmc);
      if (ctmc instanceof CallCenterCTMC11WithQueues) {
         CallCenterCTMC11WithQueues ctmc1 = (CallCenterCTMC11WithQueues) ctmc;
         queues.init (ctmc1.queues);
      }
      else
         throw new IllegalArgumentException ();
   }

   @Override
   public boolean selectContact (int i) {
      if (super.selectContact (i)) {
         queues.update (this, TransitionType.ENDSERVICEANDDEQUEUE);
         return true;
      }
      return false;
   }

   @Override
   public TransitionType nextStateInt (int v) {
      TransitionType type = super.nextStateInt (v);
      queues.update (this, type);
      return type;
   }

   @Override
   public TransitionType nextState (double u) {
      TransitionType type = super.nextState (u);
      queues.update (this, type);
      return type;
   }

   @Override
   public void generateAbandonment (int k, int kpos, int np, int nf) {
      super.generateAbandonment (k, kpos, np, nf);
      queues.update (this, TransitionType.ABANDONMENT);
   }

   @Override
   public void generateArrivalQueued (int k, int np, int nf) {
      super.generateArrivalQueued (k, np, nf);
      queues.update (this, TransitionType.ARRIVALQUEUED);
   }

   @Override
   public void generateArrivalServed (int k, int i, int np, int nf) {
      super.generateArrivalServed (k, i, np, nf);
      queues.update (this, TransitionType.ARRIVALSERVED);
   }

   @Override
   public void generateEndService (int k, int i, int kp, int np, int nf) {
      super.generateEndService (k, i, kp, np, nf);
      queues.update (this, TransitionType.ENDSERVICEANDDEQUEUE);
   }

   @Override
   public void generateArrival (int k, int np, int nf) {
      super.generateArrival (k, np, nf);
      queues.update (this, TransitionType.ARRIVALBALKED);
   }

   @Override
   public CallCenterCTMC11WithQueues clone () {
      CallCenterCTMC11WithQueues cpy = (CallCenterCTMC11WithQueues) super
            .clone ();
      cpy.queues = queues.clone ();
      return cpy;
   }
}
