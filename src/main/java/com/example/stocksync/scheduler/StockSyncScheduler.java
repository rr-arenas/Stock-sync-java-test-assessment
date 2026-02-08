package com.example.stocksync.scheduler;

import com.example.stocksync.service.StockSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(StockSyncScheduler.class);

	private final StockSyncService stockSyncService;

	public StockSyncScheduler(StockSyncService stockSyncService) {
		this.stockSyncService = stockSyncService;
	}

	@Scheduled(cron = "${stock.sync.cron:0 */5 * * * ?}")
	public void runSync() {
		log.debug("Starting scheduled stock sync");
		try {
			stockSyncService.sync();
			log.debug("Scheduled stock sync completed");
		} catch (Exception e) {
			log.error("Scheduled stock sync failed", e);
		}
	}
}
