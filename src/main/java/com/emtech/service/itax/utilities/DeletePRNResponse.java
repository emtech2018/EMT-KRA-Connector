package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*@author Omukubwa Emukule*/

@XmlRootElement(name = "DELETE PRN")
public class DeletePRNResponse {
    private String response;
    public String getResponse() {
        return response;
    }

    @XmlElement(name = "Response")
    public void setResponse(String response) {
        this.response = response;
    }
}
