package umontreal.iro.lecuyer.util;

import java.io.IOException;
import java.util.Formatter;

/**
 * Provides helper methods to format exceptions in a more compact
 * way than the default exception formatting.
 * The default behavior of the Java Virtual Machine when an
 * uncaught exception occurs is to
 * call the {@link Throwable#printStackTrace()}
 * method in order to display the full stack trace on the console.
 * This produces a rather verbose output, especially if
 * the exception has a cause, i.e., {@link Throwable#getCause()}
 * returns \texttt{true}.
 * However, for some exception types, e.g., {@link IOException},
 * it might be sufficient to display only the exception class name
 * and message. This can be done by
 * {@link Throwable#toString()}, but the cause of the
 * exception is then lost.
 * This class provides the
 * {@link #throwableToString(Throwable)}
 * which can format and return a short version of the
 * a throwable's cause chain.
 * Methods are also provided to install a default
 * exception handler which calls
 * {@link #throwableToString(Throwable)}
 * instead of {@link Throwable#printStackTrace()}. 
 */
public class ExceptionUtil {
   private static final String DEBUG_PROPERTY = "umontreal.iro.lecuyer.util.PrintStackTrace";

   private static final class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
      public void uncaughtException (Thread t, Throwable e) {
         System.err.print ("Exception in thread ");
         System.err.print (t.getName());
         System.err.print (" ");
         if (e instanceof Exception)
            System.err.println (throwableToString (e));
         else
            e.printStackTrace();
      }
   }

   /**
    * Converts the throwable \texttt{throwable} to a string,
    * and returns the constructed string.
    * This method uses {@link Throwable#toString()}
    * to get a string from the given throwable, and
    * all its causes.
    * The formatting is similar to the default
    * way Java prints exceptions, but
    * stack trace elements are omitted.
    * However, if the \texttt{umontreal.iro.lecuyer.util.PrintStackTrace}
    * property is set to \texttt{true}, the full
    * stack trace is also formatted for each
    * throwable.
    * @param throwable the throwable to convert.
    * @return the string corresponding to the throwable.
    */
   public static String throwableToString (Throwable throwable) {
      final boolean debug = System.getProperty (DEBUG_PROPERTY) != null;
      final Formatter fmt = new Formatter();
      Throwable currentThrowable = throwable;
      boolean firstThrowable = true;
      while (currentThrowable != null) {
         if (firstThrowable)
            firstThrowable = false;
         else
            fmt.format ("%nCaused by ");
         fmt.format ("%s", currentThrowable.toString ());
         if (debug)
            for (final StackTraceElement el : currentThrowable.getStackTrace ())
               fmt.format ("%n     %s", el.toString ());
         currentThrowable = currentThrowable.getCause ();
      }
      return fmt.toString ();
   }

   /**
    * Sets the uncaught exception handler to
    * use {@link #throwableToString(Throwable)} to
    * print exceptions, i.e., instances of {@link Exception}.
    * Other instances of {@link Throwable}, e.g.,
    * instances of {@link Error}, are printed using
    * {@link Throwable#printStackTrace()}.
    */
   public static void replaceDefaultExceptionHandler() {
      Thread.setDefaultUncaughtExceptionHandler (new DefaultExceptionHandler());
   }

   /**
    * Constructs and returns a string giving memory
    * status of the virtual machine.
    * This string gives the free, available and maximal
    * memory of the JVM.
    * @return the string with memory status.
    */
   public static String formatMemoryStatus() {
      final Runtime rt = Runtime.getRuntime ();
      return String.format ("%,d free bytes over %,d available tyes, with a maximum of %,d bytes",
            rt.freeMemory (), rt.totalMemory (), rt.maxMemory ());
   }
}
