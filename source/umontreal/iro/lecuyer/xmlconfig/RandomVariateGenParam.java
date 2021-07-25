package umontreal.iro.lecuyer.xmlconfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import umontreal.ssj.probdist.ConstantDist;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.DistributionFactory;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.probdist.PiecewiseLinearEmpiricalDist;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.randvar.RandomVariateGenInt;
import umontreal.ssj.randvar.RandomVariateGenIntWithShift;
import umontreal.ssj.randvar.RandomVariateGenWithShift;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.util.ClassFinder;
import umontreal.ssj.util.Introspection;
import umontreal.ssj.util.NameConflictException;
import umontreal.iro.lecuyer.util.StringConvert;
import umontreal.ssj.util.TimeUnit;
import umontreal.iro.lecuyer.util.UnsupportedConversionException;

/**
 * Stores the parameters of a probability distribution to create the
 * distribution object or a matching random variate generator at a later time.
 * Three parameters need to be defined to use this object: the class of the
 * probability distribution, a string defining the parameters as given to the
 * distribution class' constructor, and the class of the random variate
 * generator. Alternatively, the parameters of the distribution can be replaced
 * by an array of values for parameters to be estimated by maximum likeliihood.
 * One can also specify a location parameter $\ell$ for the generated values.
 * This parameter, which defaults to 0, is subtracted from any generated value
 * and added to every value specified as data to estimate parameters. It does
 * not affect explicitly-specified parameters.
 * 
 * The {@link #createDistribution} and {@link #createDistributionInt} methods
 * can be used to construct a distribution object whereas the
 * {@link #createGenerator} and {@link #createGeneratorInt} methods construct a
 * generator.
 * 
 * In an XML element, the \texttt{distributionClass} attribute is required to
 * correspond to the name of a class implementing the {@link Distribution}
 * interface. The nested text of the element is used as an array of parameters
 * the class tries to pass to a constructor inside the distribution class. To
 * use MLE, one must specify the data in a \texttt{data} element or
 * \texttt{dataURL} attribute. \texttt{data} is an array of double-precision
 * values while \texttt{dataURL} points to a resource containing the data, one
 * value per line. Note that the file is searched in the same directory as the
 * XML parameter file. For discrete distributions on the integers, the given
 * values are rounded to the the nearest integers.
 * 
 * The \texttt{data} element or the \texttt{dataFile} attribute can also be used
 * for empirical distributions. More specifically, when
 * \texttt{distributionClass} is set to {@link EmpiricalDist} or
 * {@link PiecewiseLinearEmpiricalDist}, the given data is sorted and used
 * directly by these distributions. The shift $\ell$ is given via the
 * \texttt{shift} attribute.
 * 
 * The class of the random variate generator can optionally be changed by
 * providing a compatible class name in the \texttt{generatorClass} attribute.
 * This allows the method for generating the variates to be selected from a
 * parameter file.
 * 
 * For example, the element
 * \begin{verbatim}
 * 
 * <?import umontreal.ssj.probdist.*?>
 * <?import umontreal.ssj.randvar.*?>
 * ...
 *    <rvg distributionClass="GammaDist"
 *         generatorClass="GammaAcceptanceRejectionGen">
 *       32.3, 25.2
 *    </rvg>
 * \end{verbatim}
 * can be maped to a gamma distribution with $\alpha=32.3$ and
 * $\lambda=25.2$ and a gamma variate generator using acceptance-rejection.
 */
