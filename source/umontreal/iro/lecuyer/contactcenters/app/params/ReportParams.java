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
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.xmlbind.params.PropertiesParams;


/**
 * 
 *             Defines parameters for reports on simulation results.
 *             Reporting parameters include the statistics to put in
 *             the report, confidence level as well as general options.
 *          
 * 
 * <p>Java class for ReportParams complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReportParams">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="properties" type="{http://www.iro.umontreal.ca/lecuyer/ssj}PropertiesParams" minOccurs="0"/>
 *         &lt;element name="printedStat" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/app}PrintedStatParams" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="printedObs" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/app}PerformanceMeasureParams" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="shownContactTypeProperty" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/app}PropertyNameParam" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="shownAgentGroupProperty" type="{http://www.iro.umontreal.ca/lecuyer/contactcenters/app}PropertyNameParam" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="confidenceLevel" use="required" type="{http://www.iro.umontreal.ca/lecuyer/ssj}double01" />
 *       &lt;attribute name="numDigits" type="{http://www.iro.umontreal.ca/lecuyer/ssj}positiveInt" default="3" />
 *       &lt;attribute name="maxColumns" type="{http://www.iro.umontreal.ca/lecuyer/ssj}positiveInt" default="15" />
 *       &lt;attribute name="summarySheetName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="detailedSheetNameWithoutPeriods" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="detailedSheetNameWithPeriods" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="obsSheetName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="defaultDetailed" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="defaultPeriods" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="defaultOnlyAverages" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReportParams", propOrder = {
    "properties",
    "printedStats",
    "printedObs",
    "shownContactTypeProperties",
    "shownAgentGroupProperties"
})
public class ReportParams {

    protected PropertiesParams properties;
    @XmlElement(name = "printedStat")
    protected List<PrintedStatParams> printedStats;
    protected List<PerformanceMeasureParams> printedObs;
    @XmlElement(name = "shownContactTypeProperty")
    protected List<PropertyNameParam> shownContactTypeProperties;
    @XmlElement(name = "shownAgentGroupProperty")
    protected List<PropertyNameParam> shownAgentGroupProperties;
    @XmlAttribute(name = "confidenceLevel", required = true)
    protected double confidenceLevel;
    @XmlAttribute(name = "numDigits")
    protected Integer numDigits;
    @XmlAttribute(name = "maxColumns")
    protected Integer maxColumns;
    @XmlAttribute(name = "summarySheetName")
    protected String summarySheetName;
    @XmlAttribute(name = "detailedSheetNameWithoutPeriods")
    protected String detailedSheetNameWithoutPeriods;
    @XmlAttribute(name = "detailedSheetNameWithPeriods")
    protected String detailedSheetNameWithPeriods;
    @XmlAttribute(name = "obsSheetName")
    protected String obsSheetName;
    @XmlAttribute(name = "defaultDetailed")
    protected Boolean defaultDetailed;
    @XmlAttribute(name = "defaultPeriods")
    protected Boolean defaultPeriods;
    @XmlAttribute(name = "defaultOnlyAverages")
    protected Boolean defaultOnlyAverages;

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesParams }
     *     
     */
    public PropertiesParams getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesParams }
     *     
     */
    public void setProperties(PropertiesParams value) {
        this.properties = value;
    }

    public boolean isSetProperties() {
        return (this.properties!= null);
    }

    /**
     * Gets the value of the printedStats property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the printedStats property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrintedStats().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PrintedStatParams }
     * 
     * 
     */
    public List<PrintedStatParams> getPrintedStats() {
        if (printedStats == null) {
            printedStats = new ArrayList<PrintedStatParams>();
        }
        return this.printedStats;
    }

    public boolean isSetPrintedStats() {
        return ((this.printedStats!= null)&&(!this.printedStats.isEmpty()));
    }

    public void unsetPrintedStats() {
        this.printedStats = null;
    }

    /**
     * Gets the value of the printedObs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the printedObs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrintedObs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PerformanceMeasureParams }
     * 
     * 
     */
    public List<PerformanceMeasureParams> getPrintedObs() {
        if (printedObs == null) {
            printedObs = new ArrayList<PerformanceMeasureParams>();
        }
        return this.printedObs;
    }

    public boolean isSetPrintedObs() {
        return ((this.printedObs!= null)&&(!this.printedObs.isEmpty()));
    }

    public void unsetPrintedObs() {
        this.printedObs = null;
    }

    /**
     * Gets the value of the shownContactTypeProperties property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the shownContactTypeProperties property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getShownContactTypeProperties().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PropertyNameParam }
     * 
     * 
     */
    public List<PropertyNameParam> getShownContactTypeProperties() {
        if (shownContactTypeProperties == null) {
            shownContactTypeProperties = new ArrayList<PropertyNameParam>();
        }
        return this.shownContactTypeProperties;
    }

    public boolean isSetShownContactTypeProperties() {
        return ((this.shownContactTypeProperties!= null)&&(!this.shownContactTypeProperties.isEmpty()));
    }

    public void unsetShownContactTypeProperties() {
        this.shownContactTypeProperties = null;
    }

    /**
     * Gets the value of the shownAgentGroupProperties property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the shownAgentGroupProperties property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getShownAgentGroupProperties().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PropertyNameParam }
     * 
     * 
     */
    public List<PropertyNameParam> getShownAgentGroupProperties() {
        if (shownAgentGroupProperties == null) {
            shownAgentGroupProperties = new ArrayList<PropertyNameParam>();
        }
        return this.shownAgentGroupProperties;
    }

    public boolean isSetShownAgentGroupProperties() {
        return ((this.shownAgentGroupProperties!= null)&&(!this.shownAgentGroupProperties.isEmpty()));
    }

    public void unsetShownAgentGroupProperties() {
        this.shownAgentGroupProperties = null;
    }

    /**
     * Gets the value of the confidenceLevel property.
     * 
     */
    public double getConfidenceLevel() {
        return confidenceLevel;
    }

    /**
     * Sets the value of the confidenceLevel property.
     * 
     */
    public void setConfidenceLevel(double value) {
        this.confidenceLevel = value;
    }

    public boolean isSetConfidenceLevel() {
        return true;
    }

    /**
     * Gets the value of the numDigits property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getNumDigits() {
        if (numDigits == null) {
            return  3;
        } else {
            return numDigits;
        }
    }

    /**
     * Sets the value of the numDigits property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumDigits(int value) {
        this.numDigits = value;
    }

    public boolean isSetNumDigits() {
        return (this.numDigits!= null);
    }

    public void unsetNumDigits() {
        this.numDigits = null;
    }

    /**
     * Gets the value of the maxColumns property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getMaxColumns() {
        if (maxColumns == null) {
            return  15;
        } else {
            return maxColumns;
        }
    }

    /**
     * Sets the value of the maxColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxColumns(int value) {
        this.maxColumns = value;
    }

    public boolean isSetMaxColumns() {
        return (this.maxColumns!= null);
    }

    public void unsetMaxColumns() {
        this.maxColumns = null;
    }

    /**
     * Gets the value of the summarySheetName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSummarySheetName() {
        return summarySheetName;
    }

    /**
     * Sets the value of the summarySheetName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSummarySheetName(String value) {
        this.summarySheetName = value;
    }

    public boolean isSetSummarySheetName() {
        return (this.summarySheetName!= null);
    }

    /**
     * Gets the value of the detailedSheetNameWithoutPeriods property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDetailedSheetNameWithoutPeriods() {
        return detailedSheetNameWithoutPeriods;
    }

    /**
     * Sets the value of the detailedSheetNameWithoutPeriods property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDetailedSheetNameWithoutPeriods(String value) {
        this.detailedSheetNameWithoutPeriods = value;
    }

    public boolean isSetDetailedSheetNameWithoutPeriods() {
        return (this.detailedSheetNameWithoutPeriods!= null);
    }

    /**
     * Gets the value of the detailedSheetNameWithPeriods property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDetailedSheetNameWithPeriods() {
        return detailedSheetNameWithPeriods;
    }

    /**
     * Sets the value of the detailedSheetNameWithPeriods property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDetailedSheetNameWithPeriods(String value) {
        this.detailedSheetNameWithPeriods = value;
    }

    public boolean isSetDetailedSheetNameWithPeriods() {
        return (this.detailedSheetNameWithPeriods!= null);
    }

    /**
     * Gets the value of the obsSheetName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getObsSheetName() {
        return obsSheetName;
    }

    /**
     * Sets the value of the obsSheetName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setObsSheetName(String value) {
        this.obsSheetName = value;
    }

    public boolean isSetObsSheetName() {
        return (this.obsSheetName!= null);
    }

    /**
     * Gets the value of the defaultDetailed property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDefaultDetailed() {
        if (defaultDetailed == null) {
            return true;
        } else {
            return defaultDetailed;
        }
    }

    /**
     * Sets the value of the defaultDetailed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDefaultDetailed(boolean value) {
        this.defaultDetailed = value;
    }

    public boolean isSetDefaultDetailed() {
        return (this.defaultDetailed!= null);
    }

    public void unsetDefaultDetailed() {
        this.defaultDetailed = null;
    }

    /**
     * Gets the value of the defaultPeriods property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDefaultPeriods() {
        if (defaultPeriods == null) {
            return true;
        } else {
            return defaultPeriods;
        }
    }

    /**
     * Sets the value of the defaultPeriods property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDefaultPeriods(boolean value) {
        this.defaultPeriods = value;
    }

    public boolean isSetDefaultPeriods() {
        return (this.defaultPeriods!= null);
    }

    public void unsetDefaultPeriods() {
        this.defaultPeriods = null;
    }

    /**
     * Gets the value of the defaultOnlyAverages property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDefaultOnlyAverages() {
        if (defaultOnlyAverages == null) {
            return false;
        } else {
            return defaultOnlyAverages;
        }
    }

    /**
     * Sets the value of the defaultOnlyAverages property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDefaultOnlyAverages(boolean value) {
        this.defaultOnlyAverages = value;
    }

    public boolean isSetDefaultOnlyAverages() {
        return (this.defaultOnlyAverages!= null);
    }

    public void unsetDefaultOnlyAverages() {
        this.defaultOnlyAverages = null;
    }

}
