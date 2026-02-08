package com.example.stocksync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockSyncServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockSyncServiceApplication.class, args);
	}

}
