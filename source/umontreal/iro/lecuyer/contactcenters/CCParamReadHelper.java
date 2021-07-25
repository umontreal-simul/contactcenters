package umontreal.iro.lecuyer.contactcenters;

import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.contactcenters.params.MultiPeriodGenParams;
import umontreal.iro.lecuyer.contactcenters.params.MultiPeriodGenParams.PeriodGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.xmlbind.DistributionCreationException;
import umontreal.iro.lecuyer.xmlbind.GeneratorCreationException;
import umontreal.iro.lecuyer.xmlbind.ParamReadHelper;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;

/**
 * Provides helper methods to convert parameter objects into usable objects.
 */
public class CCParamReadHelper {
   private static String getPeriodNameDist (int p) {
      if (p < 0)
         return "default distribution";
      if (p == 0)
         return "distribution for preliminary period";
      if (p == Integer.MAX_VALUE)
         return "distribution for wrap-up period";
      return "distribution for period " + p;
   }

   private static String getPeriodNameGen (int p) {
      if (p < 0)
         return "default generator";
      if (p == 0)
         return "generator for preliminary period";
      if (p == Integer.MAX_VALUE)
         return "generator for wrap-up period";
      return "generator for period " + p;
   }
   
   private static boolean hasSingleDist (MultiPeriodGenParams par) {
      boolean def = par.isSetDefaultGen ();
      boolean pre = par.isSetPreGen ();
      boolean wrap = par.isSetWrapGen ();
      if (par.getPeriodGen ().isEmpty ())
         // At most one of theese booleans can be true
         return def ^ pre ^ wrap;
      else {
         if (def || pre || wrap)
            return false;
         return par.getPeriodGen ().size () == 1;
      }
   }

   /**
    * Calls
    * {@link ParamReadHelper#createGenerator(RandomVariateGenParams,RandomStream)},
    * but sets up \texttt{rvgp} to inherit unspecified information distribution
    * class, generator class, and shift, from \texttt{mpgp}. This method also
    * wraps exceptions thrown by the generator-creation method in order to add
    * information about the period of the error.
    * 
    * @param mpgp
    *           the multiple-period generator parameters.
    * @param p
    *           the period index.
    * @param rvgp
    *           the random-variate generator parameters.
    * @param stream
    *           the random stream.
    * @return the random variate generator.
    * @throws DistributionCreationException
    *            if an exception occurred during the creation of the
    *            distribution.
    * @throws GeneratorCreationException
    *            if an exception occurred during the creation of the generator.
    */
   private static RandomVariateGen createGenerator (MultiPeriodGenParams mpgp,
         int p, RandomVariateGenParams rvgp, RandomStream stream)
         throws DistributionCreationException, GeneratorCreationException {
      if (rvgp == null)
         throw new DistributionCreationException (
               "No parameters available for the " + getPeriodNameDist (p));
      boolean unsetDistClass = false;
      boolean unsetGenClass = false;
      boolean unsetEstPar = false;
      boolean unsetShift = false;
      if (!rvgp.isSetDistributionClass ()) {
         unsetDistClass = true;
         rvgp.setDistributionClass (mpgp.getDistributionClass ());
      }
      if (!rvgp.isSetGeneratorClass ()) {
         unsetGenClass = true;
         rvgp.setGeneratorClass (mpgp.getGeneratorClass ());
      }
      if (!rvgp.isSetEstimateParameters ()) {
         unsetEstPar = true;
         rvgp.setEstimateParameters (mpgp.isEstimateParameters ());
      }
      if (!rvgp.isSetShift () && mpgp.isSetShift ()) {
         unsetShift = true;
         rvgp.setShift (mpgp.getShift ());
      }
      try {
         return ParamReadHelper.createGenerator (rvgp, stream);
      }
      catch (final DistributionCreationException dce) {
         if (hasSingleDist (mpgp))
            throw dce;
         final DistributionCreationException e = new DistributionCreationException (
               "Error creating " + getPeriodNameDist (p));
         e.initCause (dce);
         throw e;
      }
      catch (final GeneratorCreationException gce) {
         if (hasSingleDist (mpgp))
            throw gce;
         final GeneratorCreationException e = new GeneratorCreationException (
               "Error creating " + getPeriodNameGen (p));
         e.initCause (gce);
         throw e;
      }
      finally {
         if (unsetDistClass)
            rvgp.setDistributionClass (null);
         if (unsetGenClass)
            rvgp.setGeneratorClass (null);
         if (unsetEstPar)
            rvgp.unsetEstimateParameters ();
         if (unsetShift)
            rvgp.unsetShift ();
      }
   }

