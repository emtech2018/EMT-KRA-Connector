
package com.emtech.service.itax.tcs.kra.pg.facade.impl;


import com.emtech.service.itax.utilities.EslipDetails;
import com.emtech.service.itax.utilities.EslipHeader;
import com.emtech.service.itax.utilities.Result;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

@XmlRootElement(name = "ESLIP")
@XmlType(name="EslipResponse")
public class CheckEslipResponse {
    private ArrayList<Result> resultArrayList;
    private ArrayList<EslipHeader> eslipHeaderArrayList;
    private ArrayList<EslipDetails> eslipDetailsArrayList;
    private String hashCode;

    public String getHashCode() {
        return hashCode;
    }

    @XmlElement(name = "HASHCODE")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public ArrayList<Result> getResultArrayList() {
        return resultArrayList;
    }

    @XmlElement(name = "RESULT")
    public void setResultArrayList(ArrayList<Result> resultArrayList) {
        this.resultArrayList = resultArrayList;
    }

    public ArrayList<EslipHeader> getEslipHeaderArrayList() {
        return eslipHeaderArrayList;
    }

    @XmlElement(name = "ESLIPHEADER")
    public void setEslipHeaderArrayList(ArrayList<EslipHeader> eslipHeaderArrayList) {
        this.eslipHeaderArrayList = eslipHeaderArrayList;
    }

    public ArrayList<EslipDetails> getEslipDetailsArrayList() {
        return eslipDetailsArrayList;
    }

    @XmlElement(name = "ESLIPDETAILS")
    public void setEslipDetailsArrayList(ArrayList<EslipDetails> eslipDetailsArrayList) {
        this.eslipDetailsArrayList = eslipDetailsArrayList;
    }
}
