//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.xmlbind.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


/**
 * 
 *                   Represents a property whose value is a list of time
 *                   durations.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DurationListProperty", propOrder = {
    "value"
})
public class DurationListProperty
    extends AbstractProperty
{

    @XmlList
    @XmlElement(required = true)
    protected Duration[] value;

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Duration }
     *     
     */
    public Duration[] getValue() {
        if (this.value == null) {
            return new Duration[ 0 ] ;
        }
        Duration[] retVal = new Duration[this.value.length] ;
        System.arraycopy(this.value, 0, retVal, 0, this.value.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Duration }
     *     
     */
    public Duration getValue(int idx) {
        if (this.value == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.value[idx];
    }

    public int getValueLength() {
        if (this.value == null) {
            return  0;
        }
        return this.value.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Duration }
     *     
     */
    public void setValue(Duration[] values) {
        int len = values.length;
        this.value = ((Duration[]) new Duration[len] );
        for (int i = 0; (i<len); i ++) {
            this.value[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public Duration setValue(int idx, Duration value) {
        return this.value[idx] = value;
    }

    public boolean isSetValue() {
        return ((this.value!= null)&&(this.value.length > 0));
    }

    public void unsetValue() {
        this.value = null;
    }

}
