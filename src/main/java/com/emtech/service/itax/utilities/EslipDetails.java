package com.emtech.service.itax.utilities;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*@author Omukubwa Emukule*/

@XmlRootElement(name = "ESLIPDETAILS")
public class EslipDetails {
    private String taxCode;

    private String taxHead;

    private String taxComponent;

    private String amountPerTax;

    private String taxPeriod;

    public String getTaxCode() {
        return this.taxCode;
    }

    @XmlElement(name = "TaxCode")
    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getTaxHead() {
        return this.taxHead;
    }

    @XmlElement(name = "TaxHead")
    public void setTaxHead(String taxHead) {
        this.taxHead = taxHead;
    }

    public String getTaxComponent() {
        return this.taxComponent;
    }

    @XmlElement(name = "TaxComponent")
    public void setTaxComponent(String taxComponent) {
        this.taxComponent = taxComponent;
    }

    public String getAmountPerTax() {
        return this.amountPerTax;
    }

    @XmlElement(name = "AmountPerTax")
    public void setAmountPerTax(String amountPerTax) {
        this.amountPerTax = amountPerTax;
    }

    public String getTaxPeriod() {
        return this.taxPeriod;
    }

    @XmlElement(name = "TaxPeriod")
    public void setTaxPeriod(String taxPeriod) {
        this.taxPeriod = taxPeriod;
    }
}
