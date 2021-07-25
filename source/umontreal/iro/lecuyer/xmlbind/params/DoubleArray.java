//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.xmlbind.params;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import umontreal.iro.lecuyer.contactcenters.app.params.PMMatrix;
import umontreal.iro.lecuyer.contactcenters.msk.params.DoubleArrayWithMinWaitingTime;


/**
 * 
 *                   Represents a 2D array of double-precision numbers.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DoubleArray", propOrder = {
    "rows"
})
@XmlSeeAlso({
    DoubleArrayWithMinWaitingTime.class,
    PMMatrix.class
})
public class DoubleArray {

    @XmlElement(name = "row")
    protected List<DoubleArray.Row> rows;

    /**
     * Gets the value of the rows property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rows property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRows().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DoubleArray.Row }
     * 
     * 
     */
    public List<DoubleArray.Row> getRows() {
        if (rows == null) {
            rows = new ArrayList<DoubleArray.Row>();
        }
        return this.rows;
    }

    public boolean isSetRows() {
        return ((this.rows!= null)&&(!this.rows.isEmpty()));
    }

    public void unsetRows() {
        this.rows = null;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.iro.umontreal.ca/lecuyer/ssj>doubleList">
     *       &lt;attribute name="repeat" type="{http://www.iro.umontreal.ca/lecuyer/ssj}nonNegativeInt" default="1" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Row {

        @XmlValue
        protected List<Double> value;
        @XmlAttribute(name = "repeat")
        protected Integer repeat;

        /**
         * Gets the value of the value property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the value property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getValue().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Double }
         * 
         * 
         */
        public List<Double> getValue() {
            if (value == null) {
                value = new ArrayList<Double>();
            }
            return this.value;
        }

        public boolean isSetValue() {
            return ((this.value!= null)&&(!this.value.isEmpty()));
        }

        public void unsetValue() {
            this.value = null;
        }

        /**
         * Gets the value of the repeat property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public int getRepeat() {
            if (repeat == null) {
                return  1;
            } else {
                return repeat;
            }
        }

        /**
         * Sets the value of the repeat property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setRepeat(int value) {
            this.repeat = value;
        }

        public boolean isSetRepeat() {
            return (this.repeat!= null);
        }

        public void unsetRepeat() {
            this.repeat = null;
        }

    }

}
