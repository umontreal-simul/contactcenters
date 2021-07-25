package umontreal.iro.lecuyer.xmlconfig;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.randvar.RandomVariateGen;

/**
 * This exception is thrown when a problem occurs during the construction of a
 * random variate generator by {@link RandomVariateGenParam#createGenerator} or
 * {@link RandomVariateGenParam#createGeneratorInt}.
 */
public class GeneratorCreationException extends RuntimeException {
   private static final long serialVersionUID = -6370766112362279106L;
   private Class<? extends Distribution> distClass;
   private Class<? extends RandomVariateGen> genClass;
   private String distParams;

   /**
    * Constructs a new generator creation exception with no generator
    * information or message.
    */
   public GeneratorCreationException () {
      super ();
   }

   /**
    * Constructs a new generator creation exception with no generator
    * information and message \texttt{message}.
    * 
    * @param message
    *           the message describing the exception.
    */
   public GeneratorCreationException (String message) {
      super (message);
   }

   /**
    * Constructs a new generator creation exception with distribution class
    * \texttt{distClass}, distribution parameters \texttt{distParams}, generator
    * class \texttt{genClass}, and no message.
    * 
    * @param distClass
    *           the class of the distribution associated with the generator..
    * @param distParams
    *           the parameters given to the constructor of the distribution
    *           class.
    * @param genClass
    *           the class of the random variate generator that cannot be
    *           created.
    */
   public GeneratorCreationException (Class<? extends Distribution> distClass,
         String distParams, Class<? extends RandomVariateGen> genClass) {
      super ();
      this.distClass = distClass;
      this.distParams = distParams;
      this.genClass = genClass;
   }

   /**
    * Constructs a new generator creation exception with distribution class
    * \texttt{distClass}, distribution parameters \texttt{distParams}, generator
    * class \texttt{genClass}, and message \texttt{message}.
    * 
    * @param distClass
    *           the class of the distribution associated with the generator..
    * @param distParams
    *           the parameters given to the constructor of the distribution
    *           class.
    * @param genClass
    *           the class of the random variate generator that cannot be
    *           created.
    * @param message
    *           the message describing the exception.
    */
   public GeneratorCreationException (Class<? extends Distribution> distClass,
         String distParams, Class<? extends RandomVariateGen> genClass,
         String message) {
      super (message);
      this.distClass = distClass;
      this.distParams = distParams;
      this.genClass = genClass;
   }

   /**
    * Returns the distribution class which caused the exception.
    * 
    * @return the distribution class having caused the exception.
    */
   public Class<? extends Distribution> getDistributionClass () {
      return distClass;
   }

   /**
    * Returns the distribution parameters for which there is no corresponding
    * constructor in the distribution class.
    * 
    * @return the distribution parameters having caused the exception.
    */
   public String getDistributionParameters () {
      return distParams;
   }

   /**
    * Returns the generator class which caused the exception.
    * 
    * @return the generator class having caused the exception.
    */
   public Class<? extends RandomVariateGen> getGeneratorClass () {
      return genClass;
   }

   /**
    * Returns a short description of this exception. If no distribution class,
    * distribution parameters and generator class are associated with this
    * exception, this method returns the result of the superclass's
    * \texttt{toString} method. Otherwise, it returns a string with the
    * following contents. \begin{itemize} \item The name of this class \item
    * \texttt{": For "} \item If the distribution class or parameters are
    * available \begin{itemize} \item \texttt{"distribution "} \item The name of
    * the distribution class if available \item If the parameters are available,
    * \texttt{"("}, parameters, \texttt{")"} \item If the generator class is
    * available \texttt{", "} \end{itemize} \item If the generator class is
    * given, \texttt{"generator "} followed by the generator class name \item If
    * a message is given, \texttt{", "} followed by the message string.
    * \end{itemize}
    * 
    * @return the short string describing the exception.
    */
   @Override
   public String toString () {
      if (getDistributionClass () == null
            && getDistributionParameters () == null
            && getGeneratorClass () == null)
         return super.toString ();
      final StringBuilder msg = new StringBuilder (getClass ().getName ());
      msg.append (": For ");
      if (getDistributionClass () != null
            || getDistributionParameters () != null) {
         msg.append ("distribution ");
         if (getDistributionClass () != null) {
            msg.append (getDistributionClass ().getName ());
            if (getDistributionParameters () != null
                  && getDistributionParameters ().length () > 0)
               msg.append (' ');
         }
         if (getDistributionParameters () != null) {
            String par = getDistributionParameters ();
            if (par.length () > 100)
               par = par.substring (0, 100) + "...";
            msg.append ('(').append (par).append (')');
         }
         if (getGeneratorClass () != null)
            msg.append (", ");
      }
      if (getGeneratorClass () != null)
         msg.append ("generator ").append (getGeneratorClass ().getName ());
      if (getMessage () != null)
         msg.append (", ").append (getMessage ());
      return msg.toString ();
   }
}
