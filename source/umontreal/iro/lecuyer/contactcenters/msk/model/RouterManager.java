package umontreal.iro.lecuyer.contactcenters.msk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.collections.DescendingOrderComparator;
import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.ToggleElement;
import umontreal.iro.lecuyer.contactcenters.app.RouterPolicyType;
import umontreal.iro.lecuyer.contactcenters.msk.conditions.Condition;
import umontreal.iro.lecuyer.contactcenters.msk.params.CallTypeRoutingParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.DoubleArrayWithMinWaitingTime;
import umontreal.iro.lecuyer.contactcenters.msk.params.RouterParams;
import umontreal.iro.lecuyer.contactcenters.msk.spi.RouterFactory;
import umontreal.iro.lecuyer.contactcenters.router.AgentSelectionScore;
import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouter;
import umontreal.iro.lecuyer.contactcenters.router.AgentsPrefRouterWithDelays;
import umontreal.iro.lecuyer.contactcenters.router.ContactSelectionScore;
import umontreal.iro.lecuyer.contactcenters.router.ExpDelayRouter;
import umontreal.iro.lecuyer.contactcenters.router.LocalSpecRouter;
import umontreal.iro.lecuyer.contactcenters.router.LongestQueueFirstRouter;
import umontreal.iro.lecuyer.contactcenters.router.LongestWeightedWaitingTimeRouter;
import umontreal.iro.lecuyer.contactcenters.router.OverflowAndPriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueueAtLastGroupRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueuePriorityRouter;
import umontreal.iro.lecuyer.contactcenters.router.QueueRatioOverflowRouter;
import umontreal.iro.lecuyer.contactcenters.router.RankFunction;
import umontreal.iro.lecuyer.contactcenters.router.Router;
import umontreal.iro.lecuyer.contactcenters.router.RoutingTableUtils;
import umontreal.iro.lecuyer.contactcenters.router.SingleFIFOQueueRouter;
import umontreal.iro.lecuyer.contactcenters.server.DetailedAgentGroup;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;

/**
 * Manages the creation of the router as well as the data structures containing
 * routing information.
 * This class provides the necessary facility to
 * read and validate routing tables stored in
 * {@link RouterParams} instances, construct
 * missing routing tables according to the rules
 * specified in {@link RouterParams}, and
 * create the appropriate {@link Router} instance used
 * for simulation.
 */
public class RouterManager {
   private static final List<RouterFactory> routerFactories = new ArrayList<RouterFactory> ();
   private static final ServiceLoader<RouterFactory> routerFactoryLoader =
      ServiceLoader.load (RouterFactory.class);
   private Logger logger = Logger.getLogger
      ("umontreal.iro.lecuyer.msk.model");
   private CallCenter cc;
   private Map<String, Object> properties;
   private Router router;
   private int[][] typeToGroupMap;
   private int[][] groupToTypeMap;
   private double[][] ranksTG;
   private double[][] ranksGT;
   private final SortedMap<Double, double[][]> ranksGTDelayMap =
      new TreeMap<Double, double[][]> (new DescendingOrderComparator<Double>());
   private double[][] weightsTG;
   private double[][] weightsGT;
   private double[] queueWeights;
   private double[][] delaysGT;
   private boolean[][] incidenceMatrixTG;
   private boolean[][] incidenceMatrixGT;
   private int[] typeRegions;
   private int[] groupRegions;
   private int[] skillCounts;
   private CallCenterRoutingStageInfo[][] stages;

   /**
    * Constructs a new router manager using the call center
    * model \texttt{cc}, and the router's parameters
    * \texttt{par}.
    * @param cc the call center model.
    * @param par the router's parameters.
    * @throws RouterCreationException if a problem
    * occurs during the construction of the router.
    */
   public RouterManager (CallCenter cc, RouterParams par)
         throws RouterCreationException {
      this.cc = cc;
      router = createRouter (par);

      final int I = cc.getNumAgentGroups();
      for (int i = 0; i < I; i++) {
         if (!router.needsDetailedAgentGroup (i))
            continue;
         if (cc.getAgentGroup (i) instanceof DetailedAgentGroup)
            continue;
         logger.warning ("The router policy requires that agent group " + i + " be detailed; use detailed=\"true\" in agentGroup element");
      }
   }

   /**
    * Returns a reference to the router
    * managed by this object.
    * @return the managed router.
    */
   public Router getRouter () {
      return router;
   }

   /**
    * Sets the managed router to \texttt{router}.
    * @param router the new managed router.
    */
   public void setRouter (Router router) {
      if (router == null)
         throw new NullPointerException
         ("The given router must not be null");
      this.router = router;
   }
   
   public Map<String, Object> getProperties() {
      return properties;
   }

   private boolean creatingTypeToGroupMap = false;

