package umontreal.iro.lecuyer.xmlconfig;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Represents a parameter object containing an array of parameters. When a
 * creater method constructs this parameter object, it needs to indicate the
 * class of the components in the array. The component class can be any
 * non-array primitive type or class supported by {@link StringConvert}. It is
 * recommended to use this class as a temporary placeholder. A creater method
 * constructs and returns an instance whereas an adder method receives the
 * configured instance and extracts the array, without having to keep the
 * parameter object.
 * 
 * In the XML file, two formats are supported for the array parameter. A list of
 * comma-separated strings that will be converted to objects of the component
 * class can be used as contents. Alternatively, the array can be encoded using
 * a \texttt{row} nested element for each element. The \texttt{row} element
 * supports the \texttt{repeat} attribute allowing the specification of the
 * number of times an element must be repeated in the array.
 * 
 * Fo rexample,
 * \begin{verbatim}
 * 
 *    <doublearray>2.3, 3.2, 3.2, 5.1</doublearray>
 * \end{verbatim}
 * is equivalent to
 * \begin{verbatim}
 * 
 *    <doublearray>
 *       <row>2.3</row>
 *       <row repeat="2">3.2</row>
 *       <row>5.1</row>
 *    </doublearray>
 * \end{verbatim}
 * and can be converted into an array containing
 * \texttt{2.3}, \texttt{3.2}, \texttt{3.2}, and \texttt{5.1}.
 * 
 * Arrays can also be specified externally
 * by using the \texttt{CSV} or \texttt{DB}
 * sub-elements.  See {@link ParamWithSourceArray}
 * for more information.
 * The constructed source subset is read
 * row by row to obtain an array of objects.
 *
 * This class provides methods to get the extracted array as well as methods for
 * the primitive types.
 */
public class ArrayParam extends ParamWithSourceArray implements Cloneable, StorableParam {
   private Class<?> componentClass;
   private Object[] values;
   private List<RowParam> rows;
   private String elementName;
   
   public ArrayParam (Class<?> componentClass) {
      this (componentClass, "row");
   }

