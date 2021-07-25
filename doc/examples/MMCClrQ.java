import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class MMCClrQ extends MMC {
   int numDisconnected;
   Tally disconnected = new Tally ("Number of disconnected contacts");
   FunctionOfMultipleMeansTally serviceLevel = new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Service level with disconnected contacts", 2);

   MMCClrQ() {
      super();
      router.addExitedContactListener (new MyContactMeasuresClr());
   }

   class MyContactMeasuresClr implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, DequeueEvent ev) {
         ++numDisconnected;
      }
      public void served (Router router, EndServiceEvent ev) {}
   }

   @Override
   void endSim() { super.endSim();   queue.clear (5); }

   @Override
   void simulateOneDay() {
      numDisconnected = 0;
      super.simulateOneDay();
   }

   @Override
   void addObs() {
      super.addObs();
      disconnected.add (numDisconnected);
      serviceLevel.add (numGoodSL, numServed + numDisconnected);
   }

   @Override
   void simulate (int days) {
      disconnected.init();
      serviceLevel.init();
      super.simulate (days);
   }

   @Override
   public void printStatistics() {
      System.out.println (disconnected.reportAndCIStudent (LEVEL, 3));
      super.printStatistics();
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
   }

   public static void main (String[] args) {
      final MMCClrQ s = new MMCClrQ();
      final Chrono timer = new Chrono();
      s.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      s.printStatistics();
   }
}