   private static boolean estimateParameters (MultiPeriodGenParams mpgp, int p,
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      if (rvgp == null)
         throw new DistributionCreationException (
               "No parameters available for the " + getPeriodNameDist (p));
      boolean unsetDistClass = false;
      boolean unsetEstPar = false;
      boolean unsetShift = false;
      if (!rvgp.isSetDistributionClass ()) {
         unsetDistClass = true;
         rvgp.setDistributionClass (mpgp.getDistributionClass ());
      }
      if (!rvgp.isSetEstimateParameters ()) {
         unsetEstPar = true;
         rvgp.setEstimateParameters (mpgp.isEstimateParameters ());
      }
      if (!rvgp.isSetShift () && mpgp.isSetShift ()) {
         unsetShift = true;
         rvgp.setShift (mpgp.getShift ());
      }
      try {
         return ParamReadHelper.estimateParameters (rvgp);
      }
      catch (final DistributionCreationException dce) {
         if (hasSingleDist (mpgp))
            throw dce;
         final DistributionCreationException e = new DistributionCreationException (
               "Error creating " + getPeriodNameDist (p));
         e.initCause (dce);
         throw e;
      }
      finally {
         if (unsetDistClass)
            rvgp.setDistributionClass (null);
         if (unsetEstPar)
            rvgp.unsetEstimateParameters ();
         if (unsetShift)
            rvgp.unsetShift ();
      }
   }

   private static double getMean (MultiPeriodGenParams mpgp, int p,
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      if (rvgp == null)
         throw new DistributionCreationException (
               "No parameters available for the " + getPeriodNameDist (p));
      boolean unsetDistClass = false;
      boolean unsetEstPar = false;
      boolean unsetShift = false;
      if (!rvgp.isSetDistributionClass ()) {
         unsetDistClass = true;
         rvgp.setDistributionClass (mpgp.getDistributionClass ());
      }
      if (!rvgp.isSetEstimateParameters ()) {
         unsetEstPar = true;
         rvgp.setEstimateParameters (mpgp.isEstimateParameters ());
      }
      if (!rvgp.isSetShift () && mpgp.isSetShift ()) {
         unsetShift = true;
         rvgp.setShift (mpgp.getShift ());
      }
      try {
         return ParamReadHelper.getMean (rvgp);
      }
      catch (final DistributionCreationException dce) {
         if (hasSingleDist (mpgp))
            throw dce;
         final DistributionCreationException e = new DistributionCreationException (
               "Error creating " + getPeriodNameDist (p));
         e.initCause (dce);
         throw e;
      }
      finally {
         if (unsetDistClass)
            rvgp.setDistributionClass (null);
         if (unsetEstPar)
            rvgp.unsetEstimateParameters ();
         if (unsetShift)
            rvgp.unsetShift ();
      }
   }

   private static double getVariance (MultiPeriodGenParams mpgp, int p,
         RandomVariateGenParams rvgp) throws DistributionCreationException {
      if (rvgp == null)
         throw new DistributionCreationException (
               "No parameters available for the " + getPeriodNameDist (p));
      boolean unsetDistClass = false;
      boolean unsetEstPar = false;
      boolean unsetShift = false;
      if (!rvgp.isSetDistributionClass ()) {
         unsetDistClass = true;
         rvgp.setDistributionClass (mpgp.getDistributionClass ());
      }
      if (!rvgp.isSetEstimateParameters ()) {
         unsetEstPar = true;
         rvgp.setEstimateParameters (mpgp.isEstimateParameters ());
      }
      if (!rvgp.isSetShift () && mpgp.isSetShift ()) {
         unsetShift = true;
         rvgp.setShift (mpgp.getShift ());
      }
      try {
         return ParamReadHelper.getVariance (rvgp);
      }
      catch (final DistributionCreationException dce) {
         if (hasSingleDist (mpgp))
            throw dce;
         final DistributionCreationException e = new DistributionCreationException (
               "Error creating " + getPeriodNameDist (p));
         e.initCause (dce);
         throw e;
      }
      finally {
         if (unsetDistClass)
            rvgp.setDistributionClass (null);
         if (unsetEstPar)
            rvgp.unsetEstimateParameters ();
         if (unsetShift)
            rvgp.unsetShift ();
      }
   }

