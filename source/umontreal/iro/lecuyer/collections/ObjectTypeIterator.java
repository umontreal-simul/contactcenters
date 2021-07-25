package umontreal.iro.lecuyer.collections;

import java.util.Iterator;

/**
 * Represents an iterator traversing objects of
 * a particular class enumerated by another iterator.
 * @param <E> the type of the objects.
 */
public class ObjectTypeIterator<E> extends FilteredIterator<E> {
   private final Class<E> objectClass;
   
   /**
    * Constructs a new iterator traversing objects
    * of class \texttt{objectClass} enumerated by
    * the inner iterator \texttt{it}.
    * @param it the inner iterator.
    * @param objectClass the object class.
    * @exception NullPointerException if \texttt{it} or
    * \texttt{objectClass} are \texttt{null}.
    */
   public ObjectTypeIterator (Iterator<? super E> it, Class<E> objectClass) {
      super (it);
      if (objectClass == null)
         throw new NullPointerException();
      this.objectClass = objectClass;
   }

   /**
    * Constructs a new iterator traversing
    * at most \texttt{maxNumElements} objects
    * of class \texttt{objectClass} enumerated by
    * the inner iterator \texttt{it}.
    * @param it the inner iterator.
    * @param objectClass the object class.
    * @param maxNumElements the maximal number of traversed objects.
    * @exception NullPointerException if \texttt{it} or
    * \texttt{objectClass} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{maxNumElements} is negative.
    */
   public ObjectTypeIterator (Iterator<? super E> it, Class<E> objectClass, int maxNumElements) {
      super (it, maxNumElements);
      if (objectClass == null)
         throw new NullPointerException();
      this.objectClass = objectClass;
   }
   
   /**
    * Returns the class of the objects returned
    * by this iterator.
    * @return the class of the iterated objects.
    */
   public Class<E> getObjectClass() {
      return objectClass;
   }
   
   @Override
   public boolean filter (Object o) {
      return objectClass.isInstance (o);
   }
}
