package umontreal.iro.lecuyer.contactcenters.msk.model;

import umontreal.iro.lecuyer.contactcenters.PeriodChangeEvent;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.contact.ServiceTimes;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

/**
 * Represents a call in the multi-skill call center simulator. A call is a
 * special type of contact that stores the periods of its arrival,
 * of its service startup and its service termination.
 * These periods can be stored,
 * because the model uses a single period-change event.
 * A call also holds additional information such as
 * transfer times, conference times, etc.
 */
public class Call extends Contact {
   private PeriodChangeEvent pce;
   private int arrivalPeriod;
   private int beginServicePeriod = -1;
   private int exitPeriod = -1;
   private double waitingTimeVQ;
   private boolean rightPartyConnect = true;
   private EndServiceEvent primaryEndServiceEvent;
   //private double conferenceTime = 0;
   //private double[] conferenceTimeGroups;
   //private double preServiceTimeNoConf = 0;
   //private double[] preServiceTimeNoConfGroups;
   //private double transferTime = 0;
   //private double[] transferTimeGroups;
   private ServiceTimes conferenceTimes;
   private ServiceTimes preServiceTimesNoConf;
   private ServiceTimes transferTimes;
   private double uTransfer;
   private double uTransferWait;
   private double uVQ;
   private double uVQCallBack;
   private int kBeforeVQ = -1;

   /**
    * Equivalent to {@link #Call(PeriodChangeEvent,int,double,int) Call}
    * \texttt{(pce, arrivalPeriod, 1, 0)}.
    * @param pce the period-change event associated with the call.
    * @param arrivalPeriod the period of arrival of the call.
    * @exception NullPointerException if \texttt{pce} is \texttt{null}.
    */
   public Call (PeriodChangeEvent pce, int arrivalPeriod) {
      super (pce.simulator ());
      this.pce = pce;
      this.arrivalPeriod = arrivalPeriod;
   }

   /**
    * Equivalent to {@link #Call(PeriodChangeEvent,int,double,int) Call}
    * \texttt{(pce, arrivalPeriod, 1, typeId)}.
    * @param pce the period-change event associated with the call.
    * @param arrivalPeriod the period of arrival of the call.
    * @param typeId the type identifier of the call.
    * @exception NullPointerException if \texttt{pce} is \texttt{null}.
    */
   public Call (PeriodChangeEvent pce, int arrivalPeriod, int typeId) {
      super (pce.simulator (), typeId);
      this.pce = pce;
      this.arrivalPeriod = arrivalPeriod;
   }

   /**
    * Constructs a new call with period-change event
    * \texttt{pce}, period of arrival \texttt{arrivalPeriod},
    * priority \texttt{priority}, and type identifier
    * \texttt{typeId}.
    * The period-change event is used to set the simulator
    * associated with the call, and to determine
    * periods of service termination or abandonment.
    * @param pce the period-change event associated with the call.
    * @param arrivalPeriod the period of arrival of the call.
    * @param priority the priority of the call.
    * @param typeId the type identifier of the call.
    * @exception NullPointerException if \texttt{pce} is \texttt{null}.
    */
   public Call (PeriodChangeEvent pce, int arrivalPeriod, double priority, int typeId) {
      super (pce.simulator (), priority, typeId);
      this.pce = pce;
      this.arrivalPeriod = arrivalPeriod;
   }

   /**
    * Returns the period during which this call has arrived. This corresponds to the
    * period during which the call object was constructed.
    * 
    * @return the period during which the call arrived.
    */
   public int getArrivalPeriod () {
      return arrivalPeriod;
   }
   
   /**
    * Sets the period of arrival of this call to
    * \texttt{arrivalPeriod}.
    * @param arrivalPeriod the new period of arrival.
    */
   public void setArrivalPeriod (int arrivalPeriod) {
      this.arrivalPeriod = arrivalPeriod;
   }
   
   /**
    * Returns the period at which the
    * service of this call started, or
    * -1 if this call was never served.
    * 
    * @return the period at which the service
    * of this call began.
    */
   public int getBeginServicePeriod() {
      return beginServicePeriod;
   }
   
   /**
    * Sets the period at which the service of this call begins
    * to \texttt{beginServicePeriod}.
    * @param beginServicePeriod the period at which the service of this call begins.
    */
   public void setBeginServicePeriod (int beginServicePeriod) {
      this.beginServicePeriod = beginServicePeriod;
   }
   