   /**
    * Initializes the type-to-group map from
    * the router parameters \texttt{par}, or
    * constructs a new type-to-group map from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{typeToGroupMap}
    * attribute.
    * This method does nothing if
    * a type-to-group map was already constructed.
    * The obtained type-to-group map can be accessed
    * through {@link #getTypeToGroupMap()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initTypeToGroupMap (RouterParams par) {
      if (typeToGroupMap != null)
         return;
      if (creatingTypeToGroupMap)
         throw new IllegalArgumentException ("Unavailable type-to-group map");
      if (par.isSetTypeToGroupMap ()) {
         typeToGroupMap = ArrayConverter.unmarshalArray (par
               .getTypeToGroupMap ());
         if (typeToGroupMap.length != cc.getNumContactTypes ())
            throw new IllegalArgumentException
            ("The type-to-group map must have one row per contact type");
         RoutingTableUtils.checkTypeToGroupMap (cc.getNumAgentGroups (),
               typeToGroupMap);
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getTypeToGroupMap () != null) {
         creatingTypeToGroupMap = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getTypeToGroupMap ()) {
            case GROUP_TO_TYPE_MAP:
               sourceName = "groupToTypeMap";
               initGroupToTypeMap (par);
               typeToGroupMap = RoutingTableUtils.getTypeToGroupMap (cc
                     .getNumContactTypes (), groupToTypeMap);
               break;
            case INCIDENCE_MATRIX_TG:
               sourceName = "type-to-group incidence matrix";
               initIncidenceMatrixTG (par);
               final boolean[][] m = ArrayUtil.getTranspose (incidenceMatrixTG);
               typeToGroupMap = RoutingTableUtils.getTypeToGroupMap (m);
               break;
            case RANKS_TG:
               sourceName = "type-to-group matrix of ranks";
               initRanksTG (par);
               typeToGroupMap = RoutingTableUtils
               .getTypeToGroupMap (ranksTG);
               break;
            case RANKS_TG_AND_REGIONS:
               sourceName = "type-to-group matrix of ranks, and regions";
               initRanksTG (par);
               initTypeRegions ();
               initGroupRegions ();
               typeToGroupMap = RoutingTableUtils.getTypeToGroupMap (
                     ranksTG, typeRegions, groupRegions);
               break;
            default:
               throw new AssertionError ("Unknown routing table source "
                     + par.getRoutingTableSources ().getTypeToGroupMap ()
                           .value ());
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable type-to-group map";
            else
               msg = "Cannot create type-to-group map from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingTypeToGroupMap = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable type-to-group map");
   }

   private boolean creatingGroupToTypeMap = false;

   /**
    * Initializes the group-to-type map from
    * the router parameters \texttt{par}, or
    * constructs a new group-to-type map from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{groupToTypeMap}
    * attribute.
    * This method does nothing if
    * a group-to-type map was already constructed.
    * The obtained group-to-type map can be accessed
    * through {@link #getGroupToTypeMap()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initGroupToTypeMap (RouterParams par) {
      if (groupToTypeMap != null)
         return;
      if (creatingGroupToTypeMap)
         throw new IllegalArgumentException ("Unavailable group-to-type map");
      if (par.isSetGroupToTypeMap ()) {
         groupToTypeMap = ArrayConverter.unmarshalArray (par
               .getGroupToTypeMap ());
         if (groupToTypeMap.length != cc.getNumAgentGroups ())
            throw new IllegalArgumentException
            ("The group-to-type map must have one row per agent group");
         RoutingTableUtils.checkGroupToTypeMap (cc.getNumContactTypes (),
               groupToTypeMap);
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getGroupToTypeMap () != null) {
         creatingGroupToTypeMap = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getGroupToTypeMap ()) {
            case TYPE_TO_GROUP_MAP:
               sourceName = "type-to-group map";
               initTypeToGroupMap (par);
               groupToTypeMap = RoutingTableUtils.getGroupToTypeMap (cc
                     .getNumAgentGroups (), typeToGroupMap);
               break;
            case INCIDENCE_MATRIX_GT:
               sourceName = "group-to-type incidence matrix";
               initIncidenceMatrixGT (par);
               groupToTypeMap = RoutingTableUtils
               .getGroupToTypeMap (incidenceMatrixGT);
               break;
            case RANKS_GT:
               sourceName = "group-to-type matrix of ranks";
               initRanksGT (par);
               groupToTypeMap = RoutingTableUtils
               .getGroupToTypeMap (ranksGT);
               break;
            case RANKS_GT_AND_REGIONS:
               sourceName = "group-to-type matrix of ranks, and regions";
               initRanksGT (par);
               initTypeRegions ();
               initGroupRegions ();
               groupToTypeMap = RoutingTableUtils.getGroupToTypeMap (
                     ranksGT, typeRegions, groupRegions);
               break;
            default:
               throw new AssertionError ("Unknown routing table source "
                     + par.getRoutingTableSources ().getGroupToTypeMap ()
                           .value ());
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable group-to-type map";
            else
               msg = "Cannot create group-to-type map from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingGroupToTypeMap = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable group-to-type map");
   }

   private boolean creatingRanksTG = false;

   /**
    * Initializes the type-to-group matrix of ranks from
    * the router parameters \texttt{par}, or
    * constructs a new type-to-group matrix of ranks from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{ranksTG}
    * attribute.
    * This method does nothing if
    * a type-to-group matrix of ranks was already constructed.
    * The obtained matrix can be accessed
    * through {@link #getRanksTG()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initRanksTG (RouterParams par) {
      if (ranksTG != null)
         return;
      if (creatingRanksTG)
         throw new IllegalArgumentException ("Unavailable type-to-group matrix of ranks");
      if (par.isSetRanksTG ()) {
         final double[][] ranksTG1 = ArrayConverter.unmarshalArray (par.getRanksTG ());
         try {
            checkMatrixTG (ranksTG1);
         }
         catch (final IllegalArgumentException iae) {
            throw new IllegalArgumentException (
                  "Invalid type-to-group matrix of ranks", iae);
         }
         this.ranksTG = ranksTG1;
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getRanksTG () != null) {
         creatingRanksTG = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getRanksTG ()) {
            case TYPE_TO_GROUP_MAP:
               sourceName = "type-to-group map";
               initTypeToGroupMap (par);
               ranksTG = RoutingTableUtils.getRanksFromTG (cc
                     .getNumAgentGroups (), typeToGroupMap);
               break;
            case RANKS_GT:
               sourceName = "group-to-type matrix of ranks";
               initRanksGT (par);
               ranksTG = ArrayUtil.getTranspose (ranksGT);
               break;
            case INCIDENCE_MATRIX_TG:
               sourceName = "type-to-group incidence matrix";
               initIncidenceMatrixTG (par);
               ranksTG = new double[incidenceMatrixTG.length][incidenceMatrixTG[0].length];
               for (int k = 0; k < ranksTG.length; k++)
                  for (int i = 0; i < ranksTG[k].length; i++)
                     ranksTG[k][i] = incidenceMatrixTG[k][i] ? 1
                           : Double.POSITIVE_INFINITY;
               break;
            case INCIDENCE_MATRIX_TG_AND_SKILL_COUNTS:
               sourceName = "type-to-group incidence matrix, and skill counts";
               initIncidenceMatrixTG (par);
               initSkillCounts (par);
               ranksTG = new double[incidenceMatrixTG.length][incidenceMatrixTG[0].length];
               for (int k = 0; k < ranksTG.length; k++)
                  for (int i = 0; i < ranksTG[k].length; i++)
                     ranksTG[k][i] = incidenceMatrixTG[k][i] ? skillCounts[i]
                           : Double.POSITIVE_INFINITY;
               break;
            default:
               throw new AssertionError ("Unknown routing table source "
                     + par.getRoutingTableSources ().getRanksTG ().value ());
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable type-to-group matrix of ranks";
            else
               msg = "Cannot create type-to-group matrix of ranks from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingRanksTG = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable type-to-group matrix of ranks");
   }

   private boolean creatingRanksGT = false;

   /**
    * Initializes the group-to-type matrix of ranks from
    * the router parameters \texttt{par}, or
    * constructs a new group-to-type matrix of ranks from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{ranksGT}
    * attribute.
    * This method does nothing if
    * a group-to-type matrix of ranks was already constructed.
    * The obtained matrix can be accessed
    * through {@link #getRanksGT()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initRanksGT (RouterParams par) {
      if (ranksGT != null)
         return;
      if (creatingRanksGT)
         throw new IllegalArgumentException ("Unavailable group-to-type matrix of ranks");
      if (par.isSetRanksGT ()) {
         final double[][] ranksGT1 = ArrayConverter.unmarshalArray (par.getRanksGT ());
         try {
            checkMatrixGT (ranksGT1);
         }
         catch (final IllegalArgumentException iae) {
            throw new IllegalArgumentException (
                  "Invalid group-to-type matrix of ranks", iae);
         }
         this.ranksGT = ranksGT1;
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getRanksGT () != null) {
         creatingRanksGT = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getRanksGT ()) {
            case GROUP_TO_TYPE_MAP:
               sourceName = "group-to-type map";
               initGroupToTypeMap (par);
               ranksTG = RoutingTableUtils.getRanksFromGT (cc
                     .getNumContactTypes (), groupToTypeMap);
               break;
            case RANKS_TG:
               sourceName = "type-to-group matrix of ranks";
               initRanksTG (par);
               ranksGT = ArrayUtil.getTranspose (ranksTG);
               break;
            case INCIDENCE_MATRIX_GT:
               sourceName = "group-to-type incidence matrix";
               initIncidenceMatrixGT (par);
               ranksGT = new double[incidenceMatrixGT.length][incidenceMatrixGT[0].length];
               for (int i = 0; i < ranksGT.length; i++)
                  for (int k = 0; k < ranksGT[i].length; k++)
                     ranksGT[i][k] = incidenceMatrixGT[i][k] ? 1
                           : Double.POSITIVE_INFINITY;
               break;
            case INCIDENCE_MATRIX_GT_AND_SKILL_COUNTS:
               sourceName = "group-to-type incidence matrix, and skill counts";
               initIncidenceMatrixGT (par);
               initSkillCounts (par);
               ranksGT = new double[incidenceMatrixGT.length][incidenceMatrixGT[0].length];
               for (int i = 0; i < ranksGT.length; i++)
                  for (int k = 0; k < ranksGT[i].length; k++)
                     ranksGT[i][k] = incidenceMatrixGT[i][k] ? skillCounts[i]
                           : Double.POSITIVE_INFINITY;
               break;
            default:
               throw new AssertionError ("Unknown routing table source "
                     + par.getRoutingTableSources ().getRanksGT ().value ());
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable group-to-type matrix of ranks";
            else
               msg = "Cannot create group-to-type matrix of ranks from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingRanksGT = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable group-to-type matrix of ranks");
   }
   
   /**
    * Initializes the
    * auxiliary group-to-type matrix of ranks associated
    * with minimal waiting times.
    * This method does nothing if no
    * \texttt{ranksGTUpdate} elements is given
    * in \texttt{par}.
    * These matrices can be retrieved
    * using the {@link #getRanksGTDelay()} method.
    * @param par the router's parameters.
    */
   public void initRanksGTUpdate (RouterParams par) {
      ranksGTDelayMap.clear ();
      if (!par.isSetRanksGTUpdate ())
         return;
      for (final DoubleArrayWithMinWaitingTime r : par.getRanksGTUpdate ()) {
         final double minWaitingTime = cc.getTime (r.getMinWaitingTime ());
         final double[][] ranksGT1 = ArrayConverter.unmarshalArray (r);
         try {
            checkMatrixGT (ranksGT1);
         }
         catch (final IllegalArgumentException iae) {
            throw new IllegalArgumentException (
                  "Invalid group-to-type matrix of ranks for minimal waiting time " + r.getMinWaitingTime ().toString (), iae);
         }
         ranksGTDelayMap.put (minWaitingTime, ranksGT1);
      }
   }

