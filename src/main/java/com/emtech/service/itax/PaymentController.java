package com.emtech.service.itax;

import com.emtech.service.Numbers2Words;
import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.utilities.*;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import net.sf.jasperreports.engine.JRException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;

/*@author Omukubwa Emukule*/

@RestController
@RequestMapping("/kra")
@CrossOrigin(origins = "*")
public class PaymentController {
    //Instance of the Configuration Classes
    Configurations cn = new Configurations();

    //Instance of the Payment Service Class
    PaymentService service = new PaymentService();

    //Instance of Internet Banking Service
    InternetBankingService ib = new InternetBankingService();

    //Instance of numbers (amount) to words class
    Numbers2Words ntw = new Numbers2Words();

    //Receipts Folder
    String folder = cn.getProperties().getProperty("itax.folder").trim();

    //Encryption Key
    private String key = cn.getProperties().getProperty("enc.key").trim();

    //Encryption Init Vector
    private String initVector =  cn.getProperties().getProperty("enc.initVector").trim();
    //Instance of SFTP class
    sftp ftp = new sftp();

    //Consulting the E-Slip end point
    @RequestMapping("/consult/{prn}")
    public CheckEslipResponse checkEslip(@PathVariable("prn") String prn) throws Exception {
        return service.consultEslip(prn);
    }

    //Posting Tax Payment end point
    @RequestMapping("/pay/{prn}/{mop}/{teller}/{tellerid}/{branch}/{cno}/{account}")
    public PaymentResponse payTaxCash(@PathVariable("prn") String prn, @PathVariable("mop") String mop,@PathVariable("teller") String teller,@PathVariable("tellerid") String tellerid,@PathVariable("branch") String branch, @PathVariable("cno") String cno,@PathVariable("account") String account) throws IOException, JAXBException, JRException, SQLException, ClassNotFoundException, JSchException, SftpException {
        return service.postTaxPayment(prn,mop,teller,tellerid,branch,cno,account);
    }

    //Print Receipt End-Point
    @RequestMapping("/pdf/{prn}")
    public ReceiptResponse getReceipt(@PathVariable("prn")  String prn) throws SQLException, ClassNotFoundException, JRException, IOException, JSchException, SftpException, JAXBException {
        return service.printReceipt(prn);
    }

    //Print CBK Settlement Report
    @RequestMapping("/pdfcbk/{fromdate}/{todate}")
    public ReceiptResponse printCBKreport(@PathVariable("fromdate")  String fromdate,@PathVariable("todate")  String todate) throws SQLException, ClassNotFoundException, JRException, IOException, JSchException, SftpException, JAXBException {
        return service.printCBKReport(fromdate,todate);
    }

    //Delete PRN end point
    @RequestMapping("/delete/{prn}")
    public DeletePRNResponse delete(@PathVariable("prn") String prn) {
        return service.deletePRN(prn);
    }

    @RequestMapping("/as")
    public void auto() throws JRException, SQLException, JAXBException, IOException, ClassNotFoundException {
        KRAQueue kq = new KRAQueue();
        kq.autoSendToKra();
    }

}
