package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.ServiceLevelParamReadHelper;
import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.msk.model.Call;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.SegmentInfo;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
//import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouter;
import umontreal.iro.lecuyer.contactcenters.router.ExitedContactListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.server.EndServiceEvent;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.SumMatrix;
import umontreal.ssj.stat.mperiods.SumMatrixSW;
import cern.jet.math.Functions;
import java.util.ArrayList;
import java.text.*;
/**
 * Contains and updates call-by-call measures for
 * a call center model.
 * This includes the number of
 * arrivals, the number of services, etc.
 * Any object of this class encapsulates matrices of sums
 * for each type of call-by-call measure.
 * It is also an exited-contact listener which can be notified
 * each time a call leaves the system, for statistical collecting.
 */
public class CallByCallMeasureManager implements ExitedContactListener
{
   private CallCenter cc;
   private StatPeriod statP;

   private SumMatrix numArriv;
   private SumMatrix numWrong;
   private SumMatrix numServed;
   private SumMatrix numBlocked;
   private SumMatrix numAbandoned;
   private SumMatrix numAbandonedBeforeAwt;
   private SumMatrix numAbandonedAfterAwt;
   private SumMatrix numDelayed;
   private SumMatrix numServedBeforeAWT;
   private SumMatrix numServedAfterAWT;
   private SumMatrix sumExcessTimesServed;
   private SumMatrix sumExcessTimesAbandoned;
   private SumMatrix maxWaitingTimeServed;
   private SumMatrix maxWaitingTimeAbandoned;
   private SumMatrix sumWaitingTimesServed;
   private SumMatrix sumWaitingTimesAbandoned;
   private SumMatrix sumWaitingTimesVQServed;
   private SumMatrix sumWaitingTimesVQAbandoned;

   private SumMatrix sumSEWaitingTimesServed;        //Ajouter pour le calcul du MSE
   private SumMatrix sumSEWaitingTimesAbandoned;     //Ajouter pour le calcul du MSE
   private SumMatrix sumSEWaitingTimesVQServed;       //Ajouter pour le calcul du MSE
   private SumMatrix sumSEWaitingTimesVQAbandoned;    //Ajoutrer pour le calcul du MSE


   private ArrayList<String> distMseServed;      //Ajouter pour le calcul de la dist MSE
   private ArrayList<String> distMseAbandoned;    //Ajouter pour le calcul du MSE
   private ArrayList<String> distMseVQServed;     //Ajouter pour le calcul du MSE
   private ArrayList<String> distMseVQAbandoned;  //Ajouter pour le calcul du MSE
   //private  List<Integer> TabListeGroupAgent[] ;  /* Ajouter pour initialer le tableau de la liste des
   //                                                         types que chaque groupe peut traiter*/

   private SumMatrix sumServiceTimes;
   private SumMatrix sumServed;
   private boolean contactTypeAgentGroup;


   private int K, Ki, Ko, I;
   private int Kip, Ip;

   /**
    * Constructs an observer for all supported types of
    * call-by-call measures, for the call center model \texttt{cc}, and
    * using \texttt{statP} to obtain the statistical period of
    * each counted call.
    *
    * Many counters concerning a call type can be separated into
    * $I$ counters, one for each agent group.
    * This can be useful to obtain statistics concerning
    * specific (call type, agent group) pairs, but this requires
    * more memory.
    * The boolean argument \texttt{contactTypeAgentGroups} determines
    * if this separation is needed.
    * If (call type, agent group) statistics are needed, this argument is
    * \texttt{true}. Otherwise, it is \texttt{false}.
    * @param cc the call center model.
    * @param statP the object used to get statistical periods of calls.
    * @param contactTypeAgentGroups determines if statistics for
    * (call type, agent group) pairs are needed.
    */
   public CallByCallMeasureManager (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroups)
   {
      this (cc, statP, contactTypeAgentGroups, null);
      //initTabListDesGroupAgent(cc.getRouter());//Ajouter pour intialiser le tab de la liste des type que groupe
      // d'agent peuvent traiter
   }

