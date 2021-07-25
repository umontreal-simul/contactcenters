package umontreal.iro.lecuyer.contactcenters;

/**
 * Represents an object having a name.
 */
public interface Named {
   /**
    * Returns the name associated with this object. If no name was set, this
    * must return an empty string, not \texttt{null}.
    * 
    * @return the name of this object.
    */
   public String getName ();

   /**
    * Sets the name of this object to \texttt{name}. The given name cannot be
    * \texttt{null} and the implementation can throw an
    * {@link UnsupportedOperationException} if the name is read-only.
    * 
    * @param name
    *           the new name of the object.
    * @exception UnsupportedOperationException
    *               if the name cannot be changed.
    * @exception NullPointerException
    *               if \texttt{name} is \texttt{null}.
    */
   public void setName (String name);
}
