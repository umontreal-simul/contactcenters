package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Collections;
import java.util.Map;

import umontreal.iro.lecuyer.collections.MergedMap;

/**
 * \javadoc{Represents the row type for a matrix regrouping performance
 * measures. }Each type of performance measure has a row type that affects the
 * number and role of rows in any matrix of performance measures of that type.
 * Of course, the number of rows is also affected by the parameters of the
 * contact center.
 *
 * Each row of a matrix of performance measures corresponds to one type of
 * event. Usually, there is one row per contact type or agent group, and an
 * extra row for the aggregate measures.\javadoc{ If estimates of some
 * performance measures are missing in a matrix of results, e.g., an
 * approximation cannot compute them, they can be replaced by
 * \texttt{Double.NaN}.} The aggregate value is often defined as the sum of the
 * values for each event type. In this case, if there is a single event type,
 * the matrix has a single row since the per-type and aggregate values are the
 * same.
 *
 * @xmlconfig.title Supported row types
 */
public enum RowType {
   /**
    * Rows representing
    * segments of inbound contact types. More specifically,
    * let $\Ki'\ge\Ki$ be the number of rows of this type for a specific model of contact center.
    * If a matrix
    * has rows of this type and if there are $\Ki$ inbound contact types
    * in the model, row
    * $k=0,\ldots,\Ki-1$ represents contact type~$k$ while row~$\Ki'-1$ is used to
    * represent all contact types. Rows $\Ki,\ldots,\Ki'-2$ represent user-defined
    * segments regrouping inbound contact types.
    * If $\Ki=1$, a single row represents the single inbound
    * contact type, and $\Ki'=\Ki$.
    *
    * @xmlconfig.title
    */
   INBOUNDTYPE {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Types"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         final int Ki = eval.getNumInContactTypes();
         final int Kis = Ki > 1 ? eval.getNumInContactTypeSegments() : 0;

         if (row >= Ki + Kis)
            return Messages.getString ("RowType.AllInboundTypes"); //$NON-NLS-1$
         else if (row >= Ki) {
            final int s = row - Ki;
            final String typn = eval.getInContactTypeSegmentName (s);
            if (typn == null || typn.length () == 0)
               return String.format (Messages
                     .getString ("RowType.InboundTypeSegment"), s);
            else
               return typn;
         }
         else if (eval.getNumOutContactTypes () == 0)
            return CONTACTTYPE.getName (eval, row);
         else {
            final String typn = eval.getContactTypeName (row);
            if (typn == null || typn.length () == 0) {
               final int k = row;
               return String.format (
                     Messages.getString ("RowType.InboundType"), k);
            }
            else
               return typn;
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Ki = eval.getNumInContactTypes();
         final int Kis = Ki > 1 ? eval.getNumInContactTypeSegments() : 0;

         if (row >= Ki + Kis)
            return Collections.EMPTY_MAP;
         else if (row >= Ki) {
            final int s = row - Ki;
            return eval.getInContactTypeSegmentProperties (s);
         }
         else
            return eval.getContactTypeProperties (row);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int K = eval.getNumInContactTypes ();
         if (K <= 1)
            return K;
         return K + 1 + eval.getNumInContactTypeSegments();
      }
   },

