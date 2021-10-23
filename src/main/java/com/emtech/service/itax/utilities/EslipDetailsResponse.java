package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*@author Omukubwa Emukule*/

@XmlRootElement(name = "PRN DETAILS")
public class EslipDetailsResponse {
    private String eslipnumber;
    private String taxpayername;
    private String taxpayerpin;
    private String amount;
    private String status;
    private String remarks;
    private String paymentadvicedate;
    private String taxcomponent;
    private String taxcode;

    public String getEslipnumber() {
        return eslipnumber;
    }

    @XmlElement(name = "eslipnumber")
    public void setEslipnumber(String eslipnumber) {
        this.eslipnumber = eslipnumber;
    }

    public String getTaxpayername() {
        return taxpayername;
    }

    @XmlElement(name = "taxpayername")
    public void setTaxpayername(String taxpayername) {
        this.taxpayername = taxpayername;
    }

    public String getTaxpayerpin() {
        return taxpayerpin;
    }

    @XmlElement(name = "taxpayerpin")
    public void setTaxpayerpin(String taxpayerpin) {
        this.taxpayerpin = taxpayerpin;
    }

    public String getAmount() {
        return amount;
    }

    @XmlElement(name = "amount")
    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    @XmlElement(name = "status")
    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    @XmlElement(name = "remarks")
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getPaymentadvicedate() {
        return paymentadvicedate;
    }

    @XmlElement(name = "paymentadvicedate")
    public void setPaymentadvicedate(String paymentadvicedate) {
        this.paymentadvicedate = paymentadvicedate;
    }

    public String getTaxcomponent() {
        return taxcomponent;
    }

    @XmlElement(name = "taxcomponent")
    public void setTaxcomponent(String taxcomponent) {
        this.taxcomponent = taxcomponent;
    }

    public String getTaxcode() {
        return taxcode;
    }

    @XmlElement(name = "taxcode")
    public void setTaxcode(String taxcode) {
        this.taxcode = taxcode;
    }
}
