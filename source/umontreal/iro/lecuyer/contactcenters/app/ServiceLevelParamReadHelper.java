package umontreal.iro.lecuyer.contactcenters.app;

import java.util.Date;

import javax.xml.datatype.Duration;

import umontreal.iro.lecuyer.contactcenters.app.params.ServiceLevelParams;
import umontreal.iro.lecuyer.util.ArrayUtil;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.ArrayConverter;

/**
 * Provides helper methods used to read AWT and target
 * service level information from {@link ServiceLevelParams}
 * objects.
 */
public class ServiceLevelParamReadHelper {
   private String name;
   private Duration[][] awt;
   private long[][] awtMillis;
   private double[][] awtSim;
   private double[][] target;
   
   /**
    * Constructs a new service level parameter reader from
    * the parameters stored in \texttt{slp}.
    * @param slp the service level parameters.
    * @param startingDate the starting date used to transform durations
    * to times in milliseconds.
    */
   public ServiceLevelParamReadHelper (ServiceLevelParams slp, Date startingDate, TimeUnit unit) {
      if (slp == null)
         throw new NullPointerException();
      name = slp.getName ();
      if (startingDate == null)
         throw new NullPointerException();
      awt = ArrayConverter.unmarshalArray (slp.getAwt());
      try {
         ArrayUtil.checkRectangularMatrix (awt);
      }
      catch (final IllegalArgumentException iae) {
         final IllegalArgumentException iaeOut = new IllegalArgumentException
         ("Non-rectangular matrix of AWTs");
         iaeOut.initCause (iae);
         throw iaeOut;
      }
      awtMillis = new long[awt.length][awt[0].length];
      awtSim = new double[awt.length][awt[0].length];
      for (int k = 0; k < awt.length; k++)
         for (int mp = 0; mp < awt[k].length; mp++) {
            if (awt[k][mp] == null)
               throw new NullPointerException
               ("Element (" + k + ", " + mp + ") of the AWT matrix is null");
            awtMillis[k][mp] = awt[k][mp].getTimeInMillis (startingDate);
            awtSim[k][mp] = TimeUnit.convert (awtMillis[k][mp], TimeUnit.MILLISECOND, unit);
         }
      if (slp.isSetTarget ()) {
         target = ArrayConverter.unmarshalArray (slp.getTarget());
         try {
            ArrayUtil.checkRectangularMatrix (target);
         }
         catch (final IllegalArgumentException iae) {
            final IllegalArgumentException iaeOut = new IllegalArgumentException
            ("Non-rectangular matrix of targets");
            iaeOut.initCause (iae);
            throw iaeOut;
         }
         if (awt.length != 1 && target.length != 1 &&
               awt.length != target.length)
            throw new IllegalArgumentException
            ("The matrix of AWTs has " + awt.length + " rows, but the matrix of targets has "
                  + target.length + " rows; these matrices should have the same dimensions");
      }
      if (awt.length >= 1 && target != null && target.length >= 1 &&
            awt[0].length != 1 && target[0].length != 1 &&
            awt[0].length != target[0].length)
         throw new IllegalArgumentException
         ("The matrix of AWTs has " + awt[0].length + " columns, but the matrix of targets has "
               + target[0].length + " columns; these matrices should have the same dimensions");
   }
   
   /**
    * Returns the name associated with this matrix.
    * @return the name associated with this matrix.
    */
   public String getName() {
      return name;
   }
   
   /**
    * Returns the number of rows in the matrices,
    * i.e., the number of contact types.
    * @return the number of rows.
    */
   public int getRows() {
      final int nrAwt = awt == null ? 0 : awt.length;
      final int nrTarget = target == null ? 0 : target.length;
      return Math.max (nrAwt, nrTarget);
   }
   
