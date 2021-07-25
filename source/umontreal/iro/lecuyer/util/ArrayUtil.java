package umontreal.iro.lecuyer.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides static utility methods to resize and test arrays.
 */
public class ArrayUtil {
	private ArrayUtil() {
	}

	private static Object resizeArrayObject(Object oldArray, int newLength) {
		final Class<?> cl = oldArray.getClass();
		if (!cl.isArray())
			throw new IllegalArgumentException(
					"The given argument is not an array");
		final int oldLength = Array.getLength(oldArray);
		if (oldLength == newLength)
			// No resize needed
			return oldArray;
		final Object newArray = Array.newInstance(cl.getComponentType(),
				newLength);
		System.arraycopy(oldArray, 0, newArray, 0, Math.min(oldLength, newLength));
		return newArray;
	}

	public static byte[] resizeArray(byte[] oldArray, int newLength) {
		return (byte[]) resizeArrayObject(oldArray, newLength);
	}

	public static short[] resizeArray(short[] oldArray, int newLength) {
		return (short[]) resizeArrayObject(oldArray, newLength);
	}

	public static int[] resizeArray(int[] oldArray, int newLength) {
		return (int[]) resizeArrayObject(oldArray, newLength);
	}

	/**
	 * Resize array \texttt{oldArray} to \texttt{newRowLen} rows and \texttt{newColLen} columns.
	 * If the new lengths are smaller than the old lengths, the last elements
	 * of the arrays are lost. If the new lengths are greater than the old 
	 * lengths, new elements having the default value \texttt{t} are appended to the
	 * array. Returns a reference to the new array. 
	 * 
	 * @param oldArray old array
	 * @param newRowLen new number of rows
	 * @param newColLen new number of columns
	 * @return resized array
	 */
	public static int[][] resizeArray(int[][] oldArray, int newRowLen, int newColLen) {
		int[][] newArray = new int [newRowLen][newColLen];
		int r = Math.min(newRowLen, oldArray.length);
		for (int i = 0; i < r; i++) {
			int s = Math.min(newColLen, oldArray[i].length);
			for (int j = 0; j < s; j++)
			   newArray[i][j] = oldArray[i][j];
		}
	
		return newArray;
	}

	/**
	 * Resize row \texttt{row} of array \texttt{array} to have \texttt{len}
	 * elements. The other rows of \texttt{array} are unchanged. 
	 * If the new length \texttt{len} is greater than the old length, new
	 * elements having the default value 0 are appended to row \texttt{row}.
	 * If \texttt{row} is greater or equal to the old number of rows, 
	 * the array is extended to have \texttt{row + 1} rows. The new rows have
	 * length 0, except for row \texttt{row} which has length \texttt{len}.
	 * 
	 * Returns a reference to the resized array. 
	 * 
	 * @param array old array
	 * @param row  row index
	 * @param len  new length of row \texttt{row}
	 * @return resized array
	 */
	public static int[][] resizeRow(int[][] array, int row, int len) {
		if (row < array.length){
			array[row] = resizeArray(array[row], len);
			return array;
		}
		
		int[][] newArray = new int [row + 1][];
		for (int i = 0; i < array.length; i++) {
			newArray[i] = new int [array[i].length];
			newArray[i] = Arrays.copyOf(array[i], array[i].length);
		}
		
		for (int i = array.length; i < row; i++)
			newArray[i] = new int [0];
		newArray[row] = new int [len];
	
		return newArray;
	}

	public static long[] resizeArray(long[] oldArray, int newLength) {
		return (long[]) resizeArrayObject(oldArray, newLength);
	}

	public static char[] resizeArray(char[] oldArray, int newLength) {
		return (char[]) resizeArrayObject(oldArray, newLength);
	}

	public static boolean[] resizeArray(boolean[] oldArray, int newLength) {
		return (boolean[]) resizeArrayObject(oldArray, newLength);
	}

