package com.autofine.mandate_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MandateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MandateServiceApplication.class, args);
	}

}