   /**
    * Returns the mean value of the distribution for the main period \texttt{p}.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @param p
    *           the index of the main period.
    * @return the mean.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getMeanForPeriod (MultiPeriodGenParams mpgp, int p)
         throws DistributionCreationException {
      if (mpgp.getPeriodGen ().size () < p)
         return getMean (mpgp, p + 1, mpgp.getPeriodGen ().get (p));
      return getMean (mpgp, -1, mpgp.getDefaultGen ());
   }

   /**
    * Returns the variance value of the distribution for the main period
    * \texttt{p}.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @param p
    *           the index of the main period.
    * @return the variance.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getVarianceForPeriod (MultiPeriodGenParams mpgp, int p)
         throws DistributionCreationException {
      if (mpgp.getPeriodGen ().size () < p)
         return getVariance (mpgp, p + 1, mpgp.getPeriodGen ().get (p));
      return getVariance (mpgp, p + 1, mpgp.getDefaultGen ());
   }

   /**
    * Returns the mean for the preliminary period, if parameters are set for
    * this period.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @return the mean.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getMeanPre (MultiPeriodGenParams mpgp)
         throws DistributionCreationException {
      if (!mpgp.isSetPreGen ())
         throw new DistributionCreationException (
               "No parameters available for the preliminary period");
      return getMean (mpgp, 0, mpgp.getPreGen ());
   }

   /**
    * Returns the variance for the preliminary period, if parameters are set for
    * this period.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @return the variance.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getVariancePre (MultiPeriodGenParams mpgp)
         throws DistributionCreationException {
      if (!mpgp.isSetPreGen ())
         throw new DistributionCreationException (
               "No parameters available for the preliminary period");
      return getVariance (mpgp, 0, mpgp.getPreGen ());
   }

   /**
    * Returns the mean for the wrap-up period, if parameters are set for this
    * period.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @return the mean.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getMeanWrap (MultiPeriodGenParams mpgp)
         throws DistributionCreationException {
      if (!mpgp.isSetWrapGen ())
         throw new DistributionCreationException (
               "No parameters available for the wrap-up period");
      return getMean (mpgp, Integer.MAX_VALUE, mpgp.getWrapGen ());
   }

   /**
    * Returns the variance for the wrap-up period, if parameters are set for
    * this period.
    * 
    * @param mpgp
    *           the parameters of the random variate generator for multiple
    *           periods.
    * @return the variance.
    * @throws DistributionCreationException
    *            if an error occurs during the creation of the distribution.
    */
   public static double getVarianceWrap (MultiPeriodGenParams mpgp)
         throws DistributionCreationException {
      if (!mpgp.isSetWrapGen ())
         throw new DistributionCreationException (
               "No parameters available for the wrap-up period");
      return getVariance (mpgp, Integer.MAX_VALUE, mpgp.getWrapGen ());
   }

