package com.moneymoment.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MoneyMomentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyMomentApplication.class, args);
		System.out.println("MoneyMomentApplication started successfully.");
	}

}