   /**
    * Returns the period at which this
    * call exited the system, or -1 if the
    * call is still in the system.
    * @return the period at which this call exited the system.
    */
   public int getExitPeriod() {
      return exitPeriod;
   }
   
   /**
    * Sets the period at which this call exits the system
    * to \texttt{exitPeriod}.
    * @param exitPeriod the period at which this call exits the system.
    */
   public void setExitPeriod (int exitPeriod) {
      this.exitPeriod = exitPeriod;
   }
   
   /**
    * Returns the period-change event used
    * to initializes the period at which
    * the service begins, and at which this
    * call exits.
    * @return the period-change event.
    */
   public PeriodChangeEvent getPeriodChangeEvent() {
      return pce;
   }
   
   /**
    * Sets the period-change event of this call
    * to \texttt{pce}.
    * @param pce the period-change event associated with this call.
    * @exception NullPointerException if \texttt{pce} is \texttt{null}.
    */
   public void setPeriodChangeEvent (PeriodChangeEvent pce) {
      if (pce == null)
         throw new NullPointerException();
      this.pce = pce;
   }

   @Override
   public void beginService (EndServiceEvent ev) {
      super.beginService (ev);
      beginServicePeriod = pce.getCurrentPeriod ();
   }

   @Override
   public void blocked (int bType) {
      super.blocked (bType);
      exitPeriod = pce.getCurrentPeriod ();
   }

   @Override
   public void dequeued (DequeueEvent ev) {
      super.dequeued (ev);
      exitPeriod = pce.getCurrentPeriod ();
   }

   @Override
   public void endContact (EndServiceEvent ev) {
      super.endContact (ev);
      exitPeriod = pce.getCurrentPeriod ();
   }
   
   /**
    * Determines if this call is a right party connect.
    * By default, this method returns \texttt{true}, but for
    * outbound calls, {@link OutboundCallFactory} can
    * set this flag to \texttt{false} in order to
    * generate a wrong party connect.
    * This differs from a failed call, which is handled by
    * the dialer itself, because an agent is needed to
    * screen the call.
    * The main use of the returned value is for
    * statistical collecting.
    * @return \texttt{true} if and only if this call is a right
    * party connect, or an inbound call.
    */
   public boolean isRightPartyConnect() {
      return rightPartyConnect;
   }
   
   /**
    * Sets the indicator for right party connect to
    * \texttt{rightPartyConnect}.
    * @param rightPartyConnect the new value of the indicator.
    * @see #isRightPartyConnect()
    */
   public void setRightPartyConnect (boolean rightPartyConnect) {
      this.rightPartyConnect = rightPartyConnect;
   }
   
   /**
    * If this object represents a transferred call,
    * returns a reference to the end-service event
    * representing the service with the primary agent,
    * before the transfer.
    * This end-service event is used to terminate
    * the service with the primary agent after
    * a conference time.
    * This returns \texttt{null} if this object
    * does not represent a transferred call, or
    * if the primary agent does not wait for a secondary
    * agent after the transfer.
    * @return the end-service event associated with
    * the primary agent for a transferred call.
    */
   public EndServiceEvent getPrimaryEndServiceEvent () {
      return primaryEndServiceEvent;
   }

   /**
    * Sets the end-service event associated with
    * the primary agent for a transferred call to
    * \texttt{primaryEndServiceEvent}.
    * @param primaryEndServiceEvent the new end-service event.
    */
   public void setPrimaryEndServiceEvent (
         EndServiceEvent primaryEndServiceEvent) {
      this.primaryEndServiceEvent = primaryEndServiceEvent;
   }
   
   /**
    * Returns the random number used to test if
    * a call is transferred after its service is
    * over.  This uniform is initialized
    * by the call factory if call transfers are
    * supported.
    * Otherwise, it is set to 0.
    * @return the uniform for deciding if the call
    * is transferred.
    */
   public double getUTransfer () {
      return uTransfer;
   }

   /**
    * Sets the uniform for transfer decision to
    * \texttt{transfer}.
    * @param transfer the new uniform.
    * @exception IllegalArgumentException if \texttt{transfer} is out of
    * $[0,1]$.
    * @see #getUTransfer()
    */
   public void setUTransfer (double transfer) {
      if (transfer < 0 || transfer > 1)
         throw new IllegalArgumentException ("transfer not in [0,1]");
      uTransfer = transfer;
   }

