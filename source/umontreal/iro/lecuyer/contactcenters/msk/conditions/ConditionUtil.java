package umontreal.iro.lecuyer.contactcenters.msk.conditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.ConditionParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.ConditionParamsList;
import umontreal.iro.lecuyer.contactcenters.msk.params.IndexThreshIntParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.IndexThreshIntWithTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.IndexThreshWithTypeParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.Relationship;
import umontreal.iro.lecuyer.contactcenters.msk.params.StatConditionParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.TwoIndicesParams;
import umontreal.iro.lecuyer.contactcenters.msk.params.TwoIndicesWithTypesParams;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;
import umontreal.iro.lecuyer.xmlbind.params.Named;
import umontreal.iro.lecuyer.xmlbind.params.PropertiesParams;

/**
 * Provides helper methods to construct condition objects
 * using {@link ConditionParams} instances usually parsed
 * from XML configuration files.
 * The main method of this class is 
 * {@link #createCondition(CallCenter,int,ConditionParams)}
 * which uses other methods to make a {@link Condition}
 * object out of the information provided by the parameters. 
 */
public class ConditionUtil {
   private ConditionUtil() {}
   
   /**
    * Constructs a condition object for call center \texttt{cc}, and
    * using parameters \texttt{par}.
    * The type of the returned value depends on the
    * parameters in \texttt{par}.
    * If the \texttt{either} or \texttt{all} elements are set,
    * the method calls 
    * {@link #createOrCondition(CallCenter,int,ConditionParamsList)} or
    * {@link #createAndCondition(CallCenter,int,ConditionParamsList)}, 
    * respectively, and returns the result.
    * If the \texttt{queueSizes} element is set, the
    * method returns the result of 
    * {@link #createQueueSizeCondition(CallCenter,TwoIndicesWithTypesParams)}.
    * If the \texttt{queueSizeThresh} element is set,
    * the method returns the result of 
    * {@link #createQueueSizeThreshCondition(CallCenter,IndexThreshIntWithTypeParams)}.
    * If \texttt{numFreeAgents} is set, the method
    * returns the result of {@link #createNumFreeAgentsCondition(CallCenter,TwoIndicesParams)}.
    * If \texttt{numFreeAgentsThresh} is set, the method
    * returns the result of {@link #createNumFreeAgentsThreshCondition(CallCenter,IndexThreshIntParams)}.
    * If \texttt{fracBusyAgents} is set, the method
    * returns the result of {@link #createFracBusyAgentsCondition(CallCenter,TwoIndicesWithTypesParams)}.
    * If \texttt{fracBusyAgentsThresh} is set, the method
    * returns the result of {@link #createFracBusyAgentsThreshCondition(CallCenter,IndexThreshWithTypeParams)}.
    * The result of {@link #createCustomCondition(CallCenter,int,Named)} is
    * returned if \texttt{custom} is set.
    * @param cc the call center model.
    * @param k the index of the call type for which the condition concerns.
    * @param par the parameters from which conditions are created.
    * @return the condition obtained from parameters.
    * @exception IllegalArgumentException if a problem occurs
    * during the creation of the condition.
    */
   public static Condition createCondition (CallCenter cc, int k, ConditionParams par) {
      if (par.isSetEither ()) {
         ConditionParamsList par2 = par.getEither ();
         return createOrCondition (cc, k, par2);
      }
      else if (par.isSetAll ()) {
         ConditionParamsList par2 = par.getAll ();
         return createAndCondition (cc, k, par2);
      }
      else if (par.isSetQueueSizes ()) {
         final TwoIndicesWithTypesParams par2 = par.getQueueSizes ();
         return createQueueSizeCondition (cc, par2);
      }
      else if (par.isSetQueueSizeThresh ()) {
         final IndexThreshIntWithTypeParams par2 = par.getQueueSizeThresh ();
         return createQueueSizeThreshCondition (cc, par2);
      }
      else if (par.isSetNumFreeAgents ()) {
         final TwoIndicesParams par2 = par.getNumFreeAgents ();
         return createNumFreeAgentsCondition (cc, par2);
      }
      else if (par.isSetNumFreeAgentsThresh ()) {
         final IndexThreshIntParams par2 = par.getNumFreeAgentsThresh ();
         return createNumFreeAgentsThreshCondition (cc, par2);
      }
      else if (par.isSetFracBusyAgents ()) {
         final TwoIndicesWithTypesParams par2 = par.getFracBusyAgents ();
         return createFracBusyAgentsCondition (cc, par2);
      }
      else if (par.isSetFracBusyAgentsThresh ()) {
         final IndexThreshWithTypeParams par2 = par.getFracBusyAgentsThresh ();
         return createFracBusyAgentsThreshCondition (cc, par2);
      }
      else if (par.isSetStat ()) {
         final StatConditionParams par2 = par.getStat ();
         return new StatCondition (cc, par2);
      }
      else if (par.isSetCustom ()) {
         final Named par2 = par.getCustom ();
         return createCustomCondition (cc, k, par2);
      }
      else
         throw new IllegalArgumentException
         ("Unknown type of condition");
   }
   
