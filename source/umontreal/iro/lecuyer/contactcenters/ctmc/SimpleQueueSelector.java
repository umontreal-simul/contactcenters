package umontreal.iro.lecuyer.contactcenters.ctmc;

public class SimpleQueueSelector implements WaitingQueueSelector {
   public int selectWaitingQueue (CallCenterCTMC ctmc, int k, int tr) {
      assert ctmc.getNumContactTypes() == 1;
      if (ctmc.getNumContactsInQueue() > 0)
         return 0;
      return -1;
   }

   public double[] getRanks() {
      return new double[] {
            1
      };
   }
}
