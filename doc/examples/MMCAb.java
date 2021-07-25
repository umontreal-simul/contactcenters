import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.stat.FunctionOfMultipleMeansTally;
import umontreal.iro.lecuyer.stat.Tally;
import umontreal.iro.lecuyer.util.Chrono;
import umontreal.iro.lecuyer.util.RatioFunction;

public class MMCAb extends MMC {
   static final double NU = 1.0;      // 1/Mean patience time

   ExponentialGen pgen = new ExponentialGen (new MRG32k3a(), NU);
   int numAbandoned;
   int numAbandonedAfterAWT;
   Tally abandoned = new Tally ("Number of contacts having abandoned");
   FunctionOfMultipleMeansTally serviceLevel = new FunctionOfMultipleMeansTally
      (new RatioFunction(), "Service level with abandonment", 2);

   MMCAb() {
      super();
      arrivProc.setContactFactory (new MyContactFactoryAb());
      router.addExitedContactListener (new MyContactMeasuresAb());
   }

   class MyContactFactoryAb extends MyContactFactory {
      @Override
      public Contact newInstance() {
         final Contact contact = super.newInstance();
         contact.setDefaultPatienceTime (pgen.nextDouble());
         return contact;
      }
   }

   class MyContactMeasuresAb implements ExitedContactListener {
      public void blocked (Router router, Contact contact, int bType) {}
      public void dequeued (Router router, DequeueEvent ev) {
         ++numAbandoned;
         if (ev.getContact().getTotalQueueTime() >= AWT) ++numAbandonedAfterAWT;
      }
      public void served (Router router, EndServiceEvent ev) {}
   }

   @Override
   void simulateOneDay() {
      numAbandoned = numAbandonedAfterAWT = 0;
      super.simulateOneDay();
   }

   @Override
   void addObs() {
      super.addObs();
      abandoned.add (numAbandoned);
      serviceLevel.add (numGoodSL, numServed + numAbandonedAfterAWT);
   }

   @Override
   void simulate (int days) {
      abandoned.init();
      serviceLevel.init();
      super.simulate (days);
   }

   @Override
   public void printStatistics() {
      System.out.println (abandoned.reportAndCIStudent (LEVEL, 3));
      super.printStatistics();
      System.out.println (serviceLevel.reportAndCIDelta (LEVEL, 3));
   }

   public static void main (String[] args) {
      final MMCAb s = new MMCAb();
      final Chrono timer = new Chrono();
      s.simulate (NUMDAYS);
      System.out.println ("CPU time: " + timer.format());
      s.printStatistics();
   }
}
