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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 *                   Defines parameters for an individual agent.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentParams", propOrder = {
    "shift",
    "serviceTime"
})
public class AgentParams
    extends Named
{

    @XmlElement(required = true)
    protected ScheduleShiftParams shift;
    protected List<ServiceTimeParams> serviceTime;

    /**
     * Gets the value of the shift property.
     * 
     * @return
     *     possible object is
     *     {@link ScheduleShiftParams }
     *     
     */
    public ScheduleShiftParams getShift() {
        return shift;
    }

    /**
     * Sets the value of the shift property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScheduleShiftParams }
     *     
     */
    public void setShift(ScheduleShiftParams value) {
        this.shift = value;
    }

    public boolean isSetShift() {
        return (this.shift!= null);
    }

    /**
     * Gets the value of the serviceTime property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the serviceTime property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getServiceTime().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ServiceTimeParams }
     * 
     * 
     */
    public List<ServiceTimeParams> getServiceTime() {
        if (serviceTime == null) {
            serviceTime = new ArrayList<ServiceTimeParams>();
        }
        return this.serviceTime;
    }

    public boolean isSetServiceTime() {
        return ((this.serviceTime!= null)&&(!this.serviceTime.isEmpty()));
    }

    public void unsetServiceTime() {
        this.serviceTime = null;
    }

}