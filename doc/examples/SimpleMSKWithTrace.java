import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;

public class SimpleMSKWithTrace extends SimpleMSK {
   static final int NUMDAYS = 5; // Number of replications
   private TraceManager trace;
   private File traceFile;
   private int currentRep = 0;

   public SimpleMSKWithTrace (File traceFile, int timePrecision) {
      super ();
      this.traceFile = traceFile;
      trace = new TraceManager (timePrecision);
      router.addExitedContactListener (trace);
   }

   final class TraceManager implements ExitedContactListener {
      private PrintWriter output;
      private boolean closed = true;
      private int timePrecision;

      public TraceManager (int timePrecision) {
         this.timePrecision = timePrecision;
      }

      public void init (Writer output) {
         if (output == null)
            throw new NullPointerException ();
         this.output = new PrintWriter (new BufferedWriter (output));
         closed = false;
      }

      public void close () {
         output.close ();
         closed = true;
      }

      public void writeHeader () {
         if (closed)
            return;
         final String timeP = "%" + (7 + timePrecision) + "s";
         output.format ("  Step    Type  Period  " + timeP + "  "
               + timeP + "    Outcome   Group  " + timeP, "ArvTime",
               "QueueTime", "SrvTime");
         output.println ();
      }

      public void blocked (Router router, Contact contact, int bType) {}

      public void dequeued (Router router, DequeueEvent ev) {
         if (closed)
            return;
         final Contact contact = ev.getContact ();
         final int itr = getStep (contact);
         final int k = contact.getTypeId ();
         final int p = getPeriod (contact);
         final double a = contact.getArrivalTime ();
         final double q = contact.getTotalQueueTime ();
         final String timeP = "%" + (7 + timePrecision) + "." + timePrecision + "f";
         output.printf ("%6d  %6d  %6d  " + timeP + "  " + timeP
               + "  %9s  %6d  " + timeP, itr, k, p, a, q, "Abandoned", -1,
               Double.NaN);
         output.println ();
      }

      public void served (Router router, EndServiceEvent ev) {
         if (closed)
            return;
         final Contact contact = ev.getContact ();
         final int itr = getStep (contact);
         final int k = contact.getTypeId ();
         final int p = getPeriod (contact);
         final double a = contact.getArrivalTime ();
         final double q = contact.getTotalQueueTime ();
         final int i = contact.getLastAgentGroup ().getId ();
         final double s = contact.getTotalServiceTime ();
         final String timeP = "%" + (7 + timePrecision) + "." + timePrecision + "f";
         output.printf ("%6d  %6d  %6d  " + timeP + "  " + timeP
               + "  %9s  %6d  " + timeP, itr, k, p, a, q, "Served", i, s);
         output.println ();
      }

      private int getStep (Contact contact) {
         return currentRep;
      }

      public int getPeriod (Contact contact) {
         return pce.getPeriod (contact.getArrivalTime ());
      }
   }

   @Override
   void simulateOneDay () {
      super.simulateOneDay ();
      ++currentRep;
   }

   @Override
   void simulate (int n) {
      try {
         final Writer output = new FileWriter (traceFile);
         trace.init (output);
         trace.writeHeader ();
      }
      catch (final IOException ioe) {
         ioe.printStackTrace ();
      }
      currentRep = 0;
      try {
         super.simulate (n);
      }
      finally {
         trace.close ();
      }
   }

   public static void main (String[] args) {
      final SimpleMSKWithTrace s = new SimpleMSKWithTrace (new File (
            "contactTrace.log"), 3);
      s.simulate (NUMDAYS);
      s.printStatistics ();
   }
}
