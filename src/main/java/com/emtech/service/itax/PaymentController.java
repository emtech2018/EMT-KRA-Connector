package com.emtech.service.itax;

import com.emtech.service.Numbers2Words;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.utilities.DatabaseMethods;
import com.emtech.service.itax.utilities.PaymentResponse;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/kra")
public class PaymentController {
    PaymentService service = new PaymentService();
    Numbers2Words ntw = new Numbers2Words();

    //Consulting the E-Slip
    @RequestMapping("/consult/{prn}")
    public CheckEslipResponse checkEslip(@PathVariable("prn") String prn) throws Exception {
        return service.consultEslip(prn);
    }

    //Posting Tax Payment
    @RequestMapping("/pay/{prn}/{mop}/{cno}/{account}")
    public PaymentResponse payTaxCash(@PathVariable("prn") String prn, @PathVariable("mop") String mop, @PathVariable("cno") String cno,@PathVariable("account") String account) throws IOException, JAXBException, JRException, SQLException, ClassNotFoundException {
        return service.postTaxPayment(prn,mop,cno,account);
    }

    @RequestMapping("/pdf/{prn}")
    public String  getReceipt(@PathVariable("prn")  String prn) throws SQLException, ClassNotFoundException, JRException {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String serverName = "3.21.220.181";
        String portNumber = "1521";
        String sid = "sitdb";
        String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
        String username = "system";
        String password = "manager";
        String input = "/home/emukule/Downloads/emtech-vcb-itax/receipt.jasper";
        String output ="/home/emukule/Downloads/emtech-vcb-itax/receipt.pdf";
        String select_amount = "SELECT totalamount FROM custom.eslip_data WHERE eslipnumber = ?";
        String amount = DatabaseMethods.selectValues("SELECT totalamount FROM custom.eslip_data WHERE eslipnumber = ?", 1, 1, prn);
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
            // Fill the Jasper Report
            JasperPrint jasperPrint
                    = JasperFillManager.fillReport(jasperReport, parameters, con);
            // Creation of the Pdf Jasper Reports
            File f = new File(output.trim());
            if (f.exists() && !f.isDirectory()) {
                output = "/home/emukule/Downloads/emtech-vcb-itax/receipt-" + prn + ".pdf";
            }
            JasperExportManager.exportReportToPdfFile(jasperPrint, output);
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
}
