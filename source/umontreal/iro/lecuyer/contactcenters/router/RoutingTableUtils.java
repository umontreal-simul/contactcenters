package umontreal.iro.lecuyer.contactcenters.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import umontreal.iro.lecuyer.util.ArrayUtil;
import cern.colt.Sorting;
import cern.colt.function.IntComparator;

/**
 * Provides some utility methods to manage
 * routing tables represented using 2D arrays.
 * Three types of routing tables
 * are supported: type-to-group and group-to-type maps,
 * incidence matrices and matrices of ranks.
 * This class
 * provides facilities to check the consistency
 * of such routing tables, to generate one table
 * from the other, and to format them as strings.
 * However, converting from one routing table to another
 * may destroy some information or force the conversion
 * algorithm to infer information, which can lead to bad routing policies.
 */
public final class RoutingTableUtils {
   /*
    * This is used to separate the routing table management
    * facilities from the Router class.
    * Since Router is an abstract class, this
    * could be integrated to it.
    */

   private RoutingTableUtils() {}

   /**
    * Applies a consistency check for the type-to-group
    * map \texttt{typeToGroupMap} supporting
    * $I=$~\texttt{numGroups}
    * agent groups.
    * This checks that every positive element
    * of the given matrix corresponds to an agent group
    * index and no group index appears more than once
    * in an ordered list.
    * If an inconsistency is detected, this
    * throws an {@link IllegalArgumentException}
    * describing the problem.
    @param numGroups the number of agent groups $I$.
    @param typeToGroupMap the type-to-group map being checked.
    @exception NullPointerException if \texttt{typeToGroupMap} or
    one of its elements are \texttt{null}.
    @exception IllegalArgumentException if the matrix is incorrect.
    */
   public static void checkTypeToGroupMap (int numGroups,
                                           int[][] typeToGroupMap) {
      for (int k = 0; k < typeToGroupMap.length; k++) {
         final int[] rankList = typeToGroupMap[k];
         if (rankList == null)
            // This exception would be thrown automatically by
            // the nest statement, but there would be no
            // describing message for the user.
            throw new NullPointerException
               ("Row " + k + " of the type-to-group map is null");
         for (int idx = 0; idx < rankList.length; idx++) {
            final int i = rankList[idx];
            if (i < 0)
               continue;
            if (i >= numGroups)
               throw new IllegalArgumentException
                  ("Invalid element [" + k + "][" + idx + "] = " + i +
                   ", does not correspond to a valid agent group index");
            for (int sidx = idx + 1; sidx < rankList.length; sidx++)
               if (rankList[sidx] == i)
                  throw new IllegalArgumentException
                     ("Agent group index " + i + " appears twice in the rank list " +
                      k + ", at indices " +
                      idx + " and " + sidx);
         }
      }
   }

   /**
    * Applies a consistency check for the group-to-type
    * map \texttt{groupToTypeMap} supporting $K=$~\texttt{numTypes}
    * contact types.
    * This checks that every positive element
    * of the given matrix corresponds to a contact type
    * index and no type index appears more than once
    * in an ordered list.
    * If an inconsistency is detected, this
    * throws an {@link IllegalArgumentException}
    * describing the problem.
    @param numTypes the number of contact types $K$.
    @param groupToTypeMap the group-to-type map being checked.
    @exception NullPointerException if \texttt{groupToTypeMap} or
    one of its elements are \texttt{null}.
    @exception IllegalArgumentException if the matrix is incorrect.
    */
   public static void checkGroupToTypeMap (int numTypes,
                                           int[][] groupToTypeMap) {
      for (int i = 0; i < groupToTypeMap.length; i++) {
         final int[] rankList = groupToTypeMap[i];
         if (rankList == null)
            // This exception would be thrown automatically by
            // the nest statement, but there would be no
            // describing message for the user.
            throw new NullPointerException
               ("Row " + i + " of the group-to-type map is null");
         for (int idx = 0; idx < rankList.length; idx++) {
            final int k = rankList[idx];
            if (k < 0)
               continue;
            if (k >= numTypes)
               throw new IllegalArgumentException
                  ("Invalid element [" + i + "][" + idx + "] = " + k +
                   ", does not correspond to a valid contact type index");
            for (int sidx = idx + 1; sidx < rankList.length; sidx++)
               if (rankList[sidx] == k)
                  throw new IllegalArgumentException
                     ("Contact type index " + k + " appears twice in the rank list " +
                      i + ", at indices " + idx + " and " + sidx);
         }
      }
   }

