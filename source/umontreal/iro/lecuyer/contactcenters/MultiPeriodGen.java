package umontreal.iro.lecuyer.contactcenters;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.randvar.ConstantGen;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.randvar.GammaAcceptanceRejectionGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.randvar.RandomVariateGenIntWithShift;
import umontreal.ssj.randvar.RandomVariateGenWithCache;
import umontreal.ssj.randvar.RandomVariateGenWithShift;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.util.TimeUnit;
import cern.colt.list.DoubleArrayList;

/**
 * Represents a random variate generator for non-stationary distributions with
 * constant parameters during each period. When a new random variate is
 * required, a random variate generator corresponding to the appropriate period
 * is selected and a value is drawn from this generator.
 * 
 * This generator supports caching by using internal
 * {@link RandomVariateGenWithCache} instances for each period. If a single
 * cache was used, the generator could recover a value whose distribution does
 * not correspond with the current period. Caching is disabled by default, and
 * can be enabled by using the {@link #setCaching} method.
 */
public class MultiPeriodGen extends RandomVariateGen implements ValueGenerator {
   private PeriodChangeEvent pce;
   private RandomVariateGen[] gens;
   private RandomVariateGenWithCache[] gensCached;
   private RandomVariateGen[] usedGens;
   private TimeUnit sourceUnit;
   private TimeUnit targetUnit;
   private double mult = 1;

   /**
    * Constructs a new multi-period random variate generator with period-change
    * event \texttt{pce}, and random variate generator \texttt{gen} for every
    * period.
    * 
    * @param pce
    *           the period-change event.
    * @param gen
    *           one random variate generator for every period.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public MultiPeriodGen (PeriodChangeEvent pce, RandomVariateGen gen) {
      super ();
      if (gen == null)
         throw new NullPointerException ("Null random variate generator");
      gens = new RandomVariateGen[pce.getNumPeriods ()];
      for (int g = 0; g < gens.length; g++)
         gens[g] = gen;
      this.pce = pce;
      usedGens = gens;
   }

   /**
    * Constructs a new multi-period random variate generator with period-change
    * event \texttt{pce}, and the per-period random variate generators
    * \texttt{gens}.
    * 
    * @param pce
    *           the period change event.
    * @param gens
    *           one random variate generator for each period.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the length of \texttt{gens} does not correspond to the
    *               number of periods.
    */
   public MultiPeriodGen (PeriodChangeEvent pce, RandomVariateGen[] gens) {
      super ();
      if (gens.length != pce.getNumPeriods ())
         throw new IllegalArgumentException ("Invalid length of gens");
      this.gens = gens.clone ();
      this.pce = pce;
      for (int p = 0; p < gens.length; p++)
         if (gens[p] == null)
            throw new NullPointerException (
                  "Null random variate generator for period " + p);
      usedGens = this.gens;
   }

   /**
    * Determines if this multiple-periods generator is caching the generated
    * values, using internal {@link RandomVariateGenWithCache} objects. By
    * default, caching is disabled for better memory utilization.
    * 
    * @return the status of the caching.
    */
   public boolean isCaching () {
      return gensCached != null;
   }

   /**
    * Sets the status of the caching for this generator.
    * 
    * @param caching
    *           the new status of the caching.
    */
   public void setCaching (boolean caching) {
      if (gensCached == null && caching) {
         gensCached = new RandomVariateGenWithCache[gens.length];
         for (int g = 0; g < gensCached.length; g++)
            gensCached[g] = new RandomVariateGenWithCache (gens[g]);
         usedGens = gensCached;
      }
      else if (gensCached != null && !caching) {
         gensCached = null;
         usedGens = gens;
      }
   }

   /**
    * Returns the period-change event associated with this object.
    * 
    * @return the associated period-change event.
    */
   public PeriodChangeEvent getPeriodChangeEvent () {
      return pce;
   }

   /**
    * Returns the random variate generators associated with this object.
    * 
    * @return the associated random variate generators.
    */
   public RandomVariateGen[] getGenerators () {
      return gens.clone ();
   }

   /**
    * Returns the random variate generators with cache used by this object. If
    * caching is disabled (the default), this method throws an
    * {@link IllegalStateException}.
    * 
    * @return the random variate generators with cache.
    * @exception IllegalStateException
    *               if caching is disabled.
    */
   public RandomVariateGenWithCache[] getGeneratorsWithCache () {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      return gensCached.clone ();
   }

