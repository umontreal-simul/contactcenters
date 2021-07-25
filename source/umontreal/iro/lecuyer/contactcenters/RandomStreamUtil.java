package umontreal.iro.lecuyer.contactcenters;

import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.rng.RandomStreamFactory;
import umontreal.iro.lecuyer.util.ArrayUtil;

/**
 * Provides utility methods to create
 * and extend
 * arrays of random streams.
 */
public class RandomStreamUtil {
   private RandomStreamUtil() {}

   /**
    * Creates or extends an array of random streams. This can be useful when
    * reconstructing a contact center with new parameters, to keep as many
    * random streams as possible for maximizing random number synchronization.
    *
    * If \texttt{oldArray} is \texttt{null}, a new array of length \texttt{size}
    * will be allocated and filled with random streams. If \texttt{oldArray} is
    * not \texttt{null} and its length is greater than or equal to
    * \texttt{size}, it is returned unchanged. Otherwise, a new array is
    * created, the already constructed random streams are copied and new ones
    * are constructed to fill the array. The random streams are created using
    * the given random stream factory. The method returns an array of random
    * streams with length greater than or equal to \texttt{size}.
    *
    * @param oldArray
    *           the old array of random streams.
    * @param size
    *           the minimal size of the returned array.
    * @param rsf
    *           the random stream factory used to create the random streams.
    * @return the constructed array of random streams.
    */
   public static RandomStream[] createRandomStreamArray (
         RandomStream[] oldArray, int size, RandomStreamFactory rsf) {
      RandomStream[] res;
      int idx;
      if (oldArray == null) {
         res = new RandomStream[size];
         idx = 0;
      }
      else if (oldArray.length < size) {
         res = new RandomStream[size];
         System.arraycopy (oldArray, 0, res, 0, oldArray.length);
         idx = oldArray.length;
      }
      else
         return oldArray;
      for (int i = idx; i < res.length; i++)
         res[i] = rsf.newInstance ();
      return res;
   }

   /**
    * Creates or extends a matrix (i.e., 2D array) of random streams.
    * This can be useful when
    * reconstructing a contact center with new parameters, to keep as many
    * random streams as possible for maximizing random number synchronization.
    *
    * If \texttt{oldMatrix} is \texttt{null}, a new array of length \texttt{rows}
    * will be allocated and filled with
    * arrays of  random streams. If \texttt{oldMatrix} is
    * not \texttt{null} and its length is greater than or equal to
    * \texttt{rows}, it is returned unchanged. Otherwise, a new array is
    * created, the already constructed arrays of random streams are copied and new ones
    * are constructed to fill the array. The internal
    * arrays of random streams are created using
    * {@link #createRandomStreamArray(RandomStream[],int,RandomStreamFactory)}.

    * @param oldMatrix the old matrix of random streams.
    * @param rows the required number of rows.
    * @param columns the required number of columns.
    * @param rsf the random stream factory used to create streams.
    * @return the new matrix of random streams.
    */
   public static RandomStream[][] createRandomStreamMatrix (RandomStream[][] oldMatrix, int rows, int columns, RandomStreamFactory rsf) {
      final RandomStream[][] matrix;
      if (oldMatrix == null)
         matrix = new RandomStream[rows][];
      else if (oldMatrix.length < rows)
         matrix = ArrayUtil.resizeArray (oldMatrix, rows);
      else
         matrix = oldMatrix;
      for (int i = 0; i < matrix.length; i++)
         matrix[i] = createRandomStreamArray (matrix[i], columns, rsf);
      return matrix;
   }
}
