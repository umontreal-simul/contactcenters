package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.ArrayList;
//import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
//import umontreal.iro.lecuyer.contactcenters.expdelay.MeanNLastWaitingTimePerQueuePredictor.QueueListener;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;

public class ExpectedDelayPredictorHQ implements WaitingTimePredictor {
	private final QueueListener ql = new QueueListener();
	private double waitingTimePredictor[];
	private ArrayList<Double> list[];
	private Router router;
	private int[] windowSizeVQ; // ajouter
	private double[] waitingTimeDefaultVQ; // ajouter

	// private final ArrayList<Double> list= new ArrayList<Double>(); //ajouter
	private boolean collectingAbandonment = false;
	private boolean collectingService = true;

	public void setWindowSizeVQ(int[] taille) {
		windowSizeVQ = taille;
	}

	// Ajout de la methode qui modifie la taille de la fenetre

	public void setWaitingTimeDefaultVQ(double attente[]) {
		waitingTimeDefaultVQ = attente;
	}

	// modifier
	public double getWaitingTime(Contact contact, WaitingQueue queue) {
		double somme = 0;
		final int q = queue.getId();
		if (list != null) {
			for (Double d : list[q])
				somme += d;
			waitingTimePredictor[q] = somme / windowSizeVQ[q];
		}

		return waitingTimePredictor[q];

	}

	public double getWaitingTime(Contact contact) {
		int q = contact.getTypeId();
		return waitingTimePredictor[q];
	}

	// Ajout de la methdoe update pour mettre a jour le Arraylist des N dernier
	// temps de service

	public double updateWaitingTime(DequeueEvent ev) {
		int q = ev.getWaitingQueue().getId();
		if (list != null) {
			list[q].remove(0);
			// double d=(Double)(ev.getEffectiveQueueTime ());
			double d = (Double) (ev.getContact().getTotalServiceTime());
			list[q].add(d);
		}
		return getWaitingTime(ev.getContact(), ev.getWaitingQueue());

	}

	// modifier pour initialiser le WaitingTime
	public void init() {
		if (router != null) {
			int Q = router.getNumWaitingQueues();
			if (Q != 0) {
				waitingTimePredictor = new double[Q];
				windowSizeVQ = new int[Q];
				waitingTimeDefaultVQ = new double[Q];
				for (int q = 0; q < Q; q++) {
					list[q] = new ArrayList<Double>();
					for (int i = 0; i < windowSizeVQ[q] - 1; i++)
						list[q].add(waitingTimeDefaultVQ[q]);
				}
			}
		}
	}

	public Router getRouter() {
		return router;
	}

	public void setRouter(Router newRouter) {
		if (router != null && newRouter != router) {
			final int nq = router.getNumWaitingQueues();
			if ((waitingTimePredictor == null) || (waitingTimeDefaultVQ == null)) // ajout
			{
				waitingTimePredictor = new double[nq];
				waitingTimeDefaultVQ = new double[nq];
			}
			for (int q = 0; q < nq; q++) {
				final WaitingQueue queue = router.getWaitingQueue(q);
				if (queue != null)
					queue.removeWaitingQueueListener(ql);
			}
			for (int i = 0; i < nq; i++)
				waitingTimePredictor[i] = waitingTimeDefaultVQ[i];
		}
		if (newRouter != null && router != newRouter) {
			final int nq = newRouter.getNumWaitingQueues();

			if ((waitingTimePredictor == null) || (waitingTimeDefaultVQ == null)) // ajout
			{
				waitingTimePredictor = new double[nq];
				waitingTimeDefaultVQ = new double[nq];
			}

			for (int q = 0; q < nq; q++) {
				final WaitingQueue queue = newRouter.getWaitingQueue(q);
				if (queue != null)
					queue.addWaitingQueueListener(ql);
			}
			for (int i = 0; i < nq; i++)
				waitingTimePredictor[i] = waitingTimeDefaultVQ[i];
		}
		router = newRouter;
	}

	protected void dequeued(DequeueEvent ev) {
		if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_BEGINSERVICE) {
			if (collectingService)
				waitingTimePredictor[ev.getWaitingQueue().getId()] = updateWaitingTime(ev); // modifier
		} else if (ev.getEffectiveDequeueType() != Router.DEQUEUETYPE_TRANSFER)
			if (collectingAbandonment)
				waitingTimePredictor[ev.getWaitingQueue().getId()] = updateWaitingTime(ev); // modifier
	}

	// modifier
	private class QueueListener implements WaitingQueueListener {
		public void dequeued(DequeueEvent ev) {
			ExpectedDelayPredictorHQ.this.dequeued(ev);
		}

		public void enqueued(DequeueEvent ev) {
		}

		public void init(WaitingQueue queue) {
		}
	}
}