   /**
    * Checks the consistency of the routing tables
    * \texttt{typeToGroupMap} and \texttt{groupToTypeMap}.
    * It is assumed that the matrices are themselves
    * consistent routing tables, i.e.,
    * {@link #checkTypeToGroupMap(int,int[][]) check\-Type\-To\-Group\-Map}
    * \texttt{(groupToTypeMap.length, typeToGroupMap)} and
    * {@link #checkGroupToTypeMap(int,int[][]) check\-Group\-To\-Type\-Map}
    * \texttt{(typeToGroupMap.length, groupToTypeMap)} do
    * not throw exceptions.
    * This methods checks that an agent group index~$i$ appears
    * in the ordered list for contact type~$k$ if and only if the contact
    * type~$k$ appears in the ordered list for agent group~$i$.
    * If this consistency criterion is violated,
    * an {@link IllegalArgumentException} is thrown.
    @param typeToGroupMap the type-to-group map.
    @param groupToTypeMap the group-to-type map.
    @exception IllegalArgumentException if the
    consistency check fails.
    @see #checkTypeToGroupMap(int,int[][])
    @see #checkGroupToTypeMap(int,int[][])
    */
   public static void checkConsistency (int[][] typeToGroupMap,
                                        int[][] groupToTypeMap) {
      for (int k = 0; k < typeToGroupMap.length; k++)
         for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
            final int i = typeToGroupMap[k][idx];
            if (i < 0)
               continue;
            boolean found = false;
            for (int sidx = 0; sidx < groupToTypeMap[i].length && !found; sidx++)
               if (groupToTypeMap[i][sidx] == k)
                  found = true;
            if (!found)
               throw new IllegalArgumentException
                  ("The rank list for contact type " + k + " refers to agent group " + i +
                   " whose rank list does not contain " + k);
         }

