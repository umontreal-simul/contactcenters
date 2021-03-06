//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.25 at 06:40:43 PM SGT 
//


package umontreal.iro.lecuyer.xmlbind.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                   Represents the parameters for a database connection
 *                   established using JDBC.
 *                
 * 
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DBConnectionParams", propOrder = {
    "properties"
})
public class DBConnectionParams {

    protected PropertiesParams properties;
    @XmlAttribute(name = "jndiDataSourceName")
    protected String jndiDataSourceName;
    @XmlAttribute(name = "jdbcDriverClass")
    protected String jdbcDriverClass;
    @XmlAttribute(name = "jdbcURI")
    @XmlSchemaType(name = "anyURI")
    protected String jdbcURI;

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
     * Gets the value of the jndiDataSourceName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJndiDataSourceName() {
        return jndiDataSourceName;
    }

    /**
     * Sets the value of the jndiDataSourceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJndiDataSourceName(String value) {
        this.jndiDataSourceName = value;
    }

    public boolean isSetJndiDataSourceName() {
        return (this.jndiDataSourceName!= null);
    }

    /**
     * Gets the value of the jdbcDriverClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJdbcDriverClass() {
        return jdbcDriverClass;
    }

    /**
     * Sets the value of the jdbcDriverClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJdbcDriverClass(String value) {
        this.jdbcDriverClass = value;
    }

    public boolean isSetJdbcDriverClass() {
        return (this.jdbcDriverClass!= null);
    }

    /**
     * Gets the value of the jdbcURI property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJdbcURI() {
        return jdbcURI;
    }

    /**
     * Sets the value of the jdbcURI property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJdbcURI(String value) {
        this.jdbcURI = value;
    }

    public boolean isSetJdbcURI() {
        return (this.jdbcURI!= null);
    }

}
