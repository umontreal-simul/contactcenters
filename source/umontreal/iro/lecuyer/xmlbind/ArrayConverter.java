package umontreal.iro.lecuyer.xmlbind;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.xmlbind.params.BooleanArray;
import umontreal.iro.lecuyer.xmlbind.params.DoubleArray;
import umontreal.iro.lecuyer.xmlbind.params.DurationArray;
import umontreal.iro.lecuyer.xmlbind.params.IntArray;
import umontreal.iro.lecuyer.xmlbind.params.NonNegativeDurationArray;

/**
 * Provides helper methods to convert 2D arrays read by JAXB to the Java's more
 * natural representation of 2D arrays, namely arrays of arrays. For example,
 * {@link DoubleArray} can be used to read a 2D array in an XML file. It defines
 * a {@link DoubleArray#getRows()} method returning a list of
 * \texttt{DoubleArray.Row} instances
 * representing rows. Each row is defined by a list of values,
 * and a repeat count. This class defines the
 * {@link #unmarshalArray(DoubleArray)} method converting such an array object
 * to a more natural \texttt{double[][]} 2D array. It also provides the
 * {@link #marshalArray(double[][])} which does the inverse operation. Other
 * similar methods are provided for arrays of integers, and arrays of durations.
 */
public class ArrayConverter {
   /**
    * Converts the list of arrays
    * \texttt{rows} into a Java 2D array. JAXB objects representing
    * 2D arrays provide a method returning a list of elements, each element
    * being an object of an element-dependent class
    * with no common base class.
    * This method accepts a list
    * of such objects, and uses Reflection as follows to get a more natural 2D
    * array. For each element of the given list, it looks for
    * \texttt{getRepeat} and \texttt{getValue} methods to obtain the number of
    * times the inner array must be repeated in the 2D array, and the values
    * composing the inner array, respectively.
    * This method verifies that \texttt{getRepeat} returns an
    * \texttt{int} or an {@link Integer}, and that
    * \texttt{getValue} returns a {@link List}.
    * It is assumed that the list returned by \texttt{getValue}
    * contains instances of the class represented by
    * \texttt{componentClass}.
    *  This method is intended to be
    * called by front-end methods
    * public such as {@link #unmarshalArray(DoubleArray)}.
    * 
    * @param <T>
    *           the type of components in the 2D array.
    * @param componentClass
    *           the component class of the 2D array.
    * @param rows
    *           the list of row elements.
    * @return the resulting 2D array.
    */
   @SuppressWarnings ("unchecked")
   protected static <T> T[][] unmarshalArray (Class<T> componentClass,
         List<?> rows) {
      final List<T[]> resList = new ArrayList<T[]> ();
      Class<?> rowClass = null;
      Method getRepeat = null;
      Method getValue = null;
      final Logger logger = Logger.getLogger ("umontreal.iro.lecuyer.xmlbind");
      if (rows != null)
         for (final Object row : rows) {
            if (row == null)
               continue;
            if (rowClass == null || !rowClass.isInstance (row)) {
               rowClass = row.getClass ();
               try {
                  getRepeat = rowClass.getMethod ("getRepeat");
                  if (!Integer.class.isAssignableFrom (getRepeat
                        .getReturnType ()) &&
                        getRepeat.getReturnType () != int.class) {
                     logger
                           .info ("The class "
                                 + rowClass.getName ()
                                 + " contains a getRepeat() method not returning a number");
                     getRepeat = null;
                  }
               }
               catch (final NoSuchMethodException nme) {
                  logger.info ("No getRepeat() method in class "
                        + rowClass.getName ());
                  getRepeat = null;
               }
               try {
                  getValue = rowClass.getMethod ("getValue");
                  if (!List.class.isAssignableFrom (getValue.getReturnType ())) {
                     logger
                           .warning ("The class "
                                 + rowClass.getName ()
                                 + " defines a getValue() method not returning a java.util.List");
                     getValue = null;
                  }
               }
               catch (final NoSuchMethodException nme) {
                  getValue = null;
                  logger.warning ("The class " + rowClass.getName ()
                        + " does not have a getValue() method");
               }
            }
            int rep;
            // If we could tell XJC to have a derived class extend a base class
            // or
            // implement interfaces, using reflection would not be necessary.
            try {
               if (getRepeat == null)
                  rep = 1;
               else {
                  final Number ret = (Number) getRepeat.invoke (row);
                  if (ret == null) {
                     logger.warning ("The getRepeat() method in class "
                           + rowClass.getName () + " returned null");
                     rep = 1;
                  }
                  else
                     rep = ret.intValue ();
               }
            }
            catch (final IllegalAccessException iae) {
               logger.log (Level.WARNING,
                     "Cannot access the getRepeat() method in "
                           + rowClass.getName (), iae);
               rep = 1;
            }
            catch (final InvocationTargetException ite) {
               logger.log (Level.WARNING,
                     "An exception occurred during call to getRepeat() in class "
                           + rowClass.getName (), ite.getCause ());
               rep = 1;
            }
            List<?> values;
            try {
               if (getValue == null)
                  values = null;
               else {
                  values = (List<?>) getValue.invoke (row);
                  if (values == null)
                     logger.warning ("The getValue() method in class "
                           + rowClass.getName () + " returned null");
               }
            }
            catch (final IllegalAccessException iae) {
               values = null;
               logger.log (Level.WARNING,
                     "Cannot access the getValue() method in "
                           + rowClass.getName (), iae);
            }
            catch (final InvocationTargetException ite) {
               values = null;
               logger.log (Level.WARNING,
                     "An exception occurred during call to getValue() in class "
                           + rowClass.getName (), ite.getCause ());
            }

            T[] array;
            if (values == null || values.isEmpty ())
               array = (T[]) Array.newInstance (componentClass, 0);
            else
               array = values.toArray ((T[]) Array.newInstance (componentClass,
                     values.size ()));
            resList.add (array);
            for (int r = 1; r < rep; r++)
               resList.add (array.clone ());
         }
      return resList.toArray ((T[][]) Array.newInstance (componentClass,
            new int[] { resList.size (), 0 }));
   }

