package com.emtech.service;

import com.emtech.service.itax.PaymentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.xml.ws.Endpoint;

@SpringBootApplication
public class ServiceApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
		//Publish web service end point when the application runs
		Endpoint.publish("http://192.168.154.1:1958/Itax4VCBservice/ws/kra", new PaymentService());
	}
}
