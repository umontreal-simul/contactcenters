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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


/**
 * 
 *             Defines a waiting call and its waited time. The waited time is the time duration
 *             spent waiting before the start of the simulation.
 *          
 * 
 * <p>Java class for DetailedWaitingCall complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DetailedWaitingCall">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}int" default="-1" />
 *       &lt;attribute name="waitedTime" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeDuration" default="PT0S" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DetailedWaitingCall")
@XmlSeeAlso({
    DetailedServingCall.class
})
public class DetailedWaitingCall {

    @XmlAttribute(name = "type")
    protected Integer type;
    @XmlAttribute(name = "waitedTime")
    protected Duration waitedTime;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getType() {
        if (type == null) {
            return -1;
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setType(int value) {
        this.type = value;
    }

    public boolean isSetType() {
        return (this.type!= null);
    }

    public void unsetType() {
        this.type = null;
    }

    /**
     * Gets the value of the waitedTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getWaitedTime() {
        return waitedTime;
    }

    /**
     * Sets the value of the waitedTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setWaitedTime(Duration value) {
        this.waitedTime = value;
    }

    public boolean isSetWaitedTime() {
        return (this.waitedTime!= null);
    }

}
