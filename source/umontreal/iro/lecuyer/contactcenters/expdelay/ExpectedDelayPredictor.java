package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.AgentGroup;

/**
 * Approximates the expected waiting time conditional on a given queue assuming
 * that service times are i.i.d.\ exponential, and a queue is associated with
 * each agent group. More specifically, let $Q_i(t)$ be the size of waiting
 * queue $i$ at time $t$, and $N_i(t)$ be the total number of agents in group
 * $i$. We suppose that agents in group $i$ cannot pick up contacts in other
 * queues than queue $i$. Assuming that service times at agent group $i$ are
 * i.i.d.\ exponential with mean $1/\mu_i$, the expected waiting time for a
 * contact waiting at queue $i$ is $(Q_i(t) + 1)/(\mu_iN_i(t))$. The rates
 * $\mu_i$ are initialized to 1, and should be changed using
 * {@link #setMu(int,double)}. Moreover, if $K=I$, the rates can be initialized
 * automatically using {@link #setMuWithContactTypes()}.
 */
public class ExpectedDelayPredictor implements WaitingTimePredictor {
	private Router router;

	private double[] mu;

	public Router getRouter() {
		return router;
	}

	public double getWaitingTime(Contact contact) {
		return Double.NaN;
	}

	public double getWaitingTime(Contact contact, WaitingQueue queue) {
		if (router == null)
			throw new IllegalStateException(
					"Not enough information to predict the waiting time; use setRouter to associate a router with this predictor");
		if (queue == null)
			throw new NullPointerException(
					"The given waiting queue must not be null");
		final int i = queue.getId();
		if (router.getWaitingQueue(i) != queue)
			throw new IllegalStateException(
					"The given queue is not associated with the correct router");
		final AgentGroup group = router.getAgentGroup(i);
		if (group == null)
			throw new IllegalStateException(
					"No agent group associated with waiting queue " + i);
		if (group.getNumFreeAgents() > 0 && queue.isEmpty())
			return 0;
		final int qs = queue.size();
		final int n = group.getNumAgents();
		final double delay = (qs + 1.0) / (n * mu[i]);
		// if (contact.getNumWaitingQueues () > 0) {
		// DequeueEvent ev = queue.getDequeueEvent (contact);
		// if (ev == null)
		// return delay;
		// double wt = ev.simulator ().time () - ev.getEnqueueTime ();
		// return Math.max (0, delay - wt);
		// }
		return delay;
	}

	public void init() {
	}

	public void setRouter(Router router) {
		if (router != null) {
			if (router.getNumWaitingQueues() != router.getNumAgentGroups())
				throw new IllegalArgumentException(
						"This predictor requires a waiting queue to be associated with each agent group");
			mu = new double[router.getNumWaitingQueues()];
			Arrays.fill(mu, 1);
			this.router = router;
			setMuWithContactTypes();
		} else {
			mu = null;
			this.router = router;
		}
	}

	/**
	 * Returns the currently used value of $\mu_i$.
	 * 
	 * @param i
	 *           the waiting queue index.
	 * @return the value of $\mu_i$.
	 */
	public double getMu(int i) {
		return mu[i];
	}

	/**
	 * Returns an array containing a copy of the values of $\mu_i$.
	 * 
	 * @return an array containing the values of $\mu_i$.
	 */
	public double[] getMu() {
		return mu.clone();
	}

	/**
	 * Sets the value of $\mu_i$ to \texttt{m}.
	 * 
	 * @param i
	 *           the index of the affected waiting queue.
	 * @param m
	 *           the new value of $\mu_i$.
	 */
	public void setMu(int i, double m) {
		if (m <= 0)
			throw new IllegalArgumentException(
					"the value of mu_i must be positive");
		mu[i] = m;
	}

	/**
	 * Sets the values of $\mu_i$ to \texttt{mu}.
	 * 
	 * @param mu
	 *           the array containing the new values of $\mu_i$.
	 */
	public void setMu(double[] mu) {
		if (mu.length != this.mu.length)
			throw new IllegalArgumentException("Invalid length of mu");
		for (int i = 0; i < mu.length; i++)
			setMu(i, mu[i]);
	}

	/**
	 * Initializes the values of $\mu_i$ using the mean service time for contact
	 * types. This method assumes that $K=I$, i.e., there is a waiting queue for
	 * each contact type. In that setting, the service rate $\mu_i$ is
	 * initialized using the mean service time for contact type $i$.
	 */
	public void setMuWithContactTypes() {
		if (router.getNumContactTypes() != mu.length)
			throw new IllegalArgumentException(
					"This method requires a contact type for each waiting queue");
		for (int k = 0; k < mu.length; k++) {
			if (router.getContactFactory(k).getContactTimeGen() != null) {
				double c = router.getContactFactory(k).getContactTimeGen()
						.getDistribution().getMean();
				mu[k] = 1.0 / c;
			} else
				mu[k] = 1.0;
		}
	}
}
