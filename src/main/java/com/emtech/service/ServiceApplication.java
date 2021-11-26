package com.emtech.service;

import com.emtech.service.itax.InternetBankingService;
import com.emtech.service.itax.utilities.Configurations;
import com.emtech.service.itax.utilities.QueueManagementJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.xml.ws.Endpoint;

/*@author Omukubwa Emukule*/

@SpringBootApplication
public class ServiceApplication extends SpringBootServletInitializer {

	//Instance of the Configuration Classes
	static Configurations cn = new Configurations();

	//Time (Interval) for resending failed transactions
	private static int time = Integer.parseInt(cn.getProperties().getProperty("itax.kra.queue.time").trim());

	//Main
	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
		//Publish web service end point when the application runs
		Endpoint.publish("http://localhost:1958/Itax4VCBservice/ws/kra", new InternetBankingService());
		try {

			// specify the job' s details..
			JobDetail job = JobBuilder.newJob(QueueManagementJob.class)
					.withIdentity("KRAQUEUE")
					.build();

			// specify the running period of the job
			Trigger trigger = TriggerBuilder.newTrigger()
					.withSchedule(SimpleScheduleBuilder.simpleSchedule()
							.withIntervalInSeconds(time)
							.repeatForever())
					.build();

			//schedule the job
			SchedulerFactory schFactory = new StdSchedulerFactory();
			Scheduler sch = schFactory.getScheduler();
			sch.start();
			sch.scheduleJob(job, trigger);

		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
}
