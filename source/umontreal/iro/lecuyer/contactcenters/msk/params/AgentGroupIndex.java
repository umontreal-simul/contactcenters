//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.msk.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *             Gives the index of an agent group with a probability that
 *             the agent group is selected. The index is given by the
 *             
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;index&lt;/tt&gt;
 * </pre>
 * 
 *             attribute while the probability is set up using the
 *             
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;probability&lt;/tt&gt;
 * </pre>
 * 
 *             attribute.
 *          
 * 
 * <p>Java class for AgentGroupIndex complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentGroupIndex">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="index" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeInt" />
 *       &lt;attribute name="probability" type="{http://www.iro.umontreal.ca/lecuyer/ssj}double01i" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentGroupIndex")
public class AgentGroupIndex {

    @XmlAttribute(name = "index")
    protected Integer index;
    @XmlAttribute(name = "probability")
    protected Double probability;

    /**
     * Gets the value of the index property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the value of the index property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setIndex(int value) {
        this.index = value;
    }

    public boolean isSetIndex() {
        return (this.index!= null);
    }

    public void unsetIndex() {
        this.index = null;
    }

    /**
     * Gets the value of the probability property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Sets the value of the probability property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setProbability(double value) {
        this.probability = value;
    }

    public boolean isSetProbability() {
        return (this.probability!= null);
    }

    public void unsetProbability() {
        this.probability = null;
    }

}