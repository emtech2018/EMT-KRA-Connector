package com.emtech.service.itax;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.export.SimpleDocxReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.File;
import java.sql.Connection;
import java.util.Map;

/*@author Omukubwa Emukule*/

//This Class consists the code for printing receipts after a payment has been made successfully
public class TaxReceipts {

    //Create a Pdf from jasper
    /*
    public static void main(String[] args) {
       String query = "SELECT * FROM custom.eslip_data WHERE ESLIPNUMBER = ?";
       System.out.println(DatabaseMethods.selectValues(query, 21, 1, "2020210001980202"));
       /*
        try {
            Map<String, Object> map = new HashMap<>();
            Connection conn = DatabaseConnection.getInstance().getConnection();
            map.put("QUERY", "Select u.name, u.status from user_info u where u.user_name = 'Thanuj'");
            JasperReport report = JasperCompileManager.compileReport("report1.jrxml");
            JasperPrint jp = JasperFillManager.fillReport(report, map, conn);
            JasperViewer.viewReport(jp, false);
        } catch (JRException ex) {
            //Logger.getLogger(ReportTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            //Logger.getLogger(ReportTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            ///Logger.getLogger(ReportTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        */

    //}


    //Generating a report
    public static String OUT_PUT = "receipt.pdf";
    public static String REPORT = "//home//emukule//Downloads//receipt.jrxml";

    public void generateReceipt(String reportPath,
                               Map<String, Object> map, Connection con) {
        try {
            JasperReport jr = JasperCompileManager.compileReport(
                    ClassLoader.getSystemResourceAsStream(reportPath));
            JasperPrint jp = JasperFillManager.fillReport(jr, map, con);
            JRDocxExporter export = new JRDocxExporter();
            export.setExporterInput(new SimpleExporterInput(jp));
            export.setExporterOutput(new SimpleOutputStreamExporterOutput(new File(OUT_PUT)));
            SimpleDocxReportConfiguration config = new SimpleDocxReportConfiguration();
            export.setConfiguration(config);
            export.exportReport();
        } catch (JRException ex) {
            ex.printStackTrace();
        }
    }
}
