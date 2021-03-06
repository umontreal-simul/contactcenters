//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.msk.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 * 	Contains the vectors of ranks used by default, if
 * 	no other case applies for a given routing stage.
 * 	
 * 
 * <p>Java class for DefaultCaseParams complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DefaultCaseParams">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.iro.umontreal.ca/lecuyer/contactcenters/msk}VectorsOfRanks"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DefaultCaseParams", propOrder = {
    "agentGroupRanks",
    "agentGroupRanksRel",
    "agentGroupRanksFunc",
    "queueRanks",
    "queueRanksRel",
    "queueRanksFunc"
})
public class DefaultCaseParams {

    @XmlList
    @XmlElement(type = Double.class)
    protected double[] agentGroupRanks;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] agentGroupRanksRel;
    protected Named agentGroupRanksFunc;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] queueRanks;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] queueRanksRel;
    protected Named queueRanksFunc;

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getAgentGroupRanks() {
        if (this.agentGroupRanks == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.agentGroupRanks.length] ;
        System.arraycopy(this.agentGroupRanks, 0, retVal, 0, this.agentGroupRanks.length);
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
    public double getAgentGroupRanks(int idx) {
        if (this.agentGroupRanks == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.agentGroupRanks[idx];
    }

    public int getAgentGroupRanksLength() {
        if (this.agentGroupRanks == null) {
            return  0;
        }
        return this.agentGroupRanks.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setAgentGroupRanks(double[] values) {
        int len = values.length;
        this.agentGroupRanks = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.agentGroupRanks[i] = new Double(values[i]);
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
    public double setAgentGroupRanks(int idx, double value) {
        return this.agentGroupRanks[idx] = new Double(value);
    }

    public boolean isSetAgentGroupRanks() {
        return ((this.agentGroupRanks!= null)&&(this.agentGroupRanks.length > 0));
    }

    public void unsetAgentGroupRanks() {
        this.agentGroupRanks = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getAgentGroupRanksRel() {
        if (this.agentGroupRanksRel == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.agentGroupRanksRel.length] ;
        System.arraycopy(this.agentGroupRanksRel, 0, retVal, 0, this.agentGroupRanksRel.length);
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
    public double getAgentGroupRanksRel(int idx) {
        if (this.agentGroupRanksRel == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.agentGroupRanksRel[idx];
    }

    public int getAgentGroupRanksRelLength() {
        if (this.agentGroupRanksRel == null) {
            return  0;
        }
        return this.agentGroupRanksRel.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setAgentGroupRanksRel(double[] values) {
        int len = values.length;
        this.agentGroupRanksRel = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.agentGroupRanksRel[i] = new Double(values[i]);
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
    public double setAgentGroupRanksRel(int idx, double value) {
        return this.agentGroupRanksRel[idx] = new Double(value);
    }

    public boolean isSetAgentGroupRanksRel() {
        return ((this.agentGroupRanksRel!= null)&&(this.agentGroupRanksRel.length > 0));
    }

    public void unsetAgentGroupRanksRel() {
        this.agentGroupRanksRel = null;
    }

    /**
     * Gets the value of the agentGroupRanksFunc property.
     * 
     * @return
     *     possible object is
     *     {@link Named }
     *     
     */
    public Named getAgentGroupRanksFunc() {
        return agentGroupRanksFunc;
    }

    /**
     * Sets the value of the agentGroupRanksFunc property.
     * 
     * @param value
     *     allowed object is
     *     {@link Named }
     *     
     */
    public void setAgentGroupRanksFunc(Named value) {
        this.agentGroupRanksFunc = value;
    }

    public boolean isSetAgentGroupRanksFunc() {
        return (this.agentGroupRanksFunc!= null);
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getQueueRanks() {
        if (this.queueRanks == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.queueRanks.length] ;
        System.arraycopy(this.queueRanks, 0, retVal, 0, this.queueRanks.length);
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
    public double getQueueRanks(int idx) {
        if (this.queueRanks == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.queueRanks[idx];
    }

    public int getQueueRanksLength() {
        if (this.queueRanks == null) {
            return  0;
        }
        return this.queueRanks.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setQueueRanks(double[] values) {
        int len = values.length;
        this.queueRanks = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.queueRanks[i] = new Double(values[i]);
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
    public double setQueueRanks(int idx, double value) {
        return this.queueRanks[idx] = new Double(value);
    }

    public boolean isSetQueueRanks() {
        return ((this.queueRanks!= null)&&(this.queueRanks.length > 0));
    }

    public void unsetQueueRanks() {
        this.queueRanks = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getQueueRanksRel() {
        if (this.queueRanksRel == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.queueRanksRel.length] ;
        System.arraycopy(this.queueRanksRel, 0, retVal, 0, this.queueRanksRel.length);
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
    public double getQueueRanksRel(int idx) {
        if (this.queueRanksRel == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.queueRanksRel[idx];
    }

    public int getQueueRanksRelLength() {
        if (this.queueRanksRel == null) {
            return  0;
        }
        return this.queueRanksRel.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setQueueRanksRel(double[] values) {
        int len = values.length;
        this.queueRanksRel = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.queueRanksRel[i] = new Double(values[i]);
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
    public double setQueueRanksRel(int idx, double value) {
        return this.queueRanksRel[idx] = new Double(value);
    }

    public boolean isSetQueueRanksRel() {
        return ((this.queueRanksRel!= null)&&(this.queueRanksRel.length > 0));
    }

    public void unsetQueueRanksRel() {
        this.queueRanksRel = null;
    }

    /**
     * Gets the value of the queueRanksFunc property.
     * 
     * @return
     *     possible object is
     *     {@link Named }
     *     
     */
    public Named getQueueRanksFunc() {
        return queueRanksFunc;
    }

    /**
     * Sets the value of the queueRanksFunc property.
     * 
     * @param value
     *     allowed object is
     *     {@link Named }
     *     
     */
    public void setQueueRanksFunc(Named value) {
        this.queueRanksFunc = value;
    }

    public boolean isSetQueueRanksFunc() {
        return (this.queueRanksFunc!= null);
    }

}
