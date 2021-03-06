//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.app.params;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


/**
 * 
 *                   Represents generic parameters for experiments using
 *                   simulation.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimParams", propOrder = {
    "callTrace",
    "report",
    "randomStreams",
    "sequentialSampling",
    "controlVariables"
})
@XmlSeeAlso({
    BatchSimParams.class,
    RepSimParams.class
})
public abstract class SimParams {

    protected CallTraceParams callTrace;
    protected ReportParams report;
    protected RandomStreamsParams randomStreams;
    protected List<SequentialSamplingParams> sequentialSampling;
    @XmlElement(name = "controlVariable")
    protected List<ControlVariableParams> controlVariables;
    @XmlAttribute(name = "keepObs")
    protected Boolean keepObs;
    @XmlAttribute(name = "normalizeToDefaultUnit")
    protected Boolean normalizeToDefaultUnit;
    @XmlAttribute(name = "cpuTimeLimit")
    protected Duration cpuTimeLimit;
    @XmlAttribute(name = "enableChrono")
    protected Boolean enableChrono;
    @XmlAttribute(name = "restrictToPrintedStat")
    protected Boolean restrictToPrintedStat;
    @XmlAttribute(name = "estimateContactTypeAgentGroup")
    protected Boolean estimateContactTypeAgentGroup;

    /**
     * Gets the value of the callTrace property.
     * 
     * @return
     *     possible object is
     *     {@link CallTraceParams }
     *     
     */
    public CallTraceParams getCallTrace() {
        return callTrace;
    }

    /**
     * Sets the value of the callTrace property.
     * 
     * @param value
     *     allowed object is
     *     {@link CallTraceParams }
     *     
     */
    public void setCallTrace(CallTraceParams value) {
        this.callTrace = value;
    }

    public boolean isSetCallTrace() {
        return (this.callTrace!= null);
    }

    /**
     * Gets the value of the report property.
     * 
     * @return
     *     possible object is
     *     {@link ReportParams }
     *     
     */
    public ReportParams getReport() {
        return report;
    }

    /**
     * Sets the value of the report property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReportParams }
     *     
     */
    public void setReport(ReportParams value) {
        this.report = value;
    }

    public boolean isSetReport() {
        return (this.report!= null);
    }

    /**
     * Gets the value of the randomStreams property.
     * 
     * @return
     *     possible object is
     *     {@link RandomStreamsParams }
     *     
     */
    public RandomStreamsParams getRandomStreams() {
        return randomStreams;
    }

    /**
     * Sets the value of the randomStreams property.
     * 
     * @param value
     *     allowed object is
     *     {@link RandomStreamsParams }
     *     
     */
    public void setRandomStreams(RandomStreamsParams value) {
        this.randomStreams = value;
    }

    public boolean isSetRandomStreams() {
        return (this.randomStreams!= null);
    }

    /**
     * Gets the value of the sequentialSampling property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sequentialSampling property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSequentialSampling().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SequentialSamplingParams }
     * 
     * 
     */
    public List<SequentialSamplingParams> getSequentialSampling() {
        if (sequentialSampling == null) {
            sequentialSampling = new ArrayList<SequentialSamplingParams>();
        }
        return this.sequentialSampling;
    }

    public boolean isSetSequentialSampling() {
        return ((this.sequentialSampling!= null)&&(!this.sequentialSampling.isEmpty()));
    }

    public void unsetSequentialSampling() {
        this.sequentialSampling = null;
    }

    /**
     * Gets the value of the controlVariables property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the controlVariables property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getControlVariables().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ControlVariableParams }
     * 
     * 
     */
    public List<ControlVariableParams> getControlVariables() {
        if (controlVariables == null) {
            controlVariables = new ArrayList<ControlVariableParams>();
        }
        return this.controlVariables;
    }

    public boolean isSetControlVariables() {
        return ((this.controlVariables!= null)&&(!this.controlVariables.isEmpty()));
    }

    public void unsetControlVariables() {
        this.controlVariables = null;
    }

    /**
     * Gets the value of the keepObs property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isKeepObs() {
        if (keepObs == null) {
            return false;
        } else {
            return keepObs;
        }
    }

    /**
     * Sets the value of the keepObs property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setKeepObs(boolean value) {
        this.keepObs = value;
    }

    public boolean isSetKeepObs() {
        return (this.keepObs!= null);
    }

    public void unsetKeepObs() {
        this.keepObs = null;
    }

    /**
     * Gets the value of the normalizeToDefaultUnit property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isNormalizeToDefaultUnit() {
        if (normalizeToDefaultUnit == null) {
            return false;
        } else {
            return normalizeToDefaultUnit;
        }
    }

    /**
     * Sets the value of the normalizeToDefaultUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNormalizeToDefaultUnit(boolean value) {
        this.normalizeToDefaultUnit = value;
    }

    public boolean isSetNormalizeToDefaultUnit() {
        return (this.normalizeToDefaultUnit!= null);
    }

    public void unsetNormalizeToDefaultUnit() {
        this.normalizeToDefaultUnit = null;
    }

    /**
     * Gets the value of the cpuTimeLimit property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getCpuTimeLimit() {
        return cpuTimeLimit;
    }

    /**
     * Sets the value of the cpuTimeLimit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setCpuTimeLimit(Duration value) {
        this.cpuTimeLimit = value;
    }

    public boolean isSetCpuTimeLimit() {
        return (this.cpuTimeLimit!= null);
    }

    /**
     * Gets the value of the enableChrono property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEnableChrono() {
        if (enableChrono == null) {
            return true;
        } else {
            return enableChrono;
        }
    }

    /**
     * Sets the value of the enableChrono property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnableChrono(boolean value) {
        this.enableChrono = value;
    }

    public boolean isSetEnableChrono() {
        return (this.enableChrono!= null);
    }

    public void unsetEnableChrono() {
        this.enableChrono = null;
    }

    /**
     * Gets the value of the restrictToPrintedStat property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isRestrictToPrintedStat() {
        if (restrictToPrintedStat == null) {
            return false;
        } else {
            return restrictToPrintedStat;
        }
    }

    /**
     * Sets the value of the restrictToPrintedStat property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRestrictToPrintedStat(boolean value) {
        this.restrictToPrintedStat = value;
    }

    public boolean isSetRestrictToPrintedStat() {
        return (this.restrictToPrintedStat!= null);
    }

    public void unsetRestrictToPrintedStat() {
        this.restrictToPrintedStat = null;
    }

    /**
     * Gets the value of the estimateContactTypeAgentGroup property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEstimateContactTypeAgentGroup() {
        if (estimateContactTypeAgentGroup == null) {
            return false;
        } else {
            return estimateContactTypeAgentGroup;
        }
    }

    /**
     * Sets the value of the estimateContactTypeAgentGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEstimateContactTypeAgentGroup(boolean value) {
        this.estimateContactTypeAgentGroup = value;
    }

    public boolean isSetEstimateContactTypeAgentGroup() {
        return (this.estimateContactTypeAgentGroup!= null);
    }

    public void unsetEstimateContactTypeAgentGroup() {
        this.estimateContactTypeAgentGroup = null;
    }

}
