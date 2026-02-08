package com.example.stocksync.config;

import com.example.stocksync.service.StockSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs a single stock sync shortly after the application is ready,
 * so GET /products returns data without waiting for the first scheduled run.
 */
@Component
public class StockSyncInitializer {

	private static final Logger log = LoggerFactory.getLogger(StockSyncInitializer.class);

	private final StockSyncService stockSyncService;

	public StockSyncInitializer(StockSyncService stockSyncService) {
		this.stockSyncService = stockSyncService;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Order(1)
	public void onReady() {
		log.info("Running initial stock sync");
		try {
			stockSyncService.sync();
			log.info("Initial stock sync completed");
		} catch (Exception e) {
			log.warn("Initial stock sync failed (scheduler will retry): {}", e.getMessage());
		}
	}
}
