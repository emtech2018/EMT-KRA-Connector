package com.emtech.service.itax.utilities;

import net.sf.jasperreports.engine.JRException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;

public class QueueManagementJob implements Job {
    KRAQueue kq = new KRAQueue();

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            kq.autoSendToKra();
        } catch (JRException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.getLocalizedMessage();
        } catch (JAXBException e) {
            e.getLocalizedMessage();
        } catch (IOException e) {
            e.getLocalizedMessage();
        } catch (ClassNotFoundException e) {
            e.getLocalizedMessage();
        }

        //System.out.println("Failed trans sending now ...");
    }

}
