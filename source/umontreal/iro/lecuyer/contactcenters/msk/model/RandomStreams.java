package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import umontreal.iro.lecuyer.contactcenters.RandomStreamUtil;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallCenterParams;
import umontreal.ssj.rng.CloneableRandomStream;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.rng.RandomStreamFactory;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Encapsulates the random streams used by the blend/multi-skill call
 * center simulator.  The model uses one random stream for
 * each type of random variate for better synchronization
 * when using common random numbers.
 * This class creates, stores, and manages all these
 * random streams.
 *
 * Often, this class is not used directly since
 * the {@link CallCenter} class provides a constructor
 * which implicitly creates the random streams.
 * However, it can be useful to get the \texttt{RandomStreams}
 * object of a model, using
 * the {@link CallCenter#getRandomStreams()}
 * method, in order to retrieve the reference to
 * a particular random stream, or to pass the random
 * streams to a new model.
 * Creating several models with the same
 * random streams can improve
 * synchronization when comparing systems with
 * common random numbers.
 *
 * However, if several instances of {@link CallCenter} are used in parallel,
 * each instance should have its own random streams.
 * The {@link #clone()} method can be used if seeds must be shared between
 * two instances of this class.
 */
public class RandomStreams implements Cloneable
{
   // Add a constant in these enums to create a new
   // random stream.
   // However, creating a new stream will change
   // the seeds for other streams.
   /**
    * Number of random streams for a contact factory.
    */
   public static final int NUMFACTORYSTREAMS = CallFactoryStreamType.values ().length;
   /**
    * Number of random streams for a contact factory.
    */
   public static final int NUMFACTORYSTREAMS2 = CallFactoryStreamType2.values ().length;
   /**
    * Number of random streams for arrival processes.
    */
   public static final int NUMAPSTREAMS = ArrivalProcessStreamType.values ().length;
   /**
    * Number of streams for dialers.
    */
   public static final int NUMDIALERSTREAMS = DialerStreamType.values ().length;
   /**
    * Number of streams for agent groups.
    */
   public static final int NUMAGENTGROUPSTREAMS = AgentGroupStreamType
         .values ().length;

   // Sets of random streams, to perform operations on
   // all streams.
   private Set<RandomStream> streamsInit = new HashSet<RandomStream> ();
   private Set<RandomStream> streamsSim = new HashSet<RandomStream> ();
   // Factory class used to create random streams
   private RandomStreamFactory rsf;

   // A field is needed for every type of random stream
   // The method createStreams must
   // be adjusted to initialize any added field.
   private RandomStream rsmCt;
   private RandomStream rsmB;
   private RandomStream[][] factoryStreams;
   private RandomStream[][] factoryStreams2;
   private RandomStream[][] apStreams;
   private RandomStream[][] dialerStreams;
   private RandomStream[][] agStreams;
   private RandomStream streamAgentSelection;
   private RandomStream streamContactSelection;
   private RandomStream[] probCTArvProcStreams;
   private RandomStream[] probCTDialersStreams;

   private ArrayList < ArrayList < RandomStream >> agServiceTimeStream = new ArrayList < ArrayList < RandomStream >> ();

   private ArrayList < ArrayList < ArrayList < RandomStream >>> agServiceTimeStream1 =
      new ArrayList < ArrayList < ArrayList < RandomStream >>> ();

   /**
    * Creates a new set of random streams using
    * the random stream factory \texttt{rsf},
    * and the call center parameters \texttt{ccParams}.
    * The parameters are used to determine
    * the number of call types, agent groups, etc., in order
    * to set the number of streams of each type to create.
    *
    * This method sets the random stream factory returned by
    * {@link #getRandomStreamFactory()}, and
    * calls {@link #createStreams(CallCenterParams)}.
    * @param rsf the random stream factory used to
    * create each {@link RandomStream} instance.
    * @param ccParams the parameters of the call center
    * for which random streams are created.
    * @exception NullPointerException if \texttt{rsf}
    * or \texttt{ccParams} are \texttt{null}.
    */
   public RandomStreams (RandomStreamFactory rsf, CallCenterParams ccParams)
   {
      if (rsf == null)
         throw new NullPointerException (
            "The random stream factory must not be null");
      this.rsf = rsf;
      createStreams (ccParams);
   }

   /**
    * Creates the necessary random streams for supporting $K=\Ki+\Ko$ contact
    * types, and $I$
    * agent groups. This method reuses every stream associated with this object; it
    * only creates new streams when needed. Consequently, it cannot be used to
    * set new random seeds for every stream. Setting new seeds can be done by
    * constructing a new \texttt{RandomStreams} instance.
    *
    * @param ccParams the parameters of the call center.
    * @exception NullPointerException if \texttt{ccParams}
    * is \texttt{null}.
    */
   public void createStreams (CallCenterParams ccParams)
   {
      if (ccParams == null)
         throw new NullPointerException ("ccParams must not be null");
      // These create streams using rsf, but also adds them
      // to the appropriate sets.
      //final RandomStreamFactory rssrfInit = new RSSFactory (rsf, streamsInit);
      final RandomStreamFactory rssrfSim = new RSSFactory (rsf, streamsSim);
      if (rsmCt == null)
         rsmCt = rssrfSim.newInstance ();
      factoryStreams = RandomStreamUtil.createRandomStreamMatrix
                       (factoryStreams, ccParams.getInboundTypes().size() + ccParams.getOutboundTypes().size(),
                        NUMFACTORYSTREAMS, rssrfSim);
      apStreams = RandomStreamUtil.createRandomStreamMatrix
                  (apStreams, ccParams.getInboundTypes().size() + ccParams.getArrivalProcesses().size(),
                   NUMAPSTREAMS, rssrfSim);
      dialerStreams = RandomStreamUtil.createRandomStreamMatrix
                      (dialerStreams, ccParams.getOutboundTypes().size() + ccParams.getDialers().size(),
                       NUMDIALERSTREAMS, rssrfSim);
      if (rsmB == null)
         rsmB = rssrfSim.newInstance ();
      agStreams = RandomStreamUtil.createRandomStreamMatrix
                  (agStreams, ccParams.getAgentGroups().size(),
                   NUMAGENTGROUPSTREAMS, rssrfSim);
      if (streamAgentSelection == null)
         streamAgentSelection = rssrfSim.newInstance ();
      if (streamContactSelection == null)
         streamContactSelection = rssrfSim.newInstance ();
      final int numArvProcMT = ccParams.getArrivalProcesses ().size ();
      probCTArvProcStreams = RandomStreamUtil.createRandomStreamArray (probCTArvProcStreams, numArvProcMT, rssrfSim);
      final int numDialersMT = ccParams.getDialers ().size ();
      probCTDialersStreams = RandomStreamUtil.createRandomStreamArray (probCTDialersStreams, numDialersMT, rssrfSim);
      factoryStreams2 = RandomStreamUtil.createRandomStreamMatrix
                        (factoryStreams2, ccParams.getInboundTypes().size() + ccParams.getOutboundTypes().size(),
                         NUMFACTORYSTREAMS2, rssrfSim);
      moveToInit ();

      for (int i = 0;i < ccParams.getAgentGroups().size();i++) {  
         for (int j = 0;j < ccParams.getAgentGroups().get(i).getAgents().size();j++) {
            ArrayList<RandomStream> list2 = new ArrayList<RandomStream>();
            for (int k = 0;k < ccParams.getAgentGroups().get(i).getAgents().get(j).getServiceTime().size();k++) {
               list2.add(rsmCt = rssrfSim.newInstance ());
            }
            agServiceTimeStream.add(list2);
         }


      }

      for (int i = 0;i < ccParams.getAgentGroups().size();i++) {
         ArrayList < ArrayList < RandomStream >> list1 = new ArrayList < ArrayList < RandomStream >> ();
         for (int j = 0;j < ccParams.getAgentGroups().get(i).getAgents().size();j++) {
            ArrayList<RandomStream> list2 = new ArrayList<RandomStream>();
            for (int k = 0;k < ccParams.getAgentGroups().get(i).getAgents().get(j).getServiceTime().size();k++) {
               list2.add(rsmCt = rssrfSim.newInstance ());
            }
            list1.add(list2);
         }

         agServiceTimeStream1.add(list1);
      }



   }

   private void moveToInit (RandomStream stream)
   {
      if (streamsSim.contains (stream)) {
         streamsSim.remove (stream);
         streamsInit.add (stream);
      }
   }

   private void moveToInit (RandomStream[][] streams, int idx)
   {
      for (final RandomStream[] element : streams) {
         if (streamsSim.contains (element[idx])) {
            streamsSim.remove (element[idx]);
            streamsInit.add (element[idx]);
         }
      }
   }

   private void moveToInit (RandomStream[] streams)
   {
      for (RandomStream element : streams) {
         moveToInit (element);
      }
   }

   public void moveToInit ()
   {
      moveToInit (rsmCt);
      moveToInit (rsmB);
      moveToInit (apStreams, ArrivalProcessStreamType.RATES.ordinal ());
   }

   /**
    * Returns the random stream factory used by
    * the {@link #createStreams(CallCenterParams)} method of
    * this object to create random
    * streams.
    *
    * @return the associated random stream factory.
    */
   public RandomStreamFactory getRandomStreamFactory ()
   {
      return rsf;
   }

   /**
    * Sets the associated random stream factory to \texttt{rsf}.
    * The new factory will only affect streams created
    * by subsequent calls to {@link #createStreams(CallCenterParams)},
    * not already created streams.
    *
    * @param rsf
    *           the new random stream factory.
    * @exception NullPointerException
    *               if \texttt{rsf} is \texttt{null}.
    */
   public void setRandomStreamFactory (RandomStreamFactory rsf)
   {
      if (rsf == null)
         throw new NullPointerException (
            "The random stream factory must not be null");
      this.rsf = rsf;
   }

   /**
    * Returns the set regrouping random streams used during the
    * initialization of replications only.
    * Streams in this set can, for example,
    * set the busyness factor for the day,
    * the total (random) number of arrivals, etc.
    *
    * @return the set of random streams used for initialization.
    */
   public Set<RandomStream> getRandomStreamsInit ()
   {
      return streamsInit;
   }

   /**
    * Returns the set of random streams regrouping random streams used during
    * the whole simulation.
    * These streams may, for example, generate
    * inter-arrival, patience, and service times.
    *
    * @return the set of random streams.
    */
   public Set<RandomStream> getRandomStreamsSim ()
   {
      return streamsSim;
   }

   /**
    * Returns the random stream used for generating contact type indices while
    * the system is initialized non-empty, for a simulation
    * on an infinite horizon using batch means.
    *
    * @return the random stream for contact type indices.
    */
   public RandomStream getStreamCT ()
   {
      return rsmCt;
   }

   /**
    * Returns the random stream used for the global busyness factor.
    * This stream is used only at the beginning of a replication,
    * for a finite-horizon simulation, if a distribution
    * was given for the busyness factor $B$ of the day.
    *
    * @return the random stream used for the global busyness factor.
    */
   public RandomStream getStreamB ()
   {
      return rsmB;
   }

   /**
    * Returns the random stream of type \texttt{s} used by the contact factory
    * with index \texttt{k}.
    *
    * @param k
    *           the index of the call factory.
    * @param s
    *           the type of the stream.
    * @return the random stream.
    */
   public RandomStream getCallFactoryStream (int k, CallFactoryStreamType s)
   {
      return factoryStreams[k][s.ordinal ()];
   }

   /**
    * Similar to {@link #getCallFactoryStream(int,CallFactoryStreamType)},
    * for a complementary set of
    * random streams.
    * These streams, used for call transfer and virtual
    * queueing, were added at a later time, so
    * a second set was used to avoid
    * changing the seeds of other streams.
    *
    * @param k
    *           the index of the call factory.
    * @param s
    *           the type of the complementary stream.
    * @return the random stream.
    */
   public RandomStream getCallFactoryStream2 (int k, CallFactoryStreamType2 s)
   {
      return factoryStreams2[k][s.ordinal ()];
   }

   /**
    * Returns the random stream of type \texttt{s} used by the arrival process
    * with index \texttt{ki}.
    *
    * @param ki
    *           the index of the arrival process.
    * @param s
    *           the type of the stream.
    * @return the random stream.
    */
   public RandomStream getArrivalProcessStream (int ki,
         ArrivalProcessStreamType s)
   {
      return apStreams[ki][s.ordinal ()];
   }

   /**
    * Returns the random stream used to select generated call type
    * for the $ki$-th arrival process generating calls of multiple types.
    * @param ki the index of the arrival process.
    * @return the random stream.
    */
   public RandomStream getArrivalProcessPStream (int ki)
   {
      return probCTArvProcStreams[ki];
   }

   /**
    * Returns the random stream of type \texttt{s} used by the dialer
    * with index \texttt{ko}.
    *
    * @param ko
    *           the index of the dialer.
    * @param s
    *           the type of the stream.
    * @return the random stream.
    */
   public RandomStream getDialerStream (int ko, DialerStreamType s)
   {
      return dialerStreams[ko][s.ordinal ()];
   }

   /**
    * Returns the random stream used to select generated call type
    * for the $ko$-th dialer generating calls of multiple types.
    * @param ko the index of the dialer.
    * @return the random stream.
    */
   public RandomStream getDialerPStream (int ko)
   {
      return probCTDialersStreams[ko];
   }

   /**
    * Returns the random stream of type \texttt{s} used by the agent group
    * \texttt{i}.
    *
    * @param i
    *           the index of the agent group.
    * @param s
    *           the type of the stream.
    * @return the random stream.
    */
   public RandomStream getAgentGroupStream (int i, AgentGroupStreamType s)
   {
      return agStreams[i][s.ordinal ()];
   }

   /**
    * Returns the random stream used for agent selection
    * during routing, if agent selection is randomized.
    * @return the random stream used for agent selection.
    */
   public RandomStream getStreamAgentSelection()
   {
      return streamAgentSelection;
   }

   /**
    * Returns the random stream used for contact selection
    * during routing, if contact selection is randomized.
    * @return the random stream used for contact selection.
    */
   public RandomStream getStreamContactSelection()
   {
      return streamContactSelection;
   }

   private static final class RSSFactory implements RandomStreamFactory
   {
      private RandomStreamFactory inner;
      private Set<RandomStream> rss;

      public RSSFactory (RandomStreamFactory inner, Set<RandomStream> rss)
      {
         assert inner != null;
         assert rss != null;
         this.inner = inner;
         this.rss = rss;
      }

      public RandomStream newInstance ()
      {
         final RandomStream stream = inner.newInstance ();
         rss.add (stream);
         return stream;
      }
   }

   /**
    * Creates a clone of this object and all
    * the contained random streams.
    * This method creates a copy of this object,
    * and clones every random stream by casting
    * them to {@link CloneableRandomStream}
    * and calling {@link #clone()}.
    * Each generator in the cloned object has
    * the same properties and seeds as the
    * corresponding generator in the original object.
    @exception ClassCastException if at least one encapsulated random stream does not
    implement the {@link CloneableRandomStream} interface.
    */
   public RandomStreams clone()
   {
      RandomStreams cpy;
      try {
         cpy = (RandomStreams)super.clone ();
      } catch (CloneNotSupportedException cne) {
         throw new InternalError();
      }
      cpy.streamsInit = new HashSet<RandomStream>();
      cpy.streamsSim = new HashSet<RandomStream>();
      for (Field f : RandomStreams.class.getDeclaredFields ()) {
         if (Modifier.isStatic (f.getModifiers ()))
            continue;
         if (RandomStream.class.isAssignableFrom (f.getType ())) {
            CloneableRandomStream rs;
            try {
               rs = CloneableRandomStream.class.cast (f.get (this));
            } catch (IllegalAccessException e) {
               continue;
            }
            if (rs == null)
               continue;
            RandomStream rsCpy = rs.clone ();
            try {
               f.set (cpy, rsCpy);
            } catch (IllegalAccessException e) {
               e.printStackTrace();
               continue;
            }
            cpy.streamsSim.add (rsCpy);
         } else if (RandomStream[].class.isAssignableFrom (f.getType ())) {
            RandomStream[] rs;
            try {
               rs = RandomStream[].class.cast (f.get (this));
            } catch (IllegalAccessException e) {
               continue;
            }
            if (rs == null)
               continue;
            RandomStream[] rsCpy = rs.clone ();
            for (int i = 0; i < rsCpy.length; i++)
               rsCpy[i] = ((CloneableRandomStream)rs[i]).clone ();
            try {
               f.set (cpy, rsCpy);
            } catch (IllegalAccessException e) {
               e.printStackTrace();
               continue;
            }
            for (RandomStream r : rsCpy)
               cpy.streamsSim.add (r);
         } else if (RandomStream[][].class.isAssignableFrom (f.getType ())) {
            RandomStream[][] rs;
            try {
               rs = RandomStream[][].class.cast (f.get (this));
            } catch (IllegalAccessException e) {
               continue;
            }
            if (rs == null)
               continue;
            RandomStream[][] rsCpy = ArrayUtil.deepClone (rs, false);
            for (int i = 0; i < rsCpy.length; i++)
               for (int j = 0; j < rsCpy[i].length; j++)
                  rsCpy[i][j] = ((CloneableRandomStream)rsCpy[i][j]).clone ();
            try {
               f.set (cpy, rsCpy);
            } catch (IllegalAccessException e) {
               e.printStackTrace();
               continue;
            }
            for (RandomStream[] r : rsCpy)
               for (RandomStream r2 : r)
                  cpy.streamsSim.add (r2);
         }
      }
      cpy.moveToInit ();
      return cpy;
   }

   /**
    * Returns a list that contains the random streams of each service time
    * distribution parameter for every agent.
    * The first (outer) list represents the agent groups,
    * the middle list represents each agent in a group, and
    * the inner list represents the random stream of each service time distribution parameter.
    *
    * @return a list that contains the random streams of each service time
    * distribution parameter.
    */
   public ArrayList<ArrayList<ArrayList<RandomStream> > > getAgServiceTimeStream1()
   {
      return agServiceTimeStream1;
   }
}
