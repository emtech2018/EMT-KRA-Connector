package com.emtech.service.itax.utilities;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

/*@author Omukubwa Emukule*/

public class PrintReceipt
{
    public void printNow(String receiptname) {
        // TODO Auto-generated method stub
        //String fileName = "C:/Users/Omukubwa/Documents/goal.pdf";
        // Open the file
        InputStream in = null;
        try {
            in = new FileInputStream(receiptname);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Figure out what type of file we're printing
        DocFlavor myFormat = getFlavorFromFilename(receiptname);
        System.out.println("Document Flavour :\n"+getFlavorFromFilename(receiptname));
        // Create a Doc
        Doc myDoc = new SimpleDoc(in, myFormat, null);
        // Build a set of attributes
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(OrientationRequested.PORTRAIT);
        aset.add(new Copies(1));
        aset.add(Sides.ONE_SIDED);
        aset.add(MediaSizeName.INVOICE);
        aset.add(new NumberUp(2));
        aset.add(Finishings.STAPLE);
        // discover the printers that can print the format according to the
        // instructions in the attribute set
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null,null);
        System.out.println(Arrays.toString(services));

        // Create a print job from one of the print services
        if (services.length > 0) {
            System.out.println("The print sent to>>>" + services[1].getName());
            //DocPrintJob job = services[0].createPrintJob();
            DocPrintJob job = services[1].createPrintJob();

            // Monitor the print job with a listener
            job.addPrintJobListener(new PrintJobAdapter() {
                public void printDataTransferCompleted(PrintJobEvent e) {
                    System.out.println("Data transfer completed!");
                }

                public void printJobNoMoreEvents(PrintJobEvent e) {
                    System.out.println("No more events!");
                }

                public void printJobRequiresAttention(PrintJobEvent e) {
                    System.out.println("Requires Attention!");
                }

                public void printJobFailed(PrintJobEvent e) {
                    System.out.println("Print Job Failed!");
                }

                public void printJobCompleted(PrintJobEvent e) {
                    System.out.println("Print Job Completed!");
                }

                public void printJobCanceled(PrintJobEvent e) {
                    System.out.println("Print Job Cancelled!");
                }
            });
            /*
            try {
                //job.print(myDoc, aset);
            } catch (PrintException pe) {
                pe.printStackTrace();
            }
             */
            System.out.println("The print job ........");
        }
    }

    // A utility method to return a DocFlavor object matching the
    // extension of the filename.
    public static DocFlavor getFlavorFromFilename(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        extension = extension.toLowerCase();
        if (extension.equals("gif"))
            return DocFlavor.INPUT_STREAM.GIF;
        else if (extension.equals("jpeg"))
            return DocFlavor.INPUT_STREAM.JPEG;
        else if (extension.equals("jpg"))
            return DocFlavor.INPUT_STREAM.JPEG;
        else if (extension.equals("png"))
            return DocFlavor.INPUT_STREAM.PNG;
        else if (extension.equals("pdf"))
            return DocFlavor.INPUT_STREAM.PDF;
        else if (extension.equals("ps"))
            return DocFlavor.INPUT_STREAM.POSTSCRIPT;
        else if (extension.equals("txt"))
            return DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8;
            // Fallback: try to determine flavor from file content
        else
            return DocFlavor.INPUT_STREAM.AUTOSENSE;
    }
}
