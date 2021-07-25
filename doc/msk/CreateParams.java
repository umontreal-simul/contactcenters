import java.io.File;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.contactcenters.app.ArrivalProcessType;
import umontreal.iro.lecuyer.contactcenters.app.RouterPolicyType;
import umontreal.iro.lecuyer.contactcenters.app.params.ServiceLevelParams;
import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.params.AgentGroupParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.InboundTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.RouterParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ServiceTimeParams;
import umontreal.iro.lecuyer.contactcenters.params.MultiPeriodGenParams;

import umontreal.iro.lecuyer.probdist.ExponentialDist;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;
import umontreal.iro.lecuyer.xmlbind.params.TimeUnitParam;

public class CreateParams {
   static int K = 3;
   static double[] LAMBDAS = { 60, 120, 60 };
   static double[] MUS = { 60, 60, 60 };
   static double[] NUS = { 12, 6, 12 };
   static double AWT = 20.0;
   static double TARGETSL = 0.8;

   static int I = 2;
   static int[] STAFFING = { 1, 2 };

   public static MultiPeriodGenParams createExponentialGen (double rate) {
      final MultiPeriodGenParams gen = new MultiPeriodGenParams();
      gen.setDistributionClass (ExponentialDist.class.getSimpleName());
      final RandomVariateGenParams dgen = new RandomVariateGenParams();
      dgen.setParams(new double[] { rate });
      gen.setDefaultGen (dgen);
      return gen;
   }

   public static ServiceTimeParams createExponentialSTGen (double rate) {
      final ServiceTimeParams gen = new ServiceTimeParams();
      gen.setDistributionClass (ExponentialDist.class.getSimpleName());
      final RandomVariateGenParams dgen = new RandomVariateGenParams();
      dgen.setParams(new double[] { rate });
      gen.setDefaultGen (dgen);
      return gen;
   }

   public static void main (String[] args) throws DatatypeConfigurationException {
      final DatatypeFactory df = DatatypeFactory.newInstance();
      final CallCenterParams ccPs = new CallCenterParams();
      ccPs.setDefaultUnit (TimeUnitParam.HOUR);
      ccPs.setPeriodDuration (df.newDuration ("PT1H"));
      ccPs.setNumPeriods (1);
      for (int k = 0; k < K; k++) {
         final InboundTypeParams ipar = new InboundTypeParams ();
         ccPs.getInboundTypes().add (ipar);
         final ArrivalProcessParams apar = new ArrivalProcessParams ();
         ipar.setArrivalProcess (apar);
         apar.setType (ArrivalProcessType.POISSON.name());
         apar.setArrivals (new double[] { LAMBDAS[k] });
         ipar.setProbAbandon (new double[] { 0 });
         ipar.getServiceTimes().add (createExponentialSTGen (MUS[k]));
         ipar.setPatienceTime (createExponentialGen (NUS[k]));
      }
      for (int i = 0; i < I; i++) {
         final AgentGroupParams apar = new AgentGroupParams ();
         ccPs.getAgentGroups().add (apar);
         apar.setStaffing (new int[] { STAFFING[i] });
      }

      final RouterParams rpar = new RouterParams ();
      ccPs.setRouter (rpar);
      rpar.setRouterPolicy (RouterPolicyType.AGENTSPREF.name());
      final double[][] ranksGT = new double[I][K];
      for (int i = 0; i < I; i++)
         for (int k = 0; k < K; k++)
            ranksGT[i][k] = 1;
      rpar.setRanksGT (ArrayConverter.marshalArray (ranksGT));

      final ServiceLevelParams slp = new ServiceLevelParams ();
      ccPs.getServiceLevelParams().add (slp);
      final Duration[][] awt = new Duration[][] {
         new Duration[] { df.newDuration ((long)(AWT*1000)) }
      };
      slp.setAwt (ArrayConverter.marshalArrayNonNegative (awt));
      final double[][] target = new double[][] {
         new double[] { TARGETSL }
      };
      slp.setTarget (ArrayConverter.marshalArray (target));

      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      cnvCC.marshalOrExit (ccPs, new File ("testParams.xml"));
   }
}
