package umontreal.iro.lecuyer.contactcenters.msk.stat;

import java.util.EnumMap;
import java.util.Map;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import umontreal.iro.lecuyer.contactcenters.MatrixUtil;
import umontreal.iro.lecuyer.contactcenters.app.RowType;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.model.SegmentInfo;

/**
 * Constructs and caches matrices of observations derived from
 * the matrices obtained using a {@link CallCenterMeasureManager}.
 * The {@link CallCenterMeasureManager#getValues(MeasureType,boolean)} method
 * computes matrices whose rows usually correspond to call types.
 * However, the rows in matrices of statistical probes correspond
 * to segments of call types: there is one row per call type,
 * one row per user-defined group of call types, and a final row
 * regrouping all call types.
 * This class computes these aggregate rows, and stores the resulting
 * matrices in a cache for them to be retrieved faster at a later time.
 */
public class MatrixCache {
   private CallCenter cc;
   private CallCenterMeasureManager ccm;
   private final Map<MeasureType, DoubleMatrix2D> matrices = new EnumMap<MeasureType, DoubleMatrix2D> (
         MeasureType.class);
   private final Map<MeasureType, Map<RowType, DoubleMatrix2D>> derivedMatrices = new EnumMap<MeasureType, Map<RowType, DoubleMatrix2D>> (
         MeasureType.class);
   private boolean contactTypeAgentGroup;
   
   private boolean[] globalSegmentTypes;
   private boolean[] globalSegmentInTypes;
   private boolean[] globalSegmentOutTypes;
   private SegmentInfo[] typeSegments;
   private SegmentInfo[] inTypeSegments;
   private SegmentInfo[] outTypeSegments;
   private SegmentInfo[] groupSegments;

   /**
    * Constructs a new matrix cache from the call center
    * \texttt{cc}, and the measure manager \texttt{ccm}.
    */
   public MatrixCache (CallCenter cc, CallCenterMeasureManager ccm) {
      this.cc = cc;
      this.ccm = ccm;
      contactTypeAgentGroup = ccm.isContactTypeAgentGroup ();
      
      globalSegmentTypes = new boolean[cc.getNumContactTypes ()];
      globalSegmentInTypes = new boolean[cc.getNumInContactTypes ()];
      globalSegmentOutTypes = new boolean[cc.getNumOutContactTypes ()];
      for (int k = 0; k < globalSegmentTypes.length; k++) {
         if (cc.getCallFactory (k).isExcludedFromStatTotal ())
            continue;
         globalSegmentTypes[k] = true;
         if (k < globalSegmentInTypes.length)
            globalSegmentInTypes[k] = true;
         else
            globalSegmentOutTypes[k - globalSegmentInTypes.length] = true;
      }
      typeSegments = cc.getContactTypeSegments ();
      inTypeSegments = cc.getInContactTypeSegments ();
      outTypeSegments = cc.getOutContactTypeSegments ();
      groupSegments = cc.getAgentGroupSegments ();
   }

   /**
    * Clears the cached matrix.
    */
   public void clear () {
      matrices.clear ();
      derivedMatrices.clear ();
   }

