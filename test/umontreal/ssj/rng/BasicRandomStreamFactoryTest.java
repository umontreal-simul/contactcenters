package umontreal.ssj.rng;

import junit.framework.TestCase;

public class BasicRandomStreamFactoryTest extends TestCase {
   BasicRandomStreamFactory rsf;

   public BasicRandomStreamFactoryTest (String name) {
      super (name);
   }

   @Override
   public void setUp () {
      rsf = new BasicRandomStreamFactory (MRG32k3a.class);
   }

   @Override
   public void tearDown () {
      rsf = null;
   }

   public void testNewInstance () {
      final RandomStream stream = rsf.newInstance ();
      assertTrue ("newInstance() creates a MRG32k3a stream",
            stream instanceof MRG32k3a);
   }

   public void testNewInstanceWithOtherClass () {
      rsf.setRandomStreamClass (LFSR113.class);
      final RandomStream stream = rsf.newInstance ();
      assertTrue ("newInstance() creates a LSFR113 stream",
            stream instanceof LFSR113);
   }
}
