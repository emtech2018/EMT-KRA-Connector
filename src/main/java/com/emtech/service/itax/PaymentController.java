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

    //Posting Tax Payment
    @RequestMapping("/pay/{prn}/{mop}/{cno}/{account}")
    public PaymentResponse payTaxCash(@PathVariable("prn") String prn, @PathVariable("mop") String mop, @PathVariable("cno") String cno,@PathVariable("account") String account) throws IOException, JAXBException {
        return service.postTaxPayment(prn,mop,cno,account);
    }
}