   /**
    * Similar to constructor {@link #CallByCallMeasureManager(CallCenter, StatPeriod, boolean)},
    * but restricts the counters to the given
    * collection of measure types.
    */
   public CallByCallMeasureManager (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroups, Collection<MeasureType> measures)
   {
      if (statP == null)
         throw new NullPointerException();
      this.statP = statP;
      this.contactTypeAgentGroup = contactTypeAgentGroups;
      this.cc = cc;
      K = cc.getNumContactTypes ();
      Ki = cc.getNumInContactTypes ();
      Kip = Ki > 1 ? Ki + 1 + cc.getNumInContactTypeSegments() : Ki;
      Ko = cc.getNumOutContactTypes ();
      I = cc.getNumAgentGroups ();
      Ip = I > 1 ? I + 1 + cc.getNumAgentGroupSegments() : I;
      final int cg = contactTypeAgentGroup ? I : 1;
      final int cgp = contactTypeAgentGroup ? Ip : 1;

      final int np = statP.getNumPeriodsForCounters ();
      final int npAwt = statP.getNumPeriodsForCountersAwt ();
      if (measures == null || measures.contains (MeasureType.NUMARRIVALS))
         numArriv = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.NUMWRONGPARTYCONNECTS))
         numWrong = createSumMatrix (Ko, np);
      if (measures == null || measures.contains (MeasureType.NUMSERVED))
         numServed = createSumMatrix (K * cg, np);
      if (measures == null || measures.contains (MeasureType.NUMBLOCKED))
         numBlocked = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.NUMABANDONED))
         numAbandoned = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.NUMDELAYED))
         numDelayed = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.SUMSERVICETIMES))
         sumServiceTimes = createSumMatrix (K * cg, np);

      if (measures == null || measures.contains (MeasureType.SUMWAITINGTIMESSERVED))
         sumWaitingTimesServed = createSumMatrix (K * cg, np);
      if (measures == null || measures.contains (MeasureType.SUMSEWAITINGTIMESSERVED))   //Ajout
      { sumSEWaitingTimesServed = createSumMatrix (K * cg, np);
         distMseServed = new ArrayList<String>();
      }


      if (measures == null || measures.contains (MeasureType.SUMWAITINGTIMESABANDONED))
         sumWaitingTimesAbandoned = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.SUMSEWAITINGTIMESABANDONED))  //Ajout
      { sumSEWaitingTimesAbandoned = createSumMatrix (K, np);
         distMseAbandoned = new ArrayList<String>();
      }

      if (measures == null || measures.contains (MeasureType.MAXWAITINGTIMESERVED))
         maxWaitingTimeServed = createSumMatrix (K * cg, np);
      if (measures == null || measures.contains (MeasureType.MAXWAITINGTIMEABANDONED))
         maxWaitingTimeAbandoned = createSumMatrix (K, np);

      if (measures == null || measures.contains (MeasureType.SUMWAITINGTIMESVQSERVED))
         sumWaitingTimesVQServed = createSumMatrix (K * cg, np);
      if (measures == null || measures.contains (MeasureType.SUMSEWAITINGTIMESVQSERVED))  //Ajout
      { sumSEWaitingTimesVQServed = createSumMatrix (K * cg, np);
         distMseVQServed = new ArrayList<String>();
      }

      if (measures == null || measures.contains (MeasureType.SUMWAITINGTIMESVQABANDONED))
         sumWaitingTimesVQAbandoned = createSumMatrix (K, np);
      if (measures == null || measures.contains (MeasureType.SUMSEWAITINGTIMESVQABANDONED)) //Ajout
      { sumSEWaitingTimesVQAbandoned = createSumMatrix (K, np);
         distMseVQAbandoned = new ArrayList<String>();
      }

      final int nsl = Kip
                      * cc.getNumMatricesOfAWT ();
      if (measures == null || measures.contains (MeasureType.NUMSERVEDBEFOREAWT))
         numServedBeforeAWT = createSumMatrix (nsl * cgp, npAwt);
      if (measures == null || measures.contains (MeasureType.NUMSERVEDAFTERAWT))
         numServedAfterAWT = createSumMatrix (nsl * cgp, npAwt);
      if (measures == null
            || measures.contains (MeasureType.NUMABANDONEDBEFOREAWT))
         numAbandonedBeforeAwt = createSumMatrix (nsl, npAwt);
      if (measures == null
            || measures.contains (MeasureType.NUMABANDONEDAFTERAWT))
         numAbandonedAfterAwt = createSumMatrix (nsl, npAwt);
      if (measures == null
            || measures.contains (MeasureType.SUMEXCESSTIMESABANDONED))
         sumExcessTimesAbandoned = createSumMatrix (nsl, npAwt);
      if (measures == null
            || measures.contains (MeasureType.SUMEXCESSTIMESSERVED))
         sumExcessTimesServed = createSumMatrix (nsl * cgp, npAwt);
      if (measures == null || measures.contains (MeasureType.SUMSERVED))
         sumServed = new SumMatrix (K * I, 1);
   }

   private SumMatrix createSumMatrix (int nr, int nc)
   {
      if (statP.needsSlidingWindows ())
         return new SumMatrixSW (nr, nc);
      else
         return new SumMatrix (nr, nc);
   }

   /**
    * Returns the simulation logic associated with this object.
    *
    * @return the associated simulation logic.
    */
   public StatPeriod getStatPeriod ()
   {
      return statP;
   }

   /**
    * Returns \texttt{true} if this group of call center measures
    * contains matrices whose rows correspond
    * to counters concerning (contact type,
    * agent group) pairs.
    * If no matrix with rows of type
    * (contact type, agent group) is present,
    * this returns \texttt{false}.
    */
   public boolean isContactTypeAgentGroup()
   {
      return contactTypeAgentGroup;
   }


   /**
    * Initializes the given map \texttt{measureMap} with the measure matrices
    * declared by this class. Each key of the map must be an instance of
    * {@link MeasureType} while values are instances of {@link MeasureMatrix}.
    *
    * @param measureMap
    *           the map to be initialized.
    */
   public void initMeasureMap (Map<MeasureType, MeasureMatrix> measureMap)
   {
      if (numArriv != null)
         measureMap.put (MeasureType.NUMARRIVALS, numArriv);
      if (numWrong != null)
         measureMap.put (MeasureType.NUMWRONGPARTYCONNECTS, numWrong);
      if (numServed != null)
         measureMap.put (MeasureType.NUMSERVED, numServed);
      if (numBlocked != null)
         measureMap.put (MeasureType.NUMBLOCKED, numBlocked);
      if (numAbandoned != null)
         measureMap.put (MeasureType.NUMABANDONED, numAbandoned);
      if (numAbandonedBeforeAwt != null)
         measureMap.put (MeasureType.NUMABANDONEDBEFOREAWT,
                         numAbandonedBeforeAwt);
      if (numAbandonedAfterAwt != null)
         measureMap
         .put (MeasureType.NUMABANDONEDAFTERAWT, numAbandonedAfterAwt);
      if (sumExcessTimesAbandoned != null)
         measureMap
         .put (MeasureType.SUMEXCESSTIMESABANDONED, sumExcessTimesAbandoned);
      if (sumExcessTimesServed != null)
         measureMap
         .put (MeasureType.SUMEXCESSTIMESSERVED, sumExcessTimesServed);
      if (numDelayed != null)
         measureMap.put (MeasureType.NUMDELAYED, numDelayed);
      if (numServedBeforeAWT != null)
         measureMap.put (MeasureType.NUMSERVEDBEFOREAWT, numServedBeforeAWT);
      if (numServedAfterAWT != null)
         measureMap.put (MeasureType.NUMSERVEDAFTERAWT, numServedAfterAWT);
      if (sumServiceTimes != null)
         measureMap.put (MeasureType.SUMSERVICETIMES, sumServiceTimes);

      if (sumWaitingTimesServed != null)
         measureMap.put (MeasureType.SUMWAITINGTIMESSERVED, sumWaitingTimesServed);
      if (sumSEWaitingTimesServed != null)                                                   //Ajouter
         measureMap.put (MeasureType.SUMSEWAITINGTIMESSERVED, sumSEWaitingTimesServed);

      if (sumWaitingTimesAbandoned != null)
         measureMap.put (MeasureType.SUMWAITINGTIMESABANDONED, sumWaitingTimesAbandoned);
      if (sumSEWaitingTimesAbandoned != null)                                                   //Ajouter
         measureMap.put (MeasureType.SUMSEWAITINGTIMESABANDONED, sumSEWaitingTimesAbandoned);

      if (maxWaitingTimeServed != null)
         measureMap.put (MeasureType.MAXWAITINGTIMESERVED, maxWaitingTimeServed);
      if (maxWaitingTimeAbandoned != null)
         measureMap.put (MeasureType.MAXWAITINGTIMEABANDONED, maxWaitingTimeAbandoned);

      if (sumWaitingTimesVQServed != null)
         measureMap.put (MeasureType.SUMWAITINGTIMESVQSERVED, sumWaitingTimesVQServed);
      if (sumSEWaitingTimesVQServed != null)                                                   //Ajouter
         measureMap.put (MeasureType.SUMSEWAITINGTIMESVQSERVED, sumSEWaitingTimesVQServed);

      if (sumWaitingTimesVQAbandoned != null)
         measureMap.put (MeasureType.SUMWAITINGTIMESVQABANDONED, sumWaitingTimesVQAbandoned);
      if (sumSEWaitingTimesVQAbandoned != null)                                               //Ajouter
         measureMap.put (MeasureType.SUMSEWAITINGTIMESVQABANDONED, sumSEWaitingTimesVQAbandoned);

      if (sumServed != null)
         measureMap.put (MeasureType.SUMSERVED, sumServed);
   }

   // Ajouter pour recuperer les Listes des distributions dans un Map
   public void initMeasureMapMse (Map< Integer, ArrayList<String> > measureMapMse)
   {
      if (distMseServed != null)
         measureMapMse.put(1, distMseServed);
      if (distMseAbandoned != null)
         measureMapMse.put(2, distMseAbandoned);
      if (distMseVQServed != null)
         measureMapMse.put(3, distMseVQServed);
      if (distMseVQAbandoned != null)
         measureMapMse.put(4, distMseVQAbandoned);
   }
   public boolean hasMeasures ()
   {
      return numArriv != null || numWrong != null ||
             numServed != null || numBlocked != null
             || numAbandoned != null || numAbandonedBeforeAwt != null
             || numAbandonedAfterAwt != null || numDelayed != null
             || numServedBeforeAWT != null
             || sumServiceTimes != null
             || sumExcessTimesServed != null
             || sumExcessTimesAbandoned != null
             || sumWaitingTimesServed != null
             || sumSEWaitingTimesServed != null        //ajouter
             || sumWaitingTimesAbandoned != null
             || sumSEWaitingTimesAbandoned != null      //ajouter
             || maxWaitingTimeServed != null
             || maxWaitingTimeAbandoned != null
             || sumWaitingTimesVQServed != null
             || sumSEWaitingTimesVQServed != null       // Ajouter
             || sumWaitingTimesVQAbandoned != null
             || sumSEWaitingTimesVQAbandoned != null    //Ajouter
             || numServedAfterAWT != null ||
             sumServed != null;
   }

   public boolean hasMeasureMse()
   {
      return distMseServed != null || distMseAbandoned != null ||
             distMseVQServed != null || distMseVQAbandoned != null;

   }

   /**
    * Initializes every measure matrices defined by this object.
    */
   public void init ()
   {
      init (numArriv);
      init (numWrong);
      init (numServed);
      init (numBlocked);
      init (numAbandoned);
      init (numDelayed);
      init (sumServiceTimes);
      init (sumWaitingTimesServed);
      init (sumSEWaitingTimesServed); //Ajouter
      init (sumWaitingTimesAbandoned);
      init (sumSEWaitingTimesAbandoned);   //Ajouter
      init (maxWaitingTimeServed);
      init (maxWaitingTimeAbandoned);
      init (sumWaitingTimesVQServed);
      init (sumSEWaitingTimesVQServed);   //Ajouter
      init (sumWaitingTimesVQAbandoned);
      init (sumSEWaitingTimesVQAbandoned);   //Ajouter
      init (numServedBeforeAWT);
      init (numServedAfterAWT);
      init (numAbandonedBeforeAwt);
      init (numAbandonedAfterAwt);
      init (sumServed);
      init (sumExcessTimesServed);
      init (sumExcessTimesAbandoned);
   }

   private void init (MeasureMatrix m)
   {
      if (m != null)
         m.init ();
   }

   public void newArrival (Contact contact, int period)
   {
      if (numArriv == null)
         return ;
      final int type = contact.getTypeId ();
      addK (contact, numArriv, type, period, 1);
   }

   public void newWrong (Contact contact, int period)
   {
      if (numWrong == null)
         return ;
      final int type = contact.getTypeId () - Ki;
      if (type < 0)
         return ;
      addKo (contact, numWrong, type, period, 1);
   }

   public void newServiceTime (Contact contact, int period, double time)
   {
      if (sumServiceTimes == null)
         return ;
      final int type = contact.getTypeId ();
      final int group = contact.getLastAgentGroup ().getId ();
      addKI (contact, sumServiceTimes, type, group, period, time);
   }

   public void newBlocked (Contact contact, int period)
   {
      if (numBlocked == null)
         return ;
      final int type = contact.getTypeId ();
      addK (contact, numBlocked, type, period, 1);
   }

   public void newDelayed (Contact contact, int period)
   {
      if (numDelayed == null)
         return ;
      final int type = contact.getTypeId ();
      addK (contact, numDelayed, type, period, 1);
   }

   public void newWaitingTimeAbandoned (Contact contact, int period, double t)
   {   // Modifier
      if (sumWaitingTimesAbandoned == null && maxWaitingTimeAbandoned == null &&
            sumSEWaitingTimesAbandoned == null )
         return ;
      final int type = contact.getTypeId ();
      if (sumWaitingTimesAbandoned != null)
         addK (contact, sumWaitingTimesAbandoned, type, period, t);
      if (maxWaitingTimeAbandoned != null)
         maxK (contact, maxWaitingTimeAbandoned, type, period, t);

      if (sumSEWaitingTimesAbandoned != null)              //  Ajouter
      { double diff = contact.getWaitingTimeEstimate() - t;
         double squareOfDiff = diff * diff;
         DecimalFormat df = new DecimalFormat("00000.000");
         DecimalFormat dfs = new DecimalFormat("0000");
         String ES = df.format(contact.getWaitingTimeEstimate());
         ES.intern();
         String RE = df.format(t);
         RE.intern();
         String LF = dfs.format(contact.getPositionInWaitingQueue());
         LF.intern();
         String diffsESetRE = df.format(diff);
         diffsESetRE.intern();

         String LongAutreFile = new String();
         if (contact.getListeDesTraitesParLesMemeAgents() != null)
         {
            HashMap<Integer, Double> hashMap = (HashMap<Integer, Double>) contact
                                               .getListeDesTraitesParLesMemeAgents();
            double lfile = 0;
            StringBuilder sb = new StringBuilder () ;
for (Integer mapKey : hashMap.keySet()) {
               lfile = hashMap.get(mapKey) ;
               sb.append(dfs.format(lfile) + "  ");
            }
            LongAutreFile = sb.toString();
         }
         addInArrayList(distMseAbandoned, type + "  " + RE + "  " + ES + "   " + diffsESetRE + "   " + LF + "  " + LongAutreFile);
         addK (contact, sumSEWaitingTimesAbandoned, type, period, squareOfDiff);
      }
   }

   public void newWaitingTimeServed (Contact contact, int period, double t)
   {  //Modifier
      if (sumWaitingTimesServed == null && maxWaitingTimeServed == null &&
            sumSEWaitingTimesServed == null)
         return ;
      final int type = contact.getTypeId ();
      final int group = contact.getLastAgentGroup ().getId ();
      if (sumWaitingTimesServed != null)
         addKI (contact, sumWaitingTimesServed, type, group, period, t);
      if (maxWaitingTimeServed != null)
         maxKI (contact, maxWaitingTimeServed, type, group, period, t);

      if (sumSEWaitingTimesServed != null)     //Ajouter
      { double diff = contact.getWaitingTimeEstimate() - t;
         double squareOfDiff = diff * diff;
         DecimalFormat df = new DecimalFormat("00000.000");
         DecimalFormat dfs = new DecimalFormat("0000");
         String ES = df.format(contact.getWaitingTimeEstimate());
         ES.intern();
         String RE = df.format(t);
         RE.intern();
         String LF = dfs.format(contact.getPositionInWaitingQueue());
         String diffsESetRE = df.format(diff);

         String LongAutreFile = new String();
         if (contact.getListeDesTraitesParLesMemeAgents() != null)
         {
            HashMap<Integer, Double> hashMap = (HashMap<Integer, Double>) contact
                                               .getListeDesTraitesParLesMemeAgents();
            double lfile = 0;
            StringBuilder sb = new StringBuilder () ;
for (Integer mapKey : hashMap.keySet()) {
               lfile = hashMap.get(mapKey) ;
               sb.append(dfs.format(lfile) + "  ");
            }
            LongAutreFile = sb.toString();
         }
         addInArrayList(distMseServed, type + "  " + RE + "  " + ES + "    " + diffsESetRE + "   " + LF + "  " + LongAutreFile);
         addKI (contact, sumSEWaitingTimesServed, type, group, period, squareOfDiff);
      }

   }

   public void newWaitingTimeVQAbandoned (Contact contact, int period, double t)
   { //Modifier
      if (sumWaitingTimesVQAbandoned == null && sumSEWaitingTimesVQAbandoned == null)
         return ;
      final int type = contact.getTypeId ();
      if (sumWaitingTimesVQAbandoned != null)
         addK (contact, sumWaitingTimesVQAbandoned, type, period, t);

      if (sumSEWaitingTimesVQAbandoned != null)  //Ajouter
      { double diff = contact.getWaitingTimeEstimate() - t;
         double squareOfDiff = diff * diff;
         DecimalFormat df = new DecimalFormat("00000.000");
         DecimalFormat dfs = new DecimalFormat("0000");
         String ES = df.format(contact.getWaitingTimeEstimate());
         ES.intern();
         String RE = df.format(t);
         RE.intern();
         String LF = dfs.format(contact.getPositionInWaitingQueue());
         String diffsESetRE = df.format(diff);

         String LongAutreFile = new String();
         if (contact.getListeDesTraitesParLesMemeAgents() != null)
         {
            HashMap<Integer, Double> hashMap = (HashMap<Integer, Double>) contact
                                               .getListeDesTraitesParLesMemeAgents();
            double lfile = 0;
            StringBuilder sb = new StringBuilder () ;
for (Integer mapKey : hashMap.keySet()) {
               lfile = hashMap.get(mapKey) ;
               sb.append(dfs.format(lfile) + "  ");
            }
            LongAutreFile = sb.toString();
         }
         addInArrayList(distMseVQAbandoned, type + "  " + RE + "  " + ES + "    " + diffsESetRE + "    " + LF + "  " + LongAutreFile);
         addK (contact, sumSEWaitingTimesVQAbandoned, type, period, squareOfDiff);
      }
   }

   public void newWaitingTimeVQServed (Contact contact, int period, double t)
   { //Modifier
      if (sumWaitingTimesVQServed == null && sumSEWaitingTimesVQServed == null)
         return ;
      final int type = contact.getTypeId ();
      final int group = contact.getLastAgentGroup ().getId ();

      if (sumWaitingTimesVQServed != null)
         addKI (contact, sumWaitingTimesVQServed, type, group, period, t);
      if (sumSEWaitingTimesVQServed != null)     //Ajouter
      { double diff = contact.getWaitingTimeEstimate() - t;
         double squareOfDiff = diff * diff;
         DecimalFormat df = new DecimalFormat("00000.000");
         DecimalFormat dfs = new DecimalFormat("0000");
         String ES = df.format(contact.getWaitingTimeEstimate());
         ES.intern();
         String RE = df.format(t);
         RE.intern();
         String LF = dfs.format(contact.getPositionInWaitingQueue());
         String diffsESetRE = df.format(diff);

         String LongAutreFile = new String();
         if (contact.getListeDesTraitesParLesMemeAgents() != null)
         {
            HashMap<Integer, Double> hashMap = (HashMap<Integer, Double>) contact
                                               .getListeDesTraitesParLesMemeAgents();
            double lfile = 0;
            StringBuilder sb = new StringBuilder () ;
for (Integer mapKey : hashMap.keySet()) {
               lfile = hashMap.get(mapKey) ;
               sb.append(dfs.format(lfile) + "  ");
            }
            LongAutreFile = sb.toString();
         }
         addInArrayList(distMseVQServed, type + "  " + RE + "  " + ES + "    " + diffsESetRE + "   " + LF + "  " + LongAutreFile);
         addKI (contact, sumSEWaitingTimesVQServed, type, group, period, squareOfDiff);
      }

   }

   public void newAbandoned (Contact contact, int period)
   {
      if (numAbandoned == null)
         return ;
      final int type = contact.getTypeId ();
      addK (contact, numAbandoned, type, period, 1);
   }

   public void newServed (Contact contact, int period)
   {
      if (numServed == null)
         return ;
      final int type = contact.getTypeId ();
      final int group = contact.getLastAgentGroup ().getId ();
      addKI (contact, numServed, type, group, period, 1);
   }

   private void addK (Contact contact, SumMatrix mat, final int type, final int period, final double x)
   {
      mat.add (type, period, x);
      //      if (K > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            mat.add (mat.getNumMeasures() - 1, period, x);
      //         final int ns = simLogic.getModel().getNumContactTypeSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getContactTypeSegment (s);
      //            if (sinfo.containsValue (type))
      //               mat.add (K + s, period, x);
      //         }
      //      }
   }


   // Ajouter pour inserer un element dans un ArrayList
   public void addInArrayList(ArrayList<String> list, String ch)
   {
      if (list.size() < 600000)
         list.add(ch);
   }
   private void max (SumMatrix sm, int r, int c, double x)
   {
      sm.add (r, c, x, Functions.max);
   }

   private void maxK (Contact contact, SumMatrix mat, final int type, final int period, final double x)
   {
      max (mat, type, period, x);
      //      if (K > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            max (mat, mat.getNumMeasures() - 1, period, x);
      //         final int ns = simLogic.getModel().getNumContactTypeSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getContactTypeSegment (s);
      //            if (sinfo.containsValue (type))
      //               max (mat, K + s, period, x);
      //         }
      //      }
   }

   private void addKo (Contact contact, SumMatrix mat, final int type, final int period, final double x)
   {
      mat.add (type, period, x);
      //      if (Ko > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            mat.add (mat.getNumMeasures() - 1, period, x);
      //         final int ns = simLogic.getModel().getNumOutContactTypeSegments ();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getOutContactTypeSegment (s);
      //            if (sinfo.containsValue (type + Ki))
      //               mat.add (Ko + s, period, x);
      //         }
      //      }
   }

   private void addKI2 (Contact contact, SumMatrix mat, final int type, final int group, final int period, final double x)
   {
      final int idx = type * I;
      mat.add (idx + group, period, x);
      //      if (K > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            mat.add ((Kp-1)*Ip + group, period, x);
      //         final int ns = simLogic.getModel().getNumContactTypeSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getContactTypeSegment (s);
      //            if (sinfo.containsValue (type))
      //               mat.add (Ip*(K + s) + group, period, x);
      //         }
      //      }
      //      if (I > 1) {
      //         mat.add (idx + Ip - 1, period, x);
      //         final int ns = simLogic.getModel().getNumAgentGroupSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getAgentGroupSegment (s);
      //            if (sinfo.containsValue (group))
      //               mat.add (idx + I + s, period, x);
      //         }
      //      }
      //      if (K > 1 && I > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            mat.add ((Kp-1)*Ip + Ip - 1, period, x);
      //         final int nsType = simLogic.getModel().getNumContactTypeSegments();
      //         final int nsGroup = simLogic.getModel().getNumAgentGroupSegments();
      //         for (int k = 0; k < nsType; k++) {
      //            final SegmentInfo sinfoType = simLogic.getModel().getContactTypeSegment (k);
      //            if (!sinfoType.containsValue (type))
      //               continue;
      //            for (int i = 0; i < nsGroup; i++) {
      //               final SegmentInfo sinfoGroup = simLogic.getModel().getAgentGroupSegment (i);
      //               if (!sinfoGroup.containsValue (group))
      //                  continue;
      //               mat.add (Ip*(K + k) + I + i, period, x);
      //            }
      //         }
      //      }
   }

   private void addKI (Contact contact, SumMatrix mat, final int type, final int group, final int period, final double x)
   {
      if (!contactTypeAgentGroup) {
         addK (contact, mat, type, period, x);
         return ;
      }
      addKI2 (contact, mat, type, group, period, x);
   }

   private void maxKI2 (Contact contact, SumMatrix mat, final int type, final int group, final int period, final double x)
   {
      final int idx = type * I;
      max (mat, idx + group, period, x);
      //      if (K > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            max (mat, (Kp-1)*Ip + group, period, x);
      //         final int ns = simLogic.getModel().getNumContactTypeSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getContactTypeSegment (s);
      //            if (sinfo.containsValue (type))
      //               max (mat, Ip*(K + s) + group, period, x);
      //         }
      //      }
      //      if (I > 1) {
      //         max (mat, idx + Ip - 1, period, x);
      //         final int ns = simLogic.getModel().getNumAgentGroupSegments();
      //         for (int s = 0; s < ns; s++) {
      //            final SegmentInfo sinfo = simLogic.getModel().getAgentGroupSegment (s);
      //            if (sinfo.containsValue (group))
      //               max (mat, idx + I + s, period, x);
      //         }
      //      }
      //      if (K > 1 && I > 1) {
      //         if (!simLogic.getModel ().getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ())
      //            max (mat, (Kp-1)*Ip + Ip - 1, period, x);
      //         final int nsType = simLogic.getModel().getNumContactTypeSegments();
      //         final int nsGroup = simLogic.getModel().getNumAgentGroupSegments();
      //         for (int k = 0; k < nsType; k++) {
      //            final SegmentInfo sinfoType = simLogic.getModel().getContactTypeSegment (k);
      //            if (!sinfoType.containsValue (type))
      //               continue;
      //            for (int i = 0; i < nsGroup; i++) {
      //               final SegmentInfo sinfoGroup = simLogic.getModel().getAgentGroupSegment (i);
      //               if (!sinfoGroup.containsValue (group))
      //                  continue;
      //               max (mat, Ip*(K + k) + I + i, period, x);
      //            }
      //         }
      //      }
   }

   private void maxKI (Contact contact, SumMatrix mat, final int type, final int group, final int period, final double x)
   {
      if (!contactTypeAgentGroup) {
         maxK (contact, mat, type, period, x);
         return ;
      }
      maxKI2 (contact, mat, type, group, period, x);
   }

   public void blocked (Router router, Contact contact, int bType)
   {
      final int period = statP.getStatPeriod (contact);
      if (period < 0)
         return ;
      newArrival (contact, period);
      newBlocked (contact, period);
      newDelayed (contact, period);
   }

   public void dequeued (Router router, DequeueEvent ev)
   {
      if (ev.getEffectiveDequeueType () == Router.DEQUEUETYPE_TRANSFER)
         return ;
      final Contact contact = ev.getContact ();
      final int period = statP.getStatPeriod (contact);
      if (period < 0)
         return ;
      newArrival (contact, period);
      final double qt = contact.getTotalQueueTime();
      newWaitingTimeAbandoned (contact, period, qt);
      final double vt;
      if (contact instanceof Call)
         vt = ((Call)contact).getWaitingTimeVQ ();
      else
         vt = 0;
      newWaitingTimeVQAbandoned (contact, period, vt);
      newDelayed (contact, period);
      newAbandoned (contact, period);

      final int type = contact.getTypeId ();
      final int awtPeriod = statP.getAwtPeriod (contact);
      final int statPeriodAwt = statP.getStatPeriodAwt (contact);
      if (awtPeriod < 0 || statPeriodAwt < 0)
         return ;
      addGoodAndBad (contact, type, -1, qt,
                     awtPeriod, statPeriodAwt,
                     numAbandonedBeforeAwt, numAbandonedAfterAwt);
      newExcessTime (contact, type, -1, qt, awtPeriod, statPeriodAwt, sumExcessTimesAbandoned);
   }

   public void served (Router router, EndServiceEvent ev)
   {
      final Contact contact = ev.getContact ();
      final int period = statP.getStatPeriod (contact);
      if (period < 0)
         return ;
      final Call call = (Call)contact;
      if (!call.isRightPartyConnect ()) {
         newWrong (contact, period);
         return ;
      }
      newArrival (contact, period);
      newServiceTime (contact, period, ev.getEffectiveContactTime ());
      final double qt = contact.getTotalQueueTime ();
      newWaitingTimeServed (contact, period, qt);
      final double vt;
      if (contact instanceof Call)
         vt = ((Call)contact).getWaitingTimeVQ ();
      else
         vt = 0;
      newWaitingTimeVQServed (contact, period, vt);
      if (qt > 0)
         newDelayed (contact, period);
      final int type = contact.getTypeId ();
      final int group = contact.getLastAgentGroup ().getId ();
      if (sumServed != null)
         addKI2 (contact, sumServed, type, group, 0, 1);
      newServed (contact, period);

      final int awtPeriod = statP.getAwtPeriod (contact);
      final int statPeriodAwt = statP.getStatPeriodAwt (contact);
      if (awtPeriod < 0 || statPeriodAwt < 0)
         return ;
      addGoodAndBad (contact, type, group, qt, awtPeriod, statPeriodAwt, numServedBeforeAWT, numServedAfterAWT);
      newExcessTime (contact, type, group, qt, awtPeriod, statPeriodAwt, sumExcessTimesServed);
   }

   private void addI (SumMatrix mat, int offset,
                      int type, int group, int period, double x)
   {
      if (group == -1)
         mat.add (offset + type, period, x);
      else {
         mat.add (offset + group + type*Ip, period, x);
         if (I > 1) {
            mat.add (offset + Ip - 1 + type*Ip, period, x);
            final int ns = cc.getNumAgentGroupSegments();
            for (int s = 0; s < ns; s++) {
               final SegmentInfo sinfo = cc.getAgentGroupSegment (s);
               if (sinfo.containsValue (group))
                  mat.add (offset + I + s + type*Ip, period, x);
            }
         }
      }
   }

   private void addGoodAndBad (int offset, int type, int group,
                               double qt, double s, int statPeriod, SumMatrix good, SumMatrix bad)
   {
      if (qt > s) {
         if (bad != null)
            addI (bad, offset, type, group, statPeriod, 1);
      } else if (good != null)
         addI (good, offset, type, group, statPeriod, 1);
   }

   private void addGoodAndBad (Contact contact, int type, int group,
                               double qt, int awtPeriod, int statPeriod, SumMatrix good, SumMatrix bad)
   {
      if (type >= Ki)
         // Outbound contact type, ignoring
         return ;

      final boolean goodAdd = good != null;
      final boolean badAdd = bad != null;
      if (!goodAdd && !badAdd)
         return ;
      final int numStatPeriods = statP.getNumPeriodsForCountersAwt ();

      int offset = 0;
      final int nsl = cc.getNumMatricesOfAWT ();
      final int groupIndex = contactTypeAgentGroup ? group : -1;
      final boolean countedInSum = !cc.getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ();

      final int nsegType;
      if (Ki > 1)
         nsegType = cc.getNumInContactTypeSegments ();
      else
         nsegType = 0;
      final int nsegPeriod;
      if (numStatPeriods > 1 && statP.needsStatForPeriodSegmentsAwt ())
         nsegPeriod = cc.getNumMainPeriodSegments ();
      else
         nsegPeriod = -1;
      final int P = cc.getNumMainPeriods();
      for (int si = 0; si < nsl; si++) {
         final ServiceLevelParamReadHelper slp = cc.getServiceLevelParams (si);
         //         final double skp = slp.getAwtSim (type, awtPeriod);
         //         addGoodAndBad (offset, type, groupIndex, qt, skp, goodStatPeriod, badStatPeriod, good, bad);

         for (int segType = -1; segType <= nsegType; segType++) {
            // segType=1 represents the segment containing the single call type, type
            // segType=nsegType represents the implicit segment regrouping all call types
            // Any other index represents a user-defined segment of inbound call types.
            if (segType == nsegType && !countedInSum)
               continue;
            if (segType >= 0 && segType < nsegType && !cc.getInContactTypeSegment (segType).containsValue (type))
               continue;
            final int typeIndex = segType == -1 ? type : Ki + segType;
            if (Ki <= 1 && typeIndex > 0)
               continue;
            final double sseg1 = slp.getAwtDefault (typeIndex, awtPeriod);
            addGoodAndBad (offset, typeIndex, groupIndex,
                           qt, sseg1, statPeriod, good, bad);
            for (int segPeriod = 0; segPeriod <= nsegPeriod; segPeriod++) {
               if (segPeriod < nsegPeriod && !cc.getMainPeriodSegment (segPeriod).containsValue (statPeriod))
                  continue;
               final double sseg = slp.getAwtDefault (typeIndex, P + segPeriod);
               addGoodAndBad (offset, typeIndex, groupIndex,
                              qt, sseg, P + segPeriod, good, bad);
            }
         }

         offset += groupIndex == -1 ? Kip : Kip * Ip;
      }
   }

   private void newExcessTime (Contact contact, int type, int group,
                               double qt, int awtPeriod, int statPeriod, SumMatrix mat)
   {
      if (type >= Ki)
         // Outbound contact type, ignoring
         return ;

      final boolean add
         = mat != null && statPeriod >= 0;
      if (!add
         )
         return ;
      final int numStatPeriods = mat == null ? -1 :
                                 statP.getNumPeriodsForCountersAwt ();

      int offset = 0;
      final int nsl = cc.getNumMatricesOfAWT ();
      final int groupIndex = contactTypeAgentGroup ? group : -1;
      final boolean countedInSum = !cc.getCallFactory (contact.getTypeId ()).isExcludedFromStatTotal ();
      final int nsegType;
      if (Ki > 1)
         nsegType = cc.getNumInContactTypeSegments ();
      else
         nsegType = 0;
      final int nsegPeriod;
      if (numStatPeriods > 1 && statP.needsStatForPeriodSegmentsAwt ())
         nsegPeriod = cc.getNumMainPeriodSegments ();
      else
         nsegPeriod = -1;
      final int P = cc.getNumMainPeriods();
      for (int si = 0; si < nsl; si++) {
         final ServiceLevelParamReadHelper slp = cc.getServiceLevelParams (si);
         for (int segType = -1; segType <= nsegType; segType++) {
            if (segType == nsegType && !countedInSum)
               continue;
            if (segType >= 0 && segType < nsegType && !cc.getInContactTypeSegment (segType).containsValue (type))
               continue;
            final int typeIndex = segType == -1 ? type : Ki + segType;
            if (Ki <= 1 && typeIndex > 0)
               continue;
            final double sseg1 = slp.getAwtDefault (typeIndex, awtPeriod);
            final double excessTimeseg1 = Math.max (0, qt - sseg1);
            addI (mat, offset, typeIndex, groupIndex, statPeriod, excessTimeseg1);
            for (int segPeriod = 0; segPeriod <= nsegPeriod; segPeriod++) {
               if (segPeriod < nsegPeriod && !cc.getMainPeriodSegment (segPeriod).containsValue (statPeriod))
                  continue;
               final double sseg = slp.getAwtDefault (typeIndex, P + segPeriod);
               final double excessTimeseg = Math.max (0, qt - sseg);
               addI (mat, offset, typeIndex, groupIndex, P + segPeriod, excessTimeseg);
            }
         }

         offset += groupIndex == -1 ? Kip : Kip * Ip;
      }
   }
}
