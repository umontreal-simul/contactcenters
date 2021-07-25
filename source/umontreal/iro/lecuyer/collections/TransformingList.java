package umontreal.iro.lecuyer.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Represents a list that dynamically transforms the elements of another list.
 * This class extends the transforming collection to implement the {@link List}
 * interface.
 * 
 * @param <OE>
 *           the outer type of the elements.
 * @param <IE>
 *           the inner type of the elements.
 */
public abstract class TransformingList<OE, IE> extends
      TransformingCollection<OE, IE> implements List<OE> {
   /**
    * Constructs a new transforming list mapping the elements of the inner list
    * \texttt{innerList}.
    * 
    * @param innerList
    *           the inner list.
    * @exception NullPointerException
    *               if \texttt{innerList} is \texttt{null}.
    */
   public TransformingList (List<IE> innerList) {
      super (innerList);
   }

   /**
    * Returns the inner list.
    * 
    * @return the inner list.
    */
   @Override
   public List<IE> getInnerCollection () {
      return (List<IE>) super.getInnerCollection ();
   }
   
   /**
    * Attempts to make this transforming list
    * random-accessible, i.e., supporting
    * fast random access.
    * If the inner list implements the {@link RandomAccess}
    * interface, this method returns a transforming list
    * implementing {@link RandomAccess}.
    * Otherwise, the method returns this reference.
    * @return the transforming list, possibly random-accessible.
    */
   public TransformingList<OE, IE> tryToMakeRandomAccess() {
      if (getInnerCollection() instanceof RandomAccess)
         return new RandomAccessTransList();
      else
         return this;
   }

   public void add (int index, OE element) {
      getInnerCollection ().add (index, convertToInnerType (element));
   }

   public boolean addAll (int index, Collection<? extends OE> c) {
      final ListIterator<OE> itr = listIterator (index);
      final int size0 = size ();
      for (final OE e : c)
         itr.add (e);
      return size0 != size ();
   }

   public OE get (int index) {
      return convertFromInnerType (getInnerCollection ().get (index));
   }

   @SuppressWarnings ("unchecked")
   public int indexOf (Object o) {
      final OE oe;
      try {
         oe = (OE) o;
      }
      catch (final ClassCastException e) {
         return -1;
      }
      return getInnerCollection ().indexOf (convertToInnerType (oe));
   }

   @SuppressWarnings ("unchecked")
   public int lastIndexOf (Object o) {
      final OE oe;
      try {
         oe = (OE) o;
      }
      catch (final ClassCastException e) {
         return -1;
      }
      return getInnerCollection ().lastIndexOf (convertToInnerType (oe));
   }

   @Override
   public boolean equals (Object o) {
      if (!(o instanceof List))
         return false;
      final List<?> l = (List<?>) o;
      if (size () != l.size ())
         return false;
      final Iterator<?> itr1 = iterator ();
      final Iterator<?> itr2 = l.iterator ();
      while (itr1.hasNext ()) {
         final Object e1 = itr1.next ();
         final Object e2 = itr2.next ();
         boolean eq = e1 == null ? e2 == null : e1.equals (e2);
         if (!eq)
            return false;
      }
      return true;
   }

   @Override
   public int hashCode () {
      int hashCode = 1;
      for (final Object e : this)
         hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode ());
      return hashCode;
   }

   public ListIterator<OE> listIterator () {
      return new MyListIterator (getInnerCollection ().listIterator ());
   }

   public ListIterator<OE> listIterator (int index) {
      return new MyListIterator (getInnerCollection ().listIterator (index));
   }

   public OE remove (int index) {
      return convertFromInnerType (getInnerCollection ().remove (index));
   }

   public OE set (int index, OE element) {
      IE from = convertToInnerType (element);
      from = getInnerCollection ().set (index, from);
      return convertFromInnerType (from);
   }

   public List<OE> subList (int fromIndex, int toIndex) {
      return new SubList (getInnerCollection ().subList (fromIndex, toIndex));
   }

   private class SubList extends TransformingList<OE, IE> {
      public SubList (List<IE> innerList) {
         super (innerList);
      }

      @Override
      public OE convertFromInnerType (IE e) {
         return TransformingList.this.convertFromInnerType (e);
      }

      @Override
      public IE convertToInnerType (OE e) {
         return TransformingList.this.convertToInnerType (e);
      }
   }

   private class MyListIterator implements ListIterator<OE> {
      private ListIterator<IE> itr;

      public MyListIterator (ListIterator<IE> itr) {
         this.itr = itr;
      }

      public void add (OE o) {
         final IE fe = convertToInnerType (o);
         itr.add (fe);
      }

      public boolean hasNext () {
         return itr.hasNext ();
      }

      public boolean hasPrevious () {
         return itr.hasPrevious ();
      }

      public OE next () {
         return convertFromInnerType (itr.next ());
      }

      public int nextIndex () {
         return itr.nextIndex ();
      }

      public OE previous () {
         return convertFromInnerType (itr.previous ());
      }

      public int previousIndex () {
         return itr.previousIndex ();
      }

      public void remove () {
         itr.remove ();
      }

      public void set (OE o) {
         final IE fe = convertToInnerType (o);
         itr.set (fe);
      }
   }
   
   private class RandomAccessTransList extends TransformingList<OE, IE> implements RandomAccess {
      public RandomAccessTransList () {
         super (TransformingList.this.getInnerCollection ());
      }
      
      @Override
      public OE convertFromInnerType (IE e) {
         return TransformingList.this.convertFromInnerType (e);
      }

      @Override
      public IE convertToInnerType (OE e) {
         return TransformingList.this.convertToInnerType (e);
      }
   }
}