   /**
    * Sets the per-period random variate generators to \texttt{gens}. Note that
    * if caching is enabled, the cache is reset when using this method.
    * 
    * @param gens
    *           the array containing the new random variate generators.
    * @exception IllegalArgumentException
    *               if the length of \texttt{gens} is invalid.
    * @exception NullPointerException
    *               if any argument is \texttt{null}.
    */
   public void setGenerators (RandomVariateGen[] gens) {
      if (gens.length != this.gens.length)
         throw new IllegalArgumentException ("Invalid length of gens");
      System.arraycopy (gens, 0, this.gens, 0, gens.length);
      if (gensCached != null)
         for (int g = 0; g < gensCached.length; g++)
            gensCached[g].setCachedGen (gens[g]);
   }

   /**
    * Returns the random variate generator corresponding to the period
    * \texttt{p}.
    * 
    * @param p
    *           index of the period.
    * @return the corresponding random variate generator.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{p} is out of bounds.
    */
   public RandomVariateGen getGenerator (int p) {
      return gens[p];
   }

   /**
    * Returns the random variate generator with cache corresponding to the
    * period \texttt{p}. If caching is disabled (the default), this method
    * throws an {@link IllegalStateException}.
    * 
    * @param p
    *           index of the period.
    * @return the corresponding random variate generator with cache.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{p} is out of bounds.
    * @exception IllegalStateException
    *               if caching is disabled.
    */
   public RandomVariateGenWithCache getGeneratorWithCache (int p) {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      return gensCached[p];
   }

   /**
    * Sets the random variate generator for period \texttt{p} to \texttt{gen}.
    * 
    * @param p
    *           the period index.
    * @param gen
    *           the new random variate generator.
    * @exception ArrayIndexOutOfBoundsException
    *               if \texttt{p} is out of bounds.
    */
   public void setGenerator (int p, RandomVariateGen gen) {
      gens[p] = gen;
      if (gensCached != null)
         gensCached[p].setCachedGen (gens[p]);
   }

   /**
    * Resets the cache of this generator, if caching is enabled. If caching is
    * disabled, this method does nothing. When the cache is reset, cached values
    * are returned upon calls to {@link #nextDouble}, until the cache is
    * exhausted. When there is no more cached value, random variates are
    * computed as usual.
    */
   public void initCache () {
      if (gensCached == null)
         return;
      for (final RandomVariateGenWithCache rvg : gensCached)
         rvg.initCache ();
   }

   /**
    * Clears the values cached by this generator. If caching is disabled, this
    * method does nothing.
    */
   public void clearCache () {
      if (gensCached == null)
         return;
      for (final RandomVariateGenWithCache rvg : gensCached)
         rvg.clearCache ();
   }

   /**
    * Returns an array containing the cache indices of each per-period
    * generator.
    * 
    * @return the array of cache indices.
    * @exception IllegalStateException
    *               if caching is disabled.
    */
   public int[] getCacheIndices () {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      final int[] ind = new int[gensCached.length];
      for (int g = 0; g < ind.length; g++)
         ind[g] = gensCached[g].getCacheIndex ();
      return ind;
   }

   /**
    * Sets the array of cache indices to \texttt{ind}.
    * 
    * @param ind
    *           the new array of cache indices.
    * @exception NullPointerException
    *               if \texttt{ind} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if \texttt{ind} has incorrect size.
    * @exception IllegalStateException
    *               if caching is disabled.
    */
   public void setCacheIndices (int[] ind) {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      if (ind.length != gensCached.length)
         throw new IllegalArgumentException ("Invalid length of ind");
      for (int g = 0; g < ind.length; g++)
         gensCached[g].setCacheIndex (ind[g]);
   }

   /**
    * Returns an array of array lists containing the values cached by each
    * period-specific generator.
    * 
    * @return the array of cached values.
    */
   public DoubleArrayList[] getCachedValues () {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      final DoubleArrayList[] vals = new DoubleArrayList[gensCached.length];
      for (int g = 0; g < vals.length; g++)
         vals[g] = gensCached[g].getCachedValues ();
      return vals;
   }

   /**
    * Sets the array list containing the cached values to \texttt{values[g]} for
    * each period-specific generator \texttt{g}. This resets the cache index to
    * the size of the given array for each generator.
    * 
    * @param values
    *           the array list of cached values.
    * @exception NullPointerException
    *               if \texttt{values} is \texttt{null}.
    */
   public void setCachedValues (DoubleArrayList[] values) {
      if (gensCached == null)
         throw new IllegalStateException ("Caching is disabled");
      if (values.length != gensCached.length)
         throw new IllegalArgumentException ("Invalid length of values");
      for (int g = 0; g < values.length; g++)
         gensCached[g].setCachedValues (values[g]);
   }

