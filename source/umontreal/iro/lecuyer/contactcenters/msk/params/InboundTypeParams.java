//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.msk.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                   Defines the parameters for an inbound call type, for
 *                   blend/multi-skill call center.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InboundTypeParams", propOrder = {
    "arrivalProcess"
})
public class InboundTypeParams
    extends CallTypeParams
{

    protected ArrivalProcessParams arrivalProcess;

    /**
     * Gets the value of the arrivalProcess property.
     * 
     * @return
     *     possible object is
     *     {@link ArrivalProcessParams }
     *     
     */
    public ArrivalProcessParams getArrivalProcess() {
        return arrivalProcess;
    }

    /**
     * Sets the value of the arrivalProcess property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrivalProcessParams }
     *     
     */
    public void setArrivalProcess(ArrivalProcessParams value) {
        this.arrivalProcess = value;
    }

    public boolean isSetArrivalProcess() {
        return (this.arrivalProcess!= null);
    }

}
