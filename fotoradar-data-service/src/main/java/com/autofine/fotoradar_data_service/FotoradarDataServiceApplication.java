package com.autofine.fotoradar_data_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FotoradarDataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FotoradarDataServiceApplication.class, args);
	}

}
