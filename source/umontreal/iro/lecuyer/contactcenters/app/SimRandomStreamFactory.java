package umontreal.iro.lecuyer.contactcenters.app;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import umontreal.iro.lecuyer.contactcenters.app.params.RandomStreamsParams;
import umontreal.ssj.rng.BasicRandomStreamFactory;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.util.ClassFinderWithBase;
import umontreal.ssj.util.NameConflictException;

/**
 * Represents a random stream factory created using
 * simulation parameters.
 * This extends {@link BasicRandomStreamFactory}
 * to create random streams from the class whose name
 * is given by {@link RandomStreamsParams#getStreamClass()}. 
 */
public class SimRandomStreamFactory extends BasicRandomStreamFactory {
   private static final ClassFinderWithBase<RandomStream> cfRandomStream = new ClassFinderWithBase<RandomStream>(RandomStream.class);
   static {
      cfRandomStream.getImports().add ("umontreal.ssj.rng.*");
   }
   
   private static Class<? extends RandomStream> getStreamClass (RandomStreamsParams par) {
      if (par == null)
         return MRG32k3a.class;
      try {
         return cfRandomStream.findClass (par.getStreamClass());
      }
      catch (final ClassNotFoundException cne) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Could find class with name " + par.getStreamClass());
         iae.initCause (cne);
         throw iae;
      }
      catch (final NameConflictException nce) {
         final IllegalArgumentException iae = new IllegalArgumentException
         ("Could find class with name " + par.getStreamClass());
         iae.initCause (nce);
         throw iae;
      }
   }
   
   public SimRandomStreamFactory (RandomStreamsParams par) {
      super (getStreamClass (par));
   }
   
   public static void initSeed (RandomStreamsParams par) {
      if (par == null)
         // We cannot initialize the default MRG32k3a to the
         // default seed while the seed of user-specified
         // random stream class will be unchanged.
         //MRG32k3a.setPackageSeed (new long[] {12345, 12345, 12345,
         //      12345, 12345, 12345});
         return;
      else if (par.isSetStreamSeed ()) {
         final Class<? extends RandomStream> rsClass = getStreamClass (par); 
         final double[] seed = par.getStreamSeed ();
         if (seed != null)
            setPackageSeed (rsClass, seed);
      }
   }
   
   public static void setPackageSeed (Class<?> cls, double... seed) {
      for (final Method mt : cls.getMethods()) {
         if (!mt.getName().equals("setPackageSeed"))
            continue;
         if (mt.getParameterTypes().length != 1)
            continue;
         final Class<?> pcls = mt.getParameterTypes()[0];
         if (!pcls.isArray())
            continue;
         Object seedArray;
         if (pcls == double[].class)
            seedArray = seed;
         else {
            final Class<?> pclsComp = pcls.getComponentType();
            if (!Number.class.isAssignableFrom (pclsComp) &&
                  (!pclsComp.isPrimitive() || pclsComp == boolean.class || pclsComp == char.class))
               continue;
            seedArray = Array.newInstance(pclsComp, seed.length);
            for (int i = 0; i < seed.length; i++)
               if (pclsComp == byte.class || pclsComp == Byte.class)
                  Array.set (seedArray, i, (byte)Math.round (seed[i]));
               else if (pclsComp == short.class || pclsComp == Short.class)
                  Array.set (seedArray, i, (short)Math.round (seed[i]));
               else if (pclsComp == int.class || pclsComp == Integer.class)
                  Array.set (seedArray, i, (int)Math.round (seed[i]));
               else if (pclsComp == long.class || pclsComp == Long.class)
                  Array.set (seedArray, i, Math.round (seed[i]));
               else if (pclsComp == float.class || pclsComp == Float.class)
                  Array.set (seedArray, i, (float)seed[i]);
               else if (pclsComp == double.class || pclsComp == Double.class)
                  Array.set (seedArray, i, seed[i]);
               else if (pclsComp == BigInteger.class)
                  Array.set (seedArray, i, BigInteger.valueOf (Math.round (seed[i])));
               else if (pclsComp == BigDecimal.class)
                  Array.set (seedArray, i, BigDecimal.valueOf (seed[i]));
               else
                  throw new IllegalArgumentException
                  ("Incompatible argument class " + pcls.getName());
            try {
               mt.invoke (null, new Object[] { seedArray });
               return;
            }
            catch (final IllegalAccessException iae) {
               final IllegalArgumentException iaeOut = new IllegalArgumentException
               ("Cannot call method " + mt.getName());
               iaeOut.initCause (iae);
               throw iaeOut;
            }
            catch (final InvocationTargetException ite) {
               final IllegalArgumentException iaeOut = new IllegalArgumentException
               ("Cannot call method " + mt.getName());
               iaeOut.initCause (ite.getCause());
               throw iaeOut;
            }
         }         
      }
      throw new IllegalArgumentException
      ("Cannot find an appropriate setPackageSeed method in class " + cls.getName());
   }
}
