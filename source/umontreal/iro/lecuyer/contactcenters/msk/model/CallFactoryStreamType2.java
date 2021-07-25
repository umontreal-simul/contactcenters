package umontreal.iro.lecuyer.contactcenters.msk.model;

/**
 * Complementary random streams for
 * call factories.
 * See {@link RandomStreams#getCallFactoryStream2(int,CallFactoryStreamType2)}
 * for more information.
 */
public enum CallFactoryStreamType2 {
   
   /**
    * Random stream for probability of going into a virtual queue. 
    */
   VQUEUE,
   /**
    * Random stream for probability of transfer.
    */
   PROBTRANSFER,
   /**
    * Random stream used to generate transfer delays.
    */
   TRANSFERTIME,
   /**
    * Random stream used to generate conference or pre-service times
    * with secondary agent, for call transfer.
    */
   HANDOFF
}
