package umontreal.iro.lecuyer.contactcenters.msk.spi;

import umontreal.iro.lecuyer.contactcenters.contact.ContactArrivalProcess;
import umontreal.iro.lecuyer.contactcenters.msk.model.ArrivalProcessCreationException;
import umontreal.iro.lecuyer.contactcenters.msk.model.ArrivalProcessManager;
import umontreal.iro.lecuyer.contactcenters.msk.model.CallCenter;
import umontreal.iro.lecuyer.contactcenters.msk.params.ArrivalProcessParams;

/**
 * Provdes a method to create an arrival process from the
 * user-specified parameters.
 */
public interface ArrivalProcessFactory {
   /**
    * Constructs and returns an arrival process
    * for the call center model \texttt{cc} and
    * the arrival process parameters \texttt{par}.
    * This method uses the {@link ArrivalProcessParams#getType()}
    * to get the type string of the arrival process given by the user,
    * and returns an arrival process if it supports that particular type
    * identifier.
    * Otherwise, it returns \texttt{null}.
    * An arrival-process-creation exception is thrown only if
    * the given arrival process is supported by the
    * implementation, but some error occurs during
    * the construction of the arrival process, e.g., invalid
    * parameters.
    * @param cc the call center model.
    * @param par the router's parameters.
    * @return the new router, or \texttt{null}.
    */
   public ContactArrivalProcess createArrivalProcess (CallCenter cc, ArrivalProcessManager am, ArrivalProcessParams par) throws ArrivalProcessCreationException;
   
   /**
    * Estimates the parameters of an arrival process using
    * the data given in the 2D array \texttt{data}.
    * The given array is a $N\times P$ matrix where $N$ is the
    * number of vectors of observations, and $P$ is the number of
    * main periods.
    * If estimation is successful,
    * the method updates the parameter object \texttt{par}
    * with the estimated parameters, and returns \texttt{true}.
    * Otherwise, it throws an illegal-argument exception.
    * The method returns \texttt{false} if it does not recognize the
    * type of arrival process described by \texttt{par}.
    * @param par the parameters of the arrival process.
    * @param data the 2D array of vectors of observations.
    * @param periodDuration the duration of main periods, in
    * simulation time units.
    * @return the sucess indicator of the estimation.
    * @exception IllegalArgumentException if an error occurs
    * during parameter estimation.
    */
   public boolean estimateParameters (ArrivalProcessParams par, int[][] data, double periodDuration);
}