   private void checkMatrixTG (double[][] matrixTG) {
      ArrayUtil.checkRectangularMatrix (matrixTG);
      if (matrixTG.length != cc.getNumContactTypes ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixTG.length
               + " rows but it needs "
               + cc.getNumContactTypes () + " rows");
      if (matrixTG.length > 0 && matrixTG[0].length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixTG[0].length
               + " columns but it needs "
               + cc.getNumAgentGroups () + " columns");
   }

   private void checkMatrixTG (boolean[][] matrixTG) {
      ArrayUtil.checkRectangularMatrix (matrixTG);
      if (matrixTG.length != cc.getNumContactTypes ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixTG.length
               + " rows but it needs "
               + cc.getNumContactTypes () + " rows");
      if (matrixTG.length > 0 && matrixTG[0].length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixTG[0].length
               + " columns but it needs "
               + cc.getNumAgentGroups () + " columns");
   }
   
   private void checkMatrixGT (double[][] matrixGT) {
      ArrayUtil.checkRectangularMatrix (matrixGT);
      if (matrixGT.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixGT.length
               + " rows but it needs "
               + cc.getNumAgentGroups () + " rows");
      if (matrixGT.length > 0 && matrixGT[0].length != cc.getNumContactTypes ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixGT[0].length
               + " columns but it needs "
               + cc.getNumContactTypes () + " columns");
   }
   
   private void checkMatrixGT (boolean[][] matrixGT) {
      ArrayUtil.checkRectangularMatrix (matrixGT);
      if (matrixGT.length != cc.getNumAgentGroups ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixGT.length
               + " rows but it needs "
               + cc.getNumAgentGroups () + " rows");
      if (matrixGT.length > 0 && matrixGT[0].length != cc.getNumContactTypes ())
         throw new IllegalArgumentException ("The given matrix has "
               + matrixGT[0].length
               + " columns but it needs "
               + cc.getNumContactTypes () + " columns");
   }
   
