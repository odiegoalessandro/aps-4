package com.aps.weather.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApsWeatherApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApsWeatherApplication.class, args);
	}

}
