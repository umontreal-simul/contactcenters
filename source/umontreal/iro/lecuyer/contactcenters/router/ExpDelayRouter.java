package umontreal.iro.lecuyer.contactcenters.router;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.expdelay.LastWaitingTimePerQueuePredictor;
import umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.server.Agent;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Represents a router using the expected delay to assign
 * agent groups to new contacts.
 * When a contact is routed to an agent group,
 * it is assigned a free agent of this particular group.
 * If all agents in the target group are busy, the contact
 * enters a waiting queue specific to the target agent group.
 * The contact cannot move across waiting queues.
 *
 * A waiting queue is associated with each
 * agent group~$i$.
 * When a new contact of type~$k$ arrives,
 * the router uses the weighted expected delays
 * $E_i(t)/\wTG(k, i)$ for each waiting queue to
 * take its decisions.
 * Here, $E_i(t)$ is a prediction of the waiting time
 * for the new contact arrived at time $t$
 * if sent to queue $i$ while
 * $\wTG(k, i)$ is a user-defined constant weight
 * determining the importance of contacts of type $k$
 * for agents in group $i$.
 * Two decision modes are available: deterministic, or stochastic.
 * In deterministic mode, the router chooses the
 * agent group with the minimal weighted expected delay.
 * In stochastic mode, the router chooses agent group
 * $i$ with probability
 * \[p_i(t) = \frac{\wTG(k,i)/E_i(t)}{\sum_{j=0}^{I-1} \wTG(k,j)/E_i(t)}\]
 * independently of the other contacts.
 * With this formula, the smaller is the weighted expected delay for
 * an agent group $i$, the higher is the probability of selection
 * of group $i$.
 * When an agent becomes free, it picks up a new contact from
 * its associated waiting queue only.
 *
 * Note that the routing of a contact of type~$k$ to an agent
 * in group~$i$ can be prevented by
 * fixing $\wTG(k,i)=0$.
 * Increasing $\wTG(k,i)$ increases the probability of
 * a contact of type~$k$ to be routed to an agent
 * in group~$i$.
 *
 * The expected delay is estimated using a
 * waiting time predictor.
 * The default predictor is the
 * {@link LastWaitingTimePerQueuePredictor} which
 * predicts the waiting time using the last observed
 * waiting time before a service.
 */
public class ExpDelayRouter extends Router {
   private double[][] weightsTG;
   private RandomStream streamAgentSelection;
   private WaitingTimePredictor pred;
   private double[] expDelays;

   /**
    * Constructs a new router using expected delays, with
    * a weights matrix \texttt{weightsTG},
    * a random stream \texttt{stream}.
    * The $K\times I$ weights matrix is used to determine
    * the number of contact types and agent groups while
    * \texttt{stream} determines the mode of the router.
    * If \texttt{stream} is \texttt{null}, the router is
    * in deterministic mode.  Otherwise, it is
    * in stochastic mode.
    * @param weightsTG the weights matrix.
    * @param stream the random stream used in stochastic mode.
    */
   public ExpDelayRouter (double[][] weightsTG, RandomStream stream) {
      this (weightsTG, stream, new LastWaitingTimePerQueuePredictor());
   }

   /**
    * Equivalent to {@link #ExpDelayRouter(double[][],RandomStream)}
    * with a user-defined waiting time predictor \texttt{pred}.
    * @param weightsTG the weights matrix.
    * @param stream the random stream used in stochastic mode.
    * @param pred the waiting time predictor.
    */
   public ExpDelayRouter (double[][] weightsTG, RandomStream stream, WaitingTimePredictor pred) {
      super (weightsTG.length, weightsTG[0].length, weightsTG[0].length);
      if (pred != null)
         throw new NullPointerException
         ("Null waiting time predictor specified");
      ArrayUtil.checkRectangularMatrix (weightsTG);
      this.weightsTG = ArrayUtil.deepClone (weightsTG);
      streamAgentSelection = stream;
      expDelays = new double[weightsTG[0].length];
      this.pred = pred;
   }

   @Override
   public WaitingQueueType getWaitingQueueType () {
      return WaitingQueueType.AGENTGROUP;
   }

