package umontreal.iro.lecuyer.util;

import java.io.Serializable;

/**
 * Represents a pair of values.
 *
 * @param <S> the type of the first value.
 * @param <T> the type of the second value.
 */
public class Pair<S, T> implements Cloneable, Serializable {
   private static final long serialVersionUID = 916557097762043598L;
   /**
    * The first value of this pair.
    */
   private S first;
   /**
    * The second value of this pair.
    */
   private T second;
   
   /**
    * Constructs a new pair for values
    * \texttt{first} and \texttt{second}.
    * @param first the first value.
    * @param second the second value.
    */
   public Pair (S first, T second) {
      this.first = first;
      this.second = second;
   }
   
   /**
    * Constructs a new pair from the pair
    * \texttt{pair}.
    * @param pair the pair to get values from.
    * @exception NullPointerException if \texttt{pair} is \texttt{null}.
    */
   public Pair (Pair<? extends S, ? extends T> pair) {
      this.first = pair.getFirst ();
      this.second = pair.getSecond ();
   }
   
   /**
    * Returns the first value of this pair.
    * @return the first value.
    */
   public S getFirst() {
      return first;
   }
   
   /**
    * Sets the first value of this pair to \texttt{first}.
    * @param first the new first value of this pair.
    */
   public void setFirst (S first) {
      this.first = first;
   }
   
   /**
    * Returns the second value of this pair.
    * @return the second value of this pair.
    */
   public T getSecond() {
      return second;
   }
   
   /**
    * Sets the second value of this pair to
    * \texttt{second}.
    * @param second the second value of this pair.
    */
   public void setSecond (T second) {
      this.second = second;
   }

   @Override
   public int hashCode () {
      final int PRIME = 31;
      int result = 1;
      result = PRIME * result + (first == null ? 0 : first.hashCode ());
      result = PRIME * result + (second == null ? 0 : second.hashCode ());
      return result;
   }

   @Override
   public boolean equals (Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass () != obj.getClass ())
         return false;
      final Pair<?, ?> other = (Pair<?, ?>) obj;
      if (first == null) {
         if (other.first != null)
            return false;
      }
      else if (!first.equals (other.first))
         return false;
      if (second == null) {
         if (other.second != null)
            return false;
      }
      else if (!second.equals (other.second))
         return false;
      return true;
   }

   /**
    * Clones this pair.
    * This method does not clone the
    * values in the pair.
    */
   @SuppressWarnings("unchecked")
   @Override
   public Pair<S, T> clone() {
      Pair<S, T> pair;
      try {
         pair = (Pair<S, T>)super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError("CloneNotSupportedException for a class implementing Cloneable");
      }
      return pair;
   }
}
