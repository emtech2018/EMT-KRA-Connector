package com.emtech.service.itax.utilities;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ESLIPHEADER")
public class EslipHeader {
    private String systemCode;

    private String eslipNumber;

    private String slipPaymentCode;

    private String paymentAdviceDate;

    private String taxpayerPin;

    private String taxpayerFullName;

    private String totalAmount;

    private String docRefNumber;

    private String currency;

    public String getSystemCode() {
        return this.systemCode;
    }

    @XmlElement(name = "SystemCode")
    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getEslipNumber() {
        return this.eslipNumber;
    }

    @XmlElement(name = "EslipNumber")
    public void setEslipNumber(String eslipNumber) {
        this.eslipNumber = eslipNumber;
    }

    public String getSlipPaymentCode() {
        return this.slipPaymentCode;
    }

    @XmlElement(name = "SlipPaymentCode")
    public void setSlipPaymentCode(String slipPaymentCode) {
        this.slipPaymentCode = slipPaymentCode;
    }

    public String getPaymentAdviceDate() {
        return this.paymentAdviceDate;
    }

    @XmlElement(name = "PaymentAdviceDate")
    public void setPaymentAdviceDate(String paymentAdviceDate) {
        this.paymentAdviceDate = paymentAdviceDate;
    }

    public String getTaxpayerPin() {
        return this.taxpayerPin;
    }

    @XmlElement(name = "TaxpayerPin")
    public void setTaxpayerPin(String taxpayerPin) {
        this.taxpayerPin = taxpayerPin;
    }

    public String getTaxpayerFullName() {
        return this.taxpayerFullName;
    }

    @XmlElement(name = "TaxpayerFullName")
    public void setTaxpayerFullName(String taxpayerFullName) {
        this.taxpayerFullName = taxpayerFullName;
    }

    public String getTotalAmount() {
        return this.totalAmount;
    }

    @XmlElement(name = "TotalAmount")
    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDocRefNumber() {
        return this.docRefNumber;
    }

    @XmlElement(name = "DocRefNumber")
    public void setDocRefNumber(String docRefNumber) {
        this.docRefNumber = docRefNumber;
    }

    public String getCurrency() {
        return this.currency;
    }

    @XmlElement(name = "Currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
