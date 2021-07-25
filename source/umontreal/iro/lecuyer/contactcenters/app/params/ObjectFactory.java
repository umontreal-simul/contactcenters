//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.contactcenters.app.params;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the umontreal.iro.lecuyer.contactcenters.app.params package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _RepSimParams_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "repSimParams");
    private final static QName _CtmcrepSimParams_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "ctmcrepSimParams");
    private final static QName _SimParams_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "simParams");
    private final static QName _ContactCenterEvalResults_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "contactCenterEvalResults");
    private final static QName _ContactCenterSimResults_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "contactCenterSimResults");
    private final static QName _BatchSimParams_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "batchSimParams");
    private final static QName _StratSimParams_QNAME = new QName("http://www.iro.umontreal.ca/lecuyer/contactcenters/app", "stratSimParams");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: umontreal.iro.lecuyer.contactcenters.app.params
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ObsMatrix }
     * 
     */
    public ObsMatrix createObsMatrix() {
        return new ObsMatrix();
    }

    /**
     * Create an instance of {@link ContactCenterEvalResultsParams }
     * 
     */
    public ContactCenterEvalResultsParams createContactCenterEvalResultsParams() {
        return new ContactCenterEvalResultsParams();
    }

    /**
     * Create an instance of {@link ContactCenterSimResultsParams }
     * 
     */
    public ContactCenterSimResultsParams createContactCenterSimResultsParams() {
        return new ContactCenterSimResultsParams();
    }

    /**
     * Create an instance of {@link StratSimParams }
     * 
     */
    public StratSimParams createStratSimParams() {
        return new StratSimParams();
    }

    /**
     * Create an instance of {@link BatchSimParams }
     * 
     */
    public BatchSimParams createBatchSimParams() {
        return new BatchSimParams();
    }

    /**
     * Create an instance of {@link RepSimParams }
     * 
     */
    public RepSimParams createRepSimParams() {
        return new RepSimParams();
    }

    /**
     * Create an instance of {@link CTMCRepSimParams }
     * 
     */
    public CTMCRepSimParams createCTMCRepSimParams() {
        return new CTMCRepSimParams();
    }

    /**
     * Create an instance of {@link PrintedStatParams }
     * 
     */
    public PrintedStatParams createPrintedStatParams() {
        return new PrintedStatParams();
    }

    /**
     * Create an instance of {@link PMMatrixInt }
     * 
     */
    public PMMatrixInt createPMMatrixInt() {
        return new PMMatrixInt();
    }

    /**
     * Create an instance of {@link SequentialSamplingParams }
     * 
     */
    public SequentialSamplingParams createSequentialSamplingParams() {
        return new SequentialSamplingParams();
    }

    /**
     * Create an instance of {@link PerformanceMeasureParams }
     * 
     */
    public PerformanceMeasureParams createPerformanceMeasureParams() {
        return new PerformanceMeasureParams();
    }

    /**
     * Create an instance of {@link ServiceLevelParams }
     * 
     */
    public ServiceLevelParams createServiceLevelParams() {
        return new ServiceLevelParams();
    }

    /**
     * Create an instance of {@link HistogramParams }
     * 
     */
    public HistogramParams createHistogramParams() {
        return new HistogramParams();
    }

    /**
     * Create an instance of {@link ControlVariableParams }
     * 
     */
    public ControlVariableParams createControlVariableParams() {
        return new ControlVariableParams();
    }

    /**
     * Create an instance of {@link ReportParams }
     * 
     */
    public ReportParams createReportParams() {
        return new ReportParams();
    }

    /**
     * Create an instance of {@link PMMatrix }
     * 
     */
    public PMMatrix createPMMatrix() {
        return new PMMatrix();
    }

    /**
     * Create an instance of {@link RandomStreamsParams }
     * 
     */
    public RandomStreamsParams createRandomStreamsParams() {
        return new RandomStreamsParams();
    }

    /**
     * Create an instance of {@link PropertyNameParam }
     * 
     */
    public PropertyNameParam createPropertyNameParam() {
        return new PropertyNameParam();
    }

    /**
     * Create an instance of {@link CallTraceParams }
     * 
     */
    public CallTraceParams createCallTraceParams() {
        return new CallTraceParams();
    }

    /**
     * Create an instance of {@link ObsMatrix.Obs }
     * 
     */
    public ObsMatrix.Obs createObsMatrixObs() {
        return new ObsMatrix.Obs();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RepSimParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "repSimParams", substitutionHeadNamespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", substitutionHeadName = "simParams")
    public JAXBElement<RepSimParams> createRepSimParams(RepSimParams value) {
        return new JAXBElement<RepSimParams>(_RepSimParams_QNAME, RepSimParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CTMCRepSimParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "ctmcrepSimParams", substitutionHeadNamespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", substitutionHeadName = "simParams")
    public JAXBElement<CTMCRepSimParams> createCtmcrepSimParams(CTMCRepSimParams value) {
        return new JAXBElement<CTMCRepSimParams>(_CtmcrepSimParams_QNAME, CTMCRepSimParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SimParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "simParams")
    public JAXBElement<SimParams> createSimParams(SimParams value) {
        return new JAXBElement<SimParams>(_SimParams_QNAME, SimParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ContactCenterEvalResultsParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "contactCenterEvalResults")
    public JAXBElement<ContactCenterEvalResultsParams> createContactCenterEvalResults(ContactCenterEvalResultsParams value) {
        return new JAXBElement<ContactCenterEvalResultsParams>(_ContactCenterEvalResults_QNAME, ContactCenterEvalResultsParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ContactCenterSimResultsParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "contactCenterSimResults", substitutionHeadNamespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", substitutionHeadName = "contactCenterEvalResults")
    public JAXBElement<ContactCenterSimResultsParams> createContactCenterSimResults(ContactCenterSimResultsParams value) {
        return new JAXBElement<ContactCenterSimResultsParams>(_ContactCenterSimResults_QNAME, ContactCenterSimResultsParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BatchSimParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "batchSimParams", substitutionHeadNamespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", substitutionHeadName = "simParams")
    public JAXBElement<BatchSimParams> createBatchSimParams(BatchSimParams value) {
        return new JAXBElement<BatchSimParams>(_BatchSimParams_QNAME, BatchSimParams.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StratSimParams }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", name = "stratSimParams", substitutionHeadNamespace = "http://www.iro.umontreal.ca/lecuyer/contactcenters/app", substitutionHeadName = "simParams")
    public JAXBElement<StratSimParams> createStratSimParams(StratSimParams value) {
        return new JAXBElement<StratSimParams>(_StratSimParams_QNAME, StratSimParams.class, null, value);
    }

}