   /**
    * Unmarshals a 2D array JAXB object into a Java 2D array
    * of double-precision values.
    * 
    * @param array
    *           the 2D array represented as a JAXB object.
    * @return the Java 2D array.
    */
   public static double[][] unmarshalArray (DoubleArray array) {
      final Double[][] resWrap = unmarshalArray (Double.class, array.getRows ());
      final double[][] res = new double[resWrap.length][];
      for (int r = 0; r < res.length; r++) {
         res[r] = new double[resWrap[r].length];
         for (int c = 0; c < resWrap[r].length; c++)
            res[r][c] = resWrap[r][c];
      }
      return res;
   }
   
   /**
    * Unmarshals a 2D array JAXB object into a Java 2D array
    * of boolean values.
    * 
    * @param array
    *           the 2D array represented as a JAXB object.
    * @return the Java 2D array.
    */
   public static boolean[][] unmarshalArray (BooleanArray array) {
      final Boolean[][] resWrap = unmarshalArray (Boolean.class, array.getRows ());
      final boolean[][] res = new boolean[resWrap.length][];
      for (int r = 0; r < res.length; r++) {
         res[r] = new boolean[resWrap[r].length];
         for (int c = 0; c < resWrap[r].length; c++)
            res[r][c] = resWrap[r][c];
      }
      return res;
   }

   /**
    * Unmarshals a 2D array JAXB object into a Java 2D array
    * of integers.
    * 
    * @param array
    *           the 2D array represented as a JAXB object.
    * @return the Java 2D array.
    */
   public static int[][] unmarshalArray (IntArray array) {
      final Integer[][] resWrap = unmarshalArray (Integer.class, array.getRows ());
      final int[][] res = new int[resWrap.length][];
      for (int r = 0; r < res.length; r++) {
         res[r] = new int[resWrap[r].length];
         for (int c = 0; c < resWrap[r].length; c++)
            res[r][c] = resWrap[r][c];
      }
      return res;
   }

