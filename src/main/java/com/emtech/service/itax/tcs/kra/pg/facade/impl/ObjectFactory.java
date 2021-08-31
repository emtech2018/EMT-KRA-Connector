
package com.emtech.service.itax.tcs.kra.pg.facade.impl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.tcs.kra.pg.facade.impl package. 
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
@XmlType(name = "obj")
public class ObjectFactory {

    private final static QName _AcceptPaymentResponse_QNAME = new QName("http://impl.facade.pg.kra.tcs.com/", "acceptPaymentResponse");
    private final static QName _CheckEslip_QNAME = new QName("http://impl.facade.pg.kra.tcs.com/", "checkEslip");
    private final static QName _AcceptPayment_QNAME = new QName("http://impl.facade.pg.kra.tcs.com/", "acceptPayment");
    private final static QName _CheckEslipResponse_QNAME = new QName("http://impl.facade.pg.kra.tcs.com/", "checkEslipResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.tcs.kra.pg.facade.impl
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AcceptPaymentResponse }
     * 
     */
    public AcceptPaymentResponse createAcceptPaymentResponse() {
        return new AcceptPaymentResponse();
    }

    /**
     * Create an instance of {@link CheckEslip }
     * 
     */
    public CheckEslip createCheckEslip() {
        return new CheckEslip();
    }

    /**
     * Create an instance of {@link AcceptPayment }
     * 
     */
    public AcceptPayment createAcceptPayment() {
        return new AcceptPayment();
    }

    /**
     * Create an instance of {@link CheckEslipResponse }
     * 
     */
    public CheckEslipResponse createCheckEslipResponse() {
        return new CheckEslipResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AcceptPaymentResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.facade.pg.kra.tcs.com/", name = "acceptPaymentResponse")
    public JAXBElement<AcceptPaymentResponse> createAcceptPaymentResponse(AcceptPaymentResponse value) {
        return new JAXBElement<AcceptPaymentResponse>(_AcceptPaymentResponse_QNAME, AcceptPaymentResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckEslip }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.facade.pg.kra.tcs.com/", name = "checkEslip")
    public JAXBElement<CheckEslip> createCheckEslip(CheckEslip value) {
        return new JAXBElement<CheckEslip>(_CheckEslip_QNAME, CheckEslip.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AcceptPayment }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.facade.pg.kra.tcs.com/", name = "acceptPayment")
    public JAXBElement<AcceptPayment> createAcceptPayment(AcceptPayment value) {
        return new JAXBElement<AcceptPayment>(_AcceptPayment_QNAME, AcceptPayment.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckEslipResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://impl.facade.pg.kra.tcs.com/", name = "checkEslipResponse")
    public JAXBElement<CheckEslipResponse> createCheckEslipResponse(CheckEslipResponse value) {
        return new JAXBElement<CheckEslipResponse>(_CheckEslipResponse_QNAME, CheckEslipResponse.class, null, value);
    }

}
