package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;

import cern.colt.matrix.DoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.contactcenters.dialer.Dialer;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.queue.QueueSizeStat;
import umontreal.iro.lecuyer.contactcenters.queue.QueueSizeStatMeasureMatrix;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStat;
import umontreal.iro.lecuyer.contactcenters.server.GroupVolumeStatMeasureMatrix;

import umontreal.ssj.stat.mperiods.IntegralMeasureMatrix;
import umontreal.ssj.stat.mperiods.IntegralMeasureMatrixSW;
import umontreal.ssj.stat.mperiods.MeasureMatrix;
import umontreal.ssj.stat.mperiods.MeasureSet;

/**
 * Encapsulates the matrices of counters collecting observations
 * during simulation, and provides methods to determine
 * which types of counters are supported, and to extract
 * matrices of observations from the counters.
 *
 * This class encapsulates observers used to
 * update counters.
 * Therefore,
 * any instance of this
 * class should be registered with the call center using the
 * {@link #registerListeners()} for listeners
 * to be registered.
 *
 * Each matrix of counters has a type represented by
 * an enum constant in {@link MeasureType}.
 * This type determines the role played by rows in
 * the matrix of counters.
 * The user can determine for which type of measures statistics are
 * collected by giving a list of {@link MeasureType} instances
 * to the constructor of {@link CallCenterMeasureManager}.
 * This list can be retrieved by
 * using the {@link #getMeasures()} method.
 *
 * The columns correspond to time intervals which
 * are determined with the help of a {@link StatPeriod}
 * implementation. Such an implementation gives the number of
 * needed time intervals as well as a function mapping each contact, and
 * each simulation time, to one of the columns.
 * Usually, there is one column per
 * period.
 * The {@link StatPeriod} implementation of a measure manager can be obtained
 * using the {@link #getStatPeriod()} method.
 *
 * The raw matrices of counters can be obtained using    //important de noter
 * the {@link #getMeasureMatrix(MeasureType)}
 * method.
 * However, most measure managers regroup periods and
 * normalizes values with respect to time in order
 * to prepare matrices of observations for
 * statistical collectors.
 * This preparation is performed by the
 * method {@link #getValues(MeasureType,boolean)}.
 *
 * The number of columns in the matrices of observations,
 * the way
 * periods are regrouped, and how time
 * is normalized are determined by the subclass implementing
 * the {@link #getNumPeriodsForStatProbes()},
 * {@link #getValues(MeasureType,boolean)}, and
 * {@link #timeNormalize(MeasureType,DoubleMatrix2D)}
 * abstract methods.
 * These methods need to be overridden by a
 * concrete subclass.
 */
public abstract class CallCenterMeasureManager
{
   private CallCenter cc;
   private StatPeriod statP;
   private final Map<MeasureType, MeasureMatrix> measureMap = new EnumMap<MeasureType, MeasureMatrix> (
            MeasureType.class);
   private final Map < Integer, ArrayList < String >> measureMapMse = new HashMap < Integer, ArrayList < String >> ();  //Ajouter
   //private final ArrayList <ArrayList<String>> listeDeliste=new ArrayList<ArrayList<String>>();
   private CallByCallMeasureManager cm;
   private OutboundCallCounter outCounter;
   private IntegralMeasureMatrix<GroupVolumeStatMeasureMatrix>[] groupVolume;
   private IntegralMeasureMatrix<QueueSizeStatMeasureMatrix>[] intQueueSize;
   private MeasureSet svm;
   private MeasureSet wvm;
   private MeasureSet tvm;
   private MeasureSet qs;
   private QueueSizeChecker sc;
   private BusyAgentsChecker bc;
   private MeasureType[] measureTypes;

   @SuppressWarnings ("unchecked")
   private <M extends MeasureMatrix> IntegralMeasureMatrix<M>[] newArray (
      int length)
   {
      return new IntegralMeasureMatrix[length];
   }

