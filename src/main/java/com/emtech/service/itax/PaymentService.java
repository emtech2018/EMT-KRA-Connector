/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emtech.service.itax;

import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.KRAPaymentGatewayService;
import com.emtech.service.itax.utilities.*;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 *
 * @author emukule
 */
@WebService(serviceName = "PaymentService")
public class PaymentService {
    //Variable
    //Connection Timeout
    private int PAYMENT_GATEWAY_WEBSERVICE_TIMEOUT = 5000;
    //Web Service URL
    private String PAYMENT_GATEWAY_WEBSERVICE_URL = "https://196.61.52.40/PaymentGateway/KRAPaymentGateway";
    //Log In ID
    private String LOGINID = "nQ9KIrHD+Mf3CHRqrUPPUA==";
    //Password
    private String PASSWORD = "n+doimNhQwOiuV9w/hb4Eg==";
    //Bank Code
    private String BANKCODE = "54";
    //System Code
    private String SYSTEMCODE = "PG";
    //Payment Mode
    private String PAYMENTMODE = "1";
    //Currency
    private String CURRENCY = "KES";
    //Remitter Id
    private String REMiTTERID = "12345";
    //Remitter Name
    private String REMITTERNAME = "VCB TEST";
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

    //Consulting an Eslip
    @WebMethod
    @WebResult(name = "return", targetNamespace = "")
    @RequestWrapper(localName = "consult", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.CheckEslip")
    @ResponseWrapper(localName = "consultResponse", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.CheckEslipResponse")
    //1. Consulting the PRN (E-SLIP NUMBER)
    public CheckEslipResponse consultEslip(
            @WebParam(name = "arg0", targetNamespace = "") String prn)  throws IOException, JAXBException {
        //Decrypt Credentials
        String key = "VcbKey0123456789";
        String initVector = "VcbInitVector012";
        String password = Encryptor.decrypt(key,initVector,PASSWORD);
        String username = Encryptor.decrypt(key,initVector,LOGINID);
        String responseXML = "";
        //Update Query For status VALID
        String update_valid_query  = "UPDATE custom.eslip_data SET status = ?,remarks = ?,hashcode =?,systemcode = ?,slippaymentcode = ?,paymentadvicedate =?,taxpayerpin =?,taxpayerfullname = ?,totalamount = ?,docrefnumber =?,currency = ?,taxcode = ?,taxhead = ?,taxcomponent = ?,amountpertax = ?,taxperiod = ?,dateconsulted = ?,eslipstatus =?,consultstatus = ? WHERE eslipnumber ='"+prn+"'";
        //Update Query for other status
        String update_query = "UPDATE custom.eslip_data SET status = ?,remarks = ?,hashcode =?,dateconsulted = ?,eslipstatus =?,consultstatus = ? WHERE eslipnumber ='"+prn+"'";
        //Select Query
        String select_query = "SELECT consultstatus FROM custom.eslip_data WHERE eslipnumber = ?";

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
        String sendsql = "INSERT INTO custom.eslip_data (traceno,eslipnumber,status,remarks,hashcode,systemcode,slippaymentcode,paymentadvicedate,taxpayerpin,taxpayerfullname,totalamount,docrefnumber,currency,taxcode,taxhead,taxcomponent,amountpertax,taxperiod,dateconsulted,eslipstatus,consultstatus) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        //Check if the trace no is already in the database
        if(DatabaseMethods.findDuplicates("SELECT taxcode FROM custom.eslip_data WHERE traceno =?",1,String.valueOf(traceno)))
        {
            traceno = rn.nextInt(max - min + 1) + min;
        }

        String senddata = ""+traceno +","+prn+",N/A"+",N/A"+",N/A"+",N/A"+",N/A"+",N/A"+",N/A"+",N/A"+",0000"+",N/A"+",N/A"+",0000"+",N/A"+",N/A"+",0000"+","+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date())+","+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date())+","+"N"+","+"CS";
        //Check for duplicate values
        if(DatabaseMethods.findDuplicates(select_query,1,prn))
        {
            System.out.println("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: CHECKING DUPLICATES :: RESULTS :: Found this e-slip number in the db");
        }
        else {
            int insert = DatabaseMethods.DB(sendsql, 21, senddata);
            logger.info("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: DATA INSERTED :: " + senddata);
            logger.info("CONSULT E-SLIP :: SAVE DATA TO BE SENT TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + insert);
        }

        //----END OF DATABASE INSERT ----\\
        //Consulting
        logger.info("Consulting the E-SLIP No " + prn);
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
                if(slippaymentcode.equalsIgnoreCase(""))
                {
                    slippaymentcode = "N/A";
                }
                if(taxhead.equalsIgnoreCase(""))
                {
                    taxhead = "N/A";
                }
                if(docrefno.equalsIgnoreCase(""))
                {
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
                String data = ""+responseStatus+","+responseRemaks+","+responseHash+","+systemCode+","+slippaymentcode+","+paymentadvicedate+","+taxPayerpin+","+taxpayername+","+totalamount+","+docrefno+","+currency+","+taxcode+","+taxhead+","+taxcomponent+","+amountpertax+","+taxperiod+","+timeStamp+","+"N"+","+"CR";
                int update = 0;
                //Checking the Consult Status before making an update
                if(DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                    update = DatabaseMethods.DB(update_valid_query, 19, data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                }
                else
                {
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
                String data = ""+responseStatus+","+responseRemaks+","+responseHash+","+timeStamp+","+"N"+","+"CR";
                int update = 0;
                //Checking the Consult Status before making an update
                if(DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                    update = DatabaseMethods.DB(update_query, 6, data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                }
                else
                {
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
                String data = ""+responseStatus+","+responseRemaks+","+responseHash+","+timeStamp+","+"N"+","+"CR";
                int update = 0;
                //Checking the Consult Status before making an update
                if(DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                    update = DatabaseMethods.DB(update_query, 6, data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                }
                else
                {
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
                String data = ""+responseStatus+","+responseRemaks+","+responseHash+","+timeStamp+","+"N"+","+"CR";
                int update = 0;
                //Checking the Consult Status before making an update
                if(DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                    update = DatabaseMethods.DB(update_query, 6, data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                }
                else
                {
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
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                String data = ""+responseStatus+","+responseRemaks+","+responseHash+","+timeStamp+","+"N"+","+"CR";
                int update = 0;
                //Checking the Consult Status before making an update
                if(DatabaseMethods.selectValues(select_query, 1, 1, prn).equalsIgnoreCase("CS")) {
                    update = DatabaseMethods.DB(update_query, 6, data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DATA INSERTED :: " + data);
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: DONE SAVING E-SLIP RESULTS :: RESULT :: " + update);
                }
                else
                {
                    logger.info("CONSULT E-SLIP :: SAVE RESULTS TO DB :: RESPONSE FOR THIS PRN WAS ALREADY RECEIVED :: SKIPPING UPDATE TASK");
                }
                //----END OF DATABASE UPDATE ----\\
            }

            else {
                logger.info("UNKNOWN ERROR : CONSULTATION OPERATION FAILED!");
                System.out.println("UNKNOWN ERROR : CONSULTATION OPERATION FAILED!");
            }
            logger.info("DONE CONSULTING PRN " + prn);

        }
        return response;
    }


    //Process Payment
    //2.POST Payment to the I-TAX Payment Gateway
    @WebMethod
    @WebResult(name = "response", targetNamespace = "")
    @RequestWrapper(localName = "processPayment", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.AcceptPayment")
    @ResponseWrapper(localName = "processPaymentResponse", targetNamespace = "http://impl.facade.pg.kra.tcs.com/", className = "com.emtech.impl.PaymentResponse")
    public PaymentResponse postPayment(@WebParam(name = "eSlipNumber", targetNamespace = "") String eSlipNumber,
                                       @WebParam(name = "meansOfPayment", targetNamespace = "") String meansOfPayment) throws IOException, JAXBException {
        System.out.println("E-Slip Number : " + eSlipNumber);
        String ccrspayment = "";
        //Decrypt Credentials
        String key = "VcbKey0123456789";
        String initVector = "VcbInitVector012";
        String password = Encryptor.decrypt(key, initVector, PASSWORD);
        String username = Encryptor.decrypt(key, initVector, LOGINID);

        //Update Payment Details Query
        String update_query = "UPDATE custom.paymentdetails SET poststatus = ?,responsecode = ?,responsestatus =?,message = ? WHERE eslipnumber ='"+eSlipNumber+"'";
        //Select Query
        String select_query = "SELECT poststatus FROM custom.paymentdetails WHERE eslipnumber = ?";
        //Update E-Slip Details Query
        String update_eslip_query = "UPDATE custom.eslip_data SET eslipstatus = ? WHERE eslipnumber = '"+eSlipNumber+"'";
        //Select Query for checking if a payment for an e-slip has been posted
        String select_eslip_status = "SELECT eslipstatus FROM custom.eslip_data WHERE eslipnumber = ?";
        //Instance of Payment Response class
        PaymentResponse response = new PaymentResponse();

        //Checking If Payment Has Already Been Posted For an E-Slip \\
        if(DatabaseMethods.selectValues(select_eslip_status, 1, 1, eSlipNumber).equalsIgnoreCase("Y")) {
            logger.info("POSTING PAYMENT :: CHECKING PRN NUMBER :: RESPONSE :: PAYMENT WAS ALREADY POSTED FOR THIS E-SLIP NUMBER");
        }
        else {
            //Instance of the Payment File Class
            PaymentDetails pd = new PaymentDetails();
            //Cheque Details
            ChequeDetails cd = new ChequeDetails();
            cd.setBankOfCheque("");
            cd.setBranchOfCheque("");
            cd.setChequeNumber("");
            cd.setChequeDate("");
            cd.setChequeAmount("");
            cd.setChequeAccount("");
            //cd.setChequeType("");

            //Array List with Cheque details
            ArrayList<ChequeDetails> chequeDetailsArrayList = new ArrayList<>();
            chequeDetailsArrayList.add(cd);

            //Payment Header
            PaymentHeader ph = new PaymentHeader();
            ph.setSystemCode(SYSTEMCODE);
            ph.setBranchCode("54001");
            ph.setBankTellerId("12");
            ph.setBankTellerName("TEST VCB");
            ph.setPaymentMode(PAYMENTMODE);
            ph.setMeansofPayment(meansOfPayment);
            ph.setChequeAmount("0");
            ph.setRemitterId(REMiTTERID);
            ph.setRemitterName(REMITTERNAME);
            ph.setESlipNumber(eSlipNumber);
            ph.setSlipPaymentCode(DatabaseMethods.selectValues("SELECT slippaymentcode FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setPaymentAdviceDate(DatabaseMethods.selectValues("SELECT paymentadvicedate FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber).trim());
            ph.setPaymentReference(DatabaseMethods.selectValues("SELECT taxcomponent FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setTaxpayerName(DatabaseMethods.selectValues("SELECT taxpayerfullname FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setTaxpayerPIN(DatabaseMethods.selectValues("SELECT taxpayerpin FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setTotalAmount(DatabaseMethods.selectValues("SELECT totalamount FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setDocRefNumber(DatabaseMethods.selectValues("SELECT docrefnumber FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            ph.setDateOfCollection(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            //ph.setDateOfCollection("2021-07-09T16:43:06");
            ph.setCurrency(CURRENCY);
            ph.setCashAmount(DatabaseMethods.selectValues("SELECT totalamount FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, eSlipNumber));
            //Arraylist with payment header details
            ArrayList<PaymentHeader> paymentHeaderArrayList = new ArrayList<>();
            paymentHeaderArrayList.add(ph);

            //Checking the means of payment
            //Means Of Payment is Cash (Do Not Add Cheque Details to the Payment XML)
            if (meansOfPayment.equalsIgnoreCase("1")) {
                logger.info("POSTING PAYMENT :: MEANS OF PAYMENT :: CASH :: EXCLUDING CHEQUE DETAILS FROM THE PAYMENT XML");
            }
            //Means of Payment = Cheque (Same Bank) and Both (Cash & Cheque) (Add Cheque Details to the Payment XML)
            else if (meansOfPayment.equalsIgnoreCase("2") || meansOfPayment.equalsIgnoreCase("3")) {
                //Adding Cheque details to payment details
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
            String channelid = "FI";

            //Handling Null Values
            if (slippaymentcode.equalsIgnoreCase("")) {
                slippaymentcode = "N/A";
            }
            if (docrefno.equalsIgnoreCase("")) {
                docrefno = "N/a";
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
                chequeamount = "N/A";
            }
            if (chequeaccount.equalsIgnoreCase("")) {
                chequeaccount = "N/A";
            }

            //SAVE DETAILS TO BE SENT TO KRA FOR POSTING PAYMENT IN THE DATABASE
            logger.info("POST PAYMENT :: SAVE DATA TO BE SENT TO DB :: STARTING TO SAVE PAYMENT DATA");
            String sendsql = "INSERT INTO custom.paymentdetails(systemcode,branchcode,banktellerid,banktellername,paymentmode,meansofpayment,remitterid,remittername,eslipnumber,slippaymentcode,paymentadvicedate,paymentreference,taxpayerpin,taxpayerfullname,totalamount,docrefno,dateofcollection,cashamount,chequesamount,hashcode,bankofcheque,branchofcheque,chequenumber,chequedate,chequeamount,chequeaccount,dateofpayment,poststatus,responsecode,responsestatus,message,channelid,currency) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
                    String savedata = "" + post_status + "," + r_code + "," + r_status + "," + msg;
                    int update = 0;
                    //Checking the Consult Status before making an update
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
                        update = DatabaseMethods.DB(select_query, 4, savedata);
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
                    if (DatabaseMethods.selectValues(select_query, 1, 1, eSlipNumber).equalsIgnoreCase("PS")) {
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
        return response;
    }
}