   /**
    * Rows representing segments of
    * inbound contact types, for performance measures using
    * acceptable waiting times. This is similar to {@link #INBOUNDTYPE}, except
    * that there is one group of rows for each matrix of acceptable waiting
    * times.
    * More specifically, if there are $\Ki'$
    * segments of inbound contact types and
    *  $M$ user-specified  matrices of
    * acceptable waiting times (often, $M=1$), row $m\Ki' + k$ represents
    * segment of inbound contact types~$k$ with the $m$th matrix of AWTs.
    * The total number of rows is $M\Ki'$.
    *
    * @xmlconfig.title
    */
   INBOUNDTYPEAWT {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Types"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         if (eval.getNumMatricesOfAWT () == 1)
            return INBOUNDTYPE.getName (eval, row);
         final int nt = INBOUNDTYPE.count (eval);
         final int k = row % nt;
         final int m = row / nt;
         final int Ki = eval.getNumInContactTypes();
         final int Kis = Ki > 1 ? eval.getNumInContactTypeSegments() : 0;
         final String mn = eval.getMatrixOfAWTName (m);
         final String mname;
         if (mn == null || mn.length () == 0)
            mname = String.valueOf (m);
         else
            mname = mn;

         if (k == Ki + Kis)
            return String.format (Messages
                  .getString ("RowType.AllInboundTypesAWT"), mname);
         else if (k >= Ki) {
            final int s = k - Ki;
            final String typn = eval.getInContactTypeSegmentName (s);
            if (typn == null || typn.length () == 0)
               return String.format (Messages
                     .getString ("RowType.InboundTypeSegmentAWT"), s, mname);
            else
               return String.format (Messages
                     .getString ("RowType.InboundTypeNameAWT"), typn, mname);
         }
         else {
            final String typn = eval.getContactTypeName (k);
            if (typn == null || typn.length () == 0)
               return String.format (Messages
                     .getString ("RowType.InboundTypeAWT"), k, mname);
            else
               return String.format (Messages
                     .getString ("RowType.InboundTypeNameAWT"), typn, mname);
         }
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int nt = INBOUNDTYPE.count (eval);
         final int k = row % nt;
         return INBOUNDTYPE.getProperties (eval, k);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int M = eval.getNumMatricesOfAWT ();
         return M*INBOUNDTYPE.count (eval);
      }
   },

   /**
    * Rows representing
    * segments of outbound contact types. More specifically,
    * let $\Ko'\ge\Ko$ be the number of rows of this type for a specific model of contact center.
    * If a matrix
    * has rows of this type and if there are $\Ko$ outbound contact types
    * in the model, row
    * $k=0,\ldots,\Ko-1$ represents outbound contact type~$k$ while row~$\Ko'-1$ is used
    * to represent all contact types. Rows $\Ko,\ldots,\Ko'-2$ represent user-defined
    * segments regrouping outbound contact types.
    * If $\Ko=1$, a single row represents the single outbound
    * contact type, and $\Ko'=\Ko$.
    *
    * @xmlconfig.title
    */
   OUTBOUNDTYPE {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Types"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         final int Ko = eval.getNumOutContactTypes();
         final int Kos = Ko > 1 ? eval.getNumOutContactTypeSegments() : 0;

         if (row >= Ko + Kos)
            return Messages.getString ("RowType.AllOutboundTypes"); //$NON-NLS-1$
         else if (row >= Ko) {
            final int s = row - Ko;
            final String typn = eval.getOutContactTypeSegmentName (s);
            if (typn == null || typn.length () == 0)
               return String.format (Messages
                     .getString ("RowType.OutboundTypeSegment"), s);
            else
               return typn;
         }
         else if (eval.getNumInContactTypes () == 0)
            return CONTACTTYPE.getName (eval, row);
         else {
            final String typn = eval.getContactTypeName (row
                  + eval.getNumInContactTypes ());
            if (typn == null || typn.length () == 0) {
               final int k = row + eval.getNumInContactTypes ();
               return String.format (Messages
                     .getString ("RowType.OutboundType"), k);
            }
            else
               return typn;
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Ko = eval.getNumOutContactTypes();
         final int Kos = Ko > 1 ? eval.getNumOutContactTypeSegments() : 0;

         if (row >= Ko + Kos)
            return Collections.EMPTY_MAP;
         else if (row >= Ko) {
            final int s = row - Ko;
            return eval.getOutContactTypeSegmentProperties (s);
         }
         else
            return eval.getContactTypeProperties (row + eval.getNumInContactTypes());
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int K = eval.getNumOutContactTypes ();
         if (K <= 1)
            return K;
         return K + 1 + eval.getNumOutContactTypeSegments();
      }
   },

   /**
    * Rows representing
    * segments of contact types. More specifically,
    * let $K'\ge K$ be the number of rows of this type for a specific model of contact center.
    * If a matrix
    * has rows of this type and if there are $K$ contact types
    * in the model, row
    * $k=0,\ldots,K-1$ represents contact type~$k$ while row~$K'-1$ is used for
    * representing all contact types. Rows $K,\ldots,K'-2$ represent user-defined
    * segments regrouping contact types.
    * If $K=1$, a single row represents the single
    * contact type, and $K'=K$.
    *
    * @xmlconfig.title
    */
   CONTACTTYPE {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Types"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         final int K = eval.getNumContactTypes();
         final int Ks = K > 1 ? eval.getNumContactTypeSegments() : 0;

         if (row >= K + Ks)
            return Messages.getString ("RowType.AllTypes"); //$NON-NLS-1$
         else if (row >= K) {
            final int s = row - K;
            final String typn = eval.getContactTypeSegmentName (s);
            if (typn == null || typn.length () == 0)
               return String.format (Messages.getString ("RowType.TypeSegment"), s);
            else
               return typn;
         }
         else {
            final String typn = eval.getContactTypeName (row);
            if (typn == null || typn.length () == 0)
               return String.format (Messages.getString ("RowType.Type"), row);
            else
               return typn;
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int K = eval.getNumContactTypes();
         final int Ks = K > 1 ? eval.getNumContactTypeSegments() : 0;

         if (row >= K + Ks)
            return Collections.EMPTY_MAP;
         else if (row >= K) {
            final int s = row - K;
            return eval.getContactTypeSegmentProperties (s);
         }
         else
            return eval.getContactTypeProperties (row);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int K = eval.getNumContactTypes ();
         if (K <= 1)
            return K;
         return K + 1 + eval.getNumContactTypeSegments();
      }
   },

   /**
    * Rows representing inbound contact types/agent group pairs. More specifically, let
    * $\Ki'$ be the number of segments of inbound contact types, and
    * $I'$ be the number of segments of agent groups.
    * If a matrix has this type of row, row $kI' + i$,
    * for $k=0,\ldots,\Ki'-1$ and $i=0,\ldots, I'-1$, represents
    * inbound contact types in segment~$k$ served by agents
    * in segment of groups~$i$.
    * The total number of rows is $\Ki'I'$.
    *
    * @xmlconfig.title
    */
   INBOUNDTYPEAGENTGROUP {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.TypesGroups"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         // row = k*I' + i
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final String tn = INBOUNDTYPE.getName (eval, k);
         final String gn = AGENTGROUP.getName (eval, i);
         return tn + ", " + gn;
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final Map<String, String> prop1 = INBOUNDTYPE.getProperties (eval, k);
         final Map<String, String> prop2 = AGENTGROUP.getProperties (eval, i);
         return new MergedMap<String, String> (prop1, prop2);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return INBOUNDTYPE.count (eval) * AGENTGROUP.count (eval);
      }
   },

   /**
    * Rows representing inbound contact types/agent group pairs, for performance
    * measures using acceptable waiting times. This is similar to
    * {@link #INBOUNDTYPEAGENTGROUP}, except that there is one group of rows
    * for each matrix of acceptable waiting times. More specifically, if there
    * are $\Ki$ segments of inbound contact types, $I'$
    * segments of agent groups, and $M$ matrices of
    * acceptable waiting times (often, $M=1$), row $m\Ki'I' + kI' + i$
    * represents segment of inbound contact types~$k$ and agent group~$i$ with the $m$th
    * matrix of AWTs. The total number of rows is $M\Ki'I'$.
    *
    * @xmlconfig.title
    */
   INBOUNDTYPEAWTAGENTGROUP {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.TypesGroups"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         // row = m*Ki'*I' + k*I' + i
         final int Kip = INBOUNDTYPE.count (eval);
         final int Ip = AGENTGROUP.count (eval);
         final int m = row / (Kip * Ip);
         final int row2 = row % (Kip * Ip);
         final int i = row2 % Ip;
         final int k = row2 / Ip;
         final String tn = INBOUNDTYPEAWT.getName (eval, m * Kip + k);
         final String gn = AGENTGROUP.getName (eval, i);
         return tn + ", " + gn;
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Kip = INBOUNDTYPE.count (eval);
         final int Ip = AGENTGROUP.count (eval);
         final int row2 = row % (Kip * Ip);
         final int i = row2 % Ip;
         final int k = row2 / Ip;

         final Map<String, String> prop1 = INBOUNDTYPE.getProperties (eval, k);
         final Map<String, String> prop2 = AGENTGROUP.getProperties (eval, i);
         return new MergedMap<String, String> (prop1, prop2);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return INBOUNDTYPEAWT.count (eval) * AGENTGROUP.count (eval);
      }
   },

   /**
    * Rows representing outbound contact types/agent group pairs. More specifically, let
    * $\Ko'$ be the number of segments of outbound contact types, and
    * $I'$ be the number of segments of agent groups.
    * If a matrix has this type of row, row $kI' + i$,
    * for $k=0,\ldots,\Ko'-1$ and $i=0,\ldots, I'-1$, represents
    * outbound contact types in segment~$k$ served by agents
    * in segment of groups~$i$.
    * The total number of rows is $\Ko'I'$.
    *
    * @xmlconfig.title
    */
   OUTBOUNDTYPEAGENTGROUP {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.TypesGroups"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         // row = kI' + i
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final String tn = OUTBOUNDTYPE.getName (eval, k);
         final String gn = AGENTGROUP.getName (eval, i);
         return tn + ", " + gn;
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final Map<String, String> prop1 = OUTBOUNDTYPE.getProperties (eval, k);
         final Map<String, String> prop2 = AGENTGROUP.getProperties (eval, i);
         return new MergedMap<String, String> (prop1, prop2);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return OUTBOUNDTYPE.count (eval) * AGENTGROUP.count (eval);
      }
   },

   /**
    * Rows representing contact types/agent group pairs. More specifically, let
    * $K'$ be the number of segments of contact types, and
    * $I'$ be the number of segments of agent groups.
    * If a matrix has this type of row, row $kI' + i$,
    * for $k=0,\ldots,K'-1$ and $i=0,\ldots, I'-1$, represents
    * contact types in segment~$k$ served by agents
    * in segment of groups~$i$.
    * The total number of rows is $K'I'$.
    *
    * @xmlconfig.title
    */
   CONTACTTYPEAGENTGROUP {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.TypesGroups"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         // row = kI' + i
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final String tn = CONTACTTYPE.getName (eval, k);
         final String gn = AGENTGROUP.getName (eval, i);
         return tn + ", " + gn;
      }

      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Ip = AGENTGROUP.count (eval);
         final int i = row % Ip;
         final int k = row / Ip;
         final Map<String, String> prop1 = CONTACTTYPE.getProperties (eval, k);
         final Map<String, String> prop2 = AGENTGROUP.getProperties (eval, i);
         return new MergedMap<String, String> (prop1, prop2);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         return CONTACTTYPE.count (eval) * AGENTGROUP.count (eval);
      }
   },

   /**
    * Rows representing waiting queues. More specifically,
    * let $Q'\ge Q$ be the number of rows of this type for a specific model of contact center.
    * If a matrix
    * has rows of this type and if there are $Q$ waiting queues
    * in the model, row
    * $q=0,\ldots,Q-1$ represents waiting queue~$q$ while row~$Q'-1$ is used for
    * representing all waiting queues. Rows $Q,\ldots,Q'-2$ represent user-defined
    * segments regrouping waiting queues.
    * If $Q=1$, a single row represents the single
    * waiting queue, and $Q'=Q$.
    *
    * @xmlconfig.title
    */
   WAITINGQUEUE {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Queues"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         final int Q = eval.getNumWaitingQueues();
         final int Qs = Q > 1 ? eval.getNumWaitingQueueSegments() : 0;

         if (row >= Q + Qs)
            return Messages.getString ("RowType.AllQueues"); //$NON-NLS-1$
         else if (row >= Q) {
            final int s = row - Q;
            final String qn = eval.getWaitingQueueSegmentName (s);
            return qn == null || qn.length () == 0 ? String.format (
                  Messages.getString ("RowType.QueueSegment"), s) : qn;
         }
         else {
            final String qn = eval.getWaitingQueueName (row);
            if (qn == null || qn.length () == 0)
               return String.format (Messages.getString ("RowType.Queue"), row);
            else
               return qn;
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int Q = eval.getNumWaitingQueues();
         final int Qs = Q > 1 ? eval.getNumWaitingQueueSegments() : 0;

         if (row >= Q + Qs)
            return Collections.EMPTY_MAP;
         else if (row >= Q) {
            final int s = row - Q;
            return eval.getWaitingQueueSegmentProperties (s);
         }
         else
            return eval.getWaitingQueueProperties (row);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int Q = eval.getNumWaitingQueues ();
         if (Q <= 1)
            return Q;
         final int s = eval.getNumWaitingQueueSegments();
         return Q + 1 + s;
      }
   },

   /**
    * Rows representing agent groups. More specifically,
    * let $I'\ge I$ be the number of rows of this type for a specific model of contact center.
    * If a matrix
    * has rows of this type and if there are $I$ agent groups
    * in the model, row
    * $i=0,\ldots,I-1$ represents agent group~$i$ while row~$I'-1$ is used for
    * representing all agent groups. Rows $I,\ldots,I'-2$ represent user-defined
    * segments regrouping agent groups.
    * If $I=1$, a single row represents the single
    * agent group, and $I'=I$.
    *
    * @xmlconfig.title
    */
   AGENTGROUP {
      @Override
      public String getTitle () {
         return Messages.getString ("RowType.Groups"); //$NON-NLS-1$
      }

      @Override
      public String getName (ContactCenterInfo eval, int row) {
         final int I = eval.getNumAgentGroups();
         final int Is = I > 1 ? eval.getNumAgentGroupSegments() : 0;

         if (row >= I + Is)
            return Messages.getString ("RowType.AllGroups");
         else if (row >= I) {
            final int s = row - I;
            final String grpn = eval.getAgentGroupSegmentName (s);
            return grpn == null || grpn.length () == 0 ? String.format (
                  Messages.getString ("RowType.GroupSegment"), s) : grpn;
         }
         else {
            final String grpn = eval.getAgentGroupName (row);
            return grpn == null || grpn.length () == 0 ? String.format (
                  Messages.getString ("RowType.Group"), row) : grpn;
         }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Map<String, String> getProperties (ContactCenterInfo eval, int row) {
         final int I = eval.getNumAgentGroups();
         final int Is = I > 1 ? eval.getNumAgentGroupSegments() : 0;

         if (row >= I + Is)
            return Collections.EMPTY_MAP;
         else if (row >= I) {
            final int s = row - I;
            return eval.getAgentGroupSegmentProperties (s);
         }
         else
            return eval.getAgentGroupProperties (row);
      }

      @Override
      public int count (ContactCenterInfo eval) {
         final int I = eval.getNumAgentGroups ();
         if (I <= 1)
            return I;
         return I + 1 + eval.getNumAgentGroupSegments();
      }
   };

   /**
    * Determines if this row type corresponds to contact types.
    * Returns \texttt{true} if and only if
    * this row type corresponds to
    * {@link #INBOUNDTYPE},
    * {@link #INBOUNDTYPEAWT},
    * {@link #OUTBOUNDTYPE}, or
    * {@link #CONTACTTYPE}.
    @return \texttt{true} if and only if this row type is related to contact types.
    */
   public boolean isContactType () {
      switch (this) {
      case INBOUNDTYPE:
      case INBOUNDTYPEAWT:
      case OUTBOUNDTYPE:
      case CONTACTTYPE:
         return true;
      default:
         return false;
      }
   }

   /**
    * Determines if this row type corresponds to
    * (contact type, agent group) pairs.
    * Returns \texttt{true} if and only if
    * this row type corresponds to
    * {@link #INBOUNDTYPEAGENTGROUP},
    * {@link #INBOUNDTYPEAWTAGENTGROUP},
    * {@link #OUTBOUNDTYPEAGENTGROUP}, or
    * {@link #CONTACTTYPEAGENTGROUP}.
    @return \texttt{true} if and only if this row type is related to
    (contact type, agent group) pairs.
    */
   public boolean isContactTypeAgentGroup () {
      switch (this) {
      case INBOUNDTYPEAGENTGROUP:
      case INBOUNDTYPEAWTAGENTGROUP:
      case OUTBOUNDTYPEAGENTGROUP:
      case CONTACTTYPEAGENTGROUP:
         return true;
      default:
         return false;
      }
   }

   /**
    * Converts this row type to a row type representing inbound
    * contact types.
    * Returns {@link #INBOUNDTYPE} if this row type
    * corresponds to {@link #CONTACTTYPE}, and
    * {@link #INBOUNDTYPEAGENTGROUP} if this
    * row type corresponds to {@link #CONTACTTYPEAGENTGROUP}.
    * Otherwise, throws an illegal-argument exception.
    @return the equivalent of this row type for inbound contact types.
    */
   public RowType toInboundType () {
      switch (this) {
      case CONTACTTYPE:
         return INBOUNDTYPE;
      case CONTACTTYPEAGENTGROUP:
         return INBOUNDTYPEAGENTGROUP;
      case INBOUNDTYPE:
      case INBOUNDTYPEAGENTGROUP:
         return this;
      }
      throw new IllegalArgumentException ("Invalid row type " + name ());
   }

   /**
    * Similar to {@link #toInboundType()}, but converts
    * to inbound contact type with acceptable waiting times.
    * Returns {@link #INBOUNDTYPEAWT} if this row type
    * corresponds to {@link #CONTACTTYPE}, and
    * {@link #INBOUNDTYPEAWTAGENTGROUP} if this
    * row type corresponds to {@link #CONTACTTYPEAGENTGROUP}.
    * Otherwise, throws an illegal-argument exception.
    @return the equivalent of this row type for inbound contact types.
    */
   public RowType toInboundTypeAWT () {
      switch (this) {
      case CONTACTTYPE:
         return INBOUNDTYPEAWT;
      case CONTACTTYPEAGENTGROUP:
         return INBOUNDTYPEAWTAGENTGROUP;
      case INBOUNDTYPE:
         return INBOUNDTYPEAWT;
      case INBOUNDTYPEAGENTGROUP:
         return INBOUNDTYPEAWTAGENTGROUP;
      case INBOUNDTYPEAWT:
      case INBOUNDTYPEAWTAGENTGROUP:
         return this;
      }
      throw new IllegalArgumentException ("Invalid row type " + name ());
   }

   /**
    * Converts this row type to a row type representing outbound
    * contact types.
    * Returns {@link #OUTBOUNDTYPE} if this row type
    * corresponds to {@link #CONTACTTYPE}, and
    * {@link #OUTBOUNDTYPEAGENTGROUP} if this
    * row type corresponds to {@link #CONTACTTYPEAGENTGROUP}.
    * Otherwise, throws an illegal-argument exception.
    @return the equivalent of this row type for outbound contact types.
    */
   public RowType toOutboundType () {
      switch (this) {
      case CONTACTTYPE:
         return OUTBOUNDTYPE;
      case CONTACTTYPEAGENTGROUP:
         return OUTBOUNDTYPEAGENTGROUP;
      case OUTBOUNDTYPE:
      case OUTBOUNDTYPEAGENTGROUP:
         return this;
      }
      throw new IllegalArgumentException ("Invalid row type " + name ());
   }

   /**
    * Returns the equivalent of this row type for
    * pairs with agent groups.
    * This method returns {@link #INBOUNDTYPEAGENTGROUP}
    * if this row type is
    * {@link #INBOUNDTYPE},
    * {@link #CONTACTTYPEAGENTGROUP} if this row type is
    * {@link #CONTACTTYPE}, etc.
    @return the equivalent of this row type for pairs with agent groups.
    */
   public RowType toContactTypeAgentGroup() {
      switch (this) {
      case INBOUNDTYPE:
         return INBOUNDTYPEAGENTGROUP;
      case INBOUNDTYPEAWT:
         return INBOUNDTYPEAWTAGENTGROUP;
      case OUTBOUNDTYPE:
         return OUTBOUNDTYPEAGENTGROUP;
      case CONTACTTYPE:
         return CONTACTTYPEAGENTGROUP;
      }
      throw new IllegalArgumentException ("Invalid row type " + name ());
   }

   /**
    * Reverse of {@link #toContactTypeAgentGroup()}.
    * For example,
    * this returns {@link #INBOUNDTYPE}
    * if this row type is
    * {@link #INBOUNDTYPEAGENTGROUP},
    * {@link #CONTACTTYPE} if this row type is
    * {@link #CONTACTTYPEAGENTGROUP}, etc.
    */
   public RowType toContactType() {
      switch (this) {
      case INBOUNDTYPEAGENTGROUP:
         return INBOUNDTYPE;
      case INBOUNDTYPEAWTAGENTGROUP:
         return INBOUNDTYPEAWT;
      case OUTBOUNDTYPEAGENTGROUP:
         return OUTBOUNDTYPE;
      case CONTACTTYPEAGENTGROUP:
         return CONTACTTYPE;
      }
      throw new IllegalArgumentException ("Invalid row type " + name ());
   }

   /**
    * Returns the title that should identify the rows of matrices of results for
    * this type of row. For example, this may return \texttt{Groups} for
    * {@link #AGENTGROUP}.
    *
    * @return the row title.
    */
   public abstract String getTitle ();

   /**
    * Returns the name associated with the row \texttt{row} in a matrix of
    * results for this type of row estimated by \texttt{eval}. For example, if
    * the method is called for {@link #INBOUNDTYPE} and row~0, it may
    * return \texttt{inbound type 0}.
    *
    * @param eval
    *           the contact center evaluation object.
    * @param row
    *           the row index.
    * @return the row name.
    */
   public abstract String getName (ContactCenterInfo eval, int row);

   /**
    * Returns the properties associated with row
    * \texttt{row}.
    * Properties are additional strings describing
    * a row.
    * This can include, e.g., the language of the customers,
    * the originating region, etc.
    * If no property is defined for the given
    * row, this method returns an empty map.
    *
    * @param eval the evaluation system.
    * @param row the row index.
    * @return the properties.
    */
   public abstract Map<String, String> getProperties (ContactCenterInfo eval, int row);

   /**
    * Returns the usual number of rows in a matrix of
    * performance measures with
    * rows of this
    * type estimated by the evaluation system \texttt{eval}.
    *
    * @param eval
    *           the queried evaluation system.
    * @return the number of rows.
    */
   public abstract int count (ContactCenterInfo eval);
}