   /**
    * Returns the weights matrix defining $\wTG(k, i)$.
    *
    * @return the weights matrix defining $\wTG(k, i)$.
    */
   public double[][] getWeightsTG() {
      return ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Sets the weights matrix defining $\wTG(k, i)$ to \texttt{weightsTG}.
    *
    * @param weightsTG
    *           the new weights matrix defining $\wTG(k, i)$.
    * @exception NullPointerException
    *               if \texttt{weightsTG} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{weightsTG} is not rectangular or has wrong
    *               dimensions.
    */
   public void setWeightsTG (double[][] weightsTG) {
      ArrayUtil.checkRectangularMatrix (weightsTG);
      if (weightsTG.length != getNumContactTypes() ||
            weightsTG[0].length != getNumAgentGroups())
         throw new IllegalArgumentException ("Invalid dimensions of weightsTG");
      this.weightsTG = ArrayUtil.deepClone (weightsTG, true);
   }

   /**
    * Returns the random stream used for agent selection. If the agent selection
    * is not randomized, this returns \texttt{null}.
    * @return the random stream for agent selection.
    */
   public RandomStream getStreamAgentSelection () {
      return streamAgentSelection;
   }

   /**
    * Sets the random stream for agent selection to
    * \texttt{streamAgentSelection}.
    * Setting the stream to \texttt{null} disables randomized
    * agent selection.
    *
    * @param streamAgentSelection
    *           the new random stream for agent selection.
    */
   public void setStreamAgentSelection (RandomStream streamAgentSelection) {
      this.streamAgentSelection = streamAgentSelection;
   }

   @Override
   public void init() {
      super.init();
      if (pred.getRouter() != this)
         pred.setRouter (this);
      pred.init();
   }

   @Override
   protected void checkWaitingQueues (AgentGroup group) {
      if (group == null)
         return;
      if (group.getNumAgents () == 0) {
         final WaitingQueue queue = getWaitingQueue (group.getId ());
         if (queue == null)
            return;
         queue.clear (DEQUEUETYPE_NOAGENT);
      }
   }

   @Override
   public boolean canServe (int i, int k) {
      return weightsTG[k][i] > 0;
   }

   @Override
   protected EndServiceEvent selectAgent (Contact contact) {
      final int k = contact.getTypeId ();
      final int I = getNumAgentGroups ();
      int bestQueue = -1;
      double smallestDelay = Double.POSITIVE_INFINITY;
      for (int i = 0; i < I; i++) {
         final double w = weightsTG[k][i];
         if (Double.isNaN (w) || w <= 0) {
            expDelays[i] = Double.NaN;
            continue;
         }
         expDelays[i] = pred.getWaitingTime (contact, getWaitingQueue (i)) / w;
         if (expDelays[i] < smallestDelay) {
            smallestDelay = expDelays[i];
            bestQueue = i;
         }
      }
      if (bestQueue == -1)
         return null;
      int bestQueueRand;
      if (streamAgentSelection == null)
         bestQueueRand = bestQueue;
      else {
         double sum = 0;
         for (int i = 0; i < expDelays.length; i++) {
            if (Double.isNaN (expDelays[i]))
               expDelays[i] = 0;
            else
               expDelays[i] = 1 / expDelays[i];
            sum += expDelays[i];
         }
         if (sum == 0)
            return null;
         if (Double.isInfinite (sum))
            bestQueueRand = bestQueue;
         else {
            for (int i = 0; i < expDelays.length; i++)
               expDelays[i] /= sum;

            double u = streamAgentSelection.nextDouble ();
            bestQueueRand = 0;
            while (u > expDelays[bestQueueRand]) {
               u -= expDelays[bestQueueRand];
               ++bestQueueRand;
            }
         }
      }

      final AgentGroup group = getAgentGroup (bestQueueRand);
      if (group.getNumFreeAgents () > 0)
         return group.serve (contact);
      else
         contact.getAttributes ().put (this, bestQueueRand);
      return null;
   }

   @Override
   protected DequeueEvent selectWaitingQueue (Contact contact) {
      final Integer queueIndex = (Integer)contact.getAttributes ().get (this);
      if (queueIndex == null)
         return null;
      final WaitingQueue queue = getWaitingQueue (queueIndex);
      if (queue == null)
         return null;
      return queue.add (contact);
   }

   @Override
   protected DequeueEvent selectContact (AgentGroup group, Agent agent) {
      final WaitingQueue queue = getWaitingQueue (group.getId ());
      if (queue == null || queue.isEmpty ())
         return null;
      return queue.removeFirst (DEQUEUETYPE_BEGINSERVICE);
   }

   @Override
   public String getDescription() {
      return "Expected delay router";
   }

   @Override
   public String toLongString() {
      final StringBuilder sb = new StringBuilder (super.toLongString ());
      sb.append ('\n');
      sb.append ("Weights matrix for agent selection\n");
      sb.append (RoutingTableUtils.formatWeightsTG (weightsTG)).append ("\n");
      return sb.toString();
   }
}