   /**
    * Creates a measure manager for all possible types
    * of measures on the call center model \texttt{cc}, and
    * using \texttt{statP} to obtain the statistical periods
    * of calls.
    * The boolean \texttt{contactTypeAgentGroup}
    * is used to determine if matrices of counters
    * contain rows of type (call type, agent group).
    * See the constructor {@link CallByCallMeasureManager#CallByCallMeasureManager(CallCenter,StatPeriod,boolean)}
    * for more information about this.
    * @param cc the call center model.
    * @param statP the object used to obtain statistical periods.
    * @param contactTypeAgentGroup determines if rows
    * of type (call type, agent group) are needed.
    */
   public CallCenterMeasureManager (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup)
   {
      this (cc, statP, contactTypeAgentGroup, EnumSet.allOf (MeasureType.class));
   }

   /**
    * Similar to {@link #CallCenterMeasureManager(CallCenter,StatPeriod,boolean)},
    * for a given subset of the types of performance measures.
    * The subset is obtained by calling the {@link #getMeasureTypes(PerformanceMeasureType[])}
    * static method.
    */
   public CallCenterMeasureManager (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, PerformanceMeasureType[] pms)
   {
      this (cc, statP, contactTypeAgentGroup, getMeasureTypes (pms));
   }

   /**
    * Similar to {@link #CallCenterMeasureManager(CallCenter,StatPeriod,boolean)},
    * for a given collection of measure types.
    */ 
   //Modifier
   public CallCenterMeasureManager (CallCenter cc, StatPeriod statP, boolean contactTypeAgentGroup, Collection<MeasureType> measures)
   {
      // ,boolean checkSizes, boolean checkBusy
      this.cc = cc;
      this.statP = statP;
      cm = new CallByCallMeasureManager (cc, statP, contactTypeAgentGroup, measures);
      if (cm.hasMeasures ())
         cm.initMeasureMap (measureMap);      //public void initMeasureMap (Map<MeasureType, MeasureMatrix> measureMap)
      //Initializes the given map \texttt{measureMap} with the measure matrices
      else
         cm = null;
      if (cm.hasMeasureMse())
         cm.initMeasureMapMse(measureMapMse); //Ajouter


      if (measures == null || measures.contains (MeasureType.NUMTRIEDDIAL)) {
         outCounter = new OutboundCallCounter (cc, statP);
         measureMap.put (MeasureType.NUMTRIEDDIAL, outCounter.getCount ());
      }

      if (measures == null || measures.contains (MeasureType.NUMBUSYAGENTS)
            || measures.contains (MeasureType.NUMWORKINGAGENTS)
            || measures.contains (MeasureType.NUMSCHEDULEDAGENTS)) {
         groupVolume = newArray (cc.getNumAgentGroups ());
         for (int i = 0; i < groupVolume.length; i++) {
            final GroupVolumeStatMeasureMatrix gstat = new GroupVolumeStatMeasureMatrix (
                     cc.simulator (), cc.getAgentGroup (i));
            if (statP.needsSlidingWindows ())
               groupVolume[i] = new IntegralMeasureMatrixSW<GroupVolumeStatMeasureMatrix> (
                                   gstat, statP.getNumPeriodsForCounters ());
            else
               groupVolume[i] = new IntegralMeasureMatrix<GroupVolumeStatMeasureMatrix> (
                                   gstat, statP.getNumPeriodsForCounters ());
         }
         if (measures == null || measures.contains (MeasureType.NUMBUSYAGENTS)) {
            svm = GroupVolumeStatMeasureMatrix
                  .getServiceVolumeMeasureSet (groupVolume);
            svm.setComputingSumRow (false);
            measureMap.put (MeasureType.NUMBUSYAGENTS, svm);
         }
         if (measures == null
               || measures.contains (MeasureType.NUMWORKINGAGENTS)) {
            wvm = GroupVolumeStatMeasureMatrix
                  .getWorkingVolumeMeasureSet (groupVolume);
            wvm.setComputingSumRow (false);
            measureMap.put (MeasureType.NUMWORKINGAGENTS, wvm);
         }
         if (measures == null
               || measures.contains (MeasureType.NUMSCHEDULEDAGENTS)) {
            tvm = GroupVolumeStatMeasureMatrix
                  .getTotalVolumeMeasureSet (groupVolume);
            tvm.setComputingSumRow (false);
            measureMap.put (MeasureType.NUMSCHEDULEDAGENTS, tvm);
         }
      }

      if (measures == null || measures.contains (MeasureType.QUEUESIZE)) {
         intQueueSize = newArray (cc.getNumWaitingQueues ());
         for (int q = 0; q < intQueueSize.length; q++) {
            final QueueSizeStatMeasureMatrix qsstat = new QueueSizeStatMeasureMatrix (
                     cc.simulator (), cc.getWaitingQueue (q));
            if (statP.needsSlidingWindows ())
               intQueueSize[q] = new IntegralMeasureMatrixSW<QueueSizeStatMeasureMatrix> (
                                    qsstat, statP.getNumPeriodsForCounters ());
            else
               intQueueSize[q] = new IntegralMeasureMatrix<QueueSizeStatMeasureMatrix> (
                                    qsstat, statP.getNumPeriodsForCounters ());
         }
         qs = QueueSizeStatMeasureMatrix
              .getQueueSizeIntegralMeasureSet (intQueueSize);
         measureMap.put (MeasureType.QUEUESIZE, qs);
      }
      if (measures == null || measures.contains (MeasureType.MAXQUEUESIZE)) {
         sc = new QueueSizeChecker (cc, statP);
         measureMap.put (MeasureType.MAXQUEUESIZE, sc);
      }
      if (measures == null || measures.contains (MeasureType.MAXBUSYAGENTS)) {
         bc = new BusyAgentsChecker (cc, statP);
         measureMap.put (MeasureType.MAXBUSYAGENTS, bc);
      }

      measureTypes = measureMap.keySet ().toArray (
                        new MeasureType[measureMap.size ()]);
   }