	public static float[] resizeArray(float[] oldArray, int newLength) {
		return (float[]) resizeArrayObject(oldArray, newLength);
	}

	public static double[] resizeArray(double[] oldArray, int newLength) {
		return (double[]) resizeArrayObject(oldArray, newLength);
	}

	/**
	 * Resize array \texttt{oldArray} to \texttt{newRowLen} rows and \texttt{newColLen} columns.
	 * If the new lengths are smaller than the old lengths, the last elements
	 * of the arrays are lost. If the new lengths are greater than the old 
	 * lengths, new elements having the default value \texttt{x} are appended to the
	 * array. Returns a reference to the new array. 
	 * 
	 * @param oldArray old array
	 * @param newRowLen new number of rows
	 * @param newColLen new number of columns
	 * @param x default value of the new elements
	 * @return resized array
	 */
	public static double[][] resizeArray(double[][] oldArray, int newRowLen,
			int newColLen, double x) {
		int i, j;
		double[][] newArray = new double [newRowLen][newColLen];
		int r = Math.min(newRowLen, oldArray.length);
		for (i = 0; i < r; i++) {
			int s = Math.min(newColLen, oldArray[i].length);
			for (j = 0; j < s; j++)
			   newArray[i][j] = oldArray[i][j];
			for (j = s; j < newColLen; j++)
			   newArray[i][j] = x;
		}
		for (i = r; i < newRowLen; i++) {
			for (j = 0; j < newColLen; j++)
			   newArray[i][j] = x;
		}	
		return newArray;
	}

	/**
	 * Resize row \texttt{row} of array \texttt{array} to have \texttt{len}
	 * elements. The other rows of \texttt{array} are unchanged. 
	 * If the new length \texttt{len} is greater than the old length, new
	 * elements having the default value 0 are appended to row \texttt{row}.
	 * If \texttt{row} is greater or equal to the old number of rows, 
	 * the array is extended to have \texttt{row + 1} rows. The new rows have
	 * length 0, except for row \texttt{row} which has length \texttt{len}.
	 * 
	 * Returns a reference to the resized array. 
	 * 
	 * @param array old array
	 * @param row  row index
	 * @param len  new length of row \texttt{row}
	 * @return resized array
	 */
	public static double[][] resizeRow(double[][] array, int row, int len) {
		if (row < array.length){
			array[row] = resizeArray(array[row], len);
			return array;
		}
		
		double[][] newArray = new double [row + 1][];
		for (int i = 0; i < array.length; i++) {
			newArray[i] = new double [array[i].length];
			newArray[i] = Arrays.copyOf(array[i], array[i].length);
		}
		
		for (int i = array.length; i < row; i++)
			newArray[i] = new double [0];
		newArray[row] = new double [len];
	
		return newArray;
	}

