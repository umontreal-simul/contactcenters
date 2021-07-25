package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.expdelay.WaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.StandardWaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.expdelay.MeanNLastWaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.expdelay.MeanNLastWaitingTimePerQueuePredictor;
import umontreal.iro.lecuyer.contactcenters.expdelay.ExpectedDelayPredictorHQ;
/**
 * Implements the necessary logic for virtual holding, also called virtual
 * queueing.
 */
public class VirtualHoldManager
{
   private static final ValueGenerator infGen = new InfiniteValueGenerator();
   private CallCenter cc;
   private final QueueListener ql = new QueueListener();
   private final VQueueListener vql = new VQueueListener();
   private WaitingTimePredictor pred;
   private WaitingQueue[] virtualQueues;

   /**
    * Constructs a new virtual hold manager for the call center model
    * \texttt{cc}.
    */
   public VirtualHoldManager(CallCenter cc) throws CallCenterCreationException
   {
      this.cc = cc;
for (final WaitingQueue queue : cc.getWaitingQueues())
         queue.addWaitingQueueListener(ql);
      virtualQueues = new WaitingQueue[cc.getNumContactTypes()];
      for (int q = 0; q < virtualQueues.length; q++) {
         virtualQueues[q] = new StandardWaitingQueue();
         virtualQueues[q].setMaximalQueueTimeGenerator(1, infGen);
         virtualQueues[q].addWaitingQueueListener(vql);
      }

      try {
         pred = cc.getWaitingTimePredictorClass().newInstance(); //ici se trouve notre probleme pour instancier un classe avec constructeur qui des arguments
         if (pred instanceof MeanNLastWaitingTimePredictor)
         {
            double waitingTimes = cc.getCallCenterParams().getInboundTypes().get(0).getWaitingTimeDefaultVQ();
            //getInboundTypes() retourne une list de InboundTypeParam dont nous recuperons le waitingTimeDefaultVQ
            // du premier element ie le parametre du premmier inboundType du fichier xml

            int window = cc.getCallCenterParams().getInboundTypes().get(0).getWindowSizeVQ();
            ((MeanNLastWaitingTimePredictor) pred).setWaitingTimeDefaultVQ(waitingTimes) ;
            ((MeanNLastWaitingTimePredictor) pred).setWindowSizeVQ(window);
            ((MeanNLastWaitingTimePredictor) pred).init();

         }

         if (pred instanceof MeanNLastWaitingTimePerQueuePredictor ) {
            int w = cc.getCallCenterParams().getInboundTypes().size();
            double waitingTimes[] = new double[w];
            for (int i = 0;i < w;i++)
               waitingTimes[i] = cc.getCallCenterParams().getInboundTypes().get(i).getWaitingTimeDefaultVQ();
            int window[] = new int[w];
            for (int i = 0;i < w;i++)
               window[i] = cc.getCallCenterParams().getInboundTypes().get(i).getWindowSizeVQ();

            (( MeanNLastWaitingTimePerQueuePredictor) pred).setWaitingTimeDefaultVQ(waitingTimes) ;
            (( MeanNLastWaitingTimePerQueuePredictor) pred).setWindowSizeVQ(window);
            (( MeanNLastWaitingTimePerQueuePredictor) pred).init();

         }
         if (pred instanceof ExpectedDelayPredictorHQ ) {
            int w = cc.getCallCenterParams().getInboundTypes().size();
            double waitingTimes[] = new double[w];
            for (int i = 0;i < w;i++)
               waitingTimes[i] = cc.getCallCenterParams().getInboundTypes().get(i).getWaitingTimeDefaultVQ();
            int window[] = new int[w];
            for (int i = 0;i < w;i++)
               window[i] = cc.getCallCenterParams().getInboundTypes().get(i).getWindowSizeVQ();

            (( ExpectedDelayPredictorHQ) pred).setWaitingTimeDefaultVQ(waitingTimes) ;
            (( ExpectedDelayPredictorHQ) pred).setWindowSizeVQ(window);
            (( ExpectedDelayPredictorHQ) pred).init();

         }

      } catch (final IllegalAccessException iae) {
         throw new CallCenterCreationException(
            "Cannot create waiting time predictor", iae);
      } catch (final InstantiationException ie) {
         throw new CallCenterCreationException(
            "Cannot create waiting time predictor", ie);
      }
   }

   /**
    * Initializes the internal variables of this manager for a new simulation.
    */
   public void init()
   {
      if (pred.getRouter() != cc.getRouter())
         pred.setRouter(cc.getRouter());
for (final WaitingQueue queue : virtualQueues)
         queue.init();
   }

   private class QueueListener implements WaitingQueueListener
   {
      public void dequeued(DequeueEvent ev)
      {}

