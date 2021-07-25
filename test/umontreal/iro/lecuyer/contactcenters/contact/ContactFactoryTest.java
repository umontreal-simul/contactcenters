package umontreal.iro.lecuyer.contactcenters.contact;

import junit.framework.TestCase;

public class ContactFactoryTest extends TestCase {
   private ContactFactory factory = null;

   @Override
   public void setUp () {
      factory = new SimpleContactFactory ();
   }

   @Override
   public void tearDown () {
      factory = null;
   }

   public void testNewContact () {
      final Contact ct1 = factory.newInstance ();
      assertNotNull (ct1);
      final Contact ct2 = factory.newInstance ();
      assertNotNull (ct2);
      assertTrue (ct1 != ct2);
   }

   /*
    * public void testRecycle() { Contact ct1 = factory.newInstance();
    * assertNotNull (ct1); factory.dispose (ct1); Contact ct2 =
    * factory.newInstance(); assertNotNull (ct2); assertTrue (ct1 == ct2); }
    */
}
