import umontreal.iro.lecuyer.probdist.EmpiricalDist;
import umontreal.iro.lecuyer.randvar.KernelDensityGen;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.rng.RandomStream;

public class ExpKernelDensityGen extends KernelDensityGen {
   public ExpKernelDensityGen (RandomStream stream, EmpiricalDist dist) {
      super (stream, dist, new NormalGen (stream));
      setPositiveReflection (true);
   }

   @Override
   public double nextDouble () {
      return Math.exp (super.nextDouble ());
   }
}
