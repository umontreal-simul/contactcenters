import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.Router;

import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.util.Chrono;

public class TelethonMsg extends Telethon {
   @Override
   Router createRouter() {
      return new MyRouter (TYPETOGROUPMAP, GROUPTOTYPEMAP);
   }

   class MyRouter extends QueuePriorityRouter {
      MyRouter (int[][] gt, int[][] tg) { super (gt, tg); }
      @Override
      protected void dequeued (DequeueEvent ev) {
         final int dq = ev.getEffectiveDequeueType();
         if (dq == 0) return;
         else if (dq == 5) new MessageDelayEvent (ev).schedule (0.5);
         else exitDequeued (ev);
      }

      class MessageDelayEvent extends Event {
         DequeueEvent ev;
         MessageDelayEvent (DequeueEvent ev) {
            this.ev = ev;
         }
         @Override
         public void actions() { exitDequeued (ev); }
      }
   }

   public static void main (String[] args) {
      final Telethon t = new TelethonMsg();
      final Chrono timer = new Chrono();
      t.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      t.printStatistics();
   }
}