   /**
    * Returns the number of columns in the matrix,
    * i.e., the number of main periods.
    * @return the number of columns.
    */
   public int getColumns() {
      int ncAwt = awt == null || awt.length == 0 ? 0 : awt[0].length;
      int ncTarget = target == null || target.length == 0 ? 0 : target[0].length;
      return Math.max (ncAwt, ncTarget);
   }
   
   /**
    * Returns the acceptable waiting time for contacts of type
    * \texttt{k} counted during period \texttt{mp}.
    * @param k the contact type.
    * @param mp the main period.
    * @return the AWT.
    */
   public Duration getAwt (int k, int mp) {
      if (awt.length == 0 || awt[0].length == 0)
         throw new
         ArrayIndexOutOfBoundsException();
      if (awt.length == 1 && awt[0].length == 1)
         // Single row, single column: ignore k and p
         return awt[0][0];
      else if (awt.length == 1)
         // Single row: ignore k
         return awt[0][mp];
      else if (awt[k].length == 1)
         // Single column: ignore p
         return awt[k][0];
      else
         return awt[k][mp];
   }
   
   /**
    * Converts the result of {@link #getAwt(int,int)}
    * into the time unit \texttt{unit}.
    * @param k the contact type.
    * @param mp the main period.
    * @param unit the time unit.
    * @return the AWT.
    */
   public double getAwt (int k, int mp, TimeUnit unit) {
      if (awtMillis.length == 0 || awtMillis[0].length == 0)
         throw new
         ArrayIndexOutOfBoundsException();
      final long time;
      if (awtMillis.length == 1 && awtMillis[0].length == 1)
         // Single row, single column: ignore k and p
         time = awtMillis[0][0];
      else if (awtMillis.length == 1)
         // Single row: ignore k
         time = awtMillis[0][mp];
      else if (awtMillis[k].length == 1)
         // Single column: ignore p
         time = awtMillis[k][0];
      else
         time = awtMillis[k][mp];
      return TimeUnit.convert (time, TimeUnit.MILLISECOND, unit);
   }
   
   /**
    * Returns the AWT in the default time unit.
    * @param k the contact type.
    * @param mp the main period.
    * @return the AWT.
    */
   public double getAwtDefault (int k, int mp) {
      if (awtSim.length == 0 || awtSim[0].length == 0)
         throw new
         ArrayIndexOutOfBoundsException();
      if (awtSim.length == 1 && awtSim[0].length == 1)
         // Single row, single column: ignore k and p
         return awtSim[0][0];
      else if (awtSim.length == 1)
         // Single row: ignore k
         return awtSim[0][mp];
      else if (awtSim[k].length == 1)
         // Single column: ignore p
         return awtSim[k][0];
      else
         return awtSim[k][mp];
   }
   
   /**
    * Returns the target service level for contacts of type
    * \texttt{k} counted during main period \texttt{mp}.
    * @param k the contact type.
    * @param mp the main period.
    * @return the target service level.
    */
   public double getTarget (int k, int mp) {
      if (target == null || target.length == 0 || target[0].length == 0)
         return 0;
      if (target.length == 1 && target[0].length == 1)
         // Single row, single column: ignore k and p
         return target[0][0];
      else if (target.length == 1)
         // Single row: ignore k
         return target[0][mp];
      else if (target[k].length == 1)
         // Single column: ignore p
         return target[k][0];
      else
         return target[k][mp];
   }
   
   /**
    * Sets the target service level for contacts of type \texttt{k}
    * counted during main period \texttt{mp}.
    * @param k the contact type.
    * @param mp the main periods.
    * @param t the target service level.
    */
   public void setTarget (int k, int mp, double t) {
      if (target == null || target.length == 0 || target[0].length == 0) 
          throw new IllegalArgumentException("Impossible to set new service level target for call type " + 
                  k + " at main period " + mp);
      if (target.length == 1)
         // Single row: ignore k
         target[0][mp] = t;
      else if (target[k].length == 1)
         // Single column: ignore p
         target[k][0] = t;
      else
         target[k][mp] = t;
   }
}
