package com.emtech.service.itax.utilities;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "PAYMENTCHEQUE")
@XmlType(name = "", propOrder = {"bankOfCheque", "branchOfCheque", "chequeNumber", "chequeDate", "chequeAmount", "chequeAccount", "chequeType"})
public class ChequeDetails {
    private String BankOfCheque;

    private String BranchOfCheque;

    private String ChequeNumber;

    private String ChequeDate;

    private String ChequeAmount;

    private String ChequeAccount;

    private String ChequeType;

    public String getBankOfCheque() {
        return this.BankOfCheque;
    }

    @XmlElement(name = "BankOfCheque")
    public void setBankOfCheque(String bankOfCheque) {
        this.BankOfCheque = bankOfCheque;
    }

    public String getBranchOfCheque() {
        return this.BranchOfCheque;
    }

    @XmlElement(name = "BranchOfCheque")
    public void setBranchOfCheque(String branchOfCheque) {
        this.BranchOfCheque = branchOfCheque;
    }

    public String getChequeNumber() {
        return this.ChequeNumber;
    }

    @XmlElement(name = "ChequeNumber")
    public void setChequeNumber(String chequeNumber) {
        this.ChequeNumber = chequeNumber;
    }

    public String getChequeDate() {
        return this.ChequeDate;
    }

    @XmlElement(name = "ChequeDate")
    public void setChequeDate(String chequeDate) {
        this.ChequeDate = chequeDate;
    }

    public String getChequeAmount() {
        return this.ChequeAmount;
    }

    @XmlElement(name = "ChequeAmount")
    public void setChequeAmount(String chequeAmount) {
        this.ChequeAmount = chequeAmount;
    }

    public String getChequeAccount() {
        return this.ChequeAccount;
    }

    @XmlElement(name = "ChequeAccount")
    public void setChequeAccount(String chequeAccount) {
        this.ChequeAccount = chequeAccount;
    }

    public String getChequeType() {
        return this.ChequeType;
    }

    public void setChequeType(String chequeType) {
        this.ChequeType = chequeType;
    }
}
