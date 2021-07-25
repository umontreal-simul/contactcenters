package umontreal.iro.lecuyer.contactcenters.dialer;

/**
 * Represents a dialer's policy which
 * always tries to make the same number of
 * calls on each trial.
 */
public class ConstantDialerPolicy implements DialerPolicy {
   private DialerList list;
   private int n;

   /**
    * Constructs a new dialer's policy
    * with dialer list \texttt{list}, and
    * \texttt{n} calls to make
    * on each trial.
    @param list the dialer list to extract calls from.
    @param n the number of calls to make on each occasion.
    @exception NullPointerException if \texttt{list}
    is \texttt{null}.
    @exception IllegalArgumentException if \texttt{n}
    is negative.
    */
   public ConstantDialerPolicy (DialerList list, int n) {
      if (list == null)
         throw new NullPointerException ("The dialer list must not be null");
      if (n < 0)
         throw new IllegalArgumentException ("n < 0");
   }

   public void init(Dialer dialer) {
      list.clear();
   }

   public DialerList getDialerList(Dialer dialer) {
      return list;
   }

   /**
    * Sets the dialer list to \texttt{list}.
    @param list the new dialer list.
    @exception NullPointerException if \texttt{list} is \texttt{null}.
    */
   public void setDialerList (DialerList list) {
      if (list == null)
         throw new NullPointerException ("The dialer list must not be null");
      this.list = list;
   }

   public int getNumDials(Dialer dialer) {
      final int nev = dialer.isUsingNumActionsEvents () ? dialer.getNumActionEvents () : 0;
      if (n < nev)
         return 0;
      return n - nev;
   }

   public void dialerStarted(Dialer dialer) {}

   public void dialerStopped(Dialer dialer) {}

   /**
    * Sets the number of dialed contacts to
    * \texttt{n}.
    @param n the number of calls to make upon each trial.
    @exception IllegalArgumentException if \texttt{n}
    is negative.
    */
   public void setNumDials (int n) {
      if (n < 0)
         throw new IllegalArgumentException ("n < 0");
      this.n = n;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getName());
      sb.append ('[');
      sb.append ("n: ").append (n);
      sb.append (", dialer list: ").append (list);
      sb.append (']');
      return sb.toString();
   }
}
