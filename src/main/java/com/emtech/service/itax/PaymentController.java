package com.emtech.service.itax;

import com.emtech.service.itax.tcs.kra.pg.facade.impl.CheckEslipResponse;
import com.emtech.service.itax.utilities.PaymentResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBException;
import java.io.IOException;

@RestController
@RequestMapping("/kra")
public class PaymentController {
    PaymentService service = new PaymentService();

    //Consulting the E-Slip
    @RequestMapping("/consult/{prn}")
    public CheckEslipResponse checkEslip(@PathVariable("prn") String prn) throws Exception {
        return service.consultEslip(prn);
    }

    //Posting a Payment
    @RequestMapping("/pay/{prn}/{mop}")
    public PaymentResponse payTax(@PathVariable("prn") String prn, @PathVariable("mop") String mop) throws IOException, JAXBException {
        return service.postPayment(prn,mop);
    }

    /*Test DB (Save Consult E-Slip Data)
    @RequestMapping("/testdb")
    public String testDB()
    {
        return service.testDB();
    }

    //Test DB (Save Post Payment Data)
    @RequestMapping("/testpay")
    public String testDBPostPayment()
    {
        return service.testSavePayment();
    }
     */
}
