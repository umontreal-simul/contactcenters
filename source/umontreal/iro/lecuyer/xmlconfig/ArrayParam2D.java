package umontreal.iro.lecuyer.xmlconfig;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.ssj.util.ClassFinder;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a parameter object containing a 2D array or a matrix of
 * parameters. When a creater method constructs such a parameter object, it
 * needs to specify a component class for the elements in the 2D array. The
 * component class can be any non-array primitive type or class supported by
 * {@link StringConvert}. It is recommended to use this class as a temporary
 * placeholder. A creater method constructs and returns an instance whereas an
 * adder method receives the configured instance and extracts the array, without
 * having to keep the parameter object.
 * 
 * In the XML file, two formats are allowed to represent this parameter. The 2D
 * array can be specified as a list of arrays or one \texttt{row} element can be
 * used for each row of the 2D array. As with {@link ArrayParam}, the
 * \texttt{row} elements supports the \texttt{repeat} attribute.
 * 
 * For example,
 * \begin{verbatim}
 * 
 *    <matrix>
 *       {1, 2,3},
 *       {4, 5},
 *       {4, 5},
 *       {4, 5},
 *       {4, 5},
 *       {9, 11,13}
 *    </matrix>
 * \end{verbatim}
 * is equivalent to
 * \begin{verbatim}
 * 
 *    <matrix>
 *       <row>1,2,3</row>
 *       <row repeat="4">4, 5</row>
 *       <row> 9 , 11, 13</row>
 *    </matrix>
 * \end{verbatim}
 *
 * Matrices can also be specified externally
 * by using the \texttt{CSV} or \texttt{DB}
 * sub-elements.  See {@link ParamWithSourceArray}
 * for more information.
 */
public class ArrayParam2D extends ParamWithSourceArray implements Cloneable, StorableParam {
   private Class<?> componentClass;
   private Object[][] values;
   private List<MatrixRowParam> rows;

   /**
    * Constructs a new 2D array parameter with components of class
    * \texttt{componentClass}.
    * 
    * @param componentClass
    *           the component class.
    * @exception NullPointerException
    *               if \texttt{componentClass} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{componentClass} is an array class.
    */
   public ArrayParam2D (Class<?> componentClass) {
      if (componentClass == null)
         throw new NullPointerException ("Component class must not be null");
      if (componentClass.isArray ())
         throw new IllegalArgumentException (
               "Array not allowd as component class");
      if (componentClass.isPrimitive ()) {
         // If we allow primitive component class, the array
         // will not always be of class Object[].
         // It will be necessary for getValues to return an Object, which
         // will be confusing.
         if (componentClass == byte.class)
            this.componentClass = Byte.class;
         else if (componentClass == short.class)
            this.componentClass = Short.class;
         else if (componentClass == int.class)
            this.componentClass = Integer.class;
         else if (componentClass == long.class)
            this.componentClass = Long.class;
         else if (componentClass == boolean.class)
            this.componentClass = Boolean.class;
         else if (componentClass == char.class)
            this.componentClass = Character.class;
         else if (componentClass == float.class)
            this.componentClass = Float.class;
         else if (componentClass == double.class)
            this.componentClass = Double.class;
         else
            throw new IllegalStateException ("Unknown primitive type: "
                  + componentClass.getName ());
      }
      else
         this.componentClass = componentClass;
   }

   /**
    * Returns the class of the components in this array parameter.
    * 
    * @return the component class.
    */
   public Class<?> getComponentClass () {
      return componentClass;
   }
   