   /**
    * Constructs and returns a random variate generator for multiple periods
    * using the parameters in \texttt{mpgp}, the random stream
    * \texttt{stream}, and the period-change event \texttt{pce}.
    * This method constructs one random variate generator for each period, and
    * uses these generators to create a multiple-periods random variate
    * generator.
    * 
    * The parameters for the preliminary and wrap-up periods are obtained using
    * {@link MultiPeriodGenParams#getPreGen()}, and
    * {@link MultiPeriodGenParams#getWrapGen()}, respectively. If no
    * parameters are given for the preliminary [wrap-up] period, the generator
    * of the first [last] main period is used. For main periods, the method
    * obtains parameters from {@link MultiPeriodGenParams#getPeriodGen()}. If
    * the number of main periods defined by \texttt{pce} exceeds the number of
    * period-specific sets of parameters, the method constructs the default
    * generator based on parameters in
    * {@link MultiPeriodGenParams#getDefaultGen()}, and uses it for the
    * remaining main periods.
    * 
    * When constructing a period-specific generator, 
    * this method replaces the missing
    * distribution class, generator class, and shift by the default value given
    * by \texttt{mpgp}.
    * 
    * @param mpgp
    *           the parameters for the multiple-periods generator.
    * @param stream
    *           the random stream.
    * @param pce
    *           the period-change event.
    * @return the multiple-periods random variate generator.
    * @throws DistributionCreationException
    *            if an exception occurred during the construction of a
    *            period-specific distribution.
    * @throws GeneratorCreationException
    *            if an exception occurred during the construction of a
    *            period-specific generator.
    */
   public static MultiPeriodGen createGenerator (MultiPeriodGenParams mpgp,
         RandomStream stream, PeriodChangeEvent pce)
         throws DistributionCreationException, GeneratorCreationException {
      TimeUnit unit;
      if (mpgp.isSetUnit ())
         try {
            unit = TimeUnit.valueOf (mpgp.getUnit ().name ());
         }
         catch (final IllegalArgumentException iae) {
            throw new GeneratorCreationException ("Invalid time unit: "
                  + mpgp.getUnit ());
         }
      else
         unit = null;
      final RandomVariateGen[] rvg = new RandomVariateGen[pce.getNumPeriods ()];
      if (mpgp.isSetPreGen ())
         rvg[0] = createGenerator (mpgp, 0, mpgp.getPreGen (), stream);
      if (mpgp.isSetWrapGen ())
         rvg[rvg.length - 1] = createGenerator (mpgp, Integer.MAX_VALUE, mpgp
               .getWrapGen (), stream);
      int p = 1;
      for (int i = 0; i < mpgp.getPeriodGen ().size () && p < rvg.length - 1; i++) {
         final MultiPeriodGenParams.PeriodGen rvgp = mpgp.getPeriodGen ().get (i);
         rvg[p] = createGenerator (mpgp, p, rvgp, stream);
         final int p0 = p++;
         for (int r = 1; r < rvgp.getRepeat () && p < rvg.length - 1; r++)
            rvg[p++] = rvg[p0];
      }
      if (p < rvg.length - 1) {
         final RandomVariateGen defaultGen = createGenerator (mpgp, -1, mpgp
               .getDefaultGen (), stream);
         for (int i = p; i < rvg.length - 1; i++)
            rvg[i] = defaultGen;
      }

      if (rvg[0] == null)
         rvg[0] = rvg[1];
      if (rvg[rvg.length - 1] == null)
         rvg[rvg.length - 1] = rvg[rvg.length - 2];

      final MultiPeriodGen mpg = new MultiPeriodGen (pce, rvg);
      mpg.setSourceTimeUnit (unit);
      mpg.setMult (mpgp.getMult ());
      return mpg;
   }
   
