package umontreal.iro.lecuyer.contactcenters;

import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueSet;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.list.ListOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.mperiods.MeasureMatrix;

/**
 * Defines utility methods for contact center simulation. This class provides
 * facilities to initialize the contact center's objects, and to perform some
 * actions on a group of objects.
 */
public class ContactCenter {
   private ContactCenter() {}

   /**
    * Initializes all elements enumerated by the iterable
    * \texttt{el}.
    * This method calls the \texttt{init} method
    * of any iterated object which is an instance of
    * {@link Initializable}, {@link MeasureMatrix},
    * {@link ListOfStatProbes}, and {@link MatrixOfStatProbes}.
    * For other elements instance of {@link Iterable}, this
    * method is called recursively.
    */
   public static void initElements (Iterable<?> el) {
      for (final Object e : el)
         initObject (e);
   }

   /**
    * Equivalent to {@link #initElements(Iterable)} for
    * an array of objects.
    * @param el the array of elements.
    */
   public static void initElements (Object[] el) {
      for (final Object e : el)
         initObject (e);
   }

   private static void initObject (Object e) {
      if (e == null)
         return;
      if (e instanceof Initializable)
         ((Initializable)e).init();
      else if (e instanceof MeasureMatrix)
         ((MeasureMatrix)e).init();
      else if (e instanceof StatProbe)
         ((StatProbe)e).init();
      else if (e instanceof ListOfStatProbes)
         ((ListOfStatProbes<?>)e).init();
      else if (e instanceof MatrixOfStatProbes)
         ((MatrixOfStatProbes<?>)e).init();
      else if (e instanceof Iterable)
         initElements ((Iterable<?>)e);
   }

   /**
    * Initializes all elements in \texttt{el}. For each {@link Initializable}
    * object in the array, calls the {@link Initializable#init} method.
    */
   public static void initElements (Initializable[] el) {
      for (final Initializable initializable : el)
         if (initializable != null)
            initializable.init ();
   }

   /**
    * Initializes all elements in \texttt{el}. For each {@link MeasureMatrix}
    * object in the array, calls the {@link MeasureMatrix#init} method.
    */
   public static void initElements (MeasureMatrix[] el) {
      for (final MeasureMatrix mat : el)
         if (mat != null)
            mat.init ();
   }

   /**
    * Initializes all elements in \texttt{el}. For each {@link StatProbe}
    * object in the array, calls the {@link StatProbe#init} method.
    */
   public static void initElements (StatProbe[] el) {
      for (final StatProbe probe : el)
         if (probe != null)
            probe.init ();
   }

   /**
    * Initializes all elements in \texttt{el}. For each {@link ListOfStatProbes}
    * object in the array, calls the {@link ListOfStatProbes#init} method.
    */
   public static void initElements (ListOfStatProbes<?>[] el) {
      for (final ListOfStatProbes<?> probe : el)
         if (probe != null)
            probe.init ();
   }

   /**
    * Initializes all elements in \texttt{el}. For each {@link MatrixOfStatProbes}
    * object in the array, calls the {@link MatrixOfStatProbes#init} method.
    */
   public static void initElements (MatrixOfStatProbes<?>[] el) {
      for (final MatrixOfStatProbes<?> probe : el)
         if (probe != null)
            probe.init ();
   }

   /**
    * Toggles the elements to the status \texttt{enabled}. For each
    * {@link ToggleElement} object enumerated by the
    * iterable \texttt{el}, calls the
    * {@link ToggleElement#start} or {@link ToggleElement#stop} methods.
    *
    * @param el
    *           the list of toggle elements.
    * @param enabled
    *           \texttt{true} if the toggle elements are enabled, \texttt{false}
    *           if they are disabled.
    */
   public static void toggleElements (Iterable<? extends ToggleElement> el,
         boolean enabled) {
      for (final ToggleElement te : el)
         if (te != null)
            if (enabled)
               te.start ();
            else
               te.stop ();
   }

   /**
    * Toggles the elements to the status \texttt{enabled}. For each
    * {@link ToggleElement} object in the array \texttt{el}, calls the
    * {@link ToggleElement#start} or {@link ToggleElement#stop} methods.
    *
    * @param el
    *           the array of toggle elements.
    * @param enabled
    *           \texttt{true} if the toggle elements are enabled, \texttt{false}
    *           if they are disabled.
    */
   public static void toggleElements (ToggleElement[] el, boolean enabled) {
      for (final ToggleElement toggleElement : el)
         if (toggleElement != null)
            if (enabled)
               toggleElement.start ();
            else
               toggleElement.stop ();
   }