	/**
	 * Resizes an array \texttt{oldArray} to the length \texttt{newLength}, and
	 * returns a reference to an array with the appropriate length. If the length
	 * of \texttt{oldArray} corresponds to \texttt{newLength}, the method returns
	 * the old array reference. Otherwise, a new array is constructed, and the
	 * elements are copied from the old array, using {@link System#arraycopy}. If
	 * the new length is smaller than the old length, the last elements of the
	 * array are lost. If the new length is greater than the old length, new
	 * elements having the default value (\texttt{null}, \texttt{0}, or
	 * \texttt{false}, depending on the type of array) are appended to the array.
	 * 
	 * @param oldArray
	 *           the old array to be resized.
	 * @param newLength
	 *           the required length of the returned array.
	 * @return the old array or a resized copy of the old array.
	 * @exception NullPointerException
	 *               if \texttt{oldArray} is \texttt{null}.
	 * @exception IllegalArgumentException
	 *               if \texttt{oldArray} does not correspond to an array.
	 * @exception NegativeArraySizeException
	 *               if \texttt{newLength} is negative.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] resizeArray(T[] oldArray, int newLength) {
		return (T[]) resizeArrayObject(oldArray, newLength);
	}

	/**
	 * Determines if the given object \texttt{m} is a rectangular matrix. To be a
	 * rectangular matrix, the object must be an array of arrays of any primitive
	 * or non-array reference type. Each of the arrays must be non-\texttt{null}
	 * and have the same length.
	 * 
	 * @param m
	 *           the object to be tested.
	 * @exception NullPointerException
	 *               if \texttt{m} or one of its elements are \texttt{null}.
	 * @exception IllegalArgumentException
	 *               if the object is not a rectangular matrix.
	 */
	public static void checkRectangularMatrix(Object m) {
		final Class<?> cl = m.getClass();
		if (!cl.isArray())
			throw new IllegalArgumentException("The argument is not an array");
		final Class<?> cl2 = cl.getComponentType();
		if (!cl2.isArray())
			throw new IllegalArgumentException(
					"The argument is not an array of arrays");
		final Class<?> cl3 = cl2.getComponentType();
		if (cl3.isArray())
			throw new IllegalArgumentException("Only 2D arrays are supported");
		final int l = Array.getLength(m);
		int d = -1;
		for (int i = 0; i < l; i++) {
			final Object mi = Array.get(m, i);
			if (mi == null)
				throw new NullPointerException("The array at index " + i
						+ " is null");
			final int l2 = Array.getLength(mi);
			if (d < 0)
				d = l2;
			else if (d != l2)
				throw new IllegalArgumentException("The array at index " + i
						+ " has length " + l2 + " which is different from " + d);
		}
	}

