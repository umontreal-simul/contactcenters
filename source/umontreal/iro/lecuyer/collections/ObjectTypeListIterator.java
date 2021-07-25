package umontreal.iro.lecuyer.collections;

import java.util.ListIterator;

/**
 * Represents a list iterator traversing objects of
 * a particular class enumerated by another iterator.
 * @param <E> the type of the objects.
 */
public class ObjectTypeListIterator<E> extends FilteredListIterator<E> {
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
   public ObjectTypeListIterator (ListIterator<? super E> it, Class<E> objectClass) {
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
    * @param maxNumElements the maximal nuber of traverd objects.
    * @exception NullPointerException if \texttt{it} or
    * \texttt{objectClass} are \texttt{null}.
    * @exception IllegalArgumentException if \texttt{maxNumElements}
    * is negative. 
    */
   public ObjectTypeListIterator (ListIterator<? super E> it, Class<E> objectClass, int maxNumElements) {
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