      public void enqueued(DequeueEvent ev)
      {
         if (ev.getScheduledQueueTime() <= 0)
            // No processing for calls abandoning immediately
            return ;
         final Call call = (Call) ev.getContact();
         final int k = call.getTypeId();
         final int p = call.getArrivalPeriod();
         final int mp = cc.getPeriodChangeEvent().getMainPeriod(p);
         final CallFactory factory = cc.getCallFactory(k);
         final int k2 = factory.getTargetVQType();
         if (k2 < 0) {
            final int k1 = call.getTypeBeforeVQ();
            if (k1 < 0)
               // Ordinary call
               return ;
            // Call joining regular queue after leaving virtual queue
            final CallFactory factory1 = cc.getCallFactory(k1);
            // Called back customer, multiply patience time by a factor.
            if (ev.time() > 0) {
               // Correct time to abandon to apply
               // the patience time multiplier
               final double pmult = factory1.getPatienceTimesMultCallBack(mp);
               if (Double.isInfinite(pmult))
                  ev.cancel();
               else if (pmult != 1) {
                  final double time = ev.time() - ev.simulator().time();
                  ev.reschedule(time * pmult); //Programme l"heure de la suppresion s'il n'est pas traiter avant.
               }
            }
            // Alter service times for call back
            factory1.multiplyServiceTimesCallBack(call);
            return ;
         }
         if (call.getWaitingTimeVQ() > 0)
            return ;
         // Call entering the waiting queue
         final double thresh = factory.getExpectedWaitingTimeThresh(mp);
         if (Double.isInfinite(thresh))
            // Virtual queueing disabled for this call
            return ;
         final double wt = pred.getWaitingTime(ev.getContact(),
                                               ev.getWaitingQueue());
         if (wt < thresh)
            // Too small predicted waiting time, no
            // option offered to the customer.
            return ;
         // The customer is offered the possibility to join the virtual queue.
         final double prob = factory.getProbVirtualQueue(mp);
         if (call.getUVQ() > prob) {
            // The customer chooses not to be
            // called back, so we multiply its
            // patience time by a user-defined factor.
            if (ev.time() > 0) {
               final double mult = factory
                                   .getPatienceTimesMultNoVirtualQueue(mp);
               final double time = ev.time() - ev.simulator().time();
               if (Double.isInfinite(mult))
                  ev.cancel();
               else if (mult != 1)
                  ev.reschedule(time * mult);
            }
            factory.multiplyServiceTimesNoVirtualQueue(call);
            return ;
         }

         // Transfer the customer to the virtual queue
         // Remove the call from the original queue
         ev.remove(Router.DEQUEUETYPE_TRANSFER);
         cc.getRouter().exitDequeued(ev);
         // Change the call type for statistical collecting
         call.setTypeBeforeVQ(k);
         call.setTypeId(k2);
         // Add the call to the virtual queue
         final DequeueEvent ev2 = virtualQueues[k2].add(call);
         final double mult = factory.getExpectedWaitingTimeMult(mp);
         if (Double.isInfinite(mult))
            throw new IllegalStateException(
               "Infinite expected waiting time multiplier not accepted");
         ev2.schedule(wt*mult / 10); //remplacer par la ligne suivante
         //ev2.schedule(5);
      }

      public void init(WaitingQueue queue)
   {}
   }

   private class VQueueListener implements WaitingQueueListener
   {
      public void dequeued(DequeueEvent ev)
      {
         // A call exits from the virtual queue
         final Call call = (Call) ev.getContact();
         final int k = call.getTypeId();
         final int p = call.getArrivalPeriod();
         final int mp = cc.getPeriodChangeEvent().getMainPeriod(p);
         call.addToTotalQueueTime( -ev.getEffectiveQueueTime());
         call.setWaitingTimeVQ(ev.getEffectiveQueueTime());
         final CallFactory factory = cc.getCallFactory(k);
         final double prob = factory.getProbVirtualQueueCallBack(mp);
         if (call.getUVQCallBack() > prob) {
            //cc.getRouter().notifyBlocked(call, k); // remplacer par les 4 ligne suivante
            call.setDefaultPatienceTime(0);
            call.setExited(false);
            call.setRouter(null);
            cc.getRouter().newContact(call);

            return ;
         }
         call.setExited(false);
         call.setRouter(null);
         call.setDefaultPatienceTime(Double.POSITIVE_INFINITY); //ajouter
         cc.getRouter().newContact(call);
      }

      public void enqueued(DequeueEvent ev)
      {}

      public void init(WaitingQueue queue)
      {}
   }

   private static class InfiniteValueGenerator implements ValueGenerator
   {
      public void init()
      {}

      public double nextDouble(Contact contact)
      {
         return Double.POSITIVE_INFINITY;
      }
   }
}
