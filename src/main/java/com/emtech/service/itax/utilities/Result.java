package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "RESULT")
@XmlType(name = "Result", propOrder = {"status", "remarks"})
public class Result {
    private String status;
    private String remarks;

    //Getters and setters
    public String getStatus() {
        return this.status;
    }

    @XmlElement(name = "Status")
    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return this.remarks;
    }

    @XmlElement(name = "Remarks")
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}