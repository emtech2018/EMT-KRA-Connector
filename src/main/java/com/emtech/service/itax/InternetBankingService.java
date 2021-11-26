/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emtech.service.itax;

import com.emtech.service.Numbers2Words;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.KRAPaymentGatewayService;
import com.emtech.service.itax.utilities.*;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
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

/**
 *
 * @author Omukubwa Emukule
 */
@WebService(serviceName = "InternetBankingService")
public class InternetBankingService {
    //Instance of the Configuration Classes
    Configurations cn = new Configurations();

    //Log In ID
    private String LOGINID = cn.getProperties().getProperty("itax.username").trim();

    //Password
    private String PASSWORD = cn.getProperties().getProperty("itax.password").trim();

    //Bank Code
    private String BANKCODE = cn.getProperties().getProperty("itax.bankcode").trim();

    //System Code
    private String SYSTEMCODE = cn.getProperties().getProperty("itax.systemcode").trim();

    //Payment Mode
    private String PAYMENTMODE = cn.getProperties().getProperty("itax.mofp1").trim();

    //Currency
    private String CURRENCY = cn.getProperties().getProperty("itax.currency").trim();

    //Remitter Id
    private String REMiTTERID = cn.getProperties().getProperty("itax.remitterid").trim();

    //Remitter Name
    private String REMITTERNAME = cn.getProperties().getProperty("itax.remittername").trim();

    //Remitter Name
    //private String REMITTERNAME = UUID.randomUUID().toString();

    //Teller Name
    //private String tellername = cn.getProperties().getProperty("itax.tellername").trim();
    //private String tellername = UUID.randomUUID().toString();

    //Bank Branch
    //private String bankbranch = cn.getProperties().getProperty("itax.bankbranch").trim();

    //Encryption Key
    private String key = cn.getProperties().getProperty("enc.key").trim();

    //Encryption Init Vector
    private String initVector =  cn.getProperties().getProperty("enc.initVector").trim();

    //Database Connection Class
    DatabaseMethods db = new DatabaseMethods();

    //Common Utilities Class
    CommonUtils cu = new CommonUtils();

    //Instance of SFTP class
    sftp ftp = new sftp();

    //Instance of numbers (amount) to words class
    Numbers2Words ntw = new Numbers2Words();

    //LOGGING
    private static final Logger logger= LoggerFactory.getLogger(PaymentService.class);
    //Random Number (Trace No)
    int min = 10000;
    int max = 999999;
    Random rn = new Random();
    int traceno = rn.nextInt(max - min + 1) + min;
    //Receipts Folder
    String folder = cn.getProperties().getProperty("itax.folder").trim();
    //Variable
    //Connection Timeout
    private int PAYMENT_GATEWAY_WEBSERVICE_TIMEOUT = Integer.parseInt(cn.getProperties().getProperty("itax.wsdl.timeout").trim());
    //Web Service URL
    private String PAYMENT_GATEWAY_WEBSERVICE_URL = cn.getProperties().getProperty("itax.wsdl.url").trim();

