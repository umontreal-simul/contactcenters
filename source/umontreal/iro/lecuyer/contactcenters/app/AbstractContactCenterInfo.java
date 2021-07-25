package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Collections;
import java.util.Map;

/**
 * Provides default implementations for
 * some methods in {@link ContactCenterInfo}.
 * Implemented methods giving strings return
 * \texttt{null},
 * methods giving integers return
 * 0, and methods giving properties
 * return empty maps. 
 * Method {@link #getNumContactTypesWithSegments()}
 * and other similar methods return the
 * number of contact types plus the number
 * of segments regrouping contact types.
 */
public abstract class AbstractContactCenterInfo implements ContactCenterInfo {
   public String getContactTypeName (int k) {
      return null;
   }
   
   public String getAgentGroupName (int i) {
      return null;
   }
   
   public String getWaitingQueueName (int q) {
      return null;
   }
   
   public String getMainPeriodName (int mp) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getAgentGroupProperties (int i) {
      return Collections.EMPTY_MAP;
   }

   public String getAgentGroupSegmentName (int i) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getAgentGroupSegmentProperties (int i) {
      return Collections.EMPTY_MAP;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getContactTypeProperties (int k) {
      return Collections.EMPTY_MAP;
   }

   public String getContactTypeSegmentName (int k) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getContactTypeSegmentProperties (int k) {
      return Collections.EMPTY_MAP;
   }

   public String getInContactTypeSegmentName (int k) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getInContactTypeSegmentProperties (int k) {
      return Collections.EMPTY_MAP;
   }

   public String getMainPeriodSegmentName (int mp) {
      return null;
   }

   public int getNumAgentGroupSegments () {
      return 0;
   }

   public int getNumContactTypeSegments () {
      return 0;
   }

   public int getNumInContactTypeSegments () {
      return 0;
   }

   public int getNumMainPeriodSegments () {
      return 0;
   }

   public int getNumOutContactTypeSegments () {
      return 0;
   }

   public int getNumWaitingQueueSegments () {
      return 0;
   }
   
   public int getNumContactTypesWithSegments () {
      final int K = getNumContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumInContactTypesWithSegments () {
      final int K = getNumInContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumInContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumOutContactTypesWithSegments () {
      final int K = getNumOutContactTypes ();
      if (K <= 1)
         return K;
      final int s = getNumOutContactTypeSegments ();
      return K + s + 1; // Include implicit segment
   }

   public int getNumAgentGroupsWithSegments () {
      final int I = getNumAgentGroups ();
      if (I <= 1)
         return I;
      final int s = getNumAgentGroupSegments ();
      return I + s + 1; // Include implicit segment
   }

   public int getNumMainPeriodsWithSegments () {
      final int P = getNumMainPeriods ();
      if (P <= 1)
         return P;
      final int s = getNumMainPeriodSegments ();
      return P + s + 1; // Include implicit segment
   }

   public int getNumWaitingQueuesWithSegments () {
      final int Q = getNumWaitingQueues ();
      if (Q <= 1)
         return Q;
      final int s = getNumWaitingQueueSegments ();
      return Q + s + 1; // Include implicit segment
   }

   public String getOutContactTypeSegmentName (int k) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getOutContactTypeSegmentProperties (int k) {
      return Collections.EMPTY_MAP;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getWaitingQueueProperties (int q) {
      return Collections.EMPTY_MAP;
   }

   public String getWaitingQueueSegmentName (int k) {
      return null;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getWaitingQueueSegmentProperties (int q) {
      return Collections.EMPTY_MAP;
   }

   public String getMatrixOfAWTName (int m) {
      return null;
   }
}
