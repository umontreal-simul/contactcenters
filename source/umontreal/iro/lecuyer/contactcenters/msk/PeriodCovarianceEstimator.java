/**
 * 
 */
package umontreal.iro.lecuyer.contactcenters.msk;

import umontreal.iro.lecuyer.contactcenters.app.ContactCenterSim;
import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.list.ListOfTalliesWithCovariance;
import umontreal.ssj.stat.matrix.MatrixOfObservationListener;
import umontreal.ssj.stat.matrix.MatrixOfStatProbes;
import umontreal.ssj.stat.matrix.MatrixOfTallies;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class PeriodCovarianceEstimator implements MatrixOfObservationListener {
   private ListOfTalliesWithCovariance<Tally> ta;
   private int row;

   public PeriodCovarianceEstimator (ContactCenterSim sim,
         PerformanceMeasureType pm, int row) {
      final MatrixOfTallies<?> mta = sim.getMatrixOfTallies (pm);
      ta = ListOfTalliesWithCovariance.createWithTally (mta.columns ());
      mta.setBroadcasting (true);
      mta.addMatrixOfObservationListener (this);
      if (row < 0)
         this.row = row + pm.rows (sim);
      else
         this.row = row;
   }

   public void init () {
      ta.init ();
   }

   public DoubleMatrix2D getCovarianceMatrix () {
      final DoubleMatrix2D m = new DenseDoubleMatrix2D (ta.size (), ta.size ());
      ta.covariance (m);
      return m;
   }

   public DoubleMatrix2D getCorrelationMatrix () {
      final DoubleMatrix2D m = new DenseDoubleMatrix2D (ta.size (), ta.size ());
      ta.correlation (m);
      return m;
   }

   public void newMatrixOfObservations (MatrixOfStatProbes<?> msp,
         DoubleMatrix2D obs) {
      ta.add (obs.viewRow (row));
   }
}
