package umontreal.iro.lecuyer.xmlbind;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;

/**
 * This exception is thrown when a problem occurs during the construction of a
 * random variate generator by {@link ParamReadHelper#createGenerator(RandomVariateGenParams,RandomStream)}.
 */
public class GeneratorCreationException extends Exception {
   private static final long serialVersionUID = 2691115477652019531L;
   private Distribution dist;
   private Class<? extends RandomVariateGen> genClass;

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
    * Constructs a new generator creation exception with distribution
    * \texttt{dist}, generator class \texttt{genClass}, and no message.
    * 
    * @param dist
    *           the distribution associated with the generator.. = *
    * @param genClass
    *           the class of the random variate generator that cannot be
    *           created.
    */
   public GeneratorCreationException (Distribution dist,
         Class<? extends RandomVariateGen> genClass) {
      super ();
      this.dist = dist;
      this.genClass = genClass;
   }

   /**
    * Constructs a new generator creation exception with distribution
    * \texttt{dist}, generator class \texttt{genClass}, and message
    * \texttt{message}.
    * 
    * @param dist
    *           the distribution associated with the generator..
    * @param genClass
    *           the class of the random variate generator that cannot be
    *           created.
    * @param message
    *           the message describing the exception.
    */
   public GeneratorCreationException (Distribution dist,
         Class<? extends RandomVariateGen> genClass, String message) {
      super (message);
      this.dist = dist;
      this.genClass = genClass;
   }

   /**
    * Returns the distribution which caused the exception.
    * 
    * @return the distribution having caused the exception.
    */
   public Distribution getDistribution () {
      return dist;
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
    * Returns a short description of this exception. If no distribution and
    * generator class are associated with this exception, this method returns
    * the result of the superclass's \texttt{toString} method. Otherwise, it
    * returns a string with the following contents. \begin{itemize} \item The
    * name of this class \item ``\texttt{: }''
    * \item If
    * a message is given, the text of the message.
    * \item ``\texttt{, for }''
    *  \item If the distribution is
    * available \begin{itemize} \item ``\texttt{distribution }'' \item The result
    * of {@link Object#toString()} \item If the generator class is
    * available ``\texttt{, }'' \end{itemize} \item If the generator class is
    * given, ``\texttt{generator }'' followed by the generator class name
    * \end{itemize}
    * 
    * @return the short string describing the exception.
    */
   @Override
   public String toString () {
      if (getDistribution () == null && getGeneratorClass () == null)
         return super.toString ();
      final StringBuilder msg = new StringBuilder (getClass ().getName ());
      msg.append (": ");
      if (getMessage () != null) {
         msg.append (getMessage ());
         msg.append (", for ");
      }
      else
         msg.append (" For ");
      if (getDistribution () != null) {
         msg.append ("distribution ");
         msg.append (dist.toString ());
         if (getGeneratorClass () != null)
            msg.append (", and ");
      }
      if (getGeneratorClass () != null)
         msg.append ("generator class ").append (getGeneratorClass ().getName ());
      return msg.toString ();
   }
}