   /**
    * Computes and returns the base matrix corresponding
    * to \texttt{mt}.
    * If the matrix is already cached, this method returned
    * the cached matrix without any further computation.
    * Otherwise, it uses {@link CallCenterMeasureManager#getValues(MeasureType,boolean)}
    * to compute the matrix, adds rows
    * for segments regrouping several call type or
    * agent groups (depending on measure type), saves a copy
    * of the resulting matrix in the cache, and returns the matrix.
    * @param mt the type of measure for which a matrix is desired. 
    * @return the base matrix.
    */
   private DoubleMatrix2D getBaseMatrix (MeasureType mt) {
      DoubleMatrix2D mat = matrices.get (mt);
      if (mat == null) {
         mat = ccm.getValues (mt, true);
         
         final RowType rt = mt.getRowType (contactTypeAgentGroup);
         final int nr = rt.count (cc);
         if (mat.rows () < nr) {
            final DoubleDoubleFunction func = mt.getAggregationFunction ();
            switch (rt) {
            case CONTACTTYPE:
               mat = SegmentInfo.addRowSegments (mat, func, globalSegmentTypes,
                     typeSegments);
               break;
            case CONTACTTYPEAGENTGROUP:
               mat = SegmentInfo.addRowSegments (mat,
                     cc.getNumAgentGroups (),
                     func,
                     globalSegmentTypes, null,
                     typeSegments, groupSegments);
               break;
            case INBOUNDTYPE:
               mat = SegmentInfo.addRowSegments (mat, func, globalSegmentInTypes,
                     inTypeSegments);
               break;
            case INBOUNDTYPEAGENTGROUP:
               mat = SegmentInfo.addRowSegments (mat,
                     cc.getNumAgentGroups (),
                     func,
                     globalSegmentInTypes, null,
                     inTypeSegments, groupSegments);
               break;
            case OUTBOUNDTYPE:
               mat = SegmentInfo.addRowSegments (mat, func, globalSegmentOutTypes,
                     outTypeSegments);
               break;
            case OUTBOUNDTYPEAGENTGROUP:
               mat = SegmentInfo.addRowSegments (mat,
                     cc.getNumAgentGroups (),
                     func,
                     globalSegmentOutTypes, null,
                     outTypeSegments, groupSegments);
               break;
            case AGENTGROUP:
               mat = SegmentInfo.addRowSegments (mat,
                     func, null,
                     groupSegments);
               break;
            }
         }
         assert mat.rows () == nr : "Incorrect number of rows for " + mt + " and row type " + rt;
         
         matrices.put (mt, mat);
      }
      return mat;
   }

   private DoubleMatrix2D extendMatrixAgentGroups (DoubleMatrix2D mat) {
      final int I = cc.getNumAgentGroups ();
      final DoubleMatrix2D mat2;
      if (I > 1) {
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         mat2 = new DenseDoubleMatrix2D (mat.rows () * Ip, mat.columns ());
         for (int r = 0; r < mat.rows (); r++)
            for (int r2 = 0; r2 < Ip; r2++)
               mat2.viewRow (r * Ip + r2).assign (mat.viewRow (r));
      }
      else
         mat2 = mat;
      return mat2;
   }

   private DoubleMatrix2D regroupMatrixAgentGroups (DoubleMatrix2D mat) {
      final int I = cc.getNumAgentGroups ();
      final DoubleMatrix2D mat2;
      if (I > 1) {
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         mat2 = new DenseDoubleMatrix2D (mat.rows () / Ip, mat.columns ());
         for (int r = 0; r < mat2.rows (); r++)
            mat2.viewRow (r).assign (mat.viewRow ((r + 1) * Ip - 1));
      }
      else
         mat2 = mat;
      return mat2;
   }
   
   private DoubleMatrix2D getInbound (boolean contactTypeAgentGroup1, DoubleMatrix2D base,
         MeasureType mt) {
      final int Ki = cc.getNumInContactTypes ();
//      final int Ko = sim.getModel ().getNumOutContactTypes ();
      final int I = cc.getNumAgentGroups ();
//      if (Ko == 0)
//         return base;
      final DoubleDoubleFunction func = mt.getAggregationFunction ();
      if (!contactTypeAgentGroup1 || I == 1) {
         //return ContactCenter.addSumRow (base.viewPart (0, 0, Ki, base
         //      .columns ()));
         if (cc.getNumOutContactTypes () == 0 && cc.getNumInContactTypeSegments () == 0)
            return base.viewPart (0, 0, Ki > 1 ? Ki + 1 : Ki, base.columns ());
         return SegmentInfo.addRowSegments
         (base.viewPart (0, 0, Ki, base
               .columns ()), func, globalSegmentInTypes, inTypeSegments);
      }
      else {
         final int Kip = cc.getNumInContactTypesWithSegments ();
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         final DoubleMatrix2D ret = new DenseDoubleMatrix2D (Kip * Ip, base.columns ());
         ret.viewPart (0, 0, Ki * Ip, ret.columns ()).assign (
               base.viewPart (0, 0, Ki * Ip, base.columns ()));
         if (Ki > 1) {
            final int nseg = cc.getNumInContactTypeSegments ();
            for (int i = 0; i < Ip; i++)
               for (int k = 0; k < Ki; k++)
                  for (int s = 0; s <= nseg; s++) {
                     if (s < nseg && !cc.getInContactTypeSegment (s).containsValue (k))
                        continue;
                     if (s == nseg && cc.getCallFactory (k).isDisableCallSource())
                        continue;

                     for (int c = 0; c < base.columns (); c++)
                        ret.setQuick ((Ki + s) * Ip + i, c, ret.getQuick (
                              (Ki + s) * Ip + i, c)
                              + ret.getQuick (k * Ip + i, c));
                  }
         }
         return ret;
      }
   }

