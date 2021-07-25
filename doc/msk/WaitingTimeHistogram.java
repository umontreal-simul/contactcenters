import static umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType.RATEOFABANDONMENTAFTERAWT;
import static umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType.RATEOFABANDONMENTBEFOREAWT;
import static umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType.RATEOFSERVICESAFTERAWT;
import static umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType.RATEOFSERVICESBEFOREAWT;

import java.io.File;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.util.*;
import umontreal.iro.lecuyer.charts.*;
import umontreal.iro.lecuyer.contactcenters.app.ContactCenterProgressBar;
import umontreal.iro.lecuyer.contactcenters.app.ObservableContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.RowType;
import umontreal.iro.lecuyer.contactcenters.app.SimParamsConverter;
import umontreal.iro.lecuyer.contactcenters.app.SimRandomStreamFactory;
import umontreal.iro.lecuyer.contactcenters.app.params.ServiceLevelParams;
import umontreal.iro.lecuyer.contactcenters.app.params.SimParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.doublealgo.Formatter;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

public class WaitingTimeHistogram {
   public static void main (String[] args) throws CallCenterCreationException,
         DatatypeConfigurationException {
      if (args.length <= 4) {
         System.err.println ("Usage: java CallSim <call center params>"
               + " <simulation params> <countServed> "
               + " <countAbandoned> awt1 awt2 ...");
         System.exit (1);
      }
      final String ccPsFn = args[0];
      final String simPsFn = args[1];
      final boolean countServed = Boolean.parseBoolean (args[2]);
      final boolean countAbandoned = Boolean.parseBoolean (args[3]);
      if (!countServed && !countAbandoned) {
         System.err.println
            ("One of countServed and countAbandoned must be true");
         System.exit (1);
      }

      double[] awt = new double[args.length - 4];
      for (int i = 0; i < awt.length; i++) {
         awt[i] = Double.parseDouble (args[i + 4]);
         if (awt[i] == 0) {
            System.err.println ("AWT should not be 0");
            System.exit (1);
         }
         if (i > 0 && awt[i] <= awt[i - 1]) {
            System.err.println ("AWTs must be increasing");
            System.exit (1);
         }
      }

      // Reading model parameters
      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter ();
      final CallCenterParams ccPs = cnvCC.unmarshalOrExit (new File (ccPsFn));

      // Reading simulation parameters
      final SimParamsConverter cnvSim = new SimParamsConverter ();
      final SimParams simPs = cnvSim.unmarshalOrExit (new File (simPsFn));

      // Setup service level parameters
      DatatypeFactory df = DatatypeFactory.newInstance ();
      ccPs.getServiceLevelParams ().clear ();
      for (int i = 0; i < awt.length; i++) {
         ServiceLevelParams slp = new ServiceLevelParams ();
         Duration d = df.newDuration ((long) (awt[i] * 1000));
         Duration[][] array = new Duration[][] { { d } };
         slp.setAwt (ArrayConverter.marshalArrayNonNegative (array));
         ccPs.getServiceLevelParams ().add (slp);
      }

      // Construct the simulator
      SimRandomStreamFactory.initSeed (simPs.getRandomStreams ());
      final ObservableContactCenterSim sim = new CallCenterSim (ccPs, simPs);
      sim.addContactCenterSimListener (new ContactCenterProgressBar ());

      // The remainder of the program is independent of the specific simulator
      sim.eval ();

      // Obtains matrices of results from the simulator
      DoubleMatrix2D inTarget, outTarget;
      if (countServed && !countAbandoned) {
         inTarget = sim.getPerformanceMeasure (RATEOFSERVICESBEFOREAWT);
         outTarget = sim.getPerformanceMeasure (RATEOFSERVICESAFTERAWT);
      }
      else if (countAbandoned && !countServed) {
         inTarget = sim.getPerformanceMeasure (RATEOFABANDONMENTBEFOREAWT);
         outTarget = sim.getPerformanceMeasure (RATEOFABANDONMENTAFTERAWT);
      }
      else {
         inTarget = sim.getPerformanceMeasure (RATEOFSERVICESBEFOREAWT);
         outTarget = sim.getPerformanceMeasure (RATEOFSERVICESAFTERAWT);
         DoubleMatrix2D inTarget2 = sim
               .getPerformanceMeasure (RATEOFABANDONMENTBEFOREAWT);
         DoubleMatrix2D outTarget2 = sim
               .getPerformanceMeasure (RATEOFABANDONMENTAFTERAWT);
         inTarget.assign (inTarget2, Functions.plus);
         outTarget.assign (outTarget2, Functions.plus);
      }

      // Creates the matrix of counts
      DoubleMatrix2D res = new DenseDoubleMatrix2D (awt.length + 1,
                                inTarget.rows () / awt.length);
      final int p = inTarget.columns () - 1;
      for (int tr = 0; tr < res.rows (); tr++)
         for (int k = 0; k < res.columns (); k++) {
            if (tr == 0) {
               final double v = inTarget.get (k, p);
               res.set (tr, k, v);
            } else if (tr == res.rows () - 1) {
               final int idx = (tr - 1) * res.columns () + k;
               final double v = outTarget.get (idx, p);
               res.set (tr, k, v);
            } else {
               final int idx = (tr - 1) * res.columns () + k;
               double v1 = inTarget.get (idx, p);
               double v2 = inTarget.get (idx + res.columns (), p);
               res.set (tr, k, v2 - v1);
            }
         }

      // Formats the matrix as a string
      String[] threshNames = new String[res.rows ()];
      String[] inboundTypeNames = new String[res.columns ()];
      for (int tr = 0; tr < threshNames.length; tr++) {
         if (tr == 0)
            threshNames[tr] = "W<=" + awt[0];
         else if (tr == threshNames.length - 1)
            threshNames[tr] = "W>" + awt[awt.length - 1];
         else
            threshNames[tr] = awt[tr - 1] + "<W<=" + awt[tr];
      }
      for (int k = 0; k < inboundTypeNames.length; k++)
         inboundTypeNames[k] = RowType.INBOUNDTYPE.getName (sim, k);
      System.out.println (new Formatter ().toTitleString (res, threshNames,
            inboundTypeNames, "Thresholds", "Type",
            "Distribution of waiting time", null));

      // Creates an histogram for the last column of the matrix
      double[] bounds = new double[awt.length + 2];
      System.arraycopy (awt, 0, bounds, 1, awt.length);
      bounds[0] = 0;
      bounds[bounds.length - 1] = bounds[bounds.length - 2] + 10;
      int[] counts = new int[awt.length + 1];
      for (int tr = 0; tr < counts.length; tr++)
         counts[tr] = (int) Math.round (res.get (tr, res.columns () - 1));
      HistogramChart chart = new HistogramChart (
         "Distribution of the waiting time", "Time", "Counts", counts, bounds);
      chart.view (800, 600);
   }
}