    //Consulting an Eslip
    @WebMethod
    @WebResult(name = "return", targetNamespace = "")
    @RequestWrapper(localName = "consult", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.CheckEslip")
    @ResponseWrapper(localName = "consultResponse", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.CheckEslipResponse")
    //1. Consulting the PRN (E-SLIP NUMBER)
    public CheckEslipResponse consultEslip(
            @WebParam(name = "eslipNumber", targetNamespace = "") String prn)  throws IOException, JAXBException {
        String password = Encryptor.decrypt(key,initVector,PASSWORD);
        String username = Encryptor.decrypt(key,initVector,LOGINID);
        //System.out.println(username+" :: "+password);
        String responseXML = "";
        //Update Query For status VALID
        String update_valid_query = cn.getProperties().getProperty("sql.update.valid.status");
        //Update Query for other status
        String update_query = cn.getProperties().getProperty("sql.update.other.status");
        //Select Query
        String select_query = cn.getProperties().getProperty("sql.select.query");
        //Select query for tax-code (checking duplicates)
        String select_query_taxcode = cn.getProperties().getProperty("sql.select.query.taxcode");
        //Timestamp
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        CheckEslipResponse response = new CheckEslipResponse();
        KRAPaymentGatewayService kra = new KRAPaymentGatewayService();
        //Disable Certificate Checking
        cu.disableVerification();
        logger.info("STARTING CONSULTING PRN NUMBER "+ prn);
        logger.info("Connecting to the Payment Gateway " + kra.getWSDLDocumentLocation());
        kra.getWSDLDocumentLocation().openConnection().connect();
        kra.getWSDLDocumentLocation().openConnection().setConnectTimeout(PAYMENT_GATEWAY_WEBSERVICE_TIMEOUT);

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
            //Disable Certificate Checking
            cu.disableVerification();
            responseXML = kra.getKRAPaymentGatewayPort().checkEslip(username, password, prn);
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
                    logger.info("EXPIRED : Utilized PRN Number");
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

    //Process Tax Payment
    //2.POST Payment to the I-TAX Payment Gateway
    @WebMethod
    @WebResult(name = "response", targetNamespace = "")
    @RequestWrapper(localName = "processPayment", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.AcceptPayment")
    @ResponseWrapper(localName = "processPaymentResponse", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.PaymentResponse")
    public PaymentResponse postTaxPayment(@WebParam(name = "eSlipNumber", targetNamespace = "") String eSlipNumber,
                                          @WebParam(name = "meansOfPayment", targetNamespace = "") String meansOfPayment,
                                          @WebParam(name = "tellername", targetNamespace = "") String teller_name,
                                          @WebParam(name = "tellerid", targetNamespace = "") String teller_id,
                                          @WebParam(name = "branchcode", targetNamespace = "") String branch_code,
                                          @WebParam(name = "chequeno", targetNamespace = "") String chequeno,
                                          @WebParam(name = "account", targetNamespace = "") String account) throws IOException, JAXBException, ClassNotFoundException, SQLException, JRException {
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
        //Select Query (Post Status)
        String select_query = cn.getProperties().getProperty("sql.query.select.payment.status");
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
        }

        //Select Payment Details from payment table
        //Array with all data selected from payment details table
        String[] paymentdata =null;
        String selectquery = cn.getProperties().getProperty("sql.query.select.paymentdetails").trim();
        String pay_details = DatabaseMethods.selectValues(selectquery, 2, 1, eSlipNumber);
        if(!pay_details.equalsIgnoreCase("")) {
            paymentdata= pay_details.split(",");
            //poststat_us = paymentdata[0];
            means_of_pmnt = paymentdata[1];
        }

        //Decrypt Credentials
        String password = Encryptor.decrypt(key, initVector, PASSWORD);
        String username = Encryptor.decrypt(key, initVector, LOGINID);

        //Time Stamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

        //Random number for teller id
        Random r = new Random();
        int low = 10;
        int high = 100;
        //String tellerid = String.valueOf(r.nextInt(high - low) + low);
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
                ph.setBranchCode(BANKCODE+branch_code);
                ph.setBankTellerId(teller_id);
                ph.setBankTellerName(teller_name);
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
                    cd.setBranchOfCheque(BANKCODE+branch_code);
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
                KRAPaymentGatewayService kra = new KRAPaymentGatewayService();
                //Disable Certificate Checking
                cu.disableVerification();
                logger.info("POSTING PAYMENT :: Connecting to the Payment Gateway :: " + kra.getWSDLDocumentLocation());
                kra.getWSDLDocumentLocation().openConnection().connect();
                kra.getWSDLDocumentLocation().openConnection().setConnectTimeout(PAYMENT_GATEWAY_WEBSERVICE_TIMEOUT);
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
                String channelid = "IB";

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
                if (DatabaseMethods.findDuplicates(select_query, 1, eSlipNumber)) {
                    System.out.println("POST PAYMENT :: SAVE DATA TO BE SENT TO DB :: CHECKING DUPLICATES :: RESULTS :: Found this e-slip number in the db");
                } else {
                    int insert = DatabaseMethods.DB(sendsql, 33, data);
                    logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT DATA :: RESULT :: " + insert);
                }

                //Select Post Status from payment details table
                poststat_us = DatabaseMethods.selectValues(select_query,1,1,eSlipNumber);

                //----END OF DATABASE INSERT ----\\

                //Posting Payment
                logger.info("POSTING PAYMENT :: Posting Payment Now for :: " + ccrspayment);
                String responseXML = kra.getKRAPaymentGatewayPort().acceptPayment(username, password, ccrspayment);
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

                    //Variable holding data used to Update e-slip-status to Y after posting payment
                    String eslipstatus_data = "Y,"+eSlipNumber;

                    //Check Response Codes and the Status
                    //1. OK and Response Code 60000(Successfully Received the Payment information)
                    if (responsecode.equalsIgnoreCase("OK") && responseStatus.equalsIgnoreCase("60000")) {
                        System.out.println("POST PAYMENT :: OK ::  60000 :: Successfully Received the Payment information :: PAYMENT NUMBER :: " + paymentnumber);
                        logger.info("POST PAYMENT :: OK ::  60000 :: Successfully Received the Payment information :: PAYMENT NUMBER :: " + paymentnumber);

                        //----START OF DATABASE UPDATE ----\\
                        //UPDATE DETAILS IN THE DATABASE (Payment Details)
                        logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: STARTING TO SAVE PAYMENT DETAILS FOR STATUS OK :: 60000");

                        String post_status = "PR";
                        String r_code = responseStatus;
                        String r_status = responsecode;
                        String msg = message;
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg + "," + eSlipNumber;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.trim().equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 5, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
                                logger.info("POST PAYMENT :: DONE UPDATING E-SLIP-DATA-TABLE :: RESULT :: " + i);

                                //START OF RECEIPT SAVING\\
                                //Save the Receipt as a Pdf file after successful payment
                                String c_lass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.class")).trim();
                                Class.forName(c_lass);
                                String serverName = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.ip").trim());
                                String portNumber = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.port").trim());
                                String sid = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.database").trim());
                                String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
                                String uname = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.username").trim());
                                String pass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.password").trim());
                                String input = "";
                                String output =folder+"receipt.PDF";

                                //Queries
                                //Select Amount
                                String select_amount = cn.getProperties().getProperty("sql.query.select.amount").trim();
                                //Select E-Slip Status From the Database
                                String select_status = cn.getProperties().getProperty("sql.query.select.eslipstatus").trim();
                                String tax_amount = DatabaseMethods.selectValues(select_amount, 1, 1, eSlipNumber);
                                String status = DatabaseMethods.selectValues(select_status, 1, 1, eSlipNumber);
                                //Response
                                String respon_se = "";
                                //Check if Payment Has Been Posted for the selected PRN
                                if(status.equalsIgnoreCase("N"))
                                {
                                    respon_se = "Failed! E-Slip Status is N :: Payment Not Yet Posted :: Post Payment First!";
                                    System.out.println("PRINT RECEIPT :: FAILED :: RESPONSE :: " + response);
                                }
                                //Proceed Printing a receipt if payment was posted
                                else if(status.equalsIgnoreCase("Y")) {
                                    //Select Means of Payment
                                    String select_mop = cn.getProperties().getProperty("sql.query.select.meansofpayment").trim();
                                    String mop = DatabaseMethods.selectValues(select_mop, 1, 1, eSlipNumber);

                                    //Check the means of Payment
                                    if (mop.equalsIgnoreCase("1")) {
                                        mop = "Cash";
                                        input = folder + "jasper/receipt.jasper";
                                    }
                                    if (mop.equalsIgnoreCase("2")) {
                                        mop = "Cheque";
                                        input = folder + "jasper/cheque.jasper";
                                    }
                                    if (mop.equalsIgnoreCase("3")) {
                                        mop = "Both (Cheque and Cash)";
                                        input = folder + "jasper/cheque.jasper";
                                    }
                                    //Check if amount is 0
                                    if (!tax_amount.equalsIgnoreCase("0") && !tax_amount.equalsIgnoreCase("")) {
                                        String word = null;
                                        word = ntw.EnglishNumber(Long.parseLong(tax_amount));
                                        Connection con = DriverManager.getConnection(url, uname, pass);
                                        JasperReport jasperReport
                                                = (JasperReport) JRLoader.loadObjectFromFile(input);
                                        Map parameters = new HashMap();
                                        parameters.put("prn", eSlipNumber);
                                        parameters.put("words", word);
                                        parameters.put("mop", mop);
                                        // Fill the Jasper Report
                                        JasperPrint jasperPrint
                                                = JasperFillManager.fillReport(jasperReport, parameters, con);
                                        Path path = Paths.get(folder, "receipt.PDF");
                                        // Creation of the Pdf Jasper Reports
                                        File f = new File(output.trim());
                                        if (f.exists() && !f.isDirectory()) {
                                            output = folder + "receipt-" + eSlipNumber + ".PDF";
                                            path = Paths.get(folder, "receipt-" + eSlipNumber + ".PDF");
                                        }
                                        JasperExportManager.exportReportToPdfFile(jasperPrint, output);
                                        //Send to Printer
                                        //PrintReceipt pr = new PrintReceipt();
                                        //pr.printNow(output);
                                        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
                                        respon_se = "Successful!";
                                        String uploadfile = "receipt-" + eSlipNumber + ".PDF";
                                        System.out.println("PRINT RECEIPT :: DONE :: RECEIPT NAME :: " + uploadfile + " :: RESPONSE :: " + response);
                                        System.out.println("START SENDING TO SFTP SERVER :: Filename :: "+uploadfile);
                                        try {
                                            ftp.uploadReceiptToRemote(uploadfile);
                                            System.out.println("DONE SENDING TO SFTP SERVER :: Filename :: "+uploadfile);
                                        } catch (JSchException e) {
                                            System.out.println(e.getLocalizedMessage());
                                        } catch (SftpException e) {
                                            System.out.println(e.getLocalizedMessage());
                                        }

                                    } else {
                                        respon_se = "Failed because amount is Ksh.0";
                                        System.out.println("PRINT RECEIPT :: FAILED :: RESPONSE :: " + response);
                                    }
                                }
                                //If No status is returned for the prn (not Y,N)
                                else
                                {
                                    respon_se = "Failed! E-Slip Status Not Found!";
                                    System.out.println("PRINT RECEIPT :: FAILED :: RESPONSE :: " + response);
                                }
                                //END OF RECEIPT SAVING\\

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
                        String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg + "," + eSlipNumber;
                        int update = 0;
                        //Checking the Consult Status before making an update
                        if (poststat_us.equalsIgnoreCase("PS")) {
                            update = DatabaseMethods.DB(update_query, 5, savedata);
                            if (update == 1) {
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DATA INSERTED :: " + savedata);
                                logger.info("POST PAYMENT :: SAVE RESULTS TO DB :: DONE SAVING PAYMENT RESULTS :: RESULT :: " + update);

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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
                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

                                //Update Status of An E-Slip in the E-Slip data table
                                int i = DatabaseMethods.DB(update_eslip_query, 2, eslipstatus_data);
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

    //3.Print a receipt (Tax Receipt)
    //Save the Receipt as a Pdf file after successful payment
    @WebMethod
    @WebResult(name = "response", targetNamespace = "")
    @RequestWrapper(localName = "PrintReceipt", targetNamespace = "")
    @ResponseWrapper(localName = "PrintResponse", targetNamespace = "")
    public ReceiptResponse printReceipt(
            @WebParam(name = "eslipNumber", targetNamespace = "") String eSlipNumber) throws IOException, JAXBException, ClassNotFoundException, JRException, SQLException
    {
        Numbers2Words ntw = new Numbers2Words();
        String c_lass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.class")).trim();
        Class.forName(c_lass);
        String serverName = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.ip").trim());
        String portNumber = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.port").trim());
        String sid = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.database").trim());
        String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
        String uname = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.username").trim());
        String pass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.password").trim());
        String input = "";
        String output = folder + "receipt.PDF";

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
            input = folder + "jasper/receipt.jasper";
        }
        if (mop.equalsIgnoreCase("2")) {
            mop = "Cheque";
            input = folder + "jasper/cheque.jasper";
        }
        if (mop.equalsIgnoreCase("3")) {
            mop = "Both (Cheque and Cash)";
            input = folder + "jasper/cheque.jasper";
        }
        String resp_onse = "";
        //Instance of the Receipt Response Class
        ReceiptResponse response = new ReceiptResponse();
        //Check if amount is 0
        if (!amnt.equalsIgnoreCase("0") && !amnt.equalsIgnoreCase("")) {
            String word = null;
            word = ntw.EnglishNumber(Long.parseLong(amnt));
            Connection con = DriverManager.getConnection(url, uname, pass);
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
            Path path = Paths.get(folder, "receipt.PDF");
            // Creation of the Pdf Jasper Reports
            File f = new File(output.trim());
            if (f.exists() && !f.isDirectory()) {
                output = folder + "receipt-" + eSlipNumber + ".PDF";
                path = Paths.get(folder, "receipt-" + eSlipNumber + ".PDF");
            }
            JasperExportManager.exportReportToPdfFile(jasperPrint, output);
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
            resp_onse = "TAX RECEIPT :: SAVE RECEIPT :: DONE :: RECEIPT NAME :: " + output + " :: RESPONSE :: Successful!";
            response.setResponse(resp_onse);
            logger.info("TAX RECEIPT :: SAVE RECEIPT :: DONE :: RECEIPT NAME :: " + output + " :: RESPONSE :: Successful!");
            String uploadfile = "receipt-" + eSlipNumber + ".PDF";
            System.out.println("PRINT RECEIPT :: DONE :: RECEIPT NAME :: " + uploadfile + " :: RESPONSE :: " + response);
            System.out.println("START SENDING TO SFTP SERVER :: Filename :: "+uploadfile);
            try {
                ftp.uploadReceiptToRemote(uploadfile);
                System.out.println("DONE SENDING TO SFTP SERVER :: Filename :: "+uploadfile);
            } catch (JSchException e) {
                System.out.println(e.getLocalizedMessage());
            } catch (SftpException e) {
                System.out.println(e.getLocalizedMessage());
            }
        } else {
            resp_onse = "TAX RECEIPT :: SAVE RECEIPT :: FAILED :: RESPONSE :: Failed because amount is Ksh.0";
            response.setResponse(resp_onse);
            logger.info("TAX RECEIPT :: SAVE RECEIPT :: FAILED :: RESPONSE :: Failed because amount is Ksh.0");
        }
        return response;
    }

    //Delete a PRN (E-Slip Number)
    //Web Method for deleting a PRN whenever a customer request
    @WebMethod
    @WebResult(name = "response", targetNamespace = "")
    @RequestWrapper(localName = "DeletePRN", targetNamespace = "")
    @ResponseWrapper(localName = "DeleteResponse", targetNamespace = "")
    public DeletePRNResponse deletePRN(
            @WebParam(name = "eslipNumber", targetNamespace = "") String eSlipNumber){
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
        System.out.println("Flag :: " +deleteflag);
        String data = "Y,"+eSlipNumber;
        if (eslipstatus.equalsIgnoreCase("N")) {
            if(deleteflag.equalsIgnoreCase("N")) {
                update = DatabaseMethods.DB(query, 2, data);
                if (update == 1) {
                    logger.info("DELETE PRN :: DONE DELETING E-SLIP :: RESULT :: " + update);
                    response.setResponse("DELETE PRN :: SUCCESSFULLY DELETED E-SLIP (PRN)");
                } else {
                    logger.info("DELETE PRN :: FAILED TO DELETE PRN :: RESULT :: " + update);
                    response.setResponse("DELETE PRN :: FAILED TO DELETE AN E-SLIP (PRN)");
                }
            }
            else
            {
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


    //Check PRN (E-SLIP) Details before deleting a PRN
    @WebMethod
    @WebResult(name = "details", targetNamespace = "")
    @RequestWrapper(localName = "CheckPRNDetails", targetNamespace = "")
    @ResponseWrapper(localName = "PRNDetailsResponse", targetNamespace = "")
    public EslipDetailsResponse checkPRNDetails(
            @WebParam(name = "eslipNumber", targetNamespace = "") String eSlipNumber){
        //Queries
        //Select details
        String query = cn.getProperties().getProperty("sql.query.select.eslipdata.before.delete").trim();
        String details = DatabaseMethods.selectValues(query, 9, 1, eSlipNumber);
        //Instance of the Check PRN Number Response class
        EslipDetailsResponse response = new EslipDetailsResponse();
        if(!details.equalsIgnoreCase(""))
        {
            String[] data =details.split(",");
            //System.out.println(Arrays.toString(data));
            response.setEslipnumber(data[0]);
            response.setTaxpayername(data[1]);
            response.setTaxpayerpin(data[2]);
            response.setAmount(data[3]);
            response.setStatus(data[4]);
            response.setRemarks(data[5]);
            response.setPaymentadvicedate(data[6]);
            response.setTaxcomponent(data[7]);
            response.setTaxcode(data[8]);
        }
        logger.info(response.toString());
        return response;
    }
}
