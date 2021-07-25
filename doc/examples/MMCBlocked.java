import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class MMCBlocked extends MMC {
   static final int QUEUECAPACITY = 5;
   int numBlocked;
   Tally blocked = new Tally ("Number of blocked contacts");
   FunctionOfMultipleMeansTally serviceLevel = new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Service level with blocked contacts", 2);

   MMCBlocked() {
      super();
      router.addExitedContactListener (new MyContactMeasuresBlocked());
      router.setTotalQueueCapacity (QUEUECAPACITY);
   }

   class MyContactMeasuresBlocked implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {
         ++numBlocked; }
      public void dequeued (Router router, DequeueEvent ev) {}
      public void served (Router router, EndServiceEvent ev) {}
   }

   @Override
   void simulateOneDay() {
      numBlocked = 0;
      super.simulateOneDay();
   }

   @Override
   void addObs() {
      super.addObs();
      blocked.add (numBlocked);
      serviceLevel.add (numGoodSL, numBlocked + numServed);
   }

   @Override
   void simulate (int days) {
      blocked.init();
      serviceLevel.init();
      super.simulate (days);
   }

   @Override
   public void printStatistics() {
      System.out.println (blocked.reportAndCIStudent (LEVEL, 3));
      super.printStatistics();
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
   }

   public static void main (String[] args) {
      final MMCBlocked s = new MMCBlocked();
      final Chrono timer = new Chrono();
      s.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      s.printStatistics();
   }
}
