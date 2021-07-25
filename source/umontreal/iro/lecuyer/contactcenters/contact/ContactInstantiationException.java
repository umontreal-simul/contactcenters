package umontreal.iro.lecuyer.contactcenters.contact;

/**
 * This exception is thrown when a contact factory cannot instantiate a contact
 * on a call to its {@link ContactFactory#newInstance} method.
 */
public class ContactInstantiationException extends RuntimeException {
   private static final long serialVersionUID = 432848331743317334L;
   private ContactFactory factory;

   /**
    * Constructs a new contact instantiation exception with
    * no message, no cause, and thrown by the given \texttt{factory}.
    @param factory the contact factory having thrown the exception.
    */
   public ContactInstantiationException (ContactFactory factory) {
      super();
      this.factory = factory;
   }

   /**
    * Constructs a new contact instantiation exception with
    * the given \texttt{message}, no cause, and concerning \texttt{factory}.
    @param factory the contact factory concerned by the exception.
    @param message the error message describing the exception.
    */
   public ContactInstantiationException (ContactFactory factory,
                                         String message) {
      super (message);
      this.factory = factory;
   }

   /**
    * Constructs a new contact instantiation exception with
    * no message, the given \texttt{cause}, and concerning \texttt{factory}.
    @param factory the contact factory concerned by the exception.
    @param cause the cause of the exception.
    */
   public ContactInstantiationException (ContactFactory factory,
                                         Throwable cause) {
      super (cause);
      this.factory = factory;
   }

   /**
    * Constructs a new contact instantiation exception with
    * the given \texttt{message}, the supplied \texttt{cause}, and concerning \texttt{factory}.
    @param factory the contact factory concerned by the exception.
    @param message the error message describing the exception.
    @param cause the cause of the exception.
    */
   public ContactInstantiationException (ContactFactory factory,
                                         String message, Throwable cause) {
      super (message, cause);
      this.factory = factory;
   }

   /**
    * Returns the contact factory concerned by this exception.
    @return the contact factory concerned by this exception.
    */
   public ContactFactory getContactFactory() {
      return factory;
   }

   /**
    * Returns a short description of the exception.
    * If {@link #getContactFactory} returns \texttt{null},
    * this calls \texttt{super.toString}.  Otherwise, the
    * result is the concatenation of the strings:
    * \begin{itemize}
    * \item The name of the actual class of the exception
    * \item \texttt{": For contact factory "}
    * \item the result of {@link #getContactFactory}\texttt{.toString()}
    * \item If {@link #getMessage} is non-\texttt{null}
    *   \begin{itemize}
    *    \item \texttt{", "}
    *    \item The result of {@link #getMessage}
    *   \end{itemize}
    * \end{itemize}
    @return a string representation of the exception.
    */
   @Override
   public String toString() {
      if (factory == null)
         return super.toString();

      final StringBuilder sb = new StringBuilder (getClass().getName());
      sb.append (": For contact factory ");
      sb.append (factory.toString());
      final String msg = getMessage();
      if (msg != null)
         sb.append (", ").append (msg);
      return sb.toString();
   }
}