   /**
    * Similar to {@link #createCondition(CallCenter,int,ConditionParams)}, but
    * from a {@link JAXBElement} instance.
    * The type of the condition created depends on the name of the element,
    * obtained using {@link JAXBElement#getName()}.
    * The value of the element, obtained using
    * {@link JAXBElement#getValue()}, is cast to the
    * appropriate class, and needed creation method is called.
    * @param cc the call center model.
    * @param k the index of the call type for which the condition concerns.
    * @param el the JAXB element corresponding to the condition.
    * @return the created condition object.
    * @exception IllegalArgumentException if a problem occurs
    * during the creation of the condition.
    */
   public static Condition createCondition (CallCenter cc, int k, JAXBElement<?> el) {
      String name = el.getName ().getLocalPart ();
      if (name.equals ("either")) {
         final ConditionParamsList par = (ConditionParamsList) el.getValue ();
         return createOrCondition (cc, k, par);
      }
      else if (name.equals ("all")) {
         final ConditionParamsList par = (ConditionParamsList) el.getValue ();
         return createAndCondition (cc, k, par);
      }
      else if (name.equals ("queueSizes")) {
         final TwoIndicesWithTypesParams par = (TwoIndicesWithTypesParams) el.getValue ();
         return createQueueSizeCondition (cc, par);
      }
      else if (name.equals ("queueSizeThresh")) {
         final IndexThreshIntWithTypeParams par = (IndexThreshIntWithTypeParams) el.getValue ();
         return createQueueSizeThreshCondition (cc, par);
      }
      else if (name.equals ("numFreeAgents")) {
         final TwoIndicesParams par = (TwoIndicesParams) el.getValue ();
         return createNumFreeAgentsCondition (cc, par);
      }
      else if (name.equals ("numFreeAgentsThresh")) {
         final IndexThreshIntParams par = (IndexThreshIntParams) el.getValue ();
         return createNumFreeAgentsThreshCondition (cc, par);
      }
      else if (name.equals ("fracBusyAgents")) {
         final TwoIndicesWithTypesParams par = (TwoIndicesWithTypesParams) el.getValue ();
         return createFracBusyAgentsCondition (cc, par);
      }
      else if (name.equals ("fracBusyAgentsThresh")) {
         final IndexThreshWithTypeParams par = (IndexThreshWithTypeParams) el.getValue ();
         return createFracBusyAgentsThreshCondition (cc, par);
      }
      else if (name.equals ("stat")) {
         final StatConditionParams par = (StatConditionParams) el.getValue ();
         return new StatCondition (cc, par);
      }
      else if (name.equals ("custom")) {
         final Named par = (Named)el.getValue ();
         return createCustomCondition (cc, k, par);
      }
      else
         throw new IllegalArgumentException
         ("Unknown type of condition: element name is " + name);
   }
   
