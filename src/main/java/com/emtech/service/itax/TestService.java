package com.emtech.service.itax;

//Class that will handle all requests and giving responses without the need for hitting KRA
//It will be giving responses for consulting and posting of payment

import com.emtech.service.Numbers2Words;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.utilities.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TestService {
    //Instance of the Configuration Classes
    Configurations cn = new Configurations();
    //Log In ID
    private String LOGINID = cn.getProperties().getProperty("itax.username");
    //Password
    private String PASSWORD = cn.getProperties().getProperty("itax.password");
    //Bank Code
    private String BANKCODE = cn.getProperties().getProperty("itax.bankcode");
    //System Code
    private String SYSTEMCODE = cn.getProperties().getProperty("itax.systemcode");
    //Payment Mode
    private String PAYMENTMODE = cn.getProperties().getProperty("itax.mofp1");
    //Currency
    private String CURRENCY = cn.getProperties().getProperty("itax.currency");
    //Remitter Id
    private String REMiTTERID = cn.getProperties().getProperty("itax.remitterid");
    //Remitter Name
    private String REMITTERNAME = cn.getProperties().getProperty("itax.remittername");
    //Teller Name
    private String tellername = cn.getProperties().getProperty("itax.tellername");
    //Bank Branch
    private String bankbranch = cn.getProperties().getProperty("itax.bankbranch");
    //Encryption Key
    private String key = cn.getProperties().getProperty("enc.key");
    //Encryption Init Vector
    private String initVector =  cn.getProperties().getProperty("enc.initVector");
    //Database Connection Class
    DatabaseMethods db = new DatabaseMethods();
    //Common Utilities Class
    CommonUtils cu = new CommonUtils();
    //LOGGING
    private static final Logger logger= LoggerFactory.getLogger(PaymentService.class);
    //Random Number (Trace No)
    int min = 10000;
    int max = 999999;
    Random rn = new Random();
    int traceno = rn.nextInt(max - min + 1) + min;
    //Receipts Folder
    String folder = cn.getProperties().getProperty("itax.folder");

    //For testing the configuration file
    public String test()
    {
        String a1 = cn.getProperties().getProperty("itax.input");
        String a2 = cn.getProperties().getProperty("itax.output");
        String a3 = cn.getProperties().getProperty("itax.password");
        String a4 = cn.getProperties().getProperty("itax.username");
        String a5 =a1+"\n"+a2+"\n"+a3+"\n"+a4;
        return a5;
    }

    //CONSULTING AN E-SLIP
    public CheckEslipResponse consultEslip(String prn)  throws IOException, JAXBException {
        String responseXML = "";
        //Update Query For status VALID
        String update_valid_query = cn.getProperties().getProperty("sql.update.valid.status");
        //Update Query for other status
        String update_query = cn.getProperties().getProperty("sql.update.other.status");
        //Select Query
        String select_query = cn.getProperties().getProperty("sql.select.query");
        //Select query for tax-code (checking duplicates)
        String select_query_taxcode = cn.getProperties().getProperty("sql.select.query.taxcode");
        CheckEslipResponse response = new CheckEslipResponse();
        //Disable Certificate Checking
        cu.disableVerification();
        logger.info("STARTING CONSULTING PRN NUMBER "+ prn);
        //----START OF DATABASE INSERT ----\\
        //SAVE DETAILS TO BE SENT TO KRA FOR CONSULTING IN THE DATABASE
        logger.info("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: STARTING TO SAVE E-SLIP DATA");
        String sendsql = cn.getProperties().getProperty("sql.insert.query");
        //Select query for checking delete flag
        //delete flag
        String selectflag = cn.getProperties().getProperty("sql.query.select.deleteflag").trim();
        String deleteflag = DatabaseMethods.selectValues(selectflag, 1, 1, prn);
        //Checking if a PRN has been deleted (Delete Flag = Y)
        if(deleteflag.equalsIgnoreCase("Y"))
        {
            logger.info("CONSULT E-SLIP :: THIS E-SLIP WAS DELETED!");
            //Customizing the response
            //Building the results array list
            //Arraylist of results
            Result rst = new Result();
            rst.setStatus("DELETED");
            rst.setRemarks("CONSULT E-SLIP :: THIS E-SLIP WAS DELETED!");
            ArrayList<Result> resultArrayList = new ArrayList<>();
            resultArrayList.add(rst);
            response.setHashCode(null);
            response.setEslipDetailsArrayList(null);
            response.setEslipHeaderArrayList(null);
            response.setResultArrayList(resultArrayList);
        }
        //Continue with consulting if the E-Slip is not deleted
        else {
            //Check if the trace no is already in the database
            if (DatabaseMethods.findDuplicates(select_query_taxcode, 1, String.valueOf(traceno))) {
                traceno = rn.nextInt(max - min + 1) + min;
            }
            String senddata = "" + traceno + "," + prn + ",N/A" + ",N/A" + ",N/A" + ",N/A" + ",N/A" + ",N/A" + ",N/A" + ",N/A" + ",0000" + ",N/A" + ",N/A" + ",0000" + ",N/A" + ",N/A" + ",0000" + "," + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()) + "," + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()) + "," + "N" + "," + "CS";
            //Check for duplicate values
            if (DatabaseMethods.findDuplicates(select_query, 1, prn)) {
                System.out.println("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: CHECKING DUPLICATES :: RESULTS :: Found this e-slip number in the db");
            } else {
                int insert = DatabaseMethods.DB(sendsql, 21, senddata);
                logger.info("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: DATA INSERTED :: " + senddata);
                logger.info("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + insert);
            }

            //----END OF DATABASE INSERT ----\\
            //Consulting
            logger.info("Consulting the E-SLIP No " + prn);

            //Response XML for status VALID
            responseXML = "<ESLIP>\n" +
                    "<RESULT>\n" +
                    "<Status>VALID</Status>\n" +
                    "<Remarks>REQUESTED/CONSULTED E SLIP NUMBER (PRN) EXISTS IN THE SYSTEM</Remarks>\n" +
                    "</RESULT>\n" +
                    "<hashCode>526326a00c125f064c2fc7bcd3119f2a4b7b0e7bd3006651a32a766e995b2d71</hashCode>\n" +
                    "<ESLIPHEADER>\n" +
                    "<SystemCode>PG</SystemCode>\n" +
                    "<EslipNumber>" + prn + "</EslipNumber>\n" +
                    "<SlipPaymentCode></SlipPaymentCode>\n" +
                    "<PaymentAdviceDate>2021-07-09T16:43:06</PaymentAdviceDate>\n" +
                    "<TaxpayerPin>P000598480M</TaxpayerPin>\n" +
                    "<TaxpayerFullName>Siginon Group Limited</TaxpayerFullName>\n" +
                    "<TotalAmount>5493</TotalAmount>\n" +
                    "<DocRefNumber></DocRefNumber>\n" +
                    "<Currency>KES</Currency>\n" +
                    "</ESLIPHEADER>\n" +
                    "<ESLIPDETAILS>\n" +
                    "<TaxCode>1506</TaxCode>\n" +
                    "<TaxHead></TaxHead>\n" +
                    "<TaxComponent>Licence Application Fee       </TaxComponent>\n" +
                    "<AmountPerTax>5493</AmountPerTax>\n" +
                    "<TaxPeriod>2021-07-09</TaxPeriod>\n" +
                    "</ESLIPDETAILS>\n" +
                    "</ESLIP>";
        /*
        //Response XML for status UTILIZED
        responseXML = "<ESLIP>\n" +
                "<RESULT>\n" +
                "<Status>UTILIZED</Status>\n" +
                "<Remarks>REQUESTED/CONSULTED E SLIP NUMBER (PRN) HAS BEEN TRANSACTED</Remarks>\n" +
                "</RESULT>\n" +
                "</ESLIP>";

        //Response XML for status INVALID
        responseXML = "<ESLIP>\n" +
                "<RESULT>\n" +
                "<Status>INVALID</Status>\n" +
                "<Remarks>REQUESTED/CONSULTED E SLIP NUMBER (PRN) DOES NOT EXISTS IN THE SYSTEM</Remarks>\n" +
                "</RESULT>\n" +
                "</ESLIP>";

        //Response XML for status ERROR
        responseXML = "<ESLIP>\n" +
                "<RESULT>\n" +
                "<Status>ERROR</Status>\n" +
                "<Remarks>EMPTY PRN,ENTER A PRN NUMBER (VALID LENGTH = 16)</Remarks>\n" +
                "</RESULT>\n" +
                "</ESLIP>";

        //Response XML for status EXPIRED
        responseXML = "<ESLIP>\n" +
                "<RESULT>\n" +
                "<Status>EXPIRED</Status>\n" +
                "<Remarks>THE REQUESTED PRN IS EXPIRED. REQUEST YOU TO PLEASE ASK TAXPAYER TO REGISTER THE PAYMENT AGAIN ON SYSTEM</Remarks>\n" +
                "</RESULT>\n" +
                "</ESLIP>";
         */
            logger.info("Unmarshalling the XML Response Now " + responseXML);
            //response.setPaymentEslip(responseXML);
            if (responseXML != null) {
                JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{ConsultEslipXMLToJava.class});
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                StringReader XmlreaderObj = new StringReader(responseXML);
                ConsultEslipXMLToJava XmltoJavaObj = (ConsultEslipXMLToJava) jaxbUnmarshaller.unmarshal(XmlreaderObj);
                //Results
                String responseStatus = ((Result) XmltoJavaObj.getRESULT().get(0)).getStatus();
                String responseHash = XmltoJavaObj.getHashCode();
                String responseRemaks = ((Result) XmltoJavaObj.getRESULT().get(0)).getRemarks();

                //Arraylist of results
                Result rst = new Result();
                rst.setStatus(responseStatus);
                rst.setRemarks(responseRemaks);
                ArrayList<Result> resultArrayList = new ArrayList<>();
                resultArrayList.add(rst);
                System.out.println("RESULTS :: " + responseXML);


                //Check the response status of the consulted PRN
                //Condition 1 : VALID (Prn exists in the system and it is not processed)
                if (responseStatus.equalsIgnoreCase("VALID")) {
                    //E-Slip Header
                    String taxPayerpin = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getTaxpayerPin();
                    String systemCode = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getSystemCode();
                    String eslipNumber = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getEslipNumber();
                    String taxpayername = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getTaxpayerFullName();
                    String totalamount = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getTotalAmount();
                    String currency = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getCurrency();
                    String docrefno = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getDocRefNumber();
                    String paymentadvicedate = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getPaymentAdviceDate();
                    String slippaymentcode = ((EslipHeader) XmltoJavaObj.getESLIPHEADER().get(0)).getSlipPaymentCode();
                    //E-Slip Details
                    String taxcode = ((EslipDetails) XmltoJavaObj.getESLIPDETAILS().get(0)).getTaxCode();
                    String taxhead = ((EslipDetails) XmltoJavaObj.getESLIPDETAILS().get(0)).getTaxHead();
                    String taxcomponent = ((EslipDetails) XmltoJavaObj.getESLIPDETAILS().get(0)).getTaxComponent();
                    String amountpertax = ((EslipDetails) XmltoJavaObj.getESLIPDETAILS().get(0)).getAmountPerTax();
                    String taxperiod = ((EslipDetails) XmltoJavaObj.getESLIPDETAILS().get(0)).getTaxPeriod();

                    //Handling Null Values
                    if (slippaymentcode.equalsIgnoreCase("")) {
                        slippaymentcode = "N/A";
                    }
                    if (taxhead.equalsIgnoreCase("")) {
                        taxhead = "N/A";
                    }
                    if (docrefno.equalsIgnoreCase("")) {
                        docrefno = "N/A";
                    }
                    //E-slip Header Arraylist
                    EslipHeader eh = new EslipHeader();
                    eh.setSystemCode(systemCode);
                    eh.setCurrency(currency);
                    eh.setSlipPaymentCode(slippaymentcode);
                    eh.setPaymentAdviceDate(paymentadvicedate);
                    eh.setEslipNumber(eslipNumber);
                    eh.setTaxpayerFullName(taxpayername);
                    eh.setTaxpayerPin(taxPayerpin);
                    eh.setTotalAmount(totalamount);
                    eh.setDocRefNumber(docrefno);

                    ArrayList<EslipHeader> eslipHeaderArrayList = new ArrayList<>();
                    eslipHeaderArrayList.add(eh);

                    //ArrayList of E-Slip Details
                    EslipDetails ed = new EslipDetails();
                    ed.setAmountPerTax(amountpertax);
                    ed.setTaxCode(taxcode);
                    ed.setTaxHead(taxhead);
                    ed.setTaxPeriod(taxperiod);
                    ed.setTaxComponent(taxcomponent);
                    ArrayList<EslipDetails> eslipDetailsArrayList = new ArrayList<>();
                    eslipDetailsArrayList.add(ed);

                    response.setHashCode(responseHash);
                    response.setEslipDetailsArrayList(eslipDetailsArrayList);
                    response.setEslipHeaderArrayList(eslipHeaderArrayList);
                    response.setResultArrayList(resultArrayList);

                    logger.info("VALID : PRN Number is valid.");
                    System.out.println("VALID : PRN Number is valid.");
                    //Eslip Consultation results for VALID PRN
                    System.out.println("-------------------------------------------------------");
                    System.out.println("CONSULTATION FOR E-SLIP NO : " + eslipNumber);
                    System.out.println("-------------------------------------------------------");
                    //Converting the Response XML to a JSON Object
                    cu.xmlToJSON(responseXML);

                    System.out.println("-------------------------------------------------------");
                    System.out.println("--END--");
                    System.out.println("-------------------------------------------------------");

                    //----START OF DATABASE UPDATE ----\\
                    //Save PRN details in the database
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: STARTING TO SAVE E-SLIP RESULTS FOR STATUS VALID");
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    String data = "" + responseStatus + "," + responseRemaks + "," + responseHash + "," + systemCode + "," + slippaymentcode + "," + paymentadvicedate + "," + taxPayerpin + "," + taxpayername + "," + totalamount + "," + docrefno + "," + currency + "," + taxcode + "," + taxhead + "," + taxcomponent + "," + amountpertax + "," + taxperiod + "," + timeStamp + "," + "N" + "," + "CR" + "," + prn;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                        update = DatabaseMethods.DB(update_valid_query, 20, data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                    } else {
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                    }
                    //----END OF DATABASE UPDATE ----\\
                }

                //Condition 2 : INVALID (Prn Number length is less than 16)
                else if (responseStatus.equalsIgnoreCase("INVALID")) {
                    logger.info("INVALID : The length of the PRN Number Passed is Less that 16.");
                    System.out.println("INVALID : The length of the PRN Number Passed is Less that 16.");
                    //E-slip Consultation results for INVALID PRN
                    System.out.println("-------------------------------------------------------");
                    System.out.println("CONSULTATION FOR E-SLIP NO : " + prn);
                    System.out.println("-------------------------------------------------------");
                    cu.xmlToJSON(responseXML);
                    response.setHashCode(responseHash);
                    response.setEslipDetailsArrayList(null);
                    response.setEslipHeaderArrayList(null);
                    response.setResultArrayList(resultArrayList);
                    System.out.println("-------------------------------------------------------");
                    System.out.println("--END--");
                    System.out.println("-------------------------------------------------------");
                    //----START OF DATABASE UPDATE ----\\
                    //Save PRN details in the database
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: STARTING TO SAVE E-SLIP RESULTS FOR STATUS INVALID");
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    String data = "" + responseStatus + "," + responseRemaks + "," + responseHash + "," + timeStamp + "," + "N" + "," + "CR" + "," + prn;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                        update = DatabaseMethods.DB(update_query, 7, data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                    } else {
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                    }
                    //----END OF DATABASE UPDATE ----\\
                }

                //Condition 3 : ERROR (No Prn Number is passed as input (Null)
                else if (responseStatus.equalsIgnoreCase("ERROR")) {
                    logger.info("ERROR : Empty PRN Number");
                    System.out.println("ERROR : Empty PRN Number");
                    //E-slip Consultation results for ERROR PRN
                    System.out.println("-------------------------------------------------------");
                    System.out.println("CONSULTATION FOR E-SLIP NO : " + prn);
                    System.out.println("-------------------------------------------------------");
                    //Result
                    cu.xmlToJSON(responseXML);
                    response.setHashCode(responseHash);
                    response.setEslipDetailsArrayList(null);
                    response.setEslipHeaderArrayList(null);
                    response.setResultArrayList(resultArrayList);
                    System.out.println("-------------------------------------------------------");
                    System.out.println("--END--");
                    System.out.println("-------------------------------------------------------");
                    //----START OF DATABASE UPDATE ----\\
                    //Save PRN details in the database
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: STARTING TO SAVE E-SLIP RESULTS FOR STATUS ERROR");
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    String data = "" + responseStatus + "," + responseRemaks + "," + responseHash + "," + timeStamp + "," + "N" + "," + "CR" + "," + prn;
                    ;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                        update = DatabaseMethods.DB(update_query, 7, data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                    } else {
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                    }
                    //----END OF DATABASE UPDATE ----\\
                }

                //Condition 4 : UTILIZED (Prn Number exists in the database but it has been processed
                else if (responseStatus.equalsIgnoreCase("UTILIZED")) {
                    logger.info("UTILIZED : Utilized PRN Number");
                    System.out.print("UTILIZED : Utilized PRN Number");
                    //E-slip Consultation results for UTILIZED PRN
                    System.out.println("-------------------------------------------------------");
                    System.out.println("CONSULTATION FOR E-SLIP NO : " + prn);
                    System.out.println("-------------------------------------------------------");
                    //Result
                    cu.xmlToJSON(responseXML);
                    response.setHashCode(responseHash);
                    response.setEslipDetailsArrayList(null);
                    response.setEslipHeaderArrayList(null);
                    response.setResultArrayList(resultArrayList);
                    System.out.println("-------------------------------------------------------");
                    System.out.println("--END--");
                    System.out.println("-------------------------------------------------------");
                    //----START OF DATABASE UPDATE ----\\
                    //Save PRN details in the database
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: STARTING TO SAVE E-SLIP RESULTS FOR STATUS UTILIZED");
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    String data = "" + responseStatus + "," + responseRemaks + "," + responseHash + "," + timeStamp + "," + "N" + "," + "CR" + "," + prn;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                        update = DatabaseMethods.DB(update_query, 7, data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                    } else {
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                    }
                    //----END OF DATABASE UPDATE ----\\
                }

                //Condition 5 : EXPIRED (Prn Number exists in the database but the requested/consulted E Slip Number (PRN) has expired.
                else if (responseStatus.equalsIgnoreCase("EXPIRED")) {
                    logger.info("EXPIRED : Expired PRN Number");
                    System.out.print("EXPIRED : Expired PRN Number");
                    //E-slip Consultation results for EXPIRED PRN
                    System.out.println("-------------------------------------------------------");
                    System.out.println("CONSULTATION FOR E-SLIP NO : " + prn);
                    System.out.println("-------------------------------------------------------");
                    //Result
                    cu.xmlToJSON(responseXML);
                    response.setHashCode(responseHash);
                    response.setEslipDetailsArrayList(null);
                    response.setEslipHeaderArrayList(null);
                    response.setResultArrayList(resultArrayList);
                    System.out.println("-------------------------------------------------------");
                    System.out.println("--END--");
                    System.out.println("-------------------------------------------------------");

                    //----START OF DATABASE UPDATE ----\\
                    //Save PRN details in the database
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: STARTING TO SAVE E-SLIP RESULTS FOR STATUS EXPIRED");
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    String data = "" + responseStatus + "," + responseRemaks + "," + responseHash + "," + timeStamp + "," + "N" + "," + "CR" + "," + prn;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                        update = DatabaseMethods.DB(update_query, 7, data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                    } else {
                        logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                    }
                    //----END OF DATABASE UPDATE ----\\
                } else {
                    logger.info("UNKNOWN ERROR : CONSULTATION OPERATION FAILED!");
                    System.out.println("UNKNOWN ERROR : CONSULTATION OPERATION FAILED!");
                }
                logger.info("DONE CONSULTING PRN " + prn);

            }
        }
        return response;
    }

    //PAYMENT PROCESSING
    public PaymentResponse postTaxPayment(String eSlipNumber,String meansOfPayment,String chequeno,String account) throws JAXBException, SQLException, JRException, ClassNotFoundException, IOException {
        System.out.println("E-Slip Number : " + eSlipNumber);
        String ccrspayment = "";

        //More Variables
        String amount = "";
        String eslip_status = "";
        String payment_code = "";
        String pay_mentadvicedate = "";
        String tax_component = "";
        String taxpayer_fullname = "";
        String taxpayer_pin = "";
        String doc_refnumber = "";
        String poststat_us = "";
        String means_of_pmnt = "";

        //Queries
        //Select details from e-slip data table
        //Array with all data selected from the eslip data table
        String[] eslipdata = null;
        String query = cn.getProperties().getProperty("sql.query.select.eslipdetails").trim();
        String eslipdetails = DatabaseMethods.selectValues(query, 8, 1, eSlipNumber);
        if(!eslipdetails.equalsIgnoreCase("")) {
            eslipdata= eslipdetails.split(",");
            amount = eslipdata[0];
            eslip_status = eslipdata[1];
            payment_code = eslipdata[2];
            pay_mentadvicedate = eslipdata[3];
            tax_component = eslipdata[4];
            taxpayer_fullname = eslipdata[5];
            taxpayer_pin = eslipdata[6];
            doc_refnumber = eslipdata[7];
            System.out.println("E-Slip :: "+Arrays.toString(eslipdata));
        }

        //Select Payment Details from payment table
        //Array with all data selected from payment details table
        String[] paymentdata =null;
        String selectquery = cn.getProperties().getProperty("sql.query.select.paymentdetails").trim();
        String pay_details = DatabaseMethods.selectValues(selectquery, 2, 1, eSlipNumber);
        if(!pay_details.equalsIgnoreCase("")) {
            paymentdata= pay_details.split(",");
            poststat_us = paymentdata[0];
            means_of_pmnt = paymentdata[1];
            System.out.println("Pay :: "+Arrays.toString(paymentdata));
        }

        //Time Stamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

        //Random number for teller id
        Random r = new Random();
        int low = 10;
        int high = 100;
        String tellerid = String.valueOf(r.nextInt(high - low) + low);

        //Update Payment Details Query
        String update_query = cn.getProperties().getProperty("sql.query.update.paymentdetails").trim();
        //Update E-Slip Details Query
        String update_eslip_query = cn.getProperties().getProperty("sql.query.update.eslipdetails").trim();
        //Select Query for counting the number of records in the E-Slip Data Table
        String select_count = cn.getProperties().getProperty("sql.query.select.count").trim();
        //Instance of Payment Response class
        PaymentResponse response = new PaymentResponse();
        //Select query for checking delete flag
        //delete flag
        String selectflag = cn.getProperties().getProperty("sql.query.select.deleteflag").trim();
        String deleteflag = DatabaseMethods.selectValues(selectflag, 1, 1, eSlipNumber);
        //Checking if a PRN has been deleted (Delete Flag = Y)
        if(deleteflag.equalsIgnoreCase("Y"))
        {
            logger.info("CHECKING PRN NUMBER :: THIS E-SLIP WAS DELETED!");
            //Customizing the response
            response.setMessage("CHECKING PRN NUMBER :: THIS E-SLIP WAS DELETED!");
            response.setResponseCode("60010");
            response.setStatus("NOK :: DELETED");
            response.setPaymentNumber(eSlipNumber);
        }
        //Continue with Posting of Payment for this e-slip number
        else {
            //Checking if the PRN has been Consulted
            if (!DatabaseMethods.selectValues(select_count, 1, 1, eSlipNumber).equalsIgnoreCase("1")) {
                logger.info("POSTING PAYMENT :: CHECKING PRN NUMBER :: THIS PRN (E-SLIP) NUMBER HAS NOT BEEN CONSULTED :: CONSULT THE E-SLIP FIRST");
                //Customizing the response
                response.setMessage("CHECKING PRN NUMBER :: THIS PRN (E-SLIP) NUMBER HAS NOT BEEN CONSULTED :: CONSULT THE E-SLIP FIRST");
                response.setResponseCode("60010");
                //998608
                response.setStatus("NOK");
                response.setPaymentNumber(eSlipNumber);
            }
            //Checking If Payment Has Already Been Posted For an E-Slip \\
            else if (eslip_status.equalsIgnoreCase("Y")) {
                logger.info("POSTING PAYMENT :: CHECKING PRN NUMBER :: RESPONSE :: PAYMENT WAS ALREADY POSTED FOR THIS E-SLIP NUMBER");
                //Customizing the response
                response.setMessage("CHECKING PRN NUMBER :: PAYMENT WAS ALREADY POSTED FOR THIS E-SLIP NUMBER");
                response.setResponseCode("60010");
                response.setStatus("NOK");
                response.setPaymentNumber(eSlipNumber);
            } else {
                //Instance of the Payment File Class
                PaymentDetails pd = new PaymentDetails();
                //Instance of the Cheque Details Class
                ChequeDetails cd = new ChequeDetails();

                //Array List with Cheque details
                ArrayList<ChequeDetails> chequeDetailsArrayList = new ArrayList<>();
                chequeDetailsArrayList.add(cd);

                //Payment Header
                PaymentHeader ph = new PaymentHeader();
                ph.setSystemCode(SYSTEMCODE);
                ph.setBranchCode(bankbranch);
                ph.setBankTellerId(tellerid);
                ph.setBankTellerName(tellername);
                ph.setPaymentMode(PAYMENTMODE);
                ph.setMeansofPayment(meansOfPayment);
                ph.setChequeAmount("");
                ph.setRemitterId(REMiTTERID);
                ph.setRemitterName(REMITTERNAME);
                ph.setESlipNumber(eSlipNumber);
                ph.setSlipPaymentCode(payment_code.trim());
                ph.setPaymentAdviceDate(pay_mentadvicedate.trim());
                ph.setPaymentReference(tax_component.trim());
                ph.setTaxpayerName(taxpayer_fullname.trim());
                ph.setTaxpayerPIN(taxpayer_pin.trim());
                ph.setTotalAmount(amount.trim());
                ph.setDocRefNumber(doc_refnumber.trim());
                ph.setDateOfCollection(timestamp);
                ph.setCurrency(CURRENCY);
                ph.setCashAmount(amount.trim());

                //Arraylist with payment header details
                ArrayList<PaymentHeader> paymentHeaderArrayList = new ArrayList<>();
                paymentHeaderArrayList.add(ph);

                //Checking the means of payment
                //Means Of Payment is Cash (Do Not Add Cheque Details to the Payment XML)
                if (meansOfPayment.equalsIgnoreCase("1")) {
                    logger.info("POSTING PAYMENT :: MEANS OF PAYMENT :: CASH :: EXCLUDING CHEQUE DETAILS FROM THE PAYMENT XML");
                    chequeno = "N/A";
                    account = "N/A";
                    //Set Cheque Details to N/A
                    cd.setBankOfCheque("N/A");
                    cd.setBranchOfCheque("N/A");
                    cd.setChequeNumber(chequeno);
                    cd.setChequeDate("N/A");
                    cd.setChequeAmount("0");
                    cd.setChequeAccount(account);
                    ph.setChequeAmount("0");
                }

                //Means of Payment = Cheque (Same Bank) and Both (Cash & Cheque) (Add Cheque Details to the Payment XML)
                else if (meansOfPayment.equalsIgnoreCase("2") || meansOfPayment.equalsIgnoreCase("3")) {
                    //Adding Cheque details to payment details
                    ph.setCashAmount("0");
                    ph.setChequeAmount(amount);
                    cd.setChequeAmount(amount);
                    cd.setChequeDate(timestamp);
                    cd.setBankOfCheque(BANKCODE);
                    cd.setBranchOfCheque(bankbranch);
                    cd.setChequeAccount(account);
                    cd.setChequeNumber(chequeno);
                    //Adding cheque details to the arraylist
                    pd.setChequeDetails(chequeDetailsArrayList);
                    logger.info("POSTING PAYMENT :: MEANS OF PAYMENT :: BOTH/SAME BANK CHEQUE :: ADDING CHEQUE DETAILS TO THE PAYMENT XML");
                }

                pd.setPaymentHeader(paymentHeaderArrayList);

                //Create A hash code for this payment
                String hashcode = cu.createHashCode(ph.getESlipNumber(), ph.getPaymentAdviceDate(), ph.getTaxpayerPIN(), ph.getTotalAmount(), ph.getBankTellerId(), ph.getPaymentReference(), this.BANKCODE, this.LOGINID);

                //Add hash code to the payment String
                pd.setHashCode(hashcode);

                //Adding Payment Details to the final Payment information XML
                PaymentInformation information = new PaymentInformation();
                ArrayList<PaymentDetails> details = new ArrayList<>();
                details.add(pd);

                //Adding all Payment details to the final payment file
                information.setPaymentDetails(details);
                //Generate Payment XML
                logger.info("PAYMENT DOCUMENT :: GENERATING PAYMENT XML");
                try {
                    //File file = new File("src/main/resources/pay.xml");
                    JAXBContext jaxbContext = JAXBContext.newInstance(PaymentInformation.class);
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                    // output pretty printed
                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    //jaxbMarshaller.marshal(information, file);
                    StringWriter sw = new StringWriter();
                    jaxbMarshaller.marshal(information, sw);
                    //Assign the output xml to a string
                    ccrspayment = sw.toString();
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
                logger.info("POSTING PAYMENT :: PAYMENT DOCUMENT :: DONE GENERATING PAYMENT XML");
                logger.info("POSTING PAYMENT :: STARTING POSTING OF PAYMENT");
                //Disable Certificate Checking
                cu.disableVerification();
                //----START OF DATABASE INSERT ----\\
                //VARIABLES
                String systemCode = ph.getSystemCode();
                String paymentmode = ph.getPaymentMode();
                String branchcode = ph.getBranchCode();
                String branchtellerid = ph.getBankTellerId();
                String banktellername = ph.getBankTellerName();
                String meansofpayment = ph.getMeansofPayment();
                String remitterid = ph.getRemitterId();
                String remittername = ph.getRemitterName();
                String slippaymentcode = ph.getSlipPaymentCode();
                String paymentadvicedate = ph.getPaymentAdviceDate();
                String paymentreference = ph.getPaymentReference();
                String taxPayerpin = ph.getTaxpayerPIN();
                String taxpayername = ph.getTaxpayerName();
                String totalamount = ph.getTotalAmount();
                String docrefno = ph.getDocRefNumber();
                String dateofcollection = ph.getDateOfCollection();
                String currency = ph.getCurrency();
                String cashamount = ph.getCashAmount();
                String chequesamount = ph.getChequeAmount();
                String bankofcheque = cd.getBankOfCheque();
                String branchofcheque = cd.getBranchOfCheque();
                String chequenumber = cd.getChequeNumber();
                String chequedate = cd.getChequeDate();
                String chequeamount = cd.getChequeAmount();
                String chequeaccount = cd.getChequeAccount();
                String dateofpayment = new SimpleDateFormat("yyyy.MM.dd'T'HH.mm.ss").format(new Date());
                String poststatus = "PS";
                String response_Status = "N/A";
                String response_code = "N/A";
                String m_essage = "N/A";
                String channelid = "FI";

                //Handling Null Values
                if (slippaymentcode.equalsIgnoreCase("")) {
                    slippaymentcode = "N/A";
                }
                if (docrefno.equalsIgnoreCase("")) {
                    docrefno = "N/A";
                }
                if (bankofcheque.equalsIgnoreCase("")) {
                    bankofcheque = "N/A";
                }
                if (branchofcheque.equalsIgnoreCase("")) {
                    branchofcheque = "N/A";
                }
                if (chequenumber.equalsIgnoreCase("")) {
                    chequenumber = "N/A";
                }
                if (chequedate.equalsIgnoreCase("")) {
                    chequedate = "N/A";
                }
                if (chequeamount.equalsIgnoreCase("")) {
                    chequeamount = "0";
                }
                if (chequeaccount.equalsIgnoreCase("")) {
                    chequeaccount = "N/A";
                }

                //SAVE DETAILS TO BE SENT TO KRA FOR POSTING PAYMENT IN THE DATABASE
                logger.info("POST PAYMENT :: SAVE DATA TO BE SENT TO DB :: STARTING TO SAVE PAYMENT DATA");
                String sendsql = cn.getProperties().getProperty("sql.query.insert.paymentdetails").trim();
                String data = systemCode + "," + branchcode + "," + branchtellerid + "," + banktellername + "," + paymentmode + "," + meansofpayment + "," + remitterid + "," + remittername
                        + "," + eSlipNumber + "," + slippaymentcode + "," + paymentadvicedate + "," + paymentreference + "," + taxPayerpin
                        + "," + taxpayername + "," + totalamount + "," + docrefno + "," + dateofcollection + "," + cashamount + "," + chequesamount
                        + "," + hashcode + "," + bankofcheque + "," + branchofcheque + "," + chequenumber + "," + chequedate + "," + chequeamount
                        + "," + chequeaccount + "," + dateofpayment + "," + poststatus + "," + response_Status + "," + response_code + "," + m_essage + "," + channelid + "," + currency;
                //Check for duplicates
                if (poststat_us.equalsIgnoreCase("PS")) {
                    System.out.println("POST PAYMENT :: SAVE DATA TO BE SENT TO DB :: CHECKING DUPLICATES :: RESULTS :: Found this e-slip number in the db");
                } else {
                    int insert = DatabaseMethods.DB(sendsql, 33, data);
                    logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT DATA :: RESULT :: " + insert);
                }
                //----END OF DATABASE INSERT ----\\

                //Posting Payment
                logger.info("POSTING PAYMENT :: Posting Payment Now for :: " + ccrspayment);
                //1.Response XML for OK :: 60000 (Successful)
                String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>" + eSlipNumber + "</PaymentNumber><ResponseCode>OK</ResponseCode><Status>60000</Status><Message>SUCCESSFULLY RECEIVED THE PAYMENT INFORMATION</Message></PAYMENTS></RESPONSE>";
                //2.Response XML for NOK :: 60001 (Empty Message)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60001</Status><Message>EMPTY MESSAGE</Message></PAYMENTS></RESPONSE>";
                //3.Response XML for NOK :: 60002 (Invalid Message)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60002</Status><Message>Invalid Message or incoherent – Parsing: <<Error ID in iTAX>></Message></PAYMENTS></RESPONSE>";
                //4.Response XML for NOK :: 60003 (This Payment Reference is already there in Payment Gateway)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60003</Status><Message>THE PAYMENT FOR REQUESTED PRN HAS ALREADY BEEN PROCESSED</Message></PAYMENTS></RESPONSE>";
                //5.Response XML for NOK :: 60004 (Invalid Data)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60004</Status><Message>INVALID DATA</Message></PAYMENTS></RESPONSE>";
                //6.Response XML for NOK :: 60005 (Inconsistency with the Amount)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60005</Status><Message>INCONSISTENCY WITH THE AMOUNT</Message></PAYMENTS></RESPONSE>";
                //7.Response XML for NOK :: 60006 (E Slip Unknown.)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60006</Status><Message>E-SLIP IS UNKNOWN</Message></PAYMENTS></RESPONSE>";
                //8.Response XML for NOK :: 60007 (Other Errors: <<Error ID in iTAX>>)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60007</Status><Message>OTHER ERRORS</Message></PAYMENTS></RESPONSE>";
                //9.Response XML for NOK :: 60009 (Expired)
                //String responseXML = "<RESPONSE><PAYMENTS><PaymentNumber>"+eSlipNumber+"</PaymentNumber><ResponseCode>NOK</ResponseCode><Status>60009</Status><Message>THE REQUESTED PRN IS EXPIRED. REQUEST YOU TO PLEASE ASK TAXPAYER TO REGISTER THE PAYMENT AGAIN ON SYSTEM</Message></PAYMENTS></RESPONSE>";
                System.out.println("Response XML :: " + responseXML);
                //Unmarshall the response XML String
                if (responseXML != null) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{PostPaymentXMLToJava.class});
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    StringReader XmlreaderObj = new StringReader(responseXML);
                    PostPaymentXMLToJava XmltoJavaObj = (PostPaymentXMLToJava) jaxbUnmarshaller.unmarshal(XmlreaderObj);

                    //Response
                    String responseStatus = ((PaymentResponse) XmltoJavaObj.getRESPONSE().get(0)).getStatus().trim();
                    String paymentnumber = ((PaymentResponse) XmltoJavaObj.getRESPONSE().get(0)).getPaymentNumber().trim();
                    String responsecode = ((PaymentResponse) XmltoJavaObj.getRESPONSE().get(0)).getResponseCode().trim();
                    String message = ((PaymentResponse) XmltoJavaObj.getRESPONSE().get(0)).getMessage().trim();

                    //Handling response
                    response.setMessage(message);
                    response.setResponseCode(responsecode);
                    response.setPaymentNumber(paymentnumber);
                    response.setStatus(responseStatus);

                    logger.info("POST PAYMENT :: STATUS :: " + responseStatus + " :: CODE :: " + responsecode + " :: MESSAGE :: " + message);

                    //Check Response Codes and the Status
                    //1. OK and Response Code 60000(Successfully Received the Payment information)
                    if (responsecode.equalsIgnoreCase("OK") && responseStatus.equalsIgnoreCase("60000")) {
                        System.out.println("POST PAYMENT :: OK ::  60000 :: Successfully Received the Payment information :: PAYMENT NUMBER :: " + paymentnumber);
                        logger.info("POST PAYMENT :: OK ::  60000 :: Successfully Received the Payment information :: PAYMENT NUMBER :: " + paymentnumber);

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS OK :: 60000");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg + "," + eSlipNumber;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 5, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);
                                String d = "Y," + eSlipNumber;
                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, d);
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);

                                //Save the Receipt as a Pdf file after successful payment
                                Numbers2Words ntw = new Numbers2Words();
                                String c_lass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.class")).trim();
                                Class.forName(c_lass);
                                String serverName = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.ip").trim());
                                String portNumber = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.port").trim());
                                String sid = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.database").trim());
                                String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
                                String username = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.username").trim());
                                String password = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.password").trim());
                                String input = "";
                                String output = folder + "receipt.pdf";

                                //Queries
                                //Select Amount
                                String select_amount = cn.getProperties().getProperty("sql.query.select.amount").trim();
                                String amnt = DatabaseMethods.selectValues(select_amount, 1, 1, eSlipNumber);
                                //Select Means of Payment
                                String select_mop = cn.getProperties().getProperty("sql.query.select.meansofpayment").trim();
                                String mop = DatabaseMethods.selectValues(select_mop, 1, 1, eSlipNumber);

                                //Check the means of Payment
                                if (mop.equalsIgnoreCase("1")) {
                                    mop = "Cash";
                                    input = folder + "receipt.jasper";
                                }
                                if (mop.equalsIgnoreCase("2")) {
                                    mop = "Cheque";
                                    input = folder + "cheque.jasper";
                                }
                                if (mop.equalsIgnoreCase("3")) {
                                    mop = "Both (Cheque and Cash)";
                                    input = folder + "cheque.jasper";
                                }
                                String resp_onse = "";
                                //Check if amount is 0
                                if (!amount.equalsIgnoreCase("0") && !amount.equalsIgnoreCase("")) {
                                    String word = null;
                                    word = ntw.EnglishNumber(Long.parseLong(amount));
                                    Connection con = DriverManager.getConnection(url, username, password);
                                    JasperReport jasperReport
                                            = (JasperReport) JRLoader.loadObjectFromFile(input);
                                    Map parameters = new HashMap();
                                    parameters.put("prn", eSlipNumber);
                                    parameters.put("words", word);
                                    parameters.put("mop", mop);
                                    // Fill the Jasper Report
                                    JasperPrint jasperPrint
                                            = JasperFillManager.fillReport(jasperReport, parameters, con);
                                    // Creation of the Pdf Jasper Reports
                                    Path path = Paths.get(folder, "receipt.pdf");
                                    // Creation of the Pdf Jasper Reports
                                    File f = new File(output.trim());
                                    if (f.exists() && !f.isDirectory()) {
                                        output = folder + "receipt-" + eSlipNumber + ".pdf";
                                        path = Paths.get(folder, "receipt-" + eSlipNumber + ".pdf");
                                    }
                                    JasperExportManager.exportReportToPdfFile(jasperPrint, output);
                                    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
                                    resp_onse = "Successful!";
                                    logger.info("POST PAYMENT :: SAVE RECEIPT :: DONE :: RECEIPT NAME :: " + output + " :: RESPONSE :: " + resp_onse);
                                } else {
                                    resp_onse = "Failed because amount is Ksh.0";
                                    logger.info("POST PAYMENT :: SAVE RECEIPT :: FAILED :: RESPONSE :: " + resp_onse);
                                }
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }

                    //2. NOK and Response Code 60001 (Empty Message)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60001")) {
                        System.out.println("POST PAYMENT :: NOK ::  60001 :: Empty Message");
                        logger.info("POST PAYMENT :: NOK ::  60001 :: Empty Message");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60001");
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }

                    //3. NOK and Response Code 60002 (Invalid Message or incoherent – Parsing: <<Error ID in iTAX>>)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60002")) {
                        System.out.println("POST PAYMENT :: NOK ::  60002 :: Invalid Message or incoherent – Parsing: <<Error ID in iTAX>>");
                        logger.info("POST PAYMENT :: NOK ::  60002 :: Invalid Message or incoherent – Parsing: <<Error ID in iTAX>>");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60002");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }

                    //4. NOK and Response Code 60003 (This Payment Reference is already there in Payment Gateway)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60003")) {
                        System.out.println("POST PAYMENT :: NOK ::  60003 :: This Payment Reference is already there in Payment Gateway");
                        logger.info("POST PAYMENT :: NOK ::  60003 :: This Payment Reference is already there in Payment Gateway ");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60003");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }
                    //5. NOK and Response Code 60004 (Invalid Data: <<Details of the data found invalid>>)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60004")) {
                        System.out.println("POST PAYMENT :: NOK ::  60004 :: Invalid Data :: MESSAGE ::" + message);
                        logger.info("POST PAYMENT :: NOK ::  60004 :: Invalid Data :: MESSAGE ::" + message);

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60004");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }

                    //6. NOK and Response Code 60005 (Inconsistency with the Amount)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60005")) {
                        System.out.println("POST PAYMENT :: NOK ::  60005 :: Inconsistency with the Amount");
                        logger.info("POST PAYMENT :: NOK ::  60005 :: Inconsistency with the Amount");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60005");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }
                    //7. NOK and Response Code 60006 (E Slip Unknown.)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60006")) {
                        System.out.println("POST PAYMENT :: NOK ::  60006 :: E Slip Unknown.");
                        logger.info("POST PAYMENT :: NOK ::  60006 :: E Slip Unknown.");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60006");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }
                    //8. NOK and Response Code 60007 (Other Errors: <<Error ID in iTAX>>)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60007")) {
                        System.out.println("POST PAYMENT :: NOK ::  60007 :: Other Errors: <<Error ID in iTAX>>");
                        logger.info("POST PAYMENT :: NOK ::  60007 :: Other Errors: <<Error ID in iTAX>>");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60007");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }

                    //8. NOK and Response Code 60009 (E Slip Has Expired.)
                    else if (responsecode.equalsIgnoreCase("NOK") && responseStatus.equalsIgnoreCase("60009")) {
                        System.out.println("POST PAYMENT :: NOK ::  60009 :: THE REQUESTED PRN IS EXPIRED. REQUEST YOU TO PLEASE ASK TAXPAYER TO REGISTER THE PAYMENT AGAIN ON SYSTEM");
                        logger.info("POST PAYMENT :: NOK ::  60009 :: THE REQUESTED PRN IS EXPIRED. REQUEST YOU TO PLEASE ASK TAXPAYER TO REGISTER THE PAYMENT AGAIN ON SYSTEM");

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS NOK :: 60009");
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 4, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 1, "Y");
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);
                            }
                        } else {
                            logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                        }
                        //----END OF DATABASE UPDATE ----\\
                    }
                }
            }
        }
        return response;
    }


    public DeletePRNResponse deletePRN(String eSlipNumber){
            //Update Delete Flag in E-Slip Data Table to 'Y'
            String query = cn.getProperties().getProperty("sql.query.prn.delete.update.flag").trim();
            //----START OF PRN DELETION (UPDATE DELETE FLAG) ---\\
            //Instance of Delete PRN Number Response class
            DeletePRNResponse response = new DeletePRNResponse();
            int update = 0;
            //Checking if a payment has already been posted for this e-slip
            //E-Slip status
            String selectstatus = cn.getProperties().getProperty("sql.query.select.eslipstatus").trim();
            //delete flag
            String selectflag = cn.getProperties().getProperty("sql.query.select.deleteflag").trim();
            String eslipstatus = DatabaseMethods.selectValues(selectstatus, 1, 1, eSlipNumber);
            String deleteflag = DatabaseMethods.selectValues(selectflag, 1, 1, eSlipNumber);
            System.out.println("Flag :: " + deleteflag);
            String data = "Y," + eSlipNumber;
            if (eslipstatus.equalsIgnoreCase("N")) {
                if (deleteflag.equalsIgnoreCase("N")) {
                    update = DatabaseMethods.DB(query, 2, data);
                    if (update == 1) {
                        logger.info("DELETE PRN :: DONE DELETING E-SLIP :: RESULT :: " + update);
                        response.setResponse("DELETE PRN :: SUCCESSFULLY DELETED E-SLIP (PRN)");
                    } else {
                        logger.info("DELETE PRN :: FAILED TO DELETE PRN :: RESULT :: " + update);
                        response.setResponse("DELETE PRN :: FAILED TO DELETE AN E-SLIP (PRN)");
                    }
                } else {
                    logger.info("DELETE PRN :: FAILED TO DELETE AN E-SLIP (PRN) :: THIS PRN WAS ALREADY DELETED :: " + update);
                    response.setResponse("DELETE PRN :: FAILED TO DELETE AN E-SLIP (PRN) :: THIS PRN WAS ALREADY DELETED");
                }
            } else {
                logger.info("DELETE PRN :: PAYMENT WAS ALREADY POSTED FOR THIS E-SLIP :: IT CANNOT BE DELETED");
                response.setResponse("DELETE PRN :: PAYMENT WAS ALREADY POSTED FOR THIS E-SLIP :: IT CANNOT BE DELETED");
            }
        //----END OF DATABASE UPDATE ----\\
        return response;
    }

}