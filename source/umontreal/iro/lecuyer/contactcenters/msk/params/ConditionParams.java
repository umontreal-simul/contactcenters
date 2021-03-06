//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.msk.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 *    Represents a condition on the state of a call center.
 *    
 * 
 * <p>Java class for ConditionParams complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConditionParams">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.iro.umontreal.ca/lecuyer/contactcenters/msk}Conditions"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConditionParams", propOrder = {
    "either",
    "all",
    "queueSizes",
    "queueSizeThresh",
    "numFreeAgents",
    "numFreeAgentsThresh",
    "fracBusyAgents",
    "fracBusyAgentsThresh",
    "stat",
    "custom"
})
@XmlSeeAlso({
    RoutingCaseParams.class
})
public class ConditionParams {

    protected ConditionParamsList either;
    protected ConditionParamsList all;
    protected TwoIndicesWithTypesParams queueSizes;
    protected IndexThreshIntWithTypeParams queueSizeThresh;
    protected TwoIndicesParams numFreeAgents;
    protected IndexThreshIntParams numFreeAgentsThresh;
    protected TwoIndicesWithTypesParams fracBusyAgents;
    protected IndexThreshWithTypeParams fracBusyAgentsThresh;
    protected StatConditionParams stat;
    protected Named custom;

    /**
     * Gets the value of the either property.
     * 
     * @return
     *     possible object is
     *     {@link ConditionParamsList }
     *     
     */
    public ConditionParamsList getEither() {
        return either;
    }

    /**
     * Sets the value of the either property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConditionParamsList }
     *     
     */
    public void setEither(ConditionParamsList value) {
        this.either = value;
    }

    public boolean isSetEither() {
        return (this.either!= null);
    }

    /**
     * Gets the value of the all property.
     * 
     * @return
     *     possible object is
     *     {@link ConditionParamsList }
     *     
     */
    public ConditionParamsList getAll() {
        return all;
    }

    /**
     * Sets the value of the all property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConditionParamsList }
     *     
     */
    public void setAll(ConditionParamsList value) {
        this.all = value;
    }

    public boolean isSetAll() {
        return (this.all!= null);
    }

    /**
     * Gets the value of the queueSizes property.
     * 
     * @return
     *     possible object is
     *     {@link TwoIndicesWithTypesParams }
     *     
     */
    public TwoIndicesWithTypesParams getQueueSizes() {
        return queueSizes;
    }

    /**
     * Sets the value of the queueSizes property.
     * 
     * @param value
     *     allowed object is
     *     {@link TwoIndicesWithTypesParams }
     *     
     */
    public void setQueueSizes(TwoIndicesWithTypesParams value) {
        this.queueSizes = value;
    }

    public boolean isSetQueueSizes() {
        return (this.queueSizes!= null);
    }

    /**
     * Gets the value of the queueSizeThresh property.
     * 
     * @return
     *     possible object is
     *     {@link IndexThreshIntWithTypeParams }
     *     
     */
    public IndexThreshIntWithTypeParams getQueueSizeThresh() {
        return queueSizeThresh;
    }

    /**
     * Sets the value of the queueSizeThresh property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexThreshIntWithTypeParams }
     *     
     */
    public void setQueueSizeThresh(IndexThreshIntWithTypeParams value) {
        this.queueSizeThresh = value;
    }

    public boolean isSetQueueSizeThresh() {
        return (this.queueSizeThresh!= null);
    }

    /**
     * Gets the value of the numFreeAgents property.
     * 
     * @return
     *     possible object is
     *     {@link TwoIndicesParams }
     *     
     */
    public TwoIndicesParams getNumFreeAgents() {
        return numFreeAgents;
    }

    /**
     * Sets the value of the numFreeAgents property.
     * 
     * @param value
     *     allowed object is
     *     {@link TwoIndicesParams }
     *     
     */
    public void setNumFreeAgents(TwoIndicesParams value) {
        this.numFreeAgents = value;
    }

    public boolean isSetNumFreeAgents() {
        return (this.numFreeAgents!= null);
    }

    /**
     * Gets the value of the numFreeAgentsThresh property.
     * 
     * @return
     *     possible object is
     *     {@link IndexThreshIntParams }
     *     
     */
    public IndexThreshIntParams getNumFreeAgentsThresh() {
        return numFreeAgentsThresh;
    }

    /**
     * Sets the value of the numFreeAgentsThresh property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexThreshIntParams }
     *     
     */
    public void setNumFreeAgentsThresh(IndexThreshIntParams value) {
        this.numFreeAgentsThresh = value;
    }

    public boolean isSetNumFreeAgentsThresh() {
        return (this.numFreeAgentsThresh!= null);
    }

    /**
     * Gets the value of the fracBusyAgents property.
     * 
     * @return
     *     possible object is
     *     {@link TwoIndicesWithTypesParams }
     *     
     */
    public TwoIndicesWithTypesParams getFracBusyAgents() {
        return fracBusyAgents;
    }

    /**
     * Sets the value of the fracBusyAgents property.
     * 
     * @param value
     *     allowed object is
     *     {@link TwoIndicesWithTypesParams }
     *     
     */
    public void setFracBusyAgents(TwoIndicesWithTypesParams value) {
        this.fracBusyAgents = value;
    }

    public boolean isSetFracBusyAgents() {
        return (this.fracBusyAgents!= null);
    }

    /**
     * Gets the value of the fracBusyAgentsThresh property.
     * 
     * @return
     *     possible object is
     *     {@link IndexThreshWithTypeParams }
     *     
     */
    public IndexThreshWithTypeParams getFracBusyAgentsThresh() {
        return fracBusyAgentsThresh;
    }

    /**
     * Sets the value of the fracBusyAgentsThresh property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexThreshWithTypeParams }
     *     
     */
    public void setFracBusyAgentsThresh(IndexThreshWithTypeParams value) {
        this.fracBusyAgentsThresh = value;
    }

    public boolean isSetFracBusyAgentsThresh() {
        return (this.fracBusyAgentsThresh!= null);
    }

    /**
     * Gets the value of the stat property.
     * 
     * @return
     *     possible object is
     *     {@link StatConditionParams }
     *     
     */
    public StatConditionParams getStat() {
        return stat;
    }

    /**
     * Sets the value of the stat property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatConditionParams }
     *     
     */
    public void setStat(StatConditionParams value) {
        this.stat = value;
    }

    public boolean isSetStat() {
        return (this.stat!= null);
    }

    /**
     * Gets the value of the custom property.
     * 
     * @return
     *     possible object is
     *     {@link Named }
     *     
     */
    public Named getCustom() {
        return custom;
    }

    /**
     * Sets the value of the custom property.
     * 
     * @param value
     *     allowed object is
     *     {@link Named }
     *     
     */
    public void setCustom(Named value) {
        this.custom = value;
    }

    public boolean isSetCustom() {
        return (this.custom!= null);
    }

}