   /**
    * Returns the values in the 2D array represented by this parameter object.
    * The returned 2D array can safely be casted to a 2D array of the component
    * class.
    * 
    * @return the represented 2D array.
    * @exception ParamReadException
    *               if no 2D array was specified.
    */
   public Object[][] getValues () {
      if (values != null)
         return values;
      else if (rows != null) {
         int size = 0;
         for (final MatrixRowParam rp : rows)
            size += rp.getRepeat ();
         final Object[][] vals = createMatrix (size);
         int i = 0;
         for (final MatrixRowParam p : rows) {
            final Object[] v = p.getValues ();
            for (int r = 0; r < p.getRepeat (); r++) {
               vals[i] = v.clone ();
               ++i;
            }
         }
         return vals;
      }
      else if (getDataMatrix() != null) {
         initSourceArray();
         final SourceArray2D matrix = getSourceSubset ();
         final int rows1 = matrix.rows ();
         final Object[][] res = createMatrix (rows1);
         for (int r = 0; r < rows1; r++) {
            final int columns = matrix.columns (r);
            res[r] = createArray (columns);
            for (int c = 0; c < columns; c++)
               try {
                  res[r][c] = matrix.get (componentClass, r, c);
               }
               catch (final UnsupportedConversionException uce) {
                  final IllegalArgumentException iae = new IllegalArgumentException
                  ("Cannot convert element (" + r + ", " + c +") of the data matrix");
                  iae.initCause (uce);
                  throw iae;
               }
         }
         disposeSourceArray();
         return res;
      }
      else
         return createMatrix (0);
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{float} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of single-precision values.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public float[][] getFloatValues () {
      if (componentClass != double.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[][] val = (Number[][]) getValues ();
      final float[][] v = new float[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new float[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? Float.NaN : val[i][j].floatValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{double} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of double-precision values.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public double[][] getDoubleValues () {
      if (componentClass != double.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[][] val = (Number[][]) getValues ();
      final double[][] v = new double[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new double[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? Double.NaN : val[i][j].doubleValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{int} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public int[][] getIntValues () {
      if (componentClass != int.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not integer");
      final Number[][] val = (Number[][]) getValues ();
      final int[][] v = new int[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new int[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? 0 : val[i][j].intValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{short} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public short[][] getShortValues () {
      if (componentClass != short.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not integer");
      final Number[][] val = (Number[][]) getValues ();
      final short[][] v = new short[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new short[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? 0 : val[i][j].shortValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{long} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public long[][] getLongValues () {
      if (componentClass != long.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not integer");
      final Number[][] val = (Number[][]) getValues ();
      final long[][] v = new long[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new long[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? 0 : val[i][j].longValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{byte} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public byte[][] getByteValues () {
      if (componentClass != byte.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not integer");
      final Number[][] val = (Number[][]) getValues ();
      final byte[][] v = new byte[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new byte[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? 0 : val[i][j].byteValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{char} and
    * returns the resulting 2D array.
    * 
    * @return the 2D array of characters.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public char[][] getCharValues () {
      if (componentClass != char.class
            && !Character.class.isAssignableFrom (componentClass))
         throw new IllegalStateException (
               "The component class is not character");
      final Character[][] val = (Character[][]) getValues ();
      final char[][] v = new char[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new char[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? 0 : val[i][j].charValue ();
      }
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{boolean}
    * and returns the resulting 2D array.
    * 
    * @return the 2D array of booleans.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public boolean[][] getBooleanValues () {
      if (componentClass != boolean.class
            && !Boolean.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not boolean");
      final Boolean[][] val = (Boolean[][]) getValues ();
      final boolean[][] v = new boolean[val.length][];
      for (int i = 0; i < v.length; i++) {
         if (val[i] == null)
            continue;
         v[i] = new boolean[val[i].length];
         for (int j = 0; j < v[i].length; j++)
            v[i][j] = val[i][j] == null ? false : val[i][j].booleanValue ();
      }
      return v;
   }

   /**
    * Sets the 2D array to \texttt{m}. If the component type of \texttt{m} is
    * not assignable to the component class, this method throws an
    * {@link IllegalArgumentException}.
    * 
    * @param m
    *           the new 2D array.
    * @exception NullPointerException
    *               if \texttt{m} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the component class is incompatible with \texttt{m}.
    */
   public void setValues (Object[][] m) {
      final Class<?> compClass = m.getClass ().getComponentType ()
            .getComponentType ();
      if (!componentClass.isAssignableFrom (compClass))
         throw new IllegalArgumentException (
               "Incompatible array component type");
      rows = null;
      values = m;
   }

   /**
    * For internal use only.
    */
   public boolean isAttributeSupported (String a) {
      if (a.equals ("values"))
         return false;
      return true;
   }

   /**
    * For internal use only.
    */
   public MatrixRowParam createRow () {
      if (values != null || getDataMatrix() != null)
         throw new ParamReadException (
               "Cannot add a row element to this matrix, array elements already specified");
      if (rows == null)
         rows = new ArrayList<MatrixRowParam> ();
      final MatrixRowParam p = new MatrixRowParam (componentClass);
      rows.add (p);
      values = null;
      return p;
   }

   /**
    * For internal use only.
    */
   public void nestedText (ParamReader reader, String str) {
      if (rows != null || getDataMatrix() != null)
         throw new ParamReadException ("Values already specified");

      final String[] vals = StringConvert.getArrayElements (str);
      final Object[][] v = createMatrix (vals.length);
      final ArrayParam par = new ArrayParam (componentClass);
      for (int i = 0; i < vals.length; i++) {
         par.nestedText (reader, vals[i]);
         v[i] = par.getValues ();
      }

      setValues (v);
   }

   /**
    * For internal use only.
    */
   public final static class MatrixRowParam extends ArrayParam {
      private int repeat = 1;

      MatrixRowParam (Class<?> componentClass) {
         super (componentClass);
      }

      public int getRepeat () {
         return repeat;
      }

      public void setRepeat (int r) {
         if (r <= 0)
            throw new ParamReadException ("Invalid repeat count for row: " + r);
         repeat = r;
      }
      
      @Override
      public MatrixRowParam clone() {
         return (MatrixRowParam)super.clone ();
      }
   }

   Object[][] createMatrix (int size) {
      // This method creates an array of objects of length size.
      // If we use new Object[size], the user will not be
      // able to cast the array to its component class.
      return (Object[][]) Array.newInstance (componentClass, new int[] { size,
            0 });
   }
   
   Object[] createArray (int size) {
      // This method creates an array of objects of length size.
      // If we use new Object[size], the user will not be
      // able to cast the array to its component class.
      return (Object[]) Array.newInstance (componentClass, size);
   }

   @Override
   public Element toElement (ClassFinder finder, Node parent, String elementName, int spc) {
      final Element el = super.toElement (finder, parent, elementName, spc);
      if (values != null || rows != null)
         DOMUtils.addArray2DToElement (el, getValues(), null, spc);
      return el;
   }

   @Override
   public ArrayParam2D clone() {
      final ArrayParam2D cpy = (ArrayParam2D)super.clone ();
      if (values != null)
         cpy.values = ArrayUtil.deepClone (values, false);
      if (rows != null) {
         cpy.rows = new ArrayList<MatrixRowParam>();
         for (final MatrixRowParam rp : rows)
            cpy.rows.add (rp.clone ());
      }
      return cpy;
   }
}