   private DoubleMatrix2D getOutbound (boolean contactTypeAgentGroup1, DoubleMatrix2D base, MeasureType mt) {
      final int Ki = cc.getNumInContactTypes ();
      final int Ko = cc.getNumOutContactTypes ();
      final int I = cc.getNumAgentGroups ();
//      if (Ki == 0)
//         return base;
      final DoubleDoubleFunction func = mt.getAggregationFunction (); 
      if (!contactTypeAgentGroup1 || I == 1) {
         if (cc.getNumInContactTypes () == 0 && cc.getNumOutContactTypeSegments () == 0)
            return base.viewPart (0, 0, Ko > 1 ? Ko + 1 : Ko, base.columns ());
         return SegmentInfo.addRowSegments
         (base.viewPart (Ki, 0, Ko, base
                     .columns ()), func, globalSegmentOutTypes, outTypeSegments);
//         return ContactCenter.addSumRow (base.viewPart (Ki, 0, Ko, base
//               .columns ()));
      }
      else {
         final int Kop = cc.getNumOutContactTypesWithSegments ();
         final int Ip = cc.getNumAgentGroupsWithSegments ();
         final DoubleMatrix2D ret = new DenseDoubleMatrix2D (Kop * Ip, base.columns ());
         ret.viewPart (0, 0, Ko * Ip, ret.columns ()).assign (
               base.viewPart (Ki, 0, Ko * Ip, base.columns ()));
         if (Ko > 1) {
            final int nseg = cc.getNumOutContactTypeSegments ();
            for (int i = 0; i < Ip; i++)
               for (int k = 0; k < Ko; k++)
                  for (int s = 0; s <= nseg; s++) {
                     if (s < nseg && !cc.getOutContactTypeSegment (s).containsValue (k))
                        continue;
                     if (s == nseg && cc.getCallFactory(k + cc.getNumInContactTypes()).isDisableCallSource())
                        continue;
                     
                     for (int c = 0; c < base.columns (); c++)
                        ret.setQuick ((Ko + s) * Ip + i, c, ret.getQuick (
                              (Ko + s) * Ip + i, c)
                              + ret.getQuick (k * Ip + i, c));
                  }
         }
         return ret;
      }
   }

   private DoubleMatrix2D toAWT (DoubleMatrix2D mat) {
      final int M = cc.getNumMatricesOfAWT ();
      if (M == 1)
         return mat;
      return MatrixUtil.repMat (mat, M, 1);
   }

