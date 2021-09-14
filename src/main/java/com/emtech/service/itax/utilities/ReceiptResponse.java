package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "TAX RECEIPT")
public class ReceiptResponse {
    private String response;

    public String getResponse() {
        return response;
    }

    @XmlElement(name = "Response")
    public void setResponse(String response) {
        this.response = response;
    }
}