   private static Map < PerformanceMeasureType, MeasureType[] > pmMeasures;    //Tableau de matrices des performances mesurer

   private static void putMeasures (PerformanceMeasureType pm,
                                    MeasureType... measureTypes)
   {
      // Shortcut to avoid verbose array creation in the static initializer
      // With this method, the code
      // pmMeasures.put (pm, new MeasureType[] { m1, m2, m3, ... });
      // becomes
      // putMeasures (pm, m1, m2, m3, ...);
      // In other words, the vararg avoids coding for the array creation.
      pmMeasures.put (pm, measureTypes);
   }

   // Needs to be updated each time an enum constant is added
   // in PerformanceMeasureType
   static {
      pmMeasures = new EnumMap < PerformanceMeasureType, MeasureType[] > (
                      PerformanceMeasureType.class);
      for (final PerformanceMeasureType pm : PerformanceMeasureType.values ())
         // For each type of performance measure, we indicate the needed matrices
         // of counters.
         switch (pm)
         {
         case SERVICELEVEL:
         case SERVICELEVELREP:
         case SERVICELEVELIND01:
         case SERVICELEVEL2:
         case SERVICELEVEL2REP:
            putMeasures (pm, MeasureType.NUMSERVEDBEFOREAWT,
                         MeasureType.NUMABANDONEDBEFOREAWT, MeasureType.NUMARRIVALS);
            break;
         case SERVICELEVELG:
            putMeasures (pm, MeasureType.NUMSERVEDBEFOREAWT,
                         MeasureType.NUMABANDONEDAFTERAWT, MeasureType.NUMSERVED,
                         MeasureType.NUMBLOCKED);
            break;
         case ABANDONMENTRATIO:
         case ABANDONMENTRATIOREP:
            putMeasures (pm, MeasureType.NUMABANDONED, MeasureType.NUMARRIVALS);
            break;
         case SERVICERATIO:
         case SERVICERATIOREP:
            putMeasures (pm, MeasureType.NUMSERVED, MeasureType.NUMARRIVALS);
            break;
         case SPEEDOFANSWER:
         case SPEEDOFANSWERREP:
         case SPEEDOFANSWERG:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED,
                         MeasureType.NUMSERVED);
            break;
         case TIMETOABANDON:
         case TIMETOABANDONREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESABANDONED,
                         MeasureType.NUMABANDONED);
            break;
         case WAITINGTIMEVQSERVED:
         case WAITINGTIMEVQSERVEDREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQSERVED,
                         MeasureType.NUMSERVED);
            break;
         case MSEWAITINGTIMEVQSERVED:                      //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQSERVED,
                         MeasureType.NUMSERVED);
            break;
         case MSEWAITINGTIMESERVED:                      //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESSERVED,
                         MeasureType.NUMSERVED);
            break;
         case WAITINGTIMEVQABANDONED:
         case WAITINGTIMEVQABANDONEDREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQABANDONED,
                         MeasureType.NUMABANDONED);
            break;
         case MSEWAITINGTIMEVQABANDONED:      //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQABANDONED,
                         MeasureType.NUMABANDONED);
            break;
         case MSEWAITINGTIMEABANDONED:      //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESABANDONED,
                         MeasureType.NUMABANDONED);
            break;
         case WAITINGTIMEVQ:
         case WAITINGTIMEVQREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQSERVED,
                         MeasureType.SUMWAITINGTIMESVQABANDONED,
                         MeasureType.NUMARRIVALS);
            break;
         case MSEWAITINGTIMEVQ:             //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQSERVED,
                         MeasureType.SUMSEWAITINGTIMESVQABANDONED,
                         MeasureType.NUMARRIVALS);
            break;
         case SERVICETIME:
         case SERVICETIMEREP:
         case SERVICETIMEG:
            putMeasures (pm, MeasureType.SUMSERVICETIMES, MeasureType.NUMSERVED);
            break;
         case EXCESSTIME:
         case EXCESSTIMEREP:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESABANDONED,
                         MeasureType.SUMEXCESSTIMESSERVED, MeasureType.NUMARRIVALS);
            break;
         case EXCESSTIMESERVED:
         case EXCESSTIMESERVEDREP:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESSERVED,
                         MeasureType.NUMSERVED);
            break;
         case EXCESSTIMEABANDONED:
         case EXCESSTIMEABANDONEDREP:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESABANDONED,
                         MeasureType.NUMABANDONED);
            break;
         case WAITINGTIME:
         case WAITINGTIMEREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED,
                         MeasureType.SUMWAITINGTIMESABANDONED, MeasureType.NUMARRIVALS);
            break;
         case MSEWAITINGTIME:         //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESSERVED,
                         MeasureType.SUMSEWAITINGTIMESABANDONED, MeasureType.NUMARRIVALS);
            break;
         case WAITINGTIMEG:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED,
                         MeasureType.SUMWAITINGTIMESABANDONED, MeasureType.NUMSERVED,
                         MeasureType.NUMABANDONED, MeasureType.NUMBLOCKED);
            break;
         case WAITINGTIMEWAIT:
         case WAITINGTIMEWAITREP:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED,
                         MeasureType.SUMWAITINGTIMESABANDONED, MeasureType.NUMDELAYED);
            break;
         case OCCUPANCY:
         case OCCUPANCYREP:
            putMeasures (pm, MeasureType.NUMBUSYAGENTS,
                         MeasureType.NUMSCHEDULEDAGENTS);
            break;
         case OCCUPANCY2:
         case OCCUPANCY2REP:
            putMeasures (pm, MeasureType.NUMBUSYAGENTS,
                         MeasureType.NUMWORKINGAGENTS);
            break;
         case BLOCKRATIO:
         case BLOCKRATIOREP:
            putMeasures (pm, MeasureType.NUMBLOCKED, MeasureType.NUMARRIVALS);
            break;
         case DELAYRATIO:
         case DELAYRATIOREP:
            putMeasures (pm, MeasureType.NUMDELAYED, MeasureType.NUMARRIVALS);
            break;
         case ABANDONMENTRATIOBEFOREAWT:
            putMeasures (pm, MeasureType.NUMABANDONEDBEFOREAWT,
                         MeasureType.NUMARRIVALS);
            break;
         case ABANDONMENTRATIOAFTERAWT:
            putMeasures (pm, MeasureType.NUMABANDONEDAFTERAWT,
                         MeasureType.NUMARRIVALS);
            break;
         case RATEOFARRIVALS:
         case RATEOFARRIVALSIN:
            putMeasures (pm, MeasureType.NUMARRIVALS);
            break;
         case RATEOFOFFERED:
            putMeasures (pm, MeasureType.NUMARRIVALS, MeasureType.NUMBLOCKED);
            break;
         case RATEOFSERVICESBEFOREAWT:
            putMeasures (pm, MeasureType.NUMSERVEDBEFOREAWT);
            break;
         case RATEOFSERVICESAFTERAWT:
            putMeasures (pm, MeasureType.NUMSERVEDAFTERAWT);
            break;
         case RATEOFINTARGETSL:
            putMeasures (pm, MeasureType.NUMSERVEDBEFOREAWT,
                         MeasureType.NUMABANDONEDBEFOREAWT);
            break;
         case RATEOFSERVICES:
            putMeasures (pm, MeasureType.NUMSERVED);
            break;
         case RATEOFSERVICESG:
            putMeasures (pm, MeasureType.NUMSERVED);
            break;
         case RATEOFBLOCKING:
            putMeasures (pm, MeasureType.NUMBLOCKED);
            break;
         case RATEOFABANDONMENT:
            putMeasures (pm, MeasureType.NUMABANDONED);
            break;
         case RATEOFABANDONMENTBEFOREAWT:
            putMeasures (pm, MeasureType.NUMABANDONEDBEFOREAWT);
            break;
         case RATEOFABANDONMENTAFTERAWT:
            putMeasures (pm, MeasureType.NUMABANDONEDAFTERAWT);
            break;
         case RATEOFDELAY:
            putMeasures (pm, MeasureType.NUMDELAYED);
            break;
         case AVGQUEUESIZE:
            putMeasures (pm, MeasureType.QUEUESIZE);
            break;
         case RATEOFTRIEDOUTBOUND:
            putMeasures (pm, MeasureType.NUMTRIEDDIAL);
            break;
         case RATEOFWRONGPARTYCONNECT:
            putMeasures (pm, MeasureType.NUMWRONGPARTYCONNECTS);
            break;
         case AVGBUSYAGENTS:
            putMeasures (pm, MeasureType.NUMBUSYAGENTS);
            break;
         case AVGWORKINGAGENTS:
            putMeasures (pm, MeasureType.NUMWORKINGAGENTS);
            break;
         case AVGSCHEDULEDAGENTS:
            putMeasures (pm, MeasureType.NUMSCHEDULEDAGENTS);
            break;
         case SERVEDRATES:
            putMeasures (pm, MeasureType.SUMSERVED);
            break;
         case MAXQUEUESIZE:
            putMeasures (pm, MeasureType.MAXQUEUESIZE);
            break;
         case MAXBUSYAGENTS:
            putMeasures (pm, MeasureType.MAXBUSYAGENTS);
            break;
         case QUEUESIZEENDSIM:
         case BUSYAGENTSENDSIM:
            putMeasures (pm);
            break;
         case SUMSERVICETIMES:
            putMeasures (pm, MeasureType.SUMSERVICETIMES);
            break;
         case SUMEXCESSTIMES:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESSERVED,
                         MeasureType.SUMEXCESSTIMESABANDONED);
            break;
         case SUMEXCESSTIMESSERVED:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESSERVED);
            break;
         case SUMEXCESSTIMESABANDONED:
            putMeasures (pm, MeasureType.SUMEXCESSTIMESABANDONED);
            break;
         case SUMWAITINGTIMES:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED,
                         MeasureType.SUMWAITINGTIMESABANDONED);
            break;
         case SUMSEWAITINGTIMES:                                     //ajouter SUMSQUAREDIFFESTREALWAITINGTIMES
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESSERVED,
                         MeasureType.SUMSEWAITINGTIMESABANDONED);
            break;
         case SUMWAITINGTIMESSERVED:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESSERVED);
            break;
         case SUMSEWAITINGTIMESSERVED:                           // Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESSERVED);
            break;
         case SUMWAITINGTIMESABANDONED:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESABANDONED);
            break;
         case SUMSEWAITINGTIMESABANDONED:                            //  Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESABANDONED);
            break;
         case MAXWAITINGTIME:
         case MAXWAITINGTIMEG:
            putMeasures (pm, MeasureType.MAXWAITINGTIMEABANDONED, MeasureType.MAXWAITINGTIMESERVED);
            break;
         case MAXWAITINGTIMEABANDONED:
            putMeasures (pm, MeasureType.MAXWAITINGTIMEABANDONED);
            break;
         case MAXWAITINGTIMESERVED:
         case MAXWAITINGTIMESERVEDG:
            putMeasures (pm, MeasureType.MAXWAITINGTIMESERVED);
            break;
         case SUMWAITINGTIMESVQ:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQSERVED,
                         MeasureType.SUMWAITINGTIMESVQABANDONED);
            break;
         case SUMSEWAITINGTIMESVQ:          //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQSERVED,
                         MeasureType.SUMSEWAITINGTIMESVQABANDONED);
            break;
         case SUMWAITINGTIMESVQSERVED:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQSERVED);
            break;
         case SUMSEWAITINGTIMESVQSERVED:       //aJOUTER
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQSERVED);
            break;
         case SUMWAITINGTIMESVQABANDONED:
            putMeasures (pm, MeasureType.SUMWAITINGTIMESVQABANDONED);
            break;

         case SUMSEWAITINGTIMESVQABANDONED:      //Ajouter
            putMeasures (pm, MeasureType.SUMSEWAITINGTIMESVQABANDONED);
            break;
         default:
            throw new IllegalArgumentException ("Unknown performance measure: "
                                                + pm.name () + ", please update static block in " +
                                                CallCenterMeasureManager.class.getName ());
         }
   }

   /**
    * Returns the array of all types of performance measures
    * supported by this measure manager.
    */
   public static PerformanceMeasureType[] getSupportedPerformanceMeasures()
   { //Recupere toutes les cle du Map qui sont les performance
      return pmMeasures.keySet ().toArray (new PerformanceMeasureType[pmMeasures.size()]); // a mesurer par la simulation
   }

   //  Ajouter

   public ArrayList<String> [] getMeasureTypesMse()
   {
      return measureMapMse.values().toArray(new ArrayList[measureMapMse.size()]);
   }
   /**
    * Returns the types of counters needed to estimate
    * the particular type of performance measure
    * \texttt{pm}.
    * @param pm the tested type of performance measure.
    * @return the array of needed types of counters.
    */
   public static MeasureType[] getMeasureTypesPm (PerformanceMeasureType pm)
   { //Retoune un tableau de matrice a partir de la cle
      return pmMeasures.get (pm);                                              // Specifier eple m1-m2-m3-m4
   }


   /**
    * Returns the types of counters needed to estimate
    * all the performance measures in \texttt{pms}.
    * @param pms the tested types of performance measures.
    * @return the set of measure types.
    */
   public static Set<MeasureType> getMeasureTypes (     //  converi le tableau de matrice en une liste de matrice
      PerformanceMeasureType... pms)
   {
      final Set<MeasureType> measures = EnumSet.noneOf (MeasureType.class);
for (final PerformanceMeasureType pm : pms) {
         final MeasureType[] measureTypes = pmMeasures.get (pm);
         if (measureTypes == null)
            throw new IllegalArgumentException (
               "Unknown performance measure type: " + pm.name ());
for (final MeasureType mt : measureTypes)
            measures.add (mt);
      }
      return measures;
   }

   /**
    * Returns the object determining
    * how columns of matrices of counters
    * are mapped to time intervals.
    */
   public StatPeriod getStatPeriod()
   {
      return statP;
   }

   /**
    * Returns the call-by-call measure manager
    * used by this object.
    */
   public CallByCallMeasureManager getCallByCallMeasureManager ()
   {
      return cm;
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
      return cm == null ? false : cm.isContactTypeAgentGroup ();
   }

   /**
    * Determines if this simulator computes the measure matrices required to
    * estimate performance measures of type \texttt{pm}.
    *
    * @param pm
    *           the tested type of performance measures.
    * @return \texttt{true} of the measures can be estimated, \texttt{false}
    *         otherwise.
    */
   public boolean hasMeasureMatricesFor (PerformanceMeasureType pm)
   {
      if (pm == PerformanceMeasureType.MAXQUEUESIZE)
         return sc != null;
      if (pm == PerformanceMeasureType.MAXBUSYAGENTS)
         return bc != null;
      final MeasureType[] measureTypes1 = pmMeasures.get (pm);
      if (measureTypes1 == null)
         throw new IllegalArgumentException (
            "Unknown performance measure type: " + pm.name ());
for (final MeasureType mt : measureTypes1)
         if (!measureMap.containsKey (mt))
            return false;
      return true;
   }

   /**
    * Initializes the measure matrices defined by this object.
    */
   public void initMeasureMatrices ()
   {
for (final MeasureMatrix m : measureMap.values ())
         m.init ();
      if (groupVolume != null)
         for (final IntegralMeasureMatrix < ? > grpVol : groupVolume)
            grpVol.init ();
      if (intQueueSize != null)
         for (final IntegralMeasureMatrix < ? > iqs : intQueueSize)
            iqs.init ();
   }

   private void newPeriod (int currentPeriod)
   {
      if (groupVolume != null)
         for (final IntegralMeasureMatrix < ? > grpVol : groupVolume) {
            final int n = currentPeriod - grpVol.getNumStoredRecords () + 1;
            for (int i = 0; i < n; i++)
               grpVol.newRecord ();
         }
      if (intQueueSize != null)
         for (final IntegralMeasureMatrix < ? > iqs : intQueueSize) {
            final int n = currentPeriod - iqs.getNumStoredRecords () + 1;
            for (int i = 0; i < n; i++)
               iqs.newRecord ();
         }
   }

   /**
    * Indicates the end of the current statistical period,
    * whose index $p$ is
    * returned by {@link StatPeriod#getStatPeriod()}.
    * This method updates the columns $p$ of
    * matrices of counters
    * containing integrals
    * with respect to simulation time.
    * These matrices contain, for example,
    * the time-average queue size, time-average number
    * of busy agents, etc.
    */
   public void finishCurrentPeriod()
   {
      final int currentPeriod = statP.getStatPeriod ();
      newPeriod (currentPeriod + 1);
   }

   /**
    * Updates the current statistical period.
    * For any period $p$ preceding the current
    * statistical period,
    * this method fills up the columns $p$ of matrices
    * of counters containing integrals with respect to
    * simulation time.
    * It also initializes the maximal queue size and
    * maximal number of busy agents for the
    * current statistical period.
    */
   public void updateCurrentPeriod ()
   {
      final int currentPeriod = statP.getStatPeriod ();
      newPeriod (currentPeriod);
      if (sc != null)
         sc.initForCurrentPeriod();
      if (bc != null)
         bc.initForCurrentPeriod ();
   }

   /**
    * Returns an array containing all the measure types supported by this
    * object.
    *
    * @return an array of measure types.
    */
   public MeasureType[] getMeasures ()
   {
      return measureTypes.clone ();
   }

   /**
    * Determines if this object has a measure matrix for the measure type
    * \texttt{mt}.
    *
    * @param mt
    *           the tested measure type.
    * @return \texttt{true} if and only if a measure matrix of the tested type
    *         is available.
    */
   public boolean hasMeasureMatrix (MeasureType mt)
   {
      return measureMap.containsKey (mt);
   }

   /**
    * Returns the measure matrix corresponding to the measure type \texttt{mt}.
    * This method is mainly used by
    * the {@link #getValues(MeasureType,boolean)}
    * method of subclasses.
    * One should call {@link #getValues(MeasureType,boolean)}
    * instead to get matrices of counters from
    * measure types.
    *
    * @param mt
    *           the tested measure type.
    * @return the measure matrix.
    */
   public MeasureMatrix getMeasureMatrix (MeasureType mt)
   {
      final MeasureMatrix mm = measureMap.get (mt);
      if (mm == null)
         throw new NoSuchElementException (
            "No measure matrix for measure type " + mt);
      return mm;
   }

   /**
    * Returns the array of integral measure matrices used to compute measures
    * related to agent groups. Each element of this array corresponds to an
    * agent group.
    *
    * @return the integral measure matrices for agent groups.
    */
   public IntegralMeasureMatrix<GroupVolumeStatMeasureMatrix>[] getGroupVolumeStats ()
   {
      return groupVolume;
   }

   /**
    * Return the array of integral measure matrices used to compute queue sizes.
    * Each element of this array corresponds to a waiting queue.
    *
    * @return the integral measure matrices for waiting queues.
    */
   public IntegralMeasureMatrix<QueueSizeStatMeasureMatrix>[] getQueueSizeIntegralStats ()
   {
      return intQueueSize;
   }

   /**
    * Registers listeners required to get statistics during simulation.
    */
   public void registerListeners ()
   {
      if (cm != null)
         cc.getRouter ().addExitedContactListener (cm);
      if (outCounter != null)
for (final Dialer dialer : cc.getDialers ()) {
            if (dialer == null)
               continue;
            dialer.addReachListener (outCounter);
            dialer.addFailListener (outCounter);
         }
      if (groupVolume != null)
         for (int i = 0; i < groupVolume.length; i++) {
            final GroupVolumeStat gstat = groupVolume[i].getMeasureMatrix ();
            gstat.setAgentGroup (cc.getAgentGroup (i));
         }
      if (intQueueSize != null)
         for (int q = 0; q < intQueueSize.length; q++) {
            final QueueSizeStat qstat = intQueueSize[q].getMeasureMatrix ();
            qstat.setWaitingQueue (cc.getWaitingQueue (q));
         }
      if (sc != null)
         sc.register ();
      if (bc != null)
         bc.register ();
   }

   /**
    * Unregisters listeners required to get statistics during simulation.
    */
   public void unregisterListeners ()
   {
      if (cm != null)
         cc.getRouter ().removeExitedContactListener (cm);
      if (outCounter != null)
for (final Dialer dialer : cc.getDialers ()) {
            if (dialer == null)
               continue;
            dialer.removeReachListener (outCounter);
            dialer.removeFailListener (outCounter);
         }
      if (groupVolume != null)
for (final IntegralMeasureMatrix<GroupVolumeStatMeasureMatrix> grpVol : groupVolume) {
            final GroupVolumeStatMeasureMatrix gstat = grpVol
                  .getMeasureMatrix ();
            gstat.setAgentGroup (null);
         }
      if (intQueueSize != null)
for (final IntegralMeasureMatrix<QueueSizeStatMeasureMatrix> iqs : intQueueSize) {
            final QueueSizeStatMeasureMatrix qstat = iqs.getMeasureMatrix ();
            qstat.setWaitingQueue (null);
         }
      if (sc != null)
         sc.unregister ();
      if (bc != null)
         bc.unregister ();
   }

   /**
    * Returns the number of periods in matrices of statistical
    * probes used to collect
    * statistics about the simulation.
    * This usually returns $P'$, the number of segments
    * regrouping main periods.
    * However, for steady-state simulations, this returns 1.
    *
    * @return the number of periods for statistics.
    */
   public abstract int getNumPeriodsForStatProbes ();

   /**
    * Converts a matrix of counters constructed
    * during the simulation to a matrix of double-precision observations
    * to be added to a
    * matching matrix of tallies. The format of raw measures stored into
    * the matrix of counters is specific to the simulation type. This method formats
    * these measures into a matrix with one row for each measure type, and one
    * column for each
    * segment of main periods.
    *
    * If \texttt{norm} is \texttt{true},
    * the measures are normalized to the default time unit if they correspond to
    * durations. This normalization is performed by calling
    * {@link #timeNormalize(MeasureType,DoubleMatrix2D)}. Otherwise, time durations are relative to the
    * length of the corresponding period.
    *
    * Matrices of counters have a number of periods
    * depending on the type of
    * measures collected.
    * The output matrix of observations has {@link #getNumPeriodsForStatProbes()}
    * columns. See the documentation of {@link MeasureType} for more
    * information about measure types.
    *
    * @param mt
    *           the measure type queried.
    * @param norm
    *           determines if normalization to default time unit is done.
    * @return the matrix of values.
    */
   public abstract DoubleMatrix2D getValues (MeasureType mt, boolean norm);

   /**
    * Normalizes the measures in \texttt{m} using simulation time. This method
    * must normalize time durations to the default simulation time unit by
    * dividing every value by the correct period duration.
    * The given matrix should have {@link #getNumPeriodsForStatProbes()}
    * columns.
    *
    * @param mt
    *           the type of measure being processed.
    * @param m
    *           the matrix of values, obtained by
    *           {@link #getValues(MeasureType,boolean)}.
    */
   public abstract void timeNormalize (MeasureType mt, DoubleMatrix2D m);
}
