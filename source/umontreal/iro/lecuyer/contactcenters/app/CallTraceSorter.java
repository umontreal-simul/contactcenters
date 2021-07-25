package umontreal.iro.lecuyer.contactcenters.app;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.app.trace.FileContactTrace;
import umontreal.ssj.util.PrintfFormat;

/**
 * Reads a call trace produced by a call center simulator using simulation
 * parameters from an instance of {@link SimParams}, sorts the calls by
 * increasing arrival time, and writes the sorted trace into a file.
 */
public class CallTraceSorter {
   /**
    * For each line read from \texttt{reader}, creates an object representing a
    * traced call, puts the objects in a sorted set, and returns that set.
    * 
    * @param reader
    *           the reader to obtain the trace from.
    * @return the read and sorted trace.
    * @throws IOException
    *            if an I/O error occurs.
    */
   public static SortedSet<TracedCall> readTrace (Reader reader)
         throws IOException {
      final LineNumberReader input = new LineNumberReader (reader);
      final SortedSet<TracedCall> res = new TreeSet<TracedCall> ();
      String line = input.readLine (); // Skip the header
      while ((line = input.readLine ()) != null)
         try {
            res.add (new TracedCall (line));
         }
         catch (final IllegalArgumentException iae) {
            System.err.println ("Line " + input.getLineNumber ()
                  + ": error parsing line: " + iae.getMessage ());
         }
      return res;
   }

   /**
    * For each traced call in the collection \texttt{calls}, writes one line on
    * \texttt{writer} by using {@link TracedCall#toString}.
    * 
    * @param writer
    *           the output writer.
    * @param calls
    *           the traced calls to write.
    * @param timePrecision
    *           the number of decimal digits of precision for time durations.
    * @throws IOException
    *            if an I/O error occurs.
    */
   public static void writeTrace (Writer writer,
         Collection<? extends TracedCall> calls, int timePrecision)
         throws IOException {
      final BufferedWriter output = new BufferedWriter (writer);
      output.write (FileContactTrace.getHeader (timePrecision) + "\n");
      TracedCall.timePrecision = timePrecision;
      for (final TracedCall call : calls)
         output.write (call.toString () + "\n");
      writer.flush ();
   }

   /**
    * Represents a call that has been traced.
    */
   public static class TracedCall implements Comparable<TracedCall> {
      private static final Pattern traceP = Pattern
            .compile ("^\\s*([\\d+-]+)\\s*(\\d+)\\s*(\\d+)"
                  + "\\s*([\\d\\.]+)\\s*(\\S+)" + "\\s*(\\S+)\\s*([\\d+-]+)"
                  + "\\s*(\\S+)\\s*$");
      public static int timePrecision = 3;
      private int step;
      private int type;
      private int period;
      private double arvTime;
      private double queueTime;
      private String outcome;
      private int group;
      private double srvTime;

      /**
       * Constructs a new traced call from the line \texttt{line} obtained from
       * a trace file.
       * 
       * @param line
       *           the line from the trace file.
       * @exception IllegalArgumentException
       *               if a parse error occurs.
       */
      public TracedCall (String line) {
         final Matcher m = traceP.matcher (line);
         if (!m.matches ())
            throw new IllegalArgumentException ("Invalid line");
         step = Integer.parseInt (m.group (1));
         type = Integer.parseInt (m.group (2));
         period = Integer.parseInt (m.group (3));
         arvTime = Double.parseDouble (m.group (4));
         queueTime = Double.parseDouble (m.group (5));
         outcome = m.group (6);
         group = Integer.parseInt (m.group (7));
         srvTime = Double.parseDouble (m.group (8));
      }

      /**
       * Constructs a new traced call using parameters.
       * 
       * @param step
       *           the batch/replication of the call.
       * @param type
       *           the type of the call.
       * @param period
       *           the period of arrival of the call.
       * @param arvTime
       *           the arrival time of the call.
       * @param queueTime
       *           the waiting time of the call.
       * @param outcome
       *           the outcome of the call.
       * @param group
       *           the agent group of the served call.
       * @param srvTime
       *           the service time of the call.
       */
      public TracedCall (int step, int type, int period, double arvTime,
            double queueTime, String outcome, int group, double srvTime) {
         super ();
         this.step = step;
         this.type = type;
         this.period = period;
         this.arvTime = arvTime;
         this.queueTime = queueTime;
         this.outcome = outcome;
         this.group = group;
         this.srvTime = srvTime;
      }

      public double getArvTime () {
         return arvTime;
      }

      public int getGroup () {
         return group;
      }

      public String getOutcome () {
         return outcome;
      }

      public int getPeriod () {
         return period;
      }

      public double getQueueTime () {
         return queueTime;
      }

      public double getSrvTime () {
         return srvTime;
      }

      public int getStep () {
         return step;
      }

      public int getType () {
         return type;
      }

      /**
       * Compares this call to another call. The calls are ordered in ascending
       * number of step (batch/replication). Calls with the same step are
       * ordered in ascending arrival time. Calls with the same step and arrival
       * time are ordered in ascending type number.
       * 
       * @param o
       *           the other call.
       * @return the result of the comparison.
       */
      public int compareTo (TracedCall o) {
         if (step < o.step)
            return -1;
         if (step > o.step)
            return 1;
         if (arvTime < o.arvTime)
            return -1;
         if (arvTime > o.arvTime)
            return 1;
         if (type < o.type)
            return -1;
         if (type > o.type)
            return 1;
         return 0;
      }

      @Override
      public boolean equals (Object o) {
         if (!(o instanceof TracedCall))
            return false;
         final TracedCall tc = (TracedCall) o;
         return step == tc.step && type == tc.type && arvTime == tc.arvTime;
      }

      @Override
      public int hashCode () {
         int res = 17;
         res = 37 * res + step;
         res = 37 * res + type;
         final long l = Double.doubleToLongBits (arvTime);
         res = 37 * res + (int) (l ^ l >>> 32);
         return res;
      }

      @Override
      public String toString () {
         final PrintfFormat line = new PrintfFormat ();
         line.append (6, step).append ("  ").append (6, type).append ("  ")
               .append (6, period).append ("  ").append (7 + timePrecision,
                     timePrecision, arvTime).append ("  ").append (
                     7 + timePrecision, timePrecision, queueTime).append ("  ")
               .append (9, outcome).append ("  ").append (6, group).append (
                     "  ").append (7 + timePrecision, timePrecision, srvTime);
         return line.toString ();
      }
   }

   /**
    * Main method taking as arguments the name of an input trace file and the
    * name of an output file.
    * 
    * @param args
    *           the command-line arguments.
    * @throws IOException
    *            if an I/O error occurs.
    */
   public static void main (String[] args) throws IOException {
      if (args.length != 3) {
         System.err
               .println ("Usage: java umontreal.iro.lecuyer.contactcenters.app.CallTraceSorter "
                     + "inputfile outputfile timeprecision");
         System.exit (1);
      }
      final String input = args[0];
      final String output = args[1];
      final int timePrecision = Integer.parseInt (args[2]);

      final Reader reader = new FileReader (input);
      System.out.println ("Reading trace file");
      final SortedSet<TracedCall> calls = readTrace (reader);
      reader.close ();

      final Writer writer = new FileWriter (output);
      System.out.println ("Writing sorted trace file");
      writeTrace (writer, calls, timePrecision);
      writer.close ();
   }
}
