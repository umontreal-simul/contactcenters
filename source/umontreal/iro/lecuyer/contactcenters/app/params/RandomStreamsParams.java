//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.app.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                   Defines parameters of the random streams used during
 *                   an experiment based on simulation.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RandomStreamsParams", propOrder = {
    "streamSeed"
})
public class RandomStreamsParams {

    @XmlList
    @XmlElement(type = Double.class)
    protected double[] streamSeed;
    @XmlAttribute(name = "streamClass")
    protected String streamClass;
    @XmlAttribute(name = "caching")
    protected Boolean caching;

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getStreamSeed() {
        if (this.streamSeed == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.streamSeed.length] ;
        System.arraycopy(this.streamSeed, 0, retVal, 0, this.streamSeed.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Double }
     *     
     */
    public double getStreamSeed(int idx) {
        if (this.streamSeed == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.streamSeed[idx];
    }

    public int getStreamSeedLength() {
        if (this.streamSeed == null) {
            return  0;
        }
        return this.streamSeed.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setStreamSeed(double[] values) {
        int len = values.length;
        this.streamSeed = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.streamSeed[i] = new Double(values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public double setStreamSeed(int idx, double value) {
        return this.streamSeed[idx] = new Double(value);
    }

    public boolean isSetStreamSeed() {
        return ((this.streamSeed!= null)&&(this.streamSeed.length > 0));
    }

    public void unsetStreamSeed() {
        this.streamSeed = null;
    }

    /**
     * Gets the value of the streamClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStreamClass() {
        if (streamClass == null) {
            return "MRG32k3a";
        } else {
            return streamClass;
        }
    }

    /**
     * Sets the value of the streamClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStreamClass(String value) {
        this.streamClass = value;
    }

    public boolean isSetStreamClass() {
        return (this.streamClass!= null);
    }

    /**
     * Gets the value of the caching property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isCaching() {
        if (caching == null) {
            return false;
        } else {
            return caching;
        }
    }

    /**
     * Sets the value of the caching property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCaching(boolean value) {
        this.caching = value;
    }

    public boolean isSetCaching() {
        return (this.caching!= null);
    }

    public void unsetCaching() {
        this.caching = null;
    }

}
