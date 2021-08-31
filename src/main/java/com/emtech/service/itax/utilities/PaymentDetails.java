package com.emtech.service.itax.utilities;


import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "PAYMENT")
@XmlType(name = "", propOrder = {"paymentHeader", "chequeDetails", "hashCode"})
public class PaymentDetails {
    private String hashCode;
    private ArrayList<PaymentHeader> paymentHeader;
    private ArrayList<ChequeDetails> chequeDetails;

    public String getHashCode() {
        return this.hashCode;
    }

    @XmlElement(name = "hashCode")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public ArrayList<PaymentHeader> getPaymentHeader() {
        return this.paymentHeader;
    }

    @XmlElement(name = "PAYMENTHEADER")
    public void setPaymentHeader(ArrayList<PaymentHeader> paymentHeader) {
        this.paymentHeader = paymentHeader;
    }

    public ArrayList<ChequeDetails> getChequeDetails() {
        return this.chequeDetails;
    }

    @XmlElement(name = "PAYMENTCHEQUE", required = false)
    public void setChequeDetails(ArrayList<ChequeDetails> chequeDetails) {
        this.chequeDetails = chequeDetails;
    }
}
