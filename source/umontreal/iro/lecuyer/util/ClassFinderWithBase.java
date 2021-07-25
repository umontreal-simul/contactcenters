package umontreal.iro.lecuyer.util;

import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.NameConflictException;

/**
 * Extends the {@link ClassFinder} class to find classes
 * extending a specified base class or implementing
 * a given base interface.
 * The overridden {@link #findClass(String)}
 * method ensures that the class found
 * by {@link ClassFinder}
 * is assignable to the base class.
 *
 * @param <T> the type of the base class.
 */
public class ClassFinderWithBase<T> extends ClassFinder {
   private static final long serialVersionUID = -5695543108278703159L;
   private Class<T> baseClass;
   
   /**
    * Constructs a new class finder with base class
    * \texttt{baseClass}.
    * @param baseClass the base class.
    * @exception NullPointerException if \texttt{baseClass} is \texttt{null}.
    */
   public ClassFinderWithBase (Class<T> baseClass) {
      if (baseClass == null)
         throw new NullPointerException();
      this.baseClass = baseClass;
   }
   
   /**
    * Returns the base class for any class returned by
    * the {@link #findClass(String)} method.
    * @return the base class.
    */
   public Class<T> getBaseClass() {
      return baseClass;
   }

   @Override
   //@SuppressWarnings("unchecked")
   public Class<? extends T> findClass (String name) throws ClassNotFoundException, NameConflictException {
      final Class<?> cls = super.findClass (name);
      if (baseClass.isAssignableFrom (cls))
         return cls.asSubclass (baseClass);
         //return (Class<? extends T>)cls;
      throw new ClassNotFoundException
      ("The found class " + cls.getName() + " does not "
            + (baseClass.isInterface() ? "implement " : "extend ")
            + baseClass.getName());
   }
}
