package umontreal.iro.lecuyer.util;

/**
 * Exception occurring when a conversion is not supported by a method of
 * {@link StringConvert}. This exception is thrown when a conversion method is
 * given a target class it does not support. This differs from the case where
 * the target class is supported while the conversion fails.
 */
public class UnsupportedConversionException extends Exception {
   private static final long serialVersionUID = 4072031355491098958L;
   private Class<?> cls;
   private String val;

   /**
    * Constructs a new unsupported conversion exception with no target class or
    * message.
    */
   public UnsupportedConversionException () {}

   /**
    * Constructs a new unsupported conversion exception with no target class and
    * message \texttt{message}.
    * 
    * @param message
    *           the message describing the exception.
    */
   public UnsupportedConversionException (String message) {
      super (message);
   }

   /**
    * Constructs a new unsupported conversion exception with target class
    * \texttt{cls}, converted value \texttt{val} and no message.
    * 
    * @param cls
    *           the target class.
    * @param val
    *           the value being converted.
    */
   public UnsupportedConversionException (Class<?> cls, String val) {
      this.cls = cls;
      this.val = val;
   }

   /**
    * Constructs a new unsupported conversion exception with target class
    * \texttt{cls}, converted value \texttt{val} and message \texttt{message}.
    * 
    * @param cls
    *           the target class.
    * @param val
    *           the value being converted.
    * @param message
    *           the message describing the exception.
    */
   public UnsupportedConversionException (Class<?> cls, String val,
         String message) {
      super (message);
      this.cls = cls;
      this.val = val;
   }

   /**
    * Returns the target class of the conversion causing the exception.
    * 
    * @return the target class.
    */
   public Class<?> getTargetClass () {
      return cls;
   }

   /**
    * Returns the value to be converted and causing the exception.
    * 
    * @return the value to be converted.
    */
   public String getValue () {
      return val;
   }

   /**
    * Returns a short description of the exception. If no target class and value
    * is associated to the object, this calls the base class's \texttt{toString}
    * method. Otherwise, a string containing the following elements is
    * constructed and returned. \begin{itemize} \item The name of the class of
    * the object. \item \texttt{": Cannot convert value "} \item If
    * {@link #getValue} returns a non-\texttt{null} value, the returned value
    * followed by a space \item \texttt{"to target class"} \item If the target
    * class is specified, a space followed by the target class name \item If the
    * message is specified, the message preceeded by \texttt{", "}.
    * \end{itemize}
    * 
    * @return the short string describing the exception.
    */
   @Override
   public String toString () {
      if (getTargetClass () == null && getValue () == null)
         return super.toString ();
      final StringBuilder sb = new StringBuilder (getClass ().getName ());
      sb.append (": ");
      sb.append ("Cannot convert value ");
      if (getValue () != null)
         sb.append (getValue ()).append (' ');
      sb.append ("to target class");
      if (getTargetClass () != null)
         sb.append (' ').append (getTargetClass ().getName ());
      if (getMessage () != null)
         sb.append (", ").append (getMessage ());
      return sb.toString ();
   }
}
