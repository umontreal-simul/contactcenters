/**
 *
 */
package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.ssj.util.PrintfFormat;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;
import java.util.Arrays;

/**
 * FOR NOW, THIS CLASS IS UNUSED. THE BUSYNESS FACTOR IS SET
 * AND KEPT IN CLASS ContactArrivalProcess.
 * 
 * This class implements the busyness factors that allows us
 * to stretch or compress arrival rates or counts. It can be a
 * global factor or a set of specific factors for each period of the day
 * or both. 
 * @author Richard Simard
 *
 */
public class Busyness {
   private double m_b;      // common busyness factor
   private double Bar[];    // busyness factor of each period
   private double bMean = -1;
   private RandomVariateGenParams m_bgenParams;
   
   
   /**
    * Constructor with common busyness factor $b$.
    * It must be greater than or equal to 0. The default value is 1.
    *
    @param b busyness factor
    */
   public Busyness (double b) {
		setFactor (b);
		Bar = null;
	}

   /**
    * Constructor with busyness factors $B[j]$ for each period $j$.
    * Each factor must be greater than or equal to 0.
    * The factor of the preliminary period $B[0]$ and the  wrap-up
    * period $B[p+1]$  are  usually set to 0.
    *
    @param B busyness factors for each period
    */
   public Busyness (double[] B) {
		setFactors (B);
		m_b = 1.0;
	}

   /**
    * Constructor with busyness factors $B[j]$ for each period $j$,
    * and common busyness factor $b$.
    * Each factor must be greater than or equal to 0.
    *
    @param b common busyness factor
    @param Bs busyness factors for each period
   */
   public Busyness (double b, double[] Bs) {
		setFactors (Bs);
		setFactor (b);
	}

	/**
    * Returns the common busyness factor $B$.
    *
    @return the common busyness factor.
    */
   public double getFactor() {
      return m_b;
   }

   /**
    * Sets the common busyness factor to $b$.
    * It must be greater than or equal to 0, and
    * defaults to 1.
    *
    @param b common busyness factor
    */
   public void setFactor (double b) {
      if (b < 0)
         throw new IllegalArgumentException
             ("b must not be negative");
      m_b = b;
   }

	/**
    * Returns the busyness factor $B_j$ of period $j$.
    * If it is undefined, this method returns 1.
    @param j index of period
    @return the busyness factor of period $j$.
    */
   public double getFactor(int j) {
     	if (Bar == null)
   		return 1.0;
      if (j < 0 || j >= Bar.length) {
         System.err.println ("Busyness index " + j + " outside array limits");
         return 1.0;
      }
     	return Bar[j];
   }

   /**
    * Sets the busyness factor to $B[j]$ for each period $j$.
    * Each must be greater than or equal to 0.
    * The factor of the preliminary period $B[0]$ and the  wrap-up
    * period $B[p+1]$  are usually set to 0.
    @param B busyness factors
    */
   public void setFactors (double[] B) {
		int n = B.length;
		Bar = new double[n];
		for (int j = 0; j < n; ++j) {
			Bar[j] = B[j];
         if (B[j] < 0)
            throw new IllegalArgumentException
                ("B[" + j + "] is negative");
		}
   }

   /**
    * Returns the expected value of the busyness factor.
    * @return the expected value of the busyness factor.
    */
   public double getExpectedFactor () {
      return bMean;
   }

   /**
    * Sets the expected busyness factor to \texttt{bMean}.
    * @param bMean the new value of the expectation.
    * @exception IllegalArgumentException if \texttt{bMean} is negative.
    */
   public void setExpectedFactor (double bMean) {
      if (bMean < 0)
         throw new IllegalArgumentException
         ("bMean < 0");
      this.bMean = bMean;
   }

	/**
    * Returns the total busyness of period $j$. It is the product of the
    * common factor $B$ with the specific factor $B_j$ of period $j$.
    @param j index of period
    @return the busyness multiplier of period $j$.
    */
   public double getBusyness(int j) {
      return m_b * getFactor(j);
   }

   /**
    * Returns the random number generator used for busyness.
    */
   public RandomVariateGenParams getBusynessGen () {
   	return m_bgenParams;
   }

   /**
    * Sets the random number generator for busyness to \texttt{gen}.
    * @param gen
    */
   public void setBusynessGen (RandomVariateGenParams gen) {
   	m_bgenParams = gen;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder ("Busyness factors:");
      sb.append (PrintfFormat.NEWLINE);
      sb.append ("common B  = ");
      sb.append (m_b);
      sb.append (PrintfFormat.NEWLINE);
      sb.append ("period Bs = ");
      if (Bar != null) {
         sb.append (Arrays.toString(Bar));
      } else {
         sb.append ("null");
      }
      sb.append (PrintfFormat.NEWLINE);
      return sb.toString();
   }
}
