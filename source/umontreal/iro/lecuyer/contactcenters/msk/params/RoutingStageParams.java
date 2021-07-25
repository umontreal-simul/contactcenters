//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.msk.params;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


/**
 * 
 * 	Describes a specific stage of routing for a particular call type by
 * 	using a waiting time, and a sequence of cases.
 * 	The waiting time is given by the attribute
 * 	
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;waitingTime&lt;/tt&gt;
 * </pre>
 *  while the sequence of cases
 * 	is set up by 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;case&lt;/tt&gt;
 * </pre>
 *  elements optionnally followed
 * 	by a 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;default&lt;/tt&gt;
 * </pre>
 *  element.
 * 	The routing policy checks each condition given by
 * 	the cases in the specified
 *    order, and takes the vectors of ranks corresponding
 * 	to the first case that applies.
 * 	If no case applies, the vectors given in the default
 * 	case are used.
 * 	If no case applies and no 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;tt xmlns="http://www.w3.org/1999/xhtml" xmlns:cc="http://www.iro.umontreal.ca/lecuyer/contactcenters" xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app" xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk" xmlns:jxb="http://java.sun.com/xml/ns/jaxb" xmlns:ssj="http://www.iro.umontreal.ca/lecuyer/ssj" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;default&lt;/tt&gt;
 * </pre>
 *  element is given,
 * 	the stage has no effect.
 * 	
 * 
 * <p>Java class for RoutingStageParams complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RoutingStageParams">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="case" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/msk}RoutingCaseParams" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="default" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/msk}DefaultCaseParams" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="waitingTime" use="required" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeDuration" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RoutingStageParams", propOrder = {
    "_case",
    "_default"
})
public class RoutingStageParams {

    @XmlElement(name = "case")
    protected List<RoutingCaseParams> _case;
    @XmlElement(name = "default")
    protected DefaultCaseParams _default;
    @XmlAttribute(name = "waitingTime", required = true)
    protected Duration waitingTime;

    /**
     * Gets the value of the case property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the case property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RoutingCaseParams }
     * 
     * 
     */
    public List<RoutingCaseParams> getCase() {
        if (_case == null) {
            _case = new ArrayList<RoutingCaseParams>();
        }
        return this._case;
    }

    public boolean isSetCase() {
        return ((this._case!= null)&&(!this._case.isEmpty()));
    }

    public void unsetCase() {
        this._case = null;
    }

    /**
     * Gets the value of the default property.
     * 
     * @return
     *     possible object is
     *     {@link DefaultCaseParams }
     *     
     */
    public DefaultCaseParams getDefault() {
        return _default;
    }

    /**
     * Sets the value of the default property.
     * 
     * @param value
     *     allowed object is
     *     {@link DefaultCaseParams }
     *     
     */
    public void setDefault(DefaultCaseParams value) {
        this._default = value;
    }

    public boolean isSetDefault() {
        return (this._default!= null);
    }

    /**
     * Gets the value of the waitingTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getWaitingTime() {
        return waitingTime;
    }

    /**
     * Sets the value of the waitingTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setWaitingTime(Duration value) {
        this.waitingTime = value;
    }

    public boolean isSetWaitingTime() {
        return (this.waitingTime!= null);
    }

}