      for (int i = 0; i < groupToTypeMap.length; i++)
         for (int idx = 0; idx < groupToTypeMap[i].length; idx++) {
            final int k = groupToTypeMap[i][idx];
            if (k < 0)
               continue;
            boolean found = false;
            for (int sidx = 0; sidx < typeToGroupMap[k].length && !found; sidx++)
               if (typeToGroupMap[k][sidx] == i)
                  found = true;
            if (!found)
               throw new IllegalArgumentException
                  ("The rank list for agent group " + i + " refers to contact type " + k +
                   " whose rank list does not contain " + i);
         }
   }

   /**
    * Generates the type-to-group map from the group-to-type
    * map \texttt{groupToTypeMap}.  For each contact type~$k$,
    * this method constructs an ordered list containing
    * all agent groups referring to it in the group-to-type map,
    * sorted in increasing order of group identifier.
    * It is assumed that the
    * \texttt{groupToTypeMap} matrix is consistent,
    * i.e., {@link #checkGroupToTypeMap(int,int[][])} does not throw
    * an exception when called with it.
    @param numTypes the number of contact types $K$.
    @param groupToTypeMap the group-to-type map being processed.
    @return the generated type-to-group map.
    @see #checkGroupToTypeMap(int,int[][])
   */
   public static int[][] getTypeToGroupMap (int numTypes, int[][] groupToTypeMap) {
      final int[][] res = new int[numTypes][groupToTypeMap.length];
      for (final int[] element : res)
         for (int ridx = 0; ridx < element.length; ridx++)
            element[ridx] = -1;
      for (int i = 0; i < groupToTypeMap.length; i++)
         for (int idx = 0; idx < groupToTypeMap[i].length; idx++) {
            final int k = groupToTypeMap[i][idx];
            if (k < 0)
               continue;
            boolean found = false;
            int ridx;
            for (ridx = 0; ridx < res[k].length && res[k][ridx] >= 0 && !found; ridx++)
               if (res[k][ridx] == i)
                  found = true;
            if (!found)
               res[k][ridx] = i;
         }
      return res;
   }

   /**
    * Constructs and returns a new type-to-group map from the
    * incidence matrix \texttt{m}.
    * For each column~$k$ of the rectangular matrix $m$, the method creates a row
    * in the type-to-group map
    * with a column containing value~$i$ for each \texttt{true} $m(i, k)$ value.
    * This gives lists of agent groups
    * sorted in increasing order of group identifier.
    @param m the incidence matrix.
    @return the type-to-group map.
    @see ArrayUtil#checkRectangularMatrix(Object)
    */
   public static int[][] getTypeToGroupMap (boolean[][] m) {
      final int[][] tg = new int[m[0].length][];
      for (int k = 0; k < m[0].length; k++) {
         int count = 0;
         for (final boolean[] element : m)
            if (element[k])
               count++;
         tg[k] = new int[count];
         for (int i = 0, idx = 0; i < m.length; i++)
            if (m[i][k])
               tg[k][idx++] = i;
      }
      return tg;
   }

   /**
    * Generates a type-to-group map from the agent selection matrix of ranks
    * \texttt{ranksTG}.
    * Assuming that the given matrix is rectangular, the method
    *  first uses a scheme similar to
    * {@link #getTypeToGroupMap(boolean[][])} to get
    * a list of agent groups sorted in increasing order of
    * group identifier for each
    * contact type.  Each row of the resulting type-to-group map
    * is then sorted in rank-increasing order, i.e.,
    * an agent group $i_1$ goes before $i_2$ if $\rTG(k, i_1)<\rTG(k, i_2)$.
    * If $\rTG(k, i_1)=\rTG(k, i_2)$,
    * $i_1$ goes before $i_2$ in the ordered list
    * for contact type~$k$ if $i_1<i_2$.
    @param ranksTG the matrix of ranks being transformed.
    @return the generated type-to-group map.
    */
   public static int[][] getTypeToGroupMap (double[][] ranksTG) {
      final int[][] tg = new int[ranksTG.length][];
      for (int k = 0; k < tg.length; k++) {
         int ng = 0;
         for (int i = 0; i < ranksTG[k].length; i++)
            if (!Double.isInfinite (ranksTG[k][i]))
               ++ng;
         tg[k] = new int[ng];
         for (int i = 0, idx = 0; i < ranksTG[k].length; i++)
            if (!Double.isInfinite (ranksTG[k][i]))
               tg[k][idx++] = i;
         final IntComparator cmp = new GroupSorter (ranksTG, k);
         Sorting.quickSort (tg[k], 0, tg[k].length, cmp);
      }
      return tg;
   }

   /**
    * This method is similar to {@link #getTypeToGroupMap(double[][])}
    * with a sorting algorithm adapted for the local-specialist
    * policy.
    * Except from the agent selection matrix of ranks, the method needs arrays
    * associating a region identifier to each contact type and agent group.
    * For contact type~$k$,
    * an agent group~$i_1$ goes before an agent group~$i_2$
    * if the location of $i_1$ is the same as the originating
    * region of contacts of type~$k$, but $i_2$'s location is
    * different from $i_1$'s.  In other words, \texttt{groupRegions[i1] == typeRegions[k]}
    * and \texttt{groupRegions[i2] != typeRegions[k]} if $i_1$ is before $i_2$.
    * Any pair $(i_1, i_2)$ not meeting this extra condition is
    * sorted using the same algorithm as in {@link #getTypeToGroupMap(double[][])}.
    @param ranksTG the matrix of ranks.
    @param typeRegions the region identifier of each contact type.
    @param groupRegions the region identifier of each agent group.
    @return the constructed type-to-group map.
    */
   public static int[][] getTypeToGroupMap (double[][] ranksTG,
                                            int[] typeRegions, int[] groupRegions) {
      if (typeRegions == null || groupRegions == null)
         return RoutingTableUtils.getTypeToGroupMap (ranksTG);
      if (typeRegions.length != ranksTG.length)
         throw new IllegalArgumentException
            ("A region identifier is needed for each contact type");
      if (groupRegions.length != ranksTG[0].length)
         throw new IllegalArgumentException
            ("A region identifier is needed for each agent group");
      final int[][] tg = new int[ranksTG.length][];
      for (int k = 0; k < tg.length; k++) {
         int ng = 0;
         for (int i = 0; i < ranksTG[k].length; i++)
            if (!Double.isInfinite (ranksTG[k][i]))
               ++ng;
         tg[k] = new int[ng];
         for (int i = 0, idx = 0; i < ranksTG[k].length; i++)
            if (!Double.isInfinite (ranksTG[k][i]))
               tg[k][idx++] = i;
         Sorting.quickSort (tg[k], 0, tg[k].length, new GroupSorter
                            (ranksTG, k, typeRegions[k], groupRegions));
      }
      return tg;
   }

   /**
    * Generates the group-to-type map from the type-to-group
    * map \texttt{typeToGroupMap}.  For each agent group~$i$,
    * this method constructs a list containing
    * all contact types referring to it in the type-to-group map,
    * sorted in increasing order of type identifier.  It is assumed that the
    * \texttt{typeToGroupMap} matrix is consistent,
    * i.e., {@link #checkTypeToGroupMap(int,int[][])} does not throw
    * an exception when called with it.
    @param numGroups the number of agent groups $I$.
    @param typeToGroupMap the type-to-group map being processed.
    @return the generated group-to-type map.
    @see #checkTypeToGroupMap(int, int[][])
    */
   public static int[][] getGroupToTypeMap (int numGroups, int[][] typeToGroupMap) {
      final int[][] res = new int[numGroups][typeToGroupMap.length];
      for (final int[] element : res)
         for (int ridx = 0; ridx < element.length; ridx++)
            element[ridx] = -1;
      for (int k = 0; k < typeToGroupMap.length; k++)
         for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
            final int i = typeToGroupMap[k][idx];
            if (i < 0)
               continue;
            boolean found = false;
            int ridx;
            for (ridx = 0; ridx < res[i].length && res[i][ridx] >= 0 && !found; ridx++)
               if (res[i][ridx] == k)
                  found = true;
            if (!found)
               res[i][ridx] = k;
         }
      return res;
   }

   /**
    * Constructs and returns a new group-to-type map from the
    * incidence matrix \texttt{m}.
    * For each row~$i$ of \texttt{m}, the method
    * creates a row in the group-to-type map
    * with a column having a value $k$ for each \texttt{true} \texttt{m(i, k)}
    * value.  This gives lists of contact types
    * sorted in increasing order of type identifier.
    @param m the incidence matrix.
    @return the group-to-type map.
    */
   public static int[][] getGroupToTypeMap (boolean[][] m) {
      final int[][] gt = new int[m.length][];
      for (int i = 0; i < m.length; i++) {
         int count = 0;
         for (int k = 0; k < m[i].length; k++)
            if (m[i][k])
               count++;
         gt[i] = new int[count];
         for (int k = 0, idx = 0; k < m[i].length; k++)
            if (m[i][k])
               gt[i][idx++] = k;
      }
      return gt;
   }

   /**
    * Generates a group-to-type map from the
    * contact selection matrix of ranks \texttt{ranksGT}.
    * The method first uses a scheme similar to
    * {@link #getGroupToTypeMap(boolean[][])} to get
    * a list of contact types sorted in increasing order of
    * type identifier for each
    * agent group.  Each row of the resulting group-to-type map
    * is then sorted in rank-increasing order, i.e.,
    * a contact type $k_1$ goes before $k_2$ if $\rGT(i, k_1)<\rGT(i, k_2)$.
    * If $\rGT(i, k_1)=\rGT(i, k_2)$,
    * $k_1$ goes before $k_2$ in the list if $k_1<k_2$.
    @param ranksGT the matrix of ranks.
    @return the new group-to-type map.
    */
   public static int[][] getGroupToTypeMap (double[][] ranksGT) {
      final int[][] gt = new int[ranksGT.length][];
      for (int i = 0; i < gt.length; i++) {
         int nt = 0;
         for (int k = 0; k < ranksGT[i].length; k++)
            if (!Double.isInfinite (ranksGT[i][k]))
               ++nt;
         gt[i] = new int[nt];
         for (int k = 0, idx = 0; k < ranksGT[i].length; k++)
            if (!Double.isInfinite (ranksGT[i][k]))
               gt[i][idx++] = k;
         Sorting.quickSort (gt[i], 0, gt[i].length, new TypeSorter (ranksGT, i));
      }
      return gt;
   }

   /**
    * This method is similar to {@link #getGroupToTypeMap(double[][])}
    * with a sorting algorithm adapted for the local-specialist
    * policy.
    * Except from the contact selection matrix of ranks, the method needs arrays
    * associating a region identifier to each contact type and agent group.
    * For each agent group~$i$,
    * a contact type~$k_1$ goes before a contact type~$k_2$
    * if the originating region of $k_1$ is the same as the location
    * of agent group~$i$, but $k_2$'s originating region is
    * different from $k_1$'s.  In other words, \texttt{typeRegions[k1] == groupRegions[i]}
    * and \texttt{typeRegions[k2] != groupRegions[i]} if $k_1$ is before $k_2$.
    * Any pair $(k_1, k_2)$ not meeting this extra condition is
    * sorted using the same algorithm as in {@link #getGroupToTypeMap(double[][])}.
    @param ranksGT the matrix of ranks.
    @param typeRegions the region identifier of each contact type.
    @param groupRegions the region identifier of each agent group.
    @return the constructed group-to-type map.
    */
   public static int[][] getGroupToTypeMap (double[][] ranksGT,
                                            int[] typeRegions, int[] groupRegions) {
      if (typeRegions == null || groupRegions == null)
         return RoutingTableUtils.getGroupToTypeMap (ranksGT);
      if (typeRegions.length != ranksGT[0].length)
         throw new IllegalArgumentException
            ("A region identifier is needed for each contact type");
      if (groupRegions.length != ranksGT.length)
         throw new IllegalArgumentException
            ("A region identifier is needed for each agent group");
      final int[][] gt = new int[ranksGT.length][];
      for (int i = 0; i < gt.length; i++) {
         int nt = 0;
         for (int k = 0; k < ranksGT[i].length; k++)
            if (!Double.isInfinite (ranksGT[i][k]))
               ++nt;
         gt[i] = new int[nt];
         for (int k = 0, idx = 0; k < ranksGT[i].length; k++)
            if (!Double.isInfinite (ranksGT[i][k]))
               gt[i][idx++] = k;
         Sorting.quickSort (gt[i], 0, gt[i].length, new TypeSorter
                            (ranksGT, i, groupRegions[i], typeRegions));
      }
      return gt;
   }

   /**
    * Constructs and returns the incidence matrix from
    * the \texttt{typeToGroupMap}
    * with \texttt{numGroups} agent groups.
    * The returned incidence matrix has one row for each
    * agent group and one column for each contact type.
    * Element $(i,k)$ of the matrix is \texttt{true} if and only
    * agent group~$i$ is included in the list of contact type~$k$, i.e.,
    * $i_{k, j}=k$ for some $j$.
    * In the incidence matrix, all the ranking induced by
    * the type-to-group map is lost.
    * It is assumed that the type-to-group map
    * is consistent as checked by {@link #checkTypeToGroupMap(int,int[][])}.
    @param numGroups the number of agent groups.
    @param typeToGroupMap the type-to-group map.
    @return the incidence matrix.
    */
   public static boolean[][] getIncidenceFromTG (int numGroups,
                                                      int[][] typeToGroupMap) {
      final boolean[][] m = new boolean[numGroups][typeToGroupMap.length];
      for (int k = 0; k < typeToGroupMap.length; k++)
         for (int idx = 0; idx < typeToGroupMap[k].length; idx++)
            if (typeToGroupMap[k][idx] >= 0)
               m[typeToGroupMap[k][idx]][k] = true;
      return m;
   }

   /**
    * Constructs and returns the incidence matrix from
    * the \texttt{groupToTypeMap}
    * with \texttt{numTypes} contact types.
    * The returned incidence matrix has one row for each
    * agent group and one column for each contact type.
    * Element $(i,k)$ of the matrix is \texttt{true} if and only
    * if the contact type~$k$ is included in the list of agent
    * group~$i$, i.e., $k_{i, j}=k$ for some $j$.
    * In the incidence matrix, all the ranking induced by
    * the group-to-type map is lost.
    * It is assumed that the group-to-type map
    * is consistent as checked by {@link #checkGroupToTypeMap(int,int[][])}.
    @param numTypes the number of contact types.
    @param groupToTypeMap the group-to-type map.
    @return the incidence matrix.
    */
   public static boolean[][] getIncidenceFromGT (int numTypes,
                                                      int[][] groupToTypeMap) {
      final boolean[][] m = new boolean[groupToTypeMap.length][numTypes];
      for (int i = 0; i < groupToTypeMap.length; i++)
         for (int idx = 0; idx < groupToTypeMap[i].length; idx++)
            if (groupToTypeMap[i][idx] >= 0)
               m[i][groupToTypeMap[i][idx]] = true;
      return m;
   }

   /**
    * Constructs the agent selection matrix of ranks from the \texttt{typeToGroupMap}
    * with \texttt{numGroups} agent groups.  For each non-negative
    * $i_{k,j}=$~\texttt{typeToGroupMap[}$k$\texttt{][}$j$\texttt{]}, the rank
    * $\rTG(k, i_{k, j})$ of contact type~$k$
    * for agent group~$i_{k, j}$ is set to $j$.
    * If $i$
    * does not appear in the list of $k$, $\rTG(k, i)=\infty$.
    @param numGroups the number of agent groups.
    @param typeToGroupMap the type-to-group map.
    @return the matrix of ranks.
    */
   public static double[][] getRanksFromTG (int numGroups, int[][] typeToGroupMap) {
      final double[][] ranksTG = new double[typeToGroupMap.length][numGroups];
      for (int k = 0; k < typeToGroupMap.length; k++) {
         for (int i = 0; i < ranksTG[k].length; i++)
            ranksTG[k][i] = Double.POSITIVE_INFINITY;
         for (int idx = 0; idx < typeToGroupMap[k].length; idx++) {
            final int i = typeToGroupMap[k][idx];
            if (i < 0)
               continue;
            ranksTG[k][i] = idx;
         }
      }
      return ranksTG;
   }

   /**
    * Constructs the contact selection matrix of ranks from the \texttt{groupToTypeMap}
    * with \texttt{numTypes} contact types.  For each non-negative
    * $k_{i, j}=$~\texttt{groupToTypeMap[}$i$\texttt{][}$j$\texttt{]}, the rank
    * $\rGT(i, k_{i, j})$ of contact type~$k_{i, j}$
    * for agent group~$i$ is set to $j$.  If $k$
    * does not appear in the list of $i$, $\rGT(k, i)=\infty$.
    @param numTypes the number of contact types.
    @param groupToTypeMap the group-to-type map.
    @return the matrix of ranks.
    */
   public static double[][] getRanksFromGT (int numTypes, int[][] groupToTypeMap) {
      final double[][] ranksGT = new double[groupToTypeMap.length][numTypes];
      for (int i = 0; i < groupToTypeMap.length; i++) {
         for (int k = 0; k < ranksGT[i].length; k++)
            ranksGT[i][k] = Double.POSITIVE_INFINITY;
         for (int idx = 0; idx < groupToTypeMap[i].length; idx++) {
            final int k = groupToTypeMap[i][idx];
            if (k < 0)
               continue;
            ranksGT[i][k] = idx;
         }
      }
      return ranksGT;
   }

   /**
    * Constructs a contact selection matrix of ranks from the incidence matrix
    * \texttt{m} and skill counts \texttt{skillCounts}.
    * Assuming \texttt{m} is rectangular, this method
    * creates a matrix of ranks with \texttt{m.length} rows
    * and \texttt{m[0].length} columns.  For each agent group~$i$, and
    * each contact type~$k$, the method sets the rank to $\infty$ if the
    * contact cannot be served, i.e., if \texttt{m[k][i]} is \texttt{false}.
    * Otherwise, $\rGT(i, k)$ is set to \texttt{skillCounts[i]}.
    * If \texttt{skillCounts} is \texttt{null}, \texttt{skillCounts[i]}
    * is inferred by counting the number of \texttt{k} for
    * which \texttt{m[i][k]} is \texttt{true}.
    @param m the incidence matrix.
    @param skillCounts the skill counts.
    @return the matrix of ranks.
    @exception IllegalArgumentException if \texttt{m.length}
    is different from \texttt{skillCounts.length}.
    */
   public static double[][] getRanks (boolean[][] m, int[] skillCounts) {
      final int numTypes = m[0].length;
      final int numGroups = m.length;
      final double[][] ranksGT = new double[numGroups][numTypes];
      if (skillCounts != null && skillCounts.length != numGroups)
         throw new IllegalArgumentException
            ("A skill count is needed for each agent group");
      for (int i = 0; i < ranksGT.length; i++) {
         int sc;
         if (skillCounts == null) {
            sc = 0;
            for (int k = 0; k < m[i].length; k++)
               if (m[i][k])
                  sc++;
         }
         else
            sc = skillCounts[i];
         for (int k = 0; k < ranksGT[i].length; k++)
            if (m[i][k])
               ranksGT[i][k] = sc;
            else
               ranksGT[i][k] = Double.POSITIVE_INFINITY;
      }
      return ranksGT;
   }

   /**
    * Constructs and returns overflow lists from the
    * given matrix of ranks \texttt{ranksTG}.
    * More specifically,
    * the ranks matrix giving $\rTG(k,i)$ for all $k$ and $i$ is used
    * to generate \emph{overflow lists} defined as follows.
    * For each contact type $k$, this method creates a list of agent \emph{groupsets}
    * sharing the same priority.  The $j$th groupset for contact type $k$ is denoted
    * $i(k, j)=\{i=0,\ldots,I-1:\rTG(k,i)=r_{k,j}\}$. Here,
    * $r_{k,j_1}<r_{k,j_2}<\infty$ for any $j_1<j_2$.
    * The overflow list for contact type~$k$ is then
    * $i(k, 0), i(k, 1), \ldots$
    * Array \texttt{[k][j]} of the returned 3D array
    * contains the elements of $i(k,j)$.
    *
    * @param ranksTG the input matrix of ranks.
    * @return the overflow lists.
    */
   public static int[][][] getOverflowLists (double[][] ranksTG) {
      int[][][] overflowLists = new int[ranksTG.length][][];
      for (int k = 0; k < overflowLists.length; k++) {
         final Map<Double, List<Integer>> overflowMap = new TreeMap<Double, List<Integer>>();
         for (int i = 0; i < ranksTG[k].length; i++) {
            final double r = ranksTG[k][i];
            if (Double.isInfinite (r))
               continue;
            List<Integer> list = overflowMap.get (r);
            if (list == null) {
               list = new ArrayList<Integer>();
               overflowMap.put (r, list);
            }
            list.add (i);
         }
         overflowLists[k] = new int[overflowMap.size ()][];
         int idx = 0;
         for (final List<Integer> list : overflowMap.values ()) {
            overflowLists[k][idx] = new int[list.size ()];
            int idx2 = 0;
            for (final Integer in : list)
               overflowLists[k][idx][idx2++] = in;
            ++idx;
         }
      }
      return overflowLists;
   }

   /**
    * Formats the type-to-group ordered lists as a string.
    * For each supported contact type, a line containing
    * \texttt{Contact type k: [i1, i2, ...]} is generated,
    * where \texttt{i1}, \texttt{i2}, ... correspond to
    * agent group indices.
    * Each ordered list is formatted using {@link #formatOrderedList(int[])}.
    @param typeToGroupMap the type-to-group map being formatted.
    @return the type-to-group map, formatted as a string.
    */
   public static String formatTypeToGroupMap (int[][] typeToGroupMap) {
      final StringBuilder sb = new StringBuilder();
      for (int k = 0; k < typeToGroupMap.length; k++) {
         sb.append ("Contact type ").append (k).append (": [");
         sb.append (formatOrderedList (typeToGroupMap[k]));
         sb.append (']');
         if (k < typeToGroupMap.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
    * Formats the group-to-type ordered lists as a string.
    * For each supported agent group, a line containing
    * \texttt{Agent group i: [k1, k2, ...]} is generated,
    * where \texttt{k1}, \texttt{k2}, ... correspond to
    * contact type indices.
    * Each ordered list is formatted using {@link #formatOrderedList(int[])}.
    @param groupToTypeMap the group-to-type map being formatted.
    @return the group-to-type map, formatted as a string.
    */
   public static String formatGroupToTypeMap (int[][] groupToTypeMap) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < groupToTypeMap.length; i++) {
         sb.append ("Agent group ").append (i).append (": [");
         sb.append (formatOrderedList (groupToTypeMap[i]));
         sb.append ("]");
         if (i < groupToTypeMap.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /*
      @deprecated Use {@link #formatRanksGT(double[][])} instead.
    */  /*
   @Deprecated
   public static String formatRanks (double[][] ranks) {
      return formatRanksGT (ranks);
   }*/

   /**
    * Formats the agent selection matrix of ranks \texttt{ranksTG}
    * for each contact type and agent group.
    * For each contact type, the returned string contains a line
    * giving the rank of each agent group.  When a contact type cannot
    * be served by an agent group, a \texttt{-} is used to represent
    * the infinite rank.
    @param ranksTG the matrix of ranks to be formatted.
    @return the ranks formatted as a string.
    */
   public static String formatRanksTG (double[][] ranksTG) {
      final StringBuilder sb = new StringBuilder();
      for (int k = 0; k < ranksTG.length; k++) {
         sb.append ("Contact type ").append (k).append (": [");
         for (int i = 0; i < ranksTG[k].length; i++) {
            final double rank = ranksTG[k][i];
            sb.append (i > 0 ? ", " : "").append
               (Double.isInfinite (rank) ? "-" : String.valueOf (rank));
         }
         sb.append ("]");
         if (k < ranksTG.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
    * Formats the contact selection matrix of ranks \texttt{ranksGT}
    * for each contact type and agent group.
    * For each agent group, the returned string contains a line
    * giving the rank of each contact type.  When a contact type cannot
    * be served by an agent group, a \texttt{-} is used to represent
    * the infinite rank.
    @param ranksGT the matrix of ranks to be formatted.
    @return the ranks formatted as a string.
    */
   public static String formatRanksGT (double[][] ranksGT) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < ranksGT.length; i++) {
         sb.append ("Agent group ").append (i).append (": [");
         for (int k = 0; k < ranksGT[i].length; k++) {
            final double rank = ranksGT[i][k];
            sb.append (k > 0 ? ", " : "").append
               (Double.isInfinite (rank) ? "-" : String.valueOf (rank));
         }
         sb.append ("]");
         if (i < ranksGT.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
    * Formats the agent selection weights matrix \texttt{weightsTG}
    * for each contact type and agent group.
    * For each contact type, the returned string contains a line
    * giving the weight of each agent group.  A \texttt{-} is used to represent
    * an infinite weight.
    @param weightsTG the weights matrix to be formatted.
    @return the weights formatted as a string.
    */
   public static String formatWeightsTG (double[][] weightsTG) {
      final StringBuilder sb = new StringBuilder();
      for (int k = 0; k < weightsTG.length; k++) {
         sb.append ("Contact type ").append (k).append (": [");
         for (int i = 0; i < weightsTG[k].length; i++) {
            final double weight = weightsTG[k][i];
            sb.append (i > 0 ? ", " : "").append
               (Double.isInfinite (weight) ? "-" : String.valueOf (weight));
         }
         sb.append ("]");
         if (k < weightsTG.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
    * Formats the contact selection weights matrix \texttt{weightsGT}
    * for each contact type and agent group.
    * For each agent group, the returned string contains a line
    * giving the weight of each contact type.  A \texttt{-} is used to represent
    * an infinite weight.
    @param weightsGT the weights matrix to be formatted.
    @return the weights formatted as a string.
    */
   public static String formatWeightsGT (double[][] weightsGT) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < weightsGT.length; i++) {
         sb.append ("Agent group ").append (i).append (": [");
         for (int k = 0; k < weightsGT[i].length; k++) {
            final double weight = weightsGT[i][k];
            sb.append (k > 0 ? ", " : "").append
               (Double.isInfinite (weight) ? "-" : String.valueOf (weight));
         }
         sb.append ("]");
         if (i < weightsGT.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
    * Formats the incidence matrix \texttt{m}
    * for each contact type and agent group.
    * For each agent group, the returned string contains a line
    * giving the contact types it can serve.
    * Each line contains one value for each contact type.
    * The value 0 is used if the contact cannot be served and
    * 1 otherwise.
    @param m the incidence matrix to be formatted.
    @return the incidence matrix formatted as a string.
    */
   public static String formatIncidence (boolean[][] m) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < m.length; i++) {
         sb.append ("Agent group ").append (i).append (": [");
         for (int k = 0; k < m[i].length; k++)
            sb.append (k > 0 ? " " : "").append (m[i][k] ? "1" : "0");
         sb.append ("]");
         if (i < m.length - 1)
            sb.append ("\n");
      }
      return sb.toString();
   }

   /**
      @deprecated Use {@link #formatOrderedList(int[])} instead.
    */   /*
   @Deprecated
   public static String formatRankList (int[] rankList) {
      return formatOrderedList (rankList);
   } */

   /**
    * Formats the ordered list \texttt{orderedList} as a string.
    * This method constructs and returns a string containing
    * the comma-separated list of indices stored in \texttt{orderedList}.
    * If a negative index is found, it is replaced with \texttt{-1}
    * and formatted in the string only if at least one positive
    * index follows it.  For example, the ordered list \texttt{-2, 0, 3, -1, -1}
    * will be formatted as \texttt{-1, 0, 3}.
    @param orderedList the ordered list to be formatted.
    @return the string representing the ordered list.
    */
   public static String formatOrderedList (int[] orderedList) {
      if (orderedList == null)
         return "";
      final StringBuilder sb = new StringBuilder();
      boolean first = true;
      int nm = 0;
      for (final int element : orderedList)
         if (element < 0)
            nm++;
         else {
            for (int j = 0; j < nm; j++) {
               if (first)
                  first = false;
               else
                  sb.append (", ");
               sb.append ("-1");
            }
            nm = 0;
            if (first)
               first = false;
            else
               sb.append (", ");
            sb.append (element);
         }
      return sb.toString();
   }

   /**
    * Converts the routing table \texttt{table} to a rectangular
    * matrix containing \texttt{table.length} rows and
    * at least \texttt{minColumns} columns.
    * Assuming the given 2D array is a valid type-to-group or
    * group-to-type map, evaluates the maximum number
    * of columns in each row and pads the rows with -1
    * for the returned 2D array to be a rectangular matrix, i.e.,
    * each row has the same number of columns. In some
    * circumstances, this can simplify manipulation of the
    * routing table and the returned array is still compatible
    * with the routers since negative indices must be ignored.
    @param table the routing table to be converted.
    @param minColumns the minimal number of columns in the normalized
    routing table.
    @return the converted routing table.
    @exception NullPointerException if \texttt{table} or one of its
    elements is \texttt{null}.
    @exception IllegalArgumentException if \texttt{minColumns}
    is negative.
    */
   public static int[][] normalizeRoutingTable (int[][] table, int minColumns) {
      if (minColumns < 0)
         throw new IllegalArgumentException
            ("minColumns cannot be negative");
      int maxColumns = minColumns;
      for (final int[] row : table) {
         final int columns = row.length;
         if (columns > maxColumns)
            maxColumns = columns;
      }
      final int[][] res = new int[table.length][maxColumns];
      for (int i = 0; i < res.length; i++) {
         System.arraycopy (table[i], 0, res[i], 0, table[i].length);
         for (int j = table[i].length; j < res[i].length; j++)
            res[i][j] = -1;
      }
      return res;
   }

   /**
    * Equivalent to {@link #normalizeRoutingTable(int[][],int) normalizeRoutingTable}
    * \texttt{(table, 0)}.
    */
   public static int[][] normalizeRoutingTable (int[][] table) {
      return normalizeRoutingTable (table, 0);
   }

   private static final class GroupSorter implements IntComparator {
      private double[][] ranksTG;
      private int k;
      private int typeRegion;
      private int[] groupRegions;

      GroupSorter (double[][] ranksTG, int k) {
         this (ranksTG, k, 0, null);
      }

      GroupSorter (double[][] ranksTG, int k, int typeRegion, int[] groupRegions) {
         this.ranksTG = ranksTG;
         this.k = k;
         this.typeRegion = typeRegion;
         this.groupRegions = groupRegions;
      }

      public int compare (int i1, int i2) {
         if (groupRegions != null) {
            if (typeRegion == groupRegions[i1] &&
                typeRegion != groupRegions[i2])
               return -1;
            if (typeRegion != groupRegions[i1] &&
                typeRegion == groupRegions[i2])
               return 1;
         }
         final double rank1 = ranksTG[k][i1];
         final double rank2 = ranksTG[k][i2];
         if (rank1 == rank2)
            return i1 - i2;
         if (rank1 < rank2)
            return -1;
         if (rank2 < rank1)
            return 1;
         return 0;
      }
   }

   private static final class TypeSorter implements IntComparator {
      private double[][] ranksGT;
      private int i;
      private int groupRegion;
      private int[] typeRegions;

      TypeSorter (double[][] ranksGT, int i) {
         this (ranksGT, i, 0, null);
      }

      TypeSorter (double[][] ranksGT, int i, int groupRegion, int[] typeRegions) {
         this.ranksGT = ranksGT;
         this.i = i;
         this.groupRegion = groupRegion;
         this.typeRegions = typeRegions;
      }

      public int compare (int k1, int k2) {
         if (typeRegions != null) {
            if (groupRegion == typeRegions[k1] &&
                groupRegion != typeRegions[k2])
               return -1;
            if (groupRegion != typeRegions[k1] &&
                groupRegion == typeRegions[k2])
               return 1;
         }
         final double rank1 = ranksGT[i][k1];
         final double rank2 = ranksGT[i][k2];
         if (rank1 == rank2)
            return k1 - k2;
         if (rank1 < rank2)
            return -1;
         if (rank2 < rank1)
            return 1;
         return 0;
      }
   }
}