   /**
    * Replaces, in \texttt{mpg}, any group of successive period-specific
    * generators having equivalent parameters with
    * a single per-period generator with a \texttt{repeat}
    * attribute.
    * This can be used to reduce the amount of information in
    * the XML output generated by marshalling \texttt{mpg}, or
    * the amount of memory taken by the random variate generator
    * created from \texttt{mpg}.
    * This method considers that two numerical parameters $a$ and $b$
    * are equal if
    * if $|a-b|<$~\texttt{tol}. 
    * @param mpg the parameters of the multiple-periods generator..
    * @param tol the tolerance used for comparing numbers.
    */
   public static void compactPeriods (MultiPeriodGenParams mpg, double tol) {
//      MultiPeriodGenParams mpgOut = new MultiPeriodGenParams();
//      if (mpg.isSetDistributionClass ())
//         mpgOut.setDistributionClass (mpg.getDistributionClass ());
//      if (mpg.isSetGeneratorClass ())
//         mpgOut.setGeneratorClass (mpg.getGeneratorClass ());
//      if (mpg.isEstimateParameters ())
//         mpgOut.setEstimateParameters (mpg.isEstimateParameters ());
//      if (mpg.isSetMult ())
//         mpgOut.setMult (mpg.getMult ());
//      if (mpg.isSetShift ())
//         mpgOut.setShift (mpg.getShift ());
//      if (mpg.isSetUnit ())
//         mpgOut.setUnit (mpg.getUnit ());
//
//      if (mpg.isSetDefaultGen ())
//         mpgOut.setDefaultGen (mpg.getDefaultGen ());
//      if (mpg.isSetPreGen ())
//         mpgOut.setPreGen (mpg.getPreGen ());
//      if (mpg.isSetWrapGen ())
//         mpgOut.setWrapGen (mpg.getWrapGen ());
      
      final int np = mpg.getPeriodGen ().size ();
      final List<PeriodGen> pgOut = new ArrayList<PeriodGen>();
      PeriodGen prev = null;
      for (int p = 0; p < np; p++) {
         final PeriodGen ppar = mpg.getPeriodGen ().get (p);
         if (prev != null && ParamReadHelper.sameGenerators (prev, ppar, tol))
            prev.setRepeat (prev.getRepeat () + 1);
         else {
            pgOut.add (ppar);
            prev = ppar;
         }
      }
      mpg.getPeriodGen ().clear ();
      mpg.getPeriodGen ().addAll (pgOut);
   }

   /**
    * Estimates parameters for every period-specific generator specified by
    * \texttt{mpgp}. This method calls
    * {@link ParamReadHelper#estimateParameters(RandomVariateGenParams)}
    * for each period-specific generator to estimate parameters.
    * 
    * @param mpgp
    *           the parameters for the multiple-period generator.
    * @return \texttt{true} if and only if at least one call to
    *                  {@link ParamReadHelper#estimateParameters(RandomVariateGenParams)}
    *                  returned \texttt{true}.
    * @throws DistributionCreationException
    */
   public static boolean estimateParameters (MultiPeriodGenParams mpgp)
         throws DistributionCreationException {
      if (mpgp == null)
         return false;
      boolean ret = false;
      if (mpgp.isSetPreGen ())
         ret |= estimateParameters (mpgp, 0, mpgp.getPreGen ());
      if (mpgp.isSetWrapGen ())
         ret |= estimateParameters (mpgp, Integer.MAX_VALUE, mpgp.getWrapGen ());
      if (mpgp.isSetDefaultGen ())
         ret |= estimateParameters (mpgp, -1, mpgp.getDefaultGen ());
      int p = 1;
      for (final RandomVariateGenParams rvgp : mpgp.getPeriodGen ())
         ret |= estimateParameters (mpgp, p++, rvgp);
      return ret;
   }
   
