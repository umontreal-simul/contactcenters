package umontreal.iro.lecuyer.contactcenters.app;


/**
 * Contact center simulation listener displaying a progress bar
 * for the simulation.
 * This listener shows the number of completed steps over
 * the total number of steps to complete before the simulation
 * ends, with a visual progress indicator.
 */
public class ContactCenterProgressBar implements ContactCenterSimListener {
   private static int LINEWIDTH = 70;
   private static String eraser;
   static {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < LINEWIDTH; i++)
         sb.append ('\b');
      eraser = sb.toString ();
   }
   
   private int total;
   private boolean first = true;

   public ContactCenterProgressBar () {
   }

   public void simulationExtended (ObservableContactCenterSim sim,
         int newNumTargetSteps) {
      total = newNumTargetSteps;
      updateProgress (sim);
   }

   public void simulationStarted (ObservableContactCenterSim sim,
         int numTargetSteps) {
      total = numTargetSteps;
      first = true;
      updateProgress (sim);
   }

   public void simulationStopped (ObservableContactCenterSim sim,
         boolean aborted) {
      System.err.println ();
      first = true;
   }

   public void stepDone (ObservableContactCenterSim sim) {
      updateProgress (sim);
   }

   private void updateProgress (ContactCenterSim sim) {
      if (first)
         first = false;
      else
         System.err.print (eraser);
      // Code inspired from http://luka.tnode.com/posts/view/157
      // but without JLine
      // We use System.err for the progress bar to appear
      // even if the output is redirected.
      int completed = sim.getCompletedSteps ();
      int w = LINEWIDTH;
      String totalStr = String.valueOf (total);
      StringBuilder sb = new StringBuilder();
      sb.append (String.format ("%0" + totalStr.length () + "d/%s [",
            completed, totalStr));
      int numBars = w - sb.length () - 1;
      int progress = (completed * numBars) / total;
      for (int i = 0; i < progress; i++)
         sb.append ('=');
      for (int i = progress; i < numBars; i++)
         sb.append (' ');
      sb.append (']');
      System.err.print (sb.toString ());
   }
}
