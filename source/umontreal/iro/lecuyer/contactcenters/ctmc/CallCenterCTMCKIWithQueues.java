package umontreal.iro.lecuyer.contactcenters.ctmc;


/**
 * Extends the 
 * CTMC model for multiple call types and agent groups
 * with information on queued calls.
 */
public class CallCenterCTMCKIWithQueues extends CallCenterCTMCKI implements
      CallCenterCTMCWithQueues {
   private CallCenterCTMCQueues queues;

   public CallCenterCTMCKIWithQueues (double[] lambda, double[] lambdaBound,
         double[][] mu, double[][] muBound, int[] numAgents,
         int[] maxNumAgents, double[] rho, double[] nu, double[] nuBound,
         int queueCapacity, int maxQueueCapacity, AgentGroupSelector[] ags,
         WaitingQueueSelector[] wqs, boolean needsInServiceContactMix,
         int[][] thresholds) {
      super (lambda, lambdaBound, mu, muBound, numAgents, maxNumAgents, rho, nu,
            nuBound, queueCapacity, maxQueueCapacity, ags, wqs,
            needsInServiceContactMix, thresholds);
      queues = new CallCenterCTMCQueues (this);
   }

   public int getLastWaitingTime (int k) {
      return queues.getLastWaitingTime (k);
   }

   public int getLongestWaitingTime (int k) {
      CircularIntArray queue = queues.getQueue (k);
      if (queue.size() == 0)
         return 0;
      return getNumTransitionsDone() - queue.get (0);
   }
   
   @Override
   public void init (CallCenterCTMC ctmc) {
      super.init (ctmc);
      if (ctmc instanceof CallCenterCTMCKIWithQueues) {
         CallCenterCTMCKIWithQueues ctmc1 = (CallCenterCTMCKIWithQueues)ctmc;
         queues.init (ctmc1.queues);
      }
      else
         throw new IllegalArgumentException();
   }

   @Override
   public void initEmpty () {
      super.initEmpty ();
      queues.init();
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
   public TransitionType nextState (double u) {
      TransitionType type = super.nextState (u);
      queues.update (this, type);
      return type;
   }

   @Override
   public TransitionType nextStateInt (int rv) {
      TransitionType type = super.nextStateInt (rv);
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
   public void generateArrival (int k, int np, int nf) {
      super.generateArrival (k, np, nf);
      queues.update (this, TransitionType.ARRIVALBALKED);
   }
   
   @Override
   public void generateEndService (int k, int i, int kp, int np, int nf) {
      super.generateEndService (k, i, kp, np, nf);
      queues.update (this, TransitionType.ENDSERVICEANDDEQUEUE);
   }

   @Override
   public CallCenterCTMCKIWithQueues clone () {
      CallCenterCTMCKIWithQueues cpy = (CallCenterCTMCKIWithQueues)super.clone();
      cpy.queues = queues.clone();
      return cpy;
   }
}
