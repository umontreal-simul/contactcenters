import java.io.File;
import java.util.Arrays;

import umontreal.iro.lecuyer.contactcenters.msk.CallCenterParamsConverter;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.AgentGroupManagerWithSchedule;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenterCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;

import umontreal.iro.lecuyer.util.TimeUnit;

public class GetParams {
   public static void main (String[] args)
   throws CallCenterCreationException {
      if (args.length != 1 && args.length != 2) {
         System.err.println ("Usage: java GetParams <call center params> " +
               "[period]");
         System.exit (1);
      }
      final String ccPsFn = args[0];
      final int mainPeriod = args.length >= 2 ? Integer.parseInt (args[1]) : 0;

      // Reading model parameters
      final CallCenterParamsConverter cnvCC = new CallCenterParamsConverter();
      final CallCenterParams ccPs = cnvCC.unmarshalOrExit (new File (ccPsFn));

      // Creating call center model
      final CallCenter cc = new CallCenter (ccPs);
      cc.create ();

      final int period = mainPeriod + 1;
      final TimeUnit unit = cc.getDefaultUnit();
      final int KI = cc.getNumInContactTypes();
      final double[] lambdas = new double[KI];
      for (int k = 0; k < KI; k++)
         lambdas[k] = cc.getArrivalProcess(k).getExpectedArrivalRateB (period);
      System.out.printf ("Arrival rates during main period %d: %s calls per %s%n",
          mainPeriod, Arrays.toString (lambdas), unit.getLongName());

      final int K = cc.getNumContactTypes();
      final double[] st = new double[K];
      for (int k = 0; k < K; k++)
         st[k] = cc.getCallFactory (k).getServiceTimeGen ().getMean (period);
      System.out.printf ("Mean service time during main period %d: %s %s%n" ,
                          mainPeriod, Arrays.toString (st), unit.getLongName());

      final int I = cc.getNumAgentGroups();
      final int[] staffing = new int[I];
      for (int i = 0; i < I; i++) {
         AgentGroupManager grp = cc.getAgentGroupManager (i);
         staffing[i] = grp.getEffectiveStaffing (mainPeriod);
         if (grp instanceof AgentGroupManagerWithSchedule) {
            AgentGroupManagerWithSchedule sched =
               (AgentGroupManagerWithSchedule) grp;
            System.out.printf ("Number of agents in shifts for group %d: %s%n",
                        i, Arrays.toString (sched.getEffectiveNumAgents ()));
            boolean[][] shiftMatrix = sched.getShiftMatrix ();
            System.out.println ("Shift matrix = {");
            for (boolean[] b :shiftMatrix)
               System.out.println (Arrays.toString (b));
            System.out.println ("}");
         }
      }
      System.out.printf ("Staffing during period %d: %s%n",
                         mainPeriod, Arrays.toString (staffing));
   }
}