public class RandomVariateGenParam extends AbstractParam implements
      StorableParam, Cloneable {
   private Class<? extends Distribution> distClass;
   private Class<? extends RandomVariateGen> genClass;
   private String params;
   private ArrayParam dataParam;
   private double[] data;
   private double shift = 0;
   private TimeUnit unit;

   // Generated elements
   private Distribution dist;

   /**
    * Default constructor for parameter reader.
    */
   public RandomVariateGenParam () {}

   /**
    * Constructs a new distribution parameter object with distribution class
    * \texttt{distClass} and parameter string \texttt{params}. The given
    * distribution class must be a subclass of {@link Distribution} or
    * {@link DiscreteDistributionInt} and the parameter string must correspond
    * to the arguments given to the constructor of the distribution class,
    * without the parentheses. Arrays can be given as arguments by surrounding
    * them with braces.
    * 
    * @param distClass
    *           the class of the represented distribution.
    * @param params
    *           the parameters passed to the constructor.
    * @exception NullPointerException
    *               if \texttt{distClass} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given class is not a subclass of {@link Distribution}
    *               or {@link DiscreteDistributionInt}.
    */
   public RandomVariateGenParam (Class<? extends Distribution> distClass,
         String params) {
      if (!Distribution.class.isAssignableFrom (distClass))
         throw new IllegalArgumentException ("The given class "
               + distClass.getName () + " does not implement "
               + Distribution.class.getName ());
      this.distClass = distClass;
      this.params = params;
      initDefaultGenClass ();
   }

   private void initDefaultGenClass () {
      if (distClass == null)
         genClass = null;
      if (DiscreteDistributionInt.class.isAssignableFrom (distClass))
         genClass = RandomVariateGenInt.class;
      else if (Distribution.class.isAssignableFrom (distClass))
         genClass = RandomVariateGen.class;
      else
         throw new AssertionError ();
   }

   /**
    * Constructs a new random variate generator parameter object for the
    * distribution \texttt{dist}.
    * 
    * @param dist
    *           the probability distribution.
    */
   public RandomVariateGenParam (Distribution dist) {
      distClass = dist.getClass ();
      this.dist = dist;
      initDefaultGenClass ();
   }

   /**
    * Determines if the associated distribution class extends
    * {@link DiscreteDistributionInt}. If this is the case, returns
    * \texttt{true}. Otherwise, returns \texttt{false}. If the distribution
    * class is not set, i.e., {@link #getDistributionClass} returns
    * \texttt{null}, an {@link IllegalStateException} is thrown.
    * 
    * @return \texttt{true} if and only if a discrete distribution of integer is
    *         associated with this parameter object.
    * @exception IllegalStateException
    *               if the distribution class was not set up.
    */
   public boolean isDiscreteDistributionInt () {
      if (distClass == null)
         throw new IllegalStateException ("No distribution class was set");
      return DiscreteDistributionInt.class.isAssignableFrom (distClass);
   }

   /**
    * Returns the class of distribution object contained in this parameter. If
    * the distribution was not set, this returns \texttt{null}.
    * 
    * @return the class of distribution object.
    */
   public Class<? extends Distribution> getDistributionClass () {
      return distClass;
   }

   /**
    * Sets the class of distribution object to \texttt{distClass}. This method
    * resets the random variate generator class to the default value as
    * specified in {@link #getGeneratorClass}, and the distribution parameters
    * to \texttt{null}.
    * 
    * @param distClass
    *           the new distribution class.
    * @exception NullPointerException
    *               if \texttt{distClass} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given class does not implement {@link Distribution}
    *               or extend {@link DiscreteDistributionInt}.
    */
   public void setDistributionClass (Class<? extends Distribution> distClass) {
      if (distClass != null && distClass == this.distClass)
         return;
      if (!Distribution.class.isAssignableFrom (distClass))
         throw new IllegalArgumentException ("The given class "
               + distClass.getName () + " is not compatible with "
               + Distribution.class.getName ());
      this.distClass = distClass;
      params = null;
      dist = null;
      initDefaultGenClass ();
   }

   /**
    * Returns the shift $\ell$ applied to all generated values.
    * 
    * @return the shift being applied.
    */
   public double getShift () {
      return shift;
   }

   /**
    * Sets the shift $\ell$ being applied to all generated values to
    * \texttt{shift}.
    * 
    * @param shift
    *           the new value of the shift.
    */
   public void setShift (double shift) {
      this.shift = shift;
   }

   /**
    * Returns the parameters associated with the distribution. This corresponds
    * to a comma-separated list of parameters as given to the constructor of the
    * distribution class, without the parentheses.
    * 
    * @return the parameters of the distribution.
    */
   public String getDistributionParameters () {
      return params == null ? "" : params;
   }

   /**
    * Sets the distribution parameters to \texttt{params}.
    * 
    * @param params
    *           the distribution parameters.
    */
   public void setDistributionParameters (String params) {
      if (params != this.params) {
         this.params = params;
         dist = null;
      }
   }

   /**
    * Returns the data used to estimate the parameters of the selected
    * distribution. Note that this data is ignored if parameters are specified
    * directly ({@link #getDistributionParameters} returns a non-empty string).
    * 
    * @return the data for estimating the parameters.
    */
   public double[] getData () {
      return data;
   }

   /**
    * Sets the data used for estimating the parameters of the selected
    * distribution to \texttt{data}.
    * 
    * @param data
    *           the data used for parameter estimation.
    */
   public void setData (double[] data) {
      this.data = data;
   }
   
   public ArrayParam getDataParam() {
      return dataParam;
   }
   
   public void setDataParam (ArrayParam dataParam) {
      this.dataParam = dataParam;
   }

   /**
    * Returns the class of the random variate generator associated with this
    * object. The default class is {@link RandomVariateGen} for a
    * {@link Distribution} and {@link RandomVariateGenInt} for a
    * {@link DiscreteDistributionInt}. This has a non-\texttt{null} value only
    * after the distribution class is set up, using a constructor, an XML
    * element, or {@link #setDistributionClass}.
    * 
    * @return the class of random variate generator.
    */
   public Class<? extends RandomVariateGen> getGeneratorClass () {
      return genClass;
   }

   /**
    * Sets the class of random variate generator to \texttt{genClass}. This
    * method cannot be called until a distribution class is set up.
    * 
    * @param genClass
    *           the new random variate generator class.
    * @exception NullPointerException
    *               if \texttt{genClass} is \texttt{null}.
    * @exception IllegalArgumentException
    *               if the given class does not extend {@link RandomVariateGen}.
    * @exception IllegalStateException
    *               if a distribution class was not set up.
    */
   public void setGeneratorClass (Class<? extends RandomVariateGen> genClass) {
      if (genClass != null && genClass == this.genClass)
         return;
      if (distClass == null)
         throw new IllegalStateException (
               "The distribution class is not set up");
      if (DiscreteDistributionInt.class.isAssignableFrom (distClass)) {
         if (!RandomVariateGenInt.class.isAssignableFrom (genClass))
            throw new IllegalArgumentException ("The given class "
                  + genClass.getName () + " is not assignable to "
                  + RandomVariateGenInt.class.getName ());
      }
      else if (Distribution.class.isAssignableFrom (distClass)) {
         if (!RandomVariateGen.class.isAssignableFrom (genClass))
            throw new IllegalArgumentException ("The given class "
                  + genClass.getName () + " is not assignable to "
                  + RandomVariateGen.class.getName ());
      }
      else
         throw new AssertionError ();
      this.genClass = genClass;
   }

   /**
    * Returns the new probability distribution extracted from the parameters.
    * This method can be called only if {@link #isDiscreteDistributionInt}
    * returns \texttt{false}.
    * 
    * @return the extracted probability distribution.
    * @exception IllegalStateException
    *               if the distribution class is not set or corresponds to a
    *               {@link DiscreteDistributionInt} subclass.
    * @exception DistributionCreationException
    *               if the distribution cannot be created successfully.
    */
   public Distribution createDistribution () {
      if (dist == null) {
         if (distClass == null)
            throw new IllegalStateException ("Unspecified distribution class");
         if (!Distribution.class.isAssignableFrom (distClass)
               || DiscreteDistributionInt.class.isAssignableFrom (distClass))
            throw new IllegalStateException ("No Distribution available");
         if (params == null)
            params = "";
         tryToConstructDist (null);
      }
      return dist;
   }

   /**
    * Returns the new discrete probability distribution extracted from the
    * parameters. This method can be called only if
    * {@link #isDiscreteDistributionInt} returns \texttt{true}.
    * 
    * @return the extracted probability distribution.
    * @exception IllegalStateException
    *               if the distribution class is not set or corresponds to a
    *               {@link Distribution} implementation.
    * @exception DistributionCreationException
    *               if the distribution cannot be created successfully.
    */
   public DiscreteDistributionInt createDistributionInt () {
      if (dist == null) {
         if (distClass == null)
            throw new IllegalStateException ("Unspecified distribution class");
         if (!DiscreteDistributionInt.class.isAssignableFrom (distClass))
            throw new IllegalStateException (
                  "No DiscreteDistributionInt available");
         if (params == null)
            params = "";
         tryToConstructDist (null);
      }
      return (DiscreteDistributionInt)dist;
   }

   /**
    * Sets the probability distribution to \texttt{dist}. This resets the
    * generator class to {@link RandomVariateGen}.
    * 
    * @param dist
    *           the new distribution.
    */
   public void setDistribution (Distribution dist) {
      if (dist != null && dist == this.dist)
         return;
      distClass = dist.getClass ();
      params = null;
      this.dist = dist;
      initDefaultGenClass ();
   }
   
   public void setDistribution (Distribution dist, String distParams) {
      if (dist != null && dist == this.dist)
         return;
      distClass = dist.getClass ();
      params = distParams;
      this.dist = dist;
      initDefaultGenClass ();
   }
   
   /**
    * Returns the time unit in which the values produced by the probability
    * distribution are expressed. If this is set to \texttt{null} (the default),
    * no time conversion is performed.
    * 
    * @return the distribution's time unit.
    */
   public TimeUnit getUnit () {
      return unit;
   }

   /**
    * Sets the distribution's time unit to \texttt{unit}.
    * 
    * @param unit
    *           the new distribution's time unit.
    * @see #getUnit
    */
   public void setUnit (TimeUnit unit) {
      this.unit = unit;
   }

   /**
    * Sets the current distribution to a constant value \texttt{c}. This is a
    * special case of a discrete distribution with a single observation
    * \texttt{c} having probability 1.
    * 
    * @param c
    *           the constant returned by the produced generator.
    */
   public void setConstant (double c) {
      setDistributionClass (ConstantDist.class);
      // setDistributionParameters ("1, { " + c + " }, { 1.0 }");
      setDistributionParameters ("" + c);
      // dist = new DiscreteDistribution (1, new double[] { c }, new double[] {
      // 1.0 });
      dist = new ConstantDist (c);
   }
   
   /**
    * Returns the mean value for the current distribution.
    * This method calls
    * {@link #createDistribution()}{@link Distribution#getMean() .getMean()}.
    * @return the mean.
    */
   public double getMean() {
      return createDistribution().getMean();
   }
   
   /**
    * Returns the mean for the current distribution,
    * epxressed with the time unit \texttt{targetUnit}.
    * This method returns the same value as
    * {@link #getMean()} if {@link #getUnit()}
    * returns \texttt{null} or \texttt{targetUnit}
    * is \texttt{null}.
    * @param targetUnit the target time unit.
    * @return the mean, possibly converted to the target time unit.
    */
   public double getMean (TimeUnit targetUnit) {
      if (targetUnit == null || unit == null)
         return getMean();
      else
         return TimeUnit.convert (getMean(), unit, targetUnit);
   }
   
   /**
    * Returns the variance for the current distribution.
    * This method calls
    * {@link #createDistribution()}{@link Distribution#getVariance() .getVariance()}.
    * @return the mean.
    */
   public double getVariance() {
      return createDistribution().getVariance();
   }

   /**
    * Returns the $\lambda$ parameter for the associated exponential
    * distribution. If the distribution class is not set up or not exponential,
    * this throws an {@link IllegalStateException}. Otherwise, the method tries
    * to create the exponential distribution object and returns the
    * corresponding $\lambda$ parameter.
    * 
    * @return the $\lambda$ parameter.
    * @exception IllegalStateException
    *               if the distribution is incompatible.
    */
   public double getExpLambda () {
      if (distClass == null
            || !ExponentialDist.class.isAssignableFrom (distClass))
         throw new IllegalStateException ("Incompatible distribution object");
      final ExponentialDist edist = (ExponentialDist) createDistribution ();
      return edist.getLambda ();
   }

   /**
    * Sets the distribution class to exponential and sets the $\lambda$
    * parameter to \texttt{lambda}.
    * 
    * @param lambda
    *           the $\lambda$ parameter for the exponential distribution.
    * @exception IllegalArgumentException
    *               if \texttt{lambda} is negative or 0.
    */
   public void setExpLambda (double lambda) {
      if (lambda < 0)
         throw new IllegalArgumentException ("lambda <= 0");
      setDistributionClass (ExponentialDist.class);
      setDistributionParameters (String.valueOf (lambda));
      dist = new ExponentialDist (lambda);
   }

   /**
    * Returns the $\alpha$ parameter for the associated gamma distribution. If
    * the distribution class is not set up or not gamma, this throws an
    * {@link IllegalStateException}. Otherwise, the method tries to create the
    * gamma distribution object and returns the corresponding $\gamma$
    * parameter.
    * 
    * @return the $\alpha$ parameter.
    * @exception IllegalStateException
    *               if the distribution is incompatible.
    */
   public double getGammaAlpha () {
      if (distClass == null || !GammaDist.class.isAssignableFrom (distClass))
         throw new IllegalStateException ("Incompatible distribution object");
      final GammaDist gdist = (GammaDist) createDistribution ();
      return gdist.getAlpha ();
   }

   /**
    * Returns the $\lambda$ parameter for the associated gamma distribution. If
    * the distribution class is not set up or not gamma, this throws an
    * {@link IllegalStateException}. Otherwise, the method tries to create the
    * gamma distribution object and returns the corresponding $\lambda$
    * parameter.
    * 
    * @return the $\lambda$ parameter.
    * @exception IllegalStateException
    *               if the distribution is incompatible.
    */
   public double getGammaLambda () {
      if (distClass == null || !GammaDist.class.isAssignableFrom (distClass))
         throw new IllegalStateException ("Incompatible distribution object");
      final GammaDist gdist = (GammaDist) createDistribution ();
      return gdist.getLambda ();
   }

   /**
    * Sets the distribution class to gamma and sets the $\alpha$ and $\lambda$
    * parameters to \texttt{alpha} and \texttt{lambda}, respectively.
    * 
    * @param alpha
    *           the $\alpha$ parameter for the gamma distribution.
    * @param lambda
    *           the $\lambda$ parameter for the gamma distribution.
    * @exception IllegalArgumentException
    *               if \texttt{alpha} or \texttt{lambda} are negative or 0.
    */
   public void setGammaParams (double alpha, double lambda) {
      if (alpha <= 0)
         throw new IllegalArgumentException ("alpha <= 0");
      if (lambda <= 0)
         throw new IllegalArgumentException ("lambda <= 0");
      setDistributionClass (GammaDist.class);
      setDistributionParameters (String.valueOf (alpha) + ", "
            + String.valueOf (lambda));
      dist = new GammaDist (alpha, lambda);
   }

   /**
    * Constructs a new random variate generator from the distribution
    * information associated with this object and the stream \texttt{stream}.
    * This method constructs a random variate generator from the class given by
    * {@link #getGeneratorClass} by selecting an appropriate constructor. The
    * method only uses constructors taking a random stream and a probability
    * distribution. Other constructors from a random variate generator are
    * ignored. If the parameter object contains information about a discrete
    * distribution of integers, a {@link RandomVariateGenInt} is constructed.
    * 
    * @param stream
    *           the random stream used to generate uniforms.
    * @exception DistributionCreationException
    *               if the probability distribution could not be created
    *               successfully.
    * @exception GeneratorCreationException
    *               if the generator cannot be created.
    * @exception IllegalStateException
    *               if some distribution or generator parameters are missing or
    *               invalid.
    */
   public RandomVariateGen createGenerator (RandomStream stream) {
      if (distClass == null)
         throw new IllegalStateException ("No distribution class was set up");
      final RandomVariateGen rvg = tryToConstructGen (stream);
      if (shift == 0)
         return rvg;
      return new RandomVariateGenWithShift (rvg, shift);
   }

   /**
    * Constructs a new integer random variate generator from the distribution
    * information associated with this object and the stream \texttt{stream}.
    * This is similar to {@link #createGenerator}, except it returns an integer
    * random variate generator. An {@link IllegalStateException} is thrown if
    * the parameter object contains information about a {@link Distribution}
    * implementation.
    * 
    * @param stream
    *           the random stream used to generate uniforms.
    * @exception DistributionCreationException
    *               if the probability distribution could not be created
    *               successfully.
    * @exception GeneratorCreationException
    *               if the generator cannot be created.
    * @exception IllegalStateException
    *               if some distribution or generator parameters are missing or
    *               invalid, or if the associated parameters correspond to an
    *               incompatible distribution.
    */
   public RandomVariateGenInt createGeneratorInt (RandomStream stream) {
      if (distClass == null
            || !DiscreteDistributionInt.class.isAssignableFrom (distClass))
         throw new IllegalStateException (
               "A discrete distribution of integers is not set up");
      final RandomVariateGen o = tryToConstructGen (stream);
      if (o instanceof RandomVariateGenInt) {
         final RandomVariateGenInt rvg = (RandomVariateGenInt) o;
         final int shiftInt = (int) Math.round (shift);
         if (shiftInt == 0)
            return rvg;
         return new RandomVariateGenIntWithShift (rvg, shiftInt);
      }
      else
         throw new AssertionError ();
   }

   /**
    * For internal use only.
    */
   public boolean isAttributeSupported (String a) {
      return a.equals ("id") || a.equals ("xref")
            || a.equals ("distributionClass") || a.equals ("generatorClass")
            || a.equals ("dataFile")
            || a.equals ("dataURL") || a.equals ("dataQuery")
            || a.equals ("shift")
            || a.equals ("unit");
   }

   /**
    * For internal use only.
    */
   public void nestedText (ParamReader reader, String par) {
      if (distClass == null)
         throw new ParamReadException (
               "The class of probability distribution must be specified using the "
                     + "distributionClass attribute");
      if (dist != null)
         throw new ParamReadException ("Distribution already specified");
      setDistributionParameters (par);
      tryToConstructDist (reader.getClassFinder ());
   }

   /**
    * Uses data obtained by {@link #getData} to estimate
    * the parameters of the distribution with class
    * {@link #getDistributionClass}, and stores the estimation in the
    * distribution's parameters returned by {@link #getDistributionParameters}.
    * This method does nothing if {@link #getDistributionClass} returns
    * \texttt{null} or a class corresponding to an empirical distribution.
    * Otherwise, it tries to call a static method
    * \texttt{getMaximumLikelihoodEstimate (double[], int)} or
    * \texttt{getMaximumLikelihoodEstimate (int[], int)} (for discrete
    * distributions over the integers) with the values in the array returned by
    * {@link #getData}.
    * The resulting array is converted into a string assuming that an
    * appropriate constructor exists in the distribution class. If
    * \texttt{clearData} is \texttt{true}, the references to the data and the
    * data file for this object are set to \texttt{null} after parameter
    * estimation.
    * 
    * @param clearData
    *           determines if the data bound to this object must be discarded.
    */
   public void estimateParameters (boolean clearData) {
      if (distClass == null)
         return;
      initData ();

      setDistributionParameters (getDistributionParametersMLE (distClass, data, shift));
      if (clearData)
         data = null;
   }
   
   public static String getDistributionParametersMLE (Class<? extends Distribution> distClass, double[] data, double shift) {
      double[] shiftedData = data;
      if (shift != 0) {
         shiftedData = new double[data.length];
         for (int i = 0; i < data.length; i++)
            shiftedData[i] = data[i] + shift;
      }
      double[] params;
      try {
         if (DiscreteDistributionInt.class.isAssignableFrom (distClass)) {
            final int[] intData = new int[shiftedData.length];
            for (int x = 0; x < intData.length; x++)
               intData[x] = (int) Math.round (shiftedData[x]);
            final Method mt = distClass.getMethod (
                  "getMaximumLikelihoodEstimate", int[].class, int.class);
            params = (double[]) mt.invoke (null, intData, intData.length);
         }
         else {
            final Method mt = distClass.getMethod (
                  "getMaximumLikelihoodEstimate", double[].class, int.class);
            params = (double[]) mt.invoke (null, shiftedData,
                  shiftedData.length);
         }
      }
      catch (final NoSuchMethodException nme) {
         throw new DistributionCreationException (distClass,
               nme.getMessage ());
      }
      catch (final IllegalAccessException iae) {
         throw new DistributionCreationException (distClass,
               iae.getMessage ());
      }
      catch (final InvocationTargetException ite) {
         throw new DistributionCreationException (distClass,
               "An exception occurred during call to getMaximumLikelihoodEstimate: "
                     + ite.getCause ().toString ());
      }

      final StringBuilder sbpar = new StringBuilder ();
      for (int i = 0; i < params.length; i++)
         sbpar.append (i > 0 ? ", " : "").append (params[i]);
      return sbpar.toString ();
   }

   private void initData () {
      if (data != null || dataParam == null)
         return;
      data = dataParam.getDoubleValues ();
   }

   private void tryToConstructDist (ClassFinder finder) {
      if (data == null && (params == null || params.length () == 0))
         initData ();
      if (data != null && (params == null || params.length () == 0))
         dist = createDistributionMLE (distClass, data, shift);
      else {
         final String[] tokens = StringConvert
               .getArrayElements (getDistributionParameters ());
         try {
            dist = StringConvert.tryToCallConstructor (null,
                  finder, getDistributionClass (), tokens);
         }
         catch (final NoSuchMethodException nme) {
            throw new DistributionCreationException (getDistributionClass (),
                  getDistributionParameters (),
                  "Cannot find an appropriate constructor in distribution class");
         }
         catch (final UnsupportedConversionException uce) {
            throw new DistributionCreationException (getDistributionClass (),
                  getDistributionParameters (), uce.toString ());
         }
         catch (final NameConflictException nce) {
            throw new DistributionCreationException (getDistributionClass (),
                  getDistributionParameters (), nce.getMessage ());
         }
         catch (final IllegalArgumentException iae) {
            final DistributionCreationException dce = new DistributionCreationException (
                  getDistributionClass (), getDistributionParameters (), iae
                        .toString ());
            throw dce;
         }
      }
   }

   @SuppressWarnings ("unchecked")
   public static Distribution createDistributionMLE (Class<? extends Distribution> distClass, double[] data, double shift) {
      double[] shiftedData = data;
      if (shift != 0) {
         shiftedData = new double[data.length];
         for (int i = 0; i < data.length; i++)
            shiftedData[i] = data[i] + shift;
      }
      if (distClass == EmpiricalDist.class) {
         Arrays.sort (shiftedData);
         return new EmpiricalDist (shiftedData);
      }
      else if (distClass == PiecewiseLinearEmpiricalDist.class) {
         Arrays.sort (shiftedData);
         return new PiecewiseLinearEmpiricalDist (shiftedData);
      }
      else if (DiscreteDistributionInt.class.isAssignableFrom (distClass)) {
         final int[] intData = new int[shiftedData.length];
         for (int x = 0; x < intData.length; x++)
            intData[x] = (int) Math.round (shiftedData[x]);
         return DistributionFactory.getDistributionMLE (
               (Class<? extends DiscreteDistributionInt>) distClass,
               intData, intData.length);
      }
      else
         return DistributionFactory.getDistributionMLE (
               (Class<? extends ContinuousDistribution>) distClass,
               shiftedData, shiftedData.length);
   }

   private RandomVariateGen tryToConstructGen (RandomStream stream) {
      if (dist == null)
         tryToConstructDist (null);
      for (final Constructor<?> element : genClass.getConstructors ()) {
         final Class<?>[] pt = element.getParameterTypes ();
         if (pt.length != 2 || !pt[0].isAssignableFrom (stream.getClass ())
               || !pt[1].isAssignableFrom (dist.getClass ()))
            continue;
         try {
            return (RandomVariateGen) element.newInstance (stream, dist);
         }
         catch (final IllegalAccessException iae) {}
         catch (final InstantiationException ie) {
            throw new GeneratorCreationException (getDistributionClass (),
                  getDistributionParameters (), getGeneratorClass (), ie
                        .getMessage ());
         }
         catch (final InvocationTargetException ite) {
            final GeneratorCreationException gce = new GeneratorCreationException (
                  getDistributionClass (), getDistributionParameters (),
                  getGeneratorClass (),
                  "Exception occured during call to constructor: "
                        + ite.getCause ().toString ());
            throw gce;
         }
      }
      throw new GeneratorCreationException (getDistributionClass (),
            getDistributionParameters (), getGeneratorClass (),
            "Cannot find a suitable constructor");
   }

   /**
    * For internal use only.
    */
   public ArrayParam createData () {
      return dataParam = new ArrayParam (double.class);
   }

   /**
    * For internal use only.
    */
   public void addData (ArrayParam p) {
      data = p.getDoubleValues ();
   }

   public Element toElement (ClassFinder finder, Node parent,
         String elementName, int spc) {
      final Element el = DOMUtils.addNestedTextElement (parent, elementName,
            params == null ? "" : params.trim (), spc);
      if (distClass != null)
         el
               .setAttribute ("distributionClass", finder
                     .getSimpleName (distClass));
      if (shift != 0)
         el.setAttribute ("shift", String.valueOf (shift));
      if (!isDiscreteDistributionInt() && genClass != RandomVariateGen.class
            || isDiscreteDistributionInt() && genClass != RandomVariateGenInt.class)
         el.setAttribute ("generatorClass", finder.getSimpleName (genClass));
      if (unit != null)
         el.setAttribute ("unit", Introspection.getFieldName (unit));
      if (dataParam != null)
         dataParam.toElement (finder, el, "data", spc);
      return el;
   }

   @Override
   public String toString () {
      return distClass.getName () + (params == null ? "" : " (" + params + ")")
            + ", with generator " + genClass.getName ();
   }

   @Override
   public boolean equals (Object other) {
      if (!(other instanceof RandomVariateGenParam))
         return false;
      final RandomVariateGenParam rp = (RandomVariateGenParam) other;
      if (!distClass.equals (rp.getDistributionClass ()))
         return false;
      if (!genClass.equals (rp.getGeneratorClass ()))
         return false;
      if (params == null)
         return false;
      return params.equals (rp.getDistributionParameters ());
   }

   @Override
   public int hashCode () {
      int sum = 17;
      sum = 37 * sum + (distClass == null ? 0 : distClass.hashCode ());
      sum = 37 * sum + (genClass == null ? 0 : genClass.hashCode ());
      sum = 37 * sum + (params == null ? 0 : params.hashCode ());
      return sum;
   }

   @Override
   public RandomVariateGenParam clone () {
      final RandomVariateGenParam cpy;
      try {
         cpy = (RandomVariateGenParam) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new InternalError (
               "Clone not supported for a class implementing Cloneable");
      }
      return cpy;
   }
}
