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
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 *                   Gives parameters for a segment which is defined here
 *                   as a group of contact types, agent groups, or main
 *                   periods.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentParams", propOrder = {
    "values"
})
public class SegmentParams
    extends Named
{

    @XmlList
    @XmlElement(type = Integer.class)
    protected List<Integer> values;

    /**
     * Gets the value of the values property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the values property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValues().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Integer }
     * 
     * 
     */
    public List<Integer> getValues() {
        if (values == null) {
            values = new ArrayList<Integer>();
        }
        return this.values;
    }

    public boolean isSetValues() {
        return ((this.values!= null)&&(!this.values.isEmpty()));
    }

    public void unsetValues() {
        this.values = null;
    }

}
