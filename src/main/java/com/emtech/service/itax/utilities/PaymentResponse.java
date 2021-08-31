package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PAYMENTS")
public class PaymentResponse {
    private String PaymentNumber;
    private String ResponseCode;
    private String Status;
    private String Message;

    public String getPaymentNumber() {
        return this.PaymentNumber;
    }

    @XmlElement(name = "PaymentNumber")
    public void setPaymentNumber(String paymentNumber) {
        this.PaymentNumber = paymentNumber;
    }

    public String getResponseCode() {
        return this.ResponseCode;
    }

    @XmlElement(name = "ResponseCode")
    public void setResponseCode(String responseCode) {
        this.ResponseCode = responseCode;
    }

    public String getStatus() {
        return this.Status;
    }

    @XmlElement(name = "Status")
    public void setStatus(String status) {
        this.Status = status;
    }

    public String getMessage() {
        return this.Message;
    }

    @XmlElement(name = "Message")
    public void setMessage(String message) {
        this.Message = message;
    }
}
