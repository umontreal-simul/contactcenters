package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.ArrayList; //ajouter
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;

public class MeanNLastWaitingTimePredictor extends LastWaitingTimePredictor {
	private final QueueListener ql = new QueueListener();
	private double waitingTimePredictor;
	private Router router;
	private int windowSizeVQ; // ajouter
	private double waitingTimeDefaultVQ; // ajouter
	private final ArrayList<Double> list = new ArrayList<Double>(); // ajouter
	private boolean collectingAbandonment = false;
	private boolean collectingService = true;

	// constructeur pour initialiser numContact et serviceTime

	/*
	 * public MeanNLastWaitingTimePredictor(int windowSizeVQ, double
	 * waitingTimeDefaultVQ){ this.windowSizeVQ=windowSizeVQ;
	 * this.waitingTimePredictor= waitingTimeDefaultVQ; }
	 */
	/*
	 * public MeanNLastWaitingTimePredictor(){ init(); }
	 */

	// Ajout de la methode qui modifie la taille de la fenetre
	public void setWindowSizeVQ(int taille) {
		windowSizeVQ = taille;
	}

	// Ajout de la methode qui modifie la taille de la fenetre

	public void setWaitingTimeDefaultVQ(double attente) {
		waitingTimeDefaultVQ = attente;
	}

	// modifier
	public double getWaitingTime(Contact contact) {
		double somme = 0;
		for (Double d : list)
			somme += d;
		waitingTimePredictor = somme / windowSizeVQ;
		return waitingTimePredictor;
	}

	// modifier
	public double getWaitingTime(Contact contact, WaitingQueue queue) {
		double somme = 0;
		for (Double d : list)
			somme += d;
		waitingTimePredictor = somme / windowSizeVQ;
		return waitingTimePredictor;

	}

	// Ajout de la methdoe update pour mettre a jour le Arraylist des N dernier
	// temps de service

	public double updateWaitingTime(DequeueEvent ev) {
		list.remove(0);
		double d = (Double) (ev.getEffectiveQueueTime());
		list.add(d);
		return getWaitingTime(ev.getContact());

	}

	// modifier pour initialiser le WaitingTime
	public void init() {
		waitingTimePredictor = waitingTimeDefaultVQ;
		for (int i = 0; i < windowSizeVQ - 1; i++)
			list.add(waitingTimeDefaultVQ);

	}

	public void setRouter(Router newRouter) {
		if (router != null && newRouter != router) {
			final int nq = router.getNumWaitingQueues();
			for (int q = 0; q < nq; q++) {
				final WaitingQueue queue = router.getWaitingQueue(q);
				if (queue != null)
					queue.removeWaitingQueueListener(ql);
			}
			waitingTimePredictor = waitingTimeDefaultVQ;
		}
		if (newRouter != null && router != newRouter) {
			final int nq = newRouter.getNumWaitingQueues();
			for (int q = 0; q < nq; q++) {
				final WaitingQueue queue = newRouter.getWaitingQueue(q);
				if (queue != null)
					queue.addWaitingQueueListener(ql);
			}
			waitingTimePredictor = waitingTimeDefaultVQ;
		}
		router = newRouter;
	}

	protected void dequeued(DequeueEvent ev) {
		if (ev.getEffectiveDequeueType() == Router.DEQUEUETYPE_BEGINSERVICE) {
			if (collectingService)
				waitingTimePredictor = updateWaitingTime(ev); // modifier
		} else if (ev.getEffectiveDequeueType() != Router.DEQUEUETYPE_TRANSFER)
			if (collectingAbandonment)
				waitingTimePredictor = updateWaitingTime(ev); // modifier
	}

	// modifier
	private class QueueListener implements WaitingQueueListener {
		public void dequeued(DequeueEvent ev) {
			MeanNLastWaitingTimePredictor.this.dequeued(ev);
		}

		public void enqueued(DequeueEvent ev) {
		}

		public void init(WaitingQueue queue) {
		}
	}

}
