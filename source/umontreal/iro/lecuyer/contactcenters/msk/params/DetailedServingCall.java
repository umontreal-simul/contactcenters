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
import javax.xml.datatype.Duration;


/**
 * 
 *             Defines the waited time and/or served time of a call. These represent the time
 *             spent waiting or served before the execution of the simulation.
 *             This call is assumed to be in service when the simulator starts.
 *          
 * 
 * <p>Java class for DetailedServingCall complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DetailedServingCall">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.iro.umontreal.ca/lecuyer/contactcenters/msk}DetailedWaitingCall">
 *       &lt;attribute name="servedTime" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeDuration" default="PT0S" />
 *       &lt;attribute name="servingAgentID" type="{http://www.w3.org/2001/XMLSchema}int" default="-1" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DetailedServingCall")
public class DetailedServingCall
    extends DetailedWaitingCall
{

    @XmlAttribute(name = "servedTime")
    protected Duration servedTime;
    @XmlAttribute(name = "servingAgentID")
    protected Integer servingAgentID;

    /**
     * Gets the value of the servedTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getServedTime() {
        return servedTime;
    }

    /**
     * Sets the value of the servedTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setServedTime(Duration value) {
        this.servedTime = value;
    }

    public boolean isSetServedTime() {
        return (this.servedTime!= null);
    }

    /**
     * Gets the value of the servingAgentID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getServingAgentID() {
        if (servingAgentID == null) {
            return -1;
        } else {
            return servingAgentID;
        }
    }

    /**
     * Sets the value of the servingAgentID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setServingAgentID(int value) {
        this.servingAgentID = value;
    }

    public boolean isSetServingAgentID() {
        return (this.servingAgentID!= null);
    }

    public void unsetServingAgentID() {
        this.servingAgentID = null;
    }

}