   /**
    * Creates an ``or'' condition from the call center model
    * \texttt{cc}, and the parameters \texttt{par}.
    * The parameter object encapsulates a list of
    * {@link JAXBElement} representing parameters for
    * a condition.
    * The method uses {@link #createCondition(CallCenter,int,JAXBElement)}
    * to convert this element into a {@link Condition} object,
    * and gathers the created objects into an array used
    * to create the returned instance of
    * {@link OrCondition}.
    * @param cc the call center model.
    * @param k the index of the call type for which the condition concerns.
    * @param par the parameters for the condition.
    * @return the condition object.
    * @exception IllegalArgumentException if a problem occurs
    * during the creation of one of the associated conditions.
    */
   public static OrCondition createOrCondition (CallCenter cc, int k, ConditionParamsList par) {
      Condition[] conds = new Condition[par.getConditions ().size ()]; 
      for (int i = 0; i < conds.length; i++) {
         try {
            conds[i] = createCondition (cc, k, par.getConditions ().get (i));
         }
         catch (IllegalArgumentException iae) {
            IllegalArgumentException iaeOut = new IllegalArgumentException
            ("A problem occurred during the creation of the nested condition " + i + " for an or condition");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      return new OrCondition (conds);
   }

   /**
    * Similar to {@link #createOrCondition(CallCenter,int,ConditionParamsList)}, for
    * an ``and'' condition.
    */
   public static AndCondition createAndCondition (CallCenter cc, int k, ConditionParamsList par) {
      Condition[] conds = new Condition[par.getConditions ().size ()]; 
      for (int i = 0; i < conds.length; i++) {
         try {
            conds[i] = createCondition (cc, k, par.getConditions ().get (i));
         }
         catch (IllegalArgumentException iae) {
            IllegalArgumentException iaeOut = new IllegalArgumentException
            ("A problem occurred during the creation of the nested condition " + i + " for an and condition");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
      }
      return new AndCondition (conds);
   }
   
   /**
    * Creates a condition on the queue size from
    * parameters \texttt{par}, and call center model \texttt{cc}.
    * The method uses \texttt{par} to obtain the indices
    * $q_1$ and $q_2$ as well as the relationship
    * to compare with.
    * @param cc the call center model.
    * @param par the condition parameters.
    * @return the new condition object.
    */
   public static QueueSizesCondition createQueueSizeCondition (CallCenter cc, TwoIndicesWithTypesParams par) {
      final int q1 = par.getFirst ();
      final int q2 = par.getSecond ();
      final Relationship rel = par.getRel ();
      if (par.isSetFirstType () || par.isSetSecondType ()) {
         final int k1;
         if (par.isSetFirstType ())
            k1 = par.getFirstType ();
         else
            k1 = -1;
         final int k2;
         if (par.isSetSecondType ())
            k2 = par.getSecondType ();
         else
            k2 = -1;
         return new QueueSizesWithTypesCondition 
         (cc, q1, q2, k1, k2, rel);
      }
      return new QueueSizesCondition (cc, q1, q2, rel);
   }

   /**
    * Creates a condition on the queue size from
    * parameters \texttt{par}, and call center model \texttt{cc}.
    * The method uses \texttt{par} to obtain the index
    * $q$, threshold $\eta$, and relationship to
    * compare with.
    * @param cc the call center model.
    * @param par the condition parameters.
    * @return the new condition object.
    */
   public static QueueSizeThreshCondition createQueueSizeThreshCondition (CallCenter cc, IndexThreshIntWithTypeParams par) {
      final int q = par.getIndex ();
      final int threshold = par.getThreshold ();
      final Relationship rel = par.getRel ();
      if (par.isSetType ())
         return new QueueSizeThreshWithTypeCondition (cc, q, par.getType (), threshold, rel);
      else
         return new QueueSizeThreshCondition (cc, q, threshold, rel);
   }
   
   /**
    * Similar to {@link #createQueueSizeCondition(CallCenter,TwoIndicesWithTypesParams)}, for
    * a condition on the number of free agents.
    */
   public static NumFreeAgentsCondition createNumFreeAgentsCondition (CallCenter cc, TwoIndicesParams par) {
      final int i1 = par.getFirst ();
      final int i2 = par.getSecond ();
      final Relationship rel = par.getRel ();
      return new NumFreeAgentsCondition (cc, i1, i2, rel);
   }

   /**
    * Similar to {@link #createQueueSizeThreshCondition(CallCenter,IndexThreshIntWithTypeParams)}, for
    * a condition on the number of free agents.
    */
   public static NumFreeAgentsThreshCondition createNumFreeAgentsThreshCondition (CallCenter cc, IndexThreshIntParams par) {
      final int i = par.getIndex ();
      final int threshold = par.getThreshold ();
      final Relationship rel = par.getRel ();
      return new NumFreeAgentsThreshCondition (cc, i, threshold, rel);
   }
   
   /**
    * Creates a new condition on the fraction of busy agents
    * using the call center model \texttt{cc}, and
    * the parameters in \texttt{par}.
    * @param cc the call center model.
    * @param par the condition parameters.
    * @return the new condition object.
    */
   public static FracBusyAgentsCondition createFracBusyAgentsCondition (CallCenter cc, TwoIndicesWithTypesParams par) {
      final int i1 = par.getFirst ();
      final int i2 = par.getSecond ();
      final Relationship rel = par.getRel ();
      if (par.isSetFirstType () || par.isSetSecondType ()) {
         final int k1;
         if (par.isSetFirstType ())
            k1 = par.getFirstType ();
         else
            k1 = -1;
         final int k2;
         if (par.isSetSecondType ())
            k2 = par.getSecondType ();
         else
            k2 = -1;
         return new FracBusyAgentsWithTypesCondition
         (cc, i1, i2, k1, k2, rel);
      }
      return new FracBusyAgentsCondition
      (cc, i1, i2, rel);
   }

   /**
    * Creates a new condition on the fraction of busy agents
    * using the call center model \texttt{cc}, and
    * the parameters in \texttt{par}.
    * @param cc the call center model.
    * @param par the condition parameters.
    * @return the new condition object.
    */
   public static FracBusyAgentsThreshCondition createFracBusyAgentsThreshCondition (CallCenter cc, IndexThreshWithTypeParams par) {
      final int i = par.getIndex ();
      final double threshold = par.getThreshold ();
      final Relationship rel = par.getRel ();
      if (par.isSetType ()) {
         final int k = par.getType ();
         return new FracBusyAgentsThreshWithTypeCondition
         (cc, i, k, threshold, rel);
      }
      return new FracBusyAgentsThreshCondition
      (cc, i, threshold, rel);
   }
   
   /**
    * Calls {@link #createCustom(Class,CallCenter,int,Named) createCustom}
    * \texttt{(Condition.class, cc, k, par)}.
    * @param cc the call center model.
    * @param k the index of the call type concerning the condition.
    * @param par the parameters for the custom condition.
    * @return an instance representing the custom condition.
    */
   public static Condition createCustomCondition (CallCenter cc, int k, Named par) {
      return createCustom (Condition.class, cc, k, par);
   }
   
   /**
    * Creates an object of base class \texttt{base},  from the
    * parameter object \texttt{par}, and using the call
    * center model \texttt{cc}.
    * The method first uses the name associated with
    * \texttt{par} as a class name for
    * {@link Class#forName(String)}.
    * It then checks that the corresponding class is a subclass
    * of or implements \texttt{base}.
    * If this is true, it searches for a constructor, and
    * calls it to create an instance.
    * The method looks for the following signatures, and
    * the given order of priority:
    * ({@link CallCenter}, \texttt{int}, {@link Map}),
    * ({@link CallCenter}, \texttt{int}),
    * ({@link CallCenter}, {@link Map}),
    * (\texttt{int}, {@link Map}),
    * ({@link CallCenter}), 
    * (\texttt{int}), 
    * ({@link Map}), and ().
    * The last signature corresponds to the no-argument constructor.
    * The instance of {@link CallCenter} is \texttt{cc} while
    * the map is created by using
    * {@link ParamReadHelper#unmarshalProperties(PropertiesParams)} on
    * the properties associated with \texttt{par}.
    * @param base the base class to be used.
    * @param cc the call center model.
    * @param k the index of the call type concerning the condition.
    * @param par the parameters for the custom condition.
    * @return an instance representing the custom object.
    */
   public static <T> T createCustom (Class<T> base, CallCenter cc, int k, Named par) {
      Class<?> cls;
      try {
         cls = Class.forName (par.getName ());
      }
      catch (ClassNotFoundException cne) {
         throw new IllegalArgumentException
         ("Cannot find class " + par.getName ());
      }
      if (!base.isAssignableFrom (cls))
         throw new IllegalArgumentException
         ("The given class, " + cls.getName () + ", does not implement the " + base.getName () + " interface");
      
      Map<String, Object> props = ParamReadHelper.unmarshalProperties (par.getProperties ());
      Class<? extends T> condClass = cls.asSubclass (base);
      
      Constructor<? extends T> ctor;
      for (int i = 0; i < 8; i++) {
         Class<?>[] types;
         Object[] args;
         switch (i) {
         case 0:
            types = new Class[] { CallCenter.class, int.class, Map.class };
            args = new Object[] { cc, k, props };
            break;
         case 1:
            types = new Class[] { CallCenter.class, int.class };
            args = new Object[] { cc, k };
            break;
         case 2:
            types = new Class[] { CallCenter.class, Map.class };
            args = new Object[] { cc, props };
            break;
         case 3:
            types = new Class[] { int.class, Map.class };
            args = new Object[] { k, props };
            break;
         case 4:
            types = new Class[] { CallCenter.class };
            args = new Object[] { cc };
            break;
         case 5:
            types = new Class[] { int.class };
            args = new Object[] { k };
            break;
         case 6:
            types = new Class[] { Map.class };
            args = new Object[] { props };
            break;
         case 7:
            types = new Class[0];
            args = new Object[0];
            break;
         default:
            throw new AssertionError();
         }
         
         try {
            ctor = condClass.getConstructor (types);
            return ctor.newInstance (args);
         }
         catch (NoSuchMethodException nme) {
            // Will pass to the next constructor
         }
         catch (InstantiationException e) {
            IllegalArgumentException iae = new IllegalArgumentException
            ("Instantiation exception while constructing object");
            iae.initCause (e);
            throw iae;
         }
         catch (IllegalAccessException e) {
            IllegalArgumentException iae = new IllegalArgumentException
            ("Illegal access while constructing object");
            iae.initCause (e);
            throw iae;
         }
         catch (InvocationTargetException e) {
            IllegalArgumentException iae = new IllegalArgumentException
            ("The constructor for the object " + condClass.getName () + " has thrown an exception");
            iae.initCause (e.getCause ());
            throw iae;
         }
      }
      throw new IllegalArgumentException
      ("No suitable constructor in class " + condClass.getName ());
   }
   
   /**
    * Returns \texttt{true} if and only
    * if  a condition comparing
    * \texttt{v1} and \texttt{v2} based on
    * relationship \texttt{rel} applies.
    */
   public static boolean applies (double v1, double v2, Relationship rel) {
      switch (rel) {
      case EQUAL:
         return v1 == v2;
      case GREATER:
         return v1 > v2;
      case GREATEROREQUAL:
         return v1 >= v2;
      case SMALLER:
         return v1 < v2;
      case SMALLEROREQUAL:
         return v1 <= v2;
      }
      return false;
   }
}
