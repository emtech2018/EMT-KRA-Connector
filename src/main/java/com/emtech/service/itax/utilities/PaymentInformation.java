package com.emtech.service.itax.utilities;


import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*@author Omukubwa Emukule*/

@XmlRootElement(name = "CCRSPAYMENT")
public class PaymentInformation {
    private ArrayList<PaymentDetails> paymentDetails;

    public ArrayList<PaymentDetails> getPaymentDetails() {
        return this.paymentDetails;
    }

    @XmlElement(name = "PAYMENT")
    public void setPaymentDetails(ArrayList<PaymentDetails> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }
}