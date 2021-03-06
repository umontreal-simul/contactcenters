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
 *                   Represents a time interval specified by a starting and
 *                   an ending time.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TimeIntervalParams")
@XmlSeeAlso({
    ShiftPartParams.class,
    DialerLimitParams.class
})
public class TimeIntervalParams {

    @XmlAttribute(name = "startingTime", required = true)
    protected Duration startingTime;
    @XmlAttribute(name = "endingTime", required = true)
    protected Duration endingTime;

    /**
     * Gets the value of the startingTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getStartingTime() {
        return startingTime;
    }

    /**
     * Sets the value of the startingTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setStartingTime(Duration value) {
        this.startingTime = value;
    }

    public boolean isSetStartingTime() {
        return (this.startingTime!= null);
    }

    /**
     * Gets the value of the endingTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getEndingTime() {
        return endingTime;
    }

    /**
     * Sets the value of the endingTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setEndingTime(Duration value) {
        this.endingTime = value;
    }

    public boolean isSetEndingTime() {
        return (this.endingTime!= null);
    }

}