   /**
    * Returns the time unit in which the values coming from the probability
    * distribution are expressed. If the source unit is \texttt{null}, no
    * conversion of the generated values is performed. By default, this returns
    * \texttt{null}.
    * 
    * @return the source time unit.
    */
   public TimeUnit getSourceTimeUnit () {
      return sourceUnit;
   }

   /**
    * Sets the source time unit to \texttt{unit}.
    * 
    * @param unit
    *           the source time unit.
    * @see #getSourceTimeUnit
    */
   public void setSourceTimeUnit (TimeUnit unit) {
      sourceUnit = unit;
   }

   /**
    * Returns the time unit in which the values returned by {@link #nextDouble}
    * must be expressed. If the target unit is \texttt{null}, no conversion of
    * the generated values is performed. By default, this returns \texttt{null}.
    * 
    * @return the target time unit.
    */
   public TimeUnit getTargetTimeUnit () {
      return targetUnit;
   }

   /**
    * Sets the target time unit to \texttt{unit}.
    * 
    * @param unit
    *           the target time unit.
    * @see #getTargetTimeUnit
    */
   public void setTargetTimeUnit (TimeUnit unit) {
      targetUnit = unit;
   }
   
   /**
    * Returns the mean of the distribution for a random variate generator,
    * taking the shift into account.
    * This method first calls {@link Distribution#getMean()}
    * on the distribution associated with the generator.
    * If \texttt{rvg} is an instance of
    * {@link RandomVariateGenWithShift} or
    * {@link RandomVariateGenIntWithShift}, it then
    * subtracts the associated shift.
    * @param rvg the random variate generator.
    * @return the possibly shifted mean.
    */
   public static double getMean (RandomVariateGen rvg) {
      final double basicMean = rvg.getDistribution().getMean();
      if (rvg instanceof RandomVariateGenWithShift)
         return basicMean - ((RandomVariateGenWithShift)rvg).getShift();
      else if (rvg instanceof RandomVariateGenIntWithShift)
         return basicMean - ((RandomVariateGenIntWithShift)rvg).getShift();
      return basicMean;
   }
   
   /**
    * Returns the mean for period \texttt{p}.
    * @param p the index of the period.
    * @return the mean.
    */
   public double getMean (int p) {
      final RandomVariateGen rvg = getGenerator (p);
      final double basicMean = getMean (rvg)*getMult();
      if (sourceUnit != null && targetUnit != null)
         return TimeUnit.convert (basicMean, sourceUnit, targetUnit);
      else
         return basicMean;
   }
   
   /**
    * Returns the variance for the period \texttt{p}.
    * @param p the index of the period.
    * @return the variance.
    */
   public double getVariance (int p) {
      final RandomVariateGen rvg = getGenerator (p);
      final double basicVar = rvg.getDistribution().getVariance()*getMult()*getMult();
      if (sourceUnit != null && targetUnit != null) {
         final double factor = TimeUnit.convert (1, sourceUnit, targetUnit);
         return basicVar * factor * factor;
      }
      return basicVar;
   }
   
   /**
    * Returns the multiplier applied to each
    * generated random variate.
    * The default multiplier is 1.
    * @return the applied multiplier.
    */
   public double getMult() {
      return mult;
   }
   
   /**
    * Sets the multiplier applied to each
    * generated random variate to
    * \texttt{mult}.
    * @param mult the new multiplier.
    */
   public void setMult (double mult) {
      this.mult = mult;
   }

   @Override
   public double nextDouble () {
      final int p = pce.getCurrentPeriod ();
      final double v = usedGens[p].nextDouble ();
      if (sourceUnit != null && targetUnit != null)
         return TimeUnit.convert (v, sourceUnit, targetUnit)*mult;
      else
         return v*mult;
   }

   @Override
   public void nextArrayOfDouble (double[] v, int start, int n) {
      final int p = pce.getCurrentPeriod ();
      usedGens[p].nextArrayOfDouble (v, start, n);
      if (sourceUnit != null && targetUnit != null)
         for (int i = 0; i < n; i++)
            v[start + i] = TimeUnit.convert (v[start + i], sourceUnit,
                  targetUnit)*mult;
      else
         for (int i = 0; i < n; i++)
            v[start + i] *= mult;
   }

