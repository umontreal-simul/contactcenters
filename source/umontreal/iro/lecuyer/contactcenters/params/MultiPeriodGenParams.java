//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.params;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.contactcenters.msk.params.ServiceTimeParams;
import umontreal.iro.lecuyer.xmlbind.params.RandomVariateGenParams;
import umontreal.iro.lecuyer.xmlbind.params.TimeUnitParam;


/**
 * 
 *                   Represents parameters for a random variate generator
 *                   over multiple periods.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultiPeriodGenParams", propOrder = {
    "defaultGen",
    "preGen",
    "wrapGen",
    "periodGen"
})
@XmlSeeAlso({
    ServiceTimeParams.class
})
public class MultiPeriodGenParams {

    protected RandomVariateGenParams defaultGen;
    protected RandomVariateGenParams preGen;
    protected RandomVariateGenParams wrapGen;
    protected List<MultiPeriodGenParams.PeriodGen> periodGen;
    @XmlAttribute(name = "distributionClass")
    protected String distributionClass;
    @XmlAttribute(name = "generatorClass")
    protected String generatorClass;
    @XmlAttribute(name = "shift")
    protected Double shift;
    @XmlAttribute(name = "estimateParameters")
    protected Boolean estimateParameters;
    @XmlAttribute(name = "unit")
    protected TimeUnitParam unit;
    @XmlAttribute(name = "mult")
    protected Double mult;

    /**
     * Gets the value of the defaultGen property.
     * 
     * @return
     *     possible object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public RandomVariateGenParams getDefaultGen() {
        return defaultGen;
    }

    /**
     * Sets the value of the defaultGen property.
     * 
     * @param value
     *     allowed object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public void setDefaultGen(RandomVariateGenParams value) {
        this.defaultGen = value;
    }

    public boolean isSetDefaultGen() {
        return (this.defaultGen!= null);
    }

    /**
     * Gets the value of the preGen property.
     * 
     * @return
     *     possible object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public RandomVariateGenParams getPreGen() {
        return preGen;
    }

    /**
     * Sets the value of the preGen property.
     * 
     * @param value
     *     allowed object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public void setPreGen(RandomVariateGenParams value) {
        this.preGen = value;
    }

    public boolean isSetPreGen() {
        return (this.preGen!= null);
    }

    /**
     * Gets the value of the wrapGen property.
     * 
     * @return
     *     possible object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public RandomVariateGenParams getWrapGen() {
        return wrapGen;
    }

    /**
     * Sets the value of the wrapGen property.
     * 
     * @param value
     *     allowed object is
     *     {@link RandomVariateGenParams }
     *     
     */
    public void setWrapGen(RandomVariateGenParams value) {
        this.wrapGen = value;
    }

    public boolean isSetWrapGen() {
        return (this.wrapGen!= null);
    }

    /**
     * Gets the value of the periodGen property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the periodGen property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeriodGen().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MultiPeriodGenParams.PeriodGen }
     * 
     * 
     */
    public List<MultiPeriodGenParams.PeriodGen> getPeriodGen() {
        if (periodGen == null) {
            periodGen = new ArrayList<MultiPeriodGenParams.PeriodGen>();
        }
        return this.periodGen;
    }

    public boolean isSetPeriodGen() {
        return ((this.periodGen!= null)&&(!this.periodGen.isEmpty()));
    }

    public void unsetPeriodGen() {
        this.periodGen = null;
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

    /**
     * Gets the value of the unit property.
     * 
     * @return
     *     possible object is
     *     {@link TimeUnitParam }
     *     
     */
    public TimeUnitParam getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeUnitParam }
     *     
     */
    public void setUnit(TimeUnitParam value) {
        this.unit = value;
    }

    public boolean isSetUnit() {
        return (this.unit!= null);
    }

    /**
     * Gets the value of the mult property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getMult() {
        if (mult == null) {
            return  1.0D;
        } else {
            return mult;
        }
    }

    /**
     * Sets the value of the mult property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMult(double value) {
        this.mult = value;
    }

    public boolean isSetMult() {
        return (this.mult!= null);
    }

    public void unsetMult() {
        this.mult = null;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.iro.umontreal.ca/lecuyer/ssj>RandomVariateGenParams">
     *       &lt;attribute name="repeat" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeInt" default="1" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class PeriodGen
        extends RandomVariateGenParams
    {

        @XmlAttribute(name = "repeat")
        protected Integer repeat;

        /**
         * Gets the value of the repeat property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public int getRepeat() {
            if (repeat == null) {
                return  1;
            } else {
                return repeat;
            }
        }

        /**
         * Sets the value of the repeat property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setRepeat(int value) {
            this.repeat = value;
        }

        public boolean isSetRepeat() {
            return (this.repeat!= null);
        }

        public void unsetRepeat() {
            this.repeat = null;
        }

    }

}
