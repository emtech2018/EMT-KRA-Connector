package com.emtech.service.itax.utilities;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class PrintReceipt {

    public void printReceipt(String paramString1, String paramString2) throws Exception {
        System.out.println("Entry : printReceipt");
        FileInputStream fileInputStream = null;
        String str1 = null;
        str1 = paramString1;
        String str2 = paramString2 + ".txt";
        JOptionPane.showMessageDialog(null, str2);
        JOptionPane.showMessageDialog(null, "printer : " + paramString1);
        try {
            fileInputStream = new FileInputStream(new String(str2));
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            JOptionPane.showMessageDialog(null, fileNotFoundException.getMessage());
        }
        if (fileInputStream == null)
            return;
        JOptionPane.showMessageDialog(null, "preping print job");
        PrintService printService = null;
        DocFlavor.INPUT_STREAM iNPUT_STREAM = DocFlavor.INPUT_STREAM.AUTOSENSE;
        SimpleDoc simpleDoc = new SimpleDoc(fileInputStream, iNPUT_STREAM, null);
        HashPrintRequestAttributeSet hashPrintRequestAttributeSet = new HashPrintRequestAttributeSet();
        PrintService[] arrayOfPrintService = PrintServiceLookup.lookupPrintServices(iNPUT_STREAM, hashPrintRequestAttributeSet);
        for (byte b = 0; b < arrayOfPrintService.length; b++) {
            String str = arrayOfPrintService[b].toString();
            if (str.contains(str1)) {
                printService = arrayOfPrintService[b];
                JOptionPane.showMessageDialog(null, "### PRINTER FOUND ### Printer Name is:: " + str);
                break;
            }
        }
        if (printService != null) {
            DocPrintJob docPrintJob = printService.createPrintJob();
            try {
                docPrintJob.print(simpleDoc, hashPrintRequestAttributeSet);
                System.out.println("Printing was successful");
                JOptionPane.showMessageDialog(null, "Printing was successful");
            } catch (Exception exception) {
                System.out.println("Error in:: Printing Receipt");
                JOptionPane.showMessageDialog(null, "Error in:: Printing Receipt");
                exception.printStackTrace();
                System.out.println("Printing was not successful");
                JOptionPane.showMessageDialog(null, "Error in:: Printing Receipt");
            }
        } else {
            System.out.println("NO PRINTER SERVICE FOUND");
            JOptionPane.showMessageDialog(null, "Error in:: Printing Receipt");
        }
        System.out.println("Exit : printReceipt");
    }
}