   /**
    * Constructs a new array parameter with components of class
    * \texttt{componentClass}.
    * 
    * @param componentClass
    *           the component class.
    * @exception NullPointerException
    *               if \texttt{componentClass} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{componentClass} is an array class.
    */
   public ArrayParam (Class<?> componentClass, String elementName) {
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
      this.elementName = elementName;
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
    * Returns the associated array of objects. The returned array can safely be
    * casted to an array of the component class.
    * 
    * @return the associated array of objects.
    */
   public Object[] getValues () {
      if (values != null)
         return values;
      else if (rows != null) {
         int count = 0;
         for (final RowParam rp : rows)
            count += rp.repeat;
         final Object[] vals = createArray (count);
         int j = 0;
         for (final RowParam rp : rows) {
            final Object val = rp.object;
            for (int r = 0; r < rp.repeat; r++)
               vals[j++] = val;
         }
         return vals;
      }
      else if (getDataMatrix() != null) {
         initSourceArray();
         final SourceArray2D matrix = getSourceSubset ();
         final int rows1 = matrix.rows ();
         int size = 0;
         for (int r = 0; r < rows1; r++)
            size += matrix.columns (r);
         final Object[] res = createArray (size);
         int i = 0;
         for (int r = 0; r < rows1; r++) {
            final int columns = matrix.columns (r);
            for (int c = 0; c < columns; c++)
               try {
                  res[i++] = matrix.get (componentClass, r, c);
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
         return createArray (0);
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{float} and
    * returns the resulting array.
    * 
    * @return the array of single-precision values.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public float[] getFloatValues () {
      if (componentClass != float.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final float[] v = new float[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? Float.NaN : val[i].floatValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{double} and
    * returns the resulting array.
    * 
    * @return the array of double-precision values.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public double[] getDoubleValues () {
      if (componentClass != double.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final double[] v = new double[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? Double.NaN : val[i].doubleValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{int} and
    * returns the resulting array.
    * 
    * @return the array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public int[] getIntValues () {
      if (componentClass != int.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final int[] v = new int[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? 0 : val[i].intValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{short} and
    * returns the resulting array.
    * 
    * @return the array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public short[] getShortValues () {
      if (componentClass != short.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final short[] v = new short[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? 0 : val[i].shortValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{long} and
    * returns the resulting array.
    * 
    * @return the array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public long[] getLongValues () {
      if (componentClass != long.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final long[] v = new long[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? 0 : val[i].longValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{byte} and
    * returns the resulting array.
    * 
    * @return the array of integers.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public byte[] getByteValues () {
      if (componentClass != byte.class
            && !Number.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not numeric");
      final Number[] val = (Number[]) getValues ();
      final byte[] v = new byte[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? 0 : val[i].byteValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{char} and
    * returns the resulting array.
    * 
    * @return the array of characters.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public char[] getCharValues () {
      if (componentClass != char.class
            && !Character.class.isAssignableFrom (componentClass))
         throw new IllegalStateException (
               "The component class is not character");
      final Character[] val = (Character[]) getValues ();
      final char[] v = new char[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? 0 : val[i].charValue ();
      return v;
   }

   /**
    * Converts the objects returned by {@link #getValues} to \texttt{boolean}
    * and returns the resulting array.
    * 
    * @return the array of booleans.
    * @exception IllegalStateException
    *               if the component class is incompatible.
    */
   public boolean[] getBooleanValues () {
      if (componentClass != boolean.class
            && !Boolean.class.isAssignableFrom (componentClass))
         throw new IllegalStateException ("The component class is not boolean");
      final Boolean[] val = (Boolean[]) getValues ();
      final boolean[] v = new boolean[val.length];
      for (int i = 0; i < v.length; i++)
         v[i] = val[i] == null ? false : val[i].booleanValue ();
      return v;
   }

   /**
    * Sets the array elements to \texttt{v}. If the component type of \texttt{v}
    * is not assignable to the component class, this method throws an
    * {@link IllegalArgumentException}.
    * 
    * @param v
    *           the values.
    * @exception NullPointerException
    *               if \texttt{v} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the component class is incompatible with \texttt{v}.
    */
   public void setValues (Object[] v) {
      if (v == null)
         throw new NullPointerException ("Null arrays not supported");
      final Class<?> compClass = v.getClass ().getComponentType ();
      if (!componentClass.isAssignableFrom (compClass))
         throw new IllegalArgumentException (
               "Incompatible array component type");
      rows = null;
      values = v;
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
   public void nestedText (ParamReader reader, String str) {
      if (rows != null || getDataMatrix() != null)
         throw new ParamReadException ("Values are already specified");
      final String[] vals = StringConvert.getArrayElements (str);
      final Object[] v = createArray (vals.length);
      for (int i = 0; i < v.length; i++)
         try {
            v[i] = StringConvert.fromString (reader.baseURI,
                  reader.getClassFinder (), componentClass, vals[i]);
         }
         catch (final UnsupportedConversionException uce) {
            final ParamReadException pre = new ParamReadException (
                  "Unsupported conversion");
            pre.initCause (uce);
            throw pre;
         }
         catch (final NameConflictException uce) {
            final ParamReadException pre = new ParamReadException (
                  "Class name conflict");
            pre.initCause (uce);
            throw pre;
         }

      setValues (v);
   }

   /**
    * For internal use only.
    */
   public void defaultNestedElement (ParamReader reader, Element cel) {
      if (values != null || getDataMatrix() != null)
         throw new ParamReadException ("Values are already specified");
      String name = cel.getLocalName ();
      if (name == null)
         name = cel.getTagName ();
      if (!name.equals (elementName))
         throw new IllegalArgumentException ("Unsupported element with name "
               + cel.getLocalName ());
      final Param rp = makeRow ();
      reader.processElement (cel, rp);
   }
   
   protected Param makeRow () {
      if (values != null)
         throw new ParamReadException ("Array text element already specified");
      if (rows == null)
         rows = new ArrayList<RowParam> ();
      final RowParam rp = new RowParam (componentClass);
      rows.add (rp);
      return rp;
   }

   /**
    * For internal use only.
    */
   public final static class RowParam implements Param, Cloneable {
      Class<?> componentClass;
      Object object;
      int repeat = 1;

      public RowParam (Class<?> componentClass) {
         this.componentClass = componentClass;
      }

      public void setRepeat (int r) {
         if (r <= 0)
            throw new ParamReadException ("Invalid repeat count for row: " + r);
         repeat = r;
      }

      public void nestedText (ParamReader reader, String str) {
         try {
            object = StringConvert.fromString (reader.baseURI,
                  reader.getClassFinder (), componentClass, str);
         }
         catch (final UnsupportedConversionException uce) {
            final ParamReadException pre = new ParamReadException (
                  "Unsupported conversion");
            pre.initCause (uce);
            throw pre;
         }
         catch (final NameConflictException uce) {
            final ParamReadException pre = new ParamReadException (
                  "Class name conflict");
            pre.initCause (uce);
            throw pre;
         }
      }

      @Override
      public RowParam clone() {
         RowParam cpy;
         try {
            cpy = (RowParam)super.clone ();
         }
         catch (final CloneNotSupportedException cne) {
            throw new InternalError
            ("Clone not supported for a class implementing Cloneable");
         }
         return cpy;
      }
   }

   Object[] createArray (int size) {
      // This method creates an array of objects of length size.
      // If we use new Object[size], the user will not be
      // able to cast the array to its component class.
      return (Object[]) Array.newInstance (componentClass, size);
   }

   @Override
   public Element toElement (ClassFinder finder, Node parent, String elementName1, int spc) {
      final Element el = super.toElement (finder, parent, elementName1, spc);
      if (values != null || rows != null)
         DOMUtils.addArrayToElement (el, getValues(), null, spc);
      return el;
   }

   @Override
   public ArrayParam clone() {
      final ArrayParam cpy = (ArrayParam)super.clone ();
      if (values != null)
         cpy.values = values.clone ();
      if (rows != null) {
         cpy.rows = new ArrayList<RowParam>();
         for (final RowParam rp : rows)
            cpy.rows.add (rp.clone ());
      }
      return cpy;
   }
}
