package umontreal.iro.lecuyer.xmlconfig;

import umontreal.ssj.probdist.Distribution;

/**
 * This exception is thrown when a problem occurs during the construction of a
 * distribution object by {@link RandomVariateGenParam#createDistribution} or
 * {@link RandomVariateGenParam#createDistributionInt}, or during the parsing
 * of the nested text containing a distribution XML element.
 */
public class DistributionCreationException extends RuntimeException {
   private static final long serialVersionUID = 6389797833060421295L;
   private Class<? extends Distribution> distClass;
   private String distParams;

   /**
    * Constructs a new distribution creation exception with no distribution
    * information or message.
    */
   public DistributionCreationException () {
      super ();
   }

   /**
    * Constructs a new distribution creation exception with no distribution
    * information and message \texttt{message}.
    * 
    * @param message
    *           the message describing the exception.
    */
   public DistributionCreationException (String message) {
      super (message);
   }

   /**
    * Constructs a new distribution creation exception with distribution class
    * \texttt{distClass}, distribution parameters \texttt{distParams}, and no
    * message.
    * 
    * @param distClass
    *           the class of the distribution which cannot be created.
    * @param distParams
    *           the parameters given to the constructor of the distribution
    *           class.
    */
   public DistributionCreationException (
         Class<? extends Distribution> distClass, String distParams) {
      super ();
      this.distClass = distClass;
      this.distParams = distParams;
   }

   /**
    * Constructs a new distribution creation exception with distribution class
    * \texttt{distClass}, distribution parameters \texttt{distParams}, and
    * message \texttt{message}.
    * 
    * @param distClass
    *           the class of the distribution which cannot be created.
    * @param distParams
    *           the parameters given to the constructor of the distribution
    *           class.
    * @param message
    *           the message describing the exception.
    */
   public DistributionCreationException (
         Class<? extends Distribution> distClass, String distParams,
         String message) {
      super (message);
      this.distClass = distClass;
      this.distParams = distParams;
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
    * constructor in the distribution class, or an exception occurred during the
    * call to a constructor.
    * 
    * @return the distribution parameters having caused the exception.
    */
   public String getDistributionParameters () {
      return distParams;
   }

   /**
    * Returns a short description of this exception. If no distribution class
    * and parameters are associated with this exception, this method returns the
    * result of the superclass's \texttt{toString} method. Otherwise, it returns
    * a string with the following contents. \begin{itemize} \item The name of
    * this class \item \texttt{": For "} \item The name of the distribution
    * class if available \item If the parameters are available, \texttt{"("},
    * parameters, \texttt{")"} \item If a message is given, \texttt{", "}
    * followed by the message string. \end{itemize}
    * 
    * @return the short string describing the exception.
    */
   @Override
   public String toString () {
      if (getDistributionClass () == null
            && getDistributionParameters () == null)
         return super.toString ();
      final StringBuilder msg = new StringBuilder (getClass ().getName ());
      msg.append (": For ");
      if (getDistributionClass () != null) {
         msg.append (getDistributionClass ().getName ());
         if (getDistributionParameters () != null
               && getDistributionParameters ().length () > 0)
            msg.append (' ');
      }
      if (getDistributionParameters () != null) {
         String par = getDistributionParameters ();
         // Avoid very long strings to be printed
         // when an exception is thrown. This can
         // happen with distributions taking an array as paramter,
         // e.g., DiscreteDistribution.
         if (par.length () > 100)
            par = par.substring (0, 100) + "...";
         msg.append ('(').append (par).append (')');
      }
      if (getMessage () != null)
         msg.append (", ").append (getMessage ());
      return msg.toString ();
   }
}