   /**
    * Returns the random stream used during the current period.
    */
   @Override
   public RandomStream getStream () {
      final int p = pce.getCurrentPeriod ();
      return gens[p].getStream ();
   }

   /**
    * Returns the distribution used during the current period.
    */
   @Override
   public Distribution getDistribution () {
      final int p = pce.getCurrentPeriod ();
      return gens[p].getDistribution ();
   }

   /**
    * Ignores the given \texttt{contact} and calls {@link #nextDouble()}.
    */
   public double nextDouble (Contact contact) {
      return nextDouble ();
   }

   public void init () {}

   /**
    * Constructs and returns a multiple-periods random variate generator using
    * the constant distribution with value \texttt{values[p]} for period
    * \texttt{p} as defined by \texttt{pce}.
    * 
    * @param pce
    *           the period-change event.
    * @param values
    *           the values of the constant.
    * @return the constructed multiple-periods generator.
    * @exception IllegalArgumentException
    *               if the length of array is less than the number of periods.
    */
   public static MultiPeriodGen createConstant (PeriodChangeEvent pce,
         double[] values) {
      if (values.length < pce.getNumPeriods ())
         throw new IllegalArgumentException (
               "Not enough constant values specified");
      final RandomVariateGen[] gens = new RandomVariateGen[pce.getNumPeriods ()];
      for (int i = 0; i < gens.length; i++)
         // final double v = values[i];
         // gens[i] = new RandomVariateGen () {
         // public double nextDouble() {
         // return v;
         // }
         // };
         gens[i] = new ConstantGen (values[i]);
      return new MultiPeriodGen (pce, gens);
   }

   /**
    * Constructs and returns a multiple-periods random variate generator using
    * the exponential distribution with rate \texttt{lambdas[p]} for period
    * \texttt{p} as defined by \texttt{pce}. The random stream \texttt{stream}
    * is used for all the periods.
    * 
    * @param pce
    *           the period-change event.
    * @param stream
    *           the random stream.
    * @param lambdas
    *           the rates for the exponential variates.
    * @return the constructed multiple-periods generator.
    * @exception IllegalArgumentException
    *               if the length of array is less than the number of periods.
    */
   public static MultiPeriodGen createExponential (PeriodChangeEvent pce,
         RandomStream stream, double[] lambdas) {
      if (lambdas.length < pce.getNumPeriods ())
         throw new IllegalArgumentException (
               "Not enough exponential lambdas specified");
      final RandomVariateGen[] gens = new RandomVariateGen[pce.getNumPeriods ()];
      for (int i = 0; i < gens.length; i++)
         gens[i] = new ExponentialGen (stream, new ExponentialDist (lambdas[i]));
      return new MultiPeriodGen (pce, gens);
   }

   /**
    * Constructs and returns a multiple-periods random variate generator using
    * the gamma distribution with parameters \texttt{alphas[p]} and
    * \texttt{lambdas[p]} for period \texttt{p} as defined by \texttt{pce}. The
    * random stream \texttt{stream} is used for all the periods. The underlying
    * gamma generators use acceptance-rejection rather than inversion for
    * efficiency.
    * 
    * @param pce
    *           the period-change event.
    * @param stream
    *           the random stream.
    * @param alphas
    *           the alpha parameters for the gamma variates.
    * @param lambdas
    *           the lambda parameters for the gamma variates.
    * @return the constructed multiple-periods generator.
    * @exception IllegalArgumentException
    *               if the length of the arrays is less than the number of
    *               periods, or the two arrays have different lengths.
    */
   public static MultiPeriodGen createGamma (PeriodChangeEvent pce,
         RandomStream stream, double[] alphas, double[] lambdas) {
      if (alphas.length != lambdas.length)
         throw new IllegalArgumentException (
               "A lambda parameter is needed for each alpha parameter");
      if (lambdas.length < pce.getNumPeriods ())
         throw new IllegalArgumentException (
               "Not enough gamma alphas and lambdas specified");
      final RandomVariateGen[] gens = new RandomVariateGen[pce.getNumPeriods ()];
      for (int i = 0; i < gens.length; i++)
         gens[i] = new GammaAcceptanceRejectionGen (stream, new GammaDist (
               alphas[i], lambdas[i]));
      return new MultiPeriodGen (pce, gens);
   }
}
