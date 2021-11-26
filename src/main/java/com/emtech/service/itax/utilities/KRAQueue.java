package com.emtech.service.itax.utilities;
//Contains Code for managing the Queue (Retry posting to KRA for failed transactions after debiting customer A/C)

import com.emtech.service.itax.PaymentService;
import net.sf.jasperreports.engine.JRException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;

public class KRAQueue {
    //Instance of Payment Service
    PaymentService ps = new PaymentService();

    //Database Connection Class
    DatabaseMethods db = new DatabaseMethods();

    //Common Utilities Class
    CommonUtils cu = new CommonUtils();

    //Instance of the Configuration Classes
    Configurations cn = new Configurations();

    //Select Count of failed transactions query
    private String select_count = cn.getProperties().getProperty("sql.query.select.kraqueue.count").trim();

    //Select all details of failed transactions query
    private String select_failed = cn.getProperties().getProperty("sql.query.select.kraqueue").trim();

    //Auto Sending Failed Transactions
    public void autoSendToKra() throws JRException, SQLException, JAXBException, IOException, ClassNotFoundException {
        String st = "N";
        //Array with all data selected from the kra queue table
        String[] data = null;
        //No. of failed trans
        String count_failed = DatabaseMethods.selectValues(select_count,1,1,st);
        //Convert to int
        int count = Integer.parseInt(count_failed);
        System.out.println(count_failed);
        //Variables
        String eslipnumber = "";
        String branchcode = "";
        String tellername = "";
        String tellerid = "";
        String mop = "";
        String status = "";
        String chequeno = "";
        String account = "";

        if(count == 1)
        {
            //Select details of this transaction and retry sending
            String tran_details = DatabaseMethods.selectValues(select_failed,6,1,st.trim());
            if(!tran_details.equalsIgnoreCase(""))
            {
                data = tran_details.split(",");
                eslipnumber = data[0];
                branchcode = data[1];
                tellername = data[2];
                tellerid = data[3];
                mop = data[4];
                status = data[5];
                //Post to KRA
                ps.postTaxPayment(eslipnumber,mop,tellername,tellerid,branchcode,chequeno,account);
            }
        }

        else if(count > 1)
        {
            //Loop through the failed transactions and post them again
            for (int j = 0;j <= count; j++)
            {
                //Select details of this transaction and retry sending
                String tran_details = DatabaseMethods.selectValues(select_failed,6,1,st);
                if(!tran_details.equalsIgnoreCase(""))
                {
                    data = tran_details.split(",");
                    eslipnumber = data[0];
                    branchcode = data[1];
                    tellername = data[2];
                    tellerid = data[3];
                    mop = data[4];
                    status = data[5];
                    //Post to KRA
                    ps.postTaxPayment(eslipnumber,mop,tellername,tellerid,branchcode,chequeno,account);
                }
            }
        }
    }

}
