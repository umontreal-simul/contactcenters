package umontreal.iro.lecuyer.contactcenters.app;

import umontreal.iro.lecuyer.contactcenters.dialer.AgentsMoveDialerPolicy;

/**
 * Represents the dialer policy specifying when a dialer
 * must try to make calls and how many calls to try
 * at a time.  Some of the policies need parameters which
 * are specified as part of the dialer parameters.
 * @xmlconfig.title Available dialer's policies
 */
public enum DialerPolicyType {
   /**
    * Dials only when the total number of free agents $\Ntf(t)$
    * in all agent groups is
    * greater than or equal to the minimum $s_{\mathrm{t},k}(t)$,
    * and the number of free agents $\Ndf[k](t)$ capable of serving
    * the dialed call type is greater than or equal to $s_{\mathrm{d},k}(t)$.
    * The thresholds do not change during main periods, but they can
    * change from period to period.
    * If dialing is performed,
    * $\mathrm{round}(\kappa\Ndf[k](t))+c$ outbound calls are produced.
    * $\kappa$ and $c$ corresponds to predefined constants,
    * and $\mathrm{round}(\cdot)$ corresponds to
    * $\cdot$ rounded to the nearest integer.
    @xmlconfig.title
    */
   DIALXFREE (Messages.getString("DialerPolicyType.DialXFree")), //$NON-NLS-1$

   /**
    * Equivalent to {@link #DIALXFREE} with
    * $\kappa=0$ and $c=1$.
    @xmlconfig.title
    */
   DIALONE (Messages.getString("DialerPolicyType.DialOne")), //$NON-NLS-1$

   /**
    * Equivalent to {@link #DIALXFREE} with
    * $\kappa=1$ and $c=1$.
    @xmlconfig.title
    */
   DIAL1XFREE (Messages.getString("DialerPolicyType.Dial1XFree")), //$NON-NLS-1$

   /**
    * Equivalent to {@link #DIALXFREE} with
    * $\kappa=2$ and $c=0$.
    @xmlconfig.title
    */
   DIAL2XFREE (Messages.getString("DialerPolicyType.Dial2XFree")), //$NON-NLS-1$

   /**
    * When the dialing conditions defined for
    * {@link #DIALXFREE} apply, i.e., $\Ntf(t)\ge s_{\mathrm{t},k}(t)$
    * and $\Ndf[k](t)\ge s_{\mathrm{d},k}(t)$,
    * and
    * the rate of inbound calls of any type waiting more
    * than the acceptable waiting time is
    * smaller than a threshold,
    * dials some calls.
    * Let $d=\mathrm{round}(\kappa\Ndf[k](t))+c$.
    * If the mismatch rate for outbound calls of type~$k$ being dialed
    * is smaller than a threshold, dials $2d$ calls.
    * Otherwise, dials $d$.
    *
    * The number of calls waiting more than the acceptable
    * waiting time and arrivals for all inbound call types,
    * the number of mismatches for call type~$k$, and the
    * total number of tried outbound calls of type~$k$ are computed
    * for periods with fixed duration $d_{\mathrm{D}}$.
    * When the dialer is required to take a decision, it computes the
    * bad call and mismatch rates by taking these values
    * during the $P_{\mathrm{D}}$
    * last checked periods.
    @xmlconfig.title
    */
   DIALFREE_BADCALLMISMATCHRATES (Messages.getString("DialerPolicyType.DialXFree_BadCallMismatchRate")), //$NON-NLS-1$

      /**
       *  \javadoc{Dialing policy with smart agent management.
       * See {@link AgentsMoveDialerPolicy} for more information.}\begin{xmldocenv}{@linkplain AgentsMoveDialerPolicy}
       * \end{xmldocenv}
       *
       * The parameters of the agent groups managed by the dialer
       * are specified using \texttt{agentGroupInfo} children elements,
       * in the dialer parameters.
       *
       * The flags of the dialer are controlled as follows.
       * The dialer keeps track of the global service level
       * (over all inbound call types) for the last
       * $P_{\mathrm{D}}$ periods of duration
       * $d_{\mathrm{D}}$.
       * These parameters are set by the attributes
       * \texttt{numCheckedPeriods} and
       * \texttt{checkedPeriodDuration} of the dialer parameters.
       * If the service level falls below the lower threshold $s_1$
       * given by the attribute \texttt{slInboundThresh},
       * the flag outbound-to-inbound is turned on,
       * and inbound-to-outbound is turned off.
       * On the other hand, if the service level goes above the higher threshold
       * $s_2$ set by the attribute
       * \texttt{slOutboundThresh}, the flag inbound-to-outbound is turned on while
       * the flag outbound-to-inbound is off.
       * When the service level is in $[s_1, s_2]$, both flags are turned off.
       @xmlconfig.title
       */
   AGENTSMOVE (Messages.getString ("DialerPolicyType.Agents move")); //$NON-NLS-1$

   private String name;

   DialerPolicyType (String name) { this.name = name; }

   @Override
   public String toString() {
      return name;
   }
}
