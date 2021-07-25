//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.xmlbind.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * 
 *                   Gives parameters for a probability distribution and a
 *                   random number generator constructed using SSJ.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RandomVariateGenParams", propOrder = {
    "params"
})
@XmlSeeAlso({
    umontreal.iro.lecuyer.contactcenters.params.MultiPeriodGenParams.PeriodGen.class
})
public class RandomVariateGenParams {

    @XmlValue
    protected double[] params;
    @XmlAttribute(name = "distributionClass")
    protected String distributionClass;
    @XmlAttribute(name = "generatorClass")
    protected String generatorClass;
    @XmlAttribute(name = "shift")
    protected Double shift;
    @XmlAttribute(name = "lowerBound")
    protected Double lowerBound;
    @XmlAttribute(name = "upperBound")
    protected Double upperBound;
    @XmlAttribute(name = "estimateParameters")
    protected Boolean estimateParameters;

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getParams() {
        if (this.params == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.params.length] ;
        System.arraycopy(this.params, 0, retVal, 0, this.params.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Double }
     *     
     */
    public double getParams(int idx) {
        if (this.params == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.params[idx];
    }

    public int getParamsLength() {
        if (this.params == null) {
            return  0;
        }
        return this.params.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setParams(double[] values) {
        int len = values.length;
        this.params = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.params[i] = new Double(values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public double setParams(int idx, double value) {
        return this.params[idx] = new Double(value);
    }

    public boolean isSetParams() {
        return ((this.params!= null)&&(this.params.length > 0));
    }

    public void unsetParams() {
        this.params = null;
    }

    /**
     * Gets the value of the distributionClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDistributionClass() {
        return distributionClass;
    }

    /**
     * Sets the value of the distributionClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDistributionClass(String value) {
        this.distributionClass = value;
    }

    public boolean isSetDistributionClass() {
        return (this.distributionClass!= null);
    }

    /**
     * Gets the value of the generatorClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGeneratorClass() {
        return generatorClass;
    }

    /**
     * Sets the value of the generatorClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGeneratorClass(String value) {
        this.generatorClass = value;
    }

    public boolean isSetGeneratorClass() {
        return (this.generatorClass!= null);
    }

    /**
     * Gets the value of the shift property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getShift() {
        return shift;
    }

    /**
     * Sets the value of the shift property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setShift(double value) {
        this.shift = value;
    }

    public boolean isSetShift() {
        return (this.shift!= null);
    }

    public void unsetShift() {
        this.shift = null;
    }

    /**
     * Gets the value of the lowerBound property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the value of the lowerBound property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setLowerBound(double value) {
        this.lowerBound = value;
    }

    public boolean isSetLowerBound() {
        return (this.lowerBound!= null);
    }

    public void unsetLowerBound() {
        this.lowerBound = null;
    }

    /**
     * Gets the value of the upperBound property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the value of the upperBound property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setUpperBound(double value) {
        this.upperBound = value;
    }

    public boolean isSetUpperBound() {
        return (this.upperBound!= null);
    }

    public void unsetUpperBound() {
        this.upperBound = null;
    }

    /**
     * Gets the value of the estimateParameters property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEstimateParameters() {
        if (estimateParameters == null) {
            return false;
        } else {
            return estimateParameters;
        }
    }

    /**
     * Sets the value of the estimateParameters property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEstimateParameters(boolean value) {
        this.estimateParameters = value;
    }

    public boolean isSetEstimateParameters() {
        return (this.estimateParameters!= null);
    }

    public void unsetEstimateParameters() {
        this.estimateParameters = null;
    }

}
