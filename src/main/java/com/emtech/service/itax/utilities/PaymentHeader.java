package com.emtech.service.itax.utilities;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "", propOrder = {"systemCode", "branchCode", "bankTellerId", "bankTellerName", "paymentMode", "meansofPayment", "remitterId", "remitterName", "ESlipNumber", "slipPaymentCode", "paymentAdviceDate", "paymentReference", "taxpayerPIN", "taxpayerName", "totalAmount", "docRefNumber", "dateOfCollection", "currency", "cashAmount", "chequeAmount"})
@XmlRootElement(name = "PAYMENTHEADER")
public class PaymentHeader {
    private String SystemCode;
    private String BranchCode;
    private String BankTellerId;
    private String BankTellerName;
    private String PaymentMode;
    private String MeansofPayment;
    private String RemitterId;
    private String RemitterName;
    private String ESlipNumber;
    private String SlipPaymentCode;
    private String PaymentAdviceDate;
    private String PaymentReference;
    private String TaxpayerPIN;
    private String TaxpayerName;
    private String TotalAmount;
    private String DocRefNumber;
    private String DateOfCollection;
    private String Currency;
    private String CashAmount;
    private String ChequeAmount;


    public String getSystemCode() {
        return this.SystemCode;
    }

    @XmlElement(name = "SystemCode")
    public void setSystemCode(String systemCode) {
        this.SystemCode = systemCode;
    }

    public String getBranchCode() {
        return this.BranchCode;
    }

    @XmlElement(name = "BranchCode")
    public void setBranchCode(String branchCode) {
        this.BranchCode = branchCode;
    }

    public String getBankTellerId() {
        return this.BankTellerId;
    }

    @XmlElement(name = "BankTellerId")
    public void setBankTellerId(String bankTellerId) {
        this.BankTellerId = bankTellerId;
    }

    public String getBankTellerName() {
        return this.BankTellerName;
    }

    @XmlElement(name = "BankTellerName")
    public void setBankTellerName(String bankTellerName) {
        this.BankTellerName = bankTellerName;
    }

    public String getPaymentMode() {
        return this.PaymentMode;
    }

    @XmlElement(name = "PaymentMode")
    public void setPaymentMode(String paymentMode) {
        this.PaymentMode = paymentMode;
    }

    public String getMeansofPayment() {
        return this.MeansofPayment;
    }

    @XmlElement(name = "MeansOfPayment")
    public void setMeansofPayment(String meansofPayment) {
        this.MeansofPayment = meansofPayment;
    }

    public String getRemitterId() {
        return this.RemitterId;
    }

    @XmlElement(name = "RemitterId")
    public void setRemitterId(String remitterId) {
        this.RemitterId = remitterId;
    }

    public String getRemitterName() {
        return this.RemitterName;
    }

    @XmlElement(name = "RemitterName")
    public void setRemitterName(String remitterName) {
        this.RemitterName = remitterName;
    }

    public String getESlipNumber() {
        return this.ESlipNumber;
    }

    @XmlElement(name = "EslipNumber")
    public void setESlipNumber(String eSlipNumber) {
        this.ESlipNumber = eSlipNumber;
    }

    public String getSlipPaymentCode() {
        return this.SlipPaymentCode;
    }

    @XmlElement(name = "SlipPaymentCode")
    public void setSlipPaymentCode(String slipPaymentCode) {
        this.SlipPaymentCode = slipPaymentCode;
    }

    public String getPaymentAdviceDate() {
        return this.PaymentAdviceDate;
    }

    @XmlElement(name = "PaymentAdviceDate")
    public void setPaymentAdviceDate(String paymentAdviceDate) {
        this.PaymentAdviceDate = paymentAdviceDate;
    }

    public String getPaymentReference() {
        return this.PaymentReference;
    }

    @XmlElement(name = "PaymentReference")
    public void setPaymentReference(String paymentReference) {
        this.PaymentReference = paymentReference;
    }

    public String getTaxpayerPIN() {
        return this.TaxpayerPIN;
    }

    @XmlElement(name = "TaxpayerPin")
    public void setTaxpayerPIN(String taxpayerPIN) {
        this.TaxpayerPIN = taxpayerPIN;
    }

    public String getTaxpayerName() {
        return this.TaxpayerName;
    }

    @XmlElement(name = "TaxpayerFullName")
    public void setTaxpayerName(String taxpayerName) {
        this.TaxpayerName = taxpayerName;
    }

    public String getTotalAmount() {
        return this.TotalAmount;
    }

    @XmlElement(name = "TotalAmount")
    public void setTotalAmount(String totalAmount) {
        this.TotalAmount = totalAmount;
    }

    public String getDocRefNumber() {
        return this.DocRefNumber;
    }

    @XmlElement(name = "DocRefNumber")
    public void setDocRefNumber(String docRefNumber) {
        this.DocRefNumber = docRefNumber;
    }

    public String getDateOfCollection() {
        return this.DateOfCollection;
    }

    @XmlElement(name = "DateOfCollection")
    public void setDateOfCollection(String dateOfCollection) {
        this.DateOfCollection = dateOfCollection;
    }

    public String getCurrency() {
        return this.Currency;
    }

    @XmlElement(name = "Currency")
    public void setCurrency(String currency) {
        this.Currency = currency;
    }

    public String getCashAmount() {
        return this.CashAmount;
    }

    @XmlElement(name = "CashAmount")
    public void setCashAmount(String cashAmount) {
        this.CashAmount = cashAmount;
    }

    public String getChequeAmount() {
        return this.ChequeAmount;
    }

    @XmlElement(name = "ChequesAmount")
    public void setChequeAmount(String chequeAmount) {
        this.ChequeAmount = chequeAmount;
    }
}
