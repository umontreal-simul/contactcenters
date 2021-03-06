//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.app.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.DoubleArray;
import umontreal.iro.lecuyer.xmlbind.params.NonNegativeDurationArray;


/**
 * 
 *                   Represents the parameters used for estimating the
 *                   service level.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceLevelParams", propOrder = {
    "awt",
    "target"
})
public class ServiceLevelParams {

    @XmlElement(required = true)
    protected NonNegativeDurationArray awt;
    protected DoubleArray target;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the awt property.
     * 
     * @return
     *     possible object is
     *     {@link NonNegativeDurationArray }
     *     
     */
    public NonNegativeDurationArray getAwt() {
        return awt;
    }

    /**
     * Sets the value of the awt property.
     * 
     * @param value
     *     allowed object is
     *     {@link NonNegativeDurationArray }
     *     
     */
    public void setAwt(NonNegativeDurationArray value) {
        this.awt = value;
    }

    public boolean isSetAwt() {
        return (this.awt!= null);
    }

    /**
     * Gets the value of the target property.
     * 
     * @return
     *     possible object is
     *     {@link DoubleArray }
     *     
     */
    public DoubleArray getTarget() {
        return target;
    }

    /**
     * Sets the value of the target property.
     * 
     * @param value
     *     allowed object is
     *     {@link DoubleArray }
     *     
     */
    public void setTarget(DoubleArray value) {
        this.target = value;
    }

    public boolean isSetTarget() {
        return (this.target!= null);
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    public boolean isSetName() {
        return (this.name!= null);
    }

}