   /**
    * Initializes the type-to-group matrix of weights using
    * the router's parameters \texttt{par}.
    * If no such matrix is defined, a matrix filled with 1's is
    * created.
    * An illegal-argument exception is thrown
    * if any error occurs during the construction
    * and validation of the matrix of weights.
    * The matrix can be accessed using the
    * {@link #getWeightsTG()} method if this method
    * succeeds.
    * @param par the router's parameters.
    */
   public void initWeightsTG (RouterParams par) {
      if (weightsTG != null)
         return;
      final int K = cc.getNumContactTypes ();
      final int I = cc.getNumAgentGroups ();
      if (par.isSetWeightsTG ()) {
         weightsTG = ArrayConverter.unmarshalArray (par.getWeightsTG ());
         try {
            checkMatrixTG (weightsTG);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Invalid type-to-group matrix of weights");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      else {
         weightsTG = new double[K][I];
         for (int i = 0; i < I; i++)
            for (int k = 0; k < K; k++)
               weightsTG[k][i] = 1;
      }
   }
   
   /**
    * Initializes the group-to-type matrix of weights using
    * the router's parameters \texttt{par}.
    * If no such matrix is defined in \texttt{par},
    * one is initialized from the queue weights.
    * An illegal-argument exception is thrown
    * if any error occurs during the construction
    * and validation of the matrix of weights.
    * The matrix can be accessed using the
    * {@link #getWeightsGT()} method if this method
    * succeeds.
    * @param par the router's parameters.
    */
   public void initWeightsGT (RouterParams par) {
      if (weightsGT != null)
         return;
      final int K = cc.getNumContactTypes ();
      final int I = cc.getNumAgentGroups ();
      if (par.isSetWeightsGT ()) {
         weightsGT = ArrayConverter.unmarshalArray (par.getWeightsGT ());
         try {
            checkMatrixGT (weightsGT);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Invalid group-to-type matrix of weights");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      else {
         initQueueWeights (par);
         weightsGT = new double[I][K];
         for (int i = 0; i < I; i++)
            for (int k = 0; k < K; k++) {
               double w;
               if (queueWeights != null && queueWeights.length == K)
                  w = queueWeights[k];
               else
                  w = 1;
               weightsGT[i][k] = w;
            }
      }
   }

   /**
    * Initializes the matrix of delays from the router's parameters
    * \texttt{par}.
    * If no matrix of delays is defined in
    * \texttt{par}, this method creates
    * a $I\times K$ matrix filled with 0's.
    * The constructed matrix can be accessed using
    * {@link #getDelaysGT()}.
    * @param par the router's parameters.
    */
   public void initDelays (RouterParams par) {
      if (delaysGT != null)
         return;
      if (par.isSetDelaysGT ()) {
         final Duration[][] delaysGT1 = ArrayConverter.unmarshalArray (par
               .getDelaysGT ());
         this.delaysGT = cc.getTime (delaysGT1);
         try {
            checkMatrixGT (this.delaysGT);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Invalid delays matrix");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      else
         delaysGT = new double[cc.getNumAgentGroups()][cc.getNumContactTypes()];
   }

   /**
    * Initializes the queue weights using the
    * router's parameters \texttt{par}.
    * If no queue weights are specified in \texttt{par},
    * this method creates a vector of queue weights using
    * the weights associated with each call type.
    * The queue weights can be accessed using
    * the {@link #getQueueWeights()} method if
    * this method succeeds.
    * @param par the router's parameters.
    */
   public void initQueueWeights (RouterParams par) {
      if (queueWeights != null)
         return;
      if (par.isSetQueueWeights()) {
         queueWeights = par.getQueueWeights();
         if (queueWeights.length != cc.getNumContactTypes ())
            throw new IllegalArgumentException
            ("The length of queueWeights, " + queueWeights.length + 
                  ", must correspond to the number of agent groups " + cc.getNumAgentGroups());
      }
      else {
         queueWeights = new double[cc.getNumContactTypes ()];
         for (int k = 0; k < queueWeights.length; k++)
            queueWeights[k] = cc.getCallFactory (k).getWeight ();
      }
   }

   private boolean creatingIncidenceMatrixTG = false;

   /**
    * Initializes the type-to-group incidence matrix from
    * the router parameters \texttt{par}, or
    * constructs a new type-to-group incidence matrix from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{incidenceMatrixTG}
    * attribute.
    * This method does nothing if
    * a type-to-group incidence matrix was already constructed.
    * The obtained matrix can be accessed
    * through {@link #getIncidenceMatrixTG()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initIncidenceMatrixTG (RouterParams par) {
      if (incidenceMatrixTG != null)
         return;
      if (creatingIncidenceMatrixTG)
         throw new IllegalArgumentException ("Unavailable incidence matrix");
      if (par.isSetIncidenceMatrixTG ()) {
         incidenceMatrixTG = ArrayConverter.unmarshalArray (par
               .getIncidenceMatrixTG ());
         try {
            ArrayUtil.checkRectangularMatrix (incidenceMatrixTG);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Non-rectangular incidence matrix");
            iaeOut.initCause (iae);
            throw iae;
         }
         try {
            checkMatrixTG (incidenceMatrixTG);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            ("Invalid type-to-group incidence matrix");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getIncidenceMatrixTG () != null) {
         creatingIncidenceMatrixTG = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getIncidenceMatrixTG ()) {
            case INCIDENCE_MATRIX_GT:
               sourceName = "group-to-type incidence matrix";
               initIncidenceMatrixGT (par);
               incidenceMatrixTG = ArrayUtil
               .getTranspose (incidenceMatrixGT);
               break;
            case RANKS_TG:
               sourceName = "type-to-group matrix of ranks";
               initRanksTG (par);
               incidenceMatrixTG = new boolean[ranksTG.length][ranksTG[0].length];
               for (int k = 0; k < incidenceMatrixTG.length; k++)
                  for (int i = 0; i < incidenceMatrixTG[k].length; i++)
                     incidenceMatrixTG[k][i] = !Double.isInfinite (ranksTG[k][i]);
               break;
            case TYPE_TO_GROUP_MAP:
               sourceName = "type-to-group map";
               initTypeToGroupMap (par);
               final boolean[][] tmp = RoutingTableUtils.getIncidenceFromTG (cc
                     .getNumAgentGroups (), typeToGroupMap);
               incidenceMatrixTG = ArrayUtil
               .getTranspose (tmp);
               break;
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable type-to-group incidence matrix";
            else
               msg = "Cannot create type-to-group incidence matrix from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingIncidenceMatrixTG = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable incidence matrix");
   }

   private boolean creatingIncidenceMatrixGT = false;

   /**
    * Initializes the group-to-type incidence matrix from
    * the router parameters \texttt{par}, or
    * constructs a new group-to-type incidence matrix from
    * other information if
    * {@link RouterParams#getRoutingTableSources()}
    * defines the \texttt{incidenceMatrixGT}
    * attribute.
    * This method does nothing if
    * a group-to-type incidence matrix was already constructed.
    * The obtained matrix can be accessed
    * through {@link #getIncidenceMatrixGT()} after
    * this method succeeds, and
    * an illegal-argument exception is thrown
    * if this method fails. 
    * @param par the router's parameters.
    */
   public void initIncidenceMatrixGT (RouterParams par) {
      if (incidenceMatrixGT != null)
         return;
      if (creatingIncidenceMatrixGT)
         throw new IllegalArgumentException ("Unavailable incidence matrix");
      if (par.isSetIncidenceMatrixGT ()) {
         incidenceMatrixGT = ArrayConverter.unmarshalArray (par
               .getIncidenceMatrixGT ());
         try {
            ArrayUtil.checkRectangularMatrix (incidenceMatrixGT);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException (
                  "Non-rectangular incidence matrix");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         try {
            checkMatrixGT (incidenceMatrixGT);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            ("Invalid group-to-type incidence matrix");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      else if (par.getRoutingTableSources () != null
            && par.getRoutingTableSources ().getIncidenceMatrixGT () != null) {
         creatingIncidenceMatrixGT = true;
         String sourceName = null;
         try {
            switch (par.getRoutingTableSources ().getIncidenceMatrixGT ()) {
            case INCIDENCE_MATRIX_TG:
               sourceName = "type-to-group incidence matrix";
               initIncidenceMatrixTG (par);
               incidenceMatrixGT = ArrayUtil
               .getTranspose (incidenceMatrixTG);
               break;
            case RANKS_GT:
               sourceName = "group-to-type matrix of ranks";
               initRanksGT (par);
               incidenceMatrixGT = new boolean[ranksGT.length][ranksGT[0].length];
               for (int i = 0; i < incidenceMatrixGT.length; i++)
                  for (int k = 0; k < incidenceMatrixGT[i].length; k++)
                     incidenceMatrixGT[i][k] = !Double.isInfinite (ranksGT[i][k]);
               break;
            case GROUP_TO_TYPE_MAP:
               sourceName = "group-to-type map";
               initGroupToTypeMap (par);
               incidenceMatrixGT = RoutingTableUtils.getIncidenceFromGT (cc
                     .getNumContactTypes (), groupToTypeMap);
               break;
            }
         }
         catch (final IllegalArgumentException iae) {
            String msg;
            if (sourceName == null)
               msg = "Unavailable group-to-type incidence matrix";
            else
               msg = "Cannot create group-to-type incidence matrix from " + sourceName;
            final IllegalArgumentException iaeOut = new IllegalArgumentException (msg);
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         finally {
            creatingIncidenceMatrixGT = false;
         }
      }
      else
         throw new IllegalArgumentException ("Unavailable incidence matrix");
   }

   /**
    * Initializes the skill counts using the router's
    * parameters \texttt{par}.
    * If no skill count is specified in \texttt{par},
    * the skill counts are initialized from agent groups'
    * \texttt{skillCount} attribute.
    * If the skill count is not specified explicitly for
    * at least one agent group, the
    * group-to-type incidence matrix is initialized and
    * used to count the number of call types accessible for
    * this agent group.
    * @param par the router's parameters.
    */
   public void initSkillCounts (RouterParams par) {
      if (skillCounts != null)
         return;
      if (par.isSetSkillCounts ()) {
         skillCounts = par.getSkillCounts ();
         if (skillCounts.length != cc.getNumAgentGroups ())
            throw new IllegalArgumentException (
                  "A skill count is required for each agent group");
      }
      else {
         skillCounts = new int[cc.getNumAgentGroups ()];
         boolean undefinedSkillCounts = false;
         for (int i = 0; i < skillCounts.length; i++) {
            skillCounts[i] = cc.getAgentGroupManager (i).getSkillCount ();
            if (skillCounts[i] == Integer.MAX_VALUE)
               undefinedSkillCounts = true;
         }
         if (undefinedSkillCounts) {
            try {
               initIncidenceMatrixGT (par);
            }
            catch (final IllegalArgumentException iae) {
               final IllegalArgumentException iaeOut = new IllegalArgumentException (
                     "Cannot initialize the skill counts from the group-to-type incidence matrix");
               iaeOut.initCause (iae);
               throw iaeOut;
            }
            for (int i = 0; i < skillCounts.length; i++) {
               if (skillCounts[i] != Integer.MAX_VALUE)
                  continue;
               skillCounts[i] = 0;
               for (int k = 0; k < incidenceMatrixGT[i].length; k++)
                  if (incidenceMatrixGT[i][k])
                     ++skillCounts[i];
            }
         }
      }
   }
   
   public CallCenterRoutingStageInfo[][] getRoutingStages() {
      return stages;
   }
   
   /**
    * Initializes the routing stages for the
    * overflow-and-priority routing policy from
    * parameters in \texttt{par}.
    * This method does nothing if the stages, returned by the
    * {@link #getRoutingStages()} method, are already
    * initialized.
    * Otherwise, it processes parameters in
    * \texttt{par} to initialize the stages.
    * @param par the routing parameters.
    */
   public void initStages (RouterParams par) {
      if (stages != null)
         return;
      if (!par.isSetCallTypeRouting ())
         throw new IllegalArgumentException
         ("Missing callTypeRouting element");
      if (par.getCallTypeRouting ().size () < cc.getNumContactTypes ())
         throw new IllegalArgumentException
         ("A callTypeRouting element is needed for each call type");
      stages = new CallCenterRoutingStageInfo[cc.getNumContactTypes ()][];
      for (int k = 0; k < stages.length; k++) {
         final CallTypeRoutingParams park = par.getCallTypeRouting ().get (k);
         stages[k] = new CallCenterRoutingStageInfo[park.getStage ().size ()];
         for (int i = 0; i < stages[k].length; i++) {
            try {
               stages[k][i] = new CallCenterRoutingStageInfo (cc, k, park.getStage ().get (i));
            }
            catch (IllegalArgumentException iae) {
               IllegalArgumentException iaeOut = new IllegalArgumentException
               ("Exception occurred while processing information about stage " + i + " of routing for call type " + k);
               iaeOut.initCause (iae);
               throw iaeOut;
            }
         }
      }
   }

   /**
    * Constructs and returns the router to be managed.
    * This method uses {@link RouterParams#getRouterPolicy()}
    * to get a type identifier for the router's policy.
    * It then retrieves parameters and initializes a router
    * specific to the given type.
    * If the name of the policy corresponds to a constant in
    * {@link RouterPolicyType}, the method handles its construction
    * directly.
    * Otherwise, it queries every factory registered using
    * {@link #addRouterFactory(RouterFactory)} until
    * it gets one capable of creating the policy.
    * If no such factory exists, it uses the
    * {@link ServiceLoader} class to find a router policy factory
    * dynamically.
    * If that last step fails, the method throws a router-creation
    * exception.
    * @param par the parameters of the router.
    * @return the constructed router.
    * @exception RouterCreationException if an error occurs during
    * the construction.
    */
   protected Router createRouter (RouterParams par)
         throws RouterCreationException {
      properties = ParamReadHelper.unmarshalProperties (par.getProperties ());
      RouterPolicyType rp;
      try {
         rp = RouterPolicyType.valueOf (par.getRouterPolicy ());
      }
      catch (final IllegalArgumentException iae) {
         rp = null;
      }
      if (rp != null) {
         try {
            switch (rp) {
            case LONGESTWEIGHTEDWAITINGTIME:
               initQueueWeights (par);
            case QUEUEPRIORITY:
            case LONGESTQUEUEFIRST:
            case SINGLEFIFOQUEUE:
               initTypeToGroupMap (par);
               initGroupToTypeMap (par);
               break;
            case QUEUEATLASTGROUP:
               initTypeToGroupMap (par);
               break;
            case LOCALSPEC:
               initTypeRegions ();
               initGroupRegions ();
            case AGENTSPREF:
            case AGENTSPREFWITHDELAYS:
               initRanksTG (par);
               initRanksGT (par);
               initWeightsTG (par);
               initWeightsGT (par);
               if (rp == RouterPolicyType.AGENTSPREFWITHDELAYS) {
                  initDelays (par);
                  initRanksGTUpdate (par);
               }
               break;
            case QUEUERATIOOVERFLOW:
               initRanksTG (par);
               break;
            case EXPDELAY:
               initWeightsTG (par);
               break;
            case OVERFLOWANDPRIORITY:
               initStages (par);
               initWeightsTG (par);
               initWeightsGT (par);
               break;
            }
         }
         catch (final IllegalArgumentException iae) {
            throw new RouterCreationException (
                  "Error initializing data structures", iae);
         }

         final RandomStreams streams = cc.getRandomStreams ();
         switch (rp) {
         case QUEUEPRIORITY:
            return new QueuePriorityRouter (typeToGroupMap, groupToTypeMap);
         case QUEUEATLASTGROUP:
            return new QueueAtLastGroupRouter (cc.getNumAgentGroups (),
                  typeToGroupMap);
         case LONGESTQUEUEFIRST:
            return new LongestQueueFirstRouter (typeToGroupMap, groupToTypeMap);
         case SINGLEFIFOQUEUE:
            return new SingleFIFOQueueRouter (typeToGroupMap, groupToTypeMap);
         case LONGESTWEIGHTEDWAITINGTIME:
            return new LongestWeightedWaitingTimeRouter (typeToGroupMap,
                  groupToTypeMap, queueWeights);
         case AGENTSPREF:
            final AgentsPrefRouter router1 = new AgentsPrefRouter (ranksTG,
                  ranksGT, weightsTG, weightsGT);
            initAgentsPrefBased (par, router1);
            return router1;
         case AGENTSPREFWITHDELAYS:
            final AgentsPrefRouterWithDelays router2 = new AgentsPrefRouterWithDelays (
                  ranksTG, ranksGT, weightsTG, weightsGT, delaysGT);
            router2.setOverflowTransferStatus (par.isOverflowTransfer ());
            router2.setLongestWaitingTimeStatus (par.isLongestWaitingTime ());
            for (final Map.Entry<Double, double[][]> e : ranksGTDelayMap.entrySet ())
               router2.setRanksGT (e.getKey (), e.getValue ());
            initAgentsPrefBased (par, router2);
            return router2;
         case LOCALSPEC:
            final double overflowDelay = cc
                  .getTime (par.getLocalSpecOverflowDelay ());
            final LocalSpecRouter lsRouter = new LocalSpecRouter (typeRegions,
                  groupRegions, overflowDelay, ranksTG, ranksGT, weightsTG,
                  weightsGT);
            initAgentsPrefBased (par, lsRouter);
            return lsRouter;
         case QUEUERATIOOVERFLOW:
            return new QueueRatioOverflowRouter (cc.getNumAgentGroups (),
                  ranksTG, par.getTargetQueueRatio (), par.isAllowCopies (),
                  par.isOverflowTransfer ());
         case EXPDELAY:
            return new ExpDelayRouter (weightsTG, par
                  .isRandomizedAgentSelection ()
                  && streams != null ? streams.getStreamAgentSelection ()
                  : null);
         case OVERFLOWANDPRIORITY:
            final OverflowAndPriorityRouter r = new
            OverflowAndPriorityRouter (cc.getNumAgentGroups (), stages);
            r.setWeightsTG (weightsTG);
            r.setWeightsGT (weightsGT);
            r.setAgentSelectionScore (AgentSelectionScore.valueOf (par
                  .getAgentSelectionScore ().name ()));
            r.setContactSelectionScore (ContactSelectionScore.valueOf (par
                  .getContactSelectionScore ().name ()));
            return r;
         default:
            throw new RouterCreationException ("Unrecognized routing policy "
                  + par.getRouterPolicy ());
         }
      }

      for (final RouterFactory rf : routerFactories) {
         final Router router1 = rf.createRouter (cc, this, par);
         if (router1 != null)
            return router1;
      }
      for (final RouterFactory rf : routerFactoryLoader) {
         final Router router1 = rf.createRouter (cc, this, par);
         if (router1 != null)
            return router1;
      }
      throw new RouterCreationException ("Unrecognized routing policy "
            + par.getRouterPolicy ());
   }

   /**
    * Registers the router factory \texttt{rf} for
    * router managers.
    * If the user-specified router policy does not
    * correspond to a predefined policy, the
    * registered factories are queried to find
    * one capable of creating a router.
    * This method must be called before the call-center
    * simulator is initialized. 
    * @param rf the new router factory to register.
    */
   public static void addRouterFactory (RouterFactory rf) {
      if (rf == null)
         throw new NullPointerException ();
      if (!routerFactories.contains (rf))
         routerFactories.add (rf);
   }

   /**
    * Initializes an agents preference-based router
    * using the parameters \texttt{par}.
    * This method sets the score type for contact and
    * agent selection as well as random streams for
    * randomized selections if it is enabled.
    * @param par the router's parameters.
    * @param router1 the router object.
    */
   public void initAgentsPrefBased (RouterParams par, AgentsPrefRouter router1) {
      final RandomStreams streams = cc.getRandomStreams();
      router1.setAgentSelectionScore (AgentSelectionScore.valueOf (par
            .getAgentSelectionScore ().name ()));
      router1.setContactSelectionScore (ContactSelectionScore.valueOf (par
            .getContactSelectionScore ().name ()));
      router1
            .setStreamAgentSelection (par.isRandomizedAgentSelection () ? streams
                  .getStreamAgentSelection ()
                  : null);
      router1
            .setStreamContactSelection (par.isRandomizedContactSelection () ? streams
                  .getStreamContactSelection ()
                  : null);
   }

   private Map<String, Integer> regionMap;

   /**
    * Returns the region name corresponding to region identifier \texttt{id}.
    * This must be called after {@link #initTypeRegions()} or
    * {@link #initGroupRegions()}, and throws a {@link NoSuchElementException} if
    * no region name has been associated with the given identifier.
    * 
    * @param id
    *           the region identifier.
    * @return the corresponding region name.
    * @exception NoSuchElementException
    *               if no region name is associated with the corresponding
    *               region identifier.
    */
   public String getRegion (int id) {
      if (regionMap != null)
         for (final Map.Entry<String, Integer> e : regionMap.entrySet ()) {
            final int v = e.getValue ();
            if (v == id)
               return e.getKey ();
         }
      throw new NoSuchElementException ("Undefined region identifier: " + id);
   }

   /**
    * Returns the region identifier corresponding to the region name
    * \texttt{regStr}. This method must be called only after
    * {@link #initTypeRegions()} or {@link #initGroupRegions()}, and throws a
    * {@link NoSuchElementException} if no identifier is associated with
    * \texttt{regStr}.
    * 
    * @param regStr
    *           the tested region name.
    * @return the corresponding region identifier.
    * @exception NoSuchElementException
    *               if no region identifier is associated with the given region
    *               name.
    */
   public int getRegion (String regStr) {
      if (regionMap != null) {
         final Integer v = regionMap.get (regStr);
         if (v != null)
            return v;
      }
      throw new NoSuchElementException ("Undefined region name: " + regStr);
   }

   /**
    * Clears the internal region map used by {@link #initTypeRegions()} and
    * {@link #initGroupRegions()}. After this method is called, it is not possible
    * to get the region name corresponding to the region identifiers.
    */
   public void clearRegionMap () {
      regionMap = null;
   }

   private static final String REGION_PROPERTY = "region";

   /**
    * Initializes the call type region identifiers used by the
    * local-specialist routing policy.
    * This method obtains a region name for each call type, and
    * maps each identical name to the same integer.
    * At the end of this process, the array returned by
    * {@link #getTypeRegions()} associates a region identifier
    * to each call type.
    * 
    * The region name of a call type is computed as follows.
    * First, the call type factory is obtained using
    * {@link CallCenter#getCallFactory(int)}.
    * If the properties returned by {@link CallFactory#getProperties()}
    * contains a property named \texttt{region}, its
    * value is used as the region string.
    * Otherwise, the name of the call type, returned by
    * {@link CallFactory#getName()},
    * is split using the semicolon as a delimiter, and
    * the region corresponds to the string following the
    * semicolon.
    */
   public void initTypeRegions () {
      if (typeRegions != null)
         return;
      if (regionMap == null)
         regionMap = new HashMap<String, Integer> ();
      final int numTypes = cc.getNumContactTypes ();
      typeRegions = new int[numTypes];
      for (int k = 0; k < typeRegions.length; k++) {
         final String regStr;
         if (cc.getCallFactory (k).getProperties ().containsKey (
               REGION_PROPERTY)) {
            final Object o = cc.getCallFactory (k).getProperties ().get (
                  REGION_PROPERTY);
            if (o == null)
               regStr = "null";
            else
               regStr = o.toString ().trim ();
         }
         else {
            final String typeName = cc.getCallFactory (k).getName ();
            if (typeName == null || typeName.length () == 0)
               throw new IllegalStateException (
                     "No region information available for call type " + k);
            final String[] s = typeName.split (";");
            if (s.length != 2)
               throw new IllegalStateException ("Invalid call type name: "
                     + typeName);
            regStr = s[1].trim ();
         }
         final Integer ir = regionMap.get (regStr);
         if (ir == null) {
            typeRegions[k] = regionMap.size ();
            regionMap.put (regStr, new Integer (regionMap.size ()));
         }
         else
            typeRegions[k] = ir.intValue ();
      }
   }

   /**
    * Initializes the agent group region identifiers used by the
    * local-specialist routing policy.
    * This method obtains a region name for each agent group, and
    * maps each identical name to the same integer.
    * At the end of this process, the array returned by
    * {@link #getGroupRegions()} associates a region identifier
    * to each call type.
    * 
    * The region name of an agent group is computed as follows.
    * First, the agent group manager is obtained using
    * {@link CallCenter#getAgentGroupManager(int)}.
    * If the properties returned by \texttt{getProperties()}
    * contains a property named \texttt{region}, its
    * value is used as the region string.
    * Otherwise, the name of the call type, returned by
    * \texttt{getName()},
    * is split using the semicolon as a delimiter, and
    * the region corresponds to the string following the
    * semicolon.
    */
   public void initGroupRegions () {
      if (groupRegions != null)
         return;
      if (regionMap == null)
         regionMap = new HashMap<String, Integer> ();
      final int numGroups = cc.getNumAgentGroups ();
      groupRegions = new int[numGroups];
      for (int i = 0; i < groupRegions.length; i++) {
         final String regStr;
         if (cc.getAgentGroupManager (i).getProperties ().containsKey (
               REGION_PROPERTY)) {
            final Object o = cc.getAgentGroupManager (i).getProperties ().get (
                  REGION_PROPERTY);
            if (o == null)
               regStr = "null";
            else
               regStr = o.toString ().trim ();
         }
         else {
            final String groupName = cc.getAgentGroupManager (i).getName ();
            if (groupName == null || groupName.length () == 0)
               throw new IllegalArgumentException (
                     "No region information available for agent group " + i);
            final String[] s = groupName.split (";");
            if (s.length != 2)
               throw new IllegalArgumentException ("Invalid agent group name: "
                     + groupName);
            regStr = s[1].trim ();
         }
         final Integer ir = regionMap.get (regStr);
         if (ir == null) {
            groupRegions[i] = regionMap.size ();
            regionMap.put (regStr, new Integer (regionMap.size ()));
         }
         else
            groupRegions[i] = ir.intValue ();
      }
   }

   /**
    * Returns the currently used group-to-type map.
    * If {@link #initGroupToTypeMap(RouterParams)}
    * or {@link #setGroupToTypeMap(int[][])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, this method returns an array of $I$
    * arrays giving an order list of call types for
    * each agent group.
    * @return the currently used group-to-type map.
    */
   public int[][] getGroupToTypeMap () {
      return groupToTypeMap == null ? null : ArrayUtil.deepClone (groupToTypeMap);
   }

   /**
    * Sets the group-to-type map to
    * \texttt{groupToTypeMap}.
    * @param groupToTypeMap the new group-to-type map.
    */
   public void setGroupToTypeMap (int[][] groupToTypeMap) {
      if (groupToTypeMap == null)
         this.groupToTypeMap = null;
      else {
         RoutingTableUtils.checkGroupToTypeMap (cc.getNumContactTypes(), groupToTypeMap);
         this.groupToTypeMap = ArrayUtil.deepClone (groupToTypeMap); 
      }
   }

   /**
    * Returns the currently used group-to-type incidence matrix.
    * If {@link #initIncidenceMatrixGT(RouterParams)}
    * or {@link #setIncidenceMatrixGT(boolean[][])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, this returns a $I\times K$ incidence matrix.
    * @return the currently used group-to-type incidence matrix.
    */
   public boolean[][] getIncidenceMatrixGT () {
      return incidenceMatrixGT == null ? null :
         ArrayUtil.deepClone (incidenceMatrixGT);
   }

   /**
    * Sets the group-to-type incidence matrix to
    * \texttt{incidenceMatrixGT}.
    * @param incidenceMatrixGT the group-to-type incidence matrix.
    */
   public void setIncidenceMatrixGT (boolean[][] incidenceMatrixGT) {
      if (incidenceMatrixGT == null)
         this.incidenceMatrixGT = null;
      else {
         checkMatrixGT (incidenceMatrixGT);
         this.incidenceMatrixGT = ArrayUtil.deepClone (incidenceMatrixGT);
      }
   }

   /**
    * Returns the currently used type-to-group incidence matrix.
    * If {@link #initIncidenceMatrixTG(RouterParams)}
    * or {@link #setIncidenceMatrixTG(boolean[][])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, this returns a $K\times I$ incidence matrix.
    * @return the currently used type-to-group incidence matrix.
    */
   public boolean[][] getIncidenceMatrixTG () {
      return incidenceMatrixTG == null ? null :
         ArrayUtil.deepClone (incidenceMatrixTG);
   }

   /**
    * Sets the type-to-group incidence matrix to
    * \texttt{incidenceMatrixTG}.
    * @param incidenceMatrixTG the type-to-group incidence matrix.
    */
   public void setIncidenceMatrixTG (boolean[][] incidenceMatrixTG) {
      if (incidenceMatrixTG == null)
         this.incidenceMatrixTG = null;
      else {
         checkMatrixTG (incidenceMatrixTG);
         this.incidenceMatrixTG = ArrayUtil.deepClone (incidenceMatrixTG);
      }
   }

   /**
    * Returns the currently used queue weights vector.
    * If {@link #initQueueWeights(RouterParams)}
    * or {@link #setQueueWeights(double[])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, element \texttt{k} of the returned array
    * gives the weight for contact type \texttt{k}
    * when entering in queue.
    * @return  the vector of queue weights.
    */
   public double[] getQueueWeights () {
      return queueWeights == null ? null : queueWeights.clone();
   }

   /**
    * Sets the vector of queue weights to
    * \texttt{queueWeights}.
    * @param queueWeights the new vector of queue weights.
    */
   public void setQueueWeights (double[] queueWeights) {
      if (queueWeights == null)
         this.queueWeights = null;
      else {
         if (queueWeights.length != cc.getNumContactTypes ())
            throw new IllegalArgumentException
            ("Invalid length of queue weights");
         this.queueWeights = queueWeights.clone();
      }
   }

   /**
    * Returns the currently used group-to-type matrix of ranks.
    * If {@link #initRanksGT(RouterParams)}
    * or {@link #setRanksGT(double[][])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, it returns a $I\times K$ matrix of
    * ranks.
    * @return the currently used group-to-type matrix of ranks.
    */
   public double[][] getRanksGT () {
      return ranksGT == null ? null : ArrayUtil.deepClone (ranksGT);
   }

   /**
    * Sets the group-to-type matrix of ranks to \texttt{ranksGT}.
    * @param ranksGT the new matrix of ranks.
    */
   public void setRanksGT (double[][] ranksGT) {
      if (ranksGT == null)
         this.ranksGT = null;
      else {
         checkMatrixGT (ranksGT);
         this.ranksGT = ArrayUtil.deepClone (ranksGT);
      }
   }
   
   /**
    * Returns a map giving the auxiliary matrices of ranks
    * with associated minimal waiting times.
    * Each entry of the returned map has a key giving
    * the minimal waiting time, and a value corresponding
    * to the matrix of ranks.
    * If no auxiliary matrix of ranks were given in routing
    * parameters, this returns an empty map.
    * @return the map of auxiliary matrices of ranks.
    */
   public SortedMap<Double, double[][]> getRanksGTDelay() {
      return Collections.unmodifiableSortedMap (ranksGTDelayMap);
   }

   /**
    * Returns the currently used type-to-group matrix of ranks.
    * If {@link #initRanksTG(RouterParams)}
    * or {@link #setRanksTG(double[][])}
    * were never called, this method returns \texttt{null}.
    * Otherwise, it returns a $K\times I$ matrix of
    * ranks.
    * @return the currently used type-to-group matrix of ranks.
    */
   public double[][] getRanksTG () {
      return ranksTG == null ? null : ArrayUtil.deepClone (ranksTG);
   }

   /**
    * Sets the type-to-group matrix of ranks to \texttt{ranksTG}.
    * @param ranksTG the new matrix of ranks.
    */
   public void setRanksTG (double[][] ranksTG) {
      if (ranksTG == null)
         this.ranksTG = null;
      else {
         checkMatrixTG (ranksTG);
         this.ranksTG = ArrayUtil.deepClone (ranksTG);
      }
   }

   /**
    * Returns the currently used skill counts.
    * This method returns \texttt{null} if
    * {@link #initSkillCounts(RouterParams)}
    * or {@link #setSkillCounts(int[])}
    * were never called.
    * Otherwise, it returns an array whose
    * element \texttt{i} gives the skill count
    * for agent group \texttt{i}.
    * @return the currently used array of skill counts.
    */
   public int[] getSkillCounts () {
      return skillCounts == null ? null : skillCounts.clone();
   }

   /**
    * Returns the skill count for agent group
    * \texttt{i}, i.e., the number of call types
    * agents in this group can serve.
    * This method returns {@link Integer#MAX_VALUE}
    * if {@link #initSkillCounts(RouterParams)}
    * or {@link #setSkillCounts(int[])}
    * were never called.
    * @param i the index of the agent group.
    * @return the skill count.
    */
   public int getSkillCount (int i) {
      if (skillCounts == null)
         return Integer.MAX_VALUE;
      return skillCounts[i];
   }

   /**
    * Sets the currently used skill counts to
    * \texttt{skillCounts}.
    * @param skillCounts the new skill counts.
    */
   public void setSkillCounts (int[] skillCounts) {
      if (skillCounts == null)
         this.skillCounts = null;
      else {
         if (skillCounts.length != cc.getNumAgentGroups())
            throw new IllegalArgumentException
            ("The length of skillCounts must correspond to the number of agent groups");
         this.skillCounts = skillCounts.clone();
      }
   }

   /**
    * Returns the currently used type regions vector.
    * This method returns \texttt{null} if
    * {@link #initTypeRegions()} or
    * {@link #setTypeRegions(int[])}
    * were never called.
    * Otherwise, index \texttt{k} of the returned
    * array gives the region identifier for
    * calls of type \texttt{k}.
    * @return  the vector of type regions.
    */
   public int[] getTypeRegions () {
      return typeRegions == null ? null : typeRegions.clone();
   }

   /**
    * Sets the vector of type regions to
    * \texttt{typeRegions}.
    * @param typeRegions the new vector of type regions.
    */
   public void setTypeRegions (int[] typeRegions) {
      if (typeRegions == null)
         this.typeRegions = null;
      else {
         if (typeRegions.length != cc.getNumContactTypes())
            throw new IllegalArgumentException
            ("Invalid length of type regions");
         this.typeRegions = typeRegions.clone();
      }
   }
   
   /**
    * Returns the currently used group regions.
    * This method returns \texttt{null} if
    * {@link #initGroupRegions()}
    * or {@link #setGroupRegions(int[])}
    * were never called.
    * Otherwise, index \texttt{i} of the returned
    * array gives the region identifier for
    * agents in group \texttt{i}.
    * @return the currently used group regions.
    */
   public int[] getGroupRegions () {
      return groupRegions == null ? null : groupRegions.clone();
   }

   /**
    * Sets the currently used group regions to
    * \texttt{groupRegions}.
    * @param groupRegions the new group regions.
    */
   public void setGroupRegions (int[] groupRegions) {
      if (groupRegions == null)
         this.groupRegions = null;
      else {
         if (groupRegions.length != cc.getNumAgentGroups())
            throw new IllegalArgumentException
            ("The length of groupRegions must correspond to the number of agent groups");
         this.groupRegions = groupRegions.clone();
      }
   }

   /**
    * Returns the currently used type-to-group map.
    * If {@link #initTypeToGroupMap(RouterParams)}
    * or {@link #setTypeToGroupMap(int[][])}
    * were never called, this method returns
    * \texttt{null}.
    * Otherwise, it returns an array of $K$ arrays giving
    * an ordered list of agent groups for each call type. 
    * @return the currently used type-to-group map.
    */
   public int[][] getTypeToGroupMap () {
      return typeToGroupMap == null ? null : ArrayUtil.deepClone (typeToGroupMap);
   }

   /**
    * Sets the currently used type-to-group map
    * to \texttt{typeToGroupMap}.
    * @param typeToGroupMap the new type-to-group map.
    */
   public void setTypeToGroupMap (int[][] typeToGroupMap) {
      if (typeToGroupMap == null)
         this.typeToGroupMap = null;
      else {
         RoutingTableUtils.checkTypeToGroupMap (cc.getNumAgentGroups(), typeToGroupMap);
         this.typeToGroupMap = ArrayUtil.deepClone (typeToGroupMap);
      }
   }

   /**
    * Returns the currently used group-to-type matrix of weights.
    * If {@link #initWeightsGT(RouterParams)} or
    * {@link #setWeightsGT(double[][])} were
    * never called, this method returns \texttt{null}.
    * Otherwise, it returns a $I\times K$ matrix of
    * weights.
    * @return the currently used group-to-type matrix of weights.
    */
   public double[][] getWeightsGT () {
      return weightsGT == null ? null : ArrayUtil.deepClone (weightsGT);
   }

   /**
    * Sets the group-to-type matrix of weights to \texttt{weightsGT}.
    * @param weightsGT the new matrix of weights.
    */
   public void setWeightsGT (double[][] weightsGT) {
      if (weightsGT == null)
         this.weightsGT = null;
      else {
         checkMatrixGT (weightsGT);
         this.weightsGT = ArrayUtil.deepClone (weightsGT);
      }
   }

   /**
    * Returns the currently used type-to-group matrix of weights.
    * If {@link #initWeightsTG(RouterParams)}
    * or {@link #setWeightsTG(double[][])} were 
    * never called, this method returns \texttt{null}.
    * Otherwise, it returns a $K\times I$ matrix of
    * weights.
    * @return the currently used type-to-group matrix of weights.
    */
   public double[][] getWeightsTG () {
      return weightsTG == null ? null : ArrayUtil.deepClone (weightsTG);
   }

   /**
    * Sets the type-to-group matrix of weights to \texttt{weightsTG}.
    * @param weightsTG the new matrix of weights.
    */
   public void setWeightsTG (double[][] weightsTG) {
      if (weightsTG == null)
         this.weightsTG = null;
      else {
         checkMatrixTG (weightsTG);
         this.weightsTG = ArrayUtil.deepClone (weightsTG);
      }
   }

   /**
    * Returns the currently used group-to-type delays matrix.
    * If {@link #initDelays(RouterParams)} or
    * {@linkplain #setDelaysGT(double[][])}
    * were never called, this method returns
    * \texttt{null}.
    * Otherwise, it returns a $I\times K$ matrix of delays
    * expressed in the default time unit of the simulator.
    * @return the currently used group-to-type delays matrix.
    */
   public double[][] getDelaysGT () {
      return delaysGT == null ? null : ArrayUtil.deepClone (delaysGT);
   }

   /**
    * Sets the group-to-type delays matrix to \texttt{delaysGT}.
    * @param delaysGT the new delays matrix.
    */
   public void setDelaysGT (double[][] delaysGT) {
      if (delaysGT == null)
         this.delaysGT = null;
      else {
         checkMatrixGT (delaysGT);
         this.delaysGT = ArrayUtil.deepClone (delaysGT);
      }
   }
   
   public void init() {
      router.init ();
      if (stages != null && router instanceof OverflowAndPriorityRouter)
         for (CallCenterRoutingStageInfo[] stagesk : stages)
            for (CallCenterRoutingStageInfo stage : stagesk)
               for (RoutingCase c : stage.getCases ()) {
                  Condition cond = c.getCondition ();
                  if (cond != null) {
                     if (cond instanceof Initializable)
                        ((Initializable)cond).init ();
                     if (cond instanceof ToggleElement)
                        ((ToggleElement)cond).start ();
                  }
                  RankFunction fn = c.getAgentGroupRanksFunction ();
                  if (fn != null) {
                     if (fn instanceof Initializable)
                        ((Initializable)fn).init ();
                     if (fn instanceof ToggleElement)
                        ((ToggleElement)fn).start ();
                  }
                  RankFunction fn2 = c.getQueueRanksFunction ();
                  if (fn != fn2 && fn2 != null) {
                     if (fn2 instanceof Initializable)
                        ((Initializable)fn2).init ();
                     if (fn2 instanceof ToggleElement)
                        ((ToggleElement)fn2).start ();
                  }
               }
   }
}
