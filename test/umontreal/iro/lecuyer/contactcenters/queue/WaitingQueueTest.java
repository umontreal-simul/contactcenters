package umontreal.iro.lecuyer.contactcenters.queue;

import java.util.Iterator;

import junit.framework.TestCase;

import umontreal.iro.lecuyer.contactcenters.ConstantValueGenerator;
import umontreal.iro.lecuyer.contactcenters.ValueGenerator;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;

import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class WaitingQueueTest extends TestCase {
   private WaitingQueue queue;
   private final WaitingQueueListener listener = new TestListener ();
   private final double[] times = { Double.POSITIVE_INFINITY, 100.0, 0.0 };
   private final ValueGenerator pgen = new ConstantValueGenerator (times);
   private Contact patientContact;
   private Contact impatientContact;
   private Contact veryImpatientContact;
   private int nAbandons;
   private int nlEnqueued;
   private int nlDequeued;
   private int nlAbandons;

   public WaitingQueueTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      queue = new StandardWaitingQueue ();
      queue.setMaximalQueueTimeGenerator (1, pgen);
      queue.addWaitingQueueListener (listener);
      nAbandons = 0;
      nlEnqueued = 0;
      nlDequeued = 0;
      nlAbandons = 0;
      Sim.init ();
      patientContact = new MyContact (0);
      impatientContact = new MyContact (1);
      veryImpatientContact = new MyContact (2);
   }

   @Override
   public void tearDown () {
      queue = null;
   }

   public void testImmediateAbandon () {
      assertNotNull (queue.add (veryImpatientContact));
      assertEquals (0, queue.size ());
      assertEquals ("nlAbandons", 1, nAbandons);
      assertEquals ("nlEnqueued", 1, nlEnqueued);
      assertEquals ("nlDequeued", 0, nlDequeued);
      assertEquals ("nlAbandons", 1, nlAbandons);
   }

   public void testAbandon () {
      assertNotNull (queue.add (impatientContact));
      assertEquals (1, queue.size ());
      new AbandonCheckEvent ().schedule (90.0);
      new EndSimEvent ().schedule (150.0);
      Sim.start ();
      assertEquals ("nAbandons", 1, nAbandons);
      assertEquals ("nlEnqueued", 1, nlEnqueued);
      assertEquals ("nlDequeued", 0, nlDequeued);
      assertEquals ("nlAbandons", 1, nlAbandons);
   }

   public void testCancelAbandon () {
      assertNotNull (queue.add (impatientContact));
      new RemoveEvent ().schedule (90.0);
      new EndSimEvent ().schedule (150.0);
      Sim.start ();
      assertEquals ("nAbandons", 0, nAbandons);
      assertEquals ("nlEnqueued", 1, nlEnqueued);
      assertEquals ("nlDequeued", 1, nlDequeued);
      assertEquals ("nlAbandons", 0, nlAbandons);
   }

   public void testIterator () {
      queue.add (impatientContact);
      queue.add (patientContact);
      final Iterator<DequeueEvent> itr = queue.iterator (0);
      assertTrue (itr.hasNext ());
      final DequeueEvent qci1 = itr.next ();
      assertTrue (itr.hasNext ());
      final DequeueEvent qci2 = itr.next ();
      assertFalse (itr.hasNext ());

      assertEquals ("First queued contact", impatientContact, qci1
            .getContact ());
      assertEquals ("First contact queue time", 0.0, qci1.getEnqueueTime (),
            1e-6);
      assertEquals ("First contact patience time", 100.0, qci1
            .getScheduledQueueTime (), 1e-6);
      assertEquals ("First contact queue time", 0, qci1.getEnqueueTime (), 1e-6);
      assertEquals ("First contact wait queue", queue, qci1.getWaitingQueue ());
      assertFalse (qci1.dequeued ());

      assertEquals ("Second queued contact", patientContact, qci2.getContact ());
      assertEquals ("Second contact queue time", 0.0, qci2.getEnqueueTime (),
            1e-6);
      assertEquals ("Second contact patience time", Double.POSITIVE_INFINITY,
            qci2.getScheduledQueueTime (), 1e-6);
      assertEquals ("Second contact queue time", 0, qci2.getEnqueueTime (),
            1e-6);
      assertEquals ("Second contact wait queue", queue, qci2.getWaitingQueue ());
      assertFalse (qci2.dequeued ());
      assertFalse (itr.hasNext ());

      new ItrTestEvent1 (qci1, qci2).schedule (50.0);
      Sim.start ();
   }

   class ItrTestEvent1 extends Event {
      private DequeueEvent qci1;
      private DequeueEvent qci2;

      public ItrTestEvent1 (DequeueEvent qci1, DequeueEvent qci2) {
         this.qci1 = qci1;
         this.qci2 = qci2;
      }

      @Override
      public void actions () {
         new ItrTestEvent2 (qci1, qci2).schedule (51.0);
      }
   }

   class ItrTestEvent2 extends Event {
      private DequeueEvent qci1;
      private DequeueEvent qci2;

      public ItrTestEvent2 (DequeueEvent qci1, DequeueEvent qci2) {
         this.qci1 = qci1;
         this.qci2 = qci2;
      }

      @Override
      public void actions () {
         assertTrue (qci1.dequeued ());
         assertEquals ("Dequeue type", 1, qci1.getEffectiveDequeueType ());

         Iterator<DequeueEvent> itr = queue.iterator (0);
         assertTrue (itr.hasNext ());
         assertTrue (itr.next () == qci2);
         assertFalse (itr.hasNext ());

         queue.remove (patientContact, 0);
         assertTrue (qci2.dequeued ());
         assertEquals ("Dequeue type", 0, qci2.getEffectiveDequeueType ());
         itr = queue.iterator (0);
         assertFalse (itr.hasNext ());

         queue.removeWaitingQueueListener (listener);
         queue.add (patientContact);
         queue.add (impatientContact);
         new EndSimEvent ().schedule (230.0);
         Sim.start ();
         itr = queue.iterator (0);
         assertTrue (itr.hasNext ());
         assertNotNull (itr.next ());
         assertFalse (itr.hasNext ());
      }
   }

   private class AbandonCheckEvent extends Event {
      @Override
      public void actions () {
         assertTrue (nAbandons == 0);
      }
   }

   private class RemoveEvent extends Event {
      @Override
      public void actions () {
         assertNotNull (queue.removeFirst (0));
      }
   }

   private class EndSimEvent extends Event {
      @Override
      public void actions () {
         Sim.stop ();
      }
   }

   private class MyContact extends Contact {
      private static final long serialVersionUID = -2549493981150781601L;

      public MyContact (int typeId) {
         super (typeId);
      }

      @Override
      public void dequeued (DequeueEvent ev) {
         super.dequeued (ev);
         if (ev.getEffectiveDequeueType () == 1) {
            assertTrue (queue == ev.getWaitingQueue ());
            nAbandons++;
         }
      }
   }

   class TestListener implements WaitingQueueListener {
      public void init (WaitingQueue queue) {}

      public void enqueued (DequeueEvent ev) {
         assertTrue (ev.getWaitingQueue () == WaitingQueueTest.this.queue);
         nlEnqueued++;
      }

      public void dequeued (DequeueEvent ev) {
         assertTrue (ev.getWaitingQueue () == WaitingQueueTest.this.queue);
         assertEquals ("Queue time", ev.getEffectiveQueueTime (), ev
               .getContact ().getTotalQueueTime (), 1e-6);
         assertTrue (ev.dequeued ());
         final int dqType = ev.getEffectiveDequeueType ();
         if (dqType == 0)
            nlDequeued++;
         else if (dqType == 1)
            nlAbandons++;
         else
            fail ("Invalid dequeue type: " + dqType);
      }
   }
}