   /**
    * Returns a matrix of observations for type of measure
    * \texttt{mt} adapted to row type \texttt{targetRowType}.
    * Usually, \texttt{targetRowType} corresponds to the
    * value returned by \texttt{mt.getRowType(false)}.
    * But in some situations where
    * \texttt{mt.getRowType(false)} corresponds to
    * {@link RowType#CONTACTTYPE}, the row types might differ.
    * 
    * In particular, \texttt{targetRowType} is set to
    * {@link RowType#INBOUNDTYPE} if we want to keep only the rows
    * corresponding to segments of inbound call types, and
    * {@link RowType#OUTBOUNDTYPE} to keep rows for segment of outbound
    * types only. For example, if
    * \texttt{mt} is {@link MeasureType#NUMARRIVALS}, the base
    * matrix gives the number of arrivals for each segment of call type.
    * By setting the target row type to {@link RowType#INBOUNDTYPE},
    * we obtain the number of arrivals for segments of inbound types only. 
    * 
    * If the target row type is
    * {@link RowType#INBOUNDTYPEAWT},
    * the set of $\Ki'$ rows corresponding to segments of inbound types is duplicated
    * $m$ times, where $m$ is the number of matrices of acceptable
    * waiting times.
    * If \texttt{mt} is {@link MeasureType#NUMSERVED}, the resulting
    * base matrix has one row per segment of call type.
    * By using {@link RowType#INBOUNDTYPEAWT} as target row type,
    * the obtained matrix of numbers has $m\Ki'$ segments of inbound
    * call types, and is compatible with the matrix
    * returned if \texttt{mt} is {@link MeasureType#NUMSERVEDBEFOREAWT}. 
    * 
    * The method first obtains the base matrix
    * using {@link #getBaseMatrix(MeasureType)}.
    * If the row type associated with \texttt{mt} does not correspond
    * to contact types, the method does nothing else.
    * It then adds or subtracts rows for the returned matrix
    * to have the number of rows corresponding to \texttt{targetRowType}.
    * 
    * @param mt the type of measure matrix for which a matrix of
    * observations is needed.
    * @param targetRowType the target row type.
    * @return the generated matrix of observations.
    */
   public DoubleMatrix2D getMatrix (MeasureType mt, RowType targetRowType) {
      if (!ccm.hasMeasureMatrix (mt))
         return null;
      if (mt.getRowType (contactTypeAgentGroup) == targetRowType)
         return getBaseMatrix (mt);
      Map<RowType, DoubleMatrix2D> der = derivedMatrices.get (mt);
      if (der != null) {
         final DoubleMatrix2D mat = der.get (targetRowType);
         if (mat != null)
            return mat;
      }
      else {
         der = new EnumMap<RowType, DoubleMatrix2D> (RowType.class);
         derivedMatrices.put (mt, der);
      }

      DoubleMatrix2D base = getBaseMatrix (mt);
      RowType baseRowType = mt.getRowType (contactTypeAgentGroup);
      DoubleMatrix2D newBase;
      RowType newRowType;
      switch (mt.getRowType (contactTypeAgentGroup)) {
         case CONTACTTYPE:
         case CONTACTTYPEAGENTGROUP:
            switch (targetRowType) {
               case INBOUNDTYPE:
               case INBOUNDTYPEAWT:
               case INBOUNDTYPEAGENTGROUP:
               case INBOUNDTYPEAWTAGENTGROUP:
                  newRowType = baseRowType.toInboundType ();
                  newBase = der.get (newRowType);
                  if (newBase == null) {
                     newBase = getInbound (baseRowType.isContactTypeAgentGroup (), base, mt);
                     der.put (newRowType, newBase);
                  }
                  base = newBase;
                  baseRowType = newRowType;
                  break;
               case OUTBOUNDTYPE:
               case OUTBOUNDTYPEAGENTGROUP:
                  newRowType = baseRowType.toOutboundType ();
                  newBase = der.get (newRowType);
                  if (newBase == null) {
                     newBase = getOutbound (baseRowType.isContactTypeAgentGroup (), base, mt);
                     der.put (newRowType, newBase);
                  }
                  base = newBase;
                  baseRowType = newRowType;
                  break;
            }
      }
      
      if (baseRowType.isContactType () &&
            targetRowType.isContactTypeAgentGroup ()) {
         newRowType = baseRowType.toContactTypeAgentGroup ();
         newBase = der.get (newRowType);
         if (newBase == null) {
            newBase = extendMatrixAgentGroups (base);
            der.put (newRowType, newBase);
         }
         base = newBase;
         baseRowType = newRowType;
      }
      else if (baseRowType.isContactTypeAgentGroup () &&
            targetRowType.isContactType ()) {
         newRowType = baseRowType.toContactType ();
         newBase = der.get (newRowType);
         if (newBase == null) {
            newBase = regroupMatrixAgentGroups (base);
            der.put (newRowType, newBase);
         }
         base = newBase;
         baseRowType = newRowType;
      }
      
      if (targetRowType == RowType.INBOUNDTYPEAWT ||
            targetRowType == RowType.INBOUNDTYPEAWTAGENTGROUP)
         if (baseRowType != RowType.INBOUNDTYPEAWT &&
               baseRowType != RowType.INBOUNDTYPEAWTAGENTGROUP) {
            newRowType = baseRowType.toInboundTypeAWT ();
            newBase = der.get (newRowType);
            if (newBase == null) {
               newBase = toAWT (base);
               der.put (newRowType, newBase);
            }
            base = newBase;
            baseRowType = newRowType;
         }
      if (baseRowType != targetRowType)
         throw new IllegalArgumentException
         ("Row type " + baseRowType.name () + " incompatible with row type " + targetRowType.name ());
      return base;
   }
}