	/**
	 * Constructs and returns a deep clone of the array \texttt{array}. If the
	 * given object corresponds to a one-dimensional array, the method clones the
	 * array. It also clones the elements in the array if \texttt{cloneElements}
	 * is \texttt{true}. If the given array is multi-dimensional, the method
	 * creates a new array of the same length, and recursively calls itself to
	 * clone nested arrays.
	 * 
	 * If \texttt{cloneElements} is \texttt{true}, the elements in the given
	 * array must be arrays, primitive elements, or objects implementing the
	 * {@link Cloneable} interface.
	 * 
	 * This method is equivalent to \texttt{array.clone()} if the given array is
	 * one-dimensional, and \texttt{cloneElements} is \texttt{false}.
	 * 
	 * @param <T>
	 *           the type of the array.
	 * @param array
	 *           the array to clone.
	 * @param cloneElements
	 *           determines if elements in the array are cloned.
	 * @return the cloned array.
	 * @exception IllegalArgumentException
	 *               if the class of the given object is not an array.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deepClone(T array, boolean cloneElements) {
		if (array == null)
			return null;
		final Class<?> cls = array.getClass();
		if (!cls.isArray())
			throw new IllegalArgumentException("The given object is not an array");
		final Class<?> ctype = cls.getComponentType();
		final int length = Array.getLength(array);
		final T res = (T) Array.newInstance(ctype, length);
		if (!ctype.isArray() && (ctype.isPrimitive() || !cloneElements))
			System.arraycopy(array, 0, res, 0, length);
		else {
			final Object[] objArray = (Object[]) array;
			final Object[] objRes = (Object[]) res;
			final Method cloneMethod;
			if (ctype.isArray())
				cloneMethod = null;
			else {
				if (!Cloneable.class.isAssignableFrom(ctype))
					throw new IllegalArgumentException(
							"The objects in the array, which are instances of "
									+ ctype.getName() + ", are not Cloneable");
				try {
					cloneMethod = ctype.getMethod("clone");
				} catch (final NoSuchMethodException nme) {
					throw new IllegalArgumentException(
							"Cannot find the clone() method in " + ctype.getName());
				}
			}
			for (int i = 0; i < length; i++) {
				// final Object e = Array.get (array, i);
				final Object e = objArray[i];
				final Object clone;
				if (e == null)
					clone = null;
				else if (ctype.isArray())
					clone = deepClone(e, cloneElements);
				else
					try {
						clone = cloneMethod.invoke(e);
					} catch (final IllegalAccessException iae) {
						throw new IllegalArgumentException(
								"Illegal access to the clone() method in "
										+ ctype.getName());
					} catch (final InvocationTargetException ite) {
						final IllegalArgumentException iae = new IllegalArgumentException(
								"Exception occurred while calling the clone() method");
						iae.initCause(ite.getCause());
						throw iae;
					}
				// Array.set (res, i, clone);
				objRes[i] = clone;
			}
		}
		return res;
	}

	/**
	 * Equivalent to {@link #deepClone(Object, boolean) deepClone (array, false)}
	 * .
	 * 
	 * @param <T>
	 *           the type of the array.
	 * @param array
	 *           the array to clone.
	 * @return the cloned array.
	 */
	public static <T> T deepClone(T array) {
		return deepClone(array, false);
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static byte[][] getTranspose(byte[][] m) {
		if (m.length == 0)
			return new byte[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final byte[][] r = new byte[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static short[][] getTranspose(short[][] m) {
		if (m.length == 0)
			return new short[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final short[][] r = new short[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static int[][] getTranspose(int[][] m) {
		if (m.length == 0)
			return new int[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final int[][] r = new int[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static long[][] getTranspose(long[][] m) {
		if (m.length == 0)
			return new long[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final long[][] r = new long[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static float[][] getTranspose(float[][] m) {
		if (m.length == 0)
			return new float[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final float[][] r = new float[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static double[][] getTranspose(double[][] m) {
		if (m.length == 0)
			return new double[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final double[][] r = new double[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static char[][] getTranspose(char[][] m) {
		if (m.length == 0)
			return new char[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final char[][] r = new char[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	public static boolean[][] getTranspose(boolean[][] m) {
		if (m.length == 0)
			return new boolean[0][0];
		final int a = m.length;
		final int b = m[0].length;
		final boolean[][] r = new boolean[b][a];
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the transpose of a matrix \texttt{m}. This method assumes that
	 * \texttt{m} is rectangular and has dimensions $a\times b$. It creates a
	 * matrix of dimensions $b\times a$, and stores element $(i, j)$ of
	 * \texttt{m} in its element $(j, i)$.
	 * 
	 * @param m
	 *           the matrix to be transposed.
	 * @return the transposed matrix.
	 * @exception NullPointerException
	 *               if \texttt{m} is \texttt{null}, or \texttt{m[i]} is
	 *               \texttt{null} for at least one index \texttt{i}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[][] getTranspose(T[][] m) {
		final Class<?> matrixClass = m.getClass();
		final Class<?> arrayClass = matrixClass.getComponentType();
		final Class<?> elementClass = arrayClass.getComponentType();
		assert matrixClass.isArray();
		assert arrayClass.isArray();
		assert !elementClass.isArray();
		final int a, b;
		if (m.length == 0) {
			a = 0;
			b = 0;
		} else {
			a = m.length;
			b = m[0].length;
		}
		final T[][] r = (T[][]) Array.newInstance(elementClass,
				new int[] { a, b });
		for (int i = 0; i < m.length; i++) {
			if (m[i].length != b)
				throw new IllegalArgumentException(
						"The given array is not rectangular");
			for (int j = 0; j < m[i].length; j++)
				r[j][i] = m[i][j];
		}
		return r;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static byte min(byte... a) {
		if (a.length == 0)
			return Byte.MAX_VALUE;
		byte min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static short min(short... a) {
		if (a.length == 0)
			return Short.MAX_VALUE;
		short min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static int min(int... a) {
		if (a.length == 0)
			return Integer.MAX_VALUE;
		int min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static long min(long... a) {
		if (a.length == 0)
			return Long.MAX_VALUE;
		long min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static float min(float... a) {
		if (a.length == 0)
			return Float.NaN;
		float min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static double min(double... a) {
		if (a.length == 0)
			return Double.NaN;
		double min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the smallest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimum.
	 */
	public static <T extends Comparable<T>> T min(T... a) {
		if (a.length == 0)
			return null;
		T min = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] != null && a[i].compareTo(min) < 0)
				min = a[i];
		return min;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static byte max(byte... a) {
		if (a.length == 0)
			return Byte.MIN_VALUE;
		byte max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static short max(short... a) {
		if (a.length == 0)
			return Short.MIN_VALUE;
		short max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static int max(int... a) {
		if (a.length == 0)
			return Integer.MIN_VALUE;
		int max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static long max(long... a) {
		if (a.length == 0)
			return Long.MIN_VALUE;
		long max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static float max(float... a) {
		if (a.length == 0)
			return Float.NaN;
		float max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static double max(double... a) {
		if (a.length == 0)
			return Double.NaN;
		double max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Returns the value of the greatest element in the array \texttt{a}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the maximum.
	 */
	public static <T extends Comparable<T>> T max(T... a) {
		if (a.length == 0)
			return null;
		T max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] != null && a[i].compareTo(max) > 0)
				max = a[i];
		return max;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(byte... a) {
		if (a.length == 0)
			return true;
		final Set<Byte> values = new HashSet<Byte>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(short... a) {
		if (a.length == 0)
			return true;
		final Set<Short> values = new HashSet<Short>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(int... a) {
		if (a.length == 0)
			return true;
		final Set<Integer> values = new HashSet<Integer>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(long... a) {
		if (a.length == 0)
			return true;
		final Set<Long> values = new HashSet<Long>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(float... a) {
		if (a.length == 0)
			return true;
		final Set<Float> values = new HashSet<Float>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Determines if the elements in the array \texttt{a} are all different, and
	 * returns the result of the test.
	 * 
	 * @param a
	 *           the tested array.
	 * @return \texttt{true} if and only if all elements are different.
	 */
	public static boolean allDifferent(double... a) {
		if (a.length == 0)
			return true;
		final Set<Double> values = new HashSet<Double>();
		values.add(a[0]);
		for (int i = 1; i < a.length; i++)
			if (!values.add(a[i]))
				return false;
		return true;
	}

	/**
	 * Roudns each number in \texttt{a} to \texttt{maxDigits} digits.
	 * 
	 * @param maxDigits
	 *           the maximal number of digits.
	 * @param a
	 *           the array.
	 * @return the resulting array.
	 */
	public static float[] round(int maxDigits, float... a) {
		final float[] res = new float[a.length];
		final double mult = Math.pow(10, maxDigits);
		for (int i = 0; i < a.length; i++)
			res[i] = (float) (Math.round(a[i] * mult) / mult);
		return res;
	}

	/**
	 * Roudns each number in \texttt{a} to \texttt{maxDigits} digits.
	 * 
	 * @param maxDigits
	 *           the maximal number of digits.
	 * @param a
	 *           the array.
	 * @return the resulting array.
	 */
	public static double[] round(int maxDigits, double... a) {
		final double[] res = new double[a.length];
		final double mult = Math.pow(10, maxDigits);
		for (int i = 0; i < a.length; i++)
			res[i] = Math.round(a[i] * mult) / mult;
		return res;
	}

	/**
	 * Returns the minimal number of digits after the decimal point required for
	 * the numbers in the array \texttt{a} to be rounded without different values
	 * becoming equal. For example, this method returns 2 when given the array
	 * \texttt{\{ 1.26, 1.28, 1.26, 1.27 \}} but 1 for the array \texttt{\{ 1.26,
	 * 1.42 \}}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimal number of digits.
	 */
	public static int getMinDigits(float... a) {
		if (a.length == 0)
			return 0;
		// Eliminate duplicates
		final Set<Float> values = new HashSet<Float>();
		for (final float element : a)
			if (!Float.isInfinite(element) && !Float.isNaN(element))
				values.add(element);
		if (values.isEmpty())
			return 0;
		final float[] b = new float[values.size()];
		int idx = 0;
		for (final float d : values)
			b[idx++] = d;

		// Find the minimal value of n
		int n = 0;
		for (final float element : b) {
			final float absa = Math.abs(element);
			if (absa < 1 && absa != 0) {
				final int exp = -(int) Math.floor(Math.log(absa) / Math.log(10));
				if (n < exp)
					n = exp;
			}
		}

		final int[] inta = new int[b.length];
		--n;
		do {
			++n;
			final double n10 = Math.pow(10, n);
			for (int i = 0; i < b.length; i++)
				inta[i] = (int) Math.round(b[i] * n10);
		} while (!allDifferent(inta));
		return n;
	}

	/**
	 * Returns the minimal number of digits after the decimal point required for
	 * the numbers in the array \texttt{a} to be rounded without different values
	 * becoming equal. For example, this method returns 2 when given the array
	 * \texttt{\{ 1.26, 1.28, 1.26, 1.27 \}} but 1 for the array \texttt{\{ 1.26,
	 * 1.42 \}}.
	 * 
	 * @param a
	 *           the tested array.
	 * @return the minimal number of digits.
	 */
	public static int getMinDigits(double... a) {
		if (a.length == 0)
			return 0;
		// Eliminate duplicates
		final Set<Double> values = new HashSet<Double>();
		for (final double element : a)
			if (!Double.isInfinite(element) && !Double.isNaN(element))
				values.add(element);
		if (values.isEmpty())
			return 0;
		final double[] b = new double[values.size()];
		int idx = 0;
		for (final double d : values)
			b[idx++] = d;

		// Find the minimal value of n
		int n = 0;
		for (final double element : b) {
			final double absa = Math.abs(element);
			if (absa < 1 && absa != 0) {
				final int exp = -(int) Math.floor(Math.log(absa) / Math.log(10));
				if (n < exp)
					n = exp;
			}
		}

		final long[] inta = new long[b.length];
		--n;
		do {
			++n;
			final double n10 = Math.pow(10, n);
			if (Double.isInfinite(n10))
				return n;
			for (int i = 0; i < b.length; i++)
				inta[i] = Math.round(b[i] * n10);
		} while (!allDifferent(inta));
		return n;
	}

	/**
	 * Merges the given arrays into a single array, and returned the constructed
	 * array.
	 * 
	 * @param arrays
	 *           the arrays to be merged together.
	 * @return the merged array.
	 */
	public static int[] merge(int[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		int[] res = new int[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static byte[] merge(byte[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		byte[] res = new byte[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static short[] merge(short[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		short[] res = new short[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static long[] merge(long[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		long[] res = new long[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static float[] merge(float[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		float[] res = new float[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static double[] merge(double[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		double[] res = new double[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static char[] merge(char[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		char[] res = new char[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	public static boolean[] merge(boolean[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		boolean[] res = new boolean[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] merge(T[]... arrays) {
		int length = 0;
		for (int i = 0; i < arrays.length; i++)
			length += arrays[i].length;
		T[] res;
		if (arrays.length > 0) {
			Class<?> cls = arrays[0].getClass().getComponentType();
			res = (T[]) Array.newInstance(cls, length);
		} else
			res = (T[]) new Object[length];
		for (int i = 0, start = 0; i < arrays.length; start += arrays[i].length, i++)
			System.arraycopy(arrays[i], 0, res, start, arrays[i].length);
		return res;
	}
	
	/**
	 * Copies the array $M$ and returns the copy.
	 * @param M
	 * @return copy of M
	 */
	public static double[][] copy(double[][] M) {
		double[][] R = new double[M.length][];
      for (int i = 0; i < M.length; i++) {
      	R[i] = new double[M[i].length];
         for (int j = 0; j < M[i].length; j++) {
            R[i][j] = M[i][j];
         }
      }
      return R;
	}
}
