
package com.emtech.service.itax.tcs.kra.pg.facade.impl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for acceptPaymentResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="acceptPaymentResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="eslipStatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AcceptResponse", propOrder = {
    "eslipStatus"
})
public class AcceptPaymentResponse {

    protected String eslipStatus;

    /**
     * Gets the value of the eslipStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEslipStatus() {
        return eslipStatus;
    }

    /**
     * Sets the value of the eslipStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEslipStatus(String value) {
        this.eslipStatus = value;
    }

}
