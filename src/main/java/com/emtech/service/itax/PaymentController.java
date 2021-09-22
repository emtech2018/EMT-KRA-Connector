package com.emtech.service.itax;

import com.emtech.service.Numbers2Words;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.utilities.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/kra")
public class PaymentController {
    //Instance of the Configuration Classes
    Configurations cn = new Configurations();
    //Instance of Print Receipt Class
    PrintReceipt pr = new PrintReceipt();
    //Instance of the Payment Service Class
    PaymentService service = new PaymentService();
    //Instance of Test Service class
    TestService tservice = new TestService();
    //Instance of numbers (amount) to words class
    Numbers2Words ntw = new Numbers2Words();
    //Receipts Folder
    String folder = cn.getProperties().getProperty("itax.folder").trim();
    //Encryption Key
    private String key = cn.getProperties().getProperty("enc.key").trim();
    //Encryption Init Vector
    private String initVector =  cn.getProperties().getProperty("enc.initVector").trim();

    //Consulting the E-Slip end point
    @RequestMapping("/consult/{prn}")
    public CheckEslipResponse checkEslip(@PathVariable("prn") String prn) throws Exception {
        return service.consultEslip(prn);
    }

    //Posting Tax Payment end point
    @RequestMapping("/pay/{prn}/{mop}/{cno}/{account}")
    public PaymentResponse payTaxCash(@PathVariable("prn") String prn, @PathVariable("mop") String mop, @PathVariable("cno") String cno,@PathVariable("account") String account) throws IOException, JAXBException, JRException, SQLException, ClassNotFoundException {
        return service.postTaxPayment(prn,mop,cno,account);
    }

    //Print Receipt End-Point
    @RequestMapping("/pdf/{prn}")
    public String  getReceipt(@PathVariable("prn")  String prn) throws SQLException, ClassNotFoundException, JRException, IOException {
        String c_lass = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.class")).trim();
        Class.forName(c_lass);
        String serverName = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.ip").trim());
        String portNumber = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.port").trim());
        String sid = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.database").trim());
        String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
        String username = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.username").trim());
        String password = Encryptor.decrypt(key, initVector, cn.getProperties().getProperty("db.password").trim());
        String input = "";
        String output =folder+"receipt.pdf";

        //Queries
        //Select Amount
        String select_amount = cn.getProperties().getProperty("sql.query.select.amount").trim();
        String amount = DatabaseMethods.selectValues(select_amount, 1, 1, prn);
        //Select Means of Payment
        String select_mop = cn.getProperties().getProperty("sql.query.select.meansofpayment").trim();
        String mop = DatabaseMethods.selectValues(select_mop, 1, 1, prn);

        //Check the means of Payment
        if(mop.equalsIgnoreCase("1"))
        {
            mop = "Cash";
            input = folder+"receipt.jasper";
        }
        if(mop.equalsIgnoreCase("2"))
        {
            mop = "Cheque";
            input = folder+"cheque.jasper";
        }
        if(mop.equalsIgnoreCase("3"))
        {
            mop = "Both (Cheque and Cash)";
            input = folder+"cheque.jasper";
        }
        String response = "";
        //Check if amount is 0
        if(!amount.equalsIgnoreCase("0")&& !amount.equalsIgnoreCase("")) {
            String word = null;
            word = ntw.EnglishNumber(Long.parseLong(amount));
            Connection con = DriverManager.getConnection(url, username, password);
            JasperReport jasperReport
                    = (JasperReport) JRLoader.loadObjectFromFile(input);
            Map parameters = new HashMap();
            parameters.put("prn", prn);
            parameters.put("words", word);
            parameters.put("mop",mop);
            // Fill the Jasper Report
            JasperPrint jasperPrint
                    = JasperFillManager.fillReport(jasperReport, parameters, con);
            Path path = Paths.get(folder, "receipt.pdf");
            // Creation of the Pdf Jasper Reports
            File f = new File(output.trim());
            if (f.exists() && !f.isDirectory()) {
                output = folder+"receipt-" + prn + ".pdf";
                path = Paths.get(folder, "receipt-" + prn + ".pdf");
            }
            JasperExportManager.exportReportToPdfFile(jasperPrint, output);
            //Send to Printer
            PrintReceipt pr = new PrintReceipt();
            pr.printNow(output);
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
            response = "Successful!";
            System.out.println("POST PAYMENT :: SAVE RECEIPT :: DONE :: RECEIPT NAME :: "+output+ " :: RESPONSE :: "+response);
        }
        else
        {
            response = "Failed because amount is Ksh.0";
            System.out.println("POST PAYMENT :: SAVE RECEIPT :: FAILED :: RESPONSE :: "+response);
        }
        return response;
    }

   //Test endpoint
   @RequestMapping("/test")
   public void test(){
       //return tservice.test();
       //pr.printReceipt();
   }

   //Delete PRN end point
    @RequestMapping("/delete/{prn}")
    public DeletePRNResponse delete(@PathVariable("prn") String prn) {
        return service.deletePRN(prn);
    }
}
