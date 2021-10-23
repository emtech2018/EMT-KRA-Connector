package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/*@author Omukubwa Emukule*/

@XmlRootElement(name = "RESPONSE")
public class PostPaymentXMLToJava {
    private ArrayList<PaymentResponse> RESPONSE;

    public ArrayList<PaymentResponse> getRESPONSE() {
        return RESPONSE;
    }

    @XmlElement(name = "PAYMENTS")
    public void setRESPONSE(ArrayList<PaymentResponse> RESPONSE) {
        this.RESPONSE = RESPONSE;
    }
}