   /**
    * Unmarshals a 2D array JAXB object into a Java 2D array
    * of time durations.
    * Note that time durations can be converted to
    * times in milliseconds using {@link Duration#getTimeInMillis(java.util.Date)}.
    * 
    * @param array
    *           the 2D array represented as a JAXB object.
    * @return the Java 2D array.
    */
   public static Duration[][] unmarshalArray (DurationArray array) {
      return unmarshalArray (Duration.class, array.getRows ());
   }

   /**
    * Similar to {@link #unmarshalArray(DurationArray)}
    * for non-negative durations.
    * 
    * @param array
    *           the 2D array represented as a JAXB object.
    * @return the Java 2D array.
    */
   public static Duration[][] unmarshalArray (NonNegativeDurationArray array) {
      return unmarshalArray (Duration.class, array.getRows ());
   }

   /**
    * Represents a factory object for creating rows when marshalling a 2D array
    * to JAXB object.
    * 
    * @param <T>
    *           the type of components in the 2D array.
    */
   protected static interface RowFactory<T> {
      /**
       * Constructs and returns a new row from the list of values.
       * 
       * @param values
       *           the list of values.
       * @return the new row object.
       */
      public Object createRow (List<T> values);
   }

   /**
    * Uses the given row factory to convert the specified Java 2D array into a
    * list intended to be associated with the JAXB representation of a 2D array.
    * For each array in \texttt{array}, this method
    * creates a list and passes it to the row factory
    * \texttt{factory}.
    * The row objects corresponding to the inner arrays
    * are then regrouped into a list which
    * is returned. 
    * This method is intended to be used by
    * {@link #marshalArray(double[][])}.
    * 
    * @param <T>
    *           the class of the components in the array.
    * @param factory
    *           the row factory.
    * @param array
    *           the Java 2D array.
    * @return the list to be used in the JAXB representation of the array.
    */
   protected static <T> List<?> marshalArray (RowFactory<T> factory, T[][] array) {
      if (array == null || array.length == 0)
         return new ArrayList<Object> ();
      final List<Object> res = new ArrayList<Object> ();
      for (final T[] element : array) {
         final List<T> values = Arrays.asList (element);
         res.add (factory.createRow (values));
      }
      return res;
   }

   private static final RowFactory<Boolean> booleanRowFactory = new RowFactory<Boolean> () {
      public Object createRow (List<Boolean> values) {
         BooleanArray.Row row = new BooleanArray.Row ();
         row.getValue ().addAll (values);
         return row;
      }
   };

   /**
    * Marshals a Java 2D array
    * of boolean values into an object that can be serialized to XML by
    * JAXB.
    * 
    * @param array
    *           the input Java 2D array.
    * @return the output object for JAXB.
    */
   @SuppressWarnings ("unchecked")
   public static BooleanArray marshalArray (boolean[][] array) {
      final Boolean[][] arrayWrap = new Boolean[array.length][];
      for (int r = 0; r < arrayWrap.length; r++) {
         arrayWrap[r] = new Boolean[array[r].length];
         for (int c = 0; c < arrayWrap[r].length; c++)
            arrayWrap[r][c] = array[r][c];
      }
      final List list = marshalArray (booleanRowFactory, arrayWrap);
      final BooleanArray res = new BooleanArray ();
      res.getRows ().addAll (list);
      return res;
   }
   
   private static final RowFactory<Double> doubleRowFactory = new RowFactory<Double> () {
      public Object createRow (List<Double> values) {
         DoubleArray.Row row = new DoubleArray.Row ();
         row.getValue ().addAll (values);
         return row;
      }
   };

