package com.emtech.service.itax.utilities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "ESLIP")
public class ConsultEslipXMLToJava {
    private String hashCode;

    private ArrayList<Result> RESULT;

    private ArrayList<EslipHeader> ESLIPHEADER;

    private ArrayList<EslipDetails> ESLIPDETAILS;

    public ArrayList<Result> getRESULT() {
        return this.RESULT;
    }

    public void setRESULT(ArrayList<Result> rESULT) {
        this.RESULT = rESULT;
    }

    public ArrayList<EslipHeader> getESLIPHEADER() {
        return this.ESLIPHEADER;
    }

    public void setESLIPHEADER(ArrayList<EslipHeader> eSLIPHEADER) {
        this.ESLIPHEADER = eSLIPHEADER;
    }

    public ArrayList<EslipDetails> getESLIPDETAILS() {
        return this.ESLIPDETAILS;
    }

    public void setESLIPDETAILS(ArrayList<EslipDetails> eSLIPDETAILS) {
        this.ESLIPDETAILS = eSLIPDETAILS;
    }

    public String getHashCode() {
        return this.hashCode;
    }

    @XmlElement(name = "hashCode")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }
}
