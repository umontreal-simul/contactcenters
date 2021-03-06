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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 *                   Defines the parameters for a call source generating
 *                   inbound or outbound calls, possibly of more than one
 *                   type.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CallSourceParams", propOrder = {
    "sourceToggleTimes",
    "producedCallTypes"
})
@XmlSeeAlso({
    ArrivalProcessParams.class,
    DialerParams.class
})
public abstract class CallSourceParams
    extends Named
{

    @XmlElement(name = "sourceToggleTime")
    protected List<TimeIntervalParams> sourceToggleTimes;
    @XmlElement(name = "call")
    protected List<ProducedCallTypeParams> producedCallTypes;
    @XmlAttribute(name = "sourceEnabled")
    protected Boolean sourceEnabled;
    @XmlAttribute(name = "checkAgentsForCall")
    protected Boolean checkAgentsForCall;

    /**
     * Gets the value of the sourceToggleTimes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sourceToggleTimes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSourceToggleTimes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TimeIntervalParams }
     * 
     * 
     */
    public List<TimeIntervalParams> getSourceToggleTimes() {
        if (sourceToggleTimes == null) {
            sourceToggleTimes = new ArrayList<TimeIntervalParams>();
        }
        return this.sourceToggleTimes;
    }

    public boolean isSetSourceToggleTimes() {
        return ((this.sourceToggleTimes!= null)&&(!this.sourceToggleTimes.isEmpty()));
    }

    public void unsetSourceToggleTimes() {
        this.sourceToggleTimes = null;
    }

    /**
     * Gets the value of the producedCallTypes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the producedCallTypes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProducedCallTypes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ProducedCallTypeParams }
     * 
     * 
     */
    public List<ProducedCallTypeParams> getProducedCallTypes() {
        if (producedCallTypes == null) {
            producedCallTypes = new ArrayList<ProducedCallTypeParams>();
        }
        return this.producedCallTypes;
    }

    public boolean isSetProducedCallTypes() {
        return ((this.producedCallTypes!= null)&&(!this.producedCallTypes.isEmpty()));
    }

    public void unsetProducedCallTypes() {
        this.producedCallTypes = null;
    }

    /**
     * Gets the value of the sourceEnabled property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isSourceEnabled() {
        if (sourceEnabled == null) {
            return true;
        } else {
            return sourceEnabled;
        }
    }

    /**
     * Sets the value of the sourceEnabled property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSourceEnabled(boolean value) {
        this.sourceEnabled = value;
    }

    public boolean isSetSourceEnabled() {
        return (this.sourceEnabled!= null);
    }

    public void unsetSourceEnabled() {
        this.sourceEnabled = null;
    }

    /**
     * Gets the value of the checkAgentsForCall property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isCheckAgentsForCall() {
        if (checkAgentsForCall == null) {
            return false;
        } else {
            return checkAgentsForCall;
        }
    }

    /**
     * Sets the value of the checkAgentsForCall property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCheckAgentsForCall(boolean value) {
        this.checkAgentsForCall = value;
    }

    public boolean isSetCheckAgentsForCall() {
        return (this.checkAgentsForCall!= null);
    }

    public void unsetCheckAgentsForCall() {
        this.checkAgentsForCall = null;
    }

}