   /**
    * Returns the uniform used to decide if the primary agent
    * waits for a secondary agent after a transfer.
    * This uniform is generated by the call factory only if
    * call transfers are supported. If transfers
    * are disabled, this method
    * always returns 0.
    * @return the uniform for deciding if the primary agent
    * waits for the secondary agent with the caller.
    */
   public double getUTransferWait () {
      return uTransferWait;
   }

   /**
    * Sets the uniform for deciding if
    * the primary agent waits for a secondary agent to
    * \texttt{transferWait}.
    * @param transferWait the new uniform.
    * @exception IllegalArgumentException if \texttt{transferWait} is out of
    * $[0,1]$.
    * @see #getUTransferWait()
    */
   public void setUTransferWait (double transferWait) {
      if (transferWait < 0 || transferWait > 1)
         throw new IllegalArgumentException ("transferWait not in [0,1]");
      uTransferWait = transferWait;
   }
   
   /**
    * Returns the uniform used to decide if a
    * call accepts to be called back (or join the
    * virtual queue) if offered the possibility.
    * This uniform is generated by the call factory
    * if virtual queueing is used. If virtual queueing
    * is disabled, this method always returns 0.
    * @return the uniform for virtual queueing decision.
    */
   public double getUVQ() {
      return uVQ;
   }
   
   /**
    * Sets the uniform for deciding if a call
    * chooses to be called back to \texttt{u}.
    * @param u the new uniform.
    * @exception IllegalArgumentException if \texttt{u}
    * is not in $[0,1]$.
    * @see #getUVQ()
    */
   public void setUVQ (double u) {
      if (u < 0 || u > 1)
         throw new IllegalArgumentException
         ("u not in [0,1]");
      uVQ = u;
   }

   /**
    * Returns the uniform used to decide if a call
    * returning from the virtual queue is successfully
    * called back. This uniform is generated
    * by the call factory, and is always 0 if
    * virtual queueing is disabled.
    * @return the uniform for call back success.
    */
   public double getUVQCallBack() {
      return uVQCallBack;
   }
   
   /**
    * Sets the uniform for call back success
    * to \texttt{u}.
    * @param u the new uniform.
    * @exception IllegalArgumentException if \texttt{u}
    * is not in $[0,1]$.
    * @see #getUVQCallBack()
    */
   public void setUVQCallBack (double u) {
      if (u < 0 || u > 1)
         throw new IllegalArgumentException
         ("u not in [0,1]");
      uVQCallBack = u;
   }
   
   /**
    * Returns the conference times spent by a primary agent with a secondary
    * before the service of this transferred call begins with
    * the secondary agent.
    * By default, this is set to 0.
    * This time is set by the call factory if call transfers are
    * enabled.
    @return an object storing the conference times.
    */
   public ServiceTimes getConferenceTimes() {
      if (conferenceTimes == null)
         return conferenceTimes = new ServiceTimes (0);
      return conferenceTimes;
   }

   /**
    * Returns the pre-service times with an agent.
    * By default, this is set to 0.
    @return an object storing pre-service times.
    */
   public ServiceTimes getPreServiceTimesNoConf() {
      if (preServiceTimesNoConf == null)
         return preServiceTimesNoConf = new ServiceTimes (0);
      return preServiceTimesNoConf;
   }
   
   /**
    * Returns the transfer times spent by primary agents
    * to initiate call transfers.
    * By default, this is set to 0.
    * @return an object storing transfer times.
    */
   public ServiceTimes getTransferTimes () {
      if (transferTimes == null)
         return transferTimes = new ServiceTimes (0);
      return transferTimes;
   }
   
   /**
    * Returns the time spent in virtual queue by this call.
    * If virtual queueing is disabled, this method always returns 0.
    * @return the waiting time of this call in virtual queue.
    */
   public double getWaitingTimeVQ () {
      return waitingTimeVQ;
   }

   /**
    * Sets the waiting time in virtual queue of this call
    * to \texttt{waitingTimeVQ}.
    * @param waitingTimeVQ the new waiting time in virtual queue.
    */
   public void setWaitingTimeVQ (double waitingTimeVQ) {
      this.waitingTimeVQ = waitingTimeVQ;
   }

   /**
    * Returns the type of this call before entering
    * virtual queue.
    * @return the type identifier of this call before entering
    * virtual queue.
    */
   public int getTypeBeforeVQ () {
      return kBeforeVQ;
   }

   /**
    * Sets the type of this call before it enters virtual queue
    * to \texttt{beforeVQ}.
    * @param beforeVQ the original type of this call.
    */
   public void setTypeBeforeVQ (int beforeVQ) {
      kBeforeVQ = beforeVQ;
   }
}