   /**
    * For each period-change event enumerated by the iterable \texttt{pce}, calls the
    * {@link PeriodChangeEvent#start} method.
    *
    * @param pce
    *           the list of period-change events.
    */
   public static void startPeriodChangeEvents (
         Iterable<? extends PeriodChangeEvent> pce) {
      for (final PeriodChangeEvent p : pce) {
         if (p == null)
            continue;
         p.start ();
      }
   }

   /**
    * For each period-change event in the array \texttt{pce}, calls the
    * {@link PeriodChangeEvent#start} method.
    *
    * @param pce
    *           the array of period-change events.
    */
   public static void startPeriodChangeEvents (PeriodChangeEvent[] pce) {
      for (final PeriodChangeEvent ev : pce)
         if (ev != null)
            ev.start ();
   }

   /**
    * For each period-change event enumerated by the iterable
    * \texttt{pce}, calls the
    * {@link PeriodChangeEvent#stop} method.
    *
    * @param pce
    *           the list of period-change events.
    */
   public static void stopPeriodChangeEvents (
         Iterable<? extends PeriodChangeEvent> pce) {
      for (final PeriodChangeEvent p : pce) {
         if (p == null)
            continue;
         p.stop ();
      }
   }

   /**
    * For each period-change event in the array \texttt{pce}, calls the
    * {@link PeriodChangeEvent#stop} method.
    *
    * @param pce
    *           the array of period-change events.
    */
   public static void stopPeriodChangeEvents (PeriodChangeEvent[] pce) {
      for (final PeriodChangeEvent ev : pce)
         if (ev != null)
            ev.stop ();
   }

   /**
    * Clears all waiting queues enumerated by the iterable \texttt{waitingQueues} with dequeue type
    * \texttt{dqType}. For each {@link WaitingQueue} object in the list, calls
    * the {@link WaitingQueue#clear} method with the given \texttt{dqType}.
    *
    * @param waitingQueues
    *           the list of waiting queues.
    * @param dqType
    *           the dequeue type being used.
    * @exception NullPointerException
    *               if the given list is \texttt{null}.
    */
   public static void clearWaitingQueues (
         Iterable<? extends WaitingQueue> waitingQueues, int dqType) {
      for (final WaitingQueue queue : waitingQueues)
         if (queue != null)
            queue.clear (dqType);
   }

   /**
    * Clears all waiting queues in \texttt{waitingQueues} with dequeue type
    * \texttt{dqType}. For each {@link WaitingQueue} object in the array, calls
    * the {@link WaitingQueue#clear} method with the given \texttt{dqType}.
    *
    * @param waitingQueues
    *           the array of waiting queues.
    * @param dqType
    *           the dequeue type being used.
    * @exception NullPointerException
    *               if the given list is \texttt{null}.
    */
   public static void clearWaitingQueues (WaitingQueue[] waitingQueues,
         int dqType) {
      for (final WaitingQueue queue : waitingQueues)
         if (queue != null)
            queue.clear (dqType);
   }

   /**
    * Clears all waiting queues in \texttt{waitingQueues} with dequeue type
    * \texttt{dqType}. For each {@link WaitingQueueSet} object in the array,
    * clears all registered waiting queues with the given \texttt{dqType}.
    *
    * @param waitingQueues
    *           the array of waiting queues.
    * @param dqType
    *           the dequeue type being used.
    * @exception NullPointerException
    *               if the given list is \texttt{null}.
    */
   public static void clearWaitingQueues (WaitingQueueSet[] waitingQueues,
         int dqType) {
      for (final WaitingQueueSet queueSet : waitingQueues)
         if (queueSet != null)
            for (final WaitingQueue queue : queueSet)
               queue.clear (dqType);
   }

   /**
    * Returns a short string representation of the named object \texttt{named}.
    * If the length of \texttt{named.}{@link Named#getName getName()} is
    * greater than 0, returns that name. Otherwise, this returns the result of
    * the \texttt{toString} method defined in {@link Object}.
    *
    * @return a short string representation of the named object.
    */
   public static String toShortString (Named named) {
      if (named == null)
         return "null";
      final String n = named.getName ();
      if (n.length () > 0)
         return n;
      return named.getClass ().getName () + "@" + named.hashCode ();
   }
}
