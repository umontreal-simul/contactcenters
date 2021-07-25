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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import umontreal.iro.lecuyer.contactcenters.params.MultiPeriodGenParams;
import umontreal.iro.lecuyer.xmlbind.params.IntArray;
import umontreal.iro.lecuyer.xmlbind.params.Named;


/**
 * 
 *                   Contains the parameters for an agent group in a
 *                   blend/multi-skill call center.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentGroupParams", propOrder = {
    "staffing",
    "probAgents",
    "staffingData",
    "schedule",
    "agents",
    "serviceTimesMult",
    "probDisconnect",
    "disconnectTime",
    "maxAgentsPeriod",
    "minAgentsPeriod",
    "idleCostPeriod",
    "busyCostPeriod",
    "perUseCostPeriod",
    "weightPeriod"
})
public class AgentGroupParams
    extends Named
{

    @XmlList
    @XmlElement(type = Integer.class)
    protected int[] staffing;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] probAgents;
    protected IntArray staffingData;
    protected AgentGroupScheduleParams schedule;
    @XmlElement(name = "agent")
    protected List<AgentParams> agents;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] serviceTimesMult;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] probDisconnect;
    protected MultiPeriodGenParams disconnectTime;
    @XmlList
    @XmlElement(type = Integer.class)
    protected int[] maxAgentsPeriod;
    @XmlList
    @XmlElement(type = Integer.class)
    protected int[] minAgentsPeriod;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] idleCostPeriod;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] busyCostPeriod;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] perUseCostPeriod;
    @XmlList
    @XmlElement(type = Double.class)
    protected double[] weightPeriod;
    @XmlAttribute(name = "weight")
    protected Double weight;
    @XmlAttribute(name = "maxAgents")
    protected Integer maxAgents;
    @XmlAttribute(name = "minAgents")
    protected Integer minAgents;
    @XmlAttribute(name = "idleCost")
    protected Double idleCost;
    @XmlAttribute(name = "busyCost")
    protected Double busyCost;
    @XmlAttribute(name = "perUseCost")
    protected Double perUseCost;
    @XmlAttribute(name = "efficiency")
    protected Double efficiency;
    @XmlAttribute(name = "skillCount")
    protected Integer skillCount;
    @XmlAttribute(name = "detailed")
    protected Boolean detailed;
    @XmlAttribute(name = "convertSchedulesToStaffing")
    protected Boolean convertSchedulesToStaffing;
    @XmlAttribute(name = "agentsMult")
    protected Double agentsMult;

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Integer }
     *     
     */
    public int[] getStaffing() {
        if (this.staffing == null) {
            return new int[ 0 ] ;
        }
        int[] retVal = new int[this.staffing.length] ;
        System.arraycopy(this.staffing, 0, retVal, 0, this.staffing.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Integer }
     *     
     */
    public int getStaffing(int idx) {
        if (this.staffing == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.staffing[idx];
    }

    public int getStaffingLength() {
        if (this.staffing == null) {
            return  0;
        }
        return this.staffing.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Integer }
     *     
     */
    public void setStaffing(int[] values) {
        int len = values.length;
        this.staffing = ((int[]) new int[len] );
        for (int i = 0; (i<len); i ++) {
            this.staffing[i] = new Integer(values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public int setStaffing(int idx, int value) {
        return this.staffing[idx] = new Integer(value);
    }

    public boolean isSetStaffing() {
        return ((this.staffing!= null)&&(this.staffing.length > 0));
    }

    public void unsetStaffing() {
        this.staffing = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getProbAgents() {
        if (this.probAgents == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.probAgents.length] ;
        System.arraycopy(this.probAgents, 0, retVal, 0, this.probAgents.length);
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
    public double getProbAgents(int idx) {
        if (this.probAgents == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.probAgents[idx];
    }

    public int getProbAgentsLength() {
        if (this.probAgents == null) {
            return  0;
        }
        return this.probAgents.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setProbAgents(double[] values) {
        int len = values.length;
        this.probAgents = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.probAgents[i] = new Double(values[i]);
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
    public double setProbAgents(int idx, double value) {
        return this.probAgents[idx] = new Double(value);
    }

    public boolean isSetProbAgents() {
        return ((this.probAgents!= null)&&(this.probAgents.length > 0));
    }

    public void unsetProbAgents() {
        this.probAgents = null;
    }

    /**
     * Gets the value of the staffingData property.
     * 
     * @return
     *     possible object is
     *     {@link IntArray }
     *     
     */
    public IntArray getStaffingData() {
        return staffingData;
    }

    /**
     * Sets the value of the staffingData property.
     * 
     * @param value
     *     allowed object is
     *     {@link IntArray }
     *     
     */
    public void setStaffingData(IntArray value) {
        this.staffingData = value;
    }

    public boolean isSetStaffingData() {
        return (this.staffingData!= null);
    }

    /**
     * Gets the value of the schedule property.
     * 
     * @return
     *     possible object is
     *     {@link AgentGroupScheduleParams }
     *     
     */
    public AgentGroupScheduleParams getSchedule() {
        return schedule;
    }

    /**
     * Sets the value of the schedule property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentGroupScheduleParams }
     *     
     */
    public void setSchedule(AgentGroupScheduleParams value) {
        this.schedule = value;
    }

    public boolean isSetSchedule() {
        return (this.schedule!= null);
    }

    /**
     * Gets the value of the agents property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the agents property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAgents().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AgentParams }
     * 
     * 
     */
    public List<AgentParams> getAgents() {
        if (agents == null) {
            agents = new ArrayList<AgentParams>();
        }
        return this.agents;
    }

    public boolean isSetAgents() {
        return ((this.agents!= null)&&(!this.agents.isEmpty()));
    }

    public void unsetAgents() {
        this.agents = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getServiceTimesMult() {
        if (this.serviceTimesMult == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.serviceTimesMult.length] ;
        System.arraycopy(this.serviceTimesMult, 0, retVal, 0, this.serviceTimesMult.length);
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
    public double getServiceTimesMult(int idx) {
        if (this.serviceTimesMult == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.serviceTimesMult[idx];
    }

    public int getServiceTimesMultLength() {
        if (this.serviceTimesMult == null) {
            return  0;
        }
        return this.serviceTimesMult.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setServiceTimesMult(double[] values) {
        int len = values.length;
        this.serviceTimesMult = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.serviceTimesMult[i] = new Double(values[i]);
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
    public double setServiceTimesMult(int idx, double value) {
        return this.serviceTimesMult[idx] = new Double(value);
    }

    public boolean isSetServiceTimesMult() {
        return ((this.serviceTimesMult!= null)&&(this.serviceTimesMult.length > 0));
    }

    public void unsetServiceTimesMult() {
        this.serviceTimesMult = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getProbDisconnect() {
        if (this.probDisconnect == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.probDisconnect.length] ;
        System.arraycopy(this.probDisconnect, 0, retVal, 0, this.probDisconnect.length);
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
    public double getProbDisconnect(int idx) {
        if (this.probDisconnect == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.probDisconnect[idx];
    }

    public int getProbDisconnectLength() {
        if (this.probDisconnect == null) {
            return  0;
        }
        return this.probDisconnect.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setProbDisconnect(double[] values) {
        int len = values.length;
        this.probDisconnect = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.probDisconnect[i] = new Double(values[i]);
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
    public double setProbDisconnect(int idx, double value) {
        return this.probDisconnect[idx] = new Double(value);
    }

    public boolean isSetProbDisconnect() {
        return ((this.probDisconnect!= null)&&(this.probDisconnect.length > 0));
    }

    public void unsetProbDisconnect() {
        this.probDisconnect = null;
    }

    /**
     * Gets the value of the disconnectTime property.
     * 
     * @return
     *     possible object is
     *     {@link MultiPeriodGenParams }
     *     
     */
    public MultiPeriodGenParams getDisconnectTime() {
        return disconnectTime;
    }

    /**
     * Sets the value of the disconnectTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultiPeriodGenParams }
     *     
     */
    public void setDisconnectTime(MultiPeriodGenParams value) {
        this.disconnectTime = value;
    }

    public boolean isSetDisconnectTime() {
        return (this.disconnectTime!= null);
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Integer }
     *     
     */
    public int[] getMaxAgentsPeriod() {
        if (this.maxAgentsPeriod == null) {
            return new int[ 0 ] ;
        }
        int[] retVal = new int[this.maxAgentsPeriod.length] ;
        System.arraycopy(this.maxAgentsPeriod, 0, retVal, 0, this.maxAgentsPeriod.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Integer }
     *     
     */
    public int getMaxAgentsPeriod(int idx) {
        if (this.maxAgentsPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.maxAgentsPeriod[idx];
    }

    public int getMaxAgentsPeriodLength() {
        if (this.maxAgentsPeriod == null) {
            return  0;
        }
        return this.maxAgentsPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Integer }
     *     
     */
    public void setMaxAgentsPeriod(int[] values) {
        int len = values.length;
        this.maxAgentsPeriod = ((int[]) new int[len] );
        for (int i = 0; (i<len); i ++) {
            this.maxAgentsPeriod[i] = new Integer(values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public int setMaxAgentsPeriod(int idx, int value) {
        return this.maxAgentsPeriod[idx] = new Integer(value);
    }

    public boolean isSetMaxAgentsPeriod() {
        return ((this.maxAgentsPeriod!= null)&&(this.maxAgentsPeriod.length > 0));
    }

    public void unsetMaxAgentsPeriod() {
        this.maxAgentsPeriod = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Integer }
     *     
     */
    public int[] getMinAgentsPeriod() {
        if (this.minAgentsPeriod == null) {
            return new int[ 0 ] ;
        }
        int[] retVal = new int[this.minAgentsPeriod.length] ;
        System.arraycopy(this.minAgentsPeriod, 0, retVal, 0, this.minAgentsPeriod.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Integer }
     *     
     */
    public int getMinAgentsPeriod(int idx) {
        if (this.minAgentsPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.minAgentsPeriod[idx];
    }

    public int getMinAgentsPeriodLength() {
        if (this.minAgentsPeriod == null) {
            return  0;
        }
        return this.minAgentsPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Integer }
     *     
     */
    public void setMinAgentsPeriod(int[] values) {
        int len = values.length;
        this.minAgentsPeriod = ((int[]) new int[len] );
        for (int i = 0; (i<len); i ++) {
            this.minAgentsPeriod[i] = new Integer(values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public int setMinAgentsPeriod(int idx, int value) {
        return this.minAgentsPeriod[idx] = new Integer(value);
    }

    public boolean isSetMinAgentsPeriod() {
        return ((this.minAgentsPeriod!= null)&&(this.minAgentsPeriod.length > 0));
    }

    public void unsetMinAgentsPeriod() {
        this.minAgentsPeriod = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getIdleCostPeriod() {
        if (this.idleCostPeriod == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.idleCostPeriod.length] ;
        System.arraycopy(this.idleCostPeriod, 0, retVal, 0, this.idleCostPeriod.length);
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
    public double getIdleCostPeriod(int idx) {
        if (this.idleCostPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.idleCostPeriod[idx];
    }

    public int getIdleCostPeriodLength() {
        if (this.idleCostPeriod == null) {
            return  0;
        }
        return this.idleCostPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setIdleCostPeriod(double[] values) {
        int len = values.length;
        this.idleCostPeriod = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.idleCostPeriod[i] = new Double(values[i]);
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
    public double setIdleCostPeriod(int idx, double value) {
        return this.idleCostPeriod[idx] = new Double(value);
    }

    public boolean isSetIdleCostPeriod() {
        return ((this.idleCostPeriod!= null)&&(this.idleCostPeriod.length > 0));
    }

    public void unsetIdleCostPeriod() {
        this.idleCostPeriod = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getBusyCostPeriod() {
        if (this.busyCostPeriod == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.busyCostPeriod.length] ;
        System.arraycopy(this.busyCostPeriod, 0, retVal, 0, this.busyCostPeriod.length);
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
    public double getBusyCostPeriod(int idx) {
        if (this.busyCostPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.busyCostPeriod[idx];
    }

    public int getBusyCostPeriodLength() {
        if (this.busyCostPeriod == null) {
            return  0;
        }
        return this.busyCostPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setBusyCostPeriod(double[] values) {
        int len = values.length;
        this.busyCostPeriod = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.busyCostPeriod[i] = new Double(values[i]);
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
    public double setBusyCostPeriod(int idx, double value) {
        return this.busyCostPeriod[idx] = new Double(value);
    }

    public boolean isSetBusyCostPeriod() {
        return ((this.busyCostPeriod!= null)&&(this.busyCostPeriod.length > 0));
    }

    public void unsetBusyCostPeriod() {
        this.busyCostPeriod = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getPerUseCostPeriod() {
        if (this.perUseCostPeriod == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.perUseCostPeriod.length] ;
        System.arraycopy(this.perUseCostPeriod, 0, retVal, 0, this.perUseCostPeriod.length);
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
    public double getPerUseCostPeriod(int idx) {
        if (this.perUseCostPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.perUseCostPeriod[idx];
    }

    public int getPerUseCostPeriodLength() {
        if (this.perUseCostPeriod == null) {
            return  0;
        }
        return this.perUseCostPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setPerUseCostPeriod(double[] values) {
        int len = values.length;
        this.perUseCostPeriod = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.perUseCostPeriod[i] = new Double(values[i]);
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
    public double setPerUseCostPeriod(int idx, double value) {
        return this.perUseCostPeriod[idx] = new Double(value);
    }

    public boolean isSetPerUseCostPeriod() {
        return ((this.perUseCostPeriod!= null)&&(this.perUseCostPeriod.length > 0));
    }

    public void unsetPerUseCostPeriod() {
        this.perUseCostPeriod = null;
    }

    /**
     * 
     * 
     * @return
     *     array of
     *     {@link Double }
     *     
     */
    public double[] getWeightPeriod() {
        if (this.weightPeriod == null) {
            return new double[ 0 ] ;
        }
        double[] retVal = new double[this.weightPeriod.length] ;
        System.arraycopy(this.weightPeriod, 0, retVal, 0, this.weightPeriod.length);
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
    public double getWeightPeriod(int idx) {
        if (this.weightPeriod == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.weightPeriod[idx];
    }

    public int getWeightPeriodLength() {
        if (this.weightPeriod == null) {
            return  0;
        }
        return this.weightPeriod.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Double }
     *     
     */
    public void setWeightPeriod(double[] values) {
        int len = values.length;
        this.weightPeriod = ((double[]) new double[len] );
        for (int i = 0; (i<len); i ++) {
            this.weightPeriod[i] = new Double(values[i]);
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
    public double setWeightPeriod(int idx, double value) {
        return this.weightPeriod[idx] = new Double(value);
    }

    public boolean isSetWeightPeriod() {
        return ((this.weightPeriod!= null)&&(this.weightPeriod.length > 0));
    }

    public void unsetWeightPeriod() {
        this.weightPeriod = null;
    }

    /**
     * Gets the value of the weight property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getWeight() {
        if (weight == null) {
            return  1.0D;
        } else {
            return weight;
        }
    }

    /**
     * Sets the value of the weight property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setWeight(double value) {
        this.weight = value;
    }

    public boolean isSetWeight() {
        return (this.weight!= null);
    }

    public void unsetWeight() {
        this.weight = null;
    }

    /**
     * Gets the value of the maxAgents property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getMaxAgents() {
        return maxAgents;
    }

    /**
     * Sets the value of the maxAgents property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxAgents(int value) {
        this.maxAgents = value;
    }

    public boolean isSetMaxAgents() {
        return (this.maxAgents!= null);
    }

    public void unsetMaxAgents() {
        this.maxAgents = null;
    }

    /**
     * Gets the value of the minAgents property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getMinAgents() {
        if (minAgents == null) {
            return  0;
        } else {
            return minAgents;
        }
    }

    /**
     * Sets the value of the minAgents property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMinAgents(int value) {
        this.minAgents = value;
    }

    public boolean isSetMinAgents() {
        return (this.minAgents!= null);
    }

    public void unsetMinAgents() {
        this.minAgents = null;
    }

    /**
     * Gets the value of the idleCost property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getIdleCost() {
        if (idleCost == null) {
            return  0.0D;
        } else {
            return idleCost;
        }
    }

    /**
     * Sets the value of the idleCost property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setIdleCost(double value) {
        this.idleCost = value;
    }

    public boolean isSetIdleCost() {
        return (this.idleCost!= null);
    }

    public void unsetIdleCost() {
        this.idleCost = null;
    }

    /**
     * Gets the value of the busyCost property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getBusyCost() {
        if (busyCost == null) {
            return  0.0D;
        } else {
            return busyCost;
        }
    }

    /**
     * Sets the value of the busyCost property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setBusyCost(double value) {
        this.busyCost = value;
    }

    public boolean isSetBusyCost() {
        return (this.busyCost!= null);
    }

    public void unsetBusyCost() {
        this.busyCost = null;
    }

    /**
     * Gets the value of the perUseCost property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getPerUseCost() {
        if (perUseCost == null) {
            return  0.0D;
        } else {
            return perUseCost;
        }
    }

    /**
     * Sets the value of the perUseCost property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPerUseCost(double value) {
        this.perUseCost = value;
    }

    public boolean isSetPerUseCost() {
        return (this.perUseCost!= null);
    }

    public void unsetPerUseCost() {
        this.perUseCost = null;
    }

    /**
     * Gets the value of the efficiency property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getEfficiency() {
        if (efficiency == null) {
            return  1.0D;
        } else {
            return efficiency;
        }
    }

    /**
     * Sets the value of the efficiency property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setEfficiency(double value) {
        this.efficiency = value;
    }

    public boolean isSetEfficiency() {
        return (this.efficiency!= null);
    }

    public void unsetEfficiency() {
        this.efficiency = null;
    }

    /**
     * Gets the value of the skillCount property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getSkillCount() {
        return skillCount;
    }

    /**
     * Sets the value of the skillCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSkillCount(int value) {
        this.skillCount = value;
    }

    public boolean isSetSkillCount() {
        return (this.skillCount!= null);
    }

    public void unsetSkillCount() {
        this.skillCount = null;
    }

    /**
     * Gets the value of the detailed property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDetailed() {
        return detailed;
    }

    /**
     * Sets the value of the detailed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDetailed(boolean value) {
        this.detailed = value;
    }

    public boolean isSetDetailed() {
        return (this.detailed!= null);
    }

    public void unsetDetailed() {
        this.detailed = null;
    }

    /**
     * Gets the value of the convertSchedulesToStaffing property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isConvertSchedulesToStaffing() {
        if (convertSchedulesToStaffing == null) {
            return false;
        } else {
            return convertSchedulesToStaffing;
        }
    }

    /**
     * Sets the value of the convertSchedulesToStaffing property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setConvertSchedulesToStaffing(boolean value) {
        this.convertSchedulesToStaffing = value;
    }

    public boolean isSetConvertSchedulesToStaffing() {
        return (this.convertSchedulesToStaffing!= null);
    }

    public void unsetConvertSchedulesToStaffing() {
        this.convertSchedulesToStaffing = null;
    }

    /**
     * Gets the value of the agentsMult property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getAgentsMult() {
        if (agentsMult == null) {
            return  1.0D;
        } else {
            return agentsMult;
        }
    }

    /**
     * Sets the value of the agentsMult property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAgentsMult(double value) {
        this.agentsMult = value;
    }

    public boolean isSetAgentsMult() {
        return (this.agentsMult!= null);
    }

    public void unsetAgentsMult() {
        this.agentsMult = null;
    }

}