   /**
    * Marshals a Java 2D array
    * of double-precision values into an object
    * that can be serialized to XML by
    * JAXB.
    * 
    * @param array
    *           the input Java 2D array.
    * @return the output object for JAXB.
    */
   @SuppressWarnings ("unchecked")
   public static DoubleArray marshalArray (double[][] array) {
      final Double[][] arrayWrap = new Double[array.length][];
      for (int r = 0; r < arrayWrap.length; r++) {
         arrayWrap[r] = new Double[array[r].length];
         for (int c = 0; c < arrayWrap[r].length; c++)
            arrayWrap[r][c] = array[r][c];
      }
      final List list = marshalArray (doubleRowFactory, arrayWrap);
      final DoubleArray res = new DoubleArray ();
      res.getRows ().addAll (list);
      return res;
   }

   private static final RowFactory<Integer> intRowFactory = new RowFactory<Integer> () {
      public Object createRow (List<Integer> values) {
         IntArray.Row row = new IntArray.Row ();
         row.getValue ().addAll (values);
         return row;
      }
   };

   /**
    * Marshals a Java 2D array
    * of integers into an object that can be serialized to XML by
    * JAXB.
    * 
    * @param array
    *           the input Java 2D array.
    * @return the output object for JAXB.
    */
   @SuppressWarnings ("unchecked")
   public static IntArray marshalArray (int[][] array) {
      final Integer[][] arrayWrap = new Integer[array.length][];
      for (int r = 0; r < arrayWrap.length; r++) {
         arrayWrap[r] = new Integer[array[r].length];
         for (int c = 0; c < arrayWrap[r].length; c++)
            arrayWrap[r][c] = array[r][c];
      }
      final List list = marshalArray (intRowFactory, arrayWrap);
      final IntArray res = new IntArray ();
      res.getRows ().addAll (list);
      return res;
   }

   private static final RowFactory<Duration> durationRowFactory = new RowFactory<Duration> () {
      public Object createRow (List<Duration> values) {
         DurationArray.Row row = new DurationArray.Row ();
         row.getValue ().addAll (values);
         return row;
      }
   };

   /**
    * Marshals a Java 2D array
    * of durations into an object that can be serialized to XML by
    * JAXB.
    * Duration objects can be created using
    * {@link DatatypeFactory}.
    * 
    * @param array
    *           the input Java 2D array.
    * @return the output object for JAXB.
    */
   @SuppressWarnings ("unchecked")
   public static DurationArray marshalArray (Duration[][] array) {
      final List list = marshalArray (durationRowFactory, array);
      final DurationArray res = new DurationArray ();
      res.getRows ().addAll (list);
      return res;
   }

   private static final RowFactory<Duration> nonNegativeDurationRowFactory = new RowFactory<Duration> () {
      public Object createRow (List<Duration> values) {
         NonNegativeDurationArray.Row row = new NonNegativeDurationArray.Row ();
         row.getValue ().addAll (values);
         return row;
      }
   };

   /**
    * Similar to {@link #marshalArray(Duration[][])}, for
    * non-negative durations only.
    * 
    * @param array
    *           the input Java 2D array.
    * @return the output object for JAXB.
    */
   @SuppressWarnings ("unchecked")
   public static NonNegativeDurationArray marshalArrayNonNegative (
         Duration[][] array) {
      final List list = marshalArray (nonNegativeDurationRowFactory, array);
      final NonNegativeDurationArray res = new NonNegativeDurationArray ();
      res.getRows ().addAll (list);
      return res;
   }
   
   /**
    * Converts a list containing
    * double-precision values wrapped into
    * objects of class {@link Double} to an array
    * of double-precision values.
    * @param list the list of wrapped values.
    * @return the array of values.
    */
   public static double[] unmarshalArray (List<Double> list) {
      final double[] res = new double[list.size ()];
      int idx = 0;
      for (final Double d : list)
         res[idx++] = d;
      return res;
   }
   
   /**
    * Converts an array of double-precision values
    * to a list containing values wrapped into objects
    * of class {@link Double}.
    * @param array the array of values.
    * @return the list of values.
    */
   public static List<Double> marshalArray (double[] array) {
      final List<Double> list = new ArrayList<Double>();
      for (final double d : array)
         list.add (d);
      return list;
   }
}