   private static boolean sameGenerators (MultiPeriodGenParams mpgp1,
         RandomVariateGenParams rvgp1,
         MultiPeriodGenParams mpgp2,
         RandomVariateGenParams rvgp2, double tol) {
      if (rvgp1 == null && rvgp2 == null)
         return true;
      if (rvgp1 == null || rvgp2 == null)
         return false;

      boolean unsetDistClass1 = false;
      boolean unsetGenClass1 = false;
      boolean unsetEstPar1 = false;
      boolean unsetShift1 = false;
      if (!rvgp1.isSetDistributionClass ()) {
         unsetDistClass1 = true;
         rvgp1.setDistributionClass (mpgp1.getDistributionClass ());
      }
      if (!rvgp1.isSetGeneratorClass ()) {
         unsetGenClass1 = true;
         rvgp1.setGeneratorClass (mpgp1.getGeneratorClass ());
      }
      if (!rvgp1.isSetEstimateParameters ()) {
         unsetEstPar1 = true;
         rvgp1.setEstimateParameters (mpgp1.isEstimateParameters ());
      }
      if (!rvgp1.isSetShift () && mpgp1.isSetShift ()) {
         unsetShift1 = true;
         rvgp1.setShift (mpgp1.getShift ());
      }
      
      final boolean unsetDistClass2 = false;
      final boolean unsetGenClass2 = false;
      final boolean unsetEstPar2 = false;
      boolean unsetShift2 = false;
      if (!rvgp2.isSetDistributionClass ()) {
         unsetDistClass1 = true;
         rvgp2.setDistributionClass (mpgp2.getDistributionClass ());
      }
      if (!rvgp2.isSetGeneratorClass ()) {
         unsetGenClass1 = true;
         rvgp2.setGeneratorClass (mpgp2.getGeneratorClass ());
      }
      if (!rvgp2.isSetEstimateParameters ()) {
         unsetEstPar1 = true;
         rvgp2.setEstimateParameters (mpgp2.isEstimateParameters ());
      }
      if (!rvgp2.isSetShift () && mpgp2.isSetShift ()) {
         unsetShift2 = true;
         rvgp2.setShift (mpgp2.getShift ());
      }
      
      try {
         return ParamReadHelper.sameGenerators (rvgp1, rvgp2, tol);
      }
      finally {
         if (unsetDistClass1)
            rvgp1.setDistributionClass (null);
         if (unsetGenClass1)
            rvgp1.setGeneratorClass (null);
         if (unsetEstPar1)
            rvgp1.unsetEstimateParameters ();
         if (unsetShift1)
            rvgp1.unsetShift ();

         if (unsetDistClass2)
            rvgp2.setDistributionClass (null);
         if (unsetGenClass2)
            rvgp2.setGeneratorClass (null);
         if (unsetEstPar2)
            rvgp2.unsetEstimateParameters ();
         if (unsetShift2)
            rvgp2.unsetShift ();
      }
   }
   
   private static int getNumPeriods (List<PeriodGen> periods) {
      int np = 0;
      for (final PeriodGen pg : periods)
         np += pg.getRepeat ();
      return np;
   }
   
   /**
    * Determines if the two generators \texttt{mpg1}, and
    * \texttt{mpg2} are equivalent, i.e., if they
    * use the same distribution and parameters for each period.
    * This method uses {@link ParamReadHelper#sameGenerators(RandomVariateGenParams,RandomVariateGenParams,double)}
    * to compare period-specific generators.
    * @param mpg1 the first multiple-periods generators.
    * @param mpg2 the second multiple-periods generators.
    * @param tol the tolerance used to compare numbers.
    * @return \texttt{true} if and only if the two generators are
    * equivalent.
    */
   public static boolean sameGenerators (MultiPeriodGenParams mpg1, MultiPeriodGenParams mpg2, double tol) {
      if (mpg1 == null && mpg2 == null)
         return true;
      if (mpg1 == null || mpg2 == null)
         return false;
      if (!sameGenerators (mpg1, mpg1.getPreGen (), mpg2, mpg2.getPreGen (), tol))
         return false;
      if (!sameGenerators (mpg1, mpg1.getWrapGen (), mpg2, mpg2.getWrapGen (), tol))
         return false;
      if (!sameGenerators (mpg1, mpg1.getDefaultGen (), mpg2, mpg2.getDefaultGen (), tol))
         return false;
      
      final int np1 = getNumPeriods (mpg1.getPeriodGen ());
      final int np2 = getNumPeriods (mpg2.getPeriodGen ());
      if (np1 != np2)
         return false;
      PeriodGen prev1 = null;
      PeriodGen prev2 = null;
      int p1 = 0, p2 = 0;
      int r1 = 0, r2 = 0;
      for (int p = 0; p < np1; p++) {
         if (prev1 == null || r1 <= 0) {
            prev1 = mpg1.getPeriodGen ().get (p1++);
            r1 = prev1.getRepeat () - 1;
         }
         if (prev2 == null || r2 <= 0) {
            prev2 = mpg2.getPeriodGen ().get (p2++);
            r2 = prev2.getRepeat ();
         }
         if (!sameGenerators (mpg1, prev1, mpg2, prev2, tol))
            return false;
      }
      return true;
   }
}
